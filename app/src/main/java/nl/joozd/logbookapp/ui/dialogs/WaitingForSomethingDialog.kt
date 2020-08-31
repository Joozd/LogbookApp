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

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogWaitingForSomethingBinding
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast

class WaitingForSomethingDialog: JoozdlogFragment() {

    private val viewModel: MyViewModel by viewModels()
    private var binding: DialogWaitingForSomethingBinding? = null




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        DialogWaitingForSomethingBinding.bind(inflater.inflate(R.layout.dialog_waiting_for_something, container, false)).apply{
            // Disable onBack click
            disableBackPressed()
            backgroundLayout.setOnClickListener {
                //Do nothing
            }

            // put presets into viewModel
            presetDescription?.let{
                description = it
            }
            presetCancelText?.let{
                viewModel.cancelText = it
            }
            presetOnCancelAction?.let{
                viewModel.onCancel = it
            }
            cancelTextView.setOnClickListener {
                Log.d("test 1", "presetNnCancel = $presetOnCancelAction, viewModel version = ${viewModel.onCancel}")
                viewModel.onCancel?.let{
                    toast("click!")
                    it()
                }
            }
            cancelTextView.visibility = if (viewModel.onCancel == null) View.GONE else View.VISIBLE
            viewModel.cancelText?.let{
                cancelTextView.text = it
            }
            viewModel.description?.let{
                descriptionTextView.text = it
            }
            return root
        }
    }



    private var presetCancelText: String? = null
    private var presetOnCancelAction: (() -> Unit)? = null
    /**
     * Set action to perform when 'Cancel' button is clicked.
     * If set to not null it will show the button
     * @param action: Function to perform
     * @param text: Set the text of the Cancel dialog
     */
    fun setCancel(text: String? = null, textResource: Int? = null, action: (() -> Unit)? = null){
        if (isAdded) {
            Log.d("Test 2", "Plekje 1")
            viewModel.onCancel = action
        }
        else {
            Log.d("Test 2", "Plekje 2")
            presetOnCancelAction = action
        }
        text?.let {
            if (isAdded)
                viewModel.cancelText = it
            else presetCancelText = it

            binding?.cancelTextView?.text = it
        }
        textResource?.let{
            viewModel.cancelText = requireActivity().getString(it).also{s ->
                if (isAdded)
                    viewModel.cancelText = s
                else presetCancelText = s
            }

        }
        binding?.cancelTextView?.visibility = if (action == null) View.GONE else View.VISIBLE
    }

    private var presetDescription: String? = null
    var description: String?
        get() = viewModel.description
        set(it){
            if (isAdded)
                viewModel.description = it
            else presetDescription = it
            binding?.descriptionTextView?.text = it
        }

    fun setDescription(id: Int){
        description = ctx.getString(id)
    }

    /**
     * Close the dialog
     */
    fun done() {
        Log.d("Test 3", "Done!")
        closeFragment()
    }
}

    class MyViewModel: ViewModel() {
        var description: String? = null
        var onCancel: (() -> Unit)? = null
        var cancelText: String? = null
}