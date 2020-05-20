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

package nl.joozd.logbookapp.ui.utils.customs


import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_progressbar.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.getActivity

class JoozdlogProgressBar(private val target: ViewGroup) {
    private val activity = target.getActivity()!!
    private val progBarParent = activity.layoutInflater.inflate(R.layout.item_progressbar, target, false)
    private val progressBar = progBarParent.progressBar

    var text: String
        get() = progBarParent.progressBarText.text.toString()
        set(t) {progBarParent.progressBarText.text = t}

    var progress: Int
        get() = progressBar.progress
        set(p) { progressBar.progress = p}

    var backgroundColor: Int?
        get() = null
        set(color) {
            color?.let{
                progBarParent.setBackgroundColor(it)
            }
        }

    fun show(): JoozdlogProgressBar {
        target.addView(progBarParent)
        return this
    }
    fun remove() = target.removeView(progBarParent)

}