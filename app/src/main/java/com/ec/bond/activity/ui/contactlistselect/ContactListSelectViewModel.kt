package com.ec.bond.activity.ui.contactlistselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ec.bond.model.ChatList
import kotlinx.coroutines.*
import javax.inject.Inject


class ContactListSelectViewModel @Inject constructor(): ViewModel() {

    private val _contact_data = MutableLiveData<ArrayList<ChatList>>()
    val contact_data: LiveData<ArrayList<ChatList>> get() = _contact_data

    private val _retringDone = MutableLiveData<Boolean>()
    val retringDone: LiveData<Boolean> get() = _retringDone

    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    private val _msg_sent = MutableLiveData<String>()
    val msg_sent: LiveData<String> get() = _msg_sent

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }



    fun retry() {

    }

    fun send_file(uri: String, msg: String, recipients: ArrayList<String>){

    }


}