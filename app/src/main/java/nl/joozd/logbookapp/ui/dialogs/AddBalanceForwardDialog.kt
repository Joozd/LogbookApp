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

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import nl.joozd.joozdlogcommon.BalanceForward
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogAddBalanceForwardBinding
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.BalanceForwardDialogEvents
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.dialogs.AddBalanceForwardDialogViewmodel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast


class AddBalanceForwardDialog: JoozdlogFragment() {
    val viewModel: AddBalanceForwardDialogViewmodel by viewModels()
    private val bf: BalanceForward
        get() = viewModel.workingBalanceForward

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogAddBalanceForwardBinding.bind(layoutInflater.inflate(R.layout.dialog_add_balance_forward, container, false)).apply{

            /**
             * FIll fields opon (re)-creation of Fragment:
             */
            fillFields()
            logbookNameText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setName(logbookNameText.text)
            }
            multiPilotTimeEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setMultipilotTime(multiPilotTimeEditText.text)
            }
            totalTimeOfFlightEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setTotalTime(
                        totalTimeOfFlightEditText.text)
            }
            landingDayText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setLandingsDay(landingDayText.text)
            }
            landingNightText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setLandingsNight(landingNightText.text)
            }
            nightTimeText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setNightTime(nightTimeText.text)
            }
            ifrTimeText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setIfrTime(ifrTimeText.text)
            }
            picText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setPicTime(picText.text)
            }
            copilotText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setCopilotTime(copilotText.text)
            }
            dualText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setDualTime(dualText.text)
            }
            instructorText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setInstructorTime(instructorText.text)
            }
            simulatorTimeEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setSimTime(simulatorTimeEditText.text)
            }
            simulatorTimeEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //Clear focus here from edittext
                    viewModel.setSimTime(simulatorTimeEditText.text)
                    root.clearFocus()
                }
                false
            }

            viewModel.feedbackEvent.observe(viewLifecycleOwner) {
                when (it.getEvent()){
                    BalanceForwardDialogEvents.NUMBER_PARSE_ERROR -> toast ( "ERROR")
                    BalanceForwardDialogEvents.UPDATE_FIELDS -> fillFields()
                    BalanceForwardDialogEvents.CLOSE_DIALOG -> {
                        closeFragment()
                    }
                }
            }

            cancelBalanceForwardDialogButton.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                closeFragment()
            }

            saveBalanceForwardDialogButton.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.saveBalanceForward()
            }

            addBalanceForwardDialogBackground.setOnClickListener {
                // Do nothing
            }

            /*************************************************************************************
             * Observers
             *************************************************************************************/

            viewModel.name.observe(viewLifecycleOwner){
                logbookNameText.setText(it)
            }

            viewModel.multiPilot.observe(viewLifecycleOwner) {
                multiPilotTimeEditText.setText(it)
            }

            viewModel.totalTimeOfFlight.observe(viewLifecycleOwner) {
                totalTimeOfFlightEditText.setText(it)
            }

            viewModel.landingDay.observe(viewLifecycleOwner) {
                landingDayText.setText(it)
            }

            viewModel.landingNight.observe(viewLifecycleOwner) {
                landingNightText.setText(it)
            }

            viewModel.nightTime.observe(viewLifecycleOwner) {
                nightTimeText.setText(it)
            }

            viewModel.ifrTime.observe(viewLifecycleOwner) {
                ifrTimeText.setText(it)
            }

            viewModel.picTime.observe(viewLifecycleOwner) {
                picText.setText(it)
            }

            viewModel.copilotTime.observe(viewLifecycleOwner) {
                copilotText.setText(it)
            }

            viewModel.dualTime.observe(viewLifecycleOwner) {
                dualText.setText(it)
            }

            viewModel.instructorTime.observe(viewLifecycleOwner) {
                instructorText.setText(it)
            }

            viewModel.simTime.observe(viewLifecycleOwner) {
                simulatorTimeEditText.setText(it)
            }

        }.root



    /*********************************************************************************************
     * Private helper functions
     *********************************************************************************************/

    /**
     * Sets EditText fields to value as in ViewModel
     */
    private fun DialogAddBalanceForwardBinding.fillFields(){
        logbookNameText.setText(bf.logbookName)
        multiPilotTimeEditText.setText(minutesToHoursAndMinutesString(bf.multiPilotTime))
        totalTimeOfFlightEditText.setText(minutesToHoursAndMinutesString(bf.aircraftTime))
        landingDayText.setText(bf.landingDay.toString())
        landingNightText.setText(bf.landingNight.toString())
        nightTimeText.setText(minutesToHoursAndMinutesString(bf.nightTime))
        ifrTimeText.setText(minutesToHoursAndMinutesString(bf.ifrTime))
        picText.setText(minutesToHoursAndMinutesString(bf.picTime))
        copilotText.setText(minutesToHoursAndMinutesString(bf.copilotTime))
        dualText.setText(minutesToHoursAndMinutesString(bf.dualTime))
        instructorText.setText(minutesToHoursAndMinutesString(bf.instructortime))
        simulatorTimeEditText.setText(minutesToHoursAndMinutesString(bf.simTime))
    }
}