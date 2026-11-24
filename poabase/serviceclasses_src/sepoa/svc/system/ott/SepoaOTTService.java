package sepoa.svc.system.ott;

import java.util.Map;

import org.apache.commons.collections4.map.SingletonMap;

import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.ott.SepoaOTTManager;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("일회용 토큰")
public class SepoaOTTService extends SepoaService{
	public SepoaOTTService(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
	
	@SuppressWarnings("unchecked")
	@Description("생성")
	public SepoaOut getToken(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		if(allData.containsKey(SepoaModuleDataMapper.KEY_OBJECT_DATA)) {
			Map<String, Object> objectData = (Map<String, Object>) allData.get(SepoaModuleDataMapper.KEY_OBJECT_DATA);
			if(objectData.containsKey("data")) {
				Object data = objectData.get("data");
				setValue(new SingletonMap<>("token", SepoaOTTManager.generate(data)));
			}else {
				setStatus(0);
			}
		}else {
			setStatus(0);
		}
		if(getStatus() != 1) {
			setFlag(false);
			setMessage("not found data");
		}
		return getSepoaOut();
	}
}
