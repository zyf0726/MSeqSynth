package mseqsynth.algo;

import static mseqsynth.smtlib.SMTOperator.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import mseqsynth.algo.WrappedHeap.MatchResult;
import mseqsynth.heap.ClassH;
import mseqsynth.heap.FieldH;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ApplyExpr;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntConst;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTExpression;
import mseqsynth.smtlib.SMTOperator;
import mseqsynth.wrapper.symbolic.Specification;

public class WrappedHeapTest2 {
	
	@SuppressWarnings("unused")
	static class Node {
		private int value;
		private Node next;
	}
	
	@SuppressWarnings("unused")
	static class Entry {
		private Object val;
		private Entry nxt;
	}
	
	private static ClassH cNode, cEntry, cObj;
	private static FieldH fValue, fNext;
	private static FieldH fVal, fNxt;
	private static ObjectH oNull;
	
	static final int N = 20;
	private static IntVar[] iv;
	private static ObjectH[] ov;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		cNode = ClassH.of(Node.class);
		cEntry = ClassH.of(Entry.class);
		cObj = ClassH.of(Object.class);
		fValue = FieldH.of(Node.class.getDeclaredField("value"));
		fNext = FieldH.of(Node.class.getDeclaredField("next"));
		fVal = FieldH.of(Entry.class.getDeclaredField("val"));
		fNxt = FieldH.of(Entry.class.getDeclaredField("nxt"));
		oNull = ObjectH.NULL;
		iv = new IntVar[N];
		ov = new ObjectH[N];
		for (int i = 0; i < N; ++i) {
			iv[i] = new IntVar();
			ov[i] = new ObjectH(iv[i]);
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test1() {
		ObjectH o1 = new ObjectH(cNode, ImmutableMap.of(fValue, ov[1], fNext, oNull));
		ObjectH o2 = new ObjectH(cNode, ImmutableMap.of(fValue, ov[2], fNext, o1));
		ObjectH o3 = new ObjectH(cNode, ImmutableMap.of(fValue, ov[3], fNext, o2));
		ObjectH o4 = new ObjectH(cNode, ImmutableMap.of(fValue, ov[4], fNext, o2));
		SMTExpression expr1 = new ApplyExpr(SMTOperator.AND,
				// o4.value = 2x + 1
				new ApplyExpr(BIN_EQ, iv[4],
						new ApplyExpr(ADD, new IntConst(1),
								new ApplyExpr(MUL, iv[0], new IntConst(2)))),
				// o3.value = 3x + 1
				new ApplyExpr(BIN_EQ, iv[3],
						new ApplyExpr(ADD, new IntConst(1),
								new ApplyExpr(MUL, iv[0], new IntConst(3)))),
				// o2.value = x - 1
				new ApplyExpr(BIN_EQ, iv[2],
						new ApplyExpr(SUB, iv[0], new IntConst(1))),
				// o1.value = x + 1
				new ApplyExpr(BIN_EQ, iv[1],
						new ApplyExpr(ADD, iv[0], new IntConst(1)))
		);
		SymbolicHeap supSymHeap = new SymbolicHeapAsDigraph(
				Arrays.asList(o2, o3, o4, oNull),
				new ExistExpr(Arrays.asList(iv[0]), expr1)
		);
		WrappedHeap supHeap = new WrappedHeap(supSymHeap); 
		
		ObjectH p1 = new ObjectH(cNode, ImmutableMap.of(fValue, ov[5], fNext, oNull));
		ObjectH p2 = new ObjectH(cNode, ImmutableMap.of(fValue, ov[6], fNext, p1));
		ObjectH p3 = new ObjectH(cNode, ImmutableMap.of(fValue, ov[7], fNext, p2));
		
		Specification spec1 = new Specification();
		spec1.expcHeap = new SymbolicHeapAsDigraph(Arrays.asList(p2, oNull), null);
		// p1.value + p2.value = 4
		spec1.condition = new ApplyExpr(BIN_EQ, new IntConst(4),
				new ApplyExpr(ADD, iv[5], iv[6]));
		MatchResult ret1 = supHeap.matchSpecification(spec1);
		assertEquals(o2, ret1.objSrcMap.get(p2));
		assertNull(ret1.objSrcMap.get(p1));
		assertEquals("2", ret1.model.get(iv[0]).toSMTString()); // x = 2
		assertEquals("3", ret1.model.get(iv[1]).toSMTString()); // o1.value = 3
		assertEquals("1", ret1.model.get(iv[2]).toSMTString()); // o2.value = 1
		assertEquals("7", ret1.model.get(iv[3]).toSMTString()); // o3.value = 7
		assertEquals("5", ret1.model.get(iv[4]).toSMTString()); // o4.value = 5
		
		Specification spec2 = new Specification();
		spec2.expcHeap = new SymbolicHeapAsDigraph(Arrays.asList(p2, oNull), null);
		// p2.value - p1.value = 2
		spec2.condition = new ApplyExpr(BIN_EQ, new IntConst(2),
				new ApplyExpr(SUB, iv[6], iv[5]));
		assertNull(supHeap.matchSpecification(spec2));
		
		Specification spec3 = new Specification();
		spec3.expcHeap = new SymbolicHeapAsDigraph(Arrays.asList(p1, p2, oNull), null);
		// true
		spec3.condition = null;
		assertNull(supHeap.matchSpecification(spec3));
		
		Specification spec4 = new Specification();
		spec4.expcHeap = new SymbolicHeapAsDigraph(Arrays.asList(p3, oNull), null);
		// p3.value = 4y + 2
		spec4.condition = new ApplyExpr(BIN_EQ, iv[7],
				new ApplyExpr(ADD, new IntConst(2),
						new ApplyExpr(MUL, iv[8], new IntConst(4))));
		MatchResult ret2 = supHeap.matchSpecification(spec4);
		assertEquals(o3, ret2.objSrcMap.get(p3));
		assertEquals(3 * Integer.parseInt(ret2.model.get(iv[0]).toSMTString()) + 1,
				4 * Integer.parseInt(ret2.model.get(iv[8]).toSMTString()) + 2);
		
		Specification spec5 = new Specification();
		spec5.expcHeap = new SymbolicHeapAsDigraph(Arrays.asList(p2, p3, oNull), null);
		// p3.value = 6y + 3
		spec5.condition = new ApplyExpr(BIN_EQ, iv[7],
				new ApplyExpr(ADD, new IntConst(3),
						new ApplyExpr(MUL, iv[8], new IntConst(6))));
		MatchResult ret3 = supHeap.matchSpecification(spec5);
		assertEquals(o4, ret3.objSrcMap.get(p3));
		assertEquals(2 * Integer.parseInt(ret3.model.get(iv[0]).toSMTString()) + 1,
				6 * Integer.parseInt(ret3.model.get(iv[8]).toSMTString()) + 3);
		
		Specification spec6 = new Specification();
		spec6.expcHeap = new SymbolicHeapAsDigraph(Arrays.asList(p2, p3, oNull), null);
		// p3.value = 6y + 2
		spec6.condition = new ApplyExpr(BIN_EQ, iv[7],
				new ApplyExpr(ADD, new IntConst(2),
						new ApplyExpr(MUL, iv[8], new IntConst(6))));
		assertNull(supHeap.matchSpecification(spec6));
	}
	
	@Test
	public void test2() {
		ObjectH u0 = new ObjectH(cObj, Collections.emptyMap());
		ObjectH u1 = new ObjectH(cObj, Collections.emptyMap());
		ObjectH u2 = new ObjectH(cObj, Collections.emptyMap());
		ObjectH u3 = new ObjectH(cObj, Collections.emptyMap());
		ObjectH o3 = new ObjectH(cEntry, ImmutableMap.of(fNxt, oNull, fVal, u3));
		ObjectH o2 = new ObjectH(cEntry, ImmutableMap.of(fNxt, o3, fVal, u2));
		ObjectH o1 = new ObjectH(cEntry, ImmutableMap.of(fNxt, o2, fVal, u1));
		SMTExpression e0 = new ApplyExpr(BIN_EQ, u0.getVariable(), new IntConst(0));
		SMTExpression e1 = new ApplyExpr(BIN_EQ, u1.getVariable(), new IntConst(1));
		SMTExpression e2 = new ApplyExpr(BIN_EQ, u2.getVariable(), new IntConst(2));
		SMTExpression e3 = new ApplyExpr(BIN_EQ, u3.getVariable(), new IntConst(0));
		SymbolicHeap supSymHeap1 = new SymbolicHeapAsDigraph(
				Arrays.asList(o1, o2, oNull, u0, u2),
				new ExistExpr(Arrays.asList(iv[0]), new ApplyExpr(AND, e0, e1, e2, e3))
		);
		SymbolicHeap supSymHeap2 = new SymbolicHeapAsDigraph(
				Arrays.asList(o1, oNull, u0, u1, u2, u3),
				new ExistExpr(Arrays.asList(iv[0]), new ApplyExpr(AND, e0, e1, e2, e3))
		);
		WrappedHeap supHeap1 = new WrappedHeap(supSymHeap1);
		WrappedHeap supHeap2 = new WrappedHeap(supSymHeap2);
		supHeap1.__debugPrintOut(System.out);
		supHeap2.__debugPrintOut(System.out);
		
		ObjectH v0 = new ObjectH(cObj, Collections.emptyMap());
		ObjectH v1 = new ObjectH(cObj, Collections.emptyMap());
		ObjectH v2 = new ObjectH(cObj, Collections.emptyMap());
		ObjectH v3 = new ObjectH(cObj, Collections.emptyMap());
		ObjectH p2 = new ObjectH(cEntry, ImmutableMap.of(fVal, v2));
		ObjectH p1 = new ObjectH(cEntry, ImmutableMap.of(fNxt, p2, fVal, v1));
		SMTExpression f0 = new ApplyExpr(BIN_EQ, v0.getVariable(), new IntConst(0));
		SMTExpression f3 = new ApplyExpr(DISTINCT, v3.getVariable(), new IntConst(0));
		Specification spec1 = new Specification();
		spec1.expcHeap = new SymbolicHeapAsDigraph(Arrays.asList(oNull, p1, v0, v1, v2, v3), null);
		spec1.condition = new ApplyExpr(AND, f0, f3);
		MatchResult ret11 = supHeap1.matchSpecification(spec1);
		assertNotNull(ret11);
		assertEquals(o2, ret11.objSrcMap.get(p1));
		assertEquals("0", ret11.model.get(v2.getVariable()).toSMTString());
		assertNotEquals("0", ret11.model.get(v3.getVariable()).toSMTString());
		assertNotEquals("1", ret11.model.get(v3.getVariable()).toSMTString());
		MatchResult ret21 = supHeap2.matchSpecification(spec1);
		assertNotNull(ret21);
		assertEquals(o1, ret21.objSrcMap.get(p1));
		assertEquals("2", ret21.model.get(v2.getVariable()).toSMTString());
	}

}
