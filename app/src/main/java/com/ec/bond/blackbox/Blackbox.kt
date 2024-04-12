package com.ec.bond.blackbox

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.ec.bond.activity.VoiceCallActivity
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingListItem
import com.ec.bond.activity.ui.chatbrowsing.DateItem
import com.ec.bond.activity.ui.chatbrowsing.DatePosition
import com.ec.bond.activity.ui.chatbrowsing.MessageItem
import com.ec.bond.blackbox.model.*
import com.ec.bond.blackbox.model.api_responses.CreateGroupChatResponse
import com.ec.bond.blackbox.model.api_responses.FetchNotificationSoundResponse
import com.ec.bond.blackbox.model.api_responses.GeneralResponse
import com.ec.bond.blackbox.model.callsHistory.BBCallHistory
import com.ec.bond.blackbox.model.callsHistory.BBCallHistoryGroup
import com.ec.bond.model.StarredMessageModel
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.CommonUtils.getTimeString
import com.ec.bond.utils.TestClass
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


object Blackbox {
    var datePositionList = ArrayList<DatePosition>()
    val account: BBAccount = BBAccount()
    var pwdConf: String?=null
    var currentCall: BBCall? = null
    var starredMessages = mutableListOf<com.ec.bond.model.Message>()
    var messages = ArrayList<ChatBrowsingListItem>()
    /**
     * The Following objects are defined as LiveData so you can observe them from everywhere.
     * For example:
     * If you fetch the Contacts List from a background thread and update the contactsSections you can easily observe the changes and update the Contacts List UI accordingly.
     */
    // Contacts
    private val _contacts = MutableLiveData<MutableMap<String, BBContact>>().apply {
        postValue(hashMapOf())
    }
    val contacts: LiveData<MutableMap<String, BBContact>> get() = _contacts
    val temporaryContacts = mutableMapOf<String, BBContact>()

    // Chat Items (Chats list page)
    private val _chatItems = MutableLiveData<ArrayList<ChatItem>>().apply {
        postValue(arrayListOf())
    }
    val chatItems: LiveData<ArrayList<ChatItem>> get() = _chatItems

    // Archived Chat items (Archived chats list page)
    private val _archivedChatItems = MutableLiveData<ArrayList<ChatItem>>().apply {
        postValue(arrayListOf())
    }
    val archivedChatItems: LiveData<ArrayList<ChatItem>> get() = _archivedChatItems



    private val _starredChatItems = MutableLiveData<MutableList<ChatBrowsingListItem>>().apply {
        postValue(arrayListOf())
    }
    val starredChatItems: LiveData<MutableList<ChatBrowsingListItem>> get() = _starredChatItems

    // Calls history
    private val _callsHistory = MutableLiveData<ArrayList<BBCallHistoryGroup>>().apply {
        postValue(arrayListOf())
    }
    val callsHistory: LiveData<ArrayList<BBCallHistoryGroup>> get() = _callsHistory

    fun getDocumentsDir(context: Context) : String = context.filesDir.path + "/Documents"

    // region Blackbox API CAlls

    suspend fun decrypt_pwdconf(context: Context) : Boolean = withContext(Dispatchers.Main)  {
        val folderPath = getDocumentsDir(context)
        val filePath = folderPath + "/pwdconf.enc"

        val data: ByteArray = CommonUtils.getByte(filePath)
        val pwdconfg: String = bb_decrypt_pwdconf(data, data.size, "", data.count() - 64, folderPath)

        Log.w("pwdconfg", pwdconfg)

        if (pwdconfg.isEmpty()) {
            false
        } else {
            pwdConf = pwdconfg
            true
        }
    }

    suspend fun decrypt_pwdconf(context: Context,key:String) : Boolean = withContext(Dispatchers.Main)  {
        val folderPath = getDocumentsDir(context)
        val filePath = folderPath + "/pwdconf.enc"
        val data: ByteArray = CommonUtils.getByte(filePath)

        try {

            val pwdconfg: String = bb_decrypt_pwdconf(data, data.size, key, data.count() - 64, folderPath
            )
            Log.w("pwdconfgiiii",""+ (pwdconfg))

            if (pwdconfg.isEmpty()) {
                false
            } else {
                pwdConf = pwdconfg
                true
            }
        }catch (e: Exception){
            false
        }



    }


