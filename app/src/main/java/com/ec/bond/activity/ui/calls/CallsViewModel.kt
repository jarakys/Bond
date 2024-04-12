package com.ec.bond.activity.ui.calls

import android.view.Menu
import android.view.MenuInflater
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ec.bond.blackbox.model.callsHistory.BBCallHistoryGroup


class CallsViewModel : ViewModel() {

    private val _isLongPressed = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isLongPressed: LiveData<Boolean> get() = _isLongPressed

    private val _longPressedTitle = MutableLiveData<String>()

    val longPressedTitle: LiveData<String> get() = _longPressedTitle

    var menuActionBar = MutableLiveData<Menu>()

    var menuInflater = MutableLiveData<MenuInflater>()

    private var _selectedCalls = MutableLiveData<ArrayList<BBCallHistoryGroup>>().apply {
        value = ArrayList()
    }


    fun addToSelectedCalls(item: BBCallHistoryGroup){
        _selectedCalls.value?.add(item)
    }

    fun clearSelectedCalls(){
        _selectedCalls.value?.clear()
    }

    fun getSizeSelectedCalls(): Int? {
       return _selectedCalls.value?.size
    }

    fun removeItemSelectedCalls(item: BBCallHistoryGroup) {
        val removedItem = _selectedCalls.value?.filter { it.isSelected == item.isSelected }?.first()
        removedItem!!.isSelected = false
        _selectedCalls.value?.remove(removedItem)
    }

    fun getSelectedCalls() : ArrayList<BBCallHistoryGroup> {
        return _selectedCalls.value!!
    }

    fun setIsLongPressed(isLongPress: Boolean) = _isLongPressed.postValue(isLongPress)

    fun setLongPressedTitle(title: String) = _longPressedTitle.postValue(title)

//    fun getLastVoiceCalls() = GlobalScope.launch(Dispatchers.Main) {
//        if (Blackbox.fetchCallsHistoryAsync()) {
//            // Success
//        } else {
//            // Fail
//        }
//    }

}