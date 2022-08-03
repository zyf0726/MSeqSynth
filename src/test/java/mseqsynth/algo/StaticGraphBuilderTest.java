package mseqsynth.algo;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import dsclasses.ListNode;
import dsclasses.ManualExecutor;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ApplyExpr;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTOperator;
import mseqsynth.wrapper.symbolic.Specification;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;
import mseqsynth.wrapper.symbolic.SymbolicExecutorWithCachedJBSE;

public class StaticGraphBuilderTest {

	private void buildGraphForListNode(SymbolicExecutor executor,
			String logFileName, String objFileName) throws Exception {
		StaticGraphBuilder gb = new StaticGraphBuilder(
				executor,
				Arrays.asList(
						ListNode.mNew, ListNode.mGetNext,
						ListNode.mSetElem, ListNode.mGetElem,
						ListNode.mAddBefore, ListNode.mAddAfter
				)
		);
		SymbolicHeap initHeap = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
		List<WrappedHeap> genHeaps = gb.buildGraph(initHeap, false);
		PrintStream ps = new PrintStream(logFileName);
		StaticGraphBuilder.__debugPrintOut(genHeaps, executor, ps);
		ps.close();
		WrappedHeap.exportHeapsTo(genHeaps, objFileName);
	}
	
	private List<WrappedHeap> buildWithManual() throws Exception {
		final String logFileNamePre = "build/testListNode-Manual-pre.log";
		final String logFileNamePost = "build/testListNode-Manual-post.log";
		final String objFileName = "build/testListNode-Manual.javaobj";
		SymbolicExecutor executor = ManualExecutor.I();
		buildGraphForListNode(executor, logFileNamePre, objFileName);
		List<WrappedHeap> genHeaps = WrappedHeap.importHeapsFrom(objFileName);
		
		PrintStream psPost = new PrintStream(logFileNamePost);
		StaticGraphBuilder.__debugPrintOut(genHeaps, executor, psPost);
		psPost.close();
		
		return genHeaps;
	}
	
	private List<WrappedHeap> buildWithJBSE() throws Exception {
		final String logFileNamePre = "build/testListNode-CachedJBSE-pre.log";
		final String logFileNamePost = "build/testListNode-CachedJBSE-post.log";
		final String objFileName = "build/testListNode-CachedJBSE.javaobj";
		SymbolicExecutor executor = new SymbolicExecutorWithCachedJBSE();
		buildGraphForListNode(executor, logFileNamePre, objFileName);
		List<WrappedHeap> genHeaps = WrappedHeap.importHeapsFrom(objFileName);
		
		PrintStream psPost = new PrintStream(logFileNamePost);
		StaticGraphBuilder.__debugPrintOut(genHeaps, executor, psPost);
		psPost.close();
		
		return genHeaps;
	}
	
	private void makeTest(List<WrappedHeap> genHeaps, PrintStream ps) throws Exception {
		IntVar y = new IntVar();
		IntVar x1 = new IntVar(), x2 = new IntVar();
		IntVar x3 = new IntVar(), x4 = new IntVar();
		ObjectH o1 = new ObjectH(ListNode.classH,
				ImmutableMap.of(ListNode.fElem, new ObjectH(x1), ListNode.fNext, ObjectH.NULL));
		ObjectH o2 = new ObjectH(ListNode.classH,
				ImmutableMap.of(ListNode.fElem, new ObjectH(x2), ListNode.fNext, o1));
		ObjectH o3 = new ObjectH(ListNode.classH,
				ImmutableMap.of(ListNode.fElem, new ObjectH(x3), ListNode.fNext, o2));
		ObjectH o4 = new ObjectH(ListNode.classH,
				ImmutableMap.of(ListNode.fElem, new ObjectH(x4), ListNode.fNext, o2));
		Specification spec = new Specification();
		spec.expcHeap = new SymbolicHeapAsDigraph(
				Arrays.asList(o3, o4, o1, ObjectH.NULL), null);
		spec.condition = new ApplyExpr(SMTOperator.AND,
				new ApplyExpr(SMTOperator.BIN_EQ, x1, y),
				new ApplyExpr(SMTOperator.BIN_NE, x3, y),
				new ApplyExpr(SMTOperator.BIN_NE, x4, y));
		
		TestGenerator testgen = new TestGenerator(genHeaps);
		List<Statement> stmts = testgen.generateTestWithSpec(spec, o3, o4, o1, new ObjectH(y));
		Statement.printStatements(stmts, ps);
		ps.flush();
	}
	
	@Test
	public void testManual() throws Exception {
		List<WrappedHeap> genHeaps = buildWithManual();
		makeTest(genHeaps, new PrintStream("build/ManualTestgen.txt"));
	}
	
	@Test
	public void testCachedJBSE() throws Exception {
		List<WrappedHeap> genHeaps = buildWithJBSE();
		makeTest(genHeaps, new PrintStream("build/CachedJBSETestgen.txt"));
	}

}
