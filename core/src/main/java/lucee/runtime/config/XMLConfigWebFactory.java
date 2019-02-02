/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Assosication Switzerland
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package lucee.runtime.config;

import static lucee.runtime.config.Password.ORIGIN_ENCRYPTED;
import static lucee.runtime.db.DatasourceManagerImpl.QOQ_DATASOURCE_NAME;
import static lucee.runtime.security.SecurityManager.VALUE_NO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;



import lucee.loader.servlet.CFMLServlet;
import lucee.runtime.crypt.BlowfishEasy;
import lucee.transformer.cfml.evaluator.impl.Static;
import lucee.transformer.library.function.FunctionLibFactory;
import lucee.transformer.library.tag.TagLibFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import lucee.Info;
import lucee.aprint;
import lucee.commons.collection.MapFactory;
import lucee.commons.date.TimeZoneConstants;
import lucee.commons.date.TimeZoneUtil;
import lucee.commons.digest.HashUtil;
import lucee.commons.digest.MD5;
import lucee.commons.io.CharsetUtil;
import lucee.commons.io.DevNullOutputStream;
import lucee.commons.io.FileUtil;
import lucee.commons.io.IOUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.log.Log;
import lucee.commons.io.log.LogUtil;
import lucee.commons.io.log.LoggerAndSourceData;
import lucee.commons.io.log.log4j.Log4jUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.ResourcesImpl;
import lucee.commons.io.res.type.cfml.CFMLResourceProvider;
import lucee.commons.io.res.type.s3.DummyS3ResourceProvider;
import lucee.commons.io.res.util.ResourceUtil;
import lucee.commons.lang.ByteSizeParser;
import lucee.commons.lang.ClassException;
import lucee.commons.lang.ClassUtil;
import lucee.commons.lang.ExceptionUtil;
import lucee.commons.lang.StringUtil;
import lucee.commons.lang.SystemOut;
import lucee.commons.net.URLDecoder;
import lucee.loader.engine.CFMLEngine;
import lucee.runtime.CFMLFactoryImpl;
import lucee.runtime.Component;
import lucee.runtime.Mapping;
import lucee.runtime.MappingImpl;
import lucee.runtime.cache.CacheConnection;
import lucee.runtime.cache.CacheConnectionImpl;
import lucee.runtime.cache.ServerCacheConnection;
import lucee.runtime.cache.tag.CacheHandler;
import lucee.runtime.cache.tag.request.RequestCacheHandler;
import lucee.runtime.cache.tag.timespan.TimespanCacheHandler;
import lucee.runtime.cfx.customtag.CFXTagClass;
import lucee.runtime.cfx.customtag.JavaCFXTagClass;
import lucee.runtime.component.ImportDefintion;
//import lucee.runtime.config.ajax.AjaxFactory;
import lucee.runtime.config.component.ComponentFactory;
import lucee.runtime.db.ClassDefinition;
import lucee.runtime.db.DataSource;
import lucee.runtime.db.DataSourceImpl;
import lucee.runtime.db.JDBCDriver;
import lucee.runtime.db.ParamSyntax;
import lucee.runtime.dump.ClassicHTMLDumpWriter;
import lucee.runtime.dump.DumpWriter;
import lucee.runtime.dump.DumpWriterEntry;
import lucee.runtime.dump.HTMLDumpWriter;
import lucee.runtime.dump.SimpleHTMLDumpWriter;
import lucee.runtime.dump.TextDumpWriter;
import lucee.runtime.engine.CFMLEngineImpl;
import lucee.runtime.engine.ConsoleExecutionLog;
import lucee.runtime.engine.ExecutionLog;
import lucee.runtime.engine.ExecutionLogFactory;
import lucee.runtime.engine.InfoImpl;
import lucee.runtime.engine.ThreadLocalConfig;
import lucee.runtime.engine.ThreadQueueImpl;
import lucee.runtime.engine.ThreadQueueNone;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.exp.SecurityException;
import lucee.runtime.extension.Extension;
import lucee.runtime.extension.ExtensionImpl;
import lucee.runtime.extension.ExtensionProvider;
import lucee.runtime.extension.ExtensionProviderImpl;
import lucee.runtime.extension.RHExtension;
import lucee.runtime.extension.RHExtensionProvider;
import lucee.runtime.gateway.GatewayEngineImpl;
import lucee.runtime.gateway.GatewayEntry;
import lucee.runtime.gateway.GatewayEntryImpl;
import lucee.runtime.listener.AppListenerUtil;
import lucee.runtime.listener.ApplicationContextSupport;
import lucee.runtime.listener.ApplicationListener;
import lucee.runtime.listener.MixedAppListener;
import lucee.runtime.listener.ModernAppListener;
import lucee.runtime.monitor.ActionMonitor;
import lucee.runtime.monitor.ActionMonitorCollector;
import lucee.runtime.monitor.ActionMonitorFatory;
import lucee.runtime.monitor.ActionMonitorWrap;
import lucee.runtime.monitor.AsyncRequestMonitor;
import lucee.runtime.monitor.IntervallMonitor;
import lucee.runtime.monitor.IntervallMonitorWrap;
import lucee.runtime.monitor.Monitor;
import lucee.runtime.monitor.RequestMonitor;
import lucee.runtime.monitor.RequestMonitorPro;
import lucee.runtime.monitor.RequestMonitorProImpl;
import lucee.runtime.monitor.RequestMonitorWrap;
import lucee.runtime.net.amf.AMFEngine;
import lucee.runtime.net.http.ReqRspUtil;
import lucee.runtime.net.mail.Server;
import lucee.runtime.net.mail.ServerImpl;
import lucee.runtime.net.proxy.ProxyData;
import lucee.runtime.net.proxy.ProxyDataImpl;
import lucee.runtime.op.Caster;
import lucee.runtime.op.Decision;
import lucee.runtime.op.date.DateCaster;
import lucee.runtime.orm.DummyORMEngine;
import lucee.runtime.orm.ORMConfiguration;
import lucee.runtime.orm.ORMConfigurationImpl;
import lucee.runtime.osgi.BundleInfo;
import lucee.runtime.osgi.OSGiUtil;
import lucee.runtime.reflection.Reflector;
import lucee.runtime.reflection.pairs.ConstructorInstance;
import lucee.runtime.search.DummySearchEngine;
import lucee.runtime.search.SearchEngine;
import lucee.runtime.security.SecurityManager;
import lucee.runtime.security.SecurityManagerImpl;
import lucee.runtime.spooler.SpoolerEngineImpl;
import lucee.runtime.tag.TagUtil;
import lucee.runtime.tag.listener.TagListener;
import lucee.runtime.text.xml.XMLCaster;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.dt.TimeSpan;
import lucee.runtime.type.scope.Cluster;
import lucee.runtime.type.scope.ClusterRemote;
import lucee.runtime.type.scope.Undefined;
import lucee.runtime.type.util.ArrayUtil;
import lucee.runtime.type.util.KeyConstants;
import lucee.runtime.type.util.ListUtil;
import lucee.runtime.video.VideoExecuter;
import lucee.transformer.library.ClassDefinitionImpl;
import lucee.transformer.library.function.FunctionLib;
import lucee.transformer.library.function.FunctionLibException;
import lucee.transformer.library.tag.TagLib;
import lucee.transformer.library.tag.TagLibException;

/**
 *
 */
public final class XMLConfigWebFactory extends XMLConfigFactory {

	private static final String TEMPLATE_EXTENSION = "cfm";
	private static final String COMPONENT_EXTENSION = "cfc";
	private static final String COMPONENT_EXTENSION_LUCEE = "lucee";
	private static final long GB1 = 1024 * 1024 * 1024;
	public static final boolean LOG = true;
	private static final int DEFAULT_MAX_CONNECTION = 100;

	/**
	 * creates a new ServletConfigDead Impl Object
	 *
	 * @param configServer
	 * @param configDir
	 * @param ServletConfigDead
	 * @return new Instance
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws PageException
	 * @throws IOException
	 * @throws TagLibException
	 * @throws FunctionLibException
	 * @throws NoSuchAlgorithmException
	 * @throws BundleException
	 */

