package mseqsynth.util;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;

public class BijectionTest {

	@Test
	public void testBijection1() {
		final int numObj = 15;
		Object[] objUs = new Object[numObj];
		Object[] objVs = new Object[numObj];
		for (int i = 0; i < numObj; ++i) {
			objUs[i] = new Object();
			objVs[i] = new Object();
		}
		
		Bijection<Object, Object> biject1 = new Bijection<>();		
		for (int i = 0; i < numObj; ++i) {
			assertFalse(biject1.containsU(objUs[i]));
			assertFalse(biject1.containsV(objVs[i]));
			assertNull(biject1.getV(objUs[i]));
			assertNull(biject1.getU(objVs[i]));
		}
		
		for (int i = 0; i < numObj; i += 3)
			assertTrue(biject1.putUV(objUs[i], objVs[i]));
		Bijection<Object, Object> biject2 = new Bijection<>(biject1);
		for (int i = 0; i < numObj; ++i) {
			assertEquals(biject1.containsU(objUs[i]), biject2.containsU(objUs[i]));
			assertEquals(biject1.containsV(objVs[i]), biject2.containsV(objVs[i]));
			assertEquals(biject1.getMapU2V().get(objUs[i]), biject2.getV(objUs[i]));
			assertEquals(biject1.getU(objVs[i]), biject2.getMapV2U().get(objVs[i]));
		}
		
		for (int i = 1; i < numObj; i += 3)
			assertTrue(biject2.putUV(objUs[i], objVs[i]));
		for (int i = 1; i < numObj; i += 3) {
			assertFalse(biject1.containsU(objUs[i]));
			assertNull(biject1.getU(objVs[i]));
			assertTrue(biject2.getMapV2U().containsKey(objVs[i]));
			assertEquals(objVs[i], biject2.getMapU2V().get(objUs[i]));
		}
		assertEquals(numObj / 3, biject1.size());
		assertEquals(2 * numObj / 3, biject2.size());
		
		for (int i = 2; i < numObj; i += 3)
			assertTrue(biject1.putUV(objUs[i], objVs[i]));
		for (int i = 2; i < numObj; i += 3) {
			assertTrue(biject1.getMapU2V().containsKey(objUs[i]));
			assertEquals(objUs[i], biject1.getU(objVs[i]));
			assertFalse(biject2.containsV(objVs[i]));
			assertNull(biject2.getMapV2U().get(objUs[i]));
		}
		
		biject1.clear();
		assertEquals(Collections.emptyMap(), biject1.getMapV2U());
		assertEquals(0, biject1.size());
	}
	
	@Test
	public void testBijection2() {
		Object objA = new Object();
		Object objB = new Object();
		Object objC = new Object();
		Object objD = new Object();
		Bijection<Object, Object> B = new Bijection<>();
		
		assertTrue(B.putUV(objA, objA));
		assertTrue(B.putUV(objB, objC));
		assertEquals(objA, B.getV(objA));
		assertEquals(objA, B.getU(objA));
		assertEquals(objB, B.getU(objC));
		assertNull(B.getV(objC));
		assertEquals(2, B.size());
		
		assertFalse(B.putUV(objA, objD));
		assertFalse(B.putUV(objD, objC));
		assertFalse(B.containsU(objD));
		assertFalse(B.containsV(objD));
		assertEquals(2, B.size());
		
		assertTrue(B.putUV(objC, objD));
		assertEquals(objD, B.getV(objC));
		assertTrue(B.containsV(objD));
		assertTrue(B.putUV(objC, objD));
		assertEquals(3, B.size());
		
		assertFalse(B.containsU(null));
		assertFalse(B.containsV(null));
		assertNull(B.getU(null));
		assertNull(B.getV(null));
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testBijectionExc1() {
		Bijection<Object, Object> B = new Bijection<>();
		B.getMapU2V().put(new Object(), new Object());
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testBijectionExc2() {
		Bijection<Object, Object> B = new Bijection<>();
		B.getMapV2U().put(new Object(), new Object());
	}
	
	@Test(expected = NullPointerException.class)
	public void testBijectionExc3() {
		Bijection<Object, Object> B = new Bijection<>();
		B.putUV(null, new Object());
	}
	
	@Test(expected = NullPointerException.class)
	public void testBijectionExc4() {
		Bijection<Object, Object> B = new Bijection<>();
		B.putUV(new Object(), null);
	}

}
