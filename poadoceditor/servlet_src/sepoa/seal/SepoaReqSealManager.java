package sepoa.seal;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.tika.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import sepoa.contract.util.SepoaContractImageProcessor;
import sepoa.filemanager.FileManager.LEVEL;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.filemanager.utils.storage.PathManager;
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
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaImage;
import sepoa.fw.util.converter.SepoaConverter;
import sepoa.fw.util.renderer.image.SepoaImageCapture;
import sepoa.fw.util.renderer.image.SepoaImageCapture.Data;
import sepoa.fw.util.renderer.image.SepoaImageCapture.Engine;
import sepoa.fw.util.ux.sUtils;
import sepoa.seal.util.MatrixToImageConfig;
import sepoa.seal.util.MatrixToImageWriter;
import sepoa.seal.util.PDFUtils;
import sepoa.svl.util.SepoaMultipart;

public class SepoaReqSealManager extends SepoaSealManager {
	
	protected String[] documentFileLayer = new String[] {"DOC"};
	protected String[] convertLayers = {"CONVERT_SEAL_MERGE", "CONVERT_SEAL_ORG", "CONVERT_SEAL_SYM"};
	protected String[] convertCertLayers = {"CONVERT_CERT_MERGE", "CONVERT_CERT_ORG", "CONVERT_CERT_SYM"};
	
