package mseqsynth.common.exceptions;

public class UnhandledJBSEPrimitive extends RuntimeException{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8728277652808961054L;

	public UnhandledJBSEPrimitive() {
		super();
	}

	public UnhandledJBSEPrimitive(String message) {
		super(message);
	}

}
