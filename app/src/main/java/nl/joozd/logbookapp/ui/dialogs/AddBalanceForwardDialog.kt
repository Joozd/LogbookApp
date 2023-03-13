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

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogAddBalanceForwardBinding
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.viewmodels.dialogs.AddBalanceForwardDialogViewmodel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

//NOTE:
//try { hoursAndMinutesStringToInt(it?.toString())?.let { something = it } }
class AddBalanceForwardDialog: JoozdlogFragment() {
    val viewModel: AddBalanceForwardDialogViewmodel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogAddBalanceForwardBinding.bind(layoutInflater.inflate(R.layout.dialog_add_balance_forward, container, false)).apply{
            initializeTextViews()

            initializeUIButtons()

        }.root

    private fun DialogAddBalanceForwardBinding.initializeUIButtons() {
        cancelBalanceForwardDialogButton.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            closeFragment()
        }

        saveBalanceForwardDialogButton.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            saveBalanceForwardAndCloseFragment()
        }

        addBalanceForwardDialogBackground.setOnClickListener {
            activity?.currentFocus?.clearFocus()
        }
    }

    private fun DialogAddBalanceForwardBinding.initializeTextViews(){
        initializeLogbookNameTextView()
        initializeLandingsDayTextView()
        initializeLandingsNightTextView()
        initializeFlightTime()
        initializeMultiPilotTime()
        initializeNightTime()
        initializeIFRTime()
        initializePICTime()
        initializeCopilotTime()
        initializeDualTime()
        initializeInstructorTime()
        initializeSimulatorTime()
    }

    private fun DialogAddBalanceForwardBinding.initializeLogbookNameTextView() {
        viewModel.logbookNameFlow.launchCollectWhileLifecycleStateStarted {
            logbookNameText.setText(it)
        }
        logbookNameText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                logbookNameText.text?.let { viewModel.logbookName = it.toString() }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeLandingsDayTextView() {
        viewModel.landingDayFlow.launchCollectWhileLifecycleStateStarted {
            landingDayText.setText(it)
        }
        landingDayText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                landingDayText.text?.let {
                    try { viewModel.landingDay = it.toString().toInt() }
                    catch(e: NumberFormatException){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeLandingsNightTextView() {
        viewModel.landingNightFlow.launchCollectWhileLifecycleStateStarted {
            landingNightText.setText(it)
        }
        landingNightText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                landingNightText.text?.let {
                    try { viewModel.landingNight = it.toString().toInt() }
                    catch(e: NumberFormatException){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeFlightTime() {
        viewModel.totalTimeOfFlightFlow.launchCollectWhileLifecycleStateStarted {
            totalTimeOfFlightEditText.setText(it)
        }
        totalTimeOfFlightEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                totalTimeOfFlightEditText.text?.let {
                    try { viewModel.aircraftTime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeMultiPilotTime() {
        viewModel.multiPilotFlow.launchCollectWhileLifecycleStateStarted {
            multiPilotTimeEditText.setText(it)
        }
        multiPilotTimeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                multiPilotTimeEditText.text?.let {
                    try { viewModel.multipilotTime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeNightTime() {
        viewModel.nightTimeFlow.launchCollectWhileLifecycleStateStarted {
            nightTimeTextview.setText(it)
        }
        nightTimeTextview.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                nightTimeTextview.text?.let {
                    try { viewModel.nightTime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeIFRTime() {
        viewModel.ifrTimeFlow.launchCollectWhileLifecycleStateStarted {
            ifrTimeTextview.setText(it)
        }
        ifrTimeTextview.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                ifrTimeTextview.text?.let {
                    try { viewModel.ifrTime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializePICTime() {
        viewModel.picTimeFlow.launchCollectWhileLifecycleStateStarted {
            picText.setText(it)
        }
        picText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                picText.text?.let {
                    try { viewModel.picTime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeCopilotTime() {
        viewModel.copilotTimeFlow.launchCollectWhileLifecycleStateStarted {
            copilotText.setText(it)
        }
        copilotText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                copilotText.text?.let {
                    try { viewModel.copilotTime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeDualTime() {
        viewModel.dualTimeFlow.launchCollectWhileLifecycleStateStarted {
            dualText.setText(it)
        }
        dualText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                dualText.text?.let {
                    try { viewModel.dualTime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeInstructorTime() {
        viewModel.instructorTimeFlow.launchCollectWhileLifecycleStateStarted {
            instructorText.setText(it)
        }
        instructorText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                instructorText.text?.let {
                    try { viewModel.instructortime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }
    }
    private fun DialogAddBalanceForwardBinding.initializeSimulatorTime() {
        viewModel.simTimeFlow.launchCollectWhileLifecycleStateStarted {
            simulatorTimeEditText.setText(it)
        }

        simulatorTimeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                simulatorTimeEditText.text?.let {
                    try { viewModel.simTime = hoursAndMinutesStringToInt(it.toString())!! }
                    catch(e: Throwable){
                        showBadInputDataError()
                    }
                }
        }

        simulatorTimeEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                root.clearFocus()
            }
            false
        }
    }

    private fun showBadInputDataError() {
        requireActivity().showFragment(MessageDialog.make("Bad input data"))
    }

    private fun saveBalanceForwardAndCloseFragment(){
        lifecycleScope.launch {
            viewModel.saveBalanceForward()
            closeFragment()
        }
    }
}