package com.ec.bond.blackbox.model.api_responses

import com.google.gson.annotations.SerializedName

data class FetchNotificationSoundResponse(override val answer: String = "KO",
                                          override val message: String = "",
                                          @SerializedName("notifications") val notificationsSounds: List<BBNotificationSound>) : BaseResponse()

data class BBNotificationSound(@SerializedName("contactnumber") val contactNumber: String,
                               @SerializedName("groupchatid") val groupChatId: String,
                               @SerializedName("soundname") val soundName: String)