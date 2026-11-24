package sepoa.svc.batch;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaFormater;

@Description("실물날인첨부 알람 배치 서비스")
public class SepoaRealSealAfter extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaRealSealAfter() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaRealSealAfter(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("실물날인첨부 알람 배치 서비스")
	public SepoaOut executeWeb(Map<String, Object> allData) throws SepoaServiceException {
		return execute();
	}

	// dbcp 초기화(멀티커넥트 지원 sepoa.properties에 기술되어 있어야함)
	public static void initialize() {
		DBCPSetup.initDBCP();
	}

	// 실행(타켓 취득 및 비즈니스로직 수행)
	public SepoaOut execute() throws SepoaServiceException {
		setFlag(true);
		setStatus(1);
		ConnectionContext ctx = null;
		try {
			ctx = getConnectionContext();
			List<Map<String, String>> dataList = getTargetRecord(ctx);

			if (dataList.size() != 0 || dataList != null) {
				setTargetRecord(ctx, dataList);
			} 

		} catch (Exception exception) {
			Rollback();
			exception.printStackTrace();
			setFlag(false);
			setStatus(0);
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ")
					.append(exception.getMessage()).toString());
		} finally {
			try {
				ctx.getConnection().close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return getSepoaOut();
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (isRunning) {
			System.out.println("The Test process is already running.");
			Logger.info.println("The Test process is already running.");
			return; // 현재 처리중
		}
		isRunning = true;
		String house_code = "013";
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();

			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : SepoaRealSealAfter";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaRealSealAfter("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaRealSealAfter";
			System.out.println(endLog);
			Logger.info.println(endLog);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			isRunning = false;
			sepoainfo.unsetThreadLocal();
		}
	}

	@Override
	public List<Map<String, String>> getTargetRecord(ConnectionContext var1) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		Config config = new Configuration();
		ConnectionContext ctx = var1;

		List<Map<String, String>> realSealList = new ArrayList<Map<String, String>>();

		try {
			// 검수신청 프로세스  모듈
			boolean useSealModule = SepoaModuleService.checkBusiness(new Configuration(), info, "USW1687397809419"); 
			boolean legalSealModule = SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1687399123985");
			
			String countDay = config.getString("sepoa.realseal.alarm.day", "3");
			Map<String, String> tmpData = new HashMap<>();
			tmpData.put("COUNT_DAY", countDay);
			if(useSealModule || legalSealModule) {
				sxp = new SepoaXmlParser(this, "doQueryCompleteList");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(tmpData));
			}
			
			if(sf != null) {
				realSealList = sf.getListMap();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info.print("Get TargetRecord Fail!!");
		}

		return realSealList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		for (Map<String, String> target : var2) {
			try {
				Configuration conf = new Configuration();
				
				SepoaPushManager mailpush = new SepoaPushManager(info, conf);
            	Map<String, String> mailData = new HashMap<String, String>();
            	SepoaMessage message = new SepoaMessage();
				
				mailData.put("SUBJECT", Cache.text.get("MAIL.REAL_SEAL_SUBJECT"));
            	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
            	mailData.put("SMR_NUMBER", target.get("SMR_NUMBER"));
            	mailData.put("DOC_SUBJECT", target.get("DOC_SUBJECT"));
            	
            	message.addParams(mailData);
				message.setMessageType("REAL_SEAL_ALRAM");
				message.addReceiver(target.get("USER_ID"), target.get("USER_NAME_LOC"), target.get("EMAIL"), null);
            	mailpush.addMessage(message);
            	
            	mailpush.accept();
            	
            	Logger.info.println("### 실물날인 첨부 알림 발송완료 ###");
            	Logger.info.println("TARGET: " + target.get("SMR_NUMBER"));
            	Logger.info.println("COUNT_DAY: " + target.get("COUNT_DAT") + "Day");
            	
			} catch (Exception e) {
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-SepoaRealSealAfterBatch "
						+ ": " + e;
				System.out.println(log);
				Logger.info.println(log);
				Logger.info.println(e.getMessage());
				continue;
			}
		}

	}
}
