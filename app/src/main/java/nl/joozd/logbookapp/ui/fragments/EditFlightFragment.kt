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

package nl.joozd.logbookapp.ui.fragments

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.LayoutEditFlightFragmentBinding
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.EditFlightFragmentEvents
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.dialogs.*
import nl.joozd.logbookapp.ui.utils.toast

import nl.joozd.logbookapp.model.viewmodels.fragments.NewEditFlightFragmentViewModel
import nl.joozd.logbookapp.ui.adapters.AircraftAutoCompleteAdapter
import nl.joozd.logbookapp.ui.dialogs.airportPicker.DestPicker
import nl.joozd.logbookapp.ui.dialogs.airportPicker.OrigPicker
import nl.joozd.logbookapp.ui.dialogs.namesDialog.Name1Dialog
import nl.joozd.logbookapp.ui.dialogs.namesDialog.Name2Dialog


class EditFlightFragment: JoozdlogFragment(){
    private val viewModel: NewEditFlightFragmentViewModel by viewModels()

    /**
     * Will define all listeners etc, and set initial
     */
    @ExperimentalCoroutinesApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        with (LayoutEditFlightFragmentBinding.bind(inflater.inflate(R.layout.layout_edit_flight_fragment, container, false))) {
            (flightInfoText.background as GradientDrawable).colorFilter = PorterDuffColorFilter(
                requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
                PorterDuff.Mode.SRC_IN
            ) // set background color to background with rounded corners

            flightNameField.setAdapter(ArrayAdapter<String>(ctx, R.layout.item_custom_autocomplete))
            flightName2Field.setAdapter(ArrayAdapter<String>(ctx, R.layout.item_custom_autocomplete))
            val aircraftFieldAdapter = AircraftAutoCompleteAdapter(requireActivity(), R.layout.item_custom_autocomplete)
            flightAircraftField.setAdapter(aircraftFieldAdapter)

            /************************************************************************************
             * observers to show data in editText fields
             ************************************************************************************/

            viewModel.date.observe(viewLifecycleOwner, {
                flightDateField.setTextIfNotFocused(it)
            })

            viewModel.flightNumber.observe(viewLifecycleOwner, {
                flightFlightNumberField.setTextIfNotFocused(it)
            })


            viewModel.origin.observe(viewLifecycleOwner) {
                flightOrigField.setTextIfNotFocused(it)
            }

            viewModel.originIsValid.observe(viewLifecycleOwner) { checked ->
                val drawable: Drawable? = when(checked){
                    true -> ContextCompat.getDrawable(App.instance, R.drawable.ic_check_circle_outline_20px)
                    false -> ContextCompat.getDrawable(App.instance, R.drawable.ic_error_outline_20px)
                    null -> null
                }
                flightOrigField.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
            }

            viewModel.destination.observe(viewLifecycleOwner) {
                flightDestField.setTextIfNotFocused(it)
            }
            viewModel.destinationIsValid.observe(viewLifecycleOwner) { checked ->
                val drawable: Drawable? = when(checked){
                    true -> ContextCompat.getDrawable(App.instance, R.drawable.ic_check_circle_outline_20px)
                    false -> ContextCompat.getDrawable(App.instance, R.drawable.ic_error_outline_20px)
                    null -> null
                }
                flightDestField.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
            }

            viewModel.timeOut.observe(viewLifecycleOwner) {
                // removed unnessecary sim check as setting an invisible fiedl doesn't do much bad
                flighttOutStringField.setTextIfNotFocused(it)
            }

            viewModel.timeIn.observe(viewLifecycleOwner, {
                flighttInStringField.setTextIfNotFocused(it)
            })

            viewModel.aircraft.observe(viewLifecycleOwner) {
                flightAircraftField.setTextIfNotFocused(it)
            }

            viewModel.landings.observe(viewLifecycleOwner) {
                flightTakeoffLandingField.setTextIfNotFocused(it)
            }

            viewModel.name.observe(viewLifecycleOwner) {
                flightNameField.setTextIfNotFocused(it)
            }

            viewModel.name2.observe(viewLifecycleOwner) {
                flightName2Field.setTextIfNotFocused(it)
            }

            viewModel.remarks.observe(viewLifecycleOwner) {
                flightRemarksField.setTextIfNotFocused(it)
            }
            viewModel.simTime.observe(viewLifecycleOwner) {
                flightSimTimeField.setText(minutesToHoursAndMinutesString(it))
            }

            /************************************************************************************
             * observers to show data in toggle fields
             ************************************************************************************/

            viewModel.isSigned.observe(viewLifecycleOwner) { active -> signSelector.showIfActive(active) }

            //This one does a little bit more
            viewModel.isSim.observe(viewLifecycleOwner) { active ->
                if (active)
                    makeSimLayout()
                else
                    makeNormalLayout()
                simSelector.showIfActive(active)
            }

            viewModel.isDual.observe(viewLifecycleOwner) { active -> dualSelector.showIfActive(active) }
            viewModel.isInstructor.observe(viewLifecycleOwner, { active -> instructorSelector.showIfActive(active) })
            viewModel.isIfr.observe(viewLifecycleOwner) { active -> ifrSelector.showIfActive(active) }
            viewModel.isPic.observe(viewLifecycleOwner) { active -> picSelector.showIfActive(active) }
            viewModel.isPF.observe(viewLifecycleOwner) { active -> pfSelector.showIfActive(active) }
            viewModel.isAutoValues.observe(viewLifecycleOwner) { active ->
                autoFillCheckBox.isChecked = active
            }

            /************************************************************************************
             * miscellaneous observers
             ************************************************************************************/

            @Suppress("UNCHECKED_CAST")
            viewModel.allNames.observe(viewLifecycleOwner) {
                (flightNameField.adapter as ArrayAdapter<String>).apply {
                    clear()
                    addAll(it)
                }
                (flightName2Field.adapter as ArrayAdapter<String>).apply {
                    clear()
                    addAll(it)
                }
            }
            viewModel.knownRegistrations.observe(viewLifecycleOwner) { registrations ->
                aircraftFieldAdapter.setItems(registrations)
            }

            /************************************************************************************
             * Event handler observer
             ************************************************************************************/

            //TODO make this Resource strings
            viewModel.feedbackEvent.observe(viewLifecycleOwner) {event ->
                when (event.getEvent()) {
                    EditFlightFragmentEvents.NOT_IMPLEMENTED -> toast("Not implemented!")
                    EditFlightFragmentEvents.INVALID_REG_TYPE_STRING -> toast("Error in regType string")
                    EditFlightFragmentEvents.AIRPORT_NOT_FOUND -> toast("Airport not found, no night time logged.")
                    EditFlightFragmentEvents.AIRCRAFT_NOT_FOUND -> {
                        supportFragmentManager.commit{
                            add(R.id.mainActivityLayout, AircraftPicker().apply{
                                presetEnteredRegistration = event.getString()
                            })
                            addToBackStack(null)
                        }
                        // toast("TODO (open aircraft picker) [${event.getString()}]")
                    }
                    EditFlightFragmentEvents.AIRPORT_NOT_FOUND_FOR_LANDINGS -> toast("airport not found, all logged as day")
                    EditFlightFragmentEvents.INVALID_TIME_STRING -> toast("Error in time string, no changes")
                    EditFlightFragmentEvents.INVALID_SIM_TIME_STRING -> toast("Error in time string, simTime = 0")
                    EditFlightFragmentEvents.CLOSE_EDIT_FLIGHT_FRAGMENT -> closeFragment()
                }
                Unit
            }

            /************************************************************************************
             * Toggle switches onClickListeners
             ************************************************************************************/

            //This one only does work though dialog
            signSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, SignatureDialog())
                    addToBackStack(null)
                }
            }

            simSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.toggleSim()
            }

            dualSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.toggleDual()
            }

            instructorSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.toggleInstructor()
            }

            ifrSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.toggleIfr()
            }

            picSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.togglePic()
            }

            pfSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.togglePF()
            }

            autoFillCheckBox.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.toggleAutoValues()
            }

            /*************************************************************************************
             * define reused or reassigned listeners
             ************************************************************************************/

            /**
             * Get dateDialog, update flight when a date is picked
             * As times are the same, just change dates in those times
             */
            val dateOnClickListener = View.OnClickListener {
                activity?.currentFocus?.clearFocus()
                DatePickerFragment().show(supportFragmentManager, "datePicker")
            }

            /**
             * get a [TimePicker] dialog which will update through viewModel
             */
            val timeOnClickListener = View.OnClickListener {
                activity?.currentFocus?.clearFocus()
                // Get timePicker dialog, update flight in that dialog.
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, TimePicker())
                    addToBackStack(null)
                }
            }

            /**************************************************************************************
             * onClickListeners for selectors (the triangle thingies on side of this dialog)
             *************************************************************************************/

            flightDateSelector.setOnClickListener(dateOnClickListener)

            flightFlightNumberSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                toast("Not implemented yet!")
            }

            //TODO set current orig as initial selection in dialog
            //also: this might not work after rotation etc
            flightOrigSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                supportFragmentManager.commit {
                    //It's okay to have a parameter in this fragment constructor (see [AirportPicker])
                    add(R.id.mainActivityLayout, OrigPicker())
                    addToBackStack(null)
                }
            }
            //TODO set current dest as initial selection in dialog
            flightDestSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, DestPicker())
                    addToBackStack(null)
                }
            }

            flighttOutSelector.setOnClickListener (timeOnClickListener)
            flighttInSelector.setOnClickListener (timeOnClickListener)

            flightAcRegSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                //TODO remake this dialog as complete aircraft editor
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, if (viewModel.sim) SimTypePicker() else AircraftPicker())
                    addToBackStack(null)
                }
            }
            flightTakeoffLandingSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, LandingsDialog())
                    addToBackStack(null)
                }
            }

            flightNameSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout,
                        Name1Dialog()
                    )
                    addToBackStack(null)
                }
            }
            flightName2Selector.setOnClickListener {
                Log.d("DEBUG", "xXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxX 1 1 1 1 1 1")
                activity?.currentFocus?.clearFocus()
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout,
                        Name2Dialog().also{
                            Log.d("DEBUG", "xXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxX 2 2 2 2 2 2")
                        }
                    )
                    addToBackStack(null)
                }
                Log.d("DEBUG", "xXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxX 3 3 3 3 3 3")
            }

            /**************************************************************************************
             * onFocusChangedListeners for for fields to handle inputs in EditTexts
             *************************************************************************************/

            // flightDateField has an onClickListener, not an onFocusChanged as it always uses dialog
            flightDateField.setOnClickListener(dateOnClickListener)

            flightFlightNumberField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setFlightNumber(flightFlightNumberField.text)
                else {
                    flightFlightNumberField.removeTrailingDigits()
                }
            }

            flightOrigField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setOrig(flightOrigField.text.toString())
            }

            flightDestField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setDest(flightDestField.text.toString())
            }

            flighttOutStringField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setTimeOut(flighttOutStringField.text)
            }

            flighttInStringField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setTimeIn(flighttInStringField.text)
            }
            flightSimTimeField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setSimTime(flightSimTimeField.text)
            }

            flightAircraftField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setRegAndType(flightAircraftField.text.toString())
            }

            flightTakeoffLandingField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setTakeoffLandings(flightTakeoffLandingField.text.toString())
            }

            flightNameField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setName(flightNameField.text.toString())
            }
            flightName2Field.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setName2(flightName2Field.text.toString())
            }
            flightRemarksField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setRemarks(flightRemarksField.text.toString())
            }





            //ignore clicks on empty parts of dialog
            flightBox.setOnClickListener {  }

            /**
             * Functions that handle closing fragments.
             * Always call onCloseListener?.run()
             */
            //click on empty part == cancel
            flightInfoLayout.setOnClickListener {
                //TODO fire some "undo cancel" SnackBar?
                closeFragment()
            }

            flightCancelButton2.setOnClickListener {
                //TODO fire some "undo cancel" SnackBar?
                closeFragment()
            }

            flightSaveButton.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.saveAndClose()
            }
            return root
        } // end of binding.apply()

    } // end of onCreateView

    /**************************************************************************
     * private worker functions:
     **************************************************************************/

    // private fun LayoutEditFlightFragmentBinding.isSimLayout(): Boolean = ifrSelector.visibility == View.GONE

    /**
     * Switch layout for edit_flight View to sim
     * @param binding: LayoutEditFlightFragmentBinding to change layout on
     */
    private fun LayoutEditFlightFragmentBinding.makeSimLayout() {
        //flighttOutStringWrapper.hint=getString(R.string.simtTime)
        //flighttOutStringField.hint=getString(R.string.simtTime)
        flightSimTimeWrapper.visibility = View.VISIBLE
        flightAircraftWrapper.constrainTopToBottom(flightSimTimeWrapper, "4dp")
        flighttOutSelector.constrainToCenterVertical(flightSimTimeWrapper)
        flighttInSelector.constrainToCenterVertical(flightSimTimeWrapper)
        flighttOutStringWrapper.visibility = View.GONE
        autoFillCheckBox.isChecked = false
        autoFillCheckBox.isEnabled = false
        flighttInStringWrapper.visibility=View.GONE
        flightFlightNumberWrapper.visibility=View.GONE
        dualSelector.visibility=View.GONE
        instructorSelector.visibility=View.GONE
        ifrSelector.visibility=View.GONE
        picSelector.visibility=View.GONE
        pfSelector.visibility=View.GONE
        flightOrigSelector.visibility=View.GONE
        flightOrigWrapper.visibility=View.GONE
        flightDestWrapper.visibility=View.GONE
        flightDestSelector.visibility=View.GONE
        //v.flightTakeoffLandingWrapper.visibility=View.GONE
        //v.flightTakeoffLandingSelector.isEnabled=false
    }

    /**
     * Switch layout for edit_flight View to normal
     * @param binding: LayoutEditFlightFragmentBinding to change layout on
     */
    private fun LayoutEditFlightFragmentBinding.makeNormalLayout() {
        // v.autoFillCheckBox.isEnabled = viewModel.workingFlight.value?.autoFill ?: 0 > 0
        flightSimTimeWrapper.visibility = View.GONE
        flighttOutStringWrapper.visibility = View.VISIBLE
        flightAircraftWrapper.constrainTopToBottom(flighttOutStringWrapper, "4dp")
        flighttOutSelector.constrainToCenterVertical(flighttOutStringWrapper)
        flighttInSelector.constrainToCenterVertical(flighttOutStringWrapper)
        flighttInStringWrapper.visibility = View.VISIBLE
        flightFlightNumberWrapper.visibility = View.VISIBLE
        dualSelector.visibility = View.VISIBLE
        instructorSelector.visibility = View.VISIBLE
        ifrSelector.visibility = View.VISIBLE
        picSelector.visibility = View.VISIBLE
        pfSelector.visibility = View.VISIBLE
        flightOrigSelector.visibility = View.VISIBLE
        flightOrigWrapper.visibility = View.VISIBLE
        flightDestWrapper.visibility = View.VISIBLE
        flightDestSelector.visibility = View.VISIBLE
        flightTakeoffLandingWrapper.visibility = View.VISIBLE
        //flightTakeoffLandingSelector.isEnabled = true
        autoFillCheckBox.isChecked = viewModel.isAutoValues.value == true
        autoFillCheckBox.isEnabled = true
    }



}