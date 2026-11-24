package sepoa.sealmk2;

import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.ses.SepoaInfo;

public class SepoaExtSealManager extends sepoa.seal.SepoaExtSealManager {
	public SepoaExtSealManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
	}
}
