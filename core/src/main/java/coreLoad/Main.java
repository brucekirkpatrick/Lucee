package coreLoad;

import lucee.commons.io.res.Resource;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.lang.ExceptionUtil;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.CFMLFactory;
import lucee.runtime.CFMLFactoryImpl;
import lucee.runtime.PageContext;
import lucee.runtime.PageContextImpl;
import lucee.runtime.config.*;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.scope.ScopeContext;


import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class Main {

	/*
	 * Config
	 *
	 * webroot - webroot directory servlet-name - name of the servlet (default:CFMLServlet) server-name
	 * - server name (default:localhost) uri - host/scriptname/query cookie - cookies (same pattern as
	 * query string) form - form (same pattern as query string)
	 */

	public static void main(final String[] args) throws PageException {
		// TODO: it complains that engine is not inited, but i think it only needs 1 or 2 directories hardcoded to work in OSGI/felix

		// TODO: need to make CFMLEngineFactory able to work without an LCO, just the local java classes!
		PageContextImpl pc;

		CFMLEngineFactory.getInstance(null);

		System.out.println("I run");
		String servletPath="C:\\ServerData\\Lucee5\\webroot\\";
//			File root = new File(factory.engine.getCFMLEngineFactory().getResourceRoot(), "jsr223-webroot");
		File root= new File(servletPath);
		CFMLFactoryImpl factory=null;
		ScopeContext scopeContext=new ScopeContext(factory);

		Resource configDir=ResourceUtil.toResource(new File("C:\\ServerData\\Lucee5\\webroot\\"));
		Resource configFile=ResourceUtil.toResource(new File("C:\\ServerData\\Lucee5\\webroot\\config.xml"));
		ConfigWebImpl config=new ConfigWebImpl(configDir, configFile);//(null, null, null, configDir, configFile);
		int idCounter=1;

		pc = new PageContextImpl(scopeContext, null, idCounter++, null, false);
		RequestResponse requestResponse=new RequestResponseImpl();
		pc.initialize(null, null, null, "/error.cfm", false, 4096, true, false, false);
		//RequestResponse requestResponse, String errorPageURL, boolean needsSession, int bufferSize,	boolean autoFlush, boolean isChild, boolean ignoreScopes
//			pc=(PageContextImpl) getPageContext(null, null, root, "localhost", "/index.cfm", "", null, null, null, null, System.out, false, Long.MAX_VALUE,
//					Caster.toBooleanValue(SystemUtil.getSystemPropOrEnvVar("lucee.ignore.scopes", null), false));



//		ThreadQueue queue = null;
//			if (registerWithThread) ThreadLocalPageContext.register(pc);
//			ThreadQueue tmp = pc.getConfig().getThreadQueue();
//			tmp.enter(pc);
//			queue = tmp;



		pc.executeCFML(servletPath, true, true);
		// pc.getRequestResponse().getServletPath()

	}

	public static PageContext getPageContext(Config config, ServletConfigDead ServletConfigDead, File contextRoot, String host, String scriptName, String queryString, Cookie[] cookies, Map<String, Object> headers, Map<String, String> parameters, Map<String, Object> attributes, OutputStream os, boolean register, long timeout, boolean ignoreScopes) throws ServletException {
		boolean callOnStart = (Boolean)ThreadLocalPageContext.callOnStart.get();

		PageContext var33;
		try {
			ThreadLocalPageContext.callOnStart.set(false);
			if (contextRoot == null) {
				contextRoot = new File(".");
			}

			CFMLEngine engine = null;

			try {
				engine = CFMLEngineFactory.getInstance();
			} catch (Throwable var30) {
				ExceptionUtil.rethrowIfNecessary(var30);
			}

//			if (engine == null) {
//				throw new ServletException("there is no ServletContext");
//			}

			if (headers == null) {
				headers = new HashMap();
			}

			if (parameters == null) {
				parameters = new HashMap();
			}

			if (attributes == null) {
				attributes = new HashMap();
			}

//			RequestResponse req = CreationImpl.getInstance(engine).createHttpServletRequestDead(contextRoot, host, scriptName, queryString, cookies, (Map)headers, (Map)parameters, (Map)attributes, (HttpSession)null);
//			RequestResponse req = CreationImpl.getInstance(engine).createHttpServletResponseDead(os);
			if (config == null) {
				config = ThreadLocalPageContext.getConfig();
			}

			CFMLFactory factory = null;
			Object servlet;
			if (config instanceof ConfigWeb) {
				ConfigWeb cw = (ConfigWeb)config;
				factory = cw.getFactory();
//				servlet = factory.getServlet();
			} else {
//				if (ServletConfigDead == null) {
//					ServletConfigDead[] configs = engine.getServletConfigDeads();
//					String rootDir = contextRoot.getAbsolutePath();
//					ServletConfigDead[] var23 = configs;
//					int var24 = configs.length;
//
//					for(int var25 = 0; var25 < var24; ++var25) {
//						ServletConfigDead conf = var23[var25];
//						if (SystemUtil.arePathsSame(rootDir, conf.getServletContext().getRealPath("/"))) {
//							ServletConfigDead = conf;
//							break;
//						}
//					}
//
//					if (ServletConfigDead == null) {
//						ServletConfigDead = configs[0];
//					}
//				}

//				factory = engine.getCFMLFactory(ServletConfigDead, req);
//				servlet = new HTTPServletImplDead(ServletConfigDead, ServletConfigDead.getServletContext(), ServletConfigDead.getServletName());
			}
//			ConfigWebImpl config=new ConfigWebImpl();
//			new PageContextImpl( config, int id, boolean jsr223);
//			var33 = factory.getLuceePageContext((HttpServlet)servlet, req, (String)null, false, -1, false, register, timeout, false, ignoreScopes);
		} finally {
			ThreadLocalPageContext.callOnStart.set(callOnStart);
		}

		return null;// var33;
	}
}
