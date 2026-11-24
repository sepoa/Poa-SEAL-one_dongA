package sepoa.svc.tools;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cache.Cache;
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

@Description("컴포넌트 개인화")
public class Personalization extends SepoaService {
	public Personalization(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
	
	@SuppressWarnings("unchecked")
	@Description("프리셋 리스트 조회")
	public SepoaOut doQueryList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try{
			Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "doQueryList");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		}catch(Exception e) {
			setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            e.printStackTrace();
            Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("프리셋 저장")
	public SepoaOut doSavePreset(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try{
			Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			String useFlag = MapUtils.getBoolean(headerData, "USE_FLAG", false) == true ? "Y" : "N";
			headerData.put("USE_FLAG", useFlag);
			
			if(StringUtils.isBlank(headerData.get("CODE"))) {
				headerData.put("CODE", UUID.randomUUID().toString());
			}
			
			sxp = new SepoaXmlParser(this, "doQueryPreset");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			List<Map<String, String>> result = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			
			if(result.size() > 0) {
				// update
				sxp = new SepoaXmlParser(this, "doUpdatePreset");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(headerData);
			} else {
				// insert
				sxp = new SepoaXmlParser(this, "doInsertPreset");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(headerData);
			}
			
			if(StringUtils.equals(useFlag, "Y")) {
				sxp = new SepoaXmlParser(this, "doUnusePreset");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(headerData);
			}
			
			sxp = new SepoaXmlParser(this, "doSetUsePreset");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(headerData);
			
			Commit();
			
			Cache.clearAll();
		}catch(Exception e) {
			Rollback();
			setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            e.printStackTrace();
            Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("프리셋 삭제")
	public SepoaOut doDeletePreset(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try{
			Map<String, String> headerData = (Map<String, String>)allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "doDeletePreset");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(headerData);
			
			Commit();
			
			Cache.clearAll();
		}catch(Exception e) {
			Rollback();
			setStatus(0);
            setValue("");
            setFlag(false);
            setMessage(e.getMessage());
            e.printStackTrace();
            Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
}
