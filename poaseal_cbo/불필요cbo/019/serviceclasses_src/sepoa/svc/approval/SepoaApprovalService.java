package sepoa.svc.approval;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import sepoa.fw.approval.SepoaApprovalConfirm;
import sepoa.fw.approval.SepoaApprovalReject;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.ux.link.SepoaLinkManager;
import sepoa.seal.SepoaReqSealManager;
import sepoa.seal.SepoaSealManagementManager;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("결재 서비스")
public class SepoaApprovalService extends SepoaService {
	List<String> innerList = new ArrayList<>();
	
	public SepoaApprovalService(String opt, SepoaInfo info) throws SepoaServiceException {
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

	@Description("승인/미승인함 조회")
	public SepoaOut doQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
			if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {	// 관리자
				headerData.put("APP_USER_ID", "");
				headerData.put("APP_LINE_SEQ", "1");
				headerData.put("SHOW", "true");
			}else {
				headerData.put("APP_USER_ID", info.getSession("ID"));
			}
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}

	@Description("결재선 조회")
	public SepoaOut getAppLine(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	
	@Description("반출신청의 결재선 조회")
	public SepoaOut doQueryExtSelect(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	
	@Description("날인신청현황의 결재선 조회")
	public SepoaOut doQuerySelect(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			setValue(sf);
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	
	@Description("결재 승인")
	public SepoaOut doApprovalConfirm(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doQueryApprovalInfo");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			headerData.put("APP_TYPE", sf.getValue("APP_TYPE", 0));
			allData.put("APP_LINE", sf.getListMap());
			headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
			
			// 결재
            Map<String, String> map = new HashMap<>();
            map.put("returnType" , "param");
            map.put("APP_NO"     , MapUtils.getString(headerData, "APP_NO"     , ""));
            map.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
            map.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "I"));
            map.put("SAVE_MODE", MapUtils.getString(headerData,"SAVE_MODE",""));
            setObject(map);
            
            //LinkManager 생성
            Configuration conf = new Configuration();
            HttpServletRequest req = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
            String baseURL = req.getRequestURL().toString().replace(req.getRequestURI(),"");
            String appNo 	= MapUtils.getString(headerData, "APP_NO", "");
            String appNoSeq = MapUtils.getString(headerData, "APP_NO_SEQ" , "");
            
            String conFirmUser = "";
            String companyCode = info.getSession("COMPANY_CODE");
            String appUser = "";
            String smrNumber = "";

            for (Map<String, String> linkMgrAppLine : sf.getListMap()) {
            	if(linkMgrAppLine.get("APP_LINE_STATUS").equals("L01")) {
            		appUser = linkMgrAppLine.get("APP_USER_ID");
            		smrNumber = linkMgrAppLine.get("SUX_WKEY1");
            	} else if (linkMgrAppLine.get("APP_LINE_STATUS").equals("") && "".equals(conFirmUser)) {
            		conFirmUser = linkMgrAppLine.get("APP_USER_ID");
            	}
            }
            
            Map<String, String> mailCustomData = new HashMap<String, String>();
            SepoaLinkManager linkManager = new SepoaLinkManager(info, conf, ctx);
            Map<String, String> linkParams = new HashMap<String, String>();
            
            String screenType = "";
			switch (headerData.get("APP_TYPE")) {
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
            
            linkParams.put("APP_NO", appNo);
            linkParams.put("APP_NO_SEQ", appNoSeq);
            linkParams.put("SMR_NUMBER", smrNumber);
            linkParams.put("SCREEN_TYPE", screenType);
            linkParams.put("APP_TYPE", headerData.get("APP_TYPE"));
            linkParams.put("RE_FLAG", headerData.get("RE_FLAG"));
            if(!linkManager.newLink("approvalm", appNo, appNoSeq) || !linkManager.setParams(linkParams)) {
            	int errCode = linkManager.getLastErrCode();
            }else{
            	if(conFirmUser == "") {
            		String privateLink = linkManager.addUser(companyCode, appUser);
            		mailCustomData.put("URL", baseURL + "/link.sux?ln="+privateLink);
            	}else {
            		String privateLink = linkManager.addUser(companyCode, conFirmUser);
            		mailCustomData.put("URL", baseURL + "/link.sux?ln="+privateLink);
            	}
            	allData.put("MAIL_CUSTOM_DATA", mailCustomData);
            }
            
            SepoaApprovalConfirm sepoaApprovalConfirm = new SepoaApprovalConfirm(ctx, info);
            sepoaApprovalConfirm.doApprovalProcess(allData);
            
            // 결재 승인 날인이력 히스토리 적재
            Boolean historyFlag = true;
            if(sf.getListMap().get(sf.getListMap().size()-1).get("APP_USER_ID").equals(info.getSession("ID"))) {
            	historyFlag = false;
            }
            
            if(historyFlag) {
	            SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
	            Map<String, String> tmpData = new HashMap<>();
	            tmpData.put("SMR_NUMBER", smrNumber);
	            srm.setDocBaiscInfo(tmpData);
	            
	            if(getStatus() == 1) {
					if(!srm.doAddProcessHistory("SEAL_COMPLETE")) {
						int errCode = srm.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				} 
            }
            
			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	
	@Description("결재 반려")
	public SepoaOut doApprovalReject(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();

		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doQueryApprovalInfo");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			headerData.put("APP_TYPE", sf.getValue("APP_TYPE", 0));
			allData.put("APP_LINE", sf.getListMap());
			headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
			//headerData.put("APP_TYPE", reqSealInfo.get("APP_TYPE"));
			
			// 결재
            Map<String, String> map = new HashMap<>();
            map.put("returnType" , "param");
            map.put("APP_NO"     , MapUtils.getString(headerData, "APP_NO"     , ""));
            map.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
            map.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "R"));
            map.put("SAVE_MODE", MapUtils.getString(headerData,"SAVE_MODE",""));
            setObject(map);
            
            //LinkManager 생성
            Configuration conf = new Configuration();
            
            HttpServletRequest req = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
            String baseURL = req.getRequestURL().toString().replace(req.getRequestURI(),"");
            String appNo 	= MapUtils.getString(headerData, "APP_NO", "");
            String appNoSeq = MapUtils.getString(headerData, "APP_NO_SEQ" , "");
            
            String conFirmUser = "";
            String companyCode = "";
            String smrNumber = "";
            for (Map<String, String> linkMgrAppLine : sf.getListMap()) {
            	if(linkMgrAppLine.get("APP_LINE_STATUS").equals("L01")) {
            		conFirmUser = linkMgrAppLine.get("APP_USER_ID");
            		companyCode = linkMgrAppLine.get("COMPANY_CODE");
            		smrNumber = linkMgrAppLine.get("SUX_WKEY1");
            		break;
            	}
            }
            Map<String, String> mailCustomData = new HashMap<String, String>();
            SepoaLinkManager linkManager = new SepoaLinkManager(info, conf, ctx);
            Map<String, String> linkParams = new HashMap<String, String>();
            
            String screenType = "";
			switch (headerData.get("APP_TYPE")) {
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
			
            linkParams.put("APP_NO", appNo);
            linkParams.put("APP_NO_SEQ", appNoSeq);
            linkParams.put("SMR_NUMBER", smrNumber);
            linkParams.put("SCREEN_TYPE", screenType);
            linkParams.put("APP_TYPE", headerData.get("APP_TYPE"));
            linkParams.put("RE_FLAG", headerData.get("RE_FLAG"));
            if(!linkManager.newLink("approvalm", appNo, appNoSeq) || !linkManager.setParams(linkParams)) {
            	int errCode = linkManager.getLastErrCode();
            }else{
            	String privateLink = linkManager.addUser(companyCode, conFirmUser); 
            	mailCustomData.put("URL", baseURL + "/link.sux?ln="+privateLink);
            	allData.put("MAIL_CUSTOM_DATA", mailCustomData);
            }
            
            SepoaApprovalReject sepoaApprovalReject = new SepoaApprovalReject(ctx, info);
            sepoaApprovalReject.doApprovalProcess(allData);
			
			Commit();
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	
	@Description("대리결재자 리스트 조회")
    @SuppressWarnings("unchecked")
    public SepoaOut doQueryDelegation(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);

            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            String myMenuProfile = info.getSession("MENU_PROFILE_CODE");
            if (myMenuProfile.equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {	// 관리자
				headerData.put("IS_ADMIN", "true");
			}
            
            String session_house_code = info.getSession("HOUSE_CODE");
            String session_company_code = info.getSession("COMPANY_CODE");
            String session_language = info.getSession("LANGUAGE");
            
            Map<String, String> map = new HashMap<String, String>();
            
            map.put("house_code", session_house_code);
            map.put("company_code", session_company_code);
            
            sxp = new SepoaXmlParser();
            map.put("HOUSE_CODE", session_house_code);
            map.put("COMPANY_CODE", session_company_code);
            map.put("LANGUAGE", session_language);
            
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData,map));
            
        } catch (Exception e) {
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
	
	@Description("대리결재(부재자) 설정 저장")
    @SuppressWarnings("unchecked")
    public SepoaOut doDelegation(Map<String, Object> allData) {
    	ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;

        try {
            setStatus(1);
            setFlag(true);

            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
            for(Map<String, String> map : gridData){
                if("".equals(map.get("DELEGATION_KEY"))){//해당 값이 없다면 인서트, 행추가, 또는 기존행 복사
                	map.put("DELEGATION_KEY", DocumentUtil.getDocNumberRemote(info, "DELEGATION").result[0]);    
                	
                    sxp = new SepoaXmlParser(this, "doInsertSMAND");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doInsert(map);

                }else if(!"".equals(map.get("DELEGATION_KEY"))) {
                    sxp = new SepoaXmlParser(this, "doUpdateSMAND");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doUpdate(map);
                }
            }
            
            Commit();
            
        } catch (Exception e) {
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
	
	@Description("대리결재자 삭제")
    @SuppressWarnings("unchecked")
    public SepoaOut doDelete(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
            
            for(Map<String, String> map : gridData){
                sxp = new SepoaXmlParser(this, "doDeleteSMAND");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doDelete(map);
            }
            
            Commit();

        } catch (Exception e) {
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
	
	@Description("대리결재자인지 확인")
    @SuppressWarnings("unchecked")
    public SepoaOut doCheckDelegation(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            setStatus(1);
            setFlag(true);
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            if(sf.getRowCount() > 0) {
            	setValue(ssm.doSelect(headerData));            	
            }else {
            	setStatus(0);
            }
        } catch (Exception e) {
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
	@Description("결재 미처리건 작업")
	@SuppressWarnings("unchecked")
	public SepoaOut ApprovalReceiver() {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		List<Map<String, String>> list = null;
		SepoaOut so = null;
		
		try {
			sxp = new SepoaXmlParser(this, "getNonProsessedApproval");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(new HashMap<>()));
			if (sf.getRowCount() > 0) {
				list = sf.getListMap();
			
				for (Map<String, String> map : list) {
					String houseCode = map.get("HOUSE_CODE");
					String companyCode = map.get("COMPANY_CODE");
					String confirmUserId = MapUtils.getString(map, "CONFIRM_USER_ID");	// 승인자
					SepoaInfo tempInfo = new SepoaInfo(houseCode,"ID="+confirmUserId+"^@^LANGUAGE=KO^@^NAME_LOC=Batch^@^NAME_ENG=Batch^@^COMPANY_CODE="+companyCode+"^@^");
					tempInfo.setThreadLocal();
					String smrNumber = "";
					
					// 20220829 그룹웨어 결재상태 받을 수 있도록 이런 방식으로 만들었으나 지금은 날인신청 결재완료만 구현했으니 나머지는 필요하면 구현할 것
					
					try {
						Map<String, Object> allMap = new HashMap<String, Object>();
						Map<String, String> headerMap = new HashMap<String, String>();
						allMap.put(SepoaModuleDataMapper.KEY_HEADER_DATA, headerMap);
						Object[] paramObj = { ctx, allMap };
						headerMap.put("HOUSE_CODE", houseCode);
						headerMap.put("COMPANY_CODE", companyCode);
						headerMap.put("ACCEPT_REASON", map.get("APP_COMMENT"));	// 결재사유
						smrNumber = MapUtils.getString(map, "SMR_NUMBER");
						headerMap.put("SUX_WKEY1", smrNumber);
						headerMap.put("SMR_NUMBER", smrNumber);
						headerMap.put("RE_FLAG", MapUtils.getString(map, "RE_FLAG", ""));
						headerMap.put("SIGN_USER_ID_FORCED", confirmUserId);
						
						String applyFor = MapUtils.getString(map, "APPLY_FOR");
						String appStatus = MapUtils.getString(map, "APP_STATUS");
						String method;
						if ("CR".equals(appStatus)) {	// 결재대기
							method = "doApprovalConfirmPostprocess";
							headerMap.put("APP_STATUS", "F");
						} else if ("R".equals(appStatus)) {
							method = "doApprovalReject";
							headerMap.put("APP_STATUS", "R");
						} else if ("I".equals(appStatus)) {
							method = "doApprovalProgress";
							headerMap.put("APP_STATUS", "I");
						} else {
							continue;
						}
						
						String sealProcess = CommonUtil.getConfig("sepoa.seal.process","");
						if ("XT".equals(applyFor)) {	// 반출
							so = ServiceConnector.doService(tempInfo, "sepoa.svc.seal"+sealProcess+".SepoaReqExternalService", "TRANSACTION", method, paramObj);
						} else {	// 날인신청
							so = ServiceConnector.doService(tempInfo, "sepoa.svc.seal"+sealProcess+".SepoaReqSealService", "TRANSACTION", method, paramObj);
						}
						
						String postprocessed = null;
						if (so.flag) {
							postprocessed = "Y";
			            } else {
			            	Rollback();
			            	
			            	SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddhhmmss");
			    			Date addDateTime = fmt.parse(map.get("ADD_DATE")+map.get("ADD_TIME"));
			    			Date now = fmt.parse(SepoaDate.getShortDateString()+SepoaDate.getShortTimeString());
			    			long diffMin = (now.getTime() - addDateTime.getTime()) / 60000;	// 분
			    			if (diffMin > Long.parseLong(CommonUtil.getConfig("sepoa.approval.error.minute","10"))) {	// 시간이 지났는데도 계속 처리가 안됐다면 오류처리
			    				postprocessed = "E";
			    			}
			            }
						
						if (postprocessed != null) {
							// DLCON_IF_APP 상태값 변경 (POSTPROCESSED = 'Y' or 'E')
							headerMap.put("POSTPROCESSED", postprocessed);							
							headerMap.put("PP_SEQ", MapUtils.getString(map, "SEQ"));
							sxp = new SepoaXmlParser(this, "updatePostProcessed");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.doUpdate(headerMap);
							
							if ("E".equals(postprocessed)) {
								String queryName = null;
								if ("XT".equals(applyFor)) {	// 반출
									queryName = "updateStatusSSETL";
								} else {	// 날인신청
									queryName = "updateStatusSSIAD";
								}
								if ("CR".equals(appStatus)) {	// 결재완료대기
									headerMap.put("SMR_STATUS", "CE");	// 결재완료처리 오류
								}
								sxp = new SepoaXmlParser(this, queryName);
								ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
								ssm.doUpdate(headerMap);
							}
							
							Commit();
						}
						
						if (!so.flag) {
							throw new Exception(so.message);
						}
					} catch (Exception e) {
						String log = "[ERROR-ApprovalReceiver : "+smrNumber+"] [" + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(new Date())+ "] " + e;
						e.printStackTrace();
						System.out.println(log);
						Logger.err.println(log);
						Rollback();
						continue;
					}
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
}
