package sepoa.svc.cbo.ct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

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

@Description("계약 목록 커스텀 처리 클래스")
public class ContractObjectListService extends SepoaService{
	public ContractObjectListService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("남부발전 계약 완료 데이터 조회 구간")
	public SepoaOut doSelectContractData(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
        
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		try {
			Map<String, String> headerData = (Map<String, String>) data.get("data");
			Map<String, String> condition = new HashMap<>();
			
			condition.putAll(headerData);
			
			condition.put("MODE_TYPE_OPERATOR", "in");
			condition.put("MODE_TYPE_STATUS", "A,X,F");
			condition.put("ORDER_COLUMN", "CONT_NO");
			condition.put("ORDER_SORT"  , "DESC");
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(condition));
			
			if(sf.getRowCount() > 0) {
				List<Map<String, String>> list = sf.getListMap();
				ObjectMapper mapper = new ObjectMapper();
				String str = mapper.writeValueAsString(list);
				
				setValue(str);
			}
			
		} catch (Exception e) {
			if(getStatus() == 1) setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}
