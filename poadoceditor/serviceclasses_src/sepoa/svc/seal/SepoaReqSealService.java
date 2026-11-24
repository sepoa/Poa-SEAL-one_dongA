package sepoa.svc.seal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.filemanager.FileManager;
import sepoa.filemanager.utils.cipher.CipherManager;
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
import sepoa.fw.jtx.SepoaTransactionalResource;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
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
import sepoa.fw.ux.link.SepoaLinkManager;
import sepoa.seal.SepoaExtSealManager;
import sepoa.seal.SepoaReqSealManager;
import sepoa.seal.SepoaSealManagementManager;
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

	/**
	 * 날인신청현황 전체조회 허용 부서 여부 (동아ST 038 전용)
	 * sepoa.seal.request.view.all.dept (세미콜론 구분 부서코드)
	 * 038: 총무팀(D804287025), 법무팀(D406102150)
	 * 다른 하우스(012 등)는 설정이 있어도 적용하지 않음
	 */
	protected boolean isSealRequestViewAllDept() {
		try {
			// 동아ST(038)만 적용
			if (!"038".equals(info.getSession("HOUSE_CODE"))) {
				return false;
			}
			String deptConfig = CommonUtil.getConfig("sepoa.seal.request.view.all.dept", "");
			if (StringUtils.isBlank(deptConfig)) {
				return false;
			}
			String sessionDept = StringUtils.defaultString(info.getSession("DEPARTMENT")).trim();
			if (StringUtils.isBlank(sessionDept)) {
				return false;
			}
			String[] allowDepts = deptConfig.replaceAll("\\s", "").split(";");
			for (String allowDept : allowDepts) {
				if (StringUtils.isNotBlank(allowDept) && allowDept.equals(sessionDept)) {
					return true;
				}
			}
		} catch (Exception e) {
			Logger.err.println(info.getSession("ID"), this, "isSealRequestViewAllDept check fail: " + e.getMessage());
		}
		return false;
	}

	/** 관리자 / SEAL_CONFIRM / 전체조회 허용 부서 */
	protected boolean canViewAllSealRequestList(String systemAdminMenuProfilecode, List<String> ctrlCodeList) {
		String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
		return myMenuProfile.equals(systemAdminMenuProfilecode)
				|| (ctrlCodeList != null && ctrlCodeList.contains("SEAL_CONFIRM"))
				|| isSealRequestViewAllDept();
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
			List<Map<String,String>> linkMapList = new ArrayList<Map<String,String>>();
			
			String currentPrc = headerData.get("CURRENT_PRC").toString();
			String status = "";
			String applyType = "";
			String smrNumber = "";
			String smdNumber = "";
			String confirmStatus = "";
			boolean doAfterFlag = false;
			boolean isGrant = false;
			boolean isGrantSub = false;
			boolean isGrantRes = false;
			boolean mailNow = true;	// 바로 메일을 보냄
			
			SepoaReqSealManager srm = getSepoaReqSealManager(info, config, getConnectionContext());
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			Map<String, String> reqSealInfo = (Map<String, String>)parseMap.get("REQ_SEAL_INFO");
			Map<String, String> params = (Map<String, String>)parseMap.get("PARAMS");
			
			if(StringUtils.isNotBlank(currentPrc)) {
				// 날인요청 객체 정보
				srm.setDocBaiscInfo(reqSealInfo);
				// 날인요청 서류 문서 리스트
				//List<Map<String, Object>> documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
				// 교부서류 문서 리스트
				List<Map<String, String>> attachDocumentList = (List<Map<String, String>>)parseMap.get("ATTACH_DOCUMENT_LIST");
				switch (currentPrc) {
				case "CREATE_CERT":
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					applyType = reqSealInfo.get("APPLY_TYPE");
					// 날인요청 객체 Setting
					Map<String, String> paramMap_cert = new HashMap<String, String>();
					paramMap_cert.put("SMR_NUMBER", smrNumber);
					
					List<Map<String, String>> srmCertList = srm.doSelectRequestList(paramMap_cert);
					if(srmCertList != null && srmCertList.size() > 0) {
						String oldSmrStatus = srmCertList.get(0).get("SMR_STATUS");
						String reqSmrStatus = reqSealInfo.get("SMR_STATUS");
						
						if(!"".equalsIgnoreCase(reqSmrStatus) && // 최초 상신이 아닌경우
								!oldSmrStatus.equalsIgnoreCase(reqSmrStatus)) { 
							int errCode = 100016;
							setStatus(errCode);
						}
						
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
							if(!srm.doAddProcessHistory("CERT_REQUEST")) {
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
						
						// 전자 교부서류이면서 템플릿 폼이 지정되었을 경우 템플릿 데이터가 입력되었는 지 검증
						for(Map<String, String> attDoc : attachDocumentList) {
							if(attDoc.get("REAL_DOC_CHK").equals("E") && 
									!StringUtils.isBlank(srm.doSelectCorpAttachDocHeader(attDoc).get(0).get("TEMPLATE_FORM"))) {
								if(StringUtils.isEmpty(attDoc.get("TEMPLATE_MAPPING_DATA"))) {
									setStatus(102013);
								}
							}
						}
						
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
							
							String reFlag = MapUtils.getString(attDoc, "REAL_DOC_CHK");
							if("E".equalsIgnoreCase(reFlag) && SepoaModuleService.checkBusiness(new Configuration(), info, "CERT1687742002001")) {
								if(!srm.doCreateExternalAttDocAccessLink(attDoc)) {
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
					List<Map<String, Object>> documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					applyType = reqSealInfo.get("APPLY_TYPE");
					// 날인요청 객체 Setting
					Map<String, String> paramMap = new HashMap<String, String>();
					paramMap.put("SMR_NUMBER", smrNumber);
					
					List<Map<String, String>> srmList = srm.doSelectRequestList(paramMap);
					if(srmList != null && srmList.size() > 0) {
						String oldSmrStatus = srmList.get(0).get("SMR_STATUS");
						String reqSmrStatus = reqSealInfo.get("SMR_STATUS");
						
						if(!"".equalsIgnoreCase(reqSmrStatus) &&  // 최초상신이 아닐경우 
								!oldSmrStatus.equalsIgnoreCase(reqSmrStatus)) {  // 상태값이 서로 다르면
							int errCode = 100016;
							setStatus(errCode);
						}
						
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
							setStatus(1);
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
						if("E".equals(reqSealInfo.get("RE_FLAG")) && getStatus() == 1 && headerData.get("tempFlag") != null && "N".equals(headerData.get("tempFlag"))) {
							if(!doCheckInputSealData(documentList,srm)) {
								int errCode = srm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}	
						}
						if(getStatus() == 1) {
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
								
								if("U".equalsIgnoreCase(applyType) && SepoaModuleService.checkBusiness(new Configuration(), info, "USW1687741881664")) {
									if(!srm.doCreateExternalReqSealDocAccessLink(sealDocParsed)) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								} else if("C".equalsIgnoreCase(applyType) && SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1687741963370")) {
									if(!srm.doCreateExternalReqSealDocAccessLink(sealDocParsed)) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								}
							}
						}
					}
					
					String str_delrow2 = headerData.get("DEL_ROW2");
					if(getStatus() == 1 && str_delrow2 != null && !str_delrow2.isEmpty()) {
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
						
						// 전자 교부서류이면서 템플릿 폼이 지정되었을 경우 템플릿 데이터가 입력되었는 지 검증
						for(Map<String, String> attDoc : attachDocumentList) {
							if(attDoc.get("REAL_DOC_CHK").equals("E") && 
									!StringUtils.isBlank(srm.doSelectCorpAttachDocHeader(attDoc).get(0).get("TEMPLATE_FORM"))) {
								if(StringUtils.isEmpty(attDoc.get("TEMPLATE_MAPPING_DATA"))) {
									setStatus(102013);
								}
							}
						}
						
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
							
							if("E".equalsIgnoreCase(attDoc.get("REAL_DOC_CHK"))) {
								if("U".equalsIgnoreCase(applyType) && SepoaModuleService.checkBusiness(new Configuration(), info, "USW1687741881664")) {
									if(!srm.doCreateExternalAttDocAccessLink(attDoc)) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								} else if("C".equalsIgnoreCase(applyType) && SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1687741963370")) {
									if(!srm.doCreateExternalAttDocAccessLink(attDoc)) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								}
							}
						}
					}
					
					//SSIAD 부가 데이터 저장 서비스
					Map<String, Object> extraData = MapUtils.getMap(reqSealInfo, "extraInfo", null);
					if(getStatus() == 1 && (extraData != null && extraData.size() > 0)) {
						extraData.put("SMR_NUMBER", smrNumber);
						if(srm.doSaveExtraInfo(extraData) == null) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					}
					status = "T";
					break;
				case "APPROVAL":
					ctx = getConnectionContext();
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
					headerData.put("APP_TYPE", reqSealInfo.get("APP_TYPE"));
					
					Logger.debug.println("==APP_TYPE="+MapUtils.getString(reqSealInfo, "APP_TYPE"));
					Logger.debug.println("==APP_STATUS="+MapUtils.getString(reqSealInfo, "APP_STATUS" , ""));
					
					headerData.put("SUX_WKEY1", smrNumber);
					String screenType = "";
					switch (reqSealInfo.get("APP_TYPE")) {
					case "J01":
						screenType = "MS01";
						break;
					case "J02":
						screenType = "MS02";
						break; 
					case "J09":
						screenType = "MS09";
						break;
					default:
						break;
					}
					
					String conFirmUser = "";
	     			List<Map<String, String>> linkMgrAppLine = (List<Map<String, String>>) parseMap.get("APP_LINE");
	     			for(Map<String,String> linkMgrAppLines : linkMgrAppLine) {
	     				if(linkMgrAppLines.containsKey("APP_STAGE") && linkMgrAppLines.get("APP_LINE_SEQ").equals("1")) { 
	                		conFirmUser = linkMgrAppLines.get("APP_USER_ID");
	                	}
	     			}
	     			
					HttpServletRequest req = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
		 			String baseURL = req.getRequestURL().toString().replace(req.getRequestURI(),"");
		 			Map<String, String> mailCustomData = new HashMap<String, String>();
		 			
		 			mailCustomData.put("SSIAD_SUBJECT", Cache.text.get("MAIL.REQ_HEADERTEXT") + " " + reqSealInfo.get("SUBJECT"));
		 			mailCustomData.put("URL2", conf.get("sepoa.system.domain.name", "seal.poacloud.net"));
		 			
		 			SepoaLinkManager linkManager = new SepoaLinkManager(info, conf, ctx);
		 			Map<String, String> linkParams = new HashMap<String, String>();
		 			
		 			SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
		 			sepoaApprovalLineSetting.doApprovalProcess(allData);
		 			
		            String appNo = MapUtils.getString(headerData, "APP_NO", "");
		            String appNoSeq = MapUtils.getString(headerData, "APP_NO_SEQ", "");
		            linkParams.put("APP_NO", appNo);
		            linkParams.put("APP_NO_SEQ", appNoSeq);
		            linkParams.put("SMR_NUMBER", smrNumber);
		            linkParams.put("SCREEN_TYPE", screenType);
		            linkParams.put("APP_TYPE", reqSealInfo.get("APP_TYPE"));
		            linkParams.put("RE_FLAG", reqSealInfo.get("RE_FLAG"));
		            
		            if(!linkManager.newLink("approvalm", appNo, appNoSeq) || !linkManager.setParams(linkParams)) {
		            	int errCode = linkManager.getLastErrCode();
		            }else{
		            	String companyCode = info.getSession("COMPANY_CODE");
		            	String privateLink = linkManager.addUser(companyCode, conFirmUser, linkParams);
		            	mailCustomData.put("URL", baseURL + "/link.sux?ln="+privateLink);
		            	allData.put("MAIL_CUSTOM_DATA", mailCustomData);
		            }
		            
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
		            Map<String, String> map = new HashMap<>();
		            map.put("returnType" , "param");
		            map.put("APP_NO"     , MapUtils.getString(headerData, "APP_NO"     , ""));
		            map.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
		            map.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "I"));
		            map.put("SAVE_MODE", MapUtils.getString(headerData,"SAVE_MODE",""));
		            setObject(map);
		            
	                SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
	                sepoaApprovalProgress.doApprovalProcess(allData);
					
	                //외부 결재 호출
					applyType = reqSealInfo.get("APPLY_TYPE");
					if("U".equalsIgnoreCase(applyType) && SepoaModuleService.checkBusiness(new Configuration(), info, "USW1677739447992")) {
						//사용인감 날인요청이면서 외부 결재 모듈을 사용할 때
						Map<String, Object> externalServiceParams = new HashMap<String, Object>();
						externalServiceParams.put("REQ_SEAL_INFO", reqSealInfo);
						externalServiceParams.put("MANAGER_OBJ", srm);
						externalServiceParams.put("APP_LINE", parseMap.get("APP_LINE"));
						externalServiceParams.put("APP_INFO", parseMap.get("APP_INFO"));
						externalServiceParams.put("DOCUMENT_LIST", parseMap.get("DOCUMENT_LIST"));
						// 전자결재 번호 추가 [2023-09-25 김민수 추가]
						externalServiceParams.put("APP_NO", MapUtils.getString(headerData, "APP_NO", ""));
						
						SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.approval.SepoaSealApprovalService", ctx, "useSealReqApproval", new Object[] {externalServiceParams});
						if(out == null) {
							setStatus(70009);
						} else if(out != null && out.status != 1) {
							setStatus(out.status);
						}
					} else if("C".equalsIgnoreCase(applyType) && SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1677739530413")) {
						//법인인감 날인요청이면서 외부 결재 모듈을 사용할 때
						Map<String, Object> externalServiceParams = new HashMap<String, Object>();
						externalServiceParams.put("REQ_SEAL_INFO", reqSealInfo);
						externalServiceParams.put("MANAGER_OBJ", srm);
						externalServiceParams.put("APP_LINE", parseMap.get("APP_LINE"));
						externalServiceParams.put("APP_INFO", parseMap.get("APP_INFO"));
						externalServiceParams.put("DOCUMENT_LIST", parseMap.get("DOCUMENT_LIST"));
						// 전자결재 번호 추가 [2023-09-25 김민수 추가]
						externalServiceParams.put("APP_NO", MapUtils.getString(headerData, "APP_NO", ""));
						
						SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.approval.SepoaSealApprovalService", ctx, "corpSealReqApproval", new Object[] {externalServiceParams});
						if(out == null) {
							setStatus(70009);
						} else if(out != null && out.status != 1) {
							setStatus(out.status);
						}
					} else if("D".equalsIgnoreCase(applyType) && SepoaModuleService.checkBusiness(new Configuration(), info, "CERT1679392777575")) {
						Map<String, Object> externalServiceParams = new HashMap<String, Object>();
						externalServiceParams.put("REQ_SEAL_INFO", reqSealInfo);
						externalServiceParams.put("MANAGER_OBJ", srm);
						externalServiceParams.put("DOCUMENT_LIST", parseMap.get("DOCUMENT_LIST"));
						externalServiceParams.put("APP_LINE", parseMap.get("APP_LINE"));
						externalServiceParams.put("APP_INFO", parseMap.get("APP_INFO"));
						// 전자결재 번호 추가 [2023-09-25 김민수 추가]
						externalServiceParams.put("APP_NO", MapUtils.getString(headerData, "APP_NO", ""));

						SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.approval.SepoaSealApprovalService", ctx, "certReqApproval", new Object[] {externalServiceParams});
						if(out == null) {
							setStatus(70009);
						} else if(out != null && out.status != 1) {
							setStatus(out.status);
						}
					}
					mailNow = false;
					status = "I";
					break;
				case "LEGAL":
					//법인인감 인감책임자 로직
					applyType = reqSealInfo.get("APPLY_TYPE");
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					confirmStatus = reqSealInfo.get("CONFIRM_STATUS");
					ctx = getConnectionContext();
					//메일발송 필요 데이터 셋팅
					mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
                	mailData.put("SMR_NUMBER", smrNumber);
                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
                	mailData.put("TIME", format.format(new Date()));
                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
                	mailData.put("CONTENTS", smrNumber);
                	
                	if(!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "REQ_SEAL", smrNumber, null);
					}
                	
                	if("C".equals(applyType) && (SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1635988016964"))) {
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
						//법인인감 인감책임자 승인로직
						status = "O";
						doAfterFlag = true;
						headerData.put("NEXT_PRC", "COMPLETE");
						headerData.put("SUX_WKEY1", smrNumber);
						doApprovalConfirmReady(ctx, allData);
						mailNow = false;
					} else if("LEGAL_REJECT".equals(confirmStatus)) {
						//법인인감 인감책임자 반려로직
						status = "N";
						
						//메일 셋팅
						mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REJECT"));
						mailData.put("REQUESTER", info.getSession("NAME_LOC"));
						mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_REJECT"));
						mailData.put("MESSAGE_TYPE", "LEGAL_REJECT");
					} else if("LEGAL_REQUEST".equals(confirmStatus)) {
						//법인인감 인감책임자 요청로직
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
					ctx = getConnectionContext();
					documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
					applyType = reqSealInfo.get("APPLY_TYPE");
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					confirmStatus = reqSealInfo.get("CONFIRM_STATUS");
					
					//메일발송 필요 데이터 셋팅
					mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
                	mailData.put("SMR_NUMBER", smrNumber);
                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
                	mailData.put("TIME", format.format(new Date()));
                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
                	mailData.put("CONTENTS", smrNumber);
					
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
							status = "M";
							doAfterFlag = true;
							headerData.put("NEXT_PRC", "COMPLETE");
							headerData.put("SUX_WKEY1", smrNumber);
							doApprovalConfirmReady(ctx, allData);
							mailNow = false;
 						} else {
							status = "W";
							//승인모듈 히스토리 적재
							if(getStatus() == 1) {
								if(!srm.doAddProcessHistory("SEAL_COMPLETE")) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							} 
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
				case "DECIDE":
					ctx = getConnectionContext();
					documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
					applyType = reqSealInfo.get("APPLY_TYPE");
					smrNumber = reqSealInfo.get("SMR_NUMBER");
					confirmStatus = reqSealInfo.get("CONFIRM_STATUS");
					headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
					
					//메일발송 필요 데이터 셋팅
					mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
                	mailData.put("SMR_NUMBER", smrNumber);
                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
                	mailData.put("TIME", format.format(new Date()));
                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
                	mailData.put("CONTENTS", smrNumber);
                	
                	String decideScreenType = "";
                	switch (reqSealInfo.get("APP_TYPE")) {
					case "J01":
						decideScreenType = "MS01";
						break;
					case "J02":
						decideScreenType = "MS02";
						break; 
					case "J09":
						decideScreenType = "MS09";
						break;
					default:
						break;
					}
	     			
					if(!srm.checkAuthManager()) {
						srm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "DOC_DECIDE", smrNumber, null);
					}

					if("DECIDE_REQUEST".equals(confirmStatus)) {
						
						//메일 링크 생성
						HttpServletRequest decideReq = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
			 			String decideBaseURL = decideReq.getRequestURL().toString().replace(decideReq.getRequestURI(),"");
			 			
			 			SepoaLinkManager decideLinkManager = new SepoaLinkManager(info, conf, ctx);
			 			Map<String, String> decideLinkParams = new HashMap<String, String>();
			 			
			            List<Map<String,String>> appNoList = srm.doSearchAppNo(smrNumber);
			            String decideAppNo = "";
			            for(Map<String,String> appNoMap : appNoList) {
			            	decideAppNo = appNoMap.get("APP_NO");
			            }
			            String decideAppNoSeq = "1";
			            
			            decideLinkParams.put("APP_NO", decideAppNo);
			            decideLinkParams.put("APP_NO_SEQ", decideAppNoSeq);
			            decideLinkParams.put("SMR_NUMBER", smrNumber);
			            decideLinkParams.put("SCREEN_TYPE", decideScreenType);
			            decideLinkParams.put("APP_TYPE", reqSealInfo.get("APP_TYPE"));
			            decideLinkParams.put("RE_FLAG", reqSealInfo.get("RE_FLAG"));
			            decideLinkParams.put("SCREEN_MODE", "TODO");
			            
			            //인감 담당자 조회
	                	List<Map<String,String>> reqSealDocManagerList = srm.doSearchReqSealDocManager(smrNumber);
			            
	                	//인감 담당자별 링크 생성
			            for(Map<String,String> managerMap : reqSealDocManagerList) {
			            	if(!decideLinkManager.newLink("approvalm", decideAppNo, decideAppNoSeq) || !decideLinkManager.setParams(decideLinkParams)) {
				            	int errCode = decideLinkManager.getLastErrCode();
				            }else{
				            	String companyCode = info.getSession("COMPANY_CODE");
				            	String reqSealDocManager = managerMap.get("SEAL_MG_USER_ID");
				            	String addUserId = managerMap.get("ADD_USER_ID");
				            	String email = managerMap.get("EMAIL");
				            	String userNameLoc = managerMap.get("USER_NAME_LOC");
				            	String privateLink = decideLinkManager.addUser(companyCode, reqSealDocManager, decideLinkParams);
				            	
				            	Map<String,String> linkMap = new HashMap<String,String>();
				            	linkMap.put("SEAL_MG_USER_ID", reqSealDocManager);
				            	linkMap.put("URL", decideBaseURL + "/link.sux?ln=" + privateLink);
				            	linkMap.put("ADD_USER_ID", addUserId);
				            	linkMap.put("EMAIL", email);
				            	linkMap.put("USER_NAME_LOC", userNameLoc);
				            	linkMapList.add(linkMap);
				            }
			            }
						
						status = "DW";
						// 요청시에 인감담당자한테 권한을 줘야한다.
						if(documentList != null && documentList.size() > 0) {
							for(Map<String, Object> sealDoc : documentList) {
								List<Map<String, String>> selectSealList = (List<Map<String, String>>) sealDoc.get("SELECT_SEAL_LIST");
								for(Map<String, String> selectedSeal : selectSealList) {
									boolean subMg = true;
									boolean resMg = true;
									
									isGrant = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), selectedSeal.get("SEAL_MG_USER_ID"), Authorized.WRITE);
									Id_data.add(selectedSeal.get("SEAL_MG_USER_ID"));
									//mailData.put("SEAL_MG_USER_ID", selectedSeal.get("SEAL_MG_USER_ID"));
									//삭제예정
									if(selectedSeal.get("SUB_MG_USER_ID") != null && !"".equals(selectedSeal.get("SUB_MG_USER_ID"))) {
										isGrantSub = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), selectedSeal.get("SUB_MG_USER_ID"), Authorized.WRITE);
										Id_data.add(selectedSeal.get("SUB_MG_USER_ID"));
										//mailData.put("SEAL_MG_USER_ID", selectedSeal.get("SUB_MG_USER_ID"));
										
										if(!isGrantSub) {
											subMg = false;
										}
									}else if(selectedSeal.get("RES_MG_USER_ID") != null && !"".equals(selectedSeal.get("RES_MG_USER_ID"))){
										isGrantRes = srm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), selectedSeal.get("RES_MG_USER_ID"), Authorized.WRITE);
										Id_data.add(selectedSeal.get("RES_MG_USER_ID"));
										//mailData.put("SEAL_MG_USER_ID", selectedSeal.get("RES_MG_USER_ID"));
										
										if(!isGrantSub) {
											resMg = false;
										}
									}
									
									if(getStatus() == 1 && !isGrant || !subMg || !resMg) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
									
									//메일 셋팅
									mailData.put("SUBJECT", Cache.text.get("MAIL.DECIDE_REQUEST"));
									mailData.put("REQUESTER", info.getSession("NAME_LOC"));
									mailData.put("STEP", Cache.text.get("MAIL.DECIDE_STATUS_PROGRESS"));
									mailData.put("MESSAGE_TYPE", "DECIDE_REQ");
								}
							}
						}
						
					} else if("DECIDE_ACCEPT".equals(confirmStatus)) {
						
						// 메일 링크 생성
						HttpServletRequest decideReq = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
			 			String decideBaseURL = decideReq.getRequestURL().toString().replace(decideReq.getRequestURI(),"");
			 			
			 			SepoaLinkManager decideLinkManager = new SepoaLinkManager(info, conf, ctx);
			 			Map<String, String> decideLinkParams = new HashMap<String, String>();
			 			
			            List<Map<String,String>> appNoList = srm.doSearchAppNo(smrNumber);
			            String decideAppNo = "";
			            for(Map<String,String> appNoMap : appNoList) {
			            	decideAppNo = appNoMap.get("APP_NO");
			            }
			            String decideAppNoSeq = "1";
			            
			            decideLinkParams.put("APP_NO", decideAppNo);
			            decideLinkParams.put("APP_NO_SEQ", decideAppNoSeq);
			            decideLinkParams.put("SMR_NUMBER", smrNumber);
			            decideLinkParams.put("SCREEN_TYPE", decideScreenType);
			            decideLinkParams.put("APP_TYPE", reqSealInfo.get("APP_TYPE"));
			            decideLinkParams.put("RE_FLAG", reqSealInfo.get("RE_FLAG"));
			            decideLinkParams.put("SCREEN_MODE", "TODO");
			            
			            //인감 담당자 조회
	                	List<Map<String,String>> reqSealDocManagerList = srm.doSearchReqSealDocManager(smrNumber);
			            
	                	//인감 담당자별 링크 생성
			            for(Map<String,String> managerMap : reqSealDocManagerList) {
			            	if(!decideLinkManager.newLink("approvalm", decideAppNo, decideAppNoSeq) || !decideLinkManager.setParams(decideLinkParams)) {
				            	int errCode = decideLinkManager.getLastErrCode();
				            }else{
				            	String companyCode = info.getSession("COMPANY_CODE");
				            	String reqSealDocManager = managerMap.get("SEAL_MG_USER_ID");
				            	String addUserId = managerMap.get("ADD_USER_ID");
				            	String email = managerMap.get("EMAIL");
				            	String userNameLoc = managerMap.get("USER_NAME_LOC");
				            	String privateLink = decideLinkManager.addUser(companyCode, reqSealDocManager, decideLinkParams);
				            	
				            	Map<String,String> linkMap = new HashMap<String,String>();
				            	linkMap.put("SEAL_MG_USER_ID", reqSealDocManager);
				            	linkMap.put("URL", decideBaseURL + "/link.sux?ln=" + privateLink);
				            	linkMap.put("ADD_USER_ID", addUserId);
				            	linkMap.put("EMAIL", email);
				            	linkMap.put("USER_NAME_LOC", userNameLoc);
				            	linkMapList.add(linkMap);
				            }
			            }
						
						// 승인시에 반복문을 사용하여 인감담당자의 다른 승인이 남았는지 체크
						// 인감담당자의 승인이 남은 날인요청의 경우에는 status를 변경하지 않는다.
						// 먼저 본인 인감 승인 여부 변경해주고 시작
						Map<String, String> parameter = new HashMap<String, String>();
						
						smrNumber = reqSealInfo.get("SMR_NUMBER"); 
						parameter.put("SMR_NUMBER", smrNumber);
						for(Map<String, Object> docMap : documentList) {
							List<Map<String, String>> selectSealList = (List<Map<String, String>>) docMap.get("SELECT_SEAL_LIST");
							for(Map<String, String> selectedSeal : selectSealList) {
								if("C".equals(applyType)) {
									selectedSeal.put("SMR_NUMBER", smrNumber);
									selectedSeal.put("SMD_NUMBER", docMap.get("SMD_NUMBER").toString());
								}
								String sessionId = info.getSession("ID");
								// 현재세션(승인을 누른 인감담당자)의 아이디와 부서가 선택된 인감의 관리부서, 관리자ID랑 일치하여야한다.
 								if(sessionId.equals(selectedSeal.get("SEAL_MG_USER_ID")) || sessionId.equals(selectedSeal.get("SEAL_MG_USER_ID")) || 
 										sessionId.equals(selectedSeal.get("SEAL_MG_USER_ID")) || sessionId.equals(selectedSeal.get("RECE_USER_ID"))) {
									// 승인여부 변경 및 선택인감 상태 업데이트
									selectedSeal.put("DEC_FLAG", "Y");
									if(!srm.doUpdateSealDocSeal(selectedSeal)) {
										int errCode = srm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								}
							}
						}
						parameter.put("DEC_FLAG", "N");
						// 승인 여부가 N(미승인) 상태의 데이터가 한건도 없으면 다음모듈로 진행
						if(srm.doSelectReqSealDocSeal(parameter) == null) {
							status = "DC";

							//날인요청 요청자 조회, 메일 수신자
							List<Map<String,String>> docRequesterList = srm.doSearchDocRequestor(smrNumber);
							for(Map<String, String> docRequesterMap : docRequesterList) {
								linkMapList.add(docRequesterMap);
							}
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.DECIDE_COMPLETE"));
							mailData.put("REQUESTER", info.getSession("NAME_LOC"));
							mailData.put("STEP", Cache.text.get("MAIL.DECIDE_STATUS_COMPLETE"));
							mailData.put("MESSAGE_TYPE", "DECIDE_COMP");
 						} else {
							status = "DW";
							//검수 모듈 히스토리 적재
							if(getStatus() == 1) {
								if(!srm.doAddProcessHistory("DECIDE_COMPLETE")) {
									int errCode = srm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							} 
						}
					} else if("DECIDE_REJECT".equals(confirmStatus)) {
						// 메일 링크 생성
						HttpServletRequest decideReq = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
			 			String decideBaseURL = decideReq.getRequestURL().toString().replace(decideReq.getRequestURI(),"");
			 			
			 			SepoaLinkManager decideLinkManager = new SepoaLinkManager(info, conf, ctx);
			 			Map<String, String> decideLinkParams = new HashMap<String, String>();
			 			
			            List<Map<String,String>> appNoList = srm.doSearchAppNo(smrNumber);
			            String decideAppNo = "";
			            for(Map<String,String> appNoMap : appNoList) {
			            	decideAppNo = appNoMap.get("APP_NO");
			            }
			            String decideAppNoSeq = "1";
			            
			            decideLinkParams.put("APP_NO", decideAppNo);
			            decideLinkParams.put("APP_NO_SEQ", decideAppNoSeq);
			            decideLinkParams.put("SMR_NUMBER", smrNumber);
			            decideLinkParams.put("SCREEN_TYPE", decideScreenType);
			            decideLinkParams.put("APP_TYPE", reqSealInfo.get("APP_TYPE"));
			            decideLinkParams.put("RE_FLAG", reqSealInfo.get("RE_FLAG"));
			            decideLinkParams.put("SCREEN_MODE", "TODO");
			            
			            //인감 담당자 조회
	                	List<Map<String,String>> reqSealDocManagerList = srm.doSearchReqSealDocManager(smrNumber);
			            
	                	//인감 담당자별 링크 생성
			            for(Map<String,String> managerMap : reqSealDocManagerList) {
			            	if(!decideLinkManager.newLink("approvalm", decideAppNo, decideAppNoSeq) || !decideLinkManager.setParams(decideLinkParams)) {
				            	int errCode = decideLinkManager.getLastErrCode();
				            }else{
				            	String companyCode = info.getSession("COMPANY_CODE");
				            	String reqSealDocManager = managerMap.get("SEAL_MG_USER_ID");
				            	String addUserId = managerMap.get("ADD_USER_ID");
				            	String email = managerMap.get("EMAIL");
				            	String userNameLoc = managerMap.get("USER_NAME_LOC");
				            	String privateLink = decideLinkManager.addUser(companyCode, reqSealDocManager, decideLinkParams);
				            	
				            	Map<String,String> linkMap = new HashMap<String,String>();
				            	linkMap.put("SEAL_MG_USER_ID", reqSealDocManager);
				            	linkMap.put("URL", decideBaseURL + "/link.sux?ln=" + privateLink);
				            	linkMap.put("ADD_USER_ID", addUserId);
				            	linkMap.put("EMAIL", email);
				            	linkMap.put("USER_NAME_LOC", userNameLoc);
				            	linkMapList.add(linkMap);
				            }
			            }
						
						status = "DR";
						
 						Map<String, String> parameter = new HashMap<String, String>();
						
						smrNumber = reqSealInfo.get("SMR_NUMBER");
						parameter.put("SMR_NUMBER", smrNumber);
						parameter.put("DEC_FLAG", "N");

						if(!srm.doUpdateSealDocSeal(parameter)) {
							int errCode = srm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}		
						
						//날인요청 요청자 조회, 메일 수신자
						List<Map<String,String>> docRequesterList = srm.doSearchDocRequestor(smrNumber);
						for(Map<String, String> docRequesterMap : docRequesterList) {
							linkMapList.add(docRequesterMap);
						}
						
						//메일 셋팅
						mailData.put("SUBJECT", Cache.text.get("MAIL.DECIDE_REJECT"));
						mailData.put("REQUESTER", info.getSession("NAME_LOC"));
						mailData.put("STEP", Cache.text.get("MAIL.DECIDE_STATUS_REJECT"));
						mailData.put("MESSAGE_TYPE", "DECIDE_REJECT");
					}
					mailNow = true;
					break;
					
				case "REAL_SEAL_DOC_UPDATE":
					documentList = (List<Map<String, Object>>)parseMap.get("DOCUMENT_LIST");
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
	     			

		     		HttpServletRequest req = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
	    	 		String baseURL = req.getRequestURL().toString().replace(req.getRequestURI(),"");
	    	 		mailData.put("URL",baseURL);
	     			
	     			
	     			if(Id_data.size()>0) {
	     				String beforeId = "";
	    	 			for(String id :Id_data) {
	    	 				for(Map<String,String> linkMap : linkMapList) {
	    	 					if(beforeId.equals(linkMap.get("SEAL_MG_USER_ID"))){ // 중복 발신 금지
	    	 						continue;
	    	 					}
	    	 					if(id.equals(linkMap.get("SEAL_MG_USER_ID"))) {
	    	 						mailData.put("SEAL_MG_USER_ID", linkMap.get("SEAL_MG_USER_ID"));
	    	    	 				mailData.put("URL", linkMap.get("URL"));
	    	    	 				
	    	    	 				SepoaMessage message = new SepoaMessage();
	    	    	 				message.addParams(mailData);
	    	    					message.setMessageType(mailData.get("MESSAGE_TYPE"));
	    	    					message.addReceiver(linkMap.get("SEAL_MG_USER_ID"), linkMap.get("USER_NAME_LOC"), linkMap.get("EMAIL"), null);
	    	    					mailpush.addMessage(message);
	    	                    	mailpush.accept();
	    	                    	
	    	                    	beforeId = id;
	    	 					}
	    	 				}
	    	 			}
    	 			}else {
    	 				SepoaMessage message = new SepoaMessage();
    	 				//완료, 반려의 경우 어떤 링크든 상관없으므로 첫번째 링크 임의 지정
    	 				if(linkMapList.size() > 0) {
    	 					mailData.put("SEAL_MG_USER_ID", linkMapList.get(0).get("SEAL_MG_USER_ID"));
    	 					mailData.put("URL", linkMapList.get(0).get("URL"));
    	 					message.addReceiver(linkMapList.get(0).get("ADD_USER_ID"), linkMapList.get(0).get("USER_NAME_LOC"), linkMapList.get(linkMapList.size()-1).get("EMAIL"), null);
    	 				}
    	 				message.addParams(mailData);
    					message.setMessageType(mailData.get("MESSAGE_TYPE"));
    					mailpush.addMessage(message);
                    	mailpush.accept();
    	 			}
                	
	        		Logger.debug.println("##### 메일 발송 성공 #####");
				} catch (Exception e2) {
					Logger.err.println("$$$$$ 메일 발송 실패 $$$$$");
					e2.printStackTrace();
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
				case "COMPLETE":
					status = "C";
					break;
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
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
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
				for(Map<String, String> resultMap : searchResult) {
					resultMap.get("MANAGE_NUMBER");
					List<Map<String, String>> resultList = srm.doSearchExtUseShow(resultMap);
					
					if(resultList.size() > 0) {
						resultMap.put("COMPLETE_FLAG", "N");
					}else {
						resultMap.put("COMPLETE_FLAG", "Y");
					}
				}
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
			
			List<Map<String, String>> fileList = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
			
			//파일 업로드 전처리
			boolean isPreProcess = SepoaModuleService.checkBusiness(config, info, "SID1681979423086");
			if(getStatus() == 1 && isPreProcess && "E".equalsIgnoreCase(headerData.get("RE_FLAG"))) {
				Map<String, Object> preProcessData = new HashMap<String, Object>();
				preProcessData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, (String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				preProcessData.put("HEADER_DATA", headerData);
				preProcessData.put("SMR_MANAGER", sm);
				preProcessData.put("FILE_LIST", fileList);
				
				SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.preprocess.SepoaReqSealDocUploadPreProcessor", getConnectionContext(), "doProcess", new Object[] {preProcessData});
				
				if(so == null) {
					setStatus(102308);
				} else {
					if(so.status != 1 || !so.flag) {
						setStatus(so.status);
					}
				}
			}
			
			Map<String, String> storedFileData = null;
			if(getStatus() == 1) {
				for(Map<String, String> file : fileList) {
					storedFileData = sm.doUploadDocumentFile("REQ_SEAL", file, headerData);
					break;
				}
				
				if(storedFileData == null) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			//파일 업로드 후처리
			boolean isPostProcess = SepoaModuleService.checkBusiness(config, info, "SID1681982432278");
			if(getStatus() == 1 && isPostProcess && "E".equalsIgnoreCase(headerData.get("RE_FLAG"))) {
				Map<String, Object> postProcessData = new HashMap<String, Object>();
				postProcessData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, (String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				postProcessData.put("HEADER_DATA", headerData);
				postProcessData.put("SMR_MANAGER", sm);
				postProcessData.put("UPLOAD_FILE", storedFileData);
				
				SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.postprocess.SepoaReqSealDocUploadPostProcessor", getConnectionContext(), "doProcess", new Object[] {postProcessData});
				
				if(so == null) {
					setStatus(102308);
				} else {
					if(so.status != 1 || !so.flag) {
						setStatus(so.status);
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
			if(getStatus() == 1) setStatus(0);
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
			
			if(StringUtils.isBlank(headerData.get("KEY"))) {
				setStatus(103338);
			}
			
			if(getStatus() == 1 && headerData.containsKey("EDITOR_HTMLS")) {
				Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
				SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
				sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				sm.setDocBaiscInfo(headerData);
				
				if(((List)jsonData.get("EDITOR_HTMLS")).size() == 0) {
					List<Map<String, String>> pageList = sm.doListImage();
					List<Map<String, String>> dummyPageDataList = new ArrayList<Map<String, String>> ();
					
					for(Map<String, String> page : pageList) {
						Map<String, String> dummyPageData = new HashMap<String, String>();
						dummyPageData.put("DOC_NUMBER", page.get("DOC_NUMBER"));
						dummyPageData.put("FILE_ID", page.get("FILE_ID"));
						dummyPageData.put("LAYER", page.get("LAYER"));
						dummyPageData.put("FIX_HTML", "");
						
						dummyPageDataList.add(dummyPageData);
					}
					
					if(!sm.doUpdateEditorHTML(dummyPageDataList)) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else {
					if(!sm.doUpdateEditorHTML((List<Map<String, String>>) jsonData.get("EDITOR_HTMLS"))) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
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
	
	@Description("편집기 파일 삭제")
	@SuppressWarnings("unchecked")
	public SepoaOut doDeleteDocumentFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			if(StringUtils.isBlank(headerData.get("KEY"))) {
				setStatus(103338);
			}
			String caller = null;
			if(getStatus() == 1) {
				caller = MapUtils.getString(headerData, "CALLER", null);
				if(caller == null) {
					setStatus(103338);
				}
			}
			
			if(getStatus() == 1) {
				SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
				sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				sm.setDocBaiscInfo(headerData);
				
				String[] keyArr = headerData.get("KEY").split("_");
				
				if(caller.endsWith("REQ_SEAL")) {
					String smrNumber = keyArr[0];
					String smdNumber = keyArr[1];
					headerData.put("SMR_NUMBER", smrNumber);
					headerData.put("SMD_NUMBER", smdNumber);
					
					Map<String, String> map = sm.doSelectDocumentFile();
					if(map == null) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					if (getStatus() == 1 && !sm.doDeleteDocumentFile(map)) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else if(caller.endsWith("ATTACH_DOC_TEMPLATE")) {
					String smiNumber = keyArr[0];
					headerData.put("SMI_NUMBER", smiNumber);
					
					List<Map<String, String>> attachTemplateData = sm.doSelectAttachDocTemplateFile();
					if(attachTemplateData == null) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					if(getStatus() == 1 && !sm.doDeleteAttachDocTemplateFile(attachTemplateData.get(0))) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else if(caller.endsWith("REQ_SEAL_HISTORY")) {
					String smrNumber = keyArr[0];
					headerData.put("SMR_NUMBER", smrNumber);
					
					List<Map<String, String>> historyFileList = sm.doSelectReqSealHistoryFile();
					if(historyFileList == null) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					if (getStatus() == 1 && !sm.doDeleteReqSealHistoryFile(historyFileList.get(0))) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else if(caller.endsWith("CONTRACT_HISTORY")) {
					String contNo = keyArr[0];
					String contGLSeq = keyArr[1];
					headerData.put("CONT_NO", contNo);
					headerData.put("CONT_GL_SEQ", contGLSeq);
					
					List<Map<String, String>> historyFileList = sm.doSelectContractHistoryFile();
					if(historyFileList == null) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					if (getStatus() == 1 && !sm.doDeleteContractHistoryFile(historyFileList.get(0))) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else if(caller.endsWith("REQ_SEAL_REAL_DOC" )) {
					String smrNumber = keyArr[0];
					String smdNumber = keyArr[1];
					headerData.put("SMR_NUMBER", smrNumber);
					headerData.put("SMD_NUMBER", smdNumber);
					
					List<Map<String, String>> realDocList = sm.doSelectReqSealRealDocFile();
					if(realDocList == null) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					if (getStatus() == 1 && !sm.doDeleteReqSealRealDocFile(realDocList.get(0))) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else if(caller.endsWith("EXT_SEAL_REAL_DOC")) {
					//반출 실날인서류 삭제 구간인데.. 제어부가 애초에 날인요청 서비스에 밖에 없어서 여기다 구현한다.
					
					String smrNumber = keyArr[0];
					String smdNumber = keyArr[1];
					headerData.put("SMR_NUMBER", smrNumber);
					headerData.put("SMD_NUMBER", smdNumber);
					
					SepoaExtSealManager sem = new SepoaExtSealManager(info, config, getConnectionContext());
					sem.setDocBaiscInfo(headerData);
					List<Map<String, String>> realDocList = sem.doSelectExtSealRealDocFile();
					if(realDocList == null) {
						int errCode = sem.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					if (getStatus() == 1 && !sem.doDeleteExtSealRealDocFile(realDocList.get(0))) {
						int errCode = sem.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else if(caller.endsWith("EXT_SEAL_DOC")) {
					//반출 실날인서류 삭제 구간인데.. 제어부가 애초에 날인요청 서비스에 밖에 없어서 여기다 구현한다.
					
					String smrNumber = keyArr[0];
					String smdNumber = keyArr[1];
					headerData.put("SMR_NUMBER", smrNumber);
					headerData.put("SMD_NUMBER", smdNumber);
					
					SepoaExtSealManager sem = new SepoaExtSealManager(info, config, getConnectionContext());
					sem.setDocBaiscInfo(headerData);
					List<Map<String, String>> realDocList = sem.doSelectExtSealDocFile();
					if(realDocList == null) {
						int errCode = sem.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					if (getStatus() == 1 && !sem.doDeleteExtSealDocFile(realDocList.get(0))) {
						int errCode = sem.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
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
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaReqSealManager sm = getSepoaReqSealManager(info, config, getConnectionContext());
			sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			headerData.put("KEY", headerData.get("KEY"));
			headerData.put("LAYER", headerData.get("LAYER"));
			sm.setDocBaiscInfo(headerData);
			
			List<Map<String, String>> fileList =  null;
			
			List<String> layers = Arrays.asList(MapUtils.getString(headerData, "LAYER", "REQ_SEAL").split("#"));
			for(String layer : layers) {
				fileList = sm.doListImage(layer);
				if(fileList != null) {
					break;
				}
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
			String legalMenuProfilecode = CommonUtil.getConfig("sepoa.legal.porfile.code");	// 인감책임자
			String ctrlCode = info.getSession("CTRL_CODE").replaceAll("\\s", "");
			List<String> ctrlCodeList = Arrays.asList(ctrlCode.split(","));
			headerData.put("F_KEYWORD", MapUtils.getString(headerData, "F_KEYWORD", "").trim()); 
			
			if(headerData.get("SUMMARY")!=null && headerData.get("SUMMARY").equals("true")) { // SUMMARY에서 조회일 경우
				headerData.put("APPLY_TYPE", "C,U,D"); // APPLY_TYPE 설정 (C=법인인감, U=사용인감, D=증명서)
			}
			
			if(StringUtils.isNotBlank(mode)) {
				if("LIST".equals(mode) || "LIST_COUNT".equals(mode)) {
					boolean viewSameDept = SepoaModuleService.checkBusiness(config, info, "SRL1655791676756");	// 같은 부서원의 신청내역을 볼 수 있는지
					// 관리자 / SEAL_CONFIRM / 전체조회 허용 부서(총무·법무 등)가 아니면 본인 또는 동일부서로 제한
					if (!canViewAllSealRequestList(systemAdminMenuProfilecode, ctrlCodeList)) {
						if (viewSameDept) {
							headerData.put("REQ_USER_DEPT", info.getSession("DEPARTMENT"));
						} else {
							headerData.put("REQ_USER_ID", info.getSession("ID"));
						}
					} else {
						// 전체 조회: 본인/부서 필터 제거
						headerData.remove("REQ_USER_ID");
						headerData.remove("REQ_USER_DEPT");
					}
				} else if ("TODO".equals(mode) || "TODO_COUNT".equals(mode)) {
					if (myMenuProfile.equals(systemAdminMenuProfilecode)) {	// 관리자
						headerData.put("SMR_STATUS", "G,W,DW");
					} else if (ctrlCodeList.contains("SEAL_CONFIRM")) {	// 날인승인권한
						headerData.put("SMR_STATUS", "G,W,DW");
					} else if (myMenuProfile.equals(managerMenuProfilecode)) {// 인감담당자
						headerData.put("SMR_STATUS", "W,DW");	// 인감담당자 승인 진행중
						headerData.put("MY_SEAL", "true");	// 본인 소유의 인감만 조회
						headerData.put("ACCEPT_FLAG", "N");	// 승인하지 않은 인감만 조회
					} else if (myMenuProfile.equals(legalMenuProfilecode)) {// 인감책임자
						headerData.put("SMR_STATUS", "W,DW");	// 인감책임자가 담당자일 때 승인 진행중
						headerData.put("MY_SEAL", "true");	// 본인 소유의 인감만 조회
						headerData.put("ACCEPT_FLAG", "N");	// 승인하지 않은 인감만 조회
					} else {
						if ("TODO_COUNT".equals(mode)) {
							setValue(0);
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

			/*
				팀장일 경우, 부서원 모든 건 조회 가능
				단, 관리자 / SEAL_CONFIRM / 전체조회 허용 부서면 전사 조회이므로 팀장 체크 생략
			*/
			if (!canViewAllSealRequestList(systemAdminMenuProfilecode, ctrlCodeList)) {
				boolean isTeamManager = srm.doSelectIsTeamManager();
				if (isTeamManager) {
					headerData.put("REQ_USER_DEPT", info.getSession("DEPARTMENT"));
					headerData.remove("REQ_USER_ID");
				}
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
					boolean isResultDataPostPrc = SepoaModuleService.checkBusiness(new Configuration(), info, "SRL1687322155554");
					if(isResultDataPostPrc) {
						SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.postprocess.ReqeustListPostProcessor", getConnectionContext(), "reqSealDoProcess", new Object[] {resultList});
						if(out == null || out.status != 1 || !out.flag) {
							setStatus(102012); // 날인신청현황 후처리 서비스에서 문제가 발생하였습니다.
						} else {
							resultList = (List<Map<String, String>>) out.getData(0);
						}
					}
					if(headerData.containsKey("RETURN_TYPE") && "JSON".equalsIgnoreCase(headerData.get("RETURN_TYPE"))) {
						setValue(new Gson().toJson(resultList));
					} else {
						setValue(resultList);
					}
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
				paramHeader.put("SUMMARY", "true");
				so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaReqSealService", ctx, "doSelectRequestList", new Object[] {paramMap});
				int listSize = Integer.parseInt(so.result[0]);
				countMap.put("REQ_SEAL_COUNT_"+status, listSize);
			}

			Map<String, String> paramHeader = new HashMap<String, String>();
			paramMap.put(SepoaDataMapper.KEY_HEADER_DATA, paramHeader);
			paramHeader.put("MODE", "TODO_COUNT");
			paramHeader.put("SUMMARY", "true");
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
	
	@Description("교부서류 현황 템플릿 파일업로드 서비스")
	@SuppressWarnings("unchecked")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doUploadCorpAttachDocTemplateFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> fileList = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> smiData = new Gson().fromJson(MapUtils.getString(headerData, "DOC_DATA", "{}"), Map.class);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(smiData);
			
			Map<String, String> uploadFileData = null;
			if(getStatus() == 1) {
				for(Map<String, String> fileData : fileList) {
					uploadFileData = srm.doUploadAttachDocTemplateFile("ATTACH_DOC_TEMPLATE", fileData, smiData);
					break;
				}
				
				if(uploadFileData == null) {
					int errCode = srm.getLastErrCode();
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
			if(getStatus() == 1) setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("교부서류 현황 템플릿 파일조회 서비스")
	@SuppressWarnings("unchecked")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doSelectCorpAttachDocTemplateFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> smiData = new Gson().fromJson(MapUtils.getString(headerData, "DOC_DATA", "{}"), Map.class);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(smiData);
			
			List<Map<String, String>> attachTemplateDocList = srm.doSelectAttachDocTemplateFile();
			Map<String, String> returnParams = new HashMap<String, String>();
			if(attachTemplateDocList == null || attachTemplateDocList.size() <= 0) {
				returnParams.put("RESULT", "ATTACH_DOC_EMPTY");
			} else {
				returnParams.put("RESULT", new Gson().toJson(attachTemplateDocList));
			}
			
			setValue(new Gson().toJson(returnParams));
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("교부서류 현황 리스트 서비스")
	@SuppressWarnings("unchecked")
	@Permission(key="WORK_TYPE", value="Z")
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
			
			// 실제첨부파일이 없고, SF_KEY만있을경우 초기화
			for(int index=0; index < resultList.size(); index++) {
				Map<String, String> result = resultList.get(index); {
					Map<String, String> data = new HashMap<String,String>();
					data.put("attach_key", result.get("ATTACH_NO"));
					if(!"".equals(data.get("attach_key"))) {
						Object[] obj = { data };
						SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.fi.FI_001", "TRANSACTION", "getSFileInfo", obj);
						if(so != null && so.flag && so.status == 1) {
							List<Map<String,String>> fileData = new SepoaFormater(so.result[0]).getListMap();
							if(fileData.size() < 1 && !"".equals(result.get("ATTACH_NO"))) {
								result.put("ATTACH_NO", "");
								resultList.set(index, result);
							}
						}
					}
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
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doSaveCorpAttachDocHeader(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			
			if(SepoaModuleService.checkBusiness(new Configuration(), info, "STIF1686537827571")) {
				for(Map<String, String> rows : gridData) {
					if(!"1".equalsIgnoreCase(rows.get("REAL_DOC_FLAG"))) {
						Map<String, String> docData = new HashMap<>();
						docData.putAll(rows);
						docData.put("KEY", rows.get("SMI_NUMBER"));
						docData.put("LAYER", "ATTACH_DOC_TEMPLATE");
						srm.setDocBaiscInfo(docData);
						
						List<Map<String, String>> docList = srm.doListImage();
						if(docList == null || docList.size() == 0) {
							setStatus(102010);
							break;
						}
					}
				}
			}
			
			if(getStatus() == 1) {
				for(Map<String, String> rows : gridData) {
					if(!srm.doSaveCorpAttachDocHeader(rows)) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							break;
						}
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
	@Permission(key="WORK_TYPE", value="Z")
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
			String screenType = headerData.get("SCREEN_TYPE");
			
			List<Map<String, String>> otherFileList = new ArrayList<Map<String, String>>();
			Map<String, String> otherFileNo = new HashMap<String, String>();

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
						if("M".equalsIgnoreCase(screenType) && !rows.get("OTHER_FILE_NO").equals("")) {
							otherFileNo.put("attach_key", rows.get("OTHER_FILE_NO"));
							Object[] obj = { otherFileNo };
							SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.fi.FI_001", "TRANSACTION", "getSFileInfo", obj);
							if(so != null && so.flag && so.status == 1) {
								List<Map<String,String>> otherFiles = new SepoaFormater(so.result[0]).getListMap();
								if(otherFiles.size() > 0) {
									for (Map<String, String> res : otherFiles) {
										res.put("ATTACH_KEY", rows.get("OTHER_FILE_NO"));
										otherFileList.add(res);
									}
								}
							}
						}
						if(docSealList != null && docSealList.size() > 0) {
							for(Map<String, String> sealRow : docSealList) {
								sealList.add(sealRow);
							}
						}
					}
					resultMap.put("DOCUMENT_LIST", reqDocList);
					resultMap.put("SELECTED_SEAL_LIST", sealList);
					if("M".equalsIgnoreCase(screenType) && otherFileList != null) {
						resultMap.put("OTHER_FILE_LIST", otherFileList);
					}
				} 
				//if("C".equals(applyType)) {
					attachList = srm.doSelectCorpAttachDoc(headerData);
					if(attachList != null) {
						resultMap.put("ATTACH_DOCUMENT_LIST", attachList);
					}
				//}
			}
			
			boolean useExtraDataFlag = SepoaModuleService.checkBusiness(new Configuration(), info, "USW1679981296396");
			boolean corpExtraDataFlag = SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1679981321802");
			if(getStatus() == 1 && ( useExtraDataFlag ||corpExtraDataFlag)) {
				Map<String, String> extraInfo = srm.doSelectExtraInfo(headerData);
				if(extraInfo != null) {
					resultMap.put("EXTRA_INFO", extraInfo);
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
				
				// 회수 가능한 상태인지 확인 (후처리 인터페이스가 있다면 프론트 검사만으로 종결한다)
				if(!SepoaModuleService.checkBusiness(new Configuration(), info, "SR1691301437948") && !srm.doCheckReqSealWithdrawable(rows.get("SMR_NUMBER"))) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				String status = "T";
				// 회수 후 재사용 불가 모듈 체크
				if(SepoaModuleService.checkBusiness(new Configuration(), info, "SR1687857484836")) {
					status = "Z";
				}
				
				// 날인요청 회수
				if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus(status)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 회수로 인한 기존 AccessLink 삭제
				List<Map<String, String>> appDataList = srm.doSearchAppNo(rows.get("SMR_NUMBER"));
				String docNumber = appDataList.get(0).get("APP_NO");
				
				if(getStatus() == 1 && srm.expireAccessLink(docNumber)) {
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
				
				// 회수 이후 별도 프로세스 진행 할 것인가 CBO판단
				if(SepoaModuleService.checkBusiness(new Configuration(), info, "SR1691301437948")) {
					SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.postprocess.SepoaReqSealWithDrawPostProcessor", getConnectionContext(), "doProcess", new Object[] {rows});
					if(out == null) {
						setStatus(107003);
					} else if(out != null && out.status != 1) {
						setStatus(107003);
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

			// 검수기능 사용할경우, 검수과정은 날인요청완료된 이후의 프로세스이므로 표출
			if (SepoaModuleService.checkBusiness(new Configuration(), info, "CSW1687399123985") || SepoaModuleService.checkBusiness(new Configuration(), info, "USW1687397809419")) {
				headerData.put("SMR_STATUS", "C,DW,DC,DR");
			} else {
				headerData.put("SMR_STATUS", "C");
			}
			
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			
			if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {	// 관리자
				headerData.put("MANAGER_ID", "");
				headerData.put("CONFIRM_USER_ID", "");
			} else if (info.getSession("CTRL_CODE").toUpperCase().contains("SEAL_CONFIRM")) {	// 직무정보에 날인요청 승인자로 등록된사람
				headerData.put("CONFIRM_USER_ID", "");
				headerData.put("MANAGER_ID", "");
			} else if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.seal.manager.porfile.code")) ||
					myMenuProfile.equals(CommonUtil.getConfig("sepoa.legal.porfile.code"))) {	// 인감담당자 or 인감책임자
				headerData.put("CONFIRM_USER_ID", info.getSession("ID"));
				headerData.put("MANAGER_ID", info.getSession("ID"));
				headerData.put("MY_SEAL", "true");	// 본인 소유의 인감만 조회
				headerData.put("ACCEPT_FLAG", "N");	// 승인하지 않은 인감만 조회
			} else {
				int errCode = 200051;	// 권한이 없는 사용자입니다.
				setStatus(errCode);
				throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+errCode));
			}
			
			if (SepoaModuleService.checkBusiness(new Configuration(), info, "USW1637309480717")) {
				headerData.put("SEAL_MANAGER_CONFIRM_USER_FLAG", "TRUE");
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
			
			String key = headerData.get("KEY");
			if(StringUtils.isBlank(headerData.get("KEY"))) {
				setStatus(103338);
			}
			
			String caller = null;
			if(getStatus() == 1) {
				caller = MapUtils.getString(headerData, "CALLER", null);
				if(caller == null) {
					setStatus(103338);
				}
			}
			
			if(getStatus() == 1) {
				String[] keyArr = headerData.get("KEY").split("_");
				if(caller.endsWith("REQ_SEAL")) {
					String smrNumber = keyArr[0];
					String smdNumber = keyArr[1];
					
					headerData.put("SMR_NUMBER", smrNumber);
					headerData.put("SMD_NUMBER", smdNumber);
				} else if(caller.endsWith("ATTACH_DOC_TEMPLATE")){
					//다운로드는 날인요청 건 외 아직은 예정없는 듯?
					String smrNumber = keyArr[0];
					String sdNumber = keyArr[1];
					
					headerData.put("SMR_NUMBER", smrNumber);
					headerData.put("SD_NUMBER", sdNumber);
				} else if(caller.endsWith("REQ_SEAL_REAL_DOC")){
					//실날인서류 편집기모듈 사용시
					String smrNumber = keyArr[0];
					String smdNumber = keyArr[1];
					
					headerData.put("SMR_NUMBER", smrNumber);
					headerData.put("SMD_NUMBER", smdNumber);
				}
			}
			
			List<Map<String, String>> docList = null;
			String downTarget = "";
			if(getStatus() == 1) {
				Map<String, String> paramMap = new HashMap<String, String>();
				for(Entry<String, String> entry : headerData.entrySet()) {
					paramMap.put(entry.getKey(), entry.getValue());
				}
				srm.setDocBaiscInfo(paramMap);
				
				boolean checkYn = true;
				
				boolean useSealModule = SepoaModuleService.checkBusiness(new Configuration(), info, "US1638852635132");
				boolean certModule = SepoaModuleService.checkBusiness(new Configuration(), info, "CERT1686730093044");
				boolean summaryModule = SepoaModuleService.checkBusiness(new Configuration(), info, "SUM1693975008371");
				
				switch(caller.toUpperCase()) {
					case "USE_REQ_SEAL":
					case "CORP_REQ_SEAL":
						if (useSealModule || summaryModule) {
							checkYn = srm.doCheckFileDownload(paramMap);
						}
						downTarget = "REQ_SEAL";
						break;
					case "ATTACH_DOC_TEMPLATE":
						if (certModule) {
							checkYn = srm.doCheckFileDownload(paramMap);
						}
						downTarget = "ATTACH_DOC_TEMPLATE";
						break; 
					case "REQ_SEAL_REAL_DOC":
						checkYn = srm.doCheckFileDownload(paramMap);
						downTarget = "REQ_SEAL_REAL_DOC";
						break; 
				}
				
				
				if(checkYn) {
					switch(caller.toUpperCase()) {
						case "USE_REQ_SEAL":
						case "CORP_REQ_SEAL":
							docList = srm.doSelectReqSealDoc(paramMap);
							break;
						case "ATTACH_DOC_TEMPLATE":
							docList = srm.doSelectCorpAttachDoc(paramMap);
							break;
						case "REQ_SEAL_REAL_DOC":
							docList = srm.doSelectReqSealRealDocFile();
							break;
					}
				} else {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						setFlag(false);
						setValue("");
						return getSepoaOut();
					}
				}
				
				if(docList == null) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			Map<String, String> fileInfo = null;
			if(getStatus() == 1) {
				fileInfo = srm.doDocumentDownload(downTarget, docList, true, key);
				
				if(fileInfo == null) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			if(getStatus() == 1) {
				Boolean res = srm.addDownloadCount(docList);
				if (res) {
					addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
					
					if(SepoaModuleService.checkBusiness(new Configuration(), info, "SID1681893415740")) {
						addParams("SepoaActionController.KEY_FILE_NAME", fileInfo.get("FILE_NAME_CLIENT"));
						fileInfo.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, (String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
						SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.download.SepoaReqSealCustomDown", getConnectionContext(), "doProcess", new Object[] {fileInfo});
						if(out == null || out.status != 1 || !out.flag) {
							setStatus(103401); // 파일 다운로드 후처리 서비스 중 에러가 발생하였습니다.
						} else {
							setValue(out.getData(0));
						}
					} else {
						setValue(fileInfo);
					}
				} else {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setValue("");
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				srm.doAddProcessHistory("DOC_DOWNLOAD");
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

	@Description("날인확정서류 전체 ZIP 다운로드")
	@SuppressWarnings("unchecked")
	public SepoaOut doRealAttachDocumentDownloadAll(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			ConnectionContext ctx = getConnectionContext();
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			
			String smrNumber = MapUtils.getString(headerData, "SMR_NUMBER", "");
			String subject = MapUtils.getString(headerData, "SUBJECT", "");
			if(StringUtils.isBlank(smrNumber)) {
				setStatus(103338);
			}
			
			Map<String, String> fileInfo = null;
			if(getStatus() == 1) {
				Map<String, String> paramMap = new HashMap<String, String>();
				paramMap.put("SMR_NUMBER", smrNumber);
				paramMap.put("SUBJECT", subject);
				paramMap.put("CALLER", "REQ_SEAL_REAL_DOC");
				srm.setDocBaiscInfo(paramMap);
				
				String zipFileName = StringUtils.isNotBlank(subject) ? subject : smrNumber;
				fileInfo = srm.doRealAttachDocumentDownloadAll(smrNumber, zipFileName);
				if(fileInfo == null) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					} else {
						setStatus(100511);
					}
				}
			}
			
			if(getStatus() == 1) {
				addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
				
				if(SepoaModuleService.checkBusiness(new Configuration(), info, "SID1681893415740")) {
					addParams("SepoaActionController.KEY_FILE_NAME", fileInfo.get("FILE_NAME_CLIENT"));
					fileInfo.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, (String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
					SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.download.SepoaReqSealCustomDown", getConnectionContext(), "doProcess", new Object[] {fileInfo});
					if(out == null || out.status != 1 || !out.flag) {
						setStatus(103401);
					} else {
						setValue(out.getData(0));
					}
				} else {
					setValue(fileInfo);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setValue("");
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				srm.doAddProcessHistory("DOC_DOWNLOAD");
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

	@Description("편집모듈 파일 출력 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doDocumentPrint(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			SepoaOut so = doDocumentDownload(allData);
			if (so.flag && so.getDataCount() > 0) {
				Map<String, String> fileData = (Map<String, String>) so.getData(0);
				String filePath = fileData.get("FILE_PATH");

				InputStream in = CipherManager.getCipher().getInputStream(new File(filePath), false);
				byte[] b = IOUtils.toByteArray(in);
				IOUtils.closeQuietly(in);
				setObject(b);
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
			srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));

			byte[] pdfBytes = null;
			if ("SEALREGIST".equals(headerData.get("TYPE"))) {// 인감등록 증명서
				pdfBytes = srm.makeRegistrationCertificate();
			} else if ("AUDITTRAIL".equals(headerData.get("TYPE"))) {// 날인이력 보고서
				pdfBytes = srm.makeAuditTrailCertificate();
			} else if ("CERT_AUDITTRAIL".equalsIgnoreCase(headerData.get("TYPE"))) {
				pdfBytes = srm.makeCertAuditTrailCertificate();
			}

			if (pdfBytes == null) {
				int errCode = srm.getLastErrCode();
				if (errCode != 0) {
					setStatus(errCode);
				}
			}

			if (getStatus() == 1) {
				if (SepoaModuleService.checkBusiness(new Configuration(), info, "MA1693206115042")) {
					addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
					Map<String, Object> fileInfo = new HashMap<>();
					fileInfo.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, (String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
					fileInfo.put("FILE_NAME_CLIENT", headerData.get("SMR_NUMBER") + "_REPORT.pdf");
					fileInfo.put("FILE_INPUT_STREAM", new ByteArrayInputStream(pdfBytes));
					setValue(fileInfo);
				} else {
					setObject(pdfBytes);
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
			
			if(headerData.get("PARAMETER") != null) {
				String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
				Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
				Map<String, String> reqSealInfo = (Map<String, String>)parseMap.get("REQ_SEAL_INFO");
				if(reqSealInfo.size() >0) {
					headerData.put("HOUSE_CODE", reqSealInfo.get("HOUSE_CODE"));
					headerData.put("COMPANY_CODE", reqSealInfo.get("COMPANY_CODE"));
					sealInfo.put("RE_FLAG", reqSealInfo.get("RE_FLAG"));
					sealInfo.put("ACCEPT_REASON", reqSealInfo.get("ACCEPT_REASON"));
				}
			}
			
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
			
			//날인완료 히스토리 적재
			if(getStatus() == 1) {
				if(!srm.doAddProcessHistory("SEAL_COMPLETE")) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
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

		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			if(SepoaApproval.APP_STATUS_FINAL.equals(MapUtils.getString(headerData, "APP_STATUS", ""))){
				SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
				
				
				String smrNumber = headerData.get("SUX_WKEY1");
				Map<String, String> sealInfo = new HashMap<String, String>();
				sealInfo.put("SMR_NUMBER", smrNumber);
				sealInfo.put("ACCEPT_REASON", headerData.get("ACCEPT_REASON"));
				sealInfo.put("CONFIRM_DATE", SepoaDate.getShortDateString());
				sealInfo.put("CONFIRM_TIME", SepoaDate.getShortTimeString());
				String confirmUserIdForced = MapUtils.getString(headerData, "CONFIRM_USER_ID_FORCED", null);
				sealInfo.put("CONFIRM_USER_ID", confirmUserIdForced == null ? info.getSession("ID") : confirmUserIdForced);
				
				if(SepoaModuleService.checkBusiness(new Configuration(), info, "STIF1686537827571")) {
					// 인장 치환 처리
					sealInfo.put("SMR_STATUS", "CR");
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
					sealInfo.put("APPLY_FOR", "RD"); //증명서 발급신청 전용 APPLY_FOR
					
					if(getStatus() == 1 && !srm.doAddPostProcessLine(sealInfo)) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else {
					sealInfo.put("SMR_STATUS", "C");
					srm.setDocBaiscInfo(sealInfo);
					
					// 날인요청 Status 업데이트
					if(getStatus() == 1 && !srm.doUpdateReqSealDoc(sealInfo)) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				// 증명서발급신청 완료 히스토리 적재
				if(getStatus() == 1) {
					if(!srm.doAddProcessHistory("CERT_COMPLETE")) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
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
			
			List<Map<String, String>> reqSealInfoList = srm.doSelectRequestList();
			if(reqSealInfoList == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				Map<String, String> reqSealInfo = reqSealInfoList.get(0);
				pm.setLayer(PathManager.LAYER_TEMP);
				String tempFilePath = pm.getUniquePath();
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, tempFilePath);
				srm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				
				if("D".equalsIgnoreCase(reqSealInfo.get("APPLY_TYPE"))) {
					//교부서류 컨버팅
					if(!srm.doConvertCertDocument()) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} else{
					// 날인 문서 컨버팅
					if(!srm.doConvertReqSealDocument()) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				// 메일 발송 작업
				
				List<Map<String, String>> reqDuplicateList = srm.doSelectMailReqSealDoc(reqSealInfo);
				if(reqDuplicateList != null && reqDuplicateList.size() > 0) {
					Map<String, String> reqDeuplicateMap = reqDuplicateList.get(0);
					
					List<Map<String, String>> reqList = srm.doSelectReqSealDoc(reqSealInfo);
					/*Map<String, String> reqMap = reqList.get(0);
					
					if (MapUtils.isNotEmpty(reqDeuplicateMap)) {
						
						String reqSmrNumber = reqMap.get("SMR_NUMBER");
						String smdNumber = reqMap.get("SMD_NUMBER");*/
					
					if (MapUtils.isNotEmpty(reqDeuplicateMap) && reqList != null && reqList.size() > 0) {
						Map<String, String> reqMap = reqList.get(0);
																		
						SepoaPushManager mailPush = new SepoaPushManager(info, config);
						Map<String, String> mailData = new HashMap<>();
						SimpleDateFormat format = new SimpleDateFormat ("yyyy년 MM월 dd일 HH시 mm분");
						
						mailData.putAll(reqMap);
						mailData.put("HOUSE_CODE", house_code);
						mailData.put("COMPANY_CODE", company_code);
						mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
						mailData.put("APP_NO", "");
						
						mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
						mailData.put("SUBJECT", "[동아제약] " + reqSealInfo.get("SUBJECT") + " 날인완료 안내");
						mailData.put("REQUESTER", reqMap.get("ADD_USER_ID"));
						mailData.put("TIME", format.format(new Date()));
						mailData.put("STEP", Cache.text.get("MAIL.APP_STATUS_COMPLETE"));
						mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.APP_NO"));
						mailData.put("CONTENTS", "");
						
						SepoaMessage message = new SepoaMessage();
						
						// 동아제약 외부결재 모듈 사용으로 인한 리시버 추가(메일 템플릿 설정 -> APPROVAL_COMP 쿼리 삭제해야함)
						message.addReceiver(reqDeuplicateMap.get("ADD_USER_ID"), reqDeuplicateMap.get("USER_NAME"), reqDeuplicateMap.get("USER_EMAIL"), null);
						
						
						// 파일매니저 세팅
						FileManager fm = new FileManager(info, config, ctx);
						/*
						// 문서번호 세팅
						fm.setDocNumber(reqSmrNumber + "_" + smdNumber);
						// 레이어설정
						fm.setLayer("CONVERT_SEAL_ORG");
						
						List<Map<String, String>> completeFileList = fm.doList();
						pm = new PathManager(info, config);
						if(completeFileList != null && completeFileList.size() > 0) {
							pm.setLayer("CONVERT_SEAL_ORG");
							
							for(Map<String, String> completeContFile : completeFileList) {
								String CompleteFilePath = pm.getLayerPath() + "" + completeContFile.get("FILE_NAME_SERVER");
								message.addAttach(CipherManager.getCipher().getInputStream(new File(CompleteFilePath), false), completeContFile.get("FILE_NAME_CLIENT"));
							}
						}*/
						pm = new PathManager(info, config);
						pm.setLayer("CONVERT_SEAL_ORG");
						
						for(Map<String, String> reqDoc : reqList) {
							fm.setDocNumber(reqDoc.get("SMR_NUMBER") + "_" + reqDoc.get("SMD_NUMBER"));
							fm.setLayer("CONVERT_SEAL_ORG");
							
							List<Map<String, String>> completeFileList = fm.doList();
							if(completeFileList != null && completeFileList.size() > 0) {
								for(Map<String, String> completeContFile : completeFileList) {
									String CompleteFilePath = pm.getLayerPath() + completeContFile.get("FILE_NAME_SERVER");
									message.addAttach(CipherManager.getCipher().getInputStream(new File(CompleteFilePath), false), completeContFile.get("FILE_NAME_CLIENT"));
								}
							}
						}
						
						message.addParams(mailData);
						message.setMessageType("APPROVAL_COMP");
						mailPush.addMessage(message);
						mailPush.accept(); 
					} else {
						Logger.err.println(smrNumber + "작성자의 이메일이 없거나 날인 서류 정보가 없어 메일발송에 실패했습니다.");
					}
				}
				
				// 날인요청 Status 업데이트
				String finalDocStatus = "C";
				if(getStatus() == 1 && !srm.doUpdateReqSealDocStatus(finalDocStatus)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage("["+smrNumber+"] "+srm.getThisSessionText(house_code, "POA_CONTRACT", "STATUS_"+String.valueOf(this.getStatus()), null));
			}
			
			Commit();
			
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
			
			// 결재 반려 이후 별도 프로세스 진행 할 것인가 CBO판단
			if(SepoaModuleService.checkBusiness(new Configuration(), info, "SID1691306591594")) {
				SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.postprocess.SepoaReqSealAppRejectPostProcessor", ctx, "doProcess", new Object[] {sealInfo});
				if(out == null) {
					setStatus(107003);
				} else if(out != null && out.status != 1) {
					setStatus(107003);
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
	
	@Description("결재선 기초 정보 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectInitAppLine(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			if(SepoaModuleService.checkBusiness(new Configuration(), info, "SID1678070343724")) {
				SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.seal.approval.SepoaSealApprovalService", getConnectionContext(), "doSelectSealInfo", new Object[] {allData});
				if(out.status != 1) {
					setStatus(out.status);
				}else {					
					setValue(out.result[0]);
				}
			} else {
				//기본 결재선 지정
			}
			
			if(getStatus() == 1) {
				//setValue(sf);
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
	
	@Description("인장정보 체크")
	@SuppressWarnings("unchecked")
	public Boolean doCheckInputSealData(List<Map<String, Object>> documentList, SepoaReqSealManager srm) {
		boolean result = false;
		setStatus(1);
		setFlag(true);
		try {
			for(Map<String, Object> sealDoc : documentList) {
				Map<String, String> data = new HashMap<String, String>(); 
				String doc_No = (String)sealDoc.get("DOC_NUMBER");
				data.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
				data.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
				data.put("LAYER", "REQ_SEAL");
				data.put("DOC_NUMBER", doc_No);
				List<Map<String, String>> selectSealList = (List<Map<String, String>>) sealDoc.get("SELECT_SEAL_LIST");
				Map<String, String> resultDataSet = srm.doCheckInputSealData(data);
				
				String sealPreFix = "contract-item-box-seal-code-";
				for(Map<String, String> selectSeal : selectSealList) {
					if(resultDataSet != null && resultDataSet.size() > 0) {
						for(Entry<String, String> entry : resultDataSet.entrySet()) {
							String htmlValue = entry.getValue();
							if(htmlValue.contains(sealPreFix + selectSeal.get("MANAGE_NUMBER"))) {
								selectSeal.put("USE_FLAG", "true");
								break;
							}
						}
					}
				}
				
				if(selectSealList.stream().filter(e -> !e.containsKey("USE_FLAG")).collect(Collectors.toList()).size() > 0) {
					throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+104015));
				}
			}
			if(getStatus() == 1) {
				result = true;
			} else {
				result = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return result;
	}
	
	@Description("증명서 조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut searchSMIData (Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			List<Map<String, String>> searchResult = new ArrayList<Map<String, String>>(); 
			searchResult = srm.doSearchSMI(headerData);
			
			if(SepoaModuleService.checkBusiness(new Configuration(), info, "STIF1686537827571")) { // 증명서 발급신청 템플릿모듈
				// 리스트중 실제 템플릿이 등록된 리스트인지 유효성검사
				searchResult.stream().filter(row 
						-> row.get("ELEC_DOC_FLAG").equals("1")).collect(Collectors.toList()).removeIf(row -> {
					    srm.setDocBaiscInfo(row);
					    List<Map<String, String>> tmpList = srm.doSelectAttachDocTemplateFile();
					    return (tmpList == null || tmpList.size() < 1);
				});
			}
				
			if(getStatus() == 1) {
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
	
	@Description("날인요청 과거이력 파일조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectReqSealHistoryFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> smiData = new Gson().fromJson(MapUtils.getString(headerData, "DOC_DATA", "{}"), Map.class);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(smiData);
			
			List<Map<String, String>> attachTemplateDocList = srm.doSelectReqSealHistoryFile();
			Map<String, String> returnParams = new HashMap<String, String>();
			if(attachTemplateDocList == null || attachTemplateDocList.size() <= 0) {
				returnParams.put("RESULT", "ATTACH_DOC_EMPTY");
			} else {
				returnParams.put("RESULT", new Gson().toJson(attachTemplateDocList));
			}
			
			setValue(new Gson().toJson(returnParams));
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("날인요청 과거이력 파일업로드 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doUploadReqSealHistoryFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> fileList = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> smrData = new Gson().fromJson(MapUtils.getString(headerData, "DOC_DATA", "{}"), Map.class);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(smrData);
			
			Map<String, String> uploadFileData = null;
			if(getStatus() == 1) {
				for(Map<String, String> fileData : fileList) {
					uploadFileData = srm.doUploadReqSealHistoryFile("REQ_SEAL_HISTORY", fileData, smrData);
					break;
				}
				
				if(uploadFileData == null) {
					int errCode = srm.getLastErrCode();
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
			if(getStatus() == 1) setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("인감날인 과거이력 엑셀 업로드 처리")
	public SepoaOut doUploadHistoryExcel(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData,SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
            
            if(excelDataList == null || excelDataList.size() == 0) {
            	setStatus(105002);
            	Logger.err.println("Not Found Req Seal History Data!");
            	throw new Exception("Not Found Req Seal History Data!");
            } else {
            	SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
            	srm.setDocBaiscInfo(headerData);
            	
            	final String smrStatusDefineStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+"#"+info.getSession("LANGUAGE")+"#SLM992");
            	final String sealTypeDefineStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+"#"+info.getSession("LANGUAGE")+"#SM003");
            	final String reFlagDefineStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+"#"+info.getSession("LANGUAGE")+"#SM001_3");
            	final List<Map<String, String>> sealTypeDefine = new Gson().fromJson(sealTypeDefineStr, List.class);
            	final List<Map<String, String>> reFlagDefine = new Gson().fromJson(reFlagDefineStr, List.class);
            	final List<Map<String, String>> smrStatusDefine = new Gson().fromJson(smrStatusDefineStr, List.class);
            	final List<Map<String, String>> sealUseTypeDefine = srm.doSelectSUDGL(headerData);
            	
            	if(smrStatusDefine == null || smrStatusDefine.size() == 0 || sealTypeDefine == null || sealTypeDefine.size() == 0 || reFlagDefine == null || reFlagDefine.size() == 0  || sealUseTypeDefine ==null || sealUseTypeDefine.size() == 0) {
            		setStatus(105003);
            		Logger.err.println("Req Seal History data validation data is inaccurate.");
            		throw new Exception("Req Seal History data validation data is inaccurate.");
            	}
            	
            	List<Map<String, String>> convertExcelDataList = new ArrayList<Map<String, String>>();
            	for(Map<String, String> rowData : excelDataList) {
            		Map<String, String> convertRowData = new HashMap<String, String>();
            		
            		for(Entry<String, String> data : rowData.entrySet()) {
            			String key = data.getKey();
            			
            			switch(key.toUpperCase()) {
            			case "SEAL_TYPE" :
            				convertRowData.put(data.getKey(), sealTypeDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
            				break;
            			case "RE_FLAG" :
            				convertRowData.put(data.getKey(), reFlagDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
            				break;
            			case "SMR_STATUS" : 
            				convertRowData.put(data.getKey(), smrStatusDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
            				break;
            			case "SEAL_USE_TYPE" :
            				convertRowData.put(data.getKey(), sealUseTypeDefine.stream().filter(e -> data.getValue().equals(e.get("SUBJECT"))).findFirst().get().get("USE_NUMBER"));
            				break;
            			default :
            				convertRowData.put(data.getKey(), data.getValue());
            				break;
            			}
            		}
            		
            		convertExcelDataList.add(convertRowData);
            	}
            	
            	setValue(new Gson().toJson(convertExcelDataList));
            }
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("인감날인 과거이력 엑셀 데이터 저장")
	public SepoaOut doSaveHistoryExcelData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext newCtx = null;
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> excelDataList = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_RAW_GRID_DATA);
			List<Map<String, String>> resultExcelDataList = new ArrayList<Map<String, String>>();
			newCtx = new SepoaTransactionalResource().getConnectionContext();
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), newCtx);
			
			for(Map<String, String> excelData : excelDataList) {
				Map<String, String> tmpData = new HashMap<String, String>();
				tmpData.putAll(excelData);
				srm.setDocBaiscInfo(tmpData);
				int status = 1;
				
				try {
					//중복 키 검사
					List<Map<String, String>> legacyData = srm.doSelectReqSealHistory();
					if(legacyData != null && legacyData.size() > 0) {
						status = 105004;
						throw new Exception("This data already exists.");
					}
					
					//SSIAD_HISTORY INSERT
					if(srm.doSaveReqSealHistory() == null) {
						status = srm.getLastErrCode();
						throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+status));
					}
					
					((SepoaTransactionalResource) newCtx).getUserTransaction().commit();
					tmpData.put("PRC_RESULT", Cache.text.get("POA_CONTRACT.STATUS_105006"));
				} catch (Exception e) {
					tmpData.put("PRC_RESULT", Cache.text.get("POA_CONTRACT.STATUS_105009") + Cache.text.get("POA_CONTRACT.STATUS_" + status));
					Logger.err.println("SMR_NUMBER [" + excelData.get("SMR_NUMBER") + "] Save Fail!", e);
					((SepoaTransactionalResource) newCtx).getUserTransaction().rollback();
				} finally {
					resultExcelDataList.add(tmpData);
				}
			}
			
			Map<String, String> returnMap = new HashMap<String, String>();
			returnMap.put("returnType", "param");
			returnMap.put("prcResult", new Gson().toJson(resultExcelDataList));
			setObject(returnMap);
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		} finally {
			if(newCtx != null) {
				((SepoaTransactionalResource) newCtx).release();
			}
		}
		
		return getSepoaOut();
	}
	
	@Description("인감날인 과거이력 조회")
	public SepoaOut doSelectReqSealHistory(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(headerData);
			
			List<Map<String, String>> historyData = srm.doSelectReqSealHistory();
			if(historyData != null && historyData.size() > 0) {
				setValue(new SepoaFormater(historyData));
			} else {
				int errCode = srm.getLastErrCode();
				setStatus(errCode);
				throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+errCode));
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("전자계약 과거이력 파일조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectContractHistoryFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> contData = new Gson().fromJson(MapUtils.getString(headerData, "DOC_DATA", "{}"), Map.class);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(contData);
			
			List<Map<String, String>> attachTemplateDocList = srm.doSelectContractHistoryFile();
			Map<String, String> returnParams = new HashMap<String, String>();
			if(attachTemplateDocList == null || attachTemplateDocList.size() <= 0) {
				returnParams.put("RESULT", "ATTACH_DOC_EMPTY");
			} else {
				returnParams.put("RESULT", new Gson().toJson(attachTemplateDocList));
			}
			
			setValue(new Gson().toJson(returnParams));
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("전자계약 과거이력 파일업로드 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doUploadContractHistoryFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> fileList = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> contData = new Gson().fromJson(MapUtils.getString(headerData, "DOC_DATA", "{}"), Map.class);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(contData);
			
			Map<String, String> uploadFileData = null;
			if(getStatus() == 1) {
				for(Map<String, String> fileData : fileList) {
					uploadFileData = srm.doUploadContractHistoryFile("CONTRACT_HISTORY", fileData, contData);
					break;
				}
				
				if(uploadFileData == null) {
					int errCode = srm.getLastErrCode();
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
			if(getStatus() == 1) setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("전자계약 과거이력 조회")
	public SepoaOut doSelectContractHistory(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(headerData);
			
			List<Map<String, String>> historyData = srm.doSelectContractHistory();
			if(historyData != null && historyData.size() > 0) {
				setValue(new SepoaFormater(historyData).getString());
			} else {
				int errCode = srm.getLastErrCode();
				setStatus(errCode);
				throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+errCode));
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("전자계약 과거이력 삭제")
	public SepoaOut doDeleteContractHistory(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			for(Map<String, String> rows : gridData) {
				if(!srm.doDeleteContractHistory(rows)) {
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
				Rollback();
			} else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("전자계약 과거이력 엑셀 업로드 처리")
	public SepoaOut doUploadContractHistoryExcel(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData,SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
			
			if(excelDataList == null || excelDataList.size() == 0) {
				setStatus(105002);
				Logger.err.println("Not Found Contract History Data!");
				throw new Exception("Not Found Contract History Data!");
			} else {
				SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
				srm.setDocBaiscInfo(headerData);
				
				final String contractStatusDefineStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+"#"+info.getSession("LANGUAGE")+"#CT002");
				final String contMethodDefineStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+"#"+info.getSession("LANGUAGE")+"#CT003");
				final String categoryDefineStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+"#"+info.getSession("LANGUAGE")+"#CT004");
				final List<Map<String, String>> contractStatusTypeDefine = new Gson().fromJson(contractStatusDefineStr, List.class);
				final List<Map<String, String>> contrMethodDefine = new Gson().fromJson(contMethodDefineStr, List.class);
				final List<Map<String, String>> categoryDefine = new Gson().fromJson(categoryDefineStr, List.class);
				
				if(categoryDefine == null || categoryDefine.size() == 0 || contractStatusTypeDefine == null || contractStatusTypeDefine.size() == 0 || contrMethodDefine == null || contrMethodDefine.size() == 0 ) {
					setStatus(105003);
					Logger.err.println("Contract History data validation data is inaccurate.");
					throw new Exception("Contract History data validation data is inaccurate.");
				}
				
				List<Map<String, String>> convertExcelDataList = new ArrayList<Map<String, String>>();
				for(Map<String, String> rowData : excelDataList) {
					Map<String, String> convertRowData = new HashMap<String, String>();
					
					for(Entry<String, String> data : rowData.entrySet()) {
						String key = data.getKey();
						
						switch(key.toUpperCase()) {
						case "CONT_STATUS" :
							convertRowData.put(data.getKey(), contractStatusTypeDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
							break;
						case "CATEGORY" :
							convertRowData.put(data.getKey(), categoryDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
							break;
						case "CONT_METHOD" : 
							convertRowData.put(data.getKey(), contrMethodDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
							break;
						default :
							convertRowData.put(data.getKey(), data.getValue());
							break;
						}
					}
					
					convertExcelDataList.add(convertRowData);
				}
				
				setValue(new Gson().toJson(convertExcelDataList));
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("전자계약 과거이력 엑셀 데이터 저장")
	public SepoaOut doSaveContractHistoryExcelData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext newCtx = null;
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> excelDataList = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_RAW_GRID_DATA);
			List<Map<String, String>> resultExcelDataList = new ArrayList<Map<String, String>>();
			newCtx = new SepoaTransactionalResource().getConnectionContext();
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), newCtx);
			
			for(Map<String, String> excelData : excelDataList) {
				Map<String, String> tmpData = new HashMap<String, String>();
				tmpData.putAll(excelData);
				srm.setDocBaiscInfo(tmpData);
				int status = 1;
				
				try {
					//중복 키 검사
					List<Map<String, String>> legacyData = srm.doSelectContractHistory();
					if(legacyData != null && legacyData.size() > 0) {
						status = 105004;
						throw new Exception("This data already exists.");
					}
					
					//CONTRACT_HISTORY INSERT
					if(srm.doSaveContractHistory() == null) {
						status = srm.getLastErrCode();
						throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+status));
					}
					
					((SepoaTransactionalResource) newCtx).getUserTransaction().commit();
					tmpData.put("PRC_RESULT", Cache.text.get("POA_CONTRACT.STATUS_105006"));
				} catch (Exception e) {
					tmpData.put("PRC_RESULT", Cache.text.get("POA_CONTRACT.STATUS_105009") + Cache.text.get("POA_CONTRACT.STATUS_" + status));
					Logger.err.println("CONT_NO [" + excelData.get("CONT_NO") + "], CONT_GL_SEQ [" + excelData.get("CONT_GL_SEQ") + "] Save Fail!", e);
					((SepoaTransactionalResource) newCtx).getUserTransaction().rollback();
				} finally {
					resultExcelDataList.add(tmpData);
				}
			}
			
			Map<String, String> returnMap = new HashMap<String, String>();
			returnMap.put("returnType", "param");
			returnMap.put("prcResult", new Gson().toJson(resultExcelDataList));
			setObject(returnMap);
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		} finally {
			if(newCtx != null) {
				((SepoaTransactionalResource) newCtx).release();
			}
		}
		
		return getSepoaOut();
	}
	
	@Description("인장날인이력현황-전체")
	@SuppressWarnings("unchecked")
	public SepoaOut doSealStatistics(Map<String, Object> allData) throws Exception {		
		setStatus(1);
		setFlag(true);
        
		try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			List<Map<String, String>> resultList = srm.doSealStatisticsList(headerData);

            if( resultList != null && resultList.size() > 0) {
            	setValue(resultList);
            }else {
            	int errCode = srm.getLastErrCode();
				setStatus(errCode);
				throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_"+errCode));
			}
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@Description("날짜별결재현황")
	@SuppressWarnings("unchecked")
	public SepoaOut doByDateStatistics(Map<String, Object> allData) throws Exception {		
		setStatus(1);
		setFlag(true);
        
		try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
			if ("CORP".equals(headerData.get("TABTYPE")) || "USE".equals(headerData.get("TABTYPE"))) {
				headerData.put("RE","Y");
			} else {
				headerData.put("RE","N");
			}
            
            SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			List<Map<String, String>> resultList = srm.doByDateStatisticsList(headerData);
            
			setValue(new Gson().toJson(resultList));
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}	
	
	@Description("검수요청 확인")
	@SuppressWarnings("unchecked")
	public SepoaOut doCheckDecide(Map<String, Object> allData) throws Exception {		
		setStatus(1);
		setFlag(true);
        
		try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			boolean result = srm.doCheckDecide(headerData);

			setValue(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	
	@Description("날인요청 실물날인서류 파일조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectReqSealRealDocFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> smrData = new Gson().fromJson(MapUtils.getString(headerData, "DOC_DATA", "{}"), Map.class);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(smrData);
			
			List<Map<String, String>> realReqSealDocList = srm.doSelectReqSealRealDocFile();
			Map<String, String> returnParams = new HashMap<String, String>();
			if(realReqSealDocList == null || realReqSealDocList.size() <= 0) {
				returnParams.put("RESULT", "ATTACH_DOC_EMPTY");
			} else {
				returnParams.put("RESULT", new Gson().toJson(realReqSealDocList));
			}
			
			setValue(new Gson().toJson(returnParams));
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("날인요청 실물날인서류 파일업로드 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doUploadReqSealRealDocFile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> fileList = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, String> smrData = new Gson().fromJson(MapUtils.getString(headerData, "DOC_DATA", "{}"), Map.class);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			srm.setDocBaiscInfo(smrData);
			
			Map<String, String> uploadFileData = null;
			if(getStatus() == 1) {
				for(Map<String, String> fileData : fileList) {
					uploadFileData = srm.doUploadReqSealRealDocFile("REQ_SEAL_REAL_DOC", fileData, smrData);
					break;
				}
				
				if(uploadFileData == null) {
					int errCode = srm.getLastErrCode();
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
			if(getStatus() == 1) setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("발행번호 발급")
	public SepoaOut doCreateIssueNo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			
			String issueNo = UUID.randomUUID().toString()+System.nanoTime();
			issueNo = issueNo.replaceAll("[^0-9]", "");

			Date nowDate = new Date();
			String addDate = new SimpleDateFormat("yyyyMMdd").format(nowDate);
			String addTime =  new SimpleDateFormat("HHmmss").format(nowDate);
		
			Map<String, String> docIssueParam = new HashMap<>();
			docIssueParam.put("ADD_DATE", addDate);
			docIssueParam.put("ADD_TIME", addTime);
			docIssueParam.put("ISSUE_NO", issueNo);
			
			if(srm.doCreateIssueNo(docIssueParam)) {
				setValue(issueNo);
				Commit();
			} else {
				throw new Exception("Create Issue No Fail!");
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("날인요청 삭제 DEL_FLAG Y")
	@SuppressWarnings("unchecked")
	public SepoaOut doDeleteRequestInfoHis(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			for(Map<String, String> rows : gridData) {
				
				srm.setDocBaiscInfo(rows);
				// 날인요청 삭제여부 Y
				if(!srm.doDeleteReqSealHis(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 날인요청 - 날인서류  삭제여부 Y
				if(getStatus() == 1 &&!srm.doDeleteReqSealDocHis(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 날인요청 - 날인서류인장  삭제여부 Y
				if(getStatus() == 1 && !srm.doDeleteReqSealDocSealHis(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				// 날인요청 - 교부서류  삭제여부 Y
				if(getStatus() == 1 && !srm.doDeleteReqSealAttachDocHis(rows)) {
					int errCode = srm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						break;
					}
				}
				
				Map<String, String> sealData = new HashMap<>();
				String hsNo = DocumentUtil.getDocNumber(info, "HN").result[0];
				sealData.put("HS_NO",hsNo);
				sealData.put("REMARK",gridData.get(0).get("REMARK"));
				sealData.put("SMR_NUMBER",rows.get("SMR_NUMBER"));
				sealData.put("SMR_STATUS",rows.get("SMR_STATUS"));
				
				// 날인삭제 이력
				if(getStatus() == 1 && !srm.doSaveSSCEH(sealData)) {
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
	
	@Description("교부서류 히스토리 리스트")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectCorpAttachDocHis(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			SepoaFormater sf = null;
			srm.setDocBaiscInfo(headerData);
			List<Map<String, String>> resultList = srm.doSelectCorpAttachDocHis(headerData);
			
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
	
	@Description("교부서류 히스토리 저장")
	@SuppressWarnings("unchecked")
	public SepoaOut doSaveCorpAttachDocHis(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			
			for(Map<String, String> rows : gridData) {
				if(!srm.doSaveCorpAttachDocHis(rows)) {
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
	
	@Description("교부서류 히스토리 삭제")
	@SuppressWarnings("unchecked")
	public SepoaOut doDeleteCorpAttachDocHis(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), getConnectionContext());
			for(Map<String, String> rows : gridData) {
				if(!srm.doDeleteCorpAttachDocHis(rows)) {
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
}