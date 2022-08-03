package mseqsynth.wrapper.smt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;
import com.microsoft.z3.Symbol;

import mseqsynth.common.Logger;
import mseqsynth.smtlib.BoolConst;
import mseqsynth.smtlib.Constant;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntConst;
import mseqsynth.smtlib.SMTExpression;
import mseqsynth.smtlib.SMTSort;
import mseqsynth.smtlib.UserFunc;
import mseqsynth.smtlib.Variable;


public class Z3JavaAPI implements IncrSMTSolver {
	
	private static Sort convertSort(Context ctx, SMTSort sort) {
		switch (sort) {
		case BOOL:
			return ctx.getBoolSort();
		case INT:
			return ctx.getIntSort();
		}
		return null;
	}
	
	private static SortedSet<UserFunc> getAllUserFunctions(SMTExpression expr) {
		SortedSet<UserFunc> funcs = new TreeSet<>();
		Deque<UserFunc> funcQueue = new ArrayDeque<>();
		funcs.addAll(expr.getUserFunctions());
		funcQueue.addAll(expr.getUserFunctions());
		while (!funcQueue.isEmpty()) {
			UserFunc uf = funcQueue.removeFirst();
			for (UserFunc bodyUf : uf.getBody().getUserFunctions()) {
				if (!funcs.contains(bodyUf)) {
					funcs.add(bodyUf);
					funcQueue.addLast(bodyUf);
				}
			}
		}
		return funcs;
	}
	
	@Override
	public boolean checkSat(SMTExpression constraint, Map<Variable, Constant> model) {
		boolean toCheck = (model != null) && (!model.isEmpty());
		long startT = System.currentTimeMillis();
		boolean isSat = this.__checkSat(constraint, model);
		long endT = System.currentTimeMillis();
		Logger.debug("invoke Z3 Java API to " + (toCheck ? "check" : "solve") +
				", elapsed " + (endT - startT) + "ms");
		return isSat;
	}
	
	private boolean __checkSat(SMTExpression constraint, Map<Variable, Constant> model) {
		if (model != null) {
			constraint = constraint.getSubstitution(model);
		}
		
		Context ctx = new Context();
		StringBuilder sb = new StringBuilder();
		List<Symbol> declNames = new ArrayList<>();
		List<FuncDecl<?>> decls = new ArrayList<>();
		for (Variable v : constraint.getFreeVariables()) {
			// sb.append(v.getSMTDecl() + "\n");
			Symbol varSymb = ctx.mkSymbol(v.toSMTString());
			declNames.add(varSymb);
			FuncDecl<?> varDecl = ctx.mkConstDecl(varSymb, convertSort(ctx, v.getSMTSort()));
			decls.add(varDecl);
		}
		for (UserFunc uf : getAllUserFunctions(constraint)) {
			// 'define-fun' is semantically equivalent to 'declare-fun' + 'assert forall',
			// but the former is more efficient than the latter.
			// sb.append(uf.getSMTDecl() + "\n");
			// sb.append(uf.getSMTAssert() + "\n");
			sb.append(uf.getSMTDef() + "\n");
		}
		sb.append("(assert " + constraint.toSMTString() + ")\n");
		
		Solver z3Solver = ctx.mkSolver();
		BoolExpr[] es = ctx.parseSMTLIB2String(sb.toString(), null, null,
				declNames.toArray(new Symbol[0]), decls.toArray(new FuncDecl[0]));
		z3Solver.add(es);
		Status result = z3Solver.check();
		if (result == Status.UNSATISFIABLE) {
			ctx.close();
			return false;
		}
		if (result == Status.UNKNOWN) {
			Logger.warn("unknown solver result in checkSat");
			ctx.close();
			return false;
		}
		
		if (model != null) {
			Model z3Model = z3Solver.getModel();
			for (Variable var : constraint.getFreeVariables()) {
				if (model.containsKey(var)) continue;
				Symbol varSymb = ctx.mkSymbol(var.toSMTString());
				FuncDecl<?> varDecl = ctx.mkConstDecl(varSymb, convertSort(ctx, var.getSMTSort()));
				if (z3Model.getConstInterp(varDecl) == null) {
					switch (var.getSMTSort()) {
					case BOOL:
						model.put(var, BoolConst.DEFAULT);
						break;
					case INT:
						model.put(var, IntConst.DEFAULT);
						break;
					}
				} else {
					Expr<?> val = z3Model.getConstInterp(varDecl).simplify();
					switch (var.getSMTSort()) {
					case BOOL:
						model.put(var, new BoolConst(((BoolExpr) val).isTrue()));
						break;
					case INT:
						model.put(var, new IntConst(((IntNum) val).getInt64()));
						break;
					}
				}
			}
		}
		ctx.close();
		return true;
	}
	
	@Override
	public boolean checkSat$pAndNotq(SMTExpression p, ExistExpr q) {
		long startT = System.currentTimeMillis();
		boolean isSat = this.__checkSat$pAndNotq(p, q);
		long endT = System.currentTimeMillis();
		Logger.debug("invoke Z3 Java API to check implication, elapsed " + (endT - startT) + "ms");
		return isSat;
	}
	
