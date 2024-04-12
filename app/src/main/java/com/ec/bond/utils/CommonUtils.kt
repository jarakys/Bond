package com.ec.bond.utils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.ec.bond.R
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


object CommonUtils {
    var filterText = MutableLiveData("")
    var PERMISSIONS_CODE = 69

    var recipient: String = ""

    @SuppressLint("all")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
        )
    }

    fun logd(text: String) {
        Log.d("logmasmak", text.toString())
    }

    fun toTitleCase(str: String?): String? {
        if (str == null) {
            return null
        }
        var space = true
        val builder = StringBuilder(str)
        val len = builder.length
        for (i in 0 until len) {
            val c = builder[i]
            if (space) {
                if (!Character.isWhitespace(c)) { // Convert to title case and switch out of whitespace mode.
                    builder.setCharAt(i, Character.toTitleCase(c))
                    space = false
                }
            } else if (Character.isWhitespace(c)) {
                space = true
            } else {
                builder.setCharAt(i, Character.toLowerCase(c))
            }
        }
        return builder.toString()
    }


    fun saveFile(context: Context?, fileName: String, value: String) {
        try {
            // Creates a file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            val testFile = File(getFilesDirectory(context!!), fileName)
            if (!testFile.exists()) testFile.createNewFile()
            // Adds a line to the file
            val writer = BufferedWriter(FileWriter(testFile, true /*append*/))
            writer.write(value)
            writer.close()
            Log.e("ReadWriteFile", "file has been written. " + fileName)
        } catch (e: IOException) {
            Log.e("ReadWriteFile", "Unable to write to the file." + fileName)
        }
    }

    fun getFilesDirectory(context: Context): String {
        Log.w("getTempDirectory", "getTempDirectory")
        return context.filesDir.path
    }

    fun getByte(path: String): ByteArray {
        var getBytes = byteArrayOf()
        try {
            val file = File(path)
            getBytes = ByteArray(file.length().toInt())
            val `is`: InputStream = FileInputStream(file)
            `is`.read(getBytes)
            `is`.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return getBytes
    }

    fun decode(pwdconfg: String): String {
        val data: ByteArray = Base64.decode(pwdconfg, Base64.DEFAULT)
        return String(data)
    }

    fun hideKeybord(context: Activity) {
        val view = context.currentFocus
        view?.let { v ->
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    fun showKeybord(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }


    fun bitmapToJpegFile(context: Context, bitmap: Bitmap): File {
        val outputDir = context.cacheDir
        val outputFile = File.createTempFile("image", ".jpg", outputDir)
        val stream = FileOutputStream(outputFile, false)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.close()
        return outputFile
    }

    fun getPathFromUri(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

            } else if (isDownloadsDocument(uri)) {
                var id = DocumentsContract.getDocumentId(uri)

                val contentUri: Uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                        split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.getScheme(), ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.getLastPathSegment() else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.getScheme(), ignoreCase = true)) {
            return uri.getPath()
        }
        return null
    }

    fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                      selectionArgs: Array<String>?): String? {
        uri?.let { uri ->
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                    column
            )
            var input:FileInputStream?=null
            var output:FileInputStream?=null

            try {
                cursor = context.contentResolver.query(uri, projection, selection, selectionArgs,
                        null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index: Int = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            }catch (e: IllegalArgumentException){
                e.printStackTrace();
            }

            finally {
                if (cursor != null) cursor.close()
            }
        }

        return null
    }




    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.getAuthority()
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    fun getRealPaths(context: Context, data: Intent?): ArrayList<String> {
        val paths = ArrayList<String>()
        if (data?.clipData == null) {
            if (data?.data == null) return ArrayList()
            else {
                paths.add(data.data?.let { getPathFromUri(context, it) }!!)
                return paths
            }
        }
        for (i in 0 until data?.clipData?.itemCount!!) {
            paths.add(data.clipData?.getItemAt(i)?.uri?.let { getPathFromUri(context, it) }!!)
        }
        return paths
    }

    fun Date.getTimeString(pattern: String = "MMMM d,YYYY"): String {
        var response: String = ""
        when (this.getDifferenceBetweenDates()) {
            0 -> response = "TODAY"
            -1 -> response = "YESTERDAY"
            else -> response = SimpleDateFormat(pattern).format(this)
        }
        return response
    }

    @SuppressLint("SimpleDateFormat")
    fun Date.getTimeString2(pattern: String = "MMMM d,YYYY"): String {
        var response: String = ""
        response = when (this.getDifferenceBetweenDates()) {
            0 -> SimpleDateFormat("HH:mm").format(this)
            -1 -> "YESTERDAY"
            else -> SimpleDateFormat(pattern).format(this)
        }
        return response
    }

    private fun Date.getDifferenceBetweenDates(): Int {
        return TimeUnit.DAYS.convert(this.time - Date().time, TimeUnit.MILLISECONDS).toInt()
    }

    suspend fun pickPhoto(context: Context, paths: Array<String>): ArrayList<String> {

        val response = ArrayList<String>()
        val imageExtensions = arrayOf(".jpg", ".png", ".gif", ".jpeg", ".bmp")
        val videoExtensions = arrayOf(".mp4", ".3gp", ".mkv", ".webm")

        for (path in paths) {

            if (imageExtensions.any { path.contains(it) }) {
                var bitmap = BitmapFactory.decodeFile(path)
                var file = File(path)
                var x = file.length()
                Log.i("Image Original Size", x.toString())
                val bytes2 = ByteArrayOutputStream()

                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, bytes2)
                Log.i("Expected final array size", bytes2.size().toString())

                try {
                    var output = createImageFile(context, "jpg")
                    Log.i("Expected empty size", output.length().toString())
                    FileOutputStream(output).use { it.write(bytes2.toByteArray()) }
                    Log.i("Expected final size", output.length().toString())
                    if (output.length() > 242705) {
                        checkImageSizeThenCompress(output, bytes2, 10)
//                        var bitmap = BitmapFactory.decodeFile(output.path)
//                        bytes2.reset()
//                        Log.i("Expected final 2 array size after reset", bytes2.size().toString())
//
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 8, bytes2)
//                        FileOutputStream(output).use { it.write(bytes2.toByteArray()) }
//                        Log.i("Expected final 2 array size", bytes2.size().toString())
//                        Log.i("Expected final 2 size", output.length().toString())
//                        GlobalScope.launch {
//                            checkImageSizeThenCompress(context,output,bytes2,10)
//                        }
//                        withContext(Dispatchers.IO) {
//                            output = Compressor.compress(context, file) {
//                                size(20)
//                                quality(10)
//                            }
//                            Log.i("Expected final 2 size", output.length().toString())
//
//                        }
                    }

                    response.add(output.absolutePath)

                } catch (exc: IOException) {
                    Log.e("imageFailed", "Unable to write JPEG image to file", exc)

                }
            } else {
//                val bytes2 = ByteArrayOutputStream()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val bytes = Files.readAllBytes(Paths.get(path))
                    val output = createVideoFile(context, "mp4")
                    FileOutputStream(output).use { it.write(bytes) }
                    response.add(output.absolutePath)
                } else {
                    TODO("VERSION.SDK_INT < O")
                }


            }


        }
        return response
