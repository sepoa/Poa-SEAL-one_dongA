package sepoa.svc.seal.cbo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
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
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.authority.Permission.PrincipalAgentType;
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;
import sepoa.seal.SepoaExtSealManager;
import sepoa.seal.SepoaSealManagementManager;
import sepoa.svc.seal.SepoaSealService;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자인장 - 사외반출 요청 모듈")
public class SepoaReqExternalService extends SepoaSealService{ 
	final static String USE_EXT_CREATE = "ET";
	
	public SepoaReqExternalService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	protected String getClassName() {
		return this.getClass().getName();
	}
	
	protected SepoaExtSealManager getSepoaExtSealManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		if(this.getClassName().startsWith("sepoa.svc.sealmk2")) {
			return new sepoa.sealmk2.SepoaExtSealManager(info, config, ctx);
		} else {
			return new SepoaExtSealManager(info, config, ctx);
		}
	}
	
	protected SepoaSealManagementManager getSepoaSealManagementManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		if(this.getClass().getName().startsWith("sepoa.svc.sealmk2")) {
			return new sepoa.sealmk2.SepoaSealManagementManager(info, config, ctx);
		} else {
			return new SepoaSealManagementManager(info, config, ctx);
		}
	}
	
	@Description("사외반출 요청등록 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doRequestService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = null;
		
		try {
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Configuration conf = new Configuration();
			String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
			
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
			String attachNo ="";
			String count ="";
			boolean doAfterFlag = false;
			boolean isGrant = false;
			SepoaExtSealManager srm = getSepoaExtSealManager(info, config, getConnectionContext());
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			Map<String, String> reqExtInfo = (Map<String, String>)parseMap.get("REQ_EXT_INFO");
			
			if(StringUtils.isNotBlank(currentPrc)) {
				// 반출요청 객체 정보
				srm.setDocBaiscInfo(reqExtInfo);
				// 반출요청 서류 문서 리스트
				List<Map<String, Object>> documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
				// 교부서류 문서 리스트
				List<Map<String, String>> attachDocumentList = (List<Map<String, String>>)parseMap.get("ATTACH_DOCUMENT_LIST");
				switch (currentPrc) {
				case "CREATE":
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					applyType = reqExtInfo.get("APPLY_TYPE");
					// 반출요청 객체 Setting
					Map<String, String> paramMap = new HashMap<String, String>();
					paramMap.put("SMR_NUMBER", smrNumber);
					paramMap.put("APPLY_TYPE", applyType);
					if(srm.doSelectRequestList(paramMap) != null) {
						// 동일한 채번의 반출요청이 존재할 시
						if(!srm.doUpdateReqExtDoc(reqExtInfo)) {
							// 반출요청 Table Update
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					} else {
						// 반출요청 객체 처리 Table Insert
						if(srm.doCreateReqExtSeal() == null) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						} else {
							if(!srm.doAddProcessHistory("EXT_REQUEST")) {
								int errCode = srm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}
					}
					
					// 반출요청 - 반출서류 데이터
					if(getStatus() == 1 && (documentList != null && documentList.size() > 0)) {
						
						List<String> delRow_list = (List<String>) documentList.get(0).get("DELROW_DATA");
						if(delRow_list != null && delRow_list.size() > 0) {
							for(String selectedRow : delRow_list) {
								Map<String, String> delRow = new HashMap<String, String>(); 
								delRow.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
								delRow.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
								delRow.put("SMR_NUMBER", smrNumber);
								delRow.put("SMD_NUMBER", selectedRow);
								
								if(!srm.doDeleteReqExtDoc(delRow)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
								
								if(!srm.doDeleteReqExtDocSeal(delRow)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
								
							}
						}
						
						for(Map<String, Object> extDoc : documentList) {
							smdNumber = String.valueOf(extDoc.get("SMD_NUMBER"));
							extDoc.put("SMR_NUMBER", smrNumber);
							
							List<Map<String, String>> selectSealList = (List<Map<String, String>>) extDoc.get("SELECT_EXT_LIST");
							if(selectSealList != null && selectSealList.size() > 0) {
								for(Map<String, String> selectedSeal : selectSealList) {
									// 날인서류 인장에 필요한 데이터 셋팅 / 날인요청번호 / 날인서류번호
									selectedSeal.put("SMR_NUMBER", smrNumber);
									selectedSeal.put("SMD_NUMBER", smdNumber);
									selectedSeal.put("TEMP_FLAG", "Y");// 그룹웨어 결재요청 완료 후 콜백에서만 COMPLETE_FLAG ="N" 으로 변경하기 위해 여기선 무조건 Y 
									if(srm.doSelectReqSealDocSeal(selectedSeal) != null) {
										// 반출 예상 서류 인장 Update
										if(!srm.doUpdateSealDocSeal(selectedSeal)) {
											int errCode = srm.getLastErrCode();
											if(errCode != 0) {
												setStatus(errCode);
											}
										}
									} else {
										// 반출 예상 서류 인장 데이터 INSERT
										if(srm.doSaveSealDocSeal(selectedSeal) == null) {
											int errCode = srm.getLastErrCode();
											if(errCode != 0) {
												setStatus(errCode);
											}
										}
									}
								}
							}
							//사외반출 예상서류 갯수 파악
							if(srm.doSelectReqExtDoc(parseReqSealDocData(extDoc)) != null) {
								// 반출 예상 서류 데이터 Update
								if(!srm.doUpdateExtDoc(parseReqSealDocData(extDoc))) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							} else {
								// 반출 예상 서류 데이터 INSERT
								if(srm.doSaveExtDoc(parseReqSealDocData(extDoc)) == null) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
						}
						
					}else if(getStatus() == 1 && (documentList != null && documentList.size() == 0)) {
						Map<String, Object> extDoc = new HashMap<String, Object>();
						//smdNumber = String.valueOf(extDoc.get("SMD_NUMBER"));
						extDoc.put("SMR_NUMBER", smrNumber);
						//사외반출 예상서류 갯수 파악
						if(srm.doSelectReqExtDoc(parseReqSealDocData(extDoc)) != null) {
							// 반출 예상 서류 데이터 Update
							if(!srm.doDeleteReqExtDoc(parseReqSealDocData(extDoc))) {
								int errCode = srm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}
					}
					status = "T";
					break;
				case "APPROVAL":
					// 결재모듈 호출
					ctx = getConnectionContext();
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
					headerData.put("APP_TYPE", reqExtInfo.get("APP_TYPE"));
					
					// 결재
					// ※※※ 결재라인이 들어가는 페이지에 대해서는 아래 서비스들을 모두 호출해 주어야 한다. 제거 금지!!!
		            // SepoaApprovalLineSetting
		            // SepoaApprovalProgress
		            // SepoaApprovalConfirm
		            // 각각의 생성자에 ctx를 넘겨줄 경우 Connection관리는 업무에서 처리되며,
		            // 넘겨주지 않을경우 결재모듈에서 자체 커밋한다. 해당부분에 대해서는 데이타 처리에 따라 알맞게 처리하면 된다.
		            // 결재선 저장 시작 -----------------------------------------------------------------------------
					Logger.debug.println("==APP_TYPE="+MapUtils.getString(reqExtInfo, "APP_TYPE"));
					Logger.debug.println("==APP_STATUS="+MapUtils.getString(reqExtInfo, "APP_STATUS" , ""));
		            headerData.put("SUX_WKEY1", smrNumber);
		            headerData.put("SUX_WKEY2", currentPrc);
		            SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
		            sepoaApprovalLineSetting.doApprovalProcess(allData);
		            
		            if(!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_EXT", smrNumber, null);
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
				case "MANAGER":
					applyType = reqExtInfo.get("APPLY_TYPE");
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					confirmStatus = reqExtInfo.get("CONFIRM_STATUS");
					
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
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_EXT", smrNumber, null);
					}
					/*
					isGrant = srm.grantTo(PrincipalAgentType.GROUP, info.getSession("COMPANY_CODE"), CommonUtil.getConfig("sepoa.rm.dept.code", "999"), Authorized.WRITE);
					if(getStatus() == 1 && !isGrant) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					*/
					if("MANAGER_REQUEST".equals(confirmStatus)) {
						status = "W";
						
						Map<String, String> parameter = new HashMap<String, String>();
						smrNumber = reqExtInfo.get("SMR_NUMBER");
						parameter.put("SMR_NUMBER", smrNumber);
						
						// 요청시에 인감담당자한테 권한을 줘야한다.
						if(documentList != null && documentList.size() > 0) {
							for(Map<String, Object> sealDoc : documentList) {
								List<Map<String, String>> selectSealList = (List<Map<String, String>>) sealDoc.get("SELECT_EXT_LIST");
								for(Map<String, String> selectedSeal : selectSealList) {
									isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), selectedSeal.get("SEAL_MG_USER_ID"), Authorized.WRITE);
									if(getStatus() == 1 && !isGrant) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
									if(srm.doSelectReqSealDocSeal(parameter).size() > 0) {
										selectedSeal.put("ACCEPT_FLAG", "N");
										selectedSeal.put("COMPLETE_FLAG", "N");
										selectedSeal.put("MANAGE_NUMBER", selectedSeal.get("MANAGE_NUMBER"));
										selectedSeal.put("SMR_NUMBER", smrNumber);
										selectedSeal.put("SMD_NUMBER", selectedSeal.get("SMD_NUMBER"));
										if(!srm.doUpdateSealDocSeal(selectedSeal)) {
											int errCode = srm.getLastErrCode();
											if(errCode != 0) {
												setStatus(errCode);
											}
										}
									};
									
									//메일 셋팅
									Id_data.add(selectedSeal.get("SEAL_MG_USER_ID"));
				                	mailData.put("SEAL_MG_USER_ID", selectedSeal.get("SEAL_MG_USER_ID"));
				                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REQUEST"));
				                	mailData.put("REQUESTER", info.getSession("NAME_LOC"));
				                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_PROGRESS"));
				                	mailData.put("MESSAGE_TYPE", "EXTERNAL_REQ");
									
								}
							}
						}
						
					} else if("MANAGER_ACCEPT".equals(confirmStatus)) {
						// 승인시에 반복문을 사용하여 인감관리자의 다른 승인이 남았는지 체크
						// 인감관리자의 승인이 남은 날인요청의 경우에는 status를 변경하지 않는다.
						// 먼저 본인 인감 승인 여부 변경해주고 시작
						Map<String, String> parameter = new HashMap<String, String>();
						
						smrNumber = reqExtInfo.get("SMR_NUMBER");
						parameter.put("SMR_NUMBER", smrNumber);
 						for(Map<String, Object> docMap : documentList) {
							List<Map<String, String>> selectSealList = (List<Map<String, String>>) docMap.get("SELECT_EXT_LIST");
							for(Map<String, String> selectedSeal : selectSealList) {
								String sessionId = info.getSession("ID");
								String sessionDept = info.getSession("DEPARTMENT");
								// 선택된 인감의 값이 비어있지 않아야 한다
								if(!"".equals(selectedSeal.get("SEAL_MG_USER_ID")) && !"".equals(selectedSeal.get("SEAL_MG_DEPT"))) {
									// 현재세션(승인을 누른 인감관리자)의 아이디와 부서가 선택된 인감의 관리부서, 관리자ID랑 일치하여야한다.
 									if(sessionId.equals(selectedSeal.get("SEAL_MG_USER_ID")) || sessionId.equals(selectedSeal.get("RECE_USER_ID"))){
										// 승인여부 변경 및 선택인감 상태 업데이트
										selectedSeal.put("ACCEPT_FLAG", "Y");
										selectedSeal.put("MANAGER_ID", selectedSeal.get("SEAL_MG_USER_ID"));
										selectedSeal.put("MANAGE_DEPT", selectedSeal.get("SEAL_MG_DEPT"));
										selectedSeal.put("SMD_NUMBER", selectedSeal.get("SMD_NUMBER"));
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
							status = "W";
							doAfterFlag = true;
							
							// 반납 완료 메일 발송 진행	                
		                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_COMPLETE"));
		                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_COMPLETE"));
		                	mailData.put("MESSAGE_TYPE", "EXTERNAL_COMP");
						} else {
							status = "W";
						}
					} else if("MANAGER_REJECT".equals(confirmStatus)) {
						status = "F";
						
						for(Map<String, Object> docMap : documentList) {
							List<Map<String, String>> selectSealList = (List<Map<String, String>>) docMap.get("SELECT_EXT_LIST");
							for(Map<String, String> selectedSeal : selectSealList) {
								// 승인여부 변경 및 선택인감 상태 업데이트
								selectedSeal.put("ACCEPT_FLAG", "N");
								selectedSeal.put("COMPLETE_FLAG", "Y");
								selectedSeal.put("MANAGER_ID", selectedSeal.get("SEAL_MG_USER_ID"));
								selectedSeal.put("MANAGE_DEPT", selectedSeal.get("SEAL_MG_DEPT"));
								selectedSeal.put("SMD_NUMBER", selectedSeal.get("SMD_NUMBER"));
								if(!srm.doUpdateSealDocSeal(selectedSeal)) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
						}
						
						//메일 셋팅
						mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REJECT"));
						mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_REJECT"));
						mailData.put("MESSAGE_TYPE", "EXTERNAL_REJECT");
					}
					
					break;
				case "RETURN":
					applyType = reqExtInfo.get("APPLY_TYPE");
					smrNumber = reqExtInfo.get("SMR_NUMBER");
					confirmStatus = reqExtInfo.get("CONFIRM_STATUS");
					
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
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_EXT", smrNumber, null);
					}
					
					if("RETURN_REQUEST".equals(confirmStatus)) {
						status = "RR";
						// 요청시에 인감관리자한테 권한을 줘야한다.
						if(documentList != null && documentList.size() > 0) {
							for(Map<String, Object> sealDoc : documentList) {
								List<Map<String, String>> selectSealList = (List<Map<String, String>>) sealDoc.get("SELECT_EXT_LIST");
								for(Map<String, String> selectedSeal : selectSealList) {
									isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), selectedSeal.get("SEAL_MG_USER_ID"), Authorized.WRITE);
									if(getStatus() == 1 && !isGrant) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
					     			// 반납 요청 메일 발송 진행
									Id_data.add(selectedSeal.get("MANAGER_ID"));
				                	mailData.put("SEAL_MG_USER_ID", selectedSeal.get("MANAGER_ID"));
				                	mailData.put("SMD_NUMBER", selectedSeal.get("SMD_NUMBER"));
				                	mailData.put("SUBJECT", Cache.text.get("MAIL.RETURN_REQUEST"));
				                	mailData.put("REQUESTER", info.getSession("NAME_LOC"));
				                	mailData.put("STEP", Cache.text.get("MAIL.RETURN_STATUS_PROGRESS"));
				                	mailData.put("MESSAGE_TYPE", "RETURN_REQ");
								}
							}
						}
		 				
					} else if("RETURN_COMPLETE".equals(confirmStatus)) {
						// 승인시에 반복문을 사용하여 인감관리자의 다른 승인이 남았는지 체크
						// 인감관리자의 승인이 남은 날인요청의 경우에는 status를 변경하지 않는다.
						// 먼저 본인 인감 승인 여부 변경해주고 시작
						Map<String, String> parameter = new HashMap<String, String>();
						
						smrNumber = reqExtInfo.get("SMR_NUMBER");
						parameter.put("SMR_NUMBER", smrNumber);
 						for(Map<String, Object> docMap : documentList) {
							List<Map<String, String>> selectSealList = (List<Map<String, String>>) docMap.get("SELECT_EXT_LIST");
							for(Map<String, String> selectedSeal : selectSealList) {
								String sessionId = info.getSession("ID");
								String sessionDept = info.getSession("DEPARTMENT");
								// 선택된 인감의 값이 비어있지 않아야 한다
								if(!"".equals(selectedSeal.get("SEAL_MG_USER_ID")) && !"".equals(selectedSeal.get("SEAL_MG_DEPT"))) {
									// 현재세션(승인을 누른 인감관리자)의 아이디와 부서가 선택된 인감의 관리부서, 관리자ID랑 일치하여야한다.
									//if(sessionId.equals(selectedSeal.get("SEAL_MG_USER_ID")) && sessionDept.equals(selectedSeal.get("SEAL_MG_DEPT"))) {
										// 승인여부 변경 및 선택인감 상태 업데이트
										selectedSeal.put("COMPLETE_FLAG", "Y");
										selectedSeal.put("SEAL_MG_USER_ID", info.getSession("ID"));
										selectedSeal.put("MANAGER_ID", selectedSeal.get("SEAL_MG_USER_ID"));
										selectedSeal.put("MANAGE_DEPT", selectedSeal.get("SEAL_MG_DEPT"));
										if(!srm.doUpdateSealDocSeal(selectedSeal)) {
											int errCode = srm.getLastErrCode();
											if(errCode != 0) {
												setStatus(errCode);
											}
										}
									//}
								}
							}
						}
						parameter.put("COMPLETE_FLAG", "N");
						
						// 승인 여부가 N(미승인) 상태의 데이터가 한건도 없으면 다음모듈로 진행
						if(srm.doSelectReqSealDocSeal(parameter) == null) {
							status = "C";
							doAfterFlag = true;
							
			     			// 반납 완료 메일 발송 진행	                
		                	mailData.put("SUBJECT", Cache.text.get("MAIL.RETURN_COMPLETE"));
		                	mailData.put("STEP", Cache.text.get("MAIL.RETURN_STATUS_COMPLETE"));
		                	mailData.put("MESSAGE_TYPE", "RETURN_COMP");
						} else {
							status = "RR";
						}
					}
					break;
				default:
					break;
				}				
			}
			
			// 반출요청 Status 업데이트
			String curStatus = MapUtils.getString(reqExtInfo, "SMR_STATUS", null);
			if (curStatus == null || !"T".equals(curStatus) || !"T".equals(status)) {
				if(getStatus() == 1 && !srm.doUpdateReqExtDocStatus(status)) {
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
			if(getStatus() == 1 && SepoaModuleService.checkBusiness(new Configuration(), info, "MAIL1649307822120")) {
	            try {
	            	SepoaPushManager mailpush = new SepoaPushManager(info, conf);
	     			SepoaMessage message = new SepoaMessage();
                	
	     			HttpServletRequest req = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
    	 			//String baseURL = req.getRequestURL().toString().replace(req.getRequestURI(),"");
	     			//개발
    	 			//String baseURL = "http://swpsso.posco.net/idms/U61/jsp/redirectSMSP.jsp?redir_url=https%3A%2F%2Fsealdev.poscochemical.com%2FLogin%2FSSO%2FLoginSSO.sux";
	     			//운영
    	 			String baseURL = "http://swpsso.posco.net/idms/U61/jsp/redirectSMSP.jsp?redir_url=https%3A%2F%2Fseal.poscochemical.com%2FLogin%2FSSO%2FLoginSSO.sux";
    	 																							  
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
	
	@Description("반출요청 서비스 후처리")
	@SuppressWarnings("unchecked")
	public boolean doRequestServiceAfter(Map<String, Object> allData, SepoaExtSealManager manager) {
		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
		String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
		Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
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
				headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
				parseMap = new Gson().fromJson(MapUtils.getString(headerData, "PARAMETER"), Map.class);
				currentPrc = headerData.get("CURRENT_PRC");
				nextPrc = MapUtils.getString(headerData, "NEXT_PRC", null);
			}
			
			// 반출요청 객체 정보
			Map<String, String> reqExtInfo = (Map<String, String>)parseMap.get("REQ_EXT_INFO");
			manager.setDocBaiscInfo(reqExtInfo);
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			// 반출요청 서류 문서 리스트
			List<Map<String, Object>> documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
			// 교부서류 문서 리스트
			List<Map<String, String>> attachDocumentList = (List<Map<String, String>>)parseMap.get("ATTACH_DOCUMENT_LIST");
			
			
			String currentAlias = MapUtils.getString(reqExtInfo, "APPLY_TYPE");
			if("C".equals(currentAlias)) {}
			String parentAlias = USE_EXT_CREATE;
			
			String status = "";
			String smrNumber = reqExtInfo.get("SMR_NUMBER");
			String applyType = reqExtInfo.get("APPLY_TYPE");
			if(nextPrc == null || "".equals(nextPrc)) {
				Map<String, String> paramsMap = new HashMap<>();
				paramsMap.put("ALIAS", currentPrc);
				paramsMap.put("PARENT", parentAlias);
				Map<String, String> nextModuleMap = this.doFindNextModule(paramsMap);
				nextPrc = MapUtils.getString(nextModuleMap, "ALIAS", nextModuleMap.get("MODULE"));
			}
			
			switch(nextPrc) {
				case "MANAGER": //승인모듈
					reqExtInfo.remove("SMR_STATUS");
					manager.setDocBaiscInfo(reqExtInfo);
					
					status = "A";
				break;
				case "RETURN": //반입모듈
					reqExtInfo.remove("SMR_STATUS");
					manager.setDocBaiscInfo(reqExtInfo);
					
					status = "RW";
				break;
				case "COMPLETE": //반입완료모듈
					reqExtInfo.remove("SMR_STATUS");
					manager.setDocBaiscInfo(reqExtInfo);
					
					//날인완료 히스토리 적재
					if(getStatus() == 1) {
						if(!manager.doAddProcessHistory("EXT_COMPLETE")) {
							int errCode = manager.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					}
					
					status = "C";
					break;
			}
			
			// 날인요청 Status 업데이트
			if(getStatus() == 1 && !manager.doUpdateReqExtDocStatus(status)) {
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
			
			List<Map<String, String>> searchResult = new ArrayList<Map<String, String>>();
			if(SepoaModuleService.checkBusiness(new Configuration(), info, "US1637133464814")) {
				// 용도기준
				searchResult = sm.doSelectSealListRequiredUse(headerData);
			//} else if (SepoaModuleService.checkBusiness(new Configuration(), info, "US1637133452574")) { // 부서 기준
//				headerData.put("SEAL_MG_DEPT", headerData.get("SEAL_MG_DEPT"));
//				searchResult = sm.doSelectSealList(headerData);
			} else { // 부서 기준
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
	
	@Description("반출 요청내역 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectRequestList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaExtSealManager srm = getSepoaExtSealManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			Configuration config = new Configuration();
			String ctrlCode = info.getSession("CTRL_CODE").replaceAll("\\s", "");
			List<String> ctrlCodeList = Arrays.asList(ctrlCode.split(","));
			headerData.put("F_KEYWORD", MapUtils.getString(headerData, "F_KEYWORD", "").trim());
			String mode = MapUtils.getString(headerData, "MODE", null);
			
			if("LIST".equals(mode) || "LIST_COUNT".equals(mode)) {
				boolean viewSameDept = SepoaModuleService.checkBusiness(config, info, "ETL1655792101584");	// 같은 부서원의 신청내역을 볼 수 있는지
				if (!(myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code")) || ctrlCodeList.contains("EXT_CONFIRM"))) {	// 관리자
					if (viewSameDept) {
						headerData.put("REQ_USER_DEPT", info.getSession("DEPARTMENT"));
					} else {
						headerData.put("REQ_USER_ID", info.getSession("ID"));
					}
				}
				
			} else if ("TODO".equals(mode) || "TODO_COUNT".equals(mode)) {
				if(headerData.get("SMR_STATUS") == null) {
					headerData.put("SMR_STATUS", "I,W,RR");					
				}
				if (!myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {	// 관리자
					Map<String, String> resultData = srm.doSearchMAND(headerData);
					if(resultData == null) {
						headerData.put("SEAL_MG_USER_ID", info.getSession("ID"));
					}else{
						headerData.put("SEAL_MG_USER_ID", resultData.get("GIVE_USER_ID"));
					}
				}
			}
				
			srm.setDocBaiscInfo(headerData);
			List<Map<String, String>> resultList = srm.doSelectRequestList();
			
			if(resultList == null) {
				int errCode = srm.getLastErrCode();
				if (("LIST_COUNT".equals(mode) || "TODO_COUNT".equals(mode)) && errCode == 104013) {
					setStatus(1);
					setValue(0);
					return getSepoaOut();
				} else if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				sf = new SepoaFormater(resultList);
				if(sf.getRowCount() > 0) {
					resultList = new SepoaFormater(resultList).getListMap();
				}
				
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
	
	@Description("반출 요청내역 상태값별 카운트")
	@SuppressWarnings("unchecked")
	public SepoaOut doCountRequestList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaOut so = null;
		ConnectionContext ctx = null;
		
		try {
			ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, Integer> countMap = new HashMap<String, Integer>();
			Map<String, Object> paramMap = new HashMap<String, Object>();
			
			String[] statusList = headerData.get("SEAL_EXT_STATUS_LIST").split(",");
			for (String status : statusList) {
				Map<String, String> paramHeader = new HashMap<String, String>();
				paramMap.put(SepoaDataMapper.KEY_HEADER_DATA, paramHeader);
				paramHeader.putAll(headerData);
				paramHeader.put("MODE", "LIST_COUNT");
				paramHeader.put("SMR_STATUS", status);
				so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaReqExternalService", ctx, "doSelectRequestList", new Object[] {paramMap});
				int listSize = Integer.parseInt(so.result[0]);
				countMap.put("SEAL_EXT_COUNT_"+status, listSize);
			}
			
			Map<String, String> paramHeader = new HashMap<String, String>();
			paramMap.put(SepoaDataMapper.KEY_HEADER_DATA, paramHeader);
			paramHeader.put("MODE", "TODO_COUNT");
			so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaReqExternalService", ctx, "doSelectRequestList", new Object[] {paramMap});
			int todoSize = Integer.parseInt(so.result[0]);
			countMap.put("SEAL_EXT_COUNT_TODO", todoSize);
			
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
	
	@Description("반출요청 조회")
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
			SepoaExtSealManager srm = getSepoaExtSealManager(info, new Configuration(), getConnectionContext());
			String applyType = headerData.get("APPLY_TYPE");
			srm.setDocBaiscInfo(headerData);
			// 반출요청 정보조회
			reqList = srm.doSelectRequestList();
			if(reqList == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				reqInfo = reqList.get(0);
				resultMap.put("REQ_EXT_INFO", reqInfo);
			}
			
			if(getStatus() == 1 && reqInfo != null) {
				// 반출 요청서류 조회
				reqDocList = srm.doSelectReqExtDoc(headerData);
				if(reqDocList != null) {
					for(Map<String, String> rows : reqDocList) {
						// 반출 요청서류에 쓰인 인감 조회
						List<Map<String, String>> docSealList = srm.doSelectReqSealDocSeal(rows);
						for(Map<String, String> sealRow : docSealList) {
							sealList.add(sealRow);
						}
					}
					resultMap.put("DOCUMENT_LIST", reqDocList);
					resultMap.put("SELECTED_EXT_LIST", sealList);
				} 
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
	
	@Description("반출요청 삭제")
	@SuppressWarnings("unchecked")
	public SepoaOut doDeleteRequestInfo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaExtSealManager srm = getSepoaExtSealManager(info, new Configuration(), getConnectionContext());
			for(Map<String, String> rows : gridData) {
				srm.setDocBaiscInfo(rows);
				// 반출요청 삭제
				if(!srm.doDeleteReqExt(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 반출요청 - 날인서류 삭제
				if(getStatus() == 1 &&!srm.doDeleteReqExtDoc(rows)) {
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
	
	
	@Description("관리대장 리스트 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectLedgerList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaExtSealManager srm = getSepoaExtSealManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			headerData.put("SMR_STATUS", "C");
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			String ctrl_code_array= info.getSession("CTRL_CODE");
						
			if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {	// 관리자
				headerData.put("MANAGER_ID", "");
			} else if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.legal.porfile.code"))) {// 인감책임자
				if(ctrl_code_array.contains("EXT_CONFIRM") ) {
					headerData.put("MANAGER_ID", "");
				}else {
					headerData.put("MANAGER_ID", info.getSession("ID"));
					headerData.put("MY_SEAL", "true");	// 본인 소유의 인감만 조회
				}
			} else if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.seal.manager.porfile.code"))) {	// 인감담당자
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
	
	
	@Description("반출 가능한 인감인지 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doSearchExtUse(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaExtSealManager srm = getSepoaExtSealManager(info, new Configuration(), getConnectionContext());
			String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
			Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
			Map<String, String> resultMap = new HashMap<String, String>();
			List<Map<String, Object>> documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
			
			for(Map<String, Object> sealDoc : documentList) {
				resultMap.put("EXT_OUT_EXPECT_DATE", headerData.get("EXT_OUT_EXPECT_DATE"));
				resultMap.put("EXT_IN_EXPECT_DATE", headerData.get("EXT_IN_EXPECT_DATE"));
				resultMap.put("EXT_OUT_EXPECT_HOUR", headerData.get("EXT_OUT_EXPECT_HOUR"));
				resultMap.put("EXT_IN_EXPECT_HOUR", headerData.get("EXT_IN_EXPECT_HOUR"));
				List<Map<String, String>> selectSealList = (List<Map<String, String>>) sealDoc.get("SELECT_EXT_LIST");
				for(Map<String, String> selectedSeal : selectSealList) {
					resultMap.put("MANAGE_NUMBER", selectedSeal.get("MANAGE_NUMBER"));
					
					if("FALSE".equals(headerData.get("CHK_CAL_VAL"))) {
						resultMap.put("CHK_CAL_VAL", headerData.get("CHK_CAL_VAL"));
					}
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
	
	@Description("반출요청 결재진행")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalProgress(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			
			SepoaExtSealManager srm = getSepoaExtSealManager(info, config, ctx);
			
			// 인장 치환 처리
			Map<String, String> sealInfo = new HashMap<String, String>();
			sealInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
			srm.setDocBaiscInfo(sealInfo);

			String docStatus = "I";
			
			// 날인요청 Status 업데이트
			if(getStatus() == 1 && !srm.doUpdateReqExtSignStatus(docStatus)) {
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
	@Description("반출요청 결재완료")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalConfirm(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			//String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
			if(SepoaApproval.APP_STATUS_FINAL.equals(MapUtils.getString(headerData, "APP_STATUS", ""))){
				SepoaExtSealManager srm = getSepoaExtSealManager(info, config, ctx);
				
				Map<String, String> sealInfo = new HashMap<String, String>();
				sealInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
				sealInfo.put("CURRENT_PRC", headerData.get("SUX_WKEY2"));
				sealInfo.put("REJECT_REMARK", headerData.get("ACCEPT_REASON"));
				sealInfo.put("SIGN_DATE", SepoaDate.getShortDateString());
				sealInfo.put("SIGN_TIME", SepoaDate.getShortTimeString());
				String signUserIdForced = MapUtils.getString(headerData, "SIGN_USER_ID_FORCED", null);
				sealInfo.put("SIGN_USER_ID", signUserIdForced == null ? info.getSession("ID") : signUserIdForced);
				
				srm.setDocBaiscInfo(sealInfo);
				
				String finalDocStatus ;
				
				if (SepoaModuleService.checkBusiness(config, info, "ET1647222908416")) {
					finalDocStatus = "RW";
				}else {
					finalDocStatus = "C";
				}
				
				
				if(getStatus() == 1 && !srm.doUpdateReqExtSignStatus(finalDocStatus)) {
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
	@Description("반출결재 요청 거부")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalReject(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			
			if(SepoaApproval.APP_STATUS_REJECT.equals(MapUtils.getString(headerData, "APP_STATUS", ""))){
				SepoaExtSealManager srm = getSepoaExtSealManager(info, config, ctx);
				
				Map<String, String> sealInfo = new HashMap<String, String>();
				sealInfo.put("SMR_NUMBER", headerData.get("SUX_WKEY1"));
				sealInfo.put("REJECT_REMARK", headerData.get("ACCEPT_REASON"));
				sealInfo.put("SIGN_DATE", SepoaDate.getShortDateString());
				sealInfo.put("SIGN_TIME", SepoaDate.getShortTimeString());
				String signUserIdForced = MapUtils.getString(headerData, "SIGN_USER_ID_FORCED", null);
				sealInfo.put("SIGN_USER_ID", signUserIdForced == null ? info.getSession("ID") : signUserIdForced);
				
				srm.setDocBaiscInfo(sealInfo);
				
				PathManager pm = new PathManager(config);
				pm.setLayer(PathManager.LAYER_TEMP);
				String tempFilePath = pm.getUniquePath();
				
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, tempFilePath);
				srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				
				//결재거부 처리
				String finalDocStatus = "R";
				
				Map<String ,String> sealAll = new HashMap<String, String>();
				// 승인여부 변경 및 선택인감 상태 업데이트(결재의 경우 각 인감담당자가 아닌 결재자가 반려를 하므로 한번에 상태값 업데이트 처리 진행)
				sealAll.put("ACCEPT_FLAG", "N");
				sealAll.put("COMPLETE_FLAG", "Y");
				sealAll.put("MANAGER_ID", info.getSession("ID"));
				sealAll.put("MANAGE_DEPT", info.getSession("DEPT"));
				sealAll.put("SMR_NUMBER", sealInfo.get("SMR_NUMBER"));
				sealAll.put("EXT_APP_MODULE", "Y");
				if(!srm.doUpdateSealDocSeal(sealAll)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				// 반출 결재 거부 Status 업데이트
				if(getStatus() == 1 && !srm.doUpdateReqExtSignStatus(finalDocStatus)) {
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
	@Description("반출결재 요청 회수")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalWithdraw(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
}
