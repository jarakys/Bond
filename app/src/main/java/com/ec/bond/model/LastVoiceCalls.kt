package com.ec.bond.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class LastVoiceCalls(var callid: String,
                     var recipient: String,
                     var name: String,
                     var photoname: String,
                     var direction: String,
                     var dtsetup: String,
                     var dtanswer: String,
                     var dthangup: String,
                     var duration: String,
                     var video: String): Parcelable {

}