package mseqsynth.algo;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import mseqsynth.algo.WrappedHeap.BackwardRecord;
import mseqsynth.algo.WrappedHeap.MatchResult;
import mseqsynth.common.exceptions.UnexpectedInternalException;
import mseqsynth.common.settings.Options;
import mseqsynth.heap.ObjectH;
import mseqsynth.smtlib.Constant;
import mseqsynth.smtlib.Variable;
import mseqsynth.wrapper.symbolic.Specification;

public class TestGenerator {
	
//	private SMTSolver smtSolver;
	private List<WrappedHeap> heaps;
	
	public TestGenerator(Collection<WrappedHeap> heaps) {
//		this.smtSolver = Options.I().getSMTSolver();
		this.heaps = ImmutableList.copyOf(heaps);
	}
	
	private MatchResult __matchResult;
	private WrappedHeap __finalHeap;
	private boolean tryMatchSpec(Specification spec) {
		this.__matchResult = null;
		this.__finalHeap = null;
		List<WrappedHeap> activeHeaps = this.heaps.stream()
				.filter(o -> o.isActive()).collect(Collectors.toList());
		
		if (Options.I().getMaxNumThreads() <= 0) {
			for (WrappedHeap heap : activeHeaps) {
				MatchResult ret = heap.matchSpecification(spec);
				if (ret != null) {
					this.__matchResult = ret;
					this.__finalHeap = heap;
					return true;
				}
			}
			return false;
		}
		
		final CountDownLatch cdlAll = new CountDownLatch(activeHeaps.size());
		final CountDownLatch cdlOne = new CountDownLatch(1);
		Thread thIfNotFound = new Thread(() -> {
			try {
				cdlAll.await();		// all threads terminated
				cdlOne.countDown(); // specification not matched, wake up the main thread
			} catch (InterruptedException e) {
				// interrupted by the main thread - specification matched
				// stop waiting for the remaining threads
			}
		});
		thIfNotFound.start();
		
		ExecutorService es = Executors.newFixedThreadPool(Options.I().getMaxNumThreads());
		for (WrappedHeap heap : activeHeaps) {
			es.submit(() -> {
				MatchResult ret = heap.matchSpecification(spec);
				if (ret != null) {
					synchronized (this) {
						this.__matchResult = ret;
						this.__finalHeap = heap;
					}
					cdlOne.countDown(); // specification matched, wake up the main thread
				} else {
					cdlAll.countDown(); // one thread terminated
				}
			});
		}
		
		try {
			cdlOne.await();
		} catch (InterruptedException e) {
			throw new UnexpectedInternalException("main thread interrupted unexpectedly");
		}
		thIfNotFound.interrupt();
		es.shutdownNow();
		
		synchronized (this) {
			return this.__matchResult != null;
		}
	}
	
	public List<Statement> generateTestWithSpec(Specification spec, ObjectH... arguments) {
		return this.generateTestWithSpec(spec, null, null, arguments);
	}
	
