package sepoa.svc.ct;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import filters.ThreadLocalFilter;
import sepoa.contractmk2.SepoaContractTemplateManager;
import sepoa.filemanager.FileManager;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.approval.SepoaApprovalLineSetting;
import sepoa.fw.approval.SepoaApprovalProgress;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaService;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자인장 - 임직원 계약 모듈 호출용 서비스")
public class ECT_205 extends SepoaService {

	public ECT_205(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@SuppressWarnings("unchecked")
	@Description("계약 문서 조회")
	public SepoaOut doQueryContract(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;

		try {
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, "doDocumentDetail", new Object[] {allData});
			if(so.flag) {
				List<Object> result = so.getAllData();
				if(result.size() > 0) {
					Map<String, Object> detail = (Map<String, Object>) result.get(0);
					Map<String, Object> docInfo = MapUtils.getMap(detail, "DOC_INFO_DATA");
					detail.putAll(docInfo);
					
					setValue(detail);
				}
			} else {
				throw new Exception(so.message);
			}
		} catch (Exception e) {
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
	@Description("계약 문서 생성")
	public SepoaOut doSaveContract(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		String fileTempDirectory = null;
		String contractNo = null;
		String contractGlSeq = null;
		String method = null;
		SepoaOut so = null;
		
		try {
			if(checkCreatePermission(allData) == false) {
				setStatus(0);
				setFlag(false);
				setMessage(Cache.text.get("MESSAGE.0073"));
				return getSepoaOut();
			}
			
			checkDataValidate(allData);
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
			Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
			Config config = (Config) MapUtils.getObject(allData, SepoaModuleDataMapper.KEY_CONFIGURATION, null);
			if(config == null) {
				config = new Configuration();
				allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			}
			
			String tempDirectory = MapUtils.getString(allData, SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, null);
			if(tempDirectory == null) {
				PathManager pm = new PathManager(info, config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			}
			
			Map<String, Object> contractData = new HashMap<String, Object>();	// 계약 문서 생성을 위한 계약 데이터 객체
			
			Map<String, String> docInfoData = getDocInfoData(allData);			// 계약 문서의 기본 데이터 적재
			contractData.put("DOC_INFO_DATA", docInfoData);
			
			List<Map<String, String>> signerList = getSignerListData(allData);	// 계약 문서의 계약자 데이터 적재
			contractData.put("SIGNER", signerList);
			
			Map<String, String> docItem = getExtraDocInfoData(allData);			// 계약 문서의 확장 데이터 적재
			contractData.put("DOC_ITEM", docItem);
			
			List<Map<String, String>> docAuthList = getDocAuthList(allData);
			contractData.put("DOC_AUTH_LIST", docAuthList);
			
			List<Map<String, String>> extraFiles = getExtraFileList(allData);
			if(extraFiles != null) {
				contractData.put("EXTRA_FILES", extraFiles);
			}
			
			Map<String, String> templateMappingData = getTemplateMappingData(allData); // 템플릿 아이템 데이터 적재
			if(templateMappingData.size()>0) {
				contractData.put("TEMPLATE_MAPPING_DATA", templateMappingData);
			}
			
			contractData.entrySet().removeIf(entry -> entry.getValue() == null);
			
			Map<String, Object> paramData = new HashMap<>();					// 전자계약 모듈 API 호출을 위한 파라미터 세팅 (액션 컨트롤러 파라미터 구조를 수동으로 세팅해준다.)
			paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, docInfoData);
			paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfoData);
			paramData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, contractData);
			paramData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			paramData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, MapUtils.getString(allData, SepoaModuleDataMapper.KEY_TEMP_DIRECTORY));
			paramData.put(SepoaModuleDataMapper.WITHOUT_COMMIT, true);
			
			if(!StringUtils.isBlank(headerData.get("SMR_NUMBER"))) {			// 날인 완료된 문서를 기반으로 계약 문서를 작성할 경우
				boolean createContractFlag = false;
				boolean copyContractFileFlag = false;
				
				Map<String, String> condition = new HashMap<>();
				condition.put("MODE", "__SOURCE_NO__");
				condition.put("SOURCE_NO", headerData.get("SMR_NUMBER"));
				
				if(!StringUtils.isBlank(headerData.get("CONT_NO")) && !StringUtils.isBlank(headerData.get("CONT_GL_SEQ"))) {
					condition.put("MODE", "__CONT_NO__");
					condition.put("CONT_NO", headerData.get("CONT_NO"));
					condition.put("CONT_GL_SEQ", headerData.get("CONT_GL_SEQ"));
				}
				
				sxp = new SepoaXmlParser(this, "doQuerySSCGL");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				
				sf = new SepoaFormater(ssm.doSelect(headerData, condition));
				if(sf.getRowCount() > 0) {						// 이미 작성된 계약 문서가 존재할 경우
					Map<String, String> scglData = sf.getListMap().get(0);
					contractNo = scglData.get("CONT_NO");
					contractGlSeq = scglData.get("CONT_GL_SEQ");
					
					String beforeFormNo = MapUtils.getString(scglData, "CONT_FORM_NO", null);
					if(StringUtils.isBlank(beforeFormNo)) {
						docInfoData.put("CONT_NO", contractNo);
						docInfoData.put("CONT_GL_SEQ", contractGlSeq);
					} else {
						if(beforeFormNo.equals(docInfoData.get("CONT_FORM_NO"))) {
							docInfoData.put("CONT_NO", contractNo);
							docInfoData.put("CONT_GL_SEQ", contractGlSeq);
						} else {								// 계약 서식을 변경한 경우 기존 문서를 제거후 새로 작성
							sxp = new SepoaXmlParser(this, "doDeleteSSCGL");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							ssm.doDelete(scglData);
							
							createContractFlag = true;
						}
					}
					
					/*
					 * 작성된 계약 문서가 존재하지만, 해당 문서에 계약 문서 파일이 존재하지 않을 경우
					 * SMR_NUMBER 번호 기준으로 날인 완료된 문서 파일을 복사하는 작업을 진행한다.
					 * (예 : 계약대상현황에서 계약 문서 생성 전에 기타 첨부 파일을 업로드 한 경우 계약 문서 파일(CONT_C)이 없는 임시 계약 문서가 생성되기 때문에 파일 복사가 필요하다.)
					 * */
					FileManager fm = new FileManager(info, config, ctx);
					String docNumber = contractNo + "_" + contractGlSeq;
					fm.setDocNumber(docNumber);
					fm.setLayer("CONT_C");
					List<Map<String, String>> contFileList = fm.doList();
					if(contFileList == null || contFileList.size() == 0) {
						copyContractFileFlag = true;
					}
				} else {
					createContractFlag = true;
				}
				
				if(createContractFlag) {						// 날인 완료 문서 파일 복사 및 계약 문서 생성 
					contractNo = DocumentUtil.getDocNumber(info, "CT").result[0];
					contractGlSeq = "1";
					docInfoData.put("CONT_NO", contractNo);
					docInfoData.put("CONT_GL_SEQ", contractGlSeq);
					
					method = "doDocumentCreate";				// 임시저장 계약 문서 생성
					so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {paramData});
					if(so.flag) {
						copyContractFileFlag = true;
					} else {
						throw new Exception(so.message);
					}
				}
				
				if(copyContractFileFlag) {
					sxp = new SepoaXmlParser(this, "doQuerySSIPD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					
					sf = new SepoaFormater(ssm.doSelect(headerData));
					
					FileManager fm = new FileManager(info, config, ctx);
					List<Map<String, String>> copyList = new ArrayList<>();
					List<Map<String, String>> fileTargetList = sf.getListMap();
					for(Map<String, String> target : fileTargetList) {
						String smrNumber = MapUtils.getString(target, "SMR_NUMBER");
						String smdNumber = MapUtils.getString(target, "SMD_NUMBER");
						String docNumber = smrNumber + "_" + smdNumber;
						if(fm.setDocNumber(docNumber) != null) {
							List<Map<String, String>> fileList = fm.doList();
							for(Map<String, String> file : fileList) {
								if(MapUtils.getString(file, "LAYER", "").startsWith("CONVERT_SEAL")) {
									copyList.add(file);
								}
							}
						}
					}
					
					if(copyList.size() > 0) {
						String docNumber = contractNo + "_" + contractGlSeq;
						fm.setDocNumber(docNumber);
						fm.setLayer("CONT_C");
						
						for(Map<String, String> file : copyList) {
							fm.doCopy(file.get("DOC_NUMBER"), file.get("FILE_ID"), "CONT_C");
						}
						
						for(Map<String, String> file : fm.doList()) {
							file.put("FILE_TYPE", "C");
							fm.doModifyMeta(file);
						}
					}
				}
			} else {
				if(!StringUtils.isBlank(headerData.get("CONT_NO")) && !StringUtils.isBlank(headerData.get("CONT_GL_SEQ"))) {
					contractNo = headerData.get("CONT_NO");
					contractGlSeq = headerData.get("CONT_GL_SEQ");
					
					docInfoData.put("CONT_NO", contractNo);
					docInfoData.put("CONT_GL_SEQ", contractGlSeq);
					contractData.put("CONT_FORM_MAPPING", "Y");		// 현재 계약서 정보들을 FIX_HTML에 매핑시키기 위한 설정
				} else {										// 신규 계약서를 작성할 경우
					method = "doDocumentCreateIF";
					so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {contractData});
					if(so.flag) {
						contractNo = so.result[0];
						contractGlSeq = so.result[1];
						
						docInfoData.put("CONT_NO", contractNo);
						docInfoData.put("CONT_GL_SEQ", contractGlSeq);
					} else {
						throw new Exception(so.message);
					}
				}
			}
			
