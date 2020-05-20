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

package nl.joozd.logbookapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_settings.*
import nl.joozd.logbookapp.model.viewmodels.activities.SettingsActivityViewModel
import nl.joozd.logbookapp.ui.activities.JoozdlogActivity

class SettingsActivity : JoozdlogActivity() {
    val viewModel: SettingsActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_settings)

        setSupportActionBarWithReturn(settings_toolbar)?.apply{
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.settings)
        }

        /****************************************************************************************
         * Logic for setters
         ****************************************************************************************/
        settingsUseIataSelector.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setUseIataAirports(isChecked)
        }


        /****************************************************************************************
         * Observers for feedback
         ****************************************************************************************/

        viewModel.settingsUseIataSelectorTextResource.observe(this, Observer {
            settingsUseIataSelector.text = getString(it)
        })
        viewModel.useIataAirports.observe(this, Observer {
            settingsUseIataSelector.isChecked = it
        })



    }
}
