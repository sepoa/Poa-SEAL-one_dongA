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

@Description("인터페이스 콜백")
@WebServlet("/SepoaApprovalStatusCallback.sux")
public class SepoaApprovalStatusCallback extends HttpServlet {

	private static final long serialVersionUID = -8769764939563527972L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		this.doCallbackParams(req, res);

	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		this.doCallbackParams(req, res);

	}

	@SuppressWarnings("unchecked")
	@Description("")
	private void doCallbackParams(HttpServletRequest req, HttpServletResponse res) throws IOException {

		Map<String, String[]> paramMap = req.getParameterMap();
		Map<String, Object> dataMap = new HashMap<String, Object>();
		Map<String, String> infoData = new HashMap<String, String>();
		Map<String, Object> returnMap = new HashMap<String, Object>();

		String houseCode = CommonUtil.getConfig(req.getServerName() + ".house_code", "");

		Config config = null;
		SepoaInfo info = null;

		String processId = null;
		String methodName = null;
		String returnMapMethodName = null;
		SepoaOut so = null;
		try {
			config = new Configuration(houseCode);

			processId = config.getString("sepoa.callback.processid", "sepoa.svc.approval.SepoaGWApprovalService");
			methodName = config.getString("sepoa.callback.methodname", "doApproval");
			returnMapMethodName = config.getString("sepoa.callback.returnmap.methodname", "getReturnMap");

			String contentType = config.getString("sepoa.callback.request.contenttype", req.getContentType());

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
				Logger.debug.println("jsonString : " + jsonString);				
				Gson gson = new Gson();
				Map<String, Object> map = gson.fromJson(jsonString, Map.class);
				dataMap.putAll(map);
			} else {
				for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
					dataMap.put(entry.getKey(), entry.getValue()[0]);
					Logger.dev.println(entry.getKey() + ":" + entry.getValue()[0]);
				}
			}

			Object[] obj = { dataMap };
			so = ServiceConnector.doService(info, processId, "TRANSACTION", methodName, obj);

			if (StringUtils.isNotBlank(returnMapMethodName) && so != null) {
				obj = new Object[] { so };
				so = ServiceConnector.doService(info, processId, "CONNECTION", methodName, obj);
				returnMap = (Map) so.getData(0);
			} else {
				// 서비스로직 문제가 있을경우, FAIL추가
				throw new Exception("##### doService Process Fail! #####");
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.print(e.getMessage());

			returnMap.put("RESULT", "FAIL");
			returnMap.put("MESSAGE", e.getMessage());
		} finally {
			try {
				resultWriter(res, returnMap);
				info.unsetThreadLocal();
			} catch (Exception e) {
				e.printStackTrace();
				Logger.err.print(e);
			}
		}

	}

	private void resultWriter(HttpServletResponse res, Map<String, Object> resultMap) {
		res.setContentType("application/json;charset=UTF-8");
		res.setCharacterEncoding("UTF-8");
		try {
			PrintWriter out = res.getWriter();
			out.println(new Gson().toJson(resultMap));
			out.flush();
		} catch (Exception e) {
			Logger.err.println(e);
		}
	}

}