			if(contractNo != null && contractGlSeq != null) {
				contractData.put("CONT_NO",       contractNo);
				contractData.put("CONT_GL_SEQ",   contractGlSeq);
				contractData.put("NEXT_STEP",     headerData.get("NEXT_STEP"));
				
				/*
				 * 기존 저장된 Extra 데이터와 현재 세팅된 Extra 데이터 병합 구간
				 * */
				method = "doDocumentExtra";
				so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {paramData});
				if(so.flag) {
					Map<String, String> beforeDocItem = (Map<String, String>) so.getData(0);
					beforeDocItem.putAll(docItem);
					contractData.put("DOC_ITEM", beforeDocItem);
				} else {
					throw new Exception(so.message);
				}
				
				/*
				 * 입력된 템플릿 데이터를 SCTTI 에 저장한다.
				 */
				if(contractData.get("TEMPLATE_MAPPING_DATA") != null) {
					so = doSaveTemplateMappingData(contractData);
					if(so.flag) {
					} else {
						throw new Exception(so.message);
					}
				}
				
				// 계약 리스너 처리를 위한 감시 대상 테이블(SMCWT) 데이터 적재
				String docNumber = contractNo + "_" + contractGlSeq;
				
				Map<String, String> condition = new HashMap<>();
				condition.put("DOC_NUMBER", docNumber);
				
