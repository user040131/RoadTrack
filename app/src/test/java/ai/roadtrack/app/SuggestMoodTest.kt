package ai.roadtrack.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestMoodTest {
    @Test fun defaultWhenNothingHappens() = assertEquals(Mood.DEFAULT, suggestMood(CarSignals()))
    @Test fun rainBeatsNightAndPassenger() =
        assertEquals(Mood.RAIN, suggestMood(CarSignals(raining = true, night = true, passenger = true)))
    @Test fun nightBeatsPassenger() = assertEquals(Mood.NIGHT, suggestMood(CarSignals(night = true, passenger = true)))
    @Test fun songColorWinsOverMood() =
        assertEquals(0xFF112233.toInt(), lightFor(Song("a", "b", 0xFF112233.toInt()), Mood.RAIN))
    @Test fun moodColorWhenSongHasNone() = assertEquals(Mood.NIGHT.ambientArgb, lightFor(Song("a", "b"), Mood.NIGHT))
    @Test fun passengerAlone() = assertEquals(Mood.TOGETHER, suggestMood(CarSignals(passenger = true)))
}
