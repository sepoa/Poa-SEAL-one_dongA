package sepoa.svc.ct;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("계약대상")
public class CT_201 extends SepoaService {
	
	public CT_201(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@SuppressWarnings("unchecked")
	@Description("계약 대상 테이블 조회")
	public SepoaOut doQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if(!headerData.containsKey("F_KEYWORD")) {
				headerData.put("F_KEYWORD", "__EMPTY__KEYWORD__");
			}
			
			Config config = new Configuration();
			Map<String, String> condition = new HashMap<>();
			String targetUseNumber = StringUtils.join(config.get("sepoa.req.seal.contract.use.number").split("[;,]"), ",");
			condition.put("REQ_SEAL_USE_TYPE", targetUseNumber);
			condition.put("SMR_STATUS", "C");		// 날인 완료 상태
			condition.put("RE_FLAG", "E");			// 전자 날인 여부
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData, condition));
			SimpleDateFormat input = new SimpleDateFormat("yyyyMMdd HHmmss");
			SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			List<Map<String, String>> result = sf.getListMap();
			for(Map<String, String> row : result) {
				String addDate = row.get("ADD_DATE");
				String addTime = row.get("ADD_TIME");
				
				row.put("ADD_DATE_TIME", output.format(input.parse(addDate + " " + addTime)));
			}
			
			setValue(result);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
}
