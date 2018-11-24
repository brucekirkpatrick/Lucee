<!---
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
 --->


 <cftry>
<cfswitch expression="#url.action2#">
	<cfcase value="settings">
		<cfif !structKeyExists(form, "location") OR !structKeyExists(form, "locationCustom")>
			<cfset form.locationCustom = "http://release.lucee.org">
		</cfif>
		<cfadmin
			action="UpdateUpdate"
			type="#request.adminType#"
			password="#session["password"&request.adminType]#"

			updateType="#form.type#"
			updateLocation="#form.locationCustom#"
			remoteClients="#request.getRemoteClients()#">
	</cfcase>
</cfswitch>
<cfcatch>
		<cfset error.message=cfcatch.message>
		<cfset error.detail=cfcatch.Detail>
		<cfset error.cfcatch=cfcatch>
	</cfcatch>
</cftry>
 <cfif request.admintype EQ "web"><cflocation url="#request.self#" addtoken="no"></cfif>
<cfset error.message="">
<cfset error.detail="">

 <cfscript>
	include template="ext.functions.cfm";
	include template="services.update.functions.cfm";

	ud=getUpdateData();
	if(isNull(application.UpdateProvider[ud.location]))  {
		application.UpdateProvider[ud.location]=getAvailableVersion();
	}
	updateData = application.UpdateProvider[ud.location];
	

	admin
			action="getUpdate"
			type="#request.adminType#"
			password="#session["password"&request.adminType]#"
			returnvariable="upd";
	
	stText.services.update.downUpDesc=replace(stText.services.update.downUpDesc,'{version}',server.lucee.version);

		/*if(isNull(providerData.message) || providerData.type == 'warning'){
			error.message = "Couldn't able to reach the server. Please try after some times";
			result.otherVersions = [];
		} else{
			result = providerData;
		}
		updateData=getAvailableVersion();*/


		if(updateData.provider.location EQ "http://update.lucee.org"){
			version = "lucee";
		} 
		else{
			version = "custom";
		}
		versionsStr = {};
		versionsStr.snapShot = {};
		versionsStr.pre_Release= {};
		versionsStr.release = {};
		if(version eq 'custom'){
			versionsStr.custom = {};
		}
		for(type in versionsStr){
			versionsStr[type].upgrade = [];
			versionsStr[type].downgrade = [];
		}
		if(version eq 'custom' && structKeyExists(updateData, "otherVersions") && Len(updateData.otherVersions)){
			for(versions in updateData.otherVersions){
				if(toVersionSortable(versions) LTE toVersionSortable(server.lucee.version)){
						arrayPrepend(versionsStr.custom.downgrade, versions);
				}else{
					arrayPrepend(versionsStr.custom.upgrade, versions);
				}
			}
		}
		
		admin
			action="getMinVersion"
			type="#request.adminType#"
			password="#session["password"&request.adminType]#"
			returnvariable="minVersion";
		minVs = toVersionSortable(minVersion);

		if(structKeyExists(updateData, "otherVersions") && len(updateData.otherVersions)){

			for(versions in updateData.otherVersions ){
				if(versions EQ server.lucee.version) cfcontinue;
				vs=toVersionSortable(versions);
				if(vs LTE minVS) cfcontinue;
				;
				if(FindNoCase("SNAPSHOT", versions)){
					if(vs LTE toVersionSortable(server.lucee.version)){
						arrayPrepend(versionsStr.SNAPSHOT.downgrade, versions);
					} else{
						arrayPrepend(versionsStr.SNAPSHOT.upgrade, versions);
					}
				} 
				else if(FindNoCase("ALPHA", versions) || FindNoCase("BETA", versions) || FindNoCase("RC", versions)){
					if(vs LTE toVersionSortable(server.lucee.version)){
						arrayPrepend(versionsStr.pre_Release.downgrade, versions);
					} else{
						arrayPrepend(versionsStr.pre_Release.upgrade, versions);
					}
				}
				else{
					if(vs LTE toVersionSortable(server.lucee.version)){
						arrayPrepend(versionsStr.release.downgrade, versions);
					} else{
						arrayPrepend(versionsStr.release.upgrade, versions);
					}
				}
			}
		}
		//dump(var:versionsStr,expand:false);
		//dump(var:updateData,expand:false);
	printError(error);
