package sepoa.svl.common;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import javax.mail.internet.ContentDisposition;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.mime.MimeTypes;

import com.google.gson.Gson;

import sepoa.fw.log.Logger;
import sepoa.svl.util.SepoaMultipart;

public class RestUtil {
	private String charset = "UTF-8";
	private Boolean resultSaveFileFlag;
	private Boolean consoleLog = false;
	private Integer connectTimeLimit;
	private Integer responseCode = 0;
	private String filePathValue;
	private String urlStr = null;
	private Map<String, String> headerData = new HashMap<>();
	private Map<String, Object> paramsData = new HashMap<>();
	
	public final static String REQ_METHOD = "REQ_METHOD";
	public final static String CONNECT_TIME_LIMIT = "CONNECT_TIME_LIMIT";
	public final static String RESULT_SAVE_FILE = "RESULT_SAVE_FILE";
	public final static String FILE_SAVE_PATH = "FILE_SAVE_PATH";
	public final static String URL = "URL";
	
	private final static List<String> STATIC_LIST;
	static {
		STATIC_LIST = new ArrayList<>();
		STATIC_LIST.add(REQ_METHOD);
		STATIC_LIST.add(CONNECT_TIME_LIMIT);
		STATIC_LIST.add(RESULT_SAVE_FILE);
		STATIC_LIST.add(FILE_SAVE_PATH);
		STATIC_LIST.add(URL);
	}

	public RestUtil() {
	}

	public String getCharset() {
		return this.charset;
	}

	public RestUtil setCharset(String charset) {
		this.charset = charset;
		return this;
	}
	
	public RestUtil setconsoleLog(boolean flag) {
		this.consoleLog = flag;
		return this;
	}
	
	public Integer getResponseCode() {
		return this.responseCode;
	}

	public Map<String, String> getHeaderData() {
		return this.headerData;
	}

	public void setHeaderData(String key, String value) {
		this.headerData.put(key, value);
	}

	public RestUtil setHeaderData(Map<String, String> headerData) {
		this.headerData = headerData;
		return this;
	}

	public Map<String, Object> getParamsData() {
		return this.paramsData;
	}

	public void setParamsData(String key, String value) {
		this.paramsData.put(key, value);
	}

	public RestUtil setParamsData(Map<String, Object> paramsData) {
		this.paramsData = paramsData;
		return this;
	}

	private void initMemberVariables() {
		this.connectTimeLimit = MapUtils.getInteger(this.paramsData, CONNECT_TIME_LIMIT, 10000);
		this.resultSaveFileFlag = MapUtils.getBoolean(this.paramsData, RESULT_SAVE_FILE, false);
		this.filePathValue = MapUtils.getString(this.paramsData, FILE_SAVE_PATH, "").replaceAll("\\\\", "/");
		if(this.urlStr == null || this.urlStr.isEmpty()) {
			this.urlStr = MapUtils.getString(this.paramsData, URL, null);
		}

		for(String key : STATIC_LIST) {
			this.paramsData.remove(key);
		}
	}
	
	public String connect(String urlStr) throws Exception{
		this.urlStr = urlStr;
		return connect();
	}

