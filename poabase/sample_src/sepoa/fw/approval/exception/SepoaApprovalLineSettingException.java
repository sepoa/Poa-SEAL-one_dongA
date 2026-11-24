package sepoa.fw.approval.exception;

public class SepoaApprovalLineSettingException extends SepoaApprovalException {

    /**
     * 
     */
    private static final long serialVersionUID = 8674274958139594761L;

    public SepoaApprovalLineSettingException() {
        super();
    }

    public SepoaApprovalLineSettingException(String msg) {
        super(msg);
    }

    public SepoaApprovalLineSettingException(Throwable t) {
        super(t.getMessage());
    }

    public SepoaApprovalLineSettingException(String msg, Throwable t) {
        super(msg, t);
    }
}
