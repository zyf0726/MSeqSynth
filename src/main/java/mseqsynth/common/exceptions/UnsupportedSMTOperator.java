package mseqsynth.common.exceptions;

public class UnsupportedSMTOperator extends RuntimeException{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2182479637090915251L;

	public UnsupportedSMTOperator() {
		super();
	}

	public UnsupportedSMTOperator(String message) {
		super(message);
	}

}
