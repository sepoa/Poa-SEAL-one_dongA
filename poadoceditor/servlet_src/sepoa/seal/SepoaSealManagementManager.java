package sepoa.seal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;
import org.springframework.util.ObjectUtils;

import sepoa.filemanager.FileManager;
import sepoa.filemanager.FileManager.LEVEL;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaFormater;

public class SepoaSealManagementManager extends SepoaSealManager{
	/*
	 * Constructors 
	 */
	public SepoaSealManagementManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.docBasicInfo = new HashMap<String, String>();
	}
	
	
	/*
	 * Process 
	 */	
	
	//인감 신청 프로세스 호출 메소드
	public Map<String, String> doCreateSeal() {
		//권한 매니저 셋팅
		if(this.checkPermission) {
			if(!this.initAuthManager(
					  info.getSession("HOUSE_CODE")
					, info.getSession("COMPANY_CODE")
					, "SEAL"
					, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
					, MapUtils.getString(this.docBasicInfo, "DOC_NO", null))) {
				this.errCode = 100002;
				return null;
			} else {
				try {
					//해당 문서에 오너 권한 부여
					authObject.setOwner(
							info.getSession("COMPANY_CODE"), 
							info.getSession("ID"),
							false);
					
					//여기서 왜 재세팅 하는거지?
					initAuthManager( info.getSession("HOUSE_CODE")
							, info.getSession("COMPANY_CODE")
							, "SEAL"
							, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
							, MapUtils.getString(this.docBasicInfo, "DOC_NO", null));
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return null;
				}
			}
		}
		
		//SSHNE 테이블 작성
		if(this.doInsertSSHNE(docBasicInfo) == null) {
			this.errCode = 100000;
			return null;
		}
		
		return new HashMap<>(docBasicInfo);
	}


	public Map<String, String> doModifySeal() {
		//권한 매니저 셋팅
		if(this.checkPermission) {
			if(!this.initAuthManager(
					  info.getSession("HOUSE_CODE")
					, info.getSession("COMPANY_CODE")
					, "SEAL"
					, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
					, MapUtils.getString(this.docBasicInfo, "DOC_NO", null))) {
				this.errCode = 100002;
				return null;
			} else {
				try {
					//해당 문서에 오너 권한 부여
					authObject.setOwner(
							info.getSession("COMPANY_CODE"), 
							info.getSession("ID"),
							false);
					
					//여기서 왜 재세팅 하는거지?
					initAuthManager( info.getSession("HOUSE_CODE")
							, info.getSession("COMPANY_CODE")
							, "SEAL"
							, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
							, MapUtils.getString(this.docBasicInfo, "DOC_NO", null));
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return null;
				}
			}
		}
		
		//SSHUP 테이블 작성
		if(this.doInsertSSHUP(docBasicInfo) == null) {
			this.errCode = 100000;
			return null;
		}
		
		return new HashMap<>(docBasicInfo);
	}


	public Map<String, String> doLossSeal() {
		//권한 매니저 셋팅
		if(this.checkPermission) {
			if(!this.initAuthManager(
					  info.getSession("HOUSE_CODE")
					, info.getSession("COMPANY_CODE")
					, "SEAL"
					, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
					, MapUtils.getString(this.docBasicInfo, "DOC_NO", null))) {
				this.errCode = 100002;
				return null;
			} else {
				try {
					//해당 문서에 오너 권한 부여
					authObject.setOwner(
							info.getSession("COMPANY_CODE"), 
							info.getSession("ID"),
							false);
					
					//여기서 왜 재세팅 하는거지?
					initAuthManager( info.getSession("HOUSE_CODE")
							, info.getSession("COMPANY_CODE")
							, "SEAL"
							, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
							, MapUtils.getString(this.docBasicInfo, "DOC_NO", null));
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return null;
				}
			}
		}
		
		//SSHLO 테이블 작성
		if(this.doInsertSSHLO(docBasicInfo) == null) {
			this.errCode = 100000;
			return null;
		}
		
		return new HashMap<>(docBasicInfo);
	}

	public Map<String, String> doReturnSeal() {
		//권한 매니저 셋팅
		if(this.checkPermission) {
			if(!this.initAuthManager(
					  info.getSession("HOUSE_CODE")
					, info.getSession("COMPANY_CODE")
					, "SEAL"
					, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
					, MapUtils.getString(this.docBasicInfo, "DOC_NO", null))) {
				this.errCode = 100002;
				return null;
			} else {
				try {
					//해당 문서에 오너 권한 부여
					authObject.setOwner(
							info.getSession("COMPANY_CODE"), 
							info.getSession("ID"),
							false);
					
					//여기서 왜 재세팅 하는거지?
					initAuthManager( info.getSession("HOUSE_CODE")
							, info.getSession("COMPANY_CODE")
							, "SEAL"
							, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
							, MapUtils.getString(this.docBasicInfo, "DOC_NO", null));
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return null;
				}
			}
		}
		
		//SSHRE 테이블 작성
		if(this.doInsertSSHRE(docBasicInfo) == null) {
			this.errCode = 100000;
			return null;
		}
		
		return new HashMap<>(docBasicInfo);
	}

	//인감 등록 메소드
	public Map<String, String> doRegSeal() {
		//권한 매니저 셋팅
		if(this.checkPermission) {
			if(!this.initAuthManager( 
					  info.getSession("HOUSE_CODE")
					, info.getSession("COMPANY_CODE")
					, "SEAL"
					, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
					, null)) {
				this.errCode = 100002;
				return null;
			} else {
				try {
					authObject.setOwner(
							info.getSession("COMPANY_CODE"), 
							info.getSession("ID"),
							false);
					
					initAuthManager(info.getSession("HOUSE_CODE")
							, info.getSession("COMPANY_CODE")
							, "SEAL"
							, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
							, null);
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return null;
				}
			}
		}
		
		//SSEAL 테이블 작성
		if(this.doInsertSSEAL(docBasicInfo) == null) {
			this.errCode = 102407;
			return null;
		}
		
		return new HashMap<>(docBasicInfo);
	}

	//인감 이미지 업로드
	public Map<String, String> doUploadSealImage(String fileType, Map<String, String> file, Map<String, String> extraData) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("MANAGE_DOC_NO"))) {
			this.errCode = 100001;
			return null;
		}
		
		List<Map<String, String>>  storedFileList = null;
		
		this.fileManager.setLayer(fileType);
		storedFileList = this.fileManager.doUploadFile(file, extraData, false);
		
		if(fileManager.getLastErrCode() != 0) {
			this.errCode = 100401;
			return null;
		}
		
		return this.fileManager.doList(storedFileList.get(0)).get(0);
	}
	
	//인감 이미지 조회(레이어 미지정)
	public Map<String, String> doSelectSealImage() {
		String layer = "SEAL_IMAGE";
		return doSelectSealImage(layer);
	}
	
	//인감 이미지 조회(레이어 지정)
	public Map<String, String> doSelectSealImage(String layer) {
		//파일매니저 초기화
		if(this.fileManager != null) {
			this.destoryFileManager();
		}
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("MANAGE_DOC_NO"))) {
			this.errCode = 100001;
			return null;
		}

		Map<String, String> appendData = new HashMap<>();
		appendData.put("LAYER", layer);
		
		//인감 이미지 업로드 파일리스트 조회
		List<Map<String, String>> sealList = this.fileManager.doList(appendData, LEVEL.FILE);
		
		if(sealList == null) {
			this.errCode = 102313;
			return null;
		}
		
		//패스 매니저 생성(인감이미지 업로드 콜백할 때 조회를 위한 용도 일반적으로는 사용할 일 없음)
		PathManager pm = new PathManager(info, config);
		pm.setLayer(layer);
		
		Map<String, String> sealData = new HashMap<>();
		sealData.putAll(sealList.get(0));
		
		//ActionControllor에서 파일을 내보낼 때 사용할려고 설정함.
		sealData.put("FILE_PATH", pm.getLayerPath() + sealList.get(0).get("FILE_NAME_SERVER"));
		
		return sealData;
	}
	
	//인감 이미지 업데이트
	public boolean doDeleteSealImage(Map<String, String> updateInfo) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("MANAGE_DOC_NO"))) {
			this.errCode = 100001;
			return false;
		}
		
		try {
			this.fileManager.doDelete(updateInfo.get("FILE_ID"));
		} catch(Exception e) {
			this.errCode = 102312;
			return false;
		}
		
		return true;
	}
	
	//인감관리 처리대상 리스트 조회
	public List<Map<String, String>> doQuerySealPrcList(boolean descSort){
		List<Map<String, String>> resultList = this.doSelectSealPrcTarget();
		if(resultList == null) {
			this.errCode = 102309;
			return null;
		}
		
		if(!descSort) {
			resultList = resultList.stream().sorted((o1, o2) -> o1.get("DOC_NO").toString().compareTo(o2.get("DOC_NO").toString())).collect(Collectors.toList());
		} else {
			resultList = resultList.stream().sorted((o1, o2) -> o2.get("DOC_NO").toString().compareTo(o1.get("DOC_NO").toString())).collect(Collectors.toList());
		}
		
		return resultList;
	}
	
	//인감관리 신청현황 리스트 조회
	public List<Map<String, String>> doQuerySealReqList(boolean descSort){
		List<Map<String, String>> resultList = this.doSelectSealReqTarget();
		if(resultList == null) {
			this.errCode = 103324;
			return null;
		}
		
		if(!descSort) {
			resultList = resultList.stream().sorted((o1, o2) -> o1.get("DOC_NO").toString().compareTo(o2.get("DOC_NO").toString())).collect(Collectors.toList());
		} else {
			resultList = resultList.stream().sorted((o1, o2) -> o2.get("DOC_NO").toString().compareTo(o1.get("DOC_NO").toString())).collect(Collectors.toList());
		}
		
		return resultList;
	}
	
	//인감 등록 신청 문서 조회
	public Map<String, String> doQueryRegistDoc(){
		Map<String, String> resultMap = this.doSelectSSHNE(this.docBasicInfo);
		if(resultMap == null) {
			this.errCode = 102406;
			return null;
		}
		
		return resultMap;
	}
	
	//인감 변경 신청 문서 조회 
	public Map<String, String> doQueryModifyDoc(){
		Map<String, String> resultMap = this.doSelectSSHUP(this.docBasicInfo);
		if(resultMap == null) {
			this.errCode = 102415;
			return null;
		}
		
		return resultMap;
	}

	//인감 분실 신청 문서 조회
	public Map<String, String> doQueryLossDoc(){
		Map<String, String> resultMap = this.doSelectSSHLO(this.docBasicInfo);
		if(resultMap == null) {
			this.errCode = 102414;
			return null;
		}
		
		return resultMap;
	}	

	//인감 반납 신청 문서 조회
	public Map<String, String> doQueryReturnDoc(){
		Map<String, String> resultMap = this.doSelectSSHRE(this.docBasicInfo);
		if(resultMap == null) {
			this.errCode = 103328;
			return null;
		}
		
		return resultMap;
	}	
	
	//문서(NE, LO, UP, RE) 논리 삭제
	public Boolean doDeleteDocument(String targetTable){
		boolean updateResult = false;
		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.putAll(this.docBasicInfo);
		paramsMap.put("DEL_FLAG", "Y");
		
		switch(targetTable.toUpperCase()) {
			case "SSHNE" :
				updateResult = this.doUpdateSSHNE(paramsMap);
				break;
			case "SSHUP" :
				updateResult = this.doUpdateSSHUP(paramsMap);
				break;
			case "SSHLO" :
				updateResult = this.doUpdateSSHLO(paramsMap);
				break;
			case "SSHRE" :
				updateResult = this.doUpdateSSHRE(paramsMap);
				break;
		}
		
		if(!updateResult) {
			this.errCode = 102401;
			return false;
		}
		return true;
	}
		
	//SUSE, SUDGL join 데이터 조회
	public List<Map<String, String>> doSelectSealUseInfo(String manageNo, String companyCode) {
		try {
			Map<String, String> paramsMap = new HashMap<>();
			paramsMap.put("MANAGE_NUMBER", manageNo);
			if(!ObjectUtils.isEmpty(companyCode)) {
				paramsMap.put("COMPANY_CODE", companyCode);
			}
			
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectSealUseInfo");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(paramsMap)).getListMap();
			
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	public List<Map<String, String>> doSelectSealUseInfo(String manageNo){
		return this.doSelectSealUseInfo(manageNo, null);
	}

	//인감-용도 데이터 적재
	public Map<String, String> doInsertSealUse(String useNo){
		Map<String, String> resultMap = this.doInsertSSUSE(useNo);
		if(resultMap == null) {
			this.errCode = 102408;
			return null;
		}
		return resultMap;
	}
	
	//인감-용도 SUSE 믈리 삭제
	public Map<String, String> doDeleteSealUse(){
		Boolean flag = this.doDeleteSSUSE(this.docBasicInfo);
		if(!flag) {
			this.errCode = 102410;
			return null;
		}
		return new HashMap<>(this.docBasicInfo);
	}
	
	//인감별 결재자 모듈 - 데이터 수정 프로세스
	public Boolean doChangeAppProcess(Map<String, String> data){
		List<Map<String, String>> selectResult = this.doSelectSAPUD(data);
		
		if(selectResult.size() > 0) {//CHANGE_FLAG, DEL_FALG 모두 Y인 경우가 있다면 삭제
			Map<String,String> delMap = new HashMap<String, String>();
			delMap.put("KEY_NUMBER", data.get("KEY_NUMBER"));
			delMap.put("CHANGE_FLAG", "Y");
			delMap.put("DEL_FLAG", "Y");
			if(!doDeleteSealApprover(delMap)) {
				return false;
			}
		}
		//CHANGE_FLAG, DEL_FALG 모두 N인 경우 모두 Y로 변경
		data.put("CATEGORY", "N");
		if(!doUpadteSealApprover(data)) {
			return false;
		}
		
		//CHANGE_FLAG가 Y인 경우 N으로 변경
		data.put("CATEGORY", "Y");
		if(!doUpadteSealApprover(data)) {
			return false;
		}
		return true;
	}
	
	//인감별 결재자 모듈 - 데이터 조회
	public List<Map<String, String>> doSelectSealAppData(Map<String, String> data){
		List<Map<String, String>> selectResult = this.doSelectSAPUD(data);
		if(selectResult == null) {
			this.errCode = 102499;
			return null;
		}
		
		return selectResult;
	}
	
	//인감별 결재자 모듈 - 데이터 적재
	public Boolean doInsertSealApprover(Map<String, String> data){
		if(doSelectSealAppData(data).size() > 0) {//날인용도 변경신청시 필요
			return true;
		}else {
			Boolean resultMap = this.doInsertSAPUD(data);
			if(!resultMap) {
				this.errCode = 102500;
				return false;
			}
			return true;			
		}
		
	}
	
	//인감별 결재자 모듈 - 데이터 수정
	public Boolean doUpadteSealApprover(Map<String, String> data){
		if("N".equals(data.get("CATEGORY"))) {
			Boolean resultMap = this.doUpdateSAPUD_N(data);
			if(!resultMap) {
				this.errCode = 102502;
				return false;
			}			
		}else if("Y".equals(data.get("CATEGORY"))) {
			Boolean resultMap = this.doUpdateSAPUD_Y(data);
			if(!resultMap) {
				this.errCode = 102502;
				return false;
			}		
		}
		return true;
	}
	
	//인감별 결재자 모듈 - 데이터 삭제
	public Boolean doDeleteSealApprover(Map<String, String> data){
		Boolean deleteMap = this.doDeleteSAPUD(data);
		if(!deleteMap) {
			this.errCode = 102502;
			return false;
		}
		return true;
	}
	
	//인감관리 프로세스 관련 테이블 업데이트
	public boolean doUpateSeal(String table, Map<String, String> data) {
		//권한 매니저 셋팅
		if(this.checkPermission) {
			if(!this.initAuthManager(
					  info.getSession("HOUSE_CODE")
					, info.getSession("COMPANY_CODE")
					, "SEAL"
					, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
					, MapUtils.getString(this.docBasicInfo, "DOC_NO", null))) {
				this.errCode = 100002;
				return false;
			} else {
				try {
					//해당 문서에 오너 권한 부여
					authObject.setOwner(
							info.getSession("COMPANY_CODE"), 
							info.getSession("ID"),
							false);
					
					//여기서 왜 재세팅 하는거지?
					initAuthManager( info.getSession("HOUSE_CODE")
							, info.getSession("COMPANY_CODE")
							, "SEAL"
							, MapUtils.getString(this.docBasicInfo, "MANAGE_NUMBER", null)
							, MapUtils.getString(this.docBasicInfo, "DOC_NO", null));
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return false;
				}
		
			}
		}
		
		boolean updateResult = false;
		switch(table.toUpperCase()) {
			case "SSHNE" :
				updateResult = doUpdateSSHNE(data);
				break;
			case "SSEAL" :
				updateResult = doUpdateSSEAL(data);
				break;
			case "SSHUP" :
				updateResult = doUpdateSSHUP(data);
				break;
			case "SSHLO" :
				updateResult = doUpdateSSHLO(data);
				break;
			case "SSHRE" :
				updateResult = doUpdateSSHRE(data);
				break;
		}
		
		if(!updateResult) {
			this.errCode = 100302;
			return false;
		}
		
		return true;
	}
	
	public boolean doUpdateSealInfo(Map<String, String> data) {
		boolean updateResult = false;
		updateResult = doUpdateSSEAL(data);
		
		if(!updateResult) {
			this.errCode = 100302;
			return false;
		}
		
		return true;
	}

	//인감관리 프로세스 관련 테이블 STATUS 업데이트
	public boolean doUpdateDocStatus(String table, String status) {
		if(status == null || status.isEmpty()) {
			this.errCode = 100301;
			return false;
		}
		
		Map<String, String> data = new HashMap<String, String>();
		data.put("MANAGE_NUMBER", this.docBasicInfo.get("MANAGE_NUMBER"));
		data.put("DOC_NO", this.docBasicInfo.get("DOC_NO"));
		data.put("REASON", this.docBasicInfo.get("ACCEPT_REASON"));
		data.put("STATUS", status);
		
		boolean updateResult = doUpateSeal(table, data);
		
		if(!updateResult) {
			this.errCode = 100302;
			return false;
		}
		this.docBasicInfo.put("STATUS", status);
		return true;
	}
	
	//인감 STATUS 업데이트
	public boolean doUpdateSealStatus(String status) {
		if(status == null || status.isEmpty()) {
			this.errCode = 100301;
			return false;
		}
		
		Map<String, String> data = new HashMap<String, String>();
		data.putAll(this.docBasicInfo);
		data.put("SEAL_STATUS", status);
		
		boolean updateResult = doUpateSeal("SSEAL", data);
		
		if(!updateResult) {
			this.errCode = 100302;
			return false;
		}
		this.docBasicInfo.put("SEAL_STATUS", status);
		return true;
	}	
	
	//인감 용도 조회 params=String
	public Map<String, String> doSelectSealUse(String manageNumber){
		if(manageNumber == null || manageNumber.isEmpty()) {
			this.errCode = 102317;
			return null;
		}
		
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("MANAGE_NUMBER", manageNumber);
		return this.doSelectSealUse(queryMap);
	}
	
	//인감 용도 조회 params=Map
	public Map<String, String> doSelectSealUse(Map<String, String> sealData){
		//인감 데이터 조회
		Map<String, String> selectResult = this.doSelectSSUSE(sealData);
		if(selectResult == null) {
			this.errCode = 102318;
			return null;
		}
		
		return new HashMap<>(selectResult);
	}
	
	//인감 데이터 조회 params=String
	public Map<String, String> doSelectSealData(String manageNumber){
		if(manageNumber == null || manageNumber.isEmpty()) {
			this.errCode = 102317;
			return null;
		}
		
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("MANAGE_NUMBER", manageNumber);
		return this.doSelectSealData(queryMap);
	}
	
	//인감 데이터 조회 params=Map
	public Map<String, String> doSelectSealData(Map<String, String> sealData){
		//인감 데이터 조회
		Map<String, String> selectResult = this.doSelectSSEAL(sealData);
		if(selectResult == null) {
			this.errCode = 102318;
			return null;
		}
		
		return new HashMap<>(selectResult);
	}
	
	//날인요청 시 조건에 맞는 인감 리스트 조회
	public List<Map<String, String>> doSelectSealListRequiredUse(Map<String, String> params){
		//인감조회에 필요한 용도 파싱 후 유효성 검사
		String sealUseType = MapUtils.getString(params, "SEAL_USE_TYPE", null);
		if(sealUseType == null || "".equals(sealUseType.trim())) {
			this.errCode = 103322;
			return null;
		}
		
		List<Map<String, String>> resultList = this.doSelectSealMappedUse(params);
		if(resultList == null) {
			this.errCode = 102318;
			return null;
		}
		
		return resultList;
	}
	
	//인감 리스트 조회
	public List<Map<String, String>> doSelectSealList(Map<String, String> params){
		//인감 리스트 조회
		List<Map<String, String>> resultList = this.doSelectSealMappedPim(params); 
		if(resultList == null) {
			this.errCode = 103323;
			return null;
		}
		
		return resultList;
	}
	
	//인감책임자 조회 
	public List<Map<String, String>> doSearchCtrlCode(Map<String, String> data){
		//인감 리스트 조회
		List<Map<String, String>> resultList = this.doSelectCtrlCode(data); 
		if(resultList == null) {
			this.errCode = 103323;
			return null;
		}
		
		return resultList;
	}
	
	//인감 이미지 미리보기 로드
	public Map<String, String> doSelectSealPreviewImage(String itemCode) {
		//아이템 코드 검사
		if(itemCode == null) {
			this.errCode = 102405;
			return null;
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(itemCode)) {
			this.errCode = 100001;
			return null;
		}
		
		//인감 이미지 업로드 파일리스트 조회
		List<Map<String, String>> sealList = this.fileManager.doList(LEVEL.FILE);
		if(sealList == null) {
			this.errCode = 102313;
			return null;
		}
		
		//패스 매니저 생성(인감이미지 업로드 콜백할 때 조회를 위한 용도 일반적으로는 사용할 일 없음)
		PathManager pm = new PathManager(info, config);
		pm.setLayer("SEAL_PRE");
		
		Map<String, String> sealData = new HashMap<>();
		sealData.putAll(sealList.get(0));
		
		//ActionControllor에서 파일을 내보낼 때 사용할려고 설정함.
		sealData.put("FILE_PATH", pm.getLayerPath() + sealList.get(0).get("FILE_NAME_SERVER"));
		
		return sealData;
	}

	//인감 리스트 조회
	public List<Map<String, String>> doQuerySEALList(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectSSEAL");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list == null || list.size() < 1) {
				this.errCode = 102318;
				return null;
			}else {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//SDUGL 데이터 조회
	public Map<String, String> doSelectSUDGL(String useNo, String companyCode){
		try {
			Map<String, String> paramMap = new HashMap<String, String>();
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			paramMap.put("USE_NUMBER", useNo);
			if(!ObjectUtils.isEmpty(companyCode)) {
				paramMap.put("COMPANY_CODE", companyCode);
			}
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(paramMap)).getListMap();
			if(resultList.size() > 0) {
				return resultList.get(0);
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	public Map<String, String> doSelectSUDGL(String useNo){
		return doSelectSUDGL(useNo, null);
	}

	//인감 이미지 복제
		
	public Boolean doCopySealImg(String originalDocNo, String fileId, String desDocNo) {
		return doCopySealImg(originalDocNo, fileId, desDocNo, null);
	}		
	
	public Boolean doCopySealImg(String originalDocNo, String fileId, String desDocNo, String lay) {
		//파일 매니저 셋팅
		if(!this.initFileManager(originalDocNo)) {
			this.errCode = 100001;
			return false;
		}
		
		FileManager fm = new FileManager(info, config, ctx);
		if(fm.setDocNumber(desDocNo, true) == null) {
			this.errCode = 100001;
			return false;
		}
		
		fm.doCopy(originalDocNo, fileId, lay);
		int err = fm.getLastErrCode();
		if(err != 0) {
			this.errCode = err;
			return false;
		}
		
		return true;
	}

	//Seal Image -> String 처리
	public String sealImageToString(String type, String sealPath) throws Exception{
		String out = new String();
		switch (type.toUpperCase()) {
			case "BASE64":
				out = this.imageToBase64(sealPath);
				break;
			case "BINARYSTRING":
				out = this.fileToBinaryString(sealPath);
				break;
		}
		
		return out;
	}
	//인감 리스트 조회
	public Boolean doCheckMultiCorpSeal(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectSSEAL");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list.size() > 0) {
				this.errCode = 103329;
				return false;
			}else {
				return true;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//로직 진행중인건 조회
	public Boolean doProcessList(Map<String, String> data){
		List<Map<String, String>> resultList = new ArrayList<Map<String,String>>();
		if("EXT".equals(data.get("processType"))) {
			resultList = this.doProcessExtList(data);
			if(resultList == null) {
				return true;
			}else {
				for(Map<String, String> listData : resultList) {
					listData.put("processType", "EXT");
					listData.put("SEAL_MG_DEPT", data.get("SEAL_MG_DEPT"));
					listData.put("SEAL_MG_USER_ID", data.get("SEAL_MG_USER_ID"));
					
					if(!doUpdateTable(listData)) {
						return false;
					}
				}
			}
		}else if("SEAL".equals(data.get("processType"))) {
			resultList = this.doProcessUseList(data);
			if(resultList == null) {
				return true;
			}else {
				for(Map<String, String> listData : resultList) {
					listData.put("processType", "SEAL");
					listData.put("SEAL_MG_DEPT", data.get("SEAL_MG_DEPT"));
					listData.put("SEAL_MG_USER_ID", data.get("SEAL_MG_USER_ID"));
					
					if(!doUpdateTable(listData)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public Boolean doUpdateTable(Map<String, String> data){
		
		if("EXT".equals(data.get("processType"))) {
			if(!this.doUpdateSSEDC(data)) {
				this.errCode = 104017;
				return false;
			}
			if(!this.doUpdateSSEDC_SEAL(data)) {
				this.errCode = 104018;
				return false;
			}
		}else if("SEAL".equals(data.get("processType"))) {
			if(!this.doUpdateSSIPD_SEAL(data)) {
				this.errCode = 104020;
				return false;
			}
		}
		
		return true;
	}
	/*
	 * Table Action 
	 */
			
		
	//인감 테이블 데이터 조회 
	private Map<String, String> doSelectSSEAL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list.size() > 0) {
				return list.get(0);
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//인감 테이블 데이터 적재
	private Map<String, String> doInsertSSEAL(Map<String, String> data) {
		try {
			String sealID = UUID.randomUUID().toString().replaceAll("-", "")+System.nanoTime();
			data.put("SEAL_ID", sealID);
			
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSEAL(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//인감 테이블 데이터 업데이트
	private boolean doUpdateSSEAL(Map<String, String> data) {
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

	//인감 등록 신청 테이블 데이터 조회
	private Map<String, String> doSelectSSHNE(Map<String, String> data) {
		try {
			
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			Map<String, String> resultMap = new HashMap<>();
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				resultMap = resultList.get(0);
				return resultMap;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감 등록 신청 테이블 데이터 적재 
	private Map<String, String> doInsertSSHNE(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSHNE(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감 등록 신청 테이블 데이터 업데이트
	private boolean doUpdateSSHNE(Map<String, String> data) {
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
	
	
	//인감 변경 신청 테이블 데이터 조회
	private Map<String, String> doSelectSSHUP(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			Map<String, String> resultMap = new HashMap<>();
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				resultMap = resultList.get(0);
				return resultMap;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감 변경 신청 테이블 데이터 적재
	private Map<String, String> doInsertSSHUP(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSHUP(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감 변경 신청 테이블 데이터 업데이트
	private boolean doUpdateSSHUP(Map<String, String> data) {
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

	//인감 분실 신청 테이블 데이터 조회
	private Map<String, String> doSelectSSHLO(Map<String, String> data) {
		try {
			
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			Map<String, String> resultMap = new HashMap<>();
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				resultMap = resultList.get(0);
				return resultMap;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감 분실 신청 테이블 데이터 적재
	private Map<String, String> doInsertSSHLO(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSHLO(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감 분실 신청 테이블 데이터 업데이트
	private boolean doUpdateSSHLO(Map<String, String> data) {
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

	//인감 반납 신청 테이블 데이터 조회
	private Map<String, String> doSelectSSHRE(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			Map<String, String> resultMap = new HashMap<>();
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				resultMap = resultList.get(0);
				return resultMap;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감 반납 신청 테이블 데이터 적재
	private Map<String, String> doInsertSSHRE(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSHRE(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감 반납 신청 테이블 데이터 업데이트
	private boolean doUpdateSSHRE(Map<String, String> data) {
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

	//인감-용도 테이블 데이터 조회
	private Map<String, String> doSelectSSUSE(Map<String, String> data){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				return resultList.get(0);
			}
			
			return null;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감-용도 테이블 데이터 적재
	private Map<String, String> doInsertSSUSE(String useNo){
		try {
			Map<String, String> paramMap = new HashMap<>();
			paramMap.putAll(this.docBasicInfo);
			paramMap.put("USE_NUMBER", useNo);
			
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(paramMap);
			
			List<Map<String, String>> resultList = new SepoaFormater(this.doSelectSSUSE(paramMap)).getListMap();
			if(resultList.size() > 0) {
				return resultList.get(0);
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}

	//인감-용도 테이블 데이터 물리 삭제
	private Boolean doDeleteSSUSE(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(data);
			
			List<Map<String, String>> result = this.doSelectSealUseInfo(this.docBasicInfo.get("MANAGE_NUMBER"));
			
			//데이터가 있으면  false
			if(result == null) {
				return true;
			}
			return false;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}

	//인감관리 프로세스 처리대상 조회 
	private List<Map<String, String>> doSelectSealPrcTarget() {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			List<Map<String, String>> resultList = new ArrayList<Map<String,String>>();

			//SSHNE
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSHNE");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> SHNEList = new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap();
			resultList.addAll(SHNEList);
			
			//SSHRE
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSHRE");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> SHREList = new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap();
			resultList.addAll(SHREList);
			
			//SSHLO
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSHLO");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> SHLOList = new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap();
			resultList.addAll(SHLOList);			
			
			//SSHUP
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSHUP");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> SHUPList = new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap();
			resultList.addAll(SHUPList);				
					
			return resultList;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//인감관리 프로세스 신청현황 조회
	private List<Map<String, String>> doSelectSealReqTarget() {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			List<Map<String, String>> resultList = new ArrayList<Map<String,String>>();

			//SSHNE
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSHNE");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> SHNEList = new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap();
			resultList.addAll(SHNEList);
			
			//SSHRE
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSHRE");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> SHREList = new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap();
			resultList.addAll(SHREList);
			
			//SSHLO
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSHLO");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> SHLOList = new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap();
			resultList.addAll(SHLOList);			
			
			//SSHUP
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSHUP");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> SHUPList = new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap();
			resultList.addAll(SHUPList);				
					
			return resultList;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//인감관리 신청 개수 조회
	public int doCountSealMng(String manageType) {
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			
			sxp = new SepoaXmlParser(this.getClass(), "doSelectSSH"+manageType);
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			return new SepoaFormater(ssm.doSelect(this.docBasicInfo)).getListMap().size();
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		this.errCode = 103324;
		return -1;
	}
	
	//SSEAL, SSUSE 조인 후 인감 리스트 조회
	private List<Map<String, String>> doSelectSealMappedPim(Map<String, String> data){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				return resultList;
			}
			
			return null;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//SSEAL, SSUSE 조인 후 인감 리스트 조회
	private List<Map<String, String>> doSelectSealMappedUse(Map<String, String> data){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				return resultList;
			}
			
			return null;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}		
	
	//인장날인내역 조회
	public List<Map<String, String>> doSealHistoryList(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSealHistoryList");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if (list == null || list.size() < 1) {
				this.errCode = 102318;
				return null;
			}else {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	//인감책임자 조회
	private List<Map<String, String>> doSelectCtrlCode(Map<String, String> data){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				return resultList;
			}
			
			return null;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//인감 테이블 데이터 적재
	private boolean doInsertSAPUD(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doInsert(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	private List<Map<String, String>> doSelectSAPUD(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			
			if (list.size() >= 0) {
				return list;
			}
			
			return null;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//인감 테이블 데이터 업데이트
	private boolean doUpdateSAPUD_N(Map<String, String> data) {
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
	
	//인감 테이블 데이터 업데이트
	private boolean doUpdateSAPUD_Y(Map<String, String> data) {
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
	
	//인감 테이블 데이터 삭제
	private boolean doDeleteSAPUD(Map<String, String> data) {
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
	
	//반출로직 진행중인 데이터 조회 
	private List<Map<String, String>> doProcessExtList(Map<String, String> data) {
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
	//인감담당자 로직 진행중인 데이터 조회 
	private List<Map<String, String>> doProcessUseList(Map<String, String> data) {
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
	//SSEDC 반출로직의 담당자 정보 변경
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
	//SSEDC_SEAL 반출로직의 담당자 정보 변경
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
	//SSIPD_SEAL 날인신청 로직의 담당자 정보 변경
	private boolean doUpdateSSIPD_SEAL(Map<String, String> data) {
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
}
