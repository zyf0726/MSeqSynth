package mseqsynth.common.settings;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map.Entry;

import jbse.apps.run.RunParameters;
import jbse.apps.run.RunParameters.DecisionProcedureType;
import jbse.apps.run.RunParameters.StateFormatMode;
import jbse.apps.run.RunParameters.StepShowMode;
import jbse.apps.settings.ParseException;
import jbse.apps.settings.SettingsReader;
import mseqsynth.common.exceptions.LoadSettingsException;

public class JBSEParameters {
	
	private static final String JRE_SOURCEPATH = System.getProperty("java.home", "") + "src.zip";
	
	// JBSE class path
	private Path jbseClassPath = Options.I().getHomeDirectory().resolve("libs/jbse-0.10.0-SNAPSHOT.jar");
	
	public String getJBSEClassPath() {
		return this.jbseClassPath.toAbsolutePath().toString();
	}
	
	// target class path
	private Path targetClassPath = null;
	
	public String getTargetClassPath() {
		if (this.targetClassPath == null) {
			this.targetClassPath = Options.I().getHomeDirectory().resolve("bin/test");
		}
		return this.targetClassPath.toAbsolutePath().toString();
	}
	
	public void setTargetClassPath(String tcp) {
		this.targetClassPath = Paths.get(tcp);
	}
	
	// target source path
	private Path targetSrcPath = null;
	
	public String getTargetSourcePath() {
		if (this.targetSrcPath == null) {
			this.targetSrcPath = Options.I().getHomeDirectory().resolve("src/test/java");
		}
		return this.targetSrcPath.toAbsolutePath().toString();
	}
	
	public void setTargetSourcePath(String tsp) {
		this.targetSrcPath = Paths.get(tsp);
	}
	
	// target method
	private Method targetMethod;
	
	public void setTargetMethod(Method targetMethod) {
		this.targetMethod = targetMethod;
	}
	
	
	private boolean doSignAnalysis			=	false;
	private boolean doEqualityAnalysis		=	false;
	private StateFormatMode stateFormatMode	=	StateFormatMode.PATH;
	private StepShowMode stepShowMode		=	StepShowMode.LEAVES;
	private boolean showOnConsole	=	false;
	private String outFilePath		=	null;
	private String settingsPath		=	null;
	
	public void setFormatMode(StateFormatMode stateFormatMode) {
		this.stateFormatMode = stateFormatMode;
	}
	
	public void setShowMode(StepShowMode stepShowMode) {
		this.stepShowMode = stepShowMode;
	}
	
	public void setShowOnConsole(boolean onConsole) {
		this.showOnConsole = onConsole;
	}
	
	public void setOutFilePath(String outFilePath) {
		this.outFilePath = outFilePath;
	}
	
	public void setSettingsPath(String settingsPath) {
		this.settingsPath = settingsPath;
	}
	
	
	private HashMap<String, Integer> heapScope = new HashMap<>();
	private int depthScope = 0; // 0 means unlimited
	private int countScope = 0; // 0 means unlimited
	
	public void setHeapScope(String className, int heapScope) {
		this.heapScope.put(className, heapScope);
	}
	
	public void setHeapScope(Class<?> javaClass, int heapScope) {
		this.heapScope.put(javaClass.getName().replace('.', '/'), heapScope);
	}
	
	public void setDepthScope(int depthScope) {
		this.depthScope = depthScope;
	}
	
	public void setCountScope(int countScope) {
		this.countScope = countScope;
	}
	
	public void resetAllScope() {
		this.heapScope.clear();
		this.depthScope = 0;
		this.countScope = 0;
	}
	
	
	private static JBSEParameters INSTANCE = null;
	
	private JBSEParameters() { }
	
	public static JBSEParameters I() {
		if (INSTANCE == null) {
			INSTANCE = new JBSEParameters();
		}
		return INSTANCE;
	}
	
	public RunParameters getRunParameters() {
		RunParameters rp = new RunParameters();
		rp.setJBSELibPath(this.getJBSEClassPath());
		rp.addUserClasspath(this.getTargetClassPath());
		rp.addSourcePath(this.getTargetSourcePath(), JRE_SOURCEPATH);
		rp.setMethodSignature(
				this.targetMethod.getDeclaringClass().getName().replace('.', '/'),
				getMethodSignature(this.targetMethod).replace('.', '/'),
				this.targetMethod.getName()
		);
		rp.setDecisionProcedureType(DecisionProcedureType.Z3);
		rp.setExternalDecisionProcedurePath(Options.I().getSolverExecPath());
		rp.setDoSignAnalysis(this.doSignAnalysis);
		rp.setDoEqualityAnalysis(this.doEqualityAnalysis);
		rp.setStateFormatMode(this.stateFormatMode);
		rp.setStepShowMode(this.stepShowMode);
		rp.setShowOnConsole(this.showOnConsole);
		rp.setDepthScope(this.depthScope);
		rp.setCountScope(this.countScope);
		if (this.outFilePath != null) {
			rp.setOutputFilePath(this.outFilePath);
		} else {
			rp.setOutputFileNone();
		}
		if (this.settingsPath != null) {
			try {
				SettingsReader sr = new SettingsReader(this.settingsPath);
				sr.fillRunParameters(rp);
			} catch (NoSuchFileException e) {
				throw new LoadSettingsException("settings file not found");
			} catch (ParseException e) {
				throw new LoadSettingsException("settings file syntactically ill-formed");
			} catch (IOException e) {
				throw new LoadSettingsException("error while closing settings file");
			}
		}
		for (Entry<String, Integer> entry : this.heapScope.entrySet()) {
			rp.setHeapScope(entry.getKey(), entry.getValue());
		}
		return rp;
	}
	
	// https://stackoverflow.com/questions/45072268/how-can-i-get-the-signature-field-of-java-reflection-method-object
	private static String getMethodSignature(Method m) {
		StringBuilder sb = new StringBuilder("(");
		for (Class<?> c : m.getParameterTypes()) {
			String sig = Array.newInstance(c, 0).toString();
			sb.append(sig.substring(1, sig.indexOf('@')));
		}
		sb.append(")");
		if (m.getReturnType() == void.class) {
			sb.append("V");
		} else {
			String sig = Array.newInstance(m.getReturnType(), 0).toString();
			sb.append(sig.substring(1, sig.indexOf('@')));
		}
		return sb.toString();
	}
	
}
