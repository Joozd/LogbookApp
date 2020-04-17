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

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.dialog_edit_aircraft.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.extensions.showAsActive
import nl.joozd.logbookapp.extensions.showAsInactive
import nl.joozd.logbookapp.ui.dialogs.popups.SimpleEditTextAlert
import nl.joozd.logbookapp.ui.utils.OnSpinnerItemSelectedListener

/**
 * Set aircraft before inflating dialog, or it won't fill in the correct thingies!
 */
class EditAircraftDialog: Fragment(){
    companion object{
        const val TAG = "EditaircraftDialog"
        val engineTypes = listOf ("Turbofan", "Piston", "Turbojet",  "Rocket", "Other")
    }
    var allAircraft: List<Aircraft> = emptyList()
    var mEngineTypesAdapter: ArrayAdapter<String>? = null
    var mManufacturerSpinnerAdapter: ArrayAdapter<String>? = null
    var mModelSpinnerAdapter: ArrayAdapter<String>? = null


    class OnSave(private val f: (ac: Aircraft) -> Unit){
        fun save (ac: Aircraft) {
            f(ac)
        }
    }
    var onSave: OnSave? = null
    fun setOnSave(f: (ac: Aircraft) -> Unit){
        onSave = OnSave(f)
    }
    class OnClose(private val f: () -> Unit){
        fun closing () {
            f()
        }
    }
    var onClose: OnClose? = null
    fun setOnClose(f: () -> Unit){
        onClose = OnClose(f)
    }
    private var thisView: View? = null
    var aircraft: Aircraft =
        Aircraft(
            1,
            "XX-XXX",
            "",
            "",
            "",
            0,
            0,
            1,
            1,
            1
        ) // working aircraft, should be overwritten by parent before inflating
    set(ac){
        field = ac
        Log.d(TAG, "filled aircraft: $ac")
    }




