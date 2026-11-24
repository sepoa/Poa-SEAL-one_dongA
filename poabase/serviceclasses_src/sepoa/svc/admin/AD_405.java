package sepoa.svc.admin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import sepoa.fw.auth.encrypt.SepoaAuthEncryptManager;
import sepoa.fw.cfg.Configuration;
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

@Description("클라이언트관리 - 기본 설정")
public class AD_405 extends SepoaService {

	public AD_405(String opt, SepoaInfo info) throws SepoaServiceException {
		super("TRANSACTION", info);
	}
	
	@SuppressWarnings("unchecked")
	@Description("폼 조회")
	public SepoaOut doQueryForm(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = null;
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			
			ctx = getClientCTX(headerData.get("CLIENT_CODE"));
			
			if(ctx == null) {
				throw new Exception("Client ConnectionContext is null");
			}
			
			sxp = new SepoaXmlParser(this, "doQueryForm");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			SepoaFormater masterSF = new SepoaFormater(ssm.doSelect(headerData));
            Map<String, String> masterSFMap = new HashMap<String, String>();
            if(masterSF.getRowCount() > 0) {
            	masterSFMap = new SepoaFormater(ssm.doSelect(headerData)).getListMap().get(0);
            } else {
            	masterSFMap.put("CLIENT_CODE", headerData.get("CLIENT_CODE"));
            	masterSFMap.put("COMPANY_CODE", headerData.get("COMPANY_CODE"));
            }
            
            Configuration config = null;
            if("000".equals(headerData.get("CLIENT_CODE"))) {
            	config = new Configuration("000");
            } else {
            	config = new Configuration(headerData.get("CLIENT_CODE"));
            }
            
            if(headerData.get("APP_NAME") == null) {
            	masterSFMap.put("APP_NAME", config.getString("sepoa.app.name", "#NO_DATA#"));
            } else {
            	masterSFMap.put("APP_NAME", headerData.get("APP_NAME"));
            }

            String vendorFinder = "sepoa.app." + masterSFMap.get("APP_NAME").toLowerCase() + ".datasource";
            Matcher m = Pattern.compile("jdbc/poa-([a-zA-Z]{0,})-datasource").matcher(config.get(vendorFinder, ""));
            
            if(m.find()) {
            	masterSFMap.put("APP_DB_VENDOR", "#NO_DATA#");
            	if(!m.group(1).isEmpty()) {
            		masterSFMap.put("APP_DB_VENDOR", m.group(1).toUpperCase());
            	}
            }
            
            Configuration masterConfig = new Configuration("000");
            Set<String> matchedKeys = masterConfig.getMatchedKey(".*.house_code");
            
            for(String key : matchedKeys) {
            	if(masterConfig.getString(key, "").equals(headerData.get("CLIENT_CODE"))) {
            		masterSFMap.put("SERVICE_URL", key.replaceAll(".house_code", ""));
            		break;
            	}
            }
            
            setValue(new SepoaFormater(masterSFMap).getString());
            
		} catch (Exception e) {
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("폼 저장")
	@Permission(key="WORK_TYPE", value="Z")
	public SepoaOut doSaveForm(Map<String, Object> allData) {
		
		setStatus(1);
		setFlag(true);
		
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			
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
			
			Map<String, Object> copyTargetData = new HashMap<String, Object>();
			String copyClientCode = headerData.get("COPY_CLIENT_CODE");
			if(!StringUtils.isBlank(copyClientCode)) {
				ConnectionContext copyCTX = getClientCTX(copyClientCode);
	            
	            if(copyCTX == null) {
	            	throw new Exception("Copy ConnectionContext is null");
	            }
				
				sxp = new SepoaXmlParser(this, "doQueryUCONF");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, copyCTX, sxp);
				copyTargetData.put("UCONF", new SepoaFormater(ssm.doSelect(headerData)).getListMap());
				
				sxp = new SepoaXmlParser(this, "doQueryUPUSO");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, copyCTX, sxp);
				copyTargetData.put("UPUSO", new SepoaFormater(ssm.doSelect(headerData)).getListMap());
				
				sxp = new SepoaXmlParser(this, "doQuerySFFDT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, copyCTX, sxp);
				copyTargetData.put("SFFDT", new SepoaFormater(ssm.doSelect(headerData)).getListMap());
				
				sxp = new SepoaXmlParser(this, "doQuerySGRFT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, copyCTX, sxp);
				copyTargetData.put("SGRFT", new SepoaFormater(ssm.doSelect(headerData)).getListMap());
				
				sxp = new SepoaXmlParser(this, "doQuerySMUFP");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, copyCTX, sxp);
				copyTargetData.put("SMUFP", new SepoaFormater(ssm.doSelect(headerData)).getListMap());
				
				sxp = new SepoaXmlParser(this, "doQuerySCMAM");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, copyCTX, sxp);
				copyTargetData.put("SCMAM", new SepoaFormater(ssm.doSelect(headerData)).getListMap());
				
				sxp = new SepoaXmlParser(this, "doQuerySCODE");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, copyCTX, sxp);
				copyTargetData.put("SCODE", new SepoaFormater(ssm.doSelect(headerData)).getListMap());
				
