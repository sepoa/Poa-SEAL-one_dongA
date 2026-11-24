package sepoa.fw.approval;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;

import com.google.gson.Gson;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.approval.exception.SepoaApprovalProgressException;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBOpenException;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.push.SepoaMessage;
import sepoa.fw.push.SepoaPushManager;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.srv.module.SepoaModuleService;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public class SepoaApprovalProgress extends SepoaApproval {

    public SepoaApprovalProgress(SepoaInfo info) throws SepoaApprovalException {
        super(info);
    }

    public SepoaApprovalProgress(ConnectionContext ctx, SepoaInfo info) throws SepoaApprovalException {
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
    public void doSolution(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalProgressException {
        try {

            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            sxp = new SepoaXmlParser(this.getClass(), "getAppLine");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            if(sf.getRowCount() > 0){
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
            }
            doStatusUpdate(info, ctx, allData);
            // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
            SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_PROGRESS);
            if (!so.flag) {
                throw new SepoaApprovalProgressException(so.message);
            }
        } catch (Exception e) {
        	e.printStackTrace();
            throw new SepoaApprovalProgressException(e);
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
    public void doGroupware(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalProgressException {
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
    public void doNotApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalProgressException {
        try {
            doStatusUpdate(info, ctx, allData);
            // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
            SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_PROGRESS);
            if (!so.flag) {
                throw new SepoaApprovalProgressException(so.message);
            }
        } catch (Exception e) {
            throw new SepoaApprovalProgressException(e);
        }
        
    }

    /**
     * 
     * 결재요청상태로 만들고 업무단에서 승인을 받는 경우.
     * 
     * @param ctx
     * @param allData
     * @return
     */
    @Override
    public void doDirectApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalProgressException {
        try {
            doStatusUpdate(info, ctx, allData);
            // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
            SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_PROGRESS);
            if (!so.flag) {
                throw new SepoaApprovalProgressException(so.message);
            }
        } catch (Exception e) {
            throw new SepoaApprovalProgressException(e);
        }
    }
    
    /**
     * 
     * 결재라인 진행시 결재라인 Update
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalProgressException 
     */
    @SuppressWarnings("unchecked")
    public void doStatusUpdate(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData) throws SepoaApprovalProgressException {
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            Configuration conf = new Configuration();

            // 결재라인 상신자 상신상태로 변경
            headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_REPORT);
            headerData.put("APP_LINE_SEQ", "0");
            sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatus");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);

            // 다음 결재라인 진행중 상태로 변경
            if(sf != null && sf.getRowCount()>1){
                for(int i = 0 ; i < sf.getRowCount() ; i++){
                    if(SepoaApproval.APP_METHOD_REFERENCE.equals(sf.getValue("APP_METHOD", i)) || 
                            SepoaApproval.APP_METHOD_REPORT.equals(sf.getValue("APP_METHOD", i))){
                        continue;
                    }else{
                        headerData.put("APP_STAGE", sf.getValue("APP_STAGE", i));
                        break;
                    }
                }
            }else{
                headerData.put("APP_STAGE", "1");
            }
            
            if ("".equals(MapUtils.getString(headerData, "APP_STAGE", ""))) {
                throw new SepoaApprovalProgressException("PROGRESS_MSG_01");
            }
            
            headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_PROGRESS);
            sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatusStage");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);   


            headerData.put("CURRENT_STAGE", MapUtils.getString(headerData, "APP_STAGE"));
            // 결재라인 헤더정보에 결재진행으로 상태값 변경
            headerData.put("APP_STATUS", SepoaApproval.APP_STATUS_PROGRESS);
            sxp = new SepoaXmlParser(this.getClass(), "setApprovalHeaderStatus");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);
            
            //다음 결재자에게 메일 발송
            if(SepoaModuleService.checkBusiness(new Configuration(), info, "MAIL1649307822120")) {
	            try {
	            	String jsonStr = headerData.get("PARAMETER").replaceAll("&#34;", "\"");
	     			Map<String, Object> parseMap = new Gson().fromJson(jsonStr, Map.class);
	     			Map<String, String> appInfo = (Map<String, String>) parseMap.get("APP_INFO");

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
                	mailData.put("SUBJECT", Cache.text.get("MAIL.APPROVAL_REQUEST"));
                	mailData.put("TIME", format.format(new Date()));
                	mailData.put("REQUESTER", info.getSession("NAME_LOC"));
                	mailData.put("STEP", Cache.text.get("MAIL.APPROVAL_PROGRESS"));
                	mailData.put("CONTENTS_NAME", Cache.text.get("MAIL.REQUEST_COMMENT"));
                	mailData.put("CONTENTS", appInfo.get("APP_COMMENT"));
                	
                	if(allData.containsKey("MAIL_CUSTOM_DATA")) {
                		mailData.putAll((Map<String, String>) allData.get("MAIL_CUSTOM_DATA"));
                	}
                	
                	SepoaMessage message = new SepoaMessage();
					message.addParams(mailData);
					message.setMessageType("APPROVAL_REQ");
					mailpush.addMessage(message);
                	mailpush.accept();
                	
	        		Logger.debug.println("##### 결재요청 메일 발송 #####");
				} catch (Exception e2) {
					Logger.err.println("$$$$$ 결재요청 메일 발송 실패 $$$$$");
				}
            }
        } catch (Exception e) {
            throw new SepoaApprovalProgressException(e);
        }
    }

}
