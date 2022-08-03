package mseqsynth.smtlib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/*
 * Classes Under Test:
 * - ExistExpr
 * 
 */

public class SMTQuanExprTest {
	
	private final int N = 5;
	private Variable ivs[], bvs[];
	
	@Before
	public void setUp() throws Exception {
		ivs = new IntVar[N];
		bvs = new BoolVar[N];
		for (int i = 0; i < N; ++i) {
			ivs[i] = new IntVar();
			bvs[i] = new BoolVar();
		}
	}
	
	@Test
	public void testExistExpr1() {
		Variable iv0 = new IntVar("IV0");
		Variable bv1 = new BoolVar("BV1");
		Variable iv2 = new BoolVar("IV2");
		Variable iv3 = new IntVar("IV3");
		
		assertTrue(ExistExpr.ALWAYS_TRUE.getBoundVariables().isEmpty());
		assertEquals("(exists ((DUMMY Bool)) false)", ExistExpr.ALWAYS_FALSE.toSMTString());
		ExistExpr e = new ExistExpr(Arrays.asList(iv0, bv1),
			new ApplyExpr(SMTOperator.BIN_EQ,
					IntConst.DEFAULT,
					new ApplyExpr(SMTOperator.ADD, iv0, iv2, iv3)
			)
		);
		assertEquals(Sets.newHashSet(bv1, iv0), e.getBoundVariables());
		String str1 = "(exists ((IV0 Int) (BV1 Bool)) (= 0 (+ IV0 IV2 IV3)))";
		String str2 = "(exists ((BV1 Bool) (IV0 Int)) (= 0 (+ IV0 IV2 IV3)))";
		assertTrue(e.toSMTString().equals(str1) || e.toSMTString().equals(str2));
	}
	
	@Test
	public void testExistExpr2() {
		ExistExpr e0 = new ExistExpr(
				Arrays.asList(ivs[0], ivs[2], ivs[0]),
				new ApplyExpr(SMTOperator.OR, ivs[0], ivs[3])
		);
		ExistExpr e1 = new ExistExpr(
				Arrays.asList(ivs[0], ivs[1], ivs[1]),
				new ApplyExpr(SMTOperator.ADD, ivs[1], ivs[0])
		);
		ExistExpr e2 = new ExistExpr(
				Arrays.asList(ivs[1], bvs[0]),
				new ApplyExpr(SMTOperator.OR, bvs[0], ivs[1])
		);
		ExistExpr e3 = new ExistExpr(
				Arrays.asList(bvs[1], bvs[2]),
				new ApplyExpr(SMTOperator.AND, bvs[1], bvs[2])
		);
		Map<Variable, Variable> renameMap = new HashMap<>();
		ExistExpr e = ExistExpr.makeOr(Arrays.asList(e0, e1, e2, e3), renameMap);
		
		assertEquals(4, e.getBoundVariables().size());
		assertEquals(ImmutableSet.of(ivs[0], ivs[1], ivs[2], bvs[0], bvs[1], bvs[2]),
				renameMap.keySet());
		assertEquals(e.getBoundVariables(), ImmutableSet.copyOf(renameMap.values()));
		assertNotEquals(renameMap.get(ivs[0]), renameMap.get(ivs[2]));
		assertNotEquals(renameMap.get(ivs[0]), renameMap.get(ivs[1]));
		assertNotEquals(renameMap.get(bvs[1]), renameMap.get(bvs[2]));
		assertEquals(SMTSort.INT, renameMap.get(ivs[1]).getSMTSort());
		assertEquals(SMTSort.BOOL, renameMap.get(bvs[0]).getSMTSort());
		
		SMTExpression tempE = new ApplyExpr(SMTOperator.OR,
				e0.getBody(), e1.getBody(), e2.getBody(), e3.getBody()
		);
		assertEquals(tempE.getSubstitution(renameMap).toSMTString(), e.getBody().toSMTString());
	}
	
	@Test
	public void testExistExpr3() {
		ExistExpr e0 = new ExistExpr(Arrays.asList(ivs), BoolConst.DEFAULT);
		ExistExpr e1 = new ExistExpr(Arrays.asList(bvs), BoolConst.DEFAULT);
		ExistExpr e = ExistExpr.makeOr(Arrays.asList(e0, e1), null);
		assertEquals(N + N, e.getBoundVariables().size());
	}
	
	@Test(expected = NullPointerException.class)
	public void testExistExprExc1() {
		new ExistExpr(Arrays.asList(), null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testExistExprExc2() {
		ExistExpr.ALWAYS_TRUE.getBoundVariables().add(ivs[0]);
	}
	
	@Test(expected = NullPointerException.class)
	public void testExistExprExc3() {
		new ExistExpr(Arrays.asList(ivs[0], null, bvs[0]), BoolConst.DEFAULT);
	}
	
}
