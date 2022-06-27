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


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.CompoundButton
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.core.background.startBackgroundTasks
import nl.joozd.logbookapp.core.usermanagement.UserManagement

@SuppressLint("Registered")
open class JoozdlogActivity: AppCompatActivity() {
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    protected fun <T> Flow<T>.launchCollectWhileLifecycleStateStarted(collector: FlowCollector<T>){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(collector)
            }
        }
    }

    private fun startMainActivity(context: Context) = with (context) {
        startActivity(packageManager.getLaunchIntentForPackage(packageName))
    }


    protected fun sendMessageToOtherApp(message: String, subject: String? = null){
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, null))
    }

    /**
     * Bind a CompoundButton (e.g. a Switch) to a Flow<Boolean>.
     * The only way to change this buttons state after calling this function is to change the output of [flow]
     */
    protected fun CompoundButton.bindToFlow(flow: Flow<Boolean>){
        setOnCheckedChangeListener { _, _ ->
            lifecycleScope.launch {
                flow.first().let{
                    if (isChecked != it)
                        isChecked = it
                }
            }
        }
        flow.launchCollectWhileLifecycleStateStarted{
            println("COLLECTED: $it from $flow")
            isChecked = it
        }
    }

    /**
     * dp to pixels and reverse
     */

    protected fun Float.pixelsToDp() = this / resources.displayMetrics.density
    protected fun Float.dpToPixels() = this * resources.displayMetrics.density
    protected fun Int.pixelsToDp() = this.toFloat() / resources.displayMetrics.density
    protected fun Int.dpToPixels() = this.toFloat() * resources.displayMetrics.density



}