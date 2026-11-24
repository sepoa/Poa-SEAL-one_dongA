package sepoa.svc.batch;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
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
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaFormater;

@Description("외부 전자결재 배치 서비스")
public class HhiOfficeLogBatch extends SepoaBatchService {

	static boolean isRunning = false; // 처리중인데 실행되는 오류를 막기 위한 변수

	// crontab 등록 후 실행시
	public HhiOfficeLogBatch() throws SepoaServiceException, ConfigurationException {
		super();
		setVersion("1.0.0");
	}

	// 웹에서 실행시
	public HhiOfficeLogBatch(String opt, SepoaInfo info) throws SepoaServiceException, ConfigurationException {
		super(opt, info);
		setVersion("1.0.0");
	}

	// 웹에서 실행시 호출
	@Description("외부 전자결재 배치 서비스")
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

			if (dataList != null) {
				if(dataList.size() > 0) {
					setTargetRecord(ctx, dataList);
				}
			} else {
				throw new Exception("Target is Null!");
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
		String house_code = "019";
		SepoaInfo sepoainfo = new SepoaInfo(house_code,
				"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");

		try {
			sepoainfo.setThreadLocal();

			String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] START : HhiOfficeLogBatch";
			System.out.println(startLog);
			Logger.info.println(startLog);
			SepoaBatchService.initialize();
			SepoaBatchService.run(new HhiOfficeLogBatch("TRANSACTION", sepoainfo));

			String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
					+ "] END : HhiOfficeLogBatch";
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

		List<Map<String, String>> OfficeLogList = new ArrayList<Map<String, String>>();

		try {
			sxp = new SepoaXmlParser(this, "doSelectOfficeLog");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));

			if (sf.getRowCount() > 0) {
				OfficeLogList = sf.getListMap();
			} else {
				throw new Exception("Get TargetRecord Empity!!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			Logger.info.print("Get TargetRecord Fail!!");
		}

		return OfficeLogList;
	}

