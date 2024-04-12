package com.ec.bond.blackbox.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.ec.bond.activity.ui.chatbrowsing.*
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.getStringOrNull
import com.ec.bond.blackbox.model.api_responses.FetchAutoDeleteTimerResponse
import com.ec.bond.blackbox.model.api_responses.GeneralResponse
import com.ec.bond.utils.CommonUtils.getTimeString
import com.ec.bond.utils.DateTimeUtils
import com.ec.bond.utils.getDateInTimezone
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * This class is the shared base object of BBContact & BBGroup and holds everything thats used in the Chat screen.
 */
open class BBChat {



    var updateUI = MutableLiveData<Message>()
    var updateAlertMessages = MutableLiveData<Message>()
    val typingMessage = MutableLiveData("")

    var datePositionList = ArrayList<DatePosition>()

    var messageNotificationSoundName: String = "Default"
    var messages = ArrayList<ChatBrowsingListItem>()
    var starredMessages = listOf<ChatBrowsingListItem>()
    val isLoadedLiveData = MutableLiveData(LoadedMessagesStatus.NotStarted)
    val notifyItemChangedPosition = MutableLiveData<String>()

    var isGetData: Boolean = false
    var chatStatusType = ChatStatusType.Start
    var numberNewMessages: Int = 0

    var oldestUnreadMsgID: String? = null
    var unreadMessagesCount: Int = 0

    private val _autoDeleteTimer = MutableLiveData(ChatAutoDeleteTimer.Never)
    val autoDeleteTimer: LiveData<ChatAutoDeleteTimer> get() = _autoDeleteTimer

    val _isReceiveNewMessages = MutableLiveData<Message>()

    // Path to Contact profile image
    private val _chatImagePath = MutableLiveData<String>()
    val chatImagePath: LiveData<String> get() = _chatImagePath

    fun setChatImagePath(path: String) {
        _chatImagePath.postValue(path)
    }
    fun getChatImagePath() : String? {
        return _chatImagePath.value
    }

