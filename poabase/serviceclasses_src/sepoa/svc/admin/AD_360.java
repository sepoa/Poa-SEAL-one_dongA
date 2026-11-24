package sepoa.svc.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaFormater;

@Description("index")
public class AD_360 extends SepoaService {

	public AD_360(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("셋팅값 조회")
	public SepoaOut getSettings() {
		Map<String, String> settingsData = new HashMap<String, String>();
		Map<String, String> headerData = new HashMap<String, String>();
		//dummyData.put("sepoa.login.page", CommonUtil.getConfig("sepoa.login.page"));
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		headerData.put("CLIENT_CODE", info.getSession("HOUSE_CODE"));
		headerData.put("COMPANY_CODE", "1000");
		headerData.put("GROUP", "DESIGN");
		
		try {
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			List<Map<String, String>> sfList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			
			for(Map<String, String> item : sfList) {
				String key = item.get("KEY");
				String value = item.get("VALUE");
				
				settingsData.put(key, value);
			}

			setValue(settingsData);
		}catch(Exception e) {
			Logger.err.println(e);
			e.printStackTrace();
		}
		return getSepoaOut(); 
	}
	
	@Description("공지사항 조회")
	public SepoaOut getNotice(Map<String, Object> allData) {
		
		setFlag(true);
		setStatus(1);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = new HashMap<String, String>();
			
			headerData.put("CLIENT_CODE", info.getSession("HOUSE_CODE"));
			
			sxp = new SepoaXmlParser(this, "getNotice");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

			setValue(ssm.doSelect(headerData));
		}catch(Exception e) {
			setFlag(false);
			setStatus(0);
			setMessage(e.getMessage());
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
}
