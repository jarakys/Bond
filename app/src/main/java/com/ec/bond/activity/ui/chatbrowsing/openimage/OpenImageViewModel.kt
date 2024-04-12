package com.ec.bond.activity.ui.chatbrowsing.openimage

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.concurrent.TimeUnit

class OpenImageViewModel: ViewModel() {
    lateinit var videoThumbnail: Bitmap
    lateinit var path: String

    private val _isPlaying = MutableLiveData<Boolean>(true)

    val isPlaying: LiveData<Boolean> get() = _isPlaying

    fun getThumbnailAtSpecificTime(duration: Int) : Bitmap?{
        var retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)

            return retriever.getFrameAtTime((TimeUnit.MILLISECONDS.toMicros(duration.toLong())),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (ex: Exception) {
            Log.i("videoExp", "MediaMetadataRetriever got exception:$ex")
        }
        return null
    }

    fun setVideoPlay(isPlay: Boolean) {
        _isPlaying.postValue(isPlay)
    }
}