package sepoa.svc.batch;

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

@Description("거래처 배치 서비스")
public class SepoaVendorBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaVendorBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaVendorBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("거래처 배치 서비스")
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
				throw new Exception("VendorList is Empity!");
			}

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
		String house_code = "013";
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();

			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : SepoaVendorBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new SepoaVendorBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaVendorBatch";
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

		List<Map<String, String>> vendorInfoList = new ArrayList<Map<String, String>>();

		try {
			sxp = new SepoaXmlParser(this, "doSelectVendorInfo");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));

			if (sf.getRowCount() > 0) {
				vendorInfoList = sf.getListMap();
			} else {
				throw new Exception("Get TargetRecord Fail!!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.info.print(e.getMessage());
		}

		return vendorInfoList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		for (Map<String, String> vendorData : var2) {
			try {
				sxp = new SepoaXmlParser(this, "checkExistVendor");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(vendorData));
				
				if (sf.getRowCount() > 0) {
					try {
						sxp = new SepoaXmlParser(this, "doUpdateSBUPR");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(vendorData);
						
						sxp = new SepoaXmlParser(this, "doUpdateSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(vendorData);
						
						sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
						ssm.setLogEnable(false);
						ssm.doUpdate(vendorData);
						
					} catch (Exception e) {
						Logger.err.println("BIZ_CODE : [ " + vendorData.get("BIZ_CODE") + " ] UPDATE FAIL!!");
						Logger.err.println("COMPANY_NAME_LOC : [ " + vendorData.get("COMPANY_NAME_LOC") + " ] UPDATE FAIL!!");
					}
				} else {
					try {
						sxp = new SepoaXmlParser(this, "doInsertSBUPR");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
						ssm.setLogEnable(false);
						ssm.doInsert(vendorData);
						
						vendorData.put("SEQ", "0");
						vendorData.put("USER_ID", vendorData.get("BIZ_CODE"));
						sxp = new SepoaXmlParser(this, "doInsertSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
						ssm.setLogEnable(false);
						ssm.doInsert(vendorData);
						
						vendorData.put("PASSWORD", SepoaAuthEncryptManager.getEncrypted(vendorData.get("IRS_NO").replaceAll("[^\\d]", "")));
						sxp = new SepoaXmlParser(this, "doInsertSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
						ssm.setLogEnable(false);
						ssm.doInsert(vendorData);
					} catch (Exception e) {
						Logger.err.println("BIZ_CODE : [ " + vendorData.get("BIZ_CODE") + " ] INSERT FAIL!!");
						Logger.err.println("COMPANY_NAME_LOC : [ " + vendorData.get("COMPANY_NAME_LOC") + " ] INSERT FAIL!!");
					}
				}
				
				Commit();
			} catch (Exception e) {
				Rollback();
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] ERROR-SepoaVendorBatch : " + e;
				System.out.println(log);
				Logger.info.println(log);
				Logger.info.println(e.getMessage());
				continue;
			}
		}

	}
}
