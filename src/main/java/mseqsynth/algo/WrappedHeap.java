package mseqsynth.algo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import mseqsynth.common.Logger;
import mseqsynth.common.settings.Options;
import mseqsynth.heap.ActionIfFound;
import mseqsynth.heap.FieldH;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ApplyExpr;
import mseqsynth.smtlib.BoolConst;
import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.Constant;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntConst;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTExpression;
import mseqsynth.smtlib.SMTOperator;
import mseqsynth.smtlib.SMTSort;
import mseqsynth.smtlib.UserFunc;
import mseqsynth.smtlib.Variable;
import mseqsynth.util.Bijection;
import mseqsynth.wrapper.smt.SMTSolver;
import mseqsynth.wrapper.symbolic.PathDescriptor;
import mseqsynth.wrapper.symbolic.Specification;

public class WrappedHeap implements Serializable, Comparable<WrappedHeap> {
	
	private static final long serialVersionUID = 3749580422241371835L;
	
	@Override
	public int compareTo(WrappedHeap o) {
		return Integer.compare(this.__heapID, o.__heapID);
	}
	
	/*============== debugging information ******************/
	private static int __countHeapGenerated = 0;
	private int __heapID;
	private String __heapName;
	private Map<ObjectH, String> __objNameMap;
	private List<UserFunc> __funCreated;
	
	private void __generateDebugInformation() {
		this.__heapID = WrappedHeap.__countHeapGenerated++;
		this.__heapName = "[H" + this.__heapID + "]";
		this.__objNameMap = new HashMap<>();
		this.__funCreated = new ArrayList<>();
		int countNonNullObjs = 0;
		for (ObjectH o : this.heap.getAllObjects()) {
			if (o.isNullObject()) {
				this.__objNameMap.put(o, "null");
			} else if (o.isVariable()) {
				this.__objNameMap.put(o, o.getVariable().toSMTString());
			} else {
				this.__objNameMap.put(o, "o" + (countNonNullObjs++) + "#H" + this.__heapID);
			}
		}
	}
	
	public int __debugGetID() {
		return this.__heapID;
	}
	
	public String __debugGetName() {
		return this.__heapName;
	}
	
