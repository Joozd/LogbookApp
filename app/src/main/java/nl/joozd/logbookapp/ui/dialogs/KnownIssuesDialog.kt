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

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogTextDisplayBinding
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.utils.DispatcherProvider

/**
 * Fragment for displaying text
 * If using LiveData for filling title/etxt, it will go stale on recreation and use last observed data.
 */
class KnownIssuesDialog: JoozdlogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogTextDisplayBinding.bind(inflater.inflate(R.layout.dialog_text_display, container, false)).apply {
            displayTextDialogTitle.text = requireActivity().getString(R.string.joozdlog_todo_title)
            lifecycleScope.launch {
                displayTextDialogTextview.text = resources.openRawResource(R.raw.joozdlog_todo_list).use{
                    withContext(DispatcherProvider.io()) { it.reader().readText() }
                }
            }

            headerLayout.setOnClickListener {  } // catch clicks
            bodyLayout.setOnClickListener {  } // catch clicks

            textDisplayDialogBackground.setOnClickListener {
                requireActivity().supportFragmentManager.popBackStack()
            }

            okButton.setOnClickListener {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }.root
}