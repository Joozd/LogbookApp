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
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogAboutBinding
import nl.joozd.logbookapp.model.viewmodels.dialogs.AboutDialogViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

class AboutDialog: JoozdlogFragment() {
    private val viewModel: AboutDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        DialogAboutBinding.bind(inflater.inflate(R.layout.dialog_about, container, false)).apply{
            aboutDialogTopHalf.joozdLogSetBackgroundColor()

            aboutTextView.movementMethod = LinkMovementMethod.getInstance()

            viewModel.text.observe(viewLifecycleOwner){
                aboutTextView.text = HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT)
            }

            aboutDialogBox.setOnClickListener {  } // do nothing

            aboutDialogBackground.setOnClickListener { closeFragment() }

            okButton.setOnClickListener { closeFragment() }

            return root
        }
    }
}