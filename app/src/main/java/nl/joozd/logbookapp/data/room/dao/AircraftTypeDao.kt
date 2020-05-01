package nl.joozd.logbookapp.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import nl.joozd.logbookapp.data.room.model.AircraftTypeData

@Dao
interface AircraftTypeDao {
    @Query("SELECT * FROM AircraftTypeData")
    fun requestAllAircraftTypes(): List<AircraftTypeData>

    @Query("SELECT * FROM AircraftTypeData")
    fun requestLiveAircraftTypes(): LiveData<List<AircraftTypeData>>

    @Query("SELECT * FROM AircraftTypeData where name = :name LIMIT 1")
    fun getAircraftType(name: String): AircraftTypeData?

    @Query("SELECT * FROM AircraftTypeData where shortName = :name LIMIT 1")
    fun getAircraftTypeFromShortName(name: String): AircraftTypeData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAircraftTypes(vararg aircraftTypeData: AircraftTypeData)

    @Query ("DELETE FROM AircraftTypeData")
    suspend fun clearDb()
}