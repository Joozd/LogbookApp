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
import nl.joozd.logbookapp.databinding.ItemFlightCardBinding
import nl.joozd.logbookapp.extensions.toMonthYear
import nl.joozd.logbookapp.extensions.toTimeString
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.utils.customs.Swiper

class FlightViewHolder(containerView: View) : FlightsListItemViewHolder(containerView) {
    val binding = ItemFlightCardBinding.bind(containerView)
    private var isLongClicked = false
    private var isClicked = false

    override fun bindItem(
        flight: ModelFlight,
        useIata: Boolean,
        picNameMustBeSet: Boolean,
        onClick: (ModelFlight) -> Unit,
        onLongClick: (ModelFlight) -> Unit,
        onDelete: (ModelFlight) -> Unit
    ) {
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
                flightLayout.setTextViewChildrenColorAccordingstatus(isPlanned, this.checkIfIncomplete(picNameMustBeSet))
                dateDayText.text = date().dayOfMonth.toString()
                dateMonthYearText.text = timeOut.toMonthYear()
                namesText.text = namesString()
                aircraftText.text = aircraft.toString()
                remarksText.text = remarks
                flightNumberText.text = flightNumber
                origText.text = orig.displayString(useIata)
                destText.text = dest.displayString(useIata)
                timeOutText.text = timeOut.toTimeString()
                totalTimeText.text = calculateTotalTime().minutesToHoursAndMinutesString()
                timeInText.text = timeIn.toTimeString()
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
                flightLayout.setOnClickListener {
                // make sure onClick doesn't interfere with onLongClick
                    if(isLongClicked) isLongClicked = false
                    else {
                        isClicked = true
                        onClick(flight)
                    }
                }
                flightLayout.setOnLongClickListener {
                    if(isClicked) isClicked = false
                    else {
                        isLongClicked = true
                        onLongClick(flight)
                    }
                    true
                }
            }
        }
    }

    private fun Airport.displayString(useIata: Boolean): String =
        if (useIata)
            iata_code.takeIf{ it.isNotBlank() } ?: ident
        else ident
}