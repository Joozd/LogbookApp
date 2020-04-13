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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.caverock.androidsvg.SVG
import kotlinx.android.synthetic.main.dialog_signature.view.*
import com.github.gcacace.signaturepad.views.SignaturePad
import nl.joozd.logbookapp.R


/**
 * This will make a signature fragment. Sends the follinwing to onSignedListener:
 * - Doesn't trigger if nothing changed
 * - "" if empty
 * - an SVG string if a signature is made
 * this.signature can be set to an SVG which will be displayed (cannot edit), will not trigger
 * listener if saved. (counts as no change)
 */


class SignatureDialog: Fragment() {
    companion object{
        const val TAG = "SignatureDialog"
    }
    class OnSignedListener(private val f: (signature: String) -> Unit) {
        fun sign(signature: String) {
            f(signature)
        }
    }

    var onSignedListener: OnSignedListener? = null
    fun setOnSignedListener(f: (signature: String) -> Unit) {
        onSignedListener = OnSignedListener(f)
    }


    private var mView: View? = null
    private var changesMade: Boolean = false
    private var empty: Boolean = true
    private var signatureSet: Boolean = false // true if signature manually set, false if nothing set or manual entry done
    var signature: String? = null
    get() = if (signatureSet) field else mView?.signature_pad?.signatureSvg
    set(signatureString) {
        field = signatureString
        signatureSet = true
        mView?.signature_pad?.visibility = View.INVISIBLE
        mView?.signatureImageView?.setSVG(SVG.getFromString(signatureString))
        mView?.signatureImageView?.visibility = View.VISIBLE


    }





    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(R.layout.dialog_signature, container, false)


        if (signatureSet){
            v.signature_pad.visibility = View.INVISIBLE
            v.signatureImageView.setSVG(SVG.getFromString(signature))
            v.signatureImageView.visibility = View.VISIBLE
        }

        v.signature_pad.setOnSignedListener(object : SignaturePad.OnSignedListener {

            override fun onStartSigning() {
                //Event triggered when the pad is touched
            }

            override fun onSigned() {
                //Event triggered when the pad is signed
                empty = false
                changesMade = true
            }

            override fun onClear() {
                //Event triggered when the pad is cleared
            }
        })

        v.clearTextView.setOnClickListener {
            // Hide possible imageView with old signature, show new one, set empty and changesMade to true
            v.signatureImageView.visibility = View.INVISIBLE
            v.signature_pad.visibility = View.VISIBLE
            empty = true
            changesMade = true

            activity?.currentFocus?.clearFocus()
            v.signature_pad.clear()
        }
        v.cancelTextView.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            fragmentManager?.popBackStack()
        }
        v.backgroundLayout.setOnClickListener {
            activity?.currentFocus?.clearFocus()
            fragmentManager?.popBackStack()
        }


        v.saveTextView.setOnClickListener {
            Log.d(TAG, v.signature_pad.signatureSvg)
            if (changesMade) {
                onSignedListener?.sign(if (empty) "" else v.signature_pad.signatureSvg)
            }
            activity?.currentFocus?.clearFocus()
            fragmentManager?.popBackStack()
        }

        v.signLayout.setOnClickListener {  }


        return v
    }
}