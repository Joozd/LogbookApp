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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.room.JoozdlogDatabase
import nl.joozd.logbookapp.data.room.dao.BalanceForwardDao
import nl.joozd.logbookapp.utils.DispatcherProvider

class BalanceForwardRepositoryImpl private constructor(
    private val balanceforwardDao: BalanceForwardDao
): BalanceForwardRepository {
    /**
     * Deleted BalanceForward for undo purposes
     */
    private var deletedBF: BalanceForward? = null

    /**********************************************************************************************
     * Public parts
     **********************************************************************************************/

    @Deprecated("Deprecated", ReplaceWith("getBalancesForward()"))
    suspend fun getAll(): List<BalanceForward> = withContext (DispatcherProvider.io()) { getBalancesForward() }

    override val balanceForwardsFlow: Flow<List<BalanceForward>>
        get() = balanceforwardDao.balanceForwardFlow()

    override suspend fun getBalancesForward(): List<BalanceForward> =
        balanceForwardsFlow.first()


    override suspend fun save (balanceForward: BalanceForward) =
        save(listOf(balanceForward))

    override suspend fun save (balancesForward: List<BalanceForward>) = withContext(DispatcherProvider.io()) {
        balanceforwardDao.save(*assignIdsIfNeeded(balancesForward).toTypedArray())
    }

    override suspend fun delete(balanceForward: BalanceForward){
        deletedBF = balanceForward
        delete(listOf(balanceForward))
    }

    override suspend fun delete(balancesForward: List<BalanceForward>) = withContext(DispatcherProvider.io()) {
        balanceforwardDao.delete(*balancesForward.toTypedArray())
    }

    // ID -1 gets a new one autoAssigned
    private suspend fun assignIdsIfNeeded(balancesForward: List<BalanceForward>): List<BalanceForward>{
        var highestTaken = (getBalancesForward().maxOfOrNull { it.id } ?: 0)
        return balancesForward.map{
            if (it.id == -1) it.copy(id = ++highestTaken)
            else it
        }
    }


    /**
     * Undo most recent deletion of a balance forward
     * NOTE only the most decent deletion can be undeleted.
     */
    suspend fun undelete(): Boolean {
        deletedBF?.let{
            save(it)
            return true
        } ?: return false
    }

    /**********************************************************************************************
     * Companion object
     **********************************************************************************************/

    companion object {
        val instance: BalanceForwardRepositoryImpl by lazy {
            val dataBase = JoozdlogDatabase.getInstance()
            val balanceForwardDao = dataBase.balanceForwardDao()
            BalanceForwardRepositoryImpl(balanceForwardDao)
        }

        fun mock(db: JoozdlogDatabase) = BalanceForwardRepositoryImpl(db.balanceForwardDao())
    }


}