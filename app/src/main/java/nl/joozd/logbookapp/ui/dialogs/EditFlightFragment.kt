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
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputEditText
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.LayoutEditFlightFragmentBinding
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.EditFlightFragmentEvents
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.utils.toast

import nl.joozd.logbookapp.model.viewmodels.fragments.NewEditFlightFragmentViewModel
import nl.joozd.logbookapp.ui.adapters.AircraftAutoCompleteAdapter
import nl.joozd.logbookapp.ui.dialogs.airportPicker.DestPicker
import nl.joozd.logbookapp.ui.dialogs.airportPicker.OrigPicker
import nl.joozd.logbookapp.ui.dialogs.namesDialog.Name1Dialog
import nl.joozd.logbookapp.ui.dialogs.namesDialog.Name2Dialog
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

class EditFlightFragment: JoozdlogFragment(){
    private val viewModel: NewEditFlightFragmentViewModel by viewModels()


    /**
     * Will define all listeners etc, and set initial
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val inflatedLayout = inflater.inflate(R.layout.layout_edit_flight_fragment, container, false)
        val binding = LayoutEditFlightFragmentBinding.bind(inflatedLayout).apply {
            /*
            (flightInfoText.background as GradientDrawable).colorFilter = PorterDuffColorFilter(
                requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
                PorterDuff.Mode.SRC_IN
            ) // set background color to background with rounded corners

             */

            setAndattachAdaptersForAutocompleteFields()

            setLongPressListenersForHelpDialogs()

            setOnClickListeners()

            observeLiveData()

            setFeedbackObserver()

