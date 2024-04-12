package com.ec.bond.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.italic
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.ui.chat.ChatFragment.Companion.islongpress
import com.ec.bond.activity.ui.chatbrowsing.MessageBody
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.ChatItem
import com.ec.bond.blackbox.model.CheckmarkType
import com.ec.bond.blackbox.model.Message
import com.ec.bond.blackbox.model.MessageType
import com.ec.bond.utils.CommonUtils.getMediaDuration
import com.ec.bond.utils.CommonUtils.getTimeString
import com.ec.bond.utils.CommonUtils.getTimeString2
import kotlinx.android.synthetic.main.item_chat_list_group.view.*
import kotlinx.android.synthetic.main.item_chat_list_person.view.*
import java.util.*
import kotlin.collections.ArrayList


class ChatListAdapter(
        private var context: Context,
        private var arrContact: ArrayList<ChatItem>,
        private var listner: Chatlistitemclick
) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {


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
            setMessageTime(itemView.time_date_txt, item?.lastMessage?.dateSent)
            setMessageDeliveredStatus(itemView.read_status_chatList_imageView, item?.lastMessage?.checkmarkType?.value) // TODO need check for type
            setLastMsgTypeImage(false, itemView.lastMsgType_chatList_imageView, itemView.lastMessage_chatList_txt, item?.lastMessage)
            setNoUnReadMessages(itemView.noUnReadMsgs_txt, itemView.time_date_txt, item?.lastMessage?.chatUnreadMessagesCount.toString())
            setChatItemName(itemView.contact_name_chatList_txt, if (item?.contact?.name!!.isNotEmpty()) item.contact.name else item.contact.registeredNumber)
            if(item.isSelected){
                itemView.item_selectedCheck_person.visibility = View.VISIBLE
                itemView.setBackgroundResource(R.color.light_gray_selection)
            } else {
                itemView.item_selectedCheck_person.visibility = View.INVISIBLE
                val outValue = TypedValue()
                context.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                itemView.setBackgroundResource(outValue.resourceId)
            }
            itemView.setOnClickListener {
                if (islongpress) {
                    listner.onChatitemSelect(item, itemView.item_selectedCheck_person, itemView, islongpress)
                } else {
                    listner.onChatitemclick(item)
                }
            }
            itemView.setOnLongClickListener {
                listner.onChatitemSelect(item, itemView.item_selectedCheck_person, itemView, islongpress)
                return@setOnLongClickListener true
            }
        }
    }

    inner class GroupTypeChatListViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        fun bind(item: ChatItem?) {
            setProfileImage(itemView.group_chatList_imageView, item?.group?.getChatImagePath())
            setMessageTime(itemView.gtime_date_txt, item?.lastMessage?.dateSent)
            setMessageDeliveredStatus(itemView.gRead_status_chatList_imageView, item?.lastMessage?.checkmarkType?.value)
            setLastMsgTypeImage(false, itemView.gLastMsgType_chatList_imageView, itemView.gLastMessage_chatList_txt, item?.lastMessage)
            setNoUnReadMessages(itemView.gnoUnReadMsgs_txt, itemView.gtime_date_txt, item?.lastMessage?.chatUnreadMessagesCount.toString())
            setChatItemName(itemView.group_name_chatList_txt, item?.group?.description?.value)
            setLastSendMessageName(itemView.gLastMessageName_chatList_txt, item?.lastMessage?.sender)
            if (item!!.isSelected) {
                itemView.item_selectedCheck_group.visibility = View.VISIBLE
                itemView.setBackgroundResource(R.color.light_gray_selection)
            } else {
                itemView.item_selectedCheck_group.visibility = View.INVISIBLE
                val outValue = TypedValue()
                context.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                itemView.setBackgroundResource(outValue.resourceId)
            }
            itemView.setOnClickListener {
                if (islongpress) {
                    listner.onChatitemSelect(contactListFiltered[adapterPosition], itemView.item_selectedCheck_group, itemView, islongpress)
                } else {
                    listner.onChatitemclick(item)
                }
            }
            itemView.setOnLongClickListener {
                listner.onChatitemSelect(contactListFiltered[adapterPosition], itemView.item_selectedCheck_group, itemView, islongpress)
                return@setOnLongClickListener true
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (contactListFiltered?.get(position)?.contact != null) {
            TYPE_PERSON
        } else {
            TYPE_GROUP
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

    override fun getItemCount(): Int {
        return contactListFiltered.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is PersonTypeChatListViewHolder) {

            holder.bind(contactListFiltered.get(position))

        } else if (holder is GroupTypeChatListViewHolder) {

            holder.bind(contactListFiltered.get(position))

        }
    }

    fun setLongpress(longpress: Boolean) {
        islongpress = longpress
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

    fun unSelectItem(item: ChatItem) {
        var index = contactListFiltered.indexOf(item)
        if (item.isSelected) {
            item.isSelected = false
            notifyItemChanged(index)
        }

    }

    fun unselectandremoveitem(item: ChatItem, position: Int) {
        item.isSelected = false
        contactListFiltered.remove(item)
        notifyItemRemoved(position)
    }

    fun updateChatlist() {
        /*contactListFiltered.sortByDescending {
            it.lastMessage!!.dateSent
        }*/
        notifyDataSetChanged()
    }

    // this method get name of who send last message in group and call getSenderName to know it
    private fun setLastSendMessageName(textView: TextView?, sender: String?) {
        if (sender == null) return
        textView?.text = getSenderName(sender)
    }

    // this method get name if the sender in my contacts but if not return number otherwise return you
    private fun getSenderName(sender: String): String {
        if (sender == "0000001")
        {
            return "System"
        }
        if (sender == Blackbox.account.registeredNumber) {
            return "You"
        }
        val contact = Blackbox.getContact(sender) ?: Blackbox.getTemporaryContact(sender) ?: return ""
        if (contact.contactFullName.trim().isEmpty()) {
            return sender
        }
        return contact.contactFullName
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

    // this method append time of last message on text view person / group depends on the reference i sent
    private fun setMessageTime(textView: TextView?, dateSent: Date?) {
        textView?.text = dateSent?.getTimeString2("d/MM/YY")
    }

    // this method append Chat name on text view person / group depends on the reference i sent
    private fun setChatItemName(textView: TextView?, name: String?) {
        textView?.text = name
    }

    // this method check if number of unread messages = 0 hide it from page otherwise show it
    private fun setNoUnReadMessages(noUnReadMsgsTxt: TextView?, timeText: TextView?, chatUnreadMessagesCount: String?) {
        if (chatUnreadMessagesCount == "0") {
            noUnReadMsgsTxt?.visibility = View.GONE
            timeText?.setTextColor(context.getColor(R.color.chat_brw_date_Txt))
        }
        else {
            noUnReadMsgsTxt?.visibility = View.VISIBLE
            noUnReadMsgsTxt?.text = chatUnreadMessagesCount
            timeText?.setTextColor(context.getColor(R.color.no_unreaded_msgs))
        }
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
        if (message?.type?.isDocumentMessage!!) {
            msgTypeImageView.visibility = View.VISIBLE
            msgTypeImageView.set(R.drawable.document_chatlist)
            msgTypeText.text = message.body
            return
        }
        when (message.type) {
            MessageType.Photo -> {
                msgTypeImageView.visibility = View.VISIBLE
                msgTypeImageView.set(R.drawable.last_msg_photo)
//                if (message.body.isNotEmpty()) {
//                    msgTypeText.text = message.body
//                } else {
                msgTypeText.text = context.resources.getString(R.string.media_photo)
//                }
            }
            MessageType.Audio -> {
                msgTypeImageView.visibility = View.VISIBLE
                msgTypeImageView.set(R.drawable.mic_grey_icon)
                msgTypeText.text = getMediaDuration(context, message.localFileName.value)
//                msgTypeText.text = context.resources.getString(R.string.media_audio)
            }

            MessageType.Video -> {
                msgTypeImageView.visibility = View.VISIBLE
                msgTypeImageView.set(R.drawable.video_chatlist)
                msgTypeText.text = context.resources.getString(R.string.media_video)
            }

            MessageType.AlertScreenshot -> {
                msgTypeImageView.visibility = View.GONE
                val normalBOLD = getSenderName(message.sender)
                val normalAfter = context.getString(R.string.took_a_screenshot)
                val s = SpannableStringBuilder()
                        .bold { append(normalBOLD) }
                        .append(" $normalAfter ")
                val italicStr = SpannableStringBuilder().italic { append(s) }
                msgTypeText.text = italicStr
            }

            MessageType.AlertScreenRecording -> {
                msgTypeImageView.visibility = View.GONE
                val normalBOLD = getSenderName(message.sender)
                val normalAfter = context.getString(R.string.took_a_video_recording)
                val s = SpannableStringBuilder()
                        .bold { append(normalBOLD) }
                        .append(" $normalAfter ")
                val italicStr = SpannableStringBuilder().italic { append(s) }
                msgTypeText.text = italicStr
            }

            else -> {
                msgTypeImageView.visibility = View.GONE
                val detailBody = MessageBody.convertStringToMessageBody(message.body)
                val realBody = if (detailBody.size > 0) {
                    MessageBody.getFullString(detailBody[0].text, detailBody.filter { it.startPosition != -1 })
                } else {
                    SpannableString(message.body)
                }
                msgTypeText.text = realBody
            }
            //TODO wait for more details about msgtype
        }

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

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val charString = query.toString()
                if (charString.isEmpty()) {
                    contactListFiltered = arrContact
                } else {
                    val filteredList: MutableList<ChatItem> = ArrayList()
                    for (row in arrContact) {
                        if (row.contact != null) {
                            if (row.contact!!.getContactName().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row)
                            }
                        } else {
                            if (row.group!!.desc.toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row)
                            }
                        }

                    }
                    contactListFiltered = filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = contactListFiltered
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, filterResults: FilterResults?) {
                contactListFiltered = filterResults?.values as MutableList<ChatItem>
                notifyDataSetChanged()
            }

        }
    }

    interface Chatlistitemclick {
        fun onChatitemSelect(chatItem: ChatItem, checkview: ImageView, itemview: View, islongpress: Boolean)
        fun onChatitemclick(chatItem: ChatItem)
    }
}