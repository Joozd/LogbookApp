package nl.joozd.logbookapp.workmanager.userManagementWorkers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.UserManagement
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.CloudFunctionResult
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.generateKey

/*
 * This gets called from TaskDispatcher when [TaskDispatcher.newUserWantedFlow] == true,
 *  which is triggered primarily by [TaskFlags.createNewUser]
 * return:
 *  - Success() if successful, should set it's flag to false
 *  - Retry() if connection error from cloud function, should not touch its flag
 *  - Failure() if server refused to perform task, should not touch its flag
 *      (server refusal handling should have set another flag which will prevent this worker from being called until it is fixed)
 */
class CreateNewUserWorker(appContext: Context, workerParams: WorkerParameters, private val cloud: Cloud = Cloud())
: CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(DispatcherProvider.io()) {
        createNewUser().also {
            if (it == Result.success())
                TaskFlags.createNewUser = false // blocking is OK in this context
        }
    }


    /*
     * This will invalidate current login data.
     * - Asks a username from server
     * - generates a password
     * - saves them to Prefs.
     * @return success() if new data received and saved, retry() if connection error, failure() if server refused.
     */
    private suspend fun createNewUser(): Result =
        cloud.requestUsername()?.let { n ->
            val k = generateKey()
            createNewUserOnServer(n, k)
        } ?: Result.retry()


    /*
     * Create a new user on server
     * Returns whether this can eb considered a success, failure or should be retried.
     */
    private suspend fun createNewUserOnServer(username: String, key: ByteArray): Result =
        when(cloud.createNewUser(username, key)){
            CloudFunctionResult.OK -> {
                storeLoginData(username, key) // blocking is OK in this context
                resetEmailData()
                Result.success()
            }
            CloudFunctionResult.CONNECTION_ERROR -> Result.retry()
            CloudFunctionResult.SERVER_REFUSED -> Result.failure()
        }

    // mark email as unverified
    private fun resetEmailData() {
        EmailPrefs.postEmailVerified(false)
        UserManagement().requestEmailVerificationMail()
    }

    // has blocking IO ops
    private fun storeLoginData(username: String, key: ByteArray) {
        Prefs.username = username
        Prefs.key = key
        Prefs.postLastUpdateTime(-1)
    }
}
