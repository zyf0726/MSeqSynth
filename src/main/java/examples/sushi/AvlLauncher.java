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
import mseqsynth.wrapper.symbolic.SpecFactory;
import mseqsynth.wrapper.symbolic.Specification;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;
import mseqsynth.wrapper.symbolic.SymbolicExecutorWithCachedJBSE;

import static examples.common.Settings.*;

public class AvlLauncher {
	
	private static final int scope$AvlTree	= 1;
	private static final int scope$AvlNode	= 6;
	private static final int maxSeqLength	= 7;
	private static final String hexFilePathAccurate = "HEXsettings/sushi/avltree-accurate.jbse";
	private static final String hexFilePathPartial  = "HEXsettings/sushi/avltree-partial.jbse";
	private static final String logFilePath 		= "tmp/sushi/avl.txt";
	
	private static final Predicate<String> fieldFilter = (name -> !name.startsWith("_"));
	
	private static Class<?> cls$AvlTree;
	private static Class<?> cls$AvlNode;
	private static String hexFilePath;
	
	private static TestGenerator testGenerator;
	
	private static void init(boolean useAccurateSpec) throws ClassNotFoundException {
		cls$AvlTree = Class.forName("examples.sushi.avl.AvlTree");
		cls$AvlNode = Class.forName("examples.sushi.avl.AvlNode");
		hexFilePath = useAccurateSpec ? hexFilePathAccurate : hexFilePathPartial;
	}
	
	private static void configure(boolean showOnConsole) {
		JBSEParameters parms = JBSEParameters.I();
		parms.setTargetClassPath(TARGET_CLASS_PATH);
		parms.setTargetSourcePath(TARGET_SOURCE_PATH);
		parms.setShowOnConsole(showOnConsole);
		parms.setSettingsPath(hexFilePath);
		parms.setHeapScope(cls$AvlTree, scope$AvlTree);
		parms.setHeapScope(cls$AvlNode, scope$AvlNode);
	}
	
	private static List<Method> getMethods() throws NoSuchMethodException {
		List<Method> decMethods = Arrays.asList(cls$AvlTree.getDeclaredMethods());
		List<Method> pubMethods = Arrays.asList(cls$AvlTree.getMethods());
		List<Method> methods = decMethods.stream()
				.filter(m -> pubMethods.contains(m))
				.collect(Collectors.toList());
		System.out.println("public methods (sushi.AvlTree):");
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
		gb.setHeapScope(cls$AvlTree, scope$AvlTree);
		gb.setHeapScope(cls$AvlNode, scope$AvlNode);
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
		gb.setHeapScope(cls$AvlTree, scope$AvlTree);
		gb.setHeapScope(cls$AvlNode, scope$AvlNode);
		SymbolicHeap initHeap = new SymbolicHeapAsDigraph(ExistExpr.ALWAYS_TRUE);
		List<WrappedHeap> heaps = gb.buildGraph(initHeap, maxSeqLength);
		DynamicGraphBuilder.__debugPrintOut(heaps, executor, new PrintStream(logFilePath));
		testGenerator = new TestGenerator(heaps);
		long end = System.currentTimeMillis();
		System.out.println(">> buildGraph (dynamic): " + (end - start) + "ms\n");
	}
	
	public static void main(String[] args) throws Exception {
		final boolean useAccurateSpec = true;
		final boolean showOnConsole = true;
		final boolean simplify = true;
		final boolean useDynamicAlgorithm = true;
		init(useAccurateSpec);
		configure(showOnConsole);
		List<Method> methods = getMethods();
		if (useDynamicAlgorithm) {
			buildGraphDynamic(methods);
		} else {
			buildGraphStatic(methods, simplify);
		}
		genTest4$1();
		genTest4$2();
		genTest6$1();
	}
	
	private static void genTest4$1() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH avlTree = specFty.mkRefDecl(cls$AvlTree, "t");
		specFty.addRefSpec("t", "root", "root");
		specFty.addRefSpec("root", "element", "v2", "left", "root.l", "right", "root.r");
		specFty.addRefSpec("root.l", "element", "v1", "left", "root.l.l", "right", "null");
		specFty.addRefSpec("root.l.l", "element", "v0", "left", "null", "right", "null");
		specFty.addRefSpec("root.r", "element", "v3", "left", "null", "right", "null");
		specFty.setAccessible("t");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, avlTree);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest4$1: " + (end - start) + "ms\n");
	}
	
	private static void genTest4$2() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH avlTree = specFty.mkRefDecl(cls$AvlTree, "t");
		specFty.addRefSpec("t", "root", "root");
		specFty.addRefSpec("root", "element", "v1", "left", "root.l", "right", "root.r");
		specFty.addRefSpec("root.l", "element", "v0", "left", "null", "right", "null");
		specFty.addRefSpec("root.r", "element", "v3", "left", "root.r.l", "right", "null");
		specFty.addRefSpec("root.r.l", "element", "v2", "left", "null", "right", "null");
		specFty.setAccessible("t");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, avlTree);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest4$2: " + (end - start) + "ms\n");
	}

	private static void genTest6$1() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH avlTree = specFty.mkRefDecl(cls$AvlTree, "t");
		specFty.addRefSpec("t", "root", "root");
		specFty.addRefSpec("root", "element", "v3", "left", "root.l", "right", "root.r");
		specFty.addRefSpec("root.l", "element", "v1", "left", "root.l.l", "right", "root.l.r");
		specFty.addRefSpec("root.r", "element", "v4", "left", "null", "right", "root.r.r");
		specFty.addRefSpec("root.l.l", "element", "v0", "left", "null", "right", "null");
		specFty.addRefSpec("root.l.r", "element", "v2", "left", "null", "right", "null");
		specFty.addRefSpec("root.r.r", "element", "v5", "left", "null", "right", "null");
		specFty.setAccessible("t");
		specFty.addVarSpec("(= (- v1 v0) 27)"); // v0 = -115
		specFty.addVarSpec("(= (- v2 v1) 47)"); // v1 = -88
		specFty.addVarSpec("(= (- v3 v2) 36)"); // v2 = -41
		specFty.addVarSpec("(= (- v4 v3) 71)"); // v3 = -5
		specFty.addVarSpec("(= (- v5 v4) 62)"); // v4 = 66
		specFty.addVarSpec("(= (+ v0 v5) 13)"); // v5 = 128
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, avlTree);
		Statement.printStatements(stmts, System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTest6$1: " + (end - start) + "ms\n");
	}

}
