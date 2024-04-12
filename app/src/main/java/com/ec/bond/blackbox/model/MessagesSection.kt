package com.ec.bond.blackbox.model

import java.util.*
import kotlin.collections.ArrayList

/**
 * This object will store an array of messages sent/received on a specific date.
 */
data class MessagesSection (var date: Date, var messagesSection: ArrayList<Message>)
