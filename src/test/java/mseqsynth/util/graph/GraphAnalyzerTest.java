package mseqsynth.util.graph;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class GraphAnalyzerTest {
	
	private GraphAnalyzer<Integer, Integer>
		gaEmp, gaTour, gaNoEdge;
	private GraphAnalyzer<Integer, String>
		gaRand1, gaRand2;
	private GraphAnalyzer<String, String>
		ga1, ga2, ga3;

	@Before
	public void setUp() throws Exception {
		gaEmp = new GraphAnalyzer<>(null, null);
		
		ArrayList<Integer> nodes = new ArrayList<>();
		ArrayList<Edge<Integer, Integer>> edges = new ArrayList<>();		
		for (int uid = 0; uid < 12; ++uid)
			nodes.add(uid);
		for (int uid = 0; uid < 12; ++uid)
			for (int vid = uid + 1; vid < 12; ++vid) {
				edges.add(new Edge<Integer, Integer>(uid, vid, vid));
			}
		gaTour = new GraphAnalyzer<>(nodes, edges);
		gaNoEdge = new GraphAnalyzer<>(nodes, null);
		
		gaRand1 = new GraphAnalyzer<>(
			Arrays.asList(0, 1, 2, 3, 4, 5),
			Arrays.asList(
				new Edge<>(4, 0, "e1"),
				new Edge<>(1, 1, "e2"),
				new Edge<>(1, 2, "e3"),
				new Edge<>(4, 2, "e4"),
				new Edge<>(3, 1, "e5"),
				new Edge<>(5, 3, "e6"),
				new Edge<>(0, 5, "e7"),
				new Edge<>(3, 4, "e8")
			)
		);
		
		gaRand2 = new GraphAnalyzer<>(
			Arrays.asList(1, 2, 3, 4, 5, 6),
			Arrays.asList(
				new Edge<>(1, 2, "e1"),
				new Edge<>(2, 4, "e2"),
				new Edge<>(2, 6, "e3"),
				new Edge<>(6, 2, "e4"),
				new Edge<>(6, 6, "e5"),
				new Edge<>(5, 6, "e6"),
				new Edge<>(6, 3, "e7"),
				new Edge<>(4, 4, "e8")
			)
		);
		
		ga1 = new GraphAnalyzer<>(
			Arrays.asList("o1", "o2", "o3", "o4", "o5", "o6", "o7", "o8", "o9", "null"),
			Arrays.asList(
				new Edge<>("o1", "o3", ".A"),
				new Edge<>("o2", "o3", ".A"),
				new Edge<>("o3", "null", ".A"),
				new Edge<>("o4", "o5", ".A"),
				new Edge<>("o5", "o6", ".A"),
				new Edge<>("o6", "null", ".A"),
				new Edge<>("o7", "o8", ".A"),
				new Edge<>("o8", "o9", ".A"),
				new Edge<>("o9", "null", ".A")
			)
		);
		
		ga2 = new GraphAnalyzer<>(
			Arrays.asList("s1", "s2", "s3", "s4", "s5", "s7", "s8", "s9", "s6", "s3", "null"),
			Arrays.asList(
				new Edge<>("s3", "s1", ".A"),
				new Edge<>("s4", "s1", ".A"),
				new Edge<>("s5", "s2", ".A"),
				new Edge<>("s2", "s8", ".A"),
				new Edge<>("s9", "s7", ".A"),
				new Edge<>("s7", "s6", ".A"),
				new Edge<>("s1", "null", ".A"),
				new Edge<>("s8", "null", ".A"),
				new Edge<>("s6", "null", ".A")
			)
		);
		
		ga3 = new GraphAnalyzer<>(
			Arrays.asList("o1", "o2", "o3", "o4", "o5", "o6", "o7", "o8", "o9", "null"),
			Arrays.asList(
				new Edge<>("o1", "o3", ".A"),
				new Edge<>("o3", "null", ".A"),
				new Edge<>("o2", "o4", ".A"),
				new Edge<>("o4", "null", ".A"),
				new Edge<>("o5", "o6", ".A"),
				new Edge<>("o6", "o9", ".A"),
				new Edge<>("o7", "o8", ".A"),
				new Edge<>("o8", "o9", ".A"),
				new Edge<>("o9", "null", ".A")
			)
		);
	}
	
	@Test(expected = NullPointerException.class)
	public void testConstructor1() {
		new GraphAnalyzer<Integer, Integer>(
			null,
			Arrays.asList(new Edge<>(0, 0, -1))
		);
	}
	
	@Test(expected = NullPointerException.class)
	public void testConstructor2() {
		new GraphAnalyzer<Integer, String>(
			Arrays.asList(1, 2, 3),
			Arrays.asList(new Edge<>(1, 4, ".L"))
		);
	}
	
	@Test(expected = NullPointerException.class)
	public void testConstructor3() {
		new GraphAnalyzer<Integer, String>(
			Arrays.asList(1),
			Arrays.asList(new Edge<>(1, 1, null))
		);
	}
	
	@Test
	public void testGetFeatureNodewise() {
		assertNull(gaEmp.getFeatureNodewise(0));
		assertEquals(ga1.getFeatureNodewise("o1"), ga1.getFeatureNodewise("o2"));
		assertEquals(ga1.getFeatureNodewise("o4"), ga1.getFeatureNodewise("o7"));
		assertNotEquals(ga1.getFeatureNodewise("o1"), ga2.getFeatureNodewise("s1"));
		assertEquals(gaRand1.getFeatureNodewise(1), gaRand1.getFeatureNodewise(1));
		assertNotEquals(gaRand1.getFeatureNodewise(1), gaRand2.getFeatureNodewise(2));
		/* test in-degree */
		assertEquals(3, ga2.getFeatureNodewise("null").getInDegree());
		assertEquals(2, ga1.getFeatureNodewise("o3").getInDegree());
		assertEquals(2, ga3.getFeatureNodewise("o9").getInDegree());
		assertEquals(11, gaTour.getFeatureNodewise(11).getInDegree());
		assertEquals(7, gaTour.getFeatureNodewise(7).getInDegree());
		assertEquals(0, gaNoEdge.getFeatureNodewise(7).getInDegree());
		/* test SCC size */
		assertEquals(4, gaRand1.getFeatureNodewise(0).getSizeSCC());
		assertEquals(2, gaRand2.getFeatureNodewise(6).getSizeSCC());
		assertEquals(1, gaTour.getFeatureNodewise(3).getSizeSCC());
	}

	@Test
	public void testGetFeatureGraphwise() {
		assertEquals(gaEmp.getFeatureGraphwise(), gaEmp.getFeatureGraphwise());
		assertNotEquals(gaEmp.getFeatureGraphwise(), gaTour.getFeatureGraphwise());
		assertEquals(ga1.getFeatureGraphwise(), ga2.getFeatureGraphwise());
		assertNotEquals(gaRand1.getFeatureGraphwise(), gaRand2.getFeatureGraphwise());
		// assertNotEquals(ga1.getFeatureGraphwise(), ga3.getFeatureGraphwise());
	}

	@Test
	public void testComputeNumberNodes() {
		assertEquals(0, gaEmp.computeNumberNodes());
		assertEquals(12, gaNoEdge.computeNumberNodes());
		assertEquals(6, gaRand1.getFeatureGraphwise().getNumberNodes());
		assertEquals(10, ga2.computeNumberNodes());
	}
	
	@Test
	public void testComputeSizeSCCsRepr() {
		assertEquals("", gaEmp.computeSizeSCCsRepr());
		assertEquals("1.0.0.0.0.0.0.0.0.0.", ga3.computeSizeSCCsRepr());
		assertEquals("1.0.3.", gaRand1.computeSizeSCCsRepr());
		assertEquals("1.0.0.0.1.", gaRand2.getFeatureGraphwise().getSizeSCCsRepr());
	}
	
	@Test
	public void testAboutTarjanAlgorithm1() {
		assertEquals(null, gaRand1.getSCCIdentifier(6));
		assertEquals(null, gaRand2.getSCCMembers(0));
		
		assertEquals(Arrays.asList(0, 3, 4, 5),
				gaRand1.getSCCMembers(3).stream().sorted().collect(Collectors.toList()));
		assertEquals(Arrays.asList(1),
				gaRand1.getSCCMembers(1).stream().sorted().collect(Collectors.toList()));
		assertEquals(Arrays.asList(2, 6),
				gaRand2.getSCCMembers(2).stream().sorted().collect(Collectors.toList()));
		assertEquals(Arrays.asList(4),
				gaRand2.getSCCMembers(4).stream().sorted().collect(Collectors.toList()));
		assertTrue(gaRand1.getSCCIdentifier(5) == gaRand1.getSCCIdentifier(0));
		assertTrue(gaRand1.getSCCIdentifier(3) == gaRand1.getSCCIdentifier(4));
		assertTrue(gaRand2.getSCCIdentifier(2) == gaRand2.getSCCIdentifier(6));
		
		// check SCCs are extracted in reversed topological order
		assertTrue(gaRand1.getSCCIdentifier(2) < gaRand1.getSCCIdentifier(1));
		assertTrue(gaRand1.getSCCIdentifier(1) < gaRand1.getSCCIdentifier(3));
		assertTrue(gaRand2.getSCCIdentifier(3) < gaRand2.getSCCIdentifier(6));
		assertTrue(gaRand2.getSCCIdentifier(4) < gaRand2.getSCCIdentifier(2));
		assertTrue(gaRand2.getSCCIdentifier(6) < gaRand2.getSCCIdentifier(5));
		assertTrue(gaRand2.getSCCIdentifier(2) < gaRand2.getSCCIdentifier(1));
	}
	
	@Test
	public void testAboutTarjanAlgorithm2() {
		List<Edge<Integer, Integer>> es = new ArrayList<>();
		es.add(new Edge<>(0, 1, 0));
		es.add(new Edge<>(8, 1, 1));
		es.add(new Edge<>(15, 1, 2));
		es.add(new Edge<>(22, 1, 3));
		es.add(new Edge<>(35, 1, 4));
		es.add(new Edge<>(53, 1, 5));
		es.add(new Edge<>(72, 1, 6));
		es.add(new Edge<>(90, 1, 7));
		es.add(new Edge<>(109, 1, 8));
		es.add(new Edge<>(1, 3, 9));
		es.add(new Edge<>(7, 3, 10));
		es.add(new Edge<>(3, 5, 11));
		es.add(new Edge<>(10, 5, 12));
		es.add(new Edge<>(13, 5, 13));
		es.add(new Edge<>(3, 6, 14));
		es.add(new Edge<>(20, 6, 15));
		es.add(new Edge<>(21, 6, 16));
		es.add(new Edge<>(3, 7, 17));
		es.add(new Edge<>(3, 8, 18));
		es.add(new Edge<>(5, 10, 19));
		es.add(new Edge<>(5, 11, 20));
		es.add(new Edge<>(12, 11, 21));
		es.add(new Edge<>(14, 11, 22));
		es.add(new Edge<>(17, 11, 23));
		es.add(new Edge<>(18, 11, 24));
		es.add(new Edge<>(19, 11, 25));
		es.add(new Edge<>(26, 11, 26));
		es.add(new Edge<>(27, 11, 27));
		es.add(new Edge<>(29, 11, 28));
		es.add(new Edge<>(31, 11, 29));
		es.add(new Edge<>(33, 11, 30));
		es.add(new Edge<>(5, 12, 31));
		es.add(new Edge<>(5, 13, 32));
		es.add(new Edge<>(5, 14, 33));
		es.add(new Edge<>(5, 15, 34));
		es.add(new Edge<>(6, 17, 35));
		es.add(new Edge<>(6, 18, 36));
		es.add(new Edge<>(6, 19, 37));
		es.add(new Edge<>(6, 20, 38));
		es.add(new Edge<>(6, 21, 39));
		es.add(new Edge<>(6, 22, 40));
		es.add(new Edge<>(11, 24, 41));
		es.add(new Edge<>(28, 24, 42));
		es.add(new Edge<>(39, 24, 43));
		es.add(new Edge<>(43, 24, 44));
		es.add(new Edge<>(44, 24, 45));
		es.add(new Edge<>(46, 24, 46));
		es.add(new Edge<>(47, 24, 47));
		es.add(new Edge<>(48, 24, 48));
		es.add(new Edge<>(49, 24, 49));
		es.add(new Edge<>(52, 24, 50));
		es.add(new Edge<>(11, 25, 51));
		es.add(new Edge<>(58, 25, 52));
		es.add(new Edge<>(62, 25, 53));
		es.add(new Edge<>(63, 25, 54));
		es.add(new Edge<>(65, 25, 55));
		es.add(new Edge<>(66, 25, 56));
		es.add(new Edge<>(68, 25, 57));
		es.add(new Edge<>(70, 25, 58));
		es.add(new Edge<>(11, 26, 59));
		es.add(new Edge<>(11, 27, 60));
		es.add(new Edge<>(11, 28, 61));
		es.add(new Edge<>(11, 29, 62));
		es.add(new Edge<>(11, 30, 63));
		es.add(new Edge<>(32, 30, 64));
		es.add(new Edge<>(74, 30, 65));
		es.add(new Edge<>(76, 30, 66));
		es.add(new Edge<>(77, 30, 67));
		es.add(new Edge<>(79, 30, 68));
		es.add(new Edge<>(83, 30, 69));
		es.add(new Edge<>(84, 30, 70));
		es.add(new Edge<>(85, 30, 71));
		es.add(new Edge<>(86, 30, 72));
		es.add(new Edge<>(11, 31, 73));
		es.add(new Edge<>(11, 32, 74));
		es.add(new Edge<>(11, 33, 75));
		es.add(new Edge<>(11, 34, 76));
		es.add(new Edge<>(95, 34, 77));
		es.add(new Edge<>(96, 34, 78));
		es.add(new Edge<>(99, 34, 79));
		es.add(new Edge<>(102, 34, 80));
		es.add(new Edge<>(104, 34, 81));
		es.add(new Edge<>(105, 34, 82));
		es.add(new Edge<>(106, 34, 83));
		es.add(new Edge<>(11, 35, 84));
		es.add(new Edge<>(24, 39, 85));
		es.add(new Edge<>(24, 43, 86));
		es.add(new Edge<>(24, 44, 87));
		es.add(new Edge<>(24, 46, 88));
		es.add(new Edge<>(24, 47, 89));
		es.add(new Edge<>(24, 48, 90));
		es.add(new Edge<>(24, 49, 91));
		es.add(new Edge<>(24, 52, 92));
		es.add(new Edge<>(24, 53, 93));
		es.add(new Edge<>(25, 58, 94));
		es.add(new Edge<>(25, 62, 95));
		es.add(new Edge<>(25, 63, 96));
		es.add(new Edge<>(25, 65, 97));
		es.add(new Edge<>(25, 66, 98));
		es.add(new Edge<>(25, 68, 99));
		es.add(new Edge<>(25, 70, 100));
		es.add(new Edge<>(25, 72, 101));
		es.add(new Edge<>(30, 74, 102));
		es.add(new Edge<>(30, 76, 103));
		es.add(new Edge<>(30, 77, 104));
		es.add(new Edge<>(30, 79, 105));
		es.add(new Edge<>(30, 83, 106));
		es.add(new Edge<>(30, 84, 107));
		es.add(new Edge<>(30, 85, 108));
		es.add(new Edge<>(30, 86, 109));
		es.add(new Edge<>(30, 90, 110));
		es.add(new Edge<>(34, 95, 111));
		es.add(new Edge<>(34, 96, 112));
		es.add(new Edge<>(34, 99, 113));
		es.add(new Edge<>(34, 102, 114));
		es.add(new Edge<>(34, 104, 115));
		es.add(new Edge<>(34, 105, 116));
		es.add(new Edge<>(34, 106, 117));
		es.add(new Edge<>(34, 109, 118));
		Set<Integer> vs = new HashSet<>();
		for (Edge<Integer, Integer> e : es) {
			vs.add(e.head);
			vs.add(e.tail);
		}
		GraphAnalyzer<Integer, Integer> ga = new GraphAnalyzer<>(vs, es);
		for (Integer v : vs) {
			if (v != 0) {
				assertEquals(Integer.valueOf(0), ga.getSCCIdentifier(v));
			} else {
				assertEquals(Integer.valueOf(1), ga.getSCCIdentifier(v));
			}
		}
	}

}
