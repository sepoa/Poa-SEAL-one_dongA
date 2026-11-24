package sepoa.svc.cbo.qna.postprocess;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.axis2.databinding.types.soapencoding.Array;

import sepoa.fw.cache.Cache;
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
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;
import sepoa.svl.util.SepoaMultipart;

@Description("QnA 등록 후처리 프로세서")
public class SepoaQnaPostProcessor extends SepoaService{
	public SepoaQnaPostProcessor(String opt, SepoaInfo info) throws SepoaServiceException {
		super(opt, info);
		setVersion("1.0.0");
	}
	
	@Description("QnA 댓글 등록 후처리 프로세서")
	public SepoaOut doReplyProcess(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
            Configuration conf = new Configuration();
            sxp = new SepoaXmlParser(this, "doSelectReWriter");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            List<Map<String, String>> replyList = new ArrayList<Map<String, String>>();
            List<Map<String, String>> sendList = new ArrayList<>();
            Set<Map<String, String>> convertMap = new HashSet<>();
            
            if(sf.getListMap().size() > 0) {
            	for(Map<String, String> rowData : sf.getListMap()) {
            		// 작성자를 제외한 댓글 사용자들 목록 추가
            		if(!rowData.get("ADD_USER_ID").equals(info.getSession("ID"))) { // 세션(작성자와) 아이디가 다르면
            			Map<String, String> tmpData = new HashMap<String, String>();
            			tmpData.put("ADD_USER_ID", 		rowData.get("ADD_USER_ID"));
            			tmpData.put("USER_NAME_LOC", 	rowData.get("USER_NAME_LOC"));
            			tmpData.put("EMAIL", 			rowData.get("EMAIL"));
            			replyList.add(tmpData);
            		}
            	}
            	
            	// 중복 리스트 제거
                for (Map<String, String> map : replyList) {
                    if (!convertMap.contains(map)) {
                    	sendList.add(map);
                        convertMap.add(map);
                    }
                }
            }
            
            if(sf.getRowCount() > 0) {
            	Map<String, String> writerData = sf.getListMap().get(0);
            	// 메일 발송 진행
            	SepoaPushManager mailpush = new SepoaPushManager(info, conf);
            	Map<String, String> mailData = new HashMap<String, String>();
            	
            	SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
				SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
				
				SimpleDateFormat fromTimeFormat = new SimpleDateFormat("HHmmss");
				SimpleDateFormat toTimeFormat = new SimpleDateFormat("HH:mm");
				
            	mailData.put("SUBJECT", Cache.text.get("MAIL.QNA_REPLY_MAIL_TILE"));
            	mailData.put("MAIL_SUBJECT", Cache.text.get("MAIL.QNA_REPLY_MAIL_HEADER"));
            	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.QNA_REPLY_TITLE"));
            	mailData.put("CONTENTS", Cache.text.get("MAIL.QNA_REPLY_CONTENTS"));
            	mailData.put("CONTENTS_TITLE", headerData.get("SUBJECT"));
            	mailData.put("QNA_NUMBER", headerData.get("QNA_NUMBER"));
            	mailData.put("ADD_DATE", toDateFormat.format(fromDateFormat.parse(writerData.get("ADD_DATE"))));
            	mailData.put("ADD_TIME", toTimeFormat.format(fromTimeFormat.parse(writerData.get("ADD_TIME"))));
            	mailData.put("URL", CommonUtil.getConfig("sepoa.system.domain.name"));
            	
            	
            	for (Map<String, String> map : sendList ) {
            		SepoaMessage message = new SepoaMessage();
            		message.addParams(mailData);
            		message.setMessageType("QNA_REPLAY_ALARM");
            		message.addReceiver(map.get("ADD_USER_ID"), map.get("USER_NAME_LOC"), map.get("EMAIL"), null);
            		if(headerData.containsKey("ATTACH_NO")) {
            			sxp = new SepoaXmlParser(this, "doSelectAttachFile");
            			ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            			sf = new SepoaFormater(ssm.doSelect(headerData));
            			
            			if(sf.getRowCount() > 0) {
            				for(Map<String, String> fileData : sf.getListMap()) {
            					String tmpType = fileData.get("TYPE");
            					String tmpFileRootPath = SepoaMultipart.getTypePath(tmpType, "upload");
            					String tmpDesFilePath = fileData.get("DES_FILE_NAME");
            					String tmpSrcFileName = fileData.get("SRC_FILE_NAME");
            					File tmpFile = new File(tmpFileRootPath + "/" + tmpDesFilePath);
            					if(tmpFile.exists()) {
            						message.addAttach(new FileInputStream(tmpFile), tmpSrcFileName);
            					}
            				}
            			}
            		}
            		
            		mailpush.addMessage(message);
            		mailpush.accept();
            	}
				
            } else {
            	setStatus(120003);
            	throw new Exception("Not Found Legal Check Writer Info!");
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
	
	
	@Description("QnA 등록 후처리 프로세서")
	public SepoaOut doWriteProcess(Map<String, Object> allData) {
		setStatus(1);
		setFlag(true);
		SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        ConnectionContext ctx = getConnectionContext();
        
		try {
			Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
            Configuration conf = new Configuration();
            
			sxp = new SepoaXmlParser(this, "doSelectQnaManager");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            if(sf.getRowCount() > 0) {
            	SepoaMessage message = new SepoaMessage();
            	List<Map<String, String>> qnaManagerList = sf.getListMap();
            	
               	SimpleDateFormat fromDateFormat = new SimpleDateFormat("yyyyMMdd");
				SimpleDateFormat toDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    				
				SimpleDateFormat fromTimeFormat = new SimpleDateFormat("HHmmss");
				SimpleDateFormat toTimeFormat = new SimpleDateFormat("HH:mm");
            	
            	for(Map<String, String> qnaManager : qnaManagerList) {
            		sxp = new SepoaXmlParser(this, "doSelectQnaData");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    sf = new SepoaFormater(ssm.doSelect(headerData));
                    
                    if(sf.getRowCount() > 0) {
                    	Map<String, String> legalData = sf.getListMap().get(0);
                    	
                    	// 메일 발송 진행
                    	SepoaPushManager mailpush = new SepoaPushManager(info, conf);
                    	Map<String, String> mailData = new HashMap<String, String>();
                    	
                    	mailData.put("SUBJECT", Cache.text.get("MAIL.QNA_WRITE_MAIL_TITLE"));
                    	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.QNA_WRITE_TITLE"));
                    	mailData.put("CONTENTS", Cache.text.get("MAIL.QNA_WRITE_CONTENTS"));
                    	mailData.put("QNA_NUMBER", legalData.get("QNA_NUMBER"));
                    	mailData.put("CONTENTS_TITLE", legalData.get("SUBJECT"));
                    	mailData.put("ADD_DATE", toDateFormat.format(fromDateFormat.parse(legalData.get("ADD_DATE"))));
                    	mailData.put("ADD_TIME", toTimeFormat.format(fromTimeFormat.parse(legalData.get("ADD_TIME"))));
                    	mailData.put("URL", CommonUtil.getConfig("sepoa.system.domain.name"));
                    	
    					message.addParams(mailData);
    					message.setMessageType("QNA_WRITE_ALARM");
    					message.addReceiver(qnaManager.get("USER_ID"), qnaManager.get("USER_NAME_LOC"), qnaManager.get("EMAIL"), null);
    					
    					if(headerData.containsKey("ATTACH_NO")) {
    						sxp = new SepoaXmlParser(this, "doSelectAttachFile");
    	                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    	                    sf = new SepoaFormater(ssm.doSelect(headerData));
    	                    
    	                    if(sf.getRowCount() > 0) {
    	                    	for(Map<String, String> fileData : sf.getListMap()) {
    	                    		String tmpType = fileData.get("TYPE");
    	                    		String tmpFileRootPath = SepoaMultipart.getTypePath(tmpType, "upload");
    	                    		String tmpDesFilePath = fileData.get("DES_FILE_NAME");
    	                    		String tmpSrcFileName = fileData.get("SRC_FILE_NAME");
    	                    		File tmpFile = new File(tmpFileRootPath + "/" + tmpDesFilePath);
    	                    		if(tmpFile.exists()) {
    	                    			message.addAttach(new FileInputStream(tmpFile), tmpSrcFileName);
    	                    		}
    	                    	}
    	                    }
    					}
    					
    					mailpush.addMessage(message);
                    	mailpush.accept();
                    } else {
                    	setStatus(120001);
                    	throw new Exception("Not Found Legal Check Data!");
                    }
            	}
            } else {
            	setStatus(120002);
            	throw new Exception("Not Found Legal Check Manager!");
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
}
