package com.ec.bond.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.blackbox.Blackbox
import kotlinx.android.synthetic.main.item_archive_list.view.*

class ArchiveCount(var context: Context,var data: String,private var listner: Archivecountitemclick) : RecyclerView.Adapter<ArchiveCount.ArchiveTypeChatstViewHolder>(){

    private var layoutInflater: LayoutInflater? = null
    init {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
    }
    inner class ArchiveTypeChatstViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview) {
        fun bind(title: String) {
            itemView.txt_archivetitle.text = title
            itemView.setOnClickListener {
                if(Blackbox.archivedChatItems.value!!.isNotEmpty()){
                    listner.onArchiveitemclick()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveTypeChatstViewHolder {
        val view = layoutInflater!!.inflate(R.layout.item_archive_list, parent, false)
        return ArchiveTypeChatstViewHolder(view)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: ArchiveTypeChatstViewHolder, position: Int) {
        holder.bind(data)
    }
    fun updateCount(){
        val size = Blackbox.archivedChatItems.value!!.size
        data = if (size == 0) "Tap and hold on a chat for more options" else "Archived (${size})"
        notifyItemChanged(0)
    }
    interface Archivecountitemclick {
        fun onArchiveitemclick()
    }
}