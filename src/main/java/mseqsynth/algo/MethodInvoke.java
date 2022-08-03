package mseqsynth.algo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import mseqsynth.heap.ObjectH;

public class MethodInvoke implements Serializable {

	private static final long serialVersionUID = 2767448143905942829L;
	
	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.writeObject(this.javaMethod.getDeclaringClass());
		oos.writeObject(this.javaMethod.getName());
		oos.writeObject(this.javaMethod.getParameterTypes());
		oos.writeObject(this.invokeArgs);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois)
	throws ClassNotFoundException, IOException, NoSuchMethodException, SecurityException {
		Class<?> javaClass = (Class<?>) ois.readObject();
		String methodName = (String) ois.readObject();
		Class<?>[] paraTypes = (Class<?>[]) ois.readObject();
		this.javaMethod = javaClass.getDeclaredMethod(methodName, paraTypes);
		this.invokeArgs = (ArrayList<ObjectH>) ois.readObject();
	}
	
	
	private Method javaMethod;
	private ArrayList<ObjectH> invokeArgs;
	
	public MethodInvoke(Method javaMethod, Collection<ObjectH> invokeArgs) {
		this.javaMethod = javaMethod;
		if (invokeArgs == null) {
			this.invokeArgs = new ArrayList<>();
		} else {
			this.invokeArgs = new ArrayList<>(invokeArgs);
		}
	}
	
	public Method getJavaMethod() {
		return this.javaMethod;
	}
	
	public ArrayList<ObjectH> getInvokeArguments() {
		return new ArrayList<>(this.invokeArgs);
	}
	
}
