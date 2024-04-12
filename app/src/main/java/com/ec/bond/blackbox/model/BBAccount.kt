package com.ec.bond.blackbox.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ec.bond.BondApp
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.getStringOrNull
import com.ec.bond.blackbox.model.api_responses.GeneralResponse
import com.ec.bond.utils.Constant
import com.ec.bond.utils.Event
import com.ec.bond.utils.SharePreferenceUtility
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

enum class BBStatus {
    online, offline
}

class BBAccount {
    private val accountScope = CoroutineScope(Job() + Dispatchers.IO)

    private var _registeredNumber: String? = null
    val registeredNumber: String?
        get() {
            if (_registeredNumber != null)
                return _registeredNumber

            if (Blackbox.pwdConf != null) {
                val jsonString = bb_get_registered_mobilenumber(Blackbox.pwdConf!!)
                val jsonObject = JSONObject(jsonString)
                if (jsonObject.has("answer")) {
                    if (jsonObject.getString("answer") == "OK") {
                        if (jsonObject.has("mobilenumber")) {
                            _registeredNumber = jsonObject.getString("mobilenumber")
                            return _registeredNumber
                        }
                    }
                }
            }
            return null
        }
    val isValid: Boolean get() = registeredNumber != null

    private val _name = MutableLiveData<String>()
    val name: LiveData<String> get() = _name

    private var _photoProfilePath = MutableLiveData("")
    val photoProfilePath: LiveData<String> get() = _photoProfilePath

    private val _statusMessage = MutableLiveData("")
    val statusMessage: LiveData<String> get() = _statusMessage

    private val _incomingCallPublisher = MutableLiveData<Event<BBCall>>()
    val incomingCallPublisher: LiveData<Event<BBCall>> get() = _incomingCallPublisher

    private val _incomingMessagesPublisher = MutableLiveData<Event<Message>>()
    val incomingMessagesPublisher: LiveData<Event<Message>> get() = _incomingMessagesPublisher

    var lastSeen: Date? = null
    var needUpdate: Boolean = false
    var appUpdateUrl: String? = null
    var currentAvailableVersion: String? = null
    var onlineVisibility: Boolean = false
    private val _settings = MutableLiveData<BBAccountSettings>()
    val settings: LiveData<BBAccountSettings> get() = _settings

    // Archived Chat items (Archived chats list page)
    private val _state = MutableLiveData(BBAccountRegistrationState.Offline)
    val state: LiveData<BBAccountRegistrationState> get() = _state

    suspend fun registerInternalPush() = withContext(Dispatchers.IO) {
        registeredNumber?.let { regNumber ->
            bb_register_internal_push(regNumber)
        }
    }

    suspend fun unregisterInternalPush() = withContext(Dispatchers.IO) {
        bb_unregister_internal_push()
    }

    fun internalPushCallBack(pushType: Int) {
        Log.e("binh", "internalPushCallBack pushType: $pushType")
        when (pushType) {
            0 -> {
                // new message
                getNewMessages()
            }
            1 -> {
                // incoming Voice call
                processIncomingCall()
            }
            2 -> {
                // incoming Video call
                processIncomingCall(true)
            }
        }
    }

    private fun processIncomingCall(hasVideo: Boolean = false) =
        accountScope.launch(Dispatchers.IO) {
            Log.e("binh", "incoming_call--" + Blackbox.currentCall)
            if (Blackbox.currentCall == null) {
                val call = BBCall(false, hasVideo)
                if (call.getCallInfo()) {
                    if (Blackbox.fetchContactsAsync())
                        Blackbox.fetchChatListAsync()
                    _incomingCallPublisher.postValue(Event(call))

                }
            } else {
                /*val call = BBCall(false, hasVideo)
                if (call.getCallInfo()) {
                    call.endCall()
                }*/
            }

        }

    private var isProcessingMessages: Boolean = false
    private fun getNewMessages() = GlobalScope.launch(Dispatchers.IO) {
        if (isProcessingMessages) return@launch
        while (processSingleMessageInternalPush() > 0) {
            delay(250)
        }
        isProcessingMessages = false
    }

    private fun getNewMessagesAgain() {
        /* response
            {"answer":"OK","message":"Request accepted","token":"1ebb38e13880552fc02898ce5c33525aae4cad62b1b5b5e6603bda198078aef91a507736d0c7b2c59102c7c4ec062ff43ca984c3d19c04cfec9e37f45f4834cd",
            "msgid":"353913","sender":"9660006004","recipient":"9660005011","msgtype":"received","msgref":"","msgbody":"353909",
            "repliedto":"0","repliedtotxt":"","groupid":"","groupchatid":"","dtsent":"2020-08-16 17:55:16","forwarded":"0",
            "dtdeleted":"0000-00-00 00:00:00","autodelete":"0","filename":"","uidrecipient":"","filesize":"0","queuemsgs":"2",
            "unreadmsg":"5","totunreadmsg":"16","autodownloadphotos":"Y","autodownloadvideos":"Y","autodownloadaudios":"Y",
            "autodownloaddocuments":"Y"}
         */
        getNewMessages()
    }

