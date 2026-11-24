package sepoa.svc.batch;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import sepoa.fw.srv.Base64;
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
		ConnectionContext ctx2 = null;
		try {

			joinTransaction("donga_gw");
			ctx = getConnectionContext("donga_gw");
			ctx2 = getConnectionContext();

			Map<String, Object> dataList = getTargetRecord(ctx, null);

			List<Map<String, String>> userMap = (List<Map<String, String>>) dataList.get("USER_INFO");
			for (Map<String, String> map : userMap) {
				map.put("HOUSE_CODE", "012");
			}

			setTargetRecord(ctx2, dataList);

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
		String house_code = "012"; // 하우스코드
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

	@SuppressWarnings("unchecked")
	public Map<String, Object> getTargetRecord(ConnectionContext var1, String s) throws Exception {

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = var1;

		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		List<Map<String, String>> userInfoList = new ArrayList<Map<String, String>>();
		Map<String, String> sampleMap = new HashMap<String, String>();

		// 사용자정보조회
		sxp = new SepoaXmlParser(this, "doSelectUserInfo");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		sf = new SepoaFormater(ssm.doSelect(sampleMap));

		if (sf.getRowCount() > 0) {
			userInfoList = sf.getListMap();
			dataMap.put("USER_INFO", userInfoList);
		}

		return dataMap;
	}

	public void setTargetRecord(ConnectionContext var1, Map<String, Object> dataMap) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		// 유저 정보 넣기
		List<Map<String, String>> userMap = (List<Map<String, String>>) dataMap.get("USER_INFO");
		for (Map<String, String> user : userMap) {
			try {

				user.put("USER_ID", user.get("USER_ID").toUpperCase());

				// SUSMT에 ID가 있는지 확인
				sxp = new SepoaXmlParser(this, "checkExistSUSMT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(true);
				sf = new SepoaFormater(ssm.doSelect(user));

				// 업데이트 될 때 원래의 프로파일코드 유지하기 위함(인감관리자일 경우가 있기에)
				if (sf.getRowCount() > 0) {
					String orgProfileCode = MapUtils.getString(sf.getListMap().get(0), "MENU_PROFILE_CODE", "");

					if (!"".equals(orgProfileCode)) {
						user.put("MENU_PROFILE_CODE", orgProfileCode);
					} else {
						// 이 구간은 사실 탈 일없음
						user.put("MENU_PROFILE_CODE",
								CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
					}
				} else {
					user.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
				}

				if (sf.getRowCount() > 0) {
					// 있으면 SUSMT 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(true);
					ssm.doUpdate(user);
				} else {
					// 없으면 SUSMT Insert
					user.put("PASSWORD", SepoaAuthEncryptManager.getEncrypted((user.get("USER_ID"))));
					sxp = new SepoaXmlParser(this, "doInsertSUSMT");
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
				Logger.info.println(e.getMessage());
				continue;
			}
		}
		
		// 동아제약의 SBUPI 데이터 조회
		sxp = new SepoaXmlParser(this, "doSelect_SBUPI");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		ssm.setLogEnable(false);
		SepoaFormater bupiSF = new SepoaFormater(ssm.doSelect(new HashMap<String, String>()));
		List<Map<String, String>> bupiList = bupiSF.getListMap();
		
		Map<String, Map<String, String>> bupiObj = new HashMap<>();
		for(Map<String, String> bupi : bupiList) {
			String bizCode = MapUtils.getString(bupi, "BIZ_CODE", "");
			String userId = MapUtils.getString(bupi, "USER_ID", "");
			String id = StringUtils.join(new String[] {
				bizCode,
				userId
			});
			
			bupiObj.put(id, bupi);
		}
		
		List<Map<String, String>> insertList = new ArrayList<>();
		List<Map<String, String>> updateList = new ArrayList<>();
		for(Map<String, String> user : userMap) {
			String companyCode = MapUtils.getString(user, "COMPANY_CODE", "");
			String userId = MapUtils.getString(user, "USER_ID", "");
			String id = StringUtils.join(new String[] {
				companyCode,
				userId
			});
			
			if(bupiObj.containsKey(id)) {
				updateList.add(user);
			} else {
				insertList.add(user);
				bupiObj.put(id, user);
			}
		}
		
		if(insertList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsert_SBUPI");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(true);
			ssm.doInsert(insertList, true);
		}

		if(updateList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doUpdate_SBUPI");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(true);
			ssm.doUpdate(updateList, true);
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
}
