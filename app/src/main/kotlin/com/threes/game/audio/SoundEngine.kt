package com.threes.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
                    ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private val hapticAudioAttributes by lazy {
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()
    }

    private val sampleRate = 44100

    // Thread-safe audio caches for fast playback
    private val chimeCache = ConcurrentHashMap<Int, ByteArray>()
    @Volatile private var slideAudio: ByteArray? = null
    @Volatile private var snapAudio: ByteArray? = null
    @Volatile private var gameOverAudio: ByteArray? = null
    @Volatile private var newRecordAudio: ByteArray? = null

    init {
        scope.launch {
            precomputeAudio()
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun precomputeAudio() {
        val frequencies = mapOf(
            3 to 523.25f,    // C5
            6 to 587.33f,    // D5
            12 to 659.25f,   // E5
            24 to 783.99f,   // G5
            48 to 880.00f,   // A5
            96 to 1046.50f,  // C6
            192 to 1174.66f, // D6
            384 to 1318.51f, // E6
            768 to 1567.98f, // G6
            1536 to 1760.00f,// A6
            3072 to 2093.00f,// C7
            6144 to 2637.02f // E7
        )

        for ((value, freq) in frequencies) {
            chimeCache[value] = generateBellChime(freq, 0.35f)
        }

        slideAudio = generateSlideSound(0.06f)
        snapAudio = generateSnapSound(0.03f)
        gameOverAudio = generateMelancholicJingle()
        newRecordAudio = generateVictoryFanfare()
    }

    fun playMerge(tileValue: Int) {
        if (soundEnabled) {
            scope.launch {
                val pcm = chimeCache[tileValue] ?: generateBellChime(523.25f, 0.3f)
                playPcm(pcm)
            }
        }
        vibrateMerge()
    }

    fun playSlide() {
        if (soundEnabled) {
            scope.launch {
                slideAudio?.let { playPcm(it) }
            }
        }
        vibrateSlide()
    }

    fun playSnap() {
        if (soundEnabled) {
            scope.launch {
                snapAudio?.let { playPcm(it) }
            }
        }
        vibrateTick()
    }

    fun playGameOver() {
        if (soundEnabled) {
            scope.launch {
                gameOverAudio?.let { playPcm(it) }
            }
        }
        vibrateGameOver()
    }

    fun playNewRecord() {
        if (soundEnabled) {
            scope.launch {
                newRecordAudio?.let { playPcm(it) }
            }
        }
        vibrateNewRecord()
    }

    fun playCharacterVoice(value: Int) {
        if (!soundEnabled) return
        scope.launch {
            val freq = when (value) {
                1 -> 440f
                2 -> 554.37f
                3 -> 659.25f
                6 -> 587.33f
                12 -> 523.25f
                24 -> 698.46f
                48 -> 783.99f
                96 -> 880f
                192 -> 392f
                384 -> 330f
                else -> 523.25f
            }
            val voicePcm = generateCuteVoiceChirp(freq)
            playPcm(voicePcm)
        }
    }

    private fun playPcm(pcmData: ByteArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track.play()
            // Release after playing
            scope.launch {
                val durationMs = (pcmData.size.toDouble() / (sampleRate * 2) * 1000).toLong() + 100
                delay(durationMs)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun generateBellChime(frequency: Float, durationSec: Float): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 9.0) // Warm exponential decay
            val decayHarmonic = exp(-t * 16.0)

            // Fundamental + 2nd + 3rd harmonic
            val sample = (
                0.7 * sin(2.0 * PI * frequency * t) * decay +
                0.25 * sin(2.0 * PI * frequency * 2.0 * t) * decayHarmonic +
                0.1 * sin(2.0 * PI * frequency * 3.0 * t) * exp(-t * 22.0)
            ) * 0.9

            val shortSample = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            byteBuffer.putShort(shortSample)
        }
        return byteBuffer.array()
    }

    private fun generateSlideSound(durationSec: Float): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        var phase = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec
            val envelope = sin(progress * PI) // Hanning window
            val instFreq = 300.0 + progress * 200.0
            phase += 2.0 * PI * instFreq / sampleRate

            val noise = (Math.random() * 2.0 - 1.0) * 0.14
            val tone = sin(phase) * 0.1
            val sample = (noise + tone) * envelope

            val shortSample = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            byteBuffer.putShort(shortSample)
        }
        return byteBuffer.array()
    }

    private fun generateSnapSound(durationSec: Float): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 120.0)
            val sample = sin(2.0 * PI * 800.0 * t) * decay * 0.6

            val shortSample = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            byteBuffer.putShort(shortSample)
        }
        return byteBuffer.array()
    }

    private fun generateCuteVoiceChirp(baseFreq: Float): ByteArray {
        val durationSec = 0.18f
        val totalSamples = (sampleRate * durationSec).toInt()
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        var phase = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val instFreq = baseFreq * (1.0 + 0.15 * sin(2.0 * PI * 6.0 * t))
            phase += 2.0 * PI * instFreq / sampleRate
            val env = sin(t / durationSec * PI)
            val sample = sin(phase) * env * 0.7

            val shortSample = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            byteBuffer.putShort(shortSample)
        }
        return byteBuffer.array()
    }

    private fun generateMelancholicJingle(): ByteArray {
        val notes = listOf(783.99f, 659.25f, 523.25f, 440.00f) // G5, E5, C5, A4
        val noteDur = 0.18f
        val totalSamples = (sampleRate * noteDur * notes.size).toInt()
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (freq in notes) {
            val noteSamples = (sampleRate * noteDur).toInt()
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 6.0)
                val sample = sin(2.0 * PI * freq * t) * decay * 0.7
                val shortSample = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                byteBuffer.putShort(shortSample)
            }
        }
        return byteBuffer.array()
    }

    private fun generateVictoryFanfare(): ByteArray {
        val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
        val noteDur = 0.14f
        val totalSamples = (sampleRate * noteDur * notes.size).toInt()
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (freq in notes) {
            val noteSamples = (sampleRate * noteDur).toInt()
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 5.0)
                val sample = (0.7 * sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)) * decay * 0.8
                val shortSample = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                byteBuffer.putShort(shortSample)
            }
        }
        return byteBuffer.array()
    }

    // Haptics
    private fun doVibrate(
        durationMs: Long,
        predefinedEffectId: Int = -1,
        amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE
    ) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        try {
            if (!v.hasVibrator()) return

            var played = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && predefinedEffectId >= 0) {
                try {
                    val effect = VibrationEffect.createPredefined(predefinedEffectId)
                    v.vibrate(effect, hapticAudioAttributes)
                    played = true
                } catch (_: Exception) {
                    played = false
                }
            }

            if (!played) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = try {
                        if (v.hasAmplitudeControl() && amplitude in 1..255) {
                            VibrationEffect.createOneShot(durationMs, amplitude)
                        } else {
                            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                        }
                    } catch (_: Exception) {
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    v.vibrate(effect, hapticAudioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    fun vibrateTick() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else -1
        doVibrate(durationMs = 15, predefinedEffectId = effectId, amplitude = 100)
    }

    fun vibrateSlide() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else -1
        doVibrate(durationMs = 12, predefinedEffectId = effectId, amplitude = 70)
    }

    fun vibrateDragThreshold() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else -1
        doVibrate(durationMs = 15, predefinedEffectId = effectId, amplitude = 120)
    }

    fun vibrateBlocked() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else -1
        doVibrate(durationMs = 25, predefinedEffectId = effectId, amplitude = 160)
    }

    fun vibrateMerge() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else -1
        doVibrate(durationMs = 35, predefinedEffectId = effectId, amplitude = 180)
    }

    fun vibrateNewRecord() {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        try {
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 35, 45, 50, 45, 90),
                        intArrayOf(0, 120, 0, 160, 0, 220),
                        -1
                    ),
                    hapticAudioAttributes
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 35, 45, 50, 45, 90), -1)
            }
        } catch (_: Exception) {}
    }

    fun vibrateGameOver() {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        try {
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 80, 100),
                        intArrayOf(0, 100, 0, 150),
                        -1
                    ),
                    hapticAudioAttributes
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 50, 80, 100), -1)
            }
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                v.vibrate(200)
            } catch (_: Exception) {}
        }
    }
}
