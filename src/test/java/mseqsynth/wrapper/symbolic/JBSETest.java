package mseqsynth.wrapper.symbolic;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import jbse.apps.run.Run;
import jbse.apps.run.RunParameters;
import jbse.apps.run.RunParameters.DecisionProcedureType;
import jbse.apps.run.RunParameters.StateFormatMode;
import jbse.apps.run.RunParameters.StepShowMode;
import jbse.apps.settings.ParseException;
import jbse.apps.settings.SettingsReader;
import mseqsynth.common.settings.JBSEParameters;
import mseqsynth.common.settings.Options;

public class JBSETest {
	
    private static final String TARGET_CLASSPATH  = "bin/test/";
    private static final String TARGET_SOURCEPATH = "src/test/java/";
    private static final String JRE_SOURCEPATH    = System.getProperty("java.home", "") + "src.zip";

    //Leave them alone, or add more stuff
    private static final String[] CLASSPATH       = { TARGET_CLASSPATH };
    private static final String[] SOURCEPATH      = { TARGET_SOURCEPATH, JRE_SOURCEPATH };
    
    private static void makeTest(String mClass, String mDesc, String mName,
    		String outPath, String hexFilePath,
    		Map<String, Integer> heapScope) {
		final RunParameters p = new RunParameters();
        p.setJBSELibPath(JBSEParameters.I().getJBSEClassPath());
        p.addUserClasspath(CLASSPATH);
        p.addSourcePath(SOURCEPATH);
        p.setMethodSignature(mClass, mDesc, mName);
        p.setDecisionProcedureType(DecisionProcedureType.Z3);
        p.setExternalDecisionProcedurePath(Options.I().getSolverExecPath());
        p.setStateFormatMode(StateFormatMode.TEXT);
        p.setStepShowMode(StepShowMode.LEAVES);
        p.setShowWarnings(false);
        p.setShowOnConsole(false);
        
        if (outPath != null) {
        	p.setOutputFilePath(outPath);
        } else {
        	p.setOutputFileNone();
        }
        
        if (heapScope != null) {
        	for (Entry<String, Integer> entry : heapScope.entrySet())
        		p.setHeapScope(entry.getKey(), entry.getValue());
        }
        
        if (hexFilePath != null) {
			try {
				new SettingsReader(hexFilePath).fillRunParameters(p);
			} catch (NoSuchFileException e) {
				System.err.println("Error: settings file not found.");
				System.exit(1);
			} catch (ParseException e) {
				System.err.println("Error: settings file syntactically ill-formed.");
				System.exit(2);
			} catch (IOException e) {
				System.err.println("Error while closing settings file.");
				System.exit(2);
			}
        }
        
		final Run r = new Run(p);
		r.run();
    }

	@Test
	public void testListNode() {
    	final String METHOD_CLASS		= "dsclasses/ListNode"; 
    	final String METHOD_DESCRIPTOR	= "(I)Z"; 
    	final String METHOD_NAME		= "setElem";
    	final String OUTPUT_FILE_PATH	= "tmp/JBSETest-ListNode.out";
    	
		makeTest(METHOD_CLASS, METHOD_DESCRIPTOR, METHOD_NAME,
				OUTPUT_FILE_PATH, null, null);
	}
	
	@Test
	public void testAATree() {
    	final String METHOD_CLASS		= "dsclasses/kiasan/aatree/AATree"; 
    	final String METHOD_DESCRIPTOR	= "(I)V"; 
    	final String METHOD_NAME		= "remove";
    	final String OUTPUT_FILE_PATH	= "tmp/JBSETest-AATree.out";
    	final String SETTINGS_FILE		= "HEXsettings/kiasan.jbse";
    	
		makeTest(METHOD_CLASS, METHOD_DESCRIPTOR, METHOD_NAME,
				OUTPUT_FILE_PATH, SETTINGS_FILE,
				ImmutableMap.of("dsclasses/kiasan/aatree/AATree$AANode", 4));
	}
    
	@Test
	public void testBST() {
    	final String METHOD_CLASS		= "dsclasses/kiasan/bst/BinarySearchTree"; 
    	final String METHOD_DESCRIPTOR	= "(I)V"; 
    	final String METHOD_NAME		= "remove";
    	final String OUTPUT_FILE_PATH	= "tmp/JBSETest-BST.out";
    	final String SETTINGS_FILE		= "HEXsettings/kiasan.jbse";
    	
    	makeTest(METHOD_CLASS, METHOD_DESCRIPTOR, METHOD_NAME,
    			OUTPUT_FILE_PATH, SETTINGS_FILE,
    			ImmutableMap.of("dsclasses/kiasan/bst/BinaryNode", 5));
	}
    
	@Test
	public void testLeftist() {
    	final String METHOD_CLASS		= "dsclasses/kiasan/leftist/LeftistHeap"; 
    	final String METHOD_DESCRIPTOR	= "(Ldsclasses/kiasan/leftist/LeftistHeap;)V"; 
    	final String METHOD_NAME		= "merge";
    	final String OUTPUT_FILE_PATH	= "tmp/JBSETest-Leftist.out";
    	final String SETTINGS_FILE		= "HEXsettings/kiasan.jbse";
    	
		makeTest(METHOD_CLASS, METHOD_DESCRIPTOR, METHOD_NAME,
				OUTPUT_FILE_PATH, SETTINGS_FILE,
				ImmutableMap.of("dsclasses/kiasan/leftist/LeftistHeap$LeftistNode", 6));
	}
	
	@Test
	public void testStackLi() {
    	final String METHOD_CLASS		= "dsclasses/kiasan/stackli/StackLi"; 
    	final String METHOD_DESCRIPTOR	= "()Ljava/lang/Object;"; 
    	final String METHOD_NAME		= "topAndPop";
    	final String OUTPUT_FILE_PATH	= "tmp/JBSETest-StackLi.out";
    	final String SETTINGS_FILE		= "HEXsettings/kiasan.jbse";
    	
		makeTest(METHOD_CLASS, METHOD_DESCRIPTOR, METHOD_NAME,
				OUTPUT_FILE_PATH, SETTINGS_FILE, null);
	}
	
}
