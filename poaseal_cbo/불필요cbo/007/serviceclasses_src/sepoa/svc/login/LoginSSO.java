package sepoa.svc.login;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;

import filters.ThreadLocalFilter;
import net.sf.json.JSONObject;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
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

		String eplogintime = ""; // SECL : SSO 인증시간 필요없을 것 같은데 일단 놔둠

		try { // IE 브라우저인 경우는 고려 안 됨 => 원 소스에서는 있으나 그 부분 삭제함
			Config conf = new Configuration();

			// for SECL 아래 2개중에 어느게 맞는건지 ?? 1 번

			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String ssoData = headerData.get("SSODATA");
			ssoData = ssoData.replace(" ", "+");

			// SECL 소스에서 IP 를 가져오는 부분이 있어서 놔 둠
			HttpServletRequest request = ThreadLocalFilter.getHttpServletRequest();

			// SECL script => IP 체크 안해도 되면 아래 주석처리 필요함
			String ip = request.getHeader("X-Forwarded-For"); // 클라이언트 IP
			if (ip == null) {
				ip = request.getRemoteAddr(); // 다른 방법으로 IP GET
			}
			// SECL : forceLogin 에 넘겨줄 user id , 실제 사용은 이거 하나만
			String userId = null;
			JSONObject jsonObj = JSONObject.fromObject(ssoData); // 암호화된 data 전체
			
			try {
				jsonObj = JSONObject.fromObject(jsonObj.get("data")); // data 전체
				String encodeUserInfo = (String) jsonObj.get("userInfo"); // data 중에서 userinfo - 실제 사용할 data
				String encodeAesKey = (String) jsonObj.get("key"); // data 중에서 key - 실제 사용할 data

				// 연계시스템 개인키
				String privateKey = "";
				privateKey += "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDKIAtNAu2knZ3v\n";
				privateKey += "Q8YNEb4xr1rQW5lrg8qQI+SfrRHzhMM5E2LAEUFMbK3a+7yNVt/nPCdzsAvY74P1\n";
				privateKey += "lHOKFl49SBFj78hyCVmFYVgxl+lS6sNDh53jlWT4nykQCWAgkJuSjCi4ikGaKLFG\n";
				privateKey += "x6KYdCSPFN2gb64NnR3TaKL+IdabqJjDZoma8qYdcD9roP0IeI7TMs/ZijP5czb3\n";
				privateKey += "Wv4bHNMm/hzMJ2gGKCsb26oG55Hl++AbUq9+8F8XPEM2EAZoEGCwhKHKNlgmzvwX\n";
				privateKey += "WKwpwMXN3EJ4Z0BaimixCSPdsdtLHdq04BJo9kGO4SBHvIQfuAQof5RvkYMcwL+s\n";
				privateKey += "NQStivs9AgMBAAECggEAEgCKoSBKpCe5ok7J++4Bn5E7qGUQJAhH6VCZEzOp7qN7\n";
				privateKey += "c8DrQaoHw9PGJEPZhkkfhgvQg7hBReAHQ78mDz1wxUp+6gS4OJfmlj+WSEx8Znss\n";
				privateKey += "Sv5wLI3owVADqz76hcv7XzEGi3Ec6KsJJwGu1J87hlbYamqn+ORufJ7MLZA7+f8A\n";
				privateKey += "eVSGPB/EcZIUU7Mp0f5NCf30NwhRgHeD2KQWnYlWpC/1xqTRk2cWXXNPWAxQlK3R\n";
				privateKey += "BqO2TyxIvvU3PVnysUtLrlVh4DHSMAzH5G7huamwsDArS9CiXSeyDIL4bmdYbdDt\n";
				privateKey += "NUkC6pT2ccodpmh1ZNjdGLmnkQcEAKljhI5ABVVODQKBgQD3sxHr8lAVpilYZWhF\n";
				privateKey += "QW4umU/UekxrP01zt0/BzLCLczJ/g7/gyqI+sOMRLwNSU5bUO/kwAc0lWZq+JtiA\n";
				privateKey += "QB73tewQjoiuHyEoI36urc/Vud2e0v67U5DOIp2Er7wpGazoPp7OFiQErVkpeWS7\n";
				privateKey += "agwq9p4LJbpxu49v7t4HpqGAswKBgQDQ5gHrIKy26imQ2kxMtoAtbPAWIEgRO+xL\n";
				privateKey += "d3vDq0XPBoeBGXM6jws39jwlyacVHhDDkwEJ0owYDA0NIFtxIym5mz7eRD53jg/P\n";
				privateKey += "QBY+i7Y9fjmx5pWkTq4z4BVa9rVR1GcIu4qLhevZjZscXpZsEV4L6bOPZg8pQK0z\n";
				privateKey += "75yFqdSsTwKBgG0O+8oN4zLU+QlBhNpRR6jeuaZMvo7NzVUwlb43WFfdvZMlN/tK\n";
				privateKey += "XIsLqL4tG+JIiXUifsGjWWajgB1Ozy4QjVH1WF2l1AoODLQ2hJV2U7a/YhOGxL9H\n";
				privateKey += "uM75sK72DPtvoZCT2anVV8BzbnlzSGW3+GmGMemoj+F5elsHKdzmgd8/AoGBAJAT\n";
				privateKey += "Ep+oGXbOaXyOErGV7TFnQO8BtLKuMXqbHbEGH095oVtYwjBRF9uw6CrnuWTHJ6i7\n";
				privateKey += "aqvrmtO6/AVhm6+cINryR5nkV3D9ecnm2XCpyzb+J6ya6/o2ic8dbZ0fKk8Vvzo2\n";
				privateKey += "noc8Bj41fBVE51oBYGu28zCjozFAejSnvbwyNYvhAoGBALw0HIpFku+dCyX3fPW7\n";
				privateKey += "tW2A4uDmgqM4aqC4UPydqsNs3OtbdDVFt5QaF98jmqJAJrOOeTm68S/fnLIjGbFJ\n";
				privateKey += "EhZicP04Cb7djYMGrIpfy/0jV3pRiPmB8CH+yO7wk6UfKFwGvIoslp3gV348Ek/x\n";
				privateKey += "pqGVC8VmtWG7L2Cjyhf2iPzq\n";

				// 사용자 정보 암호화 키 복호화
				byte[] bytePrivateKey = Base64.decodeBase64(privateKey.getBytes()); // 키 값 복호화
				PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytePrivateKey);
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

				Cipher cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);

				byte[] decodeAesKey = cipher.doFinal(Base64.decodeBase64(encodeAesKey.getBytes()));

				// 사용자 정보 복호화
				// iv값 고정
				byte[] ivBytes = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

				SecretKey aesKey = new SecretKeySpec(decodeAesKey, "AES");
				AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);

				Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
				aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
				byte[] byteDecodeUserInfo = aesCipher.doFinal(Base64.decodeBase64(encodeUserInfo.getBytes()));

				String decodeUserInfo = new String(byteDecodeUserInfo);
				// 복호화 된 최종 사용자 정보
				jsonObj = JSONObject.fromObject(decodeUserInfo);
			} catch (Exception e) {
				setStatus(85);
				setFlag(false);
				setMessage("SSO 데이터 복호화 중 문제가 발생하였습니다.");
				Logger.err.println(info.getSession("ID"), this, e);
			}
			
			eplogintime = (String) jsonObj.get("EP_LOGINTIMEFORMIS");
			SimpleDateFormat Format = new SimpleDateFormat("yyyyMMddHHmmss");
	        Format.setTimeZone(TimeZone.getTimeZone("GMT"));
	        Date sDate = Format.parse(eplogintime); // sso 인증 시간
			
	        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); 
	        Date LoginDate =  cal.getTime(); 
	        long diffTime =(LoginDate.getTime() - sDate.getTime()) / 1000;
	        long stdTime = Long.parseLong(CommonUtil.getConfig("sepoa.secl.sso.allow.time.second", "10"));
	        if(diffTime >  stdTime) {
	        	setStatus(77);
	        	setFlag(false);
	        	setMessage("SSO 로그인 유효시간이 만료되었습니다.");
	        }
	        
			if(getStatus() == 1) {
				userId = (String) jsonObj.get("EP_LOGINID");
				headerData.put("ID", userId);
				out = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION", "forceLogin", new Object[] { headerData });
			}
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage("SSO 로그인 프로세스 처리 중 문제가 발생하였습니다.");
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return out != null ? out : getSepoaOut();
	}

}
