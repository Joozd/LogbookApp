package nl.joozd.logbookapp.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import nl.joozd.logbookapp.data.room.model.AircraftRegistrationWithTypeData

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM AircraftRegistrationWithTypeData")
    suspend fun requestAllRegistrations(): List<AircraftRegistrationWithTypeData>

    @Query("SELECT * FROM AircraftRegistrationWithTypeData")
    fun requestLiveRegistrations(): LiveData<List<AircraftRegistrationWithTypeData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRegistrations(vararg regs: AircraftRegistrationWithTypeData)
}