//        val response = ArrayList<String>()
//        val imageExtensions = arrayOf(".jpg", ".png", ".gif", ".jpeg",".bmp")
//        val videoExtensions = arrayOf(".mp4", ".3gp", ".mkv", ".webm")
//
//        for (path in paths) {
//
//            if(imageExtensions.any { path.contains(it) }){
//                var bitmap = BitmapFactory.decodeFile(path)
//                val bytes2 = ByteArrayOutputStream()
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, bytes2)
//                try {
//                    val output = CameraFragment.createFile(context, "jpg")
//                    FileOutputStream(output).use { it.write(bytes2.toByteArray()) }
//                    response.add(output.absolutePath)
//
//                } catch (exc: IOException) {
//                    Log.e("imageFailed", "Unable to write JPEG image to file", exc)
//
//                }
//            } else {
////                val bytes2 = ByteArrayOutputStream()
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    val bytes = Files.readAllBytes(Paths.get(path))
//                    val output = createVideoFile(context, "mp4")
//                    FileOutputStream(output).use { it.write(bytes) }
//                    response.add(output.absolutePath)
//                } else {
//                    TODO("VERSION.SDK_INT < O")
//                }
//
//
//            }
//
//
//
//
//        }
//        return response
    }

    suspend fun pickPhoto(context: Context, paths: ArrayList<String>): ArrayList<String> {

        val response = ArrayList<String>()
        val imageExtensions = arrayOf(".jpg", ".png", ".gif", ".jpeg", ".bmp")
        val videoExtensions = arrayOf(".mp4", ".3gp", ".mkv", ".webm")
        Log.i("Image_Original_Size", ""+paths)
        for (path in paths) {

            if (imageExtensions.any { path.contains(it) }) {
                var bitmap = BitmapFactory.decodeFile(path)
                var file = File(path)
                var x = file.length()
                Log.i("Image_Original_Size", ""+x.toString())
                Log.i("Image Original Size", x.toString())
                val bytes2 = ByteArrayOutputStream()

                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, bytes2)
                Log.i("Expected final array size", bytes2.size().toString())

                try {
                    var output = createImageFile(context, "jpg")
                    Log.i("Expected empty size", output.length().toString())
                    FileOutputStream(output).use { it.write(bytes2.toByteArray()) }
                    Log.i("Expected final size", output.length().toString())
                    if (output.length() > 242705) {
                        checkImageSizeThenCompress(output, bytes2, 10)
//                        var bitmap = BitmapFactory.decodeFile(output.path)
//                        bytes2.reset()
//                        Log.i("Expected final 2 array size after reset", bytes2.size().toString())
//
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 8, bytes2)
//                        FileOutputStream(output).use { it.write(bytes2.toByteArray()) }
//                        Log.i("Expected final 2 array size", bytes2.size().toString())
//                        Log.i("Expected final 2 size", output.length().toString())
//                        GlobalScope.launch {
//                            checkImageSizeThenCompress(context,output,bytes2,10)
//                        }
//                        withContext(Dispatchers.IO) {
//                            output = Compressor.compress(context, file) {
//                                size(20)
//                                quality(10)
//                            }
//                            Log.i("Expected final 2 size", output.length().toString())
//
//                        }
                    }

                    response.add(output.absolutePath)

                } catch (exc: IOException) {
                    Log.e("imageFailed", "Unable to write JPEG image to file", exc)

                }
            } else {
//                val bytes2 = ByteArrayOutputStream()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val bytes = Files.readAllBytes(Paths.get(path))
                    val output = createVideoFile(context, "mp4")
                    FileOutputStream(output).use { it.write(bytes) }
                    response.add(output.absolutePath)
                } else {
                    TODO("VERSION.SDK_INT < O")
                }


            }


        }
        return response
