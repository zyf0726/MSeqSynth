package mseqsynth.heap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.Variable;
import mseqsynth.util.Bijection;
import mseqsynth.util.graph.Edge;
import mseqsynth.util.graph.FeatureNodewise;
import mseqsynth.util.graph.GraphAnalyzer;

public class SymbolicHeapAsDigraph implements SymbolicHeap {
	
	private static final long serialVersionUID = 8975670477694251838L;
	
	
	private ImmutableSet<ObjectH> accObjs;
	private ExistExpr constraint;
	
	private ImmutableSet<ObjectH> allObjs;  // must be reachable
	private ImmutableList<Variable> vars;
	
	private GraphAnalyzer<ObjectH, FieldH> GA;
	
	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.writeObject(this.accObjs);
		oos.writeObject(this.constraint);
		oos.writeObject(this.allObjs);
		oos.writeObject(this.vars);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois)
	throws ClassNotFoundException, IOException, NoSuchFieldException, SecurityException {
		this.accObjs = (ImmutableSet<ObjectH>) ois.readObject();
		this.constraint = (ExistExpr) ois.readObject();
		this.allObjs = (ImmutableSet<ObjectH>) ois.readObject();
		this.vars = (ImmutableList<Variable>) ois.readObject();
		List<Edge<ObjectH, FieldH>> edges = new ArrayList<>();
		for (ObjectH o : this.allObjs) {
			for (Entry<FieldH, ObjectH> entry : o.getEntries())
				edges.add(new Edge<ObjectH, FieldH>(o, entry.getValue(), entry.getKey()));
		}
		this.GA = new GraphAnalyzer<>(this.allObjs, edges);
	}

	public SymbolicHeapAsDigraph(ExistExpr constraint) {
		this.accObjs = ImmutableSet.of(ObjectH.NULL);
		this.setConstraint(constraint);
		this.allObjs = ImmutableSet.of(ObjectH.NULL);
		this.vars = ImmutableList.of(new BoolVar());  // a dummy variable, to avoid zero-argument user function
		this.GA = new GraphAnalyzer<>(this.allObjs, null);
	}
	
	public SymbolicHeapAsDigraph(Collection<ObjectH> accObjs, ExistExpr constraint) {
		Preconditions.checkNotNull(accObjs,
				"a non-null collection of accessible objects expected");
		Preconditions.checkArgument(accObjs.contains(ObjectH.NULL),
				"ObjectH.NULL must be accessible");
		this.accObjs = ImmutableSet.copyOf(accObjs);  // accessible objects cannot be null
		this.setConstraint(constraint);
		
		Set<ObjectH> objVisited = new HashSet<>(accObjs);
		Deque<ObjectH> objQueue = new ArrayDeque<>(accObjs);
		List<Edge<ObjectH, FieldH>> edges = new ArrayList<>();
		while (!objQueue.isEmpty()) {
			ObjectH o = objQueue.removeFirst();
			for (Entry<FieldH, ObjectH> entry : o.getEntries()) {
				edges.add(new Edge<ObjectH, FieldH>(o, entry.getValue(), entry.getKey()));
				if (objVisited.add(entry.getValue())) {
					objQueue.addLast(entry.getValue());
				}
			}
		}
		
		this.allObjs = ImmutableSet.copyOf(objVisited);
		this.vars = ImmutableList.copyOf(objVisited.stream()
				.filter(o -> o.isVariable())
				.map(o -> o.getVariable())
				.collect(Collectors.toList()));
		this.GA = new GraphAnalyzer<>(objVisited, edges);
	}

	@Override
	public Set<ObjectH> getAllObjects() {
		return this.allObjs;
	}

	@Override
	public Set<ObjectH> getAccessibleObjects() {
		return this.accObjs;
	}

	@Override
	public List<Variable> getVariables() {
		return this.vars;
	}

	@Override
	public ExistExpr getConstraint() {
		return this.constraint;
	}

	@Override
	public void setConstraint(ExistExpr constraint) {
		this.constraint = constraint;
	}

	@Override
	public long getFeatureCode() {
		// TODO hash-based isomorphism decision
		return ImmutableList.of(
				this.accObjs.size(),
				this.allObjs.size(),
				this.vars.size()
				).hashCode();
	}

	@Override
	public boolean maybeIsomorphicWith(SymbolicHeap heap) {
		if (!(heap instanceof SymbolicHeapAsDigraph))
			return false;
		SymbolicHeapAsDigraph other = (SymbolicHeapAsDigraph) heap;
		
		if (this.accObjs.size() != other.accObjs.size())
			return false;
		if (this.allObjs.size() != other.allObjs.size())
			return false;
		if (this.vars.size() != other.vars.size())
			return false;
		
		// TODO hash-based isomorphism decision
		return this.GA.getFeatureGraphwise().equals(other.GA.getFeatureGraphwise());
	}

	@Override
	public boolean surelySubsumedBy(SymbolicHeap heap) {
		// TODO CEGIS or MSA (or abandoned?)
		return false;
	}

	@Override
	public boolean findIsomorphicMappingTo(SymbolicHeap heap, ActionIfFound action) {
		if (!(heap instanceof SymbolicHeapAsDigraph))
			return false;
		SymbolicHeapAsDigraph other = (SymbolicHeapAsDigraph) heap;
		
		Bijection<ObjectH, ObjectH> initMap = new Bijection<>();
		initMap.putUV(ObjectH.NULL, ObjectH.NULL);
		List<ObjectH> sortedAccObjs = Stream.concat(
				this.accObjs.stream().filter(o -> o.isHeapObject()),
				this.accObjs.stream().filter(o -> !o.isHeapObject())
				).collect(Collectors.toList());
		boolean isTerminated = searchMapping(0, sortedAccObjs, other, action, false, initMap);
		return isTerminated;
	}
	
	@Override
	public boolean findEmbeddingInto(SymbolicHeap heap, ActionIfFound action) {
		if (!(heap instanceof SymbolicHeapAsDigraph))
			return false;
		SymbolicHeapAsDigraph supHeap = (SymbolicHeapAsDigraph) heap;
		
		Bijection<ObjectH, ObjectH> initMap = new Bijection<>();
		initMap.putUV(ObjectH.NULL, ObjectH.NULL);
		List<ObjectH> sortedAccObjs = Stream.concat(
				this.accObjs.stream().filter(o -> o.isHeapObject()),
				this.accObjs.stream().filter(o -> !o.isHeapObject())
				).collect(Collectors.toList());
		boolean isTerminated = searchMapping(0, sortedAccObjs, supHeap, action, true, initMap);
		return isTerminated;
	}
	
	private boolean searchMapping(int depth, List<ObjectH> accObjList,
			SymbolicHeapAsDigraph other,
			ActionIfFound action,
			boolean embedding,
			Bijection<ObjectH, ObjectH> curMap) {
		if (depth == accObjList.size()) {
			boolean terminate = action.emitMapping(curMap);
			return terminate;
		}
		ObjectH objU = accObjList.get(depth); 
		if (curMap.containsU(objU))
			return searchMapping(depth + 1, accObjList, other, action, embedding, curMap);
		
		for (ObjectH objV : other.accObjs) {
			Bijection<ObjectH, ObjectH> newMap = new Bijection<>(curMap);
			if (updateMappingRecur(objU, objV, newMap, other, embedding)) {
				boolean terminate = searchMapping(depth + 1, accObjList, other, action, embedding, newMap);
				if (terminate) return true;
			}
		}
		return false;
	}
	
	private boolean updateMappingRecur(ObjectH objU, ObjectH objV,
			Bijection<ObjectH, ObjectH> aMap,
			SymbolicHeapAsDigraph other,
			boolean embedding) {
		if (objU.getClassH() != objV.getClassH())
			return false;
		if (!embedding) {
			if (this.accObjs.contains(objU) != other.accObjs.contains(objV))
				return false;
			FeatureNodewise featU = this.GA.getFeatureNodewise(objU);
			FeatureNodewise featV = other.GA.getFeatureNodewise(objV);
			if (!featU.equals(featV))
				return false;
		} else {
			if (this.accObjs.contains(objU) && !other.accObjs.contains(objV))
				return false;
		}
		
		if (aMap.containsU(objU))
			return aMap.getV(objU) == objV;
		if (aMap.containsV(objV))
			return false;
		aMap.putUV(objU, objV);
		
		for (FieldH field : objU.getFields()) {
			ObjectH valU = objU.getFieldValue(field);
			ObjectH valV = objV.getFieldValue(field);
			if (!updateMappingRecur(valU, valV, aMap, other, embedding))
				return false;
		}
		return true;
	}

	@Override
	public Set<ObjectH> cloneAllObjects(Bijection<ObjectH, ObjectH> cloneMap) {
		if (cloneMap == null) {
			cloneMap = new Bijection<>();
		} else {
			cloneMap.clear();
		}
		Set<ObjectH> accObjsClone = new HashSet<>();
		for (ObjectH obj : this.allObjs) {
			ObjectH objClone = 
				obj.getClassH().isNonNullClass() ?
					new ObjectH(obj.getClassH(), null) :
				obj.isVariable() ?
					new ObjectH(obj.getVariable().cloneVariable()) :
				/* obj.isNullObject() ! */
					ObjectH.NULL;
			cloneMap.putUV(obj, objClone);
			if (this.accObjs.contains(obj)) {
				accObjsClone.add(objClone);
			}
		}
		for (ObjectH obj : this.allObjs) {
			Map<FieldH, ObjectH> fieldValMap = new HashMap<>();
			for (Entry<FieldH, ObjectH> entry : obj.getEntries()) {
				FieldH field = entry.getKey();
				ObjectH value = entry.getValue();
				fieldValMap.put(field, cloneMap.getV(value));
			}
			cloneMap.getV(obj).setFieldValueMap(fieldValMap);
		}
		return accObjsClone;
	}

}
