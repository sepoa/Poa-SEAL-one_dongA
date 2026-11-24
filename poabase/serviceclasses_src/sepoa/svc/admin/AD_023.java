package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.fw.cache.Cache;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaConnectionInfo;
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

@SuppressWarnings({ "unused", "unchecked" })
@Description("그리드뷰 관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_023 extends SepoaService {

    public AD_023(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
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
    
    @SuppressWarnings({ "unchecked" })
    @Description("그리드 조회")
    public SepoaOut doList(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            sxp = new SepoaXmlParser(this, "doList");
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
    
    @Description("그리드뷰 아이템 조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
    	ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            setStatus(1);
            setFlag(true);

			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "getMasterGridData");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			List<Map<String, String>> masterGridList = new SepoaFormater(ssm.doSelect(headerData)).getListMap(true);
			
			sxp = new SepoaXmlParser(this, "getFilteredGridData");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			List<Map<String, String>> filterList = new SepoaFormater(ssm.doSelect(headerData)).getListMap(true);
			
			for(Map<String, String> mData : masterGridList) {
				Map<String, String> temp = new HashMap<>();
				for(String key : mData.keySet()) {
					if(key.equals("COL_VISIBLE")) continue;
					
					temp.put("ORG_"+key, mData.get(key));
				}
				mData.putAll(temp);
			}
			
			if(filterList.size() > 0) {
				Map<String, Map<String, String>> codeMap = new HashMap<>();
				for(Map<String, String> fData : filterList) {
					codeMap.put(fData.get("CODE"), fData);
				}
				
				for(Map<String, String> mData : masterGridList) {
					String code = mData.get("CODE");
					if(codeMap.containsKey(code)) {
						Map<String, String> fData = codeMap.get(code);
						for(Entry<String, String> entry : fData.entrySet()) {
							mData.put("ORG_"+entry.getKey(), mData.get(entry.getKey()));
							String value = entry.getValue();
							if(value != null && !value.isEmpty()) {
								mData.put(entry.getKey(), (!value.equals(mData.get(entry.getKey()))) ? value : mData.get(entry.getKey()));
							}
						}
						mData.put("COL_VISIBLE", "true");
					} else {
						mData.put("COL_VISIBLE", "false");
						mData.put("COL_WIDTH", mData.get("COL_WIDTH"));
						mData.put("NOT_NULL", "0");
					}
				}
			}
			
			setValue(new SepoaFormater(masterGridList).getString());
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
    @Description("그리드 뷰 저장")
    public SepoaOut doSave(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
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
			
			for(ConnectionContext ctx : ctxList) {
				// Delete Grid Filter Data
	            sxp = new SepoaXmlParser(this, "doDeleteSGRFT");
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            ssm.doDelete(headerData);
	            
	            List<Map<String, String>> langList = new ArrayList<Map<String, String>> (gridData.size());
	            for(Map<String, String> row : gridData) {
	            	for(Entry<String, String> attr : row.entrySet()) {
	            		String attrKey = attr.getKey();
	            		if(!attrKey.startsWith("ORG_") && row.containsKey("ORG_"+attrKey.toUpperCase())) {
	            			String mValue = row.get("ORG_" + attrKey);
	            			String cValue = row.get(attrKey);
	            			if(mValue.equals(cValue)) {
	            				attr.setValue("");
	            			} else {
	            				attr.setValue(cValue);
	            			}
	            		}
	            	}
	            	if(StringUtils.isNotEmpty(row.get("CONTENTS")) || StringUtils.isNotEmpty(row.get("COL_WIDTH"))) {
	            		langList.add(row);
	            	}
	            }
	            
	            if(gridData.size() > 0) {
	                // Insert SGRFT Data
	                sxp = new SepoaXmlParser(this, "doInsertSGRFT");
	                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                ssm.doInsert(gridData, headerData, true);
	                
	                if(langList.size() > 0) {
	                	// Insert SLANG Filter Data
	                	sxp = new SepoaXmlParser(this, "doDeleteSLANG");
	                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                    ssm.doDelete(langList, headerData);
	                    
	                    sxp = new SepoaXmlParser(this, "doInsertSLANG");
	                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                    ssm.doDelete(langList, headerData);
	                } else {
	                	List<Map<String, String>> deleteLangList = new ArrayList<Map<String, String>>();
	                	for(Map<String, String> row : gridData) {
	                		String cContents = MapUtils.getString(row, "CONTENTS", "");
	                		String cWidth = MapUtils.getString(row, "COL_WIDTH", "");
	                		
	                		if(StringUtils.isBlank(cContents) && StringUtils.isBlank(cWidth)) {
	                			deleteLangList.add(row);
	                		}
	                	}
	                	
	                	if(deleteLangList.size() > 0) {
                			sxp = new SepoaXmlParser(this, "doDeleteSLANG");
                			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    	                    ssm.doDelete(deleteLangList, headerData, true);
                		}
	                }
	            }
			}
            
			Commit();
            Cache.grid.removeAll();
            Cache.text.removeAll();
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
}