	public void __debugPrintOut(PrintStream ps) {
		ps.print(">> Heap " + __heapName + ": " + status + ", ");
		if (heap.getConstraint() != null) {
			ps.println(heap.getConstraint().toSMTString());
		} else {
			ps.println("constraint undetermined");
		}
		for (UserFunc uf : __funCreated) {
			ps.println("   " + uf.getSMTDef());
		}
		for (ObjectH o : heap.getAllObjects()) {
			if (!o.isNonNullObject()) continue;
			if (heap.getAccessibleObjects().contains(o)) {
				ps.print("<" + __objNameMap.get(o) + ">");
			} else {
				ps.print(" " + __objNameMap.get(o) + " ");
			}
			ps.print(":");
			for (FieldH field : o.getFields())
				ps.print(" (." + field.getName() + ", " + __objNameMap.get(o.getFieldValue(field)) + ")");
			ps.println();
		}
		if (heap.getAccessibleObjects().stream().anyMatch(o -> o.isVariable())) {
			for (ObjectH o : heap.getAccessibleObjects()) {
				if (o.isVariable())
					ps.print("<" + __objNameMap.get(o) + "> ");
			}
			ps.println();
		}
		if (!renameMapList.isEmpty()) {
			ps.println("variable renaming map list:");
			for (Map<Variable, Variable> renameMap : renameMapList) {
				ps.print("   { ");
				for (Entry<Variable, Variable> entry : renameMap.entrySet()) {
					ps.print(entry.getKey().toSMTString() + "=>");
					ps.print(entry.getValue().toSMTString() + ", ");
				}
				ps.println("}");
			}
		}
		if (!indicators.isEmpty()) {
			ps.println("indicator variables:");
			ps.print("   { ");
			for (Variable var : indicators) {
				ps.print(var.toSMTString() + ", ");
			}
			ps.println("}");
		}
		for (BackwardRecord br : rcdBackwards) {
			ps.print("original heap " + br.oriHeap.__heapName);
			if (br.mInvoke != null) {
				ps.print(", invoke " + br.mInvoke.getJavaMethod().getName() + "(");
				StringBuilder sb = new StringBuilder();
				for (ObjectH arg : br.mInvoke.getInvokeArguments()) {
					if (arg.isHeapObject()) {
						sb.append(br.oriHeap.__objNameMap.get(arg) + ", ");
					} else {
						sb.append(arg.getVariable().toSMTString() + ", ");
					}
				}
				sb.delete(Math.max(0,  sb.length() - 2), sb.length());
				ps.print(sb.toString() + ")");
				if (br.retVal != null) {
					ps.print(", return value is " + __objNameMap.get(br.retVal));
				}
			} else {
				ps.print(", isomorphic");
			}
			ps.println();
			if (!br.objSrcMap.isEmpty()) {
				ps.print("   ");
				for (Entry<ObjectH, ObjectH> entry : br.objSrcMap.entrySet()) {
					if (entry.getKey().isNonNullObject()) {
						ps.print(__objNameMap.get(entry.getKey()) + "<=");
						ps.print(br.oriHeap.__objNameMap.get(entry.getValue()) + ", ");
					}
				}
				ps.println();
			}
			if (!br.varExprMap.isEmpty()) {
				ps.print("   ");
				for (Entry<Variable, SMTExpression> entry : br.varExprMap.entrySet()) {
					ps.print(entry.getKey().toSMTString() + ":=");
					ps.print(entry.getValue().toSMTString() + ", ");
				}
				ps.println();
			}
			if (!br.guardCondList.isEmpty()) {
				for (ExistExpr guardCond : br.guardCondList) {
					ps.println("   " + guardCond.toSMTString());
				}
			} else {
				ps.println("   guard condition undetermined");
			}
		}
		for (ForwardRecord fr : rcdForwards) {
			if (fr.mInvoke == null) {
				ps.println("isomorphic to " + fr.finHeap.__heapName);
			} else {
				ps.print("generate " + fr.finHeap.__heapName + " by invoking ");
				ps.print(fr.mInvoke.getJavaMethod().getName() + "(");
				StringBuilder sb = new StringBuilder();
				for (ObjectH arg : fr.mInvoke.getInvokeArguments()) {
					if (arg.isHeapObject()) {
						sb.append(__objNameMap.get(arg) + ", ");
					} else {
						sb.append(arg.getVariable().toSMTString() + ", ");
					}
				}
				sb.delete(Math.max(0, sb.length() - 2), sb.length());
				ps.print(sb.toString() + "), ");
				if (fr.pathCond != null) {
					ps.println("path condition is " + fr.pathCond.toSMTString());
				} else {
					ps.println("path condition is true");
				}
			}
		}
		ps.println();
	}
	/*====================== debugging information ====================*/
	
	
	// symbolic heap
	private SymbolicHeap heap;
	
	// status while building heap transformation graph
	private HeapStatus status;
	
	// record for backtracking
	static class BackwardRecord implements Serializable {
		private static final long serialVersionUID = -333529127300593092L;
		WrappedHeap oriHeap;
		MethodInvoke mInvoke;
		SMTExpression pathCond;
		ObjectH retVal;
		Map<ObjectH, ObjectH> objSrcMap;
		Map<Variable, SMTExpression> varExprMap;
		ArrayList<ExistExpr> guardCondList;
	}
	
	private ArrayList<BackwardRecord> rcdBackwards;
	private ArrayList<Map<Variable, Variable>> renameMapList;
	private ArrayList<Variable> indicators;
	
	private void addBackwardRecord(WrappedHeap oriHeap, MethodInvoke mInvoke,
			SMTExpression pathCond, ObjectH retVal,
			Map<ObjectH, ObjectH> objSrcMap,
			Map<Variable, SMTExpression> varExprMap) {
		BackwardRecord br = new BackwardRecord();
		br.oriHeap = oriHeap;
		br.mInvoke = mInvoke;
		br.pathCond = pathCond;
		br.retVal = retVal;
		br.objSrcMap = ImmutableMap.copyOf(objSrcMap);
		br.varExprMap = ImmutableMap.copyOf(varExprMap);
		br.guardCondList = new ArrayList<>();
		if (!this.rcdBackwards.isEmpty()) {
			int maxVersion = this.rcdBackwards.stream()
				.mapToInt(o -> o.guardCondList.size()).max().getAsInt();
			for (int i = 0; i < maxVersion; ++i)
				br.guardCondList.add(ExistExpr.ALWAYS_FALSE);
		}
		this.rcdBackwards.add(br);
	}

	
	// record for forward traversal
	static class ForwardRecord implements Serializable {
		private static final long serialVersionUID = 5004804424170987685L;
		WrappedHeap finHeap;
		Bijection<ObjectH, ObjectH> mapping;
		MethodInvoke mInvoke;
		SMTExpression pathCond;
	}
	
