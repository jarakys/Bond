package com.ec.bond.activity.ui.chatbrowsing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ec.bond.blackbox.model.BBChat

class ChatBrowsingDetailFactory(private val chatTypeRef: BBChat) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(ChatBrowsingDetailViewModel::class.java)) {
            ChatBrowsingDetailViewModel(chatTypeRef) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }

}