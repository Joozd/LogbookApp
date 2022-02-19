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

package nl.joozd.joozdlogfiletypedetector

import nl.joozd.joozdlogfiletypedetector.interfaces.FileTypeDetector
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import nl.joozd.joozdlogfiletypedetector.supportedFileTypes.*
import java.io.InputStream

/**
 * WORK IN PROGRESS
 */
class PdfTypeDetector(inputStream: InputStream): FileTypeDetector() {
    private val reader = PdfReader(inputStream)
    private fun lines() =
        (1..reader.numberOfPages).map { page ->
            PdfTextExtractor.getTextFromPage(reader, page, SimpleTextExtractionStrategy()).lines()
        }.flatten()

    override fun getTypeOfFile() = getType(lines())

    /**
     * Returns the SupportedImportType that matches the data in [reader].
     */
    private fun getType(lines: List<String>): SupportedImportTypes =
        KlcRosterFile.buildIfMatches(lines)
            ?: KlcBriefingSheetFile.buildIfMatches(lines)
            ?: KlcMonthlyFile.buildIfMatches(lines)
            ?: KlmIcaRosterFile.buildIfMatches(lines)
            ?: KlmIcaMonthly.buildIfMatches(lines)
            ?: UnsupportedPdf(lines)
}
