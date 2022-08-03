package mseqsynth.smtlib;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class UserFunc implements SMTFunction, Comparable<UserFunc> {
	
	private static final long serialVersionUID = 1216842976409320454L;
	

	static final String FUNCNAME_PREFIX = "FUN";
	
	private static int countFuncs = 0;
	
	public static void resetCounter(int start) {
		UserFunc.countFuncs = start;
	}
	
	public static int getCounter() {
		return UserFunc.countFuncs;
	}
	
	private int timeStamp;
	private String funcName;
	private ImmutableList<Variable> funcArgs;
	private SMTSort funcRange;
	private SMTExpression funcBody;
	
	UserFunc(String name, List<? extends Variable> args, SMTSort range, SMTExpression body) {
		Preconditions.checkNotNull(name, "a non-null function name expected");
		Preconditions.checkNotNull(args, "a non-null argument list expected");
		Preconditions.checkArgument(!args.isEmpty(), "a non-empty argument list expected");
		Preconditions.checkNotNull(range, "a non-null function range expected");
		this.timeStamp = UserFunc.countFuncs;
		this.funcName = name;
		this.funcArgs = ImmutableList.copyOf(args);  // the argument must be a non-null variable
		this.funcRange = range;
		this.setBody(body);
		UserFunc.countFuncs += 1;
	}
	
	public UserFunc(List<? extends Variable> args, SMTSort Range, SMTExpression body) {
		this(FUNCNAME_PREFIX + UserFunc.countFuncs, args, Range, body);
	}
	
	void setBody(SMTExpression body) {
		Preconditions.checkNotNull(body, "a non-null function body expected");
		Set<Variable> fvSet = body.getFreeVariables();
		Set<Variable> argSet = ImmutableSet.copyOf(this.funcArgs);
		Preconditions.checkArgument(Sets.difference(fvSet, argSet).isEmpty(),
				"a non-argument free variable found in the body");
		this.funcBody = body;
	}
	
	@Override
	public int compareTo(UserFunc o) {
		return this.timeStamp - o.timeStamp;
	}

	@Override
	public String getName() {
		return this.funcName;
	}

	@Override
	public List<Variable> getArgs() {
		return this.funcArgs;
	}

	@Override
	public SMTSort getRange() {
		return this.funcRange;
	}

	@Override
	public SMTExpression getBody() {
		return this.funcBody;
	}
	
	public String getSMTDecl() {
		StringBuilder sb = new StringBuilder();
		sb.append("(declare-fun " + this.funcName + " (");
		for (Variable v : this.funcArgs) {
			sb.append(v.getSMTSort().toSMTString() + " ");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(") " + this.funcRange.toSMTString() + ")");
		return sb.toString();
		
	}
	
	public String getSMTAssert() {
		StringBuilder sb = new StringBuilder();
		sb.append("(assert (forall (");
		for (Variable v : this.funcArgs) {
			sb.append("(" + v.toSMTString() + " " + v.getSMTSort().toSMTString() + ") ");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(") (= (" + this.funcName);
		for (Variable v : this.funcArgs) {
			sb.append(" " + v.toSMTString());
		}
		sb.append(") " + this.funcBody.toSMTString() + ")))");
		return sb.toString();
	}
	
	public String getSMTDef() {
		StringBuilder sb = new StringBuilder();
		sb.append("(define-fun " + this.funcName + " (");
		for (Variable v : this.funcArgs) {
			sb.append("(" + v.toSMTString() + " " + v.getSMTSort().toSMTString() + ") ");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(") " + this.funcRange.toSMTString() + " ");
		sb.append(this.funcBody.toSMTString() + ")");
		return sb.toString();
	}

}
