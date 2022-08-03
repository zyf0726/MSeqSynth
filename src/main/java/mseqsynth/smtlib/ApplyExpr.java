package mseqsynth.smtlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class ApplyExpr implements SMTExpression {
	
	private static final long serialVersionUID = 8850037903095904635L;
	
	
	private SMTFunction operator;
	private ImmutableList<SMTExpression> operands;
	
	public ApplyExpr(SMTFunction operator, List<? extends SMTExpression> operands) {
		Preconditions.checkNotNull(operator, "a non-null operator expected");
		Preconditions.checkNotNull(operands, "a non-null operand list expected");
		Preconditions.checkArgument(!operands.isEmpty(), "a non-empty operand list expected");
		
		this.operator = operator;
		this.operands = ImmutableList.copyOf(operands);  // the operand must be a non-null SMTExpression
	}
	
	public ApplyExpr(SMTFunction operator, SMTExpression... operands) {
		this(operator, Arrays.asList(operands));
	}

	@Override
	public SMTSort getSMTSort() {
		return null;
	}

	@Override
	public String toSMTString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(" + this.operator.getName());
		for (SMTExpression operand : this.operands) {
			sb.append(" " + operand.toSMTString());
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public Set<Variable> getFreeVariables() {
		Set<Variable> fvSet = new HashSet<>();
		for (SMTExpression operand : this.operands) {
			fvSet.addAll(operand.getFreeVariables());
		}
		return fvSet;
	}
	
	@Override
	public Set<UserFunc> getUserFunctions() {
		Set<UserFunc> ufSet = new HashSet<>();
		if (this.operator instanceof UserFunc) {
			ufSet.add((UserFunc) this.operator);
		}
		for (SMTExpression operand : this.operands) {
			ufSet.addAll(operand.getUserFunctions());
		}
		return ufSet;
	}

	@Override
	public SMTExpression getSubstitution(Map<Variable, ? extends SMTExpression> vMap) {
		List<SMTExpression> subOpds = new ArrayList<>();
		for (SMTExpression operand : this.operands) {
			subOpds.add(operand.getSubstitution(vMap));
		}
		return new ApplyExpr(this.operator, subOpds);
	}

}