	/*
	 * Constructors 
	 */
	public SepoaReqSealManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.docBasicInfo = new HashMap<String, String>();
	}
	
	/*
	 * Process 
	 */
	
	//교부서류 헤더 데이터 조회
	public List<Map<String, String>> doSelectCorpAttachDocHeader(Map<String, String> corpAttachDoc){
		List<Map<String, String>> resultData = this.doSelectSSRAD(corpAttachDoc);
		
		if(resultData == null) {
			this.errCode = 102320;
			return null;
		}
		
		return resultData;
	}
	
	//교부 서류 헤더 데이터 저장
	public boolean doSaveCorpAttachDocHeader(Map<String, String> cropAttchDoc) {
		// 교부서류 헤더에 해당 데이터 있는지 확인
		boolean resultData = false;
		if(this.doSelectSSRAD(cropAttchDoc) != null) {
			resultData = this.doUpdateSSRAD(cropAttchDoc);
		} else {
			resultData = this.doInsertSSRAD(cropAttchDoc);
		}
		
		if(resultData == false) {
			this.errCode = 102002;
			return false;
		}
		return true;
	}
	
	//교부 서류 헤더 데이터 삭제
	public boolean doDeleteCorpAttachDocHeader(Map<String, String> cropAttchDoc) {
		boolean status = this.doDeleteSSRAD(cropAttchDoc);
		
		if(status == false) {
			this.errCode = 102009;
			return false;
		}
		
		return true;
	}
	
	//날인신청 요청 서류 조회
	public List<Map<String, String>> doSelectReqSealDoc(Map<String, String> corpAttachDoc){
		List<Map<String, String>> resultData = this.doSelectSSIPD(corpAttachDoc);
		
		if(resultData == null) {
			this.errCode = 102416;
			return null;
		}
		
		return resultData;
	}
	
	// 동아제약 메일 발송 작업을 위한 날인신청 요청 서류 조회(유저 이름과 유저 이메일 찾는 용도)
	public List<Map<String, String>> doSelectMailReqSealDoc(Map<String, String> corpAttachDoc){
		List<Map<String, String>> resultData = this.doSelectForMailSSIPD(corpAttachDoc);
		
		if(resultData == null) {
			this.errCode = 10240709;
			return null;
		}
		
		return resultData;
	}
	
	//날인신청 요청서류 인감 조회
	public List<Map<String, String>> doSelectReqSealDocSeal(Map<String, String> corpAttachDoc){
		List<Map<String, String>> resultData = this.doSelectSSIPD_SEAL(corpAttachDoc);
		
		if(resultData == null) {
			this.errCode = 102318;
			return null;
		}
		
		return resultData;
	}
	
	//교부요청 서류 데이터 조회
	public List<Map<String, String>> doSelectCorpAttachDoc(Map<String, String> corpAttachDoc){
		List<Map<String, String>> resultData = this.doSelectSSISD(corpAttachDoc);
		
		if(resultData == null) {
			this.errCode = 102320;
			return null;
		}
		
		return resultData;
	}
	
	//FMFI 테이블 데이터 조회
	public boolean doSearchFMFI(Map<String, String> data){
		List<Map<String, String>> resultData = this.doSearchDataFMFI(data);
		
		if(resultData == null || resultData.isEmpty()) {
			return false;
		}
		
		return true;
	}
	
	//인장데이터 조회 
	public Map<String, String> doCheckInputSealData(Map<String, String> data){
		List<Map<String, String>> resultData = this.doSearchDataFMFI(data);
		Map<String, String> resultMap = new HashMap<String, String>();
		Map<String, String> resultListMap = new HashMap<String, String>();
		
		if(resultData.size() == 0) {
			this.errCode = 104016;
			return null;
		}else {
			resultMap.put("FILE_ID", resultData.get(0).get("FILE_ID"));
			List<Map<String, String>> resultList  = this.doSelectSFMMT(resultMap);
			
			if(resultList.size() == 0) {
				this.errCode = 104015;
				return null;
			}
			resultListMap.put("VALUE", resultList.get(0).get("VALUE"));
		}		
		return resultListMap;
	}
	
	public boolean doCreateExternalReqSealDocAccessLink(Map<String, String> reqSealDoc) {
		if(this.linkManager!=null) this.linkManager = null;
		
		if(!initLinkManager("seal_doc_ex", reqSealDoc.get("SMD_NUMBER"), "0", reqSealDoc)) {
			this.errCode = 100008;
			return false;
		}
		
		reqSealDoc.put("DOC_TYPE", "REQ_SEAL");
		this.linkManager.setParams(reqSealDoc);
		String accessLink = this.linkManager.addUser(info.getSession("COMPANY_CODE"), info.getSession("ID"), reqSealDoc);
		if(StringUtils.isBlank(accessLink)) {
			this.errCode = this.linkManager.getLastErrCode();
			return false;
		}
		
		reqSealDoc.put("ACCESS_LINK", accessLink);
		if(!this.doUpdateSSIPD(reqSealDoc)) {
			this.errCode = 102417;
			return false;
		}
		
		return true;
	}
	
	
	public boolean doCreateExternalAttDocAccessLink(Map<String, String> attDocInfo) {
		if(this.linkManager!=null) this.linkManager = null;
		
		if(!initLinkManager("seal_doc_ex", attDocInfo.get("SD_NUMBER"), "0", attDocInfo)) {
			this.errCode = 100008;
			return false;
		}
		
		attDocInfo.put("DOC_TYPE", "ATTACH_DOC_TEMPLATE");
		this.linkManager.setParams(attDocInfo);
		String accessLink = this.linkManager.addUser(info.getSession("COMPANY_CODE"), info.getSession("ID"), attDocInfo);
		
		if(StringUtils.isBlank(accessLink)) {
			this.errCode = this.linkManager.getLastErrCode();
			return false;
		}
		
		attDocInfo.put("ACCESS_LINK",accessLink);
		if(!this.doUpdateSSISD(attDocInfo)) {
			this.errCode = 102417;
			return false;
		}
		
		return true;
	}
	
	//교부 요청 서류 데이터 저장
	public Map<String, String> doSaveCorpAttachDoc(Map<String, String> corpAttchDoc) throws NoSuchAlgorithmException {
		List<Map<String, String>> resultList = this.doSelectFileData(corpAttchDoc); 
		if(resultList == null) {
			this.errCode = 102008;
			return null;
		}else {
			/*if(StringUtils.isBlank(resultList.get(0).get("DOC_SEQ"))) {
				String doc_no = DocumentUtil.getDocNumber(info, "SFILE").result[0];
				corpAttchDoc.put("ATTACH_NO", doc_no);
				corpAttchDoc.put("DOC_SEQ", resultList.get(0).get("DOC_SEQ"));
				Map<String, String> resultData = this.doInsertSSISD(corpAttchDoc);
				
				return resultData;
			}else {*/
				String doc_no = DocumentUtil.getDocNumber(info, "SFILE").result[0];
				for(Map<String, String> map : resultList) {
					map.put("DOC_NO", doc_no);//새로운 파일첨부 채번
					
					String type = map.get("TYPE");
					String desFileName = map.get("DES_FILE_NAME");


					String serverName = info.getSession("COMPANY_CODE") + "_" + String.valueOf(System.currentTimeMillis()) + SecureRandom.getInstance("SHA1PRNG").nextInt(1000);
	                
	            	String typePath = SepoaMultipart.getTypePath(type, "upload");
					String desFilePath = SepoaMultipart.generateFilePath(info, type, serverName);
					String newDesFileName = desFilePath + "/" + serverName;
					
					File dir = new File(typePath + "/" + desFilePath);
	                if (!dir.exists()) {
	                    dir.mkdirs();
	                }
	                
	                File file1 = new File(typePath + "/" + desFileName);
	                File file2 = new File(typePath + "/" + newDesFileName);
	                
	                String text = corpAttchDoc.get("SD_NUMBER");
	                if(!map.get("DOC_SEQ").isEmpty()) {
	                	try {
	                		if(SepoaModuleService.checkBusiness(config, info, "SID1666682622483")) {
	                			PDFUtils.writeTextToPDF(file1, file2, text, 11f, PDFUtils.RIGHT_TOP);
	                			map.put("DES_FILE_NAME", newDesFileName);
	                		}else {	                			
	                			FileUtils.copyFile(file1, file2);
	                		}
		               		this.doInsertSFILE(map);
		                } catch (Exception e ) {
		               	 	// e.printStackTrace();
		               	 	throw new RuntimeException("File copy failed. [" + file1 + "] to [" + file2 + "]");
		                }
	                }
				}
				corpAttchDoc.put("ATTACH_NO", doc_no);
				Map<String, String> resultData = this.doInsertSSISD(corpAttchDoc);
	
				if(resultData == null) {
					this.errCode = 102002;
					return null;
				}
				return resultData;
			//}
		}		
	}
	
	// 교부요청서류 데이터 수정
	public boolean doUpdateCorpAttachDoc(Map<String, String> cropAttchDoc) throws NoSuchAlgorithmException {
		List<Map<String, String>> resultList = this.doSelectFileData(cropAttchDoc); 
		if(resultList == null) {
			this.errCode = 102008;
			return false;
		}else {
			String doc_no = DocumentUtil.getDocNumber(info, "SFILE").result[0];
			for(Map<String, String> map : resultList) {
				map.put("DOC_NO", doc_no);//새로운 파일첨부 채번
				
				String type = map.get("TYPE");
				String desFileName = map.get("DES_FILE_NAME");
				String serverName = info.getSession("COMPANY_CODE") + "_" + String.valueOf(System.currentTimeMillis()) + SecureRandom.getInstance("SHA1PRNG").nextInt(1000);
                
            	String typePath = SepoaMultipart.getTypePath(type, "upload");
				String desFilePath = SepoaMultipart.generateFilePath(info, type, serverName);
				String newDesFileName = desFilePath + "/" + serverName;
				
				File dir = new File(typePath + "/" + desFilePath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                 
                File file1 = new File(typePath + "/" + desFileName);
                File file2 = new File(typePath + "/" + newDesFileName);
                 
                String text = cropAttchDoc.get("SD_NUMBER"); 
                
                if(!map.get("DOC_SEQ").isEmpty()) {
	                try {
	                	if(SepoaModuleService.checkBusiness(config, info, "SID1666682622483")) {
                			PDFUtils.writeTextToPDF(file1, file2, text, 11f, PDFUtils.RIGHT_TOP);
                			map.put("DES_FILE_NAME", newDesFileName);
                		}else {	                			
                			FileUtils.copyFile(file1, file2);
                		}
	               		this.doInsertSFILE(map);				
	                } catch (Exception e ) {
	               	 // e.printStackTrace();
	               	 throw new RuntimeException("File copy failed. [" + file1 + "] to [" + file2 + "]");
	                }
                }
			}
			cropAttchDoc.put("ATTACH_NO", doc_no);
			boolean resultData = this.doUpdateSSISD(cropAttchDoc);
			
			if(resultData == false) {
				this.errCode = 102417;
				return false;
			}
		}
		return true;
	}
	
	//날인 요청 부가 데이터 조회
	public Map<String, String> doSelectExtraInfo(Map<String, String> data) {
		Map<String, String> resultData  = this.doSelectSSIADExtraInfo(data);
		if(resultData == null) {
			return null;
		}
		
		return resultData;
	}
	
	//날인 요청 부가 데이터 저장
	public Map<String, String> doSaveExtraInfo(Map<String, Object> data) {
		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("SMR_NUMBER", MapUtils.getString(data, "SMR_NUMBER", null));
		paramsMap.put("DATA", new Gson().toJson(data));
		
		if(!this.doDeleteSSIADExtraInfo(paramsMap)) {
			this.errCode = 10331;
			return null;
		}
		
		Map<String, String> resultData = this.doSaveSSIADExtraInfo(paramsMap);
		if(resultData == null) {
			this.errCode = 10330; //날인 요청 부가 데이터 저장에 실패하였습니다.
			return null;
		}
		
		return resultData;
	}
	
	private boolean doDeleteSSIADExtraInfo(Map<String, String> data) {
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
	
	private Map<String, String> doSaveSSIADExtraInfo(Map<String, String> data){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSIADExtraInfo(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return null;
	}
	
	private Map<String, String> doSelectSSIADExtraInfo(Map<String, String> data) {
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
	
	//날인 서류 데이터 저장
	public Map<String, String> doSaveSealDoc(Map<String, String> sealDoc) {
		Map<String, String> resultData = this.doInsertSSIPD(sealDoc);
		
		if(resultData == null) {
			this.errCode = 102003;
			return null;
		}
		
		return resultData;
	}
	
	// 날인 서류 데이터 수정
	public boolean doUpdateSealDoc(Map<String, String> sealDoc) {
		boolean resultData = this.doUpdateSSIPD(sealDoc);
		
		if(resultData == false) {
			this.errCode = 103325;
			return false;
		}
		
		return true;
	}
	
	//날인서류에 사용할 인감 데이터 저장 
	public Map<String, String> doSaveSealDocSeal(Map<String, String> sealDocSealData){
		if(!"Y".equals(sealDocSealData.get("ACCEPT_FLAG"))) {
			sealDocSealData.put("ACCEPT_FLAG", "N");
		}
		if(!"Y".equals(sealDocSealData.get("DEC_FLAG"))) {
			sealDocSealData.put("DEC_FLAG", "N");
		}
		Map<String, String> resultData = this.doInsertSSIPD_SEAL(sealDocSealData);
		
		if(resultData == null) {
			this.errCode = 102005;
			return null;
		}
		
		return resultData;
	}
	
	// 날인서류에 사용할 인감 데이터 추가하기
	public boolean doAppendSealFixHtml(Map<String, String> appendData) {
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return false;
		}
		
		Map<String, String> fileData = this.fileManager.doList().stream().filter(e -> e.get("DOC_SEQ").equals(appendData.get("PAGE"))).collect(Collectors.toList()).get(0);
//		fileData.put("PART_ID", StringUtils.leftPad(appendData.get("PAGE"), 3, "0"));
		fileData.put("FIX_HTML", appendData.get("FIX_HTML"));
		this.fileManager.doModifyMeta(fileData);
		int errCode = this.fileManager.getLastErrCode();
		if(errCode != 0) {
			this.errCode = 103325;
			return false;
		}
		
		return true;
	}
	
	// 날인서류 인감데이터 수정
	public boolean doUpdateSealDocSeal(Map<String, String> sealDocSealData){
		boolean resultData = this.doUpdateSSIPD_SEAL(sealDocSealData);
		
		if(resultData == false) {
			this.errCode = 102005;
			return false;
		}
		
		return true;
	}
	
	//날인요청 생성
	public Map<String, String> doCreateReqSeal() {
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
		
		//날인요청 인감 구분 유무 체크
		String applyType= MapUtils.getString(this.docBasicInfo, "APPLY_TYPE", null);
		if("".equals(applyType) || applyType == null) {
			this.errCode = 102315;
			return null;
		}
		
 		//날인요청 용도 체크
//		if(SepoaModuleService.checkBusiness(config, info, "US1637133464814")) {
//			String reqSealUseType = MapUtils.getString(this.docBasicInfo, "REQ_SEAL_USE_TYPE", null);
//			if("".equals(reqSealUseType) || reqSealUseType == null) {
//				this.errCode = 102316;
//				return null;
//			}
//		}
		
		
		//날인요청 테이블 등록
		if(this.doInsertSSIAD(docBasicInfo) == null) {
			this.errCode = 102000;
			return null;
		}
		
		return new HashMap<>(docBasicInfo);
	}
	
	//날인요청 제거
	public boolean doDeleteReqSeal(Map<String, String> data) {
		if(!this.doDeleteSSIAD(data)) {
			this.errCode = 102418;
			return false;
		}
		
		return true;
	}
	
	//날인요청 서류 제거
	public boolean doDeleteReqSealDoc(Map<String, String> data) {
		if(!this.doDeleteSSIPD(data)) {
			this.errCode = 102310;
			return false;
		}
		
		return true;
	}
	
	//날인요청 서류 인장 제거
	public boolean doDeleteReqSealDocSeal(Map<String, String> data) {
		if(!this.doDeleteSSIPD_SEAL(data)) {
			this.errCode = 102311;
			return false;
		}
		
		return true;
	}
	
	//날인요청 서류 인장 제거 (인감번호 없이)
	public boolean doDeleteReqSealDocSealWithoutManageNumber(Map<String, String> data) {
		if(!this.doDeleteSSIPD_SEAL_NO_MANAGE_NUMBER(data)) {
			this.errCode = 102311;
			return false;
		}
		
		return true;
	}
	
	//날인요청 교부서류 제거
	public boolean doDeleteReqSealAttachDoc(Map<String, String> data) {
		if(!this.doDeleteSSISD(data)) {
			this.errCode = 102310;
			return false;
		}
		
		return true;
	}
	
	//날인요청 프로세스 관련 테이블 업데이트
	public boolean doUpdateReqSealDoc(Map<String, String> data) {
		if(!this.doUpdateSSIAD(data)) {
			this.errCode = 102009;
			return false;
		}
		
		return true;
	}
	
	//날인요청 프로세스 관련 테이블 STATUS 업데이트
	public boolean doUpdateReqSealDocStatus(String status) {
		if(status == null || status.isEmpty()) {
			this.errCode = 100301;
			return false;
		}
		
		Map<String, String> data = new HashMap<String, String>();
		data.putAll(this.docBasicInfo);
		data.put("SMR_STATUS", status);
		
		if(!this.doUpdateReqSealDoc(data)) {
			this.errCode = 100302;
			return false;
		}
		
		return true;
	}
	
	// 날인요청 후처리 대기열 추가
	public boolean doAddPostProcessLine(Map<String, String> data) {
		if(!this.doAddSAPPP(data)) {
			this.errCode = 102520;	// 날인요청 후처리 대기열 추가에 실패했습니다
			return false;
		}
		return true;
	}
	
	public List<Map<String, String>> doSelectRequestList(Map<String, String> map){
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		if(map == null) {
			map = this.docBasicInfo;
		}
		resultList = this.doSelectSSIAD(map);

		
		if(resultList == null) {
			this.errCode = 102007;
			return null;
		}
		
		return resultList;
	}
	
	// 그룹웨어 테이블 리스트 조회
	public List<Map<String, String>> doSelectRequestList(){
		return this.doSelectRequestList(null);
	}
	
	public List<Map<String, String>> doGroupWareRequestList(Map<String, String> map){
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		if(map == null) {
			map = this.docBasicInfo;
		}
		
		resultList = this.doSelectSSIAD_GW(map);
		if(resultList == null) {
			this.errCode = 102007;
			return null;
		}
		
		return resultList;
	}
	
	// 그룹웨어 테이블 리스트 조회
	public List<Map<String, String>> doGroupWareRequestList(){
		return this.doGroupWareRequestList(null);
	}
	
	// 인장 아이템 조회 
	public String doSelectSignItem(Map<String, String> data){
		String sealListStr = this.doSelectSUPIM(data); 
		if(StringUtils.isBlank(sealListStr)) {
			this.errCode = 102006;
			return null;
		}
		
		return sealListStr;
	}
	
	public List<Map<String, String>> doListImage(String layer){
		if("".equals(layer) || layer == null) {
			layer = this.docBasicInfo.get("LAYER");
		}
		List<Map<String, String>> result = new LinkedList<Map<String, String>>();
		this.initFileManager(this.docBasicInfo.get("KEY"));
		
		List<Map<String, String>> layerImage = this.fileManager.doList(new SingletonMap<String, String>("LAYER", layer), LEVEL.PARTS);
		if(layerImage != null) {
			result.addAll(layerImage);
		}
		if (result.size() == 0) {
			this.errCode = 102321;
			return null;
		} 
		return result;
	}
	
	// 이미지 리스트 조회
	public List<Map<String, String>> doListImage(){
		return this.doListImage(null);
	}
	
	// 이미지 조회
	public Map<String, String> doImage(String fileID, String partID, boolean thumb, boolean noImageFlag){
		if(!this.initFileManager(this.docBasicInfo.get("KEY"))) {
			this.errCode = 100001;
			return null;
		}
		
		if(fileID.isEmpty() || partID.isEmpty()) {
			this.errCode = 100004;
			return null;
		}else {
			Map<String, String> result = this.fileManager.doImage(fileID, partID, thumb, noImageFlag);
			if(result == null) {
				Logger.err.println(info.getSession("ID"), this, "not found image : "+this.fileManager.getLastErrCode());
				this.errCode = 102321;
				return null;
			}
			return result;
		}
	}
	//그룹웨어 날인신청 요청 서류 조회
	public List<Map<String, String>> getAttachInfo(Map<String, String> corpAttachDoc){
		List<Map<String, String>> resultData = this.doSelectSSIPD_GW(corpAttachDoc);
		
		if(resultData == null) {
			this.errCode = 102416;
			return null;
		}
		
		return resultData;
	}
	//날인서류 파일 업로드
	public Map<String, String> doUploadDocumentFile(String fileType, Map<String, String> file, Map<String, String> extraData) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		List<Map<String, String>>  storedFileList = null;
		
		this.fileManager.setLayer(fileType);
		storedFileList = this.fileManager.doUploadFile(file, extraData, false);
		
		if(fileManager.getLastErrCode() != 0) {
			Logger.err.println("File Manager Upload Fail! Status : " + fileManager.getLastErrCode());
			this.errCode = 102308;
			return null;
		}
		
		return this.fileManager.doList(storedFileList.get(0)).get(0);
	}
	
	// 날인서류 파일 수정
	public boolean doUpdateEditorHTML(List<Map<String, String>> editorHtmls) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("KEY"))) {
			this.errCode = 100001;
			return false;
		}
		
		for(Map<String, String> page : editorHtmls) {
			this.fileManager.doModifyMeta(page);
			int errCode = this.fileManager.getLastErrCode();
			if(errCode != 0) {
				this.errCode = 103325;
				return false;
			}
		}
		return true;
	}
	
	//전자계약 과거이력 파일 조회
	public List<Map<String, String>> doSelectContractHistoryFile(){
		if (this.fileManager != null) {
			this.destoryFileManager();
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("CONT_NO") + "_" + this.docBasicInfo.get("CONT_GL_SEQ"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONTRACT_HISTORY"), LEVEL.FILE);
		if(documentList == null || documentList.size() < 1) {
			this.errCode = 102008;
			return null;
		}
		
		return documentList;
	}
	
	//전자계약 과거이력 파일 업로드
	public Map<String, String> doUploadContractHistoryFile(String fileType, Map<String, String> file, Map<String, String> extraData){
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("CONT_NO") + "_" + this.docBasicInfo.get("CONT_GL_SEQ"))) {
			this.errCode = 100001;
			return null;
		}
		
		// 있으면 삭제후 다시업로드
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONTRACT_HISTORY"), LEVEL.FILE);
		if(documentList != null) {
			this.fileManager.doDelete(this.docBasicInfo.get("CONT_NO") + "_" + this.docBasicInfo.get("CONT_GL_SEQ"));
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
	
	//계약 과거이력 파일 삭제
	public boolean doDeleteContractHistoryFile(Map<String, String> updateInfo) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("CONT_NO") + "_" + this.docBasicInfo.get("CONT_GL_SEQ"))) {
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
	public List<Map<String, String>> doSelectReqSealRealDocFile(){
		if (this.fileManager != null) {
			this.destoryFileManager();
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL_REAL_DOC"), LEVEL.FILE);
		if(documentList == null || documentList.size() < 1) {
			this.errCode = 102008;
			return null;
		}
		
		return documentList;
	}
	
	//실물날인서류 파일 업로드
	public Map<String, String> doUploadReqSealRealDocFile(String fileType, Map<String, String> file, Map<String, String> extraData){
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER") + "_" + this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		// 있으면 삭제후 다시업로드
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL_REAL_DOC"), LEVEL.FILE);
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
	public boolean doDeleteReqSealRealDocFile(Map<String, String> updateInfo) {
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//날인 과거이력 파일 조회
	public List<Map<String, String>> doSelectReqSealHistoryFile(){
		if (this.fileManager != null) {
			this.destoryFileManager();
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL_HISTORY"), LEVEL.FILE);
		if(documentList == null || documentList.size() < 1) {
			this.errCode = 102008;
			return null;
		}
		
		return documentList;
	}
	
	//날인 과거이력 파일 업로드
	public Map<String, String> doUploadReqSealHistoryFile(String fileType, Map<String, String> file, Map<String, String> extraData){
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		// 있으면 삭제후 다시업로드
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL_HISTORY"), LEVEL.FILE);
		if(documentList != null) {
			this.fileManager.doDelete(this.docBasicInfo.get("SMR_NUMBER"));
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
	
	//날인 과거이력 파일 삭제
	public boolean doDeleteReqSealHistoryFile(Map<String, String> updateInfo) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER"))) {
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
	
	//교부서류 관리 템플릿 모드 파일 조회
	public List<Map<String, String>> doSelectAttachDocTemplateFile(){
		if (this.fileManager != null) {
			this.destoryFileManager();
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMI_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "ATTACH_DOC_TEMPLATE"), LEVEL.FILE);
		if(documentList == null || documentList.size() < 1) {
			this.errCode = 102008;
			return null;
		}
		
		return documentList;
	}
	
	//교부서류 관리 템플릿 모드 파일 삭제
	public boolean doDeleteAttachDocTemplateFile(Map<String, String> updateInfo) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMI_NUMBER"))) {
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
	
	//교부서류 관리 템플릿 모드 파일 업로드
	public Map<String, String> doUploadAttachDocTemplateFile(String fileType, Map<String, String> file, Map<String, String> extraData){
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMI_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		// 있으면 삭제후 다시업로드
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "ATTACH_DOC_TEMPLATE"), LEVEL.FILE);
		if(documentList != null) {
			this.fileManager.doDelete(this.docBasicInfo.get("SMI_NUMBER"));
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
	
	//날인서류 파일 조회
	public Map<String, String> doSelectDocumentFile() {
		if (this.fileManager != null) {
			this.destoryFileManager();
		}
		
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL"), LEVEL.PARTS);
		if(documentList.size() < 1) {
			this.errCode = 103326;
			return null;
		}
		
		//패스 매니저 생성
		PathManager pm = new PathManager(info, config);
		pm.setLayer("REQ_SEAL");
		
		Map<String, String> sealData = new HashMap<>();
		sealData.putAll(documentList.get(0));
		
		//ActionControllor에서 파일을 내보낼 때 사용할려고 설정함.
		sealData.put("FILE_PATH", pm.getLayerPath() + documentList.get(0).get("FILE_NAME_SERVER"));
		
		return sealData;
	}
		
	//날인서류 파일 업데이트
	public boolean doDeleteDocumentFile(Map<String, String> updateInfo) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SMD_NUMBER"))) {
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
	
	//교부서류 파일 조회
	public Map<String, String> doSelectAttachDocumentFile() {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		//날인서류 업로드 파일리스트 조회
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "SEAL_ATC"), LEVEL.PARTS);
		if(documentList == null) {
			this.errCode = 103326;
			return null;
		}
		
		//패스 매니저 생성
		PathManager pm = new PathManager(info, config);
		pm.setLayer("REQ_SEAL");
		
		Map<String, String> sealData = new HashMap<>();
		sealData.putAll(documentList.get(0));
		
		//ActionControllor에서 파일을 내보낼 때 사용할려고 설정함.
		sealData.put("FILE_PATH", pm.getLayerPath() + documentList.get(0).get("FILE_NAME_SERVER"));
		
		return sealData;
	}
	
	// 교부서류 파일 업로드
	public Map<String, String> doUploadAttachDocumentFile(String fileType, Map<String, String> file, Map<String, String> extraData) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SD_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		// 있으면 삭제후 다시업로드
		List<Map<String, String>> documentList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "SEAL_ATC"), LEVEL.PARTS);
		if(documentList != null) {
			this.fileManager.doDelete(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SD_NUMBER"));
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
	
	//교부서류 파일 삭제
	public boolean doDeleteAttachDocumentFile(Map<String, String> updateInfo) {
		//파일 매니저 셋팅
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER")+"_"+this.docBasicInfo.get("SD_NUMBER"))) {
			this.errCode = 100001;
			return false;
		}
		
		try {
			this.fileManager.doDelete(updateInfo.get("FILE_ID"));
		} catch(Exception e) {
			this.errCode = 102009;
			return false;
		}
		
		return true;
	}
	
	//PDF 열람 후 반환
	public byte[] doLoadPDF() {
		if(!this.initFileManager(this.docBasicInfo.get("SMR_NUMBER"))) {
			this.errCode = 100001;
			return null;
		}
		
		List<Map<String, String>> dataList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "AUDIT_TRAIL"), LEVEL.FILE);
		if(dataList == null) {
			this.errCode = 103343;
			return null;
		}
		
		Map<String, String> auditTrailData = dataList.get(0);
		PathManager pm = new PathManager(info, config);
		pm.setLayer("AUDIT_TRAIL");
		File auditTrailFile = new File(pm.getLayerPath() + auditTrailData.get("FILE_NAME_SERVER"));
		
		if(!auditTrailFile.exists()) {
			this.errCode = 103343;
			return null;
		}
		
		InputStream in = null;
		byte[] bArr = null;
		
		try {
			in = CipherManager.getCipher().getInputStream(auditTrailFile, false);
			bArr = IOUtils.toByteArray(in);
		} catch (Exception e) {
			this.errCode = 103344;
			return null;
		}
		
		return bArr;
	}
	
	// 날인요청 다이렉트 접근 링크 발급 
	public String doCreateReqSealAccessLink(String linkType, String smrNo, Map<String, String> linkParams, String linkUserId, String open) {
		// open :  PUBLIC: 전체공개 / ALL : 사용자 전부 / PRIVATE: 개인
		if(this.linkFlag && !initLinkManager(linkType, smrNo, null, linkParams)) {
			this.errCode = 100008;
			return null;
		}
		
		String publicLinkCode = this.linkManager.getLink();
		if(StringUtils.isBlank(publicLinkCode)) {
			this.errCode = this.linkManager.getLastErrCode();
			return null;
		}
		
		String userParam = null;
		if ("PUBLIC".equals(open)) {
			userParam = "TEMP_USER";
		} else if ("ALL".equals(open)) {
			userParam = "*";
		} else {
			userParam = (linkUserId == null) ? info.getSession("ID") : linkUserId;
		}
		String privateLinkCode = this.linkManager.addUser(info.getSession("COMPANY_CODE"), userParam, linkParams);
		if(StringUtils.isBlank(privateLinkCode)) {
			this.errCode = this.linkManager.getLastErrCode();
			return null;
		}
		
		if (!"PRIVATE".equals(open)) {
			return publicLinkCode;
		} else {
			return privateLinkCode;
		}
	}
	
	// 날인대상문서 다이렉트 접근 링크 발급 
	public String doCreateReqSealDocAccessLink(String linkType, String smdNo, Map<String, String> linkParams, String linkUserId, String open) {
		// open :  PUBLIC: 전체공개 / ALL : 사용자 전부 / PRIVATE: 개인
		if(this.linkFlag && !initLinkManager(linkType, smdNo, null, linkParams)) {
			this.errCode = 100008;
			return null;
		}
		
		String publicLinkCode = this.linkManager.getLink();
		if(StringUtils.isBlank(publicLinkCode)) {
			this.errCode = this.linkManager.getLastErrCode();
			return null;
		}
		
		String userParam = null;
		if ("PUBLIC".equals(open)) {
			userParam = "TEMP_USER";
		} else if ("ALL".equals(open)) {
			userParam = "*";
		} else {
			userParam = (linkUserId == null) ? info.getSession("ID") : linkUserId;
		}
		String privateLinkCode = this.linkManager.addUser(info.getSession("COMPANY_CODE"), userParam, linkParams);
		if(StringUtils.isBlank(privateLinkCode)) {
			this.errCode = this.linkManager.getLastErrCode();
			return null;
		}
		
		if ("ALL".equals(open)) {
			return publicLinkCode;
		} else {
			return privateLinkCode;
		}
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
	
	//날인신청 통계 조회 
	public List<Map<String, String>> doSealStatisticsList(Map<String, String> data){
		//인감 리스트 조회
		List<Map<String, String>> resultList = this.SealStatisticsList(data); 
		if(resultList == null) {
			this.errCode = 105010;
			return null;
		}
		
		return resultList;
	}
	
	//날짜별 통계 조회 
	public List<Map<String, String>> doByDateStatisticsList(Map<String, String> data){
		List<Map<String, String>> resultList = this.ByDateStatisticsList(data); 
		if(resultList == null) {
			this.errCode = 105010;
			return null;
		}
		
		return resultList;
	}
	
	public boolean doCheckDecide(Map<String, String> data) {
		return this.doCheckDecideReject(data);
	}
	
	public boolean doConvertCertDocument() {
		return this.doConvertCertDocument(null, null);
	}
	
	public boolean doConvertCertDocument(String smrNo, String smdNo) {
		return true;
	}
	
	//날인요청 인장 이미지 치환 (특정 문서)
	public boolean doConvertReqSealDocument(String smrNo, String smdNo) {
		String houseCode = info.getSession("HOUSE_CODE");
		List<Map<String, String>> replaceDocList = new ArrayList<Map<String, String>>();
		if ("".equals(smrNo) || smrNo == null || "".equals(smdNo) || smdNo == null) {
			List<Map<String, String>> upDocList = this.doSelectSSIPD(this.docBasicInfo);
			if (upDocList == null) {
				this.errCode = 103327;
				return false;
			}

			smrNo = this.docBasicInfo.get("SMR_NUMBER");
			if("R".equals(this.docBasicInfo.get("RE_FLAG"))) {	 // 실물이면 컨버팅하지 않음
				return true;
			}else {
				for (Map<String, String> upDoc : upDocList) {
					Map<String, String> doc = new HashMap<String, String>();
					doc.put("SMR_NUMBER", smrNo);
					String smdNumber = upDoc.get("SMD_NUMBER");
					if (smdNumber.startsWith("NO_DOC")) continue;	// 날인대상문서를 사용하지 않음
					doc.put("SMD_NUMBER", smdNumber);
	
					replaceDocList.add(upDoc);
				}
			}
		} else {
			Map<String, String> doc = new HashMap<String, String>();
			doc.put("SMR_NUMBER", smrNo);
			doc.put("SMD_NUMBER", smdNo);

			replaceDocList.add(doc);
		}

		if (replaceDocList.size() == 0) {
			return true;
			//this.errCode = 103327;
			//return false;
		}

		for (Map<String, String> replaceDoc : replaceDocList) {
			if (this.fileManager != null) {
				this.destoryFileManager();
			}

			if (!this.initFileManager(replaceDoc.get("SMR_NUMBER") + "_" + replaceDoc.get("SMD_NUMBER"))) {
				this.errCode = 100001;
				return false;
			}

			// 발행번호 저장
			String publishNo = UUID.randomUUID().toString()+System.nanoTime();
			replaceDoc.put("PUBLISH_NO", publishNo);
			if (doUpdatePublishNo(replaceDoc) == 0) {
				this.errCode = 100000;
				return false;
			}
			
			String publishText = getThisSessionText(houseCode, "FOOTER_TEXT","PUB_NO", null) + " : " + publishNo;
			String publishNoHtml = "<div class=\"footer\" style=\"position:absolute;bottom:25px;width:100%;font-size:13px;z-index:100;display:flex;justify-content:left;text-align:left;\"><div style=\"padding:10px;color:#999;\">"+publishText+"</div></div>";
			
			
			List<Map<String, String>> layerImages = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL"), LEVEL.PARTS);
			if (layerImages == null) {
				this.errCode = 103333;
				return false;
			}

			Map<String, String> optionMap = parseOptionReqSeal(layerImages);
			Map<String, String> customCssMap = new HashMap<String, String>();
			customCssMap.put("CONVERT_SEAL_ORG", "<style>.contract-item-box-source-symmetry .contract-item-prop-symmetry_page_copy-ON{display: none;}.contract-item-box-source-copy .contract-item-prop-symmetry_page_copy-ON{display:block;}.contract-item-box-source-org .contract-item-prop-symmetry_page_copy-ON{display:block;}</style>");
			customCssMap.put("CONVERT_SEAL_SYM", "<style>.contract-item-box-source-symmetry .contract-item-prop-symmetry_page_copy-ON{display: block;}.contract-item-box-source-copy .contract-item-prop-symmetry_page_copy-ON{display:none;}.contract-item-box-source-org .contract-item-prop-symmetry_page_copy-ON{display:none;}</style>");
			customCssMap.put("CONVERT_SEAL_MERGE", "");
			
			//간인 여부 확인
			String[] prcLayers = convertLayers;
			if (!"ON".equalsIgnoreCase(optionMap.get("PAGE_COPY"))) {
				String[] notSymLayer = { "CONVERT_SEAL_ORG" };
				prcLayers = notSymLayer;
			}

			//레이어 단위로 루프 시작
			for (String layer : prcLayers) {
				List<File> mergeHtmlList = new ArrayList<File>();
				String clientFileName = "";
				
				for (Map<String, String> image : layerImages) {
					SepoaContractImageProcessor scip = new SepoaContractImageProcessor(info, config, ctx);
					scip.setTempDirectory(this.tempStorage);
					
					String fixHtml = image.get("FIX_HTML");
					clientFileName = image.get("FILE_NAME_CLIENT");
					
					//footer 추가 부분 - fixHtml이 없어도 모든 장에 들어가야 하므로 맨 앞으로 뺌
					String legalWatermark = MapUtils.getString(this.docBasicInfo, "LEGAL_WATERMARK", "").toUpperCase();
					if(StringUtils.isNotBlank(legalWatermark)) {
						String legalfooterText = "";
						if("K".equals(legalWatermark)) {
							legalfooterText = getThisSessionText(houseCode, "FOOTER_TEXT","LEGAL_TEXT_KO", null);
						}else if("E".equals(legalWatermark)) {
							legalfooterText = getThisSessionText(houseCode, "FOOTER_TEXT","LEGAL_TEXT_EN", null);
						}
						
						String footerContents = "<div class=\"footer\" style=\"position:absolute;bottom:0%;width:100%;font-size:12px;z-index:100;display:flex;justify-content:#ALIGN#;text-align:#ALIGN#;\"><div style=\"padding:10px;\">#TEXT#</div></div>";
						footerContents = footerContents.replace("#TEXT#", legalfooterText);
						footerContents = footerContents.replace("#ALIGN#", CommonUtil.getConfig("sepoa.footer.legal.align", "left").toLowerCase());
						
						fixHtml += footerContents;
					}
					// 발행번호
					fixHtml += publishNoHtml;
					
					//여기까지  footer 추가 부분
					
					if (StringUtils.isNotBlank(fixHtml)) {
						fixHtml += customCssMap.get(layer);
						String repSealHtml="";
						
						try {
							repSealHtml = parseAttachSeal(fixHtml, replaceDoc,optionMap);
							if(repSealHtml == null) {
								this.errCode = this.getLastErrCode();
								return false;
							}
						} catch (Exception e) {
							this.errCode = this.getLastErrCode();
							return false;
						}
						
						//fixHtml += repSealHtml;						
						image.put("FIX_HTML", repSealHtml);
					}
					
					String newImagePath = scip.mergeHtml(image);
					if(StringUtils.isBlank(newImagePath)) {
						this.errCode = 103334;
						return false;
					}
					
					mergeHtmlList.add(new File(newImagePath));
				}
				
				//파츠 뭉탱이 모아서 PDF화
				File toPdfTempPath = new File(this.tempStorage + "/" + System.nanoTime() + "_" + System.currentTimeMillis() + ".pdf");
				SepoaImage.createPDF(mergeHtmlList, toPdfTempPath);
				
				//sfmfi에 업로드 진행
				Map<String, String> uploadFileData = new HashMap<String, String>();
				uploadFileData.put("PATH", toPdfTempPath.getAbsolutePath());
				
				String fileExt = clientFileName.substring(clientFileName.lastIndexOf(".") + 1).toLowerCase();
				String convertExt = ".pdf";
				if(StringUtils.isNotBlank(fileExt)) {
					clientFileName = String.valueOf(clientFileName.substring(0, clientFileName.lastIndexOf("."))) + convertExt;
				}
				
				uploadFileData.put("FILE_NAME_CLIENT", getThisSessionText(houseCode, "REQ_SEAL","layer", null) + "_" + clientFileName);
				uploadFileData.put("FILE_NAME_SERVER", toPdfTempPath.getName());
				
				this.fileManager.setLayer(layer);
				this.fileManager.doUploadFile(uploadFileData, null, false);
			}
		}

		return true;
	}
	
	//날인요청 인장 이미지 치환 (전체)
	public boolean doConvertReqSealDocument() {
		return this.doConvertReqSealDocument(null, null);
	}
	
	public boolean doCheckRealDocument(Map<String, String> data) {
		List<Map<String, String>> docList = new ArrayList<Map<String, String>>();
		docList = this.doSelectSSIPD(data);
		boolean flag = true;
		for (int i=0; i<docList.size(); i++) {			
			if (Integer.parseInt(docList.get(i).get("REAL_ATTACH_CNT")) == 0) {
				flag = false;
			}
		}
		return flag;
	}
	
	// 요청자에게 결재완료 메일 발송
	public boolean sendCompleteMailToRequester() {
    	
        if(SepoaModuleService.checkBusiness(config, info, "MAIL1649307822120")) {
            try {
            	SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "getApprovalData");
            	SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            	ssm.setSepoaInfo(info);
            	SepoaFormater sf = new SepoaFormater(ssm.doSelect(docBasicInfo));
            	
     			// 메일 발송 진행
            	SepoaPushManager mailpush = new SepoaPushManager(info, config);
            	Map<String, String> mailData = new HashMap<String, String>();
            	
            	SimpleDateFormat format = new SimpleDateFormat ("yyyy년 MM월 dd일 HH시 mm분");
            	String house_code = docBasicInfo.get("HOUSE_CODE");
            	mailData.put("HOUSE_CODE", house_code);
            	mailData.put("COMPANY_CODE", docBasicInfo.get("COMPANY_CODE"));
            	String lang = sf.getValue("LANGUAGE", 0);
            	mailData.put("LANGUAGE", lang);
            	String appNo = sf.getValue("APP_NO", 0);
            	mailData.put("APP_NO", appNo);
            	mailData.put("APP_NO_SEQ", sf.getValue("APP_NO_SEQ", 0));
            	
            	mailData.put("MAIL_TITLE", getThisSessionText(house_code, "MAIL", "SEAL_MAIL_TITLE", lang));
            	mailData.put("SUBJECT", getThisSessionText(house_code, "MAIL", "APPROVAL_COMPLETE", lang));
            	mailData.put("REQUESTER", sf.getValue("APP_USER_NAME", 0));
            	mailData.put("TIME", format.format(new Date()));
            	mailData.put("STEP", getThisSessionText(house_code, "MAIL", "APP_STATUS_COMPLETE", lang));
            	if ("".equals(appNo)) {
            		mailData.put("CONTENTS_NAME", getThisSessionText(house_code, "MAIL", "REQ_NUMBER", lang));
            		mailData.put("CONTENTS", docBasicInfo.get("SMR_NUMBER"));
            	} else {
            		mailData.put("CONTENTS_NAME", getThisSessionText(house_code, "MAIL", "APP_NO", lang));
            		mailData.put("CONTENTS", sf.getValue("APP_NO", 0));
            	}
            	
	 			String baseURL = config.get("sepoa."+house_code+".domain");
	 			mailData.put("URL",baseURL);
            	SepoaMessage message = new SepoaMessage();
				message.addParams(mailData);
				message.setMessageType("APPROVAL_COMP");
				mailpush.addMessage(message);
            	mailpush.accept();
            	
        		Logger.debug.println("##### 완료 메일 발송 #####");
        		
			} catch (Exception e2) {
				Logger.err.println("$$$$$ 완료 메일 발송 실패 $$$$$");
				return false;
			}
        }
        return true;
    }
	
	//발행번호 저장 외부 서비스
	public boolean doCreateIssueNo(Map<String, String> data) {
		if(!this.doInsertSDOIS(data)) {
			return false;
		}
		
		//상세 코드 값 구현 예정
		
		return true;
	}
	
	//날인요청 이미지 치환 옵션 파싱
	protected Map<String, String> parseOptionReqSeal(List<Map<String, String>> layerImages) {
		Map<String, String> options = new HashMap<String, String>();
		
		for(Map<String, String> layerImage : layerImages) {
			
        	if(StringUtils.isNotBlank(MapUtils.getString(layerImage, "FIX_HTML", "")) &&
        			layerImage.get("FIX_HTML").indexOf("contract-item-prop-symmetry_page_copy-ON") >= 1) {
        		options.put("PAGE_COPY", "ON");
        	}
        }
		return options;
		
	}
	
	// 발행번호 저장 - 날인서류
	protected int doUpdatePublishNo(Map<String, String> data) {
		int rtn = 0;
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			rtn = ssm.doUpdate(data);
			
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return rtn;
	}
	
	// 발행번호 저장 - 교부서류
	protected int doUpdateCertDocPublishNo(Map<String, String> data) {
		int rtn = 0;
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			rtn = ssm.doUpdate(data);
			
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return rtn;
	}
	
	
	//특수 인장 파싱 로직
	protected String parseSpecialSeal(Element el) throws Exception{
		return parseSpecialSeal(el, new HashMap<String, String>());
	}
	
	protected String parseSpecialSeal(Element el, Map<String, String> extraParams) throws Exception{
		List<String> classList = Arrays.asList(el.attr("class").split(" "));
		String boxCodePrefix = "contract-item-box-code-";
		String boxCode = "";
		
		for(String classStr : classList) {
			if(classStr.startsWith(boxCodePrefix)) {
				boxCode = classStr.substring(boxCodePrefix.length(), classStr.length());
				break;
			}
		}
		
		if(StringUtils.isBlank(boxCode)) {
			return null;
		}

		String specialSeal = null;
		switch(boxCode.toUpperCase()) {
			case "SEND_SEAL":
				specialSeal = this.getSendSealCore();
				break;
			case "QR_LINK":
				specialSeal = this.getSealDocLinkQR(extraParams);
				break;
			case "DOC_INFO_QR":
				specialSeal = this.getDocInfoQR(extraParams);
				break;
			default:
				break;
		}
		
		return specialSeal;
	}
	
	//날인요청 FIX_HTML 분석 후 이미지 반영 CSS생성
	protected String parseAttachSeal(String htmlStr) throws Exception{
		return parseAttachSeal(htmlStr, new HashMap<String, String>(), new HashMap<String, String>());
	}
	
	protected String parseAttachSeal(String htmlStr, Map<String, String> extraParams, Map<String, String> optionMap) throws Exception{
		Elements elements = null;
		StringBuffer sbSealID = new StringBuffer();
		try {
			elements = new Element("div").html(htmlStr).children();
			Map<String, String> templateMappingData = null;
			
			try {
				templateMappingData = new Gson().fromJson(MapUtils.getString(extraParams, "TEMPLATE_MAPPING_DATA", "{}"), Map.class);
			} catch (Throwable t) {
				Logger.err.println("Template Data Deserialization Fail!", t);
				templateMappingData = null;
			}
			for(Element el : elements) {
				List<String> classList = Arrays.asList(el.attr("class").split(" "));
				String boxSealCodePrefix = "contract-item-box-seal-code-";
				String boxCodePrefix = "contract-item-box-code-";
				String itemBoxSealCode = "";
				String itemBoxCode = "";
				String copyFlagVal = "contract-item-prop-symmetry_page_copy-ON";
				Boolean copyFlag = el.children().toString().contains(copyFlagVal); //true 일 경우 간인 인장
				for(String classStr : classList) {
					if(classStr.startsWith(boxSealCodePrefix)) {
						itemBoxSealCode = classStr.substring(boxSealCodePrefix.length(), classStr.length());
					} else if(classStr.startsWith(boxCodePrefix)) {
						itemBoxCode = classStr.substring(boxCodePrefix.length(), classStr.length());
					}
					if (!"".equals(itemBoxSealCode) && !"".equals(itemBoxCode)) break;
				}
				
				String specialSeal = parseSpecialSeal(el, extraParams);
				if(specialSeal != null) {
					el.children().remove();
					el.append(specialSeal);
				} else {
					if(StringUtils.isNotBlank(itemBoxSealCode)) {
						Configuration cfg = null;
						
						try {
							cfg = new Configuration();
						} catch(Exception e) {
							this.errCode = 103336;
							return null;
						}
						
						SepoaSealManagementManager sm = new SepoaSealManagementManager(info, cfg, this.ctx);
						sm.setDocBaiscInfo(new SingletonMap<>("MANAGE_DOC_NO", itemBoxSealCode));
						
						Map<String, String> sealImage = sm.doSelectSealImage("SEAL_IMAGE");
						if(sealImage == null) {
							if(templateMappingData != null && templateMappingData.size() != 0) {
								sm.setDocBaiscInfo(new SingletonMap<>("MANAGE_DOC_NO", templateMappingData.get(itemBoxCode)));
								sealImage = sm.doSelectSealImage("SEAL_IMAGE");
							}
							if(sealImage == null) {
								this.errCode = 103336;
								return null;
							}
						}
						String imgStr = "data:image/png;base64,"+this.imageToBase64(sealImage.get("FILE_PATH"));
						
						StringBuffer sb = new StringBuffer();
						sb.append("<style>");
						sb.append(".contract-item-box-seal-code-"+itemBoxSealCode+" .contract-item.contract-item-input { \r\n");
						sb.append("		background-image:url('" + imgStr + "') !important; \r\n");
						sb.append("		background-size:100% 100%; \r\n");
						sb.append("		background-repeat:no-repeat; \r\n");
						sb.append("} \r\n");
						
						sb.append(".contract-item-box-seal-code-"+itemBoxSealCode+" .contract-item.contract-item-cover { \r\n");
						sb.append("		background-image: none !important; \r\n");
						sb.append("} \r\n");
						sb.append("</style>");
						
						el.append(sb.toString());
						
						final String sealTypeStr = Cache.combo.get("SL0001,"+info.getSession("HOUSE_CODE")+'#'+info.getSession("LANGUAGE")+"#SM003");
						final List<Map<String,String>> sealTypeDefine = new Gson().fromJson(sealTypeStr, List.class);
						List<String> sealList = sealTypeDefine.stream().map(map -> map.get("value")).collect(Collectors.toList());
						
						if(sealList.contains(itemBoxCode)) {
							extraParams.put("MANAGE_NUMBER", itemBoxSealCode);
							writeStampHistory(extraParams);
							if(SepoaModuleService.checkBusiness(new Configuration(), info, "SID1657078543436")) {
								if (!copyFlag) {//간인이 아닐경우						
									String sealID = extraParams.get("SEAL_ID");
									String sealID_html = "<p style='"+getSealID_style(el.attr("style"))+"color: #c4c4c4;opacity: 0.7;'>"+sealID.substring(0, 11)+"..</p>"; 
									sbSealID.append(sealID_html);
								}
								
							}
						} 
					} else if(StringUtils.isNotBlank(itemBoxCode)) {
						if(templateMappingData != null && templateMappingData.size() != 0 && templateMappingData.containsKey(itemBoxCode)) {
							String value = templateMappingData.get(itemBoxCode);
							el.child(0).val(value);
							el.attr("style", el.attr("style") + " width: 100% !important; height: 100% !important;");
						} else {
							switch(itemBoxCode) {
								case "PRINT_DATE":
								el.attr("style", el.attr("style") + " width: 100% !important; height: 100% !important;");
								Element targetEl = el.child(0);
								String dateFormatPrefix = "contract-item-prop-date_format-";
								String dateFormat = "";
								List<String> targetElClassList = Arrays.asList(targetEl.attr("class").split(" "));
								
								for(String cls : targetElClassList) {
									if(cls.startsWith(dateFormatPrefix)) {
										dateFormat = cls.substring(dateFormatPrefix.length(), cls.length());
										break;
									}
								}
								
								if(StringUtils.isNotBlank(dateFormat)) {
									String javaDatePattern = convertDateFormatEditorToManager(dateFormat);
									if(StringUtils.isNotBlank(javaDatePattern)) {
										SimpleDateFormat legacyFormat = new SimpleDateFormat("yyyyMMdd");
										SimpleDateFormat newFormat = new SimpleDateFormat(javaDatePattern);
										
										String date = newFormat.format(legacyFormat.parse(sUtils.date.getToday()));
										el.child(0).val(date);
									}
								}
								break;
							}
						}
					} 
				}
			}
		} catch (Exception e) {
			Logger.err.println("ParseAttachSeal Fail!" + extraParams, e);
			throw new Exception(e.getMessage());
		}
		
		
		return elements.toString()+"\r\n"+sbSealID.toString();
	}
	
	private String convertDateFormatEditorToManager(String pattern) {
		final Map<String, String> format = new HashMap<>();
		format.put("%Y", "yyyy");
		format.put("%Y/%m/%d", "yyyy/MM/dd");
		format.put("%m", "MM");
		format.put("%d", "dd");
		format.put("%Y년_%m월_%d일", "yyyy년 MM월 dd일");
		return MapUtils.getString(format, pattern, "");
	}
	
	// 인장식별번호 style 반환
	protected String getSealID_style(String css) {	// 파라미터 : 인장의 style
		String[] styles = css.split(";");
		Double top = 0.0d;
		StringBuilder sbCss = new StringBuilder();
		for (String style : styles) {
			String[] kv = style.split(":");
			String key = kv[0].trim();
			if ("left".equals(key) || "z-index".equals(key)) {
				sbCss.append(style+";");
			} else if ("top".equals(key) || "height".equals(key)) {
				String val = kv[1].trim();
				top += Double.parseDouble(val.replace("%", ""));
			}
		}
		sbCss.append("top: calc("+String.format("%.2f",top)+"% - 8px);");
		sbCss.append("position: absolute;font-size: 12px;");
		
		return sbCss.toString();
	}
	
	// 인장날인이력 기록
	protected void writeStampHistory(Map<String, String> extraParams) throws Exception {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "getSealID");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(extraParams));
			extraParams.put("SEAL_ID", sf.getValue("SEAL_ID",0));

			sxp = new SepoaXmlParser(this.getClass(), "checkStampHistory");
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			sf = new SepoaFormater(ssm.doSelect(extraParams));
			if (sf.getRowCount() == 0) {
				sxp = new SepoaXmlParser(this.getClass(), "writeStampHistory");
				ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
				ssm.setSepoaInfo(info);
				ssm.doInsert(extraParams);
			}
			
		} catch(Exception e) {
			this.errCode = 102419;
			throw new Exception(e.getMessage());
		}
		return;
	}
	
	
	public boolean doCheckFileDownload(String smrNumber) {
		List<Map<String, String>> checkList = this.doSelectRequestList(new SingletonMap<String, String>("SMR_NUMBER", smrNumber));
		
		if(checkList == null) {
			this.errCode = 102007;
			return false;
		} else {
			for(Map<String, String> doc : checkList) {
				boolean checkYn = this.doCheckFileDownload(doc);
				if(!checkYn) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public boolean doCheckFileDownload(Map<String, String> checkMap) {
		List<Map<String, String>> docList = null;
		
		if(MapUtils.getString(this.getDocBaiscInfo(), "CALLER", "REQ_SEAL").endsWith("REQ_SEAL")) {
			docList = this.doSelectReqSealDoc(checkMap);
		} else if("ATTACH_DOC_TEMPLATE".equalsIgnoreCase(this.getDocBaiscInfo().get("CALLER"))) {
			docList = this.doSelectCorpAttachDoc(checkMap);
		} else if("REQ_SEAL_REAL_DOC".equalsIgnoreCase(this.getDocBaiscInfo().get("CALLER"))) {
			docList = this.doSelectReqSealRealDocFile();
		}
		Map<String, String> docInfo = new HashMap<String, String>();
		
		if(docList.size() > 0) {
			docInfo = docList.get(0);
		} else {
			this.errCode = 102416;
			return false;
		}
		
		// 아래 조건을 회피할방법이 필요하다
		if((MapUtils.getIntValue(docInfo, "COUNT", 0) - MapUtils.getIntValue(docInfo, "DOWN_COUNT", 0)) <= 0 && "E".equals(docList.get(0).get("RE_FLAG"))) {
			this.errCode = 103340;
			return false;
		}else if((MapUtils.getIntValue(docInfo, "COUNT", 0) - MapUtils.getIntValue(docInfo, "DOWN_COUNT", 0)) <= 0 && "E".equals(docList.get(0).get("REAL_DOC_CHK"))){
			this.errCode = 103340;
			return false;
		}
		
		return true;
	}
	
	public Map<String, String> doDocumentDownload(List<Map<String, String>> paramList, boolean useDefaultCondition){
		return this.doDocumentDownload("REQ_SEAL", paramList, useDefaultCondition, null);
	}
	
	/**
	 * 날인요청(SMR) 기준 실물날인확정서류(REQ_SEAL_REAL_DOC) 전체 ZIP 다운로드.
	 * 파일이 1건이어도 ZIP으로 내려준다. (FileManager는 수정하지 않음)
	 */
	public Map<String, String> doRealAttachDocumentDownloadAll(String smrNumber, String fileName){
		Map<String, String> param = new HashMap<String, String>();
		param.put("SMR_NUMBER", smrNumber);
		
		List<Map<String, String>> docList = this.doSelectReqSealDoc(param);
		if(docList == null || docList.isEmpty()) {
			this.errCode = 102416;
			return null;
		}
		
		List<String> fileIdList = new LinkedList<String>();
		for(Map<String, String> docInfo : docList) {
			String smdNumber = docInfo.get("SMD_NUMBER");
			if(StringUtils.isBlank(smdNumber)) {
				continue;
			}
			
			if(this.fileManager != null) {
				this.destoryFileManager();
			}
			
			if(!this.initFileManager(smrNumber + "_" + smdNumber)) {
				continue;
			}
			
			List<Map<String, String>> layerFiles = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL_REAL_DOC"), LEVEL.FILE);
			if(layerFiles != null) {
				for(Map<String, String> file : layerFiles) {
					String fileId = file.get("FILE_ID");
					if(StringUtils.isNotBlank(fileId)) {
						fileIdList.add(fileId);
					}
				}
			}
		}
		
		if(fileIdList.isEmpty()) {
			this.errCode = 100511;
			return null;
		}
		
		if(this.fileManager == null) {
			this.errCode = 100001;
			return null;
		}
		
		// 서로 다른 SMD(DOC_NUMBER) 파일을 모으므로 DOC_NUMBER 조건 없이 FILE_ID로만 조회
		Map<String, String> result = this.fileManager.doDownload(fileIdList, false, false);
		if(result == null) {
			this.errCode = 100512;
			return null;
		}
		
		String zipName = StringUtils.isNotBlank(fileName) ? fileName : smrNumber;
		if(!zipName.toLowerCase().endsWith(".zip")) {
			zipName = zipName + ".zip";
		}
		
		// FileManager는 1건일 때 원본 파일을 반환하므로, 단건도 ZIP으로 감싼다.
		String clientName = MapUtils.getString(result, "FILE_NAME_CLIENT", "");
		if(!clientName.toLowerCase().endsWith(".zip")) {
			result = this.wrapSingleFileAsZip(result, zipName);
			if(result == null) {
				this.errCode = 100512;
				return null;
			}
		} else {
			result.put("FILE_NAME_CLIENT", zipName);
		}
		
		return result;
	}
	
	/**
	 * FileManager 단건 다운로드 결과를 ZIP 1개로 재포장한다.
	 */
	private Map<String, String> wrapSingleFileAsZip(Map<String, String> fileInfo, String zipFileName) {
		ZipArchiveOutputStream zout = null;
		OutputStream fos = null;
		InputStream in = null;
		try {
			String entryName = MapUtils.getString(fileInfo, "FILE_NAME_CLIENT", "document");
			String sourcePath = MapUtils.getString(fileInfo, "FILE_PATH", "");
			File sourceFile = new File(sourcePath);
			if(!sourceFile.exists()) {
				return null;
			}
			
			String tempFileName = this.tempStorage + File.separatorChar + System.nanoTime() + "_" + String.valueOf((int) (Math.random() * 10000));
			File tempZipFile = new File(tempFileName);
			
			fos = CipherManager.getCipher().getOutputStream(tempZipFile, true);
			zout = new ZipArchiveOutputStream(fos);
			
			in = CipherManager.getCipher().getInputStream(sourceFile, false);
			zout.putArchiveEntry(new ZipArchiveEntry(entryName));
			byte[] buff = new byte[0x1000];
			int len;
			while ((len = in.read(buff)) > 0) {
				zout.write(buff, 0, len);
			}
			zout.closeArchiveEntry();
			zout.close();
			zout = null;
			
			Map<String, String> zipFileInfo = new HashMap<String, String>();
			zipFileInfo.put("FILE_PATH", tempFileName);
			zipFileInfo.put("FILE_NAME_CLIENT", zipFileName);
			return zipFileInfo;
		} catch (Exception e) {
			Logger.err.println(e);
			return null;
		} finally {
			if(in != null) {
				try { in.close(); } catch (IOException e) { Logger.err.println(e); }
			}
			if(zout != null) {
				try { zout.close(); } catch (IOException e) { Logger.err.println(e); }
			}
			if(fos != null) {
				try { fos.close(); } catch (IOException e) { Logger.err.println(e); }
			}
		}
	}
	
	public Map<String, String> doDocumentDownload(String target, List<Map<String, String>> paramList, boolean useDefaultCondition, String key){
		String targetLayers[] = {};
		String reqSealLayers[] = {"CONVERT_SEAL_ORG", "CONVERT_SEAL_SYM"};
		String attachDocLayers[] = {"CONVERT_CERT_ORG", "CONVERT_CERT_MERGE"};
		String reqSealRealDoc[] = {"REQ_SEAL_REAL_DOC"};
		if("REQ_SEAL".equals(target.toUpperCase())) {
			if("R".equals(paramList.get(0).get("RE_FLAG")) || "I".equals(paramList.get(0).get("SMR_STATUS"))) {
				String[] attchLayers = {"REQ_SEAL"};
				targetLayers = attchLayers;
			} else {
				targetLayers = reqSealLayers;
			}
		} else if("ATTACH_DOC_TEMPLATE".equalsIgnoreCase(target.toUpperCase())) {
			targetLayers = attachDocLayers;
		} else if("REQ_SEAL_REAL_DOC".equalsIgnoreCase(target.toUpperCase())) {
			targetLayers = reqSealRealDoc;
		};
		
		List<String> fileList = new LinkedList<String>();
		
		for(Map<String, String> docInfo : paramList) {
			if(this.fileManager != null) {
				this.destoryFileManager();
			}
			
			if(StringUtils.isNotBlank(key)) {
				this.initFileManager(key);
			} else {
				this.initFileManager(docInfo.get("SMR_NUMBER") + "_" + docInfo.get("SMD_NUMBER"));
			}
			
			for(String layer : targetLayers) {
				List<Map<String, String>> layerFiles = this.fileManager.doList(new SingletonMap<String, String>("LAYER", layer), LEVEL.FILE);
				if(layerFiles != null) {
					for(Map<String, String> file : layerFiles) {
						fileList.add(file.get("FILE_ID"));
					}
				}
			}
		}
		
		if(fileList.size() <= 0) {
			this.errCode = 100511;
			return null;
		}else{
			Map<String, String> result = this.fileManager.doDownload(fileList, false, useDefaultCondition);
			if(result == null) {
				this.errCode = 100512;
				return null;
			}else {
				String ext = result.get("FILE_NAME_CLIENT").endsWith("zip")?".zip":".pdf";
				String fileName = "";
				if(StringUtils.isBlank(result.get("FILE_NAME_CLIENT"))) {
					fileName = this.docBasicInfo.get("SUBJECT");
				} else {
					if(StringUtils.contains(result.get("FILE_NAME_CLIENT"), this.docBasicInfo.get("SMR_NUMBER"))) {
						fileName = this.docBasicInfo.get("SUBJECT");
					} else {
						int fileNameIdx = result.get("FILE_NAME_CLIENT").indexOf(".");
						fileName = result.get("FILE_NAME_CLIENT").substring(0, fileNameIdx);
					}
				}
				result.put("FILE_NAME_CLIENT", fileName+ext);
			}
			
			return result;
		}
	}
	
	public boolean addDownloadCount(List<Map<String, String>> paramList){
		// 다운로드 제한 모듈 사용여부를 변경할 경우 대응할 코드 필요
		if (SepoaModuleService.checkBusiness(config, info, "US1638852635132") || SepoaModuleService.checkBusiness(config, info, "CERT1686730093044") || SepoaModuleService.checkBusiness(config, info, "SUM1693975008371")){
			for(Map<String, String> docInfo : paramList) {
				int downCount = MapUtils.getIntValue(docInfo, "DOWN_COUNT", 0);
				docInfo.put("DOWN_COUNT", String.valueOf(downCount+1));
				boolean resultData = false;
				if(this.getDocBaiscInfo().get("CALLER").endsWith("REQ_SEAL")) {
					resultData = this.doUpdateSSIPD(docInfo);
				} else if("ATTACH_DOC_TEMPLATE".equalsIgnoreCase(this.getDocBaiscInfo().get("CALLER"))) {
					resultData = this.doUpdateSSISD(docInfo);
				} else if("REQ_SEAL_REAL_DOC".equalsIgnoreCase(this.getDocBaiscInfo().get("CALLER"))) {
					return true;
				}
				
				if(resultData == false) {
					this.errCode = 103325;
					return false;
				}
			}
		}
		
		return true;
	}
	
	// 발송인 처리 로직
	public boolean doProcessSendSeal(Map<String, String> docInfo) {
		String layer = "REQ_SEAL";
		
		if(this.fileManager != null) {
			this.destoryFileManager();
		}
		
		if(!this.initFileManager(docInfo.get("SMR_NUMBER")+"_"+docInfo.get("SMD_NUMBER"))) {
			this.errCode = 100001;
			return false;
		}
		
		try {
			List<Map<String, String>> layerFiles = this.fileManager.doList(new SingletonMap<String, String>("LAYER", layer), LEVEL.PARTS);
			if(layerFiles != null) {
				Map<String, String> firstPageFile = layerFiles.get(0);
				String fixHtml = firstPageFile.get("FIX_HTML");
				if(StringUtils.isBlank(fixHtml)) {
					fixHtml = "";
				}
				
				StringBuffer sb = new StringBuffer("");
				sb.append(fixHtml);
				
				String html = getSendSealCore();
				sb.append(html);
				
				firstPageFile.put("FIX_HTML", sb.toString());
				this.fileManager.doModifyMeta(firstPageFile);
				int errCode = this.fileManager.getLastErrCode();
				if(errCode != 0) {
					this.errCode = 103325;
					return false;
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return true;
	}

	private String getSendSealCore() throws IOException {
		String html = FileUtils.readFileToString(new File(CommonUtil.getConfig("sepoa.send.seal.html.dir")+"/sendSeal.html"), "UTF-8");
		LocalDateTime date = LocalDateTime.now();
		double currentHour = date.getHour();
		double currentMinutes = date.getMinute();

		Elements elements = Jsoup.parse(html).body().children();
		Elements numberTarget = elements.select(".sign-image-circle-container.number .sign-image-circle-circle");
		Elements hourTarget = elements.select(".sign-image-circle-container.hours .sign-image-circle-circle");
		Elements minuteTarget = elements.select(".sign-image-circle-container.minutes .sign-image-circle-circle");
		Map<String, Elements> targetMap = new HashMap<>();
		targetMap.put("NUMBER", numberTarget);
		targetMap.put("HOURS", hourTarget);
		targetMap.put("MINUTES", minuteTarget);

		Pattern pattern = Pattern.compile("(rotate.*(.\\d\\.\\d*))");
		for(String key : targetMap.keySet()) {
			Elements e = (Elements)MapUtils.getObject(targetMap, key);
			if(e.size() > 0) {
				Double replaceValue = null;
				String target = e.get(0).attr("style");
				Matcher matcher = pattern.matcher(target);
				while(matcher.find()) {
					switch(key) {
					case "NUMBER":
						replaceValue = -0.5 + currentHour - 1 + ((currentMinutes / (60 / 4)) / 4);
						break;
					case "HOURS":
						replaceValue = 0.5 + -0.5 + currentHour - 1 + ((currentMinutes / (60 / 4)) / 4);
						break;
					case "MINUTES":
						replaceValue = (currentMinutes / (60 / 4));
						break;
					default:
						break;
					}
					html = html.replaceAll(matcher.group(2), " " + new String().valueOf(replaceValue));
				}

			}
		}
		
		String no = "";
		Map<String, String> paramMap = new HashMap<String, String>();
		// 완료인 항목
		paramMap.put("SMR_STATUS", "C");
		paramMap.put("APPLY_TYPE", "C");
		paramMap.put("F_CALENDAR_FROM", String.valueOf(SepoaDate.getYear()) + "0101");
		List<Map<String, String>> numberList = this.doSelectSSIAD(paramMap);
		if(numberList != null) {
			no = Integer.toString(numberList.size() + 1);
		} else {
			no = "1";
		}
		
		html = html.replace("#NO#", no);
		html = html.replace("#DATE#", String.valueOf(LocalDate.now()));
		html = html.replace("#COMPANY_NAME#", getThisSessionText(null, "SEND_SEAL","COMPANY_NAME_"+info.getSession("HOUSE_CODE"), null));
		return html;
	}
	
	private String getDocInfoQR(Map<String, String> paramMap) throws Exception {
		List<Map<String, String>> headerInfoList = this.doSelectSSIAD(paramMap);
		List<Map<String, String>> docList=null;
		if("D".equalsIgnoreCase(paramMap.get("APPLY_TYPE"))) {
			docList = doSelectSSISD(paramMap); 
		} else {
			docList = doSelectSSIPD(paramMap);
		}
		
		if(headerInfoList == null || docList == null) {
			throw new Exception("Not Found Header or Document in Parse SpecialSeal");
		}
		
		Map<String, String> document = docList.get(0);
		Map<String, String> headerInfo = headerInfoList.get(0);
		StringBuilder sbi = new StringBuilder();
		sbi.append("요청 번호: " + headerInfo.get("SMR_NUMBER") + "\r\n");
		sbi.append("요청 제목: " + headerInfo.get("SUBJECT") + "\r\n");
		sbi.append("작성일 : " + headerInfo.get("ADD_DATE") + "\r\n");
		sbi.append("작성자 : " + headerInfo.get("ADD_USER_ID") + "\r\n");
		sbi.append("문서명 : " + document.get("SUBJECT") + "\r\n");
		sbi.append("페이지 : " + paramMap.get("pageCnt") + "/" + paramMap.get("pdfTotalPage") + "\r\n");
		
		Logger.info.println(info.getSession("ID"), this, "Doc Info QR Link : "+sbi.toString());
		
		String QR64 = QR_Generater(sbi.toString());
        StringBuffer sb = new StringBuffer();
		sb.append("<div style=\"height:100%;width:100%;background-image:url('data:image/png;base64," + QR64 + "') !important;");
		sb.append("background-size:100% 100%;background-repeat:no-repeat;\" />");
		sb.append("</div>\r\n");
	    
		return sb.toString();
	}
	
	private String getSealDocLinkQR(Map<String, String> paramMap) throws Exception {
		String smrNo = paramMap.get("SMR_NUMBER");
		String smdNo = paramMap.get("SMD_NUMBER");
		SepoaReqSealManager srm = new SepoaReqSealManager(info, new Configuration(), ctx);
		
		Map<String, String> linkParams = new HashMap<String, String>();
		linkParams.put("SMR_NUMBER", smrNo);
		linkParams.put("SMD_NUMBER", smdNo);
		linkParams.put("TARGET", "REQ_SEAL");
		linkParams.put("SMR_STATUS", "C");
		
		String linkType = null;
		if("C".equals(paramMap.get("APPLY_TYPE"))){
			linkType = "CORP.REQ.SEAL.DOC";
		} else {
			linkType = "USE.REQ.SEAL.DOC";
		}
		
		String accessLinkCode = srm.doCreateReqSealDocAccessLink(linkType, smdNo, linkParams, null, "PUBLIC");
		if(accessLinkCode == null || accessLinkCode.isEmpty()) {
			int errCode = srm.getLastErrCode();
			//if(errCode != 0) {
				throw new Exception("[ERROR "+errCode+"]");
			//}
		}
		String contextName = CommonUtil.getConfig("sepoa.system.context.name", "");
		String accessLink = CommonUtil.getConfig("sepoa.system.domain.name")+("".equals(contextName) ? "" : "/"+contextName)+"/link_viewer.jsp?ln="+accessLinkCode;
		accessLink = accessLink.replaceAll("//", "/");
		
		Logger.info.println(info.getSession("ID"), this, "QR Link : "+accessLink);
		
		String QR64 = QR_Generater(accessLink);
        StringBuffer sb = new StringBuffer();
		sb.append("<div style=\"height:100%;width:100%;background-image:url('data:image/png;base64," + QR64 + "') !important;");
		sb.append("background-size:100% 100%;background-repeat:no-repeat;\" />");
		sb.append("</div>\r\n");
	    
		return sb.toString();
	}
	
	private String QR_Generater(String url) throws WriterException, IOException {
		QRCodeWriter writer = new QRCodeWriter();
		String urlIso = new String(url.getBytes("UTF-8"), "ISO-8859-1");
		BitMatrix matrix = writer.encode(urlIso, BarcodeFormat.QR_CODE, 1, 1,ImmutableMap.of(EncodeHintType.MARGIN,5));
		
		MatrixToImageConfig config = new MatrixToImageConfig(MatrixToImageConfig.BLACK, MatrixToImageConfig.WHITE);
		BufferedImage QR_Image = MatrixToImageWriter.toBufferedImage(matrix, config);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		ImageIO.write(QR_Image, "png", bos);
		byte[] imageBytes = bos.toByteArray();

        String imgBase64 = new String(Base64.encodeBase64(imageBytes)).replaceAll("\r", "").replaceAll("\n", "");
        bos.close();
        
	    return imgBase64;
		
	}
	
	public List<Map<String, String>> doLedgerList(Map<String, String> map){
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		if(map == null) {
			map = this.docBasicInfo;
		}
		
		resultList = this.doSelectLedgerList(map);
		if(resultList == null) {
			this.errCode = 102007;
			return null;
		}
		
		return resultList;
	}
	
	public boolean doInsertFixHtml(String fixHtml, String smrNumber, String smdNumber) {
		
		if(this.fileManager != null) {
			this.destoryFileManager();
		}
		
		if(!this.initFileManager(smrNumber+"_"+smdNumber)) {
			this.errCode = 100001;
			return false;
		}
		
		List<Map<String, String>> layerFiles = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "REQ_SEAL"), LEVEL.PARTS);
		if(layerFiles != null) {
			Map<String, String> firstPageFile = layerFiles.get(0);
			if(StringUtils.isNotBlank(firstPageFile.get("FIX_HTML"))) {
				firstPageFile.put("FIX_HTML", firstPageFile.get("FIX_HTML") + " " + fixHtml);
			} else {
				firstPageFile.put("FIX_HTML", fixHtml);
			}
			
			this.fileManager.doModifyMeta(firstPageFile);
			int errCode = this.fileManager.getLastErrCode();
			if(errCode != 0) {
				this.errCode = 103325;
				return false;
			}
		}
		
		
		
		return true;
	}
	
	
	/*
	 * Table Action 
	 */
	//날인요청 관리 테이블 조회 
	protected List<Map<String, String>> doSelectSSIAD(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			String sealItemStr = ssm.doSelect(data);
			List<Map<String, String>> list = new SepoaFormater(sealItemStr).getListMap();
			
			if (list.size() > 0) {
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//인장 테이블 조회
	protected List<Map<String, String>> doSelectSSEAL(Map<String, String> data) {
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
	
	//날인요청 관리 테이블 데이터 적재 
	protected Map<String, String> doInsertSSIAD(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSIAD(data).get(0);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//날인요청 관리 테이블 업데이트
	protected boolean doUpdateSSIAD(Map<String, String> data) {
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
		
	//날인요청 관리 테이블 데이터 삭제
	protected boolean doDeleteSSIAD(Map<String, String> data) {
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
	
	//날인서류 관리 테이블 조회 
	protected List<Map<String, String>> doSelectSSIPD(Map<String, String> data) {
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
	
	// 동아제약 메일 발송을 위한 날인서류 관리 테이블 조회
	protected List<Map<String, String>> doSelectForMailSSIPD(Map<String, String> data) {
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
	
	//날인서류 관리 테이블 데이터 적재 
	protected Map<String, String> doInsertSSIPD(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSIPD(data).get(0);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//날인서류 관리 테이블 업데이트
	protected boolean doUpdateSSIPD(Map<String, String> data) {
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
	
	//날인서류 관리 테이블 데이터 삭제
	protected boolean doDeleteSSIPD(Map<String, String> data) {
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
	
	//날인서류에 사용할 인감 관리 테이블 조회 
	protected List<Map<String, String>> doSelectSSIPD_SEAL(Map<String, String> data) {
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
	
	//날인서류에 사용할 인감 관리 테이블 데이터 적재 
	protected Map<String, String> doInsertSSIPD_SEAL(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectSSIPD_SEAL(data).get(0);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//날인서류에 사용할 인감 관리 테이블 업데이트
	protected boolean doUpdateSSIPD_SEAL(Map<String, String> data) {
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
	
	//날인서류에 사용할 인감 관리 테이블 데이터 삭제
	protected boolean doDeleteSSIPD_SEAL(Map<String, String> data) {
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
	
	//날인서류에 사용할 인감 관리 테이블 데이터 삭제 (인감번호 없이)
	protected boolean doDeleteSSIPD_SEAL_NO_MANAGE_NUMBER(Map<String, String> data) {
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
	
	//날인요청 관리 테이블 조회 
	protected String doSelectSUPIM(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			String sealItemStr = ssm.doSelect(data);
			if (StringUtils.isNotBlank(sealItemStr)) {
				return sealItemStr;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//날인요청 교부요청서류 관리 테이블 조회 
	protected List<Map<String, String>> doSelectSSISD(Map<String, String> data) {
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
	
	//날인요청 교부요청서류 관리 테이블 데이터 적재 
	protected Map<String, String> doInsertSSISD(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
			
			return this.doSelectSSISD(data).get(0);
			
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//날인요청 교부요청서류 관리 테이블 업데이트
	protected boolean doUpdateSSISD(Map<String, String> data) {
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
	
	//날인요청 교부요청서류 관리 테이블 데이터 삭제
	protected boolean doDeleteSSISD(Map<String, String> data) {
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
	
	//교부서류 관리 테이블 조회 
	protected List<Map<String, String>> doSelectSSRAD(Map<String, String> data) {
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
	
	//교부서류 관리 테이블 데이터 적재 
	protected boolean doInsertSSRAD(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//교부서류 관리 테이블 업데이트
	protected boolean doUpdateSSRAD(Map<String, String> data) {
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
	
	//교부서류 관리 테이블 업데이트
	protected boolean doDeleteSSRAD(Map<String, String> data) {
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
	
	//관리대장 리스트 조회
	protected List<Map<String, String>> doSelectLedgerList(Map<String, String> data) {
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
	
	//문서 발행 관리 테이블 데이터 적재 
	protected boolean doInsertSDOIS(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//그룹웨어 관리 테이블 조회 
	protected List<Map<String, String>> doSelectSSIAD_GW(Map<String, String> data) {
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
	
	//그룹웨어 서류 관리 테이블 조회 
	protected List<Map<String, String>> doSelectSSIPD_GW(Map<String, String> data) {
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
	
	// 회수가능여부 확인
	public boolean doCheckReqSealWithdrawable(String smrNumber) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doCheckReqSealWithdrawable");
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
	
	public byte[] makeCertAuditTrailCertificate() throws Exception {
		String templateHtml = null;
		StringBuilder htmls = new StringBuilder();
		byte[] pdfBytesArr = null;
		File tempFdfFile = null;
		
		try {
			String certAuditTemplateFilePath = CommonUtil.getConfig("sepoa.seal.certaudittrail.resoucre");
			
			File resource = new File (certAuditTemplateFilePath);
			if(resource.exists()) {
				templateHtml = new String(FileUtils.readFileToByteArray(resource), "UTF-8");
				
				//다국어로 치환할 것들 여기서 치환 
				templateHtml = templateHtml
				.replace("#MAIN_HEADER#", Cache.text.get("AUDIT_TRAIL.MAIN_HEADER_CERT"))
				.replace("#HEADER_SUBJECT#", Cache.text.get("AUDIT_TRAIL.HEADER_SUBJECT"))
				.replace("#REQUEST_NUMBER#", Cache.text.get("AUDIT_TRAIL.REQUEST_NUMBER"))
				.replace("#REQUEST_DEPT#", Cache.text.get("AUDIT_TRAIL.REQUEST_DEPT"))
				.replace("#REQUESTER_NAME#", Cache.text.get("AUDIT_TRAIL.REQUESTER_NAME"))
				.replace("#REQUEST_DATE#", Cache.text.get("AUDIT_TRAIL.REQUEST_DATE"))
				.replace("#SEALED_DATE#", Cache.text.get("AUDIT_TRAIL.SEALED_DATE"))
				.replace("#SEAL_INFORMATION#", Cache.text.get("AUDIT_TRAIL.SEAL_INFORMATION"))
				.replace("#SEAL_CATEGORY#", Cache.text.get("AUDIT_TRAIL.SEAL_CATEGORY"))
				.replace("#MANAGE_DEPT#", Cache.text.get("AUDIT_TRAIL.MANAGE_DEPT"))
				.replace("#USAGE#", Cache.text.get("AUDIT_TRAIL.USAGE"))
				.replace("#ELECT_IMAGE#", Cache.text.get("AUDIT_TRAIL.ELECT_IMAGE"))
				.replace("#HISTORY#", Cache.text.get("AUDIT_TRAIL.HISTORY_CONTENT"))
				.replace("#USER_IP#", Cache.text.get("AUDIT_TRAIL.USER_IP"))
				.replace("#TIME_ENVIRONMENT#", Cache.text.get("AUDIT_TRAIL.TIME_ENVIRONMENT"))
				.replace("#USER_ID_NAME#", Cache.text.get("AUDIT_TRAIL.USER_ID_NAME"))
				.replace("#HISTORY_CONTENT#", Cache.text.get("AUDIT_TRAIL.HISTORY_CONTENT"))
				.replace("#SEAL_DOCUMENT#", Cache.text.get("AUDIT_TRAIL.CERT_DOCUMENT"))
				.replace("#DOCUMENT_NAME#", Cache.text.get("AUDIT_TRAIL.DOCUMENT_NAME"))
				.replace("#PUBLISH_DATE#", Cache.text.get("AUDIT_TRAIL.PUBLISH_DATE"))
				.replace("#PUBLISH_NO#", Cache.text.get("AUDIT_TRAIL.PUBLISH_NO"))
				.replace("#CERT_DOC_TYPE#", Cache.text.get("AUDIT_TRAIL.CERT_DOC_TYPE"))
				.replace("#PUB_DATE#", Cache.text.get("AUDIT_TRAIL.PUB_DATE"))
				.replace("#PUB_NO#", Cache.text.get("AUDIT_TRAIL.PUB_NO"));
				
				//인감문서 조회(SIAD)
				List<Map<String, String>> siadList = doSelectSSIAD(docBasicInfo);
				Map<String, String> siadData = null;
				if(siadList == null) {
					this.errCode = 103341; //감사추적인증서를 제작할 수 있는 문서가 날인요청문서가 없습니다.
					return null;
				}else {
					//하나이지만 루프 
					for(Map<String, String> row : siadList) {
						siadData = row;
					}
					
					//-날인대상서류 조회(SISD)
					List<Map<String, String>> sisdList = doSelectSSISD(docBasicInfo);
					if(sisdList.size() > 0) {
						//siadData안에 SIAD SMR_NUMBER, SUBJECT, CONFIRM_DATE 등이 들어 있음
						//pmgt의 SEAL_COMPLETE의 DATE, TIME이 날인일시가 됨
						List<Map<String, String>> pmgtList = doQueryProcessHistory(docBasicInfo);
						String sealedDate = "";
						for(Map<String, String> row: pmgtList) {
							if("CERT_COMPLETE".equalsIgnoreCase(MapUtils.getString(row, "ACTION", ""))) {
								String addDate = MapUtils.getString(row, "ADD_DATE");
								String addTime = MapUtils.getString(row, "ADD_TIME");
								sealedDate = addDate + addTime;
							}
						}
						
						//모든 증명서 문서 리스트에 발급일시 넣어주기
						if(!"".equalsIgnoreCase(sealedDate)) {
							for(Map<String, String> sisdData : sisdList) {
								sisdData.put("PUBLISH_DATE", sealedDate);
							}
						}
						
						//1.날인 이력 문서 헤더영역 생성
						templateHtml = createCertAuditTrailHeader(templateHtml, siadData, sealedDate);
						
						if(this.getDocBaiscInfo().containsKey("EXCEPT") && "APPROVAL".equalsIgnoreCase(this.getDocBaiscInfo().get("EXCEPT"))) {
							templateHtml = templateHtml.replace("#HISTORY_ROWS_VIEW#", ".table-3.default-table {display: none}");
							templateHtml = templateHtml.replace("#HISTORY_ROWS#", "");
						} else {
							//3.날인 이력 문서 승인이력 영역 생성
							if(pmgtList.size() > 0) {
								templateHtml = templateHtml.replace("#HISTORY_ROWS#", createAuditTrailHistory(pmgtList));
							}
						}
						
						//4.날인 이력 문서 서류정보 영역 생성
						templateHtml = templateHtml.replace("#DOCUMENT_ROWS#", createCertAuditTrailDocument(sisdList));
						
						//5.문서 발행 번호 , 발행일 (발행일과 시간은 문서발행관리 테이블과 같아야 함)
						String issueNo = UUID.randomUUID().toString()+System.nanoTime();
						issueNo = issueNo.replaceAll("[^0-9]", "");

						Date nowDate = new Date();
						SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");						
						String lang = info.getSession("LANGUAGE");
						if("KO".equalsIgnoreCase(lang)) {
							toDateFormat = new SimpleDateFormat(CommonUtil.getConfig("sepoa.seal.audittrail.pubdate.format", "yyyy년 MM월 dd일 HH시 mm분 ss초"));			
						}else {
							toDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");			
						}
						String addDate = new SimpleDateFormat("yyyyMMdd").format(nowDate);
						String addTime =  new SimpleDateFormat("HHmmss").format(nowDate);
					
						Map<String, String> docIssueParam = new HashMap<>();
						docIssueParam.put("ADD_DATE", addDate);
						docIssueParam.put("ADD_TIME", addTime);
						docIssueParam.put("ISSUE_NO", issueNo);
						
						doInsertSDOIS(docIssueParam);
						
						templateHtml = templateHtml.replace("#ADD_DATE# #ADD_TIME#", toDateFormat.format(nowDate));
						templateHtml = templateHtml.replace("#PUBLISHED_NO#", issueNo);

						SepoaConverter sc = new SepoaConverter();
						sc.setSource(templateHtml);
						tempFdfFile = sc.getPDF();
						
						if(tempFdfFile == null || !tempFdfFile.exists()) {
							this.errCode = 103342; 
							return null;
						}
						
						pdfBytesArr = FileUtils.readFileToByteArray(tempFdfFile);
					}
					
				}		
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			File f = new File(tempFdfFile.getAbsolutePath());
			if(f.exists()) {
				try {
					FileUtils.forceDelete(f);
				}catch(Exception e) {
					Logger.err.println(info.getSession("ID"), this, "[FAIL] Audit Trail temporary PDF file delete fail");
				}
			}			
		}
		
		return pdfBytesArr;
	}
	
	public byte[] makeAuditTrailCertificate() throws Exception {
		String templateHtml = null;
		StringBuilder htmls = new StringBuilder();
		byte[] pdfBytesArr = null;
		File tempFdfFile = null;
		try {
			
			File resource = new File (CommonUtil.getConfig("sepoa.seal.audittrail.resoucre"));
			if(resource.exists()) {
				templateHtml = new String(FileUtils.readFileToByteArray(resource), "UTF-8");
				
				//다국어로 치환할 것들 여기서 치환 
				templateHtml = templateHtml
				.replace("#MAIN_HEADER#", Cache.text.get("AUDIT_TRAIL.MAIN_HEADER"))
				.replace("#HEADER_SUBJECT#", Cache.text.get("AUDIT_TRAIL.HEADER_SUBJECT"))
				.replace("#REQUEST_NUMBER#", Cache.text.get("AUDIT_TRAIL.REQUEST_NUMBER"))
				.replace("#REQUEST_DEPT#", Cache.text.get("AUDIT_TRAIL.REQUEST_DEPT"))
				.replace("#REQUESTER_NAME#", Cache.text.get("AUDIT_TRAIL.REQUESTER_NAME"))
				.replace("#REQUEST_DATE#", Cache.text.get("AUDIT_TRAIL.REQUEST_DATE"))
				.replace("#SEALED_DATE#", Cache.text.get("AUDIT_TRAIL.SEALED_DATE"))
				.replace("#SEAL_INFORMATION#", Cache.text.get("AUDIT_TRAIL.SEAL_INFORMATION"))
				.replace("#SEAL_CATEGORY#", Cache.text.get("AUDIT_TRAIL.SEAL_CATEGORY"))
				.replace("#MANAGE_DEPT#", Cache.text.get("AUDIT_TRAIL.MANAGE_DEPT"))
				.replace("#USAGE#", Cache.text.get("AUDIT_TRAIL.USAGE"))
				.replace("#ELECT_IMAGE#", Cache.text.get("AUDIT_TRAIL.ELECT_IMAGE"))
				.replace("#HISTORY#", Cache.text.get("AUDIT_TRAIL.HISTORY"))
				.replace("#USER_IP#", Cache.text.get("AUDIT_TRAIL.USER_IP"))
				.replace("#TIME_ENVIRONMENT#", Cache.text.get("AUDIT_TRAIL.TIME_ENVIRONMENT"))
				.replace("#USER_ID_NAME#", Cache.text.get("AUDIT_TRAIL.USER_ID_NAME"))
				.replace("#HISTORY_CONTENT#", Cache.text.get("AUDIT_TRAIL.HISTORY_CONTENT"))
				.replace("#SEAL_DOCUMENT#", Cache.text.get("AUDIT_TRAIL.SEAL_DOCUMENT"))
				.replace("#SEAL_CERT_DOCUMENT#", Cache.text.get("AUDIT_TRAIL.SEAL_CERT_DOCUMENT"))
				.replace("#DOCUMENT_NAME#", Cache.text.get("AUDIT_TRAIL.DOCUMENT_NAME"))
				.replace("#PUBLISH_DATE#", Cache.text.get("AUDIT_TRAIL.PUBLISH_DATE"))
				.replace("#PUBLISH_NO#", Cache.text.get("AUDIT_TRAIL.PUBLISH_NO"))
				.replace("#PAGE_COUNT#", Cache.text.get("AUDIT_TRAIL.PAGE_COUNT"))
				.replace("#PUB_DATE#", Cache.text.get("AUDIT_TRAIL.PUB_DATE"))
				.replace("#PUB_NO#", Cache.text.get("AUDIT_TRAIL.PUB_NO"))
				.replace("#DOC_TYPE_LOC#", Cache.text.get("AUDIT_TRAIL.DOC_TYPE_LOC"));
				
				//인감문서 조회(SIAD)
				List<Map<String, String>> siadList = doSelectSSIAD(docBasicInfo);
				Map<String, String> siadData = null;
				
				if(siadList == null) {
					this.errCode = 103341; //감사추적인증서를 제작할 수 있는 문서가 날인요청문서가 없습니다.
					return null;
				}else {
					//하나이지만 루프 
					for(Map<String, String> row : siadList) {
						siadData = row;
					}
					//-날인대상서류 조회(SIPD)
					List<Map<String, String>> sipdList = doSelectSSIPD(docBasicInfo);
					if(sipdList.size() > 0) {
						String firstDocNumber = sipdList.get(0).get("SMR_NUMBER") +"_"+sipdList.get(0).get("SMD_NUMBER");
						
						//siadData안에 SIAD SMR_NUMBER, SUBJECT, CONFIRM_DATE 등이 들어 있음
						//pmgt의 SEAL_COMPLETE의 DATE, TIME이 날인일시가 됨
						List<Map<String, String>> pmgtList = doQueryProcessHistory(docBasicInfo);
						String sealedDate = "";
						for(Map<String, String> row: pmgtList) {
							if("SEAL_COMPLETE".equalsIgnoreCase(MapUtils.getString(row, "ACTION", ""))) {
								String addDate = MapUtils.getString(row, "ADD_DATE");
								String addTime = MapUtils.getString(row, "ADD_TIME");
								sealedDate = addDate + addTime;
							}
						}
						
						List<Map<String, String>> sisdList = doSelectSSISD(docBasicInfo);
						if(sisdList != null && sisdList.size() > 0) {
							//모든 증명서 문서 리스트에 발급일시 넣어주기
							if(!"".equalsIgnoreCase(sealedDate)) {
								for(Map<String, String> sisdData : sisdList) {
									sisdData.put("PUBLISH_DATE", sealedDate);
								}
							}
						}
						
						//1.날인 이력 문서 헤더영역 생성
						templateHtml = createAuditTrailHeader(templateHtml, siadData, firstDocNumber, sealedDate);
						
						//날인 이력 문서 인감정보 문서 조회 (SSIPD_SSEAL)
						List<Map<String, String>> sipdSealList = doSelectSSIPD_SEAL(docBasicInfo);
						
						if(sipdSealList.size() > 0) {
							//2.날인 이력 문서 인감정보 영역 생성 
							templateHtml = templateHtml.replace("#SEAL_INFO_ROWS#", createAuditTrailSeal(sipdSealList, siadData));
						}
						
						if(this.getDocBaiscInfo().containsKey("EXCEPT") && "APPROVAL".equalsIgnoreCase(this.getDocBaiscInfo().get("EXCEPT"))) {
							templateHtml = templateHtml.replace("#HISTORY_ROWS_VIEW#", ".table-3.default-table {display: none}");
							templateHtml = templateHtml.replace("#HISTORY_ROWS#", "");
						} else {
							//3.날인 이력 문서 승인이력 영역 생성
							if(pmgtList.size() > 0) {
								templateHtml = templateHtml.replace("#HISTORY_ROWS#", createAuditTrailHistory(pmgtList));
							}
						}
						
						//4.날인 이력 문서 서류정보 영역 생성
						templateHtml = templateHtml.replace("#DOCUMENT_ROWS#", createAuditTrailDocument(sipdList));
						
						//5.교부 서류 이력 생성
						if(sisdList != null && sisdList.size() > 0) {
							templateHtml = templateHtml.replace("#CERT_DOCUMENT_ROWS#", createCertAuditTrailDocument(sisdList));
						} else {
							templateHtml = templateHtml.replace("#CERT_DOCUMENT_ROWS#", "<tr><td colspan=\"4\" class=\"center-middle\">"+Cache.text.get("AUDIT_TRAIL.EMPTY_CERT_DOCUMENT")+"</td></tr>");
						}
						
						
						
						//6.문서 발행 번호 , 발행일 (발행일과 시간은 문서발행관리 테이블과 같아야 함)
						String issueNo = UUID.randomUUID().toString()+System.nanoTime();
						issueNo = issueNo.replaceAll("[^0-9]", "");

						Date nowDate = new Date();
						SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");						
						String lang = info.getSession("LANGUAGE");
						if("KO".equalsIgnoreCase(lang)) {
							toDateFormat = new SimpleDateFormat(CommonUtil.getConfig("sepoa.seal.audittrail.pubdate.format", "yyyy년 MM월 dd일 HH시 mm분 ss초"));
						}else {
							toDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");			
						}
						String addDate = new SimpleDateFormat("yyyyMMdd").format(nowDate);
						String addTime =  new SimpleDateFormat("HHmmss").format(nowDate);
					
						Map<String, String> docIssueParam = new HashMap<>();
						docIssueParam.put("ADD_DATE", addDate);
						docIssueParam.put("ADD_TIME", addTime);
						docIssueParam.put("ISSUE_NO", issueNo);
						
						doInsertSDOIS(docIssueParam);
						
						templateHtml = templateHtml.replace("#ADD_DATE# #ADD_TIME#", toDateFormat.format(nowDate));
						templateHtml = templateHtml.replace("#PUBLISHED_NO#", issueNo);

						SepoaConverter sc = new SepoaConverter();
						sc.setSource(templateHtml);
						tempFdfFile = sc.getPDF();
						
						if(tempFdfFile == null || !tempFdfFile.exists()) {
							this.errCode = 103342; 
							return null;
						}
						
						pdfBytesArr = FileUtils.readFileToByteArray(tempFdfFile);
					}
				}		
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			File f = new File(tempFdfFile.getAbsolutePath());
			if(f.exists()) {
				try {
					FileUtils.forceDelete(f);
				}catch(Exception e) {
					Logger.err.println(info.getSession("ID"), this, "[FAIL] Audit Trail temporary PDF file delete fail");
				}
			}			
		}

		return pdfBytesArr;
	}
	
	public byte[] makeRegistrationCertificate() throws Exception {
		String templateHtml = null;
		StringBuilder htmls = new StringBuilder();
		byte[] pdfBytesArr = null;
		File tempFdfFile = null;
		try {
			
			File resource = new File (CommonUtil.getConfig("sepoa.seal.sealregist.resoucre"));
			if(resource.exists()) {
				templateHtml = new String(FileUtils.readFileToByteArray(resource), "UTF-8");
				
				//다국어로 치환할 것들 여기서 치환 
				templateHtml = templateHtml
						.replace("#MAIN_HEADER#", Cache.text.get("SEAL_REGIST.MAIN_HEADER"))
						.replace("#HEADER_SUBJECT#", Cache.text.get("SEAL_REGIST.HEADER_SUBJECT"))
						.replace("#REQUEST_TYPE#", Cache.text.get("SEAL_REGIST.REQUEST_TYPE"))
						.replace("#REQUESTER_NAME#", Cache.text.get("SEAL_REGIST.REQUESTER_NAME"))
						.replace("#MANAGE_DEPT#", Cache.text.get("SEAL_REGIST.MANAGE_DEPT"))
						.replace("#REQUEST_DATE#", Cache.text.get("SEAL_REGIST.REQUEST_DATE"))
						.replace("#REQUEST_TIME#", Cache.text.get("SEAL_REGIST.REQUEST_TIME"))
						.replace("#MANAGE_USER#", Cache.text.get("SEAL_REGIST.MANAGE_USER"))
						.replace("#REQUESTER_UDGL#", Cache.text.get("SEAL_REGIST.REQUESTER_UDGL"))
						.replace("#SEAL_INFORMATION#", Cache.text.get("SEAL_REGIST.SEAL_INFORMATION"))
						.replace("#REGIST_INFORMATION#", Cache.text.get("SEAL_REGIST.REGIST_INFORMATION"))
						.replace("#MANAGE_NUMBER#", Cache.text.get("SEAL_REGIST.MANAGE_NUMBER"))
						.replace("#SEAL_ID#", Cache.text.get("SEAL_REGIST.SEAL_ID"))
						.replace("#ELECT_IMAGE#", Cache.text.get("SEAL_REGIST.ELECT_IMAGE"))
						.replace("#PUB_DATE#", Cache.text.get("SEAL_REGIST.PUB_DATE"))
						.replace("#ADD_DATE#", Cache.text.get("SEAL_REGIST.ADD_DATE"))
						.replace("#ADD_TIME#", Cache.text.get("SEAL_REGIST.ADD_TIME"))
						.replace("#PUB_NO#", Cache.text.get("SEAL_REGIST.PUB_NO"))
						.replace("#COMPANY_NAME#", Cache.text.get("SEAL_REGIST.COMPANY_NAME"));
				//인감문서 조회(SEAL)
				List<Map<String, String>> sealList = doSelectSSEAL(docBasicInfo);
				Map<String, String> sealData = null;
				
				if(sealList == null) {
					this.errCode = 103341; //해당 인장이없습니다. (코드 새로 등록);
					return null;
				}else {
					StringJoiner udgl_name = new StringJoiner(", ");
					for(Map<String, String> row : sealList) {
						udgl_name.add(row.get("UDGL_SUBJECT"));
						sealData = row;
					}
					sealData.put("UDGL_SUBJECT", udgl_name.toString());
					//1.날인 이력 문서 헤더영역 생성
					templateHtml = createRegistCertHeader(templateHtml, sealData);

					//2.날인 이력 문서 인감정보 영역 생성 
					templateHtml = templateHtml.replace("#SEAL_INFO_ROWS#", createRegistCertSeal(sealData));
					
					//5.문서 발행 번호 , 발행일 (발행일과 시간은 문서발행관리 테이블과 같아야 함)
					String issueNo = UUID.randomUUID().toString()+System.nanoTime();
					issueNo = issueNo.replaceAll("[^0-9]", "");

					Date nowDate = new Date();
					SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					SimpleDateFormat regDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일");
					String lang = info.getSession("LANGUAGE");
					if("KO".equalsIgnoreCase(lang)) {
						toDateFormat = new SimpleDateFormat(CommonUtil.getConfig("sepoa.seal.audittrail.pubdate.format", "yyyy년 MM월 dd일 HH시 mm분 ss초"));	
					}else {
						toDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");			
					}
					String addDate = new SimpleDateFormat("yyyyMMdd").format(nowDate);
					String addTime =  new SimpleDateFormat("HHmmss").format(nowDate);
				
					Map<String, String> docIssueParam = new HashMap<>();
					docIssueParam.put("ADD_DATE", addDate);
					docIssueParam.put("ADD_TIME", addTime);
					docIssueParam.put("ISSUE_NO", issueNo);
					
					doInsertSDOIS(docIssueParam);
					
					templateHtml = templateHtml.replace("#NOW_DATE#", toDateFormat.format(nowDate));
					templateHtml = templateHtml.replace("#PUBLISHED_NO#", issueNo);
					templateHtml = templateHtml.replace("#COMPANY_NM#", sealData.get("COMPANY_NM"));
					templateHtml = templateHtml.replace("#INFO_NOW_DATE#", regDateFormat.format(nowDate));
					SepoaConverter sc = new SepoaConverter();
					sc.setSource(templateHtml);
					tempFdfFile = sc.getPDF();
					
					if(tempFdfFile == null || !tempFdfFile.exists()) {
						this.errCode = 103342; 
						return null;
					}
					
					pdfBytesArr = FileUtils.readFileToByteArray(tempFdfFile);
				}
				
			}		
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			File f = new File(tempFdfFile.getAbsolutePath());
			if(f.exists()) {
				try {
					FileUtils.forceDelete(f);
				}catch(Exception e) {
					Logger.err.println(info.getSession("ID"), this, "[FAIL] Audit Trail temporary PDF file delete fail");
				}
			}			
		}

		return pdfBytesArr;
	}
	
	public List<Map<String, String>> doSelectTodoAttachDocList(Map<String, String> data) {
		try {
			return this.doSelectSSISDList(data);
		}catch(Exception e) {
			this.errCode = 102501;
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	protected String createCertAuditTrailHeader(String templateHtml, Map<String, String> data, String sealedDate) throws Exception {
		//MK2에서 구현
		return null;
	}
	
	protected String createAuditTrailHeader(String templateHtml, Map<String, String> data, String firstDocNumber, String sealedDate) throws Exception {
		//첫 번째 이미지 base64로 가져오기 위함
		String layer = "REQ_SEAL";
		Map<String, String> appendData = new HashMap<>();
		appendData.put("LAYER", layer);
		
		//파일매니저 초기화
		if(this.fileManager != null) {
			this.fileManager = null;
		}
		//파일 매니저 셋팅
		if(!this.initFileManager(firstDocNumber)) {
			this.errCode = 100001;
			return null;
		}
		
		List<Map<String, String>> docImageList = this.fileManager.doList(appendData, LEVEL.PARTS);
		
		//패스 매니저 생성
		PathManager pm = new PathManager(info, config);
		pm.setLayer(layer);
		String firstDocImagePath = pm.getLayerPath() + docImageList.get(0).get("FILE_NAME_SERVER");
		String base64 = imageToBase64(firstDocImagePath);
		
		SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		String subject = MapUtils.getString(data, "SUBJECT");
		String docId = MapUtils.getString(data, "SMR_NUMBER");
		String reqDate = toDateFormat.format(fromDateFormat.parse(MapUtils.getString(data, "ADD_DATE")));
		String deptName = MapUtils.getString(data, "DEPT_NAME");
		String reqName = MapUtils.getString(data, "ADD_USER_NAME");
		String sealDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new SimpleDateFormat("yyyyMMddHHmmss").parse(sealedDate));
		String reFlag = MapUtils.getString(data, "RE_FLAG");
		
		templateHtml = templateHtml.replace("#SUBJECT#", Matcher.quoteReplacement(subject));
		templateHtml = templateHtml.replace("#FIRST_PAGE_IMAGE_BASE64#", base64);
		templateHtml = templateHtml.replace("#DOC_ID#", docId);
		templateHtml = templateHtml.replace("#REQ_DATE#", reqDate);
		templateHtml = templateHtml.replace("#DEPT_NAME#", deptName);
		templateHtml = templateHtml.replace("#REQ_NAME#", reqName);
		templateHtml = templateHtml.replace("#SEAL_DATE#", sealDate);
		templateHtml = templateHtml.replace("#RE_FLAG#", reFlag);
		return templateHtml;
	}
	
	protected String createCertAuditTrailDocument(List<Map<String, String>> sipdList) throws Exception{
		//MK2에서 구현 예정
		return null;
	}
	
	protected String createAuditTrailDocument(List<Map<String, String>> sipdList) throws Exception{
		String returnHtml = "";

		for(Map<String, String> docInfo : sipdList) {
			String nodeHtml = "<tr><td class=\"left-middle\">#DOCUMENT_NAME#</td><td class=\"left-middle\">#PAGE_COUNT#</td></tr>";
			//문서 장수 뽑아오기 위함
			String docNo = docInfo.get("SMR_NUMBER") +"_"+docInfo.get("SMD_NUMBER");
			String layer = "REQ_SEAL";
			Map<String, String> appendData = new HashMap<>();
			appendData.put("LAYER", layer);
			
			//파일매니저 초기화
			if(this.fileManager != null) {
				this.fileManager = null;
			}
			//파일 매니저 셋팅
			if(!this.initFileManager(docNo)) {
				this.errCode = 100001;
				return null;
			}
			
			List<Map<String, String>> docImageList = this.fileManager.doList(appendData, LEVEL.PARTS);
			
			returnHtml += nodeHtml.replace("#DOCUMENT_NAME#", MapUtils.getString(docInfo, "SUBJECT"))
					.replace("#PAGE_COUNT#", Integer.toString(docImageList.size())+"p");
		}
		return returnHtml;
	}
	
	private String createAuditTrailSeal(List<Map<String, String>> sipdSealList, Map<String, String> siadData) throws Exception{
		String returnHtml = "";

		//중복 SEAL 검사를 위함
		ArrayList<String> sealManageNo = new ArrayList<>();
		String reFlag = siadData.get("RE_FLAG");
		if("E".equals(reFlag)) {
			reFlag = Cache.text.get("SLM_211.ELECTRON_FLAG_1");
		}else if("R".equals(reFlag)){
			reFlag = Cache.text.get("SLM_211.REAL_FLAG_1");
		}
		
		for(Map<String, String> sealInfo : sipdSealList) {
			String sealID = sealInfo.get("SEAL_ID");
			String nodeHtml = "<tr>"
					+ "<td class=\"center-middle\">#SEAL_TYPE#</td>"
					+ "<td class=\"center-middle\">#DEPT_NAME#</td>"
					+ "<td class=\"center-middle\">#USE_TYPE#</td>"
					+ "<td class=\"center-middle\">"
					+ "    <img class=\"seal-image\"src='data:image/png;base64,#SEAL_IMAGE_BASE64#' />"
					+ "    <p style='font-size:0.8em;margin-top:3px;'>"+sealID.substring(0, 11)+"..</p>"
					+ "</td>"
					+ "</tr>";
			
			String manageNumber = sealInfo.get("MANAGE_NUMBER");
			if(sealManageNo.indexOf(manageNumber) < 0) {
				sealManageNo.add(manageNumber);

				//seal 이미지 base64로 가져오기 위함
				SepoaSealManagementManager sm = new SepoaSealManagementManager(info, new Configuration(), ctx);
				sm.setDocBaiscInfo(sealInfo);
				sealInfo.put("MANAGE_DOC_NO", manageNumber);
				Map<String, String> sealImage = sm.doSelectSealImage();
				String sealImagePath = sealImage.get("FILE_PATH");
	
				String base64 = imageToBase64(sealImagePath);
				StringJoiner sealUseName = new StringJoiner(", ");
				
				List<Map<String, String>> sealUseInfoList = sm.doSelectSealUseInfo(sealInfo.get("MANAGE_NUMBER"));
				if(sealUseInfoList != null) {
					if(sealUseInfoList.size() > 0) {
						for(Map<String, String> sealUseRow : sealUseInfoList) {
							sealUseName.add(sealUseRow.get("SUBJECT"));
						}
					}
				}
				returnHtml += nodeHtml.replace("#SEAL_TYPE#", reFlag +" "+ MapUtils.getString(sealInfo, "SEAL_TYPE_NAME", "")).replace("#DEPT_NAME#", MapUtils.getString(sealInfo, "DEPT_NAME", "")).replace("#USE_TYPE#", sealUseName==null ? "" : sealUseName.toString()).replace("#SEAL_IMAGE_BASE64#", base64==null ? "" : base64);
			}
		}

		return returnHtml;
	}
	
	private String createAuditTrailHistory(List<Map<String, String>> pmgtList) throws Exception{
		String returnHtml = "";
		SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		for(Map<String, String> pmgt : pmgtList) {
			String nodeHtml = "<tr><td class=\"center-middle\">#IP#</td><td class=\"center-middle\">#DATE# #TIME#</td><td class=\"left-middle\">#USER_COMPANY_NAME# #USER_NAME#(#USER_ID#)</td><td class=\"left-middle\">#DEPT_NAME# #USER_NAME# #CONTENTS#</td></tr>";
			nodeHtml = nodeHtml.replace("#IP#", MapUtils.getString(pmgt, "IP", ""));
			nodeHtml = nodeHtml.replace("#DATE# #TIME#", toDateFormat.format(fromDateFormat.parse(MapUtils.getString(pmgt, "ADD_DATE") + MapUtils.getString(pmgt, "ADD_TIME"))));
			nodeHtml = nodeHtml.replace("#USER_COMPANY_NAME#", MapUtils.getString(pmgt, "USER_COMPANY_NAME", ""));
			nodeHtml = nodeHtml.replace("#USER_NAME#", MapUtils.getString(pmgt, "USER_NAME", ""));
			nodeHtml = nodeHtml.replace("#USER_ID#", MapUtils.getString(pmgt, "USER_ID", ""));
			nodeHtml = nodeHtml.replace("#DEPT_NAME#", MapUtils.getString(pmgt, "USER_DEPT_NAME", ""));
			nodeHtml = nodeHtml.replace("#CONTENTS#", Cache.text.get("AUDIT_TRAIL.COMMENT_"+MapUtils.getString(pmgt, "ACTION")));
			
			returnHtml += nodeHtml;
		}
		return returnHtml;
	}
	
	protected String createRegistCertHeader(String templateHtml, Map<String, String> data) throws Exception {
		SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat fromTimeFormat =  new SimpleDateFormat("HHmmss");
		SimpleDateFormat toTimeFormat = new SimpleDateFormat("HH시 mm분 ss초");			


		String subject = MapUtils.getString(data, "SUBJECT");
		String saelType = MapUtils.getString(data, "SEAL_TYPE");
		String registDate = toDateFormat.format(fromDateFormat.parse(MapUtils.getString(data, "REGIST_DATE")));
		String registTime = toTimeFormat.format(fromTimeFormat.parse(MapUtils.getString(data, "REGIST_TIME")));
		String userName = MapUtils.getString(data, "USER_NAME");
		String deptName = MapUtils.getString(data, "USER_DEPT");
		String useName = MapUtils.getString(data, "UDGL_SUBJECT");
		
		
		templateHtml = templateHtml.replace("#SUBJECT#", Matcher.quoteReplacement(subject));
		templateHtml = templateHtml.replace("#SEAL_TYPE#", saelType);
		templateHtml = templateHtml.replace("#REGIST_DATE#", registDate);
		templateHtml = templateHtml.replace("#REGIST_TIME#", registTime);
		templateHtml = templateHtml.replace("#USER_NAME#", userName);
		templateHtml = templateHtml.replace("#USER_DEPT#", deptName);
		templateHtml = templateHtml.replace("#UDGL_SUBJECT#", useName);
		
		return templateHtml;
	}
	
	private String createRegistCertSeal(Map<String, String> sealData) throws Exception{
		String returnHtml = "";

		String sealID = sealData.get("SEAL_ID");
		String manageNumber = sealData.get("MANAGE_NUMBER");
		String nodeHtml = "<tr>"
				+ "<td class=\"center-middle\">#MANAGE_NUM#</td>"
				+ "<td class=\"center-middle\">#SEAL_ID#</td>"
				+ "<td class=\"center-middle\">"
				+ "    <img class=\"seal-image\"src='data:image/png;base64,#ELECT_IMG#' />"
				//+ "    <p style='font-size:0.8em;margin-top:3px;'>"+sealID.substring(0, 11)+"..</p>" 
				+ "</td>"
				+ "</tr>";
		

		//seal 이미지 base64로 가져오기 위함
		SepoaSealManagementManager sm = new SepoaSealManagementManager(info, new Configuration(), ctx);
		sm.setDocBaiscInfo(sealData);
		sealData.put("MANAGE_DOC_NO", manageNumber);
		Map<String, String> sealImage = sm.doSelectSealImage();
		String sealImagePath = sealImage.get("FILE_PATH");

		String base64 = imageToBase64(sealImagePath);
		
		returnHtml += nodeHtml.replace("#ELECT_IMG#", base64==null ? "" : base64).replace("#MANAGE_NUM#", manageNumber).replace("#SEAL_ID#", sealID);

		return returnHtml;
	}
	
	//반출된 인감인지 조회
	public String doSearchExtUse(Map<String, String> data){
		String resultData = this.doSearchExtUseList(data);
		if(resultData == null) {
			return null;
		}
			return resultData;
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
	public List<Map<String,String>> doSearchExtUseShow(Map<String, String> data){
		List<Map<String,String>> ShowList = this.doSearchExtUseList_2(data);			
		if(ShowList == null) {
			return null;
		}
			return ShowList;
	}
	//반출된 인감인지 조회
	private List<Map<String, String>> doSearchExtUseList_2(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> ShowList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			return ShowList ;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//법인인감 정보조회
	public String doSearchCorpSeal(Map<String, String>data){
		String resultData = this.doSearchCorpSealData(data);
		if(resultData == null) {
			return null;
		}
			return resultData;
		}
	//법인인감 정보조회
	private String doSearchCorpSealData(Map<String, String>data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			String resultData = ssm.doSelect(data);
			return resultData;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	private List<Map<String, String>> doSelectSSISDList(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> resultData = new SepoaFormater(ssm.doSelect(data)).getListMap();
			return resultData;
		} catch(Exception e) {
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
	
	//교부서류 파일정보 조회 
	private List<Map<String, String>> doSelectFileData(Map<String, String> data){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			
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
	
	//교부서류 복사파일 데이터 삽입 
	protected boolean doInsertSFILE(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	// 결재 후처리 대기열에 추가
	private boolean doAddSAPPP(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doOldSAPPP");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data);
			
			sxp = new SepoaXmlParser(this.getClass());
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	// 현재 세션의 텍스트 가져오기 (Cache.text.get 의 house code가 제대로 세팅되지 않을때)
	public String getThisSessionText(String houseCode, String screenId, String code, String language) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			if (houseCode == null) houseCode = info.getSession("HOUSE_CODE");
			if (language == null) language = info.getSession("LANGUAGE");
			
			map.put("HOUSE_CODE", houseCode);
			map.put("SCREEN_ID", screenId);
			map.put("CODE", code);
			map.put("LANGUAGE", language);
			
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			SepoaFormater sf = new SepoaFormater(ssm.doSelect(map));
			if (sf.getRowCount() == 0) {
				return "";
			} else {
				return sf.getValue("CONTENTS", 0);
			}
	
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return "";
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
	
	//////// ADD  20230110 sos
	
	@Description("캡쳐기능 테스트 2")
	public List<Map<String, String>> doSelectSFMFI(Map<String, String> data) {
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
	
	public List<Map<String, String>> doSelectSFMMT(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			
			return list;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	public List<Map<String, String>> doSelectAttachFileForSfile() {
		return this.doSelectAttachFileForSfile(this.docBasicInfo);
	}
	
	public List<Map<String, String>> doSelectAttachFileForSfile(Map<String, String> data) {
		List<Map<String, String>> resultData = this.doSelectSFILE(data);
		
		if(resultData == null) {
			this.errCode = 105008;
			return null;
		}
		
		return resultData;
	}
	
	private List<Map<String, String>> doSelectSFILE(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> resultData = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultData != null && resultData.size() > 0) {
				return resultData;
			}
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	public Map<String, String> doSaveAttachFileForSfile(){
		return this.doSaveAttachFileForSfile(this.getDocBaiscInfo());
	}
	
	public Map<String, String> doSaveAttachFileForSfile(Map<String, String> data){
		Map<String, String> resultData = this.doSaveSFILE(data);
		
		if(resultData == null) {
			this.errCode = 105008;
			return null;
		}
		
		return resultData;
	}
	
	private Map<String, String> doSaveSFILE(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectAttachFileForSfile(data).get(0);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	public Map<String, String> doSaveReqSealHistory() {
		return this.doSaveReqSealHistory(this.getDocBaiscInfo());
	}
	
	public Map<String, String> doSaveReqSealHistory(Map<String, String> data) {
		Map<String, String> resultData = this.doInertSSIAD_HISTORY(data);
		
		if(resultData == null) {
			this.errCode = 105007;
			return null;
		}
		
		return resultData;
	}
	
	private Map<String, String> doInertSSIAD_HISTORY (Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectReqSealHistory(data).get(0);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	public List<Map<String, String>> doSelectReqSealHistory() {
		return this.doSelectReqSealHistory(this.getDocBaiscInfo());
	}
	
	public List<Map<String, String>> doSelectReqSealHistory(Map<String, String> data) {
		List<Map<String, String>> resultData = this.doSelectSSIAD_HISTORY(data);
		
		if(resultData == null) {
			this.errCode = 105000; 
			return null;
		}
		
		return resultData;
	}
	
	private List<Map<String, String>> doSelectSSIAD_HISTORY(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> resultData = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultData != null && resultData.size() > 0) {
				return resultData;
			}
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	public List<Map<String, String>> doSelectContractHistory() {
		return this.doSelectContractHistory(this.getDocBaiscInfo());
	}
	
	public List<Map<String, String>> doSelectContractHistory(Map<String, String> data) {
		List<Map<String, String>> resultData = this.doSelectCONTRACT_HISTORY(data);
		
		if(resultData == null) {
			this.errCode = 105000; 
			return null;
		}
		
		return resultData;
	}
	
	private List<Map<String, String>> doSelectCONTRACT_HISTORY(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> resultData = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultData != null && resultData.size() > 0) {
				return resultData;
			}
		} catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	// 게약 과거이력 히스토리 삭제
	public boolean doDeleteContractHistory(Map<String, String> data) {
		boolean status = this.doDeleteCONTRACT_HISTORY(data);
		if(status == false) {
			this.errCode = 105000;
		}
		return true;
	}
	
	private boolean doDeleteCONTRACT_HISTORY(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doDelete(data);
			return true;
		} catch (Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	public Map<String, String> doSaveContractHistory() {
		return this.doSaveContractHistory(this.getDocBaiscInfo());
	}
	
	public Map<String, String> doSaveContractHistory(Map<String, String> data) {
		Map<String, String> resultData = this.doInsertCONTRACT_HISTORY(data);
		
		if(resultData == null) {
			this.errCode = 105007;
			return null;
		}
		
		return resultData;
	}
	
	private Map<String, String> doInsertCONTRACT_HISTORY (Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectCONTRACT_HISTORY(data).get(0);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public boolean doConvertDocBeforeSeal(Map<String, String> capHtml, String srcFile, String desFile,String smrNo, String smdNo) throws  IOException {
		List<Map<String, String>> replaceDocList = new ArrayList<Map<String, String>>();
		if ("".equals(smrNo) || smrNo == null || "".equals(smdNo) || smdNo == null) {
			List<Map<String, String>> upDocList = this.doSelectSSIPD(this.docBasicInfo);
			if (upDocList == null) {
				this.errCode = 103327;
				return false;
			}
			
			smrNo = this.docBasicInfo.get("SMR_NUMBER");
			if("R".equals(this.docBasicInfo.get("RE_FLAG"))) {	 // 실물이면 컨버팅하지 않음
				return true;
			}else {
				for (Map<String, String> upDoc : upDocList) {
					Map<String, String> doc = new HashMap<String, String>();
					doc.put("SMR_NUMBER", smrNo);
					String smdNumber = upDoc.get("SMD_NUMBER");
					if (smdNumber.startsWith("NO_DOC")) continue;	// 날인대상문서를 사용하지 않음
					doc.put("SMD_NUMBER", smdNumber);
					replaceDocList.add(upDoc);
				}
			}
		} else {
			Map<String, String> doc = new HashMap<String, String>();
			doc.put("SMR_NUMBER", smrNo);
			doc.put("SMD_NUMBER", smdNo);
			replaceDocList.add(doc);
		}
		
		if (replaceDocList.size() == 0) {
			return true;
		}
		
		for (Map<String, String> replaceDoc : replaceDocList) {
			if (this.fileManager != null) {
				this.destoryFileManager();
			}
			if (!this.initFileManager(replaceDoc.get("SMR_NUMBER") + "_" + replaceDoc.get("SMD_NUMBER"))) {
				this.errCode = 100001;
				return false;
			}
		}
 
		InputStream in = null;
		File pdfile = new File(srcFile);
		try {
			if(pdfile.exists()) {
				in = CipherManager.getCipher().getInputStream(pdfile, false);
			}
		} catch (Exception e) {
			this.errCode = 102411;
			Logger.err.println(this.info.getSession("ID"), this, e);
			return false;
		}

		PDDocument document = PDDocument.load(in);
		Map<String, String> capHtmlMap = capHtml;
		SepoaImageCapture capture = new SepoaImageCapture();
		capture.setEngine(Engine.CHROME);        
		capture.setCssStyle("<style>.contract-item {display:inline-block;position:fixed;background-size:cover;margin:0;padding:0;width:100%; height:100%;font-weight: bold;filter: opacity(0.7) drop-shadow(0 0 0 #6600CC); }</style>");
		
		Map<Integer, Map<Data, String>> data = new HashMap<>();
		for(Entry<String, String> entry : capHtmlMap.entrySet()) {
			String fixHtmlVal = entry.getValue(); // 001 부터 정제된  html 가져옴  capHtml = capHtmlMap
			int keyNo = Integer.parseInt(entry.getKey() ) - 1 ;  // 001 -> 0 , 002 -> 1 , 003 -> 2
			PDPage page = document.getPage(keyNo);  // HTML에 있는 PDF의 해당 페이지 
			float pageW = page.getCropBox().getWidth();
			float pageH = page.getCropBox().getHeight();
			Map<Data, String> capturehtml = new HashMap<>();

			// 캡쳐한 HTML 에 폭, 높이 지정 
			capturehtml.put(SepoaImageCapture.Data.WIDTH, String.valueOf(pageH) );
			capturehtml.put(SepoaImageCapture.Data.HEIGHT, String.valueOf(pageW) );
			capturehtml.put(SepoaImageCapture.Data.HTML, fixHtmlVal);
	
			// 이미지 배경 투명 여부 (기본값 : true)
			capturehtml.put(SepoaImageCapture.Data.TRANSPARENT, "true");
	
			// HTML 페이지 전체화면 캡쳐 여부 (기본값 : false)
			capturehtml.put(SepoaImageCapture.Data.FULL_PAGE, "true");
			data.put(keyNo, capturehtml);
		}
		capture.setData(data);
 
        Map<Integer, byte[]> results = null;
		try {
			results = capture.capture();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
 
		try {
			File resultFile = new File(desFile);				
			for(Entry<Integer, byte[]> result : results.entrySet()) {
				int pageNum = result.getKey();
				byte[] imageData = result.getValue();
			
				// PDF 페이지 가져옴
				PDPage page = document.getPage(pageNum);
				float pageW = page.getCropBox().getWidth();
				float pageH = page.getCropBox().getHeight();
	
				PDRectangle cropBox = page.getCropBox();
				PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageData, String.valueOf(pageNum));
				PDPageContentStream content = new PDPageContentStream(document, page,PDPageContentStream.AppendMode.APPEND, true,true);
	
				int rotate = page.getRotation();  // 페이지가 회전되어 있으면 
				if(rotate > 0) {
		    	  float tx = (cropBox.getLowerLeftX() + cropBox.getUpperRightX()) / 2;
		    	  float ty = (cropBox.getLowerLeftY() + cropBox.getUpperRightY()) / 2;
		    	  Matrix matrix = Matrix.getRotateInstance(Math.toRadians(rotate), 0, 0);
		    	  content.transform(Matrix.getTranslateInstance(tx, ty));
		    	  content.transform(matrix);
			
		    	  Rectangle rectangle = cropBox.transform(matrix).getBounds();
		    	  content.transform(Matrix.getTranslateInstance((float)-rectangle.getWidth() / 2, (float)-rectangle.getHeight() / 2));
		    	  content.drawImage(pdImage, 0, 0, pageW, pageH);
				} else {
		    	  content.drawImage(pdImage, 0, 0, pageW,pageH );
				}
				content.close() ;
			}
			
			try {
				document.save(resultFile);
			} catch(Exception e) {
				this.errCode = 102308;
				e.printStackTrace();
				return false;
			}
			
			if ( document != null ) {
		    	  document.close() ;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	//법인인감 정보조회
	public List<Map<String, String>> doSearchSMI(Map<String, String>data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(data)).getListMap();
			return list;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	// 용도조회
	public List<Map<String, String>> doSelectSUDGL(Map<String, String>data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = null;
			List<Map<String, String>> list = null;
			ssm.setSepoaInfo(info);
			
			sf = new SepoaFormater(ssm.doSelect(data));
			if(sf.getRowCount() > 0) {
				list = sf.getListMap();
				return list;
			}
			
			return null;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	//날인신청 통계조회
	private List<Map<String, String>> SealStatisticsList(Map<String, String> data){
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
	
	//날짜별 통계조회
	private List<Map<String, String>> ByDateStatisticsList(Map<String, String> data){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			return resultList;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	private boolean doCheckDecideReject(Map<String,String>data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			
			List<Map<String, String>> resultList = new SepoaFormater(ssm.doSelect(data)).getListMap();
			if(resultList.size() > 0) {
				return true;
			}
		} catch (Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	public List<Map<String, String>> doSearchReqSealDocManager(String smrNum){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectReqSealDocSealManager");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = null;
			List<Map<String, String>> list = null;
			ssm.setSepoaInfo(info);
			
			Map<String,String> data = new HashMap<String,String>();
			data.put("SMR_NUM", smrNum);
			sf = new SepoaFormater(ssm.doSelect(data));
			if(sf.getRowCount() > 0) {
				list = sf.getListMap();
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}
		return null;
	}
	
	public List<Map<String,String>> doSearchDocRequestor(String smrNum){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectDocRequester");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = null;
			List<Map<String, String>> list = null;
			ssm.setSepoaInfo(info);
			
			Map<String,String> data = new HashMap<String,String>();
			data.put("SMR_NUM", smrNum);
			sf = new SepoaFormater(ssm.doSelect(data));
			if(sf.getRowCount() > 0) {
				list = sf.getListMap();
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}
		return null;
	}
	
	public List<Map<String,String>> doSearchAppNo(String smrNum){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectAppNo");
        	SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
        	ssm.setSepoaInfo(info);
        	SepoaFormater sf = null;
			List<Map<String, String>> list = null;
			
			Map<String,String> data = new HashMap<String,String>();
			data.put("SMR_NUM", smrNum);
			sf = new SepoaFormater(ssm.doSelect(data));
			
			if(sf.getRowCount() > 0) {
				list = sf.getListMap();
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}
		return null;
	}
	
	// 액세스링크 삭제
	public boolean expireAccessLink(String docNumber) {
		List<Map<String, String>> targetList = this.doSelectULNMA(docNumber);
		if(targetList != null && targetList.size() > 0 ) {
			if(!this.doDeleteULNMA(docNumber)) {
				this.errCode = 102010;
				return false;
			}
		}
		
		List<Map<String, String>> privateTarget = this.doSelectULNUS(targetList.get(0));
		if(privateTarget != null && privateTarget.size() > 0) {
			if(!this.doDeleteULNUS(privateTarget.get(0))) {
				this.errCode = 102010;
				return false;
			}
		}
		return true;
	}
	
	// ULNMA 조회
	protected List<Map<String, String>> doSelectULNMA(String docNumber) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectULNMA");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = null;
			List<Map<String, String>> list = null;
			ssm.setSepoaInfo(info);
			
			Map<String,String> data = new HashMap<String,String>();
			data.put("DOC_NUMBER", docNumber);
			sf = new SepoaFormater(ssm.doSelect(data));
			if(sf.getRowCount() > 0) {
				list = sf.getListMap();
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}
		return null;
	}
	
	// ULNMA 삭제
	protected Boolean doDeleteULNMA(String docNumber) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doDeleteULNMA");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(new SingletonMap<String, String>("DOC_NUMBER", docNumber));
			
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}
		return null;
	}
	
	// ULNUS 조회
	protected List<Map<String, String>> doSelectULNUS(Map<String, String> targetData) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doSelectULNUS");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			SepoaFormater sf = null;
			List<Map<String, String>> list = null;
			ssm.setSepoaInfo(info);
			
			sf = new SepoaFormater(ssm.doSelect(targetData));
			if(sf.getRowCount() > 0) {
				list = sf.getListMap();
				return list;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}
		return null;
	}
	
	// ULNMA 삭제
	protected Boolean doDeleteULNUS(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass(), "doDeleteULNMA");
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(data);
			
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			e.printStackTrace();
		}
		return null;
	}
	
	//날인완료 삭제이력
	public boolean doSaveSSCEH(Map<String, String> data) {

		boolean resultData = false;
		resultData = this.doInsertSSCEH(data);
		if(resultData == false) {
			this.errCode = 102010;
			return false;
		}
		return true;
	}
	
	// 날인완료 삭제이력 
	protected boolean doInsertSSCEH(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//날인요청 삭제여부 Y
	public boolean doDeleteReqSealHis(Map<String, String> data) {
		if(!this.doDeleteHisSSIAD(data)) {
			this.errCode = 102418;
			return false;
		}
		
		return true;
	}
	
	//날인요청 서류 삭제여부 Y
	public boolean doDeleteReqSealDocHis(Map<String, String> data) {
		if(!this.doDeleteHisSSIPD(data)) {
			this.errCode = 102310;
			return false;
		}
		
		return true;
	}
	
	//날인요청 서류 인장 삭제여부 Y
	public boolean doDeleteReqSealDocSealHis(Map<String, String> data) {
		if(!this.doDeleteHisSSIPD_SEAL(data)) {
			this.errCode = 102311;
			return false;
		}
		
		return true;
	}
	
	//날인요청 교부서류 삭제여부 Y
	public boolean doDeleteReqSealAttachDocHis(Map<String, String> data) {
		if(!this.doDeleteHisSSISD(data)) {
			this.errCode = 102310;
			return false;
		}
		
		return true;
	}
	
	//날인요청 관리 테이블 데이터 삭제여부 Y
	protected boolean doDeleteHisSSIAD(Map<String, String> data) {
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
	
	//날인서류 관리 테이블 데이터 삭제여부 Y
	protected boolean doDeleteHisSSIPD(Map<String, String> data) {
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
	
	//날인서류에 사용할 인감 관리 테이블 데이터 삭제여부 Y
	protected boolean doDeleteHisSSIPD_SEAL(Map<String, String> data) {
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
	
	//날인요청 교부요청서류 관리 테이블 데이터 삭제여부 Y
	protected boolean doDeleteHisSSISD(Map<String, String> data) {
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
	
	/*
	 * Process 
	 */
	
	//교부서류 히스토리 데이터 조회
	public List<Map<String, String>> doSelectCorpAttachDocHis(Map<String, String> corpAttachDoc){
		List<Map<String, String>> resultData = this.doSelectSSRST(corpAttachDoc);
		
		if(resultData == null) {
			this.errCode = 102320;
			return null;
		}
		
		return resultData;
	}
	
	//교부 서류 히스토리 데이터 저장
	public boolean doSaveCorpAttachDocHis(Map<String, String> cropAttchDoc) {
		// 교부서류 히스토리에 해당 데이터 있는지 확인
		boolean resultData = false;
		if(this.doSelectSSRST(cropAttchDoc) != null) {
			resultData = this.doUpdateSSRST(cropAttchDoc);
		} else {
			resultData = this.doInsertSSRST(cropAttchDoc);
		}
		
		if(resultData == false) {
			this.errCode = 102002;
			return false;
		}
		return true;
	}
	
	//교부 서류 히스토리 데이터 삭제
	public boolean doDeleteCorpAttachDocHis(Map<String, String> cropAttchDoc) {
		boolean status = this.doDeleteSSRST(cropAttchDoc);
		
		if(status == false) {
			this.errCode = 102009;
			return false;
		}
		
		return true;
	}
	
	//교부서류 히스토리 테이블 조회 
	protected List<Map<String, String>> doSelectSSRST(Map<String, String> data) {
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
	
	//교부서류 히스토리 테이블 데이터 적재 
	protected boolean doInsertSSRST(Map<String, String> data) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	//교부서류 히스토리 테이블 업데이트
	protected boolean doUpdateSSRST(Map<String, String> data) {
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
	
	//교부서류 히스토리 테이블 업데이트
	protected boolean doDeleteSSRST(Map<String, String> data) {
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
	
	// 팀장 여부 조회
	public boolean doSelectIsTeamManager() {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(new HashMap<>())).getListMap();
			if (list.size() > 0) {
				return true;
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
}