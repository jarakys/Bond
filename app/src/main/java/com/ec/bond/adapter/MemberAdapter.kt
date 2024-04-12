package com.ec.bond.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.Contact
import de.hdodenhof.circleimageview.CircleImageView


class MemberAdapter(private var context: Context, private var arrContact: ArrayList<Contact>) : RecyclerView.Adapter<MemberAdapter.Memberviewholder>() {
    private var layoutInflater: LayoutInflater? = null

    inner class Memberviewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var contact_name: TextView = itemView.findViewById(R.id.contact_name)
        internal var image: CircleImageView = itemView.findViewById(R.id.img_pic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Memberviewholder {
        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view = layoutInflater!!.inflate(R.layout.item_member, parent, false)

        return Memberviewholder(view)
    }

    override fun getItemCount(): Int {
        return arrContact!!.size
    }

    override fun onBindViewHolder(holder: Memberviewholder, position: Int) {
        val contact = arrContact[position]
        if(!contact.contact.name.isNullOrEmpty()){
            holder.contact_name.text = contact.contact.name
        }else{
            holder.contact_name.text = contact.registerno
        }
        if(!contact.image.isNullOrEmpty()){
            holder.image.set(BitmapFactory.decodeFile(contact.image))
        }
    }
}