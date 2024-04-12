package com.ec.bond.activity.ui.chatbrowsing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.text.bold
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.MessageBody.Companion.convertStringToMessageBody
import com.ec.bond.activity.ui.chatbrowsing.MessageBody.Companion.getFullString
import com.ec.bond.activity.ui.chatbrowsing.protocols.IChatBrowsingListener
import com.ec.bond.activity.ui.chatbrowsing.protocols.ISearchChat
import com.ec.bond.activity.ui.chatbrowsing.textTimeMessage.ImFlexboxLayout
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.*
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.CommonUtils.emojiCount
import com.ec.bond.utils.CommonUtils.getMediaDuration
import com.ec.bond.utils.CommonUtils.getTimeString
import com.ec.bond.utils.CommonUtils.showKeybord
import com.ec.bond.utils.TimeStyle
import com.ec.bond.utils.timeString
import kotlinx.android.synthetic.main.item_auto_delete_message.view.*
import kotlinx.android.synthetic.main.item_date_chat_browsing.view.*
import kotlinx.coroutines.*
import java.io.File

class ChatBrowsingAdapter(
        val context: Context,
        private val chatBrowsingViewModel: ChatBrowsingViewModel,
        val chatBrowsingFragment: ChatBrowsingFragment,
        val ichatBrowsingListener: IChatBrowsingListener,

        ): RecyclerView.Adapter<RecyclerView.ViewHolder>(), Handler.Callback, ISearchChat {
    val TYPE_DATE = 0
    val TYPE_MESSAGE_OUTGOING = 1
    val TYPE_MESSAGE_INCOMING = 2
    val TYPE_DELETED_MESSAGE_OUTGOING = 3
    val TYPE_DELETED_MESSAGE_INCOMING = 4
    val TYPE_PHOTO_MESSAGE_OUTGOING = 5
    val TYPE_PHOTO_MESSAGE_INCOMING = 6
    val TYPE_VIDEO_MESSAGE_OUTGOING = 7
    val TYPE_VIDEO_MESSAGE_INCOMING = 8
    val TYPE_Document_MESSAGE_OUTGOING = 9
    val TYPE_Document_MESSAGE_INCOMING = 10
    val TYPE_Audio_MESSAGE_OUTGOING = 11
    val TYPE_Audio_MESSAGE_INCOMING = 12
    val VIDEO_TYPE = 13
    val Document_TYPE = 14
    val Audio_TYPE = 15
    val PHOTO_TYPE = 16
    val TYPE_SYSTEM = 17
    val TYPE_SYSTEM_MISSED_CALL = 18
    val TYPE_SYSTEM_AUTO_DELETE = 19
    val TYPE_SYSTEM_SCREENSHOT = 20
    val TYPE_SYSTEM_SCREENRECORDING = 21
    val TYPE_SYSTEM_ALERT_OUTGOING = 22
    val TYPE_SYSTEM_ALERT_INCOMING = 23
    val TYPE_TEMPORARY_DELETE = 24
    var searchIndex = -1
    var lastSearchIndex = -1
    var replyIndex = -1
    var mediaPlayer: MediaPlayer? = null
    var playingHolder: AudioMessageViewHolder? = null
    private var uiUpdateHandler: Handler? = Handler(Looper.getMainLooper(),this)
    private val MSG_UPDATE_SEEK_BAR = 1845
    private var playingPosition = -1
    private var isSameSection = false
    private var layoutInflater: LayoutInflater? = null

    private var isSearch = false
    private var searchWord = ""
    private var charFiltered = ArrayList<Char>()

    private var isSearchedBKColorEnabled = false
    private var timerForSearchBackground = object: CountDownTimer(2000, 2000) {
        override fun onTick(millisUntilFinished: Long) {

        }
        override fun onFinish() {
            if (replyIndex != -1) {
                notifyDataSetChanged()
                replyIndex = -1

            } else {
                notifyItemChanged(searchIndex)
            }
            isSearchedBKColorEnabled = false
        }
    }


    private var currentPosition: Int = 0

    val waterMark by lazy {
        CommonUtils.waterMarkText(Blackbox.account.registeredNumber
                ?: "", "51e3eb37471db46a4c4f9472deb594d4a56ceae0a163728aa45b6a06ed1d43cb")
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        var view: View
        return when (viewType) {
            TYPE_MESSAGE_OUTGOING -> {
                view = layoutInflater!!.inflate(R.layout.item_messsage_outgoing, parent, false)
                TextMessageViewHolder(view)
            }
            TYPE_MESSAGE_INCOMING -> {
                view = layoutInflater!!.inflate(R.layout.item_messsage_incoming, parent, false)
                TextMessageViewHolder(view)
            }
            TYPE_PHOTO_MESSAGE_OUTGOING -> {
                view = layoutInflater!!.inflate(R.layout.item_photo_messsage_outgoing, parent, false)
                PhotoMessageViewHolder(view)
            }
            TYPE_PHOTO_MESSAGE_INCOMING -> {
                view = layoutInflater!!.inflate(R.layout.item_photo_messsage_incoming, parent, false)
                PhotoMessageViewHolder(view)
            }
            TYPE_VIDEO_MESSAGE_OUTGOING -> {
                view = layoutInflater!!.inflate(R.layout.item_photo_messsage_outgoing, parent, false)
                VideoMessageViewHolder(view)
            }
            TYPE_VIDEO_MESSAGE_INCOMING -> {
                view = layoutInflater!!.inflate(R.layout.item_photo_messsage_incoming, parent, false)
                VideoMessageViewHolder(view)
            }
            TYPE_DELETED_MESSAGE_OUTGOING -> {
                view = layoutInflater!!.inflate(R.layout.item_deleted_message_outgoing, parent, false)
                DeletedMessageViewHolder(view)
            }
            TYPE_DELETED_MESSAGE_INCOMING -> {
                view = layoutInflater!!.inflate(R.layout.item_deleted_message_incoming, parent, false)
                DeletedMessageViewHolder(view)
            }
            TYPE_Document_MESSAGE_OUTGOING -> {
                view = layoutInflater!!.inflate(R.layout.item_document_messsage_outgoing, parent, false)
                DocumentMessageViewHolder(view)
            }
            TYPE_Document_MESSAGE_INCOMING -> {
                view = layoutInflater!!.inflate(R.layout.item_document_messsage_incoming, parent, false)
                DocumentMessageViewHolder(view)
            }
            TYPE_Audio_MESSAGE_OUTGOING -> {
                view = layoutInflater!!.inflate(R.layout.item_audio_messsage_outgoing, parent, false)
                AudioMessageViewHolder(view)
            }
            TYPE_Audio_MESSAGE_INCOMING -> {
                view = layoutInflater!!.inflate(R.layout.item_audio_messsage_incoming, parent, false)
                AudioMessageViewHolder(view)
            }
            TYPE_SYSTEM -> {
                view = layoutInflater!!.inflate(R.layout.item_date_chat_browsing, parent, false)
                SystemMessagesSecViewHolder(view)
            }
            TYPE_SYSTEM_MISSED_CALL -> {
                view = layoutInflater!!.inflate(R.layout.item_missed_call_chat_browsing, parent, false)
                SystemMissedCallMessagesSecViewHolder(view)
            }
            TYPE_SYSTEM_AUTO_DELETE -> {
                view = layoutInflater!!.inflate(R.layout.item_auto_delete_message, parent, false)
                SystemAutoDeleteMessageSecViewHolder(view)
            }
            TYPE_SYSTEM_SCREENSHOT -> {
                view = layoutInflater!!.inflate(R.layout.item_message_screenshot, parent, false)
                SystemScreenshotMessageSecViewHolder(view)
            }
            TYPE_SYSTEM_SCREENRECORDING -> {
                view = layoutInflater!!.inflate(R.layout.item_message_screenshot, parent, false)
                SystemScreenRecordingMessageSecViewHolder(view)
            } TYPE_SYSTEM_ALERT_OUTGOING -> {
                view = layoutInflater!!.inflate(R.layout.item_alert_copy_forward_messsage_outgoing, parent, false)
                SystemAlertCopyMessageSecViewHolder(view)
            }  TYPE_SYSTEM_ALERT_INCOMING -> {
                view = layoutInflater!!.inflate(R.layout.item_alert_copy_forward_messsage_incoming, parent, false)
                SystemAlertCopyMessageSecViewHolder(view)
            }  TYPE_TEMPORARY_DELETE -> {
                view = layoutInflater!!.inflate(R.layout.item_auto_delete_message, parent, false)
                SystemTemporaryDeleteMessageSecViewHolder(view)
            } else -> {
                view = layoutInflater!!.inflate(R.layout.item_date_chat_browsing, parent, false)
                DateSecViewHolder(view)
            }
        }
    }

    override fun getItemCount(): Int = chatBrowsingViewModel.chatTypeRef.messages.size
    override fun getItemViewType(position: Int): Int {
        if (chatBrowsingViewModel.chatTypeRef.messages[position].getType() == TYPE_DATE){
            return TYPE_DATE
        } else if ((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type == MessageType.System) {
            return TYPE_SYSTEM
        } else if ((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type == MessageType.SystemMissedAudioCall || (chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type == MessageType.SystemMissedVideoCall) {
            return TYPE_SYSTEM_MISSED_CALL
        } else if ((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type == MessageType.SystemAutoDelete) {
            return TYPE_SYSTEM_AUTO_DELETE
        } else if ((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type == MessageType.AlertScreenshot) {
            return TYPE_SYSTEM_SCREENSHOT
        } else if ((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type == MessageType.AlertScreenRecording) {
            return TYPE_SYSTEM_SCREENRECORDING
        } else if ((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type == MessageType.SystemTemporaryChat) {
            return TYPE_TEMPORARY_DELETE
        }
        else{

            return when((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.isOutgoing){
                true -> {
                    when((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type) {
                        MessageType.Deleted -> TYPE_DELETED_MESSAGE_OUTGOING
                        MessageType.Photo -> TYPE_PHOTO_MESSAGE_OUTGOING
                        MessageType.Video -> TYPE_VIDEO_MESSAGE_OUTGOING
                        MessageType.DocumentAppleKeynote -> TYPE_Document_MESSAGE_OUTGOING
                        MessageType.DocumentApplePages -> TYPE_Document_MESSAGE_OUTGOING
                        MessageType.DocumentAppleNumbers -> TYPE_Document_MESSAGE_OUTGOING
                        MessageType.DocumentMicrosoftExcel -> TYPE_Document_MESSAGE_OUTGOING
                        MessageType.DocumentMicrosoftPowerPoint -> TYPE_Document_MESSAGE_OUTGOING
                        MessageType.DocumentMicrosoftWord -> TYPE_Document_MESSAGE_OUTGOING
                        MessageType.DocumentPDF -> TYPE_Document_MESSAGE_OUTGOING
                        MessageType.DocumentGeneric -> TYPE_Document_MESSAGE_OUTGOING
                        MessageType.Audio -> TYPE_Audio_MESSAGE_OUTGOING
                        MessageType.AlertCopy -> TYPE_SYSTEM_ALERT_OUTGOING
                        MessageType.AlertForward -> TYPE_SYSTEM_ALERT_OUTGOING
                        else -> TYPE_MESSAGE_OUTGOING
                    }
                }
                else -> {
                    when((chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.type) {
                        MessageType.Deleted -> TYPE_DELETED_MESSAGE_INCOMING
                        MessageType.Photo -> TYPE_PHOTO_MESSAGE_INCOMING
                        MessageType.Video -> TYPE_VIDEO_MESSAGE_INCOMING
                        MessageType.DocumentAppleKeynote -> TYPE_Document_MESSAGE_INCOMING
                        MessageType.DocumentApplePages -> TYPE_Document_MESSAGE_INCOMING
                        MessageType.DocumentAppleNumbers -> TYPE_Document_MESSAGE_INCOMING
                        MessageType.DocumentMicrosoftExcel -> TYPE_Document_MESSAGE_INCOMING
                        MessageType.DocumentMicrosoftPowerPoint -> TYPE_Document_MESSAGE_INCOMING
                        MessageType.DocumentMicrosoftWord -> TYPE_Document_MESSAGE_INCOMING
                        MessageType.DocumentPDF -> TYPE_Document_MESSAGE_INCOMING
                        MessageType.DocumentGeneric -> TYPE_Document_MESSAGE_INCOMING
                        MessageType.Audio -> TYPE_Audio_MESSAGE_INCOMING
                        MessageType.AlertCopy -> TYPE_SYSTEM_ALERT_INCOMING
                        MessageType.AlertForward -> TYPE_SYSTEM_ALERT_INCOMING
                        else -> TYPE_MESSAGE_INCOMING
                    }
                }
            }

        }

    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ChatBrowsingAdapter.GeneralMessageViewHolder) {
            holder.bind(position)
        }
        else if (holder is ChatBrowsingAdapter.DateSecViewHolder) {
            holder.bind(position)
        }
        else if (holder is ChatBrowsingAdapter.DeletedMessageViewHolder) {
            holder.bind(position)
        } else if(holder is ChatBrowsingAdapter.SystemMessagesSecViewHolder){
            holder.bind(position)
        } else if (holder is ChatBrowsingAdapter.SystemAutoDeleteMessageSecViewHolder) {
            holder.bind(position)
        } else if (holder is ChatBrowsingAdapter.SystemScreenshotMessageSecViewHolder) {
            holder.bind(position)
        } else if (holder is ChatBrowsingAdapter.SystemScreenRecordingMessageSecViewHolder) {
            holder.bind(position)
        } else if (holder is ChatBrowsingAdapter.SystemMissedCallMessagesSecViewHolder) {
            holder.bind(position)
        } else if (holder is ChatBrowsingAdapter.SystemAlertCopyMessageSecViewHolder) {
            holder.bind(position)
        } else if (holder is ChatBrowsingAdapter.SystemTemporaryDeleteMessageSecViewHolder) {
            holder.bind(position)
        }

    }

    open inner class GeneralMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var message_relativeLayout = itemView.findViewById<RelativeLayout>(R.id.message_relativeLayout)
        internal var messageFrame_relativeLayout = itemView.findViewById<RelativeLayout>(R.id.messageFrame_relativeLayout)
        internal var message_arrow = itemView.findViewById<RelativeLayout>(R.id.message_arrow)
        internal var starred_message_imageView = itemView.findViewById<ImageView>(R.id.starred_message_imageView)
        internal var chatTV = itemView.findViewById<TextView>(R.id.chatTV)
        internal var timeTV = itemView.findViewById<TextView>(R.id.timeTV)
        internal var chatIV = itemView.findViewById<LottieAnimationView>(R.id.chatIV)
        internal var messageStatusIV = itemView.findViewById<ImageView>(R.id.messageStatusIV)

        internal var messageFooter_autoDelete_CL = itemView.findViewById<ConstraintLayout>(R.id.messageFooter_autoDelete_CL)
        internal var autoDelete_imageView = itemView.findViewById<ImageView>(R.id.autoDelete_imageView)
        internal var starred_message_imageView_autoDelete = itemView.findViewById<ImageView>(R.id.starred_message_imageView_autoDelete)
        internal var timeTV_autoDelete = itemView.findViewById<TextView>(R.id.timeTV_autoDelete)
        internal var messageStatusIV_autoDelete = itemView.findViewById<ImageView>(R.id.messageStatusIV_autoDelete)
        internal var messageFooter = itemView.findViewById<LinearLayout>(R.id.messageFooter)
        internal val group_layout = itemView.findViewById<RelativeLayout>(R.id.group_layout)
        internal var detailBody = ArrayList<MessageBody>()
        internal lateinit var realBody: SpannableString


        internal lateinit var messageItem: MessageItem
        open fun bind(position: Int) {
            currentPosition = position
            starred_message_imageView.visibility = View.GONE

            if (chatBrowsingViewModel.chatTypeRef.messages[position] is MessageItem){
                messageItem = chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem
                detailBody = convertStringToMessageBody(messageItem.message.body)
                realBody = if (detailBody.size > 0) {
                    getFullString(detailBody[0].text, detailBody.filter { it.startPosition != -1 })
                } else {
                    SpannableString(messageItem.message.body)
                }
                if (position < chatBrowsingViewModel.chatTypeRef.messages.size-1) {
                    if (group_layout != null) {
                        group_layout.visibility = View.GONE
                    }
                    if (chatBrowsingViewModel.chatTypeRef.messages[position + 1].getType() != TYPE_DATE && (chatBrowsingViewModel.chatTypeRef.messages[position + 1] as MessageItem).message.sender == messageItem.message.sender == true && (chatBrowsingViewModel.chatTypeRef.messages[position + 1] as MessageItem).message.type.isSystemMessage != true) {
                        message_arrow.visibility = View.INVISIBLE
                        if (messageItem.message.isOutgoing)
                            messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_right_fullrounded)
                        else {
                            messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_left_fullrounded)
                        }
                        message_relativeLayout.setTopMargin(0)
                    } else {
                        if (Blackbox.account.registeredNumber != messageItem.message.sender) {
                            checkGroup(itemView, messageItem.message.sender, group_layout)
                        }
                        message_relativeLayout.setTopMargin(10)
                        message_arrow.visibility = View.VISIBLE
                        if (messageItem.message.isOutgoing)
                            messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_right)
                        else {
                            messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_left)
                        }
                    }
                }


                if (chatBrowsingViewModel.getSelectedMessages().any { it.message.ID == (chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.ID }) {
                    itemView.setBackgroundResource(R.color.blue_selected_message)
                }
                else{
                    itemView.setBackgroundResource(R.drawable.flag_transparent)
                }


                var spannableString = SpannableString(realBody)
                val color: Int = Color.parseColor("#FFFF00")
                if (searchIndex == position && isSearch && charFiltered.size > 0) {
                    if (isSearchedBKColorEnabled) {
                        itemView.setBackgroundResource(R.color.blue_selected_message)
                    } else {
                        itemView.setBackgroundResource(R.drawable.flag_transparent)
                    }
                    var selectedIndex = ArrayList<Int>()
                    var foundWords = findWordIndeces(realBody.toString().toLowerCase(),searchWord.toLowerCase())
                    for (word in foundWords) {
                        spannableString.setSpan(BackgroundColorSpan(color), word.firstPosition, word.lastPosition, 0);
                        chatTV.text = buildBackgroundColorSpan(spannableString, realBody, searchWord, color)
                    }
                    if (foundWords.isEmpty()) {
                        chatTV.text = realBody
                    }

                }else {
                    chatTV.text = realBody

                }
                if (replyIndex == position) {
                    if (isSearchedBKColorEnabled) {
                        itemView.setBackgroundResource(R.color.blue_selected_message)
                    } else {
                        itemView.setBackgroundResource(R.drawable.flag_transparent)
                    }
                }
                timeTV.text = messageItem.message.dateSent?.timeString(TimeStyle.short)

                if (messageItem.message.isOutgoing)
                    Glide.with(context).load(getStatusImageType(messageItem.message.checkmarkType.value)).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(messageStatusIV)


                if (messageItem.message.isStarred.value == true){
                    starred_message_imageView.visibility = View.VISIBLE
                }
                itemView.setOnClickListener { it: View? ->

                    if (chatBrowsingViewModel.isLongPressed.value!!){
                        handleClickOnView(position,itemView)
                    }
                    else {
                        //handle onClick only
                    }
                }
                itemView.setOnLongClickListener {
                    return@setOnLongClickListener handleLongPress(position,it)
                }

            }

        }
        open fun buildBackgroundColorSpan(spannableString: SpannableString,
                                          text: SpannableString, searchString: String, color: Int): SpannableString? {
            val indexOf = text.toString().toUpperCase().indexOf(searchString.toUpperCase())
            try {
                spannableString.setSpan(BackgroundColorSpan(color), indexOf,
                indexOf + searchString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } catch (e: java.lang.Exception) {
            }
            return spannableString
        }
    }

    open inner class TextMessageViewHolder(itemView: View) : GeneralMessageViewHolder(itemView) {
        internal var fixWidthTime_TV = itemView.findViewById<TextView>(R.id.fixWidthTime_TV)
        internal var fixWidth_TV = itemView.findViewById<TextView>(R.id.fixWidth_TV)
        internal var outInMessage_replyPart_ConstraintLayout = itemView.findViewById<ConstraintLayout>(R.id.outInMessage_replyPart_ConstraintLayout)
        internal var outIn_replyOwnerMsgName_Txt = itemView.findViewById<TextView>(R.id.outIn_replyOwnerMsgName_Txt)
        internal var outInMsg_lineReplyDecoration = itemView.findViewById<View>(R.id.outInMsg_lineReplyDecoration)
        internal var outIn_replyOwnerMsg_Txt = itemView.findViewById<TextView>(R.id.outIn_replyOwnerMsg_Txt)
        internal var mediaReply_imageView = itemView.findViewById<ImageView>(R.id.outIn_mediaReply_imageView)
        internal var mediaTypeReply_imageView = itemView.findViewById<ImageView>(R.id.outIn_mediaTypeReply_imageView)


        override fun bind(position: Int) {
            super.bind(position)
            outInMessage_replyPart_ConstraintLayout.visibility = View.GONE

            fixWidth_TV.text = chatTV.text
            fixWidthTime_TV.text = timeTV.text
            if (messageItem.message.isStarred.value == true){
                fixWidth_TV.text = "${fixWidth_TV.text}Sta"
            }
            val count = chatTV.text.toString().trim().emojiCount()
            val emojiCount = count[0]
            val nonEmojiCount = count[1]
//            Log.i("count","emojiCount = ${emojiCount}")
//            Log.i("count","nonEmojiCount = ${nonEmojiCount}")

            if (emojiCount in 0..3 && nonEmojiCount == 0) {
                var min = 0f
                if (emojiCount > 1) {
                    min = emojiCount * 2f
                }
                chatTV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28F - min)
                fixWidth_TV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28F - min)
            } else {
                chatTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                fixWidth_TV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
            }

            if (messageItem.message.repliedToMsgId != "0"){
                outInMessage_replyPart_ConstraintLayout.setOnClickListener {
                    searchForSpecificMessage(messageItem.message.repliedToMsgId)
                }
                outInMessage_replyPart_ConstraintLayout.visibility = View.VISIBLE
                var originalMsg = chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>().firstOrNull() { it.message.ID == messageItem.message.repliedToMsgId }?.message

                if (originalMsg?.sender == Blackbox.account.registeredNumber){
                    outIn_replyOwnerMsgName_Txt.text = (context as Activity).getString(R.string.you)
                    outIn_replyOwnerMsgName_Txt.setTextColor(ContextCompat.getColor(context, R.color.whatsapp_color))
                    outInMsg_lineReplyDecoration.setBackgroundResource(R.drawable.rounded_reply_view_decoration_outgoing)
                }
                else {
                    outIn_replyOwnerMsgName_Txt.text = Blackbox.getContact(originalMsg?.sender ?: "")?.name
                    outIn_replyOwnerMsgName_Txt.setTextColor(ContextCompat.getColor(context, R.color.purple))
                    outInMsg_lineReplyDecoration.setBackgroundResource(R.drawable.rounded_reply_view_decoration_incoming)
                }
                val detailReply = convertStringToMessageBody(messageItem.message.repliedToText)
                if (detailReply.size > 0) {
                    outIn_replyOwnerMsg_Txt.text = getFullString(detailReply[0].text, detailReply.filter { it.startPosition != -1 })
                } else {
                    outIn_replyOwnerMsg_Txt.text = messageItem.message.repliedToText
                }
                if (originalMsg?.type == MessageType.Photo) {
                    fixWidth_TV.text = context.getString(R.string.long_text)
                    chatBrowsingViewModel.replyForPhotos(context,originalMsg,mediaReply_imageView,mediaTypeReply_imageView,outIn_replyOwnerMsg_Txt)
                } else if (originalMsg?.type == MessageType.Video) {
                    fixWidth_TV.text = context.getString(R.string.long_text)
                    chatBrowsingViewModel.replyForVideos(context,originalMsg,mediaReply_imageView,mediaTypeReply_imageView,outIn_replyOwnerMsg_Txt)

                } else if(originalMsg?.type?.isDocumentMessage == true) {
                    fixWidth_TV.text = context.getString(R.string.long_text)
                    chatBrowsingViewModel.replyForDocuments(context,originalMsg.body,mediaTypeReply_imageView,outIn_replyOwnerMsg_Txt)
                } else{
                    if (messageItem.message.repliedToText.length > realBody.toString().length) {
                        fixWidth_TV.text = messageItem.message.repliedToText
                    } else {
                        fixWidth_TV.text = realBody
                    }
                    mediaReply_imageView.visibility = View.GONE
                    mediaTypeReply_imageView.visibility = View.GONE
                }
            }
            if (chatBrowsingViewModel.chatTypeRef.autoDeleteTimer.value != ChatAutoDeleteTimer.Never) {
                messageFooter.visibility = View.GONE
                messageFooter_autoDelete_CL.visibility = View.VISIBLE
                starred_message_imageView_autoDelete.visibility = starred_message_imageView.visibility
                timeTV_autoDelete.text = timeTV.text
                if (messageItem.message.isOutgoing)
                    Glide.with(context).load(getStatusImageType(messageItem.message.checkmarkType.value)).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(messageStatusIV_autoDelete)

            } else {
                messageFooter.visibility = View.VISIBLE
                messageFooter_autoDelete_CL.visibility = View.GONE
            }
        }
    }

    open inner class PhotoMessageViewHolder(itemView: View) : GeneralMessageViewHolder(itemView) {
        internal var progressPercentage_TV = itemView.findViewById<TextView>(R.id.progressPercentage_TV)
        internal var timeTV_Photo = itemView.findViewById<TextView>(R.id.timeTV_Photo)
        internal var starred_message_imageView_Photo = itemView.findViewById<ImageView>(R.id.starred_message_imageView_Photo)
        internal var messageStatusIV_Photo = itemView.findViewById<ImageView>(R.id.messageStatusIV_Photo)
        internal var msg_flexLayout = itemView.findViewById<ImFlexboxLayout>(R.id.msg_flexLayout)
        internal var waterMark_TV = itemView.findViewById<TextView>(R.id.waterMark_TV)



        var imagePath = ""
        override fun bind(position: Int) {
            super.bind(position)
            if (messageItem.message.body.trim().isNotEmpty()) {
                msg_flexLayout.visibility = View.VISIBLE
                timeTV.visibility = View.GONE
                starred_message_imageView.visibility = View.GONE
                if (messageItem.message.isOutgoing) {
                    messageStatusIV.visibility = View.GONE
                }
                timeTV_Photo.text = timeTV.text
                if (messageItem.message.isStarred.value == true) {
                    starred_message_imageView_Photo.visibility = View.VISIBLE
                } else {
                    starred_message_imageView_Photo.visibility = View.GONE
                }
                messageStatusIV_Photo = messageStatusIV
                if (chatBrowsingViewModel.chatTypeRef.autoDeleteTimer.value != ChatAutoDeleteTimer.Never) {
                    messageFooter.visibility = View.GONE
                    messageFooter_autoDelete_CL.visibility = View.VISIBLE
                    starred_message_imageView_autoDelete.visibility = starred_message_imageView.visibility
                    timeTV_autoDelete.text = timeTV.text
                    if (messageItem.message.isOutgoing)
                        Glide.with(context).load(getStatusImageType(messageItem.message.checkmarkType.value)).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(messageStatusIV_autoDelete)
                } else {
                    messageFooter.visibility = View.VISIBLE
                    messageFooter_autoDelete_CL.visibility = View.GONE
                }
            }
            else {
                msg_flexLayout.visibility = View.GONE
                timeTV.visibility = View.VISIBLE
                if (messageItem.message.isStarred.value == true) {
                    starred_message_imageView.visibility = View.VISIBLE
                }
                if (messageItem.message.isOutgoing) {
                    messageStatusIV.visibility = View.VISIBLE
                    if (messageItem.message.checkmarkType.value == CheckmarkType.read) {
                        ImageViewCompat.setImageTintList(messageStatusIV, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.read_blue)));

                    } else {
                        ImageViewCompat.setImageTintList(messageStatusIV, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
                    }
                }
                messageFooter_autoDelete_CL.visibility = View.GONE
                if (chatBrowsingViewModel.chatTypeRef.autoDeleteTimer.value != ChatAutoDeleteTimer.Never) {
                    autoDelete_imageView.visibility = View.VISIBLE
                } else {
                    autoDelete_imageView.visibility = View.GONE
                }
            }

            timeTV.text = messageItem.message.dateSent?.timeString(TimeStyle.short)
            messageItem.message.localFileName.observe(itemView.context as LifecycleOwner) { filePath ->
                imagePath = filePath
                val file = File(imagePath)
                if (file.exists() && file.length() == messageItem.message.fileSize) {
                    Log.i("progress", " appear from first fun messageId = "+ messageItem.message.ID + "With File Path = " + filePath)

                    showImage(position)
                } else {
                    if (!messageItem.message.isInNetworkProgress) {
                        Log.i("progress", " start for messageId = "+ messageItem.message.ID)

                        handleDownloadStatus(position)
                    }
                }
            }

            if (messageItem.message.body.isEmpty()){
                chatTV.visibility = View.GONE
            } else {
                chatTV.visibility = View.VISIBLE
            }
            chatIV.setOnLongClickListener {
                return@setOnLongClickListener handleLongPress(position,itemView)
            }
        }

        private fun handleDownloadStatus(position: Int) {
            chatIV.setImageDrawable(null);
            chatIV.setAnimation(R.raw.image_loader);
            chatIV.scaleType = ImageView.ScaleType.CENTER_INSIDE
            chatIV.playAnimation()
            waterMark_TV.visibility = View.GONE
            progressPercentage_TV.visibility = View.VISIBLE
            updateDowloadStatus(messageItem.message, PHOTO_TYPE, progressPercentage_TV ,null,0) {
                Log.i("progress", " appear from second fun messageId = "+ messageItem.message.ID + "With File Path = " + imagePath)
                progressPercentage_TV.visibility = View.GONE

                showImage(position)
            }
        }
        fun showImage(position: Int) {
            waterMark_TV.text = waterMark
            waterMark_TV.visibility = View.GONE
            chatIV.scaleType = ImageView.ScaleType.CENTER_CROP
            Log.e("image_path---",imagePath)
            Glide.with((context as AppCompatActivity).applicationContext).load(imagePath).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(chatIV)
//            chatIV.apply {
//                transitionName = imagePath
//            }
            chatIV.setOnClickListener {
                if (!chatBrowsingViewModel.isLongPressed.value!!) {
                    if (File(imagePath).exists()) {
                        ichatBrowsingListener.setBigImageVisible(chatIV, true, messageItem.message.body, setContactName(messageItem.message.sender), imagePath)
                    } else {
                        Toast.makeText(context, R.string.try_again,Toast.LENGTH_SHORT).show()
                    }
//                    chatIV.transitionName = imagePath
//                    val extras = FragmentNavigatorExtras(
//                            chatIV to imagePath
//                    )
//                    val action = ChatBrowsingFragmentDirections.actionNavigationHomeToOpenImageFragment(imagePath, messageItem.message.body, true)
//                    NavHostFragment.findNavController(chatBrowsingFragment).navigate(action, extras)
                } else {
                    handleClickOnView(position,itemView)
                }
            }
        }
    }

    private fun handleClickOnView(position: Int,itemView: View) {
        chatBrowsingViewModel.getSelectedMessages().forEach {
            if (chatBrowsingViewModel.chatTypeRef.messages[position] is MessageItem){
                if(it.message.ID == (chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.ID) {
                    chatBrowsingViewModel.removeItemSelectedMessages(chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem)
                    itemView.setBackgroundResource(R.drawable.flag_transparent)
                    checkStarAction()
                    if (chatBrowsingViewModel.getSizeSelectedMessages() == 0){
                        chatBrowsingViewModel.setIsLongPressed(false,itemView.findViewById(R.id.messageFrame_relativeLayout))
                    }
                    else {
                        chatBrowsingViewModel.setLongPressedTitle(chatBrowsingViewModel.getSizeSelectedMessages().toString())
                    }
                    return
                }
            }
        }

        itemView.setBackgroundResource(R.color.blue_selected_message)
        addInSelectedList(position)
        checkStarAction()
    }

    private fun handleLongPress(position: Int,itemView: View): Boolean {
        return if (!chatBrowsingViewModel.isLongPressed.value!!) {

            addInSelectedList(position)
            chatBrowsingViewModel.setIsLongPressed(true,itemView.findViewById(R.id.messageFrame_relativeLayout))
            itemView.isSelected = true
            itemView.setBackgroundResource(R.color.blue_selected_message)
            ichatBrowsingListener.showBallonDialog(position,itemView.findViewById(R.id.messageFrame_relativeLayout))

            true
        }
        else{
            false
        }
    }
    /**
     * to Get File Status (Download / Upload)
     * params ->
     *          message -> this is message that contains file
     *          fileType -> To know the type of file to handle progress animation as Photo and Video have different one compared
     *                      with Document and Audio
     *          progressPercentagTV -> this reference of progress Percentage
     *          loadingProgressbar -> this reference of progress Animation
     */
    private fun updateDowloadStatus(message: Message, fileType: Int, progressPercentagTV: TextView? , loadingProgressbar: ProgressBar?,counter: Int,handleAction: () -> Unit) = GlobalScope.launch {
        message.isInNetworkProgress = true
        var count: Int = counter
        val progress= waitForDownload(message)
        Log.i("progressuuuu ",progress.toString() + "messageId = "+ message.ID+"=="+fileType)
        when {
            progress == 100 -> {
                delay(250)
                message.isInNetworkProgress = false
                Log.i("progress",progress.toString() + "messageId = "+ message.ID + "Finished")
                launch(Dispatchers.Main) {
                    handleAction()
                }
            }
            progress < 0 -> {

                delay(500)
                count += 1
                if (counter < 60) {
                    callUpdateDowloadStatusAgain(message, fileType, progressPercentagTV, loadingProgressbar,count, handleAction)
                } else {
                    message.isInNetworkProgress = false
                }
            }
            else -> {
                launch(Dispatchers.Main) {
                    if (fileType == PHOTO_TYPE || fileType == VIDEO_TYPE) {
                        progressPercentagTV?.text = "${progress.toFloat()}%"
                    } else {
                        loadingProgressbar?.progress = progress
                    }
                }
                delay(250)
                callUpdateDowloadStatusAgain(message,fileType,progressPercentagTV,loadingProgressbar,count,handleAction)
            }

        }
    }

    /**
     * to open image in image View Fragment
     * params ->
     *          path -> full path of image on device
     *          imageView -> image reference to handle animation in open
     *          msgBody -> to send body to image View Fragment
     */
    private fun handleImageClick(path: String?, imageView: ImageView?,msgBody: String, sender: String) {
        if (imageView != null) {
            if (File(path).exists()) {
                ichatBrowsingListener.setBigImageVisible(imageView, false, msgBody, setContactName(sender), path)
            } else {
                Toast.makeText(context, R.string.try_again,Toast.LENGTH_SHORT).show()
            }
        }
//        imageView?.transitionName = "VID"
//        val extras = FragmentNavigatorExtras(
//                imageView!! to "VID"
//        )
//        val action = ChatBrowsingFragmentDirections.actionNavigationHomeToOpenImageFragment(path!!,msgBody,false)
//        NavHostFragment.findNavController(chatBrowsingFragment).navigate(action,extras)


    }
    /**
     * to open document for every type pdf, word, excell, ... etc
     * params ->
     *      path -> document Path on device
     *      fname -> file name
     *      outInMessage_documentPart_ConstraintLayout -> reference of parent view
     *      type -> to know type of file like pdf, word, excell, ... etc
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleDocumentAction(path: String, fname: String, outInMessage_documentPart_ConstraintLayout: ConstraintLayout,type: String) {
        outInMessage_documentPart_ConstraintLayout.setOnClickListener {
            Log.i("documentPath",path)
            try {
                val data: Uri = FileProvider.getUriForFile(context, "com.spe2eeapp.masmak.fileprovider", File(path));
                context.grantUriPermission(context.getPackageName(), data, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(data, type)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } catch (ex: Exception) {
                Toast.makeText(context,context.getString(R.string.try_again),Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun copyFileWithWaterMarks() {

    }
//    /**
//     * to get video duration in form 00:00
//     * params ->
//     *      path -> video Path on device
//     *      videodurationTv -> reference of Text view to bind data on it
//     */
//    private fun getVideoDuration(path: String?, videodurationTv: TextView?) {
//        var mp = MediaPlayer.create(context, Uri.parse(path))
//        if (mp != null) {
//            val duration = mp.duration
//            mp.release()
//            videodurationTv?.text = String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration.toLong()), TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration.toLong())))
//        }
//    }

    /**
     * this function help to call updateDowloadStatus again for update status every 250ms
     * params ->
     *      message,
     *      fileType,
     *      progressPercentagTV -> to update progress
     */
    private fun callUpdateDowloadStatusAgain(message: Message, fileType: Int,progressPercentagTV: TextView?, loadingProgressbar: ProgressBar?,counter: Int,handleAction: () -> Unit) {
        updateDowloadStatus(message, fileType,progressPercentagTV, loadingProgressbar,counter,handleAction)
    }

    /**
     * suspend function to call updateFileTransferState() function
     * params -> message
     */
    private suspend fun waitForDownload(message: Message): Int = withContext(Dispatchers.IO) {
        return@withContext Blackbox.updateFileTransferState(message)
    }


    /**
     * view holder for Video Message
     */
    open inner class VideoMessageViewHolder(itemView: View) : GeneralMessageViewHolder(itemView) {
        internal var videoSection_constrainLayout = itemView.findViewById<ConstraintLayout>(R.id.videoSection_constrainLayout)
        internal var videoDuration_TV = itemView.findViewById<TextView>(R.id.videoDuration_TV)
        internal var progressPercentage_TV = itemView.findViewById<TextView>(R.id.progressPercentage_TV)
        internal var videoTypeIcon_IV = itemView.findViewById<ImageView>(R.id.videoTypeIcon_IV)
        internal var play_IV = itemView.findViewById<ImageView>(R.id.play_IV)
        internal var timeTV_Photo = itemView.findViewById<TextView>(R.id.timeTV_Photo)
        internal var starred_message_imageView_Photo = itemView.findViewById<ImageView>(R.id.starred_message_imageView_Photo)
        internal var messageStatusIV_Photo = itemView.findViewById<ImageView>(R.id.messageStatusIV_Photo)
        internal var msg_flexLayout = itemView.findViewById<ImFlexboxLayout>(R.id.msg_flexLayout)
        internal var waterMark_TV = itemView.findViewById<TextView>(R.id.waterMark_TV)

        internal var path: String? = ""
        override fun bind(position: Int) {
            super.bind(position)
            if (messageItem.message.body.trim().isNotEmpty()) {
                msg_flexLayout.visibility = View.VISIBLE
                timeTV.visibility = View.GONE
                starred_message_imageView.visibility = View.GONE
                if (messageItem.message.isOutgoing) {
                    messageStatusIV.visibility = View.GONE
                }
                timeTV_Photo.text = timeTV.text
                if (messageItem.message.isStarred.value == true) {
                    starred_message_imageView_Photo.visibility = View.VISIBLE
                } else {
                    starred_message_imageView_Photo.visibility = View.GONE
                }
                messageStatusIV_Photo = messageStatusIV

                autoDelete_imageView.visibility = View.GONE
                if (chatBrowsingViewModel.chatTypeRef.autoDeleteTimer.value != ChatAutoDeleteTimer.Never) {
                    messageFooter.visibility = View.GONE
                    messageFooter_autoDelete_CL.visibility = View.VISIBLE
                    starred_message_imageView_autoDelete.visibility = starred_message_imageView.visibility
                    timeTV_autoDelete.text = timeTV.text
                    if (messageItem.message.isOutgoing)
                        Glide.with(context).load(getStatusImageType(messageItem.message.checkmarkType.value)).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(messageStatusIV_autoDelete)

                } else {
                    messageFooter.visibility = View.VISIBLE
                    messageFooter_autoDelete_CL.visibility = View.GONE
                }
            } else {
                msg_flexLayout.visibility = View.GONE
                timeTV.visibility = View.VISIBLE
                if (messageItem.message.isStarred.value == true) {
                    starred_message_imageView.visibility = View.VISIBLE
                }
                if (messageItem.message.isOutgoing) {
                    messageStatusIV.visibility = View.VISIBLE
                    if (messageItem.message.checkmarkType.value == CheckmarkType.read) {
                        ImageViewCompat.setImageTintList(messageStatusIV, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.read_blue)));

                    } else{
                        ImageViewCompat.setImageTintList(messageStatusIV, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
                    }
                }

                messageFooter_autoDelete_CL.visibility = View.GONE
                if (chatBrowsingViewModel.chatTypeRef.autoDeleteTimer.value != ChatAutoDeleteTimer.Never) {
                    autoDelete_imageView.visibility = View.VISIBLE
                } else {
                    autoDelete_imageView.visibility = View.GONE
                }
            }
            videoSection_constrainLayout.visibility = View.VISIBLE

            messageItem.message.localFileName.observe(itemView.context as LifecycleOwner) { filePath ->
                path = filePath
                val file = File(filePath)
                if (file.exists() && file.length() == messageItem.message.fileSize) {
                    showVideo(position)
                } else {
                    if (!messageItem.message.isInNetworkProgress) {
                        Log.i("progress", " start for messageId = "+ messageItem.message.ID)

                        handleDownloadStatus(position)
                    }
                }
            }

//            chatIV.apply {
//                transitionName = "VID"
//            }

            if (messageItem.message.body.isEmpty()){
                chatTV.visibility = View.GONE

            } else {
                chatTV.visibility = View.VISIBLE
            }
            chatIV.visibility = View.VISIBLE

            chatIV.setOnLongClickListener {
                return@setOnLongClickListener handleLongPress(position,itemView)
            }
        }

        private fun handleDownloadStatus(position: Int) {
            videoDuration_TV?.visibility = View.GONE
            videoTypeIcon_IV?.visibility = View.GONE
            play_IV.visibility = View.GONE

            chatIV.setImageDrawable(null);
            chatIV.setAnimation(R.raw.image_loader);

            chatIV.scaleType = ImageView.ScaleType.CENTER_INSIDE
            chatIV.playAnimation()
            waterMark_TV.visibility = View.GONE

            progressPercentage_TV?.visibility = View.VISIBLE
            updateDowloadStatus(messageItem.message,VIDEO_TYPE,progressPercentage_TV,null,0) {
                showVideo(position)
            }
        }

        private fun showVideo(position: Int) {
            waterMark_TV.text = waterMark
            waterMark_TV.visibility = View.GONE

            play_IV.visibility = View.VISIBLE

            chatIV.pauseAnimation()
            videoDuration_TV?.visibility = View.VISIBLE
            videoTypeIcon_IV?.visibility = View.VISIBLE
            chatIV.scaleType = ImageView.ScaleType.CENTER_CROP
            // to get first picture to make it as thumbnail
            Glide.with((context as AppCompatActivity).applicationContext).load(path).thumbnail(0.1f).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(chatIV)

            chatIV?.setOnClickListener {
                if (!chatBrowsingViewModel.isLongPressed.value!!) {
                    handleImageClick(path, chatIV, messageItem.message.body, messageItem.message.sender)
                } else {
                    handleClickOnView(position,itemView)
                }
            }
            videoDuration_TV.text = getMediaDuration(context, path)
            progressPercentage_TV?.visibility = View.GONE

        }
    }

    /**
     * view holder for Document Message
     */
    open inner class DocumentMessageViewHolder(itemView: View): GeneralMessageViewHolder(itemView) {
        internal var fileType_TV = itemView.findViewById<TextView>(R.id.fileType_TV)
        internal var fileType_IV = itemView.findViewById<ImageView>(R.id.fileType_IV)
        internal var loading_CV = itemView.findViewById<CardView>(R.id.loading_CV)
        internal var loading_progressBar = itemView.findViewById<ProgressBar>(R.id.loading_progressBar)
        internal var outInMessage_documentPart_ConstraintLayout = itemView.findViewById<ConstraintLayout>(R.id.outInMessage_documentPart_ConstraintLayout)
        internal var fileType = FileType()
        @SuppressLint("UseCompatLoadingForDrawables")
        override fun bind(position: Int) {
            super.bind(position)
            chatTV.text = messageItem.message.originFileName
            chatBrowsingViewModel.drawDocumentUI(messageItem.message.type,context,fileType_IV,fileType_TV,fileType)

            if (chatBrowsingViewModel.chatTypeRef.autoDeleteTimer.value != ChatAutoDeleteTimer.Never) {
                autoDelete_imageView.visibility = View.VISIBLE
            } else {
                autoDelete_imageView.visibility = View.GONE
            }

            messageItem.message.localFileName.observe(itemView.context as LifecycleOwner) { filePath ->
//                path = filePath
                val file = File(filePath)
                if (file.exists() && file.length() == messageItem.message.fileSize) {
                    loading_CV?.visibility = View.GONE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        handleDocumentAction(filePath,messageItem.message.fileName,outInMessage_documentPart_ConstraintLayout,fileType.type)
                    }
                } else {
                    if (!messageItem.message.isInNetworkProgress) {
                        Log.i("progress", " start for messageId = "+ messageItem.message.ID)

                        handleDownloadStatus(filePath)
                    }
                }
            }
        }
        private fun handleDownloadStatus(path: String) {
            loading_CV?.visibility = View.VISIBLE
            updateDowloadStatus(messageItem.message,Document_TYPE,null,loading_progressBar,0) {
                showDocument(path)
            }
        }

        private fun showDocument(path: String) {
            loading_CV?.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                handleDocumentAction(path,messageItem.message.fileName,outInMessage_documentPart_ConstraintLayout,fileType.type)
            }
        }

        inner class FileType {
            internal var type = ""
        }

    }

    /**
     * view holder for Audio Message
     */
    open inner class AudioMessageViewHolder(itemView: View) : GeneralMessageViewHolder(itemView) {
        internal var audioDuration_TV = itemView.findViewById<TextView>(R.id.audioDuration_TV)
        internal var loading_CV = itemView.findViewById<CardView>(R.id.loading_CV)
        internal var loading_progressBar = itemView.findViewById<ProgressBar>(R.id.loading_progressBar)
        internal var play_IV = itemView.findViewById<ImageView>(R.id.play_IV)
        internal var audioSender_IV = itemView.findViewById<ImageView>(R.id.audioSender_IV)
        internal var audioSeekBar = itemView.findViewById<SeekBar>(R.id.audioSeekBar)


        internal var path: String? = ""
        override fun bind(position: Int) {
            super.bind(position)
            var photoUserPath: String? = null
            photoUserPath = if (messageItem.message.isOutgoing) {
                Blackbox.getDocumentsDir(context) + "/" + Blackbox.account.photoProfilePath.value
            } else {
                chatBrowsingViewModel.chatTypeRef.chatImagePath.value
            }
            Glide.with(context).load(if (photoUserPath == null || photoUserPath.isEmpty()) R.drawable.contact else photoUserPath).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(audioSender_IV)

            if (chatBrowsingViewModel.chatTypeRef.autoDeleteTimer.value != ChatAutoDeleteTimer.Never) {
                autoDelete_imageView.visibility = View.VISIBLE
            } else {
                autoDelete_imageView.visibility = View.INVISIBLE
            }
            // to check if file downloaded / uploaded or not
            messageItem.message.localFileName.observe(itemView.context as LifecycleOwner, Observer { filePath ->
                path = filePath
                val file = File(filePath)
                if (file.exists() && file.length() == messageItem.message.fileSize) {
                    Log.i("progress", " showAudio() in file.exists() && file.length() == messageItem.message.fileSize")

                    showAudio(position)
                } else {
                    if (!messageItem.message.isInNetworkProgress) {
                        Log.i("progress", " start for messageId = "+ messageItem.message.ID)
                        handleDownloadStatus(position)
                    }
                }
            })
        }
        /**
         * Download / upload file
         */
        private fun handleDownloadStatus(position: Int) {
            audioDuration_TV?.visibility = View.GONE
            play_IV.visibility = View.INVISIBLE


            loading_CV?.visibility = View.VISIBLE
            updateDowloadStatus(messageItem.message,Audio_TYPE,null,loading_progressBar,0) {
                Log.i("progress", " showAudio() in closure")

                showAudio(position)
            }
        }

        private fun showAudio(position: Int) {
            play_IV.visibility = View.VISIBLE

            audioDuration_TV?.visibility = View.VISIBLE

            handleAudioClick(position)

            audioDuration_TV.text = getMediaDuration(context, path)
            loading_CV?.visibility = View.GONE

        }

        private fun handleAudioClick(position: Int) {
            if (position == playingPosition) {
                playingHolder = this
                // this view holder corresponds to the currently playing audio cell
                // update its view to show playing progress
                updatePlayingView()
            } else {
                // and this one corresponds to non playing
                updateNonPlayingView()
            }
            audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(SeekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress)
                    }
                    Log.i("audioplay","onProgressChanged")
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    Log.i("audioplay","onStartTrackingTouch")
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    Log.i("audioplay","onStopTrackingTouch")
                    mediaPlayer?.seekTo(p0?.progress!!)
                    messageItem.message.audioTimer = p0?.progress!!
                }

            })
            play_IV?.setOnClickListener {
                if (adapterPosition == playingPosition) {
                    // toggle between play/pause of audio
                    if (mediaPlayer?.isPlaying!!) {
                        mediaPlayer?.pause()
                    } else {
                        mediaPlayer?.start()
                    }
                } else {
                    // start another audio playback
                    playingPosition = adapterPosition
                    if (mediaPlayer != null) {
                        if (null != playingHolder) {
                            updateNonPlayingView()
                        }
                        mediaPlayer!!.release()
                    }
                    playingHolder = this
                    startMediaPlayer()
                }
                updatePlayingView()
            }
        }

        fun startMediaPlayer() {
            mediaPlayer = MediaPlayer.create(context, path?.toUri())
            mediaPlayer?.setOnCompletionListener(OnCompletionListener { releaseMediaPlayer() })
            mediaPlayer?.start()
        }
        /**
         * update its view to show playing progress
         */
        fun updatePlayingView() {

            playingHolder!!.audioSeekBar.max = mediaPlayer?.duration!!
            playingHolder!!.audioSeekBar.progress = mediaPlayer?.currentPosition!!
//            playingHolder!!.audioSeekBar.isEnabled = true
            if (mediaPlayer?.isPlaying!!) {
                uiUpdateHandler!!.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 100)
                playingHolder!!.play_IV.animate().scaleXBy(1f).scaleYBy(1f).scaleXBy(0f).scaleYBy(0f).setDuration(200).setInterpolator(OvershootInterpolator()).start()

                playingHolder!!.play_IV.setImageResource(R.drawable.pause_ic)
                Log.i("audioplay","Play Audio")

            } else {
                uiUpdateHandler!!.removeMessages(MSG_UPDATE_SEEK_BAR)
                playingHolder?.play_IV?.animate()!!.scaleXBy(1f).scaleYBy(1f).scaleXBy(0f).scaleYBy(0f).setDuration(200).setInterpolator(OvershootInterpolator()).start()

                playingHolder?.play_IV?.setImageResource(R.drawable.play_ic)
                Log.i("audioplay","pause Play")

            }
        }

        /**
         * to reset playing view to non playing view
         */
        fun updateNonPlayingView() {
            if (this === playingHolder) {
                uiUpdateHandler!!.removeMessages(MSG_UPDATE_SEEK_BAR)
                Log.i("audioplay","updateNonPlayingView when this === playingHolder")

            }
            Log.i("audioplay","updateNonPlayingView")

            playingHolder?.audioSeekBar?.progress = 0
            playingHolder?.play_IV?.setImageResource(R.drawable.play_ic)
        }


        /**
         * to release MediaPlayer of audio when audio track in Completed or start new One
         */
        fun releaseMediaPlayer() {
            Log.i("audioplay","releaseMediaPlayer()")

            if (null != playingHolder) {
                Log.i("audioplay","releaseMediaPlayer() when null != playingHolder")

                updateNonPlayingView()
            }
            mediaPlayer!!.release()
            mediaPlayer = null
            playingPosition = -1
        }
    }

    /**
     * view holder for Deleted Message
     */
    open inner class DeletedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var message_relativeLayout = itemView.findViewById<RelativeLayout>(R.id.message_relativeLayout)
        internal var messageFrame_relativeLayout = itemView.findViewById<RelativeLayout>(R.id.messageFrame_relativeLayout)
        internal var message_arrow = itemView.findViewById<RelativeLayout>(R.id.message_arrow)
        internal val group_layout = itemView.findViewById<RelativeLayout>(R.id.group_layout)

        internal var timeTV = itemView.findViewById<TextView>(R.id.timeTV)
        open fun bind(position: Int) {
            if (chatBrowsingViewModel.chatTypeRef.messages[position] is MessageItem) {
                var messageItem = chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem
                if (position < chatBrowsingViewModel.chatTypeRef.messages.size-1) {
                    if (group_layout != null) {
                        group_layout.visibility = View.GONE
                    }
                    if (chatBrowsingViewModel.chatTypeRef.messages[position + 1].getType() != TYPE_DATE && (chatBrowsingViewModel.chatTypeRef.messages[position + 1] as MessageItem).message.sender == messageItem.message.sender) {
                        message_arrow.visibility = View.INVISIBLE
                        if (messageItem.message.isOutgoing)
                            messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_right_fullrounded)
                        else {
                            messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_left_fullrounded)
                        }
                        message_relativeLayout.setTopMargin(0)
                    } else {
                        if (Blackbox.account.registeredNumber != messageItem.message.sender) {
                            checkGroup(itemView, messageItem.message.sender, group_layout)
                        }
                        message_relativeLayout.setTopMargin(10)
                        message_arrow.visibility = View.VISIBLE
                        if (messageItem.message.isOutgoing)
                            messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_right)
                        else {
                            messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_left)
                        }
                    }
                }
                timeTV.text = messageItem.message.dateSent?.timeString(TimeStyle.short)
            }
        }
    }
    /**
     * to check if it's in Group or not to show specific cells and name who send every message section
     */
    private fun checkGroup(itemView: View, sender: String, group_layout: RelativeLayout) {
        if (chatBrowsingViewModel.chatTypeRef is BBGroup) {
//            val group_layout = itemView.findViewById<RelativeLayout>(R.id.group_layout)
            val contact_TV = itemView.findViewById<TextView>(R.id.contact_TV)
            group_layout.visibility = View.VISIBLE
            val contactName = setContactName(sender)
            contact_TV.text = contactName
            group_layout.setOnClickListener {
                chatBrowsingFragment.onContactGroupSelect(contactName, sender)
            }
        }
    }
    /**
     * params: sender -> number of the sender
     * get Contact name to appear it in group header message or in screenshot taken by specific contact
     */
    private fun setContactName(sender: String): String {
        val contact = Blackbox.getContact(sender)
        if (sender == Blackbox.account.registeredNumber) {
            return "You"
        }
        if (contact != null && contact?.name!!.isNotEmpty()) {
            return contact.name
        }
        return sender
    }

    /**
     * to update audio seekbar every 100 milii seconds to make it appear as move continous
     */
    override fun handleMessage(p0: android.os.Message): Boolean {
        when (p0.what) {
            MSG_UPDATE_SEEK_BAR -> {
                playingHolder?.audioSeekBar?.progress = mediaPlayer!!.currentPosition
                uiUpdateHandler!!.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 100)
                return true
            }
        }
        return false
    }

    /**
     * check if message is  a star message or not
     */
    private fun checkStarAction() {
        if (chatBrowsingViewModel.getSelectedMessages().any { it.message.isStarred.value == false }){
            chatBrowsingViewModel.menuActionBar.value?.findItem(R.id.action_starMsg)?.setIcon(R.drawable.star_icon);
            chatBrowsingViewModel.isStarredMessagesAction = true
        }
        else{
            chatBrowsingViewModel.menuActionBar.value?.findItem(R.id.action_starMsg)?.setIcon(R.drawable.unstar_white);
            chatBrowsingViewModel.isStarredMessagesAction = false
        }
    }

    /**
     * view holder for Date group section
     */
    open inner class DateSecViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bind(position: Int) {
            val date =  (chatBrowsingViewModel.chatTypeRef.messages[position] as DateItem).date
            itemView.date_section_txt.text = date.getTimeString()
        }
    }
    /**
     * view holder for AutoDelete Message
     */
    open inner class SystemAutoDeleteMessageSecViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bind(position: Int) {
            var text = (chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.body
            val textArr = text.split("*")
            if (textArr.size > 1) {
                val normalBefore = textArr[0]
                val normalBOLD = textArr[1]
                val normalAfter = textArr[2]
                val finalString = "$normalBefore $normalBOLD $normalAfter"
                /**
                 * use spannable to make part of text bold like "1 hour" and rest of text regular
                 */
                val sb: Spannable = SpannableString(finalString)
                sb.setSpan(StyleSpan(Typeface.BOLD), finalString.indexOf(normalBOLD), finalString.indexOf(normalBOLD) + normalBOLD.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //bold
                sb.setSpan(AbsoluteSizeSpan(Int.SIZE_BITS),finalString.indexOf(normalBOLD), finalString.indexOf(normalBOLD) + normalBOLD.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //resize size
                itemView.autoDelete_section_txt.text = sb
            } else {
                itemView.autoDelete_section_txt.text = text
            }
            itemView.autoDelete_layout.setOnClickListener {
                ichatBrowsingListener.showAlertDialog(R.layout.dialog_auto_delete)
            }
            itemView.time_IV.setImageResource(R.drawable.quick_timer)
        }
    }
    /**
     * view holder for Temporary Delete Message
     */
    open inner class SystemTemporaryDeleteMessageSecViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bind(position: Int) {
            itemView.autoDelete_section_txt.text = (chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message.body
            itemView.time_IV.setImageResource(R.drawable.temporary_delete)
        }
    }

    /**
     * view holder for Screenshot Message
     */
    open inner class SystemScreenshotMessageSecViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var time_TV = itemView.findViewById<TextView>(R.id.time_TV)
        internal var takenBy_TV = itemView.findViewById<TextView>(R.id.takenBy_TV)
        internal var screenshot_layout = itemView.findViewById<RelativeLayout>(R.id.screenshot_layout)
        internal var progressPercentage_TV = itemView.findViewById<TextView>(R.id.progressPercentage_TV)
        internal var preview_TV = itemView.findViewById<TextView>(R.id.preview_TV)
        internal var imageView = itemView.findViewById<ImageView>(R.id.imageView)
        internal lateinit var messageItem: MessageItem
        var imagePath = ""

        open fun bind(position: Int) {
            if (chatBrowsingViewModel.chatTypeRef.messages[position] is MessageItem) {
                messageItem = chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem
                time_TV.text = messageItem.message.dateSent?.timeString(TimeStyle.short)
                takenBy_TV.text = setContactName(messageItem.message.sender)

                messageItem.message.localFileName.observe(itemView.context as LifecycleOwner) { filePath ->
                    imagePath = filePath
                    val file = File(imagePath)
                    if (file.exists() && file.length() == messageItem.message.fileSize) {
                        Log.i("progress", " appear from first fun messageId = "+ messageItem.message.ID + "With File Path = " + filePath)

                        showImage()
                    } else {
                        if (!messageItem.message.isInNetworkProgress) {
                            Log.i("progress", " start for messageId = "+ messageItem.message.ID)

                            handleDownloadStatus()
                        }
                    }
                }
            }
        }

        /**
         * Download / upload file
         */
        private fun handleDownloadStatus() {

            progressPercentage_TV.visibility = View.VISIBLE
            preview_TV.text = "Click here to preview - ↓ "
            updateDowloadStatus(messageItem.message, PHOTO_TYPE, progressPercentage_TV ,null,0) {
                Log.i("progress", " appear from second fun messageId = "+ messageItem.message.ID + "With File Path = " + imagePath)
                progressPercentage_TV.visibility = View.GONE

                showImage()
            }
        }

        /**
         * to handle click event while message upload / download complete to sure that image exist in Directory
         */
        fun showImage() {
            progressPercentage_TV.visibility = View.GONE
            preview_TV.text = "Click here to preview"
            screenshot_layout.setOnClickListener {
                if (File(imagePath).exists()) {
                    Glide.with(context).load(imagePath).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(imageView)
                    ichatBrowsingListener.setBigImageVisible(imageView, true, "", setContactName(messageItem.message.sender), imagePath)
                } else {
                    Toast.makeText(context, R.string.try_again,Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * view holder for Screenrecording Message
     */
    open inner class SystemScreenRecordingMessageSecViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var time_TV = itemView.findViewById<TextView>(R.id.time_TV)
        internal var takenBy_TV = itemView.findViewById<TextView>(R.id.takenBy_TV)
        internal var takenConst_TV = itemView.findViewById<TextView>(R.id.takenConst_TV)
        internal var progress_layout = itemView.findViewById<LinearLayout>(R.id.progress_layout)
        internal var screenShot_TV = itemView.findViewById<TextView>(R.id.screenShot_TV)
        internal lateinit var messageItem: MessageItem

        open fun bind(position: Int) {
            if (chatBrowsingViewModel.chatTypeRef.messages[position] is MessageItem) {
                screenShot_TV.text = "RECORDING"
                messageItem = chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem
                time_TV.text = messageItem.message.dateSent?.timeString(TimeStyle.short)
                takenConst_TV.text = "Started by: "
                takenBy_TV.text = setContactName(messageItem.message.sender)
                progress_layout.visibility = View.GONE

            }
        }

    }
    /**
     * view holder for generic System Message
     */
    open inner class SystemMessagesSecViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal lateinit var messageItem: MessageItem
        open fun bind(position: Int) {
            if (chatBrowsingViewModel.chatTypeRef.messages[position] is MessageItem) {
                messageItem = chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem
                val text = messageItem.message.body
                val textArr = text.split("*")
                if (textArr.size > 1) {
                    val normalBOLD = textArr[1]
                    val normalAfter = textArr[2]
                    val finalString = "$normalBOLD $normalAfter"
                    /**
                     * use spannable to make part of text bold like "1 hour" and rest of text regular
                     */
                    val sb: Spannable = SpannableString(finalString)
                    sb.setSpan(StyleSpan(Typeface.BOLD), finalString.indexOf(normalBOLD), finalString.indexOf(normalBOLD) + normalBOLD.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //bold
                    sb.setSpan(StyleSpan(Typeface.NORMAL),finalString.indexOf(normalBOLD), finalString.indexOf(normalBOLD) + normalBOLD.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //resize size

                    itemView.date_section_txt.text = sb
                } else {
                    itemView.date_section_txt.text = text
                }
                val typeface = ResourcesCompat.getFont(context.applicationContext, R.font.font_regular)

                itemView.date_section_txt.setTypeface(typeface, Typeface.NORMAL)

            }
        }
    }

    /**
     * view holder for display Missed Call Cell
     */
    open inner class SystemMissedCallMessagesSecViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var missedCall_IV = itemView.findViewById<ImageView>(R.id.missedCall_IV)
        internal var missed_txt = itemView.findViewById<TextView>(R.id.missed_txt)
        internal lateinit var messageItem: MessageItem

        fun bind(position: Int) {
            if (chatBrowsingViewModel.chatTypeRef.messages[position] is MessageItem) {
                messageItem = chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem
                if (messageItem.message.type == MessageType.SystemMissedAudioCall){
                    Glide.with(context).load(R.drawable.missed_call_ic).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(missedCall_IV)
                } else {
                    Glide.with(context).load(R.drawable.missed_video_ic).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(missedCall_IV)
                }
                val text = messageItem.message.body.replace("\n"," at ")
                missed_txt.text = text
            }
        }
    }

    /**
     * view holder for display System Alert Copy Message Cell
     */
    open inner class SystemAlertCopyMessageSecViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var message_relativeLayout = itemView.findViewById<RelativeLayout>(R.id.message_relativeLayout)
        internal var messageFrame_relativeLayout = itemView.findViewById<RelativeLayout>(R.id.messageFrame_relativeLayout)
        internal var message_arrow = itemView.findViewById<RelativeLayout>(R.id.message_arrow)

        internal var chatTV = itemView.findViewById<TextView>(R.id.chatTV)
        internal var timeTV = itemView.findViewById<TextView>(R.id.timeTV)
        internal var fixWidthTime_TV = itemView.findViewById<TextView>(R.id.fixWidthTime_TV)
        internal var fixWidth_TV = itemView.findViewById<TextView>(R.id.fixWidth_TV)
        internal var outInMessage_replyPart_ConstraintLayout = itemView.findViewById<ConstraintLayout>(R.id.outInMessage_replyPart_ConstraintLayout)
        internal var outIn_replyOwnerMsgName_Txt = itemView.findViewById<TextView>(R.id.outIn_replyOwnerMsgName_Txt)
        internal var outInMsg_lineReplyDecoration = itemView.findViewById<View>(R.id.outInMsg_lineReplyDecoration)
        internal var outIn_replyOwnerMsg_Txt = itemView.findViewById<TextView>(R.id.outIn_replyOwnerMsg_Txt)
        internal var mediaReply_imageView = itemView.findViewById<ImageView>(R.id.outIn_mediaReply_imageView)
        internal var mediaTypeReply_imageView = itemView.findViewById<ImageView>(R.id.outIn_mediaTypeReply_imageView)
        internal val group_layout = itemView.findViewById<RelativeLayout>(R.id.group_layout)

        fun bind(position: Int) {
            var messageItem: MessageItem = chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem
            outInMessage_replyPart_ConstraintLayout.setOnClickListener {
                searchForSpecificMessage(messageItem.message.alertMsgIdRef ?: "0")
            }
            if (position < chatBrowsingViewModel.chatTypeRef.messages.size-1) {
                if (group_layout != null) {
                    group_layout.visibility = View.GONE
                }
                if (chatBrowsingViewModel.chatTypeRef.messages[position + 1].getType() != TYPE_DATE && (chatBrowsingViewModel.chatTypeRef.messages[position + 1] as? MessageItem)?.message?.isOutgoing == messageItem.message.isOutgoing) {
                    message_arrow.visibility = View.INVISIBLE
                    if (messageItem.message.type == MessageType.AlertCopy) {
                        messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_copy_fullrounded)
                    }
                    else {
                        messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_forward_fullrounded)
                    }
                    message_relativeLayout.setTopMargin(0)
                } else {
                    if (Blackbox.account.registeredNumber != messageItem.message.sender) {
                        checkGroup(itemView, messageItem.message.sender, group_layout)
                    }
                    message_relativeLayout.setTopMargin(10)
                    message_arrow.visibility = View.VISIBLE
                    if (messageItem.message.type == MessageType.AlertCopy) {
                        message_arrow.setBackgroundResource(R.drawable.v_bubble_corner_copy)
                        messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_copy)
                    }
                    else {
                        message_arrow.setBackgroundResource(R.drawable.v_bubble_corner_forward)
                        messageFrame_relativeLayout.setBackgroundResource(R.drawable.bg_msg_forward)
                    }
                }
            }
            if (messageItem.message.alertMsgSenderRef == Blackbox.account.registeredNumber){
                outIn_replyOwnerMsgName_Txt.text = (context as Activity).getString(R.string.you)
                outIn_replyOwnerMsgName_Txt.setTextColor(ContextCompat.getColor(context, R.color.whatsapp_color))
                outInMsg_lineReplyDecoration.setBackgroundResource(R.drawable.rounded_reply_view_decoration_outgoing)
            }
            else {
                outIn_replyOwnerMsgName_Txt.text = Blackbox.getContact(messageItem.message.alertMsgSenderRef ?: "")?.name
                outIn_replyOwnerMsgName_Txt.setTextColor(ContextCompat.getColor(context, R.color.purple))
                outInMsg_lineReplyDecoration.setBackgroundResource(R.drawable.rounded_reply_view_decoration_incoming)
            }
            outIn_replyOwnerMsg_Txt.text = messageItem.message.alertMsgContentRef

            if (messageItem.message.body.contains("alert:#copy")) {
                messageItem.message.body = "_*You* copied this message_"
            } else if (messageItem.message.body.contains("alert:#forward")){
                messageItem.message.body = "_*You* forwarded this message_"
            }
            val text = messageItem.message.body.replace("_", "")
            val textArr = text.split("*")
            if (textArr.size > 1) {
                val normalBOLD = textArr[1]
                val normalAfter = textArr[2]
                val s = SpannableStringBuilder()
                        .bold { append(normalBOLD) }
                        .append("$normalAfter ")
                chatTV.text = s
            } else {
                chatTV.text = text
            }



            timeTV.text = messageItem.message.dateSent?.timeString(TimeStyle.short)

            fixWidth_TV.text = chatTV.text
            fixWidthTime_TV.text = timeTV.text

        }
    }

    /**
     * return required icon for image status
     */

    private fun getStatusImageType(checkmarkType: CheckmarkType?): Int {
        //TODO need to confirm it
        return when (checkmarkType) {
            CheckmarkType.sent -> R.drawable.msg_single_checkmark_unread
            CheckmarkType.received -> R.drawable.msg_double_checkmark_unread
            CheckmarkType.read -> R.drawable.msg_double_checkmark_read
            else -> R.drawable.msg_unsent
        }

    }

    open fun RelativeLayout.setTopMargin(top: Int) {
        if (this.layoutParams is MarginLayoutParams) {
            val p = this.layoutParams as MarginLayoutParams
            p.topMargin = top
            this.requestLayout()
        }
    }

    /**
     * to add selected message in list to make actions on it like delete, reply, star/unstar , Forward or get message info
     */

    fun addInSelectedList(position: Int){
        chatBrowsingViewModel.addToSelectedMessages(chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem)
        chatBrowsingViewModel.setLongPressedTitle(chatBrowsingViewModel.getSizeSelectedMessages().toString())
    }

    override fun searchWithText(word: String, firstPosition: Int, lastPosition: Int) {
        isSearch = true
        searchWord = word
        charFiltered.clear()
        currentPosition = firstPosition
        for (i in firstPosition..lastPosition) {
            val message = (chatBrowsingViewModel.chatTypeRef.messages[i] as? MessageItem)?.message
            if (message != null) {
                if (message.body.toLowerCase().contains(word.toLowerCase()) && (message.type == MessageType.Text || message.type == MessageType.Photo || message.type == MessageType.Video)) {
                    searchIndex = i
                    break
                }
            }
        }
        searchWord.toCharArray().toCollection(charFiltered)
        if (searchIndex >= 0){

            notifyItemChanged(searchIndex)
            if (lastSearchIndex >= 0) {
                notifyItemChanged(lastSearchIndex)
            }
            lastSearchIndex = searchIndex

        }
    }

    override fun searchUpArrow(textView: TextView, firstPosition: Int, lastPosition: Int, recyclerView: RecyclerView) {
        isSearch = true
        searchWord = textView.text.toString()
        if (searchWord.trim().isNotEmpty()) {

            for (i in firstPosition..chatBrowsingViewModel.chatTypeRef.messages.size - 1) {
                val message = (chatBrowsingViewModel.chatTypeRef.messages[i] as? MessageItem)?.message
                if (message != null) {

                    if (message.body.toLowerCase().contains(searchWord.toLowerCase())) {
                        if (i > searchIndex && (message.type == MessageType.Text || message.type == MessageType.Photo || message.type == MessageType.Video)) {
                            searchIndex = i
                            break
                        }
                    }

                }
            }
            searchWord.toCharArray().toCollection(charFiltered)
            if (searchIndex == lastSearchIndex) {
                searchNotFound()
                isSearchedBKColorEnabled = true
                recyclerView.scrollToPosition(searchIndex)
                timerAppear(timerForSearchBackground)
                notifyItemChanged(searchIndex)
            } else if (searchIndex >= 0) {
                isSearchedBKColorEnabled = true
                timerAppear(timerForSearchBackground)
                recyclerView.scrollToPosition(searchIndex)
                notifyItemChanged(searchIndex)
                notifyItemChanged(lastSearchIndex)
                lastSearchIndex = searchIndex
            }
        }
    }

    override fun searchDownArrow(textView: TextView, lastPosition: Int, recyclerView: RecyclerView) {
        isSearch = true
        searchWord = textView.text.toString()
        if (searchWord.trim().isNotEmpty()) {
            for (i in chatBrowsingViewModel.chatTypeRef.messages.size - 1 downTo 0) {
                val message = (chatBrowsingViewModel.chatTypeRef.messages[i] as? MessageItem)?.message
                if (message != null) {

                    if (message.body.toLowerCase().contains(searchWord.toLowerCase())) {
                        if (searchIndex < 0) {
                            searchIndex = i
                            break
                        } else if (i < searchIndex && (message.type == MessageType.Text || message.type == MessageType.Photo || message.type == MessageType.Video)) {
                            searchIndex = i
                            break
                        }
                    }

                }
            }
            searchWord.toCharArray().toCollection(charFiltered)
            if (searchIndex == lastSearchIndex) {
                searchNotFound()
                isSearchedBKColorEnabled = true
                recyclerView.scrollToPosition(searchIndex)
                timerAppear(timerForSearchBackground)
                notifyItemChanged(searchIndex)
            } else if (searchIndex >= 0) {
                isSearchedBKColorEnabled = true
                timerAppear(timerForSearchBackground)
                recyclerView.scrollToPosition(searchIndex)
                notifyItemChanged(searchIndex)
                notifyItemChanged(lastSearchIndex)
                lastSearchIndex = searchIndex
//            notifyDataSetChanged()
            }
        }
    }

    private fun searchNotFound() {
        val toast = Toast.makeText(context,context.getText(R.string.not_found),Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
        showKeybord(context)
    }

    override fun cancelSearch() {
        isSearch = false
        searchWord = ""
        searchIndex = -1
        lastSearchIndex = -1
        notifyDataSetChanged()
    }
    fun timerAppear(timer: CountDownTimer) {
        Handler(Looper.getMainLooper()).post {
            timer.cancel()
            timer.start()
        }
    }
    fun findWordIndeces(str: String, findStr: String): List<SearchWord>  {
        var searchWords = ArrayList<SearchWord>()
        var lastIndex = 0
        var count = 0

        var tracker = 0

        while (lastIndex != -1) {
            lastIndex = str.indexOf(findStr, lastIndex)
            if (lastIndex != -1) {
                if (lastIndex == 0 || str[lastIndex - 1] == ' ') {
                    var searchWord = SearchWord(findStr, lastIndex, lastIndex + 1)
                    tracker = lastIndex + 1
                    count++
                    for (c in tracker until str.length) {
                        if (str[c] == ' ') {
                            searchWord.lastPosition = c
                            break
                        } else if (c == str.length - 1) {
                            searchWord.lastPosition = c+1
                        }
                    }
                    searchWords.add(searchWord)
                }
                lastIndex += findStr.length
            }
        }
        return searchWords
    }


    fun searchForSpecificMessage(msgId: String) {
        var message = chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>().firstOrNull {
            it.message.ID == msgId
        }
        message?.let {
            val messagePosition = chatBrowsingViewModel.chatTypeRef.messages.indexOf(it)
            ichatBrowsingListener.scrollToSpecificMessage(messagePosition)
            replyIndex = messagePosition
            notifyItemChanged(replyIndex)
            highlightMessage()
            return
        }

        ichatBrowsingListener.retrieveOldMessagesToGetMessage(msgId)

    }

    private fun highlightMessage() {
        isSearchedBKColorEnabled = true
        timerAppear(timerForSearchBackground)
    }



}
class SearchWord(
        val word: String,
        val firstPosition: Int,
        var lastPosition: Int
)