package sepoa.fw.approval.exception;

public class SepoaApprovalConfirmException extends SepoaApprovalException {


    /**
     * 
     */
    private static final long serialVersionUID = 7389244076947607064L;

    public SepoaApprovalConfirmException() {
        super();
    }

    public SepoaApprovalConfirmException(String msg) {
        super(msg);
    }

    public SepoaApprovalConfirmException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalConfirmException(String msg, Throwable t) {
        super(msg, t);
    }
}
