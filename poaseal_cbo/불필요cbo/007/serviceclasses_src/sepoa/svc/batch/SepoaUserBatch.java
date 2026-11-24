package sepoa.svc.batch;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;

import com.google.gson.Gson;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Base64;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.common.RestUtil;

/**
 * 
 * @author PARK
 * 사용자정보_120
 *
 */
public class SepoaUserBatch extends SepoaBatchService {

	static boolean isRunning = true; // 처리중인데 실행되는 오류를 막기 위한 변수

	static String curDate = SepoaDate.getShortDateString();
	static String curTime = SepoaDate.getShortTimeString();
	static boolean log_enable_flag = false;
	
	public SepoaUserBatch(String opt, SepoaInfo sepoainfo) throws SepoaServiceException {
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
		List<Map<String, String>> userDataList = null;
		  
		try {
			RestUtil util = new RestUtil();
			String getUserApiHost = CommonUtil.getConfig("sepoa.secl.hr.rest.host", "http://66.10.54.49:13039");
			String getUserApiUrl =  CommonUtil.getConfig("sepoa.secl.hr.emp.rest.url", "/cube_bmd_server/hr/getEmployeeInfoList");
			String secretKey = CommonUtil.getConfig("sepoa.secl.hr.secret.key", "4BCF4F4800643_DEV");
			String url = getUserApiHost + getUserApiUrl;
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
				throw new Exception("Failed to generate hash value required to accept user information.");
			}
			
			try {
				restResult = util.connect(url);
			} catch (Exception e) {
				throw new Exception("A problem occurred while using the User Information Inquiry API.", e);
			}
		
			if(restResult != null && util.getResponseCode() == HttpURLConnection.HTTP_OK) {
				//사용자 조회 API 조회 결과가 어떤 규격인지 20230308 현재 시간으로 확인할 수 없음
				//상세 명세서 도착 후 아래에서 파싱로직 추가 개발 필요
				Map<String, Object> resultObj = new Gson().fromJson(restResult, Map.class);
				if(resultObj != null && "SUCCESS".equalsIgnoreCase(String.valueOf(resultObj.get("resultCd")))) {
					userDataList = (List<Map<String, String>>) resultObj.get("data");
					if(userDataList.size() == 0) {
						throw new Exception("There are no users queried.");
					}
				} else {
					Logger.warn.println("The user information API call was successful, but the status value is not Success." + restResult);
				}
			} else {
				throw new Exception("User information inquiry API call failed." + restResult);
			}
		}catch(Exception e) {
			Logger.err.println("Failed to get user information.", e);
			throw new Exception("Failed to get user information.", e);
		}
		
		return userDataList;
	}

	// 타겟데이터 수행 메소드
	public void setTargetRecord(ConnectionContext ctx, List<Map<String,String>> userDataList) throws Exception {
		Logger.info.println(info.getSession("ID"), this, "###########  setTargetRecord Method Start USER,POSITION ###########");
		Gson gson = new Gson();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		Map<String, String> positionMap = new HashMap<String, String>();
		
		// 사용자 정보 INSERT, UPDATE 건 by 건 Transaction
		for(Map<String, String> userData : userDataList) {
			try {
				sxp = new SepoaXmlParser(this, "doQueryUser");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(userData));
				
				if(sf.getRowCount() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdateUser");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(userData);	
				} else {
					userData.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.secl.batch.default.profile.code", "MUP21111900000"));
					userData.put("DATE_FORMAT", CommonUtil.getConfig("sepoa.user.default.date_format", "%Y/%m/%d"));
					userData.put("PASSWORD", getEncryptedPassword("!!!NOT_USE"));
					userData.put("USER_TYPE", "S");
					
					sxp = new SepoaXmlParser(this, "doInsertUser");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(userData);
				}
				
				String position = MapUtils.getString(userData, "JIKGUB", null);
				String positionNm = MapUtils.getString(userData, "JIKGUBNM", null);
				if(position != null && positionNm != null) {
					positionMap.put(userData.get("JIKGUB"), userData.get("JIKGUBNM"));
				}
				
				Commit();
			} catch (Exception e) {
				Rollback();
				Logger.err.println("[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())+ "] An exception occurred while adding the user. userData : " + gson.toJson(userData), e);
				continue;
			}
		}
		
		// 직급 정보 INSERT, UPDATE 일괄 Transaction
		if(positionMap.size() > 0) {
			for(Entry<String, String> entry : positionMap.entrySet()) {
				Map<String, String> positionCommonParams = new HashMap<String, String>();
				positionCommonParams.put("JIKGUB", entry.getKey());
				positionCommonParams.put("JIKGUBNM", entry.getValue());
				
				sxp = new SepoaXmlParser(this, "doQueryPosition");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(positionCommonParams));
				
				if(sf.getRowCount() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdatePosition");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(positionCommonParams);
				} else {
					sxp = new SepoaXmlParser(this, "doInsertPosition");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(positionCommonParams);
				}
			}
			
			Commit();
		}
	}

	// 실행(타켓 취득 및 비즈니스로직 수행)
	public SepoaOut execute() throws SepoaServiceException {
		Logger.debug.println("############### USER,POSITION 배치실행	###############");
		setFlag(true);
		setStatus(1);
		ConnectionContext ctx = getConnectionContext();
		try {
			// 타겟 취득
			List<Map<String,String>> userDataList = getTargetRecord(ctx);
			Logger.info.println(info.getSession("ID"), this, "getTargetRecord(USER, POSITION) size : " + userDataList.size());
			
			// 타겟 세팅
			if (userDataList.size() > 0) {
				setTargetRecord(ctx, userDataList);
			}
			
			Commit();
		} catch (Exception exception) {
			exception.printStackTrace();
			Rollback();
			setFlag(false);
			setStatus(0);
			Logger.err.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(SepoaUserBatch.class.getSimpleName()+"() ERROR : ").append(exception.getMessage()).toString());
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

			String house_code = "007"; // 하우스코드
			SepoaInfo sepoainfo = new SepoaInfo(house_code,
					"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

			sepoainfo.setThreadLocal();

			try {				
				String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] START : BatchRecvUserInfo";
				System.out.println(startLog);
				Logger.info.println(startLog);
				SepoaBatchService.initialize();
				SepoaBatchService.run(new SepoaUserBatch("TRANSACTION", sepoainfo));

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
	
	private String getEncryptedPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));

		byte byteData[] = md.digest();
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < byteData.length; i++) {
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}

		return Base64.base64Encode(sb.toString()).toUpperCase();
	}
}
