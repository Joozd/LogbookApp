package nl.joozd.logbookapp.data.sharedPrefs.utils

import androidx.datastore.preferences.core.*

fun generatePreferencesKey(key: String, defaultValue: Any) =
    when(defaultValue){
        is Boolean -> booleanPreferencesKey(key)
        is Int -> intPreferencesKey(key)
        is Long -> longPreferencesKey(key)
        is Float -> floatPreferencesKey(key)
        is String -> stringPreferencesKey(key)
        else -> throw IllegalArgumentException()
    }