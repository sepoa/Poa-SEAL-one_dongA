package sepoa.svc.batch;

import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;

import com.google.gson.Gson;

import sepoa.fw.batch.SepoaBatchService;
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
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;
import sepoa.svl.common.RestUtil;

@Description("SECL 날인요청 결재 진행현황 확인 배치 서비스")
public class SepoaApprovalBatch extends SepoaBatchService {

	static boolean isRunning = true; // 처리중인데 실행되는 오류를 막기 위한 변수

	static String curDate = SepoaDate.getShortDateString();
	static String curTime = SepoaDate.getShortTimeString();
	static boolean log_enable_flag = false;
	
	public SepoaApprovalBatch(String opt, SepoaInfo sepoainfo) throws SepoaServiceException {
		super(opt, sepoainfo);
		setVersion("1.0.0");
	}

	// dbcp 초기화(멀티커넥트 지원 sepoa.properties에 기술되어 있어야함)
	public static void initialize() {
		DBCPSetup.initDBCP();
	}

	// 웹에서 실행시 호출
	public SepoaOut executeWeb(Object allData) throws SepoaServiceException {
		return execute();
	}
	
	// 타겟데이터 취득 메소드
	public List<Map<String, String>> getTargetRecord(ConnectionContext ctx) throws Exception {
		Logger.info.println(info.getSession("ID"), this, "########  getTargetRecord Method Start SepoaApproval Batch ########");
		SepoaFormater sf =null;
		
		try {		
			SepoaXmlParser sxp = new SepoaXmlParser(this,"doSelectSSIAD");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"),this,ctx,sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
			
			if(sf.getRowCount() == 0) {
				throw new Exception("########  getTargetRecord Method Result Empty! ########");
			}
		}catch(Exception e) {
			Logger.err.println("########  getTargetRecord Method Start SepoaApproval Batch Fail! ########", e);
		}
		
		return sf.getListMap();
	}

