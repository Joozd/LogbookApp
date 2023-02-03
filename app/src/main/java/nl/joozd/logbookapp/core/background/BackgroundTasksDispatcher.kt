package nl.joozd.logbookapp.core.background

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

abstract class BackgroundTasksDispatcher {
    protected abstract fun startCollectors(activity: JoozdlogActivity)

    fun start(activity: JoozdlogActivity) {
        startCollectors(activity)
    }

    protected fun Flow<Boolean>.doIfTrueEmitted(activity: JoozdlogActivity, block: () -> Unit) = activity.lifecycleScope.launch {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect {
                if (it)
                    block()
            }
        }
    }
}