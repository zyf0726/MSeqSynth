package examples.sir;

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

public class DllLauncher {

	private static final int scope$List		= 1;
	private static final int scope$Entry	= 5;
	private static final int scope$Iter		= 1;
	private static final int maxSeqLength	= 6; 
	private static final int maxDepth		= 20;
	private static final int maxCount		= 100;
	private static final String hexFilePath	= "HEXsettings/sir/sir-dll.jbse";
	private static final String logFilePath	= "tmp/sir/dll.txt";
	
	private static final Predicate<String> fieldFilter = (name ->
			!name.equals("modCount") && !name.equals("expectedModCount"));
	
	private static Class<?> cls$List, cls$Entry, cls$Iter;
	
	private static TestGenerator testGenerator;
	
	private static void init() throws ClassNotFoundException {
		cls$List = Class.forName("examples.sir.dll.DoubleLinkedList");
		cls$Entry = Class.forName("examples.sir.dll.DoubleLinkedList$Entry");
		cls$Iter = Class.forName("examples.sir.dll.DoubleLinkedList$ListItr");
	}
	
	private static void configure(boolean showOnConsole) {
		JBSEParameters parms = JBSEParameters.I();
		parms.setTargetClassPath(TARGET_CLASS_PATH);
		parms.setTargetSourcePath(TARGET_SOURCE_PATH);
		parms.setShowOnConsole(showOnConsole);
		parms.setSettingsPath(hexFilePath);
		parms.setHeapScope(cls$List, scope$List);
		parms.setHeapScope(cls$Entry, scope$Entry);
		parms.setHeapScope(cls$Iter, scope$Iter);
		parms.setDepthScope(maxDepth);
		parms.setCountScope(maxCount);
	}
	
	private static List<Method> getMethods() throws NoSuchMethodException {
		List<Method> decMethods = Arrays.asList(cls$List.getDeclaredMethods());
		List<Method> pubMethods = Arrays.asList(cls$List.getMethods());
		List<Method> methods = decMethods.stream()
				.filter(m -> pubMethods.contains(m))
				.collect(Collectors.toList());
		System.out.println("public methods (sir.DoublyLinkedList):");
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
		gb.setHeapScope(cls$Entry, scope$Entry);
		gb.setHeapScope(cls$Iter, scope$Iter);
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
		gb.setHeapScope(cls$Entry, scope$Entry);
		gb.setHeapScope(cls$Iter, scope$Iter);
		SymbolicHeap initHeap = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
		List<WrappedHeap> heaps = gb.buildGraph(initHeap, maxSeqLength);
		DynamicGraphBuilder.__debugPrintOut(heaps, executor, new PrintStream(logFilePath));
		testGenerator = new TestGenerator(heaps);
		long end = System.currentTimeMillis();
		System.out.println(">> buildGraph (dynamic): " + (end - start) + "ms\n");
	}
	
	public static void main(String[] args) throws Exception {
		final boolean showOnConsole = true;
		final boolean simplify = false;
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
		genTest(0);
		genTest(3);
	}
	
	private static void genTest1() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH dll = specFty.mkRefDecl(cls$List, "dll");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		specFty.addRefSpec("dll", "header", "o1", "size", "size");
		specFty.addRefSpec("o1", "next", "o2", "element", "null");
		specFty.addRefSpec("o2", "next", "o3", "element", "e1");
		specFty.addRefSpec("o3", "next", "o4", "element", "null");
		specFty.addRefSpec("o4", "next", "o1", "element", "e1");
		specFty.setAccessible("dll");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, dll, size);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest1: " + (end - start) + "ms\n");
	}
	
	private static void genTest2() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH dll = specFty.mkRefDecl(cls$List, "dll");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		specFty.mkRefDecl(cls$Iter, "it");
		specFty.addRefSpec("dll", "header", "o1", "size", "size");
		specFty.addRefSpec("o1", "previous", "o0");
		specFty.addRefSpec("o0", "next", "o1");
		specFty.addRefSpec("it", "next", "o0");
		specFty.addVarSpec("(> size 2)");
		specFty.setAccessible("dll", "it");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, dll, size);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest2: " + (end - start) + "ms\n");
	}
	
	private static void genTest(int expcSize) {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH dll = specFty.mkRefDecl(cls$List, "dll");
		ObjectH size = specFty.mkVarDecl(SMTSort.INT, "size");
		specFty.addRefSpec("dll", "size", "size");
		specFty.addVarSpec("(= size " + expcSize + ")");
		specFty.setAccessible("dll");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, dll, size);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest(" + expcSize + "): " + (end - start) + "ms\n");
	}

}
