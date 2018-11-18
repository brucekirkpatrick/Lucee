/**
 * Copyright (c) 2015, Lucee Assosication Switzerland. All rights reserved.
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
package lucee.runtime.functions.other;

import lucee.runtime.Component;
import lucee.runtime.ComponentImpl;
import lucee.runtime.PageContext;
import lucee.runtime.component.ComponentLoader;
import lucee.runtime.exp.FunctionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;

public class ReloadComponent extends BIF {
    public static Component call(PageContext pc, Component component) throws PageException {
        return ComponentLoader.reloadComponent(pc, (ComponentImpl) component, false);
    }
    public static Component call(PageContext pc, Component component, boolean onlyIfChanged) throws PageException {
        return ComponentLoader.reloadComponent(pc, (ComponentImpl) component, onlyIfChanged);
    }
    public static Component call(PageContext pc, ComponentImpl component) throws PageException {
        return ComponentLoader.reloadComponent(pc, component, false);
    }
    public static Component call(PageContext pc, ComponentImpl component, boolean onlyIfChanged) throws PageException {
        return ComponentLoader.reloadComponent(pc, component, onlyIfChanged);
    }
    @Override
    public Object invoke(PageContext pc, Object[] args) throws PageException {
        if(args.length==2) {
            return call(pc, (ComponentImpl) args[0], (boolean) args[1]);
        }else if(args.length==1){
                return call(pc, (ComponentImpl) args[0]);
        }else{
            throw new FunctionException(pc, "ReloadComponent", 1, 2, args.length);
        }
    }
}