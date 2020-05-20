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

package nl.joozd.logbookapp.ui.activities

import android.content.Intent
import android.graphics.pdf.PdfDocument
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_export_pdf.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.miscClasses.TotalsForward
import nl.joozd.logbookapp.data.room.Repository
import java.util.concurrent.CountDownLatch

class ExportPdfActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object{
        private const val WRITE_REQUEST_CODE: Int = 1337
        private const val PDF_MIME_TYPE = "application/pdf"
        private const val FILENAME = "logbook.pdf"
        const val TAG ="ExportPdfActivity"
    }
    private val repository = Repository.getInstance()

    private val pdDoc = PdfDocument()
    private val countDownLatch = CountDownLatch(1)
    private val totalsForward = TotalsForward()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WRITE_REQUEST_CODE) {
            val uri = data?.data.also { uri ->
                Log.d(TAG, "Uri: $uri")
                uri?.let { uri ->
                    countDownLatch.await()
                    contentResolver.openOutputStream(uri).use {
                        pdDoc.writeTo(it)
                    }

                    pdDoc.close()


                    val share = Intent(Intent.ACTION_SEND)
                    share.type = PDF_MIME_TYPE
                    share.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(Intent.createChooser(share, "view Logbook"))
                }

            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_pdf)
        progressBar.setProgress(0, true)
/*

        launch{
            //get all aircraft:
            val allAircraft = AircraftDb().requestAllAircraft()
            val aircraftMap = allAircraft.map { it.registration to it }.toMap()

            //get all flights:
            runOnUiThread { workingOnTextView.text = getString(R.string.loadingFlights) }

            //flights to insert are all flights in DB that are not Deleted of Planned
            val allFlights = (repository.requestValidFlights())
            allFlights.forEach{
                if (it.registration in aircraftMap.keys) it.actualAircraft = aircraftMap[it.registration]
            }

            progressBar.setProgress(5, true)
            workingOnTextView.text = getString(R.string.loadingSignatures)

            //filter is bugfix in case somehow an empty string is saved as signature (should not happen anymore but better be sure)
            // val allSignatures = SignatureDb().getAllSignatures().toMap().filter{it.value.isNotEmpty()}
            val balancesForward = BalanceForwardDb().requestAllBalanceForwards()

            // balance forward into logbook
            // TODO: Put this in balance forward // totalsForward.multiPilot = balancesForward.sumBy { it.multi }
            totalsForward.totalTime = balancesForward.sumBy {it.aircraftTime}
            totalsForward.landingDay = balancesForward.sumBy {it.landingDay}
            totalsForward.landingNight = balancesForward.sumBy {it.landingNight}
            totalsForward.nightTime = balancesForward.sumBy {it.nightTime}
            totalsForward.ifrTime = balancesForward.sumBy {it.ifrTime}
            totalsForward.picTime = balancesForward.sumBy {it.picTime}
            totalsForward.copilotTime = balancesForward.sumBy {it.copilotTime}
            totalsForward.dualTime = balancesForward.sumBy {it.dualTime}
            totalsForward.instructorTime = balancesForward.sumBy {it.instructortime}
            totalsForward.simTime = balancesForward.sumBy {it.simTime}

            //update progress bar
            runOnUiThread {
                progressBar.setProgress(10, true)
            }

            //make the PDF
            Log.d(TAG, "AAAAAAAAAAAAAAAAAAAAAAAAAAA")
            pdDoc.flightsToPdf(allFlights)
            Log.d(TAG, "BBBBBBBBBBBBBBBBBBBBBBBBBBBB")
            Log.d(TAG, "${pdDoc.pages.size} pages")

            /**
             * TODO what will happen here:
             * TODO design layout (and think of how this will work for left/right pages)
             * - fill pages untill running out of flights, don;t forget totals on bottom
             * - keep updating progress bar
             * - When done, get PdfDocument to onActivityResult
             */
            countDownLatch.countDown()
        }
        createFile(PDF_MIME_TYPE, FILENAME) // If I am correct this means building of PDF starts while picking file location
    }

    private fun createFile(mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            // Create a file with the requested MIME type.
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    private fun PdfDocument.flightsToPdf(flights: List<Flight>){
        var pageNumber = 1
/*
        var page = startPage(PdfDocument.PageInfo.Builder(Values.A4_LENGTH, Values.A4_WIDTH, pageNumber).create())
        pdfTools.drawFrontPage(page.canvas) // TODO get name from settings for entering here
        finishPage(page)
        pageNumber += 1
        page = startPage(PdfDocument.PageInfo.Builder(Values.A4_LENGTH, Values.A4_WIDTH, pageNumber).create())
        pdfTools.drawSecondPage(page.canvas, Address()) // TODO address can also be not empty, gte from settings
        finishPage(page)
        pageNumber += 1
*/
        var flightsList = flights
        val flightsPerPage = pdfTools.maxLines()
        Log.d(TAG, "$flightsPerPage")
        while (flightsList.isNotEmpty()){
            val currentFlights: List<Flight> = flightsList.take(flightsPerPage)
            flightsList = flightsList.drop(flightsPerPage)
            var page = startPage(PdfDocument.PageInfo.Builder(Values.A4_LENGTH, Values.A4_WIDTH, pageNumber).create()) // left page
            pdfTools.drawLeftPage(page.canvas)
            pdfTools.fillLeftPage(page.canvas, currentFlights, totalsForward)
            finishPage(page)
            pageNumber += 1

            page = startPage(PdfDocument.PageInfo.Builder(Values.A4_LENGTH, Values.A4_WIDTH, pageNumber).create()) // left page
            pdfTools.drawRightPage(page.canvas)
            pdfTools.fillRightPage(page.canvas, currentFlights, totalsForward)
            finishPage(page)
            pageNumber += 1

            runOnUiThread { progressBar.setProgress(100-(flightsList.size.toDouble()/flights.size*90).toInt(), true) }
            Log.d(TAG, "${(flightsList.size.toDouble()/flights.size*90).toInt()}")
        }



 */
    }
}
