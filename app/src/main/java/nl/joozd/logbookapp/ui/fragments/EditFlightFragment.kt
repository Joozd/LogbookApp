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

package nl.joozd.logbookapp.ui.fragments

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import kotlinx.android.synthetic.main.edit_flight.view.*
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.ui.dialogs.*
import nl.joozd.logbookapp.ui.dialogs.NamesDialog
import nl.joozd.logbookapp.ui.utils.CustomAutoComplete
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast
import java.time.ZoneOffset


class EditFlightFragment: JoozdlogFragment(){
    companion object{
        const val TAG = "EditFlightFragment"
    }

    //This will be set to "true" when updating data in fields. When this is "true", onTextChanged etc should not trigger.
    private var settingFields = false

    /**
     * Listener class and setting s
     */
    class Listener (private val f: () ->Unit){ // oldFlight =  flight before changes, to undo if needed
        fun run(){
            f()
        }
    }
    private var onSaveListener: Listener? = null
    private var onCloseListener: Listener? = null

    fun setOnSaveListener(f: () -> Unit){
        onSaveListener = Listener(f)
    }
    fun setOnCloseListener(f: () -> Unit){
        onCloseListener = Listener(f)
    }


    /************************************************************************
     *  OnCreateView and other overrides below here                         *
     ************************************************************************/