</cfscript>
<cfoutput>

<style>
	.btn {
		color:white;
		background-color:##CC0000;
	}
</style>


	<!--- <h1>#stText.services.update.luceeProvider#</h1>--->
	<p>
		#replace(stText.services.update.titleDesc,'{version}',"<b>"&server.lucee.version&"</b>") #
	</p>
	<cfset hiddenFormContents = "" >
	<cfset count = 1>
	<cfset listVrs = "Release,Pre_Release,SnapShot">
	<cfloop list="#listVrs#" index="key">
		<cfset len = 0>
		<cfset len = len(versionsStr[key].upgrade) + len(versionsStr[key].downgrade)>
		<span>
			<input
				<cfif count EQ 1>class="bl button alignLeft" <cfelseif count EQ StructCount(versionsStr)> class="br button" <cfelse> class="bm button" </cfif>  
				style="width:180px"
				name="changeConnection" 
				id="btn_#UcFirst(Lcase(key))#" 
				value="#stText.services.update.short[key]# (#len#)" 
				onclick="enableVersion('#UcFirst(Lcase(key))#');"  
				type="button"></span>
		<cfset count++>
	</cfloop>
	<div class="msg"></div>
	<cfsavecontent variable="tmpContent">
		<div  class="topBottomSpace">
			<div class="whitePanel">
				<form onerror="customError" action="#request.self#?action=#url.action#" method="post">
					<select name="UPDATE" id="upt_version"  class="large">
						<option value="">--- select the version ---</option>
						<cfloop list="#listVrs#" index="key">
							<cfif len(versionsStr[key].upgrade) || len(versionsStr[key].downgrade)>
								<optgroup class="td_#UcFirst(Lcase(key))#" label="#stText.services.update.short[key]#">
									<cfloop array="#versionsStr[key].upgrade#" index="i">
										<option class="td_#UcFirst(Lcase(key))#" value="#i#">#stText.services.update.upgradeTo# #i#</option>
									</cfloop>

									<cfloop array="#versionsStr[key].downgrade#" index="i">
										<option class="td_#UcFirst(Lcase(key))#" value="#i#">#stText.services.update.downgradeTo# #i#</option>
									</cfloop>
								</optgroup>
							</cfif>
						</cfloop>
					</select>
					<input type="button" class="button submit"
						onclick="changeVersion(this, UPDATE)" 
						name="mainAction" 
						value="#stText.services.update.downUpBtn#">
					<div class="comment">
						<cfloop list="#listVrs#" index="key">
							<div class="itemintro"><b>#stText.services.update.short[key]# :</b> #stText.services.update[key&"Desc"]#</div>
						</cfloop>
					</div>
				</form>
			</div>
		</div>
	</cfsavecontent>

	<div id="updateInfoDesc" style="text-align: center;"></div>
	<div id="group_Connection">
		#tmpContent#
	</div>

	<!--- for custom --->
	<form onerror="customError" action="#go(url.action,"settings")#" method="post">
		<h1>#stText.services.update.customProvider#</h1>
		<table class="maintbl alignLeft">
			<tbody>
				<tr>
					<th scope="row">#stText.services.update.provider#</th>
					<td>
						<ul class="radiolist" id="updatelocations">
							<!--- Release --->
							<li>
								<label>
									<input type="checkbox" id="sp_radio_custom" name="location"<cfif  version EQ 'custom'> checked</cfif> value="cutsomVersion" />
									<input type="hidden" value="#updateData.provider.location#" name="updatedInfo">
									<b>#stText.services.update.location_custom#</b>
								</label>
								<input id="customtextinput" type="text" class="text" name="locationCustom" size="40" value="<cfif  version EQ 'custom'>#updateData.provider.location#</cfif>" disabled>
								<div class="comment">#replace("#stText.services.update.location_customDesc#","{url}","<a href=""http://docs.lucee.org"">http://docs.lucee.org</a>")#</div>
								<cfif version EQ 'custom'>
									<cfhtmlbody>
										<script type="text/javascript">
											$( '##customtextinput' ).attr( 'disabled', false);
										</script>
									</cfhtmlbody>
								</cfif>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<th scope="row">#stText.services.update.type#</th>
					<td>
						<select name="type">
							<option value="manual" <cfif upd.type EQ "manual">selected</cfif>>#stText.services.update.type_manually#</option>
							<option value="auto" <cfif upd.type EQ "auto">selected</cfif>>#stText.services.update.type_auto#</option>
						</select>
						<div class="comment">#stText.services.update.typeDesc#</div>
					</td>
				</tr>
			</tbody>
			<tfoot>
				<tr>
					<td colspan="2">
						<input type="submit" class="bl button submit" name="mainAction" value="#stText.Buttons.Update#">
						<input type="reset" class="br button reset" name="cancel" value="#stText.Buttons.Cancel#">
					</td>
				</tr>
			</tfoot>
		</table>
	</form>

	<cfhtmlbody>
		<script type="text/javascript">
			$(document).ready(function(){
				if($('##sp_radio_custom').prop("checked")){
					$( '##customtextinput' ).attr( 'disabled', false);
					$('##customURL').attr( 'disabled', false);
				}
				var version = 'Release';

				enableVersion(version, "intial");
				$("##btn_"+version).addClass("btn");
			});

			function enableVersion(v, i){
				if(i== 'intial'){
					$("##group_Connection").find('optgroup' ).each(function(index) {
						var xx = $(this).attr('class');
						window[xx] = $("."+xx).detach();
						if("td_"+v == xx){
							$("##upt_version").append(window[xx]);
						}
				  		$(".btn").removeClass('btn');
				  		$("##btn_"+v).addClass("btn");
					});
				} else {
					if($( "##btn_"+v).hasClass( "btn" )){
						window[v] = $(".td_"+v).detach();
						$("##btn_"+v).removeClass('btn');
					} else {
						;
						$("##upt_version").append(window["td_"+v]);
						$("##btn_"+v).addClass('btn');
					}
				}
			}

			function changeVersion(field, frm) {
				if( frm.value != ""){
					submitted = true;
					$('##group_Connection').hide();
					url='changeto.cfm?#session.urltoken#&adminType=#request.admintype#&version='+frm.value;
					$(document).ready(function(){
						$('##updateInfoDesc').html('<img src="../res/img/spinner16.gif.cfm">');
						disableBlockUI=true;

				 		$.get(url, function(response) {
				      		field.disabled = false;

				 			if((response+"").trim()=="")
								window.location=('#request.self#?action=#url.action#'); //$('##updateInfoDesc').html("<p>#stText.services.update.restartOKDesc#</p>");
							else
								$('##updateInfoDesc').html('<div class="error">'+response+'</div>');
								//window.location=('#request.self#?action=#url.action#'); //$('##updateInfoDesc').html(response);
				 		});
					});
				} else {
					$( ".msg" ).append( "<div class='error'>Please Choose any version</p>" );
				}
			}

			$('##sp_radio_custom').change(function(){
				// enable / disable custom URL field
				if($(this).prop("checked")){
					$( '##customtextinput' ).attr( 'disabled', false);
				} else{
					$( '##customtextinput' ).attr( 'disabled', true);
				}
			});
		</script>
	</cfhtmlbody>

	<p class="comment">* #replace(stText.services.update.titleDesc2,'{min-version}',"<b>"&minVersion&"</b>") #</p>
	
</cfoutput>