package sepoa.svc.approval.extif;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64.Decoder;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaFormater;

@Description("결재 카운팅 외부 연동 CBO 서비스")
public class SepoaApprovalCountService extends SepoaService{
	public SepoaApprovalCountService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("NIA 결재 카운트 외부연동 서비스")
	public SepoaOut doProcess(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = null;
		
		try {
			ctx = getConnectionContext();
			HttpServletRequest req = (HttpServletRequest) data.get("REQUEST_OBJECT");
			
			Decoder decode = Base64.getDecoder();
			String endcodeId = req.getParameter("UID");
			String decodeId = new String(decode.decode(endcodeId));
			
//			String userId = req.getParameter("UID");
			Logger.info.println("ApprovalCountService inputId :" + decodeId);
			System.out.println("ApprovalCountService inputId :" + decodeId);
			// 세션생성
			SepoaInfo info = null;
			Map<String, String> infoData = new HashMap<String, String>();
			infoData.put("HOUSE_CODE", "015");
			infoData.put("COMPANY_CODE", "1000");
			infoData.put("USER_ID", decodeId.toUpperCase());
			infoData.put("LANGUAGE", "KO");
			
			info = new SepoaInfo(infoData);
			info.setThreadLocal();
			
			//사용자 정보 확인
			sxp = new SepoaXmlParser(this, "doSelectUserInfo");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
			
			if(sf.getListMap().size() < 1 ) {
				throw new Exception(infoData.get("HOUSE_CODE") + ": Invalid user information");
			}
			
			Map<String, String> resultData = new HashMap<>();
			
			// SSIAD 리스트 결재진행중인 데이터
			sxp = new SepoaXmlParser(this, "doSelectSSIAD");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
			resultData.put("APPOVAL_LIST", String.valueOf(sf.getListMap().size()));
			if(sf.getRowCount() > 0) {
				resultData.put("SID", "SLM_201");
			}
			
			// SGWGL 미결재함 리스트 
			sxp = new SepoaXmlParser(this, "doSelectSGWGL");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
			resultData.put("CONFRIM_LIST", String.valueOf(sf.getListMap().size()));
			if(sf.getRowCount() > 0){
				resultData.put("SID", "AP_102");
			}
			
			Map<String, Object> returnObj = new HashMap<>();
			returnObj.put("RETURN_OBJECT", resultData);
			
			setValue(new Gson().toJson(returnObj));
		} catch (Exception e) {
			Map<String, Object> returnObj = new HashMap<>();
			returnObj.put("RETURN_OBJECT", "### SepoaApprovalCountService ERROR: " + e.getMessage() + " ###");
			setValue(new Gson().toJson(returnObj));
			
			if(getStatus() == 1) setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}
