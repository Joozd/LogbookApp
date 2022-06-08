package nl.joozd.logbookapp.comm

import androidx.work.ListenableWorker.Result

/**
 * Invoking this will return true on [SUCCESS], or false on any other condition.
 */
enum class ServerFunctionResult {
    SUCCESS,
    FAILURE,
    RETRY;

    fun toListenableWorkerResult(): Result = when (this){
        SUCCESS -> Result.success()
        FAILURE -> Result.failure()
        RETRY -> Result.retry()
    }

    fun isOK(): Boolean = this == SUCCESS
}