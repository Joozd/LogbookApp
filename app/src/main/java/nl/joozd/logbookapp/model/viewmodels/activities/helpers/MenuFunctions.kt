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

package nl.joozd.logbookapp.model.viewmodels.activities.helpers

import android.util.Log
import android.view.View

import kotlinx.android.synthetic.main.activity_main_new.*
import kotlinx.android.synthetic.main.item_progressbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.ui.activities.MainActivity
import nl.joozd.logbookapp.ui.utils.longToast

object MenuFunctions {


    suspend fun sendAllFlightsAndToastResult() = withContext(Dispatchers.Main) {
        val repository = FlightRepository.getInstance()
        when (Cloud.justSendFlights(repository.requestWholeDB())) {
            0 -> longToast("Flights sent ok!")
            1 -> longToast("login error")
            2 -> longToast("sending error")
            else -> error("This should not happen")
        }
    }

    suspend fun rebuildFlightsFromServer(activity: MainActivity) = withContext(Dispatchers.Main) {
        with (activity) {
            val progBarParent =
                layoutInflater.inflate(R.layout.item_progressbar, progressBarField, false)

            progBarParent.setBackgroundColor(getColorFromAttr(android.R.attr.colorPrimary))
            progressBarField.setBackgroundColor(getColorFromAttr(android.R.attr.colorPrimary))
            val progBar = progBarParent.progressBar
            progBarParent.progressBarText.text = "Downloading from server..."
            progressBarField.addView(progBarParent)



            //download with listener
            Cloud.requestAllFlights { p ->
                launch(Dispatchers.Main) {
                    progBar.progress = p
                    if (p % 10 == 0)
                        Log.d("Downloading", "$p%")
                }
            }?.let {
                launch(Dispatchers.Main) {
                    progBarParent.progressBarText.text = "Saving flights..."
                    progBar.max = it.size
                    progBar.progress = 0
                }
                val repository = FlightRepository.getInstance()
                repository.clearDB()
                repository.save(it)
                /* val flightDb = FlightDb()
                flightDb.clearDB()
                flightDb.saveFlights(it) { progress ->
                    launch(Dispatchers.Main) {
                        progBar.progress = progress
                        if (progress % 100 == 0) Log.d("Rebuilding", "$progress flights")
                    }
                }*/

                //progressBarField.invalidate()
                //progressBarField.requestLayout()
            }
            launch(Dispatchers.Main) {
                progBarParent.visibility = View.GONE
                progressBarField.removeView(progBarParent)
            }


        }

    }
}