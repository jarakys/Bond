package com.ec.bond.blackbox.model.api_responses

import com.google.gson.annotations.SerializedName


abstract class BaseResponse {
    abstract val answer: String
    abstract val message: String

    val isSuccess: Boolean get() = answer == "OK"
}

data class GeneralResponse(override val answer: String = "KO",
                           override val message: String = "",
                           val filename: String? = null,
                           val msgid: String? = null,
                           val autodelete: String? = null,
                           @SerializedName("localfilename") val localFilename: String? = null) : BaseResponse() {



}