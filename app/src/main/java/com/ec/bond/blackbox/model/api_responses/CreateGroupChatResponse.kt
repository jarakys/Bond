package com.ec.bond.blackbox.model.api_responses

import com.google.gson.annotations.SerializedName

data class CreateGroupChatResponse(
        override val answer: String,
        override val message: String,
        @SerializedName("groupid") val groupID: String
) : BaseResponse()