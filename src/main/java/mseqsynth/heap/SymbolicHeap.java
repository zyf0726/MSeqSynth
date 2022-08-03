package mseqsynth.heap;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.Variable;
import mseqsynth.util.Bijection;

public interface SymbolicHeap extends Serializable {
	
	public Set<ObjectH> getAllObjects();
	public Set<ObjectH> getAccessibleObjects();
	public List<Variable> getVariables();
	public ExistExpr getConstraint();
	
	public void setConstraint(ExistExpr constraint);
	
	public long getFeatureCode();
	public boolean maybeIsomorphicWith(SymbolicHeap heap);
	public boolean surelySubsumedBy(SymbolicHeap heap);
	public boolean findIsomorphicMappingTo(SymbolicHeap heap, ActionIfFound action);
	public boolean findEmbeddingInto(SymbolicHeap heap, ActionIfFound action);
	
	public Set<ObjectH> cloneAllObjects(Bijection<ObjectH, ObjectH> cloneMap);

}
