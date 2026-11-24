package sepoa.svc.seal.cbo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.approval.SepoaApproval;
import sepoa.fw.approval.SepoaApprovalLineSetting;
import sepoa.fw.approval.SepoaApprovalProgress;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.authority.Permission.PrincipalAgentType;
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;
import sepoa.seal.SepoaReqSealManager;
import sepoa.seal.SepoaSealManagementManager;
import sepoa.svc.seal.SepoaSealService;
import sepoa.svl.util.SepoaActionController;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자인장 - 날인요청 모듈")
public class SepoaReqSealService extends SepoaSealService {
	final static String USE_SEAL_CREATE = "USW";
	final static String CORP_SEAL_CREATE = "CSW";
	
	public SepoaReqSealService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	protected String getClassName() {
		return this.getClass().getName();
	}
	
	protected SepoaReqSealManager getSepoaReqSealManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		if(this.getClassName().startsWith("sepoa.svc.sealmk2")) {
			return new sepoa.sealmk2.SepoaReqSealManager(info, config, ctx);
		} else {
			return new SepoaReqSealManager(info, config, ctx);
		}
	}
	
	protected SepoaSealManagementManager getSepoaSealManagementManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		if(this.getClassName().startsWith("sepoa.svc.sealmk2")) {
			return new sepoa.sealmk2.SepoaSealManagementManager(info, config, ctx);
		} else {
			return new SepoaSealManagementManager(info, config, ctx);
		}
	}
	
	@Description("날인신청등록 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doRequestService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = null;
		
		try {
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Configuration conf = new Configuration();
			String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"").replaceAll("~REPTOQM~", "&#34;");
			
			Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
			
			List<String>  Id_data = new ArrayList<String> ();
			Map<String, String> mailData = new HashMap<String, String>();
			SimpleDateFormat format = new SimpleDateFormat ("yyyy년 MM월 dd일 HH시 mm분");
			
			String currentPrc = headerData.get("CURRENT_PRC").toString();
			String status = "";
			String applyType = "";
			String smrNumber = "";
			String smdNumber = "";
			String confirmStatus = "";
			boolean doAfterFlag = false;
			boolean isGrant = false;
			boolean mailNow = true;	// 바로 메일을 보냄
			
			SepoaReqSealManager srm = getSepoaReqSealManager(info, config, getConnectionContext());
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			Map<String, String> reqSealInfo = (Map<String, String>)parseMap.get("REQ_SEAL_INFO");
			
			if(StringUtils.isNotBlank(currentPrc)) {
				// 날인요청 객체 정보
				srm.setDocBaiscInfo(reqSealInfo);
				// 날인요청 서류 문서 리스트
				List<Map<String, Object>> documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
				// 교부서류 문서 리스트
				List<Map<String, String>> attachDocumentList = (List<Map<String, String>>)parseMap.get("ATTACH_DOCUMENT_LIST");
				switch (currentPrc) {
				case "CREATE_CERT":
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					applyType = reqSealInfo.get("APPLY_TYPE");
					// 날인요청 객체 Setting
					Map<String, String> paramMap_cert = new HashMap<String, String>();
					paramMap_cert.put("SMR_NUMBER", smrNumber);
					paramMap_cert.put("APPLY_TYPE", applyType);
					if(srm.doSelectRequestList(paramMap_cert) != null) {
						// 동일한 채번의 날인요청이 존재할 시
						reqSealInfo.put("ADD_DATE", SepoaDate.getShortDateString());
						if(!srm.doUpdateReqSealDoc(reqSealInfo)) {
							// 날인요청 Table Update
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					} else {
						// 날인요청 객체 처리 Table Insert
						if(srm.doCreateReqSeal() == null) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						} else {
							if(!srm.doAddProcessHistory("SEAL_REQUEST")) {
								int errCode = srm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}
					}
					
					String cert_delrow2 = headerData.get("DEL_ROW2");
					if(!cert_delrow2.isEmpty()) {
						List<String> delRow_list = Arrays.asList(cert_delrow2.split(","));
						
						if(delRow_list != null && delRow_list.size() > 0) {
							for(String selectedRow : delRow_list) {
								Map<String, String> delRow2 = new HashMap<String, String>(); 
								delRow2.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
								delRow2.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
								delRow2.put("SMR_NUMBER", smrNumber);
								delRow2.put("SD_NUMBER", selectedRow);
								
								if(!srm.doDeleteReqSealAttachDoc(delRow2)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
								
							}
						}
					}
					
					//법인 &&사용인감 모두 교부서류 사용 
					if(getStatus() == 1 && (attachDocumentList != null && attachDocumentList.size() > 0)) {
						for(Map<String, String> attDoc : attachDocumentList) {
							attDoc.put("SMR_NUMBER", smrNumber);
							
							if(srm.doSelectCorpAttachDoc(attDoc) != null) {
								// 교뷰서류 데이터 Update
								if(!srm.doUpdateCorpAttachDoc(attDoc)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							} else {
								// 교부서류 데이터 Insert
								if(srm.doSaveCorpAttachDoc(attDoc) == null) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
						}
					}
					status = "T";
					break;
				case "CREATE":
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					applyType = reqSealInfo.get("APPLY_TYPE");
					// 날인요청 객체 Setting
					Map<String, String> paramMap = new HashMap<String, String>();
					paramMap.put("SMR_NUMBER", smrNumber);
					paramMap.put("APPLY_TYPE", applyType);
					if(srm.doSelectRequestList(paramMap) != null) {
						// 동일한 채번의 날인요청이 존재할 시
						reqSealInfo.put("ADD_DATE", SepoaDate.getShortDateString());
						if(!srm.doUpdateReqSealDoc(reqSealInfo)) {
							// 날인요청 Table Update
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					} else {
						// 날인요청 객체 처리 Table Insert
						if(srm.doCreateReqSeal() == null) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						} else {
							if(!srm.doAddProcessHistory("SEAL_REQUEST")) {
								int errCode = srm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}
					}
					
					// 날인신청 - 날인서류 데이터
					if(getStatus() == 1 && (documentList != null && documentList.size() > 0)) {
						
						List<String> delRow_list = (List<String>) documentList.get(0).get("DELROW_DATA");
						if(delRow_list != null && delRow_list.size() > 0) {
							for(String selectedRow : delRow_list) {
								Map<String, String> delRow = new HashMap<String, String>(); 
								delRow.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
								delRow.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
								delRow.put("SMR_NUMBER", smrNumber);
								delRow.put("SMD_NUMBER", selectedRow);
								
								if(!srm.doDeleteReqSealDoc(delRow)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
								if(!srm.doDeleteReqSealDocSealWithoutManageNumber(delRow)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
						}
						
						for(Map<String, Object> sealDoc : documentList) {
							smdNumber = String.valueOf(sealDoc.get("SMD_NUMBER"));
							sealDoc.put("SMR_NUMBER", smrNumber);
							
							Map<String, String> smdParam = new HashMap<String, String>();
							smdParam.put("SMR_NUMBER", smrNumber);
							smdParam.put("SMD_NUMBER", smdNumber);
							if(!srm.doDeleteReqSealDocSealWithoutManageNumber(smdParam)) {
								int errCode = srm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
							
							List<Map<String, String>> selectSealList = (List<Map<String, String>>) sealDoc.get("SELECT_SEAL_LIST");
							if(selectSealList != null && selectSealList.size() > 0) {
								for(Map<String, String> selectedSeal : selectSealList) {
									// 날인서류 인장에 필요한 데이터 셋팅 / 날인요청번호 / 날인서류번호
									selectedSeal.put("SMR_NUMBER", smrNumber);
									selectedSeal.put("SMD_NUMBER", smdNumber);
									
									if(srm.doSelectReqSealDocSeal(selectedSeal) != null) {
										// 날인서류 인장 Update
										if(!srm.doUpdateSealDocSeal(selectedSeal)) {
											int errCode = srm.getLastErrCode();
											if(errCode != 0) {
												setStatus(errCode);
											}
										}
									} else {
										// 날인서류 인장 데이터 INSERT
										if(srm.doSaveSealDocSeal(selectedSeal) == null) {
											int errCode = srm.getLastErrCode();
											if(errCode != 0) {
												setStatus(errCode);
											}
										}
									}
								}
							}
							
							Map<String, String> sealDocParsed = parseReqSealDocData(sealDoc); 
							if(srm.doSelectReqSealDoc(sealDocParsed) != null) {
								// 날인서류 데이터 Update
								if(!srm.doUpdateSealDoc(sealDocParsed)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							} else {
								// 날인서류 데이터 INSERT
								if(srm.doSaveSealDoc(sealDocParsed) == null) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
						}
					}
					
					String str_delrow2 = headerData.get("DEL_ROW2");
					if(!str_delrow2.isEmpty()) {
						List<String> delRow_list = Arrays.asList(str_delrow2.split(","));
						
						if(delRow_list != null && delRow_list.size() > 0) {
							for(String selectedRow : delRow_list) {
								Map<String, String> delRow2 = new HashMap<String, String>(); 
								delRow2.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
								delRow2.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
								delRow2.put("SMR_NUMBER", smrNumber);
								delRow2.put("SD_NUMBER", selectedRow);
								
								if(!srm.doDeleteReqSealAttachDoc(delRow2)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
								
							}
						}
					}
					
					//법인 &&사용인감 모두 교부서류 사용 
					if(getStatus() == 1 && (attachDocumentList != null && attachDocumentList.size() > 0)) {
						for(Map<String, String> attDoc : attachDocumentList) {
							attDoc.put("SMR_NUMBER", smrNumber);
							
							if(srm.doSelectCorpAttachDoc(attDoc) != null) {
								// 교뷰서류 데이터 Update
								if(!srm.doUpdateCorpAttachDoc(attDoc)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							} else {
								// 교부서류 데이터 Insert
								if(srm.doSaveCorpAttachDoc(attDoc) == null) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
						}
					}
					status = "T";
					break;
				case "APPROVAL":
					// 결재모듈 호출
					System.out.println("결재 004");
					ctx = getConnectionContext();
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
					headerData.put("APP_TYPE", reqSealInfo.get("APP_TYPE"));
					
					// 결재
					// ※※※ 결재라인이 들어가는 페이지에 대해서는 아래 서비스들을 모두 호출해 주어야 한다. 제거 금지!!!
		            // SepoaApprovalLineSetting
		            // SepoaApprovalProgress
		            // SepoaApprovalConfirm
		            // 각각의 생성자에 ctx를 넘겨줄 경우 Connection관리는 업무에서 처리되며,
		            // 넘겨주지 않을경우 결재모듈에서 자체 커밋한다. 해당부분에 대해서는 데이타 처리에 따라 알맞게 처리하면 된다.
		            // 결재선 저장 시작 -----------------------------------------------------------------------------
					Logger.debug.println("==APP_TYPE="+MapUtils.getString(reqSealInfo, "APP_TYPE"));
					Logger.debug.println("==APP_STATUS="+MapUtils.getString(reqSealInfo, "APP_STATUS" , ""));
					applyType = reqSealInfo.get("APPLY_TYPE");
		            headerData.put("SUX_WKEY1", smrNumber);
		            SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
		            sepoaApprovalLineSetting.doApprovalProcess(allData);
		            
		            if(!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_SEAL", smrNumber, null);
					}
		            List<Map<String, String>> appLine = (List<Map<String, String>>) parseMap.get("APP_LINE");
		            for (int i=0; i<appLine.size(); i++) {
		            	Map<String, String> appLineMap = appLine.get(i);
		            	
		            	if (appLineMap.get("APP_METHOD").equals("03")) {
		            		isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.READ);
		            	} else {
		            		isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.WRITE);
		            	}
						if(getStatus() == 1 && !isGrant) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
		            }
		            // 결재선 저장 종료 -----------------------------------------------------------------------------
		            // 결재선 까지 같이 저장 후 Commit하자.
		            //Commit();            
		            // 업무단에서 리턴받아서 처리할 키값들을 셋팅한다.
		            Map<String, String> map = new HashMap<>();
		            map.put("returnType" , "param");
		            map.put("APP_NO"     , MapUtils.getString(headerData, "APP_NO"     , ""));
		            map.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
		            map.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "I"));
		            map.put("SAVE_MODE", MapUtils.getString(headerData,"SAVE_MODE",""));
		            setObject(map);
		            //if("I".equals(MapUtils.getString(headerData, "APP_STATUS","T"))){
		                // ※※※ 중요 BackProcess에서 업무단 상태값도 보통 변경을 하기 때문에 Commit후 호출을 권장.
		                // 결재상신 후 처리되는 BackProcess 시작---------------------------------------------------------
		                SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
		                sepoaApprovalProgress.doApprovalProcess(allData);
		                //Commit();
		                // 결재상신 후 처리되는 BackProcess 종료---------------------------------------------------------
		                
		                /*
		                // N-결재미사용 인경우만 바로 승인되도록 처리한다.
		                String approvalType = CommonUtil.getConfig("sepoa.approval.type." + MapUtils.getString(headerData, "APP_TYPE", ""));
		                if("N".equals(approvalType)){
		                    // 결재완료 후 처리되는 BackProcess 시작---------------------------------------------------------
		                    SepoaApprovalConfirm sepoaApprovalConfirm = new SepoaApprovalConfirm(ctx, info);
		                    sepoaApprovalConfirm.doApprovalProcess(allData);
		                    Commit();
		                    // 결재완료 후 처리되는 BackProcess 종료---------------------------------------------------------
		                }*/
		            //}
					
					status = "I";
					break;
				case "LEGAL":
					// 인감책임자 승인 로직
					applyType = reqSealInfo.get("APPLY_TYPE");
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					confirmStatus = reqSealInfo.get("CONFIRM_STATUS");
					
					//####### 메일발송시 필요한 데이터 셋팅
					mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
                	mailData.put("SMR_NUMBER", smrNumber);
                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
                	mailData.put("TIME", format.format(new Date()));
                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
                	mailData.put("CONTENTS", smrNumber);
                	//###########################
					
                	if(!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_SEAL", smrNumber, null);
					}
                	
                	if("U".equals(applyType) && (SepoaModuleService.checkBusiness(new Configuration(), info, "USW1635987860553")) 
                			||"C".equals(applyType) && (SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1635988016964"))) {
                		List<Map<String, String>>  doSearchCtrlCodeList = srm.doSearchCtrlCode(srm.getDocBaiscInfo()) ;
    					if (doSearchCtrlCodeList.size() > 0) {
    						for(Map<String,String> resultMap : doSearchCtrlCodeList) {
    							isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), resultMap.get("CTRL_PERSON_ID"), Authorized.WRITE);
    							if(getStatus() == 1 && !isGrant) {
    								int errCode = srm.getLastErrCode();
    								if(errCode != 0) {
    									setStatus(errCode);
    								}
    							}
    						}
    					}
        			}
                	
					if("LEGAL_ACCEPT".equals(confirmStatus)) {
						// 인감책임자 승인
						/*
						headerData.put("NEXT_PRC", "COMPLETE");
						status = "O";
						doAfterFlag = true;
						
						//메일 셋팅
	                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_COMPLETE"));
	                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_COMPLETE"));
	                	mailData.put("MESSAGE_TYPE", "LEGAL_COMP");
	                	*/
						doApprovalConfirmReady(ctx, allData);
						mailNow = false;
					} else if("LEGAL_REJECT".equals(confirmStatus)) {
						// 인감책임자 반려
						status = "N";
						
						//메일 셋팅
						mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REJECT"));
						mailData.put("REQUESTER", info.getSession("NAME_LOC"));
						mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_REJECT"));
						mailData.put("MESSAGE_TYPE", "LEGAL_REJECT");
					} else if("LEGAL_REQUEST".equals(confirmStatus)) {
						// 인감책임자한테 승인 요청
						status = "G";
						
						//메일 셋팅
						mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REQUEST"));
						mailData.put("REQUESTER", info.getSession("NAME_LOC"));
						mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_PROGRESS"));
						mailData.put("MESSAGE_TYPE", "LEGAL_REQ");
						
						if(!srm.doAddProcessHistory("LEGAL_APP")) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					}
					
					break;
				case "MANAGER":
					applyType = reqSealInfo.get("APPLY_TYPE");
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					confirmStatus = reqSealInfo.get("CONFIRM_STATUS");
					
					//####### 메일발송시 필요한 데이터 셋팅
					mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
                	mailData.put("SMR_NUMBER", smrNumber);
                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
                	mailData.put("TIME", format.format(new Date()));
                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
                	mailData.put("CONTENTS", smrNumber);
                	//###########################
					
					if(!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_SEAL", smrNumber, null);
					}

					if("MANAGER_REQUEST".equals(confirmStatus)) {
						status = "W";
						// 요청시에 인감담당자한테 권한을 줘야한다.
						if(documentList != null && documentList.size() > 0) {
							for(Map<String, Object> sealDoc : documentList) {
								List<Map<String, String>> selectSealList = (List<Map<String, String>>) sealDoc.get("SELECT_SEAL_LIST");
								for(Map<String, String> selectedSeal : selectSealList) {
									isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), selectedSeal.get("SEAL_MG_USER_ID"), Authorized.WRITE);
									if(getStatus() == 1 && !isGrant) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
									//메일 셋팅
									Id_data.add(selectedSeal.get("MANAGER_ID"));
				                	mailData.put("SEAL_MG_USER_ID", selectedSeal.get("MANAGER_ID"));
				                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REQUEST"));
				                	mailData.put("REQUESTER", info.getSession("NAME_LOC"));
				                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_PROGRESS"));
				                	mailData.put("MESSAGE_TYPE", "MANAGER_REQ");
						            
								}
							}
						}
						
					} else if("MANAGER_ACCEPT".equals(confirmStatus)) {
						// 승인시에 반복문을 사용하여 인감담당자의 다른 승인이 남았는지 체크
						// 인감담당자의 승인이 남은 날인요청의 경우에는 status를 변경하지 않는다.
						// 먼저 본인 인감 승인 여부 변경해주고 시작
						Map<String, String> parameter = new HashMap<String, String>();
						
						smrNumber = reqSealInfo.get("SMR_NUMBER");
						parameter.put("SMR_NUMBER", smrNumber);
						for(Map<String, Object> docMap : documentList) {
							List<Map<String, String>> selectSealList = (List<Map<String, String>>) docMap.get("SELECT_SEAL_LIST");
							for(Map<String, String> selectedSeal : selectSealList) {
								String sessionId = info.getSession("ID");
								String sessionDept = info.getSession("DEPARTMENT");
								// 선택된 인감의 값이 비어있지 않아야 한다
								if(!"".equals(selectedSeal.get("MANAGER_ID")) && !"".equals(selectedSeal.get("MANAGE_DEPT"))) {
									// 현재세션(승인을 누른 인감담당자)의 아이디와 부서가 선택된 인감의 관리부서, 관리자ID랑 일치하여야한다.
	 								if(sessionId.equals(selectedSeal.get("MANAGER_ID"))	|| sessionId.equals(selectedSeal.get("RECE_USER_ID"))) {
										// 승인여부 변경 및 선택인감 상태 업데이트
										selectedSeal.put("ACCEPT_FLAG", "Y");
										if(!srm.doUpdateSealDocSeal(selectedSeal)) {
											int errCode = srm.getLastErrCode();
											if(errCode != 0) {
												setStatus(errCode);
											}
										}
									}
								}
							}
						}
						parameter.put("ACCEPT_FLAG", "N");
						
						// 승인 여부가 N(미승인) 상태의 데이터가 한건도 없으면 다음모듈로 진행
						if(srm.doSelectReqSealDocSeal(parameter) == null) {
							/*status = "M";
							doAfterFlag = true;
							headerData.put("NEXT_PRC", "COMPLETE");
							
							//메일 셋팅
		                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_COMPLETE"));
		                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_COMPLETE"));
		                	mailData.put("MESSAGE_TYPE", "MANAGER_COMP");*/
							doApprovalConfirmReady(ctx, allData);
							mailNow = false;
						} else {
							status = "W";
						}
					} else if("MANAGER_REJECT".equals(confirmStatus)) {
						status = "F";
						
 						Map<String, String> parameter = new HashMap<String, String>();
						
						smrNumber = reqSealInfo.get("SMR_NUMBER");
						parameter.put("SMR_NUMBER", smrNumber);
						parameter.put("ACCEPT_FLAG", "N");

						if(!srm.doUpdateSealDocSeal(parameter)) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}		
						
						//메일 셋팅
						mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REJECT"));
						mailData.put("REQUESTER", info.getSession("NAME_LOC"));
						mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_REJECT"));
						mailData.put("MESSAGE_TYPE", "MANAGER_REJECT");
					}
					
					break;
				case "REAL_SEAL_DOC_UPDATE":
					if(getStatus() == 1 && (documentList != null && documentList.size() > 0)) {
						for(Map<String, Object> sealDoc : documentList) {
							if(srm.doSelectReqSealDoc(parseReqSealDocData(sealDoc)) != null) {
								// 날인서류 데이터 Update
								if(!srm.doUpdateSealDoc(parseReqSealDocData(sealDoc))) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
						}
					}
					
					status = "C";
					break;
				case "COMPLETE":
					/*smrNumber = reqSealInfo.get("SMR_NUMBER");
					applyType = reqSealInfo.get("APPLY_TYPE");
					
					// 인장 치환 처리
					if(getStatus() == 1) {
						reqSealInfo.remove("SMR_STATUS");
						srm.setDocBaiscInfo(reqSealInfo);
						if(!srm.doConvertReqSealDocument()) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					}
					
					//날인완료 히스토리 적재
					if(getStatus() == 1) {
						if(!srm.doAddProcessHistory("SEAL_COMPLETE")) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					}
					
					status = "C";*/
					doApprovalConfirmReady(ctx, allData);
					mailNow = false;
					break;
				default:
					break;
				}				
			}
			
			// 날인요청 Status 업데이트
			String curStatus = MapUtils.getString(reqSealInfo, "SMR_STATUS", null);
			if (curStatus == null || !"T".equals(curStatus) || !"T".equals(status)) {
				if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus(status)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			if(doAfterFlag) {
				if(getStatus() == 1 && !doRequestServiceAfter(allData, srm)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			//메일
			if(getStatus() == 1 && mailNow && SepoaModuleService.checkBusiness(new Configuration(), info, "MAIL1649307822120")) {
	            try {
	            	SepoaPushManager mailpush = new SepoaPushManager(info, conf);
	     			SepoaMessage message = new SepoaMessage();
	     			
	     			HttpServletRequest req = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
    	 			String baseURL = req.getRequestURL().toString().replace(req.getRequestURI(),"");
    	 			mailData.put("URL",baseURL);
	     			
	     			if(Id_data.size()>0) {
	    	 			for(String id :Id_data) {
	    	 				mailData.put("SEAL_MG_USER_ID", id);
	    	 				
	    	 				message.addParams(mailData);
	    					message.setMessageType(mailData.get("MESSAGE_TYPE"));
	    					mailpush.addMessage(message);
	                    	mailpush.accept();
	    	 			}
    	 			}else {
    	 				message.addParams(mailData);
    					message.setMessageType(mailData.get("MESSAGE_TYPE"));
    					mailpush.addMessage(message);
                    	mailpush.accept();
    	 			}
                	
	        		Logger.debug.println("##### 메일 발송 성공 #####");
				} catch (Exception e2) {
					Logger.err.println("$$$$$ 메일 발송 실패 $$$$$");
				}
            }
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("날인요청 서비스 후처리")
	@SuppressWarnings("unchecked")
	public boolean doRequestServiceAfter(Map<String, Object> allData, SepoaReqSealManager manager) {
		Map<String, String> headerRawData = new HashMap<String, String>();
		Map<String, String> headerData = new HashMap<String, String>();
		Map<String, Object> parseMap = new HashMap<String, Object>();
		
		try {
			String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
			String nextPrc = "";
			String externalFlag = (String)allData.get("externalFlag");
			if(StringUtils.isNotBlank(externalFlag) && "Y".equals(externalFlag)) {
				headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
				parseMap = MapUtils.getMap(headerData, "PARAMETER");
				currentPrc = (String)allData.get("CURRENT_PRC");
				nextPrc = MapUtils.getString(allData, "NEXT_PRC", null);
			} else {
				headerRawData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
				parseMap = new Gson().fromJson(MapUtils.getString(headerRawData, "PARAMETER"), Map.class);
				headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
				currentPrc = headerData.get("CURRENT_PRC");
				nextPrc = MapUtils.getString(headerData, "NEXT_PRC", null);
			}
			
			// 날인요청 객체 정보
			Map<String, String> reqSealInfo = (Map<String, String>)parseMap.get("REQ_SEAL_INFO");
			manager.setDocBaiscInfo(reqSealInfo);
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			// 날인요청 서류 문서 리스트
			List<Map<String, Object>> documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
			// 교부서류 문서 리스트
			List<Map<String, String>> attachDocumentList = (List<Map<String, String>>)parseMap.get("ATTACH_DOCUMENT_LIST");
			
			
			String currentAlias = MapUtils.getString(reqSealInfo, "APPLY_TYPE");
			String parentAlias = "C".equals(currentAlias)? CORP_SEAL_CREATE : USE_SEAL_CREATE;
			String status = "";
			String smrNumber = reqSealInfo.get("SMR_NUMBER");
			String applyType = reqSealInfo.get("APPLY_TYPE");
			if(nextPrc == null || "".equals(nextPrc)) {
				Map<String, String> paramsMap = new HashMap<>();
				paramsMap.put("ALIAS", currentPrc);
				paramsMap.put("PARENT", parentAlias);
				Map<String, String> nextModuleMap = this.doFindNextModule(paramsMap);
				nextPrc = MapUtils.getString(nextModuleMap, "ALIAS", nextModuleMap.get("MODULE"));
			}
			
			switch(nextPrc) {
				case "LEGAL":
					status = "G";
					break;
				/*case "COMPLETE":
					// 인장 치환 처리
					reqSealInfo.remove("SMR_STATUS");
					reqSealInfo.put("CONFIRM_DATE", SepoaDate.getShortDateString());
					reqSealInfo.put("CONFIRM_TIME", SepoaDate.getShortTimeString());
					reqSealInfo.put("CONFIRM_USER_ID", info.getSession("ID"));
					
					manager.setDocBaiscInfo(reqSealInfo);
					if(getStatus() == 1) {
						if(!manager.doConvertReqSealDocument()) {
							int errCode = manager.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					}
					
					//날인완료 히스토리 적재
					if(getStatus() == 1) {
						if(!manager.doAddProcessHistory("SEAL_COMPLETE")) {
							int errCode = manager.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					}
					
					status = "C";
					break;*/
			}
			
			// 날인요청 Status 업데이트
			if(getStatus() == 1 && !manager.doUpdateReqSealDocStatus(status)) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return true;
	}
	
	@Description("발송인 처리 서비스")
	@SuppressWarnings("unchecked")
	public boolean doProcessSendSeal(String smrNumber, SepoaReqSealManager srm) {
		setStatus(1);
		setFlag(true);
		
		try {
			Config config = new Configuration();
			Map<String, String> paramMap = new HashMap<String, String>();
			paramMap.put("SMR_NUMBER", smrNumber);
			srm.setDocBaiscInfo(paramMap);
			List<Map<String, String>> docList = srm.doSelectReqSealDoc(paramMap);
			if(docList != null) {
				for(Map<String, String> docInfo : docList) {
					srm.doProcessSendSeal(docInfo);
				}
			} else {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
			return false;
		}
		
		return true;
	}
	
	@Description("인장조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectSealService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, getConnectionContext());
			SepoaFormater sf = null;
			
			String flagArr = headerData.get("FLAGARR");
			
			if(flagArr.equals("E")) {
				headerData.put("ELECTRON_FLAG", "Y");
			}else if(flagArr.equals("EE")) {
				headerData.put("ELECTRON_SIGN", "Y");
			}else if(flagArr.equals("R")) {
				headerData.put("REAL_FLAG", "Y");
			}
			
			String sealType = MapUtils.getString(headerData, "SEAL_TYPE", "");
			
			if(SepoaModuleService.checkBusiness(new Configuration(), info, "US1647933334337")) {
				headerData.put("SEAL_STATUS", "C,R,U");
			}
			
			List<Map<String, String>> searchResult = new ArrayList<Map<String, String>>();
			String systemAdminMenuProfileCode = CommonUtil.getConfig("sepoa.system.manager.profile.code");
			String myMenuProfileCode = info.getSession("MENU_PROFILE_CODE");
			
			if (!systemAdminMenuProfileCode.equals(myMenuProfileCode)) {	// 시스템관리자가 아니면
				if(SepoaModuleService.checkBusiness(new Configuration(), info, "US1637133452574")) { // 부서 기준
					if (!sealType.equals("C")) headerData.put("SORT_DEPT", "Y");	// 법인인감은 부서무관
				} 
			}
			
			if(SepoaModuleService.checkBusiness(new Configuration(), info, "US1637133464814")) {
				// 용도 기준
				searchResult = sm.doSelectSealListRequiredUse(headerData); 
			} else {
				// 부서 기준
				searchResult = sm.doSelectSealList(headerData);
			}
			
			if(searchResult == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				sf = new SepoaFormater(searchResult);
				if(sf.getRowCount() > 0) {
					searchResult = new SepoaFormater(searchResult).getListMap();
				}
				
				setValue(searchResult);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("외부 교부요청 서류조회 서비스")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut externalSelectAttachDocList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			ConnectionContext ctx = getConnectionContext();			
			//Request Body
			Map<String, Object> reqSealParmas = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
			//파라미터 파싱
			Map<String, Object> parameter = MapUtils.getMap(reqSealParmas, "PARAMETER");
			// result List
			List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
			Map<String, Object> resultObject = new HashMap<String, Object>();
			//하우스코드
			String houseCode = String.valueOf(parameter.get("HOUSE_CODE"));
			//컴퍼니코드
			String companyCode = String.valueOf(parameter.get("COMPANY_CODE"));
			//조회 요청자 ID
			String reqUserId = String.valueOf(parameter.get("REQ_USER_ID"));
			//Manager
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
			//사용자 조회 후 변경될 인포 객체
			SepoaInfo newInfo = null;
			
			Map<String, String> paramMap = new HashMap<String, String>();
			paramMap.put("HOUSE_CODE", houseCode);
			paramMap.put("COMPANY_CODE", companyCode);
			paramMap.put("USER_ID", reqUserId);
			
			if(!srm.doCheckExistUser(houseCode, companyCode, reqUserId)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				//실제 사용자로 판단되면 info객체를 현재 스레드에서 사용할 수 있게 설정
				newInfo = new SepoaInfo(houseCode, "COMPANY_CODE="+companyCode+"^@^ID="+reqUserId+"^@^LANGUAGE=KO^@^");
				newInfo.setThreadLocal();
				srm = getSepoaReqSealManager(newInfo, new Configuration(), ctx);
			}
			
			List<Map<String, String>> attachList = srm.doSelectCorpAttachDocHeader(paramMap);
			if(attachList == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				resultObject.put("ATTACH_LIST", attachList);
				resultList.add(resultObject);
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				setValue(resultList);
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@Description("외부 인터페이스 날인 완료 문서 다운로드 서비스")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut completeDocDowonload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			ConnectionContext ctx = getConnectionContext();		
			String smrNumber = headerData.get("docNo");
			String reqUserId = headerData.get("reqUserId");
			String houseCode = headerData.get("houseCode");
			String companyCode = headerData.get("companyCode");
			Map<String, String> paramMap = new HashMap<String, String>();
			SepoaInfo newInfo = null;
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
			if(!srm.doCheckExistUser(houseCode, companyCode, reqUserId)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				if(getStatus() == 1) {
					//실제 사용자로 판단되면 info객체를 현재 스레드에서 사용할 수 있게 설정
					newInfo = new SepoaInfo(houseCode, "COMPANY_CODE="+companyCode+"^@^ID="+reqUserId+"^@^LANGUAGE=KO^@^");
					newInfo.setThreadLocal();
					srm = getSepoaReqSealManager(newInfo, new Configuration(), ctx);
				}
				
				if(StringUtils.isNotBlank(smrNumber)) {
					paramMap.put("SMR_NUMBER", smrNumber);
					srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
					srm.setDocBaiscInfo(paramMap);
					List<Map<String, String>> reqInfo = srm.doSelectRequestList();
					if(reqInfo == null) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					} else {
						srm.setDocBaiscInfo(reqInfo.get(0));
					}
					
					String smrStatus = MapUtils.getString(reqInfo.get(0), "SMR_STATUS");
					
					if(StringUtils.isBlank(smrStatus)) {
						setStatus(102412);
						setFlag(false);
						setValue("");
						setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
						return getSepoaOut();
					}
					
					if(!"C".equals(smrStatus)) {
						setStatus(103337);
						setFlag(false);
						setValue("");
						setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
						return getSepoaOut();
					}
					
					List<Map<String, String>> docList = srm.doSelectReqSealDoc(paramMap);
					if(docList == null) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					//US1638852635132
					boolean checkYn = true;
					if (SepoaModuleService.checkBusiness(new Configuration(), info, "US1638852635132")) {
						checkYn = srm.doCheckFileDownload(smrNumber);
					}
					
					if(checkYn) {
						Map<String, String> fileInfo = srm.doDocumentDownload(docList, false);
						if(fileInfo == null) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
								setFlag(false);
							}
						} else {
							Boolean res = srm.addDownloadCount(docList);
							if (res) {
								setValue(fileInfo);
								addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
							} else {
								int errCode = srm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
									setFlag(false);
								}
							}
						}
						
					} else {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							setFlag(false);
							setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
							return getSepoaOut();
						}
					}
					
					
					if(getStatus() != 1) {
						setFlag(false);
						setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
					}
				} else {
					setStatus(100004);
					setFlag(false);
					setValue("");
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				}
			}
			
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@Description("외부 인터페이스 날인요청 서비스")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut externalReqSealCreateService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			ConnectionContext ctx = getConnectionContext();			
			//Request Body
			Map<String, Object> reqSealParmas = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
			//파라미터 파싱
			Map<String, Object> parameter = MapUtils.getMap(reqSealParmas, "PARAMETER");
			//날인요청 정보
			Map<String, String> reqSealInfo = MapUtils.getMap(parameter, "REQ_SEAL_INFO");
			//날인서류 리스트 
			List<Map<String, Object>> reqSealDocList = (List<Map<String, Object>>) parameter.get("DOCUMENT_LIST");
			//교부서류 정보
			List<Map<String, String>> attachDocList = (List<Map<String, String>>) parameter.get("ATTACH_DOCUMENT_LIST");
			//날인요청 매니저 생성
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
			//사용할 날인요청 채번은 미리 발급하고 시작
			String smrNo = DocumentUtil.getDocNumber(info, "SMR").result[0];
			//링크 매니저를 사용하여 발급되는 키 값
			String accessLink = null;
			// 자동 완료 플래그
			String autoCompleteFlag = reqSealInfo.get("AUTO_COMPLETE_FLAG");
			if(StringUtils.isBlank(autoCompleteFlag)) {
				autoCompleteFlag = "N";
			}
			//사용자 조회 후 변경될 인포 객체
			SepoaInfo newInfo = null;
			// return Map
			Map<String, String> returnMap = new HashMap<String, String>();
			
			//날인요청자 ID 파싱후 유효성검사
			String houseCode = reqSealInfo.get("HOUSE_CODE");
			String companyCode = reqSealInfo.get("COMPANY_CODE");
			String userId = reqSealInfo.get("REQ_USER_ID");
			if(!srm.doCheckExistUser(houseCode, companyCode, userId)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				//실제 사용자로 판단되면 info객체를 현재 스레드에서 사용할 수 있게 설정
				newInfo = new SepoaInfo(houseCode, "COMPANY_CODE="+companyCode+"^@^ID="+userId+"^@^LANGUAGE=KO^@^");
				newInfo.setThreadLocal();
				this.info = newInfo;
				
				//SMR_NUMBER 채번 발급 및 기본데이터 적재
				reqSealInfo.put("SMR_NUMBER", smrNo);
				reqSealInfo.put("SMR_STATUS", "T");
				
				//날인요청 매니저 재생성 및 재할당
				srm = getSepoaReqSealManager(newInfo, new Configuration(), ctx);
				srm.setDocBaiscInfo(reqSealInfo);
			}
			
			//날인요청 테이블 작성
			if(getStatus() == 1 && srm.doCreateReqSeal() == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			//날인서류 데이터 가공
			if(getStatus() == 1 && (reqSealDocList != null && reqSealDocList.size() > 0)) {
				//날인서류 루프
				for(Map<String, Object> reqSealDoc : reqSealDocList) {
					//날인서류 관련 채번 발급
					String smdNo = DocumentUtil.getDocNumber(info, "SMD").result[0];
					reqSealDoc.put("SMR_NUMBER", smrNo);
					reqSealDoc.put("SMD_NUMBER", smdNo);
										
					//인감구분 조합 셋 준비
					Set<String> sealTypeSet = new HashSet<String>();
					
					//날인서류에 사용할 인감 루프
					List<Map<String, String>> selectSealList = (List<Map<String, String>>) reqSealDoc.get("SELECT_SEAL_LIST");
					
					//자동날인에 사용할 적재
					List<Map<String, String>> autoPrintSealDataList = new ArrayList<>();
					for(Map<String, String> selectSeal : selectSealList) {
						selectSeal.put("SMR_NUMBER", smrNo);
						selectSeal.put("SMD_NUMBER", smdNo);
						
						//인감관리 매니저 객체 생성
						SepoaSealManagementManager smm = getSepoaSealManagementManager(newInfo, new Configuration(), ctx);
						
						//인감 데이터 조회 후 인감 데이터가 없다면 continue;
						Map<String, String> sealDetail = smm.doSelectSealData(selectSeal);
						if(sealDetail == null) continue;

						//용도검색 기반일 때, 인감 용도 매핑 테이블 조회후 날인요청의 용도와 일치하지 않는다면 continue;
						if(SepoaModuleService.checkBusiness(new Configuration(), info, "US1637133464814")) {
							selectSeal.put("USE_NUMBER", reqSealInfo.get("REQ_SEAL_USE_TYPE"));
							if(smm.doSelectSealUse(selectSeal) == null) continue;
						} 
						
						//날인요청 구분이 사용인감인데 법인인감을 선택한 경우 continue; 이부분은 고객사마다 달라질 수 있어서 별도 분기처리가 필요할 듯 
						if("U".equals(MapUtils.getString(reqSealInfo, "APPLY_TYPE", "")) && "C".equals(sealDetail.get("SEAL_TYPE"))) continue;
												
						//조회한 SEAL 정보 pullAll	
						selectSeal.putAll(sealDetail);
						//날인서류에 사용할 인감 SSIPD_SEAL에 INSERT하고 결과가 없다면 continue;
						if(srm.doSaveSealDocSeal(selectSeal) == null) continue;
						
						//사용한 인감 구분을 sealTypeSet에 적재
						sealTypeSet.add(sealDetail.get("SEAL_TYPE"));
						
						//자동 날인에 해당되는 문서면 데이터 전달
						if("Y".equalsIgnoreCase(MapUtils.getString(reqSealDoc, "AUTO_PRINT_FLAG", ""))) {
							Map<String, String> autoPrintSealData = new HashMap<>();
							autoPrintSealData.put("MANAGE_NUMBER", selectSeal.get("MANAGE_NUMBER"));
							autoPrintSealData.put("SMR_NUMBER", smrNo);
							autoPrintSealData.put("SMD_NUMBER", smdNo);
							
							autoPrintSealDataList.add(autoPrintSealData);
						}
					}

					//인감구분으로 사용할 값 사이즈 확인
					if(sealTypeSet.size() > 0) {
						//인감구분으로 사용할 값이 확인이 된다면,
						reqSealDoc.put("SEAL_TYPE", combineSealType(sealTypeSet));
					} else {
						//인감구분으로 사용할 값이 확인이 되지 않는다면 날인요청의 법인,사용 구분을 기본값으로 세팅한다.
						reqSealDoc.put("SEAL_TYPE", MapUtils.getString(reqSealInfo, "APPLY_TYPE", ""));
					}
					
					Map<String, String> uploadFileData = new HashMap<String, String>();
					if(new File(String.valueOf(reqSealDoc.get("PATH"))).exists()) {
						String copyFilePath = String.valueOf(reqSealDoc.get("PATH"))+System.nanoTime()+"_"+System.currentTimeMillis();
						FileUtils.copyFile(new File(String.valueOf(reqSealDoc.get("PATH"))), new File(copyFilePath));
						
						uploadFileData.put("PATH", copyFilePath);
						uploadFileData.put("FILE_NAME_CLIENT", String.valueOf(reqSealDoc.get("SOURCE_FILE_NAME")));
						uploadFileData.put("FILE_NAME_SERVER", new File(String.valueOf(reqSealDoc.get("PATH"))).getName());
					} else {
						//파일 존재 유무 검사는 모듈에서 할일이 아니라 해당 서비스에서만 하기때문에 여기서 별도로 처리한다.
						setStatus(102412);
						break;
					}
					
					//날인서류 업로드 진행(컨버팅까지 진행한다.)
					SepoaReqSealManager uploadManager = getSepoaReqSealManager(newInfo, new Configuration(), ctx);
					Map<String, String> parseBasicData = parseReqSealDocData(reqSealDoc); 
					uploadManager.setDocBaiscInfo(parseBasicData);
					if(uploadManager.doUploadDocumentFile("REQ_SEAL", uploadFileData, parseBasicData) == null) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							break;
						}
					}
					
					//AUTO_PRINT_FLAG가 Y일경우 CBO서비스 호출 한 뒤, FIX_HTML업데이트
					if(autoPrintSealDataList.size() > 0 && "Y".equalsIgnoreCase(MapUtils.getString(reqSealDoc, "AUTO_PRINT_FLAG", ""))) {
						SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.SepoaSealAutoPrint", getConnectionContext(), "doAutoPrintSeal", new Object[] {autoPrintSealDataList});
						if(out.status == 1) {
							List<Map<String, String>> fixHtmlData = (List<Map<String, String>>) out.getData(0);
							for(Map<String, String> fixHtml : fixHtmlData) {
								uploadManager.doAppendSealFixHtml(fixHtml);
							}
						}
					}
					
					//날인서류 관리 테이블 적재
					srm.doSaveSealDoc(parseBasicData);
				}
			}
			
			//법인인감이면서 교부서류가 있는 경우 교부서류 데이터 적재
			if(getStatus() == 1 && ("C".equals(reqSealInfo.get("APPLY_TYPE")) && (attachDocList != null && attachDocList.size() > 0))) {
				for(Map<String, String> attachDoc : attachDocList) {
					//실존 하는 교부서류인지 검토
					attachDoc.put("SMR_NUMBER", smrNo);
					Map<String, String> stdAttachDocData = srm.doSelectCorpAttachDocHeader(attachDoc).get(0);
					
					if(stdAttachDocData == null) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					//교부서류 세팅
					stdAttachDocData.putAll(attachDoc);
					if(srm.doSaveCorpAttachDoc(stdAttachDocData) == null) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
			}
			
			//링크 매니저를 통한 링크 발급 진행
			if(getStatus() == 1) {
				String linkType = null;
				Map<String, String> linkParams = new HashMap<String, String>();
				linkParams.put("SMR_NUMBER", smrNo);
				
				if("C".equals(reqSealInfo.get("APPLY_TYPE"))){
					linkType = "CORP.REQ.SEAL";
				} else {
					linkType = "USE.REQ.SEAL";
				}
				
				accessLink = srm.doCreateReqSealAccessLink(linkType, smrNo, linkParams, null, "ALL");
				if(accessLink == null || accessLink.isEmpty()) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
						
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				returnMap.put("KEY", accessLink);
				returnMap.put("DOC_NO", smrNo);
				setValue(returnMap);
				Commit();
			}
			
			if("Y".equals(autoCompleteFlag)) {
				allData.put("externalFlag", "Y");
				allData.put("CURRENT_PRC", "CREATE");
				allData.put("NEXT_PRC", "COMPLETE");
				doRequestServiceAfter(allData, srm);
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	//날인서류 저장 데이터 파싱 Object -> String
	private Map<String, String> parseReqSealDocData(Map<String, Object> reqSealDocData){
		Map<String, String> returnData = new HashMap<String, String>();
		
		for(String key : reqSealDocData.keySet()) {
			if(reqSealDocData.get(key) instanceof String) {
				returnData.put(key, String.valueOf(reqSealDocData.get(key)));
			}
		}
		
		return returnData;
	}
	
	//인감 구분 조합식 (수정여지있음)
	private String combineSealType(Set<String> sealTypeSet) {
		String result = "";
		
		if(sealTypeSet.contains("CU")) {
			result = "CU";
		} else if (sealTypeSet.contains("C")) {
			result = "C";
		} else if (sealTypeSet.contains("UE")) {
			result = "UE";
		} else if (sealTypeSet.contains("U") && sealTypeSet.contains("E")) {
			result = "UE";
		} else if (sealTypeSet.contains("U")) {
			result = "U";
		} else if (sealTypeSet.contains("E")) {
			result = "E";
		}
			
		return result;
	}
	
	@Description("날인서류 파일 수정")
	@SuppressWarnings("unchecked")
	public SepoaOut doUploadDocumentFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
			sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			sm.setDocBaiscInfo(headerData);
			
			Map<String, String> storedFileData = null;
			for(Map<String, String> file : (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA)) {
				storedFileData = sm.doUploadDocumentFile("REQ_SEAL", file, headerData);
			}
			
			if(storedFileData == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				Rollback();
			} else {
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("날인서류 파일 업데이트")
	@SuppressWarnings("unchecked")
	public SepoaOut doDocumentUpdate(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			if(StringUtils.isBlank(headerData.get("SMR_NUMBER")) || StringUtils.isBlank(headerData.get("SMD_NUMBER"))) {
				setStatus(103338);
			}
			
			if(getStatus() == 1 && headerData.containsKey("EDITOR_HTMLS")) {
				Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
				
				SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
				sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				sm.setDocBaiscInfo(headerData);
				
				if(!sm.doUpdateEditorHTML((List<Map<String, String>>) jsonData.get("EDITOR_HTMLS"))) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				Rollback();
			} else {
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("날인서류 파일 삭제")
	@SuppressWarnings("unchecked")
	public SepoaOut doDeleteDocumentFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			if(StringUtils.isBlank(headerData.get("SMR_NUMBER")) || StringUtils.isBlank(headerData.get("SMD_NUMBER"))) {
				setStatus(103338);
			}
			
			SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
			sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			sm.setDocBaiscInfo(headerData);
			
			Map<String, String> map = sm.doSelectDocumentFile();
				
			if (!sm.doDeleteDocumentFile(map)) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
				
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				Rollback();
			} else {
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("아이템 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectSignItem(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
			sm.setDocBaiscInfo(headerData);
			String sealItemStr = sm.doSelectSignItem(headerData);
			if(StringUtils.isBlank(sealItemStr)) {
				setStatus(sm.getLastErrCode());
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				setValue(sealItemStr);
			}
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("이미지 리스트 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doImageList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
			sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			headerData.put("KEY", headerData.get("KEY"));
			headerData.put("LAYER", headerData.get("LAYER"));
			sm.setDocBaiscInfo(headerData);
			
			List<Map<String, String>> fileList =  null;
			List<Map<String, String>> reqList = sm.doSelectRequestList();
			String smrStatus = "T";
			if(reqList != null) {
				smrStatus = MapUtils.getString(reqList.get(0), "SMR_STATUS");
			}
			
			if("REQ_SEAL".equals(headerData.get("LAYER"))) {
				if("C".equals(smrStatus)) {
					String[] convertLayers = {"CONVERT_SEAL_MERGE", "CONVERT_SEAL_ORG"};	
					
					if("1".equals(headerData.get("REAL_FLAG"))) {
						convertLayers = new String[]{"REQ_SEAL"};
					} 
					
					for(String layer : convertLayers) {
						fileList = sm.doListImage(layer);
						if(fileList != null) {
							break;
						}
					}
				} else {
					fileList = sm.doListImage(headerData.get("LAYER"));
				}
			} else {
				fileList = sm.doListImage(headerData.get("LAYER"));
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				setValue(fileList);
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("이미지 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doImage(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
			sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			sm.setDocBaiscInfo(headerData);
			String fileID = MapUtils.getString(headerData, "FILE_ID", "");
			String partID = MapUtils.getString(headerData, "PART_ID", "");
			boolean thumb = "Y".equals(headerData.get("THUMB"));
			boolean noImageFlag = "Y".equals(headerData.get("DEFAULT"));
			Map<String, String> fileList = sm.doImage(fileID, partID, thumb, noImageFlag);
			if(fileList == null) {
				setStatus(sm.getLastErrCode());
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				setValue(fileList);
				addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("다음모듈 조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectNextModule(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, String> nextModuleMap = new HashMap<String, String>();
			nextModuleMap = this.doFindNextModule(headerData);
			
			if(nextModuleMap == null) {
				setStatus(103339);
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				setValue(new Gson().toJson(nextModuleMap));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("날인신청현황 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectRequestList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			String mode = MapUtils.getString(headerData, "MODE", null);
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			Configuration config = new Configuration();
			String systemAdminMenuProfilecode = CommonUtil.getConfig("sepoa.system.manager.profile.code");	// 관리자
			String managerMenuProfilecode = CommonUtil.getConfig("sepoa.seal.manager.porfile.code");	// 인감담당자
			String ctrlCode = info.getSession("CTRL_CODE").replaceAll("\\s", "");
			List<String> ctrlCodeList = Arrays.asList(ctrlCode.split(","));
			headerData.put("F_KEYWORD", MapUtils.getString(headerData, "F_KEYWORD", "").trim()); 
			
			if(StringUtils.isNotBlank(mode)) {
				if("LIST".equals(mode) || "LIST_COUNT".equals(mode)) {
					boolean viewSameDept = SepoaModuleService.checkBusiness(config, info, "SRL1655791676756");	// 같은 부서원의 신청내역을 볼 수 있는지
					if (!(myMenuProfile.equals(systemAdminMenuProfilecode) || ctrlCodeList.contains("SEAL_CONFIRM"))) {	// 관리자거나 날인승인권한이 아니면
						if (viewSameDept) {
							headerData.put("REQ_USER_DEPT", info.getSession("DEPARTMENT"));
						} else {
							headerData.put("REQ_USER_ID", info.getSession("ID"));
						}
					}
				} else if ("TODO".equals(mode) || "TODO_COUNT".equals(mode)) {
					if (myMenuProfile.equals(systemAdminMenuProfilecode)) {	// 관리자
						headerData.put("SMR_STATUS", "G,W");
					} else if (ctrlCodeList.contains("SEAL_CONFIRM")) {	// 날인승인권한
						headerData.put("SMR_STATUS", "G,W");
					} else if (myMenuProfile.equals(managerMenuProfilecode)) {	// 인감담당자
						headerData.put("SMR_STATUS", "W");	// 인감담당자 승인 진행중
						headerData.put("MY_SEAL", "true");	// 본인 소유의 인감만 조회
						headerData.put("ACCEPT_FLAG", "N");	// 승인하지 않은 인감만 조회
					} else {
						if ("TODO_COUNT".equals(mode)) {
							setValue(-1);
							return getSepoaOut();
						} else {
							int errCode = 200051;	// 권한이 없는 사용자입니다.
							setStatus(errCode);
							throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+errCode));
						}
					}
				}
			}
			
			if(getStatus() != 1) {
				throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
			srm.setDocBaiscInfo(headerData);
			List<Map<String, String>> resultList = srm.doSelectRequestList();
			
			if(resultList == null) {
				int errCode = srm.getLastErrCode();
				if (errCode == 102007 && ("LIST_COUNT".equals(mode) || "TODO_COUNT".equals(mode))) {
					setStatus(1);
					setValue(0);
					return getSepoaOut();
				} else if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				if ("LIST_COUNT".equals(mode) || "TODO_COUNT".equals(mode)) {
					setValue(resultList.size());
				} else {
					setValue(resultList);
				}
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("요청내역 상태값별 카운트")
	@SuppressWarnings("unchecked")
	public SepoaOut doCountRequestList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaOut so = null;
		ConnectionContext ctx = null;
		
		try {
			ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			String[] statusList = headerData.get("REQ_SEAL_STATUS_LIST").split(",");
			Map<String, Integer> countMap = new HashMap<String, Integer>();
			Map<String, Object> paramMap = new HashMap<String, Object>();
			
			for (String status : statusList) {
				Map<String, String> paramHeader = new HashMap<String, String>();
				paramMap.put(SepoaDataMapper.KEY_HEADER_DATA, paramHeader);
				paramHeader.putAll(headerData);
				paramHeader.put("MODE", "LIST_COUNT");
				paramHeader.put("SMR_STATUS", status);
				so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaReqSealService", ctx, "doSelectRequestList", new Object[] {paramMap});
				int listSize = Integer.parseInt(so.result[0]);
				countMap.put("REQ_SEAL_COUNT_"+status, listSize);
			}

			Map<String, String> paramHeader = new HashMap<String, String>();
			paramMap.put(SepoaDataMapper.KEY_HEADER_DATA, paramHeader);
			paramHeader.put("MODE", "TODO_COUNT");
			so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaReqSealService", ctx, "doSelectRequestList", new Object[] {paramMap});
			int todoSize = Integer.parseInt(so.result[0]);
			countMap.put("REQ_SEAL_COUNT_TODO", todoSize);
			
			if(getStatus() == 1) {
				setValue(countMap);
				setMessage("");
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("교부서류 현황 리스트 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectCorpAttachDocHeader(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			srm.setDocBaiscInfo(headerData);
			List<Map<String, String>> resultList = srm.doSelectCorpAttachDocHeader(headerData);
			
			if(resultList == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				sf = new SepoaFormater(resultList);
				if(sf.getRowCount() > 0) {
					resultList = new SepoaFormater(resultList).getListMap();
				}
				
				setValue(resultList);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("교부서류 저장 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSaveCorpAttachDocHeader(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			
			for(Map<String, String> rows : gridData) {
				if(!srm.doSaveCorpAttachDocHeader(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("교부서류 삭제 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doDeleteCorpAttachDocHeader(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			for(Map<String, String> rows : gridData) {
				if(!srm.doDeleteCorpAttachDocHeader(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("법인인감 정보 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectBasicCorpSeal(Map<String, Object> allData) {
		// 법인인감 날인요청의 경우 기본적으로 법인인감의 정보가 날인서류에 적재되어야 하기때문에 법인인감 정보조회
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, new Configuration(), getConnectionContext());
			headerData.put("SEAL_TYPE", "C");
			List<Map<String, String>> searchResult = sm.doSelectSealList(headerData);
			
			if(searchResult == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				setValue(new Gson().toJson(searchResult));
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("날인요청 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectRequestInfo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, Object> resultMap = new HashMap<String, Object>();
			List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
			List<Map<String, String>> reqDocList = new ArrayList<Map<String, String>>();
			List<Map<String, String>> reqList = new ArrayList<Map<String, String>>();
			List<Map<String, String>> sealList = new ArrayList<Map<String, String>>();
			List<Map<String, String>> attachList = new ArrayList<Map<String, String>>();
			Map<String, String> reqInfo = new HashMap<String, String>();
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			String applyType = headerData.get("APPLY_TYPE");
			srm.setDocBaiscInfo(headerData);
			// 날인요청 정보조회
			reqList = srm.doSelectRequestList();
			if(reqList == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				reqInfo = reqList.get(0);
				resultMap.put("REQ_SEAL_INFO", reqInfo);
			}
			if(getStatus() == 1 && reqInfo != null) {
				// 날인서류 조회
				reqDocList = srm.doSelectReqSealDoc(headerData);
				if(reqDocList != null) {
					for(Map<String, String> rows : reqDocList) {
						// 날인서류에 쓰인 인감 조회
						List<Map<String, String>> docSealList = srm.doSelectReqSealDocSeal(rows);
						for(Map<String, String> sealRow : docSealList) {
							sealList.add(sealRow);
						}
					}
					resultMap.put("DOCUMENT_LIST", reqDocList);
					resultMap.put("SELECTED_SEAL_LIST", sealList);
				} 
				//if("C".equals(applyType)) {
					attachList = srm.doSelectCorpAttachDoc(headerData);
					if(attachList != null) {
						resultMap.put("ATTACH_DOCUMENT_LIST", attachList);
					}
				//}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				resultList.add(resultMap);
				setValue(new Gson().toJson(resultList));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("날인요청 회수")
	@SuppressWarnings("unchecked")
	public SepoaOut doWithdrawRequest(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			for(Map<String, String> rows : gridData) {
				srm.setDocBaiscInfo(rows);
				
				// 회수 가능한 상태인지 확인
				if(!srm.doCheckReqSealWithdrawable(rows.get("SMR_NUMBER"))) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 날인요청 회수
				if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus("T")) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 결재 종료
				if(getStatus() == 1 && !srm.doDeleteAppInfo(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("날인요청 삭제")
	@SuppressWarnings("unchecked")
	public SepoaOut doDeleteRequestInfo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			for(Map<String, String> rows : gridData) {
				srm.setDocBaiscInfo(rows);
				// 날인요청 삭제
				if(!srm.doDeleteReqSeal(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 날인요청 - 날인서류 삭제
				if(getStatus() == 1 &&!srm.doDeleteReqSealDoc(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 날인요청 - 날인서류인장 삭제
				if(getStatus() == 1 && !srm.doDeleteReqSealDocSeal(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 날인요청 - 교부서류 삭제
				if(getStatus() == 1 && !srm.doDeleteReqSealAttachDoc(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			Rollback();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("교부서류 파일 업로드")
	@SuppressWarnings("unchecked")
	public SepoaOut doUploadAttachDocumentFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
			sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			sm.setDocBaiscInfo(headerData);
			Map<String, String> stdDocumentList = sm.doSelectAttachDocumentFile();
			if(stdDocumentList != null) {
				sm.doDeleteDocumentFile(stdDocumentList);
			}
			
			Map<String, String> storedFileData = null;
			for(Map<String, String> file : (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA)) {
				storedFileData = sm.doUploadAttachDocumentFile("SEAL_ATC", file, headerData);
			}
			
			if(storedFileData == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				Rollback();
			} else {
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("교부서류 다운로드")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doAttachDocumentDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaReqSealManager srm = getSepoaReqSealManager(info, config, getConnectionContext());
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			srm.setDocBaiscInfo(headerData);
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("교부서류 처리대상 목록")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doSelectTodoAttachDocList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			
			setValue(srm.doSelectTodoAttachDocList(headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("관리대장 리스트 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectLedgerList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			headerData.put("SMR_STATUS", "C");
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			
			if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {	// 관리자
				headerData.put("MANAGER_ID", "");
				headerData.put("CONFIRM_USER_ID", "");
			} else if (info.getSession("CTRL_CODE").contains("SEAL_CONFIRM")) {	// 인감책임자
				headerData.put("CONFIRM_USER_ID", "");
				headerData.put("MANAGER_ID", "");
			} else if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.seal.manager.porfile.code"))) {	// 인감담당자
				headerData.put("CONFIRM_USER_ID", info.getSession("ID"));
				headerData.put("MANAGER_ID", info.getSession("ID"));
				headerData.put("MY_SEAL", "true");	// 본인 소유의 인감만 조회
				headerData.put("ACCEPT_FLAG", "N");	// 승인하지 않은 인감만 조회
			} else {
				int errCode = 200051;	// 권한이 없는 사용자입니다.
				setStatus(errCode);
				throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+errCode));
			}
			
			List<Map<String, String>> resultList = srm.doLedgerList(headerData);
			
			if(getStatus() == 1 && resultList != null) {
				sf = new SepoaFormater(resultList);
				if(sf.getRowCount() > 0) {
					resultList = new SepoaFormater(resultList).getListMap();
				}
				
				setValue(resultList);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("편집모듈 파일 다운로드 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doDocumentDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();		
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			
			Map<String, String> paramMap = new HashMap<String, String>();
			String key = headerData.get("KEY");
			String downTarget = headerData.get("TARGET");
			
			List<Map<String, String>> docList = new ArrayList<Map<String, String>>();
			paramMap.put("SMR_NUMBER", headerData.get("SMR_NUMBER"));
			
			srm.setDocBaiscInfo(headerData);
			// 날인요청 정보조회
			List<Map<String, String>> reqList = srm.doSelectRequestList();
			if(reqList == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				srm.setDocBaiscInfo(reqList.get(0));
			}
			
			switch(downTarget.toUpperCase()) {
				case "REQ_SEAL":
					paramMap.put("SMD_NUMBER", headerData.get("SMD_NUMBER"));
					boolean checkYn = true;
					if (SepoaModuleService.checkBusiness(new Configuration(), info, "US1638852635132")) {
						checkYn = srm.doCheckFileDownload(paramMap);
					}
					
					if(checkYn) {
						docList = srm.doSelectReqSealDoc(paramMap);
					} else {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							setFlag(false);
							setValue("");
							return getSepoaOut();
						}
					}
					break;
				case "SEAL_ATC":
					paramMap.put("SD_NUMBER", headerData.get("SD_NUMBER"));
					docList = srm.doSelectCorpAttachDoc(paramMap);
					break;
			}
			
			if(docList == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				Map<String, String> fileInfo = srm.doDocumentDownload(downTarget, docList, true, key);
				if(fileInfo == null) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				} else {
					Boolean res = srm.addDownloadCount(docList);
					if (res) {
						setValue(fileInfo);
						addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
					} else {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setValue("");
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
			
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@Description("PDF 뷰어 연동 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doLoadPdf(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();		
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
			srm.setDocBaiscInfo(headerData);
			
			byte[] pdfBytes = srm.makeAuditTrailCertificate();
			if(pdfBytes == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				setObject(pdfBytes);
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}

	@Description("날인요청 결재진행")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalProgress(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			
			SepoaReqSealManager srm = getSepoaReqSealManager(info, config, ctx);
			
			// 인장 치환 처리
			Map<String, String> sealInfo = new HashMap<String, String>();
			sealInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
			srm.setDocBaiscInfo(sealInfo);

			String docStatus = "I";
			
			// 날인요청 Status 업데이트
			if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus(docStatus)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@Description("날인요청 결재완료처리 준비")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalConfirmReady(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		Config config;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			config = new Configuration();
			
			SepoaReqSealManager srm = getSepoaReqSealManager(info, config, ctx);
			
			// 인장 치환 처리
			Map<String, String> sealInfo = new HashMap<String, String>();
			String smrNumber = headerData.get("SUX_WKEY1");
			sealInfo.put("SMR_NUMBER", smrNumber);
			sealInfo.put("ACCEPT_REASON", headerData.get("ACCEPT_REASON"));
			sealInfo.put("CONFIRM_DATE", SepoaDate.getShortDateString());
			sealInfo.put("CONFIRM_TIME", SepoaDate.getShortTimeString());
			sealInfo.put("CONFIRM_USER_ID", info.getSession("ID"));				
			sealInfo.put("RE_FLAG", headerData.get("RE_FLAG"));
			sealInfo.put("SMR_STATUS", "CR");	// 결재완료처리대기
			
			srm.setDocBaiscInfo(sealInfo);
			 
			// 날인요청 Status 업데이트
			if(getStatus() == 1 && !srm.doUpdateReqSealDoc(sealInfo)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			// 결재 후처리 목록에 Insert
			sealInfo.put("COMPANY_CODE", headerData.get("COMPANY_CODE"));
			sealInfo.put("APP_NO", MapUtils.getString(headerData, "APP_NO", ""));
			sealInfo.put("APP_NO_SEQ", MapUtils.getString(headerData, "APP_NO_SEQ", ""));
			sealInfo.put("APP_LINE_SEQ", MapUtils.getString(headerData, "APP_LINE_SEQ", ""));
			sealInfo.put("POSTPROCESSED", "N");	// 후처리여부 (N미처리 Y처리 Null처리불필요)
			sealInfo.put("APP_COMMENT", headerData.get("ACCEPT_REASON"));
			String appType = MapUtils.getString(headerData, "APP_TYPE", null);
			if (appType == null) {
				sealInfo.put("APPLY_FOR", "RS");	// 날인요청
			} else if ("J01".equals(appType)) {	// 법인인감날인요청
				sealInfo.put("APPLY_FOR", "CS");
			} else if ("J02".equals(appType)) {	// 사용인감날인요청
				sealInfo.put("APPLY_FOR", "US");
			}
			
			
			if(getStatus() == 1 && !srm.doAddPostProcessLine(sealInfo)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage("["+smrNumber+"] "+Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	@Description("증명서 발급 결재완료")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalCertConfirm(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		Config config;
		PathManager pm;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			config = new Configuration();
			pm = new PathManager(config);
			
			if(SepoaApproval.APP_STATUS_FINAL.equals(MapUtils.getString(headerData, "APP_STATUS", ""))){
				SepoaReqSealManager srm = getSepoaReqSealManager(info, config, ctx);
				
				// 인장 치환 처리
				Map<String, String> sealInfo = new HashMap<String, String>();
				String smrNumber = headerData.get("SUX_WKEY1");
				sealInfo.put("SMR_NUMBER", smrNumber);
				sealInfo.put("ACCEPT_REASON", headerData.get("ACCEPT_REASON"));
				sealInfo.put("CONFIRM_DATE", SepoaDate.getShortDateString());
				sealInfo.put("CONFIRM_TIME", SepoaDate.getShortTimeString());
				String confirmUserIdForced = MapUtils.getString(headerData, "CONFIRM_USER_ID_FORCED", null);
				sealInfo.put("CONFIRM_USER_ID", confirmUserIdForced == null ? info.getSession("ID") : confirmUserIdForced);				

				//날인완료 히스토리 적재
				if(getStatus() == 1) {
					if(!srm.doAddProcessHistory("CERT_COMPLETE")) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				String finalDocStatus = "C";
				sealInfo.put("SMR_STATUS", finalDocStatus);
				// 날인요청 Status 업데이트
				if(getStatus() == 1 && !srm.doUpdateReqSealDoc(sealInfo)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				if(getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage("["+smrNumber+"] "+Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				} else {
					Commit();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		} 
		
		return getSepoaOut();
	}
	@Description("날인요청 결재완료 후처리")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalConfirmPostprocess(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		Config config;
		PathManager pm;
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
		String house_code = headerData.get("HOUSE_CODE");
		
		try {
			config = new Configuration();
			pm = new PathManager(config);
			
			SepoaReqSealManager srm = getSepoaReqSealManager(info, config, ctx);
			
			// 인장 치환 처리
			Map<String, String> sealInfo = new HashMap<String, String>();
			String company_code = headerData.get("COMPANY_CODE");
			String smrNumber = headerData.get("SMR_NUMBER");
			sealInfo.put("HOUSE_CODE", house_code);
			sealInfo.put("COMPANY_CODE", company_code);
			sealInfo.put("SMR_NUMBER", smrNumber);
			sealInfo.put("RE_FLAG", MapUtils.getString(headerData, "RE_FLAG", ""));
			srm.setDocBaiscInfo(sealInfo);
			 
			pm.setLayer(PathManager.LAYER_TEMP);
			String tempFilePath = pm.getUniquePath();
			
			allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, tempFilePath);
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			
			if(!srm.doConvertReqSealDocument()) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			//날인완료 히스토리 적재
			if(getStatus() == 1) {
				if(!srm.doAddProcessHistory("SEAL_COMPLETE")) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			String finalDocStatus = "C";
			
			// 날인요청 Status 업데이트
			if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus(finalDocStatus)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage("["+smrNumber+"] "+srm.getThisSessionText(house_code, "POA_CONTRACT", "STATUS_"+String.valueOf(this.getStatus()), null));
			} else {
				// 요청자에게 결재완료 메일 발송
				Boolean mailResult = srm.sendCompleteMailToRequester();
				
				Commit();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		} finally {
			File fileTempDirectory = new File((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			try {
				if(fileTempDirectory.exists()) {
					FileUtils.forceDelete(fileTempDirectory);
				}
			} catch (IOException e) {
				Logger.debug.println("temp file delete : fail" + fileTempDirectory.getAbsolutePath());
			}
		}
		
		return getSepoaOut();
	}
	

    @Description("날인요청 결재완료")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalConfirm(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		Config config;
		PathManager pm;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			config = new Configuration();
			pm = new PathManager(config);
			
			if(SepoaApproval.APP_STATUS_FINAL.equals(MapUtils.getString(headerData, "APP_STATUS", ""))){
				SepoaReqSealManager srm = getSepoaReqSealManager(info, config, ctx);
				
				// 인장 치환 처리
				Map<String, String> sealInfo = new HashMap<String, String>();
				String smrNumber = headerData.get("SUX_WKEY1");
				sealInfo.put("SMR_NUMBER", smrNumber);
				sealInfo.put("ACCEPT_REASON", headerData.get("ACCEPT_REASON"));
				sealInfo.put("CONFIRM_DATE", SepoaDate.getShortDateString());
				sealInfo.put("CONFIRM_TIME", SepoaDate.getShortTimeString());
				String confirmUserIdForced = MapUtils.getString(headerData, "CONFIRM_USER_ID_FORCED", null);
				//sealInfo.put("LEGAL_WATERMARK", headerData.get("LEGAL_WATERMARK")); 하단 워터마크
				sealInfo.put("CONFIRM_USER_ID", confirmUserIdForced == null ? info.getSession("ID") : confirmUserIdForced);				
				sealInfo.put("RE_FLAG", headerData.get("RE_FLAG"));
				srm.setDocBaiscInfo(sealInfo);
				 
				pm.setLayer(PathManager.LAYER_TEMP);
				String tempFilePath = pm.getUniquePath();
				
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, tempFilePath);
				srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				
				if(!srm.doConvertReqSealDocument()) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				//날인완료 히스토리 적재
				if(getStatus() == 1) {
					if(!srm.doAddProcessHistory("SEAL_COMPLETE")) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				String finalDocStatus = "C";
				
				// 날인요청 Status 업데이트
				if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus(finalDocStatus)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				if(getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage("["+smrNumber+"] "+Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				} else {
					Commit();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		} finally {
			File fileTempDirectory = new File((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			try {
				if(fileTempDirectory.exists()) {
					FileUtils.forceDelete(fileTempDirectory);
				}
			} catch (IOException e) {
				Logger.debug.println("temp file delete : fail" + fileTempDirectory.getAbsolutePath());
			}
		}
		
		return getSepoaOut();
	}
	@Description("날인요청 결재반려")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalReject(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			
			SepoaReqSealManager srm = getSepoaReqSealManager(info, config, ctx);
			
			// 인장 치환 처리
			Map<String, String> sealInfo = new HashMap<String, String>();
			sealInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
			sealInfo.put("ACCEPT_REASON", headerData.get("ACCEPT_REASON"));
			String confirmUserIdForced = MapUtils.getString(headerData, "CONFIRM_USER_ID_FORCED", null);
			sealInfo.put("CONFIRM_USER_ID", confirmUserIdForced == null ? info.getSession("ID") : confirmUserIdForced);
			srm.setDocBaiscInfo(sealInfo);

			String docStatus = "R";
			
			// 날인요청 Status 업데이트
			if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus(docStatus)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	@Description("날인요청 회수")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalWithdraw(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			
			SepoaReqSealManager srm = getSepoaReqSealManager(info, config, ctx);
			
			// 인장 치환 처리
			Map<String, String> sealInfo = new HashMap<String, String>();
			sealInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
			srm.setDocBaiscInfo(sealInfo);

			String docStatus = "T";
			
			// 날인요청 Status 업데이트
			if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus(docStatus)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@Description("구룹웨어 요청내역 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doGroupWareRequestList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			if(StringUtils.isNotBlank(headerData.get("MODE"))) {
				if("LIST".equals(headerData.get("MODE"))) {
					headerData.put("REQ_USER_ID", info.getSession("ID"));
				}
			}
			srm.setDocBaiscInfo(headerData);
			List<Map<String, String>> resultList = srm.doGroupWareRequestList();
			
			if(resultList == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				sf = new SepoaFormater(resultList);
				if(sf.getRowCount() > 0) {
					resultList = new SepoaFormater(resultList).getListMap();
				}
				
				setValue(resultList);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("그룹웨어 파일 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doQueryAttach(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			SepoaFormater sf = null;
			
			SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
			sf = new SepoaFormater(sm.getAttachInfo(headerData));
			SepoaFormater returnSF = null;
			
			if(sf.getRowCount() > 0) {
				List<Map<String, String>> attachList = sf.getListMap();
				/*if(config.getBoolean("sepoa.skdnd.attachId.encrypt", false)) {
					Cipher cipher = Cipher.getInstance("AES");
					SecretKeySpec secretKey = new SecretKeySpec("SKDnDEasySealKey".getBytes(), "AES");
					cipher.init(Cipher.ENCRYPT_MODE, secretKey);

					for(Map<String, String> attach : attachList) {
						String attachId = Base64.getEncoder().encodeToString(cipher.doFinal(MapUtils.getString(attach, "ATTACHID").getBytes()));
						attach.put("ATTACHID", attachId);
					}
				}*/
				returnSF = new SepoaFormater(attachList);
			}

			setValue(returnSF);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("반출된 인감인지 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doSearchExtUse(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());

		/*	Map<String, String> resultMap = new HashMap<String, String>();
			resultMap.put("MANAGE_NUMBER", headerData.get("PARAMETER"));

			String resultList = srm.doSearchExtUse(resultMap);
			if(getStatus() == 1) {
				sf = new SepoaFormater(resultList);
				if(sf.getRowCount() > 0) {
					setValue(sf);
				}
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}*/
			String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
			Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
			Map<String, String> resultMap = new HashMap<String, String>();
			List<Map<String, Object>> documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
			for(Map<String, Object> sealDoc : documentList) {
				List<Map<String, String>> selectSealList = (List<Map<String, String>>) sealDoc.get("SELECT_SEAL_LIST");
				for(Map<String, String> selectedSeal : selectSealList) {
					resultMap.put("MANAGE_NUMBER", selectedSeal.get("MANAGE_NUMBER"));
					String resultList = srm.doSearchExtUse(resultMap);
					
					if(getStatus() == 1) {
						sf = new SepoaFormater(resultList);
						if(sf.getRowCount() > 0) {
							setValue(sf);
						}		
					} else {
						setFlag(false);
						setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("법인인감 정보 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut searchCorpSealAjax(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			//Map<String, String> resultData = new HashMap<String, String>();
			String resultData = srm.doSearchCorpSeal(headerData);
			if(getStatus() == 1) {
				setValue(resultData);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("날인서류목록 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectDocumentList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = new SepoaFormater(srm.doSelectReqSealDoc(headerData));
			if(getStatus() == 1) {
				setValue(sf);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("교부서류 정보조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doCorpDocInfo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = new SepoaFormater(srm.doSelectCorpAttachDocHeader(headerData));
			if(getStatus() == 1) {
				setValue(sf);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}	
	
	@Description("QR링크 접속 로깅")
	@SuppressWarnings("unchecked")
	public SepoaOut addQR_accessLog(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			String linkCode = headerData.get("LINK_CODE");
			sxp = new SepoaXmlParser(this, "getQRLinkParams");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			if (sf.getRowCount() == 0) {
				throw new Exception("잘못된 링크입니다");
			}
			
			String no = System.nanoTime()+"_"+linkCode;
			headerData.put("NO", no);
			
			String house_code = sf.getValue("HOUSE_CODE", 0);
			String company_code = sf.getValue("USER_COMPANY_CODE", 0);
			headerData.put("HOUSE_CODE", house_code);
			headerData.put("COMPANY_CODE", company_code);
			String extraParam = sf.getValue("EXTRA_PARAM", 0);
			String[] extraParamArr = extraParam.split("&");
			for (String kv : extraParamArr) {
				String[] kvArr = kv.split("=");
				if (kvArr.length != 2) continue;
				headerData.put(kvArr[0], kvArr[1]);
			}
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(headerData);
			
			Commit();
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("FMFI 테이블의 실제데이터 조회 ")
	@SuppressWarnings("unchecked")
	public SepoaOut doSearchFMFI(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			boolean reasultData = srm.doSearchFMFI(headerData);
			if(getStatus() == 1 && reasultData == true) {
				setValue(true);
			} else {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}	
}