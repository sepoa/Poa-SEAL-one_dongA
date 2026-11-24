package sepoa.svc.login.cbo_006;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import filters.ThreadLocalFilter;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;

@SuppressWarnings({ "unchecked" })
@Description("LoginSSO")
public class LoginSSO extends SepoaService {

	public LoginSSO(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("SSO 로그인")
	public SepoaOut loginSSO(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaOut out = null;
		try {
			Config conf = new Configuration();
			String KISED_SSO_URL = conf.getString("kised.sso.url");

			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String ssoToken = headerData.get("token");
			String ip = filters.ThreadLocalFilter.getClientIP();

			String id = null;
			String urlString = KISED_SSO_URL + "/sso/api/getUsrData.do?token=" + ssoToken + "&clientIp=" + ip;
			Logger.debug.println("url : " + urlString);
			URL url = new URL(urlString);
			if (url.getProtocol().equals("https")) {
				ignoreSsl();
			}
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			con.setReadTimeout(30 * 1000); // InputStream 읽어 오는 Timeout 시간 설정
			con.addRequestProperty("Accept-Charset", "UTF-8"); // Charset 설정
			con.addRequestProperty("Content-Type", "application/json"); // type설정
			con.setRequestMethod("GET"); // 요청방식 설정
			con.setDoOutput(false); // URLConnection이 서버에 데이터를 보낼 것인지 여부 설정(기본값은 false)

			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) { // 서버가 정상적으로 응답했다면
				InputStream is = con.getInputStream();
				String response = IOUtils.toString(is, "UTF-8");
				IOUtils.closeQuietly(is);
				
				Logger.debug.println("response : " + response);

				Map<String, Object> map = new ObjectMapper().readValue(response, new TypeReference<Map>() {
				});

				if (map == null) {
					throw new Exception("SSO 서비스 오류 : map is null");
				}
				if (!"success".equals(map.get("status"))) {
					throw new Exception("SSO 서비스 오류 : " + map.get("messageParams"));
				}
				Map<String, Object> objectMap = (Map<String, Object>) map.get("objects");
				if (objectMap == null) {
					throw new Exception("SSO 리턴형식 오류 : object doesn't exists in response");
				}
				Map<String, String> userInfoMap = (Map<String, String>) objectMap.get("userInfo");
				if (userInfoMap == null) {
					throw new Exception("SSO 리턴형식 오류 : userInfo doesn't exists in response");
				}

				id = userInfoMap.get("usrId");
//				id = userInfoMap.get("empNo");
				if (id == null) {
					throw new Exception("SSO 리턴형식 오류 : userInfo doesn't exists in response");
				}

			} else { // 정상적인 응답 코드가 아니라면 실행되는 예외처리..?
				throw new Exception("SSO 서비스 통신에 실패하였습니다. " + con.getResponseCode() + " 오류");
			}

			headerData.put("ID", id);

			out = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION",
					"forceLogin", new Object[] { headerData });

			Map<String, String> rawHeaderData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
			HttpServletRequest request = ThreadLocalFilter.tl.get().request;
			SepoaSession.putValue(request, "ssoToken", rawHeaderData.get("ssoToken"));
			Logger.debug.println("ssoToken setted : " + rawHeaderData.get("ssoToken"));

			System.out.println(out.flag);
			System.out.println(out.message);

		} catch (Exception e) {
			// e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return out != null ? out : getSepoaOut();
	}

	public static void ignoreSsl() throws Exception {
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				return true;
			}
		};
		trustAllHttpsCertificates();
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}

	private static void trustAllHttpsCertificates() throws Exception {
		TrustManager[] trustAllCerts = new TrustManager[1];
		TrustManager tm = new miTM();
		trustAllCerts[0] = tm;
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	static class miTM implements TrustManager, X509TrustManager {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public boolean isServerTrusted(X509Certificate[] certs) {
			return true;
		}

		public boolean isClientTrusted(X509Certificate[] certs) {
			return true;
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
			return;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
			return;
		}
	}
}