				sxp = new SepoaXmlParser(this, "doQuerySLANG");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, copyCTX, sxp);
				copyTargetData.put("SLANG", new SepoaFormater(ssm.doSelect(headerData)).getListMap());
			}
			
			for(ConnectionContext ctx : ctxList) {
				sxp = new SepoaXmlParser(this, "doUpdateForm");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				int count = ssm.doUpdate(headerData);
				
				if(count == 0) {
					sxp = new SepoaXmlParser(this, "doInsertForm");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(headerData);
					
					//Create Default Admin
					Map<String, String> admin = new HashMap<String, String>();
					admin.put("HOUSE_CODE", headerData.get("CLIENT_CODE"));
					admin.put("USER_ID", "ADMIN");
					admin.put("PASSWORD", SepoaAuthEncryptManager.getEncrypted("admin1234"));
					admin.put("USER_NAME", "관리자");
					admin.put("MENU_PROFILE_CODE", CommonUtil.getConfig("sepoa.defult.admin.profile"));
					
					sxp = new SepoaXmlParser(this, "doCreateAdmin");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(admin);
					
					if(!StringUtils.isBlank(headerData.get("COPY_CLIENT_CODE"))) {
						copyClientData(ctx, headerData.get("CLIENT_CODE"), copyTargetData);
					}
				}
			}
			
			List<String> masterConfigList = new ArrayList<String>();
			List<String> subConfigList = new ArrayList<String>();
			
			String configFilePath = "";
			if("000".equals(headerData.get("CLIENT_CODE"))) {
				configFilePath = System.getProperty("sepoa.config.file");
			} else {
				String parent = new File(System.getProperty("sepoa.config.file")).getParent();
				configFilePath = new File(parent + "/" + "sepoa."+headerData.get("CLIENT_CODE")+".properties").getPath();
				File file = new File(configFilePath);
				
				if(!file.exists()) {
					file.createNewFile();
				}
				
				file = null;
				
				masterConfigList.add(headerData.get("SERVICE_URL")+".house_code = "+headerData.get("CLIENT_CODE"));
				masterConfigList.add("sepoa."+headerData.get("CLIENT_CODE")+".config.file = " + configFilePath.replaceAll("\\\\", "/"));
			}
			
			subConfigList.add("sepoa.app.use=true");
			subConfigList.add("sepoa.app.name=" + headerData.get("APP_NAME"));
			subConfigList.add("sepoa.app."+headerData.get("APP_NAME").toLowerCase()+".datasource=jdbc/poa-" + headerData.get("APP_DB_VENDOR").toLowerCase() + "-datasource");
			
			subConfigList.add("sepoa.service.root.CBO=$config{sepoa.work.base}/poasrm_cbo/"+headerData.get("CLIENT_CODE")+"/CBO-Package/");
			subConfigList.add("sepoa.attach.upload.CBO=$config{sepoa.work.base}/poasrm_cbo/"+headerData.get("CLIENT_CODE")+"/CBO-Package/design/");
			subConfigList.add("sepoa.attach.download.CBO=$config{sepoa.work.base}/poasrm_cbo/"+headerData.get("CLIENT_CODE")+"/CBO-Package/design/");
			
			configurationWriter(System.getProperty("sepoa.config.file"), masterConfigList, false); // Master Config File Write
			configurationWriter(configFilePath, subConfigList, false); // Sub Config File Write
			
			Commit();
		} catch (Exception e) {
			Rollback();
			setMessage(e.getMessage());
			setStatus(0);
			setFlag(false);
			System.err.println(e);
			Logger.err.println(e);
			e.printStackTrace();
		}
		
		return getSepoaOut();
	}
	
	/**
	 * Configuration Writer
	 * @param writeFilePath
	 * @param sourceData
	 * @param isAppend
	 * @throws Exception
	 */
	private void configurationWriter(String writeFilePath, List<String> sourceData, boolean isAppend) throws Exception {
		List<String> writeConfigList = FileUtils.readLines(new File(writeFilePath));
		List<String> sourceChecker = new ArrayList<>(sourceData);
		
		for(int i = 0; i < writeConfigList.size(); i++) {
			for(String item : sourceData) {
				String orgKey = writeConfigList.get(i);
				String key = item.split("=")[0];
				if(orgKey.startsWith(key)) {
					writeConfigList.remove(i);
					writeConfigList.add(i, item);
					sourceChecker.remove(item);
					break;
				}
			}
		}
		
		for(String item : sourceChecker) {
			writeConfigList.add(item);
		}
		
		FileUtils.writeLines(new File(writeFilePath), writeConfigList, isAppend);
	}
	
	/**
	 * Copy Client (Don't commit in this method)
	 * @param ctx
	 * @param headerData
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void copyClientData(ConnectionContext ctx, String newClientCode, Map<String, Object> targetData) throws Exception {
		
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		Map<String, String> updateMap = new HashMap<String, String>();
		updateMap.put("HOUSE_CODE", newClientCode);
		
		// UCONF - 디자인설정, 알림설정----------------------------------
		List<Map<String, String>> uconfList = (List<Map<String, String>>) targetData.get("UCONF");
		if(uconfList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsertUCONF");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doInsert(uconfList, updateMap, true);
		}
		// ---------------------------------------------------------------
		
		
		
		// UPUSO - 알림템플릿---------------------------------------------
		List<Map<String, String>> upusoList = (List<Map<String, String>>) targetData.get("UPUSO");
		if(upusoList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsertUPUSO");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doInsert(upusoList, updateMap, true);
		}
		// ---------------------------------------------------------------
		
		
		
		// SFFDT - 폼설정-------------------------------------------------
		List<Map<String, String>> sffdtList = (List<Map<String, String>>) targetData.get("SFFDT");
		if(sffdtList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsertSFFDT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doInsert(sffdtList, updateMap, true);
		}
		// ---------------------------------------------------------------
		
		
		
		// SGRFT - 그리드설정---------------------------------------------
		List<Map<String, String>> sgrftList = (List<Map<String, String>>) targetData.get("SGRFT");
		if(sgrftList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsertSGRFT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doInsert(sgrftList, updateMap, true);
		}
		// ---------------------------------------------------------------
		
		
		
		// SMUFP - 메뉴설정-----------------------------------------------
		List<Map<String, String>> smufpList = (List<Map<String, String>>) targetData.get("SMUFP");
		if(smufpList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsertSMUFP");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doInsert(smufpList, updateMap, true);
		}
		// ---------------------------------------------------------------
		
		
		
		// SCMAM - 모듈설정-----------------------------------------------
		List<Map<String, String>> scmamList = (List<Map<String, String>>) targetData.get("SCMAM");
		if(scmamList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsertSCMAM");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doInsert(scmamList, updateMap, true);
		}
		// ---------------------------------------------------------------
		
		
		
		
		// SCODE - 코드관리-----------------------------------------------
		List<Map<String, String>> scodeList = (List<Map<String,String>>) targetData.get("SCODE");
		if(scodeList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsertSCODE");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doInsert(scodeList, updateMap, true);
		}
		// ---------------------------------------------------------------
		
		
		
		
		// SLANG - 다국어관리---------------------------------------------
		List<Map<String, String>> slangList = (List<Map<String,String>>) targetData.get("SLANG");
		if(slangList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doInsertSLANG");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(false);
			ssm.doInsert(slangList, updateMap, true);
		}
		// ---------------------------------------------------------------
	}
	
	private ConnectionContext getClientCTX(String clientCode) {
		ConnectionContext ctx = null;
		
		try {
			Configuration config = new Configuration(clientCode);
			String appName = config.getString("sepoa.app.name", "TOMCAT");
			String dataSource = config.get("sepoa.app." + appName.toLowerCase() + ".datasource");
			
			if(StringUtils.isBlank(dataSource)) {
				throw new Exception("sepoa.app." + appName.toLowerCase() + ".datasource not found");
			}
			
			Matcher m = Pattern.compile("jdbc/poa-([a-zA-Z]{0,})-datasource").matcher(dataSource);
	        if(m.find()) {
	        	if(!m.group(1).isEmpty()) {
	        		String jndi = m.group(1).toLowerCase();
        			joinTransaction(jndi);
	        		ctx = getConnectionContext(jndi);
	        	}
	        }
		} catch (Exception e) {
			Logger.err.println(this, e);
		}
		
		return ctx;
	}
}