	public String connect() throws Exception{
		initMemberVariables();

		String result = null;
		byte[] paramsByte = null;

		if("".equals(this.urlStr) || this.urlStr == null) {
			throw new Exception("URL is Empty.");
		}

		if("GET".equalsIgnoreCase(MapUtils.getString(this.headerData, REQ_METHOD, "GET"))) {
			StringJoiner paramsStr = new StringJoiner("&");
			for(Entry<String, Object> entry : this.paramsData.entrySet()) {
				if(entry.getValue() instanceof String) {
					paramsStr.add(entry.getKey() + "=" + entry.getValue());
				}
			}
			
			this.urlStr += "?" + paramsStr.toString();
		}

		URL url = new URL(this.urlStr);
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		if(conn != null) {
			conn.setConnectTimeout(this.connectTimeLimit);
			conn.setDoInput(true);
			conn.setRequestProperty("Accept", "application/json");
		    conn.setRequestProperty("Content-type", "application/json");

		    if(this.headerData != null && this.headerData.size() > 0) {
				for(String key : this.headerData.keySet()) {
					if(REQ_METHOD.equalsIgnoreCase(key)) {
						conn.setRequestMethod(MapUtils.getString(this.headerData, key, "GET"));
					} else {
						conn.setRequestProperty(key, MapUtils.getString(this.headerData, key, null));
					}
				}
			}

			if(this.paramsData != null || this.paramsData.size() > 0) {
				if("POST".equalsIgnoreCase(conn.getRequestMethod())) {
					conn.setDoOutput(true);

					String reqContentType = MapUtils.getString(this.headerData, "Content-type", null);
					if(reqContentType == null) {
						paramsByte = new Gson().toJson(this.paramsData).getBytes(this.charset);
						conn.setRequestProperty("Content-Length", String.valueOf(paramsByte.length));
						conn.getOutputStream().write(paramsByte);
					} else if(StringUtils.containsIgnoreCase(reqContentType, "application/x-www-form-urlencode")) {
						StringJoiner sj = new StringJoiner("&");
						for(String key : this.paramsData.keySet()) {
							sj.add(key + "=" + URLEncoder.encode(MapUtils.getString(this.paramsData, key), this.charset));
						}
						paramsByte = sj.toString().getBytes("UTF-8");
						conn.setRequestProperty("Content-Length", String.valueOf(paramsByte.length));
						conn.getOutputStream().write(paramsByte);
					} else if(StringUtils.containsIgnoreCase(reqContentType, "multipart/form-data")) {
						String boundary = null;
						for(String v : reqContentType.split(";")) {
							if(v.contains("boundary")) {
								String[] tmp = v.split("=");
								if(tmp.length == 2) {
									boundary = tmp[1];
								}
							}
						}

						if(boundary == null || boundary.isEmpty()) {
							boundary = RandomStringUtils.randomAlphanumeric(8);
							conn.setRequestProperty("Content-type", "multipart/form-data;boundary=" + boundary);
						}

						OutputStream out = conn.getOutputStream();
						for(String key : this.paramsData.keySet()) {
							Object value = MapUtils.getObject(this.paramsData, key);

							boolean isLF = true;
							if(this.paramsData.keySet().size()-1  ==  new ArrayList<>(this.paramsData.keySet()).indexOf(key)) {
								isLF = false;
							}
							
							if (value instanceof List) {
								for(Object innerValue : (List) value) {
									if(innerValue instanceof String) {
										this.multipartAddFormData(out, boundary, key, innerValue.toString(), isLF);
									} else if(innerValue instanceof File) {
										this.multipartAddFileData(out, boundary, key, (File) innerValue);
									}
								}
							} else if(value instanceof String) {
								this.multipartAddFormData(out, boundary, key, value.toString(), isLF);
							} else if(value instanceof File) {
								this.multipartAddFileData(out, boundary, key, (File) value);
							}
						}
						this.multipartFinish(out, boundary);
						//FileUtils.write(new File("D:\\"+System.currentTimeMillis()+".txt"), out.toString(), "UTF-8");
					} else {
						paramsByte = new Gson().toJson(this.paramsData).getBytes(this.charset);
						conn.setRequestProperty("Content-Length", String.valueOf(paramsByte.length));
						conn.getOutputStream().write(paramsByte);
					}
				}
			}

			conn.connect();
			conn.disconnect();

			Map<String, Object> reqHeader = new HashMap<>();
			reqHeader.put(conn.getRequestMethod(), url.getPath());
			reqHeader.put("HOST", url.getHost());
			reqHeader.put("Content_length", conn.getRequestProperty("Content-Length") == null ? "0" : conn.getRequestProperty("Content-Length"));
			reqHeader.putAll(conn.getRequestProperties());
			
			printLogSection("=====REQUEST HEADER=====", reqHeader, this.consoleLog);

			printLogSection("======REQUEST BODY======", this.paramsData, this.consoleLog);

			InputStream in = null;
			this.responseCode = conn.getResponseCode();
			if(this.responseCode == HttpURLConnection.HTTP_OK) {
				in = conn.getInputStream();
			} else {
				in = conn.getErrorStream();
			}

			if(this.responseCode == HttpURLConnection.HTTP_OK && this.resultSaveFileFlag && conn.getContentLength() > 0) {
				if(this.filePathValue == null) {
					this.filePathValue = (SepoaMultipart.getTempPath() + System.currentTimeMillis()).replaceAll("\\\\", "/");
				}

				String filePath = this.filePathValue;
				String fileName = "";
				List<String> dispositionList = ((List<String>) MapUtils.getObject(conn.getHeaderFields(), "Content-Disposition"));
				if(dispositionList != null && dispositionList.size() > 0) {
					ContentDisposition content = new ContentDisposition(dispositionList.get(0));
					if("attachment".equalsIgnoreCase(content.getDisposition())) {
						String tmpStr = content.getParameter("filename*").replace("UTF-8''", "");
						fileName = tmpStr.length() > 0 ? tmpStr : content.getParameter("filename");
					}
				}

				if("".equals(fileName) || fileName.isEmpty()) {
					fileName = FilenameUtils.getBaseName(url.getPath()) + MimeTypes.getDefaultMimeTypes().getRegisteredMimeType(conn.getContentType()).getExtension();
				}

				fileName = URLDecoder.decode(fileName, this.charset);
				String fileStr = filePath + "/" + fileName;
				File file = new File(fileStr);
				if(!file.getParentFile().exists()) {
					FileUtils.forceMkdir(file.getParentFile());
				}

				DataInputStream dis = new DataInputStream(in);
				FileOutputStream os = new FileOutputStream(file);
				DataOutputStream dos = new DataOutputStream(os);

				int buf = -1;
	            while ((buf = dis.read()) != -1) {
	            	dos.write(buf);
	            }

	            result = file.getAbsolutePath().replaceAll("\\\\", "/");
	            dos.close();
	            in.close();
			} else {
				BufferedReader br = new BufferedReader(new InputStreamReader(in, this.charset));
				StringBuilder sb = new StringBuilder();
				String line;
				while((line = br.readLine()) != null) {
					sb.append(line);
				}
				
				result = sb.toString();
				br.close();
				in.close();
			}

			printLogSection("=====RESPONSE HEADER=====", conn.getHeaderFields(), this.consoleLog);

			printLogSection("======RESPONSE BODY======", result, this.consoleLog);
		}

		return result;
	}

