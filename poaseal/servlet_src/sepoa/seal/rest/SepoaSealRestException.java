package sepoa.seal.rest;

public class SepoaSealRestException extends Exception{
	private static final long serialVersionUID = 5272951657949683198L;

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