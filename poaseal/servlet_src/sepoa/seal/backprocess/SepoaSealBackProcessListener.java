package sepoa.seal.backprocess;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.ServiceConnector;

@WebListener
public class SepoaSealBackProcessListener implements ServletContextListener {

	private static boolean flag = true;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				while (flag) {
					SepoaInfo tempInfo = null;
					try {
						Config conf = new Configuration("000");
						int delay = conf.getInt("sepoa.seal.backprocess.period", 10);
						Thread.sleep(delay * 1000); 

						Set<String> configFileKeys = conf.getMatchedKey("sepoa\\....\\.config\\.file");
						for (String configFileKey : configFileKeys) {
							try {
								String houseCode = configFileKey.split("\\.")[1];
								tempInfo = new SepoaInfo(houseCode, "ID=SEAL_LISTENER^@^LANGUAGE=KO^@^NAME_LOC=Batch^@^NAME_ENG=Batch^@^COMPANY_CODE=1000^@^");
								tempInfo.setThreadLocal();

								ServiceConnector.doService(tempInfo, "sepoa.svc.approval.SepoaApprovalService", "TRANSACTION", "ApprovalReceiver", new Object[] {});
							} catch (Exception e) {
								Logger.err.println(e);
							} finally {
								try {
									tempInfo.unsetThreadLocal();
								} catch (Exception e) {
								}
							}
						}

					} catch (Exception e) {
						Logger.err.println("Backprocess has failed.", e);
					}
				}
			}
		};

		new Thread(runnable).start();
		System.out.println("Back-process for Signed PDF generation is started.");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		flag = false;
		System.out.println("Back-process for Signed PDF generation is stoped.");
	}

}
