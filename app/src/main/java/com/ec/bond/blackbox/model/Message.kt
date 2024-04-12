package com.ec.bond.blackbox.model

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.utils.*
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

enum class MessageType {
    Text,
    Location,
    Photo,
    Video,
    Audio,
    Contact,
    DocumentPDF,
    DocumentGeneric,
    DocumentText,
    DocumentMicrosoftWord,
    DocumentMicrosoftExcel,
    DocumentMicrosoftPowerPoint,
    DocumentApplePages,
    DocumentAppleNumbers,
    DocumentAppleKeynote,
    Received,
    Read,
    Typing,
    Deleted,
    AlertCopy,
    AlertForward,
    AlertDelete,
    AlertScreenshot,
    AlertScreenRecording,
    DateHeaderSection,
    System,
    SystemAutoDelete,
    SystemTemporaryChat,
    SystemMissedAudioCall,
    SystemMissedVideoCall,
    SystemGroupNameChanged,
    Status;

    val isDocumentMessage: Boolean get() {
        return when (this) {
            DocumentPDF, DocumentGeneric, DocumentText, DocumentMicrosoftWord,
            DocumentMicrosoftExcel, DocumentMicrosoftPowerPoint, DocumentApplePages,
            DocumentAppleNumbers, DocumentAppleKeynote -> true
            else -> false
        }
    }

    val isSystemMessage : Boolean get() {
        return when (this) {
            System, SystemAutoDelete, SystemTemporaryChat, SystemMissedAudioCall,
            SystemMissedVideoCall, SystemGroupNameChanged -> true
            else -> return false
        }
    }

    val isAlertMessage : Boolean get() {
        return when (this) {
            AlertCopy, AlertForward, AlertDelete, AlertScreenshot, AlertScreenRecording -> true
            else -> false
        }
    }
}

enum class CheckmarkType {
    none, unSent, sent,received, read
}

class MessageJsonDeserializer : JsonDeserializer<Message> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Message {
        if (json == null) return Message()

