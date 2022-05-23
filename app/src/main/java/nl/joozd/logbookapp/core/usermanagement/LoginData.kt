package nl.joozd.logbookapp.core.usermanagement

import nl.joozd.logbookapp.data.sharedPrefs.Prefs

class LoginData(val username: String, val key: ByteArray){
    companion object{
        fun fromPrefs(): LoginData?{
            val n = Prefs.username
            val k = Prefs.key
            if (n == null || k == null) {
                TODO("Handle no login data present")
                return null
            }
            return LoginData(n, k)
        }
    }
}