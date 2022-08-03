package mseqsynth.wrapper.smt;

import java.util.Map;

import mseqsynth.smtlib.Constant;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.SMTExpression;
import mseqsynth.smtlib.Variable;

public interface SMTSolver {
	
	public boolean checkSat(SMTExpression constraint, Map<Variable, Constant> model);
	
	public boolean checkSat$pAndNotq(SMTExpression p, ExistExpr q);
	
}
