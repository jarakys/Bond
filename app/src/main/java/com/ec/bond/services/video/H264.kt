package com.ec.bond.services.video

import android.media.MediaCodec
import java.io.IOException
import kotlin.jvm.Throws

open class H264 {
    open val VIDEO_MIME_TYPE = "video/avc"
    open val VIDEO_HEIGHT = 640
    open val VIDEO_WIDTH = 480
    open var mediaCodec: MediaCodec? = null
    open var isReady: Boolean = false

    @Throws(IOException::class)
    fun close() {
        mediaCodec?.stop()
        mediaCodec?.release()
        mediaCodec = null
        isReady = false
    }
}