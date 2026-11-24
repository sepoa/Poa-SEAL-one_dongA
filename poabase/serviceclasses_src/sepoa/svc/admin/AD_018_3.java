package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import sepoa.svc.common.COMMON;

@Description("다국어")
@Permission(key="WORK_TYPE", value="Z")
public class AD_018_3 extends SepoaService {

	public AD_018_3(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("조회")
	public SepoaOut doQuery(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);

			SepoaFormater result = null;

			List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, ctx);
			for (Map<String, String> language : languageList) {
				Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
				sxp = new SepoaXmlParser();
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

				if (result == null) {
					result = new SepoaFormater(ssm.doSelect(headerData, language));
				} else {
					result.add(new SepoaFormater(ssm.doSelect(headerData, language)));
				}
			}

			if (result == null) {
				throw new Exception("not Found data");
			}

			setValue(result.getString());
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
			List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, getConnectionContext());
			
			//adjust
			for(Map<String, String> languageMap : languageList) {
				String language = languageMap.get("LANGUAGE");
				for(Map<String, String> row : gridData) {
					row.put("ACTION_MODE", "DELETE");
					row.put("LANGUAGE", language);
				}
			}
			for (ConnectionContext ctx : ctxList) {
				sxp = new SepoaXmlParser(this, "doDelete");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(gridData, true);
				
				doInsertHistory(ctx, gridData);
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

	@SuppressWarnings("unchecked")
	@Description("저장")
	public SepoaOut doSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, getConnectionContext());
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);

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

			// pivot
			List<Map<String, String>> pivotData = new ArrayList<Map<String, String>>(gridData.size() * languageList.size());
			for (Map<String, String> map : gridData) {
				for (Map<String, String> languageMap : languageList) {
					String language = languageMap.get("LANGUAGE");

					Map<String, String> data = new HashMap<String, String>(map);
					data.put("LANGUAGE", language);
					data.put("CONTENTS", map.get("CONTENTS_" + language));
					pivotData.add(data);
				}
			}

			for (ConnectionContext ctx : ctxList) {
				List<Map<String, String>> insertList = new ArrayList<Map<String, String>>(pivotData.size());
				List<Map<String, String>> updateList = new ArrayList<Map<String, String>>(pivotData.size());
				List<Map<String, String>> history = new ArrayList<Map<String, String>>(pivotData.size());
				for (Map<String, String> row : pivotData) {
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

				doInsertHistory(ctx, history);
			}

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

	@SuppressWarnings("unchecked")
	@Description("세션언어로저장")
	public SepoaOut doSaveSessionLang(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);

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

			// adjust
			for (Map<String, String> map : gridData) {
				map.put("LANGUAGE", info.getSession("LANGUAGE"));
			}

			for (ConnectionContext ctx : ctxList) {
				List<Map<String, String>> insertList = new ArrayList<Map<String, String>>(gridData.size());
				List<Map<String, String>> updateList = new ArrayList<Map<String, String>>(gridData.size());
				List<Map<String, String>> history = new ArrayList<Map<String, String>>(gridData.size());
				for (Map<String, String> row : gridData) {
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

				doInsertHistory(ctx, history);
			}

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
	private synchronized void doInsertHistory(ConnectionContext ctx, List<Map<String, String>> list) throws Exception {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> keyMap = new SingletonMap<String, String>("HISTORY_KEY", String.valueOf(System.nanoTime()));
			sxp = new SepoaXmlParser(this, "doInsertSLANGHistory");
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
