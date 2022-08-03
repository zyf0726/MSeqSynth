package mseqsynth.wrapper.smt;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import mseqsynth.smtlib.ApplyExpr;
import mseqsynth.smtlib.BoolConst;
import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.Constant;
import mseqsynth.smtlib.IntConst;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTExpression;
import mseqsynth.smtlib.SMTOperator;
import mseqsynth.smtlib.SMTSort;
import mseqsynth.smtlib.UserFunc;
import mseqsynth.smtlib.Variable;

public class SMTSolverTest {
	
	private static final int N = 40;
	
	private static SMTSolver z3Java;
	private Variable[] iv, bv;
	private Map<Variable, Constant> model;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		z3Java = new Z3JavaAPI();
	}

	@Before
	public void setUp() throws Exception {
		iv = new IntVar[N];
		bv = new BoolVar[N];
		for (int i = 0; i < N; ++i) {
			iv[i] = new IntVar();
			bv[i] = new BoolVar();
		}
		model = new HashMap<>();
	}
	
	private void mkTest1(SMTSolver z3) {
		// add(x, y) := x + y
		UserFunc add = new UserFunc(Arrays.asList(iv[0], iv[1]), SMTSort.INT,
				new ApplyExpr(SMTOperator.ADD, iv[0], iv[1]));
		// mul2(x) := add(x, x)
		UserFunc mul2 = new UserFunc(Arrays.asList(iv[0]), SMTSort.INT,
				new ApplyExpr(add, iv[0], iv[0]));
		// mul3(y) := add(y, mul2(y))
		UserFunc mul3 = new UserFunc(Arrays.asList(iv[1]), SMTSort.INT,
				new ApplyExpr(add, iv[1], new ApplyExpr(mul2, iv[1])));
		// eq1: y = 1 + mul3(x)
		SMTExpression eq1 = new ApplyExpr(SMTOperator.BIN_EQ, iv[1],
				new ApplyExpr(SMTOperator.ADD, new IntConst(1), new ApplyExpr(mul3, iv[0])));
		// eq2: y = mul2(x)
		SMTExpression eq2 = new ApplyExpr(SMTOperator.BIN_EQ, iv[1],
				new ApplyExpr(mul2, iv[0]));
		// eq3: mul2(y) = add(1, mul2(x))
		SMTExpression eq3 = new ApplyExpr(SMTOperator.BIN_EQ,
				new ApplyExpr(mul2, iv[1]),
				new ApplyExpr(add, new IntConst(1), new ApplyExpr(mul2, iv[0])));
		
		assertTrue(z3.checkSat(new ApplyExpr(SMTOperator.AND, eq1, eq2), model));
		assertEquals("-1", model.get(iv[0]).toSMTString());
		assertEquals("-2", model.get(iv[1]).toSMTString());
		assertFalse(z3.checkSat(eq3, null));
	}

	@Test
	public void testZ3JavaAPI1() {
		mkTest1(z3Java);
	}
	
	private void mkTest2(SMTSolver z3) {
		SMTExpression e1 = new ApplyExpr(SMTOperator.BIN_NE, bv[0], bv[1]);
		SMTExpression e2 = new ApplyExpr(SMTOperator.BIN_NE, bv[1], bv[2]);
		SMTExpression constraint = new ApplyExpr(SMTOperator.AND, e1, e2, bv[2]);
		model.put(bv[0], new BoolConst(false));
		assertFalse(z3.checkSat(constraint, model));
		assertTrue(z3.checkSat(constraint, null));
		model.put(bv[0], new BoolConst(true));
		assertTrue(z3.checkSat(constraint, model));
		assertEquals("false", model.get(bv[1]).toSMTString());
	}
	
	@Test
	public void testZ3JavaAPI2() {
		mkTest2(z3Java);
	}
	
	private void mkTest3(SMTSolver z3) {
		UserFunc[] f = new UserFunc[N];
	 	// f[0](x) = x
	 	f[0] = new UserFunc(Arrays.asList(iv[0]), SMTSort.INT, iv[0]);
	 	// f[1](x) = 3x
	 	f[1] = new UserFunc(Arrays.asList(iv[1]), SMTSort.INT,
	 			new ApplyExpr(SMTOperator.MUL, new IntConst(3), iv[1]));
	 	for (int i = 2; i < N; ++i) {
	 		// e1: 2f[i-1](x)
	 		SMTExpression e1 = new ApplyExpr(SMTOperator.MUL,
	 				new IntConst(2), new ApplyExpr(f[i-1], iv[i]));
	 		// e2: 4f[i-2](x)
	 		SMTExpression e2 = new ApplyExpr(SMTOperator.MUL,
	 				new IntConst(4), new ApplyExpr(f[i-2], iv[i]));
	 		// f[i](x) = 2f[i-1](x) - 4[f-2](x)
	 		f[i] = new UserFunc(Arrays.asList(iv[i]), SMTSort.INT,
	 				new ApplyExpr(SMTOperator.SUB, e1, e2));
	 	}
	 	// f[38](x) = 5 * (2 ** 37)
	 	SMTExpression constr1 = new ApplyExpr(SMTOperator.BIN_EQ,
	 			new ApplyExpr(f[38], iv[0]), new IntConst(5l << 37));
	 	assertTrue(z3.checkSat(constr1, model));
	 	assertEquals("5", model.get(iv[0]).toSMTString());
	 	// f[39](x) = 1234567890
	 	SMTExpression constr2 = new ApplyExpr(SMTOperator.BIN_EQ,
	 			new ApplyExpr(f[39], iv[0]), new IntConst(1234567890l));
	 	assertFalse(z3.checkSat(constr2, null));
	}
	
	@Test
	public void testZ3JavaAPI3() {
		mkTest3(z3Java);
	}

	private void mkTest4(SMTSolver z3) {
		UserFunc f = new UserFunc(Arrays.asList(bv[0], iv[0]), SMTSort.BOOL, BoolConst.FALSE);
		SMTExpression e = new ApplyExpr(SMTOperator.OR, bv[0],
				new ApplyExpr(f, bv[1], iv[0]));
		assertTrue(z3.checkSat(e, model));
	}
	
	@Test
	public void testZ3JavaAPI4() {
		mkTest4(z3Java);
	}
	
	private void mkTest5(SMTSolver z3) {
		UserFunc[] F = new UserFunc[41];
		Variable x = new IntVar();
		F[0] = new UserFunc(Arrays.asList(x), SMTSort.BOOL,
				new ApplyExpr(SMTOperator.BIN_GT,
						new ApplyExpr(SMTOperator.MUL, x, x),
						new ApplyExpr(SMTOperator.ADD, x, new IntConst(100))));
		// (define-fun F0 ((x Int)) Bool (> (* x x) (+ x 100)))
		F[1] = new UserFunc(Arrays.asList(x), SMTSort.BOOL,
				new ApplyExpr(SMTOperator.BIN_LT,
						new ApplyExpr(SMTOperator.MUL, x, x, x),
						new IntConst(1799491)));
		// (define-fun F1 ((x Int)) Bool (< (* x x x) 1799491))
		for (int k = 2; k <= 40; ++k) {
			F[k] = new UserFunc(Arrays.asList(x), SMTSort.BOOL,
					new ApplyExpr(SMTOperator.BIN_IMPLY,
							new ApplyExpr(SMTOperator.BIN_GT, x, new IntConst(k)),
							new ApplyExpr(SMTOperator.DISTINCT,
									new ApplyExpr(F[k - 1], x), new ApplyExpr(F[k - 2], x))));
		// (define-fun F#k ((x Int)) Bool (=> (> x k) (distinct (F#{k-1} x) (F#{k-2} x))))
		}
		Variable a = new IntVar();
		// (declare-const a Int)
		SMTExpression e1 = new ApplyExpr(SMTOperator.BIN_GT, a, new IntConst(0));
		SMTExpression e2 = new ApplyExpr(F[40], a);
		SMTExpression e3 = new ApplyExpr(F[40],
				new ApplyExpr(SMTOperator.ADD, a, new IntConst(17)));
		SMTExpression e = new ApplyExpr(SMTOperator.AND, e1, e2, e3);
		// (assert (and (> a 0) (F40 a) (F40 (+ a 17))))
		assertTrue(z3.checkSat(e, model));
	}
		
	@Test
	public void testZ3JavaAPI5() {
		mkTest5(z3Java);
	}

}
