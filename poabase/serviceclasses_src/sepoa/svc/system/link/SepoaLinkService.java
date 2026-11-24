package sepoa.svc.system.link;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import filters.ThreadLocalFilter;
import platform.fw.module.token.TokenManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.login.SepoaLoginManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.ses.SepoaSession;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaJSON;
import sepoa.fw.ux.link.SepoaLinkManager;

@Description("링크")
public class SepoaLinkService  extends SepoaService {
    public SepoaLinkService(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    @SuppressWarnings("unchecked")
	@Description("링크 검증")
    public SepoaOut checkLink(Map<String, String> params) {
    	setStatus(1);
    	setFlag(true);
    	String link = params.get("ln");
    	if(StringUtils.isBlank(link)) {
    		setStatus(100001);
    	}else {
    		ConnectionContext ctx = getConnectionContext();
    		Config config = null;
    		try {
    			config = new Configuration();
    		}catch(Exception e) {
    			setStatus(100002);
    		}
    		if(getStatus() == 1) {
    			SepoaLinkManager linkManager = new SepoaLinkManager(info, config, ctx);
    			Map<String, String> linkInfo = linkManager.openLink(link);
    			Map<String, Object> resultMap = new HashMap<String, Object>();
    			
    			if(linkInfo == null) {
    				setStatus(linkManager.getLastErrCode());
    			}else {
    				String docType = linkInfo.get("DOC_TYPE").toLowerCase();
	    			resultMap.put("url", config.get("sepoa.link.page."+docType));
	    			
	    			if(link.equals(linkManager.getLink())) {
	    				String encSSOKey = params.get("ssoKey");
	    				if(StringUtils.isBlank(encSSOKey)) {
	    					if(info != null && StringUtils.isNotBlank(info.getSession("ID"))) {
	        					if(linkManager.setActive(info.getSession("COMPANY_CODE"), info.getSession("ID"))) {
	            					resultMap.put("params", linkManager.getUserParams());
	        					}else {
	        						setStatus(100004);
	        					}
	        				}else {
	        					setStatus(100004);
	        				}
	    				}else {
	    					SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.login.LoginSSO", "CONNECTION", "decryptSSOKey", new Object[] { params });
	    					if(so.status != 1 || !so.flag) {
	    						setStatus(100006);
	    					}else {
	    						Map<String, String> data = (Map<String, String>) so.getData(0);
	    						
	    						SepoaLoginManager loginManager = new SepoaLoginManager(info, config, ctx);
	    						if(!loginManager.initUser(data.get("COMPANY_CODE"), data.get("ID"))) {
	    							setStatus(100008);
	    						}else {
	    							Map<String, String> sessionData = loginManager.getSessionData();
	    							if(!linkManager.setActive(sessionData.get("COMPANY_CODE"), sessionData.get("ID"))) {
	    								setStatus(100004);
	    							}else {
	    								resultMap.put("params", linkManager.getUserParams());
	    								if(config.getBoolean("sepoa.link.page."+docType+".token", true)) {
	    									resultMap.put("token", TokenManager.getBuilder()
	    											.putAll(new HashMap<String, Object>(sessionData))
	    											.setUnitTime(config.getInt("sepoa.token.inactive", 1800))
	    											.setAutorenewal(true)
	    											.build());
		    	    						resultMap.put("params", linkManager.getUserParams());
	    								}else {
	    									HttpServletRequest request = ThreadLocalFilter.getHttpServletRequest();
	    									
	    									SepoaLoginManager.logout(true);
	        			    				Map<String, String> sessionInfo = loginManager.getSessionData();
	        			    				for(Entry<String, String> entry : sessionInfo.entrySet()) {
	        				    				SepoaSession.putValue(request, entry.getKey(), entry.getValue());
	        				    			}
	        			    				this.info = SepoaSession.getAllValue(request);  
	        			    				this.info.setThreadLocal();
	    								}
	            					}
	    						}
	    					}
	    				}
	    			} else {
	    				//private
	    				Map<String, String> userInfo = linkManager.getUserInfo();
	    				SepoaLoginManager manager = new SepoaLoginManager(info, config, ctx);
	    				if(!manager.initUser(userInfo.get("USER_COMPANY_CODE"), userInfo.get("USER_ID"))) {
	    					setStatus(100003);
	    				}
	    				resultMap.put("token", TokenManager.getBuilder()
								.putAll(new HashMap<String, Object>(manager.getSessionData()))
								.setUnitTime(config.getInt("sepoa.token.inactive", 1800))
								.setAutorenewal(true)
								.build());
						resultMap.put("params", linkManager.getUserParams());
	    			}
	    		}
    			if(getStatus() == 1) {
    				setValue(SepoaJSON.toJSONString(resultMap));
    			}
    		}
    	}
    	return getSepoaOut();
    }
}