	private ArrayList<ForwardRecord> rcdForwards;
	
	private void addForwardRecord(WrappedHeap finHeap, Bijection<ObjectH, ObjectH> mapping, 
			MethodInvoke mInvoke, SMTExpression pathCond) {
		ForwardRecord fr = new ForwardRecord();
		fr.finHeap = finHeap;
		fr.mapping = mapping;
		fr.mInvoke = mInvoke;
		fr.pathCond = pathCond;
		this.rcdForwards.add(fr);
	}
	
	
	// constructor for initial heap
	public WrappedHeap(SymbolicHeap initHeap) {
		this.heap = initHeap;
		this.status = HeapStatus.ACTIVE;
		this.rcdBackwards = new ArrayList<>();
		this.renameMapList = new ArrayList<>();
		this.rcdForwards = new ArrayList<>();
		this.indicators = new ArrayList<>();
		__generateDebugInformation();
	}
	
	// constructed by invoking a public method
	public WrappedHeap(WrappedHeap oriHeap, MethodInvoke mInvoke, PathDescriptor pd) {
		this(pd.finHeap);
		this.addBackwardRecord(oriHeap, mInvoke, pd.pathCond, pd.retVal,
				pd.objSrcMap, pd.varExprMap);
		oriHeap.addForwardRecord(this, null, mInvoke, pd.pathCond);
	}
	
	// subsume an isomorphic heap
	public void subsumeHeap(WrappedHeap otherHeap, Bijection<ObjectH, ObjectH> isoMap) {
		Preconditions.checkArgument(isoMap.getMapU2V().keySet()
				.equals(otherHeap.heap.getAllObjects()));
		Preconditions.checkArgument(isoMap.getMapV2U().keySet()
				.equals(this.heap.getAllObjects()));
		
		Map<Variable, SMTExpression> varExprMap = new HashMap<>();
		for (ObjectH o : this.heap.getAllObjects()) {
			if (o.isVariable())
				varExprMap.put(o.getVariable(), isoMap.getU(o).getVariable());
		}
		this.addBackwardRecord(otherHeap, null, null, null, isoMap.getMapV2U(), varExprMap);
		otherHeap.addForwardRecord(this, isoMap, null, null);
		otherHeap.status = HeapStatus.SUBSUMED;
	}

	// recompute the constraint of this heap
	public void recomputeConstraint() {
		recomputeConstraint(null);
	}
	public void recomputeConstraint(SMTSolver checker) {
		if (this.rcdBackwards.isEmpty())
			return;
		
		Variable indicator = new IntVar();
		for (int index = 0; index < this.rcdBackwards.size(); ++index) {
			BackwardRecord br = this.rcdBackwards.get(index);
			List<SMTExpression> andClauses = new ArrayList<>();
			List<Variable> boundVars = new ArrayList<>();
			boundVars.addAll(br.oriHeap.heap.getConstraint().getBoundVariables());
			andClauses.add(br.oriHeap.heap.getConstraint().getBody());
			boundVars.addAll(br.oriHeap.heap.getVariables());
			if (br.mInvoke != null) {
				boundVars.addAll(br.mInvoke.getInvokeArguments().stream()
						.filter(o -> o.isVariable())
						.map(o -> o.getVariable())
						.collect(Collectors.toList()));
			}
			if (br.pathCond != null) {
				andClauses.add(br.pathCond);
			}
			for (Variable var : this.heap.getVariables()) {
				if (br.varExprMap.containsKey(var)) {
					SMTExpression clause = new ApplyExpr(SMTOperator.BIN_EQ,
							var, br.varExprMap.get(var));
					andClauses.add(clause);
				}
			}
			andClauses.add(new ApplyExpr(SMTOperator.BIN_EQ,
					indicator, new IntConst(index)));
			ExistExpr guardCond = new ExistExpr(boundVars,
					new ApplyExpr(SMTOperator.AND, andClauses));
			assert(Sets.difference(
					guardCond.getBody().getFreeVariables(),
					guardCond.getBoundVariables()).immutableCopy().equals(
							ImmutableSet.copyOf(Sets.union(
									ImmutableSet.copyOf(this.heap.getVariables()),
									ImmutableSet.of(indicator)									
									))
							));
			if (checker == null || checker.checkSat(guardCond.getBody(), null)) {
				br.guardCondList.add(guardCond);
			} else {
				br.guardCondList.add(new ExistExpr(boundVars, BoolConst.FALSE));
			}
		}
		
		Map<Variable, Variable> newRenameMap = new HashMap<>();
		List<ExistExpr> orClauses = this.rcdBackwards.stream()
				.map(o -> o.guardCondList.get(o.guardCondList.size() - 1))
				.collect(Collectors.toList());
		ExistExpr orExpr = ExistExpr.makeOr(orClauses, newRenameMap);
		this.renameMapList.add(newRenameMap);
		
		List<Variable> funcArgs = new ArrayList<>(this.heap.getVariables());
		funcArgs.addAll(orExpr.getBoundVariables());
		funcArgs.add(indicator);
		UserFunc func = new UserFunc(funcArgs, SMTSort.BOOL, orExpr.getBody());
		this.__funCreated.add(func);
		List<Variable> boundVars = new ArrayList<>(orExpr.getBoundVariables());
		boundVars.add(indicator);
		ExistExpr constraint = new ExistExpr(boundVars, new ApplyExpr(func, funcArgs));
		this.heap.setConstraint(constraint);
		this.indicators.add(indicator);
	}
	
