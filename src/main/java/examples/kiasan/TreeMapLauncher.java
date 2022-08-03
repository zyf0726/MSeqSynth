package examples.kiasan;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import mseqsynth.algo.DynamicGraphBuilder;
import mseqsynth.algo.Statement;
import mseqsynth.algo.StaticGraphBuilder;
import mseqsynth.algo.TestGenerator;
import mseqsynth.algo.WrappedHeap;
import mseqsynth.common.settings.JBSEParameters;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.SMTSort;
import mseqsynth.wrapper.symbolic.SpecFactory;
import mseqsynth.wrapper.symbolic.Specification;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;
import mseqsynth.wrapper.symbolic.SymbolicExecutorWithCachedJBSE;

import static examples.common.Settings.*;

public class TreeMapLauncher {
	
	private static final int scope$TreeMap		= 1;
	private static final int scopeForJBSE$Entry = 5;
	private static final int scopeForHeap$Entry = 6;
	private static final int maxSeqLength		= 10;
	private static final String hexFilePath		= "HEXsettings/kiasan/treemap.jbse";
	private static final String logFilePath 	= "tmp/kiasan/treemap.txt";
	
	private static Predicate<String> fieldFilter = (name -> !name.equals("modCount"));
	
	private static Class<?> cls$TreeMap;
	private static Class<?> cls$Entry;
	
	private static TestGenerator testGenerator;
	
	private static void init() throws ClassNotFoundException {
		cls$TreeMap = Class.forName("examples.kiasan.redblacktree.TreeMap");
		cls$Entry = Class.forName("examples.kiasan.redblacktree.TreeMap$Entry");
	}
	
	private static void configure(boolean showOnConsole) {
		JBSEParameters parms = JBSEParameters.I();
		parms.setTargetClassPath(TARGET_CLASS_PATH);
		parms.setTargetSourcePath(TARGET_SOURCE_PATH);
		parms.setShowOnConsole(showOnConsole);
		parms.setSettingsPath(hexFilePath);
		parms.setHeapScope(cls$TreeMap, scope$TreeMap);
		parms.setHeapScope(cls$Entry, scopeForJBSE$Entry);
	}
	
	private static List<Method> getMethods() throws NoSuchMethodException {
		List<Method> decMethods = Arrays.asList(cls$TreeMap.getDeclaredMethods());
		List<Method> pubMethods = Arrays.asList(cls$TreeMap.getMethods());
		List<Method> methods = decMethods.stream()
				.filter(m -> pubMethods.contains(m))
				.collect(Collectors.toList());
		System.out.println("public methods (kiasan.TreeMap):");
		for (Method m : methods) {
			System.out.print("  " + m.getName() + "(");
			AnnotatedType[] paraTypes = m.getAnnotatedParameterTypes();
			for (int i = 0; i < paraTypes.length; ++i) {
				System.out.print(paraTypes[i].getType().getTypeName());
				if (i < paraTypes.length - 1)
					System.out.print(", ");
			}
			System.out.println(")");
		}
		return methods;
	}
	
	private static void buildGraphStatic(Collection<Method> methods, boolean simplify)
			throws FileNotFoundException {
		long start = System.currentTimeMillis();
		SymbolicExecutor executor = new SymbolicExecutorWithCachedJBSE(fieldFilter);
		StaticGraphBuilder gb = new StaticGraphBuilder(executor, methods);
		gb.setHeapScope(cls$TreeMap, scope$TreeMap);
		gb.setHeapScope(cls$Entry, scopeForHeap$Entry);
		SymbolicHeap initHeap = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
		List<WrappedHeap> heaps = gb.buildGraph(initHeap, simplify);
		StaticGraphBuilder.__debugPrintOut(heaps, executor, new PrintStream(logFilePath));
		testGenerator = new TestGenerator(heaps);
		long end = System.currentTimeMillis();
		System.out.println(">> buildGraph (static): " + (end - start) + "ms\n");
	}

	private static void buildGraphDynamic(Collection<Method> methods)
			throws FileNotFoundException {
		long start = System.currentTimeMillis();
		SymbolicExecutor executor = new SymbolicExecutorWithCachedJBSE(fieldFilter);
		DynamicGraphBuilder gb = new DynamicGraphBuilder(executor, methods);
		gb.setHeapScope(cls$TreeMap, scope$TreeMap);
		gb.setHeapScope(cls$Entry, scopeForHeap$Entry);
		SymbolicHeap initHeap = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
		List<WrappedHeap> heaps = gb.buildGraph(initHeap, maxSeqLength);
		DynamicGraphBuilder.__debugPrintOut(heaps, executor, new PrintStream(logFilePath));
		testGenerator = new TestGenerator(heaps);
		long end = System.currentTimeMillis();
		System.out.println(">> buildGraph (dynamic): " + (end - start) + "ms\n");
	}
	
	public static void main(String[] args) throws Exception {
		final boolean showOnConsole = true;
		final boolean simplify = true;
		final boolean useDynamicAlgorithm = true;
		init();
		configure(showOnConsole);
		List<Method> methods = getMethods();
		if (useDynamicAlgorithm) {
			buildGraphDynamic(methods);
		} else {
			buildGraphStatic(methods, simplify);
		}
		genTest0();
		genTest4$1();
		genTest4$2();
		genTest5$1();
		genTest6$1();
		genTest6$2();
	}
	
