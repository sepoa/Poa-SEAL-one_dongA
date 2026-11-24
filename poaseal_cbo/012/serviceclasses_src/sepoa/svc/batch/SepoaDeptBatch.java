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

import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Base64;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaFormater;

@Description("부서정보 배치 서비스")
public class SepoaDeptBatch extends SepoaBatchService {

	static boolean isRunning = false;

	// cronttab 등록 후 실행시
	public SepoaDeptBatch(String houseCode) throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaDeptBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
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

			List<Map<String, String>> deptMap = (List<Map<String, String>>) dataList.get("DEPT_INFO");
			for (Map<String, String> map : deptMap) {
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
		List<Map<String, String>> deptInfoList = new ArrayList<Map<String, String>>();
		Map<String, String> sampleMap = new HashMap<String, String>();

		// 부서정보조회
		sxp = new SepoaXmlParser(this, "doSelectDeptInfo");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		sf = new SepoaFormater(ssm.doSelect(sampleMap));

		if (sf.getRowCount() > 0) {
			deptInfoList = sf.getListMap();
			dataMap.put("DEPT_INFO", deptInfoList);
		}

		return dataMap;
	}

	public void setTargetRecord(ConnectionContext var1, Map<String, Object> dataMap) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		// 부서 정보넣기
		List<Map<String, String>> deptMap = (List<Map<String, String>>) dataMap.get("DEPT_INFO");
		for (Map<String, String> dept : deptMap) {
			try {
				// SDEPT에 부서가 있는지 확인
				sxp = new SepoaXmlParser(this, "checkExistSDEPT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(dept));

				if (sf.getRowCount() > 0) {
					// 부서가 있으면 SDEPT 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doUpdate(dept);
				} else {
					// 부서가 없으면 SDEPT insert
					sxp = new SepoaXmlParser(this, "doInsertSDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.setLogEnable(false);
					ssm.doInsert(dept);
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

	@Override
	public List<Map<String, String>> getTargetRecord(ConnectionContext var1) throws Exception {
		List<Map<String, String>> dataMapList = new ArrayList<Map<String, String>>();

		return dataMapList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {

	}

}
