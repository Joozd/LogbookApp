package nl.joozd.pdflogbookbuilder

import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.pdf.PdfPTable
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.pdflogbookbuilder.Values.ENTRIES_PER_PAGE
import nl.joozd.pdflogbookbuilder.extensions.cell
import nl.joozd.pdflogbookbuilder.extensions.date
import nl.joozd.pdflogbookbuilder.extensions.enterTimeInTwoCellsIfNotZero
import nl.joozd.pdflogbookbuilder.extensions.fillWithEmptyLines
import nl.joozd.pdflogbookbuilder.extensions.headerCell
import nl.joozd.pdflogbookbuilder.extensions.pdfPCellWithTextShrunkToFit
import nl.joozd.pdflogbookbuilder.extensions.timeIn
import nl.joozd.pdflogbookbuilder.extensions.timeOut
import nl.joozd.pdflogbookbuilder.extensions.toStringIfNotZero
import nl.joozd.pdflogbookbuilder.extensions.totalTime

/**
 * Can receive maximum of 27 flights.
 * @param flights: The flights to be added to this page
 * @param totalsPrevious: Total times from previous pages
 */
internal class LeftPage(private val flights: List<BasicFlight>, private val totalsPrevious: TotalsPrevious): Page() {
    fun addToDocument(document: Document){
        document.add(buildTable())
    }

    /**
     * Cells are added left to right until the row is full.
     * The table has a total of 16 columns for the flight data part, in header and footer some cells span multiple rows and/or columns.
     */
    private fun buildTable(): PdfPTable =PdfPTable(CELLS_PER_LINE).apply{
        widthPercentage = 100f
        setWidths(widths)
        buildHeader()
        insertFlights()
        fillWithEmptyLines(ENTRIES_PER_PAGE - flights.size, CELLS_PER_LINE)
        buildTotals()
    }

    private fun PdfPTable.buildHeader(){
        buildFirstRow()
        addColumnNames()
    }

    private fun PdfPTable.insertFlights(){
        flights.forEach { f ->
            if(f.isSim){
                // sim entries are empty on left page
                fillWithEmptyLines(1, CELLS_PER_LINE)
            }
            else {
                addCell(cell(f.date()))
                addCell(cell(f.orig))
                addCell(cell(f.timeOut()))
                addCell(cell(f.dest))
                addCell(cell(f.timeIn()))
                addCell(cell(f.aircraft))
                addCell(cell(f.registration))
                addCell(cell(" ")) // single pilot not supported
                addCell(cell(" ")) // single pilot not supported
                enterTimeInTwoCellsIfNotZero(f.multiPilotTime)
                enterTimeInTwoCellsIfNotZero(f.totalTime())
                addCell(pdfPCellWithTextShrunkToFit(f.name, getWidthOfCell(13, widths)))
                addCell(cell(f.landingDay.toStringIfNotZero()))
                addCell(cell(f.landingNight.toStringIfNotZero()))
            }
        }
    }

    /**
     * First row: 8 cells, numbered 1 to 8
     */
    private fun PdfPTable.buildFirstRow() {
        var cellID = 1
        addCell(headerCell(cellID++.toString(), columnSpan = 1))                                             // 1: Date
        addCell(headerCell(cellID++.toString(), columnSpan = 2))                                             // 2: Departure
        addCell(headerCell(cellID++.toString(), columnSpan = 2))                                             // 3: Arrival
        addCell(headerCell(cellID++.toString(), columnSpan = 2))                                             // 4: Aircraft
        addCell(headerCell(cellID++.toString(), columnSpan = 4))                                             // 5: SP/MP Time
        addCell(headerCell(cellID++.toString(), columnSpan = 2))                                             // 6: Total Time of Flight
        addCell(headerCell(cellID++.toString(), columnSpan = 1))                                             // 7: Name(s) PIC
        addCell(headerCell(cellID.toString(), columnSpan = 2))                                               // 8: Landings
        // This should complete the first line. Next cell added will start on second line.
    }

    /**
     * Second and third row: Columns names (Date, Departure, Arrival, etc)
     */
    private fun PdfPTable.addColumnNames() {
        addCell(headerCell(DATE, columnSpan = 1, rowSpan = 2))                                    // 1: Date
        addCell(headerCell(DEPARTURE, columnSpan = 2))                                            // 2: Departure
        addCell(headerCell(ARRIVAL, columnSpan = 2))                                              // 3: Arrival
        addCell(headerCell(AIRCRAFT, columnSpan = 2))                                             // 4: Aircraft
        addCell(headerCell(SP_TIME, columnSpan = 2))                                              // 5a: SP Time
        addCell(headerCell(MP_TIME, columnSpan = 2, rowSpan = 2))                                 // 5b: MP Time
        addCell(headerCell(TOTAL_TOF, columnSpan = 2, rowSpan = 2))                               // 6: Total Time of Flight
        addCell(headerCell(NAME, columnSpan = 1, rowSpan = 2))                                    // 7: Name(s) PIC
        addCell(headerCell(LANDINGS, columnSpan = 2))                                             // 8: Landings

        // another row complete. Next row will have 10 cells as some are already occupied

        addCell(cell(PLACE))                                                                        // 2a: Departure place
        addCell(cell(TIME))                                                                         // 2b: Departure time
        addCell(cell(PLACE))                                                                        // 3a: Arrival place
        addCell(cell(TIME))                                                                         // 3b: Arrival time
        addCell(cell(MODEL))                                                                        // 4a: Model
        addCell(cell(REGISTRATION))                                                                 // 4b: Registration
        addCell(cell(SE))                                                                           // 5aa: SP SE
        addCell(cell(ME))                                                                           // 5ab: SP ME
        addCell(cell(DAY))                                                                          // 8a: Landings Day
        addCell(cell(NIGHT))                                                                        // 8b: Landings Night
    }



