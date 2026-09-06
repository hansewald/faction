package de.faction.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RosterEntity::class], version = 1, exportSchema = false)
abstract class RaidDatabase : RoomDatabase() {
    abstract fun rosterDao(): RosterDao

    companion object {
        @Volatile private var instance: RaidDatabase? = null

        fun get(context: Context): RaidDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RaidDatabase::class.java,
                "raid-companion.db",
            ).build().also { instance = it }
        }
    }
}
