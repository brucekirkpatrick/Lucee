package lucee.runtime.net.rpc.ref;

import java.lang.reflect.Method;

import coreLoad.RequestResponseImpl;


import lucee.runtime.Component;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.exp.PageRuntimeException;
import lucee.runtime.net.rpc.WSHandler;
import lucee.runtime.net.rpc.server.WSServer;
import lucee.runtime.op.Caster;

public class WSServerReflector implements WSServer {

    private final Object obj;
    private final Class<? extends Object> clazz;
    private Method doGet;
    private Method doPost;
    private Method invoke;
    private Method registerTypeMapping;
    private Method getWSHandler;

    public WSServerReflector(Object obj) {
	this.obj = obj;
	this.clazz = obj.getClass();
    }

    @Override
    public void doGet(PageContext pc, RequestResponse request, Component component) throws PageException {
	try {
	    if (doGet == null) doGet = clazz.getMethod("doGet", new Class[] { PageContext.class, HttpServletRequestDead.class, HttpServletResponseDead.class, Component.class });
	    doGet.invoke(obj, new Object[] { pc, request, response, component });
	}
	catch (Exception e) {
	    throw Caster.toPageException(e);
	}
    }

    @Override
    public void doPost(PageContext pc, RequestResponse req, HttpServletResponseDead res, Component component) throws PageException {
	try {
	    if (doPost == null) doPost = clazz.getMethod("doPost", new Class[] { PageContext.class, HttpServletRequestDead.class, HttpServletResponseDead.class, Component.class });
	    doPost.invoke(obj, new Object[] { pc, req, res, component });
	}
	catch (Exception e) {
	    throw Caster.toPageException(e);
	}
    }

    @Override
    public Object invoke(String name, Object[] args) throws PageException {
	try {
	    if (invoke == null) invoke = clazz.getMethod("invoke", new Class[] { String.class, Object[].class });
	    return invoke.invoke(obj, new Object[] { name, args });
	}
	catch (Exception e) {
	    throw Caster.toPageException(e);
	}
    }

    @Override
    public void registerTypeMapping(Class clazz) {
	try {
	    if (registerTypeMapping == null) registerTypeMapping = clazz.getMethod("registerTypeMapping", new Class[] { Class.class });
	    registerTypeMapping.invoke(obj, new Object[] { clazz });
	}
	catch (Exception e) {
	    throw new PageRuntimeException(e);
	}
    }

    @Override
    public WSHandler getWSHandler() {
	try {
	    if (getWSHandler == null) getWSHandler = clazz.getMethod("getWSHandler", WSHandlerReflector.EMPTY_CLASS);
	    return (WSHandler) getWSHandler.invoke(obj, WSHandlerReflector.EMPTY_OBJECT);
	}
	catch (Exception e) {
	    throw new PageRuntimeException(e);
	}
    }

}
