package com.ec.bond.activity.ui.chatbrowsing

import android.os.Parcelable
import com.ec.bond.blackbox.model.Message
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MessageItem(var message: Message): ChatBrowsingListItem(), Parcelable {
    override fun getType(): Int {
        return TYPE_MESSAGE
    }
}