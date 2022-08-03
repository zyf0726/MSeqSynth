package mseqsynth.algo;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import mseqsynth.heap.ObjectH;
import mseqsynth.smtlib.BoolConst;
import mseqsynth.smtlib.Constant;
import mseqsynth.smtlib.IntConst;
import mseqsynth.smtlib.Variable;

public class Statement {
	
	private Method javaMethod;
	private List<ObjectH> objArgs;
	private Map<Variable, Constant> constValues;
	private ObjectH returnValue;
	
	public Statement(ObjectH... arguments) {
		this.javaMethod = null;
		this.objArgs = Lists.newArrayList(arguments); 
		this.constValues = new HashMap<>();
		this.returnValue = null;
	}
	
	public Statement(MethodInvoke mInvoke, ObjectH retVal) {
		this.javaMethod = mInvoke.getJavaMethod();
		this.objArgs = mInvoke.getInvokeArguments();
		this.constValues = new HashMap<>();
		this.returnValue = retVal;
	}
	
	public void updateVars(Map<Variable, Constant> vModel) {
		for (ObjectH arg : this.objArgs) {
			if (arg.isHeapObject()) continue;
			Variable var = arg.getVariable();
			Constant val = vModel.get(var);
			if (val != null) {
				this.constValues.put(var, val);
			} else {
				switch (var.getSMTSort()) {
				case INT:
					this.constValues.put(var, IntConst.DEFAULT); break;
				case BOOL:
					this.constValues.put(var, BoolConst.DEFAULT); break;
				}
			}
		}
		if (this.returnValue != null && !this.returnValue.isHeapObject()) {
			Variable var = this.returnValue.getVariable();
			Constant val = vModel.get(var);
			if (val != null) {
				this.constValues.put(var, val);
			} else {
				switch (var.getSMTSort()) {
				case INT:
					this.constValues.put(var, IntConst.DEFAULT); break;
				case BOOL:
					this.constValues.put(var, BoolConst.DEFAULT); break;
				}
			}
		}
	}
	
	public void updateObjs(Map<ObjectH, ObjectH> objSrc) {
		for (int i = 0; i < this.objArgs.size(); ++i) {
			ObjectH arg = this.objArgs.get(i);
			if (arg.isNonNullObject()) {
				this.objArgs.set(i, objSrc.get(arg));
			}
		}
	}
	
	public static void printTest(List<Statement> stmts, PrintStream ps, String targetPackage, String targetClass, Method targetMethod, int testCount) {
		if(targetPackage!=null) {
			ps.println("package "+targetPackage+";");
		}
		
		Map<Integer, String> createdObjs = new HashMap<>();
		Map<ObjectH, String> objNames = new HashMap<>();
		Set<String> clsNames = new HashSet<>();
		
		StringBuilder imsb = new StringBuilder();
		for (Statement stmt : stmts) {
			if (stmt.returnValue != null) {
				ObjectH o = stmt.returnValue;
				if (o.isNonNullObject()) {
					clsNames.add(o.getClassH().getJavaClass().getName());
				}
			}
		}
		
		imsb.append("import org.junit.Test;\n"
				+ "import static org.junit.Assert.*;\n");
		for(String s: clsNames) {
			imsb.append("import "+s+";\n");
		}
		
		ps.println(imsb);
		
		ps.println("public class "+targetClass+"_"+testCount+"_Test { \n"
				+ "@Test(timeout = 4000)\n"
				+ "public void test0() throws Throwable {");
		
		createdObjs.put(0, "null");
		objNames.put(ObjectH.NULL, "null");
		int countRetVals = 0;
		for (Statement stmt : stmts) {
			StringBuilder sb = new StringBuilder();
			if (stmt.returnValue != null) {
				ObjectH o = stmt.returnValue;
				if (o.isNonNullObject()) {
					objNames.put(o, "o" + (countRetVals++));
					sb.append(o.getClassH().getJavaClass().getSimpleName() + " ");
					sb.append(objNames.get(o) + " = ");
				} else if (o.getClassH().isNonNullClass()) {
					Variable idVar = o.getVariable();
					Constant idVal = stmt.constValues.get(idVar);
					Integer id = Integer.parseInt(idVal.toSMTString());
					if (id != 0) {
						if (!createdObjs.containsKey(id)) {
							createdObjs.put(id, "o" + (countRetVals++));
						}
						sb.append(o.getClassH().getJavaClass().getSimpleName() + " ");
						sb.append(createdObjs.get(id) + " = ");
					}
				}
			}
			Method method=stmt.javaMethod != null ? stmt.javaMethod : targetMethod;

			if(!Modifier.isStatic(method.getModifiers())) {
				ObjectH arg=stmt.objArgs.get(0);
				sb.append(objNames.get(arg) + ".");
				stmt.objArgs.remove(0);
			}
			else {
				Class<?> clz=method.getDeclaringClass();
				sb.append(clz.getName()+ ".");
			}
			sb.append(method.getName() + "(");
			for (ObjectH arg : stmt.objArgs) {
				if (arg.isHeapObject()) {
					sb.append(objNames.get(arg) + ", ");
				} else {
					Variable var = arg.getVariable();
					Constant val = stmt.constValues.get(var);
					if (arg.getClassH().isJavaClass()) {
						Integer id = Integer.parseInt(val.toSMTString());
						if (createdObjs.containsKey(id)) {
							sb.append(createdObjs.get(id) + ", ");
						} else {
							String type = arg.getClassH().getJavaClass().getSimpleName();
							String name = "o" + (countRetVals++);
							createdObjs.put(id, name);
							ps.println(type + " " + name + " = new " + type + "();");
							sb.append(name + ", ");
						}					
					} else {
						sb.append(val.toSMTString() + ", ");
					}
				}
			}
			if (!stmt.objArgs.isEmpty()) {
				sb.delete(sb.length() - 2, sb.length());
			}
			
			ps.println(sb.toString() + ");");
		}
		ps.println("}\n}");
	}
	
