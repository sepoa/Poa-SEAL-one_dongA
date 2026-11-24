package sepoa.svc.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;

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
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svc.common.COMMON;

@SuppressWarnings({ "unchecked" })
@Description("코드관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_014 extends SepoaService {

	public AD_014(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("코드리스트조회")
	public SepoaOut doListCode(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
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

	@Description("모듈리스트조회")
	public SepoaOut doListModule(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
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

	@Description("헤더조회")
	public SepoaOut doQueryHeader(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			List<Map<String, String>> result = null;
			Map<String, Map<String, String>> dataMap = new HashMap<String, Map<String, String>>(); 
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> languageMap = COMMON.getSystemActiveLanguage(info, ctx);
			for (Map<String, String> language : languageMap) {
				sxp = new SepoaXmlParser();
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

				if (result == null) {
					result = new SepoaFormater(ssm.doSelect(headerData, language)).getListMap(true);
					for(Map<String, String> tmp : result) {
						dataMap.put(tmp.get("CODE"), tmp);
					}
				} else {
					List<Map<String, String>> tmpList = new SepoaFormater(ssm.doSelect(headerData, language)).getListMap(true);
					for(Map<String, String> tmpMap : tmpList) {
						Map<String, String> target = dataMap.get(tmpMap.get("CODE"));
						for(Entry<String, String> entry : tmpMap.entrySet()) {
							if(!target.containsKey(entry.getKey())) {
								target.put(entry.getKey(), entry.getValue());
							}
						}
					}
				}
			}
			setValue(new SepoaFormater(result).getString());
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

	@Description("상세조회")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doQueryCodeDetail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			List<Map<String, String>> result = null;
			Map<String, Map<String, String>> dataMap = new HashMap<String, Map<String, String>>();

			List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, ctx);
			
			SepoaFormater dummy = null;
			
			for (Map<String, String> language : languageList) {
				sxp = new SepoaXmlParser();
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

				if (result == null) {
					dummy = new SepoaFormater(ssm.doSelect(headerData, language));
					result = dummy.getListMap(true);
					for(Map<String, String> tmp : result) {
						dataMap.put(tmp.get("CODE"), tmp);
					}
				} else {
					List<Map<String, String>> tmpList = new SepoaFormater(ssm.doSelect(headerData, language)).getListMap(true);
					for(Map<String, String> tmpMap : tmpList) {
						Map<String, String> target = dataMap.get(tmpMap.get("CODE"));
						for(Entry<String, String> entry : tmpMap.entrySet()) {
							if(!target.containsKey(entry.getKey())) {
								target.put(entry.getKey(), entry.getValue());
							}
						}
					}
				}
			}

			if (result != null) {
				dummy = new SepoaFormater(result);
			}

			setValue(dummy);
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

	@Description("코드삭제")
	public SepoaOut doDeleteCode(Map<String, Object> allData) {

		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);

			sxp = new SepoaXmlParser(this, "doDeleteCodeHeader");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(headerData);

			sxp = new SepoaXmlParser(this, "doDeleteCodeDetail");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(headerData);
			Commit();
			Cache.combo.removeAll();
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

	@Description("상세삭제")
	public SepoaOut doDeleteCodeDetail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);

			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(gridData, headerData);
			Commit();
			Cache.combo.removeAll();
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

	@Description("저장")
	public SepoaOut doSaveCode(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, ctx);

			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_RAW_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData,SepoaDataMapper.KEY_GRID_DATA);

			for (Map<String, String> languageMap : languageList) {
				String language = languageMap.get("LANGUAGE");

				Map<String, String> tmpHeader = new HashMap<String, String>();
				tmpHeader.put("LANGUAGE", language);
				tmpHeader.put("TEXT1"   , headerData.get("TEXT1_"+language));
				
				if("S000".equals(headerData.get("MODULE_TYPE"))) {
					tmpHeader.put("TEXT3"   , headerData.get("TEXT3_"+language));
					tmpHeader.put("TEXT4"   , headerData.get("TEXT4_"+language));
				}
				
				sxp = new SepoaXmlParser(this, "doCheckHeader");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData, tmpHeader));
				if (sf.getDouble("CNT", 0) > 0) {
					sxp = new SepoaXmlParser(this, "doUpdateHeader");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(headerData, tmpHeader);
				} else {
					sxp = new SepoaXmlParser(this, "doInsertHeader");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(headerData, tmpHeader);
				}

				if (gridData != null) {
					Map<String, String> tmpRow = new HashMap<String, String>();
					tmpRow.put("TYPE", headerData.get("TYPE"));
					
					for (Map<String, String> map : gridData) {
						tmpRow.put("LANGUAGE", language);
						
						tmpRow.put("TEXT1", map.get("TEXT1_" + language));
						tmpRow.put("TEXT2", map.get("TEXT2_" + language));
						tmpRow.put("TEXT3", map.get("TEXT3_" + language));
						tmpRow.put("TEXT4", map.get("TEXT4_" + language));
						tmpRow.put("TEXT5", map.get("TEXT5_" + language));
						tmpRow.put("TEXT6", map.get("TEXT6_" + language));
						tmpRow.put("TEXT7", map.get("TEXT7_" + language));

						sxp = new SepoaXmlParser(this, "doCheckDetail");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						sf = new SepoaFormater(ssm.doSelect(map, tmpRow));
						boolean existRow = sf.getValue(0, 0).equals("1") ? true : false;
						if (existRow) {
							sxp = new SepoaXmlParser(this, "doUpdateDetail");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.doUpdate(map, tmpRow);
						} else {
							sxp = new SepoaXmlParser(this, "doInsertDetail");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.doInsert(map, tmpRow);
						}
					}
				}
			}
			Cache.combo.removeAll();
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
}