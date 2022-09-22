#Email Backup
Email backups are sent by serverFunctions.sendBackupMailThroughServer()
It works as follows:
- Flights are collected and put in a CSV file
- CSV file is sent to server, along with stored email address
- Server checks email address. 
- if address correct, sends an email with CSV file.
- If address incorrect (unknown or does not produce correct hash), server will respond JoozdlogCommsResponses.EMAIL_NOT_KNOWN_OR_VERIFIED. This should trigger a dialog to the user allowing them to re-enter their email address. See [email](email.md).