package mseqsynth.wrapper.symbolic;

import static mseqsynth.wrapper.symbolic.JBSEHeapTransformer.BLANK_OBJ;

import java.lang.reflect.Method;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;

import jbse.apps.run.Run;
import jbse.apps.run.RunParameters;
import jbse.mem.Clause;
import jbse.mem.ClauseAssume;
import jbse.mem.ClauseAssumeAliases;
import jbse.mem.ClauseAssumeNull;
import jbse.mem.ClauseAssumeReferenceSymbolic;
import jbse.mem.HeapObjekt;
import jbse.mem.State;
import jbse.val.Expression;
import jbse.val.Operator;
import jbse.val.Primitive;
import jbse.val.PrimitiveSymbolicLocalVariable;
import jbse.val.PrimitiveSymbolicMemberField;
import jbse.val.ReferenceConcrete;
import jbse.val.ReferenceSymbolic;
import jbse.val.ReferenceSymbolicMemberField;
import jbse.val.Simplex;
import jbse.val.Value;
import jbse.val.WideningConversion;
import mseqsynth.algo.MethodInvoke;
import mseqsynth.common.Logger;
import mseqsynth.common.exceptions.UnexpectedInternalException;
import mseqsynth.common.exceptions.UnhandledJBSEPrimitive;
import mseqsynth.common.exceptions.UnsupportedPrimitiveType;
import mseqsynth.common.exceptions.UnsupportedSMTOperator;
import mseqsynth.common.settings.JBSEParameters;
import mseqsynth.heap.ClassH;
import mseqsynth.heap.FieldH;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ApplyExpr;
import mseqsynth.smtlib.BoolConst;
import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntConst;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTExpression;
import mseqsynth.smtlib.SMTOperator;
import mseqsynth.wrapper.symbolic.StateConverger.StateInfos;

// TODO refactor

public class SymbolicExecutorWithCachedJBSE implements SymbolicExecutor{
	
	private static Map<Operator,SMTOperator> opMap=new HashMap<>();
	
	static {
		opMap.put(Operator.ADD, SMTOperator.ADD);
		opMap.put(Operator.SUB, SMTOperator.SUB);
		opMap.put(Operator.AND, SMTOperator.AND);
		opMap.put(Operator.OR, SMTOperator.OR);
		opMap.put(Operator.EQ, SMTOperator.BIN_EQ);
		opMap.put(Operator.NE, SMTOperator.BIN_NE);
		opMap.put(Operator.MUL, SMTOperator.MUL);
		opMap.put(Operator.NOT, SMTOperator.UN_NOT);
		opMap.put(Operator.LE, SMTOperator.BIN_LE);
		opMap.put(Operator.LT, SMTOperator.BIN_LT);
		opMap.put(Operator.GE, SMTOperator.BIN_GE);
		opMap.put(Operator.GT, SMTOperator.BIN_GT);
		opMap.put(Operator.NEG, SMTOperator.UN_MINUS);
		
	}
		
	private int __countExecution=0;
	
	private Predicate<String> fieldFilter;
	private boolean doCoverge = true;
	
	@Override
	public int getExecutionCount() {
		return this.__countExecution;
	}
	
	
	private static RunParameters getRunParameters(MethodInvoke mInvoke) {
		JBSEParameters parms = JBSEParameters.I(); 
		parms.setTargetMethod(mInvoke.getJavaMethod());
		return parms.getRunParameters();
	}

	//private Map<Method,Set<State>> cachedJBSE;
	private Map<Method,StateConverger> cachedStates;
	private Map<State,JBSEHeapTransformer> cachedTrans;
	
	public SymbolicExecutorWithCachedJBSE() {
		//this.cachedJBSE=new HashMap<>();
		this.cachedStates=new HashMap<>();
		this.cachedTrans=new HashMap<>();
		this.fieldFilter=name -> true;
	}
	
	public SymbolicExecutorWithCachedJBSE(Predicate<String> fieldFilter) {
		//this.cachedJBSE=new HashMap<>();
		this.cachedStates=new HashMap<>();
		this.cachedTrans=new HashMap<>();
		this.fieldFilter=fieldFilter;
	}
	
	public SymbolicExecutorWithCachedJBSE(Predicate<String> fieldFilter, boolean doConverge) {
		//this.cachedJBSE=new HashMap<>();
		this.cachedStates=new HashMap<>();
		this.cachedTrans=new HashMap<>();
		this.fieldFilter=fieldFilter;
		this.doCoverge = doConverge;
	}
	
