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

package nl.joozd.logbookapp.ui.adapters

/*
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_fligh_oldt.*

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.dataclasses.Flight
import android.widget.TextView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.miscClasses.crew.Crew
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.data.utils.flightAirportsToIata
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.extensions.noColon
import nl.joozd.logbookapp.extensions.toMonthYear
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.getDateStringFromEpochSeconds
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.core.App

@Deprecated ("Use new [FlightsAdapter]")
class FlightsAdapterOld(var allFlights: List<Flight>, private val deleteListener: (Flight) -> Unit, private val itemClick: (Flight) -> Unit) : RecyclerView.Adapter<FlightsAdapterOld.ViewHolder>(),
    RecyclerViewFastScroller.OnPopupTextUpdate, CoroutineScope by MainScope() {
    companion object {
        private var normalColor: Int = App.instance.ctx.getColorFromAttr(android.R.attr.textColorSecondary)
        private var plannedColor: Int = App.instance.ctx.getColorFromAttr(android.R.attr.textColorHighlight)
        private var icaoToIataMap = emptyMap<String, String>()
    }
    init{
        launch (){
            icaoToIataMap = AirportRepository.getInstance().getIcaoToIataMap()
            notifyDataSetChanged()
        }
    }
    private var mRecyclerView: RecyclerView? = null

    // Only scroll to last planned in certain cases (eg. new creation, not on delete or insert)
    var scrollToLastPlanned = true

    /**
     * Make sure new created flight strips start non-swiped in case a recycled one leaves screen swiped
     */
    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.flightLayout.translationX = 0f
        super.onViewAttachedToWindow(holder)
    }


    /**
     * Create reference to recyclerView
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
        normalColor = recyclerView.context.getColorFromAttr(android.R.attr.textColorSecondary)
        plannedColor = recyclerView.context.getColorFromAttr(android.R.attr.textColorHighlight)

        // scroll to first not planned flight
        val firstNotPlanned=allFlights.indexOfFirst { it.isPlanned == 0 }
        recyclerView.scrollToPosition(if (firstNotPlanned > 3) firstNotPlanned - 3 else 0)
    }

    private val icaoIataPairs=  emptyList<Pair<String, String>>()// airportDb.makeIcaoIataPairs()
    // private var allFlights = flightAirportsToIata(enteredFlights, icaoIataPairs)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(R.layout.item_fligh_oldt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindFlight(allFlights[position])
        holder.deleteLayer.setOnClickListener {
            deleteListener(allFlights[position])
            //Animate removal of strip
            holder.containerView.animateToZeroHeight()
        }
        holder.flightLayout.setOnClickListener { itemClick(allFlights[position]) }
    }

    override fun getItemCount(): Int = allFlights.size


    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
        LayoutContainer {
        fun bindFlight(flight: Flight) {
            with(flight) {
                for (c in 0 until flightLayout.childCount) {
                    val v: View = flightLayout.getChildAt(c)
                    if (v is TextView) {
                        v.setTextColor(if (planned) plannedColor else normalColor)
                    }
                }
                dateDayText.text = tOut().dayOfMonth.toString()
                dateMonthYearText.text = tOut().toMonthYear().toUpperCase()
                namesText.text = allNames
                aircraftTypeText.text = aircraft
                remarksText.text = remarks
                if (sim) {
                    flightNumberText.visibility = View.INVISIBLE
                    airportPickerTitle.visibility = View.INVISIBLE
                    arrow1.visibility = View.INVISIBLE
                    arrow2.visibility = View.INVISIBLE
                    timeOutText.visibility = View.INVISIBLE
                    timeInText.visibility = View.INVISIBLE
                    origText.visibility = View.INVISIBLE
                    destText.visibility = View.INVISIBLE
                    simText.visibility = View.VISIBLE
                    takeoffLandingText.visibility = View.INVISIBLE
                    totalTimeText.text = minutesToHoursAndMinutesString(simTime)
                } else {
                    flightNumberText.visibility = View.VISIBLE
                    airportPickerTitle.visibility = View.VISIBLE
                    arrow1.visibility = View.VISIBLE
                    arrow2.visibility = View.VISIBLE
                    timeOutText.visibility = View.VISIBLE
                    timeInText.visibility = View.VISIBLE
                    origText.visibility = View.VISIBLE
                    destText.visibility = View.VISIBLE
                    simText.visibility = View.INVISIBLE

                    takeoffLandingText.visibility = View.VISIBLE

                    flightNumberText.text = flightNumber
                    origText.text = if (Preferences.useIataAirports) icaoToIataMap[orig]?.nullIfEmpty()
                        ?: orig else orig
                    destText.text = if (Preferences.useIataAirports) icaoToIataMap[dest]?.nullIfEmpty()
                        ?: dest else dest
                    timeOutText.text = tOut().noColon()
                    totalTimeText.text = minutesToHoursAndMinutesString(Crew.of(augmentedCrew).getLogTime(((timeIn-timeOut)/60).toInt()))
                    timeInText.text = tIn().noColon()
                    airportPickerTitle.text = registration
                    takeoffLandingText.text = takeoffLanding
                }

                /*
                if (sim) isSimText.setLayoutToOn() else isSimText.setLayoutToOff()
                if (dual) isDualText.setLayoutToOn() else isDualText.setLayoutToOff()
                if (instructor) isInstructorText.setLayoutToOn() else isInstructorText.setLayoutToOff()
                if (picus) isPicusText.setLayoutToOn() else isPicusText.setLayoutToOff()
                if (pic) isPicText.setLayoutToOn() else isPicText.setLayoutToOff()
                if (pf) isPFText.setLayoutToOn() else isPFText.setLayoutToOff()
                */
                val crewValue = Crew.of(augmentedCrew)
                isSimText.visibility = if (sim) View.VISIBLE else View.GONE
                isAugmentedText.visibility = if (crewValue.crewSize <= 2) View.GONE else View.VISIBLE
                isIFRText.visibility= if (ifrTime > 0) View.VISIBLE else View.GONE
                isDualText.visibility = if (dual) View.VISIBLE else View.GONE
                isInstructorText.visibility = if (instructor) View.VISIBLE else View.GONE
                isPicusText.visibility = if (picus) View.VISIBLE else View.GONE
                isPicText.visibility = if (pic) View.VISIBLE else View.GONE
                isPFText.visibility = if (pf) View.VISIBLE else View.GONE
                remarksText.visibility = if (remarks.isEmpty()) View.GONE else View.VISIBLE
            }

        }
    }

    fun scrollToNotPlanned(){
        val firstNotPlanned=allFlights.indexOfFirst { it.isPlanned == 0 }
        mRecyclerView?.scrollToPosition(if (firstNotPlanned > 3) firstNotPlanned - 3 else 0)
    }

    @Deprecated("Do not use this")
    fun insertFlight(flight: Flight) {
        if (allFlights.firstOrNull { it.flightID == flight.flightID } == null) { // its a new flight!
            allFlights += flightAirportsToIata(listOf(flight), icaoIataPairs)
        } else {// its a known flight!
            allFlights = allFlights.map { if (flight.flightID == it.flightID) flightAirportsToIata(listOf(flight), icaoIataPairs).first() else it}
        }
        this.notifyDataSetChanged()
    }

    override fun onChange(position: Int): CharSequence {
        return getDateStringFromEpochSeconds(allFlights[position].timeOut)
    }



}

 */