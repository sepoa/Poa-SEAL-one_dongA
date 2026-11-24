package sepoa.fw.approval.exception;

public class SepoaApprovalReuseException extends SepoaApprovalException {

    /**
     * 
     */
    private static final long serialVersionUID = -4162543287221830466L;

    public SepoaApprovalReuseException() {
        super();
    }

    public SepoaApprovalReuseException(String msg) {
        super(msg);
    }

    public SepoaApprovalReuseException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalReuseException(String msg, Throwable t) {
        super(msg, t);
    }
}
