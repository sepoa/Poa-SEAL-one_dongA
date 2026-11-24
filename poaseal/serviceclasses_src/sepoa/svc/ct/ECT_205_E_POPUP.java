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

@Description("임직원 계약 계약자 지정 팝업")
public class ECT_205_E_POPUP extends SepoaService {

	public ECT_205_E_POPUP(String opt, SepoaInfo info) throws SepoaServiceException {
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
			headerData.put("BIZ_CATEGORY", "C");
			sxp = new SepoaXmlParser(this, "doQueryCompany");
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
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			if(!headerData.containsKey("F_KEYWORD")) {
				headerData.put("F_KEYWORD", "__EMPTY__KEYWORD__");
			}
			if(headerData.containsKey("SIGNER_COMPANY_CODE")) {
				headerData.put("BIZ_CODE", MapUtils.getString(headerData, "SIGNER_COMPANY_CODE"));
			}
			
			if(headerData.containsKey("SINGLE_SIGN")) { // 비정형서식일 경우 부서정보를 포함한 조회
				sxp = new SepoaXmlParser(this, "doQuerySingleSign"); 
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
			}else {
				sxp = new SepoaXmlParser(this, "doQuerySigner"); // 정형서식(템플릿)일경우
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
			}
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
