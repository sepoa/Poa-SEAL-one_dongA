package sepoa.svc.board;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

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
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.DocumentUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

@SuppressWarnings({ "unchecked" })
@Description("Q&A 게시판")
public class SepoaQAService extends SepoaService {

    public SepoaQAService(String opt, SepoaInfo info) throws SepoaServiceException {
        super(opt, info);
        setVersion("1.0.0");
    }
    
    public String getConfig(String s) {
        return CommonUtil.getConfig(s);
    }
    
    private String cleanXSS(String value) {
       	value = value.toLowerCase();
    	
    		value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    		value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
    		value = value.replaceAll("'", "&#39;");
    		value = value.replaceAll("eval\\((.*)\\)", "");
    		value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
    		value = value.replaceAll("script", "");
    		value = value.replaceAll("alert", "");
    		value = value.replaceAll("onerror", "");
    		value = value.replaceAll("append", "");
    		value = value.replaceAll("applet", "");
    		value = value.replaceAll("base", "");
    		value = value.replaceAll("bgsound", "");
    		value = value.replaceAll("binding", "");
    		value = value.replaceAll("blink", "");
    		value = value.replaceAll("charset", "");
    		value = value.replaceAll("cookie", "");
    		value = value.replaceAll("create", "");
    		value = value.replaceAll("embed", "");
    		value = value.replaceAll("expression", "");
    		value = value.replaceAll("frame", "");
    		value = value.replaceAll("frameset", "");
    		value = value.replaceAll("href", "");
    		value = value.replaceAll("iframe", "");
    		value = value.replaceAll("innerHTML", "");
    		value = value.replaceAll("layer", "");
    		value = value.replaceAll("link", "");
    		value = value.replaceAll("meta", "");
    		value = value.replaceAll("msgbox", "");
    		value = value.replaceAll("object", "");
    		value = value.replaceAll("refresh", "");
    		value = value.replaceAll("vbscript", "");
    		value = value.replaceAll("script", "");
    		value = value.replaceAll("string", "");
    		value = value.replaceAll("style", "");
    		value = value.replaceAll("title", "");
    		value = value.replaceAll("void", "");
    		value = value.replaceAll("xml", "");
    		value = value.replaceAll("FSCommand", "");		
    		value = value.replaceAll("onafter", "");
    		value = value.replaceAll("onbefore", "");
    		value = value.replaceAll("onbegin", "");
    		value = value.replaceAll("onblur", "");		
    		value = value.replaceAll("onbounce", "");
    		value = value.replaceAll("onchange", "");
    		value = value.replaceAll("oncontext", "");		
    		value = value.replaceAll("onbounce", "");
    		value = value.replaceAll("oncell", "");
    		value = value.replaceAll("onchange", "");
    		value = value.replaceAll("onclick", "");		
    		value = value.replaceAll("oncopy", "");		
    		value = value.replaceAll("oncut", "");		
    		value = value.replaceAll("ondata", "");		
    		value = value.replaceAll("ondbClick", "").replaceAll("ondbclick", "");		
    		value = value.replaceAll("ondrag", "");		
    		value = value.replaceAll("ondrop", "");		
    		value = value.replaceAll("onfilter", "");		
    		value = value.replaceAll("onfinish", "");		
    		value = value.replaceAll("onfocus", "");		
    		value = value.replaceAll("onmouse", "");		
    		value = value.replaceAll("onkey", "");		
    		value = value.replaceAll("onlayout", "");		
    		value = value.replaceAll("onlose", "");		
    		value = value.replaceAll("onload", "");		
    		value = value.replaceAll("onmove", "");		
    		value = value.replaceAll("onout", "");		
    		value = value.replaceAll("onprogress", "");		
    		value = value.replaceAll("onresize", "");		
    		value = value.replaceAll("onrow", "");		
    		value = value.replaceAll("onscroll", "");		
    		value = value.replaceAll("onseek", "");		
    		value = value.replaceAll("onselect", "");		
    		value = value.replaceAll("onstart", "");		
    		value = value.replaceAll("onstop", "");		
    		value = value.replaceAll("onsync", "");		
    		value = value.replaceAll("onsubmit", "");		
    		value = value.replaceAll("ontime", "");		
    		value = value.replaceAll("ontrack", "");		
    		value = value.replaceAll("onunload", "");		
    		value = value.replaceAll("onurlflip", "");		
    		value = value.replaceAll("seeksegmenttime", "");
		return value;
	}

