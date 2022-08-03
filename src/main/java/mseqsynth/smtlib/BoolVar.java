package mseqsynth.smtlib;

public class BoolVar extends Variable {
	
	private static final long serialVersionUID = 1818819346656345230L;
	

	static final String VARNAME_PREFIX = "B";
	
	private static int countVars = 0;
	
	public static void resetCounter(int start) {
		BoolVar.countVars = start;
	}
	
	public static int getCounter() {
		return BoolVar.countVars;
	}
	
	BoolVar(String varName) {
		super(varName);
		BoolVar.countVars += 1;
	}
	
	public BoolVar() {
		this(VARNAME_PREFIX + BoolVar.countVars);
	}
	
	@Override
	public SMTSort getSMTSort() {
		return SMTSort.BOOL;
	}
}
