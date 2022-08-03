package mseqsynth.smtlib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

/*
 * Classes Under Test:
 * - SMTOperator
 * - UserFunc
 */

public class SMTFunctionTest {
	
	private static Variable iv1, bv2, iv3;
	private static SMTExpression e1, e2;
	private static UserFunc uf0, uf1, uf2;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		UserFunc.resetCounter(0);
		iv1 = new IntVar("iv1");
		bv2 = new BoolVar("bv2");
		iv3 = new IntVar("iv3");
		// uf0(iv1, iv3) := false
		uf0 = new UserFunc("UF0", Arrays.asList(iv1, iv3), SMTSort.BOOL, BoolConst.FALSE);
		// e1 := (iv1 * iv1) != (-iv3)
		e1 = new ApplyExpr(SMTOperator.BIN_NE,
				new ApplyExpr(SMTOperator.MUL, iv1, iv1),
				new ApplyExpr(SMTOperator.UN_MINUS, iv3));
		// e2 := bv2 and uf0(iv3, iv1, iv3)
		e2 = new ApplyExpr(SMTOperator.AND, bv2, new ApplyExpr(uf0, iv3, iv1, iv3));
		// uf1(bv2, iv1, iv3) := e2
		uf1 = new UserFunc(Arrays.asList(bv2, iv1, iv3), SMTSort.INT, e2);
		// uf2(iv1) := uf1(iv1, iv1)
		uf2 = new UserFunc(Arrays.asList(iv1), SMTSort.INT, new ApplyExpr(uf1, iv1, iv1));
	}

	@Test
	public void testSMTOperator() {
		SMTFunction fImply = SMTOperator.BIN_IMPLY;
		SMTFunction fDistinct = SMTOperator.DISTINCT;
		assertNull(fImply.getArgs());
		assertNull(fDistinct.getRange());
		assertNull(fDistinct.getBody());
	}
	
	@Test
	public void testUserFunc() {
		assertEquals(3, UserFunc.getCounter());
		
		assertEquals(Arrays.asList(bv2, iv1, iv3), uf1.getArgs());
		assertNotEquals(Arrays.asList(iv1, iv3, bv2), uf1.getArgs());
		assertEquals(Collections.singletonList(iv1), uf2.getArgs());
		assertEquals(SMTSort.BOOL, uf0.getRange());
		assertEquals(SMTSort.INT, uf1.getRange());
		
		assertEquals("false", uf0.getBody().toSMTString());
		assertEquals("(and bv2 (UF0 iv3 iv1 iv3))", uf1.getBody().toSMTString());
		uf0.setBody(e1);
		assertEquals("(distinct (* iv1 iv1) (- iv3))", uf0.getBody().toSMTString());
		
		assertEquals("(declare-fun UF0 (Int Int) Bool)", uf0.getSMTDecl());
		assertEquals("(define-fun FUN1 ((bv2 Bool) (iv1 Int) (iv3 Int)) Int " + 
						"(and bv2 (UF0 iv3 iv1 iv3)))",
						uf1.getSMTDef());
		assertEquals("(assert (forall ((iv1 Int)) (= (FUN2 iv1) (FUN1 iv1 iv1))))",
						uf2.getSMTAssert());
		
		assertTrue(uf0.compareTo(uf1) < 0);
		assertTrue(uf2.compareTo(uf1) > 0);
		assertTrue(uf0.compareTo(uf0) == 0);
	}
	
	@Test(expected = NullPointerException.class)
	public void testUserFuncExc1() {
		new UserFunc(null, Arrays.asList(iv1), SMTSort.INT, IntConst.DEFAULT);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testUserFuncExc2() {
		new UserFunc(Arrays.asList(), SMTSort.BOOL, BoolConst.DEFAULT);
	}
	
	@Test(expected = NullPointerException.class)
	public void testUserFuncExc3() {
		new UserFunc("G", null, SMTSort.BOOL, BoolConst.DEFAULT);
	}
	
	@Test(expected = NullPointerException.class)
	public void testUserFuncExc4() {
		new UserFunc(Arrays.asList(iv1), null, IntConst.DEFAULT);
	}
	
	@Test(expected = NullPointerException.class)
	public void testUserFuncExc5() {
		new UserFunc(Arrays.asList(iv1), SMTSort.BOOL, null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testUserFuncExc6() {
		new UserFunc(Arrays.asList(iv1), SMTSort.INT, bv2);
	}
	
	@Test(expected = NullPointerException.class)
	public void testUserFuncExc7() {
		new UserFunc(Arrays.asList(iv1, null, bv2), SMTSort.BOOL, bv2);
	}
	
}
