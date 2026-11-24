package sepoa.svc.system.login;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import filters.ThreadLocalFilter;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.login.SepoaLoginManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.SepoaWorkingLogger;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("로그인")
public class SepoaLoginService  extends SepoaService {
    public SepoaLoginService(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    @SuppressWarnings("unchecked")
    @Description("로그인")
	public SepoaOut login(Map<String, Object> allData) {
    	setStatus(1);
    	setFlag(true);

    	ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
    	try {
    		int resultCode = 1;
    		Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
    		Map<String, String> dataLIHI = new HashMap<String, String>();
    		Map<String, String> checkPW = new HashMap<String, String>(); 
    		dataLIHI.putAll(headerData);
	    	String id = headerData.get("ID");
	    	if(StringUtils.isBlank(id)) {
	    		resultCode = 100006;
	    	}else {
	    		Config config = null;
	    		try {
	    			config = new Configuration();
	    		}catch(Exception e) {
	    			resultCode  = 0;
	    		}
	    		
	    		if(resultCode == 1) {
	    			id = id.toUpperCase();
		    		String password = headerData.get("PASSWORD");
		    		SepoaLoginManager manager = new SepoaLoginManager(info, config, getConnectionContext());
		    		
		    		if(!manager.initUser(id) || !manager.checkPassword(password)) {
		    			resultCode = manager.getLastErrCode();
		    			if(resultCode == 100016) {
		    				Map<String, String> callback = manager.getLoginFailCallback();
		    				callback.put("returnType", "param");
		    				setObject(callback);
		    			}
		    		}else if(!manager.checkPassword(password)) {
		    			Map<String, String> sInfo = manager.getSessionData();
		    			checkPW.put("HOUSE_CODE", sInfo.get("HOUSE_CODE"));
		    			checkPW.put("USER_ID", id);
		            	
		            	sxp = new SepoaXmlParser(this, "doQueryPassCheckCnt");
		            	ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
		            	Map<String, String> sfMap = new SepoaFormater(ssm.doSelect(checkPW)).getListMap().get(0);
		            	
		            	String passCheckCnt = StringUtils.defaultIfEmpty(sfMap.get("PASS_CHECK_CNT"), "0");
		            	
		        		int checkCount = Integer.parseInt(passCheckCnt);
		        		
		        		if(checkCount > 5) {
		        			checkPW.put("PW_RESET_FLAG","Y");
		        			checkPW.put("PASS_CHECK_CNT", String.valueOf(checkCount));
		        			sxp = new SepoaXmlParser(this, "updatePassCheckCnt");
		                    ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
		                    ssm.doUpdate(checkPW);
		                    Commit();
		                    
		        			resultCode = 100015;
		        		} else {
		        			checkPW.put("PW_RESET_FLAG","N");
		        			checkPW.put("PASS_CHECK_CNT", String.valueOf(checkCount));
		        			sxp = new SepoaXmlParser(this, "updatePassCheckCnt");
		                    ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
		                    ssm.doUpdate(checkPW);
		                    Commit();
		                    resultCode = 100001;
		        		}
	    			}else {
		    			HttpServletRequest request = ThreadLocalFilter.getHttpServletRequest();
		    			if(request == null){
		    				resultCode  = 100007;
		    			}else {
		    				Map<String, String> sessionInfo = manager.getSessionData();
		    				
		    				checkPW.put("HOUSE_CODE", sessionInfo.get("HOUSE_CODE"));
		    				checkPW.put("USER_ID", id);
		    				
		    				sxp = new SepoaXmlParser(this, "doQueryPassCheckCnt");
			            	ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
			            	Map<String, String> sfMap = new SepoaFormater(ssm.doSelect(checkPW)).getListMap().get(0);
			            	
			            	if("N".equals(sfMap.get("PW_RESET_FLAG"))){			            		
			            		checkPW.put("PASS_CHECK_CNT", "0");
			            		checkPW.put("PW_RESET_FLAG","N");
			            		sxp = new SepoaXmlParser(this, "updatePassCheckCnt");
			            		ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
			            		ssm.doUpdate(checkPW);
			            		Commit();
			            	
			    				//Map<String, String> sessionInfo = manager.getSessionData();
			    				sessionInfo.put("current_date", headerData.get("current_date"));
			    				Configuration conf = new Configuration();
			    				dataLIHI.putAll(sessionInfo);
			    				// 위임자 목록 조회
			    				sxp = new SepoaXmlParser(this, "doSearchSMAND");
			    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			    	            List<Map<String, String>> userMandList = new SepoaFormater(ssm.doSelect(sessionInfo)).getListMap();
			    	            
			    	            // 메뉴 프로파일코드 우선순위
			    	            List<String> profileCodePriority = new ArrayList<String>();
			    	            profileCodePriority.add(conf.get("sepoa.system.manager.profile.code"));
			    	            profileCodePriority.add(conf.get("sepoa.sysadmin.manager.profile.code", ""));
			    	            String legalProfileCode = conf.get("sepoa.legal.porfile.code"); 
			    	            profileCodePriority.add(legalProfileCode);
			    	            String managerProfileCode = conf.get("sepoa.seal.manager.porfile.code"); 
			    	            profileCodePriority.add(managerProfileCode);
			    	            profileCodePriority.add(conf.get("sepoa.default.porfile.code"));
			    	            profileCodePriority.add(conf.get("sepoa.supplier.default.profile_code"));
			    	            int resultMenuProfileCodeIndex = profileCodePriority.indexOf(sessionInfo.get("MENU_PROFILE_CODE"));	// 디폴트는 본인
			    	            String userIds = id;
			    	            
			    	            // 사용자 정보상 가장 높은 프로파일을 찾고 계정목록을 세팅한다
			    	            for (Map<String, String> userMand : userMandList) {
			    	            	String mpc = userMand.get("MENU_PROFILE_CODE");
			    	            	int mpcIndex = profileCodePriority.indexOf(mpc);
			    	            	if (mpcIndex < resultMenuProfileCodeIndex) resultMenuProfileCodeIndex = mpcIndex;	// 순위가 높은(index 숫자가 적은) 인덱스로 세팅
			    	            	
			    	            	userIds = ","+userMand.get("GIVE_USER_ID");
			    	            }
			    	           
			    	            
			    	            // 인감책임자 직무를 세팅한다
			    	            Map<String, String> paramMap = new HashMap<String, String>();
			    	            paramMap.put("HOUSE_CODE", sessionInfo.get("HOUSE_CODE"));
			    	            paramMap.put("COMPANY_CODE", sessionInfo.get("COMPANY_CODE"));
			    	            paramMap.put("IDS", userIds);
			    	            
			    	            sxp = new SepoaXmlParser(this, "doSearchCTRL");
			    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			    	            List<Map<String, String>> baglList = new SepoaFormater(ssm.doSelect(paramMap)).getListMap();
			    	            if(baglList.size()>0) {
				    	            List<String> ctrl_code_list = new ArrayList<String> ();
				    				for(Map<String, String>code_map : baglList) {
				    					String ctrlCode = code_map.get("CTRL_CODE");
				    					ctrl_code_list.add(ctrlCode);
				    					
				    					if("MANAGE_CONFIRM".equals(ctrlCode) || "SEAL_CONFIRM".equals(ctrlCode) || "EXT_CONFIRM".equals(ctrlCode)) {
				    						int legalProfileIdx = profileCodePriority.indexOf(legalProfileCode);
				    						if (legalProfileIdx < resultMenuProfileCodeIndex) resultMenuProfileCodeIndex = legalProfileIdx;
				    					}
				    				}
				    				/*
				    				List<String> ctrl_type_list = new ArrayList<String> ();
				    				for(Map<String, String>type_map : baglList) {
				    					ctrl_type_list.add(type_map.get("CTRL_TYPE"));
				    				}
				    				List<String> ctrl_rep_list = new ArrayList<String> ();
				    				for(Map<String, String>req_map : baglList) {
				    					ctrl_rep_list.add(req_map.get("REPRESENT_CTRL"));
				    				}*/
				    				
				    				sessionInfo.put("CTRL_CODE", String.join(",", ctrl_code_list));
			    				}
			    	            
			    	            // 담당하는 인감이 있으면 인감담당자 프로파일 세팅
		    					sxp = new SepoaXmlParser(this, "getOwnSealList");
			    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			    	            List<Map<String, String>> sealDataList = new SepoaFormater(ssm.doSelect(paramMap)).getListMap();
			    	            List<String> sealList = new ArrayList<String>();
		    					if (sealDataList.size() > 0) {
		    						for (Map<String, String> sealDataMap : sealDataList) sealList.add(sealDataMap.get("MANAGE_NUMBER"));
		    						int managerProfileIdx = profileCodePriority.indexOf(managerProfileCode);
		    						if (managerProfileIdx < resultMenuProfileCodeIndex) resultMenuProfileCodeIndex = managerProfileIdx;
		    					} 
		    					sessionInfo.put("MY_SEAL", String.join(",", sealList));
			    	            
			    	            // 최종 프로파일코드 세팅
			    	            String resultMenuProfileCode = profileCodePriority.get(resultMenuProfileCodeIndex);
			    	            sessionInfo.put("MENU_PROFILE_CODE", resultMenuProfileCode);
			    	            
			    	            // Master Admin Flag 세팅
			    	            sxp = new SepoaXmlParser(this, "doSelectMasterCompany");
			    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			    	            SepoaFormater sf = new SepoaFormater(ssm.doSelect(sessionInfo));
			    	            if(sf.getRowCount() > 0) {
			    	            	sessionInfo.put("MASTER_COMPANY_FLAG", "Y");
			    	            }
			    	            
			    				for(Entry<String, String> entry : sessionInfo.entrySet()) {
				    				SepoaSession.putValue(request, entry.getKey(), entry.getValue());
				    			}
			    				SepoaWorkingLogger.println(SepoaSession.getAllValue(request), "SepoaLoginService", "LOGIN", "LOGIN", "LI");
			            	}else {
			            		resultCode = 100015;
			            	}
		    			}
		    		}
		    	}
	    	}
	    	
	    	if(resultCode == 1){
	    		dataLIHI.put("login_result", "성공");
	    	}else {
	    		dataLIHI.put("login_result", "실패");
	    		setStatus(resultCode);
	    		setFlag(false); 
	    	}

	    	String no = System.nanoTime() + dataLIHI.get("ID");
	    	dataLIHI.put("no",no);
	    	sxp = new SepoaXmlParser(this, "doInsertLIHI");
	    	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	    	ssm.doInsert(dataLIHI);
	    	
    	}catch(Throwable e) {
    		setStatus(0);
    		setFlag(false);
    		Logger.err.println(info.getSession("ID"), this, e);
    	}
    	return getSepoaOut();
    }
    
    @Description("강제 로그인")
	public SepoaOut forceLogin(Map<String, String> headerData) {
    	setStatus(1);
    	setFlag(true);
    	
    	ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
    	
    	try {
    		int resultCode = 1;
    		Map<String, String> dataLIHI = new HashMap<String, String>();
    		dataLIHI.putAll(headerData);
    		String id = headerData.get("ID");
	    	if(StringUtils.isBlank(id)) {
	    		resultCode = 100006;
	    	}else {
	    		Config config = null;
	    		try {
	    			config = new Configuration();
	    		}catch(Exception e) {
	    			resultCode  = 0;
	    		}
	    		
	    		if(resultCode == 1) {
	    			id = id.toUpperCase();
		    		SepoaLoginManager manager = new SepoaLoginManager(info, config, getConnectionContext());
		    		if(!manager.initUser(id)) {
		    			setStatus(manager.getLastErrCode());
		    		}else {
		    			HttpServletRequest request = ThreadLocalFilter.getHttpServletRequest();
		    			if(request == null){
		    				resultCode  = 100007;
		    			}else {
		    				SepoaLoginManager.logout(true);
		    				Map<String, String> sessionInfo = manager.getSessionData();
		    				dataLIHI.putAll(sessionInfo);
		    				sessionInfo.put("current_date", headerData.get("current_date"));
		    				Configuration conf = new Configuration();
		    				
		    				// 위임자 목록 조회
		    				sxp = new SepoaXmlParser(this, "doSearchSMAND");
		    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		    	            List<Map<String, String>> userMandList = new SepoaFormater(ssm.doSelect(sessionInfo)).getListMap();
		    	            
		    	            // 메뉴 프로파일코드 우선순위
		    	            List<String> profileCodePriority = new ArrayList<String>();
		    	            profileCodePriority.add(conf.get("sepoa.sysadmin.manager.profile.code", ""));
		    	            profileCodePriority.add(conf.get("sepoa.system.manager.profile.code"));
		    	            String legalProfileCode = conf.get("sepoa.legal.porfile.code"); 
		    	            profileCodePriority.add(legalProfileCode);
		    	            String managerProfileCode = conf.get("sepoa.seal.manager.porfile.code"); 
		    	            profileCodePriority.add(managerProfileCode);
		    	            profileCodePriority.add(conf.get("sepoa.default.porfile.code"));
		    	            profileCodePriority.add(conf.get("sepoa.supplier.default.profile_code"));
		    	            int resultMenuProfileCodeIndex = profileCodePriority.indexOf(sessionInfo.get("MENU_PROFILE_CODE"));	// 디폴트는 본인
		    	            String userIds = id;
		    	            
		    	            // 사용자 정보상 가장 높은 프로파일을 찾고 계정목록을 세팅한다
		    	            for (Map<String, String> userMand : userMandList) {
		    	            	String mpc = userMand.get("MENU_PROFILE_CODE");
		    	            	int mpcIndex = profileCodePriority.indexOf(mpc);
		    	            	if (mpcIndex < resultMenuProfileCodeIndex) resultMenuProfileCodeIndex = mpcIndex;	// 순위가 높은(index 숫자가 적은) 인덱스로 세팅
		    	            	
		    	            	userIds = ","+userMand.get("GIVE_USER_ID");
		    	            }
		    	           
		    	            
		    	            // 인감책임자 직무를 세팅한다
		    	            Map<String, String> paramMap = new HashMap<String, String>();
		    	            paramMap.put("HOUSE_CODE", sessionInfo.get("HOUSE_CODE"));
		    	            paramMap.put("COMPANY_CODE", sessionInfo.get("COMPANY_CODE"));
		    	            paramMap.put("IDS", userIds);
		    	            
		    	            sxp = new SepoaXmlParser(this, "doSearchCTRL");
		    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		    	            List<Map<String, String>> baglList = new SepoaFormater(ssm.doSelect(paramMap)).getListMap();
		    	            if(baglList.size()>0) {
			    	            List<String> ctrl_code_list = new ArrayList<String> ();
			    				for(Map<String, String>code_map : baglList) {
			    					String ctrlCode = code_map.get("CTRL_CODE");
			    					ctrl_code_list.add(ctrlCode);
			    					
			    					if("MANAGE_CONFIRM".equals(ctrlCode) || "SEAL_CONFIRM".equals(ctrlCode) || "EXT_CONFIRM".equals(ctrlCode)) {
			    						int legalProfileIdx = profileCodePriority.indexOf(legalProfileCode);
			    						if (legalProfileIdx < resultMenuProfileCodeIndex) resultMenuProfileCodeIndex = legalProfileIdx;
			    					}
			    				}
			    				/*
			    				List<String> ctrl_type_list = new ArrayList<String> ();
			    				for(Map<String, String>type_map : baglList) {
			    					ctrl_type_list.add(type_map.get("CTRL_TYPE"));
			    				}
			    				List<String> ctrl_rep_list = new ArrayList<String> ();
			    				for(Map<String, String>req_map : baglList) {
			    					ctrl_rep_list.add(req_map.get("REPRESENT_CTRL"));
			    				}*/
			    				
			    				sessionInfo.put("CTRL_CODE", String.join(",", ctrl_code_list));
		    				}
		    	            
		    	            // 담당하는 인감이 있으면 인감담당자 프로파일 세팅
	    					sxp = new SepoaXmlParser(this, "getOwnSealList");
		    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		    	            List<Map<String, String>> sealDataList = new SepoaFormater(ssm.doSelect(paramMap)).getListMap();
		    	            List<String> sealList = new ArrayList<String>(sealDataList.size());
	    					if (sealDataList.size() > 0) {
	    						for (Map<String, String> sealDataMap : sealDataList) sealList.add(sealDataMap.get("MANAGE_NUMBER"));
	    						int managerProfileIdx = profileCodePriority.indexOf(managerProfileCode);
	    						if (managerProfileIdx < resultMenuProfileCodeIndex) resultMenuProfileCodeIndex = managerProfileIdx;
	    					} 
	    					sessionInfo.put("MY_SEAL", String.join(",", sealList));
	    					
		    	            // 최종 프로파일코드 세팅
		    	            String resultMenuProfileCode = profileCodePriority.get(resultMenuProfileCodeIndex);
		    	            sessionInfo.put("MENU_PROFILE_CODE", resultMenuProfileCode);
		    	            
		    	            // Master Admin Flag 세팅
		    	            sxp = new SepoaXmlParser(this, "doSelectMasterCompany");
		    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		    	            SepoaFormater sf = new SepoaFormater(ssm.doSelect(sessionInfo));
		    	            if(sf.getRowCount() > 0) {
		    	            	sessionInfo.put("MASTER_COMPANY_FLAG", "Y");
		    	            }
		    	           
		    				for(Entry<String, String> entry : sessionInfo.entrySet()) {
			    				SepoaSession.putValue(request, entry.getKey(), entry.getValue());
			    			}
		    				SepoaWorkingLogger.println(SepoaSession.getAllValue(request), "SepoaLoginService", "LOGIN", "LOGIN", "LI");
		    			}
		    		}
		    	}
	    	}

	    	if(resultCode == 1){
	    		dataLIHI.put("login_result", "성공");
	    	}else {
	    		dataLIHI.put("login_result", "실패");
	    		setStatus(resultCode);
	    		setFlag(false); 
	    	}

	    	String no = System.nanoTime() + dataLIHI.get("ID");
	    	dataLIHI.put("no",no);
	    	sxp = new SepoaXmlParser(this, "doInsertLIHI");
	    	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	    	ssm.doInsert(dataLIHI);
	    	
    	}catch(Throwable e) {
    		setStatus(0);
    		setFlag(false);
    		Logger.err.println(info.getSession("ID"), this, e);
    	}
    	return getSepoaOut();
    }
    
    @Description("비밀번호 초기화")
    public SepoaOut resetPassword(Map<String, Object> allData) {
    	setStatus(1);
    	setFlag(true);
    	try {
    		Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
    		String userId = headerData.get("USER_ID");
    		String userEmail = headerData.get("EMAIL");
    		String newPwd = headerData.get("NEW_PASSWORD");
    		String mailFlag = headerData.get("SEND_MAIL_FLAG");
    		Config config = new Configuration();
    		SepoaXmlParser sxp = null;
            SepoaSQLManager ssm = null;
            
    		SepoaLoginManager manager = new SepoaLoginManager(info, config, getConnectionContext());
    		
    		if(!manager.initUser(userId)) {
    			setStatus(manager.getLastErrCode());
    		} else {
    			boolean clearYn = manager.clearPassword(newPwd, userEmail);
    			if(!clearYn) {
    				setStatus(0);
    			} else {
    				Map<String, String> checkPW = new HashMap<String, String>();
    				Map<String, String> sInfo = manager.getSessionData();
    				int checkCount = 0;
    				checkPW.put("PW_RESET_FLAG","N");
        			checkPW.put("PASS_CHECK_CNT", String.valueOf(checkCount));
        			checkPW.put("HOUSE_CODE", sInfo.get("HOUSE_CODE"));
        			checkPW.put("USER_ID", userId);
        			sxp = new SepoaXmlParser(this, "updatePassCheckCnt");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
                    ssm.doUpdate(checkPW);
                    Commit();
    			}
    		}
    	} catch(Exception e) {
    		setStatus(0);
    		setFlag(false);
    		Logger.err.println(info.getSession("ID"), this, e);
    	}
    	return getSepoaOut();
    }
    
    @Description("비밀번호 확인")
    public SepoaOut docheckpwexist(Map<String, Object> allData) {
    	setFlag(true); 
    	try {
    		Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
    		String id = headerData.get("ID");
    		String inputpw = headerData.get("PASSWORD");    		
    		Config config = new Configuration();
    		SepoaLoginManager manager = new SepoaLoginManager(info, config, getConnectionContext());
    		if(!manager.initUser(id) || !manager.checkPassword(inputpw)){
    			setStatus(0);
    			setFlag(false);
    		}else {
    			setStatus(1);
    			setFlag(true);
    		}
    	} catch(Exception e) {
    		setStatus(0);
    		setFlag(false);
    		Logger.err.println(info.getSession("ID"), this, e);
    	}
    	return getSepoaOut();
    }
    
}
