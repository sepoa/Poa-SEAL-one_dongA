package sepoa.fw.approval.exception;

public class SepoaApprovalException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -7754929535080811480L;

    public SepoaApprovalException() {
        super();
    }

    public SepoaApprovalException(String msg) {
        super(msg);
    }

    public SepoaApprovalException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalException(String msg, Throwable t) {
        super(msg, t);
    }
}
