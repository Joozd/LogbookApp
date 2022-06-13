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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityNewUserPageIntroBinding
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel

/**
 * Page 0 is just a basic introduction saying welcome to the new user
 */
class NewUserActivityIntroPage(override var pageNumber: Int? = null): NewUseractivityPage() {
    constructor(): this(null)

    val viewModel: NewUserActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ActivityNewUserPageIntroBinding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_intro, container, false)).apply {
        continueButton.setOnClickListener { continueClicked() }
    }.root
}