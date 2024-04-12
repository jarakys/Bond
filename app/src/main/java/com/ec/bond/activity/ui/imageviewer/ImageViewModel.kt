package com.ec.bond.activity.ui.imageviewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import javax.inject.Inject


class ImageViewModel @Inject constructor() : ViewModel() {

    private val _msg_sent = MutableLiveData<Boolean>()
    val msg_sent: LiveData<Boolean> get() = _msg_sent

    private val _imageSelected = MutableLiveData<Int>(0)
    val imageSelected: LiveData<Int> get() = _imageSelected

    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    val paths = ArrayList<String>()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
    fun changeItemSelected(position: Int) {
        _imageSelected.postValue(position)
    }


}