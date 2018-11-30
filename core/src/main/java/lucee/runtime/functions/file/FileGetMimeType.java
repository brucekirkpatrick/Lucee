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
package lucee.runtime.functions.file;

import lucee.commons.io.res.Resource;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.lang.StringUtil;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Caster;

public class FileGetMimeType {
    public static String call(PageContext pc, Object oSrc) throws PageException {
	return call(pc, oSrc, true);
    }

    public static String call(PageContext pc, Object oSrc, boolean checkHeader) throws PageException {
		String mimeType;
		if(oSrc instanceof Resource){
			Resource src = Caster.toResource(pc, oSrc, false);
			if(checkHeader) {
				pc.getConfig().getSecurityManager().checkFileLocation(src);
				mimeType = ResourceUtil.getMimeType(src, null);
			}else {
				mimeType = ResourceUtil.getMimeType(src.getAbsolutePath(), null);
			}
		}else if(oSrc instanceof String){
			mimeType = ResourceUtil.getMimeType((String) oSrc, null);
		}else{
			throw new RuntimeException("Input must be a file that exists or a string file path.");
		}
		if (StringUtil.isEmpty(mimeType, true)) return "application/octet-stream";
		return mimeType;
    }
}