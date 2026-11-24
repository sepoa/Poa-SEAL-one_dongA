package com.onelogin.oidc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWKSecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;

/**
 * Microsoft Entra ID OIDC (PKCE S256 + ID 토큰 JWKS 서명 검증).
 * 고객 예제(3-Java_EntraID_Build_1.8)와 동일한 검증 로직.
 * 설정은 {@code sepoa.entraid.*} (Sepoa {@link Configuration})에서 로드한다.
 */
public class EntraOidcAuth {

	private static final long DEFAULT_CLOCK_SKEW_SECONDS = 300L;
	private static final int DEFAULT_HTTP_CONNECT_TIMEOUT_MS = 5000;
	private static final int DEFAULT_HTTP_READ_TIMEOUT_MS = 5000;

	public static final String SESSION_ATTR_HOUSE_CODE = "entraid_oauth_house_code";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static volatile Discovery cachedDiscovery;
	private static volatile long discoveryFetchedAtMillis = 0L;
	private static final long DISCOVERY_CACHE_TTL_MILLIS = 60L * 60L * 1000L;
	private static volatile JWKSource<JWKSecurityContext> cachedJwkSource;
	private static volatile String cachedJwksUri;
	private static volatile ResourceRetriever cachedResourceRetriever;

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final Properties config;

	private boolean authenticated;
	private final List<String> errors = new ArrayList<String>();
	private Map<String, String> claims = new LinkedHashMap<String, String>();

	private String idToken;

