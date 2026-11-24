package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import sepoa.fw.srv.ServletServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.ux.design.DynamicCSS;

@Description("클라이언트관리 - 디자인설정")
public class AD_404 extends SepoaService {

	public AD_404(String opt, SepoaInfo info) throws SepoaServiceException {
		super("TRANSACTION", info);
	}

	@SuppressWarnings("unchecked")
	@Description("조회")
	public SepoaOut doQuery(Map<String, Object> allData) {

		setStatus(1);
		setFlag(true);

		ConnectionContext ctx = null;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		Map<String, String> defaultCssOptions = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			ctx = getClientCTX(headerData.get("CLIENT_CODE"));
			
			if(ctx == null) {
				throw new Exception("Client ConnectionContext is null");
			}
			
			if(headerData.containsKey("NOT_MERGE")) {
				defaultCssOptions = new HashMap<String, String>();
			}else {
				defaultCssOptions = DynamicCSS.getBuiltInCssOptions();
			}
			
			List<Map<String, String>> result = new ArrayList<Map<String, String>>(1);
			result.add(defaultCssOptions);
			
			headerData.put("GROUP", "DESIGN");

			sxp = new SepoaXmlParser(this, "doQuery");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			if(sf.getRowCount() > 0) {
				List<Map<String, String>> list = sf.getListMap();
            	Map<String, String> clientCssOptions = new HashMap<String, String>();
				for (Map<String, String> item : list) {
					clientCssOptions.put(item.get("KEY"), item.get("VALUE"));
				}
            	defaultCssOptions.putAll(clientCssOptions);
			}
			setValue(new SepoaFormater(result).getString());
		} catch (Exception e) {
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			Logger.err.println(e);
			e.printStackTrace();
		}

		return getSepoaOut();
	}

	@SuppressWarnings("unchecked")
	@Description("저장")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = null;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			ctx = getClientCTX(headerData.get("CLIENT_CODE"));
			
			if(ctx == null) {
				throw new Exception("Client ConnectionContext is null");
			}
			
			List<Map<String, String>> configData = new ArrayList<Map<String, String>>();
			Map<String, String> clientData = new HashMap<String, String>();
			clientData.put("GROUP", "DESIGN");
			
			for (Entry<String, String> entry : headerData.entrySet()) {
				switch (entry.getKey()) {
				case "blank":
				case "session_user_name":
				case "session_user_id":
				case "current_date":
				case "current_time":
					continue;
				case "CLIENT_CODE":
				case "COMPANY_CODE":
					clientData.put(entry.getKey(), entry.getValue());
					break;
				default:
					Map<String, String> configMap = new HashMap<String, String>();
					if (!entry.getValue().isEmpty() && !entry.getKey().startsWith("unescaped_")) {
						configMap.put("KEY", entry.getKey());
						configMap.put("VALUE", entry.getValue());
						configData.add(configMap);
					}
					break;
				}
			}
			
			sxp = new SepoaXmlParser(this, "doDelete");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(clientData);

			sxp = new SepoaXmlParser(this, "doSave");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(configData, clientData);
			
			Commit();
			
			ServletServiceConnector.doService(info, "sepoa.fw.ux.design.DynamicCSS", "CONNECTION", "clearAll", new Object[] {clientData});
			Cache.client.removeAll();
		} catch (Exception e) {
			Rollback();
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			Logger.err.println(e);
			e.printStackTrace();
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
