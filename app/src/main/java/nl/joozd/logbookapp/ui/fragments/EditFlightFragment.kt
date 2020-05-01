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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.edit_flight.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.MainViewModel
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.ui.dialogs.*
import nl.joozd.logbookapp.ui.dialogs.NamesDialog
import nl.joozd.logbookapp.ui.utils.customs.CustomAutoComplete
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.model.viewmodels.EditFlightFragmentViewModel


class EditFlightFragment: JoozdlogFragment(){
    companion object{
        const val TAG = "EditFlightFragment"
    }
    val mainViewModel: MainViewModel by activityViewModels()
    val fragmentViewModel: EditFlightFragmentViewModel by viewModels()

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


            //Initialize autoCompleters for names fields
            val flightNameFieldAutoComplete = CustomAutoComplete()
            val flightName2FieldAutoComplete = CustomAutoComplete()

            //TODO observe these from viewModel
            /*
            launch {
                flightNameFieldAutoComplete.items = viewModel.allNamesDeferred.await()
                flightName2FieldAutoComplete.items = viewModel.allNamesDeferred.await()
            }
            */

            /************************************************************************************
             * observers to show data in editText fields
             ************************************************************************************/

            fragmentViewModel.date.observe(viewLifecycleOwner, Observer{
                flightDateField.setText(it)
            })

            fragmentViewModel.flightNumber.observe(viewLifecycleOwner, Observer{
                flightFlightNumberField.setText(it)
            })

            fragmentViewModel.orig.observe(viewLifecycleOwner, Observer{
                flightOrigField.setText(it)
            })

            fragmentViewModel.dest.observe(viewLifecycleOwner, Observer{
                flightDestField.setText(it)
            })

            fragmentViewModel.timeOut.observe(viewLifecycleOwner, Observer{
                flighttOutStringField.setText(it)
            })

            fragmentViewModel.timeIn.observe(viewLifecycleOwner, Observer{
                flighttInStringField.setText(it)
            })

            fragmentViewModel.regAndType.observe(viewLifecycleOwner, Observer{
                flightAircraftField.setText(it)
            })

            fragmentViewModel.takeoffLandings.observe(viewLifecycleOwner, Observer{
                flightTakeoffLandingField.setText(it)
            })

            fragmentViewModel.name.observe(viewLifecycleOwner, Observer{
                flightNameField.setText(it)
            })

            fragmentViewModel.name2.observe(viewLifecycleOwner, Observer{
                flightName2Field.setText(it)
            })

            fragmentViewModel.remarks.observe(viewLifecycleOwner, Observer{
                flightRemarksField.setText(it)
            })

            /************************************************************************************
             * observers to show data in toggle fields
             ************************************************************************************/

            fragmentViewModel.sign.observe(viewLifecycleOwner, Observer {active -> signSelector.showIfActive(active) })

            //This one does a little bit more
            fragmentViewModel.sim.observe(viewLifecycleOwner, Observer{active ->
                if (active)
                    makeSimLayout(this)
                else
                    makeNormalLayout(this)
                simSelector.showIfActive(active)
            })

            fragmentViewModel.dual.observe(viewLifecycleOwner, Observer {active -> dualSelector.showIfActive(active) })
            fragmentViewModel.instructor.observe(viewLifecycleOwner, Observer {active -> instructorSelector.showIfActive(active) })
            fragmentViewModel.picus.observe(viewLifecycleOwner, Observer {active -> picusSelector.showIfActive(active) })
            fragmentViewModel.pic.observe(viewLifecycleOwner, Observer {active -> picSelector.showIfActive(active) })
            fragmentViewModel.pf.observe(viewLifecycleOwner, Observer {active -> pfSelector.showIfActive(active) })
            fragmentViewModel.autoFill.observe(viewLifecycleOwner, Observer {active -> autoFillCheckBox.isChecked = active })

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
                fragmentViewModel.toggleSim()
            }

            dualSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                fragmentViewModel.toggleDual()
            }

            instructorSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                fragmentViewModel.toggleInstructor()
            }

            picusSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                fragmentViewModel.togglePicus()
            }

            picSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                fragmentViewModel.togglePic()
            }

            pfSelector.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                fragmentViewModel.togglePf()
            }

            autoFillCheckBox.setOnCheckedChangeListener { _, isChecked ->
                fragmentViewModel.setAutoFill(isChecked)
            }

            /*************************************************************************************
             * define reused listeners
             ************************************************************************************/

            /**
             * Get dateDialog, update flight when a date is picked
             * As times are the same, just change dates in those times
             */
            val dateOnClickListener = View.OnClickListener {
                DatePickerFragment{pickedDate ->
                    fragmentViewModel.setDate(pickedDate)
                }.show(supportFragmentManager, "datePicker")
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
                mainViewModel.workingOnOrig = true
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AirportPicker())
                    addToBackStack(null)
                }
            }
            //TODO set current dest as initial selection in dialog
            flightDestSelector.setOnClickListener {
                mainViewModel.workingOnOrig = false
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AirportPicker())
                    addToBackStack(null)
                }
            }

            flighttOutSelector.setOnClickListener (timeOnClickListener)
            flighttInSelector.setOnClickListener (timeOnClickListener)

            flightAcRegSelector.setOnClickListener {
                //TODO remake this dialog as complete aircraft editor
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AircraftPicker())
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
             * onFocusChangedListsners for for fields to handle inputs in EditTexts
             * These need to check [settingFields] to make sure they don't get stuck in a loop
             *************************************************************************************/

            // flightDateField has an onClickListener, not an onFocusChanged as it always uses dialog
            flightDateField.setOnClickListener(dateOnClickListener)

            flightFlightNumberField.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flightOrigField.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flightDestField.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flighttOutStringField.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flighttInStringField.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flightAircraftField.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flightTakeoffLandingField.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flightNameField.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flightName2Field.setOnFocusChangeListener { v, hasFocus -> toast("Not working yet!") }

            flightRemarksField.setOnFocusChangeListener { _, hasFocus ->
                toast("remarksField focus now $hasFocus")
                if (!hasFocus && !settingFields)
                    fragmentViewModel.setRemarks(flightRemarksField.text.toString())
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
                clearFocus()
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
        // v.autoFillCheckBox.isEnabled = viewModel.workingFlight.value?.autoFill ?: 0 > 0
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

    private fun TextView.showIfActive(active: Boolean){
        if (active) this.showAsActive()
        else this.showAsInactive()
    }

}