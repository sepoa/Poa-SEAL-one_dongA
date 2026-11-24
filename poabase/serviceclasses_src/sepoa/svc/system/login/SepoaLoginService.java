package sepoa.svc.system.login;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import filters.ThreadLocalFilter;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
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

@Description("로그인")
public class SepoaLoginService  extends SepoaService {
    public SepoaLoginService(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    @SuppressWarnings("unchecked")
    @Description("로그인")
	public SepoaOut login(Map<String, Object> allData) {
    	Map<String, String> paramMap = new HashMap<String, String>();
    	paramMap.put("returnType", "param");
    	
    	setStatus(1);
    	setFlag(true);
    	
    	try {
    		int resultCode = 1;
    		Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
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
	    	    	paramMap.put("userId", id);
	    	    	
	    			id = id.toUpperCase();
		    		String password = headerData.get("PASSWORD");
		    		SepoaLoginManager manager = new SepoaLoginManager(info, config, getConnectionContext());
		    		if(!manager.initUser(id) || !manager.checkPassword(password) || !manager.checkLoginPermission()) {
		    			resultCode = manager.getLastErrCode();
		    			if(resultCode == 100016) {
		    				paramMap.putAll(manager.getLoginFailCallback());
		    			}
		    		}else {
		    			HttpServletRequest request = ThreadLocalFilter.getHttpServletRequest();
		    			if(request == null){
		    				resultCode  = 100007;
		    			}else {
		    				Map<String, String> sessionInfo = manager.getSessionData();
		    				for(Entry<String, String> entry : sessionInfo.entrySet()) {
			    				SepoaSession.putValue(request, entry.getKey(), entry.getValue());
			    			}
		    				SepoaWorkingLogger.println(SepoaSession.getAllValue(request), "SepoaLoginService", "LOGIN", "LOGIN", "LI");
		    			}
		    		}
		    	}
	    	}
	    	if(resultCode != 1) {
	    		setStatus(resultCode);
	    		setFlag(false);
	    	}
	    	
	    	setObject(paramMap);
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
    	
    	try {
    		int resultCode = 1;
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
		    				for(Entry<String, String> entry : sessionInfo.entrySet()) {
			    				SepoaSession.putValue(request, entry.getKey(), entry.getValue());
			    			}
		    				SepoaWorkingLogger.println(SepoaSession.getAllValue(request), "SepoaLoginService", "LOGIN", "LOGIN", "LI");
		    			}
		    		}
		    	}
	    	}
	    	if(resultCode != 1) {
	    		setStatus(resultCode);
	    		setFlag(false);
	    	}
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
    		SepoaLoginManager manager = new SepoaLoginManager(info, config, getConnectionContext());
    		if(!manager.initUser(userId)) {
    			setStatus(manager.getLastErrCode());
    		} else {
    			boolean clearYn = manager.clearPassword(newPwd, userEmail);
    			if(!clearYn) {
    				setStatus(0);
    			} else {
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
}
