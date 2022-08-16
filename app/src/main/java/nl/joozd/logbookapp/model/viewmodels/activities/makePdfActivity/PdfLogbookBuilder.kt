package nl.joozd.logbookapp.model.viewmodels.activities.makePdfActivity

import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.miscClasses.TotalsForward
import nl.joozd.logbookapp.extensions.popFirstNElements
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.pdf.PdfLogbookDrawing
import nl.joozd.logbookapp.utils.pdf.PdfLogbookMakerValues
import java.util.*
import kotlin.math.ceil

class PdfLogbookBuilder(
    private val balancesForward: Collection<BalanceForward>,
    private val flights: Collection<Flight>
) {
    private val progressKeeper = ProgressKeeper()
    private var progressListeners = mutableSetOf<ProgressListener>()

    private var currentPage get() = progressKeeper.currentProgress
    set(it) { progressKeeper.currentProgress = it }

    var cancelled = false; private set

    //Cancelling makes buildLogbook() return a partially completed logbook that should be discarded.
    fun cancel(){
        cancelled = true
    }

    suspend fun buildLogbook(): PdfDocument = withContext (DispatcherProvider.default()) {
        PdfDocument().apply {
            //get all aircraft:
            val allFlights = LinkedList(flights.filter { !it.isPlanned }.sortedBy { it.timeOut })
            val originalListSize = allFlights.size
            val flightsPerPage = PdfLogbookDrawing.maxLines
            val amountOfPagesWithFlights = getAmountOfPages(originalListSize, flightsPerPage)
            println("amountOfPagesWithFlights = $amountOfPagesWithFlights")

            currentPage += insertLeadingPages()
            // plus one because page numbering starts with 1; works nice because some work is also already done by this point
            progressKeeper.numberOfLastPage = currentPage + amountOfPagesWithFlights
            println("progressKeeper.numberOfLastPage = currentPage + amountOfPagesWithFlights - ${progressKeeper.numberOfLastPage} = ${currentPage + amountOfPagesWithFlights}")


            //fill Totals Forward with balance forward totals
            val totalsForward = TotalsForward().apply {
                multiPilot = balancesForward.sumOf { it.multiPilotTime }
                totalTime = balancesForward.sumOf { it.aircraftTime }
                landingDay = balancesForward.sumOf { it.landingDay }
                landingNight = balancesForward.sumOf { it.landingNight }
                nightTime = balancesForward.sumOf { it.nightTime }
                ifrTime = balancesForward.sumOf { it.ifrTime }
                picTime = balancesForward.sumOf { it.picTime }
                copilotTime = balancesForward.sumOf { it.copilotTime }
                dualTime = balancesForward.sumOf { it.dualTime }
                instructorTime = balancesForward.sumOf { it.instructortime }
                simTime = balancesForward.sumOf { it.simTime }
            }


            /**
             * Create flights pages
             */
            while (allFlights.isNotEmpty() && isActive) {
                val currentFlights = allFlights.popFirstNElements(flightsPerPage)
                println("Currently on page $currentPage")

                addPage(++currentPage) {
                    PdfLogbookDrawing(canvas)
                        .drawLeftPage()
                        .fillLeftPage(currentFlights, totalsForward)
                }

                addPage(++currentPage) {
                    PdfLogbookDrawing(canvas)
                        .drawRightPage()
                        .fillRightPage(currentFlights, totalsForward)
                }
            }
            if (!isActive) close()
        }
    }

    fun registerProgressListener(listener: ProgressListener){
        println("Registered $listener")
        progressListeners.add(listener)
    }

    fun unRegisterProgressListener(listener: ProgressListener){
        progressListeners.remove(listener)
    }

    // returns number of inserted pages at the start of this (presumed empty) document
    private fun PdfDocument.insertLeadingPages(): Int {
        var insertedPages = 0
        addPage(++insertedPages) {
            PdfLogbookDrawing(canvas).drawFrontPage()
        }

        addPage(++insertedPages) {
            PdfLogbookDrawing(canvas).drawNamePage()
        }

        addPage(++insertedPages) {
            PdfLogbookDrawing(canvas).drawAddressPage()
        }
        return insertedPages
    }

    private fun getAmountOfPages(originalListSize: Int, flightsPerPage: Int) = ceil(originalListSize.toDouble() / flightsPerPage).toInt()

    private fun PdfDocument.addPage(pageNumber: Int, f: PdfDocument.Page.() -> Unit){
        finishPage(startPage(PdfDocument.PageInfo.Builder(PdfLogbookMakerValues.A4_LENGTH, PdfLogbookMakerValues.A4_WIDTH, pageNumber).create()).apply{
            f()
        })
    }

    private inner class ProgressKeeper{
        var numberOfLastPage = Int.MAX_VALUE
        var currentProgress = 0
        set(it){
            field = it
            println("New progress is $it (out of $numberOfLastPage)")
            val p = currentProgress.toDouble() / numberOfLastPage
            println("Dus p is $p")
            progressListeners.forEach { l ->
                l.onProgressChanged(p)
            }
        }
    }

    fun interface ProgressListener{
        fun onProgressChanged(progress: Double)
    }
}