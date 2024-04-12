package com.ec.bond.blackbox.model.api_responses

import com.google.gson.annotations.SerializedName

data class CallInfoResponse(
        override val answer: String,
        override val message: String,
        @SerializedName("callerid") val callerID: String,
        @SerializedName("contactid") val contactID: String,
        @SerializedName("contactname") val contactName: String,
        @SerializedName("callid") val callID: String
) : BaseResponse()