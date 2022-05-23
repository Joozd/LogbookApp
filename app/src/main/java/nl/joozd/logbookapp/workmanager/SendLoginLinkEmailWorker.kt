package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.comm.CloudFunctionResult

class SendLoginLinkEmailWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        when(Cloud().requestLoginLinkMail()){
            CloudFunctionResult.OK -> {
                TaskFlags.postSendLoginLink(false)
                Result.success()
            }
            CloudFunctionResult.SERVER_REFUSED ->
                //flag remains set, Cloud handles the reason for failure; fixing that should re-trigger collection of TaskFlag.

                /*
                  For my own understanding -> Handler sets "EmailPrefs.emailVerifiedFlow" to false, and triggers logic to re-verify.
                  If EmailPrefs.emailVerified(Flow) is false, TaskDispatcher.validEmailFlow is false and therefore also backupEmailWantedFlow.
                  Once EmailPrefs.emailVerified gets to true again, that goes down the chain to backupEmailWantedFlow, which will trigger activation of this worker.
                 */
                Result.failure()

            CloudFunctionResult.CONNECTION_ERROR -> Result.retry() // retry on connection error. In case app gets restarted and TaskFlag is collected again, this Worker will be REPLACE 'd
        }
    }
}