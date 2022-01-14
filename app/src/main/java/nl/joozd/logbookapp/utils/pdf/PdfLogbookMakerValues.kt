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

package nl.joozd.logbookapp.utils.pdf

object PdfLogbookMakerValues{
    const val A4_WIDTH=595 // points
    const val A4_LENGTH=842 // points
    // const val TESTSVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" baseProfile=\"tiny\" height=\"304\" width=\"912\"><g stroke-linejoin=\"round\" stroke-linecap=\"round\" fill=\"none\" stroke=\"black\"><path stroke-width=\"16\" d=\"M131,172c0,0 0,0 0,0 \"/><path stroke-width=\"15\" d=\"M131,172c0,-8 -2,-8 0,-15 \"/><path stroke-width=\"10\" d=\"M131,157c7,-20 7,-20 18,-38 \"/><path stroke-width=\"8\" d=\"M149,119c13,-21 11,-23 29,-40 19,-18 21,-19 45,-30 20,-10 22,-10 44,-11 13,0 17,0 26,8 11,9 13,12 15,27 3,28 2,29 -3,57 -5,29 -7,29 -17,56 -5,17 -6,16 -14,33 \"/><path stroke-width=\"9\" d=\"M274,219c-2,5 -4,8 -5,11 \"/><path stroke-width=\"12\" d=\"M269,230c-1,1 0,-2 2,-4 \"/><path stroke-width=\"11\" d=\"M271,226c17,-18 17,-18 36,-35 \"/><path stroke-width=\"8\" d=\"M307,191c36,-33 34,-36 74,-65 26,-18 27,-18 57,-29 17,-6 19,-6 36,-4 12,2 14,3 23,12 14,13 11,16 22,31 9,11 7,14 19,22 17,11 19,14 39,16 26,3 28,0 55,-5 32,-5 31,-10 63,-16 30,-6 30,-6 60,-8 22,-1 22,1 45,3 17,2 17,3 35,6 13,2 13,3 26,4 8,0 8,1 15,-1 \"/><path stroke-width=\"11\" d=\"M876,157c4,-1 6,-2 6,-5 -1,-4 -2,-7 -7,-10 \"/><path stroke-width=\"9\" d=\"M875,142c-27,-15 -28,-19 -59,-25 \"/><path stroke-width=\"8\" d=\"M816,117c-47,-10 -49,-6 -98,-7 -56,-2 -56,-2 -112,0 -62,3 -62,4 -124,10 -61,5 -61,7 -121,12 -50,4 -50,5 -100,7 -40,2 -40,3 -79,1 -19,-1 -19,-2 -37,-7 \"/><path stroke-width=\"9\" d=\"M145,133c-6,-1 -9,-2 -10,-5 \"/><path stroke-width=\"10\" d=\"M135,128c-1,-3 1,-7 5,-8 33,-11 34,-15 69,-18 \"/><path stroke-width=\"8\" d=\"M209,102c45,-4 46,-2 92,4 61,6 61,6 120,20 48,12 47,18 94,31 51,14 52,10 103,24 33,8 34,8 66,19 14,5 13,6 25,13 8,5 10,4 15,11 \"/><path stroke-width=\"9\" d=\"M724,224c3,4 5,7 3,11 \"/><path stroke-width=\"10\" d=\"M727,235c-3,7 -6,7 -13,11 \"/><path stroke-width=\"9\" d=\"M714,246c-10,5 -10,3 -21,7 \"/><path stroke-width=\"11\" d=\"M693,253c0,0 0,0 -1,0 \"/><path stroke-width=\"11\" d=\"M693,253c0,0 0,0 -1,0 \"/></g></svg>"

    const val BOTTOM_SECTION_HEIGHT = 90f
    const val BOTTOM_SECTION_OFFSET = A4_WIDTH - BOTTOM_SECTION_HEIGHT
    const val TOP_SECTION_HEIGHT = 105f

    const val ENTRY_HEIGHT=20f
    const val TOTALS_LINE_HEIGHT = 30f

    // Values for name page
    const val NAME_PAGE_TITLE_HEIGHT = 200f
    const val NAME_PAGE_NAME_HEIGHT = 300f
    const val NAME_PAGE_LICENSE_HEIGHT = 400f
    const val NAME_PAGE_HORIZONTAL_MARGIN = 150f

