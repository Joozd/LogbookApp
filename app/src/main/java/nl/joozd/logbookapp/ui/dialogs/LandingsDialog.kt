package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.picker_landings.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.extensions.minusOneWithFloor
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

class LandingsDialog: JoozdlogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        unchangedFlight = flight
        return inflater.inflate(R.layout.picker_landings, container, false).apply{
            //set current values in views:
            setView(this)

            toDayUpButton.setOnClickListener { flight = flight.copy(takeOffDay = flight.takeOffDay + 1) }
            toNightUpButton.setOnClickListener { flight = flight.copy(takeOffNight = flight.takeOffNight + 1) }
            ldgDayUpButton.setOnClickListener { flight = flight.copy(landingDay = flight.landingDay + 1) }
            ldgNightUpButton.setOnClickListener { flight = flight.copy(landingNight = flight.landingNight + 1) }
            autolandUpButton.setOnClickListener { flight = flight.copy(autoLand = flight.autoLand + 1) }

            toDayDownButton.setOnClickListener { flight = flight.copy(takeOffDay = flight.takeOffDay.minusOneWithFloor(0)) }
            toNightDownButton.setOnClickListener { flight = flight.copy(takeOffNight = flight.takeOffNight.minusOneWithFloor(0)) }
            ldgDayDownButton.setOnClickListener { flight = flight.copy(landingDay = flight.landingDay.minusOneWithFloor(0)) }
            ldgNightDownButton.setOnClickListener { flight = flight.copy(landingNight = flight.landingNight.minusOneWithFloor(0)) }
            autolandDownButton.setOnClickListener { flight = flight.copy(autoLand = flight.autoLand.minusOneWithFloor(0)) }



            //catch clicks on empty parts of dialog
            landingCardsWrapper.setOnClickListener {  }

            //set cancel functions
            cancelTextButton.setOnClickListener {
                unchangedFlight?.let {flight = it}
                supportFragmentManager.popBackStack()
            }
            landingPickerBackground.setOnClickListener {
                unchangedFlight?.let {flight = it}
                supportFragmentManager.popBackStack()
            }

            saveTextButon.setOnClickListener {
                supportFragmentManager.popBackStack()
            }

            viewModel.distinctWorkingFlight.observe(viewLifecycleOwner, Observer {setView(this, it)})
        }
    }

    private fun setView(v: View, f: Flight = flight){
        v.autolandField.setText(f.autoLand.toString())
        v.toDayField.setText(f.takeOffDay.toString())
        v.toNightField.setText(f.takeOffNight.toString())
        v.ldgDayField.setText(f.landingDay.toString())
        v.ldgNightField.setText(f.landingNight.toString())
    }
}