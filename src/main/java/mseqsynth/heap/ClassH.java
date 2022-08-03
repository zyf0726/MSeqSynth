package mseqsynth.heap;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import mseqsynth.smtlib.SMTSort;

public final class ClassH implements Serializable {
	
	private static final long serialVersionUID = 4108294590353253314L;
	
	
	private static Map<Class<?>, ClassH> INST_JAVA = new HashMap<>();
	private static Map<SMTSort, ClassH> INST_SMT = new HashMap<>();
	
	static ClassH CLS_NULL = new ClassH(); 
	
	private Class<?> javaClass;
	private SMTSort smtSort;
	
	private ClassH() {
		this.javaClass = null;
		this.smtSort = null;
	}
	
	private ClassH(Class<?> javaClass) {
		Preconditions.checkNotNull(javaClass, "a non-null java class expected");
		this.javaClass = javaClass;
		this.smtSort = null;
	}
	
	private ClassH(SMTSort smtSort) {
		Preconditions.checkNotNull(smtSort, "a non-null SMT-LIB sort expected");
		this.javaClass = null;
		this.smtSort = smtSort;
	}
	
	public static ClassH of(Class<?> javaClass) {
		if (!INST_JAVA.containsKey(javaClass)) {
			INST_JAVA.put(javaClass, new ClassH(javaClass));
		}
		return INST_JAVA.get(javaClass);
	}
	
	public static ClassH of(SMTSort smtSort) {
		if (!INST_SMT.containsKey(smtSort)) {
			INST_SMT.put(smtSort, new ClassH(smtSort));
		}
		return INST_SMT.get(smtSort);
	}
	
	public Class<?> getJavaClass() {
		return this.javaClass;
	}
	
	public SMTSort getSMTSort() {
		return this.smtSort;
	}
	
	public boolean isNullClass() {
		return (this.javaClass == null) && (this.smtSort == null);
	}
	
	public boolean isNonNullClass() {
		return this.javaClass != null;
	}
	
	public boolean isJavaClass() {
		return this.smtSort == null;
	}
	
	public boolean isSMTSort() {
		return this.smtSort != null;
	}
	
	private Object readResolve() throws ObjectStreamException {
		if (this.javaClass != null) {
			return of(this.javaClass);
		} else if (this.smtSort != null) {
			return of(this.smtSort);
		} else {
			return CLS_NULL;
		}
	}
	
}
