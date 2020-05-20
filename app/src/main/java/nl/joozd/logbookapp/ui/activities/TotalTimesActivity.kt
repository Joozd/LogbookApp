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

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_total_times.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.helpers.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.activities.TotalTimesViewModel
import nl.joozd.logbookapp.ui.activities.helpers.graphView.DateFormatter
import nl.joozd.logbookapp.ui.adapters.TotalTimesExpandableListAdapter
import nl.joozd.logbookapp.ui.utils.toast


class TotalTimesActivity : JoozdlogActivity() {
    val viewModel: TotalTimesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.fillGraph()

        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_total_times)

        setSupportActionBarWithReturn(totalTimesToolbar as Toolbar)?.apply{
            setDisplayHomeAsUpEnabled(true)
            title = resources.getString(R.string.totalTimes)
        }
        val adapter = TotalTimesExpandableListAdapter(this, emptyList()) // maybe fill async and load while expanding items
        expandible_listview.setAdapter(adapter)
        expandible_listview.expandGroup(0)

        // activate horizontal zooming and scrolling
        with (totalTimesTestGraph) {
            viewport.isScalable = true
            gridLabelRenderer.labelFormatter = DateFormatter()
// activate horizontal scrolling
            viewport.isScrollable = true;
            viewport.isYAxisBoundsManual = true
        }


            viewModel.barGraphData.observe(this, Observer {
                with (totalTimesTestGraph) {
                    addSeries(it)
                    viewport.setMinX(it.lowestValueX)
                    viewport.setMaxX(it.highestValueX)
                    viewport.setMinY((it.lowestValueY *1.1))
                    viewport.setMaxY((it.highestValueY *1.1))


                }
                    Log.d("w00000000000000000t", "htphpthtphtphpthtphtphpthtphtphpthtphtphpthtp")
            })

        viewModel.lineGraphData.observe(this, Observer {
            with (totalTimesTestGraph) {
                addSeries(it)

            }
            Log.d("w00000000000000000t2", "htphpthtphtphpthtphtphp222222222thtphtphpthtphtphpthtp")
        })

            viewModel.text.observe(this, Observer {
                graphTextView.text = it
            })

        viewModel.feedbackEvent.observe(this, Observer {
            when(it.getEvent()){
                FeedbackEvents.GenericEvents.EVENT_1 -> {
                    val date = it.extraData.getString("date")
                    val time = it.extraData.getDouble("value")
                    toast("$date - ${(time.toInt()/60)}:${(time.toInt()%60)} hours!")
                }
            }
        })


    }

}
