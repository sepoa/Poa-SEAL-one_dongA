package sepoa.svc.ct;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import sepoa.filemanager.FileManager;
import sepoa.filemanager.utils.cipher.CipherManager;
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
import sepoa.fw.push.SepoaMessage;
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
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;
import sepoa.svl.util.datamapper.SepoaModuleDataMapper;

@Description("전자인장 - 전자계약 모듈 호출용 서비스")
public class CT_205 extends SepoaService {

	public CT_205(String opt, SepoaInfo info) throws SepoaServiceException {
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
//			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, "doDocumentDetail", new Object[] {allData});
			if(so.flag) {
				List<Object> result = so.getAllData();
				if(result.size() > 0) {
					Map<String, Object> detail = (Map<String, Object>) result.get(0);
					Map<String, Object> docInfo = MapUtils.getMap(detail, "DOC_INFO_DATA");
					List<Map<String, String>> signerList = (List<Map<String, String>>) MapUtils.getObject(detail, "SIGNER", null);
					if(signerList != null) {
						sxp = new SepoaXmlParser(this, "doQuerySSCSI");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						
						Map<String, String> condition = new HashMap<>();
						condition.put("CONT_NO", String.valueOf(docInfo.get("CONT_NO")));
						condition.put("CONT_GL_SEQ", String.valueOf(docInfo.get("CONT_GL_SEQ")));
						List<Map<String, String>> scsiList = new SepoaFormater(ssm.doSelect(condition)).getListMap();
						
						for(Map<String, String> signer : signerList) {
							Map<String, String> scsiData = scsiList.get(signerList.indexOf(signer));
							if(scsiData.containsKey("FIXED_CATEGORY")) {
								signer.put("CATEGORY", scsiData.get("FIXED_CATEGORY"));
							}
							
							if(signer.containsKey("SIGN_KEY")) {
								signer.put("IRS_NO", signer.get("SIGN_KEY"));
							}
						}
						
						docInfo.put("SIGNER", signerList);
					}
					
					setValue(docInfo);
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
					so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
					if(so.flag) {
						copyContractFileFlag = true;
					} else {
						throw new Exception(so.message);
					}
				}
				
				if(copyContractFileFlag) {
					List<Map<String, String>> fileTargetList = null;
					String reqType = "REQ_SEAL";
					String targetLayer = "";
					
					//화면에서 요청 구분을 받아올 수 있는 구조가 아니라서; 날인요청 조회해보고 없으면 반출건으로 조회한다.
					sxp = new SepoaXmlParser(this, "doQuerySSIPD");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					sf = new SepoaFormater(ssm.doSelect(headerData));
					
					if(sf.getRowCount() == 0) {
						sxp = new SepoaXmlParser(this, "doSelectSSEDC");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						sf = new SepoaFormater(ssm.doSelect(headerData));
						reqType = "EXT_SEAL";
						targetLayer = "EXT_SEAL_REAL_DOC";
					}
					
					fileTargetList = sf.getListMap();
					if(fileTargetList != null && fileTargetList.size() > 0) {
						if("REQ_SEAL".equals(reqType)) {
							sxp = new SepoaXmlParser(this, "doSelectSSIAD");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							sf = new SepoaFormater(ssm.doSelect(headerData));
							Map<String, String> reqSealHeader = sf.getListMap().get(0);
								targetLayer = "REQ_SEAL_REAL_DOC";
						}
						
						FileManager fm = new FileManager(info, config, ctx);
						List<Map<String, String>> copyList = new ArrayList<>();
						for(Map<String, String> target : fileTargetList) {
							String smrNumber = MapUtils.getString(target, "SMR_NUMBER");
							String smdNumber = MapUtils.getString(target, "SMD_NUMBER");
							String docNumber = smrNumber + "_" + smdNumber;
							if(fm.setDocNumber(docNumber) != null) {
								List<Map<String, String>> fileList = fm.doList();
								for(Map<String, String> file : fileList) {
									if(MapUtils.getString(file, "LAYER", "").equals(targetLayer)) {
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
					} else {
						throw new Exception("Not Found Document Target File");
					}
				}
			} else {
				if(!StringUtils.isBlank(headerData.get("CONT_NO")) && !StringUtils.isBlank(headerData.get("CONT_GL_SEQ"))) {
					contractNo = headerData.get("CONT_NO");
					contractGlSeq = headerData.get("CONT_GL_SEQ");
					
					docInfoData.put("CONT_NO", contractNo);
					docInfoData.put("CONT_GL_SEQ", contractGlSeq);
				} else {										// 신규 계약서를 작성할 경우
					method = "doDocumentCreateIF";
					so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {contractData});
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
				so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
				if(so.flag) {
					Map<String, String> beforeDocItem = (Map<String, String>) so.getData(0);
					beforeDocItem.putAll(docItem);
					contractData.put("DOC_ITEM", beforeDocItem);
				} else {
					throw new Exception(so.message);
				}
				
				
				/*
				 * 기타 첨부파일을 업로드 한 후 서식을 선택한 경우 기존 파일 제거 후 서식의 파일이 복사되기 때문에
				 * 기존 기타 첨부파일을 보존하고 싶을 경우 아래 로직이 동작되어야 한다.
				 * */
				method = "doDocumentDetail";
				so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
				if(so.flag) {
					List<Object> result = so.getAllData();
					if(result.size() > 0) {
						Map<String, Object> detail = (Map<String, Object>) result.get(0);
						Map<String, String> docInfo = MapUtils.getMap(detail, "DOC_INFO_DATA");
						
						String beforeFormNo = docInfo.get("CONT_FORM_NO");
						if(!MapUtils.getString(docInfoData, "CONT_FORM_NO", "").equals(beforeFormNo)) {
							FileManager fm = new FileManager(info, config, ctx);
							String docNumber = contractNo + "_" + contractGlSeq;
							fm.setDocNumber(docNumber);
							fm.setLayer("CONT_A");
							List<Map<String, String>> extraFileList = fm.doList();
							if(extraFileList != null && extraFileList.size() > 0) {
								contractData.put("EXTRA_FILES", extraFileList);
								
								PathManager pm = new PathManager(info, config);
								for(Map<String, String> extraFile : extraFileList) {
									File decrypt = CipherManager.decrypt(pm.getFile(extraFile), false);
									String tempDir = MapUtils.getString(allData, SepoaModuleDataMapper.KEY_TEMP_DIRECTORY);
									File tempFile = new File(tempDir + File.separator + extraFile.get("FILE_NAME_CLIENT"));
									FileUtils.copyFile(decrypt, tempFile);
									extraFile.put("PATH", tempFile.getAbsolutePath());
								}
							}
						}
					}
				} else {
					throw new Exception(so.message);
				}
				
				
				paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, contractData);
				paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, contractData);
				method = "doDocumentUpdate";					// 계약 문서 정보 업데이트
				so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
				if(so.flag) {
					paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, docInfoData);
					paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfoData);
					
					method = "doDocumentDetail";
					so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
					if(so.flag) {
						List<Object> result = so.getAllData();
						if(result.size() > 0) {
							Map<String, Object> detail = (Map<String, Object>) result.get(0);
							Map<String, String> docInfo = MapUtils.getMap(detail, "DOC_INFO_DATA");
							List<Map<String, String>> signerInfo = (List<Map<String, String>>) MapUtils.getObject(detail, "SIGNER", null);
							this.doSaveContractData(ctx, allData, docInfo, signerInfo);
							
							setValue(docInfo);		// 편집창 오픈을 위한 계약 데이터 결과값 반환
							
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
				
				/* TODO 동아제약용 도급처 서명인 자동 서명 로직 임시 위치
				 * TODO 추후 CBO로 코드 위치 이관 예정
				 * */
				if(scgl != null && !"".equals(scgl.get("SOURCE_NO"))) {	// 날인완료된 문서로 작성된 계약일 경우
					if("A".equalsIgnoreCase(docInfo.get("STATUS"))) {
						sxp = new SepoaXmlParser(this, "doUpdateSSIGN");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doUpdate(docInfo, new SingletonMap("SIGN_FLAG", "S"));
						
						FileManager fm = new FileManager(info, new Configuration(), ctx);
						fm.setDocNumber(docInfo.get("CONT_NO") + "_" + docInfo.get("CONT_GL_SEQ"));
						fm.setLayer("CONT_C");
						List<Map<String, String>> contDocFileList = fm.doList();
						
						
						if(contDocFileList != null && contDocFileList.size() > 0) {
							for(Map<String, String> contDocFile : contDocFileList) {
								fm.doCopy(contDocFile.get("DOC_NUMBER"), contDocFile.get("FILE_ID"), "CONT_CC");
								contDocFile.put("FILE_TYPE", "C");
								fm.doModifyMeta(contDocFile);
							}
						}
					
						// MAIL_SEND_YN=Y  start
						
						if("Y".equals(docInfo.get("MAIL_SEND_YN"))) {
							
							sxp = new SepoaXmlParser(this, "doSelectSSIGN");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
							sf = new SepoaFormater(ssm.doSelect(docInfo));
							
							if(sf.getRowCount() > 0) {
								// 메일 발송 진행
				            	SepoaPushManager mailpush = new SepoaPushManager(info, new Configuration());
								Map<String, String> mailData = new HashMap<>();
								mailData.putAll(docInfo);
								mailData.put("TITLE", "계약완료");
								mailData.put("TITLE_COMMENT", "계약이 완료되었습니다.");
								mailData.put("LABEL_1", "서명자");
								mailData.put("SUBJECT", "[동아ST] " + docInfo.get("CONT_SUBJECT") + " 계약완료 안내");
								
								SepoaMessage message = new SepoaMessage();
								StringBuilder signerComment = new StringBuilder("");
								List<Map<String, String>> signerList = sf.getListMap();
								for(Map<String, String> signer : signerList) {
									message.addReceiver(signer.get("SIGNER_ID"), signer.get("SIGNER_NAME"), signer.get("SIGNER_EMAIL"), null);
									signerComment.append(signer.get("SIGNER_NAME") + "(" + signer.get("SIGNER_EMAIL") + ")<br/>");
								}
								mailData.put("CONTENTS_1", signerComment.toString());
								
								fm.setLayer("CONT_CC");
								List<Map<String, String>> completeContFileList = fm.doList();
								if(completeContFileList != null && completeContFileList.size() > 0) {
									PathManager pm = new PathManager(info, new Configuration());
									pm.setLayer("CONT_CC");
									
									for(Map<String, String> completeContFile : completeContFileList) {
										String CompleteFilePath = pm.getLayerPath() + "/" + completeContFile.get("FILE_NAME_SERVER");
										message.addAttach(CipherManager.getCipher().getInputStream(new File(CompleteFilePath), false), completeContFile.get("FILE_NAME_CLIENT"));
									}
								}
								
								message.addParams(mailData);
								message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
								mailpush.addMessage(message);
				            	mailpush.accept();
							}
						}	// MAIL_SEND_YN=Y  end
						
					} else if("D".equals(docInfo.get("CONT_METHOD")) && "G".equals(docInfo.get("STATUS"))) {		// 디지털 서명 계약이면서 현재 계약상태가 서명진행중(G)일 경우
						Map<String, Object> paramData = new HashMap<>();
						paramData.put(SepoaModuleDataMapper.KEY_HEADER_DATA, docInfo);
						paramData.put(SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, docInfo);
						
						Config config = new Configuration();
						paramData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
						
						SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, "doDocumentDetail", new Object[] {paramData});
						if(so.flag) {
							List<Object> result = so.getAllData();
							if(result.size() > 0) {
								Map<String, Object> detail = (Map<String, Object>) result.get(0);
								List<Map<String, String>> signerList = (List<Map<String, String>>) MapUtils.getObject(detail, "SIGNER", null);
								if(signerList != null) {
									List<Map<String, String>> currentSignerList = signerList.stream().filter(signer -> {return signer.get("SIGN_FLAG").equals("M");}).collect(Collectors.toList()); // SSIGN 서명자 리스트 중 현재 서명순서(M)인 것만 필터링
									if(currentSignerList.size() > 0) {
										sxp = new SepoaXmlParser(this, "doQuerySSCSI");
										ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
										
										List<Map<String, String>> scsiList = new SepoaFormater(ssm.doSelect(docInfo)).getListMap();
										scsiList = scsiList.stream().filter(scsi -> {return scsi.get("FIXED_CATEGORY").equals("C");}).collect(Collectors.toList());	// SSCSI 서명자 리스트 중 도급처인 것만 필터링
										for(Map<String, String> currentSigner : currentSignerList) {
											Optional<Map<String, String>> filteredSigner = scsiList.stream().filter(scsi -> {return currentSigner.get("SIGNER_ID").equals(scsi.get("SIGNER_ID"));}).findFirst();
											if(filteredSigner.isPresent()) {
												Map<String, String> target = filteredSigner.get();
												target.put("ID", target.get("SIGNER_ID"));
												target.put("COMPANY_CODE", target.get("SIGNER_COMPANY_CODE"));
												docInfo.put("SIGN_TYPE", docInfo.get("CONT_METHOD"));
												docInfo.put("SIGN_FIX_HTML", "[]");
												docInfo.put("SIGN_COMMENT", "");
												
												// 서명 API 호출
												SepoaInfo dummyInfo = new SepoaInfo(target);
												so = ServiceConnector.doService(dummyInfo, "sepoa.svc.contract.SepoaContractService", ctx, "doSign", new Object[] {paramData});
											}
										}
									}
								}
							}
						} else {
							throw new Exception(so.message);
						}
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
			Logger.err.println(info.getSession("ID"), this, e);
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

			headerData.put("SUX_WKEY1", info.getSession("HOUSE_CODE"));
			headerData.put("SUX_WKEY2",info.getSession("COMPANY_CODE"));
			headerData.put("SUX_WKEY3", MapUtils.getString(headerData,"CONT_NO"));
			headerData.put("SUX_WKEY4", MapUtils.getString(headerData,"CONT_GL_SEQ"));
			headerData.put("APP_SUBJECT", MapUtils.getString(headerData,"SUBJECT","")); 
			headerData.put("APP_USER_AMT", MapUtils.getString(headerData,"EXEC_AMT",""));

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
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {allData});
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
			
			if(!allData.containsKey(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY)) {
				PathManager pm = new PathManager(config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			}
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {allData});
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
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {allData});
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
		
		String fileTempDirectory = null;
		String method = "doDocumentUpdate";
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaModuleDataMapper.KEY_HEADER_DATA);
			headerData.put("DOC_INFO_DATA", SepoaModuleDataMapper.IS_JSON_OBJECT);
			headerData.put("NEXT_STEP", "");
			
			Config config = new Configuration();
			allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			
			if(!allData.containsKey(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY)) {
				PathManager pm = new PathManager(config);
				pm.setLayer(PathManager.LAYER_TEMP);
				fileTempDirectory = pm.getUniquePath();
				allData.put(SepoaModuleDataMapper.KEY_TEMP_DIRECTORY, fileTempDirectory);
			}
			
			Map<String, Object> jsonData = new HashMap<>();
			allData.put(SepoaModuleDataMapper.KEY_OBJECT_DATA, jsonData);
			
			Map<String, String> docInfoData = new HashMap<>();
			jsonData.put("DOC_INFO_DATA", docInfoData);
			docInfoData.put("STATUS", "TM");
			
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {allData});
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
				SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
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
				so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
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
			ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
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
							so = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", ctx, method, new Object[] {paramData});
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
	
	@Description("감추서 추가데이터")
	public SepoaOut getAuditTrailExtraData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		
		try {
			SepoaOut so = ServiceConnector.doService(info, "sepoa.svc.seal.SepoaReqSealService", ctx, "doCreateIssueNo", new Object[] {new HashMap<String, String>()});
			if(so.flag) {
				Date nowDate = new Date();
				String addDate = new SimpleDateFormat("yyyy-MM-dd").format(nowDate);
				String addTime =  new SimpleDateFormat("HH:mm:ss").format(nowDate);
				String issueNo = (String) so.getData(0);
				
				
				Map<String, String> footer = new HashMap<>();
				allData.put("FOOTER", footer);
				
				footer.put("PUB_DATE", "Published Date");
				footer.put("ADD_DATE", addDate);
				footer.put("ADD_TIME", addTime);
				footer.put("PUB_NO", "Publish No.");
				footer.put("PUBLISHED_NO", issueNo);
				
				setValue(allData);
			}
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
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
				condition.put("MODE_TYPE_STATUS", "A,X,CR,EW,ES,EF");
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
				condition.put("MODE_TYPE_STATUS", "A,X,F,CR");
				
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
			
			List<Map<String, String>> result = new SepoaFormater(ssm.doSelect(condition, headerData)).getListMap();
			List<Map<String, String>> filteredResult = new ArrayList<>(result.size());
			for(Map<String, String> row : result) {
				String houseCode = row.get("HOUSE_CODE");
				String companyCode = row.get("COMPANY_CODE");
				String contNo = row.get("CONT_NO");
				String contGlSeq = row.get("CONT_GL_SEQ");
				
				List<Map<String, String>> target = filteredResult.stream().filter(e -> {
					boolean isSameHouseCode = houseCode.equals(e.get("HOUSE_CODE"));
					boolean isSameCompanyCode = companyCode.equals(e.get("COMPANY_CODE"));
					boolean isSameContNo = contNo.equals(e.get("CONT_NO"));
					boolean isSameContGlSeq = contGlSeq.equals(e.get("CONT_GL_SEQ"));
					
					return isSameHouseCode && isSameCompanyCode && isSameContNo && isSameContGlSeq;
				}).collect(Collectors.toList());
				
				boolean hasRow = target.size() > 0;
				
				if(hasRow) {
					target.get(0).put(row.get("ITEM_CODE"), row.get("ITEM_VALUE"));
					target.get(0).put("EXT_" + row.get("ITEM_CODE"), row.get("ITEM_VALUE"));
				} else {
					row.put(row.get("ITEM_CODE"), row.get("ITEM_VALUE"));
					row.put("EXT_" + row.get("ITEM_CODE"), row.get("ITEM_VALUE"));
					
					filteredResult.add(row);
				}
			}
			
			// 계약금액 , 정규식추가요청
			for(Map<String, String> converData : filteredResult) {
				String amount = converData.get("CONT_AMT");
				converData.put("CONT_AMT", amount.replaceAll("\\B(?=(\\d{3})+(?!\\d))", ","));
			}
			
			filteredResult = filteredResult.stream().filter(e -> {
				boolean innerFilter = true;
				boolean outerFilter = true;
				
				if(headerData.containsKey("F_INNER_IRS_NO")) {
					innerFilter = false;
					String filter = MapUtils.getString(headerData, "F_INNER_IRS_NO", null);
					String target = MapUtils.getString(e, "EX_INNER_IRS_NO", null);
					if(filter != null && target != null) {
						innerFilter = target.contains(filter);
					}
				}
				
				if(headerData.containsKey("F_OUTER_IRS_NO")) {
					outerFilter = false;
					String filter = MapUtils.getString(headerData, "F_OUTER_IRS_NO", null);
					String target = MapUtils.getString(e, "EX_OUTER_IRS_NO");
					if(filter != null && target != null) {
						outerFilter = target.contains(filter);
					}
				}
				
				return innerFilter && outerFilter;
			}).collect(Collectors.toList());
			
			setValue(filteredResult);
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
	
	
	
	
	@SuppressWarnings("unchecked")
	protected Map<String, String> getDocInfoData(Map<String, Object> allData) {
		Map<String, String> docInfoData = new HashMap<String, String>();
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		docInfoData.put("CONT_FORM_NO",  headerData.get("CONT_FORM_NO"));						// 계약 서식 번호
//		docInfoData.put("CONT_TYPE",     headerData.get("CONT_TYPE"));							// 계약 타입 (B : 거래처 계약, E : 임직원 계약)
		docInfoData.put("CONT_SUBJECT",  headerData.get("CONT_SUBJECT"));						// 계약서 명
		docInfoData.put("CATEGORY"   ,   headerData.get("CATEGORY"));							// 계약 카테고리 [공통코드 : CT004]
		docInfoData.put("CONT_METHOD",   headerData.get("CONT_METHOD"));						// 서명방식 [공통코드 : CT003]
		docInfoData.put("CONT_DATE"  ,   headerData.get("CONT_DATE"));							// 계약 체결일 [YYYYMMDD]
		docInfoData.put("CONT_FROM"  ,   headerData.get("CONT_FROM"));							// 계약 효력 시작일 [YYYYMMDD]
		docInfoData.put("CONT_TO",       headerData.get("CONT_TO"));							// 계약 효력 만료일 [YYYYMMDD]
		docInfoData.put("EXPIRED_ALARM", headerData.get("EXPIRED_ALARM"));						// 만기도래 알림 [Y / N]
		docInfoData.put("CONT_AMT",      MapUtils.getString(headerData, "CONT_AMT", "0"));		// 계약금액
		docInfoData.put("SIGN_ORDER",    headerData.get("SIGN_ORDER"));							// 서명 순서 (S : 순서대로, P : 순서무시)
		docInfoData.put("NEXT_STEP",     headerData.get("NEXT_STEP"));
		docInfoData.put("DATA_SOURCE",   "SEAL");												// 계약 문서 생성 솔루션
		
		if(StringUtils.isBlank(docInfoData.get("CONT_TYPE"))) {
			docInfoData.put("CONT_TYPE", "B");
		}
		
		if(!StringUtils.isBlank(headerData.get("SMR_NUMBER"))) {
			docInfoData.put("SOURCE_NO", headerData.get("SMR_NUMBER"));
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
		
		if(!StringUtils.isBlank(headerData.get("SUPPLY_AMT"))) {
			extraDocInfo.put("EX_SUPPLY_AMT", headerData.get("SUPPLY_AMT"));
		}
		
		if(!StringUtils.isBlank(headerData.get("TAX_AMT"))) {
			extraDocInfo.put("EX_TAX_AMT", headerData.get("TAX_AMT"));
		}
		
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		if(jsonData != null && SepoaModuleDataMapper.IS_JSON_OBJECT.equals(MapUtils.getString(headerData, "SIGNER", ""))) {
			List<Map<String, String>> signerList = (List<Map<String, String>>) jsonData.get("SIGNER");
			
			List<String> innerSignerName = new ArrayList<>(signerList.size());
			List<String> innerIRS = new ArrayList<>(signerList.size());
			List<String> outerSignerCompanyName = new ArrayList<>(signerList.size());
			List<String> outerSignerName = new ArrayList<>(signerList.size());
			List<String> outerIRS = new ArrayList<>(signerList.size());
			for(Map<String, String> signer : signerList) {
				String category = MapUtils.getString(signer, "CATEGORY", "");
				if("C".equals(category)) {
					innerSignerName.add(MapUtils.getString(signer, "SIGNER_NAME"));
					innerIRS.add(MapUtils.getString(signer, "SIGN_KEY", ""));
				} else if("S".equals(category)) {
					outerSignerCompanyName.add(MapUtils.getString(signer, "SIGNER_COMPANY_NAME", ""));
					outerSignerName.add(MapUtils.getString(signer, "SIGNER_NAME", ""));
					outerIRS.add(MapUtils.getString(signer, "SIGN_KEY", ""));
				}
			}
			
			if(innerSignerName.size() > 0) {
				extraDocInfo.put("EX_INNER_SIGNER_NAME", StringUtils.join(innerSignerName, ","));
			}
			
			if(innerIRS.size() > 0) {
				extraDocInfo.put("EX_INNER_IRS_NO", StringUtils.join(innerIRS, ","));
			}
			
			if(outerSignerCompanyName.size() > 0) {
				extraDocInfo.put("EX_OUTER_SIGNER_COMPANY_NAME", StringUtils.join(outerSignerCompanyName, ","));
			}
			
			if(outerSignerName.size() > 0) {
				extraDocInfo.put("EX_OUTER_SIGNER_NAME", StringUtils.join(outerSignerName, ","));
			}
			
			if(outerIRS.size() > 0) {
				extraDocInfo.put("EX_OUTER_IRS_NO", StringUtils.join(outerIRS, ","));
			}
		}
		
		
		
		return extraDocInfo;
	}
	
	@SuppressWarnings("unchecked")
	protected List<Map<String, String>> getSignerListData(Map<String, Object> allData) {
		List<Map<String, String>> signerList = null;
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		if(jsonData != null && SepoaModuleDataMapper.IS_JSON_OBJECT.equals(MapUtils.getString(headerData, "SIGNER", ""))) {
			signerList = (List<Map<String, String>>) jsonData.get("SIGNER");
			for(Map<String, String> signer : signerList) {
				String signerType = null;
				if("C".equals(signer.get("CATEGORY"))) {					// 도급처 : 'C', SCTGL의 INNER_MANAGER 데이터 적재를 위한 SIGNER_TYPE 설정
					signerType = "IN";
				} else {													// 수급처 : 'S'. SCTGL의 OUTER_MANAGER 데이터 적재를 위한 SIGNER_TYPE 설정
					signerType = "OUT";
				}
				
				signer.put("SIGNER_TYPE", signerType);
			}
		}
		
		return signerList;
	}
	
	@SuppressWarnings("unchecked")
	protected List<Map<String, String>> getDocAuthList(Map<String, Object> allData) throws Exception {
		List<Map<String, String>> docAuthList = null;
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		if(docAuthList == null) {
			docAuthList = new ArrayList<>();
		}
		
		sxp = new SepoaXmlParser(this,"doQueryWorkTypeZ");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		
		List<Map<String, String>> targetList = new SepoaFormater(ssm.doSelect(new HashMap<String, String>())).getListMap();
		for(Map<String,String> target : targetList) {
			Map<String, String> auth = new HashMap<String, String>();
			auth.put("COMPANY_CODE", target.get("COMPANY_CODE"));
			auth.put("TARGET_CODE", target.get("USER_ID"));
			auth.put("PERMISSION", Authorized.READ.toString());
			
			docAuthList.add(auth);
		}
		
		//계약 관리자 권한 사람 조회
		final String contractSuperUserDefineStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+"#"+info.getSession("LANGUAGE")+"#CT_305");
		final List<Map<String, String>> contractSuperUserDefine = new Gson().fromJson(contractSuperUserDefineStr, List.class);
		for(Map<String, String> contSUElement : contractSuperUserDefine) {
			sxp = new SepoaXmlParser(this,"doCheckExistUser");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(contSUElement));
			
			if(sf.getRowCount() > 0) {
				Map<String, String> superAuthUser = sf.getListMap().get(0);
				Map<String, String> auth = new HashMap<String, String>();
				auth.put("COMPANY_CODE", superAuthUser.get("COMPANY_CODE"));
				auth.put("TARGET_CODE", superAuthUser.get("USER_ID"));
				auth.put("PERMISSION", Authorized.READ.toString());
				
				docAuthList.add(auth);
			}
		}
		
		/* TODO : 추후 이관하기
		 * 동아제약 결재 콜백용 계정 권한 추가
		 * */
		
		List<Map<String, String>> signerList = new ArrayList<>();
		Map<String, Object> jsonData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_OBJECT_DATA, null);
		signerList = (List<Map<String, String>>) jsonData.get("SIGNER");

		if(signerList != null && signerList.size() > 0) {
			Map<String, String> auth = new HashMap<>();
			for(Map<String, String> singer : signerList) {
				if(!singer.get("SIGNER_COMPANY_CODE").equals(info.getSession("COMPANY_CODE"))) { // 외부사용자는 BIZ코드를 COMPANY_CODE 사용
					auth.put("COMPANY_CODE", singer.get("SIGNER_COMPANY_CODE"));
					auth.put("TARGET_CODE", singer.get("SIGNER_ID").toUpperCase());
					auth.put("PERMISSION", Authorized.WRITE.toString());
					docAuthList.add(auth);
				}
			}
		}

		/*
		 * 팀장일 경우, 부서원 모두에게 READ 권한 부여
		 * 현재 세션 사용자의 부서원들 중 RESPONSIBILITY가 UX050에 해당하는 사용자들에게 권한 부여
		 */
		sxp = new SepoaXmlParser(this, "doSelectTeamManagers");
		ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
		ssm.setSepoaInfo(info);
		sf = new SepoaFormater(ssm.doSelect(new HashMap<String, String>()));
		
		if(sf != null && sf.getRowCount() > 0) {
			List<Map<String, String>> teamManagerList = sf.getListMap();
			for(Map<String, String> teamManager : teamManagerList) {
				Map<String, String> auth = new HashMap<String, String>();
				auth.put("COMPANY_CODE", teamManager.get("COMPANY_CODE"));
				auth.put("TARGET_CODE", teamManager.get("USER_ID"));
				auth.put("PERMISSION", Authorized.READ.toString());
				
				docAuthList.add(auth);
			}
		}
		
		return docAuthList;
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
	
	@Description("계약서 조회")
	public SepoaOut doSelectContractData(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
			Config config = (Config) MapUtils.getObject(allData, SepoaModuleDataMapper.KEY_CONFIGURATION, null);
			if(config == null) {
				config = new Configuration();
				allData.put(SepoaModuleDataMapper.KEY_CONFIGURATION, config);
			}
			
			SepoaOut so = this.doQueryContract(allData);
			if(so.flag) {
				Map<String, Object> resultData = (Map<String, Object>) so.getData(0);
				if(resultData != null && resultData.size() > 0) {
					SepoaOut so2 = ServiceConnector.doService(info, "sepoa.svc.contract.SepoaContractService", getConnectionContext(), "doDocumentExtra", new Object[] {allData});
					if(so2.flag) {
						Map<String, String> extraItem = (Map<String, String>) so2.getData(0);
						if(extraItem != null && extraItem.size() > 0) {
							resultData.putAll(extraItem);
						}
					}
					
					setValue(new Gson().toJson(resultData));
				} else {
					throw new Exception("계약서 조회실패");
				}
			} else {
				throw new Exception("계약서 조회실패");
			}
		} catch(Exception e) {
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), this, e.getMessage());
		}
		return getSepoaOut();
	}
	
	
	@Description("팝업에서 수정한 내용으로 계약완료 내용 update")
	public SepoaOut doUpdateModifyContract(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_RAW_HEADER_DATA, null);
		List<Map<String, String>> signerInfo = new Gson().fromJson(headerData.get("SIGNER_INFO"), List.class);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext();
		
		try {
			if(headerData == null || headerData.size() == 0) {
				throw new Exception("계약서 헤더정보 수신 실패");
			}
			
			if(signerInfo == null || signerInfo.size() == 0) {
				throw new Exception("서명자 정보 수신 실패");
			}
			
			String contDate = headerData.get("CONT_DATE");
			String contFrom = headerData.get("CONT_FROM");
			String contTo = headerData.get("CONT_TO");
			if(!StringUtils.isBlank(contFrom)) {
				headerData.put("CONT_FROM", contFrom.replaceAll("\\D", ""));
			}
			if(!StringUtils.isBlank(contTo)) {
				headerData.put("CONT_TO", contTo.replaceAll("\\D", ""));
			}
			if(!StringUtils.isBlank(contDate)){
				headerData.put("CONT_DATE", contDate.replaceAll("\\D", ""));
			}
			headerData.put("STATUS", "A");
			
			sxp = new SepoaXmlParser(this, "adminUpdateSCTGL");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(headerData);
			
			sxp = new SepoaXmlParser(this, "adminUpdateSSCGL");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(headerData);
			
			Map<String, String> extraData = new HashMap<String, String>();
			extraData.put("EX_SUPPLY_AMT", headerData.get("SUPPLY_AMT"));
			extraData.put("EX_TAX_AMT", headerData.get("TAX_AMT"));
			extraData.put("EX_CONT_FROM", headerData.get("CONT_FROM"));
			extraData.put("EX_CONT_TO", headerData.get("CONT_TO"));
			
			List<String> innerSignerName = new ArrayList<>();
			List<String> innerIrsNo = new ArrayList<>();
			List<String> outerSignerName = new ArrayList<>();
			List<String> outerIrsNo = new ArrayList<>();
			List<String> outerCompanyName = new ArrayList<>();
			
			for(Map<String, String> tmpSigner : signerInfo) {
				if("C".equalsIgnoreCase(tmpSigner.get("CATEGORY"))) {
					innerSignerName.add(tmpSigner.get("SIGNER_NAME"));
					innerIrsNo.add(tmpSigner.get("IRS_NO"));
				} else {
					outerSignerName.add(tmpSigner.get("SIGNER_NAME"));
					outerIrsNo.add(tmpSigner.get("IRS_NO"));
					outerCompanyName.add(tmpSigner.get("SIGNER_COMPANY_NAME"));
				}
			}
			extraData.put("EX_INNER_SIGNER_NAME", StringUtils.join(innerSignerName, ","));
			extraData.put("EX_INNER_IRS_NO", StringUtils.join(innerIrsNo, ","));
			extraData.put("EX_OUTER_IRS_NO", StringUtils.join(outerIrsNo, ","));
			extraData.put("EX_OUTER_SIGNER_COMPANY_NAME", StringUtils.join(outerCompanyName, ","));
			extraData.put("EX_OUTER_SIGNER_NAME", StringUtils.join(outerSignerName, ","));
			
			
			for(Map<String, String> signer : signerInfo) {
				signer.put("CONT_NO", headerData.get("CONT_NO"));
				signer.put("CONT_GL_SEQ", headerData.get("CONT_GL_SEQ"));
				
				sxp = new SepoaXmlParser(this, "updateSigner");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(signer);
			}
			
			for(Entry<String, String> tmpEntry : extraData.entrySet()) {
				Map<String, String> tmpData = new HashMap<String, String>();
				tmpData.put("KEY", tmpEntry.getKey());
				tmpData.put("VALUE", tmpEntry.getValue());
				tmpData.put("CONT_NO", headerData.get("CONT_NO"));
				tmpData.put("CONT_GL_SEQ", headerData.get("CONT_GL_SEQ"));
				
				sxp = new SepoaXmlParser(this, "updateMetaData");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.doUpdate(tmpData);
			}
			
            Commit();
		} catch(Exception e) {
			Rollback();
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
		}
		return getSepoaOut();
	}
	
	@Description("계약완료된 건 관리자 권한으로 반려 상태로 변경")
	public SepoaOut doRejectCont(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		Map<String, String> headerData = MapUtils.getMap(allData, SepoaModuleDataMapper.KEY_HEADER_DATA, null);
		List<Map<String, String>> gridData = (List<Map<String, String>>) allData.get(SepoaDataMapper.KEY_GRID_DATA);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		ConnectionContext ctx = getConnectionContext(); 
		Map<String, String> gridDataMap = gridData.get(0);
		
		try {
			if (MapUtils.isNotEmpty(gridDataMap)) {
				String contStatus = gridDataMap.get("CONT_STATUS");
				String contNo = gridDataMap.get("CONT_NO");
	            String contGlSeq = gridDataMap.get("CONT_GL_SEQ");
	            
				if ("A".equals(contStatus)) {
					headerData.put("STATUS", "CR");
					headerData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
					headerData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
					headerData.put("CONT_NO", contNo);
					headerData.put("CONT_GL_SEQ", contGlSeq);
				}
				
	    		sxp = new SepoaXmlParser(this, "rejectContSSCGL");
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            ssm.doUpdate(headerData);
	            
	    		sxp = new SepoaXmlParser(this, "rejectContSCTGL");
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            ssm.doUpdate(headerData);
			}
			
			
			Commit();
		} catch (Exception e) {
			Rollback();
            setStatus(0);
            setFlag(false);
            setMessage(e.toString());
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
				
				line.put("CATEGORY", "S"); // 카테고리 조건 지정
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
}
