package sepoa.svc.batch.cbo_002;

import java.io.UnsupportedEncodingException;
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

import org.apache.commons.collections.MapUtils;
import org.jsoup.select.Evaluator.IsEmpty;

import com.google.gson.Gson;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cache.Cache;
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
import sepoa.svc.batch.cbo_002.SepoaUserBatch;

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
		ConnectionContext ctx = getConnectionContext();  
		try {
			List<Map<String, String>> dataList = getTargetRecord(ctx, null);
			setTargetRecord(ctx, dataList);

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
			System.out.println("The SepoaUserBatch process is already running.");
			Logger.info.println("The SepoaUserBatch process is already running.");
			return; // 현재 처리중
		}
		isRunning = true;
		String house_code = "002"; // 하우스코드
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

	public List<Map<String, String>> getTargetRecord(ConnectionContext var1, String s) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		// 사원정보 select
		sxp = new SepoaXmlParser(this, "doQueryUserData");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
		ssm.setLogEnable(false);
		sxp.addVar("GW_DATABASE", CommonUtil.getConfig("dlcon.gw.database.name"));

		sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));

		return sf.getListMap();
	}

	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> dataList) throws Exception {
		ConnectionContext ctx = var1;
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
			
			//유저 정보 넣기
			for (Map<String, String> user : dataList) {
				// SUSMT에 ID가 있는지 확인
				sxp = new SepoaXmlParser(this, "checkExistSUSMT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(user));
				try {
					String date_format = CommonUtil.getConfig("sepoa.user.default.date_format");
					
					user.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.default.porfile.code", "MUP21111900000"));
					user.put("USER_TYPE", "S");
					user.put("DATE_FORMAT", date_format);
					
					if("Y".equals(user.get("USAGESTATE"))) {
						user.put("DEL_FLAG", "N");
						
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
							//user.put("PASSWORD", getEncryptedPassword(user.get("PERSONID")));
							user.put("PASSWORD","!!!NOT_USE");
							
							sxp = new SepoaXmlParser(this, "doInsertSUSMT");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							ssm.doInsert(user);
	
							sxp = new SepoaXmlParser(this, "doInsertSADDR");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							ssm.doInsert(user);
	
						}
					}else {
						user.put("DEL_FLAG", "Y");
						sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(user);

						sxp = new SepoaXmlParser(this, "doUpdateSADDR");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(user);
					}
					
					Commit();
				}catch (Exception e) {
					// 로깅
					Rollback();
					String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
							+ "] ERROR-SepoaUserBatch_1 : " + e;
					System.out.println(log);
					Logger.info.println(log);
					continue;
				}
			}
		} catch (Exception e) {
			// 로깅
			Rollback();
			String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] ERROR-SepoaUserBatch_2 : " + e;
			System.out.println(log);
			Logger.info.println(log);
		}
	}

	@Override
	public List<Map<String, String>> getTargetRecord(ConnectionContext var1) throws Exception {
		List<Map<String, String>> dataMapList = new ArrayList<Map<String, String>>();

		return dataMapList;
	}

/*	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {

	}*/

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
