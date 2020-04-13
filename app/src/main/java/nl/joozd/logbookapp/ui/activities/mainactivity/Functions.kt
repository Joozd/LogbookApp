/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.ui.activities.mainactivity

import kotlinx.android.synthetic.main.activity_main_new.*
import kotlinx.android.synthetic.main.item_progressbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel
import nl.joozd.logbookapp.extensions.getColorFromAttr

/**
 * This is here to make MainActivity a bit more structured and readable.
 */
object Functions{
    suspend fun update(activity: MainActivity, viewModel: JoozdlogViewModel) = withContext(Dispatchers.IO) f@{
        with (activity) {
            val progBarParent =
                layoutInflater.inflate(R.layout.item_progressbar, progressBarField, false)
            launch (Dispatchers.Main) {
                progBarParent.setBackgroundColor(getColorFromAttr(android.R.attr.colorPrimary))
                progressBarField.setBackgroundColor(getColorFromAttr(android.R.attr.colorPrimary))
            }
            val progBar = progBarParent.progressBar
            launch (Dispatchers.Main)  {
                progBarParent.progressBarText.text = getString(R.string.synchronizing)
                progressBarField.addView(progBarParent)
            }
            launch {
                Cloud.runFullUpdate { p ->
                    launch(Dispatchers.Main) {
                        progBar.progress = p
                    }
                }
                // val allFlights = flightDb.requestAllFlights().sortedByDescending { it.timeOut }
                launch(Dispatchers.Main) {
                    // progBarParent.visibility = View.GONE
                    progressBarField.removeView(progBarParent)
                }
                // allFlights
            }
        }
    }
}