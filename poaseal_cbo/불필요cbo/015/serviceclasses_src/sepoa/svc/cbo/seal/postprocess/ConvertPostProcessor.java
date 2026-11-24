package sepoa.svc.cbo.seal.postprocess;

import java.util.Map;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;

@Description("컨버팅 프로세스 - 파일명 커스텀 변경")
public class ConvertPostProcessor extends SepoaService{
	public ConvertPostProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("컨버팅파일 업로드전 파일명 커스텀 변경")
	public SepoaOut doProcess(Map<String, Object> data) throws Exception {
		setStatus(1);
		setFlag(true);
		Config config = new Configuration();
		try {
			String fileNameClient = (String) data.get("FILE_NAME_CLIENT");
			String layer = (String) data.get("LAYER");
			
			String sealSymText = config.getString("sepoa.seal.sym.text", fileNameClient); // 좌측날인
			String sealOrgText = config.getString("sepao.seal.org.text", fileNameClient); // 우측날인
			
			if("CONVERT_SEAL_SYM".equalsIgnoreCase(layer)) {
				data.put("FILE_NAME_CLIENT", "갑_" + fileNameClient);
			}else if("CONVERT_SEAL_ORG".equalsIgnoreCase(layer)) {
				data.put("FILE_NAME_CLIENT", "을_" + fileNameClient);
			}
			
			setValue(data);
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
