package dsclasses;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import dsclasses.ManualExecutor;
import mseqsynth.algo.MethodInvoke;
import mseqsynth.heap.FieldH;
import mseqsynth.heap.ObjectH;
import mseqsynth.heap.SymbolicHeap;
import mseqsynth.heap.SymbolicHeapAsDigraph;
import mseqsynth.smtlib.ApplyExpr;
import mseqsynth.smtlib.ExistExpr;
import mseqsynth.smtlib.IntVar;
import mseqsynth.smtlib.SMTExpression;
import mseqsynth.smtlib.SMTOperator;
import mseqsynth.smtlib.Variable;
import mseqsynth.util.Bijection;
import mseqsynth.wrapper.symbolic.PathDescriptor;
import mseqsynth.wrapper.symbolic.SymbolicExecutor;

public class ManualExecutor implements SymbolicExecutor {
	
	private static ManualExecutor INSTANCE;
	
	public static ManualExecutor I() {
		if (INSTANCE == null)
			INSTANCE = new ManualExecutor();
		return INSTANCE;
	}
	
	private int __countExecution = 0;
	
	private Set<ObjectH> tempAccObjs;
	private Bijection<ObjectH, ObjectH> tempCloneMap;
	private Map<Variable, SMTExpression> tempVarExprMap;
	
	private ManualExecutor() {
		ObjectH.STRICT_MODE = false;
		this.tempCloneMap = new Bijection<>();
		this.tempVarExprMap = new HashMap<>();
	}
	
	@Override
	public int getExecutionCount() {
		return this.__countExecution;
	}

