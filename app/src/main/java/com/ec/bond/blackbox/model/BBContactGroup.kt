package com.ec.bond.blackbox.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BBContactGroup(var contact: BBContact,
                          var isselected: Boolean) : Parcelable{
}