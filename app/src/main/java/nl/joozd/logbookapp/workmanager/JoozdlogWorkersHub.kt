package nl.joozd.logbookapp.workmanager

import androidx.work.*
import nl.joozd.logbookapp.core.App

abstract class JoozdlogWorkersHub {
    private val context get() = App.instance.ctx
    /**
     * Make an input data object with a single string
     */
    protected fun makeDataWithString(tag: String, value: String) =
        Data.Builder().putString(tag, value).build()

    protected fun enqueue(
        periodicWork: PeriodicWorkRequest,
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy
    ) = with (WorkManager.getInstance(context)){
        enqueueUniquePeriodicWork(uniqueWorkName, existingPeriodicWorkPolicy, periodicWork)
    }

    protected fun enqueue(
        work: OneTimeWorkRequest,
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy
    ) = with(WorkManager.getInstance(context)) {
        enqueueUniqueWork(uniqueWorkName, existingWorkPolicy, work)
    }

    /**
     * Build Constraints with requiredNetworkType = NetworkType.CONNECTED
     */
    protected fun makeConstraintsNeedNetwork() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    protected fun OneTimeWorkRequest.Builder.needsNetwork(): OneTimeWorkRequest.Builder{
        setConstraints(makeConstraintsNeedNetwork())
        return this
    }

    protected fun OneTimeWorkRequest.Builder.needsNetwork(needsNetwork: Boolean): OneTimeWorkRequest.Builder{
        if(needsNetwork)
            setConstraints(makeConstraintsNeedNetwork())
        return this
    }

    protected fun PeriodicWorkRequest.Builder.needsNetwork(): PeriodicWorkRequest.Builder{
        setConstraints(makeConstraintsNeedNetwork())
        return this
    }

    protected inline fun <reified T: CoroutineWorker> enqueueOneTimeWorker(
        tag: String,
        needNetwork: Boolean = false,
        existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
    ){
        val task = OneTimeWorkRequestBuilder<T>()
            .addTag(tag)
            .needsNetwork(needNetwork)
            .build()

        enqueue(task, tag, existingWorkPolicy)
    }
}