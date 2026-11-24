package sepoa.svc.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.admin.MultipartUploader;

@SuppressWarnings({ "unused", "unchecked" })
@Description("Deploy관리")
public class AD_350 extends SepoaService {

	public AD_350(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("조회")
	public SepoaOut doList(Map<String, Object> allData) {

		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("헤더조회")
	public SepoaOut doQueryHeader(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("조회")
	public SepoaOut doQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));

			Map<String, String> returnMap = new HashMap<String, String>();
			returnMap.put("returnType", "param");
			sxp = new SepoaXmlParser(this, "doQueryHeader");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			returnMap.put("headerData", ssm.doSelect(headerData));
			setObject(returnMap);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("삭제")
	public SepoaOut doDelete(Map<String, Object> allData) {

		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);

			if (gridData == null) {
				sxp = new SepoaXmlParser(this, "doDelete_SDCGL");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);

				sxp = new SepoaXmlParser(this, "doDelete_SDCLN");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);
			} else {
				sxp = new SepoaXmlParser(this, "doDelete_SDCLN");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(gridData);
			}

			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("저장")
	public SepoaOut doSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		SepoaFormater sf1 = null;

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,
					SepoaDataMapper.KEY_GRID_DATA);

			List<Map<String, String>> DPLNList = new ArrayList<Map<String, String>>();
			List<Map<String, String>> DPSEList = new ArrayList<Map<String, String>>();
			for (Map<String, String> map : gridData) {
				if ("*".equals(map.get("TREE_PARENT_ID"))) {
					DPLNList.add(map);
				} else {
					DPSEList.add(map);
				}
			}

			String mode = headerData.get("SAVE_MODE");
			if ("INSERT".equals(mode)) {
				String deployId = DocumentUtil.getDocNumberRemote(info, "DP").result[0];
				headerData.put("DEPLOY_ID", deployId);

				sxp = new SepoaXmlParser(this, "doInsert_SDPGL");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(headerData);

				if (DPLNList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsert_SDPLN");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(DPLNList, headerData);

					if (DPSEList.size() > 0) {
						sxp = new SepoaXmlParser(this, "doInsert_SDPSE");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(DPSEList, headerData);
					}
				}

			} else if ("UPDATE".equals(mode)) {
				sxp = new SepoaXmlParser(this, "doUpdate_SDPGL");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(headerData);

				sxp = new SepoaXmlParser(this, "doDelete_SDPLN");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);

				sxp = new SepoaXmlParser(this, "doDelete_SDPSE");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);

				if (DPLNList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsert_SDPLN");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(DPLNList, headerData);

					if (DPSEList.size() > 0) {
						sxp = new SepoaXmlParser(this, "doInsert_SDPSE");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(DPSEList, headerData);
					}
				}
			}
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("실행")
	public SepoaOut doExecute(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,
					SepoaDataMapper.KEY_GRID_DATA);

			String sourceTempDir = headerData.get("SOURCE_TEMP_DIR");
			String targetTempDir = headerData.get("TARGET_TEMP_DIR");
			String serverUrl = headerData.get("SERVER_URL");

			Logger.debug.println(gridData);

			Map<Integer, Map<String, String>> rootMap = new TreeMap<Integer, Map<String, String>>();
			Map<String, Map<Integer, Map<String, String>>> cacheMap = new HashMap<String, Map<Integer, Map<String, String>>>();
			for (Map<String, String> map : gridData) {
				map.put("TREE_ID", map.get("MODULE_NAME_TMP"));
				String treeParentID = map.get("TREE_PARENT_ID");
				if ("*".equals(treeParentID)) {
					Map<Integer, Map<String, String>> inMap = cacheMap.get(treeParentID);
					if (inMap == null) {
						String treeId = map.get("TREE_ID");
						Integer order = Integer.parseInt(map.get("ORDER"));
						inMap = new TreeMap<Integer, Map<String, String>>();
						rootMap.put(order, map);
						cacheMap.put(treeId, inMap);
					}
				}
			}
			for (Map<String, String> map : gridData) {
				String treeParentID = map.get("TREE_PARENT_ID");
				if (!"*".equals(treeParentID)) {
					Map<Integer, Map<String, String>> inMap = cacheMap.get(treeParentID);
					Integer order = Integer.parseInt(map.get("SEQ"));
					Logger.debug.println(map + " into " + inMap);
					inMap.put(order, map);
				}
			}

			File outFileDir = new File(sourceTempDir);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			String date = sdf.format(new Date());
			int cnt = 1;
			File outFile = null;
			while (true) {
				outFile = new File(outFileDir, date + "_" + cnt);
				if (!outFile.exists()) {
					FileUtils.forceMkdir(outFile);
					break;
				} else {
					cnt++;
				}
			}

			String outFilePath = outFile.getAbsolutePath();

			SepoaOut so = new SepoaOut();

			Collection<Map<String, String>> modules = rootMap.values();
			for (Map<String, String> rMap : modules) {
				Map<Integer, Map<String, String>> root = cacheMap.get(rMap.get("TREE_ID"));
				Collection<Map<String, String>> actions = root.values();

				String tmpDir = rMap.get("SOURCE_TEMP_DIR");

				for (Map<String, String> actionMap : actions) {
					if (StringUtils.isBlank(actionMap.get("SCRIPT"))) {
						String dir = rMap.get("FROM") + "/" + actionMap.get("FROM");
						String toDir = rMap.get("TO") + "/" + actionMap.get("TO");
						String tDir = outFilePath;
						String includes = actionMap.get("INCLUDE");
						String excludes = actionMap.get("EXCLUDE");
						String fileName = rMap.get("MODULE_NAME_TMP") + "_" + actionMap.get("FROM") + ".zip";

						Logger.debug.println(fileName);
						Logger.debug.println(tDir);
						File file = compress(dir, tDir, fileName, includes, excludes);
						Logger.debug.println(file);

						transfer(serverUrl, targetTempDir, toDir, file);
					} else {
						String cmds = actionMap.get("SCRIPT");
						execute(serverUrl, cmds);
					}
				}
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	private File compress(String dir, String tmpDir, String fileName, String include, String exclude) throws Exception {
		DirectoryScanner scanner = new DirectoryScanner();

		if (include != null) {
			scanner.setIncludes(include.split(","));
		}
		if (exclude != null) {
			Logger.debug.println("Exclude : (" + exclude.split(",").length + ")" + exclude);
			scanner.setExcludes(exclude.split(","));
		}
		scanner.setBasedir(dir);
		scanner.setCaseSensitive(false);
		scanner.scan();
		String[] files = scanner.getIncludedFiles();
		File outFile = new File(tmpDir, fileName);
		ZipArchiveOutputStream zaos = null;
		zaos = new ZipArchiveOutputStream(new FileOutputStream(outFile));
		zaos.setEncoding("UTF-8");

		File inFile = null;
		FileInputStream fis = null;
		for (int i = 0; i < files.length; i++) {
			inFile = new File(dir, files[i]);
			zaos.putArchiveEntry(new ZipArchiveEntry(files[i]));
			fis = new FileInputStream(inFile);
			IOUtils.copy(fis, zaos);
			IOUtils.closeQuietly(fis);
			zaos.closeArchiveEntry();
		}

		IOUtils.closeQuietly(zaos);

		return outFile;
	}

	private void transfer(String serverUrl, String targetTempDir, String toDir, File file) throws Exception {
		String url = serverUrl + "/DeployManagement.sux?TARGET_TEMP_DIR=" + URLEncoder.encode(targetTempDir, "UTF-8")
				+ "&TO_DIR=" + URLEncoder.encode(toDir, "UTF-8");
		Logger.debug.println(url);
		MultipartUploader uploader = new MultipartUploader(url, "UTF-8");
		uploader.addFilePart("fileUpload", file);
		List<String> response = uploader.finish();
		Logger.debug.println(response);
	}

	private void execute(String serverUrl, String cmds) throws Exception {
		String requestURL = serverUrl + "/DeployManagement.sux?cmds=" + URLEncoder.encode(cmds, "UTF-8");

		HttpURLConnection httpConn = null;
		OutputStream os = null;
		InputStream is = null;

		try {
			URL url = new URL(requestURL);
			httpConn = (HttpURLConnection) url.openConnection();

			httpConn.setUseCaches(false);
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);

			if (requestURL.toLowerCase().startsWith("https://")) {
				HttpsURLConnection httpsConn = (HttpsURLConnection) httpConn;

				// Set Hostname verification
				httpsConn.setHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						// Ignore host name verification. It always returns true.
						return true;
					}

				});

				// SSL setting
				SSLContext context = SSLContext.getInstance("TLS");
				context.init(null, new TrustManager[] { new X509TrustManager() {

					public void checkClientTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {
						Logger.debug.println("not used");
					}

					public void checkServerTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {
						Logger.debug.println("not used");
					}

					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}
				} }, null);
				httpsConn.setSSLSocketFactory(context.getSocketFactory());

				// Connect to host
				httpsConn.connect();
				httpsConn.setInstanceFollowRedirects(true);
			}

			os = httpConn.getOutputStream();
			IOUtils.write(cmds.getBytes("UTF-8"), os);

			is = httpConn.getInputStream();
			StringWriter sw = new StringWriter();
			IOUtils.copy(is, sw);

			Logger.debug.println("Result : " + sw.toString());

		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.println(e);
		} finally {
			IOUtils.closeQuietly(os);
			IOUtils.closeQuietly(is);
			try {
				if (httpConn != null) {
					httpConn.disconnect();
				}
			} catch (Exception e) {
				Logger.err.println(e);
			}
		}
	}
}