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

package nl.joozd.logbookapp.ui.utils

import android.view.View
import android.widget.AdapterView

class OnSpinnerItemSelectedListener: AdapterView.OnItemSelectedListener{
    class ItemSelected(private val f: (parent: AdapterView<*>?, view: View?, position: Int, id: Long) -> Unit){
        fun select (parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            f(parent, view, position, id)
        }
    }
    var onItemSelectedListener: ItemSelected? = null
    fun setOnItemSelectedListener (f: (parent: AdapterView<*>?, view: View?, position: Int, id: Long) -> Unit){
        onItemSelectedListener = ItemSelected(f)
    }

    class NothingSelected(private val f: (parent: AdapterView<*>?) -> Unit){
        fun select (parent: AdapterView<*>?) {
            f(parent)
        }
    }
    var onNothingSelectedListener: NothingSelected? = null
    fun setOnNothingSelectedListener (f: (parent: AdapterView<*>?) -> Unit){
        onNothingSelectedListener = NothingSelected(f)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onItemSelectedListener?.select(parent, view, position, id)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        onNothingSelectedListener?.select(parent)
    }

}