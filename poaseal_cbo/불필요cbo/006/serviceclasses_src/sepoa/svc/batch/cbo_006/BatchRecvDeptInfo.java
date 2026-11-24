package sepoa.svc.batch.cbo_006;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;

/**
 * 
 * @author CKM
 * 부서정보_DB
 *
 */
public class BatchRecvDeptInfo extends SepoaBatchService {

	static boolean isRunning = true; // 처리중인데 실행되는 오류를 막기 위한 변수

	static String curDate = SepoaDate.getShortDateString();
	static String curTime = SepoaDate.getShortTimeString();
	static boolean log_enable_flag = false;
	
	public BatchRecvDeptInfo(String opt, SepoaInfo sepoainfo) throws SepoaServiceException {
		super(opt, sepoainfo);
		setVersion("1.0.0");
	}

	// dbcp 초기화(멀티커넥트 지원 sepoa.properties에 기술되어 있어야함)
	public static void initialize() {
		DBCPSetup.initDBCP();
	}

	// 웹에서 실행시 호출
	public SepoaOut executeWeb(Object allData) throws SepoaServiceException {
		return execute();
	}
	
	// 타겟데이터 취득 메소드
	public List<Map<String, String>> getTargetRecord(ConnectionContext ctx) throws Exception {
		Logger.info.println(info.getSession("ID"), this, "########  getTargetRecord Method Start DEPT ########");

		SepoaFormater sf =null;
		
		try {		
			SepoaXmlParser sxp = new SepoaXmlParser(this,"doQuerySDEPT_Target");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"),this,ctx,sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
			
		}catch(Exception e) {
			e.printStackTrace();
	
		}		
		return sf.getListMap();
	}

	// 타겟데이터 수행 메소드
	public void setTargetRecord(ConnectionContext ctx ,List<Map<String, String>> dataList) throws Exception {
		Logger.info.println(info.getSession("ID"), this, "########  setTargetRecord Method Start DEPT ########");
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		for (Map<String, String> map : dataList) {
			try {
				// SDEPT에 부서가 있는지 확인 (DEPTCODE로)
				sxp = new SepoaXmlParser(this, "checkExistSDEPT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(map));

				if (sf.getRowCount() > 0) {
					// 부서가 있으면 SDEPT 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(map);
				} else {
					// 부서가 없으면 SDEPT insert
					sxp = new SepoaXmlParser(this, "doInsertSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(map);
				}

			} catch (Exception e) {
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-BatchRecvDeptInfo : " + e;
				System.out.println(log);
				Logger.info.println(log);
				continue;
			}
		}
	}

	// 실행(타켓 취득 및 비즈니스로직 수행)
	public SepoaOut execute() throws SepoaServiceException {
		Logger.debug.println("############### DEPT 배치실행  ###############");
		setFlag(true);
		setStatus(1);
		ConnectionContext ctx = getConnectionContext();
		try {			
			joinTransaction("kised");
			ConnectionContext ctx2 = getConnectionContext("kised");
			ctx = getConnectionContext();
			
			List<Map<String, String>> dataList = getTargetRecord(ctx2);
			if (dataList.size() > 0) {
				setTargetRecord(ctx, dataList);
			}
			Commit();

		} catch (Exception exception) {
			exception.printStackTrace();
			Rollback();
			setFlag(false);
			setStatus(0);
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(BatchRecvDeptInfo.class.getSimpleName()+"() ERROR : ").append(exception.getMessage()).toString());
		} finally {
			try {
				ctx.getConnection().close();
			} catch (SQLException e) {
			}
		}

		return getSepoaOut();
	}
	
	public static void main(String[] args) {
		if (isRunning) {
			isRunning = false; // 현재처리중

			String house_code = "006"; // 하우스코드
			SepoaInfo sepoainfo = new SepoaInfo(house_code,
					"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

			try {
				sepoainfo.setThreadLocal();

				String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] START : BatchRecvDeptInfo";
				System.out.println(startLog);
				Logger.info.println(startLog);
				SepoaBatchService.initialize();
				SepoaBatchService.run(new BatchRecvDeptInfo("TRANSACTION", sepoainfo));

				String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] END : BatchRecvDeptInfo";
				System.out.println(endLog);
				Logger.info.println(endLog);
			} catch (Exception e) {
				Logger.err.println(sepoainfo.getSession("id"), "BATCH", "Unexpected Exception.", e);
			} finally {
				sepoainfo.unsetThreadLocal();
				isRunning = true;
			}
		}
	}
}
