package sepoa.fw.approval.exception;

public class SepoaApprovalWithdrawException extends SepoaApprovalException {


    /**
     * 
     */
    private static final long serialVersionUID = 190053307760030084L;

    public SepoaApprovalWithdrawException() {
        super();
    }

    public SepoaApprovalWithdrawException(String msg) {
        super(msg);
    }

    public SepoaApprovalWithdrawException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalWithdrawException(String msg, Throwable t) {
        super(msg, t);
    }
}
