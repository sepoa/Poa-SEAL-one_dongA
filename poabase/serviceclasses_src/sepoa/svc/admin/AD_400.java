package sepoa.svc.admin;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;

@Description("시스템관리 [서비스]")
public class AD_400 extends SepoaService {

	public AD_400(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
	@Description("세팅값 조회")
	public SepoaOut initializeConfig(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			
			Map<String, String> headerData = new HashMap<String, String>();
			
			headerData.put("CLIENT_CODE", info.getSession("HOUSE_CODE"));
			headerData.put("COMPANY_CODE", "1000");
			headerData.put("GROUP", "DESIGN");
			
			sxp = new SepoaXmlParser(this, "initializeConfig");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
			
		} catch (Exception e) {
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			System.err.println(e);
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("중복 조회")
	public SepoaOut doCheckDuplicate(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doCheckDuplicate");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
			
		} catch (Exception e) {
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			System.err.println(e);
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
}
