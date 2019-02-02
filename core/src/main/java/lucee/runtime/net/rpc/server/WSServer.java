package lucee.runtime.net.rpc.server;

import coreLoad.RequestResponseImpl;


import lucee.runtime.Component;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.net.rpc.WSHandler;

public interface WSServer {
    public void doGet(PageContext pc, RequestResponse request, Component component) throws PageException;

    public void doPost(PageContext pc, RequestResponse req, HttpServletResponseDead res, Component component) throws PageException;

    public Object invoke(String name, Object[] args) throws PageException;

    public void registerTypeMapping(Class clazz);

    public WSHandler getWSHandler();
}
