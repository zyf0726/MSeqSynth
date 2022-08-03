package mseqsynth.smtlib;

import java.util.List;

/**
 * SMT-LIB built-in operators
 */

public enum SMTOperator implements SMTFunction {
	// multivariable
	ADD("+"),
	SUB("-"),
	MUL("*"),
	DISTINCT("distinct"),
	AND("and"),
	OR("or"),
	
	// unary
	UN_NOT("not"),
	UN_MINUS("-"),
	
	// binary
	BIN_EQ("="),
	BIN_NE("distinct"),
	BIN_IMPLY("=>"),
	
	BIN_LT("<"),
	BIN_LE("<="),
	BIN_GT(">"),
	BIN_GE(">=")
	
	
	;
	
	private String repr;
	
	private SMTOperator(String repr) {
		this.repr = repr;
	}

	@Override
	public String getName() {
		return this.repr;
	}

	@Override
	public List<Variable> getArgs() {
		return null;
	}

	@Override
	public SMTSort getRange() {
		return null;
	}

	@Override
	public SMTExpression getBody() {
		return null;
	}

}