    // Values for address page
    const val ADDRESS_PAGE_BOX_NUMBER_OF_LINES = 4
    const val ADDRESS_LINE_SPACING = 30f
    const val ADDRESS_PAGE_TITLE_HEIGHT = 150f
    const val ADDRESS_PAGE_BOX_SIDE_MARGIN = 200f
    const val ADDRESS_PAGE_BOX_SIDE_MARGIN_INSIDE_BOX = 40f
    const val ADDRESS_PAGE_BOX_TOP = 220f
    const val ADDRESS_PAGE_BOX_BOTTOM = ADDRESS_PAGE_BOX_TOP + 2* ADDRESS_PAGE_BOX_SIDE_MARGIN_INSIDE_BOX + ADDRESS_PAGE_BOX_NUMBER_OF_LINES * ADDRESS_LINE_SPACING



    // Values for left page
    const val DATE_OFFSET = 0f
    const val DATE_WIDTH = 61f // 781
    const val DEPARTURE_PLACE_OFFSET = DATE_OFFSET + DATE_WIDTH
    const val DEPARTURE_PLACE_WIDTH = 40f //741
    const val DEPARTURE_TIME_OFFSET = DEPARTURE_PLACE_OFFSET + DEPARTURE_PLACE_WIDTH
    const val DEPARTURE_TIME_WIDTH = 40f // 701
    const val ARRIVAL_PLACE_OFFSET = DEPARTURE_TIME_OFFSET + DEPARTURE_TIME_WIDTH
    const val ARRIVAL_PLACE_WIDTH = 40f // 661
    const val ARRIVAL_TIME_OFFSET = ARRIVAL_PLACE_OFFSET + ARRIVAL_PLACE_WIDTH
    const val ARRIVAL_TIME_WIDTH = 40f // 621
    const val AIRCRAFT_MODEL_OFFSET = ARRIVAL_TIME_OFFSET + ARRIVAL_TIME_WIDTH
    const val AIRCRAFT_MODEL_WIDTH = 150f // 491
    const val AIRCRAFT_REGISTRATION_OFFSET = AIRCRAFT_MODEL_OFFSET + AIRCRAFT_MODEL_WIDTH
    const val AIRCRAFT_REGISTRATION_WIDTH = 80f //411
    const val SP_TIME_SE_OFFSET = AIRCRAFT_REGISTRATION_OFFSET + AIRCRAFT_REGISTRATION_WIDTH
    const val SP_TIME_SE_WIDTH = 20f // 391
    const val SP_TIME_ME_OFFSET = SP_TIME_SE_OFFSET + SP_TIME_SE_WIDTH
    const val SP_TIME_ME_WIDTH = 20f // 371
    const val MP_HOURS_OFFSET = SP_TIME_ME_OFFSET + SP_TIME_ME_WIDTH
    const val MP_HOURS_WIDTH = 50f // 321
    const val MP_MINS_OFFSET = MP_HOURS_OFFSET + MP_HOURS_WIDTH
    const val MP_MINS_WIDTH = 20f // 301
    const val TOTAL_HOURS_OFFSET = MP_MINS_OFFSET + MP_MINS_WIDTH
    const val TOTAL_HOURS_WIDTH = 50f // 251
    const val TOTAL_MINS_OFFSET = TOTAL_HOURS_OFFSET + TOTAL_HOURS_WIDTH
    const val TOTAL_MINS_WIDTH = 20f // 231
    const val NAME_OFFSET = TOTAL_MINS_OFFSET + TOTAL_MINS_WIDTH
    const val NAME_WIDTH = 130f // 111
    const val LDG_DAY_OFFSET = NAME_OFFSET + NAME_WIDTH
    const val LDG_DAY_WIDTH = 40f // 71
    const val LDG_NIGHT_OFFSET = LDG_DAY_OFFSET + LDG_DAY_WIDTH
    const val LDG_NIGHT_WIDTH = 41f // 30
    const val TOTAL_WIDTH_LEFT_PAGE = LDG_NIGHT_OFFSET + LDG_NIGHT_WIDTH

