package sepoa.fw.sign.onepro;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.kica.kiap.sdk.model.KICAResult;
import com.kica.kiap.sdk.sign.KiapPollingService;
import com.kica.kiap.sdk.sign.KiapService;
import com.kica.kiap.sdk.util.KiapCertUtil;
import com.kica.kiap.sdk.util.KiapUtil;

import platform.fw.module.token.TokenManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import signgate.crypto.auth.KICAAuth;

public class SepoaSignONEProServer extends HttpServlet {
	private static final long serialVersionUID = -4860444909442707204L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doPost(req, res);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String params = IOUtils.toString(req.getReader());
		String type = req.getParameter("type");
		String mode = req.getParameter("mode");
		
		try {
			PrintWriter pout = res.getWriter();
			Config config = new Configuration();
			String licensePath = config.get("sepoa.sign.one.path") + "/license/license.kica";
			KICAAuth.init(licensePath);
			
			if("pro".equalsIgnoreCase(type)) {
				if ("request".equalsIgnoreCase(mode)) {
					this.proRequest(pout, params);
				} else if ("result".equalsIgnoreCase(mode)) {
					this.proResult(pout, params);
				}
			} else if("mobileId".equalsIgnoreCase(type)) {
				System.out.println("mobileId action here");
			} else if("verify".equalsIgnoreCase(type)) {
				this.verify(pout, params);
			} else {
				throw new Exception("not supported request type : " + type);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void proRequest(PrintWriter pout, String params) throws Exception {
		Config config = new Configuration();
		String configHome = config.get("sepoa.sign.one.path") + "/config";
		KiapPollingService kiapPollingService = new KiapPollingService();
		String result = null;
		result = kiapPollingService.kiapReqSign(configHome, params);
		
		pout.println(result);
		pout.flush();
	}
	
	private void proResult(PrintWriter pout, String params) throws Exception {
		KiapService kiapService = new KiapService();
		KICAResult result = kiapService.getSignResult(params);
		JSONObject response = new JSONObject(result);
		
		if (result.getResultCd().equalsIgnoreCase("0000")) {
			String provider = result.getProvider();
			String p7 = result.getSignedData();
			
//			String encUserCi = result.getUserCi();
			String encUserName = result.getUserName();
			String encUserPhone = result.getUserPhoneNum();
			String encBirthday = result.getUserBirthday();
			
			try {
//				String userCi = KiapUtil.decryptCI(provider, encUserCi);
				String userName = KiapUtil.doDecryption(provider, encUserName);
				String userPhone = KiapUtil.doDecryption(provider, encUserPhone);
				String userBirthday = KiapUtil.doDecryption(provider, encBirthday);
				
				Logger.info.println("====================================================");
//				Logger.info.println("SepoaSignOnePro Result > CI       : " + userCi);
				Logger.info.println("SepoaSignOnePro Result > name     : " + userName);
				Logger.info.println("SepoaSignOnePro Result > phone    : " + userPhone);
				Logger.info.println("SepoaSignOnePro Result > birthday : " + userBirthday);
				Logger.info.println("====================================================");
				
				KiapCertUtil kiapCertUtil = new KiapCertUtil(provider, p7);
				Logger.info.println("SepoaSignOnePro Certification Info");
				Logger.info.println("간편인증서 시작일 : " + kiapCertUtil.getNotBefore());
				Logger.info.println("간편인증서 만료일 : " + kiapCertUtil.getNotAfter());
				Logger.info.println("간편인증서 시리얼넘버 : " + kiapCertUtil.getSerialNumber());
				Logger.info.println("간편인증서 주체 DN : " + kiapCertUtil.getSubjectDN());
				Logger.info.println("간편인증서 발급자 DN :" + kiapCertUtil.getIssuerDN());
			} catch(Exception e) {
				Logger.info.println("인증서 정보 추출 실패 : " + e.getMessage());
			}
			
			Logger.info.println("====================================================");
		}
		
		pout.println(response.toString());
		pout.flush();
	}
	
	private void verify(PrintWriter pout, String params) {
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> paramData = new Gson().fromJson(params, Map.class);
		
		try {
			String provider = MapUtils.getString(paramData, "EXT_PROVIDER");
			
			String signValue = MapUtils.getString(paramData, "SIGN_VALUE");
			String orgData = MapUtils.getString(paramData, "ORG_DATA");
			
			String message = KiapUtil.verifySignedData(provider, signValue);
			if("KAKAO".equals(provider)) {
				if(!message.contains(orgData)) {
					throw new Exception("Signed Data verify failed.");
				}
			} else {
				if(!orgData.equals(message)) {
					throw new Exception("Signed Data verify failed.");
				}
			}
			
			Map<String, String> targetInfo = MapUtils.getMap(paramData, "TARGET");
			paramData.remove("TARGET");
			
			String userName = KiapUtil.doDecryption(provider, MapUtils.getString(paramData, "EXT_USER_NAME"));
			String targetName = MapUtils.getString(targetInfo, "SIGNER_NAME");
			boolean isSameName = userName.equals(targetName);
			
			String userPhone = KiapUtil.doDecryption(provider, MapUtils.getString(paramData, "EXT_USER_PHONE"));
			String targetPhone = MapUtils.getString(targetInfo, "SIGNER_PHONE").replaceAll("[^\\d]", "");
			boolean isSamePhone = userPhone.equals(targetPhone);
			
			if(!(isSameName && isSamePhone)) {
				throw new Exception("Signed User verify failed.");
			}
			
			String token = TokenManager.getBuilder()
					.putAll(TokenManager.getToken(paramData.get("s-token").toString()).getData())
					.build();
			result.put("s-token", token);
			result.putAll(paramData);
			result.put("status", "1");
			result.put("flag", true);
		} catch(Exception e) {
			e.printStackTrace();
			result.put("status", 0);
			result.put("flag", false);
			result.put("message", e.getMessage());
		}
		
		pout.print(new Gson().toJson(result));
	}
}
