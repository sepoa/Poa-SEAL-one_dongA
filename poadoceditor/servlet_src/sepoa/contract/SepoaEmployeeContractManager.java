package sepoa.contract;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;

import filters.ThreadLocalFilter;
import sepoa.contract.util.SepoaImageProcessor;
import sepoa.filemanager.FileManager;
import sepoa.filemanager.FileManager.LEVEL;
import sepoa.filemanager.utils.cipher.CipherException;
import sepoa.filemanager.utils.cipher.CipherManager;
import sepoa.filemanager.utils.storage.PathManager;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Config;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.module.SepoaModuleManager;
import sepoa.fw.module.pdf.SepoaPDFDocument;
import sepoa.fw.module.pdf.SepoaPDFModifier;
import sepoa.fw.module.pdf.content.SepoaPDFContent;
import sepoa.fw.module.pdf.content.SepoaPDFContentImage;
import sepoa.fw.module.pdf.pdfbox.SepoaPDFDocumentPDFBox;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.sign.SepoaSign;
import sepoa.fw.sign.SepoaSignFactory;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.util.SepoaJSON;
import sepoa.fw.util.authority.Permission.PrincipalAgentType;
import sepoa.fw.util.authority.SepoaAuthorityManager;
import sepoa.fw.util.authority.SepoaAuthorityObject;
import sepoa.fw.util.authority.SepoaAuthorityObject.Authorized;
import sepoa.fw.util.converter.SepoaConverter;
import sepoa.fw.util.ux.sUtils;
import sepoa.fw.ux.link.SepoaLinkManager;

public class SepoaEmployeeContractManager extends SepoaModuleManager{
	protected FileManager fileManager;
	protected SepoaAuthorityObject  authObject;
	protected SepoaAuthorityManager authManager;
	protected SepoaLinkManager linkManager;
	protected SepoaPushManager pushManager;
	protected boolean checkPermission = true;
	protected boolean historyFlag = true;
	protected boolean linkFlag = true;
	protected final String pluginService = "sepoa.svc.ct.SepoaContractCustomService";
	
	protected static Pattern itemBoxClassNamePattern = Pattern.compile("contract-item-box-code-([a-zA-Z0-9_]{0,})");
	protected static Pattern itemPropertiesClassNamePattern = Pattern.compile("contract-item-prop-([a-zA-Z0-9_]{0,})-([a-zA-Z0-9_/%]{0,})");
	protected static Pattern signItemClassNamePattern = Pattern.compile("contract-item-box-sign-id-(\\w+)");
	
	protected Map<String, String> docBasicInfo;
	
	protected enum ACTION_TYPE{
		CREATE, DOCUMENT_UPDATE, MANUAL_COMPLETE, 
		REVIEW_START, REVIEW_REQUEST, REVIEW_ACCEPT, REVIEW_REJECT, REVIEW_COMPLETE, 
		SIGN_START, SIGN_REQUEST, SIGN_REJECT, SIGN, SIGN_COMPLETE, 
		TERMINATE_START, TERMINATE_REQUEST, TERMINATE_AGREE, TERMINATE_COMPLETE
	}
	
	protected String[] progressImageLayer = new String[] {"G", "C", "E", "B"};
	protected String[] progressFileLayer = new String[] {"G", "C", "E", "B", "A"};
	protected String[] completeFileLayer = new String[] {"CC"};
	
	public SepoaEmployeeContractManager(SepoaInfo info, Config config, ConnectionContext ctx) {
		super(info, config, ctx);
		this.docBasicInfo = new HashMap<String, String>();
	}
	
	/*
	 * Common
	 */
	public void setPushManager(SepoaPushManager pushManager) {
		this.pushManager = pushManager;
	}
	
	protected boolean initFileManager() {
		String docNumber = MapUtils.getString(this.docBasicInfo, "CONT_NO", "") +"_"+ MapUtils.getString(this.docBasicInfo, "CONT_GL_SEQ", "");
		FileManager fm = new FileManager(info, config, this.ctx);
		if (fm.setDocNumber(docNumber, true) == null) {
			return false;
		}else {
			fm.setTempDirectory(this.tempStorage);
			this.fileManager = fm;
			return true;
		}
	}
	
	protected boolean initAuthManager() {
		if(checkPermission) {
			if(this.authManager == null) {
				this.authManager = SepoaAuthorityManager.getManager(info, ctx);
			}
			SepoaAuthorityObject authObject = this.authManager.getObject(
					info.getSession("HOUSE_CODE"), 
					this.docBasicInfo.get("COMPANY_CODE"), 
					"CONT", 
					this.docBasicInfo.get("CONT_NO"), 
					this.docBasicInfo.get("CONT_GL_SEQ"));
			if(authObject == null) {
				return false;
			}else {
				this.authObject = authObject; 
			}
		}
		return true;
	}
	
