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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.BuildConfig
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class AboutDialogViewModel: JoozdlogDialogViewModel() {
    private val _text = MutableLiveData<String>().apply{
        viewModelScope.launch{
            value = withContext(Dispatchers.IO) {App.instance.resources.openRawResource(R.raw.about_joozdlog)
                .reader()
                .readText()
                .replace(VERSION_STRING, BuildConfig.VERSION_NAME)
                .replace(BUILD_STRING, BuildConfig.VERSION_CODE.toString())
            }
        }
    }

    val text: LiveData<String>
        get() = _text

    companion object{
        const val VERSION_STRING = "\$VERSION\$"
        const val BUILD_STRING = "\$BUILD\$"

    }
}