/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
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

package nl.joozd.logbookapp.ui.activities

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.Menu
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import nl.joozd.logbookapp.extensions.getColorFromAttr

@SuppressLint("Registered")
open class JoozdlogActivity: AppCompatActivity() {

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    protected val activity: JoozdlogActivity
        get() = this

    /**
     * use  setSupportActionBarWithReturn(this_activities_toolbar)?.apply { title="HALLON AUB GRGR" }
     */
    fun setSupportActionBarWithReturn(toolbar: Toolbar?): ActionBar? {
        super.setSupportActionBar(toolbar)
        return supportActionBar
    }

    fun closeAndstartMainActivity(){
        startMainActivity(this)
        finish()
    }

    protected fun View.joozdLogSetBackgroundColor(color: Int = getColorFromAttr(R.attr.colorPrimary)){
        (this.background as GradientDrawable).colorFilter = PorterDuffColorFilter( color, PorterDuff.Mode.SRC_IN)
    }

    fun startMainActivity(context: Context) = with (context) {
        startActivity(packageManager.getLaunchIntentForPackage(packageName))
    }

    /**
     * dp to pixels and reverse
     */

    protected fun Float.pixelsToDp() = this / resources.displayMetrics.density
    protected fun Float.dpToPixels() = this * resources.displayMetrics.density
    protected fun Int.pixelsToDp() = this.toFloat() / resources.displayMetrics.density
    protected fun Int.dpToPixels() = this.toFloat() * resources.displayMetrics.density



}