    suspend fun fetchStarredMessages(recipient1:String) : List<ChatBrowsingListItem> = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext listOf()
        val recipient =  recipient1
        val groupId =  ""
        try {

            val jsonString = bb_get_starred_messages(recipient, groupId, pwdConf)
            val jsonObject = JSONObject(jsonString)
            val answer = jsonObject.getStringOrNull("answer") ?: return@withContext arrayListOf()
            Log.e("starreed",""+recipient)
            if (answer == "KO") {
                if (jsonObject.getStringOrNull("message")!!.contains("No messages has been found")) {

                }
            }
            val messagesString = jsonObject.getStringOrNull("messages")
                ?: return@withContext arrayListOf()
            if (answer == "OK") {
                // add next two lines --> Hazem
                val gsonBuilder = GsonBuilder()
                gsonBuilder.registerTypeAdapter(Message::class.java, MessageJsonDeserializer())
                val gson = gsonBuilder.create()
                val type: Type = object : TypeToken<List<Message>>() {}.type
                val messages = gson.fromJson<ArrayList<Message>>(messagesString, type)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val calendar = GregorianCalendar.getInstance()

                val groupedMessages = messages.reversed().groupBy {
                    val date = dateFormat.parse(it.dateSentString!!)
                    calendar.time = date!!
                    return@groupBy calendar.get(Calendar.DAY_OF_MONTH)
                }

                groupedMessages.toList().forEach {
                    val date = it.second.first().dateSent ?: return@forEach
                    if (this@Blackbox.messages.size > 0) {
                        if ((this@Blackbox.messages[this@Blackbox.messages.lastIndex] as DateItem).date.getTimeString() == it.second.first().dateSent?.getTimeString()) {
                            this@Blackbox.messages.removeAt(this@Blackbox.messages.lastIndex)
                        }
                    }
                    this@Blackbox.messages.addAll(it.second.map { MessageItem(it) })
                    this@Blackbox.messages.add(DateItem(date))
                }
                Log.i("getInitialMessages","fetchMessages() chatId = $recipient or group = $groupId and messages No = ${messages.size}")

                fillDatePositions()
                _starredChatItems.postValue(this@Blackbox.messages)
                Log.i("getInitialMessages","fetchMessages() chatId = $_starredChatItems")

                return@withContext this@Blackbox.messages.toList()
            }
            listOf()
        } catch (ex: Exception) {
            Log.i("getInitialMessages","fetchMessages() chatId = $recipient or group = $groupId and messages No = ${messages.size}")

            if (messages.size == 0) {

            }
            Log.e("fetchMessages()", ex.message.toString())
            listOf()
        }
    }

    private fun fillDatePositions() {
        try {
            datePositionList.clear()
            var lastIndex = 0
            messages.forEachIndexed { index, chatBrowsingListItem ->
                if (chatBrowsingListItem.getType() == ChatBrowsingListItem.TYPE_DATE) {
                    val dateItem = (chatBrowsingListItem as DateItem)
                    if (datePositionList.size > 0) {
                        datePositionList.add(DatePosition(dateItem.date.getTimeString(), lastIndex + 1, dateItem.date, index - lastIndex - 1))
                    } else {
                        datePositionList.add(DatePosition(dateItem.date.getTimeString(), 0, dateItem.date, index))
                    }
//                }
                    lastIndex = index
                }
            }
        }
        catch (ex: Exception) {
            Log.e("fillDatePositions()", ex.message.toString())
        }

    }

    /**
     * Fetch Contacts and update the *contacts* Observable object
     */
    suspend fun fetchContactsAsync() : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = pwdConf ?: return@withContext false
        val jsonString = bb_get_contacts("",
                0,
                0,
                100000,
                pwdConf)
        Log.d("bb_get_contacts", jsonString)
        val jsonObject = JSONObject(jsonString)

        when {
            jsonObject.has("contacts") -> {
                val gson = Gson()
                val type: Type = object : TypeToken<List<BBContact>>() {}.type
                val contacts = gson.fromJson<ArrayList<BBContact>>(jsonObject.getString("contacts"), type)
                contacts.sortBy { contact -> contact.name }

                // Set the contact register number
                contacts.forEach { contact ->
                    contact.phonesjson.firstOrNull()?.let {
                        contact.registeredNumber = it.phone
                    }
                    contact.phonejsonreg.firstOrNull()?.let {
                        contact.registeredNumber = it.phone
                    }
                }

                val newContacts = mutableMapOf<String, BBContact>()
                contacts.associateByTo(newContacts, { contact -> contact.registeredNumber}, {it})
                if (newContacts != _contacts.value) {
                    _contacts.postValue(newContacts)

                    GlobalScope.launch(Dispatchers.IO) {
                        for (contact in newContacts) {
                            async { contact.value.fetchProfileImageAsync() }
                            async { contact.value.refreshInfo() }
                        }
                    }
                }

                return@withContext true

            }
            (jsonObject.get("message") as CharSequence).contains("No contacts found") -> return@withContext true
            else -> return@withContext false
        }

    }

    /**
     * Fetch the last 100 calls and update the *callsHistory* Observable object
     */
    suspend fun fetchCallsHistoryAsync() : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = pwdConf ?: return@withContext false
        val jsonString = bb_last_voicecalls(pwdConf)
        Log.d("fetchCallsHistoryAsync", jsonString)
        val jsonObject = JSONObject(jsonString)
        if (jsonObject.getString("answer") == "OK") {
            val response: ArrayList<BBCallHistory>
            val gson = Gson()
            val type: Type = object : TypeToken<List<BBCallHistory?>?>() {}.type
            response = gson.fromJson<ArrayList<BBCallHistory>>(jsonObject.getString("voicecalls"), type)

            val groupedCallsHistory = generateGroupedCallsHistory((response))
            _callsHistory.postValue(groupedCallsHistory)

            return@withContext true
        } else
            return@withContext false

    }


    /**
     * Fetch Chats lists and update the *chatItems* Observable object
     *
     * @return true if success
     */
    suspend fun fetchChatListAsync() : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = pwdConf ?: return@withContext false
        val chatItems = ArrayList<ChatItem>()
        val archivedChatItems = ArrayList<ChatItem>()
        val starredChatItems = ArrayList<ChatItem>()
        val jsonString = bb_get_list_chat(pwdConf)
        Log.d("chatlist", jsonString)

        val jsonObject = JSONObject(jsonString)
        val answer = jsonObject.getStringOrNull("answer")
        val message = jsonObject.getStringOrNull("message")
        if (answer != null && message != null) {
            if (answer == "OK" && jsonObject.has("chats")) {

                val gsonBuilder = GsonBuilder()
                gsonBuilder.registerTypeAdapter(Message::class.java, MessageJsonDeserializer())
                val gson = gsonBuilder.create()
                val type: Type = object : TypeToken<List<Message?>?>() {}.type
                val chats = gson.fromJson<ArrayList<Message>>(jsonObject.getString("chats"), type)

                chats.sortByDescending { msg -> msg.dateSent }

                for (msg in chats) {

                    if (msg.isGroupChat) {
                        val group = BBGroup(msg.groupID, msg.groupDescritpion ?: "", BBGroupRole.Normal)
                        async { group.refreshMembersListAsync() }

                        if (msg.isArchived) {
                            archivedChatItems.add(ChatItem(null, group, msg, true))
                        } else {
                            chatItems.add(ChatItem(null, group, msg))
                        }

                        // TODO: Fetch Photo profile
                        msg.groupPhoto?.let { fileName -> String
                            async {
                                group.fetchProfileImageAsync(fileName)
                            }
                        }

                        group.unreadMessagesCount = msg.chatUnreadMessagesCount
                        group.oldestUnreadMsgID = msg.oldestUnreadMessageID
                        msg.groupDateExpiry?.let { date ->
                            group.expiryDate = date
                        }

                    } else {
                        if (msg.sender == "0000001") {
                            continue
                        }

                        val contact = getContactFromMessage(msg)
                        if (msg.isArchived) {
                            archivedChatItems.add(ChatItem(contact, null, msg, true))
                        } else {
                            chatItems.add(ChatItem(contact, null, msg))
                        }

                        // TODO: Fetch Photo profile
                        msg.contactPhoto?.let { fileName -> String
                            async {
                                contact.fetchProfileImageAsync()
                            }
                        }
                        async { contact.refreshInfo() }

                        contact.unreadMessagesCount = msg.chatUnreadMessagesCount
                        contact.oldestUnreadMsgID = msg.oldestUnreadMessageID
                    }
                }

                _archivedChatItems.postValue(archivedChatItems)
                setChatItems(chatItems)
//                    _chatItems.postValue(chatItems)

                return@withContext true
            }

            if (message.contains("No Chats found")) {
                _chatItems.postValue(ArrayList())

                return@withContext true
            }
        }
        return@withContext false
    }

    fun setChatItems(chatItems: ArrayList<ChatItem>) {
        chatItems.sortByDescending { it.lastMessage?.dateSent }
        _chatItems.postValue(chatItems)
//        val oldChatItems = this.chatItems.value!!
//        if (oldChatItems.isEqual(chatItems) == false) {
//            _chatItems.postValue(chatItems)
//        }
    }

    fun setArchivedChatItems(chatItems: ArrayList<ChatItem>) {
        chatItems.sortByDescending { it.lastMessage?.dateSent }
        _archivedChatItems.postValue(chatItems)
//        val oldChatItems = this.archivedChatItems.value!!
//        if (oldChatItems.isEqual(chatItems) == false) {
//            _archivedChatItems.postValue(chatItems)
//        }
    }

    /**
     * Return the json string from bb_get_profileinfo
     */
    suspend fun fetchProfileInfoJson(recipient: String) : String? = withContext(Dispatchers.IO) {
        if (pwdConf != null) {
            val jsonString = bb_get_profileinfo(recipient, pwdConf!!)
            return@withContext jsonString
        }
        return@withContext null
    }

    /**
     * Api Call to add contact to rooster. Add the contact to *contacts* and notify Observer if successful.
     */
    suspend fun addContactAsync(contact: BBContact) : Boolean = withContext(Dispatchers.IO) {
        if (contact.phonesjson.count() == 0) {
            return@withContext false
        }

        contacts.value?.let {
            // Return false if the contact already exist
            if (it.containsKey(contact.phonesjson.first().phone)) return@withContext false
        }

        val pwdConf = pwdConf ?: return@withContext false
        val gson = Gson()
        val contactJson = gson.toJson(contact)

        val responseJson = bb_add_contact(contactJson, pwdConf)
        val jsonObject = JSONObject(responseJson)
        val answer = jsonObject.getStringOrNull("answer") ?: return@withContext false
        if (answer == "OK") {
            jsonObject.getStringOrNull("id")?.let {
                contact.ID = it
            }
            jsonObject.getStringOrNull("phonejsonreg")?.let {
                val type: Type = object : TypeToken<List<BBPhoneNumber?>?>() {}.type
                val phones = gson.fromJson<ArrayList<BBPhoneNumber>>(jsonObject.getString("phonejsonreg"), type)

                if (phones.count() > 0) {
                    contact.registeredNumber = phones.first().phone
                }
            }
            var contactsMap = contacts.value ?: hashMapOf()
            var contacts = contactsMap.values?.toMutableList()
            contacts.add(contact)

            val newContacts = mutableMapOf<String, BBContact>()
            contacts.associateByTo(newContacts, { contact -> contact.registeredNumber}, {it})

            _contacts.postValue(newContacts)

            return@withContext true
        }
        false
    }

    suspend fun updateContactAsync(contact: BBContact) : Boolean = withContext(Dispatchers.IO) {
       /* if (contact.phonesjson.count() == 0) {
            return@withContext false
        }*/

        /*contacts.value?.let {
            // Return false if the contact already exist
            if (it.containsKey(contact.phonesjson.first().phone)) return@withContext false
        }*/

        val pwdConf = pwdConf ?: return@withContext false
        val gson = Gson()
        val contactJson = gson.toJson(contact)

        val responseJson = bb_update_contact(contactJson, pwdConf)
        val jsonObject = JSONObject(responseJson)
        val answer = jsonObject.getStringOrNull("answer") ?: return@withContext false
        if (answer == "OK") {
            jsonObject.getStringOrNull("id")?.let {
                contact.ID = it
            }
            jsonObject.getStringOrNull("phonejsonreg")?.let {
                val type: Type = object : TypeToken<List<BBPhoneNumber?>?>() {}.type
                val phones = gson.fromJson<ArrayList<BBPhoneNumber>>(jsonObject.getString("phonejsonreg"), type)

                if (phones.count() > 0) {
                    contact.registeredNumber = phones.first().phone
                }
            }
            var contactsMap = contacts.value ?: hashMapOf()
            var contacts = contactsMap.values?.toMutableList()
            contacts.add(contact)

            val newContacts = mutableMapOf<String, BBContact>()
            contacts.associateByTo(newContacts, { contact -> contact.registeredNumber}, {it})

            _contacts.postValue(newContacts)

            return@withContext true
        }
        false
    }

    /**
     * API Call to delete a contact
     */
    suspend fun deleteContactAsync(contact: BBContact) : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = pwdConf ?: return@withContext false
        val jsonString = bb_delete_contact(contact.ID, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        return@withContext response.isSuccess
    }

    /**
     * Create a new group and add it to the Observable property *chatItems*
     *
     * @param description BBGroup Description : It represents the group subject
     * @param members BBGroup Members : Number of members included in group chat
     *
     * @return The newly created group
     */
    suspend fun createGroup(description: String, members: List<BBContact>) : BBGroup? = withContext(Dispatchers.IO) {
        pwdConf?.let {
            val jsonString = bb_new_groupchat(description, it)
            val gson = Gson()
            val response = gson.fromJson(jsonString, CreateGroupChatResponse::class.java)
            if (response.isSuccess) {
                val group = BBGroup(response.groupID, description, BBGroupRole.Creator)

                val addedMembers = group.addMembers(members)
                if (addedMembers.size != members.size) {
                    // TODO: Show error if not all members have been added.
                    Log.d("createGroupAsync", "Unable to add contact all contacts to the group")
                }

                // fake message just used to put this group at the top of the chatItems list
                val message = Message()
                message.dateSent = Date()

                // Add to the chatItems list
                val chatItems = _chatItems.value?.toMutableList() ?: mutableListOf()
                chatItems.add(ChatItem(null, group, message))

                chatItems.sortByDescending { chat -> chat.lastMessage?.dateSent }

                // post notification to observers
                setChatItems(ArrayList(chatItems))

                return@withContext group
            }
        }
        null
    }

    /**
     * Archive Chat in batches
     *
     * @return return the successfully archived chats
     */
    suspend fun archiveChats(chats: List<ChatItem>) : List<ChatItem> = withContext(Dispatchers.IO) {
        val pwdConf = pwdConf ?: return@withContext listOf()
        val chatItems = chatItems.value?.toMutableList() ?: mutableListOf()
        val archivedChats = archivedChatItems.value?.toMutableList() ?: mutableListOf()

        // First move the chats to archived
        for (chat in chats) {
            val recipient = if (chat.isGroup == false) chat.contact!!.registeredNumber else ""
            val groupId = if (chat.isGroup) chat.group!!.ID else ""
            chat.isArchived = true
            archivedChats.add(chat)
            chatItems.removeIf {
                if (it.isGroup && chat.isGroup && it.group!!.ID == groupId) {
                    return@removeIf true
                } else return@removeIf it.isGroup == false && it.contact!!.registeredNumber == recipient
            }
        }
        // Update the observable variables
        setArchivedChatItems(ArrayList(archivedChats))
        setChatItems(ArrayList(chatItems))

        // Now try to archive the chats API
        val successfullyArchivedChats = mutableListOf<ChatItem>()
        for (chat in chats) {
            val recipient = if (chat.isGroup == false) chat.contact!!.registeredNumber else ""
            val groupId = if (chat.isGroup) chat.group!!.ID else ""

            val jsonString = bb_set_archivedchat(recipient, groupId, pwdConf)
            val response = Gson().fromJson(jsonString, GeneralResponse::class.java)

            if (response.isSuccess) {
                successfullyArchivedChats.add(chat)
            }
        }

        return@withContext successfullyArchivedChats.toList()
    }

    /**
     * Unarchive Chats in batches
     *
     * @return return the successfully unArchived chats
     */
    suspend fun unArchiveChats(chats: List<ChatItem>) : List<ChatItem> = withContext(Dispatchers.IO) {
        val chatItems = chatItems.value?.toMutableList() ?: mutableListOf()
        val archivedChats = archivedChatItems.value?.toMutableList() ?: mutableListOf()

        // remove archived chats
        for (chat in chats) {
            val recipient = if (chat.isGroup == false) chat.contact!!.registeredNumber else ""
            val groupId = if (chat.isGroup) chat.group!!.ID else ""
            chat.isArchived = false
            chatItems.add(chat)
            archivedChats.removeIf {
                if (it.isGroup && chat.isGroup && it.group!!.ID == groupId) {
                    return@removeIf true
                } else return@removeIf it.isGroup == false && it.contact!!.registeredNumber == recipient
            }
        }
        // Update the observable variables
        setChatItems(ArrayList(chatItems))
        setArchivedChatItems(ArrayList(archivedChats))

        // Now try to unarchive the chats API
        val successfullyUnArchivedChats = mutableListOf<ChatItem>()
        for (chat in chats) {
            if (unarchiveChat(chat)) {
                successfullyUnArchivedChats.add(chat)
            }
        }
        return@withContext successfullyUnArchivedChats.toList()
    }

    /**
     * Unarchive a single chat
     */
    private suspend fun unarchiveChat(chat: ChatItem) : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = pwdConf ?: return@withContext false
        val recipient = if (chat.isGroup == false) chat.contact!!.registeredNumber else ""
        val groupId = if (chat.isGroup) chat.group!!.ID else ""

        val jsonString = bb_unset_archivedchat(recipient, groupId, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        return@withContext response.isSuccess
    }


    /**
     * Return the file transfer state
     *
     * Return values:
     * -1           -> File not found. In this case you should try again for max 30 seconds.
     * -100         -> Upload/Download interrupted.
     * 0 to 100     -> State progress
     * 100          -> Completed
     */
    fun updateFileTransferState(message: Message) : Int {
        var fileName = ""
        message.originalFilePath?.let {
            // Upload
            Log.e("file_bond---",""+message.originalFilePath!!)
            if (it.isNotBlank()) {
                fileName = it
            }
        }
        message.localFileName.value?.let {
            Log.e("file_bond1---",""+message.localFileName.value!!)
            // Download
            if (it.isNotBlank()) {
                fileName = it
            }
        }
        val file = File(fileName)
        if (file.exists() && file.length() == message.fileSize) {
            return 100
        }
        return bb_filetransfer_getstatus(fileName)
    }

    /**
     * Set the Contact notification sound name
     * @return return true if success
     */
    suspend fun setContactNotification(contact: BBContact, soundName: String) : Boolean = withContext(Dispatchers.IO) {
        val success = setNotification(contactNumber = contact.registeredNumber, soundName = soundName)
        if (success) {
            contact.messageNotificationSoundName = soundName
        }
        success
    }

    /**
     * Set the Group notification sound name
     * @return return true if success
     */
    suspend fun setGroupNotification(group: BBGroup, soundName: String) : Boolean = withContext(Dispatchers.IO) {
        val success = setNotification(groupid = group.ID, soundName = soundName)
        if (success) {
            group.messageNotificationSoundName = soundName
        }
        success
    }

    private fun setNotification(contactNumber: String = "", groupid: String = "", soundName: String) : Boolean {
        val pwdConf = pwdConf ?: return false
        val jsonString = bb_set_notification(contactNumber, groupid, soundName, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        return response.isSuccess
    }

    /**
     * Fetch and update the notifications sound for each contact and group
     */
    suspend fun fetchNotificationsSounds() : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = pwdConf ?: return@withContext false
        val jsonString = bb_get_notifications(pwdConf)
        val response = Gson().fromJson(jsonString, FetchNotificationSoundResponse::class.java)
        if (response.isSuccess) {
            response.notificationsSounds.forEach { notification ->
                if (notification.groupChatId.isEmpty() || notification.groupChatId == "0") {
                    // Contact notification
                    val contact = getContact(notification.contactNumber)
                            ?: getTemporaryContact(notification.contactNumber)
                            ?: return@forEach

                    contact.messageNotificationSoundName = notification.soundName
                } else {
                    // Group notification
                    chatItems.value?.forEach { chatItem ->
                        if (chatItem.isGroup && chatItem.group!!.ID == notification.groupChatId) {
                            chatItem.group!!.messageNotificationSoundName = notification.soundName
                        }
                    }
                }
            }
        }
        response.isSuccess
    }

    // endregion

    // region Utility Functions

    /**
     * Group the calls by type, direction, and recipient.
     */
    private fun generateGroupedCallsHistory(calls: ArrayList<BBCallHistory>) : ArrayList<BBCallHistoryGroup> {
        val groupedCalls: ArrayList<BBCallHistoryGroup> = ArrayList()

        // Group the call by same date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val calendar = GregorianCalendar.getInstance()
        val groupedByDate = calls.groupBy {
            val date = dateFormat.parse(it.dtsetup)
            calendar.time = date!!
            calendar.get(Calendar.DAY_OF_MONTH)
        }

        // For each day, group the calls by the recipient
        groupedByDate.forEach { sameDateGroup ->
            val groupedByNumber = sameDateGroup.value.groupBy { it.recipient }

            for (sameNumberGroup in groupedByNumber) {
                // find the contact or create a new one
                val contact = contacts.value!![sameNumberGroup.key] ?: BBContact( ID = "", registeredNumber = sameNumberGroup.key, name = "")

                // Group The Calls By the Type
                val groupedByType = sameNumberGroup.value.groupBy { it.type }

                // Group the calls by the direction
                groupedByType.forEach { sameTypeGroup ->
                    val groupedByDirection = sameTypeGroup.value.groupBy { it.directionType }
                    groupedByDirection.forEach {
                        groupedCalls.add(BBCallHistoryGroup(it.value as ArrayList<BBCallHistory>, it.key, sameTypeGroup.key, contact))
                    }
                }
            }
        }
        return groupedCalls
    }

    /**
     * Group Contacts by the name
     */
    private fun groupContactByName(contacts: ArrayList<BBContact>) : ArrayList<BBContactsSection> {
        val regContacts = contacts.filter { it.phonejsonreg.count() > 0 }
        val sections: ArrayList<BBContactsSection> = ArrayList()
        val placeholder = HashMap<String, Int>()
        regContacts.forEach {
            val initials = it.name.toLowerCase(Locale.getDefault()).substring(IntRange(0, 1))
            if (placeholder.containsKey(initials)) {
                // There is already a section with this initial, so we just append a contact to it
                for (i in 0..it.phonejsonreg.size-1) {
                    val contact = BBContact()
                    contact.registeredNumber = it.phonejsonreg.get(i).phone
                    sections.get(placeholder[initials]!!).contacts.add(contact)
                }
            } else {
                // We must create a new section for this initial
                placeholder[initials] = if (sections.count() == 0) 0 else sections.count()
                val _contacts = ArrayList<BBContact>()
                for (i in it.phonejsonreg.indices) {
                    val contact = BBContact()
                    contact.registeredNumber = it.phonejsonreg.get(i).phone
                    _contacts.add(it)
                }
                sections.add(BBContactsSection(initials, _contacts))
            }
        }
        sections.forEach {
            it.contacts.sortBy { it.name }
        }
        return sections
    }

    /**
     * Retrieve the contact from the contacts or temporaryContacts or create a new group and set his ID
     * @return BBGroup
     */
    fun getContactFromMessage(message: Message) : BBContact {
        Log.d("masmak", "getContactFromMessage: ${message.toString()}")
        getContact(message.buddyNumberOrGroupId)?.let {
            it.isSavedContact = true
            return it
        }

        getTemporaryContact(message.buddyNumberOrGroupId)?.let {
            return it
        }

        // create a temporary the contact
        val contact = BBContact()
        contact.ID = message.ID
        contact.name = message.buddyNumberOrGroupId
        contact.phonejsonreg = listOf(BBPhoneNumber("mobile", message.buddyNumberOrGroupId))
        contact.phonesjson = listOf(BBPhoneNumber("mobile", message.buddyNumberOrGroupId))
        contact.registeredNumber = message.buddyNumberOrGroupId
        contact.isSavedContact = false
        addTemporaryContact(contact)
        return contact
    }

    /**
     * Retrieve the group fromthe ChatItems or create a new group and set his ID
     * @return BBGroup
     */
    fun getGroupFromMessage(message: Message) : BBGroup {
        chatItems.value?.let {chatItems ->
            chatItems.firstOrNull { item -> item.group != null && item.group!!.ID == message.groupID }?.let {
                return it.group!!
            }
        }
        return BBGroup(message.groupID)
    }

    fun getContact(registeredNumber: String) : BBContact? {
        val contacts = contacts.value ?: return null
        return contacts[registeredNumber]
    }

    fun getTemporaryContact(registeredNumber: String) : BBContact? {
        return temporaryContacts[registeredNumber]
    }

    fun addTemporaryContact(contact: BBContact) {
        if (temporaryContacts.containsKey(contact.registeredNumber) == false) {
            temporaryContacts[contact.registeredNumber] = contact
            // TODO - Refresh Contacts
        }
    }

    fun openCallActivity(call: BBCall, context: Context,fromFirebase:Boolean=false, autoAcceptCall: Boolean = false) = GlobalScope.launch(Dispatchers.Main) {
        currentCall = call
        if(fromFirebase){
            context.startActivity(Intent(context, VoiceCallActivity::class.java).putExtra("fromFirebase",fromFirebase)
                .putExtra("ACCEPT_CALL",autoAcceptCall)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }else{
            context.startActivity(Intent(context, VoiceCallActivity::class.java).putExtra("fromFirebase",fromFirebase)
                .putExtra("ACCEPT_CALL",autoAcceptCall)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }

    }

    suspend fun updateChatItems(contact: BBContact, message: Message) = withContext(Dispatchers.Main) {
        val chatItems = chatItems.value?.toMutableList() ?: mutableListOf()
        val archivedChats = archivedChatItems.value?.toMutableList() ?: mutableListOf()
        var chatItem = chatItems.firstOrNull { it.contact != null && it.contact.registeredNumber == contact.registeredNumber }

        if (chatItem != null) {
            chatItem.lastMessage = message
        } else {
            chatItem = archivedChats.firstOrNull { it.contact != null && it.contact.registeredNumber == contact.registeredNumber }
                    ?: ChatItem(contact, null, null)
            chatItem.lastMessage = message

            if (chatItem.isArchived) {
                chatItem.isArchived = false
                archivedChats.remove(chatItem)
                setArchivedChatItems(ArrayList(archivedChats))
                async { unarchiveChat(chatItem) }
            }
            chatItems.add(chatItem)
        }
        setChatItems(ArrayList(chatItems))
    }

    suspend fun updateChatItems(group: BBGroup, message: Message) = withContext(Dispatchers.Main) {
        val chatItems = chatItems.value?.toMutableList() ?: mutableListOf()
        val archivedChats = archivedChatItems.value?.toMutableList() ?: mutableListOf()
        var chatItem = chatItems.firstOrNull { it.group != null && it.group!!.ID == group.ID }

        if (chatItem != null) {
            chatItem.lastMessage = message
        } else {
            chatItem = archivedChats.firstOrNull { it.group != null && it.group!!.ID == group.ID }
                    ?: ChatItem(null, group, null)
            chatItem.lastMessage = message

            if (chatItem.isArchived) {
                chatItem.isArchived = false
                archivedChats.remove(chatItem)
                setArchivedChatItems(ArrayList(archivedChats))
                async { unarchiveChat(chatItem) }
            }
            chatItems.add(chatItem)
        }
        setChatItems(ArrayList(chatItems))
    }

    fun removeGroupFromChatItems(group: BBGroup) {
        val chatItems = chatItems.value?.toMutableList() ?: mutableListOf()
        val archivedChats = archivedChatItems.value?.toMutableList() ?: mutableListOf()
        archivedChats.removeAll { it.isGroup && it.group!!.ID == group.ID }
        chatItems.removeAll { it.isGroup && it.group!!.ID == group.ID }
        setChatItems(ArrayList(chatItems))
    }

    fun removeContactFromChatItems(contact: BBContact) {
        val chatItems = chatItems.value?.toMutableList() ?: mutableListOf()
        val archivedChats = archivedChatItems.value?.toMutableList() ?: mutableListOf()
        archivedChats.removeAll { !it.isGroup && it.contact!!.registeredNumber == contact.registeredNumber }
        chatItems.removeAll { !it.isGroup && it.contact!!.registeredNumber == contact.registeredNumber }
        setChatItems(ArrayList(chatItems))
    }

    // endregion

    // region Messages Functions
    suspend fun fetchSingleMessageInternalPushAsync() : Message? = withContext(Dispatchers.IO) {
        account.state.value?.let { accountState ->
            if (accountState != BBAccountRegistrationState.Registered) return@withContext null
        }

        val pwdConf = pwdConf ?: return@withContext null
        val jsonString = bb_get_newmsg_fileasync(pwdConf)
        Log.d("Message_received", jsonString)
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(Message::class.java, MessageJsonDeserializer())
        val message = gsonBuilder.create().fromJson(jsonString, Message::class.java)
        if (message.answer == "KO") return@withContext null
        return@withContext message
    }
    // endregion

    // region C EXTERNAL FUNCTIONS

    private external fun bb_get_contacts(search_text: String, contactid: Int, flagsearch: Int, limitsearch: Int, pwdconf: String) : String
    private external fun bb_last_voicecalls(pwdconf: String) : String
    private external fun bb_get_list_chat(pwdconf: String) : String
    private external fun bb_get_profileinfo(recipient: String, pwdconf: String) : String
    private external fun bb_decrypt_pwdconf(pwdconfenc: ByteArray, pwdconfenclen: Int, keyp: String, pwdconflen: Int, tmpfolder: String) : String
    private external fun bb_add_contact(contactJson: String, pwdconf: String) : String;
    private external fun bb_update_contact(contactJson: String, pwdconf: String): String;
    private external fun bb_get_newmsg_fileasync(pwdconf: String) : String
    private external fun bb_new_groupchat(description: String, pwdconf: String) : String
    private external fun bb_filetransfer_getstatus(filename: String) : Int
    private external fun bb_delete_contact(contactID: String, pwdconf: String) : String
    private external fun bb_set_archivedchat(recipient: String = "", groupid: String = "", pwdconf: String) : String
    private external fun bb_unset_archivedchat(recipient: String = "", groupid: String = "", pwdconf: String) : String
    private external fun bb_set_notification(recipient: String, groupid: String, soundName: String, pwdconf: String) : String
    private external fun bb_get_notifications(pwdconf: String) : String
    private external fun bb_get_starred_messages(recipient: String, groupid: String, pwdconf: String) : String
    // endregion

}




fun JSONObject.getStringOrNull(string: String) : String? {
    if (has(string) == false)
        return null

    return getString(string)
}

fun <T> MutableLiveData<T>.notifyObserver() {
    this.postValue(this.value)
}



