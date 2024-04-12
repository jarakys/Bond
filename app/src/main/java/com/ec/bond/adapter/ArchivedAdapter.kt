package com.ec.bond.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.ArchiveActivity.Companion.islongpress
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.ChatItem
import com.ec.bond.blackbox.model.CheckmarkType
import com.ec.bond.blackbox.model.Message
import com.ec.bond.utils.DateTimeUtils
import kotlinx.android.synthetic.main.item_chat_list_group.view.*
import kotlinx.android.synthetic.main.item_chat_list_person.view.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class ArchivedAdapter(private var context: Context,
                      private var arrContact: ArrayList<ChatItem>,
                      private var listner: ArchiveChatlistitemclick) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private val TYPE_PERSON = 1
    private val TYPE_GROUP = 2
    private var contactListFiltered: MutableList<ChatItem> = mutableListOf()
    init {
        contactListFiltered = arrContact
    }

    private var layoutInflater: LayoutInflater? = null

    inner class PersonTypeChatListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ChatItem?) {
            setProfileImage(itemView.contact_chatList_imageView, item?.contact?.getChatImagePath())
            setMessageTime(itemView.time_date_txt, item?.lastMessage?.dateSentString)
            setMessageDeliveredStatus(itemView.read_status_chatList_imageView, item?.lastMessage?.checkmarkType?.value) // TODO need check for type
            setLastMsgTypeImage(false, itemView.lastMsgType_chatList_imageView, itemView.lastMessage_chatList_txt, item?.lastMessage)
            setNoUnReadMessages(itemView.noUnReadMsgs_txt, itemView.time_date_txt, item?.lastMessage?.chatUnreadMessagesCount.toString())
            setChatItemName(itemView.contact_name_chatList_txt, item?.contact?.name)
            if(item!!.isSelected){
                itemView.item_selectedCheck_person.visibility = View.VISIBLE
                itemView.setBackgroundResource(R.color.light_gray_selection)
            }else{
                itemView.item_selectedCheck_person.visibility = View.INVISIBLE
                val outValue = TypedValue()
                context.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                itemView.setBackgroundResource(outValue.resourceId)
            }
            itemView.setOnClickListener {
                if(islongpress){
                    listner.onChatitemSelect(item,itemView.item_selectedCheck_person,itemView, islongpress)
                }else{
                    listner.onChatitemclick(item)
                }
            }
            itemView.setOnLongClickListener {
                listner.onChatitemSelect(item,itemView.item_selectedCheck_person,itemView, islongpress)
                return@setOnLongClickListener true
            }
        }
    }

    inner class GroupTypeChatListViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        fun bind(item: ChatItem?) {
            setProfileImage(itemView.group_chatList_imageView, item?.group?.getChatImagePath())
            setMessageTime(itemView.gtime_date_txt, item?.lastMessage?.dateSentString)
            setMessageDeliveredStatus(itemView.gRead_status_chatList_imageView, item?.lastMessage?.checkmarkType?.value)
            setLastMsgTypeImage(false, itemView.gLastMsgType_chatList_imageView, itemView.gLastMessage_chatList_txt, item?.lastMessage)
            setNoUnReadMessages(itemView.gnoUnReadMsgs_txt, itemView.gtime_date_txt, item?.lastMessage?.chatUnreadMessagesCount.toString())
            setChatItemName(itemView.group_name_chatList_txt, item?.group?.description?.value)
            setLastSendMessageName(itemView.gLastMessageName_chatList_txt, item?.lastMessage?.sender)
            if(item!!.isSelected){
                itemView.item_selectedCheck_group.visibility = View.VISIBLE
                itemView.setBackgroundResource(R.color.light_gray_selection)
            }else{
                itemView.item_selectedCheck_group.visibility = View.INVISIBLE
                val outValue = TypedValue()
                context.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                itemView.setBackgroundResource(outValue.resourceId)
            }
            itemView.setOnClickListener {
                if(islongpress){
                    listner.onChatitemSelect(contactListFiltered[adapterPosition],itemView.item_selectedCheck_group,itemView, islongpress)
                }else{
                    listner.onChatitemclick(item)
                }
            }
            itemView.setOnLongClickListener {
                listner.onChatitemSelect(contactListFiltered[adapterPosition],itemView.item_selectedCheck_group,itemView, islongpress)
                return@setOnLongClickListener true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view: View
        if (viewType == TYPE_GROUP) {
            view = layoutInflater!!.inflate(R.layout.item_chat_list_group, parent, false)
            return GroupTypeChatListViewHolder(view)
        }
        view = layoutInflater!!.inflate(R.layout.item_chat_list_person, parent, false)
        return PersonTypeChatListViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (contactListFiltered?.get(position)?.contact != null) {
            TYPE_PERSON
        }else{
            TYPE_GROUP
        }
    }
    override fun getItemCount(): Int {
        return contactListFiltered!!.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PersonTypeChatListViewHolder) {

            holder.bind(contactListFiltered?.get(position))

        } else if (holder is GroupTypeChatListViewHolder) {

            holder.bind(contactListFiltered?.get(position))

        }
    }

    fun setLongpress(longpress: Boolean){
        islongpress = longpress
    }
    fun unSelectItem(item: ChatItem){
        var index = contactListFiltered.indexOf(item)
        item.isSelected = false
        notifyItemChanged(index)
    }

    fun unselectandremoveitem(item: ChatItem?,position: Int){
        item!!.isSelected = false
        notifyItemChanged(position)
        contactListFiltered.remove(item)
        notifyItemRemoved(position)
    }

    // this method append time of last message on text view person / group depends on the reference i sent
    private fun setMessageTime(textView: TextView?, dateSentString: String?) {
        textView?.text = dateSentString?.getTimeString()
    }
    private fun setProfileImage(imageView: ImageView?, imagePath: String?, isGroup: Boolean = false) {
        if (!imagePath.isNullOrEmpty()) {
            imageView?.set(BitmapFactory.decodeFile(imagePath))
        } else {
            ContextCompat.getDrawable(
                    context,
                    if (isGroup) R.drawable.group_image else R.drawable.contact)?.let {
                imageView?.set(it)
            }
        }
    }

    /* this method show read and delivered status like whatsApp if I sent this message and call method 'getStatusImageType'
        but if this message send from other to me it will disappear the imageView
     */
    private fun setMessageDeliveredStatus(imageView: ImageView?, checkmarkType: CheckmarkType?) {
        if (checkmarkType != CheckmarkType.none)
            imageView?.set(getStatusImageType(checkmarkType)) // TODO need check for type
        else
            imageView?.visibility = View.GONE
    }

    /*
        if message send but not delivered to other person ->
            it will return 'R.drawable.msg_sent' and known from 'deliveredToServer' = 'false' flag
        but if message delivered only but not read yet ->
            it will return 'R.drawable.msg_delivered' and know from 'deliveredToServer' = 'true' flag
            and 'dateReadString' if default like "0000-00-00 00:00:00"
        third and last message read ->
            it will return 'R.drawable.msg_read' and know from 'deliveredToServer' = 'true' flag
            and 'dateReadString' if not like "0000-00-00 00:00:00"
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

    // this method get name if the sender in my contacts but if not return number otherwise return you
    private fun getSenderName(sender: String): String {
        if (sender == Blackbox.account.registeredNumber) {
            return "You"
        }
        val contact = Blackbox.getContact(sender) ?: Blackbox.getTemporaryContact(sender)
        ?: return ""
        return contact.name
    }

    // this method append Chat name on text view person / group depends on the reference i sent
    private fun setChatItemName(textView: TextView?, name: String?) {
        textView?.text = name
    }

    // this method get name of who send last message in group and call getSenderName to know it
    private fun setLastSendMessageName(textView: TextView?, sender: String?) {
        if (sender == null) return
        textView?.text = getSenderName(sender)
    }

    // this method show image type and content of last message if last message string it will disappear the imageView otherwise show it corresponding to its type
    private fun setLastMsgTypeImage(isGroup: Boolean, msgTypeImageView: ImageView, msgTypeText: TextView, message: Message?) {
        if (message?.dateDeleted != null) {
            msgTypeImageView.set(R.drawable.deletemessage_circle_gray)
//            if (message.isOutgoing) {
//                msgTypeText.text = context.getString(R.string.you_deleted_this_message)
//            }
//            else
//                msgTypeText.text = context.getString(R.string.this_message_was_deleted)
            msgTypeImageView.visibility = View.GONE
            return
        }
        when (message?.msgtype) {
            "file" -> {
                if (message?.body!!.contains(".m4a")) { // need to refactor it later after Blackbox message type done
                    msgTypeImageView.set(R.drawable.last_msg_photo)
                    msgTypeText.text = context.resources.getString(R.string.media_photo)
                } else {
                    msgTypeImageView.set(R.drawable.document_chatlist)
                    msgTypeText.text = message?.fileName
                }
            }
            "photo" -> {
                msgTypeImageView.set(R.drawable.last_msg_photo)
                msgTypeText.text = context.resources.getString(R.string.media_photo)
            }
            "video" -> {
                msgTypeImageView.set(R.drawable.video_chatlist)
                msgTypeText.text = context.resources.getString(R.string.media_video)
            }
            else -> {
                msgTypeImageView.visibility = View.GONE
                msgTypeText.text = message?.body
            }
            //TODO wait for more details about msgtype
        }

    }

    // this method check if number of unread messages = 0 hide it from page otherwise show it
    private fun setNoUnReadMessages(noUnReadMsgsTxt: TextView?, timeText: TextView?, chatUnreadMessagesCount: String?) {
        if (chatUnreadMessagesCount == "0")
            noUnReadMsgsTxt?.visibility = View.GONE
        else {
            noUnReadMsgsTxt?.visibility = View.VISIBLE
            noUnReadMsgsTxt?.text = chatUnreadMessagesCount
            timeText?.setTextColor(context.getColor(R.color.no_unreaded_msgs))
        }
    }

    /* this method get the time like whatsApp if message send in the same day ->
           this show time only in format 'h:mm a' without date
       but if message sent yesterday ->
            this show word yesterday only without mention the time
       but more than one day ->
            show date only in format d/MM/YY
     */
    fun String.getTimeString(): String {
        var response: String = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val date = LocalDate.parse(this, formatter)
            val noOfDaysDifference = date.compareTo(LocalDate.now())
            when (noOfDaysDifference) {
                0 -> response = LocalTime.parse(this, formatter).format(DateTimeFormatter.ofPattern("h:mm a"))
                -1 -> response = "Yesterday"
                -2 -> response = LocalDate.parse(this, formatter).format(DateTimeFormatter.ofPattern("d/MM/YY"))
            }
        } else {
            response = this
        }

        return response
    }

    fun chatlistisEmpty(): Boolean {
        return contactListFiltered.isEmpty()
    }

    interface ArchiveChatlistitemclick {
        fun onChatitemSelect(chatItem: ChatItem,checkview: ImageView,itemview: View,islongpress:Boolean)
        fun onChatitemclick(chatItem: ChatItem)
    }
}