            if (Preferences.editFlightFragmentWelcomeMessageShouldBeDisplayed)
                showWelcomeMessage()
        }
        return binding.root
    }

    private fun LayoutEditFlightFragmentBinding.observeLiveData() {
        setObserversForEditTextContents()

        setObserversForToggleButtons()

        //Handle changed source data (airports, names, etc)
        setSourceDataObservers()
    }

    private fun LayoutEditFlightFragmentBinding.setOnClickListeners() {
        setToggleSwitchOnClickListeners()

        setDialogLaunchingOnClickListeners()

        setOnFocusChangedListeners()

        setClosingOnClickListeners()

        catchAndIgnoreClicksOnEmptyPartOfDialog()
    }

    private fun LayoutEditFlightFragmentBinding.catchAndIgnoreClicksOnEmptyPartOfDialog() {
        flightBox.setOnClickListener { }
    }

    /**
     * Functions that handle closing fragments.
     */
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

    /**
     * Close fragment, and save working flight to DB
     */
    private fun saveAndClose() {
        clearFocus()
        viewModel.saveWorkingFlightAndCloseFragment()
    }

    /**
     * Close fragment, ignore all changes
     */
    private fun cancelAndClose() {
        //If I wanted some "undo cancel" function (SnackBar?) this might be a good place to add it
        viewModel.closeWithoutSaving()
    }

    /**
     * onFocusChangedListeners to handle inputs in EditTexts
     */
    private fun LayoutEditFlightFragmentBinding.setOnFocusChangedListeners() {
        // flightDateField has an onClickListener, not an onFocusChanged as it always uses dialog

        flightFlightNumberField.setOnFocusChangeListener { _, hasFocus ->
            handleFlightNumberFocusChanged(hasFocus)
        }

        flightOrigField.setOnFocusChangeListener { _, hasFocus ->
            handleOrigFocusChanged(hasFocus)
        }

        flightDestField.setOnFocusChangeListener { _, hasFocus ->
            handleDestFocusChanged(hasFocus)
        }

        flighttOutStringField.setOnFocusChangeListener { _, hasFocus ->
            handleTimeOutFocusChanged(hasFocus)
        }

        flighttInStringField.setOnFocusChangeListener { _, hasFocus ->
            handleTimeInFocusChanged(hasFocus)
        }
        flightSimTimeField.setOnFocusChangeListener { _, hasFocus ->
            handleSimTimeFocusChanged(hasFocus)
        }

        flightAircraftField.setOnFocusChangeListener { _, hasFocus ->
            handleAircraftFocusChanged(hasFocus)
        }

        flightTakeoffLandingField.setOnFocusChangeListener { _, hasFocus ->
            handleTakeoffLandingFocusChanged(hasFocus)
        }

        flightNameField.setOnFocusChangeListener { _, hasFocus ->
            handleNameFocusChanged(hasFocus)
        }

        flightName2Field.setOnFocusChangeListener { _, hasFocus ->
            handleName2FocusChanged(hasFocus)
        }

        flightRemarksField.setOnFocusChangeListener { _, hasFocus ->
            handleRemarksFocusChanged(hasFocus)
        }
    }

    private fun LayoutEditFlightFragmentBinding.handleRemarksFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setRemarks(flightRemarksField.text.toString())
    }

    private fun LayoutEditFlightFragmentBinding.handleName2FocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setName2(flightName2Field.text.toString())
    }

    private fun LayoutEditFlightFragmentBinding.handleNameFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setName(flightNameField.text.toString())
    }

    private fun LayoutEditFlightFragmentBinding.handleTakeoffLandingFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            flightTakeoffLandingField.text?.let {
                if (it.isBlank())
                    flightTakeoffLandingField.setText(viewModel.landingsLiveData.value)
                else
                    viewModel.setTakeoffLandings(it.toString().trim())
            }
    }

    private fun LayoutEditFlightFragmentBinding.handleAircraftFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setRegAndType(flightAircraftField.text)
    }

    private fun LayoutEditFlightFragmentBinding.handleSimTimeFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setSimTime(flightSimTimeField.text)
    }

    private fun LayoutEditFlightFragmentBinding.handleTimeInFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setTimeIn(flighttInStringField.text)
    }

    private fun LayoutEditFlightFragmentBinding.handleTimeOutFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setTimeOut(flighttOutStringField.text)
    }

    private fun LayoutEditFlightFragmentBinding.handleDestFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setDest(flightDestField.text)
    }

    private fun LayoutEditFlightFragmentBinding.handleOrigFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setOrig(flightOrigField.text)
    }

    private fun LayoutEditFlightFragmentBinding.handleFlightNumberFocusChanged(
        hasFocus: Boolean
    ) {
        if (!hasFocus)
            viewModel.setFlightNumber(flightFlightNumberField.text)
        else {
            flightFlightNumberField.removeTrailingDigits()
        }
    }

    /**
     * Set OnClickListeners that launch a dialog
     * (mostly for the LSK buttons at the side of this dialog, also for flightDateField)
     */
    private fun LayoutEditFlightFragmentBinding.setDialogLaunchingOnClickListeners() {
        flightDateSelector.setOnClickListener(makeLaunchDateDialogOnClickListener())

        // flightDateField has an onClickListener, not an onFocusChanged as it always uses dialog
        flightDateField.setOnClickListener(makeLaunchDateDialogOnClickListener())

        flightFlightNumberSelector.setOnClickListener {
            toastNotImplementedYet()
        }

        //also: this might not work after rotation etc
        flightOrigSelector.setOnClickListener { showOrigPicker() }

        flightDestSelector.setOnClickListener { showDestPicker() }

        flighttOutSelector.setOnClickListener(makeLaunchTimePickerOnClickListener())

        flighttInSelector.setOnClickListener(makeLaunchTimePickerOnClickListener())

        flightAcRegSelector.setOnClickListener { launchSimOrAircraftPicker() }

        flightTakeoffLandingSelector.setOnClickListener { launchLandingsDialog() }

        flightNameSelector.setOnClickListener { launchName1Dialog() }

        flightName2Selector.setOnClickListener { launchName2Dialog() }
    }

    // Launch dialog to edit name(s) for other crew
    private fun launchName2Dialog() {
        clearFocus()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, Name2Dialog())
            addToBackStack(null)
        }
    }

    // Launch dialog to edit name for PIC
    private fun launchName1Dialog() {
        clearFocus()
        supportFragmentManager.commit {
            add(
                R.id.mainActivityLayout,
                Name1Dialog()
            )
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
        supportFragmentManager.commit {
            add(
                R.id.mainActivityLayout,
                if (viewModel.isSim) SimTypePicker() else AircraftPicker()
            )
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

    private fun toastNotImplementedYet() {
        clearFocus()
        toast("Not implemented yet!")
    }

    /**
     * get a [TimePicker] dialog which will update WorkingFlight
     */
    private fun makeLaunchTimePickerOnClickListener() = View.OnClickListener {
        clearFocus()
        // Get timePicker dialog, update flight in that dialog.
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, TimePicker())
            addToBackStack(null)
        }
    }

    /**
     * OnClickListener that launches a [LocalDatePickerDialog]
     */
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

    /**
     * Set Toggle Switches onClickListeners
     */
    private fun LayoutEditFlightFragmentBinding.setToggleSwitchOnClickListeners() {
        //Signature set or not is only changed through a SignatureDialog
        signSelector.setOnClickListener { launchSignatureDialog() }

        simSelector.setOnClickListener {
            clearFocus()
            viewModel.toggleSim()
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
            viewModel.toggleIfr()
        }

        picSelector.setOnClickListener {
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
    }

    /**
     * Clear focus for this Activity
     */
    private fun clearFocus() {
        activity?.currentFocus?.clearFocus()
    }

    /**
     * Launch a Signature dialog
     */
    private fun launchSignatureDialog() {
        clearFocus()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, SignatureDialog())
            addToBackStack(null)
        }
    }

    /**
     * Set Long-press help dialogs:
     */
    private fun LayoutEditFlightFragmentBinding.setLongPressListenersForHelpDialogs() {
        flightDateField             .setOnLongClickListener { showHelpMessage(R.string.edit_flight_date_help) }
        flightFlightNumberField     .setOnLongClickListener { showHelpMessage(R.string.edit_flight_flight_number_help) }
        flightOrigField             .setOnLongClickListener { showHelpMessage(R.string.edit_flight_orig_help) }
        flightDestField             .setOnLongClickListener { showHelpMessage(R.string.edit_flight_dest_help) }
        flighttOutStringField       .setOnLongClickListener { showHelpMessage(R.string.edit_flight_time_out_help) }
        flighttInStringField        .setOnLongClickListener { showHelpMessage(R.string.edit_flight_time_in_help) }
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
        picSelector                 .setOnLongClickListener { showHelpMessage(R.string.edit_flight_pic_help) }
        pfSelector                  .setOnLongClickListener { showHelpMessage(R.string.edit_flight_pf_help) }
        autoFillCheckBox            .setOnLongClickListener { showHelpMessage(R.string.edit_flight_autovalues_help) }
    }

    /**
     * Event handler observer
     * TODO use string resources instead of strings
     */
    private fun setFeedbackObserver() {
        viewModel.feedbackEvent.observe(viewLifecycleOwner) { event ->
            when (event.getEvent()) {
                EditFlightFragmentEvents.NOT_IMPLEMENTED -> toast("Not implemented!")
                EditFlightFragmentEvents.INVALID_REG_TYPE_STRING -> toast("Error in regType string")
                EditFlightFragmentEvents.AIRPORT_NOT_FOUND -> toast("Airport not found, no night time logged.")
                EditFlightFragmentEvents.AIRCRAFT_NOT_FOUND -> openAircraftPicker()
                EditFlightFragmentEvents.AIRPORT_NOT_FOUND_FOR_LANDINGS -> toast("airport not found, all logged as day")
                EditFlightFragmentEvents.INVALID_TIME_STRING -> toast("Error in time string, no changes")
                EditFlightFragmentEvents.INVALID_SIM_TIME_STRING -> toast("Error in time string, simTime = 0")
                EditFlightFragmentEvents.CLOSE_EDIT_FLIGHT_FRAGMENT -> closeFragment()
                EditFlightFragmentEvents.EDIT_FLIGHT_CALENDAR_CONFLICT -> showCalendarConflictDialog()
            }
        }
    }

    /**
     * If this fragment is not closing, open an [AircraftPicker] dialog
     */
    private fun openAircraftPicker() {
            supportFragmentManager.commit {
                add(R.id.mainActivityLayout, AircraftPicker())
                addToBackStack(null)
            }
    }

    /**
     * Observers for when data for adapters has been changed
     */
    private fun LayoutEditFlightFragmentBinding.setSourceDataObservers() {
        observeNames()
        observeKnownAircraftRegistrations()


        // Notify viewmodel that aircraft DB has changed. Triggered from here to keep
        // LiveData lifecycle pattern intact and prevent using observeForever in viewModel
        //TODO I don't like this
        viewModel.aircraftDbLiveData.observe(viewLifecycleOwner) {
            viewModel.notifyAircraftDbChanged()
        }

        // Notify viewmodel that aircraft DB has changed. Triggered from here to keep
        // LiveData lifecycle pattern intact and prevent using observeForever in viewModel
        //TODO I don't like this
        viewModel.airportDbLiveData.observe(viewLifecycleOwner) {
            viewModel.notifyAirportDbChanged()
        }
    }

    private fun LayoutEditFlightFragmentBinding.observeKnownAircraftRegistrations() {
        viewModel.knownRegistrationsLiveData.observe(viewLifecycleOwner) { registrations ->
            (flightAircraftField.adapter as AircraftAutoCompleteAdapter).apply {
                setItems(registrations)
            }
        }
    }

    private fun LayoutEditFlightFragmentBinding.observeNames() {
        @Suppress("UNCHECKED_CAST")
        viewModel.allNamesLiveData.observe(viewLifecycleOwner) {
            (flightNameField.adapter as ArrayAdapter<String>).apply {
                clear()
                addAll(it)
            }
            (flightName2Field.adapter as ArrayAdapter<String>).apply {
                clear()
                addAll(it)
            }
        }
    }

    /**
     * observers to show data in toggle fields
     */
    private fun LayoutEditFlightFragmentBinding.setObserversForToggleButtons() {
        viewModel.isSignedLiveData.observe(viewLifecycleOwner) { active -> signSelector.showIfActive(active) }

        //This one does a little bit more
        viewModel.isSimLiveData.observe(viewLifecycleOwner) { isSim ->
            setSimOrNormalLayout(isSim)
        }

        viewModel.dualInstructor.observe(viewLifecycleOwner) { flag ->
            setDualInstructorField(flag)
        }

        viewModel.picPicusText.observe(viewLifecycleOwner) {
            picSelector.text = it
        }

        viewModel.isMultiPilotLiveData.observe(viewLifecycleOwner) { isActive ->
            multiPilotSelector.showIfActive(isActive)
        }

        viewModel.isIfrLiveData.observe(viewLifecycleOwner) { active ->
            ifrSelector.showIfActive(active)
        }

        viewModel.isPic.observe(viewLifecycleOwner) { active ->
            picSelector.showIfActive(active)
        }

        viewModel.isPFLiveData.observe(viewLifecycleOwner) { active ->
            pfSelector.showIfActive(active)
        }

        viewModel.isAutoValuesLiveData.observe(viewLifecycleOwner) { isActive ->
            autoFillCheckBox.isChecked = isActive
        }
    }

    private fun LayoutEditFlightFragmentBinding.setSimOrNormalLayout(
        isSim: Boolean
    ) {
        if (isSim)
            makeSimLayout()
        else
            makeNormalLayout()
        simSelector.showIfActive(isSim)
    }

    /**
     * Set the contents of [LayoutEditFlightFragmentBinding.dualInstructorSelector] to the correct value
     */
    private fun LayoutEditFlightFragmentBinding.setDualInstructorField(flag: Int?) {
        dualInstructorSelector.showIfActive(flag != NewEditFlightFragmentViewModel.DUAL_INSTRUCTOR_FLAG_NONE)
        dualInstructorSelector.text = getDualInstructorStringForFlag(flag)
    }

    private fun getDualInstructorStringForFlag(flag: Int?) = when (flag) {
        NewEditFlightFragmentViewModel.DUAL_INSTRUCTOR_FLAG_DUAL -> getString(R.string.dualString)
        NewEditFlightFragmentViewModel.DUAL_INSTRUCTOR_FLAG_INSTRUCTOR -> getString(R.string.instructorString)
        else -> getString(R.string.dualInstructorString)
    }

    /**
     * observers to show data in editText fields
     */
    private fun LayoutEditFlightFragmentBinding.setObserversForEditTextContents() {
        viewModel.title.observe(viewLifecycleOwner) {
            flightInfoText.text = it
        }

        viewModel.dateStringLiveData.observe(viewLifecycleOwner) {
            flightDateField.setTextIfNotFocused(it)
        }

        viewModel.flightNumberLiveData.observe(viewLifecycleOwner) {
            flightFlightNumberField.setTextIfNotFocused(it)
        }


        viewModel.origin.observe(viewLifecycleOwner) {
            flightOrigField.setTextIfNotFocused(it)
        }


        viewModel.originIsValid.observe(viewLifecycleOwner) { isValid ->
            flightOrigField.setAirportFieldToValidOrInvalidLayout(isValid)
        }

        viewModel.destination.observe(viewLifecycleOwner) {
            flightDestField.setTextIfNotFocused(it)
        }

        viewModel.destinationIsValid.observe(viewLifecycleOwner) { isValid ->
            flightDestField.setAirportFieldToValidOrInvalidLayout(isValid)
        }

        viewModel.timeOut.observe(viewLifecycleOwner) {
            flighttOutStringField.setTextIfNotFocused(it)
        }

        viewModel.timeIn.observe(viewLifecycleOwner) {
            flighttInStringField.setTextIfNotFocused(it)
        }

        viewModel.aircraft.observe(viewLifecycleOwner) {
            flightAircraftField.setTextIfNotFocused(it)
        }

        viewModel.landingsLiveData.observe(viewLifecycleOwner) {
            flightTakeoffLandingField.setTextIfNotFocused(it)
        }

        viewModel.nameLiveData.observe(viewLifecycleOwner) {
            flightNameField.setTextIfNotFocused(it)
        }

        viewModel.name2LiveData.observe(viewLifecycleOwner) {
            flightName2Field.setTextIfNotFocused(it)
        }

        viewModel.remarksLiveData.observe(viewLifecycleOwner) {
            flightRemarksField.setTextIfNotFocused(it)
        }
        viewModel.simTimeLiveData.observe(viewLifecycleOwner) {
            flightSimTimeField.setText(minutesToHoursAndMinutesString(it))
        }
    }

    private fun EditText.setTextIfNotFocused(text: CharSequence?){
        if(!isFocused) setText(text ?: "")
    }

    /**
     * Set an airport field to valid or invalid
     * (invalid means airport not found in DB and auto values light night time cannot be calculated)
     * Will also instruct [viewModel] to check autoValues.
     */
    private fun TextInputEditText.setAirportFieldToValidOrInvalidLayout(isValid: Boolean) {
        viewModel.toggleAutovaluesSoftOffIfUnknownAirport()

        val drawable = if (isValid) null else ContextCompat.getDrawable(
            App.instance,
            R.drawable.ic_error_outline_20px
        )
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            null,
            null,
            drawable,
            null
        )
    }

    /**
     * Add adapters to fields that have an adapter (autocomplee fields)
     */
    private fun LayoutEditFlightFragmentBinding.setAndattachAdaptersForAutocompleteFields() {
        flightNameField.setAdapter(ArrayAdapter<String>(ctx, R.layout.item_custom_autocomplete))

        flightName2Field.setAdapter(ArrayAdapter<String>(ctx, R.layout.item_custom_autocomplete))

        flightAircraftField.setAdapter(AircraftAutoCompleteAdapter(requireActivity(), R.layout.item_custom_autocomplete))
    }

    /**
     * Switch layout for edit_flight View to sim
     */
    private fun LayoutEditFlightFragmentBinding.makeSimLayout() {
        flightSimTimeWrapper.visibility = View.VISIBLE
        flightAircraftWrapper.constrainTopToBottom(flightSimTimeWrapper, flightSimTimeWrapper.marginTop) // clone flightSimTimeWrapper's top margin as somehow dp to pixels seems to not work
        flighttOutSelector.constrainToCenterVertical(flightSimTimeWrapper)
        flighttInSelector.constrainToCenterVertical(flightSimTimeWrapper)
        flighttOutStringWrapper.visibility = View.GONE
        autoFillCheckBox.isChecked = false
        autoFillCheckBox.isEnabled = false
        flighttInStringWrapper.visibility=View.GONE
        flightFlightNumberWrapper.visibility=View.GONE
        flightNameWrapper.visibility=View.GONE
        dualInstructorSelector.visibility = View.GONE
        ifrSelector.visibility=View.GONE
        picSelector.visibility=View.GONE
        pfSelector.visibility=View.GONE
        flightOrigSelector.visibility=View.GONE
        flightOrigWrapper.visibility=View.GONE
        flightDestWrapper.visibility=View.GONE
        flightDestSelector.visibility=View.GONE
    }

    /**
     * Switch layout for edit_flight View to normal
     */
    private fun LayoutEditFlightFragmentBinding.makeNormalLayout() {
        flightSimTimeWrapper.visibility = View.GONE
        flighttOutStringWrapper.visibility = View.VISIBLE
        flightAircraftWrapper.constrainTopToBottom(flighttOutStringWrapper, flighttOutStringWrapper.marginTop) // clone flighttOutStringWrapper's top margin as somehow dp to pixels seems to not work
        flighttOutSelector.constrainToCenterVertical(flighttOutStringWrapper)
        flighttInSelector.constrainToCenterVertical(flighttOutStringWrapper)
        flighttInStringWrapper.visibility = View.VISIBLE
        flightFlightNumberWrapper.visibility = View.VISIBLE
        flightNameWrapper.visibility=View.VISIBLE
        dualInstructorSelector.visibility = View.VISIBLE
        ifrSelector.visibility = View.VISIBLE
        picSelector.visibility = View.VISIBLE
        pfSelector.visibility = View.VISIBLE
        flightOrigSelector.visibility = View.VISIBLE
        flightOrigWrapper.visibility = View.VISIBLE
        flightDestWrapper.visibility = View.VISIBLE
        flightDestSelector.visibility = View.VISIBLE
        flightTakeoffLandingWrapper.visibility = View.VISIBLE
        autoFillCheckBox.isChecked = viewModel.isAutoValuesLiveData.value == true
        autoFillCheckBox.isEnabled = true
    }

    private fun showHelpMessage(message: Int): Boolean{
        supportFragmentManager.commit {
            add(R.id.editFlightFragmentBackground, MessageDialog.make(message))
            addToBackStack(null)
        }
        return true
    }

    /**************************************************************************
     * Dialogs:
     **************************************************************************/

    private fun showCalendarConflictDialog() = JoozdlogAlertDialog().show(requireActivity()){
        titleResource = R.string.calendar_sync_conflict
        messageResource = R.string.calendar_sync_edited_flight
        setPositiveButton(android.R.string.ok) {
            viewModel.disableCalendarSync()
            viewModel.saveWorkingFlightAndCloseFragment()
        }
        setNegativeButton(R.string.delete_calendar_flight_until_end){
            viewModel.postponeCalendarSync()
            viewModel.saveWorkingFlightAndCloseFragment()
        }
        setNeutralButton(android.R.string.cancel){
            //do nothing and just close this dialog
        }
    }

    private fun showWelcomeMessage() = JoozdlogAlertDialog().show(requireActivity()){
        titleResource = R.string.edit_flight_welcome_title
        messageResource = R.string.edit_flight_welcome_message
        setPositiveButton(android.R.string.ok){
            Preferences.editFlightFragmentWelcomeMessageShouldBeDisplayed = false
        }
    }
}