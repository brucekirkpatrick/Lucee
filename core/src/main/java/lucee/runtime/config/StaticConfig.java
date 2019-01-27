package lucee.runtime.config;

public class StaticConfig {
	// <system err="default" out="null"/>
	public static String systemError="default";
	public static String systemOut="null";
	// <mode develop="false"/>
	public static boolean modeDevelop=false;


	// <setting allow-compression="false" buffering-output="false" cfml-writer="white-space-pref" content-length="" suppress-content="false"/>
	public static boolean settingAllowCompression=false;
	public static boolean settingBufferingOutput=false;
	public static boolean settingSuppressContent=false;
	public static String settingCFMLWriter="white-space-pref";
	public static String settingContentLength="";


	// <file-system fld-default-directory="{lucee-config}/library/fld/" function-default-directory="{lucee-config}/library/function/" tag-default-directory="{lucee-config}/library/tag/" temp-directory="{lucee-config}/temp/" tld-default-directory="{lucee-config}/library/tld/">
	public static String fileSystemFLDDirectory="{lucee-config}/library/fld/";
	public static String fileSystemTLDDirectory="{lucee-config}/library/tld/";
	public static String fileSystemFunctionDirectory="{lucee-config}/library/function/";
	public static String fileSystemTempDirectory="{lucee-config}/temp/";
	public static String fileSystemTagDirectory="{lucee-config}/library/tag/";
	// <dump-writers>
	public static String dumpWriterBrowser="lucee.runtime.dump.HTMLDumpWriter";
	public static String dumpWriterConsole="lucee.runtime.dump.TextDumpWriter";
	// <resources>
	public static String defaultResourceProviderClass="lucee.commons.io.res.type.file.FileResourceProvider";
	public static String defaultResourceProviderArguments="lock-timeout:1000;";
	public static String ftpResourceProviderClass="lucee.commons.io.res.type.ftp.FTPResourceProvider";
	public static String ftpResourceProviderArguments="lock-timeout:20000;socket-timeout:-1;client-timeout:60000";
	public static String zipResourceProviderClass="lucee.commons.io.res.type.zip.ZipResourceProvider";
	public static String zipResourceProviderArguments="lock-timeout:1000;case-sensitive:true";
	public static String tarResourceProviderClass="lucee.commons.io.res.type.tar.TarResourceProvider";
	public static String tarResourceProviderArguments="lock-timeout:1000;case-sensitive:true;";
	public static String tgzResourceProviderClass="lucee.commons.io.res.type.tgz.TGZResourceProvider";
	public static String tgzResourceProviderArguments="lock-timeout:1000;case-sensitive:true;";
	public static String httpResourceProviderClass="lucee.commons.io.res.type.http.HTTPResourceProvider";
	public static String httpResourceProviderArguments="lock-timeout:10000;case-sensitive:false;";
	public static String httpsResourceProviderClass="lucee.commons.io.res.type.http.HTTPSResourceProvider";
	public static String httpsResourceProviderArguments="lock-timeout:10000;case-sensitive:false;";
	// <scope applicationtimeout="1,0,0,0" cascade-to-resultset="yes" cascading="strict" client-directory-max-size="10mb" client-max-age="90" clientmanagement="no" merge-url-form="no" requesttimeout="0,0,0,50" sessionmanagement="yes" sessiontimeout="0,0,30,0" setclientcookies="yes" setdomaincookies="no"/>
	public static String scopeApplicationTimeout="365,0,0,0";
	public static boolean scopeCascadeToResultset=false;
	public static String scopeCascading="strict";
	public static boolean scopeMergeURLForm=true;
	public static String requestTimeout="0,0,0,25";
	public static boolean sessionManagement=false;
	public static String sessionTimeout="0,0,30,0";
	// <mail spool-enable="yes" spool-interval="5" timeout="30">
	//	<server idle="10000" life="60000" password="" port="25" reuse-connection="true" smtp="localhost" ssl="false" tls="false" username=""/></mail>
	public static boolean mailSpoolEnable=true;
	public static int mailSpoolInterval=5;
	public static int mailSpoolTimeout=30;
	public static int mailSpoolServerIdle=10000;
	public static int mailSpoolServerLife=60000;
	public static int mailSpoolServerPort=25;
	public static boolean mailSpoolServerReuseConnection=true;
	public static String mailSpoolServerHost="localhost";
	public static boolean mailSpoolServerSSL=false;
	public static boolean mailSpoolServerTLS=false;
	public static String mailSpoolServerUsername="";
	// <component base-cfml="/lucee/Component.cfc" base-lucee="/lucee/Component.lucee" data-member-default-access="public" dump-template="/lucee/component-dump.cfm">
	public static String componentBaseCFML="/lucee/Component.cfc";
	public static String componentMemberDefaultAccess="public";
	public static String componentDumpTemplate="/lucee/component-dump.cfm";
	// <regional timeserver="pool.ntp.org"/>
	public static String regionalTimeServer="pool.ntp.org";
	public static String ormEngineClass="lucee.runtime.orm.DummyORMEngine";
	// <debugging database="true" debug="true" dump="true" exception="true" implicit-access="false" max-records-logged="10" query-usage="false" template="" timer="false" tracing="false"/>
	public static boolean debugLogMemoryUsage=false;
	public static String debugTemplate="";
	public static boolean debugEnabled=true;
	public static boolean debugDump=true;
	public static boolean debugException=true;
	public static int debugMaxRecords=10;
	public static boolean debugTimer=false;
	public static boolean debugTracing=false;
	public static boolean debugQueryUsage=false;
	// <application allow-url-requesttimeout="false" cache-directory="{lucee-web}/cache/" cache-directory-max-size="100mb" listener-mode="root" listener-type="modern" requesttimeout="365,0,0,25" script-protect="none" type-checking="true"/>
	public static boolean applicationAllowURLRequestTimeout=false;
	public static String applicationCacheDirectory="{lucee-web}/cache/";
	public static String applicationCacheDirectoryMaxSize="100mb";
	public static String applicationListenerMode="root";
	public static String applicationListenerType="modern";
	public static String applicationRequestTimeout="365,0,0,0";
	public static String applicationScriptProtect="none";
	public static boolean applicationTypeChecking=true;
	// <logging>
	public static String logRemoteClientArguments="path:{lucee-config}/logs/remoteclient.log";
	public static String logRemoteClientLevel="info";
	public static String logRequestTimeoutArguments="path:{lucee-config}/logs/requesttimeout.log";
	public static String logRequestTimeoutLevel="";
	public static String logMailArguments="path:{lucee-config}/logs/mail.log";
	public static String logMailLevel="";
	public static String logSchedulerArguments="path:{lucee-config}/logs/scheduler.log";
	public static String logSchedulerLevel="";
	public static String logTraceArguments="path:{lucee-config}/logs/trace.log";
	public static String logTraceLevel="";
	public static String logApplicationArguments="path:{lucee-config}/logs/application.log";
	public static String logApplicationLevel="info";
	public static String logExceptionArguments="path:{lucee-config}/logs/exception.log";
	public static String logExceptionLevel="info";
	public static String logMappingArguments="path:{lucee-config}/logs/mapping.log";
	public static String logMappingLevel="";
	public static String logRestArguments="path:{lucee-config}/logs/rest.log";
	public static String logRestLevel="";
	public static String logGatewayArguments="path:{lucee-config}/logs/gateway.log";
	public static String logGatewayLevel="";
	public static String logSearchArguments="path:{lucee-config}/logs/search.log";
	public static String logSearchLevel="";
	public static String logScopeArguments="path:{lucee-config}/logs/scope.log";
	public static String logScopeLevel="";
	public static String logThreadArguments="path:{lucee-config}/logs/thread.log";
	public static String logThreadLevel="";
	public static String logDeployArguments="path:{lucee-config}/logs/deploy.log";
	public static String logDeployLevel="";
	public static String logMemoryArguments="path:{lucee-config}/logs/memory.log";
	public static String logMemoryLevel="";
	public static String logDatasourceArguments="path:{lucee-config}/logs/datasource.log";
	public static String logDatasourceLevel="";

	// <java inspect-templates="once">
	public static boolean inspectTemplates=true;

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
			"/zcorecachemapping",
			"/zcorerootmapping",
			"/jetendo-themes",
			"/jetendo-sites-writable",
			"/jetendo-database-upgrade"
	};
	public static String[] mappingPhysicalPaths=new String[]{
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
	public static boolean securityExecute=false;
	public static boolean securityTagImport=false;
	public static boolean securityDirectJavaAccess=false;
	public static boolean securityRegistry=false;
	public static boolean securityJavaObject=false;
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
