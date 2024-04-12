package com.ec.bond.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


abstract class BaseViewModel<UiModel> : ViewModel(){
    val uiModelData = MutableLiveData<UiModel>()
}
