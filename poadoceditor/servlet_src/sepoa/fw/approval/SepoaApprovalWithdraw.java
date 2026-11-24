package sepoa.fw.approval;

import java.util.Map;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.approval.exception.SepoaApprovalWithdrawException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.log.Logger;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public class SepoaApprovalWithdraw extends SepoaApproval {

    public SepoaApprovalWithdraw(SepoaInfo info) throws SepoaApprovalException {
        super(info);
    }

    public SepoaApprovalWithdraw(ConnectionContext ctx, SepoaInfo info) throws SepoaApprovalException {
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
    public void doSolution(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalWithdrawException {
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            // 결재라인정보를 읽어온다. 현재 결재순번이전 결재라인 확인.
            headerData.put("APP_METHODS", SepoaApproval.APP_METHOD_REPORT + "," + SepoaApproval.APP_METHOD_AGREEMENT
                    + "," + SepoaApproval.APP_METHOD_APPROVAL);
            headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_PROGRESS);
            sxp = new SepoaXmlParser(this.getClass(), "getAppLine");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            //첫번째 라인에 마지막 결재자 정보가 들어있다.
            if (sf != null && sf.getRowCount() > 0) {
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
                boolean approvalCheck = false;
                for (int j = 0; j < sf.getRowCount(); j++) {
                    if (sf.getValue("APP_STAGE", 0).equals(sf.getValue("APP_STAGE", j))
                            && sf.getValue("APP_USER_ID", 0).equals(info.getSession("ID"))) {
                        approvalCheck = true;
                    }
                }
                if (!approvalCheck) {
                    throw new SepoaApprovalWithdrawException("WITHDRAW_MSG_01");
                }

                if (SepoaApproval.APP_METHOD_REPORT.equals(sf.getValue("APP_METHOD", 0))) {
                    
                    // 마지막 결재정보가 상신일 경우
                    headerData.put("APP_STATUS", SepoaApproval.APP_STATUS_TEMP);
                    headerData.put("CURRENT_STAGE", "1");
                    headerData.put("APP_LINE_STATUS", "");
                    headerData.put("APP_LINE_SEQ", "N");
                    headerData.put("ACCEPT_USER_ID", "");
                    headerData.put("ACCEPT_DATE", "");
                    headerData.put("ACCEPT_TIME", "");
                    headerData.put("ACCEPT_REASON", "");
                    // 현재 디테일 진행상태 값 모두 삭제
                    sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatus");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doUpdate(headerData);
                    
                    // 헤더데이타 임시저장상태로 변경
                    sxp = new SepoaXmlParser(this.getClass(), "setApprovalHeaderStatus");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doUpdate(headerData);


                    // BackProcess호출
                    SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_WITHDRAW);
                    if (!so.flag) {
                        throw new SepoaApprovalWithdrawException(so.message);
                    } else {
                    	
                    	//회수 메일 발송
                    	try {
                    		Logger.debug.println("##### 회수 메일 발송 #####");
                    		Object[] obj = { allData, "SEND_APPROVAL_WITHDRAW", ctx };
                    		ServiceConnector.doService(info, "SendMessage", "NONDBJOB", "sendMessage", obj);
                    		
                    	} catch (Exception e2) {
                    		Logger.err.println("$$$$$ 회수 메일 발송 실패 $$$$$");
                    	}
                    }
                    
                } else if (!SepoaApproval.APP_METHOD_REPORT.equals(sf.getValue("APP_METHOD", 0))) {
                    
                    // 결재나 합의일 경우
                    headerData.put("APP_STATUS", SepoaApproval.APP_STATUS_PROGRESS);
                    headerData.put("CURRENT_STAGE", sf.getValue("APP_STAGE", 0));
                    headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_PROGRESS);
                    headerData.put("APP_LINE_SEQ", sf.getValue("APP_LINE_SEQ", 0));
                    headerData.put("ACCEPT_USER_ID", "");
                    headerData.put("ACCEPT_DATE", "");
                    headerData.put("ACCEPT_TIME", "");
                    headerData.put("ACCEPT_REASON", "");
                    // 마지막 결재자 디테일 정보 진행중 상태로 변경
                    sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatus");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doUpdate(headerData);

                    headerData.put("APP_LINE_STATUS", "");
                    // 마지막 결재자 디테일 정보 진행중 상태로 변경
                    sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatusOverSeq");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doUpdate(headerData);
                    
                    // 헤더정보에 마지막 결재자 Stage업데이트
                    sxp = new SepoaXmlParser(this.getClass(), "setApprovalHeaderStatus");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    ssm.doUpdate(headerData);

                }
            }

        } catch (Exception e) {
            throw new SepoaApprovalWithdrawException(e);
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
    public void doGroupware(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalWithdrawException {
        // 그룹웨어는.......
    }

    /**
     * 
     * 결재를 진행하지 않는 건에 대해서는 임의 1인 결재완료 상태로 만든다.
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalWithdrawException 
     */
    @Override
    public void doNotApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalWithdrawException {
        //결재를 진행하지 않는데 취소할리가 있나~

    }

    /**
     * 
     * 결재요청상태로 만들고 업무단에서 승인을 받는 경우.
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalWithdrawException 
     */
    @SuppressWarnings("unchecked")
    @Override
    public void doDirectApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalWithdrawException {
        try {
            // 그냥 결재올라간건을 그냥 삭제처리한다.
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

            sxp = new SepoaXmlParser(this.getClass(), "delApprovalHeader");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);

            sxp = new SepoaXmlParser(this.getClass(), "delApprovalLine");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doDelete(headerData);
            // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
            SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_WITHDRAW);
            if (!so.flag) {
                throw new SepoaApprovalWithdrawException(so.message);
            }
        } catch (Exception e) {
            throw new SepoaApprovalWithdrawException(e);
        }
    }

}
