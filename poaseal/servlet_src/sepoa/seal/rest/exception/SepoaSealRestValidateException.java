package sepoa.seal.rest.exception;

public class SepoaSealRestValidateException extends Exception{
	private static final long serialVersionUID = 7872744484405507488L;

	public SepoaSealRestValidateException() {
		super("SepoaSealRestValidateException");
	}
	
	public SepoaSealRestValidateException(String msg) {
		super(msg);
	}
	
	public SepoaSealRestValidateException(String msg, Throwable e) {
		super(msg, e);
	}
}