package sepoa.fw.approval.exception;

public class SepoaApprovalProgressException extends SepoaApprovalException {
    /**
     * 
     */
    private static final long serialVersionUID = -1963427751982646718L;

    public SepoaApprovalProgressException() {
        super();
    }

    public SepoaApprovalProgressException(String msg) {
        super(msg);
    }

    public SepoaApprovalProgressException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalProgressException(String msg, Throwable t) {
        super(msg, t);
    }
}
