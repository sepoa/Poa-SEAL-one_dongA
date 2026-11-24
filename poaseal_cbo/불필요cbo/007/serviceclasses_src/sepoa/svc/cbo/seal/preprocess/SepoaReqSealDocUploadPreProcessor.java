package sepoa.svc.cbo.seal.preprocess;

import java.util.List;
import java.util.Map;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.sealmk2.SepoaReqSealManager;

@Description("날인서류 업로드 전처리기")
public class SepoaReqSealDocUploadPreProcessor extends SepoaService{
	public SepoaReqSealDocUploadPreProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("SECL 날인서류 업로드 전처리 프로세스")
	public SepoaOut doProcess(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		try {
			
		} catch (Exception e) {
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
}
