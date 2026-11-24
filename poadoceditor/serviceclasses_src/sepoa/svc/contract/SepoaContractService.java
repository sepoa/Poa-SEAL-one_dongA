package sepoa.svc.contract;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.contract.SepoaContractManager;
import sepoa.contract.SepoaContractTemplateManager;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.authority.Permission.PrincipalAgentType;
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;
import sepoa.svl.util.SepoaActionController;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자계약 - 모듈")
public class SepoaContractService extends SepoaService {
	public SepoaContractService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	protected String getClassName() {
		return this.getClass().getName();
	}
	
	protected SepoaContractManager getManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		String process = config.get("sepoa.contract.process", "");
		if(this.getClassName().endsWith("SepoaContractService")) {
			if("mk2".equalsIgnoreCase(process)) {
				return new sepoa.contractmk2.SepoaContractManager(info, config, ctx);
			} else {
				return new SepoaContractManager(info, config, ctx);
			}
		}else {
			if("mk2".equalsIgnoreCase(process)) {
				return new sepoa.contractmk2.SepoaContractTemplateManager(info, config, ctx);
			} else {
				return new SepoaContractTemplateManager(info, config, ctx);
			}
		}
	}
	
	@Description("화면 설정 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doGetScreenConfig(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			List<Map<String, String>> queryData = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			
			String type = headerData.get("TYPE");
			
			Map<String, Object> result = new HashMap<String, Object>();
			for(Map<String, String> row : queryData) {
				Map<String, Object> optionMap = new HashMap<String, Object>();
				result.put(row.get("CODE"), optionMap);
				
				optionMap.put("FILTER", Arrays.asList(row.get("TEXT5").split(",")).indexOf(type) >= 0);
			}
			
			setValue(new Gson().toJson(result));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("문서 리스트 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doDocumentList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			String screenType = headerData.get("TYPE");
			String querySufix = screenType;
			String statusQuery = null;
			int suffixIndex = screenType.indexOf('_');
			if(suffixIndex >= 0) {
				querySufix = screenType.substring(0, suffixIndex);
			}
			Map<String, String> condition = new HashMap<String, String>();
			condition.put("CONT_TYPE", "B");
			
			switch(screenType) {
			case "CONTRACT":
				condition.put("MODE_TYPE_OPERATOR", "not in");
				condition.put("MODE_TYPE_STATUS", "A,X");
				/*
				 * condition.put("MANUAL_FLAG_OPERATOR", "!=");
				 * condition.put("MANUAL_FLAG_VALUE", "S");
				 */
				condition.put("ORDER_COLUMN", "CONT_NO");
				condition.put("ORDER_SORT", "DESC");

				if(headerData.containsKey("F_STATUS")) {
					String status = headerData.get("F_STATUS");
					if(!status.isEmpty()) {
						condition.put("MODE_TYPE_OPERATOR", "=");
						condition.put("MODE_TYPE_STATUS", status);
					}
				}
				
				condition.put("EXPIRED_ALARM_FROM", SepoaDate.getShortDateString());
				condition.put("EXPIRED_ALARM_TO"  , SepoaDate.addSepoaDateMonth(SepoaDate.getShortDateString(), +1));
				break;
			case "CONTRACT_MANUAL":
				condition.put("MODE_TYPE_OPERATOR", "not in");
				condition.put("MODE_TYPE_STATUS", "A,X");
				condition.put("MANUAL_FLAG_OPERATOR", "=");
				condition.put("MANUAL_FLAG_VALUE", "S");
				
				condition.put("ORDER_COLUMN", "CONT_NO");
				condition.put("ORDER_SORT", "DESC");
				break;
			case "CONTRACT_MANUAL_SAVE":
				condition.put("MANUAL_FLAG_OPERATOR", "=");
				condition.put("MANUAL_FLAG_VALUE", "S");
				
				condition.put("ORDER_COLUMN", "CONT_NO");
				condition.put("ORDER_SORT", "DESC");
				break;
			case "CONTRACT_COMPLETE":
				condition.put("MODE_TYPE_OPERATOR", "in");
				condition.put("MODE_TYPE_STATUS", "A,X,F");
				
				condition.put("ORDER_COLUMN", "CONT_NO");
				condition.put("ORDER_SORT"  , "DESC");
				
				condition.put("EXPIRED_ALARM_FROM", SepoaDate.getShortDateString());
				condition.put("EXPIRED_ALARM_TO"  , SepoaDate.addSepoaDateMonth(SepoaDate.getShortDateString(), +1));
				break;
			case "APPROVAL_DRAFT":
				statusQuery = "AND CASE WHEN GWLN.APP_LINE_STATUS = 'L01' \r\n" + 
						"                    THEN CTGL.STATUS \r\n" + 
						"                    WHEN GWLN.APP_LINE_STATUS = 'L03'\r\n" + 
						"                    THEN 'R'\r\n" + 
						"                    ELSE GWLN.APP_LINE_STATUS \r\n" + 
						"                     END";
				condition.put("F_STATUS_QUERY", statusQuery);
				break;
			case "APPROVAL":
				statusQuery = "AND GWLN.APP_LINE_STATUS";
				condition.put("F_STATUS_QUERY", statusQuery);
				break;
			default:
				break;
			}

			sxp = new SepoaXmlParser(this, "doQuery_"+querySufix);
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			setValue(ssm.doSelect(condition, headerData));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("문서 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentDetail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				Map<String, Object> result = new HashMap<String, Object>();
				result.put("DOC_INFO_DATA", manager.getDocumentInfo());
				result.put("DOC_ITEM"     , manager.doSelectItem());
				result.put("SIGNER"       , manager.doSelectSigner());
				result.put("TERMINATOR"   , manager.doSelectTerminator());
				result.put("REVIEWER"     , manager.doSelectReviewer());
				
				setValue(result);
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("문서 확장 데이터 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentExtra(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setLinkFlag(false);
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				setValue(manager.doSelectItem());
				setValue(manager.doSelectSigner());
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("새 문서")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentCreate(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ"), true) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else if(!manager.doUpdateStatus("T")){
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				Rollback();
			}else if(!MapUtils.getBoolean(allData, SepoaModuleDataMapper.WITHOUT_COMMIT, false)){
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("문서 다운로드")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				Map<String, String> fileInfo = manager.doDocumentDownload();
				if(fileInfo == null) {
					setStatus(manager.getLastErrCode());
					setFlag(false);
				}else {
					setValue(fileInfo);
					addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("완전삭제")
	private SepoaOut doDocumentDeletePhysical(Map<String, Object> allData) {
		//physical delete
		//prohibited to use alone
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ"), true) != null) {
				if(!manager.deleteDocument(false)) {
					setStatus(manager.getLastErrCode());
					setFlag(false);
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("새 문서[I/F]")
	public SepoaOut doDocumentCreateIF(Map<String, Object> docData) {
		return doDocumentCreateWithData(docData, false);
	}
	
	@Description("새 문서[대량자동발송]")
	public SepoaOut doDocumentAutoSendList(List<Map<String, Object>> docList) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>(docList.size());
		for(Map<String, Object> docData : docList) {
			Map<String, Object> resultMap = new HashMap<String, Object>(docData);
			resultList.add(resultMap);
			SepoaOut so = doDocumentCreateWithData(docData, true);
			if(so.flag) {
				resultMap.put("RESULT_STATUS", "S");
			}else {
				resultMap.put("RESULT_STATUS", "E");
				resultMap.put("RESULT_MESSAGE", so.message);
			}
		}
		SepoaOut result = new SepoaOut();
		result.flag = true;
		result.status = 1;
		result.obj = resultList;
		return result; 
	}
	
	@Description("새 문서[자동발송]")
	public SepoaOut doDocumentAutoSend(Map<String, Object> docData) {
		return doDocumentCreateWithData(docData, true);
	}
	
	@SuppressWarnings("unchecked")
	@Description("새 문서[with Data]")
	public SepoaOut doDocumentCreateWithData(Map<String, Object> docData, boolean isAutoSend) {
		setStatus(1);
		setFlag(true);
		String tempFilePath = null;
		try {
			//check DOC_INFO_DATA
			if(!docData.containsKey("DOC_INFO_DATA")) {
				throw new Exception("not found DOC_INFO_DATA");
			}
			Map<String, String> docInfoData = (Map<String, String>) docData.get("DOC_INFO_DATA");
			
			Config config = new Configuration();
			
			//create dummy alldata
			Map<String, Object> allData = new HashMap<String, Object>();
			allData.put(SepoaModuleDataMapper.KEY_HEADER_DATA,docInfoData );
			allData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfoData);
			allData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, docData);
			allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			allData.put(SepoaModuleDataMapper.WITHOUT_COMMIT, true);
			
			PathManager pm = new PathManager(config);
			pm.setLayer(PathManager.LAYER_TEMP);
			tempFilePath = pm.getUniquePath();
			
			allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, tempFilePath);
			
			//validate
			if(StringUtils.isBlank(docInfoData.get("CONT_METHOD"))) {
				throw new Exception("not found CONT_METHOD");
			}
			
			if(StringUtils.isBlank(docInfoData.get("SIGN_ORDER"))) {
				throw new Exception("not found SIGN_ORDER");
			}
			
			if(StringUtils.isBlank(docInfoData.get("CONT_TYPE"))) {
				docInfoData.put("CONT_TYPE", "B");
			}
			
			//adjust docInfoData
			for(Entry<String, Object> entry : docData.entrySet()) {
				docInfoData.put(entry.getKey(), SepoaModuleDataMapper.IS_JSON_OBJECT);
			}
			
			//check CONT_NO
			String contractNo = null;
			String contractGlSeq = null;
			if(!docInfoData.containsKey("CONT_NO")) {
				contractNo = DocumentUtil.getDocNumber(info, "CT").result[0];
				contractGlSeq = "1";
				docInfoData.put("CONT_NO"    , contractNo);
				docInfoData.put("CONT_GL_SEQ", "1");
			}else {
				contractNo = docInfoData.get("CONT_NO");
				contractGlSeq = docInfoData.get("CONT_GL_SEQ");
				
				
				if(MapUtils.getBoolean(docData, "BEFORE_CONTRACT_DROP_FLAG", true)) {
					this.doDocumentDeletePhysical(allData);
				}
			}
			
			if(this.getStatus() == 1) {
				//check CONT_FORM_NO
				if(docInfoData.containsKey("CONT_FORM_NO")) {
					SepoaContractManager templateManager = null;
					String process = config.get("sepoa.contract.process", "");
					if("mk2".equalsIgnoreCase(process)) {
						templateManager = new sepoa.contractmk2.SepoaContractTemplateManager(info, config, getConnectionContext());
					} else {
						templateManager = new SepoaContractTemplateManager(info, config, getConnectionContext());
					}
					
					if(templateManager.setDocNumber(docInfoData.get("CONT_FORM_NO"), "1") == null) {
						int errCode = templateManager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							setMessage("get contract form info fail");
						}
					}else {
						Map<String, String> templateInfo = templateManager.getDocumentInfo();
						if(docData.containsKey("SIGNER")) {
							List<Map<String, String>> signerList = (List<Map<String, String>>) docData.get("SIGNER");
							if(signerList.size() > 0) {
								String signerCodeString = templateInfo.get("SIGNER_CODE");
								if(StringUtils.isNotBlank(signerCodeString)) {
									String[] signerCodeArray = signerCodeString.split(",");
									if(signerList.size() != signerCodeArray.length) {
										throw new Exception("required signer count ("+templateInfo.get("CONT_FORM_NO")+") : "+signerCodeString);
									}
									for(int i=0;i<signerList.size();i++) {
										Map<String, String> signer = signerList.get(i);
										signer.put("SIGNER_NO", signerCodeArray[i]);
										signer.put("SEQ", String.valueOf(i+1));
									}
								}
							}
						};
						allData.put("CONT_FORM_INFO", templateInfo);
						if(isAutoSend) {
							docInfoData.put("CONT_FORM_MAPPING", "");
						}
					}
				}else if(isAutoSend) {
					setFlag(false);
					setMessage("not found CONT_FORM_NO");
				}
			}
			if(getStatus() != 1) {
				setFlag(false);
			}else {
				if(isAutoSend) {
					docInfoData.put("NEXT_STEP", "END");
				}else {
					docInfoData.put("NEXT_STEP", "");
				}
				
				//process
				if(doDocumentCreate(allData).flag
						&& doDocumentUpdate(allData).flag) {
					setValue(contractNo);
					setValue(contractGlSeq);
					Commit();
					
					if(MapUtils.getBoolean(allData, SepoaModuleDataMapper.WITHOUT_COMMIT, false)){
						SepoaContractManager manager = getManager(info, config, getConnectionContext());
						manager.setDocNumber(docInfoData.get("CONT_NO"), docInfoData.get("CONT_GL_SEQ"));
						doAfterProcess(manager);
						Commit();
					}
				}else {
					Rollback();
				}
			}
		}catch(Exception e) {
			Rollback();
			setFlag(false);
			setMessage(e.getMessage());
			e.printStackTrace();
			Logger.err.println(this, e);
		}finally {
			if(!StringUtils.isBlank(tempFilePath)) {
				try {
					FileUtils.forceDelete(new File(tempFilePath));
				}catch(Exception e) {
					Logger.warn.println(info.getSession("ID"), this, "drop directory fail : "+tempFilePath);
				}
			}
		}
		return getSepoaOut();
	}
	
	@Description("새 문서[I/F]")
	public SepoaOut doNewContract(Map<String, Object> docData) {
		return doDocumentCreateWithOutData(docData, false);
	}
	
	@SuppressWarnings("unchecked")
	@Description("새 문서[with Out Data]")
	public SepoaOut doDocumentCreateWithOutData(Map<String, Object> docData, boolean isAutoSend) {
		setStatus(1);
		setFlag(true);
		String tempFilePath = null;
		try {
			//check DOC_INFO_DATA
			if(!docData.containsKey("DOC_INFO_DATA")) {
				throw new Exception("not found DOC_INFO_DATA");
			}
			Map<String, String> docInfoData = (Map<String, String>) docData.get("DOC_INFO_DATA");
			
			Config config = new Configuration();
			
			//create dummy alldata
			Map<String, Object> allData = new HashMap<String, Object>();
			allData.put(SepoaModuleDataMapper.KEY_HEADER_DATA,docInfoData );
			allData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfoData);
			allData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, docData);
			allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			allData.put(SepoaModuleDataMapper.WITHOUT_COMMIT, true);
			
			PathManager pm = new PathManager(config);
			pm.setLayer(PathManager.LAYER_TEMP);
			tempFilePath = pm.getUniquePath();
			
			allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, tempFilePath);
			
			//validate
			if(StringUtils.isBlank(docInfoData.get("CONT_METHOD"))) {
				throw new Exception("not found CONT_METHOD");
			}
			
			if(StringUtils.isBlank(docInfoData.get("SIGN_ORDER"))) {
				throw new Exception("not found SIGN_ORDER");
			}
			
			if(StringUtils.isBlank(docInfoData.get("CONT_TYPE"))) {
				docInfoData.put("CONT_TYPE", "B");
			}
			
			//adjust docInfoData
			for(Entry<String, Object> entry : docData.entrySet()) {
				docInfoData.put(entry.getKey(), SepoaModuleDataMapper.IS_JSON_OBJECT);
			}
			
			//check CONT_NO
			String contractNo = null;
			String contractGlSeq = null;
			if(!docInfoData.containsKey("CONT_NO")) {
				contractNo = DocumentUtil.getDocNumber(info, "CT").result[0];
				contractGlSeq = "1";
				docInfoData.put("CONT_NO"    , contractNo);
				docInfoData.put("CONT_GL_SEQ", "1");
			}else {
				contractNo = docInfoData.get("CONT_NO");
				contractGlSeq = docInfoData.get("CONT_GL_SEQ");
				this.doDocumentDeletePhysical(allData);
			}
			
			if(getStatus() != 1) {
				setFlag(false);
			}else {
				if(isAutoSend) {
					docInfoData.put("NEXT_STEP", "END");
				}else {
					docInfoData.put("NEXT_STEP", "");
				}
				
				//process
				if(doDocumentCreate(allData).flag
						&& doDocumentUpdate(allData).flag) {
					setValue(contractNo);
					setValue(contractGlSeq);
					Commit();
					
					if(MapUtils.getBoolean(allData, SepoaModuleDataMapper.WITHOUT_COMMIT, false)){
						SepoaContractManager manager = getManager(info, config, getConnectionContext());
						manager.setDocNumber(docInfoData.get("CONT_NO"), docInfoData.get("CONT_GL_SEQ"));
						doAfterProcess(manager);
						Commit();
					}
				}else {
					Rollback();
				}
			}
		}catch(Exception e) {
			Rollback();
			setFlag(false);
			setMessage(e.getMessage());
			e.printStackTrace();
			Logger.err.println(this, e);
		}finally {
			if(!StringUtils.isBlank(tempFilePath)) {
				try {
					FileUtils.forceDelete(new File(tempFilePath));
				}catch(Exception e) {
					Logger.warn.println(info.getSession("ID"), this, "drop directory fail : "+tempFilePath);
				}
			}
		}
		return getSepoaOut();
	}

	
	@Description("문서 업데이트")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentUpdate(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory(MapUtils.getString(allData,SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
				if(getStatus() == 1 && headerData.containsKey("DOC_INFO_DATA")) {
					Map<String, String> docInfoData = (Map<String, String>) jsonData.get("DOC_INFO_DATA");
					if(docInfoData.containsKey("CONT_FORM_NO")) {
						if(!manager.doUpdateForm(docInfoData.get("CONT_FORM_NO"))) {
							setStatus(manager.getLastErrCode());
						}
					}
					
					if(getStatus() == 1 && !manager.doUpdate(docInfoData)) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("DOC_AUTH_LIST")) {
					List<Map<String, String>> docAuthList = (List<Map<String, String>>) jsonData.get("DOC_AUTH_LIST");
					
					for(Map<String, String> row : docAuthList) {
						PrincipalAgentType pat = PrincipalAgentType.PERSONAL;
						if("GROUP".equals(row.get("LEVEL"))) {
							pat = PrincipalAgentType.GROUP;
						}
						Authorized auth = Authorized.READ;
						if("WRITE".equals(row.get("PERMISSION"))) {
							auth = Authorized.WRITE;
						}
						manager.grantTo(pat, row.get("COMPANY_CODE"), row.get("TARGET_CODE"), auth);
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("EDITOR_HTMLS")) {
					if(!manager.doUpdateEditorHTML((List<Map<String, String>>) jsonData.get("EDITOR_HTMLS"))) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("DOC_ITEM")) {
					if(!manager.doUpdateItem((Map<String, String>) jsonData.get("DOC_ITEM"))) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("SIGNER")) {
					if(!manager.doDeleteSigner()) {
						setStatus(manager.getLastErrCode());
					}else {
						List<String> validated = manager.doInsertSigner((List<Map<String, String>>) jsonData.get("SIGNER"));
						if(validated != null) {
							int errCode = manager.getLastErrCode();
							setStatus(errCode);
							setMessage(StringUtils.join(validated, "\n"));
						}
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("EXTRA_FILES")) {
					List<Map<String, String>> fileList = (List<Map<String, String>>) jsonData.get("EXTRA_FILES");
					for(Map<String, String> fileInfo : fileList) {
						manager.doAddFile(fileInfo.get("FILE_TYPE"), fileInfo, true);
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							break;
						}
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("CONT_FORM_MAPPING")) {
					if(!manager.doMappingData()) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
			}
			String beforeContStatus = manager.getDocumentInfo().get("STATUS");
			Map<String, String> docInfoData = new HashMap<>();
			if(getStatus() == 1 && this.onDocumentUpdateAfter(allData, manager)) {
				docInfoData = manager.getDocumentInfo();
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				if(StringUtils.isBlank(getMessage())){
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				}
				Rollback();
			}else if(!MapUtils.getBoolean(allData, SepoaModuleDataMapper.WITHOUT_COMMIT, false)){
				Commit();
				
				if(!MapUtils.getString(docInfoData, "STATUS", "").equals(beforeContStatus)) {
					doAfterProcess(manager);
					Commit();
				}
				
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	public boolean onDocumentUpdateAfter(Map<String, Object> allData, SepoaContractManager manager) {
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
		
		String nextStep = headerData.get("NEXT_STEP");
		
		switch(nextStep) {
		case "MODULE":             // 계약 문서 임시 저장
			if(!manager.doUpdateStatus("T")) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
				return false;
			}
			
			break;
		case "REVIEW_REQ" : {      // 계약 검토 요청
			SepoaPushManager pushManager = new SepoaPushManager(info, (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION));
			manager.setPushManager(pushManager);
			if(!manager.doReviewStart()) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
				return false;
			}
			pushManager.accept();
			break;
		}
		case "END": {              // 계약 문서 상신
			SepoaPushManager pushManager = new SepoaPushManager(info, (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION));
			manager.setPushManager(pushManager);
			if(!manager.doStartSign()) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
				return false;
			}
			pushManager.accept();
			break;
		}
		case "COMPLETE":           // 계약 완료
			if(!manager.doUpdateStatus("A")) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
				return false;
			}
			break;
		}

		return true;
	}
	
	@Description("파일 업로드")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doFileUpload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				Map<String, String> storedFileData = null;
				//only one file
				for(Map<String, String> file : (List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA)) {
					storedFileData = manager.doAddFile(headerData.get("FILE_TYPE"), file, headerData);
				}
				
				if(storedFileData == null) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}else if(!manager.doUpdate(headerData)) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}else if(!manager.doUpdateStatus("T")) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}else {
					storedFileData.put("TEMP_FILE_ID", headerData.get("FILE_ID"));
					setValue(storedFileData);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				Rollback();
			}else {
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("파일 리스트 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doFileList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				List<Map<String, String>> fileList = manager.doListFile();
				if(fileList == null) {
					setStatus(manager.getLastErrCode());
				}else {
					setValue(fileList);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("파일 삭제")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doFileDelete(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				if(!manager.doDeleteFile(headerData.get("FILE_ID"))) {
					setStatus(manager.getLastErrCode());
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("이미지 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doImageList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				List<Map<String, String>> fileList = manager.doListImage();
				if(fileList == null) {
					setStatus(manager.getLastErrCode());
				}else {
					setValue(fileList);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}

	@Description("이미지 다운로드")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doImage(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				String fileID = MapUtils.getString(headerData, "FILE_ID", "");
				String partID = MapUtils.getString(headerData, "PART_ID", "");
				boolean thumb = "Y".equals(headerData.get("THUMB"));
				boolean noImageFlag = "Y".equals(headerData.get("DEFAULT"));
				Map<String, String> fileInfo = manager.doImage(fileID, partID, thumb, noImageFlag);
				if(fileInfo == null) {
					setStatus(manager.getLastErrCode());
				}else {
					setValue(fileInfo);
					addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("감사추적인증서 PDF VIEWER 서비스")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut viewAuditTrail(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		String fileTempDirectory = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			if(config == null) {
				config = new Configuration();
			}
			
			if((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY) == null) {
				PathManager pm = new PathManager(info, config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			}
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				File auditTrail = manager.createAuditTrail();
				setObject(FileUtils.readFileToByteArray(auditTrail));
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		} finally {
			if(fileTempDirectory != null) {
				try {
					FileUtils.forceDelete(new File(fileTempDirectory));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return getSepoaOut();
	}
	
	@Description("결재 요청")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalStart(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory(MapUtils.getString(allData,SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
				
				if(getStatus() == 1 && allData.containsKey("DOC_AUTH_LIST")) {
					List<Map<String, String>> docAuthList = (List<Map<String, String>>) allData.get("DOC_AUTH_LIST");
					
					for(Map<String, String> row : docAuthList) {
						PrincipalAgentType pat = PrincipalAgentType.PERSONAL;
						Authorized auth = Authorized.WRITE;
						manager.grantTo(pat, row.get("COMPANY_CODE"), row.get("USER_ID"), auth);
					}
				}
				
				if(getStatus() == 1 && headerData.containsKey("EDITOR_HTMLS")) {
					if(!manager.doUpdateEditorHTML((List<Map<String, String>>) jsonData.get("EDITOR_HTMLS"))) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
				}
				
				if(!manager.doApprovalStart()) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				if(getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				}else {
					Commit();
					
					doAfterProcess(manager);
					Commit();
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("결재 완료")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalFinish(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			
			SepoaPushManager pushManager = new SepoaPushManager(info, (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION));
			manager.setPushManager(pushManager);
			
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				if(!manager.doApprovalFinish()) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				if(getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				}else {
					Commit();
					pushManager.accept();
					
					doAfterProcess(manager);
					Commit();
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("결재 반려")
	@SuppressWarnings("unchecked")
	public SepoaOut doApprovalReject(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				if(!manager.doApprovalReject()) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				}
				
				if(getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				}else {
					Commit();
					
					doAfterProcess(manager);
					Commit();
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("검토 승인")
	@SuppressWarnings("unchecked")
	public SepoaOut doReviewAccept(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			
			SepoaPushManager pushManager = new SepoaPushManager(info, (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION));
			manager.setPushManager(pushManager);
			
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				String comment = MapUtils.getString(headerData, "REVIEW_COMMENT", null);
				if(!manager.doReviewAccecpt(comment)) {
					setStatus(manager.getLastErrCode());
				}
			}
			
			if(getStatus() == 1) {
				Commit();
				pushManager.accept();
				
				doAfterProcess(manager);
				Commit();
			} else {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("검토 반려")
	@SuppressWarnings("unchecked")
	public SepoaOut doReviewReject(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			
			SepoaPushManager pushManager = new SepoaPushManager(info, (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION));
			manager.setPushManager(pushManager);
			
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				String comment = MapUtils.getString(headerData, "REVIEW_COMMENT", null);
				if(!manager.doReviewReject(comment)) {
					setStatus(manager.getLastErrCode());
				}
			}
			
			if(getStatus() == 1) {
				Commit();
				pushManager.accept();
				
				doAfterProcess(manager);
				Commit();
			} else {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("서명")
	@SuppressWarnings("unchecked")
	public SepoaOut doSign(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			
			SepoaPushManager pushManager = new SepoaPushManager(info, (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION));
			manager.setPushManager(pushManager);
			
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				String signType = headerData.get("SIGN_TYPE");
				String fixHtml = headerData.get("SIGN_FIX_HTML");
				String comment = headerData.get("SIGN_COMMENT");
				String sValue = headerData.get("SIGN_VALUE");
				
				String prefix = "EXT_";
				Map<String, String> extData = null;
				for(Entry<String, String> entry : headerData.entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue();
					if(StringUtils.startsWith(entry.getKey(), prefix)) {
						if(extData == null) {
							extData = new HashMap<>();
						}
						
						extData.put(key.replace(prefix, ""), value);
					}
				}
				
				Map<String, String> signerInfo = null;
				switch(signType) {
				case "D":
					signerInfo = manager.doSignDigital(fixHtml, comment);
					if(signerInfo == null){
						setStatus(manager.getLastErrCode());
					}
					break;
				default:
					if(extData == null) {
						signerInfo = manager.doSignCert(signType, fixHtml, comment, sValue);
					} else {
						signerInfo = manager.doSignCert(signType, fixHtml, comment, sValue, extData);
					}
					
					if(signerInfo == null) {
						setStatus(manager.getLastErrCode());
					}
					break;
				}
				
				if(getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				}else {
					Commit();
					pushManager.accept();
					
					doAfterProcess(manager, signerInfo);
					Commit();
				}
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("서명거부")
	@SuppressWarnings("unchecked")
	public SepoaOut doSignReject(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			
			SepoaPushManager pushManager = new SepoaPushManager(info, (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION));
			manager.setPushManager(pushManager);
			
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				if(!manager.doSignReject()) {
					setStatus(manager.getLastErrCode());
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				Commit();
				pushManager.accept();
				
				doAfterProcess(manager);
				Commit();
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("서명문서 정보조회")
	public SepoaOut doSelectClientSignInfo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				boolean checkCurrentSinger = MapUtils.getBoolean(headerData, "CHECK_CURRENT_SIGNER", true);
				Map<String, String> signerInfo = manager.getSignerInfo(checkCurrentSinger);
				if(signerInfo == null) {
					setStatus(manager.getLastErrCode());
				}else {
					signerInfo.put("SIGN_HASH", manager.getContractHash());
					setValue(signerInfo);
				}
			}
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("카테고리 변경")
	public SepoaOut doChangeCategory(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
				
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				if(!manager.doUpdate(new SingletonMap<String, String>("CATEGORY", headerData.get("CATEGORY")))) {
					setStatus(manager.getLastErrCode());
				}
			}
				
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				Rollback();
			}else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("만기도래 알람 변경")
	public SepoaOut doChangeAlarm(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
				
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				if(!manager.doUpdate(new SingletonMap<String, String>("EXPIRED_ALARM", headerData.get("EXPIRED_ALARM")))) {
					setStatus(manager.getLastErrCode());
				}
			}
				
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				Rollback();
			}else {
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@Description("문서 재작성")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentRewrite(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				if(getStatus() == 1) {
					if(!manager.doDocumentRewrite()) {
						setStatus(manager.getLastErrCode());
						
					}
				}
				
				if(getStatus() != 1) {
					Rollback();
					setFlag(false);
					setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
				}else {
					Commit();
					
					doAfterProcess(manager);
					Commit();
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("삭제")
	public SepoaOut doDocumentDelete(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				if(!manager.deleteDocument(true)) {
					setStatus(manager.getLastErrCode());
				}
			}
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				Commit();
				
				doAfterProcess(manager);
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("계약변경")
	public SepoaOut doDocumentAlteration(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			} else {
				if(!manager.doSelectAlterationDocument()) {
					setStatus(manager.getLastErrCode());
				}
			}
			
			if(getStatus() != 1) {
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("계약폐기")
	public SepoaOut doDocumentTermination(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaPushManager pushManager = new SepoaPushManager(info, config);
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				manager.setPushManager(pushManager);
				
				if(!manager.doUpdateTerminatior(headerData.get("T_MESSAGE"))) {
					setStatus(manager.getLastErrCode());
				}else {
					if("true".equals(headerData.get("IS_FORCE"))){
						if(!manager.doTermination(true)) {
							setStatus(manager.getLastErrCode());
						}
					}else {
						if(!manager.doTerminationStart()) {
							setStatus(manager.getLastErrCode());
						}
					}
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				pushManager.accept();
				Commit();
				
				doAfterProcess(manager);
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("계약폐기 - 동의")
	public SepoaOut doDocumentTerminationAgree(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaPushManager pushManager = new SepoaPushManager(info, config);
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				manager.setPushManager(pushManager);
				if(!manager.doTerminationAgree(info.getSession("COMPANY_CODE"), info.getSession("ID"))) {
					setStatus(manager.getLastErrCode());
				}
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				pushManager.accept();
				Commit();
				Map<String, String> docInfoData = manager.getDocumentInfo();
				if("X".equals(docInfoData.get("STATUS"))) {
					doAfterProcess(manager);
					Commit();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("수기계약서 업로드")
	public SepoaOut doManualContractUpload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			SepoaPushManager pushManager = new SepoaPushManager(info, config);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			manager.setPushManager(pushManager);
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) != null) {
				if(!manager.doCompleteForManual((List<Map<String, String>>) allData.get(SepoaModuleDataMapper.KEY_FILE_DATA))) {
					setStatus(manager.getLastErrCode());
				}
			}else {
				setStatus(manager.getLastErrCode());
			}
			
			if(getStatus() != 1) {
				Rollback();
				setFlag(false);
				setMessage(Cache.text.get("POA_CONTRACT.STATUS_"+String.valueOf(this.getStatus())));
			}else {
				pushManager.accept();
				Commit();
				
				doAfterProcess(manager);
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("계약서 알림 재전송")
	public SepoaOut doPushResend(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			SepoaPushManager pushManager = new SepoaPushManager(info, config);
			
			Map<String, Object> objData = (Map<String, Object>) allData.get(SepoaModuleDataMapper.KEY_OBJECT_DATA);
			List<Map<String, String>> listData = (List<Map<String, String>>) objData.get("list");
			
			for(Map<String, String> row : listData) {
				SepoaContractManager manager = getManager(info, config, getConnectionContext());
				manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
				manager.setContType(row.get("CONT_TYPE"));
				manager.setPushManager(pushManager);
				
				if(manager.setDocNumber(row.get("CONT_NO"), row.get("CONT_GL_SEQ")) == null) {
					setStatus(manager.getLastErrCode());
				}else {
					manager.doRePushSigner();
				}
				pushManager.accept();
				Commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		
		return getSepoaOut();
	}
	
	protected void doAfterProcess(SepoaContractManager manager) {
		doAfterProcess(manager, new HashMap<String, String>());
	}
	
	protected void doAfterProcess(SepoaContractManager manager, Map<String, String> signerInfo) {
		try {
			Map<String, String> docInfoData = manager.getDocumentInfo();
			Map<String, String> updateData = new HashMap<>();
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.ct.CT_205", "TRANSACTION", "doContStatusUpdate", new Object[] {docInfoData, signerInfo});
			if(so.flag) {
				updateData.put("AP_STATUS", "S");
				updateData.put("AP_MESSAGE", "　");
			} else {
				updateData.put("AP_STATUS", "F");
				updateData.put("AP_MESSAGE", so.message);
			}
			
			manager.doUpdate(updateData);
		} catch(Exception e) {
			e.printStackTrace();
			Logger.err.println(info.getSession("ID"), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Description("계약서 후속 프로세스 실행 서비스")
	public SepoaOut doAfterProcess(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaContractManager manager = getManager(info, config, getConnectionContext());
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}
			
			Map<String, String> docInfoData = manager.getDocumentInfo();
			Map<String, String> signerInfo = new HashMap<>();
			
			//STATUS (G: SignStart, A: SignComplete);
			List<String> targetStatus = new ArrayList<>();
			targetStatus.add("G");
			targetStatus.add("A");
			
			if(targetStatus.indexOf(MapUtils.getString(docInfoData, "STATUS")) >= 0) {
				Map<String, String> target = null;
				List<Map<String, String>> signerList = manager.doSelectSigner();
				for(Map<String, String> signer : signerList) {
					String signFlag = MapUtils.getString(signer, "SIGN_FLAG", "");
					Integer seq = MapUtils.getInteger(signer, "SEQ");
					if("S".equalsIgnoreCase(signFlag)) {
						if(target == null || seq > MapUtils.getInteger(target, "SEQ")) {
							target = signer;
						}
					}
				}
				
				if(target != null) {
					signerInfo = target;
				}				
			}

			doAfterProcess(manager, signerInfo);
			Commit();
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
}