	// 타겟데이터 수행 메소드
	public void setTargetRecord(ConnectionContext ctx, List<Map<String, String>> dataList) throws Exception {
		int forkJoinPoolSize = Integer.valueOf(CommonUtil.getConfig("sepoa.approval.batch.pool.size", "3"));
		final String host = CommonUtil.getConfig("sepoa.secl.req.approval.host", "http://openapi.stage.samsung.net");
		ForkJoinPool forkJoinPool = new ForkJoinPool(forkJoinPoolSize);
		forkJoinPool.submit(() -> {
			dataList.parallelStream().forEach(element -> {
				boolean nextFlag = true;
				try {
					SepoaXmlParser sxp = null;
					SepoaSQLManager ssm = null;
					SepoaFormater sf = null;
							
					String apInfId = "";
					String addDateStr = element.get("ADD_DATE");
					String oldStdDate = CommonUtil.getConfig("sepoa.req.seal.approval.oldstd.date", "20230405");

					//4월 5일자 이후로 결재 ID기준이 바뀌어 기존 결재건을 살리기 위해 아래와 같이 처리한다.
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					Date stdDate = sdf.parse(oldStdDate);
					Date addDate = sdf.parse(addDateStr);
					if(addDate.after(stdDate)) {
						//Sepoa 결재 정보 조회
						String appNo = "";
						sxp = new SepoaXmlParser(this, "doSelectApprovalInfo");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
						ssm.setSepoaInfo(info);
						sf = new SepoaFormater(ssm.doSelect(element));
						if(sf.getRowCount() > 0) {
							Map<String, String> approvalInfo = sf.getListMap().get(0);
							appNo = approvalInfo.get("APP_NO");
						} else {
							Logger.warn.println("Approval Info Not Found! [" + element.get("SMR_NUMBER")+ "]");
							nextFlag = false;
						}
						
						apInfId = SepoaString.getRpad(element.get("SMR_NUMBER") + appNo, 32, "0");
					} else {
						apInfId = SepoaString.getRpad(element.get("SMR_NUMBER"), 32, "0");
					}
					
					
					String appStatusUrl = CommonUtil.getConfig("sepoa.secl.req.approval.status.url", "/approval/api/v2.0/approvals/{apInfId}/detail");
					appStatusUrl = appStatusUrl.replace("{apInfId}", apInfId);
					String url = host + appStatusUrl;

					// 건 by 건으로 REST 호출
					String restResult = null;
					RestUtil util = new RestUtil();
					util.setHeaderData(RestUtil.REQ_METHOD, "GET");
					util.setHeaderData("System-ID", CommonUtil.getConfig("sepoa.secl.req.approval.systemID", "KCD80REST00016"));
					util.setHeaderData("Authorization", CommonUtil.getConfig("sepoa.secl.req.approval.auth.header", "Bearer 2c1628c0-8e84-3db6-b274-690e4713293c"));

					Logger.info.println("Req Approval Status URL : " + url);

					try {
						restResult = util.connect(url);
					} catch (Exception e) {
						Logger.err.println("There was a problem checking the approval status.", e);
						nextFlag = false;
					}

					if (util.getResponseCode() != HttpURLConnection.HTTP_OK) {
						Logger.warn.println("Approval Status Result Not 200! [" + element.get("SMR_NUMBER") + "] : " + restResult);
						nextFlag = false;
					} else {
						if (!restResult.trim().isEmpty() && restResult != null) {
							Logger.info.println("Approval Status Result Success!");
						} else {
							Logger.warn.println("Approval Status Result is Empty! [" + element.get("SMR_NUMBER")+ "] : " + restResult);
							nextFlag = false;
						}
					}

					if (nextFlag) {
						Map<String, Object> restResultMap = new Gson().fromJson(restResult, Map.class);
						if ("success".equalsIgnoreCase(String.valueOf(restResultMap.get("result")))) {
							List<String> okStautsList = Arrays.asList(CommonUtil.getConfig("sepoa.secl.approval.ok.status", "2;5;6").split(";"));
							Map<String, String> appStatusData = MapUtils.getMap(restResultMap, "data");
							String appStatus = String.valueOf(appStatusData.get("status"));
							if (okStautsList.contains(appStatus)) {
								if("C".equalsIgnoreCase(element.get("APPLY_TYPE")) || "U".equalsIgnoreCase(element.get("APPLY_TYPE"))) {
									// 승인 처리
									Map<String, String> apppObj = new HashMap<String, String>();
									apppObj.put("SMR_NUMBER", element.get("SMR_NUMBER"));
									apppObj.put("RECENT", "Y");
									apppObj.put("APP_STATUS", "CR");
									apppObj.put("APPLY_FOR",(("C".equalsIgnoreCase(element.get("APPLY_TYPE")) ? "CS" : "US")));
									apppObj.put("APP_COMMENT", "EMPTY");
									apppObj.put("POSTPROCESSED", "N");
									apppObj.put("APP_NO_SEQ", "1");
									apppObj.put("APP_LINE_SEQ", "1");
									apppObj.put("RE_FLAG", MapUtils.getString(element, "RE_FLAG", "E"));
									
									//SPMGT 테이블 INSERT
	                                Map<String, String> pmgtMap = new HashMap<String, String>();
	                                pmgtMap.put("ACTION", "SEAL_COMPLETE");
	                                pmgtMap.put("IP", CommonUtil.getConfig("sepoa.secl.host.ip", "66.86.2.191"));
	                                
	                                sxp = new SepoaXmlParser(this, "doDeleteSPMGT");
	                                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                                ssm.setSepoaInfo(info);
	                                ssm.doUpdate(element, pmgtMap);
	                                
	                                sxp = new SepoaXmlParser(this, "doInsertSPMGT");
	                                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                                ssm.setSepoaInfo(info);
	                                ssm.doUpdate(element, pmgtMap);
									
									// SSIAD CR상태로 업데이트
									sxp = new SepoaXmlParser(this, "doUpdateSSIAD");
									ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
									ssm.setSepoaInfo(info);
									ssm.doUpdate(element, new SingletonMap("SMR_STATUS", "CR"));

									// 후처리 큐 테이블에 데이터 INSERT
									sxp = new SepoaXmlParser(this, "doInsertSAPPP");
									ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
									ssm.setSepoaInfo(info);
									ssm.doInsert(apppObj);
								} else {
									// 증명서 발급신청 배치
									sxp = new SepoaXmlParser(this, "doUpdateSSIAD");
									ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
									ssm.setSepoaInfo(info);
									ssm.doUpdate(element, new SingletonMap("SMR_STATUS", "C"));
								}
							} else if ("3".equalsIgnoreCase(appStatus)) {
								// 반려 처리 SSIAD R상태로 업데이트
								sxp = new SepoaXmlParser(this, "doUpdateSSIAD");
								ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
								ssm.setSepoaInfo(info);
								ssm.doUpdate(element, new SingletonMap("SMR_STATUS", "R"));
							} else if ("4".equalsIgnoreCase(appStatus)) {
								// 상신 취소 SSIAD T상태로 업데이트
								sxp = new SepoaXmlParser(this, "doUpdateSSIAD");
								ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
								ssm.setSepoaInfo(info);
								ssm.doUpdate(element, new SingletonMap("SMR_STATUS", "T"));
							} else {
								Logger.warn.println("Not in a processable status SMR_NUMBER : " + element.get("SMR_NUMBER"));
							}
						} else {
							Logger.warn.println("succeeded in checking the approval status, but the result is not correct. SMR_NUMBER : " + element.get("SMR_NUMBER"));
						}
					}
				} catch (Exception e) {
					Logger.err.println("The approval completion process failed for the seal request SMR_NUMBER : " + element.get("SMR_NUMBER"), e);
				}
			});
		}).get();
	}