        json.asJsonObject?.let { jsonObject ->
            val message = Message()
            jsonObject.get("answer")?.let {
                if (it.isJsonNull == false) {
                    message.answer = it.asString
                }
            }
            jsonObject.get("msgid")?.let {
                if (it.isJsonNull == false) {
                    message.ID = it.asString
                }
            }
            jsonObject.get("msgtype")?.let {
                if (it.isJsonNull == false) {
                    message.msgtype = it.asString
                }
            }
            jsonObject.get("sender")?.let {
                if (it.isJsonNull == false) {
                    message.sender = it.asString
                }
            }
            jsonObject.get("contactid")?.let {
                if (it.isJsonNull == false) {
                    message.contactID = it.asString
                }
            }
            jsonObject.get("recipient")?.let {
                if (it.isJsonNull == false) {
                    message.recipient = it.asString
                }
            }
            jsonObject.get("archived")?.let {
                if (it.isJsonNull == false) {
                    message.isArchived = it.asString == "Y"
                }
            }
            jsonObject.get("forwarded")?.let {
                if (it.isJsonNull == false) {
                    message.isForwarded = it.asString == "1"
                }
            }
            jsonObject.get("filename")?.let {
                if (it.isJsonNull == false) {
                    message.fileName = it.asString
                }
            }
            jsonObject.get("localfilename")?.let {
                if (it.isJsonNull == false) {
                    message._localFileName.postValue(it.asString)
                }
            }
            jsonObject.get("originfilename")?.let {
                if (it.isJsonNull == false) {
                    message.originFileName = it.asString
                }
            }
            jsonObject.get("starred")?.let {
                if (it.isJsonNull == false) {
                    message._isStarred.postValue(it.asString == "1")
                }
            }
            jsonObject.get("filesize")?.let {
                if (it.isJsonNull == false) {
                    message.fileSize = it.asLong
                }
            }
            jsonObject.get("msgbody")?.let {
                if (it.isJsonNull == false) {
                    message.body = it.asString
                }
            }
            jsonObject.get("photoname")?.let {
                if (it.isJsonNull == false) {
                    message.contactPhoto = it.asString
                }
            }
            jsonObject.get("groupphoto")?.let {
                if (it.isJsonNull == false) {
                    message.groupPhoto = it.asString
                }
            }
            jsonObject.get("repliedto")?.let {
                if (it.isJsonNull == false) {
                    message.repliedToMsgId = it.asString
                }
            }
            jsonObject.get("repliedtotxt")?.let {
                if (it.isJsonNull == false) {
                    message.repliedToText = it.asString
                }
            }
            jsonObject.get("groupid")?.let {
                if (it.isJsonNull == false) {
                    message.groupID = it.asString
                }
            }
            jsonObject.get("groupdesc")?.let {
                if (it.isJsonNull == false) {
                    message.groupDescritpion = it.asString
                }
            }
            jsonObject.get("groupdtexpiry")?.let {
                if (it.isJsonNull == false) {
                    if (it.asString != "0000-00-00 00:00:00") {
                        message.groupDateExpiry = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.asString)?.getDateInTimezone()
                    }
                }
            }
            jsonObject.get("dtsent")?.let {
                if (it.isJsonNull == false) {
                    if (it.asString != "0000-00-00 00:00:00")
                    {
                        message.dateSent = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.asString)?.getDateInTimezone()
                    }
                }
            }
            jsonObject.get("dtreceived")?.let {
                if (it.isJsonNull == false) {
                    if (it.asString != "0000-00-00 00:00:00") {
                        message.dateReceived = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.asString)?.getDateInTimezone()
                    }
                }
            }
            jsonObject.get("dtread")?.let {
                if (it.isJsonNull == false) {
                    if (it.asString != "0000-00-00 00:00:00") {
                        message.dateRead = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.asString)?.getDateInTimezone()
                    }
                }
            }
            jsonObject.get("dtdeleted")?.let {
                if (it.isJsonNull == false) {
                    if (it.asString.contains("0000-00-00 00:00:00") == false) {     // edit by hazem some times make error because json return ""0000-00-00 00:00:00""
                        message.dateDeleted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.asString)?.getDateInTimezone()
                    }
                }
            }
            jsonObject.get("autodelete")?.let {
                if (it.isJsonNull == false) {
                    message.setAutoDelete(it.asString == "1")
                }
            }
            jsonObject.get("olderunreadmsgid")?.let {
                if (it.isJsonNull == false) {
                    message.oldestUnreadMessageID = it.asString
                }
            }
            jsonObject.get("unreadmsg")?.let {
                if (it.isJsonNull == false) {
                    message.chatUnreadMessagesCount = it.asInt
                }
            }
            jsonObject.get("totunreadmsg")?.let {
                if (it.isJsonNull == false) {
                    message.totUnreadMsgs = it.asInt
                }
            }
            jsonObject.get("autodownload")?.let {
                if (it.isJsonNull == false) {
//                    message.totUnreadMsgs = it.asInt      // error line -->fixed by Hazem
                    message._autoDownload.postValue(it.asString == "Y")
                }
            }
            jsonObject.get("queuemsgs")?.let {
                if (it.isJsonNull == false) {
                    message.queue = it.asInt
                }
            }

            message.deliveredToServer = true
            finalizeMessage(message)

            if (message.isOutgoing == false || message.type == MessageType.Text && message.body.isBlank()) {
                message.setCheckmarkType(CheckmarkType.none)
            }
            else {
                when {
                    message.dateRead != null -> {
                        message.setCheckmarkType(CheckmarkType.read)
                    }
                    message.dateReceived != null -> {
                        message.setCheckmarkType(CheckmarkType.received)
                    }
                    else -> {
                        message.setCheckmarkType(CheckmarkType.sent)
                    }
                }
            }

            return message
        }

        return Message()
    }

    private fun finalizeMessage(message: Message) {
        // If deleted
        message.dateDeleted?.let {
            val isAutoDelete = message.isAutoDelete.value ?: false
            message.body = if (isAutoDelete) {
                "This message has been self disappeared automatically"
            } else {
                if (message.isOutgoing) "_You deleted this message_" else "This message has been deleted"
            }
            message.repliedToMsgId = ""
            message.repliedToText = ""
            message.type = MessageType.Deleted
            return
        }

        var type: MessageType? = null

        when (message.msgtype) {
            "status"        -> type = MessageType.Status
            "location"      -> type = MessageType.Location
            "received"      -> type = MessageType.Received
            "read"          -> type = MessageType.Read
            "typing"        -> type = MessageType.Typing
            "deleted"       -> {
                message.repliedToMsgId = ""
                message.repliedToText = ""
                type = MessageType.Deleted
            }
            "system"        -> {
                // The default MessageType for this section.
                type = MessageType.System
                when {
                    message.body.contains("[AUTODELETE]") -> {
                        type = MessageType.SystemAutoDelete

                        message.body = message.body.replace("[AUTODELETE]", "")
                        val startIndex = message.body.indexOf("[")
                        val endIndex = message.body.lastIndexOf("]")
                        if (startIndex >= 0 && endIndex >= 1) {
                            val seconds = message.body.substring(startIndex+1, endIndex)

                            message.body = when (seconds) {
                                "3600 seconds" -> "Messages will self disappear after *1 hour* from this point.\n Tap here to change settings"
                                "7200 seconds" -> "Messages will self disappear after *2 hours* from this point.\n Tap here to change settings"
                                "86400 seconds" -> "Messages will self disappear after *1 day* from this point.\n Tap here to change settings"
                                "172800 seconds" -> "Messages will self disappear after *2 days* from this point.\n Tap here to change settings"
                                "604800 seconds" -> "Messages will self disappear after *1 week* from this point.\n Tap here to change settings"
                                else -> {
                                    "Self-disappearing messages have been canceled from this point.\n Tap here to change settings."
                                }
                            }
                        }
                    }
                    message.body.contains("<EXPIRYDATETIME>") -> {
                        type = MessageType.SystemTemporaryChat

                        val dateString = message.body.replace("This conversation will be deleted on ", "")
                                .replace("<EXPIRYDATETIME>", "")
                                .replace("</EXPIRYDATETIME>", "")

                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString)?.let {
                            message.body = "This conversation will be deleted on:\n" + it.getDateInTimezone().dateString(DateStyle.medium) + " at " + it.getDateInTimezone().timeString(TimeStyle.short)
                        }
                    }
                    message.body.contains("You have been added to Chat Group") -> {
                        message.body = message.body.replace("You have been added to Chat Group", "You have been added to Chat Group")
                    }
                    message.body.contains("Your role in the Chat Group") -> {
                        message.body = message.body.replace("Your role in the Chat Group", "Your role in the Chat Group")
                    }
                    message.body.contains("has been changed to administrator") -> {
                        message.body = message.body.replace("has been changed to administrator", "has been changed to administrator")
                    }
                    message.body.contains("You have created this Chat Group") -> {
                        message.body = message.body.replace("You have created this Chat Group", "You have created this Chat Group")
                    }
                    message.body.contains("Missed audio call") -> {
                        type = MessageType.SystemMissedAudioCall
                        message.dateSent?.let {
                            message.body = "Missed Audio Call\n" + it.timeString(TimeStyle.short)
                        }
                    }
                    message.body.contains("Missed video call") -> {
                        type = MessageType.SystemMissedVideoCall
                        message.dateSent?.let {
                            message.body = "Missed Video Call\n" + it.timeString(TimeStyle.short)
                        }
                    }
                    message.body.contains("The group's name is changed to:", true) -> {
//                            t = MessageType.SystemGroupNameChanged
                    }
                    message.body.contains("has left this Chat Group", true) -> {
                        val contactNumber = message.body.replace("has left this Chat Group", "")
                                .replace("<CONTACTNUMBER>", "")
                                .replace("</CONTACTNUMBER>", "")
                                .replace(" ", "")

                        val contact = Blackbox.getContact(contactNumber)?.getContactName()
                                ?: Blackbox.getTemporaryContact(contactNumber)?.getContactName()
                                ?: contactNumber
                        message.body = "*$contact* has left this Chat Group"

                        message.removedFromGroupContactNumber = contactNumber
                    }
                    message.body.contains("has joined this Chat Group", true) -> {
                        val contactNumber = message.body
                                .replace("has joined this Chat Group", "")
                                .replace("<CONTACTNUMBER>", "")
                                .replace("</CONTACTNUMBER>", "")
                                .replace(" ", "")

                        val contact = Blackbox.getContact(contactNumber)?.getContactName()
                                ?: Blackbox.getTemporaryContact(contactNumber)?.getContactName()
                                ?: contactNumber

                        message.body = "*$contact* has joined this Chat Group"
                    }
                }
            }
            "file"          -> {
                type = if (message.body.contains("alert:#screenshot")) {
                    MessageType.AlertScreenshot
                } else {
                    val filename = if (message.originFileName.isNotEmpty()) message.originFileName else message.fileName
                    Message.getFileType(filename)
                }
            }
            else -> {
                // Text Messages. They can be formatted in a psci

                if (message.body.contains(":#")) {
                    val parts = message.body.split(":#").filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) {
                        if (parts[0] == "alert") {
                            if (parts.size > 1) {
                                when (parts[1]) {
                                    "copy" -> {
                                        if (parts.size > 5) {
                                            message.alertMsgSenderRef = parts[2]
                                            message.alertMsgIdRef = parts[3]
                                            getMessageTypeFromAlertString(parts[4])?.let {
                                                message.alertMsgTypeRef = it
                                                message.alertMsgContentRef = parts[5]
                                                type = MessageType.AlertCopy

                                                message.body = if (message.isOutgoing) {
                                                    "_*You* copied this message_"
                                                } else {
                                                    val contact = Blackbox.getContact(message.sender)?.getContactName()
                                                            ?: Blackbox.getTemporaryContact(message.sender)?.getContactName()
                                                            ?: message.sender
                                                    "_*$contact* copied this message_"
                                                }

                                            }
                                        }
                                    }
                                    "forward" -> {
                                        if (parts.size > 5) {
                                            message.alertMsgSenderRef = parts[2]
                                            message.alertMsgIdRef = parts[3]
                                            getMessageTypeFromAlertString(parts[4])?.let {
                                                message.alertMsgTypeRef = it
                                                message.alertMsgContentRef = parts[5]
                                                type = MessageType.AlertForward

                                                message.body = if (message.isOutgoing) {
                                                    "_*You* forwarded this message_"
                                                } else {
                                                    val contact = Blackbox.getContact(message.sender)?.getContactName()
                                                            ?: Blackbox.getTemporaryContact(message.sender)?.getContactName()
                                                            ?: message.sender
                                                    "_*$contact* forwarded this message_"
                                                }
                                            }
                                        }
                                    }
                                    "delete" -> {
                                        if (parts.size > 5) {
                                            message.alertMsgSenderRef = parts[2]
                                            message.alertMsgIdRef = parts[3]
                                            getMessageTypeFromAlertString(parts[4])?.let {
                                                message.alertMsgTypeRef = it
                                                message.alertMsgContentRef = parts[5]
                                                type = MessageType.AlertDelete
                                            }

                                            message.body = if (message.isOutgoing) {
                                                "_*You* delete this message_"
                                            } else {
                                                val contact = Blackbox.getContact(message.sender)?.getContactName()
                                                        ?: Blackbox.getTemporaryContact(message.sender)?.getContactName()
                                                        ?: message.sender
                                                "_*$contact* delete this message_"
                                            }
                                        }
                                    }
                                    "screenrecording" -> type = MessageType.AlertScreenRecording
                                    "screenshot" -> type = MessageType.AlertScreenshot
                                }
                            }
                        }
                    }
                }
            }
        }

        message.type = type ?: MessageType.Text
    }

    fun getMessageTypeFromAlertString(string: String) : MessageType? {
        return when (string) {
            "audio" -> MessageType.Audio
            "text" -> MessageType.Text
            "photo" -> MessageType.Photo
            "video" -> MessageType.Video
            "location" -> MessageType.Location
            "document" -> MessageType.DocumentGeneric
            "contact" -> MessageType.Contact
            else -> {
                return null
            }
        }
    }
}

