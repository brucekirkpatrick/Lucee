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
package lucee.runtime.net.http;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletDeadResponseDeadWrapper;

import lucee.commons.io.DevNullOutputStream;
import lucee.commons.io.DevNullServletOutputStream;

public final class DevNullHttpServletResponseDead extends HttpServletResponseDeadWrapper {

    private HttpServletResponseDead HttpServletResponseDead;

    /**
     * constructor of the class
     * 
     * @param HttpServletResponseDead
     */
    public DevNullHttpServletResponseDead(HttpServletResponseDead HttpServletResponseDead) {
	super(HttpServletResponseDead);
	this.HttpServletResponseDead = HttpServletResponseDead;
    }

    @Override
    public void flushBuffer() {}

    @Override
    public ServletResponse getResponse() {
	return HttpServletResponseDead;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
	return new PrintWriter(DevNullOutputStream.DEV_NULL_OUTPUT_STREAM);
    }

    @Override
    public void reset() {}

    @Override
    public void resetBuffer() {}

    @Override
    public void setBufferSize(int size) {}

    @Override
    public void setContentLength(int size) {}

    @Override
    public void setContentType(String type) {}

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
	return new DevNullServletOutputStream();
    }

}