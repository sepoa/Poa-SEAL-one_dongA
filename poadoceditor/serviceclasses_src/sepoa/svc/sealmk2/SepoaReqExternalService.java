package sepoa.svc.sealmk2;

import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaServiceException;

@Description("전자인장MK2 - 사외반출 요청 모듈")
public class SepoaReqExternalService extends sepoa.svc.seal.SepoaReqExternalService {

	public SepoaReqExternalService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
}
