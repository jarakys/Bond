package com.ec.bond.blackbox.model.api_responses

import com.google.gson.annotations.SerializedName

data class FetchAutoDeleteTimerResponse(override val answer: String,
                                        override val message: String,
                                        @SerializedName("autodeleteseconds") val seconds: Int) : BaseResponse()