    /**
     * Will define all listeners etc, and set initial
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.edit_flight, container, false).apply {
            (flightInfoText.background as GradientDrawable).colorFilter = PorterDuffColorFilter(
                requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
                PorterDuff.Mode.SRC_IN
            ) // set background color to background with rounded corners

            // Initially set Views' contents
            setViews(this, flight)

            //Initialize autoCompleters for names fields
            val flightNameFieldAutoComplete = CustomAutoComplete(defaultItems = viewModel.allNames)
            val flightName2FieldAutoComplete = CustomAutoComplete(defaultItems = viewModel.allNames)

            //Fill autocomplete fields with names as soon as they are available, in case they weren't yet
            launch {
                flightNameFieldAutoComplete.items = viewModel.allNamesDeferred.await()
                flightName2FieldAutoComplete.items = viewModel.allNamesDeferred.await()
            }


            /************************************************************************************
             * Toggle switches onClickListeners
             ************************************************************************************/
            pfSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                flight =
                    flight.copy(isPF = if (flight.pf) 0 else flight.duration.toMinutes().toInt())
            }
            dualSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                flight = flight.copy(
                    isDual = if (flight.dual) 0 else flight.duration.toMinutes().toInt()
                )
            }
            instructorSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                flight = flight.copy(
                    isInstructor = if (flight.instructor) 0 else flight.duration.toMinutes().toInt()
                )
            }
            picusSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                flight = flight.copy(
                    isPICUS = if (flight.picus) 0 else flight.duration.toMinutes().toInt()
                )
            }

            picSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                flight =
                    flight.copy(isPIC = if (flight.pic) 0 else flight.duration.toMinutes().toInt())
            }
            simSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                if (!flight.sim) makeSimLayout(this) else makeNormalLayout(this)
                flight = flight.copy(
                    isSim = if (flight.sim) 0 else 1,
                    simTime = if (flight.sim) 0 else 210
                )
            }
            //TODO this won't work on rotation
            signSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, SignatureDialog())
                    addToBackStack(null)
                }
            }
            autoFillCheckBox.setOnCheckedChangeListener { _, isChecked ->
                flight = flight.copy(autoFill = isChecked.toInt())
            }

            /*************************************************************************************
             * define reused listeners
             ************************************************************************************/

            /**
             * Get dateDialog, update flight when a date is picked
             * As times are the same, just change dates in those times
             */
            val dateOnClickListener = View.OnClickListener {
                val datePickerFragment= DatePickerFragment()
                datePickerFragment.setOnDatePickedListener { pickedDate ->
                    val tOut = flight.tOut.atDate(pickedDate)
                    val tInToCheck = flight.tIn.atDate(pickedDate)
                    val tIn = if (tInToCheck < tOut)  tInToCheck.plusDays(1) else tInToCheck
                    flight = flight.copy(
                        timeOut = tOut.toInstant(ZoneOffset.UTC).epochSecond,
                        timeIn = tIn.toInstant(ZoneOffset.UTC).epochSecond
                    )
                }
                datePickerFragment.show(requireActivity().supportFragmentManager, "datePicker")
            }

            /**
             * get a [TimePicker] dialog which will update through viewModel
             */
            val timeOnClickListener = View.OnClickListener {
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
                toast("Not implemented yet!")
            }

            //TODO set current orig as initial selection in dialog
            //also: this might not work after rotation etc
            flightOrigSelector.setOnClickListener {
                viewModel.workingOnOrig = true
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AirportPicker())
                    addToBackStack(null)
                }
            }
            //TODO set current dest as initial selection in dialog
            flightDestSelector.setOnClickListener {
                viewModel.workingOnOrig = false
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AirportPicker())
                    addToBackStack(null)
                }
            }

            flighttOutSelector.setOnClickListener (timeOnClickListener)
            flighttInSelector.setOnClickListener (timeOnClickListener)

            flightAcRegSelector.setOnClickListener {
                //TODO remake this dialog as complete aircraft editor
                toast("Not implemented yet!")
            }
            flightTakeoffLandingSelector.setOnClickListener {
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, LandingsDialog())
                    addToBackStack(null)
                }
            }

            flightNameSelector.setOnClickListener {
                viewModel.namePickerWorkingOnName1 = true
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout,
                        NamesDialog()
                    )
                    addToBackStack(null)
                }
            }
            flightName2Selector.setOnClickListener {
                viewModel.namePickerWorkingOnName1 = false
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout,
                        NamesDialog()
                    )
                    addToBackStack(null)
                }
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
                onCloseListener?.run()
                closeFragment()
            }

            //on cancel, close without calling onSaveListener
            flightCancelButton2.setOnClickListener {
                //TODO fire some "undo cancel" SnackBar?
                onCloseListener?.run()
                closeFragment()
            }

            flightSaveButton.setOnClickListener {
                onSaveListener?.run()
                onCloseListener?.run()
                closeFragment()
            }

        } // end of layoutInflater.apply()
    } // end of onCreateView


    /**************************************************************************
     * private worker functions:
     **************************************************************************/

    /**
     * setViews will set fields according to Flight
     * @param v: View to set values on (can be null)
     * @param f: Flight containing the values to set
     * @see JoozdlogFragment for how this triggers
     */
    override fun setViews(v: View?, f: Flight){
        v?.let { notNullView -> // don't do anything if view == null
            settingFields = true

            // Set editText fields
            notNullView.flightDateField.setText(f.date)
            notNullView.flightFlightNumberField.setText(f.flightNumber)
            notNullView.flightOrigField.setText(
                if (Preferences.useIataAirports) viewModel.icaoToIataMap[f.orig]?.nullIfEmpty()
                    ?: f.orig else f.orig
            )
            notNullView.flightDestField.setText(
                if (Preferences.useIataAirports) viewModel.icaoToIataMap[f.dest]?.nullIfEmpty()
                    ?: f.dest else f.dest
            )
            notNullView.flighttOutStringField.setText(if (f.sim) "${f.simTime / 60}:${f.simTime % 60}" else f.timeOutString)
            notNullView.flighttInStringField.setText(f.timeInString)
            notNullView.flightAircraftField.setText(if (!f.sim) ("${f.registration}(${f.aircraft})") else f.aircraft)
            notNullView.flightTakeoffLandingField.setText((f.landingDay + f.landingNight).toString())
            notNullView.flightNameField.setText(f.name)
            notNullView.flightName2Field.setText(f.name2)
            notNullView.flightRemarksField.setText(f.remarks)

            //set toggle buttons
            if (f.sim) notNullView.simSelector.showAsActive() else notNullView.simSelector.showAsInactive()
            if (f.dual) notNullView.dualSelector.showAsActive() else notNullView.dualSelector.showAsInactive()
            if (f.instructor) notNullView.instructorSelector.showAsActive() else notNullView.instructorSelector.showAsInactive()
            if (f.picus) notNullView.picusSelector.showAsActive() else notNullView.picusSelector.showAsInactive()
            if (f.pic) notNullView.picSelector.showAsActive() else notNullView.picSelector.showAsInactive()
            if (f.pf) notNullView.pfSelector.showAsActive() else notNullView.pfSelector.showAsInactive()
            if (f.signature.isNotEmpty()) notNullView.signSelector.showAsActive() else notNullView.signSelector.showAsInactive()

            notNullView.autoFillCheckBox.isChecked = f.autoFill > 0

            settingFields = false
        } ?: Log.w(this::class.simpleName, "Trying to set fields on null view. (warning 1)")
    }

    /**
     * Switch layout for edit_flight View to sim
     * @param v: View to change layout on
     */
    private fun makeSimLayout(v: View){
        v.flighttOutStringWrapper.hint=getString(R.string.simtTime)
        v.flighttOutStringField.hint=getString(R.string.simtTime)
        v.autoFillCheckBox.isChecked = false
        v.autoFillCheckBox.isEnabled = false
        v.flighttInStringWrapper.visibility=View.GONE
        v.flightFlightNumberWrapper.visibility=View.GONE
        v.dualSelector.visibility=View.GONE
        v.instructorSelector.visibility=View.GONE
        v.picusSelector.visibility=View.GONE
        v.picSelector.visibility=View.GONE
        v.pfSelector.visibility=View.GONE
        v.flightOrigSelector.visibility=View.GONE
        v.flightOrigWrapper.visibility=View.GONE
        v.flightDestWrapper.visibility=View.GONE
        v.flightDestSelector.visibility=View.GONE
        v.flightTakeoffLandingWrapper.visibility=View.GONE
        v.flightTakeoffLandingSelector.isEnabled=false
    }

    /**
     * Switch layout for edit_flight View to normal
     * @param v: View to change layout on
     */
    private fun makeNormalLayout(v: View) {
        v.autoFillCheckBox.isEnabled = viewModel.workingFlight.value?.autoFill ?: 0 > 0
        v.flighttOutStringWrapper.hint = getString(R.string.timeOut)
        v.flighttOutStringField.hint = getString(R.string.timeOut)
        v.flighttInStringWrapper.visibility = View.VISIBLE
        v.flightFlightNumberWrapper.visibility = View.VISIBLE
        v.dualSelector.visibility = View.VISIBLE
        v.instructorSelector.visibility = View.VISIBLE
        v.picusSelector.visibility = View.VISIBLE
        v.picSelector.visibility = View.VISIBLE
        v.pfSelector.visibility = View.VISIBLE
        v.flightOrigSelector.visibility=View.VISIBLE
        v.flightOrigWrapper.visibility=View.VISIBLE
        v.flightDestWrapper.visibility=View.VISIBLE
        v.flightDestSelector.visibility=View.VISIBLE
        v.flightTakeoffLandingWrapper.visibility=View.VISIBLE
        v.flightTakeoffLandingSelector.isEnabled=true
    }


}