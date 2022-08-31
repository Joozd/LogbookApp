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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

//TODO get rid of LiveData, use Flow.
class CloudSyncTermsDialogViewModel: JoozdlogDialogViewModel() {
    private val _text = MutableLiveData(App.instance.resources.openRawResource(R.raw.joozdlog_cloud_terms).use { it.reader().readText() })
    private val _waitedLongEnough = MutableLiveData(false)


    val text: LiveData<String>
        get() = _text

    val waitedLongEnough: LiveData<Boolean>
        get() = _waitedLongEnough

    init{
        viewModelScope.launch {
            Log.d("WATING....", "started! (${waitedLongEnough.value})")
            delay(10000)
            Log.d("WAITED....", "long enough! (${waitedLongEnough.value})")
            _waitedLongEnough.value = true
            Log.d("WAITED....", "done! (${waitedLongEnough.value})")
        }
    }

    fun scrolledToBottom(){
        _waitedLongEnough.value = true
    }
}