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

import lucee.runtime.type.scope.JetendoImpl;
import lucee.transformer.bytecode.reflection.ASMProxyFactory;
import lucee.transformer.expression.ExprBoolean;
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
			throw new RuntimeException("There is no field named: " + memberName + " in " + clazz.getCanonicalName());
		}
		// verify field is accessible
		int modifiers = field.getModifiers();
		if (Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers)) {
			throw new IllegalAccessError(memberName + " is not an accessible field in " + clazz.getCanonicalName());
		}
		String fieldName = field.getName();
		Class fieldClazz = field.getType();
		Type fieldType = Type.getType(fieldClazz);
		if (Modifier.isStatic(modifiers)) {
			// the value is not always a CFML Variable or Function
			if(value instanceof Variable) {
				// this code runs when it is a CFML function or variable

				// create a new variable to be able store the variable value
				int doubleValue= adapter.newLocal(Types.DOUBLE);
				// load the pageContext
				adapter.loadArg(0);
				// invoke the value's scope
				TypeScope.invokeScope(adapter, variable.getScope());
				// invoke the last key of the value
				getFactory().registerKey(bc, member.getName(), false);
				// get the value out of the key
				writeValue(bc);
				// cast the value to the right type for the Jetendo scope, if possible
				adapter.checkCast(Types.DOUBLE);
				// we have to store the result in a local variable to be able to call PUTSTATIC
				adapter.storeLocal(doubleValue);
				// we have to remove the Key and Double from the stack
				adapter.pop2();
				// reload the result
				adapter.loadLocal(doubleValue);

				// we have to duplicate the value so that we can both put and return it
				adapter.dup();
				// assign the variable to the static field of the JetendoImpl class
				adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDoubleStatic", "Ljava/lang/Double;");
			}else {
				// A plain Java type was found, put it's value on the stack
				value.writeOut(bc, MODE_VALUE);
				// adapter.valueOf(); // this might be better for all types
				// get the Double value onto the stack
				adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");

				// we have to duplicate the value so that we can both put and return it
				adapter.dup();
				// assign the variable to the static field of the JetendoImpl class
				adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDoubleStatic", "Ljava/lang/Double;");
			}
			// need to load the variable that will be assigned to top of stack
//			adapter.loadArg(0);
			// need to make sure it is int 1 or 0 for boolean, try without first
			//adapter.visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
			//adapter.pop(); // stack was pagecontext and integer, we only need the second, but possibly discard that too since we are loading our own below
//			adapter.loadArg(0);
//			adapter.pop();
//			adapter.loadArg(1);
//			adapter.loadArg(2);
//			adapter.loadLocal(0);
//			adapter.visitFieldInsn(Opcodes.PUTSTATIC, clazz.getTypeName(), fieldName, fieldType.getDescriptor());
//			adapter.checkCast(fieldType);
//			adapter.visitInsn(Opcodes.ICONST_1);
//			adapter.box(fieldType);
//			adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
//			adapter.visitFieldInsn(Opcodes.PUTSTATIC, clazz.getTypeName(), "memberBool", "Ljava/lang/Boolean;");
//			adapter.checkCast(fieldType);
//			adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "toString", "(D)Ljava/lang/String;", false);
//			adapter.checkCast(Types.STRING);
//			adapter.dup();

//			adapter.visitLdcInsn(new Double("3.0"));
//			adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
//			adapter.putStatic(clazzType, fieldName, fieldType);
//			adapter.visitFieldInsn(Opcodes.PUTSTATIC, clazz.getTypeName(), "memberDouble", "Ljava/lang/Double;");
//			adapter.visitFieldInsn(Opcodes.GETSTATIC, clazz.getTypeName(), "memberDouble", "Ljava/lang/Double;");
		} else {
			adapter.loadArg(0);
			adapter.checkCast(Types.PAGE_CONTEXT_IMPL);
			adapter.invokeVirtual(Types.PAGE_CONTEXT_IMPL, TypeScope.METHODS[scope]);
			adapter.checkCast(clazzType);
			adapter.loadArg(1);
			adapter.checkCast(fieldType);
			adapter.putField(clazzType, fieldName, fieldType);
		}
		// convert primitives like boolean to Boolean so we can always return Object.
//		ASMProxyFactory.boxPrimitive(adapter, fieldClazz);
		return fieldType;
	}

    private Type _writeOutOneDataMember(BytecodeContext bc, DataMember member, boolean last, boolean doOnlyScope) throws TransformerException {
	GeneratorAdapter adapter = bc.getAdapter();

	if (doOnlyScope) {
	    adapter.loadArg(0);
	    if (variable.getScope() == Scope.SCOPE_LOCAL) {
		return TypeScope.invokeScope(adapter, TypeScope.METHOD_LOCAL_TOUCH, Types.PAGE_CONTEXT);
	    }
	    return TypeScope.invokeScope(adapter, variable.getScope());
	}

	    // pc.get
	    if (last) {
		    if(variable.getScope() == Scope.SCOPE_JETENDO){
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
			    TypeScope.invokeScope(adapter, variable.getScope());
			    getFactory().registerKey(bc, member.getName(), false);
			    writeValue(bc);
			    adapter.invokeInterface(TypeScope.SCOPES[variable.getScope()], METHOD_SCOPE_SET_KEY);
		    }

	    }
	    else {
		    adapter.loadArg(0);
		    adapter.loadArg(0);
		    TypeScope.invokeScope(adapter, variable.getScope());
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