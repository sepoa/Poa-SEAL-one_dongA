package sepoa.svc.login.cbo_019;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;

import filters.ThreadLocalFilter;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
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
			
			out = ServiceConnector.doService(info, "sepoa.svc.system.login.SepoaLoginService", "CONNECTION",
					"forceLogin", new Object[] { headerData });
			
			/*
			Map<String, String> rawHeaderData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
	
			HttpServletRequest request = ThreadLocalFilter.tl.get().request;
			SepoaSession.putValue(request, "ssoToken", rawHeaderData.get("ssoToken"));
			SepoaSession.putValue(request, "domainName", rawHeaderData.get("domainName"));
			Logger.debug.println("ssoToken setted : " + rawHeaderData.get("ssoToken"));
			Logger.debug.println("domainName setted : " + rawHeaderData.get("domainName"));
			

			System.out.println(out.flag);
			System.out.println(out.message);
			*/

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