	public List<Statement> generateTestWithSpec(Specification spec,
			Map<ObjectH, ObjectH> objSrc, Map<Variable, Constant> vModel,
			ObjectH... arguments) {
		for (ObjectH arg : arguments) {
			Preconditions.checkArgument(!arg.isHeapObject() ||
					spec.expcHeap.getAccessibleObjects().contains(arg),
					"object in arguments not accessible in spec.expcHeap");
		}
		
		if (!tryMatchSpec(spec)) return null;
		MatchResult matchRet = null;
		WrappedHeap finHeap = null;
		synchronized (this) {
			matchRet = this.__matchResult;
			finHeap = this.__finalHeap;
		}
		
		if (objSrc == null) {
			objSrc = new HashMap<>();
		} else {
			objSrc.clear();
		}
		for (ObjectH o : arguments) {
			if (o.isHeapObject())
				objSrc.put(o, matchRet.objSrcMap.get(o));
		}
		
		if (vModel == null) {
			vModel = new HashMap<>();
		} else {
			vModel.clear();
		}
		vModel.putAll(matchRet.model);
		
		Statement testStmt = new Statement(arguments);
		testStmt.updateVars(vModel);
		List<Statement> stmts = Lists.newArrayList(testStmt);
		Set<ObjectH> objRets = Sets.newHashSet(ObjectH.NULL);
		
		while (true) {
			List<BackwardRecord> rcdBackwards = finHeap.getBackwardRecords();
			if (rcdBackwards.isEmpty()) break;
			
			List<Map<Variable, Variable>> renameMapList = finHeap.getRenameMapList();
			int version = -1;
			for (Map<Variable, Variable> renameMap : renameMapList) {
				if (vModel.keySet().containsAll(renameMap.values())) {
					version = renameMapList.indexOf(renameMap);
					break;
				}
			}
			Map<Variable, Variable> renameMap = renameMapList.get(version);
			for (Variable v : renameMap.keySet()) {
				vModel.put(v, vModel.get(renameMap.get(v)));
			}
			for (Variable v : renameMap.values()) {
				vModel.remove(v);
			}
			Variable indicator = finHeap.getIndicatorVariables().get(version);
			assert(vModel.keySet().contains(indicator));
			int index = Integer.valueOf(vModel.get(indicator).toSMTString());
			BackwardRecord br = rcdBackwards.get(index);
			if (br.retVal != null && br.retVal.isNonNullObject()) {
				objRets.add(br.retVal);
				objSrc.put(br.retVal, br.retVal);
			}
			for (Entry<ObjectH, ObjectH> entry : objSrc.entrySet()) {
				ObjectH interSrc = entry.getValue();
				if (!objRets.contains(interSrc)) {
					assert(br.objSrcMap.containsKey(interSrc));
					entry.setValue(br.objSrcMap.get(interSrc));
				}
			}
			if (br.mInvoke != null) {
				for (ObjectH arg : br.mInvoke.getInvokeArguments()) {
					if (arg.isHeapObject())
						objSrc.put(arg, arg);
				}
				Statement stmt = new Statement(br.mInvoke, br.retVal);
				stmt.updateVars(vModel);
				stmts.add(stmt);
			}
			finHeap = br.oriHeap;
			
			/*
			List<BoolVar> guardVars = new ArrayList<>();
			List<SMTExpression> andClauses = new ArrayList<>();
			for (BackwardRecord br : rcdBackwards) {
				BoolVar guardVar = new BoolVar();
				guardVars.add(guardVar);
				SMTExpression guardCond = br.guardCondList.get(version).getBody();
				andClauses.add(new ApplyExpr(SMTOperator.BIN_EQ, guardVar, guardCond));
				assert(vModel.keySet().containsAll(guardCond.getFreeVariables()));
			}
			andClauses.add(new ApplyExpr(SMTOperator.OR, guardVars)); // only for assertion
			SMTExpression constraint = new ApplyExpr(SMTOperator.AND, andClauses);
			boolean isSat = this.smtSolver.checkSat(constraint, vModel);
			assert(isSat);
			
			for (int i = 0; i < rcdBackwards.size(); ++i) {
				BoolVar guardVar = guardVars.get(i);
				if (vModel.get(guardVar).toSMTString().equals("true")) {
					BackwardRecord br = rcdBackwards.get(i);
					if (br.retVal != null && br.retVal.isNonNullObject()) {
						objRets.add(br.retVal);
						objSrc.put(br.retVal, br.retVal);
					}
					for (Entry<ObjectH, ObjectH> entry : objSrc.entrySet()) {
						ObjectH interSrc = entry.getValue();
						if (!objRets.contains(interSrc)) {
							assert(br.objSrcMap.containsKey(interSrc));
							entry.setValue(br.objSrcMap.get(interSrc));
						}
					}
					if (br.mInvoke != null) {
						for (ObjectH arg : br.mInvoke.getInvokeArguments()) {
							if (arg.isHeapObject())
								objSrc.put(arg, arg);
						}
						Statement stmt = new Statement(br.mInvoke, br.retVal);
						stmt.updateVars(vModel);
						stmts.add(stmt);
					}
					finHeap = br.oriHeap;
					break;
				}
			}
			*/
		}
		
		Collections.reverse(stmts);
		for (Statement stmt : stmts) {
			stmt.updateObjs(objSrc);
		}		
		return stmts;
	}
	
	public List<Statement> generateTestForMethod(Method testMethod) {
		// TODO
		return null;
	}

}