	@Override
	public void setTargetRecord(ConnectionContext var1, List<Map<String, String>> var2) throws Exception {
		ConnectionContext ctx = var1;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;

		for (Map<String, String> officeLog : var2) {
			try {
				
				String sysKey = officeLog.get("A_SYSKEY");
				String smrNumber = "";
				String appNo = "";
				Map<String, String> params = new HashMap<>();
				
				if(sysKey != null && !sysKey.equals("")) {
					// SYSKEY 값 있음
					String[] arr = sysKey.split("#");
					smrNumber = arr[0];
					appNo = arr[1];
				} else {
					// 오류
					throw new Exception("============ [ERROR] No SYSKEY ============");
				}
				
				params.put("SMR_NUMBER", smrNumber);
				params.put("APP_NO", appNo);
				
				sxp = new SepoaXmlParser(this, "checkExistSGWGL");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				sf = new SepoaFormater(ssm.doSelect(params));
				
				if (sf.getRowCount() > 0) {
					//결재 메소드 행위 파싱
					String aprCod = officeLog.get("APRCOD");
					
					switch(aprCod.toUpperCase()) {
						case "B": //결재자가 승인했을 때
							//결재선 승인 상태값 업데이트
							params.put("APP_STATUS", "L02");
							sxp = new SepoaXmlParser(this, "doConfirmApprovalLine");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							ssm.doUpdate(officeLog, params);
							break;
						case "Y": //모든 결재자의 승인이 완료 되었을 때
							sxp = new SepoaXmlParser(this, "doSelectSSIAD");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							sf = new SepoaFormater(ssm.doSelect(params));
							
							if(sf.getRowCount() > 0) {
								params.put("APP_STATUS", "L02");
								sxp = new SepoaXmlParser(this, "doConfirmApprovalLine");
								ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
								ssm.setLogEnable(false);
								ssm.doUpdate(officeLog, params);
								
								Map<String, String> reqSealData = sf.getListMap().get(0);
								reqSealData.put("REJECT_REASON", officeLog.get("APRNOTE"));
								reqSealData.put("CONFIRM_USER_ID", officeLog.get("AEMPNO"));
								
								// 승인 처리
								Map<String, String> apppObj = new HashMap<String, String>();
								String applyForStr = "";
								 if("D".equalsIgnoreCase(reqSealData.get("APPLY_TYPE"))) {
									 applyForStr = "RD";
	                            } else {
	                            	applyForStr = ("C".equalsIgnoreCase(reqSealData.get("APPLY_TYPE")) ? "CS" : "US");
	                            }
								apppObj.putAll(reqSealData);
								apppObj.put("SMR_NUMBER", params.get("SMR_NUMBER"));
								apppObj.put("APP_NO", params.get("APP_NO"));
								apppObj.put("RECENT", "Y");
								apppObj.put("APP_STATUS", "CR");
								apppObj.put("APPLY_FOR", applyForStr);
								apppObj.put("APP_COMMENT", "EMPTY");
								apppObj.put("POSTPROCESSED", "N");
								apppObj.put("APP_NO_SEQ", "1");
								apppObj.put("APP_LINE_SEQ", "1");
								apppObj.put("RE_FLAG", MapUtils.getString(reqSealData, "RE_FLAG", ""));
								
								//SPMGT 테이블 INSERT
	                            Map<String, String> pmgtMap = new HashMap<String, String>();
	                            String pmgtAction = "";
	                            if("D".equalsIgnoreCase(reqSealData.get("APPLY_TYPE"))) {
	                            	pmgtAction = "CERT_COMPLETE";
	                            } else {
	                            	pmgtAction = "SEAL_COMPLETE";
	                            }
	                            
	                            pmgtMap.put("ACTION", pmgtAction);
	                            pmgtMap.put("IP", CommonUtil.getConfig("sepoa.hd.host.ip", "10.100.45.132"));
	                            
	                            sxp = new SepoaXmlParser(this, "doDeleteSPMGT");
	                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                            ssm.setSepoaInfo(info);
	                            ssm.doUpdate(reqSealData, pmgtMap);
	                            
	                            
	                            sxp = new SepoaXmlParser(this, "doInsertSPMGT");
	                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                            ssm.setSepoaInfo(info);
	                            ssm.doUpdate(reqSealData, pmgtMap);
	                            
	                            // SGWGL status F로 변경
	                            Map<String, String> gwglObj = new HashMap<String, String>();
								gwglObj.put("APP_STATUS", "F");
								
								sxp = new SepoaXmlParser(this, "doConfirmApprovalHeader");
	                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                            ssm.setSepoaInfo(info);
	                            ssm.doUpdate(reqSealData, gwglObj);
								
								// SSIAD CR상태로 업데이트
								sxp = new SepoaXmlParser(this, "doUpdateSSIAD");
								ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
								ssm.setSepoaInfo(info);
								ssm.doUpdate(reqSealData, new SingletonMap("SMR_STATUS", "CR"));

								// 후처리 큐 테이블에 데이터 INSERT
								sxp = new SepoaXmlParser(this, "doInsertSAPPP");
								ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
								ssm.setSepoaInfo(info);
								ssm.doInsert(apppObj);
							} else {
								throw new Exception("Not Found Req Seal Data!");
							}
							
							break;
						case "Z": //결재자가 반려했을 때
							//결재선에 반려 상태값 업데이트
							params.put("APP_STATUS", "L03");
							sxp = new SepoaXmlParser(this, "doRejectApprovalLine");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							ssm.doUpdate(officeLog, params);
							
							//날인요청 문서 반려 상태로 업데이트
							params.put("DOC_STATUS", "R");
							sxp = new SepoaXmlParser(this, "doRejectDocument");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							ssm.doUpdate(officeLog, params);
							break;
						case "C": //결재자가 회수했을 때
							//날인요청 문서 회수 상태로 업데이트
							params.put("DOC_STATUS", "T");
							sxp = new SepoaXmlParser(this, "doWithDrawDocument");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.setLogEnable(false);
							ssm.doUpdate(officeLog, params);
							break;
						default:
							throw new Exception("Not Found APRCOD Type!");
					} 
					
				} else {
					throw new Exception("============ [ERROR] No SGWGL Data Found ============");
				}
				
				// IF_FLAG = Y 업데이트
				officeLog.put("IF_RESULT", "Y");
			} catch (Exception e) {
				Rollback();
				String log = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date()) + "] ERROR-HhiOfficeLogBatch : " + e;
				Logger.err.println(this, log);
				
				// IF_FLAG = F 업데이트
				officeLog.put("IF_RESULT", "F");
			} finally {
				sxp = new SepoaXmlParser(this, "doUpdateOfficeLog");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(false);
				ssm.doUpdate(officeLog);
				Commit();
			}
		}

	}
}
