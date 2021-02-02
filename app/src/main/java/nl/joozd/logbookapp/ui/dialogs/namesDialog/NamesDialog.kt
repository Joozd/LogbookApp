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

package nl.joozd.logbookapp.ui.dialogs.namesDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogNamesBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.viewmodels.dialogs.namesDialog.NamesDialogViewModel
import nl.joozd.logbookapp.ui.adapters.SelectableStringAdapter
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

//TODO if only one name selected in recyclerView, set that as active name if OK pressed
 abstract class NamesDialog: JoozdlogFragment() {
    /**
     * Set to true if working on PIC, or false if working on other names (Flight.name2)
     */
    abstract val workingOnName1: Boolean
    // If this is true, we are editing PIC name so only one name allowed
    // if null or false, will return false (null check on different places)
    /**
     * ViewModel to use for this dialog
     */
    abstract val viewModel: NamesDialogViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        DialogNamesBinding.bind(inflater.inflate(R.layout.dialog_names, container, false)).apply{
            //set color of top part
            namesDialogTopHalf.joozdLogSetBackgroundColor()

            //initialize RecyclerView
            val namesPickerAdapter = SelectableStringAdapter { name ->
                viewModel.selectName(name)
            }.also {
                namesPickerList.layoutManager = LinearLayoutManager(context)
                namesPickerList.adapter = it
            }

            //Search field changed:
            namesSearchField.onTextChanged {
                viewModel.searchNames(it)
            }

            //Buttons OnClickListeners:
            removeLastButon.setOnClickListener {
                viewModel.removeLastName()

            }

            //add name in search field to list, or replace if working on name1
            addSearchFieldNameButton.setOnClickListener {
                viewModel.addManualNameClicked()
            }

            //add selected name to list, or replace if working on name1
            addSelectedNameButton.setOnClickListener {
                viewModel.addSelectedName()

            }

            // Save/Cancel onClickListeners:
            saveTextView.setOnClickListener{
                closeFragment()
            }

            //on cancel, revert to previous flight, set viewModel.namePickerWorkingOnName1 to null and close
            cancelTextView.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }
            editAircraftLayout.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }

            //catch clicks on empty parts of this dialog
            editAircraftDialogLayout.setOnClickListener {  }

            /**
             * observers:
             */

            viewModel.addSearchFieldNameButtonTextResource.observe(viewLifecycleOwner, Observer{
                addSearchFieldNameButton.text = getString(it)
            })
            viewModel.addSelectedNameButtonTextResource.observe(viewLifecycleOwner, Observer{
                addSelectedNameButton.text = getString(it)
            })
            viewModel.removeLastButonTextResource.observe(viewLifecycleOwner, Observer{
                removeLastButon.text = getString(it)
            })

            viewModel.allNames.observe(viewLifecycleOwner, Observer {
                namesPickerAdapter.updateList(it)
            })
            viewModel.selectedName.observe(viewLifecycleOwner, Observer {
                namesPickerAdapter.selectActiveItem(it)
            })
            viewModel.currentNames.observe(viewLifecycleOwner, Observer{
                selectedNames.text = it
            })

            return root
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }
}