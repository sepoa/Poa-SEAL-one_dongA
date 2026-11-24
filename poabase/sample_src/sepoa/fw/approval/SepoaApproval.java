package sepoa.fw.approval;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.cache.Cache;
import sepoa.fw.cfg.Configuration;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.jtx.SepoaTransactionalResource;
import sepoa.fw.msg.Message;
import sepoa.fw.msg.MessageUtil;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.srv.SepoaOut;
import sepoa.fw.srv.ServiceConnector;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public abstract class SepoaApproval implements SepoaApprovalInterface {

    /**
     * APP_TYPE 현업
     */
    public static final String APP_TYPE_OPERATION = "01";

    /**
     * APP_TYPE 관리
     */
    public static final String APP_TYPE_MANAGEMENT = "02";

    /**
     * APP_STATUS 임시저장
     */
    public static final String APP_STATUS_TEMP = "T";

    /**
     * APP_STATUS 결재진행
     */
    public static final String APP_STATUS_PROGRESS = "I";

    /**
     * APP_STATUS 결재완료
     */
    public static final String APP_STATUS_FINAL = "F";

    /**
     * APP_STATUS 결재반려
     */
    public static final String APP_STATUS_REJECT = "R";

    /**
     * APP_METHOD 상신
     */
    public static final String APP_METHOD_REPORT = "01";

    /**
     * APP_METHOD 합의
     */
    public static final String APP_METHOD_AGREEMENT = "02";

    /**
     * APP_METHOD 참조
     */
    public static final String APP_METHOD_REFERENCE = "03";

    /**
     * APP_METHOD 결재
     */
    public static final String APP_METHOD_APPROVAL = "04";

    /**
     * APP_LINE_STATUS 상신
     */
    public static final String APP_LINE_STATUS_REPORT = "L01";

    /**
     * APP_LINE_STATUS 승인
     */
    public static final String APP_LINE_STATUS_CONFIRM = "L02";

    /**
     * APP_LINE_STATUS 반려
     */
    public static final String APP_LINE_STATUS_REJECT = "L03";

    /**
     * APP_LINE_STATUS 진행
     */
    public static final String APP_LINE_STATUS_PROGRESS = "L04";

    /**
     * APP_LINE_STATUS 완료
     */
    public static final String APP_LINE_STATUS_COMPLETED = "L05";
    
    /**
     * 관리 결재선 LINE_TYPE 기본값
     */
    public static final String LINE_TYPE_DEFAULT = "01";
    
    /**
     * 관리 결재선 LINE_TYPE 모듈별 부서 
     */
    public static final String LINE_TYPE_MODULE_DEPT = "02";
    
    /**
     * 관리 결재선 LINE_TYPE 계정별
     */
    public static final String LINE_TYPE_ACCOUNT = "03";
    
    /**
     * 관리 결재선 계정코드 넘겨줘야 하는 컬럼명.
     */
    public static final String LINE_TYPE_ACCOUNT_CODE = "ACCOUNT_CODE";
    
    /**
     * 결재 BackProcess Type 결재진행
     */
    public static final String APP_BACKPROCESS_TYPE_PROGRESS = "PROGRESS";
    
    /**
     * 결재 BackProcess Type 승인
     */
    public static final String APP_BACKPROCESS_TYPE_CONFIRM = "CONFIRM";
    
    /**
     * 결재 BackProcess Type 반려
     */
    public static final String APP_BACKPROCESS_TYPE_REJECT = "REJECT";

    /**
     * 결재취소 BackProcess Type 반려
     */
    public static final String APP_BACKPROCESS_TYPE_WITHDRAW = "WITHDRAW";

    /**
     * 결재재사용 BackProcess Type 반려
     */
    public static final String APP_BACKPROCESS_TYPE_REUSE = "REUSE";

    /**
     * 결재삭제 BackProcess Type 반려
     */
    public static final String APP_BACKPROCESS_TYPE_DELETE = "DELETE";
    /**
     * COMMON OBJECT
     */
    public static Object SEPOA_APPROVAL_COMMON = null;
    
    /**
     * 전결 타입 키
     */
    public static String ARBITRARY_DECISION_TYPE_KEY = "ARBITRARY_DECISION_TYPE_KEY";

    /**
     * 전결 타입 전체 전결
     */
    public static String ARBITRARY_DECISION_TYPE_ALL = "ALL";

    /**
     * 결재가 동시다발적으로 처리되면 문제가 발생할 수 있으므로, 차례차례 처리되도록 한다.
     */
    private static final Object mutex = new Object();
    
    private Configuration conf = null;
    private Map<String,ConnectionContext> ctxMap = new HashMap<String, ConnectionContext>();
    /**
     * Connection이 현재 Class 에서 생성되었는지 여부.
     * 내부에서 생성된건이라면 내부에서 Commit 및 Rollback 및 Release가 되어야한다.
     */
    private Map<String,Boolean> connectionFlagMap = new HashMap<String,Boolean>();

    /**
     * infoMap
     */
    private Map<String, SepoaInfo> sepoainfoMap = new HashMap<String,SepoaInfo>();
    

    protected SepoaXmlParser sxp = null;
    protected SepoaSQLManager ssm = null;
    protected SepoaFormater sf = null;
    
    private Message msg = null;

    /**
     * 컨넥션을 안넘겨줄 경우 Connection은 자체적으로 생성하기 때문에
     * 프레임워크 단에서 Transaction관리를 한다.
     * @param info
     * @throws SepoaApprovalException 
     */
    protected SepoaApproval(SepoaInfo info) throws SepoaApprovalException {
        this.sepoainfoMap.put(info.getSession("ID"), info);
        this.connectionFlagMap.put(info.getSession("ID"),false);
        try {
            msg = new Message(info, "SEPOA_APPROVAL");
            this.conf = new Configuration();
        } catch (Exception e) {
            throw new SepoaApprovalException(e);
        }
    }

    /**
     * 컨넥션을 넘겨주면 Transaction관리는 업무단에서 처리해야한다.
     * @param ctx
     * @param info
     * @throws SepoaApprovalException 
     */
    protected SepoaApproval(ConnectionContext ctx, SepoaInfo info) throws SepoaApprovalException {
        this.sepoainfoMap.put(info.getSession("ID"), info);
        this.ctxMap.put(info.getSession("ID"), ctx);
        this.connectionFlagMap.put(info.getSession("ID"),false);
        try {
            msg = new Message(info, "SEPOA_APPROVAL");
            this.conf = new Configuration();
        } catch (Exception e) {
            throw new SepoaApprovalException(e);
        }
    }

    protected ConnectionContext getConnectionContext(Map<String, String> headerData) throws SepoaApprovalException {
        try {
            if (this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)) == null) {
                this.ctxMap.put(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID), new SepoaTransactionalResource());
                this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)).getConnection().setAutoCommit(false);
                this.connectionFlagMap.put(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID),true);
            }else {
                this.connectionFlagMap.put(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID),false);
            }
        } catch (Exception e) {
            throw new SepoaApprovalException(e);
        }
        return this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID));
    }

    protected SepoaInfo getInfo(String id) {
        return this.sepoainfoMap.get(id);
    }

    protected String getConfig(String name) {
        if (this.conf != null) {
            return this.conf.getString(name);
        } else {
            return "";
        }
    }

    protected void Release(Map<String, String> headerData) throws SepoaApprovalException {
        if (this.connectionFlagMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID))) {
            if (this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)) == null) {
                try {
                    if (!this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)).getConnection().isClosed()) {
                        this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)).getConnection().close();
                    }
                } catch (SQLException e) {
                    throw new SepoaApprovalException(e);
                }
            }
        }
    }

    protected void Rollback(Map<String, String> headerData) throws SepoaApprovalException {
        if (this.connectionFlagMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID))) {
            if (this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)) != null) {
                try {
                    this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)).getConnection().rollback();
                } catch (SQLException e) {
                    throw new SepoaApprovalException(e);
                }
            }
        }
    }

    protected void Commit(Map<String, String> headerData) throws SepoaApprovalException {
        if (this.connectionFlagMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID))) {
            if (this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)) != null) {
                try {
                    this.ctxMap.get(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID)).getConnection().commit();
                } catch (SQLException e) {
                    throw new SepoaApprovalException(e);
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    protected SepoaOut doBusinessLogicCall(ConnectionContext ctx, Map<String, Object> allData, String Type) throws SepoaApprovalException {
        SepoaOut businessOut = null;
        Object[] businessObj = { ctx, allData };
        SepoaInfo info = null;
        try {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            info = this.getInfo(MapUtils.getString(headerData, SepoaDataMapper.KEY_SESSION_USER_ID, ""));
            
            sxp = new SepoaXmlParser(SepoaApproval.class, "getAppType");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            
            sxp = new SepoaXmlParser(SepoaApproval.class, "doBusinessLogic");
            ssm = new SepoaSQLManager(info.getSession("ID"), this, ctx, sxp);
            sf = new SepoaFormater(ssm.doSelect(headerData));
            if (sf != null && sf.getRowCount() > 0) {
                String BusinessService = sf.getValue("BUSINESS_SERVICE", 0);
                if(Type.equals(SepoaApproval.APP_BACKPROCESS_TYPE_PROGRESS)){
                    String ProgressMethod  = sf.getValue("PROGRESS_METHOD" , 0);
                    businessOut = ServiceConnector.doService(info, BusinessService , "NONDBJOB", ProgressMethod, businessObj);
                    
                }else if(Type.equals(SepoaApproval.APP_BACKPROCESS_TYPE_CONFIRM)){
                    String ConfirmMethod  = sf.getValue("CONFIRM_METHOD" , 0);
                    businessOut = ServiceConnector.doService(info, BusinessService , "NONDBJOB", ConfirmMethod, businessObj);
                    
                }else if(Type.equals(SepoaApproval.APP_BACKPROCESS_TYPE_REJECT)){
                    String RejectMethod  = sf.getValue("REJECT_METHOD" , 0);
                    businessOut = ServiceConnector.doService(info, BusinessService , "NONDBJOB", RejectMethod, businessObj);
                    
                }else if(Type.equals(SepoaApproval.APP_BACKPROCESS_TYPE_WITHDRAW)){
                    String RejectMethod  = sf.getValue("WITHDRAW_METHOD" , 0);
                    businessOut = ServiceConnector.doService(info, BusinessService , "NONDBJOB", RejectMethod, businessObj);
                    
                }else if(Type.equals(SepoaApproval.APP_BACKPROCESS_TYPE_REUSE)){
                    String RejectMethod  = sf.getValue("REUSE_METHOD" , 0);
                    businessOut = ServiceConnector.doService(info, BusinessService , "NONDBJOB", RejectMethod, businessObj);
                    
                }else if(Type.equals(SepoaApproval.APP_BACKPROCESS_TYPE_DELETE)){
                    String RejectMethod  = sf.getValue("DELETE_METHOD" , 0);
                    businessOut = ServiceConnector.doService(info, BusinessService , "NONDBJOB", RejectMethod, businessObj);
                    
                }
            }
        } catch (Exception e) {
            throw new SepoaApprovalException(e);
        }

        return businessOut;
    }

    /**
     * 
     * 결재라인을 셋팅을 하자. 
     * 솔루션 내부 결재 , 그룹웨어결재 , 결재미사용 구분을 지어야한다.
     * 
     * @param ctx
     * @param allData
     * @return
     * @throws SepoaApprovalException 
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> doApprovalProcess(Map<String, Object> allData) throws SepoaApprovalException {
        synchronized (mutex) {
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            List<Map<String, String>> appLine = (List<Map<String, String>>) MapUtils.getObject(allData, "APP_LINE");
            SepoaInfo info = null;
            try {
                info = this.getInfo(MapUtils.getString(headerData, SepoaDataMapper.KEY_SESSION_USER_ID, ""));
            	if(!"".equals(MapUtils.getString(headerData, "COMPANY_CODE", ""))) {
            		headerData.put("SEPOA_APPROVAL_S_COMPANY_CODE", MapUtils.getString(headerData, "COMPANY_CODE", ""));
            	}else {
            		headerData.put("SEPOA_APPROVAL_S_COMPANY_CODE", info.getSession("COMPANY_CODE"));
            	}
            	
            	if(appLine != null) {
                	for (Map<String, String> map : appLine) {
                    	if(!"".equals(MapUtils.getString(headerData, "COMPANY_CODE", ""))) {
                    		map.put("SEPOA_APPROVAL_S_COMPANY_CODE", MapUtils.getString(headerData, "COMPANY_CODE", ""));
                    	}else {
                    		map.put("SEPOA_APPROVAL_S_COMPANY_CODE", info.getSession("COMPANY_CODE"));
                    	}
    				}	
            	}
            	
                if (!"".equals(MapUtils.getString(headerData, "APP_TYPE", ""))) {
                    
                    ConnectionContext ctx = getConnectionContext(headerData);
                    String approvalType = "";
                    // 프로퍼티에 지정된 모듈별 결재방식을 가져오자.
                    approvalType = getConfig("sepoa.approval.type." + MapUtils.getString(headerData, "APP_TYPE", ""));
    
                    //  S-세포아결재, G-그룹웨어결재, N-결재미사용, A-승인요청
                    if ("S".equals(approvalType)) {
                        doSolution(info, ctx, allData);
                    } else if ("G".equals(approvalType)) {
                        doGroupware(info, ctx, allData);
                    } else if ("N".equals(approvalType)) {
                        doNotApproval(info, ctx, allData);
                    } else if ("A".equals(approvalType)) {
                        doDirectApproval(info, ctx, allData);
                    }
                }
                Commit(headerData);
            } catch (Exception e) {
                Rollback(headerData);
                e.printStackTrace();
                String message = Cache.text.get("SEPOA_APPROVAL." + e.getMessage());
                if(message.indexOf("Has not found text") != -1) {
                	message = e.getMessage();
                }
                throw new SepoaApprovalException(message);
            } finally {
                Release(headerData);
                this.ctxMap.remove(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID));
                this.connectionFlagMap.remove(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID));
                this.sepoainfoMap.remove(headerData.get(SepoaDataMapper.KEY_SESSION_USER_ID));
            }
            return allData;
        }
    }
}
