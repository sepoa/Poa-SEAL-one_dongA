package sepoa.svc.cbo.seal.approval;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

import filters.ThreadLocalFilter;
import sepoa.filemanager.utils.storage.PathManager;
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
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;
import sepoa.seal.SepoaExtSealManager;
import sepoa.seal.SepoaReqSealManager;
import sepoa.seal.SepoaSealManager;
import sepoa.svc.cbo.approval.stub.ApprovalWebServiceStub;
import sepoa.svc.cbo.approval.stub.ApprovalWebServiceStub.CreateWorkflow;
import sepoa.svc.cbo.approval.stub.ApprovalWebServiceStub.CreateWorkflowResponse;
import sepoa.svl.common.RestUtil;
import sepoa.svl.util.SepoaMultipart;

@Description("SECL 외부 결재 사용 처리 서비스")
public class SepoaSealApprovalService extends SepoaService{
	public SepoaSealApprovalService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("인감 반출 결재 요청")
	public SepoaOut extReqApproval(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		Map<String, String> orgDocBasicInfo = new HashMap<String, String>();
		List<File> attachments = null;
		SepoaExtSealManager srm = null;
		
		try {
			Config conf = new Configuration();
			
			//반출 매니저 객체 초기화
			srm = (SepoaExtSealManager) data.get("MANAGER_OBJ");
			if(srm == null) {
				setStatus(70200);
				throw new Exception("SepoaExtSealManager Init Fail!");
			}
			
			//원본 데이터 백업용 (이후 프로세스 흘러갈 것을 고려해 백업함)
			orgDocBasicInfo.putAll(srm.getDocBaiscInfo());
			
			//반출 정보
			Map<String, String> reqSealInfo = MapUtils.getMap(data, "EXT_REQ_INFO", null);
			if(reqSealInfo == null) {
				setStatus(70201);
				throw new Exception("Not Found External Seal Info!");
			}
			
			//결재자들 정보
			List<Map<String, String>> appLineList = (List<Map<String, String>>) data.get("APP_LINE");
			if(appLineList == null || appLineList.size() == 0) {
				setStatus(70004);
				throw new Exception("Not Found Approval Info!");
			}
			
			//기타 결재 정보(상신의견 등)
			Map<String, String> appInfo = (Map<String, String>) data.get("APP_INFO");
			List<Map<String, Object>> documentList = (List<Map<String, Object>>) data.get("DOCUMENT_LIST");
			
			//ApitemVo 가공
			Map<String, Object> apItem = makeAppVO(reqSealInfo, documentList, appInfo, appLineList, conf, srm);
			if(apItem == null || apItem.entrySet().size() == 0) {
				setStatus(70005);
				throw new Exception("A problem occurred while processing the Approval Data!");
			}
			
			//날인 예상 서류 수집
			List<Map<String, String>> files = null;
			try {
				files = doSelectExternalSealDocument(documentList);
			} catch (Exception e) {
				setStatus(70202);
				throw new Exception("Failed to search ExternalSeal files to be attached to Approval.", e);
			}
			
			String url = conf.getString("sepoa.secl.req.approval.host", "http://openapi.stage.samsung.net") + conf.getString("sepoa.secl.req.approval.url", "/approval/api/v2.0/approvals/submit");
			String restResult = null;
			attachments = new ArrayList<File>();
			
			//req approval Api 발송
			RestUtil util = new RestUtil();
			
			//set Header
			util.setHeaderData(RestUtil.REQ_METHOD, "POST");
			util.setHeaderData("System-ID", conf.getString("sepoa.secl.req.approval.systemID", "KCD80REST00016"));
			util.setHeaderData("Authorization", conf.getString("sepoa.secl.req.approval.auth.header", "Bearer 2c1628c0-8e84-3db6-b274-690e4713293c"));
			util.setHeaderData("Content-type", "multipart/form-data");
			Logger.info.println("Req Approval URL : " + url);
			
			//set body Object
			Map<String, Object> body = new LinkedHashMap<String, Object>();
			
			//set body (Json)
			body.put("approval", new Gson().toJson(apItem));
			
			//set ext Seal Doc
			if(files != null && files.size() > 0) {
				for(Map<String, String> file : files) {
					File tmpFile = new File(file.get("PATH"));
					attachments.add(tmpFile);
				}
			}
			
			//set Body (File)
			if(attachments.size() > 0) {
				body.put("attachments", attachments);
			}
			
			//set Params
			util.setParamsData(body);
			
			//call
			try {
				restResult = util.connect(url);
			} catch(Exception e) {
				setStatus(70006);
				e.printStackTrace();
				throw new Exception("There was a problem calling the approval request API.", e);
			}
			
			// parse result
			if(util.getResponseCode() != HttpURLConnection.HTTP_OK) {
				Map<String, String> resultMap = new Gson().fromJson(restResult, Map.class);
				if(resultMap.containsKey("errorCode")) {
					switch(resultMap.get("errorCode")) {
					case "APE015":
						setStatus(70010);
						Logger.err.println("Attache File Size Over! : " + restResult);
						throw new Exception("Attache File Size Over! : " + restResult);
					case "APE017":
						setStatus(70011);
						Logger.err.println("ApIn is invalid" + restResult);
						throw new Exception("ApIn is invalid : " + restResult);
					default :
						setStatus(70099);
						Logger.err.println("Knox Approval System Error! : " + restResult);
						throw new Exception("Knox Approval System Error! : " + restResult);
					}
				} else {
					setStatus(70007);
					Logger.err.println("Approval Request Result Not 200! : " + restResult);
					throw new Exception("Approval Request Result Not 200! : " + restResult);
				}
			} else {
				if(!restResult.trim().isEmpty() && restResult != null) {
					Map<String, Object> responseData = new Gson().fromJson(restResult, Map.class);
					if("success".equalsIgnoreCase(String.valueOf(responseData.get("result")))) {
						Logger.info.println("==================== Req Approval API Send Success! ====================", restResult);
					} else {
						setStatus(70007);
						throw new Exception("Req Approval Result is Not Success!");
					}
				} else {
					Logger.err.println("Approval Request Result is Empty! : " + restResult);
					throw new Exception("Approval Request Result is Empty! : " + restResult);
				}
			}
		} catch (Exception e) {
			Logger.info.println("==================== External Req Approval API Send Fail! ====================", e);
			e.printStackTrace();
			if(getStatus() == 1) {
				setStatus(70088);
			}
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		} finally {
			//이후 진행될 프로세스를 위한 원본 데이터 복원 
			if(srm != null) {
				srm.setDocBaiscInfo(orgDocBasicInfo);
			}
			//프리뷰 쓰레기 파일 삭제
			boolean devFlag = Boolean.parseBoolean(CommonUtil.getConfig("sepoa.server.development.flag", "false"));
			if(!devFlag && attachments != null && attachments.size() > 0) {
				for(File targetFile : attachments) {
					try {
						FileUtils.forceDelete(targetFile);
					} catch (IOException e) {
						Logger.err.println("target file force delete fail! : " + targetFile, e);
					}
				}
			}
		}
		
		return getSepoaOut();
	}
	
