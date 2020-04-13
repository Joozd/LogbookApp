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

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.dialog_augmented_crew.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel
import nl.joozd.logbookapp.extensions.onTextChanged

class AugmentedCrewDialog: Fragment(){
    private val supportFragmentManager: FragmentManager by lazy { requireActivity().supportFragmentManager }
    private val viewModel: JoozdlogViewModel by viewModels()

    private val crewValue = MutableLiveData<Crew>()
    private var cv: Crew
        get() = crewValue.value ?: Crew() // shouldn't fall back to default
        set(crew: Crew) { crewValue.value = crew }
    private var flight
        get() = viewModel.workingFlight.value!!
        set(f: Flight) { viewModel.workingFlight.value = f }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val unchangedFlight = flight

        val view = inflater.inflate(R.layout.dialog_augmented_crew, container, false).apply {
            crewDownButton.setOnClickListener {
                if (cv.crewSize > 0) cv = cv.copy(crewSize = cv.crewSize - 1)
            }
            crewUpButton.setOnClickListener {
                if (cv.crewSize < 9) cv = cv.copy(crewSize = cv.crewSize + 1)
            }
            didTakeoffCheckbox.setOnCheckedChangeListener { _, b ->
                cv = cv.copy(didTakeoff = b)
            }
            didLandingCheckbox.setOnCheckedChangeListener { _, b ->
                cv = cv.copy(didLanding = b)
            }

            timeForTakeoffLandingEditText.onTextChanged { time ->
                if (time.isNotEmpty()) cv = cv.copy(takeoffLandingTimes = time.toInt())
                else cv = cv.copy(takeoffLandingTimes = Preferences.standardTakeoffLandingTimes)
            }

            cancelCrewDialogButton.setOnClickListener {
                flight = unchangedFlight
                supportFragmentManager.popBackStack()
            }
            augmentedCrewDialogBackground.setOnClickListener {
                flight = unchangedFlight
                supportFragmentManager.popBackStack()
            }
            saveCrewDialogButon.setOnClickListener {
                supportFragmentManager.popBackStack()
            }
        }

        crewValue.observe (viewLifecycleOwner, Observer{
            flight = flight.copy(augmentedCrew = it.toInt())
        })
        viewModel.workingFlight.observe (viewLifecycleOwner, Observer{
            updateView(view)
        })
        cv = Crew.of(flight.augmentedCrew)

        return view
    }

    private fun updateView(view: View){
        if (view.crewSizeEditText.text.toString() != cv.crewSize.toString()) view.crewSizeEditText.setText(cv.crewSize.toString())
        if (view.didLandingCheckbox.isChecked != cv.didLanding) view.didLandingCheckbox.isChecked = cv.didLanding
        if (view.didTakeoffCheckbox.isChecked != cv.didTakeoff) view.didTakeoffCheckbox.isChecked = cv.didTakeoff
        if (view.timeForTakeoffLandingEditText.text.toString() != cv.takeoffLandingTimes.toString()) view.timeForTakeoffLandingEditText.setText(cv.takeoffLandingTimes.toString())
    }
}

