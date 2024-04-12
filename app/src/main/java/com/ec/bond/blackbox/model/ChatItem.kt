package com.ec.bond.blackbox.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * This Object represent a single ChatList Item
 */

@Parcelize
data class ChatItem(val contact: BBContact?,
               var group: BBGroup?,
               var lastMessage: Message? = null,
               var isArchived: Boolean = false,
               var isSelected: Boolean = false): Parcelable {
    val isGroup: Boolean get() = group != null
}

fun ArrayList<ChatItem>.isEqual(rhs: List<ChatItem>) : Boolean {

    if (size != rhs.size) return false

    return zip(rhs).all { (chatItem1, chatItem2) ->
        if (chatItem1.isGroup && chatItem2.isGroup) {
            // Compare the 2 groups
            val g1 = chatItem1.group!!
            val g2 = chatItem2.group!!

            if (g1.ID == g2.ID) {
                // compare the last message
                if (chatItem1.lastMessage == null && chatItem2.lastMessage == null) {
                    return@all true
                }
                val m1 = chatItem1.lastMessage ?: return@all false
                val m2 = chatItem2.lastMessage ?: return@all false

                if (m1.ID == m2.ID && m1.checkmarkType.value == m2.checkmarkType.value) {
                    return@all true
                }
            }
        } else if (chatItem1.isGroup == false && chatItem2.isGroup == false) {
            // Compare the 2 contacts
            // Compare the 2 groups
            val c1 = chatItem1.contact!!
            val c2 = chatItem2.contact!!

            if (c1.registeredNumber == c2.registeredNumber) {
                // compare the last message
                if (chatItem1.lastMessage == null && chatItem2.lastMessage == null) {
                    return@all true
                }
                val m1 = chatItem1.lastMessage ?: return@all false
                val m2 = chatItem2.lastMessage ?: return@all false

                if (m1.ID == m2.ID && m1.checkmarkType.value == m2.checkmarkType.value) {
                    return@all true
                }
            }
        }
        return@all false
    }
}
