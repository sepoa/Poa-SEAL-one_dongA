package sepoa.svl.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.Gson;

import filters.ThreadLocalFilter;
import net.sf.jxls.exception.ParsePropertyException;
import net.sf.jxls.transformer.XLSTransformer;
import sepoa.dhtmlx.basic.BasicDataStructure;
import sepoa.dhtmlx.grid.GridDataStructure;
import sepoa.dhtmlx.operator.SepoaDataOperator;
import sepoa.dhtmlx.operator.SepoaDataStructure;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.drm.fasoo2.Fasoo2DRM;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.RequestWrapper;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;

public class SepoaExcel extends HttpServlet {

	private static final long serialVersionUID = -8506679708547027830L;

	public void init(ServletConfig config) throws ServletException {
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		doPost(req, res);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		try {
			SepoaInfo info = SepoaSession.getAllValue(req);

			req.setCharacterEncoding("UTF-8");
			res.setContentType("text/html;charset=UTF-8"); 

			RequestWrapper rw = new RequestWrapper(req);
			String excelType = rw.getParameter("excelType");
			if ("up".equals(excelType)) {
				SepoaDataOperator.write(fileUpload(req, res, rw), res);
			} else if ("down".equals(excelType)) {
				fileDownload(req, res, rw);
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public SepoaDataStructure fileUpload(HttpServletRequest req, HttpServletResponse res, RequestWrapper xls_file)
			throws Exception {
//		BasicDataStructure result = new BasicDataStructure(req);
		GridDataStructure result = new GridDataStructure();
		try {
			SepoaInfo info = SepoaSession.getAllValue(req);
			FileItem excel_item = xls_file.getFileItem("excelUploadInput");

			File tempDir = FileUtils.getTempDirectory();
			File tempFile = new File(tempDir, String.valueOf(System.nanoTime()));

			FileOutputStream fos = FileUtils.openOutputStream(tempFile);
			IOUtils.copy(excel_item.getInputStream(), fos);
			IOUtils.closeQuietly(fos);

			File excelFile = doDrm(info, tempFile);

			String template = xls_file.getParameter("template_file");
			int headerRowIndex = 5;
			int dataRowIndex = 8;
			int sheetIndex = 0;

			if ("GRID_DEFAULT".equals(template)) {
				headerRowIndex = 0;
				dataRowIndex = 1;
			} else {
				try {
					headerRowIndex = Integer.parseInt(xls_file.getParameter("HEADER_ROW_INDEX"));
				} catch (Exception e) {
					headerRowIndex = 5;
				}

				try {
					dataRowIndex = Integer.parseInt(xls_file.getParameter("DATA_ROW_INDEX"));
				} catch (Exception e) {
					dataRowIndex = 8;
				}
			}
			try {
				sheetIndex = Integer.parseInt(xls_file.getParameter("SHEET_INDEX"));
			} catch (Exception e) {
				sheetIndex = 0;
			}

			Workbook workbook = null;
			InputStream is = null;
			try {
				is = FileUtils.openInputStream(excelFile);
				workbook = new HSSFWorkbook(is);
			} catch (Exception e) {
				IOUtils.closeQuietly(is);
				is = FileUtils.openInputStream(excelFile);
				OPCPackage pkg = OPCPackage.open(is);
				workbook = new XSSFWorkbook(pkg);
			} finally {
				IOUtils.closeQuietly(is);
				FileUtils.forceDelete(excelFile);
			}
			FormulaEvaluator eval = workbook.getCreationHelper().createFormulaEvaluator();

			Sheet sheet = workbook.getSheetAt(sheetIndex);
			Row row = null;
			int row_count = sheet.getLastRowNum();
			Row headerRow = sheet.getRow(headerRowIndex);
			int cell_count = headerRow.getLastCellNum();

			Map<String, Object> allData = new HashMap<String, Object>();
			Map<String, String> headerData = new HashMap<String, String>();

			List<Map<String, String>> detailData = new ArrayList<Map<String, String>>();
			allData.put(SepoaDataMapper.KEY_HEADER_DATA, headerData);
			allData.put(SepoaDataMapper.KEY_GRID_DATA, detailData);

			Enumeration e = xls_file.getParameterNames();
			while (e.hasMoreElements()) {
				String paramName = (String) e.nextElement();
				headerData.put(paramName, xls_file.getParameter(paramName));
			}

			for (int i = dataRowIndex; i <= row_count; i++) {
				row = sheet.getRow(i);
				Map<String, String> rowData = new HashMap<String, String>();
				int blank_check = 0;
				if (row != null) {
					for (int j = 0; j < cell_count; j++) {
						if (!"".equals(getCellValue(headerRow.getCell(j), eval))) {
							String c_value = getCellValue(row.getCell(j), eval);
							rowData.put(getCellValue(headerRow.getCell(j), eval), c_value);
							if (c_value == null || "".equals(c_value.trim())) {
								blank_check++;
							}

						} else {
							blank_check++;
						}
					}
				}
				if (rowData != null && !rowData.isEmpty() && blank_check != cell_count) {
					detailData.add(rowData);
				}

			}

			String returnDataFlag = xls_file.getParameter("RETURN_DATA");
			if ("true".equals(returnDataFlag)) {
//				result.setStatus("true");
//				result.addData(new Gson().toJson(detailData));
				result.addUserData(SepoaDataStructure.MESSAGE, Cache.text.get("MESSAGE.0001"));
				result.addUserData(SepoaDataStructure.STATUS, "true");
			} else {
				String process_id = xls_file.getParameter("PROCESS_ID");
				String method = xls_file.getParameter("METHOD");

				Object[] obj = { allData };
				SepoaOut value = ServiceConnector.doService(info, process_id, "TRANSACTION", method, obj);
				if (value.flag) {
//					result.setStatus("true");
					result.addUserData(SepoaDataStructure.MESSAGE, Cache.text.get("MESSAGE.0001"));
					result.addUserData(SepoaDataStructure.STATUS, "true");
				} else {
					result.addUserData(SepoaDataStructure.MESSAGE,
							(value.message == null) ? Cache.text.get("MESSAGE.1002") : value.message);
					result.addUserData(SepoaDataStructure.STATUS, "false");
				}

				if (value.params != null && value.params.size() > 0) {
					result.addUserData(value.params);
				}

				for (int i = 0; i < value.getDataCount(); i++) {
					Object o = value.getData(i);
//					result.addData((o == null) ? "" : o.toString());
				}
			}
		} catch (Throwable e) {
			// e.printStackTrace();

			result.addUserData(SepoaDataStructure.MESSAGE, e.getMessage());
			result.addUserData(SepoaDataStructure.STATUS, "false");
		}
		return result;
	}

	public void fileDownload(HttpServletRequest req, HttpServletResponse res, RequestWrapper rw) throws Exception {
		try {
			SepoaInfo info = SepoaSession.getAllValue(req);
			Map<String, Object> allData = new HashMap<String, Object>();
			Map<String, String> headerData = new HashMap<String, String>();
			List<Map<String, String>> detailData = new ArrayList<Map<String, String>>();
			allData.put(SepoaDataMapper.KEY_HEADER_DATA, headerData);
			allData.put(SepoaDataMapper.KEY_GRID_DATA, detailData);

			Object[] obj = { allData };

			String process_id = rw.getParameter("PROCESS_ID");
			String method = rw.getParameter("METHOD");
			String templateFile = rw.getParameter("template_file");

			@SuppressWarnings("rawtypes")
			Enumeration e = rw.getParameterNames();
			while (e.hasMoreElements()) {
				String paramName = (String) e.nextElement();
				try {
					headerData.put(paramName, URLDecoder.decode(rw.getParameter(paramName), "UTF-8"));
				} catch (Throwable t) {
					headerData.put(paramName, rw.getParameter(paramName));
				}
			}

			SepoaOut value = ServiceConnector.doService(info, process_id, "CONNECTION", method, obj);

			if (value.flag) {
				SepoaFormater sf = new SepoaFormater(value.result[0]);
				if (sf.getRowCount() > 0) {
					for (int i = 0; i < sf.getRowCount(); i++) {
						Map<String, String> rowData = new HashMap<String, String>();
						for (int j = 0; j < sf.getColumnCount(); j++) {
							rowData.put(sf.getColumnNames(true)[j], sf.getValue(i, j));
						}
						detailData.add(rowData);
					}
				} else {
					Map<String, String> rowData = new HashMap<String, String>();
					detailData.add(rowData);
				}
				ServletContext sc = ThreadLocalFilter.getHttpServletRequest().getSession().getServletContext();

				Configuration conf = new Configuration();
				String browser = getBrowser(req);

				String course = conf.getString("sepoa.attach.download");

				String file_name = templateFile;
				String filename = getFileName(file_name, browser);

				String templateFileName = sc.getRealPath("/WEB-INF/xls/" + templateFile);
				String destFileName = course + "/" + System.currentTimeMillis() + filename;

				XLSTransformer transformer = new XLSTransformer();

				transformer.transformXLS(templateFileName, allData, destFileName);

				res.reset();
				File file = new File(destFileName);

				res.setContentType("application/smnet");
				res.setHeader("Content-Type", "application/smnet; charset=UTF-8");
				res.setHeader("Content-Disposition",
						"attachment; " + getFileName(System.currentTimeMillis() + " " + templateFile, req));
				res.setHeader("Content-Transfer-Encoding", "binary");
				res.setHeader("Content-Length", file.length() + "");

				FileInputStream fileInputStream = new FileInputStream(file);
				ServletOutputStream servletOutputStream = res.getOutputStream();

				byte b[] = new byte[1024];
				int data = 0;

				while ((data = (fileInputStream.read(b))) > 0) {
					servletOutputStream.write(b, 0, data);
					servletOutputStream.flush();
				}

				res.flushBuffer();
				servletOutputStream.close();
				fileInputStream.close();

			}
		} catch (ParsePropertyException e) {
			// e.printStackTrace();
		} catch (InvalidFormatException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			// e.printStackTrace();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	private String getBrowser(HttpServletRequest request) {
		String header = request.getHeader("User-Agent");
		if (header.indexOf("MSIE") > -1 || header.indexOf("Trident") > -1) {
			return "MSIE";
		} else if (header.indexOf("Chrome") > -1) {
			return "Chrome";
		} else if (header.indexOf("Opera") > -1) {
			return "Opera";
		}
		return "Firefox";
	}

	private String getFileName(String filename, String browser) throws Exception {
		String encodedFilename = null;

		if (browser.equals("MSIE")) {
			encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
		} else if (browser.equals("Firefox")) {
			encodedFilename = new String(filename.getBytes("UTF-8"), "8859_1");
		} else if (browser.equals("Opera")) {
			encodedFilename = new String(filename.getBytes("UTF-8"), "8859_1");
		} else if (browser.equals("Chrome")) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < filename.length(); i++) {
				char c = filename.charAt(i);
				if (c > '~') {
					sb.append(URLEncoder.encode("" + c, "UTF-8"));
				} else {
					sb.append(c);
				}
			}
			encodedFilename = sb.toString();
		} else {
			throw new RuntimeException("Not supported browser");
		}

		return encodedFilename;
	}

	private String getCellValue(Cell cell, FormulaEvaluator eval) {
		String result_value = "";
		BigDecimal num_dec = null;

		if (cell != null) {
			switch (cell.getCellType()) {
			case Cell.CELL_TYPE_FORMULA:

				CellValue cellValue = eval.evaluate(cell);

				switch (cellValue.getCellType()) {
				case Cell.CELL_TYPE_NUMERIC:
					if (HSSFDateUtil.isCellDateFormatted(cell)) {
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						result_value = formatter.format(cell.getDateCellValue());
						result_value = SepoaString.getDateUnDashFormat(result_value);
					} else {
						num_dec = new BigDecimal(cellValue.getNumberValue());
						result_value = num_dec.toString();

						int point_index = result_value.indexOf(".");
						if (point_index >= 0) {
							result_value = String.valueOf(cellValue.getNumberValue());
						}
					}
					break;
				case Cell.CELL_TYPE_STRING:
					result_value = cellValue.getStringValue();
					break;
				case Cell.CELL_TYPE_BLANK:
					result_value = "";
					break;
				case Cell.CELL_TYPE_BOOLEAN:
					result_value = cellValue.getBooleanValue() + "";
					break;
				case Cell.CELL_TYPE_ERROR:
					result_value = cellValue.getErrorValue() + "";
					break;
				}
				break;
			case Cell.CELL_TYPE_NUMERIC:
				// cell의 값이 numeric일 경우 날짜와 숫자 두가지일 경우이다. 아래와 같이 확인
				if (HSSFDateUtil.isCellDateFormatted(cell)) {

					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
					result_value = formatter.format(cell.getDateCellValue());
					result_value = SepoaString.getDateUnDashFormat(result_value);
				} else {
					num_dec = new BigDecimal(cell.getNumericCellValue());
					result_value = num_dec.toString();
					int point_index = result_value.indexOf(".");
					if (point_index >= 0) {
						result_value = String.valueOf(cell.getNumericCellValue());
					}
				}

				break;
			case Cell.CELL_TYPE_STRING:
				result_value = cell.getStringCellValue();
				break;
			case Cell.CELL_TYPE_BLANK:
				result_value = "";
				break;
			case Cell.CELL_TYPE_BOOLEAN:
				result_value = cell.getBooleanCellValue() + "";
				break;
			case Cell.CELL_TYPE_ERROR:
				result_value = cell.getErrorCellValue() + "";
				break;
			default:
			}
		}

		return result_value.trim();

	}

	private File doDrm(SepoaInfo info, File orgFile) throws Exception {
		
		Config conf = new Configuration();

		Fasoo2DRM drm = new Fasoo2DRM();
		drm.setSession(info);
		drm.setConfig(conf);

		File destFile = null;

		destFile = new File(FileUtils.getTempDirectory() + "/extract_" + orgFile.getName());

		Logger.info.println("Fasoo2 > Set Original File : " + orgFile);
		drm.setOrgFile(orgFile);
		Logger.info.println("DRMPlugin > Set Dest File : " + destFile);
		drm.setDestFile(destFile);

		if (drm.doExtract()) {
			FileUtils.forceDelete(orgFile);
			return destFile;
		} else {
			return orgFile;
		}
	}

	private String getFileName(String fileName, HttpServletRequest request) {
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
}
