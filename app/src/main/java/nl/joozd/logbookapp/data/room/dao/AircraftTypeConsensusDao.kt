package nl.joozd.logbookapp.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import nl.joozd.logbookapp.data.dataclasses.AircraftTypeConsensus

@Dao
interface AircraftTypeConsensusDao {
    @Query("SELECT * FROM AircraftTypeConsensus")
    suspend fun getAllConsensusData(): List<AircraftTypeConsensus>

    @Query("SELECT * FROM AircraftTypeConsensus")
    fun getLiveConsensusData(): LiveData<List<AircraftTypeConsensus>>

    @Query("SELECT * FROM AircraftTypeConsensus WHERE registration = :reg LIMIT 1")
    suspend fun getConsensus(reg: String): AircraftTypeConsensus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsensusData(vararg consensus: AircraftTypeConsensus)

    @Query ("DELETE FROM AircraftTypeConsensus")
    suspend fun clearDb()
}