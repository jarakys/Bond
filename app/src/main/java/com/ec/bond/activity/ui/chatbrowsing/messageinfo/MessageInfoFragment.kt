package com.ec.bond.activity.ui.chatbrowsing.messageinfo

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.ec.bond.R
import com.ec.bond.activity.IOnBackPressed
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.activity.ui.chatbrowsing.MessageItem
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.CheckmarkType
import com.ec.bond.blackbox.model.MessageType
import com.ec.bond.utils.CommonUtils.getMediaDuration
import com.ec.bond.utils.TimeStyle
import com.ec.bond.utils.timeString

import kotlinx.android.synthetic.main.fragment_message_info.*
import kotlinx.android.synthetic.main.fragment_message_info.view.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MessageInfoFragment: Fragment(), IOnBackPressed {
    lateinit var currentActivity: AppCompatActivity
    lateinit var root: View
    lateinit var outgoingInclude: View
    lateinit var inComingInclude: View
    lateinit var chatTV: TextView
    lateinit var timeTV: TextView
    lateinit var starImageView: ImageView
    lateinit var replyPartConstraintLayout: ConstraintLayout
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel
    lateinit var replyOwnerMsgName: TextView
    lateinit var lineReplyDecoration: View
    lateinit var replyOwnerMsg: TextView
    lateinit var messageStatusIV: ImageView
    lateinit var chatIV: ImageView
    lateinit var fixWidth_TV: TextView
    lateinit var fixWidthTime_TV: TextView
    lateinit var videoSection_constrainLayout: ConstraintLayout
    lateinit var videoDuration_TV: TextView
    lateinit var numberOfPages_TV: TextView
    lateinit var fileType_TV: TextView
    lateinit var fileType_IV: ImageView
    lateinit var audioDuration_TV: TextView
    lateinit var play_IV: ImageView
    lateinit var audioSender_IV: ImageView
    lateinit var audioSeekBar: SeekBar


    private val args: MessageInfoFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_message_info, container, false) as View

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActionBar()
        chatBrowsingViewModel = ViewModelProvider(requireActivity()).get(ChatBrowsingViewModel::class.java)
        if (args.message.isOutgoing){
            if (args.message.type == MessageType.Photo || args.message.type == MessageType.Video){
                initializeUIComponent(item_photo_messsage_outgoing_include)
            } else if (args.message.type.isDocumentMessage) {
                initializeUIComponent(item_document_messsage_outgoing_include)
            } else if (args.message.type == MessageType.Audio) {
                initializeUIComponent(item_audio_messsage_outgoing_include)
            } else {
                initializeUIComponent(outcoming_message_include)
            }
//            messageStatusIV.set(getStatusImageType(args.message.checkmarkType.value))
            Glide.with(requireContext()).load(getStatusImageType(args.message.checkmarkType.value)).apply(chatBrowsingViewModel.requestOptions).into(messageStatusIV)

        }
        else{
            if (args.message.type == MessageType.Photo || args.message.type == MessageType.Video){
                initializeUIComponent(item_photo_messsage_incoming_include)
            } else if (args.message.type.isDocumentMessage) {
                initializeUIComponent(item_document_messsage_incoming_include)
            } else if (args.message.type == MessageType.Audio) {
                initializeUIComponent(item_audio_messsage_incoming_include)
            } else {
                initializeUIComponent(incoming_message_include)
            }

        }
        chatTV.text = args.message.body
        timeTV.text = args.message.dateSent?.timeString(TimeStyle.short)

        if (args.message.isStarred.value == true) {
            starImageView.visibility = View.VISIBLE
        }

        if (args.message.type == MessageType.Photo || args.message.type == MessageType.Video) {
            var path = args.message.localFileName.value
            if (args.message.type == MessageType.Video) {
                Glide.with(requireContext()).load(path).thumbnail(0.1f).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(chatIV)

                videoSection_constrainLayout.visibility = View.VISIBLE
                videoDuration_TV.text = context?.let { getMediaDuration(it, path) }

            } else {
                Glide.with(requireContext()).load(path).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(chatIV)
            }

        } else if (args.message.type.isDocumentMessage){
            chatBrowsingViewModel.drawDocumentUI(args.message.type, requireContext(), fileType_IV, fileType_TV, null)

        } else if (args.message.type == MessageType.Audio) {


            var photoUserPath: String? = null
            if (args.message.isOutgoing) {
                photoUserPath = Blackbox.account.photoProfilePath.value
            } else {
                photoUserPath = chatBrowsingViewModel.chatTypeRef.chatImagePath.value
            }
            Glide.with(requireContext()).load(if (photoUserPath == null || photoUserPath.isEmpty()) R.drawable.contact else photoUserPath).apply(chatBrowsingViewModel.requestOptions).dontTransform().into(audioSender_IV)
            var path = args.message.localFileName.value
            audioDuration_TV.visibility = View.VISIBLE
            audioDuration_TV.text = context?.let { getMediaDuration(it, path) }
        }
        else if(args.message.type == MessageType.Text) {
            fixWidth_TV.text = chatTV.text
            fixWidthTime_TV.text = timeTV.text

            if (args.message.repliedToMsgId != "0") {
                replyPartConstraintLayout.visibility = View.VISIBLE
                var originalMsg = chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>().firstOrNull() { it.message.ID == args.message.repliedToMsgId }?.message

                if (originalMsg?.sender == Blackbox.account.registeredNumber){

                    replyOwnerMsgName.text = (context as Activity).getString(R.string.you)
                    replyOwnerMsgName.setTextColor(ContextCompat.getColor(requireContext(), R.color.whatsapp_color))
                    lineReplyDecoration.setBackgroundResource(R.drawable.rounded_reply_view_decoration_outgoing)
                }
                else {
                    replyOwnerMsgName.text = Blackbox.getContact(originalMsg?.sender ?: "")?.name
                    replyOwnerMsgName.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple))
                    lineReplyDecoration.setBackgroundResource(R.drawable.rounded_reply_view_decoration_incoming)
                }
                if (args.message.repliedToText.length > args.message.body.length) {
                    fixWidth_TV.text = args.message.repliedToText
                } else {
                    fixWidth_TV.text = args.message.body
                }
            }
        }
        if (args.message.dateRead != null)
            root.readDate_textView.text = args.message.dateReadString?.getTimeString()
        else {
            root.readDate_textView.text = "__"
        }
        if (args.message.dateReceived != null)
            root.deliveredDate_textView.text = args.message.dateReceivedString?.getTimeString()
        else {
            root.deliveredDate_textView.text = "__"
        }
    }

    private fun initializeUIComponent(viewRef: View) {
        viewRef.visibility = View.VISIBLE
        chatTV = viewRef.findViewById(R.id.chatTV)
        timeTV = viewRef.findViewById(R.id.timeTV)
        starImageView = viewRef.findViewById<ImageView>(R.id.starred_message_imageView)


        if (viewRef === outcoming_message_include || viewRef === item_photo_messsage_outgoing_include || viewRef === item_document_messsage_outgoing_include || viewRef === item_audio_messsage_outgoing_include)
            messageStatusIV = viewRef.findViewById(R.id.messageStatusIV)

        if (viewRef === item_photo_messsage_outgoing_include || viewRef === item_photo_messsage_incoming_include){
            chatIV = viewRef.findViewById(R.id.chatIV)
            videoSection_constrainLayout = viewRef.findViewById(R.id.videoSection_constrainLayout)
            videoDuration_TV = viewRef.findViewById(R.id.videoDuration_TV)
        } else if (args.message.type.isDocumentMessage){
            numberOfPages_TV = viewRef.findViewById(R.id.numberOfPages_TV)
            fileType_TV = viewRef.findViewById(R.id.fileType_TV)
            fileType_IV = viewRef.findViewById(R.id.fileType_IV)
        } else if (args.message.type == MessageType.Audio) {
            audioDuration_TV = viewRef.findViewById(R.id.audioDuration_TV)
            play_IV = viewRef.findViewById(R.id.play_IV)
            audioSender_IV = viewRef.findViewById(R.id.audioSender_IV)
            audioSeekBar = viewRef.findViewById(R.id.audioSeekBar)

        } else {
            replyPartConstraintLayout = viewRef.findViewById<ConstraintLayout>(R.id.outInMessage_replyPart_ConstraintLayout)
            replyOwnerMsgName = viewRef.findViewById<TextView>(R.id.outIn_replyOwnerMsgName_Txt)
            lineReplyDecoration = viewRef.findViewById<View>(R.id.outInMsg_lineReplyDecoration)
            replyOwnerMsg = viewRef.findViewById<TextView>(R.id.outIn_replyOwnerMsg_Txt)
            fixWidth_TV = viewRef.findViewById(R.id.fixWidth_TV)
            fixWidthTime_TV = viewRef.findViewById(R.id.fixWidthTime_TV)
        }
    }
    private fun getStatusImageType(checkmarkType: CheckmarkType?): Int {
        return when (checkmarkType) {
            CheckmarkType.sent -> R.drawable.msg_single_checkmark_unread
            CheckmarkType.received -> R.drawable.msg_double_checkmark_unread
            CheckmarkType.read -> R.drawable.msg_double_checkmark_read
            else -> R.drawable.msg_unsent
        }
    }
    fun String.getTimeString(): String {
        var response: String = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val date = LocalDate.parse(this, formatter)
            val noOfDaysDifference = date.compareTo(LocalDate.now())
            when (noOfDaysDifference){
//                0 -> response = LocalTime.parse(this,formatter).format(DateTimeFormatter.ofPattern("h:mm a"))
                0 -> response = "Today" + ", " + LocalTime.parse(this,formatter).format(DateTimeFormatter.ofPattern("h:mm a"))
                -1 -> response = "Yesterday" + ", " + LocalTime.parse(this,formatter).format(DateTimeFormatter.ofPattern("h:mm a"))
                else -> response = LocalDate.parse(this, formatter).format(DateTimeFormatter.ofPattern("MMMM dd")) + ", " + LocalTime.parse(this,formatter).format(DateTimeFormatter.ofPattern("h:mm a"))
            }
        } else {
            response = this
        }

        return response
    }
    override fun onBackPressed(): Boolean {
        NavHostFragment.findNavController(this).navigateUp()
        return true
    }
    private fun setupActionBar() {
        setHasOptionsMenu(true)
        currentActivity = activity as AppCompatActivity
        currentActivity.setSupportActionBar(toolbar_messageInfo)

        currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        currentActivity.supportActionBar?.setDisplayShowHomeEnabled(true);
        toolbar_messageInfo.setNavigationOnClickListener { onBackPressed() }
    }
}