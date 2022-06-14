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

package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.toggle
import nl.joozd.logbookapp.ui.dialogs.CloudSyncTermsDialog
import nl.joozd.logbookapp.databinding.ActivityNewUserPageCloudBinding

/**
 * Preferences pertaining to cloud.
 * If Cloud is enabled, sync worker will create a new account if needed.
 */
class NewUserActivityCloudPage: NewUseractivityPage() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ActivityNewUserPageCloudBinding.bind(inflater.inflate(R.layout.activity_new_user_page_cloud, container, false)).apply {

            collectCloudTermsAcceptedFlow()
            collectUseCloudFlow()

            acceptTermsCheckbox.setOnClickListener {
                turnOffOrShowTermsDialog()
            }

            useCloudCheckbox.setOnClickListener {
                Prefs.useCloud.toggle()
            }

        }.root

    private fun turnOffOrShowTermsDialog() {
        lifecycleScope.launch {
            if (Prefs.acceptedCloudSyncTerms())
                Prefs.acceptedCloudSyncTerms.toggle()
            else launchCloudSyncTermsDialog()
        }
    }

    private fun ActivityNewUserPageCloudBinding.collectCloudTermsAcceptedFlow() = Prefs.acceptedCloudSyncTerms.flow.launchCollectWhileLifecycleStateStarted{
        println("Prefs.acceptedCloudSyncTerms = $it")
        acceptTermsCheckbox.isChecked = it
        useCloudCheckbox.isEnabled = it
    }

    private fun ActivityNewUserPageCloudBinding.collectUseCloudFlow() = Prefs.useCloud.flow.launchCollectWhileLifecycleStateStarted{
        useCloudCheckbox.isChecked = it
        continueButton.setText(if (it) R.string._continue else R.string.dont_use)
        if(it)
            UserManagement().createNewUserIfNotLoggedIn()
    }

    private fun launchCloudSyncTermsDialog(){
        supportFragmentManager.commit {
            add(R.id.newUserActivityLayout, CloudSyncTermsDialog())
            addToBackStack(null)
        }
    }
}