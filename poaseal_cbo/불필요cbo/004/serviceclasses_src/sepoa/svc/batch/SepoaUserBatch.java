package sepoa.svc.batch;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Base64;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaFormater;

@Description("사용자정보 배치 서비스")
public class SepoaUserBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	final private static String URL = "http://is.poscochemtech.com:8080/RESTful_GetHrInfo/rest/emp/para/1/%25";

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
		ConnectionContext ctx2 = null;
		try {
			Map<String, Object> dataList = getTargetRecord(null, null);

			joinTransaction("easysign");
			ctx = getConnectionContext();  
			ctx2 = getConnectionContext("easysign");  
			
			List<Map<String, String>> positionMap = (List<Map<String, String>>) dataList.get("POSITION_DATA");
			for(Map<String, String> map : positionMap) {
				map.put("HOUSE_CODE" , "004");
				map.put("COMPANY_CODE" , "1000");
			}
			List<Map<String, String>> userMap = (List<Map<String, String>>) dataList.get("USER_DATA");
			for(Map<String, String> map : userMap) {
				map.put("HOUSE_CODE" , "004");
				map.put("COMPANY_CODE" , "1000");
			}
			setTargetRecord(ctx, dataList);

			positionMap = (List<Map<String, String>>) dataList.get("POSITION_DATA");
			for(Map<String, String> map : positionMap) {
				map.put("HOUSE_CODE" , "000");
				map.put("COMPANY_CODE" , "1000");
			}
			userMap = (List<Map<String, String>>) dataList.get("USER_DATA");
			for(Map<String, String> map : userMap) {
				map.put("CTGY", "Contract");//계약임을 구분하기 위한 구분값
				map.put("HOUSE_CODE" , "000");
				map.put("COMPANY_CODE" , "1000");
			}
			setTargetRecord(ctx2, dataList);

			Commit();
		} catch (Exception exception) {
			Rollback();
			// exception.printStackTrace();
			setFlag(false);
			setStatus(0);
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ")
					.append(exception.getMessage()).toString());
		} finally {
			try {
				ctx.getConnection().close();
			} catch (SQLException e) {
				// e.printStackTrace();
			}
			try {
				ctx2.getConnection().close();
			} catch (SQLException e) {
				// e.printStackTrace();
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
		String house_code = "004"; // 하우스코드
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
			// e.printStackTrace();
		} finally {
			isRunning = false;
			sepoainfo.unsetThreadLocal();
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getTargetRecord(ConnectionContext var1, String s) throws Exception {

		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		
		HttpURLConnection httpConn = null;
		InputStream is = null;
		List<Map<String, String>> list = null;
		Map<String, Map<String, String>> positionMap = new HashMap<String, Map<String, String>>();
		try {
			httpConn = (HttpURLConnection) new URL(URL).openConnection();
			httpConn.setRequestProperty("Accetp", "application/json");
			httpConn.setRequestMethod("GET");
			httpConn.connect();

			int respCode = httpConn.getResponseCode();
			if (respCode == HttpServletResponse.SC_OK) {
				is = httpConn.getInputStream();
				String json = IOUtils.toString(is, "UTF8");
				Map<String, Object> map = new HashMap<String, Object>();
				map = (Map<String, Object>) new Gson().fromJson(json, map.getClass());
				list = (List<Map<String, String>>) map.get("empInfo");
				
				for(Map<String, String> empMap : list) {
					String deptcd = empMap.get("jikwicd");
					positionMap.put(deptcd, empMap);
				}
				dataMap.put("POSITION_DATA", new ArrayList<Map<String, String>>(positionMap.values()));
				dataMap.put("USER_DATA", list);
				
			} else {
				throw new Exception("Error Http Code : " + respCode);
			}

		} catch (Exception e) {
			throw new Exception("Failed to retrive JSON data", e);
		} finally {
			try {
				httpConn.disconnect();
			} catch (Exception e) {
			}
		}
		return dataMap;
	}

	public void setTargetRecord(ConnectionContext var1, Map<String, Object> dataMap) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		// 직위정보 먼저 scode에 ux049에 넣기
		List<Map<String, String>> positionMap = (List<Map<String, String>>) dataMap.get("POSITION_DATA");
		for (Map<String, String> position : positionMap) {
			try {
				// UX049에 Code가 있는지 먼저 조회
				sxp = new SepoaXmlParser(this, "checkExistPositionCode");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(position));

				if (sf.getRowCount() > 0) {
					// 있으면 업데이트 - KO
					position.put("LANGUAGE", "KO");
					
					sxp = new SepoaXmlParser(this, "doUpdatePositionCode");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(position);
					
				} else {
					// 없으면 넣기 - KO
					position.put("LANGUAGE", "KO");					
					
					sxp = new SepoaXmlParser(this, "doInsertPositionCode");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(position);
					
				}
			} catch (Exception e) {
				// 로깅
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-SepoaUserBatch-Position : " + e;
				System.out.println(log);
				Logger.info.println(log);
				continue;
			}
		}

		//유저 정보 넣기
		List<Map<String, String>> userMap = (List<Map<String, String>>) dataMap.get("USER_DATA");
		for (Map<String, String> user : userMap) {
			try {
				// SUSMT에 ID가 있는지 확인
				sxp = new SepoaXmlParser(this, "checkExistSUSMT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(user));

				// 활성여부(UsageState) -> DEL_FLAG 치환하기
				if(StringUtils.isBlank(user.get("retdat"))) {
					user.put("DEL_FLAG", "N");
				} else {
					user.put("DEL_FLAG", "Y");
				}
				if("Contract".equals(user.get("CTGY"))) { //계약일경우 
					if(sf.getRowCount() > 0) {
						String orgProfileCode = MapUtils.getString(sf.getListMap().get(0), "MENU_PROFILE_CODE", "");
						String orgUser_type = MapUtils.getString(sf.getListMap().get(0), "USER_TYPE", "");
						if(!"".equals(orgProfileCode)) {
							user.put("MENU_PROFILE_CODE", orgProfileCode);
							user.put("USER_TYPE", orgUser_type);
						} else {
							//이 구간은 사실 탈 일없음
							user.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
							user.put("USER_TYPE", "E");
						}
					} else {
						user.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
						user.put("USER_TYPE", "E");
					}
				}else { //인장일경우
					//업데이트 될 때 원래의 프로파일코드 유지하기 위함(인감관리자일 경우가 있기에)
					if(sf.getRowCount() > 0) {
						String orgProfileCode = MapUtils.getString(sf.getListMap().get(0), "MENU_PROFILE_CODE", "");
						if(!"".equals(orgProfileCode)) {
							user.put("MENU_PROFILE_CODE", orgProfileCode);
						} else {
							//이 구간은 사실 탈 일없음
							user.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
						}
					} else {
						user.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
					}
					
					//부서가 법무팀 부서코드이면(여러개 가능) 법무팀 프로파일 코드를 부여하기
					String[] rmDeptCode = CommonUtil.getConfig("sepoa.rm.dept.code", "V100_01").split(",");
					if(Arrays.asList(rmDeptCode).contains(user.get("deptcd"))){
						user.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.legal.porfile.code", "MUP21111000001"));
					}
				}
				
				if (sf.getRowCount() > 0) {
					// 있으면 SUSMT,SADDR 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(user);

					sxp = new SepoaXmlParser(this, "doUpdateSADDR");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(user);
					
				} else {
					// 없으면 SUSMT, SADDR Insert
					user.put("PASSWORD", getEncryptedPassword(user.get("empcd")));
					user.put("USER_TYPE", "S");
					
					sxp = new SepoaXmlParser(this, "doInsertSUSMT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(user);

					sxp = new SepoaXmlParser(this, "doInsertSADDR");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(user);

				}
			} catch (Exception e) {
				// 로깅
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-SepoaUserBatch : " + e;
				System.out.println(log);
				Logger.info.println(log);
				continue;

			}
		}
	}

	@Override
	public List<Map<String, String>> getTargetRecord(ConnectionContext var1) throws Exception {
		List<Map<String, String>> dataMapList = new ArrayList<Map<String, String>>();

		return dataMapList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {

	}

	private String getEncryptedPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {

		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));

		byte byteData[] = md.digest();
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < byteData.length; i++)
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));

//        return Base64Coder.encodeString(sb.toString());
		return Base64.base64Encode(sb.toString()).toUpperCase();
	}
}