				sxp = new SepoaXmlParser(this, "doDeleteSMCWT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doDelete(condition);
				
				Map<String, String> mcwtData = new HashMap<>();
				mcwtData.putAll(headerData);
				mcwtData.put("DOC_NUMBER", docNumber);
				mcwtData.put("POSTPROCESSED", "W");
				mcwtData.put("PRC_TYPE", "EMPLOYEE");
				mcwtData.put("ATTACH_NUMBER", MapUtils.getString(headerData, "ATTACH_NUMBER", ""));
				
				Map<String, Object> data = new HashMap<>();
				data.put("SIGNER_GROUP", MapUtils.getObject(jsonData, "SIGNER_GROUP", "[]"));
				
				Map<String, String> creatorInfo = new HashMap<>();
				creatorInfo.put("HOUSE_CODE",		info.getSession("HOUSE_CODE"));
				creatorInfo.put("COMPANY_CODE",		info.getSession("COMPANY_CODE"));
				creatorInfo.put("ID",				info.getSession("ID"));
				creatorInfo.put("DEPARTMENT",		info.getSession("DEPARTMENT"));
				creatorInfo.put("IP",				ThreadLocalFilter.getClientIP());
				
				data.put("CREATOR_INFO", creatorInfo);
				
				mcwtData.put("DATA", new Gson().toJson(data));
				
				sxp = new SepoaXmlParser(this, "doInsertSMCWT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(mcwtData);
				
				// 파일 중복 저장 방지
				contractData.remove("EXTRA_FILES");
				
				paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, contractData);
				paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, contractData);
				method = "doDocumentUpdate";					// 계약 문서 정보 업데이트
				so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {paramData});
				if(so.flag) {
					paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, docInfoData);
					paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfoData);
					
					method = "doDocumentDetail";
					so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {paramData});
					if(so.flag) {
						List<Object> result = so.getAllData();
						if(result.size() > 0) {
							Map<String, Object> detail = (Map<String, Object>) result.get(0);
							Map<String, String> docInfo = MapUtils.getMap(detail, "DOC_INFO_DATA");
							List<Map<String, String>> signerInfo = (List<Map<String, String>>) MapUtils.getObject(detail, "SIGNER", null);
							this.doSaveContractData(ctx, allData, docInfo, signerInfo);
							
							if(contractData.get("TEMPLATE_MAPPING_DATA") != null) {
								Map<String,String> map = (Map<String,String>) contractData.get("TEMPLATE_MAPPING_DATA");
								for(Entry templateMappingDataEntry : map.entrySet()) {
									docInfo.put((String) templateMappingDataEntry.getKey(), (String) templateMappingDataEntry.getValue());
								}
							}
							
							setValue(docInfo);		// 편집창 오픈을 위한 계약 데이터 결과값 반환
							
							if("DC".equals(MapUtils.getString(docInfo, "STATUS", ""))) {
								condition = new HashMap<>();
								condition.put("DOC_NUMBER", docNumber);
								
								String postProcessed = "N";
								if("true".equalsIgnoreCase(CommonUtil.getConfig("sepoa.server.development.flag", "false"))) {
									postProcessed += this.getUniqueString();
								}
								condition.put("POSTPROCESSED", postProcessed);
								
								sxp = new SepoaXmlParser(this, "doUpdateSMCWT_STATUS");
								ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
								ssm.doUpdate(condition);
							}
							
							Commit();
						}
					}
				} else {
					throw new Exception(so.message);
				}
			}
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
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
	
	@Description("계약 진행 상태 업데이트 콜백")
	public SepoaOut doContStatusUpdate(Map<String, String> docInfo, Map<String, String> signerInfo) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		Map<String, String> scgl = null;
		
		try {
			if(docInfo.size() > 0) {
				Map<String, String> condition = new HashMap<>();
				condition.put("MODE", "__CONT_NO__");
				
				sxp = new SepoaXmlParser(this, "doQuerySSCGL");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				
				SepoaFormater sf = new SepoaFormater(ssm.doSelect(docInfo, condition));
				if(sf != null && sf.getRowCount() > 0) {
					scgl = sf.getListMap().get(0);
					
					sxp = new SepoaXmlParser(this, "doUpdateSSCGL");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					
					ssm.doUpdate(docInfo);
				}
				
				if(signerInfo.size() > 0) {
					sxp = new SepoaXmlParser(this, "doQuerySSCSI");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					
					sf = new SepoaFormater(ssm.doSelect(docInfo, signerInfo));
					List<Map<String, String>> scsiList = sf.getListMap();
					if(scsiList.size() > 0) {
						for(int i = 0; i < scsiList.size(); i++) {
							Map<String, String> data = new HashMap<>();
							data.put("CONT_NO", docInfo.get("CONT_NO"));
							data.put("CONT_GL_SEQ", docInfo.get("CONT_GL_SEQ"));
							data.put("SIGNER_ID", signerInfo.get("SIGNER_ID"));
							data.put("SEQ", signerInfo.get("SEQ"));
							data.put("SIGNER_COMPANY_CODE", signerInfo.get("SIGNER_COMPANY_CODE"));
							data.put("FIXED_SIGNER_NAME", signerInfo.get("SIGNER_NAME"));
							data.put("FIXED_SIGNER_EMAIL", signerInfo.get("SIGNER_EMAIL"));
							data.put("FIXED_SIGNER_PHONE", signerInfo.get("SIGNER_PHONE"));
							data.put("FIXED_IRS_NO", signerInfo.get("SIGNER_KEY"));
							
							sxp = new SepoaXmlParser(this, "doUpdateSSCSI");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							
							ssm.doUpdate(data);
						}
					}
				}
			}
			
			if("DC".equals(MapUtils.getString(docInfo, "STATUS", ""))) {
				String docNumber = docInfo.get("CONT_NO") + "_" + docInfo.get("CONT_GL_SEQ");
				Map<String, String> condition = new HashMap<>();
				condition.put("DOC_NUMBER", docNumber);
				
				String postProcessed = "N";
				if("true".equalsIgnoreCase(CommonUtil.getConfig("sepoa.server.development.flag", "false"))) {
					postProcessed += this.getUniqueString();
				}
				condition.put("POSTPROCESSED", postProcessed);
				
				sxp = new SepoaXmlParser(this, "doUpdateSMCWT_STATUS");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
				ssm.doUpdate(condition);
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
	@Description("결재 요청")
	public SepoaOut doApprovalRequest(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
			if(headerData.containsKey("APP_LINE")) {
				List<Map<String, String>> appLine = (List<Map<String, String>>) jsonData.get("APP_LINE");
				for(Map<String, String> app : appLine) {
					String appStage = Integer.toString(MapUtils.getInteger(app, "APP_STAGE"));
					String appLineSeq = Integer.toString(MapUtils.getInteger(app, "APP_LINE_SEQ"));
					
					app.put("APP_STAGE", appStage);
					app.put("APP_LINE_SEQ", appLineSeq);
					app.put("COMPANY_CODE", app.get("SELLER_CODE"));
				}
				
				allData.put("APP_LINE", appLine);
			}
			if(headerData.containsKey("APP_FORM_DATA")) {
				Map<String, String> appFormData = (Map<String, String>) jsonData.get("APP_FORM_DATA");
				headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, MapUtils.getString(appFormData, "APP_USER_ID",""));
				headerData.put(SepoaDataMapper.KEY_SESSION_USER_NAME, appFormData.get("APP_USER"));
				headerData.putAll(appFormData);
			}

			
			if(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID) == null ||
					headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID).equals("")) {
				headerData.put(SepoaDataMapper.KEY_SESSION_USER_ID, info.getSession("ID"));
			}

			if(headerData.containsKey("DOC_INFO_DATA")) {
				Map<String, String> docInfo = (Map<String, String>) jsonData.get("DOC_INFO_DATA");
				headerData.put("SUX_WKEY3", MapUtils.getString(docInfo,"CONT_NO"));
				headerData.put("SUX_WKEY4", MapUtils.getString(docInfo,"CONT_GL_SEQ"));
				headerData.put("APP_SUBJECT", MapUtils.getString(docInfo,"CONT_SUBJECT","")); 
				headerData.put("APP_USER_AMT", MapUtils.getString(docInfo,"CONT_AMT",""));
			}
			
			headerData.put("SUX_WKEY1", info.getSession("HOUSE_CODE"));
			headerData.put("SUX_WKEY2",info.getSession("COMPANY_CODE"));

			SepoaApprovalLineSetting sepoaApprovalLineSetting = new SepoaApprovalLineSetting(ctx, info);
			sepoaApprovalLineSetting.doApprovalProcess(allData);
			Commit();

			// 업무단에서 리턴받아서 처리할 키값들을 셋팅한다.
			Map<String, String> map = new HashMap<>();
			map.put("returnType" , "param");
			map.put("APP_NO"	 , MapUtils.getString(headerData, "APP_NO"	 , ""));
			map.put("APP_NO_SEQ" , MapUtils.getString(headerData, "APP_NO_SEQ" , ""));
			map.put("APP_STATUS" , MapUtils.getString(headerData, "APP_STATUS" , "T"));
			setObject(map);
			List<Map<String, String>> temp = new ArrayList();
			temp.add(map);
			setValue((new SepoaFormater(temp)).getString());
			
			Configuration conf = new Configuration();
			Map<String, String> mailCustomData = new HashMap<String, String>();
			
			mailCustomData.put("URL", conf.get("sepoa.system.domain.name", "seal.poacloud.net"));
			mailCustomData.put("SUBJECT", headerData.get("APP_SUBJECT"));
			allData.put("MAIL_CUSTOM_DATA", mailCustomData);
			 
			SepoaApprovalProgress sepoaApprovalProgress = new SepoaApprovalProgress(ctx, info);
			sepoaApprovalProgress.doApprovalProcess(allData);
			Commit();
			
		} catch(Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("approval request failed");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("결재 상신")
	public SepoaOut doApprovalProgress(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		String method = "doApprovalStart";
		String fileTempDirectory = null;
		try {
			Config config = new Configuration();
			allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			
			List<Map<String, String>> appLine = (List<Map<String, String>>)allData.get("APP_LINE");
			if(appLine != null && appLine.size() > 0) {
				List<Map<String, String>> docAuthList = new ArrayList<Map<String, String>>();

				for(Map<String, String> app : appLine) {
					String appCompanyCode = MapUtils.getString(app, "COMPANY_CODE", null);
					String appUserId = MapUtils.getString(app, "APP_USER_ID", null);
					
					if(appCompanyCode != null && appUserId != null) {
						Map<String, String> author = new HashMap<String, String>();
						author.put("COMPANY_CODE", appCompanyCode);
						author.put("USER_ID", appUserId);
						
						docAuthList.add(author);
					}
					
				}
				
				allData.put("DOC_AUTH_LIST", docAuthList);
			}
			
			if(!allData.containsKey(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY)) {
				PathManager pm = new PathManager(config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			}
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {allData});
			setFlag(so.flag);
			setStatus(so.status);
			setMessage(so.message);
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("doService SepoaContractService." + method + " failed.");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
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
	
	@SuppressWarnings("unchecked")
	@Description("결재 완료")
	public SepoaOut doApprovalConfirm(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		String method = "doApprovalFinish";
		String fileTempDirectory = null;
		try {
			Config config = (Config) MapUtils.getObject(allData, SepoaModuleDataMapper.KEY_CONFIGURATION, null);
			if(config == null) {
				config = new Configuration();
				allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			}
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			headerData.put("CONT_NO", MapUtils.getString(headerData, "SUX_WKEY3", null));
			headerData.put("CONT_GL_SEQ", MapUtils.getString(headerData, "SUX_WKEY4", null));
			
			if(!allData.containsKey(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY)) {
				PathManager pm = new PathManager(config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			}
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {allData});
			setFlag(so.flag);
			setStatus(so.status);
			setMessage(so.message);
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("doService SepoaContractService." + method + " failed.");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
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
	
	@SuppressWarnings("unchecked")
	@Description("결재 반려")
	public SepoaOut doApprovalReject(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		String method = "doApprovalReject";
		try {
			Config config = new Configuration();
			allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			headerData.put("CONT_NO", MapUtils.getString(headerData, "SUX_WKEY3", null));
			headerData.put("CONT_GL_SEQ", MapUtils.getString(headerData, "SUX_WKEY4", null));
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {allData});
			setFlag(so.flag);
			setStatus(so.status);
			setMessage(so.message);
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("doService SepoaContractService." + method + " failed.");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("결재 회수")
	public SepoaOut doApprovalWithdraw(ConnectionContext ctx, Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		String method = "doDocumentRewrite";
		try {
			Config config = new Configuration();
			allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			headerData.put("CONT_NO", MapUtils.getString(headerData, "SUX_WKEY3", null));
			headerData.put("CONT_GL_SEQ", MapUtils.getString(headerData, "SUX_WKEY4", null));
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {allData});
			setFlag(so.flag);
			setStatus(so.status);
			setMessage(so.message);
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("doService SepoaContractService." + method + " failed.");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		
		return getSepoaOut();
	}
	
	@Description("전자계약 파일 목록 조회 - vault custom method")
	public SepoaOut doFileList(Map<String, String> fileInfo) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		String fileTempDirectory = null;
		
		try {
			SepoaFormater result = null;
			
			String[] docKey = MapUtils.getString(fileInfo, "attach_key", "").split("#");
			if(docKey.length == 3) {
				String contNo = docKey[0];
				String contGlSeq = docKey[1];
				String layer = docKey[2];
				
				Map<String, String> docInfo = new HashMap<>();
				docInfo.put("CONT_NO", contNo);
				docInfo.put("CONT_GL_SEQ", contGlSeq);
				
				Map<String, Object> paramData = new HashMap<>();
				paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, docInfo);
				paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfo);
				paramData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, paramData);
				
				Config config = new Configuration();
				paramData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
				
				PathManager pm = new PathManager(info, config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				paramData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
				
				String method = "doFileList";
				SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {paramData});
				if(so.flag) {
					List<Object> fileResult = so.getAllData();
					if(fileResult.size() > 0) {
						List<Map<String, String>> fileList = (List<Map<String, String>>) fileResult.get(0);
						fileList = fileList.stream().filter(file -> file.get("LAYER").equalsIgnoreCase(layer)).collect(Collectors.toList());
						
						List<Map<String, String>> resultFileList = new ArrayList<>(fileList.size());
						for(Map<String, String> file : fileList) {
							Map<String, String> data = new HashMap<>();
							data.put("DES_FILE_NAME", file.get("FILE_NAME_SERVER"));
							data.put("SRC_FILE_NAME", file.get("FILE_NAME_CLIENT"));
							data.put("DOC_SEQ", file.get("DOC_SEQ"));
							data.put("FILE_SIZE", String.valueOf(MapUtils.getInteger(file, "FILE_SIZE")));
							data.put("TYPE", file.get("LAYER"));
							data.put("FILE_ID", file.get("FILE_ID"));
							
							resultFileList.add(data);
						}
						
						if(resultFileList.size() > 0) {
							result = new SepoaFormater(resultFileList);
						}
					}
				} else {
					throw new Exception(so.message);
				}
			}
			
			if(result == null) {
				Map<String, String> dummy = new HashMap<>();
				dummy.put("DES_FILE_NAME", "");
				dummy.put("SRC_FILE_NAME", "");
				dummy.put("DOC_SEQ", "");
				dummy.put("FILE_SIZE", "");
				dummy.put("TYPE", "");
				
				SepoaFormater sf = new SepoaFormater(dummy);
				sf.removeAllValue();
				result = sf;
			}
			
			setValue(result);
			
			SepoaFormater maxSeqResult = new SepoaFormater(new SingletonMap("MAX_SEQ", String.valueOf(result.getRowCount())));
			setValue(maxSeqResult);
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
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
	
	@Description("전자계약 파일 업로드 - vault custom method")
	public SepoaOut doFileUpload(Map<String, String> fileInfo) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		String fileTempDirectory = null;
		SepoaOut so = null;
		
		try {
			Map<String, String> docInfo = null;
			
			Map<String, String> headerData = new HashMap<>();
			headerData.put("FILE_TYPE", fileInfo.get("TYPE").replaceAll("CONT_", ""));
			headerData.put("CONT_METHOD", "D");
			headerData.put("SIGN_ORDER", "S");
			headerData.put("NEXT_STEP", "MODULE");
			
			Map<String, Object> paramData = new HashMap<>();
			paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, headerData);
			paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, headerData);
			paramData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, paramData);
			
			Config config = new Configuration();
			paramData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			
			PathManager pm = new PathManager(info, config);
			pm.setLayer(PathManager.LAYER_TEMP);
			fileTempDirectory = pm.getUniquePath();
			paramData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			
			List<Map<String, String>> fileList = new ArrayList<>();		//List but only one file
			Map<String, String> fileData = new HashMap<>();
			pm.setLayer(fileInfo.get("TYPE"));
			fileData.put("PATH", pm.getLayerPath() + "/" + fileInfo.get("UNIQ_FILE_NAME"));
			fileData.put("FILE_NAME_CLIENT", fileInfo.get("FILE_NAME"));
			
			fileList.add(fileData);
			paramData.put(SepoaModuleDataMapper.KEY_FILE_DATA, fileList);
			
			String[] docKey = MapUtils.getString(fileInfo, "ATTACH_KEY", "").split("#");
			if(docKey.length == 3) {
				String contNo = docKey[0];
				String contGlSeq = docKey[1];
				String layer = docKey[2];
				
				headerData.put("CONT_NO", contNo);
				headerData.put("CONT_GL_SEQ", contGlSeq);
				
				String method = "doDocumentDetail";
				so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {paramData});
				if(so.flag) {
					List<Object> result = so.getAllData();
					if(result.size() > 0) {
						Map<String, Object> detail = (Map<String, Object>) result.get(0);
						docInfo = MapUtils.getMap(detail, "DOC_INFO_DATA");
					}
				} else {
					so = null;
				}
			}
			
			if(so == null) {
				so = doSaveContract(paramData);
				if(so.getDataCount() > 0) {
					docInfo = (Map<String, String>) so.getData(0);
					headerData.putAll(docInfo);
				}
			}
			
			if(docInfo != null) {
				docInfo.entrySet().removeIf(e -> StringUtils.isBlank(e.getValue()));
				setObject(docInfo);
			}
			
			String method = "doFileUpload";
			ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {paramData});
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
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
	
	@Description("전자계약 파일 삭제 - vault custom method")
	public SepoaOut doFileDelete(Map<String, String> fileInfo) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		String fileTempDirectory = null;
		
		try {
			String[] docKey = MapUtils.getString(fileInfo, "attach_key", "").split("#");
			if(docKey.length == 3) {
				String contNo = docKey[0];
				String contGlSeq = docKey[1];
				
				Map<String, String> docInfo = new HashMap<>();
				docInfo.put("CONT_NO", contNo);
				docInfo.put("CONT_GL_SEQ", contGlSeq);
				
				Map<String, Object> paramData = new HashMap<>();
				paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, docInfo);
				paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfo);
				
				Config config = new Configuration();
				paramData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
				
				PathManager pm = new PathManager(info, config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				paramData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
				
				SepoaOut so = doFileList(fileInfo);
				if(so.getDataCount() == 2) {
					List<Map<String, String>> fileList = ((SepoaFormater)so.getData(0)).getListMap();
					if(fileList.size() > 0) {
						List<Map<String, String>> target = fileList.stream().filter(file -> {
							return file.get("DES_FILE_NAME").equals(fileInfo.get("DES_FILE_NAME"));
						}).collect(Collectors.toList());
						
						if(target.size() > 0) {
							String fileId = target.get(0).get("FILE_ID");
							docInfo.put("FILE_ID", fileId);
							
							String method = "doFileDelete";
							so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaEmployeeContractService", ctx, method, new Object[] {paramData});
							if(so.flag) {
								
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
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
	
	protected boolean checkCreatePermission(Map<String, Object> allData) throws Exception {
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		String contType = MapUtils.getString(headerData, "CONT_TYPE", "");	// 계약 타입 (B : 거래처 계약, E : 임직원 계약)
		if("B".equals(contType)) {
			return true;
		}
		
		List<String> ctrlCodeList = Arrays.asList(info.getSession("CTRL_CODE").split(","));
		boolean hasEmpContCreate = ctrlCodeList.contains("EMP_CONT_CREATE");
		
		return hasEmpContCreate;
	}
	
	protected void checkDataValidate(Map<String, Object> allData) throws Exception {
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		
		List<Map<String, String>> signerList = (List<Map<String, String>>) jsonData.get("SIGNER");
		if(signerList.isEmpty()) {
			throw new Exception("SIGNER Data is empty.");
		}
		
		List<Map<String, String>> signerGroupList = (List<Map<String, String>>) jsonData.get("SIGNER_GROUP");
		if(signerGroupList.isEmpty()) {
			throw new Exception("SIGNER_GROUP Data is empty.");
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, String> getDocInfoData(Map<String, Object> allData) {
		Map<String, String> docInfoData = new HashMap<String, String>();
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		docInfoData.put("CONT_FORM_NO",  headerData.get("CONT_FORM_NO"));						// 계약 서식 번호
		docInfoData.put("CONT_TYPE",     headerData.get("CONT_TYPE"));							// 계약 타입 (B : 거래처 계약, E : 임직원 계약)
		docInfoData.put("CONT_SUBJECT",  headerData.get("CONT_SUBJECT"));						// 계약서 명
		docInfoData.put("CATEGORY"   ,   headerData.get("CATEGORY"));							// 계약 카테고리 [공통코드 : ECT004]
		docInfoData.put("CONT_METHOD",   headerData.get("CONT_METHOD"));						// 서명방식 [공통코드 : ECT003]
		docInfoData.put("CONT_DATE"  ,   headerData.get("CONT_DATE"));							// 계약 체결일 [YYYYMMDD]
		docInfoData.put("CONT_FROM"  ,   headerData.get("CONT_FROM"));							// 계약 효력 시작일 [YYYYMMDD]
		docInfoData.put("CONT_TO",       headerData.get("CONT_TO"));							// 계약 효력 만료일 [YYYYMMDD]
		docInfoData.put("EXPIRED_ALARM", headerData.get("EXPIRED_ALARM"));						// 만기도래 알림 [Y / N]
		docInfoData.put("CONT_AMT",      MapUtils.getString(headerData, "CONT_AMT", "0"));		// 계약금액
		docInfoData.put("SIGN_ORDER",    MapUtils.getString(headerData, "SIGN_ORDER", "S"));							// 서명 순서 (S : 순서대로, P : 순서무시)
		docInfoData.put("NEXT_STEP",     MapUtils.getString(headerData, "NEXT_STEP", "EMPLOYEE_CREATE"));
		docInfoData.put("DATA_SOURCE",   "SEAL");												// 계약 문서 생성 솔루션
		docInfoData.put("CONT_CASE",     headerData.get("CONT_CASE"));							// 계약방식 (SINGLE : 단일, MULTIPLE : 다중)
		docInfoData.put("CONT_NO",       headerData.get("CONT_NO"));							// 계약번호
		
		if(StringUtils.isBlank(docInfoData.get("CONT_TYPE"))) {
			// 임직원 계약용 서비스 이므로 기본값 E로 설정
			docInfoData.put("CONT_TYPE", "E");
		}
		
		if(!StringUtils.isBlank(headerData.get("SMR_NUMBER"))) {
			docInfoData.put("SOURCE_NO", headerData.get("SMR_NUMBER"));
		}
		
		if(headerData.containsKey("EDITOR_HTMLS")) {
			docInfoData.put("EDITOR_HTMLS", "__CONTAINS_KEY__");
		}
		
		docInfoData.entrySet().removeIf(entry -> StringUtils.isBlank(entry.getValue()));
		
		return docInfoData;
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, String> getExtraDocInfoData(Map<String, Object> allData) {
		Map<String, String> extraDocInfo = new HashMap<>();
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		if(!StringUtils.isBlank(headerData.get("SMR_NUMBER"))) {
			extraDocInfo.put("EX_SOURCE_NO", headerData.get("SMR_NUMBER"));
		}
		
		if(StringUtils.isBlank(MapUtils.getString(headerData, "COMPANY_SIGN", "FIRST")) == false) {
			extraDocInfo.put("EX_COMPANY_SIGN", MapUtils.getString(headerData, "COMPANY_SIGN", "FIRST"));
		}
		
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		if(jsonData != null && SepoaModuleDataMapper.IS_JSON_OBJECT.equals(MapUtils.getString(headerData, "SIGNER", ""))) {
			List<Map<String, String>> signerList = (List<Map<String, String>>) jsonData.get("SIGNER");
			
			List<String> innerSignerName = new ArrayList<>(signerList.size());
			List<String> innerID = new ArrayList<>(signerList.size());
			List<String> innerIRS = new ArrayList<>(signerList.size());
			
			List<String> outerSignerName = new ArrayList<>(signerList.size());
			List<String> outerID = new ArrayList<>(signerList.size());
			List<String> outerIRS = new ArrayList<>(signerList.size());
			for(Map<String, String> signer : signerList) {
				String category = MapUtils.getString(signer, "CATEGORY", "");
				String signerName = MapUtils.getString(signer, "SIGNER_NAME");
				String signerId = MapUtils.getString(signer, "SIGNER_ID", "");
				String signKey = MapUtils.getString(signer, "SIGN_KEY", "");
				
				if("COMPANY".equalsIgnoreCase(category)) {
					innerSignerName.add(signerName);
					innerID.add(signerId);
					innerIRS.add(signKey);
				} else if("EMPLOYEE".equalsIgnoreCase(category)) {
					outerSignerName.add(signerName);
					outerID.add(signerId);
					outerIRS.add(signKey);
				}
			}
			
			if(innerSignerName.size() > 0) {
				extraDocInfo.put("EX_INNER_SIGNER_NAME", StringUtils.join(innerSignerName, ","));
			}
			
			if(innerID.size() > 0) {
				extraDocInfo.put("EX_INNER_ID", StringUtils.join(innerID, ","));
			}
			
			if(innerIRS.size() > 0) {
				extraDocInfo.put("EX_INNER_IRS_NO", StringUtils.join(innerIRS, ","));
			}
			
			if(outerSignerName.size() > 0) {
				extraDocInfo.put("EX_OUTER_SIGNER_NAME", StringUtils.join(outerSignerName, ","));
			}
			
			if(outerID.size() > 0) {
				extraDocInfo.put("EX_OUTER_ID", StringUtils.join(outerID, ","));
			}
			
			if(outerIRS.size() > 0) {
				extraDocInfo.put("EX_OUTER_IRS_NO", StringUtils.join(outerIRS, ","));
			}
		}
		
		for(Entry<String, Object> entry : jsonData.entrySet()) {
			if(entry.getKey().startsWith("EX_") && entry.getValue() instanceof String) {
				extraDocInfo.put(entry.getKey(), (String) entry.getValue());
			}
		}
		
		return extraDocInfo;
	}
	
	@SuppressWarnings("unchecked")
	protected List<Map<String, String>> getSignerListData(Map<String, Object> allData) throws Exception {
		List<Map<String, String>> signerList = null;
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		Config config = (Config) MapUtils.getObject(allData, SepoaModuleDataMapper.KEY_CONFIGURATION, null);
		
		if(jsonData != null) {
			List<Map<String, String>> checkList = new ArrayList<>();
			if(SepoaModuleDataMapper.IS_JSON_OBJECT.equals(MapUtils.getString(headerData, "SIGNER", ""))) {
				signerList = (List<Map<String, String>>) jsonData.get("SIGNER");
				if(signerList != null) {
					for(Map<String, String> signer : signerList) {
						if(signer.containsKey("CATEGORY") == false) {
							signer.put("CATEGORY", "COMPANY");
						}
						
						checkList.add(signer);
					}
				}
			}
			
			if(SepoaModuleDataMapper.IS_JSON_OBJECT.equals(MapUtils.getString(headerData, "SIGNER_GROUP", ""))) {
				List<Map<String, String>> signerGroupList = (List<Map<String, String>>) jsonData.get("SIGNER_GROUP");
				if(signerGroupList != null) {
					Map<String, String> dummySigner = new HashMap<>();
					dummySigner.put("SIGNER_COMPANY_CODE", "SAMPLE_CODE");
					dummySigner.put("SIGNER_COMPANY_NAME", "회사명(견본)");
					dummySigner.put("SIGNER_ID", "SAMPLE_ID");
					dummySigner.put("SIGNER_NAME", "서명자(견본)");
					dummySigner.put("SIGNER_EMAIL", "이메일(견본)");
					
					signerList.add(dummySigner);
					
					checkList.addAll(signerGroupList);
				}
			}
			
			Map<String, Object> checkResult = this.checkSignerListValidate(checkList);
			if(MapUtils.getBoolean(checkResult, "RESULT", false) == false) {
				String msg = "";
				List<String> require = (List<String>) MapUtils.getObject(checkResult, "REQUIRE", null);
				if(require != null) {
					msg = "Require Data is Empty. : [ " + StringUtils.join(require, ", ") + " ]";
				}
				
				List<String> duplicate = (List<String>) MapUtils.getObject(checkResult, "DUPLICATE", null);
				if(duplicate != null) {
					msg = "Duplicate Signer. : [ " + StringUtils.join(require, ", ") + " ]";
				}
				
				if(StringUtils.isBlank(msg)) {
					msg = "Signer Data is Empty.";
				}
				
				throw new Exception(msg);
			}
			
			if(signerList != null) {
				for(int i=0;i<signerList.size();i++) {
					Map<String, String> signer = signerList.get(i);
					signer.put("SEQ", String.valueOf(i+1));
				}
				
				if(headerData.containsKey("CONT_FORM_NO") && !StringUtils.isBlank(headerData.get("CONT_FORM_NO"))) {
					SepoaContractTemplateManager templateManager = new sepoa.contractmk2.SepoaContractTemplateManager(info, config, getConnectionContext());
					if(templateManager.setDocNumber(headerData.get("CONT_FORM_NO"), "1") == null) {
						int errCode = templateManager.getLastErrCode();
						if(errCode != 0) {
							setStatus(errCode);
							setMessage("get contract form info fail");
						}
					}else {
						Map<String, String> templateInfo = templateManager.getDocumentInfo();
						if(signerList.size() > 0) {
							int requiredSignerCnt = 0;
							boolean isSignerCntEquals = true;
							String signerCodeString = MapUtils.getString(templateInfo, "SIGNER_CODE", "");
							if(StringUtils.isBlank(signerCodeString)) {
								isSignerCntEquals = false;
							} else {
								String[] signerCodeArray = signerCodeString.split(",");
								requiredSignerCnt = signerCodeArray.length;
								if(signerList.size() == requiredSignerCnt) {
									for(int i=0;i<signerList.size();i++) {
										Map<String, String> signer = signerList.get(i);
										signer.put("SIGNER_NO", signerCodeArray[i]);
									}
								} else {
									isSignerCntEquals = false;
								}
							}
							
							if(isSignerCntEquals == false) {
								String msg = "선택하신 [" + templateInfo.get("CONT_FORM_NAME") + "] 서식의 계약자는 " + String.valueOf(requiredSignerCnt) + "명이 필요합니다. (현재 : " + String.valueOf(signerList.size()) + " 명)";
								throw new Exception(msg);
							}
						}
					}
				}
				
				List<String> innerTypeList = Arrays.asList(new String[] {"COMPANY", "C"});
				List<String> outerTypeList = Arrays.asList(new String[] {"EMPLOYEE", "S"});
				for(Map<String, String> signer : signerList) {
					String signerType = "";
					String category = MapUtils.getString(signer, "CATEGORY", "");
					if(innerTypeList.contains(category)) {
						signerType = "IN";
					} else if(outerTypeList.contains(category)) {
						signerType = "OUT";
					}
					
					/*
					if("COMPANY".equalsIgnoreCase(category)) {					// 회사측 계약자 : 'COMPANY', SCTGL의 INNER_MANAGER 데이터 적재를 위한 SIGNER_TYPE 설정
						signerType = "IN";
					} else if("EMPLOYEE".equalsIgnoreCase(category)) {			// 임직원 계약자 : 'EMPLOYEE'. SCTGL의 OUTER_MANAGER 데이터 적재를 위한 SIGNER_TYPE 설정
						signerType = "OUT";
					}
					*/
					
					signer.put("SIGNER_TYPE", signerType);
				}
			}
		}
		
		return signerList;
	}
	
	protected Map<String, Object> checkSignerListValidate(List<Map<String, String>> signerList) {
		/*
		 * 
		 * {
		 * 		RESULT : false,
		 * 		INDEX : 0,
		 * 		REQUIRE : [],
		 * 		DUPLICATE : []
		 * }
		 * 
		 * */
		Map<String, Object> result = new HashMap<>();
		if(signerList == null || signerList.isEmpty()) {
			result.put("RESULT", false);
			return result;
		}
		
		List<String> require = new ArrayList<>();
		List<String> duplicate = new ArrayList<>();
		List<String> signerIdList = new ArrayList<>();
		for(Map<String, String> signer : signerList) {
			String signerCompanyCode = signer.get("SIGNER_COMPANY_CODE");
			String signerCompanyName = signer.get("SIGNER_COMPANY_NAME");
			String signerId = signer.get("SIGNER_ID");
			String signerName = signer.get("SIGNER_NAME");
			String signerEmail = signer.get("SIGNER_EMAIL");
			
			if(StringUtils.isBlank(signerCompanyCode)) {
				require.add("SIGNER_COMPANY_CODE");
			}
			if(StringUtils.isBlank(signerCompanyName)) {
				require.add("SIGNER_COMPANY_NAME");
			}
			if(StringUtils.isBlank(signerId)) {
				require.add("SIGNER_ID");
			}
			if(StringUtils.isBlank(signerName)) {
				require.add("SIGNER_NAME");
			}
			if(StringUtils.isBlank(signerEmail)) {
				require.add("SIGNER_EMAIL");
			}
			
			if(StringUtils.isBlank(signerId) == false) {
				if(signerIdList.contains(signerId)) {
					duplicate.add(signerId);
				} else {
					signerIdList.add(signerId);
				}
			}
			
			if(require.size() > 0) {
				result.put("RESULT", false);
				result.put("INDEX", signerList.indexOf(signer));
				result.put("REQUIRE", require);
				
				return result;
			}
			
			if(duplicate.size() > 0) {
				result.put("RESULT", false);
				result.put("INDEX", signerList.indexOf(signer));
				result.put("DUPLICATE", duplicate);
				
				return result;
			}
		}
		
		result.put("RESULT", true);
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	protected List<List<Map<String, String>>> getMultiSignerListData(Map<String, Object> allData) throws Exception {
		List<Map<String, String>> signerList = null;
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		
		List<List<Map<String, String>>> groupSignerList = new ArrayList<>();
		Map<String, String> dummySigner = new HashMap<>();
		
		// 편집창에 표기할 더미데이터 세팅
		dummySigner.put("SIGNER_COMPANY_CODE", "DUMMY_CODE");
		dummySigner.put("SIGNER_COMPANY_NAME", Cache.text.get("POA_CONTRACT.TARGET_COMPANY"));
		dummySigner.put("SIGNER_ID", "TARGET_ID");
		dummySigner.put("SIGNER_NAME", Cache.text.get("POA_CONTRACT.TARGET_NAME"));
		dummySigner.put("SIGNER_EMAIL", "TARGET_EMAIL");
		dummySigner.put("SEQ", "2");
		dummySigner.put("SIGNER_PHONE", "TARGET_PHONE");
		dummySigner.put("SIGNER_COMPANY_OWNER", Cache.text.get("POA_CONTRACT.TARGET_COMPANY_OWNER"));
		dummySigner.put("SIGNER_COMPANY_ADDR", Cache.text.get("POA_CONTRACT.TARGET_ADDR"));
		dummySigner.put("SIGN_KEY", Cache.text.get("POA_CONTRACT.TARGET_SIGN_KEY"));
		dummySigner.put("CATEGORY", "S");
		dummySigner.put("SIGNER_TYPE", "TARGET_SIGNER_TYPE");
		
		
		if(jsonData != null && SepoaModuleDataMapper.IS_JSON_OBJECT.equals(MapUtils.getString(headerData, "SIGNER", ""))) {
			signerList = (List<Map<String, String>>) jsonData.get("SIGNER");
			Map<String , String> fixSigner = new HashMap<>();
			
			for(int i=0; i < signerList.size(); i++) {
				List<Map<String, String>> groupSginer = new ArrayList<>();
				if(signerList.get(i).get("CATEGORY").equals("C")) {
					fixSigner.putAll(signerList.get(i));
				}
				
				Map<String, String> signer = new HashMap<>();
				signer = signerList.get(i);
				signer.put("SIGNER_ID", signer.get("SIGNER_ID").toUpperCase());
				
				String signerType = null;
				if("C".equals(signer.get("CATEGORY"))) {					// 도급처 : 'C', SCTGL의 INNER_MANAGER 데이터 적재를 위한 SIGNER_TYPE 설정
					signerType = "IN";
					fixSigner.put("SIGNER_TYPE", signerType);
				} else {													// 수급처 : 'S'. SCTGL의 OUTER_MANAGER 데이터 적재를 위한 SIGNER_TYPE 설정
					signerType = "OUT";
					signer.put("SIGNER_TYPE", signerType);
				}
				
				if(i == 0) {
					groupSginer.add(fixSigner);
					groupSginer.add(dummySigner);
				} else {
					groupSginer.add(fixSigner);
					groupSginer.add(signer);
				}
				
				if(groupSginer.size() > 1) {
					groupSignerList.add(groupSginer);
				}
			}
		}
		
		return groupSignerList;
	}
	
	@SuppressWarnings("unchecked")
	protected List<Map<String, String>> getDocAuthList(Map<String, Object> allData) throws Exception {
		List<Map<String, String>> docAuthList = null;
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		if(docAuthList == null) {
			docAuthList = new ArrayList<>();
		}
		
//		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
//		
//		sxp = new SepoaXmlParser(this,"doQueryWorkTypeZ");
//		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
//		
//		List<Map<String, String>> targetList = new SepoaFormater(ssm.doSelect(new HashMap<String, String>())).getListMap();
//		for(Map<String,String> target : targetList) {
//			Map<String, String> auth = new HashMap<String, String>();
//			auth.put("COMPANY_CODE", target.get("COMPANY_CODE"));
//			auth.put("TARGET_CODE", target.get("USER_ID").toUpperCase());
//			auth.put("PERMISSION", Authorized.READ.toString());
//			
//			docAuthList.add(auth);
//		}
		
		return docAuthList;
	}
	
	protected List<Map<String, String>> getExtraFileList(Map<String, Object> allData) throws Exception {
		List<Map<String, String>> extraFileList = null;
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		
		if(headerData.containsKey("EXTRA_FILES")) {
			extraFileList = (List<Map<String, String>>) jsonData.get("EXTRA_FILES");
		}
		
		return extraFileList;
	}
	
	@SuppressWarnings("unchecked")
	@Description("전자인장 내 계약테이블 데이터 저장")
	protected void doSaveContractData(ConnectionContext ctx, Map<String, Object> allData, Map<String, String> docInfo, List<Map<String, String>> signerList) throws Exception {
		/*
		 * 전자인장에서 관리하는 전자계약 정보 테이블 데이터 삽입 메소드
		 * 계약문서 헤더 테이블	: SSCGL <-> SCTGL
		 * 계약문서 서명자 테이블	: SSCSI <-> SSIGN
		 * 
		 * */
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		String contNo = docInfo.get("CONT_NO");
		String contGlSeq = docInfo.get("CONT_GL_SEQ");
		
		Map<String, String> condition = new HashMap<>();
		if(!StringUtils.isBlank(contNo) && !StringUtils.isBlank(contGlSeq)) {
			condition.put("MODE", "__CONT_NO__");
			condition.put("CONT_NO", contNo);
			condition.put("CONT_GL_SEQ", contGlSeq);
			condition.put("SOURCE_NO", MapUtils.getString(headerData, "SMR_NUMBER", ""));
		} else if(!StringUtils.isBlank(headerData.get("SMR_NUMBER"))) {
			condition.put("MODE", "__SOURCE_NO__");
			condition.put("SOURCE_NO", MapUtils.getString(headerData, "SMR_NUMBER", ""));
		}
		
		if(headerData.containsKey("SELLER_EXPIRE_ALARM")) {
			String sellerExpireAlarm = MapUtils.getString(headerData, "SELLER_EXPIRE_ALARM", "");
			if("1".equals(sellerExpireAlarm)) {
				condition.put("EXPIRED_ALARM", "G");
			}
		}
		
		if(condition.size() == 0) {
			sf = null;
		} else {
			sxp = new SepoaXmlParser(this, "doQuerySSCGL");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			sf = new SepoaFormater(ssm.doSelect(condition));
		}
		
		if(sf != null && sf.getRowCount() > 0) {
			sxp = new SepoaXmlParser(this, "doUpdateSSCGL");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doUpdate(docInfo, condition);
		} else {
			sxp = new SepoaXmlParser(this, "doInsertSSCGL");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doInsert(docInfo, condition);
		}
		
		if(signerList != null && signerList.size() > 0) {
			sxp = new SepoaXmlParser(this, "doDeleteSSCSI");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doDelete(docInfo);
			
			List<Map<String, String>> frontSignerList = null;
			if(jsonData != null) {
				frontSignerList = (List<Map<String, String>>) MapUtils.getObject(jsonData, "SIGNER", null);
			}
			
			List<Map<String, String>> scsiData = new ArrayList<>(signerList.size());
			for(Map<String, String> signer : signerList) {
				Map<String, String> data = new HashMap<>();
				data.put("CONT_NO", contNo);
				data.put("CONT_GL_SEQ", contGlSeq);
				data.put("SIGNER_ID", signer.get("SIGNER_ID"));
				data.put("SEQ", signer.get("SEQ"));
				data.put("SIGNER_COMPANY_CODE", signer.get("SIGNER_COMPANY_CODE"));
				data.put("FIXED_SIGNER_NAME", signer.get("SIGNER_NAME"));
				data.put("FIXED_SIGNER_EMAIL", signer.get("SIGNER_EMAIL"));
				data.put("FIXED_SIGNER_PHONE", signer.get("SIGNER_PHONE"));
				data.put("FIXED_IRS_NO", signer.get("SIGN_KEY"));
				
				if(frontSignerList != null) {
					Map<String, String> frontSigner = frontSignerList.get(signerList.indexOf(signer));
					data.put("FIXED_CATEGORY", frontSigner.get("CATEGORY"));
				}
				
				scsiData.add(data);
			}
			sxp = new SepoaXmlParser(this, "doInsertSSCSI");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			ssm.doInsert(scsiData);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	@Description("템플릿에 지정된 아이템 조회")
	public SepoaOut doGetTemplateItems(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaFormater sf = null;
		String signItemPrefix = "SIGN_ITEM_";
		String CONT_TYPE;

		try {
			// 서식 데이터 조회
			Map<String,String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
			String fileTempDirectory = MapUtils.getString(allData, SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, null);
			Config config = (Config) MapUtils.getObject(allData, SepoaModuleDataMapper.KEY_CONFIGURATION, null);
			if(config == null) {
				config = new Configuration();
			}
			
			String contFormNo = headerData.get("CONT_FORM_NO");
			
			CONT_TYPE = MapUtils.getString(headerData, "CONT_TYPE", "E");
			
			Map<String, String> docInfo = new HashMap<>();
			docInfo.put("CONT_NO", contFormNo);
			docInfo.put("CONT_GL_SEQ", "1");
			docInfo.put("CONT_TYPE", CONT_TYPE);
			
			List<String> signerNoList = new ArrayList<>();
			List<String> commonItemCodeList = new ArrayList<>();
			Map<String, List<String>> signerItemListObj = new LinkedHashMap<>();;
			
			Map<String, Object> paramData = new HashMap<>();
			paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, docInfo);
			paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfo);
			paramData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, paramData);
			paramData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			paramData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			
			
			String method = "doDocumentDetail";
			SepoaOut detailSO = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractTemplateService", ctx, method, new Object[] {paramData});
			if(detailSO.flag) {
				List<Object> detailResult = detailSO.getAllData();
				Map<String, Object> detailObj = (Map<String, Object>) detailResult.get(0);
				Map<String, String> templateInfo = (Map<String, String>) detailObj.get("DOC_INFO_DATA");
				signerNoList = Arrays.asList(MapUtils.getString(templateInfo, "SIGNER_CODE", "").split(","));
				
				for(String signerNo : signerNoList) {
					signerItemListObj.put(signerNo, new ArrayList<String>());
				}
			}
			
			method = "doFileList";
			List<Map<String,String>> fileList = new ArrayList<>();
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractTemplateService", ctx, method, new Object[] {paramData});
			if(so.flag) {
				List<String> layers = Arrays.asList(new String[]{"CONT_C", "CONT_E"});
				List<Object> fileResult = so.getAllData();
				List<Map<String, String>> allFileList = (List<Map<String, String>>) fileResult.get(0);
				for(Map<String, String> fileInfo : allFileList) {
					String layer = MapUtils.getString(fileInfo, "LAYER", "");
					if(layers.contains(layer)) {
						fileList.add(fileInfo);
					}
				}
			}
			
			Pattern itemBoxClassNamePattern = Pattern.compile("contract-item-box-code-([a-zA-Z0-9_]{0,})");
			Pattern signItemBoxCodePattern = Pattern.compile("contract-item-box-sign-id-([a-zA-Z0-9_]{0,})");
			for(Map<String, String> file : fileList) {
				// 서식의 문서 파일별 반복문(CONT_C , CONT_E)
				String fixHtml = MapUtils.getString(file, "FIX_HTML", "{}");
				Map<String, String> fixHtmlMap = new Gson().fromJson(fixHtml, Map.class);
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					// 문서의 페이지별 반복문
					Document doc = Jsoup.parse(entry.getValue());
					Elements itemList = doc.select("[class~=contract-item-box-code-]");
					for(Element item : itemList) {
						// 아이템별 반복문
						String classList = item.className();
						Matcher itemMatcher = itemBoxClassNamePattern.matcher(classList);
						if(itemMatcher.find()) {
							String itemCode = itemMatcher.group(1);
							if(itemCode.contains("SIGN_ITEM")) {
								// 서명자 항목 아이템의 경우
								itemCode = itemCode.replace(signItemPrefix, "");	//'SIGN_ITEM_' prefix 제거
								
								int signCodeIndex = itemCode.lastIndexOf("_SI"); // 뒤에서부터 첫 등장하는 _SI 는 SIGNER_CODE 이므로 제거.
								itemCode = (signCodeIndex != -1) ? itemCode.substring(0, signCodeIndex) : itemCode; 
								if(classList.contains("contract-item-box-sign-id-")) {
									Matcher signItemMatcher = signItemBoxCodePattern.matcher(classList);
									if(signItemMatcher.find()) {
										String signerNo = signItemMatcher.group(1);
										if(signerItemListObj != null) {
											List<String> signerItemList = signerItemListObj.get(signerNo);
											if(signerItemList.contains(itemCode) == false) {
												signerItemList.add(itemCode);
											}
										}
									}
								}
							} else {
								// 일반 항목 아이템의 경우
								if(commonItemCodeList.contains(itemCode) == false) {
									commonItemCodeList.add(itemCode);
								}
							}
						}
					}
				}
			}
			
			// List<String> allowOutputType = Arrays.asList(new String[] {"ITEM_CODE", "ITEM_SEQ", "ITEM_TYPE", "ITEM_NAME"});
			
			// 준비된 공통, 서명자 데이터로 SUPIM 조회하여 상세 데이터 적재
			Map<String, Object> totalResult = new HashMap<>();
			List<Map<String, String>> commonItemList = new ArrayList<>();
			List<Map<String, Object>> signerItemList = new ArrayList<>();
			for(String signerNo : signerNoList) {
				Map<String, Object> signerItem = new HashMap<>();
				signerItem.put("SIGNER_NO", signerNo);
				signerItem.put("ITEMS", new ArrayList<Map<String, String>>());
				
				signerItemList.add(signerItem);
			}
			
			totalResult.put("COMMON_ITEMS", commonItemList);
			totalResult.put("SIGN_ITEMS", signerItemList);
			
			Map<String, Object> params = new HashMap<>();
			Map<String, String> itemInfo = new HashMap<>();
			params.put(SepoaModuleDataMapper.KEY_HEADER_DATA, itemInfo);
			itemInfo.put("CONT_TYPE", CONT_TYPE);
			itemInfo.put("CATEGORY", "COMMON"); // 공통 아이템
			
			method = "doQuery";
			so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractItem", ctx, method, new Object[] {params});
			if(so.flag) {
				sf = new SepoaFormater(so.result[0]);
				if(sf.getRowCount()> 0) {
					List<Map<String, String>> commonItemInfoList = sf.getListMap();
					for(Map<String, String> commonItemInfo : commonItemInfoList) {
						String itemCode = commonItemInfo.get("ITEM_CODE");
						if(commonItemCodeList.contains(itemCode)) {
							commonItemList.add(commonItemInfo);
						}
					}
				}
			}
			
			itemInfo.put("CATEGORY", "SIGNER"); // 서명자 아이템
			so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractItem", ctx, method, new Object[] {params});
			if(so.flag) {
				sf = new SepoaFormater(so.result[0]);
				if(sf.getRowCount() > 0) {
					List<Map<String, String>> signerItemInfoList = sf.getListMap();
					for(Map<String, String> signerItemInfo : signerItemInfoList) {
						String itemCode = signerItemInfo.get("ITEM_CODE");
						for(Entry<String, List<String>> entry : signerItemListObj.entrySet()) {
							String signerNo = entry.getKey();
							if(entry.getValue().contains(itemCode)) {
								Map<String, Object> target = signerItemList.get(signerNoList.indexOf(signerNo));
								List<Map<String, String>> targetSignerItemList =  (List<Map<String, String>>) target.get("ITEMS");
								targetSignerItemList.add(signerItemInfo);
							}
						}
					}
				}
			}
			
			setValue(totalResult);
		}catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		
		return getSepoaOut();
	}
	
	@Description("템플릿 아이템에 입력한 데이터 추출")
	protected Map<String,String> getTemplateMappingData(Map<String,Object> allData){
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		
		Map<String,String> result = new HashMap<>();
		
		if(jsonData.containsKey("TEMPLATE_MAPPING_DATA") 
				&& jsonData != null 
				&& SepoaModuleDataMapper.IS_JSON_OBJECT.equals(MapUtils.getString(headerData, "TEMPLATE_MAPPING_DATA", ""))) {
			Map<String,String> templateMappingData = (Map<String,String>) jsonData.get("TEMPLATE_MAPPING_DATA");
			for(Entry item : templateMappingData.entrySet()) {
				if(item.getValue() != null && !StringUtils.isBlank(String.valueOf(item.getValue()))) {
					result.put(String.valueOf(item.getKey()), String.valueOf(item.getValue()));
				}
			}
		}
		
		return result;
	}
	
	@Description("템플릿 데이터 SCTTI 저장")
	public SepoaOut doSaveTemplateMappingData(Map<String,Object> contractData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String,String> docInfoData = (Map<String,String>) contractData.get("DOC_INFO_DATA");
			Map<String,String> templateMappingData = (Map<String,String>) contractData.get("TEMPLATE_MAPPING_DATA");
			
			ObjectMapper mapper = new ObjectMapper();
	        String jsonTemplateMappingData = mapper.writeValueAsString(templateMappingData);
	        
	        Map<String,String> params = new HashMap<>();
	        
	        params.put("CONT_NO", docInfoData.get("CONT_NO"));
	        params.put("CONT_GL_SEQ", docInfoData.get("CONT_GL_SEQ"));
	        params.put("ITEM_VALUE", jsonTemplateMappingData);
			
	        sxp = new SepoaXmlParser(this, "doSelectSCTTI");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(params));
			
	        params.put("CONT_FORM_NO", docInfoData.get("CONT_FORM_NO"));
			
			if(sf.getRowCount()>0) {
				sxp = new SepoaXmlParser(this, "doUpdateSCTTI");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(params);
			}else {
				sxp = new SepoaXmlParser(this, "doInsertSCTTI");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(params);
			}
			
			Commit();
			
			setValue(jsonTemplateMappingData);
		}catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	
	@Description("템플릿 데이터 SCTTI 조회")
	public SepoaOut doGetTemplateData(Map<String,String> paramData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(paramData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
			
	        sxp = new SepoaXmlParser(this, "doSelectSCTTI");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0 && sf.getListMap() != null) {
				setValue(new Gson().toJson(sf.getListMap().get(0)));
			}

		}catch(Exception e) {
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
	@Description("전자인장 - 다중계약 대상자정보 조회")
	public SepoaOut doQueryMultiContract(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser(this, "doQueryMultiContData"); // 다중계약 대상자 테이블
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(headerData)).getListMap();
			setValue(resultList);
		} catch (Exception e) {
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	
	@SuppressWarnings("unchecked")
	@Description("다중계약 문서정보 저장")
	public SepoaOut doSaveMultiContract(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		String fileTempDirectory = null;
		try {
			
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
			Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
			Config config = (Config) MapUtils.getObject(allData, SepoaModuleDataMapper.KEY_CONFIGURATION, null);
			
			if (config == null) {
				config = new Configuration();
				allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			}

			String tempDirectory = MapUtils.getString(allData, SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, null);

			if (tempDirectory == null) {
				PathManager pm = new PathManager(info, config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			}
			
			// SMCWT 테이블에 적재하기위해 signerGroupList 정보로 가공. [A-B], [A-C] ... [A-Z] 도급처와 수급처의 각각의 1:1 계약건으로 생성되도록
			List<List<Map<String, String>>> signerGroupList = getMultiSignerListData(allData);

			Map<String, String> multiContAllData = new HashMap<>();
			Map<String, Object> multiContMap = new HashMap<>();

			multiContMap.put(SepoaModuleDataMapper.KEY_HEADER_DATA, headerData);
			multiContMap.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, jsonData);
			multiContMap.put("SIGNER", jsonData.get("SIGNER"));
			multiContMap.put("SIGNER_GROUP_LIST", signerGroupList);
			
			// SIGNER_NO 가 없을경우 매번 기존데이터 삭제 후 모두 새로운키를 생성하기때문에 SIGNER_NO값을 추가로 세팅하여 저장.
			sxp = new SepoaXmlParser(this, "doSelectSSIGN");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			String innerSignKey = "";
			String outerSignKey = "";
			
			if(sf.getListMap() != null && sf.getListMap().size() > 0) {
				for(Map<String, String> rowData : sf.getListMap()) {
					if(rowData.get("SEQ").equals("1")) {
						innerSignKey = rowData.get("SIGNER_NO");
					}else {
						outerSignKey = rowData.get("SIGNER_NO");
					}
				}
				
			}
			
			if (signerGroupList != null && signerGroupList.size() > 0) {
				for (int i = 0; i < signerGroupList.size(); i++) {
					List<Map<String, String>> signerGroup = signerGroupList.get(i);
					
					for(Map<String, String> rowData : signerGroup) {
						if(rowData.get("SEQ").equals("1")) {
							rowData.put("SIGNER_NO", innerSignKey);
						}else {
							rowData.put("SIGNER_NO", outerSignKey);
						}
					}
					
				}
			}
				
			multiContAllData.put("DOC_NUMBER", headerData.get("CONT_NO"));
			multiContAllData.put("DATA", new Gson().toJson(multiContMap));

			// 하나의 계약 데이터의 모든 계약세부정보를 모두 저장 - 각각의 모든 서명자들정보를 포함한다.
			sxp = new SepoaXmlParser(this, "doQueryMultiContData");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(multiContAllData));

			if (sf.getListMap() != null && sf.getListMap().size() < 1) {
				sxp = new SepoaXmlParser(this, "doSaveMultiContData");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doInsert(multiContAllData);
			} else {
				sxp = new SepoaXmlParser(this, "doUpdateMultiContData");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(multiContAllData);
			}
			
			
			List<Map<String, String>> editorSignerGroup = new ArrayList<>();
			for(int i=0; i < signerGroupList.size() ; i++) {
				for(Map<String, String> rowData : signerGroupList.get(i)) {
					if(rowData.get("SIGNER_COMPANY_CODE").equals("DUMMY_CODE")) {
						editorSignerGroup.addAll(signerGroupList.get(i));
						break;
					}
				}
			}
			
			jsonData.put("SIGNER", editorSignerGroup);
			allData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, jsonData);
			
			doSaveContract(allData);
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		
		return getSepoaOut();
	}
	
	@Description("엑셀 템플릿 다운로드")
	public SepoaOut doExcelDownload(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String,String>> columnData = new Gson().fromJson(headerData.get("COLUMN_LIST"), List.class);
			setValue(new SepoaFormater(columnData));
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
	
	@Description("엑셀 업로드")
	public SepoaOut doExcelUpload(Map<String, Object> allData) {
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) MapUtils.getObject(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> excelDataList = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_GRID_DATA);
			if (excelDataList == null || excelDataList.size() == 0) {
				setStatus(105002);
				Logger.err.println("Not Found User Interface Data!");
				throw new Exception("Not Found User Interface Data!");
			}
			
			for(Map<String,String> line: excelDataList) {
				//중복 및 누락데이터 확인 함수 사용을 위한 더미데이터 삽입
				if("".equals(MapUtils.getString(line, "SIGNER_COMPANY_CODE", ""))) {
					line.put("SIGNER_COMPANY_CODE","__DUMMY__");
					line.put("SIGNER_COMPANY_NAME","__DUMMY__");
				}
			}
			
			Map<String, Object> checkResult = this.checkSignerListValidate(excelDataList); //중복 및 누락데이터 체크
			if(MapUtils.getBooleanValue(checkResult, "RESULT") == false) {
				List<String> dupe = (List<String>) MapUtils.getObject(checkResult, "DUPLICATE", new ArrayList<>());
				if(dupe.size() > 0) {
					throw new Exception((MapUtils.getIntValue(checkResult, "INDEX")+9) + "행에 중복된 데이터가 존재합니다.\r\n" + dupe.stream().map((e) -> Cache.text.get("ECT_205_E." + e)).collect(Collectors.toList()));
				}
				
				List<String> require = (List<String>) MapUtils.getObject(checkResult, "REQUIRE", new ArrayList<>());
				
				if(require.size() > 0) {
					throw new Exception((MapUtils.getIntValue(checkResult, "INDEX")+9) + "행에 누락된 데이터가 존재합니다.\r\n" + require.stream().map((e) -> Cache.text.get("ECT_205_E." + e)).collect(Collectors.toList()));
				}
			}
			
			int seq = 1;
			for(Map<String,String> line: excelDataList) {
				sxp = new SepoaXmlParser(this, "doCheckUSMT");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(line));
				if(sf.getRowCount() < 1) {
					throw new Exception((seq+8)+"행 사용자가 존재하지 않습니다");
				}
				
				line.put("CATEGORY", "C"); // 카테고리 조건 지정
				sxp = new SepoaXmlParser(this, "doCheckBUPI");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(line));
				if(sf.getRowCount() > 0) {
					Map<String,String> temp = sf.getListMap().get(0);
					line.put("SIGNER_COMPANY_CODE", MapUtils.getString(temp, "BIZ_CODE"));
					line.put("SIGNER_COMPANY_NAME", MapUtils.getString(temp, "SELLER_NAME_LOC"));
				}else {
					throw new Exception((seq+8)+"행 계약자 정보가 존재하지 않습니다");
				}
				
				String contFrom = line.get("CONT_FROM");
				String contTo = line.get("CONT_TO");
				
				String regex = "^\\d{4}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])$"; // 20240101 날짜형태의 정규식
				if(!StringUtils.isBlank(contFrom) && !Pattern.matches(regex, contFrom)) {
					throw new Exception((seq+8) + "행 계약시작일 형태가 올바르지 않습니다.");
					
				}else if(!StringUtils.isBlank(contTo) && !Pattern.matches(regex, contTo)) {
					throw new Exception((seq+8) + "행 계약종료일 형태가 올바르지 않습니다.");
				}
				
				line.put("SEQ", Integer.toString(seq++));
			}
			setValue(new SepoaFormater(excelDataList));
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("계약 진행현황 - 서명요청")
	public SepoaOut doRequestSigning(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, Object> headerData = (Map<String, Object>) MapUtils.getObject(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA);
			List<Map<String, String>> targetDataList = (List<Map<String, String>>) headerData.get("TARGET_DATA");
			
			if(targetDataList == null || targetDataList.size() == 0) {
				setStatus(280004);
				throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_280004"));
			}
			
			Map<String, Object> data = new HashMap<>();
			Map<String, String> requesterInfo = new HashMap<>();
			requesterInfo.put("HOUSE_CODE",		info.getSession("HOUSE_CODE"));
			requesterInfo.put("COMPANY_CODE",	info.getSession("COMPANY_CODE"));
			requesterInfo.put("ID",				info.getSession("ID"));
			requesterInfo.put("DEPARTMENT",		info.getSession("DEPARTMENT"));
			requesterInfo.put("IP",				ThreadLocalFilter.getClientIP());
			data.put("REQUESTER_INFO", requesterInfo);
			
			for(Map<String, String> targetData : targetDataList) {
				targetData.put("DATA", new Gson().toJson(data));
				targetData.put("DOC_NUMBER", targetData.get("CONT_NO") + "_" + targetData.get("CONT_GL_SEQ"));
				
				String postProcessed = "N";
				if("true".equalsIgnoreCase(CommonUtil.getConfig("sepoa.server.development.flag", "false"))) {
					postProcessed += this.getUniqueString();
				}
				targetData.put("POSTPROCESSED", postProcessed);
				
				targetData.put("PRC_TYPE", "SIGN_WAIT");
				targetData.put("STATUS", "DC");
			}
			
			sxp = new SepoaXmlParser(this, "doInsertSMCWT");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(targetDataList);
			
			sxp = new SepoaXmlParser(this, "doUpdateSCTGL");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(targetDataList);
			
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(getStatus() == 1 ? 0 : getStatus());
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
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
