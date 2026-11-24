package sepoa.seal.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.oreilly.servlet.MultipartRequest;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.svl.util.SepoaMultipart;

public class SepoaSealRestUtil {
	public static Map<String, Object> parseData(HttpServletRequest req) throws Exception{
		Map<String, Object> bodyData = new LinkedHashMap<>();

		try {
			Map<String, Object> allData = new Gson().fromJson(getBody(req), Map.class);
			bodyData = allData;
		} catch(Exception e) {
			Logger.err.print(e);
			e.printStackTrace();
			throw new Exception("Invalid JSON Syntax");
		}

		return bodyData;
	}
	
	public static Map<String, Object> parseData(MultipartRequest mreq) throws Exception{
		Map<String, Object> allData = new HashMap<>();
		List<String> keys = new ArrayList<>();
		Map<String, String> regexMap = new HashMap<>();
		regexMap.put("OBJECT_PARSE", "^CONT-(\\w{0,})-(\\w{0,})$");
		regexMap.put("LIST_OBJECT_PARSE", "^CONT-(\\w{0,})-(\\d{0,})-(\\w{0,})$");
		regexMap.put("HEADER_PARSE", "^HEADER-(\\w{0,})$");
		
		Map<String, String> headerMap = new HashMap<>();
		Map<String, Map<String, Object>> objectMap = new HashMap<>();
		Map<String, Map<String, Map<String, Object>>> objectListMap = new TreeMap<>();
		
		Enumeration<String> paramsEnum = mreq.getParameterNames();
		while(paramsEnum.hasMoreElements()) {
			String key = paramsEnum.nextElement();
			keys.add(key);
		}
		
		Enumeration<String> fileEnum = mreq.getFileNames();
		while(fileEnum.hasMoreElements()) {
			String key = fileEnum.nextElement();
			keys.add(key);
		}
		
		for(String tmpParamKey : keys) {
			for(String regexKey : regexMap.keySet()) {
				String regex = regexMap.get(regexKey);
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(tmpParamKey);
				
				if(m.matches()) {
					if("OBJECT_PARSE".equals(regexKey)) {
						String group = m.group(1);
						String key = m.group(2);
						
						Map<String, Object> putMap = null;
						if(objectMap.containsKey(group)) {
							putMap = objectMap.get(group);
						} else {
							putMap = new HashMap<>();
							objectMap.put(group, putMap);
						}
						
						if(mreq.getParameter(tmpParamKey) == null) {
							putMap.put(key, mreq.getFile(tmpParamKey).getAbsolutePath());
						} else {
							putMap.put(key, mreq.getParameter(tmpParamKey));
						}
					} else if("LIST_OBJECT_PARSE".equals(regexKey)){
						String group = m.group(1);
						String index = m.group(2);
						String key = m.group(3);
						
						Map<String, Map<String, Object>> putList = null;
						if(objectListMap.containsKey(group)) {
							putList = objectListMap.get(group);
						} else {
							putList = new HashMap<>();
							objectListMap.put(group, putList);
						}
						
						Map<String, Object> putMap = null;
						if(putList.containsKey(index)) {
							putMap = putList.get(index);
						} else {
							putMap = new HashMap<>();
							putList.put(index, putMap);
						}
						
						if(mreq.getParameter(tmpParamKey) == null) {
							putMap.put(key, mreq.getFile(tmpParamKey).getAbsolutePath());
						} else {
							putMap.put(key, mreq.getParameter(tmpParamKey));
						}
					} else if("HEADER_PARSE".equals(regexKey)) {
						String headerKey = m.group(1);
						headerMap.put(headerKey,  mreq.getParameter(tmpParamKey));
					}
					break;
				} else {
					continue;
				}
			}
		}
		
		allData.put("HEADER", headerMap);
		
		for(String objKey : objectMap.keySet()) {
			allData.put(objKey, objectMap.get(objKey));
		}
		
		for(String objListKey : objectListMap.keySet()) {
			if(!allData.containsKey(objListKey)) {
				allData.put(objListKey, new ArrayList<Map<String, Object>>());
			}
			
			for(String objKey : objectListMap.get(objListKey).keySet()) {
				((List<Map<String, Object>>) allData.get(objListKey)).add(objectListMap.get(objListKey).get(objKey));
			}
		}
		
		return allData;
	}
	
	public static String getRestClientIP(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");

	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = request.getHeader("Proxy-Client-IP");
	    }
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = request.getHeader("WL-Proxy-Client-IP");
	    }
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = request.getHeader("HTTP_CLIENT_IP");
	    }
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
	    }
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = request.getRemoteAddr();
	    }

	    return ip;
	}

	public static SepoaInfo setSession(HttpServletRequest req, SepoaInfo info, String userId) throws Exception {
		if(userId == null || userId.isEmpty()) {
			throw new Exception("Invalid ID Parameter");
		}
		
		Map<String, String> loginData = new HashMap<String, String>();
		loginData.put("HOUSE_CODE", CommonUtil.getConfig(req.getServerName() + ".house_code"));
		loginData.put("ID", userId);
		//Map<String, Object> arg = new HashMap<String, Object>();
		//arg.put(SepoaDataMapper.KEY_HEADER_DATA, loginData);
		//arg.put(SepoaDataMapper.KEY_REQUEST, req);

		SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION", "forceLogin", new Object[]{ loginData });
		if (!out.flag) throw new Exception(out.message);
		
		return SepoaSession.getAllValue(req);
	}
	
	public static String getBody(HttpServletRequest req) throws Exception{
		InputStream is = req.getInputStream();
		byte[] b = IOUtils.toByteArray(is);
		return new String(b, "UTF-8");
	}
	
	public static File binaryToFile(String binaryString, String fileName) throws Exception {
		String tempPath = SepoaMultipart.getTempPath();

		FileOutputStream fos = null;
		File destFile = new File(tempPath + "/" + fileName);

		try {
			byte[] b64dec = Base64.decodeBase64(binaryString.getBytes("UTF-8"));
			
			fos = new FileOutputStream(destFile);
	        fos.write(b64dec);
	        fos.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("File Decode Fail : " + fileName); 
		}

		return destFile;
	}
	
	public static String fileToBinaryString(String filePath) throws Exception {
		String out = new String();
		FileInputStream fis = new FileInputStream(new File(filePath));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		int len = 0;
		byte[] buf = new byte[1024];
		while ((len = fis.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
 
        byte[] fileArray = baos.toByteArray();
        out = new String(Base64.encodeBase64(fileArray));
 
        fis.close();
        baos.close();
		return out;
	}
	
}
