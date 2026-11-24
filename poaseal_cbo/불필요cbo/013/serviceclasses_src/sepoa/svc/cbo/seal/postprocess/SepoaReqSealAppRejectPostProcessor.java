package sepoa.svc.cbo.seal.postprocess;

import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

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
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.common.RestUtil;

@Description("결재반려 프로세스 후처리 프로세서")
public class SepoaReqSealAppRejectPostProcessor extends SepoaService{
	public SepoaReqSealAppRejectPostProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("포스코플로우 날인 요청 건 결재반려 후 후처리 (인터페이스 건 답장 발송 용)")
	public SepoaOut doProcess(Map<String, String> data) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = null;
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        
		try {
			ctx = getConnectionContext();
			
			// 인터페이스 건인지 검사
			sxp = new SepoaXmlParser(this, "doQuerySSIAD");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(data));
			
			if(sf.getRowCount() > 0) {
				Map<String, String> reqSealData = sf.getListMap().get(0);
				
				if(StringUtils.isNotBlank(reqSealData.get("SOURCE_NO"))) {
					Map<String, Object> body = new HashMap<>();
					Map<String, Object> responseParams = new LinkedHashMap<String, Object>();
					
					// 인터페이스면 REST 발송
					String restResult = null;
					String url = CommonUtil.getConfig("sepoa.seal.pscoflow.distribution.system.url", "http://local.poscoflow.co.kr:17790/ActionController.sux");
					RestUtil util = new RestUtil();
					util.setHeaderData(RestUtil.REQ_METHOD, "POST");
					util.setHeaderData("Content-type", "multipart/form-data");
					Logger.info.println("Req Seal Approval Reject Request URL : " + url);
					
					String updateDate = reqSealData.get("CHANGE_DATE") + reqSealData.get("CHANGE_TIME");
					responseParams.put("UPDATE_DATE", updateDate);
					responseParams.put("SMR_NUMBER", reqSealData.get("SMR_NUMBER"));
					responseParams.put("STATUS", "R");
					responseParams.put("APP_COMMENT", data.get("ACCEPT_REASON"));
					
					body.put("data", new Gson().toJson(responseParams));
					
					//set Params
					util.setParamsData(body);
					
					try {
						restResult = util.connect(url);
					} catch(Exception e) {
						setStatus(107003);
						e.printStackTrace();
						throw new Exception("There was a problem calling the Req Seal Approval Reject Request API.", e);
					}
					
					// parse result
					if(util.getResponseCode() != HttpURLConnection.HTTP_OK) {
						//HTTP 신호가 200이 아닐 때 다른 작업이 더 있다면 별도 처리 필요
						Logger.err.println("Req Seal Approval Reject Request Result Not 200! : " + restResult);
						throw new Exception("Req Seal Approval Reject Request Result Not 200! : " + restResult);
					} else {
						if(!restResult.trim().isEmpty() && restResult != null) {
							//성공일 떄
							Logger.info.println("==================== Req Seal Approval Reject Request API Send Success! ====================", restResult);
						} else {
							//실패 일때
							Logger.err.println("Req Seal Approval Reject Request Result is Empty! : " + restResult);
							throw new Exception("Req Seal Approval Reject Request Result is Empty! : " + restResult);
						}
					}
				}
			}
			
			// 아니면 쉿
		} catch (Exception e) {
			if(getStatus() == 1) setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}
