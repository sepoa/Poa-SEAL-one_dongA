package sepoa.seal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang.StringUtils;

import sepoa.filemanager.FileManager.LEVEL;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.SepoaFormater;

public class SepoaExtSealManager extends SepoaSealManager {
	
	private String[] documentFileLayer = new String[] {"DOC"};
	private String[] convertLayers = {"CONVERT_SEAL_MERGE", "CONVERT_SEAL_ORG", "CONVERT_SEAL_SYM"};
	
	/*
	 * Constructors 
	 */
	public SepoaExtSealManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.docBasicInfo = new HashMap<String, String>();
	}
	
	/*
	 * Process 
	 */
	
	//반출 요청 예상서류 조회
	public List<Map<String, String>> doSelectReqExtDoc(Map<String, String> AttachDoc){
		List<Map<String, String>> resultData = this.doSelectSSEDC(AttachDoc);
		
		if(resultData == null) {
			this.errCode = 104000;
			return null;
		}
		
		return resultData;
	}
	
	//반출 요청 예상서류 인감 조회
	public List<Map<String, String>> doSelectReqSealDocSeal(Map<String, String> AttachDoc){
		List<Map<String, String>> resultData = this.doSelectSSEDC_SEAL(AttachDoc);
		
		if(resultData == null) {
			this.errCode = 104001;
			return null;
		}
		
		return resultData;
	}
	
	//반출 서류 데이터 저장
	public Map<String, String> doSaveExtDoc(Map<String, String> sealDoc) {
		Map<String, String> resultData = this.doInsertSSEDC(sealDoc);
		
		if(resultData == null) {
			this.errCode = 104002;
			return null;
		}
		
		return resultData;
	}
	
	//예상 반출 서류 데이터 수정
	public boolean doUpdateExtDoc(Map<String, String> sealDoc) {
		boolean resultData = this.doUpdateSSEDC(sealDoc);
		
		if(resultData == false) {
			this.errCode = 104003;
			return false;
		}
		
		return true;
	}
	
	//예상 반출 서류에 사용할 인감 데이터 저장 
	public Map<String, String> doSaveSealDocSeal(Map<String, String> sealDocSealData){
		Map<String, String> resultData = this.doInsertSSEDC_SEAL(sealDocSealData);
		
		if(resultData == null) {
			this.errCode = 104004;
			return null;
		}
		
		return resultData;
	}
	
	//예상 반출 서류 인감데이터 수정
	public boolean doUpdateSealDocSeal(Map<String, String> sealDocSealData){
		boolean resultData ;
		
		if("Y".equals(sealDocSealData.get("EXT_APP_REJECT"))) {//결재모듈이라면
			resultData = this.doUpdateAppSSEDC_SEAL(sealDocSealData);
		}else {
			resultData = this.doUpdateSSEDC_SEAL(sealDocSealData);
		}
		if(resultData == false) {
			this.errCode = 104005;
			return false;
		}
		
		return true;
	}
	
	//반출요청 생성
	public Map<String, String> doCreateReqExtSeal() {
		//권한 매니저 셋팅
		if(this.checkPermission) {
			if(!this.initAuthManager(
					  info.getSession("HOUSE_CODE")
					, info.getSession("COMPANY_CODE")
					, "REQ_SEAL"
					, this.docBasicInfo.get("SMR_NUMBER")
					, null)) {
				this.errCode = 100002;
				return null;
			} else {
				try {
					authObject.setOwner(
							info.getSession("COMPANY_CODE"), 
							info.getSession("ID"),
							false);
					
					initAuthManager(
							  info.getSession("HOUSE_CODE")
							, info.getSession("COMPANY_CODE")
							, "REQ_SEAL"
							, this.docBasicInfo.get("SMR_NUMBER")
							, null);
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return null;
				}
			}
		}
		
		//반출 요청 인감 구분 유무 체크
		String applyType= MapUtils.getString(this.docBasicInfo, "APPLY_TYPE", null);
		if("".equals(applyType) || applyType == null) {
			this.errCode = 104006;
			return null;
		}
		
		//반출요청 테이블 등록
		if(this.doInsertSSETL(docBasicInfo) == null) {
			this.errCode = 104007;
			return null;
		}
		
		return new HashMap<>(docBasicInfo);
	}
	
	//반출요청 제거
	public boolean doDeleteReqExt(Map<String, String> data) {
		if(!this.doDeleteSSETL(data)) {
			this.errCode = 104008;
			return false;
		}
		
		return true;
	}
	
	
	//반출요청 서류 제거
	public boolean doDeleteReqExtDoc(Map<String, String> data) {
		if(!this.doDeleteSSEDC(data)) {
			this.errCode = 104009;
			return false;
		}
		
		return true;
	}
	
	//반출요청 서류의 인장제거
	public boolean doDeleteReqExtDocSeal(Map<String, String> data) {
		if(!this.doDeleteSSEDC_SEAL(data)) {
			this.errCode = 104009;
			return false;
		}
		
		return true;
	}
	
	//반출요청 프로세스 관련 테이블 업데이트
	public boolean doUpdateReqExtDoc(Map<String, String> data) {
		if(!this.doUpdateSSETL(data)) {
			this.errCode = 104010;
			return false;
		}
		
		return true;
	}
	
	//반출요청 프로세스 관련 테이블 STATUS 업데이트
	public boolean doUpdateReqExtDocStatus(String status) {
		if(status == null || status.isEmpty()) {
			this.errCode = 104011;
			return false;
		}
		
		Map<String, String> data = new HashMap<String, String>();
		data.putAll(this.docBasicInfo);
		data.put("SMR_STATUS", status);
		
		if(!this.doUpdateReqExtDoc(data)) {
			this.errCode = 104012;
			return false;
		}
		
		if(status == "C") {//
			this.doConfirmSSETL(data);
		}else if(status == "RR") {//반입완료 요청
			this.doConfirmReqSSETL(data);
		}
		
		return true;
	}
	
	//반출요청 결재 프로세스 관련 테이블 STATUS 업데이트
	public boolean doUpdateReqExtSignStatus(String status) {
		if(status == null || status.isEmpty()) {
			this.errCode = 104011;
			return false;
		}
		
		Map<String, String> data = new HashMap<String, String>();
		data.putAll(this.docBasicInfo);
		data.put("SMR_STATUS", status);
		
		this.doConfirmSignSSETL(data);
		
		return true;
	}
	
	//반출된 인감인지 조회
	public String doSearchExtUse(Map<String, String> data){
		String resultData = null;
		if("FALSE".equals(data.get("CHK_CAL_VAL"))) {
			resultData = this.doSearchExtUseList_2(data);
		}else {
			resultData = this.doSearchExtUseList(data);
		}
		
		if(resultData == null) {
			return null;
		}
		
		return resultData;
	}
	
	//수임자인지 확인
	public Map<String, String> doSearchMAND(Map<String, String> data){
		Map<String, String> result = this.doSelectMAND(data);
		
		if(result == null) {
			return null;
		}
		
		return result;
	} 
	
	public List<Map<String, String>> doSelectRequestList(Map<String, String> map){
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		if(map == null) {
			map = this.docBasicInfo;
		}

		Map<String, String> queryMap = new HashMap<String, String>(map);
		// APPLY_TYPE 미지정 시 인감반출(U)만 조회. 비어 있으면 $INS 조건이 통째로 빠져 CEXT까지 조회됨
		if (StringUtils.isBlank(MapUtils.getString(queryMap, "APPLY_TYPE", null))) {
			queryMap.put("APPLY_TYPE", "U");
		}
		
		resultList = this.doSelectSSETL(queryMap);
		if(resultList == null) {
			this.errCode = 104013;
			return null;
		}

		List<Map<String, String>> filtered = new ArrayList<Map<String, String>>();
		for (Map<String, String> row : resultList) {
			String applyType = MapUtils.getString(row, "APPLY_TYPE", "");
			String smrNumber = MapUtils.getString(row, "SMR_NUMBER", "");
			if ("CEXT".equals(applyType) || "CERTEXT".equals(applyType)
					|| (smrNumber != null && smrNumber.startsWith("SCE"))) {
				continue;
			}
			filtered.add(row);
		}
		
		return filtered;
	}
	
	// 반출요청 테이블 리스트 조회
	public List<Map<String, String>> doSelectRequestList(){
		return this.doSelectRequestList(null);
	}
	

		
	public List<Map<String, String>> doLedgerList(Map<String, String> map){
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		if(map == null) {
			map = this.docBasicInfo;
		}
		
		resultList = this.doSelectLedgerList(map);
		if(resultList == null) {
			this.errCode = 104014;
			return null;
		}
		
		return resultList;
	}
	
	// 회수가능여부 확인
	public boolean doCheckReqExtWithdrawable(String smrNumber) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doCheckExtSealWithdrawable");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(new SingletonMap("SMR_NUMBER", smrNumber))).getListMap();
			if (list.size() == 1) {
				String status = list.get(0).get("SMR_STATUS");
				String appStage = list.get(0).get("APP_STAGE");
				if (!("W".equals(status) || "G".equals(status) || ("I".equals(status) && "1".equals(appStage)))) {
					this.errCode = 102510;	// 회수할 수 없는 상태입니다
					return false;
				}
				
			} else {
				this.errCode = 102416;
			}
			
		}catch(Exception e) {
			this.errCode = 100004;
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return true;
	}
	
	// 결재선 삭제
	public boolean doDeleteAppInfo(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(data);
	
			return true;
		}catch(Exception e) {
			this.errCode = 102501;
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	// 반출 신청시 날인예상서류 LINK생성
	public boolean doCreateExternalSealDocAccessLink(Map<String, Object> data) {
		
		Map<String, String> extSealDoc = 
				data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
		
		if(this.linkManager!=null) this.linkManager = null;
		
		if(!initLinkManager("seal_doc_ex", extSealDoc.get("SMD_NUMBER"), "0", extSealDoc)) {
			this.errCode = 100008;
			return false;
		}
		
		extSealDoc.put("DOC_TYPE", "EXT_SEAL_DOC");
		this.linkManager.setParams(extSealDoc);
		String accessLink = this.linkManager.addUser(info.getSession("COMPANY_CODE"), info.getSession("ID"), extSealDoc);
		if(StringUtils.isBlank(accessLink)) {
			this.errCode = this.linkManager.getLastErrCode();
			return false;
		}
		
		extSealDoc.put("ACCESS_LINK", accessLink);
		if(!this.doUpdateSSEDC(extSealDoc)) {
			this.errCode = 104017;
			return false;
		}
		
		return true;
	}
	
	
	//반출 날인예상서류 파일 조회
	public List<Map<String, String>> doSelectExtSealDocFile(){
		if (this.fileManager != null) {
			this.destoryFileManager();
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "EXT_SEAL_DOC"), LEVEL.FILE);
		if(documentList == null || documentList.size() < 1) {
			this.errCode = 102008;
			return null;
		}
		
		return documentList;
	}
	
	//반출 날인예상서실물날인서류 파일 업로드
	public Map<String, String> doUploadExtSealDocFile(String fileType, Map<String, String> file, Map<String, String> extraData){
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		// 있으면 삭제후 다시업로드
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "EXT_SEAL_DOC"), LEVEL.FILE);
		if(documentList != null) {
			this.fileManager.doDelete(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"));
		}
		
		List<Map<String, String>>  storedFileList = null;
		
		this.fileManager.setLayer(fileType);
		storedFileList = this.fileManager.doUploadFile(file, extraData, false);
		
		if(fileManager.getLastErrCode() != 0) {
			this.errCode = 102308;
			return null;
		}
		
		return this.fileManager.doList(storedFileList.get(0)).get(0);
	}
	
	//반출 날인예상 서류 파일 삭제
	public boolean doDeleteExtSealDocFile(Map<String, String> updateInfo) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return false;
		}
		
		try {
			this.fileManager.doDelete(updateInfo.get("FILE_ID"));
		} catch(Exception e) {
			this.errCode = 102310;
			return false;
		}
		
		return true;
	}
	
	//실물날인서류 파일 조회
	public List<Map<String, String>> doSelectExtSealRealDocFile(){
		if (this.fileManager != null) {
			this.destoryFileManager();
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "EXT_SEAL_REAL_DOC"), LEVEL.FILE);
		if(documentList == null || documentList.size() < 1) {
			this.errCode = 102008;
			return null;
		}
		
		return documentList;
	}
	
	//실물날인서류 파일 업로드
	public Map<String, String> doUploadExtSealRealDocFile(String fileType, Map<String, String> file, Map<String, String> extraData){
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		// 있으면 삭제후 다시업로드
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "EXT_SEAL_REAL_DOC"), LEVEL.FILE);
		if(documentList != null) {
			this.fileManager.doDelete(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"));
		}
		
		List<Map<String, String>>  storedFileList = null;
		
		this.fileManager.setLayer(fileType);
		storedFileList = this.fileManager.doUploadFile(file, extraData, false);
		
		if(fileManager.getLastErrCode() != 0) {
			this.errCode = 102308;
			return null;
		}
		
		return this.fileManager.doList(storedFileList.get(0)).get(0);
	}
	
	//실물날인서류 파일 삭제
	public boolean doDeleteExtSealRealDocFile(Map<String, String> updateInfo) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return false;
		}
		
		try {
			this.fileManager.doDelete(updateInfo.get("FILE_ID"));
		} catch(Exception e) {
			this.errCode = 102310;
			return false;
		}
		
		return true;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/*
	 * Table Action 
	 */
	//반출요청 관리 테이블 조회 
	private List<Map<String, String>> doSelectSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//반입 완료 요청 SSETL update
		private boolean doConfirmReqSSETL(Map<String, String> data) {
			try {
				SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
				SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setSepoaInfo(info);
				ssm.doUpdate(data);
		
				return true;
			}catch(Exception e) {
				Logger.err.println(info.getSession("ID"), this, e);
			}
			return false;
		}
	
	//반입 완료 SSETL update
	private boolean doConfirmSignSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}	
	//반입 완료 SSETL update
	private boolean doConfirmSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	//반출요청 관리 테이블 데이터 적재 
	private Map<String, String> doInsertSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSETL(data).get(0);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

		
	//반출요청 관리 테이블 데이터 삭제
	private boolean doDeleteSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//반출 예상 서류 관리 테이블 조회 
	private List<Map<String, String>> doSelectSSEDC(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//사외반출 예상서류 갯수 파악
	private Map<String, String> doGetSeq_SEDC(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			String count = ssm.doSelect(data);
			System.out.println("count : "+count);
			Map<String,String> resultMap = new HashMap<String, String>();
			resultMap.put("SMR_SEQ", count);
			
			return resultMap;
			
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//예상반출서류 관리 테이블 데이터 적재 
	private Map<String, String> doInsertSSEDC(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSEDC(data).get(0);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//예상반출서류 관리 테이블 업데이트
	private boolean doUpdateSSEDC(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//예상반출서류 관리 테이블 데이터 삭제
	private boolean doDeleteSSEDC(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	//예상반출서류 관리 테이블 데이터 삭제
	private boolean doDeleteSSEDC_SEAL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}

	
	//예상반출에 사용할 서류의 인감 관리 테이블 조회 
	private List<Map<String, String>> doSelectSSEDC_SEAL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//예상반출서류에 사용할 인감 관리 테이블 데이터 적재 
/*	private Map<String, String> doInsertSSEDC_SEAL(Map<String, String> data) {
		try {
			if("Y".equals(data.get("TEMP_FLAG"))) {
				data.put("COMPLETE_FLAG", "Y"); //임시저장이기에 Y 
			}else {
				data.put("COMPLETE_FLAG", "N"); //반출신청하였기에 N을 넣어줌 Y일 경우 반출 가능(반입완료 되었음을 표시)
			}
			
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
			
			return this.doSelectSSEDC_SEAL(data).get(0);
		}catch(Exception e) { 
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
*/	
	private Map<String, String> doInsertSSEDC_SEAL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
			
			return this.doSelectSSEDC_SEAL(data).get(0);
		}catch(Exception e) { 
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	//예상반출서류에 사용할 인감 관리 테이블 업데이트
	private boolean doUpdateSSEDC_SEAL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//결재모듈시 예상반출서류에 사용할 인감 관리 테이블 업데이트
	private boolean doUpdateAppSSEDC_SEAL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//반출된 인감인지 조회
	private String doSearchExtUseList(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			return ssm.doSelect(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//반출된 인감인지 조회
	private String doSearchExtUseList_2(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			return ssm.doSelect(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//반출요청 관리 테이블 업데이트
	private boolean doUpdateSSETL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//관리대장 리스트 조회
	private List<Map<String, String>> doSelectLedgerList(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//수임자인지 확인 
	private Map<String, String>doSelectMAND(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(data));
			if(sf.getRowCount() < 1) {
				return null;
			}
			
			Map<String,String> resultMap = new HashMap<String, String>();
			resultMap.put("GIVE_USER_ID", sf.getValue("GIVE_USER_ID",0));
			return resultMap;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//FMFI 테이블 데이터 조회
	public boolean doSearchFMFI(Map<String, String> data){
		List<Map<String, String>> resultData = this.doSearchDataFMFI(data);
		
		if(resultData == null || resultData.isEmpty()) {
			return false;
		}
		
		return true;
	}
	
	//FMDC 실제 데이터 조회 
	protected List<Map<String, String>> doSearchDataFMFI(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
}
