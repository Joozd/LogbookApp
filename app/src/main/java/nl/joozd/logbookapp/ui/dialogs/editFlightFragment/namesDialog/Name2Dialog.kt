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
import nl.joozd.logbookapp.ui.adapters.flightsadapter.SwipableStringAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.textscanner.TextScannerActivity

class Name2Dialog: JoozdlogFragment() {
    private val viewModel: Name2DialogViewModel by viewModels()

    private val startScanActivityForResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let{ data ->
                val names = data.getStringArrayListExtra(NAMES_LIST) ?: emptyList()
                val ranks = data.getStringArrayListExtra(RANKS_LIST) ?: emptyList()

                // create toast with amount of found names
                val message = if(names.isEmpty()) getString(R.string.no_names_found_in_scan)
                    else getString(R.string.n_names_found_in_scan, names.size)
                activity?.toast(message)

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
        initializePickedNamesList()
    }

    private fun DialogNamesBinding.initializePickedNamesList(){
        pickedNamesList.apply{
            val swipableStringAdapter = SwipableStringAdapter(requireContext(), R.layout.item_name_dialog){ swipedNameString ->
                viewModel.removeName(swipedNameString)
            }.apply{
                setOnLongClickListener{
                    toast("WIP: long-clicked $it")
                }
            }

            layoutManager = LinearLayoutManager(context)
            adapter = swipableStringAdapter
            viewModel.currentNamesFlow.launchCollectWhileLifecycleStateStarted { namesList ->
                swipableStringAdapter.submitList(namesList.filter { it.isNotBlank() })
            }
        }




    }

    /**
     * Initialize the buttons in this dialog
     */
    private fun DialogNamesBinding.initializeButtons(){
        initializeCameraButton()
        initializeAddNameButton()
        initializePickKnownNamesButton()
    }

    private fun DialogNamesBinding.initializeCameraButton(){
        cameraButton.setOnClickListener {
            startScanActivityForResultLauncher.launch(
                TextScannerActivity.createLaunchIntent(requireContext(), TextScannerActivity.CREW_NAMES_FROM_KLM_FLIGHTDECK, listOf(NAMES_LIST, RANKS_LIST))
            )
        }

    }
    private fun DialogNamesBinding.initializeAddNameButton(){
        addNameButton.apply {
            setOnClickListener {
                addNewRecordAndResetTextView()
            }
        }
    }

    private fun DialogNamesBinding.addNewRecordAndResetTextView() {
        viewModel.addNewEmptyName()
        editNameTextView.setText("") // this will trigger update in viewModel through its onTextChanged, so make sure to add empty name first, else last entered name will be empty as well
        editNameTextView.requestFocus()
    }

    private fun DialogNamesBinding.initializePickKnownNamesButton(){
        pickNamesButton.apply {
            //TODO
            setOnClickListener {
                toast("TODO!")
            }
        }
    }

    /**
     * Initialize the TextViews and EditTexts in this dialog
     */
    private fun DialogNamesBinding.initializeTextViews(){
        initializeNameField()
    }

    /**
     * The Name Field is the field where you can change the bottom name in the list of names.
     */
    private fun DialogNamesBinding.initializeNameField() {
        editNameTextView.apply {
            viewModel.addNewEmptyName() // Make sure names list ends with an empty name so text in this Textview matches actual value for last name
            onTextChanged { viewModel.updateLastName(it) }
            setOnEditorActionListener { _, _, event ->
                if(event != null)
                    addNewRecordAndResetTextView()
                true
            }

        }
    }

    /**
     * Initialize UI components (eg SAVE and CANCEL buttons) in this dialog
     */
    private fun DialogNamesBinding.initializeUIComponents(){
        initializeSaveButton()
        initializeCancelButton()
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

    companion object{
        private const val NAMES_LIST = "NAMES_LIST"
        private const val RANKS_LIST = "RANKS_LIST"
    }


}