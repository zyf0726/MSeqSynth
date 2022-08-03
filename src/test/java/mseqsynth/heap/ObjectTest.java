package mseqsynth.heap;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTSort;
import mseqsynth.smtlib.Variable;

public class ObjectTest {
	
	private static final String filename = "tmp/ObjectTest.tmp";
	private static final File file = new File(filename);
	
	@SuppressWarnings("unused")
	private class Node {
		public Node next;
		public int value;
	}
	
	private static ClassH cNode;
	private static FieldH fNext, fValue;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		cNode = ClassH.of(Node.class);
		fNext = FieldH.of(Node.class.getDeclaredField("next"));
		fValue = FieldH.of(Node.class.getDeclaredField("value"));
	}
	
	@After
	public void tearDown() throws Exception {
		ObjectH.STRICT_MODE = true;
		file.delete();
	}

	@Test
	public void testClass1() {
		ClassH cNull = ClassH.CLS_NULL;
		
		assertNull(cNull.getJavaClass());
		assertNull(cNull.getSMTSort());
		assertTrue(cNull.isNullClass());
		assertFalse(cNull.isNonNullClass());
		assertTrue(cNull.isJavaClass());
		assertFalse(cNull.isSMTSort());
	}
	
	@Test
	public void testClass2() {
		ClassH cNode_dup = ClassH.of(Node.class);
		assertTrue(cNode == cNode_dup);
		assertNotEquals(ClassH.CLS_NULL, cNode_dup);
		
		assertEquals(Node.class, cNode.getJavaClass());
		assertNull(cNode.getSMTSort());
		assertFalse(cNode.isNullClass());
		assertTrue(cNode.isNonNullClass());
		assertTrue(cNode.isJavaClass());
		assertFalse(cNode.isSMTSort());
	}
	
	@Test
	public void testClass3() {
		ClassH cBool = ClassH.of(SMTSort.BOOL);
		ClassH cInt = ClassH.of(SMTSort.INT);
		assertTrue(ClassH.of(SMTSort.BOOL) == cBool);
		assertEquals(cInt, ClassH.of(SMTSort.INT));
		assertNotEquals(cBool, cInt);
		
		assertNull(cBool.getJavaClass());
		assertEquals(SMTSort.BOOL, cBool.getSMTSort());
		assertEquals(SMTSort.INT, cInt.getSMTSort());
		assertFalse(cInt.isNullClass());
		assertFalse(cBool.isNonNullClass());
		assertFalse(cInt.isJavaClass());
		assertTrue(cBool.isSMTSort());
	}
	
	@Test
	public void testClassSerial() throws Exception {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(ClassH.CLS_NULL); 
		oos.writeObject(ClassH.of(Node.class)); 
		oos.writeObject(ClassH.of(Node.class));
		oos.writeObject(ClassH.of(SMTSort.BOOL));
		oos.writeObject(ClassH.of(SMTSort.INT));
		oos.close();
		fos.close();
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		assertTrue(ClassH.CLS_NULL == ois.readObject());
		assertTrue(ClassH.of(Node.class) == ois.readObject());
		assertTrue(ClassH.of(Node.class) == ois.readObject());
		assertTrue(ClassH.of(SMTSort.BOOL) == ois.readObject());
		assertTrue(ClassH.of(SMTSort.INT) == ois.readObject());
		ois.close();
		fis.close();
	}
	
	@Test(expected = NullPointerException.class)
	public void testClassExc1() {
		ClassH.of((Class<Node>) null);
	}
	
	@Test(expected = NullPointerException.class)
	public void testClassExc2() {
		ClassH.of((SMTSort) null);
	}
	
	@Test
	public void testField() throws NoSuchFieldException {
		FieldH fNext_dup = FieldH.of(Node.class.getField("next"));
		FieldH fValue_dup = FieldH.of(Node.class.getField("value"));
		assertTrue(fNext == fNext_dup);
		assertEquals(fValue, fValue_dup);
		assertNotEquals(fNext, fValue);
		
		assertTrue(fValue.compareTo(fNext) / fNext.compareTo(fValue) < 0);
		assertTrue(fValue.compareTo(fValue) == 0);
		assertEquals("next", fNext.getName());
		assertEquals("value", fValue.getName());
	}
	
	@Test
	public void testFieldSerial() throws Exception {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(FieldH.of(Node.class.getField("next")));
		oos.writeObject(FieldH.of(Node.class.getField("value")));
		oos.writeObject(FieldH.of(Node.class.getField("next")));
		oos.writeObject(FieldH.of(Node.class.getField("value")));
		oos.close();
		fos.close();
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		assertTrue(fNext == ois.readObject());
		assertTrue(fValue == ois.readObject());
		assertTrue(fNext == ois.readObject());
		assertTrue(fValue == ois.readObject());
		ois.close();
		fis.close();
	}
	
	@Test(expected = NullPointerException.class)
	public void testFieldExc1() {
		FieldH.of(null);
	}
	
	@Test
	public void testObject1() {
		ObjectH oNull = ObjectH.NULL;
		assertTrue(oNull.isNullObject());
		assertFalse(oNull.isNonNullObject());
		assertTrue(oNull.isHeapObject());
		assertFalse(oNull.isVariable());
		assertEquals(ClassH.CLS_NULL, oNull.getClassH());
		assertNull(oNull.getVariable());
	}
	
	@Test
	public void testObject2() {
		Variable v1 = new IntVar(), v2 = new BoolVar();
		ObjectH ov1 = new ObjectH(v1), ov2 = new ObjectH(v2);
		assertFalse(ov1.isNullObject());
		assertFalse(ov2.isNonNullObject());
		assertFalse(ov1.isHeapObject());
		assertTrue(ov2.isVariable());
		assertEquals(ClassH.of(SMTSort.INT), ov1.getClassH());
		assertEquals(ClassH.of(SMTSort.BOOL), ov2.getClassH());
		assertEquals(v1, ov1.getVariable());
		assertEquals(v2, ov2.getVariable());
	}
	
	@Test
	public void testObject3() {
		ObjectH.STRICT_MODE = false;
		Variable v1 = new IntVar(), v2 = new IntVar();
		ObjectH ov1 = new ObjectH(v1), ov2 = new ObjectH(v2);
		ObjectH o1 = new ObjectH(cNode, ImmutableMap.of());
		ObjectH o2 = new ObjectH(cNode, ImmutableMap.of(fNext, o1, fValue, ov2));
		assertFalse(o1.isNullObject());
		assertTrue(o2.isNonNullObject());
		assertTrue(o1.isHeapObject());
		assertFalse(o2.isVariable());
		assertEquals(cNode, o1.getClassH());
		assertNull(o2.getVariable());
		
		assertEquals(Collections.emptySet(), o1.getFields());
		assertEquals(ImmutableSet.of(o1, ov2), ImmutableSet.copyOf(o2.getValues()));;
		assertEquals(ObjectH.NULL.getEntries(), o1.getEntries());
		o1.setFieldValueMap(ImmutableMap.of(fNext, o2, fValue, ov1));
		assertEquals(o1, o1.getFieldValue(fNext).getFieldValue(fNext));
		assertEquals(ov2, o2.getFieldValue(fValue));
	}
	
	@Test
	public void testObject4() {
		ObjectH o1 = new ObjectH(cNode, null);
		o1.setFieldValueMap(ImmutableMap.of(fNext, o1));
		ObjectH o2 = new ObjectH(cNode, ImmutableMap.of(fNext, o1));
		assertEquals(o1.getEntries(), o2.getEntries());
	}
	
	@Test
	public void testObject5() {
		ObjectH o = new ObjectH(ClassH.of(Object.class), null);
		assertFalse(o.isNonNullObject());
		assertFalse(o.isHeapObject());
		assertFalse(o.isNullObject());
		assertTrue(o.isVariable());
		assertNotNull(o.getVariable());
	}
	
	@Test
	public void testObjectSerial() throws Exception {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(ObjectH.NULL);
		ObjectH ov = new ObjectH(new IntVar());
		ObjectH o1 = new ObjectH(cNode, ImmutableMap.of());
		ObjectH o2 = new ObjectH(cNode, ImmutableMap.of(fNext, o1, fValue, ov));
		oos.writeObject(ov);
		oos.writeObject(o1);
		oos.writeObject(o2);
		oos.close();
		fos.close();
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		assertTrue(ObjectH.NULL == ois.readObject());
		ObjectH rv = (ObjectH) ois.readObject();
		assertEquals(SMTSort.INT, rv.getVariable().getSMTSort());
		assertEquals(ov.getVariable(), rv.getVariable());
		ObjectH r1 = (ObjectH) ois.readObject();
		assertTrue(cNode == r1.getClassH());
		assertTrue(r1.getEntries().isEmpty());
		ObjectH r2 = (ObjectH) ois.readObject();
		assertTrue(cNode == r2.getClassH());
		assertTrue(r1 == r2.getFieldValue(fNext));
		assertTrue(rv == r2.getFieldValue(fValue));
		ois.close();
		fis.close();
	}
	
	@Test(expected = IllegalStateException.class)
	public void testObjectExc1() {
		ObjectH o = new ObjectH(cNode, ImmutableMap.of());
		o.setFieldValueMap(ImmutableMap.of());
	}
	
	@Test(expected = NullPointerException.class)
	public void testObjectExc2() {
		ObjectH o = new ObjectH(cNode, null);
		o.setFieldValueMap(null);
	}
	
	@Test(expected = NullPointerException.class)
	public void testObjectExc3() {
		new ObjectH(null);
	}

	@Test(expected = NullPointerException.class)
	public void testObjectExc4() {
		new ObjectH(null, ImmutableMap.of());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testObjectExc5() {
		new ObjectH(ClassH.CLS_NULL, ImmutableMap.of());
	}
	
}
