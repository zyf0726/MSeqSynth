package mseqsynth.algo;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import mseqsynth.heap.ClassH;
import mseqsynth.heap.FieldH;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ApplyExpr;
import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntConst;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTOperator;
import mseqsynth.util.Bijection;
import mseqsynth.wrapper.symbolic.PathDescriptor;

public class WrappedHeapTest1 {
	
	@SuppressWarnings("unused")
	static class MyInteger {
		private int value;
		public MyInteger makeAdd(int delta) {
			this.value += delta; return this;
		}
		public void incValue(int dummy) {
			this.value += 1;
		}
		public static MyInteger zero() {
			MyInteger o = new MyInteger(); o.value = 0; return o;
		}
		public static MyInteger one(boolean unused) {
			MyInteger o = new MyInteger(); o.value = 1; return o;
		}
	}
	
	@SuppressWarnings("unused")
	static class MyNode {
		private MyNode next;
		public static MyNode __new__() {
			return new MyNode();
		}
	}
	
	private static ClassH cInt, cNode;
	private static FieldH fValue, fNext;
	private static Method mAdd, mInc, mZero, mOne, mNew;
	private static SymbolicHeap emp;
	private static ObjectH oNull;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		cInt = ClassH.of(MyInteger.class);
	 	fValue = FieldH.of(MyInteger.class.getDeclaredField("value"));
	 	mAdd = MyInteger.class.getDeclaredMethod("makeAdd", int.class);
	 	mInc = MyInteger.class.getDeclaredMethod("incValue", int.class);
	 	mZero = MyInteger.class.getDeclaredMethod("zero");
	 	mOne = MyInteger.class.getDeclaredMethod("one", boolean.class);
	 	cNode = ClassH.of(MyNode.class);
	 	fNext = FieldH.of(MyNode.class.getDeclaredField("next"));
	 	mNew = MyNode.class.getDeclaredMethod("__new__");
	 	emp = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
	 	oNull = ObjectH.NULL;
	}

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void test1() {
		System.err.println(":: Test 1");
		IntVar dummy = new IntVar(), delta = new IntVar();
		IntVar x = new IntVar(), y = new IntVar();
		IntVar u = new IntVar(), v = new IntVar();
		ObjectH o1 = new ObjectH(cInt, ImmutableMap.of(fValue, new ObjectH(x)));
		ObjectH o2 = new ObjectH(cInt, ImmutableMap.of(fValue, new ObjectH(y)));
		ObjectH r1 = new ObjectH(cInt, ImmutableMap.of(fValue, new ObjectH(u)));
		ObjectH r2 = new ObjectH(cInt, ImmutableMap.of(fValue, new ObjectH(v)));
		
		SymbolicHeap heap1 = new SymbolicHeapAsDigraph(
				Arrays.asList(o1, o2, oNull),
				new ExistExpr(Arrays.asList(dummy), new ApplyExpr(SMTOperator.BIN_EQ, x, y))
		);
		WrappedHeap H1 = new WrappedHeap(heap1);
		
		SymbolicHeap heap2 = new SymbolicHeapAsDigraph(
				Arrays.asList(r1, r2, oNull),
				ExistExpr.ALWAYS_FALSE
		);
		MethodInvoke mInvoke = new MethodInvoke(mAdd, Arrays.asList(o1, new ObjectH(delta)));
		PathDescriptor pd = new PathDescriptor();
		pd.pathCond = new ApplyExpr(SMTOperator.BIN_NE, x, new IntConst(0));
		pd.retVal = r1;
		pd.finHeap = heap2;
		pd.objSrcMap = ImmutableMap.of(r1, o1, r2, o2);
		pd.varExprMap = ImmutableMap.of(u, new ApplyExpr(SMTOperator.ADD, x, delta), v, y);
		WrappedHeap H2 = new WrappedHeap(H1, mInvoke, pd);
		
		H1.__debugPrintOut(System.out);
		H2.__debugPrintOut(System.out);
		
		H2.recomputeConstraint();
		H2.__debugPrintOut(System.out);
	}
	
	@Test
	public void test2() {
		System.err.println(":: Test 2");
	 	IntVar dummy = new IntVar(); BoolVar unused = new BoolVar();
	 	IntVar x1 = new IntVar(), x2 = new IntVar(), x3 = new IntVar();
	 	ObjectH o1 = new ObjectH(cInt, ImmutableMap.of(fValue, new ObjectH(x1)));
	 	ObjectH o2 = new ObjectH(cInt, ImmutableMap.of(fValue, new ObjectH(x2)));
	 	ObjectH o3 = new ObjectH(cInt, ImmutableMap.of(fValue, new ObjectH(x3)));
	 	
	 	SymbolicHeap heap1 = new SymbolicHeapAsDigraph(
	 			Arrays.asList(o1, oNull), ExistExpr.ALWAYS_FALSE);
	 	SymbolicHeap heap2 = new SymbolicHeapAsDigraph(
	 			Arrays.asList(o2, oNull), ExistExpr.ALWAYS_FALSE);
	 	SymbolicHeap heap3 = new SymbolicHeapAsDigraph(
	 			Arrays.asList(o3, oNull), ExistExpr.ALWAYS_FALSE);
	 	
	 	// H0 (empty heap) 
	 	WrappedHeap H0 = new WrappedHeap(emp);
	 	
	 	// H0 => H1, invoke zero()
	 	MethodInvoke mInvoke1 = new MethodInvoke(mZero, null);
	 	PathDescriptor pd1 = new PathDescriptor();
	 	pd1.pathCond = null;
	 	pd1.retVal = o1;
	 	pd1.finHeap = heap1;
	 	pd1.objSrcMap = ImmutableMap.of();
	 	pd1.varExprMap = ImmutableMap.of(x1, new IntConst(0));
	 	WrappedHeap H1 = new WrappedHeap(H0, mInvoke1, pd1);
	 	
	 	// H0 => H2, invoke one(unused)
	 	MethodInvoke mInvoke2 = new MethodInvoke(mOne, Arrays.asList(new ObjectH(unused)));
	 	PathDescriptor pd2 = new PathDescriptor();
	 	pd2.pathCond = null;
	 	pd2.retVal = o2;
	 	pd2.finHeap = heap2;
	 	pd2.objSrcMap = ImmutableMap.of();
	 	pd2.varExprMap = ImmutableMap.of(x2, new IntConst(1));
	 	WrappedHeap H2 = new WrappedHeap(H0, mInvoke2, pd2);
	 	
	 	// H1 => H3, invoke o1.incValue(dummy)
	 	MethodInvoke mInvoke3 = new MethodInvoke(mInc, Arrays.asList(o1, new ObjectH(dummy)));
	 	PathDescriptor pd3 = new PathDescriptor();
	 	pd3.pathCond = new ApplyExpr(SMTOperator.BIN_NE, x1, new IntConst(0));
	 	pd3.retVal = null;
	 	pd3.finHeap = heap3;
	 	pd3.objSrcMap = ImmutableMap.of(o3, o1);
	 	pd3.varExprMap = ImmutableMap.of(x3, new ApplyExpr(SMTOperator.ADD, x1, new IntConst(1)));
	 	WrappedHeap H3 = new WrappedHeap(H1, mInvoke3, pd3);
	 	
	 	// H2 ---> H1, isomorphic
	 	H1.subsumeHeap(H2, new Bijection<ObjectH, ObjectH>() {
	 		{ putUV(o2, o1); }
	 		{ putUV(o2.getFieldValue(fValue), o1.getFieldValue(fValue)); }
	 		{ putUV(oNull, oNull); }
	 	});
	 	
	 	// H3 ---> H1, isomorphic
	 	H1.subsumeHeap(H3, new Bijection<ObjectH, ObjectH>() {
	 		{ putUV(o3, o1); }
	 		{ putUV(o3.getFieldValue(fValue), o1.getFieldValue(fValue)); }
	 		{ putUV(oNull, oNull); }
	 	});
	 	
	 	H0.__debugPrintOut(System.out);
	 	H1.__debugPrintOut(System.out);
	 	H2.__debugPrintOut(System.out);
	 	H3.__debugPrintOut(System.out);
	 	
	 	System.err.println(":: recompute constraint of (H0, H2)");
	 	H0.recomputeConstraint();
	 	H2.recomputeConstraint();
	 	H0.__debugPrintOut(System.out);
	 	H2.__debugPrintOut(System.out);
	 	
	 	System.err.println(":: recompute constraint of (H1, H3)");
	 	H1.recomputeConstraint();
	 	H3.recomputeConstraint();
	 	H1.__debugPrintOut(System.out);
	 	H3.__debugPrintOut(System.out);
	 	
	 	System.err.println(":: recompute constraint of (H1, H3, H1, H3)");
	 	H1.recomputeConstraint();
	 	H3.recomputeConstraint();
	 	H1.recomputeConstraint();
	 	H3.recomputeConstraint();
	 	H1.__debugPrintOut(System.out);
	 	H3.__debugPrintOut(System.out);
	}
	
	@Test
	public void test3() {
		System.err.println(":: Test 3");
		ObjectH o1 = new ObjectH(cNode, ImmutableMap.of(fNext, oNull));
		SymbolicHeap heap1 = new SymbolicHeapAsDigraph(
				Arrays.asList(o1, oNull), ExistExpr.ALWAYS_FALSE);
		MethodInvoke mInvoke = new MethodInvoke(mNew, null);
		PathDescriptor pd = new PathDescriptor();
		pd.pathCond = null; 
		pd.retVal = o1;
		pd.finHeap = heap1;
		pd.objSrcMap = ImmutableMap.of();
		pd.varExprMap = ImmutableMap.of();
		
		WrappedHeap H0 = new WrappedHeap(emp);
		WrappedHeap H1 = new WrappedHeap(H0, mInvoke, pd);
		
		H0.__debugPrintOut(System.out);
		H1.__debugPrintOut(System.out);
		
		H1.recomputeConstraint();
		H1.__debugPrintOut(System.out);		
	}

}
