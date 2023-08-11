/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.logbookapp.ui.utils


import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.background.startBackgroundTasks

abstract class JoozdlogActivity: AppCompatActivity() {
    // make this true to not start background tasks in activity
    open val runBackgroundTasks = true

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    override fun onStart() {
        super.onStart()
        if(runBackgroundTasks)
            startBackgroundTasks()
    }

    protected val activity: JoozdlogActivity
        get() = this

    /**
     * use  setSupportActionBarWithReturn(this_activities_toolbar)?.apply { title="HALLON AUB GRGR" }
     */
    protected fun setSupportActionBarWithReturn(toolbar: Toolbar?): ActionBar? {
        super.setSupportActionBar(toolbar)
        return supportActionBar
    }

    protected fun closeAndStartMainActivity(){
        startMainActivity(this)
        finish()
    }

    fun <T> Flow<T>.launchCollectWhileLifecycleStateStarted(collector: FlowCollector<T>){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(collector)
            }
        }
    }

    protected fun removeFragment(it: Fragment) {
        supportFragmentManager.commit { remove(it) }
    }

    private fun startMainActivity(context: Context) = with (context) {
        startActivity(packageManager.getLaunchIntentForPackage(packageName))
    }

    /**
     * dp to pixels and reverse
     */

    @Suppress("unused")
    protected fun Float.pixelsToDp() = this / resources.displayMetrics.density

    @Suppress("unused")
    protected fun Float.dpToPixels() = this * resources.displayMetrics.density

    @Suppress("unused")
    protected fun Int.pixelsToDp() = this.toFloat() / resources.displayMetrics.density

    protected fun Int.dpToPixels() = this.toFloat() * resources.displayMetrics.density
}