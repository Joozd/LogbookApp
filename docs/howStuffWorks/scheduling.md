#Scheduling
Tasks are scheduled through the TaskFlags object.
Simply set a flag to true (eg. updateEmailWithServer) will schedule an action.
Required data from `TaskPayloads` needs to be set before an action will actually be scheduled.
Example: Setting `verifyEmailCode` without setting `TaskPayloads.emailConfirmationStringWaiting` will do nothing.
`TaskDispatcher` takes care of combining inputs and the actual scheduling.

## Flags in use:
##### sendBackupEmail
- Will wait for a valid email (valid address + verified) is stored.
- Schedules a backup email to be sent
##### verifyEmailCode
- Will wait for `TaskPayloads.emailConfirmationStringWaiting` to be not blank.
- Verifies an email code.
##### updateEmailWithServer
- Will wait for `EmailPrefs.emailAddress` to be not blank.
- Will send an email address to server to be confirmed.
##### feedbackWaiting
- Will wait for `TaskPayloads.feedbackWaiting` to be not blank. 
- Does not care about state of `TaskPayloads.feedbackContactInfoWaiting` but task uses it when set.
- Will send user feedback to server
##### syncDataFiles
- Will sync data files (airport/aircraftTypes/forcedTypes)