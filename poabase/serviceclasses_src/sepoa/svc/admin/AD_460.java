package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

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
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("알림설정")
@Permission(key="WORK_TYPE", value="Z")
public class AD_460 extends SepoaService {

	public AD_460(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
	@SuppressWarnings({ "unchecked" })
    @Description("클라이언트 조회")
    public SepoaOut doQueryClient(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            
            String houseCode = info.getSession("HOUSE_CODE");
			if(!"000".equals(houseCode)) {
				headerData.put("HOUSE_CODE", houseCode);
			}
            
            sxp = new SepoaXmlParser(this, "doQueryClient");
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
	@Description("리스트 조회")
	public SepoaOut doList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser(this, "doSelectUPUSH");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			System.err.println(e);
			Logger.err.println(e);
			e.printStackTrace();
		}

		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("알림 템플릿 상세조회")
	public SepoaOut doQueryTemplate(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, String> resultData = new HashMap<String, String>();
			
			List<Map<String, String>> soData = null;
			
			sxp = new SepoaXmlParser(this, "doSelectUPUSO");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			soData = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			
			if(soData.isEmpty()) {
				sxp = new SepoaXmlParser(this, "doSelectUPUSOMaster");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				soData = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			} else {
				List<String> options = new ArrayList<String>();
				for(Map<String, String> client : soData) {
					options.add(client.get("TYPE") + "_" + client.get("OPTION_NAME"));
				}
				
				sxp = new SepoaXmlParser(this, "doSelectUPUSOMaster");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				List<Map<String, String>> masterList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
				
				for(Map<String, String> master : masterList) {
					if(!options.contains(master.get("TYPE") + "_" + master.get("OPTION_NAME"))) {
						soData.add(master);
					}
				}
			}
			
			for(Map<String, String> row : soData) {
				resultData.put(row.get("TYPE")+"_"+row.get("OPTION_NAME"), row.get("OPTION_VALUE"));
			}
			
			resultData.put("CODE", headerData.get("CODE"));
			resultData.put("DESCRIPTION", StringEscapeUtils.unescapeHtml(headerData.get("DESCRIPTION")));
			
			List<Map<String, String>> result = new ArrayList<Map<String, String>>(1);
			result.add(resultData);
			setValue(new SepoaFormater(result).getString());
			
		} catch (Exception e) {
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			Logger.err.println(e);
		}

		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("저장")
	public SepoaOut doSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
			
			ArrayList<ConnectionContext> ctxList = new ArrayList<ConnectionContext>(5);
			ctxList.add(getConnectionContext());
			
			String jndiStr = CommonUtil.getConfig("sepoa.admin.sync.jndi");
			if (!StringUtils.isBlank(jndiStr)) {
				String defaultDatasource = CommonUtil.getConfig("sepoa.app."+CommonUtil.getConfig("sepoa.app.name").toLowerCase()+".datasource");
				String[] jndiStrArray = jndiStr.split(";");
				for (String jndi : jndiStrArray) {
					String datasource = CommonUtil.getConfig("sepoa.app."+jndi+".datasource");
					if (!datasource.equalsIgnoreCase(defaultDatasource)) {
						joinTransaction(jndi);
						ctxList.add(getConnectionContext(jndi));
					}
				}
			}
			
			String activeModuleID = headerData.get("ACTIVE_MODULE_ID");
			String subModuleID = getSubModuleID(headerData);
			headerData.put("TYPE", activeModuleID);
			
			for(ConnectionContext ctx : ctxList) {
				sxp = new SepoaXmlParser(this, "doSelectUPUSO");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				List<Map<String, String>> soData = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
				List<Map<String, String>> deleteHistory = new ArrayList<Map<String, String>>();
				
				for(Map<String, String> so : soData) {
					if(!StringUtils.isBlank(subModuleID)) {
						if(!so.get("OPTION_NAME").equals(subModuleID)) {
							continue;
						}
					}
					
					Map<String, String> deleteMap = new HashMap<String, String>(so);
					deleteMap.put("HOUSE_CODE", headerData.get("HOUSE_CODE"));
					deleteMap.put("ACTION_MODE", "DELETE");
					
					sxp = new SepoaXmlParser(this, "doDeleteUPUSO");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doDelete(deleteMap);
					
					deleteHistory.add(deleteMap);
				}
				
				if(deleteHistory.size() > 0) {
					doInsertHistory(ctx, deleteHistory, "UPUSO");
				}
				
				List<Map<String, String>> gridData = new LinkedList<Map<String, String>>();
				List<Map<String, String>> insertHistory = new LinkedList<Map<String, String>>();
				for (Entry<String, String> entry : headerData.entrySet()) {
					String key = entry.getKey();
					String value = null;
					
					if (key.startsWith(activeModuleID + "_")) {
						value = entry.getValue();
						key = key.substring(activeModuleID.length() + 1);
						
						if(!StringUtils.isBlank(subModuleID)) {
							if(!key.equals(subModuleID)) {
								continue;
							}
						}
						
						Map<String, String> row = new HashMap<String, String>();
						
						row.put("HOUSE_CODE", headerData.get("HOUSE_CODE"));
						row.put("CODE", headerData.get("CODE"));
						row.put("ACTION_MODE", "INSERT");
						row.put("TYPE", activeModuleID);
						row.put("OPTION_NAME", key);
						row.put("OPTION_VALUE", value);

						gridData.add(row);
						insertHistory.add(row);
					}
				}
				
				if (gridData.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsertUPUSO");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(gridData, true);
				}
				
				if(insertHistory.size() > 0) {
					doInsertHistory(ctx, insertHistory, "UPUSO");
				}
			}

			Commit();
		} catch (Exception e) {
			Rollback();
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			Logger.err.println(e);
		}

		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("삭제")
	public SepoaOut doDelete(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			ArrayList<ConnectionContext> ctxList = new ArrayList<ConnectionContext>(5);
			ctxList.add(getConnectionContext());
			
			String jndiStr = CommonUtil.getConfig("sepoa.admin.sync.jndi");
			if (!StringUtils.isBlank(jndiStr)) {
				String defaultDatasource = CommonUtil.getConfig("sepoa.app."+CommonUtil.getConfig("sepoa.app.name").toLowerCase()+".datasource");
				String[] jndiStrArray = jndiStr.split(";");
				for (String jndi : jndiStrArray) {
					String datasource = CommonUtil.getConfig("sepoa.app."+jndi+".datasource");
					if (!datasource.equalsIgnoreCase(defaultDatasource)) {
						joinTransaction(jndi);
						ctxList.add(getConnectionContext(jndi));
					}
				}
			}
			
			String activeModuleID = headerData.get("ACTIVE_MODULE_ID");
			String subModuleID = getSubModuleID(headerData);
			headerData.put("TYPE", activeModuleID);
			
			for(ConnectionContext ctx : ctxList) {
				sxp = new SepoaXmlParser(this, "doSelectUPUSO");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				List<Map<String, String>> soData = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
				List<Map<String, String>> deleteHistory = new ArrayList<Map<String, String>>();
				
				for(Map<String, String> so : soData) {
					if(!StringUtils.isBlank(subModuleID)) {
						if(!so.get("OPTION_NAME").equals(subModuleID)) {
							continue;
						}
					}
					
					Map<String, String> deleteMap = new HashMap<String, String>(so);
					deleteMap.put("HOUSE_CODE", headerData.get("HOUSE_CODE"));
					deleteMap.put("ACTION_MODE", "DELETE");
					
					sxp = new SepoaXmlParser(this, "doDeleteUPUSO");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doDelete(deleteMap);
					
					deleteHistory.add(deleteMap);
				}
				
				if(deleteHistory.size() > 0) {
					doInsertHistory(ctx, deleteHistory, "UPUSO");
				}
			}
			
			Commit();
		} catch (Exception e) {
			Rollback();
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			Logger.err.println(e);
		}

		return getSepoaOut();
	}
	
	private String getSubModuleID(Map<String, String> headerData) {
		String res = null;
		String module = headerData.get("ACTIVE_MODULE_ID");
		
		if(module.equals("DATASOURCE")) {
			res = headerData.get("ACTIVE_DATASOURCE_ID");
		}
		
		return res;
	}
	
	@SuppressWarnings("unchecked")
	@Description("히스토리 저장")
	private void doInsertHistory(ConnectionContext ctx, Object data, String type) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> keyMap = new SingletonMap<String, String>("HISTORY_KEY", String.valueOf(System.nanoTime()));
			
			sxp = new SepoaXmlParser(this, "doInsert" + type + "History");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			if(data instanceof Map) {
				ssm.doInsert((Map<String, String>) data, keyMap);
			} else if(data instanceof List) {
				ssm.doInsert((List<Map<String, String>>) data, keyMap, true);
			}
			
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
	}
}
