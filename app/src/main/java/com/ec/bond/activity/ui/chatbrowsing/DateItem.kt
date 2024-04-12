package com.ec.bond.activity.ui.chatbrowsing

import java.util.*

class DateItem(var date: Date): ChatBrowsingListItem() {
    override fun getType(): Int {
        return TYPE_DATE
    }
}