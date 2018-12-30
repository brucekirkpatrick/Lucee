/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Assosication Switzerland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package lucee.transformer.bytecode.expression.var;

import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.scope.JetendoImpl;
import lucee.transformer.bytecode.reflection.ASMProxyFactory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import lucee.runtime.type.scope.Scope;
import lucee.runtime.type.scope.ScopeFactory;
import lucee.transformer.Position;
import lucee.transformer.TransformerException;
import lucee.transformer.bytecode.BytecodeContext;
import lucee.transformer.bytecode.expression.ExpressionBase;
import lucee.transformer.bytecode.util.ExpressionUtil;
import lucee.transformer.bytecode.util.TypeScope;
import lucee.transformer.bytecode.util.Types;
import lucee.transformer.expression.Expression;
import lucee.transformer.expression.var.DataMember;
import lucee.transformer.expression.var.Member;
import lucee.transformer.expression.var.Variable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Assign extends ExpressionBase {

    // java.lang.Object set(String,Object)
    private final static Method METHOD_SCOPE_SET_KEY = new Method("set", Types.OBJECT, new Type[] { Types.COLLECTION_KEY, Types.OBJECT });

    // .setArgument(obj)
    private final static Method SET_ARGUMENT = new Method("setArgument", Types.OBJECT, new Type[] { Types.OBJECT });

    // Object touch (Object,String)
    private final static Method TOUCH_KEY = new Method("touch", Types.OBJECT, new Type[] { Types.OBJECT, Types.COLLECTION_KEY });

    // Object set (Object,String,Object)
    private final static Method SET_KEY = new Method("set", Types.OBJECT, new Type[] { Types.OBJECT, Types.COLLECTION_KEY, Types.OBJECT });

    // Object getFunction (Object,String,Object[])
    private final static Method GET_FUNCTION_KEY = new Method("getFunction", Types.OBJECT, new Type[] { Types.OBJECT, Types.COLLECTION_KEY, Types.OBJECT_ARRAY });

    // Object getFunctionWithNamedValues (Object,String,Object[])
    private final static Method GET_FUNCTION_WITH_NAMED_ARGS_KEY = new Method("getFunctionWithNamedValues", Types.OBJECT,
	    new Type[] { Types.OBJECT, Types.COLLECTION_KEY, Types.OBJECT_ARRAY });

    private static final Method DATA_MEMBER_INIT = new Method("<init>", Types.VOID, new Type[] { Types.INT_VALUE, Types.INT_VALUE, Types.OBJECT });

    private final Variable variable;
    private final Expression value;

    private int access = -1;
    private int modifier = 0;

    /**
     * Constructor of the class
     * 
     * @param variable
     * @param value
     */
    public Assign(Variable variable, Expression value, Position end) {
	super(variable.getFactory(), variable.getStart(), end);
	this.variable = variable;
	this.value = value;
	if (value instanceof Variable) ((Variable) value).assign(this);
	// this.returnOldValue=returnOldValue;
    }

    @Override
    public Type _writeOut(BytecodeContext bc, int mode) throws TransformerException {
	GeneratorAdapter adapter = bc.getAdapter();

	int count = variable.getCount();
	// count 0
	if (count == 0) {
	    if (variable.ignoredFirstMember() && variable.getScope() == Scope.SCOPE_VAR) {
		// print.dumpStack();
		return Types.VOID;
	    }
	    return _writeOutEmpty(bc);
	}

	boolean doOnlyScope = variable.getScope() == Scope.SCOPE_LOCAL;

	Type rtn = Types.OBJECT;
	// boolean last;
	for (int i = doOnlyScope ? 0 : 1; i < count; i++) {
	    adapter.loadArg(0);
	}
	rtn = _writeOutFirst(bc, (variable.getMembers().get(0)), mode, count == 1, doOnlyScope);

	// pc.get(
	for (int i = doOnlyScope ? 0 : 1; i < count; i++) {
	    Member member = (variable.getMembers().get(i));
	    boolean last = (i + 1) == count;

	    // Data Member
	    if (member instanceof DataMember) {
		// ((DataMember)member).getName().writeOut(bc, MODE_REF);
		getFactory().registerKey(bc, ((DataMember) member).getName(), false);

		if (last) writeValue(bc);
		adapter.invokeVirtual(Types.PAGE_CONTEXT, last ? SET_KEY : TOUCH_KEY);
		rtn = Types.OBJECT;
	    }

	    // UDF
	    else if (member instanceof UDF) {
		if (last) throw new TransformerException("can't assign value to a user defined function", getStart());
		UDF udf = (UDF) member;
		getFactory().registerKey(bc, udf.getName(), false);
		ExpressionUtil.writeOutExpressionArray(bc, Types.OBJECT, udf.getArguments());
		adapter.invokeVirtual(Types.PAGE_CONTEXT, udf.hasNamedArgs() ? GET_FUNCTION_WITH_NAMED_ARGS_KEY : GET_FUNCTION_KEY);
		rtn = Types.OBJECT;
	    }
	}
	return rtn;
    }

    private void writeValue(BytecodeContext bc) throws TransformerException {
	// set Access
	if ((access > -1 || modifier > 0)) {
	    GeneratorAdapter ga = bc.getAdapter();
	    ga.newInstance(Types.DATA_MEMBER);
	    ga.dup();
	    ga.push(access);
	    ga.push(modifier);
	    value.writeOut(bc, MODE_REF);
	    ga.invokeConstructor(Types.DATA_MEMBER, DATA_MEMBER_INIT);
	}
	else value.writeOut(bc, MODE_REF);

    }

    private Type _writeOutFirst(BytecodeContext bc, Member member, int mode, boolean last, boolean doOnlyScope) throws TransformerException {

	if (member instanceof DataMember) {
	    return _writeOutOneDataMember(bc, (DataMember) member, last, doOnlyScope);
	    // return Variable._writeOutFirstDataMember(adapter,(DataMember)member,variable.scope, last);
	}
	else if (member instanceof UDF) {
	    if (last) throw new TransformerException("can't assign value to a user defined function", getStart());
	    return VariableImpl._writeOutFirstUDF(bc, (UDF) member, variable.getScope(), doOnlyScope);
	}
	else {
	    if (last) throw new TransformerException("can't assign value to a built in function", getStart());
	    return VariableImpl._writeOutFirstBIF(bc, (BIF) member, mode, last, getStart());
	}
    }
//	private Field getValueOf(GeneratorAdapter adapter, Type fieldType) {
//    	if(fieldType==Types.BOOLEAN){
//    		adapter.invokeStatic("java/lang/Boolean", BOOL_VALUE_OF);
//    		adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
//	    }
    	/*


    private static final org.objectweb.asm.commons.Method BOOL_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.BOOLEAN, new Type[] { Types.BOOLEAN_VALUE });
    private static final org.objectweb.asm.commons.Method SHORT_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.SHORT, new Type[] { Types.SHORT_VALUE });
    private static final org.objectweb.asm.commons.Method INT_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.INTEGER, new Type[] { Types.INT_VALUE });
    private static final org.objectweb.asm.commons.Method LONG_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.LONG, new Type[] { Types.LONG_VALUE });
    private static final org.objectweb.asm.commons.Method FLT_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.FLOAT, new Type[] { Types.FLOAT_VALUE });
    private static final org.objectweb.asm.commons.Method DBL_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.DOUBLE, new Type[] { Types.DOUBLE_VALUE });
    private static final org.objectweb.asm.commons.Method CHR_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.CHARACTER, new Type[] { Types.CHARACTER });
    private static final org.objectweb.asm.commons.Method BYT_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.BYTE, new Type[] { Types.BYTE_VALUE });

    	 */
//	}
// primitive to reference type
private static final org.objectweb.asm.commons.Method BOOL_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.BOOLEAN, new Type[] { Types.BOOLEAN_VALUE });
	private static final org.objectweb.asm.commons.Method SHORT_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.SHORT, new Type[] { Types.SHORT_VALUE });
	private static final org.objectweb.asm.commons.Method INT_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.INTEGER, new Type[] { Types.INT_VALUE });
	private static final org.objectweb.asm.commons.Method LONG_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.LONG, new Type[] { Types.LONG_VALUE });
	private static final org.objectweb.asm.commons.Method FLT_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.FLOAT, new Type[] { Types.FLOAT_VALUE });
	private static final org.objectweb.asm.commons.Method DBL_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.DOUBLE, new Type[] { Types.DOUBLE_VALUE });
	private static final org.objectweb.asm.commons.Method CHR_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.CHARACTER, new Type[] { Types.CHARACTER });
	private static final org.objectweb.asm.commons.Method BYT_VALUE_OF = new org.objectweb.asm.commons.Method("valueOf", Types.BYTE, new Type[] { Types.BYTE_VALUE });
