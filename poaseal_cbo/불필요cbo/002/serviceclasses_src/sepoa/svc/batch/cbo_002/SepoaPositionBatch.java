package sepoa.svc.batch.cbo_002;

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

import org.apache.axiom.om.util.CommonUtils;

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

@Description("직위정보 배치 서비스")
public class SepoaPositionBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaPositionBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaPositionBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("직위정보 배치 서비스")
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
			Map<String, Object> dataList = getTargetRecord(ctx, null);
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
			System.out.println("The SepoaPositionBatch process is already running.");
			Logger.info.println("The SepoaPositionBatch process is already running.");
			return; // 현재 처리중
		}
		isRunning = true;
		String house_code = "002"; // 하우스코드
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();			
			
			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : SepoaPositionBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaPositionBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaPositionBatch";
			System.out.println(endLog);
			Logger.info.println(endLog);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			isRunning = false;
			sepoainfo.unsetThreadLocal();
		}
	}

	public Map<String, Object> getTargetRecord(ConnectionContext var1, String s) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		HashMap<String, Object> dataMap = new HashMap<>();
		
		// 직위정보 group by 
		sxp = new SepoaXmlParser(this, "doQueryPositionData");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
		sxp.addVar("GW_DATABASE", CommonUtil.getConfig("dlcon.gw.database.name"));
		sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
		dataMap.put("POSITION_DATA", sf.getListMap());

		return dataMap;  
	}

	public void setTargetRecord(ConnectionContext var1, Map<String, Object> dataMap) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		// 직위정보 먼저 scode에 ux049에 넣기
		List<Map<String, String>> positionMap = (List<Map<String, String>>) dataMap.get("POSITION_DATA");
		
		try {
			sxp = new SepoaXmlParser(this, "DEL_FLAG_Y");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(new HashMap<>());
			for (Map<String, String> position : positionMap) {
				// UX049에 Code가 있는지 먼저 조회
				sxp = new SepoaXmlParser(this, "checkExistPositionCode");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(position));
				try {
					if (sf.getRowCount() > 0) {
						// 있으면 업데이트 - KO
						position.put("GRADENAME", position.get("GRADENAME"));
						position.put("LANGUAGE", "KO");
						
						sxp = new SepoaXmlParser(this, "doUpdatePositionCode");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(position);
						
						// 있으면 업데이트 - EN
						position.put("GRADENAME", position.get("GRADENAMEEN"));
						position.put("LANGUAGE", "EN");					
						
						sxp = new SepoaXmlParser(this, "doUpdatePositionCode");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(position);
					} else {
						// 없으면 넣기 - KO
						position.put("GRADENAME", position.get("GRADENAME"));
						position.put("LANGUAGE", "KO");					
						
						sxp = new SepoaXmlParser(this, "doInsertPositionCode");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doInsert(position);
						
						// 없으면 넣기 - EN
						position.put("GRADENAME", position.get("GRADENAMEEN"));
						position.put("LANGUAGE", "EN");
						
						sxp = new SepoaXmlParser(this, "doInsertPositionCode");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.setLogEnable(false);
						ssm.doInsert(position);	
					}
					Commit();
				} catch (Exception e) {
					// 로깅
					Rollback();
					String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
							+ "] ERROR-SepoaPositionBatch-Position_1 : " + e;
					System.out.println(log);
					Logger.info.println(log);
					continue;
				}
			}
		} catch (Exception e) {
			// 로깅
			String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] ERROR-SepoaPositionBatch-Position_2 : " + e;
			System.out.println(log);
			Logger.info.println(log);
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
