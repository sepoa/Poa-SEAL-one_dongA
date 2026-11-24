package sepoa.svc.ct;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

public class SepoaContractCustomService extends SepoaService{
	
	public SepoaContractCustomService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@SuppressWarnings("unchecked")
	@Description("결재 프로세스 완료 이후 진행옵션 지정")
	public SepoaOut doApprovalFinishAfter(Map<String, String> options) {
		setStatus(1);
		setFlag(true);
		try {
			options.put("NEXT_SERVICE", "REVIEW");
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("검토 프로세스 완료 이후 진행옵션 지정")
	public SepoaOut doReviewCompleteAfter(Map<String, String> options) {
		setStatus(1);
		setFlag(true);
		try {
			options.put("AUTO_SIGN_FLAG", "true");
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		} finally {
			
		}
		
		return getSepoaOut();
	}
}
