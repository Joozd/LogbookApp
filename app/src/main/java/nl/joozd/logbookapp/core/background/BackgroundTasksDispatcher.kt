package nl.joozd.logbookapp.core.background

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

abstract class BackgroundTasksDispatcher {
    protected abstract suspend fun startCollectors()

    fun start(activity: JoozdlogActivity) = with(activity) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                startCollectors()
            }
        }
    }

    protected suspend fun Flow<Boolean>.doIfTrueCollected(block: () -> Unit){
        this.collect{
            if (it)
                block()
        }
    }
}