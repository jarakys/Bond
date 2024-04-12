package com.ec.bond.activity.ui.settings.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.model.MainUserInfo
import kotlinx.coroutines.*
import javax.inject.Inject

class MainSettingsViewModel @Inject constructor() : ViewModel() {

    private val _mainUserInfo = MutableLiveData<MainUserInfo>()
    val mainUserInfo: LiveData<MainUserInfo> get() = _mainUserInfo

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    fun getMainUserInfo() = GlobalScope.launch(Dispatchers.Main) {
        Blackbox.account.fetchAccountInfoAsync()
        println(Blackbox.account)
    }
}