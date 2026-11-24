package sepoa.svc.cbo.ct;

import sepoa.contract.SepoaContractManager;
import sepoa.contract.SepoaContractTemplateManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.svc.contract.SepoaContractService;

@Description("전자계약 - 모듈")
public class SepoaCboContractService extends SepoaContractService {

	public SepoaCboContractService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected SepoaContractManager getManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		// TODO Auto-generated method stub		
		return new SepoaContractNoPermissionManager(info, config, ctx);

	}
	
	
	
}
