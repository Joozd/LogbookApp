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

package nl.joozd.logbookapp.ui.utils

import android.widget.TextView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.showAsActiveIf
import nl.joozd.logbookapp.model.enumclasses.DualInstructorFlag
import nl.joozd.logbookapp.model.enumclasses.PicPicusFlag

/**
 * Set as active and set text for a PicPicus toggle field
 */
fun TextView.setPicPicusField(flag: PicPicusFlag){
    showAsActiveIf(flag != PicPicusFlag.NONE)
    text = getPicPicusStringForFlag(flag)
}

/**
 * Set as active and set text for a DualInstructor toggle field
 */
fun TextView.setDualInstructorField(flag: DualInstructorFlag){
    showAsActiveIf(flag != DualInstructorFlag.NONE)
    text = getDualInstructorStringForFlag(flag)
}


private fun TextView.getPicPicusStringForFlag(flag: PicPicusFlag) = when(flag){
    PicPicusFlag.PICUS -> context.getString(R.string.picus)
    else -> context.getString(R.string.pic)
}

private fun TextView.getDualInstructorStringForFlag(flag: DualInstructorFlag?) = when (flag) {
    DualInstructorFlag.DUAL -> context.getString(R.string.dualString)
    DualInstructorFlag.INSTRUCTOR -> context.getString(R.string.instructorString)
    else -> context.getString(R.string.dualInstructorString)
}