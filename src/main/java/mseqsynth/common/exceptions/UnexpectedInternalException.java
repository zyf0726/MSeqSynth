package mseqsynth.common.exceptions;

public class UnexpectedInternalException extends RuntimeException {
	
	private static final long serialVersionUID = -4869941112308876859L;

	public UnexpectedInternalException() {
		super();
	}
	
    public UnexpectedInternalException(String message) {
        super(message);
    }

    public UnexpectedInternalException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedInternalException(Throwable cause) {
        super(cause);
    }

}
