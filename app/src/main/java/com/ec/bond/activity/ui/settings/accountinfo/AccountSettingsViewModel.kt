package com.ec.bond.activity.ui.settings.accountinfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBAccount
import com.ec.bond.blackbox.model.BBAccountSettings
import com.ec.bond.model.MainUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class AccountSettingsViewModel: ViewModel() {
    public val _mainUserInfo = MutableLiveData<BBAccount>()
    private val _isConfUpdated = MutableLiveData<Boolean>()
    val isConfUpdated : LiveData<Boolean> get() = _isConfUpdated
    private val _isImageSavedSuccessfully = MutableLiveData<Boolean>()
    val isImageSavedSuccessfully : LiveData<Boolean> get() = _isImageSavedSuccessfully
    fun updateAccountSettings(settings: BBAccountSettings) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = Blackbox.account.updateAccountSettings(settings)
            _isConfUpdated.postValue(result)
        }
    }

    fun setPhotoProfile(path: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = Blackbox.account.setPhotoProfile(path ?: "")
            _isImageSavedSuccessfully.postValue(result)
            File(path).delete()
        }
    }

    fun getMainUserInfo() = GlobalScope.launch(Dispatchers.Main) {
        val accountInfoSuccess = Blackbox.account.fetchAccountInfoAsync()
        if(accountInfoSuccess){
            _mainUserInfo.value=Blackbox.account
        }
    }

}