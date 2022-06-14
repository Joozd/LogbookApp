package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.sharedPrefs.JoozdlogSharedPreferenceDelegate

fun JoozdlogSharedPreferenceDelegate.Pref<Boolean>.toggle(){
    MainScope().launch {
        setValue(!invoke())
    }
}