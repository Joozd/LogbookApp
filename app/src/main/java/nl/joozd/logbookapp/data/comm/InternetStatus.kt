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

package nl.joozd.logbookapp.data.comm

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App


/**
 * Gives observable variables about internet status.
 * Coroutines used to insert LiveData updating onto main thread.
 *
 * Observable variables:
 * [internetAvailableLiveData]: true if at least one network available with NET_CAPABILITY_INTERNET
 */
object InternetStatus: CoroutineScope by MainScope() {
    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    // context. Can get it wherever you want, I get it from my App class
    private val context = App.instance
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


    /**
     * Mutable livedata
     */

    // This is the one that gets changed, immutable one is exposed for observing as `internetAvailable`
    // wanted to name it _internetAvailable, but compiler complained.
    private val mutableInternetAvailable = MutableLiveData<Boolean>()

    /**
     * Local variables
     */

    private val onlineNetworks = mutableListOf<Network>()

    /**
     * Register listeners
     */
    init{
        val request = NetworkRequest.Builder().apply{
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.build()

        connectivityManager.registerNetworkCallback(request, object: ConnectivityManager.NetworkCallback(){
            override fun onAvailable(network: Network) {
                onlineNetworks.add(network)
                if (Looper.myLooper() == Looper.getMainLooper()) // make sure this is set on main thread
                    mutableInternetAvailable.value = onlineNetworks.isNotEmpty()
                else launch {
                    mutableInternetAvailable.value = onlineNetworks.isNotEmpty()
                }
            }

            override fun onLost(network: Network) {
                onlineNetworks.remove(network)
                if (Looper.myLooper() == Looper.getMainLooper()) // make sure this is set on main thread
                    mutableInternetAvailable.value = onlineNetworks.isNotEmpty()
                else launch {
                    mutableInternetAvailable.value = onlineNetworks.isNotEmpty()
                }
            }
        })
    }


    /*******************************************************************************************
     * Observables
     *******************************************************************************************/

    // This is the one to watch. Immutable version of mutableInternetAvailable.
    val internetAvailableLiveData: LiveData<Boolean>
        get() = mutableInternetAvailable

    //If you need a snapshot
    val internetAvailable: Boolean?
        get() = mutableInternetAvailable.value
}