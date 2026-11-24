package sepoa.svc.batch;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

public class SeclTimeHash {
	private final String ALGORITHM = "HmacSHA256";

	public String getTimeHash(String SECRET_KEY, long millis) {
		try {
			byte[] secretKey = SECRET_KEY.getBytes();
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secretKey, ALGORITHM));
			ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
			buffer.putLong(millis);
			byte[] bytes = buffer.array();
			byte[] hash = mac.doFinal(bytes);
			// return Base64.getEncoder().encodeToString(hash);

			return new String(Base64.encodeBase64(hash));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}

		return null;

	}

	public Map<String, String> getTimeNowHash(String SECRET_KEY) {
		Map<String, String> map = new HashMap<>();
		long millis = System.currentTimeMillis();
		String hash = getTimeHash(SECRET_KEY, millis);
		map.put("hashTime", String.valueOf(millis));
		map.put("hashValue", hash);
		return map;
	}

	public boolean isOkHash(String SECRET_KEY, String header_time, String header_hash) {
		long millis = Long.parseLong(header_time);
		if (!StringUtils.isBlank(header_hash) && header_hash.equals(getTimeHash(SECRET_KEY, millis))) {
			return true;
		}
		return false;
	}
}
