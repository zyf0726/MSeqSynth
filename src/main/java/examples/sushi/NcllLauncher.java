package examples.sushi;

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

public class NcllLauncher {

	private static final int scope$List			= 1;
	private static final int scopeForJBSE$Node	= 5;
	private static final int scopeForHeap$Node	= 8;
	private static final int maxSeqLength		= 14;
	private static final String hexFilePath 	= "HEXsettings/sushi/ncll-accurate.jbse";
	private static final String logFilePath 	= "tmp/sushi/ncll.txt";
	
	private static final Predicate<String> fieldFilter = (name ->
			(!name.startsWith("_") || name.equals("_owner"))
			&& !name.equals("modCount"));
	
	private static Class<?> cls$List;
	private static Class<?> cls$Node;
	
	private static TestGenerator testGenerator;
	
	private static void init() throws ClassNotFoundException {
		cls$List = Class.forName("examples.sushi.ncll.NodeCachingLinkedList");
		cls$Node = Class.forName("examples.sushi.ncll.NodeCachingLinkedList$LinkedListNode");
	}
	
	private static void configure(boolean showOnConsole) {
		JBSEParameters parms = JBSEParameters.I();
		parms.setTargetClassPath(TARGET_CLASS_PATH);
		parms.setTargetSourcePath(TARGET_SOURCE_PATH);
		parms.setShowOnConsole(showOnConsole);
		parms.setSettingsPath(hexFilePath);
		parms.setHeapScope(cls$List, scope$List);
		parms.setHeapScope(cls$Node, scopeForJBSE$Node);
	}
	
	private static List<Method> getMethods() throws NoSuchMethodException {
		List<Method> decMethods = Arrays.asList(cls$List.getDeclaredMethods());
		List<Method> pubMethods = Arrays.asList(cls$List.getMethods());
		List<Method> methods = decMethods.stream()
				.filter(m -> pubMethods.contains(m))
				.collect(Collectors.toList());
		System.out.println("public methods (sushi.NodeCachingLinkedList):");
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
		gb.setHeapScope(cls$List, scope$List);
		gb.setHeapScope(cls$Node, scopeForHeap$Node);
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
		gb.setHeapScope(cls$List, scope$List);
		gb.setHeapScope(cls$Node, scopeForHeap$Node);
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
		genTest1();
		genTest2();
		genTest3();
		genTest4();
		genTest(4, 2); 
		genTest(1, 6);
	}
	
	private static void genTest1() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH ncll = specFty.mkRefDecl(cls$List, "ncll");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		ObjectH cacheSize = specFty.mkVarDecl(SMTSort.INT, "cacheSize");
		specFty.addRefSpec("ncll", "header", "h1",
				"size", "size", "cacheSize", "cacheSize");
		specFty.addRefSpec("h1", "next", "h2", "value", "null");
		specFty.addRefSpec("h2", "next", "h3", "value", "v");
		specFty.addRefSpec("h3", "next", "h4", "value", "v");
		specFty.addRefSpec("h4", "next", "h1", "value", "v");
		specFty.setAccessible("ncll");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, ncll, size, cacheSize);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest1: " + (end - start) + "ms\n");
	}
	
	private static void genTest2() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH ncll = specFty.mkRefDecl(cls$List, "ncll");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		ObjectH cacheSize = specFty.mkVarDecl(SMTSort.INT, "cacheSize");
		specFty.addRefSpec("ncll", "firstCachedNode", "c1",
				"size", "size", "cacheSize", "cacheSize");
		specFty.addRefSpec("c1", "next", "c2");
		specFty.addRefSpec("c2", "next", "c3");
		specFty.addRefSpec("c3", "next", "c4");
		specFty.addRefSpec("c4", "next", "c5");
		specFty.addRefSpec("c5", "next", "null");
		specFty.setAccessible("ncll");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, ncll, size, cacheSize);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest2: " + (end - start) + "ms\n");
	}
	
	private static void genTest3() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH ncll = specFty.mkRefDecl(cls$List, "ncll");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		ObjectH cacheSize = specFty.mkVarDecl(SMTSort.INT, "cacheSize");
		specFty.addRefSpec("ncll", "firstCachedNode", "c1", "header", "o1",
				"size", "size", "cacheSize", "cacheSize");
		specFty.addRefSpec("c1", "next", "c2");
		specFty.addRefSpec("c2", "next", "c3");
		specFty.addRefSpec("c3", "next", "null");
		specFty.addRefSpec("o1", "next", "o2");
		specFty.addRefSpec("o2", "next", "o1");
		specFty.setAccessible("ncll");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, ncll, size, cacheSize);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest3: " + (end - start) + "ms\n");
	}
	
	private static void genTest4() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH ncll = specFty.mkRefDecl(cls$List, "ncll");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		ObjectH cacheSize = specFty.mkVarDecl(SMTSort.INT, "cacheSize");
		specFty.addRefSpec("ncll", "firstCachedNode", "c1", "header", "o1",
				"size", "size", "cacheSize", "cacheSize");
		specFty.addRefSpec("c1", "next", "c2");
		specFty.addRefSpec("c2", "next", "null");
		specFty.addRefSpec("o1", "next", "o2");
		specFty.addRefSpec("o2", "next", "o3");
		specFty.addRefSpec("o3", "next", "o1");
		specFty.setAccessible("ncll");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, ncll, size, cacheSize);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest4: " + (end - start) + "ms\n");
	}
	
	private static void genTest(int expcSize, int expcCacheSize) {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH ncll = specFty.mkRefDecl(cls$List, "ncll");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		ObjectH cacheSize = specFty.mkVarDecl(SMTSort.INT, "cacheSize");
		specFty.addRefSpec("ncll", "size", "size", "cacheSize", "cacheSize");
		specFty.setAccessible("ncll");
		specFty.addVarSpec("(= size " + expcSize + ")");
		specFty.addVarSpec("(= cacheSize " + expcCacheSize + ")");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, ncll, size, cacheSize);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.print(">> genTest(" + expcSize + ", " + expcCacheSize + "): ");
		System.out.println((end - start) + "ms\n");
	}

}
