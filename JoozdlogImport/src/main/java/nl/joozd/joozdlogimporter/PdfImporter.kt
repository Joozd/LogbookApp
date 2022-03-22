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

package nl.joozd.joozdlogimporter

import nl.joozd.joozdlogimporter.interfaces.FileImporter
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import nl.joozd.joozdlogimporter.supportedFileTypes.*
import java.io.IOException
import java.io.InputStream

/**
 * WORK IN PROGRESS
 */
class PdfImporter(private val lines: List<String>): FileImporter() {
    override fun getFile() = getType(lines)

    /**
     * Returns the SupportedImportType that matches the data in [reader].
     */
    private fun getType(lines: List<String>): ImportedFile =
        KlcRosterFile.buildIfMatches(lines)
            ?: KlcBriefingSheetFile.buildIfMatches(lines)
            ?: KlcMonthlyFile.buildIfMatches(lines)
            ?: KlmIcaRosterFile.buildIfMatches(lines)
            ?: KlmIcaMonthlyFile.buildIfMatches(lines)
            ?: UnsupportedPdfFile(lines)

    companion object{
        @Throws(IOException::class)
        fun ofInputStream(inputStream: InputStream): PdfImporter {
            val reader = PdfReader(inputStream)
            val lines = (1..reader.numberOfPages).map { page ->
                PdfTextExtractor.getTextFromPage(reader, page, SimpleTextExtractionStrategy()).lines()
            }.flatten()
            return PdfImporter(lines)
        }
    }
}


