package sepoa.iams;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.bxi.iams.client.web.IamsClientCallbackExecuterWithDt;
import com.bxi.iams.enums.ResultStatus;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.ServiceConnector;

public class SepoaIamsCallBack implements IamsClientCallbackExecuterWithDt {

	private final static String PROCESS_ID = "sepoa.svc.iams.IamsApproval";

	private final static String METHOD_NAME = "doApproval";

	@Override
	public void execute(HttpServletRequest arg0, String arg1, int arg2, int arg3, int arg4, ResultStatus arg5,
			String arg6) throws Exception {

		Map<String, String[]> paramMap = arg0.getParameterMap(); 
		Map<String, String> dataMap = new HashMap<String, String>();

		SepoaInfo info = new SepoaInfo("004",
				"COMPANY_CODE=1000^@^ID=IAMS^@^LANGUAGE=KO^@^NAME_LOC=IAMS^@^NAME_ENG=IAMS^@^DEPARTMENT=IAMS^@^DEPARTMENT_NAME_LOC=IAMS^@^");
		info.setThreadLocal();
		try {
			for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
				dataMap.put(entry.getKey(), entry.getValue()[0]);
			}

			Object[] obj = { dataMap };
			ServiceConnector.doService(info, PROCESS_ID, "TRANSACTION", METHOD_NAME, obj);

		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.print(e);
		} finally {
			info.unsetThreadLocal();
		}

	}
}
