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

package nl.joozd.logbookapp.ui.dialogs.namesDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogNamesBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.viewmodels.dialogs.namesDialog.Name2DialogViewModel
import nl.joozd.logbookapp.ui.adapters.SelectableStringAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

class Name2Dialog: JoozdlogFragment() {
    private val viewModel: Name2DialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogNamesBinding.bind(inflater.inflate(R.layout.dialog_names, container, false)).apply{
            val namesPickerAdapter = makeNamesPickerAdapterAndAddToList()
            val selectedNamesAdapter = makeSelectedNamesAdapterAndAddToList()

            launchFlowCollectors(namesPickerAdapter, selectedNamesAdapter)
            setOnTextChangedListeners()
            setOnClickListeners()
        }.root

    private fun DialogNamesBinding.makeNamesPickerAdapterAndAddToList() =
        makeNamesPickerAdapter().also{
            namesPickerList.layoutManager = LinearLayoutManager(context)
            namesPickerList.adapter = it
        }

    private fun DialogNamesBinding.makeSelectedNamesAdapterAndAddToList() =
        makeSelectedNamesAdapter().also{
            pickedNamesList.layoutManager = LinearLayoutManager(context)
            pickedNamesList.adapter = it
        }

    private fun makeNamesPickerAdapter() = SelectableStringAdapter{ name ->
        viewModel.pickNewName(name)
    }

    private fun makeSelectedNamesAdapter() = SelectableStringAdapter{ name ->
        viewModel.pickSelectedName(name)
    }

    @Suppress("unused") // it is here because it is here in all dialogs (for uniformity)
    private fun DialogNamesBinding.launchFlowCollectors(
        namesPickerAdapter: SelectableStringAdapter,
        selectedNamesAdapter: SelectableStringAdapter
    ){
        collectPickableNamesFlow(namesPickerAdapter)
        collectSelectedNamesFlow(selectedNamesAdapter)
    }

    private fun DialogNamesBinding.setOnClickListeners(){
        addNameButton.setOnClickListener {
            viewModel.addSelectedName()
        }
        removeNameButton.setOnClickListener {
            viewModel.removeSelectedName()
        }
        addSearchFieldNameButton.setOnClickListener {
            viewModel.addQueryAsName()
            namesSearchField.setText("")
        }
        saveNamesDialogTextview.setOnClickListener {
            closeFragment()
        }
        cancelNamesDialogTextview.setOnClickListener {
            viewModel.undo()
            closeFragment()
        }

    }

    private fun DialogNamesBinding.setOnTextChangedListeners(){
        namesSearchField.onTextChanged {
            viewModel.updateQuery(it)
        }
    }

    private fun collectSelectedNamesFlow(selectedNamesAdapter: SelectableStringAdapter) {
        viewModel.currentNamesListFlow().launchCollectWhileLifecycleStateStarted {
            selectedNamesAdapter.submitList(it)
        }
    }

    private fun collectPickableNamesFlow(namesPickerAdapter: SelectableStringAdapter) {
        viewModel.pickableNamesListFlow().launchCollectWhileLifecycleStateStarted {
            namesPickerAdapter.submitList(it)
        }
    }


}