	private Map<ReferenceSymbolic,ObjectH> ref2ObjMap(ArrayList<ReferenceSymbolic> resolved,Map<Value,ObjectH> val2Obj) {
		Map<ReferenceSymbolic,ObjectH> ref2Obj=new HashMap<>();
		
		for(Entry<Value,ObjectH> entry:val2Obj.entrySet()) {
			if((entry.getKey() instanceof ReferenceSymbolic) && resolved.contains(entry.getKey()) ) {
				ref2Obj.put((ReferenceSymbolic)entry.getKey(), entry.getValue());
			}
		}
		
		for(int i=0;i<resolved.size();++i) {
			ReferenceSymbolic ref=resolved.get(i);
			Stack<ReferenceSymbolic> stack=new Stack<>();
			ReferenceSymbolic localref=ref;
			while(!ref2Obj.containsKey(localref)) {
				stack.push(localref);
				localref=((ReferenceSymbolicMemberField) localref).getContainer();
			}
			ObjectH obj=ref2Obj.get(localref);
			while(!stack.empty()) {
				if(obj.isNullObject()) return null;
				ReferenceSymbolicMemberField memberref=(ReferenceSymbolicMemberField) stack.pop();
				String fieldname=memberref.getFieldName();
				for(FieldH field:obj.getFields()) {
					if(field.getName().equals(fieldname)) {
						obj=obj.getFieldValue(field);
						ref2Obj.put(memberref, obj);
						break;
					}
				}
			}
		}
		
		return ref2Obj;
	}
	
	private boolean isSat(ArrayList<ClauseAssumeReferenceSymbolic> refclause,Map<ReferenceSymbolic,ObjectH> ref2Obj) {
		if(ref2Obj==null) return false;
		for(int i=0;i<refclause.size();++i) {
			ClauseAssumeReferenceSymbolic clause=refclause.get(i);
			ReferenceSymbolic ref=clause.getReference();
			ObjectH obj=ref2Obj.get(ref);
			if(clause instanceof ClauseAssumeNull) {
				if(!obj.isNullObject()) return false;
			}
			else if(clause instanceof ClauseAssumeAliases){
				HeapObjekt heapObj=((ClauseAssumeAliases) clause).getObjekt();
				ReferenceSymbolic oriref=heapObj.getOrigin();
				if(obj!=ref2Obj.get(oriref)) return false;
			}
			else {
				if(obj.isNullObject()) return false;
				for(int j=0;j<i;++j) {
					ClauseAssumeReferenceSymbolic preclause=(ClauseAssumeReferenceSymbolic) refclause.get(j);
					if(obj==ref2Obj.get(preclause.getReference())) return false;
				}
			}
		}
		return true;
	}
	
	private Map<ReferenceSymbolic,ObjectH> obj2ref(Map<HeapObjekt,ObjectH> m) {
		Map<ReferenceSymbolic,ObjectH> ret=new HashMap<>();
		for(Entry<HeapObjekt,ObjectH> entry:m.entrySet()) {
			HeapObjekt ho=entry.getKey();
			ObjectH oh=entry.getValue();
			ret.put(ho.getOrigin(), oh);
		}
		return ret;
	}
	
	private Map<ObjectH, ObjectH> getchangedObjSrcMap(Map<ReferenceSymbolic, ObjectH> init,
			Map<HeapObjekt, ObjectH> fin) {
		Map<ObjectH, ObjectH> ret=new HashMap<>();
		Map<ReferenceSymbolic,ObjectH> rfin=this.obj2ref(fin);
		
		//this.rvsobjSrcMap=new HashMap<>();
		
		for(Entry<ReferenceSymbolic,ObjectH> entry:init.entrySet()) {
			ReferenceSymbolic ref=entry.getKey();
			ObjectH oh=entry.getValue();
			if(rfin.containsKey(ref)) {
				ret.put(rfin.get(ref),oh);
				//this.rvsobjSrcMap.put(oh, rfin.get(ho));
			}
		}
		return ret;
	}
	
