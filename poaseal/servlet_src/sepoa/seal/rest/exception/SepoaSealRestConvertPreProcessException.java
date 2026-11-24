package sepoa.seal.rest.exception;

public class SepoaSealRestConvertPreProcessException extends Exception{
	private static final long serialVersionUID = -8738038865005641358L;

	public SepoaSealRestConvertPreProcessException() {
		super("SepoaSealRestConvertPreProcessException");
	}
	
	public SepoaSealRestConvertPreProcessException(String msg) {
		super(msg);
	}
	
	public SepoaSealRestConvertPreProcessException(String msg, Throwable e) {
		super(msg, e);
	}
}