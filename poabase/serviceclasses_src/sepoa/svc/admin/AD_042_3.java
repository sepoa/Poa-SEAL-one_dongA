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

@Description("폼 필터 관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_042_3 extends SepoaService {

	public AD_042_3(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
	@Description("필터 폼 조회")
	public SepoaOut doQueryFilterData(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, String> res = new HashMap<String, String>();
			
			res.put("returnType", "param");
			
			sxp = new SepoaXmlParser(this, "doQueryMasterData");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			res.put("MASTER", ssm.doSelect(headerData));

			sxp = new SepoaXmlParser(this, "doQueryFilterData");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			res.put("FILTER", ssm.doSelect(headerData));
			
			res.put("HOUSE_CODE", headerData.get("HOUSE_CODE"));
			
			setObject(res);
			
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
}
