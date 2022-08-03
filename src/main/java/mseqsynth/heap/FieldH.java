package mseqsynth.heap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

public final class FieldH implements Serializable, Comparable<FieldH> {
	
	private static final long serialVersionUID = 6842704252957604517L;
	

	private static Map<Field, FieldH> INST = new HashMap<>();
	
	private Field javaField;
	
	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.writeObject(this.javaField.getDeclaringClass());
		oos.writeObject(this.javaField.getName());
	}
	
	private void readObject(ObjectInputStream ois)
	throws ClassNotFoundException, IOException, NoSuchFieldException, SecurityException {
		Class<?> javaClass = (Class<?>) ois.readObject();
		String fieldName = (String) ois.readObject();
		this.javaField = javaClass.getDeclaredField(fieldName);
	}
	
	private Object readResolve() throws ObjectStreamException {
		return of(this.javaField);
	}

	private FieldH(Field javaField) {
		Preconditions.checkNotNull(javaField, "a non-null java class field expected");
		this.javaField = javaField;
	}
	
	public static FieldH of(Field javaField) {
		if (!INST.containsKey(javaField)) {
			INST.put(javaField, new FieldH(javaField));
		}
		return INST.get(javaField);
	}
	
	@Override
	public int compareTo(FieldH other) {
		return this.javaField.toString().compareTo(other.javaField.toString());
	}
	
	public String getName() {
		return this.javaField.getName();
	}
	
	public Field getJavaField() {
		return this.javaField;
	}
}
