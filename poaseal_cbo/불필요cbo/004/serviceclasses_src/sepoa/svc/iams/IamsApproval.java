package sepoa.svc.iams;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@Description("IamsApproval")
public class IamsApproval extends SepoaService {

	public IamsApproval(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("")
	public SepoaOut doApproval(Map<String, String> dataMap) {
		
		final String RESULT_STATUS_SUBMITTED = "SUBMITTED"; //상신
		final String RESULT_STATUS_PROCEED = "PROCEED"; 	//진행
		final String RESULT_STATUS_APPROVED = "APPROVED";	//결재완료
		final String RESULT_STATUS_REJECTED = "RETURNED"; 	//반려

		final String SSID_SMR_STATUS_REJECTED = "R";
		final String SSID_SMR_STATUS_COMPLETED = "CR";
		final String SSID_SMR_STATUS_EXT_COMPLETE = "RW";	//반입대기

		setStatus(1);
		setFlag(true);
		SepoaOut out = null;

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Logger.debug.println("DataMap recieved from Iams : " + dataMap);

			String resultStatus = dataMap.get("RESULT_STATUS");
			String resultDate = dataMap.get("RESULT_DATE");

			ConnectionContext ctx = getConnectionContext();
			if("EX".equals(dataMap.get("LEGACY_JOB_ID"))) {
				sxp = new SepoaXmlParser(this, "doQuerySSETL_SMR_NUMBER");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(dataMap));
			}else {				
				sxp = new SepoaXmlParser(this, "doQuerySSIAD_SMR_NUMBER");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(dataMap));
			}

			List<Map<String, String>> listMap = sf.getListMap();
			if (listMap == null || listMap.size() == 0) {
				throw new Exception("Data doesn't exists");
			}
			if (listMap.size() > 1) {
				throw new Exception("Data is ambiguous - Multiple data");
			}
			dataMap.putAll(listMap.get(0));

			if (RESULT_STATUS_SUBMITTED.equals(resultStatus)) {
				//결재 상신
				if("EX".equals(dataMap.get("LEGACY_JOB_ID"))) {
					sxp = new SepoaXmlParser(this, "updateSSETL_SUBMITTED");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);
					
					dataMap.put("COMPLETE_FLAG", "N");
					sxp = new SepoaXmlParser(this, "doChangeFlag");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setSepoaInfo(info);
					ssm.doUpdate(dataMap);
				}else {
					sxp = new SepoaXmlParser(this, "updateSSIAD_SUBMITTED");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);
				}
			} else if (RESULT_STATUS_PROCEED.equals(resultStatus)) {
				//결재 진행중
			} else if (RESULT_STATUS_REJECTED.equals(resultStatus)) {
				//결재 반려
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				Date date = sdf.parse(resultDate);
				sdf = new SimpleDateFormat("yyyyMMdd");
				dataMap.put("ACCEPT_DATE", sdf.format(date));
				sdf = new SimpleDateFormat("HHmmss");
				dataMap.put("ACCEPT_TIME", sdf.format(date));

				if("EX".equals(dataMap.get("LEGACY_JOB_ID"))) {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_REJECTED);
					
					sxp = new SepoaXmlParser(this, "updateSSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);
				}else {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_REJECTED);
					
					sxp = new SepoaXmlParser(this, "updateSSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);					
				}
			} else if (RESULT_STATUS_APPROVED.equals(resultStatus)) {
				//결재 완료
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				Date date = sdf.parse(resultDate);
				sdf = new SimpleDateFormat("yyyyMMdd");
				dataMap.put("ACCEPT_DATE", sdf.format(date));
				sdf = new SimpleDateFormat("HHmmss");
				dataMap.put("ACCEPT_TIME", sdf.format(date));
				
				if("EX".equals(dataMap.get("LEGACY_JOB_ID"))) { 
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_EXT_COMPLETE);
					sxp = new SepoaXmlParser(this, "updateSSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);
					
					sxp = new SepoaXmlParser(this, "doQuerySSETL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(dataMap));
					dataMap.putAll(sf.getListMap().get(0));
					
				}else {
					dataMap.put("SMR_STATUS", SSID_SMR_STATUS_COMPLETED);
					sxp = new SepoaXmlParser(this, "updateSSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(dataMap);
					
					sxp = new SepoaXmlParser(this, "doQuerySSIAD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(dataMap));
					dataMap.putAll(sf.getListMap().get(0));
					
				}
				sxp = new SepoaXmlParser(this, "doAddSAPPP");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(dataMap);
			}
			Commit();

		} catch (Exception e) {
			// e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return out != null ? out : getSepoaOut();
	}
}