    /**
     * Fetch the messages from the server using the filters provided.
     *
     * @return An List of Messages ChatBrowsingListItem
     */
    suspend fun fetchMessages(fromId: String = "", toId: String = "", fromDate: String = "",
                              toDate: String = "", limit: Int = 40) : List<ChatBrowsingListItem> = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext listOf()
        val recipient = if (this@BBChat is BBContact) this@BBChat.registeredNumber else ""
        val groupId = if (this@BBChat is BBGroup) this@BBChat.ID else ""
        try {

            isLoadedLiveData.postValue(LoadedMessagesStatus.Waiting)

            val jsonString = bb_get_msgs_fileasync(pwdConf, recipient, groupId, fromId, toId, fromDate, toDate, limit)
            val jsonObject = JSONObject(jsonString)
            val answer = jsonObject.getStringOrNull("answer") ?: return@withContext arrayListOf()
            Log.e("previous_messages---",answer)
            if (answer == "KO") {
                if (jsonObject.getStringOrNull("message")!!.contains("No messages has been found")) {
                    isLoadedLiveData.postValue(LoadedMessagesStatus.Loaded)
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
                    val date = dateFormat.parse(it.dateSentString!!)?.getDateInTimezone()
                    calendar.time = date!!
                    return@groupBy calendar.get(Calendar.DAY_OF_MONTH)
                }

                groupedMessages.toList().forEach {
                    val date = it.second.first().dateSent ?: return@forEach
                    if (this@BBChat.messages.size > 0) {
                        if ((this@BBChat.messages[this@BBChat.messages.lastIndex] as DateItem).date.getTimeString() == it.second.first().dateSent?.getTimeString()) {
                            this@BBChat.messages.removeAt(this@BBChat.messages.lastIndex)
                        }
                    }
                    this@BBChat.messages.addAll(it.second.map { MessageItem(it) })
                    this@BBChat.messages.add(DateItem(date))
                }
                isLoadedLiveData.postValue(LoadedMessagesStatus.Loaded)
                Log.i("getInitialMessages","fetchMessages() chatId = $recipient or group = $groupId and messages No = ${messages.size}")

                fillDatePositions()


                return@withContext this@BBChat.messages.toList()
            }
            listOf()
        } catch (ex: Exception) {
            Log.i("getInitialMessages","fetchMessages() chatId = $recipient or group = $groupId and messages No = ${messages.size}")

            if (messages.size == 0) {
                isLoadedLiveData.postValue(LoadedMessagesStatus.Failed)
                isGetData = false
            }
            Log.e("fetchMessages()", ex.message.toString())
            listOf()
        }
    }

    suspend fun fetchStarredMessages() : List<ChatBrowsingListItem> = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext listOf()
        val recipient = if (this@BBChat is BBContact) this@BBChat.registeredNumber else ""
        val groupId = if (this@BBChat is BBGroup) this@BBChat.ID else ""
        try {

            isLoadedLiveData.postValue(LoadedMessagesStatus.Waiting)

            val jsonString = bb_get_starred_messages(recipient, groupId, pwdConf)
            val jsonObject = JSONObject(jsonString)
            val answer = jsonObject.getStringOrNull("answer") ?: return@withContext arrayListOf()
            if (answer == "KO") {
                if (jsonObject.getStringOrNull("message")!!.contains("No messages has been found")) {
                    isLoadedLiveData.postValue(LoadedMessagesStatus.Loaded)
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
                    val date = dateFormat.parse(it.dateSentString!!)?.getDateInTimezone()
                    calendar.time = date!!
                    return@groupBy calendar.get(Calendar.DAY_OF_MONTH)
                }

                groupedMessages.toList().forEach {
                    val date = it.second.first().dateSent ?: return@forEach
                    if (this@BBChat.messages.size > 0) {
                        if ((this@BBChat.messages[this@BBChat.messages.lastIndex] as DateItem).date.getTimeString() == it.second.first().dateSent?.getTimeString()) {
                            this@BBChat.messages.removeAt(this@BBChat.messages.lastIndex)
                        }
                    }
                    this@BBChat.messages.addAll(it.second.map { MessageItem(it) })
                    this@BBChat.messages.add(DateItem(date))
                }
                isLoadedLiveData.postValue(LoadedMessagesStatus.Loaded)
                Log.i("getInitialMessages","fetchMessages() chatId = $recipient or group = $groupId and messages No = ${messages.size}")

                fillDatePositions()


                return@withContext this@BBChat.messages.toList()
            }
            listOf()
        } catch (ex: Exception) {
            Log.i("getInitialMessages","fetchMessages() chatId = $recipient or group = $groupId and messages No = ${messages.size}")

            if (messages.size == 0) {
                isLoadedLiveData.postValue(LoadedMessagesStatus.Failed)
                isGetData = false
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
     * Delete the Chat on a background thread
     */
    suspend fun clearChat() : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val recipient = if (this@BBChat is BBContact) this@BBChat.registeredNumber else ""
        val groupId = if (this@BBChat is BBGroup) this@BBChat.ID else ""
        val jsonString = bb_delete_chat(recipient, groupId, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)

        if (response.isSuccess) {
            val messagesItems = messages.filterIsInstance<MessageItem>().toList()
            async {
                for (messageItem in messagesItems) {
                    if (messageItem.message.msgtype == "file") {
                        val filePath = messageItem.message.localFileName.value ?: continue
                        val file = File(filePath)
                        file.delete()
                    }
                }
            }
            messages.clear()
            return@withContext true
        } else {
            Log.e("clearChat", response.message)
        }
        false
    }

    fun addInGeneralMessageList(message: Message) {
//        Log.i("newMessage","in addInGeneralMessageList() messageId = ${message.ID} and messageBody = ${message.body} and contactId = ${(this as BBContact).registeredNumber}")

        val date = message.dateSent ?: return
        if (messages.size > 0) {

            if (messages.filterIsInstance<DateItem>().first().date.getTimeString() == date.getTimeString()) {
                messages.add(0, MessageItem(message))
            } else {
                messages.add(0, DateItem(date))
                messages.add(0, MessageItem(message))

            }
        } else {
            messages.add(0, DateItem(message.dateSent!!))
            messages.add(0, MessageItem(message))
        }
        GlobalScope.launch(Dispatchers.Main) {
            updateUI.value = message
        }
    }

    fun updateMessageCheckmarkType(msg: Message,isRead: Boolean) {

        val message = messages.filterIsInstance<MessageItem>().firstOrNull { message ->
            message.message.ID == msg.body
        }?.message ?: return

        if (isRead)
            message.setCheckmarkType(CheckmarkType.read)
        else
            message.setCheckmarkType(CheckmarkType.received)
        notifyItemChangedPosition(message.ID)
    }

    fun notifyItemChangedPosition(id: String) {
        GlobalScope.launch(Dispatchers.Main) {
            notifyItemChangedPosition.value = id
        }
    }

    fun deleteMessage(msg: Message?) {

        val message = messages.filterIsInstance<MessageItem>().firstOrNull { message ->
            message.message.ID == msg?.body
        }?.message ?: return

        if (message.localFileName.value?.isNotEmpty() == true) {
            var file = File(message.localFileName.value)
            file.delete()
        }
        message.type = MessageType.Deleted
        message.body = ""
        notifyItemChangedPosition(message.ID)
    }

    fun getNewMessages(message: Message) {
        numberNewMessages++
        _isReceiveNewMessages.postValue(message)
    }

    suspend fun setUnStarredMsg(message: Message) : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_unset_starredmsg(message.ID, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            return@withContext true
        }
        else {
            message._isStarred.postValue(true)
        }
        false
    }

    suspend fun setStarredMsg(message: Message): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_set_starredmsg(message.ID, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        message._isStarred.postValue(response.isSuccess)
        return@withContext response.isSuccess
    }

    suspend fun deleteMsg(message: Message) = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_delete_message(message.ID, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        return@withContext response.isSuccess
    }

    suspend fun sendReadReceipt(message: Message) = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val msgID = message.ID.toIntOrNull() ?: return@withContext false
        val recipient = message.sender
        val jsonString = bb_send_read_receipt(recipient, msgID, pwdConf)
        Log.i("readstatus","$jsonString and messageId = $msgID")
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            message.setCheckmarkType(CheckmarkType.read)
        }
        return@withContext response.isSuccess
    }

    suspend fun setForwardMessage(message: Message) : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_set_forwardedmsg(message.ID, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        return@withContext response.isSuccess
    }

    /**
     * API call to set the messages auto delete timer.
     *
     * @param timer: The selected timer
     * @return true if success and update the autoDeleteTimer observable property
     */
    suspend fun setAutoDeleteMessages(timer: ChatAutoDeleteTimer) : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val recipient = if (this@BBChat is BBContact) this@BBChat.registeredNumber else ""
        val groupId = if (this@BBChat is BBGroup) this@BBChat.ID else ""

        val jsonString = bb_autodelete_chat(timer.toSeconds(), recipient, groupId, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            // post the updated Value
            _autoDeleteTimer.postValue(timer)
            return@withContext true
        }
        false
    }

    /**
     * API call to fetch the chat Messages Auto Delete Timer.
     */
    suspend fun fetchAutoDeleteTimer() = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext
        val recipient = if (this@BBChat is BBContact) this@BBChat.registeredNumber else ""
        val groupId = if (this@BBChat is BBGroup) this@BBChat.ID else ""

        val jsonString = bb_autodelete_chat_getconf(recipient, groupId, pwdConf)
        val response = Gson().fromJson(jsonString, FetchAutoDeleteTimerResponse::class.java)

        if (response.isSuccess) {
            _autoDeleteTimer.postValue(ChatAutoDeleteTimer.secondsToTimer(response.seconds))
        }
    }


    private external fun bb_get_msgs_fileasync(pwdconf: String, recipient: String, groupid: String, fromId: String, toId: String, fromDate: String, toDate: String, limit: Int) : String
    private external fun bb_delete_chat(contactnumber: String, groupid: String, pwdconf: String) : String
    external fun bb_get_photo(filename: String, pwdconf: String) : String
    private external fun bb_unset_starredmsg(msgid: String,pwdconf: String) : String
    private external fun bb_set_starredmsg(msgid: String,pwdconf: String) : String
    private external fun bb_delete_message(msgid: String, pwdconf: String) : String
    private external fun bb_send_read_receipt(recipient: String, msgID: Int, pwconf: String) : String
    private external fun bb_set_forwardedmsg(msgid: String, pwdconf: String) : String

    private external fun bb_autodelete_chat(seconds: Int, recipient: String = "", groupid: String = "", pwdconf: String) : String
    private external fun bb_autodelete_chat_getconf(recipient: String = "", groupid: String = "", pwdconf: String) : String
    private external fun bb_get_starred_messages(recipient: String, groupid: String, pwdconf: String) : String

//    external fun bb_get_newmsg(pwconf: String) : String
}

enum class ChatStatusType {
    Start,PreviousMsg,NewMsg,FinishPrev,GettingNewData
}

enum class ChatAutoDeleteTimer {
    OneHour, TwoHours, OneDay, TwoDays, OneWeek, Never;

    fun toSeconds() : Int {
        return when (this) {
            Never -> 0
            OneHour -> 3600
            TwoHours -> 7200
            OneDay -> 86400
            TwoDays -> 172800
            OneWeek -> 604800
        }
    }

    companion object {
        @JvmStatic
        fun secondsToTimer(seconds: Int) : ChatAutoDeleteTimer {
            return when (seconds) {
                0 -> Never
                3600 -> OneHour
                7200 -> TwoHours
                86400 -> OneDay
                172800 -> TwoDays
                604800 -> OneWeek
                else -> Never
            }
        }
    }
}

enum class LoadedMessagesStatus {
    Loaded, Failed, Waiting, NotStarted
}