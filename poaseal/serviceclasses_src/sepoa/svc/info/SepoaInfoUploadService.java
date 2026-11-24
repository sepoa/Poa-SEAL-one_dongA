package sepoa.svc.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.msg.Message;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@Description("기준정보등록")
public class SepoaInfoUploadService extends SepoaService {
	private Message msg;
	private String string;

	public SepoaInfoUploadService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
		msg = new Message(info, "DF_001");
	}
	
	@Description("사용자템플릿다운로드")
	public SepoaOut doUserExcelDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			List<Map<String, String>> tmpList = new ArrayList<>();
			Map<String, String> tmpMap = new HashMap<>();
			tmpMap.put("USER_ID", "");
			tmpMap.put("USER_NAME_LOC", "");
			tmpMap.put("USER_NAME_ENG", "");
			tmpMap.put("DEPT", "");
			tmpMap.put("POSITION", "");
			//tmpMap.put("POSITION_NAME", "");
			tmpMap.put("LANGUAGE", "");
			tmpMap.put("TIME_ZONE", "");
			tmpMap.put("MODEL_NAME", "");
			tmpMap.put("EMAIL", "");
			tmpMap.put("COUNTRY", "");
			//tmpMap.put("WORK_TYPE", "");
			tmpMap.put("MENU_PROFILE_CODE", "");
			tmpMap.put("DATE_FORMAT", "");
			tmpMap.put("COMPANY_CODE", "");
			tmpMap.put("EMPLOYEE_NO", "");
			tmpMap.put("PHONE_NO", "");
			tmpList.add(tmpMap);
			setValue(new SepoaFormater(tmpList));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}

	@Description("부서템플릿다운로드")
	public SepoaOut doDeptExcelDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			List<Map<String, String>> tmpList = new ArrayList<>();
			Map<String, String> tmpMap = new HashMap<>();
			tmpMap.put("DEPT", "");
			tmpMap.put("DEPT_NAME_LOC", "");
			tmpMap.put("DEPT_NAME_ENG", "");
			tmpMap.put("DEPT_NAME_JP", "");
			tmpMap.put("DEPT_NAME_CN", "");
			tmpMap.put("PARENT_DEPT", "");
			tmpMap.put("MANAGER_ID", "");
			tmpList.add(tmpMap);
			setValue(new SepoaFormater(tmpList));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@Description("직위템플릿다운로드")
	public SepoaOut doPositionExcelDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			List<Map<String, String>> tmpList = new ArrayList<>();
			Map<String, String> tmpMap = new HashMap<>();
			tmpMap.put("POSITION_CODE", "");
			tmpMap.put("POSITION_NAME", "");
			tmpMap.put("POSITION_ENG", "");
			tmpMap.put("POSITION_JP", "");
			tmpMap.put("POSITION_CN", "");
			tmpMap.put("SORT_SEQ", "");
			tmpList.add(tmpMap);
			setValue(new SepoaFormater(tmpList));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("사용자db저장")
	public SepoaOut doUserSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils
					.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils
					.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			List<Map<String, String>> tmpList = new ArrayList<>();

			String house_code = headerData.get("TARGET_HOUSE");
			String company_code = headerData.get("TARGET_COMPANY");
			
			for (Map<String, String> tmpData : gridData) {
				Map<String, String> tmpMap = new HashMap<>();
				tmpMap.put("ROW_NUM", tmpData.get("ROW_NUM"));

				tmpMap.put("HOUSE_CODE", house_code);
				tmpMap.put("COMPANY_CODE", tmpData.get("COMPANY_CODE"));
				
				String userID = tmpData.get("USER_ID");
				String changeString[] = { "$", "&" };
				changeString[0] = java.util.regex.Matcher.quoteReplacement("$");
				changeString[1] = java.util.regex.Matcher.quoteReplacement("&");
				userID = userID.replaceAll("&#36;", changeString[0]);
				userID = userID.replaceAll("&#38;", changeString[1]);
				String password = SepoaAuthEncryptManager.getEncrypted(userID);
				tmpMap.put("USER_ID", userID);
				tmpMap.put("PASSWORD", password);
				System.out.println("USER_ID: "+userID);
				System.out.println("PASSWORD: "+password);
				
				tmpMap.put("USER_NAME_LOC", tmpData.get("USER_NAME_LOC"));
				tmpMap.put("USER_NAME_ENG", tmpData.get("USER_NAME_ENG"));
				tmpMap.put("DEPT", tmpData.get("DEPT"));
				tmpMap.put("POSITION", tmpData.get("POSITION"));
				tmpMap.put("POSITION_NAME", tmpData.get("POSITION_NAME"));
				tmpMap.put("LANGUAGE", tmpData.get("LANGUAGE"));
				tmpMap.put("TIME_ZONE", tmpData.get("TIME_ZONE"));
				tmpMap.put("MODEL_NAME", tmpData.get("MODEL_NAME"));
				tmpMap.put("EMAIL", tmpData.get("EMAIL"));
				tmpMap.put("COUNTRY", tmpData.get("COUNTRY"));
				tmpMap.put("WORK_TYPE", tmpData.get("WORK_TYPE"));
				tmpMap.put("MENU_PROFILE_CODE", tmpData.get("MENU_PROFILE_CODE"));
				tmpMap.put("DATE_FORMAT", tmpData.get("DATE_FORMAT"));
				tmpMap.put("EMPLOYEE_NO", tmpData.get("EMPLOYEE_NO"));
				tmpMap.put("PHONE_NO", tmpData.get("PHONE_NO"));

				sxp = new SepoaXmlParser(this, "doQueryUser");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(tmpData, tmpMap));

				if (Integer.parseInt(sf.getValue("USMT_CNT", 0)) > 0) {
					try {
						sxp = new SepoaXmlParser(this, "updateUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(tmpData, tmpMap);
						
						tmpMap.put("STATUS", Cache.text.get("DF_001.UPDATE_SUCCESS"));
						tmpList.add(tmpMap);
					} catch(Exception e) {
						tmpMap.put("STATUS", Cache.text.get("DF_001.ERROR"));
						tmpMap.put("ERR_MSG", e.getMessage());
						//e.printStackTrace();
						Logger.err.println(info.getSession("ID"), this, e.getMessage());
					} finally {
						tmpList.add(tmpMap);
					}
				} else {
					try {
						sxp = new SepoaXmlParser(this, "insertUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(tmpData, tmpMap);
						
						tmpMap.put("STATUS", Cache.text.get("DF_001.INSERT_SUCCESS"));
						tmpMap.put("ERR_MSG", "");
					} catch(Exception e) {
						tmpMap.put("STATUS", Cache.text.get("DF_001.ERROR"));
						tmpMap.put("ERR_MSG", e.getMessage());
						//e.printStackTrace();
						Logger.err.println(info.getSession("ID"), this, e.getMessage());
					} finally {
						tmpList.add(tmpMap);
					}
				}
				
				if (Integer.parseInt(sf.getValue("ADDR_CNT", 0)) > 0) {
					try {
						sxp = new SepoaXmlParser(this, "updateADDR");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(tmpData, tmpMap);
					} catch(Exception e) {
						//e.printStackTrace();
						Logger.err.println(info.getSession("ID"), this, e.getMessage());
					}
				} else {
					try {
						sxp = new SepoaXmlParser(this, "insertADDR");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(tmpData, tmpMap);
					} catch(Exception e) {
						//e.printStackTrace();
						Logger.err.println(info.getSession("ID"), this, e.getMessage());
					}				
				}
				
			}
			Commit();
			
			JSONArray jsonArray = new JSONArray();
			for (Map<String, String> map : tmpList) {
				JSONObject json = new JSONObject();
				for (Map.Entry<String, String> entry : map.entrySet()) {
					json.put(entry.getKey(), entry.getValue());
				}
				jsonArray.add(json);
			}
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put("returnType", "param");
			returnMap.put("params", jsonArray.toString());
			
			setObject(returnMap);
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}

		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("부서db저장")
	public SepoaOut doDeptSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils
					.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils
					.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			List<Map<String, String>> tmpList = new ArrayList<>();

			String house_code = headerData.get("TARGET_HOUSE");
			String company_code = headerData.get("TARGET_COMPANY");
			
			for (Map<String, String> tmpData : gridData) {
				Map<String, String> tmpMap = new HashMap<>();
				tmpMap.put("ROW_NUM", tmpData.get("ROW_NUM"));

				tmpMap.put("HOUSE_CODE", house_code);
				tmpMap.put("COMPANY_CODE", tmpData.get("COMPANY_CODE"));
				tmpMap.put("DEPT", tmpData.get("DEPT"));
				tmpMap.put("DEPT_NAME_LOC", tmpData.get("DEPT_NAME_LOC"));
				tmpMap.put("DEPT_NAME_ENG", tmpData.get("DEPT_NAME_ENG"));
				tmpMap.put("DEPT_NAME_JP", tmpData.get("DEPT_NAME_JP"));
				tmpMap.put("DEPT_NAME_CN", tmpData.get("DEPT_NAME_CN"));
				tmpMap.put("PARENT_DEPT", tmpData.get("PARENT_DEPT"));
				tmpMap.put("MANAGER_ID", tmpData.get("MANAGER_ID"));

				sxp = new SepoaXmlParser(this, "doQueryDept");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(tmpData, tmpMap));

				if (Integer.parseInt(sf.getValue("CNT", 0)) > 0) {
					try {
					sxp = new SepoaXmlParser(this, "updateDEPT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doUpdate(tmpData, tmpMap);
					
					tmpMap.put("STATUS", Cache.text.get("DF_001.UPDATE_SUCCESS"));
					tmpList.add(tmpMap);
					} catch(Exception e) {
						tmpMap.put("STATUS", Cache.text.get("DF_001.ERROR"));
						tmpMap.put("ERR_MSG", e.getMessage());
						//e.printStackTrace();
						Logger.err.println(info.getSession("ID"), this, e.getMessage());
					} finally {
						tmpList.add(tmpMap);
					}
				} else {
					try {
						sxp = new SepoaXmlParser(this, "insertDEPT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(tmpData, tmpMap);
						
						tmpMap.put("STATUS", Cache.text.get("DF_001.INSERT_SUCCESS"));
						tmpMap.put("ERR_MSG", "");
					} catch(Exception e) {
						tmpMap.put("STATUS", Cache.text.get("DF_001.ERROR"));
						tmpMap.put("ERR_MSG", e.getMessage());
						//e.printStackTrace();
						Logger.err.println(info.getSession("ID"), this, e.getMessage());
					} finally {
						tmpList.add(tmpMap);
					}
				}
			}
			Commit();
			
			JSONArray jsonArray = new JSONArray();
			for (Map<String, String> map : tmpList) {
				JSONObject json = new JSONObject();
				for (Map.Entry<String, String> entry : map.entrySet()) {
					json.put(entry.getKey(), entry.getValue());
				}
				jsonArray.add(json);
			}
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put("returnType", "param");
			returnMap.put("params", jsonArray.toString());
			
			setObject(returnMap);
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}

		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("직위db저장")
	public SepoaOut doPositionSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils
					.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils
					.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			List<Map<String, String>> tmpList = new ArrayList<>();

			String house_code = headerData.get("TARGET_HOUSE");
			String company_code = headerData.get("TARGET_COMPANY");
			
			for (Map<String, String> tmpData : gridData) {
				Map<String, String> tmpMap = new HashMap<>();
				tmpMap.put("ROW_NUM", tmpData.get("ROW_NUM"));

				tmpMap.put("HOUSE_CODE", house_code);
				tmpMap.put("COMPANY_CODE", tmpData.get("COMPANY_CODE"));
				tmpMap.put("POSITION_CODE", tmpData.get("POSITION_CODE"));
				tmpMap.put("SORT_SEQ", tmpData.get("SORT_SEQ"));
				String[] langs = {"KO", "EN", "JA", "ZH"};
				for (String lang : langs) {
					if (lang == "KO") {
						if (!StringUtils.isEmpty(tmpData.get("POSITION_NAME_LOC"))) {
							tmpMap.put("POSITION_NAME", tmpData.get("POSITION_NAME_LOC"));
						} else {
							continue;
						}
					} else if (lang == "EN") {
						if (!StringUtils.isEmpty(tmpData.get("POSITION_NAME_ENG"))) {
							tmpMap.put("POSITION_NAME", tmpData.get("POSITION_NAME_ENG"));
						} else {
							continue;
						}
					} else if (lang == "JA") {
						if (!StringUtils.isEmpty(tmpData.get("POSITION_NAME_JP"))) {
							tmpMap.put("POSITION_NAME", tmpData.get("POSITION_NAME_JP"));
						} else {
							continue;
						}
					} else if (lang == "ZH") {
						if (!StringUtils.isEmpty(tmpData.get("POSITION_NAME_CN"))) {
							tmpMap.put("POSITION_NAME", tmpData.get("POSITION_NAME_CN"));
						} else {
							continue;
						}
					}
					tmpMap.put("LANGUAGE", lang);
					
					sxp = new SepoaXmlParser(this, "doQueryPosition");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(tmpData, tmpMap));
						
					if (Integer.parseInt(sf.getValue("CNT", 0)) > 0) {
						try {
							sxp = new SepoaXmlParser(this, "updateCODE");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.doUpdate(tmpData, tmpMap);
							
							tmpMap.put("STATUS", Cache.text.get("DF_001.UPDATE_SUCCESS"));
							tmpList.add(tmpMap);
						} catch(Exception e) {
							tmpMap.put("STATUS", Cache.text.get("DF_001.ERROR"));
							tmpMap.put("ERR_MSG", e.getMessage());
							//e.printStackTrace();
							Logger.err.println(info.getSession("ID"), this, e.getMessage());
						} finally {
							tmpList.add(tmpMap);
						}
					} else {
						try {
							sxp = new SepoaXmlParser(this, "insertCODE");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.doInsert(tmpData, tmpMap);
							
							tmpMap.put("STATUS", Cache.text.get("DF_001.INSERT_SUCCESS"));
							tmpMap.put("ERR_MSG", "");
						} catch(Exception e) {
							tmpMap.put("STATUS", Cache.text.get("DF_001.ERROR"));
							tmpMap.put("ERR_MSG", e.getMessage());
							//e.printStackTrace();
							Logger.err.println(info.getSession("ID"), this, e.getMessage());
						} finally {
							tmpList.add(tmpMap);
						}
					}
				}
			}
			Commit();
			
			JSONArray jsonArray = new JSONArray();
			for (Map<String, String> map : tmpList) {
				JSONObject json = new JSONObject();
				for (Map.Entry<String, String> entry : map.entrySet()) {
					json.put(entry.getKey(), entry.getValue());
				}
				jsonArray.add(json);
			}
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put("returnType", "param");
			returnMap.put("params", jsonArray.toString());
			
			setObject(returnMap);
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}

		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("부서장검색")
	public SepoaOut searchUser(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "searchUser");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("부서명검색")
	public SepoaOut searchDept(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "searchDept");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("직위명검색")
	public SepoaOut searchPosition(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "searchPosition");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("사용자리스트검색")
	public SepoaOut searchUserList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		JSONArray jsonArray = new JSONArray();;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			String house_code = headerData.get("HOUSE_CODE");
			String[] company_codes = headerData.get("COMPANY_CODES").split(",");
			String[] depts = headerData.get("DEPTS").split(",");
			String[] positions = headerData.get("POSITIONS").split(",");
			
			for (int i=0; i<company_codes.length; i++) {
				Map<String, String> tmpMap = new HashMap<>();
				tmpMap.put("HOUSE_CODE", house_code);
				tmpMap.put("COMPANY_CODE", company_codes[i]);
				tmpMap.put("DEPT_CODE", depts[i]);
				tmpMap.put("POSITION_CODE", positions[i]);
				
				JSONObject json = new JSONObject();
				
				sxp = new SepoaXmlParser(this, "searchDept");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(tmpMap));
				json.put("DEPT_NAME_LOC", sf.getValue("DEPT_NAME_LOC"));
				json.put("DEPT_NAME_ENG", sf.getValue("DEPT_NAME_ENG"));
				json.put("DEPT_NAME_JP", sf.getValue("DEPT_NAME_JP"));
				json.put("DEPT_NAME_CN", sf.getValue("DEPT_NAME_CN"));
				
				sxp = new SepoaXmlParser(this, "searchPosition");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(tmpMap));
				json.put("POSITION_NAME_LOC", sf.getValue("POSITION_NAME_LOC"));
				json.put("POSITION_NAME_ENG", sf.getValue("POSITION_NAME_ENG"));
				json.put("POSITION_NAME_JP", sf.getValue("POSITION_NAME_JP"));
				json.put("POSITION_NAME_CN", sf.getValue("POSITION_NAME_CN"));
				jsonArray.add(json);
			}			
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put("returnType", "param");
			returnMap.put("params", jsonArray.toString());
			
			setObject(returnMap);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("부서리스트검색")
	public SepoaOut searchDeptList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		JSONArray jsonArray = new JSONArray();;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			String house_code = headerData.get("HOUSE_CODE");
			String[] company_codes = headerData.get("COMPANY_CODES").split(",");
			String[] parent_depts = headerData.get("PARENT_DEPTS").split(",");
			String[] manager_ids = headerData.get("MANAGER_IDS").split(",");
			
			for (int i=0; i<company_codes.length; i++) {
				Map<String, String> tmpMap = new HashMap<>();
				tmpMap.put("HOUSE_CODE", house_code);
				tmpMap.put("COMPANY_CODE", company_codes[i]);
				tmpMap.put("DEPT_CODE", parent_depts[i]);
				tmpMap.put("USER_ID", manager_ids[i]);
				
				JSONObject json = new JSONObject();
				
				sxp = new SepoaXmlParser(this, "searchDept");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(tmpMap));
				json.put("PARENT_DEPT_NAME_LOC", sf.getValue("DEPT_NAME_LOC"));
				json.put("PARENT_DEPT_NAME_ENG", sf.getValue("DEPT_NAME_ENG"));
				json.put("PARENT_DEPT_NAME_JP", sf.getValue("DEPT_NAME_JP"));
				json.put("PARENT_DEPT_NAME_CN", sf.getValue("DEPT_NAME_CN"));
				
				sxp = new SepoaXmlParser(this, "searchUser");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(tmpMap));
				json.put("MANAGER_NAME_LOC", sf.getValue("USER_NAME_LOC"));
				json.put("MANAGER_NAME_ENG", sf.getValue("USER_NAME_ENG"));
				jsonArray.add(json);
			}			
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put("returnType", "param");
			returnMap.put("params", jsonArray.toString());
			
			setObject(returnMap);
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
}
