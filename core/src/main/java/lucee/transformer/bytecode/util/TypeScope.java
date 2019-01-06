/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
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
 **/
package lucee.transformer.bytecode.util;

import lucee.runtime.type.scope.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public final class TypeScope {

    public static int SCOPE_UNDEFINED_LOCAL = 17;

    public final static Type SCOPE = Type.getType(Scope.class);
    public final static Type[] SCOPES = new Type[ScopeSupport.SCOPE_COUNT];
    static {
	SCOPES[Scope.SCOPE_APPLICATION] = Type.getType(Application.class);
	SCOPES[Scope.SCOPE_ARGUMENTS] = Type.getType(Local.class);
	SCOPES[Scope.SCOPE_CGI] = Type.getType(CGI.class);
	SCOPES[Scope.SCOPE_CLIENT] = Type.getType(Client.class);
	SCOPES[Scope.SCOPE_COOKIE] = Type.getType(Cookie.class);
	SCOPES[Scope.SCOPE_FORM] = Type.getType(Form.class);
	SCOPES[Scope.SCOPE_LOCAL] = Type.getType(Local.class);
	SCOPES[Scope.SCOPE_REQUEST] = Type.getType(Request.class);
	SCOPES[Scope.SCOPE_SERVER] = Type.getType(Server.class);
	SCOPES[Scope.SCOPE_SESSION] = Type.getType(Session.class);
	SCOPES[Scope.SCOPE_UNDEFINED] = Type.getType(Undefined.class);
	SCOPES[Scope.SCOPE_URL] = Type.getType(URL.class);
	SCOPES[Scope.SCOPE_VARIABLES] = Types.VARIABLES;
//	SCOPES[Scope.SCOPE_CLUSTER] = Type.getType(Cluster.class);
	SCOPES[Scope.SCOPE_VAR] = SCOPES[Scope.SCOPE_LOCAL];
	SCOPES[Scope.SCOPE_JETENDO] = Type.getType(Jetendo.class);
	// SCOPES[SCOPE_UNDEFINED_LOCAL]= SCOPES[Scope.SCOPE_LOCAL];
    }
	public final static Type[] SCOPES_IMPL = new Type[ScopeSupport.SCOPE_COUNT];
	static {
		SCOPES_IMPL[Scope.SCOPE_APPLICATION] = Type.getType(ApplicationImpl.class);
		SCOPES_IMPL[Scope.SCOPE_ARGUMENTS] = Type.getType(LocalImpl.class);
		SCOPES_IMPL[Scope.SCOPE_CGI] = Type.getType(CGIImplReadOnly.class);
		SCOPES_IMPL[Scope.SCOPE_CLIENT] = Type.getType(Client.class);
		SCOPES_IMPL[Scope.SCOPE_COOKIE] = Type.getType(CookieImpl.class);
		SCOPES_IMPL[Scope.SCOPE_FORM] = Type.getType(UrlFormImpl.class);
		SCOPES_IMPL[Scope.SCOPE_LOCAL] = Type.getType(LocalImpl.class);
		SCOPES_IMPL[Scope.SCOPE_REQUEST] = Type.getType(RequestImpl.class);
		SCOPES_IMPL[Scope.SCOPE_SERVER] = Type.getType(ServerImpl.class);
		SCOPES_IMPL[Scope.SCOPE_SESSION] = Type.getType(Session.class);
		SCOPES_IMPL[Scope.SCOPE_UNDEFINED] = Type.getType(UndefinedImpl.class);
		SCOPES_IMPL[Scope.SCOPE_URL] = Type.getType(UrlFormImpl.class);
		SCOPES_IMPL[Scope.SCOPE_VARIABLES] = Type.getType(VariablesImpl.class);
//		SCOPES_IMPL[Scope.SCOPE_CLUSTER] = Type.getType(Cluster.class);
		SCOPES_IMPL[Scope.SCOPE_VAR] = SCOPES_IMPL[Scope.SCOPE_LOCAL];
		SCOPES_IMPL[Scope.SCOPE_JETENDO] = Type.getType(JetendoImpl.class);
		// SCOPES_IMPL[SCOPE_UNDEFINED_LOCAL]= SCOPES_IMPL[Scope.SCOPE_LOCAL];
	}


	public final static Method[] METHODS = new Method[ScopeSupport.SCOPE_COUNT + 1];
    static {
	METHODS[Scope.SCOPE_APPLICATION] = new Method("applicationScope", SCOPES[Scope.SCOPE_APPLICATION], new Type[] {});
	METHODS[Scope.SCOPE_ARGUMENTS] = new Method("argumentsScope", SCOPES[Scope.SCOPE_LOCAL], new Type[] {});
	METHODS[Scope.SCOPE_CGI] = new Method("cgiScope", SCOPES[Scope.SCOPE_CGI], new Type[] {});
	METHODS[Scope.SCOPE_CLIENT] = new Method("clientScope", SCOPES[Scope.SCOPE_CLIENT], new Type[] {});
	METHODS[Scope.SCOPE_COOKIE] = new Method("cookieScope", SCOPES[Scope.SCOPE_COOKIE], new Type[] {});
	METHODS[Scope.SCOPE_FORM] = new Method("formScope", SCOPES[Scope.SCOPE_FORM], new Type[] {});
	METHODS[Scope.SCOPE_LOCAL] = new Method("localGet", Types.OBJECT, new Type[] {});
	METHODS[Scope.SCOPE_REQUEST] = new Method("requestScope", SCOPES[Scope.SCOPE_REQUEST], new Type[] {});
	METHODS[Scope.SCOPE_SERVER] = new Method("serverScope", SCOPES[Scope.SCOPE_SERVER], new Type[] {});
	METHODS[Scope.SCOPE_SESSION] = new Method("sessionScope", SCOPES[Scope.SCOPE_SESSION], new Type[] {});
	METHODS[Scope.SCOPE_UNDEFINED] = new Method("us", SCOPES[Scope.SCOPE_UNDEFINED], new Type[] {});
	METHODS[Scope.SCOPE_URL] = new Method("urlScope", SCOPES[Scope.SCOPE_URL], new Type[] {});
	METHODS[Scope.SCOPE_VARIABLES] = new Method("variablesScope", SCOPES[Scope.SCOPE_VARIABLES], new Type[] {});
//	METHODS[Scope.SCOPE_CLUSTER] = new Method("clusterScope", SCOPES[Scope.SCOPE_CLUSTER], new Type[] {});
	METHODS[Scope.SCOPE_VAR] = new Method("localScope", SCOPES[Scope.SCOPE_VAR], new Type[] {});
	METHODS[Scope.SCOPE_JETENDO] = new Method("jetendoScope", SCOPES[Scope.SCOPE_JETENDO], new Type[] {});
	METHODS[SCOPE_UNDEFINED_LOCAL] = new Method("usl", SCOPE, new Type[] {});
    }
    // Argument argumentsScope (boolean)
    public final static Method METHOD_ARGUMENT_BIND = new Method("argumentsScope", SCOPES[Scope.SCOPE_LOCAL], new Type[] { Types.BOOLEAN_VALUE });
    public final static Method METHOD_VAR_BIND = new Method("localScope", SCOPES[ScopeSupport.SCOPE_VAR], new Type[] { Types.BOOLEAN_VALUE });

    public final static Method METHOD_LOCAL_EL = new Method("localGet", Types.OBJECT, new Type[] { Types.BOOLEAN_VALUE, Types.OBJECT });
    public final static Method METHOD_LOCAL_BIND = new Method("localGet", Types.OBJECT, new Type[] { Types.BOOLEAN_VALUE });
    public final static Method METHOD_LOCAL_TOUCH = new Method("localTouch", Types.OBJECT, new Type[] {});

    // public final static Method METHOD_THIS_BINDX=new Method("thisGet",Types.OBJECT,new
    // Type[]{Types.BOOLEAN_VALUE});
    // public final static Method METHOD_THIS_TOUCHX=new Method("thisTouch", Types.OBJECT,new Type[]{});

    public final static Type SCOPE_ARGUMENT = Type.getType(Local.class);

	public static Type invokeScope(GeneratorAdapter adapter, int scope) {
//		if(scope==Scope.SCOPE_APPLICATION){// || scope==Scope.SCOPE_REQUEST){// || scope==Scope.SCOPE_CGI || scope==Scope.SCOPE_COOKIE) {//
//			adapter.invokeVirtual(Types.PAGE_CONTEXT, TypeScope.METHODS[scope]);
//		}else {
			if(scope == SCOPE_UNDEFINED_LOCAL){
				scope=Scope.SCOPE_UNDEFINED;
			}
//			adapter.checkCast(Types.PAGE_CONTEXT_IMPL);
			adapter.getField(Types.PAGE_CONTEXT_IMPL, ScopeFactory.toStringScope(scope, "undefined"), SCOPES[scope]);
//		}
		return SCOPES[scope];
	}

//	public static Type scopeField(GeneratorAdapter adapter, int scope) {
//		if(scope==Scope.SCOPE_APPLICATION || scope==Scope.SCOPE_CGI || scope==Scope.SCOPE_REQUEST || scope==Scope.SCOPE_COOKIE) {
//			invokeScope(adapter, TypeScope.METHODS[scope], Types.PAGE_CONTEXT);
//		}else {
//			if(scope == SCOPE_UNDEFINED_LOCAL){
//				scope=Scope.SCOPE_UNDEFINED;
//			}
//			adapter.getField(Types.PAGE_CONTEXT_IMPL, ScopeFactory.toStringScope(scope, "undefined"), SCOPES[scope]);
//		}
//		return SCOPES[scope];
//	}
//    public static Type invokeScope(GeneratorAdapter adapter, int scope) {
//	if (scope == SCOPE_UNDEFINED_LOCAL) {
//	    adapter.checkCast(Types.PAGE_CONTEXT_IMPL);
//	    return invokeScope(adapter, TypeScope.METHODS[scope], Types.PAGE_CONTEXT_IMPL);
//	}
//	else return invokeScope(adapter, TypeScope.METHODS[scope], Types.PAGE_CONTEXT);
//    }

    public static Type invokeScope(GeneratorAdapter adapter, Method m, Type type) {
	if (type == null) type = Types.PAGE_CONTEXT;
	adapter.invokeVirtual(type, m);
	return m.getReturnType();
    }

}