	class MatchResult implements ActionIfFound {
		private Specification spec;
		public Map<Variable, Constant> model;
		public Map<ObjectH, ObjectH> objSrcMap;
		
		public MatchResult(Specification spec) {
			this.spec = spec;
			this.model = new HashMap<>();
			this.objSrcMap = new HashMap<>();
		}
		
		@Override
		public boolean emitMapping(Bijection<ObjectH, ObjectH> isoMap) {
			List<SMTExpression> conds = new ArrayList<>();
			conds.add(WrappedHeap.this.heap.getConstraint().getBody()); // heap constraint
			conds.add(this.spec.condition); // path condition
			
			// conditions about embedding
			for (Entry<ObjectH, ObjectH> e : isoMap.getMapU2V().entrySet()) {
				if (e.getKey().isVariable()) {
					conds.add(new ApplyExpr(SMTOperator.BIN_EQ,
							e.getKey().getVariable(), e.getValue().getVariable()));
				}
			}
			
			// conditions about accessibility
			Set<ObjectH> os = WrappedHeap.this.heap.getAllObjects().stream()
					.filter(o -> o.getClassH().getJavaClass() == Object.class)
					.collect(Collectors.toSet());
			Set<ObjectH> aos = WrappedHeap.this.heap.getAccessibleObjects().stream()
					.filter(o -> o.getClassH().getJavaClass() == Object.class)
					.collect(Collectors.toSet());
			if (!aos.containsAll(os)) {
				for (ObjectH r : this.spec.expcHeap.getAccessibleObjects()) {
					if (r.getClassH().getJavaClass() != Object.class) continue;
					List<SMTExpression> andClauses = new ArrayList<>();
					for (ObjectH o : os) {
						SMTExpression clause = new ApplyExpr(SMTOperator.DISTINCT,
								r.getVariable(), o.getVariable());
						andClauses.add(clause);
					}
					List<SMTExpression> orClauses = Lists.newArrayList(
							new ApplyExpr(SMTOperator.AND, andClauses));
					for (ObjectH ao : aos) {
						SMTExpression clause = new ApplyExpr(SMTOperator.BIN_EQ,
								r.getVariable(), ao.getVariable());
						orClauses.add(clause);
					}
					conds.add(new ApplyExpr(SMTOperator.OR, orClauses));
				}
			}
			
			SMTExpression constraint = new ApplyExpr(SMTOperator.AND, conds);
			SMTSolver solver = Options.I().getSMTSolver();
			if (solver.checkSat(constraint, this.model)) {
				for (ObjectH o : this.spec.expcHeap.getAccessibleObjects()) {
					if (o.isHeapObject())
						this.objSrcMap.put(o, isoMap.getV(o));
				}
				return true;
			}
			return false;
		}
		
	}
	
