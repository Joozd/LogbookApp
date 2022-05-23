package nl.joozd.logbookapp.core.usermanagement

import androidx.work.ListenableWorker.Result

enum class UserManagementFunctionResult {
    SUCCESS,
    FAILURE,
    RETRY;

    fun toListenableWorkerResult(): Result = when (this){
        SUCCESS -> Result.success()
        FAILURE -> Result.failure()
        RETRY -> Result.retry()
    }
}