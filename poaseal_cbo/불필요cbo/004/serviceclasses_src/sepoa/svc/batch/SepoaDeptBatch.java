package sepoa.svc.batch;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

import filters.SecurityFilter;
import filters.SecurityRequst;
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
import sepoa.fw.util.SepoaFormater;

@Description("부서정보 배치 서비스")
public class SepoaDeptBatch extends SepoaBatchService {

	private static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	final private static String URL = "http://is.poscochemtech.com:8080/RESTful_GetHrInfo/rest/dept/para/1/%25";

	// crontab 등록 후 실행시
	public SepoaDeptBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaDeptBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("부서정보 배치 서비스")
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
		ConnectionContext ctx2 = null;
		try {
			joinTransaction("easysign");
			ctx = getConnectionContext();
			ctx2 = getConnectionContext("easysign");
			List<Map<String, String>> dataList = getTargetRecord(null);

			for(Map<String, String> map : dataList) {
				String parentDept = map.get("pre_deptcd");
				if("!".equals(parentDept)) {
					map.put("pre_deptcd", "");
				}
				map.put("HOUSE_CODE" , "004");
				map.put("COMPANY_CODE" , "1000");
			}
			setTargetRecord(ctx, dataList);

			for(Map<String, String> map : dataList) {
				map.put("HOUSE_CODE" , "000");
				map.put("COMPANY_CODE" , "1000");
			}
			setTargetRecord(ctx2, dataList);

			Commit();
			
			setClear();
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
					+ "] START : SepoaDeptBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaDeptBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaDeptBatch";
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
	@Override
	public List<Map<String, String>> getTargetRecord(ConnectionContext ctx) throws Exception {
		HttpURLConnection httpConn = null;
		InputStream is = null;
		List<Map<String, String>> result = null;
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
				result = (List<Map<String, String>>) map.get("deptInfo");
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

		return result;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		for (Map<String, String> map : var2) {
			try {
				// SDEPT에 부서가 있는지 확인 (DEPTCODE로)
				sxp = new SepoaXmlParser(this, "checkExistSDEPT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(map));

				if (sf.getRowCount() > 0) {
					// 부서가 있으면 SDEPT 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(map);
				} else {
					// 부서가 없으면 SDEPT insert
					sxp = new SepoaXmlParser(this, "doInsertSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(map);
				}

			} catch (Exception e) {
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-SepoaDeptBatch : " + e;
				System.out.println(log);
				Logger.info.println(log);
				continue;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setClear() throws Exception {
		HttpURLConnection httpConn = null;
		InputStream is = null;
		List<Map<String, String>> result = null;
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
				result = (List<Map<String, String>>) map.get("deptInfo");
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
	}
}
