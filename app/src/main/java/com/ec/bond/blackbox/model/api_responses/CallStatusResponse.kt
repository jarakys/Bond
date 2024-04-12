package com.ec.bond.blackbox.model.api_responses

import com.google.gson.*
import com.ec.bond.blackbox.model.*
import java.lang.reflect.Type

class CallStatusResponseJsonDeserializer : JsonDeserializer<CallStatusResponse> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): CallStatusResponse {
        val response = CallStatusResponse.createEmpty()
        if (json == null) return response
        val jsonObject =  json.asJsonObject ?: return response

        var answer = ""
        var message = ""
        var status = BBCallStatus.none
        var callID = ""
        var conferenceMembersStatus: List<ConferenceMemberStatus>? = null

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
        jsonObject.get("status")?.let {
            if (it.isJsonNull == false) {
                status = when (it.asString) {
                    "setup" -> BBCallStatus.setup
                    "ringing" -> BBCallStatus.ringing
                    "answeredA" -> BBCallStatus.answeredAudioOnly
                    "answered" -> BBCallStatus.answered
                    "active" -> BBCallStatus.active
                    "hangup" -> BBCallStatus.hangup
                    else -> BBCallStatus.none
                }
            }
        }
        jsonObject.get("callid")?.let {
            if (it.isJsonNull == false) {
                callID = it.asString
            }
        }
        jsonObject.get("audioconference")?.let {
            if (it.isJsonNull == false) {
                conferenceMembersStatus = it.asJsonArray.filter { elem ->
                    elem.asJsonObject.get("status").asString != "hangup"
                }.map { elem ->
                    val callerID = elem.asJsonObject.get("callerid").asString
                    val calledID = elem.asJsonObject.get("calledid").asString
                    val memberStatus = when (elem.asJsonObject.get("status").asString) {
                        "setup" -> BBCallStatus.setup
                        "ringing" -> BBCallStatus.ringing
                        "answeredA" -> BBCallStatus.answeredAudioOnly
                        "answered" -> BBCallStatus.answered
                        "active" -> BBCallStatus.active
                        "hangup" -> BBCallStatus.hangup
                        else -> BBCallStatus.none
                    }
                    ConferenceMemberStatus(callerID, calledID, memberStatus)
                }
            }
        }

        return CallStatusResponse(answer, message, status, callID, conferenceMembersStatus)
    }
}

data class CallStatusResponse(
        val answer: String,
        val message: String,
        val status: BBCallStatus,
        val callID: String,
        val conferenceMembersStatus: List<ConferenceMemberStatus>? = null
) {

    val isSuccess : Boolean get() = answer == "OK"

    companion object {
        fun createEmpty() : CallStatusResponse {
            return CallStatusResponse(answer = "KO", message = "Invalid Json", status = BBCallStatus.none, callID = "")
        }
    }

}


data class ConferenceMemberStatus(val callerID: String, val calledID: String, val status: BBCallStatus)