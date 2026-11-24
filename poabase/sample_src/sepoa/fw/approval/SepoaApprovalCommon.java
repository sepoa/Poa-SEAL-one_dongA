package sepoa.fw.approval;

import java.util.Map;

import org.apache.commons.collections.MapUtils;

import sepoa.fw.approval.exception.SepoaApprovalCommonException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.db.SepoaSQLManager;
import sepoa.fw.db.SepoaXmlParser;
import sepoa.fw.ses.SepoaInfo;
import sepoa.fw.util.SepoaDataMapper;
import sepoa.fw.util.SepoaFormater;

public class SepoaApprovalCommon {

    /**
     * 업무Key로 결재Key가져오는 메소드
     * 넘겨준 AllData에 바로 결재번호를 셋팅해주므로, 별도로 저장해주는 로직은 필요 없다.
     * @param allData
     * @throws SepoaApprovalCommonException
     */
    @SuppressWarnings("unchecked")
    public static void doBusinessNumberToApprovalNumber(ConnectionContext ctx, SepoaInfo info, Map<String, Object> allData)
            throws SepoaApprovalCommonException {
        SepoaXmlParser sxp = null;
        SepoaSQLManager ssm = null;
        try {
            
            SepoaApprovalCommon.doCommonObjectCheck();
            
            Map<String, String> headerData = (Map<String, String>) allData.get(SepoaDataMapper.KEY_HEADER_DATA);
            
            if(!"".equals(MapUtils.getString(headerData, "COMPANY_CODE", ""))) {
        		headerData.put("SEPOA_APPROVAL_S_COMPANY_CODE", MapUtils.getString(headerData, "COMPANY_CODE", ""));
        	}else {
        		headerData.put("SEPOA_APPROVAL_S_COMPANY_CODE", info.getSession("COMPANY_CODE"));
        	}
            
            if ("".equals(MapUtils.getString(headerData, "APP_NO", ""))) {
                if ("".equals(MapUtils.getString(headerData, "SUX_WKEY1", ""))) {
                    headerData.put("SUX_WKEY1", "NO_DATA");
                }
                sxp = new SepoaXmlParser(SepoaApprovalCommon.class, "doGetAppNo");
                ssm = new SepoaSQLManager(info.getSession("ID"), SepoaApproval.SEPOA_APPROVAL_COMMON, ctx, sxp);
                SepoaFormater sf = new SepoaFormater(ssm.doSelect(headerData));
                if (sf != null && sf.getRowCount() > 0) {
                    headerData.put("APP_NO", sf.getValue("APP_NO", 0));
                    headerData.put("APP_NO_SEQ", sf.getValue("APP_NO_SEQ", 0));
                } else {
                    headerData.put("APP_NO", "NO-DATA");
                    headerData.put("APP_NO_SEQ", "NO-DATA");
                }
            }
        } catch (Exception e) {
            throw new SepoaApprovalCommonException(e);
        }
    }
    /**
     * 쿼리호출할려면 객체가 있어야 해서 임의로 생성해서 하나 가지고 있는다.
     * @throws SepoaApprovalCommonException
     */
    public static void doCommonObjectCheck() throws SepoaApprovalCommonException{
        if(SepoaApproval.SEPOA_APPROVAL_COMMON == null){
            try {
                SepoaApproval.SEPOA_APPROVAL_COMMON = SepoaApprovalCommon.class.newInstance();
            } catch (Exception e) {
                throw new SepoaApprovalCommonException(e);
            } 
        }
    }
}
