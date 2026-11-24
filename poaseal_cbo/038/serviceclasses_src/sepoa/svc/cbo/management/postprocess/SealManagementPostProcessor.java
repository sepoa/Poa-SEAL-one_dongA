package sepoa.svc.cbo.management.postprocess;

import java.util.List;
import java.util.Map;

import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;

@Description("날인대장 데이터 조회결과 후처리 프로세서")
public class SealManagementPostProcessor extends SepoaService{
	public SealManagementPostProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("동아제약 날인대장 데이터 조회결과 후처리 프로세서")
	public SepoaOut doProcess(List<Map<String, String>> data) {
		setStatus(1);
		setFlag(true);
        
		try {
			for(Map<String, String> element : data) {
				element.put("NO", String.valueOf(data.indexOf(element) + 1));
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
