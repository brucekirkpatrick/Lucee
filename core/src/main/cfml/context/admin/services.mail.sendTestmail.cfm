<cfoutput>
<!--- <h2>#stText.Mail.Settings#</h2> --->
<h2>Send test email</h2>
<form onerror="customError" action="#request.self#?action=#url.action#&row=#url.row#" method="post">
	<!--- <input type="hidden" name="mainAction" value="#stText.Buttons.Setting#">
	<input type="hidden" name="mainAction" value="#stText.Buttons.Setting#"> --->
	<table class="maintbl">
		<tbody>
			<tr>
				<th scope="row">Email</th>
				<td>
					<input type="text" name="toMail" value="" class="medium" required="yes" message="Please enter a valid email, to where you need to send the test email.">
				</td>
			</tr>
		</tbody>
		<tfoot>
			<tr>
				<td colspan="2"><cfoutput>
					<input type="submit" class="button submit" name="mainAction" value="Send test mail">
				</cfoutput></td>
			</tr>
		</tfoot>
	</table>
</form>
</cfoutput>