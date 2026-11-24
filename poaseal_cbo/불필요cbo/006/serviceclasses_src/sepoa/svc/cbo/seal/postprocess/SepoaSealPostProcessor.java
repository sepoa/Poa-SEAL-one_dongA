package sepoa.svc.cbo.seal.postprocess;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("전자인장 - 날인요청 모듈 후처리")
public class SepoaSealPostProcessor extends SepoaService {

	public SepoaSealPostProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("결재진행 - 승인")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalProgress(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser(this, "searchNextAppLine");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));

			String title = "[전자인장시스템] " + Cache.text.get("MAIL.APPROVAL_PROGRESS");
			String content = "[전자인장시스템] " + sf.getValue("NAME", 0) + "님, " + Cache.text.get("MAIL.APP_NO") + " : "
					+ headerData.get("APP_NO") + "의 결재요청이 도착하였습니다.";

			sendMessenger(sf.getValue("ID", 0), "결재승인", title, content);

		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}

	@Description("결재진행 - 완료")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalConfirmReady(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser(this, "searchApprovalWriter");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));

			String title = "[전자인장시스템] " + Cache.text.get("MAIL.APPROVAL_COMPLETE");
			String content = "[전자인장시스템] " + sf.getValue("APP_USER_NAME", 0) + "님, " + Cache.text.get("MAIL.APP_NO")
					+ " : " + headerData.get("APP_NO") + "의 결재가 완료되었습니다.";

			sendMessenger(sf.getValue("ADD_USER_ID", 0), "결재승인", title, content);
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}

	@Description("외부 인터페이스 날인요청 서비스")
	public SepoaOut saveExternalReqSealData(Map<String, String> data) {
		setStatus(1);
		setFlag(true);

		try {
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser(this, "saveExternalReqSealData");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(data);

			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}

	@SuppressWarnings("unchecked")
	@Description("다운로드 후처리")
	public SepoaOut doDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			ConnectionContext ctx = getConnectionContext();

			SepoaXmlParser sxp = new SepoaXmlParser(this, "doSelectExternalReqSealData");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() > 0) {
				String keyValue = sf.getValue("KEY_VALUE", 0);
				String objCode = sf.getValue("OBJ_CODE", 0);
				if (StringUtils.isNotBlank(keyValue) && StringUtils.isNotBlank(objCode)) {
					sendDownloadCallback(keyValue, objCode);
				}
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}

	@Description("메신저발송")
	public void sendMessenger(String id, String type, String title, String content) {
		HttpURLConnection con = null;
		try {
			Config conf = new Configuration();
			String KISED_MESSENGER_URL = conf.getString("kised.messenger.url");

			StringBuilder postData = new StringBuilder();
			postData.append("dmlUid").append("=").append(URLEncoder.encode("@admin", "UTF-8")).append("&");
			postData.append("ruidList").append("=").append(URLEncoder.encode(id, "UTF-8")).append("&");
			postData.append("dmlEtcCode").append("=").append(URLEncoder.encode(type, "UTF-8")).append("&");
			postData.append("dmlTitle").append("=").append(URLEncoder.encode(title, "UTF-8")).append("&");
			postData.append("dmlContentHtml").append("=").append(URLEncoder.encode(content, "UTF-8"));

			byte[] postDataBytes = postData.toString().getBytes("UTF-8");

			String urlString = KISED_MESSENGER_URL;
			URL url = new URL(urlString);
			if (url.getProtocol().equals("https")) {
				ignoreSsl();
			}
			con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			con.setReadTimeout(30 * 1000); // InputStream 읽어 오는 Timeout 시간 설정
			con.addRequestProperty("Accept-Charset", "UTF-8"); // Charset 설정
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			con.setRequestMethod("POST"); // 요청방식 설정
			con.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			con.setDoOutput(true);
			con.setDoInput(true);

			OutputStream os = con.getOutputStream();
			os.write(postDataBytes);
			os.flush();
			IOUtils.closeQuietly(os);

			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) { // 서버가 정상적으로 응답했다면
				Logger.debug.println("request sended : " + postData);
			} else {
				throw new Exception("메신저서비스 통신에 실패하였습니다. " + con.getResponseCode() + " 오류");
			}
		} catch (Exception e) {
			Logger.err.println(e);
		} finally {
			try {
				con.disconnect();
			} catch (Exception e) {
			}
		}
	}

	@Description("다운로드 콜백호출")
	public void sendDownloadCallback(String keyValue, String objtCode) {
		HttpURLConnection con = null;
		try {
			Config conf = new Configuration();
			String KISED_DOWNLOAD_CALLBACK_URL = conf.getString("kised.download.callback.url");

			StringBuilder postData = new StringBuilder();
			postData.append("keyValue").append("=").append(URLEncoder.encode(keyValue, "UTF-8")).append("&");
			postData.append("objtCode").append("=").append(URLEncoder.encode(objtCode, "UTF-8"));
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");

			String urlString = KISED_DOWNLOAD_CALLBACK_URL;
			URL url = new URL(urlString);
			if (url.getProtocol().equals("https")) {
				ignoreSsl();
			}
			con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			con.setReadTimeout(30 * 1000); // InputStream 읽어 오는 Timeout 시간 설정
			con.addRequestProperty("Accept-Charset", "UTF-8"); // Charset 설정
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			con.setRequestMethod("POST"); // 요청방식 설정
			con.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			con.setDoOutput(true);
			con.setDoInput(true);

			OutputStream os = con.getOutputStream();
			os.write(postDataBytes);
			os.flush();
			IOUtils.closeQuietly(os);

			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) { // 서버가 정상적으로 응답했다면
				Logger.debug.println("request sended : " + postData);
			} else {
				throw new Exception("통신에 실패하였습니다. " + con.getResponseCode() + " 오류");
			}
		} catch (Exception e) {
			Logger.err.println(e);
		} finally {
			try {
				con.disconnect();
			} catch (Exception e) {
			}
		}
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
