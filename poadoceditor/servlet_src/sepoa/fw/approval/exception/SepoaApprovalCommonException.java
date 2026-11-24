package sepoa.fw.approval.exception;

public class SepoaApprovalCommonException extends SepoaApprovalException {

    /**
     * 
     */
    private static final long serialVersionUID = 626722042173504599L;

    public SepoaApprovalCommonException() {
        super();
    }

    public SepoaApprovalCommonException(String msg) {
        super(msg);
    }

    public SepoaApprovalCommonException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalCommonException(String msg, Throwable t) {
        super(msg, t);
    }
}
