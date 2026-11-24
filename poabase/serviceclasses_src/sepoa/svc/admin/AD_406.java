package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import sepoa.fw.util.SepoaFormater;

@Description("클라이언트관리 - 알림설정")
@Permission(key="WORK_TYPE", value="Z")
public class AD_406 extends SepoaService {

	private static final String[] customOptions = {"CUSTOM_USE", "CUSTOM_INFO"};
	private static final String[] notUseOptions = {"USE", "FORCE_RECEIVER"};
	private static final String[] kakaoDefaultOptions = {"BIZPLUS_SENDER_KEY", "BIZPLUS_CLIENT_ID", "BIZPLUS_CLIENT_PASSWORD"};
	private static final String[] smtpDefaultOptions = {"SERVER", "PORT", "SSL", "ENCODING", "AUTH", "AUTH_ID", "AUTH_PASS", "SENDER_NAME", "SENDER_MAIL"};
	private static final String[] smsDefaultOptions = {"SEPOA_API_USER_CODE", "CALLBACK"};
	
	public AD_406(String opt, SepoaInfo info) throws SepoaServiceException {
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

		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			ctx = getClientCTX(headerData.get("CLIENT_CODE"));
			
			if(ctx == null) {
				throw new Exception("Client ConnectionContext is null");
			}
			
			List<Map<String, String>> result = new ArrayList<Map<String, String>>(1);
			
			sxp = new SepoaXmlParser(this, "doQuery");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doSelect(headerData);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			if(sf.getRowCount() > 0) {
				List<Map<String, String>> list = sf.getListMap();
            	Map<String, String> clientNotificationOptions = new HashMap<String, String>();
				for (Map<String, String> item : list) {
					String key = item.get("GROUP") + "_" + item.get("KEY");
					
					clientNotificationOptions.put(key, item.get("VALUE"));
					result.add(clientNotificationOptions);
				}
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
			
			List<Map<String, String>> configList = new ArrayList<Map<String, String>>();
			Map<String, String> clientData = new HashMap<String, String>();
			
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
				case "GROUPS":
					clientData.put(entry.getKey(), entry.getValue());
					break;
				default:
					Map<String, String> configMap = new HashMap<String, String>();
					if (!entry.getValue().isEmpty() && !entry.getKey().startsWith("unescaped_")) {
						String key = entry.getKey();
						String group = entry.getKey().split("_")[0];
						
						key = key.replace(group + "_", "");
						
						configMap.put("KEY", key);
						configMap.put("VALUE", entry.getValue());
						configMap.put("GROUP", group);
						configList.add(configMap);
					}
					break;
				}
			}
			
			List<Map<String, String>> useList = configList.stream().filter(x -> "USE".equalsIgnoreCase(x.get("KEY"))).collect(Collectors.toList());
			
			
			for(Map<String, String> useMap : useList) {
				String group = useMap.get("GROUP");
				boolean useFlag = MapUtils.getBooleanValue(useMap, "VALUE");
				
				List<String> optionsList = new ArrayList<String>();
				optionsList.addAll(Arrays.asList(notUseOptions));
				
				if("SMS".equalsIgnoreCase(group)) {
					optionsList.add("MODULE");
				}
				
				if(!useFlag) {
					configList.removeIf(x -> group.equalsIgnoreCase(x.get("GROUP")) && !Arrays.asList(notUseOptions).contains(x.get("KEY")));
				} else {
					boolean customFlag = false;
					
					if("SMS".equalsIgnoreCase(group)) {
						Map<String, String> smsMap = configList.stream().filter(x -> "SMS".equalsIgnoreCase(x.get("GROUP")) && "MODULE".equalsIgnoreCase(x.get("KEY"))).findFirst().orElse(new HashMap<String, String>());
						String module = MapUtils.getString(smsMap, "VALUE");
						
						if("customservice".equalsIgnoreCase(module)) {
							customFlag = true;
						}
					} else {
						Map<String, String> customUseMap = configList.stream().filter(x -> group.equalsIgnoreCase(x.get("GROUP")) && "CUSTOM_USE".equalsIgnoreCase(x.get("KEY"))).findFirst().orElse(new HashMap<String, String>());
						customFlag = MapUtils.getBooleanValue(customUseMap, "VALUE", false);
					}
					
					if(customFlag) {
						optionsList.addAll(Arrays.asList(customOptions));
					} else {
						if("KAKAO".equalsIgnoreCase(group)) {
							optionsList.addAll(Arrays.asList(kakaoDefaultOptions));
						} else if("SMTP".equalsIgnoreCase(group)) {
							optionsList.addAll(Arrays.asList(smtpDefaultOptions));
						} else if("SMS".equalsIgnoreCase(group)) {
							optionsList.addAll(Arrays.asList(smsDefaultOptions));
						}
					}
					
					configList.removeIf(x -> group.equalsIgnoreCase(x.get("GROUP")) && !optionsList.contains(x.get("KEY")));
				}
			}
			
			sxp = new SepoaXmlParser(this, "doDelete");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(clientData);

			sxp = new SepoaXmlParser(this, "doSave");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(configList, clientData);
			
			Commit();
			
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
