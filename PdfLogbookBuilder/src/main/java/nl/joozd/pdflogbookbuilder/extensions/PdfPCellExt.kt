package nl.joozd.pdflogbookbuilder.extensions

import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable

/*
 * Not just extensions, also  utilities and factories
 */

/**
 * Font size for headers
 */
private const val FONT_SIZE_HEADER = 11f
private const val FONT_SIZE_ENTRY = 10f


fun pdfPCellWithTextShrunkToFit(text: String, cellWidth: Float, minimumFontSize: Float = 6f, initialFontSize: Float = 10f, padding: Float = 1f): PdfPCell{
    val space = cellWidth - 2*padding
    var truncatedText = text
    val font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
    var fontSize = initialFontSize
    // make text smaller until minimum size, or until it fits
    while (fontSize > minimumFontSize && font.getWidthPoint(truncatedText, fontSize) > space) {
        fontSize -= 0.5f
    }

    //make sure we get ellipsis if truncated
    if(font.getWidthPoint(truncatedText, fontSize) > space){
        truncatedText = truncatedText.dropLast(1) + "..."
    }

    while(font.getWidthPoint(truncatedText, fontSize) > space){
        truncatedText = truncatedText.dropLast(4) + "..." // drop 4 since we also drop 3 dots at the end, then add them dots again
    }

    return PdfPCell(Phrase(truncatedText, Font(font, fontSize))).apply{
        isNoWrap = true
    }
}

private val headerFont = Font(Font.FontFamily.HELVETICA, FONT_SIZE_HEADER)
private val entryFont = Font(Font.FontFamily.HELVETICA, FONT_SIZE_ENTRY)

fun headerCell(string: String, columnSpan: Int = 1, rowSpan: Int = 1) = headerCell(Phrase(string, headerFont), columnSpan, rowSpan)
fun cell(string: String, columnSpan: Int = 1, rowSpan: Int = 1) = cell(Phrase(string, entryFont), columnSpan, rowSpan)

private fun headerCell(phrase: Phrase, columnSpan: Int = 1, rowSpan: Int = 1): PdfPCell =
    cell(phrase, columnSpan, rowSpan).apply {
        horizontalAlignment = Element.ALIGN_CENTER
        isNoWrap = false
    }

private fun cell(phrase: Phrase, columnSpan: Int = 1, rowSpan: Int = 1): PdfPCell =
    PdfPCell(phrase).apply{
        horizontalAlignment = Element.ALIGN_CENTER
        colspan = columnSpan
        rowspan = rowSpan
        isNoWrap = true
    }

/**
 * add empty lines
 */
fun PdfPTable.fillWithEmptyLines(amountOfLines: Int, cellsPerLine: Int){
    repeat(amountOfLines){
        repeat(cellsPerLine){
            addCell(emptyCell())
        }
    }
}

/**
 * If [minutes] is not zero, this will insert two cells, one with hours and one with minutes.
 * If it is zero, this will insert two empty cells.
 */
fun PdfPTable.enterTimeInTwoCellsIfNotZero(minutes: Int){
    if(minutes == 0){
        addCell(cell(" "))
        addCell(cell(" "))
    }
    else{
        addCell(cell(minutes.hours()).apply{ horizontalAlignment = Element.ALIGN_RIGHT})
        addCell(cell(minutes.minutes()).apply{ horizontalAlignment = Element.ALIGN_RIGHT})
    }
}

fun emptyCell(): PdfPCell = cell(" ")