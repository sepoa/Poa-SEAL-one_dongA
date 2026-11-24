package sepoa.svc.sealmk2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import platform.fw.datamapper.DataMapper;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.Description;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.SepoaServiceException;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.DocumentUtil;
import sepoa.seal.SepoaReqSealManager;

@Description("전자인장MK2 - 날인요청 모듈")
public class SepoaReqSealService extends sepoa.svc.seal.SepoaReqSealService {
	
	public SepoaReqSealService(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
	}
	
	/**
	 * 해당 서비스는 SepoaAPI/service를 통해 인터페이스 받을 때 사용합니다.
	 * Multipart/form-data 형식을 지원합니다
	 * JSON+파일 인터페이스 수신시 사용하시길 바랍니다.
	 */
	@Description("외부 인터페이스 날인요청 서비스")
	@SuppressWarnings({ "unchecked" })
	
	public SepoaOut externalReqSealCreateService(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		
		try {
			ConnectionContext ctx = getConnectionContext();
			
			//서블릿에서 넘어온 데이터 객체 초기화
			Map<String, Object> businessData = (Map<String, Object>) allData.get("data");
			List<Map<String, Object>> files = (List<Map<String, Object>>) allData.get("files");
			
			//날인요청 정보 객체 초기화
			Map<String, String> reqSealInfo = (Map<String, String>) businessData.get("REQ_SEAL_INFO");
			//날인요청 문서 정보 객체 초기화
			List<Map<String, String>> documentList = (List<Map<String, String>>) businessData.get("DOCUMENT_LIST");
			
			//날인요청 매니저 생성
			SepoaReqSealManager srm = getSepoaReqSealManager(info, new Configuration(), ctx);
			
			//사용자 조회 후 변경될 인포 객체
			SepoaInfo newInfo = null;
			
			//날인요청 번호 초기 객체
			String smrNo = "";
			
			//날인요청자 ID 파싱후 유효성검사
			String houseCode = info.getSession("HOUSE_CODE");
			String companyCode = info.getSession("COMPANY_CODE");
			String userId = info.getSession("ID").toUpperCase();
			if(!srm.doCheckExistUser(houseCode, companyCode, userId)) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			} else {
				//실제 사용자로 판단되면 info객체를 현재 스레드에서 사용할 수 있게 설정
				newInfo = new SepoaInfo(houseCode, "COMPANY_CODE="+companyCode+"^@^ID="+userId+"^@^LANGUAGE=KO^@^");
				newInfo.setThreadLocal();
				this.info = newInfo;
			}
			
			if(getStatus() == 1) {
				//사용할 날인요청 정보 취득
				smrNo = MapUtils.getString(reqSealInfo, "SMR_NUMBER", "");
				if(StringUtils.isBlank(smrNo)) {
					smrNo = DocumentUtil.getDocNumber(info, "SMR").result[0];
					reqSealInfo.put("SMR_NUMBER", smrNo);
				}
				reqSealInfo.put("SOURCE_NO", smrNo);
				
				//기본데이터 적재
				reqSealInfo.put("SMR_STATUS", "T");
				reqSealInfo.put("RE_DETAIL", MapUtils.getString(reqSealInfo, "RE_FLAG", "E"));
				
				//날인요청 매니저 재생성 및 재할당
				srm = getSepoaReqSealManager(newInfo, new Configuration(), ctx);
				srm.setDocBaiscInfo(reqSealInfo);
				srm.setTempDirectory((String) allData.get(DataMapper.KEY_TEMP_DIRECTORY));
			}
			
			//날인요청 테이블 작성
			if(getStatus() == 1 && srm.doCreateReqSeal() == null) {
				int errCode = srm.getLastErrCode();
				if(errCode != 0) {
					setStatus(errCode);
				}
			}
			
			//날인 서류 파일 데이터 적재
			if(getStatus() == 1) {
				//데이터로 넘어온 파일 객체와, 업무 데이터 파일 리스트가 존재 할 경우에만 동작한다.
				if(files != null && files.size() > 0 && documentList != null && documentList.size() > 0) {
					String sealType = ("U".equals(reqSealInfo.get("APPLY_TYPE")) ? "U,E" : "C");
					//날인서류 루프
					docLoop:
					for(Map<String, String> reqSealDoc : documentList) {
						//전달 받은 파일과 문서 리스트의 제목이 일치하는 부분으로 조건 설정
						String docSubject = reqSealDoc.get("SUBJECT");
						for(Map<String, Object> file : files) {
							if(docSubject.equalsIgnoreCase((String) file.get("CLIENT_NAME"))) {
								//날인서류 관련 채번 발급
								String smdNo = DocumentUtil.getDocNumber(info, "SMD").result[0];
								reqSealDoc.put("SMR_NUMBER", smrNo);
								reqSealDoc.put("SMD_NUMBER", smdNo);
								
								//날인서류 인감 기초 데이터 세팅
								reqSealDoc.put("SEAL_TYPE", sealType);
								
								//문서 데이터에 파일 업로드를 위한 정보 세팅
								Map<String, String> uploadData = new HashMap<String, String>();
								uploadData.put("PATH", (String) file.get("PATH"));
								uploadData.put("FILE_NAME_CLIENT", (String) file.get("CLIENT_NAME"));
								uploadData.put("FILE_NAME_SERVER", (String) file.get("SERVER_NAME"));
								
								SepoaReqSealManager uploadManager = getSepoaReqSealManager(newInfo, new Configuration(), ctx);
								uploadManager.setDocBaiscInfo(reqSealDoc);
								uploadManager.setTempDirectory((String) allData.get(DataMapper.KEY_TEMP_DIRECTORY));
								if(uploadManager.doUploadDocumentFile("REQ_SEAL", uploadData, reqSealDoc) == null) {
									int errCode = uploadManager.getLastErrCode();
									if(errCode != 0) {
										setStatus(errCode);
										break docLoop;
									}
								} else {
									//날인서류 관리 테이블 적재
									srm.doSaveSealDoc(reqSealDoc);
								}
							}
						}
						
					}
				}
			}
			
			//CBO 후처리 클래스 호출
			if(getStatus() == 1 && SepoaModuleService.checkBusiness(new Configuration(), info, "SID1690952354569")) {
				Map<String, Object> paramsMap = new HashMap<String, Object>();
				paramsMap.put("MANAGER", srm);
				paramsMap.put("DATA", allData);
				
				SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.reqseal.ExternalReqSealPostProcessor", ctx, "doProcess", new Object[] {paramsMap});
				if(out == null) {
					setStatus(107001);
				} else if(out != null && out.status != 1) {
					setStatus(out.status);
				} 
			}
			
			if(getStatus() == 1) {
				Commit();
			} else {
				throw new Exception("External Req Seal Create Interface Service Fail!");
			}
			
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			if(getStatus() == 1) setStatus(0);
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
}