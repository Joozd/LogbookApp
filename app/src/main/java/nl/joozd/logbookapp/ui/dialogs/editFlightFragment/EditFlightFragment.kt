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

package nl.joozd.logbookapp.ui.dialogs.editFlightFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputEditText
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.databinding.LayoutEditFlightFragmentBinding
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.utils.toast

import nl.joozd.logbookapp.model.viewmodels.fragments.NewEditFlightFragmentViewModel
import nl.joozd.logbookapp.ui.adapters.AircraftAutoCompleteAdapter
import nl.joozd.logbookapp.ui.dialogs.*
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.aircraftPicker.AircraftPicker
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.aircraftPicker.BaseAircraftPicker
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.aircraftPicker.SimTypePicker
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.airportPicker.DestPicker
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.airportPicker.OrigPicker
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.namesDialog.PicNameDialog
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.namesDialog.Name2Dialog
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.setDualInstructorField
import nl.joozd.logbookapp.ui.utils.setPicPicusField

class EditFlightFragment: JoozdlogFragment() {
    private val viewModel: NewEditFlightFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflatedLayout =
            inflater.inflate(R.layout.layout_edit_flight_fragment, container, false)
        val binding = LayoutEditFlightFragmentBinding.bind(inflatedLayout).apply {
            setDialogTitle()
            setLongPressListenersForHelpDialogs()
            setAndAttachAdaptersForAutocompleteFields()
            setOnClickListeners()
            setOnFocusChangedListeners()
            startFlowCollectors()
            catchAndIgnoreClicksOnEmptyPartOfDialog()



        }
        return binding.root
    }

    private fun clearFocus() {
        activity?.currentFocus?.clearFocus()
    }

    private fun toastNotImplementedYet() {
        clearFocus()
        toast("Not implemented yet!")
    }

    private fun LayoutEditFlightFragmentBinding.setDialogTitle() {
        flightInfoText.text =
            getString(if (viewModel.isNewFlight) R.string.add_flight else R.string.edit_flight)
    }

    private fun LayoutEditFlightFragmentBinding.setLongPressListenersForHelpDialogs() {
        flightDateField             .setOnLongClickListener { showHelpMessage(R.string.edit_flight_date_help) }
        flightFlightNumberField     .setOnLongClickListener { showHelpMessage(R.string.edit_flight_flight_number_help) }
        flightOrigEditText          .setOnLongClickListener { showHelpMessage(R.string.edit_flight_orig_help) }
        flightDestEditText          .setOnLongClickListener { showHelpMessage(R.string.edit_flight_dest_help) }
        flightTimeOutEditText       .setOnLongClickListener { showHelpMessage(R.string.edit_flight_time_out_help) }
        flightTimeInEditText        .setOnLongClickListener { showHelpMessage(R.string.edit_flight_time_in_help) }
        flightAircraftField         .setOnLongClickListener { showHelpMessage(R.string.edit_flight_aircraft_help) }
        flightTakeoffLandingField   .setOnLongClickListener { showHelpMessage(R.string.edit_flight_takeoff_landing_help) }
        flightNameField             .setOnLongClickListener { showHelpMessage(R.string.edit_flight_name_help) }
        flightName2Field            .setOnLongClickListener { showHelpMessage(R.string.edit_flight_name2_help) }
        flightRemarksField          .setOnLongClickListener { showHelpMessage(R.string.edit_flight_remarks_help) }

        simSelector                 .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sim_help) }
        signSelector                .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sign_help) }
        dualInstructorSelector      .setOnLongClickListener { showHelpMessage(R.string.edit_flight_dual_instructor_help) }
        multiPilotSelector          .setOnLongClickListener { showHelpMessage(R.string.edit_flight_multipilot_help) }
        ifrSelector                 .setOnLongClickListener { showHelpMessage(R.string.edit_flight_ifr_help) }
        picPicusSelector            .setOnLongClickListener { showHelpMessage(R.string.edit_flight_pic_help) }
        pfSelector                  .setOnLongClickListener { showHelpMessage(R.string.edit_flight_pf_help) }
        autoFillCheckBox            .setOnLongClickListener { showHelpMessage(R.string.edit_flight_autovalues_help) }

        simDateField                .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sim_date_help) }
        simTimeField                .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sim_time_help) }
        simAircraftField            .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sim_aircraft_help) }
        simTakeoffLandingsField     .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sim_takeoff_landing_help) }
        simNamesField               .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sim_names_help) }
        simRemarksField             .setOnLongClickListener { showHelpMessage(R.string.edit_flight_remarks_help) }
        simSimSelector              .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sim_help) }
        simSignSelector             .setOnLongClickListener { showHelpMessage(R.string.edit_flight_sign_help) }
    }
    private fun showHelpMessage(message: Int): Boolean{
        supportFragmentManager.commit {
            add(R.id.editFlightFragmentBackground, MessageDialog.make(message))
            addToBackStack(null)
        }
        return true
    }

    private fun LayoutEditFlightFragmentBinding.setAndAttachAdaptersForAutocompleteFields() {
        flightNameField.setAdapter(ArrayAdapter<String>(ctx, R.layout.item_custom_autocomplete))
        flightName2Field.setAdapter(ArrayAdapter<String>(ctx, R.layout.item_custom_autocomplete))
        flightAircraftField.setAdapter(AircraftAutoCompleteAdapter(requireActivity(), R.layout.item_custom_autocomplete))
    }

    private fun LayoutEditFlightFragmentBinding.setOnClickListeners() {
        setToggleSwitchOnClickListeners()
        setLineSelectorKeysOnClickListeners()
        setClosingOnClickListeners()
    }

    private fun LayoutEditFlightFragmentBinding.setToggleSwitchOnClickListeners() {

        simSelector.setOnClickListener {
            clearFocus()
            viewModel.toggleSim()
        }

        signSelector.setOnClickListener {
            launchSignatureDialog()
        }

        dualInstructorSelector.setOnClickListener {
            clearFocus()
            viewModel.toggleDualInstructorNone()
        }

        multiPilotSelector.setOnClickListener {
            clearFocus()
            viewModel.toggleMultiPilot()
        }

        ifrSelector.setOnClickListener {
            clearFocus()
            viewModel.toggleIFR()
        }

        picPicusSelector.setOnClickListener {
            clearFocus()
            viewModel.togglePicusPicNone()
        }

        pfSelector.setOnClickListener {
            clearFocus()
            viewModel.togglePF()
        }

        autoFillCheckBox.setOnClickListener {
            clearFocus()
            viewModel.toggleAutoValues()
        }

        simSimSelector.setOnClickListener {
            clearFocus()
            viewModel.toggleSim()
        }

        simSignSelector.setOnClickListener {
            launchSignatureDialog()
        }


    }

    private fun LayoutEditFlightFragmentBinding.setLineSelectorKeysOnClickListeners() {
        val dateDialogOnClickListener = makeLaunchDateDialogOnClickListener()

        flightDateSelector.setOnClickListener(dateDialogOnClickListener)

        // flightDateField has an onClickListener, not an onFocusChanged as it always uses dialog
        flightDateField.setOnClickListener(dateDialogOnClickListener)
        flightFlightNumberSelector.setOnClickListener { toastNotImplementedYet() }

        //also: this might not work after rotation etc
        flightOrigSelector.setOnClickListener { showOrigPicker() }
        flightDestSelector.setOnClickListener { showDestPicker() }
        flighttOutSelector.setOnClickListener(makeLaunchTimePickerOnClickListener())
        flighttInSelector.setOnClickListener(makeLaunchTimePickerOnClickListener())
        flightAcRegSelector.setOnClickListener { launchSimOrAircraftPicker() }
        flightTakeoffLandingSelector.setOnClickListener { launchLandingsDialog() }
        flightNameSelector.setOnClickListener { launchName1Dialog() }
        flightName2Selector.setOnClickListener { launchName2Dialog() }

        simDateField.setOnClickListener(dateDialogOnClickListener)
        simDateSelectorLeft.setOnClickListener(dateDialogOnClickListener)
        simDateSelectorRight.setOnClickListener(dateDialogOnClickListener)
        simTimeSelectorLeft.setOnClickListener{ toastNotImplementedYet() }
        simTimeSelectorRight.setOnClickListener{ toastNotImplementedYet() }
        simAircraftSelectorLeft.setOnClickListener { launchSimOrAircraftPicker() }
        simTakeoffLandingSelector.setOnClickListener { launchLandingsDialog() }
        simNamesSelectorLeft.setOnClickListener { launchName2Dialog() }
        simNamesSelectorRight.setOnClickListener { launchName2Dialog() }
    }

    private fun makeLaunchDateDialogOnClickListener() = View.OnClickListener {
        clearFocus()
        supportFragmentManager.commit {
            add(
                R.id.mainActivityLayout,
                LocalDatePickerDialog().apply { selectedDate = viewModel.localDate },
                "datePicker"
            )
            addToBackStack(null)
        }
    }

    private fun makeLaunchTimePickerOnClickListener() = View.OnClickListener {
        clearFocus()
        // Get timePicker dialog, update flight in that dialog.
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, TimePicker())
            addToBackStack(null)
        }
    }

    private fun launchSignatureDialog() {
        clearFocus()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, SignatureDialog())
            addToBackStack(null)
        }
    }

    // Launch dialog to edit name for PIC
    private fun launchName1Dialog() {
        clearFocus()
        supportFragmentManager.commit {
            add(
                R.id.mainActivityLayout,
                PicNameDialog()
            )
            addToBackStack(null)
        }
    }
    // Launch dialog to edit name(s) for other crew
    private fun launchName2Dialog() {
        clearFocus()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, Name2Dialog())
            addToBackStack(null)
        }
    }

    private fun launchLandingsDialog() {
        clearFocus()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, LandingsDialog())
            addToBackStack(null)
        }
    }

    private fun launchSimOrAircraftPicker() {
        clearFocus()
        val picker: BaseAircraftPicker = if (viewModel.isSim) SimTypePicker() else AircraftPicker()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, picker)
            addToBackStack(null)
        }
    }

    private fun showDestPicker() {
        clearFocus()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, DestPicker())
            addToBackStack(null)
        }
    }

    private fun showOrigPicker() {
        clearFocus()
        supportFragmentManager.commit {
            //It's okay to have a parameter in this fragment constructor (see [AirportPicker])
            add(R.id.mainActivityLayout, OrigPicker())
            addToBackStack(null)
        }
    }

    private fun LayoutEditFlightFragmentBinding.setOnFocusChangedListeners() {
        // flightDateField has an onClickListener, not an onFocusChanged as it always uses dialog

        flightFlightNumberField.setOnFocusChangeListener { _, hasFocus ->
            flightFlightNumberField.handleFlightNumberFocusChanged(hasFocus)
        }

        flightOrigEditText.setOnFocusChangeListener { _, hasFocus ->
            flightOrigEditText.handleOrigFocusChanged(hasFocus)
        }

        flightDestEditText.setOnFocusChangeListener { _, hasFocus ->
            flightDestEditText.handleDestFocusChanged(hasFocus)
        }

        flightTimeOutEditText.setOnFocusChangeListener { _, hasFocus ->
            flightTimeOutEditText.handleTimeOutFocusChanged(hasFocus)
        }

        flightTimeInEditText.setOnFocusChangeListener { _, hasFocus ->
            flightTimeInEditText.handleTimeInFocusChanged(hasFocus)
        }

        flightAircraftField.setOnFocusChangeListener { _, hasFocus ->
            flightAircraftField.handleAircraftFocusChanged(hasFocus)
        }

        flightTakeoffLandingField.setOnFocusChangeListener { _, hasFocus ->
            flightTakeoffLandingField.handleTakeoffLandingFocusChanged(hasFocus)
        }

        flightNameField.setOnFocusChangeListener { _, hasFocus ->
            flightNameField.handleNameFocusChanged(hasFocus)
        }

        flightName2Field.setOnFocusChangeListener { _, hasFocus ->
            flightName2Field.handleNamesFieldFocusChanged(hasFocus)
        }

        flightRemarksField.setOnFocusChangeListener { _, hasFocus ->
            flightRemarksField.handleRemarksFieldFocusChanged(hasFocus)
        }


        simTimeField.setOnFocusChangeListener { _, hasFocus ->
            simTimeField.handleSimTimeFocusChanged(hasFocus)
        }

        simAircraftField.setOnFocusChangeListener { _, hasFocus ->
            simAircraftField.handleSimAircraftFieldFocusChanged(hasFocus)
        }

        simTakeoffLandingsField.setOnFocusChangeListener { _, hasFocus ->
            simTakeoffLandingsField.handleSimTakeoffLandingsFieldFocusChanged(hasFocus)
        }

        simNamesField.setOnFocusChangeListener { _, hasFocus ->
            simNamesField.handleNamesFieldFocusChanged(hasFocus)
        }

        simRemarksField.setOnFocusChangeListener { _, hasFocus ->
            simRemarksField.handleRemarksFieldFocusChanged(hasFocus)
        }

        simAircraftField
    }

    //This one either sends entered data to viewModel when focus lost,
    //or removes trailing digits when focus gained.
    private var previousEntry = ""
    private fun EditText.handleFlightNumberFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) {
            val t = text?.toString()
            setText(previousEntry)// reset previous entry; any text changes must come from viewModel.
            if (t != previousEntry.removeTrailingDigits())
                viewModel.setFlightNumber(t)
        } else {
            previousEntry = text.toString()
            removeTrailingDigits()
        }
    }

    private fun EditText.handleOrigFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setOrig(text?.toString())
    }

    private fun EditText.handleDestFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setDest(text?.toString())
    }


    private fun EditText.handleTimeOutFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setTimeOut(text?.toString())
    }

    private fun EditText.handleTimeInFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setTimeIn(text?.toString())
    }

    private fun EditText.handleAircraftFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setRegAndType(text?.toString())
    }

    private fun EditText.handleTakeoffLandingFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setTakeoffLandings(text?.toString())
    }

    private fun EditText.handleNameFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setName(text?.toString())
    }

    private fun EditText.handleNamesFieldFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setName2(text?.toString())
    }

    private fun EditText.handleRemarksFieldFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setRemarks(text?.toString())
    }

    private fun EditText.handleSimTimeFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setSimTime(text?.toString())
    }

    private fun EditText.handleSimAircraftFieldFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setSimAircraft(text?.toString())
    }

    private fun EditText.handleSimTakeoffLandingsFieldFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.setTakeoffLandings(text?.toString())
    }


    private fun LayoutEditFlightFragmentBinding.startFlowCollectors(){
        showWelcomeMessageIfNeeded()
        collectNamesForAutoCompleteTextViews()
        collectRegistrationsForAutoCompleteTextView()
        collectFlightPropertyFlows()
    }

    private fun showWelcomeMessageIfNeeded() {
        Prefs.editFlightFragmentWelcomeMessageShouldBeDisplayed.flow.launchCollectWhileLifecycleStateStarted {
            if (it)
                showWelcomeMessage()
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectNamesForAutoCompleteTextViews() {
        viewModel.namesFlow.launchCollectWhileLifecycleStateStarted {
            @Suppress("UNCHECKED_CAST")
            (flightNameField.adapter as ArrayAdapter<String>).apply {
                clear()
                addAll(it)
            }
            @Suppress("UNCHECKED_CAST")
            (flightName2Field.adapter as ArrayAdapter<String>).apply {
                clear()
                addAll(it)
            }
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectRegistrationsForAutoCompleteTextView(){
        viewModel.sortedRegistrationsFlow.launchCollectWhileLifecycleStateStarted{
            (flightAircraftField.adapter as AircraftAutoCompleteAdapter).apply {
                setItems(it)
            }
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectFlightPropertyFlows(){
        collectDateFlow()
        collectFlightNumberFlow()
        collectOrigFLow()
        collectDestFlow()
        collectTimeOutFlow()
        collectTimeInFlow()
        collectAircraftFlow()
        collectTakeoffLandingsFlow()
        collectNameFlow()
        collectName2Flow()
        collectRemarksFlow()

        collectIsSimFlow()
        collectIsSignedFlow()
        collectDualInstructorFlow()
        collectIsMultiPilotFlow()
        collectIsIfrFlow()
        collectPicPicusFlow()
        collectIsPfFlow()

        collectSimTimeFlow()

        collectIsAutoValuesFlow()

    }

    private fun LayoutEditFlightFragmentBinding.collectDateFlow(){
        viewModel.dateFlow.launchCollectWhileLifecycleStateStarted{
            val s = it.toDateString()
            simDateField.setText(s)
            flightDateField.setText(s)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectFlightNumberFlow(){
        viewModel.flightNumberFlow.launchCollectWhileLifecycleStateStarted{
            flightFlightNumberField.setText(it)
        }
    }

    /*
     * We can use setTextIfNotFocused for these fields,
     * as their contents are not changed from any other field, only from entering data in themselves
     * (which is done when losing focus so also ok)
     */
    private fun LayoutEditFlightFragmentBinding.collectOrigFLow(){
        viewModel.origTextFlow.launchCollectWhileLifecycleStateStarted{
            flightOrigEditText.setTextIfNotFocused(it)
        }
        viewModel.origValidFlow.launchCollectWhileLifecycleStateStarted{
            flightOrigEditText.setAirportFieldToValidOrInvalidLayout(it)
            if (!it) toastAirportNotFound()
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectDestFlow(){
        viewModel.destTextFlow.launchCollectWhileLifecycleStateStarted{
            flightDestEditText.setTextIfNotFocused(it)
        }
        viewModel.destValidFlow.launchCollectWhileLifecycleStateStarted{
            flightDestEditText.setAirportFieldToValidOrInvalidLayout(it)
            if (!it) toastAirportNotFound()
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectTimeOutFlow(){
        viewModel.timeOutFlow.launchCollectWhileLifecycleStateStarted{
            flightTimeOutEditText.setTextIfNotFocused(it.toTimeString())
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectTimeInFlow(){
        viewModel.timeInFlow.launchCollectWhileLifecycleStateStarted{
            flightTimeInEditText.setTextIfNotFocused(it.toTimeString())
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectAircraftFlow(){
        viewModel.aircraftFlow.launchCollectWhileLifecycleStateStarted{
            if(it.source == Aircraft.UNKNOWN) launchSimOrAircraftPicker()
            flightAircraftField.setText(it.toString())
            simAircraftField.setTextIfNotFocused(it.type?.shortName ?: "")
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectTakeoffLandingsFlow(){
        viewModel.takeoffLandingsFlow.launchCollectWhileLifecycleStateStarted{
            val s = it.toString()
            flightTakeoffLandingField.setText(s)
            simTakeoffLandingsField.setTextIfNotFocused(s)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectNameFlow(){
        viewModel.nameFlow.launchCollectWhileLifecycleStateStarted{
            flightNameField.setTextIfNotFocused(it)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectName2Flow(){
        viewModel.name2Flow.launchCollectWhileLifecycleStateStarted{
            val s = it.joinToString(";")
            flightName2Field.setTextIfNotFocused(s)
            simNamesField.setTextIfNotFocused(s)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectRemarksFlow(){
        viewModel.remarksFlow.launchCollectWhileLifecycleStateStarted{
            flightRemarksField.setTextIfNotFocused(it)
            simRemarksField.setTextIfNotFocused(it)
        }
    }


    private fun LayoutEditFlightFragmentBinding.collectIsSimFlow(){
        viewModel.isSimFlow.launchCollectWhileLifecycleStateStarted{ isSim ->
            // We don't need to set simSimSelector as active because it is hardcoded like that
            // in XML layout file and only visible in Sim layout.
            // Same goes for inactive simSelector.
            if (isSim){
                flightInputFieldsLayout.visibility = View.GONE
                simInputFieldsLayout.visibility = View.VISIBLE
            }
            else{
                flightInputFieldsLayout.visibility = View.VISIBLE
                simInputFieldsLayout.visibility = View.GONE
            }
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectIsSignedFlow(){
        viewModel.isSignedFlow.launchCollectWhileLifecycleStateStarted{
            signSelector.showAsActiveIf(it)
            simSignSelector.showAsActiveIf(it)

        }
    }

    private fun LayoutEditFlightFragmentBinding.collectDualInstructorFlow(){
        viewModel.dualInstructorFlow.launchCollectWhileLifecycleStateStarted{ flag ->
            dualInstructorSelector.setDualInstructorField(flag)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectIsMultiPilotFlow() {
        viewModel.isMultiPilotFlow.launchCollectWhileLifecycleStateStarted {
            multiPilotSelector.showAsActiveIf(it)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectIsIfrFlow() {
        viewModel.isIfrFlow.launchCollectWhileLifecycleStateStarted {
            ifrSelector.showAsActiveIf(it)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectPicPicusFlow() {
        viewModel.picPicusFlow.launchCollectWhileLifecycleStateStarted { flag ->
            picPicusSelector.setPicPicusField(flag)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectIsPfFlow() {
        viewModel.isPfFlow.launchCollectWhileLifecycleStateStarted {
            pfSelector.showAsActiveIf(it)
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectSimTimeFlow() {
        viewModel.simTimeFlow.launchCollectWhileLifecycleStateStarted{
            simTimeField.setText(it.minutesToHoursAndMinutesString())
        }
    }

    private fun LayoutEditFlightFragmentBinding.collectIsAutoValuesFlow() {
        viewModel.isAutoValuesFlow.launchCollectWhileLifecycleStateStarted {
            autoFillCheckBox.isChecked = it
        }
    }




    private fun LayoutEditFlightFragmentBinding.setClosingOnClickListeners() {
        //click on empty part == cancel
        editFlightFragmentBackground.setOnClickListener {
            cancelAndClose()
        }
        flightCancelButton2.setOnClickListener {
            cancelAndClose()
        }
        flightSaveButton.setOnClickListener {
            saveAndClose()
        }
    }

    private fun saveAndClose() {
        clearFocus()
        viewModel.saveAndClose()
    }
    private fun cancelAndClose() {
        viewModel.closeWithoutSaving()
    }

    private fun showWelcomeMessage() = JoozdlogAlertDialog().show(requireActivity()){
        titleResource = R.string.edit_flight_welcome_title
        messageResource = R.string.edit_flight_welcome_message
        setPositiveButton(android.R.string.ok){
            Prefs.editFlightFragmentWelcomeMessageShouldBeDisplayed(false)
        }
    }

    private fun LayoutEditFlightFragmentBinding.catchAndIgnoreClicksOnEmptyPartOfDialog() {
        flightBox.setOnClickListener { }
    }

    private fun toastAirportNotFound() {
        toast(R.string.airport_not_found_no_night_time)
    }

    private fun TextInputEditText.setAirportFieldToValidOrInvalidLayout(isValid: Boolean) {
        val drawable = if (isValid) null else ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_error_outline_20px
        )
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            null,
            null,
            drawable,
            null
        )
    }
}