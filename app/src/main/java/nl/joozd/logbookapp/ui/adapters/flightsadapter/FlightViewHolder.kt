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

package nl.joozd.logbookapp.ui.adapters.flightsadapter

import android.view.View
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ItemFlightCardBinding
import nl.joozd.logbookapp.extensions.toLocalTimeString
import nl.joozd.logbookapp.extensions.toMonthYear
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.utils.customs.Swiper

class FlightViewHolder(containerView: View) : ListItemViewHolder(containerView) {
    val binding = ItemFlightCardBinding.bind(containerView)
    override fun bindItem(flight: ModelFlight, onClick: (ModelFlight) -> Unit, onDelete: (ModelFlight) -> Unit) {
        binding.apply {
            with(flight) {
                Swiper(binding.deleteLayer).apply {
                    deleteLayer.setOnClickListener {
                        if (isOpen) {
                            close()
                            onDelete(flight)
                        }
                    }
                }
                flightLayout.setTextViewChildrenColorAccordingstatus(isPlanned, this.checkIfIncomplete())
                dateDayText.text = date().dayOfMonth.toString()
                dateMonthYearText.text = timeOut.toMonthYear()
                namesText.text = namesString()
                aircraftText.text = aircraft.toString()
                remarksText.text = remarks
                flightNumberText.text = flightNumber
                origText.text = orig.displayString()
                destText.text = dest.displayString()
                timeOutText.text = timeOut.toLocalTimeString()
                totalTimeText.text = calculateTotalTime().minutesToHoursAndMinutesString()
                timeInText.text = timeOut.toLocalTimeString()
                takeoffLandingText.text = takeoffLandings.toString()

                isAugmentedText.shouldBeVisible(isAugmented())
                isIFRText.shouldBeVisible(isIfr())
                isDualText.shouldBeVisible(isDual)
                isInstructorText.shouldBeVisible(isInstructor)
                isPicusText.shouldBeVisible(isPICUS)
                isPicText.shouldBeVisible(isPIC)
                isPFText.shouldBeVisible(isPF)
                remarksText.shouldBeVisible(remarks.isNotEmpty())
                flightLayout.translationZ = 10f
                flightLayout.setOnClickListener { onClick(flight) }
            }
        }
    }

    private fun Airport.displayString(): String =
        if (Preferences.useIataAirports)
            iata_code
        else ident
}