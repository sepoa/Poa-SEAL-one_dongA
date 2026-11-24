package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
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

@Description("알림 관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_450 extends SepoaService {

	public AD_450(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
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
	@Description("상세 조회")
	public SepoaOut doQuery(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser(this, "doSelectUPUSH");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			List<Map<String, String>> pushDataList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			if(pushDataList.size() > 0) {
				Map<String, String> resultData = null;
				
				for(Map<String, String> item : pushDataList) {
					if(item.get("CODE").equals(headerData.get("CODE"))) {
						resultData = new HashMap<String, String>();
						resultData.putAll(item);
					}
				}
				
				if(resultData == null) {
					throw new Exception("not found data");
				}
				
				sxp = new SepoaXmlParser(this, "doSelectUPUSO");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				List<Map<String, String>> soData = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
				for(Map<String, String> row : soData) {
					resultData.put(row.get("TYPE")+"_"+row.get("OPTION_NAME"), row.get("OPTION_VALUE"));
				}
				List<Map<String, String>> result = new ArrayList<Map<String, String>>(1);
				result.add(resultData);
				setValue(new SepoaFormater(result).getString());
			}else {
				throw new Exception("not found data");
			}
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
			
			for(ConnectionContext ctx : ctxList) {
				headerData.put("ACTION_MODE", "DELETE");
				sxp = new SepoaXmlParser(this, "doDeleteUPUSH");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);
				doInsertHistory(ctx, headerData, "UPUSH");
				
				headerData.put("ACTION_MODE", "INSERT");
				sxp = new SepoaXmlParser(this, "doInsertUPUSH");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(headerData);
				doInsertHistory(ctx, headerData, "UPUSH");
				
				sxp = new SepoaXmlParser(this, "doSelectUPUSO");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				List<Map<String, String>> pusoList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
				List<Map<String, String>> deleteHistory = new LinkedList<Map<String, String>>();
				
				for(Map<String, String> puso : pusoList) {
					Map<String, String> deleteMap = new HashMap<String, String>(puso);
					deleteMap.put("ACTION_MODE", "DELETE");
					deleteHistory.add(deleteMap);
					
					sxp = new SepoaXmlParser(this, "doDeleteUPUSO");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doDelete(puso);
				}
				
				if(deleteHistory.size() > 0) {
					doInsertHistory(ctx, deleteHistory, "UPUSO");
				}

				List<Map<String, String>> gridData = new LinkedList<Map<String, String>>();
				List<Map<String, String>> insertHistory = new LinkedList<Map<String, String>>();
				for (Entry<String, String> entry : headerData.entrySet()) {
					String key = entry.getKey();
					String value = null;

					for (String type : new String[] { "DATASOURCE", "MAIL", "KAKAO", "SMS" }) {
						if (key.startsWith(type + "_")) {
							value = entry.getValue();
							key = key.substring(type.length() + 1);
							Map<String, String> row = new HashMap<String, String>();
							
							row.put("CODE", headerData.get("CODE"));
							row.put("ACTION_MODE", "INSERT");
							row.put("TYPE", type);
							row.put("OPTION_NAME", key);
							row.put("OPTION_VALUE", value);
							
							gridData.add(row);
							insertHistory.add(row);
						}
					}
				}

				if (gridData.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsertUPUSO");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(gridData, headerData, true);
					
					doInsertHistory(ctx, insertHistory, "UPUSO"); // INSERT HISTORY
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
			Map<String, String> condition = new SingletonMap<String, String>("DEL_FLAG", "Y");
			
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
				Map<String, String> deleteMap = new HashMap<String, String>(headerData);
				sxp = new SepoaXmlParser(this, "doDelete");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(headerData, condition);
				
				deleteMap.put("DEL_FLAG", "Y");
				deleteMap.put("ACTION_MODE", "UPDATE");
				
				doInsertHistory(ctx, deleteMap, "UPUSH");
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
