package sepoa.seal.rest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.webservice.logger.WSLogger;
import sepoa.fw.webservice.logger.WSLoggerManager;
import sepoa.svl.util.SepoaMultipart;

public class SepoaSealRest extends HttpServlet {
	private static final long serialVersionUID = 1191464997450346129L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doPost(req, res);
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");

		String ifCode = null;
		SepoaInfo loggerInfo = null;
		
		String tempPath = null;
		String returnFlag = "";
		String returnMsg = "";
		String returnType = "";
		String house_code = null;
		
		WSLogger logger = null;
		Map<String, Object> returnMap = new HashMap<String, Object>();
		Map<String, Object> allData = new HashMap<String, Object>();
		Map<String, String> header = new HashMap<String, String>();
		String serviceName = null;
		String companyCode = null;
		String senderID = null;
		
		ArrayList<File> trashFileList = new ArrayList<File>();
		
		File returnFile = null;
		try {
			house_code = CommonUtil.getConfig(req.getServerName() + ".house_code");

			// log
			loggerInfo = new SepoaInfo(house_code, "COMPANY_CODE=-^@^ID=IF_MNGR^@^LANGUAGE=KO^@^");
			ifCode = DocumentUtil.getDocNumberRemote(loggerInfo, "REST_IF").result[0];
			logger = WSLoggerManager.getLogger(loggerInfo);
			logger.setTransactionCode(ifCode);
			logger.setFuncionName("PARSING");
			
			String reqContentType = req.getHeader("Content-type");

			if(reqContentType != null && reqContentType.startsWith("multipart")) {
				// 사용하지 않음
				int maxSize = 1024 * 1024 * Integer.parseInt(CommonUtil.getConfig("sepoa.attach.maxsize"));
				tempPath = SepoaMultipart.getTempPath();
				trashFileList.add(new File(tempPath));

				MultipartRequest mreq = new MultipartRequest(req, tempPath, maxSize, "utf-8", new DefaultFileRenamePolicy());
				allData = SepoaSealRestUtil.parseData(mreq);
			} else {
				//Parse HEADER
				allData = SepoaSealRestUtil.parseData(req);
			}

			header = MapUtils.getMap(allData, "HEADER", null);
			serviceName = MapUtils.getString(header, "SERVICE", "");
			companyCode = MapUtils.getString(header, "COMPANY_CODE", "1000");
			senderID = MapUtils.getString(header, "SENDER_ID", CommonUtil.getConfig("sepoa.interface.rest.sender.id.default"));
			
			loggerInfo = new SepoaInfo(house_code, "COMPANY_CODE="+companyCode+"^@^ID="+senderID+"^@^LANGUAGE=KO^@^");
			logger = WSLoggerManager.getLogger(loggerInfo);
			logger.setTransactionCode(ifCode);
			logger.setFuncionName(serviceName);
			
			if ("REQ_STAMP".equals(serviceName)) {
				logger.setData("SEAL_INFO", MapUtils.getMap(allData, "SEAL_INFO", null));
				
				if (allData.containsKey("DOC_LIST")) {
					List<Map<String, String>> docList = (List<Map<String, String>>) MapUtils.getObject(allData, "DOC_LIST", new ArrayList<>());
					List<Map<String, String>> docListLog = new ArrayList<Map<String, String>>();
					for (Map<String, String> docMap : docList) {
						Map<String, String> docMapLog = new HashMap<String, String>();
						docMapLog.putAll(docMap);
						docMapLog.remove("FILE64");
						docListLog.add(docMapLog);
					}
					logger.setData("DOC_LIST", docListLog);
				}
				if (allData.containsKey("APP_LINE")) {
					logger.setData("APP_LINE", (List<Map<String, String>>) MapUtils.getObject(allData, "APP_LINE", new ArrayList<>()));
				}
			} else {
				throw new SepoaSealRestException("The value of 'SERVICE' is incorrect.");
			}
			
			
			//Filter Checking
			SepoaSealRestFilter.doValidationCheck(req, header);
			
			//Call Manager
			SepoaSealRestManager mng = new SepoaSealRestManager(house_code, serviceName, allData);
			SepoaOut so = mng.runService();
			if(so == null) {
				throw new SepoaSealRestException("Invalid SERVICE parameter");
			} else if(!so.flag) {
				throw new SepoaSealRestException(so.message);
			}

			if(so.obj != null) {
				//res.setStatus(200);
				if(so.obj instanceof Map) {
					returnType = "json";
					returnMap.putAll((Map<String, Object>)so.obj);
				} else if(so.obj instanceof File) {
					returnType = "file";
					returnFile = (File) so.obj;
				}
				
			}
			returnFlag = "S";
			returnMsg = "Success";
		} catch (Exception e) {
			//res.setStatus(500);
			returnFlag = "F";
			returnMsg = e.getMessage();
			e.printStackTrace();
			
		} finally {
			//Service Return File or Json
			returnMap.put("E_RESULT", returnFlag);
			returnMap.put("E_MSG", returnMsg);
			
			try {
				switch (returnType) {
					case "file":
						setFileToResponse(returnFile, res);
						break;
					case "json":
					default:
						setJsonToResponse(returnMap, res);
						break;
				}
			} catch (Exception e) {
				returnMsg = e.getMessage();
				e.printStackTrace();
			}
			
			try {
				logger.write();
				logger.result(returnFlag, returnMsg);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//Remove Trash File
			for (File f : trashFileList) {
				if (f.exists()) {
					FileUtils.forceDelete(f);
				}
			}
		}
	}
	
	public void setJsonToResponse(Map returnMap, HttpServletResponse res) throws Exception{
		res.setContentType("application/json; charset=UTF-8");
		PrintWriter pout = res.getWriter();
		pout.println(new Gson().toJson(returnMap));
		pout.flush();
	}
	
	public void setFileToResponse(File file, HttpServletResponse res) throws Exception{
		res.setContentType("application/octet-stream; charset=utf-8");
		res.setContentLength((int) file.length());
		res.setHeader("Content-Transfer-Encoding", "binary");
		ServletOutputStream out = res.getOutputStream();
		FileInputStream fis = new FileInputStream(file);

		BufferedInputStream bis = new BufferedInputStream(fis);
	    BufferedOutputStream bos = new BufferedOutputStream(out);
	    
	    int readBytes = 0;
	    while((readBytes = bis.read()) != -1) {
	      bos.write(readBytes);
	    }

	    bos.flush();
	    fis.close();
	    bis.close();
	    bos.close();
	}
}
