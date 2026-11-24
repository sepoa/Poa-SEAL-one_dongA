package sepoa.seal.callback;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public class SepoaSealCallback extends HttpServlet {

	private static final long serialVersionUID = -8769764939563527972L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		this.doCallbackParams(req, res);

	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		this.doCallbackParams(req, res);

	}

	@Description("")
	private void doCallbackParams(HttpServletRequest req, HttpServletResponse res) throws IOException {

		Map<String, String[]> paramMap = req.getParameterMap();
		Map<String, String> dataMap = new HashMap<String, String>();
		Map<String, String> infoData = new HashMap<String, String>();
		Map<String, Object> returnMap = new HashMap<String, Object>();

		String houseCode = CommonUtil.getConfig(req.getServerName() + ".house_code", "");

		Config config = null;
		SepoaInfo info = null;
		try {
			config = new Configuration(houseCode);

			String processId = config.getString("sepoa.callback.processid", "");
			String methodName = config.getString("sepoa.callback.methodname", "");

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
			for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
				dataMap.put(entry.getKey(), entry.getValue()[0]);
			}
			
			Logger.dev.println(" ########### retrun Data ===> ######### ");
			Logger.dev.println(dataMap);
			
			Object[] obj = { dataMap };
			SepoaOut so = ServiceConnector.doService(info, processId, "TRANSACTION", methodName, obj);
			if (so == null || !so.flag) {
				throw new Exception("##### doService Process Fail! #####");
			}
			returnMap.put("RESULT", "SUCCESS!");

		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.print(e.getMessage());
			returnMap.put("RESULT", "FAILURE!");
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
