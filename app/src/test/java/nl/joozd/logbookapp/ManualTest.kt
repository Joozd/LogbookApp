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

package nl.joozd.logbookapp

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.File
import java.lang.StringBuilder

class ManualTest {
    @Test
    fun manualTest() {
        val file = File("C:\\temp\\joozdlog\\klc_idp.pdf")
        val reader = PdfReader(file.inputStream())
        var completeStringBuilder = StringBuilder()
        repeat(reader.numberOfPages) { pageNumber ->
            val pageText = PdfTextExtractor.getTextFromPage(
                reader,
                pageNumber + 1,
                SimpleTextExtractionStrategy()
            ).trim()
            completeStringBuilder.append(pageText)
        }
        println(completeStringBuilder)

    }

}