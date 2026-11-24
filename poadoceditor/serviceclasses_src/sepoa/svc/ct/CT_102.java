package sepoa.svc.ct;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.tika.Tika;

import com.google.gson.Gson;

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
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaDate;
import sepoa.fw.util.SepoaFormater;
import sepoa.fw.ux.link.SepoaLinkManager;

@Description("계약현황")
public class CT_102 extends SepoaService {
	
	public CT_102(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	
	
	@Description("카테고리 변경")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doChangeCategory(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(headerData);
			
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@Description("만기도래 알람 설정 변경")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doChangeExpiredAlarm(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			sxp = new SepoaXmlParser();
			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
			ssm.doUpdate(headerData);
			
			Commit();
		} catch (Exception e) {
			Rollback();
			e.printStackTrace();
			setStatus(0);
			setValue("");
			setFlag(false);
			setMessage(e.getMessage());
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
	
	@SuppressWarnings("unchecked")
	@Description("수기계약서 파일 조회 - dummy")
	public SepoaOut doListManualFile(Map<String, String> data) {
		setStatus(1);
		setFlag(true);
		try {
			String dummy = new SepoaFormater(new SingletonMap("MAX_SEQ", "0")).getString();
			setValue(dummy);
			setValue(dummy);
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
	
	//@Description("수기계약서 파일 업로드")
	//public SepoaOut doSaveManualFile(Map<String, String> data) {
	//	setStatus(1);
	//	setFlag(true);
    //
	//	ConnectionContext ctx = getConnectionContext();
	//	SepoaXmlParser sxp = null;
	//	SepoaSQLManager ssm = null;
	//	SepoaFormater sf = null;
	//	List<File> trashFileList = new ArrayList<File>();
    //
	//	try {
	//		String srcFilePath = SepoaContractMultipart.getFilePath(data.get("TYPE"), data.get("UNIQ_FILE_NAME"));
	//		File srcFile = new File(srcFilePath);
	//		trashFileList.add(srcFile);
    //
	//		if("TEMP".equalsIgnoreCase(data.get("TYPE")) && srcFile.exists()) {
	//			trashFileList.add(srcFile.getParentFile());
	//		}
    //
	//		String[] contractInfo = data.get("ATTACH_KEY").split("[_]");
	//		
	//		//validation
	//		Map<String, String> conditionMap = new HashMap<String, String>();
	//		conditionMap.put("CONT_NO", contractInfo[0]);
	//		conditionMap.put("CONT_GL_SEQ", contractInfo[1]);
	//		conditionMap.put("TYPE", "CONTRACT_MANUAL_SAVE");
    //
	//		SepoaOut so = ServiceConnector.doService(info, this.getClass().getName(), ctx, "doQuery", new Object[] {new SingletonMap(SepoaDataMapper.KEY_HEADER_DATA, conditionMap)});
	//		sf = new SepoaFormater(so.result[0]);
    //
	//		if(sf.getRowCount() <= 0) {
	//			throw new Exception(Cache.text.get("CT_102.MESSAGE0006"));
	//		}
    //
	//		SepoaAuthorityObject sao = SepoaAuthorityManager.getManager(info, ctx)
	//				.getObject(
	//						  sf.getValue("HOUSE_CODE", 0)
	//						, sf.getValue("COMPANY_CODE", 0)
	//						, "CONT"
	//						, sf.getValue("CONT_NO", 0)
	//						, sf.getValue("CONT_GL_SEQ", 0)
	//		);
	//		if(!sao.isWritable()) {
	//			throw new Exception(Cache.text.get("CT_102.MESSAGE0011"));
	//		}
    //
	//		if(!"S".equals(sf.getValue("CONT_METHOD", 0))) {
	//			throw new Exception(Cache.text.get("CT_102.MESSAGE0007"));
	//		}
    //
	//		String status = sf.getValue("CONT_STATUS", 0);
	//		if(!("A".equals(status) || "G".equals(status))) {
	//			throw new Exception(Cache.text.get("CT_102.MESSAGE0008"));
	//		}
	//		
	//		//clean before data
	//		if("1".equals(data.get("ATTACH_SEQ"))) {
	//			sxp = new SepoaXmlParser(this, "doDeleteSCTLN");
	//			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	//			ssm.doDelete(conditionMap);
    //
	//			sxp = new SepoaXmlParser(this, "doDeleteSCTSE");
	//			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	//			ssm.doDelete(conditionMap);
	//		}
    //
	//		//file save
	//		String layer = "contract";
	//		String rootPathType = "CONTRACT";
    //
	//		if(!isDocument(srcFilePath, data.get("FILE_NAME"))) {
	//			throw new Exception(Cache.text.get("CT_102.MESSAGE0009"));
	//		}
    //
	//		String fileItemNumber = DocumentUtil.getDocNumber(info, "CONT_ITEM").result[0];
    //
	//		String destTypePath = SepoaContractMultipart.getTypePath(rootPathType, "upload");
	//		String destFilePath = "/" + SepoaContractMultipart.generateFilePath(info, layer, fileItemNumber) +"/"+ fileItemNumber+ "/" + RandomStringUtils.randomAlphabetic(4) + String.valueOf(System.nanoTime());
	//		File destFile = new File(destTypePath + destFilePath);
    //
	//		if(!destFile.getParentFile().exists()) {
	//			FileUtils.forceMkdir(destFile.getParentFile());
	//			Logger.info.println(info.getSession("ID"), this, "mkdirs : "+destFile.getParent());
	//		}
    //
	//		if(destFile.exists()) {
	//			FileUtils.forceDelete(destFile);
	//			Logger.info.println(info.getSession("ID"), this, "delete : "+destFile.getAbsolutePath());
	//		}
	//		
	//		Logger.debug.println(info.getSession("ID"), this, "from : " + srcFile.getAbsolutePath());
	//		Logger.debug.println(info.getSession("ID"), this, "to : " + destFile.getAbsolutePath());
    //
	//		FileUtils.moveFile(srcFile, destFile);
    //
	//		if(!destFile.exists()) {
	//			throw new Exception(Cache.text.get("CT_102.MESSAGE0010"));
	//		}
    //
	//		Map<String, String> fileData = new HashMap<String, String>();
	//		fileData.put("CONT_ITEM_NO"  , fileItemNumber);
	//		fileData.put("UPLOAD_SEQ"    , data.get("ATTACH_SEQ"));
	//		fileData.put("SRC_FILE_NAME" , data.get("FILE_NAME"));
	//		fileData.put("DES_FILE_NAME" , destFilePath);
	//		fileData.put("FILE_SIZE"     , String.valueOf(destFile.length()));
	//		fileData.put("ROOT_PATH_TYPE", rootPathType);
    //
	//		List<Map<String, String>> fileList = new ArrayList<Map<String, String>>();
	//		fileList.add(fileData);
    //
	//		Map<String, String> contInfo = new HashMap<String, String>();
	//		contInfo.put("mode", "convert");
	//		contInfo.put("FILE_TYPE", "C");
	//		contInfo.put("CONVERT_FLAG", "true");
	//		contInfo.putAll(conditionMap);
    //
	//		Map<String, Object> newFileDataMap = new HashMap<String, Object>();
	//		newFileDataMap.put("CONT_INFO", contInfo);
	//		newFileDataMap.put("FILE_DATA", fileList);
	//		newFileDataMap.put("NOT_COMMIT_FLAG", "true");
    //
	//		//save ctln, ctse (convert)
	//		so = ServiceConnector.doService(info, "sepoa.svc.ct.CT_020_C", ctx, "doUploadDocument", new Object[] {newFileDataMap});
    //
	//		if(!so.flag) {
	//			throw new SepoaContractConvertException(so.message);
	//		}else {
	//			sxp = new SepoaXmlParser(this, "doCompleteManual");
	//			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	//			conditionMap.put("CONTRACT_FILE" , destFilePath);
	//			conditionMap.put("ROOT_PATH_TYPE", rootPathType);
    //
	//			ssm.doUpdate(conditionMap);
	//		}
    //
	//		Commit();
    //
    //
	//	} catch (Exception e) {
	//		Rollback();
	//		e.printStackTrace(); 
	//		setStatus(0);
	//		setValue("");
	//		setFlag(false);
	//		setMessage(e.getMessage());
	//		Logger.err.println(info.getSession("ID"), this, e);
	//	} finally {
	//		try {
	//			for(File file : trashFileList) {
	//				if(file.exists()) {
	//					FileUtils.forceDelete(file);
	//				}
	//			}
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//	}
	//	return getSepoaOut();
	//}
    //
	//@Description("수기계약 완료 메일 보내기 서비스")
	//public SepoaOut sendMailUploadFinish(Map<String, String> allData) {
	//	setStatus(1);
	//	setFlag(true);
	//	
	//	Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
    //
	//	Object[] mailObj = {headerData, SendMessage.HAND_CONT_UPLOAD_FINISH };
	//	new ThreadServiceConnector(info, "SendMessage", "CONNECTION", "sendMessage", mailObj);
    //
	//	return getSepoaOut();
	//}

	private boolean isDocument(String src, String srcFileName) {
		try {
			Config conf = new Configuration();
			String mimeType = new Tika().detect(new File(src));
			
			String ext = srcFileName.substring(srcFileName.lastIndexOf('.') + 1).toLowerCase();
			if(conf.getBoolean("sepoa.converter.ms.active", false) && Arrays.asList(conf.get("sepoa.converter.ms.ext").split(";")).contains(ext)) {
				return true;
			} else if(conf.getBoolean("sepoa.converter.hwp.active", false) && Arrays.asList(conf.get("sepoa.converter.hwp.ext").split(";")).contains(ext)) {
				return true;	
			} else if("pdf".equalsIgnoreCase(ext)) {
				return true;
			} else if(mimeType.matches(".*/tiff") || mimeType.matches("image/.*")) {
				return true;
			}
		} catch(Exception e) {
			Logger.err.println(e);
		}
		return false;
	}
	
	@Description("계약서 서명요청")
	@SuppressWarnings({ "unchecked" })
	public SepoaOut doSignRequest(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		ConnectionContext ctx = getConnectionContext();
		SepoaXmlParser sxp = null;
		SepoaSQLManager ssm = null;
		try {
			Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
			List<Map<String, String>> gridData = (List<Map<String, String>>) MapUtils.getObject(allData, SepoaDataMapper.KEY_RAW_GRID_DATA);
			Configuration conf = new Configuration();

			Map<String, String> rowData = gridData.get(0);
			//권한관리
			List<Map<String, String>> docAuthList = new ArrayList<Map<String, String>>();
			
			sxp = new SepoaXmlParser(this, "doQuerySigner");
			ssm = new SepoaSQLManager(info.getSession("ID"),this,ctx,sxp);
			SepoaFormater signerSf = new SepoaFormater(ssm.doSelect(rowData));
			if(signerSf != null  && signerSf.getRowCount() < 1) {
				throw new Exception(Cache.text.get("POA_CONTRACT.STATUS_200051"));//권한이 없는 사용자입니다.
			}
			
			sxp = new SepoaXmlParser(this, "doQueryManager");
			ssm = new SepoaSQLManager(info.getSession("ID"),this,ctx,sxp);
			SepoaFormater sf_p = new SepoaFormater(ssm.doSelect(headerData));
			for(Map<String,String> m :sf_p.getListMap()) {
				Map<String, String> idAuth = new HashMap<String, String>();
				if (m.get("MANAGER_ID") == null || m.get("MANAGER_ID").trim().isEmpty()) {
					continue;
				}
				idAuth.put("COMPANY_CODE", m.get("COMPANY_CODE"));
				idAuth.put("TARGET_CODE", m.get("MANAGER_ID"));
				idAuth.put("PERMISSION", "READ");
				idAuth.put("EMAIL", m.get("EMAIL"));
				idAuth.put("NAME", m.get("MANAGER_NAME"));

				docAuthList.add(idAuth);
			}
			
			if (docAuthList.size() > 0) {
				//System.out.println(docAuthList);
				
				// 로고 지정
    			String logo = "http://onesource.sepoa.co.kr/010/form/default/img/logo.png";
    			String logoAttachKey = "D"+info.getSession("HOUSE_CODE")+"LOGINLOGO";
    			headerData.put("LOGO_ATTACH_KEY", logoAttachKey);

    			sxp = new SepoaXmlParser("sepoa.svc.ct.CT_205", "getLogo");
    			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    			SepoaFormater sf_logo = new SepoaFormater(ssm.doSelect(headerData));
    			if (!"0".equals(sf_logo.getValue("SEQ",0))) {
    				logo = conf.get("sepoa.system.domain.name")+"/SepoaImage.sux?attach_key="+logoAttachKey+"&attach_seq="+sf_logo.getValue("SEQ",0)+"&HOUSE_CODE="+info.getSession("HOUSE_CODE"); 
    			}
    			
    			Map<String, String> linkParams = new HashMap<String, String>();
    			linkParams = new HashMap<String, String>();
    			linkParams.put("CONT_NO", rowData.get("CONT_NO"));
    			linkParams.put("CONT_GL_SEQ", rowData.get("CONT_GL_SEQ"));
    			linkParams.put("CONT_MODULE", "SIGN");
    			SepoaLinkManager linkManager = new SepoaLinkManager(info, conf, ctx);
    			linkManager.newLink("CONTRACT",rowData.get("CONT_NO"), rowData.get("CONT_GL_SEQ"));
    			linkManager.setParams(linkParams);
    			String privateLink = linkManager.addUser(
    					docAuthList.get(0).get("COMPANY_CODE"), 
    					headerData.get("APPROVAL_ID"),
						linkParams);
    			
	        	Map<String, String> mailData = new HashMap<String, String>();
    			mailData.put("LOGO", logo);
    			mailData.put("LINK", privateLink);
    			
    			mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
    			mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
    			mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
    			mailData.put("DEPARTMENT", info.getSession("DEPARTMENT"));
    			
    			mailData.put("SUBJECT", Cache.text.get("CT_102.SIGN_REQUEST_MAIL"));//계약서 서명이 요청되었습니다.
    			mailData.put("MAIL_TITLE", rowData.get("CONT_SUBJECT"));
    			
	        	mailData.put("TITLE1", Cache.text.get("CT_102.CONT_NO"));//계약번호
	        	mailData.put("CONTENT1", rowData.get("CONT_NO"));
    			
    			mailData.put("TITLE2", Cache.text.get("CT_102.CONT_SUBJECT"));//계약명
    			mailData.put("CONTENT2", rowData.get("CONT_SUBJECT"));
    			
    			mailData.put("TITLE3", Cache.text.get("CT_102.CONT_MANAGER"));//담당자
    			mailData.put("CONTENT3", info.getSession("NAME_LOC"));

    			Calendar cal = Calendar.getInstance();
    			cal.setTime(new Date());
    			mailData.put("TIME", String.valueOf(cal.getTimeInMillis()));
    			
    			//mailData.put("LINK", conf.get("sepoa.system.domain.name"));
				
    			// 담당자 각각 메일 전송
				for (Map<String, String> user: docAuthList) {
	    			// 담당자 권한 추가
					user.put("TARGET_USER_ID", user.get("TARGET_CODE"));
					user.put("CONT_NO", rowData.get("CONT_NO"));
					user.put("CONT_GL_SEQ", rowData.get("CONT_GL_SEQ"));
					
					System.out.println(user);
	    			sxp = new SepoaXmlParser(this, "doQueryPermission");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					SepoaFormater sf_auth = new SepoaFormater(ssm.doSelect(user));
	    			
					if (sf_auth.getRowCount() < 1) {
						sxp = new SepoaXmlParser(this, "doInsertPermission");
						ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
						ssm.doInsert(user);
					}
					sxp = new SepoaXmlParser(this, "doSignerUpdateSSIGN");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(user);
					sxp = new SepoaXmlParser(this, "doSignerUpdateSPCSI");
					ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
					ssm.doInsert(user);
					

					sxp = new SepoaXmlParser(this, "doQuerySignerInfo");
					ssm = new SepoaSQLManager(info.getSession("ID"),this,ctx,sxp);
					SepoaFormater sf = new SepoaFormater(ssm.doSelect(user));
					if(sf != null  && sf.getRowCount() > 0) {
						mailData.put("SIGNER_ID", sf.getValue("SIGNER_ID", 0));
						
						Map<String, String> returnMap = new HashMap<String, String>();
						returnMap.put("returnType", "param");
						returnMap.put("returnMessage", Cache.text.get("POA_CONTRACT.MANUAL_COMPLETE_LABEL_1") + " : " + signerSf.getValue("SIGNER_NAME", 0) + " [ " + signerSf.getValue("SIGNER_ID", 0) + " ] ");
						setObject(returnMap);
					}
					
					// 메일 보내기
					SepoaMessage message = new SepoaMessage();
	    			message.addParams(mailData);
	    			message.setMessageType("SIGN_REQUEST");
					message.addReceiver(user.get("TARGET_USER_ID"),user.get("NAME"), user.get("EMAIL"), "");
					SepoaPushManager spm = new SepoaPushManager(info, new Configuration());
					spm.addMessage(message);
	    			spm.accept();
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
			Logger.err.println(info.getSession("ID"), e);
		}
		return getSepoaOut();
	}
}
