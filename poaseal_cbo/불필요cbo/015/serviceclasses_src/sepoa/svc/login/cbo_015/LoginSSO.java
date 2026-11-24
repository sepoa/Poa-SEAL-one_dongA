package sepoa.svc.login.cbo_015;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;

@SuppressWarnings({ "unchecked" })
@Description("LoginSSO")
public class LoginSSO extends SepoaService {

	public LoginSSO(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@Description("SSO 로그인")
	public SepoaOut loginSSO(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaOut out = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			Decoder decode = Base64.getDecoder();
			
			String endcodeId = headerData.get("ID");
			String decodeId = new String(decode.decode(endcodeId));
			
			headerData.put("ID", decodeId);
			
			out = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION",
					"forceLogin", new Object[] { headerData });

		} catch (Exception e) {
			// e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return out != null ? out : getSepoaOut();
	}
}
