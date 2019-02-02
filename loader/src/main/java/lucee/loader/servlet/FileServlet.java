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
package lucee.loader.servlet;

import java.io.IOException;


import javax.servlet.ServletException;
import coreLoad.RequestResponse;


import lucee.loader.engine.CFMLEngineFactory;

/**
 */
public class FileServlet extends AbsServlet {

    private static final long serialVersionUID = 1555107078656945805L;

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfigDead)
     */
    @Override
    public void init(final ServletConfigDead sg) throws ServletException {
	super.init(sg);
	engine = CFMLEngineFactory.getInstance(this);
    }

    /**
     * @see javax.servlet.http.HttpServletDead#service(javax.servlet.http.HttpServletDeadRequestDead,
     *      javax.servlet.http.HttpServletDeadResponseDead)
     */
    @Override
    protected void service(final RequestResponse req) throws ServletException, IOException {
//	engine.serviceFile(this, req, rsp);
    }
}