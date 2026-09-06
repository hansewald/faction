package de.faction.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.faction.data.model.ImportSource
import de.faction.data.model.OwnedChampion

@Entity(tableName = "roster", indices = [Index("championId")])
data class RosterEntity(
    @PrimaryKey(autoGenerate = true) val instanceId: Long = 0,
    val championId: String,
    val level: Int,
    val ascension: Int,
    val rank: Int,
    val awakeningLevel: Int,
    val power: Int?,
    val locked: Boolean,
    val source: String,
) {
    fun toModel(): OwnedChampion = OwnedChampion(
        instanceId = instanceId,
        championId = championId,
        level = level,
        ascension = ascension,
        rank = rank,
        awakeningLevel = awakeningLevel,
        power = power,
        locked = locked,
        source = runCatching { ImportSource.valueOf(source) }.getOrDefault(ImportSource.MANUAL),
    )

    companion object {
        fun from(model: OwnedChampion) = RosterEntity(
            instanceId = model.instanceId,
            championId = model.championId,
            level = model.level,
            ascension = model.ascension,
            rank = model.rank,
            awakeningLevel = model.awakeningLevel,
            power = model.power,
            locked = model.locked,
            source = model.source.name,
        )
    }
}
