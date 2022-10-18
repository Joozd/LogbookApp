package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.messages.MessagesWaiting
import nl.joozd.logbookapp.comm.confirmEmail
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.utils.DispatcherProvider


class ConfirmEmailWorker(appContext: Context, workerParams: WorkerParameters, private val cloud: Cloud = Cloud())
    : CoroutineWorker(appContext, workerParams) {
    constructor(appContext: Context, workerParams: WorkerParameters): this(appContext, workerParams, Cloud()) // constructor needed to instantiate as a Worker
    override suspend fun doWork(): Result = withContext(DispatcherProvider.io()) {
        getEmailConfirmationStringWaitingOrNull()?.let{ confirmationString ->
            return@withContext confirmEmail(confirmationString, cloud).also{
                if (it.isOK()) {
                    resetEmailCodeVerificationFlag()
                    MessagesWaiting.emailConfirmed(true)
                }
            }.toListenableWorkerResult()
        }
        // Fallback, this should not happen.
        TaskFlags.verifyEmailCode(false) // no code to verify, will never succeed
        MessagesWaiting.noVerificationCodeSavedBug(true)
        return@withContext Result.failure()
    }

    private suspend fun getEmailConfirmationStringWaitingOrNull() =
        TaskPayloads.emailConfirmationStringWaiting().takeIf { it.isNotBlank() }


    private fun resetEmailCodeVerificationFlag() {
        TaskFlags.verifyEmailCode(false)
        TaskPayloads.emailConfirmationStringWaiting("")
    }
}