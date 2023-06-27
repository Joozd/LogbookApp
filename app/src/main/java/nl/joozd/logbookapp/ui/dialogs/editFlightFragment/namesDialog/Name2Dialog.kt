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

package nl.joozd.logbookapp.ui.dialogs.editFlightFragment.namesDialog

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogNamesBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.viewmodels.dialogs.namesDialog.Name2DialogViewModel
import nl.joozd.logbookapp.ui.adapters.SelectableStringAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.textscanner.TextScannerActivity

class Name2Dialog: JoozdlogFragment() {
    private val viewModel: Name2DialogViewModel by viewModels()

    private var nameIsSetFromList = false

    private val startScanActivityForResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let{ data ->
                val names = data.getStringArrayListExtra(NAMES_LIST) ?: emptyList()
                val ranks = data.getStringArrayListExtra(RANKS_LIST) ?: emptyList()
                viewModel.handleScanActivityResult(names, ranks)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogNamesBinding.bind(inflater.inflate(R.layout.dialog_names, container, false)).apply{
            initializeRecyclerViews()
            initializeButtons()
            initializeTextViews()
            initializeUIComponents()
        }.root

    private fun DialogNamesBinding.initializeRecyclerViews(){
        initializeNamesPickerRecyclerView()
        initizlizeSelectedNamesRecyclerView()
    }

    /**
     * Initialize the buttons in this dialog
     */
    private fun DialogNamesBinding.initializeButtons(){
        initializeCameraButton()
        initializeRemoveNameButton()
        initializeAddNameButton()
    }

    /**
     * Initialize the TextViews and EditTexts in this dialog
     */
    private fun DialogNamesBinding.initializeTextViews(){
        initializeNameField()
    }

    /**
     * Initialize UI components (eg SAVE and CANCEL buttons) in this dialog
     */
    private fun DialogNamesBinding.initializeUIComponents(){
        initializeSaveButton()
        initializeCancelButton()
    }


    private fun DialogNamesBinding.initializeNamesPickerRecyclerView(){
        val adapter = makeNamesPickerAdapter()
        namesPickerList.layoutManager = LinearLayoutManager(context)
        namesPickerList.adapter = adapter
        collectPickableNamesFlow(adapter)
    }

    private fun DialogNamesBinding.makeNamesPickerAdapter() = SelectableStringAdapter { name ->
        // Set [namesSearchField] to selected name. Set flag so query that updates the list of pickable names is not changed.
        nameIsSetFromList = true
        namesSearchField.setText(name)
        namesSearchField.selectAll()
        viewModel.pickNewName(name)
    }

    private fun DialogNamesBinding.initizlizeSelectedNamesRecyclerView(){
        val adapter = SelectableStringAdapter{ name -> viewModel.pickSelectedName(name) }
        pickedNamesList.layoutManager = LinearLayoutManager(context)
        pickedNamesList.adapter = adapter
        collectSelectedNamesFlow(adapter)
    }


    private fun DialogNamesBinding.initializeAddNameButton() {
        addNameButton.setOnClickListener {
            // add namesSearchField's contents to picked names list if anything not blank is present
            namesSearchField.text.toString().takeIf { it.isNotBlank() }?.let {
                viewModel.addName(it)
            }
            namesSearchField.setText("") // this
        }
    }

    private fun DialogNamesBinding.initializeCameraButton(){
        cameraButton.setOnClickListener {
            startScanActivityForResultLauncher.launch(
                TextScannerActivity.createLaunchIntent(requireContext(), TextScannerActivity.CREW_NAMES_FROM_KLM_FLIGHTDECK, listOf(NAMES_LIST, RANKS_LIST))
            )
        }

    }

    private fun DialogNamesBinding.initializeRemoveNameButton() {
        removeNameButton.setOnClickListener {
            viewModel.removeCurrentNameFromSelectedNames()
        }
    }

    /**
     * The Name Field is the field that shows what gets added to the selected names when the button with the up arrow is pressed.
     */
    private fun DialogNamesBinding.initializeNameField() {
        namesSearchField.onTextChanged {
            // This makes sure the names list query is not changed when a name is selected from the list,
            // but does change when anything is typed into this field.
            if (nameIsSetFromList)
                nameIsSetFromList = false
            else
                viewModel.updateQuery(it)
        }
    }

    private fun DialogNamesBinding.initializeCancelButton() {
        cancelNamesDialogTextview.setOnClickListener {
            viewModel.undo()
            closeFragment()
        }
    }

    private fun DialogNamesBinding.initializeSaveButton() {
        saveNamesDialogTextview.setOnClickListener {
            closeFragment()
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

    companion object{
        private const val NAMES_LIST = "NAMES_LIST"
        private const val RANKS_LIST = "RANKS_LIST"
    }


}