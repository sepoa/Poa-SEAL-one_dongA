package sepoa.svc.sealmk2;

import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaServiceException;

@Description("전자인장MK2 - 모듈")
public class SepoaSealManagementService extends sepoa.svc.seal.SepoaSealManagementService {

	public SepoaSealManagementService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
}
