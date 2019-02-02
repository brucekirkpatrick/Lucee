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
package lucee.cli.servlet;


import javax.servlet.ServletContext;


public class HTTPServletImplDead extends HttpServlet {
    private static final long serialVersionUID = 3270816399105433603L;

    private final ServletConfigDead config;
    private final ServletContext context;
    private final String servletName;

    public HTTPServletImplDead(final ServletConfigDead config, final ServletContext context, final String servletName) {
	this.config = config;
	this.context = context;
	this.servletName = servletName;
    }

    /**
     * @see javax.servlet.GenericServlet#getServletConfigDead()
     */
    @Override
    public ServletConfigDead getServletConfigDead() {
	return config;
    }

    /**
     * @see javax.servlet.GenericServlet#getServletContext()
     */
    @Override
    public ServletContext getServletContext() {
	return context;
    }

    /**
     * @see javax.servlet.GenericServlet#getServletName()
     */
    @Override
    public String getServletName() {
	return servletName;
    }

}