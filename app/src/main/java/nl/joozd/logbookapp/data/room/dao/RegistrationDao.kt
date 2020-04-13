package nl.joozd.logbookapp.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import nl.joozd.logbookapp.data.dataclasses.AircraftRegistrationWithType

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM AircraftRegistrationWithType")
    suspend fun requestAllRegistrations(): List<AircraftRegistrationWithType>

    @Query("SELECT * FROM AircraftRegistrationWithType")
    fun requestLiveRegistrations(): LiveData<List<AircraftRegistrationWithType>>
}