package sepoa.svc.contract;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import filters.ThreadLocalFilter;
import filters.ThreadLocalFilter.ReqRes;
import sepoa.contract.SepoaEmployeeContractManager;
import sepoa.contractmk2.SepoaContractTemplateManager;
import sepoa.filemanager.FileManager;
import sepoa.filemanager.utils.cipher.CipherManager;
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
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.authority.Permission.PrincipalAgentType;
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;
import sepoa.svl.util.SepoaActionController;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자계약 - 모듈")
public class SepoaEmployeeContractService extends SepoaService {
	public SepoaEmployeeContractService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}

	protected String getClassName() {
		return this.getClass().getName();
	}
	
	protected SepoaEmployeeContractManager getManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		return new SepoaEmployeeContractManager(info, config, ctx);
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
			condition.put("CONT_TYPE", "E");	// 임직원 계약
			
			List<String> ctrlCodeList = Arrays.asList(info.getSession("CTRL_CODE").split(","));
			boolean hasEmpContAllSelect = ctrlCodeList.contains("EMP_CONT_ALL_SELECT");
			if(hasEmpContAllSelect) {
				condition.put("PERMISSION", "ALL_SELECT");
			}
			
			int expiredWarnMonth = Integer.parseInt(CommonUtil.getConfig("sepoa.contract.expired.warn.month", "1"));
			switch(screenType) {
			case "CONTRACT":
				condition.put("MODE_TYPE_OPERATOR", "not in");
				condition.put("MODE_TYPE_STATUS", "A,X,Z,ES");
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
				condition.put("EXPIRED_ALARM_TO"  , SepoaDate.addSepoaDateMonth(SepoaDate.getShortDateString(), + expiredWarnMonth));
				break;
			case "CONTRACT_MANUAL":
				condition.put("MODE_TYPE_OPERATOR", "not in");
				condition.put("MODE_TYPE_STATUS", "A,X,Z");
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
				condition.put("MODE_TYPE_STATUS", "A,X,Z");
				
				condition.put("ORDER_COLUMN", "CONT_NO");
				condition.put("ORDER_SORT"  , "DESC");
				
				condition.put("EXPIRED_ALARM_FROM", SepoaDate.getShortDateString());
				condition.put("EXPIRED_ALARM_TO"  , SepoaDate.addSepoaDateMonth(SepoaDate.getShortDateString(), + expiredWarnMonth));
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setContType(headerData.get("CONT_TYPE"));
			manager.setLinkFlag(false);
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
				result.put("FILES"        , manager.doListFile());
				
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setLinkFlag(false);
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				if(headerData.containsKey("DISABLE_SELECT_ITEM") == false) {
					setValue(manager.doSelectItem());
				} else {
					setValue("");
				}
				
				if(headerData.containsKey("DISABLE_SELECT_SIGNER") == false) {
					setValue(manager.doSelectSigner());
				} else {
					setValue("");
				}
				
				if(headerData.containsKey("DISABLE_SELECT_HISTORY") == false) {
					setValue(manager.doSelectHistory());
				} else {
					setValue("");
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
	
	@Description("새 문서")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentCreate(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
	
	@Description("문서 파일 선택 다운로드")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doDocumentFileDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			String fileTempDirectory = (String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY);
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				String layer = headerData.get("LAYER");
				String seq = headerData.get("SEQ");
				Map<String, String> fileInfo = manager.doDocumentFileDownload(layer, seq);
				if(fileInfo == null) {
					setStatus(manager.getLastErrCode());
					setFlag(false);
				} else {
					File f = new File(fileTempDirectory + File.separator + fileInfo.get("FILE_NAME_CLIENT"));
					FileUtils.copyFile(new File(fileInfo.get("FILE_PATH")), f);
					File decryptedFile = CipherManager.decrypt(f, true);
					
					Map<String, Object> result = new HashMap<>();
					result.putAll(fileInfo);
					result.put("FILE_INPUT_STREAM", FileUtils.openInputStream(decryptedFile));
					
					setValue(result);
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
	@Description("완전삭제 - 예외처리")
	private SepoaOut doDocumentDeletePhysical(Map<String, Object> allData, Set<String> exclude) {
		//physical delete
		//prohibited to use alone
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setContType(headerData.get("CONT_TYPE"));
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ"), true) != null) {
				if(exclude.size() > 0) {
					int lastErrCode = 0;
					for(Map<String, String> file : manager.doListFile()) {
						if(!exclude.contains(file.get("LAYER"))) {
							if(!manager.doDeleteFile(file.get("FILE_ID"))) {
								lastErrCode = manager.getLastErrCode();
								break;
							}
						}
					}
					if(lastErrCode != 0) {
						setStatus(manager.getLastErrCode());
						setFlag(false);
					}
				}else {
					if(!manager.deleteDocument(false)) {
						setStatus(manager.getLastErrCode());
						setFlag(false);
					}
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
	
	@Description("완전삭제")
	private SepoaOut doDocumentDeletePhysical(Map<String, Object> allData) {
		return this.doDocumentDeletePhysical(allData, new HashSet<String>());
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
				docInfoData.put("CONT_TYPE", "E");
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
				}else {
					this.doDocumentDeletePhysical(allData, new HashSet<String> (Arrays.asList(new String[] {"CONT_C"})));
				}
			}
			
			if(this.getStatus() == 1) {
				//check CONT_FORM_NO
				if(docInfoData.containsKey("CONT_FORM_NO") || docInfoData.containsKey("CONT_EXT_FORM_NO")) {
					SepoaContractTemplateManager templateManager = new sepoa.contractmk2.SepoaContractTemplateManager(info, config, getConnectionContext());
					
					//메인, 서브 서식정보 융합
					String mainFormNumber = docInfoData.get("CONT_FORM_NO");
					String extFormNumbers = docInfoData.get("CONT_EXT_FORM_NO");
					
					List<String> concatFormNumbers = new ArrayList<String>();
					if(StringUtils.isNotEmpty(mainFormNumber)) {
						concatFormNumbers.add(mainFormNumber);
					}
					
					if(StringUtils.isNotEmpty(extFormNumbers)) {
						for(String extFormNo : extFormNumbers.split(",")) {
							if(StringUtils.isNotEmpty(extFormNo)) {
								concatFormNumbers.add(extFormNo);
							}
						}
					}
					
					if(concatFormNumbers.size() > 0) {
						//서식정보가 있음
						for(int formIndex=0;formIndex<concatFormNumbers.size();formIndex++) {
							String nowFormNo = concatFormNumbers.get(formIndex); 
							if(templateManager.setDocNumber(nowFormNo, "1") == null) {
								int errCode = templateManager.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
									setMessage("get contract form info fail : "+nowFormNo);
									break;
								}
							}else if(formIndex == 0){
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
								}
							}
						}
					}
					
					//자동전송시
					if(isAutoSend) {
						docInfoData.put("CONT_FORM_MAPPING", "");
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
						SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
				docInfoData.put("CONT_TYPE", "E");
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
						SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory(MapUtils.getString(allData,SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			
			if(StringUtils.isNotBlank(headerData.get("MAIL_SEND_YN")) && "Y".equals(headerData.get("MAIL_SEND_YN"))) {
				manager.setMailSendYn(headerData.get("MAIL_SEND_YN"));
			} else {
				manager.setMailSendYn("N");
			}
			
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}else {
				Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
				if(getStatus() == 1 && headerData.containsKey("DOC_INFO_DATA")) {
					Map<String, String> docInfoData = (Map<String, String>) jsonData.get("DOC_INFO_DATA");
					
					String extFormNo = docInfoData.get("CONT_EXT_FORM_NO");
					if(docInfoData.containsKey("CONT_FORM_NO")) {
						if(!manager.doUpdateForm(docInfoData.get("CONT_FORM_NO"), extFormNo)) {
							setStatus(manager.getLastErrCode());
						}
					}
					
					//서브 서식 정보 제거시 데이터 업데이틀 위한 추가코드
					if(StringUtils.isEmpty(extFormNo)) {
						docInfoData.put("CONT_EXT_FORM_NO", "_CLEAR_");
					}
					
					if(getStatus() == 1 && !manager.doUpdate(docInfoData)) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					
					//서브 서식 정보 제거시 데이터 업데이틀 위한 추가코드
					if(StringUtils.isEmpty(extFormNo)) {
						docInfoData.put("CONT_EXT_FORM_NO", "");
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
	public boolean onDocumentUpdateAfter(Map<String, Object> allData, SepoaEmployeeContractManager manager) {
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
		Map<String, String> documentInfo = manager.getDocumentInfo();
		
		String nextStep = headerData.get("NEXT_STEP");
		
		switch(nextStep) {
		case "MODULE":             // 계약 문서 임시 저장
			if((documentInfo != null && "T".equals(documentInfo.get("STATUS"))) && !manager.doUpdateStatus("T")) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
				return false;
			}
			
			break;
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
		case "UPLOAD_WAIT": 		//수기계약 업로드 대기
			if(!manager.doUpdateStatus("UW")) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
				return false;
			}
			
			break;
		case "EMPLOYEE_CREATE":			// 임직원 계약 생성
			if(!manager.doUpdateStatus("DC")) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			break;
		default:
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			manager.setLinkFlag(false);
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
	
	@Description("파일 순서 리스트 조회")
	@SuppressWarnings("unchecked")
	public SepoaOut doFileSeqList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setLinkFlag(false);
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
	
//	@Description("파일 순서 변경")
//	@SuppressWarnings("unchecked")
//	public SepoaOut doChangeFileSeq(Map<String, Object> allData) {
//		setStatus(1);
//		setFlag(true);
//		ConnectionContext ctx = getConnectionContext();
//		
//		try {
//			Config config =  new Configuration();
//			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
//			String jsonStr = headerData.get("PARAMETER");
//			List<Map<String,String>> fileList = new Gson().fromJson(jsonStr, new TypeToken<List<Map<String,String>>>(){}.getType());
//			FileManager fm = new FileManager(info, config, ctx);
//			fm.doChangeDocSeq(fileList);
//			Commit();
//		} catch (Exception e) {
//			Rollback();
//			e.printStackTrace();
//			setStatus(0);
//			setValue("");
//			setFlag(false);
//			setMessage(e.toString());
//			Logger.err.println(info.getSession("ID"), this, e);
//		}
//		return getSepoaOut();
//	}
	
	@Description("이미지 조회")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doImageList(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Config config = (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION);
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			manager.setLinkFlag(false);
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			manager.setLinkFlag(false);
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			} else {
				File auditTrail = manager.createAuditTrail();
				
				if("FILE".equalsIgnoreCase(MapUtils.getString(headerData, "RETURN_TYPE", ""))) {
					Map<String, Object> fileData = new HashMap<>();
					fileData.put("FILE_NAME_CLIENT", headerData.get("CONT_NO") + "_" + headerData.get("CONT_GL_SEQ") + "_REPORT.pdf");
					fileData.put("FILE_INPUT_STREAM", FileUtils.openInputStream(auditTrail));
					
					setValue(fileData);
					
					addParams(SepoaActionController.KEY_RETURN_TYPE, SepoaActionController.RETURN_TYPE_FILE);
				} else {
					setObject(FileUtils.readFileToByteArray(auditTrail));
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			
			SepoaPushManager pushManager = new SepoaPushManager(info, (Config) allData.get(SepoaModuleDataMapper.KEY_CONFIGURATION));
			manager.setPushManager(pushManager);
			
			if(manager.setDocNumber(headerData.get("CONT_NO"), headerData.get("CONT_GL_SEQ")) == null) {
				setStatus(manager.getLastErrCode());
			}else {
				String fixHtml = headerData.get("HTML");
				if(StringUtils.isNotBlank(fixHtml)) {
					List<Map<String, String>> htmls = new Gson().fromJson(fixHtml, List.class);
					if(!manager.doUpdateEditorHTML(htmls)) {
						setStatus(manager.getLastErrCode());
					}
				}
				
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
			manager.setTempDirectory((String) allData.get(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			manager.setContType(headerData.get("CONT_TYPE"));
			manager.setLinkFlag(false);
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
				
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
				
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
				SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
	
	protected void doAfterProcess(SepoaEmployeeContractManager manager) {
		doAfterProcess(manager, new HashMap<String, String>());
	}
	
	protected void doAfterProcess(SepoaEmployeeContractManager manager, Map<String, String> signerInfo) {
		try {
			Map<String, String> docInfoData = manager.getDocumentInfo();
			Map<String, String> updateData = new HashMap<>();
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.ct.ECT_205", "TRANSACTION", "doContStatusUpdate", new Object[] {docInfoData, signerInfo});
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
			
			SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
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
	
	@Description("계약 리스너 프로세스")
	public SepoaOut doListenerProcess(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);

		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		Map<String, String> condition = new HashMap<>();
		try {
			List<String> prcTypeList = Arrays.asList(new String[] {
					"EMPLOYEE",
					"SIGN_WAIT"
			});
			
			String target = "N";
			if("true".equalsIgnoreCase(CommonUtil.getConfig("sepoa.server.development.flag", "false"))) {
				target += this.getUniqueString();
			}
			condition.put("POSTPROCESSED", target);
			
			
			condition.put("PRC_TYPE", StringUtils.join(prcTypeList, ","));

			sxp = new SepoaXmlParser(this, "doQuerySMCWT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setLogEnable(true); // 반복적으로 동작하는 쿼리이기에 로깅 해제
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(condition));
			if(sf.getRowCount() > 0) {
				sxp = new SepoaXmlParser(this, "doUpdateSMCWT_STATUS_PENDING");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setLogEnable(true); // 반복적으로 동작하는 쿼리이기에 로깅 해제
				ssm.doUpdate(condition);
				Commit();
			}

			List<Map<String, String>> taskList = sf.getListMap();
			for (Map<String, String> task : taskList) {
				condition = new HashMap<>();
				String docNumber = MapUtils.getString(task, "DOC_NUMBER", "");
				String prcType = MapUtils.getString(task, "PRC_TYPE", "");
				condition.put("DOC_NUMBER", docNumber);
				condition.put("POSTPROCESSED", "Y");

				try {
					if ("EMPLOYEE".equalsIgnoreCase(prcType)) {
						this.doCreateEmployeeContractProcess(task);
					} else if("SIGN_WAIT".equalsIgnoreCase(prcType)) {
						this.doStartEmployeeContractProcess(task);
					}

					Commit();
				} catch (Exception e) {
					Rollback();
					e.printStackTrace();
					setStatus(0);
					setValue("");
					setFlag(false);
					Logger.err.println(info.getSession("ID"), this, e);

					condition.put("POSTPROCESSED", "F");
					condition.put("POSTPROCESSED_RESULT", e.getMessage());
				} finally {
					try {
						String[] baseCont = docNumber.split("_");
						if(baseCont.length == 2) {
							String contNo = baseCont[0];
							String contGlSeq = baseCont[1];
							Config config = new Configuration();
							SepoaEmployeeContractManager manager = getManager(info, config, ctx);
							manager.setCheckPermission(false);
							if(manager.setDocNumber(contNo, contGlSeq) == null) {
								int errCode = manager.getLastErrCode();
								if(errCode != 0) {
									setStatus(errCode);
									throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_" + getStatus()));
								}
							} else {
								int errCode = getStatus();
								String postProcessed = condition.get("POSTPROCESSED");
								if(errCode != 1 || "F".equals(postProcessed)) {
									manager.doUpdateStatus("EF");		// 임직원 계약 생성 실패
								}
							}
						}
						
						sxp = new SepoaXmlParser(this, "doUpdateSMCWT_STATUS");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(condition);
						Commit();
					} catch (Exception e) {
						Rollback();
						e.printStackTrace();
						Logger.err.println(info.getSession("ID"), this, e);
					}
				}
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			Logger.err.println(info.getSession("ID"), this, e);
		}

		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	private void doStartEmployeeContractProcess(Map<String, String> task) throws Exception {
		setStatus(1);
		setFlag(true);
		
		String fileTempDirectory = null;
		ConnectionContext ctx = getConnectionContext();
		try {
			String[] docNumber = MapUtils.getString(task, "DOC_NUMBER", "").split("_");
			if(docNumber.length < 2) {
				throw new Exception("Invalid DOC_NUMBER Type.");
			}
			
			Config config = new Configuration();
			PathManager pm = new PathManager(config);
			pm.setLayer(PathManager.LAYER_TEMP);
			fileTempDirectory = pm.getUniquePath();
			
			String contNo = docNumber[0];
			String contGlSeq = docNumber[1];
			Map<String, Object> data = new Gson().fromJson(MapUtils.getString(task, "DATA", "{}"), Map.class);
			if(data.containsKey("REQUESTER_INFO")) {
				Map<String, String> requesterSessionInfo = MapUtils.getMap(data, "REQUESTER_INFO", null);
				if(requesterSessionInfo == null) {
					SepoaEmployeeContractManager manager = getManager(info, config, getConnectionContext());
					manager.setCheckPermission(false);
					manager.setTempDirectory(fileTempDirectory);
					if(manager.setDocNumber(contNo, contGlSeq) == null) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					} else {
						Map<String, String> docInfo = manager.getDocumentInfo();
						String creatorHouseCode = docInfo.get("HOUSE_CODE");
						String creatorCompanyCode = docInfo.get("COMPANY_CODE");
						String creatorId = docInfo.get("CREATOR");
						
						requesterSessionInfo = new HashMap<>();
						requesterSessionInfo.put("HOUSE_CODE", creatorHouseCode);
						requesterSessionInfo.put("COMPANY_CODE", creatorCompanyCode);
						requesterSessionInfo.put("ID", creatorId);
					}
				}
				
				if(requesterSessionInfo == null) {
					throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_" + getStatus()));
				}
				
				SepoaInfo requesterInfo = new SepoaInfo(requesterSessionInfo);
				ReqRes tempReqRes = ThreadLocalFilter.tl.get();
				requesterInfo.setThreadLocal();
				
				// 요청자의 info로 manager를 생성한다.
				SepoaEmployeeContractManager manager = getManager(requesterInfo, config, ctx);
				manager.setCheckPermission(false);
				manager.setTempDirectory(fileTempDirectory);
				if(manager.setDocNumber(contNo, contGlSeq) == null) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
					}
				} else {
					SepoaPushManager pushManager = new SepoaPushManager(info, config);
					manager.setPushManager(pushManager);
					if(manager.doStartSign() == false) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
						}
					}
					pushManager.accept();
				}
				
				info.setThreadLocal();
				ThreadLocalFilter.tl.set(tempReqRes);
				
				if(getStatus() != 1) {
					throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_" + getStatus()));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}  finally {
			if(fileTempDirectory != null) {
				try {
					FileUtils.forceDelete(new File(fileTempDirectory));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void doCreateEmployeeContractProcess(Map<String, String> task) throws Exception {
		ConnectionContext ctx = getConnectionContext();
		String[] docNumber = MapUtils.getString(task, "DOC_NUMBER", "").split("_");
		if(docNumber.length < 2) {
			throw new Exception("Invalid DOC_NUMBER Type.");
		}
		
		String baseContNo = docNumber[0];
		String baseContGlSeq = docNumber[1];
		Map<String, Object> data = new Gson().fromJson(MapUtils.getString(task, "DATA", "{}"), Map.class);
		if(data.containsKey("SIGNER_GROUP")) {
			Map<String, String> creatorInfo = MapUtils.getMap(data, "CREATOR_INFO", null);
			List<Map<String, String>> signerGroup = (List<Map<String, String>>) data.get("SIGNER_GROUP");
			for(Map<String, String> signer : signerGroup) {
				Map<String, Object> extCopyData = new HashMap<>();
				
				if(creatorInfo != null) {
					extCopyData.put("CREATOR_INFO", creatorInfo);
				}
				
				List<Map<String, String>> signergroupList = new ArrayList<>();
				signergroupList.add(signer);
				
				extCopyData.put("SIGNER_GROUP", signergroupList);
				
				String taskIndex = " [" + (signerGroup.indexOf(signer) + 1) + "/" + signerGroup.size() + "]";
				String messagePrefix = "[" + baseContNo + "_" + baseContGlSeq + "]" + taskIndex;
				if(this.doCopyEmployeeContract(baseContNo, baseContGlSeq, extCopyData)) {
					Logger.info.println(info.getSession("ID"), this, messagePrefix + " Employee Contract Create Done.");
				} else {
					String suffix = "";
					if(getStatus() != 1) {
						suffix = " : " + Cache.text.get("POA_CONTRACT.STATUS_" + getStatus());
					} else {
						if(StringUtils.isBlank(getMessage()) == false) {
							suffix = " : " + getMessage();
						}
					}
					
					String msg = messagePrefix;
					if(StringUtils.isBlank(suffix)) {
						msg += " Employee Contract Create Failed.";
					} else {
						msg += suffix;
					}
					
					throw new Exception(msg);
				}
			}
			
			Config config = new Configuration();
			SepoaEmployeeContractManager manager = getManager(info, config, ctx);
			manager.setCheckPermission(false);
			if(manager.setDocNumber(baseContNo, baseContGlSeq) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
					throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_" + getStatus()));
				}
			} else {
				manager.doUpdateStatus("ES");		// 임직원 계약 생성
			}
		} else {
			throw new Exception("DATA['SIGNER_GROUP'] dosen't exists.");
		}
	}
	
	@SuppressWarnings("unchecked")
	private boolean doCopyEmployeeContract(String baseContNo, String baseContGlSeq, Map<String, Object> extCopyData) {
		setStatus(1);
		setFlag(true);
		
		boolean result = true;
		ConnectionContext ctx = getConnectionContext();
		String fileTempDirectory = null;
		try {
			Config config = new Configuration();
			SepoaEmployeeContractManager manager = getManager(info, config, ctx);
			manager.setCheckPermission(false);
			if(manager.setDocNumber(baseContNo, baseContGlSeq) == null) {
				int errCode = manager.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
					return false;
				}
			} else {
				String contNo = DocumentUtil.getDocNumber(info, "CT").result[0];
				String contGlSeq = "1";
				
				Map<String, Object> contractData = new HashMap<>();
				contractData.put("CONT_NO", contNo);
				contractData.put("CONT_GL_SEQ", contGlSeq);
				
				FileManager fm = new FileManager(info, config, ctx);
				List<Map<String, String>> baseFileList = null;
				List<Map<String, String>> copyList = new ArrayList<>();
				if(fm.setDocNumber(baseContNo + "_" + baseContGlSeq) != null) {
					baseFileList = fm.doList();
					if(baseFileList == null) {
						setStatus(fm.getLastErrCode());
						
						return false;
					}
					
					for(Map<String, String> file : baseFileList) {
						copyList.add(file);
					}
				}
				
				Map<String, String> mergeDocInfo = this._getMergeDocInfo(manager, extCopyData);
				if(mergeDocInfo == null) {
					return false;
				}
				mergeDocInfo.put("CONT_NO", contNo);
				mergeDocInfo.put("CONT_GL_SEQ", contGlSeq);
				mergeDocInfo.put("STATUS", "EW");
				
				if(StringUtils.isBlank("CONT_TYPE")) {
					mergeDocInfo.put("CONT_TYPE", "E");
				}
				
				contractData.put("DOC_INFO_DATA", mergeDocInfo);
				
				List<Map<String, String>> mergeSignerList = this._getMergeSignerList(manager, extCopyData);
				if(mergeSignerList == null || mergeSignerList.isEmpty()) {
					return false;
				}
				contractData.put("SIGNER", mergeSignerList);
				
				List<String> excludeList = Arrays.asList(new String[] {
						"HOUSE_CODE", "COMPANY_CODE", "ADD_DATE", "ADD_TIME", "ADD_USER_ID",
						"CHANGE_DATE", "CHANGE_TIME", "DEL_FLAG", "CHANGE_USER_ID", "CATEGORY",
						"SIGNER_NO", "SEQ", "SIGNER_ID", "SIGNER_COMPANY_CODE", "SIGN_FLAG"
				});
				for(Map<String, String> signer : mergeSignerList) {
					String signerType = MapUtils.getString(signer, "SIGNER_TYPE", "");
					if("OUT".equalsIgnoreCase(signerType)) {
						for(Entry<String, String> entry : signer.entrySet()) {
							if(excludeList.contains(entry.getKey()) == false) {
								mergeDocInfo.put(entry.getKey(), entry.getValue());
							}
						}
					}
				}
				
				// 임직원 계약 별 계약명 세팅
				List<String> employeeNameList = new ArrayList<>();
				for(Map<String, String> signer : mergeSignerList) {
					String signerType = MapUtils.getString(signer, "SIGNER_TYPE");
					if("OUT".equalsIgnoreCase(signerType)) {
						String signerId = MapUtils.getString(signer, "SIGNER_ID");
						String signerName = MapUtils.getString(signer, "SIGNER_NAME");
						employeeNameList.add(signerId + " / " + signerName);
					}
				}
				
				String subjectPrefix = "[" + StringUtils.join(employeeNameList, ", ") + "] ";
				String contSubject = MapUtils.getString(mergeDocInfo, "CONT_SUBJECT", "");
				mergeDocInfo.put("CONT_SUBJECT", subjectPrefix + contSubject);
				
				Map<String, String> mergeDocItem = this._getMergeDocItem(manager, contractData, extCopyData);
				if(mergeDocItem == null) {
					return false;
				}
				contractData.put("DOC_ITEM", mergeDocItem);
				
				List<Map<String, String>> mergeDocAuthList = this._getMergeDocAuthList(manager, extCopyData);
				if(mergeDocAuthList == null) {
					return false;
				}
				contractData.put("DOC_AUTH_LIST", mergeDocAuthList);
				
				contractData.put("CONT_FORM_MAPPING", "Y");		// 추가된 서명자 데이터와 FIX_HTML의 SIGNER_NO 동기화
				
				PathManager pm = new PathManager(config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				
				Map<String, String> creator = MapUtils.getMap(extCopyData, "CREATOR_INFO", null);
				String creatorHouseCode = MapUtils.getString(creator, "HOUSE_CODE", mergeDocInfo.get("HOUSE_CODE"));
				String creatorCompanyCode = MapUtils.getString(creator, "COMPANY_CODE", mergeDocInfo.get("COMPANY_CODE"));
				String creatorId = MapUtils.getString(creator, "ID", mergeDocInfo.get("ADD_USER_ID"));
				String creatorDept = MapUtils.getString(creator, "DEPARTMENT", mergeDocInfo.get("CREATOR_DEPT"));
				String creatorIp = MapUtils.getString(creator, "IP", "");
				
				Map<String, String> creatorSessionInfo = new HashMap<>();
				creatorSessionInfo.put("HOUSE_CODE", creatorHouseCode);
				creatorSessionInfo.put("COMPANY_CODE", creatorCompanyCode);
				creatorSessionInfo.put("ID", creatorId);
				creatorSessionInfo.put("DEPARTMENT", creatorDept);
				creatorSessionInfo.put("IP", creatorIp);
			
				SepoaInfo creatorInfo = new SepoaInfo(creatorSessionInfo);
				ReqRes tempReqRes = ThreadLocalFilter.tl.get();
				creatorInfo.setThreadLocal();
				
				// 서명자의 info로 manager를 재생성한다.
				manager = getManager(creatorInfo, config, ctx);
				
				if(manager.setDocNumber(contNo, contGlSeq, true) == null) {
					int errCode = manager.getLastErrCode();
					if(errCode != 0) {
						setStatus(errCode);
						return false;
					}
				} else {
					// 전자계약 모듈 API 호출을 위한 파라미터 세팅 (액션 컨트롤러 파라미터 구조를 수동으로 세팅해준다.)
					Map<String, Object> paramData = new HashMap<>();
					paramData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
					paramData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
					paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, contractData);
					paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, contractData);
					paramData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, contractData);
					paramData.put(SepoaModuleDataMapper.WITHOUT_COMMIT, true);
					
					String nextStep = null;
					List<Map<String, String>> signerGroupList = (List<Map<String, String>>) MapUtils.getObject(extCopyData, "SIGNER_GROUP", null);
					if(signerGroupList != null) {
						for(Map<String, String> signer : signerGroupList) {
							String signerNextStep = MapUtils.getString(signer, "NEXT_STEP", "");
							if(nextStep == null && StringUtils.isBlank(signerNextStep) == false) {
								nextStep = signerNextStep;
								break;
							}
						}
					}
					
					if(StringUtils.isBlank(nextStep)) {
						nextStep = "EMPLOYEE_WAIT";	// 기본값 지정 : 임직원 계약 생성 대기
					}
					contractData.put("NEXT_STEP", nextStep);
					
					SepoaOut so = ServiceConnector.doService(creatorInfo, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, "doDocumentUpdate", new Object[] {paramData});
					if(so.flag == false) {
						setStatus(so.status);
						return false;
					}
					
					if (copyList.size() > 0) {
						String docNumber = contNo + "_" + contGlSeq;
						fm.setDocNumber(docNumber);
						
						fm.setLayer("");
						fm.doDelete();
						
						for (Map<String, String> file : copyList) {
							fm.setLayer(file.get("LAYER"));
							fm.doCopy(file.get("DOC_NUMBER"), file.get("FILE_ID"), file.get("LAYER"));
						}
					}
					
					// 커스텀맵핑데이터 추가 작업
					if(manager.doMappingData(contractData) == false) {
						int errCode = manager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							return false;
						}
					}
					
					String contType = MapUtils.getString(mergeDocInfo, "CONT_TYPE", "");
					if("B".equals(contType)) {
						contractData.put("NEXT_STEP", "END");
						so = ServiceConnector.doService(creatorInfo, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, "doDocumentUpdate", new Object[] {paramData});
						if(so.flag == false) {
							setStatus(so.status);
							return false;
						}
					}
					
					info.setThreadLocal();
					ThreadLocalFilter.tl.set(tempReqRes);
				}
			}
		} catch(Exception e) {
			result = false;
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
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, String> _getMergeDocInfo(SepoaEmployeeContractManager manager, Map<String, Object> extCopyData) {
		Map<String, String> mergeDocInfo = new HashMap<>();
		
		Map<String, String> baseDocInfo = manager.getDocumentInfo();
		mergeDocInfo.putAll(baseDocInfo);
		
		Map<String, String> docInfo = MapUtils.getMap(extCopyData, "DOC_INFO_DATA", null);
		if(docInfo != null) {
			mergeDocInfo.putAll(docInfo);
		}
		
		return mergeDocInfo;
	}
	
	@SuppressWarnings("unchecked")
	private List<Map<String, String>> _getMergeSignerList(SepoaEmployeeContractManager manager, Map<String, Object> extCopyData) {
		List<Map<String, String>> mergeSignerList = new ArrayList<>();
		
		List<Map<String, String>> companySignerList = new ArrayList<>();
		List<Map<String, String>> baseSignerList = manager.doSelectSigner();
		if(baseSignerList != null) {
			List<String> innerSignerList = null;
			Map<String, String> itemObj = manager.doSelectItem();
			if(itemObj.containsKey("EX_INNER_ID")) {
				innerSignerList = Arrays.asList(MapUtils.getString(itemObj, "EX_INNER_ID", "").split(","));
			}
			
			for(Map<String, String> baseSigner : baseSignerList) {
				String signerId = MapUtils.getString(baseSigner, "SIGNER_ID");
				String seq = MapUtils.getString(baseSigner, "SEQ", "");
				
				// 불필요 데이터 제거
				baseSigner.remove("CONT_NO");
				baseSigner.remove("CONT_GL_SEQ");
				baseSigner.remove("LINK");
				
				// 회사측(내부) 계약자인 경우를 위한 데이터 세팅
				baseSigner.put("SIGNER_TYPE", "IN");
				
				if(innerSignerList != null) {
					if(innerSignerList.contains(signerId)) {
						companySignerList.add(baseSigner);
					}
				} else {
					if("1".equals(seq)) {
						companySignerList.add(baseSigner);
					}
				}
				
			}
		}
		
		if(companySignerList.isEmpty()) {
			String msg = "Company Signer is empty.";
			Logger.err.println(info.getSession("ID"), this, msg);
			setMessage(msg);
			return null;
		}
		
		String companySignOrder = "FIRST";
		List<Map<String, String>> employeeSignerList = new ArrayList<>();
		List<Map<String, String>> signerGroupList = (List<Map<String, String>>) MapUtils.getObject(extCopyData, "SIGNER_GROUP", null);
		if(signerGroupList != null) {
			for(Map<String, String> signer : signerGroupList) {
				String signerId = MapUtils.getString(signer, "SIGNER_ID", "");
				String companySign = MapUtils.getString(signer, "COMPANY_SIGN", "");
				if(companySignOrder == null && StringUtils.isBlank(companySign) == false) {
					companySignOrder = companySign;
				}
				
				// 임직원(외부) 계약자인 경우를 위한 데이터 세팅
				signer.put("SIGNER_ID", signerId.toUpperCase());
				signer.put("SIGNER_TYPE", "OUT");
				
				employeeSignerList.add(signer);
			}
		}
		
		if(employeeSignerList.isEmpty()) {
			String msg = "Employee Signer is empty.";
			Logger.err.println(info.getSession("ID"), this, msg);
			setMessage(msg);
			return null;
		}
		
		if("FIRST".equalsIgnoreCase(companySignOrder)) {
			mergeSignerList.addAll(companySignerList.stream().map(e -> new HashMap<>(e)).collect(Collectors.toList()));
			mergeSignerList.addAll(employeeSignerList.stream().map(e -> new HashMap<>(e)).collect(Collectors.toList()));
		} else if("LAST".equalsIgnoreCase(companySignOrder)) {
			mergeSignerList.addAll(employeeSignerList.stream().map(e -> new HashMap<>(e)).collect(Collectors.toList()));
			mergeSignerList.addAll(companySignerList.stream().map(e -> new HashMap<>(e)).collect(Collectors.toList()));
		}
		
		// 병합한 서명자 목록의 순서 데이터(SEQ) 설정
		List<String> signerIdList = new ArrayList<>();
		List<String> signerNoList = new ArrayList<>();
		for(Map<String, String> signer : mergeSignerList) {
			String signerId = signer.get("SIGNER_ID");
			String signerName = signer.get("SIGNER_NAME");
			String baseSignerNo = baseSignerList.get(mergeSignerList.indexOf(signer)).get("SIGNER_NO");
			
			signer.put("SEQ", String.valueOf(mergeSignerList.indexOf(signer) + 1));
			signer.put("SIGNER_NO", baseSignerNo);
			
			if(signerIdList.contains(signerId)) {
				String msg = "Duplicated Signer : " + signerName + "(" + signerId + ")";
				Logger.err.println(info.getSession("ID"), this, msg);
				setMessage(msg);
				return null;
			} else {
				signerIdList.add(signerId);
			}
			
			if(signerNoList.contains(baseSignerNo)) {
				String msg = "Duplicated Signer : " + baseSignerNo;
				Logger.err.println(info.getSession("ID"), this, msg);
				setMessage(msg);
				return null;
			} else {
				signerNoList.add(baseSignerNo);
			}
			
		}
		
		return mergeSignerList;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, String> _getMergeDocItem(SepoaEmployeeContractManager manager, Map<String, Object> contractData, Map<String, Object> extCopyData) throws Exception {
		Map<String, String> mergeDocItem = new HashMap<>();
		Map<String, String> baseDocItem = manager.doSelectItem();
		mergeDocItem.putAll(baseDocItem);
		
		Map<String, String> docItem = MapUtils.getMap(extCopyData, "DOC_ITEM", null);
		if(docItem != null) {
			mergeDocItem.putAll(docItem);
		}
		
		List<String> innerSignerName = new ArrayList<>();
		List<String> innerID = new ArrayList<>();
		List<String> innerIRS = new ArrayList<>();
		
		List<String> outerSignerCompanyName = new ArrayList<>();
		List<String> outerSignerName = new ArrayList<>();
		List<String> outerID = new ArrayList<>();
		List<String> outerIRS = new ArrayList<>();
		List<Map<String, String>> signerList = (List<Map<String, String>>) MapUtils.getObject(contractData, "SIGNER");
		for(Map<String, String> signer : signerList) {
			String signerType = MapUtils.getString(signer, "SIGNER_TYPE", "");
			String signerName = MapUtils.getString(signer, "SIGNER_NAME");
			String signerId = MapUtils.getString(signer, "SIGNER_ID", "");
			String signKey = MapUtils.getString(signer, "SIGN_KEY", "");
			String signerSeq = MapUtils.getString(signer, "SEQ", "__EMPTY__");
			String signerCompanyName = MapUtils.getString(signer, "SIGNER_COMPANY_NAME");
			
			if("IN".equals(signerType)) {
				innerSignerName.add(signerName);
				innerID.add(signerId);
				innerIRS.add(signKey);
			} else if("OUT".equals(signerType)) {
				outerSignerCompanyName.add(signerCompanyName);
				outerSignerName.add(signerName);
				outerID.add(signerId);
				outerIRS.add(signKey);
			}
			
			for(Entry<String, String> entry : signer.entrySet()) {
				mergeDocItem.put("EX_SIGNER_" + signerSeq + "_" + entry.getKey(), entry.getValue());
			}
		}
		
		if(innerSignerName.size() > 0) {
			mergeDocItem.put("EX_INNER_SIGNER_NAME", StringUtils.join(innerSignerName, ","));
		}
		
		if(innerID.size() > 0) {
			mergeDocItem.put("EX_INNER_ID", StringUtils.join(innerID, ","));
		}
		
		if(innerIRS.size() > 0) {
			mergeDocItem.put("EX_INNER_IRS_NO", StringUtils.join(innerIRS, ","));
		}
		
		if(outerSignerCompanyName.size() > 0) {
			mergeDocItem.put("EX_OUTER_SIGNER_COMPANY_NAME", StringUtils.join(outerSignerCompanyName, ","));
		}
		
		if(outerSignerName.size() > 0) {
			mergeDocItem.put("EX_OUTER_SIGNER_NAME", StringUtils.join(outerSignerName, ","));
		}
		
		if(outerID.size() > 0) {
			String outerIDstr = StringUtils.join(outerID, ",");
			mergeDocItem.put("EX_OUTER_ID", outerIDstr);
			
			Map<String, String> condition = new HashMap<>();
			condition.put("USER_ID_LIST", outerIDstr);
			
			ConnectionContext ctx = getConnectionContext();
			SepoaXmlParser sxp = new SepoaXmlParser(this,"doSelectUserInfo");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(condition));
			if(sf.getRowCount() > 0) {
				List<String> outerDeptName = new ArrayList<>();
				List<String> outerPosition = new ArrayList<>();
				for(String id : outerID) {
					outerDeptName.add("");
					outerPosition.add("");
				}
				
				List<Map<String, String>> userInfoList = sf.getListMap();
				for(Map<String, String> userInfo : userInfoList) {
					String userId = MapUtils.getString(userInfo, "USER_ID");
					String deptName = MapUtils.getString(userInfo, "DEPT_NAME_LOC");
					String position = MapUtils.getString(userInfo, "POSITION");
					int index = outerID.indexOf(userId);
					
					outerDeptName.set(index, deptName);
					outerPosition.set(index, position);
				}
				
				mergeDocItem.put("EX_OUTER_SIGNER_DEPT_NAME", StringUtils.join(outerDeptName, ","));
				mergeDocItem.put("EX_OUTER_SIGNER_POSITION", StringUtils.join(outerPosition, ","));
			}
			
		}
		
		if(outerIRS.size() > 0) {
			mergeDocItem.put("EX_OUTER_IRS_NO", StringUtils.join(outerIRS, ","));
		}
		
		return mergeDocItem;
	}
	
	private List<Map<String, String>> _getMergeDocAuthList(SepoaEmployeeContractManager manager, Map<String, Object> extCopyData) throws Exception {
		List<Map<String, String>> docAuthList = new ArrayList<>();
		
//		ConnectionContext ctx = getConnectionContext();
//		SepoaXmlParser sxp = new SepoaXmlParser(this,"doQueryWorkTypeZ");
//		SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
//		List<Map<String, String>> targetList = new SepoaFormater(ssm.doSelect(new HashMap<String, String>())).getListMap();
//		for(Map<String,String> target : targetList) {
//			Map<String, String> auth = new HashMap<String, String>();
//			String targetCode = MapUtils.getString(target, "USER_ID", "").toUpperCase();
//			auth.put("COMPANY_CODE", target.get("COMPANY_CODE"));
//			auth.put("TARGET_CODE", targetCode);
//			auth.put("PERMISSION", Authorized.READ.toString());
//			
//			if(targetCode.contains("LISTENER")) {
//				auth.put("PERMISSION", Authorized.WRITE.toString());
//			}
//			
//			docAuthList.add(auth);
//		}
		
		return docAuthList;
	}
	
	private String getUniqueString() {
		String result = "";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
			while(networks.hasMoreElements()) {
				NetworkInterface network = networks.nextElement();
				byte[] mac = network.getHardwareAddress();
				if(mac != null) {
					String macAddr = new String(Hex.encodeHex(md.digest(mac)));
					result = "-" + macAddr.substring(0, 8);
					break;
				}
			}
		} catch(Exception e) {
			
		}
		
		return result;
	}
}
