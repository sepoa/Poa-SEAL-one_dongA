package sepoa.svl.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.SepoaConnectionResource;
import sepoa.fw.drm.fasoo2.Fasoo2DRM;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaPDF;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

/**
 * @version 1.0
 * @author
 */
public class SepoaReportPDFDRM extends HttpServlet implements Servlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8676510793072319103L;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {		
		Config config = null;
		try {
			config = new Configuration();
		} catch (Exception e) {
			throw new ServletException("SepoaActionController get configuration error", e);
		}

		SepoaModuleDataMapper dataMapper = new SepoaModuleDataMapper();
		dataMapper.setConfig(config);
		dataMapper.setReqRes(req, resp);
		Map<String, Object> allData = dataMapper.getData();
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
		//config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
		try {
			byte[] bytes = null;
			bytes = getBytes(headerData, req);
			
			if(bytes == null) {
				throw new Exception("generate pdf fail");
			}

			generatePDFOutput(config, req, resp, bytes, MapUtils.getString(headerData, "FILE_NAME", String.valueOf(System.nanoTime())+".pdf"));
		} catch (Exception e) {
			e.printStackTrace();
			req.setAttribute("exception", e);
			getServletContext().getRequestDispatcher("/UxDefault/Error/SystemError.sux").forward(req, resp);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected byte[] getBytes(Map<String, String> headerData, HttpServletRequest req) throws Exception {
		String processId = headerData.get("module");
		String method    = headerData.get("action");
		
		Map<String, Object> allData = new HashMap<String, Object>();
		allData.put(SepoaDataMapper.KEY_HEADER_DATA, headerData);
		
		SepoaInfo info = SepoaSession.getAllValue(req);
		SepoaOut so = ServiceConnector.doService(info, processId, "CONNECTION", method, new Object[] { allData });
		
		if(("ireport").equals(headerData.get("mode"))) {
			Map<String, Object> data = (Map<String, Object>)so.obj;
			
			SepoaPDF pdf = new SepoaPDF();
			pdf.createPDF(data);
			return pdf.getByteArrayData();
		}else {
			return (byte[]) so.obj;
		}				
	}
	
	private void generatePDFOutput(Config config , HttpServletRequest req, HttpServletResponse resp, byte[] bytes,  String fileName) throws Exception {
		SepoaInfo info = SepoaSession.getAllValue(req);
		
		File orgFile = new File(FileUtils.getTempDirectory() + "/org_" + fileName + ".pdf");
		FileUtils.writeByteArrayToFile(orgFile, bytes);
		
		File drmFile = new File(FileUtils.getTempDirectory() + "/pkg_" + fileName + ".pdf");
		
		Fasoo2DRM drm = new Fasoo2DRM();
		drm.setSession(info);
		drm.setConfig(config);

		Logger.info.println("Fasoo2 > Set Original File : " + orgFile);
		drm.setOrgFile(orgFile);
		Logger.info.println("DRMPlugin > Set Dest File : " + drmFile);
		drm.setDestFile(drmFile);

		drm.doPackaging();
		List<File> fileList = new ArrayList<File>();
		fileList.add(orgFile);
		fileList.add(drmFile);


		ServletOutputStream ouputStream = resp.getOutputStream();
		
		byte[] drmBytes = FileUtils.readFileToByteArray(drmFile);
		
		resp.setContentType("application/pdf");
		resp.setContentLength(drmBytes.length);
		resp.setHeader("Content-Type", "application/pdf; charset=UTF-8");
		resp.setHeader("Content-Disposition", "attachment"+ SepoaMultipart.getFileName(fileName, req));
		resp.setHeader("Content-Length", String.valueOf(drmBytes.length));
		resp.setHeader("Accept-Ranges", "none");
		resp.setHeader("Cache-Control", "no-store");
		
		IOUtils.write(drmBytes, ouputStream);
		ouputStream.flush();
		ouputStream.close();
	}

	protected Connection getConnection() throws Exception {
		return new SepoaConnectionResource().getConnection();

	}
}