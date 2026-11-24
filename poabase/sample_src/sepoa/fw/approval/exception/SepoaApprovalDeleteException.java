package sepoa.fw.approval.exception;

public class SepoaApprovalDeleteException extends SepoaApprovalException {

    /**
     * 
     */
    private static final long serialVersionUID = -2451077113600339062L;

    public SepoaApprovalDeleteException() {
        super();
    }

    public SepoaApprovalDeleteException(String msg) {
        super(msg);
    }

    public SepoaApprovalDeleteException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalDeleteException(String msg, Throwable t) {
        super(msg, t);
    }
}
