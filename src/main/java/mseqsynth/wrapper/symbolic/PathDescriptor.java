package mseqsynth.wrapper.symbolic;

import java.util.Map;

import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.smtlib.SMTExpression;
import mseqsynth.smtlib.Variable;

public class PathDescriptor {
	
	public SMTExpression pathCond;
	public ObjectH retVal;
	public SymbolicHeap finHeap;
	
	public Map<ObjectH, ObjectH> objSrcMap;
	public Map<Variable, SMTExpression> varExprMap;
	
}
