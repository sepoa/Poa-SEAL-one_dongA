package sepoa.svl.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import filters.SecurityRequst;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.svl.util.SepoaMultipart;

public class DeployManagement extends SepoaMultipart {
	private static final long serialVersionUID = 8614518419219098054L;

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		doPost(req, res);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		//System.out.println("DeployManagement doPost::::");
		SepoaInfo info = sepoa.fw.ses.SepoaSession.getAllValue(req);

		String contentType = req.getContentType();
		if (contentType != null && contentType.toLowerCase().contains("multipart")) {
			fileUpload(req, res, info);
		} else {
			execute(req, res, info);
		}
	}

	@SuppressWarnings("unchecked")
	public void fileUpload(HttpServletRequest req, HttpServletResponse res, SepoaInfo info) {
		res.setContentType("text/html;charset=UTF-8");
		res.setCharacterEncoding("UTF-8");

		String targetTempDir = req.getParameter("TARGET_TEMP_DIR");
		String toDir = req.getParameter("TO_DIR");
		String makeDir = req.getParameter("MAKE_DIR");

		Map<String, Object> returnData = new HashMap<String, Object>();
		int status = 1;
		String message = "SUCCESS";
		try {
			File outFile = null;
			if ("true".equals(makeDir)) {
				File tempDir = new File(targetTempDir);
				if (!tempDir.exists()) {
					tempDir.mkdirs();
				}
				File outFileDir = new File(targetTempDir);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				String date = sdf.format(new Date());
				int cnt = 1;
				while (true) {
					outFile = new File(outFileDir, date + "_" + cnt);
					if (!outFile.exists()) {
						FileUtils.forceMkdir(outFile);
						break;
					} else {
						cnt++;
					}
				}
			} else {
				outFile = new File(targetTempDir);
			}

			// File Size
			int maxSize = 1024 * 1024 * 500;
			MultipartRequest multi = new MultipartRequest(req, outFile.getAbsolutePath(), maxSize, "utf-8",
					new DefaultFileRenamePolicy());

			Enumeration<String> files = multi.getFileNames();

			String fileName = "";
			while (files.hasMoreElements()) {
				fileName = (String) files.nextElement();

				File file = multi.getFile(fileName);
				String dirName = StringUtils.removeEnd(file.getName(), ".zip");
				outFile = new File(outFile, dirName);
				decompress(file, outFile);

				File to = new File(toDir);
				if (!to.exists()) {
					FileUtils.forceMkdir(to);
				}
				move(outFile, new File(toDir));
			}
			returnData.put("outDir", outFile.getAbsolutePath());

		} catch (Exception e) {
			returnData.put("value", null);
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		} finally {
			PrintWriter out = null;
			try {
				out = res.getWriter();
			} catch (IOException e) {
				status = 1;
				e.printStackTrace();
				Logger.err.println(info.getSession("ID"), this, e);
			}
			Logger.debug.println("STATUS : " + status);
			returnData.put("status", status);
			returnData.put("message", message);
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			ObjectMapper jsonMapper = new ObjectMapper();
			if(out != null) {
				try {
					out.println(jsonMapper.writeValueAsString(list));
				} catch (JsonProcessingException e) {
					Logger.err.println("Error : " + e);
				}
				out.flush();
			}
		}

	}

	private static void decompress(File zipFile, File dir) throws Exception {
		if (!dir.exists())
			FileUtils.forceMkdir(dir);
		FileInputStream fis;
		fis = new FileInputStream(zipFile);
		ZipInputStream zis = new ZipInputStream(fis);
		ZipEntry ze = zis.getNextEntry();
		while (ze != null) {
			String fileName = ze.getName();
			File newFile = new File(dir, fileName);
			FileUtils.forceMkdir(new File(newFile.getParent()));
			FileOutputStream fos = new FileOutputStream(newFile);
			IOUtils.copy(zis, fos);
			fos.close();
			zis.closeEntry();
			ze = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
		fis.close();
	}

	private void move(File fromDir, File toDir) throws Exception {
		if (!toDir.exists()) {
			FileUtils.forceMkdir(toDir);
		}
		FileUtils.copyDirectory(fromDir, toDir);
	}

	private void execute(HttpServletRequest req, HttpServletResponse res, SepoaInfo info)
			throws ExecuteException, IOException {
		String cmds = ((SecurityRequst) req).getRawParameter("cmds");
		cmds = StringEscapeUtils.unescapeHtml(cmds);

		List<Map<String, String>> result = new LinkedList<Map<String, String>>();

		try {
			String[] commands = cmds.split("\n");

			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ExecuteStreamHandler stream = new PumpStreamHandler(bos, bos, null);

			for (String line : commands) {
				CommandLine cmdLine = CommandLine.parse(line);
				DefaultExecutor executor = new DefaultExecutor();
				executor.setStreamHandler(stream);
				executor.setExitValue(1);
				try {
					executor.execute(cmdLine, resultHandler);
					resultHandler.waitFor();
				} catch (Exception e) {
					new RuntimeException("Command execution failed. [" + cmdLine + "]", e);
				}
				Map<String, String> map = new HashMap<String, String>();
				map.put("EXIT_VALUE", String.valueOf(resultHandler.getExitValue()));
				// String encoding = System.getProperty("sun.jnu.encoding");
				String encoding = System.getProperty("file.encoding");
				map.put("RESULT", new String(bos.toByteArray(), encoding));
				result.add(map);
			}
		} finally {
			PrintWriter out = null;
			try {
				out = res.getWriter();
			} catch (IOException e) {
				e.printStackTrace();
				Logger.err.println(info.getSession("ID"), this, e);
			}
			
			Map<String, Object> returnData = new HashMap<String, Object>();
			returnData.put("result", result);
			ObjectMapper jsonMapper = new ObjectMapper();
			if(out != null) {
				try {
					out.println(jsonMapper.writeValueAsString(result));
				} catch (JsonProcessingException e) {
					Logger.err.println("Error : " + e);
				}
				out.flush();
			}

		}
	}
}
