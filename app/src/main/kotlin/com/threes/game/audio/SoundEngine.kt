package com.threes.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

interface SoundController {
    var soundEnabled: Boolean
    var hapticsEnabled: Boolean
    fun playMerge(tileValue: Int)
    fun playSlide()
    fun playSnap()
    fun playGameOver()
    fun playNewRecord()
    fun playCharacterVoice(value: Int)
    fun vibrateTick()
    fun vibrateSlide()
    fun vibrateDragThreshold()
    fun vibrateBlocked()
    fun vibrateMerge()
    fun vibrateNewRecord()
    fun vibrateGameOver()
    fun release()
}

class SoundEngine(private val context: Context) : SoundController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override var soundEnabled: Boolean = true
    override var hapticsEnabled: Boolean = true

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

    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val chimeSoundIds = ConcurrentHashMap<Int, Int>()
    private val voiceSoundIds = ConcurrentHashMap<Int, Int>()
    @Volatile private var slideSoundId: Int = 0
    @Volatile private var snapSoundId: Int = 0
    @Volatile private var gameOverSoundId: Int = 0
    @Volatile private var newRecordSoundId: Int = 0
    @Volatile private var isReleased = false

    private val soundPool: SoundPool? = try {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()
            .apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSoundIds.add(sampleId)
                    }
                }
            }
    } catch (_: Exception) {
        null
    }

    init {
        scope.launch {
            precomputeAndLoadSounds()
        }
    }

    override fun release() {
        isReleased = true
        scope.cancel()
        try {
            soundPool?.release()
        } catch (_: Exception) {}
        loadedSoundIds.clear()
        chimeSoundIds.clear()
        voiceSoundIds.clear()
    }

    private fun precomputeAndLoadSounds() {
        val pool = soundPool ?: return
        val cacheDir = try {
            File(context.cacheDir, "threes_sfx_v2").apply { mkdirs() }
        } catch (_: Exception) {
            return
        }

        val mergeFrequencies = mapOf(
            3 to 523.25f,     // C5
            6 to 587.33f,     // D5
            12 to 659.25f,    // E5
            24 to 783.99f,    // G5
            48 to 880.00f,    // A5
            96 to 1046.50f,   // C6
            192 to 1174.66f,  // D6
            384 to 1318.51f,  // E6
            768 to 1567.98f,  // G6
            1536 to 1760.00f, // A6
            3072 to 2093.00f, // C7
            6144 to 2637.02f  // E7
        )

        for ((value, freq) in mergeFrequencies) {
            if (isReleased) return
            val id = loadPcmIntoPool(pool, cacheDir, "chime_$value.wav") {
                generateBellChime(freq, 0.35f)
            }
            if (id > 0) chimeSoundIds[value] = id
        }

        val voiceFrequencies = mapOf(
            1 to 440.00f,     // Uno
            2 to 554.37f,     // Dos
            3 to 659.25f,     // Trey
            6 to 587.33f,     // Todd
            12 to 523.25f,    // Terry
            24 to 698.46f,    // Torval
            48 to 783.99f,    // Twee
            96 to 880.00f,    // Thelma
            192 to 392.00f,   // Thurston
            384 to 329.63f,   // Torrance
            768 to 987.77f,   // Tycho
            1536 to 293.66f,  // Terrence
            3072 to 739.99f,  // Theron
            6144 to 1046.50f  // Trinity
        )

        for ((value, freq) in voiceFrequencies) {
            if (isReleased) return
            val id = loadPcmIntoPool(pool, cacheDir, "voice_$value.wav") {
                generateCuteVoiceChirp(freq)
            }
            if (id > 0) voiceSoundIds[value] = id
        }

        if (isReleased) return
        slideSoundId = loadPcmIntoPool(pool, cacheDir, "slide.wav") { generateSlideSound(0.06f) }
        snapSoundId = loadPcmIntoPool(pool, cacheDir, "snap.wav") { generateSnapSound(0.03f) }
        gameOverSoundId = loadPcmIntoPool(pool, cacheDir, "gameover.wav") { generateMelancholicJingle() }
        newRecordSoundId = loadPcmIntoPool(pool, cacheDir, "newrecord.wav") { generateVictoryFanfare() }
    }

    private inline fun loadPcmIntoPool(
        pool: SoundPool,
        dir: File,
        filename: String,
        generator: () -> ByteArray
    ): Int {
        return try {
            val file = File(dir, filename)
            if (!file.exists() || file.length() <= 44L) {
                val pcm = generator()
                val tempFile = File(dir, "$filename.tmp")
                writeWavFile(tempFile, pcm)
                if (!tempFile.renameTo(file)) {
                    writeWavFile(file, pcm)
                    tempFile.delete()
                }
            }
            if (isReleased) 0 else pool.load(file.absolutePath, 1)
        } catch (_: Exception) {
            0
        }
    }

    private fun writeWavFile(file: File, pcmData: ByteArray) {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + pcmData.size)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2) // Byte rate
            putShort(2) // Block align
            putShort(16) // Bits per sample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmData.size)
        }.array()

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmData)
        }
    }

    private fun playSoundId(soundId: Int, volume: Float = 1f) {
        if (!soundEnabled || isReleased || soundId <= 0) return
        if (!loadedSoundIds.contains(soundId)) return
        try {
            soundPool?.play(soundId, volume, volume, 1, 0, 1f)
        } catch (_: Exception) {}
    }

    override fun playMerge(tileValue: Int) {
        val id = chimeSoundIds[tileValue] ?: chimeSoundIds[3] ?: 0
        playSoundId(id)
        vibrateMerge()
    }

    override fun playSlide() {
        playSoundId(slideSoundId, volume = 0.85f)
        vibrateSlide()
    }

    override fun playSnap() {
        playSoundId(snapSoundId, volume = 0.9f)
        vibrateTick()
    }

    override fun playGameOver() {
        playSoundId(gameOverSoundId)
        vibrateGameOver()
    }

    override fun playNewRecord() {
        playSoundId(newRecordSoundId)
        vibrateNewRecord()
    }

    override fun playCharacterVoice(value: Int) {
        val id = voiceSoundIds[value] ?: voiceSoundIds[3] ?: 0
        playSoundId(id)
    }

    private fun generateBellChime(frequency: Float, durationSec: Float): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val fadeInSamples = (sampleRate * 0.003f).toInt().coerceAtLeast(1)
        val fadeOutSamples = (sampleRate * 0.015f).toInt().coerceAtLeast(1)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 9.0)
            val decayHarmonic = exp(-t * 16.0)
            val env = edgeEnvelope(i, totalSamples, fadeInSamples, fadeOutSamples)

            val sample = (
                0.7 * sin(2.0 * PI * frequency * t) * decay +
                    0.25 * sin(2.0 * PI * frequency * 2.0 * t) * decayHarmonic +
                    0.1 * sin(2.0 * PI * frequency * 3.0 * t) * exp(-t * 22.0)
                ) * 0.9 * env

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
            val envelope = sin(progress * PI)
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
        val fadeInSamples = (sampleRate * 0.002f).toInt().coerceAtLeast(1)
        val fadeOutSamples = (sampleRate * 0.006f).toInt().coerceAtLeast(1)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 120.0)
            val env = edgeEnvelope(i, totalSamples, fadeInSamples, fadeOutSamples)
            val sample = sin(2.0 * PI * 800.0 * t) * decay * 0.6 * env

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
        val noteSamples = (sampleRate * noteDur).toInt()
        val totalSamples = noteSamples * notes.size
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val fadeIn = (sampleRate * 0.005f).toInt().coerceAtLeast(1)
        val fadeOut = (sampleRate * 0.020f).toInt().coerceAtLeast(1)

        for (freq in notes) {
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 6.0)
                val env = edgeEnvelope(i, noteSamples, fadeIn, fadeOut)
                val sample = sin(2.0 * PI * freq * t) * decay * 0.7 * env
                val shortSample = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                byteBuffer.putShort(shortSample)
            }
        }
        return byteBuffer.array()
    }

    private fun generateVictoryFanfare(): ByteArray {
        val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
        val noteDur = 0.14f
        val noteSamples = (sampleRate * noteDur).toInt()
        val totalSamples = noteSamples * notes.size
        val byteBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val fadeIn = (sampleRate * 0.004f).toInt().coerceAtLeast(1)
        val fadeOut = (sampleRate * 0.018f).toInt().coerceAtLeast(1)

        for (freq in notes) {
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 5.0)
                val env = edgeEnvelope(i, noteSamples, fadeIn, fadeOut)
                val sample = (0.7 * sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)) *
                    decay * 0.8 * env
                val shortSample = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                byteBuffer.putShort(shortSample)
            }
        }
        return byteBuffer.array()
    }

    private fun edgeEnvelope(
        index: Int,
        totalSamples: Int,
        fadeInSamples: Int,
        fadeOutSamples: Int
    ): Double {
        val inGain = if (index < fadeInSamples) index.toDouble() / fadeInSamples else 1.0
        val remaining = totalSamples - 1 - index
        val outGain = if (remaining < fadeOutSamples) remaining.toDouble() / fadeOutSamples else 1.0
        return min(inGain, outGain).coerceIn(0.0, 1.0)
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

    override fun vibrateTick() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else -1
        doVibrate(durationMs = 15, predefinedEffectId = effectId, amplitude = 100)
    }

    override fun vibrateSlide() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else -1
        doVibrate(durationMs = 12, predefinedEffectId = effectId, amplitude = 70)
    }

    override fun vibrateDragThreshold() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else -1
        doVibrate(durationMs = 15, predefinedEffectId = effectId, amplitude = 120)
    }

    override fun vibrateBlocked() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else -1
        doVibrate(durationMs = 25, predefinedEffectId = effectId, amplitude = 160)
    }

    override fun vibrateMerge() {
        val effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else -1
        doVibrate(durationMs = 35, predefinedEffectId = effectId, amplitude = 180)
    }

    override fun vibrateNewRecord() {
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

    override fun vibrateGameOver() {
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
