package com.ec.bond.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.ui.calls.CallsFragment
import com.ec.bond.activity.ui.calls.CallsViewModel
import com.ec.bond.activity.ui.contactlist.ContactListFragment
import com.ec.bond.activity.ui.contactlist.ContactListViewModel
import com.ec.bond.blackbox.model.BBContact
import kotlinx.android.synthetic.main.item_call.view.*


class ContactAdapter(private var context: Context, private var arrContact: ArrayList<BBContact>, callback: ContactsAdapterListener?,private var callsFragment: ContactListFragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    private var contactListFiltered: MutableList<BBContact>? = null
    var listener: ContactsAdapterListener? = null

    private var layoutInflater: LayoutInflater? = null
    private val TYPE_NEW_GROUP = 1
    private val TYPE_CONTACT = 2
    private val TYPE_NEW_CONTACT = 3
    var contactViewModel: ContactListViewModel = ViewModelProvider(callsFragment).get(ContactListViewModel::class.java)
    init {
        contactListFiltered = arrContact
        listener = callback
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var contact_name: TextView = itemView.findViewById(R.id.contact_name)
        internal var contact_number: TextView = itemView.findViewById(R.id.contact_number)
        internal var add_contact: CheckBox = itemView.findViewById(R.id.cb_add_contact)
        internal var image: ImageView = itemView.findViewById(R.id.img_pic)
        internal var phoneImage: ImageView = itemView.findViewById(R.id.img_phonecall)
        internal var videoImage: ImageView = itemView.findViewById(R.id.img_videocall)

        init {
            itemView.setOnClickListener {
                getContactAt(adapterPosition)?.let {
                    listener?.onContactSelected(it,itemView)
                }
            }
            itemView.setOnLongClickListener {
                getContactAt(adapterPosition)?.let {
                    listener?.onContactSelected(it,itemView)
                }
                return@setOnLongClickListener true
            }
            phoneImage.setOnClickListener {
                getContactAt(adapterPosition)?.let {
                    listener?.onCallSelected(it)
                }
            }
            videoImage.setOnClickListener {
                getContactAt(adapterPosition)?.let {
                    listener?.onVideoCallSelected(it)
                }
            }
        }
    }

    inner class NewGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var contact_name: TextView = itemView.findViewById(R.id.contact_name)
        internal var contact_number: TextView = itemView.findViewById(R.id.contact_number)
        internal var add_contact: CheckBox = itemView.findViewById(R.id.cb_add_contact)
        internal var image: ImageView = itemView.findViewById(R.id.img_pic)
        internal var phoneImage: ImageView = itemView.findViewById(R.id.img_phonecall)
        internal var videoImage: ImageView = itemView.findViewById(R.id.img_videocall)

        init {
            itemView.setOnClickListener {
                listener?.onGroupSelected()
            }
        }
    }

    inner class NewContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var contact_name: TextView = itemView.findViewById(R.id.contact_name)
        internal var contact_number: TextView = itemView.findViewById(R.id.contact_number)
        internal var add_contact: CheckBox = itemView.findViewById(R.id.cb_add_contact)
        internal var image: ImageView = itemView.findViewById(R.id.img_pic)
        internal var phoneImage: ImageView = itemView.findViewById(R.id.img_phonecall)
        internal var videoImage: ImageView = itemView.findViewById(R.id.img_videocall)

        init {
            itemView.setOnClickListener {
                listener?.onNewContactSelected()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_NEW_GROUP) {
            layoutInflater =
                    this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
            val view = layoutInflater!!.inflate(R.layout.item_contact, parent, false)

            return NewGroupViewHolder(view)
        }else if(viewType == TYPE_NEW_CONTACT){
            layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
            val view = layoutInflater!!.inflate(R.layout.item_contact, parent, false)

            return NewContactViewHolder(view)
        }
        else {
            layoutInflater =
                    this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
            val view = layoutInflater!!.inflate(R.layout.item_contact, parent, false)

            return ContactViewHolder(view)
        }

    }

    override fun getItemCount(): Int {
        return contactListFiltered!!.size
    }

    override fun getItemViewType(position: Int): Int {
        if (contactListFiltered!![position].ID.equals("000000")) {
            return TYPE_NEW_GROUP
        }else if(contactListFiltered!![position].ID.equals("0000000")){
            return TYPE_NEW_CONTACT
        }
        else {
            return TYPE_CONTACT
        }
    }

    override fun onBindViewHolder(holders: RecyclerView.ViewHolder, position: Int) {

        if (getItemViewType(position) == TYPE_NEW_GROUP) {
            val holder = holders as NewGroupViewHolder
            holder.contact_name.text = contactListFiltered?.get(position)!!.name
            holder.image.setImageResource(R.mipmap.ic_group)
            holder.contact_number.visibility = View.GONE
            holder.phoneImage.visibility = View.GONE
            holder.videoImage.visibility = View.GONE
        }else if(getItemViewType(position) == TYPE_NEW_CONTACT){
            val holder = holders as NewContactViewHolder
            holder.contact_name.text = contactListFiltered?.get(position)!!.name
            holder.image.setImageResource(R.mipmap.ic_contact)
            holder.contact_number.visibility = View.GONE
            holder.phoneImage.visibility = View.GONE
            holder.videoImage.visibility = View.GONE
        }
        else {
            val holder = holders as ContactViewHolder
            if(contactListFiltered?.get(position)?.isSelected!!){

                    holder.itemView.item_selectedCheck.visibility = View.VISIBLE
                }else{
                holder.itemView.item_selectedCheck.visibility = View.INVISIBLE
            }

            holder.contact_name.text = contactListFiltered?.get(position)!!.name+" "+contactListFiltered?.get(position)!!.surname
            holder.contact_number.text = contactListFiltered!![position].phonesjson.get(0).phone
            if (!contactListFiltered!![position].getChatImagePath().isNullOrEmpty()) {
                holder.image.set(BitmapFactory.decodeFile(contactListFiltered!![position].getChatImagePath()))
            } else {
                holder.image.setImageResource(R.drawable.contact)
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
                    val filteredList: MutableList<BBContact> = ArrayList()

                    for (row in arrContact) {
                        if (row.phonesjson.isEmpty()) {
                            if (row.name.toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row)
                            }
                        } else {
                            if (row.name.toLowerCase().contains(charString.toLowerCase()) || row.phonesjson.get(0).phone.contains(charSequence)) {
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
                contactListFiltered = filterResults.values as MutableList<BBContact>
                // refresh the list with filtered data
                notifyDataSetChanged()
            }
        }
    }

    private fun getContactAt(position: Int) : BBContact? {
        val contacts = contactListFiltered ?: return null
        if (position >= 0 && position < contacts.size) {
            return contacts[position]
        }
        return null
    }

    public fun deleteContactAt(contact: BBContact){
        contactListFiltered?.remove(contact)
        notifyDataSetChanged()
    }

    public fun selectedContact(contact: BBContact){
       var index=contactListFiltered?.indexOf(contact)
        contactListFiltered?.set(index!!,contact)
        notifyDataSetChanged()
    }

    public fun deselectedContact(contact: BBContact){
        var index=contactListFiltered?.indexOf(contact)
        if(index!=-1)
        contactListFiltered?.set(index!!,contact)
        notifyDataSetChanged()
    }

    interface ContactsAdapterListener {
        fun onGroupSelected()
        fun onNewContactSelected()
        fun onContactSelected(contact: BBContact?, view: View)
        fun onCallSelected(contact: BBContact)
        fun onVideoCallSelected(contact: BBContact)
    }
}