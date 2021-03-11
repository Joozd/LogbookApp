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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import com.caverock.androidsvg.SVG
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

class SignatureDialogViewModel: JoozdlogDialogViewModelWithWorkingFlight() {
    private val wf = flightRepository.wf
    private var _signature: String = wf.signatureLiveData.value ?: ""
    val signature: String
        get() = _signature

    private val undoSignature = signature

    val signatureSvg: SVG
        get() = SVG.getFromString(signature)


    fun updateSignature(newSignature: String){
        _signature = newSignature
        wf.setSignature(newSignature)
    }

    fun signatureCleared(){
        _signature = ""
    }

    /**
     * Set signature
     */
    fun setSignature(signature: String) = wf.setSignature(signature)

    override fun undo() = wf.setSignature(undoSignature)
}