package sepoa.contract.backprocess;

import java.util.HashMap;
import java.util.Map;
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
public class SepoaContractBackProcessListener implements ServletContextListener {

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
						int delay = conf.getInt("sepoa.contract.backprocess.period", 10);
						Thread.sleep(delay * 1000); 

						Set<String> configFileKeys = conf.getMatchedKey("sepoa\\....\\.config\\.file");
						for (String configFileKey : configFileKeys) {
							try {
								String houseCode = configFileKey.split("\\.")[1];
								Config configCBO = new Configuration(houseCode);
								
								Map<String, String> infoMap = new HashMap<>();
								infoMap.put("HOUSE_CODE", houseCode);
								infoMap.put("ID", "CONTRACT_LISTENER");
								infoMap.put("COMPANY_CODE", configCBO.get("sepoa.default.company_code", "1000"));
								infoMap.put("LANGUAGE", "KO");
								infoMap.put("NAME_LOC", "Batch");
								infoMap.put("NAME_ENG", "Batch");
								
								tempInfo = new SepoaInfo(infoMap);
								tempInfo.setThreadLocal();
								
								Map<String, Object> allData = new HashMap<>();
//								ServiceConnector.doService(tempInfo, "sepoa.svc.contract.SepoaContractService", "TRANSACTION", "doListenerProcess", new Object[] {allData});
								ServiceConnector.doService(tempInfo, "sepoa.svc.contract.SepoaEmployeeContractService", "TRANSACTION", "doListenerProcess", new Object[] {allData});
							} catch (Exception e) {
								Logger.err.println(e);
							} finally {
								try {
									tempInfo.unsetThreadLocal();
								} catch (Exception e) {
									e.printStackTrace();
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
		System.out.println("Back-process for Contract is started.");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		flag = false;
		System.out.println("Back-process for Contract is stoped.");
	}

}
