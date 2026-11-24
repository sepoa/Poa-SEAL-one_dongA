package sepoa.svl;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.onelogin.oidc.EntraOidcAuth;

import filters.ThreadLocalFilter;
import filters.ThreadLocalFilter.ReqRes;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;

/**
 * Microsoft Entra ID OIDC: 고객 예제 {@link EntraOidcAuth}와 동일한 PKCE + JWKS 검증.
 * 설정은 sepoa.properties / sepoa.{house}.properties 의 {@code sepoa.entraid.*} 키.
 */
@SuppressWarnings("deprecation")
public class EntraIdSSOServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");

		String code = request.getParameter("code");
		String state = request.getParameter("state");
		String oauthError = request.getParameter("error");
		Logger.debug.println("EntraIdSSO", this,
				"Request uri=" + request.getRequestURI()
				+ ", hasCode=" + (code != null && !code.isEmpty())
				+ ", hasState=" + (state != null && !state.isEmpty())
				+ ", hasError=" + (oauthError != null && !oauthError.isEmpty()));

		if (code != null) {
			code = code.trim().replaceAll("[\\r\\n\\t]", "");
			int spaces = 0;
			for (int i = 0; i < code.length(); i++) {
				if (code.charAt(i) == ' ') {
					spaces++;
				}
			}
			if (spaces > 0) {
				code = code.replace(' ', '+');
			}
		}

		try {
			if (oauthError != null && !oauthError.isEmpty()) {
				Logger.err.println("EntraIdSSO", this, "OAuth error: " + oauthError);
				redirectToClassicLogin(request, response, null);
				return;
			}

			if (code != null && state != null) {
				handleCallback(request, response, code);
				return;
			}

			startOAuth(request, response);

		} catch (Exception e) {
			Logger.err.println("EntraIdSSO", this, e.getMessage());
			redirectToClassicLogin(request, response, null);
		}
	}

	private void startOAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String houseCode = firstNonEmpty(request.getParameter("house_code"),
				(String) request.getSession().getAttribute("HOUSE_CODE"));
		if (houseCode == null || houseCode.trim().isEmpty()) {
			Logger.err.println("EntraIdSSO", this, "Missing house_code on startOAuth");
			response.sendRedirect(request.getContextPath() + "/index.jsp");
			return;
		}
		houseCode = houseCode.trim();
		Logger.debug.println("EntraIdSSO", this, "startOAuth houseCode=" + houseCode);

		if (isBlank(cfg(houseCode, "sepoa.entraid.client.id", ""))
				|| isBlank(cfg(houseCode, "sepoa.entraid.tenant.id", ""))
				|| isBlank(cfg(houseCode, "sepoa.entraid.client.secret", ""))) {
			Logger.err.println("EntraIdSSO", this, "Missing Entra config (client.id / tenant.id / client.secret)");
			redirectToClassicLogin(request, response, houseCode);
			return;
		}

		HttpSession session = request.getSession(true);
		session.setAttribute(EntraOidcAuth.SESSION_ATTR_HOUSE_CODE, houseCode);
		SepoaSession.putValue(request, "HOUSE_CODE", houseCode);

		new EntraOidcAuth(request, response).login();
	}

	private void handleCallback(HttpServletRequest request, HttpServletResponse response, final String normalizedCode)
			throws IOException {
		HttpSession session = request.getSession(false);
		if (session == null) {
			Logger.err.println("EntraIdSSO", this, "Callback session is null");
			redirectToClassicLogin(request, response, null);
			return;
		}
		Logger.debug.println("EntraIdSSO", this,
				"handleCallback sessionId=" + session.getId() + ", codeLen=" + (normalizedCode != null ? normalizedCode.length() : 0));

		String houseCode = (String) session.getAttribute(EntraOidcAuth.SESSION_ATTR_HOUSE_CODE);
		if (isBlank(houseCode)) {
			try {
				SepoaInfo info = SepoaSession.getAllValue(request);
				if (info != null) {
					String h = info.getSession("HOUSE_CODE");
					if (h != null && !h.trim().isEmpty()) {
						houseCode = h.trim();
					}
				}
			} catch (Exception ignored) {
			}
		}
		if (isBlank(houseCode)) {
			Logger.err.println("EntraIdSSO", this, "Missing house code on callback");
			redirectToClassicLogin(request, response, null);
			return;
		}

		SepoaSession.putValue(request, "HOUSE_CODE", houseCode);

		HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
			@Override
			public String getParameter(String name) {
				if ("code".equals(name)) {
					return normalizedCode;
				}
				return super.getParameter(name);
			}
		};

		EntraOidcAuth auth = new EntraOidcAuth(wrapped, response);
		auth.processResponse();
		Logger.debug.println("EntraIdSSO", this,
				"OIDC process done authenticated=" + auth.isAuthenticated() + ", errors=" + auth.getErrors());

		if (!auth.isAuthenticated()) {
			// 더블클릭/새로고침/프록시 재시도 등으로 동일 콜백이 재유입되면 state가 이미 소모되어
			// invalid_state가 발생할 수 있다. 이때 이미 로그인 세션이 있으면 메인으로 보낸다.
			if (auth.getErrors().contains("invalid_state") && isAlreadyLoggedIn(request)) {
				response.sendRedirect(request.getContextPath() + "/Main.sux");
				return;
			}
			Logger.err.println("EntraIdSSO", this, "OIDC failed: " + auth.getErrors());
			redirectToClassicLogin(request, response, houseCode);
			return;
		}

		String sepoaUserId = resolveSepoaUserId(houseCode, auth.getClaims());
		if (isBlank(sepoaUserId)) {
			Logger.err.println("EntraIdSSO", this, "Could not map Entra user to Sepoa ID");
			redirectToClassicLogin(request, response, houseCode);
			return;
		}
		// POA 사용자 마스터 검색은 대문자 기준으로 수행되므로, forceLogin에 전달하기 전 정규화한다.
		sepoaUserId = sepoaUserId.toUpperCase();
		Logger.debug.println("EntraIdSSO", this, "Resolved Sepoa userId=" + sepoaUserId + ", houseCode=" + houseCode);

		SepoaSession.putValue(request, "HOUSE_CODE", houseCode);
		SepoaInfo info = SepoaSession.getAllValue(request);

		Map<String, String> headerData = new HashMap<String, String>();
		headerData.put("ID", sepoaUserId);

		SepoaOut loginResult = null;
		ReqRes old = null;
		try {
			old = ThreadLocalFilter.tl.get();
			ReqRes rr = new ReqRes();
			rr.request = request;
			rr.response = response;
			ThreadLocalFilter.tl.set(rr);
			loginResult = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION",
					"forceLogin", new Object[] { headerData });
		} finally {
			if (old != null) {
				ThreadLocalFilter.tl.set(old);
			} else {
				ThreadLocalFilter.tl.remove();
			}
		}

		if (loginResult == null || loginResult.status != 1) {
			Logger.err.println("EntraIdSSO", this,
					"forceLogin failed status=" + (loginResult != null ? loginResult.status : "null"));
			redirectToClassicLogin(request, response, houseCode);
			return;
		}
		Logger.debug.println("EntraIdSSO", this, "forceLogin success userId=" + sepoaUserId);

		try {
			HttpSession cleanupSession = request.getSession(false);
			if (cleanupSession != null) {
				cleanupSession.removeAttribute(EntraOidcAuth.SESSION_ATTR_HOUSE_CODE);
			}
		} catch (IllegalStateException ise) {
			// forceLogin 내부 처리에서 세션이 무효화될 수 있으므로, 정리 실패는 무시한다.
			Logger.warn.println("EntraIdSSO", this, "Skip session cleanup: " + ise.getMessage());
		}
		response.sendRedirect(request.getContextPath() + "/Main.sux");
	}

	private String resolveSepoaUserId(String houseCode, Map<String, String> claims) {
		if (claims == null) {
			return null;
		}
		String preferred = claims.get("preferred_username");
		String email = claims.get("email");
		String upn = claims.get("upn");
		String name = claims.get("name");

		String mailLike = firstNonEmpty(preferred, email, upn);
		boolean localPart = cfgBoolean(houseCode, "sepoa.entraid.user.mapping.email.localpart", false);

		if (localPart && mailLike != null && mailLike.contains("@")) {
			return mailLike.substring(0, mailLike.indexOf('@')).trim();
		}
		if (!isBlank(mailLike)) {
			return mailLike.trim();
		}
		if (!isBlank(name)) {
			return name.trim();
		}
		return null;
	}

	private void redirectToClassicLogin(HttpServletRequest request, HttpServletResponse response, String houseCode)
			throws IOException {
		if (isBlank(houseCode)) {
			houseCode = firstNonEmpty(request.getParameter("house_code"),
					(String) request.getSession().getAttribute("HOUSE_CODE"));
		}
		if (isBlank(houseCode)) {
			Logger.err.println("EntraIdSSO", this, "redirectToClassicLogin -> /index.jsp (houseCode missing)");
			response.sendRedirect(request.getContextPath() + "/index.jsp");
			return;
		}
		String url = request.getContextPath() + "/UxDefault/Index/IndexLogin.sux?house_code="
				+ URLEncoder.encode(houseCode, "UTF-8") + "&entraid_err=1";
		Logger.err.println("EntraIdSSO", this, "redirectToClassicLogin -> " + url);
		response.sendRedirect(url);
	}

	private static String cfg(String houseCode, String key, String def) {
		try {
			return new Configuration(houseCode).getString(key, def);
		} catch (Exception e) {
			return def;
		}
	}

	private static boolean cfgBoolean(String houseCode, String key, boolean def) {
		try {
			return new Configuration(houseCode).getBoolean(key, def);
		} catch (Exception e) {
			return def;
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static String firstNonEmpty(String... vals) {
		if (vals == null) {
			return null;
		}
		for (String v : vals) {
			if (v != null && !v.trim().isEmpty()) {
				return v;
			}
		}
		return null;
	}

	private static boolean isAlreadyLoggedIn(HttpServletRequest request) {
		try {
			SepoaInfo info = SepoaSession.getAllValue(request);
			if (info == null) {
				return false;
			}
			String id = info.getSession("ID");
			return !isBlank(id);
		} catch (Exception e) {
			return false;
		}
	}
}