	// try to match a test specification
	public MatchResult matchSpecification(Specification spec) {
		if (spec.condition == null) {
			spec.condition = new BoolConst(true);
		}
	 	MatchResult action = new MatchResult(spec);
	 	SymbolicHeap coreHeap = new SymbolicHeapAsDigraph(
	 			Sets.filter(spec.expcHeap.getAccessibleObjects(), o -> o.isHeapObject()), null);
	 	if (coreHeap.findEmbeddingInto(this.heap, action)) {
	 		return action;
	 	}
		return null;
	}
	
	public void setUnsat() {
		this.status = HeapStatus.UNSAT;
	}
	
	public void setActive() {
		this.status = HeapStatus.ACTIVE;
	}
	
	public boolean isUnsat() {
		return this.status.equals(HeapStatus.UNSAT);
	}
	
	public boolean isActive() {
		return this.status.equals(HeapStatus.ACTIVE);
	}
	
	public boolean isSubsumed() {
		return this.status.equals(HeapStatus.SUBSUMED);
	}
	
	public List<ForwardRecord> getForwardRecords() {
		return ImmutableList.copyOf(this.rcdForwards);
	}
	
	public List<BackwardRecord> getBackwardRecords() {
		return ImmutableList.copyOf(this.rcdBackwards);
	}
	
	SymbolicHeap getHeap() {
		return this.heap;
	}
	
	ArrayList<Map<Variable, Variable>> getRenameMapList() {
		return this.renameMapList;
	}
	
	ArrayList<Variable> getIndicatorVariables() {
		return this.indicators;
	}
	
	boolean surelyEntails(WrappedHeap other, Bijection<ObjectH, ObjectH> mapping, SMTSolver solver) {
		ExistExpr oldP = this.heap.getConstraint();
		this.recomputeConstraint();
		Map<Variable, Variable> renameMap = deriveVariableMapping(mapping.getMapV2U()); 
		List<Variable> Xs = this.heap.getVariables();
		assert(Xs.containsAll(renameMap.values()));
		Set<Variable> As = this.heap.getConstraint().getBoundVariables();
		Set<Variable> Bs = other.heap.getConstraint().getBoundVariables();
		assert(Sets.intersection(As, Bs).isEmpty());
		SMTExpression p = this.heap.getConstraint().getBody();
		SMTExpression q = other.heap.getConstraint().getBody().getSubstitution(renameMap);
		// forall Xs, (exists As, p(Xs, As)) -> (exists Bs, q(Xs, Bs))
		// <-->
		// forall Xs, forall As, p(Xs, As) -> exists Bs, q(Xs, Bs)
		// <-->
		// ~ (exists Xs, exists As, p(Xs, As) /\ (~ exists Bs, q(Xs, Bs)))
		boolean entails = !solver.checkSat$pAndNotq(p, new ExistExpr(Bs, q));
		if (entails) {
			Logger.info(this.__debugGetName() + " ---> " + other.__debugGetName());
		} else {
			Logger.info(this.__debugGetName() + " -\\-> " + other.__debugGetName());
		}
		this.heap.setConstraint(oldP);
		return entails;
	}
	
	/*===== auxiliary information for graph building algorithm =====*/
	transient boolean isEverExpanded = false;
	
	public static Map<Variable, Variable>
	deriveVariableMapping(Map<ObjectH, ObjectH> mapping) {
		Map<Variable, Variable> varMapping = new HashMap<>();
		for (Entry<ObjectH, ObjectH> entry : mapping.entrySet()) {
			if (entry.getKey().isVariable())
				varMapping.put(entry.getKey().getVariable(), entry.getValue().getVariable());
		}
		return varMapping;
	}
	
	public static void exportHeapsTo(List<WrappedHeap> heaps, String filename)
			throws IOException {
		FileOutputStream fos = new FileOutputStream(filename);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(heaps);
		// DO NOT forget to write static fields!!!
		oos.writeInt(IntVar.getCounter());
		oos.writeInt(BoolVar.getCounter());
		oos.writeInt(UserFunc.getCounter());
		oos.close();
		fos.close();
	}
	
	public static List<WrappedHeap> importHeapsFrom(String filename)
			throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fis);
		List<WrappedHeap> heaps = ((List<?>) ois.readObject()).stream()
				.map(o -> (WrappedHeap) o).collect(Collectors.toList());
		IntVar.resetCounter(ois.readInt());
		BoolVar.resetCounter(ois.readInt());
		UserFunc.resetCounter(ois.readInt());
		ois.close();
		fis.close();
		return heaps;
	}
	
}
