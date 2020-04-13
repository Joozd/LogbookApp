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

package nl.joozd.logbookapp.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_total_times.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.miscClasses.TotalsListGroup
import nl.joozd.logbookapp.ui.adapters.TotalTimesExpandableListAdapter

class TotalTimesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_total_times)
        setSupportActionBar(totalTimesToolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources.getString(R.string.totalTimes)

        val totalTimesData: List<TotalsListGroup> = (intent.getParcelableArrayExtra("totalTimes") ?: emptyArray()).map{it as TotalsListGroup}

        val adapter = TotalTimesExpandableListAdapter(this, totalTimesData as MutableList<TotalsListGroup>) // maybe fill async and load while expanding items
        expandible_listview.setAdapter(adapter)
        expandible_listview.expandGroup(0)
    }
}