    suspend fun registerAsync(token: String): Boolean = withContext(Dispatchers.IO) {
        Blackbox.pwdConf?.let {
            Log.w("FirebaseInstanceId", token)
            // val token = "eJWbaoYsQAS7zfsHZ-LFat:APA91bG_ZbMqp9Sug6GWC0_dwTPUAajuXEdP1DR81hDMJXhzW_arFxiPAN87Q1YRGdX3qWIPmGKOyWp-MI9zKdaGfEUKOQyN2RoWJ6QQODBqqJZNVeiOir7oHdQmGuY2J7JOYfcO4jhf";
            Log.e("bb_register_presence", "start")
            val jsonString = bb_register_presence(it, "android", token)
            Log.e("output_of_register", jsonString)
            val jsonObject = JSONObject(jsonString)

            if (jsonObject.has("answer")) {
                if (jsonObject.getString("answer") == "OK") {
                    _state.postValue(BBAccountRegistrationState.Registered)

                    GlobalScope.launch(Dispatchers.IO) {
                        registerInternalPush()
                    }

                    return@withContext true
                } else {
                    Log.e("error", jsonObject.getString("message"))
                    _state.postValue(BBAccountRegistrationState.Offline)
                    return@withContext false
                }
            } else {
                _state.postValue(BBAccountRegistrationState.Offline)
                Log.e("error", jsonString)
                return@withContext false
            }
        }
        false
    }

