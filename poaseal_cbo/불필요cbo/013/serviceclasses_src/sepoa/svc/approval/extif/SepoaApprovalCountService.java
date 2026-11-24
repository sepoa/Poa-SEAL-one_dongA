package sepoa.svc.approval.extif;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("결재 카운팅 외부 연동 CBO 서비스")
public class SepoaApprovalCountService extends SepoaService{
	public SepoaApprovalCountService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("포스코플로우 EP앱 결재 카운트 외부연동 서비스")
	public SepoaOut doProcess(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		String reqCallbackName = null;
		try {
			ConnectionContext ctx = getConnectionContext();
			HttpServletRequest req = (HttpServletRequest) data.get("REQUEST_OBJECT");
			
			reqCallbackName = req.getParameter("callback");
			
			Map<String, String> reqHeaders = new HashMap<>();
			Enumeration<String> headerNames = req.getHeaderNames();
			String headerName = headerNames.nextElement();
			while(headerNames.hasMoreElements()) {
				reqHeaders.put(headerName, req.getHeader(headerName));
				headerName = headerNames.nextElement();
			}
			
			SepoaInfo info = null;
			Map<String, String> infoData = new HashMap<String, String>();
			infoData.put("HOUSE_CODE", "013");
			infoData.put("COMPANY_CODE", "1000");
			infoData.put("ID", reqHeaders.get("iv-user").toUpperCase());
			infoData.put("LANGUAGE", "KO");
			
			info = new SepoaInfo(infoData);
			info.setThreadLocal();
			
			Map<String, Object> allData = new HashMap<>();
			Map<String, String> headerData = new HashMap<>();
			headerData.put("MODE", "TURN");
			allData.put(SepoaDataMapper.KEY_HEADER_DATA, headerData);
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.approval.SepoaApprovalService", ctx, "doQuery", new Object[] {allData});
			if (so == null || !so.flag) {
				throw new Exception("##### sepoa.svc.approval.SepoaApprovalService Process Fail! #####");
			}
			
			List<Map<String, String>> approvalData = new SepoaFormater(so.getData(0).toString()).getListMap();
			Map<String, String> returnValue = new HashMap<>();
			returnValue.put("cnt", String.valueOf(approvalData.size()));
			setValue(reqCallbackName + "(" + new Gson().toJson(returnValue) +")");
		} catch (Exception e) {
			Map<String, String> returnValue = new HashMap<>();
			returnValue.put("cnt", "0");
			setValue(reqCallbackName + "(" + new Gson().toJson(returnValue) +")");
			
			if(getStatus() == 1) setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}
