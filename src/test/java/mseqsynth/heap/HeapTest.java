package mseqsynth.heap;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import mseqsynth.smtlib.ApplyExpr;
import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntConst;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTOperator;
import mseqsynth.smtlib.Variable;
import mseqsynth.util.Bijection;

public class HeapTest {
	
	@SuppressWarnings("unused")
	class Node {
		private Node next;
		private Object value;
	}
	
	@SuppressWarnings("unused")
	class ClassA {
		private int i;
		private ClassA a;
		private ClassB b;
	}
	
	@SuppressWarnings("unused")
	class ClassB {
		private int i;
		private long l;
	}
	
	private static ClassH cNode, cObj, cA, cB;
	private static FieldH fNext, fValue;
	private static FieldH fAI, fAA, fAB, fBI, fBL;
	
	private static final int N = 50;
	private static SymbolicHeap emp;
	private static ObjectH nodes[], ivs[], ovs[];

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		cNode = ClassH.of(Node.class);
		cObj = ClassH.of(Object.class);
		cA = ClassH.of(ClassA.class);
		cB = ClassH.of(ClassB.class);
		fNext = FieldH.of(Node.class.getDeclaredField("next"));
		fValue = FieldH.of(Node.class.getDeclaredField("value"));
		fAI = FieldH.of(ClassA.class.getDeclaredField("i"));
		fAA = FieldH.of(ClassA.class.getDeclaredField("a"));
		fAB = FieldH.of(ClassA.class.getDeclaredField("b"));
		fBI = FieldH.of(ClassB.class.getDeclaredField("i"));
		fBL = FieldH.of(ClassB.class.getDeclaredField("l"));
		assertNotEquals(fAI, fBI);
		emp = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_FALSE);
		nodes = new ObjectH[N];
		for (int i = 0; i < N; ++i)
			nodes[i] = new ObjectH(cNode, null);
		ivs = new ObjectH[N];
		for (int i = 0; i < N; ++i)
			ivs[i] = new ObjectH(new IntVar());
		ovs = new ObjectH[N];
		for (int i = 0; i < N; ++i)
			ovs[i] = new ObjectH(cObj, Collections.emptyMap());
		ObjectH.STRICT_MODE = false;
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ObjectH.STRICT_MODE = true;
	}

	@Test
	public void testAboutConstructor() {
		ObjectH oNull = ObjectH.NULL;
		ObjectH o2 = new ObjectH(cB, ImmutableMap.of(fBI, ivs[0], fBL, ivs[1]));
		ObjectH o1 = new ObjectH(cA, ImmutableMap.of());
		ObjectH o0 = new ObjectH(cA, ImmutableMap.of(fAA, o1, fAB, oNull, fAI, ivs[3]));
		SymbolicHeap h1 = new SymbolicHeapAsDigraph(Arrays.asList(o1, oNull), null);
		o1.setFieldValueMap(ImmutableMap.of(fAB, o2, fAI, ivs[2], fAA, o1));
		SymbolicHeap h2 = new SymbolicHeapAsDigraph(Arrays.asList(o1, oNull), null);
		ExistExpr e = new ExistExpr(Arrays.asList(ivs[0].getVariable()),
				new ApplyExpr(SMTOperator.UN_NOT, ivs[1].getVariable()));
		SymbolicHeap h3 = new SymbolicHeapAsDigraph(Arrays.asList(o2, oNull, o0), e);
		
		assertFalse(emp.getVariables().isEmpty());
		assertEquals(ImmutableSet.of(oNull), emp.getAllObjects());
		assertEquals(ImmutableSet.of(oNull), emp.getAccessibleObjects());
		assertEquals(ImmutableSet.of(o1, oNull), h1.getAllObjects());
		assertEquals(ImmutableSet.of(o1, o2, oNull, ivs[0], ivs[1], ivs[2]), h2.getAllObjects());
		assertTrue(h3.getAllObjects().contains(o1));
		
		assertEquals(ImmutableSet.of(o0, o2, oNull), h3.getAccessibleObjects());
		
		assertNull(h1.getConstraint());
		assertEquals(e, h3.getConstraint());
		
		assertEquals(Collections.emptyList(), h1.getVariables());
		for (int i = 0; i < 4; ++i)
			assertTrue(h3.getVariables().contains(ivs[i].getVariable()));
	}
	
	@Test(expected = NullPointerException.class)
	public void testAboutConstructorExc1() {
		new SymbolicHeapAsDigraph(null, ExistExpr.ALWAYS_TRUE);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testAboutConstructorExc2() {
		new SymbolicHeapAsDigraph(Arrays.asList(nodes[0]), null);
	}
	
	@Test(expected = NullPointerException.class)
	public void testAboutConstructorExc3() {
		new SymbolicHeapAsDigraph(Arrays.asList(ObjectH.NULL, null, nodes[1]), null);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testAboutConstructorExc4() {
		emp.getVariables().add(new IntVar());
	}
	
	class MappingChecker implements ActionIfFound {
		Bijection<ObjectH, ObjectH> aMap = null;
		boolean mappingFound = false;
		@Override
		public boolean emitMapping(Bijection<ObjectH, ObjectH> ret) {
			this.aMap = ret;
			this.mappingFound = true;
			return true;
		}
	}
	
	class MappingCounter implements ActionIfFound {
		Bijection<ObjectH, ObjectH> aMap = null;
		int mappingCounter = 0;
		@Override
		public boolean emitMapping(Bijection<ObjectH, ObjectH> ret) {
			this.aMap = ret;
			this.mappingCounter += 1;
			return false;
		}
	}
	
	@Test
	public void testCloneObjects() {
		nodes[0].setFieldValueMap(ImmutableMap.of(fNext, nodes[2], fValue, ovs[0]));
		nodes[1].setFieldValueMap(ImmutableMap.of(fNext, nodes[2], fValue, ovs[1]));
		nodes[2].setFieldValueMap(ImmutableMap.of(fNext, nodes[3], fValue, ivs[2]));
		nodes[3].setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL, fValue, ivs[3]));
		SymbolicHeap h = new SymbolicHeapAsDigraph(
				Arrays.asList(nodes[0], nodes[1], ObjectH.NULL), null);
		Bijection<ObjectH, ObjectH> cloneMap = new Bijection<>();
		Set<ObjectH> accObjsClone = h.cloneAllObjects(cloneMap);
		
		assertEquals(h.getAccessibleObjects().size(), accObjsClone.size());
		assertTrue(accObjsClone.contains(cloneMap.getV(nodes[1])));
		assertFalse(accObjsClone.contains(cloneMap.getV(nodes[2])));
		assertEquals(cloneMap.getV(nodes[3]), cloneMap.getV(nodes[2]).getFieldValue(fNext));
		assertEquals(cloneMap.getV(nodes[2]), cloneMap.getV(nodes[1]).getFieldValue(fNext));
		assertEquals(cloneMap.getV(nodes[2]), cloneMap.getV(nodes[0]).getFieldValue(fNext));
		assertEquals(ObjectH.NULL, cloneMap.getV(nodes[3]).getFieldValue(fNext));
		assertEquals(nodes[2].getClassH(), cloneMap.getV(nodes[2]).getClassH());
		for (int i = 0; i < 2; ++i) {
			assertNotEquals(ovs[i].getVariable(), cloneMap.getV(ovs[i]).getVariable());
			assertEquals(ovs[i].getClassH(), cloneMap.getV(ovs[i]).getClassH());
		}
		for (int i = 2; i < 4; ++i) {
			assertNotEquals(ivs[i].getVariable(), cloneMap.getV(ivs[i]).getVariable());
			assertEquals(ivs[i].getClassH(), cloneMap.getV(ivs[i]).getClassH());
		}
		
		SymbolicHeap h1 = new SymbolicHeapAsDigraph(accObjsClone, null);
		SymbolicHeap h2 = new SymbolicHeapAsDigraph(h.cloneAllObjects(null), null);
		MappingChecker chk = new MappingChecker();
		h1.findIsomorphicMappingTo(h2, chk);
		assertTrue(chk.mappingFound);
	}
	
	@Test
	public void testAboutIsomorphism1() {
		List<ObjectH> nodesA = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(nodes, 0, 10)));
		List<ObjectH> nodesB = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(nodes, 10, 20)));
		nodesA.add(ObjectH.NULL); nodesB.add(ObjectH.NULL);
		for (int i = 0; i < 10; ++i) {
			nodesA.get(i).setFieldValueMap(ImmutableMap.of(fNext, nodesA.get((i + 1) % 10)));
			nodesB.get(i).setFieldValueMap(ImmutableMap.of(fNext, nodesB.get((i + 9) % 10)));
		}
		SymbolicHeap hA = new SymbolicHeapAsDigraph(nodesA, null);
		SymbolicHeap hB = new SymbolicHeapAsDigraph(nodesB, null);
		
		assertTrue(hA.maybeIsomorphicWith(hB));
		assertEquals(hA.getFeatureCode(), hB.getFeatureCode());		
		MappingCounter ctr = new MappingCounter();
		hA.findIsomorphicMappingTo(hB, ctr);
		assertEquals(10, ctr.mappingCounter);
		for (int i = 0; i < 10; ++i) {
			ObjectH a0$b = ctr.aMap.getV(nodesA.get(0));
			ObjectH ai$b = ctr.aMap.getV(nodesA.get(i));
			assertEquals((0 + nodesB.indexOf(a0$b)) % 10, (i + nodesB.indexOf(ai$b)) % 10);
		}
		
		assertFalse(hA.maybeIsomorphicWith(emp));
		assertNotEquals(emp.getFeatureCode(), hA.getFeatureCode());
		MappingChecker chk = new MappingChecker();
		hA.findIsomorphicMappingTo(emp, chk);
		assertFalse(chk.mappingFound);
		assertNull(chk.aMap);
	}
	
	@Test
	public void testAboutIsomorphism2() {
		List<ObjectH> nodesA = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(nodes, 0, 5)));
		List<ObjectH> nodesB = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(nodes, 5, 15)));
		nodesA.add(ObjectH.NULL); nodesB.add(ObjectH.NULL);
		
		for (int i = 0; i < 5; ++i)
			nodesA.get(i).setFieldValueMap(ImmutableMap.of(fValue, ovs[i]));
		for (int i = 0; i < 10; ++i)
			nodesB.get(i).setFieldValueMap(ImmutableMap.of(fValue, ovs[i + 5]));
		SymbolicHeap hA = new SymbolicHeapAsDigraph(nodesA, null);
		SymbolicHeap hB = new SymbolicHeapAsDigraph(nodesB, null);
		assertFalse(hA.maybeIsomorphicWith(hB));
		assertNotEquals(hA.getFeatureCode(), hB.getFeatureCode());
		MappingCounter ctr = new MappingCounter();
		hA.findEmbeddingInto(hB, ctr);
		assertEquals(10 * 9 * 8 * 7 * 6, ctr.mappingCounter);
		
		for (int i = 0; i < 5; ++i)
			nodesA.get(i).setFieldValueMap(ImmutableMap.of(fNext, nodesA.get(i + 1)));
		for (int i = 0; i < 10; ++i)
			nodesB.get(i).setFieldValueMap(ImmutableMap.of(fNext, nodesB.get(i + 1)));
		hA = new SymbolicHeapAsDigraph(nodesA, null);
		hB = new SymbolicHeapAsDigraph(nodesB, null);
		assertFalse(hB.maybeIsomorphicWith(hA));
		assertNotEquals(hB.getFeatureCode(), hA.getFeatureCode());
		ctr = new MappingCounter();
		hA.findEmbeddingInto(hB, ctr);
		assertEquals(1, ctr.mappingCounter);
		for (int i = 0; i < 5; ++i)
			assertEquals(nodesB.get(5 + i), ctr.aMap.getV(nodesA.get(i)));
	}
	
	@Test
	public void testAboutIsomorphism3() {
		ObjectH u3 = new ObjectH(cNode, ImmutableMap.of(fNext, ObjectH.NULL));
		ObjectH u2 = new ObjectH(cNode, ImmutableMap.of(fNext, u3));
		ObjectH u1 = new ObjectH(cNode, ImmutableMap.of(fNext, u2));
		ObjectH v3 = new ObjectH(cNode, ImmutableMap.of(fNext, ObjectH.NULL));
		ObjectH v2 = new ObjectH(cNode, ImmutableMap.of(fNext, v3));
		ObjectH v1 = new ObjectH(cNode, ImmutableMap.of(fNext, v2));
		
		SymbolicHeap uh1 = new SymbolicHeapAsDigraph(Arrays.asList(u1, u3, ObjectH.NULL), null);
		SymbolicHeap vh = new SymbolicHeapAsDigraph(Arrays.asList(v1, v2, ObjectH.NULL), null);
		MappingChecker chk = new MappingChecker();
		// assertFalse(uh.maybeIsomorphicWith(vh));
		// assertNotEquals(uh.getFeatureCode(), vh.getFeatureCode());
		uh1.findIsomorphicMappingTo(vh, chk);
		assertFalse(chk.mappingFound);
		uh1.findEmbeddingInto(vh, chk);
		assertFalse(chk.mappingFound);
		
		SymbolicHeap uh2 = new SymbolicHeapAsDigraph(Arrays.asList(u1, ObjectH.NULL), null);
		chk = new MappingChecker();
		uh2.findEmbeddingInto(vh, chk);
		assertTrue(chk.mappingFound);
		assertEquals(v1, chk.aMap.getV(u1));
		assertEquals(u2, chk.aMap.getU(v2));
		
		SymbolicHeap uh3 = new SymbolicHeapAsDigraph(Arrays.asList(u1, u2, u3, ObjectH.NULL), null);
		chk = new MappingChecker();
		uh3.findEmbeddingInto(vh, chk);
		assertFalse(chk.mappingFound);
	}
	
	@Test
	public void testAboutIsomorphism4() {
		List<ObjectH> A = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(nodes, 0, 9)));
		List<ObjectH> B = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(nodes, 10, 19)));
		List<ObjectH> C = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(nodes, 20, 29)));
		A.add(ObjectH.NULL); B.add(ObjectH.NULL); C.add(ObjectH.NULL);
		
		A.get(0).setFieldValueMap(ImmutableMap.of(fNext, A.get(2)));
		A.get(1).setFieldValueMap(ImmutableMap.of(fNext, A.get(2)));
		A.get(2).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		A.get(3).setFieldValueMap(ImmutableMap.of(fNext, A.get(4)));
		A.get(4).setFieldValueMap(ImmutableMap.of(fNext, A.get(5)));
		A.get(5).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		A.get(6).setFieldValueMap(ImmutableMap.of(fNext, A.get(7)));
		A.get(7).setFieldValueMap(ImmutableMap.of(fNext, A.get(8)));
		A.get(8).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		SymbolicHeap hA = new SymbolicHeapAsDigraph(A, null);
		
		B.get(2).setFieldValueMap(ImmutableMap.of(fNext, B.get(0)));
		B.get(3).setFieldValueMap(ImmutableMap.of(fNext, B.get(0)));
		B.get(4).setFieldValueMap(ImmutableMap.of(fNext, B.get(1)));
		B.get(1).setFieldValueMap(ImmutableMap.of(fNext, B.get(7)));
		B.get(8).setFieldValueMap(ImmutableMap.of(fNext, B.get(6)));
		B.get(6).setFieldValueMap(ImmutableMap.of(fNext, B.get(5)));
		B.get(0).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		B.get(7).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		B.get(5).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		SymbolicHeap hB = new SymbolicHeapAsDigraph(B, null);
		
		C.get(0).setFieldValueMap(ImmutableMap.of(fNext, C.get(2)));
		C.get(2).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		C.get(1).setFieldValueMap(ImmutableMap.of(fNext, C.get(3)));
		C.get(3).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		C.get(4).setFieldValueMap(ImmutableMap.of(fNext, C.get(5)));
		C.get(5).setFieldValueMap(ImmutableMap.of(fNext, C.get(8)));
		C.get(6).setFieldValueMap(ImmutableMap.of(fNext, C.get(7)));
		C.get(7).setFieldValueMap(ImmutableMap.of(fNext, C.get(8)));
		C.get(8).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		SymbolicHeap hC = new SymbolicHeapAsDigraph(C, null);
		
		assertTrue(hB.maybeIsomorphicWith(hA));
		assertEquals(hA.getFeatureCode(), hB.getFeatureCode());
		// assertFalse(hC.maybeIsomorphicWith(hA));
		// assertNotEquals(hA.getFeatureCode(), hC.getFeatureCode());
		
		MappingCounter ctr = new MappingCounter();
		hB.findIsomorphicMappingTo(hA, ctr);
		assertEquals(4, ctr.mappingCounter);
		assertEquals(10, ctr.aMap.size());
		assertTrue(Arrays.asList(A.get(0), A.get(1)).contains(ctr.aMap.getV(B.get(2))));
		assertTrue(Arrays.asList(A.get(0), A.get(1)).contains(ctr.aMap.getV(B.get(3))));
		assertTrue(Arrays.asList(A.get(3), A.get(6)).contains(ctr.aMap.getV(B.get(4))));
		assertTrue(Arrays.asList(A.get(3), A.get(6)).contains(ctr.aMap.getV(B.get(8))));
		assertEquals(B.get(0), ctr.aMap.getU(A.get(2)));
		assertEquals(ObjectH.NULL, ctr.aMap.getV(ObjectH.NULL));
		
		MappingChecker chk = new MappingChecker();
		hA.findIsomorphicMappingTo(hC, chk);
		assertFalse(chk.mappingFound);
		hA.findIsomorphicMappingTo(hB, chk);
		assertTrue(chk.mappingFound);
	}

	@Test
	public void testAboutIsomorphism5() {
		ObjectH s1 = new ObjectH(cA, null);
		ObjectH s2 = new ObjectH(cB, null);
		ObjectH s3 = new ObjectH(cB, null);
		ObjectH s4 = new ObjectH(cNode, null);
		s1.setFieldValueMap(ImmutableMap.of(fAA, s2, fAB, s3, fAI, s4));
		s2.setFieldValueMap(ImmutableMap.of(fBL, s3));
		s3.setFieldValueMap(ImmutableMap.of(fBL, s2));
		s4.setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL, fValue, ovs[0]));
		
		ObjectH o1 = new ObjectH(cA, null);
		ObjectH o2 = new ObjectH(cA, null);
		ObjectH o3 = new ObjectH(cA, null);
		ObjectH o4 = new ObjectH(cB, null);
		ObjectH o5 = new ObjectH(cB, null);
		ObjectH o6 = new ObjectH(cNode, null);
		ObjectH o7 = new ObjectH(cNode, null);
		o1.setFieldValueMap(ImmutableMap.of(fAI, o1, fAA, o2, fAB, o3));
		o2.setFieldValueMap(ImmutableMap.of(fAI, o6, fAA, o4, fAB, o5));
		o3.setFieldValueMap(ImmutableMap.of(fAI, o7, fAA, o5, fAB, o4));
		o4.setFieldValueMap(ImmutableMap.of(fBL, o5));
		o5.setFieldValueMap(ImmutableMap.of(fBL, o4));
		o6.setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL, fValue, ovs[1]));
		o7.setFieldValueMap(ImmutableMap.of(fNext, o7, fValue, ovs[2]));
		
		SymbolicHeap HS = new SymbolicHeapAsDigraph(
				Arrays.asList(s1, ObjectH.NULL), null);
		SymbolicHeap HO = new SymbolicHeapAsDigraph(
				Arrays.asList(o1, o2, o3, ObjectH.NULL), null);
		MappingCounter ctr = new MappingCounter();
		
		HS.findEmbeddingInto(HO, ctr);
		assertEquals(1, ctr.mappingCounter);
		assertEquals(ovs[1], ctr.aMap.getV(ovs[0]));
		
		// DANGEROUS operation! set the field-value map of an in-heap object
		o7.setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL, fValue, ovs[2]));
		HS = new SymbolicHeapAsDigraph(Arrays.asList(s1, ObjectH.NULL), null);
		HO = new SymbolicHeapAsDigraph(Arrays.asList(o1, o2, o3, ObjectH.NULL), null);
		ctr = new MappingCounter();
		
		HS.findEmbeddingInto(HO, ctr);
		assertEquals(2, ctr.mappingCounter);
	}
	
	@Test
	public void testAboutSerialization1() throws Exception {
		Variable v1 = new IntVar();
		Variable v2 = new BoolVar();
		List<ObjectH> A = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(nodes, 0, 9)));
		A.add(ObjectH.NULL);
		A.get(0).setFieldValueMap(ImmutableMap.of(fNext, A.get(2)));
		A.get(1).setFieldValueMap(ImmutableMap.of(fNext, A.get(2)));
		A.get(2).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		A.get(3).setFieldValueMap(ImmutableMap.of(fNext, A.get(4)));
		A.get(4).setFieldValueMap(ImmutableMap.of(fNext, A.get(5)));
		A.get(5).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		A.get(6).setFieldValueMap(ImmutableMap.of(fNext, A.get(7)));
		A.get(7).setFieldValueMap(ImmutableMap.of(fNext, A.get(8)));
		A.get(8).setFieldValueMap(ImmutableMap.of(fNext, ObjectH.NULL));
		SymbolicHeap hA = new SymbolicHeapAsDigraph(A,
				new ExistExpr(Arrays.asList(v1, v2), new IntConst(404)));
		File tmpfile = new File("tmp/HeapTest.tmp");
		FileOutputStream fos = new FileOutputStream(tmpfile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(emp);
		oos.writeObject(hA);
		oos.close();
		fos.close();
		FileInputStream fis = new FileInputStream(tmpfile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		SymbolicHeap __emp = (SymbolicHeap) ois.readObject();
		assertEquals(emp.getAccessibleObjects(), __emp.getAccessibleObjects());
		assertEquals(emp.getAllObjects(), __emp.getAllObjects());
		assertEquals(emp.getConstraint().toSMTString(), __emp.getConstraint().toSMTString());
		assertEquals(emp.getVariables(), __emp.getVariables());
		assertEquals(emp.getFeatureCode(), __emp.getFeatureCode());
		SymbolicHeap __hA = (SymbolicHeap) ois.readObject();
		assertEquals(hA.getConstraint().toSMTString(), __hA.getConstraint().toSMTString());
		assertEquals(hA.getVariables(), __hA.getVariables());
		assertTrue(__hA.maybeIsomorphicWith(hA));
		assertTrue(__hA.findIsomorphicMappingTo(__hA, new MappingChecker()));
		ois.close();
		fis.close();
		tmpfile.delete();
	}
	
	@Test
	public void testAboutSerialization2() throws Exception {
		ObjectH o1 = new ObjectH(cNode, null);
		ObjectH o2 = new ObjectH(cNode, null);
		ObjectH o3 = new ObjectH(cNode, null);
		o1.setFieldValueMap(ImmutableMap.of(fNext, o2));
		o2.setFieldValueMap(ImmutableMap.of(fNext, o3));
		o3.setFieldValueMap(ImmutableMap.of(fNext, o1));
		SymbolicHeap h = new SymbolicHeapAsDigraph(Arrays.asList(o1, ObjectH.NULL), null);
		File tmpfile = new File("tmp/HeapTest.tmp");
		FileOutputStream fos = new FileOutputStream(tmpfile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(h);
		oos.close();
		fos.close();
		FileInputStream fis = new FileInputStream(tmpfile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		SymbolicHeap h2 = (SymbolicHeap) ois.readObject();
		ois.close();
		fis.close();
		tmpfile.delete();
		ObjectH p1 = h2.getAccessibleObjects()
				.stream().filter(o -> o != null).findAny().get();
		assertEquals(p1, p1.getFieldValue(fNext).getFieldValue(fNext).getFieldValue(fNext));
	}
	
}
