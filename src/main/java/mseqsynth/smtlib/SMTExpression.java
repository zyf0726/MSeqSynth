package mseqsynth.smtlib;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * SMT-LIB quantifier-free expression
 */

public interface SMTExpression extends Serializable {

	public SMTSort getSMTSort();
	public String toSMTString();
	
	public Set<Variable> getFreeVariables();
	public Set<UserFunc> getUserFunctions();
	public SMTExpression getSubstitution(Map<Variable, ? extends SMTExpression> vMap);
	
}