	private SMTExpression JBSEexpr2SMTexpr(Map<ReferenceSymbolic,ObjectH> ref2Obj,Map<Value,ObjectH> val2Obj,Primitive p) {
		if(p instanceof PrimitiveSymbolicLocalVariable) {
			return val2Obj.get(p).getVariable();
		}
		if(p instanceof PrimitiveSymbolicMemberField) {
			PrimitiveSymbolicMemberField pfield=(PrimitiveSymbolicMemberField)p;
			ReferenceSymbolic ref=pfield.getContainer();
			ObjectH container=ref2Obj.get(ref);
			for(FieldH field:container.getFields()) {
				if(field.getName().equals(pfield.getFieldName())) {
					return container.getFieldValue(field).getVariable();
				}
			}
			return null;
		}
		else if(p instanceof Simplex) {
			Simplex s=(Simplex) p;
			if(s.getType()=='I') return new IntConst((Integer)s.getActualValue());
			else if(s.getType()=='Z') return new BoolConst((Boolean)s.getActualValue());
			else throw new UnsupportedPrimitiveType();
		}
		else if(p instanceof Expression) {
			Expression expr=(Expression) p;
			Primitive fst=expr.getFirstOperand();
			Primitive snd=expr.getSecondOperand();
			Operator op=expr.getOperator();
			SMTOperator smtop=opMap.get(op);
			if(smtop==null) throw new UnsupportedSMTOperator(op.toString());
			if(fst instanceof WideningConversion || snd instanceof WideningConversion) { //only boolean to int
				assert(smtop==SMTOperator.BIN_EQ||smtop==SMTOperator.BIN_NE);
				if(fst instanceof WideningConversion) {
					assert(snd instanceof Simplex&&snd.getType()=='I');
					Integer n=(Integer) ((Simplex) snd).getActualValue();
					assert(n==0||n==1);
					Primitive ori=((WideningConversion) fst).getArg();
					assert(ori.getType()=='Z');
					return new ApplyExpr(smtop,JBSEexpr2SMTexpr(ref2Obj,val2Obj,ori),new BoolConst(n==1));
				}
				else if(snd instanceof WideningConversion) {
					assert(fst instanceof Simplex&&fst.getType()=='I');
					Integer n=(Integer) ((Simplex) fst).getActualValue();
					assert(n==0||n==1);
					Primitive ori=((WideningConversion) snd).getArg();
					assert(ori.getType()=='Z');
					return new ApplyExpr(smtop,JBSEexpr2SMTexpr(ref2Obj,val2Obj,ori),new BoolConst(n==1));
				}
			}
			if(expr.isUnary()) {
				return new ApplyExpr(smtop,JBSEexpr2SMTexpr(ref2Obj,val2Obj,snd));
			}
			else {
				return new ApplyExpr(smtop,JBSEexpr2SMTexpr(ref2Obj,val2Obj,fst),JBSEexpr2SMTexpr(ref2Obj,val2Obj,snd));
			}
		}	else {
			throw new UnhandledJBSEPrimitive(p.getClass().getName());
		}
	}
	
	private SMTExpression getPathcond(List<Clause> clauses,Map<ReferenceSymbolic,ObjectH> ref2Obj,Map<Value,ObjectH> val2Obj) {
		ArrayList<SMTExpression> pds=new ArrayList<>();
		for(int i=0;i<clauses.size();++i) {
			ClauseAssume ca =(ClauseAssume) clauses.get(i);
			pds.add(this.JBSEexpr2SMTexpr(ref2Obj,val2Obj,ca.getCondition()));
		}
		if(pds.size()==0) return new BoolConst(true);
//		else {
//			ApplyExpr ret=(ApplyExpr)pds.get(0);
//			for(int i=1;i<pds.size();++i) {
//				ret=new ApplyExpr(SMTOperator.AND,ret,(ApplyExpr)pds.get(i));
//			}
//			return ret;
//		}
		else return new ApplyExpr(SMTOperator.AND,pds);
	}
	
	private SMTExpression getObjcond(List<Clause> clauses,Map<ReferenceSymbolic,ObjectH> ref2Obj) {
		ArrayList<SMTExpression> pds=new ArrayList<>();
		ArrayList<mseqsynth.smtlib.Variable> vars=new ArrayList<>();
		for(int i=0;i<clauses.size();++i) {
			ClauseAssumeReferenceSymbolic ca =(ClauseAssumeReferenceSymbolic) clauses.get(i);
			ReferenceSymbolic ref=ca.getReference();
			ObjectH obj=ref2Obj.get(ref);
			if(obj==null) {
				throw new UnexpectedInternalException("Object misses reference");
			}
//			if(obj==ObjectH.NULL) {
//				if(ca instanceof ClauseAssumeNull) continue;
//				return null;
//			}
//			if(!isObj(obj)) {
//				return null;
//				//if(obj!=ObjectH.NULL) System.out.println(obj.toString());
//			}
			if(ca instanceof ClauseAssumeNull) {
				pds.add(new ApplyExpr(SMTOperator.BIN_EQ,obj.getVariable(),new IntConst(0)));
			}
			else if(ca instanceof ClauseAssumeAliases) {
				HeapObjekt heapObj=((ClauseAssumeAliases) ca).getObjekt();
				ReferenceSymbolic oriref=heapObj.getOrigin();
				ObjectH oriObj=ref2Obj.get(oriref);
//				if(oriObj==null) {
//					return null;
//					//throw new UnexpectedInternalException("Object misses reference");
//				}
				pds.add(new ApplyExpr(SMTOperator.BIN_EQ,obj.getVariable(),oriObj.getVariable()));
			}
			else {
				pds.add(new ApplyExpr(SMTOperator.AND, new ApplyExpr(SMTOperator.BIN_NE,obj.getVariable(),new IntConst(0)),
						forall(SMTOperator.BIN_NE,SMTOperator.AND,obj.getVariable(),vars)));
			}
			vars.add(obj.getVariable());
		}
		if(pds.isEmpty()) return new BoolConst(true);
		return new ApplyExpr(SMTOperator.AND,pds);
	}
	
