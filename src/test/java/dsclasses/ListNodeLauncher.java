package dsclasses;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mseqsynth.algo.Statement;
import mseqsynth.algo.StaticGraphBuilder;
import mseqsynth.algo.TestGenerator;
import mseqsynth.algo.WrappedHeap;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.SMTSort;
import mseqsynth.wrapper.symbolic.SpecFactory;
import mseqsynth.wrapper.symbolic.Specification;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;
import mseqsynth.wrapper.symbolic.SymbolicExecutorWithCachedJBSE;

public class ListNodeLauncher {
	
	private static TestGenerator testGenerator;
	
	private static void buildGraph(Collection<Method> methods) {
		long start = System.currentTimeMillis();
		SymbolicExecutor executor = new SymbolicExecutorWithCachedJBSE();
		StaticGraphBuilder gb = new StaticGraphBuilder(executor, methods);
		gb.setHeapScope(ListNode.class, 5);
		SymbolicHeap initHeap = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
		List<WrappedHeap> heaps = gb.buildGraph(initHeap, false);
		testGenerator = new TestGenerator(heaps);
		System.out.println("number of all heaps = " + heaps.size());
		System.out.println("number of symbolic execution = " + executor.getExecutionCount());
		long end = System.currentTimeMillis();
		System.out.println(">> buildGraph: " + (end - start) + "ms\n");
	}
	
	private static void genTest1() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH p = specFty.mkRefDecl(ListNode.class, "p");
		specFty.addRefSpec("p", "next", "q", "elem", "pv");
		specFty.addRefSpec("q", "next", "r", "elem", "qv");
		specFty.addRefSpec("r", "next", "s", "elem", "rv");
		specFty.addRefSpec("s", "next", "null", "elem", "sv");
		specFty.setAccessible("p");
		specFty.addVarSpec("(not (= pv 0))");
		specFty.addVarSpec("(distinct qv 0)");
		specFty.addVarSpec("(= rv (+ pv qv))");
		specFty.addVarSpec("(= sv (+ qv rv))");
		ObjectH tv = specFty.mkVarDecl(SMTSort.INT, "tv");
		specFty.addVarSpec("(= tv (+ rv sv))");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, p, tv);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest1: " + (end - start) + "ms\n");
	}
	
	private static void genTest2() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH r = specFty.mkRefDecl(ListNode.class, "r");
		ObjectH s = specFty.mkRefDecl(ListNode.class, "s");
		ObjectH t = specFty.mkRefDecl(ListNode.class, "t");
		ObjectH boolArg = specFty.mkVarDecl(SMTSort.BOOL, "boolArg");
		specFty.addRefSpec("r", "next", "p", "elem", "r.elem");
		specFty.addRefSpec("s", "next", "p", "elem", "s.elem");
		specFty.addRefSpec("t", "next", "q", "elem", "t.elem");
		specFty.addRefSpec("p", "next", "q", "elem", "p.elem");
		specFty.addRefSpec("q", "next", "null", "elem", "q.elem");
		specFty.setAccessible("r", "s", "t");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, r, s, t, boolArg);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest2: " + (end - start) + "ms\n");
	}
	
	public static void main(String[] args) {
		List<Method> methods = new ArrayList<>();
		methods.add(ListNode.mNew);
		methods.add(ListNode.mGetNext);
		methods.add(ListNode.mSetElem);
		methods.add(ListNode.mGetElem);
		methods.add(ListNode.mAddAfter);
		methods.add(ListNode.mAddBefore);
		buildGraph(methods);
		genTest1();
		genTest2();
	}

}