	private static void genTest0() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH treemap = specFty.mkRefDecl(cls$TreeMap, "t");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		specFty.addRefSpec("t", "root", "null", "size", "size");
		specFty.addVarSpec("(>= size 0)");
		specFty.setAccessible("t");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, treemap, size);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest0: " + (end - start) + "ms\n");
	}
	
	private static void genTest4$1() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH treemap = specFty.mkRefDecl(cls$TreeMap, "t");
		ObjectH v1 = specFty.mkRefDecl(Object.class, "v1");
		ObjectH v2 = specFty.mkRefDecl(Object.class, "v2");
		ObjectH v3 = specFty.mkRefDecl(Object.class, "v3");
		specFty.addRefSpec("t", "root", "o1");
		specFty.addRefSpec("o1", "parent", "null", "left", "o2", "right", "o3", "value", "v1");
		specFty.addRefSpec("o2", "parent", "o1", "left", "null", "right", "o4", "value", "null");
		specFty.addRefSpec("o4", "parent", "o2", "left", "null", "right", "null", "value", "v1");
		specFty.addRefSpec("o3", "parent", "o1", "value", "v2");
		specFty.setAccessible("t", "v1", "v2", "v3");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, treemap, v1, v2, v3);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest4$1: " + (end - start) + "ms\n");
	}
	
	private static void genTest4$2() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH treemap = specFty.mkRefDecl(cls$TreeMap, "t");
		ObjectH v1 = specFty.mkRefDecl(Object.class, "v1");
		ObjectH v2 = specFty.mkRefDecl(Object.class, "v2");
		specFty.addRefSpec("t", "root", "o1");
		specFty.addRefSpec("o1", "parent", "null", "left", "o2", "right", "o3", "value", "null");
		specFty.addRefSpec("o2", "parent", "o1", "left", "null", "right", "null", "value", "null");
		specFty.addRefSpec("o3", "parent", "o1", "left", "null", "right", "o4", "value", "v1");
		specFty.addRefSpec("o4", "parent", "o3", "value", "v2");
		specFty.setAccessible("t", "v2");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, treemap, v1, v2);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest4$2: " + (end - start) + "ms\n");
	}
	
	private static void genTest5$1() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH treemap = specFty.mkRefDecl(cls$TreeMap, "t");
		ObjectH v1 = specFty.mkRefDecl(Object.class, "v1");
		ObjectH v3 = specFty.mkRefDecl(Object.class, "v3");
		specFty.addRefSpec("t", "root", "o1");
		specFty.addRefSpec("o1", "parent", "null", "left", "o2", "right", "o3", "value", "v1");
		specFty.addRefSpec("o2", "left", "o4", "right", "o5", "value", "null");
		specFty.addRefSpec("o4", "parent", "o2", "left", "null", "right", "null", "value", "v2");
		specFty.addRefSpec("o5", "parent", "o2", "value", "v1");
		specFty.addRefSpec("o3", "left", "null", "right", "null", "value", "v2");
		specFty.setAccessible("t", "v1", "v3");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, treemap, v1, v3);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest5$1: " + (end - start) + "ms\n");
	}
	
	private static void genTest6$1() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH treemap = specFty.mkRefDecl(cls$TreeMap, "t");
		specFty.addRefSpec("t", "root", "o1");
		specFty.addRefSpec("o1", "left", "o2", "right", "o4", "color", "b1");
		specFty.addVarSpec("(= b1 true)");   // BLACK
		specFty.addRefSpec("o2", "left", "o3", "color", "b2");
		specFty.addVarSpec("(= b2 true)");   // BLACK
		specFty.addRefSpec("o3", "color", "b3");
		specFty.addVarSpec("(= b3 false)");  // RED
		specFty.addRefSpec("o4", "color", "b4");
		specFty.addVarSpec("(= b4 false)");  // RED
		specFty.setAccessible("t");
		Specification spec = specFty.genSpec();
		// +50 +30 +70 +80 +60 +90 -90 +20
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, treemap);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest6$1: " + (end - start) + "ms\n");
	}

	private static void genTest6$2() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH treemap = specFty.mkRefDecl(cls$TreeMap, "t");
		ObjectH v0 = specFty.mkRefDecl(Object.class, "v0");
		specFty.addRefSpec("t", "root", "o1");
		specFty.addRefSpec("o1", "left", "o2", "right", "o3");
		specFty.addRefSpec("o2", "left", "o4", "right", "o5", "color", "b2");
		specFty.addVarSpec("(= b2 false)");  // RED
		specFty.addRefSpec("o3", "left", "null", "right", "null", "color", "b3");
		specFty.addVarSpec("(= b3 true)");   // BLACK
		specFty.addRefSpec("o4", "left", "null", "right", "null");
		specFty.addRefSpec("o5", "left", "null", "right", "null");
		specFty.setAccessible("t", "v0");
		Specification spec = specFty.genSpec();
		// +50 +30 +70 +80 -80 +10 +40 +20 -20
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, treemap, v0);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest6$2: " + (end - start) + "ms\n");
	}

}
