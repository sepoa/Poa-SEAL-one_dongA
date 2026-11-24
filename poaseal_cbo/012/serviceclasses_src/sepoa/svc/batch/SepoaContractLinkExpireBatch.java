package sepoa.svc.batch;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
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
import sepoa.fw.util.SepoaFormater;

@Description("전자계약 서명자 링크 만료 배치")
public class SepoaContractLinkExpireBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaContractLinkExpireBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaContractLinkExpireBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("링크 만료 처리 서비스")
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
					+ "] START : SepoaContractLinkExpireBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaContractLinkExpireBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaContractLinkExpireBatch";
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
		Config config = new Configuration();
		
		int linkExpireDay = config.getInt("sepoa.contract.link.expire.day", 3);
		
		sxp = new SepoaXmlParser(this, "doSelectSSIGN");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		ssm.setLogEnable(false);
		List<Map<String, String>> signerList = new SepoaFormater(ssm.doSelect(new HashMap<String ,String>())).getListMap();
		List<String> linkList = signerList.stream().map(data -> data.get("LINK")).collect(Collectors.toList());
		
		Map<String, String> condition = new HashMap<>();
		condition.put("PRIVATE_CODE", StringUtils.join(linkList, ","));
		
		sxp = new SepoaXmlParser(this, "doSelectULNUS");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		ssm.setLogEnable(false);
		List<Map<String, String>> linkInfoList = new SepoaFormater(ssm.doSelect(condition)).getListMap();
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Date current = new Date();
		List<Map<String, String>> targetList = linkInfoList.stream().filter(e -> {
			boolean result = false;
			try {
				Date target = sdf.parse(e.get("ADD_DATE") + e.get("ADD_TIME"));
				long timeDiff = current.getTime() - target.getTime();
				long dayDiff = timeDiff / (24 * 60 * 60 * 1000);
				
				if(dayDiff > linkExpireDay) {
					result = true;
				}
			} catch (ParseException ex) {
				ex.printStackTrace();
			}
			
			return result;
		}).collect(Collectors.toList());
		
		targetList = targetList.stream().map(e -> {
			Optional<Map<String, String>> signerInfo = signerList.parallelStream().filter(signer -> e.get("PRIVATE_CODE").equals(signer.get("LINK"))).findFirst();
			if(signerInfo.isPresent()) {
				Map<String, String> i = signerInfo.get();
				e.put("CONT_NO", i.get("CONT_NO"));
				e.put("CONT_GL_SEQ", i.get("CONT_GL_SEQ"));
				e.put("SIGNER_NO", i.get("SIGNER_NO"));
			}
			
			return e;
		}).collect(Collectors.toList());
		
		return targetList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = var1;
		
		Map<String, String> condition = new HashMap<>();
		condition.put("LINK", "");
		
		for(Map<String, String> target : var2) {
			sxp = new SepoaXmlParser(this, "doDeleteULNUS");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doDelete(target);
			
			sxp = new SepoaXmlParser(this, "doUpdateSSIGN_LINK");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doDelete(target, condition);
		}
	}
}
