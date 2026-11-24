package sepoa.svc.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;

@Description("리소스 탐색기")
public class RESOURCE_FINDER extends SepoaService {
	public RESOURCE_FINDER(String opt, SepoaInfo info) throws SepoaServiceException {
		super("NONDBJOB", info);
		setVersion("1.0.0");
	}
	
	@Description("파일 조회")
	public SepoaOut getFile(Map<String, String> headerData) {
		setStatus(1);
		try {
			String name = headerData.get("name");
			name = name.replaceAll("\\.\\./", "");
			
			Config config = new Configuration();
			String modules[] = config.getString("sepoa.module.priority", "FRWK").split(",");
			String resourcePath = "resource/" + name;
			String resultPath = null;
			
			List<String> searchPath = new ArrayList<String>();
			
			for(String module : modules) {
				String serviceRoot = "";
				
				if(!module.equals("FRWK")) {
					serviceRoot = config.getString("sepoa.service.root." + module);
				} else {
					serviceRoot = config.getString("sepoa.service.root");
				}
				
				File target = new File(serviceRoot + "/" + resourcePath);
				if(target.exists()) {
					resultPath = target.getAbsolutePath();
					break;
				}else {
					searchPath.add(target.getAbsolutePath());
				}
			}
			if(!StringUtils.isEmpty(resultPath)) {
				setValue(resultPath);
				setValue("NOT");
			}else{
				throw new Exception("not found resource file in \n"+StringUtils.join(searchPath, "\n"));
			}
		}catch(Exception e) {
			setStatus(0);
			setMessage(Cache.text.get("MESSAGE.1002"));
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
}