	@Description("사용인감 날인요청 결재 요청")
	public SepoaOut useSealReqApproval(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		System.out.println("=== HOUSE_CODE 019 APPROVAL ===");
		
		try {
			//결재 요청 API 호출
			reqApprovalDoProcess(data);
		} catch (Exception e) {
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("법인인감 날인요청 결재 요청")
	public SepoaOut corpSealReqApproval(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		try {
			//결재 요청 API 호출
			reqApprovalDoProcess(data);
		} catch (Exception e) {
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("증명서발급 날인요청 결재 요청")
	public SepoaOut certReqApproval(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		try {
			//결재 요청 API 호출
			reqApprovalDoProcess(data);
		} catch (Exception e) {
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("현대중공업 결재라인 조회 서비스")
	@SuppressWarnings("unchecked")
	public SepoaOut doSelectSealInfo(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			List<Map<String, String>> rslt = new ArrayList<Map<String, String>>();
			
			// 1. 상신하는 사람의 부서장(세션의 dept로 sdept테이블 조회해서 MANAGER_ID따고 그걸로 SUSMT조회해서 세팅하면 됨)
			sxp = new SepoaXmlParser(this, "doSelectSealInfoDeptHead");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				Map<String, String> deptManager = sf.getListMap().get(0);
				rslt.add(deptManager);
			}

			// 2. 기준정보 용도관리 - 용도별 결재자 (모듈 SID1681780347367 켜져있는지 검사 후 진행 SAPUD에 용도번호(YDNUMBER)로 조회하면 됩니다. )
			/**
			String useNumber = MapUtils.getString(headerData, "USE_NUMBER", null);
			if(StringUtils.isNotBlank(useNumber) && SepoaModuleService.checkBusiness(new Configuration(), info, "SID1681780347367")) {
				sxp = new SepoaXmlParser(this, "doSelectSealInfoPurpose");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
				
				if(sf.getRowCount() > 0) {
					for(Map<String, String> purposeApprover : sf.getListMap()) {
						rslt.add(purposeApprover);
					}
				}
			}
			*/

			// 3. 인감관리 - 인감목록 - 인감별 결재자 (모듈 US1681189618235 켜져있는지 검사 후 진행 SAPUD에 인감번호 (SLM_NUMBER)로 조회하면 됩니다. )
			String manageNumber = MapUtils.getString(headerData, "MANAGER_NUMBER", null);
			if(StringUtils.isNotBlank(manageNumber) && SepoaModuleService.checkBusiness(new Configuration(), info, "US1681189618235")) {
				sxp = new SepoaXmlParser(this, "doSelectSealInfoSeal");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
				
				if(sf.getRowCount() > 0) {
					for(Map<String, String> sealApprover : sf.getListMap()) {
						rslt.add(sealApprover);
					}
				}
			}
			
			// 4. 총무담당자 (이거는 이미 작업이 되어있을 겁니다. 마지막에 세팅 되게만 해주시면 될듯요?)
			/**
			sxp = new SepoaXmlParser(this, "doSelectSealInfoCtrl");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				for(Map<String, String> ctrlApprover : sf.getListMap()) {
					rslt.add(ctrlApprover);
				}
			}
			*/
			
			sf = new SepoaFormater(rslt);
			setValue(sf);			
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
	
	
	@Description("외부 결재 처리 로직")
	private void reqApprovalDoProcess(Map<String, Object> data) throws Exception {
		Map<String, String> orgDocBasicInfo = new HashMap<String, String>();
		List<File> attachments = null;
		SepoaReqSealManager srm = null;
		
		try {
			Config conf = new Configuration();
			
			//날인요청 매니저 객체 초기화
			srm = (SepoaReqSealManager) data.get("MANAGER_OBJ");
			if(srm == null) {
				setStatus(70002);
				throw new Exception("SepoaReqSealManager Init Fail!");
			}
			
			//원본 데이터 백업용 (이후 프로세스 흘러갈 것을 고려해 백업함)
			orgDocBasicInfo.putAll(srm.getDocBaiscInfo());
			
			//날인요청 정보
			Map<String, String> reqSealInfo = MapUtils.getMap(data, "REQ_SEAL_INFO", null);
			if(reqSealInfo == null) {
				setStatus(70003);
				throw new Exception("Not Found Requset Seal Info!");
			}
				
			//결재자들 정보
			List<Map<String, String>> appLineList = (List<Map<String, String>>) data.get("APP_LINE");
			if(appLineList == null || appLineList.size() == 0) {
				setStatus(70004);
				throw new Exception("Not Found Approval Info!");
			}
			//기타 결재 정보(상신의견 등)
			Map<String, String> appInfo = (Map<String, String>) data.get("APP_INFO");
			List<Map<String, Object>> documentList = (List<Map<String, Object>>) data.get("DOCUMENT_LIST");
			//ApitemVo 가공
			Map<String, Object> apItem = makeAppVO(reqSealInfo, documentList, appInfo, appLineList, conf, srm);
			if(apItem == null || apItem.entrySet().size() == 0) {
				setStatus(70005);
				throw new Exception("A problem occurred while processing the Approval Data!");
			}
			
			if("C".equalsIgnoreCase(reqSealInfo.get("APPLY_TYPE")) || "U".equalsIgnoreCase(reqSealInfo.get("APPLY_TYPE"))) {
				//업로드 파일 조회 및 프리뷰 파일 가공
				List<Map<String, String>> files = null;
				try {
					files = doSelectUploadFiles(conf, reqSealInfo, srm);
				} catch (Exception e) {
					setStatus(70014);
					throw new Exception("Failed to search ReqSeal files to be attached to Approval.");
				}
				
				//Extra Info Files
				List<Map<String, String>> extraFiles = null;
				try {
					extraFiles = doSelectExtraDataFiles(reqSealInfo); 
				} catch (Exception e) {
					setStatus(70013);
					throw new Exception("Failed to search etc files to be attached to Approval.");
				}
			}
			
			String A_SYSKEY = "";
			
			if(data.get("APP_NO")!= null) {
				A_SYSKEY = reqSealInfo.get("SMR_NUMBER") + "#" + data.get("APP_NO");
			} else {
				// doceditor에서 app_id를 파라미터에 추가해줘야 함
				throw new Exception("=================  No APP_NO in data  =================");
			}
			
			attachments = new ArrayList<File>();
			
			// 전자결재 외부결재 연동
			ApprovalWebServiceStub stub = new ApprovalWebServiceStub();
			CreateWorkflow cr = new CreateWorkflow();
			
			String createXML = "";
			String bodyHTML = (String)apItem.get("contents");
			String CIRCULAR = ""; // 참조  
			
			createXML += "<CreateWorkflow>"
				    +  "<Application>"
				    +  "<FormID>Lob546</FormID>"
				    +  "<Title>" + reqSealInfo.get("SUBJECT") + "</Title>"
				    +  "<AuthorID>" +info.getSession("ID") + "</AuthorID>"
				    +  "<AuthorComment>" + "#AuthorComment#" + "</AuthorComment>"
				    +  "<BodyType>HTML</BodyType>"
				    +  "<Body Type=\"html\">"
				    +  "<FORMNAME>" + reqSealInfo.get("SUBJECT") + "</FORMNAME>"
				    +  "<CONTENTS><![CDATA[" + bodyHTML + "]]></CONTENTS>"
				    +  "<A_SYSKEY>" + A_SYSKEY + "</A_SYSKEY>"
					+  "</Body>"
					+  "</Application>"
					+  "<Participant>";
//			결재선 등록
			int cnt = 101;
			
			List<Map<String, Object>> applines = (List<Map<String, Object>>)apItem.get("aplns");
			
//			Collections.sort(applines, new Comparator<Map<String, Object>>() {
//				@Override
//				public int compare(Map<String, Object> o1, Map<String, Object> o2) {
//					Integer seq1 = Integer.parseInt((String)o1.get("seq"));
//					Integer seq2 = Integer.parseInt((String)o2.get("seq"));
//					return seq2.compareTo(seq1);
//				}
//			});
			
			for(Map<String, Object> line : applines) {
				if(line.get("role") != null) {
					if(!line.get("role").equals("") && Integer.parseInt((String)line.get("role")) > 0) {
						createXML +=  "<Line Name=\"E" + cnt + "\">" + line.get("userId") + "</Line>";   // 송정훈 DR
						cnt++;
					} else if (!line.get("role").equals("") && Integer.parseInt((String)line.get("role")) == 0){
						createXML = createXML.replace("#AuthorComment#", (String)line.get("opinion"));
					} else if (line.get("role").equals("")) {
						// 참조
						CIRCULAR += line.get("userId") + ";";
					}
				}
			}
			
			if(!CIRCULAR.equals("")) {
				// 참조 추가
				createXML +=  "<Line Name=\"CIRCULAR\">" + CIRCULAR + "</Line>"; 
			}
			
			createXML +=  "</Participant>"
					+  "</CreateWorkflow>";
			
			cr.setXmlSrc(createXML);
			
			System.out.println("[createXML] " + createXML);
			
			CreateWorkflowResponse rs =  stub.createWorkflow(cr);
			String rsmsg = rs.getCreateWorkflowResult();
			
			System.out.println("[RESULT] " + rsmsg);
			
			if(rsmsg.toUpperCase().startsWith("E")) {
				throw new Exception(rsmsg);
			} 
			
		} catch (Exception e) {
			Logger.info.println("==================== Req Approval API Send Fail! ====================", e);
			if(getStatus() == 1) {
				setStatus(70088);
			}
			setFlag(false);
			setMessage(e.getMessage());
		} finally {
			//이후 진행될 프로세스를 위한 원본 데이터 복원 
			if(srm != null) {
				srm.setDocBaiscInfo(orgDocBasicInfo);
			}
			//프리뷰 쓰레기 파일 삭제
			boolean devFlag = Boolean.parseBoolean(CommonUtil.getConfig("sepoa.server.development.flag", "false"));
			if(!devFlag && attachments != null && attachments.size() > 0) {
				for(File targetFile : attachments) {
					FileUtils.forceDelete(targetFile);
				}
			}
		}
		
	}

	private Map<String, Object> makeAppVO(Map<String, String> reqSealInfo, List<Map<String, Object>> documentList, Map<String, String> appInfo, List<Map<String, String>> appLineList, Config conf, SepoaSealManager srm) throws Exception{
		Map<String, Object> apItemMap = new HashMap<>();
		List<Map<String, String>> apIns = new ArrayList<>();
		String contents = "";
		
		if(srm instanceof SepoaReqSealManager) {
			contents = makeContents(reqSealInfo, documentList, srm); 
		} else if(srm instanceof SepoaExtSealManager) {
			contents = makeContentsExt(reqSealInfo, documentList, srm);
		}
		
		//Sepoa 결재 정보 조회
		SepoaXmlParser sxp = new SepoaXmlParser(this, "doSelectApprovalInfo");
		SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
		SepoaFormater sf = new SepoaFormater(ssm.doSelect(reqSealInfo));
		String appNo = "";
		if(sf.getRowCount() > 0) {
			Map<String, String> approvalInfo = sf.getListMap().get(0);
			appNo = approvalInfo.get("APP_NO");
		} else {
			setStatus(70012);
			Logger.err.println("Not Found Approval Info!");
			throw new Exception("Not Found Approval Info!");
		}
		
		String apInfId = SepoaString.getRpad(reqSealInfo.get("SMR_NUMBER") + appNo, 32, "0");
		//마스터 데이터 작성
		apItemMap.put("aplns", apIns);
		apItemMap.put("contentsType", "HTML");
		apItemMap.put("docSecuType", "PERSONAL");
		apItemMap.put("notifyOption", "0");
		apItemMap.put("urgYn", "N");
		apItemMap.put("sbmDt", SepoaDate.getFormatString("yyyyMMddHHmmss"));
		apItemMap.put("timeZone", "GMT+9");
		apItemMap.put("docMngSaveCode", "0");
		apItemMap.put("subject", reqSealInfo.get("SUBJECT"));
		apItemMap.put("sbmLang", "KO");
		apItemMap.put("apInfId", apInfId);
		apItemMap.put("contents", contents);
		
		Collections.sort(appLineList, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> o1, Map<String, String> o2) {
				Integer seq1 = Integer.parseInt((String)o1.get("APP_LINE_SEQ"));
				Integer seq2 = Integer.parseInt((String)o2.get("APP_LINE_SEQ"));
				return seq1.compareTo(seq2);
			}
		});
		
		//결재자 데이터 작성
		for(Map<String, String> approvalInfo : appLineList) {
			String idx = String.valueOf(appLineList.indexOf(approvalInfo));
			Map<String, String> tmpApprover = new HashMap<String, String>();
			tmpApprover.put("epId", MapUtils.getString(approvalInfo, "EMPLOYEE_NO", ""));
			tmpApprover.put("userId", MapUtils.getString(approvalInfo, "APP_USER_ID", ""));
			tmpApprover.put("seq", idx);
			tmpApprover.put("role", convertRoleValue(approvalInfo.get("APP_METHOD")));
			tmpApprover.put("aplnStatsCode", "0");
			tmpApprover.put("arbPmtYn", "Y");
			tmpApprover.put("contentsMdfyPmtYn", "Y");
			tmpApprover.put("aplnMdfyPmtYn", "Y");
			tmpApprover.put("opinion", "");
			
			if(appLineList.indexOf(approvalInfo) == 0) {
				//상신자 전용 데이터 작성 구간 opinion 덮어씌우기		
				tmpApprover.put("opinion", appInfo.get("APP_COMMENT"));
			}
			
			apIns.add(tmpApprover);
		}
		
		return apItemMap;
	}
	
	//날인요청 전용 본문 만들기
	private String makeContents(Map<String, String> reqSealInfo, List<Map<String, Object>> documentList, SepoaSealManager srm) throws Exception{
		String templateRootPath = ThreadLocalFilter.getHttpServletRequest().getServletContext().getRealPath("/"); 
		String templateFilePath = templateRootPath + "/resource/template/approval/externalRequestApprovalDefaultTemplate";
		String contents = null;
		
		try {
			
			try {
				contents = new String(FileUtils.readFileToString(new File(templateFilePath), "UTF-8"));
			} catch(Exception e) {
				Logger.err.println("Approval Request Template Load Fail! Set Default Template");
				contents = new String(""
						+ "신청명 : ${SUBJECT}"
						+ "<br/><br/>"
						+ "설명 : ${EXPLANATION}"
						+ "<br/><br/>"
						+ "구분 : ${RE_FLAG}"
						+ "<br/><br/>"
						+ "용도 : ${SEAL_USE_TYPE}"
						+ "<br/><br/>"
						+ "제출처 : ${SUBMISSION_PLACE}"
						+ "<br/><br/>"
						+ "날인 대상 서류 : <br/>" 
						+ "${DOCUMENT_LIST}"
						+ "<br/><br/>"
						+ "교부요청서류 : <br/>" 
						+ "${ATTACH_DOCUMENT_LIST}");
			}
			
			String useSealText = "";
			if(documentList != null && documentList.size() > 0) {
				List<String> tmpSelectSealText = new ArrayList<String>();
				
				for(Map<String, Object> documentData : documentList) {
					if(documentData.containsKey("SELECT_SEAL_LIST")) {
						List<Map<String, String>> selectSealList = (List<Map<String, String>>) documentData.get("SELECT_SEAL_LIST");
						for(Map<String, String> selectSeal : selectSealList) {
							tmpSelectSealText.add(selectSeal.get("SUBJECT")+"("+selectSeal.get("MANAGE_NUMBER")+")");
						}
					}
				}
				
				useSealText = String.join(", ", tmpSelectSealText);
			}
			
			SepoaXmlParser sxp = new SepoaXmlParser(this, "doSelectUse");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(reqSealInfo));
			List<Map<String, String>> useList = sf.getListMap();
			String useText = "";
			if(useList != null && useList.size() > 0) {
				List<String> tmpUseTextList = new ArrayList<String>();
				for(Map<String, String> useData : useList) {
					tmpUseTextList.add(useData.get("SUBJECT"));
				}
				
				useText = String.join(", ", tmpUseTextList);
			}
			
			
			
			List<Map<String, String>> uploadFiles = new ArrayList<Map<String, String>>();
			if(reqSealInfo.get("APPLY_TYPE").equals("U") || reqSealInfo.get("APPLY_TYPE").equals("C")) {
				uploadFiles = ((SepoaReqSealManager)srm).doSelectReqSealDoc(new SingletonMap("SMR_NUMBER", reqSealInfo.get("SMR_NUMBER")));
			} else if(reqSealInfo.get("APPLY_TYPE").equals("D")) { // 제증명의 경우 SSISD 에서 조회한다.
				uploadFiles = ((SepoaReqSealManager)srm).doSelectCorpAttachDoc(new SingletonMap("SMR_NUMBER", reqSealInfo.get("SMR_NUMBER")));
			}
			String uploadFileText = "";
			if(uploadFiles != null && uploadFiles.size() > 0) {
				List<String> tmpUploadFileTextList = new ArrayList<String>();
				for(Map<String, String> filesData : uploadFiles) {
					if(reqSealInfo.get("APPLY_TYPE").equals("D")) {
						List<Map<String, String>> ssradList = ((SepoaReqSealManager)srm).doSelectCorpAttachDocHeader(new SingletonMap("SMI_NUMBER", filesData.get("SMI_NUMBER")));
						if(!filesData.get("REAL_DOC_CHK").equals("R")) {
							String access_link = "";
							Config config = new Configuration();
							String systemDomain = config.get("sepoa.system.domain.name") +"/link.sux?ln=";
							// 날인서류 외부조회 링크
							sxp = new SepoaXmlParser(this, "doSelectAccessLink");
							ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
							sf = new SepoaFormater(ssm.doSelect(filesData));
							
							if(sf!=null) {
								access_link = sf.getValue("ACCESS_LINK", 0);
							}
							
							tmpUploadFileTextList.add("<a href='"+ systemDomain + sf.getValue("ACCESS_LINK", 0) + "' target='_blank'>" + ssradList.get(0).get("SUBJECT") + "</a>");
						} else {
							tmpUploadFileTextList.add(ssradList.get(0).get("SUBJECT"));
						}
					} else {
						String access_link = "";
						Config config = new Configuration();
						String systemDomain = config.get("sepoa.system.domain.name") +"/link.sux?ln=";
						// 날인서류 외부조회 링크
						sxp = new SepoaXmlParser(this, "doSelectAccessLink");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
						sf = new SepoaFormater(ssm.doSelect(filesData));
						
						if(sf!=null) {
							access_link = sf.getValue("ACCESS_LINK", 0);
						}
						tmpUploadFileTextList.add("<a href='"+ systemDomain + sf.getValue("ACCESS_LINK", 0) + "' target='_blank'>" + filesData.get("SUBJECT") + "</a>");
					}
				}
				
				uploadFileText = String.join("<br/> ", tmpUploadFileTextList);
			} else {
				uploadFileText = " - 첨부된 파일이 없습니다.";
			}
			
			
			
			
			sxp = new SepoaXmlParser(this, "doSelectAttachDocHeaders");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
			sf = new SepoaFormater(ssm.doSelect(reqSealInfo));
			List<Map<String, String>> attachDocHeaders = sf.getListMap();
			String attachDocFileText = "";
			if(attachDocHeaders != null && attachDocHeaders.size() > 0) {
				List<String> tmpAttachDocTextList = new ArrayList<String>();
				for(Map<String, String> attachDocHeader : attachDocHeaders) {
					tmpAttachDocTextList.add(" - " + attachDocHeader.get("SUBJECT"));
				}
				
				attachDocFileText = String.join("<br/> - ", tmpAttachDocTextList);
			} else {
				attachDocFileText = " - 요청한 교부서류가 없습니다.";
			}
			
			//smr넘버로 sfile을 조회하는 로직을 만들고 del_flag = n
			// http://local.hhi.sepoa.co.kr:17790/CustomDownload.sux?attach_key=SF23102400006&attach_seq=2
			sxp = new SepoaXmlParser(this, "doSelectSfile");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
			sf = new SepoaFormater(ssm.doSelect(reqSealInfo));
			List<Map<String, String>> attachFileList = sf.getListMap();
			String attachFileText = "";
			if(attachFileList != null && attachFileList.size() > 0) {
				List<String> tmpAttachFileTextList = new ArrayList<String>();
				String systemDomain = CommonUtil.getConfig("sepoa.system.domain.name") +"/CustomDownload.sux?";
				for(Map<String, String> file : attachFileList) {
					String attachKey = file.get("DOC_NO");
					String attachSeq = file.get("DOC_SEQ");
					
					tmpAttachFileTextList.add("<a href='"+ systemDomain + "attach_key="+attachKey.toUpperCase()+"&attach_seq="+attachSeq+"' target='_blank'>" + file.get("SRC_FILE_NAME") + "</a>");
				}
				
				attachFileText = String.join("<br/> ", tmpAttachFileTextList);
			} else {
				attachFileText = " - 첨부한 파일이 없습니다.";
			}
			
			if(reqSealInfo.get("APPLY_TYPE").equals("U") || reqSealInfo.get("APPLY_TYPE").equals("C")) {
				String reStr = ("E".equalsIgnoreCase(reqSealInfo.get("RE_FLAG")) ? Cache.text.get("SLM_211.ELECTRON_FLAG_1") : Cache.text.get("SLM_211.REAL_FLAG_1"));
				String applyStr = ("U".equalsIgnoreCase(reqSealInfo.get("APPLY_TYPE")) ? "사용인감" : "법인인감");
				String sealInfoStr = reStr + applyStr;
				
				String textExplation = null;
				textExplation = reqSealInfo.get("EXPLANATION");
				textExplation = textExplation.replace("\n", "<br/>").replace("\r\n", "<br/>");
				contents = contents.replace("${SUBJECT}", reqSealInfo.get("SUBJECT"));
				contents = contents.replace("${EXPLANATION}", textExplation);
				contents = contents.replace("${RE_FLAG}", sealInfoStr);
				contents = contents.replace("${SEAL_USE_TYPE}", useText);
				contents = contents.replace("${SUBMISSION_PLACE}", reqSealInfo.get("SUBMISSION_PLACE"));
				contents = contents.replace("${DOCUMENT_LIST}", uploadFileText);
				contents = contents.replace("${ATTACH_DOCUMENT_LIST}", attachDocFileText);
				contents = contents.replace("${ATTACH_FILE_LIST}", attachFileText);
				contents = contents.replace("${MANAGE_NUMBER}", useSealText);
				contents = contents.replace("${SMR_NUMBER}", reqSealInfo.get("SMR_NUMBER"));
				contents = contents.replace("${USE_NUMBER}", reqSealInfo.get("USE_NUMBER")); 
				
			} else if(reqSealInfo.get("APPLY_TYPE").equals("D")) {
				String textExplation = null;
				textExplation = reqSealInfo.get("EXPLANATION");
				textExplation = textExplation.replace("\n", "<br/>").replace("\r\n", "<br/>");
				contents = contents.replace("${SUBJECT}", reqSealInfo.get("SUBJECT")); //제목 -> 템플릿 파일 조회 실패 시
				contents = contents.replace("${EXPLANATION}", textExplation);// 설명
				contents = contents.replace("${RE_FLAG}", "");// (구분)
				contents = contents.replace("${SEAL_USE_TYPE}", ""); // 용도 -> 템플릿 파일 조회 실패 시
				contents = contents.replace("${SUBMISSION_PLACE}", reqSealInfo.get("SUBMISSION_PLACE"));//제출처
				contents = contents.replace("${DOCUMENT_LIST}", "");// 날인서류
				contents = contents.replace("${ATTACH_DOCUMENT_LIST}", uploadFileText); // 교부서류 -> 교부서류에 SSISD ACCESS LINK를 담기위해 uploadFileText 로 지정한다.
				contents = contents.replace("${ATTACH_FILE_LIST}", attachFileText); // 첨부파일
				contents = contents.replace("${MANAGE_NUMBER}", "");// 인장번호
				contents = contents.replace("${SMR_NUMBER}", reqSealInfo.get("SMR_NUMBER"));// 관리번호
				contents = contents.replace("${USE_NUMBER}", Cache.text.get("SLM_211.ATTACH_DOC_NAME")); // 구분
			}
			
			/*
			if("U".equalsIgnoreCase(applyStr) || "C".equalsIgnoreCase(applyStr)) {
				contents = contents.replace("${REMARK}", reqSealInfo.get("REMARK"));
			}*/
		} catch(Exception e) {
			throw new Exception("Make Approval Contents Fail!", e);
		}
		return contents;
	}
	
	//반출 전용 컨텐츠 만들기
	private String makeContentsExt(Map<String, String> extSealInfo, List<Map<String, Object>> documentList, SepoaSealManager srm) throws Exception{
		SepoaExtSealManager tmpManager = (SepoaExtSealManager) srm;
		
		String templateRootPath = ThreadLocalFilter.getHttpServletRequest().getServletContext().getRealPath("/"); 
		String templateFilePath = templateRootPath + "/resource/template/approval/externalSealExtApprovalDefaultTemplate";
		String contents = new String(FileUtils.readFileToString(new File(templateFilePath), "UTF-8"));
		
		String outDate = extSealInfo.get("EXT_OUT_EXPECT_DATE");
		String outHour = extSealInfo.get("EXT_OUT_EXPECT_HOUR");
		String inDate = extSealInfo.get("EXT_IN_EXPECT_DATE");
		String inHour = extSealInfo.get("EXT_IN_EXPECT_HOUR");
		String outDateHour = "";
		String inDateHour = "";
		
		SimpleDateFormat oldFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat newFormat = new SimpleDateFormat("yyy년 MM월 dd일");
		Date dateParser = null;
		
		dateParser = oldFormat.parse(outDate);
		outDateHour = newFormat.format(dateParser) + " " + outHour + "시";
		
		dateParser = oldFormat.parse(inDate);
		inDateHour = newFormat.format(dateParser) + " " + inHour + "시";
		
		String attachDocInfo = "";
		List<String> tmpSelectSealTextjoiner = new ArrayList<String>();
		List<String> tmpDocFileTextJoiner = new ArrayList<String>();
		String sealText = "";
		String attachFileText = "";
		for(Map<String, Object> document : documentList) {
			List<Map<String, String>> selectSealList = (List<Map<String, String>>) document.get("SELECT_EXT_LIST");
			
			for(Map<String, String> selectSeal : selectSealList) {
				tmpSelectSealTextjoiner.add(selectSeal.get("SUBJECT") + "(" + selectSeal.get("MANAGE_NUMBER") +")");
			}
			
			SepoaXmlParser sxp = new SepoaXmlParser(this, "doSelectExternalDocList");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(new SingletonMap("ATTACH_NO" , String.valueOf(document.get("ATTACH_NO")))));
			if(sf.getRowCount() > 0) {
				for(Map<String, String> docFile : sf.getListMap()) {
					tmpDocFileTextJoiner.add(" - " + docFile.get("SRC_FILE_NAME"));
				}
			}
		}
		sealText = String.join("<br/>", tmpSelectSealTextjoiner);
		attachFileText = String.join("<br/>", tmpDocFileTextJoiner);
		
		contents = contents.replace("${SEAL_INFO}", sealText);
		contents = contents.replace("${EXT_REQ_NUMBER}", extSealInfo.get("SMR_NUMBER"));
		contents = contents.replace("${SEAL_USE_TYPE}", extSealInfo.get("SEAL_USE_TYPE_LOC"));
		contents = contents.replace("${OUT_IN_TIME}", outDateHour + " ~ " + inDateHour);
		contents = contents.replace("${SUBMISSION_PLACE}", extSealInfo.get("SUBMISSION_PLACE"));
		contents = contents.replace("${REMARK}", extSealInfo.get("REMARK"));
		contents = contents.replace("${ATTACH_DOC_INFO}", attachFileText);
		return contents;
	}
	
	
	
	private String convertRoleValue(String sepoaAppMethod) {
		/*sepoa -> secl (reqapproval)
		 <select class="table-body-item-inner table-body-item-center" data-key="APP_METHOD">
		 	<option value="04">결재</option>
		 	<option value="06">합의</option>
		 	<option value="07">통보</option>
		 </select> 
		 */
		String result = "";
		switch(sepoaAppMethod) {
		case "01": //상신
			result = "0";
			break;
		case "04": //결재
			result = "1";
			break;
		case "06": //합의	
			result = "2";
			break;
		case "07": //통보
			result = "9";
			break;
		}
		
		return result;
	}
	
	private List<Map<String, String>> doSelectExternalSealDocument(List<Map<String, Object>> documentList) throws Exception{
		List<Map<String, String>> externalSealDocumentList = new ArrayList<Map<String, String>>();
		
		if(documentList.size() > 0) {
			SepoaXmlParser sxp = null;
			SepoaSQLManager ssm = null;
			SepoaFormater sf = null;
			
			for(Map<String, Object> document : documentList) {
				Map<String, String> queryParams = new HashMap<String, String>();
				for(Entry<String, Object> entry : document.entrySet()) {
					if(entry.getValue() instanceof String) {
						queryParams.put(entry.getKey(), String.valueOf(entry.getValue()));
					}
				}
				
				sxp = new SepoaXmlParser(this, "doSelectExternalDocList");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
				sf = new SepoaFormater(ssm.doSelect(queryParams));
				
				if(sf.getRowCount() > 0) {
					List<Map<String, String>> externalFileList = sf.getListMap();
					for(Map<String, String> externalFile : externalFileList) {
						Map<String, String> tmpFile = new HashMap<String, String>();
						
						String type = externalFile.get("TYPE");
						String desFileName = externalFile.get("DES_FILE_NAME");
						String srcFileName = externalFile.get("SRC_FILE_NAME"); 
						
						String orgFileRootPath = SepoaMultipart.getTypePath(type, "upload");
						String orgFilePath = orgFileRootPath + "/" + desFileName;
						File orgFile = new File(orgFilePath);
						
						String newFileRootPath = orgFile.getParent() + "/" +System.currentTimeMillis() + "_" +System.nanoTime();
						if(!new File(newFileRootPath).exists()) {
							FileUtils.forceMkdir(new File(newFileRootPath));
						}
						
						String newFilePath = newFileRootPath + "/" + srcFileName;
						File newFile = new File(newFilePath);
						
						FileUtils.copyFile(orgFile, newFile);
						tmpFile.put("PATH", newFilePath);
						
						externalSealDocumentList.add(tmpFile);
					}
				}
			}
		}
		
		return externalSealDocumentList;
	}
	
	private List<Map<String, String>> doSelectExtraDataFiles(Map<String, String> reqSealInfo) throws Exception{
		List<Map<String, String>> extraFileResult = new ArrayList<>();
		SepoaXmlParser sxp = new SepoaXmlParser(this, "doSelectExtraInfo");
		SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
		SepoaFormater sf = new SepoaFormater(ssm.doSelect(reqSealInfo));
		
		if(sf.getRowCount() > 0) {
			Map<String, String> extraInfoMap = sf.getListMap().get(0);
			Map<String, Object> extraData = new Gson().fromJson(extraInfoMap.get("DATA"), Map.class);
			
			String attachDoc = MapUtils.getString(extraData, "ATTACH_DOC", null);
			if(attachDoc != null && (!"".equals(attachDoc))) {
				sxp = new SepoaXmlParser(this, "doSelectExtraFiles");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
				sf = new SepoaFormater(ssm.doSelect(new SingletonMap("DOC_NO", extraData.get("ATTACH_DOC"))));
				
				if(sf.getRowCount() > 0) {
					for(Map<String, String> file : sf.getListMap()) {
						Map<String, String> tmpFile = new HashMap<String, String>();
						String type = file.get("TYPE");
						String desFileName = file.get("DES_FILE_NAME");
						String srcFileName = file.get("SRC_FILE_NAME");
						
						String orgFileRootPath = SepoaMultipart.getTypePath(type, "upload");
						String orgFilePath = orgFileRootPath + "/" + desFileName;
						File orgFile = new File(orgFilePath);
						
						String newFileRootPath = orgFile.getParent();
						String newFilePath = newFileRootPath + "/" + srcFileName;
						File newFile = new File(newFilePath);
						
						FileUtils.copyFile(orgFile, newFile);
						tmpFile.put("PATH", newFilePath);
						
						extraFileResult.add(tmpFile);
					}
				}
			}
		} 
		
		return extraFileResult;
	}
	
	private List<Map<String, String>> doSelectUploadFiles(Config conf, Map<String, String> reqSealInfo, SepoaReqSealManager srm) throws Exception {
		List<Map<String, String>> previewFiles = new ArrayList<>();
		String smrNumber = reqSealInfo.get("SMR_NUMBER");
		
		//pathManager 세팅
		PathManager pm = new PathManager(info, conf);
		pm.setLayer("REQ_SEAL");
		
		//SSIAD, SSIPD기반의 업로드 파일 정보 조회
		List<Map<String, String>> uploadFiles = srm.doSelectReqSealDoc(new SingletonMap("SMR_NUMBER", smrNumber));
		for(Map<String, String> uploadFile : uploadFiles) {
			String smdNumber = uploadFile.get("SMD_NUMBER");
			//파일 매니저 내 적재된 파일 정보 조회
			reqSealInfo.put("SMD_NUMBER", smdNumber);
			srm.setDocBaiscInfo(reqSealInfo);
			Map<String, String> fileData = srm.doSelectDocumentFile(); 
			
			//조회된 파일 없으면 에러상태값 적용
			if(fileData == null ) {
				setStatus(70001);
				throw new Exception("Unable to find the document related to the request for sealing.");
			}
			
			/**
			//fix_html조회
			Map<String, String> fixHtml = new Gson().fromJson(fileData.get("FIX_HTML"), Map.class);
			if(fixHtml == null) {
				fixHtml = new HashMap<>();
			}
			
			String srcFile = pm.getLayerPath() + fileData.get("FILE_NAME_SERVER") ;
			String desFile = srcFile + "_preview" + System.currentTimeMillis() + ".pdf";
			
			if(!srm.doConvertDocBeforeSeal(fixHtml, srcFile, desFile, smrNumber, smdNumber)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
					throw new Exception("There was a problem replacing the preview document.");
				}
			}
			fileData.put("preview_file", desFile);
			previewFiles.add(fileData);
			*/
		}
		
		return previewFiles;
	}
	
}

