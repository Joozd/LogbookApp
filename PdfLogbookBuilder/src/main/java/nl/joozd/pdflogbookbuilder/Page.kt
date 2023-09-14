package nl.joozd.pdflogbookbuilder

import com.itextpdf.text.PageSize


abstract class Page {
    // I might make a version that takes variable paper sizes, but for now its all A4 and fixed font sizes.

    /**
     * Get width of a cell. Cells are 0-indexed.
     */
    fun getWidthOfCell(cellNumber: Int, widths: FloatArray): Float {
        val columnsWeight = widths.sum()
        val columnFranction = widths[cellNumber] / columnsWeight
        val pageWidth = PageSize.A4.height - 2*36f // page is rotated when we use it so width = height
        return columnFranction * pageWidth
    }

    /**
     * Get height of a cell. Might be a bit on the small side so images won't make it bigger when using this
     */
    fun getHeightOfCell(rows: Int): Float {
        val margin = 10f // add 10 points margin so cells are actually a bit bigger than this will return
        val rowsPerPage = 5 + 27 + 6

        val pageHeight = PageSize.A4.width - 2*36f // page is rotated when we use it so height = width
        return rows * (pageHeight - margin) / rowsPerPage
    }
}