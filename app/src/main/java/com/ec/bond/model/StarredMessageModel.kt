package com.ec.bond.model


import com.google.gson.annotations.SerializedName

data class StarredMessageModel(
    @SerializedName("answer")
    val answer: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("messages")
    val messages: List<Message>?,
    @SerializedName("token")
    val token: String?
)