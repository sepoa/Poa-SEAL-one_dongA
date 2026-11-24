package sepoa.svc.seal.cbo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import net.sf.json.JSONObject;
import sepoa.fw.approval.SepoaApproval;
import sepoa.fw.approval.SepoaApprovalLineSetting;
import sepoa.fw.approval.SepoaApprovalProgress;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
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
import sepoa.seal.SepoaSealManagementManager;
import sepoa.svc.seal.SepoaSealService;
import sepoa.svl.util.SepoaActionController;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자인장 - 모듈")
public class SepoaSealManagementService extends SepoaSealService {
	
	final static String SEAL_TABLE = "SSEAL";
	final static String CREATE_TABLE = "SSHNE";
	final static String UPDATE_TABLE = "SSHUP";
	final static String LOSS_TABLE = "SSHLO";
	final static String RETURN_TABLE = "SSHRE";
	final static String USE_SEAL_CREATE = "US";
	final static String CORP_SEAL_CREATE = "CS";
	final static String SEAL_UPDATE = "SM";
	final static String SEAL_LOSS = "SL";
	final static String SEAL_RETURN = "SR";
	
	public SepoaSealManagementService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	protected String getClassName() {
		return this.getClass().getName();
	}
	
	protected SepoaSealManagementManager getSepoaSealManagementManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		if(this.getClassName().startsWith("sepoa.svc.sealmk2")) {
			return new sepoa.sealmk2.SepoaSealManagementManager(info, config, ctx);
		} else {
			return new SepoaSealManagementManager(info, config, ctx);
		}
	}
	
	@Description("인장 등록 신청 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doCreateService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
			
		try {
			Configuration conf = new Configuration();
			ConnectionContext ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, Object> createSealData = new Gson().fromJson(MapUtils.getString(headerData, "SEAL_DATA"), Map.class);
			List<Map<String, String>> sealImageList = null;
			
			List<String>  Id_data = new ArrayList<String> ();
			Map<String, String> mailData = new HashMap<String, String>();
			SimpleDateFormat format = new SimpleDateFormat ("yyyy년 MM월 dd일 HH시 mm분");
			
			String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
			String status = "T";
			boolean doAfterFlag = true;
			
			SepoaSealManagementManager sm  = getSepoaSealManagementManager(info, new Configuration(), ctx);
			if(currentPrc != null) {
				Map<String, Object> paramMap = (Map<String, Object>)createSealData.get("parameter");
				Map<String, String> basicInfo = (Map<String, String>) paramMap.get("basic_info");
				String currentAlias = MapUtils.getString(basicInfo, "SEAL_TYPE");
				sm.setDocBaiscInfo(basicInfo);
				String prcType = null;
				
				//do Process CurrentPrc
				switch (currentPrc) {
					case "CREATE":
						//법인인감 다중 등록 확인 부분  
						if("C".equals(currentAlias)) {
							if(!SepoaModuleService.checkBusiness(new Configuration(), info, "CS1638177847009")) {
								Map<String, String> dataMap = new HashMap<>();
								dataMap.put("SEAL_STATUS", "C");
								dataMap.put("SEAL_TYPE", "C");

								if(!sm.doCheckMultiCorpSeal(dataMap)) {
									int errCode = sm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
									doAfterFlag = false;
								}
							}
						}						
												
						if(getStatus() == 1) {
							//있으면 업데이트 없으면 insert
							if(sm.doQueryRegistDoc() == null) {
								//인감 신규 등록 테이블 데이터 등록
								if(sm.doCreateSeal() == null) {
									int errCode = sm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}else {
								if(!sm.doUpateSeal(CREATE_TABLE, basicInfo)) {
									int errCode = sm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
							
							//인감 이미지 파일 존재유무 확인 후 있으면 등록
							if(getStatus() == 1) {
								sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
								sealImageList = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
								if(sealImageList != null && sealImageList.size()>0) {
									Map<String, String> stdSealList = sm.doSelectSealImage();
									if(stdSealList != null) {
										sm.doDeleteSealImage(stdSealList);
									}
									
									Map<String, String> uploadResult = null;
									for(Map<String, String> file : sealImageList) {
										uploadResult = sm.doUploadSealImage("SEAL_IMAGE", file, headerData);
									}
									
									if(uploadResult == null) {
										int errCode = sm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								}
								status = "T";
							}
						}
						doAfterFlag =false;
						break;
					case "APPROVAL":
						
						// 결재모듈 호출
						ctx = getConnectionContext();
						// 결재
						// ※※※ 결재라인이 들어가는 페이지에 대해서는 아래 서비스들을 모두 호출해 주어야 한다. 제거 금지!!!
			            // SepoaApprovalLineSetting
			            // SepoaApprovalProgress
			            // SepoaApprovalConfirm
			            // 각각의 생성자에 ctx를 넘겨줄 경우 Connection관리는 업무에서 처리되며,
			            // 넘겨주지 않을경우 결재모듈에서 자체 커밋한다. 해당부분에 대해서는 데이타 처리에 따라 알맞게 처리하면 된다.
			            // 결재선 저장 시작 -----------------------------------------------------------------------------
						Map<String, Object> tmpMap = new HashMap<>();
						Map<String, Object> tmpheaderMap = new HashMap<>();
						Map<String, Object> tmpParamMap = new HashMap<>();
						tmpheaderMap.put("CONNECTION", headerData.get("CONNECTION"));
						tmpheaderMap.put("CURRENT_PRC", headerData.get("CURRENT_PRC"));
						tmpheaderMap.put("module", headerData.get("module"));
						tmpheaderMap.put("action", headerData.get("action"));
						basicInfo.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
						basicInfo.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
						tmpParamMap.put("REQ_SEAL_INFO", basicInfo);
						tmpParamMap.put("APP_LINE", paramMap.get("APP_LINE"));
						tmpParamMap.put("APP_INFO", paramMap.get("APP_INFO"));
						JSONObject paramJson = new JSONObject();
						for (String key : tmpParamMap.keySet()) {
							paramJson.put(key, tmpParamMap.get(key));
						}
						tmpheaderMap.put("PARAMETER", paramJson.toString());
						tmpheaderMap.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
						tmpheaderMap.put("SUX_WKEY1", "SSHNE");
						tmpheaderMap.put("SUX_WKEY2", sm.getDocBaiscInfo().get("MANAGE_NUMBER"));
			            tmpheaderMap.put("SUX_WKEY3", sm.getDocBaiscInfo().get("DOC_NO"));
			            tmpheaderMap.put("APP_TYPE", paramMap.get("APP_TYPE"));
			            tmpMap.put("headerData", tmpheaderMap);
			            
			            SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
			            sepoaApprovalLineSetting.doApprovalProcess(tmpMap);
			            
			            if(!sm.checkAuthManager()) {
							sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", basicInfo.get("MANAGE_NUMBER"), basicInfo.get("DOC_NO"));
						}
			            List<Map<String, String>> appLine = (List<Map<String, String>>) paramMap.get("APP_LINE");
			            for (int i=0; i<appLine.size(); i++) {
			            	Map<String, String> appLineMap = appLine.get(i);
			            	
			            	boolean isGrant;
			            	if (appLineMap.get("APP_METHOD").equals("03")) {
			            		isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.READ);
			            	} else {
			            		isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.WRITE);
			            	}
							if(getStatus() == 1 && !isGrant) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
			            }
			            // 결재선 저장 종료 -----------------------------------------------------------------------------
			            // 결재선 까지 같이 저장 후 Commit하자.
			            //Commit();            
			            // 업무단에서 리턴받아서 처리할 키값들을 셋팅한다.
			            tmpMap.put("returnType" , "param");
			            tmpMap.put("APP_NO"     , MapUtils.getString(headerData, "APP_NO"     , ""));
			            tmpMap.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
			            tmpMap.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "I"));
			            tmpMap.put("SAVE_MODE", MapUtils.getString(headerData,"SAVE_MODE",""));
			            setObject(tmpMap);
			            
			            SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
			            sepoaApprovalProgress.doApprovalProcess(tmpMap);
			            
						status = "I";
						doAfterFlag = false;
						break;
					case "LEGAL":
						prcType = basicInfo.get("LEGAL_PRC_TYPE");
						
						//####### 메일발송시 필요한 데이터 셋팅
						mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
	                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
	                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
	                	mailData.put("DOC_NO", basicInfo.get("DOC_NO"));
	                	mailData.put("MANAGE_NUMBER", basicInfo.get("MANAGE_NUMBER"));
	                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
	                	mailData.put("TIME", format.format(new Date()));
	                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
	                	mailData.put("CONTENTS", basicInfo.get("DOC_NO"));
	                	//###########################
						
						if("LEGAL_REQUEST".equals(prcType)) {
							status = "G";
							if(!sm.checkAuthManager()) {
								sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", basicInfo.get("MANAGE_NUMBER"), basicInfo.get("DOC_NO"));
							}
							List<Map<String, String>>  doSearchCtrlCodeList = sm.doSearchCtrlCode(sm.getDocBaiscInfo()) ;
							if (doSearchCtrlCodeList.size() > 0) {
								for(Map<String,String> resultMap : doSearchCtrlCodeList) {
									boolean isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), resultMap.get("CTRL_PERSON_ID"), Authorized.WRITE);
									if(getStatus() == 1 && !isGrant) {
										int errCode = sm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								}
							}
							
							doAfterFlag = false;
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REQUEST"));
							mailData.put("REQUESTER", info.getSession("NAME_LOC"));
							mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_PROGRESS"));
							mailData.put("MESSAGE_TYPE", "REGI_LEGAL_REQ");
							
						} else if("LEGAL_ACCEPT".equals(prcType)) {
							status = "O";
							sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
							sealImageList = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
							if(sealImageList != null && sealImageList.size()>0) {
								Map<String, String> stdSealList = sm.doSelectSealImage();
								if(stdSealList != null) {
									sm.doDeleteSealImage(stdSealList);
								}
								
								Map<String, String> uploadResult = null;
								for(Map<String, String> file : sealImageList) {
									uploadResult = sm.doUploadSealImage("SEAL_IMAGE", file, headerData);
								}
								
								if(uploadResult == null) {
									int errCode = sm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
							
							//메일 셋팅
		                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_COMPLETE"));
		                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_COMPLETE"));
		                	mailData.put("MESSAGE_TYPE", "REGI_LEGAL_COMP");
		                	
						} else if("LEGAL_REJECT".equals(prcType)) {
							status = "N";
							doAfterFlag = false;
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REJECT"));
							mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_REJECT"));
							mailData.put("MESSAGE_TYPE", "REGI_LEGAL_REJECT");
						}
						break;
					case "SEND":
						status = "S";
						break;
					case "RECEIVE":
						status = "V";
						break;					
					case "COMPLETE":
						
						Map<String, String> createData = sm.doQueryRegistDoc();
						if(getStatus()==1 && createData == null) {
							int errCode = sm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						} else {
							createData.put("SEAL_STATUS", "C");
							sm.setDocBaiscInfo(createData);
							if(!sm.checkAuthManager()) {
								sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", createData.get("MANAGE_NUMBER"), createData.get("DOC_NO"));
							}
						}
						//기존에 사용하고있던 용도 데이터 제거
						if(getStatus() == 1 && sm.doDeleteSealUse() == null) {
							int errCode = sm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
						//용도 데이터 삽입
						if(getStatus() == 1) {
							Set<String >sealUseSet = new HashSet<>(Arrays.asList(createData.get("SEAL_USE").split(",")));
							for(String useNo : sealUseSet) {
								if(!"".equals(useNo)) {
									if(sm.doInsertSealUse(useNo) == null) {
										int errCode = sm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
										break;
									}
									
								}
							}
						}
						//등록할때 사용했던 이미지 실제 인감이미지로 카피
						String origManageDocNo = createData.get("MANAGE_NUMBER")+"_"+createData.get("DOC_NO");
						String desManageDocNo = createData.get("MANAGE_NUMBER");
						if(getStatus() == 1 && !sm.doCopySealImg(origManageDocNo, "", desManageDocNo)) {
							int errCode = sm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
						
						//인감 관리테이블(SSEAL에 등록)
						if(getStatus() == 1 && sm.doRegSeal() == null) {
							int errCode = sm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}						
						
						status = "C";
						doAfterFlag =false;
						break;
				}
				
				// set status
				if(getStatus() == 1 && !sm.doUpdateDocStatus(CREATE_TABLE, status)) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}					
				}
				
				//다음 프로세스 진행여부 확인
				if(doAfterFlag) {
					if(!doCreateServiceAfter(allData, sm)) {
						int errCode = sm.getLastErrCode();
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
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
					Rollback();
				}else {
					Commit();
				}		
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("인장 등록 신청 서비스 후처리")
	@SuppressWarnings("unchecked")
	public boolean doCreateServiceAfter(Map<String, Object> allData, SepoaSealManagementManager manager) {
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
		String nextPrc = MapUtils.getString(headerData, "NEXT_PRC", null);
		
		Map<String, Object> createSealData = new Gson().fromJson(MapUtils.getString(headerData, "SEAL_DATA"), Map.class);
		Map<String, Object> paramMap = (Map<String, Object>)createSealData.get("parameter");
		Map<String, String> basicInfo = (Map<String, String>) paramMap.get("basic_info");
			
		String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
		String currentAlias = MapUtils.getString(basicInfo, "SEAL_TYPE");
		String parentAlias = "C".equals(currentAlias)? CORP_SEAL_CREATE : USE_SEAL_CREATE;
		
		
		if(nextPrc == null || "".equals(nextPrc)) {
			Map<String, String> paramsMap = new HashMap<>();
			paramsMap.put("ALIAS", currentPrc);
			paramsMap.put("PARENT", parentAlias);
			Map<String, String> nextModuleMap = this.doFindNextModule(paramsMap);
			nextPrc = MapUtils.getString(nextModuleMap, "ALIAS", nextModuleMap.get("MODULE"));
		}
		
		switch(nextPrc) {
			case "COMPLETE":
				Map<String, String> createData = manager.doQueryRegistDoc();
				if(getStatus()==1 && createData == null) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				} else {
					createData.put("SEAL_STATUS", "C");
					manager.setDocBaiscInfo(createData);
					if(!manager.checkAuthManager()) {
						manager.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", createData.get("MANAGE_NUMBER"), createData.get("DOC_NO"));
					}
				}
				//기존에 사용하고있던 용도 데이터 제거
				if(getStatus() == 1 && manager.doDeleteSealUse() == null) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				//용도 데이터 삽입
				if(getStatus() == 1) {
					Set<String >sealUseSet = new HashSet<>(Arrays.asList(createData.get("SEAL_USE").split(",")));
					for(String useNo : sealUseSet) {
						if(!"".equals(useNo)) {
							if(manager.doInsertSealUse(useNo) == null) {
								int errCode = manager.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
								break;
							}
							
						}
					}
				}
				//등록할때 사용했던 이미지 실제 인감이미지로 카피
				String origManageDocNo = createData.get("MANAGE_NUMBER")+"_"+createData.get("DOC_NO");
				String desManageDocNo = createData.get("MANAGE_NUMBER");
				if(getStatus() == 1 && !manager.doCopySealImg(origManageDocNo, "", desManageDocNo)) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				//인감 관리테이블(SSEAL에 등록)
				if(getStatus() == 1 && manager.doRegSeal() == null) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				//인감 등록 관리테이블 상태 완료처리
				if(getStatus() == 1 && !manager.doUpdateDocStatus(CREATE_TABLE, "C")) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				break;
		}
		
		return true;
	}

	@Description("인장 변경 신청 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doModifyService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
			
		try {
			Configuration conf = new Configuration();
			ConnectionContext ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, Object> createSealData = new Gson().fromJson(MapUtils.getString(headerData, "SEAL_DATA"), Map.class);
			List<Map<String, String>> sealImageList = null;
			
			String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
			String status = "T";
			boolean doAfterFlag = true;
			
			List<String>  Id_data = new ArrayList<String> ();
			Map<String, String> mailData = new HashMap<String, String>();
			SimpleDateFormat format = new SimpleDateFormat ("yyyy년 MM월 dd일 HH시 mm분");
			
			SepoaSealManagementManager sm  = getSepoaSealManagementManager(info, new Configuration(), ctx);
			if(currentPrc != null) {
				Map<String, Object> paramMap = (Map<String, Object>)createSealData.get("parameter");
				Map<String, String> basicInfo = (Map<String, String>) paramMap.get("basic_info");
				
				sm.setDocBaiscInfo(basicInfo);
				String prcType = null;
				
				//do Process CurrentPrc
				switch (currentPrc) {
					case "CREATE":
						//있으면 업데이트 없으면 insert
						if(sm.doQueryModifyDoc() == null) {
							//인감 변경신청 테이블 데이터 등록
							if(sm.doModifySeal() == null) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}else {
							if(!sm.doUpateSeal(UPDATE_TABLE, basicInfo)) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}
						
						sm.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));

						//ORIGIN 있었으면 삭제 
						basicInfo.put("MANAGE_DOC_NO", basicInfo.get("MANAGE_NUMBER")+"_"+basicInfo.get("DOC_NO"));
						sm.setDocBaiscInfo(basicInfo);
						
						Map<String, String> SealList = sm.doSelectSealImage("ORG_SEAL_IMAGE");
						if(SealList != null) {
							sm.doDeleteSealImage(SealList);
						}
						
						//2. SHUP문서의 이미지를 SEAL의 MANAGE_NUMBER로 copy
						String desManageDocNo = basicInfo.get("MANAGE_NUMBER")+"_"+basicInfo.get("DOC_NO");
						String origManageDocNo= basicInfo.get("MANAGE_NUMBER");						
						if(getStatus() == 1 && !sm.doCopySealImg(origManageDocNo, "", desManageDocNo, "ORG_SEAL_IMAGE")) {
							int errCode = sm.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}						
						
						//인감 이미지 파일 존재유무 확인 후 있으면 등록
						if(getStatus() == 1) {
							sealImageList = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
							if(sealImageList != null && sealImageList.size()>0) {
								
								Map<String, String> stdSealList = sm.doSelectSealImage("SEAL_IMAGE");
								if(stdSealList != null) {
									sm.doDeleteSealImage(stdSealList);
								}
								
								Map<String, String> uploadResult = null;
								for(Map<String, String> file : sealImageList) {
									uploadResult = sm.doUploadSealImage("SEAL_IMAGE", file, headerData);
								}
								
								if(uploadResult == null) {
									int errCode = sm.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
								}
							}
							status = "T";
						}
						
						break;
					case "APPROVAL":
						// 결재모듈 호출
						ctx = getConnectionContext();
						// 결재
						// ※※※ 결재라인이 들어가는 페이지에 대해서는 아래 서비스들을 모두 호출해 주어야 한다. 제거 금지!!!
			            // SepoaApprovalLineSetting
			            // SepoaApprovalProgress
			            // SepoaApprovalConfirm
			            // 각각의 생성자에 ctx를 넘겨줄 경우 Connection관리는 업무에서 처리되며,
			            // 넘겨주지 않을경우 결재모듈에서 자체 커밋한다. 해당부분에 대해서는 데이타 처리에 따라 알맞게 처리하면 된다.
			            // 결재선 저장 시작 -----------------------------------------------------------------------------
						Map<String, Object> tmpMap = new HashMap<>();
						Map<String, Object> tmpheaderMap = new HashMap<>();
						Map<String, Object> tmpParamMap = new HashMap<>();
						tmpheaderMap.put("CONNECTION", headerData.get("CONNECTION"));
						tmpheaderMap.put("CURRENT_PRC", headerData.get("CURRENT_PRC"));
						tmpheaderMap.put("module", headerData.get("module"));
						tmpheaderMap.put("action", headerData.get("action"));
						basicInfo.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
						basicInfo.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
						tmpParamMap.put("REQ_SEAL_INFO", basicInfo);
						tmpParamMap.put("APP_LINE", paramMap.get("APP_LINE"));
						tmpParamMap.put("APP_INFO", paramMap.get("APP_INFO"));
						JSONObject paramJson = new JSONObject();
						for (String key : tmpParamMap.keySet()) {
							paramJson.put(key, tmpParamMap.get(key));
						}
						tmpheaderMap.put("PARAMETER", paramJson.toString());
						tmpheaderMap.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
						tmpheaderMap.put("SUX_WKEY1", "SSHUP");
						tmpheaderMap.put("SUX_WKEY2", sm.getDocBaiscInfo().get("MANAGE_NUMBER"));
			            tmpheaderMap.put("SUX_WKEY3", sm.getDocBaiscInfo().get("DOC_NO"));
			            tmpheaderMap.put("APP_TYPE", paramMap.get("APP_TYPE"));
			            tmpMap.put("headerData", tmpheaderMap);
			            
			            SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
			            sepoaApprovalLineSetting.doApprovalProcess(tmpMap);
			            
			            if(!sm.checkAuthManager()) {
							sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", basicInfo.get("MANAGE_NUMBER"), basicInfo.get("DOC_NO"));
						}
			            List<Map<String, String>> appLine = (List<Map<String, String>>) paramMap.get("APP_LINE");
			            for (int i=0; i<appLine.size(); i++) {
			            	Map<String, String> appLineMap = appLine.get(i);
			            	
			            	boolean isGrant;
			            	if (appLineMap.get("APP_METHOD").equals("03")) {
			            		isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.READ);
			            	} else {
			            		isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.WRITE);
			            	}
							if(getStatus() == 1 && !isGrant) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
			            }
			            // 결재선 저장 종료 -----------------------------------------------------------------------------
			            // 결재선 까지 같이 저장 후 Commit하자.
			            //Commit();            
			            // 업무단에서 리턴받아서 처리할 키값들을 셋팅한다.
			            tmpMap.put("returnType" , "param");
			            tmpMap.put("APP_NO"     , MapUtils.getString(headerData, "APP_NO"     , ""));
			            tmpMap.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
			            tmpMap.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "I"));
			            tmpMap.put("SAVE_MODE", MapUtils.getString(headerData,"SAVE_MODE",""));
			            setObject(tmpMap);
			            
			            SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
			            sepoaApprovalProgress.doApprovalProcess(tmpMap);
			            
						status = "I";
						break;
					case "LEGAL":
						prcType = basicInfo.get("LEGAL_PRC_TYPE");
						
						//####### 메일발송시 필요한 데이터 셋팅
						mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
	                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
	                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
	                	mailData.put("DOC_NO", basicInfo.get("DOC_NO"));
	                	mailData.put("MANAGE_NUMBER", basicInfo.get("MANAGE_NUMBER"));
	                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
	                	mailData.put("TIME", format.format(new Date()));
	                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
	                	mailData.put("CONTENTS", basicInfo.get("DOC_NO"));
	                	//###########################
						
						if("LEGAL_REQUEST".equals(prcType)) {
							//1.문서상태 -> 법무팀승인진행중
							status = "G";
							if(!sm.checkAuthManager()) {
								sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", basicInfo.get("MANAGE_NUMBER"), basicInfo.get("DOC_NO"));
							}
							
							List<Map<String, String>>  doSearchCtrlCodeList = sm.doSearchCtrlCode(sm.getDocBaiscInfo()) ;
							if (doSearchCtrlCodeList.size() > 0) {
								for(Map<String,String> resultMap : doSearchCtrlCodeList) {
									boolean isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), resultMap.get("CTRL_PERSON_ID"), Authorized.WRITE);
									if(getStatus() == 1 && !isGrant) {
										int errCode = sm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								}
							}
							
							//2.SSEAL 상태를 변경신청중으로 변경
							if(getStatus() == 1 && !sm.doUpdateSealStatus("UI")) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}							
							doAfterFlag = false;
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REQUEST"));
							mailData.put("REQUESTER", info.getSession("NAME_LOC"));
							mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_PROGRESS"));
							mailData.put("MESSAGE_TYPE", "MODI_LEGAL_REQ");
							
						} else if("LEGAL_ACCEPT".equals(prcType)) {
							status = "O";
							
							//메일 셋팅
		                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_COMPLETE"));
		                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_COMPLETE"));
		                	mailData.put("MESSAGE_TYPE", "MODI_LEGAL_COMP");
		                	
						} else if("LEGAL_REJECT".equals(prcType)) {
							//SSEAL 상태를 변경신청중에서 C(등록)으로 변경
							if(getStatus() == 1 && !sm.doUpdateSealStatus("C")) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}									
							status = "N";
							doAfterFlag = false;
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REJECT"));
							mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_REJECT"));
							mailData.put("MESSAGE_TYPE", "MODI_LEGAL_REJECT");
						}
						break;				
					case "COMPLETE":
						status = "C";
						break;
				}
				// set status
				if(getStatus() == 1 && !sm.doUpdateDocStatus(UPDATE_TABLE, status)) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}					
				}
				
				//다음 프로세스 진행여부 확인
				if(doAfterFlag) {
					if(!doModifyServiceAfter(allData, sm)) {
						int errCode = sm.getLastErrCode();
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
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
					Rollback();
				}else {
					Commit();
				}		
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("인장 변경 신청 서비스 후처리")
	@SuppressWarnings("unchecked")
	public boolean doModifyServiceAfter(Map<String, Object> allData, SepoaSealManagementManager manager) {
		ConnectionContext ctx = getConnectionContext();
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
		String nextPrc = MapUtils.getString(headerData, "NEXT_PRC", null);
		String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
		String parentAlias = SEAL_UPDATE;
		
		if(nextPrc == null || "".equals(nextPrc)) {
			Map<String, String> paramsMap = new HashMap<>();
			paramsMap.put("ALIAS", currentPrc);
			paramsMap.put("PARENT", parentAlias);
			Map<String, String> nextModuleMap = this.doFindNextModule(paramsMap);
			nextPrc = MapUtils.getString(nextModuleMap, "ALIAS", nextModuleMap.get("MODULE"));
		}
		
		switch(nextPrc) {
			case "COMPLETE":
				Map<String, String> modifyData = manager.doQueryModifyDoc();
				if(getStatus()==1 && modifyData == null) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				} else {
					String docNo = MapUtils.getString(modifyData, "DOC_NO", "");
					String manageNumber = MapUtils.getString(modifyData, "MANAGE_NUMBER", "");
					modifyData.put("ORG_SEAL_MG_USER_ID", MapUtils.getString(modifyData, "SEAL_MG_USER_ID", null));
					modifyData.put("SEAL_MG_DEPT", MapUtils.getString(modifyData, "CHANGE_SEAL_MG_DEPT", null));
					modifyData.put("SEAL_MG_USER_ID", MapUtils.getString(modifyData, "CHANGE_SEAL_MG_USER_ID", null));
					modifyData.put("ELECTRON_FLAG", MapUtils.getString(modifyData, "CHANGE_ELECTRON_FLAG", null));
					modifyData.put("REAL_FLAG", MapUtils.getString(modifyData, "CHANGE_REAL_FLAG", null));
					modifyData.put("EXTERNAL_FLAG", MapUtils.getString(modifyData, "CHANGE_EXTERNAL_FLAG", null));
					modifyData.put("SEAL_CATEGORY", MapUtils.getString(modifyData, "CHANGE_SEAL_CATEGORY", null));
					modifyData.put("PUBLIC_YN", MapUtils.getString(modifyData, "CHANGE_PUBLIC_YN", null));
					modifyData.put("MANAGE_DOC_NO", manageNumber+'_'+docNo);
					modifyData.put("PLACE", MapUtils.getString(modifyData, "CHANGE_PLACE", null));
					modifyData.put("SUPERVISOR", MapUtils.getString(modifyData, "CHANGE_SUPERVISOR", null));
					Logger.debug.println("################PLACE,SUPERVISOR###############");
					manager.setDocBaiscInfo(modifyData);
					if(!manager.checkAuthManager()) {
						manager.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", modifyData.get("MANAGE_NUMBER"), modifyData.get("DOC_NO"));
					}
				}
				
				//용도가 바뀌었으면 SSEAL의 기존 용도 삭제하고 새로 insert(안 바뀔 수도 있음)
				if(!"".equals(MapUtils.getString(modifyData, "CHANGE_SEAL_USE", ""))) {
					if(getStatus() == 1 && manager.doDeleteSealUse() == null) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					//용도 데이터 삽입
					if(getStatus() == 1) {
						Set<String >sealUseSet = new HashSet<>(Arrays.asList(modifyData.get("CHANGE_SEAL_USE").split(",")));
						for(String useNo : sealUseSet) {
							if(!"".equals(useNo)) {
								if(manager.doInsertSealUse(useNo) == null) {
									int errCode = manager.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
									}
									break;
								}
								
							}
						}
					}
				}
				
				//SHUP에 해당하는 이미지가 있으면(바뀌었으면) SSEAL 이미지 copy
				Map<String, String> shupSealList = manager.doSelectSealImage("SEAL_IMAGE");
				if(shupSealList != null) {
					//1. SSEAL에 이미지가 있으면 먼저 삭제 -> 있을 수밖에 없음
					try {
						SepoaSealManagementManager sm  = getSepoaSealManagementManager(info, new Configuration(), ctx);
						Map<String, String> sealDocInfo = new HashMap<>();
						sealDocInfo.put("MANAGE_DOC_NO", modifyData.get("MANAGE_NUMBER"));
						sm.setDocBaiscInfo(sealDocInfo);
						Map<String, String> ssealSealList = sm.doSelectSealImage();
						if(ssealSealList != null) {
							sm.doDeleteSealImage(ssealSealList);
						}
						
						//2. SHUP문서의 이미지를 SEAL의 MANAGE_NUMBER로 copy
						//SHUP문서 SEAL_IMAGE 레이어의 fILE ID를 찾아야 한다. (그렇지 않으면 SHUP에 달린 다른 레이어 모두가 복제됨)
						String fileId = shupSealList.get("FILE_ID");
						String origManageDocNo = modifyData.get("MANAGE_NUMBER")+"_"+modifyData.get("DOC_NO");
						String desManageDocNo = modifyData.get("MANAGE_NUMBER");						
						if(getStatus() == 1 && !manager.doCopySealImg(origManageDocNo, fileId, desManageDocNo)) {
							int errCode = manager.getLastErrCode();
							if(errCode != 0) {
								setStatus(errCode);
							}
						}
					} catch (ConfigurationException e) {
						e.printStackTrace();
					}
				}				
				
				//SSEAL data update (SSHUP data로)
				if(getStatus() == 1 && !manager.doUpateSeal(SEAL_TABLE, modifyData)) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				//SSEAL status update (등록 상태)
				if(getStatus() == 1 && !manager.doUpdateSealStatus("C")) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}				
				
				//SSHUP status update (완료 상태)
				if(getStatus() == 1 && !manager.doUpdateDocStatus(UPDATE_TABLE, "C")) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				break;
		}
		
		return true;
	}
	
	@Description("인장 분실 신청 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doLossService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
			
		try {
			ConnectionContext ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, Object> createSealData = new Gson().fromJson(MapUtils.getString(headerData, "SEAL_DATA"), Map.class);
			
			Configuration conf = new Configuration();
			
			String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
			String status = "T";
			boolean doAfterFlag = true;
			
			List<String>  Id_data = new ArrayList<String> ();
			Map<String, String> mailData = new HashMap<String, String>();
			SimpleDateFormat format = new SimpleDateFormat ("yyyy년 MM월 dd일 HH시 mm분");
			
			SepoaSealManagementManager sm  = getSepoaSealManagementManager(info, new Configuration(), ctx);
			if(currentPrc != null) {
				Map<String, Object> paramMap = (Map<String, Object>)createSealData.get("parameter");
				Map<String, String> basicInfo = (Map<String, String>) paramMap.get("basic_info");
				sm.setDocBaiscInfo(basicInfo);
				String prcType = null;
				//do Process CurrentPrc
				switch (currentPrc) {
					case "CREATE":
						//있으면 업데이트 없으면 insert
						if(sm.doQueryLossDoc() == null) {
							//인감 분실 신청 테이블 데이터 등록
							if(sm.doLossSeal() == null) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}else {
							if(!sm.doUpateSeal(LOSS_TABLE, basicInfo)) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}
						break;
					case "APPROVAL":
						// 결재모듈 호출
						ctx = getConnectionContext();
						// 결재
						// ※※※ 결재라인이 들어가는 페이지에 대해서는 아래 서비스들을 모두 호출해 주어야 한다. 제거 금지!!!
			            // SepoaApprovalLineSetting
			            // SepoaApprovalProgress
			            // SepoaApprovalConfirm
			            // 각각의 생성자에 ctx를 넘겨줄 경우 Connection관리는 업무에서 처리되며,
			            // 넘겨주지 않을경우 결재모듈에서 자체 커밋한다. 해당부분에 대해서는 데이타 처리에 따라 알맞게 처리하면 된다.
			            // 결재선 저장 시작 -----------------------------------------------------------------------------
						Map<String, Object> tmpMap = new HashMap<>();
						Map<String, Object> tmpheaderMap = new HashMap<>();
						Map<String, Object> tmpParamMap = new HashMap<>();
						tmpheaderMap.put("CONNECTION", headerData.get("CONNECTION"));
						tmpheaderMap.put("CURRENT_PRC", headerData.get("CURRENT_PRC"));
						tmpheaderMap.put("module", headerData.get("module"));
						tmpheaderMap.put("action", headerData.get("action"));
						basicInfo.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
						basicInfo.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
						tmpParamMap.put("REQ_SEAL_INFO", basicInfo);
						tmpParamMap.put("APP_LINE", paramMap.get("APP_LINE"));
						tmpParamMap.put("APP_INFO", paramMap.get("APP_INFO"));
						JSONObject paramJson = new JSONObject();
						for (String key : tmpParamMap.keySet()) {
							paramJson.put(key, tmpParamMap.get(key));
						}
						tmpheaderMap.put("PARAMETER", paramJson.toString());
						tmpheaderMap.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
						tmpheaderMap.put("SUX_WKEY1", "SSHLO");
						tmpheaderMap.put("SUX_WKEY2", sm.getDocBaiscInfo().get("MANAGE_NUMBER"));
			            tmpheaderMap.put("SUX_WKEY3", sm.getDocBaiscInfo().get("DOC_NO"));
			            tmpheaderMap.put("APP_TYPE", paramMap.get("APP_TYPE"));
			            tmpMap.put("headerData", tmpheaderMap);
			            
			            SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
			            sepoaApprovalLineSetting.doApprovalProcess(tmpMap);
			            
			            if(!sm.checkAuthManager()) {
							sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", basicInfo.get("MANAGE_NUMBER"), basicInfo.get("DOC_NO"));
						}
			            List<Map<String, String>> appLine = (List<Map<String, String>>) paramMap.get("APP_LINE");
			            for (int i=0; i<appLine.size(); i++) {
			            	Map<String, String> appLineMap = appLine.get(i);
			            	
			            	boolean isGrant;
			            	if (appLineMap.get("APP_METHOD").equals("03")) {
			            		isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.READ);
			            	} else {
			            		isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.WRITE);
			            	}
							if(getStatus() == 1 && !isGrant) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
			            }
			            // 결재선 저장 종료 -----------------------------------------------------------------------------
			            // 결재선 까지 같이 저장 후 Commit하자.
			            //Commit();            
			            // 업무단에서 리턴받아서 처리할 키값들을 셋팅한다.
			            tmpMap.put("returnType" , "param");
			            tmpMap.put("APP_NO"     , MapUtils.getString(headerData, "APP_NO"     , ""));
			            tmpMap.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
			            tmpMap.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "I"));
			            tmpMap.put("SAVE_MODE", MapUtils.getString(headerData,"SAVE_MODE",""));
			            setObject(tmpMap);
			            
			            SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
			            sepoaApprovalProgress.doApprovalProcess(tmpMap);
			            
						status = "I";
						break;
					case "LEGAL":
						prcType = basicInfo.get("LEGAL_PRC_TYPE");
						
						//####### 메일발송시 필요한 데이터 셋팅
						mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
	                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
	                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
	                	mailData.put("DOC_NO", basicInfo.get("DOC_NO"));
	                	mailData.put("MANAGE_NUMBER", basicInfo.get("MANAGE_NUMBER"));
	                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
	                	mailData.put("TIME", format.format(new Date()));
	                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
	                	mailData.put("CONTENTS", basicInfo.get("DOC_NO"));
	                	//###########################
						
						if("LEGAL_REQUEST".equals(prcType)) {
							//1.법무팀에 권한 부여 
							status = "G";
							if(!sm.checkAuthManager()) {
								sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", basicInfo.get("MANAGE_NUMBER"), basicInfo.get("DOC_NO"));
							}
							List<Map<String, String>>  doSearchCtrlCodeList = sm.doSearchCtrlCode(sm.getDocBaiscInfo()) ;
							if (doSearchCtrlCodeList.size() > 0) {
								for(Map<String,String> resultMap : doSearchCtrlCodeList) {
									boolean isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), resultMap.get("CTRL_PERSON_ID"), Authorized.WRITE);
									if(getStatus() == 1 && !isGrant) {
										int errCode = sm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								}
							}
							
							//2. seal상태 바꾸기 -> 분실신청진행중으로 
							if(getStatus() == 1 && !sm.doUpdateSealStatus("LI")) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
							doAfterFlag = false;
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REQUEST"));
							mailData.put("REQUESTER", info.getSession("NAME_LOC"));
							mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_PROGRESS"));
							mailData.put("MESSAGE_TYPE", "LOSS_LEGAL_REQ");
							
						} else if("LEGAL_ACCEPT".equals(prcType)) {
							status = "O";
							
							//메일 셋팅
		                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_COMPLETE"));
		                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_COMPLETE"));
		                	mailData.put("MESSAGE_TYPE", "LOSS_LEGAL_COMP");
							
						} else if("LEGAL_REJECT".equals(prcType)) {
							//1.다시 인감을 C (등록상태로 바꿈) SSEAL
							if(getStatus() == 1 && !sm.doUpdateSealStatus("C")) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}							
							
							//2. SHLO 반려로 바꿈 
							status = "N";
							doAfterFlag = false;
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REJECT"));
							mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_REJECT"));
							mailData.put("MESSAGE_TYPE", "LOSS_LEGAL_REJECT");
						}
						break;
					case "COMPLETE":
						status = "C";
						break;
				}
				
				// set status
				if(getStatus() == 1 && !sm.doUpdateDocStatus(LOSS_TABLE, status)) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}					
				}
				
				//다음 프로세스 진행여부 확인
				if(doAfterFlag) {
					if(!doLossServiceAfter(allData, sm)) {
						int errCode = sm.getLastErrCode();
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
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
					Rollback();
				}else {
					Commit();
				}		
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("인장 분실 신청 서비스 후처리")
	@SuppressWarnings("unchecked")
	public boolean doLossServiceAfter(Map<String, Object> allData, SepoaSealManagementManager manager) {
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
		String nextPrc = MapUtils.getString(headerData, "NEXT_PRC", null);
		String prcType = null;
		
		Map<String, Object> createSealData = new Gson().fromJson(MapUtils.getString(headerData, "SEAL_DATA"), Map.class);
		Map<String, Object> paramMap = (Map<String, Object>)createSealData.get("parameter");
		Map<String, String> basicInfo = (Map<String, String>) paramMap.get("basic_info");
			
		String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
		String parentAlias = SEAL_LOSS;
		
		if(nextPrc == null || "".equals(nextPrc)) {
			Map<String, String> paramsMap = new HashMap<>();
			paramsMap.put("ALIAS", currentPrc);
			paramsMap.put("PARENT", parentAlias);
			Map<String, String> nextModuleMap = this.doFindNextModule(paramsMap);
			if (nextModuleMap == null) {
				nextPrc = "COMPLETE";
			} else {
				nextPrc = MapUtils.getString(nextModuleMap, "ALIAS", nextModuleMap.get("MODULE"));
			}
		}
		
		switch(nextPrc) {
			case "COMPLETE":
				String sealStatus = "L";
				prcType = basicInfo.get("LEGAL_PRC_TYPE");
				//1. SSHLO 분실 테이블 상태 완료처리
				if(getStatus() == 1 && !manager.doUpdateDocStatus(LOSS_TABLE, "C")) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				//2. SSEAL 상태 바꾸기  -> 분실
				if(getStatus() == 1 && !manager.doUpdateSealStatus(sealStatus)) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				break;
		}
		return true;
	}
	
	@Description("인장 반납 신청 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doReturnService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
			
		try {
			Configuration conf = new Configuration();
			ConnectionContext ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			Map<String, Object> createSealData = new Gson().fromJson(MapUtils.getString(headerData, "SEAL_DATA"), Map.class);
			
			List<String>  Id_data = new ArrayList<String> ();
			Map<String, String> mailData = new HashMap<String, String>();
			SimpleDateFormat format = new SimpleDateFormat ("yyyy년 MM월 dd일 HH시 mm분");
			
			String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
			String status = "T";
			boolean doAfterFlag = true;
			
			SepoaSealManagementManager sm  = getSepoaSealManagementManager(info, new Configuration(), ctx);
			if(currentPrc != null) {
				Map<String, Object> paramMap = (Map<String, Object>)createSealData.get("parameter");
				Map<String, String> basicInfo = (Map<String, String>) paramMap.get("basic_info");
				sm.setDocBaiscInfo(basicInfo);
				String prcType = null;
				//do Process CurrentPrc
				switch (currentPrc) {
					case "CREATE":
						//있으면 업데이트 없으면 insert
						if(sm.doQueryReturnDoc() == null) {
							//인감 반납 신청 테이블 데이터 등록
							if(sm.doReturnSeal() == null) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}else {
							if(!sm.doUpateSeal(RETURN_TABLE, basicInfo)) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
						}
						break;
					case "APPROVAL":
						// 결재모듈 호출
						ctx = getConnectionContext();
						// 결재
						// ※※※ 결재라인이 들어가는 페이지에 대해서는 아래 서비스들을 모두 호출해 주어야 한다. 제거 금지!!!
			            // SepoaApprovalLineSetting
			            // SepoaApprovalProgress
			            // SepoaApprovalConfirm
			            // 각각의 생성자에 ctx를 넘겨줄 경우 Connection관리는 업무에서 처리되며,
			            // 넘겨주지 않을경우 결재모듈에서 자체 커밋한다. 해당부분에 대해서는 데이타 처리에 따라 알맞게 처리하면 된다.
			            // 결재선 저장 시작 -----------------------------------------------------------------------------
						Map<String, Object> tmpMap = new HashMap<>();
						Map<String, Object> tmpheaderMap = new HashMap<>();
						Map<String, Object> tmpParamMap = new HashMap<>();
						tmpheaderMap.put("CONNECTION", headerData.get("CONNECTION"));
						tmpheaderMap.put("CURRENT_PRC", headerData.get("CURRENT_PRC"));
						tmpheaderMap.put("module", headerData.get("module"));
						tmpheaderMap.put("action", headerData.get("action"));
						basicInfo.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
						basicInfo.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
						tmpParamMap.put("REQ_SEAL_INFO", basicInfo);
						tmpParamMap.put("APP_LINE", paramMap.get("APP_LINE"));
						tmpParamMap.put("APP_INFO", paramMap.get("APP_INFO"));
						JSONObject paramJson = new JSONObject();
						for (String key : tmpParamMap.keySet()) {
							paramJson.put(key, tmpParamMap.get(key));
						}
						tmpheaderMap.put("PARAMETER", paramJson.toString());
						tmpheaderMap.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
						tmpheaderMap.put("SUX_WKEY1", "SSHRE");
						tmpheaderMap.put("SUX_WKEY2", sm.getDocBaiscInfo().get("MANAGE_NUMBER"));
			            tmpheaderMap.put("SUX_WKEY3", sm.getDocBaiscInfo().get("DOC_NO"));
			            tmpheaderMap.put("APP_TYPE", paramMap.get("APP_TYPE"));
			            tmpMap.put("headerData", tmpheaderMap);
			            
			            SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
			            sepoaApprovalLineSetting.doApprovalProcess(tmpMap);
			            
			            if(!sm.checkAuthManager()) {
							sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", basicInfo.get("MANAGE_NUMBER"), basicInfo.get("DOC_NO"));
						}
			            List<Map<String, String>> appLine = (List<Map<String, String>>) paramMap.get("APP_LINE");
			            for (int i=0; i<appLine.size(); i++) {
			            	Map<String, String> appLineMap = appLine.get(i);
			            	
			            	boolean isGrant;
			            	if (appLineMap.get("APP_METHOD").equals("03")) {
			            		isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.READ);
			            	} else {
			            		isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), appLineMap.get("APP_USER_ID"), Authorized.WRITE);
			            	}
							if(getStatus() == 1 && !isGrant) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
			            }
			            // 결재선 저장 종료 -----------------------------------------------------------------------------
			            // 결재선 까지 같이 저장 후 Commit하자.
			            //Commit();            
			            // 업무단에서 리턴받아서 처리할 키값들을 셋팅한다.
			            tmpMap.put("returnType" , "param");
			            tmpMap.put("APP_NO"     , MapUtils.getString(headerData, "APP_NO"     , ""));
			            tmpMap.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
			            tmpMap.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "I"));
			            tmpMap.put("SAVE_MODE", MapUtils.getString(headerData,"SAVE_MODE",""));
			            setObject(tmpMap);
			            
			            SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
			            sepoaApprovalProgress.doApprovalProcess(tmpMap);
			            
						status = "I";
						break;
					case "LEGAL":
						prcType = basicInfo.get("LEGAL_PRC_TYPE");
						
						//####### 메일발송시 필요한 데이터 셋팅
						mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
	                	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
	                	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
	                	mailData.put("DOC_NO", basicInfo.get("DOC_NO"));
	                	mailData.put("MANAGE_NUMBER", basicInfo.get("MANAGE_NUMBER"));
	                	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
	                	mailData.put("TIME", format.format(new Date()));
	                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_NO"));
	                	mailData.put("CONTENTS", basicInfo.get("DOC_NO"));
	                	//###########################
						
						if("LEGAL_REQUEST".equals(prcType)) {
							//1.법무팀에 권한 부여 
							status = "G";
							if(!sm.checkAuthManager()) {
								sm.initAuthManager(info.getSession("HOUSE_CODE"), info.getSession("COMPANY_CODE"), "SEAL", basicInfo.get("MANAGE_NUMBER"), basicInfo.get("DOC_NO"));
							}
							List<Map<String, String>>  doSearchCtrlCodeList = sm.doSearchCtrlCode(sm.getDocBaiscInfo()) ;
							if (doSearchCtrlCodeList.size() > 0) {
								for(Map<String,String> resultMap : doSearchCtrlCodeList) {
									boolean isGrant = sm.grantTo(PrincipalAgentType.PERSONAL, info.getSession("COMPANY_CODE"), resultMap.get("CTRL_PERSON_ID"), Authorized.WRITE);
									if(getStatus() == 1 && !isGrant) {
										int errCode = sm.getLastErrCode();
										if(errCode != 0) {
											setStatus(errCode);
										}
									}
								}
							}
							//2. SSEAL 상태-반납 진행 중 update 
							if(getStatus() == 1 && !sm.doUpdateSealStatus("RI")) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}
							doAfterFlag = false;
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REQUEST"));
							mailData.put("REQUESTER", info.getSession("NAME_LOC"));
							mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_PROGRESS"));
							mailData.put("MESSAGE_TYPE", "RETURN_LEGAL_REQ");
							
						} else if("LEGAL_ACCEPT".equals(prcType)) {
							status = "O";
							
							//메일 셋팅
		                	mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_COMPLETE"));
		                	mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_COMPLETE"));
		                	mailData.put("MESSAGE_TYPE", "RETURN_LEGAL_COMP");
		                	
						} else if("LEGAL_REJECT".equals(prcType)) {
							//1.SSEAL 상태-등록 update
							if(getStatus() == 1 && !sm.doUpdateSealStatus("C")) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
							}							
							
							//2. SSHRE 상태-반려 update
							status = "N";
							doAfterFlag = false;
							
							//메일 셋팅
							mailData.put("SUBJECT", Cache.text.get("MAIL.MANAGE_REJECT"));
							mailData.put("STEP", Cache.text.get("MAIL.MANAGE_STATUS_REJECT"));
							mailData.put("MESSAGE_TYPE", "RETURN_LEGAL_REJECT");
						}
						break;
					case "COMPLETE":
						status = "C";
						break;
				}
				
				// set status
				if(getStatus() == 1 && !sm.doUpdateDocStatus(RETURN_TABLE, status)) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}					
				}
				
				//다음 프로세스 진행여부 확인
				if(doAfterFlag) {
					if(!doReturnServiceAfter(allData, sm)) {
						int errCode = sm.getLastErrCode();
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
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
					Rollback();
				}else {
					Commit();
				}		
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}	

	@Description("인장 반납 신청 서비스 후처리")
	@SuppressWarnings("unchecked")
	public boolean doReturnServiceAfter(Map<String, Object> allData, SepoaSealManagementManager manager) {
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
		String nextPrc = MapUtils.getString(headerData, "NEXT_PRC", null);
		String prcType = null;
		
		Map<String, Object> createSealData = new Gson().fromJson(MapUtils.getString(headerData, "SEAL_DATA"), Map.class);
		Map<String, Object> paramMap = (Map<String, Object>)createSealData.get("parameter");
		Map<String, String> basicInfo = (Map<String, String>) paramMap.get("basic_info");
			
		String currentPrc = MapUtils.getString(headerData, "CURRENT_PRC", null);
		String parentAlias = SEAL_RETURN;
		
		if(nextPrc == null || "".equals(nextPrc)) {
			Map<String, String> paramsMap = new HashMap<>();
			paramsMap.put("ALIAS", currentPrc);
			paramsMap.put("PARENT", parentAlias);
			Map<String, String> nextModuleMap = this.doFindNextModule(paramsMap);
			nextPrc = MapUtils.getString(nextModuleMap, "ALIAS", nextModuleMap.get("MODULE"));
		}
		
		switch(nextPrc) {
			case "COMPLETE":
				String sealStatus = "R";
				prcType = basicInfo.get("LEGAL_PRC_TYPE");
				//1. SSHRE 반납 테이블 상태-완료 update
				if(getStatus() == 1 && !manager.doUpdateDocStatus(RETURN_TABLE, "C")) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				//2. SSEAL 상태-반납 update 
				if(getStatus() == 1 && !manager.doUpdateSealStatus(sealStatus)) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				break;
		}
		return true;
	}	
	
	@Description("인장 신청현황 리스트 조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doRequestList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			headerData.put("F_KEYWORD", MapUtils.getString(headerData, "F_KEYWORD", "").trim());
			Boolean counter = "true".equals(MapUtils.getString(headerData, "COUNTER", "false")) ? true : false;
			
			List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {	// 관리자
				headerData.put("IS_ADMIN", "true");
			}
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, ctx);
			sm.setDocBaiscInfo(headerData);
			resultList = sm.doQuerySealReqList(true);
			
			if(resultList == null) {
				int errCode = sm.getLastErrCode();
				if(counter && errCode == 103324) {
					setValue(0);
					setStatus(1);
				}
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				if (counter) {
					setValue(resultList.size());
				} else {
					setValue(resultList);
				}
			}
			
			if(getStatus() != 1) {
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
	
	@Description("인장 신청현황 유형별 카운트")
	@SuppressWarnings("unchecked")
	public SepoaOut doCountRequestList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaOut so = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, Integer> countMap = new HashMap<String, Integer>();
			Map<String, Object> paramMap = new HashMap<String, Object>();
			
			String[] mngTypeList = headerData.get("SEAL_MNG_TYPE_LIST").split(",");
			String countPrefix = "SEAL_MNG_COUNT_";
			
			for (String mngType : mngTypeList) {
				Map<String, String> paramHeader = new HashMap<String, String>();
				paramMap.put(SepoaDataMapper.KEY_HEADER_DATA, paramHeader);
				paramHeader.putAll(headerData);
				paramHeader.put("COUNTER", "true");
				paramHeader.put("F_APPLY_TYPE", mngType);
				so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaSealManagementService", ctx, "doRequestList", new Object[] {paramMap});
				int count = Integer.parseInt(so.result[0]);
				countMap.put(countPrefix+mngType, count);
			}
			
			// 처리대상
			Map<String, String> paramHeader = new HashMap<String, String>();
			paramMap.put(SepoaDataMapper.KEY_HEADER_DATA, paramHeader);
			paramHeader.putAll(headerData);
			paramHeader.put("COUNTER", "true");
			paramHeader.put("F_APPLY_TYPE", "");
			
			so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaSealManagementService", ctx, "doToDoList", new Object[] {paramMap});
			int todoSize = Integer.parseInt(so.result[0]);
			countMap.put(countPrefix+"TODO", todoSize);
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
			
			setValue(countMap);
			
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

	@Description("인장 처리대상 리스트 조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doToDoList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Boolean counter = "true".equals(MapUtils.getString(headerData, "COUNTER", "false")) ? true : false;
			
			//법무팀이 처리대상에서 처리할 문서의 상태 G : 법무팀 승인 진행중, O : 법무팀 승인 (발송대기)
			headerData.put("STATUS", "G,O");
			List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, new Configuration(), ctx);
			sm.setDocBaiscInfo(headerData);
			
			resultList = sm.doQuerySealPrcList(true);
			if(resultList == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				if (counter) {
					setValue(resultList.size());
				} else {
					setValue(resultList);
				}
			}
			
			if(getStatus() != 1) {
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

	@Description("문서 디테일 조회-등록, 변경, 분실, 반납")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentDetail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			Map<String, String> resultMap = new HashMap<>();
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, getConnectionContext());
			sm.setDocBaiscInfo(headerData);
			String applyType = MapUtils.getString(headerData, "APPLY_TYPE", "");
			String manageNumber = MapUtils.getString(headerData, "MANAGE_NUMBER", null);
			StringJoiner sj = null;
			List<Map<String, String>> sealUseInfoList = null;
			switch (applyType) {
				case "NE":
					resultMap = sm.doQueryRegistDoc();
					String sealUse = resultMap.get("SEAL_USE");
					String[] sealUseArr = sealUse.split(",");
					sj = new StringJoiner(", ");
					//
					if(sealUseArr.length > 0) {
						for(String useNo : sealUseArr) {
							if(!"".equals(useNo)) {
								Map<String, String> udglInfo = sm.doSelectSUDGL(useNo);
								if(udglInfo != null) {
									sj.add(udglInfo.get("SUBJECT"));
								}
							}
						}
						resultMap.put("SEAL_USE_LOC", sj.toString());
					}
					break;
				case "RE":
					resultMap = sm.doQueryReturnDoc();
					sealUseInfoList = sm.doSelectSealUseInfo(manageNumber);
					sj = new StringJoiner(", ");
					if(sealUseInfoList != null) {
						if(sealUseInfoList.size() > 0) {
							for(Map<String, String> sealUseRow : sealUseInfoList) {
								sj.add(sealUseRow.get("SUBJECT"));
							}
						}
					}
					resultMap.put("SEAL_USE_LOC", sj.toString());	
					break;
				case "UP":
					resultMap = sm.doQueryModifyDoc();
					//변경하기 전의 seal_use
					String beforeSealUse = resultMap.get("SEAL_USE");
					String[] beforeSealUseArr = beforeSealUse.split(",");
					sj = new StringJoiner(", ");
					if(beforeSealUseArr.length > 0) {
						for(String useNo : beforeSealUseArr) {
							useNo = useNo.trim();
							if(!"".equals(useNo)) {
								Map<String, String> udglInfo = sm.doSelectSUDGL(useNo);
								if(udglInfo != null) {
									sj.add(udglInfo.get("SUBJECT"));
								}
							}
						}
						resultMap.put("SEAL_USE_LOC", sj.toString());
					}					
					
					//변경하려고 하는 seal_use
					String changeSealUse = resultMap.get("CHANGE_SEAL_USE");
					String[] changeSealUseArr = changeSealUse.split(",");
					sj = new StringJoiner(", ");
					//
					if(changeSealUseArr.length > 0) {
						for(String useNo : changeSealUseArr) {
							useNo = useNo.trim();
							if(!"".equals(useNo)) {
								Map<String, String> udglInfo = sm.doSelectSUDGL(useNo);
								if(udglInfo != null) {
									sj.add(udglInfo.get("SUBJECT"));
								}
							}
						}
						resultMap.put("CHANGE_SEAL_USE_LOC", sj.toString());
					}					
					break;
				case "LO":
					resultMap = sm.doQueryLossDoc();
					sealUseInfoList = sm.doSelectSealUseInfo(manageNumber);
					sj = new StringJoiner(", ");
					if(sealUseInfoList != null) {
						if(sealUseInfoList.size() > 0) {
							for(Map<String, String> sealUseRow : sealUseInfoList) {
								sj.add(sealUseRow.get("SUBJECT"));
							}
						}
					}
					resultMap.put("SEAL_USE_LOC", sj.toString());					
					break;
				default:
					break;
					
				}
			
			if(resultMap == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				setValue(new SepoaFormater(resultMap));
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

	@Description("인감 상세 조회 - SEAL")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doSealDetail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
	
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, getConnectionContext());
			sm.setDocBaiscInfo(headerData);
			String manageNo = headerData.get("MANAGE_NUMBER");
			
			//SSEAL 조회
			Map<String, String> resultMap = new HashMap<>();
			resultMap = sm.doSelectSealData(headerData);
			if(resultMap == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				//SSUSE 조회
				List<Map<String, String>> sealUseInfoList = sm.doSelectSealUseInfo(manageNo);
				StringJoiner nameSj = new StringJoiner(", ");
				StringJoiner codeSj = new StringJoiner(", ");
				if(sealUseInfoList != null) {
					if(sealUseInfoList.size() > 0) {
						for(Map<String, String> sealUseRow : sealUseInfoList) {
							nameSj.add(sealUseRow.get("SUBJECT"));
							codeSj.add(sealUseRow.get("USE_NUMBER"));
						}
					}
				}
				
				resultMap.put("SEAL_USE_LOC", nameSj.toString());
				resultMap.put("SEAL_USE", codeSj.toString());
				setValue(new SepoaFormater(resultMap));
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
	
	@Description("인감 수정")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doSealUpdate(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> fileData = (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA);
			Map<String, String> data = new Gson().fromJson(MapUtils.getString(headerData, "SEAL_DATA").replaceAll("&#34;", "\""), Map.class);
			
			Config config = new Configuration();
	
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, getConnectionContext());
			
			headerData.put("MANAGE_DOC_NO", headerData.get("MANAGE_NUMBER"));
			sm.setDocBaiscInfo(headerData);
			String manageNo = headerData.get("MANAGE_NUMBER");
			
			//SSEAL 조회
			Map<String, String> resultMap = new HashMap<>();
			resultMap = sm.doSelectSealData(headerData);
			if(resultMap == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				if (!sm.doUpdateSealInfo(data)) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				};
				
				if(getStatus() == 1 && sm.doDeleteSealUse() == null) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				
				if(getStatus() == 1) {
					Set<String >sealUseSet = new HashSet<>(Arrays.asList(data.get("SEAL_USE").replaceAll(" ", "").split(",")));
					for(String useNo : sealUseSet) {
						if(!"".equals(useNo)) {
							if(sm.doInsertSealUse(useNo) == null) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
								break;
							}
							
						}
					}
				}
				
				if (fileData.size() > 0) {
					if(getStatus() == 1) {
						Map<String, String> img = sm.doSelectSealImage("SEAL_IMAGE");
						if (!(img==null)) {
							sm.doDeleteSealImage(img);
						}
						
						for(Map<String, String> file : fileData) {
							Map<String, String> uploadResult = sm.doUploadSealImage("SEAL_IMAGE", file, headerData);
							
							if(uploadResult == null) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
									break;
								}
							}
						}
					}
				}

				if(getStatus() != 1) {
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
					Rollback();
				}else {
					Commit();
				}		
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

	@Description("인감 이미지 업로드")
	@SuppressWarnings("unchecked")
	public SepoaOut doUploadSealImage(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaSealManagementManager manager = getSepoaSealManagementManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setDocBaiscInfo(headerData);
			
			Map<String, String> stdSealList = manager.doSelectSealImage();
			if(stdSealList != null) {
				manager.doDeleteSealImage(stdSealList);
			}
			
			Map<String, String> storedFileData = null;
			for(Map<String, String> file : (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA)) {
				storedFileData = manager.doUploadSealImage("SEAL_IMAGE", file, headerData);
			}
			
			if(storedFileData == null) {
				int errCode = manager.getLastErrCode();
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
	
	@Description("이미지 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doImage(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaSealManagementManager manager = getSepoaSealManagementManager(info, config, getConnectionContext());
			manager.setDocBaiscInfo(headerData);
			
			Map<String, String> sealImage = manager.doSelectSealImage();
			if(sealImage == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				setValue(sealImage);
				addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
			}
			
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

	@Description("외부 인감리스트 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut externalSelectSealList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			ConnectionContext ctx = getConnectionContext();
			
			//파라미터 파싱
			Map<String, String> keyObjectData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
			Map<String, String> sealListParams = MapUtils.getMap(keyObjectData, "PARAMETER");
			
			//인감 관리 매니저 객체 생성
			SepoaSealManagementManager manager = getSepoaSealManagementManager(info, new Configuration(), ctx);
			
			//사용자 조회 후 변경될 인포 객체
			SepoaInfo newInfo = null;
			
			//반환결과 객체
			List<Map<String, Object>> resultDataList = null;
			
			//요청자 ID 파싱후 유효성검사
			String houseCode = sealListParams.get("HOUSE_CODE");
			String companyCode = sealListParams.get("COMPANY_CODE");
			String userId = sealListParams.get("REQ_USER_ID");
			if(!manager.doCheckExistUser(houseCode, companyCode, userId)) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() == 1) {
				//실제 사용자로 판단되면 info객체를 현재 스레드에서 사용할 수 있게 설정
				newInfo = new SepoaInfo(houseCode, "COMPANY_CODE="+companyCode+"^@^ID="+userId+"^@^LANGUAGE=KO^@^");
				newInfo.setThreadLocal();
				
				//날인요청 매니저 재생성 및 재할당
				manager = getSepoaSealManagementManager(newInfo, new Configuration(), ctx);
				
				//인감 상태는 C(등록인 것만) 조회
				sealListParams.put("SEAL_STATUS", "C");

				//용도는 param에 안 들어오지만 들어올 경우를 대비해서 무조건 제거
				if(sealListParams.get("SEAL_USE_TYPE") != null) {
					sealListParams.remove("SEAL_USE_TYPE");
				}
				
				List<Map<String, String>> totalList = new ArrayList<>();
				List<Map<String, String>> sealList = null;
				//인감 구분 : ALL일 때 법인인감은 조건없이 모두 검색
				if("ALL".equals(MapUtils.getString(sealListParams, "SEAL_TYPE", null))) {
					String reqUserId = sealListParams.get("REQ_USER_ID");
					//법인인감만 부서 조건 없이 SELECT 
					sealListParams.put("SEAL_TYPE", "C,S");
					
					if(sealListParams.get("REQ_USER_ID") != null) {
						sealListParams.remove("REQ_USER_ID");
					}
					
					if(SepoaModuleService.checkBusiness(new Configuration(), info, "US1637133464814")) {
						//용도기준
						sealList = manager.doSelectSealListRequiredUse(sealListParams);
					} else {
						sealList = manager.doSelectSealList(sealListParams);
					}
					
					if(sealList != null && sealList.size() > 0) {
						totalList.addAll(sealList);
					}
					
					//밑에서 사용인감, 영문서명인 SELECT를 위해 다시 세팅
					sealListParams.put("SEAL_TYPE", "E,U");
					sealListParams.put("REQ_USER_ID", reqUserId);
				}
				
				//법인인감 혹은 직인인경우 조건 없이 모든 법인인감 혹은 직인 검색
				if("C".equals(MapUtils.getString(sealListParams, "SEAL_TYPE", null))||
						"S".equals(MapUtils.getString(sealListParams, "SEAL_TYPE", null))) {
					if(sealListParams.get("REQ_USER_ID") != null) {
						sealListParams.remove("REQ_USER_ID");
					}
				}
				
				//인감 관리 매니저를 통한 인감 리스트 조회
				//모듈 조회에서 용도기반 검색 켜저있으면 용도기반, 아니면 그냥 일반 검색 
				if(SepoaModuleService.checkBusiness(new Configuration(), info, "US1637133464814")) {
					//용도기준
					sealList = manager.doSelectSealListRequiredUse(sealListParams);
				} else {
					if("U".equals(MapUtils.getString(sealListParams, "SEAL_TYPE", null))){
						/*if(SepoaModuleService.checkBusiness(new Configuration(), info, "US1637133452574")) {
							// 부서 기준
							sealListParams.put("SEAL_MG_DEPT", sealListParams.get("SEAL_MG_DEPT"));
						}*/
					}
					sealList = manager.doSelectSealList(sealListParams);
				}
				
				if(sealList != null && sealList.size() > 0) {
					totalList.addAll(sealList);	
				}
				
				if(totalList.size() < 1) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				} else {
					resultDataList = new ArrayList<>();
					Map<String, Object> resultObject = new HashMap<>();
					List<Map<String, String>> sealResultList = new ArrayList<>();
					
					//인감 리스트 루프하면서 리턴결과 추출
					for(Map<String, String> sealData : totalList) {
						//인감 조회결과 리턴 단일 객체 생성
						Map<String, String> resultData = new HashMap<>();
						resultData.put("MANAGE_NUMBER", sealData.get("MANAGE_NUMBER"));
						resultData.put("SUBJECT", sealData.get("SUBJECT"));
						resultData.put("SEAL_TYPE", sealData.get("SEAL_TYPE"));
						
						sealResultList.add(resultData);
					}
					
					//IMAGE 포함 여부 파싱
					if("Y".equalsIgnoreCase(MapUtils.getString(sealListParams, "IMAGE"))) {
						//이미지 ReturnType 파싱
						String imageReturnType = MapUtils.getString(sealListParams, "IMAGE_RETURN_TYPE", null);
						if(imageReturnType == null || "".equals(imageReturnType.trim())) {
							imageReturnType = "BASE64";
						}
						
						//인감 리스트 루프
						for(Map<String, String> resultData : sealResultList) {
							//인감 이미지 조회를 위한 더미 파라미터 생성후 세팅
							Map<String, String> sealImageParms = new HashMap<>();
							sealImageParms.putAll(resultData);
							sealImageParms.put("MANAGE_DOC_NO", resultData.get("MANAGE_NUMBER"));
							manager.setDocBaiscInfo(sealImageParms);
							//매니저를 통한 인감 이미지 파일 조회
							Map<String, String> sealFile = manager.doSelectSealImage();
							if(sealFile != null) {
								//이미지 별로 BinaryString 혹은 Base64 생성 후 적재
								String imageFilePath = sealFile.get("FILE_PATH");
								String imageStr = manager.sealImageToString(imageReturnType, imageFilePath);
								
								if(imageStr == null) {
									imageStr = "undefined";
								}
								
								//이미지 값 반환(Base64 or BinaryString)
								resultData.put("IMAGE_VALUE", imageStr);
							}
						}
					}
					resultObject.put("SEAL_LIST", sealResultList);
					resultDataList.add(resultObject);
				}
			}			
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			} else {
				setValue(resultDataList);
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
	
	
	@Description("인장 등록현황 리스트 조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSealList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		
		try {
			//이 서비스는 인감관리-등록현황과 변경/반납/분실 인감선택 팝업에서 같이 쓰임 
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
			
			//시스템관리자, 법무팀이면 조회 조건 삭제 
			String[] systemManagerProfileCode = CommonUtil.getConfig("sepoa.system.manager.profile.code", "MUP2011020001").split(",");
			if(info.getSession("CTRL_CODE").contains("MANAGE_CONFIRM") ||
					Arrays.asList(systemManagerProfileCode).contains(info.getSession("MENU_PROFILE_CODE"))){
				headerData.remove("SEAL_MG_USER_ID");
				headerData.remove("USER_DEPT");
			}
			
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, ctx);
			sm.setDocBaiscInfo(headerData);
			
			resultList = sm.doQuerySEALList(sm.getDocBaiscInfo());
			
			if(resultList == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				setValue(resultList);
			}
			
			if(getStatus() != 1) {
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
	
	
	@Description("항목관리 인감 부문 파일업로드 ")
	@SuppressWarnings("unchecked")
	public SepoaOut doUploadSettingSealImage(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaSealManagementManager manager = getSepoaSealManagementManager(info, config, ctx);
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setDocBaiscInfo(headerData);
			
			String fmLayer = headerData.get("FM_LAYER");
			
			Map<String, String> stdSealList = manager.doSelectSealImage(fmLayer);
			if(stdSealList != null) {
				manager.doDeleteSealImage(stdSealList);
			}
			
			Map<String, String> storedFileData = null;
			for(Map<String, String> file : (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA)) {
				storedFileData = manager.doUploadSealImage(fmLayer, file, headerData);
			}
			
			if(storedFileData == null) {
				int errCode = manager.getLastErrCode();
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
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("편집툴 인감 미리보기 이미지 호출")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectSealPreviewImage(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaSealManagementManager manager = getSepoaSealManagementManager(info, config, ctx);
			manager.setDocBaiscInfo(headerData);
			
			String itemCode = headerData.get("id");
			Map<String, String> sealPreviewImage = manager.doSelectSealPreviewImage(itemCode);
			if(sealPreviewImage == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				setValue(sealPreviewImage);
				addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
			}
			
			if(getStatus() != 1) {
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
	
	@Description("문서 (논리) 삭제 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doDocumentDelete(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			boolean resultFlag = false;
			String jsonStr = headerData.get("unescaped_SEAL_DATA");
			
			Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
			Map<String, Object> paramMap = (Map<String, Object>)parseMap.get("parameter");
			Map<String, String> basicInfo = (Map<String, String>)paramMap.get("basic_info");
			String documentType = parseMap.get("TYPE").toString();
			if(StringUtils.isNotBlank(documentType)) {			
				// seal manager init
				SepoaSealManagementManager sm  = getSepoaSealManagementManager(info, config, getConnectionContext());

				// set doc data
				sm.setDocBaiscInfo(basicInfo);
		
				String targetTable = "";
				switch (documentType) {
				case "NE":
					targetTable = CREATE_TABLE;
					break;
				case "UP":
					targetTable = UPDATE_TABLE;
					break;
				case "LO":
					targetTable = LOSS_TABLE;
					break;
				case "RE":
					targetTable = RETURN_TABLE;
					break;					
				default:
					break;
				}				

				// set DEL_FLAG = Y
				resultFlag = sm.doDeleteDocument(targetTable);
				
				if(!resultFlag) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}					
				}
				
				// return map
				if(getStatus() != 1) {
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
					Rollback();
				}else {
					Commit();
				}				
			} else {
				Rollback();
				setStatus(0);
				setValue("");
				setMessage("APPLY TYPE IS EMPTY");
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
	
	@Description("문서 실물날인, 전자날인, 사외반출 FLAG update 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSealFlagUpdateService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			ConnectionContext ctx = getConnectionContext();
			Config config = new Configuration();
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
		
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, ctx);
			for(Map<String, String> row : gridData) {
				sm.setDocBaiscInfo(row);
				Map<String, String> data = new HashMap<>();
				data.put("MANAGE_NUMBER", row.get("MANAGE_NUMBER"));
				data.put("ELECTRON_FLAG", "1".equals(row.get("ELECTRON_FLAG"))?"Y":"N");
				data.put("REAL_FLAG", "1".equals(row.get("REAL_FLAG"))?"Y":"N");
				data.put("EXTERNAL_FLAG", "1".equals(row.get("EXTERNAL_FLAG"))?"Y":"N");

				if(!sm.doUpateSeal(SEAL_TABLE, data)) {
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
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}	
	
	@Description("변경전 원래 이미지 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doOriginalImage(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaSealManagementManager manager = getSepoaSealManagementManager(info, config, getConnectionContext());
			manager.setDocBaiscInfo(headerData);
			
			Map<String, String> sealImage = manager.doSelectSealImage("ORG_SEAL_IMAGE");
			
			if(sealImage == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				setValue(sealImage);
				addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
			}
			
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
	
	@Description("인장날인내역 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doSealHistoryList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
			
			//시스템관리자, 법무팀이면 조회 조건 삭제 
			/*String[] rmpartProfileCode = CommonUtil.getConfig("sepoa.legal.porfile.code", "MUP21111000001").split(",");
			String[] systemManagerProfileCode = CommonUtil.getConfig("sepoa.system.manager.profile.code", "MUP2011020001").split(",");
			if(Arrays.asList(rmpartProfileCode).contains(info.getSession("MENU_PROFILE_CODE")) ||
					Arrays.asList(systemManagerProfileCode).contains(info.getSession("MENU_PROFILE_CODE"))){
				headerData.remove("SEAL_MG_USER_ID");
				headerData.remove("USER_DEPT");
			}*/
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, new Configuration(), ctx);
			
			sm.setDocBaiscInfo(headerData);
			resultList = sm.doSealHistoryList(sm.getDocBaiscInfo());
			
			if(resultList == null) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				setValue(resultList);
			}
			
			if(getStatus() != 1) {
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
	
	@Description("결재진행")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalProgress(ConnectionContext ctx, Map<String, Object> allData) {
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
	@Description("결재완료")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalConfirm(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			
			if(SepoaApproval.APP_STATUS_FINAL.equals(MapUtils.getString(headerData, "APP_STATUS", ""))){
				SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, ctx);
				
				Map<String, String> sealInfo = new HashMap<String, String>();
				sealInfo.put("TABLE", headerData.get("SUX_WKEY1"));
				sealInfo.put("MANAGE_NUMBER", headerData.get("SUX_WKEY2"));
				sealInfo.put("DOC_NO", headerData.get("SUX_WKEY3"));
				sealInfo.put("CONFIRM_DATE", SepoaDate.getShortDateString());
				sealInfo.put("CONFIRM_TIME", SepoaDate.getShortTimeString());
				sealInfo.put("CONFIRM_USER_ID", info.getSession("ID"));
				sm.setDocBaiscInfo(sealInfo);
				
				//히스토리 적재
				if(getStatus() == 1) {
					if(!sm.doAddProcessHistory("SEAL_COMPLETE")) {
						int errCode = sm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				String finalDocStatus = "C";
				
				//Status 업데이트
				if(getStatus() == 1 && !sm.doUpdateDocStatus(sm.getDocBaiscInfo().get("TABLE"), finalDocStatus)) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				if(getStatus() == 1 && !sm.doUpdateSealStatus(finalDocStatus)) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				Map<String, String> sealDocData = sm.doQueryRegistDoc();
				//기존에 사용하고있던 용도 데이터 제거
				if(getStatus() == 1 && sm.doDeleteSealUse() == null) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				//용도 데이터 삽입
				if(getStatus() == 1) {
					Set<String >sealUseSet = new HashSet<>(Arrays.asList(sealDocData.get("SEAL_USE").split(",")));
					for(String useNo : sealUseSet) {
						if(!"".equals(useNo)) {
							if(sm.doInsertSealUse(useNo) == null) {
								int errCode = sm.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
								}
								break;
							}
							
						}
					}
				}
				
				//등록할때 사용했던 이미지 실제 인감이미지로 카피
				String origManageDocNo = sealDocData.get("MANAGE_NUMBER")+"_"+sealDocData.get("DOC_NO");
				String desManageDocNo = sealDocData.get("MANAGE_NUMBER");
				if(getStatus() == 1 && !sm.doCopySealImg(origManageDocNo, "", desManageDocNo)) {
					int errCode = sm.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				if (sm.getDocBaiscInfo().get("TABLE").equals("SSHNE")) {
					sealInfo.putAll(sealDocData);
					sm.setDocBaiscInfo(sealInfo);
					sm.doRegSeal();
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
	@Description("결재거부")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalReject(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, ctx);

			// 인장 치환 처리
			Map<String, String> sealInfo = new HashMap<String, String>();
			sealInfo.put("TABLE", headerData.get("SUX_WKEY1"));
			sealInfo.put("MANAGE_NUMBER", headerData.get("SUX_WKEY2"));
			sealInfo.put("DOC_NO", headerData.get("SUX_WKEY3"));
			sm.setDocBaiscInfo(sealInfo);

			String docStatus = "R";
			
			//Status 업데이트
			if(getStatus() == 1 && !sm.doUpdateDocStatus(sm.getDocBaiscInfo().get("TABLE"), docStatus)) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			if(getStatus() == 1 && !sm.doUpdateSealStatus(docStatus)) {
				int errCode = sm.getLastErrCode();
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
	@Description("요청회수")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalWithdraw(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = new Configuration();
			
			SepoaSealManagementManager sm = getSepoaSealManagementManager(info, config, ctx);
			
			// 인장 치환 처리
			Map<String, String> sealInfo = new HashMap<String, String>();
			sealInfo.put("TABLE", headerData.get("SUX_WKEY1"));
			sealInfo.put("MANAGE_NUMBER", headerData.get("SUX_WKEY2"));
			sealInfo.put("DOC_NO", headerData.get("SUX_WKEY3"));
			sm.setDocBaiscInfo(sealInfo);

			String docStatus = "T";
			
			//Status 업데이트
			if(getStatus() == 1 && !sm.doUpdateDocStatus(sm.getDocBaiscInfo().get("TABLE"), docStatus)) {
				int errCode = sm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			if(getStatus() == 1 && !sm.doUpdateSealStatus(docStatus)) {
				int errCode = sm.getLastErrCode();
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
}
