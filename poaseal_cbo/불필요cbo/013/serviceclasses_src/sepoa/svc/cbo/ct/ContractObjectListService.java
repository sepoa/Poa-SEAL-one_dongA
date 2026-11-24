package sepoa.svc.cbo.ct;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.google.gson.Gson;

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
import sepoa.sealmk2.SepoaReqSealManager;

@Description("계약대상현황 커스텀 처리 클래스")
public class ContractObjectListService extends SepoaService{
	public ContractObjectListService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("포스코플로우 날인요청 Extra 데이터 조회 구간")
	public SepoaOut doSelectContractObjectExtraData(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
        
		try {
			ConnectionContext ctx = getConnectionContext();
			Map<String, String> headerData = (Map<String, String>) data.get(SepoaDataMapper.KEY_HEADER_DATA);
			SepoaReqSealManager smr = new SepoaReqSealManager(info, new Configuration(), ctx);
			smr.setDocBaiscInfo(headerData);
			
			Map<String, String> extraData = smr.doSelectExtraInfo(headerData);
			if(extraData == null) {
				setValue("__NO_EXTRA_DATA__");
			} else {
				Map<String, Object> extraDetailData = new Gson().fromJson(extraData.get("DATA"), Map.class);
				List<Map<String, String>> signerDataList = (List<Map<String, String>>) extraDetailData.get("DATA");
				Collections.sort(signerDataList, new Comparator<Map<String, String>>() {
					@Override
					public int compare(Map<String, String> o1, Map<String, String> o2) {
						// TODO Auto-generated method stub
						return Integer.valueOf(MapUtils.getString(o1, "SIGNER_SEQ", "0")) - Integer.valueOf(MapUtils.getString(o2, "SIGNER_SEQ", "0"));
					}
					
				});
				
				List<Map<String, String>> returnSignerList = new LinkedList<>();
				for(Map<String, String> signerData : signerDataList) {
					SepoaXmlParser sxp = new SepoaXmlParser(this, "doSelectSignerCompanyAndManager");
					SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					SepoaFormater sf = new SepoaFormater(ssm.doSelect(signerData));
					
					Map<String, String> returnSignerData = new HashMap<>();
					if(sf.getRowCount() > 0) {
						String category = "S";
						if(signerDataList.indexOf(signerData) == 0) {
							category = "C";
						}
						
						Map<String, String> signerCompanyInfoAndManagerData = sf.getListMap().get(0);
						//인터페이스시 받은 서명 값으로 세팅하는 구간
						returnSignerData.put("SIGNER_ID", signerData.get("SIGNER_ID"));
						returnSignerData.put("SIGNER_EMAIL", signerData.get("EMAIL"));
						returnSignerData.put("SEQ", signerData.get("SIGNER_SEQ"));
						returnSignerData.put("SIGNER_PHONE", signerData.get("SIGNER_PHONE"));
						returnSignerData.put("SIGN_KEY", signerData.get("SIGN_KEY"));
						
						//서명자 회사 정보로 데이터 세팅하는 구간
						returnSignerData.put("SIGNER_COMPANY_CODE", signerCompanyInfoAndManagerData.get("BIZ_CODE"));
						returnSignerData.put("SIGNER_COMPANY_NAME", signerCompanyInfoAndManagerData.get("SELLER_NAME_LOC"));
						returnSignerData.put("SIGNER_NAME", signerCompanyInfoAndManagerData.get("USER_NAME"));
						returnSignerData.put("SIGNER_COMPANY_OWNER", signerCompanyInfoAndManagerData.get("CEO_NAME_LOC"));
						returnSignerData.put("SIGNER_COMPANY_ADDR", signerCompanyInfoAndManagerData.get("ADDRESS_LOC"));
						returnSignerData.put("IRS_NO", signerCompanyInfoAndManagerData.get("IRS_NO"));
						returnSignerData.put("CATEGORY", category);
					}
					
					returnSignerList.add(returnSignerData);
				}
				
				setValue(new Gson().toJson(returnSignerList));
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
