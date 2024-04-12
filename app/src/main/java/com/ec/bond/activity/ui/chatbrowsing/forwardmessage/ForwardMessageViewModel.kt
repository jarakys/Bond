package com.ec.bond.activity.ui.chatbrowsing.forwardmessage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ec.bond.blackbox.model.BBChat
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBGroup
import kotlin.collections.ArrayList

@Suppress("LABEL_NAME_CLASH")
class ForwardMessageViewModel: ViewModel() {
    private var _selectedContacts = MutableLiveData<ArrayList<BBChat>>().apply {
        value = ArrayList()
    }
    private val _selectionTitle = MutableLiveData<String>()

    val selectionTitle: LiveData<String> get() = _selectionTitle

    fun addToSelectedContacts(item: BBChat){
        _selectedContacts.value?.add(item)
        setSelectionTitle()
    }

    fun clearSelectedContacts(){
        _selectedContacts.value?.clear()
        setSelectionTitle()
    }

    fun getSizeSelectedContacts(): Int = _selectedContacts.value?.size!!


    fun removeItemSelectedContacts(item: BBChat) {
        val removedItem: BBChat
        if (item is BBContact) {
            removedItem = _selectedContacts.value?.filterIsInstance<BBContact>()?.first { it.ID == item.ID }!!
            _selectedContacts.value?.remove(removedItem)
        } else if (item is BBGroup){
            removedItem = _selectedContacts.value?.filterIsInstance<BBGroup>()?.first { it.ID == item.ID }!!
            _selectedContacts.value?.remove(removedItem)
        }
        setSelectionTitle()
    }

    fun getSelectedContacts() : ArrayList<BBChat> {
        return _selectedContacts.value!!
    }
    fun setSelectionTitle() {
        var title = ""
        _selectedContacts.value?.forEach {
            if (it is BBContact) {
                if (it.name.isNotEmpty())
                    title += it.name + ", "
                else
                    title += it.registeredNumber + ", "
            } else if (it is BBGroup){
                title += it.desc + ", "
            }
        }
        _selectionTitle.postValue(title.dropLast(2))
    }



}