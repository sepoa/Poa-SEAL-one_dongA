package sepoa.svc.batch;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.bxi.iams.client.config.IamsConfig;
import com.bxi.iams.enums.Action;
import com.bxi.iams.enums.ApprMethod;
import com.bxi.iams.enums.ApprType;
import com.bxi.iams.xml.build.ApprovalBuilder;
import com.bxi.iams.xml.element.Approval;

import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.fw.batch.SepoaBatchService;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBCPSetup;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.drm.fasoo2.Fasoo2DRM;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaFormater;

@Description("부서정보 배치 서비스")
public class SepoaIamsBatch extends SepoaBatchService {

	private static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public SepoaIamsBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public SepoaIamsBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
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
		try {
			Config config = new Configuration();
			String iamsProperties = config.get("iams.properties.file");
			InputStream is = FileUtils.openInputStream(new File(iamsProperties));
			IamsConfig.load(is);
			IOUtils.closeQuietly(is);
		} catch (Exception e) {
			throw new RuntimeException("Iams Properties loding failed.", e);
		}
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
					+ "] START : SepoaIamsBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaIamsBatch.initialize();
			SepoaBatchService.run(new SepoaIamsBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : SepoaIamsBatch";
			System.out.println(endLog);
			Logger.info.println(endLog);
		} catch (Exception e) {
			// e.printStackTrace();
		} finally {
			isRunning = false;
			sepoainfo.unsetThreadLocal();
		}
	}

	@Override
	public List<Map<String, String>> getTargetRecord(ConnectionContext ctx) throws Exception {
		List<Map<String, String>> listMap = null;
		setFlag(true);
		setStatus(1);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			ctx = getConnectionContext();
			sxp = new SepoaXmlParser(this, "doQuery");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

			SepoaFormater sfList = new SepoaFormater(ssm.doSelect(new HashMap<String, String>()));
			listMap = sfList.getListMap();
		} catch (Exception exception) {
			Rollback();
			// exception.printStackTrace();
			setFlag(false);
			setStatus(0);
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ")
					.append(exception.getMessage()).toString());
		}

		return listMap;
	}

	@Override
	public void setTargetRecord(ConnectionContext ctx, List<Map<String, String>> listMap) throws Exception {

		final String LEGACY_JOB_ID = "SE01";
		final String FORM_ID = "SE01";
		final String apprIp = InetAddress.getLocalHost().getHostAddress();

		setFlag(true);
		setStatus(1);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			Config conf = new Configuration();
			boolean devFlag = conf.getBoolean("sepoa.server.development.flag", true);
			ctx = getConnectionContext();
			for (Map<String, String> map : listMap) {

				String title = map.get("APP_SUBJECT");
				String empCd = null;
				if (devFlag) {
					empCd = "02z90305";
				} else {
					empCd = map.get("REQ_USER_ID");
				}

				ApprovalBuilder ab = new ApprovalBuilder(Action.SUBMIT, title, LEGACY_JOB_ID, FORM_ID,
						ApprMethod.INTERFACE, empCd, apprIp);

				System.out.println(map);
				// 결재선
				System.out.println("====AppLine ");
				sxp = new SepoaXmlParser(this, "getAppLine");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfAppLine = new SepoaFormater(ssm.doSelect(map));

				for (int i = 0; i < sfAppLine.getRowCount(); i++) {
					String appMethod = sfAppLine.getValue("APP_METHOD", i);
					String appUserId = null;
					ApprType appType = null;
					if ("04".equals(appMethod)) {
						// 결재
						appType = ApprType.APPROVAL;
					} else if ("06".equals(appMethod)) {
						// 협조
						appType = ApprType.COOPERATION;
					} else if ("07".equals(appMethod)) {
						// 검토
						appType = ApprType.EXAMINATION;
					}

					if (devFlag) {
						switch (i % 3) {
						case 0:
							appUserId = "212730";
							break;
						case 1:
							appUserId = "221682";
							break;
						case 2:
							appUserId = "221746";
							break;
						default:
							break;
						}
					} else {
						appUserId = sfAppLine.getValue("APP_USER_ID", i);
					}
					ab.addApprovalLine(ab.createApprovalLine(i + 1, 1, appType, appUserId));
				}

				// 헤더내용
				sxp = new SepoaXmlParser(this, "doSelectSSIAD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfHeader = new SepoaFormater(ssm.doSelect(map));

				List<Map<String, String>> headers = sfHeader.getListMap();
				ab.addDataset("SM_HEADER", headers);

				sxp = new SepoaXmlParser(this, "doSelectSSIPD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfTarget = new SepoaFormater(ssm.doSelect(map));

				List<Map<String, String>> targets = sfTarget.getListMap();
				ab.addDataset("SM_DOC_DETAIL", targets);

				sxp = new SepoaXmlParser(this, "doSelectSSRAD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfAdditional = new SepoaFormater(ssm.doSelect(map));

				List<Map<String, String>> additionals = sfAdditional.getListMap();
				ab.addDataset("SM_ADDITIONAL_DOC_DETAIL", additionals);

				// Attachements
				sxp = new SepoaXmlParser(this, "doSelectSFMFI");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sfAttach = new SepoaFormater(ssm.doSelect(map));

				List<File> fileList = new ArrayList<File>();
				for (int i = 0; i < sfAttach.getRowCount(); i++) {
					String fileNameClient = sfAttach.getValue("FILE_NAME_CLIENT", i);
					String fileNameServer = sfAttach.getValue("FILE_NAME_SERVER", i);

					File file = null;

					String ext = FilenameUtils.getExtension(fileNameClient);
					if ("pdf".equalsIgnoreCase(ext)) {
						File orgFile = new File(conf.get("sepoa.attach.download"),
								sfAttach.getValue("LAYER", i) + fileNameServer);
						file = new File(conf.get("sepoa.attach.download"),
								sfAttach.getValue("LAYER", i) + fileNameServer + ".pdf");
						FileUtils.copyFile(orgFile, file);
						CipherManager.decrypt(file, true);
						fileList.add(file);
					} else {
						file = new File(conf.get("sepoa.attach.download"),
								sfAttach.getValue("LAYER", i) + fileNameServer + "." + ext);
					}

					Fasoo2DRM drm = new Fasoo2DRM();
					drm.setSession(info);
					drm.setConfig(conf);

					File destFile = new File(FileUtils.getTempDirectory() + "/package_" + file.getName());

					Logger.info.println("Fasoo2 > Set Original File : " + file);
					drm.setOrgFile(file);
					Logger.info.println("DRMPlugin > Set Dest File : " + destFile);
					drm.setDestFile(destFile);

					drm.doPackaging();
					fileList.add(destFile);

					ab.addAttachFile(destFile, fileNameClient);
				}

				// 6. 결재 데이터 결재서버로 전송
				Approval r = ab.send(ab.getApproval());
				String iamsDocNo = r.getHeader().getDocumentNo();

				if (StringUtils.isNotEmpty(iamsDocNo)) {
					map.put("IAMS_DOC_NO", iamsDocNo);
					sxp = new SepoaXmlParser(this, "updateSGWGL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(map);

					Commit();
				}

				for (File file : fileList) {
					FileUtils.forceDelete(file);
				}
			}
		} catch (Exception exception) {
			Rollback();
			// exception.printStackTrace();
			setFlag(false);
			setStatus(0);
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(this.getClass().getSimpleName()).append(" ERROR : ")
					.append(exception.getMessage()).toString());
		}

	}
}
