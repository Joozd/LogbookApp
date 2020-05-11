package nl.joozd.logbookapp.ui.adapters.flightsadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_flight.*
import kotlinx.android.synthetic.main.item_sim.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.model.dataclasses.DisplayFlight
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.getDateStringFromEpochSeconds

/**
 * Adapter for RecyclerView for displaying Flights in JoozdLog
 * Needs
 * @param itemClick: Action to be performed onClick on an item
 */
class FlightsAdapter(
    var list: List<DisplayFlight> = emptyList()
): RecyclerViewFastScroller.OnPopupTextUpdate, RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onDelete: (Int) -> Unit = {}
    var itemClick: (Int) -> Unit = {}

    class SimViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindItem(flight: DisplayFlight, onClick: (Int) -> Unit, onDelete: (Int) -> Unit) {
            with(flight){
                simLayout.makePlannedColorIfNeeded(planned)
                simDateDayText.text = dateDay
                simDateMonthYearText.text = monthAndYear
                simNamesText.text = names
                simAircraftTypeText.text = type
                simRemarksText.text = remarks
                simTotalTimeText.text = simTime
                simTakeoffLandingsText.text = takeoffsAndLandings

                simLayout.closeIfSwiped()
                simLayout.setOnClickListener { onClick(flightID) }
                simDeleteLayer.setOnClickListener { onDelete(flightID) }
            }
        }
    }
    class FlightViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindItem(flight: DisplayFlight, onClick: (Int) -> Unit, onDelete: (Int) -> Unit) {
            with(flight) {
                flightLayout.makePlannedColorIfNeeded(planned)
                dateDayText.text = dateDay
                dateMonthYearText.text = monthAndYear
                namesText.text = names
                registrationText.text = registration
                aircraftTypeText.text = type
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

                flightLayout.closeIfSwiped()
                flightLayout.setOnClickListener { onClick(flightID) }
                deleteLayer.setOnClickListener { onDelete(flightID) }
            }
        }
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
            FLIGHT -> FlightViewHolder(LayoutInflater.from(parent.ctx).inflate(R.layout.item_flight, parent, false))
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

    override fun onChange(position: Int): CharSequence {
        return with (list[position]) { "$dateDay $monthAndYear"}
    }
}