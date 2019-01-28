package lucee.runtime.config;

import lucee.commons.io.log.Log;
import lucee.runtime.listener.ApplicationListener;

import java.util.Locale;
import java.util.TimeZone;

import static lucee.runtime.config.Config.INSPECT_ONCE;
import static lucee.runtime.config.Config.RECOMPILE_NEVER;
import static lucee.runtime.security.SecurityManager.VALUE_NO;

public class StaticConfig {
	public static class StaticResourceProvider{
		public String clazz;
		public String scheme;
		public String arguments;
		public StaticResourceProvider(String clazz, String scheme, String arguments){
			this.clazz=clazz;
			this.scheme=scheme;
			this.arguments=arguments;
		}
	}
	public static class StaticLog{
		public String name;
		public String arguments;
		public int level;
		public StaticLog(String name, String arguments, int level){
			this.name=name;
			this.arguments=arguments;
			this.level=level;
		}
	}
	// <system err="default" out="null"/>
	public static String systemError="default";
	public static String systemOut="null";
	// <mode develop="false"/>
	public static boolean modeDevelop=false;

	public static boolean queueEnable=false;
	public static int queueMax=100;
	public static long queueTimeout=0L;

	// <setting allow-compression="false" buffering-output="false" cfml-writer="white-space-pref" content-length="" suppress-content="false"/>
	public static boolean settingAllowCompression=false;
	public static boolean settingBufferingOutput=false;
	public static boolean settingSuppressContent=false;
	public static String settingCFMLWriter="white-space-pref";
	public static boolean settingContentLength=false;


	// <file-system fld-default-directory="{lucee-config}/library/fld/" function-default-directory="{lucee-config}/library/function/" tag-default-directory="{lucee-config}/library/tag/" temp-directory="{lucee-config}/temp/" tld-default-directory="{lucee-config}/library/tld/">
	public static String fileSystemFLDDirectory="{lucee-config}/library/fld/";
	public static String fileSystemTLDDirectory="{lucee-config}/library/tld/";
	public static String fileSystemDeployDirectory="{lucee-config}/cfclasses/";
	public static String fileSystemFunctionDirectory="{lucee-config}/library/function/";
	public static String fileSystemTempDirectory="/temp/";
	public static String fileSystemTagDirectory="{lucee-config}/library/tag/";

	public static String fileSystemWebFLDDirectory="{lucee-web}/library/fld/";
	public static String fileSystemWebTLDDirectory="{lucee-web}/library/tld/";
	public static String fileSystemWebDeployDirectory="{lucee-web}/cfclasses/";
	public static String fileSystemWebFunctionDirectory="{lucee-web}/library/function/";
	public static String fileSystemWebTempDirectory="/temp/";
	public static String fileSystemWebTagDirectory="{lucee-web}/library/tag/";
	// <resources>
	public static String defaultResourceProviderClass="lucee.commons.io.res.type.file.FileResourceProvider";
	public static String defaultResourceProviderArguments="lock-timeout:1000;";
	public static StaticResourceProvider[] resourceProviders=new StaticResourceProvider[]{
			new StaticResourceProvider("lucee.commons.io.res.type.ftp.FTPResourceProvider", "ftp", "lock-timeout:20000;socket-timeout:-1;client-timeout:60000"),
			new StaticResourceProvider("lucee.commons.io.res.type.zip.ZipResourceProvider", "zip", "lock-timeout:1000;case-sensitive:true"),
			new StaticResourceProvider("lucee.commons.io.res.type.tar.TarResourceProvider", "tar", "lock-timeout:1000;case-sensitive:true;" ),
			new StaticResourceProvider("lucee.commons.io.res.type.tgz.TGZResourceProvider", "tgz", "lock-timeout:1000;case-sensitive:true;"),
			new StaticResourceProvider("lucee.commons.io.res.type.http.HTTPResourceProvider", "http", "lock-timeout:10000;case-sensitive:false;"),
			new StaticResourceProvider("lucee.commons.io.res.type.http.HTTPSResourceProvider", "https", "lock-timeout:10000;case-sensitive:false;")
	};
	// <scope applicationtimeout="1,0,0,0" cascade-to-resultset="yes" cascading="strict" client-directory-max-size="10mb" client-max-age="90" clientmanagement="no" merge-url-form="no" requesttimeout="0,0,0,50" sessionmanagement="yes" sessiontimeout="0,0,30,0" setclientcookies="yes" setdomaincookies="no"/>
	public static String scopeApplicationTimeout="365,0,0,0";
	public static String requestTimeout="0,0,0,25";
	public static String sessionStorage="memory";
	public static String sessionType="j2ee";
	public static boolean sessionManagement=false;
	public static String sessionTimeout="0,0,30,0";
	public static boolean setDomainCookies=false;
	// <mail spool-enable="yes" spool-interval="5" timeout="30">
	//	<server idle="10000" life="60000" password="" port="25" reuse-connection="true" smtp="localhost" ssl="false" tls="false" username=""/></mail>
	public static boolean mailSpoolEnable=true;
	public static int mailSpoolInterval=5;
	public static String mailDefaultCharset="UTF-8";
	public static int mailSpoolTimeout=30;
	public static int mailSpoolServerIdle=10000;
	public static int mailSpoolServerLife=60000;
	public static int mailSpoolServerPort=25;
	public static boolean mailSpoolServerReuseConnection=true;
	public static String mailSpoolServerHost="localhost";
	public static boolean mailSpoolServerSSL=false;
	public static boolean mailSpoolServerTLS=false;
	public static String mailSpoolServerUsername="";
	public static String mailSpoolServerPassword="";
	// <component base-cfml="/lucee/Component.cfc" base-lucee="/lucee/Component.lucee" data-member-default-access="public" dump-template="/lucee/component-dump.cfm">
	public static String componentBaseCFML="/lucee/Component.cfc";
	public static String componentMemberDefaultAccess="public";
	public static String componentDumpTemplate="/lucee/component-dump.cfm";
	// <regional timeserver="pool.ntp.org"/>
	public static String regionalTimeServer="pool.ntp.org";
	public static Locale regionalLocale= Locale.US;
	public static TimeZone regionalTimeZone= TimeZone.getTimeZone("America/New_York");

