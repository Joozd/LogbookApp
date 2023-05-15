package nl.joozd.logbookapp.extensions

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable

/**
 * Gets parcelable in correct way from SDK 33 and up, or the old way for lower SDK versions
 */
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}