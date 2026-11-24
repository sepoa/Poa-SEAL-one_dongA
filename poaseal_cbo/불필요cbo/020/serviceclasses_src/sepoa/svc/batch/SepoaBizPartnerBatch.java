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
import sepoa.fw.srv.Base64;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaFormater;

@Description("[남부발전]사용자정보 배치 서비스")
public class SepoaBizPartnerBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaBizPartnerBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaBizPartnerBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
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
			
			getEAIData(ctx);
			
			List<Map<String, String>> dataList = getTargetRecord(ctx);

			if (dataList != null && dataList.size() != 0) {
				setDeleteSBUPR(ctx, dataList);
				setTargetRecord(ctx, dataList);
			} else {
				throw new Exception("EAI Table is Empity!");
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
		String house_code = "020";
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
			sxp = new SepoaXmlParser(this, "doSelectImbupr");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect());

			if (sf.getRowCount() > 0) {
				userInfoList = sf.getListMap();
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
				sxp = new SepoaXmlParser(this, "checkExistSBUPR");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(userData));
				
				if (sf.getRowCount() > 0) {
					// 기존 데이터가 있으면 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSBUPR");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(userData);
				} else {
					// 기존 데이터가 없으면 INSERT
					sxp = new SepoaXmlParser(this, "doInsertSBUPR");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(userData);
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
	}
	
	public void getEAIData(ConnectionContext var1) throws Exception {
		// EAI 테이블 데이터를 임시테이블(BAT_UP_IMBUPR)로 Insert
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = var1;
		
		try {
			// 오늘자 BAT_UP_IMBUPR 데이터 추가
			sxp = new SepoaXmlParser(this, "doInsertImbupr");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate();
			
			Commit();
			
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			Logger.info.print("[ERROR] Failed insert into BAT_UP_IMBUPR from EAI");
		}
	}
	
	public void setDeleteSBUPR(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		// 전자인장 시스템 목록과 EAI 목록을 비교해서 삭제 처리
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		List<Map<String, String>> userList = new ArrayList<Map<String, String>>();
		
		try {
			sxp = new SepoaXmlParser(this, "doSelectSBUPR");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect());
			
			if (sf.getRowCount() > 0) {
				userList = sf.getListMap();
				
				boolean del_flag = true;
				
				for(Map<String, String> suser : userList) {
					for(Map<String, String> tmpuser : var2) {
						if(tmpuser.containsKey("BIZ_CODE") && tmpuser.get("BIZ_CODE").equals(suser.get("BIZ_CODE"))) {
							del_flag = false;
							break;
						}
					}
					
					if(del_flag) {
						sxp = new SepoaXmlParser(this, "doDeleteSBUPR");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(suser);
						Logger.info.print("[" + suser.get("BIZ_CODE") +"]" + suser.get("SELLER_NAME_LOC") + " Delete");
					}
					del_flag = true;
				}
			}
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			Logger.info.print("[ERROR] SBUPR TARGET DELETE FAILED");
		}
		
	}
}