	private boolean isIsomorphic(SymbolicHeap finHeap,SymbolicHeap initHeap,
			Map<ObjectH, ObjectH> objSrcMap,Map<mseqsynth.smtlib.Variable,SMTExpression> varExprMap) {
		Set<ObjectH> finacc=finHeap.getAccessibleObjects();
		Set<ObjectH> initacc=initHeap.getAccessibleObjects();
		Set<SMTExpression> initvars=new HashSet<>();
		for(ObjectH obj:initacc) {
			if(isObj(obj)) initvars.add(obj.getVariable());
		}
		for(ObjectH obj:finacc) {
			if(obj.isNullObject()) continue;
			if(isObj(obj)) {
				if(!initvars.contains(varExprMap.get(obj.getVariable()))) 
					return false;
			}
			else if(!initacc.contains(objSrcMap.get(obj)))
				return false;
		}
		Set<ObjectH> finobjs=finHeap.getAllObjects();
		//Set<ObjectH> initobjs=initHeap.getAllObjects();
		for(ObjectH obj:finobjs) {
			if(obj.isVariable()||obj.isNullObject()) continue;
			ObjectH initobj=objSrcMap.get(obj);
			if(initobj==null) return false;
			for(FieldH field:obj.getFields()) {
				ObjectH val=obj.getFieldValue(field);
				if(val.isNonNullObject()) {
					if(initobj.getFieldValue(field)!=objSrcMap.get(val)) return false;
				}
			}
		}
		return true;
	}
	
	private boolean isObj(ObjectH obj) {
		return obj.getClassH().getJavaClass()==Object.class;
	}
	
	private SMTExpression forall(SMTOperator eq,SMTOperator ao,mseqsynth.smtlib.Variable intv,ArrayList<mseqsynth.smtlib.Variable> intvs) {
		if(intvs.isEmpty()) {
			if(eq==SMTOperator.BIN_EQ) return new BoolConst(false);
			else if(eq==SMTOperator.BIN_NE) return new BoolConst(true);
		}
		ArrayList<ApplyExpr> ret=new ArrayList<>();
		for(mseqsynth.smtlib.Variable v:intvs) {
			ret.add(new ApplyExpr(eq,intv,v));
		}
		//if(ret.isEmpty()) return new BoolConst(true);
		return new ApplyExpr(ao,ret);
	}
	
	private SMTExpression objparams(SymbolicHeap initHeap,ArrayList<ObjectH> args) {
		Set<ObjectH> allobjs= initHeap.getAllObjects();
		ArrayList<mseqsynth.smtlib.Variable> Oobjs=new ArrayList<>();
		ArrayList<mseqsynth.smtlib.Variable> accOobjs=new ArrayList<>();
		boolean allacc=true;
		for(ObjectH obj:allobjs) {
			if(isObj(obj)) {
				Oobjs.add(obj.getVariable());
				if(!initHeap.getAccessibleObjects().contains(obj)) allacc=false;
				else accOobjs.add(obj.getVariable());
			}
		}
		if(allacc==true&&(!accOobjs.isEmpty())) return new BoolConst(true);
		ArrayList<ApplyExpr> conds=new ArrayList<>();
		for(ObjectH obj:args) {
			if(isObj(obj)) {
				ApplyExpr cond=new ApplyExpr(SMTOperator.OR,forall(SMTOperator.BIN_NE,SMTOperator.AND,obj.getVariable(),Oobjs),
						forall(SMTOperator.BIN_EQ,SMTOperator.OR,obj.getVariable(),accOobjs));
				conds.add(cond);
			}
		}
		if(conds.isEmpty()) return new BoolConst(true);
		return new ApplyExpr(SMTOperator.AND,conds);
	}
	
	private SMTExpression NobjCond(Set<ObjectH> Oobjs,Map<ObjectH,ReferenceConcrete> Nobjs) {
		ArrayList<mseqsynth.smtlib.Variable> ovars=new ArrayList<>();
		ArrayList<mseqsynth.smtlib.Variable> nvars=new ArrayList<>();
		Map<ReferenceConcrete,ObjectH> rcs=new HashMap<>();
		for(ObjectH obj:Oobjs) ovars.add(obj.getVariable());
		for(ObjectH obj:Nobjs.keySet()) nvars.add(obj.getVariable());
		ArrayList<SMTExpression> pds=new ArrayList<>();
		for(Entry<ObjectH,ReferenceConcrete> entry:Nobjs.entrySet()) {
			mseqsynth.smtlib.Variable var=entry.getKey().getVariable();
			if(entry.getValue()==null) {
				pds.add(new ApplyExpr(SMTOperator.AND, new ApplyExpr(SMTOperator.BIN_NE,var,new IntConst(0)),
						forall(SMTOperator.BIN_NE,SMTOperator.AND,var,ovars)));
			}
			else {
				if(!rcs.containsKey(entry.getValue())) {
					pds.add(new ApplyExpr(SMTOperator.AND, new ApplyExpr(SMTOperator.BIN_NE,var,new IntConst(0)),
							forall(SMTOperator.BIN_NE,SMTOperator.AND,var,ovars)));
					rcs.put(entry.getValue(),entry.getKey());
				}
				else {
					pds.add(new ApplyExpr(SMTOperator.BIN_EQ,var,rcs.get(entry.getValue()).getVariable()));
				}
			}
			ovars.add(var);
		}
		if(pds.isEmpty()) return new BoolConst(true);
		return new ApplyExpr(SMTOperator.AND,pds);
	}
	

