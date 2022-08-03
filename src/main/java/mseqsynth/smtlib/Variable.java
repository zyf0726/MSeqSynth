package mseqsynth.smtlib;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

public abstract class Variable implements SMTExpression, Comparable<Variable> {
	
	private static final long serialVersionUID = -1151227570581088219L;
	
	
	private String varName;
	
	Variable(String varName) {
		Preconditions.checkNotNull(varName, "a non-null variable name expected");
		this.varName = varName;
	}
	
	public static Variable create(SMTSort smtSort) {
		switch (smtSort) {
		case BOOL:
			return new BoolVar();
		case INT:
			return new IntVar();
		}
		return null;
	}
	
	public Variable cloneVariable() {
		return Variable.create(this.getSMTSort());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Variable))
			return false;
		return this.varName.equals(((Variable) obj).varName);
	}
	
	@Override
	public int compareTo(Variable o) {
		return this.varName.compareTo(o.varName);
	}
	
	@Override
	public int hashCode() {
		return this.varName.hashCode();
	}
	
	@Override
	public String toSMTString() {
		return this.varName;
	}
	
	@Override
	public Set<Variable> getFreeVariables() {
		return new HashSet<>(Collections.singleton(this));
	}
	
	@Override
	public Set<UserFunc> getUserFunctions() {
		return new HashSet<>();
	}
	
	@Override
	public SMTExpression getSubstitution(Map<Variable, ? extends SMTExpression> vMap) {
		if (vMap.containsKey(this)) {
			return vMap.get(this);
		} else {
			return this;
		}
	}
	
	public String getSMTDecl() {
		return "(declare-const " + this.varName + " " + this.getSMTSort().toSMTString() + ")";
	}

}
