package sepoa.seal.filters;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.ParamSql;
import sepoa.fw.db.SepoaConnectionResource;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.SepoaFormater;

public final class SepoaIpUrlCheckFilter implements Filter {

	private boolean logFlag;
	private boolean isVerbose = false;;
	
	private FilterConfig filterConfig = null;

	final static public String POLICY_ALLOW = "allow";
	final static public String POLICY_DENY = "deny";

	// HOUSE_CODE, IP, Pattern
	private Map<String, List<Map.Entry<IpAddressMatcher, Pattern>>> houseCodeAllowMap = null;
	private Map<String, List<Map.Entry<IpAddressMatcher, Pattern>>> houseCodeDenyMap = null;

	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("initializing... > " + this.getClass().getSimpleName());
		try {
			this.filterConfig = filterConfig;
			Config config = new Configuration();
			logFlag = config.getBoolean("sepoa.security.session.check.log", true);
			String verbose = filterConfig.getInitParameter("verbose");
			if ("true".equalsIgnoreCase(verbose)) {
				isVerbose = true;
			}
			loadDBData();
			System.out.println("initialized. > " + this.getClass().getSimpleName());
		} catch (Exception e) {
			throw new ServletException("initialize failed. > " + this.getClass().getSimpleName() + "\n", e);
		}
	}

	public void loadDBData() throws Exception {
		synchronized (this) {
			Connection conn = null;
			try {
				ConnectionContext ctx = new SepoaConnectionResource();
				conn = ctx.getConnection();
				ParamSql sm = new ParamSql("URL_FILTER", this, conn);

				StringBuffer sb = new StringBuffer();
				sb.append("SELECT HOUSE_CODE, IP, URL_REGEX, POLICY \n");
				sb.append("  FROM STCPW 				\n");
				sb.append(" WHERE DEL_FLAG IS NULL OR DEL_FLAG = 'N' \n");
				sb.append(" ORDER BY HOUSE_CODE, IP \n");
				SepoaFormater sf = new SepoaFormater(sm.doSelect(sb.toString()));

				Map<String, TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>>> houseCodeAllowTMap = new HashMap<String, TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>>>();
				Map<String, TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>>> houseCodeDenyTMap = new HashMap<String, TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>>>();

				for (int i = 0; i < sf.getRowCount(); i++) {
					String houseCode = sf.getValue("HOUSE_CODE", i);
					String ip = sf.getValue("IP", i);
					String urlRegex = sf.getValue("URL_REGEX", i);
					String policy = sf.getValue("POLICY", i);
					if (POLICY_ALLOW.equalsIgnoreCase(policy)) {
						TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>> ipAllowTMap = houseCodeAllowTMap
								.get(houseCode);
						if (ipAllowTMap == null) {
							ipAllowTMap = new TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>>();
							houseCodeAllowTMap.put(houseCode, ipAllowTMap);
						}
						IpAddressMatcher iam = new IpAddressMatcher(ip);
						Map.Entry<IpAddressMatcher, Pattern> entry = new AbstractMap.SimpleImmutableEntry<IpAddressMatcher, Pattern>(
								iam, Pattern.compile(urlRegex));
						
						String key = StringUtils.leftPad(String.valueOf(iam.getNMaskBits()), 2, '0') + iam;
						ipAllowTMap.put(key, entry);
					} else if (POLICY_DENY.equalsIgnoreCase(policy)) {
						TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>> ipDenyTMap = houseCodeAllowTMap
								.get(houseCode);
						if (ipDenyTMap == null) {
							ipDenyTMap = new TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>>();
							houseCodeDenyTMap.put(houseCode, ipDenyTMap);
						}
						IpAddressMatcher iam = new IpAddressMatcher(ip);
						Map.Entry<IpAddressMatcher, Pattern> entry = new AbstractMap.SimpleImmutableEntry<IpAddressMatcher, Pattern>(
								iam, Pattern.compile(urlRegex));

						String key = StringUtils.leftPad(String.valueOf(iam.getNMaskBits()), 2, '0') + iam;
						ipDenyTMap.put(key, entry);
					}
				}

				houseCodeAllowMap = new HashMap<String, List<Map.Entry<IpAddressMatcher, Pattern>>>();
				houseCodeDenyMap = new HashMap<String, List<Map.Entry<IpAddressMatcher, Pattern>>>();
				for (Map.Entry<String, TreeMap<String , Map.Entry<IpAddressMatcher, Pattern>>> entry : houseCodeAllowTMap
						.entrySet()) {
					TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>> tMap = entry.getValue();
					List<Map.Entry<IpAddressMatcher, Pattern>> list =  new ArrayList<>(tMap.values());
					Collections.reverse(list);
					
					houseCodeAllowMap.put(entry.getKey(), list);
				}
				for (Map.Entry<String, TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>>> entry : houseCodeDenyTMap
						.entrySet()) {
					TreeMap<String, Map.Entry<IpAddressMatcher, Pattern>> tMap = entry.getValue();
					List<Map.Entry<IpAddressMatcher, Pattern>> list =  new ArrayList<>(tMap.values());
					Collections.reverse(list);
					
					houseCodeDenyMap.put(entry.getKey(), list);
				}
			} finally {
				DbUtils.closeQuietly(conn);
			}
		}
	}

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String houseCode = null;
		boolean isAllowed = false;
		boolean isDenied = false;
		String clientIP = null;
		String page = null;
		int nMaskBits = -1;

		try {
			HttpServletRequest httpReq = (HttpServletRequest) request;
			HttpServletResponse httpRes = (HttpServletResponse) response;
			SepoaInfo info = sepoa.fw.ses.SepoaSession.getAllValue(httpReq);
			houseCode = info.getSession("HOUSE_CODE");
			if (StringUtils.isBlank(houseCode)) {
				houseCode = "000";
			}
			List<Map.Entry<IpAddressMatcher, Pattern>> ipAllowList = null;
			List<Map.Entry<IpAddressMatcher, Pattern>> ipDenyList = null;
			ipAllowList = houseCodeAllowMap.get(houseCode);
			ipDenyList = houseCodeDenyMap.get(houseCode);

			clientIP = getClientIP(httpReq);
			page = httpReq.getRequestURI().substring(httpReq.getContextPath().length());

			if (ipAllowList != null) {
				for (Map.Entry<IpAddressMatcher, Pattern> entry : ipAllowList) {
					if (entry.getKey().matches(clientIP)) {
						if (entry.getValue().matcher(page).find()) {
							isAllowed = true;
							nMaskBits = entry.getKey().getNMaskBits();
							break;
						}
					}
				}
			}

			if (isAllowed && ipDenyList != null) {
				for (Map.Entry<IpAddressMatcher, Pattern> entry : ipDenyList) {
					if (entry.getKey().matches(clientIP)) {
						if (nMaskBits > entry.getKey().getNMaskBits()) {
							continue;
						}
						if (entry.getValue().matcher(page).find()) {
							isDenied = true;
							break;
						}
					}
				}
			}

			// Allow / Deny 양쪽 모두 세팅되지 않은 경우라면 Deny from all.
			if (!isAllowed || isDenied) {

				if ("postConnection".equals(httpReq.getParameter("requestMode"))) {
					if (logFlag) {
						System.out.println("Access Denied. postConnection --> [HOUSE_CODE : " + houseCode + ", URL : "
								+ page + ", IP : " + clientIP + "]");
						Logger.debug.println("Access Denied. postConnection --> [HOUSE_CODE : " + houseCode + ", URL : "
								+ page + ", IP : " + clientIP + "]");
					}
					StringBuffer sb = new StringBuffer();
					sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					sb.append("<rows>");
					sb.append("<userdata name=\"message\">Access Denied.</userdata>");
					sb.append("<userdata name=\"data_type\">doSelect</userdata>");
					sb.append("<userdata name=\"status\">SESSIONCLOSE</userdata>");
					sb.append("<userdata name=\"mode\">query</userdata>");
					sb.append("</rows>");
					getOutputStream(httpRes, sb);
				} else if ("postTransaction".equals(httpReq.getParameter("requestMode"))) {
					if (logFlag) {
						System.out.println("Access Denied. postTransaction --> [HOUSE_CODE : " + houseCode + ", URL : "
								+ page + ", IP : " + clientIP + "]");
						Logger.debug.println("Access Denied. postTransaction --> [HOUSE_CODE : " + houseCode
								+ ", URL : " + page + ", IP : " + clientIP + "]");
					}
					StringBuffer sb = new StringBuffer();
					sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					sb.append("<data><action type=\"doSaveEnd\" ");
					sb.append(" message = \"Access Denied.\"  ");
					sb.append(
							" data_type = \"doData\"  status = \"SESSIONCLOSE\"  mode = \"insert\" ></action></data>");
					getOutputStream(httpRes, sb);
				} else if ("doServiceAjax".equals(httpReq.getParameter("requestMode"))) {
					if (logFlag) {
						System.out.println("Access Denied. doServiceAjax --> [HOUSE_CODE : " + houseCode + ", URL : "
								+ page + ", IP : " + clientIP + "]");
						Logger.debug.println("Access Denied. doServiceAjax --> [HOUSE_CODE : " + houseCode + ", URL : "
								+ page + ", IP : " + clientIP + "]");
					}
					StringBuffer sb = new StringBuffer();
					sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					sb.append("<SepoaOut>");
					sb.append("<status>ACCESS_DENIED</status>");
					sb.append("<message>Access Denied</message>");
					sb.append("</SepoaOut>");
					getOutputStream(httpRes, sb);
				} else if ("Other".equals(httpReq.getParameter("requestMode"))) {
					if (logFlag) {
						System.out.println("Access Denied. Other --> [HOUSE_CODE : " + houseCode + ", URL : " + page
								+ ", IP : " + clientIP + "]");
						Logger.debug.println("Access Denied. Other --> [HOUSE_CODE : " + houseCode + ", URL : " + page
								+ ", IP : " + clientIP + "]");
					}
					StringBuffer sb = new StringBuffer();
					sb.append("ACCESS_DENIED");
					getOutputStream(httpRes, sb);

				} else {
					if (logFlag) {
						System.out.println("Access Denied. Unknown --> [HOUSE_CODE : " + houseCode + ", URL : " + page
								+ ", IP : " + clientIP + "]");
						Logger.debug.println("Access Denied. Unknown --> [HOUSE_CODE : " + houseCode + ", URL : " + page
								+ ", IP : " + clientIP + "]");
					}
					StringBuffer sb = new StringBuffer();
					sb.append("ACCESS_DENIED");
					getOutputStream(httpRes, sb);
					ServletContext sc = filterConfig.getServletContext();
					sc.getRequestDispatcher("/UxDefault/Error/AccessDenied.sux").forward(request, response);
					return;
				}
			} else {
				String reload = httpReq.getParameter("reload");
				if ("SepoaIpUrlCheckFilter".equals(reload)) {
					loadDBData();
				} else {
					chain.doFilter(request, response);
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
			throw new ServletException(t);
		} finally {
			if (isVerbose) {
				Logger.debug.print(
						"[HOUSE_CODE : " + houseCode + ", URL : " + page + ", IP : " + clientIP + "] - isAllowed : "
								+ isAllowed + ", isDenied : " + isDenied + ", isBlocked : " + (!isAllowed || isDenied));
			}
		}

	}

	private void getOutputStream(HttpServletResponse httpRes, StringBuffer sb) throws IOException {

		httpRes.setContentType("text/xml;charset=UTF-8");
		httpRes.setHeader("Content-Length", "" + sb.toString().getBytes("UTF-8").length);

		StringReader sr_xml = new StringReader(sb.toString());
		Writer out = httpRes.getWriter();
		char[] cbuf = new char[1024];
		int leng = 0;

		while ((leng = sr_xml.read(cbuf)) > 0) {
			out.write(cbuf, 0, leng);
		}
		out.flush();
		out.close();
		sr_xml.close();
	}

	public static String getClientIP(HttpServletRequest req) {
		String ip = null;
		try {
			String[] headerKeyArray = new String[] { "X-Forwarded-For", "Proxy-Client-ip", "WL-Proxy-Client-ip",
					"HTTP_CLIENT_ip", "HTTP_X_FORWARDED_FOR", "X-Real-ip", "X-Realip" };

			for (String headerKey : headerKeyArray) {
				ip = req.getHeader(headerKey);
				if (!StringUtils.isBlank(headerKey)) {
					break;
				}
			}

			if (StringUtils.isBlank(ip)) {
				ip = req.getRemoteAddr();
			}
		} catch (Exception e) {
			Logger.err.println(e);
		}
		if (StringUtils.isBlank(ip)) {
			ip = "0.0.0.0";
		}
		return ip;
	}
}
