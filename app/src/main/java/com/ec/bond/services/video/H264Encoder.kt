package com.ec.bond.services.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer

@Suppress("PrivatePropertyName", "LocalVariableName", "LocalVariableName")
class H264Encoder(private val listener: H264EncoderListener) : H264() {
    private val TAG = H264Encoder::class.java.simpleName
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null
    private val VERBOSE = true
    private val FRAME_RATE = 30
    private val IFRAME_INTERVAL: Float = 10f // 10 seconds between I-frames
    private val NUM_FRAMES = FRAME_RATE / 3

    private val TEST_Y = 120 // YUV values for colored rect
    private val TEST_U = 160
    private val TEST_V = 200

    // size of a frame, in pixels
    private var mWidth = -1
    private var mHeight = -1

    // bit rate, in bits per second
    private var BIT_RATE = 500000

    fun prepare(width: Int, height: Int) {

        close()
        BIT_RATE = 32 * width * height * FRAME_RATE / 100
        mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
        val mediaFormat: MediaFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height)

        mWidth = width
        mHeight = height

        // Failing to specify some of these can cause the MediaCodec configure() call to throw an
        // unhelpful exception. About COLOR_FormatSurface, see
        // https://stackoverflow.com/q/28027858/4288782
        // This just means it is an opaque, implementation-specific format that the device
        // GPU prefers. So as long as we use the GPU to draw, the format will match what
        // the encoder expects.
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
//        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // seconds between key frames!
        mediaFormat.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1200000)
        // We rotate the texture using transformRotation. Pass rotation=0 to super so that
        // no rotation metadata is written into the output file.

        mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()
        isReady = true

    }

    fun encode(input: ByteArray) {
        if (isReady == false) {
            Log.d(
                TAG,
                "Encoder is not Reade! You MUST run prepare function before encoding new frames"
            )
            return
        }

        val TIMEOUT_USEC: Long = 10000
//        val TIMEOUT_USEC: Long = -1
        val mediaCodec = mediaCodec ?: return
        try {
            val inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC)
            if (inputBufferIndex >= 0) {
                val inputBuffer: ByteBuffer? = mediaCodec.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(input)
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.size, 0, 0)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)

            while (outputBufferIndex >= 0) {
                val outputBuffer: ByteBuffer? = mediaCodec.getOutputBuffer(outputBufferIndex)
                // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                outputBuffer?.position(bufferInfo.offset)
                outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)

                val outData = ByteArray(bufferInfo.size)
                outputBuffer?.get(outData)
                outputBuffer?.clear()

                // NALU header 0x00 00 00 01 YY where the first 5 bit indicate what
                // type of NALU it is and therefore what type of data follows the header.
                val frameBuffer: ByteBuffer = ByteBuffer.wrap(outData)
                var naluType = -1

                while (true) {
                    val naluHeaderPosition = getNextNALUHeaderEndIndex(frameBuffer) ?: break

                    // NALU header 0x00 00 00 01 YY where the first 5 bit indicate what
                    // type of NALU it is and therefore what type of data follows the header.
                    frameBuffer.position(naluHeaderPosition)
                    naluType = outData[frameBuffer.position()].toInt().and(0x1F)

                    if (naluType == 8) {
                        // PPS NALU
                        val ppsIndex = frameBuffer.position()

                        // SPS size == 2 NALU - 2 Headers (4 bytes each)
                        sps = ByteArray(ppsIndex - 8)
                        System.arraycopy(outData, 4, sps!!, 0, sps!!.size)
                        pps = ByteArray(outData.size - ppsIndex)
                        System.arraycopy(outData, ppsIndex, pps!!, 0, pps!!.size)
                    }

                    Log.d("NALU TYPE received", naluType.toString())
                }

                // Add SPS and PPS NALU to IDR frame
                val nalu = if (naluType == 5 && pps != null && sps != null) {
                    // Format is
                    // NALU header 0x00 00 00 01
                    // SPS Bytes
                    // NALU header 0x00 00 00 01
                    // PPS Bytes
                    // NALU header 0x00 00 00 01
                    // IDR Frame Data
                    val startCode = byteArrayOf(0x00, 0x00, 0x00, 0x01)
                    startCode + sps!! + startCode + pps!! + outData
                } else {
                    outData
                }
                listener.onFrameEncoded(outData)

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            }

        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun getNextNALUHeaderEndIndex(byteBuffer: ByteBuffer): Int? {
        if (byteBuffer.hasRemaining() == false) return null
        val currentPos = byteBuffer.position()
        val capacity = byteBuffer.capacity()
        if (currentPos >= capacity - 5) return null

        for (i in currentPos..capacity) {
            if (byteBuffer.position() >= capacity - 5) break
            if (byteBuffer.get().toInt() == 0x00 &&
                byteBuffer.get().toInt() == 0x00 &&
                byteBuffer.get().toInt() == 0x00 &&
                byteBuffer.get().toInt() == 0x01 == true
            ) {

                // get the nalu position
                val naluPos = byteBuffer.position()
                // Restore position
                byteBuffer.position(currentPos)
                // Return the position and subtract the 4 bytes of the NALU HEADER delimiter
                return naluPos
            }
        }

        // Restore position
        byteBuffer.position(currentPos)
        return null
    }

    interface H264EncoderListener {
        fun onFrameEncoded(encodedFrame: ByteArray)
    }


}

