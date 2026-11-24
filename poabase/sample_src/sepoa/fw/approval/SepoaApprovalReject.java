package sepoa.fw.approval;

import java.util.Map;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.approval.exception.SepoaApprovalRejectException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
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
            
            // 현재 결재라인에 해당하는 담당자가 맞는지 검사. ( 로우가 존재한다면 해당 담당자가 맞다 )
            headerData.put("APP_USER_ID", info.getSession("ID"));
            sxp = new SepoaXmlParser(this.getClass(), "getAppUserCheck");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
                    
            if(sf != null && sf.getRowCount() > 0){
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
