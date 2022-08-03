package mseqsynth.smtlib;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class ExistExpr implements SMTQuantifiedExpr {
	
	private static final long serialVersionUID = -4908507598779926176L;
	
	
	public static ExistExpr ALWAYS_TRUE = new ExistExpr(null, new BoolConst(true));
	public static ExistExpr ALWAYS_FALSE = new ExistExpr(null, new BoolConst(false));
	
	private ImmutableSet<Variable> boundVars;
	private SMTExpression body;
	
	public ExistExpr(Collection<Variable> boundVars, SMTExpression body) {
		Preconditions.checkNotNull(body, "the body must be a non-null (quantifier-free) SMTExpression");
		this.body = body;
		if (boundVars == null) {
			this.boundVars = ImmutableSet.of(); 
		} else {
			this.boundVars = ImmutableSet.copyOf(boundVars);  // bound variables must be non-null
		}
	}

	@Override
	public String toSMTString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(exists (");
		for (Variable v : this.boundVars) {
			sb.append("(" + v.toSMTString() + " " + v.getSMTSort().toSMTString() + ") ");
		}
		if (this.boundVars.isEmpty()) {
			sb.append("(DUMMY " + SMTSort.BOOL.toSMTString() + ") ");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(") " + this.body.toSMTString() + ")");
		return sb.toString();
	}

	@Override
	public Set<Variable> getBoundVariables() {
		return this.boundVars;
	}

	@Override
	public SMTExpression getBody() {
		return this.body;
	}
	
	public static ExistExpr makeOr(Collection<ExistExpr> es, Map<Variable, Variable> renameMap) {
		List<IntVar> ivCreated = new ArrayList<>();
		List<BoolVar> bvCreated = new ArrayList<>();
		if (renameMap == null) {
			renameMap = new HashMap<>();
		} else {
			renameMap.clear();
		}
		for (ExistExpr e : es) {
			ArrayDeque<IntVar> ivRemain = new ArrayDeque<>(ivCreated);
			ArrayDeque<BoolVar> bvRemain = new ArrayDeque<>(bvCreated);
			Set<Variable> vMapped = new HashSet<>();
			for (Variable v : e.getBoundVariables()) {
				if (renameMap.containsKey(v))
					vMapped.add(renameMap.get(v));
			}
			ivRemain.removeAll(vMapped);
			bvRemain.removeAll(vMapped);
			for (Variable v : e.getBoundVariables()) {
				if (renameMap.containsKey(v))
					continue;
				switch (v.getSMTSort()) {
				case BOOL:
					if (bvRemain.isEmpty()) {
						BoolVar bv = new BoolVar();
						bvCreated.add(bv);
						renameMap.put(v, bv);
					} else {
						renameMap.put(v, bvRemain.pop());
					}
					break;
				case INT:
					if (ivRemain.isEmpty()) {
						IntVar iv = new IntVar();
						ivCreated.add(iv);
						renameMap.put(v, iv);
					} else {
						renameMap.put(v, ivRemain.pop());
					}
					break;
				}
			}
		}
		List<Variable> boundVars = new ArrayList<>(ivCreated);
		boundVars.addAll(bvCreated);
		List<SMTExpression> renamedBodies = new ArrayList<>();
		for (ExistExpr e : es) {
			renamedBodies.add(e.getBody().getSubstitution(renameMap));
		}
		return new ExistExpr(boundVars, new ApplyExpr(SMTOperator.OR, renamedBodies));
	}

}
