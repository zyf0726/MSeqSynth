package examples.kiasan;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

public class LeftistLauncher {
	
	private static final int scope$Heap			= 2;
	private static final int scopeForJBSE$Node	= 5;
	private static final int scopeForHeap$Node	= 7;
	private static final int maxSeqLength		= 10;
	private static final String hexFilePath	= "HEXsettings/kiasan/leftist.jbse";
	private static final String logFilePath = "tmp/kiasan/leftist.txt";
	
	private static Class<?> cls$Heap;
	private static Class<?> cls$Node;
	
	private static TestGenerator testGenerator;
	
	private static void init() throws ClassNotFoundException {
		cls$Heap = Class.forName("examples.kiasan.leftistheap.LeftistHeap");
		cls$Node = Class.forName("examples.kiasan.leftistheap.LeftistHeap$LeftistNode");
	}
	
	private static void configure(boolean showOnConsole) {
		JBSEParameters parms = JBSEParameters.I();
		parms.setTargetClassPath(TARGET_CLASS_PATH);
		parms.setTargetSourcePath(TARGET_SOURCE_PATH);
		parms.setShowOnConsole(showOnConsole);
		parms.setSettingsPath(hexFilePath);
		parms.setHeapScope(cls$Heap, scope$Heap);
		parms.setHeapScope(cls$Node, scopeForJBSE$Node);
	}
	
	private static List<Method> getMethods() throws NoSuchMethodException {
		List<Method> decMethods = Arrays.asList(cls$Heap.getDeclaredMethods());
		List<Method> pubMethods = Arrays.asList(cls$Heap.getMethods());
		List<Method> methods = decMethods.stream()
				.filter(m -> pubMethods.contains(m))
				.collect(Collectors.toList());
		System.out.println("public methods (kiasan.LeftistHeap):");
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
		SymbolicExecutor executor = new SymbolicExecutorWithCachedJBSE();
		StaticGraphBuilder gb = new StaticGraphBuilder(executor, methods);
		gb.setHeapScope(cls$Heap, scope$Heap);
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
		SymbolicExecutor executor = new SymbolicExecutorWithCachedJBSE();
		DynamicGraphBuilder gb = new DynamicGraphBuilder(executor, methods);
		gb.setHeapScope(cls$Heap, scope$Heap);
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
		genTest0();
		genTest6();
		genTest3$3();
		genTest5$2();
	}
	
	private static void genTest0() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH heap = specFty.mkRefDecl(cls$Heap, "h");
		specFty.addRefSpec("h", "root", "null");
		specFty.setAccessible("h");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, heap);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest0: " + (end - start) + "ms\n");
	}
	
	private static void genTest6() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH heap = specFty.mkRefDecl(cls$Heap, "h");
		ObjectH npl = specFty.mkVarDecl(SMTSort.INT, "npl");
		specFty.addRefSpec("h", "root", "o0");
		specFty.addRefSpec("o0", "left", "o1", "right", "o2", "npl", "npl");
		specFty.addRefSpec("o1", "left", "o3", "right", "o4");
		specFty.addRefSpec("o3", "left", "o5", "right", "null");
		specFty.addRefSpec("o2", "left", "null", "right", "null");
		specFty.addRefSpec("o4", "element", "v2");
		specFty.addRefSpec("o5", "element", "v5");
		specFty.addVarSpec("(= v2 v5)");
		specFty.setAccessible("h");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, heap, npl);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest6: " + (end - start) + "ms\n");
	}
	
	private static void genTest3$3() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH h1 = specFty.mkRefDecl(cls$Heap, "h1");
		ObjectH h2 = specFty.mkRefDecl(cls$Heap, "h2");
		specFty.addRefSpec("h1", "root", "o0");
		specFty.addRefSpec("o0", "left", "o1", "right", "o2");
		specFty.addRefSpec("o1", "left", "null", "right", "null", "element", "v1");
		specFty.addRefSpec("o2", "left", "null", "right", "null", "element", "v2");
		specFty.addRefSpec("h2", "root", "o3");
		specFty.addRefSpec("o3", "left", "o4", "right", "null", "element", "v3");
		specFty.addRefSpec("o4", "left", "o5", "right", "null");
		specFty.addRefSpec("o5", "left", "null", "right", "null");
		specFty.addVarSpec("(< v1 v3)");
		specFty.addVarSpec("(< v2 v3)");
		specFty.setAccessible("h1", "h2");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, h1, h2);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest3$3: " + (end - start) + "ms\n");
	}
	
	private static void genTest5$2() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH h1 = specFty.mkRefDecl(cls$Heap, "h1");
		ObjectH h2 = specFty.mkRefDecl(cls$Heap, "h2");
		ObjectH v2 = specFty.mkVarDecl(SMTSort.INT, "v2");
		ObjectH v3 = specFty.mkVarDecl(SMTSort.INT, "v3");
		ObjectH v5 = specFty.mkVarDecl(SMTSort.INT, "v5");
		specFty.addRefSpec("h1", "root", "o0");
		specFty.addRefSpec("o0", "left", "o1", "right", "null");
		specFty.addRefSpec("o1", "left", "o2", "right", "o3");
		specFty.addRefSpec("o2", "left", "o4", "right", "null", "element", "v2");
		specFty.addRefSpec("o3", "left", "null", "right", "null", "element", "v3");
		specFty.addRefSpec("o4", "left", "null", "right", "null");
		specFty.addRefSpec("h2", "root", "o5");
		specFty.addRefSpec("o5", "left", "o6", "right", "null", "element", "v5");
		specFty.addRefSpec("o6", "right", "null");
		specFty.addVarSpec("(< 2022 (+ v5 v2))");
		specFty.addVarSpec("(> v3 v2)");
		specFty.addVarSpec("(< v2 -100)");
		specFty.setAccessible("h1", "h2");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, h1, h2, v2, v3, v5);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest5$2: " + (end - start) + "ms\n");
	}

}
