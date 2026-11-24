package sepoa.fw.approval.exception;

public class SepoaApprovalRejectException extends SepoaApprovalException {

    /**
     * 
     */
    private static final long serialVersionUID = -6702525936137766027L;

    public SepoaApprovalRejectException() {
        super();
    }

    public SepoaApprovalRejectException(String msg) {
        super(msg);
    }

    public SepoaApprovalRejectException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalRejectException(String msg, Throwable t) {
        super(msg, t);
    }
}
