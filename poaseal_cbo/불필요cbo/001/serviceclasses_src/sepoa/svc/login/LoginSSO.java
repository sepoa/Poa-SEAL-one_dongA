package sepoa.svc.login;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sun.misc.BASE64Decoder;

@SuppressWarnings({ "unchecked" })
@Description("LoginSSO")
public class LoginSSO extends SepoaService {

	public LoginSSO(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("ssoKey 복호화")
	public SepoaOut decryptSSOKey(Map<String, String> allData) {
		setStatus(1);
		setFlag(true);
		try {
			String ssoKey = allData.get("ssoKey");
			String secretKey = CommonUtil.getConfig("sepoa.sso.secert.key", "");
			String ivKey = CommonUtil.getConfig("sepoa.sso.iv.key", "");
			
			String decId = decrypt(ssoKey, secretKey, ivKey);
			Map<String, String> returnMap = new HashMap<String, String>();
			returnMap.put("ID", decId);
			setValue(returnMap);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	private String encrypt(String text, String secretKey, String ivKey) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		BASE64Decoder dc = new BASE64Decoder();
        byte[] secretCutBytes= new byte[32];
        InputStream is = new ByteArrayInputStream(secretKey.getBytes("UTF-8"));
        byte[] secretKeyBytes= dc.decodeBuffer(is);
        int len= secretKeyBytes.length;
        if (len > secretCutBytes.length) {
        	len = secretCutBytes.length;
        }

        System.arraycopy(secretKeyBytes, 0, secretCutBytes, 0, len);
        byte[] ivCutBytes= new byte[16];
        is = new ByteArrayInputStream(ivKey.getBytes("UTF-8"));
        byte[] ivKeyBytes= dc.decodeBuffer(is);
        len= ivKeyBytes.length;
        if (len > ivCutBytes.length) {
        	len = ivCutBytes.length;
        }

        System.arraycopy(ivKeyBytes, 0, ivCutBytes, 0, len);
        
        SecretKeySpec keySpec = new SecretKeySpec(secretCutBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivCutBytes);
        
        cipher.init(Cipher.ENCRYPT_MODE,keySpec,ivSpec);
        byte[] results = cipher.doFinal(text.getBytes("UTF-8"));
        Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(results);
	}
	
	private String decrypt(String text, String secretKey, String ivKey) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		BASE64Decoder dc = new BASE64Decoder();
		
		byte[] secretCutBytes = new byte[32];
		InputStream is = new ByteArrayInputStream(secretKey.getBytes("UTF-8"));
        byte[] secretKeyBytes= dc.decodeBuffer(is);
		int len = secretKeyBytes.length;
		if (len > secretCutBytes.length) {
			len = secretCutBytes.length;
		}

		System.arraycopy(secretKeyBytes, 0, secretCutBytes, 0, len);

		byte[] ivCutBytes = new byte[16];
		is = new ByteArrayInputStream(ivKey.getBytes("UTF-8"));
        byte[] ivKeyBytes= dc.decodeBuffer(is);
		len = ivKeyBytes.length;
		if (len > ivCutBytes.length) {
			len = ivCutBytes.length;
		}

		System.arraycopy(ivKeyBytes, 0, ivCutBytes, 0, len);

		SecretKeySpec keySpec = new SecretKeySpec(secretCutBytes, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(ivCutBytes);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] results = cipher.doFinal(decoder.decodeBuffer(text));
		return new String(results, "UTF-8");
	}
	
	public static void main(String[] args) throws Exception{
		String html = "<div class=\"contract-item contract-item-box contract-item-box-no-CI21111500010 contract-item-box-code-ORG_CONTRAST contract-item-box-seal-data contract-item-box-seal-code-undefined contract-item-box-itemkey-1638250875387 contract-item-box-source-org contract-item-type-seal contract-item-unit-save-ready\" style=\"left: 26.35%; top: 10.92%; width: 33.33%; height: 10.1%; z-index: 1;\"><textarea class=\"contract-item contract-item-input contract-item-prop-seal_template-STD contract-item-prop-repeat-OFF contract-item-prop-cut_position-LEFT contract-item-prop-cut_percent-0 contract-item-prop-symmetry-OFF contract-item-prop-symmetry_cut_position-LEFT contract-item-prop-symmetry_cut_percent-0 contract-item-prop-symmetry_page_copy-OFF contract-item-prop-seal_width-7 contract-item-prop-seal_height-3\" readonly=\"\" wrap=\"off\" placeholder=\"원본대조필\" style=\"-webkit-transform: translateX(0%);transform: translateX(0%);\"></textarea><div class=\"contract-item contract-item-input-origin\" style=\"display:none;\"></div><div class=\"contract-item contract-item-input-suffix\" style=\"display:none;\"></div><div class=\"contract-item contract-item-input-prefix\" style=\"display:none;\"></div><div class=\"contract-item contract-item-cover\" style=\"background-image: url(\"http://gw.kyoboaxa.com:17790/SepoaActionController.sux?module=seal.management&action=doSelectSealPreviewImage&id=CI21111500010\");\"></div><div class=\"contract-item-se-resize\"></div><div class=\"contract-item-delete\"></div></div><div class=\"contract-item contract-item-box contract-item-box-no-CI21111300001 contract-item-box-code-C contract-item-box-seal-data contract-item-box-seal-code-SLM211125000022 contract-item-box-itemkey-1638250877363 contract-item-box-source-org contract-item-type-seal contract-item-unit-save-ready\" style=\"left: 65.71%; top: 14.21%; width: 9.52%; height: 6.73%; z-index: 2;\"><textarea class=\"contract-item contract-item-input contract-item-prop-seal_template-STD contract-item-prop-repeat-OFF contract-item-prop-cut_position-LEFT contract-item-prop-cut_percent-0 contract-item-prop-symmetry-OFF contract-item-prop-symmetry_cut_position-LEFT contract-item-prop-symmetry_cut_percent-0 contract-item-prop-symmetry_page_copy-OFF contract-item-prop-seal_width-2 contract-item-prop-seal_height-2\" readonly=\"\" wrap=\"off\" placeholder=\"법인인감\" style=\"-webkit-transform: translateX(0%);transform: translateX(0%);\"></textarea><div class=\"contract-item contract-item-input-origin\" style=\"display:none;\"></div><div class=\"contract-item contract-item-input-suffix\" style=\"display:none;\"></div><div class=\"contract-item contract-item-input-prefix\" style=\"display:none;\"></div><div class=\"contract-item contract-item-cover\" style=\"background-image: url(\"http://gw.kyoboaxa.com:17790/SepoaActionController.sux?module=seal.management&action=doSelectSealPreviewImage&id=CI21111300001\");\"></div><div class=\"contract-item-se-resize\"></div><div class=\"contract-item-delete\"></div></div><style>.contract-item-box-source-symmetry .contract-item-prop-symmetry_page_copy-ON{display: none;}.contract-item-box-source-copy .contract-item-prop-symmetry_page_copy-ON{display:block;}.contract-item-box-source-org .contract-item-prop-symmetry_page_copy-ON{display:block;}</style>";
		
		Elements elements = new Element("div").html(html).children();
		for(Element el : elements) {
			List<String> classList = Arrays.asList(el.attr("class").split(" "));
			String boxNoPrefix = "contract-item-box-no-";
			String boxSealCodePrefix = "contract-item-box-seal-code-";
			String itemBoxNo = "";
			String itemBoxSealCode = "";
			String targetSealCode = "";
			
			for(String classStr : classList) {
				if(classStr.startsWith(boxNoPrefix)) {
					itemBoxNo = classStr.substring(boxNoPrefix.length(), classStr.length());
				} else if(classStr.startsWith(boxSealCodePrefix)) {
					itemBoxSealCode = classStr.substring(boxSealCodePrefix.length(), classStr.length());
				}
			}
			
			if("undefined".equals(itemBoxSealCode)) {
				targetSealCode = itemBoxNo;
			} else {
				targetSealCode = itemBoxSealCode;
			}
			
			if(StringUtils.isNotBlank(targetSealCode)) {
				
			}
		}
		
//		String secert_key="QjJhSHNvNVg4RjJsMHNqTW16MzYwUHMyY25Pd0dSc3g=";
//		String iv_key="WWpONE4wZHBZemQ0VUcxMw==";
//		System.out.println("USER1 : " + encrypt("USER1", secert_key, iv_key));
//		System.out.println("USER2 : " + encrypt("USER2", secert_key, iv_key));
//		System.out.println("USER3 : " + encrypt("USER3", secert_key, iv_key));
//		System.out.println("ADMIN : " + encrypt("ADMIN", secert_key, iv_key));
//		System.out.println("CYH : " + encrypt("CYH", secert_key, iv_key));
//		System.out.println("KSJ : " + encrypt("KSJ", secert_key, iv_key));
//		System.out.println("JOMIC : " + encrypt("JOMIC", secert_key, iv_key));
	}
}
