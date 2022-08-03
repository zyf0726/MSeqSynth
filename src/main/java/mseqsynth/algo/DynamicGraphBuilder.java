package mseqsynth.algo;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mseqsynth.algo.WrappedHeap.BackwardRecord;
import mseqsynth.algo.WrappedHeap.ForwardRecord;
import mseqsynth.common.Logger;
import mseqsynth.common.exceptions.UnsupportedPrimitiveType;
import mseqsynth.common.settings.Options;
import mseqsynth.heap.ActionIfFound;
import mseqsynth.heap.ClassH;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntVar;
import mseqsynth.util.Bijection;
import mseqsynth.wrapper.smt.IncrSMTSolver;
import mseqsynth.wrapper.symbolic.PathDescriptor;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;

public class DynamicGraphBuilder {
	
	private final boolean checkSubsumption;
	
	private IncrSMTSolver solver;
	private SymbolicExecutor executor;
	private List<Method> methods;
	private Map<ClassH, Integer> heapScope;
	
	private ArrayList<WrappedHeap> allHeaps;
	private Map<Long, List<WrappedHeap>> activeHeapsByCode;
	
	public DynamicGraphBuilder(SymbolicExecutor executor, Collection<Method> methods) {
		this(executor, methods, true);
	}
	
	public DynamicGraphBuilder(SymbolicExecutor executor, Collection<Method> methods,
			boolean checkSubsumption) {
		this.checkSubsumption = checkSubsumption;
		this.solver = Options.I().getSMTSolver();
		this.executor = executor;
		this.methods = ImmutableList.copyOf(methods);
		this.heapScope = new HashMap<>();
		this.allHeaps = new ArrayList<>();
		this.activeHeapsByCode = new HashMap<>();
	}
	
	public void setHeapScope(Class<?> javaClass, int scope) {
		this.heapScope.put(ClassH.of(javaClass), scope);
	}
	
	class FindOneMapping implements ActionIfFound {
		Bijection<ObjectH, ObjectH> mapping = null;
		@Override
		public boolean emitMapping(Bijection<ObjectH, ObjectH> ret) {
			this.mapping = ret;
			return true;
		}
	}
	
	private void addNewHeap(WrappedHeap newHeap) {
		assert(!this.isOutOfScope(newHeap.getHeap()));
		assert(newHeap.isActive());
		SymbolicHeap newSymHeap = newHeap.getHeap();
		long code = newSymHeap.getFeatureCode();
		List<WrappedHeap> activeHeaps = this.activeHeapsByCode.get(code);
		if (activeHeaps == null) {
			this.activeHeapsByCode.put(code, Lists.newArrayList(newHeap));
		} else {
			boolean subsumed = false;
			for (WrappedHeap activeHeap : activeHeaps) {
				SymbolicHeap activeSymHeap = activeHeap.getHeap();
				if (!newSymHeap.maybeIsomorphicWith(activeSymHeap))
					continue;
				FindOneMapping action = new FindOneMapping();
				if (!newSymHeap.findIsomorphicMappingTo(activeSymHeap, action))
					continue;
				subsumed = true;
				activeHeap.subsumeHeap(newHeap, action.mapping);
				break;
			}
			if (!subsumed) activeHeaps.add(newHeap);
		}
		this.allHeaps.add(newHeap);
	}
	
	private TreeSet<WrappedHeap> expandGraph(TreeSet<WrappedHeap> updHeaps, int curLength, long startTime) {
		TreeSet<WrappedHeap> restHeaps = new TreeSet<>(updHeaps);
		updHeaps = new TreeSet<>();
		while (!restHeaps.isEmpty()) {
			WrappedHeap curHeap = restHeaps.pollFirst();
			assert(curHeap.isActive());
			Logger.info("current heap is " + curHeap.__debugGetName() + " of length " + curLength);
			
//			ExistExpr oldP = curHeap.getHeap().getConstraint();
			curHeap.recomputeConstraint();
			ExistExpr newP = curHeap.getHeap().getConstraint();
			this.solver.initIncrSolver();
			this.solver.pushAssert(newP.getBody());
//			if (curLength != 0) {
//				this.solver.pushAssertNot(oldP);
//			} 
			this.solver.endPushAssert();
			
			List<WrappedHeap> finHeaps = new ArrayList<>();
			if (curHeap.isEverExpanded) {
				curHeap.getForwardRecords().forEach(fr -> finHeaps.add(fr.finHeap));
			} else {
				for (Method method : this.methods) {
					for (WrappedHeap newHeap : this.tryInvokeMethod(curHeap, method)) {
						newHeap.setUnsat();
						finHeaps.add(newHeap);
					}
				}
			}
			
			for (WrappedHeap finHeap : finHeaps) {
				if (System.currentTimeMillis() - startTime > 1000 * Options.I().getTimeBudget()) {
					continue; // timeout
				}
				BackwardRecord br = finHeap.getBackwardRecords().stream()
						.filter(r -> r.oriHeap == curHeap).findAny().get();
				if (finHeap.isUnsat()) {
					if (br.pathCond != null && !this.solver.checkSatIncr(br.pathCond))
						continue;
					finHeap.setActive();
					this.addNewHeap(finHeap);
				}
				WrappedHeap succHeap = null;
				if (finHeap.isSubsumed()) {
					finHeap.recomputeConstraint();
					ForwardRecord fr = finHeap.getForwardRecords().get(0);
					if (this.checkSubsumption && finHeap.surelyEntails(fr.finHeap, fr.mapping, this.solver)) {
						finHeap.getHeap().setConstraint(ExistExpr.ALWAYS_FALSE);
					} else {
						succHeap = fr.finHeap;
					}
				} else {
					succHeap = finHeap;
				}
				if (succHeap != null && !restHeaps.contains(succHeap)) {
					updHeaps.add(succHeap);
				}
			}
			curHeap.isEverExpanded = true;
		}
		return updHeaps;
	}
	
