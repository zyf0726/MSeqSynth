package mseqsynth.smtlib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/* 
 * Classes Under Test:
 * - SMTSort
 * - ApplyExpr
 * - Constant, BoolConst, IntConst
 * - Variable, BoolVar, IntVar
 * 
 */

public class SMTExpressionTest {
	
	private static Variable bv0, bvTest, bv2;
	private static Variable ivTest, iv1;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BoolVar.resetCounter(0);
		IntVar.resetCounter(0);
		bv0 = new BoolVar();
		ivTest = new IntVar("Vtest");
		bvTest = new BoolVar("Vtest");
		bv2 = new BoolVar();
		iv1 = new IntVar();
	}

	@Test
	public void testConstant() {
		Constant bcFalse = BoolConst.FALSE;
		Constant bcTrue = BoolConst.TRUE;
		Constant bcDefault = BoolConst.DEFAULT;
		Constant icZero = IntConst.DEFAULT;
		Constant icNeg = new IntConst(-7);
		assertEquals(Collections.emptySet(), bcTrue.getFreeVariables());
		assertEquals(Collections.emptySet(), icZero.getUserFunctions());
		assertEquals(icNeg, icNeg.getSubstitution(Collections.emptyMap()));
		assertEquals(SMTSort.BOOL, bcFalse.getSMTSort());
		assertEquals(SMTSort.INT, icZero.getSMTSort());
		assertEquals("false", bcFalse.toSMTString());
		assertEquals("true", bcTrue.toSMTString());
		assertEquals("false", bcDefault.toSMTString());
		assertEquals("0", icZero.toSMTString());
		assertEquals("-7", icNeg.toSMTString());
	}
	
	@Test
	public void testVariable1() {
		assertEquals(3, BoolVar.getCounter());
		assertEquals(2, IntVar.getCounter());
		assertEquals(SMTSort.BOOL, bv0.getSMTSort());
		assertEquals(SMTSort.INT, ivTest.getSMTSort());
		
		assertEquals(BoolVar.VARNAME_PREFIX + "0", bv0.toSMTString());
		assertEquals("Vtest", ivTest.toSMTString());
		assertEquals(IntVar.VARNAME_PREFIX + "1", iv1.toSMTString());
		
		assertTrue(ivTest.equals(bvTest));
		assertTrue(bv2.equals(bv2));
		assertFalse(bv2.equals(null));
		assertEquals(0, bvTest.compareTo(ivTest));
		assertEquals(bv0.toSMTString().compareTo(bv2.toSMTString()), bv0.compareTo(bv2));
		assertEquals(iv1.toSMTString().compareTo(bv0.toSMTString()), iv1.compareTo(bv0));
		assertEquals(ivTest.hashCode(), bvTest.hashCode());
		assertNotEquals(bv2.hashCode(), bv0.hashCode());
		
		assertEquals("(declare-const Vtest Int)", ivTest.getSMTDecl());
		assertEquals("(declare-const B2 Bool)", bv2.getSMTDecl());
	}
	
	@Test
	public void testVariable2() {
		Variable ivClone = ivTest.cloneVariable();
		Variable bvClone = bvTest.cloneVariable();
		assertEquals(ivTest.getSMTSort(), ivClone.getSMTSort());
		assertEquals(bvTest.getSMTSort(), bvClone.getSMTSort());
		assertNotEquals(ivTest.toSMTString(), ivClone.toSMTString());
		assertNotEquals(bvTest.toSMTString(), bvClone.toSMTString());
		assertEquals(SMTSort.INT, Variable.create(SMTSort.INT).getSMTSort());
		assertEquals(SMTSort.BOOL, Variable.create(SMTSort.BOOL).getSMTSort());
		assertEquals(Collections.singleton(ivClone), ivClone.getFreeVariables());
		assertEquals(Collections.singleton(bvClone), bvClone.getFreeVariables());
		assertEquals(Collections.emptySet(), ivClone.getUserFunctions());
		assertEquals(Collections.emptySet(), bvClone.getUserFunctions());
	}
	
	@Test
	public void testVariable3() {
		Map<Variable, SMTExpression> vMap = new HashMap<>();
		vMap.put(ivTest, new BoolConst(true));
		vMap.put(bv2, bv0);
		vMap.put(bv0, bv2);
		assertEquals("true", bvTest.getSubstitution(vMap).toSMTString());
		assertEquals(bv0.toSMTString(), bv2.getSubstitution(vMap).toSMTString());
		assertEquals(bv2.toSMTString(), bv0.getSubstitution(vMap).toSMTString());
		assertEquals(iv1.toSMTString(), iv1.getSubstitution(vMap).toSMTString());
	}
	
	@Test(expected = NullPointerException.class)
	public void testVariableExc1() {
		new IntVar(null);
	}
	
	@Test(expected = NullPointerException.class)
	public void testVariableExc2() {
		Variable.create(null);
	}
	
	@Test
	public void testApplyExpr() {
		UserFunc F = new UserFunc("F", Arrays.asList(iv1), SMTSort.BOOL, BoolConst.DEFAULT);
		UserFunc G = new UserFunc("G", Arrays.asList(bv0), SMTSort.INT, IntConst.DEFAULT);
		ApplyExpr e0 = new ApplyExpr(F, BoolConst.DEFAULT);
		ApplyExpr e1 = new ApplyExpr(F, bv0, bvTest, bv2);
		ApplyExpr e2 = new ApplyExpr(SMTOperator.UN_MINUS, iv1);
		ApplyExpr e3 = new ApplyExpr(SMTOperator.SUB, e0, e2, e2);
		ApplyExpr e4 = new ApplyExpr(G, e3, e1);
		Set<Variable> allVars = ImmutableSet.of(bv0, bvTest, bv2, iv1);
		
		assertNull(e3.getSMTSort());
		assertEquals(Collections.emptySet(), e0.getFreeVariables());
		assertEquals(Collections.singleton(iv1), e3.getFreeVariables());
		assertEquals(allVars, e4.getFreeVariables());
		assertEquals(Collections.emptySet(), e2.getUserFunctions());
		assertEquals(Collections.singleton(F), e0.getUserFunctions());
		assertEquals(ImmutableSet.of(F, G), e4.getUserFunctions());
		assertEquals("(G (- (F false) (- I1) (- I1)) (F B0 Vtest B2))",
				e4.toSMTString());
		
		Map<Variable, SMTExpression> vMap = new HashMap<>();
		vMap.put(iv1, e1);
		vMap.put(bv2, bv0);
		vMap.put(bv0, IntConst.DEFAULT);
		assertEquals("(- (F false) (- (F B0 Vtest B2)) (- (F B0 Vtest B2)))",
				e3.getSubstitution(vMap).toSMTString());
		assertEquals("(F 0 Vtest B0)", e1.getSubstitution(vMap).toSMTString());
	}
	
	@Test(expected = NullPointerException.class)
	public void testApplyExprExc1() {
		new ApplyExpr(null, ivTest);
	}
	
	@Test(expected = NullPointerException.class)
	public void testApplyExprExc2() {
		List<SMTExpression> lv = null;
		new ApplyExpr(SMTOperator.ADD, lv);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testApplyExprExc3() {
		new ApplyExpr(SMTOperator.ADD);
	}
	
	@Test(expected = NullPointerException.class)
	public void testApplyExprExc4() {
		new ApplyExpr(SMTOperator.ADD, bvTest, null, ivTest);
	}
	
}