	public EntraOidcAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
		this.request = request;
		this.response = response;
		this.config = loadConfig(request);
	}

	public void login() throws IOException {
		errors.clear();
		authenticated = false;

		String tenantId = requireConfig("onelogin.oidc.tenant_id");
		String clientId = requireConfig("onelogin.oidc.client_id");
		String redirectUri = resolveUri(requireConfig("onelogin.oidc.redirect_uri"));
		String scopes = getConfig("onelogin.oidc.scopes", "openid profile email");

		String state = randomUrlSafe(32);
		String nonce = randomUrlSafe(32);

		String codeVerifier = randomUrlSafe(64);
		String codeChallenge;
		try {
			codeChallenge = sha256Base64Url(codeVerifier);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}

		request.getSession().setAttribute("oidc_state", state);
		request.getSession().setAttribute("oidc_nonce", nonce);
		request.getSession().setAttribute("oidc_code_verifier", codeVerifier);

		Map<String, String> query = new LinkedHashMap<String, String>();
		query.put("client_id", clientId);
		query.put("response_type", "code");
		query.put("redirect_uri", redirectUri);
		query.put("response_mode", "query");
		query.put("scope", scopes);
		query.put("state", state);
		query.put("nonce", nonce);
		query.put("code_challenge", codeChallenge);
		query.put("code_challenge_method", "S256");

		String authorizeUrl = authorityBase(tenantId) + "/oauth2/v2.0/authorize?" + buildQuery(query);
		Logger.debug.println("EntraOidcAuth", this,
				"login redirect authorizeUrl=" + authorizeUrl + ", houseCode=" + resolveHouseCode(request));
		response.sendRedirect(authorizeUrl);
	}

	public void processResponse() throws IOException {
		errors.clear();
		authenticated = false;
		claims = new LinkedHashMap<String, String>();
		idToken = null;

		javax.servlet.http.HttpSession session = request.getSession(false);
		if (session == null) {
			errors.add("missing_session");
			Logger.err.println("EntraOidcAuth", this, "processResponse failed: missing_session");
			return;
		}

		String error = request.getParameter("error");
		if (error != null && !error.isEmpty()) {
			errors.add(error);
			String errorDescription = request.getParameter("error_description");
			if (errorDescription != null && !errorDescription.isEmpty()) {
				errors.add(errorDescription);
			}
			Logger.err.println("EntraOidcAuth", this, "OAuth callback error=" + error + ", desc=" + errorDescription);
			// OAuth error로 조기 종료되는 경우에도 재사용/오염 방지를 위해 임시값을 정리한다.
			session.removeAttribute("oidc_code_verifier");
			session.removeAttribute("oidc_nonce");
			session.removeAttribute("oidc_state");
			return;
		}

		try {
			String code = request.getParameter("code");
			String returnedState = normalizeState(request.getParameter("state"));

			if (code == null || code.isEmpty()) {
				errors.add("missing_code");
				Logger.err.println("EntraOidcAuth", this, "processResponse failed: missing_code");
				return;
			}

			String expectedState = normalizeState((String) session.getAttribute("oidc_state"));
			if (expectedState == null || returnedState == null || !expectedState.equals(returnedState)) {
				errors.add("invalid_state");
				Logger.err.println("EntraOidcAuth", this,
						"processResponse failed: invalid_state expected=" + safe(expectedState) + ", returned=" + safe(returnedState));
				return;
			}

			// state는 1회성으로 사용 후 정리한다.
			session.removeAttribute("oidc_state");

			String tenantId = requireConfig("onelogin.oidc.tenant_id");
			String clientId = requireConfig("onelogin.oidc.client_id");
			String clientSecret = requireConfig("onelogin.oidc.client_secret");
			String redirectUri = resolveUri(requireConfig("onelogin.oidc.redirect_uri"));
			String scopes = getConfig("onelogin.oidc.scopes", "openid profile email");

			String codeVerifier = (String) session.getAttribute("oidc_code_verifier");

			Map<String, String> form = new LinkedHashMap<String, String>();
			form.put("client_id", clientId);
			form.put("client_secret", clientSecret);
			form.put("grant_type", "authorization_code");
			form.put("code", code);
			form.put("redirect_uri", redirectUri);
			form.put("scope", scopes);
			if (codeVerifier != null && !codeVerifier.isEmpty()) {
				form.put("code_verifier", codeVerifier);
			}

			String tokenUrl = authorityBase(tenantId) + "/oauth2/v2.0/token";
			Logger.debug.println("EntraOidcAuth", this,
					"Token exchange start tokenUrl=" + tokenUrl + ", redirectUri=" + redirectUri + ", codeVerifier=" + !isBlank(codeVerifier));
			String tokenResponse = httpPostForm(tokenUrl, form);
			String extractedIdToken = extractTokenField(tokenResponse, "id_token");

			if (extractedIdToken == null || extractedIdToken.isEmpty()) {
				errors.add("missing_id_token");
				Logger.err.println("EntraOidcAuth", this, "processResponse failed: missing_id_token");
				return;
			}
			Logger.debug.println("EntraOidcAuth", this, "Token exchange success idTokenLen=" + extractedIdToken.length());

			this.idToken = extractedIdToken;

			String expectedNonce = (String) session.getAttribute("oidc_nonce");
			Map<String, String> tokenClaims;
			try {
				tokenClaims = validateAndDecodeIdToken(extractedIdToken, tenantId, clientId, expectedNonce);
			} catch (IllegalArgumentException e) {
				errors.add(e.getMessage() == null ? "invalid_id_token" : e.getMessage());
				Logger.err.println("EntraOidcAuth", this, "ID token validation failed: " + e.getMessage());
				return;
			} catch (Exception e) {
				errors.add("invalid_id_token");
				Logger.err.println("EntraOidcAuth", this, "ID token validation failed: invalid_id_token");
				return;
			}

			this.claims = tokenClaims;
			authenticated = true;

			String username = firstNonEmpty(
					tokenClaims.get("name"),
					tokenClaims.get("preferred_username"),
					tokenClaims.get("upn"),
					tokenClaims.get("unique_name"),
					tokenClaims.get("email")
			);
			String mail = firstNonEmpty(tokenClaims.get("email"), tokenClaims.get("preferred_username"), tokenClaims.get("upn"));
			String loginId = firstNonEmpty(tokenClaims.get("oid"), tokenClaims.get("sub"), tokenClaims.get("preferred_username"));

			session.setAttribute("username", username);
			session.setAttribute("mail", mail);
			session.setAttribute("loginId", loginId);
			session.setAttribute("oidc_claims", new LinkedHashMap<String, String>(tokenClaims));
			session.setAttribute("oidc_id_token", extractedIdToken);
			Logger.debug.println("EntraOidcAuth", this,
					"processResponse success claimsKeys=" + tokenClaims.keySet());
		} finally {
			// 재사용/오염 방지를 위해 state 관련 임시 값 정리
			session.removeAttribute("oidc_code_verifier");
			session.removeAttribute("oidc_nonce");
			session.removeAttribute("oidc_state");
		}
	}

	public void logout() throws IOException {
		String tenantId = requireConfig("onelogin.oidc.tenant_id");
		String postLogoutRedirectUri = resolveUri(getConfig("onelogin.oidc.post_logout_redirect_uri", null));

		String existingIdToken = (String) request.getSession().getAttribute("oidc_id_token");
		request.getSession().invalidate();

		Map<String, String> query = new LinkedHashMap<String, String>();
		if (existingIdToken != null && !existingIdToken.isEmpty()) {
			query.put("id_token_hint", existingIdToken);
		}
		if (postLogoutRedirectUri != null && !postLogoutRedirectUri.isEmpty()) {
			query.put("post_logout_redirect_uri", postLogoutRedirectUri);
		}

		String logoutUrl = authorityBase(tenantId) + "/oauth2/v2.0/logout";
		if (!query.isEmpty()) {
			logoutUrl += "?" + buildQuery(query);
		}
		response.sendRedirect(logoutUrl);
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public List<String> getErrors() {
		return Collections.unmodifiableList(errors);
	}

	public Map<String, String> getClaims() {
		return Collections.unmodifiableMap(claims);
	}

	public String getIdToken() {
		return idToken;
	}

	private String authorityBase(String tenantId) {
		String authorityHost = getConfig("onelogin.oidc.authority_host", "https://login.microsoftonline.com");
		if (authorityHost.endsWith("/")) {
			authorityHost = authorityHost.substring(0, authorityHost.length() - 1);
		}
		return authorityHost + "/" + tenantId;
	}

	private String requireConfig(String key) throws IOException {
		String value = getConfig(key, null);
		if (value == null || value.trim().isEmpty()) {
			throw new IOException("Missing config: " + key);
		}
		return value.trim();
	}

	private String getConfig(String key, String defaultValue) {
		String value = config.getProperty(key);
		if (value == null) {
			return defaultValue;
		}
		value = value.trim();
		if (value.isEmpty()) {
			return defaultValue;
		}
		return value;
	}

	private String resolveUri(String configured) {
		if (configured == null) {
			return null;
		}
		String value = configured.trim();
		if (value.isEmpty()) {
			return null;
		}
		String lower = value.toLowerCase();
		if (lower.startsWith("http://") || lower.startsWith("https://")) {
			return value;
		}
		String path = value;
		if (!path.startsWith("/")) {
			path = "/" + path;
		}

		String scheme = request.getScheme();
		String host = request.getServerName();
		int port = request.getServerPort();
		String contextPath = request.getContextPath();
		if (contextPath == null) {
			contextPath = "";
		}

		StringBuilder base = new StringBuilder();
		base.append(scheme).append("://").append(host);
		if (!(port == 80 && "http".equalsIgnoreCase(scheme)) && !(port == 443 && "https".equalsIgnoreCase(scheme))) {
			base.append(":").append(port);
		}
		base.append(contextPath);

		return base.toString() + path;
	}

	/**
	 * {@code sepoa.entraid.*} → 예제와 동일한 내부 키로 매핑.
	 */
	private static Properties loadConfig(HttpServletRequest request) throws IOException {
		String houseCode = resolveHouseCode(request);
		if (houseCode == null || houseCode.trim().isEmpty()) {
			throw new IOException("Missing HOUSE_CODE for Entra OIDC config");
		}
		houseCode = houseCode.trim();

		Configuration cfg;
		try {
			cfg = new Configuration(houseCode);
		} catch (Exception e) {
			throw new IOException("Cannot load Sepoa configuration for house: " + houseCode, e);
		}

		Properties props = new Properties();
		props.setProperty("onelogin.oidc.authority_host",
				cfg.getString("sepoa.entraid.authority.host", "https://login.microsoftonline.com"));
		props.setProperty("onelogin.oidc.tenant_id", cfg.getString("sepoa.entraid.tenant.id", ""));
		props.setProperty("onelogin.oidc.client_id", cfg.getString("sepoa.entraid.client.id", ""));
		props.setProperty("onelogin.oidc.client_secret", cfg.getString("sepoa.entraid.client.secret", ""));
		String redirect = cfg.getString("sepoa.entraid.redirect.uri", "").trim();
		if (redirect.isEmpty()) {
			redirect = "/entraIdSSO";
		}
		props.setProperty("onelogin.oidc.redirect_uri", redirect);
		props.setProperty("onelogin.oidc.scopes", cfg.getString("sepoa.entraid.scope", "openid profile email"));
		String postLogout = cfg.getString("sepoa.entraid.post.logout.redirect.uri", "").trim();
		if (!postLogout.isEmpty()) {
			props.setProperty("onelogin.oidc.post_logout_redirect_uri", postLogout);
		}
		return props;
	}

	private static String resolveHouseCode(HttpServletRequest request) {
		try {
			SepoaInfo info = SepoaSession.getAllValue(request);
			if (info != null) {
				String h = info.getSession("HOUSE_CODE");
				if (h != null && !h.trim().isEmpty()) {
					return h.trim();
				}
			}
		} catch (Exception ignored) {
		}
		HttpSession s = request.getSession(false);
		if (s != null) {
			Object o = s.getAttribute(SESSION_ATTR_HOUSE_CODE);
			if (o instanceof String && !((String) o).trim().isEmpty()) {
				return ((String) o).trim();
			}
		}
		return null;
	}

	private static String httpPostForm(String url, Map<String, String> form) throws IOException {
		byte[] body = buildQuery(form).getBytes(StandardCharsets.UTF_8);

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Accept", "application/json");

		try (OutputStream os = conn.getOutputStream()) {
			os.write(body);
		}

		int status = conn.getResponseCode();
		InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
		String responseBody = readAll(is);

		if (status < 200 || status >= 300) {
			throw new IOException("Token request failed: HTTP " + status + " - " + responseBody);
		}

		return responseBody;
	}

	private static String readAll(InputStream is) throws IOException {
		if (is == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		}
		return sb.toString();
	}

	private static String buildQuery(Map<String, String> params) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : params.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(urlEncode(e.getKey()));
			sb.append('=');
			sb.append(urlEncode(e.getValue()));
		}
		return sb.toString();
	}

	private static String urlEncode(String s) throws IOException {
		if (s == null) {
			return "";
		}
		// URLEncoder는 공백을 '+'로 변환하므로 RFC 3986 스타일로 보정한다.
		return URLEncoder.encode(s, "UTF-8")
				.replace("+", "%20")
				.replace("*", "%2A")
				.replace("%7E", "~");
	}

	private static String normalizeState(String state) {
		if (state == null) {
			return null;
		}
		return state.trim().replaceAll("[\\r\\n\\t]", "");
	}

	private static String randomUrlSafe(int bytes) {
		byte[] buffer = new byte[bytes];
		new SecureRandom().nextBytes(buffer);
		return base64UrlEncode(buffer);
	}

	private static String sha256Base64Url(String value) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(value.getBytes(StandardCharsets.US_ASCII));
		return base64UrlEncode(hash);
	}

	private static String base64UrlEncode(byte[] input) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
	}

	private Map<String, String> validateAndDecodeIdToken(String jwt, String tenantId, String clientId, String expectedNonce) throws Exception {
		if (expectedNonce == null || expectedNonce.trim().isEmpty()) {
			throw new IllegalArgumentException("missing_expected_nonce");
		}

		SignedJWT signedJWT;
		try {
			signedJWT = SignedJWT.parse(jwt);
		} catch (Exception e) {
			throw new IllegalArgumentException("invalid_id_token");
		}

		JWSHeader header = signedJWT.getHeader();
		if (header.getAlgorithm() == null || !JWSAlgorithm.RS256.equals(header.getAlgorithm())) {
			throw new IllegalArgumentException("invalid_id_token_alg");
		}
		String kid = header.getKeyID();
		if (kid == null || kid.trim().isEmpty()) {
			throw new IllegalArgumentException("missing_kid");
		}

		Discovery discovery = getDiscovery(tenantId);
		JWKSource<JWKSecurityContext> jwkSource = getOrCreateJwkSource(discovery.jwksUri);
		RSAKey rsaKey;
		try {
			rsaKey = resolveRsaKey(jwkSource, header);
		} catch (IllegalArgumentException e) {
			if (!"unknown_kid".equals(e.getMessage())) {
				throw e;
			}
			jwkSource = forceRefreshJwkSource(discovery.jwksUri);
			rsaKey = resolveRsaKey(jwkSource, header);
		}
		boolean signatureValid;
		try {
			signatureValid = signedJWT.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()));
		} catch (JOSEException e) {
			throw new IllegalArgumentException("invalid_id_token_signature");
		}
		if (!signatureValid) {
			throw new IllegalArgumentException("invalid_id_token_signature");
		}

		JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
		validateStandardClaims(claimsSet, discovery.issuer, clientId, expectedNonce);
		return toFlatStringMap(claimsSet);
	}

	private static void validateStandardClaims(JWTClaimsSet claims, String expectedIssuer, String clientId, String expectedNonce) {
		String iss = claims.getIssuer();
		if (expectedIssuer == null || expectedIssuer.trim().isEmpty()) {
			throw new IllegalArgumentException("missing_expected_issuer");
		}
		if (iss == null || iss.trim().isEmpty()) {
			throw new IllegalArgumentException("missing_issuer");
		}
		if (!expectedIssuer.equals(iss)) {
			throw new IllegalArgumentException("invalid_issuer");
		}

		List<String> aud = claims.getAudience();
		if (aud == null || aud.isEmpty() || clientId == null || clientId.trim().isEmpty() || !aud.contains(clientId)) {
			throw new IllegalArgumentException("invalid_audience");
		}

		Date now = new Date();
		Date exp = claims.getExpirationTime();
		if (exp == null) {
			throw new IllegalArgumentException("missing_exp");
		}
		if (isAfterWithSkew(now, exp, DEFAULT_CLOCK_SKEW_SECONDS)) {
			throw new IllegalArgumentException("token_expired");
		}

		Date nbf = claims.getNotBeforeTime();
		if (nbf != null && isBeforeWithSkew(now, nbf, DEFAULT_CLOCK_SKEW_SECONDS)) {
			throw new IllegalArgumentException("token_not_yet_valid");
		}

		Object nonceObj = claims.getClaim("nonce");
		String nonce = (nonceObj instanceof String) ? (String) nonceObj : null;
		if (nonce == null || nonce.trim().isEmpty()) {
			throw new IllegalArgumentException("missing_nonce");
		}
		if (!expectedNonce.equals(nonce)) {
			throw new IllegalArgumentException("invalid_nonce");
		}
	}

	private static boolean isAfterWithSkew(Date now, Date time, long skewSeconds) {
		return now.getTime() - (skewSeconds * 1000L) >= time.getTime();
	}

	private static boolean isBeforeWithSkew(Date now, Date time, long skewSeconds) {
		return now.getTime() + (skewSeconds * 1000L) < time.getTime();
	}

	private static Map<String, String> toFlatStringMap(JWTClaimsSet claimsSet) {
		Map<String, Object> raw = claimsSet.getClaims();
		Map<String, String> out = new LinkedHashMap<String, String>();
		for (Map.Entry<String, Object> e : raw.entrySet()) {
			Object v = e.getValue();
			out.put(e.getKey(), v == null ? null : String.valueOf(v));
		}
		return out;
	}

	private static final class Discovery {
		final String issuer;
		final String jwksUri;
		Discovery(String issuer, String jwksUri) {
			this.issuer = issuer;
			this.jwksUri = jwksUri;
		}
	}

	private Discovery getDiscovery(String tenantId) throws IOException {
		long now = System.currentTimeMillis();
		Discovery existing = cachedDiscovery;
		if (existing != null && (now - discoveryFetchedAtMillis) < DISCOVERY_CACHE_TTL_MILLIS) {
			return existing;
		}

		String authorityHost = getConfig("onelogin.oidc.authority_host", "https://login.microsoftonline.com");
		if (authorityHost.endsWith("/")) {
			authorityHost = authorityHost.substring(0, authorityHost.length() - 1);
		}
		String discoveryUrl = authorityHost + "/" + tenantId + "/v2.0/.well-known/openid-configuration";
		String discoveryJson = httpGetJson(discoveryUrl);
		Map<String, Object> doc = parseJsonObject(discoveryJson);
		String issuer = asString(doc.get("issuer"));
		String jwksUri = asString(doc.get("jwks_uri"));
		if (issuer == null || issuer.trim().isEmpty() || jwksUri == null || jwksUri.trim().isEmpty()) {
			throw new IOException("Invalid discovery document");
		}

		Discovery d = new Discovery(issuer, jwksUri);
		cachedDiscovery = d;
		discoveryFetchedAtMillis = now;
		return d;
	}

	private static synchronized JWKSource<JWKSecurityContext> getOrCreateJwkSource(String jwksUri) throws IOException {
		if (jwksUri == null || jwksUri.trim().isEmpty()) {
			throw new IOException("Missing jwks_uri");
		}
		if (cachedJwkSource != null && jwksUri.equals(cachedJwksUri)) {
			return cachedJwkSource;
		}
		ResourceRetriever rr = new DefaultResourceRetriever(DEFAULT_HTTP_CONNECT_TIMEOUT_MS, DEFAULT_HTTP_READ_TIMEOUT_MS);
		cachedResourceRetriever = rr;
		RemoteJWKSet<JWKSecurityContext> remote = new RemoteJWKSet<JWKSecurityContext>(new URL(jwksUri), rr);
		cachedJwkSource = remote;
		cachedJwksUri = jwksUri;
		return remote;
	}

	private static synchronized JWKSource<JWKSecurityContext> forceRefreshJwkSource(String jwksUri) throws IOException {
		ResourceRetriever rr = cachedResourceRetriever;
		if (rr == null) {
			rr = new DefaultResourceRetriever(DEFAULT_HTTP_CONNECT_TIMEOUT_MS, DEFAULT_HTTP_READ_TIMEOUT_MS);
			cachedResourceRetriever = rr;
		}
		RemoteJWKSet<JWKSecurityContext> remote = new RemoteJWKSet<JWKSecurityContext>(new URL(jwksUri), rr);
		cachedJwkSource = remote;
		cachedJwksUri = jwksUri;
		return remote;
	}

	private static RSAKey resolveRsaKey(JWKSource<JWKSecurityContext> jwkSource, JWSHeader header) throws Exception {
		JWKMatcher matcher = new JWKMatcher.Builder()
				.keyType(KeyType.RSA)
				.keyUse(KeyUse.SIGNATURE)
				.keyID(header.getKeyID())
				.build();
		JWKSelector selector = new JWKSelector(matcher);
		List<JWK> jwks = jwkSource.get(selector, null);
		if (jwks != null && !jwks.isEmpty()) {
			JWK jwk = jwks.get(0);
			if (jwk instanceof RSAKey) {
				return (RSAKey) jwk;
			}
		}
		throw new IllegalArgumentException("unknown_kid");
	}

	private static String extractTokenField(String json, String field) throws IOException {
		Map<String, Object> obj = parseJsonObject(json);
		return asString(obj.get(field));
	}

	private static Map<String, Object> parseJsonObject(String json) throws IOException {
		if (json == null) {
			return new LinkedHashMap<String, Object>();
		}
		return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() { });
	}

	private static String asString(Object o) {
		return o == null ? null : String.valueOf(o);
	}

	private static String httpGetJson(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("GET");
		conn.setInstanceFollowRedirects(false);
		conn.setRequestProperty("Accept", "application/json");
		conn.setConnectTimeout(DEFAULT_HTTP_CONNECT_TIMEOUT_MS);
		conn.setReadTimeout(DEFAULT_HTTP_READ_TIMEOUT_MS);
		int status = conn.getResponseCode();
		InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
		String responseBody = readAll(is);
		if (status < 200 || status >= 300) {
			throw new IOException("HTTP " + status + " - " + responseBody);
		}
		return responseBody;
	}

	private static String firstNonEmpty(String... values) {
		if (values == null) {
			return null;
		}
		for (String v : values) {
			if (v != null && !v.trim().isEmpty()) {
				return v;
			}
		}
		return null;
	}

	private static boolean isBlank(String v) {
		return v == null || v.trim().isEmpty();
	}

	private static String safe(String v) {
		return v == null ? "null" : v;
	}
}
