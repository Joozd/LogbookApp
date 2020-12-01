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

package nl.joozd.logbookapp.ui.activities.pdfParserActivity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityPdfParserBinding
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.PdfParserActivityViewModel
import nl.joozd.logbookapp.ui.activities.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialogV1
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.ui.utils.viewPagerTransformers.DepthPageTransformer

/**
 * PdfParserActivity does the following:
 * - Get an intent with a PDF file
 * - read that PDF file
 * - Decide what type it is (KLC Roster, Lufthansa Monthly Overview, etc)
 * - parse that type
 * - do whatever needs to be done with parsed data (insert roster from planned, check flights from monthlies etc)
 * - Fixes conflicts when importing Monthlies
 * - launch MainActivity
 */
class PdfParserActivity : JoozdlogActivity(), CoroutineScope by MainScope() {
    private val viewModel: PdfParserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Preferences.aircraftTypesVersion == 0 || Preferences.airportDbVersion == 0) {
            toast("Cannot import without Airport DB, try again later")
            //TODO try to download it first before failing?
            finish()
        }


        Log.d("PdfParserActivity", "started")
        ActivityPdfParserBinding.inflate(layoutInflater).apply {

            viewModel.runOnce(intent)


            /****************************************************************************************
             * Observers:
             ****************************************************************************************/

            viewModel.feedbackEvent.observe(activity, Observer {
                when (it.getEvent()) {
                    PdfParserActivityEvents.NOT_IMPLEMENTED -> {
                        toast("Not supported yet")
                        closeAndstartMainActivity()
                    }

                    PdfParserActivityEvents.IMPORTING_LOGBOOK -> {
                        pdfParserStatusTextView.text = getString(R.string.working_ellipsis)
                    }

                    PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED -> {
                        longToast("YAAAAY it worked!")
                        closeAndstartMainActivity()
                    }
                    PdfParserActivityEvents.CHRONO_SUCCESSFULLY_ADDED -> {
                        showChronoDialog(it.extraData)
                    }
                    PdfParserActivityEvents.CHRONO_CONFLICTS_FOUND -> {
                        showChronoConflictFixer(this)
                    }
                    PdfParserActivityEvents.SOME_FLIGHTS_FAILED_TO_IMPORT ->{
                        val failedFlights = it.extraData.getStringArrayList(PdfParserActivityViewModel.FAILED_IMPORTS_TAG)
                        longToast("${failedFlights?.size} failed to import!!!!!!1")
                        //TODO make this better
                    }




                    PdfParserActivityEvents.ERROR, PdfParserActivityEvents.FILE_NOT_FOUND -> {
                        toast("Error reading file: ${it.getString()}")
                        closeAndstartMainActivity()
                    }
                    PdfParserActivityEvents.NOT_A_KNOWN_ROSTER, PdfParserActivityEvents.NOT_A_KNOWN_LOGBOOK -> {
                        Log.d("LALALA", "DEBUG POINT 1")
                        JoozdlogAlertDialog().show(activity){
                            Log.d("LALALA", "DEBUG POINT 2")
                            titleResource = R.string.unknown_file_title
                            messageResource = R.string.unknown_file_message
                            setPositiveButton(android.R.string.ok){
                                closeAndstartMainActivity()
                            }
                        }
                    }
                    PdfParserActivityEvents.CALENDAR_SYNC_ENABLED -> {
                        JoozdlogAlertDialog().show(activity) {
                            messageResource = R.string.calendar_update_active
                            setPositiveButton(R.string.always) {
                                viewModel.disableCalendarUntilAfterLastFlight()
                                Preferences.alwaysPostponeCalendarSync = true
                                viewModel.saveFlights()
                            }
                            setNegativeButton(android.R.string.ok) {
                                viewModel.disableCalendarUntilAfterLastFlight()
                                viewModel.saveFlights(finish = false)
                            }
                            setNeutralButton(android.R.string.cancel) {
                                startMainActivity(this@PdfParserActivity)
                            }
                        }
                    }
                    PdfParserActivityEvents.CALENDAR_SYNC_PAUSED -> {
                        JoozdlogAlertDialog().show(activity) {
                            messageResource = R.string.you_can_start_calendar_sync_again
                            setPositiveButton(android.R.string.ok) {
                                closeAndstartMainActivity()
                            }
                        }
                    }
                    null -> {
                    }
                    else -> {
                        toast("SOMETHING NOT IMPLEMENTED HAPPENED")
                        startMainActivity(activity)
                        finish()
                    }

                }
            })

            setContentView(root)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private fun showChronoDialog(extraData: Bundle){
        //TODO use resources
        JoozdlogAlertDialogV1(activity).apply {
            val total =
                extraData.getInt(PdfParserActivityViewModel.TOTAL_FLIGHTS_IN_CHRONO)
            val new = extraData.getInt(PdfParserActivityViewModel.NEW_FLIGHTS)
            val changed =
                extraData.getInt(PdfParserActivityViewModel.ADJUSTED_FLIGHTS)
            title = "Successfully added chrono"
            message =
                "flights in Chrono: $total\nadded new flights: $new\nUpdated flights: $changed"
            setPositiveButton(android.R.string.ok) {
                closeAndstartMainActivity()
            }
        }.show()
    }

    private fun showChronoConflictFixer(binding: ActivityPdfParserBinding) = with (binding) {
        pdfParserProgressBar.visibility = View.GONE
        pdfParserStatusTextView.visibility = View.GONE
        tabLayout.visibility = View.VISIBLE
        viewPager.apply{
            visibility=View.VISIBLE
            adapter = ScreenSlidePagerAdapter(activity, viewModel.parsedChronoData?.conflicts?.size ?: 0)
            setPageTransformer(DepthPageTransformer(TRANSFORMER_MIN_SCALE))
            TabLayoutMediator(binding.tabLayout, this) { _, _ ->
                // empty for now
            }.attach()
        }
    }

    private fun hideChronoConflictfixer(binding: ActivityPdfParserBinding) = with (binding) {
        pdfParserProgressBar.visibility = View.VISIBLE
        pdfParserStatusTextView.visibility = View.VISIBLE
        tabLayout.visibility = View.GONE
        viewPager.visibility = View.GONE
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity, private var availablePages: Int) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = availablePages

        override fun createFragment(position: Int): Fragment = ChronoConflictSolverFragment().apply{
            assignPageNumber(position)
        }
    }

    companion object {
        const val TAG = "PdfParserActivity"
        private const val TRANSFORMER_MIN_SCALE = 0.75f
    }
}



