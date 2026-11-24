package sepoa.seal.approval;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;

@Description("인터페이스 - HTML")
@WebServlet("/SepoaApprovalHTML.sux")
public class SepoaApprovalHTML extends HttpServlet {

	private static final long serialVersionUID = 4375878140660622325L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		this.doGenerateHtml(req, res);

	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		this.doGenerateHtml(req, res);

	}

	@Description("인터페이스 - HTML")
	private void doGenerateHtml(HttpServletRequest req, HttpServletResponse res) throws IOException {

		
		Map<String, String[]> paramMap = req.getParameterMap();
		Map<String, String> dataMap = new HashMap<String, String>();
		Map<String, String> infoData = new HashMap<String, String>();

		String houseCode = CommonUtil.getConfig(req.getServerName() + ".house_code", "");

		Config config = null;
		SepoaInfo info = null;

		String processId = null;
		String methodName = null;
		String returnHtml = null;

		SepoaOut so = null;
		try {
			config = new Configuration(houseCode);

			processId = config.getString("sepoa.generate.html.processid", "sepoa.svc.approval.SepoaGWApprovalService");
			methodName = config.getString("sepoa.generate.html.methodname", "doGenerateHtml");

			String contentType = config.getString("sepoa.generate.html.request.contenttype", req.getContentType());

			if ("".equals(processId) || "".equals(methodName)) {
				throw new Exception("##### Check the properties. Mapping data not found! #####");
			}

			infoData.put("HOUSE_CODE", houseCode);
			infoData.put("COMPANY_CODE", "1000");
			infoData.put("ID", "INTERFACE");
			infoData.put("LAGUAGE", "KO");
			infoData.put("NAME_LOC", "INTERFACE");
			infoData.put("NAME_ENG", "INTERFACE");
			infoData.put("DEPARTMENT", "INTERFACE");
			infoData.put("DEPARTMENT_NAME_LOC", "INTERFACE");

			info = new SepoaInfo(infoData);
			info.setThreadLocal();

			if (StringUtils.containsIgnoreCase(contentType, "json")) {
				String jsonString = IOUtils.toString(req.getInputStream());
				Gson gson = new Gson();
				Map<String, String> map = gson.fromJson(jsonString, Map.class);
				dataMap.putAll(map);
			} else {
				for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
					dataMap.put(entry.getKey(), entry.getValue()[0]);
					Logger.dev.println(entry.getKey() + ":" + entry.getValue()[0]);
				}
			}
			
			Object[] obj = { dataMap };
			so = ServiceConnector.doService(info, processId, "TRANSACTION", methodName, obj);

			if (so != null && so.flag) {
				returnHtml = so.result[0];
			} else {
				// 서비스로직 문제가 있을경우, FAIL추가
				throw new Exception("##### doService Process Fail! #####");
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.print(e.getMessage());

			returnHtml = e.getMessage();
		} finally {
			try {
				resultWriter(res, returnHtml);
			} catch (Exception e) {
				e.printStackTrace();
				Logger.err.print(e);
			}
			try {
				info.unsetThreadLocal();
			} catch (Exception e) {
			}
		}
	}

	private void resultWriter(HttpServletResponse res, String html) {
		res.setContentType("text/html;charset=UTF-8");
		res.setCharacterEncoding("UTF-8");
		try {
			PrintWriter out = res.getWriter();
			out.println(html);
			out.flush();
		} catch (Exception e) {
			Logger.err.println(e);
		}
	}

}
