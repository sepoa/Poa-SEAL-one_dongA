package sepoa.svc.batch;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaFormater;

@Description("계약정보 미입력 알림 배치")
public class SepoaContractConfirmWriteBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaContractConfirmWriteBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaContractConfirmWriteBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("계약정보입력 알림 처리")
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
				if(ctx != null) {
					ctx.getConnection().close();
				}
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
		String house_code = "012"; // 하우스코드
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();

			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : SepoaContractConfirmWriteBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaContractConfirmWriteBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaContractConfirmWriteBatch";
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
	public List<Map<String, String>> getTargetRecord(ConnectionContext var1) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = var1;
		
		sxp = new SepoaXmlParser(this, "doSelectSIAD");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		List<Map<String, String>> targetList = new SepoaFormater(ssm.doSelect(new HashMap<String ,String>())).getListMap(); 
		
		return targetList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = var1;
		Configuration conf = new Configuration();
		SepoaFormater sf = null;
		
		Map<String, String> mailData = new HashMap<String, String>();
		SepoaPushManager mailpush = new SepoaPushManager(info, conf);
    	for(Map<String, String> target : var2) {
    		try {
    			SepoaMessage message = new SepoaMessage();
    			mailData.put("SUBJECT", "[동아제약] "+target.get("SUBJECT")+"건 계약정보 입력필요");
    			mailData.put("TITLE", "계약정보 미입력 알림");
    			mailData.put("CONT_SUBJECT", target.get("SUBJECT"));
    			mailData.put("NOTICE_COMMENT", "계약정보생성이 입력되지 않았습니다. 계약정보를 입력바랍니다.");
		    	mailData.put("CREATOR_NAME", target.get("ADD_USER_NAME"));
		    	mailData.put("LABEL_1", "요청번호");
		    	mailData.put("CONTENTS_1", target.get("SMR_NUMBER"));
		    	
		    	message.addParams(mailData);
				message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
				message.addReceiver(target.get("ADD_USER_ID"), target.get("ADD_USER_NAME"), target.get("EMAIL"), null);

				// 작성자 부서의 팀장에게도 알림 발송 (260113)
				sxp = new SepoaXmlParser(this, "doQueryDeptManager");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(target));
				
				if(sf != null && sf.getListMap().size() > 0) {
					for(Map<String, String> deptManager : sf.getListMap()) {
						if(MapUtils.getString(deptManager,"RESPONSIBILITY_NAME","").equals("팀장")) {
							String managerId = deptManager.get("MANAGER_ID");
							String managerName = deptManager.get("MANAGER_NAME");
							String managerEmail = deptManager.get("MANAGER_EMAIL");
							message.addReceiver(managerId, managerName, managerEmail, null);
						}
					}
				}
				
		    	mailpush.addMessage(message);
		    	mailpush.accept();
    		} catch (Exception e) {
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-SepoaContractConfirmWriteBatch "
						+ ": " + e;
				System.out.println(log);
				Logger.info.println(log);
				Logger.info.println(e.getMessage());
				continue;
			}
		}
    	Logger.info.println("### 계약정보생성 알림 발송완료 ###");
    	Logger.info.println("TOTAL_COUNT: " + var2.size());    	
	}
}
