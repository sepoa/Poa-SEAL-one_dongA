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

@Description("부서정보 배치 서비스")
public class SepoaDeptBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaDeptBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaDeptBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("부서정보 배치 서비스")
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
			
			// 임시테이블에 데이터 삽입
			getEAIData(ctx);
			
			// 타겟 데이터 조회
			List<Map<String, String>> dataList = getTargetRecord(ctx);

			if (dataList != null && dataList.size() != 0 ) {
				setDeleteSDEPT(ctx, dataList);
				setTargetRecord(ctx, dataList);
			} else {
				throw new Exception("EAI Table is Empity!");
			}

			Commit();
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
		String house_code = "020";
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();

			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : SepoaDeptBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaDeptBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaDeptBatch";
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

		List<Map<String, String>> deptInfoList = new ArrayList<Map<String, String>>();

		try {
			// BAT_UP_IMDEPT 반영 안된 데이터 조회
			sxp = new SepoaXmlParser(this, "doSelectTmpDept");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect());
			
			if (sf.getRowCount() > 0) {
				deptInfoList = sf.getListMap();
			}
			
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			Logger.info.print("Get TargetRecord Fail!!");
		}
		return deptInfoList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		// 전자인장 부서목록 수정 처리
		for (Map<String, String> deptData : var2) {
			try {
				// 조건별 SDEPT 삭제/수정/신규등록 처리
				// SDEPT에 부서코드별 값이 있으면 UPDATE, 없으면 INSERT(삭제 조건 아직 모름)
				sxp = new SepoaXmlParser(this, "checkExistSDEPT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect());
				
				if (sf.getRowCount() > 0) {
					// 수정 (삭제 추가 필요) 
					sxp = new SepoaXmlParser(this, "doUpdateSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(deptData);
					
				} else {
					// 신규 추가
					sxp = new SepoaXmlParser(this, "doInsertSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(deptData);
				}
				
				// IF_FLAG = Y 업데이트
				deptData.put("IF_RESULT", "Y");
				
			} catch (Exception e) {
				Rollback();
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())+ "] ERROR-SepoaUserBatch : " + deptData;
				Logger.err.println(log, e);
				
				// IF_FLAG = F 업데이트
				deptData.put("IF_RESULT", "F");
			} finally {
				sxp = new SepoaXmlParser(this, "doUpdateTmp");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				ssm.doUpdate(deptData);
				Commit();
			}
		}
	}
	
	public void getEAIData(ConnectionContext var1) throws Exception {
		// EAI 테이블 데이터를 임시테이블(BAT_UP_IMDEPT)로 Insert
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = var1;
		
		try {
			// DEPT_HMS >> BAT_UP_IMDEPT 변경 데이터 추가
			sxp = new SepoaXmlParser(this, "doInsertImDept");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate();
			
			Commit();
			
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			Logger.info.print("[ERROR] Failed insert into BAT_UP_IMDEPT from EAI");
		}
	}
	
	public void setDeleteSDEPT(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		// 전자인장 부서목록과 EAI 부서 목록을 비교해서 삭제 처리
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		List<Map<String, String>> sdeptList = new ArrayList<Map<String, String>>();
		
		try {
			sxp = new SepoaXmlParser(this, "doSelectSDEPT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect());
			
			if (sf.getRowCount() > 0) {
				sdeptList = sf.getListMap();
				
				boolean del_flag = true;
				
				for(Map<String, String> sdept : sdeptList) {
					for(Map<String, String> tmpdept : var2) {
						if(tmpdept.containsKey("DEPT") && tmpdept.get("DEPT").equals(sdept.get("DEPT"))) {
							del_flag = false;
							break;
						}
					}
					
					if(del_flag) {
						sxp = new SepoaXmlParser(this, "doDeleteSDEPT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(sdept);
						Logger.info.print("[" + sdept.get("DEPT") +"]" + sdept.get("DEPT_NAME_LOC") + " Delete");
					}
					del_flag = true;
				}
			}
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			Logger.info.print("[ERROR] SDEPT TARGET DELETE FAILED");
		}
		
	}
	
}
