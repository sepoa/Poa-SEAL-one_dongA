package sepoa.sealmk2;

import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.ses.SepoaInfo;

public class SepoaSealManager extends sepoa.seal.SepoaSealManager{
	/*
	 * Constructors 
	 */
	public SepoaSealManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
	}
}
