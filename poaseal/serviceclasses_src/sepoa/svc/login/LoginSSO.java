package sepoa.svc.login;

import java.security.Key;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;

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
		ConnectionContext ctx = getConnectionContext();
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);

			out = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION", "forceLogin", new Object[] { headerData });

			System.out.println(out.flag);
			System.out.println(out.message);

		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return out != null ? out : getSepoaOut();
	}

	private String encrypt(String id) throws Exception {
		Config config = new Configuration();
		// h2h0i1c5
		String keyString = config.getString("sso.encryption.key");
		String instance = "DES/CBC/PKCS5Padding";

		byte[] ivBytes = keyString.getBytes();
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		Cipher c = Cipher.getInstance(instance);

		Key key = new SecretKeySpec(new DESKeySpec(keyString.getBytes()).getKey(), "DES");

		c.init(Cipher.ENCRYPT_MODE, key, iv);
		byte encrypted[] = c.doFinal(id.getBytes("UTF-8"));

		return new String(Base64.encodeBase64(encrypted));
	}

	private String decrypt(String enc) throws Exception {
		Config config = new Configuration();
		// h2h0i1c5
		String keyString = config.getString("sso.encryption.key");
		String instance = "DES/CBC/PKCS5Padding";

		byte[] ivBytes = keyString.getBytes();
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		Cipher c = Cipher.getInstance(instance);

		Key key = new SecretKeySpec(new DESKeySpec(keyString.getBytes()).getKey(), "DES");

		byte[] b = Base64.decodeBase64(enc.getBytes());

		c.init(Cipher.DECRYPT_MODE, key, iv);
		byte output[] = c.doFinal(b);
		return new String(output);
	}

	public static void main(String[] args) throws Exception {
		String id = "admin";
		String keyString = "h2h0i1c5";
		String instance = "DES/CBC/PKCS5Padding";

		byte[] ivBytes = keyString.getBytes();
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		Cipher c = Cipher.getInstance(instance);

		Key key = new SecretKeySpec(new DESKeySpec(keyString.getBytes()).getKey(), "DES");

		c.init(Cipher.ENCRYPT_MODE, key, iv);
		byte input[] = id.getBytes();
		byte encrypted[] = c.doFinal(input);

		String enc = new String(Base64.encodeBase64(encrypted));
		
		System.out.println(enc);

		enc = "azi8HdZJy5KNdarHeCbz+g==";
		System.out.println(enc);

		byte[] b = Base64.decodeBase64(enc.getBytes());

		c.init(Cipher.DECRYPT_MODE, key, iv);
		byte output[] = c.doFinal(b);
		System.out.println(new String(output));
	}
}
