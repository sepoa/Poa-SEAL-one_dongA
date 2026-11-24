package sepoa.svl.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.log.Logger;
import sepoa.fw.msg.MessageUtil;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.JSPUtil;
import sepoa.fw.util.RSACipher;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;
import xlib.cmc.GridData;
import xlib.cmc.OperateGridData;

public class FindPassword extends HttpServlet {

	private static final long serialVersionUID = -316867792871199375L;

	final static public String KEY_PROCESS_ID = "PROCESS_ID";

	final static public String KEY_METHOD = "METHOD";

	public void init() {
		Logger.err.println("SEPOA", this, getClass().getName() + " is initializing..");
	}

	public void destory() {
		Logger.err.println("SEPOA", this, getClass().getName() + " is destroying..");
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		doPost(req, res);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		SepoaInfo info = SepoaSession.getAllValue(req);

		req.setCharacterEncoding("UTF-8");
		res.setContentType("text/html;charset=UTF-8");

		GridData gdReq = null;
		GridData gdRes = null;

		PrintWriter out = res.getWriter();

		try {
			gdReq = OperateGridData.parse(req, res);
			String mode = gdReq.getParam("mode");

			if ("doQuery".equals(mode)) {
				gdRes = doQuery(info, gdReq);
				OperateGridData.write(req, res, gdRes, out);
			} else if ("doForm".equals(mode)) {
				doForm(info, req, res);
				return;
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.println(e);
		}
	}

	/**
	 * 그리드 조회 공통.
	 * 
	 * 1. Javascript에서 호출시 PROCESS_ID를 넘겨줄 것. 2. 위 PROCESS_ID의 doQuery 라는 Method를
	 * 호출한다. 3. 파라미터는 SepoaDataMapper의 값을 통째로 넘겨준다. (헤더, 그리드 데이터 모두) 4. DB연결은
	 * CONNECTION.
	 * 
	 * @param info
	 * @param gdReq
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private GridData doQuery(SepoaInfo info, GridData gdReq) throws Exception {
		GridData gdRes = new GridData();
		Vector multilang_id = new Vector();
		multilang_id.addElement("MESSAGE");
		HashMap message = MessageUtil.getMessage(info, multilang_id);

		String grid_col_id = JSPUtil.CheckInjection(gdReq.getParam("grid_col_id")).trim();
		String[] grid_col_ary = SepoaString.parser(grid_col_id, ",");

		try {
			Map<String, Object> data = SepoaDataMapper.getData(info, gdReq);

			gdRes.addParam("mode", "doQuery");

			Map<String, String> headerMap = MapUtils.getMap(data, SepoaDataMapper.KEY_HEADER_DATA);
			String processId = MapUtils.getString(data, KEY_PROCESS_ID);
			if (processId == null) {
				processId = MapUtils.getString(headerMap, KEY_PROCESS_ID);
			}

			String method = MapUtils.getString(data, KEY_METHOD);
			if (method == null) {
				method = MapUtils.getString(headerMap, KEY_METHOD);
			}
			if (method == null) {
				method = "doQuery";
			}

			Object[] obj = { data };
			SepoaOut value = ServiceConnector.doService(info, processId, "CONNECTION", method, obj);

			if (value.result[0] == null) {
				return gdRes;
			}

			SepoaFormater wf = new SepoaFormater(value.result[0]);

			for (int i = 0; i < wf.getRowCount(); i++) {
				for (int k = 0; k < grid_col_ary.length; k++) {
					gdRes.addValue(grid_col_ary[k], wf.getValue(grid_col_ary[k], i));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.print(e);
			gdRes.setMessage(message.get("MESSAGE.1002").toString());
			gdRes.setStatus("false");
		}

		return gdRes;
	}

	@SuppressWarnings({ "unchecked" })
	public void doForm(SepoaInfo info, HttpServletRequest req, HttpServletResponse res) throws Exception {
		SepoaOut value = null;
		String USER_ENCODING = CommonUtil.getConfig("sepoa.encodingset");
		try {
			res.setContentType("application/xml;charset=" + USER_ENCODING);
			GridData gdReq = OperateGridData.parse(req, res);
			Map<String, Object> data = SepoaDataMapper.getData(info, gdReq);
			String nickName = CommonUtil.nullToEmpty(req.getParameter("nickName"));
			String conType = CommonUtil.nullToEmpty(req.getParameter("conType"));
			String methodName = CommonUtil.nullToEmpty(req.getParameter("methodName"));
			value = ServiceConnector.doService(info, nickName, conType, methodName, new Object[] { data });
		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.println(e);
			value = new SepoaOut();
			value.status = 0;
			value.message = e.getMessage();
			value.result = new String[] { "" };
		} finally {
			try {
				HttpSession session = req.getSession();
				RSACipher.getRSACipherObject(session, true);
			} catch (Exception t) {
				Logger.warn.println("RSA Encryption is not avaliable.");
				Logger.warn.println(t);
			}
		}

		String xmlString = getWiseOutXML(value, USER_ENCODING);
		res.getWriter().write(xmlString);
		res.getWriter().flush();
	}

	private String getWiseOutXML(SepoaOut value, String USER_ENCODING) {
		StringBuffer xmlsb = new StringBuffer();
		xmlsb.append("<?xml version=\"1.0\" encoding=\"" + USER_ENCODING + "\"?>\r\n");
		xmlsb.append("<SepoaOut>\r\n");
		xmlsb.append("<status>");
		xmlsb.append(value.status);
		xmlsb.append("</status>\r\n");
		xmlsb.append("<message>");
		xmlsb.append(value.message);
		xmlsb.append("</message>\r\n");

		if (!"".equals(value.result) && value.result != null) {
			for (int i = 0; i < value.result.length; i++) {
				xmlsb.append("<result><![CDATA[");
				xmlsb.append(value.result[i]);
				xmlsb.append("]]></result>\r\n");

			}
		}
		xmlsb.append("</SepoaOut>\r\n");
		return xmlsb.toString();

	}
}