	public boolean __checkSat$pAndNotq(SMTExpression p, ExistExpr q) {
		Context ctx = new Context();
		StringBuilder sb = new StringBuilder();
		List<Symbol> declNames = new ArrayList<>();
		List<FuncDecl<?>> decls = new ArrayList<>();
		Set<Variable> FVs = q.getBody().getFreeVariables();
		FVs.removeAll(q.getBoundVariables());
		FVs.addAll(p.getFreeVariables());
		for (Variable v : FVs) {
			Symbol varSymb = ctx.mkSymbol(v.toSMTString());
			declNames.add(varSymb);
			FuncDecl<?> varDecl = ctx.mkConstDecl(varSymb, convertSort(ctx, v.getSMTSort()));
			decls.add(varDecl);
		}
		Set<UserFunc> UFs = getAllUserFunctions(p);
		UFs.addAll(getAllUserFunctions(q.getBody()));
		for (UserFunc uf : UFs) {
			sb.append(uf.getSMTDef() + "\n");
		}
		sb.append("(assert " + p.toSMTString() + ")\n");
		sb.append("(assert (not " + q.toSMTString() + "))\n");
		
		Solver z3Solver = ctx.mkSolver();
		BoolExpr[] es = ctx.parseSMTLIB2String(sb.toString(), null, null,
				declNames.toArray(new Symbol[0]), decls.toArray(new FuncDecl[0]));
		z3Solver.add(es);
		Status result = z3Solver.check();
		if (result == Status.UNSATISFIABLE) {
			ctx.close();
			return false;
		} else if (result == Status.UNKNOWN) {
			Logger.warn("unknown solver result in checkSat$pAndNotq");
			ctx.close();
			return false;
		} else {
			ctx.close();
			return true;
		}
	}
	
	private Context incr_ctx = null;
	private Solver incr_solver = null;
	
	@Override
	public void initIncrSolver() {
		this.incr_ctx = new Context();
		this.incr_solver = incr_ctx.mkSolver();
	}
	
	@Override
	public void pushAssert(SMTExpression p) {
		StringBuilder sb = new StringBuilder();
		List<Symbol> declNames = new ArrayList<>();
		List<FuncDecl<?>> decls = new ArrayList<>();
		for (Variable v : p.getFreeVariables()) {
			Symbol varSymb = incr_ctx.mkSymbol(v.toSMTString());
			declNames.add(varSymb);
			FuncDecl<?> varDecl = incr_ctx.mkConstDecl(varSymb, convertSort(incr_ctx, v.getSMTSort()));
			decls.add(varDecl);
		}
		for (UserFunc uf : getAllUserFunctions(p)) {
			sb.append(uf.getSMTDef() + "\n");
		}
		sb.append("(assert " + p.toSMTString() + ")\n");
		BoolExpr[] es = incr_ctx.parseSMTLIB2String(sb.toString(), null, null,
				declNames.toArray(new Symbol[0]), decls.toArray(new FuncDecl[0]));
		incr_solver.add(es);
	}
	
	@Override
	public void pushAssertNot(ExistExpr p) {
		StringBuilder sb = new StringBuilder();
		List<Symbol> declNames = new ArrayList<>();
		List<FuncDecl<?>> decls = new ArrayList<>();
		Set<Variable> FVs = p.getBody().getFreeVariables();
		FVs.removeAll(p.getBoundVariables());
		for (Variable v : FVs) {
			Symbol varSymb = incr_ctx.mkSymbol(v.toSMTString());
			declNames.add(varSymb);
			FuncDecl<?> varDecl = incr_ctx.mkConstDecl(varSymb, convertSort(incr_ctx, v.getSMTSort()));
			decls.add(varDecl);
		}
		for (UserFunc uf : getAllUserFunctions(p.getBody())) {
			sb.append(uf.getSMTDef() + "\n");
		}
		sb.append("(assert (not " + p.toSMTString() + "))\n");
		BoolExpr[] es = incr_ctx.parseSMTLIB2String(sb.toString(), null, null,
				declNames.toArray(new Symbol[0]), decls.toArray(new FuncDecl[0]));
		incr_solver.add(es);
	}
	
	@Override
	public void endPushAssert() {
		this.incr_solver.push();
	}
	
	@Override
	public boolean checkSatIncr(SMTExpression p) {
		long start = System.currentTimeMillis();
		StringBuilder sb = new StringBuilder();
		List<Symbol> declNames = new ArrayList<>();
		List<FuncDecl<?>> decls = new ArrayList<>();
		for (Variable v : p.getFreeVariables()) {
			Symbol varSymb = incr_ctx.mkSymbol(v.toSMTString());
			declNames.add(varSymb);
			FuncDecl<?> varDecl = incr_ctx.mkConstDecl(varSymb, convertSort(incr_ctx, v.getSMTSort()));
			decls.add(varDecl);
		}
		for (UserFunc uf : getAllUserFunctions(p)) {
			sb.append(uf.getSMTDef() + "\n");
		}
		sb.append("(assert " + p.toSMTString() + ")\n");
		BoolExpr[] es = incr_ctx.parseSMTLIB2String(sb.toString(), null, null,
				declNames.toArray(new Symbol[0]), decls.toArray(new FuncDecl[0]));
		incr_solver.add(es);
		Status result = incr_solver.check();
		final boolean isSat;
		if (result == Status.SATISFIABLE) {
			isSat = true;
		} else if (result == Status.UNSATISFIABLE) {
			isSat = false;
		} else {
			Logger.warn("unknown solver result in checkSatIncr");
			isSat = false;
		}
		incr_solver.pop();
		incr_solver.push();
		long elapsed = System.currentTimeMillis() - start;
		Logger.debug("invoke Z3 Java API to incrementally check, elapsed " + elapsed + "ms");
		return isSat;
	}
	
	@Override
	public void closeIncrSolver() {
		this.incr_ctx.close();
		this.incr_ctx = null;
		this.incr_solver = null;
	}	

}