    suspend fun fetchAccountInfoAsync(): Boolean = withContext(Dispatchers.Main) {
        if (registeredNumber != null) {
            val jsonString =
                Blackbox.fetchProfileInfoJson(registeredNumber!!) ?: return@withContext false
            val jsonObject = JSONObject(jsonString)
            if (jsonObject.getString("answer").equals("OK")) {

                jsonObject.getStringOrNull("name").let {
                    _name.postValue(it)
                }
                jsonObject.getStringOrNull("status").let {
                    _statusMessage.postValue(it)
                }
                jsonObject.getStringOrNull("photoname").let {
                    _photoProfilePath.postValue(it)
                }

                lastSeen = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).parse(jsonObject.getString("lastseen"))
                appUpdateUrl = jsonObject.getStringOrNull("updateurlandroid")
                currentAvailableVersion = jsonObject.getStringOrNull("currentappversion")
                jsonObject.getStringOrNull("forceupdate").let {
                    if (it != null) {
                        needUpdate = it == "Y"
                    }
                }
                jsonObject.getStringOrNull("onlinevisibility").let {
                    if (it != null) {
                        onlineVisibility = it == "Y"
                    }
                }

                true
            } else false
        } else false
    }

    /**
     * Fetch the account Settings and update the observable property *settings*
     *
     * @return true if success
     */
    suspend fun fetchAccountConfigAsync(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val json = bb_get_configuration(pwdConf)
        val jsonObject = JSONObject(json)
        jsonObject.getStringOrNull("answer")?.let {
            if (it == "OK") {
                _settings.postValue(
                    BBAccountSettings(
                        jsonObject.getString("calendar"),
                        jsonObject.getString("language"),
                        jsonObject.getString("onlinevisibility"),
                        jsonObject.getString("autodownloadphotos"),
                        jsonObject.getString("autodownloadaudio"),
                        jsonObject.getString("autodownloadvideos"),
                        jsonObject.getString("autodownloaddocuments"),
                        jsonObject.getString("maximumfilesizemb"),
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                    )
                )
                return@withContext true
            }
        }
        false
    }

    /**
     * Update account settings with provided settings
     *
     * @return true if success
     */
    suspend fun updateAccountSettings(config: BBAccountSettings): Boolean =
        withContext(Dispatchers.IO) {
            val pwdConf = Blackbox.pwdConf ?: return@withContext false
            val list = bb_set_configuration(
                pwdConf,
                config.calendar,
                config.language,
                config.onlineVisibility,
                config.autoDownloadPhotos,
                config.autoDownloadAudio,
                config.autoDownloadVideos,
                config.autoDownloadDocuments
            )

            val jsonObject = JSONObject(list)
            if (jsonObject.has("answer")) {
                if (jsonObject.getString("answer") == "OK") {
                    _settings.postValue(config)
                    true
                } else false
            } else false
        }

    /**
     * API call to Set Online status to online or offline
     *
     * @return true if success
     */
    suspend fun setOnlineStatus(status: BBStatus): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val statusString = if (status == BBStatus.offline) "offline" else "online"
        val jsonString = bb_set_onoffline(pwdConf, statusString)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        return@withContext response.isSuccess
    }

    /**
     * API call to update the user account Status
     *
     * @return true if success
     */
    suspend fun setPhotoProfile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        if (File(filePath).exists() == false) {
            Log.e("setPhotoProfile", "File does not exist")
            return@withContext false
        }

        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_update_photo_profile(filePath, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        Log.e("setPhotoProfile", "" + response.isSuccess)
        if (response.isSuccess) {
            _photoProfilePath.postValue(response.localFilename)
        }
        response.isSuccess
    }

    /**
     * This function will be called when a new message (of an type, from a simple text message to Read/Received receipts notifications) is received.
     */
    private suspend fun processSingleMessageInternalPush(): Int = withContext(Dispatchers.IO) {
        isProcessingMessages = true
        val message = Blackbox.fetchSingleMessageInternalPushAsync() ?: return@withContext 0
        Log.d("processSingleMessageInternalPush", message.toString())

        // TODO - Process every new message in here.
        if (message.isSuccess) {
            // Success.
            // At this point the message can be for a contact, for a group or simply a system message.
            var group: BBGroup? = null
            var contact: BBContact? = null
            if (message.isGroupChat) {
                group = Blackbox.getGroupFromMessage(message)
            } else {
                contact = Blackbox.getContactFromMessage(message)
            }
            if (group != null && message.sender == message.recipient) {
                return@withContext message.queue
            }

            when (message.type) {
                MessageType.Status -> {
                    Log.i("Status", "contact ID1 = ${message.body} and status = ${message.body}")
                    if (message.body == "online") {
                        contact?.updateStatus(BBStatus.online)
                    } else {
                        contact?.updateStatus(BBStatus.offline)
                    }
//                        contact?._onlineStatus.postValue(MessageType.Status)
                }
                MessageType.Typing -> {
                    if (group != null) {
                        val typingContact =
                            Blackbox.getContact(message.sender) ?: Blackbox.getTemporaryContact(
                                message.sender
                            )
                        val name = if (typingContact?.name!!.isNotEmpty()) {
                            typingContact.name
                        } else {
                            message.sender
                        }
                        group.typingMessage.postValue("$name typing...")
                    } else {
                        contact?.typingMessage?.postValue("typing...")
                        async { contact?.refreshInfo() }
                    }
                }
                MessageType.Deleted -> {
                    contact?.deleteMessage(message)
                    group?.deleteMessage(message)
                }
                MessageType.Received -> {
                    contact?.updateMessageCheckmarkType(message, false)
                    group?.updateMessageCheckmarkType(message, false)
                }
                MessageType.Read -> {
                    contact?.updateMessageCheckmarkType(message, true)
                    group?.updateMessageCheckmarkType(message, true)
                }
                else -> {
                    if (message.type.isSystemMessage && message.body.contains(
                            "You have been removed from Chat Group",
                            true
                        )
                    ) {
                        val chatItems = Blackbox.chatItems.value ?: arrayListOf()
                        val archiveChats = Blackbox.archivedChatItems.value ?: arrayListOf()
                        chatItems.removeAll { it.isGroup && it.group!!.ID == message.groupID }
                        archiveChats.removeAll { it.isGroup && it.group!!.ID == message.groupID }

                        Blackbox.setChatItems(chatItems)
                        Blackbox.setArchivedChatItems(archiveChats)

                    } else if (message.type.isSystemMessage && message.body.contains(
                            "You have been added to chat group",
                            true
                        )
                    ) {
                        val newGroup = BBGroup(
                            message.groupID,
                            message.body.replace("You have been added to chat group: ", "", true),
                            BBGroupRole.Normal
                        )
                        async { newGroup.refreshMembersListAsync() }
                        newGroup.addInGeneralMessageList(message)
                        newGroup.getNewMessages(message)

                        // TODO: Fetch Photo profile
                        message.groupPhoto?.let { fileName ->
                            String
                            async {
                                newGroup.fetchProfileImageAsync(fileName)
                            }
                        }

                        newGroup.unreadMessagesCount = message.chatUnreadMessagesCount
                        newGroup.oldestUnreadMsgID = message.oldestUnreadMessageID
                        message.groupDateExpiry?.let { date ->
                            newGroup.expiryDate = date
                        }

                        val chatItems = Blackbox.chatItems.value ?: arrayListOf()
                        chatItems.add(ChatItem(null, newGroup, message))
                        Blackbox.setChatItems(chatItems)

                    } else {
                        // We process these 2 statement here because these 2 kind of message must be added to the messages list
                        if (message.type.isSystemMessage && message.body.contains(
                                "has left this Chat Group",
                                true
                            )
                        ) {
                            group?.refreshMembersListAsync()
                        } else if (message.type.isSystemMessage && message.body.contains(
                                "The group's name is changed to:",
                                true
                            )
                        ) {
                            group?.updateGroupDescription(
                                message.body.replace(
                                    "The group's name is changed to: ",
                                    ""
                                )
                            )
                        }

                        if (message.type == MessageType.SystemAutoDelete) {
                            contact?.fetchAutoDeleteTimer()
                            group?.fetchAutoDeleteTimer()
                        }

                        contact?.addInGeneralMessageList(message)
                        contact?.getNewMessages(message)
                        group?.addInGeneralMessageList(message)
                        group?.getNewMessages(message)


                        // Update the blackbox chat items list
                        contact?.let { Blackbox.updateChatItems(it, message) }
                        group?.let { Blackbox.updateChatItems(it, message) }

                        accountScope.launch(Dispatchers.IO) {
                            var background = SharePreferenceUtility.getPreferences(
                                BondApp.applicationContext(),
                                Constant.IS_APP_IN_BACKGROUND,
                                SharePreferenceUtility.PREFTYPE_BOOLEAN
                            ) as Boolean
                            if (contact != null) {

                                Log.d("processSingleMessageInternalPush", "" + contact)
                                _incomingMessagesPublisher.postValue(Event(message))
                            } else if (group != null) {
                                _incomingMessagesPublisher.postValue(Event(message))
                            }
                        }

                    }
                }
            }
        }

        message.queue
    }

    suspend fun fetchNewMessages() = withContext(Dispatchers.IO) {
        Blackbox.pwdConf?.let { pwdConf ->

            val jsonString = bb_get_newmsg(pwdConf)
            val gson = Gson()
            val response = gson.fromJson(jsonString, GeneralResponse::class.java)

            if (response.isSuccess) {
//                messagesSection.clear()
                var jsonObject = JSONObject(jsonString)
                var queue = jsonObject.getString("queuemsgs").toInt()
                val gsonBuilder = GsonBuilder()
                gsonBuilder.registerTypeAdapter(Message::class.java, MessageJsonDeserializer())
                val gson = gsonBuilder.create()
                val type: Type = object : TypeToken<Message?>() {}.type
                val message = gson.fromJson<Message>(jsonObject.toString(), type)
                val bbChat: BBChat
                if (message.groupID.isEmpty()) {
                    bbChat = Blackbox.getContact(message.sender) ?: Blackbox.getTemporaryContact(
                        message.sender
                    ) ?: BBChat()
                } else {
                    bbChat = BBChat()
                }




                print(message)
                when {
                    jsonObject.getString("msgtype").equals("status") -> {

                    }
                    jsonObject.getString("msgtype").equals("deleted") -> {
                        bbChat.deleteMessage(message)
                    }
                    jsonObject.getString("msgtype").equals("received") -> {
                        bbChat.updateMessageCheckmarkType(message, false)
//                        ChatBrowsingMessages.allMessages.filter {  }
                    }
                    jsonObject.getString("msgtype").equals("read") -> {
                        bbChat.updateMessageCheckmarkType(message, true)
                    }
                    else -> {
                        bbChat.addInGeneralMessageList(message)
//                        ChatBrowsingMessages.addInGeneralMessageList(message,message.sender)
//                        getNewMessages(message.sender)
                        bbChat.getNewMessages(message)
//                        var message = Message()
//                        ChatBrowsingMessages.allMessages.filter { it.key.contact }
                    }
                }
                return@withContext queue
            } else {
                Log.e("clearChat", response.message)
            }

        }
        -1
    }

    external fun bb_register_presence(pwdconf: String, os: String, uniqueid: String): String
    external fun bb_get_registered_mobilenumber(pwdconf: String): String
    external fun bb_get_configuration(pwdconf: String): String
    external fun bb_set_configuration(
        pwdconf: String,
        calendar: String,
        language: String,
        onlinevisibility: String,
        autodownloadphotos: String,
        autodownloadaudio: String,
        autodownloadvideos: String,
        autodownloaddocuments: String
    ): String

    external fun bb_set_onoffline(pwdconf: String, status: String): String
    external fun bb_update_photo_profile(path: String, pwdconf: String): String
    external fun bb_get_newmsg(pwconf: String): String
    private external fun bb_register_internal_push(registeredNumber: String)
    private external fun bb_unregister_internal_push()


}