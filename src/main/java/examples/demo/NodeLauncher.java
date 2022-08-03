package examples.demo;

import static examples.common.Settings.*;

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
import mseqsynth.wrapper.symbolic.SpecFactory;
import mseqsynth.wrapper.symbolic.Specification;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;
import mseqsynth.wrapper.symbolic.SymbolicExecutorWithCachedJBSE;

public class NodeLauncher {
	
	private static final int scope$Node		= 5;
	private static final int maxSeqLength	= 10;
	private static final String logFilePath = "tmp/demo/node.txt";
	
	private static Class<?> cls$Node;
	
	private static TestGenerator testGenerator;
	
	private static void init() throws ClassNotFoundException {
		cls$Node = Class.forName("examples.demo.Node");
	}
	
	private static void configure(boolean showOnConsole) {
		JBSEParameters parms = JBSEParameters.I();
		parms.setTargetClassPath(TARGET_CLASS_PATH);
		parms.setTargetSourcePath(TARGET_SOURCE_PATH);
		parms.setShowOnConsole(showOnConsole);
		parms.setSettingsPath(null);
		parms.setHeapScope(cls$Node, scope$Node);
	}
	
	private static List<Method> getMethods() throws NoSuchMethodException {
		List<Method> decMethods = Arrays.asList(cls$Node.getDeclaredMethods());
		List<Method> pubMethods = Arrays.asList(cls$Node.getMethods());
		List<Method> methods = decMethods.stream()
				.filter(m -> pubMethods.contains(m))
				.collect(Collectors.toList());
		System.out.println("public methods (demo.Node):");
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
		gb.setHeapScope(cls$Node, scope$Node);
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
		gb.setHeapScope(cls$Node, scope$Node);
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
		genTestDemo();
	}
	
	private static void genTestDemo() {
		long start = System.currentTimeMillis();
		SpecFactory specFty = new SpecFactory();
		ObjectH o = specFty.mkRefDecl(cls$Node, "o");
		specFty.addRefSpec("o", "next", "o.next", "value", "o.value");
		specFty.addRefSpec("o.next", "next", "o.next.next", "value", "o.next.value");
		specFty.addRefSpec("o.next.next", "next", "null", "value", "o.next.next.value");
		specFty.setAccessible("o");
		specFty.addVarSpec("(= (- o.value o.next.value) 100)");
		specFty.addVarSpec("(= (- o.next.value o.next.next.value) 200)");
		specFty.addVarSpec("(= (+ o.value o.next.next.value) 800)");
		Specification spec = specFty.genSpec();
		
		List<Statement> stmts = testGenerator.generateTestWithSpec(spec, o);
		Statement.printStatements(stmts, "@DemoTest", System.out);
		long end = System.currentTimeMillis();
		System.out.println(">> genTestDemo: " + (end - start) + "ms\n");
	}
	
}
