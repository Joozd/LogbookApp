package nl.joozd.logbookapp.data.repository

import kotlinx.coroutines.flow.Flow
import nl.joozd.logbookapp.data.dataclasses.BalanceForward

interface BalanceForwardRepository {
    val balanceForwardsFlow: Flow<List<BalanceForward>>

    suspend fun getBalancesForward(): List<BalanceForward>

    /**
     * Save a [BalanceForward] to DB
     * A BalanceForward with ID -1 gets a new one autoAssigned
     * @param balanceForward: The BalanceForward to save
     */
    suspend fun save (balanceForward: BalanceForward)

    /**
     * Save a list of [BalanceForward]s to DB
     * A BalanceForward with ID -1 gets a new one autoAssigned
     * @param balancesForward: The BalanceForwards to save
     */
    suspend fun save (balancesForward: List<BalanceForward>)

    /**
     * Delete a specific BalanceForward from DB
     */
    suspend fun delete(balanceForward: BalanceForward)

    /**
     * Delete a list of BalanceForwards from DB
     */
    suspend fun delete (balancesForward: List<BalanceForward>)

    companion object{
        val instance: BalanceForwardRepository by lazy {
            BalanceForwardRepositoryImpl.instance
        }
    }
}