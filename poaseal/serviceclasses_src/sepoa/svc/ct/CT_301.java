package sepoa.svc.ct;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

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
import sepoa.fw.util.SepoaFormater;

@Description("전자계약 - 거래처 관리 - List")
public class CT_301 extends SepoaService {
	
	public CT_301(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@SuppressWarnings("unchecked")
	@Description("거래처 조회")
	public SepoaOut doQuerySBUPR(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if(StringUtils.isBlank(headerData.get("F_KEYWORD"))) {
				headerData.put("F_KEYWORD", "__EMPTY__KEYWORD__");
			}
			
			if(!StringUtils.isBlank(headerData.get("F_STATUS"))) {
				headerData.put("BIZ_TYPE", headerData.get("F_STATUS"));
			}
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
//			SimpleDateFormat inputFormat = new SimpleDateFormat("yyyymmdd");
//			SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy/mm/dd");
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> result = sf.getListMap();
			for(Map<String, String> data : result) {
//				String date = data.get("FOUNDATION_DATE");
//				if(!StringUtils.isBlank(date)) {
//					data.put("FOUNDATION_DATE", outputFormat.format(inputFormat.parse(date)));
//				}
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
	
	@SuppressWarnings("unchecked")
	@Description("거래처 담당자 조회")
	public SepoaOut doQuerySBUPI(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if(StringUtils.isBlank(headerData.get("F_KEYWORD"))) {
				headerData.put("F_KEYWORD", "__EMPTY__KEYWORD__");
			}
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			List<Map<String, String>> result = sf.getListMap();
			
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