    //values for right page
    const val CONDITIONAL_TIME_NIGHT_HOURS_OFFSET = 0f
    const val CONDITIONAL_TIME_NIGHT_HOURS_WIDTH = 51f // 791
    const val CONDITIONAL_TIME_NIGHT_MINS_OFFSET = CONDITIONAL_TIME_NIGHT_HOURS_OFFSET + CONDITIONAL_TIME_NIGHT_HOURS_WIDTH
    const val CONDITIONAL_TIME_NIGHT_MINS_WIDTH = 20f // 771
    const val CONDITIONAL_TIME_IFR_HOURS_OFFSET = CONDITIONAL_TIME_NIGHT_MINS_OFFSET + CONDITIONAL_TIME_NIGHT_MINS_WIDTH
    const val CONDITIONAL_TIME_IFR_HOURS_WIDTH = 50f // 721
    const val CONDITIONAL_TIME_IFR_MINS_OFFSET = CONDITIONAL_TIME_IFR_HOURS_OFFSET + CONDITIONAL_TIME_IFR_HOURS_WIDTH
    const val CONDITIONAL_TIME_IFR_MINS_WIDTH = 20f // 701
    const val PILOT_FUNC_PIC_HOURS_OFFSET = CONDITIONAL_TIME_IFR_MINS_OFFSET + CONDITIONAL_TIME_IFR_MINS_WIDTH
    const val PILOT_FUNC_PIC_HOURS_WIDTH = 50f // 651
    const val PILOT_FUNC_PIC_MINS_OFFSET = PILOT_FUNC_PIC_HOURS_OFFSET + PILOT_FUNC_PIC_HOURS_WIDTH
    const val PILOT_FUNC_PIC_MINS_WIDTH = 20f // 631
    const val PILOT_FUNC_COPILOT_HOURS_OFFSET = PILOT_FUNC_PIC_MINS_OFFSET + PILOT_FUNC_PIC_MINS_WIDTH
    const val PILOT_FUNC_COPILOT_HOURS_WIDTH = 50f // 581
    const val PILOT_FUNC_COPILOT_MINS_OFFSET = PILOT_FUNC_COPILOT_HOURS_OFFSET + PILOT_FUNC_COPILOT_HOURS_WIDTH
    const val PILOT_FUNC_COPILOT_MINS_WIDTH = 20f // 561
    const val PILOT_FUNC_DUAL_HOURS_OFFSET = PILOT_FUNC_COPILOT_MINS_OFFSET + PILOT_FUNC_COPILOT_MINS_WIDTH
    const val PILOT_FUNC_DUAL_HOURS_WIDTH = 50f // 511
    const val PILOT_FUNC_DUAL_MINS_OFFSET = PILOT_FUNC_DUAL_HOURS_OFFSET + PILOT_FUNC_DUAL_HOURS_WIDTH
    const val PILOT_FUNC_DUAL_MINS_WIDTH = 20f // 491
    const val PILOT_FUNC_INST_HOURS_OFFSET = PILOT_FUNC_DUAL_MINS_OFFSET + PILOT_FUNC_DUAL_MINS_WIDTH
    const val PILOT_FUNC_INST_HOURS_WIDTH = 50f // 441
    const val PILOT_FUNC_INST_MINS_OFFSET = PILOT_FUNC_INST_HOURS_OFFSET + PILOT_FUNC_INST_HOURS_WIDTH
    const val PILOT_FUNC_INST_MINS_WIDTH = 20f // 421
    const val SYNTH_DATE_OFFSET = PILOT_FUNC_INST_MINS_OFFSET + PILOT_FUNC_INST_MINS_WIDTH
    const val SYNTH_DATE_WIDTH = 60f // 381
    const val SYNTH_TYPE_OFFSET = SYNTH_DATE_OFFSET + SYNTH_DATE_WIDTH
    const val SYNTH_TYPE_WIDTH = 60f // 321
    const val SYNTH_TIME_HOURS_OFFSET = SYNTH_TYPE_OFFSET + SYNTH_TYPE_WIDTH
    const val SYNTH_TIME_HOURS_WIDTH = 40f // 261
    const val SYNTH_TIME_MINS_OFFSET = SYNTH_TIME_HOURS_OFFSET + SYNTH_TIME_HOURS_WIDTH
    const val SYNTH_TIME_MINS_WIDTH = 20f // 241
    const val SIGNATURE_OFFSET = SYNTH_TIME_MINS_OFFSET + SYNTH_TIME_MINS_WIDTH
    const val SIGNATURE_WIDTH = 70f // 181
    const val REMARKS_OFFSET = SIGNATURE_OFFSET + SIGNATURE_WIDTH
    const val REMARKS_WIDTH = 171f
    const val TOTAL_WIDTH_RIGHT_PAGE = REMARKS_OFFSET + REMARKS_WIDTH



}