//package com.ec.bond.services.video
//
//import android.media.MediaCodec
//import android.media.MediaCodecInfo
//import android.media.MediaCodecList
//import android.media.MediaFormat
//import android.util.Log
//import java.io.IOException
//import java.nio.ByteBuffer
//
//@Suppress("PrivatePropertyName", "LocalVariableName", "LocalVariableName")
//class H264Encoder(private val listener: H264EncoderListener) : H264() {
//    private val TAG = H264Encoder::class.java.simpleName
//    private var sps: ByteArray? = null
//    private var pps: ByteArray? = null
//    private val VERBOSE = true
//    private val FRAME_RATE = 30
//    private val IFRAME_INTERVAL: Float = 10f // 10 seconds between I-frames
//    private val NUM_FRAMES = FRAME_RATE / 3
//
//    private val TEST_Y = 120 // YUV values for colored rect
//    private val TEST_U = 160
//    private val TEST_V = 200
//
//    // size of a frame, in pixels
//    private var mWidth = -1
//    private var mHeight = -1
//
//    // bit rate, in bits per second
//    private var BIT_RATE = 500000
//
//    fun prepare(width: Int, height: Int) {
//
//        close()
//        BIT_RATE = 32 * width * height * FRAME_RATE / 100
//        mediaCodec?.release()
//
//        val codecInfo = selectCodec(VIDEO_MIME_TYPE) ?: return
//        mediaCodec = try {
//            MediaCodec.createByCodecName(codecInfo.name)
//        } catch (e: IOException) {
//            Log.e(TAG, "Unable to create MediaCodec ${e.message}")
//            return
//        }
//
//        val colorFormat = try {
//            selectColorFormat(codecInfo, VIDEO_MIME_TYPE)
//        } catch (e: Exception) {
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
//        }
//        val mediaFormat: MediaFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height)
//
//        mWidth = width
//        mHeight = height
//
//        // Failing to specify some of these can cause the MediaCodec configure() call to throw an
//        // unhelpful exception. About COLOR_FormatSurface, see
//        // https://stackoverflow.com/q/28027858/4288782
//        // This just means it is an opaque, implementation-specific format that the device
//        // GPU prefers. So as long as we use the GPU to draw, the format will match what
//        // the encoder expects.
//        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
//        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
////        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // seconds between key frames!
//        mediaFormat.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
//        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1200000)
//        // We rotate the texture using transformRotation. Pass rotation=0 to super so that
//        // no rotation metadata is written into the output file.
//
//        mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
//        mediaCodec?.start()
//        isReady = true
//
//    }
//
//    fun encode(input: ByteArray) {
//        if (isReady == false) {
//            Log.d(
//                TAG,
//                "Encoder is not Reade! You MUST run prepare function before encoding new frames"
//            )
//            return
//        }
//
//        val TIMEOUT_USEC: Long = 10000
////        val TIMEOUT_USEC: Long = -1
//        val mediaCodec = mediaCodec ?: return
//        try {
//            val inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC)
//            if (inputBufferIndex >= 0) {
//                val inputBuffer: ByteBuffer? = mediaCodec.getInputBuffer(inputBufferIndex)
//                inputBuffer?.clear()
//                inputBuffer?.put(input)
//                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.size, 0, 0)
//            }
//
//            val bufferInfo = MediaCodec.BufferInfo()
//            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
//
//            while (outputBufferIndex >= 0) {
//                val outputBuffer: ByteBuffer? = mediaCodec.getOutputBuffer(outputBufferIndex)
//                // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
//                outputBuffer?.position(bufferInfo.offset)
//                outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
//
//                val outData = ByteArray(bufferInfo.size)
//                outputBuffer?.get(outData)
//                outputBuffer?.clear()
//
//                // NALU header 0x00 00 00 01 YY where the first 5 bit indicate what
//                // type of NALU it is and therefore what type of data follows the header.
//                val frameBuffer: ByteBuffer = ByteBuffer.wrap(outData)
//                var naluType = -1
//
//                while (true) {
//                    val naluHeaderPosition = getNextNALUHeaderEndIndex(frameBuffer) ?: break
//
//                    // NALU header 0x00 00 00 01 YY where the first 5 bit indicate what
//                    // type of NALU it is and therefore what type of data follows the header.
//                    frameBuffer.position(naluHeaderPosition)
//                    naluType = outData[frameBuffer.position()].toInt().and(0x1F)
//
//                    if (naluType == 8) {
//                        // PPS NALU
//                        val ppsIndex = frameBuffer.position()
//
//                        // SPS size == 2 NALU - 2 Headers (4 bytes each)
//                        sps = ByteArray(ppsIndex - 8)
//                        System.arraycopy(outData, 4, sps!!, 0, sps!!.size)
//                        pps = ByteArray(outData.size - ppsIndex)
//                        System.arraycopy(outData, ppsIndex, pps!!, 0, pps!!.size)
//                    }
//
//                    Log.d("NALU TYPE received", naluType.toString())
//                }
//
//                // Add SPS and PPS NALU to IDR frame
//                val nalu = if (naluType == 5 && pps != null && sps != null) {
//                    // Format is
//                    // NALU header 0x00 00 00 01
//                    // SPS Bytes
//                    // NALU header 0x00 00 00 01
//                    // PPS Bytes
//                    // NALU header 0x00 00 00 01
//                    // IDR Frame Data
//                    val startCode = byteArrayOf(0x00, 0x00, 0x00, 0x01)
//                    startCode + sps!! + startCode + pps!! + outData
//                } else {
//                    outData
//                }
//                listener.onFrameEncoded(nalu)
//
//                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
//                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
//            }
//
//        } catch (t: Throwable) {
//            t.printStackTrace()
//        }
//    }
//
//    private fun getNextNALUHeaderEndIndex(byteBuffer: ByteBuffer): Int? {
//        if (byteBuffer.hasRemaining() == false) return null
//        val currentPos = byteBuffer.position()
//        val capacity = byteBuffer.capacity()
//        if (currentPos >= capacity - 5) return null
//
//        for (i in currentPos..capacity) {
//            if (byteBuffer.position() >= capacity - 5) break
//            if (byteBuffer.get().toInt() == 0x00 &&
//                byteBuffer.get().toInt() == 0x00 &&
//                byteBuffer.get().toInt() == 0x00 &&
//                byteBuffer.get().toInt() == 0x01 == true
//            ) {
//
//                // get the nalu position
//                val naluPos = byteBuffer.position()
//                // Restore position
//                byteBuffer.position(currentPos)
//                // Return the position and subtract the 4 bytes of the NALU HEADER delimiter
//                return naluPos
//            }
//        }
//
//        // Restore position
//        byteBuffer.position(currentPos)
//        return null
//    }
//
//    interface H264EncoderListener {
//        fun onFrameEncoded(encodedFrame: ByteArray)
//    }
//
//    private fun selectColorFormat(
//        codecInfo: MediaCodecInfo,
//        mimeType: String
//    ) = codecInfo.getCapabilitiesForType(mimeType).colorFormats.find { isRecognizedFormat(it) } ?: 0
//
//    private fun isRecognizedFormat(colorFormat: Int): Boolean {
//        return when (colorFormat) {
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
//            MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
//            else -> false
//        }
//    }
//
//    private fun selectCodec(mimeType: String): MediaCodecInfo? {
//        val numCodecs: Int = MediaCodecList.getCodecCount()
//        for (i in 0 until numCodecs) {
//            val codecInfo: MediaCodecInfo = MediaCodecList.getCodecInfoAt(i)
//            if (!codecInfo.isEncoder) {
//                continue
//            }
//            val types = codecInfo.supportedTypes
//            for (j in types.indices) {
//                if (types[j].equals(mimeType, ignoreCase = true)) {
//                    return codecInfo
//                }
//            }
//        }
//        return null
//    }
//}