package sepoa.fw.approval;

import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.approval.exception.SepoaApprovalProgressException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.DBOpenException;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
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
            

            // 결재라인 상신자 라인 상신상태로 변경
            headerData.put("APP_LINE_STATUS", SepoaApproval.APP_LINE_STATUS_REPORT);
            headerData.put("APP_LINE_SEQ", "1");
            sxp = new SepoaXmlParser(this.getClass(), "setApprovalLineStatus");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            ssm.doUpdate(headerData);

            // 다음 결재라인 진행중 상태로 변경
            if(sf != null && sf.getRowCount()>0){
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
                headerData.put("APP_STAGE", "2");
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
            
        } catch (Exception e) {
            throw new SepoaApprovalProgressException(e);
        }
    }

}