	private void multipartAddFormData(OutputStream out,String boundary, String key, String value, boolean isLF) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, this.charset), true);
		String LINE_FEED = "\r\n";
		
		writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + key + "\"").append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=" + charset).append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value);
        if(isLF) {
        	writer.append(LINE_FEED);
        }
        writer.flush();
	}
	
	private void multipartAddFileData(OutputStream out,String boundary, String key, File file) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, this.charset), true);
		String LINE_FEED = "\r\n";
		
		String fileName = file.getName();
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fileName + "\"").append(LINE_FEED);
        writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
	}
	
	private void multipartFinish(OutputStream out,String boundary) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, this.charset), true);

        writer.flush();
        writer.append("--" + boundary + "--");
        writer.close();
	}
	
	private void printLogSection(String header, Object dataMap, boolean consolePrint) {
		if(consolePrint) {
			System.out.println(header);
		}

		Logger.info.println(header);
		if(dataMap instanceof Map) {
			for(Map.Entry<String, Object> data : ((Map<String, Object>) dataMap).entrySet()) {
				if(consolePrint) {
					System.out.println((data.getKey() != null ? data.getKey() + " : " : "") + data.getValue());	
				}

				Logger.info.println((data.getKey() != null ? data.getKey() + " : " : "") + data.getValue());
			}
		} else {
			if(dataMap.toString().length() > 1000) {
				dataMap = "[DATA]";
			}

			if(consolePrint) {
				System.out.println(dataMap);
			}

			Logger.info.println(dataMap);
		}
	}
}
