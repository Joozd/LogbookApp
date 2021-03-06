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

package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityNewUserPageFinalBinding
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.activities.SettingsActivityViewModel

/**
 * Final puntjes op de i
 */
class NewUserActivityFinalPage: Fragment() {
    val pageNumber = NewUserActivityViewModel.PAGE_FINAL

    private val viewModel: NewUserActivityViewModel by activityViewModels()

    //TODO don't use this
    private val settingsViewmodel: SettingsActivityViewModel by viewModels() // this handles settings changes pretty well :)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ActivityNewUserPageFinalBinding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_final, container, false)).apply {
            icaoIataSwitch.isChecked = Preferences.useIataAirports
            consensusSwitch.isChecked = Preferences.consensusOptIn

            /*******************************************************************************************
             * OnClickListeners
             *******************************************************************************************/

            icaoIataSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setUseIataAirports(isChecked)
            }
            consensusSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setConsensusOptIn(isChecked)
            }

            /*******************************************************************************************
             * Observers
             *******************************************************************************************/

            viewModel.useIataAirports.observe(viewLifecycleOwner) { useIata ->
                icaoIataSwitch.isChecked = useIata
                // icaoIataSwitch.text = requireActivity().getString(if (useIata) R.string.useIataAirports else R.string.useIcaoAirports)
            }
        }.root
}