public static void getValueOf(GeneratorAdapter adapter, Class<?> rtn) {
	if (rtn == boolean.class || rtn==Boolean.class) adapter.invokeStatic(Types.BOOLEAN, BOOL_VALUE_OF);
	else if (rtn == short.class || rtn == Short.class) adapter.invokeStatic(Types.SHORT, SHORT_VALUE_OF);
	else if (rtn == int.class || rtn == Integer.class) adapter.invokeStatic(Types.INTEGER, INT_VALUE_OF);
	else if (rtn == long.class || rtn == Long.class) adapter.invokeStatic(Types.LONG, LONG_VALUE_OF);
	else if (rtn == float.class || rtn == Float.class) adapter.invokeStatic(Types.FLOAT, FLT_VALUE_OF);
	else if (rtn == double.class || rtn == Double.class) adapter.invokeStatic(Types.DOUBLE, DBL_VALUE_OF);
	else if (rtn == char.class || rtn == Character.class) adapter.invokeStatic(Types.CHARACTER, CHR_VALUE_OF);
	else if (rtn == byte.class || rtn == Byte.class) adapter.invokeStatic(Types.BYTE, BYT_VALUE_OF);
}
	private Field getField(Class<?> clazz, String memberName) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (field.getName().equalsIgnoreCase(memberName)) {
				return field;
			}
		}
		return null;
	}
	private Type writeOutPutScopeField(BytecodeContext bc, GeneratorAdapter adapter, Class<?> clazz, int scope, DataMember member) throws TransformerException {
    	String memberName=member.getName().toString();
		Type clazzType = Type.getType(clazz);
		// force casting to the implementation type if necessary
		// use reflection to get the Field
		Field field = getField(clazz, memberName);
		if (field == null) {
			// creates a local variable if the scope didn't have the field
			int localIndex=bc.getLocalIndex(new KeyImpl(memberName), Types.OBJECT, true);
//			value.writeOut(bc, MODE_VALUE);
			value.writeOut(bc, MODE_REF);
			adapter.dup();
			adapter.storeLocal(localIndex);
//			throw new RuntimeException("There is no field named: " + memberName + " in " + clazz.getCanonicalName());
			return Types.OBJECT;
		}
		// verify field is accessible
		int modifiers = field.getModifiers();
		if (Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers)) {
			throw new IllegalAccessError(memberName + " is not an accessible field in " + clazz.getCanonicalName());
		}
		String scopeClassName=JetendoImpl.class.getCanonicalName().replace(".", "/");
		String fieldName = field.getName();
		Class fieldClazz = field.getType();
		Type fieldType = Type.getType(fieldClazz);
		if (Modifier.isStatic(modifiers)) {
			// the value is not always a CFML Variable or Function
			if(value instanceof Variable) {
				writeValue(bc);
				// TODO: might need to use Lucee's Caster here later to be more flexible
				// cast the value to the right type for the Jetendo scope, if possible
				adapter.checkCast(fieldType);
				adapter.box(fieldType);
				// we have to duplicate the value so that we can both put and return it
				adapter.dup();
				// assign the variable to the static field of the JetendoImpl class
				adapter.visitFieldInsn(Opcodes.PUTSTATIC, scopeClassName, fieldName, fieldType.getDescriptor());
			}else {
				// A plain Java type was found, put it's value on the stack
				value.writeOut(bc, MODE_VALUE);
				getValueOf(adapter, fieldClazz);
				adapter.box(fieldType);
				// we have to duplicate the value so that we can both put and return it
				adapter.dup();

				// assign the variable to the static field of the JetendoImpl class
				adapter.visitFieldInsn(Opcodes.PUTSTATIC, scopeClassName, fieldName, fieldType.getDescriptor());
			}
		} else {
			if(value instanceof Variable) {
				// this code runs when it is a CFML function or variable

				// load PageContext twice
				adapter.loadArg(0);
				adapter.loadArg(0);
				adapter.checkCast(Types.PAGE_CONTEXT);
				// invoke the value's scope
				adapter.invokeVirtual(Types.PAGE_CONTEXT, TypeScope.METHODS[scope]);
				// cast the scope to JetendoImpl
				adapter.checkCast(clazzType);
				// get the value
				writeValue(bc);
				// TODO: might need to use Lucee's Caster here later to be more flexible
				// cast the value to the right type for the Jetendo scope, if possible
				adapter.checkCast(fieldType);
				adapter.box(fieldType);

//				// assign the variable to the public field of the JetendoImpl class
				adapter.putField(clazzType, fieldName, fieldType);
			}else {
				adapter.loadArg(0);
				adapter.loadArg(0);
				adapter.invokeVirtual(Types.PAGE_CONTEXT, TypeScope.METHODS[scope]);
				adapter.checkCast(clazzType);
				value.writeOut(bc, MODE_VALUE);
				getValueOf(adapter, fieldClazz);
				adapter.box(fieldType);

				adapter.putField(clazzType, fieldName, fieldType);
			}
		}
		return fieldType;
	}

    private Type _writeOutOneDataMember(BytecodeContext bc, DataMember member, boolean last, boolean doOnlyScope) throws TransformerException {
		GeneratorAdapter adapter = bc.getAdapter();
		int scope=variable.getScope();
		if (doOnlyScope) {
		    adapter.loadArg(0);
		    if (scope == Scope.SCOPE_LOCAL) {
			return TypeScope.invokeScope(adapter, TypeScope.METHOD_LOCAL_TOUCH, Types.PAGE_CONTEXT);
		    }
		    return TypeScope.invokeScope(adapter, variable.getScope());
		}

	    // pc.get
	    if (last) {

//		    if (scope == Scope.SCOPE_VAR || scope == Scope.SCOPE_LOCAL || scope == Scope.SCOPE_UNDEFINED) {
//			    // creates a local variable if the scope didn't have the field
//			    String memberName=member.getName().toString();
//			    int localIndex = bc.getLocalIndex(new KeyImpl(memberName), Types.OBJECT, true);
////			    adapter.loadArg(0);
////			    TypeScope.invokeScope(adapter, scope);
////			    writeValue(bc);
//			    value.writeOut(bc, MODE_REF);
////			    adapter.checkCast(Types.OBJECT);
//			    adapter.dup();
//			    adapter.storeLocal(localIndex);
//			    return Types.OBJECT;
//		    }else
		    if(scope == Scope.SCOPE_JETENDO){
		    	return writeOutPutScopeField(bc, adapter, JetendoImpl.class, Scope.SCOPE_JETENDO, member);
//
//		    	// the value is not always a CFML Variable or Function
//			    if(value instanceof Variable) {
//			    	// this code runs when it is a CFML function or variable
//
//				    // create a new variable to be able store the variable value
//				    int doubleValue= adapter.newLocal(Types.DOUBLE);
//				    // load the pageContext
//				    adapter.loadArg(0);
//				    // invoke the value's scope
//				    TypeScope.invokeScope(adapter, variable.getScope());
//				    // invoke the last key of the value
//				    getFactory().registerKey(bc, member.getName(), false);
//				    // get the value out of the key
//				    writeValue(bc);
//				    // cast the value to the right type for the Jetendo scope, if possible
//				    adapter.checkCast(Types.DOUBLE);
//				    // we have to store the result in a local variable to be able to call PUTSTATIC
//				    adapter.storeLocal(doubleValue);
//				    // we have to remove the Key and Double from the stack
//				    adapter.pop2();
//				    // reload the result
//				    adapter.loadLocal(doubleValue);
//
//				    // we have to duplicate the value so that we can both put and return it
//				    adapter.dup();
//				    // assign the variable to the static field of the JetendoImpl class
//				    adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDoubleStatic", "Ljava/lang/Double;");
//			    }else {
//			    	// A plain Java type was found, put it's value on the stack
//				    value.writeOut(bc, MODE_VALUE);
//				    // adapter.valueOf(); // this might be better for all types
//				    // get the Double value onto the stack
//				    adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
//
//				    // we have to duplicate the value so that we can both put and return it
//				    adapter.dup();
//				    // assign the variable to the static field of the JetendoImpl class
//				    adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDoubleStatic", "Ljava/lang/Double;");
//			    }
		    }else {
			    adapter.loadArg(0);
			    TypeScope.invokeScope(adapter, scope);
			    getFactory().registerKey(bc, member.getName(), false);
			    writeValue(bc);
			    adapter.invokeInterface(TypeScope.SCOPES[scope], METHOD_SCOPE_SET_KEY);
		    }

	    }
	    else {
		    adapter.loadArg(0);
		    adapter.loadArg(0);
		    TypeScope.invokeScope(adapter, scope);
		    getFactory().registerKey(bc, member.getName(), false);
		    adapter.invokeVirtual(Types.PAGE_CONTEXT, TOUCH_KEY);
	    }
	    return Types.OBJECT;

    }

    private Type _writeOutEmpty(BytecodeContext bc) throws TransformerException {
	GeneratorAdapter adapter = bc.getAdapter();

	if (variable.getScope() == Scope.SCOPE_ARGUMENTS) {
	    adapter.loadArg(0);
	    TypeScope.invokeScope(adapter, Scope.SCOPE_ARGUMENTS);
	    writeValue(bc);
	    adapter.invokeInterface(TypeScope.SCOPE_ARGUMENT, SET_ARGUMENT);
	}
	else {
	    adapter.loadArg(0);
	    TypeScope.invokeScope(adapter, Scope.SCOPE_UNDEFINED);
	    getFactory().registerKey(bc, bc.getFactory().createLitString(ScopeFactory.toStringScope(variable.getScope(), "undefined")), false);
	    writeValue(bc);
	    adapter.invokeInterface(TypeScope.SCOPES[Scope.SCOPE_UNDEFINED], METHOD_SCOPE_SET_KEY);
	}

	return Types.OBJECT;
    }

    /**
     * @return the value
     */
    public Expression getValue() {
	return value;
    }

    /**
     * @return the variable
     */
    public Variable getVariable() {
	return variable;
    }

    public void setAccess(int access) {
	this.access = access;
    }

    public int getAccess() {
	return access;
    }

    public void setModifier(int modifier) {
	this.modifier = modifier;
    }

    public int getModifier() {
	return modifier;
    }

    public void setFinal(boolean _final) {
	// TODO Auto-generated method stub

    }
}