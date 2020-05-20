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

// TODO This is fine but needs some adjustments

object PdfTools { }

/*    fun drawFrontPage(canvas: Canvas, name: String = "", licenceNumber: String = "") {
        TODO("This will make the title page of logbook as found in https://www.easa.europa.eu/sites/default/files/dfu/Part-FCL.pdf")
    }

    fun drawSecondPage(canvas: Canvas, address: String) {
        TODO("This will make the address page of logbook as found in https://www.easa.europa.eu/sites/default/files/dfu/Part-FCL.pdf")
    }


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
            Values.BOTTOM_SECTION_OFFSET,
            canvas.width.toFloat(),
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thickLine
        )
        //top box: (where all descriptions go)
        canvas.drawLine(
            0f,
            Values.TOP_SECTION_HEIGHT,
            canvas.width.toFloat(),
            Values.TOP_SECTION_HEIGHT,
            Paints.thinLine
        )
        //draw main vertical lines:
        canvas.drawLine(
            Values.DATE_OFFSET,
            0f,
            Values.DATE_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.DEPARTURE_PLACE_OFFSET,
            0f,
            Values.DEPARTURE_PLACE_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.ARRIVAL_PLACE_OFFSET,
            0f,
            Values.ARRIVAL_PLACE_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.AIRCRAFT_MODEL_OFFSET,
            0f,
            Values.AIRCRAFT_MODEL_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.SP_TIME_SE_OFFSET,
            0f,
            Values.SP_TIME_SE_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.MP_HOURS_OFFSET,
            Values.ENTRY_HEIGHT,
            Values.MP_HOURS_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            Values.TOTAL_HOURS_OFFSET,
            0f,
            Values.TOTAL_HOURS_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            Values.NAME_OFFSET,
            0f,
            Values.NAME_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            Values.LDG_DAY_OFFSET,
            0f,
            Values.LDG_DAY_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )

        //draw secondary vertical lines
        canvas.drawLine(
            Values.DEPARTURE_TIME_OFFSET,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Values.DEPARTURE_TIME_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.ARRIVAL_TIME_OFFSET,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Values.ARRIVAL_TIME_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.AIRCRAFT_REGISTRATION_OFFSET,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Values.AIRCRAFT_REGISTRATION_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.SP_TIME_ME_OFFSET,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Values.SP_TIME_ME_OFFSET,
            Values.BOTTOM_SECTION_OFFSET,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.MP_MINS_OFFSET,
            Values.TOP_SECTION_HEIGHT,
            Values.MP_MINS_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            Values.TOTAL_MINS_OFFSET,
            Values.TOP_SECTION_HEIGHT,
            Values.TOTAL_MINS_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )
        canvas.drawLine(
            Values.LDG_NIGHT_OFFSET,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Values.LDG_NIGHT_OFFSET,
            canvas.height.toFloat(),
            Paints.thinLine
        )

        //draw horizontal lines:
        canvas.drawLine(
            0f,
            Values.ENTRY_HEIGHT,
            canvas.width.toFloat(),
            Values.ENTRY_HEIGHT,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.DEPARTURE_PLACE_OFFSET,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Values.MP_HOURS_OFFSET,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.LDG_DAY_OFFSET,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Values.TOTAL_WIDTH_LEFT_PAGE,
            Values.TOP_SECTION_HEIGHT - Values.ENTRY_HEIGHT,
            Paints.thinLine
        )

        //draw numbers in top line
        canvas.drawText(
            "1",
            (Values.DATE_OFFSET + Values.DEPARTURE_PLACE_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "2",
            (Values.DEPARTURE_PLACE_OFFSET + Values.ARRIVAL_PLACE_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "3",
            (Values.ARRIVAL_PLACE_OFFSET + Values.AIRCRAFT_MODEL_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "4",
            (Values.AIRCRAFT_MODEL_OFFSET + Values.SP_TIME_ME_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "5",
            (Values.SP_TIME_ME_OFFSET + Values.TOTAL_HOURS_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "6",
            (Values.TOTAL_HOURS_OFFSET + Values.NAME_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "7",
            (Values.NAME_OFFSET + Values.LDG_DAY_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "8",
            (Values.LDG_DAY_OFFSET + canvas.width - 1) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )

        //Draw first lines of titles in columns
        lineNumber = 2
        canvas.drawText(
            "DATE",
            (Values.DATE_OFFSET + Values.DEPARTURE_PLACE_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "DEPARTURE",
            (Values.DEPARTURE_PLACE_OFFSET + Values.ARRIVAL_PLACE_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "ARRIVAL",
            (Values.ARRIVAL_PLACE_OFFSET + Values.AIRCRAFT_MODEL_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "AIRCRAFT",
            (Values.AIRCRAFT_MODEL_OFFSET + Values.SP_TIME_ME_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "SINGLE",
            (Values.SP_TIME_SE_OFFSET + Values.MP_HOURS_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will needmoar lines
        canvas.drawText(
            "MULTI",
            (Values.MP_HOURS_OFFSET + Values.TOTAL_HOURS_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will need moar lines
        canvas.drawText(
            "TOTAL",
            (Values.TOTAL_HOURS_OFFSET + Values.NAME_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )// will need moar lines
        canvas.drawText(
            "NAME PIC",
            (Values.NAME_OFFSET + Values.LDG_DAY_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )
        canvas.drawText(
            "LANDINGS",
            (Values.LDG_DAY_OFFSET + Values.TOTAL_WIDTH_LEFT_PAGE) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )

        //draw second lines of titles in columns:
        lineNumber = 3
        canvas.drawText(
            "(dd/mm/yy)",
            (Values.DATE_OFFSET + Values.DEPARTURE_PLACE_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 6,
            Paints.smallTextCentered
        )
        canvas.drawText(
            "PILOT",
            (Values.SP_TIME_SE_OFFSET + Values.MP_HOURS_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will needmoar lines
        canvas.drawText(
            "PILOT",
            (Values.MP_HOURS_OFFSET + Values.TOTAL_HOURS_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will need moar lines
        canvas.drawText(
            "TIME",
            (Values.TOTAL_HOURS_OFFSET + Values.NAME_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )// will need moar lines


        //draw third line of titles in columns:
        lineNumber = 4
        canvas.drawText(
            "TIME",
            (Values.SP_TIME_SE_OFFSET + Values.MP_HOURS_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will needmoar lines
        canvas.drawText(
            "TIME",
            (Values.MP_HOURS_OFFSET + Values.TOTAL_HOURS_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        ) // will need moar lines
        canvas.drawText(
            "OF FLIGHT",
            (Values.TOTAL_HOURS_OFFSET + Values.NAME_OFFSET) / 2,
            Values.ENTRY_HEIGHT * lineNumber - 4,
            Paints.largeText
        )// will need moar lines

        //draw bottom line of titles in columns
        canvas.drawText(
            "PLACE",
            (Values.DEPARTURE_PLACE_OFFSET + Values.DEPARTURE_TIME_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "TIME",
            (Values.DEPARTURE_TIME_OFFSET + Values.ARRIVAL_PLACE_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "PLACE",
            (Values.ARRIVAL_PLACE_OFFSET + Values.ARRIVAL_TIME_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "TIME",
            (Values.ARRIVAL_TIME_OFFSET + Values.AIRCRAFT_MODEL_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "MAKE, MODEL, VARIANT",
            (Values.AIRCRAFT_MODEL_OFFSET + Values.AIRCRAFT_REGISTRATION_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "REGISTRATION",
            (Values.AIRCRAFT_REGISTRATION_OFFSET + Values.SP_TIME_SE_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "SE",
            (Values.SP_TIME_SE_OFFSET + Values.SP_TIME_ME_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "ME",
            (Values.SP_TIME_ME_OFFSET + Values.MP_HOURS_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "DAY",
            (Values.LDG_DAY_OFFSET + Values.LDG_NIGHT_OFFSET) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )
        canvas.drawText(
            "NIGHT",
            (Values.LDG_NIGHT_OFFSET + Values.TOTAL_WIDTH_LEFT_PAGE) / 2,
            Values.TOP_SECTION_HEIGHT - 5,
            Paints.mediumText
        )

        //draw lines in bottom part
        canvas.drawLine(
            Values.MP_HOURS_OFFSET,
            canvas.height - Values.TOTALS_LINE_HEIGHT,
            Values.TOTAL_WIDTH_LEFT_PAGE,
            canvas.height - Values.TOTALS_LINE_HEIGHT,
            Paints.thinLine
        )
        canvas.drawLine(
            Values.MP_HOURS_OFFSET,
            canvas.height - Values.TOTALS_LINE_HEIGHT * 2,
            Values.TOTAL_WIDTH_LEFT_PAGE,
            canvas.height - Values.TOTALS_LINE_HEIGHT * 2,
            Paints.thinLine
        )

        //draw TOTALS texts in bottom part
        canvas.drawText(
            "TOTAL THIS PAGE",
            Values.MP_HOURS_OFFSET - 5,
            canvas.height - Values.TOTALS_LINE_HEIGHT * 2 - 9,
            Paints.largeTextRightAligned
        ) // will need moar lines
        canvas.drawText(
            "TOTAL FROM PREVIOUS PAGES",
            Values.MP_HOURS_OFFSET - 5,
            canvas.height - Values.TOTALS_LINE_HEIGHT - 9,
            Paints.largeTextRightAligned
        ) // will need moar lines
        canvas.drawText(
            "TOTAL TIME",
            Values.MP_HOURS_OFFSET - 5,
            canvas.height - 10f,
            Paints.largeTextRightAligned
        ) // will need moar lines

        //draw horizontal lines between flights
        lineNumber = 1
        while ((lineNumber * Values.ENTRY_HEIGHT) < (Values.A4_WIDTH - Values.TOP_SECTION_HEIGHT - Values.BOTTOM_SECTION_HEIGHT)) {
            canvas.drawLine(
                0f,
                Values.TOP_SECTION_HEIGHT + lineNumber * Values.ENTRY_HEIGHT,
                Values.TOTAL_WIDTH_LEFT_PAGE,
                Values.TOP_SECTION_HEIGHT + lineNumber * Values.ENTRY_HEIGHT,
                Paints.thinLine
            )
            lineNumber += 1
        }

    }
    fun drawRightPage(canvas: Canvas){
        var lineNumber = 1
        // Outside box:
        canvas.drawRect(1f,1f,canvas.width.toFloat() -1f,canvas.height.toFloat() -1f, Paints.thickLine)
        // Bottom box:
        canvas.drawLine(0f,Values.BOTTOM_SECTION_OFFSET, canvas.width.toFloat(), Values.BOTTOM_SECTION_OFFSET, Paints.thickLine)
        //top box: (where all descriptions go)
        canvas.drawLine(0f,Values.TOP_SECTION_HEIGHT, canvas.width.toFloat(), Values.TOP_SECTION_HEIGHT, Paints.thinLine)
        //draw main vertical lines:
        canvas.drawLine(Values.PILOT_FUNC_PIC_HOURS_OFFSET, 0f, Values.PILOT_FUNC_PIC_HOURS_OFFSET, Values.A4_WIDTH.toFloat(), Paints.thinLine)
        canvas.drawLine(Values.SYNTH_DATE_OFFSET, 0f, Values.SYNTH_DATE_OFFSET, Values.A4_WIDTH.toFloat(), Paints.thinLine)
        canvas.drawLine(Values.SIGNATURE_OFFSET, 0f, Values.SIGNATURE_OFFSET, Values.A4_WIDTH.toFloat(), Paints.thinLine)
        canvas.drawLine(Values.REMARKS_OFFSET, 0f, Values.REMARKS_OFFSET, Values.BOTTOM_SECTION_OFFSET, Paints.thinLine)

        //draw secondary vertical lines
        canvas.drawLine(Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET, Values.TOP_SECTION_HEIGHT, Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.CONDITIONAL_TIME_IFR_HOURS_OFFSET, Values.TOP_SECTION_HEIGHT-Values.ENTRY_HEIGHT, Values.CONDITIONAL_TIME_IFR_HOURS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.CONDITIONAL_TIME_IFR_MINS_OFFSET, Values.TOP_SECTION_HEIGHT, Values.CONDITIONAL_TIME_IFR_MINS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.PILOT_FUNC_PIC_MINS_OFFSET, Values.TOP_SECTION_HEIGHT, Values.PILOT_FUNC_PIC_MINS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.PILOT_FUNC_COPILOT_HOURS_OFFSET, Values.TOP_SECTION_HEIGHT-Values.ENTRY_HEIGHT, Values.PILOT_FUNC_COPILOT_HOURS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.PILOT_FUNC_COPILOT_MINS_OFFSET, Values.TOP_SECTION_HEIGHT, Values.PILOT_FUNC_COPILOT_MINS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.PILOT_FUNC_DUAL_HOURS_OFFSET, Values.TOP_SECTION_HEIGHT-Values.ENTRY_HEIGHT, Values.PILOT_FUNC_DUAL_HOURS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.PILOT_FUNC_DUAL_MINS_OFFSET, Values.TOP_SECTION_HEIGHT, Values.PILOT_FUNC_DUAL_MINS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.PILOT_FUNC_INST_HOURS_OFFSET, Values.TOP_SECTION_HEIGHT-Values.ENTRY_HEIGHT, Values.PILOT_FUNC_INST_HOURS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.PILOT_FUNC_INST_MINS_OFFSET, Values.TOP_SECTION_HEIGHT, Values.PILOT_FUNC_INST_MINS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.SYNTH_TYPE_OFFSET, Values.TOP_SECTION_HEIGHT-Values.ENTRY_HEIGHT, Values.SYNTH_TYPE_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.SYNTH_TIME_HOURS_OFFSET, Values.TOP_SECTION_HEIGHT-Values.ENTRY_HEIGHT, Values.SYNTH_TIME_HOURS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)
        canvas.drawLine(Values.SYNTH_TIME_MINS_OFFSET, Values.TOP_SECTION_HEIGHT, Values.SYNTH_TIME_MINS_OFFSET, Values.TOTAL_WIDTH_RIGHT_PAGE, Paints.thinLine)

        //draw horizontal lines:
        canvas.drawLine(0f,Values.ENTRY_HEIGHT, canvas.width.toFloat(), Values.ENTRY_HEIGHT, Paints.thinLine)
        canvas.drawLine(0f, Values.TOP_SECTION_HEIGHT-Values.ENTRY_HEIGHT, Values.SIGNATURE_OFFSET, Values.TOP_SECTION_HEIGHT-Values.ENTRY_HEIGHT, Paints.thinLine)

        //draw numbers in top line

        canvas.drawText("9", (Values.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + Values.PILOT_FUNC_PIC_HOURS_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("10", (Values.PILOT_FUNC_PIC_HOURS_OFFSET + Values.SYNTH_DATE_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("11", (Values.SYNTH_DATE_OFFSET + Values.SIGNATURE_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("12", (Values.REMARKS_OFFSET + Values.TOTAL_WIDTH_RIGHT_PAGE)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)

        //Draw first lines of titles in columns
        lineNumber = 2
        canvas.drawText("OPERATIONAL", (Values.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + Values.PILOT_FUNC_PIC_HOURS_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("PILOT FUNCTION TIME", (Values.PILOT_FUNC_PIC_HOURS_OFFSET + Values.SYNTH_DATE_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("SYNTHETIC TRAINING", (Values.SYNTH_DATE_OFFSET + Values.SIGNATURE_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("SIGNA-", (Values.SIGNATURE_OFFSET + Values.REMARKS_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("REMARKS AND ENDORSEMENTS", (Values.REMARKS_OFFSET + Values.TOTAL_WIDTH_RIGHT_PAGE)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText) // will needmoar lines

        //draw second lines of titles in columns:
        lineNumber = 3
        canvas.drawText("CONDITION TIME", (Values.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + Values.PILOT_FUNC_PIC_HOURS_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("DEVICES SESSION", (Values.SYNTH_DATE_OFFSET + Values.SIGNATURE_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)
        canvas.drawText("TURES", (Values.SIGNATURE_OFFSET + Values.REMARKS_OFFSET)/2, Values.ENTRY_HEIGHT*lineNumber-4, Paints.largeText)

        //draw lines in bottom part
        canvas.drawLine(Values.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET, canvas.height-Values.TOTALS_LINE_HEIGHT, Values.SIGNATURE_OFFSET, canvas.height-Values.TOTALS_LINE_HEIGHT, Paints.thinLine)
        canvas.drawLine(Values.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET, canvas.height-Values.TOTALS_LINE_HEIGHT*2, Values.SIGNATURE_OFFSET, canvas.height-Values.TOTALS_LINE_HEIGHT*2, Paints.thinLine)

        //draw TOTALS texts in bottom part
        canvas.drawText("I certify that the entries in this log are true", (Values.SIGNATURE_OFFSET + Values.TOTAL_WIDTH_RIGHT_PAGE)/2, canvas.height-Values.TOTALS_LINE_HEIGHT*2-14, Paints.largeText) // will need moar lines
        canvas.drawText("PILOT'S SIGNATURE", (Values.SIGNATURE_OFFSET + Values.TOTAL_WIDTH_RIGHT_PAGE)/2, canvas.height-6f, Paints.largeText) // will need moar lines

        //draw bottom line of titles in columns
        canvas.drawText("NIGHT", (Values.CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + Values.CONDITIONAL_TIME_IFR_HOURS_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("IFR", (Values.CONDITIONAL_TIME_IFR_HOURS_OFFSET + Values.PILOT_FUNC_PIC_HOURS_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("PIC", (Values.PILOT_FUNC_PIC_HOURS_OFFSET + Values.PILOT_FUNC_COPILOT_HOURS_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("CO-PILOT", (Values.PILOT_FUNC_COPILOT_HOURS_OFFSET + Values.PILOT_FUNC_DUAL_HOURS_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("DUAL", (Values.PILOT_FUNC_DUAL_HOURS_OFFSET + Values.PILOT_FUNC_INST_HOURS_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("INSTRUCTOR", (Values.PILOT_FUNC_INST_HOURS_OFFSET + Values.SYNTH_DATE_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("DATE", (Values.SYNTH_DATE_OFFSET + Values.SYNTH_TYPE_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("TYPE", (Values.SYNTH_TYPE_OFFSET + Values.SYNTH_TIME_HOURS_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)
        canvas.drawText("TIME", (Values.SYNTH_TIME_HOURS_OFFSET + Values.SIGNATURE_OFFSET)/2, Values.TOP_SECTION_HEIGHT-5, Paints.mediumText)

        //finally, draw horizontal lines that go between flights
        lineNumber = 1
        while ((lineNumber * Values.ENTRY_HEIGHT) < (Values.A4_WIDTH - Values.TOP_SECTION_HEIGHT - Values.BOTTOM_SECTION_HEIGHT)) {
            canvas.drawLine(
                0f,
                Values.TOP_SECTION_HEIGHT + lineNumber * Values.ENTRY_HEIGHT,
                Values.TOTAL_WIDTH_LEFT_PAGE,
                Values.TOP_SECTION_HEIGHT + lineNumber * Values.ENTRY_HEIGHT,
                Paints.thinLine
            )
            lineNumber += 1
        }
    }

    fun fillLeftPage(canvas: Canvas, flights: List<Flight>, totalsForward: TotalsForward){
        val oldTotals = totalsForward.copy()
        val currentTotals = TotalsForward()
        var index=1
        flights.forEach{f ->
            if (!f.sim) {
                canvas.drawText(
                    "${f.tOut.dayOfMonth}/${f.tOut.monthValue}/${f.tOut.year % 100}",
                    (Values.DATE_OFFSET + Values.DEPARTURE_PLACE_OFFSET) / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.orig,
                    (Values.DEPARTURE_PLACE_OFFSET + Values.DEPARTURE_TIME_OFFSET) / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.timeOutString,
                    (Values.DEPARTURE_TIME_OFFSET + Values.ARRIVAL_PLACE_OFFSET) / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.dest,
                    (Values.ARRIVAL_PLACE_OFFSET + Values.ARRIVAL_TIME_OFFSET) / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.timeInString,
                    (Values.ARRIVAL_TIME_OFFSET + Values.AIRCRAFT_MODEL_OFFSET) / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.aircraft,
                    Values.AIRCRAFT_MODEL_OFFSET + 6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallText
                )
                canvas.drawText(
                    f.registration,
                    Values.AIRCRAFT_REGISTRATION_OFFSET + 6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallText
                )
                // TODO Find out if SE or ME
                if (0 > 0) {      // if aircraft is known and MP
                    canvas.drawText(
                        f.correctedDuration.toHours().toString(),
                        Values.MP_MINS_OFFSET - 6,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight
                    )
                    canvas.drawText(
                        (f.correctedDuration.toMinutes() % 60).toString(),
                        Values.MP_MINS_OFFSET + 6,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallText
                    )
                    totalsForward.multiPilot += f.correctedDuration.toMinutes().toInt()
                    currentTotals.multiPilot += f.correctedDuration.toMinutes().toInt()
                }

                canvas.drawText(
                    f.correctedDuration.toHours().toString(),
                    Values.TOTAL_MINS_OFFSET - 6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextRight
                )
                canvas.drawText(
                    (f.correctedDuration.toMinutes() % 60).toString(),
                    Values.TOTAL_MINS_OFFSET + 6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallText)
                totalsForward.totalTime += f.correctedDuration.toMinutes().toInt()
                currentTotals.totalTime += f.correctedDuration.toMinutes().toInt()

                canvas.drawText(
                    f.name,
                    Values.NAME_OFFSET + 6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallText
                )
                canvas.drawText(
                    f.landingDay.toString(),
                    (Values.LDG_DAY_OFFSET + Values.LDG_NIGHT_OFFSET) / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                totalsForward.landingDay += f.landingDay
                currentTotals.landingDay += f.landingDay

                canvas.drawText(
                    f.landingNight.toString(),
                    (Values.LDG_NIGHT_OFFSET + Values.TOTAL_WIDTH_LEFT_PAGE) / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered)
                totalsForward.landingNight += f.landingNight
                currentTotals.landingNight += f.landingNight
            }
            index += 1
        }
        //draw totals:
        //totals this page:
        index = 1
        //multipilot
        canvas.drawText(
            (currentTotals.multiPilot/60).toString(),
            Values.MP_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.multiPilot%60).toString(),
            Values.MP_MINS_OFFSET + Values.MP_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //total time
        canvas.drawText(
            (currentTotals.totalTime/60).toString(),
            Values.TOTAL_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.totalTime%60).toString(),
            Values.TOTAL_MINS_OFFSET + Values.TOTAL_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //landings day
        canvas.drawText(
            currentTotals.landingDay.toString(),
            Values.LDG_DAY_OFFSET + Values.LDG_NIGHT_WIDTH/2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        canvas.drawText(
            currentTotals.landingNight.toString(),
            Values.LDG_NIGHT_OFFSET + Values.LDG_NIGHT_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)


        //totals previous pages:
        index = 2
        //multipilot
        canvas.drawText(
            (oldTotals.multiPilot/60).toString(),
            Values.MP_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.multiPilot%60).toString(),
            Values.MP_MINS_OFFSET + Values.MP_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //total time
        canvas.drawText(
            (oldTotals.totalTime/60).toString(),
            Values.TOTAL_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.totalTime%60).toString(),
            Values.TOTAL_MINS_OFFSET + Values.TOTAL_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //landings day
        canvas.drawText(
            oldTotals.landingDay.toString(),
            Values.LDG_DAY_OFFSET + Values.LDG_NIGHT_WIDTH/2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        canvas.drawText(
            oldTotals.landingNight.toString(),
            Values.LDG_NIGHT_OFFSET + Values.LDG_NIGHT_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)


        //total totals:
        index = 3
        //multipilot
        canvas.drawText(
            (totalsForward.multiPilot/60).toString(),
            Values.MP_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.multiPilot%60).toString(),
            Values.MP_MINS_OFFSET + Values.MP_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //total time
        canvas.drawText(
            (totalsForward.totalTime/60).toString(),
            Values.TOTAL_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.totalTime%60).toString(),
            Values.TOTAL_MINS_OFFSET + Values.TOTAL_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //landings day
        canvas.drawText(
            totalsForward.landingDay.toString(),
            Values.LDG_DAY_OFFSET + Values.LDG_NIGHT_WIDTH/2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        canvas.drawText(
            totalsForward.landingNight.toString(),
            Values.LDG_NIGHT_OFFSET + Values.LDG_NIGHT_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)


    }

    fun fillRightPage(canvas: Canvas, flights: List<Flight>, totalsForward: TotalsForward) {
        val TAG = "PdfTools.fillRightPage()"
        val oldTotals = totalsForward.copy()
        val currentTotals = TotalsForward()
        var index = 1
        flights.forEach { f ->
            if (f.sim) {
                canvas.drawText(
                    "${f.tOut.dayOfMonth}/${f.tOut.monthValue}/${f.tOut.year % 100}",
                    Values.SYNTH_DATE_OFFSET + Values.SYNTH_DATE_WIDTH / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered
                )
                canvas.drawText(
                    f.aircraft,
                    Values.SYNTH_TYPE_OFFSET + 6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallText
                )
                canvas.drawText(
                    (f.simTime/60).toString(),
                    Values.SYNTH_TIME_MINS_OFFSET -6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextRight
                )
                canvas.drawText(
                    (f.simTime%60).toString(),
                    Values.SYNTH_TIME_MINS_OFFSET + Values.SYNTH_TIME_MINS_WIDTH / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered)

                totalsForward.simTime += f.simTime
                currentTotals.simTime += f.simTime
            }
            else { // ie. if not sim:
                canvas.drawText(
                    (f.nightTime/60).toString(),
                    Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET -6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextRight)
                canvas.drawText(
                    (f.nightTime%60).toString(),
                    Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET + Values.CONDITIONAL_TIME_NIGHT_MINS_WIDTH / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered)
                totalsForward.nightTime += f.nightTime
                currentTotals.nightTime += f.nightTime

                canvas.drawText(
                    (f.ifrTime/60).toString(),
                    Values.CONDITIONAL_TIME_IFR_MINS_OFFSET -6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextRight)
                canvas.drawText(
                    (f.ifrTime%60).toString(),
                    Values.CONDITIONAL_TIME_IFR_MINS_OFFSET + Values.CONDITIONAL_TIME_IFR_MINS_WIDTH / 2,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                    Paints.smallTextCentered)
                totalsForward.ifrTime += f.ifrTime
                currentTotals.ifrTime += f.ifrTime

                if (f.pic) {
                    canvas.drawText(
                        f.correctedDuration.toHours().toString(),
                        Values.PILOT_FUNC_PIC_MINS_OFFSET - 6,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight)
                    canvas.drawText(
                        (f.correctedDuration.toMinutes()%60).toString(),
                        Values.PILOT_FUNC_PIC_MINS_OFFSET + Values.PILOT_FUNC_PIC_MINS_WIDTH / 2,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered)
                    totalsForward.picTime += f.correctedDuration.toMinutes().toInt()
                    currentTotals.picTime += f.correctedDuration.toMinutes().toInt()

                }
                if (f.coPilot) {
                    canvas.drawText(
                        f.correctedDuration.toHours().toString(),
                        Values.PILOT_FUNC_COPILOT_MINS_OFFSET - 6,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight)
                    canvas.drawText(
                        (f.correctedDuration.toMinutes()%60).toString(),
                        Values.PILOT_FUNC_COPILOT_MINS_OFFSET + Values.PILOT_FUNC_COPILOT_MINS_WIDTH / 2,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered)
                    totalsForward.copilotTime += f.correctedDuration.toMinutes().toInt()
                    currentTotals.copilotTime += f.correctedDuration.toMinutes().toInt()
                }
                if (f.dual) {
                    canvas.drawText(
                        f.correctedDuration.toHours().toString(),
                        Values.PILOT_FUNC_DUAL_MINS_OFFSET - 6,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight)
                    canvas.drawText(
                        (f.correctedDuration.toMinutes()%60).toString(),
                        Values.PILOT_FUNC_DUAL_MINS_OFFSET + Values.PILOT_FUNC_DUAL_MINS_WIDTH / 2,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered)
                    totalsForward.dualTime += f.correctedDuration.toMinutes().toInt()
                    currentTotals.dualTime += f.correctedDuration.toMinutes().toInt()
                }
                if (f.instructor) {
                    canvas.drawText(
                        f.correctedDuration.toHours().toString(),
                        Values.PILOT_FUNC_INST_MINS_OFFSET - 6,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextRight)
                    canvas.drawText(
                        (f.correctedDuration.toMinutes()%60).toString(),
                        Values.PILOT_FUNC_INST_MINS_OFFSET + Values.PILOT_FUNC_INST_MINS_WIDTH / 2,
                        Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
                        Paints.smallTextCentered)
                    totalsForward.instructorTime += f.correctedDuration.toMinutes().toInt()
                    currentTotals.instructorTime += f.correctedDuration.toMinutes().toInt()
                }
                if (f.signature.isNotEmpty()){
                    Log.d(TAG, "flightID: ${f.flightID}")
                    val svg = SVG.getFromString(f.signature)
                    svg.documentPreserveAspectRatio= PreserveAspectRatio.LETTERBOX
                    val pict2 = svg.renderToPicture(900, 300)

                    canvas.drawPicture(pict2, RectF(Values.SIGNATURE_OFFSET,Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * (index-1), Values.REMARKS_OFFSET, Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index))
                }
                canvas.drawText(
                    f.remarks,
                    Values.REMARKS_OFFSET + 6,
                    Values.TOP_SECTION_HEIGHT + Values.ENTRY_HEIGHT * index - 6,
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
            Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.nightTime%60).toString(),
            Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET + Values.CONDITIONAL_TIME_NIGHT_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //ifr
        canvas.drawText(
            (currentTotals.ifrTime/60).toString(),
            Values.CONDITIONAL_TIME_IFR_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.ifrTime%60).toString(),
            Values.CONDITIONAL_TIME_IFR_MINS_OFFSET + Values.CONDITIONAL_TIME_IFR_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //pic
        canvas.drawText(
            (currentTotals.picTime/60).toString(),
            Values.PILOT_FUNC_PIC_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.picTime%60).toString(),
            Values.PILOT_FUNC_PIC_MINS_OFFSET + Values.PILOT_FUNC_PIC_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //copilot
        canvas.drawText(
            (currentTotals.copilotTime/60).toString(),
            Values.PILOT_FUNC_COPILOT_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.copilotTime%60).toString(),
            Values.PILOT_FUNC_COPILOT_MINS_OFFSET + Values.PILOT_FUNC_COPILOT_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //dual
        canvas.drawText(
            (currentTotals.dualTime/60).toString(),
            Values.PILOT_FUNC_DUAL_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.dualTime%60).toString(),
            Values.PILOT_FUNC_DUAL_MINS_OFFSET + Values.PILOT_FUNC_DUAL_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //instructor
        canvas.drawText(
            (currentTotals.instructorTime/60).toString(),
            Values.PILOT_FUNC_INST_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.instructorTime%60).toString(),
            Values.PILOT_FUNC_INST_MINS_OFFSET + Values.PILOT_FUNC_INST_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //sim
        canvas.drawText(
            (currentTotals.simTime/60).toString(),
            Values.SYNTH_TIME_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (currentTotals.simTime%60).toString(),
            Values.SYNTH_TIME_MINS_OFFSET + Values.SYNTH_TIME_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)

        //totals previous pages:
        index = 2
        //night
        canvas.drawText(
            (oldTotals.nightTime/60).toString(),
            Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.nightTime%60).toString(),
            Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET + Values.CONDITIONAL_TIME_NIGHT_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //ifr
        canvas.drawText(
            (oldTotals.ifrTime/60).toString(),
            Values.CONDITIONAL_TIME_IFR_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.ifrTime%60).toString(),
            Values.CONDITIONAL_TIME_IFR_MINS_OFFSET + Values.CONDITIONAL_TIME_IFR_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //pic
        canvas.drawText(
            (oldTotals.picTime/60).toString(),
            Values.PILOT_FUNC_PIC_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.picTime%60).toString(),
            Values.PILOT_FUNC_PIC_MINS_OFFSET + Values.PILOT_FUNC_PIC_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //copilot
        canvas.drawText(
            (oldTotals.copilotTime/60).toString(),
            Values.PILOT_FUNC_COPILOT_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.copilotTime%60).toString(),
            Values.PILOT_FUNC_COPILOT_MINS_OFFSET + Values.PILOT_FUNC_COPILOT_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //dual
        canvas.drawText(
            (oldTotals.dualTime/60).toString(),
            Values.PILOT_FUNC_DUAL_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.dualTime%60).toString(),
            Values.PILOT_FUNC_DUAL_MINS_OFFSET + Values.PILOT_FUNC_DUAL_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //instructor
        canvas.drawText(
            (oldTotals.instructorTime/60).toString(),
            Values.PILOT_FUNC_INST_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.instructorTime%60).toString(),
            Values.PILOT_FUNC_INST_MINS_OFFSET + Values.PILOT_FUNC_INST_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //sim
        canvas.drawText(
            (oldTotals.simTime/60).toString(),
            Values.SYNTH_TIME_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (oldTotals.simTime%60).toString(),
            Values.SYNTH_TIME_MINS_OFFSET + Values.SYNTH_TIME_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)


        //total totals:
        index = 3
        //night
        canvas.drawText(
            (totalsForward.nightTime/60).toString(),
            Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.nightTime%60).toString(),
            Values.CONDITIONAL_TIME_NIGHT_MINS_OFFSET + Values.CONDITIONAL_TIME_NIGHT_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //ifr
        canvas.drawText(
            (totalsForward.ifrTime/60).toString(),
            Values.CONDITIONAL_TIME_IFR_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.ifrTime%60).toString(),
            Values.CONDITIONAL_TIME_IFR_MINS_OFFSET + Values.CONDITIONAL_TIME_IFR_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //pic
        canvas.drawText(
            (totalsForward.picTime/60).toString(),
            Values.PILOT_FUNC_PIC_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.picTime%60).toString(),
            Values.PILOT_FUNC_PIC_MINS_OFFSET + Values.PILOT_FUNC_PIC_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //copilot
        canvas.drawText(
            (totalsForward.copilotTime/60).toString(),
            Values.PILOT_FUNC_COPILOT_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.copilotTime%60).toString(),
            Values.PILOT_FUNC_COPILOT_MINS_OFFSET + Values.PILOT_FUNC_COPILOT_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //dual
        canvas.drawText(
            (totalsForward.dualTime/60).toString(),
            Values.PILOT_FUNC_DUAL_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.dualTime%60).toString(),
            Values.PILOT_FUNC_DUAL_MINS_OFFSET + Values.PILOT_FUNC_DUAL_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //instructor
        canvas.drawText(
            (totalsForward.instructorTime/60).toString(),
            Values.PILOT_FUNC_INST_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.instructorTime%60).toString(),
            Values.PILOT_FUNC_INST_MINS_OFFSET + Values.PILOT_FUNC_INST_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
        //sim
        canvas.drawText(
            (totalsForward.simTime/60).toString(),
            Values.SYNTH_TIME_MINS_OFFSET -6,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextRight)
        canvas.drawText(
            (totalsForward.simTime%60).toString(),
            Values.SYNTH_TIME_MINS_OFFSET + Values.SYNTH_TIME_MINS_WIDTH / 2,
            Values.BOTTOM_SECTION_OFFSET + Values.TOTALS_LINE_HEIGHT * index - 6,
            Paints.smallTextCentered)
    }


    fun maxLines(): Int = ((Values.A4_WIDTH - Values.TOP_SECTION_HEIGHT - Values.BOTTOM_SECTION_HEIGHT)/Values.ENTRY_HEIGHT).toInt()
}
 */
