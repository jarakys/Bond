package com.ec.bond.services.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface

@Suppress("PrivatePropertyName")
class H264Decoder : H264() {
    private val TAG = H264Encoder::class.java.simpleName

    fun prepare(surface: Surface, width: Int, height: Int) {
        close()
        mediaCodec = MediaCodec.createDecoderByType(VIDEO_MIME_TYPE)
        val mediaFormat = MediaFormat.createVideoFormat(
            VIDEO_MIME_TYPE,
            VIDEO_WIDTH,
            VIDEO_HEIGHT
        )

        mediaCodec?.configure(mediaFormat, surface, null, 0)
        mediaCodec?.start()
        isReady = true
    }

    fun decodeFrame(input: ByteArray) {
        Log.d("DRE", "decoding network feed size is ${input.size} and " +
                input.joinToString("") { "%02x".format(it) })
       if (isReady == false) {
            Log.d(
                TAG,
                "Decoder is not Ready! You MUST run initDecoder function before encoding new frames"
            )
            return
        }
        val mediaCodec = mediaCodec ?: return
        try {

            val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(input)

                mediaCodec.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    input.size,
                    0,
                    0
                )

                val bufferInfo = MediaCodec.BufferInfo()
                var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                Log.e("DRE", "buffer index is $outputBufferIndex")
                while (outputBufferIndex >= 0) {
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, true)
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }
        } catch (t: Throwable) {
            Log.e("DRE", "network feed decoder exception is ${t.message}")
            t.printStackTrace()
        }
    }
}







