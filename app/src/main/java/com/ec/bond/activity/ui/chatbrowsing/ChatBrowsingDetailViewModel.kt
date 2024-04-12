package com.ec.bond.activity.ui.chatbrowsing

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBChat
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBGroup
import com.ec.bond.blackbox.model.BBGroupRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ChatBrowsingDetailViewModel(chatRefType: BBChat) : ViewModel() {
    private val observeClearChat = MutableLiveData<Boolean>()
    private val observeMemberListResponse = MutableLiveData<Boolean>()
    private val observeDescriptionResponse = MutableLiveData<Boolean>()
    private val observeExitAndDeleteGroupResponse = MutableLiveData<Boolean>()
    private val observeRingtoneNameResponse = MutableLiveData<Pair<Boolean?, String?>>()
    private val observeGroupExpiryDateResponse = MutableLiveData<Triple<Boolean?, Date?, Boolean?>>()
    private val observeAddMembersResponse = MutableLiveData<List<BBContact>>()
    private lateinit var date: Date
    private val arrayList = arrayListOf("Default", "tone - 1", "tone - 2", "tone - 3", "tone - 4", "tone - 5", "tone - 6", "tone - 7", "tone - 8", "tone - 9", "tone - 10")

    var contact: BBContact? = null
    var group: BBGroup? = null

    init {
        if (chatRefType is BBContact) {
            contact = chatRefType
            observeRingtoneNameResponse.postValue(Pair(true, getRingtoneName()))
        } else if (chatRefType is BBGroup) {
            group = chatRefType
            observeGroupExpiryDateResponse.postValue(Triple(true, group?.expiryDate, group?.expiryDate != null))
            observeRingtoneNameResponse.postValue(Pair(true, getRingtoneName()))
        }
    }

    fun isGroupChat(): Boolean = group != null

    fun getChatName(): String = contact?.name ?: contact?.registeredNumber
    ?: group?.description?.value ?: ""

    fun getRegisteredNumber(): String = contact?.registeredNumber ?: ""

    fun getStatusMessage(): String = contact?.statusMessage ?: ""

    fun getImage(): String? = contact?.getChatImagePath()

    fun getContactData(): BBContact? = contact

    fun setGroupDeletedDate(date: Date) {
        this.date = date
    }

    fun getRingtoneName(): String {
        val fileName = group?.messageNotificationSoundName ?: contact?.messageNotificationSoundName
        ?: ""
        return when (fileName) {
            "tone_1.wav" -> arrayList[1]
            "tone_2.wav" -> arrayList[2]
            "tone_3.wav" -> arrayList[3]
            "tone_4.wav" -> arrayList[4]
            "tone_5.wav" -> arrayList[5]
            "tone_6.wav" -> arrayList[6]
            "tone_7.wav" -> arrayList[7]
            "tone_8.wav" -> arrayList[8]
            "tone_9.wav" -> arrayList[9]
            "tone_10.wav" -> arrayList[10]
            else -> arrayList[0]
        }
    }

    fun getGroupDeletedDate(): Date = date

    fun clearChat() = viewModelScope.launch(Dispatchers.IO) {
        if (group != null) {
            observeClearChat.postValue(group?.clearChat())
        } else {
            observeClearChat.postValue(contact?.clearChat())
        }
    }

    fun getClearChatResponse(): MutableLiveData<Boolean> = observeClearChat

    fun getGroupMembers(): ArrayList<BBContact> = group?.getMembers() ?: arrayListOf()

    fun isGroupAdmin(): Boolean = group?.isAccountGroupAdmin() ?: false

    fun updateMemberRole(contact: BBContact, role: BBGroupRole) = viewModelScope.launch(Dispatchers.IO) {
        observeMemberListResponse.postValue(group?.changeMemberRole(contact, role))
    }

    fun getMemberListResponse(): MutableLiveData<Boolean> = observeMemberListResponse

    fun removeMember(contact: BBContact) = viewModelScope.launch(Dispatchers.IO) {
        observeMemberListResponse.postValue(group?.removeMember(contact))
    }

    fun updateGroupDescription(description: String) = viewModelScope.launch(Dispatchers.IO) {
        observeDescriptionResponse.postValue(group?.setGroupDescription(description))
    }

    fun getDescriptionUpdateResponse(): MutableLiveData<Boolean> = observeDescriptionResponse

    fun exitGroup() = viewModelScope.launch {
        observeExitAndDeleteGroupResponse.postValue(group?.exitGroup())
    }

    fun getExitAndDeleteGroupResponse(): MutableLiveData<Boolean> = observeExitAndDeleteGroupResponse

    fun addGroupMembers(contacts: List<BBContact>) = viewModelScope.launch {
        observeAddMembersResponse.postValue(group?.addMembers(contacts))
    }

    fun getAddGroupMembersResponse(): MutableLiveData<List<BBContact>> = observeAddMembersResponse

    fun deleteGroup() = viewModelScope.launch {
        observeExitAndDeleteGroupResponse.postValue(group?.deleteGroup())
    }

    fun setGroupExpiryDate(date: Date?, switchCheckedState: Boolean) = viewModelScope.launch {
        val response = group?.setExpiryDate(date)
        observeGroupExpiryDateResponse.postValue(Triple(response, group?.expiryDate, switchCheckedState))
    }

    fun getGroupExpiryDateResponse(): MutableLiveData<Triple<Boolean?, Date?, Boolean?>> = observeGroupExpiryDateResponse

    fun setRingtone(ringtoneName: String) = viewModelScope.launch {
        if (group != null) {
            observeRingtoneNameResponse.postValue(Pair(group?.let { Blackbox.setGroupNotification(it, ringtoneName) }, getRingtoneName()))
        } else {
            observeRingtoneNameResponse.postValue(Pair(contact?.let { Blackbox.setContactNotification(it, ringtoneName) }, getRingtoneName()))
        }
    }

    fun getRingtoneNameResponse(): MutableLiveData<Pair<Boolean?, String?>> = observeRingtoneNameResponse
}