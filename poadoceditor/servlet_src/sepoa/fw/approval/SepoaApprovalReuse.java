package sepoa.fw.approval;

import java.util.Map;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.approval.exception.SepoaApprovalReuseException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.util.CommonUtil;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public class SepoaApprovalReuse extends SepoaApproval {

    public SepoaApprovalReuse(SepoaInfo info) throws SepoaApprovalException {
        super(info);
    }

    public SepoaApprovalReuse(ConnectionContext ctx, SepoaInfo info) throws SepoaApprovalException {
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
            throws SepoaApprovalReuseException {
        try {
            // 그냥 결재올라간건을 그냥 삭제처리한다.
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            // 현재 결재라인의 KEY값을 가져온다.
            sxp = new SepoaXmlParser(this.getClass(), "getAppUserData");
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
            }
            
            if ("Y".equals(CommonUtil.getConfig("sepoa.approval.reject.delete"))) {
                sxp = new SepoaXmlParser(this.getClass(), "delApprovalHeader");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doDelete(headerData);

                sxp = new SepoaXmlParser(this.getClass(), "delApprovalLine");
                ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
                ssm.doDelete(headerData);
            }
            // 결재 진행 후 처리되는 업무 프로세스를 호출한다.
            SepoaOut so = doBusinessLogicCall(ctx, allData, SepoaApproval.APP_BACKPROCESS_TYPE_REUSE);
            if (!so.flag) {
                throw new SepoaApprovalReuseException(so.message);
            }
        } catch (Exception e) {
            throw new SepoaApprovalReuseException(e);
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
            throws SepoaApprovalReuseException {
        // 그룹웨어는.......
    }

    /**
     * 
     * 결재를 진행하지 않는 건에 대해서는 임의 1인 결재완료 상태로 만든다.
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalReuseException 
     */
    @Override
    public void doNotApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalReuseException {
        //결재를 진행하지 않는데 취소할리가 있나~

    }

    /**
     * 
     * 탈려나~
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalReuseException 
     */
    @Override
    public void doDirectApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalReuseException {
        //탈려나~

    }

}
