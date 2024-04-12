package com.ec.bond.activity.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBPhoneNumber
//import com.spe2eeapp.masmak.blackbox.model.BBAccountOnlineStatus
import com.ec.bond.blackbox.model.ChatItem
import com.ec.bond.model.ChatList
import kotlinx.coroutines.*
import javax.inject.Inject


class ChatViewModel @Inject constructor() : ViewModel() {

    private val _presence_registered = MutableLiveData<Boolean>()
    val presence_registered: LiveData<Boolean> get() = _presence_registered

    private val _contact_data = MutableLiveData<ArrayList<ChatList>>()
    val contact_data: LiveData<ArrayList<ChatList>> get() = _contact_data

    private val _decrypt_pwdconf = MutableLiveData<Boolean>()
    val pwdconf_decrypted: LiveData<Boolean> get() = _decrypt_pwdconf

    lateinit var chatItems: ArrayList<ChatItem>

    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.IO + viewModelJob)
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun updateData() = viewModelScope.launch(Dispatchers.IO) {
        if (Blackbox.fetchContactsAsync())
            Blackbox.fetchChatListAsync()
    }

    fun registerNewContact () {
        val contact = BBContact()
        var number = "803012345678"
//        var number = "9660005015"
        contact.registeredNumber = number
        contact.phonesjson = listOf(BBPhoneNumber("mobile", number, ""))
        contact.phonejsonreg = listOf(BBPhoneNumber("mobile", number, ""))
//        contact.name = "Amr"
        contact.name = "Hassan"

        GlobalScope.launch {
            if (Blackbox.addContactAsync(contact))
                Log.d("Add Contact", "Success")
            else
                Log.d("Add Contact", "Failed")
        }
    }
}