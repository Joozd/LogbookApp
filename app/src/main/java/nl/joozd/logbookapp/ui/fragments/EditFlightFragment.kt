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

import android.content.Context
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.edit_flight.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.MainViewModel
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.EditFlightFragmentEvents
import nl.joozd.logbookapp.ui.dialogs.*
import nl.joozd.logbookapp.ui.dialogs.NamesDialog
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.model.viewmodels.fragments.EditFlightFragmentViewModel


class EditFlightFragment: JoozdlogFragment(){
    companion object{
        const val TAG = "EditFlightFragment"
    }
    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: EditFlightFragmentViewModel by activityViewModels()

    /**
     * Listener class and setting s
     */
    class Listener (private val f: () ->Unit){ // oldFlight =  flight before changes, to undo if needed
        fun run(){
            f()
        }
    }

    override fun onAttach(context: Context) {
        viewModel.onStart()
        super.onAttach(context)
    }

    /**
     * Will define all listeners etc, and set initial
     */
    @ExperimentalCoroutinesApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.edit_flight, container, false).apply {
            (flightInfoText.background as GradientDrawable).colorFilter = PorterDuffColorFilter(
                requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
                PorterDuff.Mode.SRC_IN
            ) // set background color to background with rounded corners

            flightNameField.setAdapter(ArrayAdapter<String>(this.ctx, R.layout.item_custom_autocomplete))
            flightName2Field.setAdapter(ArrayAdapter<String>(this.ctx, R.layout.item_custom_autocomplete))

            /************************************************************************************
             * observers to show data in editText fields
             ************************************************************************************/

            viewModel.date.observe(viewLifecycleOwner, Observer{
                flightDateField.setTextIfNotFocused(it)
            })

            viewModel.flightNumber.observe(viewLifecycleOwner, Observer{
                Log.d("HALLOOO", "Ik ben Joozd!")
                flightFlightNumberField.setTextIfNotFocused(it)
            })

            viewModel.orig.observe(viewLifecycleOwner, Observer{
                flightOrigField.setTextIfNotFocused(it)
            })
            viewModel.origChecked.observe(viewLifecycleOwner, Observer { checked ->
                val drawable: Drawable? = when(checked){
                    true -> ContextCompat.getDrawable(App.instance, R.drawable.ic_check_circle_outline_20px)
                    false -> ContextCompat.getDrawable(App.instance, R.drawable.ic_error_outline_20px)
                    null -> null
                }
                flightOrigField.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
            })

            viewModel.dest.observe(viewLifecycleOwner, Observer{
                flightDestField.setTextIfNotFocused(it)
            })
            viewModel.destChecked.observe(viewLifecycleOwner, Observer { checked ->
                val drawable: Drawable? = when(checked){
                    true -> ContextCompat.getDrawable(App.instance, R.drawable.ic_check_circle_outline_20px)
                    false -> ContextCompat.getDrawable(App.instance, R.drawable.ic_error_outline_20px)
                    null -> null
                }
                flightDestField.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
            })

            viewModel.timeOut.observe(viewLifecycleOwner, Observer{
                if (viewModel.checkSim == false) {
                    flighttOutStringField.setTextIfNotFocused(it)
                }
            })

            viewModel.timeIn.observe(viewLifecycleOwner, Observer{
                flighttInStringField.setTextIfNotFocused(it)
            })

            viewModel.simTime.observe(viewLifecycleOwner, Observer {
                if (viewModel.checkSim == true){
                    flighttOutStringField.setTextIfNotFocused(it)
                }
            })

            viewModel.regAndType.observe(viewLifecycleOwner, Observer{
                flightAircraftField.setTextIfNotFocused(it)
            })

            viewModel.takeoffLandings.observe(viewLifecycleOwner, Observer{
                flightTakeoffLandingField.setTextIfNotFocused(it)
            })

            viewModel.name.observe(viewLifecycleOwner, Observer{
                flightNameField.setTextIfNotFocused(it)
            })

            viewModel.name2.observe(viewLifecycleOwner, Observer{
                flightName2Field.setTextIfNotFocused(it)
            })

            viewModel.remarks.observe(viewLifecycleOwner, Observer{
                flightRemarksField.setTextIfNotFocused(it)
            })

            /************************************************************************************
             * observers to show data in toggle fields
             ************************************************************************************/

            viewModel.sign.observe(viewLifecycleOwner, Observer { active -> signSelector.showIfActive(active) })

            //This one does a little bit more
            viewModel.sim.observe(viewLifecycleOwner, Observer{ active ->
                if (active)
                    makeSimLayout(this)
                else
                    makeNormalLayout(this)
                simSelector.showIfActive(active)
            })

            viewModel.dual.observe(viewLifecycleOwner, Observer { active -> dualSelector.showIfActive(active) })
            viewModel.instructor.observe(viewLifecycleOwner, Observer { active -> instructorSelector.showIfActive(active) })
            viewModel.ifr.observe(viewLifecycleOwner, Observer { active -> ifrSelector.showIfActive(active) })
            viewModel.pic.observe(viewLifecycleOwner, Observer { active -> picSelector.showIfActive(active) })
            viewModel.pf.observe(viewLifecycleOwner, Observer { active -> pfSelector.showIfActive(active) })
            viewModel.autoFill.observe(viewLifecycleOwner, Observer { active -> autoFillCheckBox.isChecked = active })

            /************************************************************************************
             * miscellaneous observers
             ************************************************************************************/

            @Suppress("UNCHECKED_CAST")
            viewModel.allNames.observe(viewLifecycleOwner, Observer{
                (flightNameField.adapter as ArrayAdapter<String>).apply {
                    clear()
                    addAll(it)
                }
                (flightName2Field.adapter as ArrayAdapter<String>).apply {
                    clear()
                    addAll(it)
                }
            })

            /************************************************************************************
             * Event handler observer
             ************************************************************************************/

            viewModel.feedbackEvent.observe(viewLifecycleOwner, Observer {
                when(it.getEvent()){
                    EditFlightFragmentEvents.NOT_IMPLEMENTED -> toast("Not implemented!")
                    EditFlightFragmentEvents.INVALID_REG_TYPE_STRING -> toast("Error in regType string")
                    EditFlightFragmentEvents.AIRPORT_NOT_FOUND_FOR_LANDINGS -> toast("airport not found, all logged as day")
                    EditFlightFragmentEvents.INVALID_TIME_STRING -> toast("Error in time string, no changes")
                    EditFlightFragmentEvents.INVALID_SIM_TIME_STRING -> toast("Error in time string, simTime = 0")
                }
            })

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
                viewModel.toggleIFR()
            }

            picSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.togglePic()
            }

            pfSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                viewModel.togglePf()
            }

            autoFillCheckBox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAutoFill(isChecked)
            }

            /*************************************************************************************
             * define reused or reassigned listeners
             ************************************************************************************/

            /**
             * Get dateDialog, update flight when a date is picked
             * As times are the same, just change dates in those times
             */
            val dateOnClickListener = View.OnClickListener {
                DatePickerFragment().show(supportFragmentManager, "datePicker")
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
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AirportPicker(orig = true))
                    addToBackStack(null)
                }
            }
            //TODO set current dest as initial selection in dialog
            flightDestSelector.setOnClickListener {
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AirportPicker(orig = false))
                    addToBackStack(null)
                }
            }

            flighttOutSelector.setOnClickListener (timeOnClickListener)
            flighttInSelector.setOnClickListener (timeOnClickListener)

            flightAcRegSelector.setOnClickListener {
                //TODO remake this dialog as complete aircraft editor
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, if (viewModel.checkSim) SimTypePicker() else AircraftPicker())
                    addToBackStack(null)
                }
            }
            flightTakeoffLandingSelector.setOnClickListener {
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, LandingsDialog())
                    addToBackStack(null)
                }
            }

            flightNameSelector.setOnClickListener {
                mainViewModel.namePickerWorkingOnName1 = true
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout,
                        NamesDialog()
                    )
                    addToBackStack(null)
                }
            }
            flightName2Selector.setOnClickListener {
                mainViewModel.namePickerWorkingOnName1 = false
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout,
                        NamesDialog()
                    )
                    addToBackStack(null)
                }
            }

            /**************************************************************************************
             * onFocusChangedListeners for for fields to handle inputs in EditTexts
             *************************************************************************************/

            // flightDateField has an onClickListener, not an onFocusChanged as it always uses dialog
            flightDateField.setOnClickListener(dateOnClickListener)

            flightFlightNumberField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setFlightNumber(flightFlightNumberField.text.toString())
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
                    viewModel.setTimeOut(flighttOutStringField.text.toString())
            }

            flighttInStringField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    viewModel.setTimeIn(flighttInStringField.text.toString())
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

            //on cancel, close without calling onSaveListener
            flightCancelButton2.setOnClickListener {
                //TODO fire some "undo cancel" SnackBar?
                closeFragment()
            }

            flightSaveButton.setOnClickListener {
                viewModel.save()

                clearFocus()
                closeFragment()
            }
        } // end of layoutInflater.apply()
    } // end of onCreateView


    override fun onDetach() {
        viewModel.onClosingFragment()
        super.onDetach()
    }

    /**************************************************************************
     * private worker functions:
     **************************************************************************/

    private fun isSimLayout(v: View): Boolean = v.ifrSelector.visibility == View.GONE

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
        v.ifrSelector.visibility=View.GONE
        v.picSelector.visibility=View.GONE
        v.pfSelector.visibility=View.GONE
        v.flightOrigSelector.visibility=View.GONE
        v.flightOrigWrapper.visibility=View.GONE
        v.flightDestWrapper.visibility=View.GONE
        v.flightDestSelector.visibility=View.GONE
        //v.flightTakeoffLandingWrapper.visibility=View.GONE
        //v.flightTakeoffLandingSelector.isEnabled=false
        viewModel.simTime.value?.let{
            v.flighttOutStringField.setText(it)
        }
    }

    /**
     * Switch layout for edit_flight View to normal
     * @param v: View to change layout on
     */
    private fun makeNormalLayout(v: View) {
        // v.autoFillCheckBox.isEnabled = viewModel.workingFlight.value?.autoFill ?: 0 > 0
        v.flighttOutStringWrapper.hint = getString(R.string.timeOut)
        v.flighttOutStringField.hint = getString(R.string.timeOut)
        v.flighttInStringWrapper.visibility = View.VISIBLE
        v.flightFlightNumberWrapper.visibility = View.VISIBLE
        v.dualSelector.visibility = View.VISIBLE
        v.instructorSelector.visibility = View.VISIBLE
        v.ifrSelector.visibility = View.VISIBLE
        v.picSelector.visibility = View.VISIBLE
        v.pfSelector.visibility = View.VISIBLE
        v.flightOrigSelector.visibility = View.VISIBLE
        v.flightOrigWrapper.visibility = View.VISIBLE
        v.flightDestWrapper.visibility = View.VISIBLE
        v.flightDestSelector.visibility = View.VISIBLE
        v.flightTakeoffLandingWrapper.visibility = View.VISIBLE
        v.flightTakeoffLandingSelector.isEnabled = true
        viewModel.timeOut.value?.let {
            v.flighttOutStringField.setText(it)
        }
    }

}