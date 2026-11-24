package sepoa.svc.batch;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

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

@Description("사용자정보 배치 서비스")
public class SepoaUserBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaUserBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaUserBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("사용자정보 배치 서비스")
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
			} else {
				throw new Exception("UserList is Empity!");
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
		String house_code = "019";
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();

			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : SepoaUserBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaUserBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaUserBatch";
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

		List<Map<String, String>> userInfoList = new ArrayList<Map<String, String>>();

		try {
			sxp = new SepoaXmlParser(this, "doSelectUserInfo");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));

			if (sf.getRowCount() > 0) {
				userInfoList = sf.getListMap();
			} else {
				throw new Exception("Get TargetRecord Empity!!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.info.print("Get TargetRecord Fail!!");
		}
		return userInfoList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		Map<String, String> positionMap = new HashMap<String, String>();
		
		for (Map<String, String> userData : var2) {
			try {
				//USER_ID 존재여부 검사
				if("".equals(userData.get("USER_ID"))) {
					throw new Exception("USER_ID is Empity !! : " + userData);
				}
				
				//USER_ID 대문자 처리
				userData.put("USER_ID", userData.get("USER_ID").toUpperCase());
				
				//INT_TYPE에 따른 동작 쿼리 설정 및 필요 파라미터 세팅
				String intType = userData.get("INT_TYPE");
				switch(intType.toUpperCase()) {
					case "C":
					case "U":
						sxp = new SepoaXmlParser(this, "checkExistSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						sf = new SepoaFormater(ssm.doSelect(userData));
						
						userData.put("PASSWORD", SepoaAuthEncryptManager.getEncrypted((userData.get("USER_ID"))));
						userData.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
						
						//*****************************************
						// 회사코드_직위코드 로 변경  [2023-11-08 작업 예정]
						String companyCode = MapUtils.getString(userData, "COMPANY_CODE", null);
						String position = MapUtils.getString(userData, "POSITION", null);
						
						if(companyCode != null && position != null) {
							userData.put("POSITION_COMBINE", userData.get("COMPANY_CODE") + "_" + userData.get("POSITION"));
							userData.put("POSITION", userData.get("COMPANY_CODE") + "_" + userData.get("POSITION"));
						}
						
						if(sf.getRowCount() > 0) {
							//업데이트용 쿼리
							userData.put("DEL_FLAG", "N");
							sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							ssm.doUpdate(userData);
						} else {
							//생성용 쿼리
							userData.put("DEL_FLAG", "N");
							sxp = new SepoaXmlParser(this, "doInsertSUSMT");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							ssm.doInsert(userData);
						}
						
						break;
					case "D":
						//삭제 쿼리
						userData.put("DEL_FLAG", "Y");
						sxp = new SepoaXmlParser(this, "doDeleteSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(userData);
						
						break;
					case "E" :
						break;
					
					default :
						throw new Exception("INT_TYPE error !! : " + userData);
				}
				
				if(userData.containsKey("POSITION_COMBINE")) {
					positionMap.put(userData.get("POSITION_COMBINE"), userData.get("POSITION_LOC"));
				}
				
				// IF_FLAG = Y 업데이트
				userData.put("IF_RESULT", "Y");
			} catch (Exception e) {
				Rollback();
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())+ "] ERROR-SepoaUserBatch : " + userData;
				Logger.err.println(log, e);
				
				// IF_FLAG = F 업데이트
				userData.put("IF_RESULT", "F");
			} finally {
				sxp = new SepoaXmlParser(this, "doUpdateTmp");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				ssm.doUpdate(userData);
				Commit();
			}
		}
		
		//*****************************************
		// SCODE 데이터 수정  [2023-11-08 작업 예정] - 필요없음
		
		// 직급 정보 INSERT, UPDATE 일괄 Transaction
		if(positionMap.size() > 0) {
			for(Entry<String, String> entry : positionMap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				
				if(key != null && !"".equals(key) && value != null && !"".equals(value)) {
					Map<String, String> positionCommonParams = new HashMap<String, String>();
					positionCommonParams.put("POSITION", entry.getKey());
					positionCommonParams.put("POSITION_LOC", entry.getValue());
					
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
				} else {
					Logger.err.println("entry is empty! : " + entry);
				}
			}
			
			Commit();
			
			//*****************************************
			// 양쪽 was에다가 cache.clear()날려주면댐
			// 
			// ## batch properties 추가 항목
			// sepoa.cache.remote.server=http://10.100.46.218:8080;http://10.100.46.219:8080
			
			HttpURLConnection conn = null;
			String remoteServerUrl = CommonUtil.getConfig("sepoa.cache.remote.server", null);
			int connectTimeout = 5000; 
			int readTimeout = 5000;
			
			if(!StringUtils.isBlank(remoteServerUrl)) {
				String[] remoteServerArr = remoteServerUrl.split(";");
				
				if(remoteServerArr != null && remoteServerArr.length > 0) {
					for(String url : remoteServerArr) {
						try {
							String requestURL = url + "/SepoaJSCommon.sux?mode=cache&action=clear";
							conn = (HttpURLConnection) new URL(requestURL).openConnection();
							conn.setRequestMethod("GET");
							conn.setConnectTimeout(connectTimeout);
							conn.setReadTimeout(readTimeout);
							
							conn.connect();
							
							int responseCode = conn.getResponseCode();
							
							if(responseCode == HttpURLConnection.HTTP_OK) {
								// 성공
								Logger.info.println("Success");
							} else {
								throw new Exception("responseCode is not OK");
							}
							
						} catch (Exception e) {
							Logger.err.println("=======================================");
							Logger.err.println("[HttpURLConnection] Cache Clear Fail!!");
							Logger.err.println("=======================================");
						} finally {
							if(conn != null) {
								conn.disconnect();
							}
						}
					}
				}
			}
		}
		
	}
}
