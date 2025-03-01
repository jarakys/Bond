package com.ec.bond.extensions

import android.graphics.Bitmap
import android.util.Log


fun Bitmap.getNV21Bytes() : ByteArray? {
    if (byteCount == 0) {
        Log.e("getNV21Bytes", "Empty bitmap")
        return  null
    }
    if (config != Bitmap.Config.ARGB_8888) {
        Log.e("getNV21Bytes", "Bitmap Config must be Bitmap.Config.ARGB_8888")
        return null
    }
    val argb = IntArray(width * height)
    getPixels(argb, 0, width, 0, 0, width, height)
    return encodeYUV420SP(argb, width, height)
}
private fun encodeYUV420SP(argb: IntArray, width: Int, height: Int) : ByteArray {
    val yuv420sp = ByteArray(width * height * 3 / 2)
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize
    var a: Int
    var R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0
    for (j in 0 until height) {
        for (i in 0 until width) {
            a = argb[index] and -0x1000000 shr 24 // a is not used obviously
            R = argb[index] and 0xff0000 shr 16
            G = argb[index] and 0xff00 shr 8
            B = argb[index] and 0xff shr 0

            // well known RGB to YUV algorithm
            Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
            U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
            V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

            // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
            //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
            //    pixel AND every other scanline.
            yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
            }
            index++
        }
    }

    return yuv420sp
}

