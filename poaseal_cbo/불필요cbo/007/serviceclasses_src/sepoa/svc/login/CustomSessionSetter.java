package sepoa.svc.login;

import java.util.Map;

import filters.ThreadLocalFilter;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;

public class CustomSessionSetter extends SepoaService{
	protected String[] defaultSessionKey = new String[] {"EMPLOYEE_NO"};
	public CustomSessionSetter(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
	
	public SepoaOut doCreateSessionData(Map<String, String> userInfo, Map<String, String> sessionMap) {
		setStatus(1);
		setFlag(true);
		Logger.info.println("LoginManager", this, "[SECL] add session");
		for(String key : this.defaultSessionKey) {
			sessionMap.put(key, userInfo.get(key));
			Logger.info.println("LoginManager", this, key);
		}
		
		sessionMap.put("USER_IP", ThreadLocalFilter.getClientIP());
		
		Logger.info.println("LoginManager", this, "[SECL] end");
		return getSepoaOut();
	}
	
	public SepoaOut doCheckLoginPermission(Map<String, String> userInfo, Map<String, String> callbackOptions) {
		setStatus(1);
		setFlag(true);
		Logger.info.println("LoginManager", this, "[SECL] check login permission");
		//if not have permission : setFlag(false);
		return getSepoaOut();
	}
}
