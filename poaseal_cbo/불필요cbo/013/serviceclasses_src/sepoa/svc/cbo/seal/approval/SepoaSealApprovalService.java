package sepoa.svc.cbo.seal.approval;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaString;
import sepoa.fw.util.ux.sUtils;
import sepoa.seal.SepoaExtSealManager;
import sepoa.seal.SepoaReqSealManager;
import sepoa.seal.SepoaSealManager;
import sepoa.svl.common.RestUtil;
import sepoa.svl.util.SepoaMultipart;

@Description("포스코플로우 결재 서비스")
public class SepoaSealApprovalService extends SepoaService{
	public SepoaSealApprovalService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("포스코플로우 특정 결재선 세팅 서비스")
	public SepoaOut doSelectSealInfo(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		
		try {
			Map<String, String> headerData = (Map<String, String>) data.get(SepoaDataMapper.KEY_HEADER_DATA);
			
			List<Map<String, String>> resultList = new ArrayList<>();
			Map<String, String> approverCount = new HashMap<String,String>();
			
			//인터페이스 여부 조회
			resultList = doSelectApproverOrReferer(headerData, "doSelectSealInfo", false, false);
			
			if(resultList.size() > 0 && getStatus() != 0)  { // 조회결과가 하나라도 존재하면 인터페이스
				// 결재자 적재 리스트
				List<Map<String, String>> approverList = new ArrayList<>();
				// 참조자 적재 리스트
				List<Map<String, String>> refererList = new ArrayList<>();
				// 결재자, 참조자 데이터 적재 맵
				Map<String, Object> approvalSummaryData = new LinkedHashMap<>();
				
				//법인인감 담당자 조회
				resultList = doSelectApproverOrReferer(headerData, "doSelectCorpSealManager", false, true);
				if(resultList != null && resultList.size() > 0) {
					//결재자 : 법인인감 담당자
					approverList.addAll(resultList);
				}
				
				//경영기획그룹장, 법무섹션리더 조회
				resultList = doSelectApproverOrReferer(headerData, "doSelectFixedReferer", true, false);
				if(resultList != null && resultList.size() > 0) {
					//참조자 : 경영기획그룹장, 법무섹션리더 
					refererList.addAll(resultList);
				}
				
				
				//신청자 부서 리더 조회
				resultList = doSelectApproverOrReferer(headerData, "doSelectUserLeader", true, false);
				if(resultList != null && resultList.size() > 0) {
					//참조자 : 신청자 부서 리더
					refererList.addAll(resultList);
				}
				
				
				if(approverList != null && approverList.size() > 0) {
					approvalSummaryData.put("APPROVAL_DATA", approverList);
				}
				if(refererList != null && refererList.size() > 0) {
					approvalSummaryData.put("REFERER_DATA", refererList);
				}
				
				
				setValue(new Gson().toJson(approvalSummaryData));

				
			} else { // 조회결과 없으면 직접생성
				
				// 결재자 적재 리스트
				List<Map<String, String>> approverList = new ArrayList<>();
				// 참조자 적재 리스트
				List<Map<String, String>> refererList = new ArrayList<>();
				// 결재자, 참조자 데이터 적재 맵
				Map<String, Object> approvalSummaryData = new LinkedHashMap<>();
				
				//신청자 부서 그룹장 조회 임시 데이터
				List<Map<String, String>> tempList = new ArrayList<>();
				Map<String, String> firstDeptHeaderData = new HashMap<>();
				firstDeptHeaderData.putAll(headerData);
				
				switch(headerData.get("DIV_KEY")) {
					case "USE_SEAL": //사용인감 날인신청
						
						//신청자 부서 그룹장 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectParentDept", false, false);
						
						if(resultList != null && resultList.size() > 0 && resultList.get(0).get("PARENT_DEPT") != null) {
							firstDeptHeaderData.put("DEPT", resultList.get(0).get("DEPT"));
						}
						
						resultList = doSelectApproverOrReferer(firstDeptHeaderData, "doSelectUserLeader", false, false);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 신청자 부서 그룹장 추가
							approverList.addAll(resultList);
						}
						
						
						//사용인감 담당자 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectUseSealManager", false, true);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 사용인감 담당자
							approverList.addAll(resultList);
						}
						
						
						//경영기획그룹장, 법무섹션리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectFixedReferer", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 경영기획그룹장, 법무섹션리더 
							refererList.addAll(resultList);
						}
						
						
						//신청자 부서 리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectUserLeader", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 신청자 부서 리더
							refererList.addAll(resultList);
						}
						
						
						//법인인감 담당자 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectCorpSealManager", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 법인인감 담당자
							refererList.addAll(resultList);
						}
						
						if(approverList != null && approverList.size() > 0) {
							approvalSummaryData.put("APPROVAL_DATA", approverList);
						}
						if(refererList != null && refererList.size() > 0) {
							approvalSummaryData.put("REFERER_DATA", refererList);
						}
						
						
						setValue(new Gson().toJson(approvalSummaryData));
						break;
						
					case "CORP_SEAL": //법인인감 날인신청
						
						//신청자 부서 그룹장 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectParentDept", false, false);
						
						if(resultList != null && resultList.size() > 0 && resultList.get(0).get("PARENT_DEPT") != null) {
							firstDeptHeaderData.put("DEPT", resultList.get(0).get("DEPT"));
						}
						
						resultList = doSelectApproverOrReferer(firstDeptHeaderData, "doSelectUserLeader", false, false);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 신청자 부서 그룹장 추가
							approverList.addAll(resultList);
						}
						
						
						//법인인감 담당자 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectCorpSealManager", false, true);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 법인인감 담당자
							approverList.addAll(resultList);
						}
						
						
						
						//경영기획그룹장, 법무섹션리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectFixedReferer", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 경영기획그룹장, 법무섹션리더 
							refererList.addAll(resultList);
						}
						
						
						//신청자 부서 리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectUserLeader", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 신청자 부서 리더
							refererList.addAll(resultList);
						}
						
						if(approverList != null && approverList.size() > 0) {
							approvalSummaryData.put("APPROVAL_DATA", approverList);
						}
						if(refererList != null && refererList.size() > 0) {
							approvalSummaryData.put("REFERER_DATA", refererList);
						}
						
						
						setValue(new Gson().toJson(approvalSummaryData));
						break;
						
					case "CERT": //증명서 발급신청
						
						//신청자 부서 그룹장 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectParentDept", false, false);
						
						if(resultList != null && resultList.size() > 0 && resultList.get(0).get("PARENT_DEPT") != null) {
							firstDeptHeaderData.put("DEPT", resultList.get(0).get("DEPT"));
						}
						
						resultList = doSelectApproverOrReferer(firstDeptHeaderData, "doSelectUserLeader", false, false);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 신청자 부서 그룹장 추가
							approverList.addAll(resultList);
						}
						
						
						
						//법인인감 담당자 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectCorpSealManager", false, true);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 법인인감 담당자
							approverList.addAll(resultList);
						}
						
						
						
						//신청자 부서 리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectUserLeader", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 신청자 부서 리더
							refererList.addAll(resultList);
						}
						
						if(approverList != null && approverList.size() > 0) {
							approvalSummaryData.put("APPROVAL_DATA", approverList);
						}
						if(refererList != null && refererList.size() > 0) {
							approvalSummaryData.put("REFERER_DATA", refererList);
						}
						
						
						setValue(new Gson().toJson(approvalSummaryData));
						break;
				
					case "TEMPLATE_CONTRACT": //템플릿 계약
						
						
						//신청자 부서 그룹장 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectParentDept", false, false);
						
						if(resultList != null && resultList.size() > 0 && resultList.get(0).get("PARENT_DEPT") != null) {
							firstDeptHeaderData.put("DEPT", resultList.get(0).get("DEPT"));
						}
						
						resultList = doSelectApproverOrReferer(firstDeptHeaderData, "doSelectUserLeader", false, false);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 신청자 부서 그룹장 추가
							approverList.addAll(resultList);
						}
						
						
						
						//법인인감 담당자 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectCorpSealManager", false, true);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 법인인감 담당자
							approverList.addAll(resultList);
						}
						
						
						//경영기획그룹장, 법무섹션리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectFixedReferer", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 경영기획그룹장, 법무섹션리더 
							refererList.addAll(resultList);
						}
						
						//신청자 부서 리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectUserLeader", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 신청자 부서 리더
							refererList.addAll(resultList);
						}
						
						if(approverList != null && approverList.size() > 0) {
							approvalSummaryData.put("APPROVAL_DATA", approverList);
						}
						if(refererList != null && refererList.size() > 0) {
							approvalSummaryData.put("REFERER_DATA", refererList);
						}
						
						
						setValue(new Gson().toJson(approvalSummaryData));
						break;
						
						
					case "EXTERNAL": //인감반출 신청
						
						//신청자 부서 그룹장 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectParentDept", false, false);
						
						if(resultList != null && resultList.size() > 0 && resultList.get(0).get("PARENT_DEPT") != null) {
							firstDeptHeaderData.put("DEPT", resultList.get(0).get("DEPT"));
						}
						
						resultList = doSelectApproverOrReferer(firstDeptHeaderData, "doSelectUserLeader", false, false);
						if(resultList != null && resultList.size() > 0) {
							//결재자 : 신청자 부서 그룹장 추가
							approverList.addAll(resultList);
						}
						
						
						//사용인감 담당자 조회 -> 반출신청은 SMR NUMBER가 없고 SLM NUMBER 들을 직접 가져온다.
						String[] slmList = headerData.get("SLM_NUM_LIST").split(",");
						Map<String,String> splitHeaderData = new HashMap<>();
						String tempSlmNum = "";
						for(int i=0; i<slmList.length; i++) {
							if(tempSlmNum.equals(slmList[i])) continue;
							tempSlmNum = slmList[i];
							splitHeaderData.putAll(headerData);
							splitHeaderData.put("SLM_NUM", slmList[i]);
							resultList = doSelectApproverOrReferer(splitHeaderData, "doSelectUseSealManagerBySlmNumber", false, true);
							if(resultList != null && resultList.size() > 0) {
								//결재자 : 사용인감 담당자
								approverList.addAll(resultList);
							}
						}
						
						
						//경영기획그룹장, 법무섹션리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectFixedReferer", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 경영기획그룹장, 법무섹션리더 
							refererList.addAll(resultList);
						}
						
						
						//신청자 부서 리더 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectUserLeader", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 신청자 부서 리더
							refererList.addAll(resultList);
						}
						
						
						//법인인감 담당자 조회
						resultList = doSelectApproverOrReferer(headerData, "doSelectCorpSealManager", true, false);
						if(resultList != null && resultList.size() > 0) {
							//참조자 : 법인인감 담당자
							refererList.addAll(resultList);
						}
						
						if(approverList != null && approverList.size() > 0) {
							approvalSummaryData.put("APPROVAL_DATA", approverList);
						}
						if(refererList != null && refererList.size() > 0) {
							approvalSummaryData.put("REFERER_DATA", refererList);
						}
						
						
						setValue(new Gson().toJson(approvalSummaryData));
						break;
				}
			}
			
		} catch (Exception e) {
			setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
		
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
	
	@Description("Knox 결재선 리스트 조회")
	public SepoaOut getKnoxAppLine(Map<String, Object> data) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		try {
			Map<String, String> headerData = (Map<String, String>) data.get(SepoaDataMapper.KEY_HEADER_DATA);
			boolean isReqSeal = MapUtils.getBooleanValue(headerData, "isReqSeal", true);
			String queryName = "";
			if(isReqSeal) {
				queryName = "doQueryReqSealHeaerInfo";
			} else {
				queryName = "doQueryExtSealHeaderInfo";
			}
			
			sxp = new SepoaXmlParser(this, queryName);
			ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				Map<String, String> reqSealHeaderInfo = sf.getListMap().get(0);
				String apInfId = "";
				String appNo = "";
				
				sxp = new SepoaXmlParser(this, "doSelectApprovalInfo");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
				sf = new SepoaFormater(ssm.doSelect(headerData));
				
				if(sf.getRowCount() > 0) {
					Map<String, String> extAppInfo = sf.getListMap().get(0);
					appNo = extAppInfo.get("APP_NO");
				} else {
					setStatus(70100);
					throw new Exception("Not Found Ext Info for Approval Line");
				}
				
				if(isReqSeal) {
					String addDateStr = reqSealHeaderInfo.get("ADD_DATE");
					String oldStdDate = CommonUtil.getConfig("sepoa.req.seal.approval.oldstd.date", "20230405");
					
					//4월 5일자 이후로 결재 ID기준이 바뀌어 기존 결재건을 살리기 위해 아래와 같이 처리한다.
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
					Date stdDate = sdf.parse(oldStdDate);
					Date addDate = sdf.parse(addDateStr);
					if(addDate.after(stdDate)) {
						//Sepoa 결재 정보 조회
						apInfId = SepoaString.getRpad(headerData.get("SMR_NUMBER") + appNo, 32, "0");
					} else {
						apInfId = SepoaString.getRpad(headerData.get("SMR_NUMBER"), 32, "0");
					}
				}  else {
					apInfId = SepoaString.getRpad(headerData.get("SMR_NUMBER") + appNo, 32, "0");
				}
				
				String host = CommonUtil.getConfig("sepoa.secl.req.approval.host", "http://openapi.stage.samsung.net");
				String appStatusUrl = "/approval/api/v2.0/approvals/{apInfId}/detail";
				appStatusUrl = appStatusUrl.replace("{apInfId}", apInfId);
				String url = host + appStatusUrl;
				
				// 건 by 건으로 REST 호출
				String restResult = null;
				RestUtil util = new RestUtil();
				util.setHeaderData(RestUtil.REQ_METHOD, "GET");
				util.setHeaderData("System-ID", CommonUtil.getConfig("sepoa.secl.req.approval.systemID", "KCD80REST00016"));
				util.setHeaderData("Authorization", CommonUtil.getConfig("sepoa.secl.req.approval.auth.header", "Bearer 2c1628c0-8e84-3db6-b274-690e4713293c"));
				
				Logger.info.println("Req Approval Status URL : " + url);

				try {
					restResult = util.connect(url);
				} catch (Exception e) {
					setStatus(70012);
					throw new Exception("There was a problem checking the approval status.", e);
				}

				if (util.getResponseCode() != HttpURLConnection.HTTP_OK) {
					setStatus(70007);
					throw new Exception("Approval Status Result Not 200! [" + headerData.get("SMR_NUMBER") + "] : " + restResult);
				} else {
					if (!restResult.trim().isEmpty() && restResult != null) {
						Logger.info.println("Approval Status Result Success!" + restResult);
					} else {
						setStatus(70012);
						throw new Exception("Approval Status Result is Empty! [" + headerData.get("SMR_NUMBER")+ "] : " + restResult);
					}
				}
				
//				restResult = "{\"result\":\"success\",\"data\":{\"contentsType\":\"TEXT\",\"sbmDt\":\"20190521065357\",\"sbmLang\":\"ko\",\"apInfId\":\"TEST0000000000002120190521155428\",\"systemId\":\"C60REST0001\",\"notifyOption\":\"1\",\"urgYn\":\"0\",\"docSecuType\":\"0\",\"status\":\"1\",\"timeZone\":\"GMT\",\"subject\":\"�뿰怨꾪뀒�뒪�듃\",\"aplns\":[{\"actPrssDt\":\"20190521065357\",\"docOpenDt\":null,\"docArvDt\":\"20190521065357\",\"opinion\":\"상신자입니다.\",\"companyCode\":\"C60\",\"companyName\":\"�궪�꽦SDS\",\"departmentCode\":\"C60AH959\",\"departmentName\":\"Knox22\",\"epId\":\"M071015062623C988403\",\"emailAddress\":\"qtp03@stage.samsung.com\",\"name\":\"qtp03\",\"jobPositionCode\":\"B3P1\",\"jobPosition\":\"Engineer\",\"subOrgCode\":null,\"subOrgName\":null,\"seq\":\"0\",\"role\":\"0\",\"aplnStatsCode\":\"0\",\"arbPmtYn\":\"Y\",\"arbActYn\":\"N\",\"dlgAprEpid\":null,\"docDelCd\":null,\"aplnMdfyPmtYn\":\"Y\",\"prxAprYn\":\"N\",\"contentsMdfyPmtYn\":\"Y\",\"contenstsMdfyActYn\":\"N\",\"aplnMdfyActYn\":\"N\"},{\"actPrssDt\":\"\",\"docOpenDt\":null,\"docArvDt\":\"20190521065357\",\"opinion\":\"결재 대기중입니다.\",\"companyCode\":\"C60\",\"companyName\":\"�궪�꽦SDS\",\"departmentCode\":\"C60AH959\",\"departmentName\":\"Knox999\",\"epId\":\"M071015062623C981003\",\"emailAddress\":\"qtp1004@dstage.samsung.com\",\"name\":\"qtp1004\",\"jobPositionCode\":\"B3P1\",\"jobPosition\":\"Engineer\",\"subOrgCode\":null,\"subOrgName\":null,\"seq\":\"1\",\"role\":\"1\",\"aplnStatsCode\":\"0\",\"arbPmtYn\":\"Y\",\"arbActYn\":\"N\",\"dlgAprEpid\":null,\"docDelCd\":null,\"aplnMdfyPmtYn\":\"Y\",\"prxAprYn\":\"N\",\"contentsMdfyPmtYn\":\"Y\",\"contenstsMdfyActYn\":\"N\",\"aplnMdfyActYn\":\"N\"},{\"actPrssDt\":\"20190521065357\",\"docOpenDt\":null,\"docArvDt\":\"20190521065357\",\"opinion\":\"통보자입니다.\",\"companyCode\":\"C60\",\"companyName\":\"�궪�꽦SDS\",\"departmentCode\":\"C60AH959\",\"departmentName\":\"Knox55\",\"epId\":\"M071015062623C988403\",\"emailAddress\":\"qtp03@stage.samsung.com\",\"name\":\"qtp05\",\"jobPositionCode\":\"B3P1\",\"jobPosition\":\"Engineer\",\"subOrgCode\":null,\"subOrgName\":null,\"seq\":\"2\",\"role\":\"9\",\"aplnStatsCode\":\"0\",\"arbPmtYn\":\"Y\",\"arbActYn\":\"N\",\"dlgAprEpid\":null,\"docDelCd\":null,\"aplnMdfyPmtYn\":\"Y\",\"prxAprYn\":\"N\",\"contentsMdfyPmtYn\":\"Y\",\"contenstsMdfyActYn\":\"N\",\"aplnMdfyActYn\":\"N\"}],\"attachments\":[{\"attachfileName\":\"test.txt\",\"attachfileSize\":140,\"seq\":\"0\"}]}}";
				Map<String, Object> restResultMap = new Gson().fromJson(restResult, Map.class);
				if ("success".equalsIgnoreCase(String.valueOf(restResultMap.get("result")))) {
					Map<String, Object> resultData = (Map<String, Object>) restResultMap.get("data");
					List<Map<String, String>> aplns = (List<Map<String, String>>) resultData.get("aplns");
					
					final Map<String, String> defaultApproverSpec = new HashMap<String, String>();
					defaultApproverSpec.put("seq", "APP_LINE_SEQ_VIEW");
					defaultApproverSpec.put("aplnStatsCode", "APP_LINE_STATUS");
					defaultApproverSpec.put("name", "APP_USER_NAME");
					defaultApproverSpec.put("departmentName", "APP_USER_DEPT_NAME");
					defaultApproverSpec.put("actPrssDt", "ACCEPT_DATE");
					defaultApproverSpec.put("opinion", "ACCEPT_REASON");
					
					if(aplns.size() == 0) {
						setStatus(70011);
						throw new Exception("ApIn is invalid : " + restResult);
					}

					List<Map<String, String>> sepoaReturnData = new ArrayList<Map<String, String>>();
					for(Map<String, String> apln : aplns) {
						Map<String, String> convertData = new HashMap<String, String>();
						for(Entry<String, String> entry : apln.entrySet()) {
							if(!defaultApproverSpec.containsKey(entry.getKey())) continue;
							
							if("aplnStatsCode".equalsIgnoreCase(entry.getKey())) {
								if(convertData.containsKey(defaultApproverSpec.get(entry.getKey()))) continue;
								
								if("0".equals(apln.get("seq")) || Integer.parseInt(apln.get("seq")) == 0) {
									convertData.put(defaultApproverSpec.get(entry.getKey()), "L01");
								} else {
									convertData.put(defaultApproverSpec.get(entry.getKey()), reverseConvertRoleValue(apln.get("role")));
								}
							} else if("actPrssDt".equalsIgnoreCase(entry.getKey())) {
								String dateString = "";
								if(StringUtils.isNotEmpty(entry.getValue())) {
									try {
										LocalDate date = LocalDate.parse(entry.getValue(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
										LocalTime time = LocalTime.parse(entry.getValue(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).plusHours(9);
										dateString = date.toString() + " " +time.toString();
									} catch(Exception e) {
										Logger.warn.print("actPrssDt is null or invalid");
									}
								}
								if(StringUtils.isNotEmpty(entry.getValue()) && "9".equals(apln.get("role"))) {
									convertData.put("APP_LINE_STATUS", "L06");
								}
								
								convertData.put(defaultApproverSpec.get(entry.getKey()), dateString);
							} else {
								convertData.put(defaultApproverSpec.get(entry.getKey()), entry.getValue());
							}
						}
						
						sepoaReturnData.add(convertData);
					}
					
					setValue(new SepoaFormater(sepoaReturnData));
				} else {
					setStatus(70007);
					throw new Exception("succeeded in checking the approval status, but the result is not correct. SMR_NUMBER : " + headerData.get("SMR_NUMBER"));
				}
			} else {
				setStatus(70100);
				throw new Exception("Not Found ReqSeal Info for Approval Line");
			}
		} catch (Exception e) {
			if(getStatus() == 1) setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return getSepoaOut();
	}
	
	@Description("사용인감 날인요청 결재 요청")
	public SepoaOut useSealReqApproval(Map<String, Object> data) {
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
			
			//set Req Seal Doc
			if(files != null && files.size() > 0) {
				for(Map<String, String> file : files) {
					File tmpFile = new File(file.get("preview_file"));
					attachments.add(tmpFile);
				}
			}
			
			//set ExtraFiles
			if(extraFiles != null && extraFiles.size() > 0) {
				for(Map<String, String> file : extraFiles) {
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
			Logger.info.println("==================== Req Approval API Send Fail! ====================", e);
			if(getStatus() == 1) {
				setStatus(70088);
			}
			setFlag(false);
			throw new Exception(e);
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
							tmpSelectSealText.add(selectSeal.get("ITEM_NAME")+"("+selectSeal.get("MANAGE_NUMBER")+")");
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
			
			List<Map<String, String>> uploadFiles = ((SepoaReqSealManager)srm).doSelectReqSealDoc(new SingletonMap("SMR_NUMBER", reqSealInfo.get("SMR_NUMBER")));
			String uploadFileText = "";
			if(uploadFiles != null && uploadFiles.size() > 0) {
				List<String> tmpUploadFileTextList = new ArrayList<String>();
				for(Map<String, String> filesData : uploadFiles) {
					tmpUploadFileTextList.add(" - " + filesData.get("SUBJECT"));
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
			contents = contents.replace("${MANAGE_NUMBER}", useSealText);
			contents = contents.replace("${SMR_NUMBER}", reqSealInfo.get("SMR_NUMBER"));
			contents = contents.replace("${USE_NUMBER}", reqSealInfo.get("USE_NUMBER")); 
			contents = contents.replace("${REMARK}", reqSealInfo.get("REMARK"));
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
	
	private String reverseConvertAppStatus(String seclAppStatus) {
		/**
		 * SECL		SEPOA
		 * 0미결		L07대기
		 * 1결재		L02승인
		 * 2반려		L03반려
		 * 3전결		대응값 없음
		 * 5자동결재	대응값 없음
		 * 6자동반려	대응값 없음
		 */
		String result = "";
		switch(seclAppStatus) {
		case "0":
			result = "L07";
			break;
		case "1":
			result = "L02";
			break;
		case "2":
			result = "L03";
			break;
		}
		
		return result;
	}
	
	private String reverseConvertRoleValue(String seclAppMethod) {
		/* secl -> sepoa (reqapproval)
		 * ROLE(1:결재, 2:합의, 9:통보) */
		String result = "";
		switch(seclAppMethod) {
		case "1": //결재
			result = "04";
			break;
		case "2": //합의	
			result = "06";
			break;
		case "9": //통보
			result = "07";
			break;
		}
		return result;
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
		}
		
		return previewFiles;
	}
	
	
	@Description("포스코플로우 결재선, 참조선 조회 메소드")
	private List<Map<String,String>> doSelectApproverOrReferer(Map<String, String> headerData, String methodName, Boolean isReferer, Boolean notChange){
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		SepoaFormater sf = null;
		
		List<Map<String, String>> resultList = new ArrayList<>();
		
		try {
			
			sxp = new SepoaXmlParser(this, methodName);
			ssm = new SepoaSQLManager(info.getSession("ID"), this, getConnectionContext(), sxp);
			sf = new SepoaFormater(ssm.doSelect(headerData));
			
			if(sf.getRowCount() > 0) {
				List<Map<String, String>> tempResultList = sf.getListMap();
				if(!isReferer && notChange) { // 참조자가 아니고 변경불가 해야하는 경우
					for(Map<String, String> approver : tempResultList) {
						approver.put("IS_FIXED", "true");
					}
					resultList.addAll(tempResultList);
				} else {
					resultList.addAll(tempResultList);
				}
			}
			
			
		} catch (Exception e) {
			setStatus(0);
			e.printStackTrace();
			setFlag(false);
			setMessage(e.toString());
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return resultList;
	}
	
}

