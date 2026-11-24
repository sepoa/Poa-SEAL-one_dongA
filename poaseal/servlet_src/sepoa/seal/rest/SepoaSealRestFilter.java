package sepoa.seal.rest;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.cfg.Configuration;
import sepoa.fw.util.CommonUtil;
import sepoa.seal.rest.exception.SepoaSealRestValidateException;

public class SepoaSealRestFilter {
	public static void doValidationCheck(HttpServletRequest request, Map<String, String> headerData) throws Exception {
		if(!getValitaionIP(request)) {
			throw new SepoaSealRestValidateException("Invalid IP : " + SepoaSealRestUtil.getRestClientIP(request));
		}

		if(headerData == null || headerData.size() <= 0) {
			throw new Exception("Not found HEADER in data. Please check the parameter format.");
		}

		String accessKey = MapUtils.getString(headerData, "ACCESS_KEY", null);
		String serviceName = MapUtils.getString(headerData, "SERVICE", null);
		String companyCode = MapUtils.getString(headerData, "COMPANY_CODE", null);
		
		if(accessKey == null || "".equals(accessKey)) {
			throw new Exception("'ACCESS_KEY' is nessasary in HEADER. Please check the parameters.");
		} else if (serviceName == null || "".equals(serviceName)) {
			throw new Exception("'SERVICE' is nessasary in HEADER. Please check the parameters.");
		} else if(companyCode == null || "".equals(companyCode)) {
			throw new Exception("'COMPANY_CODE' is nessasary in HEADER. Please check the parameters.");
		}
		
		String allowAccessKey = CommonUtil.getConfig("sepoa.seal.service.key", "");
		String key = MapUtils.getString(headerData, "ACCESS_KEY", null);

		if(!key.equalsIgnoreCase(allowAccessKey)) {
			throw new SepoaSealRestValidateException("Invalid Access Key: " + key);
		}
	}
	
	public static boolean getValitaionIP(HttpServletRequest request) throws Exception {
		Configuration config = new Configuration();
		String[] availableClientIp = config.get("sepoa.seal.service.available.ip", "").split(";");
		String clientIp = SepoaSealRestUtil.getRestClientIP(request);
		boolean isValidIp = false;
		for(String ip : availableClientIp) {
			if(clientIp.equals(ip)) {
				isValidIp = true;
			}
		}

		return isValidIp;
	}
}
