package sepoa.svl.seal.auto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.svl.util.SepoaActionController;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

/**
 * 자동날인 서블릿
 */
@WebServlet("/SepoaAutoMarkSeal.sux")
public class SepoaAutoMarkSeal extends SepoaActionController {

	private static final long serialVersionUID = -1890238203601344051L;

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		doPost(req, res);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		process(req, res);
	}

	protected void process(HttpServletRequest req, HttpServletResponse res) {
//		String tokenValue = req.getParameter("tokenValue");
		String tokenValue = req.getParameter("iToken");
		String keyValue = req.getParameter("keyValue");
		String printCnt = req.getParameter("printCnt");
		String objtCode = req.getParameter("objtCode");
		String fileId = req.getParameter("fileId");
		if (printCnt == null) {
			printCnt = "1";
		}

		SepoaInfo info = SepoaSession.getAllValue(req);
		if (StringUtils.isBlank(info.getSession("ID"))) {
			Map<String, Object> allData = new HashMap<String, Object>();
			Map<String, String> rawHeaderData = new HashMap<String, String>();
			allData.put(SepoaDataMapper.KEY_RAW_HEADER_DATA, rawHeaderData);
			rawHeaderData.put("token", tokenValue);

			SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.login.cbo_006.LoginSSO", "CONNECTION",
					"loginSSO", new Object[] { allData });
			if (!out.flag) {
				throw new IllegalStateException("SSO Login failed. - " + out.message);
			}
			info = SepoaSession.getAllValue(req);
		}
		String path = null;
		try {
			Config conf = new Configuration();
			String sealImage = conf.getString("sepoa.seal.auto.mark.img." + objtCode);
			String applyType = conf.getString("sepoa.seal.auto.mark.applytype." + objtCode);
			String sourceFileName = getSourceFileName(objtCode);

			String subject = "[자동날인] " + sourceFileName + " ";
//			path = getFile(fileId);
			path = getFile(fileId, objtCode);

			Map<String, Object> allData = new HashMap<String, Object>();
			Map<String, Object> objectData = new HashMap<String, Object>();
			allData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, objectData);

			Map<String, Object> parameter = new HashMap<String, Object>();
			objectData.put("PARAMETER", parameter);

			Map<String, Object> reqSealInfo = new HashMap<String, Object>();
			parameter.put("REQ_SEAL_INFO", reqSealInfo);
			reqSealInfo.put("HOUSE_CODE", "006");
			reqSealInfo.put("COMPANY_CODE", "1000");
			reqSealInfo.put("REQ_USER_ID", info.getSession("ID"));
			reqSealInfo.put("SUBJECT", subject);
			reqSealInfo.put("APPLY_TYPE", applyType);
			reqSealInfo.put("REQ_SEAL_USE_TYPE", "1");
			reqSealInfo.put("SUBMISSION_PLACE", "");
			reqSealInfo.put("REMARK", "자동날인 완료된 문서입니다.");
			reqSealInfo.put("RE_FLAG", "E");
			reqSealInfo.put("AUTO_COMPLETE_FLAG", "Y");
			reqSealInfo.put("REGAL_WATERMARK", "K");
			reqSealInfo.put("OPEN_COMPLETE_DOC", "Y");

			List<Map<String, Object>> documentList = new ArrayList<Map<String, Object>>(1);
			Map<String, Object> document = new HashMap<String, Object>();
			documentList.add(document);
			parameter.put("DOCUMENT_LIST", documentList);
			document.put("SUBJECT", subject);
			document.put("COUNT", printCnt);
			document.put("AUTO_PRINT_FLAG", "Y");
			document.put("PATH", path);
			document.put("SOURCE_FILE_NAME", sourceFileName);
			List<Map<String, String>> sealList = new ArrayList<Map<String, String>>();
			Map<String, String> seal = new HashMap<String, String>();
			seal.put("MANAGE_NUMBER", sealImage);
			seal.put("OBJT_CODE", objtCode);
			sealList.add(seal);
			document.put("SELECT_SEAL_LIST", sealList);

			Object[] obj = { allData };
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.sealmk2.SepoaReqSealService", "TRANSACTION",
					"externalReqSealCreateService", obj);
			if (so.flag) {
				if (so.getDataCount() > 0) {
					if (so.result.length > 0) {
						Map<String, String> resultMap = new Gson().fromJson(so.result[0], Map.class);
						String key = resultMap.get("KEY");
						System.out.println("KEY : " + key);

						resultMap.put("KEY_VALUE", keyValue);
						resultMap.put("OBJ_CODE", objtCode);
						ServiceConnector.doService(info, "sepoa.svc.cbo.seal.postprocess.SepoaSealPostProcessor",
								"TRANSACTION", "saveExternalReqSealData", new Object[] { resultMap });

						String redirect = "/link.sux?ln=" + key;
						RequestDispatcher rd = req.getRequestDispatcher(redirect);
						rd.forward(req, res);
					}
				}
			} else {
				System.out.println(so.message);
				throw new Exception(so.message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
//			try {
//				FileUtils.forceDelete(new File(path));
//			} catch (Exception e) {
//			}
		}
	}

	private String getFile(String fileId) throws Exception {
		Config conf = new Configuration();
		String misUrl = conf.getString("mis.url");
		String downLoadUrl = misUrl + "/erp/RdPdf/pdfDownload.do?fileId=" + fileId;

		File file = File.createTempFile("auto_", ".pdf");

		URL url = new URL(downLoadUrl);
		if (url.getProtocol().equals("https")) {
			ignoreSsl();
		}
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(5000); // 서버에 연결되는 Timeout 시간 설정
			con.setReadTimeout(30 * 1000); // InputStream 읽어 오는 Timeout 시간 설정
			con.addRequestProperty("Accept-Charset", "UTF-8"); // Charset 설정
			con.addRequestProperty("Content-Type", "application/json"); // type설정
			con.setRequestMethod("GET"); // 요청방식 설정
			con.setDoOutput(false); // URLConnection이 서버에 데이터를 보낼 것인지 여부 설정(기본값은 false)

			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) { // 서버가 정상적으로 응답했다면
				InputStream is = con.getInputStream();
				OutputStream os = FileUtils.openOutputStream(file);

				IOUtils.copy(is, os);

				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		} finally {
			try {
				con.disconnect();
			} catch (Exception e) {
			}
		}

		return file.getAbsolutePath();
	}

	private String getSourceFileName(String objtCode) throws Exception {
		String res = "Auto";
		switch (objtCode) {
		case "01":
			res = "재직증명서.pdf";
			break;
		case "02":
			res = "경력증명서.pdf";
			break;
		case "03":
			res = "재직경력증명서(영문).pdf";
			break;
		case "04":
			res = "퇴직증명서.pdf";
			break;
		case "05":
			res = "원천징수영수증.pdf";
			break;
		case "06":
			res = "갑종근로소득에대한소득세원천징수.pdf";
			break;
		case "07":
			res = "소득자별근로소득원천징수부.pdf";
			break;
		case "08":
			res = "육아휴직 증명서.pdf";
			break;
		case "09":
			res = "육아휴직 급여 사후지급 확인서.pdf";
			break;
		case "10":
			res = "조치요구서.pdf";
			break;
		case "11":
			res = "수료증.pdf";
			break;
		case "12":
			res = "급여명세서.pdf";
			break;
		}
		return res;
	}

	private String getFile(String fileId, String objtCode) throws Exception {
		String res = "E:/202301/11223344";
		switch (objtCode) {
		case "01":
			res = "E:/202301/연동_출력물정리/1.재직증명서.pdf";
			break;
		case "02":
			res = "E:/202301/연동_출력물정리/2.경력증명서.pdf";
			break;
		case "03":
			res = "E:/202301/연동_출력물정리/3.재직경력증명서(영문).pdf";
			break;
		case "04":
			res = "E:/202301/연동_출력물정리/4.퇴직증명서.pdf";
			break;
		case "05":
			res = "E:/202301/연동_출력물정리/5.원천징수영수증.pdf";
			break;
		case "06":
			res = "E:/202301/연동_출력물정리/6.갑종근로소득에대한소득세원천징수.pdf";
			break;
		case "07":
			res = "E:/202301/연동_출력물정리/7.소득자별근로소득원천징수부.pdf";
			break;
		case "08":
			res = "E:/202301/연동_출력물정리/8.육아휴직 증명서.pdf";
			break;
		case "09":
			res = "E:/202301/연동_출력물정리/9.육아휴직 급여 사후지급 확인서.pdf";
			break;
		case "10":
			res = "E:/202301/연동_출력물정리/10.조치요구서.pdf";
			break;
		case "11":
			res = "E:/202301/연동_출력물정리/11.수료증.pdf";
			break;
		case "12":
			res = "E:/202301/연동_출력물정리/12.급여명세서-날인.pdf";
			break;
		}
		return res;

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
