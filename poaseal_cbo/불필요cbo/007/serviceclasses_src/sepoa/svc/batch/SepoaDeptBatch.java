package sepoa.svc.batch;

import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.google.gson.Gson;

import sepoa.fw.batch.SepoaBatchService;
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
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.common.RestUtil;

@Description("부서정보 배치 서비스")
public class SepoaDeptBatch extends SepoaBatchService {

	static boolean isRunning = true; // 처리중인데 실행되는 오류를 막기 위한 변수

	static String curDate = SepoaDate.getShortDateString();
	static String curTime = SepoaDate.getShortTimeString();
	static boolean log_enable_flag = false;
	
	public SepoaDeptBatch(String opt, SepoaInfo sepoainfo) throws SepoaServiceException {
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
		List<Map<String, String>> deptDataList = null;
		
		try {
			RestUtil util = new RestUtil();
			String getDeptApiHost = CommonUtil.getConfig("sepoa.secl.hr.rest.host", "http://66.10.54.49:13039");
			String getDeptApiUrl =  CommonUtil.getConfig("sepoa.secl.hr.dept.rest.url", "/cube_bmd_server/hr/getDeptInfoList");
			String secretKey = CommonUtil.getConfig("sepoa.secl.hr.secret.key", "4BCF4F4800643_DEV");
			String url = getDeptApiHost + getDeptApiUrl;
			String restResult = null;
			
			util.setHeaderData(RestUtil.REQ_METHOD, "POST");
			util.setHeaderData("Content-Type", "application/json;charset=UTF-8");
			
			SeclTimeHash timeHash = new SeclTimeHash();
			if(timeHash != null) {
				 Map<String,String> mapTimeHash = new HashMap<>();
                 mapTimeHash = timeHash.getTimeNowHash(secretKey);
                 util.setHeaderData("Access-Token", mapTimeHash.get("hashValue"));
                 util.setHeaderData("Access-Time" , mapTimeHash.get("hashTime"));
			} else {
				throw new Exception("Failed to generate hash value required to accept dept information.");
			}
			
			try {
				restResult = util.connect(url);
			} catch (Exception e) {
				throw new Exception("A problem occurred while using the Department Information Inquiry API.", e);
			}
		
			if(restResult != null && util.getResponseCode() == HttpURLConnection.HTTP_OK) {
				//부서 조회 API 조회 결과가 어떤 규격인지 20230308 현재 시간으로 확인할 수 없음
				//상세 명세서 도착 후 아래에서 파싱로직 추가 개발 필요
				Map<String, Object> resultObj = new Gson().fromJson(restResult, Map.class);
				if(resultObj != null && "SUCCESS".equalsIgnoreCase(String.valueOf(resultObj.get("resultCd")))) {
					deptDataList = (List<Map<String, String>>) resultObj.get("data");
					if(deptDataList.size() == 0) {
						throw new Exception("There are no Department queried.");
					}
				} else {
					Logger.warn.println("The dept information API call was successful, but the status value is not Success.");
				}
			} else {
				throw new Exception("Department information inquiry API call failed.");
			}
		}catch(Exception e) {
			Logger.err.println("Failed to get Department information.", e);
			throw new Exception("Failed to get Department information.", e);
		}
		
		return deptDataList;
	}

	// 타겟데이터 수행 메소드
	public void setTargetRecord(ConnectionContext ctx ,List<Map<String, String>> deptDataList) throws Exception {
		Logger.info.println(info.getSession("ID"), this, "########  setTargetRecord Method Start DEPT ########");
		Gson gson = new Gson();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		// 부서 정보 INSERT, UPDATE 건 by 건 Transaction
		for(Map<String, String> deptData : deptDataList) {
			try {
				sxp = new SepoaXmlParser(this, "doQuerySDEPT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(deptData));
				
				String deptFlag = MapUtils.getString(deptData, "DELETE_DATE", null);
				if(deptFlag == null || "".equals(deptFlag)) {
					deptData.put("DEL_FLAG", "N");
				} else {
					deptData.put("DEL_FLAG", "Y");
				}
				
				if (sf.getRowCount() > 0) {
					// 부서가 있으면 SDEPT 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(deptData);
				} else {
					// 부서가 없으면 SDEPT insert
					sxp = new SepoaXmlParser(this, "doInsertSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(deptData);
				}
				
				Commit();
			} catch (Exception e) {
				Rollback();
				Logger.err.println("[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())+ "] An exception occurred while adding the dept. deptData : " + gson.toJson(deptData), e);
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
			
			List<Map<String, String>> deptDataList = getTargetRecord(ctx);
			if (deptDataList.size() > 0) {
				setTargetRecord(ctx, deptDataList);
			}
			
			Commit();
		} catch (Exception exception) {
			exception.printStackTrace();
			Rollback();
			setFlag(false);
			setStatus(0);
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(SepoaDeptBatch.class.getSimpleName()+"() ERROR : ").append(exception.getMessage()).toString());
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

			String house_code = "007"; // 하우스코드
			SepoaInfo sepoainfo = new SepoaInfo(house_code,
					"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");
			try {
				sepoainfo.setThreadLocal();

				String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] START : BatchRecvDeptInfo";
				System.out.println(startLog);
				Logger.info.println(startLog);
				SepoaBatchService.initialize();
				SepoaBatchService.run(new SepoaDeptBatch("TRANSACTION", sepoainfo));

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
