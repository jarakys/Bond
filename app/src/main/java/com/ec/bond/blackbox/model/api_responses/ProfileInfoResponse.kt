package com.ec.bond.blackbox.model.api_responses

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.ec.bond.blackbox.model.*
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class ProfileInfoJsonDeserializer : JsonDeserializer<ProfileInfoResponse> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ProfileInfoResponse {
        if (json == null) return ProfileInfoResponse()

        json.asJsonObject?.let { jsonObject ->
            var answer: String = ""
            var message: String = ""
            var name: String = ""
            var onlineStatus: BBStatus = BBStatus.offline
            var statusMessage: String = ""
            var lastSeen: Date? = null
            var photoName: String = ""
            var uidRecipient: String = ""
            var forceUpdate: Boolean = false
            var currentAppVersion: String = ""
            var currentAndroidVersion: String = ""
            var updateUrl: String = ""
            var onlineVisibility: Boolean = false

            jsonObject.get("answer")?.let {
                if (it.isJsonNull == false) {
                    answer = it.asString
                }
            }
            jsonObject.get("message")?.let {
                if (it.isJsonNull == false) {
                    message = it.asString
                }
            }
            jsonObject.get("name")?.let {
                if (it.isJsonNull == false) {
                    name = it.asString
                }
            }
            jsonObject.get("onlinestatus")?.let {
                if (it.isJsonNull == false) {
                    onlineStatus = if (it.asString.equals("online")) BBStatus.online else BBStatus.offline
                }
            }
            jsonObject.get("status")?.let {
                if (it.isJsonNull == false) {
                    statusMessage = it.asString
                }
            }
            jsonObject.get("lastseen")?.let {
                if (it.isJsonNull == false) {
                    if (it.equals("0000-00-00 00:00:00") == false) {
                        lastSeen = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.asString)
                    }
                }
            }
            jsonObject.get("photoname")?.let {
                if (it.isJsonNull == false) {
                    photoName = it.asString
                }
            }
            jsonObject.get("uidrecipient")?.let {
                if (it.isJsonNull == false) {
                    uidRecipient = it.asString
                }
            }
            jsonObject.get("forceupdate")?.let {
                if (it.isJsonNull == false) {
                    forceUpdate = it.asString.equals("Y")
                }
            }
            jsonObject.get("currentappversion")?.let {
                if (it.isJsonNull == false) {
                    currentAppVersion = it.asString
                }
            }
            jsonObject.get("updateversionandroid")?.let {
                if (it.isJsonNull == false) {
                    currentAndroidVersion = it.asString
                }
            }
            jsonObject.get("updateurlandroid")?.let {
                if (it.isJsonNull == false) {
                    updateUrl = it.asString
                }
            }
            jsonObject.get("onlinevisibility")?.let {
                if (it.isJsonNull == false) {
                    onlineVisibility = it.asString.equals("Y")
                }
            }



            return ProfileInfoResponse(answer, message, name, onlineStatus, statusMessage, lastSeen,
            photoName, uidRecipient, forceUpdate, currentAppVersion, currentAndroidVersion,
            updateUrl, onlineVisibility)
        }


        return ProfileInfoResponse()
    }

}

data class ProfileInfoResponse(
        val answer: String,
        val message: String,
        val name: String,
        val onlineStatus: BBStatus,
        val statusMessage: String,
        val lastSeen: Date?,
        val photoName: String,
        val uidRecipient: String,
        val forceUpdate: Boolean,
        val currentAppVersion: String,
        val currentAndroidVersion: String,
        val updateUrl: String,
        @SerializedName("onlinevisibility") val onlineVisibility: Boolean
) {
    constructor() : this("", "", "", BBStatus.offline,
            "", null, "", "", false,
            "", "", "", false)

    val isSuccess: Boolean get() = answer.equals("OK")
}