package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

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

@Description("폼 필터 관리")
public class AD_042_4 extends SepoaService {

	public AD_042_4(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
	@Description("다국어 탐색")
	public SepoaOut doQuerySLANGData(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doQueryMasterSLANG");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			List<Map<String, String>> masterSLANG = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			
			sxp = new SepoaXmlParser(this, "doQueryFilteredSLANG");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			List<Map<String, String>> filteredSLANG = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			
			for(Map<String, String> master : masterSLANG) {
				for(Map<String, String> filtered : filteredSLANG) {
					if(master.get("CODE").equals(filtered.get("CODE"))) {
						master.replace("CONTENTS", filtered.get("CONTENTS"));
						break;
					}
				}
			}
			
			setValue(new SepoaFormater(masterSLANG));
			
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("트리 그리드 생성")
	public SepoaOut doTreeGridQuery(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			String gridJsonData = StringEscapeUtils.unescapeHtml(headerData.get("GRID_DATA"));
			String languageJsonData = StringEscapeUtils.unescapeHtml(headerData.get("LANGUAGE_DATA"));
			
			List<Map<String, Object>> orgGridData = new Gson().fromJson(gridJsonData, List.class);
			Map<String, String> languageData = new Gson().fromJson(languageJsonData, Map.class);
			
			if(languageData == null) {
				languageData = new HashMap<String, String>();
			}
			
			List<Map<String, String>> processData = new ArrayList<Map<String, String>>();
			for(Map<String, Object> item : orgGridData) {
				Map<String, String> data = (Map<String, String>) item.get("value");
				
				String language = languageData.get(data.get("BLOCK_NAME"));
				if(language != null && !language.isEmpty()) {
					data.put("LABEL", language);
				} else {
					data.put("LABEL", "");
				}
				
				if(!data.containsKey("ORG_INPUT_WIDTH")) {
					data.put("ORG_INPUT_WIDTH", "");
				}
				
				if(!data.containsKey("INPUT_WIDTH")) {
					data.put("INPUT_WIDTH", "");
				} else {
					if(data.get("INPUT_WIDTH") == null || data.get("INPUT_WIDTH").isEmpty()) {
						data.put("INPUT_WIDTH", data.get("ORG_INPUT_WIDTH"));
					}
				}
				
				processData.add(data);
			}
			
			setValue(new SepoaFormater(processData));
		} catch (Exception e) {
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
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
		
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
			
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
				// Delete SFFDT Data
				sxp = new SepoaXmlParser(this, "doDeleteSFFDTData");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(gridData);
				
				// Extract SFFDT Data
				List<Map<String, String>> insertSFFDTData = new ArrayList<Map<String,String>>();
				for(Map<String, String> item : gridData) {
					if("1".equals(item.get("VISIBLE"))) {
						if(item.get("ORG_REQUIRED").equals(item.get("REQUIRED"))) {
							item.put("REQUIRED", item.get("ORG_REQUIRED"));
						}
						
						if(item.get("ORG_INPUT_WIDTH").equals(item.get("INPUT_WIDTH"))) {
							item.put("INPUT_WIDTH", item.get("ORG_INPUT_WIDTH"));
						}
						
						insertSFFDTData.add(item);
					}
				}
				
				// Insert SFFDT Data
				sxp = new SepoaXmlParser(this, "doInsertSFFDTData");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(insertSFFDTData);
				
				// Extract Master SLANG Data
				sxp = new SepoaXmlParser(this, "doQueryMasterSLANG");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				List<Map<String, String>> masterSLANGData = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
				
				for(Map<String, String> item : insertSFFDTData) {
					if(!"".equals(item.get("LABEL"))) {
						boolean process = false;
						for(Map<String, String> lang : masterSLANGData) {
							if(lang.get("CODE").equals(item.get("BLOCK_NAME"))) {
								if(!lang.get("CONTENTS").equals(item.get("LABEL"))) {
									if(!lang.get("HOUSE_CODE").equals(headerData.get("HOUSE_CODE"))){
										sxp = new SepoaXmlParser(this, "doInsertSLANG");
										ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
										ssm.doInsert(item);
									} else {		
										sxp = new SepoaXmlParser(this, "doUpdateSLANG");
										ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
										ssm.doInsert(item);
									}
								}
								process = true;
								break;
							}
						}
						if(!process) {
							sxp = new SepoaXmlParser(this, "doInsertSLANG");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							Map<String, String> houseCodeMap = new SingletonMap<String, String>("HOUSE_CODE", "000");
							ssm.doInsert(item, houseCodeMap);
						}
					} else if("".equals(item.get("LABEL"))) {
						item.put("LABEL", "");
						sxp = new SepoaXmlParser(this, "doUpdateSLANG");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(item);
					}
				}
			}
			
			Commit();
			Cache.form.remove(headerData.get("FORM_ID"));
			Cache.text.remove(headerData.get("FORM_ID"));
		} catch (Exception e) {
			Rollback();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
}
