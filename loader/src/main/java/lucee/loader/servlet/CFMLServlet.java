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
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lucee.loader.engine.CFMLEngineFactory;

/**
 */
public class CFMLServlet extends AbsServlet {
	public static long startEngineTime=System.currentTimeMillis();

	public static class StartTimeEvent{
		public long time;
		public String message;
		public StartTimeEvent(String message){
			this.time=System.currentTimeMillis();
			this.message=message;
		}
	}
	public static ArrayList<StartTimeEvent> startTimes=new ArrayList<>();
	public static void logStartTime(String message){
		startTimes.add(new StartTimeEvent(message+"\n"));
	}
    private static final long serialVersionUID = -1878214660283329587L;

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(final ServletConfig sg) throws ServletException {
    	CFMLServlet.logStartTime("CFMLServlet init start");
	super.init(sg);
	    CFMLServlet.logStartTime("CFMLServlet init Servlet init end");
	    Thread cfmlServletEngine=new Thread(()->{
		    try {
			    engine = CFMLEngineFactory.getInstance(sg, this);
		    } catch (ServletException e) {
			    throw new RuntimeException(e);
		    }
	    });
	    cfmlServletEngine.start();
	    CFMLServlet.logStartTime("CFMLServlet init CFML factory instance end");
    }

    /**
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse rsp) throws ServletException, IOException {
	    CFMLServlet.logStartTime("CFMLServlet service start");
	    while(engine==null){
	    	Thread.yield();
	    }
		engine.serviceCFML(this, req, rsp);
	    CFMLServlet.logStartTime("CFMLServlet service end");
    }
}