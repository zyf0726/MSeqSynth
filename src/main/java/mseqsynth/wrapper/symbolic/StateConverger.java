package mseqsynth.wrapper.symbolic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.TreeMap;

import jbse.bc.Signature;
import jbse.mem.Clause;
import jbse.mem.ClauseAssume;
import jbse.mem.ClauseAssumeAliases;
import jbse.mem.ClauseAssumeExpands;
import jbse.mem.ClauseAssumeNull;
import jbse.mem.ClauseAssumeReferenceSymbolic;
import jbse.mem.HeapObjekt;
import jbse.mem.Objekt;
import jbse.mem.PathCondition;
import jbse.mem.ReachableObjectsCollector;
import jbse.mem.State;
import jbse.mem.Variable;
import jbse.mem.exc.FrozenStateException;
import jbse.val.Expression;
import jbse.val.Primitive;
import jbse.val.PrimitiveSymbolicLocalVariable;
import jbse.val.PrimitiveSymbolicMemberField;
import jbse.val.ReferenceSymbolic;
import jbse.val.ReferenceSymbolicMemberField;
import jbse.val.Simplex;
import jbse.val.WideningConversion;
import mseqsynth.common.exceptions.UnexpectedInternalException;
import mseqsynth.common.exceptions.UnhandledJBSEPrimitive;

// TODO refactor

public class StateConverger {
	
	private boolean doConverge;
	
	static class StateInfos {
		Map<Long,HeapObjekt> objects;
		Map<Long,HeapObjekt> nobjects;
		ArrayList<ClauseAssumeReferenceSymbolic> refcls;
		ArrayList<Clause> primcls;
		ArrayList<Clause> objcls;
		ArrayList<ReferenceSymbolic> refs;
		Set<ReferenceSymbolic> objrefs;
	}
	
	private static boolean checkRefCls(List<ClauseAssumeReferenceSymbolic> rs1,List<ClauseAssumeReferenceSymbolic> rs2) {
		if(rs1.size()!=rs2.size()) return false;
		for(int i=0;i<rs1.size();++i) {
			ClauseAssumeReferenceSymbolic car1=rs1.get(i);
			ClauseAssumeReferenceSymbolic car2=rs2.get(i);
			if(!car1.getReference().equals(car2.getReference())) return false;
			if(car1 instanceof ClauseAssumeExpands) {
				if(!(car2 instanceof ClauseAssumeExpands)) return false;
			}
			else if(car1 instanceof ClauseAssumeAliases) {
				if(!(car2 instanceof ClauseAssumeAliases)) return false;
				ReferenceSymbolic rf1=((ClauseAssumeAliases)car1).getObjekt().getOrigin();
				ReferenceSymbolic rf2=((ClauseAssumeAliases)car2).getObjekt().getOrigin();
				if(!rf1.equals(rf2)) return false;
			}
			else if(car1 instanceof ClauseAssumeNull) {
				if(!(car2 instanceof ClauseAssumeNull)) return false;
			}
		}
		return true;
	}
	
	private static boolean checkVariable(Variable var1,Variable var2) {
		if(var1.getName()!=var2.getName()) return false;
		if(var1.getType()!=var2.getType()) return false;
		if(!var1.getValue().equals(var2.getValue())) return false;
		return true;
	}
	
	private static boolean checkObjekt(HeapObjekt ho1,HeapObjekt ho2) {
		if(ho1.getType()!=ho2.getType()) return false;
		Comparator<Entry<Signature, Variable>> comp= new Comparator<Entry<Signature,Variable>>() {

			@Override
			public int compare(Entry<Signature, Variable> o1, Entry<Signature, Variable> o2) {
				return o1.getKey().hashCode()-o2.getKey().hashCode();
			}
			
		};
		ArrayList<Entry<Signature,Variable>> fields1= new ArrayList<>(ho1.fields().entrySet());
		ArrayList<Entry<Signature,Variable>> fields2= new ArrayList<>(ho2.fields().entrySet());
		Collections.sort(fields1,comp);
		Collections.sort(fields2,comp);
		for(int i=0;i<fields1.size();++i) {
			Entry<Signature,Variable> entry1=fields1.get(i);
			Entry<Signature,Variable> entry2=fields2.get(i);
			if(!entry1.getKey().equals(entry2.getKey())) 
				return false;
			Variable var1=entry1.getValue();
			Variable var2=entry2.getValue();
			if(checkVariable(var1,var2)==false) 
				return false;
		}
		
		return true;
	}
	
