package nl.joozd.logbookapp.workmanager.userManagementWorkers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.UserManagement
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.CloudFunctionResult
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.utils.DispatcherProvider

/*
 * This gets called from TaskDispatcher when [TaskDispatcher.verificationMailWantedFlow] == true,
 *  which is triggered primarily by [TaskFlags.requestVerificationEmail]
 * return:
 *  - Success() if successful, should set it's flag to false
 *  - Retry() if connection error from cloud function, should not touch its flag
 *  - Failure() if server refused to perform task, should not touch its flag
 *      (server refusal handling should have set another flag which will prevent this worker from being called until it is fixed)
 */
class RequestEmailVerificationMailWorker(appContext: Context, workerParams: WorkerParameters, private val cloud: Cloud = Cloud())
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(DispatcherProvider.io()) {
        verifyEmailAddressWithServer().also {
            if (it == Result.success())
                TaskFlags.requestVerificationEmail = false
        }
    }

    /**
     * Request an email address from server.
     * @return true if server promised to send a mail, false if not due to any reason (connection or server refusal)
     */
    /**
     * Request an email address from server.
     * @return true if server promised to send a mail, false if not due to any reason (connection or server refusal)
     */
    private suspend fun verifyEmailAddressWithServer(): Result =
        if (UserManagement().checkIfLoginDataSet())
            when(cloud.sendNewEmailAddress(Prefs.username()!!, Prefs.key()!!, EmailPrefs.emailAddress())){
                CloudFunctionResult.OK -> {
                    EmailPrefs.emailVerified = true // blocking is OK in this context
                    Result.success()
                }
                CloudFunctionResult.CONNECTION_ERROR -> Result.retry()
                CloudFunctionResult.SERVER_REFUSED -> Result.failure()
            }
        else Result.failure()
}