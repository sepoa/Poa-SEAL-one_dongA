package sepoa.svc.admin;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Configuration;
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

@Description("클라이언트관리 - 모듈 설정")
public class AD_403 extends SepoaService {

	public AD_403(String opt, SepoaInfo info) throws SepoaServiceException {
		super("TRANSACTION", info);
	}
	
	@SuppressWarnings("unchecked")
	@Description("트리그리드 조회")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doTreeGridQuery(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = null;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			ctx = getClientCTX(headerData.get("HOUSE_CODE"));
			
			if(ctx == null) {
				throw new Exception("Client ConnectionContext is null");
			}
			
			sxp = new SepoaXmlParser(this, "doQueryModuleMaster");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("필터데이터 조회")
	public SepoaOut doQueryFilteredData(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = null;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			ctx = getClientCTX(headerData.get("HOUSE_CODE"));
			
			if(ctx == null) {
				throw new Exception("Client ConnectionContext is null");
			}
			
			sxp = new SepoaXmlParser(this, "doQueryModuleFiltered");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("그리드 저장")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doSave(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = null;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			
			ctx = getClientCTX(headerData.get("HOUSE_CODE"));
			
			if(ctx == null) {
				throw new Exception("Client ConnectionContext is null");
			}
			
			sxp = new SepoaXmlParser(this, "doDelete");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(headerData);
			
			sxp = new SepoaXmlParser(this, "doInsert");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(gridData, headerData);
			
			Commit();
			
			Cache.grid.removeAll();
			Cache.form.removeAll();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	private ConnectionContext getClientCTX(String clientCode) {
		ConnectionContext ctx = null;
		
		try {
			Configuration config = new Configuration(clientCode);
			String appName = config.getString("sepoa.app.name", "TOMCAT");
			String dataSource = config.get("sepoa.app." + appName.toLowerCase() + ".datasource");
			
			if(StringUtils.isBlank(dataSource)) {
				throw new Exception("sepoa.app." + appName.toLowerCase() + ".datasource not found");
			}
			
			Matcher m = Pattern.compile("jdbc/poa-([a-zA-Z]{0,})-datasource").matcher(dataSource);
	        if(m.find()) {
	        	if(!m.group(1).isEmpty()) {
	        		String jndi = m.group(1).toLowerCase();
        			joinTransaction(jndi);
	        		ctx = getConnectionContext(jndi);
	        	}
	        }
		} catch (Exception e) {
			Logger.err.println(this, e);
		}
		
		return ctx;
	}
}
