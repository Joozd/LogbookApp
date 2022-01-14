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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ItemFlightCardBinding
import nl.joozd.logbookapp.databinding.ItemSimBinding
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.model.dataclasses.DisplayFlight
import nl.joozd.logbookapp.ui.utils.customs.Swiper

/**
 * Adapter for RecyclerView for displaying Flights in JoozdLog
 * Needs
 * [itemClick]: Action to be performed onClick on an item
 */
class FlightsAdapter(
    var list: List<DisplayFlight> = emptyList()
): RecyclerViewFastScroller.OnPopupTextUpdate, RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onDelete: (Int) -> Unit = {}
    var itemClick: (Int) -> Unit = {}

    class SimViewHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
        val binding = ItemSimBinding.bind(containerView)
        fun bindItem(flight: DisplayFlight, onClick: (Int) -> Unit, onDelete: (Int) -> Unit) {
            with(binding) {
                with(flight) {
                    Swiper(binding.simDeleteLayer).apply {
                        simDeleteLayer.setOnClickListener {
                            if (isOpen) {
                                close()
                                onDelete(flightID)
                            }
                        }
                    }
                    simLayout.setColorAccordingstatus(planned)
                    simDateDayText.text = dateDay
                    simDateMonthYearText.text = monthAndYear
                    simNamesText.text = names
                    simAircraftTypeText.text = type
                    simRemarksText.text = remarks
                    simTotalTimeText.text = simTime
                    simTakeoffLandingsText.text = takeoffsAndLandings

                    simLayout.translationZ = 10f

                    simLayout.setOnClickListener { onClick(flightID) }
                }
            }
        }
    }

    class FlightViewHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
        val binding = ItemFlightCardBinding.bind(containerView)
        fun bindItem(flight: DisplayFlight, onClick: (Int) -> Unit, onDelete: (Int) -> Unit) {
            with (binding) {
                with(flight) {
                    Swiper(binding.deleteLayer).apply {
                        deleteLayer.setOnClickListener {
                            if (isOpen) {
                                close()
                                onDelete(flightID)
                            }
                        }
                    }
                    flightLayout.setColorAccordingstatus(planned, this.checkIfIncomplete())
                    dateDayText.text = dateDay
                    dateMonthYearText.text = monthAndYear
                    namesText.text = names
                    aircraftText.text = aircraftTextMerged
                    remarksText.text = remarks
                    flightNumberText.text = flightNumber
                    origText.text = orig
                    destText.text = dest
                    timeOutText.text = timeOut
                    totalTimeText.text = totalTime
                    timeInText.text = timeIn
                    takeoffLandingText.text = takeoffsAndLandings

                    isAugmentedText.shouldBeVisible(augmented)
                    isIFRText.shouldBeVisible(ifr)
                    isDualText.shouldBeVisible(dual)
                    isInstructorText.shouldBeVisible(instructor)
                    isPicusText.shouldBeVisible(picus)
                    isPicText.shouldBeVisible(pic)
                    isPFText.shouldBeVisible(pf)
                    remarksText.shouldBeVisible(remarks.isNotEmpty())
                    flightLayout.translationZ = 10f
                    flightLayout.setOnClickListener { onClick(flightID) }
                }
            }
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
    }

    override fun getItemViewType(position: Int): Int = if(list[position].sim) SIM else FLIGHT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType){
            SIM -> (holder as SimViewHolder).bindItem(list[position], itemClick, onDelete)
            FLIGHT -> (holder as FlightViewHolder).bindItem(list[position], itemClick, onDelete)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            SIM -> SimViewHolder(LayoutInflater.from(parent.ctx).inflate(R.layout.item_sim, parent, false))
            FLIGHT -> FlightViewHolder(LayoutInflater.from(parent.ctx).inflate(R.layout.item_flight_card, parent, false))
            else -> error("SelectableStringAdapter error 0001: Type not SIM or FLIGHT")
        }
    }

    fun updateList(l: List<DisplayFlight>){
        list = l
        notifyDataSetChanged()
    }

    companion object{
        const val FLIGHT = 1
        const val SIM = 2
    }

    /**
     * For popup update
     */
    override fun onChange(position: Int): CharSequence {
        return with (list[position]) { monthAndYear}
    }
}