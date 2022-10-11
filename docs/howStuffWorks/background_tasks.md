# Background Tasks
There are two kinds of background tasks:
- Long running tasks, like sending a scheduled backup email in two weeks
- Short running tasks, like keeping track of messages that might need to be displayed, 
  or downloading data files

### Long running tasks
Long running tasks are managed with WorkManager. 
Currently the only tasks that use this are related to the server, 
and can all be found in [ServerFunctionsWorkersHub](../../app/src/main/java/nl/joozd/logbookapp/workmanager/ServerFunctionsWorkersHub.kt)

### Short running tasks
Short running tasks are tasks with a short lifespan that only run when the application is active.
Examples are keeping track of messages that need to be displayed, scheduling Long Running Tasks 
(the tasks themselves will then be taken care of by WorkManager), or downloading new data files.
These are started in the activity that needs them to run (currently only MainActivity) by calling 
[JoozdlogActivity.startBackgroundTasks()](../../app/src/main/java/nl/joozd/logbookapp/core/background/startBackgroundTasks.kt)
It tracks things by collecting Flows; calling it in this way makes sure the work stops when the Activity stops.

For a more complete example, see [Backup](backup.md)