	@Override
	public Collection<PathDescriptor> executeMethod(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		// long startT = System.currentTimeMillis();
		Collection<PathDescriptor> pathDescs = __executeMethod(initHeap, mInvoke);
		// long endT = System.currentTimeMillis();
		// System.err.print("INFO: symbolic execution on method " + mInvoke.getJavaMethod().getName());
		// System.err.println(", elapsed " + (endT - startT) + "ms");
		return pathDescs;
	}
	
	public Collection<PathDescriptor> __executeMethod(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		this.__countExecution++;
		List<PathDescriptor> pds = new ArrayList<>();
		if(!Modifier.isStatic(mInvoke.getJavaMethod().getModifiers())&&mInvoke.getInvokeArguments().get(0).isNullObject()) return pds;
		
		Method method=mInvoke.getJavaMethod();
		if(!this.cachedStates.containsKey(method)) {
			Logger.info("start to run JBSE on method [" + mInvoke.getJavaMethod() + "]");
			long start = System.currentTimeMillis();
			RunParameters p = getRunParameters(mInvoke);
			Run r=new Run(p);
			r.run();
			long end = System.currentTimeMillis();
			Logger.info("symbolic execution ended, elapsed " + (end - start) + "ms");
			Logger.info("start to converge symbolic states");
			
			start = System.currentTimeMillis();
			HashSet<State> executed = r.getPathsExecuted();
			//this.cachedJBSE.put(method, executed);
			StateConverger sc=new StateConverger(executed, this.doCoverge);
			sc.converge();
			this.cachedStates.put(method, sc);
			end = System.currentTimeMillis();
			Logger.info("symbolic states converged, elapsed " + (end - start) + "ms");
		}
		
		//Set<State> states=this.cachedJBSE.get(method);
		StateConverger sc=this.cachedStates.get(method);
		Set<State> states=sc.getclusters().keySet();
		
		
		for(Iterator<State> sit=states.iterator();sit.hasNext();) {
			State state=sit.next();
			StateInfos si=sc.getst2infos().get(state);
			Set<State> sameStates=sc.getclusters().get(state);
			
			PathDescriptor pd=new PathDescriptor();
			
			Value[] vargs=state.getValueArgs();
			ArrayList<ObjectH> margs=mInvoke.getInvokeArguments();
			
			Map<Value,ObjectH> val2Obj=new HashMap<>();
			for(int i=0;i<margs.size();++i) {
				val2Obj.put(vargs[i], margs.get(i));
			}
			
			ArrayList<ClauseAssumeReferenceSymbolic> refclause=si.refcls; // clauses about reference
			//ArrayList<Clause> primclause=new ArrayList<>(); // clauses about primitive
			//ArrayList<Clause> objclause=new ArrayList<>();
			ArrayList<ReferenceSymbolic> refs=si.refs;
			for(ReferenceSymbolic ref:si.objrefs) refs.add(ref);
			
			Map<ReferenceSymbolic,ObjectH> allref2Obj=this.ref2ObjMap(refs, val2Obj); //map between Reference and ObjectH
			Map<ReferenceSymbolic,ObjectH> ref2Obj=new HashMap<>();
			Map<ReferenceSymbolic,ObjectH> Oref2Obj=new HashMap<>();
			
			Set<ObjectH> accObjs=new HashSet<>();
			
			if(allref2Obj==null) continue;
			
			for(Entry<ReferenceSymbolic,ObjectH> entry: allref2Obj.entrySet()) {
				if(entry.getKey().getStaticType().equals("Ljava/lang/Object;")) Oref2Obj.put(entry.getKey(), entry.getValue());
				else ref2Obj.put(entry.getKey(), entry.getValue());
			}
			
			if(!this.isSat(refclause, ref2Obj)) continue;
			
			ArrayList<SMTExpression> conds=new ArrayList<>();
			
			for(State sstate:sameStates) {
				SMTExpression cond=this.getPathcond(sc.getst2infos().get(sstate).primcls, ref2Obj, val2Obj);
				SMTExpression Objcond=this.getObjcond(sc.getst2infos().get(sstate).objcls, Oref2Obj);
				conds.add(new ApplyExpr(SMTOperator.AND,cond,Objcond));
			}
			
			SMTExpression pathcond=null;
			if(conds.isEmpty()) pathcond=new BoolConst(true);
			else pathcond=new ApplyExpr(SMTOperator.OR,conds);
			
			//if(Objcond==null) continue; 
			
			JBSEHeapTransformer jhs=null;
			if(!this.cachedTrans.containsKey(state)) {
				jhs=new JBSEHeapTransformer(si.objects,this.fieldFilter);
				this.cachedTrans.put(state, jhs);
			}
			else {
				jhs=this.cachedTrans.get(state);
			}
			if(jhs.transform(state)==false) continue;

			Set<ObjectH> Oobjs=new HashSet<>(jhs.getObjRefSym().keySet());
			Map<ObjectH,ReferenceConcrete> Nobjs=new HashMap<>(jhs.getObjRefCon());
			
			Map<HeapObjekt, ObjectH> finjbseObjMap = jhs.getfinjbseObjMap();
			
			Map<ObjectH, ObjectH> changedObjSrcMap=this.getchangedObjSrcMap(ref2Obj, finjbseObjMap);
			
			Map<ObjectH, ObjectH> rvschangedObjSrcMap=new HashMap<>();
			for(Entry<ObjectH, ObjectH> entry:changedObjSrcMap.entrySet()) {
				rvschangedObjSrcMap.put(entry.getValue(), entry.getKey());
			}
			
			Map<ObjectH, ObjectH> objSrcMap=new HashMap<>();
			Map<ObjectH, ObjectH> rvsobjSrcMap=new HashMap<>();
			Map<mseqsynth.smtlib.Variable,SMTExpression> varExprMap=new HashMap<>();
			
			Set<ObjectH> allObjs=initHeap.getAllObjects();
			Collection<ObjectH> changedObjs=ref2Obj.values(); // ObjectHs in ref2Obj are changed(at least used) during the symbolic execution
			for(ObjectH obj:allObjs) { // find unchanged ObjectH
				if(!changedObjs.contains(obj)) {
					if(obj.isNonNullObject()) {
						ObjectH cpy=new ObjectH(obj.getClassH(),null);
						objSrcMap.put(cpy, obj);
						rvsobjSrcMap.put(obj, cpy);
					}
				}
			}
						
			for(Entry<ObjectH,ObjectH> entry:objSrcMap.entrySet()) { //copy unchanged ObjectH
				ObjectH key=entry.getKey();
				ObjectH value=entry.getValue();
				Map<FieldH,ObjectH> field2val=new HashMap<>();
				for(FieldH field:value.getFields()) {
					FieldH finField=FieldH.of(field.getJavaField());
					ObjectH val=value.getFieldValue(field);
					if(val.isVariable()) {
						ObjectH var=null;
						if(isObj(val)) {
							var=new ObjectH(ClassH.of(Object.class),new HashMap<FieldH, ObjectH>());
							Oobjs.add(var);
						}
						else if(val.getVariable() instanceof IntVar) {
							var=new ObjectH(new IntVar());
						}
						else if(val.getVariable() instanceof BoolVar) {
							var=new ObjectH(new BoolVar());
						}
						field2val.put(finField, var);
						varExprMap.put(var.getVariable(), val.getVariable());
						if(initHeap.getAccessibleObjects().contains(val)) {
							accObjs.add(var);
						}
					}
					else {
						if(val.isNullObject()) {
							field2val.put(finField, ObjectH.NULL);
						}
						else if(rvsobjSrcMap.containsKey(val)) {
							field2val.put(finField, rvsobjSrcMap.get(val));
						}
						else {
							field2val.put(finField, rvschangedObjSrcMap.get(val));
						}
					}
				}
				key.setFieldValueMap(field2val);
			}
			
			for(ObjectH obj:finjbseObjMap.values()) { // fill fieldValueMap of changed ObjectH
				Set<FieldH> fields=obj.getFields();
				for(FieldH field:fields) {
					ObjectH val=obj.getFieldValue(field);
					if(val==BLANK_OBJ) {
						ObjectH initObj=changedObjSrcMap.get(obj);
						for(FieldH initField:initObj.getFields()) {
							if(initField.getName().equals(field.getName())) {
								ObjectH initval=initObj.getFieldValue(initField);
								if(isObj(initval)) {
									ObjectH Obj=new ObjectH(ClassH.of(Object.class),new HashMap<FieldH, ObjectH>());
									Oobjs.add(Obj);
									obj.setFieldValue(field, Obj);
									varExprMap.put(Obj.getVariable(), initval.getVariable());
									if(initHeap.getAccessibleObjects().contains(initval))
										accObjs.add(Obj);
								}
								else {
									ObjectH newval=rvsobjSrcMap.get(initval);
									if(newval!=null) obj.setFieldValue(field, newval);
									//else obj.setFieldValue(field, ObjectH.NULL);
									else newval=rvschangedObjSrcMap.get(initval);
									if(newval!=null) obj.setFieldValue(field, newval);
									else obj.setFieldValue(field, ObjectH.NULL);
								}
								break;
							}
						}
					}
				}
			}
			
			for(Entry<ObjectH,ObjectH> entry:changedObjSrcMap.entrySet()) { //reverse
				objSrcMap.put(entry.getKey(), entry.getValue());
			}
			for(Entry<ObjectH,ObjectH> entry:rvschangedObjSrcMap.entrySet()) { //reverse
				rvsobjSrcMap.put(entry.getKey(), entry.getValue());
			}
			
			
			accObjs.add(ObjectH.NULL);
			for(ObjectH obj:jhs.getnullos()) accObjs.add(obj);
			for(ObjectH obj:initHeap.getAccessibleObjects()) {
				if(obj.isNonNullObject()) accObjs.add(rvsobjSrcMap.get(obj));
			}
			
			//Map<Primitive,ObjectH> finjbseVarMap=jhs.getfinjbseVarMap();
			Map<ObjectH,Primitive> finVarjbseMap=jhs.getfinVarjbseMap();
			
			for(Entry<ObjectH,Primitive> entry:finVarjbseMap.entrySet()) {
				Primitive prim=entry.getValue();
				SMTExpression smt=this.JBSEexpr2SMTexpr(ref2Obj,val2Obj, prim);
				//if(smt==null) continue;
				varExprMap.put(entry.getKey().getVariable(), smt);
			}
			
//			
//			SortedMap<Long, Objekt> finHeapJBSE = null;
//			try {
//				finHeapJBSE = state.getHeap();
//			} catch (FrozenStateException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			

			boolean retObj=false;
			Value retVal=state.getStuckReturn();
			if(retVal==null||retVal.getType()=='0') pd.retVal=null;
			else if (retVal instanceof ReferenceConcrete) {
				ReferenceConcrete refRetVal = (ReferenceConcrete) retVal;
				Long pos = refRetVal.getHeapPosition();
				HeapObjekt ho = (HeapObjekt) jhs.getobjects().get(pos);
				if (ho != null) {
					if(ho.getType().getClassName().replace('/', '.').equals(Object.class.getName())) {
						if(jhs.getRefObjCon().containsKey(refRetVal)) {
							accObjs.add(jhs.getRefObjCon().get(refRetVal));
							pd.retVal=jhs.getRefObjCon().get(refRetVal);
						}
						else {
							accObjs.add(jhs.gethos().get(ho));
							pd.retVal=jhs.gethos().get(ho);
						}
						//retObj=true;
					}
					else {
						accObjs.add(finjbseObjMap.get(ho));
						pd.retVal=finjbseObjMap.get(ho);
					}
				} else {
					// this should never happen
					throw new UnexpectedInternalException("returned object not in the final heap");
				}
			}
			else if(retVal instanceof ReferenceSymbolic) {
				ObjectH ret=jhs.getRefObjSym().get((ReferenceSymbolic)retVal);
				if(ret==null) {
					ObjectH o = Oref2Obj.get((ReferenceSymbolic)retVal);
					if(o!=null&&o!=ObjectH.NULL) {
						ret=new ObjectH(ClassH.of(Object.class),new HashMap<FieldH, ObjectH>());
						Oobjs.add(ret);
						varExprMap.put(ret.getVariable(), o.getVariable());
					}
					retObj=true;
				}
				if (ret==null) ret=rvsobjSrcMap.get(ref2Obj.get(retVal));
				//else retObj=true;
				if(ret!=null) {
					pd.retVal=ret;
					accObjs.add(ret);
				}
				else pd.retVal=ObjectH.NULL;
			}
			else if(retVal instanceof Primitive) {
				pd.retVal=null;
			}
			
			
			for(Entry<ObjectH,ReferenceSymbolic> entry: jhs.getObjRefSym().entrySet()) {
				ObjectH o=Oref2Obj.get(entry.getValue());
				if(o.equals(ObjectH.NULL)) {
					varExprMap.put(entry.getKey().getVariable(), new IntConst(0));
					accObjs.add(entry.getKey());
				}
				else {
					varExprMap.put(entry.getKey().getVariable(), o.getVariable());
					if(initHeap.getAccessibleObjects().contains(o)) {
						accObjs.add(entry.getKey());
					}
					if(val2Obj.containsValue(o)) {
						accObjs.add(entry.getKey());
					}
				}
			}
			
			Set<ObjectH> aaccObjs=new HashSet<>(accObjs);
			Map<mseqsynth.smtlib.Variable,SMTExpression> avarExprMap=new HashMap<>(varExprMap);
			for(ObjectH obj:allObjs) {
				if(isObj(obj)&&(!changedObjs.contains(obj))&&(!avarExprMap.containsValue(obj.getVariable()))) {
					ObjectH cpy=new ObjectH(ClassH.of(Object.class),new HashMap<FieldH, ObjectH>());
					Oobjs.add(cpy);
					avarExprMap.put(cpy.getVariable(), obj.getVariable());
					if(initHeap.getAccessibleObjects().contains(obj))
						aaccObjs.add(cpy);
				}
			}
			
			Set<ObjectH> lObjs=new HashSet<>();
			Set<ObjectH> rObjs=new HashSet<>();
			for(ObjectH obj:aaccObjs) {
				if(isObj(obj)) lObjs.add(obj);
				else rObjs.add(obj);
			}
			
			SymbolicHeap tmp=new SymbolicHeapAsDigraph(rObjs,null);
			for(ObjectH obj:lObjs) {
				if(tmp.getAllObjects().contains(obj)) rObjs.add(obj);
			}
			
						
			SymbolicHeap symHeap = new SymbolicHeapAsDigraph(aaccObjs, ExistExpr.ALWAYS_FALSE);
			
			boolean allAcc=true;
			for(ObjectH obj:symHeap.getAllObjects()) {
				if(isObj(obj)&&(!symHeap.getAccessibleObjects().contains(obj))) {
					allAcc=false;
					break;
				}
			}
			
			if(allAcc==true) {
				if(retObj==true) {
					if(!rObjs.contains(pd.retVal)) {
						varExprMap.remove(pd.retVal.getVariable());
						pd.retVal=null;
					}
				}
				symHeap = new SymbolicHeapAsDigraph(rObjs, ExistExpr.ALWAYS_FALSE);
				
			}
			else varExprMap=avarExprMap;
			
			for(Iterator<Entry<ObjectH,ObjectH>> it=objSrcMap.entrySet().iterator();it.hasNext();) {
				Entry<ObjectH,ObjectH> entry=it.next();
				if(!symHeap.getAllObjects().contains(entry.getKey())) it.remove();
			}
			
			Set<ObjectH> fOobjs=new HashSet<>();
			Map<ObjectH,ReferenceConcrete> fNobjs=new HashMap<>();
			for(ObjectH obj:symHeap.getAllObjects()) {
				if(Oobjs.contains(obj)) fOobjs.add(obj);
				else if(Nobjs.containsKey(obj)) fNobjs.put(obj, Nobjs.get(obj));
				else if(jhs.gethos().containsValue(obj)) fNobjs.put(obj, null);
			}
			
			boolean issame=true;
			
			Map<mseqsynth.smtlib.Variable,SMTExpression> rvsvarExprMap=new HashMap<>();
			for(Entry<mseqsynth.smtlib.Variable,SMTExpression> entry:varExprMap.entrySet()) {
				if(entry.getValue() instanceof mseqsynth.smtlib.Variable)
					rvsvarExprMap.put((mseqsynth.smtlib.Variable) entry.getValue(), entry.getKey());
			}
			
			if (this.doCoverge) {
				if(this.isIsomorphic(symHeap, initHeap, objSrcMap,varExprMap)&&
						this.isIsomorphic(initHeap, symHeap, rvsobjSrcMap,rvsvarExprMap)) {
					for(ObjectH obj:symHeap.getAllObjects()) {
						if(obj.isVariable()||obj.isNullObject()) continue;
						ObjectH finobj=obj;
						ObjectH initobj=objSrcMap.get(obj);
						if(initobj==null) {
							issame=false;
							break;
						}
						for(FieldH field:finobj.getFields()) {
							ObjectH val=finobj.getFieldValue(field);
							if(val.isVariable()) {
								SMTExpression expr=varExprMap.get(val.getVariable());
								if(!(expr instanceof mseqsynth.smtlib.Variable)) {
									issame=false;
									break;
								}
								mseqsynth.smtlib.Variable var=(mseqsynth.smtlib.Variable)expr;
								mseqsynth.smtlib.Variable initvar=initobj.getFieldValue(field).getVariable();
								if(var!=initvar) {
									issame=false;
									break;
								}
							}
						}
						if(issame==false) break;
					}
					if(issame==true) {
						if(pd.retVal==null||(pd.retVal.isVariable()&&!isObj(pd.retVal)))
								sit.remove();
						continue; 
					}
				}
			}
			pd.finHeap = symHeap;
			pd.objSrcMap=objSrcMap;
			
			//pd.varExprMap=varExprMap;
			
			pd.varExprMap=new HashMap<>();
			List<mseqsynth.smtlib.Variable> vars= symHeap.getVariables();
			

			for(Entry<mseqsynth.smtlib.Variable,SMTExpression> entry:varExprMap.entrySet()) {
				if(vars.contains(entry.getKey())) pd.varExprMap.put(entry.getKey(), entry.getValue());
			}
			
			for(ObjectH obj:jhs.getnullos()) {
				pd.varExprMap.put(obj.getVariable(),new IntConst(0));
			}
			
			SMTExpression Objparams=this.objparams(initHeap, margs);
			
			pd.pathCond=new ApplyExpr(SMTOperator.AND,pathcond,Objparams,this.NobjCond(fOobjs, fNobjs));
			
			pds.add(pd);
			
		}
		
		return pds;
	}

	@Override
	public Collection<PathDescriptor> executeMethodUnderTest(MethodInvoke mInvoke) {
		// TODO Auto-generated method stub
		return null;
	}


	
	

}
