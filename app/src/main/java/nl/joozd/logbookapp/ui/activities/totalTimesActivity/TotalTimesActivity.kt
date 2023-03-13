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

package nl.joozd.logbookapp.ui.activities.totalTimesActivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
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

        val binding = ActivityTotalTimesBinding.inflate(layoutInflater).apply {
            initializeToolbar()
            initializeTotalsListsExpandableListView()
        }

        setContentView(binding.root)
    }

    /*
     * Adding extra Totals lists to this is done in [TotalTimesViewModel.allLists]
     */
    private fun ActivityTotalTimesBinding.initializeTotalsListsExpandableListView() {
        val totalTimesExpandableListAdapter = TotalTimesExpandableListAdapter()
        totalTimesExListView.adapter = totalTimesExpandableListAdapter
        totalTimesExListView.layoutManager = LinearLayoutManager(activity)

        viewModel.allLists.launchCollectWhileLifecycleStateStarted { lists ->
            totalTimesExpandableListAdapter.submitList(lists)
        }
    }

    private fun ActivityTotalTimesBinding.initializeToolbar() {
        setSupportActionBarWithReturn(totalTimesToolbar)?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.totalTimes)
        }
    }
}
