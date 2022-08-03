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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mseqsynth.algo.WrappedHeap.BackwardRecord;
import mseqsynth.common.Logger;
import mseqsynth.common.exceptions.UnsupportedPrimitiveType;
import mseqsynth.common.settings.Options;
import mseqsynth.heap.ActionIfFound;
import mseqsynth.heap.ClassH;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.IntVar;
import mseqsynth.util.Bijection;
import mseqsynth.wrapper.smt.IncrSMTSolver;
import mseqsynth.wrapper.symbolic.PathDescriptor;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;

public class GraphBuilderWithoutMerging {

	private final boolean checkSubsumption;
	
	private IncrSMTSolver solver;
	private SymbolicExecutor executor;
	private List<Method> methods;
	private Map<ClassH, Integer> heapScope;
	
	private ArrayList<WrappedHeap> allHeaps;
	private Map<Long, List<WrappedHeap>> activeHeapsByCode;
	
	public GraphBuilderWithoutMerging(SymbolicExecutor executor, Collection<Method> methods) {
		this(executor, methods, false);
	}
	
	public GraphBuilderWithoutMerging(SymbolicExecutor executor, Collection<Method> methods,
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
	
	private boolean addNewHeap(WrappedHeap newHeap) {
		assert(!this.isOutOfScope(newHeap.getHeap()));
		assert(newHeap.isActive());
		newHeap.recomputeConstraint();
		SymbolicHeap newSymHeap = newHeap.getHeap();
		long code = newSymHeap.getFeatureCode();
		List<WrappedHeap> activeHeaps = this.activeHeapsByCode.get(code);
		if (activeHeaps == null) {
			this.activeHeapsByCode.put(code, Lists.newArrayList(newHeap));
		} else {
			for (WrappedHeap activeHeap : activeHeaps) {
				SymbolicHeap activeSymHeap = activeHeap.getHeap();
				if (!newSymHeap.maybeIsomorphicWith(activeSymHeap))
					continue;
				FindOneMapping action = new FindOneMapping();
				if (!newSymHeap.findIsomorphicMappingTo(activeSymHeap, action))
					continue;
				if (this.checkSubsumption && newHeap.surelyEntails(activeHeap, action.mapping, this.solver)) {
					newHeap.setUnsat();
					return false;
				}
			}
			activeHeaps.add(newHeap);
		}
		this.allHeaps.add(newHeap);
		return true;
	}
	
	private List<WrappedHeap> expandGraph(List<WrappedHeap> oldHeaps, int curLength, long startTime) {
		List<WrappedHeap> newHeaps = new ArrayList<>();
		for (WrappedHeap curHeap : oldHeaps) {
			assert(curHeap.isActive());
			Logger.info("current heap is " + curHeap.__debugGetName() + " of length " + curLength);
			
			this.solver.initIncrSolver();
			this.solver.pushAssert(curHeap.getHeap().getConstraint().getBody());
			this.solver.endPushAssert();
			
			List<WrappedHeap> finHeaps = new ArrayList<>();
			for (Method method : this.methods) {
				for (WrappedHeap finHeap : this.tryInvokeMethod(curHeap, method)) {
					finHeap.setUnsat();
					finHeaps.add(finHeap);
				}
			}
			
			for (WrappedHeap finHeap : finHeaps) {
				if (System.currentTimeMillis() - startTime > 1000 * Options.I().getTimeBudget()) {
					continue; // timeout
				}
				BackwardRecord br = finHeap.getBackwardRecords().stream()
						.filter(r -> r.oriHeap == curHeap).findAny().get();
				assert(finHeap.isUnsat());
				if (br.pathCond != null && !this.solver.checkSatIncr(br.pathCond))
					continue;
				finHeap.setActive();
				if (this.addNewHeap(finHeap)) {
					newHeaps.add(finHeap);
				}
			}
		}
		return newHeaps;
	}
	
	public List<WrappedHeap> buildGraph(SymbolicHeap symHeap, int maxSeqLen) {
		long startTime = System.currentTimeMillis();
		Logger.info("[GraphBuilderWithoutMerging.buildGraph] maxLength = " + maxSeqLen);
		WrappedHeap initHeap = new WrappedHeap(symHeap);
		this.addNewHeap(initHeap);
		List<WrappedHeap> oldHeaps = new ArrayList<>();
		oldHeaps.add(initHeap);
		for (int L = 0; L < maxSeqLen; ++L) {
			oldHeaps = this.expandGraph(oldHeaps, L, startTime);
		}
		for (WrappedHeap heap : oldHeaps) {
			Logger.info("in-queue heap " + heap.__debugGetName() + " of length " + maxSeqLen);
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
