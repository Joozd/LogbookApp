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
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ItemSimBinding
import nl.joozd.logbookapp.extensions.toMonthYear
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.utils.customs.Swiper
import nl.joozd.logbookapp.ui.utils.toast

class SimViewHolder(containerView: View) : FlightsListItemViewHolder(containerView) {
    val binding = ItemSimBinding.bind(containerView)
    override fun bindItem(
        flight: ModelFlight,
        useIata: Boolean,
        picNameMustBeSet: Boolean,
        onClick: (ModelFlight) -> Unit,
        onLongClick: (ModelFlight) -> Unit,
        onDelete: (ModelFlight) -> Unit
    ) {
        with(binding) {
            with(flight) {
                Swiper(binding.simDeleteLayer).apply {
                    simDeleteLayer.setOnClickListener {
                        if (isOpen) {
                            close()
                            onDelete(flight)
                        }
                    }
                }
                simLayout.setTextViewChildrenColorAccordingstatus(isPlanned)
                simDateDayText.text = date().dayOfMonth.toString()
                simDateMonthYearText.text = timeOut.toMonthYear()
                simNamesText.text = namesString()
                simAircraftTypeText.text = aircraft.type?.shortName ?: ""
                simRemarksText.text = remarks
                simTotalTimeText.text = simTime.minutesToHoursAndMinutesString()
                simTakeoffLandingsText.text = takeoffLandings.toString()

                simLayout.translationZ = 10f

                simLayout.setOnClickListener { onClick(flight) }
                simLayout.setOnLongClickListener { toast(R.string.placeholder); true } // sim does not support long click
            }
        }
    }
}