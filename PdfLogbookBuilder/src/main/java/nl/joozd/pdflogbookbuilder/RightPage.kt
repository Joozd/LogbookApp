package nl.joozd.pdflogbookbuilder

import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.pdflogbookbuilder.extensions.cell
import nl.joozd.pdflogbookbuilder.extensions.date
import nl.joozd.pdflogbookbuilder.extensions.emptyCell
import nl.joozd.pdflogbookbuilder.extensions.enterTimeInTwoCellsIfNotZero
import nl.joozd.pdflogbookbuilder.extensions.fillWithEmptyLines
import nl.joozd.pdflogbookbuilder.extensions.headerCell
import nl.joozd.pdflogbookbuilder.extensions.pdfPCellWithTextShrunkToFit
import nl.joozd.pdflogbookbuilder.extensions.signatureAsPng
import nl.joozd.pdflogbookbuilder.extensions.totalTime

internal class RightPage(private val flights: List<BasicFlight>, private val totalsPrevious: TotalsPrevious): Page() {
    fun addToDocument(document: Document){
        document.add(buildTable())
    }

    /**
     * Cells are added left to right until the row is full.
     * The table has a total of 16 columns for the flight data part, in header and footer some cells span multiple rows and/or columns.
     */
    private fun buildTable(): PdfPTable = PdfPTable(CELLS_PER_LINE).apply{
        widthPercentage = 100f
        setWidths(widths)
        buildHeader()
        insertFlights()
        fillWithEmptyLines(Values.ENTRIES_PER_PAGE - flights.size, CELLS_PER_LINE)
        buildTotals()
    }

    private fun PdfPTable.buildHeader(){
        buildFirstRow()
        addColumnNames()
    }

    private fun PdfPTable.insertFlights(){
        flights.forEach { f ->
            // sim flights are empty on left page
            if(f.isSim){
                // first 12 cells are empty for sim
                repeat(12){
                    addCell(emptyCell())
                }
                addCell(cell(f.date()))
                addCell(cell(f.aircraft))
                enterTimeInTwoCellsIfNotZero(f.simTime)
            }
            else {
                enterTimeInTwoCellsIfNotZero(f.nightTime)
                enterTimeInTwoCellsIfNotZero(f.ifrTime)

                val picTime = getPicTime(f)
                enterTimeInTwoCellsIfNotZero(picTime)

                val copilotTime = getCopilotTime(f)
                enterTimeInTwoCellsIfNotZero(copilotTime)

                val dualTime = getDualTime(f)
                enterTimeInTwoCellsIfNotZero(dualTime)

                val instructorTime = getInstructorTime(f)
                enterTimeInTwoCellsIfNotZero(instructorTime)

                // non-sim entries are empty at the sim cells part
                repeat(4){
                    addCell(emptyCell())
                }
            }

            // Both sim and non-sim get remarks and signature fields:
            addCell(pdfPCellWithTextShrunkToFit(f.remarks, getWidthOfCell(16, widths)))

            if (f.signature.isNotBlank()){
                val image = f.signatureAsPng()
                image.scaleToFit(
                    getWidthOfCell(17, widths),
                    getHeightOfCell(1))
                val imageCell = PdfPCell(image)
                addCell(imageCell)
            }
            else addCell(emptyCell())
        }
    }



    private fun PdfPTable.buildFirstRow(){
        var cellID = 9
        addCell(headerCell(cellID++.toString(), columnSpan = 4))                                    // 9: Operational condition time
        addCell(headerCell(cellID++.toString(), columnSpan = 8))                                    // 10: Pilot function time
        addCell(headerCell(cellID++.toString(), columnSpan = 4))                                    // 11: FSTD session
        addCell(headerCell(cellID.toString(), columnSpan = 2))                                      // 12: Remarks and endorsements. Includes signature but that is not named.
    }

    private fun PdfPTable.addColumnNames() {
        addCell(headerCell(OPERATIONAL_CONDITION_TIME, columnSpan = 4))                             // 9: Operational condition time
        addCell(headerCell(PILOT_FUNCTION_TIME, columnSpan = 8))                                    // 10: Pilot function time
        addCell(headerCell(SIMULATOR, columnSpan = 4))                                              // 11: FSTD session
        addCell(headerCell(REMARKS_AND_ENDORSEMENTS_4_LINES, columnSpan = 2, rowSpan = 2))          // 12: Remarks and endorsements. 4 lines for matching with left page.

        addCell(cell(NIGHT, columnSpan = 2))                                                        // 9a: night time
        addCell(cell(IFR, columnSpan = 2))                                                          // 9b: IFR time
        addCell(cell(PIC, columnSpan = 2))                                                          // 10a: PIC time
        addCell(cell(COPILOT, columnSpan = 2))                                                      // 10b: co-pilot time
        addCell(cell(DUAL, columnSpan = 2))                                                         // 10c: dual time
        addCell(cell(INSTRUCTOR, columnSpan = 2))                                                   // 10d: instructor time
        addCell(cell(DATE))                                                                         // 11a: date
        addCell(cell(TYPE))                                                                         // 11b: type
        addCell(cell(TOTAL_TIME_OF_SESSION, columnSpan = 2))                                        // 11c: time of session

    }

