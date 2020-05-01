package nl.joozd.logbookapp.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import nl.joozd.logbookapp.data.room.model.AircraftTypeConsensusData

@Dao
interface AircraftTypeConsensusDao {
    @Query("SELECT * FROM AircraftTypeConsensusData")
    suspend fun getAllConsensusData(): List<AircraftTypeConsensusData>

    @Query("SELECT * FROM AircraftTypeConsensusData")
    fun getLiveConsensusData(): LiveData<List<AircraftTypeConsensusData>>

    @Query("SELECT * FROM AircraftTypeConsensusData WHERE registration = :reg LIMIT 1")
    suspend fun getConsensus(reg: String?): AircraftTypeConsensusData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsensusData(vararg consensus: AircraftTypeConsensusData)

    @Query ("DELETE FROM AircraftTypeConsensusData")
    suspend fun clearDb()
}