	public List<WrappedHeap> buildGraph(SymbolicHeap symHeap, int maxSeqLen) {
		long startTime = System.currentTimeMillis();
		Logger.info("[DynamicGraphBuilder.buildGraph] maxLength = " + maxSeqLen);
		WrappedHeap initHeap = new WrappedHeap(symHeap);
		this.addNewHeap(initHeap);
		TreeSet<WrappedHeap> updHeaps = new TreeSet<>();
		updHeaps.add(initHeap);
		for (int L = 0; L < maxSeqLen; ++L) {
			updHeaps = this.expandGraph(updHeaps, L, startTime);
		}
		for (WrappedHeap heap : updHeaps) {
			Logger.info("compute heap constraint for " + heap.__debugGetName() +
					" of length " + maxSeqLen);
			heap.recomputeConstraint();
		}
		if (System.currentTimeMillis() - startTime > 1000 * Options.I().getTimeBudget()) {
			Logger.info("time budget " + Options.I().getTimeBudget() + " seconds exhausted");
		}
		return ImmutableList.copyOf(this.allHeaps);
	}
	
	private Collection<WrappedHeap>
	tryInvokeMethod(WrappedHeap oriHeap, Method method) {
		ArrayList<Class<?>> paraTypes = Lists.newArrayList(method.getParameterTypes());
		if (!Modifier.isStatic(method.getModifiers())) {
			paraTypes.add(0, method.getDeclaringClass());
		}
		Collection<ArrayList<ObjectH>> invokeArgSeqs = fillInvokeArguments(
				paraTypes.size(), paraTypes, oriHeap.getHeap().getAccessibleObjects());
		Collection<WrappedHeap> finHeaps = new ArrayList<>();
		for (ArrayList<ObjectH> invokeArgSeq : invokeArgSeqs) {
			MethodInvoke mInvoke = new MethodInvoke(method, invokeArgSeq);
			for (PathDescriptor pd : this.executor.executeMethod(oriHeap.getHeap(), mInvoke)) {
				if (!this.isOutOfScope(pd.finHeap))
					finHeaps.add(new WrappedHeap(oriHeap, mInvoke, pd));
			}
		}
		return finHeaps;
	}
	
	private static Collection<ArrayList<ObjectH>>
	fillInvokeArguments(int nRemain, ArrayList<Class<?>> paraTypes, Collection<ObjectH> objs) {
		if (nRemain == 0) {
			return Collections.singletonList(Lists.newArrayList());
		}
		Collection<ArrayList<ObjectH>> argSeqs =
				fillInvokeArguments(nRemain - 1, paraTypes, objs);
		Class<?> paraType = paraTypes.get(nRemain - 1);
		if (paraType.isPrimitive()) {
			String typeName = paraType.getName();
			ObjectH arg = null;
			if (Arrays.asList("byte", "char", "int", "long", "short").contains(typeName)) {
				arg = new ObjectH(new IntVar());
			} else if ("boolean".equals(typeName)) {
				arg = new ObjectH(new BoolVar());
			} else {
				throw new UnsupportedPrimitiveType(typeName);
			}
			for (ArrayList<ObjectH> argSeq : argSeqs)
				argSeq.add(arg);
			return argSeqs;
		} else if (paraType == Object.class) {
			ObjectH arg = new ObjectH(ClassH.of(paraType), Collections.emptyMap());
			for (ArrayList<ObjectH> argSeq : argSeqs)
				argSeq.add(arg);
			return argSeqs;
		} else {
			Collection<ArrayList<ObjectH>> extArgSeqs = new ArrayList<>();
			for (ArrayList<ObjectH> argSeq : argSeqs) {
				for (ObjectH arg : objs) {
					if (arg.isNullObject() || paraType.equals(arg.getClassH().getJavaClass())) {
						ArrayList<ObjectH> extArgSeq = new ArrayList<>(argSeq);
						extArgSeq.add(arg);
						extArgSeqs.add(extArgSeq);
					}
				}
			}
			return extArgSeqs;
		}
	}
	
	
	private boolean isOutOfScope(SymbolicHeap heap) {
		Map<ClassH, Integer> countObjs = new HashMap<>();
		for (ObjectH o : heap.getAllObjects()) {
			if (o.isNonNullObject()) {
				ClassH cls = o.getClassH();
				int cnt = countObjs.getOrDefault(cls, 0) + 1;
				int limit = this.heapScope.getOrDefault(cls, 4); // TODO
				if (cnt > limit) return true;
				countObjs.put(o.getClassH(), cnt);
			}
		}
		ClassH clsObject = ClassH.of(Object.class);
		if (this.heapScope.containsKey(clsObject)) {
			long cnt = heap.getAllObjects().stream()
					.filter(o -> o.getClassH() == clsObject).count();
			return cnt > this.heapScope.get(clsObject);
		}
		return false;
	}
	
	public static void __debugPrintOut(List<WrappedHeap> heaps,
			SymbolicExecutor executor, PrintStream ps) {
		long countActive = heaps.stream().filter(o -> o.isActive()).count();
		ps.println("number of active/all heaps = " + countActive + "/" + heaps.size());
		long countTrans = heaps.stream()
				.map(o -> o.getBackwardRecords().size())
				.reduce(0, (a, b) -> a + b);
		ps.println("number of transitions = " + countTrans);
		ps.println("number of symbolic executions = " + executor.getExecutionCount());
		ps.println();
		for (WrappedHeap heap : heaps)
			heap.__debugPrintOut(ps);
	}
	
}
