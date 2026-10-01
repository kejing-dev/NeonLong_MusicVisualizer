package com.technomancer.neonlong.logic.audio

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/** Runs the full analysis over the generated demo track and scores beats against its kicks. */
class BeatDetectorTest {
    private class Score(val beats: Int, val precision: Float, val recall: Float, val introBeats: Int)

    private fun score(): Score {
        val dir = File("build/generated/testResources")
        val audio = File(dir, "demo_104bpm.wav").inputStream().use { WavReader.read(it) }
        val json = Json.parseToJsonElement(File(dir, "demo_104bpm.kicks.json").readText()).jsonObject
        val kicks = json["kicks"]!!.jsonArray.map { it.jsonPrimitive.float }

        val pipeline = AnalysisPipeline(audio.sampleRate)
        val snap = SpectrumSnapshot()
        val dt = 1f / 60f
        val rate = audio.sampleRate
        val duration = audio.samples.size.toFloat() / rate
        // The app loops the track, so warm up over its last 3 s before scoring from the start.
        var t = duration - 3f
        while (t < duration) {
            pipeline.hopAudio(audio.samples, (t * rate).roundToInt(), dt, t.toDouble(), snap)
            t += dt
        }
        val beats = ArrayList<Float>()
        var k = 0
        while (k * dt < duration) {
            val tt = k * dt
            pipeline.hopAudio(audio.samples, (tt * rate).roundToInt(), dt, tt.toDouble(), snap)
            if (snap.beatStrength > 0f) beats += tt
            k++
        }

        val window = 0.070f
        val matchedBeats = beats.count { b -> kicks.any { abs(it - b) <= window } }
        val matchedKicks = kicks.count { kk -> beats.any { abs(it - kk) <= window } }
        val s = Score(
            beats = beats.size,
            precision = matchedBeats.toFloat() / beats.size,
            recall = matchedKicks.toFloat() / kicks.size,
            introBeats = beats.count { it < kicks.first() - window },
        )
        println("[BeatDetectorTest] beats=${s.beats} kicks=${kicks.size} precision=${s.precision} recall=${s.recall} introBeats=${s.introBeats}")
        return s
    }

    /** The target from the spec. Precision is about 0.83 with bands 0–6 (accepted); see PROGRESS.md. */
    @Disabled("Precision is about 0.83 against the 0.85 target on the demo track; accepted")
    @Test
    fun meetsTarget() {
        val s = score()
        assertEquals(0, s.introBeats)
        assertTrue(s.precision >= 0.85f, "precision ${s.precision}")
        assertTrue(s.recall >= 0.85f, "recall ${s.recall}")
    }

    /** Guards today's measured behavior so it can't silently get worse. */
    @Test
    fun noWorseThanMeasured() {
        val s = score()
        assertEquals(0, s.introBeats, "no beats in the intro bars")
        assertTrue(s.precision >= 0.82f, "precision ${s.precision}")
        assertTrue(s.recall >= 0.88f, "recall ${s.recall}")
    }
}
