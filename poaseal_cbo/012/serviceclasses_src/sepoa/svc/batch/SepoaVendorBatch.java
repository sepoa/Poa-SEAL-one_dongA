package sepoa.svc.batch;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.SingletonMap;

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
import sepoa.fw.util.SepoaFormater;

@Description("동아제약 거래처 배치 서비스")
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

			List<Map<String, String>> dataList = getTargetRecord(null);

			if (dataList != null && dataList.size() > 0) {
				setTargetRecord(ctx, dataList);
			} else {
				throw new Exception("Vendor Data is Empty!");
			}

			Commit();
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
		joinTransaction("dongaSSS"); // 매입 거래처
		joinTransaction("dongaERP"); // 매출 거래처
		ConnectionContext sssCtx = getConnectionContext("dongaSSS");
		ConnectionContext erpCtx = getConnectionContext("dongaERP");

		List<Map<String, String>> dongaVendorDataList = new ArrayList<Map<String, String>>();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		sxp = new SepoaXmlParser(this, "doSelectDongaSSS");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, sssCtx, sxp);
		sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
		if (sf.getRowCount() > 0) {
			dongaVendorDataList.addAll(sf.getListMap());
			// 데이터 규격이 다르면 가공 필요함
		}

		sxp = new SepoaXmlParser(this, "doSelectDongaERP");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, erpCtx, sxp);
		sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
		if (sf.getRowCount() > 0) {
			dongaVendorDataList.addAll(sf.getListMap());
			// 데이터 규격이 다르면 가공 필요함
		}

		return dongaVendorDataList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		// 거래처 리스트 반복문 시작
		for (Map<String, String> vendorData : var2) {
			try {
				// 거래처 코드 변수 할당
				String bizCode = vendorData.get("BIZ_CODE");

				// 등록된 회사가 있는지 검사
				sxp = new SepoaXmlParser(this, "existCompanyCheck");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
				sf = new SepoaFormater(ssm.doSelect(new SingletonMap("BIZ_CODE", bizCode)));

				if (sf.getRowCount() > 0) {
					// 이미 등록된 사용자가 있다면 SBUPR, SBUPI, SUSMT 업데이트
					sxp = new SepoaXmlParser(this, "doUpdateSBUPR");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
					ssm.doUpdate(vendorData);

					sxp = new SepoaXmlParser(this, "doUpdateSBUPI");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
					ssm.doUpdate(vendorData);

					sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
					ssm.doUpdate(vendorData);
				} else {
					vendorData.put("BIZ_CATEGORY", "S");
					// 이미 등록된 사용자가 없다면 SBUPR, SBUPI, SUSMT 생성
					sxp = new SepoaXmlParser(this, "doInsertSBUPR");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
					ssm.doInsert(vendorData);

					vendorData.put("SEQ", "0");
					sxp = new SepoaXmlParser(this, "doInsertSBUPI");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
					ssm.doInsert(vendorData);

					vendorData.put("PASSWORD", SepoaAuthEncryptManager.getEncrypted(vendorData.get("IRS_NO")));
					sxp = new SepoaXmlParser(this, "doInsertSUSMT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, var1, sxp);
					ssm.doInsert(vendorData);
				}
				Commit();
			} catch (Exception e) {
				Rollback();
				Logger.err.println("[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())+ "] An exception occurred while adding the user. userData : " + vendorData, e);
				continue;
			}
		}

	}
}
