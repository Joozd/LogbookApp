package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.comm.OldCloud
import nl.joozd.logbookapp.data.comm.ServerFunctionResult

class SendLoginLinkEmailWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        when(OldCloud.requestLoginLinkMail()){
            ServerFunctionResult.OK -> Result.success()
            ServerFunctionResult.EMAIL_DOES_NOT_MATCH, ServerFunctionResult.UNKNOWN_USER_OR_PASS -> Result.failure() // requestBackupEmail will set flags to alert user
            ServerFunctionResult.DATA_ERROR, ServerFunctionResult.CLIENT_ERROR, ServerFunctionResult.UNKNOWN_REPLY_FROM_SERVER -> Result.retry()
            else -> Result.failure()
        }
    }
}