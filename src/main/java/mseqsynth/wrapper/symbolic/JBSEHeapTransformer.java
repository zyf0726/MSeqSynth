package mseqsynth.wrapper.symbolic;

/**
 * @author Zhu Ruidong
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.Set;

import jbse.mem.HeapObjekt;
import jbse.mem.ObjektImpl;
import jbse.mem.PathCondition;
import jbse.mem.State;
import jbse.mem.Variable;
import jbse.val.Primitive;
import jbse.val.ReferenceConcrete;
import jbse.val.ReferenceSymbolic;
import jbse.val.Value;
import mseqsynth.common.exceptions.UnexpectedInternalException;
import mseqsynth.common.exceptions.UnhandledJBSEValue;
import mseqsynth.heap.ClassH;
import mseqsynth.heap.FieldH;
import mseqsynth.heap.ObjectH;
import mseqsynth.smtlib.BoolVar;
import mseqsynth.smtlib.IntVar;

// TODO refactor

public class JBSEHeapTransformer {
	
	public static ObjectH BLANK_OBJ = new ObjectH(ClassH.of(JBSEHeapTransformer.class), null);
	
	// private static int MAX_HEAP_SIZE_JBSE = 1_000_000; 
	private Map<HeapObjekt, ObjectH> finjbseObjMap = new HashMap<>();
	private Map<Primitive, ObjectH> finjbseVarMap = new HashMap<>();
	private Map<ObjectH,Primitive> finVarjbseMap = new HashMap<>(); // a Primitive may correspond to more than one ObjectH
	private Map<ObjectH,ReferenceSymbolic> ObjRefSym = new HashMap<>();
	private Map<ObjectH,ReferenceConcrete> ObjRefCon=new HashMap<>();
	private Map<ReferenceSymbolic,ObjectH> RefObjSym = new HashMap<>();
	private Map<ReferenceConcrete,ObjectH> RefObjCon=new HashMap<>();
	private Map<HeapObjekt,ObjectH> hos=new HashMap<>();
	private Set<ObjectH> nullos=new HashSet<>();
	private Map<Long,HeapObjekt> objects;
	private Predicate<String> fieldFilter;
	
	public JBSEHeapTransformer(Map<Long,HeapObjekt> objects,Predicate<String> fieldFilter) {
		this.objects=new HashMap<>(objects);
		this.fieldFilter=fieldFilter;
	}
	
	public Map<HeapObjekt, ObjectH> getfinjbseObjMap() {
		return this.finjbseObjMap;
	}
	
	public Map<Primitive, ObjectH> getfinjbseVarMap() {
		return this.finjbseVarMap;
	}
	
	public Map<ObjectH,Primitive> getfinVarjbseMap() {
		return this.finVarjbseMap;
	}
	
	public Map<ReferenceSymbolic,ObjectH> getRefObjSym() {
		return this.RefObjSym;
	}
	
	public Map<ReferenceConcrete,ObjectH> getRefObjCon() {
		return this.RefObjCon;
	}
	
	public Map<ObjectH,ReferenceSymbolic> getObjRefSym() {
		return this.ObjRefSym;
	}
	
	public Map<ObjectH,ReferenceConcrete> getObjRefCon() {
		return this.ObjRefCon;
	}
	
	public Map<HeapObjekt,ObjectH> gethos() {
		return this.hos;
	}
	
	public Set<ObjectH> getnullos() {
		return this.nullos;
	}
	
	public Map<Long,HeapObjekt> getobjects() {
		return this.objects;
	}

	
	// transform a HeapObjekt to an ObjectH (with fieldValueMap undetermined)
	private static ObjectH transHeapObjektToObjectH(ObjektImpl o) {
		try {
			String clsName = o.getType().getClassName().replace('/','.');
			Class<?> javaClass = Class.forName(clsName);
			return new ObjectH(ClassH.of(javaClass), null);
		} catch (ClassNotFoundException e) {
			// this should never happen
			throw new UnexpectedInternalException(e);
		}
	}
	
	public boolean transform(State state)  {		
		finjbseObjMap = new HashMap<>();
		finjbseVarMap = new HashMap<>();
		finVarjbseMap = new HashMap<>(); // a Primitive may correspond to more than one ObjectH
		ObjRefSym = new HashMap<>();
		ObjRefCon=new HashMap<>();
		RefObjSym = new HashMap<>();
		RefObjCon=new HashMap<>();
		hos=new HashMap<>();
		nullos=new HashSet<>();
		PathCondition pathCond=state.getRawPathCondition();
		
		Set<HeapObjekt> oos=new HashSet<>();
		
		for (HeapObjekt o : this.objects.values()) {
			if(o.getType().getClassName().replace('/', '.').equals(Object.class.getName())) {
				if(o.getOrigin()==null) oos.add(o);
				continue;
			}
			this.finjbseObjMap.put(o, transHeapObjektToObjectH((ObjektImpl) o));
		}
		
		
		for (Entry<HeapObjekt, ObjectH> entry : this.finjbseObjMap.entrySet()) {
			// determine fieldValMap for each ObjectH
			HeapObjekt ok = entry.getKey();
			ObjectH oh = entry.getValue();
			Map<FieldH, ObjectH> fieldValMap = new HashMap<>();
			String clsName = ok.getType().getClassName().replace('/', '.');
			Class<?> javaClass = null;
			try {
				javaClass = Class.forName(clsName);
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			List<Field> fieldList = new ArrayList<>() ;
			Class<?> tempClass = javaClass;
			while (tempClass != null) {
			      fieldList.addAll(Arrays.asList(tempClass .getDeclaredFields()));
			      tempClass = tempClass.getSuperclass();
			}
			for (Variable var : ok.fields().values()) {
				if(this.fieldFilter.test(var.getName())==false) continue;
				Field javaField=null;
				FieldH field = null;
				try {
					for(Field f:fieldList) {
						if(f.getName().equals(var.getName())) {
							javaField=f;
							break;
						}
					}
					if(javaField==null) throw new NoSuchFieldException();
					field = FieldH.of(javaField);
				} catch (NoSuchFieldException e) {
					//System.out.println(clsName);
					return false;
				}
//				try {
//					javaField = javaClass.getDeclaredField(var.getName());
//					field = FieldH.of(javaField);
//				} catch (NoSuchFieldException | SecurityException | ClassNotFoundException e) {
//					// this should never happen
//					return false;
//					//throw new UnexpectedInternalException(e);
//				}
				Value varValue = var.getValue();
				if (varValue instanceof ReferenceConcrete) {
					ReferenceConcrete rc = (ReferenceConcrete) varValue;
					HeapObjekt objekt = this.objects.get(rc.getHeapPosition());
					if(javaField.getType()==Object.class) {
						if(objekt!=null&&objekt.getType().getClassName().replace('/', '.').equals(Object.class.getName())) {
							ObjectH value=new ObjectH(ClassH.of(Object.class),new HashMap<FieldH, ObjectH>());
							fieldValMap.put(field, value);
							this.ObjRefCon.put(value,rc);
							this.RefObjCon.put(rc, value);
							oos.remove(objekt);
						}
						else {
							ObjectH value=new ObjectH(ClassH.of(Object.class),new HashMap<FieldH, ObjectH>());
							fieldValMap.put(field, value);
							this.nullos.add(value);
						}
					}
					else {
						ObjectH value = this.finjbseObjMap.get(objekt);
						if (value == null) {
							fieldValMap.put(field, ObjectH.NULL);
						} else {
							fieldValMap.put(field, value);
						}
					}
				} else if (varValue instanceof ReferenceSymbolic) {
					ReferenceSymbolic ref = (ReferenceSymbolic) varValue;
					ObjectH value = null;
				 	if (pathCond.resolved(ref)) {
				 		if(ref.getStaticType().equals("Ljava/lang/Object;")) {
				 			//System.out.println(ref.getStaticType());
				 			value=new ObjectH(ClassH.of(Object.class),new HashMap<FieldH, ObjectH>());
				 			this.ObjRefSym.put(value, ref);
				 			this.RefObjSym.put(ref, value);
				 		}
				 		else {
					 		Long pos = pathCond.getResolution(ref);
					 		
					 		if (pos == jbse.mem.Util.POS_NULL) {
					 			value = ObjectH.NULL;
					 		} else {
					 			HeapObjekt objekt=this.objects.get(pos);
					 			value = this.finjbseObjMap.get(objekt);
					 		}
				 		}
				 	} else {
				 		value = BLANK_OBJ;
				 	}
				 	fieldValMap.put(field, value);
				} else if (varValue instanceof Primitive) {
					ObjectH value=null; 
					if(varValue.getType()=='I') value = new ObjectH(new IntVar());
					else if(varValue.getType()=='Z') value=new ObjectH(new BoolVar());
					fieldValMap.put(field, value);
					this.finjbseVarMap.put((Primitive) varValue, value);
					this.finVarjbseMap.put(value,(Primitive) varValue);
				} 
				else {
					throw new UnhandledJBSEValue(varValue.getClass().getName()); 
				}
			}
			oh.setFieldValueMap(fieldValMap);
		}
		
		for(HeapObjekt o:oos) {
			hos.put(o,new ObjectH(ClassH.of(Object.class),new HashMap<FieldH, ObjectH>()));
		}
		
		return true;
						
	}
}
	