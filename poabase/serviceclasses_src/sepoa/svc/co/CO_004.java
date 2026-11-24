package sepoa.svc.co;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.cfg.ConfigurationException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.ParamSql;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.SepoaWorkingLogger;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.AES256Cipher;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;

@Description("웹계정")
public class CO_004 extends SepoaService {
    public CO_004(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    // all method drop
    
//    @Description("로그인")
//    public SepoaOut setLoginMap(Map map) throws Exception {
//        SepoaOut out = null;
//        try {
//            Map<String, String> paramMap = (Map<String, String>) map.get("headerData");
//            String houseCode = MapUtils.getString(paramMap, "house_code");
//            if(StringUtils.isEmpty(houseCode)) {
//            	paramMap.put("house_code", "000");
//            }
//            out = setLogin(paramMap);
//            if(out.result[0] == null) {
//            	out.status = 3;
//            	return out;
//            }
//            SepoaFormater sf = new SepoaFormater(out.result[0]);
//            if ("Z".equals(sf.getValue("WORK_TYPE",0)) 
//            		&& "true".equals(CommonUtil.getConfig("sepoa.admin.otpFlag", "false")) 
//            		&& !"Y".equals(MapUtils.getString(paramMap, "OTPCONFIRM", ""))) {//관리자 로그인이면서 OTP시스템을 이용하는 경우, OTP인증전
//        		out.status = 4;
//        		Map<String, String> returnMap = new HashMap<String, String>();
//        		returnMap.put("returnType", "param");
//        		returnMap.put("MOBILE_NO", sf.getValue("MOBILE_NO", 0));
//        		returnMap.put("EMAIL", sf.getValue("EMAIL", 0));
//        		returnMap.put("NAME", sf.getValue("NAME_LOC", 0));
//        		returnMap.put("ID", sf.getValue("ID", 0));
//        		out.obj = returnMap;
//        		return out;
//            }else if("Y".equals(sf.getValue("PW_RESET_FLAG",0)) && "true".equals(CommonUtil.getConfig("sepoa.admin.ResetFlag", "false"))) {
//            	out.status = 5;
//            	Map<String, String> returnMap = new HashMap<String, String>();
//            	returnMap.put("returnType", "param");
//        		returnMap.put("ID", sf.getValue("ID", 0));
//        		out.obj = returnMap;
//            	return out;
//            }
//
//            HttpServletRequest request = (HttpServletRequest) map.get(SepoaDataMapper.KEY_REQUEST);
//            
//
//            if (sf.getRowCount() > 0) {
//                Config sepoa_login_process_con = new Configuration();
//                
//                if(sf.getDouble("pass_check_cnt", 0) > sepoa_login_process_con.getInt("sepoa.user.password.check_cnt", 0)){
//                    out.status = 3;
//                }else{
//                    out.status = 2;
//                    paramMap.put("pass_check_cnt", "0");
//                    SepoaXmlParser sxp = new SepoaXmlParser(this, "updatePassCheckCnt");
//                    SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
//                    ssm.doUpdate(paramMap);
//                    Commit();
//                    
//                    if("S".equals(sf.getValue("USER_TYPE", 0)) && sf.getValue("ID", 0).charAt(0) == 'W'){
//                    	SepoaSession.putValue(request, "BUYER_COMPANY_CODE", "2000");
//                    }else{
//                    	SepoaSession.putValue(request, "BUYER_COMPANY_CODE", sf.getValue("BUYER_COMPANY_CODE", 0));
//                    }
//                    SepoaSession.putValue(request, "TAX_BILLS_ISSUED_TYPE", sf.getValue("TAX_BILLS_ISSUED_TYPE", 0));
//   
//                    SepoaSession.putValue(request, "COMPANY_CODE", sf.getValue("COMPANY_CODE", 0));
//                    SepoaSession.putValue(request, "DEPARTMENT", sf.getValue("DEPARTMENT", 0));
//                    SepoaSession.putValue(request, "NAME_LOC", sf.getValue("NAME_LOC", 0));
//                    SepoaSession.putValue(request, "NAME_ENG", sf.getValue("NAME_ENG", 0));
//                    SepoaSession.putValue(request, "PLANT_CODE", sf.getValue("PLANT_CODE", 0));
//                    SepoaSession.putValue(request, "PLANT_NAME", sf.getValue("PLANT_NAME_LOC", 0));
//                    SepoaSession.putValue(request, "LOCATION_CODE", sf.getValue("LOCATION_CODE", 0));
//                    SepoaSession.putValue(request, "DEPARTMENT_NAME_LOC", sf.getValue("DEPARTMENT_NAME_LOC", 0));
//                    SepoaSession.putValue(request, "DEPARTMENT_NAME_ENG", sf.getValue("DEPARTMENT_NAME_ENG", 0));
//                    SepoaSession.putValue(request, "LANGUAGE", sf.getValue("LANGUAGE", 0));
//                    SepoaSession.putValue(request, "COUNTRY", sf.getValue("COUNTRY", 0));
//                    SepoaSession.putValue(request, "EMPLOYEE_NO", sf.getValue("EMPLOYEE_NO", 0));
//                    SepoaSession.putValue(request, "TEL", sf.getValue("TEL", 0));
//                    SepoaSession.putValue(request, "MOBILE_NO", sf.getValue("MOBILE_NO", 0));
//                    SepoaSession.putValue(request, "ID", sf.getValue("ID", 0));
//                    SepoaSession.putValue(request, "POSITION", sf.getValue("POSITION", 0));
//                    SepoaSession.putValue(request, "CITY", sf.getValue("CITY", 0));
//                    SepoaSession.putValue(request, "LOCATION_NAME", sf.getValue("LOCATION_NAME", 0));
//                    SepoaSession.putValue(request, "EMAIL", sf.getValue("EMAIL", 0));
//                    SepoaSession.putValue(request, "CTRL_CODE", sf.getValue("CTRL_CODE", 0));
//                    SepoaSession.putValue(request, "OWN_CTRL_CODE_LIST", sf.getValue("OWN_CTRL_CODE_LIST", 0));
//                    SepoaSession.putValue(request, "USER_TYPE", sf.getValue("USER_TYPE", 0));
//                    SepoaSession.putValue(request, "IS_ADMIN_USER", sf.getValue("IS_ADMIN_USER", 0));
//                    SepoaSession.putValue(request, "MENU_PROFILE_CODE", sf.getValue("MENU_PROFILE_CODE", 0));
//                    SepoaSession.putValue(request, "USER_IP", request.getRemoteAddr());
//                    SepoaSession.putValue(request, "HOUSE_CODE", "100");
//                    SepoaSession.putValue(request, "USER_OS_LANGUAGE", paramMap.get("language"));
//                    SepoaSession.putValue(request, "COMPANY_NAME", sf.getValue("COMPANY_NAME", 0));
//                    SepoaSession.putValue(request, "QM_CTRL_CODE", sf.getValue("QM_CTRL_CODE", 0));
//                    SepoaSession.putValue(request, "MM_CTRL_CODE", sf.getValue("MM_CTRL_CODE", 0));
//                    SepoaSession.putValue(request, "SHIPPER_TYPE", sf.getValue("SHIPPER_TYPE", 0));
//                    SepoaSession.putValue(request, "CTRL_TYPE_CODE_LIST", sf.getValue("CTRL_TYPE_CODE_LIST", 0));
//                    SepoaSession.putValue(request, "SELLER_PP_TYPE", sf.getValue("SELLER_PP_TYPE", 0));
//                    SepoaSession.putValue(request, "AFFILIATED_COMPANY_FLAG", sf.getValue("AFFILIATED_COMPANY_FLAG", 0));
//                    SepoaSession.putValue(request, "HOUSE_CODE", sf.getValue("HOUSE_CODE", 0));
//                    SepoaSession.putValue(request, "THEME", sf.getValue("THEME", 0));
//                    SepoaSession.putValue(request, "DATE_FORMAT", sf.getValue("DATE_FORMAT", 0));
//                    SepoaSession.putValue(request, "PUBLIC_EMAIL", sf.getValue("PUBLIC_EMAIL", 0));
//                    SepoaSession.putValue(request, "WORK_TYPE", sf.getValue("WORK_TYPE", 0));
//                    SepoaSession.putValue(request, "PROFILE_ATTACHE_NO", sf.getValue("PROFILE_ATTACHE_NO", 0));
//                    SepoaSession.putValue(request, "MANAGER_POSITION", sf.getValue("MANAGER_POSITION", 0));
//                    
//                    SepoaWorkingLogger.println(SepoaSession.getAllValue(request), "CO_004", "LOGIN", "LOGIN", "LI");
//                }
//            }else{
//                SepoaXmlParser sxp = new SepoaXmlParser(this, "updatePassCheckCnt");
//                SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
//                ssm.doUpdate(paramMap);
//                Commit();
//                out.status = 0;
//            }
//            out.result = null;//json 데이터에 가끔씩 parsing 오류가 생기는 것 같다.
//        } catch (Exception e) {
//            Logger.err.println(info.getSession("ID"), this, e);
//        }
//        return out;
//    }
//
//    @Description("로그인SSO")
//    public SepoaOut setLoginSSO(Map map) throws Exception {
//        SepoaOut out = null;
//        try {
//            Map<String, String> paramMap = (Map<String, String>) map.get("headerData");
//
//            String key = CommonUtil.getConfig("sepoa.sso.key");
//            String p_userid = MapUtils.getString(paramMap, "user_id");
//            p_userid = StringEscapeUtils.unescapeHtml(p_userid);
//
//            p_userid = AES256Cipher.AES_Decode(p_userid, key);
//            paramMap.put("user_id", p_userid);
//            paramMap.put("force_login_flag", "true");
//
//            out = setLogin(paramMap);
//            SepoaFormater sf = new SepoaFormater(out.result[0]);
//
//            HttpServletRequest request = (HttpServletRequest) map.get(SepoaDataMapper.KEY_REQUEST);
//
//            if (sf.getRowCount() > 0) {
//                Config sepoa_login_process_con = new Configuration();
//                
//                if(sf.getDouble("pass_check_cnt", 0) > sepoa_login_process_con.getInt("sepoa.user.password.check_cnt")){
//                    out.status = 3;
//                }else{
//                    out.status = 2;
//                    paramMap.put("pass_check_cnt", "0");
//                    SepoaXmlParser sxp = new SepoaXmlParser(this, "updatePassCheckCnt");
//                    SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
//                    ssm.doUpdate(paramMap);
//                    Commit();
//                    
//                    String buyer_compnay_code = sepoa_login_process_con.getString("sepoa.buyer.company.code");//BUYER Company Code 추가 2012.07.25
//                    if("S".equals(sf.getValue("USER_TYPE", 0)) && sf.getValue("ID", 0).charAt(0) == 'W'){
//                    	SepoaSession.putValue(request, "BUYER_COMPANY_CODE", "2000");
//                    }else{
//                    	SepoaSession.putValue(request, "BUYER_COMPANY_CODE", sf.getValue("BUYER_COMPANY_CODE", 0));
//                    }
//                    SepoaSession.putValue(request, "TAX_BILLS_ISSUED_TYPE", sf.getValue("TAX_BILLS_ISSUED_TYPE", 0));
//   
//                    SepoaSession.putValue(request, "COMPANY_CODE", sf.getValue("COMPANY_CODE", 0));
//                    SepoaSession.putValue(request, "DEPARTMENT", sf.getValue("DEPARTMENT", 0));
//                    SepoaSession.putValue(request, "NAME_LOC", sf.getValue("NAME_LOC", 0));
//                    SepoaSession.putValue(request, "NAME_ENG", sf.getValue("NAME_ENG", 0));
//                    SepoaSession.putValue(request, "PLANT_CODE", sf.getValue("PLANT_CODE", 0));
//                    SepoaSession.putValue(request, "PLANT_NAME", sf.getValue("PLANT_NAME_LOC", 0));
//                    SepoaSession.putValue(request, "LOCATION_CODE", sf.getValue("LOCATION_CODE", 0));
//                    SepoaSession.putValue(request, "DEPARTMENT_NAME_LOC", sf.getValue("DEPARTMENT_NAME_LOC", 0));
//                    SepoaSession.putValue(request, "DEPARTMENT_NAME_ENG", sf.getValue("DEPARTMENT_NAME_ENG", 0));
//                    SepoaSession.putValue(request, "LANGUAGE", sf.getValue("LANGUAGE", 0));
//                    SepoaSession.putValue(request, "COUNTRY", sf.getValue("COUNTRY", 0));
//                    SepoaSession.putValue(request, "EMPLOYEE_NO", sf.getValue("EMPLOYEE_NO", 0));
//                    SepoaSession.putValue(request, "TEL", sf.getValue("TEL", 0));
//                    SepoaSession.putValue(request, "MOBILE_NO", sf.getValue("MOBILE_NO", 0));
//                    SepoaSession.putValue(request, "ID", sf.getValue("ID", 0));
//                    SepoaSession.putValue(request, "POSITION", sf.getValue("POSITION", 0));
//                    SepoaSession.putValue(request, "CITY", sf.getValue("CITY", 0));
//                    SepoaSession.putValue(request, "LOCATION_NAME", sf.getValue("LOCATION_NAME", 0));
//                    SepoaSession.putValue(request, "EMAIL", sf.getValue("EMAIL", 0));
//                    SepoaSession.putValue(request, "CTRL_CODE", sf.getValue("CTRL_CODE", 0));
//                    SepoaSession.putValue(request, "OWN_CTRL_CODE_LIST", sf.getValue("OWN_CTRL_CODE_LIST", 0));
//                    SepoaSession.putValue(request, "USER_TYPE", sf.getValue("USER_TYPE", 0));
//                    SepoaSession.putValue(request, "IS_ADMIN_USER", sf.getValue("IS_ADMIN_USER", 0));
//                    SepoaSession.putValue(request, "MENU_PROFILE_CODE", sf.getValue("MENU_PROFILE_CODE", 0));
//                    SepoaSession.putValue(request, "USER_IP", request.getRemoteAddr());
//                    SepoaSession.putValue(request, "HOUSE_CODE", "100");
//                    SepoaSession.putValue(request, "USER_OS_LANGUAGE", paramMap.get("language"));
//                    SepoaSession.putValue(request, "COMPANY_NAME", sf.getValue("COMPANY_NAME", 0));
//                    SepoaSession.putValue(request, "QM_CTRL_CODE", sf.getValue("QM_CTRL_CODE", 0));
//                    SepoaSession.putValue(request, "MM_CTRL_CODE", sf.getValue("MM_CTRL_CODE", 0));
//                    SepoaSession.putValue(request, "SHIPPER_TYPE", sf.getValue("SHIPPER_TYPE", 0));
//                    SepoaSession.putValue(request, "CTRL_TYPE_CODE_LIST", sf.getValue("CTRL_TYPE_CODE_LIST", 0));
//                    SepoaSession.putValue(request, "SELLER_PP_TYPE", sf.getValue("SELLER_PP_TYPE", 0));
//                    SepoaSession.putValue(request, "AFFILIATED_COMPANY_FLAG", sf.getValue("AFFILIATED_COMPANY_FLAG", 0));
//                    SepoaSession.putValue(request, "HOUSE_CODE", sf.getValue("HOUSE_CODE", 0));
//                    SepoaSession.putValue(request, "THEME", sf.getValue("THEME", 0));
//                    SepoaSession.putValue(request, "DATE_FORMAT", sf.getValue("DATE_FORMAT", 0));
//                    SepoaSession.putValue(request, "PUBLIC_EMAIL", sf.getValue("PUBLIC_EMAIL", 0));
//                    SepoaSession.putValue(request, "WORK_TYPE", sf.getValue("WORK_TYPE", 0));
//                    
//                    SepoaWorkingLogger.println(SepoaSession.getAllValue(request), "CO_004", "LOGIN", "LOGIN", "LI");
//                }
//            }else{
//                SepoaXmlParser sxp = new SepoaXmlParser(this, "updatePassCheckCnt");
//                SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, this.getConnectionContext(), sxp);
//                ssm.doUpdate(paramMap);
//                Commit();
//                out.status = 0;
//            }
//            
//            out.result = null;//json 데이터에 가끔씩 parsing 오류가 생기는 것 같다.
//        } catch (Exception e) {
//            Logger.err.println(info.getSession("ID"), this, e);
//        }
//        return out;
//    }
//
//    @Description("로그인")
//    public SepoaOut setLogin(Map<String, String> map) {
//
//        try {
//            String[] rtn = (String[]) null;
//            setStatus(1);
//            setFlag(true);
//            rtn = svcSetLogin(map);
//
//            if (rtn[1] != null) {
//                setMessage(rtn[1]);
//                Logger.debug.println(info.getSession("ID"), this,
//                        (new StringBuilder("_________ ")).append(rtn[1]).toString());
//                setStatus(0);
//                setFlag(false);
//            }
//
//            setValue(rtn[0]);
//        } catch (Exception e) {
//            setStatus(0);
//            setFlag(false);
//            setMessage(e.getMessage());
//            Logger.err.println(info.getSession("ID"), this, e);
//        }
//
//        return getSepoaOut();
//    }
//
//    private String[] svcSetLogin(Map<String, String> map) throws Exception {
//        String[] rtn = new String[2];
//        sepoa.fw.db.ConnectionContext ctx = getConnectionContext();
//
//        try {
//            boolean ldapLogin = false;
//            boolean configLdapLogin = false;
//            try {
//                configLdapLogin = "true".equals(CommonUtil.getConfig("login.ad.auth", "false"));
//            } catch (Exception e) {
//            	Logger.err.println(info.getSession("ID"), this, e);
//            }
//            String password = MapUtils.getString(map, "password", "");
//			String changeString[] = { "$", "&" };
//
//			changeString[0] = java.util.regex.Matcher.quoteReplacement("$");
//			changeString[1] = java.util.regex.Matcher.quoteReplacement("&");
//			password = password.replaceAll("&#36;", changeString[0]);
//			password = password.replaceAll("&#38;", changeString[1]);
//            
//            
//            if (configLdapLogin) {
//                String id = MapUtils.getString(map, "user_id", "");
//
//                ldapLogin = ldapLogin(id, password);
//            }
//
//            Map<String, String> addMap = new HashMap<String, String>();
//            String houseCode = map.get("house_code");
//            if (houseCode != null && houseCode.length() > 0) {
//                addMap.put("house_code", houseCode);
//            }
//            
//            addMap.put("password", SepoaAuthEncryptManager.getEncrypted(password));
//            // 테스트
//            //if ((map.get("user_id") + "1234").equals(password)) {
//            //    addMap.put("force_login_flag", "true");
//            //}
//
//            SepoaXmlParser sxp = new SepoaXmlParser();
//            SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
//
//            SepoaFormater wff1 = new SepoaFormater(ssm.doSelect(map, addMap));
//            String PW_RESET_FLAG = "";
//            String Pw_reset_cnt = "";
//            if (wff1.getRowCount() > 0) {
//                PW_RESET_FLAG = wff1.getValue("PW_RESET_FLAG", 0);
//                Pw_reset_cnt = wff1.getValue("PASS_CHECK_CNT", 0);
//            }
//
//            sxp = new SepoaXmlParser(this, "svcSetLogin_SELECT_userType");
//            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
//
//            SepoaFormater wff = new SepoaFormater(ssm.doSelect(map, addMap));
//
//            String USER_TYPE = "";
//            if (wff.getRowCount() > 0) {
//                USER_TYPE = wff.getValue("USER_TYPE", 0);
//
//                if (PW_RESET_FLAG.equals("Y") && !Pw_reset_cnt.equals("0")) {
//                    //                	byte[] b64_enc =JetsUtil.encodeBase64(_password.getBytes());
//                    //                	_password = new String(b64_enc);
//                }
//
//                //sxp = new SepoaXmlParser(this, "svcSetLogin_UPDATE_language");
//                //ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
//
//                //ssm.doUpdate(map, addMap);
//                
//                //임시 비밀번호를 사용할 경우
//                String tempPWUseFlag = CommonUtil.getConfig("sepoa.tempPW.use.flag");
//                if("true".equals(tempPWUseFlag)) {
//                	sxp = new SepoaXmlParser(this, "doSelectTempPwTimestamp");
//                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
//                    SepoaFormater sf1 = new SepoaFormater(ssm.doSelect(map, addMap));
//                    
//                	String tempPwTime = sf1.getValue("TEMP_PW_TIMESTAMP", 0);
//                	String now = SepoaDate.getShortDateString()+SepoaDate.getShortTimeString();
//                	
//                	SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
//                	Date reqDate = df.parse(tempPwTime);
//                	Date curDate = df.parse(now);
//                	
//                	Long hour = (curDate.getTime() - reqDate.getTime()) / 3600000;
//                	if(hour < 12) {//12시간미만일경우
//                		addMap.put("tempPwFlag", "true");
//                	}
//                }
//                
//                sxp = new SepoaXmlParser(this, "svcSetLogin_Login");
//                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
//
//                if (ldapLogin) {
//                    addMap.put("force_login_flag", "true");
//                }
//
//                rtn[0] = ssm.doSelect(map, addMap);
//                Commit();
//            }
//        } catch (Exception e) {
//            Rollback();
//            rtn[1] = e.getMessage();
//            Logger.err.println(info.getSession("ID"), this, e);
//        } finally {
//            ctx = null;
//        }
//        return rtn;
//
//    }
//
//    @Description("로그아웃")
//    public SepoaOut setLogoutProcess(SepoaInfo _info) {
//        try {
//            String[] rtn = (String[]) null;
//            setStatus(1);
//            setFlag(true);
//            rtn = svcSetLogoutProcess(_info);
//
//            if (rtn[1] != null) {
//                setMessage(rtn[1]);
//                Logger.debug.println(info.getSession("ID"), this,
//                        (new StringBuilder("_________ ")).append(rtn[1]).toString());
//                setStatus(0);
//                setFlag(false);
//            }
//
//            setValue(rtn[0]);
//        } catch (Exception e) {
//            setStatus(0);
//            setFlag(false);
//            setMessage(e.getMessage());
//            Logger.err.println(info.getSession("ID"), this, e);
//        }
//
//        return getSepoaOut();
//    }
//
//    @Description("로그아웃")
//    private String[] svcSetLogoutProcess(SepoaInfo _info) throws Exception {
//        String[] rtn = new String[2];
//        try {
//            SepoaWorkingLogger.println(_info, "CO_004", "LOGOUT", "LOGOUT", "LO");
//        } catch (Exception e) {
//            Rollback();
//            rtn[1] = e.getMessage();
//            e.printStackTrace();
//            Logger.err.println(info.getSession("ID"), this, e);
//        }
//
//        return rtn;
//    }
//    
//    private boolean ldapLogin(String id, String password) throws ConfigurationException {
//        boolean res = false;
//
//        LdapContext ldapContext = null;
//
//        Config config = new Configuration();
//
//        try {
//            String url = "ldap://" + config.get("active.directory.ldap.ip") + ":"
//                    + config.get("active.directory.ldap.port");
//
//            Hashtable<String, String> ldapEnv = new Hashtable<String, String>(11);
//            ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
//            ldapEnv.put(Context.PROVIDER_URL, url);
//            ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
//
//            ldapEnv.put(Context.SECURITY_PRINCIPAL, id.toLowerCase() + "@donghee.co.kr");
//            ldapEnv.put(Context.SECURITY_CREDENTIALS, password);
//            ldapEnv.put(Context.REFERRAL, "follow");
//
//            ldapContext = new InitialLdapContext(ldapEnv, null);
//
//            res = true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            Logger.err.println("Login failed... : ");
//            Logger.err.println(info.getSession("ID"), this, e);
//        } finally {
//            try {
//                if (ldapContext != null) {
//                    ldapContext.close();
//                }
//            } catch (Exception e) {
//            	Logger.err.println(info.getSession("ID"), this, e);
//            }
//        }
//        return res;
//    }
//    
//    @Description("사용자변경")
//    public SepoaOut changeUserLogon(Map<String, Object> allData){
//        setFlag(true);
//        setStatus(1);
//        try{
//        	Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
//        	
//            if("".equals(MapUtils.getString(headerData, "loginMode","")) && !Boolean.parseBoolean(info.getSession("IS_ADMIN_USER"))){
//                throw new Exception("not have permission");
//            }
//            
//            HttpServletRequest request = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
//            
//            /* logout 처리 */
//            Object[] obj = {info};
//            SepoaOut wo = ServiceConnector.doService(info, "sepoa.svc.co.CO_004", "TRANSACTION","setLogoutProcess", obj);
//            if(! wo.flag){
//               throw new Exception(SepoaString.replace(SepoaString.replace(wo.message, "\r", ""), "\n", ""));
//            }
//                
//            SepoaSession.invalidate(request);
//            Object[] obj2 = {allData};
//            wo = ServiceConnector.doService(info, "sepoa.svc.co.CO_004", "CONNECTION","setLoginMap", obj2);
//            if(!(wo.status == 2 || wo.status == 3)){
//                throw new Exception("Wrong ID or Password.");
//            }
//        }catch(Exception e){
//            setFlag(false);
//            setStatus(0);
//            setMessage(e.getMessage());
//            Logger.err.println(info.getSession("ID"), this, e);
//        }
//        return getSepoaOut();
//    }
//    
//    //신규 사용자 비밀번호 변경
//    @SuppressWarnings("unchecked")
//    public SepoaOut changePW(Map<String, Object> allData) {
//    	ConnectionContext ctx = getConnectionContext();
//    	SepoaXmlParser sxp = null;
//    	SepoaSQLManager ssm = null;
//    	try {
//    		setFlag(true);
//    		setStatus(1);
//    		Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
//    		headerData.put("HOUSE_CODE", MapUtils.getString(headerData, "HOUSE_CODE","000"));
//    		headerData.put("NEW_PASSWORD", SepoaAuthEncryptManager.getEncrypted(MapUtils.getString(headerData,"NEW_PASSWORD_1","")));
//    		sxp = new SepoaXmlParser(this, "doUpdatePW_SUSMT");
//    		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
//    		ssm.doUpdate(headerData);
//    		Commit();
//    	} catch (Exception e) {
//    		Rollback();
//    		e.printStackTrace();
//    		setFlag(false);
//    		setStatus(0);
//    		setMessage(e.getMessage());
//    		Logger.err.println(info.getSession("ID"), this, e);
//    	}
//    	return getSepoaOut();
//    }
}