@Parcelize
data class Message(var ID: String = "",
                   var sender: String = "",
                   var recipient: String = "",
                   var msgtype: String = "txt",
                   var type: MessageType = MessageType.Text,
                   var body: String = "",
                   var contactPhoto: String? = null,
                   var groupPhoto: String? = null,
                   var repliedToMsgId: String = "",
                   var repliedToText: String = "",
                   var groupID: String = "",
                   var groupDescritpion: String? = null,
                   var groupDateExpiry: Date? = null,
                   var dateSent: Date? = null,
                   var isArchived: Boolean = false,
                   var isForwarded: Boolean = false,
                   var audioTimer: Int = 0,
                   var contactID: String = "",
                   var answer: String = "KO",
                   var queue: Int = 0,
                   private var a: String = "",
                   var isInNetworkProgress: Boolean = false): Parcelable {

    var isPlayingAudio: Boolean = false


    // Common Props
    val dateSentString: String? get() {
        dateSent?.let {
            return dateSent!!.dateString(DateStyle.default)
        }
        return null
    }

    var dateReceived: Date? = null
        set (value) {
            field = value
            _checkmarkType.value?.let {
                if (it != CheckmarkType.read) {
                    _checkmarkType.postValue(CheckmarkType.received)
                }
            }
        }
    val dateReceivedString: String? get() {
        dateReceived?.let {
            return dateSent!!.dateString(DateStyle.default)
        }
        return null
    }

    var dateRead: Date? = null
        set (value) {
            field = value
            _checkmarkType.postValue(if (body.isBlank() && msgtype == "txt") CheckmarkType.none else CheckmarkType.read)
        }
    val dateReadString: String? get() {
        dateRead?.let {
            return dateSent!!.dateString(DateStyle.default)
        }
        return null
    }

    var dateDeleted: Date? = null
    val dateDeletedString: String? get() {
        dateDeleted?.let {
            return dateSent!!.dateString(DateStyle.default)
        }
        return null
    }

    var autoDeleteString: String? = null

    private var _isAutoDelete = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val isAutoDelete: LiveData<Boolean> get() = _isAutoDelete
    fun setAutoDelete(value: Boolean) {
        _isAutoDelete.postValue(value)
    }

//    var isAutoDelete: Boolean = false

    var fileName: String = ""
    // original file path used for upload
    var originalFilePath: String? = null
    var fileSize: Long = 0
    var originFileName: String = "" // Only the name+extension. Not the complete path
    var oldestUnreadMessageID: String? = null
    var chatUnreadMessagesCount: Int = 0
    var totUnreadMsgs: Int = 0

    // Alert messages
    /* The Copied/Forwarded Message sender */
    var alertMsgSenderRef: String? = null
    /* The Copied/Forwarded Message ID */
    var alertMsgIdRef: String? = null
    /* The Copied/Forwarded Message type */
    var alertMsgTypeRef: MessageType? = null
    /* The Copied/Forwarded Message content (the body) */
    var alertMsgContentRef: String? = null
    var alertMsg: String? = null


    val _localFileName = MutableLiveData<String>().apply {
        postValue("")
    }
    val localFileName: LiveData<String> get() = _localFileName


    /**
     * Observable property used to update the message file transfer percentage
     */
    val fileTransferState: LiveData<Int> get() = _fileTransferState
    val _fileTransferState = MutableLiveData<Int>().apply {
        postValue(0)
    }

    /**
     * Observable property used to update the message receipt icon
     */
    val checkmarkType: LiveData<CheckmarkType> get() = _checkmarkType
    private val _checkmarkType = MutableLiveData<CheckmarkType>().apply {
        postValue(CheckmarkType.none)
    }

    /**
     * Observable property used to update the message receipt icon
     */
    val autoDownload: LiveData<Boolean> get() = _autoDownload
    val _autoDownload: MutableLiveData<Boolean> by lazy {
        MutableLiveData(false)
    }

    /**
     * Observable property used to update the message Starred icon.
     */
    val isStarred: LiveData<Boolean> get() = _isStarred
    val _isStarred = MutableLiveData<Boolean>(false)


    var deliveredToServer: Boolean = false
        set(value) {
            field = value
            _checkmarkType.postValue(CheckmarkType.sent)
        }

    val isGroupChat: Boolean get() = groupID.isNotEmpty()

    val isOutgoing: Boolean get() {
        if(Blackbox.account.registeredNumber == sender){
            return true
        }
        return false
    }

    val isAlertMessage: Boolean get() {
        return type.isAlertMessage
    }

    val buddyNumberOrGroupId: String get() {
        if(groupID.isNotBlank())
            return groupID
        return if(isOutgoing) recipient else sender
    }

    val isSuccess: Boolean get() = answer == "OK"


    // MARK: - Metadata
    var removedFromGroupContactNumber: String? = null

    fun setCheckmarkType(type: CheckmarkType) {
        _checkmarkType.postValue(type)
    }

    companion object {
        fun getFileType(fileName: String) : MessageType {
            val extension = File(fileName).extension

            if (extension.contains("png", true) ||
                    extension.contains("jpg", true) ||
                    extension.contains("jpeg", true) ||
                    extension.contains("gif", true) ||
                    extension.contains("svg", true)) {
                return MessageType.Photo
            }

            if (extension.contains("mov", true) ||
                    extension.contains("mp4", true) ||
                    extension.contains("avi", true) ||
                    extension.contains("wmv", true) ||
                    extension.contains("3gp", true)) {
                return MessageType.Video
            }

            if (extension.contains("m4a", true) ||
                    extension.contains("mp3", true) ||
                    extension.contains("wav", true) ||
                    extension.contains("wma", true) ||
                    extension.contains("aac", true)) {
                return MessageType.Audio
            }

            if (extension.contains("pdf", true)) {
                return MessageType.DocumentPDF
            }

            if (extension.contains("txt", true) ||
                    extension.contains("rtf", true)) {
                return MessageType.DocumentText
            }

            if (extension.contains("doc", true) ||
                    extension.contains("docx", true) ||
                    extension.contains("odt", true)) {
                return MessageType.DocumentMicrosoftWord
            }

            if (extension.contains("xls", true) ||
                    extension.contains("xlsb", true) ||
                    extension.contains("xlsm", true) ||
                    extension.contains("xlsx", true)) {
                return MessageType.DocumentMicrosoftExcel
            }

            if (extension.contains("pptx", true) ||
                    extension.contains("pptm", true) ||
                    extension.contains("ppt", true)) {
                return MessageType.DocumentMicrosoftPowerPoint
            }

            if (extension.contains("pages", true)) {
                return MessageType.DocumentApplePages
            }

            if (extension.contains("numbers", true)) {
                return MessageType.DocumentAppleNumbers
            }

            if (extension.contains("key", true)) {
                return MessageType.DocumentAppleKeynote
            }

            return  MessageType.Text
        }



    }

}

val File.extension: String get() = name.substringAfterLast('.', "")