	public static void printStatements(List<Statement> stmts, PrintStream ps) {
		printStatements(stmts, "@Test", ps);
	}
	
	public static void printStatements(List<Statement> stmts, String methodUnderTest, PrintStream ps) {
		Map<Integer, String> createdObjs = new HashMap<>();
		Map<ObjectH, String> objNames = new HashMap<>();
		createdObjs.put(0, "#NULL");
		objNames.put(ObjectH.NULL, "#NULL");
		int countRetVals = 0;
		for (Statement stmt : stmts) {
			StringBuilder sb = new StringBuilder();
			if (stmt.returnValue != null) {
				ObjectH o = stmt.returnValue;
				if (o.isNonNullObject()) {
					objNames.put(o, "o" + (countRetVals++));
					sb.append(o.getClassH().getJavaClass().getSimpleName() + " ");
					sb.append(objNames.get(o) + " = ");
				} else if (o.getClassH().isNonNullClass()) {
					Variable idVar = o.getVariable();
					Constant idVal = stmt.constValues.get(idVar);
					Integer id = Integer.parseInt(idVal.toSMTString());
					if (id != 0) {
						if (!createdObjs.containsKey(id)) {
							createdObjs.put(id, "o" + (countRetVals++));
						}
						sb.append(o.getClassH().getJavaClass().getSimpleName() + " ");
						sb.append(createdObjs.get(id) + " = ");
					}
				}
			}
			if (stmt.javaMethod != null) {
				sb.append(stmt.javaMethod.getName() + "(");
			} else {
				sb.append(methodUnderTest + "(");
			}
			for (ObjectH arg : stmt.objArgs) {
				if (arg.isHeapObject()) {
					sb.append(objNames.get(arg) + ", ");
				} else {
					Variable var = arg.getVariable();
					Constant val = stmt.constValues.get(var);
					if (arg.getClassH().isJavaClass()) {
						Integer id = Integer.parseInt(val.toSMTString());
						if (createdObjs.containsKey(id)) {
							sb.append(createdObjs.get(id) + ", ");
						} else {
							String type = arg.getClassH().getJavaClass().getSimpleName();
							String name = "o" + (countRetVals++);
							createdObjs.put(id, name);
							ps.println(type + " " + name + " = new " + type + "()");
							sb.append(name + ", ");
						}					
					} else {
						sb.append(val.toSMTString() + ", ");
					}
				}
			}
			if (!stmt.objArgs.isEmpty()) {
				sb.delete(sb.length() - 2, sb.length());
			}
			ps.println(sb.toString() + ")");
		}
	}
	
}
