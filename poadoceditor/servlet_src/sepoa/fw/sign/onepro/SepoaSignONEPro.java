package sepoa.fw.sign.onepro;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.kica.kiap.sdk.util.KiapCertUtil;
import com.kica.kiap.sdk.util.KiapSDKException;
import com.kica.kiap.sdk.util.KiapUtil;

import sepoa.fw.cfg.Config;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.sign.SepoaSignAdapter;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import signgate.crypto.util.Base64Util;
import signgate.crypto.util.CertUtil;
import signgate.crypto.util.KeyUtil;
import signgate.crypto.util.PKCS7Util;


public class SepoaSignONEPro extends SepoaSignAdapter {
	protected CertUtil certUtil;
	protected String[] allowedPolicyOIDs;
	protected String signValue = null;
	
	private Map<String, String> extraData = null;
	
	public SepoaSignONEPro(SepoaInfo info, Config config) {
		super(info, config);
	}
	
	public boolean setSignValue(String signValue){
		boolean result = false;
		
		this.signValue = signValue;
		
		String provider = MapUtils.getString(this.extraData, "PROVIDER");
		String message = null;
		try {
			message = KiapUtil.verifySignedData(provider, signValue);
		} catch (Exception e) {
			this.errCode = 100802;
		}
		
		if(message != null) {
			result = true;
		}
		
		return result;
	}
	
	public boolean setExtraData(Map<String, String> extraData) {
		this.extraData = extraData;
		
		return true;
	}
	
	public boolean compareToOrigin(String orgValue) {
		boolean result = false;
		
		String provider = MapUtils.getString(this.extraData, "PROVIDER");
		String extractData = null;
		try {
			extractData = KiapUtil.verifySignedData(provider, this.signValue);
		} catch (KiapSDKException e) {
			this.errCode = 100803;
			return false;
		}
		
		if(extractData == null) {
			this.errCode = 100804;
			return false;
		}
		
		if("KAKAO".equals(provider)) {
			result = extractData.contains(orgValue);
		} else {
			result = orgValue.equals(extractData);
		}
		
		if(!result) {
			StringBuilder sb = new StringBuilder();
			sb.append("\n")
				.append("compareToOrign").append("\n")
				.append("origin : ").append(orgValue).append("\n")
				.append("signed : ").append(extractData);
			Logger.debug.println(sb.toString());
			this.errCode = 100805;
		}
		
		return result;
	}
	
	public boolean verifyCert(){
		return true;
	}
	
	public boolean verifyUser(Map<String, String> userInfo) {
		boolean result = false;
		String provider = MapUtils.getString(this.extraData, "PROVIDER");
		
		try {
			String userName = KiapUtil.doDecryption(provider, MapUtils.getString(this.extraData, "USER_NAME", ""));
			String userPhone = KiapUtil.doDecryption(provider, MapUtils.getString(this.extraData, "USER_PHONE", ""));
			
			boolean isSameName = userInfo.get("SIGNER_NAME").equals(userName);
			boolean isSamePhone = userInfo.get("SIGNER_PHONE").replaceAll("[^\\d]", "").equals(userPhone);
			
			if(isSameName && isSamePhone) {
				result = true;
			}
		} catch (Exception e) {
			this.errCode = 100809;
		}
		
		if(!result) {
			this.errCode = 100809;
		}
		
		return result;
	}
	
	public String sign(String signKey, String orgValue) {
		PKCS7Util p7 = new PKCS7Util();
		String houseCode = info.getSession("HOUSE_CODE");
		String companyCode = info.getSession("COMPANY_CODE");
		String certPath = config.get("sepoa.sign.cert.path");
		
		String signValue = null;
		String certPassword = null;
		byte[] signPri = null;
		byte[] signCert = null;
		
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
		String provider = MapUtils.getString(this.extraData, "PROVIDER");
		String result = null;
		try {
			KiapCertUtil cu = new KiapCertUtil(provider, this.signValue);
			result = getProCertString(cu);
		} catch (KiapSDKException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private String getProCertString(KiapCertUtil cu) {
		StringBuffer sb = new StringBuffer();
		
		try {
			sb.append("-----BEGIN CERTIFICATE-----");
			sb.append("\n" + Base64Util.encode(cu.getCertBytes()));
			sb.append("\n" + "-----END CERTIFICATE-----");
		} catch (KiapSDKException e) {
			e.printStackTrace();
		}
		
		sb.append("\n" + "인증서 DN\t\t: " + cu.getSubjectDN());
		sb.append("\n" + "인증서 Serial\t\t: " + cu.getSerialNumber());
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
