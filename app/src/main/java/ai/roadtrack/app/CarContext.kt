package ai.roadtrack.app

/** What the car tells us right now. Defaults assume parked so the app is usable before the SDK answers. */
data class CarSignals(
    val parked: Boolean = true,
    val raining: Boolean = false,
    val night: Boolean = false,
    val passenger: Boolean = false,
)

enum class Mood(val label: String, val ambientArgb: Int) {
    RAIN("비 오는 날", 0xFF5A7BD8.toInt()),
    NIGHT("야간 드라이브", 0xFF7F5AD8.toInt()),
    TOGETHER("함께 듣기", 0xFFF687B3.toInt()),
    DEFAULT("내 플레이리스트", 0xFF4FD1C5.toInt()),
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
