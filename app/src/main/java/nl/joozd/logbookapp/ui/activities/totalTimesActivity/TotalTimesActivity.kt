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

package nl.joozd.logbookapp.ui.activities.totalTimesActivity

import android.os.Bundle
import androidx.activity.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityTotalTimesBinding
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.TotalTimesViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * TODO: Animate expanding of lists
 */

class TotalTimesActivity : JoozdlogActivity() {
    val viewModel: TotalTimesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * UI related stuff goes in this block
         */
        with (ActivityTotalTimesBinding.inflate(layoutInflater)){

            setSupportActionBarWithReturn(totalTimesToolbar)?.apply {
                setDisplayShowHomeEnabled(true)
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.totalTimes)
            }

            //TODO maybe ever change this to a RecyclerView implementation. Too tired now to think.
            /**
             * Adding extra Totals lists to this is done in [TotalTimesViewModel.allLists]
             */
            val totalTimesExpandableListAdapter = TotalTimesExpandableListAdapter(this@TotalTimesActivity)
            totalTimesExListView.setAdapter(totalTimesExpandableListAdapter)

            viewModel.allLists.observe(activity){ unfiltered ->
                unfiltered.filterNotNull().also {
                    println ("lists: ${it.size}")
                    totalTimesExpandableListAdapter.list = it
                    it.mapIndexed { i, ttl ->
                        if (ttl.autoOpen) totalTimesExListView.expandGroup(i)
                    }
                }
            }
            setContentView(root)
        }
    }
}
