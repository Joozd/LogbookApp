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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.caverock.androidsvg.SVG
import kotlinx.android.synthetic.main.dialog_signature.view.*
import com.github.gcacace.signaturepad.views.SignaturePad
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.dialogs.SignatureDialogViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment


/**
 * This will make a signature fragment. Sends the following to onSignedListener:
 * - Doesn't trigger if nothing changed
 * - "" if empty
 * - an SVG string if a signature is made
 * this.signature can be set to an SVG which will be displayed (cannot edit), will not trigger
 * listener if saved. (counts as no change)
 */


class SignatureDialog: JoozdlogFragment() {
    val signatureDialogViewModel: SignatureDialogViewModel by viewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_signature, container, false).apply {

            signature_pad.setOnSignedListener(object : SignaturePad.OnSignedListener {
                override fun onStartSigning() {/* Event triggered when the pad is touched */}
                override fun onSigned() {
                    //Event triggered when the pad is signed
                    signatureDialogViewModel.updateSignature(signature_pad.signatureSvg)
                }
                override fun onClear() {/*Event triggered when the pad is cleared*/}
            })

            clearTextView.setOnClickListener {
                signatureDialogViewModel.updateSignature("")
            }

            /**
             * Observers:
             */

            //If a signature is set, hide the actual signing pad and display the current signature as SVGImageView
            signatureDialogViewModel.signature.observe(viewLifecycleOwner, Observer{signature ->
                if (signature.isNotEmpty()) {
                    signature_pad.visibility = View.INVISIBLE
                    signatureImageView.setSVG(SVG.getFromString(signature))
                    signatureImageView.visibility = View.VISIBLE
                }
                else {
                    signatureImageView.visibility = View.INVISIBLE
                    signature_pad.visibility = View.VISIBLE
                    signature_pad.clear()
                }
            })

            /**
             * UI related butons:
             */

            cancelTextView.setOnClickListener {
                signatureDialogViewModel.undo()
                closeFragment()
            }
            backgroundLayout.setOnClickListener {
                signatureDialogViewModel.undo()
                closeFragment()
            }
            saveTextView.setOnClickListener {
                closeFragment()
            }
            signLayout.setOnClickListener { }
        }
    }
}