package mseqsynth.wrapper.symbolic;

import java.io.Serializable;

import mseqsynth.heap.SymbolicHeap;
import mseqsynth.smtlib.SMTExpression;

public class Specification implements Serializable {
	
	private static final long serialVersionUID = -397113991957894154L;
	
	public SymbolicHeap expcHeap;
	public SMTExpression condition;

}
