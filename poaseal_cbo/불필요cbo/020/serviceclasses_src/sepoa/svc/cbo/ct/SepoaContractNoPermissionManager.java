package sepoa.svc.cbo.ct;

import sepoa.contractmk2.SepoaContractManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.ses.SepoaInfo;

public class SepoaContractNoPermissionManager extends SepoaContractManager {

	public SepoaContractNoPermissionManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.checkPermission = false;
		// TODO Auto-generated constructor stub
	}
	
	

}