	public static String ormEngineClass="lucee.runtime.orm.DummyORMEngine";
	// <debugging database="true" debug="true" dump="true" exception="true" implicit-access="false" max-records-logged="10" query-usage="false" template="" timer="false" tracing="false"/>
	public static boolean debugLogMemoryUsage=false;
	public static String debugTemplate="";
	public static int debugDatabase=ConfigImpl.CLIENT_BOOLEAN_TRUE;
	public static int debugEnabled=ConfigImpl.CLIENT_BOOLEAN_TRUE;
	public static int debugDump=ConfigImpl.CLIENT_BOOLEAN_TRUE;
	public static int debugException=ConfigImpl.CLIENT_BOOLEAN_TRUE;
	public static int debugMaxRecords=10;
	public static int debugTimer=ConfigImpl.CLIENT_BOOLEAN_TRUE;
	public static int debugTracing=ConfigImpl.CLIENT_BOOLEAN_TRUE;
	public static int debugQueryUsage=ConfigImpl.CLIENT_BOOLEAN_TRUE;
	// <application allow-url-requesttimeout="false" cache-directory="{lucee-web}/cache/" cache-directory-max-size="100mb" listener-mode="root" listener-type="modern" requesttimeout="365,0,0,25" script-protect="none" type-checking="true"/>
	public static boolean applicationAllowURLRequestTimeout=false;
	public static String applicationCacheDirectory="{lucee-web}/cache/";
	public static String applicationCacheDirectoryMaxSize="100mb";
	public static int applicationListenerMode= ApplicationListener.MODE_ROOT;
	public static String applicationListenerType="modern";
	public static String applicationRequestTimeout="365,0,0,0";
	public static String applicationScriptProtect="none";
	public static boolean applicationTypeChecking=true;
	// <logging>
	public static StaticLog[] loggers=new StaticLog[]{
			new StaticLog("remoteclient", "path:{lucee-config}/logs/remoteclient.log", Log.LEVEL_INFO),
			new StaticLog("requesttimeout", "path:{lucee-config}/logs/requesttimeout.log", Log.LEVEL_ERROR),
			new StaticLog("mail", "path:{lucee-config}/logs/mail.log", Log.LEVEL_ERROR),
			new StaticLog("scheduler", "path:{lucee-config}/logs/scheduler.log", Log.LEVEL_ERROR),
			new StaticLog("trace", "path:{lucee-config}/logs/trace.log", Log.LEVEL_ERROR),
			new StaticLog("application", "path:{lucee-config}/logs/application.log", Log.LEVEL_INFO),
			new StaticLog("mapping", "path:{lucee-config}/logs/mapping.log", Log.LEVEL_ERROR),
			new StaticLog("exception", "path:{lucee-config}/logs/exception.log", Log.LEVEL_INFO),
			new StaticLog("rest", "path:{lucee-config}/logs/rest.log", Log.LEVEL_ERROR),
			new StaticLog("gateway", "path:{lucee-config}/logs/gateway.log", Log.LEVEL_ERROR),
			new StaticLog("search", "path:{lucee-config}/logs/search.log", Log.LEVEL_ERROR),
			new StaticLog("scope", "path:{lucee-config}/logs/scope.log", Log.LEVEL_ERROR),
			new StaticLog("thread", "path:{lucee-config}/logs/thread.log", Log.LEVEL_ERROR),
			new StaticLog("deploy", "path:{lucee-config}/logs/deploy.log", Log.LEVEL_ERROR),
			new StaticLog("memory", "path:{lucee-config}/logs/memory.log", Log.LEVEL_ERROR),
			new StaticLog("datasource", "path:{lucee-config}/logs/datasource.log", Log.LEVEL_ERROR)
	};

