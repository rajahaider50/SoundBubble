package com.soundbubble.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import java.io.File

/**
 * Chooses where playback audio comes out of:
 *  - BLUETOOTH: routes through a connected Bluetooth headset's SCO link (mic sits right next to it)
 *  - SPEAKER: plays loudly through the phone's own speaker (relies on the phone mic picking it up)
 *  - AUTO: picks BLUETOOTH automatically if a headset is connected, otherwise SPEAKER
 *
 * Neither mode digitally injects audio into another app's microphone stream -- Android does not
 * allow that without root. Both modes work acoustically (the sound has to physically reach a mic).
 */
object AudioRouter {

    private const val PREFS = "soundbubble_prefs"
    private const val KEY_MODE = "audio_output_mode"

    enum class Mode { AUTO, SPEAKER, BLUETOOTH }

    fun getMode(context: Context): Mode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_MODE, Mode.AUTO.name) ?: Mode.AUTO.name
        return runCatching { Mode.valueOf(name) }.getOrDefault(Mode.AUTO)
    }

    fun cycleMode(context: Context): Mode {
        val next = when (getMode(context)) {
            Mode.AUTO -> Mode.SPEAKER
            Mode.SPEAKER -> Mode.BLUETOOTH
            Mode.BLUETOOTH -> Mode.AUTO
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, next.name).apply()
        return next
    }

    fun modeLabel(mode: Mode): String = when (mode) {
        Mode.AUTO -> "🔊 Output: Auto"
        Mode.SPEAKER -> "🔊 Output: Phone Speaker"
        Mode.BLUETOOTH -> "🎧 Output: Bluetooth Handsfree"
    }

    /** Plays [file] using the saved routing preference. Caller should keep the returned MediaPlayer
     *  reference until playback ends (it self-releases on completion). */
    fun play(context: Context, file: File): MediaPlayer? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mode = getMode(context)

        val useBluetooth = when (mode) {
            Mode.BLUETOOTH -> true
            Mode.SPEAKER -> false
            Mode.AUTO -> isBluetoothHeadsetConnected(audioManager)
        }

        return try {
            if (useBluetooth) routeToBluetooth(audioManager) else routeToSpeaker(audioManager)

            val attrs = AudioAttributes.Builder()
                .setUsage(
                    if (useBluetooth) AudioAttributes.USAGE_VOICE_COMMUNICATION
                    else AudioAttributes.USAGE_MEDIA
                )
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(file.absolutePath)
                setOnCompletionListener { mp ->
                    if (useBluetooth) stopBluetooth(audioManager)
                    mp.release()
                }
                setOnErrorListener { mp, _, _ ->
                    if (useBluetooth) stopBluetooth(audioManager)
                    mp.release()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isBluetoothHeadsetConnected(audioManager: AudioManager): Boolean {
        return try {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun routeToBluetooth(audioManager: AudioManager) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()
    }

    private fun stopBluetooth(audioManager: AudioManager) {
        try {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {
        }
    }

    private fun routeToSpeaker(audioManager: AudioManager) {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = true
    }
}
