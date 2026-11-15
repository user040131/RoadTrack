package ai.roadtrack.app

/** What the car tells us right now. Defaults assume parked so the app is usable before the SDK answers. */
data class CarSignals(
    val parked: Boolean = true,
    val raining: Boolean = false,
    val night: Boolean = false,
    val passenger: Boolean = false,
)

enum class Mood(val label: String, val ambientArgb: Int) {
    // Muted on purpose: these also tint the cabin and the glass background.
    RAIN("비 오는 날", 0xFF8FB3D9.toInt()),
    NIGHT("야간 드라이브", 0xFF9A97D6.toInt()),
    TOGETHER("함께 듣기", 0xFFE0B48A.toInt()),
    DEFAULT("내 플레이리스트", 0xFF7FC8A0.toInt()),
}

/** Cabin light color while [song] plays: the song's own color wins, else the mood color. */
fun lightFor(song: Song, mood: Mood): Int = song.colorArgb ?: mood.ambientArgb

/** One suggestion per situation; rain wins over night wins over passenger. */
fun suggestMood(s: CarSignals): Mood = when {
    s.raining -> Mood.RAIN
    s.night -> Mood.NIGHT
    s.passenger -> Mood.TOGETHER
    else -> Mood.DEFAULT
}
