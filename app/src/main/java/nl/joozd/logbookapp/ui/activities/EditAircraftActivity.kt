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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_edit_aircraft.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Aircraft

import nl.joozd.logbookapp.ui.adapters.EditAircraftAdapter
import nl.joozd.logbookapp.ui.dialogs.EditAircraftDialog
import nl.joozd.logbookapp.ui.utils.toast


// TODO: Make search function
// TODO: Make add function
// TODO make edit function


class EditAircraftActivity : AppCompatActivity() {
    companion object{
        const val TAG = "EditAircraftActivity"
    }
    //val aircraftDb = AircraftDb()
    private lateinit var allAircraft: List<Aircraft>
    private lateinit var editAircraftAdapter: EditAircraftAdapter

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_aircraft, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.addAircraftMenu -> {
            // addAircraftButton.hide()
            toast("TODO: make aircraft edit fragment")
            showEditAircraftDialog(null)

            true
        }
        else -> false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allAircraft = (intent.getParcelableArrayExtra("allAircraft") ?: emptyArray()).map{it as Aircraft }
        val missingAircraft = (intent.getParcelableArrayExtra("missingAircraft") ?: emptyArray()).map{it as Aircraft }
        Log.d(TAG,"found ${allAircraft.size} and ${missingAircraft.size} aircraft")


        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_edit_aircraft)
        setSupportActionBar(editAircraftToolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources.getString(R.string.editAircraft)

        editAircraftAdapter = EditAircraftAdapter(allAircraft, missingAircraft){
            editAircraft(it)
        }
        aircraftRecyclerView.layoutManager = LinearLayoutManager(this)
        aircraftRecyclerView.adapter = editAircraftAdapter


    }
    private fun editAircraft(aircraft: Aircraft) {
        // TODO("open aircraft editing fragment")
        toast("selected ${aircraft.registration}")
        showEditAircraftDialog(aircraft)
    }

    private fun showEditAircraftDialog(aircraft: Aircraft?){
        val editAircraftDialog = EditAircraftDialog()
        //editAircraftDialog.aircraft = aircraft ?: editAircraftDialog.aircraft.copy(id=aircraftDb.highestId+1)
        editAircraftDialog.allAircraft = (intent.getParcelableArrayExtra("allAircraft") ?: emptyArray()).map{it as Aircraft }
        editAircraftDialog.setOnSave { ac ->
            Log.d(TAG, "this will save aircraft: $ac")
            //aircraftDb.saveAircraft(ac)
            // = aircraftDb.requestAllAircraft().sortedBy { it.registration }

            // editAircraftAdapter.allAircraft = allAircraft
            editAircraftAdapter.notifyDataSetChanged()



        }
        supportFragmentManager.beginTransaction()
            .add(R.id.editAircraftBelowToolbar, editAircraftDialog)
            .addToBackStack(null)
            .commit()
    }


}
