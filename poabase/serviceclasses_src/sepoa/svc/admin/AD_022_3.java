package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cache.Cache;
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
import sepoa.svc.common.COMMON;

@Description("그리드관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_022_3 extends SepoaService {
    public AD_022_3(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    @SuppressWarnings("unchecked")
    @Description("상세조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);

            List<Map<String, String>> result = null;
			Map<String, Map<String, String>> dataMap = new HashMap<String, Map<String, String>>(); 
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> sysLangList = COMMON.getSystemActiveLanguage(info, ctx);
			
			for (Map<String, String> language : sysLangList) {
				sxp = new SepoaXmlParser();
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				
				

				if (result == null) {
					result = new SepoaFormater(ssm.doSelect(headerData, language)).getListMap(true);
					
					for(Map<String, String> tmp : result) {
						dataMap.put(tmp.get("CODE"), tmp);
					}
					
					if(result != null && result.size() == 0) {
						result = null;
					}
				} else {
					List<Map<String, String>> tmpList = new SepoaFormater(ssm.doSelect(headerData, language)).getListMap(true);
					for(Map<String, String> tmpMap : tmpList) {
						Map<String, String> target = dataMap.get(tmpMap.get("CODE"));
						if(MapUtils.isEmpty(target)) {
							continue;
						}
						for(Entry<String, String> entry : tmpMap.entrySet()) {
							if(!target.containsKey(entry.getKey())) {
								target.put(entry.getKey(), entry.getValue());
							}
						}
					}
				}
				
			}
			
			Map<String, Object> langResultMap = new HashMap<String, Object>();
			
			if(result == null) {
				result = new ArrayList<Map<String,String>>();
			} else {
				sxp = new SepoaXmlParser(this, "doQuerySLANG");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				
				for(Map<String, String> sysLangMap : sysLangList) {
					SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData, sysLangMap));
					
					if(sf.getRowCount() > 0) {
						langResultMap.put(sysLangMap.get("LANGUAGE"), sf.getListMap(true));
					}
				}
			}
			
			for(Map<String, String> row : result) {
				for(Map.Entry<String, Object> entry : langResultMap.entrySet()) {
					String language = entry.getKey();
					List<Map<String, String>> languageList = (List<Map<String, String>>) entry.getValue();
					
					for(Map<String, String> langugeMap : languageList) {
						if(row.get("CODE").equals(langugeMap.get("CODE"))) {
							row.put("CONTENTS_" + language, langugeMap.get("CONTENTS"));
							row.put("COL_WIDTH_" + language, langugeMap.get("GRID_WIDTH"));
							break;
						}
					}
				}
			}
			
			
			
			setValue(new SepoaFormater(result));
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(0);
            setFlag(false);
            setMessage(e.getMessage());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    @SuppressWarnings("unchecked")
    @Description("삭제")
    public SepoaOut doDelete(Map<String, Object> allData) {
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);

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
            
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			for(Map<String, String> row : gridData){
                row.put("ACTION_MODE", "DELETE");
            }
            
            for(ConnectionContext ctx : ctxList) {
                sxp = new SepoaXmlParser(this, "doDelete");
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            ssm.doDelete(gridData);
	            
	            doInsertHistory(ctx, gridData, "SGRID");
            }
            
            Commit();
            
            Cache.grid.removeAll();
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
    @SuppressWarnings("unchecked")
    @Description("저장")
    public SepoaOut doSave(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
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
        	
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			for(ConnectionContext ctx : ctxList) {
				//SGRID
				
				List<Map<String, String>> insertList = new ArrayList<Map<String, String>>(gridData.size());
				List<Map<String, String>> updateList = new ArrayList<Map<String, String>>(gridData.size() * 2);
				List<Map<String, String>> history = new ArrayList<Map<String, String>>(gridData.size());
				for(Map<String, String> row : gridData){
					sxp = new SepoaXmlParser(this, "doQueryExistRow");
	                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                sf = new SepoaFormater(ssm.doSelect(row));
	                
	                Map<String, String> historyMap = new HashMap<String, String>(row);
					history.add(historyMap);

					boolean existRow = sf.getValue(0, 0).equals("1") ? true : false;
					if (existRow) {
						updateList.add(row);
						historyMap.put("ACTION_MODE", "UPDATE");
					} else {
						insertList.add(row);
						historyMap.put("ACTION_MODE", "INSERT");
					}
	                
					if("1".equals(row.get("APPLY_ALL"))){
						Map<String, String> tmpRow = new HashMap<String, String>(row);
						tmpRow.remove("GRID_ID");
						
						Map<String, String> overGridHistoryMap = new HashMap<String, String>(tmpRow);
						history.add(overGridHistoryMap);
						
						historyMap.put("ACTION_MODE", "UPDATE");
						historyMap.put("GRID_ID", "*");
					}
				}
				
				if (insertList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsertData");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(insertList, true);
				}
				if (updateList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdateData");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(updateList, true);
				}

				doInsertHistory(ctx, history, "SGRID");
				
				//SLANG
				insertList = new ArrayList<Map<String, String>>(gridData.size());
				updateList = new ArrayList<Map<String, String>>(gridData.size() * 2);
				history = new ArrayList<Map<String, String>>(gridData.size());
				
				List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, ctx);
				for(Map<String, String> row : gridData){
					for (Map<String, String> languageMap : languageList) {
	    				String language = languageMap.get("LANGUAGE");
	                	Map<String, String> rowLangMap = new HashMap<String, String>(row);
	                	rowLangMap.put("LANGUAGE" , language);
	                	rowLangMap.put("CONTENTS" , row.get("CONTENTS_"+language));
	                	rowLangMap.put("COL_WIDTH", row.get("COL_WIDTH_"+language));
	                	
		                //SLANG
		                sxp = new SepoaXmlParser(this, "doQueryExistRowSLANG");
		                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		                sf = new SepoaFormater(ssm.doSelect(rowLangMap));
		                
		                Map<String, String> historyMap = new HashMap<String, String>(rowLangMap);
						history.add(historyMap);
		                
		                boolean existRow = sf.getValue(0, 0).equals("1")?true:false;
		                if (existRow) {
							updateList.add(rowLangMap);
							historyMap.put("ACTION_MODE", "UPDATE");
						} else {
							insertList.add(rowLangMap);
							historyMap.put("ACTION_MODE", "INSERT");
						}
					}
				}
				
				if (insertList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsertDataSLANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(insertList, true);
				}
				if (updateList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdateDataSLANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(updateList, true);
				}

				doInsertHistory(ctx, history, "SLANG");
        	}
			
            Cache.grid.removeAll();
            Cache.text.removeAll();
            Commit();
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
    
    @Description("히스토리 저장")
	private synchronized void doInsertHistory(ConnectionContext ctx, List<Map<String, String>> list, String type) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> keyMap = new SingletonMap<String, String>("HISTORY_KEY", String.valueOf(System.nanoTime()));
			sxp = new SepoaXmlParser(this, "doInsert"+type+"History");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(list, keyMap, true);
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
