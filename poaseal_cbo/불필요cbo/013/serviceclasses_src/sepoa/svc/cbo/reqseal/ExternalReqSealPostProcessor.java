package sepoa.svc.cbo.reqseal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;

import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
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
import sepoa.seal.SepoaReqSealManager;

@Description("외부 인터페이스 날인요청 데이터 생성 후 후처리 클래스")
public class ExternalReqSealPostProcessor extends SepoaService{
	public ExternalReqSealPostProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("포스코플로우 날인요청 인터페이스 데이터 후처리")
	public SepoaOut doProcess(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
        
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			//SepoaReqSealService에서 넘겨받은 전체 데이터
			Map<String, Object> params = (Map<String, Object>) data.get("DATA");
			//업무데이터 초기화
			Map<String, Object> businessData = (Map<String, Object>) params.get("data");
			//인감 날인요청 데이터 초기화
			Map<String, Object> reqSealInfo = (Map<String, Object>) businessData.get("REQ_SEAL_INFO");
			//서명자 리스트 데이터 초기화
			List<Map<String, String>> signerList = (List<Map<String, String>>) businessData.get("SIGNER_LIST");
			//날인요청 매니저 객체 할당
			SepoaReqSealManager smr = (SepoaReqSealManager) data.get("MANAGER");
			
			
			if(signerList != null && signerList.size() > 0) {
				for(Map<String, String> signer : signerList) {
					String houseCode = info.getSession("HOUSE_CODE");
					String companyCode = MapUtils.getString(signer, "BIZ_CODE", "");
					String userId = MapUtils.getString(signer, "SIGNER_ID", "");
					signer.put("SIGNER_ID", signer.get("SIGNER_ID").toUpperCase());
					
					if(!smr.doCheckExistUser(houseCode, companyCode, userId)) {
						String email = MapUtils.getString(signer, "EMAIL", "");
						String tempId = email.split("@")[0];
						signer.put("TMP_ID", tempId);
						
						sxp = new SepoaXmlParser(this, "doInsertSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(signer);
						
						signer.put("PASSWORD", SepoaAuthEncryptManager.getEncrypted(signer.get("SIGN_KEY").replaceAll("[^\\d]", "")));
						sxp = new SepoaXmlParser(this, "doInsertSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(signer);
					}
				}
				
				//고객사 별 추가 데이터 저장 객체
				if(getStatus() == 1) {
					Map<String, Object> extraData = new HashMap<String, Object>();
					extraData.put("SMR_NUMBER", reqSealInfo.get("SMR_NUMBER"));
					extraData.put("DATA", signerList);
					
					if(smr.doSaveExtraInfo(extraData) == null) {
						setStatus(107002);
					}
				}
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