    /**
     * Build the "Totals" and "from previous pages" lines at the bottom
     */
    private fun PdfPTable.buildTotals(){
        val mpTime = flights.sumOf { it.multiPilotTime }
        val totalTime = flights.sumOf { it.totalTime() }
        val ldgDay = flights.sumOf { it.landingDay }
        val ldgNight = flights.sumOf { it.landingNight }

        addCell(cell("", columnSpan = 4, rowSpan = 3)) // empty box left bottom

        addCell(cell(TOTAL_THIS_PAGE + EMPTY_LINE, columnSpan = 3).apply { horizontalAlignment = Element.ALIGN_RIGHT })
        addCell(cell(" ")) // empty sell, SP not supported
        addCell(cell(" ")) // empty sell, SP not supported
        enterTimeInTwoCellsIfNotZero(mpTime)
        enterTimeInTwoCellsIfNotZero(totalTime)
        addCell(cell(" ")) // empty cell, names do not add up.
        addCell(cell(ldgDay.takeIf{ it > 0}?.toString() ?: " "))    // landings day, empty if 0
        addCell(cell(ldgNight.takeIf{ it > 0}?.toString() ?: " "))  // landings night, empty if 0


        addCell(cell(TOTAL_PREVIOUS + EMPTY_LINE, columnSpan = 3).apply { horizontalAlignment = Element.ALIGN_RIGHT })
        addCell(cell(" ")) // empty sell, SP not supported
        addCell(cell(" ")) // empty sell, SP not supported
        enterTimeInTwoCellsIfNotZero(totalsPrevious.multiPilotTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.totalTime)
        addCell(cell(" ")) // empty cell, names do not add up.
        addCell(cell(totalsPrevious.landingsDay.takeIf{ it > 0}?.toString() ?: " "))
        addCell(cell(totalsPrevious.landingsNight.takeIf{ it > 0}?.toString() ?: " "))

        addCell(cell(TOTAL_TIME + EMPTY_LINE, columnSpan = 3).apply { horizontalAlignment = Element.ALIGN_RIGHT })
        addCell(cell(" ")) // empty sell, SP not supported
        addCell(cell(" ")) // empty sell, SP not supported
        enterTimeInTwoCellsIfNotZero(totalsPrevious.multiPilotTime + mpTime)
        enterTimeInTwoCellsIfNotZero(totalsPrevious.totalTime + totalTime)
        addCell(cell(" ")) // empty cell, names do not add up.
        addCell(cell((totalsPrevious.landingsDay + ldgDay).takeIf{ it > 0}?.toString() ?: " "))
        addCell(cell((totalsPrevious.landingsNight + ldgNight).takeIf{ it > 0}?.toString() ?: " "))
    }

    companion object{
        private const val CELLS_PER_LINE = 16
        // strings:
        private const val DATE = "DATE (dd/mm/yy)"
        private const val DEPARTURE = "DEPARTURE"
        private const val ARRIVAL = "ARRIVAL"
        private const val AIRCRAFT = "AIRCRAFT"
        private const val SP_TIME = "SINGLE-PILOT TIME"
        private const val MP_TIME = "MULTI-PILOT TIME"
        private const val TOTAL_TOF = "TOTAL TIME OF FLIGHT"
        private const val NAME = "NAME(S) PIC "
        private const val LANDINGS = "LANDINGS"

        private const val PLACE = "PLACE"
        private const val TIME = "TIME"
        private const val MODEL = "MODEL"
        private const val REGISTRATION = "REG"
        private const val SE = "SE"
        private const val ME = "ME"
        private const val DAY = "DAY"
        private const val NIGHT = "NIGHT"

        private const val TOTAL_THIS_PAGE = "TOTAL THIS PAGE"
        private const val TOTAL_PREVIOUS = "TOTAL FROM PREVIOUS PAGES"
        private const val TOTAL_TIME = "TOTAL TIME"

        private const val EMPTY_LINE = "\n "

        //font sizes:


        // relative widths of the columns.
        private val widths = floatArrayOf(
            0.8f,     // 1: Date
            0.5f,     // 2a: Departure place
            0.5f,     // 2b: Departure time
            0.5f,     // 3a: Arrival place
            0.5f,     // 3b: Arrival time
            0.7f,     // 4a: Aircraft model
            0.6f,     // 4b: Aircraft registration
            0.25f,     // 5aa SP SE
            0.25f,     // 5ab SP SE
            0.35f,     // 5ba MP hours
            0.2f,     // 5ba MP minutes
            0.35f,     // 6a Total time hours
            0.2f,     // 6b Total time minutes
            1.1f,     // 7: Name(s) PIC
            0.4f,     // 8a: Landings day
            0.4f      // 8a: Landings night
        )
    }
}