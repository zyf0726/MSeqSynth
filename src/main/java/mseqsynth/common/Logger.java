package mseqsynth.common;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class Logger {
	
	private enum Level {
		DEBUG, INFO, WARN;
	}
	
	private static Level level = Level.DEBUG;

	private Logger() { }
	
	public static void setLevelDebug() {
		level = Level.DEBUG;
	}
	
	public static void setLevelInfo() {
		level = Level.INFO;
	}
	
	public static void setLevelWarn() {
		level = Level.WARN;
	}
	
	public static void debug(final String message) {
		print(Level.DEBUG, message);
	}

	public static void info(final String message) {
		print(Level.INFO, message);
	}

	public static void warn(final String message) {
		print(Level.WARN, message);
	}

	private static void print(final Level level, final String message) {
		print(level, message, null);
	}

	private static void print(final Level level, final String message, final Throwable t) {
		if (level.ordinal() >= Logger.level.ordinal()) {
			try {
				final PrintStream stream = new PrintStream(System.out, true, "UTF-8");
				stream.format("%s: %s", level.name(), message);
				stream.println();
				if (t != null) {
					t.printStackTrace(stream);
				}
				stream.flush();
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
