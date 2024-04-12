package com.ec.bond.services

import android.media.*
import android.media.MediaRecorder.AudioSource.MIC
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import com.ec.bond.blackbox.model.BBCall
import com.ec.bond.blackbox.model.BBCallStatus

object VoipAudioManager {
    private val TAG = "VoiceRecord"
    private val RECORDER_SAMPLERATE = 48000
    private val RECORDER_CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO
    private val RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO
    private val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private val AUDIO_SOURCE: Int = MIC
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var autoGainControl: AutomaticGainControl? = null

    // Initialize minimum buffer size in bytes.
    private val bufferSize: Int = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING)

    private var outgoingAudioThread: Thread? = null
    private var incomingAudioThread: Thread? = null
    private var isActive = false

    private var call: BBCall? = null

    private fun getAudioTrack() : AudioTrack {
        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val format = AudioFormat.Builder()
                .setSampleRate(RECORDER_SAMPLERATE)
                .setEncoding(RECORDER_AUDIO_ENCODING)
                .setChannelMask(RECORDER_CHANNELS_OUT).build()

        return AudioTrack(attributes, format, 1920, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
    }

    fun initializeAudioForCall(call: BBCall) {
        this.call = call
    }

    fun startAudio() {
        if (isActive) return
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) Log.e(TAG, "Bad Value for \"bufferSize\", recording parameters are not supported by the hardware")
        if (bufferSize == AudioRecord.ERROR) Log.e(TAG, "Bad Value for \"bufferSize\", implementation was unable to query the hardware for its output properties")
        Log.e(TAG, "\"bufferSize\"=${bufferSize}")

        outgoingAudioThread = Thread({ recordAndSendAudio() }, "AudioRecorder Thread")
        outgoingAudioThread?.start()

        incomingAudioThread = Thread({ playIncomingAudio() }, "Audio Trak play Thread")
        incomingAudioThread?.start()

        isActive = true
    }

    fun stopAudio() {
        //  stops the recording
        isActive = false
        outgoingAudioThread?.interrupt()
        outgoingAudioThread = null
        incomingAudioThread?.interrupt()
        incomingAudioThread = null
        call = null
        echoCanceler?.release()
        echoCanceler = null
        noiseSuppressor?.release()
        noiseSuppressor = null
        autoGainControl?.release()
        autoGainControl = null
    }

    private fun playIncomingAudio() {
        val call = call ?: return
        if (call.isConference && call.isOutgoing) {
            playIncomingAudioConference()
        } else {
            playIncomingAudioOneToOne()
        }
    }

    private fun recordAndSendAudio() {
        val call = call ?: return
        if (call.isConference && call.isOutgoing) {
            recordAndSendAudioConference()
        } else {
            recordAndSendAudioOneToOne()
        }
    }

    private fun playIncomingAudioOneToOne() {
        val incomingAudioTrack = getAudioTrack()
        incomingAudioTrack.play()
        while (isActive) {
            val call = call ?: continue
            val audioPacket = call.receiveAudioPacket()

            if (audioPacket.first > 0) {
                // Valid data
                audioPacket.second?.let {
                    incomingAudioTrack.write(it, 0, it.count())
                }
            }
            else if (audioPacket.first == -1) {
                // "Audio - Timed-out"
                call.setCallStatus(BBCallStatus.hangup)
                break
            }
            else if (audioPacket.first == -2) {
                // "Audio - Hang Up"
                call.setCallStatus(BBCallStatus.hangup)
                break
            }
            else {
                Log.d("Test", "asd")
                break
            }
        }

        incomingAudioTrack.stop()
        incomingAudioTrack.release()
    }

    private fun playIncomingAudioConference() {
        val incomingAudioTrack = getAudioTrack()
        incomingAudioTrack.play()
        while (isActive) {
            val call = call ?: continue
            val members = call.members.value ?: continue
            val contact = members.filter {
                it.callInfo.answeredTime > 0 && it.callInfo.isAudioReceiveStarted
            }.minByOrNull { it.callInfo.answeredTime } ?: continue

            val callSession = contact.callInfo.callSession ?: continue
            val audioPacket = call.receiveAudioPacketSession(callSession)

            if (audioPacket.first > 0) {
                // Valid data
                val packet = audioPacket.second ?: continue
                incomingAudioTrack.write(packet, 0, packet.size)
            }
            else if (audioPacket.first == -1) {
                // "Audio - Timed-out"
                call.setCallStatus(BBCallStatus.hangup)
                break
            }
            else if (audioPacket.first == -2) {
                // "Audio - Hang Up"
                call.setCallStatus(BBCallStatus.hangup)
                break
            }
            else {
                Log.d("Test", "asd")
                break
            }
        }

        incomingAudioTrack.stop()
        incomingAudioTrack.release()
    }

    private fun improveAudioQuality(sessionId: Int?) {
        val id = sessionId ?: return
        // Try to improve the recorded Audio
        AcousticEchoCanceler.create(id)?.let {
            echoCanceler = it
            echoCanceler!!.enabled = true
        }
        NoiseSuppressor.create(id)?.let {
            noiseSuppressor = it
            noiseSuppressor!!.enabled = true
        }
        AutomaticGainControl.create(id)?.let {
            autoGainControl = it
            autoGainControl!!.enabled = true
        }
    }

    private fun recordAndSendAudioOneToOne() {
        val call = call ?: return
        // Initialize Audio Recorder.
//        val recorder = AudioRecord(AUDIO_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING, bufferSize)
        var recorder: AudioRecord? = null

        //Write the output audio in byte
        val audioBuffer = ByteArray(bufferSize)
        while (true) {
            val status = call.status.value ?: break
            if (status == BBCallStatus.hangup || status == BBCallStatus.ended) break

            if (call.isAudioStarted) {
                if (call.audioPermission == true) {
                    if (recorder == null) {
                        recorder = AudioRecord(AUDIO_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING, bufferSize)

                        improveAudioQuality(recorder.audioSessionId)

                        // Starts recording from the AudioRecord instance.
                        recorder.startRecording()
                    }
                    // gets the voice output from microphone to byte format
                    recorder.read(audioBuffer, 0, bufferSize)
                    call.sendAudio(audioBuffer)
                }
            }
        }
        recorder?.stop()
        recorder?.release()
    }

    private fun recordAndSendAudioConference() {
        val call = call ?: return
        // Initialize Audio Recorder.
//        val recorder = AudioRecord(AUDIO_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING, bufferSize)
        var recorder: AudioRecord? = null

        //Write the output audio in byte
        val audioBuffer = ByteArray(bufferSize)
        while (true) {
            val status = call.status.value ?: break
            if (status == BBCallStatus.hangup || status == BBCallStatus.ended) break

            if (call.isAudioStarted) {
                if (call.audioPermission == true) {
                    if (recorder == null) {
                        recorder = AudioRecord(AUDIO_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING, bufferSize)
                        improveAudioQuality(recorder.audioSessionId)

                        // Starts recording from the AudioRecord instance.
                        recorder.startRecording()
                    }
                    // gets the voice output from microphone to byte format
                    recorder.read(audioBuffer, 0, bufferSize)

                    val members = call.members.value ?: continue
                    val contact = members.filter {
                        it.callInfo.answeredTime > 0 && it.callInfo.isAudioReceiveStarted
                    }.minByOrNull { it.callInfo.answeredTime } ?: continue
                    val callSession = contact.callInfo.callSession ?: continue
                    call.sendAudio(audioBuffer, callSession)
                }
            }
        }
        recorder?.stop()
        recorder?.release()
    }

}