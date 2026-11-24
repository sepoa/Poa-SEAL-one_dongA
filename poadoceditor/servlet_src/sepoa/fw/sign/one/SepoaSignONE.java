package sepoa.fw.sign.one;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import filters.ThreadLocalFilter;
import sepoa.fw.cfg.Config;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.sign.SepoaSignAdapter;
import sepoa.fw.srv.SepoaEdProcess;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import signgate.crypto.util.Base64Util;
import signgate.crypto.util.CertUtil;
import signgate.crypto.util.InvalidBase64Exception;
import signgate.crypto.util.KeyUtil;
import signgate.crypto.util.PKCS7Util;


public class SepoaSignONE extends SepoaSignAdapter {
	protected PKCS7Util p7 = new PKCS7Util();
	protected CertUtil certUtil;
	protected String[] allowedPolicyOIDs;
	
	private Map<String, String> extraData = null;
	
	public SepoaSignONE(SepoaInfo info, Config config) {
		super(info, config);
	}
	
	public boolean setSignValue(String signValue){
		boolean result = false;
		int contentType = this.p7.getPKCS7Type(signValue.getBytes());
		
		/* contentType
		 * 0 : 평문
		 * 1 : 전자서명
		 * 2 : 암호화
		 * 3 : 전자서명 + 암호화
		 */
		if(contentType == 0) {
			this.errCode = 100801;
			return false;
		} else if(contentType == 1) {
			result = this.p7.verify(signValue);
		} else {
			
		}
		
		if(!result) {
			this.errCode = 100802;
		}
		
		return result;
	}
	
	public boolean setExtraData(Map<String, String> extraData) {
		this.extraData = extraData;
		
		return true;
	}
	