    private fun PdfPTable.buildTotals() {
        val nightTime = flights.sumOf { it.nightTime }
        val ifrTime = flights.sumOf { it.ifrTime }
        val picTime = flights.sumOf { getPicTime(it) }
        val coPilotTime = flights.sumOf { getCopilotTime(it) }
        val dualTime = flights.sumOf { getDualTime(it) }
        val instructorTime = flights.sumOf { getInstructorTime(it) }
        val simTime = flights.sumOf { it.simTime }

        // total this page
        enterTimeInTwoCellsIfNotZero(nightTime)
        enterTimeInTwoCellsIfNotZero(ifrTime)
        enterTimeInTwoCellsIfNotZero(picTime)
        enterTimeInTwoCellsIfNotZero(coPilotTime)
        enterTimeInTwoCellsIfNotZero(dualTime)
        enterTimeInTwoCellsIfNotZero(instructorTime)
        addCell(emptyCell())    // sim date
        addCell(emptyCell())    // sim type
        enterTimeInTwoCellsIfNotZero(simTime)
        addCell(cell(I_CERTIFY_2_LINES, columnSpan = 2)) // signature spans both remarks and signature fields. Two lines for alignment with left page.

        // totals previous page
        enterTimeInTwoCellsIfNotZero(totalsPrevious.nightTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.ifrTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.picTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.coPilotTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.dualTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.instructorTime)
        addCell(emptyCell())    // sim date
        addCell(emptyCell())    // sim type
        enterTimeInTwoCellsIfNotZero(totalsPrevious.simTime)

        // signature should span 4 lines and 2 rows.
        // To make sure the rest of the rows are 2 lines high, this is done in two cells that do not have a border in between.
        addCell(cell(PILOTS_SIGNATURE_2_LINES, columnSpan = 2,)
            .apply{
                border = PdfPCell.BOX - PdfPCell.BOTTOM // this is actually a bitwise operation, so BOX AND NOT BOTTOM
            }
        )



        // totals
        enterTimeInTwoCellsIfNotZero(totalsPrevious.nightTime + nightTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.ifrTime + ifrTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.picTime + picTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.coPilotTime + coPilotTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.dualTime + dualTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.instructorTime + instructorTime)
        addCell(emptyCell())    // sim date
        addCell(emptyCell())    // sim type
        enterTimeInTwoCellsIfNotZero(totalsPrevious.simTime + simTime)
        addCell(cell(EMPTY_2_LINES, columnSpan = 2)
            .apply{
                border = PdfPCell.BOX - PdfPCell.TOP // this is actually a bitwise operation, so BOX AND NOT TOP
            }
        ) // signature should span 4 lines and 2 rows
    }

    private fun getPicTime(f: BasicFlight) = if (f.isPIC) f.totalTime() else 0

    private fun getInstructorTime(f: BasicFlight) = if (f.isInstructor) f.totalTime() else 0

    private fun getDualTime(f: BasicFlight) = if (f.isDual) f.totalTime() else 0

    private fun getCopilotTime(f: BasicFlight) = if (f.isPIC) 0 else f.multiPilotTime


    companion object{
        private const val CELLS_PER_LINE = 18

        private const val OPERATIONAL_CONDITION_TIME = "OPERATIONAL CONDITION TIME"
        private const val PILOT_FUNCTION_TIME = "PILOT FUNCTION TIME"
        private const val SIMULATOR = "SIMULATOR"
        private const val REMARKS_AND_ENDORSEMENTS_4_LINES = "REMARKS AND ENDORSEMENTS\n\n\n\n"

        private const val NIGHT = "NIGHT"
        private const val IFR = "IFR"
        private const val PIC = "PIC"
        private const val COPILOT = "CO-PILOT"
        private const val DUAL = "DUAL"
        private const val INSTRUCTOR = "INSTRUC-\nTOR" // needs 2 lines
        private const val DATE = "DATE\n(dd/mm/yy)" // 2 lines is needed for alignment with left page
        private const val TYPE = "TYPE"
        private const val TOTAL_TIME_OF_SESSION = "TOTAL TIME\nOF SESSION"

        private const val I_CERTIFY_2_LINES = "I certify that the entries\n" +
                                      "in this log are true."
        private const val PILOTS_SIGNATURE_2_LINES = "PILOT’S SIGNATURE\n\n"
        private const val EMPTY_2_LINES = "\n\n"


        // relative widths of the columns.
        private val widths = floatArrayOf(
            0.3f,     // 9aa: Operational Night time hours
            0.2f,     // 9ab: Operational Night time minutes
            0.3f,     // 9ba: Operational IFR time hours
            0.2f,     // 9bb: Operational IFR time minutes

            0.3f,     // 10aa: Function PIC time hours
            0.2f,     // 10ab: Function PIC time minutes
            0.3f,     // 10ba: Function copilot time hours
            0.2f,     // 10bb: Function copilot time minutes
            0.3f,     // 10ca: Function dual time hours
            0.2f,     // 10cb: Function dual time minutes
            0.3f,     // 10da: Function instructor time hours
            0.2f,     // 10db: Function instructor time minutes

            0.5f,     // 11a Simulator date
            0.4f,     // 11b Simulator type
            0.4f,     // 11ca: sim time hours, slightly wider for "TOTAL TIME OF SESSION" to fit above.
            0.2f,     // 11cb: sim time minutes

            1.5f,     // 12a: remarks and endorsements
            0.8f,     // 12b: signature
        )
    }
}