package mseqsynth.wrapper.smt;

import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.SMTExpression;

public interface IncrSMTSolver extends SMTSolver {
	
	public void initIncrSolver();
	
	public void pushAssert(SMTExpression p);
	
	public void pushAssertNot(ExistExpr p);
	
	public void endPushAssert();
	
	public boolean checkSatIncr(SMTExpression p);
	
	public void closeIncrSolver();
	
}
