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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.CloudSyncTermsDialog
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.databinding.ActivityNewUserPageCloudBinding
import nl.joozd.logbookapp.ui.utils.toast

/**
 * Create new user!

 */
class NewUserActivityCloudPage: JoozdlogFragment() {
    val viewModel: NewUserActivityViewModel by activityViewModels()

    val pageNumber = NewUserActivityViewModel.PAGE_CLOUD

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ActivityNewUserPageCloudBinding.bind(inflater.inflate(R.layout.activity_new_user_page_cloud, container, false)).apply {
            useCloudCheckbox.isChecked = Preferences.useCloud

            /*******************************************************************************************
             * OnClickedListeners
             *******************************************************************************************/

            useCloudCheckbox.setOnClickListener {
                viewModel.useCloudCheckboxClicked()
                useCloudCheckbox.isChecked = Preferences.useCloud
            }

            /*******************************************************************************************
             * Observers:
             *******************************************************************************************/

            /**
             * set [ActivityNewUserPageCloudBinding.useCloudCheckbox] according to if it should be set
             */
            viewModel.useCloudCheckboxStatus.observe(viewLifecycleOwner){
                useCloudCheckbox.isChecked = it
            }

            viewModel.getFeedbackChannel(pageNumber).observe(viewLifecycleOwner) {
                Log.d("Event!", "${it.type}, already consumed: ${it.consumed}")
                when (it.getEvent()) {
                    NewUserActivityEvents.NOT_IMPLEMENTED -> {
                        toast("Not implemented!")
                    }
                    NewUserActivityEvents.SHOW_TERMS_DIALOG -> supportFragmentManager.commit {
                        add(R.id.newUserActivityLayout, CloudSyncTermsDialog())
                        addToBackStack(null)
                    }
                }
            }
        }.root

    companion object{
        private const val USERNAME_BUNDLE_KEY = "USERNAME"
    }
}