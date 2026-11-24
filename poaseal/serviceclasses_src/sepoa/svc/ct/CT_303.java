package sepoa.svc.ct;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Base64;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.Permission;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자계약 - 거래처 관리 - Write")
public class CT_303 extends SepoaService {
	
	public CT_303(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@SuppressWarnings("unchecked")
	@Description("거래처 저장")
	public SepoaOut doSave(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) MapUtils.getObject(allData, SepoaModuleDataMapper.KEY_CONFIGURATION, null);
			if(config == null) {
				config = new Configuration();
			}
			
			Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
			if(!StringUtils.isBlank(headerData.get("DATA"))) {
				Map<String, Object> data = (Map<String, Object>) jsonData.get("DATA");
				Map<String, String> infoData = (Map<String, String>) MapUtils.getMap(data, "INFO");
				if(StringUtils.isBlank(MapUtils.getString(infoData, "IF_FLAG", ""))) {
					infoData.put("IF_FLAG", "N");
				}
				
				Map<String, String> defaultData = new HashMap<>();
				
				String defaultMenuProfile = config.get("sepoa.supplier.default.profile_code");
				String defaultLanguage = config.get("sepoa.user.default.language", "KO");
				String defaultTimeZone = config.get("sepoa.user.default.time_zone", "GMT9");
				String defaultDateFormat = config.get("sepoa.user.default.date_format", "%Y/%m/%d");
				String defaultCountry = config.get("sepoa.user.default.country", "KR");
				defaultData.put("MENU_PROFILE_CODE", defaultMenuProfile);
				defaultData.put("LANGUAGE", defaultLanguage);
				defaultData.put("TIME_ZONE", defaultTimeZone);
				defaultData.put("DATE_FORMAT", defaultDateFormat);
				defaultData.put("COUNTRY", defaultCountry);
				
				if(StringUtils.isBlank(infoData.get("BIZ_CODE"))) {
					String bizCode = DocumentUtil.getDocNumber(info, "BI").result[0];
					infoData.put("BIZ_CODE", bizCode);
					
					sxp = new SepoaXmlParser(this, "doInsertSBUPR");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					
					ssm.doInsert(infoData);
					
					Map<String, String> extra = new HashMap<>();
					extra.putAll(defaultData);
					extra.put("USER_ID", bizCode);
					extra.put("USER_NAME", infoData.get("SELLER_NAME_LOC"));
					extra.put("PASSWORD", getEncryptPassword(MapUtils.getString(infoData, "IRS_NO", "1111111119").replaceAll("[^\\d]", "")));
					extra.put("SIGN_STATUS", "A");
					extra.put("WORK_TYPE", "G");
					extra.put("USER_TYPE", "S");
					
					sxp = new SepoaXmlParser(this, "doInsertSUSMT");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					
					ssm.doInsert(infoData, extra);
				} else {
					sxp = new SepoaXmlParser(this, "doUpdateSBUPR");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					
					ssm.doUpdate(infoData);
				}
				
				Map<String, Object> managerData = (Map<String, Object>) MapUtils.getMap(data, "MANAGER");
				if(managerData.containsKey("INFO")) {
					List<Map<String, String>> managerList = (List<Map<String, String>>) managerData.get("INFO");
					for(Map<String, String> manager : managerList) {
						sxp = new SepoaXmlParser(this, "doUpdateSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						
						ssm.doUpdate(infoData, manager);
					}
				}
				
				if(managerData.containsKey("NEW")) {
					List<Map<String, String>> managerList = (List<Map<String, String>>) managerData.get("NEW");
					for(Map<String, String> manager : managerList) {
						manager.put("SEQ", "__SEQ_IS_EMPTY__");
						
						sxp = new SepoaXmlParser(this, "doInsertSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						
						ssm.doInsert(infoData, manager);
						
						manager.putAll(defaultData);
						manager.put("PASSWORD", getEncryptPassword(MapUtils.getString(infoData, "IRS_NO", "1111111119").replaceAll("[^\\d]", "")));
						manager.put("SIGN_STATUS", "A");
						manager.put("WORK_TYPE", "G");
						manager.put("USER_TYPE", "S");
						
						sxp = new SepoaXmlParser(this, "doInsertSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						
						ssm.doInsert(infoData, manager);
					}
				}
				
				if(managerData.containsKey("DELETE")) {
					List<Map<String, String>> managerList = (List<Map<String, String>>) managerData.get("DELETE");
					for(Map<String, String> manager : managerList) {
						sxp = new SepoaXmlParser(this, "doDeleteSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						
						ssm.doUpdate(infoData, manager);
						
						sxp = new SepoaXmlParser(this, "doDeleteSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						
						ssm.doUpdate(infoData, manager);
					}
				}
			}
			
			Commit();
			
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("거래처 삭제")
	public SepoaOut doDelete(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
			if(!StringUtils.isBlank(headerData.get("DATA"))) {
				Map<String, Object> data = (Map<String, Object>) jsonData.get("DATA");
				Map<String, String> infoData = (Map<String, String>) MapUtils.getMap(data, "INFO");
				
				sxp = new SepoaXmlParser(this, "doDeleteSBUPR");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				
				ssm.doUpdate(infoData);
				
				Map<String, Object> managerData = (Map<String, Object>) MapUtils.getMap(data, "MANAGER");
				if(managerData.containsKey("INFO")) {
					List<Map<String, String>> managerInfo = (List<Map<String, String>>) managerData.get("INFO");
					for(Map<String, String> manager : managerInfo) {
						sxp = new SepoaXmlParser(this, "doDeleteSBUPI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						
						ssm.doUpdate(infoData, manager);
						
						sxp = new SepoaXmlParser(this, "doDeleteSUSMT");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						
						ssm.doUpdate(manager);
					}
				}
			}
			
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("사용자 계정 확인")
	public SepoaOut doCheckUserId(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			sxp = new SepoaXmlParser(this, "doQuerySUSMT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			setValue(sf.getRowCount() == 0);
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	private String getEncryptPassword(String password) {
		String result = "";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(password.getBytes("UTF-8"));
			
			StringBuffer sb = new StringBuffer();
			byte[] dataArray = md.digest();
			for(byte data : dataArray) {
				sb.append(Integer.toString((data & 0xff) + 0x100, 16).substring(1));
			}
			
			result = Base64.base64Encode(sb.toString()).toUpperCase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
