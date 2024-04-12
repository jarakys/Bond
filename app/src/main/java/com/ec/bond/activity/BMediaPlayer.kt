package com.ec.bond.activity

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.ec.bond.BondApp

object BMediaPlayer {
    private var player: MediaPlayer?=null
    private var vibrator : Vibrator?=null
    private var isPrepare = false
    private var isPlayingPendingRequest = false
    private var audioManager: AudioManager? = null
    private val DEFAULT_VIBRATE_PATTERN = longArrayOf(0, 250, 250, 250)

    fun getMediaPlayer() : MediaPlayer? {
        if (player == null) {
            try {
                vibrator =
                    (BondApp.applicationContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?)!!;
                audioManager = (BondApp.applicationContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager?)!!
                var sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                player = MediaPlayer.create(
                    BondApp.applicationContext(),
                    sound
                )
                player?.setOnPreparedListener {
                    isPrepare = true
                    if (isPlayingPendingRequest) {
                        playRingtune()
                        isPlayingPendingRequest = false
                    }
                }

            } catch (e: Exception) {
                return null
            }
        }
        return player
    }

    private fun vibrate(v: LongArray) {
        vibrator = (BondApp.applicationContext()
            .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?)!!;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(v, 0));
            //vibrator?.vibrate(v,0)
        } else {
            vibrator?.vibrate(450)
        }
    }


    fun playRingtune() {
        val mPlayer = getMediaPlayer()
        try {
            if (audioManager?.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                vibrate(DEFAULT_VIBRATE_PATTERN)
                if (audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    if (isPrepare) {
                        if (mPlayer?.isPlaying == false) {
                            mPlayer.isLooping = true
                            mPlayer.start()
                        }
                    } else
                        isPlayingPendingRequest = true
                }
            }
        } catch (e: Exception) {}

    }

    fun stopRingtune() {
        try {
            val mPlayer = getMediaPlayer()
            if (mPlayer?.isPlaying == true) {
                mPlayer.pause()
            }
            vibrator?.cancel()
        } catch (e: Exception) {}

    }
}