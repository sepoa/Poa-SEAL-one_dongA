package sepoa.sealmk2;

import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.ses.SepoaInfo;

public class SepoaSealManagementManager extends sepoa.seal.SepoaSealManagementManager{
	/*
	 * Constructors 
	 */
	public SepoaSealManagementManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
	}
}
