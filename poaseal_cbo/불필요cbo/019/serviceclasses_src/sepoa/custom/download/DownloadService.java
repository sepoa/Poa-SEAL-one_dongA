package sepoa.custom.download;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.util.SepoaMultipart;
/**
 * 해당 소스는 현대 중공업에 적용되는 파일 다운로드 소스입니다.
 * CBO 프로젝트에 커밋을 진행하나, poaseal 혹은 poa-doceditor 프로젝트에 servlet_src에 위치시킨 뒤 컴파일하여 배포하시길 바랍니다.
 * @author Sepoa
 *
 */
public class DownloadService extends SepoaMultipart {
    public void doGet(HttpServletRequest request, HttpServletResponse res)  {
    	String attachKey = request.getParameter("attach_key");
    	String attachSeq = request.getParameter("attach_seq");
    	
    	Map<String, String> data = new HashMap<String, String>();
    	data.put("attach_key", attachKey);
    	data.put("attach_seq", attachSeq);
    	
    	try {
    		SepoaInfo info = new SepoaInfo("019", "COMPANY_CODE=1000^@^ID=SYSTEM^@^LANGUAGE=EN^@^NAME_LOC=SYSTEM^@^NAME_ENG=SYSTEM^@^DEPT=ALL^@^USER_TYPE=S^@^");
        	String prcId = "sepoa.svc.fi.FI_001";
            String prcMethod = "getSFileInfo";
        	
        	Object[] obj = { data };
        	SepoaFormater sf = new SepoaFormater(ServiceConnector.doService(info, prcId, "TRANSACTION", prcMethod, obj).result[0]);

			if (sf.getRowCount() > 0) {
				Map<String, String> fileData = sf.getListMap().get(0);
				String filePath = getTypePath(fileData.get("TYPE"), "download") + "/" + fileData.get("DES_FILE_NAME");

				File file = new File(filePath);
				FileInputStream fis = new FileInputStream(file);
				int fileSize = fis.available();

				// 다운로드할 파일의 MIME 타입을 설정합니다.
				res.setContentType("application/octet-stream");
				res.setContentLength(fileSize);
				res.setHeader("Content-Disposition", "attachment;" + getFileName(fileData.get("SRC_FILE_NAME"), request));

				// 클라이언트로 파일을 전송합니다.
				OutputStream os = res.getOutputStream();
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = fis.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}

				os.close();
				fis.close();
			} else {
        		throw new Exception("Not Found File!");
        	}
        	
        	System.out.println(sf.getListMap());
    	} catch (Exception e) {
    		e.printStackTrace();
            Logger.err.println("DownloadService", this, e.getMessage());
            res.reset();
            res.setContentType("text/html");
            res.setHeader("Content-Type", "text/html; charset=UTF-8");
            try {
                res.getWriter().println("<script>alert('File download ERROR');window.close();</script>");
                res.flushBuffer();
            } catch (IOException e1) {
                e1.printStackTrace();
                Logger.err.println("DownloadService", this, e.getMessage());
            }
    	}
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
}