    /**
     * Main function of this Fragment
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisView = inflater.inflate(R.layout.dialog_edit_aircraft, container, false)
        thisView?.let { thisView ->
            (thisView.namesDialogTopHalf?.background as GradientDrawable).colorFilter = PorterDuffColorFilter(activity!!.getColorFromAttr(android.R.attr.colorPrimary), PorterDuff.Mode.SRC_IN) // set background color to bakground with rounded corners

            val models: MutableMap<String, List<String>> = allAircraft.getModelsMap().toMutableMap()
            var manufacturers = models.keys.toList()

            //set flags:
            if (aircraft.multipilot > 0) thisView.multiPilotText.showAsActive() else thisView.multiPilotText.showAsInactive()
            if (aircraft.me > 0) thisView.multiEngineText.showAsActive() else thisView.multiEngineText.showAsInactive()
            if (aircraft.isIfr > 0) thisView.ifrText.showAsActive() else thisView.ifrText   .showAsInactive()


            //fill engine types spinner (only happens once as engine types don't change)
            ArrayAdapter<String>(thisView.context,R.layout.spinner_item, engineTypes).also{
                    engineTypesAdapter ->
                engineTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                mEngineTypesAdapter = engineTypesAdapter
                thisView.engineSpinner.adapter=engineTypesAdapter
            }
            //set engine types spinner listener
            val engineTypeSpinnerListener = OnSpinnerItemSelectedListener()
            engineTypeSpinnerListener.setOnItemSelectedListener { parent, _, pos, _ ->
                parent?.getItemAtPosition(pos)?.let { engineType ->
                    aircraft = aircraft.copy(engine_type = engineType as String)
                }
            }
            thisView.engineSpinner.onItemSelectedListener = engineTypeSpinnerListener

            //fill the manufacturer and model spinners with found data
            fillSpinners(thisView, models, manufacturers)

            thisView.engineSpinner.setSelection(mEngineTypesAdapter?.getPosition(aircraft.engine_type) ?: 0)
            thisView.manufacturerSpinner.setSelection(mManufacturerSpinnerAdapter?.getPosition(aircraft.manufacturer) ?: 0)
            thisView.modelSpinner.setSelection(mModelSpinnerAdapter?.getPosition(aircraft.model) ?: 0)
            Log.d(TAG, "getPosition(aircraft.manufacturer) = ${mManufacturerSpinnerAdapter?.getPosition(aircraft.manufacturer)} ")

            Log.d(TAG, "getPosition(aircraft.model) = ${mModelSpinnerAdapter?.getPosition(aircraft.model)} ")
            Log.d(TAG, "modelSpinner has ${mModelSpinnerAdapter?.count} items")

            thisView.registrationField.onTextChanged { t ->
                Log.d(TAG, t)
                aircraft = aircraft.copy(registration = t)
                thisView.registrationField.setSelection(t.length)
            }

            thisView.registrationField.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    v.clearFocus()
                }
                false
            }

            thisView.editAircraftDialogLayout.setOnClickListener{
                //do nothing on click
            }

            //opens a dialog with an editText field to enter a new manufacturer. Will stay alive during this edit session, untill an aircraft is actually saved with it
            //MANUFACTURER:
            thisView.newManufacturerButton.setOnClickListener{
                activity?.let {ctx ->
                    val alert = SimpleEditTextAlert(ctx)
                    with (alert){
                        setTitle(getString(R.string.addManufacturer))
                        setPositiveButton(getString(android.R.string.ok)) {_, _ ->
                            if (editText.text.toString() !in manufacturers) {
                                models[editText.text.toString()] = emptyList()
                                manufacturers = models.keys.toList().sorted()
                                Log.d(TAG, "$manufacturers")
                                fillSpinners(thisView, models, manufacturers)
                                thisView.manufacturerSpinner.setSelection(mManufacturerSpinnerAdapter?.getPosition(editText.text.toString()) ?: 0)
                            }
                        }
                        setNegativeButton(getString(android.R.string.no)) { _, _ ->}
                        setHint("Manufacturer")
                        show()
                    }
                }

            }

            thisView.newModelButton.setOnClickListener{
                activity?.let {ctx ->
                    val alert = SimpleEditTextAlert(ctx)
                    with (alert){
                        setTitle(getString(R.string.addManufacturer))
                        setPositiveButton(getString(android.R.string.ok)) {_, _ ->
                            if (editText.text.toString() !in models[aircraft.manufacturer] ?: emptyList() && models[aircraft.manufacturer] != null) models[aircraft.manufacturer] = listOf(editText.text.toString()) + models[aircraft.manufacturer]!!
                            fillSpinners(thisView, models, manufacturers)
                            thisView.modelSpinner.setSelection(mModelSpinnerAdapter?.getPosition(editText.text.toString()) ?: 0)
                        }
                        setNegativeButton(getString(android.R.string.no)) { _, _ ->}
                        setHint("Manufacturer")
                        show()
                    }
                }

            }


            thisView.editAircraftLayout.setOnClickListener {
                fragmentManager?.popBackStack()
            }
            thisView.cancelTextView.setOnClickListener {
                fragmentManager?.popBackStack()
            }
            thisView.saveTextView.setOnClickListener {
                onSave?.save(aircraft)
                fragmentManager?.popBackStack()
            }

            // insert all values from flight:
            thisView.airportPickerTitle.text = aircraft.registration
            thisView.registrationField.setText(aircraft.registration)

            //toggle multipilot on clicking its textView
            thisView.multiPilotText.setOnClickListener {
                if (aircraft.multipilot == 0){
                    aircraft = aircraft.copy(multipilot = 1)
                    thisView.multiPilotText.showAsActive()
                } else {
                    aircraft = aircraft.copy(multipilot = 0)
                    thisView.multiPilotText.showAsInactive()
                }
            }
            //toggle multiEngine on clicking its textView
            thisView.multiEngineText.setOnClickListener {
                if (aircraft.me == 0){
                    aircraft = aircraft.copy(me = 1)
                    thisView.multiEngineText.showAsActive()
                } else {
                    aircraft = aircraft.copy(me = 0)
                    thisView.multiEngineText.showAsInactive()
                }
            }

            //toggle IFR on clicking its textView
            thisView.ifrText.setOnClickListener {
                if (aircraft.isIfr == 0) {
                    aircraft = aircraft.copy(isIfr = 1)
                    thisView.ifrText.showAsActive()
                } else {
                    aircraft = aircraft.copy(isIfr = 0)
                    thisView.ifrText.showAsInactive()
                }
            }


        }



        return thisView
    }

    override fun onDestroyView() {
        onClose?.closing()
        super.onDestroyView()
    }

    fun List<Aircraft>.getManufacturers(): List<String> = this.map{it.manufacturer}.distinct()
    fun List<Aircraft>.getModelsMap(): Map<String, List<String>> {
        val manufacturers = this.getManufacturers()
        val modelsMap: MutableMap<String, List<String>> = mutableMapOf()
        manufacturers.forEach{manufacturer ->
            val knownTypesOfThisManufacturer: List<String> = this.filter{ac -> ac.manufacturer == manufacturer}.map{ac -> ac.model}.distinct()
            modelsMap[manufacturer] = knownTypesOfThisManufacturer
        }
        return modelsMap
    }

    private fun fillSpinners(thisView:View, models: MutableMap<String, List<String>>, manufacturers: List<String>){
        //fill manufacturers from known aircraft
        ArrayAdapter<String>(thisView.context,R.layout.spinner_item, manufacturers).also{
                manufacturerAdapter ->
            manufacturerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mManufacturerSpinnerAdapter = manufacturerAdapter
            thisView.manufacturerSpinner.adapter=manufacturerAdapter
        }
        //fill models from known aircraft
        ArrayAdapter<String>(thisView.context,R.layout.spinner_item, models[aircraft.manufacturer] ?: emptyList()).also{ modelAdapter ->
            modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mModelSpinnerAdapter = modelAdapter
            thisView.modelSpinner.adapter = modelAdapter
            Log.d("!!!!!!!!!!!!!!!!", "found ${manufacturers.size} items")
        }


        //set spinner listeners:
        val manufacturerSpinnerListener = OnSpinnerItemSelectedListener()
        manufacturerSpinnerListener.setOnItemSelectedListener { parent, _, pos, _ ->
            parent?.getItemAtPosition(pos)?.let { manufacturer ->
                aircraft = aircraft.copy(manufacturer = manufacturer as String)


                // fill models once manufacturer is picked
                ArrayAdapter<String>(
                    thisView.context,
                    R.layout.spinner_item,
                    models[manufacturer] ?: emptyList()
                ).also { modelAdapter ->
                    modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    mModelSpinnerAdapter = modelAdapter
                    thisView.modelSpinner.adapter = modelAdapter
                    thisView.modelSpinner.setSelection(mModelSpinnerAdapter?.getPosition(aircraft.model) ?: 0)
                }
            }
        }
        val modelSpinnerListener = OnSpinnerItemSelectedListener()
        modelSpinnerListener.setOnItemSelectedListener { parent, _, pos, _ ->
            parent?.getItemAtPosition(pos)?.let { model ->
                aircraft = aircraft.copy(model = model as String)
            }
        }
        thisView.manufacturerSpinner.onItemSelectedListener = manufacturerSpinnerListener
        thisView.modelSpinner.onItemSelectedListener = modelSpinnerListener




    }


}