	public boolean compareToOrigin(String orgValue) {
		byte[] decrypedData = p7.getRecvData();
		if (decrypedData == null) {
			this.errCode = 100803;
			return false;
		}
		
		String extractData = null;
		try {
			extractData = new String(decrypedData, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			this.errCode = 100804;
			return false;
		}
		boolean result = orgValue.equals(extractData);
		if(!result) {
			StringBuilder sb = new StringBuilder();
			sb.append("\n")
				.append("compareToOrign").append("\n")
				.append("origin : ").append(orgValue).append("\n")
				.append("signed : ").append(extractData);
			Logger.debug.println(sb.toString());
			this.errCode = 100805;
			return false;
		}
		return true;
	}
	
	public boolean verifyCert(){
		if(this.certUtil == null) {
			for (X509Certificate x509Certificate : p7.getCertificate()) {
				try {
					this.certUtil = new CertUtil(x509Certificate.getEncoded());
				}catch(Exception e) {
					Logger.err.println(info.getSession("ID"), this, e);
				}
			}
		}
		if(this.certUtil == null) {
			this.errCode = 100806;
			return false;
		}
		
		if(!this.certUtil.isValid(true)) {
			this.errCode = 100807;
			return false;
		}
		
		if(allowedPolicyOIDs == null) {
			setOIDList();
		}
		
		if(!this.certUtil.isValidPolicyOid(allowedPolicyOIDs)) {
			this.errCode = 100808;
			return false;
		}
		return true;
	}
	
	public boolean verifyUser(Map<String, String> userInfo) {
		boolean result = false;
		if(this.certUtil != null) {
			String idn = userInfo.get("SIGN_KEY");
			String rand = MapUtils.getString(this.extraData, "VID_RANDOM", null);
			if(!StringUtils.isBlank(idn) && !StringUtils.isBlank(rand)) {
				result = this.certUtil.isValidUser(idn, rand);
			}
		}
		
		if(result == false) {
			this.errCode = 100809;		// 서명대상자와 인증서의 소유자가 맞지 않습니다
		}
		
		return result;
	}
	
	public String sign(String signKey, String orgValue) {
		String houseCode = info.getSession("HOUSE_CODE");
		String companyCode = info.getSession("COMPANY_CODE");
		String certPath = config.get("sepoa.sign.cert.path");
		
		String signValue = null;
		String certPassword = null;
		byte[] signPri = null;
		byte[] signCert = null;
		
		/*
		 * config.get("sepoa.sign.cert.path.class"); 있으면
		 * 해당 클래스의 특정 메소드 ("getServerCertPath") 를 호출하고
		 * 리턴 형태로 Map을 받는다 (CERT_PATH, KEY_PATH, CERT_PASSWORD)
		 * ex)
		 * CERT_PATH     : "~~~/signCert.der"
		 * KEY_PATH      : "~~~/signPri.key"
		 * CERT_PASSWORD : "signgate1!"
		 */
		try {
			String certPathClass = config.get("sepoa.sign.cert.path.class");
			if(!StringUtils.isBlank(certPathClass)) {
				SepoaOut so = ServiceConnector.doService(info, certPathClass, ctx, "getServerCertPath", new Object[] {new HashMap<String, Object>()});
				if(so.status == 1) {
					if(so.getDataCount() > 0) {
						Map<String, String> serverCertInfo = (Map<String, String>) so.getData(0);
						signCert = FileUtils.readFileToByteArray(new File(serverCertInfo.get("CERT_PATH")));
						signPri = FileUtils.readFileToByteArray(new File(serverCertInfo.get("KEY_PATH")));
						certPassword = serverCertInfo.get("CERT_PASSWORD");
					}
				}
			} else {
				signCert = FileUtils.readFileToByteArray(new File(certPath + "/signCert.der"));
				signPri = FileUtils.readFileToByteArray(new File(certPath  + "/signPri.key"));
				certPassword = config.get("sepoa.sign.cert.password");
			}
			
			if(StringUtils.isBlank(certPassword)) {
				Logger.err.println(info.getSession("ID"), this, "not found cert password : sepoa.sign.cert.password."+houseCode+"."+companyCode);
				this.errCode = 100822;
				return null;
			}
			
			if(signCert != null) {
				this.certUtil = new CertUtil(signCert);
			}
			
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			this.errCode = 100821;
			return null;
		}
		
		if(!this.certUtil.isValid(true)) {
			this.errCode = 100807;
			return null;
		}
		
		if(allowedPolicyOIDs == null) {
			setOIDList();
		}
		
		if(!this.certUtil.isValidPolicyOid(allowedPolicyOIDs)) {
			this.errCode = 100808;
			return null;
		}
		
		try {
			KeyUtil ku = new KeyUtil(signPri);
			String randomValue = ku.getRandom(certPassword);
			if(!this.certUtil.isValidUser(signKey, randomValue)) {
				Logger.err.println(info.getSession("ID"), this, "certificate identification fail : "+this.certUtil.getErrorMsg());
				this.errCode = 100823;
				return null;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			this.errCode = 100823;
			return null;
		}
		
		try {
			signValue = p7.genSignedData(signPri, certPassword, signCert, orgValue.getBytes("UTF-8"));
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			this.errCode = 100824;
			return null;
		}
		
		if(!this.setSignValue(signValue)) {
			return null;
		}
		
		return signValue;
	}
	
	public String getCertInfo() {
		return getCertString(this.certUtil);
	}
	
	private String getCertString(CertUtil cu) {
		StringBuffer sb = new StringBuffer();
		sb.append("-----BEGIN CERTIFICATE-----");
		sb.append("\n" + Base64Util.encode(cu.getCertBytes()));
		sb.append("\n" + "-----END CERTIFICATE-----");
		sb.append("\n" + "인증서 DN\t\t: " + cu.getSubjectDN());
		sb.append("\n" + "인증서 Serial\t\t: " + cu.getSerialNumber());
		sb.append("\n" + "인증서 OID\t\t: " + cu.getPolicyOid());
		sb.append("\n" + "인증서 CRL배포점\t\t: " + cu.getCrlDP());
		sb.append("\n" + "인증서 유효기간 시작일자:\t\t" + cu.getNotBefore().substring(0, 4) + "년 "+ cu.getNotBefore().substring(4, 6) + "월 " + cu.getNotBefore().substring(6, 8) + "일");
		sb.append("\n" + "인증서 유효기간 만료일자:\t\t" + cu.getNotAfter().substring(0, 4) + "년 "+ cu.getNotAfter().substring(4, 6) + "월 " + cu.getNotAfter().substring(6, 8) + "일");
		return sb.toString();
	}
	
	private void setOIDList() {
		List<String> allowedPolicyOIDList = new LinkedList<String>();
		allowedPolicyOIDList.add("1.2.410.200004.5.2.1.2"); // 한국정보인증(개인)
		allowedPolicyOIDList.add("1.2.410.200004.5.2.1.1"); // 한국정보인증(법인)
		allowedPolicyOIDList.add("1.2.410.200004.5.2.1.4"); // 한국정보인증(서버)
		allowedPolicyOIDList.add("1.2.410.200004.5.1.1.5"); // 증권전산(개인)
		allowedPolicyOIDList.add("1.2.410.200004.5.1.1.7"); // 증권전산(법인)
		allowedPolicyOIDList.add("1.2.410.200005.1.1.1"  ); // 금융결제원(개인)
		allowedPolicyOIDList.add("1.2.410.200005.1.1.5"  ); // 금융결제원(법인)
		allowedPolicyOIDList.add("1.2.410.200004.5.4.1.1"); // 전자인증(개인)
		allowedPolicyOIDList.add("1.2.410.200004.5.4.1.2"); // 전자인증(법인)
		allowedPolicyOIDList.add("1.2.410.200012.1.1.1"  ); // 한국무역정보통신(개인)
		allowedPolicyOIDList.add("1.2.410.200012.1.1.3"  ); // 한국무역정보통신(법인)
		allowedPolicyOIDList.add("1.2.410.200004.5.5.1.1"); // 이니텍 (개인) 
		allowedPolicyOIDList.add("1.2.410.200004.5.5.1.2"); // 이니텍 (법인)

		String oidInProperties = config.get("sepoa.sign.oid.list", "");
		if(!StringUtils.isBlank(oidInProperties)) {
			allowedPolicyOIDList.addAll(Arrays.asList(oidInProperties.split(";")));
		}
		allowedPolicyOIDs = allowedPolicyOIDList.toArray(new String[] {});
	}
}
