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
 * TODO: This page will save data that might not have been saved yet:
 * TODO: - Email address if still set and not the same as [Preferences.emailAddress] must be set
 * TODO: - Start a sync with server
 * TODO:     * Sync should check for username entered, if not request a new username from server. User cannot pick own name anymore as that didn't make sense anyway.
 * TODO: User cannot pick own username. Only thing to do here is accept terms (nested scrollview) and a switch stating that we want an account.
 * TODO: This will set [Preferences.useCloud] to true. If that is set to true but username is [Preferences.USERNAME_NOT_SET], Cloud will request a new username from server.
 * TODO With that username, it will make an account, check email, and if checked, send a login link to that email for recovery purposes.
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