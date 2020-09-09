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

package nl.joozd.logbookapp.utils.pdf

import android.graphics.Canvas
import android.graphics.RectF
import android.util.Log
import com.caverock.androidsvg.PreserveAspectRatio
import com.caverock.androidsvg.SVG
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.miscClasses.TotalsForward
import nl.joozd.logbookapp.extensions.toLogbookDate
import nl.joozd.logbookapp.model.dataclasses.Flight

// TODO This is fine but needs some adjustments

object PdfLogbookBuilder {
    fun drawFrontPage(canvas: Canvas, name: String = "", licenceNumber: String = "") {
        TODO("This will make the title page of logbook as found in https://www.easa.europa.eu/sites/default/files/dfu/Part-FCL.pdf")
    }

    fun drawSecondPage(canvas: Canvas, address: String) {
        TODO("This will make the address page of logbook as found in https://www.easa.europa.eu/sites/default/files/dfu/Part-FCL.pdf")
    }

    /**
     * Put the lines and text for an empty logbook left page
     * @param canvas: The canvas to draw on
     */
    fun drawLeftPage(canvas: Canvas) {
        var lineNumber = 1
        // Outside box:
        canvas.drawRect(
            1f,
            1f,
            canvas.width.toFloat() - 1f,
            canvas.height.toFloat() - 1f,
            Paints.thickLine
        )
        // Bottom box:
        canvas.drawLine(
            0f,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            canvas.width.toFloat(),
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thickLine
        )
        //top box: (where all descriptions go)
        canvas.drawLine(
            0f,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT,
            canvas.width.toFloat(),
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT,
            Paints.thinLine
        )
        //draw main vertical lines:
        canvas.drawLine(
            PdfLogbookMakerValues.DATE_OFFSET,
            0f,
            PdfLogbookMakerValues.DATE_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET,
            0f,
            PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET,
            0f,
            PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET,
            0f,
            PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.SP_TIME_SE_OFFSET,
            0f,
            PdfLogbookMakerValues.SP_TIME_SE_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.MP_HOURS_OFFSET,
            PdfLogbookMakerValues.ENTRY_HEIGHT,
            PdfLogbookMakerValues.MP_HOURS_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.TOTAL_HOURS_OFFSET,
            0f,
            PdfLogbookMakerValues.TOTAL_HOURS_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.NAME_OFFSET,
            0f,
            PdfLogbookMakerValues.NAME_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.LDG_DAY_OFFSET,
            0f,
            PdfLogbookMakerValues.LDG_DAY_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )

        //draw secondary vertical lines
        canvas.drawLine(
            PdfLogbookMakerValues.DEPARTURE_TIME_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            PdfLogbookMakerValues.DEPARTURE_TIME_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.ARRIVAL_TIME_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            PdfLogbookMakerValues.ARRIVAL_TIME_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.AIRCRAFT_REGISTRATION_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            PdfLogbookMakerValues.AIRCRAFT_REGISTRATION_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.SP_TIME_ME_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            PdfLogbookMakerValues.SP_TIME_ME_OFFSET,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.MP_MINS_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT,
            PdfLogbookMakerValues.MP_MINS_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.TOTAL_MINS_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT,
            PdfLogbookMakerValues.TOTAL_MINS_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.LDG_NIGHT_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            PdfLogbookMakerValues.LDG_NIGHT_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )

        //draw horizontal lines:
        canvas.drawLine(
            0f,
            PdfLogbookMakerValues.ENTRY_HEIGHT,
            canvas.width.toFloat(),
            PdfLogbookMakerValues.ENTRY_HEIGHT,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            PdfLogbookMakerValues.MP_HOURS_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.LDG_DAY_OFFSET,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            PdfLogbookMakerValues.TOTAL_WIDTH_LEFT_PAGE,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.ENTRY_HEIGHT,
            Paints.thinLine
        )

        //draw numbers in top line
        canvas.drawText(
            "1",
            (PdfLogbookMakerValues.DATE_OFFSET + PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "2",
            (PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET + PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "3",
            (PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET + PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "4",
            (PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET + PdfLogbookMakerValues.SP_TIME_ME_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "5",
            (PdfLogbookMakerValues.SP_TIME_ME_OFFSET + PdfLogbookMakerValues.TOTAL_HOURS_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "6",
            (PdfLogbookMakerValues.TOTAL_HOURS_OFFSET + PdfLogbookMakerValues.NAME_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "7",
            (PdfLogbookMakerValues.NAME_OFFSET + PdfLogbookMakerValues.LDG_DAY_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "8",
            (PdfLogbookMakerValues.LDG_DAY_OFFSET + canvas.width - 1) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )

        //Draw first lines of titles in columns
        lineNumber = 2
        canvas.drawText(
            "DATE",
            (PdfLogbookMakerValues.DATE_OFFSET + PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "DEPARTURE",
            (PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET + PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "ARRIVAL",
            (PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET + PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "AIRCRAFT",
            (PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET + PdfLogbookMakerValues.SP_TIME_ME_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "SINGLE",
            (PdfLogbookMakerValues.SP_TIME_SE_OFFSET + PdfLogbookMakerValues.MP_HOURS_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will needmoar lines
        canvas.drawText(
            "MULTI",
            (PdfLogbookMakerValues.MP_HOURS_OFFSET + PdfLogbookMakerValues.TOTAL_HOURS_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will need moar lines
        canvas.drawText(
            "TOTAL",
            (PdfLogbookMakerValues.TOTAL_HOURS_OFFSET + PdfLogbookMakerValues.NAME_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )// will need moar lines
        canvas.drawText(
            "NAME PIC",
            (PdfLogbookMakerValues.NAME_OFFSET + PdfLogbookMakerValues.LDG_DAY_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "LANDINGS",
            (PdfLogbookMakerValues.LDG_DAY_OFFSET + PdfLogbookMakerValues.TOTAL_WIDTH_LEFT_PAGE) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )

        //draw second lines of titles in columns:
        lineNumber = 3
        canvas.drawText(
            "(dd/mm/yy)",
            (PdfLogbookMakerValues.DATE_OFFSET + PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 6,
            Paints.smallTextCentered
        )
        canvas.drawText(
            "PILOT",
            (PdfLogbookMakerValues.SP_TIME_SE_OFFSET + PdfLogbookMakerValues.MP_HOURS_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will needmoar lines
        canvas.drawText(
            "PILOT",
            (PdfLogbookMakerValues.MP_HOURS_OFFSET + PdfLogbookMakerValues.TOTAL_HOURS_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will need moar lines
        canvas.drawText(
            "TIME",
            (PdfLogbookMakerValues.TOTAL_HOURS_OFFSET + PdfLogbookMakerValues.NAME_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )// will need moar lines


        //draw third line of titles in columns:
        lineNumber = 4
        canvas.drawText(
            "TIME",
            (PdfLogbookMakerValues.SP_TIME_SE_OFFSET + PdfLogbookMakerValues.MP_HOURS_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will needmoar lines
        canvas.drawText(
            "TIME",
            (PdfLogbookMakerValues.MP_HOURS_OFFSET + PdfLogbookMakerValues.TOTAL_HOURS_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will need moar lines
        canvas.drawText(
            "OF FLIGHT",
            (PdfLogbookMakerValues.TOTAL_HOURS_OFFSET + PdfLogbookMakerValues.NAME_OFFSET) / 2,
            PdfLogbookMakerValues.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )// will need moar lines

        //draw bottom line of titles in columns
        canvas.drawText(
            "PLACE",
            (PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET + PdfLogbookMakerValues.DEPARTURE_TIME_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "TIME",
            (PdfLogbookMakerValues.DEPARTURE_TIME_OFFSET + PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "PLACE",
            (PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET + PdfLogbookMakerValues.ARRIVAL_TIME_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "TIME",
            (PdfLogbookMakerValues.ARRIVAL_TIME_OFFSET + PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "MAKE, MODEL, VARIANT",
            (PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET + PdfLogbookMakerValues.AIRCRAFT_REGISTRATION_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "REGISTRATION",
            (PdfLogbookMakerValues.AIRCRAFT_REGISTRATION_OFFSET + PdfLogbookMakerValues.SP_TIME_SE_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "SE",
            (PdfLogbookMakerValues.SP_TIME_SE_OFFSET + PdfLogbookMakerValues.SP_TIME_ME_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "ME",
            (PdfLogbookMakerValues.SP_TIME_ME_OFFSET + PdfLogbookMakerValues.MP_HOURS_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "DAY",
            (PdfLogbookMakerValues.LDG_DAY_OFFSET + PdfLogbookMakerValues.LDG_NIGHT_OFFSET) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "NIGHT",
            (PdfLogbookMakerValues.LDG_NIGHT_OFFSET + PdfLogbookMakerValues.TOTAL_WIDTH_LEFT_PAGE) / 2,
            PdfLogbookMakerValues.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )

        //draw lines in bottom part
        canvas.drawLine(
            PdfLogbookMakerValues.MP_HOURS_OFFSET,
            canvas.height - PdfLogbookMakerValues.TOTALS_LINE_HEIGHT,
            PdfLogbookMakerValues.TOTAL_WIDTH_LEFT_PAGE,
            canvas.height - PdfLogbookMakerValues.TOTALS_LINE_HEIGHT,
            Paints.thinLine
        )
        canvas.drawLine(
            PdfLogbookMakerValues.MP_HOURS_OFFSET,
            canvas.height - PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * 2,
            PdfLogbookMakerValues.TOTAL_WIDTH_LEFT_PAGE,
            canvas.height - PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * 2,
            Paints.thinLine
        )

        //draw TOTALS texts in bottom part
        canvas.drawText(
            "TOTAL THIS PAGE",
            PdfLogbookMakerValues.MP_HOURS_OFFSET - 5,
            canvas.height - PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * 2 - 9,
            Paints.largeTextRightAligned
        ) // will need moar lines
        canvas.drawText(
            "TOTAL FROM PREVIOUS PAGES",
            PdfLogbookMakerValues.MP_HOURS_OFFSET - 5,
            canvas.height - PdfLogbookMakerValues.TOTALS_LINE_HEIGHT - 9,
            Paints.largeTextRightAligned
        ) // will need moar lines
        canvas.drawText(
            "TOTAL TIME",
            PdfLogbookMakerValues.MP_HOURS_OFFSET - 5,
            canvas.height - 10f,
            Paints.largeTextRightAligned
        ) // will need moar lines

        //draw horizontal lines between flights
        lineNumber = 1
        while ((lineNumber * PdfLogbookMakerValues.ENTRY_HEIGHT) < (PdfLogbookMakerValues.A4_WIDTH - PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.BOTTOM_SECTION_HEIGHT)) {
            canvas.drawLine(
                0f,
                PdfLogbookMakerValues.TOP_SECTION_HEIGHT + lineNumber * PdfLogbookMakerValues.ENTRY_HEIGHT,
                PdfLogbookMakerValues.TOTAL_WIDTH_LEFT_PAGE,
                PdfLogbookMakerValues.TOP_SECTION_HEIGHT + lineNumber * PdfLogbookMakerValues.ENTRY_HEIGHT,
                Paints.thinLine
            )
            lineNumber += 1
        }

    }

    /**
     * Put the lines and text for an empty logbook right page
     * * @param canvas: The canvas to draw on
     */
    fun drawRightPage(canvas: Canvas){
        var lineNumber = 1
        // Outside box:
        canvas.drawRect(1f,1f,canvas.width.toFloat() -1f,canvas.height.toFloat() -1f, Paints.thickLine)
        // Bottom box:
        canvas.drawLine(0f,PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET, canvas.width.toFloat(), PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET, Paints.thickLine)
        //top box: (where all descriptions go)
        canvas.drawLine(0f,PdfLogbookMakerValues.TOP_SECTION_HEIGHT, canvas.width.toFloat(), PdfLogbookMakerValues.TOP_SECTION_HEIGHT, Paints.thinLine)
        //draw main vertical lines:
        canvas.drawLine(PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET, 0f, PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET, PdfLogbookMakerValues.A4_WIDTH.toFloat(), Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.SYNTH_DATE_OFFSET, 0f, PdfLogbookMakerValues.SYNTH_DATE_OFFSET, PdfLogbookMakerValues.A4_WIDTH.toFloat(), Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.SIGNATURE_OFFSET, 0f, PdfLogbookMakerValues.SIGNATURE_OFFSET, PdfLogbookMakerValues.A4_WIDTH.toFloat(), Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.REMARKS_OFFSET, 0f, PdfLogbookMakerValues.REMARKS_OFFSET, PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET, Paints.thinLine)

        //draw secondary vertical lines
        canvas.drawLine(PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT, PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_HOURS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-PdfLogbookMakerValues.ENTRY_HEIGHT, PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_HOURS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT, PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT, PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.PILOT_FUNC_COPILOT_HOURS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-PdfLogbookMakerValues.ENTRY_HEIGHT, PdfLogbookMakerValues.PILOT_FUNC_COPILOT_HOURS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT, PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.PILOT_FUNC_DUAL_HOURS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-PdfLogbookMakerValues.ENTRY_HEIGHT, PdfLogbookMakerValues.PILOT_FUNC_DUAL_HOURS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT, PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.PILOT_FUNC_INST_HOURS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-PdfLogbookMakerValues.ENTRY_HEIGHT, PdfLogbookMakerValues.PILOT_FUNC_INST_HOURS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT, PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.SYNTH_TYPE_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-PdfLogbookMakerValues.ENTRY_HEIGHT, PdfLogbookMakerValues.SYNTH_TYPE_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.SYNTH_TIME_HOURS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-PdfLogbookMakerValues.ENTRY_HEIGHT, PdfLogbookMakerValues.SYNTH_TIME_HOURS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT, PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET, PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)

        //draw horizontal lines:
        canvas.drawLine(0f,PdfLogbookMakerValues.ENTRY_HEIGHT, canvas.width.toFloat(), PdfLogbookMakerValues.ENTRY_HEIGHT, Paints.thinLine)
        canvas.drawLine(0f, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-PdfLogbookMakerValues.ENTRY_HEIGHT, PdfLogbookMakerValues.SIGNATURE_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-PdfLogbookMakerValues.ENTRY_HEIGHT, Paints.thinLine)

        //draw numbers in top line

        canvas.drawText("9", (PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("10", (PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET + PdfLogbookMakerValues.SYNTH_DATE_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("11", (PdfLogbookMakerValues.SYNTH_DATE_OFFSET + PdfLogbookMakerValues.SIGNATURE_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("12", (PdfLogbookMakerValues.REMARKS_OFFSET + PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)

        //Draw first lines of titles in columns
        lineNumber = 2
        canvas.drawText("OPERATIONAL", (PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("PILOT FUNCTION TIME", (PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET + PdfLogbookMakerValues.SYNTH_DATE_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("SYNTHETIC TRAINING", (PdfLogbookMakerValues.SYNTH_DATE_OFFSET + PdfLogbookMakerValues.SIGNATURE_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("SIGNA-", (PdfLogbookMakerValues.SIGNATURE_OFFSET + PdfLogbookMakerValues.REMARKS_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("REMARKS AND ENDORSEMENTS", (PdfLogbookMakerValues.REMARKS_OFFSET + PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText) // will needmoar lines

        //draw second lines of titles in columns:
        lineNumber = 3
        canvas.drawText("CONDITION TIME", (PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("DEVICES SESSION", (PdfLogbookMakerValues.SYNTH_DATE_OFFSET + PdfLogbookMakerValues.SIGNATURE_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("TURES", (PdfLogbookMakerValues.SIGNATURE_OFFSET + PdfLogbookMakerValues.REMARKS_OFFSET)/2, PdfLogbookMakerValues.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)

        //draw lines in bottom part
        canvas.drawLine(PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET, canvas.height-PdfLogbookMakerValues.TOTALS_LINE_HEIGHT, PdfLogbookMakerValues.SIGNATURE_OFFSET, canvas.height-PdfLogbookMakerValues.TOTALS_LINE_HEIGHT, Paints.thinLine)
        canvas.drawLine(PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET, canvas.height-PdfLogbookMakerValues.TOTALS_LINE_HEIGHT*2, PdfLogbookMakerValues.SIGNATURE_OFFSET, canvas.height-PdfLogbookMakerValues.TOTALS_LINE_HEIGHT*2, Paints.thinLine)

        //draw TOTALS texts in bottom part
        canvas.drawText("I certify that the entries in this log are true", (PdfLogbookMakerValues.SIGNATURE_OFFSET + PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE)/2, canvas.height-PdfLogbookMakerValues.TOTALS_LINE_HEIGHT*2-14, Paints.largeText) // will need moar lines
        canvas.drawText("PILOT'S SIGNATURE", (PdfLogbookMakerValues.SIGNATURE_OFFSET + PdfLogbookMakerValues.TOTAL_WIDTH_RIGHT_PAGE)/2, canvas.height-6f, Paints.largeText) // will need moar lines

        //draw bottom line of titles in columns
        canvas.drawText("NIGHT", (PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_HOURS_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("IFR", (PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_HOURS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("PIC", (PdfLogbookMakerValues.PILOT_FUNC_PIC_HOURS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_COPILOT_HOURS_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("CO-PILOT", (PdfLogbookMakerValues.PILOT_FUNC_COPILOT_HOURS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_DUAL_HOURS_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("DUAL", (PdfLogbookMakerValues.PILOT_FUNC_DUAL_HOURS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_INST_HOURS_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("INSTRUCTOR", (PdfLogbookMakerValues.PILOT_FUNC_INST_HOURS_OFFSET + PdfLogbookMakerValues.SYNTH_DATE_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("DATE", (PdfLogbookMakerValues.SYNTH_DATE_OFFSET + PdfLogbookMakerValues.SYNTH_TYPE_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("TYPE", (PdfLogbookMakerValues.SYNTH_TYPE_OFFSET + PdfLogbookMakerValues.SYNTH_TIME_HOURS_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("TIME", (PdfLogbookMakerValues.SYNTH_TIME_HOURS_OFFSET + PdfLogbookMakerValues.SIGNATURE_OFFSET)/2, PdfLogbookMakerValues.TOP_SECTION_HEIGHT-5, Paints.mediumText)

        //finally, draw horizontal lines that go between flights
        lineNumber = 1
        while ((lineNumber * PdfLogbookMakerValues.ENTRY_HEIGHT) < (PdfLogbookMakerValues.A4_WIDTH - PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.BOTTOM_SECTION_HEIGHT)) {
            canvas.drawLine(
                0f,
                PdfLogbookMakerValues.TOP_SECTION_HEIGHT + lineNumber * PdfLogbookMakerValues.ENTRY_HEIGHT,
                PdfLogbookMakerValues.TOTAL_WIDTH_LEFT_PAGE,
                PdfLogbookMakerValues.TOP_SECTION_HEIGHT + lineNumber * PdfLogbookMakerValues.ENTRY_HEIGHT,
                Paints.thinLine
            )
            lineNumber += 1
        }
    }

    /**
     * Fill a left page with data
     * @param flights: List of all flights to be put on this page
     * @param totalsForward: Totals from previous page
     * @param canvas: A canvas with a left page drawn on it
     * @param aircraftMap: Map of Aircraft Identification strings and aircraft (for determining multipilot time)
     * @see drawLeftPage
     */
    fun fillLeftPage(canvas: Canvas, flights: List<Flight>, totalsForward: TotalsForward){
        val oldTotals = totalsForward.copy()
        val currentTotals = TotalsForward()
        var index=1
        flights.forEach{f ->
            if (!f.isSim) {
                canvas.drawText(f.tOut().toLogbookDate(),
                    (PdfLogbookMakerValues.DATE_OFFSET + PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET) / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.orig,
                    (PdfLogbookMakerValues.DEPARTURE_PLACE_OFFSET + PdfLogbookMakerValues.DEPARTURE_TIME_OFFSET) / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.timeOutString(),
                    (PdfLogbookMakerValues.DEPARTURE_TIME_OFFSET + PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET) / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.dest,
                    (PdfLogbookMakerValues.ARRIVAL_PLACE_OFFSET + PdfLogbookMakerValues.ARRIVAL_TIME_OFFSET) / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.timeInString(),
                    (PdfLogbookMakerValues.ARRIVAL_TIME_OFFSET + PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET) / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.aircraftType,
                    PdfLogbookMakerValues.AIRCRAFT_MODEL_OFFSET + 6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallText
                )
                canvas.drawText(
                    f.registration,
                    PdfLogbookMakerValues.AIRCRAFT_REGISTRATION_OFFSET + 6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallText
                )

                if(f.multiPilotTime > 0) {
                    canvas.drawText(
                        (f.multiPilotTime / 60).toString(),
                        PdfLogbookMakerValues.MP_MINS_OFFSET - 6,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight
                    )
                    canvas.drawText(
                        (f.multiPilotTime % 60).toString().padStart(2, '0'),
                        PdfLogbookMakerValues.MP_MINS_OFFSET + 6,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallText
                    )
                    totalsForward.multiPilot += f.multiPilotTime
                    currentTotals.multiPilot += f.multiPilotTime
                }


                canvas.drawText(
                    (f.duration() / 60).toString(),
                    PdfLogbookMakerValues.TOTAL_MINS_OFFSET - 6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextRight
                )
                canvas.drawText(
                    (f.duration() % 60).toString(),
                    PdfLogbookMakerValues.TOTAL_MINS_OFFSET + 6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallText)
                totalsForward.totalTime += f.duration()
                currentTotals.totalTime += f.duration()

                canvas.drawText(
                    f.name,
                    PdfLogbookMakerValues.NAME_OFFSET + 6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallText
                )

                if (f.landingDay != 0) {
                    canvas.drawText(
                        f.landingDay.toString(),
                        (PdfLogbookMakerValues.LDG_DAY_OFFSET + PdfLogbookMakerValues.LDG_NIGHT_OFFSET) / 2,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered
                    )
                    totalsForward.landingDay += f.landingDay
                    currentTotals.landingDay += f.landingDay
                }

                if (f.landingDay != 0) {
                    canvas.drawText(
                        f.landingNight.toString(),
                        (PdfLogbookMakerValues.LDG_NIGHT_OFFSET + PdfLogbookMakerValues.TOTAL_WIDTH_LEFT_PAGE) / 2,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered
                    )
                    totalsForward.landingNight += f.landingNight
                    currentTotals.landingNight += f.landingNight
                }
            }
            index += 1
        }
        //draw totals:
        //totals this page:
        index = 1
        //multipilot
        if (currentTotals.multiPilot != 0) {
            canvas.drawText(
                (currentTotals.multiPilot / 60).toString(),
                PdfLogbookMakerValues.MP_MINS_OFFSET - 6,
                PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
                Paints.smallTextRight
            )
            canvas.drawText(
                (currentTotals.multiPilot % 60).toString().padStart(2, '0'),
                PdfLogbookMakerValues.MP_MINS_OFFSET + PdfLogbookMakerValues.MP_MINS_WIDTH / 2,
                PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
                Paints.smallTextCentered
            )
        }

        //total time
        canvas.drawText(
            (currentTotals.totalTime/60).toString(),
            PdfLogbookMakerValues.TOTAL_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.totalTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.TOTAL_MINS_OFFSET + PdfLogbookMakerValues.TOTAL_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //landings day
        canvas.drawText(
            currentTotals.landingDay.toString(),
            PdfLogbookMakerValues.LDG_DAY_OFFSET + PdfLogbookMakerValues.LDG_NIGHT_WIDTH/2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        canvas.drawText(
            currentTotals.landingNight.toString(),
            PdfLogbookMakerValues.LDG_NIGHT_OFFSET + PdfLogbookMakerValues.LDG_NIGHT_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)


        //totals previous pages:
        index = 2
        //multipilot
        canvas.drawText(
            (oldTotals.multiPilot/60).toString(),
            PdfLogbookMakerValues.MP_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.multiPilot%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.MP_MINS_OFFSET + PdfLogbookMakerValues.MP_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //total time
        canvas.drawText(
            (oldTotals.totalTime/60).toString(),
            PdfLogbookMakerValues.TOTAL_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.totalTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.TOTAL_MINS_OFFSET + PdfLogbookMakerValues.TOTAL_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //landings day
        canvas.drawText(
            oldTotals.landingDay.toString(),
            PdfLogbookMakerValues.LDG_DAY_OFFSET + PdfLogbookMakerValues.LDG_NIGHT_WIDTH/2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        canvas.drawText(
            oldTotals.landingNight.toString(),
            PdfLogbookMakerValues.LDG_NIGHT_OFFSET + PdfLogbookMakerValues.LDG_NIGHT_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)


        //total totals:
        index = 3
        //multipilot
        canvas.drawText(
            (totalsForward.multiPilot/60).toString(),
            PdfLogbookMakerValues.MP_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.multiPilot%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.MP_MINS_OFFSET + PdfLogbookMakerValues.MP_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //total time
        canvas.drawText(
            (totalsForward.totalTime/60).toString(),
            PdfLogbookMakerValues.TOTAL_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.totalTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.TOTAL_MINS_OFFSET + PdfLogbookMakerValues.TOTAL_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //landings day
        canvas.drawText(
            totalsForward.landingDay.toString(),
            PdfLogbookMakerValues.LDG_DAY_OFFSET + PdfLogbookMakerValues.LDG_NIGHT_WIDTH/2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        canvas.drawText(
            totalsForward.landingNight.toString(),
            PdfLogbookMakerValues.LDG_NIGHT_OFFSET + PdfLogbookMakerValues.LDG_NIGHT_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)


    }

    /**
     * Fill a right page with data
     * @param flights: List of all flights to be put on this page
     * @param totalsForward: Totals from previous page
     * @param canvas: A canvas with a left page drawn on it
     * @see drawRightPage
     */

    fun fillRightPage(canvas: Canvas, flights: List<Flight>, totalsForward: TotalsForward) {
        val oldTotals = totalsForward.copy()
        val currentTotals = TotalsForward()
        var index = 1
        flights.forEach { f ->
            if (f.isSim) {
                canvas.drawText(f.tOut().toLogbookDate(),
                    PdfLogbookMakerValues.SYNTH_DATE_OFFSET + PdfLogbookMakerValues.SYNTH_DATE_WIDTH / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.aircraftType,
                    PdfLogbookMakerValues.SYNTH_TYPE_OFFSET + 6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallText
                )
                canvas.drawText(
                    (f.simTime/60).toString(),
                    PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET -6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextRight
                )
                canvas.drawText(
                    (f.simTime%60).toString().padStart(2, '0'),
                    PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET + PdfLogbookMakerValues.SYNTH_TIME_MINS_WIDTH / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered)

                totalsForward.simTime += f.simTime
                currentTotals.simTime += f.simTime
            }
            else { // ie. if not sim:
                canvas.drawText(
                    (f.nightTime/60).toString(),
                    PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET -6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextRight)
                canvas.drawText(
                    (f.nightTime%60).toString().padStart(2, '0'),
                    PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_WIDTH / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered)
                totalsForward.nightTime += f.nightTime
                currentTotals.nightTime += f.nightTime

                canvas.drawText(
                    (f.ifrTime/60).toString(),
                    PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET -6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextRight)
                canvas.drawText(
                    (f.ifrTime%60).toString().padStart(2, '0'),
                    PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_WIDTH / 2,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered)
                totalsForward.ifrTime += f.ifrTime
                currentTotals.ifrTime += f.ifrTime

                if (f.isPIC) {
                    canvas.drawText(
                        (f.duration() / 60).toString(),
                        PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET - 6,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight)
                    canvas.drawText(
                        (f.duration ()%60).toString().padStart(2, '0'),
                        PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_WIDTH / 2,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered)
                    totalsForward.picTime += f.duration()
                    currentTotals.picTime += f.duration()

                }
                if (f.isCoPilot) {
                    canvas.drawText(
                        (f.duration() / 60).toString(),
                        PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET - 6,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight)
                    canvas.drawText(
                        (f.duration ()%60).toString().padStart(2, '0'),
                        PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_WIDTH / 2,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered)
                    totalsForward.copilotTime += f.duration()
                    currentTotals.copilotTime += f.duration()
                }
                if (f.isDual) {
                    canvas.drawText(
                        (f.duration() / 60).toString(),
                        PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET - 6,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight)
                    canvas.drawText(
                        (f.duration ()%60).toString().padStart(2, '0'),
                        PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_WIDTH / 2,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered)
                    totalsForward.dualTime += f.duration()
                    currentTotals.dualTime += f.duration()
                }
                if (f.isInstructor) {
                    canvas.drawText(
                        (f.duration() / 60).toString(),
                        PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET - 6,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight)
                    canvas.drawText(
                        (f.duration ()%60).toString().padStart(2, '0'),
                        PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_WIDTH / 2,
                        PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered)
                    totalsForward.instructorTime += f.duration()
                    currentTotals.instructorTime += f.duration()
                }
                if (f.signature.isNotEmpty()){
                    val svg = SVG.getFromString(f.signature)
                    svg.documentPreserveAspectRatio= PreserveAspectRatio.LETTERBOX
                    val pict2 = svg.renderToPicture(900, 300)

                    canvas.drawPicture(pict2, RectF(PdfLogbookMakerValues.SIGNATURE_OFFSET,PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * (index-1), PdfLogbookMakerValues.REMARKS_OFFSET, PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index))
                }
                canvas.drawText(
                    f.remarks,
                    PdfLogbookMakerValues.REMARKS_OFFSET + 6,
                    PdfLogbookMakerValues.TOP_SECTION_HEIGHT + PdfLogbookMakerValues.ENTRY_HEIGHT * index - 6,
                    Paints.smallText)


            }
            index += 1
        }
        //draw totals:
        //totals this page:
        index = 1
        //night
        canvas.drawText(
            (currentTotals.nightTime/60).toString(),
            PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.nightTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //ifr
        canvas.drawText(
            (currentTotals.ifrTime/60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.ifrTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //pic
        canvas.drawText(
            (currentTotals.picTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.picTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //copilot
        canvas.drawText(
            (currentTotals.copilotTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.copilotTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //dual
        canvas.drawText(
            (currentTotals.dualTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.dualTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //instructor
        canvas.drawText(
            (currentTotals.instructorTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.instructorTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //sim
        canvas.drawText(
            (currentTotals.simTime/60).toString(),
            PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.simTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET + PdfLogbookMakerValues.SYNTH_TIME_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //totals previous pages:
        index = 2
        //night
        canvas.drawText(
            (oldTotals.nightTime/60).toString(),
            PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.nightTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //ifr
        canvas.drawText(
            (oldTotals.ifrTime/60).toString(),
            PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.ifrTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //pic
        canvas.drawText(
            (oldTotals.picTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.picTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //copilot
        canvas.drawText(
            (oldTotals.copilotTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.copilotTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //dual
        canvas.drawText(
            (oldTotals.dualTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.dualTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //instructor
        canvas.drawText(
            (oldTotals.instructorTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.instructorTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //sim
        canvas.drawText(
            (oldTotals.simTime/60).toString(),
            PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.simTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET + PdfLogbookMakerValues.SYNTH_TIME_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)


        //total totals:
        index = 3
        //night
        canvas.drawText(
            (totalsForward.nightTime/60).toString(),
            PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.nightTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_NIGHT_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //ifr
        canvas.drawText(
            (totalsForward.ifrTime/60).toString(),
            PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.ifrTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_OFFSET + PdfLogbookMakerValues.CONDITIONAL_TIME_IFR_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //pic
        canvas.drawText(
            (totalsForward.picTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.picTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_PIC_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //copilot
        canvas.drawText(
            (totalsForward.copilotTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.copilotTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_COPILOT_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //dual
        canvas.drawText(
            (totalsForward.dualTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.dualTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_DUAL_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //instructor
        canvas.drawText(
            (totalsForward.instructorTime/60).toString(),
            PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.instructorTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_OFFSET + PdfLogbookMakerValues.PILOT_FUNC_INST_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //sim
        canvas.drawText(
            (totalsForward.simTime/60).toString(),
            PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET -6,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.simTime%60).toString().padStart(2, '0'),
            PdfLogbookMakerValues.SYNTH_TIME_MINS_OFFSET + PdfLogbookMakerValues.SYNTH_TIME_MINS_WIDTH / 2,
            PdfLogbookMakerValues.BOTTOM_SECTION_OFFSET + PdfLogbookMakerValues.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
    }


    /**
     * This can be extended to accomodate multiuple paper sizes
     */
    fun maxLines(): Int = ((PdfLogbookMakerValues.A4_WIDTH - PdfLogbookMakerValues.TOP_SECTION_HEIGHT - PdfLogbookMakerValues.BOTTOM_SECTION_HEIGHT)/PdfLogbookMakerValues.ENTRY_HEIGHT).toInt()
}

