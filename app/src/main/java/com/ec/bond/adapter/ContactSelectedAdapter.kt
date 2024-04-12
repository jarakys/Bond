package com.ec.bond.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.blackbox.model.BBContactGroup

class ContactSelectedAdapter(private var context: Context, private var arrContact: ArrayList<BBContactGroup>, callback: ContactSelectedListener?) : RecyclerView.Adapter<ContactSelectedAdapter.ContactSelectedViewHolder>() {

    private var contactListFiltered: ArrayList<BBContactGroup>? = null
    var listener: ContactSelectedListener? = null

    private var layoutInflater: LayoutInflater? = null

    init {
        contactListFiltered = arrContact
        listener = callback
    }

    inner class ContactSelectedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var contact_name: TextView = itemView.findViewById(R.id.contact_name)
        internal var add_contact: CheckBox = itemView.findViewById(R.id.cb_add_contact)
        internal var image: ImageView = itemView.findViewById(R.id.img_pic)
        internal var main: ConstraintLayout = itemView.findViewById(R.id.main)

        init {
            itemView.setOnClickListener {
                val bbContactGroup = contactListFiltered!![adapterPosition]
                if (bbContactGroup.contact.ID.isNotEmpty()) {
                    listener!!.onContactSelected(bbContactGroup, adapterPosition)
                }
            }
        }
    }

    fun removeat(position: Int) {
        contactListFiltered!!.removeAt(position)
        notifyItemRemoved(position)
    }

    fun remove(item: BBContactGroup) {
        val index = contactListFiltered!!.indexOf(item)
        removeat(index)
    }

    fun add(item: BBContactGroup) {
        contactListFiltered!!.add(contactListFiltered!!.size, item)
        notifyItemInserted(contactListFiltered!!.size)
    }

    fun memberlist(): ArrayList<BBContactGroup> {
        return contactListFiltered!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactSelectedViewHolder {
        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view = layoutInflater!!.inflate(R.layout.item_contact_selected, parent, false)

        return ContactSelectedViewHolder(view)
    }

    override fun getItemCount(): Int {
        if (contactListFiltered != null) {
            return contactListFiltered!!.size
        }
        return 0
    }

    override fun onBindViewHolder(holder: ContactSelectedViewHolder, position: Int) {
        holder.add_contact.visibility = View.INVISIBLE
        holder.contact_name.text = contactListFiltered?.get(position)!!.contact.name
        if(!contactListFiltered!![position].contact.getChatImagePath().isNullOrEmpty()){
            holder.image.set(BitmapFactory.decodeFile(contactListFiltered!![position].contact.getChatImagePath()))
        }else{
            holder.image.setImageResource(R.drawable.contact)
        }

    }

    interface ContactSelectedListener {
        fun onContactSelected(contact: BBContactGroup?, index: Int)
    }
}