//        val response = ArrayList<String>()
//        val imageExtensions = arrayOf(".jpg", ".png", ".gif", ".jpeg",".bmp")
//        val videoExtensions = arrayOf(".mp4", ".3gp", ".mkv", ".webm")
//
//        for (path in paths) {
//
//            if(imageExtensions.any { path.contains(it) }){
//                var bitmap = BitmapFactory.decodeFile(path)
//                val bytes2 = ByteArrayOutputStream()
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, bytes2)
//                try {
//                    val output = CameraFragment.createFile(context, "jpg")
//                    FileOutputStream(output).use { it.write(bytes2.toByteArray()) }
//                    response.add(output.absolutePath)
//
//                } catch (exc: IOException) {
//                    Log.e("imageFailed", "Unable to write JPEG image to file", exc)
//
//                }
//            } else {
////                val bytes2 = ByteArrayOutputStream()
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    val bytes = Files.readAllBytes(Paths.get(path))
//                    val output = createVideoFile(context, "mp4")
//                    FileOutputStream(output).use { it.write(bytes) }
//                    response.add(output.absolutePath)
//                } else {
//                    TODO("VERSION.SDK_INT < O")
//                }
//
//
//            }
//
//
//
//
//        }
//        return response
    }

    private suspend fun checkImageSizeThenCompress(file: File, imageBytes: ByteArrayOutputStream, compressRatio: Int): File {
        var bitmap = BitmapFactory.decodeFile(file.path)
        imageBytes.reset()
        Log.i("Expected final 2 array size after reset", imageBytes.size().toString())

        bitmap.compress(Bitmap.CompressFormat.JPEG, compressRatio, imageBytes)
        FileOutputStream(file).use { it.write(imageBytes.toByteArray()) }
        Log.i("Expected final 2 array size", imageBytes.size().toString())
        Log.i("Expected final 2 size", file.length().toString())
        if (file.length() > 202947) {
            if (compressRatio - 2 >= 0) {
                checkImageSizeThenCompress(file, imageBytes, compressRatio - 2)
            }
        }
//        withContext(Dispatchers.IO) {
//            val compressedImageFile = Compressor.compress(context, file)
//        }
        return file
    }

    fun moveDocumentToLocalPath(context: Context, paths: Array<Pair<String, String>>): ArrayList<String> {
        val response = ArrayList<String>()
        for (path in paths) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bytes = Files.readAllBytes(Paths.get(path.first))
                val output = createDocumentFile(context, path.second)
                FileOutputStream(output).use { it.write(bytes) }
                response.add(output.absolutePath)
            } else {
                TODO("VERSION.SDK_INT < O")
            }


        }
        return response
    }

    /**
     * Create a [File] named a using formatted timestamp with the current date and time.
     *
     * @return [File] created.
     */
    fun createImageFile(context: Context, extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(context.filesDir, "IMG_${sdf.format(Date())}.$extension")
    }

    fun createVideoFile(context: Context, extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(context.filesDir, "VID_${sdf.format(Date())}.$extension")
    }

    fun createDocumentFile(context: Context, extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(context.filesDir, "FILE_${sdf.format(Date())}.$extension")
    }

    fun createAudioFile(context: Context, extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(context.filesDir, "AUDIO_${sdf.format(Date())}.$extension")
    }

