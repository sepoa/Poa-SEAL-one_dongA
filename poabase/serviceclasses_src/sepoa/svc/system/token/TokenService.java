package sepoa.svc.system.token;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;

import platform.fw.module.token.TokenManager;
import platform.fw.servlet.SepoaAPI;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.CommonUtil;

@Description("토큰 요청")
public class TokenService extends SepoaService{
	public TokenService(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
	
	@SuppressWarnings("unchecked")
	@Description("확인")
	public SepoaOut getToken(Map<String, Object> requestData) {
		setStatus(1);
		setFlag(true);
		
		try {
			requestData = (Map<String, Object>) requestData.get("data");
			this.setValue(TokenManager.getBuilder()
					.setDisposable(MapUtils.getBoolean(requestData, "disposable", false))
					.setAutorenewal(MapUtils.getBoolean(requestData, "autoRenewal", false))
					.setUnitTime(MapUtils.getLong(requestData, "unitTime", Long.parseLong(CommonUtil.getConfig("sepoa.token.inactive", "20"))))
					.putAll((requestData.get("data") instanceof Map)?(Map<String, Object>) requestData.get("data"):new SingletonMap<String, Object>())
					.build());
			this.addParams(SepoaAPI.KEY_RETURN_TYPE, SepoaAPI.RETURN_TYPE_ENC);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			setStatus(0);
			setFlag(false);
		}
		return getSepoaOut();
	}
}