	@Override
	public Collection<PathDescriptor> executeMethod(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		this.__countExecution += 1;
		Method method = mInvoke.getJavaMethod();
		if (method.equals(ListNode.mNew))
			return execListNode$new(initHeap, mInvoke);
		else if (method.equals(ListNode.mGetNext))
			return execListNode$getNext(initHeap, mInvoke);
		else if (method.equals(ListNode.mGetElem))
			return execListNode$getElem(initHeap, mInvoke);
		else if (method.equals(ListNode.mSetElem))
			return execListNode$setElem(initHeap, mInvoke);
		else if (method.equals(ListNode.mAddAfter))
			return execListNode$addAfter(initHeap, mInvoke);
		else if (method.equals(ListNode.mAddBefore))
			return execListNode$addBefore(initHeap, mInvoke);
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<PathDescriptor> executeMethodUnderTest(MethodInvoke mInvoke) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static ObjectH set(ObjectH o, FieldH f, ObjectH v) {
		Map<FieldH, ObjectH> fieldValMap = new HashMap<>();
		for (Entry<FieldH, ObjectH> entry : o.getEntries()) {
			if (entry.getKey().equals(f))
				fieldValMap.put(f, v);
			else fieldValMap.put(entry.getKey(), entry.getValue());
		}
		o.setFieldValueMap(fieldValMap);
		return o;
	}
	
	private void pathInit(SymbolicHeap initHeap) {
		tempAccObjs = initHeap.cloneAllObjects(tempCloneMap);
		tempVarExprMap.clear();
		for (ObjectH o : tempCloneMap.getMapU2V().values()) {
			if (o.isVariable())
				tempVarExprMap.put(o.getVariable(), tempCloneMap.getU(o).getVariable());
		}
	}
	
	private PathDescriptor pathReturn(SMTExpression pathCond, ObjectH retVal) {
		PathDescriptor pd = new PathDescriptor();
		pd.pathCond = pathCond;
		pd.retVal = retVal;
		if (retVal != null) {
			tempAccObjs.add(retVal);
		}
		pd.finHeap = new SymbolicHeapAsDigraph(tempAccObjs, ExistExpr.ALWAYS_FALSE);
		Map<ObjectH, ObjectH> objSrcMap = new HashMap<>(tempCloneMap.getMapV2U());
		objSrcMap.remove(ObjectH.NULL);
		objSrcMap.keySet().removeIf(o -> !o.isHeapObject());
		pd.objSrcMap = ImmutableMap.copyOf(objSrcMap);
		pd.varExprMap = ImmutableMap.copyOf(tempVarExprMap);
		return pd;
	}
	
	private Collection<PathDescriptor>
	execListNode$new(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		List<PathDescriptor> pds = new ArrayList<>();
		Variable arg$elem = mInvoke.getInvokeArguments().get(0).getVariable();
		
		{ // Path 0
			pathInit(initHeap);
			ObjectH varNew = new ObjectH(new IntVar());
			ObjectH objNew = new ObjectH(ListNode.classH,
					ImmutableMap.of(ListNode.fElem, varNew, ListNode.fNext, ObjectH.NULL));
			tempVarExprMap.put(varNew.getVariable(), arg$elem); 
			pds.add(pathReturn(null, objNew));
		}
		
		return pds;
	}
	
	private Collection<PathDescriptor>
	execListNode$getNext(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		List<PathDescriptor> pds = new ArrayList<>();
		ObjectH arg$this = mInvoke.getInvokeArguments().get(0);
		
		if (arg$this.isNonNullObject()) { // Path 0
			pathInit(initHeap);
			ObjectH $this = tempCloneMap.getV(arg$this);
			ObjectH this$next = $this.getFieldValue(ListNode.fNext);
			pds.add(pathReturn(null, this$next));
		}
		
		return pds;
	}
	
	private Collection<PathDescriptor>
	execListNode$getElem(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		List<PathDescriptor> pds = new ArrayList<>();
		ObjectH arg$this = mInvoke.getInvokeArguments().get(0);
		
		if (arg$this.isNonNullObject()) { // Path 0
			pathInit(initHeap);
			pds.add(pathReturn(null, null));
		}
		
		return pds;
	}
	
	private Collection<PathDescriptor>
	execListNode$setElem(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		List<PathDescriptor> pds = new ArrayList<>();
		ObjectH arg$this = mInvoke.getInvokeArguments().get(0);
		
		if (arg$this.isNonNullObject()) { // Path 0
			pathInit(initHeap);
			Variable arg$elem = mInvoke.getInvokeArguments().get(1).getVariable();
			Variable arg$this$elem = arg$this.getFieldValue(ListNode.fElem).getVariable();
			SMTExpression pathCond = new ApplyExpr(SMTOperator.BIN_EQ,
					arg$this$elem, arg$elem);
			pds.add(pathReturn(pathCond, null));
		}
		if (arg$this.isNonNullObject()) { // Path 1
			pathInit(initHeap);
			Variable arg$elem = mInvoke.getInvokeArguments().get(1).getVariable();
			Variable arg$this$elem = arg$this.getFieldValue(ListNode.fElem).getVariable();
			SMTExpression pathCond = new ApplyExpr(SMTOperator.BIN_NE,
					arg$this$elem, arg$elem);
			ObjectH $this = tempCloneMap.getV(arg$this);
			Variable this$elem = $this.getFieldValue(ListNode.fElem).getVariable();
			tempVarExprMap.put(this$elem, arg$elem);
			pds.add(pathReturn(pathCond, null));
		}
		
		return pds;
	}
	
	private Collection<PathDescriptor>
	execListNode$addAfter(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		List<PathDescriptor> pds = new ArrayList<>();
		ObjectH arg$this = mInvoke.getInvokeArguments().get(0);
		Variable arg$elem = mInvoke.getInvokeArguments().get(1).getVariable();
		
		if (arg$this.isNonNullObject()) { // Path 0
			pathInit(initHeap);
			ObjectH varNew = new ObjectH(new IntVar());
			ObjectH objNew = new ObjectH(ListNode.classH,
					ImmutableMap.of(ListNode.fElem, varNew, ListNode.fNext, ObjectH.NULL));
			ObjectH $this = tempCloneMap.getV(arg$this);
			set($this, ListNode.fNext, objNew);
			tempVarExprMap.put(varNew.getVariable(), arg$elem);
			pds.add(pathReturn(null, null));			
		}
		
		return pds;
	}
	
	private Collection<PathDescriptor>
	execListNode$addBefore(SymbolicHeap initHeap, MethodInvoke mInvoke) {
		List<PathDescriptor> pds = new ArrayList<>();
		ObjectH arg$this = mInvoke.getInvokeArguments().get(0);
		Variable arg$elem = mInvoke.getInvokeArguments().get(1).getVariable();
		
		if (arg$this.isNonNullObject()) { // Path 0
			pathInit(initHeap);
			ObjectH $this = tempCloneMap.getV(arg$this);
			ObjectH varNew = new ObjectH(new IntVar());
			ObjectH objNew = new ObjectH(ListNode.classH,
					ImmutableMap.of(ListNode.fElem, varNew, ListNode.fNext, $this));
			tempVarExprMap.put(varNew.getVariable(), arg$elem);
			pds.add(pathReturn(null, objNew));
		}
		
		return pds;
	}
	
	public static void main(String[] args) {
		SymbolicExecutor executor = ManualExecutor.I();
		
		SymbolicHeap emp = new SymbolicHeapAsDigraph(null);
		ObjectH elem$1 = new ObjectH(new IntVar());
		MethodInvoke invoke$new = new MethodInvoke(ListNode.mNew, Arrays.asList(elem$1));
		Collection<PathDescriptor> pds = executor.executeMethod(emp, invoke$new);
		assertEquals(1, pds.size());
		PathDescriptor pd = ImmutableList.copyOf(pds).get(0);
		
		SymbolicHeap h1 = pd.finHeap;
		ObjectH elem$2 = new ObjectH(new IntVar());
		MethodInvoke invoke$addAfter = new MethodInvoke(ListNode.mAddAfter,
				Arrays.asList(pd.retVal, elem$2));
		pds = executor.executeMethod(h1, invoke$addAfter);
		assertEquals(1, pds.size());
		pd = ImmutableList.copyOf(pds).get(0);
		
	}

}