	// 실행(타켓 취득 및 비즈니스로직 수행)
	public SepoaOut execute() throws SepoaServiceException {
		Logger.debug.println("############### Approval 배치실행  ###############");
		setFlag(true);
		setStatus(1);
		ConnectionContext ctx = getConnectionContext();
		try {			
			ctx = getConnectionContext();
			
			List<Map<String, String>> dataList = getTargetRecord(ctx);
			if (dataList.size() > 0) {
				setTargetRecord(ctx, dataList);
			}
			
			Commit();
		} catch (Exception exception) {
			exception.printStackTrace();
			Rollback();
			setFlag(false);
			setStatus(0);
			Logger.info.println(info.getSession("ID"), this, "Error is occured.", exception);
			System.out.println((new StringBuilder()).append(SepoaApprovalBatch.class.getSimpleName()+"() ERROR : ").append(exception.getMessage()).toString());
		} finally {
			try {
				ctx.getConnection().close();
			} catch (SQLException e) {
			}
		}

		return getSepoaOut();
	}
	
	public static void main(String[] args) {
		if (isRunning) {
			isRunning = false; // 현재처리중

			String house_code = "007"; // 하우스코드
			SepoaInfo sepoainfo = new SepoaInfo(house_code,
					"COMPANY_CODE=1000^@^ID=BATCH^@^LANGUAGE=KO^@^NAME_LOC=BATCH^@^NAME_ENG=BATCH^@^DEPARTMENT=BATCH^@^DEPARTMENT_NAME_LOC=BATCH^@^");
			try {
				sepoainfo.setThreadLocal();

				String startLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] START : ApprovalBatch";
				System.out.println(startLog);
				Logger.info.println(startLog);
				SepoaBatchService.initialize();
				SepoaBatchService.run(new SepoaApprovalBatch("TRANSACTION", sepoainfo));

				String endLog = "[" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())
						+ "] END : ApprovalBatch";
				System.out.println(endLog);
				Logger.info.println(endLog);
			} catch (Exception e) {
				Logger.err.println(sepoainfo.getSession("id"), "BATCH", "Unexpected Exception.", e);
			} finally {
				sepoainfo.unsetThreadLocal();
				isRunning = true;
			}
		}
	}
}
