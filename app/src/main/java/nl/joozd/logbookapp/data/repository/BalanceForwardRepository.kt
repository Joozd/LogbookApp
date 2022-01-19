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

package nl.joozd.logbookapp.data.repository

import kotlinx.coroutines.*
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.BalanceForwardDao

class BalanceForwardRepository private constructor(private val balanceforwardDao: BalanceForwardDao, private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope by MainScope()  {

    /**********************************************************************************************
     * Private parts
     **********************************************************************************************/

    /**
     * Save a [BalanceForward] to DB
     */
    private fun saveToDisk(bf: BalanceForward) = launch(dispatcher + NonCancellable) { balanceforwardDao.save(bf) }

    /**
     * Deleted BalanceForward for undo purposes
     */
    private var deletedBF: BalanceForward? = null

    /**********************************************************************************************
     * Public parts
     **********************************************************************************************/

    suspend fun getAll(): List<BalanceForward> = balanceforwardDao.requestAll()

    /**
     * Get LiveData reference to all BalanceForwards
     */
    fun getLive() = balanceforwardDao.requestLiveBalancesForward()

    /**
     * Save a BalanceForward to DB
     * A BalanceForward with ID -1 gets a new one autoAssigned
     * @param bf: The BalanceForward to save
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

    /**
     * Save a list of BalanceForwards
     * @param bff: Balance Forwards
     */
    fun save (bff: List<BalanceForward>) = launch(dispatcher + NonCancellable) {
        balanceforwardDao.save(*bff.toTypedArray())
    }

    /**
     * Delete a specific BalanceForward from DB
     */
    fun delete(bf: BalanceForward) = launch(dispatcher + NonCancellable) {
        deletedBF = bf
        balanceforwardDao.delete(bf)
    }

    /**
     * Undo most recent deletion of a balance forward
     * NOTE only the most decent deletion can be undeleted.
     */
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
                    val dataBase = JoozdlogDatabase.getInstance()
                    val balanceForwardDao = dataBase.balanceForwardDao()
                    singletonInstance = BalanceForwardRepository(balanceForwardDao)
                    singletonInstance!!
                }
        }
    }


}