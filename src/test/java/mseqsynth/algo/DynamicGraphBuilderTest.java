package mseqsynth.algo;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import dsclasses.ListNode;
import dsclasses.ManualExecutor;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;
import mseqsynth.wrapper.symbolic.SymbolicExecutorWithCachedJBSE;

public class DynamicGraphBuilderTest {

	private void makeTestListNode(SymbolicExecutor executor, PrintStream ps) throws Exception {
		DynamicGraphBuilder gb = new DynamicGraphBuilder(
				executor,
				Arrays.asList(
						ListNode.mNew, ListNode.mGetNext,
						ListNode.mSetElem, ListNode.mGetElem,
						ListNode.mAddBefore, ListNode.mAddAfter
				)
		);
		SymbolicHeap initHeap = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
		List<WrappedHeap> genHeaps = gb.buildGraph(initHeap, 6);
		DynamicGraphBuilder.__debugPrintOut(genHeaps, executor, ps);
	}
	
	@Test
	public void testListNodeManual() throws Exception {
		PrintStream ps = new PrintStream("build/testListNode-Manual-Dynamic.log");
		makeTestListNode(ManualExecutor.I(), ps);
	}
	
	@Test
	public void testListNodeCachedJBSE() throws Exception {
		PrintStream ps = new PrintStream("build/testListNode-CachedJBSE-Dynamic.log");
		makeTestListNode(new SymbolicExecutorWithCachedJBSE(), ps);
	}

}
