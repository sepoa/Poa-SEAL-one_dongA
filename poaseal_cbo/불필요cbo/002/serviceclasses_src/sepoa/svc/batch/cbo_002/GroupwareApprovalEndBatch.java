package sepoa.svc.batch.cbo_002;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;

@Description("그룹웨어 결재완료건 배치 서비스")
public class GroupwareApprovalEndBatch extends SepoaBatchService {
	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public GroupwareApprovalEndBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public GroupwareApprovalEndBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("그룹웨어 결재완료건 배치 서비스")
	public SepoaOut executeWeb(Map<String, Object> allData) throws SepoaServiceException {
		return execute();
	}

	// dbcp 초기화(멀티커넥트 지원 sepoa.properties에 기술되어 있어야함)
	public static void initialize() {
		DBCPSetup.initDBCP();
	}

	// 실행(타켓 취득 및 비즈니스로직 수행)
	@Override
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
			System.out.println("GroupwareApprovalEndBatch process is already running.");
			Logger.info.println("GroupwareApprovalEndBatch process is already running.");
			return; // 현재 처리중
		}
		isRunning = true;
		String house_code = "002"; // 하우스코드
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");
		
		try {

			sepoainfo.setThreadLocal();					
			
			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : GroupwareApprovalEndBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new GroupwareApprovalEndBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : GroupwareApprovalEndBatch";
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
	public List<Map<String, String>> getTargetRecord(ConnectionContext ctx) throws Exception {
		return null;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		ConnectionContext ctx = var1;
		
		//String domain = CommonUtil.getConfig("sepoa.system.domain.name");
		String domain = "http://localhost";
		String port = CommonUtil.getConfig("sepoa.server.port", null);
		if (port != null) domain = domain + ":" + port;
		domain = domain + "/";
		String context = CommonUtil.getConfig("sepoa.system.context.name", null);
		String domainContext = domain+(context == null ? "" : context+"/");
		String jspPath = "batch/GroupwareApprovalEndBatch.jsp";
		URL url = new URL(domainContext + jspPath);
        Logger.debug.println(domainContext + jspPath);
        URLConnection urlConn = url.openConnection();

        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        DataOutputStream out = null;
        try {
            out = new DataOutputStream(urlConn.getOutputStream());
            out.writeBytes("");
            out.flush();
        } finally {
            if (out != null) out.close();
        }

        InputStream is = urlConn.getInputStream();
        Scanner scan = new Scanner(is);

        int line = 1;
        while (scan.hasNext()) {
            String str = scan.nextLine();
            Logger.debug.println((line++) + "결재완료 서비스 호출 : " + str);
        }
        scan.close();
		
		Commit();
	}
}
