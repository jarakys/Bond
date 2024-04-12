package com.ec.bond.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.blackbox.model.BBContact


class ContactListAdapter(private var context: Context, private var arrContact: ArrayList<BBContact>, callback: ContactsAdapterListener?) : RecyclerView.Adapter<ContactListAdapter.ContactViewHolder>(), Filterable {

    private var contactListFiltered: MutableList<BBContact>? = null
    var listener: ContactsAdapterListener? = null

    private var layoutInflater: LayoutInflater? = null

    init {
        contactListFiltered = arrContact
        listener = callback
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var contact_name: TextView = itemView.findViewById(R.id.contact_name)
        internal var contact_number: TextView = itemView.findViewById(R.id.contact_number)
        internal var add_contact: CheckBox = itemView.findViewById(R.id.cb_add_contact)
        internal var image: ImageView = itemView.findViewById(R.id.img_pic)
        internal var img_videocall:ImageView=itemView.findViewById(R.id.img_videocall);
        internal var img_phonecall:ImageView=itemView.findViewById(R.id.img_phonecall)
        init {
            itemView.setOnClickListener {
                val bbContact = contactListFiltered!![adapterPosition]
                if(bbContact.ID.isNotEmpty()){
                    listener!!.onContactSelected(bbContact)
                }else{
                    if(absoluteAdapterPosition==0){
                        listener!!.onGroupSelected()
                    }else{
                        listener!!.onAddNewContact()
                    }

                }
            }

            img_videocall.setOnClickListener{
                getContactAt(adapterPosition)?.let {
                    listener?.onVideoCallSelected(it)
                }
            }

            img_phonecall.setOnClickListener{
                getContactAt(adapterPosition)?.let {
                    listener?.onCallSelected(it)
                }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view = layoutInflater!!.inflate(R.layout.item_contact, parent, false)

        return ContactViewHolder(view)
    }

    override fun getItemCount(): Int {
        if(contactListFiltered != null){
            return contactListFiltered!!.size
        }
        return 0
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.add_contact.visibility= View.INVISIBLE
        holder.contact_name.text = contactListFiltered?.get(position)!!.name
        if(contactListFiltered!![position].image != null){
            holder.image.setImageResource(contactListFiltered!![position].image!!)
        }else{
            holder.image.setImageResource(R.drawable.contact)
        }
        if(contactListFiltered!![position].phonesjson.isEmpty()){
            holder.contact_number.visibility = View.GONE
        }else{
            holder.contact_number.visibility = View.VISIBLE
            holder.contact_number.text = contactListFiltered!![position].phonesjson.get(0).phone
        }

        if(position==0 ||position==1){
            holder.img_phonecall.visibility=View.GONE
            holder.img_videocall.visibility=View.GONE
        }else{
            holder.img_phonecall.visibility=View.VISIBLE
            holder.img_videocall.visibility=View.VISIBLE
        }

//        holder.add_contact.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
//            if (b) {
//
//                addContactResultPresenter.requestAddContactResult(pwd_config,data_json.toString())
//            }
//        }
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
                        if(row.phonesjson.isEmpty()){
                            if (row.name.toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row)
                            }
                        }else{
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
                contactListFiltered = filterResults.values as? MutableList<BBContact>
                // refresh the list with filtered data
                notifyDataSetChanged()
            }
        }
    }

    interface ContactsAdapterListener {
        fun onGroupSelected()
        fun onAddNewContact()
        fun onContactSelected(contact: BBContact?)
        fun onCallSelected(contact: BBContact)
        fun onVideoCallSelected(contact: BBContact)

    }
}