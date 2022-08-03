/**
 * 
 */
package mseqsynth.smtlib;

import java.io.Serializable;
import java.util.Set;

/**
 * SMT-LIB quantified expression
 */

public interface SMTQuantifiedExpr extends Serializable {

	public String toSMTString();
	public Set<Variable> getBoundVariables();
	public SMTExpression getBody();
	
}
