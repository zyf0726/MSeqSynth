package mseqsynth.wrapper.symbolic;

import java.util.Collection;

import mseqsynth.algo.MethodInvoke;
import mseqsynth.heap.SymbolicHeap;

public interface SymbolicExecutor {
		
	public int getExecutionCount();
	
	public Collection<PathDescriptor> executeMethod(SymbolicHeap initHeap, MethodInvoke mInvoke);
	
	public Collection<PathDescriptor> executeMethodUnderTest(MethodInvoke mInvoke);

}
