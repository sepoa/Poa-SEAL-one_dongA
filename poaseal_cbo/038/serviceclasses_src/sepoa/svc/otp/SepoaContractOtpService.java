package sepoa.svc.otp;

import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBOpenException;
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

@Description("전자계약 전화번호 인증 (코스맥스 전용)")
public class SepoaContractOtpService extends SepoaService{
	public SepoaContractOtpService(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
	
	@SuppressWarnings({"unchecked"})
	@Description("인증번호 확인")
	public SepoaOut checkAuthNumber(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			String signerIrsNo = MapUtils.getString(headerData, "AUTH_NUMBER", null);

			headerData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
			headerData.put("SIGNER_ID", info.getSession("ID"));
			
			sxp = new SepoaXmlParser(this, "doSelectAuthUserIrsNo");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if (sf.getRowCount() > 0) {
				Map<String, String> authData = sf.getListMap().get(0);
				String authUserIrsNo = MapUtils.getString(authData, "SIGN_KEY", null);
				
				if(authUserIrsNo != null && signerIrsNo != null && !authUserIrsNo.equals("") && !signerIrsNo.equals("")) {
					// 하이픈 제거
					String cleanedSignerIrsNo = signerIrsNo.replaceAll("-", "");
					String cleanedAuthUserIrsNo = authUserIrsNo.replaceAll("-", "");
					
					if (!isValidIrsNo(cleanedSignerIrsNo)) {
						setStatus(2); // 번호 포멧이 맞지 않음
					} else if (!cleanedAuthUserIrsNo.equals(cleanedSignerIrsNo)) {
						setStatus(3); // 번호가 일치하지 않음
					}
				} else {
					setStatus(4); // 인증정보 찾을 수 없음
				}
			} else {
				setStatus(4); // 인증정보를 찾을 수 없음
			}
		} catch (Exception e) {
            setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("인증번호 포멧 유효성 검사")
	private boolean isValidIrsNo(String signerIrsNo) {
		if(signerIrsNo == null || signerIrsNo.trim().isEmpty()) {
			return false;
		}
		
		// 10자리 숫자인지 검증
		String regex = "^\\d{10}$";
		return signerIrsNo.matches(regex);
	}
	
}
