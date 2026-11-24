package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.SingletonMap;
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

@Description("폼관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_041_2 extends SepoaService {

	public AD_041_2(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("삭제")
	public SepoaOut doDelete(Map<String, Object> allData) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);

			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
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
			
			headerData.put("ACTION_MODE", "DELETE");
			
			for(ConnectionContext ctx : ctxList) {
				sxp = new SepoaXmlParser(this, "doDelete");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);
				
				doInsertHistory(ctx, headerData, "SFORM");
			}
			
			Commit();
			Cache.form.removeAll();
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
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);
			
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
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
			
			headerData.put("FORM_SLANG", headerData.get("unescaped_form_slang"));
			
			for(ConnectionContext ctx : ctxList) {
				// SFORM Data Begin
				sxp = new SepoaXmlParser(this, "isExistForm");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
				
				boolean existForm = sf.getValue(0, 0).equals("1") ? true : false;
				if(existForm) {
					headerData.put("ACTION_MODE", "UPDATE");
					
					sxp = new SepoaXmlParser(this, "doUpdate");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(headerData);
				} else {
					headerData.put("ACTION_MODE", "INSERT");
					
					sxp = new SepoaXmlParser(this, "doInsert");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(headerData);
				}
				
				doInsertHistory(ctx, headerData, "SFORM");
				sf = null;
				// SFORM Data End
				
				// SLANG Data Begin
				List<Map<String, String>> langList = new Gson().fromJson(headerData.get("FORM_SLANG"), List.class);
				List<Map<String, String>> historyList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> updateList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> insertList = new ArrayList<Map<String, String>>();
				
				for(Map<String, String> lang : langList) {
					
					String formID = headerData.get("FORM_ID");
					lang.put("FORM_ID", formID);
					
					sxp = new SepoaXmlParser(this, "doQueryExistSLANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(lang));
					
					Map<String, String> historyMap = new HashMap<>(lang);
					historyMap.put("LANGUAGE", info.getSession("LANGUAGE"));
					
					historyList.add(historyMap);
					
					boolean existLang = sf.getValue(0, 0).equals("1") ? true : false;
					if(existLang) {
						updateList.add(lang);
						historyMap.put("ACTION_MODE", "UPDATE");
					} else {
						insertList.add(lang);
						historyMap.put("ACTION_MODE", "INSERT");
					}
				}
				
				if(insertList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsertSLANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(insertList, true);
				}
				if(updateList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdateSLANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(updateList, true);
				}
				
				doInsertHistory(ctx, historyList, "SLANG");
				// SLANG Data End
			}
			
			Cache.form.removeAll();
			Cache.text.removeAll();
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
	
	@SuppressWarnings("unchecked")
	@Description("히스토리 저장")
	private void doInsertHistory(ConnectionContext ctx, Object data, String type) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> keyMap = new SingletonMap<String, String>("HISTORY_KEY", String.valueOf(System.nanoTime()));
			sxp = new SepoaXmlParser(this, "doInsert"+type+"History");
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
