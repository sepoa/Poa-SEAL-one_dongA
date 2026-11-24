package sepoa.seal.rest.exception;

public class SepoaSealRestPropsException extends Exception{
	private static final long serialVersionUID = 4279764573192677032L;

	public SepoaSealRestPropsException() {
		super("SepoaSealRestPropsException");
	}
	
	public SepoaSealRestPropsException(String msg) {
		super(msg);
	}
	
	public SepoaSealRestPropsException(String msg, Throwable e) {
		super(msg, e);
	}
}