	// <java inspect-templates="once">
	public static short inspectTemplates=INSPECT_ONCE;
	public static short startupCompilation=RECOMPILE_NEVER;

	// we might need the lucee mapping later, it seems to be forcing it to exist somehow currently.
//	<mapping
//	readonly="yes"
//	virtual="/lucee-server/"
//	physical="{lucee-server}/context/"
//	archive=""
//	primary="physical"
//	listener-mode="modern"
//	listener-type="curr2root"
//	inspect-template="once"/>
	// <mapping
	//			readonly="yes"
	//			virtual="/lucee/"
	//			physical="{lucee-config}/context/"
	//			archive="{lucee-config}/context/lucee-context.lar"
	//			primary="physical"
	//			listener-mode="modern"
	//			listener-type="curr2root"
	//			inspect-template="once"/>

	public static String[] mappingVirtualPaths=new String[]{
			"/lucee/",
			"/zcorecachemapping",
			"/zcorerootmapping",
			"/jetendo-themes",
			"/jetendo-sites-writable",
			"/jetendo-database-upgrade"
	};
	public static String[] mappingPhysicalPaths=new String[]{
			"{lucee-web}/context/",
			"/var/jetendo-server/jetendo/sites-writable/sa_farbeyondcode_com/_cache",
			"/var/jetendo-server/jetendo/core",
			"/var/jetendo-server/jetendo/themes",
			"/var/jetendo-server/jetendo/sites-writable",
			"/var/jetendo-server/jetendo/database-upgrade"
	};

	// <compiler dot-notation-upper-case="false" externalize-string-gte="-1" full-null-support="true" handle-unquoted-attribute-value-as-string="true" suppress-ws-before-arg="true"/><charset template-charset="UTF-8"/>
	public static boolean compilerDotNotationUpperCase=false;
	public static String compilerExternalizeStringGTE="-1";
	public static boolean compilerFullNullSupport=true;
	public static boolean compilerHandleUnquotedAttributeValueAsString=true;
	public static boolean compilerSuppressWhiteSpaceBeforeArg=true;
	public static String compilerTemplateCharset="UTF-8";

	// <rhextension>
	// mariadb is not in the list below because we install it manually instead
	public static String[] extensionName=new String[]{
		"Image extension",
		"ReloadLuceeExtension extension",
		"ScryptEncrypt extension"
	};
	public static String[] extensionFileName=new String[]{
		"rsbclp29cssd.lex",
		"1cdazsrfl31sl.lex",
		"9nus900n160j.lex"
	};
	public static String[] extensionID=new String[]{
		"B737ABC4-D43F-4D91-8E8E973E37C40D1B",
		"B8E98794-20C7-4604-8DF43FDB06535014",
		"EA788F94-59AF-4059-A76511AC6B016595"
	};
	public static String[] extensionCoreVersion=new String[]{
		"5.3.2.16",
		"5.3.2.16",
		"5.3.2.16"
	};
	public static boolean[] extensionStartBundles=new boolean[]{
		false,
		true,
		true
	};
	public static String[] extensionReleaseType=new String[]{
		"all",
		"all",
		"all"
	};

	//	<security  tag_execute="none" tag_import="none" tag_object="none" tag_registry="none">
	public static short securityExecute=VALUE_NO;
	public static short securityTagImport=VALUE_NO;
	public static short securityDirectJavaAccess=VALUE_NO;
	public static short securityRegistry=VALUE_NO;
	public static short securityJavaObject=VALUE_NO;
	// jetendo security
	public static String[] securityAllowPaths=new String[]{
		"/var/jetendo-server/lucee/tomcat/lucee-server/context/userdata",
		"/var/jetendo-server/jetendo/core",
		"/var/jetendo-server/jetendo/sites",
		"/var/jetendo-server/jetendo/share",
		"/var/jetendo-server/jetendo/execute",
		"/var/jetendo-server/jetendo/public",
		"/var/jetendo-server/luceevhosts/1599b2419bcff43008448d60f69f646e",
		"/var/jetendo-server/jetendo/sites-writable",
		"/var/jetendo-server/jetendo/themes",
		"/var/jetendo-server/jetendo/database-upgrade",
		"/var/jetendo-server/backup",
		"/var/jetendo-server/lucee/tomcat/lucee-server/context",
		"/zbackup2/backup",
		"/zbackup2/jetendo",
		"/zbackup3/jetendo",
		"/var/jetendo-server/luceevhosts/server/lucee-server"
	};
}
