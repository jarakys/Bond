package com.ec.bond.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.blackbox.model.BBContactGroup

class GrouplistAdapter(private var context: Context,
                       private var arrContact: ArrayList<BBContactGroup>,
                       callback: GroupAdapterListener?,
                       var selectLimitedContact: Boolean = false,
                       var maxNoOfContactSelected: Int = 1) : RecyclerView.Adapter<GrouplistAdapter.GroupViewHolder>(), Filterable {
    private var contactListFiltered: MutableList<BBContactGroup>? = null
    var listener: GroupAdapterListener? = null

    private var layoutInflater: LayoutInflater? = null
    var noOfContactSelected: Int = 0

    init {
        contactListFiltered = arrContact
        listener = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view = layoutInflater!!.inflate(R.layout.item_group, parent, false)

        return GroupViewHolder(view)
    }

    override fun getItemCount(): Int {
        if (contactListFiltered != null) {
            return contactListFiltered!!.size
        }
        return 0
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.contact_name.text = contactListFiltered?.get(position)!!.contact.name
        holder.contact_number.text = contactListFiltered!![position].contact.phonesjson.get(0).phone
        if (contactListFiltered?.get(position)!!.isselected) {
            holder.checkview.visibility = View.VISIBLE
        } else {
            holder.checkview.visibility = View.GONE
        }
        if (!contactListFiltered!![position].contact.getChatImagePath().isNullOrEmpty()) {
            holder.image.set(BitmapFactory.decodeFile(contactListFiltered!![position].contact.getChatImagePath()))
        } else {
            holder.image.setImageResource(R.drawable.contact)
        }

        if (selectLimitedContact && noOfContactSelected >= maxNoOfContactSelected) {
            if (contactListFiltered!![position].isselected) {
                holder.overlay.visibility = View.GONE
                holder.contact_name.setTextColor(ContextCompat.getColor(context, R.color.black))
            } else {
                holder.overlay.visibility = View.VISIBLE
                holder.contact_name.setTextColor(ContextCompat.getColor(context, R.color.light_grey))
            }
        } else {
            holder.overlay.visibility = View.GONE
            holder.contact_name.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var contact_name: TextView = itemView.findViewById(R.id.contact_name)
        internal var contact_number: TextView = itemView.findViewById(R.id.contact_number)
        internal var add_contact: CheckBox = itemView.findViewById(R.id.cb_add_contact)
        internal var checkview: ImageView = itemView.findViewById(R.id.checkview)
        internal var viewgroup: ConstraintLayout = itemView.findViewById(R.id.parent)
        internal var image: ImageView = itemView.findViewById(R.id.img_pic)
        internal var overlay: ImageView = itemView.findViewById(R.id.overlay)
        private var selectContact: Boolean = true

        init {
            itemView.setOnClickListener {
                val bbContactGroup = contactListFiltered!![adapterPosition]
                selectContact = if (selectLimitedContact) {
                    noOfContactSelected < maxNoOfContactSelected || bbContactGroup.isselected
                } else {
                    true
                }
                if (selectContact) {
                    if (bbContactGroup.contact.ID.isNotEmpty()) {
                        bbContactGroup.isselected = !bbContactGroup.isselected
                        TransitionManager.beginDelayedTransition(viewgroup, Fade())
                        checkview.visibility = if (bbContactGroup.isselected) View.VISIBLE else View.GONE
                        listener!!.onGroupSelected(bbContactGroup, adapterPosition)
                    }
                }
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    contactListFiltered = arrContact
                } else {
                    val filteredList: MutableList<BBContactGroup> = ArrayList()
                    for (row in arrContact) {
                        if (row.contact.phonesjson.isEmpty()) {
                            if (row.contact.name.toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row)
                            }
                        } else {
                            if (row.contact.name.toLowerCase().contains(charString.toLowerCase()) || row.contact.phonesjson.get(0).phone.contains(charSequence)) {
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

            override fun publishResults(
                    charSequence: CharSequence,
                    filterResults: FilterResults
            ) {
                contactListFiltered = filterResults.values as? MutableList<BBContactGroup>
                // refresh the list with filtered data
                notifyDataSetChanged()
            }
        }
    }

    interface GroupAdapterListener {
        fun onGroupSelected(contact: BBContactGroup?, position: Int)
    }


}