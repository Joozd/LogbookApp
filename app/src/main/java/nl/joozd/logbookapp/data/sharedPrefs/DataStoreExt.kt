package nl.joozd.logbookapp.data.sharedPrefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun DataStore<Preferences>.getString(key: String, defaultValue: String? = null): String? = runBlocking {
    data.first()[stringPreferencesKey(key)] ?: defaultValue
}

fun DataStore<Preferences>.putString(key: String, value: String?) = MainScope().launch{
    val k = stringPreferencesKey(key)
    edit { p ->
        if (value == null)
            p.remove(k)
        else
            p[k] = value
    }
}