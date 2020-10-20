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

package nl.joozd.joozdlogfiletypedetector

import nl.joozd.joozdlogfiletypedetector.interfaces.FileTypeDetector
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import java.io.InputStream

/**
 * WORK IN PROGRESS
 */
class PdfTypeDetector(inputStream: InputStream): FileTypeDetector {
    private val reader = PdfReader(inputStream)
    private val firstPage = PdfTextExtractor.getTextFromPage(reader, 1, SimpleTextExtractionStrategy())

    override val seemsValid = firstPage.isNotEmpty()
    override val typeOfFile = getType(firstPage)

    //for debugging
    override val debugData: String
        get() = firstPage


    /**
     * Returns the type of PDF found, or UNSUPPORTED
     */
    private fun getType(firstPage: String): SupportedTypes {
        val lines = firstPage.split('\n')
        return when {
            match(lines, TypeIdentifiers.KLC_ROSTER) -> SupportedTypes.KLC_ROSTER
            match(lines, TypeIdentifiers.KLC_BRIEFING_SHEET) -> SupportedTypes.KLC_CHECKIN_SHEET
            match(lines, TypeIdentifiers.KLC_MONTHLY) -> SupportedTypes.KLC_MONTHLY
            match(lines, TypeIdentifiers.KLM_ICA_ROSTER) -> SupportedTypes.KLM_ICA_ROSTER // this can cascade further if other types also start wiith this
            match(lines, TypeIdentifiers.KLM_ICA_MONTHLY) -> SupportedTypes.KLM_ICA_MONTHLY
            else -> SupportedTypes.UNSUPPORTED_PDF
        }
    }

    /**
     * This needs to know which line to look at
     */
    private fun match(lines: List<String>, identifier: Pair<*, *>) = when {
        identifier.first is Int && identifier.second is String -> lines[identifier.first as Int].startsWith(identifier.second as String)
        identifier.first is String && identifier.second is Boolean -> matchAnyLine(lines, identifier.first as String, identifier.second as Boolean)
        else -> throw(IllegalArgumentException("identifier pair not supported"))
    }

    /**
     * This will identify
     * @param strict If set to true, line will need to be an exact match (will be trimmed though. If false, 'in' is enough
     * @param lines Lines to check
     * @param identifier String to check against those lines
     */
    private fun matchAnyLine(lines: List<String>, identifier: String, strict: Boolean = false) = if (strict) lines.any{identifier.trim() == it.trim()} else  lines.any{identifier in it}

    private fun checkExtraLine(lines: List<String>, extraLine: String): Boolean = extraLine in lines.joinToString("\n")


}