//    fun File.copyTo(file: File) {
//        inputStream().use { input ->
//            file.outputStream().use { output ->
//                input.copyTo(output)
//            }
//        }
//    }

    fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(activity: Activity, vararg permissions: String) {
        ActivityCompat.requestPermissions(activity, permissions, PERMISSIONS_CODE);
    }

    fun animateView(view: View, actualSize: Int, boundSize: Int, boundWidth: Boolean = false, boundHeight: Boolean = false, duration: Long = 500L) {
        val valueAnimator = ValueAnimator.ofInt(actualSize, boundSize)
        valueAnimator.duration = duration
        valueAnimator.addUpdateListener {
            val animatedValue = valueAnimator.animatedValue as Int
            val layoutParams = view.layoutParams
            when {
                boundWidth -> {
                    layoutParams.width = animatedValue
                }
                boundHeight -> {
                    layoutParams.height = animatedValue
                }
                else -> {
                    layoutParams.width = animatedValue
                }
            }
            view.layoutParams = layoutParams
        }
        valueAnimator.start()
    }

    fun String.emojiCount(): Array<Int> {
        var emojiCount = 0
        var nonEmojiCount = 0
        for (i in this.indices) {
            val type: Int = Character.getType(this[i])
            if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt()) {
                emojiCount++
            } else {
                nonEmojiCount++
            }
        }
        return intArrayOf(emojiCount / 2, nonEmojiCount).toTypedArray()
    }

    fun manipulateColor(color: Int, factor: Float): Int {
        val a: Int = Color.alpha(color)
        val r = (Color.red(color) * factor).roundToInt()
        val g = (Color.green(color) * factor).roundToInt()
        val b = (Color.blue(color) * factor).roundToInt()
        return Color.argb(a,
                r.coerceAtMost(255),
                g.coerceAtMost(255),
                b.coerceAtMost(255))
    }

    fun String.indicesOf(substring: Char): List<Int> {
        var a = ArrayList<Int>()
        var i = -1
        while (indexOf(substring, i + 1) >= 0) {
            i = indexOf(substring, i + 1)
            a.add(i)
        }
        return a;
    }

    fun getToneTyoe(toneType: String, context: Context): Uri? {
        return when (toneType) {
            "tone_1.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_1)
            "tone_2.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_2)
            "tone_3.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_3)
            "tone_4.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_4)
            "tone_5.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_5)
            "tone_6.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_6)
            "tone_7.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_7)
            "tone_8.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_8)
            "tone_9.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_9)
            "tone_10.wav" -> Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tone_10)
            else -> null
        }
    }

    fun getMediaDuration(context: Context, path: String?): String {
        var mp = MediaPlayer.create(context, Uri.parse(path))
        if (mp != null) {
            val duration = mp.duration
            mp.release()
            return String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration.toLong()), TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong())))
        } else {
            return context.getString(R.string.media)
        }
    }

//    fun waterMarkText: String {
//        val inputString = "${Blackbox.account.registeredNumber}51e3eb37471db46a4c4f9472deb594d4a56ceae0a163728aa45b6a06ed1d43cb"
//        val inputData = Data(inputString.utf8Size(0, inputString.length))
//        val
////        MGF1ParameterSpec.SHA256.hashCode()
//    }

    @Throws(SignatureException::class)
    fun waterMarkText(text: String, secretKey: String): String? {
        return try {
            val sk: Key = SecretKeySpec(secretKey.toByteArray(), HASH_ALGORITHM)
            val mac: Mac = Mac.getInstance(sk.algorithm)
            mac.init(sk)
            val hmac: ByteArray = mac.doFinal(text.toByteArray())

            val hash: String = toHexString(hmac)
            var finText: String? = ""
            for (index in 0 until 120){
                finText += "Top Secret ${hash} # سري للغاية المصمك masmak"
            }
            finText
        } catch (e1: NoSuchAlgorithmException) {
            // throw an exception or pick a different encryption method
            throw SignatureException("error building signature, no such algorithm in device "
                    + HASH_ALGORITHM)
        } catch (e: InvalidKeyException) {
            throw SignatureException(
                    "error building signature, invalid key $HASH_ALGORITHM")
        }
    }
    private const val HASH_ALGORITHM = "HmacSHA256"

    fun toHexString(bytes: ByteArray): String {
        val sb = java.lang.StringBuilder(bytes.size * 2)
        val formatter = Formatter(sb)
        for (b in bytes) {
            formatter.format("%02x", b)
        }
        return sb.toString()
    }
}