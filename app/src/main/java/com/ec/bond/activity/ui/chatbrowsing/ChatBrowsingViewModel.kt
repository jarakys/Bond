package com.ec.bond.activity.ui.chatbrowsing

import android.content.Context
import android.database.ContentObserver
import android.media.MediaPlayer
import android.net.Uri
import android.os.FileObserver
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.ec.bond.R
import com.ec.bond.adapter.StarredAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.*
import com.ec.bond.blackbox.model.Message.Companion.getFileType
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.CommonUtils.createAudioFile
import com.ec.bond.utils.CommonUtils.createDocumentFile
import com.ec.bond.utils.CommonUtils.createImageFile
import com.ec.bond.utils.CommonUtils.createVideoFile
import com.ec.bond.utils.CommonUtils.getMediaDuration
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class ChatBrowsingViewModel @Inject constructor() : ViewModel() {
    lateinit var fileObserver: FileObserver
    val waitingReadMessages by lazy {
        ArrayList<Message>()
    }
    lateinit var context: Context
    var isStarredMessagesAction: Boolean = true
    var recipient: String = ""
    var isFromForward =
        false // this to know if chatTypeRef Change from forward not change again from onCreateView()
    private val _selectedMessages = MutableLiveData<ArrayList<MessageItem>>().apply {
        value = ArrayList()
    }
    private val _messages = MutableLiveData<ArrayList<MessagesSection>>()
    val messages: LiveData<ArrayList<MessagesSection>> get() = _messages

    val _ismessagesLoaded = MutableLiveData<Boolean>()
    val ismessagesLoaded: LiveData<Boolean> get() = _ismessagesLoaded
    private val _scrollToBottomBtnAppear = MutableLiveData<Boolean>()
    val scrollToBottomBtnAppear: LiveData<Boolean> get() = _scrollToBottomBtnAppear

    private val viewModelJob = SupervisorJob()

    internal val _isSendForward = MutableLiveData<Boolean>()

    private val _dateText = MutableLiveData<String>()
    val dateText: LiveData<String> get() = _dateText

    var currentDate: String = ""
    var isDateChanged: Boolean = false

    var insertedRangeFrom: Int = 0

    val _isDateAppear = MutableLiveData<Boolean>()


    lateinit var chatTypeRef: BBChat
    lateinit var prevChatTypeRef: BBChat

    ///   menu
    var menuActionBar = MutableLiveData<Menu>()

    var menuInflater = MutableLiveData<MenuInflater>()

    private val _isLongPressed = MutableLiveData<Boolean>().apply {
        value = false
    }

    private val _isLongViewPressed = MutableLiveData<View>().apply {
        value = null
    }

    val isLongPressed: LiveData<Boolean> get() = _isLongPressed
    val isLongPressedView: LiveData<View> get() = _isLongViewPressed

    private val _longPressedTitle = MutableLiveData<String>()

    val longPressedTitle: LiveData<String> get() = _longPressedTitle
    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)

    val isSearchStart = MutableLiveData<Boolean>().apply {
        value = false
    }


    lateinit var contentObserver: ContentObserver
    var lastScreenshotUri: String? = null

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun registerChatRefType(id: String) {
        chatTypeRef = (Blackbox.getContact(id) ?: Blackbox.getTemporaryContact(id)) as BBChat?
            ?: Blackbox.chatItems.value?.map { it.group }?.firstOrNull { it?.ID == id }
                    ?: Blackbox.archivedChatItems.value?.map { it.group }
                ?.firstOrNull { it?.ID == id }!!
        prevChatTypeRef = chatTypeRef
    }

    suspend fun retreiveInitialMessages(): Boolean? = withContext(Dispatchers.Main) {
        chatTypeRef.isLoadedLiveData.value == LoadedMessagesStatus.Loaded
    }

    fun retry_msg_data(fromId: String?, toId: String?, limit: Int?, id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (id.isNotEmpty()) {
                registerChatRefType(id)
            }
            var messagesCounterBeforeNewValues =
                chatTypeRef.messages.filterIsInstance<MessageItem>().count()

            if (toId == null && chatTypeRef.isGetData) {
                return@launch
            }

            chatTypeRef.isGetData = true
            chatTypeRef.fetchMessages(fromId ?: "", toId ?: "", "", "", limit ?: 80)

            if (toId != null) {

                val messagesCount = chatTypeRef.messages.filterIsInstance<MessageItem>().count()
                chatTypeRef.chatStatusType =
                    if ((messagesCount - messagesCounterBeforeNewValues) == 0) {
                        ChatStatusType.FinishPrev
                    } else
                        ChatStatusType.PreviousMsg
                _ismessagesLoaded.postValue(true)
            }
        }
    }


    suspend fun clearChat(): Boolean = withContext(Dispatchers.IO) {
        return@withContext chatTypeRef.clearChat()
    }

    fun sendMessage(chatType: BBChat, body: String, replyMessage: Pair<String, String>) =
        viewModelScope.launch(Dispatchers.Main) {
            val getLastID =
                chatTypeRef.messages.filterIsInstance<MessageItem>().firstOrNull()?.message?.ID
            var id = 0
            if (getLastID != null) {
                id = getLastID.toInt()
            }
            Log.i("message", body)
            var message = Message()
            message.body = body

            message.ID = "${id + 100}"
            message.setCheckmarkType(CheckmarkType.unSent)
            message.sender = Blackbox.account.registeredNumber ?: ""

            if (replyMessage.first.isNotEmpty()) {
                message.repliedToMsgId = replyMessage.first
                message.repliedToText = replyMessage.second
            } else {
                message.repliedToMsgId = "0"
            }
            message.dateSent = Calendar.getInstance().time
            if (body == "alert:#screenrecording") {
                message.type = MessageType.AlertScreenRecording
                message.msgtype = body
            } else if (body.contains("alert:#")) {
                val arr = body.split(":#")
                if (body.contains("alert:#copy")) {
                    message.type = MessageType.AlertCopy
                    message.body = body
                } else if (body.contains("alert:#forward")) {
                    message.type = MessageType.AlertForward
                    message.body = body
                }
                message.alertMsgSenderRef = arr[2]
                message.alertMsgIdRef = arr[3]
                message.alertMsgTypeRef = getAlertMsgTypeRef(arr[4])
                message.alertMsgContentRef = arr[5]
            } else {
                message.msgtype = "txt"
            }
            chatType.addInGeneralMessageList(message)

            if (chatType is BBContact) {
                if (chatType.sendTextMessage(message) == true) {
                    chatType.notifyItemChangedPosition(message.ID)
                }
            } else {
                if ((chatType as BBGroup).sendTextMessage(message) == true) {
                    chatType.notifyItemChangedPosition(message.ID)
                }
            }
        }

    private fun getAlertMsgTypeRef(msgType: String): MessageType? {
        return when (msgType) {
            "audio" -> {
                MessageType.Audio
            }
            "text" -> {
                MessageType.Text
            }
            "photo" -> {
                MessageType.Photo
            }
            "video" -> {
                MessageType.Video
            }
            "location" -> {
                MessageType.Location
            }
            "contact" -> {
                MessageType.Contact
            }
            else -> {
                MessageType.DocumentGeneric
            }
        }
    }

    fun send_file(uri: Array<String>, msg: String, filesNames: ArrayList<String> = ArrayList()) {
        isFromForward = true
        val getLastID =
            chatTypeRef.messages.filterIsInstance<MessageItem>().firstOrNull()?.message?.ID
        var id = 0
        if (getLastID != null) {
            id = getLastID.toInt()
        }
        for (i in uri.indices) {
            var msgBody = ""
            if (filesNames.size > 0) {
                msgBody = filesNames[i]
            } else {
                msgBody = if (i == 0) msg else ""
            }
            var fileType =
                if (msg == "alert:#screenshot") MessageType.AlertScreenshot else getFileType(uri[i])
            sendFileMessage(chatTypeRef, uri[i], msgBody, (id + 100 + i).toString(), fileType)
        }
    }

    fun sendFileMessage(
        chatType: BBChat,
        uri: String,
        msg: String,
        msgID: String,
        msgType: MessageType
    ) {
        val sender = Blackbox.account.registeredNumber ?: return
        val file = File(uri)
        val message = Message().apply {
            msgtype = "file"
            body = msg
            originalFilePath = uri   //uri[i]
            originFileName = UUID.randomUUID().toString()
            repliedToMsgId = "0"
            ID = msgID
            this.sender = sender
            dateSent = Calendar.getInstance().time
            setCheckmarkType(CheckmarkType.unSent)
            type = msgType
        }

        chatType.addInGeneralMessageList(message)
        GlobalScope.async {
            if (chatType is BBContact) {

                if (chatType.sendFileMessage(message) && chatType === chatTypeRef) {
                    chatTypeRef.notifyItemChangedPosition(message.ID)

                    // delete the temporary file.
                    // the app should update the UI based on the message.localFileName that
                    // will be updated with *sendFileMessage* function on success.
                    file.delete()
                }
            } else if (chatType is BBGroup) {
                if (chatType.sendFileMessage(message) && chatType === chatTypeRef) {
                    chatTypeRef.notifyItemChangedPosition(message.ID)

                    // delete the temporary file.
                    // the app should update the UI based on the message.localFileName that
                    // will be updated with *sendFileMessage* function on success.
                    file.delete()
                }
            }
        }

        Log.i("Msg Send Number", message.ID)
    }


    fun setStarredMsg(message: Message) = GlobalScope.launch(Dispatchers.Main) {
        chatTypeRef.setStarredMsg(message)
    }

    fun setUnStarredMsg(message: Message) = GlobalScope.launch(Dispatchers.Main) {
        chatTypeRef.setUnStarredMsg(message)
    }

    fun deleteMsg(message: Message) = GlobalScope.launch(Dispatchers.Main) {
        chatTypeRef.deleteMsg(message)
    }

    fun setIsLongPressed(isLongPress: Boolean, view: View? = null) {
        _isLongPressed.value = isLongPress
        view?.let {
            _isLongViewPressed.value = view
        }
    }

    fun setLongPressedTitle(title: String) = _longPressedTitle.postValue(title)

    fun addToSelectedMessages(item: MessageItem) {
        _selectedMessages.value?.add(item)
        menuActionBar.value?.findItem(R.id.action_replyMsg)?.isVisible =
            getSelectedMessages().size == 1
        menuActionBar.value?.findItem(R.id.action_msg_info)?.isVisible =
            getSelectedMessages().size == 1
        menuActionBar.value?.findItem(R.id.action_msg_copy)?.isVisible =
            getSelectedMessages().size == 1
    }

    fun clearSelectedMessages() {
        _selectedMessages.value?.clear()
    }

    fun getSizeSelectedMessages(): Int? {
        return _selectedMessages.value?.size
    }

    fun removeItemSelectedMessages(item: MessageItem) {
        val removedItem = _selectedMessages.value?.first { it.message.ID == item.message.ID }
        _selectedMessages.value?.remove(removedItem)
        menuActionBar.value?.findItem(R.id.action_replyMsg)?.isVisible =
            getSelectedMessages().size == 1
        menuActionBar.value?.findItem(R.id.action_msg_info)?.isVisible =
            getSelectedMessages().size == 1
    }

    fun getSelectedMessages(): ArrayList<MessageItem> {
        return _selectedMessages.value!!
    }

    fun showBottomScrollBtn(check: Boolean) {
        if (check != scrollToBottomBtnAppear.value)
            _scrollToBottomBtnAppear.postValue(check)
    }

    fun markAllMessagesAsRead() = viewModelScope.launch(Dispatchers.IO) {
        var unReadMessages = chatTypeRef.messages.filterIsInstance<MessageItem>().filter {
//            it.message.type != MessageType.Deleted && !it.message.isOutgoing && it.message.checkmarkType.value != CheckmarkType.read
            it.message.type != MessageType.Deleted && !it.message.isOutgoing && it.message.dateRead == null
        }.map { it.message }.reversed()

        unReadMessages?.forEach {
            Log.i("readstatus", "Call markAllMessagesAsRead() messageId = ${it.ID}")
            chatTypeRef.sendReadReceipt(it)
        }

    }

    fun markMessageRead(message: Message) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("readstatus", "Call markMessageRead() messageId = ${message.ID}")
        chatTypeRef.sendReadReceipt(message)
    }


    // forward Message
    fun sendForwardMessage(messages: Array<MessageItem>, contacts: ArrayList<BBChat>) =
        GlobalScope.launch(Dispatchers.IO) {
            isFromForward = true
            launch(Dispatchers.Main) {
                _isSendForward.value = true
            }
            if (contacts.size == 1) {
                prevChatTypeRef = chatTypeRef
                if (contacts[0] is BBContact) {
                    if ((contacts[0] as BBContact).registeredNumber != (chatTypeRef as? BBContact)?.registeredNumber ?: "") {
                        chatTypeRef = contacts[0]
//                    chatTypeRef.messagesSection2.clear()
//                    datePositionList.clear()
                    }
                } else {
                    if ((contacts[0] as BBGroup).ID != (chatTypeRef as? BBGroup)?.ID ?: "") {
                        chatTypeRef = contacts[0]
                    }
                }

            }
            messages.forEach { message ->
                withContext(viewModelScope.coroutineContext + Dispatchers.Main) {
                    if (message.message.isOutgoing == false) sendChatAlertAsync(
                        AlertType.MessagesForwarded,
                        message.message
                    )
                }
                chatTypeRef.setForwardMessage(message.message)

                contacts.forEachIndexed secondLoop@{ index, contact ->
                    if (message.message.type != MessageType.Text) {
                        val getLastID = chatTypeRef.messages.filterIsInstance<MessageItem>()
                            .firstOrNull()?.message?.ID
                        var id = 0
                        if (getLastID != null) {
                            id = getLastID.toInt()
                        }
                        var uri =
                            makeCopyFile(message.message.localFileName.value, message.message.type)
                        sendFileMessage(
                            contact,
                            uri,
                            message.message.body,
                            (id + 100 + index).toString(),
                            message.message.type
                        )
                    } else {
                        val getLastID = chatTypeRef.messages.filterIsInstance<MessageItem>()
                            .firstOrNull()?.message?.ID
                        var id = 0
                        if (getLastID != null) {
                            id = getLastID.toInt()
                        }
                        var messageCpy = Message()
                        messageCpy.body = message.message.body
                        messageCpy.msgtype = "txt"
                        messageCpy.ID = "${id + 100}"
                        messageCpy.setCheckmarkType(CheckmarkType.unSent)
                        messageCpy.sender = Blackbox.account.registeredNumber ?: ""
                        messageCpy.dateSent = Calendar.getInstance().time
                        messageCpy.repliedToMsgId = "0"
                        contact.addInGeneralMessageList(messageCpy)
                        if (chatTypeRef is BBContact) {
//                        if (contact === chatTypeRef) {
//                        updateUIWithNewMessages(contact,messageCpy)
//                        }
                            if (contact is BBContact) {
                                if (contact.sendTextMessage(messageCpy) == true && contact === chatTypeRef) {
                                    contact.notifyItemChangedPosition(messageCpy.ID)
                                }
                            } else if (contact is BBGroup) {
                                if (contact.sendTextMessage(messageCpy) == true && contact === chatTypeRef) {
                                    contact.notifyItemChangedPosition(messageCpy.ID)
                                }
                            }

                        }
                    }
                }

            }
        }

    private fun makeCopyFile(value: String?, type: MessageType): String {
        var file: File
        when (type) {
            MessageType.Photo -> {
                file = createImageFile(context, "jpg")
            }
            MessageType.Video -> {
                file = createVideoFile(context, "mp4")
            }
            MessageType.Audio -> {
                file = createAudioFile(context, "m4a")
            }
            MessageType.DocumentAppleKeynote -> file = createDocumentFile(context, "key")
            MessageType.DocumentApplePages -> file = createDocumentFile(context, "pages")
            MessageType.DocumentAppleNumbers -> file = createDocumentFile(context, "numbers")
            MessageType.DocumentMicrosoftExcel -> file = createDocumentFile(context, "xlsx")
            MessageType.DocumentMicrosoftPowerPoint -> file = createDocumentFile(context, "pptx")
            MessageType.DocumentMicrosoftWord -> file = createDocumentFile(context, "docx")
            MessageType.DocumentPDF -> file = createDocumentFile(context, "pdf")
            else -> file = createDocumentFile(context, "txt")
        }
        var originalFile = File(value)
        originalFile.copyTo(file)
        return file.path
    }

    fun changeDateText(date: String) {

        currentDate = date
        isDateChanged = true
        notifyDateChange()
    }

    fun notifyDateChange() {
        _dateText.postValue(currentDate)
        isDateChanged = false
    }

    fun changeDateAppear(isAppear: Boolean) {
//        if (isAppear != _isDateAppear.value) {
//            _isDateAppear.postValue(isAppear)
        _isDateAppear.value = isAppear
//        }
    }

    fun replyForDocuments(
        context: Context,
        msgBody: String,
        mediaTypeReply_imageView: ImageView,
        replyOwnerMsg_Txt: TextView
    ) {
        mediaTypeReply_imageView.visibility = View.VISIBLE
        Glide.with(context).load(R.drawable.document_chatlist).apply(requestOptions).dontTransform()
            .into(mediaTypeReply_imageView)

        replyOwnerMsg_Txt.text = msgBody
    }

    fun replyForAudio(
        context: Context,
        path: String,
        mediaTypeReply_imageView: ImageView,
        replyOwnerMsg_Txt: TextView
    ) {
        mediaTypeReply_imageView.visibility = View.VISIBLE
        Glide.with(context).load(R.drawable.mic_grey_icon).apply(requestOptions).dontTransform()
            .into(mediaTypeReply_imageView)
        val duration = getMediaDuration(context, path)
        replyOwnerMsg_Txt.text = context.getString(R.string.voice_message) + duration

    }

    fun replyForPhotos(
        context: Context,
        msg: Message,
        mediaReply_imageView: ImageView,
        mediaTypeReply_imageView: ImageView,
        replyOwnerMsg_Txt: TextView
    ) {
        mediaReply_imageView.visibility = View.VISIBLE
        mediaTypeReply_imageView.visibility = View.VISIBLE
        val path = if (msg.fileName.isEmpty()) {
            msg.originalFilePath
        } else {
            Blackbox.getDocumentsDir(context) + "/" + msg.fileName
        }

//        mediaReply_imageView.set(BitmapFactory.decodeFile(path))
        Glide.with(context).load(path).apply(requestOptions).dontTransform()
            .into(mediaReply_imageView)
        Glide.with(context).load(R.drawable.last_msg_photo).apply(requestOptions).dontTransform()
            .into(mediaTypeReply_imageView)
//        mediaTypeReply_imageView.set(R.drawable.last_msg_photo)

        if (msg.body.isEmpty()) {
            replyOwnerMsg_Txt.text = (context as AppCompatActivity).getString(R.string.media_photo)
        }
    }

    fun replyForVideos(
        context: Context,
        msg: Message,
        mediaReply_imageView: ImageView,
        mediaTypeReply_imageView: ImageView,
        replyOwnerMsg_Txt: TextView
    ) {
        mediaReply_imageView.visibility = View.VISIBLE
        mediaTypeReply_imageView.visibility = View.VISIBLE
        val path = if (msg.fileName.isEmpty()) {
            msg.originalFilePath
        } else {
            Blackbox.getDocumentsDir(context) + "/" + msg.fileName
        }
        Glide.with(context).load(path).thumbnail(0.1f).apply(requestOptions).dontTransform()
            .into(mediaReply_imageView)
        Glide.with(context).load(R.drawable.video_chatlist).apply(requestOptions).dontTransform()
            .into(mediaTypeReply_imageView)

        val mp = MediaPlayer.create(context, Uri.parse(path))
        if (mp == null) {
            return
        }
        val duration = mp.duration
        mp.release()

        if (msg.body.isEmpty()) {
            replyOwnerMsg_Txt.text =
                (context as AppCompatActivity).getString(R.string.media_video) + String.format(
                    " (%d:%02d)",
                    TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(duration.toLong())
                    )
                )
        }
    }

    fun drawDocumentUI(
        type: MessageType,
        context: Context,
        fileType_IV: ImageView,
        fileType_TV: TextView,
        fileType: ChatBrowsingAdapter.DocumentMessageViewHolder.FileType?
    ) {
        when (type) {
            MessageType.DocumentPDF -> {
                Glide.with(context).load(R.drawable.pdf_icon).apply(requestOptions).dontTransform()
                    .into(fileType_IV)

                fileType_TV.text = "PDF"
                fileType?.type = "application/pdf"
            }
            MessageType.DocumentMicrosoftWord -> {
                Glide.with(context).load(R.drawable.word_icon).apply(requestOptions).dontTransform()
                    .into(fileType_IV)
                fileType_TV.text = "DOCX"
                fileType?.type = "application/msword"
            }
            MessageType.DocumentMicrosoftPowerPoint -> {
                Glide.with(context).load(R.drawable.powerpoint_icon).apply(requestOptions)
                    .dontTransform().into(fileType_IV)

                fileType_TV.text = "PPTX"
                fileType?.type = "application/vnd.ms-powerpoint"
            }
            MessageType.DocumentMicrosoftExcel -> {
                Glide.with(context).load(R.drawable.excel_icon).apply(requestOptions)
                    .dontTransform().into(fileType_IV)

                fileType_TV.text = "XLSX"
                fileType?.type = "application/vnd.ms-excel"
            }
            MessageType.DocumentAppleKeynote -> {
                Glide.with(context).load(R.drawable.keynote_icon).apply(requestOptions)
                    .dontTransform().into(fileType_IV)

                fileType_TV.text = "KEY"
                fileType?.type = "application/*"
            }
            else -> {
                Glide.with(context).load(R.drawable.generic_file_icon).apply(requestOptions)
                    .dontTransform().into(fileType_IV)

                fileType_TV.text = "TXT"
                fileType?.type = "*/*"
            }
        }
    }

    fun drawDocumentUIS(
        type: MessageType,
        context: Context,
        fileType_IV: ImageView,
        fileType_TV: TextView,
        fileType: StarredAdapter.DocumentMessageViewHolder.FileType?
    ) {
        when (type) {
            MessageType.DocumentPDF -> {
                Glide.with(context).load(R.drawable.pdf_icon).apply(requestOptions).dontTransform()
                    .into(fileType_IV)

                fileType_TV.text = "PDF"
                fileType?.type = "application/pdf"
            }
            MessageType.DocumentMicrosoftWord -> {
                Glide.with(context).load(R.drawable.word_icon).apply(requestOptions).dontTransform()
                    .into(fileType_IV)
                fileType_TV.text = "DOCX"
                fileType?.type = "application/msword"
            }
            MessageType.DocumentMicrosoftPowerPoint -> {
                Glide.with(context).load(R.drawable.powerpoint_icon).apply(requestOptions)
                    .dontTransform().into(fileType_IV)

                fileType_TV.text = "PPTX"
                fileType?.type = "application/vnd.ms-powerpoint"
            }
            MessageType.DocumentMicrosoftExcel -> {
                Glide.with(context).load(R.drawable.excel_icon).apply(requestOptions)
                    .dontTransform().into(fileType_IV)

                fileType_TV.text = "XLSX"
                fileType?.type = "application/vnd.ms-excel"
            }
            MessageType.DocumentAppleKeynote -> {
                Glide.with(context).load(R.drawable.keynote_icon).apply(requestOptions)
                    .dontTransform().into(fileType_IV)

                fileType_TV.text = "KEY"
                fileType?.type = "application/*"
            }
            else -> {
                Glide.with(context).load(R.drawable.generic_file_icon).apply(requestOptions)
                    .dontTransform().into(fileType_IV)

                fileType_TV.text = "TXT"
                fileType?.type = "*/*"
            }
        }
    }

    fun IntRange.random() =
        Random().nextInt((endInclusive + 1) - start) + start

    fun sendTyping() = viewModelScope.launch(Dispatchers.IO) {
        if (chatTypeRef is BBContact) {
            (chatTypeRef as BBContact).sendTyping()
        } else {
            (chatTypeRef as BBGroup).sendTyping()
        }
    }

    fun setAutoDeleteMessages(timer: ChatAutoDeleteTimer) = viewModelScope.launch(Dispatchers.IO) {
        chatTypeRef.setAutoDeleteMessages(timer)
    }

    fun fetchAutoDeleteTimer() = viewModelScope.launch(Dispatchers.IO) {
        chatTypeRef.fetchAutoDeleteTimer()
    }

    fun refreshMembersList() = viewModelScope.launch(Dispatchers.IO) {
        if (chatTypeRef is BBGroup) {
            (chatTypeRef as BBGroup).refreshMembersListAsync()
        }
    }

    fun refreshInfo() {
        viewModelScope.launch {
            if (chatTypeRef is BBContact) {
                (chatTypeRef as BBContact).refreshInfo()
            }
        }
    }

    fun registerScreenshotObserver(context: Context) {
//        val path1 =  context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + "/Screenshot"
        val path2 = "Screenshots"
        val handlerThread = HandlerThread("content_observer")
        handlerThread.start()
        val handler: Handler = object : Handler(handlerThread.looper) {
            override fun handleMessage(msg: android.os.Message) {
                super.handleMessage(msg)
            }
        }
        contentObserver = object : ContentObserver(handler) {
            override fun deliverSelfNotifications(): Boolean {
                Log.i("screenShot", "deliverSelfNotifications")
                return super.deliverSelfNotifications()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {


                Log.i("screenShot", "onChange $uri")
                if (context != null) {
                    uri?.let { uri ->
                        var path = CommonUtils.getPathFromUri(context, uri)
                        if (path == lastScreenshotUri) {
                            return
                        }
                        if (path?.contains(path2)!!) {
                            Log.i("screenShot", "path = $path")
                            lastScreenshotUri = path
                            var pathArr = Array<String>(1) { path }
                            viewModelScope.launch {
                                if (!path.contains(".pending")) {
                                    var uriArr = CommonUtils.pickPhoto(context, pathArr)
                                    Log.i("screenShot", uriArr[0])
                                    if (uriArr[0].contains(".jpg")) {
                                        send_file(uriArr.toTypedArray(), "alert:#screenshot")
                                    } else {
                                        sendMessage(
                                            chatTypeRef,
                                            "alert:#screenrecording",
                                            Pair("", "")
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
                super.onChange(selfChange, uri)
            }
        }
        (context as AppCompatActivity).getContentResolver()?.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true, contentObserver
        )
        (context as AppCompatActivity).getContentResolver()?.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true, contentObserver
        )
    }

    fun sendChatAlertAsync(alert: AlertType, message: Message) {
        var msgType = ""
        var msgContent = ""
        when (message.type) {
            MessageType.Audio -> {
                msgType = "audio"
                msgContent = "Audio"
            }
            MessageType.Text -> {
                msgType = "text"
                msgContent = message.body
            }
            MessageType.Photo -> {
                msgType = "photo"
                msgContent = "Photo"
            }
            MessageType.Video -> {
                msgType = "video"
                msgContent = "Video"
            }
            MessageType.Location -> {
                msgType = "location"
                msgContent = "Location"
            }
            MessageType.Contact -> {
                msgType = "contact"
                msgContent = "Contact"
            }
            else -> {
                msgType = "document"
                msgContent = "Document"
            }
        }
        var alertMessage = ""
        alertMessage = when (alert) {
            AlertType.MessageCopied -> {
                "alert:#copy:#${message.sender}:#${message.ID}:#${msgType}:#${msgContent}"
            }
            AlertType.MessagesForwarded -> {
                "alert:#forward:#${message.sender}:#${message.ID}:#${msgType}:#${msgContent}"
            }
        }
        sendMessage(prevChatTypeRef, alertMessage, Pair("", ""))
    }

    fun unregisterScreenshotObserver(context: Context) {
        (context as AppCompatActivity).getContentResolver()
            ?.unregisterContentObserver(contentObserver)
    }

    suspend fun retrieveOldMessagesToGetMessage(msgId: String): Boolean =
        withContext(Dispatchers.Main) {
            val toId = chatTypeRef.messages.filterIsInstance<MessageItem>()
                .lastOrNull()?.message?.ID?.toInt()?.minus(1).toString()
            chatTypeRef.fetchMessages(fromId = msgId, toId = toId, limit = 0).isNotEmpty()
        }

    suspend fun retrieveStarredMessages() = withContext(Dispatchers.Main) {
        val toId =
            chatTypeRef.messages.filterIsInstance<MessageItem>().lastOrNull()?.message?.ID?.toInt()
                ?.minus(1).toString()
        chatTypeRef.fetchStarredMessages().isNotEmpty()

    }

    fun checkMessagesNotExceedLimit() = GlobalScope.launch {

        if (chatTypeRef.messages.size > 300) {
            chatTypeRef.chatStatusType = ChatStatusType.PreviousMsg
            chatTypeRef.messages.subList(301, chatTypeRef.messages.size).clear()
            if (chatTypeRef.messages.last() is MessageItem) {
                chatTypeRef.messages.add(DateItem((chatTypeRef.messages.last() as MessageItem).message.dateSent!!))
            }
        }
    }


    fun getStarredMessages(recipentId: String) = viewModelScope.launch(Dispatchers.IO) {
        Blackbox.fetchStarredMessages(recipentId)
    }

}


data class DatePosition(
    var date: String,
    var position: Int,
    val realDate: Date,
    var msgsNumber: Int
) {

}