    /**
     * Q&A 목록 조회 
     * @param allData
     * @return
     */
    @Description("Q&A 조회")
    public SepoaOut doQuery(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            
            if (info.getSession("CTRL_CODE").contains("MANAGE_CONFIRM") && !"S".equals(info.getSession("USER_TYPE"))) {
            	headerData.put("COMPANY_CODE_CONDITION", "N");
            } else {
            	headerData.put("COMPANY_CODE_CONDITION", "Y");
            }
            sxp = new SepoaXmlParser();
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData));
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
    
    /**
     * Q&A 상세 조회 
     * @param allData
     * @return
     */
    @Description("Q&A 상세 조회")
    public SepoaOut getDetail(Map<String, Object> allData) {
        setStatus(1);
        setFlag(true);
        ConnectionContext ctx = getConnectionContext();
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        try {
            Map<String, String> headerData = MapUtils.getMap(allData, SepoaDataMapper.KEY_HEADER_DATA);
            sxp = new SepoaXmlParser(this, "getQnaDetail");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            setValue(ssm.doSelect(headerData));
            
            //	Reply 존재 여부 확인
            sxp = new SepoaXmlParser(this, "getQnaReply");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            //	Reply 존재할 경우 Reply 목록을 담는다
            if(sf.getRowCount() > 0) {            	
            	setValue(ssm.doSelect(headerData));
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
    
    /**
     * Q&A 등록 , 수정, 댓글 등록
     * @param allData
     * @return
     */
    @Description("Q&A 글 / 댓글 등록, 수정")
    public SepoaOut doSave(Map<String, Object> allData) {
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        boolean nextFlag = false;
        ConnectionContext ctx = getConnectionContext();
        
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);
            Configuration conf = new Configuration();

            String qnaNumber = MapUtils.getString(headerData, "QNA_NUMBER", "");
            if("".equals(qnaNumber)){	// Q&A 등록
            	qnaNumber = DocumentUtil.getDocNumberRemote(info, "QNA").result[0];
                headerData.put("QNA_NUMBER", qnaNumber);
                
                sxp = new SepoaXmlParser(this, "insertQna");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doInsert(headerData);
                
                //QNA 작성 후처리 모듈
                if(SepoaModuleService.checkBusiness(new Configuration(), info, "DSB1684477026257")) {
                	SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.qna.postprocess.SepoaQnaPostProcessor", ctx, "doWriteProcess", new Object[] {allData});
					if(out == null) {
						setStatus(70009);
						Logger.err.println("QnA PostProcess Result Null!");
					} else if(out.status != 1 || !out.flag) {
						setStatus(out.status);
						Logger.err.println("QnA PostProcess Fail!");
					}
                }
            }else{
            	String mode = MapUtils.getString(headerData, "mode", "");
            	if("EDIT".equals(mode)) {	// Q&A 수정
    	            sxp = new SepoaXmlParser(this, "checkReply");
    	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
    	            sf = new SepoaFormater(ssm.doSelect(headerData));
    	            if (sf.getRowCount() > 0) {
        				setStatus(2);
        	            setFlag(false);
        	            setMessage(Cache.text.get("QA_002.MESSAGE_CANNOT_UPDATE")); 
        	            nextFlag = true;
    	            } else {
	            		sxp = new SepoaXmlParser(this, "updateQna");
	                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                    ssm.doUpdate(headerData);
    	            }
            	}else if("REPLY".equals(mode)){	// Q&A 답글 등록
            		sxp = new SepoaXmlParser(this, "insertReply");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doInsert(headerData);
                    
                    //QNA 댓글 작성 후처리 모듈
                    if(SepoaModuleService.checkBusiness(new Configuration(), info, "DSB1684744519396")) {
                    	SepoaOut out = ServiceConnector.doService(info, "sepoa.svc.cbo.qna.postprocess.SepoaQnaPostProcessor", ctx, "doReplyProcess", new Object[] {allData});
    					if(out == null) {
    						setStatus(70009);
    						Logger.err.println("QnA Reply PostProcess Result Null!");
    					} else if(out.status != 1 || !out.flag) {
    						setStatus(out.status);
    						Logger.err.println("QnA Reply PostProcess Fail!");
    					}
                    }
            	}
            }
            
            if (nextFlag == false) {
	            Commit();
	            setStatus(1);
            }
        } catch (Exception e) {
            Rollback();
            if(getStatus() == 1) setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    /**
     * Q&A 삭제 , 댓글 삭제
     * @param allData
     * @return
     */
    @Description("Q&A 글 / 댓글 삭제")
    public SepoaOut doDelete(Map<String, Object> allData) {
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        SepoaFormater sf = null;
        boolean chk_exception = false;        
        ConnectionContext ctx = getConnectionContext();
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_RAW_HEADER_DATA);

            if (info.getSession("CTRL_CODE").contains("MANAGE_CONFIRM")) {
            	headerData.put("ADD_USER_ID_CONDITION", "N");
            } else {
            	headerData.put("ADD_USER_ID_CONDITION", "Y");
            }
			
        	String mode = MapUtils.getString(headerData, "mode", "");
        	
        	if("".equals(mode)) {
	            sxp = new SepoaXmlParser(this, "checkReply");
	            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	            sf = new SepoaFormater(ssm.doSelect(headerData));
	            
	            if (sf.getRowCount() > 0) {
    				setStatus(2);
    	            setFlag(false);
    	            setMessage(Cache.text.get("QA_002.MESSAGE_CANNOT_UPDATE")); 
    	            chk_exception = true;
	            } else {    
	        		sxp = new SepoaXmlParser(this, "deleteQna");
	                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
	                ssm.doUpdate(headerData);
	            }
        	}else if("REPLY".equals(mode)){
        		sxp = new SepoaXmlParser(this, "deleteReply");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doInsert(headerData);
        	}
        	
        	if (chk_exception == false) {
	        	Commit();
	            setStatus(1);
        	}
        } catch (Exception e) {
        	Rollback();
            setStatus(0);
            setMessage(e.toString());
            Logger.err.println(info.getSession("ID"), this, e);
        }
        return getSepoaOut();
    }
    
    private List<String> getSplitString(String t1, int length) {
        List<String> rt = new LinkedList<String>();
        String ui = t1;

        int le = getLengthb(ui);
        
        while (le > 0 && t1 != null) {
            rt.add(getSubString(ui, 0, length));
            ui = getSubString(ui, length, le);
            le = getLengthb(ui);
        }

        return rt;
    }
    
    private int getLengthb(String str) {
        int rSize = 0;
        int len = 0;

        for (; rSize < str.length(); rSize++) {
            if (str.charAt(rSize) > 127) {
                len += 2;
            } else {
                len++;
            }
        }

        return len;
    }

    private String getSubString(String str, int start, int end) {
        int rSize = 0;
        int len = 0;

        StringBuffer ss = new StringBuffer();

        for (; rSize < str.length(); rSize++) {
            if (str.charAt(rSize) > 0x007F) {
                len += 2;
            } else {
                len++;
            }
            if ((len > start) && (len <= end)) {
                ss.append(str.charAt(rSize));
            }
        }

        return ss.toString();
    }
}