# Email
### This document explains how all email related parts work
Email is currently only used for [automatic backup](backup_email.md).
Design choices can be found [here](../design_choices/email.md)

Storing email address:
- User has an opportunity to store an email address through **TODO** fragment.
- Email address is stored locally, and confirmed with server.
- Server generates a salt, and saves a hash of the email, so email can be confirmed but not retrieved by server.
- When a new email address is sent to server, server will send a confirmation mail.
- If a new confirmation mail is needed, send a new email address to server (or resend current)

Handling server reponses:
If server encounters a request with a bad email address (unknown or does not produce correct hash):
- App raises flags:
- a flag to signal user that email was rejected. (MessagesWaiting.serverRejectedEmail(true)). 
  User will be shown a dialog with the option to re-enter their address and confirm it, or to ignore it.
- A flag to signal the app that current email is not valid (EmailPrefs.emailVerified(false)). 
  This causes all future email tasks to be on hold until email gets verified.

If user verifies email correctly after this happened, tasks that were scheduled but on hold will be completed 
because TaskDispatcher.validEmailFlow will emit, causing all taskFlows that need a valid email to emit as well.

Anytime a user performs an action that would lead to an action needing a verified email, check if email is valid and verified.
If not, give a dialog allowing user to enter an email address and prompt them to verify it.

*TODO*
On a daily basis, check if an email is entered but not verified. If not, prompt user to correct that.
(either resend email address to server and verify it, or remove email from EmailPrefs.)