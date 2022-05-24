package nl.joozd.logbookapp.core.usermanagement

import nl.joozd.logbookapp.data.sharedPrefs.Prefs

class UsernameWithKey(val username: String, val key: ByteArray){
    companion object{
        fun fromPrefs(): UsernameWithKey?{
            val n = Prefs.username
            val k = Prefs.key
            if (n == null || k == null) {
                TODO("Handle no login data present")
                return null
            }
            return UsernameWithKey(n, k)
        }
    }
}