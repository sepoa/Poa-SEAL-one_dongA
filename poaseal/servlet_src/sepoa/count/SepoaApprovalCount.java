package sepoa.count;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public class SepoaApprovalCount extends HttpServlet {
	private static final long serialVersionUID = 3804956812185228596L;
	private static final String ID = "IF_APPROVAL_CNT";

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doPost(req, res);
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		res.setCharacterEncoding("UTF-8");
		res.setContentType("application/json");
		
		Map<String, String> infoData = new HashMap<String, String>();
		String houseCode = CommonUtil.getConfig(req.getServerName() + ".house_code", "");
		
		PrintWriter pout = res.getWriter();
		
		try {
			SepoaInfo info = null;
			infoData.put("HOUSE_CODE", houseCode);
			infoData.put("COMPANY_CODE", "1000");
			infoData.put("ID", "DUMMY_SESSION");
			infoData.put("LANGUAGE", "KO");
			info = new SepoaInfo(infoData);
			info.setThreadLocal();
			
			Map<String, Object> allData = new HashMap<>();
			allData.put("REQUEST_OBJECT", req);
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.approval.extif.SepoaApprovalCountService", "CONNECTION", "doProcess", new Object[] {allData});
			//성공이던 실패던 무조건 getValue할 수 잇는 값이 있어야 한다.
			if (so == null || !so.flag) {
				Logger.err.println("Sepoa Approval Count Error ! HOUSE_CODE : " + houseCode);
			}
			
			pout.print(so.getData(0));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
