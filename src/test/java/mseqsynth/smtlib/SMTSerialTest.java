package mseqsynth.smtlib;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Test;

public class SMTSerialTest {
	
	private static final String filename = "tmp/SMTSerialTest.tmp";
	private static final File file = new File(filename);
	
	@After
	public void tearDown() throws Exception {
		file.delete();
	}
	
	private void testExprOut() throws Exception {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		Constant bTrue = new BoolConst(true);
		BoolConst bFalse = new BoolConst(false);
		Constant iZero = new IntConst(0);
		IntConst iTwo = new IntConst(2);
		oos.writeObject(bTrue);
		oos.writeObject(bFalse);
		oos.writeObject(iTwo);
		oos.writeObject(iZero);
		Variable v1 = new BoolVar("v1");
		Variable v2 = new IntVar("v2");
		oos.writeObject(v1);
		oos.writeObject(v1);
		oos.writeObject(v2);
		oos.writeObject(new IntVar("v2"));
		SMTExpression e1 = new ApplyExpr(SMTOperator.AND, v1, v2, iZero);
		oos.writeObject(e1);
		oos.close();
		fos.close();
	}
	
	private void testExprIn() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		BoolConst bTrue = (BoolConst) ois.readObject();
		BoolConst bFalse = (BoolConst) ois.readObject();
		assertEquals("true", bTrue.toSMTString());
		assertEquals("false", bFalse.toSMTString());
		assertEquals("2", ((Constant) ois.readObject()).toSMTString());
		assertEquals("0", ((Constant) ois.readObject()).toSMTString());
		BoolVar v1 = (BoolVar) ois.readObject();
		assertTrue(v1 == ois.readObject());
		IntVar v2 = (IntVar) ois.readObject();
		Variable _v2 = (Variable) ois.readObject();
		assertTrue(v2 != _v2);
		assertEquals(v2, _v2);
		assertEquals("(and v1 v2 0)", ((ApplyExpr) ois.readObject()).toSMTString());
		ois.close();
		fis.close();
	}
	
	@Test
	public void testExpr() throws Exception {
		testExprOut();
		testExprIn();
	}
	
	@Test
	public void testQExpr() throws Exception {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		Variable v1 = new IntVar("v1"), v2 = new BoolVar("v2");
		SMTQuantifiedExpr qe1 = new ExistExpr(Arrays.asList(v1, v2),
				new ApplyExpr(SMTOperator.BIN_EQ, v2,
						new ApplyExpr(SMTOperator.DISTINCT, v1, v2)));
		SMTQuantifiedExpr qe2 = new ExistExpr(Collections.emptyList(),
				new BoolConst(true));
		oos.writeObject(qe1);
		oos.writeObject(qe2);
		oos.close();
		fos.close();
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		ExistExpr _qe1 = (ExistExpr) ois.readObject();
		ExistExpr _qe2 = (ExistExpr) ois.readObject();
		assertEquals(qe1.getBoundVariables(), _qe1.getBoundVariables());
		assertEquals(qe1.getBody().toSMTString(), _qe1.getBody().toSMTString());
		assertEquals(Collections.emptySet(), _qe2.getBoundVariables());
		assertEquals(qe2.toSMTString(), _qe2.toSMTString());
		ois.close();
		fis.close();
	}
	
	@Test
	public void testFunc() throws Exception {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		Variable v1 = new BoolVar("v1"), v2 = new IntVar("v2");
		SMTFunction fun = new UserFunc("FUN", Arrays.asList(v1, v2), SMTSort.INT,
				new ApplyExpr(SMTOperator.MUL, v1, v2, v1));
		SMTFunction leq = SMTOperator.BIN_LE;
		oos.writeObject(fun);
		oos.writeObject(leq);
		oos.close();
		fos.close();
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		UserFunc _fun = (UserFunc) ois.readObject();
		assertEquals(fun.getName(), _fun.getName());
		assertEquals(fun.getRange(), _fun.getRange());
		assertEquals(fun.getArgs(), _fun.getArgs());
		assertEquals(fun.getBody().toSMTString(), _fun.getBody().toSMTString());
		assertEquals(0, _fun.compareTo((UserFunc) fun));
		assertEquals(SMTOperator.BIN_LE, ois.readObject());
		ois.close();
		fis.close();
	}

}
