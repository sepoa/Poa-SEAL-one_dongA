package sepoa.custom.download;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;

import com.google.gson.Gson;

import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.util.SepoaMultipart;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;
/**
 * 해당 소스는 남부발전에 적용되는 파일 다운로드 소스입니다.
 * CBO 프로젝트에 커밋을 진행하나, poaseal 혹은 poa-doceditor 프로젝트에 servlet_src에 위치시킨 뒤 컴파일하여 배포하시길 바랍니다.
 * @author Sepoa
 *
 */
public class DownloadService extends SepoaMultipart {
	
	final static protected String KEY_PROCESS_ID = "module";
    final static protected String KEY_METHOD = "action";
    final static protected String KEY_CONNECTION = "CONNECTION";
    final static public String KEY_RETURN_TYPE = "return_type";
    final static public String RETURN_TYPE_FILE = "file";
    
    private void resultWriter(HttpServletResponse res, Map<String, Object> data) {
		res.setContentType("application/json;charset=UTF-8");
		res.setCharacterEncoding("UTF-8");
		try {
			PrintWriter out = res.getWriter();
			out.println(new Gson().toJson(data));
			out.flush();
		} catch (Exception e) {
			Logger.err.println(e);
		}
	}
	
    public void doGet(HttpServletRequest request, HttpServletResponse res)  {
    	
    	String contNo = request.getParameter("CONT_NO");
    	String contGlSeq = request.getParameter("CONT_GL_SEQ");
    	String contType = request.getParameter("CONT_TYPE");
    	
    	
    	Map<String, String> data = new HashMap<String, String>();
    	data.put("CONT_NO", contNo);
    	data.put("CONT_GL_SEQ", contGlSeq);
    	data.put("CONT_TYPE", contType);
    	
    	Map<String, Object> headerData = new HashMap<String, Object>();
    	headerData.put("headerData", data);
    	
    	OutputStream os = null;
		InputStream in = null;
    	
    	try {
    		
    		Config config = new Configuration();
    		
    		SepoaInfo info = new SepoaInfo("020", "COMPANY_CODE=1000^@^ID=SYSTEM^@^LANGUAGE=EN^@^NAME_LOC=SYSTEM^@^NAME_ENG=SYSTEM^@^DEPT=ALL^@^USER_TYPE=S^@^");
        	String prcId = "sepoa.svc.cbo.ct.SepoaCboContractService";
            String prcMethod = "doDocumentDownload";
            
            headerData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
        	
//        	Object[] obj = { data };
            Object[] obj = { headerData};
//        	SepoaFormater sf = new SepoaFormater(ServiceConnector.doService(info, prcId, "TRANSACTION", prcMethod, obj).result[0]);
            SepoaOut so = ServiceConnector.doService(info, prcId, "TRANSACTION", prcMethod, obj);
            
            Map<String, Object> result = new HashMap<String, Object>();
            

            if(RETURN_TYPE_FILE.equals(so.params.get(KEY_RETURN_TYPE))){
				if (so.flag && so.getDataCount() > 0) {
					res.reset();
					Map<String, String> fileData = (Map<String, String>) so.getData(0);
					String redirect = MapUtils.getString(fileData, "REDIRECT", "");
					if (redirect.isEmpty()) {
						String fileName = fileData.get("FILE_NAME_CLIENT");
						String filePath = fileData.get("FILE_PATH");

						File targetFile = new File(filePath);
						String contentType = new Tika().detect(targetFile);

						res.setContentType(contentType);
						res.setHeader("Content-Type", contentType + "; charset=UTF-8");
						res.setHeader("Content-Disposition", getFileName(fileName, request));
						res.setHeader("Content-Transfer-Encoding", "binary");
						in = CipherManager.getCipher().getInputStream(targetFile, false);

						int contentLength = 0;
						
						os = res.getOutputStream();
						byte b[] = new byte[8192];
						int leng = 0;
						while ((leng = in.read(b)) > 0) {
							contentLength += leng;
							os.write(b, 0, leng);
							os.flush();
						}
						res.setHeader("Content-Length", String.valueOf(contentLength));
						res.flushBuffer();
					} else {
						RequestDispatcher rd = request.getRequestDispatcher(redirect);
						rd.forward(request, res);
					}
				} else {
					res.reset();
					result.put("status", so.status);
					result.put("flag", so.flag);
					result.put("message", so.message);
					resultWriter(res, result);
				}
			} else {
				result.put("status", so.status);
				result.put("flag", so.flag);
				result.put("message", so.message);
				if (so.getDataCount() > 0) {
					result.put("result", so.getAllData());
				}
				resultWriter(res, result);
			}
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
    	} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
}