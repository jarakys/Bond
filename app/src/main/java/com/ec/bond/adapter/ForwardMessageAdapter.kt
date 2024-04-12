package com.ec.bond.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.forwardmessage.ForwardMessageViewModel
import com.ec.bond.blackbox.model.BBChat
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBGroup
import kotlinx.android.synthetic.main.item_call.view.*
import kotlinx.android.synthetic.main.item_forward_message.view.*
import kotlinx.android.synthetic.main.item_forward_message.view.contact_imageView
import kotlinx.android.synthetic.main.item_forward_message.view.contact_name_txt
import kotlinx.android.synthetic.main.item_forward_message.view.item_selectedCheck

class ForwardMessageAdapter(
        private val context: Context,
        private val contacts: ArrayList<BBChat>,
        val forwardMessageViewModel: ForwardMessageViewModel
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//    private var layoutInflater: LayoutInflater? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?

        var view = layoutInflater!!.inflate(R.layout.item_forward_message, parent, false)
        return ForwardMessageViewHolder(view)
    }
    inner class ForwardMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    }
    override fun getItemCount(): Int = contacts.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (contacts[position].chatImagePath.value != null) {
            holder.itemView.contact_imageView.set(BitmapFactory.decodeFile(contacts[position].chatImagePath.value))
        } else {
            holder.itemView.contact_imageView.set(R.drawable.contact)
        }
        if (contacts[position] is BBContact) {
            if ((contacts[position] as BBContact).name.isNotEmpty()) {
                holder.itemView.contact_name_txt.text = (contacts[position] as BBContact).name
                holder.itemView.contact_number.text = (contacts[position] as BBContact).registeredNumber

            } else {
                holder.itemView.contact_name_txt.text = (contacts[position] as BBContact).registeredNumber

                holder.itemView.contact_number.visibility = View.GONE
            }
        } else if (contacts[position] is BBGroup) {
            holder.itemView.contact_name_txt.text = (contacts[position] as BBGroup).desc

            holder.itemView.contact_number.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            forwardMessageViewModel.getSelectedContacts().forEach {
                if (it is BBContact) {
                    if (it.ID == (contacts[position] as BBContact).ID) {
                        forwardMessageViewModel.removeItemSelectedContacts(contacts[position])
                        val outValue = TypedValue()
                        context?.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                        holder.itemView.setBackgroundResource(outValue.resourceId)
                        holder.itemView.item_selectedCheck.visibility = View.GONE
//                    forwardMessageViewModel.setSelectionTitle()
                        return@setOnClickListener
                    }
                }
            }
            holder.itemView.setBackgroundResource(R.color.light_gray_selection)
            holder.itemView.item_selectedCheck.visibility = View.VISIBLE
            addInSelectedList(position)
        }
    }
    fun handleCancelSelection(){
        forwardMessageViewModel.clearSelectedContacts()
    }
    fun addInSelectedList(position: Int){
        forwardMessageViewModel.addToSelectedContacts(contacts[position])
//        forwardMessageViewModel.setSelectionTitle()
    }
}