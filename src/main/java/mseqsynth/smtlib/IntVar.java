package mseqsynth.smtlib;

public class IntVar extends Variable {
	
	private static final long serialVersionUID = -3977981872367981816L;
	

	static final String VARNAME_PREFIX = "I";
	
	private static int countVars = 0;
	
	public static void resetCounter(int start) {
		IntVar.countVars = start;
	}
	
	public static int getCounter() {
		return IntVar.countVars;
	}
	
	IntVar(String varName) {
		super(varName);
		IntVar.countVars += 1;
	}
	
	public IntVar() {
		this(VARNAME_PREFIX + IntVar.countVars);
	}
	
	@Override
	public SMTSort getSMTSort() {
		return SMTSort.INT;
	}
	
}
