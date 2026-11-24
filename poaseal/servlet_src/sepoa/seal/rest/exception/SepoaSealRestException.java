package sepoa.seal.rest.exception;

public class SepoaSealRestException extends Exception{
	private static final long serialVersionUID = -8738038865005641358L;

	public SepoaSealRestException() {
		super("SepoaSealRestException");
	}
	
	public SepoaSealRestException(String msg) {
		super(msg);
	}
	
	public SepoaSealRestException(String msg, Throwable e) {
		super(msg, e);
	}
}