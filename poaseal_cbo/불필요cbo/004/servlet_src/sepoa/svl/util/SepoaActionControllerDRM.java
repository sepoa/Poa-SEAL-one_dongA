package sepoa.svl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;

import com.google.gson.Gson;

import filters.ThreadLocalFilter;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.drm.fasoo2.Fasoo2DRM;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

public class SepoaActionControllerDRM extends HttpServlet {
	final static protected String KEY_PROCESS_ID = "module";
	final static protected String KEY_METHOD = "action";
	final static protected String KEY_CONNECTION = "CONNECTION";
	final static public String KEY_RETURN_TYPE = "return_type";
	final static public String RETURN_TYPE_FILE = "file";

	protected class ServiceInfo {
		public String PROCESS_ID;
		public String METHOD;
		public String CONNECTION;

		public ServiceInfo extend(ServiceInfo ex) {
			if (ex.CONNECTION != null && !ex.CONNECTION.isEmpty()) {
				this.CONNECTION = ex.CONNECTION;
			}
			if (ex.METHOD != null && !ex.METHOD.isEmpty()) {
				this.METHOD = ex.METHOD;
			}

			if (ex.PROCESS_ID != null && !ex.PROCESS_ID.isEmpty()) {
				this.PROCESS_ID = ex.PROCESS_ID;
			}
			return this;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1885103796329022118L;

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		this.doProcess(req, res, false);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		this.doProcess(req, res, true);
	}

	@SuppressWarnings("unchecked")
	private void doProcess(HttpServletRequest req, HttpServletResponse res, boolean postFlag) throws ServletException {
		Config config = null;
		try {
			config = new Configuration();
		} catch (Exception e) {
			throw new ServletException("SepoaActionController get configuration rrror", e);
		}

		SepoaModuleDataMapper dataMapper = new SepoaModuleDataMapper();
		dataMapper.setConfig(config);
		dataMapper.setReqRes(req, res);
		Map<String, Object> allData = dataMapper.getData();
		String fileTempDirectory = (String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY);

		Map<String, Object> result = new HashMap<String, Object>();
		OutputStream os = null;
		InputStream in = null;
		SepoaInfo info = checkSession(allData);
		try {
			if (info != null) {
				ServiceInfo sInfo = this.getServiceInfo(allData);
				if (sInfo.PROCESS_ID != null && !sInfo.PROCESS_ID.isEmpty()) {
					String processID = config.get("sepoa.action.controller." + sInfo.PROCESS_ID, null);
					if (!StringUtils.isEmpty(processID)) {
						sInfo.PROCESS_ID = processID;
					}
				}
				if (sInfo.PROCESS_ID == null || sInfo.PROCESS_ID.isEmpty()) {
					throw new Exception("not found process id. please check parameters");
				}
				if (sInfo.METHOD == null || sInfo.METHOD.isEmpty()) {
					throw new Exception("not found method. please check parameters");
				}
				if (sInfo.CONNECTION == null || sInfo.CONNECTION.isEmpty()) {
					sInfo.CONNECTION = "TRANSACTION";
					Logger.warn.println("not found connection type. work in TRANSACTION mode. please check parameters");
					Logger.warn.println("not found connection type. work in TRANSACTION mode. please check parameters");
				}

				Object[] obj = { allData };
				SepoaOut so = ServiceConnector.doService(info, sInfo.PROCESS_ID, sInfo.CONNECTION, sInfo.METHOD, obj);
				Logger.debug.println("@@@ so : "+so);
				Logger.debug.println("@@@ so : "+so.flag); 
				Logger.debug.println("@@@ so : "+so.getDataCount()); 
				Logger.debug.println("@@@ so : "+so.getData(0));
				if (RETURN_TYPE_FILE.equals(so.params.get(KEY_RETURN_TYPE))) {
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
							res.setHeader("Content-Disposition",
									(postFlag ? "attachment; " : "") + getFileName(fileName, req));
							res.setHeader("Content-Transfer-Encoding", "binary");
							in = CipherManager.getCipher().getInputStream(targetFile, false);

							File orgFile = new File(
									FileUtils.getTempDirectory() + "/org_" + targetFile.getName() + ".pdf");
							FileOutputStream fos = new FileOutputStream(orgFile);
							IOUtils.copy(in, fos);
							IOUtils.closeQuietly(in);
							IOUtils.closeQuietly(fos);

							File drmFile = new File(
									FileUtils.getTempDirectory() + "/package_" + targetFile.getName() + ".pdf");
							try {
								if (drmFile.exists()) {
									FileUtils.forceDelete(drmFile);
								}
							} catch (Exception e) {
								Logger.err.println(e);
							}

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

							in = FileUtils.openInputStream(drmFile);

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
							try {
								for (File file : fileList) {
									FileUtils.forceDelete(file);
								}
							} catch (Exception e) {
								Logger.err.println(e);
							}
						} else {
							RequestDispatcher rd = req.getRequestDispatcher(redirect);
							rd.forward(req, res);
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
			} else {
				result.put("status", 0);
				result.put("message", "not have permission");
				resultWriter(res, result);
			}
		} catch (Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			result.put("status", 0);
			result.put("message", e.getMessage());
			resultWriter(res, result);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (Exception e) {
				}
			if (os != null)
				try {
					os.close();
				} catch (Exception e) {
				}
			try {
				FileUtils.forceDelete(new File(fileTempDirectory));
			} catch (IOException e) {
				Logger.debug.println("poascan temp file delete : fail" + fileTempDirectory);
			}
		}
	}

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

	@SuppressWarnings("unchecked")
	private SepoaInfo checkSession(Map<String, Object> allData) {
		HttpServletRequest req = (HttpServletRequest) allData.get(SepoaModuleDataMapper.KEY_REQUEST);
		Config conf = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);

		SepoaInfo info = SepoaSession.getAllValue(req);
		String id = info.getSession("ID");
		if (id == null || id.isEmpty()) {
			if (Arrays.asList(conf.getString("sepoa.action.without.session.ip", "").split(";"))
					.contains(ThreadLocalFilter.getClientIP())) {
				Map<String, String> headerData = (Map<String, String>) allData
						.get(SepoaModuleDataMapper.KEY_HEADER_DATA);

				String houseCode = headerData.get("HOUSE_CODE");

				if (StringUtils.isNotBlank(info.getSession("HOUSE_CODE"))) {
					houseCode = info.getSession("HOUSE_CODE");
				}

				if (houseCode == null || houseCode.isEmpty()) {
					houseCode = "000";
				}

				String companyCode = headerData.get("COMPANY_CODE");
				if (companyCode == null || companyCode.isEmpty()) {
					companyCode = "1000";
				}

				id = headerData.get("ID");
				if (id == null || id.isEmpty()) {
					id = "SYSTEM";
				}
				info = new SepoaInfo(houseCode, "COMPANY_CODE=" + companyCode + "^@^ID=" + id
						+ "^@^LANGUAGE=EN^@^NAME_LOC=SYSTEM^@^NAME_ENG=SYSTEM^@^DEPT=ALL^@^USER_TYPE=S^@^");
			} else {
				info = null;
			}
		}
		return info;
	}

