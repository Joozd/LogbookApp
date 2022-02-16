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
import nl.joozd.logbookapp.databinding.DialogPicNameBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.viewmodels.dialogs.namesDialog.PicNameDialogViewModel
import nl.joozd.logbookapp.ui.adapters.SelectableStringAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

class PicNameDialog: JoozdlogFragment() {
    private val viewModel: PicNameDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogPicNameBinding.bind(inflater.inflate(R.layout.dialog_pic_name, container, false)).apply{
            val namesPickerAdapter = makeNamesPickerAdapterAndAddToList()

            launchFlowCollectors(namesPickerAdapter)
            setOnTextChangedListeners()
            setOnClickListeners()
        }.root

    private fun DialogPicNameBinding.makeNamesPickerAdapterAndAddToList() =
        makeNamesPickerAdapter().also{
            namesPickerList.layoutManager = LinearLayoutManager(context)
            namesPickerList.adapter = it
        }

    private fun makeNamesPickerAdapter() = SelectableStringAdapter{ name ->
        viewModel.setName(name)
    }

    @Suppress("unused") // it is here because it is here in all dialogs (for uniformity)
    private fun DialogPicNameBinding.launchFlowCollectors(namesPickerAdapter: SelectableStringAdapter){
        viewModel.namesListFlow().launchCollectWhileLifecycleStateStarted{
            namesPickerAdapter.submitList(it)
        }
    }

    private fun DialogPicNameBinding.setOnTextChangedListeners(){
        namesSearchField.onTextChanged {
            viewModel.updateQuery(it)
        }
    }

    private fun DialogPicNameBinding.setOnClickListeners(){
        addCurrentTextButton.setOnClickListener {
            viewModel.setName(namesSearchField.text?.toString())
        }

        cancelNamesDialogTextview.setOnClickListener {
            viewModel.undo()
            closeFragment()
        }

        saveNamesDialogTextview.setOnClickListener {
            closeFragment()
        }

        picNameDialogBackground.setOnClickListener {
            // Do nothing, catches stray clicks.
        }

    }

}