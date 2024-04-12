package com.ec.bond.activity.ui.settings.usagedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBAccountSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsUsageDataViewModel : ViewModel() {
    private val _isConfigUpdated = MutableLiveData<Boolean>()
    val isConfigUpdated: LiveData<Boolean> get() = _isConfigUpdated
    /* this method set configuration data in server called singletone object Blackbox and notify when data saved successfully
     in isConfigUpdated which Fragment listen to it
     */
    fun setMediaConfigration(config: BBAccountSettings) = GlobalScope.launch(Dispatchers.Main) {
        val success = Blackbox.account.updateAccountSettings(config)
        _isConfigUpdated.postValue(success)
    }
    // this method get configuration data from server
    fun fetchAccountSettings() = GlobalScope.launch(Dispatchers.Main) {
        Blackbox.account.fetchAccountConfigAsync()
    }
}