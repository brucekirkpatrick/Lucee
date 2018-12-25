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

import lucee.runtime.Component;
import lucee.runtime.PageContext;
import lucee.runtime.PageContextImpl;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.functions.other.CreateObject;
import lucee.runtime.type.*;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static lucee.runtime.Component.ACCESS_PRIVATE;

/**
 * Server Scope
 */
public final class JetendoImpl extends ScopeSupport implements Jetendo, SharedScope {

    private static final long serialVersionUID = -6965340514668753444L;
    /* real jetendo fields */
    public static Component template=null;
    public static UDFPlus templateSetTag=null;
    public static UDFImpl templateGetString=null;


    /*
    TODO: many cfcs and java static instances cached here
     */
    public static Integer memberInt=1;
    public static Double memberDoubleStatic=1.0;
    public Double memberDouble=1.0;
    public Double memberDoubleCount=1.0;
    public static Boolean memberBool=true;
    public String memberString2="test";
    public Boolean memberBool2=true;
    public boolean memberBool3=true;
    public static Double loopIndex=1.0D;
    public static ConcurrentHashMap<String, Object> memberMap=new ConcurrentHashMap<>();
    public static String memberString="jetendo scope works";

    public PageContextImpl pageContext;

    public Boolean memberBoolFunc(){
        return memberBool2;
    }
    public static Boolean memberBoolFuncStatic(){
        return memberBool;
    }
    public static Double addOne(){
        return loopIndex++;
    }
    public double memberArgFunc(double v){
        return memberInt+v;
    }
    private static Field[] fields;
    public JetendoImpl(){
        super("jetendo", SCOPE_JETENDO, Struct.TYPE_REGULAR);
    }
    /**
     * constructor of the server scope
     *
     * @param pc
     */
    public JetendoImpl(PageContext pc, boolean jsr223) {
        super("jetendo", SCOPE_JETENDO, Struct.TYPE_REGULAR);
        reload(pc, jsr223);
        pageContext=(PageContextImpl) pc;
        fields=this.getClass().getDeclaredFields();
    }
    public Object reloadComponents(){
        try {
            template=pageContext.loadComponent("zcorerootmapping.com.zos.template");
            templateSetTag=(UDFPlus) template.getMember(ACCESS_PRIVATE, new KeyImpl("setTag"), false, false);
            templateGetString=(UDFImpl) template.getMember(ACCESS_PRIVATE, new KeyImpl("getString"), false, false);
            //(Component) CreateObject.call(pageContext, "component", "zcorerootmapping.com.zos.template", null, null);
        } catch (PageException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    public Object getJavaString() throws PageException {
        return "1";
    }
    public Object getString() throws PageException {
//        return tag+value;
        return templateGetString._callSimple(pageContext, new KeyImpl("getString"), new Object[]{}, null, false);
//        return templateGetString.call(pageContext, new Object[]{}, false);
    }
    public Object setTag(String tag, String value) throws PageException {
//        return tag+value;
        templateSetTag.call(pageContext, new Object[]{ tag, value}, false);
        return true;
    }
    public static Object getObject(ConcurrentHashMap<String, Object> obj, Double key){
        return obj.get(key.toString());
    }
    public static Object getObject(ConcurrentHashMap<String, Object> obj, String key){
        return obj.get(key);
    }
    public static Object getObject(Object obj, String key){
        if(obj instanceof Map){
            return ((Map)obj).get(key);
        }
        return null;
    }
    public static Object getObject(Object obj, Key key){
        if(obj instanceof Map){
            return ((Map)obj).get(key.toString());
        }
        return null;
    }
    public static Object putObject(ConcurrentHashMap<String, Object> obj, String key, Object value){
        return obj.put(key, value);
    }
    public static Object putObject(ConcurrentHashMap<String, Object> obj, Double key, Object value){
        return obj.put(key.toString(), value);
    }
    public static Object putObject(ConcurrentHashMap<String, Object> obj, Double key, Double value){
        return obj.put(key.toString(), value);
    }
    public static Object putObject(Object obj, String key, Object value){
        if(obj instanceof Map){
            return ((Map)obj).put(key, value);
        }
        return null;
    }
    public static Object putObject(Object obj, Key key, Object value){
        if(obj instanceof Map){
            return ((Map)obj).put(key.toString(), value);
        }
        return null;
    }
    public static String concatThree(String s1, String s2, String s3){
        return new StringBuilder(s1).append(s2).append(s3).toString();
    }
    public static Double addOneMemberDouble(){
        return memberDoubleStatic++;
    }
    public static Integer intValue(Double value){
        return value.intValue();
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