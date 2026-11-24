package sepoa.svc.admin;

import java.io.File;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;

@Description("시스템관리 [서비스]")
@Permission(key="WORK_TYPE", value="Z")
public class AD_402 extends SepoaService {

	public AD_402(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
	@SuppressWarnings("unchecked")
	@Description("서비스 삭제")
	public SepoaOut doDeleteService(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doDeleteService");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(headerData);
			
			sxp = new SepoaXmlParser(this, "doDeleteConfig");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(headerData);
			
			String configPath = System.getProperty("sepoa.config.file");
			String parentConfigPath = new File(configPath).getParent();
			File clientConfigFile = new File(parentConfigPath + File.separator + "sepoa."+headerData.get("CLIENT_CODE")+".properties");
			
			if(clientConfigFile.exists()) {
				FileUtils.forceDelete(clientConfigFile);
			}
			
			Commit();
		} catch (Exception e) {
			Rollback();
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			System.err.println(e);
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
}
