package sepoa.fw.approval;

import java.util.Map;

import sepoa.fw.approval.exception.SepoaApprovalException;
import sepoa.fw.db.ConnectionContext;
import sepoa.fw.ses.SepoaInfo;

interface SepoaApprovalInterface {
    public void doSolution(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalException;

    public void doGroupware(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalException;

    public void doNotApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalException;

    public void doDirectApproval(SepoaInfo info, ConnectionContext ctx, Map<String, Object> allData)
            throws SepoaApprovalException;

}
