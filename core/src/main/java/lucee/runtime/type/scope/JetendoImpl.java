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
package lucee.runtime.type.scope;

import lucee.runtime.PageContext;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Struct;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server Scope
 */
public final class JetendoImpl extends ScopeSupport implements Jetendo, SharedScope {
    /*
    TODO: many cfcs and java static instances cached here
     */
    public static int memberInt=1;
    public static Boolean memberBool=true;
    public String memberString2="test";
    public Boolean memberBool2=true;
    public boolean memberBool3=true;
    public static ConcurrentHashMap<String, Object> memberMap=new ConcurrentHashMap<>();
    public static String memberString="jetendo scope works";

    public Boolean memberBoolFunc(){
        memberBool2=false;
        memberString2="stuff";
        memberBool3=false;
        return memberBool;
    }
    public double memberArgFunc(double v){
        return memberInt+v;
    }
    private static Field[] fields;
    /**
     * constructor of the server scope
     *
     * @param pc
     */
    public JetendoImpl(PageContext pc, boolean jsr223) {
        super("jetendo", SCOPE_JETENDO, Struct.TYPE_LINKED);
        reload(pc, jsr223);
        fields=this.getClass().getDeclaredFields();
    }

    public static Field getField(String name){
        for(Field f:fields){
            if(f.getName().equalsIgnoreCase(name)){
                //stuff.
                return f;
            }
        }
        return null;
    }
    public static CallSite bsmCreateCallCallingtargetMethod(MethodHandles.Lookup caller, String name, MethodType type, MethodHandle mh) throws Throwable {
        return new ConstantCallSite(mh);
    }

    @Override
    public void reload() {
        reload(ThreadLocalPageContext.get());
    }

    public void reload(PageContext pc) {
        reload(pc, false);
    }

    public void reload(PageContext pc, Boolean jsr223) {
        // TODO: good place to load all the components and such
    }

    @Override
    public Object set(Collection.Key key, Object value) throws PageException {
        throw new ExpressionException("Jetendo scope is read only");
    }

    @Override
    public Object setEL(Collection.Key key, Object value) {
        return value;
    }

    @Override
    public Object get(Key key, Object defaultValue) {
        return super.get(key, defaultValue);
    }

    @Override
    public Object g(Key key, Object defaultValue) {
        return super.g(key, defaultValue);
    }

    @Override
    public Object g(Key key) throws PageException {
        return super.g(key);
    }

    @Override
    public Object get(Key key) throws PageException {
        return super.get(key);
    }

    @Override
    public void touchBeforeRequest(PageContext pc) {
        // do nothing
    }

    @Override
    public void touchAfterRequest(PageContext pc) {
        // do nothing
    }
}