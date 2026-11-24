package sepoa.svc.batch.cbo_006;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;

/**
 * 
 * @author PARK
 * 사용자정보_120
 *
 */
public class BatchRecvUserInfo extends SepoaBatchService {

	static boolean isRunning = true; // 처리중인데 실행되는 오류를 막기 위한 변수

	static String curDate = SepoaDate.getShortDateString();
	static String curTime = SepoaDate.getShortTimeString();
	static boolean log_enable_flag = false;
	
	public BatchRecvUserInfo(String opt, SepoaInfo sepoainfo) throws SepoaServiceException {
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
	public List<Map<String,String>> getTargetRecord(ConnectionContext ctx) throws Exception {
		Logger.info.println(info.getSession("ID"), this, "###########  getTargetRecord Method Start USER,POSITION ###########");
		SepoaFormater sf =null;
		  
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this,"doQuery_Target");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"),this,ctx,sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
		
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return sf.getListMap();
	}

	// 타겟데이터 수행 메소드
	public void setTargetRecord(ConnectionContext ctx, List<Map<String,String>> sfData) throws Exception {
		Logger.info.println(info.getSession("ID"), this, "###########  setTargetRecord Method Start USER,POSITION ###########");
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Map<String, String> paramMap = new HashMap<String, String>();
			String exceptIds = CommonUtil.getConfig("sepoa.system.user.ids");
			paramMap.put("EXCEPT_IDS", exceptIds);
			sxp = new SepoaXmlParser(this, "DEL_FLAG_Y");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(paramMap);

			Map<String, Map<String, String>> positionMap = new HashMap<String, Map<String, String>> ();
			for(Map<String,String> target : sfData  ) {
				// SUSMT에 ID가 있는지 확인
				sxp = new SepoaXmlParser(this, "checkExistSUSMT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(target));
				
//				target.put("DEL_FLAG", "N");
				
				//기존사용자라면
				if(sf.getRowCount() > 0) {
					String orgProfileCode = MapUtils.getString(sf.getListMap().get(0), "MENU_PROFILE_CODE", "");
					if(!"".equals(orgProfileCode)) {//사용중이던 프로파일 그대로 진행
						target.put("MENU_PROFILE_CODE", orgProfileCode);
					} else {
						//이 구간은 사실 탈 일없음
						target.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
					}
				} else {//기존사용자가 아니라면 일반사용자 프로파일 추가
					target.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
				}
				target.put("DATE_FORMAT", CommonUtil.getConfig("sepoa.user.default.date_format"));
				  
				if (sf.getRowCount() > 0) {
					// 있으면 SUSMT,SCODE 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(target);
					
				} else {
					// 없으면 SUSMT, SCODE Insert
					target.put("PASSWORD", "!!!NOT_USE");
					target.put("USER_TYPE", "S");
					
					sxp = new SepoaXmlParser(this, "doInsertSUSMT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(target);
				}
				String position = target.get("POSITION");
				if(StringUtils.isNotBlank(position)) {
					positionMap.put(position, target);
				}
			}
			
			Collection<Map<String, String>> positionCollection = positionMap.values();
			for(Map<String, String> target : positionCollection) {
				sxp = new SepoaXmlParser(this, "checkExistSCODE");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(target));
				
				if (sf.getRowCount() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdatePositionCode");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(target);
					
				} else {
					sxp = new SepoaXmlParser(this, "doInsertPositionCode");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(target);
	
				}
			}
		} catch (Exception e) {
			// 로깅
			Rollback();
			String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] ERROR-BatchRecvUserInfo : " + e;
			System.out.println(log);
			Logger.err.println(log);
		}
		
	}

	// 실행(타켓 취득 및 비즈니스로직 수행)
	public SepoaOut execute() throws SepoaServiceException {
		Logger.debug.println("############### USER,POSITION 배치실행	###############");
		setFlag(true);
		setStatus(1);
		ConnectionContext ctx = getConnectionContext();
		try {
			joinTransaction("kised");
			ConnectionContext ctx2 = getConnectionContext("kised");
			// 타겟 취득
			List<Map<String,String>> resultList = getTargetRecord(ctx2);
			Logger.info.println(info.getSession("ID"), this, "getTargetRecord(USER, POSITION) size : " + resultList.size());
			// 타겟 세팅
			if (resultList.size() > 0) {
				setTargetRecord(ctx, resultList);
			}
			Commit();
		} catch (Exception exception) {
			exception.printStackTrace();
			Rollback();
			setFlag(false);
			setStatus(0);
			Logger.err.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(BatchRecvUserInfo.class.getSimpleName()+"() ERROR : ").append(exception.getMessage()).toString());
		}finally {
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

			sepoainfo.setThreadLocal();

			try {				
				String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] START : BatchRecvUserInfo";
				System.out.println(startLog);
				Logger.info.println(startLog);
				SepoaBatchService.initialize();
				SepoaBatchService.run(new BatchRecvUserInfo("TRANSACTION", sepoainfo));

				String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] END : BatchRecvUserInfo";
				System.out.println(endLog);
				Logger.info.println(endLog);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				isRunning = true;
				sepoainfo.unsetThreadLocal();
			}
		}
	}

}
