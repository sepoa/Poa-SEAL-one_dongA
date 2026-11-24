package sepoa.svc.admin;

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

@Description("클라이언트 관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_401 extends SepoaService {

	public AD_401(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
	@SuppressWarnings("unchecked")
	@Description("리스트 조회")
	public SepoaOut doQuery(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			String houseCode = info.getSession("HOUSE_CODE");
			if(!"000".equals(houseCode)) {
				headerData.put("HOUSE_CODE", houseCode);
			}
			
			sxp = new SepoaXmlParser();
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
