package com.ec.bond.adapter

import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.ui.calls.CallsFragment
import com.ec.bond.activity.ui.calls.CallsViewModel
import com.ec.bond.blackbox.model.callsHistory.BBCallDirection
import com.ec.bond.blackbox.model.callsHistory.BBCallHistoryGroup
import com.ec.bond.blackbox.model.callsHistory.BBCallType
import com.ec.bond.utils.DateTimeUtils
import kotlinx.android.synthetic.main.item_call.view.*


class LastCallsAdapter(private var callAdapterListener: CallAdapterListener, private var lastCalls_: List<BBCallHistoryGroup>, private var callsFragment: CallsFragment) : RecyclerView.Adapter<LastCallsAdapter.CustomViewHolder>(), Filterable {

    //    private var isLongPressed = false
    var callsViewModel: CallsViewModel = ViewModelProvider(callsFragment).get(CallsViewModel::class.java)
    private var lastCalls: List<BBCallHistoryGroup> = listOf()

    init {
        lastCalls = lastCalls_
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call, parent, false)

        return CustomViewHolder(view)
    }

    override fun getItemCount() = lastCalls.size

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        if (lastCalls[position].isLastItem) {
            holder.itemView.visibility = View.INVISIBLE
        } else {
            holder.itemView.visibility = View.VISIBLE
            if (callsViewModel.getSelectedCalls().any { it.isSelected == lastCalls[position].isSelected }) {
                holder.itemView.setBackgroundResource(R.color.light_gray_selection)
                holder.itemView.item_selectedCheck.visibility = View.VISIBLE
            } else {
                val outValue = TypedValue()
                callsFragment.context?.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                holder.itemView.setBackgroundResource(outValue.resourceId)
                holder.itemView.item_selectedCheck.visibility = View.INVISIBLE
            }
            if(!lastCalls[position].contact.getChatImagePath().isNullOrEmpty()){
                val path = lastCalls[position].contact.getChatImagePath()
                holder.itemView.contact_imageView.set(BitmapFactory.decodeFile(path))
            }else{
                holder.itemView.contact_imageView.set(R.drawable.contact)
            }
            holder.itemView.contact_name_txt.text = lastCalls[position].contact.getContactName()
            try {
                val callDate = DateTimeUtils.parseUTCDateFormat(lastCalls[position].calls.first().dtsetup)
                holder.itemView.date_txt.text =
                    callDate?.let { DateTimeUtils.formatTimeZoneDateFormat(it) }
            } catch (e: Exception) {}

            holder.itemView.call_status_imageView.set(if (lastCalls[position].direction == BBCallDirection.Outgoing) R.drawable.done_call else R.drawable.done_inbounding_call)
            holder.itemView.call_Btn.set(if (lastCalls[position].type == BBCallType.Call) R.drawable.phone_call else R.drawable.video_call)
            holder.itemView.setOnClickListener { it: View? ->
                if (callsViewModel.isLongPressed.value!!) {
                    callAdapterListener.onItemclik(position, lastCalls[position], holder.itemView, holder.itemView.item_selectedCheck)
                } else {
                    callAdapterListener.onCallStarted(lastCalls[position])
                }
            }

            holder.itemView.setOnLongClickListener {
                callAdapterListener.onItemclik(position, lastCalls[position], holder.itemView, holder.itemView.item_selectedCheck)
                return@setOnLongClickListener true
            }
        }
        if (lastCalls.size > 1 && position == lastCalls.size - 2) {
            holder.itemView.view.visibility = View.INVISIBLE
        } else {
            holder.itemView.view.visibility = View.VISIBLE
        }
    }

    fun handleCancelSelection() {
        callsViewModel.clearSelectedCalls()
        callsViewModel.setIsLongPressed(false)
    }

    fun unSelectitem(item: BBCallHistoryGroup){
        var index = lastCalls.indexOf(item)
        item.isSelected = false
        notifyItemChanged(index)
   }

    fun addInSelectedList(position: Int) {
        var call = lastCalls[position]
        call.isSelected = true
        callsViewModel.addToSelectedCalls(call)
        callsViewModel.setLongPressedTitle(callsViewModel.getSizeSelectedCalls().toString())
    }

    inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

//        override fun onClick(v: View?) {
//
//        }
//
//        override fun onLongClick(v: View?): Boolean {
//            print("long clicked")
//            return true
//        }
    }

    interface CallAdapterListener {
        fun onCallStarted(contact: BBCallHistoryGroup?)
        fun onItemclik(position: Int, contact: BBCallHistoryGroup, itemeview: View, checkview: ImageView)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val charString = query.toString()
                if (charString.isEmpty()) {
                    lastCalls = lastCalls_
                } else {
                    val filteredList: MutableList<BBCallHistoryGroup> = ArrayList()
                    for (row in lastCalls_) {
                        if (row.contact.getContactName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row)
                        }
                    }
                    lastCalls = filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = lastCalls
                return filterResults
            }

            override fun publishResults(p0: CharSequence?, filterResults: FilterResults?) {
                lastCalls = filterResults!!.values as List<BBCallHistoryGroup>
                notifyDataSetChanged()
            }

        }
    }
}