	private static boolean checkState(Map<Long,HeapObjekt> state1,Map<Long,HeapObjekt> state2) {
		if(state1.size()!=state2.size()) return false;
		for(Long pos:state1.keySet()) {
			if(!state2.containsKey(pos)) return false;
			if(checkObjekt(state1.get(pos),state2.get(pos))==false) return false;
		}
		return true;
	}

	private Set<State> states;
	private Map<State,Set<State>> clusters;
	private Map<State,StateInfos> st2infos;
	private Predicate<String> fieldFilter;
	
	public Map<State,Set<State>> getclusters() {
		return this.clusters;
	}
	
	public Map<State,StateInfos> getst2infos() {
		return this.st2infos;
	}
	
	public StateConverger(Set<State> states, boolean doConverge) {
		this(states, null, doConverge);
	}
	
	private StateConverger(Set<State> states,Predicate<String> fieldFilter, boolean doConverge) {
		if(fieldFilter==null) this.fieldFilter=name -> true;
		else this.fieldFilter=fieldFilter;
		this.doConverge = doConverge;
		this.states=new HashSet<>();
		this.st2infos=new HashMap<>();
		this.clusters=new HashMap<>();
		
/*		Comparator<ClauseAssumeReferenceSymbolic> comp= new Comparator<ClauseAssumeReferenceSymbolic>() {

			@Override
			public int compare(ClauseAssumeReferenceSymbolic o1, ClauseAssumeReferenceSymbolic o2) {
				return o1.getReference().toString().compareTo(o2.getReference().toString());
			}
			
		}; */
		for(State state:states) {
			PathCondition jbsepd=state.getRawPathCondition();
			List<Clause> clauses=new ArrayList<>(jbsepd.getClauses());
			
			ArrayList<ClauseAssumeReferenceSymbolic> refclause=new ArrayList<>(); // clauses about reference
			ArrayList<Clause> primclause=new ArrayList<>(); // clauses about primitive
			ArrayList<Clause> objclause=new ArrayList<>();
			ArrayList<ReferenceSymbolic> refs=new ArrayList<>();
			Set<ReferenceSymbolic> objrefs=new HashSet<>();
			
			boolean badal=false;
			
			for(Iterator<Clause> it=clauses.iterator();it.hasNext();) {
				Clause clause=it.next();
				if((! (clause instanceof ClauseAssume)) && (!(clause instanceof ClauseAssumeReferenceSymbolic))) {
					it.remove();
					continue;
				}
				if(clause instanceof ClauseAssume) {
					//primclause.add(clause);
					Primitive p= ((ClauseAssume)clause).getCondition();
					if(toIgnore(p)&&check(p)) {
						// this should never happen
						throw new UnexpectedInternalException("a pathcondition contains both filtered vars and remained vars");
					}
					if(!toIgnore(p)) primclause.add(clause);
				}
				else if(clause instanceof ClauseAssumeReferenceSymbolic) {
					ReferenceSymbolic ref=((ClauseAssumeReferenceSymbolic)clause).getReference();
					if(ref instanceof ReferenceSymbolicMemberField) {
						ReferenceSymbolicMemberField memberref=(ReferenceSymbolicMemberField) ref;
						String fieldname=memberref.getFieldName();
						if(this.fieldFilter.test(fieldname)==false) continue;
					}
					if(ref.getStaticType().equals("Ljava/lang/Object;")) {
						if(clause instanceof ClauseAssumeAliases) {
							ReferenceSymbolic alref=((ClauseAssumeAliases)clause).getObjekt().getOrigin();
							if(!alref.getStaticType().equals("Ljava/lang/Object;")) {
								badal=true;
								break;
							}
						}
						objrefs.add(ref);
						objclause.add(clause);
						continue;
					}
					refs.add(ref);
					refclause.add((ClauseAssumeReferenceSymbolic) clause);
				}
			}
			
			if(badal==true) continue;
			
			this.states.add(state);
			
			StateInfos si=new StateInfos();
			
			Set<Map.Entry<Long, Objekt>> entries = null;
			try {
				final Set<Long> reachable= new ReachableObjectsCollector().reachable(state, false);
				entries = state.getHeap().entrySet().stream()
				        .filter(e -> reachable.contains(e.getKey()))
				        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), throwingMerger(), TreeMap::new)).entrySet();
			} catch (FrozenStateException e1) {
				throw new UnexpectedInternalException(e1);
			}
			state.cleanHeap();
			
			Long numOfobj=(long) 0;
			Map<Long,HeapObjekt> objects=new HashMap<>();
			Map<Long,HeapObjekt> nobjects=new HashMap<>();
			for(Entry<Long,Objekt> entry: entries) {
				objects.put(entry.getKey(), (HeapObjekt) entry.getValue());
				if(!entry.getValue().getType().getClassName().replace('/', '.').equals(Object.class.getName())) {
					nobjects.put(entry.getKey()-numOfobj, (HeapObjekt) entry.getValue());
				}
				else numOfobj++;
			}
			si.objects=objects;
			si.nobjects=nobjects;
			
			//Collections.sort(refclause,comp);
			si.refcls=refclause;
			si.refs=refs;
			si.objrefs=objrefs;
			si.primcls=primclause;
			si.objcls=objclause;
			
			this.st2infos.put(state, si);
			
		}
	}
	
    //copied from java.util.stream.Collectors
    private static <T> BinaryOperator<T> throwingMerger() {
        return (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
    }
	
	private boolean toIgnore(Primitive p) {
		if(p instanceof PrimitiveSymbolicLocalVariable) {
			return false;
		}
		if(p instanceof PrimitiveSymbolicMemberField) {
			PrimitiveSymbolicMemberField pfield=(PrimitiveSymbolicMemberField)p;
			if(this.fieldFilter.test(pfield.getFieldName())==false) {
			//if(pfield.getFieldName().charAt(0)=='_') {
				return true;
			}
			else return false;
		}
		else if(p instanceof Simplex) {
			return false;
		}
		else if(p instanceof Expression) {
			Expression expr=(Expression) p;
			Primitive fst=expr.getFirstOperand();
			Primitive snd=expr.getSecondOperand();
			if(expr.isUnary()) {
				return toIgnore(snd);
			}
			else {
				return toIgnore(fst)||toIgnore(snd);
			}
		}
		else if(p instanceof WideningConversion) {
			return toIgnore(((WideningConversion)p).getArg());
		}
		else {
			throw new UnhandledJBSEPrimitive(p.getClass().getName());
		}
	}
	
	private boolean check(Primitive p) {
		if(p instanceof PrimitiveSymbolicLocalVariable) {
			return false;
		}
		if(p instanceof PrimitiveSymbolicMemberField) {
			PrimitiveSymbolicMemberField pfield=(PrimitiveSymbolicMemberField)p;
			if(this.fieldFilter.test(pfield.getFieldName())==true) {
			//if(pfield.getFieldName().charAt(0)!='_') {
				return true;
			}
			else return false;
		}
		else if(p instanceof Simplex) {
			return false;
		}
		else if(p instanceof Expression) {
			Expression expr=(Expression) p;
			Primitive fst=expr.getFirstOperand();
			Primitive snd=expr.getSecondOperand();
			if(expr.isUnary()) {
				return check(snd);
			}
			else {
				return check(fst)||check(snd);
			}
		}
		else if(p instanceof WideningConversion) {
			return check(((WideningConversion)p).getArg());
		}
		else {
			throw new UnhandledJBSEPrimitive(p.getClass().getName());
		}
	}
	
	public void converge() {
		for(State state:this.states) {
			if(this.clusters.isEmpty()) {
				Set<State> sameStates=new HashSet<>();
				sameStates.add(state);
				this.clusters.put(state, sameStates);
			}
			else {
				boolean findSame=false;
				if (this.doConverge) {
					for(State repState:this.clusters.keySet()) {
						if(checkRefCls(this.st2infos.get(state).refcls,this.st2infos.get(repState).refcls)==false)
							continue;
						if(checkState(this.st2infos.get(state).nobjects,this.st2infos.get(repState).nobjects)==false)
							continue;
						this.clusters.get(repState).add(state);
						findSame=true;
						break;
					}
				}
				if(findSame==false) {
					Set<State> sameStates=new HashSet<>();
					sameStates.add(state);
					this.clusters.put(state, sameStates);
				}
			}
		}
		
		for(Entry<State,Set<State>> entry:this.clusters.entrySet()) {
			State repState=entry.getKey();
			Set<State> states=entry.getValue();
			StateInfos si=this.st2infos.get(repState);
			for(State state:states) {
				si.objrefs.addAll(this.st2infos.get(state).objrefs);
			}
		}
		
		// System.out.println("the size of states "+this.states.size());
		// System.out.println("the size of clusters "+this.clusters.size());
	}
	
	

}
