package com.ec.bond.blackbox.model.callsHistory

import com.ec.bond.blackbox.model.BBContact

/**
 * The Calls screen will show the call grouped by type, direction and contact.
 *
 * Essentially one of this object represent a row in the Calls History View.
 */
data class BBCallHistoryGroup(var calls: ArrayList<BBCallHistory>, val direction: BBCallDirection, val type: BBCallType, val contact: BBContact) {
    // Flag used to determine if the Item is Selected or Not.
    var isSelected: Boolean = false

    // Flag used to make fake view for last item
    var isLastItem: Boolean = false
}