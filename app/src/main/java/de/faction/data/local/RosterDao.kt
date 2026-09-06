package de.faction.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RosterDao {
    @Query("SELECT * FROM roster ORDER BY rank DESC, level DESC")
    fun observeAll(): Flow<List<RosterEntity>>

    @Query("SELECT * FROM roster WHERE championId = :championId")
    suspend fun instancesOf(championId: String): List<RosterEntity>

    @Insert
    suspend fun insertAll(entries: List<RosterEntity>): List<Long>

    @Update
    suspend fun update(entry: RosterEntity)

    @Query("DELETE FROM roster WHERE instanceId = :id")
    suspend fun delete(id: Long)

    /** Ersetzt den Kader vollständig — für Importe, die den ganzen Account abbilden. */
    @Query("DELETE FROM roster")
    suspend fun clear()
}
