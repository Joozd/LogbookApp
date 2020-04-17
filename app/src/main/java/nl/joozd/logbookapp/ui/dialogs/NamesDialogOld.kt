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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.dialog_names.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.NamesPickeradapterOld

@Deprecated ("Needs new")
class NamesDialogOld: Fragment() {
    class NamesSelectedListener(private val f: (names: String) -> Unit){
        fun namesSelected(names: String){
            f(names)
        }
    }
    var onSelectListener: NamesSelectedListener? = null
    // var namesWorker: NamesWorker? = null        // to be filled before inflating with an initialized NamesWorker
    var selectOnlyOne=false
    var selectedName = ""
    var previousNames = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.dialog_names, container, false)

        val namesPickerList: RecyclerView = view.findViewById(R.id.namesPickerList)
        val selectedNames = view.selectedNames
        val saveNamesDialog: TextView = view.findViewById(R.id.saveTextView)
        val cancelNamesDialog: TextView = view.findViewById(R.id.cancelTextView)
        val namesSearchField: EditText = view.findViewById(R.id.namesSearchField)
        val addSearchFieldNameButton: Button = view.findViewById(R.id.addSearchFieldNameButton)
        val addSelectedNameButton: Button = view.findViewById(R.id.addSelectedNameButton)
        val removeLastButon: Button = view.findViewById(R.id.removeLastButon)

        selectedNames.text = previousNames.replace(", ", "\n")

        view.editAircraftLayout.setOnClickListener { fragmentManager?.popBackStack() }
        view.editAircraftDialogLayout.setOnClickListener {  }

        if (selectOnlyOne){
            addSearchFieldNameButton.text = getString(R.string.select)
            addSelectedNameButton.text = getString(R.string.select)
            removeLastButon.text = getString(R.string.clear)
        }

        //set color of top part
        val namesDialogTopHalf: ConstraintLayout = view.findViewById(R.id.namesDialogTopHalf)
        (namesDialogTopHalf.background as GradientDrawable).colorFilter = PorterDuffColorFilter(requireActivity().getColorFromAttr(android.R.attr.colorPrimary), PorterDuff.Mode.SRC_IN)

        val namesPickerAdapter = NamesPickeradapterOld { name -> selectedName = name }  // yes this will crash if no namesworker inserted
        namesPickerList.layoutManager = LinearLayoutManager(context)
        namesPickerList.adapter = namesPickerAdapter


        //Search field changed:
        namesSearchField.onTextChanged {
            namesPickerAdapter.getNames(namesSearchField.text.toString())
        }

        //Buttons OnClickListeners:
        removeLastButon.setOnClickListener {
            if ("\n" in selectedNames.text.toString()){
                selectedNames.text = selectedNames.text.toString().split("\n").dropLast(1).joinToString(separator="\n")
            }
            else selectedNames.text = ""
        }

        addSearchFieldNameButton.setOnClickListener {
            selectedNames.text = if (selectOnlyOne) namesSearchField.text.toString() else listOf(selectedNames.text.toString(), namesSearchField.text.toString()).filter{ x -> x.isNotEmpty()}.joinToString(separator="\n")
            namesSearchField.setText("")
        }

        addSelectedNameButton.setOnClickListener {
            selectedNames.text = if (selectOnlyOne) selectedName else listOf(selectedNames.text.toString(), selectedName).filter{ x -> x.isNotEmpty()}.joinToString(separator="\n")
        }

        // Save/Cancel onClickListeners:
        saveNamesDialog.setOnClickListener{
            onSelectListener?.namesSelected(selectedNames.text.toString().replace("\n",", "))
            activity?.currentFocus?.clearFocus()
            fragmentManager?.popBackStack()
        }

        cancelNamesDialog.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            fragmentManager?.popBackStack()
        }

        return view
    }
}