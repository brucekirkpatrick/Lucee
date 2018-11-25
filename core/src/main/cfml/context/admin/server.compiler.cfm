<cfset error.message="">
<cfset error.detail="">

<cfadmin 
	action="securityManager"
	type="#request.adminType#"
	password="#session["password"&request.adminType]#"
	returnVariable="hasAccess"
	secType="setting"
	secValue="yes">


<!--- 
Defaults --->
<cfparam name="url.action2" default="list">
<cfparam name="form.mainAction" default="none">
<cfparam name="form.subAction" default="none">

<cftry>
	<cfswitch expression="#form.mainAction#">
	<!--- UPDATE --->
		<cfcase value="#stText.Buttons.Update#">
            <cfif not isDefined('form.suppressWSBeforeArg')>
            	<cfset form.suppressWSBeforeArg=false>
            </cfif>
            
			<cfadmin 
				action="updateCompilerSettings"
				type="#request.adminType#"
				password="#session["password"&request.adminType]#"

                suppressWSBeforeArg="#form.suppressWSBeforeArg#"
				templateCharset="#form.templateCharset#"
				externalizeStringGTE="#form.externalizeStringGTE#"
				remoteClients="#request.getRemoteClients()#">
	
		</cfcase>
	<!--- reset to server setting --->
		<cfcase value="#stText.Buttons.resetServerAdmin#">
			
			<cfadmin 
				action="updateCompilerSettings"
				type="#request.adminType#"
				password="#session["password"&request.adminType]#"

				suppressWSBeforeArg=""
				templateCharset=""
				externalizeStringGTE=""

				remoteClients="#request.getRemoteClients()#">
	
		</cfcase>
	</cfswitch>
	<cfcatch>
		<cfset error.message=cfcatch.message>
		<cfset error.detail=cfcatch.Detail>
		<cfset error.cfcatch=cfcatch>
	</cfcatch>
</cftry>


<!---  	templates.error.error_cfm$cf.str(Llucee/runtime/PageContext;II)Ljava/lang/String;
Error Output --->
<cfset printError(error)>


<!--- 
Redirtect to entry --->
<cfif cgi.request_method EQ "POST" and error.message EQ "">
	<cflocation url="#request.self#?action=#url.action#" addtoken="no">
</cfif>




<cfadmin 
	action="getCompilerSettings"
	type="#request.adminType#"
	password="#session["password"&request.adminType]#"
	returnVariable="setting">

<cfif not hasAccess><cfset noAccess(stText.setting.noAccess)></cfif>

<cfoutput>
	<div class="pageintro">#stText.setting.compiler#</div>
	<form onerror="customError" action="#request.self#?action=#url.action#" method="post">
		<table class="maintbl">
			<tbody>
				
				
				<!--- Template --->
				<tr>
					<th scope="row">#stText.charset.templateCharset#</th>
					<td>
						<cfif hasAccess>
							<input type="text" class="small" name="templateCharset" value="#setting.templateCharset#" />
						<cfelse>
							<input type="hidden" name="templateCharset" value="#setting.templateCharset#">
							<b>#charset.templateCharset#</b>
						</cfif>
						<div class="comment">#stText.charset.templateCharsetDescription#</div>
						<cfsavecontent variable="codeSample">
&lt;cfprocessingdirective pageEncoding="#setting.templateCharset#">
&lt;!--- or --->
&lt;cfscript>processingdirective pageEncoding="#setting.templateCharset#";&lt;/cfscript>
						</cfsavecontent>
						<cfset renderCodingTip( codeSample ,stText.settings.codetip)>
					</td>
				</tr>

				<!--- Externalize Strings --->
				<cfset stText.settings.externalizeStringGTE="Externalize strings">
				<cfset stText.settings.externalizeStringGTEDesc="Externalize strings from generated class files to separate files. This can drastically reduce the memory footprint for templates but can have a negative impact on execution times. A lower ""breakpoint"" will cause slower execution than a higher breakpoint.">

				<cfset stText.settings.externalizeString_1="do not externalize any strings">
				<cfset stText.settings.externalizeString10="externalize strings larger than 10 characters">
				<cfset stText.settings.externalizeString100="externalize strings larger than 100 characters">
				<cfset stText.settings.externalizeString1000="externalize strings larger than 1000 characters">
				<cfset stText.settings.externalizeStringDisabled="disabled">
				<cfscript>
					if(setting.externalizeStringGTE < 10)setting.externalizeStringGTE=-1;
					else if(setting.externalizeStringGTE < 100)setting.externalizeStringGTE=10;
					else if(setting.externalizeStringGTE < 1000)setting.externalizeStringGTE=100;
					else  setting.externalizeStringGTE=1000;
				</cfscript>
				
				<tr>
					<th scope="row">#stText.settings.externalizeStringGTE#</th>
					<td>
						<!---<div class="warning nofocus">
					This feature is experimental.
					If you have any problems while using this functionality,
					please post the bugs and errors in our
					<a href="http://issues.lucee.org" target="_blank">bugtracking system</a>. 
				</div>--->

						<cfif hasAccess>


							<ul class="radiolist">
								
								<!--- not --->
								<cfloop list="-1,1000,100,10" item="val">
									<li>
										<label>
											<input class="radio" type="radio" name="externalizeStringGTE" value="#val#"<cfif setting.externalizeStringGTE == val> checked="checked"</cfif>>
											<b>#stText.settings["externalizeString"&replace(val,"-","_")]#</b>
										</label>
									</li>
								</cfloop>
							</ul>
						<cfelse>
							<input type="hidden" name="externalizeStringGTE" value="#setting.externalizeStringGTE#">
							<b><cfif setting.externalizeStringGTE==-1>#yesNoFormat(false)#<cfelse>#stText.settings["externalizeString"&replace(setting.externalizeStringGTE,"-","_")]#</cfif></b>
						</cfif>
						<div class="comment">#stText.settings.externalizeStringGTEDesc#</div>
						
					</td>
				</tr>

				
				<!--- Suppress Whitespace in front of cfargument --->
				<tr>
					<th scope="row">#stText.setting.suppressWSBeforeArg#</th>
					<td>
						<cfif hasAccess>
        					<input class="checkbox" type="checkbox" name="suppressWSBeforeArg" value="true" <cfif setting.suppressWSBeforeArg>checked="checked"</cfif> />
						<cfelse>
							<b>#yesNoFormat(setting.suppressWSBeforeArg)#</b><br /><input type="hidden" name="suppressWSBeforeArg" value="#setting.suppressWSBeforeArg#">
						</cfif>
						<div class="comment">#stText.setting.suppressWSBeforeArgDesc#</div>
					</td>
				</tr>


				<cfif hasAccess>
					<cfmodule template="remoteclients.cfm" colspan="2">
				</cfif>
			</tbody>
			<cfif hasAccess>
				<tfoot>
					<tr>
						<td colspan="2">
							<input type="submit" class="bl submit" name="mainAction" value="#stText.Buttons.Update#">
							<input type="reset" class="<cfif request.adminType EQ "web">bm<cfelse>br</cfif> button reset" name="cancel" value="#stText.Buttons.Cancel#">
							<cfif request.adminType EQ "web"><input class="br submit" type="submit" class="submit" name="mainAction" value="#stText.Buttons.resetServerAdmin#"></cfif>
						</td>
					</tr>
				</tfoot>
			</cfif>
		</table>
	</form>
</cfoutput>