 	package sepoa.svc.sealmk2;

import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaServiceException;

@Description("전자인장MK2 - 공통모듈")
public class SepoaSealService extends sepoa.svc.seal.SepoaSealService {
	
	public SepoaSealService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
}
