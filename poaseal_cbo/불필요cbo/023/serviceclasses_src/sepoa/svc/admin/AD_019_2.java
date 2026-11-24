package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.springframework.core.Conventions;

import com.google.gson.Gson;

import listeners.SepoaSessionListener;
import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.jtx.SepoaTransactionalResource;
import sepoa.fw.log.Logger;
import sepoa.fw.login.SepoaLoginManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.seal.SepoaReqSealManager;

@SuppressWarnings({ "unused", "unchecked" })
@Description("사용자")
@Permission(key="WORK_TYPE", value="Z")
public class AD_019_2 extends SepoaService {


    public AD_019_2(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    @SuppressWarnings("unchecked")
    @Description("상세조회")
    public SepoaOut doFormLoad(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);

            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

            setValue(ssm.doSelect(headerData));
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    @Description("저장")
    public SepoaOut doFormSave(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            HttpServletRequest request = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
            
            
            String password = "";
            if(headerData.containsKey("PASSWORD_!")) {
            	password = headerData.get("PASSWORD_1");
            }else {
            	password = headerData.get("USER_ID");
            }
            headerData.put("USER_ID", headerData.get("USER_ID").toUpperCase());
            
			String changeString[] = { "$", "&" };
			if((!"".equals(password) && null != password)) {
			changeString[0] = java.util.regex.Matcher.quoteReplacement("$");
			changeString[1] = java.util.regex.Matcher.quoteReplacement("&");
			password = password.replaceAll("&#36;", changeString[0]);
			password = password.replaceAll("&#38;", changeString[1]);
			
            headerData.put("PASSWORD_1", SepoaAuthEncryptManager.getEncrypted(password));
			}
            
            headerData.put("DATE_FORMAT","%Y/%m/%d");
            headerData.put("_", SepoaAuthEncryptManager.getEncrypted(headerData.get("USER_ID")).toUpperCase());
            String UserType = headerData.get("USER_TYPE");
            if("S".equals(UserType)) {
            	headerData.put("COMPANY_CODE", headerData.get("SELLER_CODE"));
            }else {
            	headerData.put("COMPANY_CODE", headerData.get("COMPANY_CODE"));
            }
            
            String adminProfileCode = CommonUtil.getConfig("sepoa.system.manager.profile.code", "MUP2011020001");
            if(adminProfileCode.equalsIgnoreCase(headerData.get("MENU_PROFILE_CODE"))) {
            	headerData.put("WORK_TYPE", "Z");
            } else {
            	headerData.put("WORK_TYPE", "G");
            }
            
            sxp = new SepoaXmlParser(this, "doFormLoad");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            if(sf.getListMap().size() > 0) {
            	sxp = new SepoaXmlParser(this, "doUpdateSUSMT");
            	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            	ssm.doUpdate(headerData);
            }else {
            	sxp = new SepoaXmlParser(this, "doInsertSUSMT");
            	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            	ssm.doUpdate(headerData);
            }
            
            sf = new SepoaFormater(doFormLoad(allData).result[0]);
            if(sf.getRowCount() > 0) {
                SepoaSession.putValue(request, "THEME", sf.getValue("THEME", 0));
            }
            Commit();
        } catch (Exception e) {
            Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    @Description("수정")
    public SepoaOut changeUserInfo(Map<String, Object> allData){
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            
            String password = "";
            String newPassword = headerData.get("NEW_PASSWORD");
//20171208 USER_TYPE 조건 삭제            
//            if("P".equals(headerData.get("USER_TYPE"))){
//                password = getEncryptedPassword(headerData.get("PASSWORD"));
//            }else if("S".equals(headerData.get("USER_TYPE"))){
//                password = headerData.get("PASSWORD");
//                if(newPassword != null && !"".equals(newPassword)){
//                    newPassword = getEncryptedPassword(newPassword);
//                }
//            }else{
//                throw new Exception(Cache.text.get("AD_019_2.MESSAGE0001"));//유저정보가 맞지않습니다.
//            }

            password = headerData.get("PASSWORD");
			String changeString[] = { "$", "&" };

			changeString[0] = java.util.regex.Matcher.quoteReplacement("$");
			changeString[1] = java.util.regex.Matcher.quoteReplacement("&");
			password = password.replaceAll("&#36;", changeString[0]);
			password = password.replaceAll("&#38;", changeString[1]);
			
            password = SepoaAuthEncryptManager.getEncrypted(password);
            if(newPassword != null && !"".equals(newPassword)) {
            	newPassword = SepoaAuthEncryptManager.getEncrypted(newPassword);
            }
            
            headerData.put("password"  , password);
            headerData.put("user_id"   , info.getSession("ID"));
            headerData.put("house_code", info.getSession("HOUSE_CODE"));
            headerData.put("PASSWORD_1", newPassword);
            
            SepoaLoginManager manager = new SepoaLoginManager(info, new Configuration(), getConnectionContext());
    		if(!manager.initUser(info.getSession("ID")) || !manager.checkPassword(password)) {
    			throw new Exception(Cache.text.get("AD_019_2.MESSAGE0001"));//비밀번호가 틀립니다.
    		}
            
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            sxp = new SepoaXmlParser(this, "doUpdateSADDR");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            Commit();
        } catch (Exception e) {
            Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    @Description("삭제")
    public SepoaOut doDelete(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            if(headerData.get("MENU_PROFILE_CODE").equals(CommonUtil.getConfig("sepoa.system.manager.profile.code"))) {
            	setStatus(2);
                setValue("");
                setFlag(false);
            } else {
	            HttpServletRequest request = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
	            sxp = new SepoaXmlParser();
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            ssm.doUpdate(headerData);
	            Commit();
	            
	            SepoaSessionListener.logout(headerData.get("USER_ID"));
            }
        } catch (Exception e) {
            Rollback();
            e.printStackTrace();
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    @Description("임시 비밀번호 초기화")
    public SepoaOut tempPWreset(Map<String, Object> allData) {
    	setStatus(1);
    	setFlag(true);
    	ConnectionContext ctx = getConnectionContext();
    	SepoaXmlParser sxp = null;
    	SepoaSQLManager ssm = null;
    	SepoaFormater sf = null;
    	try {
    		Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
    		sxp = new SepoaXmlParser(this, "selectUser");
    		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    		sf = new SepoaFormater(ssm.doSelect(headerData));
    		List<Map<String, String>> userlist = sf.getListMap();
    		for (Map<String, String> map : userlist) {
    			map.put("password", SepoaAuthEncryptManager.getEncrypted(MapUtils.getString(map, "USER_ID","")));
			}
    		sxp = new SepoaXmlParser(this, "updateUser");
    		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    		ssm.doUpdate(userlist,true);
    		Commit();
    	} catch (Exception e) {
    		Rollback();
    		e.printStackTrace();
    		setStatus(0);
    		setValue("");
    		setFlag(false);
    		setMessage(e.getMessage());
    		Logger.err.println(info.getSession("ID"), this, e.getMessage());
    	}
    	return getSepoaOut();
    }
    
    @Description("사용자변경")
    public SepoaOut changeUserLogon(Map<String, Object> allData){
        setFlag(true);
        setStatus(1);
        try{
        	/*if(!"A".equals(info.getSession("USER_TYPE"))) {
        		throw new Exception("not have permission");
        	}*/
        	Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
        	SepoaOut wo = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION", "forceLogin", new Object[] {headerData});
            if(wo.status != 1){
                throw new Exception("not found user");
            }
        }catch(Exception e){
            setFlag(false);
            setStatus(0);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    @SuppressWarnings("unchecked")
    @Description("아이디 중복체크")
    public SepoaOut doCheckId(Map<String, Object> allData) {
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
            
            setValue(sf);            	            	
            
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e.getMessage());
        }
        return getSepoaOut();
    }
    
    @Description("사용자정보 엑셀업로드")
	public SepoaOut doUploadInterfaceData(Map<String, Object> allData) {
    	ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData,SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
            
            if(excelDataList == null || excelDataList.size() == 0) {
            	setStatus(105002);
            	Logger.err.println("Not Found User Interface Data!");
            	throw new Exception("Not Found User Interface Data!");
            } 
            
            //등록된 부서정보조회
            sxp = new SepoaXmlParser(this, "doSelectSDEPT");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            //매뉴프로파일코드, 직급, 국가, 언어, 부서, 직책
            final String userProfileStr = Cache.combo.get("SL0035,"+info.getSession("LANGUAGE"));
            final String userPositionStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+'#'+info.getSession("LANGUAGE")+"#UX049");
            final String userCountryStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+'#'+info.getSession("LANGUAGE")+"#AD002");
            final String userLagauageStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+'#'+info.getSession("LANGUAGE")+"#AD003");
            final String userResponsibilityStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+'#'+info.getSession("LANGUAGE")+"#UX050");
            
            final List<Map<String,String>> userProfileDefine = new Gson().fromJson(userProfileStr, List.class);
            final List<Map<String,String>> userPrositionDefine = new Gson().fromJson(userPositionStr, List.class); 
            final List<Map<String,String>> userCountryDefine = new Gson().fromJson(userCountryStr, List.class); 
            final List<Map<String,String>> userLaguageDefine = new Gson().fromJson(userLagauageStr, List.class); 
            final List<Map<String,String>> userResponsibilityDefine = new Gson().fromJson(userResponsibilityStr, List.class); 
            final List<Map<String,String>> userDeptDefine = sf.getListMap();

            List<Map<String,String>> converExcelDataList = new ArrayList<Map<String,String>>();
            for(Map<String,String> rowData : excelDataList) {
            	Map<String,String> convertRowData = new HashMap<String,String>();
            	for(Entry<String, String> data : rowData.entrySet()) {
            		String key = data.getKey();
            		
            		switch (key) {
					case "MENU_PROFILE_CODE":
						try {
							convertRowData.put(data.getKey(), userProfileDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
						} catch (Exception e) {
							convertRowData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105040"));
							Logger.err.println("MENU_PROFILE_CODE [" + rowData.get("MENU_PROFILE_CODE") + "] Upload Fail!");
						}
						break;
					case "POSITION":
						try {
							convertRowData.put(data.getKey(), userPrositionDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
						} catch (Exception e) {
							convertRowData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105041"));
							Logger.err.println("POSITION [" + rowData.get("POSITION") + "] Upload Fail!");
						}
						break;
					case "COUNTRY":
						try {
							convertRowData.put(data.getKey(), userCountryDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
						} catch (Exception e) {
							convertRowData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105042"));
							Logger.err.println("COUNTRY [" + rowData.get("COUNTRY") + "] Upload Fail!");
						}
						break;
					case "LANGUAGE":
						try {
							convertRowData.put(data.getKey(), userLaguageDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
						} catch (Exception e) {
							convertRowData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105043"));
							Logger.err.println("LANGUAGE [" + rowData.get("LANGUAGE") + "] Upload Fail!");
						}
						break;
					case "DEPT_NAME": 
						try {
							convertRowData.put("DEPT", userDeptDefine.stream().filter(e -> data.getValue().equals(e.get("DEPT_NAME_LOC"))).findFirst().get().get("DEPT"));
							convertRowData.put(data.getKey(), data.getValue());
						} catch (Exception e) {
							convertRowData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105044"));
							Logger.err.println("DEPT_NAME [" + rowData.get("DEPT_NAME") + "] Upload Fail!");
						}
						break;
					case "RESPONSIBILITY":
						try {
							convertRowData.put(data.getKey(), userResponsibilityDefine.stream().filter(e -> data.getValue().equals(e.get("text"))).findFirst().get().get("value"));
						} catch (Exception e) {
							convertRowData.put(data.getKey(), "");
							Logger.err.println("RESPONSIBILITY [" + rowData.get("RESPONSIBILITY") + "] Upload Fail!");
						}
						break;
					default:
						if(!"RESULT".equals(data.getKey())) {
							convertRowData.put(data.getKey(), data.getValue());
						}
						break;
					}
            	}
            	converExcelDataList.add(convertRowData);
            }
        	setValue(new Gson().toJson(converExcelDataList));
        	
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
    
    @Description("사용자정보 업로드데이터 저장")
	public SepoaOut doSaveInterfaceData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext newCtx = null; 
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData,SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);
            List<Map<String, String>> resultExcelDataList = new ArrayList<Map<String, String>>();
            newCtx = new SepoaTransactionalResource().getConnectionContext();
            	for(Map<String,String> rowData : excelDataList) {
            		Map<String, String> tmpData = new HashMap<String, String>();
    				tmpData.putAll(rowData);
    				int status = 1;
            		
            		try {
            			
            			rowData.put("USER_ID", rowData.get("USER_ID").toUpperCase());
            			if("".equals(rowData.get("PASSWORD_1"))) {
            				rowData.put("PASSWORD_1", rowData.get("USER_ID"));
            			}
            			
	            		if(!"".equals(rowData.get("PASSWORD_1"))) {
	            			String password = rowData.get("PASSWORD_1");
	            			String changeString[] = { "$", "&" };
	            			if((!"".equals(password) && null != password)) {
	            				changeString[0] = java.util.regex.Matcher.quoteReplacement("$");
	            				changeString[1] = java.util.regex.Matcher.quoteReplacement("&");
	            				password = password.replaceAll("&#36;", changeString[0]);
	            				password = password.replaceAll("&#38;", changeString[1]);
	            				rowData.put("PASSWORD_1", SepoaAuthEncryptManager.getEncrypted(password));
	            			}
	            		}
	            		String userType = rowData.get("USER_TYPE");
	            		rowData.put("PW_RESET_FLAG","N");
	            		
	            		if("S".equals(userType)) {
	            			rowData.put("COMPANY_CODE", rowData.get("SELLER_CODE"));
	            		}else {
	            			rowData.put("COMPANY_CODE", rowData.get("COMPANY_CODE"));
	            		}
	            		
	            		String adminProfileCode = CommonUtil.getConfig("sepoa.system.manager.profile.code", "MUP2011020001");
	            		if(adminProfileCode.equalsIgnoreCase(rowData.get("MENU_PROFILE_CODE"))) {
	            			rowData.put("WORK_TYPE", "Z");
	            		} else {
	            			rowData.put("WORK_TYPE", "G");
	            		}
	            		
	            		//엑셀데이터 업로드시 예외처리가 발생할경우 RESULT에 에러메세지 세팅됨
						if(!"".equals(tmpData.get("RESULT"))) {
							throw new Exception();
						}
						
	            		sxp = new SepoaXmlParser(this, "doInsertSUSMT");
	            		ssm = new SepoaSQLManager(info.getSession("ID"), this, newCtx, sxp);
	            		ssm.doUpdate(rowData);
	            		
	            		((SepoaTransactionalResource) newCtx).getUserTransaction().commit();
	            		tmpData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105006"));
            		
            	} catch (Exception e) {
            		tmpData.put("RESULT", Cache.text.get("POA_CONTRACT.STATUS_105009") + Cache.text.get("POA_CONTRACT.STATUS_200044"));
            		Logger.err.println("### USER_ID: [" + rowData.get("USER_ID") + "] Insert Fail! ### ", e);
					((SepoaTransactionalResource) newCtx).getUserTransaction().rollback();
            	} finally {
            		resultExcelDataList.add(tmpData);
            	}
            }
            	
			Map<String, String> returnMap = new HashMap<String, String>();
			returnMap.put("returnType", "param");
			returnMap.put("result", new Gson().toJson(resultExcelDataList));
			setObject(returnMap);
            
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
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
}