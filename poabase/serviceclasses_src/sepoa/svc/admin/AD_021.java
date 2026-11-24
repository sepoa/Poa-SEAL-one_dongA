package sepoa.svc.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svc.common.COMMON;

@Description("메뉴관리")
@Permission(key="WORK_TYPE", value="Z")
public class AD_021 extends SepoaService {

	public AD_021(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	@SuppressWarnings("unchecked")
	@Description("프로파일조회")
	public SepoaOut doQueryProfile(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);

			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

			List<Map<String, String>> dataList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, ctx);
			for (Map<String, String> langMap : languageList) {
				for (Map<String, String> data : dataList) {
					Map<String, String> _tmpMap = new HashMap<String, String>();
					_tmpMap.put("screen_id", "MENU_PROFILE");
					_tmpMap.put("code", data.get("MENU_PROFILE_CODE"));

					sxp = new SepoaXmlParser(this, "doQuery_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					SepoaFormater sf = new SepoaFormater(ssm.doSelect(langMap, _tmpMap));
					if (sf.getRowCount() > 0) {
						data.put("CONTENTS_" + langMap.get("LANGUAGE"), sf.getValue("CONTENTS", 0));
					} else {
						data.put("CONTENTS_" + langMap.get("LANGUAGE"), "");
					}
				}
			}
			setValue(dataList);
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
	@Description("메뉴전체조회")
	public SepoaOut doQueryMenuData(Map<String, Object> allData) {

		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

			String profile_no = MapUtils.getString(headerData, "profile_no", "");

			if ("".equals(profile_no)) {
				headerData.put("profile_no", info.getSession("MENU_PROFILE_CODE"));
			}
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
	@Description("메뉴관리용 데이터 조회")
	public SepoaOut doQueryMenuMgtData(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);

			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

			List<Map<String, String>> dataList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, ctx);
			for (Map<String, String> langMap : languageList) {
				for (Map<String, String> data : dataList) {
					Map<String, String> _tmpMap = new HashMap<String, String>();
					_tmpMap.put("screen_id", "MENU_" + data.get("MENU_PROFILE_CODE"));
					_tmpMap.put("code", data.get("MENU_ID"));

					sxp = new SepoaXmlParser(this, "doQuery_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					SepoaFormater sf = new SepoaFormater(ssm.doSelect(langMap, _tmpMap));
					if (sf.getRowCount() > 0) {
						data.put("CONTENTS_" + langMap.get("LANGUAGE"), sf.getValue("CONTENTS", 0));
					} else {
						data.put("CONTENTS_" + langMap.get("LANGUAGE"), "");
					}
				}
			}
			setValue(dataList);
			setValue(languageList);
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
	@Description("다국어 조회")
	public SepoaOut doQuerySlangFromMenuID(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);

			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> languageList = COMMON.getSystemActiveLanguage(info, ctx);

			Map<String, String> data = new HashMap<String, String>();
			List<Map<String, String>> resultList = Collections.singletonList(data);
			for (Map<String, String> langMap : languageList) {
				sxp = new SepoaXmlParser();
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				SepoaFormater sf = new SepoaFormater(ssm.doSelect(langMap, headerData));

				if (sf.getRowCount() > 0) {
					data.put("CONTENTS_" + langMap.get("LANGUAGE"), sf.getValue("CONTENTS", 0));
				} else {
					data.put("CONTENTS_" + langMap.get("LANGUAGE"), "");
				}
			}

			setValue(new SepoaFormater(resultList).getString());
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
	@Description("프로파일삭제")
	public SepoaOut doProfileDELETE(Map<String, Object> allData) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			setStatus(1);
			setFlag(true);
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

			String profileCode = headerData.get("menu_profile_code");
			if (profileCode == null || profileCode.isEmpty()) {
				throw new Exception("not found profile code");
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
				List<Map<String, String>> sysLangList = COMMON.getSystemActiveLanguage(info, ctx);
				List<Map<String, String>> languageList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> menuList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> langHistory = new ArrayList<Map<String, String>>();
				
				for(Map<String, String> sysLangMap : sysLangList) {
					Map<String, String> profileLangData = new HashMap<String, String>();
					profileLangData.put("screen_id", "MENU_PROFILE");
					profileLangData.put("code", profileCode);
					profileLangData.put("LANGUAGE", sysLangMap.get("LANGUAGE"));
					profileLangData.put("CONTENTS", headerData.get("contents_" + sysLangMap.get("LANGUAGE").toLowerCase()));
					profileLangData.put("ACTION_MODE", "DELETE");
					languageList.add(profileLangData);
					langHistory.add(profileLangData);
					
					Map<String, String> menuLangData = new HashMap<String, String>();
					menuLangData.put("screen_id", "MENU_" + profileCode);
					menuLangData.put("code", "");
					menuLangData.put("ACTION_MODE", "DELETE");
					menuList.add(menuLangData);
				}

				sxp = new SepoaXmlParser(this, "doDelete_LANG");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(languageList, true);
				
				sxp = new SepoaXmlParser(this, "doDelete_LANG");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(menuList, true);
				
				headerData.put("ACTION_MODE", "DELETE");
				headerData.put("menu_id", "0");
				headerData.put("parent_menu_id", "0");
				sxp = new SepoaXmlParser(this, "doDelete_MUPD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(headerData);
				
				doInsertHistory(ctx, langHistory, "SLANG");
				doInsertHistory(ctx, headerData, "SMUPD");
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
	@Description("프로파일명 업데이트")
	public SepoaOut doProfileMODIFY(Map<String, Object> allData) {
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
			
			String profileCode = headerData.get("menu_profile_code");

			for(ConnectionContext ctx : ctxList) {
				List<Map<String, String>> sysLangList = COMMON.getSystemActiveLanguage(info, ctx);
				List<Map<String, String>> langHistoryList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> insertLangList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> updateLangList = new ArrayList<Map<String, String>>();
				
				for(Map<String, String> sysLangMap : sysLangList) {
					Map<String, String> langMap = new HashMap<String, String>();
					langMap.put("code", profileCode);
					langMap.put("screen_id", "MENU_PROFILE");
					
					String language = sysLangMap.get("LANGUAGE");
					langMap.put("LANGUAGE", language);
					
					sxp = new SepoaXmlParser(this, "doQuery_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

					Map<String, String> beforeMap = new HashMap<String, String>();
					
					for (Map<String, String> map : new SepoaFormater(ssm.doSelect(langMap)).getListMap(true)) {
						beforeMap.put(language, map.get("CONTENTS"));
					}
					
					langMap.put("CONTENTS", headerData.get("contents_" + language.toLowerCase()));
					
					
					if(beforeMap.get(language) == null) {
						langMap.put("ACTION_MODE", "INSERT");
						insertLangList.add(langMap);
						langHistoryList.add(langMap);
					} else if(!headerData.get("contents_" + language.toLowerCase()).equals(beforeMap.get(language))) {
						langMap.put("ACTION_MODE", "UPDATE");
						updateLangList.add(langMap);
						langHistoryList.add(langMap);
					}
				}

				if(insertLangList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsert_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(insertLangList, true);
				}
				
				if(updateLangList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdate_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(updateLangList, true);
				}
				
				if(langHistoryList.size() > 0) {
					doInsertHistory(ctx, langHistoryList, "SLANG");
				}
			}

			Commit();
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
	@Description("프로파일 생성")
	public SepoaOut doProfileNEW(Map<String, Object> allData) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			String profileCode = DocumentUtil.getDocNumber(info, "MUPD").result[0];

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
				List<Map<String, String>> sysLangList = COMMON.getSystemActiveLanguage(info, ctx);
				List<Map<String, String>> languageList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> langHistory = new ArrayList<Map<String, String>>();
				
				// SLANG, SMUPD Begin
				for (Map<String, String> sysLangMap : sysLangList) {
					Map<String, String> _tmpMap = new HashMap<String, String>();
					_tmpMap.put("screen_id", "MENU_PROFILE");
					_tmpMap.put("code", profileCode);
					_tmpMap.put("LANGUAGE", sysLangMap.get("LANGUAGE"));
					_tmpMap.put("CONTENTS", headerData.get("contents_" + sysLangMap.get("LANGUAGE").toLowerCase()));
					_tmpMap.put("ACTION_MODE", "INSERT");
					languageList.add(_tmpMap);
					langHistory.add(_tmpMap);
				}
				
				Map<String, String> mupdMap = new HashMap<String, String>();
				mupdMap.put("company_code", headerData.get("new_company_code"));
				mupdMap.put("menu_profile_code", profileCode);
				mupdMap.put("menu_id", DocumentUtil.getDocNumber(info, "MUID").result[0]);
				mupdMap.put("parent_menu_id","0");
				mupdMap.put("item_seq","0");
				mupdMap.put("ACTION_MODE", "INSERT");
				
				if(languageList.size() > 0) {
					// SLANG INSERT
					sxp = new SepoaXmlParser(this, "doInsert_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(languageList, true);
					doInsertHistory(ctx, langHistory, "SLANG");
					
					// SMUPD INSERT
					sxp = new SepoaXmlParser(this, "doInsert_MUPD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(mupdMap);
					doInsertHistory(ctx, mupdMap, "SMUPD");
				}
			}
			
			Commit();
			
			addParams("menu_profile_code", profileCode);
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
	@Description("프로파일 복제")
	public SepoaOut doProfileSAVEAS(Map<String, Object> allData) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			setStatus(1);
			setFlag(true);
			SepoaOut soruceData = doQueryMenuMgtData(allData);
			List<Map<String, String>> sourceList = (List<Map<String, String>>) soruceData.getData(0);
			List<Map<String, String>> sysLangList = (List<Map<String, String>>) soruceData.getData(1);

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

			String profileCode = DocumentUtil.getDocNumber(info, "MUPD").result[0];
			
			for(ConnectionContext ctx : ctxList) {
				List<Map<String, String>> langList = new LinkedList<Map<String, String>>();
				List<Map<String, String>> mupdList = new ArrayList<Map<String, String>>(sourceList.size());
				
				for (Map<String, String> sysLangMap : sysLangList) {
					Map<String, String> _tmpMap = new HashMap<String, String>();
					_tmpMap.put("screen_id", "MENU_PROFILE");
					_tmpMap.put("code", profileCode);
					_tmpMap.put("LANGUAGE", sysLangMap.get("LANGUAGE"));
					_tmpMap.put("CONTENTS", headerData.get("contents_" + sysLangMap.get("LANGUAGE").toLowerCase()));
					_tmpMap.put("ACTION_MODE", "INSERT");
					langList.add(_tmpMap);
				}
				
				Map<String, String> newMuidMatchInfo = new HashMap<String, String>();
				newMuidMatchInfo.put("0", "0");
				
				for (Map<String, String> sourceMap : sourceList) {
					Map<String, String> _tmpMupd = new HashMap<String, String>();
					mupdList.add(_tmpMupd);
					for (Entry<String, String> entry : sourceMap.entrySet()) {
						_tmpMupd.put(entry.getKey().toLowerCase(), entry.getValue());
					}
					String screenID = _tmpMupd.get("screen_id");
					if(screenID != null && !screenID.isEmpty()) {
						_tmpMupd.put("menu_img", "");
					}
					
					_tmpMupd.put("menu_profile_code", profileCode);
					_tmpMupd.put("company_code", headerData.get("new_company_code"));
					_tmpMupd.put("ACTION_MODE", "INSERT");

					String beforeMenuId = _tmpMupd.get("menu_id");
					if (!newMuidMatchInfo.containsKey(beforeMenuId)) {
						newMuidMatchInfo.put(beforeMenuId, DocumentUtil.getDocNumberRemote(info, "MUID").result[0]);
					}
					_tmpMupd.put("menu_id", newMuidMatchInfo.get(beforeMenuId));

					String beforeParentMenuId = _tmpMupd.get("parent_menu_id");
					if (!newMuidMatchInfo.containsKey(beforeParentMenuId)) {
						newMuidMatchInfo.put(beforeParentMenuId, DocumentUtil.getDocNumber(info, "MUID").result[0]);
					}
					_tmpMupd.put("parent_menu_id", newMuidMatchInfo.get(beforeParentMenuId));

					Map<String, String> _tmpLangBase = new HashMap<String, String>();
					_tmpLangBase.put("screen_id", "MENU_" + profileCode);
					_tmpLangBase.put("code", _tmpMupd.get("menu_id"));
					for (Map<String, String> sysLangMap : sysLangList) {
						Map<String, String> _tmpLang = new HashMap<String, String>(_tmpLangBase);
						_tmpLang.put("LANGUAGE", sysLangMap.get("LANGUAGE"));
						_tmpLang.put("CONTENTS", _tmpMupd.get("contents_" + sysLangMap.get("LANGUAGE").toLowerCase()));
						_tmpLang.put("ACTION_MODE", "INSERT");
						langList.add(_tmpLang);
					}
				}
				
				if(langList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsert_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(langList, true);
					
					doInsertHistory(ctx, langList, "SLANG");
				}
				
				if(mupdList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsert_MUPD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(mupdList);
					
					doInsertHistory(ctx, mupdList, "SMUPD");
				}
			}

			Commit();
			addParams("PROFILE_CODE", profileCode);
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
	@Description("메뉴노드 업데이트")
	public SepoaOut doUpdateTreeNode(Map<String, Object> allData) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		String menuID = null;
		try {
			setStatus(1);
			setFlag(true);

			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			SepoaFormater sf = null;
			
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
				Map<String, String> muidMap = new HashMap<String, String>(headerData);

				String screenID = muidMap.get("screen_id");
				if (screenID != null && !screenID.isEmpty()) {
					sxp = new SepoaXmlParser(this, "doQuery_MUID");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(muidMap));
					if (sf.getRowCount() > 0) {
						muidMap.put("ACTION_MODE", "UPDATE");
						sxp = new SepoaXmlParser(this, "doUpdate_MUID");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(muidMap);
					} else {
						muidMap.put("ACTION_MODE", "INSERT");
						sxp = new SepoaXmlParser(this, "doInsert_MUID");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(muidMap);
					}
					
					doInsertHistory(ctx, muidMap, "SMUID");
				}
				
				Map<String, String> mupdMap = new HashMap<String, String>(headerData);
				if(StringUtils.isNotBlank(muidMap.get("screen_id"))){
					mupdMap.put("menu_img", "");
				}
				
				menuID = mupdMap.get("menu_id");
				if (menuID == null || menuID.isEmpty()) {
					menuID = DocumentUtil.getDocNumber(info, "MUID").result[0];
					mupdMap.put("menu_id", menuID);
					headerData.put("menu_id", menuID);
				}
				
				sxp = new SepoaXmlParser(this, "doQuery_MUPD");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(mupdMap));
				if (sf.getRowCount() > 0) {
					mupdMap.put("ACTION_MODE", "UPDATE");
					sxp = new SepoaXmlParser(this, "doUpdate_MUPD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(mupdMap);
				} else {
					mupdMap.put("ACTION_MODE", "INSERT");
					sxp = new SepoaXmlParser(this, "doInsert_MUPD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(mupdMap);
				}
				
				doInsertHistory(ctx, mupdMap, "SMUPD");
				
				Map<String, String> langMap = new HashMap<String, String>();
				langMap.put("code", headerData.get("menu_id"));
				langMap.put("screen_id", "MENU_" + headerData.get("menu_profile_code"));
	
				sxp = new SepoaXmlParser(this, "doQuery_LANG");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(langMap));
				
				Map<String, String> beforeMap = new HashMap<String, String>();
				for (Map<String, String> map : sf.getListMap(true)) {
					beforeMap.put(map.get("LANGUAGE"), map.get("CONTENTS"));
				}
				
				List<Map<String, String>> insertLangList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> updateLangList = new ArrayList<Map<String, String>>();
				List<Map<String, String>> historyLangList = new ArrayList<Map<String, String>>();
				for(Entry<String, String> entry : headerData.entrySet()) {
					String key = entry.getKey();
					
					if (key.startsWith("contents_")) {
						String lang = key.substring(9).toUpperCase();
						String contents = entry.getValue();
						
						Map<String, String> _tmpMap = new HashMap<String, String>(langMap);
						_tmpMap.put("LANGUAGE", lang);
						_tmpMap.put("CONTENTS", contents);
						_tmpMap.put("ACTION_MODE", "UPDATE");
						
						historyLangList.add(_tmpMap);

						if (beforeMap.get(lang) == null) {
							_tmpMap.put("ACTION_MODE", "INSERT");
							insertLangList.add(_tmpMap);
						} else if (!entry.getValue().equals(beforeMap.get(lang))) {
							_tmpMap.put("ACTION_MODE", "UPDATE");
							updateLangList.add(_tmpMap);
						}
					}						
				}
				
				if(insertLangList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doInsert_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(insertLangList, true);
				}
				
				if(updateLangList.size() > 0) {
					sxp = new SepoaXmlParser(this, "doUpdate_LANG");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(updateLangList);
				}
				
				doInsertHistory(ctx, historyLangList, "SLANG");
			}
			
			Commit();

			Map<String, String> returnMap = new HashMap<String, String>();
			returnMap.put("returnType", "param");
			returnMap.put("menu_id", menuID);
			setObject(returnMap);
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
	@Description("메뉴노드 삭제")
	public SepoaOut doDeleteTreeNode(Map<String, Object> allData) {
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
			
			headerData.remove("menu_id");
			
			for(ConnectionContext ctx : ctxList) {
				String[] menuIdArray = headerData.get("menu_id_list").split("____");
				List<Map<String, String>> gridData = new ArrayList<Map<String, String>>(menuIdArray.length);
				
				String companyCode = headerData.get("company_code");
				String menuProfileCode = headerData.get("menu_profile_code");
				String parentMenuId = headerData.get("parent_menu_id");
				for (String menuId : menuIdArray) {
					Map<String, String> gridMap = new HashMap<String, String>();
					gridMap.put("menu_id", menuId);
					gridMap.put("company_code", companyCode);
					gridMap.put("menu_profile_code", menuProfileCode);
					gridMap.put("parent_menu_id", parentMenuId);
					gridMap.put("ACTION_MODE", "DELETE");
					gridData.add(gridMap);
				}
				
				sxp = new SepoaXmlParser(this, "doDeleteTreeNode");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(gridData, true);
				
				doInsertHistory(ctx, gridData, "SMUPD");
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
	@Description("autocomplete - 화면정보")
	public SepoaOut getScreenComboList(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, String> bindData = new HashMap<String, String>();
			String mask = headerData.get("mask");
			if (mask != null) {
				mask = mask.replaceAll("\\*", "%");
				mask = mask.replaceAll("\\?", "_");
				bindData.put("MASK", mask);
			} else {
				bindData.put("VALUE", headerData.get("default"));
			}

			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);

			setValue(ssm.doSelect(bindData));
			setStatus(1);
			setMessage(Cache.text.get("MESSAGE.0001"));
		} catch (Exception e) {
			setStatus(0);
			setMessage(Cache.text.get("MESSAGE.1002"));
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}

		return getSepoaOut();
	}

	@SuppressWarnings("unchecked")
	@Description("노드 순번 저장")
	public SepoaOut doUpdateTreeIndex(Map<String, Object> allData) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
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
			
			String menuProfileCode = headerData.get("menu_profile_code");
			String parentMenuId = headerData.get("menu_id");
			
			for(Map<String, String> item : gridData) {
				item.put("ACTION_MODE", "UPDATE");
				item.put("menu_profile_code", menuProfileCode);
				item.put("parent_menu_id", parentMenuId);
				item.put("company_code", item.get("COMPANY_CODE"));
				item.put("menu_id", item.get("MENU_ID"));
				item.put("item_seq", item.get("ITEM_SEQ"));
			}
			
			for(ConnectionContext ctx : ctxList) {
				sxp = new SepoaXmlParser(this, "doUpdateTreeIndex");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(gridData, true);
				
				doInsertHistory(ctx, gridData, "SMUPD");
			}
			
			Commit();

			setStatus(1);
			setMessage(Cache.text.get("MESSAGE.0001"));
		} catch (Exception e) {
			setStatus(0);
			setMessage(Cache.text.get("MESSAGE.1002"));
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
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
