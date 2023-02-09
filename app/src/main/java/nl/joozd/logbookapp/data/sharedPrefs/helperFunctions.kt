package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Toggle the value of a Pref<Boolean> from true to false or vice versa.
 */
fun JoozdlogSharedPreferenceDelegate.Pref<Boolean>.toggle(){
    MainScope().launch {
        setValue(!invoke())
    }
}