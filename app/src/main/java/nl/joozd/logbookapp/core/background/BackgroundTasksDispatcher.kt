package nl.joozd.logbookapp.core.background

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

abstract class BackgroundTasksDispatcher {
    protected abstract fun startCollectors(scope: CoroutineScope)

    fun start(activity: JoozdlogActivity) = with(activity) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                startCollectors(activity.lifecycleScope)
            }
        }
    }

    protected fun Flow<Boolean>.launchDoIfTrueCollected(scope: CoroutineScope, block: () -> Unit) = scope.launch {
        this@launchDoIfTrueCollected.collect {
            if (it)
                block()
        }
    }
}