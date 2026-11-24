package sepoa.svc.login;

import java.util.Map;

import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.ott.SepoaOTTManager;

@Description("SSO 로그인")
public class LoginSSO extends SepoaService{
	public LoginSSO(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
	
	@SuppressWarnings("unchecked")
	@Description("key 복호화")
	public SepoaOut decryptSSOKey(Map<String, String> params) {
		setStatus(1);
		setFlag(true);
		
		String key = params.get("ssoKey");
		Object result = SepoaOTTManager.check(key);
		if(result != null) {
			setValue((Map<String, String>) result);
		}else {
			setStatus(0);
			setFlag(false);
		}
		return getSepoaOut();
	}
}
