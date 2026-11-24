package sepoa.seal.rest.exception;

public class SepoaSealRestPathException extends Exception{
	private static final long serialVersionUID = 4279764573192677032L;

	public SepoaSealRestPathException() {
		super("SepoaSealRestPathException");
	}
	
	public SepoaSealRestPathException(String msg) {
		super(msg);
	}
	
	public SepoaSealRestPathException(String msg, Throwable e) {
		super(msg, e);
	}
}
