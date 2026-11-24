package sepoa.svc.ct;

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
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("전자계약 계약자 지정 팝업")
public class CT_205_B_POPUP extends SepoaService {

	public CT_205_B_POPUP(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("회사 조회")
	public SepoaOut doQueryCompany(Map<String, Object> allData) {
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
			if(headerData.containsKey("CATEGORY")) {
				headerData.put("BIZ_CATEGORY", MapUtils.getString(headerData, "CATEGORY"));
			}
			
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			setValue(sf);
			
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		
		return getSepoaOut();
	}
	
	@Description("담당자 조회")
	public SepoaOut doQuerySigner(Map<String, Object> allData) {
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
			
			if(headerData.containsKey("SIGNER_COMPANY_CODE")) {
				headerData.put("BIZ_CODE", MapUtils.getString(headerData, "SIGNER_COMPANY_CODE"));
			}
			
			if("".equals(headerData.get("a"))) {
				sxp = new SepoaXmlParser(this, "doQueryUser");
			} else {
				sxp = new SepoaXmlParser();
			}
			
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			setValue(sf);
			
		} catch(Exception e) {
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
