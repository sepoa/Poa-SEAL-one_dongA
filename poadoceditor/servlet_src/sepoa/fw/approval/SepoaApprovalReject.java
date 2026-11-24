package sepoa.fw.approval;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.approval.exception.SepoaApprovalRejectException;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public class SepoaApprovalReject extends SepoaApproval {

    public SepoaApprovalReject(SepoaInfo info) throws SepoaApprovalException {
        super(info);
    }

    public SepoaApprovalReject(ConnectionContext ctx, SepoaInfo info) throws SepoaApprovalException {
        super(ctx, info);
    }

    /**
     * 
     * 세포아 솔루션 내부결재 시스템일 경우
     * 
     * @param ctx
     * @param allData
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public void doSolution(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalRejectException {

        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            Configuration conf = new Configuration();
            // 현재 결재라인에 해당하는 담당자가 맞는지 검사. ( 로우가 존재한다면 해당 담당자가 맞다 )
            //headerData.put("APP_USER_ID", info.getSession("ID"));
            sxp = new SepoaXmlParser(this.getClass(), "getAppUserCheck");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
                    
            if(sf.getValue("APP_USER_ID ",0).equals(info.getSession("ID")) || sf.getValue("RECE_USER_ID ",0).equals(info.getSession("ID"))){
            //if(!sf.getValue("APP_USER_ID", i).equals(info.getSession("ID")) && !sf.getValue("RECE_USER_ID", i).equals(info.getSession("ID"))) {
                headerData.put("SUX_WKEY1" , sf.getValue("SUX_WKEY1" , 0));
                headerData.put("SUX_WKEY2" , sf.getValue("SUX_WKEY2" , 0));
                headerData.put("SUX_WKEY3" , sf.getValue("SUX_WKEY3" , 0));
                headerData.put("SUX_WKEY4" , sf.getValue("SUX_WKEY4" , 0));
                headerData.put("SUX_WKEY5" , sf.getValue("SUX_WKEY5" , 0));
                headerData.put("SUX_WKEY6" , sf.getValue("SUX_WKEY6" , 0));
                headerData.put("SUX_WKEY7" , sf.getValue("SUX_WKEY7" , 0));
                headerData.put("SUX_WKEY8" , sf.getValue("SUX_WKEY8" , 0));
                headerData.put("SUX_WKEY9" , sf.getValue("SUX_WKEY9" , 0));
                headerData.put("SUX_WKEY10", sf.getValue("SUX_WKEY10", 0));
                // 결재 헤더 테이블 반려상태로 만든다.
                headerData.put("APP_STATUS", SepoaApproval.APP_STATUS_REJECT);
                sxp = new SepoaXmlParser(this.getClass(), "doRejectHeader");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doUpdate(headerData);
                
                // 결재 디테일 테이블 반려상태로 만든다.
                headerData.put("APP_LINE_SEQ", sf.getValue("APP_LINE_SEQ", 0));
                headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_REJECT);
                sxp = new SepoaXmlParser(this.getClass(), "doRejectDetail");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doUpdate(headerData);
                
                // 결재 반려 후 처리되는 업무 프로세스를 호출한다.
                SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_REJECT);
                if (!so.flag) {
                    throw new SepoaApprovalRejectException(so.message);
                }
                
                // 요청자에게 결재반려 메일 발송
                if(SepoaModuleService.checkBusiness(new Configuration(), info, "MAIL1649307822120")) {
    	            try {
    	            	sxp = new SepoaXmlParser(this.getClass(), "searchApprovalWriter");
                    	ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    	sf = new SepoaFormater(ssm.doSelect(headerData));
                    	
    	     			// 메일 발송 진행
    	            	SepoaPushManager mailpush = new SepoaPushManager(info, conf);
                    	Map<String, String> mailData = new HashMap<String, String>();
    	            	
                    	SimpleDateFormat format = new SimpleDateFormat ("yyyy년 MM월 dd일 HH시 mm분");
                    	
                    	mailData.put("HOUSE_CODE", info.getSession("HOUSE_CODE"));
                    	mailData.put("COMPANY_CODE", info.getSession("COMPANY_CODE"));
                    	mailData.put("LANGUAGE", info.getSession("LANGUAGE"));
                    	mailData.put("APP_NO", headerData.get("APP_NO"));
                    	mailData.put("APP_NO_SEQ", headerData.get("APP_NO_SEQ"));
                    	
                    	mailData.put("MAIL_TITLE", Cache.text.get("MAIL.SEAL_MAIL_TITLE"));
                    	mailData.put("SUBJECT", Cache.text.get("MAIL.APPROVAL_REJECT"));
                    	mailData.put("REQUESTER", sf.getValue("APP_USER_NAME", 0));
                    	mailData.put("TIME", format.format(new Date()));
                    	mailData.put("STEP", Cache.text.get("MAIL.APP_STATUS_REJECT"));
                    	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REJECT_COMMENT"));
                    	mailData.put("CONTENTS", headerData.get("ACCEPT_REASON"));
                    	
                    	HttpServletRequest req = (HttpServletRequest) allData.get(SepoaDataMapper.KEY_REQUEST);
        	 			String baseURL = req.getRequestURL().toString().replace(req.getRequestURI(),"");
        	 			mailData.put("URL",baseURL);
                    	
        	 			if(allData.containsKey("MAIL_CUSTOM_DATA")) {
                    		mailData.putAll((Map<String, String>) allData.get("MAIL_CUSTOM_DATA"));
                    	}
        	 			
                    	SepoaMessage message = new SepoaMessage();
    					message.addParams(mailData);
    					message.setMessageType("APPROVAL_REJECT");
    					mailpush.addMessage(message);
                    	mailpush.accept();
                    	
    	        		Logger.debug.println("##### 반려 메일 발송 #####");
    				} catch (Exception e2) {
    					Logger.err.println("$$$$$ 반려 메일 발송 실패 $$$$$");
    				}
                }
                
            }else{
                throw new SepoaApprovalRejectException("REJECT_MSG_01");
            }
        } catch (Exception e) {
            throw new SepoaApprovalRejectException(e);
        }
    }
    

    /**
     * 
     * 그룹웨어 결재라인
     * 
     * 결재라인 상태값을 모두 그룹웨어에서 받아와야한다.
     * 
     * @param ctx
     * @param allData
     * @return
     */
    @Override
    public void doGroupware(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalRejectException {
        // 그룹웨어는.......
    }

    /**
     * 
     * 결재를 진행하지 않는 건에 대해서는 임의 1인 결재완료 상태로 만든다.
     * 
     * @param ctx
     * @param allData
     * @return
     */
    @Override
    public void doNotApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalRejectException {
        // 결재를 진행하지 않으니 반려도 없다.
    }

    /**
     * 
     * 결재요청상태로 만들고 업무단에서 승인을 받는 경우.
     * 
     * @param ctx
     * @param allData
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public void doDirectApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalRejectException {
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

            // 현재결자
            sxp = new SepoaXmlParser(this.getClass(), "setApprovalHeaderStatus");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
                    
            if(sf != null && sf.getRowCount() > 0){
                // 결재 헤더 테이블 반려상태로 만든다.
                headerData.put("APP_STATUS", SepoaApproval.APP_STATUS_REJECT);
                sxp = new SepoaXmlParser(this.getClass(), "setApprovalHeaderStatus");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doUpdate(headerData);
                
                // 결재 디테일 테이블 반려상태로 만든다.
                headerData.put("APP_LINE_SEQ", sf.getValue("APP_LINE_SEQ", 0));
                headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_REJECT);
                sxp = new SepoaXmlParser(this.getClass(), "setApprovalHeaderStatus");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doUpdate(headerData);
                
                // 결재 반려 후 처리되는 업무 프로세스를 호출한다.
                SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_REJECT);
                if (!so.flag) {
                    throw new SepoaApprovalRejectException(so.message);
                }
            }else{
                throw new SepoaApprovalRejectException("REJECT_MSG_02");
            }
        } catch (Exception e) {
            throw new SepoaApprovalRejectException(e);
        }
    }
}