	protected String getFileName(String fileName, HttpServletRequest request) {
		String userAgent = request.getHeader("User-Agent");
		String result = fileName;
		try {
			StringBuffer fileParameter = new StringBuffer();
			if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
				fileParameter.append("filename=").append("\"")
						.append(URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "\\ ")).append("\"");
			} else {
				fileParameter.append("filename=").append(URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20"));
			}
			result = fileParameter.toString();
		} catch (UnsupportedEncodingException e) {
			Logger.err.println("error occurred while processing content-disposition filename", e);
		}
		return result;
	}

	protected ServiceInfo getServiceInfo(Map<String, Object> data) {
		return getServiceInfo(data, KEY_PROCESS_ID, KEY_METHOD, KEY_CONNECTION);
	}

	@SuppressWarnings("unchecked")
	protected ServiceInfo getServiceInfo(Map<String, Object> data, String k_process, String k_method,
			String k_connection) {
		HttpServletRequest req = (HttpServletRequest) data.get(SepoaDataMapper.KEY_REQUEST);
		Map<String, String> headerData = (Map<String, String>) data.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);

		ServiceInfo sInfo = new ServiceInfo();

		sInfo.PROCESS_ID = req.getParameter("raw_" + k_process);
		if (sInfo.PROCESS_ID == null) {
			sInfo.PROCESS_ID = MapUtils.getString(headerData, KEY_PROCESS_ID);
		}
		sInfo.METHOD = req.getParameter("raw_" + k_method);
		if (sInfo.METHOD == null) {
			sInfo.METHOD = headerData.get(k_method);
		}
		sInfo.CONNECTION = req.getParameter("raw_" + k_connection);
		if (sInfo.CONNECTION == null) {
			sInfo.CONNECTION = headerData.get(k_connection);
		}
		return sInfo;
	}
}
