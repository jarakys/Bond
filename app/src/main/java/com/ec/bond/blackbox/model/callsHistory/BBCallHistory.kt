package com.ec.bond.blackbox.model.callsHistory

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Class that hold the call history data
 */
@Parcelize
data class BBCallHistory(var callid: String,
                         var recipient: String,
                         var name: String,
                         var photoname: String,
                         var direction: String,
                         var dtsetup: String,
                         var dtanswer: String,
                         var dthangup: String,
                         var duration: String,
                         var video: String) : Parcelable {
    val type: BBCallType
        get() = if (video == "N") BBCallType.Call else BBCallType.Video

    val directionType: BBCallDirection
        get() = if (direction == "outbound") BBCallDirection.Outgoing else BBCallDirection.Incoming

}