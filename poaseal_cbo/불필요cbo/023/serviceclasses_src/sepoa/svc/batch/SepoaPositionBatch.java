package sepoa.svc.batch;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaFormater;

@Description("직급정보 배치 서비스")
public class SepoaPositionBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaPositionBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaPositionBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("직급정보 배치 서비스")
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
		ConnectionContext ctx2 = null;
		try {
			ctx = getConnectionContext();
			joinTransaction("kosaf");
			ctx2 = getConnectionContext("kosaf");

			List<Map<String, String>> dataList = getTargetRecord(ctx2);

			if (dataList != null && dataList.size() != 0) {
				setTargetRecord(ctx, dataList);
			} else {
				throw new Exception("PositionList is Empty!");
			}

			Commit();
		} catch (Exception exception) {
			Rollback();
			exception.printStackTrace();
			setFlag(false);
			setStatus(0);
			Logger.err.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ")
					.append(exception.getMessage()).toString());
		} finally {
			try {
				ctx.getConnection().close();
			} catch (Exception e) {
			}
			
			try {
				ctx2.getConnection().close();
			}  catch (Exception e) {
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
		String house_code = "023";
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();

			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : SepoaPositionBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaPositionBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaPositionBatch";
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
		ConnectionContext ctx = var1;

		List<Map<String, String>> positionInfoList = new ArrayList<Map<String, String>>();

		try {
			sxp = new SepoaXmlParser(this, "doSelectPositionInfo");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));

			if (sf.getRowCount() > 0) {
				positionInfoList = sf.getListMap();
			} else {
				throw new Exception("Batched TargetRecord POSITION Fail!!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.err.print(e);
		}

		return positionInfoList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		for (Map<String, String> positionData : var2) {
			try {
				sxp = new SepoaXmlParser(this, "checkExistPositionCode");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(positionData));
				
				if (sf.getRowCount() > 0) {
					// 있으면 업데이트 - KO
					positionData.put("LANGUAGE", "KO");

					sxp = new SepoaXmlParser(this, "doUpdatePositionCode");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(positionData);

				} else {
					// 없으면 넣기 - KO
					positionData.put("LANGUAGE", "KO");

					sxp = new SepoaXmlParser(this, "doInsertPositionCode");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(positionData);
				}

				Commit();
			} catch (Exception e) {
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-SepoaPositionBatch : " + e;
				Logger.info.println(log);
				Logger.err.println(e);
				Rollback();
				continue;
			}
		}

	}
}
