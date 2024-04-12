package com.ec.bond.activity.ui.chatbrowsing

abstract class ChatBrowsingListItem {
    companion object {
        const val TYPE_DATE = 0
        const val TYPE_MESSAGE = 1
    }

    abstract fun getType(): Int
}