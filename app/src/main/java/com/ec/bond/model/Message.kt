package com.ec.bond.model


import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("autodelete")
    val autodelete: String?,
    @SerializedName("dtdeleted")
    val dtdeleted: String?,
    @SerializedName("dtread")
    val dtread: String?,
    @SerializedName("dtreceived")
    val dtreceived: String?,
    @SerializedName("dtsent")
    val dtsent: String?,
    @SerializedName("filename")
    val filename: String?,
    @SerializedName("filesize")
    val filesize: String?,
    @SerializedName("forwarded")
    val forwarded: String?,
    @SerializedName("groupid")
    val groupid: String?,
    @SerializedName("msgbody")
    val msgbody: String?,
    @SerializedName("msgid")
    val msgid: String?,
    @SerializedName("msgtype")
    val msgtype: String?,
    @SerializedName("recipient")
    val recipient: String?,
    @SerializedName("repliedto")
    val repliedto: String?,
    @SerializedName("repliedtotxt")
    val repliedtotxt: String?,
    @SerializedName("sender")
    val sender: String?,
    @SerializedName("starred")
    val starred: String?
)