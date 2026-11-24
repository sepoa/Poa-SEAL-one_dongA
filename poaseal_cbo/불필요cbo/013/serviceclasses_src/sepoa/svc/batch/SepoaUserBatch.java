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
		ConnectionContext ctx2 = null;
		try {
			ctx = getConnectionContext();
			
			joinTransaction("posco_flow");
			ctx2 = getConnectionContext("posco_flow");

			List<Map<String, String>> dataList = getTargetRecord(ctx2);

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
		String house_code = "013";
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

		// 포스코플로우 도급처 등록
		Map<String, String> companyData = new HashMap<String, String>();
		try {
			// 포스코플로우 도급처 등록체크
			sxp = new SepoaXmlParser(this, "checkSBUPR");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
			
			if (sf.getRowCount() == 0 || sf.getListMap() == null) {
				// SCGML 정보 조회
				sxp = new SepoaXmlParser(this, "doQueryCompany");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
				
				if (sf.getRowCount() == 0 || sf.getListMap() == null) {
					throw new Exception("Company information does not exist!");
				}
				
				companyData.putAll(sf.getListMap().get(0));
				
				sxp = new SepoaXmlParser(this, "doInsertPosco");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				ssm.doInsert(companyData);
			}

			Commit();
		} catch (Exception e) {
			Logger.err.print("Failed to set standard information!");
		}

		// 유저 정보 넣기
		int count = 1;
		for (Map<String, String> userData : var2) {
			try {
				// 뷰 정보에 DEL_FLAG가 Y일경우 아이디가 널로 반환됨
				if("".equals(userData.get("USER_ID"))) {
					throw new Exception("USER_ID is Empity !! " +" USER_NAME: "+userData.get("USER_NAME_LOC") + count++);
				}
				
				userData.put("USER_ID", userData.get("USER_ID").toUpperCase());
				userData.put("COMPANY_NAME_LOC", companyData.get("COMPANY_NAME_LOC"));

				// SUSMT에 ID가 있는지 확인
				sxp = new SepoaXmlParser(this, "checkExistSUSMT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(userData));
				
				// 업데이트 될 때 원래의 프로파일코드 유지하기 위함(인감관리자일 경우가 있기에)
				if (sf.getRowCount() > 0) {
					String orgProfileCode = MapUtils.getString(sf.getListMap().get(0), "MENU_PROFILE_CODE", "");
					if (!"".equals(orgProfileCode)) {
						userData.put("MENU_PROFILE_CODE", orgProfileCode);
					} else {
						// 이 구간은 사실 탈 일없음
						userData.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
					}
				} else {
					userData.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
				}

				if (sf.getRowCount() > 0) {
					try {
						sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(userData);

						sxp = new SepoaXmlParser(this, "doUpdateSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(userData);

					} catch (Exception e) {
						Logger.err.println("USER_ID : [ " + userData.get("USER_ID") + " ] UPDATE FAIL!!");
					}
				} else {
					try {
						userData.put("PASSWORD", SepoaAuthEncryptManager.getEncrypted((userData.get("USER_ID"))));

						sxp = new SepoaXmlParser(this, "doInsertSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doInsert(userData);

						sxp = new SepoaXmlParser(this, "doInsertSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doInsert(userData);

					} catch (Exception e) {
						Logger.err.print(e.getMessage());
						Logger.err.println("USER_ID : [ " + userData.get("USER_ID") + " ] INSERT FAIL!!");
						Logger.err.println("USER_NAME : [ " + userData.get("USER_NAME") + " ] INSERT FAIL!!");
					}
				}

				Commit();
			} catch (Exception e) {
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-SepoaUserBatch : " + e.getMessage();
				System.out.println(log);
				Logger.info.println(log);
				Logger.info.println(e.getMessage());
				continue;
			}
		}

	}
}
