# Backup
Backing up can be done either manually (through Settings), 
prompted (Through "Backup Now" prompt in a reminder) 
or automatically (via  email, see [Email Backup](backup_email.md)).

Non-manual backups are managed by [BackupCenter](../../app/src/main/java/nl/joozd/logbookapp/core/BackupCenter.kt).

Keeping track of backups is a background task, which initially gets launched from MainActivity 
(see [Background Tasks](background_tasks.md)).
Here, BackupCenter gets initialized lazily (it's a class instead of an object for testing purposes)
and "makeOrScheduleBackupNotification()" is called.
This makes a flow with relevant data (when is next action needed, is an email already scheduled, 
does user want emails) and starts observing it. Whenever relevant data changes, it will do it's 
thing (usually just when starting, but also if user changes interval or a backup email is 
successfully sent).

It then either does nothing (backup interval set to NEVER), sends an email (backup needed and email 
enabled) or tells MessageCenter to show a notification. 

Sending an email is done by just settign TaskFlags.sendBackupEmail to true. In this case it is a 
long running task, and TaskDispatcher will take care of the rest. 