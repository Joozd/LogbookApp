/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.android.synthetic.main.picker_landings.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.toInt

@Deprecated ("needs new")
class LandingsPickerOld: Fragment() {
    /*
    var currentLandingData: LandingsCounter? = null


    class OnSaveListener (private val f: (landingData: LandingsCounter) -> Unit){
        fun saveLandings(landingData: LandingsCounter){
            f(landingData)
        }
    }

    var onSaveListener: OnSaveListener? = null

     */

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var changesMade=false
        Log.d("LandingDataPicker", "onCreateView started")
        val view: View = inflater.inflate(R.layout.picker_landings, container, false)
/*
        val landingPickerBackground: ConstraintLayout = view.findViewById(R.id.landingPickerBackground)

        val toDayField: EditText = view.findViewById(R.id.toDayField)
        val toNightField: EditText = view.findViewById(R.id.toNightField)
        val ldgDayField: EditText = view.findViewById(R.id.ldgDayField)
        val ldgNightField: EditText = view.findViewById(R.id.ldgNightField)
        val autolandField: EditText = view.findViewById(R.id.autolandField)

        val toDayDownButton: ImageButton = view.findViewById(R.id.toDayDownButton)
        val toDayUpButton: ImageButton = view.findViewById(R.id.toDayUpButton)
        val toNightDownButton: ImageButton = view.findViewById(R.id.toNightDownButton)
        val toNightUpButton: ImageButton = view.findViewById(R.id.toNightUpButton)
        val ldgDayUpButton: ImageButton = view.findViewById(R.id.ldgDayUpButton)
        val ldgDayDownButton: ImageButton = view.findViewById(R.id.ldgDayDownButton)
        val ldgNightUpButton: ImageButton = view.findViewById(R.id.ldgNightUpButton)
        val ldgNightDownButton: ImageButton = view.findViewById(R.id.ldgNightDownButton)
        val autolandDownButton: ImageButton = view.findViewById(R.id.autolandDownButton)
        val autolandUpButton: ImageButton = view.findViewById(R.id.autolandUpButton)

        val saveTextButon: TextView = view.findViewById(R.id.saveTextButon)
        val cancelTextButton: TextView = view.findViewById(R.id.cancelTextButton)
        currentLandingData?.let { ld ->
            Log.d("LandingDataPicker", "CLD not empty")
            autolandField.setText(ld.autoland.toString())
            toDayField.setText(ld.takeOffDay.toString())
            toNightField.setText(ld.takeOffNight.toString())
            ldgDayField.setText(ld.landingDay.toString())
            ldgNightField.setText(ld.landingNight.toString())

            toDayDownButton.setOnClickListener { if (toDayField.text.toInt() > 0) toDayField.setText((toDayField.text.toInt() - 1).toString()); changesMade = true }
            toNightDownButton.setOnClickListener { if (toNightField.text.toInt() > 0) toNightField.setText((toNightField.text.toInt() - 1).toString()); changesMade = true }
            ldgDayDownButton.setOnClickListener { if (ldgDayField.text.toInt() > 0) ldgDayField.setText((ldgDayField.text.toInt() - 1).toString()); changesMade = true }
            ldgNightDownButton.setOnClickListener { if (ldgNightField.text.toInt() > 0) ldgNightField.setText((ldgNightField.text.toInt() - 1).toString()); changesMade = true }
            autolandDownButton.setOnClickListener { if (autolandField.text.toInt() > 0) autolandField.setText((autolandField.text.toInt() - 1).toString()); changesMade = true }

            toDayUpButton.setOnClickListener { toDayField.setText((toDayField.text.toInt() + 1).toString()); changesMade = true }
            toNightUpButton.setOnClickListener { toNightField.setText((toNightField.text.toInt() + 1).toString()); changesMade = true }
            ldgDayUpButton.setOnClickListener { ldgDayField.setText((ldgDayField.text.toInt() + 1).toString()); changesMade = true }
            ldgNightUpButton.setOnClickListener { ldgNightField.setText((ldgNightField.text.toInt() + 1).toString()); changesMade = true }
            autolandUpButton.setOnClickListener { autolandField.setText((autolandField.text.toInt() + 1).toString()); changesMade = true }

            cancelTextButton.setOnClickListener { fragmentManager?.popBackStack() }
            landingPickerBackground.setOnClickListener {
                fragmentManager?.popBackStack()
            }
            view.landingCardsWrapper.setOnClickListener {  }

            saveTextButon.setOnClickListener {
                Log.d("toDayField", toDayField.text.toString())
                Log.d("toNightField", toNightField.text.toString())
                Log.d("ldgDayField", ldgDayField.text.toString())
                Log.d("ldgNightField", ldgNightField.text.toString())
                Log.d("autolandField", autolandField.text.toString())
                if (changesMade) onSaveListener?.saveLandings(LandingsCounter(takeOffDay = toDayField.text.toInt(), landingDay = ldgDayField.text.toInt(), takeOffNight = toNightField.text.toInt(), landingNight = ldgNightField.text.toInt(), autoland = autolandField.text.toInt()))
                fragmentManager?.popBackStack()
            }
        }


 */
        return view
    }
}
