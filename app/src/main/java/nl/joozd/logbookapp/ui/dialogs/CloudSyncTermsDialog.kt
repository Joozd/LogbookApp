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
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnScrollChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.dialog_cloud_sync_terms.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.DialogCloudSyncTermsBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.model.viewmodels.dialogs.CloudSyncTermsDialogViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

class CloudSyncTermsDialog: JoozdlogFragment() {
    val viewModel: CloudSyncTermsDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DialogCloudSyncTermsBinding.bind(inflater.inflate(R.layout.dialog_cloud_sync_terms, container, false))

        with(binding){
            acceptTermsBackground.setOnClickListener {  } // catch clicks so they don't fall through

            // termsTextView.movementMethod = ScrollingMovementMethod()

            termsScrollView.viewTreeObserver.addOnScrollChangedListener {
                    if (termsScrollView.getChildAt(0).bottom
                        <= termsScrollView.height + termsScrollView.scrollY
                    ) {
                        viewModel.scrolledToBottom()
                    } // else { //Not at bottom }
                }



            iAcceptTextView.apply {
                setOnClickListener {
                    Preferences.acceptedCloudSyncTerms = true
                    closeFragment()
                }
                val clickable = viewModel.waitedLongEnough.value ?: false
                setTextColor(if (clickable) requireActivity().getColorFromAttr(android.R.attr.colorAccent) else requireActivity().getColorFromAttr(android.R.attr.textColorTertiary))
                isClickable = clickable
            }

            cancelTextView.apply{
                setOnClickListener {
                    closeFragment()
                }
                setTextColor(requireActivity().getColorFromAttr(android.R.attr.colorAccent))
            }

        }

        viewModel.text.observe(viewLifecycleOwner, Observer {
            termsTextView.text = it
        })

        viewModel.waitedLongEnough.observe(viewLifecycleOwner, Observer {
            iAcceptTextView.isClickable = it
            if (it)
                iAcceptTextView.setTextColor(requireActivity().getColorFromAttr(android.R.attr.colorAccent))
        })

        return binding.root
    }
}