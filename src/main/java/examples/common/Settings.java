package examples.common;

import mseqsynth.common.settings.Options;

public class Settings {
	
	public static final String TARGET_CLASS_PATH =
			Options.I().getHomeDirectory().resolve("bin/main").toAbsolutePath().toString();
	public static final String TARGET_SOURCE_PATH = 
			Options.I().getHomeDirectory().resolve("src/main/java").toAbsolutePath().toString();
	
}
