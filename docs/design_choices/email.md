#Email Design Choices
##### Backup emails
Backup emails are currently sent through server.
Sending the data through the app's protocol to the server and then making the server forward it has these reasons:
- No acces to SMTP server needed by app
- Server can check if email is confirmed before sending email, preventing spam or errors
- Only certain emails can be sent, preventing spam.
