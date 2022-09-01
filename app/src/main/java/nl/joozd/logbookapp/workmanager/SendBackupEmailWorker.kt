package nl.joozd.logbookapp.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.comm.CloudFunctionResult
import nl.joozd.logbookapp.comm.sendBackupMailThroughServer
import nl.joozd.logbookapp.exceptions.CloudException

class SendBackupEmailWorker(appContext: Context, workerParams: WorkerParameters, private val cloud: Cloud = Cloud())
    : CoroutineWorker(appContext, workerParams) {
    constructor(appContext: Context, workerParams: WorkerParameters): this(appContext, workerParams, Cloud()) // constructor needed to instantiate as a Worker
    override suspend fun doWork(): Result =
        try{
            sendBackupMailThroughServer(cloud)
            Result.success()
        } catch (e: CloudException){
            when(e.cloudFunctionResult){
                CloudFunctionResult.SERVER_REFUSED -> Result.failure()
                CloudFunctionResult.CONNECTION_ERROR -> Result.retry()
                else -> error("reply ${e.cloudFunctionResult} should not generate a CloudException")
            }
        }
}
