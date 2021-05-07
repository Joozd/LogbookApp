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

package nl.joozd.logbookapp.data.repository

import kotlinx.coroutines.*
import nl.joozd.joozdlogcommon.BalanceForward
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.BalanceForwardDao

//TODO documentation!
class BalanceForwardRepository(private val balanceforwardDao: BalanceForwardDao, private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope by MainScope()  {

    /**********************************************************************************************
     * Private parts
     **********************************************************************************************/

    private fun saveToDisk(bf: BalanceForward) = launch(dispatcher + NonCancellable) { balanceforwardDao.save(bf) }
    private var deletedBF: BalanceForward? = null

    /**********************************************************************************************
     * Public parts
     **********************************************************************************************/

    suspend fun getAll(): List<BalanceForward> = balanceforwardDao.requestAll()

    fun getLive() = balanceforwardDao.requestLiveBalancesForward()

    /**
     * A BalanceForward with ID -1 gets a new one autoAssigned
     */
    fun save (bf: BalanceForward) {
        if (bf.id != -1) saveToDisk(bf)
        else {
            launch(dispatcher) {
                val highestID = getAll().maxByOrNull { it.id }?.id ?: -1
                save(bf.copy(id = highestID + 1))
            }
        }
    }

    fun save (bff: List<BalanceForward>) = launch(dispatcher + NonCancellable) {
        balanceforwardDao.save(*bff.toTypedArray())
    }

    fun delete(bf: BalanceForward) = launch(dispatcher + NonCancellable) {
        deletedBF = bf
        balanceforwardDao.delete(bf)
    }

    fun undelete(): Boolean {
        deletedBF?.let{
            save(it)
            return true
        } ?: return false
    }

    /**********************************************************************************************
     * Companion object
     **********************************************************************************************/

    companion object {
        @Volatile
        private var singletonInstance: BalanceForwardRepository? = null
        fun getInstance(): BalanceForwardRepository = synchronized(this) {
            singletonInstance
                ?: run {
                    val dataBase = JoozdlogDatabase.getDatabase(App.instance)
                    val balanceForwardDao = dataBase.balanceForwardDao()
                    singletonInstance = BalanceForwardRepository(balanceForwardDao)
                    singletonInstance!!
                }
        }
    }


}