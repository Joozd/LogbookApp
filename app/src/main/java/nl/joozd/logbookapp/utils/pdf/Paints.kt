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

package nl.joozd.logbookapp.utils.pdf


import android.graphics.Paint
import android.graphics.Typeface

object Paints {
    val smallTextCentered = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.CENTER
        textSize=9f
    }
    val smallTextRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.RIGHT
        textSize=9f
    }

    val smallText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.LEFT
        textSize=9f
    }

    val largeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 11f
        val x: Typeface? = null
        typeface = Typeface.create(x, Typeface.BOLD)
    }
    val largeTextRightAligned = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.RIGHT
        textSize = 11f
        val x: Typeface? = null
        typeface = Typeface.create(x, Typeface.BOLD)
    }


    val mediumText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        textAlign = Paint.Align.CENTER
        textSize=10f
    }


    val thickLine = Paint().apply {
        color = 0xFF000000.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    val thinLine = Paint().apply {
        color = 0xFF000000.toInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
}