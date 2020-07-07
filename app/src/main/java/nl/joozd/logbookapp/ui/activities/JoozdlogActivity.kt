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

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

@SuppressLint("Registered")
open class JoozdlogActivity: AppCompatActivity() {

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

    fun startMainActivity(context: Context) = with (context) {
        startActivity(packageManager.getLaunchIntentForPackage(packageName))
    }


}