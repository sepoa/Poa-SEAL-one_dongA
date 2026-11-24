package sepoa.fw.approval;

import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.approval.exception.SepoaApprovalConfirmException;
import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBOpenException;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public class SepoaApprovalConfirm extends SepoaApproval {

    public SepoaApprovalConfirm(SepoaInfo info) throws SepoaApprovalException {
        super(info);
    }

    public SepoaApprovalConfirm(ConnectionContext ctx, SepoaInfo info) throws SepoaApprovalException {
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
            throws SepoaApprovalConfirmException {
    	int confirmCount = 1;
    	boolean arbitraryDecisionFlag = false;
        try {

            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);

            // 전결로 들어온경우
            arbitraryDecisionFlag = headerData.containsKey(SepoaApproval.ARBITRARY_DECISION_TYPE_KEY);
            if(arbitraryDecisionFlag) {
                // 현재 결재자의 APP_TYPE을 가져온다
                // 전결처리시 해당 결재자와 같은 APP_TYPE인 건만 처리하기 위해 ( 일반, 관리 )
                sxp = new SepoaXmlParser(this.getClass(), "getAppLine");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(headerData));
                
                if(sf != null && sf.getRowCount() > 0) {
                	headerData.put("APP_TYPE", sf.getValue("APP_TYPE", 0));
                }
                
            	if(SepoaApproval.ARBITRARY_DECISION_TYPE_ALL.equals(MapUtils.getString(headerData, SepoaApproval.ARBITRARY_DECISION_TYPE_KEY))) {
                    sxp = new SepoaXmlParser(this.getClass(), "getArbitraryDecisionMaxCount");
                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                    sf = new SepoaFormater(ssm.doSelect(headerData));
                    if(sf != null && sf.getRowCount() > 0) {
                    	confirmCount += Integer.parseInt(sf.getValue(0, 0));
                    }
                    
            	} else {
            		confirmCount += Integer.parseInt(MapUtils.getString(headerData, SepoaApproval.ARBITRARY_DECISION_TYPE_KEY));
            	}
            }
            
            for(int arbitraryDecisionCount = 0 ; arbitraryDecisionCount < confirmCount ; arbitraryDecisionCount++) {
                // 결재라인정보를 읽어온다. 현재 결재자부터...
                sxp = new SepoaXmlParser(this.getClass(), "getAppLine");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                sf = new SepoaFormater(ssm.doSelect(headerData));

                // 마지막 결재자인지 확인하는 FLAG true-마지막결재, false-마지막결재가 아님.
                boolean lastApprovalLineCheckFlag = true;
                
                // 합의일 경우 해당 Stage합의자가 모두 합의했는지 확인. true-모두승인, false-합의자중 처리 안한사람이 있는경우
                boolean allAgreementCheckFlag = true;
                
                // 현재 Stage에 합의자중 몇명이 진행중인지 카운트를 센다.
                int progressAgreementCount = 0;
                
                
                String currentStage = "";
                String appStatus = "";
                String appLineStatus = "";
                
                
                
                for (int i = 0; i < sf.getRowCount(); i++) {
                    headerData.put("SUX_WKEY1" , sf.getValue("SUX_WKEY1" , i));
                    headerData.put("SUX_WKEY2" , sf.getValue("SUX_WKEY2" , i));
                    headerData.put("SUX_WKEY3" , sf.getValue("SUX_WKEY3" , i));
                    headerData.put("SUX_WKEY4" , sf.getValue("SUX_WKEY4" , i));
                    headerData.put("SUX_WKEY5" , sf.getValue("SUX_WKEY5" , i));
                    headerData.put("SUX_WKEY6" , sf.getValue("SUX_WKEY6" , i));
                    headerData.put("SUX_WKEY7" , sf.getValue("SUX_WKEY7" , i));
                    headerData.put("SUX_WKEY8" , sf.getValue("SUX_WKEY8" , i));
                    headerData.put("SUX_WKEY9" , sf.getValue("SUX_WKEY9" , i));
                    headerData.put("SUX_WKEY10", sf.getValue("SUX_WKEY10", i));
                    //현재라인이 결재,합의,참조에 따라 분기 처리
                    if (SepoaApproval.APP_METHOD_APPROVAL.equals(sf.getValue("APP_METHOD", i))) {
                        if(!sf.getValue("APP_USER_ID", i).equals(info.getSession("ID")) && arbitraryDecisionCount == 0){ //결재라인 아이디와 현재 Session이 다르고, 전결로 들어오지 않은경우. 
                            throw new SepoaApprovalConfirmException("CONFIRM_MSG_01");	
                        }
                        currentStage = sf.getValue("CURRENT_STAGE", i);
                        
                        // 현재 결재자가 마지막 결재자인지 확인
                        for (int j = i + 1; j < sf.getRowCount(); j++) {
                            // 현재결재라인모두 상위결재라인에서 결재할 건이나 합의할 건이 있을경우 마지막 결재자가 아니다.
                            if(SepoaApproval.APP_METHOD_APPROVAL.equals(sf.getValue("APP_METHOD", j))
                                    || SepoaApproval.APP_METHOD_AGREEMENT.equals(sf.getValue("APP_METHOD", j))){
                                lastApprovalLineCheckFlag = false;
                                currentStage = sf.getValue("APP_STAGE", j);
                                break;
                            }
                        }
                        
                        if(lastApprovalLineCheckFlag){
                            // 마지막 결재자일경우
                            appStatus = SepoaApproval.APP_STATUS_FINAL;
                            appLineStatus = SepoaApproval.APP_LINE_STATUS_COMPLETED;
                        }else{
                            // 마지막 결재자가 아니라면
                            appStatus = SepoaApproval.APP_STATUS_PROGRESS;
                            appLineStatus = SepoaApproval.APP_LINE_STATUS_CONFIRM;
                        }

                        headerData.put("CURRENT_STAGE", currentStage);
                        headerData.put("APP_STATUS", appStatus);
                        headerData.put("APP_LINE_SEQ", sf.getValue("APP_LINE_SEQ", i));
                        headerData.put("APP_LINE_STATUS", appLineStatus);
                        doStatusUpdate(info, ctx, allData);

                        if(!SepoaApproval.APP_STATUS_FINAL.equals(MapUtils.getString(headerData, "APP_STATUS", ""))){
                            // 결재라인 상신자 라인 완료상태로 변경
                            headerData.put("APP_STAGE", currentStage);
                            headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_PROGRESS);
                            sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatusStage");
                            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                            ssm.doUpdate(headerData);   
                        }else{
                            // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
                            SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_CONFIRM);
                            if (!so.flag) {
                                throw new SepoaApprovalConfirmException(so.message);
                            }
                        }
                        break;
                        // 완료일 경우는 결재완료 후 실행되는 업무프로세스를 호출한다.
                    } else if (SepoaApproval.APP_METHOD_AGREEMENT.equals(sf.getValue("APP_METHOD", i))) {
                    	// 합의일경우 전결처리 불가
                    	if(arbitraryDecisionFlag) {
                    		throw new SepoaApprovalConfirmException("CONFIRM_MSG_03");
                    	}
                        String tempStage = "";
                        if(!sf.getValue("APP_USER_ID", i).equals(info.getSession("ID"))){
                            if(!"".equals(tempStage) && !tempStage.equals(sf.getValue("APP_STAGE", i))){
                                throw new SepoaApprovalConfirmException("CONFIRM_MSG_02");
                            }
                            tempStage = sf.getValue("APP_STAGE", i);
                        }else{
                            // 합의인 경우 
                            currentStage = sf.getValue("CURRENT_STAGE", i);

                            // 결재라인에서 동일한 Stage의 합의자를 검색한다.
                            for (int j = 0 ; j < sf.getRowCount(); j++) {
                                // 동일한 스테이지의 합의자중에 진행중인건이 몇건인지 확인한다.
                                if ((currentStage.equals(sf.getValue("APP_STAGE", j))
                                        && SepoaApproval.APP_LINE_STATUS_PROGRESS.equals(sf.getValue("APP_LINE_STATUS", j)))
                                        || sf.getValue("APP_USER_ID", j).equals(info.getSession("ID"))) {
                                    progressAgreementCount++;
                                }
                            }
                            // 2명이상일 경우는 현재 합의자를 제외하고, 아직 합의를 진행하지 않은 사람이 있는경우이다.
                            if (1 < progressAgreementCount) {
                                allAgreementCheckFlag = false;
                            }
                            
                            if(allAgreementCheckFlag){
                                // 현재 합의자를 제외 모두 합의를 진행한경우

                                // 현재 합의자가 마지막 결재라인인지 체크
                                for (int j = i + 1; j < sf.getRowCount(); j++) {
                                    if(SepoaApproval.APP_METHOD_APPROVAL.equals(sf.getValue("APP_METHOD", j))
                                            || SepoaApproval.APP_METHOD_AGREEMENT.equals(sf.getValue("APP_METHOD", j))){
                                        if(!currentStage.equals(sf.getValue("APP_STAGE", j))){
                                            lastApprovalLineCheckFlag = false;
                                            currentStage = sf.getValue("APP_STAGE", j);
                                            break;   
                                        }
                                    }
                                }

                                if(lastApprovalLineCheckFlag){
                                    // 마지막 결재자일경우
                                    appStatus = SepoaApproval.APP_STATUS_FINAL;
                                    appLineStatus = SepoaApproval.APP_LINE_STATUS_COMPLETED;
                                }else{
                                    // 마지막 결재자가 아니라면
                                    appStatus = SepoaApproval.APP_STATUS_PROGRESS;
                                    appLineStatus = SepoaApproval.APP_LINE_STATUS_CONFIRM;
                                }

                                headerData.put("CURRENT_STAGE", currentStage);
                                headerData.put("APP_STATUS", appStatus);
                                headerData.put("APP_LINE_SEQ", sf.getValue("APP_LINE_SEQ", i));
                                headerData.put("APP_LINE_STATUS", appLineStatus);
                                doStatusUpdate(info, ctx, allData);

                                if(!SepoaApproval.APP_STATUS_FINAL.equals(MapUtils.getString(headerData, "APP_STATUS", ""))){
                                    // 결재라인 상신자 라인 완료상태로 변경
                                    headerData.put("APP_STAGE", currentStage);
                                    headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_PROGRESS);
                                    sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatusStage");
                                    ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                                    ssm.doUpdate(headerData);   
                                }else{
                                    // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
                                    SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_CONFIRM);
                                    if (!so.flag) {
                                        throw new SepoaApprovalConfirmException(so.message);
                                    }
                                }
                            }else{
                                // 현재 합의자외에 합의를 진행해야 할 경우
                                appStatus = SepoaApproval.APP_STATUS_PROGRESS;
                                appLineStatus = SepoaApproval.APP_LINE_STATUS_CONFIRM;
                                headerData.put("CURRENT_STAGE", currentStage);
                                headerData.put("APP_STATUS", appStatus);
                                headerData.put("APP_LINE_SEQ", sf.getValue("APP_LINE_SEQ", i));
                                headerData.put("APP_LINE_STATUS", appLineStatus);
                                doStatusUpdate(info, ctx, allData);
                            }
                            break;
                        }
                    } 
                }
            }
        } catch (Exception e) {
            throw new SepoaApprovalConfirmException(e);
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
            throws SepoaApprovalConfirmException {
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
    @SuppressWarnings("unchecked")
    @Override
    public void doNotApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalConfirmException {
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            headerData.put("CURRENT_STAGE", "2");
            headerData.put("APP_STATUS", SepoaApproval.APP_STATUS_FINAL);
            headerData.put("APP_LINE_SEQ", "2");
            headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_COMPLETED);
            doStatusUpdate(info, ctx, allData);

            // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
            SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_CONFIRM);
            if (!so.flag) {
                throw new SepoaApprovalConfirmException(so.message);
            }
        } catch (Exception e) {
            throw new SepoaApprovalConfirmException(e);
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
    @SuppressWarnings("unchecked")
    @Override
    public void doDirectApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalConfirmException {
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            headerData.put("CURRENT_STAGE", "2");
            headerData.put("APP_STATUS", SepoaApproval.APP_STATUS_FINAL);
            headerData.put("APP_LINE_SEQ", "2");
            headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_COMPLETED);
            doStatusUpdate(info, ctx, allData);

            // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
            SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_CONFIRM);
            if (!so.flag) {
                throw new SepoaApprovalConfirmException(so.message);
            }
        } catch (Exception e) {
            throw new SepoaApprovalConfirmException(e);
        }
    }

    /**
     * 
     * 결재라인 승인시 결재라인 Update
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalConfirmException 
     */
    @SuppressWarnings("unchecked")
    public void doStatusUpdate(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalConfirmException {
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            // 결재라인 헤더정보에 결재진행으로 상태값 변경
            sxp = new SepoaXmlParser(this.getClass(), "setApprovalHeaderStatus");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);

            // 결재라인 상신자 라인 완료상태로 변경
            sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatus");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);

        } catch (DBOpenException e) {
            throw new SepoaApprovalConfirmException(e);
        }
    }

}
