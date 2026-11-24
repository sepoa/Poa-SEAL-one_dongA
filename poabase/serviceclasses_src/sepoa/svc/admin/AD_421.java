package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sepoa.fw.cache.Cache;
import sepoa.fw.cache.SepoaCacheModule;
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

@Description("메뉴필터관리")
public class AD_421 extends SepoaService {

	public AD_421(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
	@SuppressWarnings("unchecked")
	@Description("클라이언트 조회")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doQueryClient(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doQueryClient");
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
	@Description("클라이언트 프로파일 조회")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doQueryProfile(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doQueryProfile");
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
	@Description("클라이언트 메뉴 조회")
	public SepoaOut doQueryMenuFilter(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, Object> moduleMap = ((SepoaCacheModule)Cache.module).getModule(info.getSession("HOUSE_CODE"));
			Set<String> moduleList = null;
			
			if(moduleMap != null) {
				moduleList = moduleMap.keySet();
			}
			
			sxp = new SepoaXmlParser(this, "doQueryMenuFilterAll");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			List<Map<String, String>> menuList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			List<Map<String, String>> processList = new ArrayList<Map<String, String>>();
			Stack<Map<String, String>> moduleFilteredStack = new Stack<Map<String, String>>();
			
			for(Map<String, String> menu : menuList) {
				moduleFilteredStack.push(menu);
				
				String module = menu.get("MODULE");
				if(module != null && !module.isEmpty()) {
					if(moduleList != null && !"000".equals(info.getSession("HOUSE_CODE"))) {
						moduleFilteredStack.pop();
						String[] keys = module.split(",");
						
						for(String key : keys) {
							if(moduleList.contains(key)) {
								moduleFilteredStack.push(menu);
								break;
							}
						}
					}
				}
			}
			
			processList.addAll(moduleFilteredStack);
			
			setValue(new SepoaFormater(moduleFilteredStack));
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
	@Description("메뉴 필터 저장")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doSaveMenuFilter(Map<String, Object> allData) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true); 
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			String jsonData = headerData.get("FILTER_DATA");
			jsonData = jsonData.replaceAll("&#34;", "\"");
			
			JsonParser parser = new JsonParser();
			JsonArray filterArray = (JsonArray) parser.parse(jsonData);
			List<Map<String, String>> filterMap = new ArrayList<Map<String,String>>();
			Set<String> groupSet = new HashSet<String>();
			List<Map<String, String>> groupList = new ArrayList<Map<String,String>>();
			ObjectMapper mapper = new ObjectMapper();
			
			for(int i=0; i < filterArray.size(); i++) {
				JsonObject filterObject = (JsonObject)filterArray.get(i);
				Map<String, String> fMap = mapper.readValue(filterObject.toString(), Map.class);
				groupSet.add(fMap.get("PARENT_ID"));
				filterMap.add(fMap);
			}
			
			Iterator<String> it = groupSet.iterator();
			while(it.hasNext()) {
				Map<String, String> gMap = new HashMap<String, String>();
				gMap.put("MENU_ID", it.next());
				groupList.add(gMap);
			}
			
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
			
			for(ConnectionContext ctx : ctxList) {
				sxp = new SepoaXmlParser(this, "doDeleteMenuFilter");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);
				
				if(filterArray.size() > 0) {
					sxp = new SepoaXmlParser(this, "doSaveMenuFilter");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(filterMap, headerData);
				}
				
				if(groupList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doSaveMenuFilter");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(groupList, headerData);
				}
			}
		
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
}
