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
	private Type writeOutPutScopeField(GeneratorAdapter adapter, Class<?> clazz, int scope, String memberName) {
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
			adapter.putStatic(clazzType, fieldName, fieldType);
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
//		    String memberName = member.getName().toString();

		    /*
		    ASM says they fixed negative size errors in newer version.  perhaps try using the newer one again, but leave it on V1_6 opcodes


		    i think one of the data stypes i'm working with has wrong superClass, and needs a function like this:
		        @Override
				protected String getCommonSuperClass(String type1, String type2) {
				    if(type1.matches("IntefaceImpl[AB]") && type2.matches("IntefaceImpl[AB]"))
				        return "IntefaceA";
				    return super.getCommonSuperClass(type1, type2);
				}


		    Expecting a stackmap frame at branch target 29
Exception Details:
Location:
jetendofunc13_cfc$cf.staticConstructor(Llucee/runtime/PageContext;Llucee/runtime/ComponentImpl;)V @20: aload_2
Reason:
Expected stackmap frame at this location.
Bytecode:
0x0000000: 014e 2bb6 002f 3a04 2bb6 0033 03b9 0039
0x0000010: 0200 3605 2c2b b600 3f4e a700 2b3a 062b
0x0000020: 1904 b800 4719 06b8 004d bfa7 001a 3a07
0x0000030: 2bb6 0033 1505 b900 3902 0057 2c2b 2db6
0x0000040: 0051 1907 bf2b b600 3315 05b9 0039 0200
0x0000050: 572c 2b2d b600 512b 1904 b800 47b1
Exception Handler Table:
bci [20, 26] => handler: 29
bci [20, 43] => handler: 46

This one showed actual type in stack:
Bad type on operand stack
Exception Details:
Location:
jetendofunc13_cfc$cf.udfCall(Llucee/runtime/PageContext;Llucee/runtime/type/UDF;I)Ljava/lang/Object; @132: invokestatic
Reason:
Type 'java/lang/Double' (current frame, stack[1]) is not assignable to double_2nd
Current Frame:
bci: @132
flags: { }
locals: { 'jetendofunc13_cfc$cf', 'lucee/runtime/PageContext', 'lucee/runtime/type/UDF', integer, 'lucee/runtime/tag/Content' }
stack: { 'lucee/runtime/PageContext', 'java/lang/Double' }


NegativeArraySizeException because we consume too many stack, but pop and dup don't help!
	it seems like asm is broken instead.

	this bug might fix it: https://gitlab.ow2.org/asm/asm/commit/8043b043f239e0a162c9d82978815392d8ede991?view=parallel
		requires ASM 6.0 or newer

		Operand stack underflow
Exception Details:
Location:
jetendofunc13_cfc$cf.udfCall(Llucee/runtime/PageContext;Llucee/runtime/type/UDF;I)Ljava/lang/Object; @100: pop
Reason:
Attempt to pop empty stack.
Current Frame:
bci: @100
flags: { }
locals: { 'jetendofunc13_cfc$cf', 'lucee/runtime/PageContext', 'lucee/runtime/type/UDF', integer, 'lucee/runtime/tag/Content' }
stack: { }
Bytecode:
0x0000000: 2b12 8fb6 0086 2bc0 0091 1293 1295 0312
0x0000010: 97b6 009b c000 9d3a 0419 0403 b600 a119
0x0000020: 0412 a3b6 00a6 1904 b600 a957 1904 b600
0x0000030: ac08 a000 0803 b800 b1bf a700 113a 052b
0x0000040: c000 9119 04b6 00b5 1905 bf2b c000 9119
0x0000050: 04b6 00b5 2b12 b7b6 0086 0499 000a b200
0x0000060: bdb3 00c2 572b 12c4 b600 8601 b0
Exception Handler Table:
bci [25, 61] => handler: 61
Stackmap Table:
append_frame(@58,Object[#157])
same_locals_1_stack_item_frame(@61,Object[#65])
same_frame(@75)
same_frame(@101)
		     */


//		    adapter.visitLdcInsn(new Double("3.0"));
//			adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
			    // JetendoImpl.class.getTypeName()
//		    adapter.loadArg(1);
			    adapter.loadArg(0);
//			    adapter.loadArg(1);
//			    adapter.dup2();
			    adapter.pop();
//		    adapter.visitLdcInsn(new Double("5.0"));
//			System.out.println("test 1");
//
			// this works for jetendo.memberBool=3.2, but not localVar=3; jetendo.memberBool=localVar;
//			Class valueClass=value.writeOut(bc, MODE_VALUE);
//			//if(valueClass==Double.class.toString()) {
//		    String str = valueClass.toString();
//		    if(valueClass==double.class) {
//			    str += " is a double";
//		    }
//		    if(valueClass==Variable.class){
//		    	str+=" is a Variable";
//		    }
//		    if(valueClass==VariableImpl.class){
//			    str+=" is a VariableImpl";
//		    }
//		    if(valueClass==Double.class){
//			    str+=" is a Double";
//		    }
//		    if(valueClass==DataMember.class){
//			    str+=" is a DataMember";
//		    }
//		    if(valueClass==Member.class){
//			    str+=" is a Member";
//		    }
//		    Class[] classes = valueClass.getDeclaredClasses();
//		    str+="\nDeclared Classes:\n";
//		    for (int i = 0; i < classes.length; i++) {
//			    str+="Class = " + classes[i].getName()+"\n";
//		    }
//			    if(value instanceof Variable) {
//				    str += "value is a Variable\n";
//			    }
//			    if(value instanceof ExprBoolean){
//				    str += "value is a ExprBoolean\n";
//			    }
//		    String str="";
//		    String fileName="C:\\lucee\\luceedebug.txt";
//		    try {
//		        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
//			    writer.write(str);
//		        writer.close();
//		    } catch (IOException e) {
//			    e.printStackTrace();
//		    }

//			adapter.checkCast(Types.DOUBLE);
//		    writeValue(bc);
			    if(value instanceof Variable) {
				    int doubleValue= adapter.newLocal(Types.DOUBLE);
				    adapter.loadArg(0);
				    TypeScope.invokeScope(adapter, variable.getScope());
				    getFactory().registerKey(bc, member.getName(), false);
				    writeValue(bc);
				    adapter.checkCast(Types.DOUBLE);
				    adapter.storeLocal(doubleValue);
				    adapter.pop2();
				    adapter.loadLocal(doubleValue);
//				    String str="yea";//valueClass.getName();
//				    String fileName="C:\\lucee\\luceedebug.txt";
//					adapter.pop2();
//				    try {
//					    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
//					    writer.write(str);
//					    writer.close();
//				    } catch (IOException e) {
//					    e.printStackTrace();
//				    }
//				    adapter.dup();
//				    adapter.storeLocal(doubleValue);
//				    adapter.invokeInterface(TypeScope.SCOPES[variable.getScope()], METHOD_SCOPE_SET_KEY);
//				    adapter.swap();
//				    adapter.dup();
//				    adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDouble", "Ljava/lang/Double;");

				    // this is good, since it converts object to Double

//				    adapter.loadLocal(doubleValue);
//				    adapter.checkCast(Types.DOUBLE);
//				    adapter.loadLocal(5);
//				    adapter.visitVarInsn(Opcodes.ALOAD, 5);

//		            adapter.visitLdcInsn(new Double("5.0"));
//				    adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");

				    adapter.dup();
				    adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDouble", "Ljava/lang/Double;");
//				    adapter.loadLocal(doubleValue);
//					adapter.pop2();
//					adapter.swap();
//					adapter.pop();
//				    adapter.pop();
//				    adapter.dup();
//				    adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDouble", "Ljava/lang/Double;");

				    // then refer to that value on the next line
//				    Member valueMember=variableValue.members.get(0);
//				    if(valueMember instanceof DataMember){
//					    DataMember valueDataMember=(DataMember)valueMember;
//
//					    valueDataMember.getName()
//				    }
//				    Member member2=variable.getMembers().get(0);
//				    if(member instanceof DataMember) {
//				    }
//				    adapter.dup();
//				    adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
//
//				    adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDouble", "Ljava/lang/Double;");


			    }else {
				    Class valueClass=value.writeOut(bc, MODE_VALUE);
				    // adapter.valueOf(); // this might be better for all types
				    adapter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");

				    // we have to duplicate the value so that we can both put and return it
				    adapter.dup();
				    adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDouble", "Ljava/lang/Double;");
			    }

				// this works for debugging bytecode objects that execute, but not if the bytecode fails, thats why we did file instead.
//			    String valueClassName=valueClass.toString();
//			    adapter.visitLdcInsn(valueClassName);
//			    adapter.visitFieldInsn(Opcodes.PUTSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberString", "Ljava/lang/String;");
//			}else{
				// do something else?
//			}
//		    adapter.visitFieldInsn(Opcodes.GETSTATIC, "lucee/runtime/type/scope/JetendoImpl", "memberDouble", "Ljava/lang/Double;");
//		    adapter.pop();
//		    adapter.visitFrame(Opcodes.F_NEW, 0, null, 0, null);
//		    value.writeOut(bc, MODE_REF);
			    //writeOutPutScopeField(adapter, JetendoImpl.class, Scope.SCOPE_JETENDO, memberName);
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