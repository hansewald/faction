package de.faction.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Rarity(val stars: Int, val label: String) {
    COMMON(1, "Gewöhnlich"),
    UNCOMMON(2, "Ungewöhnlich"),
    RARE(3, "Selten"),
    EPIC(4, "Episch"),
    LEGENDARY(5, "Legendär");
}

enum class Affinity(val label: String) {
    MAGIC("Magie"), FORCE("Kraft"), SPIRIT("Geist"), VOID("Leere");
}

enum class Role(val label: String) {
    ATTACK("Angriff"), DEFENSE("Verteidigung"), HP("LP"), SUPPORT("Unterstützung");
}

/**
 * Wirkungen, die ein Champion über sein Skill-Kit einbringt. Die Bewertung in
 * [de.faction.domain.ScoreEngine] arbeitet ausschließlich auf diesen Tags,
 * nicht auf fremden Tier-List-Noten.
 */
enum class Utility(val label: String) {
    DECREASE_DEFENSE("Verteidigung senken"),
    DECREASE_ATTACK("Angriff senken"),
    DECREASE_SPEED("Tempo senken"),
    DECREASE_ACCURACY("Genauigkeit senken"),
    WEAKEN("Schwächen"),
    HP_BURN("LP-Verbrennung"),
    POISON("Gift"),
    POISON_SENSITIVITY("Giftempfindlichkeit"),
    STUN("Betäuben / Einfrieren"),
    PROVOKE("Provozieren"),
    HEAL("Heilen"),
    REVIVE("Wiederbeleben"),
    SHIELD("Schild"),
    CLEANSE("Debuffs entfernen"),
    BLOCK_DEBUFFS("Debuffs blocken"),
    INCREASE_SPEED("Tempo erhöhen"),
    INCREASE_ATTACK("Angriff erhöhen"),
    INCREASE_DEFENSE("Verteidigung erhöhen"),
    INCREASE_CRIT_RATE("KritQuote erhöhen"),
    TURN_METER_BOOST("Zugleiste füllen"),
    TURN_METER_DRAIN("Zugleiste leeren"),
    COUNTERATTACK("Gegenangriff"),
    ALLY_ATTACK("Verbündeten-Angriff"),
    EXTRA_TURN("Extra-Zug"),
    IGNORE_DEFENSE("Verteidigung ignorieren"),
    AOE_DAMAGE("Flächenschaden"),
    SINGLE_TARGET_NUKE("Einzelziel-Burst"),
    BLOCK_REVIVE("Wiederbelebung blocken"),
    DECREASE_MAX_HP("Max-LP senken");
}

enum class Area(val label: String) {
    CLAN_BOSS("Clanboss"),
    ARENA("Arena"),
    DUNGEONS("Dungeons"),
    DOOM_TOWER("Turm des Schicksals"),
    FACTION_WARS("Fraktionskriege"),
    CAMPAIGN("Kampagne / Farmen");
}

/** Stammdaten eines Champions — unabhängig davon, ob der Spieler ihn besitzt. */
@Serializable
data class Champion(
    val id: String,
    val name: String,
    val faction: String,
    val rarity: Rarity,
    val affinity: Affinity,
    val role: Role,
    @SerialName("utilities") val utilities: Set<Utility> = emptySet(),
    /** Kurze, eigene Einordnung des Kits in einem Satz. */
    val kitSummary: String = "",
    /** Champions, die früh im Spiel überproportional viel Wert liefern. */
    val earlyGameCarry: Boolean = false,
    /** Aus der Kampagne farmbar — damit als Futter praktisch unbegrenzt verfügbar. */
    val campaignFarmable: Boolean = false,
)
