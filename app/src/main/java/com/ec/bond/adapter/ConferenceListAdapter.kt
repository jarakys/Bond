package com.ec.bond.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.blackbox.model.BBContact
import de.hdodenhof.circleimageview.CircleImageView

class ConferenceListAdapter(private var context: Context,
                            private var arrContact: ArrayList<BBContact>?,
                            private val conferenceAdapterListener: ConferenceAdapterListener) : RecyclerView.Adapter<ConferenceListAdapter.ConferenceViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConferenceViewHolder {
        val layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view = layoutInflater!!.inflate(R.layout.item_conference_call, parent, false)

        return ConferenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConferenceViewHolder, position: Int) {
        holder.contact_name.text = arrContact?.get(position)?.name
        if (!arrContact?.get(position)?.imagePath.isNullOrEmpty()) {
            holder.contact_image.set(BitmapFactory.decodeFile(arrContact?.get(position)?.imagePath))
        } else {
            holder.contact_image.setImageResource(R.drawable.contact)
        }
        holder.delete_caller.setOnClickListener {
            conferenceAdapterListener.deletedSelectedConference(arrContact?.get(position), position)
        }
        holder.delete_caller_parent_view.setOnClickListener {
            conferenceAdapterListener.deletedSelectedConference(arrContact?.get(position), position)
        }
        holder.caller_status.text = arrContact?.get(position)?.callStatus
        when (arrContact?.get(position)?.callStatus) {
            context.getString(R.string.ringing) -> {
                holder.caller_status.setTextColor(context.getColor(R.color.color_orange))
                holder.contact_image.borderColor = context.getColor(R.color.color_orange)
            }
            else -> {
                holder.caller_status.setTextColor(context.getColor(R.color.white))
                holder.contact_image.borderColor = context.getColor(R.color.white)
            }
        }
    }

    fun setCallStatus(position: Int, status: String) {
        arrContact?.get(position)?.callStatus = status
    }

    override fun getItemCount(): Int = arrContact?.size ?: 0

    inner class ConferenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var contact_name: TextView = itemView.findViewById(R.id.caller_name)
        internal var caller_status: TextView = itemView.findViewById(R.id.caller_status)
        internal var contact_image: CircleImageView = itemView.findViewById(R.id.caller_image)
        internal var delete_caller: ImageButton = itemView.findViewById(R.id.delete_caller)
        internal var delete_caller_parent_view: RelativeLayout = itemView.findViewById(R.id.delete_caller_parent_view)
    }

    interface ConferenceAdapterListener {
        fun deletedSelectedConference(contact: BBContact?, position: Int)
    }
}