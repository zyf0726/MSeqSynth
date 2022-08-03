package mseqsynth.common.exceptions;

public class LoadSettingsException extends RuntimeException {
	
	private static final long serialVersionUID = 2043681616862394783L;

	public LoadSettingsException() {
		super();
	}

	public LoadSettingsException(String message) {
		super(message);
	}

	public LoadSettingsException(Throwable cause) {
		super(cause);
	}

	public LoadSettingsException(String message, Throwable cause) {
		super(message, cause);
	}

}
