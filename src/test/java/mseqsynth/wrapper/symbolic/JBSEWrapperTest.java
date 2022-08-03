package mseqsynth.wrapper.symbolic;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import dsclasses.ListNode;
import mseqsynth.algo.MethodInvoke;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntVar;

public class JBSEWrapperTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		SymbolicExecutor executor = new SymbolicExecutorWithCachedJBSE();
		
		SymbolicHeap emp = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
		ObjectH elem$1 = new ObjectH(new IntVar());
		MethodInvoke invoke$new = new MethodInvoke(ListNode.mNew, Arrays.asList(elem$1));
		Collection<PathDescriptor> pds = executor.executeMethod(emp, invoke$new);
		assertEquals(1, pds.size());
		PathDescriptor pd = ImmutableList.copyOf(pds).get(0);
		
		SymbolicHeap h1 = pd.finHeap;
		ObjectH elem$2 = new ObjectH(new IntVar());
		MethodInvoke invoke$addAfter = new MethodInvoke(ListNode.mAddAfter,
				Arrays.asList(pd.retVal, elem$2));
		pds = executor.executeMethod(h1, invoke$addAfter);
		assertEquals(1, pds.size());
		pd = ImmutableList.copyOf(pds).get(0);
	}

}
