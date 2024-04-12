package com.ec.bond.activity.ui.contactlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.callsHistory.BBCallHistoryGroup
import kotlinx.coroutines.launch
import javax.inject.Inject


class ContactListViewModel @Inject constructor(): ViewModel() {
    public val contactResult = MutableLiveData<Boolean>()
//    fun fetchContacts() = GlobalScope.launch(Dispatchers.Main) {
//        if (Blackbox.fetchContactsAsync()) {
//            // success
//        } else {
//            // fail
//        }
//    }
    fun addContact(contact: BBContact) = viewModelScope.launch {
        Blackbox.addContactAsync(contact)
    }

    fun deleteContact(contact: BBContact)=viewModelScope.launch {
        contactResult.value=   Blackbox.deleteContactAsync(contact)
    }

    private val _isLongPressed = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isLongPressed: LiveData<Boolean> get() = _isLongPressed

    fun setIsLongPressed(isLongPress: Boolean) = _isLongPressed.postValue(isLongPress)

    private var _selectedContact = MutableLiveData<ArrayList<BBContact>>().apply {
        value = ArrayList()
    }

    private var _selectedCalls = MutableLiveData<ArrayList<BBContact>>().apply {
        value = ArrayList()
    }


    fun addToSelectedCalls(item: BBContact){
        _selectedCalls.value?.add(item)
    }

    fun clearSelectedCalls(){
        _selectedCalls.value?.clear()
    }

    fun getSizeSelectedCalls(): Int? {
        return _selectedCalls.value?.size
    }

    fun removeItemSelectedCalls(item: BBContact) {
        val removedItem = _selectedCalls.value?.filter { it.isSelected == item.isSelected }?.first()
        removedItem!!.isSelected = false
        _selectedCalls.value?.remove(removedItem)
    }

    fun getSelectedCalls() : ArrayList<BBContact> {
        return _selectedCalls.value!!
    }

}