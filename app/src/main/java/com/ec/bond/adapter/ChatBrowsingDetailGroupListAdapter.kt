package com.ec.bond.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBGroupRole
import com.ec.bond.utils.UserAlertUtility
import de.hdodenhof.circleimageview.CircleImageView


class ChatBrowsingDetailGroupListAdapter(private var context: Context,
                                         private var arrContact: ArrayList<BBContact>?,
                                         private var isGroupAdmin: Boolean,
                                         private var groupId: String?,
                                         private var groupMembersItemClickListener: GroupMembersItemClickListener) : RecyclerView.Adapter<ChatBrowsingDetailGroupListAdapter.GroupListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupListViewHolder {
        val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view = layoutInflater!!.inflate(R.layout.item_group_list, parent, false)

        return GroupListViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GroupListViewHolder, position: Int) {
        holder.member_name.text = arrContact?.get(position)?.getContactName()
        val checkMemberStatus = isGroupAdminOrCreator(arrContact?.get(position))
        if (checkMemberStatus.first) {
            holder.member_type.text = checkMemberStatus.second
            holder.member_type.visibility = View.VISIBLE
        } else {
            holder.member_type.text = ""
            holder.member_type.visibility = View.GONE
        }
        if (!arrContact?.get(position)?.getChatImagePath().isNullOrEmpty()) {
            holder.member_image.set(BitmapFactory.decodeFile(arrContact?.get(position)?.getChatImagePath()))
        } else {
            if (!arrContact?.get(position)?.imagePath.isNullOrEmpty()) {
                holder.member_image.set(BitmapFactory.decodeFile(arrContact?.get(position)?.imagePath))
            } else {
                holder.member_image.setImageResource(R.drawable.contact)
            }
        }
        holder.itemView.setOnClickListener {
            if (!arrContact?.get(position)?.getContactName().equals("You", ignoreCase = true)) {
                val dialog = UserAlertUtility.initCustomDialog(context, R.layout.info_group_member_item, pCancelable = true)
                val viewContact = dialog?.findViewById<TextView>(R.id.view_contact)
                val messageView = dialog?.findViewById<TextView>(R.id.message)
                val voiceCallView = dialog?.findViewById<TextView>(R.id.voice_call)
                val videoCallView = dialog?.findViewById<TextView>(R.id.video_call)
                val makeAdmin = dialog?.findViewById<TextView>(R.id.make_admin)
                val removeContact = dialog?.findViewById<TextView>(R.id.remove_contact)
                messageView?.text = "Message ".plus(arrContact?.get(position)?.getContactName())
                voiceCallView?.text = "Call ".plus(arrContact?.get(position)?.getContactName())
                videoCallView?.text = "Video Call ".plus(arrContact?.get(position)?.getContactName())
                viewContact?.text = "View ".plus(arrContact?.get(position)?.getContactName())
                if (checkMemberStatus.first) {
                    makeAdmin?.text = context.getString(R.string.dismiss_admin)
                } else {
                    makeAdmin?.text = context.getString(R.string.make_group_admin)
                }
                if (isGroupAdmin) {
                    makeAdmin?.visibility = View.VISIBLE
                    removeContact?.visibility = View.VISIBLE
                    removeContact?.text = "Remove ".plus(arrContact?.get(position)?.getContactName())
                } else {
                    makeAdmin?.visibility = View.GONE
                    removeContact?.visibility = View.GONE
                }
                viewContact?.setOnClickListener {
                    groupMembersItemClickListener.viewGroupContactInfo(arrContact?.get(position))
                    UserAlertUtility.hideCustomDialog()
                }
                messageView?.setOnClickListener {
                    groupMembersItemClickListener.openChatScreen(arrContact?.get(position))
                    UserAlertUtility.hideCustomDialog()
                }
                voiceCallView?.setOnClickListener {
                    groupMembersItemClickListener.doVoiceCall(arrContact?.get(position))
                    UserAlertUtility.hideCustomDialog()
                }
                videoCallView?.setOnClickListener {
                    groupMembersItemClickListener.doVideoCall(arrContact?.get(position))
                    UserAlertUtility.hideCustomDialog()
                }
                makeAdmin?.setOnClickListener {
                    if (checkMemberStatus.first) {
                        groupMembersItemClickListener.updateMemberRole(arrContact?.get(position), BBGroupRole.Normal)
                    } else {
                        groupMembersItemClickListener.updateMemberRole(arrContact?.get(position), BBGroupRole.Admin)
                    }
                    UserAlertUtility.hideCustomDialog()
                }
                removeContact?.setOnClickListener {
                    groupMembersItemClickListener.removeMember(arrContact?.get(position))
                    UserAlertUtility.hideCustomDialog()
                }
                UserAlertUtility.showCustomDialog()
            }
        }
    }

    private fun isGroupAdminOrCreator(bbContact: BBContact?): Pair<Boolean, String> {
        val isAdminOrCreator: Boolean
        var userType = "default"
        val group = bbContact?.groups?.get(groupId)
        userType = group?.getName() ?: "default"
        isAdminOrCreator = group?.hasSuperPowers() ?: false
        return Pair(isAdminOrCreator, userType)
    }

    override fun getItemCount(): Int = arrContact?.size ?: 0

    inner class GroupListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var member_name: TextView = itemView.findViewById(R.id.member_name)
        internal var member_type: TextView = itemView.findViewById(R.id.member_type)
        internal var member_image: CircleImageView = itemView.findViewById(R.id.member_image)
    }

    interface GroupMembersItemClickListener {
        fun viewGroupContactInfo(bbContact: BBContact?)
        fun openChatScreen(bbContact: BBContact?)
        fun doVideoCall(bbContact: BBContact?)
        fun doVoiceCall(bbContact: BBContact?)
        fun updateMemberRole(bbContact: BBContact?, role: BBGroupRole)
        fun removeMember(bbContact: BBContact?)
    }
}