	protected boolean initLinkManager() {
		if(linkFlag && this.linkManager == null) {
			String contNo = this.docBasicInfo.get("CONT_NO");
			String contGlSeq = this.docBasicInfo.get("CONT_GL_SEQ");
			
			Map<String, String> linkParams = new HashMap<String, String>();
			linkParams.put("CONT_NO", contNo);
			linkParams.put("CONT_GL_SEQ", contGlSeq);
			
			this.linkManager = new SepoaLinkManager(info, config, ctx);
			if(!this.linkManager.newLink("EMP_CONTRACT", contNo, contGlSeq) || !this.linkManager.setParams(linkParams)) {
				this.errCode = this.linkManager.getLastErrCode();
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Document
	 */
	public void setContType(String contType) {
		this.docBasicInfo.put("CONT_TYPE", contType);
	}
	
	public void setLinkFlag(boolean flag) {
		this.linkFlag = flag;
	}
	
	public void setCheckPermission(boolean flag) {
		this.checkPermission = flag;
	}
	
	public Map<String, String> setDocNumber(String docNumber, String seq) {
		return this.setDocNumber(docNumber, seq, false);
	}
	
	public Map<String, String> setDocNumber(String docNumber, String seq, boolean forceCreate) {
		Map<String, String> docInfo = null;
		boolean isNewDocument = false;
		//synchronized (getSyncObject(docNumber)) {
			if(docNumber != null && !docNumber.isEmpty()) {
				this.docBasicInfo.put("CONT_NO"    , docNumber);
				this.docBasicInfo.put("CONT_GL_SEQ", seq);
				docInfo = this.doSelectDocument(this.docBasicInfo);
				if(docInfo == null && forceCreate) {
					docInfo = this.doInsertDocument(this.docBasicInfo);
					isNewDocument = true;
				}
			}
		//}
		if(docInfo == null) {
			this.errCode = 100000;
			return null;
		}
		
		this.docBasicInfo.putAll(docInfo);
		if(!this.initFileManager()) {
			this.errCode = 100001;
			return null;
		}
		
		if(this.linkFlag && !initLinkManager()) {
			this.errCode = 100008;
			return null;
		}
		
		if(this.checkPermission) {
			if(!this.initAuthManager()) {
				this.errCode = 100002;
				return null;
			}else if(isNewDocument){
				try {
					authObject.setOwner(
							info.getSession("COMPANY_CODE"), 
							info.getSession("ID"),
							false);
					initAuthManager();
				} catch (Exception e) {
					this.errCode = 100003;
					Logger.err.println(this.info.getSession("ID"), this, e);
					return null;
				}
				if(!addHistory(ACTION_TYPE.CREATE)) {
					return null;
				}
			}
			
			boolean permissionFlag = false;
			
			List<String> ctrlCodeList = Arrays.asList(this.info.getSession("CTRL_CODE").split(","));
			boolean hasEmpContAllSelect = ctrlCodeList.contains("EMP_CONT_ALL_SELECT");
			
			if(hasEmpContAllSelect) {
				permissionFlag = true;
			} else {
				Map<String, String> optionsMap = new HashMap<>();
				// TODO : 구버전 fw라서 해당 api 미지원 하므로 코드 주석 처리
//				this.callModulePluginService(pluginService, "doBypassPermission", new Object[] {this.docBasicInfo, optionsMap});
				
				if(optionsMap.size() > 0) {
					permissionFlag = "true".equalsIgnoreCase(MapUtils.getString(optionsMap, "BYPASS_PERMISSION", "false"));
				} else {
					permissionFlag = this.authObject.isReadable();
				}
			}
			
			if(permissionFlag == false) {
				this.errCode = 100004;
				return null;
			}
		}
		
		return new HashMap<String, String>(docBasicInfo);
	}
	
	public Map<String, String> getDocumentInfo(){
		Map<String, String> result = new HashMap<String, String>(this.docBasicInfo);
		result.put("IS_WRITABLE", "TRUE");
		// result.put("MAIL_SEND_YN", "N");
		if(this.checkPermission) {
			if(!this.authObject.isWritable()) {
				result.put("IS_WRITABLE", "FALSE");
			}
		}
		return result;
	}
	
	public boolean grantTo(String userCompany, String  userID, Authorized auth) {
		return grantTo(PrincipalAgentType.PERSONAL, userCompany, userID, auth);
	}
	
	public boolean grantTo(PrincipalAgentType type, String userCompany, String  userID, Authorized auth) {
		if(this.checkPermission) {
			try {
				authObject.grant(type, userCompany, userID, auth, null);
			} catch (Exception e) {
				this.errCode = 100003;
				Logger.err.println(this.info.getSession("ID"), this, e);
				// 권한을 이미 소유하고 있는 경우 오류가 발생.
				// 권한부여 실패시 오류를 발생할 가능성은 SQL문등 기반의 문제로 보이기에 
				// 실패의 경우는 권한부여의 오류로 상정하고 권한부여시 오류가 발생하지 않도록 하겠음.
//				return false;
			}
		}
		return true;
	}
	public boolean revokeAll() {
		return revokeFrom(null, null, null);
	}
	public boolean revokeFrom(String userCompany, String  userID) {
		return revokeFrom(userCompany, userID, Authorized.WRITE);
	}
	public boolean revokeFrom(String userCompany, String  userID, Authorized auth) {
		if(this.checkPermission) {
			try {
				if(auth == null) {
					authObject.revokeAll(false);
				}else {
					switch(auth) {
					case WRITE:
						authObject.grant(PrincipalAgentType.PERSONAL, userCompany, userID, Authorized.READ, null);
						break;
					case READ:
					default:
						authObject.revoke(PrincipalAgentType.PERSONAL, userCompany, userID);
						break;
					}
				}
			} catch (Exception e) {
				this.errCode = 100005;
				Logger.err.println(this.info.getSession("ID"), this, e);
				return false;
			}
		}
		return true;
	}
	
	public boolean deleteDocument(boolean delFlag) {
		String nowStatus = this.docBasicInfo.get("STATUS");
		List<String> statusList = new ArrayList<String>();
		statusList.add("EW");
		statusList.add("EF");
		statusList.add("T");
		
		if (!statusList.contains(nowStatus)) {
			this.errCode = 100011;
			return false;
		}
		
		Map<String, String> data = new HashMap<String, String>(this.docBasicInfo);
		
		if (delFlag) {
			data.put("DEL_FLAG" , "Y");
			
			if (!this.doUpdate(data)) {
				this.errCode = 100013;
				return false;
			}
		} else {
			this.fileManager.doDelete();
			
			if(!this.doDeleteSigner()) {
				this.errCode = 100601;
				return false;
			}
			
			if(!this.doDeleteSCTMD(this.docBasicInfo)) {
				this.errCode = 100014;
				return false;
			}
			
			if(!this.revokeAll()) {
				return false;
			}
			
			if(!this.doDeleteDocument(data)) {
				this.errCode = 100013;
				return false;
			}
		}
		
		this.docBasicInfo = data;

		return true;
	}
	
	/*
	 * Termination
	 */
	public boolean doTermination(boolean isForce) {
		if(!"A".equals(this.docBasicInfo.get("STATUS")) && !"K".equals(this.docBasicInfo.get("STATUS"))) {
			this.errCode = 100101;
			return false;
		}
		
		List<Map<String, String>> agreeTargetList = this.doSelectSigner();
		if(agreeTargetList == null) {
			this.errCode = 100103;
			return false;
		}
		
		List<Map<String, String>> signerList = doSelectSigner();
		List<String> messageSignerContents = new ArrayList<String>(signerList.size());
		if(signerList != null) {
			for(int i=0;i<signerList.size(); i++) {
				Map<String, String> signer = signerList.get(i);
				messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
			}
		}
		
		String messageContent1 = StringUtils.join(messageSignerContents, "<br/>");
		for(Map<String, String> nextSigner : agreeTargetList) {
			this.linkManager.setActive(nextSigner.get("SIGNER_COMPANY_CODE"), nextSigner.get("SIGNER_ID"));
			this.linkManager.setUserParams(new SingletonMap<>("CONT_MODULE", "COMPLETE"));
			
			//create message params
			Map<String, String> messageParams = this.getMessageParams("TERMINATION_COMPLETE", nextSigner);
			messageParams.put("CONTENTS_1", messageContent1);
			
			Map<String, String> tmp = new HashMap<>();
			tmp.putAll(this.docBasicInfo);
			tmp.putAll(nextSigner);
			for(Entry<String, String> entry : messageParams.entrySet()) {
				messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
			}
			
			//create message
			SepoaMessage message = new SepoaMessage();
			message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
			message.setSepoaInfo(info);
			message.addParams(messageParams);
			message.addReceiver(nextSigner.get("SIGNER_ID"), nextSigner.get("SIGNER_NAME"), nextSigner.get("SIGNER_EMAIL"), null);
			
			//add message
			if(this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
		}
		
		if(!doUpdateStatus(isForce?"Z":"X")) {
			return false;
		}
		if(!addHistory(ACTION_TYPE.TERMINATE_COMPLETE)) {
			return false;
		}
		return true;
	}
	
	public boolean doTerminationAgree(String companyCode, String userID) {
		Map<String, String> condition = new HashMap<String, String>();
		condition.put("SIGNER_COMPANY_CODE", companyCode);
		condition.put("SIGNER_ID"          , userID);
		condition.put("TERMINATION_STATUS" , "M");
		List<Map<String, String>> terminatorList = this.doSelectSCTDS(this.docBasicInfo, condition);
		if(terminatorList == null || terminatorList.size() == 0) {
			this.errCode = 100104;
			return false;
		}
		Map<String, String> terminator = terminatorList.get(0);
		terminator.put("TERMINATION_STATUS", "S");
		if(!this.doUpdateSCTDS(this.docBasicInfo, terminator)) {
			this.errCode = 100105;
			return false;
		}
		if(!addHistory(ACTION_TYPE.TERMINATE_AGREE)) {
			return false;
		}
		
		condition = new HashMap<String, String>();
		condition.put("TERMINATION_STATUS" , "M");
		List<Map<String, String>> waitingList = this.doSelectSCTDS(this.docBasicInfo, condition);
		if(waitingList == null || waitingList.size() <= 0) {
			return doTermination(false);
		}
		return true;
	}
	
	public boolean doTerminationStart() {
		if(!"A".equals(this.docBasicInfo.get("STATUS"))) {
			this.errCode = 100101;
			return false;
		}
		
		if(!addHistory(ACTION_TYPE.TERMINATE_START) || !addHistory(ACTION_TYPE.TERMINATE_REQUEST)) {
			return false;
		}
		
		List<Map<String, String>> signerList = doSelectSigner();
		List<String> messageSignerContents = new ArrayList<String>(signerList.size());
		if(signerList != null) {
			for(int i=0;i<signerList.size(); i++) {
				Map<String, String> signer = signerList.get(i);
				messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
			}
		}
		
		String docCompanyCode = this.docBasicInfo.get("COMPANY_CODE");
		String messageContent1 = StringUtils.join(messageSignerContents, "<br/>");
		
		for(Map<String, String> nextSigner : signerList) {
			if(docCompanyCode.equals(nextSigner.get("SIGNER_COMPANY_CODE"))){
				nextSigner.put("TERMINATION_STATUS", "S");
			}else {
				nextSigner.put("TERMINATION_STATUS", "M");
				
				if(!this.grantTo(
						nextSigner.get("SIGNER_COMPANY_CODE"), 
						nextSigner.get("SIGNER_ID"), 
						Authorized.WRITE)) {
					return false;
				}
				this.linkManager.setActive(nextSigner.get("SIGNER_COMPANY_CODE"), nextSigner.get("SIGNER_ID"));
				this.linkManager.setUserParams(new SingletonMap<>("CONT_MODULE", "TERMINATION"));
				
				//create message params
				Map<String, String> messageParams = this.getMessageParams("TERMINATION", nextSigner);
				messageParams.put("CONTENTS_1", messageContent1);
				
				Map<String, String> tmp = new HashMap<>();
				tmp.putAll(this.docBasicInfo);
				tmp.putAll(nextSigner);
				for(Entry<String, String> entry : messageParams.entrySet()) {
					messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
				}
				
				//create message
				SepoaMessage message = new SepoaMessage();
				message.setMessageType("POA_CONTRACT_WITH_BUTTON");
				message.setSepoaInfo(info);
				message.addParams(messageParams);
				message.addReceiver(nextSigner.get("SIGNER_ID"), nextSigner.get("SIGNER_NAME"), nextSigner.get("SIGNER_EMAIL"), null);
				
				//add message
				if(this.pushManager.addMessage(message)) {
					Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
				}
			}
		}
		
		if(!this.doInsertSCTDS(signerList)) {
			this.errCode = 100102;
			return false;
		}
		
		if(!doUpdateStatus("K")) {
			return false;
		}
		
		return true;
	}
	
	public List<Map<String, String>> doSelectTerminator(){
		return doSelectSCTDS(this.docBasicInfo);
	}
	
	public boolean doUpdateTerminatior(String message) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("T_ID", info.getSession("ID"));
		data.put("T_NAME", info.getSession("NAME_LOC"));
		data.put("T_IP", ThreadLocalFilter.getClientIP());
		data.put("T_MESSAGE", message);
		
		return this.doUpdate(data);
	}
	
	/*
	 * Alteration
	 */
	public boolean doSelectAlterationDocument() {
		if(!"A".equals(this.docBasicInfo.get("STATUS"))) {
			this.errCode = 100020;
			return false;
		}
		
		Map<String, String> nextCtglData = new HashMap<String, String>(this.docBasicInfo);
		String nextSeq = String.valueOf(Integer.parseInt(this.docBasicInfo.get("CONT_GL_SEQ")) + 1);
		nextCtglData.put("CONT_GL_SEQ", nextSeq);
		Map<String, String> alterationData = this.doSelectDocument(nextCtglData);
		
		if(alterationData != null) {
			this.errCode = 100030;
			return false;
		}
		
		return true;
	}
	
	public Map<String, String> doCopyDocument() {
		return null;
	}
	
	public List<Map<String, String>> doListImage(){
		String[] layers = null;
		if("A".equals(this.docBasicInfo.get("STATUS"))) {
			layers = this.completeFileLayer;
		}else {
			layers = this.progressImageLayer;
		}
		
		List<Map<String, String>> result = new LinkedList<Map<String, String>>();
		for(String layer : layers) {
			List<Map<String, String>> layerFile = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_"+layer), LEVEL.FILE);
			if(layerFile != null) {
				result.addAll(layerFile);
			}
		}
		if (result.size() == 0) {
			this.errCode = 100101;
		} 
		return result;
	}
	
	public Map<String, String> doImage(String fileID, String partID, boolean thumb, boolean noImageFlag){
		if(fileID.isEmpty()) {
			this.errCode = 100102;
			return null;
		}else {
			List<String> fileList = new ArrayList<String>();
			fileList.add(fileID);
			
			Map<String, String> result = this.fileManager.doDownload(fileList, true);
			if(result == null) {
				Logger.err.println(info.getSession("ID"), this, "not found image : "+this.fileManager.getLastErrCode());
				this.errCode = 100103;
				return null;
			}
			return result;
		}
	}
	
	public boolean doUpdate(Map<String, String> data) {
		Map<String, String> updateData = new HashMap<String, String>(this.docBasicInfo);
		updateData.putAll(data);
		if(!this.doUpdateDocument(updateData)) {
			this.errCode = 100204;
			return false;
		}
		this.docBasicInfo.putAll(updateData);
		return true;
	}
	
	public boolean doUpdateForm(String formNo, String extFormNos) {
		String beforeFormNo = this.docBasicInfo.get("CONT_FORM_NO");
		if(formNo.equals(beforeFormNo)) {
			return true;
		}
		
		List<Map<String, String>> fileList = this.doListImage();
		if(fileList != null && fileList.size() > 0 && "".equals(beforeFormNo)) {
			String docNumber = formNo + "_1";
			FileManager fm = new FileManager(info, config, this.ctx);
			if (fm.setDocNumber(docNumber, true) == null) {
				return false;
			}
			Map<String, String> fixHtmlMap = new HashMap<>();
			List<Map<String, String>> templateFileList = fm.doList();
			if(templateFileList != null && templateFileList.size() > 0) {
				Collections.sort(templateFileList, Comparator.comparing(file -> file.get("DOC_SEQ")));
				List<File> tempFileList = new ArrayList<>();
				try {
					int prevPages = 0;
					PathManager pm = new PathManager(info, config);
					for(Map<String, String> file : templateFileList) {
						Map<String, String> fixHtml = new Gson().fromJson(file.get("FIX_HTML"), Map.class);
						for(Entry<String, String> entry : fixHtml.entrySet()) {
							String idx = StringUtils.leftPad(String.valueOf(prevPages + Integer.parseInt(entry.getKey())), 3, "0");
							fixHtmlMap.put(idx, entry.getValue());
						}
						
						if(file.get("LAYER").contains("DUMMY")) {
							prevPages += fixHtml.size();
						} else {
							File target = CipherManager.decrypt(pm.getFile(file), false);
							tempFileList.add(target);
							SepoaPDFDocumentPDFBox document = (SepoaPDFDocumentPDFBox) SepoaPDFDocument.load(target);
							prevPages += document.getPDDocument().getNumberOfPages();
							document.close();
						}
					}
					
					prevPages = 0;
					List<Map<String, String>> editorHtmls = new ArrayList<>();
					for(Map<String, String> file : fileList) {
						String fileId = file.get("FILE_ID");
						String layer = file.get("LAYER");
						File target = CipherManager.decrypt(pm.getFile(file), false);
						tempFileList.add(target);
						SepoaPDFDocumentPDFBox document = (SepoaPDFDocumentPDFBox) SepoaPDFDocument.load(target);
						int pageNumbers = document.getPDDocument().getNumberOfPages();
						document.close();
						
						for(int i = 1; i <= pageNumbers; i++) {
							String idx = StringUtils.leftPad(String.valueOf(prevPages + i), 3, "0");
							String fixHtml = MapUtils.getString(fixHtmlMap, idx, null);
							
							if(fixHtml != null) {
								Map<String, String> html = new HashMap<>();
								html.put("FIX_HTML", fixHtml);
								html.put("FILE_ID", fileId);
								html.put("PART_ID", StringUtils.leftPad(String.valueOf(i), 3, "0"));
								html.put("LAYER", layer);
								editorHtmls.add(html);
							}
						}
						
						prevPages += pageNumbers;
					}
					
					if(editorHtmls.size() > 0) {
						this.doUpdateEditorHTML(editorHtmls);
					}
					
				} catch(Exception e) {
					e.printStackTrace();
					Logger.err.println(e.getMessage());
				} finally {
					try {
						for(File f : tempFileList) {
							if(f.exists()) {
								FileUtils.moveFileToDirectory(f, new File(this.tempStorage), false);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
						Logger.err.println(e.getMessage());
					}
				}
			}
		} else {
			this.errCode = 0;
			
			this.doDeleteFile();
			this.fileManager.doCopy(formNo + "_1", "");
			if(this.fileManager.getLastErrCode() != 0) {
				this.errCode = 100202;
				return false;
			}
			
			fileList = this.fileManager.doList();
			for(Map<String, String> file : fileList) {
				String layer = file.get("LAYER");
				if(layer.contains("DUMMY")) {
					String docSeq = file.get("DOC_SEQ");
					String fileName = file.get("FILE_NAME_CLIENT");
					String fixHtml = file.get("FIX_HTML");
					
					Map<String, String> fixHtmlMap = new Gson().fromJson(fixHtml, Map.class);
					int maxPageNum = Integer.parseInt(fixHtmlMap.keySet().stream().max(Comparator.comparing(e -> Integer.parseInt(e))).get());
					
					String pdfPath = this.tempStorage+"/"+String.valueOf(System.nanoTime())+".pdf";
					File pdfFile = new File(pdfPath);
					try(PDDocument document = new PDDocument();) {
						for(int i = 0; i < maxPageNum; i++) {
							document.addPage(new PDPage(PDRectangle.A4));
						}
						document.save(pdfFile);
						document.close();
					} catch (IOException e) {
						Logger.err.println(e.getMessage());
					}
					
					this.fileManager.doDelete(file.get("FILE_ID"));
					
					Map<String, String> uploadFileData = new HashMap<String, String>();
					uploadFileData.put("PATH", pdfFile.getAbsolutePath());
					uploadFileData.put("FILE_NAME_CLIENT", fileName);
					uploadFileData.put("DOC_SEQ", docSeq);
					
					this.fileManager.setLayer("CONT_C");
					List<Map<String, String>> result = this.fileManager.doUploadFile(uploadFileData, null, false);
					if(fileManager.getLastErrCode() != 0) {
						this.errCode = 100401;
						return false;
					}
					
					Map<String, String> meta = new HashMap<>();
					meta.put("FILE_ID", result.get(0).get("FILE_ID"));
					meta.put("LAYER", "CONT_C");
					meta.put("FIX_HTML", fixHtml);
					
					this.fileManager.doModifyMeta(meta);
					int errCode = this.fileManager.getLastErrCode();
					if(errCode != 0) {
						this.errCode = 100204;
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	public Map<String, String> doSelectItem() {
		List<Map<String, String>> list = this.doSelectSCTMD(this.docBasicInfo);
		if(list == null) {
			this.errCode = 100205;
			return null;
		}

		Map<String, String> mappingData = new HashMap<String, String>();
		for(int i=0;i<list.size();i++) {
			mappingData.put(list.get(i).get("ITEM_CODE"), list.get(i).get("ITEM_VALUE"));
		}
		return mappingData;
	}
	
	public boolean doUpdateItem(Map<String, String> docItemMap) {
		if(docItemMap.size() > 0) {
			List<Map<String, String>> docItemList = new ArrayList<Map<String, String>>(docItemMap.size());
			for(Entry<String, String> entry : docItemMap.entrySet()) {
				Map<String, String> docItem = new HashMap<String, String>();
				docItem.put("ITEM_CODE", entry.getKey());
				docItem.put("ITEM_VALUE", entry.getValue());
				docItemList.add(docItem);
			}
			
			if(!this.doDeleteSCTMD(this.docBasicInfo)) {
				this.errCode = 100201;
				return false;
			}
			
			if(!this.doInsertSCTMD(docItemList, this.docBasicInfo)) {
				this.errCode = 100201;
				return false;
			}
		}
		return true;
	}
	
	public boolean doUpdateEditorHTML(List<Map<String, String>> editorHtmls) {
		Map<String, Map<String, String>> fileMetaDataMap = new HashMap<>();
		Map<String, Map<String, String>> fileFixHtmlMap = new HashMap<>();
		
		List<Map<String, String>> fileList = this.doListImage();
		for(Map<String, String> file : fileList) {
			String fileId = MapUtils.getString(file, "FILE_ID");
			if(!fileMetaDataMap.containsKey(fileId)) {
				fileMetaDataMap.put(fileId, new HashMap<>());
			}
			
			if(!fileFixHtmlMap.containsKey(fileId)) {
				fileFixHtmlMap.put(fileId, new HashMap<>());
			}
			
			Map<String, String> fileMetaData = fileMetaDataMap.get(fileId);
			if(!fileMetaData.containsKey("LAYER")) {
				fileMetaData.put("LAYER", MapUtils.getString(file, "LAYER"));
			}
		}
		
		for(Map<String, String> page : editorHtmls) {
			String fileId = MapUtils.getString(page, "FILE_ID");
			Map<String, String> fileMetaData = fileMetaDataMap.get(fileId);
			if(page.containsKey("FILE_TYPE")) {
				fileMetaData.put("FILE_TYPE", MapUtils.getString(page, "FILE_TYPE"));
			}
			
			Map<String, String> pageFixHtml = fileFixHtmlMap.get(fileId);
			pageFixHtml.put(MapUtils.getString(page, "PART_ID"), MapUtils.getString(page, "FIX_HTML", ""));
		}
		
		for(Entry<String, Map<String, String>> entry : fileFixHtmlMap.entrySet()) {
			String fileId = entry.getKey();
			String layer = fileMetaDataMap.get(fileId).get("LAYER");
			String fileType = MapUtils.getString(fileMetaDataMap.get(fileId), "FILE_TYPE", null);
			
			Map<String, String> fileFixHtml = entry.getValue();
			Map<String, String> meta = new HashMap<>();
			meta.put("FILE_ID", fileId);
			meta.put("LAYER", layer);
			meta.put("FIX_HTML", new Gson().toJson(fileFixHtml));
			if(fileType != null) {
				meta.put("FILE_TYPE", fileType);
			}
			
			this.fileManager.doModifyMeta(meta);
			int errCode = this.fileManager.getLastErrCode();
			if(errCode != 0) {
				this.errCode = 100204;
				return false;
			}
		}
		
		return true;
	}
	
	public boolean doUpdateStatus(String status) {
		return doUpdateStatus(status, new HashMap<String, String>());
	}
	
	public boolean doUpdateStatus(String status, Map<String, String> appendData) {
		if(status == null || status.isEmpty()) {
			this.errCode = 100301;
			return false;
		}
		
		Map<String, String> data = new HashMap<String, String>();
		data.putAll(this.docBasicInfo);
		data.putAll(appendData);
		data.put("STATUS", status);
		
		if(!doUpdateDocument(data)) {
			this.errCode = 100302;
			return false;
		}
		this.docBasicInfo.put("STATUS", status);
		return true;
	}
	
	public boolean doMappingData(){
		return this.doMappingData(null);
	}
	
	@SuppressWarnings("unchecked")
	public boolean doMappingData(Map<String, Object> extra) {
		Map<String, String> data = new HashMap<String, String>(this.docBasicInfo);
		List<String> signerNoList = new ArrayList<>();
		List<Map<String, String>> signerList = this.doSelectSigner();
		for(Map<String, String> signer : signerList) {
			String signerNo = signer.get("SIGNER_NO");
			if(!signerNoList.contains(signerNo)); {
				signerNoList.add(signerNo);
			}
			
			for(Entry<String, String> entry : signer.entrySet()) {
				data.put("SIGN_ITEM_"+entry.getKey()+"_"+signerNo, entry.getValue());
			}
		}
		
		if(extra != null) {
			Map<String, String> extraDocInfoData = MapUtils.getMap(extra, "DOC_INFO_DATA");
			for(Entry<String, String> entry : extraDocInfoData.entrySet()) {
				data.put(entry.getKey(), entry.getValue());
			}
			
			List<Map<String, String>> extraSignerDataList = (List<Map<String, String>>) MapUtils.getObject(extra, "SIGNER");
			for(Map<String, String> signer : extraSignerDataList) {
				String signerNo = signer.get("SIGNER_NO");
				for(Entry<String, String> entry : signer.entrySet()) {
					data.put("SIGN_ITEM_"+entry.getKey()+"_"+signerNo, entry.getValue());
				}
			}
		}
		
		List<Map<String, String>> cmList = this.doSelectSCTMD(this.docBasicInfo);
		for(Map<String, String> cmData : cmList) {
			data.put(cmData.get("ITEM_CODE"), cmData.get("ITEM_VALUE"));
		}
		
		List<Map<String, String>> fileList = this.doListImage();
		List<Map<String, String>> updateFileList = new ArrayList<Map<String, String>>();
		for(Map<String, String> file : fileList){
			String mergeFixHtml = MapUtils.getString(file, "FIX_HTML", "");
			if(!StringUtils.isBlank(mergeFixHtml)) {
				Map<String, String> fixHtmlMap = new Gson().fromJson(mergeFixHtml, Map.class);
				if(fixHtmlMap == null) {
					fixHtmlMap = new HashMap<>();
				}
				
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					Document doc = Jsoup.parse(entry.getValue());
					List<String> templateSignerNoList = new ArrayList<>();
					Elements signItemList = doc.select("[class~=contract-item-box-sign-id-]");
					for(Element signItem : signItemList) {
						String classList = signItem.className();
						Matcher m = signItemClassNamePattern.matcher(classList);
						if(m.find()) {
							String signerNo = m.group(1);
							if(!templateSignerNoList.contains(signerNo)) {
								templateSignerNoList.add(signerNo);
							}
						}
					}
					
					templateSignerNoList.sort(Comparator.comparing(id -> id));
					
					Elements itemList = doc.select(".contract-item.contract-item-box");
					boolean mappingFlag = false;
					for(Element item : itemList) {
						String itemValue = null;
						
						String classList = item.className();
						Matcher signNoMatcher = signItemClassNamePattern.matcher(classList);
						if(signNoMatcher.find()) {
							String signerNo = signNoMatcher.group(1);
							if(signerNoList.size() == templateSignerNoList.size()) {
								int index = templateSignerNoList.indexOf(signerNo);
								if(index >= 0 && (index + 1) <= signerNoList.size()) {
									classList = classList.replaceAll(signerNo, signerNoList.get(index));
								}
							}
						}
						
						Matcher m = itemBoxClassNamePattern.matcher(classList);
						if(m.find()) {
							String itemCode = m.group(1);
							itemValue = data.get(itemCode);
						}
						
						if(!StringUtils.isBlank(itemValue)) {
							Elements selectedInputElements = item.select("textarea.contract-item-input");
							Elements originInputElements = item.select(".contract-item-input-origin");
							if(selectedInputElements.size() > 0) {
								Element input = selectedInputElements.first();
								Element originInput = originInputElements.first();
								Map<String, String> itemProps = new HashMap<String, String>();
								for(String inputClassName : input.classNames()) {
									Matcher propMatcher = itemPropertiesClassNamePattern.matcher(inputClassName);
									if(propMatcher.find()) {
										itemProps.put(propMatcher.group(1), propMatcher.group(2));
									}
								}
								
								if(itemProps.containsKey("date_format")) {
									String format = itemProps.get("date_format");
									itemValue = sUtils.date.toString(sUtils.date.fromString(itemValue, "%Y%m%d"), format);
								}else if(itemProps.containsKey("number_format")) {
									String format = itemProps.get("number_format");
									itemValue = sUtils.number.format(itemValue, format);
								}
								
//								input.appendText(itemValue);
								input.text(itemValue);
								originInput.text(itemValue);
								
								item.addClass("contract-item-box-auto-mapping");
								mappingFlag = true;
							}
						}
					}
					if(mappingFlag) {
						fixHtmlMap.put(entry.getKey(), doc.body().html());
					}
				}
				
				Map<String, String> updateFileInfo = new HashMap<String, String>(file);
				updateFileInfo.put("FIX_HTML", new Gson().toJson(fixHtmlMap));
				updateFileList.add(updateFileInfo);
			}
		}
		
		if(updateFileList.size() > 0) {
			List<Map<String, String>> editorHtmlList = new ArrayList<>();
			for(Map<String, String> file : updateFileList) {
				if(file.containsKey("FIX_HTML")) {
					Map<String, String> fixHtml = new Gson().fromJson(file.get("FIX_HTML"), Map.class);
					for(Entry<String, String> entry : fixHtml.entrySet()) {
						Map<String, String> editorHtml = new HashMap<>();
						editorHtml.put("FILE_ID", file.get("FILE_ID"));
						editorHtml.put("LAYER", file.get("LAYER"));
						editorHtml.put("PART_ID", entry.getKey());
						editorHtml.put("FIX_HTML", entry.getValue());
						
						if(file.containsKey("FILE_TYPE")) {
							editorHtml.put("FILE_TYPE", file.get("FILE_TYPE"));
						}
						
						editorHtmlList.add(editorHtml);
					}
				}
			}
			
			this.doUpdateEditorHTML(editorHtmlList);
		}
		
		return true;
	}
	/*
	 * File
	 */
	public List<Map<String, String>> doListFile(){
		String[] layers = progressFileLayer;
		
		List<Map<String, String>> result = new LinkedList<Map<String, String>>();
		for(String layer : layers) {
			List<Map<String, String>> layerImage = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_"+layer), LEVEL.FILE);
			if(layerImage != null) {
				result.addAll(layerImage);
			}
		} 
		if (result.size() <= 0) {
			this.errCode = 100104;
		} 
		return result;
	}
	
	public Map<String, String> doAddFile(String fileType, Map<String, String> file, boolean copyMode){
		return doAddFile(fileType, file, new HashMap<String, String>(file), copyMode);
	}
	public Map<String, String> doAddFile(String fileType, Map<String, String> file, Map<String, String> extraData){
		return doAddFile(fileType, file, extraData, false);
	}
	public Map<String, String> doAddFile(String fileType, Map<String, String> file, Map<String, String> extraData, boolean copyMode) {
		if(file.containsKey("BASE64")) {
			String base64String = file.get("BASE64");
			byte[] byteArray = DatatypeConverter.parseBase64Binary(base64String);
			String filePath = this.tempStorage+"/"+String.valueOf(System.nanoTime())+RandomStringUtils.random(5, true, true);
			File sourceFile = new File(filePath);
			try {
				FileUtils.writeByteArrayToFile(sourceFile, byteArray);
			} catch (IOException e) {
				this.errCode = 100402;
				return null;
			}
			file.put("PATH", filePath);
		}
		
		fileManager.setLayer("CONT_" + fileType.toUpperCase());
		
		List<Map<String, String>>  storedFileList = null;
		if(copyMode) {
			storedFileList = fileManager.doUploadFileFormPath(file, extraData);
		}else {
			storedFileList = fileManager.doUploadFile(file, extraData, false);
		}
		
		if(fileManager.getLastErrCode() != 0) {
			this.errCode = 100401;
			return null;
		}
		
		return fileManager.doList(storedFileList.get(0)).get(0);
	}
	public boolean doDeleteFile() {
		return doDeleteFile(true, null);
	}
	
	public boolean doDeleteFile(String fileID) {
		return doDeleteFile(false, fileID);
	}
	
	protected boolean doDeleteFile(boolean allFlag, String fileID) {
		if(!allFlag && (fileID == null || fileID.isEmpty())) {
			this.errCode = 100501;
			return false;
		}
		this.fileManager.doDelete(fileID);
		int errCode = this.fileManager.getLastErrCode();
		if(errCode !=0) {
			this.errCode = errCode;
			return false;
		}
		return true;
	}
	
	public boolean doUpdateFileName(String fileID, String newFileName) {
		if(fileID == null || fileID.isEmpty()) {
			this.errCode = 100501;
			return false;
		}
		if(newFileName == null || newFileName.isEmpty()) {
			this.errCode = 100502;
			return false;
		}
		
		this.fileManager.doModifyFileName(fileID, newFileName);
		if(this.fileManager.getLastErrCode() != 0) {
			this.errCode = 100503;
			return false;
		}
		return true;
	}
	
	
	public Map<String, String> doDocumentDownload(){
		String[] layers = null;
		boolean originFlag = false;
		
		if(!"A".equals(this.docBasicInfo.get("STATUS"))) {
			layers = progressFileLayer;
		}else {
			originFlag = true;
			layers = new String[] {"G", "CC", "CZ", "B", "A"};
		}
		
		List<String> fileList = new LinkedList<String>();
		for(String layer : layers) {
			List<Map<String, String>> layerFiles = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_"+layer), LEVEL.FILE);
			if(layerFiles != null) {
				for(Map<String, String> file : layerFiles) {
					fileList.add(file.get("FILE_ID"));
				}
			}
		}
		
		if(fileList.size() <= 0) {
			this.errCode = 100511;
			return null;
		}else{
			Map<String, String> result = this.fileManager.doDownload(fileList, originFlag);
			if(result == null) {
				this.errCode = 100512;
				return null;
			}else {
				String ext = null;
				if(originFlag) {
					ext = "."+FilenameUtils.getExtension(result.get("FILE_NAME_CLIENT"));
				}else {
					ext = result.get("FILE_NAME_CLIENT").endsWith("zip")?".zip":".pdf";
				}
				result.put("FILE_NAME_CLIENT", this.docBasicInfo.get("CONT_SUBJECT")+ext);
			}
			return result;
		}
	}

	public Map<String, String> doDocumentFileDownload(String layer, String seq){
		boolean originFlag = true;
		List<String> fileList = new LinkedList<String>();
		
		List<Map<String, String>> layerFiles = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_"+layer), LEVEL.FILE);
		if(layerFiles != null) {
			for(Map<String, String> file : layerFiles) {
				if(seq == null) {
					fileList.add(file.get("FILE_ID"));
				} else if(seq.equals(String.valueOf(layerFiles.indexOf(file) + 1))) {
					fileList.add(file.get("FILE_ID"));
				}
			}
		}
		
		if(fileList.size() <= 0) {
			this.errCode = 100511;
			return null;
		}else{
			Map<String, String> result = this.fileManager.doDownload(fileList, originFlag);
			if(result == null) {
				this.errCode = 100512;
				return null;
			}
			
			return result;
		}
	}
	
	/*
	 * Signer
	 */
	public List<Map<String, String>> doSelectSigner(){
		return doSelectSSIGN(this.docBasicInfo);
	}
	
	public boolean doDeleteSigner() {
		List<Map<String, String>> signerList = doSelectSSIGN(this.docBasicInfo);
		
		String owner = this.docBasicInfo.get("CREATOR");
		String company = this.docBasicInfo.get("COMPANY_CODE");
		
		
		for(Map<String, String> signer : signerList) {
			try {
				String signerID = MapUtils.getString(signer, "SIGNER_ID", null);
				String signerCompany = MapUtils.getString(signer, "SIGNER_COMPANY_CODE", null);
				
				if(!owner.equals(signerID) || !company.equals(signerCompany)) {
					if(this.checkPermission) {
						authObject.revoke(PrincipalAgentType.PERSONAL,signerCompany,signerID);
					}
				}
			}catch(Exception e) {
				Logger.warn.println(info.getSession("ID"), this, e);
			}
		}
		
		this.initAuthManager();
		
		if(this.linkFlag && !this.linkManager.removeAllUser()) {
			Logger.warn.println(info.getSession("ID"), this, "remove link fail");
		}else {
			this.initLinkManager();
		}
		
		if(!this.doDeleteSSIGN(this.docBasicInfo)) {
			this.errCode = 100601;
			return false;
		}
		return true;
	}
	
	public List<String> doInsertSigner(List<Map<String, String>> signerList) {
		List<String> validatedList = new LinkedList<String>();
		for (int i = 0; i < signerList.size(); i++) {
			Map<String, String> signer = signerList.get(i);
			if (StringUtils.isBlank(signer.get("SIGNER_COMPANY_CODE"))) {
				validatedList.add("not found required data : SINGER[" + String.valueOf(i) + "].SIGNER_COMPANY_CODE");
			}
			if (StringUtils.isBlank(signer.get("SIGNER_COMPANY_NAME"))) {
				validatedList.add("not found required data : SINGER[" + String.valueOf(i) + "].SIGNER_COMPANY_NAME");
			}
			if (StringUtils.isBlank(signer.get("SIGNER_ID"))) {
				validatedList.add("not found required data : SINGER[" + String.valueOf(i) + "].SIGNER_ID");
			}
			if (StringUtils.isBlank(signer.get("SIGNER_NAME"))) {
				validatedList.add("not found required data : SINGER[" + String.valueOf(i) + "].SIGNER_NAME");
			}
			if (StringUtils.isBlank(signer.get("SIGNER_EMAIL"))) {
				validatedList.add("not found required data : SINGER[" + String.valueOf(i) + "].SIGNER_EMAIL");
			}
		}
		
		if(validatedList.size() <= 0) {
			for (int i = 0; i < signerList.size(); i++) {
				Map<String, String> signer = signerList.get(i);
				if(!signer.containsKey("SEQ")) {
					signer.put("SEQ", String.valueOf(i));
				}
				if(!signer.containsKey("SIGNER_NO") || "".equals(signer.get("SIGNER_NO"))) {
					signer.put("SIGNER_NO", DocumentUtil.getDocNumber(info, "SIGNER_ID").result[0]);
				}
				
				if(this.linkFlag) {
					Map<String, String> linkParams = new HashMap<String, String>();
					linkParams.put("CONT_MODULE", "SIGN");
					
					String privateLink = this.linkManager.addUser(
							signer.get("SIGNER_COMPANY_CODE"), 
							signer.get("SIGNER_ID"),
							linkParams);
					if(StringUtils.isBlank(privateLink)) {
						this.errCode = this.linkManager.getLastErrCode();
						validatedList.add("signer uuid create fail : SIGNER[" + String.valueOf(i) + "]");
					}
					
					signer.put("LINK", privateLink);
				}
						
				if(!this.doInsertSSIGN(this.docBasicInfo, signer)) {
					validatedList.add("signer regist fail : SIGNER[" + String.valueOf(i) + "]");
				}
				if(signer.containsKey("SIGNER_TYPE")) {
					String type = signer.get("SIGNER_TYPE");
					if("IN".equals(type) || "OUT".equals(type)) {
						Map<String, String> updateInfo = new HashMap<String, String>(this.docBasicInfo);
						if("IN".equals(type)) {
							updateInfo.put("INNER_MANAGER_NAME", signer.get("SIGNER_NAME"));
							updateInfo.put("INNER_MANAGER_ID", signer.get("SIGNER_ID"));
						}else {
							updateInfo.put("OUTER_COMPANY_NAME", signer.get("SIGNER_COMPANY_NAME"));
							updateInfo.put("OUTER_COMPANY_CODE", signer.get("SIGNER_COMPANY_CODE"));
							updateInfo.put("OUTER_MANAGER_NAME", signer.get("SIGNER_NAME"));
							updateInfo.put("OUTER_MANAGER_ID", signer.get("SIGNER_ID"));
						}
						this.doUpdateDocument(updateInfo);
					}
				}
			}
		} else if(validatedList.size() > 0) {
			this.errCode = 100602;
			return validatedList;
		}
		return null;
	}
	
	public Map<String, String> getSignerInfo() {
		return this.getSignerInfo(true);
	}
	
	public Map<String, String> getSignerInfo(boolean checkCurrentSinger) {
		if(!this.authObject.isReadable()) {
			this.errCode = 100004;
			return null;
		}
		Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
		condition.put("SIGNER_ID"          , info.getSession("ID"));
		condition.put("SIGNER_COMPANY_CODE", info.getSession("COMPANY_CODE"));
		List<Map<String, String>> signerList = this.doSelectSSIGN(condition);
		if(signerList.size() <= 0) {
			this.errCode = 100701;
			return null;
		}else if(signerList.size() > 1) {
			this.errCode = 100707;
			return null;
		}
		
		Map<String, String> signer = signerList.get(0);
		if(checkCurrentSinger) {
			if(!"M".equals(signer.get("SIGN_FLAG"))) {
				this.errCode = 100708;
				return null;
			}
		}
		
		return signer;
	}
	
	/* 
	 * Sign
	 */
	public boolean doStartSign() {
		if(this.checkPermission) {
			if(!authObject.isWritable()) {
				this.errCode = 10000;
				return false;
			}
		}
		
		if(!addHistory(ACTION_TYPE.SIGN_START)) {
			return false;
		}
		
		List<Map<String, String>> signerList = this.doSelectSSIGN(this.docBasicInfo);
		for(Map<String, String> signer : signerList) {
			this.linkManager.setActive(signer.get("SIGNER_COMPANY_CODE"), signer.get("SIGNER_ID"));
			this.linkManager.setUserParams(new SingletonMap<>("CONT_MODULE", "SIGN"));
		}
		
		String signOrder = this.docBasicInfo.get("SIGN_ORDER");
		boolean sequential = StringUtils.isBlank(signOrder) || "S".equals(signOrder);
		if(sequential) {
			//create message params
			Map<String, String> messageParams = this.getMessageParams("SIGN_START", null);
			
			Map<String, String> tmp = new HashMap<>();
			tmp.putAll(this.docBasicInfo);
			for(Entry<String, String> entry : messageParams.entrySet()) {
				messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
			}
			
			//create message
			SepoaMessage message = new SepoaMessage();
			message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
			message.setSepoaInfo(info);
			message.addParams(messageParams);
			
			//creator
			message.addReceiver(this.docBasicInfo.get("CREATOR"), this.docBasicInfo.get("CREATOR_NAME"), this.docBasicInfo.get("CREATOR_EMAIL"), null);
			
			//grant read to all signer
			Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
			condition.put("SIGN_FLAG", "W");
			List<Map<String, String>> waitingSignerList = this.doSelectSSIGN(condition);
			List<String> messageSignerContents = new ArrayList<String>(waitingSignerList.size());
			for(Map<String, String> signer : waitingSignerList) {
				if(!this.grantTo(
						MapUtils.getString(signer, "SIGNER_COMPANY_CODE", null), 
	    				MapUtils.getString(signer, "SIGNER_ID", null), 
	    				Authorized.READ)) {
					return false;
				}
				message.addReceiver(signer.get("SIGNER_ID"), signer.get("SIGNER_NAME"), signer.get("SIGNER_EMAIL"), null);
				messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
			}
			message.addParams("CONTENTS_1", StringUtils.join(messageSignerContents, "<br/>"));
			
			//add message
			if(this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
			
			if(!doNextSigner()) {
				return false;
			}
		}else {
			doStartAllSigner();
		}
		
		List<Map<String, String>> itemList = this.doListImage();
		if(itemList == null) {
			this.errCode = 100704;
			return false;
		}
		
		if(!documentRendering()) {
			return false;
		}
		
		this.doUpdateStatus("G", new SingletonMap<String, String>("PUSH_SEND_DATE", sUtils.date.getToday()));
		return true;
	}
	
	public boolean doRePushSigner() {
		Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
		List<Map<String, String>> signerList = this.doSelectSSIGN(condition);
		List<String> messageSignerContents = new ArrayList<String>(signerList.size());
		
		List<Map<String, String>> messageReceiverList = new ArrayList<>(signerList.size());
		if(signerList != null) {
			for(int i=0;i<signerList.size(); i++) {
				Map<String, String> signer = signerList.get(i);
				String flag = signer.get("SIGN_FLAG");
				if("M".equals(flag)) {
					messageReceiverList.add(signer);
				}
				
				messageSignerContents.add(Cache.text.get("POA_CONTRACT.SIGNER_STATUS_"+flag) + sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
			}
		}
		
		for(Map<String, String> receiver : messageReceiverList) {
			// if the link is null generate new
			if("".equals(receiver.get("LINK"))) {
				String contNo = this.docBasicInfo.get("CONT_NO");
				String contGlSeq = this.docBasicInfo.get("CONT_GL_SEQ");
				
				Map<String, String> linkParams = new HashMap<String, String>();
				linkParams.put("CONT_NO", contNo);
				linkParams.put("CONT_GL_SEQ", contGlSeq);
				
				SepoaLinkManager lm = new SepoaLinkManager(info, config, ctx);
				if(!lm.newLink("EMP_CONTRACT", contNo, contGlSeq)) {
					Logger.warn.println(info.getSession("ID"), this, "LinkManager Initialize failed > " + lm.getLastErrCode());
					continue;
				}
				lm.setParams(linkParams);
				
				linkParams = new HashMap<String, String>();
				linkParams.put("CONT_MODULE", "SIGN");
				String privateLink = lm.addUser(
						receiver.get("SIGNER_COMPANY_CODE"),
						receiver.get("SIGNER_ID"),
						linkParams);
				receiver.put("LINK", privateLink);
				
				if(StringUtils.isBlank(privateLink)) {
					Logger.warn.println(info.getSession("ID"), this, "Generate new link failed > " + lm.getLastErrCode());
					continue;
				}
				
				if(!this.doUpdateSSIGN(this.docBasicInfo, receiver)) {
					Logger.warn.println(info.getSession("ID"), this, "Update Signer Info failed > "+ 100702);
					continue;
				}
			}
			
			//create message params
			Map<String, String> messageParams = this.getMessageParams("SIGN_NEXT", receiver);
			messageParams.put("SUBJECT"       , Cache.text.get("POA_CONTRACT.SIGN_NEXT_SUBJECT"));
			messageParams.put("TITLE_COMMENT", "[" +receiver.get("SIGNER_NAME")+"] "+Cache.text.get("POA_CONTRACT.SIGN_NEXT_TITLE_COMMENT"));
			messageParams.put("CONTENTS_1", StringUtils.join(messageSignerContents, "<br/>"));
			
			Map<String, String> tmp = new HashMap<>();
			tmp.putAll(this.docBasicInfo);
			tmp.putAll(receiver);
			for(Entry<String, String> entry : messageParams.entrySet()) {
				messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
			}
			
			//create message
			SepoaMessage message = new SepoaMessage();
			message.setMessageType("POA_CONTRACT_WITH_BUTTON");
			message.setSepoaInfo(info);
			message.addParams(messageParams);
			message.addReceiver(receiver.get("SIGNER_ID"), receiver.get("SIGNER_NAME"), receiver.get("SIGNER_EMAIL"), null);
			
			//add message
			if(!this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
		}
		
		this.doUpdate(new SingletonMap<String, String>("PUSH_SEND_DATE", sUtils.date.getToday()));
		
		return true;
	}
	
	protected boolean doNextSigner() {
		Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
		
		int nextSignerIndex = -1;
		List<Map<String, String>> signerList = this.doSelectSSIGN(condition);
		List<String> messageSignerContents = new ArrayList<String>(signerList.size());
		if(signerList != null) {
			for(int i=0;i<signerList.size(); i++) {
				Map<String, String> signer = signerList.get(i);
				String flag = signer.get("SIGN_FLAG");
				if(nextSignerIndex < 0 && "W".equals(flag)) {
					signer.put("SIGN_FLAG", "M");
					flag = "M";
					nextSignerIndex = i;
				}
				messageSignerContents.add(Cache.text.get("POA_CONTRACT.SIGNER_STATUS_"+flag) + sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
			}
		}
		
		if(nextSignerIndex < 0) {
			this.errCode = 100701;
			return false;
		}
		
		Map<String, String> nextSigner = signerList.get(nextSignerIndex);
		if(!this.doUpdateSSIGN(this.docBasicInfo, nextSigner)) {
			this.errCode = 100702;
			return false;
		}
		
		if(!this.grantTo(
				nextSigner.get("SIGNER_COMPANY_CODE"), 
				nextSigner.get("SIGNER_ID"), 
				Authorized.WRITE)) {
			return false;
		}
		
		if(!addHistory(ACTION_TYPE.SIGN_REQUEST)) {
			return false;
		}
		
		//create message params
		Map<String, String> messageParams = this.getMessageParams("SIGN_NEXT", nextSigner);
		messageParams.put("SUBJECT"       , Cache.text.get("POA_CONTRACT.SIGN_NEXT_SUBJECT"));
		messageParams.put("TITLE_COMMENT", "["+nextSigner.get("SIGNER_NAME")+"] "+Cache.text.get("POA_CONTRACT.SIGN_NEXT_TITLE_COMMENT"));
		messageParams.put("CONTENTS_1", StringUtils.join(messageSignerContents, "<br/>"));
		
		Map<String, String> tmp = new HashMap<>();
		tmp.putAll(this.docBasicInfo);
		tmp.putAll(nextSigner);
		for(Entry<String, String> entry : messageParams.entrySet()) {
			messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
		}
		
		//create message
		SepoaMessage message = new SepoaMessage();
		message.setMessageType("POA_CONTRACT_WITH_BUTTON");
		message.setSepoaInfo(info);
		message.addParams(messageParams);
		message.addReceiver(nextSigner.get("SIGNER_ID"), nextSigner.get("SIGNER_NAME"), nextSigner.get("SIGNER_EMAIL"), null);
		
		//add message
		if(this.pushManager.addMessage(message)) {
			Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
		}
		
		return true;
	}
	
	protected boolean doStartAllSigner() {
		Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
		condition.put("SIGN_FLAG", "W");
		List<Map<String, String>> signerList = this.doSelectSSIGN(condition);
		if(signerList == null || signerList.size() <= 0) {
			this.errCode = 100701;
			return false;
		}
		
		//create signer info
		List<String> messageSignerContents = new ArrayList<String>(signerList.size());
		if(signerList != null) {
			for(int i=0;i<signerList.size(); i++) {
				Map<String, String> signer = signerList.get(i);
				messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
			}
		}
		String messageContent1 = StringUtils.join(messageSignerContents, "<br/>");
		
		for(Map<String, String> nextSigner : signerList) {
			nextSigner.put("SIGN_FLAG", "M");
			
			if(!this.doUpdateSSIGN(this.docBasicInfo, nextSigner)) {
				this.errCode = 100702;
				return false;
			}
			
			if(!this.grantTo(
					nextSigner.get("SIGNER_COMPANY_CODE"), 
					nextSigner.get("SIGNER_ID"), 
					Authorized.WRITE)) {
				return false;
			}
			
			//create message params
			Map<String, String> messageParams = this.getMessageParams("SIGN_START_ALL", nextSigner);
			messageParams.put("CONTENTS_1", messageContent1);
			
			Map<String, String> tmp = new HashMap<>();
			tmp.putAll(this.docBasicInfo);
			tmp.putAll(nextSigner);
			for(Entry<String, String> entry : messageParams.entrySet()) {
				messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
			}
			
			//create message
			SepoaMessage message = new SepoaMessage();
			message.setMessageType("POA_CONTRACT_WITH_BUTTON");
			message.setSepoaInfo(info);
			message.addParams(messageParams);
			
			message.addReceiver(nextSigner.get("SIGNER_ID"), nextSigner.get("SIGNER_NAME"), nextSigner.get("SIGNER_EMAIL"), null);
			
			//add message
			if(this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
		}
		return true;
	}
	
	public Map<String, String> doSignDigital(String fixHtml, String comment) {
		Map<String, String> signer = getSignerInfo();
		if(signer == null) {
			return null;
		}
		signer.put("SIGN_FLAG"    , "S");
		signer.put("SIGN_TYPE"    , "D");
		signer.put("SIGN_COMMENT" , comment);
		
		// SIGN_FIX_HTML이 4000바이트를 넘으면 4000바이트씩 분할하여 순회 업데이트 (ORA-01461 대응)
		if(fixHtml != null) {
			byte[] bytes = fixHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			if(bytes.length > 4000) {
				List<String> chunks = new ArrayList<String>();
				int offset = 0;
				while(offset < bytes.length) {
					int endIndex = Math.min(offset + 4000, bytes.length);
					String chunk = new String(bytes, offset, endIndex - offset, java.nio.charset.StandardCharsets.UTF_8);
					chunks.add(chunk);
					offset = endIndex;
				}
				
				for(int i = 0; i < chunks.size(); i++) {
					signer.put("SIGN_FIX_HTML", chunks.get(i));
					boolean ok;
					if(i == 0) {
						// 첫 청크: 전체 컬럼 갱신 + SIGN_FIX_HTML 교체
						ok = this.doUpdateSSIGN(this.docBasicInfo, signer);
					} else {
						// 이후 청크: SIGN_FIX_HTML concat
						ok = this.doUpdateSSIGNChunk(this.docBasicInfo, signer);
					}
					if(!ok) {
						this.errCode = 100709;
						return null;
					}
				}
			} else {
				signer.put("SIGN_FIX_HTML", fixHtml);
				if(!this.doUpdateSSIGN(this.docBasicInfo, signer)) {
					this.errCode = 100709;
					return null;
				}
			}
		} else {
			if(!this.doUpdateSSIGN(this.docBasicInfo, signer)) {
				this.errCode = 100709;
				return null;
			}
		}
		
		if(!doUpdateSignFlag(signer)) {
			return null;
		}
		if(!addHistory(ACTION_TYPE.SIGN)) {
			return null;
		}
		if(!doSignAfter()) {
			return null;
		}
		
		return signer;
	}
	
	public Map<String, String> doSignCert(String signType, String fixHtml, String comment, String signValue) {
		return this.doSignCert(signType, fixHtml, comment, signValue, null);
	}
	
	public Map<String, String> doSignCert(String signType, String fixHtml, String comment, String signValue, Map<String, String> extData) {
		Map<String, String> signer = getSignerInfo();
		if(signer == null) {
			return null;
		}
		String certInfo = null;
		String orgValue = this.getContractHash();
		if("SERVER".equals(signType)) {
			String certModuleType = CommonUtil.getConfig("sepoa.sign.module", "NX"); 
			SepoaSign ss = SepoaSignFactory.newInstance(config, info, certModuleType);
			signValue = ss.sign(signer.get("SIGN_KEY"), orgValue);
			if(StringUtils.isBlank(signValue)) {
				this.errCode = ss.getLastErrCode();
				return null;
			}
			certInfo = ss.getCertInfo();
		} else {
			SepoaSign ss = SepoaSignFactory.newInstance(config, info, signType);
			ss.setExtraData(extData);
			
			if(!ss.setSignValue(signValue)) {
				this.errCode = ss.getLastErrCode();
				return null;
			}
			if(StringUtils.isBlank(orgValue)) {
				this.errCode = 100710;
				return null;
			}
			if(!ss.compareToOrigin(orgValue)) {
				this.errCode = ss.getLastErrCode();
				return null;
			}
			if(!ss.verifyCert()) {
				this.errCode = ss.getLastErrCode();
				return null;
			}
			if(!ss.verifyUser(signer)) {
				this.errCode = ss.getLastErrCode();
				return null;
			}
			
			certInfo = ss.getCertInfo();
			
			Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
			condition.put("SIGN_TYPE", signType);
			condition.put("CERT_INFO_FLAG", "true");
			
			List<Map<String, String>> beforeSignList = this.doSelectSSIGN(condition);
			if(beforeSignList != null) {
				for(Map<String, String> beforeSign : beforeSignList) {
					if(certInfo.equals(beforeSign.get("CERT_INFO"))) {
						this.errCode = 100711;
						return null;
					}
				}
			}
		}
		
		signer.put("SIGN_FLAG"     , "S");
		signer.put("SIGN_TYPE"     , signType);
		signer.put("SIGN_VALUE"    , signValue);
		signer.put("SIGN_ORG_VALUE", orgValue);
		signer.put("SIGN_COMMENT"  , comment);
		signer.put("CERT_INFO"     , certInfo);
		
		// SIGN_FIX_HTML이 4000바이트를 넘으면 4000바이트씩 분할하여 순회 업데이트 (ORA-01461 대응)
		if(fixHtml != null) {
			byte[] bytes = fixHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			if(bytes.length > 4000) {
				List<String> chunks = new ArrayList<String>();
				int offset = 0;
				while(offset < bytes.length) {
					int endIndex = Math.min(offset + 4000, bytes.length);
					String chunk = new String(bytes, offset, endIndex - offset, java.nio.charset.StandardCharsets.UTF_8);
					chunks.add(chunk);
					offset = endIndex;
				}
				
				for(int i = 0; i < chunks.size(); i++) {
					signer.put("SIGN_FIX_HTML", chunks.get(i));
					boolean ok;
					if(i == 0) {
						ok = this.doUpdateSSIGN(this.docBasicInfo, signer);
					} else {
						ok = this.doUpdateSSIGNChunk(this.docBasicInfo, signer);
					}
					if(!ok) {
						this.errCode = 100709;
						return null;
					}
				}
			} else {
				signer.put("SIGN_FIX_HTML", fixHtml);
				if(!this.doUpdateSSIGN(this.docBasicInfo, signer)) {
					this.errCode = 100709;
					return null;
				}
			}
		} else {
			if(!this.doUpdateSSIGN(this.docBasicInfo, signer)) {
				this.errCode = 100709;
				return null;
			}
		}
		
		if(!this.revokeFrom(info.getSession("COMPANY_CODE"), info.getSession("ID"))) {
			return null;
		}
		if(!doUpdateSignFlag(signer)) {
			return null;
		}
		if(!addHistory(ACTION_TYPE.SIGN)) {
			return null;
		}
		if(!doSignAfter()) {
			return null;
		}
		return signer;
	}
	
	protected boolean doUpdateSignFlag(Map<String, String> signer) {
		Map<String, String> docItem = this.doSelectItem();
		String signFlagKey = "OUTER_SIGN_COUNT";
		if(signer.get("SIGNER_COMPANY_CODE").equals(this.docBasicInfo.get("COMPANY_CODE"))) {
			signFlagKey = "INNER_SIGN_COUNT";
		}
		if(docItem.containsKey(signFlagKey)) {
			int beforeCount = 0;
			try {
				beforeCount = Integer.valueOf(docItem.get(signFlagKey));
			}catch(Exception e) {}
			docItem.put(signFlagKey, String.valueOf(++beforeCount));
		}else {
			docItem.put(signFlagKey, "1");
		}
		return this.doUpdateItem(docItem);
	}
	
	protected boolean doSignAfter() {
		String signOrder = this.docBasicInfo.get("SIGN_ORDER");
		if(signOrder.isEmpty() || "S".equals(signOrder)) {
			if(!doNextSigner()) {
				if(this.errCode == 100701) {
					this.errCode = 0;
					return doSignComplete();
				}else {
					return false;
				}
			}
		}else {
			Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
			condition.put("SIGN_FLAG", "M");
			List<Map<String, String>> waitingList = this.doSelectSSIGN(condition);
			if(waitingList == null || waitingList.size() == 0) {
				return doSignComplete();
			}
		}
		return true;
	}
	
	public boolean doSignComplete() {
		if(!this.createCompletePDF()) {
			return false;
		}
		if(!this.doUpdateStatus("A")) {
			return false;
		}
		if(!addHistory(ACTION_TYPE.SIGN_COMPLETE)) {
			return false;
		}
		
		//create message params
		Map<String, String> messageParams = this.getMessageParams("SIGN_COMPLETE", null);
		
		Map<String, String> tmp = new HashMap<>();
		tmp.putAll(this.docBasicInfo);
		for(Entry<String, String> entry : messageParams.entrySet()) {
			messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
		}
		
		//create message
		SepoaMessage message = new SepoaMessage();
		message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
		message.addParams(messageParams);
		
		try {
			Map<String, String> fileData = this.doDocumentDownload();
			String fileName = fileData.get("FILE_NAME_CLIENT");
			String filePath = fileData.get("FILE_PATH");
			File targetFile = new File(filePath);
			
			//add attach
			message.addAttach(CipherManager.getCipher().getInputStream(targetFile, false), fileName);
			
			//creator
			message.addReceiver(this.docBasicInfo.get("CREATOR"), this.docBasicInfo.get("CREATOR_NAME"), this.docBasicInfo.get("CREATOR_EMAIL"), null);
			
			//signer
			List<Map<String, String>> signerList = this.doSelectSSIGN(this.docBasicInfo);
			List<String> messageSignerContents = new ArrayList<String>(signerList.size());
			for(Map<String, String> signer : signerList) {
				message.addReceiver(signer.get("SIGNER_ID"), signer.get("SIGNER_NAME"), signer.get("SIGNER_EMAIL"), null);
				messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
			}
			message.addParams("CONTENTS_1", StringUtils.join(messageSignerContents, "<br/>")); 
			
			//add message
			if(this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
		} catch (CipherException e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return true;
	}
	
	public boolean doSignReject() {
		if(!doUpdateStatus("C")) {
			return false;
		}
		
		try {
			Map<String, String> currentSigner = getSignerInfo();
			
			if(currentSigner == null) {
				return false;
			}
			
			// current Signer SignFlag = C
			currentSigner.put("SIGN_FLAG", "C");
			if(!doUpdateSSIGN(this.docBasicInfo, currentSigner)) {
				this.errCode = 100702;
				return false;
			}
			if(!addHistory(ACTION_TYPE.SIGN_REJECT)) {
				return false;
			}
			
			// document Status = C
			if(!doUpdateDocument(this.docBasicInfo)) {
				this.errCode = 100204;
				return false;
			}
			if(!addHistory(ACTION_TYPE.DOCUMENT_UPDATE)) {
				return false;
			}
			
			//signer
			List<Map<String, String>> signerList = this.doSelectSSIGN(this.docBasicInfo);
			for(Map<String, String> signer : signerList) {
				if(!this.docBasicInfo.get("CREATOR").equals(signer.get("SIGNER_ID"))) {
					if(!this.grantTo(
							MapUtils.getString(signer, "SIGNER_COMPANY_CODE", null), 
							MapUtils.getString(signer, "SIGNER_ID", null), 
							Authorized.READ)) {
						return false;
					}
				}
			}
			
			//create message params
			Map<String, String> messageParams = this.getMessageParams("SIGN_REJECT", null);
			
			Map<String, String> tmp = new HashMap<>();
			tmp.putAll(this.docBasicInfo);
			tmp.putAll(currentSigner);
			for(Entry<String, String> entry : messageParams.entrySet()) {
				messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
			}
			
			//create message
			SepoaMessage message = new SepoaMessage();
			message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
			message.addParams(messageParams);
			
			//creator
			message.addReceiver(this.docBasicInfo.get("CREATOR"), this.docBasicInfo.get("CREATOR_NAME"), this.docBasicInfo.get("CREATOR_EMAIL"), null);
			
			for(Map<String, String> signer : signerList) {
				message.addReceiver(signer.get("SIGNER_ID"), signer.get("SIGNER_NAME"), signer.get("SIGNER_EMAIL"), null);
			}
			message.addParams("CONTENTS_1", sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), currentSigner)); 
			
			//add message
			if(!this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
			
			return true;
		} catch (Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return false;
	}
	
	/*
	 * REVIEW PROCESS
	 * 
	 * */
	public List<Map<String, String>> doSelectReviewer() {
		return this.doSelectReviewer(null);
	}
	
	public List<Map<String, String>> doSelectReviewer(Map<String, String> condition){
		List<Map<String, String>> result = null;
		
		Map<String, String> innerCondition = new HashMap<>(this.docBasicInfo);
		if(condition != null) {
			innerCondition.putAll(condition);
		}
		
		List<Map<String, String>> reviewers = this.doSelectSCTRV(innerCondition);
		if(reviewers.size() > 0) {
			int reviewMaxSeq = reviewers.stream().mapToInt(m -> Integer.parseInt(m.get("SEQ"))).max().getAsInt();
			List<Map<String, String>> filteredReviewers = reviewers.stream().filter(m -> Integer.parseInt(m.get("SEQ")) == reviewMaxSeq).collect(Collectors.toList());
			result = filteredReviewers;
		}
		
		return result;
	}
	
	public Map<String, String> getReviewerInfo() {
		if(!this.authObject.isWritable()) {
			this.errCode = 100006;
			return null;
		}
		Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
		condition.put("REVIEWER_ID"          , info.getSession("ID"));
		condition.put("REVIEWER_COMPANY_CODE", info.getSession("COMPANY_CODE"));

		Map<String, String> reviewer = null;
		List<Map<String, String>> reviewerList = this.doSelectReviewer(condition);
		if(reviewerList.size() <= 0) {
			this.errCode = 101203;
			return null;
		}else if(reviewerList.size() > 1) {
			Optional<Map<String, String>> result = reviewerList.stream().max(Comparator.comparingInt(m -> Integer.parseInt(m.get("SEQ"))));
			if(result.equals(Optional.empty())) {
				this.errCode = 101205;
				return null;
			}
			
			reviewer = result.get();
		}
		
		if(reviewer == null) {
			reviewer = reviewerList.get(0);
		}
		
		if(!"RV".equals(reviewer.get("REVIEW_FLAG"))) {
			this.errCode = 101206;
			return null;
		}
		
		return reviewer;
	}
	
	public boolean doReviewStart() {
		if(!authObject.isWritable()) {
			this.errCode = 10000;
			return false;
		}
		
		if(!addHistory(ACTION_TYPE.REVIEW_START)) {
			return false;
		}

		List<Map<String, String>> signerList = this.doSelectSSIGN(this.docBasicInfo);
		if(signerList.size() == 0) {
			this.errCode = 101201;
			return false;
		}
		
		int seq = 1;
		List<Map<String, String>> reviewerList = this.doSelectReviewer();
		if(reviewerList != null && reviewerList.size() > 0) {
			int currentSeq = MapUtils.getInteger(reviewerList.get(0), "SEQ");
			seq += currentSeq;
		}

		// insert reviewer data list
		int reviewSeq = 1;
		for(Map<String, String> signer : signerList) {
			boolean isCreator = this.docBasicInfo.get("CREATOR").equalsIgnoreCase(signer.get("SIGNER_ID"));
			if(isCreator == false) {
				Map<String, String> reviewer = new HashMap<>();
				reviewer.put("SEQ",Integer.toString(seq));
				reviewer.put("REVIEW_SEQ",Integer.toString(reviewSeq));
				reviewer.put("REVIEWER_NO", signer.get("SIGNER_NO"));
				reviewer.put("REVIEWER_ID", signer.get("SIGNER_ID"));
				reviewer.put("REVIEWER_NAME", signer.get("SIGNER_NAME"));
				reviewer.put("REVIEWER_EMAIL", signer.get("SIGNER_EMAIL"));
				reviewer.put("REVIEWER_PHONE", signer.get("SIGNER_PHONE"));
				reviewer.put("REVIEWER_COMPANY_CODE", signer.get("SIGNER_COMPANY_CODE"));
				reviewer.put("REVIEWER_COMPANY_NAME", signer.get("SIGNER_COMPANY_NAME"));
				reviewer.put("REVIEWER_COMPANY_OWNER", signer.get("SIGNER_COMPANY_OWNER"));
				reviewer.put("REVIEWER_COMPANY_ADDR", signer.get("SIGNER_COMPANY_ADDR"));
				
				if(!"".equals(signer.get("LINK"))) {
					this.linkManager.setActive(reviewer.get("REVIEWER_COMPANY_CODE"), reviewer.get("REVIEWER_ID"));
					this.linkManager.setUserParams(new SingletonMap<>("CONT_MODULE", "REVIEW"));
					
					reviewer.put("LINK", signer.get("LINK"));
				} else {
					Map<String, String> linkParams = new HashMap<String, String>();
					linkParams.put("CONT_MODULE", "REVIEW");
					
					String privateLink = this.linkManager.addUser(
							reviewer.get("REVIEWER_COMPANY_CODE"), 
							reviewer.get("REVIEWER_ID"),
							linkParams);
					if(StringUtils.isBlank(privateLink)) {
						this.errCode = this.linkManager.getLastErrCode();
						return false;
					}
					
					reviewer.put("LINK", privateLink);
				}

				if(!this.doInsertSCTRV(this.docBasicInfo, reviewer)) {
					this.errCode = 101202;
					return false;
				}
				
				reviewSeq++;
			}
		}
		
		String signOrder = this.docBasicInfo.get("SIGN_ORDER");
		boolean sequential = StringUtils.isBlank(signOrder) || "S".equals(signOrder);
		if(sequential) {
			//create message params
			Map<String, String> messageParams = this.getMessageParams("REVIEW_REQ_START", null);
			
			Map<String, String> tmp = new HashMap<>();
			tmp.putAll(this.docBasicInfo);
			for(Entry<String, String> entry : messageParams.entrySet()) {
				messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
			}
			
			//create message
			SepoaMessage message = new SepoaMessage();
			message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
			message.setSepoaInfo(info);
			message.addParams(messageParams);
			
			//creator
			message.addReceiver(this.docBasicInfo.get("CREATOR"), this.docBasicInfo.get("CREATOR_NAME"), this.docBasicInfo.get("CREATOR_EMAIL"), null);
			
			//grant read to all reviewer
			List<Map<String, String>> targetReviewerList = this.doSelectReviewer();
			List<String> messageSignerContents = new ArrayList<String>(targetReviewerList.size());
			for(Map<String, String> reviewer : targetReviewerList) {
				if(!this.grantTo(
						MapUtils.getString(reviewer, "REVIEWER_COMPANY_CODE", null), 
						MapUtils.getString(reviewer, "REVIEWER_ID", null), 
						Authorized.READ)) {
					return false;
				}
				message.addReceiver(reviewer.get("REVIEWER_ID"), reviewer.get("REVIEWER_NAME"), reviewer.get("REVIEWER_EMAIL"), null);
				messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.REVIEWER_INFO"), reviewer));
			}
			message.addParams("CONTENTS_1", StringUtils.join(messageSignerContents, "<br/>"));
			
			//add message
			if(this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
			
			if(!doNextReviewer()) {
				return false;
			}
		}else {
			doAllReviewerStart();
		}
		
		if(!this.doUpdateStatus("RP", new SingletonMap<String, String>("PUSH_SEND_DATE", sUtils.date.getToday()))) {
			return false;
		}
		
		return true;
	}
	
	public boolean doNextReviewer() {
		int nextReviewerIndex = -1;
		List<Map<String, String>> reviewerList = this.doSelectReviewer();
		List<String> messageReviewerContents = new ArrayList<String>(reviewerList.size());
		if(reviewerList != null) {
			for(int i = 0; i < reviewerList.size(); i++) {
				Map<String, String> reviewer = reviewerList.get(i);
				String reviewerFlag = reviewer.get("REVIEW_FLAG");
				if(nextReviewerIndex < 0 && "RW".equals(reviewerFlag)) {
					reviewerFlag = "RV";
					reviewer.put("REVIEW_FLAG", reviewerFlag);
					nextReviewerIndex = i;
				}
				messageReviewerContents.add(Cache.text.get("POA_CONTRACT.REVIEWER_STATUS_" + reviewerFlag) + sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.REVIEWER_INFO"), reviewer));
			}
		}
		
		if(nextReviewerIndex < 0) {
			this.errCode = 101203;
			return false;
		}
		
		Map<String, String> nextReviewer = reviewerList.get(nextReviewerIndex);
		
		if(!this.doUpdateSCTRV(this.docBasicInfo, nextReviewer)) {
			this.errCode = 101204;
			return false;
		}
		
		if(!this.grantTo(
				nextReviewer.get("REVIEWER_COMPANY_CODE"),
				nextReviewer.get("REVIEWER_ID"), 
				Authorized.WRITE)) {
			return false;
		}
		
		if(!addHistory(ACTION_TYPE.REVIEW_REQUEST)) {
			return false;
		}
		
		//create message params
		Map<String, String> messageParams = this.getMessageParams("REVIEW_REQ_NEXT", nextReviewer); 
		messageParams.put("TITLE_COMMENT", "[" + nextReviewer.get("REVIEWER_NAME")+"] "+Cache.text.get("POA_CONTRACT.REVIEW_REQ_NEXT_TITLE_COMMENT"));
		messageParams.put("CONTENTS_1", StringUtils.join(messageReviewerContents, "<br/>"));
		
		Map<String, String> tmp = new HashMap<>();
		tmp.putAll(this.docBasicInfo);
		tmp.putAll(nextReviewer);
		for(Entry<String, String> entry : messageParams.entrySet()) {
			messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
		}
		
		//create message
		SepoaMessage message = new SepoaMessage();
		message.setMessageType("POA_CONTRACT_WITH_BUTTON");
		message.setSepoaInfo(info);
		message.addParams(messageParams);
		message.addReceiver(nextReviewer.get("REVIEWER_ID"), nextReviewer.get("REVIEWER_NAME"), nextReviewer.get("REVIEWER_EMAIL"), null);
		
		//add message
		if(this.pushManager.addMessage(message)) {
			Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
		}
		
		return true;
	}
	
	public boolean doAllReviewerStart() {
		Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
		condition.put("REVIEW_FLAG", "RW");
		List<Map<String, String>> waitingReviewerList = this.doSelectReviewer(condition);
		if(waitingReviewerList == null || waitingReviewerList.size() <= 0) {
			this.errCode = 101203;
			return false;
		}
		
		List<String> messageSignerContents = new ArrayList<String>(waitingReviewerList.size());
		if(waitingReviewerList != null) {
			for(int i = 0; i < waitingReviewerList.size(); i++) {
				Map<String, String> reviewer = waitingReviewerList.get(i);
				messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.REVIEWER_INFO"), reviewer));
			}
		}
		String messageContent1 = StringUtils.join(messageSignerContents, "<br/>");
		
		for(Map<String, String> reviewer : waitingReviewerList) {
			reviewer.put("REVIEW_FLAG", "RV");
			
			if(!this.doUpdateSCTRV(this.docBasicInfo, reviewer)) {
				this.errCode = 101204;
				return false;
			}
			
			if(!this.grantTo(
					reviewer.get("REVIEWER_COMPANY_CODE"), 
					reviewer.get("REVIEWER_ID"), 
					Authorized.WRITE)) {
				return false;
			}
			
			//create message params
			Map<String, String> messageParams = this.getMessageParams("REVIEW_REQ_START_ALL", reviewer);
			messageParams.put("CONTENTS_1", messageContent1);
			
			Map<String, String> tmp = new HashMap<>();
			tmp.putAll(this.docBasicInfo);
			tmp.putAll(reviewer);
			for(Entry<String, String> entry : messageParams.entrySet()) {
				messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
			}
			
			//create message
			SepoaMessage message = new SepoaMessage();
			message.setMessageType("POA_CONTRACT_WITH_BUTTON");
			message.setSepoaInfo(info);
			message.addParams(messageParams);
			
			message.addReceiver(reviewer.get("REVIEWER_ID"), reviewer.get("REVIEWER_NAME"), reviewer.get("REVIEWER_EMAIL"), null);
			
			//add message
			if(this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
		}

		return true;
	}
	
	public boolean doReviewAccecpt(String comment) {
		return this.doReviewProcess("RA", comment);
	}
	
	public boolean doReviewReject(String comment) {
		return this.doReviewProcess("RR", comment);
	}
	
	protected boolean doReviewProcess(String flag, String comment) {
		try {
			Map<String, String> currentReviewer = getReviewerInfo();
			if(currentReviewer == null) {
				return false;
			}
			
			currentReviewer.put("REVIEW_FLAG", flag);
			currentReviewer.put("REVIEW_COMMENT", comment);
			if(!doUpdateSCTRV(this.docBasicInfo, currentReviewer)) {
				this.errCode = 101204;
				return false;
			}
			
			ACTION_TYPE history = null;
			switch(flag) {
			case "RA" :
				history = ACTION_TYPE.REVIEW_ACCEPT;
				break;
			case "RR" :
				history = ACTION_TYPE.REVIEW_REJECT;
				break;
			}
			
			if(history == null) {
				this.errCode = 101207;
			}
			
			if(!addHistory(history)) {
				return false;
			}
			
			if(history == ACTION_TYPE.REVIEW_ACCEPT) {
				if(!this.doReviewAfter()) {
					return false;
				}
				
			} else if(history == ACTION_TYPE.REVIEW_REJECT) {
				if(!doUpdateStatus(flag)) {
					return false;
				}
				
				Map<String, String> messageParams = this.getMessageParams("REVIEW_REQ_REJECT", null);
				
				Map<String, String> tmp = new HashMap<>();
				tmp.putAll(this.docBasicInfo);
				tmp.putAll(currentReviewer);
				for(Entry<String, String> entry : messageParams.entrySet()) {
					messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
				}
				
				SepoaMessage message = new SepoaMessage();
				message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
				message.addParams(messageParams);
				
				message.addReceiver(this.docBasicInfo.get("CREATOR"), this.docBasicInfo.get("CREATOR_NAME"), this.docBasicInfo.get("CREATOR_EMAIL"), null);
				
				List<Map<String, String>> reviewerList = this.doSelectReviewer();
				for(Map<String, String> reviewer : reviewerList) {
					message.addReceiver(reviewer.get("REVIEWER_ID"), reviewer.get("REVIEWER_NAME"), reviewer.get("REVIEWER_EMAIL"), null);
				}
				message.addParams("CONTENTS_1", sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.REVIEWER_INFO"), currentReviewer)); 
				
				if(!this.pushManager.addMessage(message)) {
					Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
				}
			}
			
			return true;
		} catch (Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return false;
	}
	
	protected boolean doReviewAfter() {
		String signOrder = this.docBasicInfo.get("SIGN_ORDER");
		if(signOrder.isEmpty() || "S".equals(signOrder)) {
			if(!this.doNextReviewer()) {
				if(this.errCode == 101203) {
					this.errCode = 0;
					return this.doReviewComplete();
				}else {
					return false;
				}
			}
		}else {
			Map<String, String> condition = new HashMap<String, String>(this.docBasicInfo);
			condition.put("REVIEW_FLAG", "RV");
			List<Map<String, String>> waitingList = this.doSelectReviewer(condition);
			if(waitingList == null || waitingList.size() == 0) {
				return doReviewComplete();
			}
		}
		return true;
	}
	
	public boolean doReviewComplete() {
		if(!this.doUpdateStatus("RA")) {
			return false;
		}
		
		if(!addHistory(ACTION_TYPE.REVIEW_COMPLETE)) {
			return false;
		}
		
		//create message params
		Map<String, String> messageParams = this.getMessageParams("REVIEW_COMPLETE", null);
		messageParams.put("TITLE_COMMENT", "");
		
		Map<String, String> tmp = new HashMap<>();
		tmp.putAll(this.docBasicInfo);
		for(Entry<String, String> entry : messageParams.entrySet()) {
			messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
		}
		
		//create message
		SepoaMessage message = new SepoaMessage();
		message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
		message.setSepoaInfo(info);
		message.addParams(messageParams);
		
		//creator
		message.addReceiver(this.docBasicInfo.get("CREATOR"), this.docBasicInfo.get("CREATOR_NAME"), this.docBasicInfo.get("CREATOR_EMAIL"), null);
		
		List<Map<String, String>> reviewerList = this.doSelectReviewer();
		List<String> messageSignerContents = new ArrayList<String>(reviewerList.size());
		for(Map<String, String> reviewer : reviewerList) {
			messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.REVIEWER_INFO"), reviewer));
		}
		message.addParams("CONTENTS_1", StringUtils.join(messageSignerContents, "<br/>"));
		
		//add message
		if(this.pushManager.addMessage(message)) {
			Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
		}
		
		Map<String, String> optionsMap = new HashMap<String, String>();
//		this.callModulePluginService(pluginService, "doReviewCompleteAfter", new Object[] {optionsMap});
		
		if(optionsMap.containsKey("NEXT_SERVICE")) {
			String nextService = optionsMap.get("NEXT_SERVICE");
			switch(nextService.toUpperCase()) {
			case "SIGN":
				boolean autoSignFlag = Boolean.parseBoolean(MapUtils.getString(optionsMap, "AUTO_SIGN_FLAG", "false"));
				if(autoSignFlag) {
					if(!this.doStartSign()) {
						return false;
					}
				}
				break;
			case "APPROVAL":
				return true;
			case "COMPLETE":
				if(!this.doUpdateStatus("A")) {
					return false;
				}
				break;
			default:
				if(!this.doStartSign()) {
					return false;
				}
				break;
			}
		} else {
			if(!this.doStartSign()) {
				return false;
			}
		}
		
		return true;
	}
	
	/*
	 * APPROVAL
	 * 
	 * */
	public boolean doApprovalStart() {
		if(!authObject.isWritable()) {
			this.errCode = 10000;
			return false;
		}
		
		if(!this.doUpdateStatus("I")) {
			return false;
		}
		
		return true;
	}
	
	public boolean doApprovalFinish() {
		Map<String, String> optionsMap = new HashMap<String, String>();
//		this.callModulePluginService(pluginService, "doApprovalFinishAfter", new Object[] {optionsMap});
		
		if(optionsMap.containsKey("NEXT_SERVICE")) {
			if(!this.doUpdateStatus("F")) {
				return false;
			}
			
			String nextService = optionsMap.get("NEXT_SERVICE");
			switch(nextService.toUpperCase()) {
			case "SIGN":
				boolean autoSignFlag = Boolean.parseBoolean(MapUtils.getString(optionsMap, "AUTO_SIGN_FLAG", "false"));
				if(autoSignFlag) {
					if(!this.doStartSign()) {
						return false;
					}
				}
				break;
			case "REVIEW":
				if(!this.doReviewStart()) {
					return false;
				}
			case "COMPLETE":
				if(!this.doUpdateStatus("A")) {
					return false;
				}
				break;
			default :
				Map<String, String> itemData = this.doSelectItem();
				if(itemData.containsKey("EX_CONT_CASE") && "MULTIPLE".equals(itemData.get("EX_CONT_CASE"))) {
					if(this.doUpdateStatus("MW") == false) {
						return false;
					}
					
				} else {
					if(!this.doStartSign()) {
						return false;
					}
				}
				break;
			}
		} else {
			Map<String, String> itemData = this.doSelectItem();
			if(itemData.containsKey("EX_CONT_CASE") && "MULTIPLE".equals(itemData.get("EX_CONT_CASE"))) {
				if(this.doUpdateStatus("MW") == false) {
					return false;
				}
				
			} else {
				if(!this.doStartSign()) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public boolean doApprovalReject() {
		if(!this.doUpdateStatus("R")) {
			return false;
		}
		
		return true;
	}
	
	/*
	 * Manual
	 */
	public boolean doCompleteForManual(List<Map<String, String>> fileList) {
		this.errCode = 0;
		if(!"S".equals(this.docBasicInfo.get("CONT_METHOD"))) {
			this.errCode = 101104;
			return false;
		}
		
		final String[] manualUploadAllowStatus = {"A", "G", "UW"};
		if(!Arrays.asList(manualUploadAllowStatus).contains(this.docBasicInfo.get("STATUS"))) {
			this.errCode = 101101;
			return false;
		}
		
		for(Map<String, String> file : fileList) {
			if(this.doAddFile("CC", file, true) == null) {
				this.errCode = 101102;
				return false;
			}
		}
		
		List<Map<String, String>> signerList = this.doSelectSSIGN(this.docBasicInfo);
		for(Map<String, String> signer : signerList) {
			this.doUpdateSSIGN(signer, new SingletonMap<String, String>("SIGN_FLAG", "S"));
		}
		
		if(!this.doUpdateStatus("A")) {
			this.errCode = 101103;
			return false;
		}
		
		//create message params
		Map<String, String> messageParams = this.getMessageParams("MANUAL_COMPLETE", null);
		
		Map<String, String> tmp = new HashMap<>();
		tmp.putAll(this.docBasicInfo);
		for(Entry<String, String> entry : messageParams.entrySet()) {
			messageParams.put(entry.getKey(), sUtils.string.dataReplacer(entry.getValue(), tmp));
		}
		
		//create message
		SepoaMessage message = new SepoaMessage();
		message.setMessageType("POA_CONTRACT_WITHOUT_BUTTON");
		message.addParams(messageParams);
		
		try {
			Map<String, String> fileData = this.doDocumentDownload();
			String fileName = fileData.get("FILE_NAME_CLIENT");
			String filePath = fileData.get("FILE_PATH");
			File targetFile = new File(filePath);
			
			//add attach
			message.addAttach(CipherManager.getCipher().getInputStream(targetFile, false), fileName);
			
			//creator
			message.addReceiver(this.docBasicInfo.get("CREATOR"), this.docBasicInfo.get("CREATOR_NAME"), this.docBasicInfo.get("CREATOR_EMAIL"), null);
			
			//signer
			List<String> messageSignerContents = new ArrayList<String>(signerList.size());
			for(Map<String, String> signer : signerList) {
				message.addReceiver(signer.get("SIGNER_ID"), signer.get("SIGNER_NAME"), signer.get("SIGNER_EMAIL"), null);
				messageSignerContents.add(sUtils.string.dataReplacer(Cache.text.get("POA_CONTRACT.SIGNER_INFO"), signer));
			}
			message.addParams("CONTENTS_1", StringUtils.join(messageSignerContents, "<br/>")); 
			
			//add message
			if(this.pushManager.addMessage(message)) {
				Logger.warn.println(info.getSession("ID"), this, "add message fail > "+pushManager.getLastErrCode());
			}
		} catch (CipherException e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		
		return true;
	}
	
	public boolean doDocumentRewrite() {
		String formNo = this.docBasicInfo.get("CONT_FORM_NO");
		this.docBasicInfo.put("CONT_FORM_NO", "");
		
		String extFormNo = this.docBasicInfo.get("CONT_EXT_FORM_NO");
		this.docBasicInfo.put("CONT_EXT_FORM_NO", "");
		
		if(!this.doUpdateForm(formNo, extFormNo)) {
			return false;
		} else {
			if(!this.doUpdateStatus("T")) {
				return false;
			}
			
			if(!this.revokeAll()) {
				return false;
			}
		}
		
		return true;
	}
	
	/*
	 * History
	 */
	
	public List<Map<String, String>> doSelectHistory(){
		return doSelectSPMGT(this.docBasicInfo);
	}
	
	protected boolean addHistory(ACTION_TYPE action) {
		if(historyFlag) {
			Map<String, String> data = new HashMap<String, String>();
			data.put("ACTION", action.name());
			
			String ip = "";
			if(ThreadLocalFilter.tl != null) {
				ip = ThreadLocalFilter.getClientIP();
			}
			
			if(StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
				ip = info.getSession("IP");
				
				if(StringUtils.isBlank(ip)) {
					ip = this.config.get("sepoa.host.ip", "Server");
				}
			}
			
			data.put("IP", ip);
			
			if(!this.doInsertSPMGT(this.docBasicInfo, data)) {
				this.errCode = 100007;
				return false;
			}
		}
		return true;
	}
	
	/*
	 * etc
	 */
	public String getContractHash() {
		List<Map<String, String>> fileList = new LinkedList<Map<String, String>>();
		List<Map<String, String>> selectedList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_C"), LEVEL.FILE);
		if(selectedList != null) {
			fileList.addAll(selectedList);
		}
		selectedList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_E"), LEVEL.FILE);
		if(selectedList != null) {
			fileList.addAll(selectedList);
		}
		selectedList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_A"), LEVEL.FILE);
		if(selectedList != null) {
			fileList.addAll(selectedList);
		}
		selectedList = this.fileManager.doList(new SingletonMap<String, String>("LAYER", "CONT_B"), LEVEL.FILE);
		if(selectedList != null) {
			fileList.addAll(selectedList);
		}
	
		List<String> hashList = new ArrayList<String>(fileList.size());
		try {
			PathManager pm = new PathManager(info, config);
			for(Map<String, String> fileInfo : fileList) {
				File file = pm.getFile(fileInfo);
				if(file != null) {
					hashList.add(Files.hash(file, Hashing.sha512()).toString());
				}else {
					return null;
				}
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
			return null;
		}
		return StringUtils.join(hashList, "_");
	}
	
	@SuppressWarnings("unchecked")
	protected boolean documentRendering() {
		List<Map<String, String>> fileList = this.doListImage();
		if(fileList == null) {
			this.errCode = 100704;
			return false;
		}
		
		PathManager pm = new PathManager(info, config);
		for(Map<String, String> file : fileList) {
			String mergeFixHtml = MapUtils.getString(file, "FIX_HTML", "");
			Map<String, String> fixHtmlMap = new Gson().fromJson(mergeFixHtml, Map.class);
			if(fixHtmlMap != null) {
				// SepoaImageProcessor = HTML 문자열을 이미지화 하여 PDF 파일에 삽입
				SepoaImageProcessor sip = new SepoaImageProcessor(info, config, ctx);
				sip.setTempDirectory(this.tempStorage);
				sip.setPDF(file, true);
				
				Map<Integer, String> parsedFixHtmlMap = new HashMap<Integer, String>();
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					Integer pageNo = Integer.parseInt(entry.getKey()) - 1;
					String fixHtml = parseSepoaImage(entry.getValue());
					parsedFixHtmlMap.put(pageNo, fixHtml);
					
					// 현재 가로 너비 945px이 기준 (sepoa.contract.module.css의 .contract-editor-list-item-wrapper 참고)
					Map<SepoaImageProcessor.PageSize, String> sizeMap = new HashMap<>();
					sizeMap.put(SepoaImageProcessor.PageSize.WIDTH, "945");
					sip.setPageSize(pageNo, sizeMap);
				}
				
				try {
					File resultPDF = sip.process(parsedFixHtmlMap);
					if(resultPDF.exists()) {
						
						File oldFile = pm.getFile(file);
						this.fileManager.doDelete(file.get("FILE_ID"));
						FileUtils.moveFileToDirectory(oldFile, new File(this.tempStorage), false);
						
						Map<String, String> uploadFileData = new HashMap<String, String>();
						uploadFileData.put("PATH", resultPDF.getAbsolutePath());
						uploadFileData.put("FILE_NAME_CLIENT", file.get("FILE_NAME_CLIENT"));
						uploadFileData.put("DOC_SEQ", file.get("DOC_SEQ"));
						
						this.fileManager.setLayer(file.get("LAYER"));
						List<Map<String, String>> result = this.fileManager.doUploadFile(uploadFileData, null, false);
						if(fileManager.getLastErrCode() != 0) {
							this.errCode = 100401;
							return false;
						}
						
						Map<String, String> meta = new HashMap<>();
						meta.put("FILE_ID", result.get(0).get("FILE_ID"));
						meta.put("LAYER", file.get("LAYER"));
						meta.put("FIX_HTML", mergeFixHtml);
						
						this.fileManager.doModifyMeta(meta);
						int errCode = this.fileManager.getLastErrCode();
						if(errCode != 0) {
							this.errCode = 100204;
							return false;
						}
						
					} else {
						throw new Exception("SepoaImageProcessor process failed.");
					}
				} catch (Exception e) {
					e.printStackTrace();
					Logger.err.println(e.getMessage());
					this.errCode = 100705;
					return false;
				}
			}
		}
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	protected boolean createCompletePDF() {
		Map<String, Map<String, String>> fileSignHtmlMap = new HashMap<>();
		
		List<Map<String, String>> fileList = this.doListImage();
		if(fileList == null) {
			this.errCode = 100704;
			return false;
		}
		
		List<Map<String, String>> signerList = this.doSelectSigner();
		for(Map<String, String> signer : signerList) {
			String signFixHtml = signer.get("SIGN_FIX_HTML");
			if(!StringUtils.isBlank(signFixHtml)) {
				List<Map<String, String>> signerHtmlObject = (List<Map<String, String>>) SepoaJSON.toJSONObject(signFixHtml);
				for(Map<String, String> htmlPage : signerHtmlObject) {
					String fileId = htmlPage.get("FILE_ID");
					String pageNo = htmlPage.get("PART_ID");
					String fixHtml = htmlPage.get("FIX_HTML");
					
					if(!fileSignHtmlMap.containsKey(fileId)) {
						fileSignHtmlMap.put(fileId, new HashMap<>());
					}
					
					Map<String, String> signHtmlMap = fileSignHtmlMap.get(fileId);
					if(signHtmlMap.containsKey(pageNo)) {
						signHtmlMap.put(pageNo, signHtmlMap.get(pageNo) + fixHtml);
					} else {
						signHtmlMap.put(pageNo, fixHtml);
					}
				}
			}
		}
		
		List<File> pdfFileList = new ArrayList<>();
		PathManager pm = new PathManager(info, config);
		for(Map<String, String> file : fileList) {
			Map<String, String> fixHtmlMap = MapUtils.getMap(fileSignHtmlMap, file.get("FILE_ID"), null);
			if(fixHtmlMap != null) {
				SepoaImageProcessor sip = new SepoaImageProcessor(info, config, ctx);
				sip.setTempDirectory(this.tempStorage);
				sip.setPDF(file, true);
				
				Map<Integer, String> parsedFixHtmlMap = new HashMap<>();
				for(Entry<String, String> entry : fixHtmlMap.entrySet()) {
					Integer pageNo = Integer.parseInt(entry.getKey()) - 1;
					String fixHtml = parseSepoaImage(entry.getValue());
					parsedFixHtmlMap.put(pageNo, fixHtml);
					
					Map<SepoaImageProcessor.PageSize, String> sizeMap = new HashMap<>();
					sizeMap.put(SepoaImageProcessor.PageSize.WIDTH, "945");
					sip.setPageSize(pageNo, sizeMap);
				}
				
				try {
					File resultPDF = sip.process(parsedFixHtmlMap);
					if(resultPDF.exists()) {
						pdfFileList.add(resultPDF);
					}
				} catch(Exception e) {
					e.printStackTrace();
					Logger.err.println(e.getMessage());
					this.errCode = 100705;
					return false;
				}
			} else {
				try {
					File target = CipherManager.decrypt(pm.getFile(file), false);
					pdfFileList.add(target);
				} catch (CipherException e) {
					Logger.err.println(e);
					return false;
				}
			}
		}
		
		SepoaPDFDocument document = null;
		SepoaPDFModifier modifier = null;
		try {
			if(pdfFileList.size() > 0) {
				document = SepoaPDFDocument.load(pdfFileList.get(0));
				modifier = SepoaPDFModifier.load(document, this.config);
				
				for(File pdf : pdfFileList) {
					if(pdfFileList.indexOf(pdf) != 0) {
						modifier.setMergeDocument(pdf);
					}
				}
			} else {
				this.errCode = 100903;
				return false;
			}
			
			String completePdfFilePath = this.tempStorage+"/"+String.valueOf(System.nanoTime())+".pdf";
			
			//watermark
			String taskNumber = String.valueOf(System.nanoTime())+"_"+Thread.currentThread().getName();
			String customWatermarkService = config.get("sepoa.contract.watermark.service", null);
			if(StringUtils.isNotBlank(customWatermarkService)) {
				SepoaOut so = ServiceConnector.doService(info, customWatermarkService, ctx,"createPdfWatermark", new Object[] {taskNumber, this.getDocumentInfo()});
				if(so != null && so.flag) {
					if(so.getData(0) != null) {
						SepoaPDFContent content = (SepoaPDFContent) so.getData(0);
						modifier.addGlobalContent(content);
					}
				}else{
					this.errCode = 100904;
					return false;
				}
			}else {
				// TEXT watermark triggers getDocumentFonts NPE on some Chrome-captured PDFs.
				// Use configured PNG image watermark instead (same approach as CT_PDF_PLUGIN).
				String watermarkPath = config.get("sepoa.contract.watermark", null);
				if(StringUtils.isBlank(watermarkPath)) {
					watermarkPath = CommonUtil.getConfig("sepoa.contract.watermark");
				}
				File watermarkFile = StringUtils.isNotBlank(watermarkPath) ? new File(watermarkPath) : null;
				if(watermarkFile != null && watermarkFile.exists()) {
					SepoaPDFContentImage watermarkContent = new SepoaPDFContentImage(watermarkFile);
					watermarkContent.setOpacity(0.2);
					modifier.addGlobalContent(watermarkContent);
				} else {
					Logger.err.println(this, "watermark image not found: " + watermarkPath);
				}
			}
			
			File complatePdfFile = new File(completePdfFilePath);
			modifier.save(complatePdfFile);
			
			boolean result = complatePdfFile.exists(); 
			if(StringUtils.isNotBlank(customWatermarkService)) {
				ServiceConnector.doService(info, customWatermarkService, ctx,"createPdfAfterEvent", new Object[] {taskNumber, this.getDocumentInfo()});
			}
			
			if(!result) {
				this.errCode = 100904;
				return false;
			}
			
			Map<String, String> completeFileInfo = new HashMap<String, String>();
			completeFileInfo.put("FILE_TYPE"       , "CC");
			completeFileInfo.put("PATH"            , completePdfFilePath);
			completeFileInfo.put("FILE_NAME_CLIENT", this.docBasicInfo.get("CONT_SUBJECT")+".pdf"); //파일 명 [파일 다운로드시 사용되는 원본 파일명]
			
			if(this.doAddFile(completeFileInfo.get("FILE_TYPE"), completeFileInfo, true) == null) {
				this.errCode = 100905;
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.errCode = 100903;
			return false;
		} finally {
			try {
				if(document != null) {
					document.close();
				}
				
				if(pdfFileList != null && pdfFileList.size() > 0) {
					for(File pdfFile : pdfFileList) {
						if(pdfFile.exists()) {
							FileUtils.moveFileToDirectory(pdfFile, new File(this.tempStorage), false);
						}
					}
				}
			} catch (IOException e) {
				Logger.err.println(e.getMessage());
			}
		}
		
		//감사추적인증서 생성부
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public File createAuditTrail() {
		File result = null;
		
		try {
			File baseDir = new File(this.config.get("sepoa.doc.contract", "") + this.config.get("sepoa.contract.resource", ""));
			File template = new File (baseDir + File.separator + "auditTrail/template.html");
			if(template.exists() == false) {
				throw new Exception("Audit Trail Template File is not exists.");
			}
			
			String templateHtml = new String(FileUtils.readFileToByteArray(template), "UTF-8");
			
			Map<String, Object> data = new HashMap<>();
			
			/* 테이블 헤더 데이터 */
			Map<String, String> header = new HashMap<>();
			data.put("HEADER", header);
			
			header.put("MAIN_HEADER",					Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "MAIN_HEADER"));
			header.put("HEADER_CONT_SUBJECT",			Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_CONT_SUBJECT"));
			header.put("HEADER_CONT_NO",				Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_CONT_NO"));
			header.put("HEADER_CONT_METHOD",			Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_CONT_METHOD"));
			header.put("HEADER_STATUS",					Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_STATUS"));
			header.put("HEADER_DATE",					Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_DATE"));
			header.put("HEADER_PAGE",					Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_PAGE"));
			header.put("CREATOR_INFO",					Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "CREATOR_INFO"));
			header.put("HEADER_CREATOR",				Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_CREATOR"));
			header.put("HEADER_CREATOR_NAME",			Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_CREATOR_NAME"));
			header.put("HEADER_CREATOR_EMAIL",			Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_CREATOR_EMAIL"));
			header.put("SIGNER_INFO",					Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "SIGNER_INFO"));
			header.put("HEADER_SIGNER_NAME",			Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_SIGNER_NAME"));
			header.put("HEADER_SIGNER_COMPANY_NAME",	Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_SIGNER_COMPANY_NAME"));
			header.put("HEADER_SIGNER_EMAIL",			Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_SIGNER_EMAIL"));
			header.put("HEADER_SIGN",					Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_SIGN"));
			header.put("FILE_INFO",						Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "FILE_INFO"));
			header.put("HEADER_FILE_TYPE",				Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_FILE_TYPE"));
			header.put("HEADER_FILE_NAME",				Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_FILE_NAME"));
			header.put("HISTORY",						Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HISTORY"));
			header.put("HEADER_USER_IP",				Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_USER_IP"));
			header.put("HEADER_TIME_ENVIRONMENT",		Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_TIME_ENVIRONMENT"));
			header.put("HEADER_USER_ID_NAME",			Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_USER_ID_NAME"));
			header.put("HEADER_HISTORY_CONTENT",		Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_" + "HEADER_HISTORY_CONTENT"));
			
			Map<String, String> docInfo = new HashMap<>();
			data.put("DOC_INFO", docInfo);
			
			docInfo.putAll(this.docBasicInfo);
			if(docInfo.containsKey("STATUS")) {
				List<Map<String, String>> code = new Gson().fromJson(Cache.combo.get("SL0001,000#KO#CT002"), List.class);
				Optional<Map<String, String>> codeValue = code.stream().filter(e -> e.get("value").equals(this.docBasicInfo.get("STATUS"))).findFirst();
				if(codeValue.isPresent()) {
					docInfo.put("STATUS", codeValue.get().get("text"));
				}
			}
			
			if(docInfo.containsKey("CONT_METHOD")) {
				List<Map<String, String>> code = new Gson().fromJson(Cache.combo.get("SL0001,000#KO#CT003"), List.class);
				Optional<Map<String, String>> codeValue = code.stream().filter(e -> e.get("value").equals(this.docBasicInfo.get("CONT_METHOD"))).findFirst();
				if(codeValue.isPresent()) {
					docInfo.put("CONT_METHOD", codeValue.get().get("text"));
				}
			}
			
			Map<String, Object> fileInfo = new HashMap<>();
			data.put("FILE_INFO", fileInfo);
			
			List<Map<String, String>> documentList = this.doListImage();
			if(documentList.size() > 0) {
				Map<String, String> document = documentList.get(0);
				PathManager pm = new PathManager(info, config);
				File contractFile = pm.getFile(document);
				File decryptFile = CipherManager.decrypt(contractFile, false);
				SepoaPDFDocumentPDFBox pdf = (SepoaPDFDocumentPDFBox) SepoaPDFDocument.load(decryptFile);
				PDFRenderer renderer = new PDFRenderer(pdf.getPDDocument());
				BufferedImage thumbnail = renderer.renderImage(0, 0.5f);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(thumbnail, "PNG", baos);
				
				String base64Image = DatatypeConverter.printBase64Binary(baos.toByteArray());
				docInfo.put("THUMBNAIL_BASE64", base64Image);
				baos.close();
				pdf.close();
				
				int page = pdf.getPDDocument().getNumberOfPages();
				docInfo.put("PAGE", page + "");
				
				List<Map<String, String>> fileRowList = new ArrayList<>(documentList.size());
				fileInfo.put("ROWS", fileRowList);
				
				List<Map<String, String>> fileTypeCode = new Gson().fromJson(Cache.combo.get("SL0001,000#KO#CT007"), List.class);
				for(Map<String, String> doc : documentList) {
					String fileType = "";
					Optional<Map<String, String>> codeValue = fileTypeCode.stream().filter(e -> e.get("value").equals(doc.get("LAYER").replace("CONT_", ""))).findFirst();
					if(codeValue.isPresent()) {
						fileType = codeValue.get().get("text");
					}
					
					Map<String, String> row = new HashMap<>();
					fileRowList.add(row);
					
					row.put("FILE_TYPE", fileType);
					row.put("FILE_NAME_CLIENT", doc.get("FILE_NAME_CLIENT"));
				}
			}
			
			Map<String, Object> signerInfo = new HashMap<>();
			data.put("SIGNER_INFO", signerInfo);
			
			List<Map<String, String>> signerList = this.doSelectSSIGN(this.docBasicInfo);
			List<Map<String, Object>> signerRowList = new ArrayList<>(signerList.size());
			signerInfo.put("ROWS", signerRowList);
			
			for(Map<String, String> signer : signerList) {
				List<String> sign = new ArrayList<>();
				List<Map<String, String>> signFixHtmlList = new Gson().fromJson(signer.get("SIGN_FIX_HTML"), List.class);
				if(signFixHtmlList != null) {
					for(Map<String, String> signFixHtml : signFixHtmlList) {
						String parsedHtml = this.parseSepoaImage(signFixHtml.get("FIX_HTML"));
						Elements elements = new Element("div").html(parsedHtml).children();
						for(Element img : elements.select("img")) {
							img.attr("style", "width:50px;height:50px");
							if(sign.indexOf(img.attr("src")) == -1) {
								sign.add(img.attr("src"));
							}
						}
					}
				}
				
				if(sign.size() == 0) {
					sign.add("-");
				}
				
				Map<String, Object> row = new HashMap<>();
				row.putAll(signer);
				row.put("SIGN", sign);
				signerRowList.add(row);
			}
			
			Map<String, Object> historyInfo = new HashMap<>();
			data.put("HISTORY_INFO", historyInfo);
			
			SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			List<Map<String, String>> historyList = this.doSelectSPMGT(this.docBasicInfo);
			List<Map<String, String>> historyRowList = new ArrayList<>(historyList.size());
			historyInfo.put("ROWS", historyRowList);
			for(Map<String, String> history : historyList) {
				Map<String, String> row = new HashMap<>();
				historyRowList.add(row);
				
				row.putAll(history);
				row.put("ACTION", Cache.text.get("POA_CONTRACT.AUDIT_TRAIL_ACTION_" + history.get("ACTION")));
				
				String date = toDateFormat.format(fromDateFormat.parse(history.get("ADD_DATE") + history.get("ADD_TIME")));
				row.put("DATE", date);
				
				if(ACTION_TYPE.SIGN_COMPLETE.toString().equals(history.get("ACTION"))) {
					docInfo.put("DATE", date);
				}
			}
			
			String extraDataService = config.get("sepoa.contract.auditTrail.service", null);
			if(StringUtils.isNotBlank(extraDataService)) {
				SepoaOut so = ServiceConnector.doService(info, extraDataService, ctx, "getAuditTrailExtraData", new Object[] {data});
				if(so.status == 1) {
					if(so.getDataCount() > 0) {
						Map<String, Object> extraData = (Map<String, Object>) so.getData(0);
						data = extraData;
					}
				}
			}
			
			templateHtml = templateHtml.replace("#DATA#", new Gson().toJson(data));
			
			SepoaConverter sc = new SepoaConverter();
			sc.setSource(templateHtml);
			File tempPDF = sc.getPDF();
			
			result = new File(this.tempStorage + "/" + System.currentTimeMillis());
			FileUtils.moveFile(tempPDF, result);
			if(result == null || !result.exists()) {
				throw new Exception("Audit Trail File Create failed.");
			}
		} catch (Exception e) {
			this.errCode = 103342;
			e.printStackTrace();
		}
		
		return result;
	}
	
	
	protected Map<String, String> doSelectDocument(Map<String, String> condition){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			List<Map<String, String>> list = new SepoaFormater(ssm.doSelect(condition)).getListMap();
			if (list.size() > 0) {
				return list.get(0);
			}
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	protected boolean doUpdateDocument(Map<String, String> data){
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
	
	protected boolean doDeleteDocument(Map<String, String> data){
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
	
	protected Map<String, String> doInsertDocument(Map<String, String> data){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data);
	
			return this.doSelectDocument(data);
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	protected List<Map<String, String>> doSelectSCTMD(Map<String, String> condition){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			return new SepoaFormater(ssm.doSelect(condition)).getListMap();
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	protected boolean doInsertSCTMD(List<Map<String, String>> list, Map<String, String> map){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(list, map);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected boolean doDeleteSCTMD(Map<String, String> condition){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(condition);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected List<Map<String, String>> doSelectSSIGN(Map<String, String> condition){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			return new SepoaFormater(ssm.doSelect(condition)).getListMap();
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	protected boolean doInsertSSIGN(Map<String, String> data1, Map<String, String> data2){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data1, data2);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected boolean doUpdateSSIGN(Map<String, String> data1, Map<String, String> data2){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data1, data2);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	/**
	 * SIGN_FIX_HTML을 청크 단위로 업데이트 (기존 값 뒤에 concat)
	 * Oracle VARCHAR2/바인드 4000바이트 제한(ORA-01461) 대응
	 */
	protected boolean doUpdateSSIGNChunk(Map<String, String> data1, Map<String, String> data2){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data1, data2);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected boolean doDeleteSSIGN(Map<String, String> condition) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(condition);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected List<Map<String, String>> doSelectSPMGT(Map<String, String> condition){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			return new SepoaFormater(ssm.doSelect(condition)).getListMap();
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	protected boolean doInsertSPMGT(Map<String, String> data1, Map<String, String> data2) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data1, data2);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected boolean doDeleteSPMGT(Map<String, String> condition) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(condition);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected boolean doInsertSCTDS(List<Map<String, String>> data) {
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
	
	protected List<Map<String, String>> doSelectSCTDS(Map<String, String> data1, Map<String, String> data2){
		Map<String, String> data = new HashMap<String, String>(data1);
		data.putAll(data2);
		return this.doSelectSCTDS(data);
	}
	
	protected List<Map<String, String>> doSelectSCTDS(Map<String, String> data1){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			return new SepoaFormater(ssm.doSelect(data1)).getListMap();
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	protected boolean doUpdateSCTDS(Map<String, String> data1, Map<String, String> data2){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data1, data2);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected boolean doDeleteSCTDS(Map<String, String> data1) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(data1);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected List<Map<String, String>> doSelectSCTRV(Map<String, String> condition){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			return new SepoaFormater(ssm.doSelect(condition)).getListMap();
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return null;
	}
	
	protected boolean doInsertSCTRV(Map<String, String> data1, Map<String, String> data2){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doInsert(data1, data2);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected boolean doUpdateSCTRV(Map<String, String> data1, Map<String, String> data2){
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doUpdate(data1, data2);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected boolean doDeleteSCTRV(Map<String, String> condition) {
		try {
			SepoaXmlParser sxp = new SepoaXmlParser(this.getClass());
			SepoaSQLManager ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.setSepoaInfo(info);
			ssm.doDelete(condition);
			return true;
		}catch(Exception e) {
			Logger.err.println(info.getSession("ID"), this, e);
		}
		return false;
	}
	
	protected Map<String, String> getMessageParams(String type, Map<String, String> defaultParams){
		Map<String, String> messageParams = new HashMap<String, String>(this.docBasicInfo);
		if(defaultParams != null){
			messageParams.putAll(defaultParams);
		}
		
		messageParams.put("SUBJECT"       , "[" + Cache.text.get("POA_CONTRACT."+type) + "]" + this.docBasicInfo.get("CONT_SUBJECT"));
		messageParams.put("TITLE"         , Cache.text.get("POA_CONTRACT."+type));
		messageParams.put("TITLE_COMMENT" , Cache.text.get("POA_CONTRACT."+type+"_TITLE_COMMENT"));
		messageParams.put("NOTICE_COMMENT", Cache.text.get("POA_CONTRACT."+type+"_NOTICE_COMMENT"));
		messageParams.put("LABEL_1"       , Cache.text.get("POA_CONTRACT."+type+"_LABEL_1"));
		
		return messageParams;
	}
	
	protected String parseSepoaImage(String html) {
		String result = "";
		
		Elements elements = new Element("div").html(html).children();
		for(Element img : elements.select("img")) {
			Map<String, String> paramMap = new HashMap<>();
			String imgSrc = img.attr("src").substring(img.attr("src").indexOf("?") + 1);
			for(String param : imgSrc.split("&")) {
				String[] paramPair = param.split("=", 2);
				paramMap.put(paramPair[0], paramPair[1]);
			}
			
			SepoaOut so = ServiceConnector.doService(info, paramMap.get("PROCESS_ID"), "TRANSACTION", paramMap.get("METHOD"), new Object[] {paramMap});
			if(so.flag) {
				try {
					File image = new File(so.result[0]);
					if(image.exists()) {
						InputStream in = FileUtils.openInputStream(image);
						String imageBase64 = DatatypeConverter.printBase64Binary(IOUtils.toByteArray(in));
						img.attr("src", "data:image/png;base64," + imageBase64);
					}
				} catch(IOException e) {
					Logger.err.println(info.getSession("ID"), this, e);
				}
			}
		}
		
		result = elements.outerHtml();
		
		return result;
	}

	public void setMailSendYn(String mailSendYn) {
		
		this.docBasicInfo.put("MAIL_SEND_YN", mailSendYn);
		
	}
	
}
