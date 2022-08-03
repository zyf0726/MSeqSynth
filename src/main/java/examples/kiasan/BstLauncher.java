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

public class BstLauncher {
	
	private static final int scope$BinTree			= 1;
	private static final int scopeForJBSE$BinNode	= 5;
	private static final int scopeForHeap$BinNode	= 6;
	private static final int maxSeqLength			= 10;
	private static final String hexFilePath	= "HEXsettings/kiasan/bst.jbse";
	private static final String logFilePath = "tmp/kiasan/bst.txt";
	
	private static Class<?> cls$BinTree;
	private static Class<?> cls$BinNode;
	
	private static TestGenerator testGenerator;
	
	private static void init() throws ClassNotFoundException {
		cls$BinTree = Class.forName("examples.kiasan.binsearchtree.BinarySearchTree");
		cls$BinNode = Class.forName("examples.kiasan.binsearchtree.BinaryNode");
	}
	
	private static void configure(boolean showOnConsole) {
		JBSEParameters parms = JBSEParameters.I();
		parms.setTargetClassPath(TARGET_CLASS_PATH);
		parms.setTargetSourcePath(TARGET_SOURCE_PATH);
		parms.setShowOnConsole(showOnConsole);
		parms.setSettingsPath(hexFilePath);
		parms.setHeapScope(cls$BinTree, scope$BinTree);
		parms.setHeapScope(cls$BinNode, scopeForJBSE$BinNode);
	}
	
	private static List<Method> getMethods() throws NoSuchMethodException {
		List<Method> decMethods = Arrays.asList(cls$BinTree.getDeclaredMethods());
		List<Method> pubMethods = Arrays.asList(cls$BinTree.getMethods());
		List<Method> methods = decMethods.stream()
				.filter(m -> pubMethods.contains(m))
				.collect(Collectors.toList());
		System.out.println("public methods (kiasan.BinarySearchTree):");
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
		gb.setHeapScope(cls$BinTree, scope$BinTree);
		gb.setHeapScope(cls$BinNode, scopeForHeap$BinNode);
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
		gb.setHeapScope(cls$BinTree, scope$BinTree);
		gb.setHeapScope(cls$BinNode, scopeForHeap$BinNode);
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
		genTest3();
		genTest5();
	}
	
	private static void genTest0() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH bst = specFty.mkRefDecl(cls$BinTree, "t");
		specFty.addRefSpec("t", "root", "null");
		specFty.setAccessible("t");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, bst);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest0: " + (end - start) + "ms\n");
	}
	
	private static void genTest3() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH bst = specFty.mkRefDecl(cls$BinTree, "t");
		ObjectH v2 = specFty.mkVarDecl(SMTSort.INT, "v2");
		ObjectH v3 = specFty.mkVarDecl(SMTSort.INT, "v3");
		specFty.addRefSpec("t", "root", "o1");
		specFty.addRefSpec("o1", "left", "o2", "right", "o3");
		specFty.addRefSpec("o2", "element", "v2");
		specFty.addRefSpec("o3", "element", "v3");
		specFty.setAccessible("t");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, bst, v2, v3);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest3: " + (end - start) + "ms\n");
	}
	
	private static void genTest5() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH bst = specFty.mkRefDecl(cls$BinTree, "t");
		specFty.addRefSpec("t", "root", "o1");
		specFty.addRefSpec("o1", "left", "o2", "right", "o3");
		specFty.addRefSpec("o2", "left", "o4", "right", "null", "element", "v2");
		specFty.addRefSpec("o3", "element", "v3");
		specFty.addRefSpec("o4", "left", "o5");
		specFty.addRefSpec("o5", "left", "null", "right", "null");
		specFty.addVarSpec("(= 2021 (+ v2 v3))");
		specFty.setAccessible("t");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, bst);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest5: " + (end - start) + "ms\n");
	}

}
