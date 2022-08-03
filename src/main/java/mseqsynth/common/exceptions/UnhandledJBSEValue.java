package mseqsynth.common.exceptions;

public class UnhandledJBSEValue extends RuntimeException {

	private static final long serialVersionUID = -5232921841442105886L;

	public UnhandledJBSEValue() {
		super();
	}

	public UnhandledJBSEValue(String message) {
		super(message);
	}

}