	public static ConfigWebImpl newInstance(CFMLEngine engine, CFMLFactoryImpl factory, ConfigServerImpl configServer, Resource configDir, boolean isConfigDirACustomSetting)
			throws PageException {


		CFMLServlet.logStartTime("XMLConfigWebFactory web begin");
		String hash = SystemUtil.hash(ServletConfigDead.getServletContext());
		Map<String, String> labels = configServer.getLabels();
		String label = null;
		if (labels != null) {
			label = labels.get(hash);
		}
		if (label == null) label = hash;

		// make sure the web context does not point to the same directory as the server context
		if (configDir.equals(configServer.getConfigDir()))
			throw new ApplicationException("the web context [" + label + "] has defined the same configuration directory [" + configDir + "] as the server context");

		ConfigWeb[] webs = configServer.getConfigWebs();
		if (!ArrayUtil.isEmpty(webs)) {
			for (int i = 0; i < webs.length; i++) {
				// not sure this is necessary if(hash.equals(((ConfigWebImpl)webs[i]).getHash())) continue;
				if (configDir.equals(webs[i].getConfigDir())) throw new ApplicationException(
						"the web context [" + label + "] has defined the same configuration directory [" + configDir + "] as the web context [" + webs[i].getLabel() + "]");
			}
		}

//	SystemOut.print(SystemUtil.getPrintWriter(SystemUtil.OUT),
//		"===================================================================\n" + "WEB CONTEXT (" + label + ")\n"
//			+ "-------------------------------------------------------------------\n" + "- config:" + configDir + (isConfigDirACustomSetting ? " (custom setting)" : "")
//			+ "\n" + "- webroot:" + ReqRspUtil.getRootPath(ServletConfigDead.getServletContext()) + "\n" + "- hash:" + hash + "\n" + "- label:" + label + "\n"
//			+ "===================================================================\n"
//
//	);

		int iDoNew = doNew(engine, configDir, false).updateType;
		boolean doNew = iDoNew != NEW_NONE;

		Resource configFile = configDir.getRealResource("lucee-web.xml." + TEMPLATE_EXTENSION);

		String strPath = ServletConfigDead.getServletContext().getRealPath("/WEB-INF");
		Resource path = ResourcesImpl.getFileResourceProvider().getResource(strPath);

		// get config file
//	if (!configFile.exists()) {
//	    createConfigFile("web", configFile);
//	}

		Resource bugFile;
		int count = 1;

		final Document doc = null;//loadDocumentCreateIfFails(configFile, "web");

		// htaccess
//	if (path.exists()) createHtAccess(path.getRealResource(".htaccess"));
//	if (configDir.exists()) createHtAccess(configDir.getRealResource(".htaccess"));


		ExecutorService executor = Executors.newWorkStealingPool(8);
		ArrayList<Future<Boolean>> futures = new ArrayList<>();
		final ConfigWebImpl configWeb = new ConfigWebImpl(factory, configServer, ServletConfigDead, configDir, configFile);
//	    CFMLServlet.logStartTime("XMLConfigWebFactory web before load");

		if(false){
			try {
				load(configServer, configWeb, doc, false, doNew);
				configWeb.doc = doc;
				configServer.doc = doc;
				load2(configServer, configWeb, doc, true, doNew);
				load3(configServer, configWeb, doc, true, doNew);
				load4(configServer, configWeb, doc, true, doNew);
				load5(configServer, configWeb, doc, true, doNew);
				load6(configServer, configWeb, doc, true, doNew);
				load7(configServer, configWeb, doc, true, doNew);
				load8(configServer, configWeb, doc, true, doNew);
				load9(configServer, configWeb, doc, true, doNew);
				load10(configServer, configWeb, doc, true, doNew);
				loadPart2(configServer, configWeb, false, doNew);
				createContextFiles(configDir, ServletConfigDead, doNew);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}else {

			futures.add(executor.submit(() -> {
				load(configServer, configWeb, doc, false, doNew);
				configWeb.doc = doc;
				configServer.doc = doc;
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load2(configServer, configWeb, doc, true, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load3(configServer, configWeb, doc, true, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load4(configServer, configWeb, doc, true, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load5(configServer, configWeb, doc, true, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load6(configServer, configWeb, doc, true, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load7(configServer, configWeb, doc, true, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load8(configServer, configWeb, doc, true, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load9(configServer, configWeb, doc, true, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				load10(configServer, configWeb, doc, true, doNew);

				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				loadPart2(configServer, configWeb, false, doNew);
				return new Boolean(true);
			}));
			futures.add(executor.submit(() -> {
				createContextFiles(configDir, ServletConfigDead, doNew);
				return new Boolean(true);
			}));

			for (int i = 0; i < futures.size(); i++) {
				try {
					Boolean obj = futures.get(i).get();
					if (obj == null) {
						throw new RuntimeException("Invalid return type for one of the futures");
					}
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		}
		executor.shutdown();
		CFMLServlet.logStartTime("XMLConfigWebFactory web end");
		// call web.cfc for this context
//	((CFMLEngineImpl) ConfigWebUtil.getEngine(configWeb)).onStart(configWeb, false);

		return configWeb;
	}

	private static void createHtAccess(Resource htAccess) {
		if (!htAccess.exists()) {
			htAccess.createNewFile();

			String content = "AuthName \"WebInf Folder\"\n" + "AuthType Basic\n" + "<Limit GET POST>\n" + "order deny,allow\n" + "deny from all\n" + "</Limit>";
			try {
				IOUtil.copy(new ByteArrayInputStream(content.getBytes()), htAccess, true);
			} catch (Throwable t) {
				ExceptionUtil.rethrowIfNecessary(t);
			}
		}
	}

	/**
	 * reloads the Config Object
	 *
	 * @param cs
	 * @param force
	 * @throws SAXException
	 * @throws ClassNotFoundException
	 * @throws PageException
	 * @throws IOException
	 * @throws TagLibException
	 * @throws FunctionLibException
	 * @throws BundleException
	 * @throws NoSuchAlgorithmException
	 */
	public static void reloadInstance(CFMLEngine engine, ConfigServerImpl cs, ConfigWebImpl cw, boolean force)
			throws SAXException, ClassException, PageException, IOException, TagLibException, FunctionLibException, BundleException {
		Resource configFile = cw.getConfigFile();
		Resource configDir = cw.getConfigDir();

		int iDoNew = doNew(engine, configDir, false).updateType;
		boolean doNew = iDoNew != NEW_NONE;

		if (configFile == null) return;

		if (second(cw.getLoadTime()) > second(configFile.lastModified()) && !force) return;

		Document doc = null;//loadDocument(configFile);
		createContextFiles(configDir, null, doNew);
		cw.reset();
		load(cs, cw, doc, true, doNew);
		cs.doc = doc;
		cw.doc = doc;
		load2(cs, cw, doc, true, doNew);
		load3(cs, cw, doc, true, doNew);
		load4(cs, cw, doc, true, doNew);
		load5(cs, cw, doc, true, doNew);
		load6(cs, cw, doc, true, doNew);
		load7(cs, cw, doc, true, doNew);
		load8(cs, cw, doc, true, doNew);
		load9(cs, cw, doc, true, doNew);
		load10(cs, cw, doc, true, doNew);
		loadPart2(cs, cw, true, doNew);
//	createContextFilesPost(configDir, cw, null, false, doNew);

//	((CFMLEngineImpl) ConfigWebUtil.getEngine(cw)).onStart(cw, true);
	}

	private static long second(long ms) {
		return ms / 1000;
	}

	/**
	 * @param cs
	 * @param config
	 * @param doc
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws FunctionLibException
	 * @throws TagLibException
	 * @throws PageException
	 * @throws BundleException
	 */
	static void load(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		double start = System.currentTimeMillis();
		// if(LOG) SystemOut.printDate("start reading config");
//	    CFMLServlet.logStartTime("XMLConfigWebFactory load begin");

		ThreadLocalConfig.register(config);
		boolean reload = false;

		// fix stuff from older config files
//		    if (XMLConfigAdmin.fixLFI(doc)) {
//			    String xml = XMLCaster.toString(doc);
//			    // TODO 4.5->5.0
//			    xml = StringUtil.replace(xml, "<lucee-configuration", "<cfLuceeConfiguration", false);
//			    xml = StringUtil.replace(xml, "</lucee-configuration", "</cfLuceeConfiguration", false);
//			    IOUtil.write(config.getConfigFile(), xml, CharsetUtil.UTF8, false);
//			    try {
//				    doc = XMLConfigWebFactory.loadDocument(config.getConfigFile());
//			    } catch (SAXException e) {
//			    }
//		    }
//		    // if(LOG) SystemOut.printDate("fixed LFI");
//
//		    if (XMLConfigAdmin.fixSalt(doc)) reload = true;
//		    // if(LOG) SystemOut.printDate("fixed salt");
//
//		    if (XMLConfigAdmin.fixS3(doc)) reload = true;
//		    // if(LOG) SystemOut.printDate("fixed S3");
//
//		    if (XMLConfigAdmin.fixPSQ(doc)) reload = true;
//		    // if(LOG) SystemOut.printDate("fixed PSQ");
//
//		    if (XMLConfigAdmin.fixLogging(cs, config, doc)) reload = true;
//		    // if(LOG) SystemOut.printDate("fixed logging");
//
//		    if (XMLConfigAdmin.fixExtension(config, doc)) reload = true;
//		    // if(LOG) SystemOut.printDate("fixed Extension");
//
//		    if (XMLConfigAdmin.fixComponentMappings(config, doc)) reload = true;
//		    // if(LOG) SystemOut.printDate("fixed component mappings");

		// delete to big felix.log (there is also code in the loader to do this, but if the loader is not
		// updated ...)
		if (config instanceof ConfigServerImpl) {
			try {
				ConfigServerImpl _cs = (ConfigServerImpl) config;
				File root = _cs.getCFMLEngine().getCFMLEngineFactory().getResourceRoot();
				File log = new File(root, "context/logs/felix.log");
				if (log.isFile() && log.length() > GB1) {
					SystemOut.printDate("delete felix log: " + log);
					// if(LOG.delete()) ResourceUtil.touch(log);

				}
			} catch (Exception e) {
				log(config, null, e);
			}
		}
		// if(LOG) SystemOut.printDate("fixed to big felix.log");

//		    if (reload) {
//			    XMLCaster.writeTo(doc, config.getConfigFile());
//			    try {
//				    doc = XMLConfigWebFactory.loadDocument(config.getConfigFile());
//			    } catch (SAXException e) {
//			    }
//			    // if(LOG) SystemOut.printDate("reload xml");
//
//		    }


		config.setLastModified();
		if (config instanceof ConfigWeb) ConfigWebUtil.deployWebContext(cs, (ConfigWeb) config, false);
//	    CFMLServlet.logStartTime("XMLConfigWebFactory load after deployWebContext");
	}

	static void load2(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		// if(LOG) SystemOut.printDate("deploy web context");
		loadConfig(cs, config, doc);
		int mode = config.getMode();
//		CFMLServlet.logStartTime("XMLConfigWebFactory load2 after getMode");
		Log log = null;//config.getLog("application");
//		CFMLServlet.logStartTime("XMLConfigWebFactory load2 after getLog");
		// if(LOG) SystemOut.printDate("loaded config");
		loadConstants(cs, config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load2 after loadConstants");
		// if(LOG) SystemOut.printDate("loaded constants");
//		log = config.getLog("application");
		// loadServerLibDesc(cs, config, doc,log);
		// if(LOG) SystemOut.printDate("loaded loggers");
		loadTempDirectory(cs, config, doc, isReload, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load2 after loadTempDirectory");
	}

	static void load3(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		Log log = null;//config.getLog("application");
		// if(LOG) SystemOut.printDate("loaded temp dir");
		loadId(cs, config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load3 after loadId");
		// if(LOG) SystemOut.printDate("loaded id");
		loadVersion(config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load3 after loadVersion");
		// if(LOG) SystemOut.printDate("loaded version");
		loadSecurity(cs, config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load3 after loadSecurity");
		// if(LOG) SystemOut.printDate("loaded security");
//	    CFMLServlet.logStartTime("XMLConfigWebFactory load after many load operations");
	}

	static void load4(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		Log log = null;//config.getLog("application");
//	    CFMLServlet.logStartTime("XMLConfigWebFactory load after loadLib");
		// if(LOG) SystemOut.printDate("loaded lib");
		loadSystem(cs, config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load2 after loadSystem");
		// if(LOG) SystemOut.printDate("loaded system");
		loadResourceProvider(cs, config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load2 after loadResourceProvider");
		// if(LOG) SystemOut.printDate("loaded resource providers");
		config.doc = doc;
//		loadExtensionBundles(cs, config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load2 after loadExtensionBundles");
		// if(LOG) SystemOut.printDate("loaded extension bundles");
//		loadWS(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded webservice");
//		loadORM(cs, config, doc, log);
	}

	static void load5(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		int mode = config.getMode();
		Log log = null;//config.getLog("application");
		// if(LOG) SystemOut.printDate("loaded orm");
//		loadCacheHandler(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded cache handlers");
		loadCharset(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded charset");
		loadMappings(cs, config, doc, mode, log); // it is important this runs after
		// if(LOG) SystemOut.printDate("loaded mappings");
		// loadApplication
//		loadRest(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded rest");
	}

	static void load6(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		int mode = config.getMode();
		Log log = null;//config.getLog("application");
		loadApplication(cs, config, doc, mode, log);
		// if(LOG) SystemOut.printDate("loaded application");
//		loadExtensions(cs, config, doc, log);
//	    CFMLServlet.logStartTime("XMLConfigWebFactory load after many load operations");
		// if(LOG) SystemOut.printDate("loaded extensions");
//		loadPagePool(cs, config, doc, log);
//		// if(LOG) SystemOut.printDate("loaded page pool");
//		loadDataSources(cs, config, doc, log);
//		// if(LOG) SystemOut.printDate("loaded datasources");
//		loadCache(cs, config, doc, log);
//		// if(LOG) SystemOut.printDate("loaded cache");
//		loadCustomTagsMappings(cs, config, doc, mode, log);
		// if(LOG) SystemOut.printDate("loaded custom tag mappings");
	}

	static void load7(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		int mode = config.getMode();
		Log log = null;//config.getLog("application");
		// loadFilesystem(cs, config, doc, doNew); // load tlds
		// if(LOG) SystemOut.printDate("loaded tags");
		loadRegional(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded regional");
		loadCompiler(cs, config, doc, mode, log);
		// if(LOG) SystemOut.printDate("loaded compiler");
		loadScope(cs, config, doc, mode, log);
		// if(LOG) SystemOut.printDate("loaded scope");
		loadMail(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded mail");
//		loadSearch(cs, config, doc, log);
//		// if(LOG) SystemOut.printDate("loaded search");
//		loadScheduler(cs, config, doc, log);
	}

	static void load8(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		int mode = config.getMode();
		Log log = null;//config.getLog("application");
		// if(LOG) SystemOut.printDate("loaded scheduled tasks");
		loadDebug(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded debug");
		loadError(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded error");
//		loadCFX(cs, config, doc, log);
//	    CFMLServlet.logStartTime("XMLConfigWebFactory load after many load operations");
		// if(LOG) SystemOut.printDate("loaded cfx");
		loadComponent(cs, config, doc, mode, log);
		// if(LOG) SystemOut.printDate("loaded component");
//		loadUpdate(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded update");
		loadJava(cs, config, doc, log); // define compile type
		// if(LOG) SystemOut.printDate("loaded java");
		loadSetting(cs, config, doc, log);

		loadDumpWriter(cs, config, doc, log);
	}

	static void load9(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		Log log = null;//config.getLog("application");
		// if(LOG) SystemOut.printDate("loaded setting");
//		loadProxy(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded proxy");
//		loadRemoteClient(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded remote clients");
//		loadVideo(cs, config, doc, log);
//		// if(LOG) SystemOut.printDate("loaded video");
//		loadFlex(cs, config, doc, log);
//		// if(LOG) SystemOut.printDate("loaded flex");
//		loadListener(cs, config, doc, log);
		loadLoggers(cs, config, doc, isReload, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load9 after loadLoggers");

		if (config instanceof ConfigWeb) ConfigWebUtil.deployWeb(cs, (ConfigWeb) config, false);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load2 after deployWeb");

//		try {
//			ConfigWebUtil.loadLib(cs, config);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//		CFMLServlet.logStartTime("XMLConfigWebFactory load9 after loadLib");
	}

	static void load10(ConfigServerImpl cs, ConfigImpl config, Document doc, boolean isReload, boolean doNew) throws IOException {
		Log log = null;//config.getLog("application");
		// if(LOG) SystemOut.printDate("loaded listeners");
//	    CFMLServlet.logStartTime("XMLConfigWebFactory load after many load operations");
		// if(LOG) SystemOut.printDate("loaded dump writers");
//		loadGatewayEL(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded gateways");
		loadExeLog(cs, config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load10 after loadExeLog");
		// if(LOG) SystemOut.printDate("loaded exe log");
		loadQueue(cs, config, doc, log);
//		CFMLServlet.logStartTime("XMLConfigWebFactory load10 after loadQueue");
		// if(LOG) SystemOut.printDate("loaded queue");
//		loadMonitors(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded monitors");
//		loadLogin(cs, config, doc, log);
		// if(LOG) SystemOut.printDate("loaded login");
//		config.setLoadTime(System.currentTimeMillis());
//	    CFMLServlet.logStartTime("XMLConfigWebFactory load after many load operations");

	}

	static void loadPart2(ConfigServerImpl cs, ConfigImpl config, boolean isReload, boolean doNew) throws IOException {
		int mode = config.getMode();
		Document doc = config.doc;
		Log log = config.getLog("application");

		loadFilesystem(cs, config, doc, doNew, log); // load this before execute any code, what for example loadxtension does (json)
		// if(LOG) SystemOut.printDate("loaded filesystem");

		settings(config, log);
		// if(LOG) SystemOut.printDate("loaded settings2");
//		loadTag(cs, config, doc, log); // load tlds
		if (config instanceof ConfigWebImpl) {
			TagUtil.addTagMetaData((ConfigWebImpl) config, log);
			// if(LOG) SystemOut.printDate("added tag meta data");
		}
	}

	private static void loadResourceProvider(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		try{
			boolean hasCS = configServer != null;
			config.clearResourceProviders();
			// Default Resource Provider
			if (hasCS) config.setDefaultResourceProvider(configServer.getDefaultResourceProvider());
			ClassDefinition defProv = getClassDefinition(StaticConfig.defaultResourceProviderClass, "", "", "", config.getIdentification());


			try {
				config.setDefaultResourceProvider(defProv.getClazz(), toArguments(StaticConfig.defaultResourceProviderArguments, true));
			} catch (ClassException | BundleException e) {
				throw new RuntimeException(e);
			}
			// Resource Provider
			if (hasCS) config.setResourceProviderFactories(configServer.getResourceProviderFactories());
			ClassDefinition prov;
			String strProviderScheme;
			ClassDefinition httpClass = null;
			Map httpArgs = null;
			boolean hasHTTPs = false;

			for(int i=0;i<StaticConfig.resourceProviders.length;i++){
				StaticConfig.StaticResourceProvider resourceProvider=StaticConfig.resourceProviders[i];
				try {
					prov = getClassDefinition(resourceProvider.clazz, "", "", "", config.getIdentification());

					strProviderScheme = resourceProvider.scheme.trim().toLowerCase();
					config.addResourceProvider(strProviderScheme, prov, toArguments(resourceProvider.arguments, true));

					// patch for user not having
					if (strProviderScheme.equalsIgnoreCase("http")) {
						httpClass = prov;
						httpArgs = toArguments(resourceProvider.arguments, true);
					} else if (strProviderScheme.equalsIgnoreCase("https")) hasHTTPs = true;

				} catch (Throwable t) { // TODO log the exception
					ExceptionUtil.rethrowIfNecessary(t);
				}
			}

			// adding https when not exist
			if (!hasHTTPs && httpClass != null) {
				config.addResourceProvider("https", httpClass, httpArgs);
			}
		} catch (Exception e) {
			log(config, log, e);
		}
	}

	private static ClassDefinition getClassDefinition(String clazz, String bundleName, String bundleVersion, String prefix, Identification id) {
		String cn = clazz;
		String bn = bundleName;
		String bv = bundleVersion;

		// proxy jar libary no longer provided, so if still this class name is used ....
		if ("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(cn)) {
			cn = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		}

		ClassDefinition cd = new ClassDefinitionImpl(cn, bn, bv, id);
		// if(!StringUtil.isEmpty(cd.className,true))cd.getClazz();
		return cd;
	}
//    private static ClassDefinition getClassDefinition(Element el, String prefix, Identification id) {
//	String cn = getAttr(el, prefix + "class");
//	String bn = getAttr(el, prefix + "bundle-name");
//	String bv = getAttr(el, prefix + "bundle-version");
//
//	// proxy jar libary no longer provided, so if still this class name is used ....
//	if ("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(cn)) {
//	    cn = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
//	}
//
//	ClassDefinition cd = new ClassDefinitionImpl(cn, bn, bv, id);
//	// if(!StringUtil.isEmpty(cd.className,true))cd.getClazz();
//	return cd;
//    }

//	private static void loadCacheHandler(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			boolean hasCS = configServer != null;
//			// !!!! config.clearResourceProviders();
//
//			// first of all we make sure we have a request and timespan cachehandler
//			if (!hasCS) {
//				config.addCacheHandler("request", new ClassDefinitionImpl(RequestCacheHandler.class));
//				config.addCacheHandler("timespan", new ClassDefinitionImpl(TimespanCacheHandler.class));
//			}
//
//			// add CacheHandlers from server context to web context
//			if (hasCS) {
//				Iterator<Entry<String, Class<CacheHandler>>> it = configServer.getCacheHandlers();
//				if (it != null) {
//					Entry<String, Class<CacheHandler>> entry;
//					while (it.hasNext()) {
//						entry = it.next();
//						config.addCacheHandler(entry.getKey(), entry.getValue());
//					}
//				}
//			}
//
//			Element root = getChildByName(doc.getDocumentElement(), "cache-handlers");
//			Element[] handlers = getChildren(root, "cache-handler");
//			if (!ArrayUtil.isEmpty(handlers)) {
//				ClassDefinition cd;
//				String strId;
//				for (int i = 0; i < handlers.length; i++) {
//					cd = getClassDefinition(handlers[i], "", config.getIdentification());
//					strId = getAttr(handlers[i], "id");
//
//					if (cd.hasClass() && !StringUtil.isEmpty(strId)) {
//						strId = strId.trim().toLowerCase();
//						try {
//							config.addCacheHandler(strId, cd);
//						} catch (Throwable t) {
//							ExceptionUtil.rethrowIfNecessary(t);
//							log.error("Cache-Handler", t);
//						}
//					}
//				}
//			}
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	private static void loadDumpWriter(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		boolean hasCS = configServer != null;
		Struct sct = new StructImpl();

		boolean hasPlain = false;
		boolean hasRich = false;
		if (hasCS) {
			DumpWriterEntry[] entries = configServer.getDumpWritersEntries();
			if (entries != null) for (int i = 0; i < entries.length; i++) {
				if (entries[i].getDefaultType() == HTMLDumpWriter.DEFAULT_PLAIN) hasPlain = true;
				if (entries[i].getDefaultType() == HTMLDumpWriter.DEFAULT_RICH) hasRich = true;
				sct.put(entries[i].getName(), entries[i]);
			}
		}
		DumpWriterEntry dw=new DumpWriterEntry(HTMLDumpWriter.DEFAULT_RICH, "html", new HTMLDumpWriter());
//		CFMLServlet.logStartTime("XMLConfigWebFactory after ldw1");
		if(!hasRich) sct.setEL(KeyConstants._html, dw);
//		CFMLServlet.logStartTime("XMLConfigWebFactory after ldw22");
		if(!hasPlain) sct.setEL(KeyConstants._text, new DumpWriterEntry(HTMLDumpWriter.DEFAULT_PLAIN, "text", new TextDumpWriter()));

//		CFMLServlet.logStartTime("XMLConfigWebFactory after ldw2");
		sct.setEL(KeyConstants._classic, new DumpWriterEntry(HTMLDumpWriter.DEFAULT_NONE, "classic", new ClassicHTMLDumpWriter()));
//		CFMLServlet.logStartTime("XMLConfigWebFactory after ldw3");
		sct.setEL(KeyConstants._simple, new DumpWriterEntry(HTMLDumpWriter.DEFAULT_NONE, "simple", new SimpleHTMLDumpWriter()));
//		CFMLServlet.logStartTime("XMLConfigWebFactory after ldw4");

		Iterator<Object> it = sct.valueIterator();
		java.util.List<DumpWriterEntry> entries = new ArrayList<DumpWriterEntry>();
		while (it.hasNext()) {
			entries.add((DumpWriterEntry) it.next());
		}
//		CFMLServlet.logStartTime("XMLConfigWebFactory after ldw5");
		config.setDumpWritersEntries(entries.toArray(new DumpWriterEntry[entries.size()]));
	}

	static Map<String, String> toArguments(String attributes, boolean decode) {
		return cssStringToMap(attributes, decode, false);

	}

	public static Map<String, String> cssStringToMap(String attributes, boolean decode, boolean lowerKeys) {
		Map<String, String> map = new HashMap<String, String>();
		if (StringUtil.isEmpty(attributes, true)) return map;
		String[] arr = ListUtil.toStringArray(ListUtil.listToArray(attributes, ';'), null);

		int index;
		String str;
		for (int i = 0; i < arr.length; i++) {
			str = arr[i].trim();
			if (StringUtil.isEmpty(str)) continue;
			index = str.indexOf(':');
			if (index == -1) map.put(lowerKeys ? str.toLowerCase() : str, "");
			else {
				String k = dec(str.substring(0, index).trim(), decode);
				if (lowerKeys) k = k.toLowerCase();
				map.put(k, dec(str.substring(index + 1).trim(), decode));
			}
		}
		return map;
	}

	private static String dec(String str, boolean decode) {
		if (!decode) return str;
		return URLDecoder.decode(str, false);
	}

//	private static void loadListener(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			if (config instanceof ConfigServer) {
//				ConfigServer cs = (ConfigServer) config;
//				Element listener = getChildByName(doc.getDocumentElement(), "listener");
//				ClassDefinition cd = getClassDefinition(listener, "", config.getIdentification());
//				String strArguments = getAttr(listener, "arguments");
//				if (strArguments == null) strArguments = "";
//
//				if (cd.hasClass()) {
//					try {
//
//						Object obj = ClassUtil.loadInstance(cd.getClazz(), new Object[]{strArguments}, null);
//						if (obj instanceof ConfigListener) {
//							ConfigListener cl = (ConfigListener) obj;
//							cs.setConfigListener(cl);
//						}
//					} catch (Throwable t) {
//						ExceptionUtil.rethrowIfNecessary(t);
//						t.printStackTrace(config.getErrWriter());
//
//					}
//
//				}
//			} else if (configServer != null) {
//				ConfigListener listener = configServer.getConfigListener();
//				if (listener != null) listener.onLoadWebContext(configServer, (ConfigWeb) config);
//			}
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	private static void settings(ConfigImpl config, Log log) {
//		doCheckChangesInLibraries(config);
	}

	private static void loadVersion(ConfigImpl config, Document doc, Log log) {
		config.setVersion(4.2);
	}

	private static void loadId(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		try {
			// Security key
			Resource res = config.getConfigDir().getRealResource("id");
			String securityKey = null;
			try {
				if (!res.exists()) {
					res.createNewFile();
					IOUtil.write(res, securityKey = UUID.randomUUID().toString(), SystemUtil.getCharset(), false);
				} else {
					securityKey = IOUtil.toString(res, SystemUtil.getCharset());
				}
			} catch (Exception ioe) {
				log(config, log, ioe);
			}

			if (StringUtil.isEmpty(securityKey)) securityKey = UUID.randomUUID().toString();

			// API Key
			String apiKey = null;
			String str = "";//getAttr(doc.getDocumentElement(), "api-key");
			if (!StringUtil.isEmpty(str, true)) apiKey = str.trim();
			else if (configServer != null)
				apiKey = configServer.getIdentification().getApiKey(); // if there is no web api key the server api key is used

			if (config instanceof ConfigWebImpl)
				((ConfigWebImpl) config).setIdentification(new IdentificationWebImpl((ConfigWebImpl) config, securityKey, apiKey));
			else
				((ConfigServerImpl) config).setIdentification(new IdentificationServerImpl((ConfigServerImpl) config, securityKey, apiKey));
		} catch (Exception e) {
			log(config, log, e);
		}
	}

	private static boolean equal(Resource[] srcs, Resource[] trgs) {
		if (srcs.length != trgs.length) return false;
		Resource src;
		outer:
		for (int i = 0; i < srcs.length; i++) {
			src = srcs[i];
			for (int y = 0; y < trgs.length; y++) {
				if (src.equals(trgs[y])) continue outer;
			}
			return false;
		}
		return true;
	}

	private static Resource[] getNewResources(Resource[] srcs, Resource[] trgs) {
		Resource trg;
		java.util.List<Resource> list = new ArrayList<Resource>();
		outer:
		for (int i = 0; i < trgs.length; i++) {
			trg = trgs[i];
			for (int y = 0; y < srcs.length; y++) {
				if (trg.equals(srcs[y])) continue outer;
			}
			list.add(trg);
		}
		return list.toArray(new Resource[list.size()]);
	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 */
	private static void loadSecurity(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//			// Serial Number
//			if (config instanceof ConfigServer) {
//				Element luceeConfiguration = doc.getDocumentElement();
//				String serial = getAttr(luceeConfiguration, "serial-number");
//				if (!StringUtil.isEmpty(serial)) config.setSerialNumber(serial);
//			} else if (configServer != null) {
//				config.setSerialNumber(configServer.getSerialNumber());
//			}

		// Security Manger
		SecurityManager securityManager = null;
		if (config instanceof ConfigServerImpl) {
			ConfigServerImpl cs = (ConfigServerImpl) config;

			// Default SecurityManager
			SecurityManagerImpl sm = _toSecurityManager(null);

			// additional file access directories
			Resource[] resources=new Resource[StaticConfig.securityAllowPaths.length];
			for(int i=0;i<StaticConfig.securityAllowPaths.length;i++) {
				resources[i] = config.getResource(StaticConfig.securityAllowPaths[i]);
			}
			sm.setCustomFileAccess(resources);

			cs.setDefaultSecurityManager(sm);

		} else if (configServer != null) {
			securityManager = configServer.getSecurityManager(config.getIdentification().getId());
		}
		if (config instanceof ConfigWebImpl) {
			if (securityManager == null) securityManager = SecurityManagerImpl.getOpenSecurityManager();
			((ConfigWebImpl) config).setSecurityManager(securityManager);
		}

	}

//	private static Resource[] _loadFileAccess(Config config, Element[] fileAccesses) {
//		if (ArrayUtil.isEmpty(fileAccesses)) return new Resource[0];
//
//		java.util.List<Resource> reses = new ArrayList<Resource>();
//		String path;
//		Resource res;
//		for (int i = 0; i < fileAccesses.length; i++) {
//			path = getAttr(fileAccesses[i], "path");
//			if (!StringUtil.isEmpty(path)) {
//				res = config.getResource(path);
//				if (res.isDirectory()) reses.add(res);
//			}
//		}
//		return reses.toArray(new Resource[reses.size()]);
//	}

	private static SecurityManagerImpl _toSecurityManager(Element el) {
		SecurityManagerImpl sm = new SecurityManagerImpl(VALUE_NO, VALUE_NO, StaticConfig.securityDirectJavaAccess, VALUE_NO, VALUE_NO, VALUE_NO, VALUE_NO, VALUE_NO, VALUE_NO, VALUE_NO, VALUE_NO, VALUE_NO, VALUE_NO, StaticConfig.securityExecute, StaticConfig.securityTagImport, StaticConfig.securityJavaObject, StaticConfig.securityRegistry, VALUE_NO, VALUE_NO, VALUE_NO, SecurityManager.ACCESS_CLOSE, SecurityManager.ACCESS_CLOSE);
		return sm;
	}
//	private static SecurityManagerImpl _toSecurityManager(Element el) {
//		SecurityManagerImpl sm = new SecurityManagerImpl(_attr(el, "setting", SecurityManager.VALUE_YES), _attr(el, "file", SecurityManager.VALUE_ALL),
//				_attr(el, "direct_java_access", SecurityManager.VALUE_YES), _attr(el, "mail", SecurityManager.VALUE_YES), _attr(el, "datasource", SecurityManager.VALUE_YES),
//				_attr(el, "mapping", SecurityManager.VALUE_YES), _attr(el, "remote", SecurityManager.VALUE_YES), _attr(el, "custom_tag", SecurityManager.VALUE_YES),
//				_attr(el, "cfx_setting", SecurityManager.VALUE_YES), _attr(el, "cfx_usage", SecurityManager.VALUE_YES), _attr(el, "debugging", SecurityManager.VALUE_YES),
//				_attr(el, "search", SecurityManager.VALUE_YES), _attr(el, "scheduled_task", SecurityManager.VALUE_YES), _attr(el, "tag_execute", SecurityManager.VALUE_YES),
//				_attr(el, "tag_import", SecurityManager.VALUE_YES), _attr(el, "tag_object", SecurityManager.VALUE_YES), _attr(el, "tag_registry", SecurityManager.VALUE_YES),
//				_attr(el, "cache", SecurityManager.VALUE_YES), _attr(el, "gateway", SecurityManager.VALUE_YES), _attr(el, "orm", SecurityManager.VALUE_YES),
//				_attr2(el, "access_read", SecurityManager.ACCESS_PROTECTED), _attr2(el, "access_write", SecurityManager.ACCESS_PROTECTED));
//		return sm;
//	}

//	private static short _attr(Element el, String attr, short _default) {
//		return SecurityManagerImpl.toShortAccessValue(getAttr(el, attr), _default);
//	}
//
//	private static short _attr2(Element el, String attr, short _default) {
//		String strAccess = getAttr(el, attr);
//		if (StringUtil.isEmpty(strAccess)) return _default;
//		strAccess = strAccess.trim().toLowerCase();
//		if ("open".equals(strAccess)) return SecurityManager.ACCESS_OPEN;
//		if ("protected".equals(strAccess)) return SecurityManager.ACCESS_PROTECTED;
//		if ("close".equals(strAccess)) return SecurityManager.ACCESS_CLOSE;
//		return _default;
//	}

	static String createMD5FromResource(String resource) throws IOException {
		InputStream is = null;
		try {
			is = InfoImpl.class.getResourceAsStream(resource);
			byte[] barr = IOUtil.toBytes(is);
			return MD5.getDigestAsString(barr);
		} finally {
			IOUtil.closeEL(is);
		}
	}

	static String createContentFromResource(Resource resource) throws IOException {
		return IOUtil.toString(resource, (Charset) null);
	}

	static void createFileFromResourceCheckSizeDiffEL(String resource, Resource file) {
		try {
			createFileFromResourceCheckSizeDiff(resource, file);
		} catch (Exception e) {
			aprint.err(resource);
			aprint.err(file);
			SystemOut.printDate(e);
		}
	}

	/**
	 * creates a File and his content froma a resurce
	 *
	 * @param resource
	 * @param file
	 * @throws IOException
	 */
	static void createFileFromResourceCheckSizeDiff(String resource, Resource file) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtil.copy(InfoImpl.class.getResourceAsStream(resource), baos, true, false);
		byte[] barr = baos.toByteArray();

		if (file.exists()) {
			long trgSize = file.length();
			long srcSize = barr.length;
			if (srcSize == trgSize) return;

			SystemOut.printDate(SystemUtil.getPrintWriter(SystemUtil.OUT), "update file:" + file);
			SystemOut.printDate(SystemUtil.getPrintWriter(SystemUtil.OUT), " - source:" + srcSize);
			SystemOut.printDate(SystemUtil.getPrintWriter(SystemUtil.OUT), " - target:" + trgSize);

		} else file.createNewFile();

		// SystemOut.printDate("write file:"+file);
		IOUtil.copy(new ByteArrayInputStream(barr), file, true);
	}

	/**
	 * Creates all files for Lucee Context
	 *
	 * @param configDir
	 * @throws IOException
	 * @throws IOException
	 */
	private static void createContextFiles(Resource configDir, ServletConfigDead ServletConfigDead, boolean doNew) throws IOException {
		Resource contextDir = configDir.getRealResource("context");
		Resource f = contextDir.getRealResource("Component." + COMPONENT_EXTENSION);
		if (f.exists()) {
			return;
		}
		if (!contextDir.exists()) contextDir.mkdirs();

		if (!f.exists()) createFileFromResourceEL("/resource/context/Component." + COMPONENT_EXTENSION, f);


		Resource templatesDir = contextDir.getRealResource("templates");
		if (!templatesDir.exists()) templatesDir.mkdirs();

		Resource errorDir = templatesDir.getRealResource("error");
		if (!errorDir.exists()) errorDir.mkdirs();

		f = errorDir.getRealResource("error." + TEMPLATE_EXTENSION);
		if (!f.exists() || doNew)
			createFileFromResourceEL("/resource/context/templates/error/error." + TEMPLATE_EXTENSION, f);

//	    f = errorDir.getRealResource("error-neo." + TEMPLATE_EXTENSION);
//	    if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/templates/error/error-neo." + TEMPLATE_EXTENSION, f);
//
//	    f = errorDir.getRealResource("error-public." + TEMPLATE_EXTENSION);
//	    if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/templates/error/error-public." + TEMPLATE_EXTENSION, f);

		// custom locale files
//	{
//	    Resource dir = configDir.getRealResource("locales");
//	    if (!dir.exists()) dir.mkdirs();
//	    Resource file = dir.getRealResource("pt-PT-date.df");
//	    if (!file.exists()) createFileFromResourceEL("/resource/locales/pt-PT-date.df", file);
//	}
//
//	// video
//	Resource videoDir = configDir.getRealResource("video");
//	if (!videoDir.exists()) videoDir.mkdirs();
//
//	Resource video = videoDir.getRealResource("video.xml");
//	if (!video.exists()) createFileFromResourceEL("/resource/video/video.xml", video);
//
//	// bin
//	Resource binDir = configDir.getRealResource("bin");
//	if (!binDir.exists()) binDir.mkdirs();
//
//	Resource ctDir = configDir.getRealResource("customtags");
//	if (!ctDir.exists()) ctDir.mkdirs();
//
//	// Jacob
//	if (SystemUtil.isWindows()) {
//	    String name = (SystemUtil.getJREArch() == SystemUtil.ARCH_64) ? "jacob-x64.dll" : "jacob-i586.dll";
//	    Resource jacob = binDir.getRealResource(name);
//	    if (!jacob.exists()) {
//		createFileFromResourceEL("/resource/bin/windows" + ((SystemUtil.getJREArch() == SystemUtil.ARCH_64) ? "64" : "32") + "/" + name, jacob);
//	    }
//	}
//
//	Resource storDir = configDir.getRealResource("storage");
//	if (!storDir.exists()) storDir.mkdirs();
//
//	Resource compDir = configDir.getRealResource("components");
//	if (!compDir.exists()) compDir.mkdirs();
//
//	// remove old cacerts files, they are now only in the server context
//	Resource secDir = configDir.getRealResource("security");
//	if (secDir.exists()) {
//	    f = secDir.getRealResource("cacerts");
//	    if (f.exists()) f.delete();
//
//	}
//	else secDir.mkdirs();
//	f = secDir.getRealResource("antisamy-basic.xml");
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/security/antisamy-basic.xml", f);
//
//	// lucee-context
//	f = contextDir.getRealResource("lucee-context.lar");
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/lucee-context.lar", f);
//	else createFileFromResourceCheckSizeDiffEL("/resource/context/lucee-context.lar", f);
//
//	// lucee-admin
//	f = contextDir.getRealResource("lucee-admin.lar");
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/lucee-admin.lar", f);
//	else createFileFromResourceCheckSizeDiffEL("/resource/context/lucee-admin.lar", f);
//
//	// lucee-doc
//	f = contextDir.getRealResource("lucee-doc.lar");
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/lucee-doc.lar", f);
//	else createFileFromResourceCheckSizeDiffEL("/resource/context/lucee-doc.lar", f);
//
	f = contextDir.getRealResource("component-dump." + TEMPLATE_EXTENSION);
	if (!f.exists()) createFileFromResourceEL("/resource/context/component-dump." + TEMPLATE_EXTENSION, f);

		// Base Component
//	String badContent = "<cfcomponent displayname=\"Component\" hint=\"This is the Base Component\">\n</cfcomponent>";
//	String badVersion = "704b5bd8597be0743b0c99a644b65896";

//	else if (doNew && badVersion.equals(ConfigWebUtil.createMD5FromResource(f))) {
//	    createFileFromResourceEL("/resource/context/Component." + COMPONENT_EXTENSION, f);
//	}
//	else if (doNew && badContent.equals(createContentFromResource(f).trim())) {
//	    createFileFromResourceEL("/resource/context/Component." + COMPONENT_EXTENSION, f);
//	}

		// Component.lucee
//	f = contextDir.getRealResource("Component." + COMPONENT_EXTENSION_LUCEE);
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/Component." + COMPONENT_EXTENSION_LUCEE, f);

//	f = contextDir.getRealResource(Constants.CFML_APPLICATION_EVENT_HANDLER);
//	if (!f.exists()) createFileFromResourceEL("/resource/context/Application." + COMPONENT_EXTENSION, f);
//
//	f = contextDir.getRealResource("form." + TEMPLATE_EXTENSION);
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/form." + TEMPLATE_EXTENSION, f);
//
//	f = contextDir.getRealResource("graph." + TEMPLATE_EXTENSION);
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/graph." + TEMPLATE_EXTENSION, f);
//
//	f = contextDir.getRealResource("wddx." + TEMPLATE_EXTENSION);
//	if (!f.exists()) createFileFromResourceEL("/resource/context/wddx." + TEMPLATE_EXTENSION, f);
//
//	f = contextDir.getRealResource("lucee-applet." + TEMPLATE_EXTENSION);
//	if (!f.exists()) createFileFromResourceEL("/resource/context/lucee-applet." + TEMPLATE_EXTENSION, f);
//
//	f = contextDir.getRealResource("lucee-applet.jar");
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/context/lucee-applet.jar", f);
//
//	// f=new BinaryFile(contextDir,"lucee_context.ra");
//	// if(!f.exists())createFileFromResource("/resource/context/lucee_context.ra",f);
//
//	f = contextDir.getRealResource("admin." + TEMPLATE_EXTENSION);
//	if (!f.exists()) createFileFromResourceEL("/resource/context/admin." + TEMPLATE_EXTENSION, f);
//
//	// Video
//	f = contextDir.getRealResource("swfobject.js");
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/video/swfobject.js", f);
//	f = contextDir.getRealResource("swfobject.js." + TEMPLATE_EXTENSION);
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/video/swfobject.js." + TEMPLATE_EXTENSION, f);
//
//	f = contextDir.getRealResource("mediaplayer.swf");
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/video/mediaplayer.swf", f);
//	f = contextDir.getRealResource("mediaplayer.swf." + TEMPLATE_EXTENSION);
//	if (!f.exists() || doNew) createFileFromResourceEL("/resource/video/mediaplayer.swf." + TEMPLATE_EXTENSION, f);
//
//	Resource adminDir = contextDir.getRealResource("admin");
//	if (!adminDir.exists()) adminDir.mkdirs();
//
//	// Plugin
//	Resource pluginDir = adminDir.getRealResource("plugin");
//	if (!pluginDir.exists()) pluginDir.mkdirs();
//
//	f = pluginDir.getRealResource("Plugin." + COMPONENT_EXTENSION);
//	if (!f.exists()) createFileFromResourceEL("/resource/context/admin/plugin/Plugin." + COMPONENT_EXTENSION, f);
//
//	// Plugin Note
//	Resource note = pluginDir.getRealResource("Note");
//	if (!note.exists()) note.mkdirs();
//
//	f = note.getRealResource("language.xml");
//	if (!f.exists()) createFileFromResourceEL("/resource/context/admin/plugin/Note/language.xml", f);
//
//	f = note.getRealResource("overview." + TEMPLATE_EXTENSION);
//	if (!f.exists()) createFileFromResourceEL("/resource/context/admin/plugin/Note/overview." + TEMPLATE_EXTENSION, f);
//
//	f = note.getRealResource("Action." + COMPONENT_EXTENSION);
//	if (!f.exists()) createFileFromResourceEL("/resource/context/admin/plugin/Note/Action." + COMPONENT_EXTENSION, f);
//
//	// gateway
//	Resource componentsDir = configDir.getRealResource("components");
//	if (!componentsDir.exists()) componentsDir.mkdirs();
//
//	Resource gwDir = componentsDir.getRealResource("lucee/extension/gateway/");
//	create("/resource/context/gateway/", new String[] { "TaskGateway." + COMPONENT_EXTENSION, "DummyGateway." + COMPONENT_EXTENSION, "DirectoryWatcher." + COMPONENT_EXTENSION,
//		"DirectoryWatcherListener." + COMPONENT_EXTENSION, "MailWatcher." + COMPONENT_EXTENSION, "MailWatcherListener." + COMPONENT_EXTENSION }, gwDir, doNew);
//
//	// resources/language
//	Resource langDir = adminDir.getRealResource("resources/language");
//	create("/resource/context/admin/resources/language/", new String[] { "en.xml", "de.xml" }, langDir, doNew);
//
//	// add Debug
//	Resource debug = adminDir.getRealResource("debug");
//	create("/resource/context/admin/debug/", new String[] { "Debug." + COMPONENT_EXTENSION, "Field." + COMPONENT_EXTENSION, "Group." + COMPONENT_EXTENSION }, debug, doNew);
//
//	// add Cache Drivers
//	Resource cDir = adminDir.getRealResource("cdriver");
//	create("/resource/context/admin/cdriver/", new String[] { "Cache." + COMPONENT_EXTENSION, "Field." + COMPONENT_EXTENSION, "Group." + COMPONENT_EXTENSION }, cDir, doNew);
//
//	// add DB Drivers types
//	Resource dbDir = adminDir.getRealResource("dbdriver");
//	Resource typesDir = dbDir.getRealResource("types");
//	create("/resource/context/admin/dbdriver/types/", new String[] { "IDriver." + COMPONENT_EXTENSION, "Driver." + COMPONENT_EXTENSION, "IDatasource." + COMPONENT_EXTENSION,
//		"IDriverSelector." + COMPONENT_EXTENSION, "Field." + COMPONENT_EXTENSION }, typesDir, doNew);
//
//	// add Gateway Drivers
//	Resource gDir = adminDir.getRealResource("gdriver");
//	create("/resource/context/admin/gdriver/", new String[] { "Gateway." + COMPONENT_EXTENSION, "Field." + COMPONENT_EXTENSION, "Group." + COMPONENT_EXTENSION }, gDir, doNew);
//
//	// add Logging/appender
//	Resource app = adminDir.getRealResource("logging/appender");
//	create("/resource/context/admin/logging/appender/", new String[] { "Appender." + COMPONENT_EXTENSION, "Field." + COMPONENT_EXTENSION, "Group." + COMPONENT_EXTENSION }, app,
//		doNew);
//
//	// Logging/layout
//	Resource lay = adminDir.getRealResource("logging/layout");
//	create("/resource/context/admin/logging/layout/", new String[] { "Layout." + COMPONENT_EXTENSION, "Field." + COMPONENT_EXTENSION, "Group." + COMPONENT_EXTENSION }, lay,
//		doNew);

//	Resource displayDir = templatesDir.getRealResource("display");
//	if (!displayDir.exists()) displayDir.mkdirs();

	}

	private static void createContextFilesPost(Resource configDir, ConfigImpl config, ServletConfigDead ServletConfigDead, boolean isEventGatewayContext, boolean doNew) {
//	Resource contextDir = configDir.getRealResource("context");
//	if (!contextDir.exists()) contextDir.mkdirs();
//
//	Resource adminDir = contextDir.getRealResource("admin");
//	if (!adminDir.exists()) adminDir.mkdirs();
//
//	// Plugin
//	Resource pluginDir = adminDir.getRealResource("plugin");
//	if (!pluginDir.exists()) pluginDir.mkdirs();
//
//	// deploy org.lucee.cfml components
//	if (config instanceof ConfigWeb) {
//	    ImportDefintion _import = config.getComponentDefaultImport();
//	    String path = _import.getPackageAsPath();
//	    Resource components = config.getConfigDir().getRealResource("components");
//	    Resource dir = components.getRealResource(path);
//	    dir.mkdirs();
//	    // print.o(dir);
//	    ComponentFactory.deploy(dir, doNew);
//	}
	}

	private static void doCheckChangesInLibraries(ConfigImpl config) {
		// create current hash from libs
		TagLib[] ctlds = config.getTLDs(CFMLEngine.DIALECT_CFML);
//	TagLib[] ltlds = config.getTLDs(CFMLEngine.DIALECT_LUCEE);
		FunctionLib[] cflds = config.getFLDs(CFMLEngine.DIALECT_CFML);
//	FunctionLib[] lflds = config.getFLDs(CFMLEngine.DIALECT_LUCEE);

		StringBuilder sb = new StringBuilder();

		// version
		if (config instanceof ConfigWebImpl) {
			Info info = ((ConfigWebImpl) config).getFactory().getEngine().getInfo();
			sb.append(info.getVersion().toString()).append(';');
		}

		// charset
		sb.append(config.getTemplateCharset().name()).append(';');

		// dot notation upper case
		_getDotNotationUpperCase(sb, config.getMappings());
		_getDotNotationUpperCase(sb, config.getCustomTagMappings());
		_getDotNotationUpperCase(sb, config.getComponentMappings());
		_getDotNotationUpperCase(sb, config.getFunctionMappings());
		_getDotNotationUpperCase(sb, config.getTagMappings());
		// _getDotNotationUpperCase(sb,config.getServerTagMapping());
		// _getDotNotationUpperCase(sb,config.getServerFunctionMapping());

		// suppress ws before arg
		sb.append(config.getSuppressWSBeforeArg());
		sb.append(';');

		// externalize strings
		sb.append(config.getExternalizeStringGTE());
		sb.append(';');

		// function output
		sb.append(config.getDefaultFunctionOutput());
		sb.append(';');

		// full null support
		// sb.append(config.getFull Null Support()); // no longer a compiler switch
		// sb.append(';');

		// fusiondebug or not (FD uses full path name)
		sb.append(config.allowRequestTimeout());
		sb.append(';');

		// tld
		for (int i = 0; i < ctlds.length; i++) {
			sb.append(ctlds[i].getHash());
		}
//	for (int i = 0; i < ltlds.length; i++) {
//	    sb.append(ltlds[i].getHash());
//	}
		// fld
		for (int i = 0; i < cflds.length; i++) {
			sb.append(cflds[i].getHash());
		}
//	for (int i = 0; i < lflds.length; i++) {
//	    sb.append(lflds[i].getHash());
//	}

		if (config instanceof ConfigWeb) {
			boolean hasChanged = false;

			sb.append(";").append(((ConfigWebImpl) config).getConfigServerImpl().getLibHash());
			try {
				String hashValue = HashUtil.create64BitHashAsString(sb.toString());
				// check and compare lib version file
				Resource libHash = config.getConfigDir().getRealResource("lib-hash");

				if (!libHash.exists()) {
					libHash.createNewFile();
					IOUtil.write(libHash, hashValue, SystemUtil.getCharset(), false);
					hasChanged = true;
				} else if (!IOUtil.toString(libHash, SystemUtil.getCharset()).equals(hashValue)) {
					IOUtil.write(libHash, hashValue, SystemUtil.getCharset(), false);
					hasChanged = true;
				}
			} catch (IOException e) {
			}

			// change Compile type
			if (hasChanged) {
				try {
					// first we delete the physical classes
					config.getClassDirectory().remove(true);

					// now we force the pagepools to flush
					flushPageSourcePool(config.getMappings());
					flushPageSourcePool(config.getCustomTagMappings());
					flushPageSourcePool(config.getComponentMappings());
					flushPageSourcePool(config.getFunctionMappings());
					flushPageSourcePool(config.getTagMappings());

					if (config instanceof ConfigWeb) {
						flushPageSourcePool(((ConfigWebImpl) config).getApplicationMapping());
					}
					/*
					 * else { ConfigWeb[] webs = ((ConfigServerImpl)config).getConfigWebs(); for(int
					 * i=0;i<webs.length;i++){ flushPageSourcePool(((ConfigWebImpl)webs[i]).getApplicationMapping()); }
					 * }
					 */

				} catch (IOException e) {
					e.printStackTrace(config.getErrWriter());
				}
			}
		} else {
			((ConfigServerImpl) config).setLibHash(HashUtil.create64BitHashAsString(sb.toString()));
		}

	}

	private static void flushPageSourcePool(Mapping... mappings) {
		for (int i = 0; i < mappings.length; i++) {
			if (mappings[i] instanceof MappingImpl)
				((MappingImpl) mappings[i]).flush(); // FUTURE make "flush" part of the interface
		}
	}

	private static void flushPageSourcePool(Collection<Mapping> mappings) {
		Iterator<Mapping> it = mappings.iterator();
		Mapping m;
		while (it.hasNext()) {
			m = it.next();
			if (m instanceof MappingImpl) ((MappingImpl) m).flush(); // FUTURE make "flush" part of the interface
		}
	}

	private static void _getDotNotationUpperCase(StringBuilder sb, Mapping... mappings) {
		for (int i = 0; i < mappings.length; i++) {
			sb.append(((MappingImpl) mappings[i]).getDotNotationUpperCase()).append(';');
		}
	}

	private static void _getDotNotationUpperCase(StringBuilder sb, Collection<Mapping> mappings) {
		Iterator<Mapping> it = mappings.iterator();
		Mapping m;
		while (it.hasNext()) {
			m = it.next();
			sb.append(((MappingImpl) m).getDotNotationUpperCase()).append(';');
		}
	}

	/**
	 * load mappings from XML Document
	 *
	 * @param configServer
	 * @param config
	 * @param doc
	 * @throws IOException
	 */
	private static void loadMappings(ConfigServerImpl configServer, ConfigImpl config, Document doc, int mode, Log log) throws IOException {
		try {
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManager.TYPE_MAPPING);
//			Element el = getChildByName(doc.getDocumentElement(), "mappings");
//			Element[] _mappings = getChildren(el, "mapping");

			Map<String, Mapping> mappings = MapFactory.<String, Mapping>getConcurrentMap();
			Mapping tmp;

			boolean finished = false;

			if (configServer != null && config instanceof ConfigWeb) {
				Mapping[] sm = configServer.getMappings();
				if (sm != null) {
					for (int i = 0; i < sm.length; i++) {
						if (!sm[i].isHidden()) {
							if ("/".equals(sm[i].getVirtual())) finished = true;
							if (sm[i] instanceof MappingImpl) {
								tmp = ((MappingImpl) sm[i]).cloneReadOnly(config);
								mappings.put(tmp.getVirtualLowerCase(), tmp);

							} else {
								tmp = sm[i];
								mappings.put(tmp.getVirtualLowerCase(), tmp);
							}
						}
					}
				}
			}

			boolean hasServerContext = false;


			for (int i = 0; i < StaticConfig.mappingVirtualPaths.length; i++) {
				String physical = StaticConfig.mappingPhysicalPaths[i];
				String archive = "";
				String virtual = StaticConfig.mappingVirtualPaths[i];
				String listType = "root";
				String listMode = "modern";

				boolean readonly = false;
				boolean hidden = false;
				boolean toplevel = true;

				if (config instanceof ConfigServer && (virtual.equalsIgnoreCase("/lucee-server/") || virtual.equalsIgnoreCase("/lucee-server-context/"))) {
					hasServerContext = true;
				}

				// lucee
				if (virtual.equalsIgnoreCase("/lucee/")) {
					if (StringUtil.isEmpty(listType, true)) listType = "modern";
					if (StringUtil.isEmpty(listMode, true)) listMode = "curr2root";
					toplevel = true;
				}

				int listenerMode = ConfigWebUtil.toListenerMode(listMode, -1);
				int listenerType = ConfigWebUtil.toListenerType(listType, -1);
				ApplicationListener listener = ConfigWebUtil.loadListener(listenerType, null);
				if (listener != null || listenerMode != -1) {
					// type
					if (mode == ConfigImpl.MODE_STRICT) listener = new ModernAppListener();
					else if (listener == null)
						listener = ConfigWebUtil.loadListener(ConfigWebUtil.toListenerType(config.getApplicationListener().getType(), -1), null);
					if (listener == null)// this should never be true
						listener = new ModernAppListener();

					// mode
					if (listenerMode == -1) {
						listenerMode = config.getApplicationListener().getMode();
					}
					listener.setMode(listenerMode);

				}

				// physical!=null &&
				if ((physical != null || archive != null)) {

					short insTemp = ConfigImpl.INSPECT_UNDEFINED;
					if ("/lucee/".equalsIgnoreCase(virtual) || "/lucee".equalsIgnoreCase(virtual) || "/lucee-server/".equalsIgnoreCase(virtual)
							|| "/lucee-server-context".equalsIgnoreCase(virtual))
						insTemp = ConfigImpl.INSPECT_ONCE;

					String primary = "physical";
					boolean physicalFirst = primary == null || !primary.equalsIgnoreCase("archive");

					tmp = new MappingImpl(config, virtual, physical, archive, insTemp, physicalFirst, hidden, readonly, toplevel, false, false, listener, listenerMode,
							listenerType);
					mappings.put(tmp.getVirtualLowerCase(), tmp);
					if (virtual.equals("/")) {
						finished = true;
						// break;
					}
				}
			}

			// set default lucee-server-context
			if (config instanceof ConfigServer && !hasServerContext) {
				ApplicationListener listener = ConfigWebUtil.loadListener(ApplicationListener.TYPE_MODERN, null);
				listener.setMode(ApplicationListener.MODE_CURRENT2ROOT);

				tmp = new MappingImpl(config, "/lucee-server", "{lucee-server}/context/", null, ConfigImpl.INSPECT_ONCE, true, false, true, true, false, false, listener,
						ApplicationListener.MODE_CURRENT2ROOT, ApplicationListener.TYPE_MODERN);
				mappings.put(tmp.getVirtualLowerCase(), tmp);
			}
			if (!finished) {

				if ((config instanceof ConfigWebImpl) && ResourceUtil.isUNCPath(config.getRootDirectory().getPath())) {

					tmp = new MappingImpl(config, "/", config.getRootDirectory().getPath(), null, ConfigImpl.INSPECT_UNDEFINED, true, true, true, true, false, false, null, -1, -1);
				} else {

					tmp = new MappingImpl(config, "/", "/", null, ConfigImpl.INSPECT_UNDEFINED, true, true, true, true, false, false, null, -1, -1);
				}

				mappings.put("/", tmp);
			}

			Mapping[] arrMapping = new Mapping[mappings.size()];
			int index = 0;
			Iterator it = mappings.keySet().iterator();
			while (it.hasNext()) {
				arrMapping[index++] = mappings.get(it.next());
			}
			config.setMappings(arrMapping);
			// config.setMappings((Mapping[]) mappings.toArray(new
			// Mapping[mappings.size()]));
		} catch (Exception e) {
			log(config, log, e);
		}
	}

//	private static short inspectTemplate(Element el) {
//		String strInsTemp = getAttr(el, "inspect-template");
//		if (StringUtil.isEmpty(strInsTemp)) strInsTemp = getAttr(el, "inspect");
//		if (StringUtil.isEmpty(strInsTemp)) {
//			Boolean trusted = Caster.toBoolean(getAttr(el, "trusted"), null);
//			if (trusted != null) {
//				if (trusted.booleanValue()) return ConfigImpl.INSPECT_NEVER;
//				return ConfigImpl.INSPECT_ALWAYS;
//			}
//			return ConfigImpl.INSPECT_UNDEFINED;
//		}
//		return ConfigWebUtil.inspectTemplate(strInsTemp, ConfigImpl.INSPECT_UNDEFINED);
//	}

//	private static void loadRest(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			boolean hasAccess = true;// MUST
//			// ConfigWebUtil.hasAccess(config,SecurityManager.TYPE_REST);
//			boolean hasCS = configServer != null;
//			Element el = getChildByName(doc.getDocumentElement(), "rest");
//
//			// list
//			Boolean list = Caster.toBoolean(getAttr(el, "list"), null);
//			if (list != null) {
//				config.setRestList(list.booleanValue());
//			} else if (hasCS) {
//				config.setRestList(configServer.getRestList());
//			}
//
//			Element[] _mappings = getChildren(el, "mapping");
//
//			// first get mapping defined in server admin (read-only)
//			Map<String, lucee.runtime.rest.Mapping> mappings = new HashMap<String, lucee.runtime.rest.Mapping>();
//			lucee.runtime.rest.Mapping tmp;
//			if (configServer != null && config instanceof ConfigWeb) {
//				lucee.runtime.rest.Mapping[] sm = configServer.getRestMappings();
//				if (sm != null) {
//					for (int i = 0; i < sm.length; i++) {
//
//						if (!sm[i].isHidden()) {
//							tmp = sm[i].duplicate(config, Boolean.TRUE);
//							mappings.put(tmp.getVirtual(), tmp);
//						}
//					}
//				}
//			}
//
//			// get current mappings
//			if (hasAccess) {
//				for (int i = 0; i < _mappings.length; i++) {
//					el = _mappings[i];
//					String physical = el.getAttribute("physical");
//					String virtual = getAttr(el, "virtual");
//					boolean readonly = toBoolean(getAttr(el, "readonly"), false);
//					boolean hidden = toBoolean(getAttr(el, "hidden"), false);
//					boolean _default = toBoolean(getAttr(el, "default"), false);
//					if (physical != null) {
//						tmp = new lucee.runtime.rest.Mapping(config, virtual, physical, hidden, readonly, _default);
//						mappings.put(tmp.getVirtual(), tmp);
//					}
//				}
//			}
//
//			config.setRestMappings(mappings.values().toArray(new lucee.runtime.rest.Mapping[mappings.size()]));
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

//	private static void loadFlex(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			Element el = getChildByName(doc.getDocumentElement(), "flex");
//
//			// engine - we init a engine for every context, but only the server context defines the eggine class
//			if (config instanceof ConfigServerImpl) { // only server context
//
//				// arguments
//				Map<String, String> args = new HashMap<String, String>();
//				String _caster = getAttr(el, "caster");
//				if (_caster != null) args.put("caster", _caster);
//				String _config = getAttr(el, "configuration");
//				if (_config != null) args.put("configuration", _config);
//
//				ClassDefinition<AMFEngine> cd = getClassDefinition(el, "", config.getIdentification());
//				if (cd.hasClass()) ((ConfigServerImpl) config).setAMFEngine(cd, args);
//			} else if (configServer != null && configServer.getAMFEngineClassDefinition() != null && configServer.getAMFEngineClassDefinition().hasClass()) { // only web contexts
//				AMFEngine engine = toAMFEngine(config, configServer.getAMFEngineClassDefinition(), null);
//				if (engine != null) {
//					engine.init((ConfigWeb) config, configServer.getAMFEngineArgs());
//					((ConfigWebImpl) config).setAMFEngine(engine);
//				}
//				;
//			}
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}
//
//	private static AMFEngine toAMFEngine(Config config, ClassDefinition<AMFEngine> cd, AMFEngine defaultValue) {
//		Log log = config.getLog("application");
//		try {
//			Class<AMFEngine> clazz = cd.getClazz(null);
//			if (clazz != null) {
//				Object obj = clazz.newInstance();
//				if ((obj instanceof AMFEngine)) return (AMFEngine) obj;
//				log.error("Flex", "object [" + Caster.toClassName(obj) + "] must implement the interface " + AMFEngine.class.getName());
//			}
//		} catch (Exception e) {
//			log.error("Flex", e);
//		}
//		return defaultValue;
//	}

	private static void loadLoggers(ConfigServerImpl configServer, ConfigImpl config, Document doc, boolean isReload, Log log) {
		try {
			config.clearLoggers(Boolean.FALSE);
//			Element parent = getChildByName(doc.getDocumentElement(), "logging");
//			Element[] children = getChildren(parent, "logger");
			StaticConfig.StaticLog child;
			String appenderArgs, layoutArgs;
			ClassDefinition cdAppender, cdLayout;

			cdAppender = Log4jUtil.appenderClassDefintion("resource");
			cdLayout = Log4jUtil.appenderClassDefintion("layout");

			for (int i = 0; i < StaticConfig.loggers.length; i++) {
				child = StaticConfig.loggers[i];

				// appender
				appenderArgs = StringUtil.trim(child.arguments, "");

				layoutArgs = "";

				// ignore when no appender/name is defined
				if (cdAppender.hasClass() && !StringUtil.isEmpty(child.name)) {
					Map<String, String> appArgs = cssStringToMap(appenderArgs, true, true);
					if (cdLayout.hasClass()) {
						Map<String, String> layArgs = cssStringToMap(layoutArgs, true, true);
						config.addLogger(child.name, child.level, cdAppender, appArgs, cdLayout, layArgs, false, false);
					} else config.addLogger(child.name, child.level, cdAppender, appArgs, null, null, false, false);
				}
			}

			if (configServer != null) {
				Iterator<Entry<String, LoggerAndSourceData>> it = configServer.getLoggers().entrySet().iterator();
				Entry<String, LoggerAndSourceData> e;
				LoggerAndSourceData data;
				while (it.hasNext()) {
					e = it.next();

					// logger only exists in server context
					if (config.getLog(e.getKey(), false) == null) {
						data = e.getValue();
						config.addLogger(e.getKey(), data.getLevel(), data.getAppenderClassDefinition(), data.getAppenderArgs(), data.getLayoutClassDefinition(),
								data.getLayoutArgs(), true, false);
					}
				}
			}
		} catch (Exception e) {
			log(config, log, e);
		}
	}

	private static void loadExeLog(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		try {
			boolean hasServer = configServer != null;

//			Element el = getChildByName(doc.getDocumentElement(), "execution-log");

			// enabled
			Boolean bEnabled =false;// Caster.toBoolean(getAttr(el, "enabled"), null);
			if (bEnabled == null) {
				if (hasServer) config.setExecutionLogEnabled(configServer.getExecutionLogEnabled());
			} else config.setExecutionLogEnabled(bEnabled.booleanValue());

			boolean hasChanged = false;
			String val = Caster.toString(config.getExecutionLogEnabled());
			try {
				Resource contextDir = config.getConfigDir();
				Resource exeLog = contextDir.getRealResource("exe-log");

				if (!exeLog.exists()) {
					exeLog.createNewFile();
					IOUtil.write(exeLog, val, SystemUtil.getCharset(), false);
					hasChanged = true;
				} else if (!IOUtil.toString(exeLog, SystemUtil.getCharset()).equals(val)) {
					IOUtil.write(exeLog, val, SystemUtil.getCharset(), false);
					hasChanged = true;
				}
			} catch (IOException e) {
				e.printStackTrace(config.getErrWriter());
			}

			if (hasChanged) {
				try {
					if (config.getClassDirectory().exists()) config.getClassDirectory().remove(true);
				} catch (IOException e) {
					e.printStackTrace(config.getErrWriter());
				}
			}

			// class
//			String strClass = getAttr(el, "class");
//			Class clazz;
//			if (!StringUtil.isEmpty(strClass)) {
//				try {
//					if ("console".equalsIgnoreCase(strClass)) clazz = ConsoleExecutionLog.class;
//					else {
//						ClassDefinition cd = getClassDefinition(el, "", config.getIdentification());
//
//						Class c = cd.getClazz();
//						if ((c.newInstance() instanceof ExecutionLog)) {
//							clazz = c;
//						} else {
//							clazz = ConsoleExecutionLog.class;
//							SystemOut.printDate(config.getErrWriter(), "class [" + strClass + "] must implement the interface " + ExecutionLog.class.getName());
//						}
//					}
//				} catch (Exception e) {
//					SystemOut.printDate(e);
//					clazz = ConsoleExecutionLog.class;
//				}
//				if (clazz != null)
//					SystemOut.printDate(config.getOutWriter(), "loaded ExecutionLog class " + clazz.getName());
//
//				// arguments
//				String strArgs = getAttr(el, "arguments");
//				if (StringUtil.isEmpty(strArgs)) strArgs = getAttr(el, "class-arguments");
//				Map<String, String> args = toArguments(strArgs, true);
//
//				config.setExecutionLogFactory(new ExecutionLogFactory(clazz, args));
//			} else {
				if (hasServer) config.setExecutionLogFactory(configServer.getExecutionLogFactory());
				else
					config.setExecutionLogFactory(new ExecutionLogFactory(ConsoleExecutionLog.class, new HashMap<String, String>()));
//			}
		} catch (Exception e) {
			log(config, log, e);
		}
	}

//	/**
//	 * loads and sets the Page Pool
//	 *
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 */
//	private static void loadPagePool(ConfigServer configServer, Config config, Document doc, Log log) {
//		// TODO xml configuration fuer das erstellen
//		// config.setPagePool( new PagePool(10000,1000));
//	}

//	/**
//	 * loads datasource settings from XMl DOM
//	 *
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 * @throws BundleException
//	 * @throws ClassNotFoundException
//	 */
//	private static void loadDataSources(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			// load JDBC Driver definition
//			config.setJDBCDrivers(loadJDBCDrivers(configServer, config, doc, log));
//
//			// When set to true, makes JDBC use a representation for DATE data that
//			// is compatible with the Oracle8i database.
//			System.setProperty("oracle.jdbc.V8Compatible", "true");
//
//			boolean hasCS = configServer != null;
//			Map<String, DataSource> datasources = new HashMap<String, DataSource>();
//
//			// Copy Parent datasources as readOnly
//			if (hasCS) {
//				Map<String, DataSource> ds = configServer.getDataSourcesAsMap();
//				Iterator<Entry<String, DataSource>> it = ds.entrySet().iterator();
//				Entry<String, DataSource> entry;
//				while (it.hasNext()) {
//					entry = it.next();
//					if (!entry.getKey().equals(QOQ_DATASOURCE_NAME))
//						datasources.put(entry.getKey(), entry.getValue().cloneReadOnly());
//				}
//			}
//
//			// TODO support H2
//			// Default query of query DB
//			/*
//			 * setDatasource(datasources, QOQ_DATASOURCE_NAME, "org.h2.Driver" ,"" ,"" ,-1
//			 * ,"jdbc:h2:.;MODE=HSQLDB" ,"sa" ,"" ,-1 ,-1 ,true ,true ,DataSource.ALLOW_ALL, new StructImpl() );
//			 */
//			// Default query of query DB
//			try {
//				setDatasource(config, datasources, QOQ_DATASOURCE_NAME, new ClassDefinitionImpl("org.hsqldb.jdbcDriver", "hsqldb", "1.8.0", config.getIdentification()),
//						"hypersonic-hsqldb", "", -1, "jdbc:hsqldb:.", "sa", "", null, DEFAULT_MAX_CONNECTION, -1, 60000, true, true, DataSource.ALLOW_ALL, false, false, null,
//						new StructImpl(), "", ParamSyntax.DEFAULT, false, false);
//			} catch (Exception e) {
//				log.error("Datasource", e);
//			}
//
////	    SecurityManager sm = config.getSecurityManager();
////	    short access = sm.getAccess(SecurityManager.TYPE_DATASOURCE);
//			int accessCount = -1;
////	    if (access == SecurityManager.VALUE_YES) accessCount = -1;
////	    else if (access == SecurityManager.VALUE_NO) accessCount = 0;
////	    else if (access >= SecurityManager.VALUE_1 && access <= SecurityManager.VALUE_10) {
////		accessCount = access - SecurityManager.NUMBER_OFFSET;
////	    }
//
//			// Databases
//			Element databases = getChildByName(doc.getDocumentElement(), "data-sources");
//			// if(databases==null)databases=doc.createElement("data-sources");
//
//			// PSQ
//			String strPSQ = getAttr(databases, "psq");
//			if (StringUtil.isEmpty(strPSQ)) {
//				// prior version was buggy, was the opposite
//				strPSQ = getAttr(databases, "preserve-single-quote");
//				if (!StringUtil.isEmpty(strPSQ)) {
//					Boolean b = Caster.toBoolean(strPSQ, null);
//					if (b != null) strPSQ = b.booleanValue() ? "false" : "true";
//				}
//			}
//			if (!StringUtil.isEmpty(strPSQ)) {
//				config.setPSQL(toBoolean(strPSQ, true));
//			} else if (hasCS) config.setPSQL(configServer.getPSQL());
//
//			// Data Sources
//			Element[] dataSources = getChildren(databases, "data-source");
////	    if (accessCount == -1)
//			accessCount = dataSources.length;
////	    if (dataSources.length < accessCount) accessCount = dataSources.length;
//
//			// if(hasAccess) {
//			JDBCDriver jdbc;
//			ClassDefinition cd;
//			String id;
//			for (int i = 0; i < accessCount; i++) {
//				Element dataSource = dataSources[i];
//				if (dataSource.hasAttribute("database")) {
//					try {
//						// do we have an id?
//						jdbc = config.getJDBCDriverById(getAttr(dataSource, "id"), null);
//						if (jdbc != null && jdbc.cd != null) {
//							cd = jdbc.cd;
//						} else cd = getClassDefinition(dataSource, "", config.getIdentification());
//
//						// we only have a class
//						if (!cd.isBundle()) {
//							jdbc = config.getJDBCDriverByClassName(cd.getClassName(), null);
//							if (jdbc != null && jdbc.cd != null && jdbc.cd.isBundle()) cd = jdbc.cd;
//						}
//
//						// still no bundle!
//						if (!cd.isBundle()) cd = patchJDBCClass(config, cd);
//
//						setDatasource(config, datasources, getAttr(dataSource, "name"), cd, getAttr(dataSource, "host"), getAttr(dataSource, "database"),
//								Caster.toIntValue(getAttr(dataSource, "port"), -1), getAttr(dataSource, "dsn"), getAttr(dataSource, "username"),
//								ConfigWebUtil.decrypt(getAttr(dataSource, "password")), null, Caster.toIntValue(getAttr(dataSource, "connectionLimit"), DEFAULT_MAX_CONNECTION),
//								Caster.toIntValue(getAttr(dataSource, "connectionTimeout"), -1), Caster.toLongValue(getAttr(dataSource, "metaCacheTimeout"), 60000),
//								toBoolean(getAttr(dataSource, "blob"), true), toBoolean(getAttr(dataSource, "clob"), true),
//								Caster.toIntValue(getAttr(dataSource, "allow"), DataSource.ALLOW_ALL), toBoolean(getAttr(dataSource, "validate"), false),
//								toBoolean(getAttr(dataSource, "storage"), false), getAttr(dataSource, "timezone"), toStruct(getAttr(dataSource, "custom")),
//								getAttr(dataSource, "dbdriver"), ParamSyntax.toParamSyntax(dataSource, ParamSyntax.DEFAULT),
//								toBoolean(getAttr(dataSource, "literal-timestamp-with-tsoffset"), false), toBoolean(getAttr(dataSource, "always-set-timeout"), false)
//
//						);
//					} catch (Exception e) {
//						log.error("Datasource", e);
//					}
//				}
//			}
//			// }
//			config.setDataSources(datasources);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

//	private static ClassDefinition patchJDBCClass(ConfigImpl config, ClassDefinition cd) {
//		// PATCH for MySQL driver that did change the className within the same extension, JDBC extension
//		// expect that the className does not change.
//		if ("org.gjt.mm.mysql.Driver".equals(cd.getClassName()) || "com.mysql.jdbc.Driver".equals(cd.getClassName()) || "com.mysql.cj.jdbc.Driver".equals(cd.getClassName())) {
//			JDBCDriver jdbc = config.getJDBCDriverById("mysql", null);
//			if (jdbc != null && jdbc.cd != null && jdbc.cd.isBundle()) return jdbc.cd;
//
//			jdbc = config.getJDBCDriverByClassName("com.mysql.cj.jdbc.Driver", null);
//			if (jdbc != null && jdbc.cd != null && jdbc.cd.isBundle()) return jdbc.cd;
//
//			jdbc = config.getJDBCDriverByClassName("com.mysql.jdbc.Driver", null);
//			if (jdbc != null && jdbc.cd != null && jdbc.cd.isBundle()) return jdbc.cd;
//
//			jdbc = config.getJDBCDriverByClassName("org.gjt.mm.mysql.Driver", null);
//			if (jdbc != null && jdbc.cd != null && jdbc.cd.isBundle()) return jdbc.cd;
//
//			ClassDefinitionImpl tmp = new ClassDefinitionImpl("com.mysql.cj.jdbc.Driver", "com.mysql.cj", null, config.getIdentification());
//			if (tmp.getClazz(null) != null) return tmp;
//
//			tmp = new ClassDefinitionImpl("com.mysql.jdbc.Driver", "com.mysql.jdbc", null, config.getIdentification());
//			if (tmp.getClazz(null) != null) return tmp;
//		}
//		if ("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(cd.getClassName())) {
//			JDBCDriver jdbc = config.getJDBCDriverById("mssql", null);
//			if (jdbc != null && jdbc.cd != null && jdbc.cd.isBundle()) return jdbc.cd;
//
//			jdbc = config.getJDBCDriverByClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver", null);
//			if (jdbc != null && jdbc.cd != null && jdbc.cd.isBundle()) return jdbc.cd;
//
//			ClassDefinitionImpl tmp = new ClassDefinitionImpl("com.microsoft.sqlserver.jdbc.SQLServerDriver", cd.getName(), cd.getVersionAsString(), config.getIdentification());
//			if (tmp.getClazz(null) != null) return tmp;
//		}
//
//		return cd;
//	}
//
//	public static JDBCDriver[] loadJDBCDrivers(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		Map<String, JDBCDriver> map = new HashMap<String, JDBCDriver>();
//
//		// first add the server drivers, so they can be overwritten
//		if (configServer != null) {
//			JDBCDriver[] sds = configServer.getJDBCDrivers();
//			if (sds != null) {
//				for (JDBCDriver sd : sds) {
//					map.put(sd.cd.toString(), sd);
//				}
//			}
//		}
//
//		Element jdbc = getChildByName(doc.getDocumentElement(), "jdbc");
//		Element[] drivers = getChildren(jdbc, "driver");
//
//		ClassDefinition cd;
//		String label, id;
//		for (Element driver : drivers) {
//			cd = getClassDefinition(driver, "", config.getIdentification());
//			if (StringUtil.isEmpty(cd.getClassName()) && !StringUtil.isEmpty(cd.getName())) {
//				try {
//					Bundle bundle = OSGiUtil.loadBundle(cd.getName(), cd.getVersion(), config.getIdentification(), false);
//					String cn = JDBCDriver.extractClassName(bundle);
//					cd = new ClassDefinitionImpl(config.getIdentification(), cn, cd.getName(), cd.getVersion());
//				} catch (Exception e) {
//				}
//			}
//
//			label = getAttr(driver, "label");
//			id = getAttr(driver, "id");
//			// check if label exists
//			if (StringUtil.isEmpty(label)) {
//				if (log != null) log.error("Datasource", "missing label for jdbc driver [" + cd.getClassName() + "]");
//				continue;
//			}
//			// check if it is a bundle
//			if (!cd.isBundle()) {
//				if (log != null) log.error("Datasource", "jdbc driver [" + label + "] does not describe a bundle");
//				continue;
//			}
//			map.put(cd.toString(), new JDBCDriver(label, id, cd));
//		}
//		return map.values().toArray(new JDBCDriver[map.size()]);
//	}

	/*
	 * private static ClassDefinition matchJDBCBundle(Config config, ClassDefinition cd) {
	 * if(!cd.isBundle()) { if("org.hsqldb.jdbcDriver".equals(cd.getClassName())) return new
	 * ClassDefinitionImpl<>(cd.getClassName(), "hypersonic.hsqldb", null, config.getIdentification());
	 * if("org.gjt.mm.mysql.Driver".equals(cd.getClassName())) return new
	 * ClassDefinitionImpl<>(cd.getClassName(), "com.mysql.jdbc", null, config.getIdentification());
	 * if("org.h2.Driver".equals(cd.getClassName())) return new ClassDefinitionImpl<>(cd.getClassName(),
	 * "org.h2", null, config.getIdentification());
	 * if("net.sourceforge.jtds.jdbc.Driver".equals(cd.getClassName())) return new
	 * ClassDefinitionImpl<>(cd.getClassName(), "jtds", null, config.getIdentification());
	 * if("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(cd.getClassName())) return new
	 * ClassDefinitionImpl<>(cd.getClassName(), "microsoft.sqljdbc", null, config.getIdentification());
	 *
	 * } return cd; }
	 */

//	/**
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 */
//	/**
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 */
//	private static void loadCache(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			boolean hasCS = configServer != null;
//
//			// load Cache info
//			{
//				Element parent = getChildByName(doc.getDocumentElement(), "caches");
//				Element[] children = getChildren(parent, "cache");
//				Map<String, ClassDefinition> map = new HashMap<String, ClassDefinition>();
//
//				// first add the server drivers, so they can be overwritten
//				if (configServer != null) {
//					Iterator<ClassDefinition> it = configServer.getCacheDefinitions().values().iterator();
//					ClassDefinition cd;
//					while (it.hasNext()) {
//						cd = it.next();
//						map.put(cd.getClassName(), cd);
//					}
//				}
//
//				ClassDefinition cd;
//				String label;
//				for (Element child : children) {
//					cd = getClassDefinition(child, "", config.getIdentification());
//
//					// check if it is a bundle
//					if (!cd.isBundle()) {
//						log.error("Datasource", "[" + cd + "] does not have bundle info");
//						continue;
//					}
//					map.put(cd.getClassName(), cd);
//				}
//				config.setCacheDefinitions(map);
//			}
//
//			Map<String, CacheConnection> caches = new HashMap<String, CacheConnection>();
//
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManagerImpl.TYPE_CACHE);
//			// print.o("LOAD CACHE:"+hasAccess+":"+hasCS);
//
//			Element eCache = getChildByName(doc.getDocumentElement(), "cache");
//
//			// has changes
//
//			String md5 = getMD5(eCache, hasCS ? configServer.getCacheMD5() : "");
//			if (md5.equals(config.getCacheMD5())) return;
//			config.setCacheMD5(md5);
//
//			String[] typeNames = new String[]{"resource", "function", "include", "query", "template", "object", "file", "http", "webservice"};
//			int[] types = new int[]{ConfigImpl.CACHE_TYPE_RESOURCE, ConfigImpl.CACHE_TYPE_FUNCTION, ConfigImpl.CACHE_TYPE_INCLUDE, ConfigImpl.CACHE_TYPE_QUERY,
//					ConfigImpl.CACHE_TYPE_TEMPLATE, ConfigImpl.CACHE_TYPE_OBJECT, ConfigImpl.CACHE_TYPE_FILE, ConfigImpl.CACHE_TYPE_HTTP, ConfigImpl.CACHE_TYPE_WEBSERVICE};
//
//			// default cache
//			for (int i = 0; i < types.length; i++) {
//				String def = getAttr(eCache, "default-" + typeNames[i]);
//				if (hasAccess && !StringUtil.isEmpty(def)) {
//					config.setCacheDefaultConnectionName(types[i], def);
//				} else if (hasCS) {
//					if (eCache.hasAttribute("default-" + typeNames[i]))
//						config.setCacheDefaultConnectionName(types[i], "");
//					else
//						config.setCacheDefaultConnectionName(types[i], configServer.getCacheDefaultConnectionName(types[i]));
//				} else config.setCacheDefaultConnectionName(+types[i], "");
//			}
//
//			// cache connections
//			Element[] eConnections = getChildren(eCache, "connection");
//
//			// if(hasAccess) {
//			ClassDefinition cd;
//			String name;
//			CacheConnection cc;
//			// Class cacheClazz;
//			// caches
//			if (hasAccess) for (int i = 0; i < eConnections.length; i++) {
//				Element eConnection = eConnections[i];
//				name = getAttr(eConnection, "name");
//				cd = getClassDefinition(eConnection, "", config.getIdentification());
//				if (!cd.isBundle()) {
//					ClassDefinition _cd = config.getCacheDefinition(cd.getClassName());
//					if (_cd != null) cd = _cd;
//				}
//
//				{
//					Struct custom = toStruct(getAttr(eConnection, "custom"));
//
//					// Workaround for old EHCache class definitions
//					if (cd.getClassName() != null && cd.getClassName().endsWith(".EHCacheLite")) {
//						cd = new ClassDefinitionImpl("org.lucee.extension.cache.eh.EHCache");
//						if (!custom.containsKey("distributed")) custom.setEL("distributed", "off");
//						if (!custom.containsKey("asynchronousReplicationIntervalMillis"))
//							custom.setEL("asynchronousReplicationIntervalMillis", "1000");
//						if (!custom.containsKey("maximumChunkSizeBytes"))
//							custom.setEL("maximumChunkSizeBytes", "5000000");
//
//					} //
//					else if (cd.getClassName() != null
//							&& (cd.getClassName().endsWith(".extension.io.cache.eh.EHCache") || cd.getClassName().endsWith("lucee.runtime.cache.eh.EHCache")))
//						cd = new ClassDefinitionImpl("org.lucee.extension.cache.eh.EHCache");
//					// else cacheClazz = cd.getClazz();
//
//					cc = new CacheConnectionImpl(config, name, cd, custom, Caster.toBooleanValue(getAttr(eConnection, "read-only"), false),
//							Caster.toBooleanValue(getAttr(eConnection, "storage"), false));
//					if (!StringUtil.isEmpty(name)) {
//						caches.put(name.toLowerCase(), cc);
//					} else SystemOut.print(config.getErrWriter(), "missing cache name");
//
//				}
//
//			}
//			// }
//
//			// call static init once per driver
//			{
//				// group by classes
//				final Map<ClassDefinition, List<CacheConnection>> _caches = new HashMap<ClassDefinition, List<CacheConnection>>();
//				{
//					Iterator<Entry<String, CacheConnection>> it = caches.entrySet().iterator();
//					Entry<String, CacheConnection> entry;
//					List<CacheConnection> list;
//					while (it.hasNext()) {
//						entry = it.next();
//						cc = entry.getValue();
//						if (cc == null) continue;// Jira 3196 ?!
//						list = _caches.get(cc.getClassDefinition());
//						if (list == null) {
//							list = new ArrayList<CacheConnection>();
//							_caches.put(cc.getClassDefinition(), list);
//						}
//						list.add(cc);
//					}
//				}
//				// call
//				Iterator<Entry<ClassDefinition, List<CacheConnection>>> it = _caches.entrySet().iterator();
//				Entry<ClassDefinition, List<CacheConnection>> entry;
//				List<CacheConnection> list;
//				ClassDefinition _cd;
//				while (it.hasNext()) {
//					entry = it.next();
//					list = entry.getValue();
//					_cd = entry.getKey();
//					try {
//						Method m = _cd.getClazz().getMethod("init", new Class[]{Config.class, String[].class, Struct[].class});
//						if (Modifier.isStatic(m.getModifiers()))
//							m.invoke(null, new Object[]{config, _toCacheNames(list), _toArguments(list)});
//						else
//							SystemOut.print(config.getErrWriter(), "method [init(Config,String[],Struct[]):void] for class [" + _cd.toString() + "] is not static");
//
//					} catch (InvocationTargetException e) {
//						log.error("Cache", e.getTargetException());
//					} catch (RuntimeException e) {
//						log.error("Cache", e);
//					} catch (NoSuchMethodException e) {
//						log.error("Cache", "missing method [public static init(Config,String[],Struct[]):void] for class [" + _cd.toString() + "] ");
//						// SystemOut.print(config.getErrWriter(), "missing method [public static
//						// init(Config,String[],Struct[]):void] for class [" + _cd.toString() + "] ");
//					} catch (Throwable e) {
//						ExceptionUtil.rethrowIfNecessary(e);
//						log.error("Cache", e);
//					}
//				}
//			}
//
//			// Copy Parent caches as readOnly
//			if (hasCS) {
//				Map<String, CacheConnection> ds = configServer.getCacheConnections();
//				Iterator<Entry<String, CacheConnection>> it = ds.entrySet().iterator();
//				Entry<String, CacheConnection> entry;
//				while (it.hasNext()) {
//					entry = it.next();
//					cc = entry.getValue();
//					if (!caches.containsKey(entry.getKey()))
//						caches.put(entry.getKey(), new ServerCacheConnection(configServer, cc));
//				}
//			}
//			config.setCaches(caches);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	private static String getMD5(Node node, String parentMD5) {
		try {
			return MD5.getDigestAsString(XMLCaster.toString(node, "") + ":" + parentMD5);
		} catch (IOException e) {
			return "";
		}
	}

//	private static void loadGatewayEL(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			loadGateway(configServer, config, doc);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}
//
//	private static void loadGateway(ConfigServerImpl configServer, ConfigImpl config, Document doc) {
//		boolean hasCS = configServer != null;
//
//		// ConfigWebImpl cw = (ConfigWebImpl) config;
//
//		GatewayEngineImpl engine = hasCS ? ((ConfigWebImpl) config).getGatewayEngine() : null;
//		Map<String, GatewayEntry> mapGateways = new HashMap<String, GatewayEntry>();
//
//		// get from server context
//		if (hasCS) {
//			Map<String, GatewayEntry> entries = configServer.getGatewayEntries();
//			if (entries != null && !entries.isEmpty()) {
//				Iterator<Entry<String, GatewayEntry>> it = entries.entrySet().iterator();
//				Entry<String, GatewayEntry> e;
//				while (it.hasNext()) {
//					e = it.next();
//					mapGateways.put(e.getKey(), ((GatewayEntryImpl) e.getValue()).duplicateReadOnly(engine));
//				}
//			}
//		}
//
//		Element eGateWay = getChildByName(doc.getDocumentElement(), "gateways");
//		boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManagerImpl.TYPE_GATEWAY);
//		GatewayEntry ge;
//		// cache connections
//		Element[] gateways = getChildren(eGateWay, "gateway");
//
//		// if(hasAccess) {
//		String id;
//		// engine.reset();
//
//		// caches
//		if (hasAccess) {
//			for (int i = 0; i < gateways.length; i++) {
//				Element eConnection = gateways[i];
//				id = getAttr(eConnection, "id").trim().toLowerCase();
//
//				ge = new GatewayEntryImpl(engine, id, getClassDefinition(eConnection, "", config.getIdentification()), eConnection.getAttribute("cfc-path"),
//						eConnection.getAttribute("listener-cfc-path"), getAttr(eConnection, "startup-mode"), toStruct(getAttr(eConnection, "custom")),
//						Caster.toBooleanValue(getAttr(eConnection, "read-only"), false));
//
//				if (!StringUtil.isEmpty(id)) {
//					mapGateways.put(id.toLowerCase(), ge);
//				} else SystemOut.print(config.getErrWriter(), "missing id");
//			}
//			config.setGatewayEntries(mapGateways);
//		} else if (hasCS) {
//			((ConfigWebImpl) config).getGatewayEngine().clear();
//		}
//	}
//
//	private static Struct[] _toArguments(List<CacheConnection> list) {
//		Iterator<CacheConnection> it = list.iterator();
//		Struct[] args = new Struct[list.size()];
//		int index = 0;
//		while (it.hasNext()) {
//			args[index++] = it.next().getCustom();
//		}
//		return args;
//	}
//
//	private static String[] _toCacheNames(List<CacheConnection> list) {
//		Iterator<CacheConnection> it = list.iterator();
//		String[] names = new String[list.size()];
//		int index = 0;
//		while (it.hasNext()) {
//			names[index++] = it.next().getName();
//		}
//		return names;
//	}

	private static Struct toStruct(String str) {

		Struct sct = new StructImpl();
		try {
			String[] arr = ListUtil.toStringArray(ListUtil.listToArrayRemoveEmpty(str, '&'));

			String[] item;
			for (int i = 0; i < arr.length; i++) {
				item = ListUtil.toStringArray(ListUtil.listToArrayRemoveEmpty(arr[i], '='));
				if (item.length == 2)
					sct.setEL(KeyImpl.init(URLDecoder.decode(item[0], true).trim()), URLDecoder.decode(item[1], true));
				else if (item.length == 1) sct.setEL(KeyImpl.init(URLDecoder.decode(item[0], true).trim()), "");
			}
		} catch (PageException ee) {
		}

		return sct;
	}

//	private static void setDatasource(ConfigImpl config, Map<String, DataSource> datasources, String datasourceName, ClassDefinition cd, String server, String databasename,
//	                                  int port, String dsn, String user, String pass, TagListener listener, int connectionLimit, int connectionTimeout, long metaCacheTimeout, boolean blob, boolean clob,
//	                                  int allow, boolean validate, boolean storage, String timezone, Struct custom, String dbdriver, ParamSyntax ps, boolean literalTimestampWithTSOffset,
//	                                  boolean alwaysSetTimeout) throws BundleException, ClassException, SQLException {
//
//		datasources.put(datasourceName.toLowerCase(),
//				new DataSourceImpl(config, datasourceName, cd, server, dsn, databasename, port, user, pass, listener, connectionLimit, connectionTimeout, metaCacheTimeout, blob,
//						clob, allow, custom, false, validate, storage, StringUtil.isEmpty(timezone, true) ? null : TimeZoneUtil.toTimeZone(timezone, null), dbdriver, ps,
//						literalTimestampWithTSOffset, alwaysSetTimeout, config.getLog("application")));
//
//	}

	/*
	 * private static void setDatasourceEL(ConfigImpl config, Map<String, DataSource> datasources,
	 * String datasourceName, ClassDefinition cd, String server, String databasename, int port, String
	 * dsn, String user, String pass, int connectionLimit, int connectionTimeout, long metaCacheTimeout,
	 * boolean blob, boolean clob, int allow, boolean validate, boolean storage, String timezone, Struct
	 * custom, String dbdriver) { try { setDatasource(config, datasources, datasourceName, cd, server,
	 * databasename, port, dsn, user, pass, connectionLimit, connectionTimeout, metaCacheTimeout, blob,
	 * clob, allow, validate, storage, timezone, custom, dbdriver); } catch(Throwable t)
	 * {ExceptionUtil.rethrowIfNecessary(t);} }
	 */

//	/**
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 * @throws IOException
//	 */
//	private static void loadCustomTagsMappings(ConfigServerImpl configServer, ConfigImpl config, Document doc, int mode, Log log) {
//		try {
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManager.TYPE_CUSTOM_TAG);
//			boolean hasCS = configServer != null;
//
//			Element customTag = getChildByName(doc.getDocumentElement(), "custom-tag");
//			Element[] ctMappings = getChildren(customTag, "mapping");
//			// String virtualx="/custom-tag/";
//
//			// do patch cache
//			String strDoPathcache = getAttr(customTag, "use-cache-path");
//			if (hasAccess && !StringUtil.isEmpty(strDoPathcache, true)) {
//				config.setUseCTPathCache(Caster.toBooleanValue(strDoPathcache.trim(), true));
//			} else if (hasCS) {
//				config.setUseCTPathCache(configServer.useCTPathCache());
//			}
//
//			// do custom tag local search
//			if (mode == ConfigImpl.MODE_STRICT) {
//				config.setDoLocalCustomTag(false);
//			} else {
//				String strDoCTLocalSearch = getAttr(customTag, "custom-tag-local-search");
//				if (hasAccess && !StringUtil.isEmpty(strDoCTLocalSearch)) {
//					config.setDoLocalCustomTag(Caster.toBooleanValue(strDoCTLocalSearch.trim(), true));
//				} else if (hasCS) {
//					config.setDoLocalCustomTag(configServer.doLocalCustomTag());
//				}
//			}
//
//			// do custom tag deep search
//			if (mode == ConfigImpl.MODE_STRICT) {
//				config.setDoCustomTagDeepSearch(false);
//			} else {
//				String strDoCTDeepSearch = getAttr(customTag, "custom-tag-deep-search");
//				if (hasAccess && !StringUtil.isEmpty(strDoCTDeepSearch)) {
//					config.setDoCustomTagDeepSearch(Caster.toBooleanValue(strDoCTDeepSearch.trim(), false));
//				} else if (hasCS) {
//					config.setDoCustomTagDeepSearch(configServer.doCustomTagDeepSearch());
//				}
//			}
//
//			// extensions
//			if (mode == ConfigImpl.MODE_STRICT) {
//				config.setCustomTagExtensions(Constants.getComponentExtensions());
//			} else {
//				String strExtensions = getAttr(customTag, "extensions");
//				if (hasAccess && !StringUtil.isEmpty(strExtensions)) {
//					try {
//						String[] arr = ListUtil.toStringArray(ListUtil.listToArrayRemoveEmpty(strExtensions, ","));
//						config.setCustomTagExtensions(ListUtil.trimItems(arr));
//					} catch (PageException e) {
//					}
//				} else if (hasCS) {
//					config.setCustomTagExtensions(configServer.getCustomTagExtensions());
//				}
//			}
//
//			// Web Mapping
//			boolean hasSet = false;
//			Mapping[] mappings = null;
//			if (hasAccess && ctMappings.length > 0) {
//				mappings = new Mapping[ctMappings.length];
//				for (int i = 0; i < ctMappings.length; i++) {
//					Element ctMapping = ctMappings[i];
//					String physical = ctMapping.getAttribute("physical");
//					String archive = ctMapping.getAttribute("archive");
//					boolean readonly = toBoolean(getAttr(ctMapping, "readonly"), false);
//					boolean hidden = toBoolean(getAttr(ctMapping, "hidden"), false);
//					// boolean trusted = toBoolean(getAttr(ctMapping,"trusted"), false);
//					short inspTemp = inspectTemplate(ctMapping);
//					// int clMaxEl = toInt(getAttr(ctMapping,"classloader-max-elements"), 100);
//
//					String primary = getAttr(ctMapping, "primary");
//
//					boolean physicalFirst = archive == null || !primary.equalsIgnoreCase("archive");
//					hasSet = true;
//					mappings[i] = new MappingImpl(config, XMLConfigAdmin.createVirtual(ctMapping), physical, archive, inspTemp, physicalFirst, hidden, readonly, true, false, true,
//							null, -1, -1);
//					// print.out(mappings[i].isPhysicalFirst());
//				}
//
//				config.setCustomTagMappings(mappings);
//
//			}
//
//			// Server Mapping
//			if (hasCS) {
//				Mapping[] originals = configServer.getCustomTagMappings();
//				if (originals == null) originals = new Mapping[0];
//				Mapping[] clones = new Mapping[originals.length];
//				LinkedHashMap map = new LinkedHashMap();
//				Mapping m;
//				for (int i = 0; i < clones.length; i++) {
//					m = ((MappingImpl) originals[i]).cloneReadOnly(config);
//					map.put(toKey(m), m);
//					// clones[i]=((MappingImpl)m[i]).cloneReadOnly(config);
//				}
//
//				if (mappings != null) {
//					for (int i = 0; i < mappings.length; i++) {
//						m = mappings[i];
//						map.put(toKey(m), m);
//					}
//				}
//				if (originals.length > 0) {
//					clones = new Mapping[map.size()];
//					Iterator it = map.entrySet().iterator();
//					Map.Entry entry;
//					int index = 0;
//					while (it.hasNext()) {
//						entry = (Entry) it.next();
//						clones[index++] = (Mapping) entry.getValue();
//						// print.out("c:"+clones[index-1]);
//					}
//					hasSet = true;
//					// print.err("set:"+clones.length);
//
//					config.setCustomTagMappings(clones);
//				}
//			}
//
//			if (!hasSet) {
//				// MappingImpl m=new
//				// MappingImpl(config,"/default-customtags/","{lucee-web}/customtags/",null,false,true,false,false,true,false,true);
//				// config.setCustomTagMappings(new
//				// Mapping[]{m.cloneReadOnly(config)});
//			}
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//
//	}

	private static Object toKey(Mapping m) {
		if (!StringUtil.isEmpty(m.getStrPhysical(), true)) return m.getStrPhysical().toLowerCase().trim();
		return (m.getStrPhysical() + ":" + m.getStrArchive()).toLowerCase();
	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private static void loadConfig(ConfigServerImpl configServer, ConfigImpl config, Document doc) {
//		Element luceeConfiguration = doc.getDocumentElement();
//		String pwEnc = "pw";
//		String salt = "salt";
//		String rawPassword = new BlowfishEasy("tpwisgh").decryptString(pwEnc);
//		Password pw = new PasswordImpl(ORIGIN_ENCRYPTED, rawPassword, salt);
		// salt (every context need to have a salt)
//	String salt = getAttr(luceeConfiguration, "salt");
//	if (StringUtil.isEmpty(salt, true)) throw new RuntimeException("context is invalid, there is no salt!");
//	config.setSalt(salt = salt.trim());
//
//	// password
//	Password pw = PasswordImpl.readFromXML(luceeConfiguration, salt, false);
//		if (pw != null) config.setPassword(pw);
//		else if (configServer != null) config.setPassword(configServer.getDefaultPassword());
//
//		if (config instanceof ConfigServerImpl) {
//			ConfigServerImpl csi = (ConfigServerImpl) config;
//			String keyList = getAttr(luceeConfiguration, "auth-keys");
//			if (!StringUtil.isEmpty(keyList)) {
//				String[] keys = ListUtil.trimItems(ListUtil.toStringArray(ListUtil.toListRemoveEmpty(keyList, ',')));
//				for (int i = 0; i < keys.length; i++) {
//					try {
//						keys[i] = URLDecoder.decode(keys[i], "UTF-8", true);
//					} catch (UnsupportedEncodingException e) {
//					}
//				}
//
//				csi.setAuthenticationKeys(keys);
//			}
//		}

		// default password
//		if (config instanceof ConfigServerImpl) {
//			pw = PasswordImpl.readFromXML(luceeConfiguration, salt, true);
//			if (pw != null) ((ConfigServerImpl) config).setDefaultPassword(pw);
//		}

		// mode
//		String mode = getAttr(luceeConfiguration, "mode");
//		if (!StringUtil.isEmpty(mode, true)) {
//			mode = mode.trim();
//			if ("custom".equalsIgnoreCase(mode)) config.setMode(ConfigImpl.MODE_CUSTOM);
//			if ("strict".equalsIgnoreCase(mode)) config.setMode(ConfigImpl.MODE_STRICT);
//		} else
		if (configServer != null) {
			config.setMode(configServer.getMode());
		}

		// check config file for changes
//		String cFc = getAttr(luceeConfiguration, "check-for-changes");
//		if (!StringUtil.isEmpty(cFc, true)) {
//			config.setCheckForChangesInConfigFile(Caster.toBooleanValue(cFc.trim(), false));
//		} else if (configServer != null) {
//			config.setCheckForChangesInConfigFile(configServer.checkForChangesInConfigFile());
//		}
	}

	/*
	 * private static void loadLabel(ConfigServerImpl configServer, ConfigImpl config, Document doc) {
	 * // do only for web config if(configServer!=null && config instanceof ConfigWebImpl) {
	 * ConfigWebImpl cs=(ConfigWebImpl) config; String hash=SystemUtil.hash(cs.getServletContext());
	 * config.setLabel(hash);
	 *
	 * Map<String, String> labels = configServer.getLabels(); if(labels!=null) { String label =
	 * labels.get(hash); if(!StringUtil.isEmpty(label)) { print.o("label:"+label);
	 * config.setLabel(label); config.getFactory().setLabel(label); } } } }
	 */

//	private static void loadTag(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			Element parent = getChildByName(doc.getDocumentElement(), "tags");
//			{
//				Element[] tags = getChildren(parent, "tag");
//				Element tag;
//				ClassDefinition cd;
//				String nss, ns, n;
//				if (tags != null) {
//					for (int i = 0; i < tags.length; i++) {
//						tag = tags[i];
//						ns = getAttr(tag, "namespace");
//						nss = getAttr(tag, "namespace-seperator");
//						n = getAttr(tag, "name");
//						cd = getClassDefinition(tag, "", config.getIdentification());
//						config.addTag(ns, nss, n, CFMLEngine.DIALECT_CFML, cd);
//					}
//				}
//			}
//
//			// set tag default values
//			Element[] defaults = getChildren(parent, "default");
//			if (!ArrayUtil.isEmpty(defaults)) {
//				Element def;
//				String tagName, attrName, attrValue;
//				Struct tags = new StructImpl(), tag;
//				Map<Key, Map<Key, Object>> trg = new HashMap<Key, Map<Key, Object>>();
//				for (int i = 0; i < defaults.length; i++) {
//					def = defaults[i];
//					tagName = getAttr(def, "tag");
//					attrName = getAttr(def, "attribute-name");
//					attrValue = getAttr(def, "attribute-value");
//					if (StringUtil.isEmpty(tagName) || StringUtil.isEmpty(attrName) || StringUtil.isEmpty(attrValue))
//						continue;
//
//					tag = (Struct) tags.get(tagName, null);
//					if (tag == null) {
//						tag = new StructImpl();
//						tags.setEL(tagName, tag);
//					}
//					tag.setEL(attrName, attrValue);
//					ApplicationContextSupport.initTagDefaultAttributeValues(config, trg, tags, CFMLEngine.DIALECT_CFML);
////		    ApplicationContextSupport.initTagDefaultAttributeValues(config, trg, tags, CFMLEngine.DIALECT_LUCEE);
//					config.setTagDefaultAttributeValues(trg);
//				}
//
//				// initTagDefaultAttributeValues
//
//			}
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	private static void loadTempDirectory(ConfigServerImpl configServer, ConfigImpl config, Document doc, boolean isReload, Log log) {
		try {
			Resource configDir = config.getConfigDir();
			config.tempDirectory=config.getResource(configDir.getAbsolutePath()+StaticConfig.fileSystemWebTempDirectory);
			if(!config.tempDirectory.exists()) {
				config.tempDirectory.createDirectory(false);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 * @throws ExpressionException
	 * @throws TagLibException
	 * @throws FunctionLibException
	 */
	private static void loadFilesystem(ConfigServerImpl configServer, ConfigImpl config, Document doc, boolean doNew, Log log) {
		try {
			if (configServer != null) {
				Resource src = configServer.getConfigDir().getRealResource("distribution");
				Resource trg = config.getConfigDir().getRealResource("context/");
				copyContextFiles(src, trg);
			}
			Resource configDir = config.getConfigDir();

			boolean hasCS = configServer != null;

			String strAllowRealPath = null;
			String strDeployDirectory = null;
			String strFuncDirectory = null;
			String strTagDirectory = null;

//			Element fileSystem = getChildByName(doc.getDocumentElement(), "file-system");
//			if (fileSystem == null) fileSystem = getChildByName(doc.getDocumentElement(), "filesystem");

			// get library directories
			if (configServer != null) {
				strDeployDirectory = ConfigWebUtil.translateOldPath(StaticConfig.fileSystemDeployDirectory);
			}else{
				strDeployDirectory = ConfigWebUtil.translateOldPath(StaticConfig.fileSystemWebDeployDirectory);
			}

			// set default directories if necessary
			String strDefaultFLDDirectory = "{lucee-config}/library/fld/";
			String strDefaultTLDDirectory = "{lucee-config}/library/tld/";
			String strDefaultFuncDirectory = "{lucee-config}/library/function/";
			String strDefaultTagDirectory = "{lucee-config}/library/tag/";

//			if(hasCS) {
//				Resource configDirServer=configServer.getConfigDir();
//				Resource tldDir = configDirServer.getRealResource("context/library/tld");
//				if(!tldDir.exists()) {
//					try {
//						tldDir.createDirectory(true);
//					} catch (IOException e) {
//						throw new RuntimeException(e);
//					}
//				}
//				Resource f = tldDir.getRealResource("core-base.tld");
//				if (!f.exists()) createFileFromResourceEL("/resource/tld/core-base.tld", f);
//
//				Resource fldDir = configDirServer.getRealResource("context/library/fld");
//				if(!fldDir.exists()) {
//					try {
//						fldDir.createDirectory(true);
//					} catch (IOException e) {
//						throw new RuntimeException(e);
//					}
//				}
//				f = fldDir.getRealResource("core-base.fld");
//				if (!f.exists()) createFileFromResourceEL("/resource/fld/core-base.fld", f);
//			}

			// Deploy Dir
			Resource dd = ConfigWebUtil.getFile(configDir, strDeployDirectory, "cfclasses", configDir, FileUtil.TYPE_DIR, config);
			config.setDeployDirectory(dd);

			// TAG

			// init TLDS
			if (hasCS) {
				config.setTLDs(configServer.getTLDs(CFMLEngine.DIALECT_CFML), CFMLEngine.DIALECT_CFML);
			} else {
				ConfigServerImpl cs = (ConfigServerImpl) config;
				config.setTLDs(new TagLib[]{cs.cfmlCoreTLDs}, CFMLEngine.DIALECT_CFML);
			}


			// TLD Dir
			if (!StringUtil.isEmpty(strDefaultTLDDirectory)) {
				Resource tld = ConfigWebUtil.getFile(config, configDir, strDefaultTLDDirectory, FileUtil.TYPE_DIR);
				if (tld != null) config.setTldFile(tld, CFMLEngine.DIALECT_CFML);
			}

			// Tag Directory
			List<Resource> listTags = new ArrayList<Resource>();
			if (!StringUtil.isEmpty(strDefaultTagDirectory)) {
				Resource dir = ConfigWebUtil.getFile(config, configDir, strDefaultTagDirectory, FileUtil.TYPE_DIR);
//				createTagFiles(config, configDir, dir, doNew);
				if (dir != null) listTags.add(dir);
			}
			if (!StringUtil.isEmpty(strTagDirectory)) {
				String[] arr = ListUtil.listToStringArray(strTagDirectory, ',');
				for (String str : arr) {
					str = str.trim();
					if (StringUtil.isEmpty(str)) continue;
					Resource dir = ConfigWebUtil.getFile(config, configDir, str, FileUtil.TYPE_DIR);
					if (dir != null) listTags.add(dir);
				}
			}
			config.setTagDirectory(listTags);

			// allow realpath
			if (hasCS) {
				config.setAllowRealPath(configServer.allowRealPath());
			}
			if (!StringUtil.isEmpty(strAllowRealPath, true)) {
				config.setAllowRealPath(Caster.toBooleanValue(strAllowRealPath, true));
			}

			// FUNCTIONS

			// Init flds
			if (hasCS) {
				config.setFLDs(configServer.getFLDs(CFMLEngine.DIALECT_CFML), CFMLEngine.DIALECT_CFML);
			} else {
				ConfigServerImpl cs = (ConfigServerImpl) config;
				config.setFLDs(new FunctionLib[]{cs.cfmlCoreFLDs}, CFMLEngine.DIALECT_CFML);
			}

			// FLDs
			if (!StringUtil.isEmpty(strDefaultFLDDirectory)) {
				Resource fld = ConfigWebUtil.getFile(config, configDir, strDefaultFLDDirectory, FileUtil.TYPE_DIR);
				if (fld != null) config.setFldFile(fld, CFMLEngine.DIALECT_CFML);
			}

			// Function files (CFML)
			List<Resource> listFuncs = new ArrayList<Resource>();
			if (!StringUtil.isEmpty(strDefaultFuncDirectory)) {
				Resource dir = ConfigWebUtil.getFile(config, configDir, strDefaultFuncDirectory, FileUtil.TYPE_DIR);
//				createFunctionFiles(config, configDir, dir, doNew);
				if (dir != null) listFuncs.add(dir);
				// if (dir != null) config.setFunctionDirectory(dir);
			}
			if (!StringUtil.isEmpty(strFuncDirectory)) {
				String[] arr = ListUtil.listToStringArray(strFuncDirectory, ',');
				for (String str : arr) {
					str = str.trim();
					if (StringUtil.isEmpty(str)) continue;
					Resource dir = ConfigWebUtil.getFile(config, configDir, str, FileUtil.TYPE_DIR);
					if (dir != null) listFuncs.add(dir);
					// if (dir != null) config.setFunctionDirectory(dir);
				}
			}
			config.setFunctionDirectory(listFuncs);
		} catch (Exception e) {
			log(config, log, e);
		}
	}
//    private static void loadFilesystem(ConfigServerImpl configServer, ConfigImpl config, Document doc, boolean doNew, Log log) {
//	try {
////	    if (configServer != null) {
////			Resource src = configServer.getConfigDir().getRealResource("distribution");
////			Resource trg = config.getConfigDir().getRealResource("context/");
////			copyContextFiles(src, trg);
////	    }
//	    Resource configDir = config.getConfigDir();
//
//	    boolean hasCS = configServer != null;
//
//	    String strAllowRealPath = null;
//	    String strDeployDirectory = null;
////	    String strFuncDirectory=null;
////	    String strTagDirectory=null;
//
////	    Element fileSystem = getChildByName(doc.getDocumentElement(), "file-system");
////	    if (fileSystem == null) fileSystem = getChildByName(doc.getDocumentElement(), "filesystem");
//
//	    // get library directories
////	    if (fileSystem != null) {
////			strAllowRealPath = getAttr(fileSystem, "allow-realpath");
////			strDeployDirectory = ConfigWebUtil.translateOldPath(fileSystem.getAttribute("deploy-directory"));
////
////			strTagDirectory = ConfigWebUtil.translateOldPath(fileSystem.getAttribute("tag-addional-directory"));
////			strFuncDirectory = ConfigWebUtil.translateOldPath(fileSystem.getAttribute("function-addional-directory"));
////	    }
//
//	    // set default directories if necessary
////	    String strDefaultFLDDirectory = "{lucee-config}/library/fld/";
////	    String strDefaultTLDDirectory = "{lucee-config}/library/tld/";
////	    String strDefaultFuncDirectory = "{lucee-config}/library/function/";
////	    String strDefaultTagDirectory = "{lucee-config}/library/tag/";
//
//	    // Deploy Dir
//	    Resource dd = ConfigWebUtil.getFile(configDir, strDeployDirectory, "cfclasses", configDir, FileUtil.TYPE_DIR, config);
//	    config.setDeployDirectory(dd);
//
//	    // TAG
//
//	    // init TLDS
//	    if (hasCS) {
//			config.setTLDs(configServer.getTLDs(CFMLEngine.DIALECT_CFML), CFMLEngine.DIALECT_CFML);
//	    }
//	    else {
//			ConfigServerImpl cs = (ConfigServerImpl) config;
//			// wait for the tlds to be loaded
//			while(cs.cfmlCoreTLDs == null){
//				if(TagLibFactory.systemTLDs.length>0) {
//					cs.cfmlCoreTLDs = TagLibFactory.systemTLDs[CFMLEngine.DIALECT_CFML];
//					break;
//				}
//				Thread.sleep(5);
//			}
//			config.setTLDs(new TagLib[] { cs.cfmlCoreTLDs }, CFMLEngine.DIALECT_CFML);
//	    }
//
//	    // TLD Dir
////	    if (!StringUtil.isEmpty(strDefaultTLDDirectory)) {
////			Resource tld = ConfigWebUtil.getFile(config, configDir, strDefaultTLDDirectory, FileUtil.TYPE_DIR);
////			if (tld != null) config.setTldFile(tld, CFMLEngine.DIALECT_BOTH);
////	    }
//
//	    // Tag Directory
//	    List<Resource> listTags = new ArrayList<Resource>();
////		if (!StringUtil.isEmpty(strDefaultTagDirectory)) {
////			Resource dir = ConfigWebUtil.getFile(config, configDir, strDefaultTagDirectory, FileUtil.TYPE_DIR);
////			createTagFiles(config, configDir, dir, doNew);
////			if (dir != null) listTags.add(dir);
////		}
////		if (!StringUtil.isEmpty(strTagDirectory)) {
////			String[] arr = ListUtil.listToStringArray(strTagDirectory, ',');
////			for (String str : arr) {
////				str = str.trim();
////				if (StringUtil.isEmpty(str)) continue;
////				Resource dir = ConfigWebUtil.getFile(config, configDir, str, FileUtil.TYPE_DIR);
////				if (dir != null) listTags.add(dir);
////			}
////		}
////		config.setTagDirectory(listTags);
//
//		// allow realpath
//		if (hasCS) {
//			config.setAllowRealPath(configServer.allowRealPath());
//		}
//		if (!StringUtil.isEmpty(strAllowRealPath, true)) {
//			config.setAllowRealPath(Caster.toBooleanValue(strAllowRealPath, true));
//		}
//
//		// FUNCTIONS
//
//		// Init flds
//		if (hasCS) {
//			config.setFLDs(configServer.getFLDs(CFMLEngine.DIALECT_CFML), CFMLEngine.DIALECT_CFML);
//		} else {
//			ConfigServerImpl cs = (ConfigServerImpl) config;
//			// wait for the tlds to be loaded
//			while(cs.cfmlCoreFLDs == null){
//				if(FunctionLibFactory.systemFLDs.length>0) {
//					cs.cfmlCoreFLDs = FunctionLibFactory.systemFLDs[CFMLEngine.DIALECT_CFML];
//					break;
//				}
//				Thread.sleep(5);
//			}
//			config.setFLDs(new FunctionLib[]{cs.cfmlCoreFLDs}, CFMLEngine.DIALECT_CFML);
//		}
//
//		// FLDs
////		if (!StringUtil.isEmpty(strDefaultFLDDirectory)) {
////			Resource fld = ConfigWebUtil.getFile(config, configDir, strDefaultFLDDirectory, FileUtil.TYPE_DIR);
////			if (fld != null) config.setFldFile(fld, CFMLEngine.DIALECT_BOTH);
////		}
//
//		// Function files (CFML)
//		List<Resource> listFuncs = new ArrayList<Resource>();
////		if (!StringUtil.isEmpty(strDefaultFuncDirectory)) {
////			Resource dir = ConfigWebUtil.getFile(config, configDir, strDefaultFuncDirectory, FileUtil.TYPE_DIR);
////			createFunctionFiles(config, configDir, dir, doNew);
////			if (dir != null) listFuncs.add(dir);
////			// if (dir != null) config.setFunctionDirectory(dir);
////		}
////		if (!StringUtil.isEmpty(strFuncDirectory)) {
////			String[] arr = ListUtil.listToStringArray(strFuncDirectory, ',');
////			for (String str : arr) {
////				str = str.trim();
////				if (StringUtil.isEmpty(str)) continue;
////				Resource dir = ConfigWebUtil.getFile(config, configDir, str, FileUtil.TYPE_DIR);
////				if (dir != null) listFuncs.add(dir);
////				// if (dir != null) config.setFunctionDirectory(dir);
////			}
////		}
////	    config.setFunctionDirectory(listFuncs);
//	}
//	catch (Exception e) {
//	    log(config, log, e);
//	}
//    }

//	private static void createTagFiles(Config config, Resource configDir, Resource dir, boolean doNew) {
//		if (config instanceof ConfigServer) {
//
//			// Dump
//			create("/resource/library/tag/", new String[]{"Dump." + COMPONENT_EXTENSION}, dir, doNew);
//
//			/*
//			 * Resource sub = dir.getRealResource("lucee/dump/skins/");
//			 * create("/resource/library/tag/lucee/dump/skins/",new String[]{
//			 * "text."+CFML_TEMPLATE_MAIN_EXTENSION ,"simple."+CFML_TEMPLATE_MAIN_EXTENSION
//			 * ,"modern."+CFML_TEMPLATE_MAIN_EXTENSION ,"classic."+CFML_TEMPLATE_MAIN_EXTENSION
//			 * ,"pastel."+CFML_TEMPLATE_MAIN_EXTENSION },sub,doNew);
//			 */
//
//			// MediaPlayer
//			Resource f = dir.getRealResource("MediaPlayer." + COMPONENT_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/tag/MediaPlayer." + COMPONENT_EXTENSION, f);
//
//			// /resource/library/tag/build
//			Resource build = dir.getRealResource("build");
//			if (!build.exists()) build.mkdirs();
//			String[] names = new String[]{"_background.png", "_bigplay.png", "_controls.png", "_loading.gif", "_player.swf", "_player.xap",
//					"background_png." + TEMPLATE_EXTENSION, "bigplay_png." + TEMPLATE_EXTENSION, "controls_png." + TEMPLATE_EXTENSION, "jquery.js." + TEMPLATE_EXTENSION,
//					"loading_gif." + TEMPLATE_EXTENSION, "mediaelement-and-player.min.js." + TEMPLATE_EXTENSION, "mediaelementplayer.min.css." + TEMPLATE_EXTENSION,
//					"player.swf." + TEMPLATE_EXTENSION, "player.xap." + TEMPLATE_EXTENSION};
//			for (int i = 0; i < names.length; i++) {
//				f = build.getRealResource(names[i]);
//				if (!f.exists() || doNew) createFileFromResourceEL("/resource/library/tag/build/" + names[i], f);
//
//			}
//
//			// /resource/library/tag/build/jquery
//			Resource jquery = build.getRealResource("jquery");
//			if (!jquery.isDirectory()) jquery.mkdirs();
//			names = new String[]{"jquery-1.12.4.min.js"};
//			for (int i = 0; i < names.length; i++) {
//				f = jquery.getRealResource(names[i]);
//				if (!f.exists() || doNew) createFileFromResourceEL("/resource/library/tag/build/jquery/" + names[i], f);
//			}
//
//			// AJAX
//			// AjaxFactory.deployTags(dir, doNew);
//
//		}
//	}

//	private static void createFunctionFiles(Config config, Resource configDir, Resource dir, boolean doNew) {
//
//		if (config instanceof ConfigServer) {
//			Resource f = dir.getRealResource("writeDump." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/writeDump." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("dump." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/dump." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("location." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/location." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("threadJoin." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/threadJoin." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("threadTerminate." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/threadTerminate." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("throw." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/throw." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("trace." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/trace." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("queryExecute." + TEMPLATE_EXTENSION);
//			// if (!f.exists() || doNew)
//			// createFileFromResourceEL("/resource/library/function/queryExecute."+TEMPLATE_EXTENSION, f);
//			if (f.exists())// FUTURE add this instead if(updateType=NEW_FRESH || updateType=NEW_FROM4)
//				delete(dir, "queryExecute." + TEMPLATE_EXTENSION);
//
//			f = dir.getRealResource("transactionCommit." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/transactionCommit." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("transactionRollback." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/transactionRollback." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("transactionSetsavepoint." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/transactionSetsavepoint." + TEMPLATE_EXTENSION, f);
//
//			f = dir.getRealResource("writeLog." + TEMPLATE_EXTENSION);
//			if (!f.exists() || doNew)
//				createFileFromResourceEL("/resource/library/function/writeLog." + TEMPLATE_EXTENSION, f);
//
//			// AjaxFactory.deployFunctions(dir, doNew);
//
//		}
//	}

	private static void copyContextFiles(Resource src, Resource trg) {
		// directory
		if (src.isDirectory()) {
			if (trg.exists()) trg.mkdirs();
			Resource[] children = src.listResources();
			for (int i = 0; i < children.length; i++) {
				copyContextFiles(children[i], trg.getRealResource(children[i].getName()));
			}
		}
		// file
		else if (src.isFile()) {
			if (src.lastModified() > trg.lastModified()) {
				try {
					if (trg.exists()) trg.remove(true);
					trg.createFile(true);
					src.copyTo(trg, false);
				} catch (IOException e) {
					SystemOut.printDate(e);
				}
			}

		}
	}

//	/**
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 */
//	private static void loadUpdate(ConfigServer configServer, Config config, Document doc, Log log) {
//		try {
//			// Server
//			if (config instanceof ConfigServer) {
//				ConfigServer cs = (ConfigServer) config;
//				Element update = getChildByName(doc.getDocumentElement(), "update");
//
//				if (update != null) {
//					cs.setUpdateType(getAttr(update, "type"));
//
//					String location = getAttr(update, "location");
//					if (location != null) {
//						location = location.trim();
//						if ("http://snapshot.lucee.org".equals(location)) location = "http://update.lucee.org";
//						if ("http://release.lucee.org".equals(location)) location = "http://update.lucee.org";
//					}
//				}
//			}
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

//	private static void loadVideo(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			Element video = config instanceof ConfigServerImpl ? getChildByName(doc.getDocumentElement(), "video") : null;
//			boolean hasCS = configServer != null;
//			ClassDefinition cd = null;
//			// video-executer
//			if (video != null) {
//				cd = getClassDefinition(video, "video-executer-", config.getIdentification());
//			}
//
//			if (cd != null && cd.hasClass()) {
//
//				try {
//					Class clazz = cd.getClazz();
//					if (!Reflector.isInstaneOf(clazz, VideoExecuter.class, false))
//						throw new ApplicationException("class [" + cd + "] does not implement interface [" + VideoExecuter.class.getName() + "]");
//					config.setVideoExecuterClass(clazz);
//
//				} catch (Exception e) {
//					SystemOut.printDate(e);
//				}
//			} else if (hasCS) config.setVideoExecuterClass(configServer.getVideoExecuterClass());
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 */
	private static void loadSetting(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		config.setSuppressContent(StaticConfig.settingSuppressContent);

		if ("white-space".equalsIgnoreCase(StaticConfig.settingCFMLWriter)) config.setCFMLWriterType(ConfigImpl.CFML_WRITER_WS);
		else if ("white-space-pref".equalsIgnoreCase(StaticConfig.settingCFMLWriter))
			config.setCFMLWriterType(ConfigImpl.CFML_WRITER_WS_PREF);
		else if ("regular".equalsIgnoreCase(StaticConfig.settingCFMLWriter)) config.setCFMLWriterType(ConfigImpl.CFML_WRITER_REFULAR);

		config.setShowVersion(false);
		config.setCloseConnection(false);
		config.setContentLength(StaticConfig.settingContentLength);
		config.setBufferOutput(StaticConfig.settingBufferingOutput);

		config.setAllowCompression(StaticConfig.settingAllowCompression);

		config.setDevelopMode(false);
	}

//	private static void loadRemoteClient(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManagerImpl.TYPE_REMOTE);
//
//			// SNSN
//			// RemoteClientUsage
//
//			// boolean hasCS=configServer!=null;
//			Element _clients = getChildByName(doc.getDocumentElement(), "remote-clients");
//
//			// usage
//			String strUsage = getAttr(_clients, "usage");
//			Struct sct;
//			if (!StringUtil.isEmpty(strUsage))
//				sct = toStruct(strUsage);// config.setRemoteClientUsage(toStruct(strUsage));
//			else sct = new StructImpl();
//			// TODO make this generic
//			if (configServer != null) {
//				String sync = Caster.toString(configServer.getRemoteClientUsage().get("synchronisation", ""), "");
//				if (!StringUtil.isEmpty(sync)) {
//					sct.setEL("synchronisation", sync);
//				}
//			}
//			config.setRemoteClientUsage(sct);
//
//			// max-threads
//			int maxThreads = Caster.toIntValue(getAttr(_clients, "max-threads"), -1);
//			if (maxThreads < 1 && configServer != null) {
//				SpoolerEngineImpl engine = (SpoolerEngineImpl) configServer.getSpoolerEngine();
//				if (engine != null) maxThreads = engine.getMaxThreads();
//			}
//			if (maxThreads < 1) maxThreads = 20;
//
//			// directory
//			String strDir = SystemUtil.getSystemPropOrEnvVar("lucee.task.directory", null);
//			if (StringUtil.isEmpty(strDir)) strDir = _clients.getAttribute("directory");
//			Resource file = ConfigWebUtil.getFile(config.getRootDirectory(), strDir, "client-task", config.getConfigDir(), FileUtil.TYPE_DIR, config);
//			config.setRemoteClientDirectory(file);
//
//			Element[] clients;
//			Element client;
//
//			if (!hasAccess) clients = new Element[0];
//			else clients = getChildren(_clients, "remote-client");
//			java.util.List<RemoteClient> list = new ArrayList<RemoteClient>();
//			for (int i = 0; i < clients.length; i++) {
//				client = clients[i];
//				// type
//				String type = getAttr(client, "type");
//				if (StringUtil.isEmpty(type)) type = "web";
//				// url
//				String url = getAttr(client, "url");
//				String label = getAttr(client, "label");
//				if (StringUtil.isEmpty(label)) label = url;
//				String sUser = getAttr(client, "server-username");
//				String sPass = ConfigWebUtil.decrypt(getAttr(client, "server-password"));
//				String aPass = ConfigWebUtil.decrypt(getAttr(client, "admin-password"));
//				String aCode = ConfigWebUtil.decrypt(getAttr(client, "security-key"));
//				// if(aCode!=null && aCode.indexOf('-')!=-1)continue;
//				String usage = getAttr(client, "usage");
//				if (usage == null) usage = "";
//
//				String pUrl = getAttr(client, "proxy-server");
//				int pPort = Caster.toIntValue(getAttr(client, "proxy-port"), -1);
//				String pUser = getAttr(client, "proxy-username");
//				String pPass = ConfigWebUtil.decrypt(getAttr(client, "proxy-password"));
//
//				ProxyData pd = null;
//				if (!StringUtil.isEmpty(pUrl, true)) {
//					pd = new ProxyDataImpl();
//					pd.setServer(pUrl);
//					if (!StringUtil.isEmpty(pUser)) {
//						pd.setUsername(pUser);
//						pd.setPassword(pPass);
//					}
//					if (pPort > 0) pd.setPort(pPort);
//				}
//				list.add(new RemoteClientImpl(label, type, url, sUser, sPass, aPass, pd, aCode, usage));
//			}
//			if (list.size() > 0) config.setRemoteClients(list.toArray(new RemoteClient[list.size()]));
//			else config.setRemoteClients(new RemoteClient[0]);
//
//			// init spooler engine
//			Resource dir = config.getRemoteClientDirectory();
//			if (dir != null && !dir.exists()) dir.mkdirs();
//			if (config.getSpoolerEngine() == null) {
//				config.setSpoolerEngine(new SpoolerEngineImpl(config, dir, "Remote Client Spooler", config.getLog("remoteclient"), maxThreads));
//			} else {
//				SpoolerEngineImpl engine = (SpoolerEngineImpl) config.getSpoolerEngine();
//				engine.setConfig(config);
//				engine.setLog(config.getLog("remoteclient"));
//				engine.setPersisDirectory(dir);
//				engine.setMaxThreads(maxThreads);
//
//			}
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	private static void loadSystem(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {

		config.setOut(toPrintwriter(config, StaticConfig.systemOut, false));
		config.setErr(toPrintwriter(config, StaticConfig.systemError, true));
	}

	private static PrintWriter toPrintwriter(ConfigImpl config, String streamtype, boolean iserror) {
		if (!StringUtil.isEmpty(streamtype)) {
			streamtype = streamtype.trim();

			if (streamtype.equalsIgnoreCase("null")) return new PrintWriter(DevNullOutputStream.DEV_NULL_OUTPUT_STREAM);
			else if (StringUtil.startsWithIgnoreCase(streamtype, "class:")) {
				String classname = streamtype.substring(6);
				try {
					return (PrintWriter) ClassUtil.loadInstance(classname);
				} catch (Throwable t) {
					ExceptionUtil.rethrowIfNecessary(t);
				}
			} else if (StringUtil.startsWithIgnoreCase(streamtype, "file:")) {
				String strRes = streamtype.substring(5);
				try {
					strRes = ConfigWebUtil.translateOldPath(strRes);
					Resource res = ConfigWebUtil.getFile(config, config.getConfigDir(), strRes, ResourceUtil.TYPE_FILE);
					if (res != null) return new PrintWriter(res.getOutputStream(), true);
				} catch (Throwable t) {
					ExceptionUtil.rethrowIfNecessary(t);
				}
			}

		}
		if (iserror) return SystemUtil.getPrintWriter(SystemUtil.ERR);
		return SystemUtil.getPrintWriter(SystemUtil.OUT);
	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 */
	private static void loadCharset(ConfigServer configServer, ConfigImpl config, Document doc, Log log) {
		config.setTemplateCharset("UTF-8");
		config.setWebCharset("UTF-8");
		config.setResourceCharset("UTF-8");
	}

	private static void loadQueue(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		// Server
		if (config instanceof ConfigServerImpl) {

			config.setQueueMax(StaticConfig.queueMax);
			config.setQueueTimeout(StaticConfig.queueTimeout);
			config.setQueueEnable(StaticConfig.queueEnable);
			((ConfigServerImpl) config).setThreadQueue(config.getQueueEnable() ? new ThreadQueueImpl() : new ThreadQueueNone());
		}
		// Web
		else {
			config.setQueueMax(configServer.getQueueMax());
			config.setQueueTimeout(configServer.getQueueTimeout());
			config.setQueueEnable(configServer.getQueueEnable());
		}
	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 */
	private static void loadRegional(ConfigServer configServer, ConfigImpl config, Document doc, Log log) {

		config.setTimeZone(StaticConfig.regionalTimeZone);
		TimeZone.setDefault(StaticConfig.regionalTimeZone);
		config.setTimeServer(StaticConfig.regionalTimeServer);
		config.setLocale(StaticConfig.regionalLocale);
	}

//	private static void loadWS(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			Element el = getChildByName(doc.getDocumentElement(), "webservice");
//			ClassDefinition cd = getClassDefinition(el, "", config.getIdentification());
//
//			if (cd != null && !StringUtil.isEmpty(cd.getClassName())) config.setWSHandlerClassDefinition(cd);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

//	private static void loadORM(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
////			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManagerImpl.TYPE_ORM);
//
//			Element orm = hasAccess ? getChildByName(doc.getDocumentElement(), "orm") : null;
//			boolean hasCS = configServer != null;
//
//			// engine
//			ClassDefinition cdDefault = new ClassDefinitionImpl(DummyORMEngine.class);
//
//			ClassDefinition cd = null;
//			if (orm != null) {
//				cd = getClassDefinition(orm, "engine-", config.getIdentification());
//				if (cd == null || cd.isClassNameEqualTo(DummyORMEngine.class.getName()) || cd.isClassNameEqualTo("lucee.runtime.orm.hibernate.HibernateORMEngine"))
//					cd = getClassDefinition(orm, "", config.getIdentification());
//
//				if (cd != null && (cd.isClassNameEqualTo(DummyORMEngine.class.getName()) || cd.isClassNameEqualTo("lucee.runtime.orm.hibernate.HibernateORMEngine")))
//					cd = null;
//			}
//
//			if (cd == null || !cd.hasClass()) {
//				if (configServer != null) cd = configServer.getORMEngineClass();
//				else cd = cdDefault;
//			}
//
//			// load class (removed because this unnecessary loads the orm engine)
//			/*
//			 * try { cd.getClazz(); // TODO check interface as well } catch (Exception e) { log.error("ORM", e);
//			 * cd=cdDefault; }
//			 */
//
//			config.setORMEngineClass(cd);
//
//			// config
//			if (orm == null) orm = doc.createElement("orm"); // this is just a dummy
//			ORMConfiguration def = hasCS ? configServer.getORMConfig() : null;
//			ORMConfiguration ormConfig = ORMConfigurationImpl.load(config, null, orm, config.getRootDirectory(), def);
//			config.setORMConfig(ormConfig);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 * @throws PageException
	 * @throws IOException
	 */
	private static void loadScope(ConfigServerImpl configServer, ConfigImpl config, Document doc, int mode, Log log) {
		try {
			boolean hasCS = configServer != null;
			config.setLocalMode(Undefined.MODE_LOCAL_OR_ARGUMENTS_ALWAYS);
			config.setCGIScopeReadonly(true);
			config.setSessionType(AppListenerUtil.toSessionType(StaticConfig.sessionType, hasCS ? configServer.getSessionType() : Config.SESSION_TYPE_APPLICATION));
			config.setScopeCascadingType(Config.SCOPE_STRICT);
			config.setAllowImplicidQueryCall(false);
			config.setMergeFormAndURL(true);
			config.setSessionStorage(StaticConfig.sessionStorage);
			config.setSessionTimeout(StaticConfig.sessionTimeout);
			config.setApplicationTimeout(StaticConfig.scopeApplicationTimeout);
			config.setSessionManagement(StaticConfig.sessionManagement);
			config.setDomainCookies(StaticConfig.setDomainCookies);
		} catch (PageException e) {
			log(config, log, e);
		}
	}

	private static void loadJava(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		config.setInspectTemplate(StaticConfig.inspectTemplates);
		config.setCompileType(StaticConfig.startupCompilation);
	}

	private static void loadConstants(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		boolean hasCS = configServer != null;
		Struct sct = null;
		if (hasCS) {
			sct = configServer.getConstants();
			if (sct != null) sct = (Struct) sct.duplicate(false);
		}
		if (sct == null) sct = new StructImpl();
		config.setConstants(sct);
	}

	public static void log(Config config, Log log, Exception e) {
		try {
			if (log != null) log.error("configuration", e);
			else {
				PrintWriter pw = null;
				if (config != null) {
					pw = config.getErrWriter();
				}
				if (pw == null) pw = new PrintWriter(System.err);
				SystemOut.printDate(config.getErrWriter(), ExceptionUtil.getStacktrace(e, true));
			}
		} catch (Exception ee) {
			ee.printStackTrace();
		}
	}

//	private static void loadLogin(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			// server context
//			if (config instanceof ConfigServer) {
//				Element login = getChildByName(doc.getDocumentElement(), "login");
//				boolean captcha = Caster.toBooleanValue(getAttr(login, "captcha"), false);
//				boolean rememberme = Caster.toBooleanValue(getAttr(login, "rememberme"), true);
//
//				int delay = Caster.toIntValue(getAttr(login, "delay"), 1);
//				ConfigServerImpl cs = (ConfigServerImpl) config;
//				cs.setLoginDelay(delay);
//				cs.setLoginCaptcha(captcha);
//				cs.setRememberMe(rememberme);
//			}
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 * @throws IOException
	 */
	private static void loadMail(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		boolean hasCS = configServer != null;
		config.setMailSendPartial(false);
		config.setMailSpoolInterval(StaticConfig.mailSpoolInterval);
		config.setMailDefaultEncoding(StaticConfig.mailDefaultCharset);
		config.setMailSpoolEnable(StaticConfig.mailSpoolEnable);
		config.setMailTimeout(StaticConfig.mailSpoolTimeout);
		List<Server> servers = new ArrayList<Server>();
		servers.add(0,
				new ServerImpl(1, StaticConfig.mailSpoolServerHost, StaticConfig.mailSpoolServerPort, StaticConfig.mailSpoolServerUsername,
						StaticConfig.mailSpoolServerPassword, StaticConfig.mailSpoolServerLife, StaticConfig.mailSpoolServerIdle,
						StaticConfig.mailSpoolServerTLS, StaticConfig.mailSpoolServerSSL, StaticConfig.mailSpoolServerReuseConnection,
						hasCS ? ServerImpl.TYPE_LOCAL : ServerImpl.TYPE_GLOBAL));
		config.setMailServers(servers.toArray(new Server[servers.size()]));
	}

//	private static void loadMonitors(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			// only load in server context
//			if (configServer != null) return;
//
//			configServer = (ConfigServerImpl) config;
//
//			Element parent = getChildByName(doc.getDocumentElement(), "monitoring");
//			Boolean enabled = Caster.toBoolean(getAttr(parent, "enabled"), null);
//			if (enabled != null) configServer.setMonitoringEnabled(enabled.booleanValue());
//			// SystemOut.printDate(config.getOutWriter(), "monitoring is " + (enabled ? "enabled" :
//			// "disabled"));
//
//			Element[] children = getChildren(parent, "monitor");
//
//			java.util.List<IntervallMonitor> intervalls = new ArrayList<IntervallMonitor>();
//			java.util.List<RequestMonitor> requests = new ArrayList<RequestMonitor>();
//			java.util.List<MonitorTemp> actions = new ArrayList<MonitorTemp>();
//			String strType, name;
//			ClassDefinition cd;
//			boolean _log, async;
//			short type;
//			for (int i = 0; i < children.length; i++) {
//				Element el = children[i];
//				cd = getClassDefinition(el, "", config.getIdentification());
//				strType = getAttr(el, "type");
//				name = getAttr(el, "name");
//				async = Caster.toBooleanValue(getAttr(el, "async"), false);
//				_log = Caster.toBooleanValue(getAttr(el, "log"), true);
//
//				if ("request".equalsIgnoreCase(strType)) type = IntervallMonitor.TYPE_REQUEST;
//				else if ("action".equalsIgnoreCase(strType)) type = Monitor.TYPE_ACTION;
//				else type = IntervallMonitor.TYPE_INTERVAL;
//
//				if (cd.hasClass() && !StringUtil.isEmpty(name)) {
//					name = name.trim();
//					try {
//						Class clazz = cd.getClazz();
//						Object obj;
//						ConstructorInstance constr = Reflector.getConstructorInstance(clazz, new Object[]{configServer}, null);
//						if (constr != null) obj = constr.invoke();
//						else obj = clazz.newInstance();
//						SystemOut.printDate(config.getOutWriter(), "loaded " + (strType) + " monitor [" + clazz.getName() + "]");
//						if (type == IntervallMonitor.TYPE_INTERVAL) {
//							IntervallMonitor m = obj instanceof IntervallMonitor ? (IntervallMonitor) obj : new IntervallMonitorWrap(obj);
//							m.init(configServer, name, _log);
//							intervalls.add(m);
//						} else if (type == Monitor.TYPE_ACTION) {
//							ActionMonitor am = obj instanceof ActionMonitor ? (ActionMonitor) obj : new ActionMonitorWrap(obj);
//							actions.add(new MonitorTemp(am, name, _log));
//						} else {
//							RequestMonitorPro m = new RequestMonitorProImpl(obj instanceof RequestMonitor ? (RequestMonitor) obj : new RequestMonitorWrap(obj));
//							if (async) m = new AsyncRequestMonitor(m);
//							m.init(configServer, name, _log);
//							SystemOut.printDate(config.getOutWriter(), "initialize " + (strType) + " monitor [" + clazz.getName() + "]");
//
//							requests.add(m);
//						}
//					} catch (Throwable t) {
//						ExceptionUtil.rethrowIfNecessary(t);
//						SystemOut.printDate(config.getErrWriter(), ExceptionUtil.getStacktrace(t, true));
//					}
//				}
//
//			}
//			configServer.setRequestMonitors(requests.toArray(new RequestMonitor[requests.size()]));
//			configServer.setIntervallMonitors(intervalls.toArray(new IntervallMonitor[intervalls.size()]));
//			ActionMonitorCollector actionMonitorCollector = ActionMonitorFatory.getActionMonitorCollector(configServer, actions.toArray(new MonitorTemp[actions.size()]));
//			configServer.setActionMonitorCollector(actionMonitorCollector);
//
//			((CFMLEngineImpl) configServer.getCFMLEngine()).touchMonitor(configServer);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

//	/**
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 * @throws PageException
//	 */
//	private static void loadSearch(ConfigServer configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			Element search = getChildByName(doc.getDocumentElement(), "search");
//
//			// class
//			ClassDefinition<SearchEngine> cd = getClassDefinition(search, "engine-", config.getIdentification());
//			if (!cd.hasClass() || "lucee.runtime.search.lucene.LuceneSearchEngine".equals(cd.getClassName())) {
//				if (configServer != null) cd = ((ConfigImpl) configServer).getSearchEngineClassDefinition();
//				else cd = new ClassDefinitionImpl(DummySearchEngine.class);
//			}
//
//			// directory
//			String dir = search.getAttribute("directory");
//			if (StringUtil.isEmpty(dir)) {
//				if (configServer != null) dir = ((ConfigImpl) configServer).getSearchEngineDirectory();
//				else dir = "{lucee-web}/search/";
//			}
//
//			config.setSearchEngine(cd, dir);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}
//
//	/**
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 * @param log
//	 * @throws IOException
//	 * @throws PageException
//	 */
//	private static void loadScheduler(ConfigServer configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			if (config instanceof ConfigServer) return;
//
//			Resource configDir = config.getConfigDir();
//			Element scheduler = getChildByName(doc.getDocumentElement(), "scheduler");
//
//			// set scheduler
//			Resource file = ConfigWebUtil.getFile(config.getRootDirectory(), scheduler.getAttribute("directory"), "scheduler", configDir, FileUtil.TYPE_DIR, config);
//			config.setScheduler(configServer.getCFMLEngine(), file);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 */
	private static void loadDebug(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//			Map<String, DebugEntry> list = new HashMap<String, DebugEntry>();
//			config.setDebugEntries(list.values().toArray(new DebugEntry[list.size()]));

		config.setDebug(StaticConfig.debugEnabled);
		config.setDebugLogOutput(ConfigImpl.CLIENT_BOOLEAN_FALSE);

		// debug options
		int options = 0;
		if(StaticConfig.debugDatabase==1) options += ConfigImpl.DEBUG_DATABASE;
		if (StaticConfig.debugException==1) options += ConfigImpl.DEBUG_EXCEPTION;
		if (StaticConfig.debugDump==1) options += ConfigImpl.DEBUG_DUMP;
		if (StaticConfig.debugTracing==1) options += ConfigImpl.DEBUG_TRACING;
		if (StaticConfig.debugTimer==1) options += ConfigImpl.DEBUG_TIMER;
		if (StaticConfig.debugQueryUsage==1) options += ConfigImpl.DEBUG_QUERY_USAGE;
		config.setDebugMaxRecordsLogged(StaticConfig.debugMaxRecords);
		config.setDebugOptions(options);
	}

//	/**
//	 * @param configServer
//	 * @param config
//	 * @param doc
//	 */
//	private static void loadCFX(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManager.TYPE_CFX_SETTING);
//
//			Map<String, CFXTagClass> map = MapFactory.<String, CFXTagClass>getConcurrentMap();
//			if (configServer != null) {
//				try {
//					if (configServer.getCFXTagPool() != null) {
//						Map<String, CFXTagClass> classes = configServer.getCFXTagPool().getClasses();
//						Iterator<Entry<String, CFXTagClass>> it = classes.entrySet().iterator();
//						Entry<String, CFXTagClass> e;
//						while (it.hasNext()) {
//							e = it.next();
//							map.put(e.getKey(), e.getValue().cloneReadOnly());
//						}
//					}
//				} catch (SecurityException e) {
//				}
//			}
//
//			if (hasAccess) {
//				if (configServer == null) {
//					System.setProperty("cfx.bin.path", config.getConfigDir().getRealResource("bin").getAbsolutePath());
//				}
//
//				// Java CFX Tags
//				Element cfxTagsParent = getChildByName(doc.getDocumentElement(), "ext-tags", false, true);
//				if (cfxTagsParent == null)
//					cfxTagsParent = getChildByName(doc.getDocumentElement(), "cfx-tags", false, true);
//				if (cfxTagsParent == null) cfxTagsParent = getChildByName(doc.getDocumentElement(), "ext-tags");
//
//				boolean oldStyle = cfxTagsParent.getNodeName().equals("cfx-tags");
//
//				Element[] cfxTags = oldStyle ? getChildren(cfxTagsParent, "cfx-tag") : getChildren(cfxTagsParent, "ext-tag");
//				for (int i = 0; i < cfxTags.length; i++) {
//					String type = getAttr(cfxTags[i], "type");
//					if (type != null) {
//						// Java CFX Tags
//						if (type.equalsIgnoreCase("java")) {
//							String name = getAttr(cfxTags[i], "name");
//							ClassDefinition cd = getClassDefinition(cfxTags[i], "", config.getIdentification());
//							if (!StringUtil.isEmpty(name) && cd.hasClass()) {
//								map.put(name.toLowerCase(), new JavaCFXTagClass(name, cd));
//							}
//						}
//					}
//				}
//
//			}
//			config.setCFXTagPool(map);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	/**
	 * loads the bundles defined in the extensions
	 *
	 * @param cs
	 * @param config
	 * @param doc
	 * @param log
	 */
	private static void loadExtensionBundles(ConfigServerImpl cs, ConfigImpl config, Document doc, Log log) {
		try {
//			Element parent = getChildByName(doc.getDocumentElement(), "extensions");
//			Element[] children = getChildren(parent, "rhextension");
			String strBundles;
			List<RHExtension> extensions = new ArrayList<RHExtension>();

			RHExtension rhe;
			for (int i=0;i<StaticConfig.extensionName.length;i++) {
				BundleInfo[] bfsq;
				try {
					rhe = new RHExtension(config, i);

					if (rhe.getStartBundles()) rhe.deployBundles(config);
					extensions.add(rhe);
				} catch (Exception e) {
					log.error("load-extension", e);
					continue;
				}
			}
			config.setExtensions(extensions.toArray(new RHExtension[extensions.size()]));
		} catch (Exception e) {
			log(config, log, e);
		}
	}

//	private static void loadExtensions(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			Element xmlExtParent = getChildByName(doc.getDocumentElement(), "extensions");
//
//			String strEnabled = getAttr(xmlExtParent, "enabled");
//			if (!StringUtil.isEmpty(strEnabled)) {
//				config.setExtensionEnabled(Caster.toBooleanValue(strEnabled, false));
//			}
//
//			// RH Providers
//			{
//				// providers
//				Element[] xmlProviders = getChildren(xmlExtParent, "rhprovider");
//				String strProvider;
//				Map<RHExtensionProvider, String> providers = new LinkedHashMap<RHExtensionProvider, String>();
//
//				for (int i = 0; i < Constants.RH_EXTENSION_PROVIDERS.length; i++) {
//					providers.put(Constants.RH_EXTENSION_PROVIDERS[i], "");
//				}
//				for (int i = 0; i < xmlProviders.length; i++) {
//					strProvider = getAttr(xmlProviders[i], "url");
//					if (!StringUtil.isEmpty(strProvider, true)) {
//						try {
//							providers.put(new RHExtensionProvider(strProvider.trim(), false), "");
//						} catch (MalformedURLException e) {
//							SystemOut.printDate(e);
//						}
//					}
//				}
//				config.setRHExtensionProviders(providers.keySet().toArray(new RHExtensionProvider[providers.size()]));
//			}
//
//			// classic providers
//			{
//				Element[] xmlProviders = getChildren(xmlExtParent, "provider");
//				String provider;
//				Map list = new HashMap();
//
//				for (int i = 0; i < Constants.CLASSIC_EXTENSION_PROVIDERS.length; i++) {
//					list.put(Constants.CLASSIC_EXTENSION_PROVIDERS[i], "");
//				}
//
//				for (int i = 0; i < xmlProviders.length; i++) {
//					provider = getAttr(xmlProviders[i], "url");
//					if (!StringUtil.isEmpty(provider, true)) {
//						list.put(new ExtensionProviderImpl(provider.trim(), false), "");
//					}
//				}
//				config.setExtensionProviders((ExtensionProvider[]) list.keySet().toArray(new ExtensionProvider[list.size()]));
//			}
//
//			// extensions
//			Element[] xmlExtensions = getChildren(xmlExtParent, "extension");
//			Extension[] extensions = new Extension[xmlExtensions.length];
//			Element xmlExtension;
//			for (int i = 0; i < xmlExtensions.length; i++) {
//				xmlExtension = xmlExtensions[i];
//				extensions[i] = new ExtensionImpl(getAttr(xmlExtension, "config"), getAttr(xmlExtension, "id"), getAttr(xmlExtension, "provider"), getAttr(xmlExtension, "version"),
//						getAttr(xmlExtension, "name"), getAttr(xmlExtension, "label"), getAttr(xmlExtension, "description"), getAttr(xmlExtension, "category"),
//						getAttr(xmlExtension, "image"), getAttr(xmlExtension, "author"), getAttr(xmlExtension, "codename"), getAttr(xmlExtension, "video"),
//						getAttr(xmlExtension, "support"), getAttr(xmlExtension, "documentation"), getAttr(xmlExtension, "forum"), getAttr(xmlExtension, "mailinglist"),
//						getAttr(xmlExtension, "network"), DateCaster.toDateAdvanced(getAttr(xmlExtension, "created"), null, null), getAttr(xmlExtension, "type"));
//			}
//			config.setExtensions(extensions);
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 * @throws IOException
	 */
	private static void loadComponent(ConfigServer configServer, ConfigImpl config, Document doc, int mode, Log log) {
//		try {
//			Element component = getChildByName(doc.getDocumentElement(), "component");
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManager.TYPE_SETTING);
			boolean hasSet = false;
			boolean hasCS = configServer != null;

			// String virtual="/component/";

//			if (component != null && hasAccess) {

			// component-default-import
			String strCDI = "";//getAttr(component, "component-default-import");
			if (StringUtil.isEmpty(strCDI, true) && configServer != null) {
				strCDI = ((ConfigServerImpl) configServer).getComponentDefaultImport().toString();
			}
			if (!StringUtil.isEmpty(strCDI, true)) config.setComponentDefaultImport(strCDI);

			// Base CFML
			String strBase = StaticConfig.componentBaseCFML; //getAttr(component, "base-cfml");
			if (StringUtil.isEmpty(strBase, true) && configServer != null) {
				strBase = configServer.getBaseComponentTemplate(CFMLEngine.DIALECT_CFML);
			}
			config.setBaseComponentTemplate(CFMLEngine.DIALECT_CFML, strBase);

			String strDeepSearch = null;
			if (!StringUtil.isEmpty(strDeepSearch)) {
				config.setDoComponentDeepSearch(Caster.toBooleanValue(strDeepSearch.trim(), false));
			} else if (hasCS) {
				config.setDoComponentDeepSearch(((ConfigServerImpl) configServer).doComponentDeepSearch());
			}
			config.setComponentDumpTemplate(StaticConfig.componentDumpTemplate);

			// data-member-default-access
			if (mode == ConfigImpl.MODE_STRICT) {
				config.setComponentDataMemberDefaultAccess(Component.ACCESS_PRIVATE);
			} else {
				String strDmda = StaticConfig.componentMemberDefaultAccess;//getAttr(component, "data-member-default-access");
				if (strDmda != null && strDmda.trim().length() > 0) {
					strDmda = strDmda.toLowerCase().trim();
					if (strDmda.equals("remote"))
						config.setComponentDataMemberDefaultAccess(Component.ACCESS_REMOTE);
					else if (strDmda.equals("public"))
						config.setComponentDataMemberDefaultAccess(Component.ACCESS_PUBLIC);
					else if (strDmda.equals("package"))
						config.setComponentDataMemberDefaultAccess(Component.ACCESS_PACKAGE);
					else if (strDmda.equals("private"))
						config.setComponentDataMemberDefaultAccess(Component.ACCESS_PRIVATE);
				} else if (configServer != null) {
					config.setComponentDataMemberDefaultAccess(configServer.getComponentDataMemberDefaultAccess());
				}
			}

			Boolean tp = null;//Caster.toBoolean(getAttr(component, "trigger-data-member"), null);
			if (tp != null) config.setTriggerComponentDataMember(tp.booleanValue());
			else if (configServer != null) {
				config.setTriggerComponentDataMember(configServer.getTriggerComponentDataMember());
			}

			Boolean ls = null;
			if (ls != null) config.setComponentLocalSearch(ls.booleanValue());
			else if (configServer != null) {
				config.setComponentLocalSearch(((ConfigServerImpl) configServer).getComponentLocalSearch());
			}

			// use cache path
			Boolean ucp = null;
			if (ucp != null) config.setUseComponentPathCache(ucp.booleanValue());
			else if (configServer != null) {
				config.setUseComponentPathCache(((ConfigServerImpl) configServer).useComponentPathCache());
			}

			Boolean ucs = null;
			if (ucs != null) config.setUseComponentShadow(ucs.booleanValue());
			else if (configServer != null) {
				config.setUseComponentShadow(configServer.useComponentShadow());
			}

//			if (mode == ConfigImpl.MODE_STRICT) {
//				config.setDoComponentDeepSearch(false);
//				config.setComponentDataMemberDefaultAccess(Component.ACCESS_PRIVATE);
//				config.setTriggerComponentDataMember(true);
//				config.setComponentLocalSearch(false);
//				config.setUseComponentShadow(false);
//
//			}

			// Web Mapping

//			Element[] cMappings = getChildren(component, "mapping");
//			hasSet = false;
//			Mapping[] mappings = null;
//			if (hasAccess && cMappings.length > 0) {
//				mappings = new Mapping[cMappings.length];
//				for (int i = 0; i < cMappings.length; i++) {
//					Element cMapping = cMappings[i];
//					String physical = cMapping.getAttribute("physical");
//					String archive = cMapping.getAttribute("archive");
//					boolean readonly = toBoolean(getAttr(cMapping, "readonly"), false);
//					boolean hidden = toBoolean(getAttr(cMapping, "hidden"), false);
//
//					int listMode = ConfigWebUtil.toListenerMode(getAttr(cMapping, "listener-mode"), -1);
//					int listType = ConfigWebUtil.toListenerType(getAttr(cMapping, "listener-type"), -1);
//					short inspTemp = inspectTemplate(cMapping);
//					String virtual = XMLConfigAdmin.createVirtual(cMapping);
//
//					String primary = getAttr(cMapping, "primary");
//
//					boolean physicalFirst = archive == null || !primary.equalsIgnoreCase("archive");
//					hasSet = true;
//					mappings[i] = new MappingImpl(config, virtual, physical, archive, inspTemp, physicalFirst, hidden, readonly, true, false, true, null, listMode, listType);
//				}
//
//				config.setComponentMappings(mappings);
//
//			}
//
//			 Server Mapping
//			if (hasCS) {
//				Mapping[] originals = ((ConfigServerImpl) configServer).getComponentMappings();
//				Mapping[] clones = new Mapping[originals.length];
//				LinkedHashMap map = new LinkedHashMap();
//				Mapping m;
//				for (int i = 0; i < clones.length; i++) {
//					m = ((MappingImpl) originals[i]).cloneReadOnly(config);
//					map.put(toKey(m), m);
//					// clones[i]=((MappingImpl)m[i]).cloneReadOnly(config);
//				}
//
//				if (mappings != null) {
//					for (int i = 0; i < mappings.length; i++) {
//						m = mappings[i];
//						map.put(toKey(m), m);
//					}
//				}
//				if (originals.length > 0) {
//					clones = new Mapping[map.size()];
//					Iterator it = map.entrySet().iterator();
//					Map.Entry entry;
//					int index = 0;
//					while (it.hasNext()) {
//						entry = (Entry) it.next();
//						clones[index++] = (Mapping) entry.getValue();
//						// print.out("c:"+clones[index-1]);
//					}
//					hasSet = true;
//					// print.err("set:"+clones.length);
//
//					config.setComponentMappings(clones);
//				}
//			}
//
//			if (!hasSet) {
//				MappingImpl m = new MappingImpl(config, "/default", "{lucee-web}/components/", null, ConfigImpl.INSPECT_UNDEFINED, true, false, false, true, false, true, null, -1,
//						-1);
//				config.setComponentMappings(new Mapping[]{m.cloneReadOnly(config)});
//			}

//		} catch (Exception e) {
//			log(config, log, e);
//		}
	}

//	private static void loadProxy(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
//		try {
//			boolean hasCS = configServer != null;
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManager.TYPE_SETTING);
//			Element proxy = getChildByName(doc.getDocumentElement(), "proxy");
//
//			// proxy server
//			String server = getAttr(proxy, "server");
//			String username = getAttr(proxy, "username");
//			String password = getAttr(proxy, "password");
//			int port = Caster.toIntValue(getAttr(proxy, "port"), -1);
//
//			if (hasAccess && !StringUtil.isEmpty(server)) {
//				config.setProxyData(ProxyDataImpl.getInstance(server, port, username, password));
//			} else if (hasCS) config.setProxyData(configServer.getProxyData());
//		} catch (Exception e) {
//			log(config, log, e);
//		}
//	}

	private static void loadError(ConfigServerImpl configServer, ConfigImpl config, Document doc, Log log) {
		try {
//			Element error = getChildByName(doc.getDocumentElement(), "error");
			boolean hasCS = configServer != null;
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManager.TYPE_DEBUGGING);

			// error template
			String template = null;//error.getAttribute("template");

			// 500
			String template500 = null;//error.getAttribute("template-500");
			if (StringUtil.isEmpty(template500)) template500 = null;//error.getAttribute("template500");
			if (StringUtil.isEmpty(template500)) template500 = null;//error.getAttribute("500");
			if (StringUtil.isEmpty(template500)) template500 = template;
			if (!StringUtil.isEmpty(template500)) {
				config.setErrorTemplate(500, template500);
			} else if (hasCS) config.setErrorTemplate(500, configServer.getErrorTemplate(500));
			else config.setErrorTemplate(500, "/lucee/templates/error/error." + TEMPLATE_EXTENSION);

			// 404
			String template404 = null;//error.getAttribute("template-404");
			if (StringUtil.isEmpty(template404)) template404 = null;//error.getAttribute("template404");
			if (StringUtil.isEmpty(template404)) template404 = null;//error.getAttribute("404");
			if (StringUtil.isEmpty(template404)) template404 = template;
			if (!StringUtil.isEmpty(template404)) {
				config.setErrorTemplate(404, template404);
			} else if (hasCS) config.setErrorTemplate(404, configServer.getErrorTemplate(404));
			else config.setErrorTemplate(404, "/lucee/templates/error/error." + TEMPLATE_EXTENSION);

			// status code
			Boolean bStausCode =null;// Caster.toBoolean(SystemUtil.getSystemPropOrEnvVar("lucee.status.code", null), null);
//			if (bStausCode == null) bStausCode = Caster.toBoolean(getAttr(error, "status-code"), null);
//			if (bStausCode == null) bStausCode = Caster.toBoolean(getAttr(error, "statusCode"), null);
//			if (bStausCode == null) bStausCode = Caster.toBoolean(getAttr(error, "status"), null);

			if (bStausCode != null) {
				config.setErrorStatusCode(bStausCode.booleanValue());
			} else if (hasCS) config.setErrorStatusCode(configServer.getErrorStatusCode());
		} catch (Exception e) {
			log(config, log, e);
		}

	}

	private static void loadCompiler(ConfigServerImpl configServer, ConfigImpl config, Document doc, int mode, Log log) {
		boolean hasCS = configServer != null;

		config.setSuppressWSBeforeArg(StaticConfig.compilerSuppressWhiteSpaceBeforeArg);
		config.setDotNotationUpperCase(StaticConfig.compilerDotNotationUpperCase);
		config.setFullNullSupport(StaticConfig.compilerFullNullSupport);
		config.setDefaultFunctionOutput(true);

		// suppress WS between cffunction and cfargument
		String str = StaticConfig.compilerExternalizeStringGTE;//getAttr(compiler, "externalize-string-gte");
		if (Decision.isNumber(str)) {
			config.setExternalizeStringGTE(Caster.toIntValue(str, -1));
		} else if (hasCS) {
			config.setExternalizeStringGTE(configServer.getExternalizeStringGTE());
		}

		config.setAllowLuceeDialect(false);
		config.setHandleUnQuotedAttrValueAsString(StaticConfig.compilerHandleUnquotedAttributeValueAsString);
	}

	/**
	 * @param configServer
	 * @param config
	 * @param doc
	 * @throws IOException
	 * @throws PageException
	 */
	private static void loadApplication(ConfigServerImpl configServer, ConfigImpl config, Document doc, int mode, Log log) {
//			boolean hasAccess = ConfigWebUtil.hasAccess(config, SecurityManager.TYPE_SETTING);


//			StaticConfig.applicationAllowURLRequestTimeout=false;
//			StaticConfig.applicationCacheDirectory="{lucee-web}/cache/";
//			StaticConfig.applicationCacheDirectoryMaxSize="100mb";
//			StaticConfig.applicationListenerMode="root";
//			StaticConfig.applicationListenerType="modern";
//			StaticConfig.applicationRequestTimeout="365,0,0,0";
//			StaticConfig.applicationScriptProtect="none";
//			StaticConfig.applicationTypeChecking=true;
			// Listener type
			ApplicationListener listener= new ModernAppListener();

//			String[] strTypes = new String[]{"function", "include", "query", "resource", "http", "file", "webservice"};
//			int[] types = new int[]{Config.CACHEDWITHIN_FUNCTION, Config.CACHEDWITHIN_INCLUDE, Config.CACHEDWITHIN_QUERY, Config.CACHEDWITHIN_RESOURCE, Config.CACHEDWITHIN_HTTP,
//					Config.CACHEDWITHIN_FILE, Config.CACHEDWITHIN_WEBSERVICE};

			// cachedwithin
//			for (int i = 0; i < types.length; i++) {
//				String cw = getAttr(application, "cached-within-" + strTypes[i]);
//				if (!StringUtil.isEmpty(cw, true)) config.setCachedWithin(types[i], cw);
//				else if (hasCS) config.setCachedWithin(types[i], configServer.getCachedWithin(types[i]));
//			}

			config.setTypeChecking(StaticConfig.applicationTypeChecking);
			listener.setMode(StaticConfig.applicationListenerMode);
			config.setApplicationListener(listener);
			config.setAllowURLRequestTimeout(false);
			// Req Timeout
		try {
			config.setRequestTimeout(Caster.toTimespan(StaticConfig.applicationRequestTimeout));
		} catch (PageException e) {
			throw new RuntimeException(e);
		}
		config.setScriptProtect(AppListenerUtil.translateScriptProtect(StaticConfig.applicationScriptProtect));

			DateCaster.classicStyle = true;

			// Cache
			Resource configDir = config.getConfigDir();
			String strCacheDirectory = ConfigWebUtil.translateOldPath(StaticConfig.applicationCacheDirectory);
			Resource res = ConfigWebUtil.getFile(configDir, strCacheDirectory, "cache", configDir, FileUtil.TYPE_DIR, config);
			config.setCacheDir(res);

			config.setCacheDirSize(ByteSizeParser.parseByteSizeDefinition(StaticConfig.applicationCacheDirectoryMaxSize, config.getCacheDirSize()));

			// admin sync
//			ClassDefinition asc = getClassDefinition(application, "admin-sync-", config.getIdentification());
//			if (!asc.hasClass())
//				asc = getClassDefinition(application, "admin-synchronisation-", config.getIdentification());
//
//			if (hasAccess && asc.hasClass()) {
//				try {
//					Class clazz = asc.getClazz();
//					if (!Reflector.isInstaneOf(clazz, AdminSync.class, false))
//						throw new ApplicationException("class [" + clazz.getName() + "] does not implement interface [" + AdminSync.class.getName() + "]");
//					config.setAdminSyncClass(clazz);
//
//				} catch (Exception e) {
//					SystemOut.printDate(e);
//				}
//			} else if (hasCS) config.setAdminSyncClass(configServer.getAdminSyncClass());
	}

//	/**
//	 * cast a string value to a boolean
//	 *
//	 * @param value String value represent a booolean ("yes", "no","true" aso.)
//	 * @param defaultValue if can't cast to a boolean is value will be returned
//	 * @return boolean value
//	 */
//	private static boolean toBoolean(String value, boolean defaultValue) {
//
//		if (value == null || value.trim().length() == 0) return defaultValue;
//
//		try {
//			return Caster.toBooleanValue(value.trim());
//		} catch (PageException e) {
//			return defaultValue;
//		}
//	}

	public static long toLong(String value, long defaultValue) {

		if (value == null || value.trim().length() == 0) return defaultValue;
		long longValue = Caster.toLongValue(value.trim(), Long.MIN_VALUE);
		if (longValue == Long.MIN_VALUE) return defaultValue;
		return longValue;
	}

//    /**
//     * reads an attribute from a xml Element and parses placeholders
//     *
//     * @param el
//     * @param name
//     * @return
//     */
//    public static String getAttr(Element el, String name) {
//	String v = el.getAttribute(name);
//	return replaceConfigPlaceHolder(v);
//    }

//	public static String replaceConfigPlaceHolder(String v) {
//		if (StringUtil.isEmpty(v) || v.indexOf('{') == -1) return v;
//
//		int s = -1, e = -1;
//		int prefixLen, start = -1, end;
//		String _name, _prop;
//		while ((s = v.indexOf("{system:", start)) != -1 | /* don't change */ (e = v.indexOf("{env:", start)) != -1) {
//			boolean isSystem = false;
//			// system
//			if (s != -1 && (e == -1 || e > s)) {
//				start = s;
//				prefixLen = 8;
//				isSystem = true;
//			}
//			// env
//			else {
//				start = e;
//				prefixLen = 5;
//
//			}
//
//			end = v.indexOf('}', start);
//			/*
//			 * print.e("----------------"); print.e(s+"-"+e); print.e(v); print.e(start); print.e(end);
//			 */
//			if (end > prefixLen) {
//				_name = v.substring(start + prefixLen, end);
//				// print.e(_name);
//				_prop = isSystem ? System.getProperty(_name) : System.getenv(_name);
//				if (_prop != null) {
//					v = new StringBuilder().append(v.substring(0, start)).append(_prop).append(v.substring(end + 1)).toString();
//					start += _prop.length();
//				} else start = end;
//			} else start = end; // set start to end for the next round
//			s = -1;
//			e = -1; // reset index
//		}
//		return v;
//	}

	public static class MonitorTemp {

		public final ActionMonitor am;
		public final String name;
		public final boolean log;

		public MonitorTemp(ActionMonitor am, String name, boolean log) {
			this.am = am;
			this.name = name;
			this.log = log;
		}

	}
}