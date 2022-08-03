package mseqsynth.smtlib;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Constant implements SMTExpression {
	
	private static final long serialVersionUID = 6479308170255516677L;
	

	@Override
	public Set<Variable> getFreeVariables() {
		return new HashSet<>();
	}
	
	@Override
	public Set<UserFunc> getUserFunctions() {
		return new HashSet<>();
	}

	@Override
	public SMTExpression getSubstitution(Map<Variable, ? extends SMTExpression> vMap) {
		return this;
	}
	
}
