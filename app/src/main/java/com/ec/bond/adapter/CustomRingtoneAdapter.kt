package com.ec.bond.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R

class CustomRingtoneAdapter(val context: Context,
                            private val arrayList: ArrayList<String>,
                            private val ringtoneSelectionListener: RingtoneSelectionListener,
                            private var selectedPosition: Int = -1) : RecyclerView.Adapter<CustomRingtoneAdapter.CustomRingtoneViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomRingtoneViewHolder {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view = layoutInflater!!.inflate(R.layout.item_custom_ringtone, parent, false)

        return CustomRingtoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomRingtoneViewHolder, position: Int) {
        holder.ringtone_name.text = arrayList[position]
        holder.ringtone_name.setOnClickListener {
            selectedPosition = position
            ringtoneSelectionListener.onRingtoneSelectionListener(position)
            notifyDataSetChanged()
        }
        if (selectedPosition != -1 && selectedPosition == position) {
            holder.selected_ringtone.visibility = View.VISIBLE
        } else {
            holder.selected_ringtone.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = arrayList.size

    inner class CustomRingtoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var ringtone_name: TextView = itemView.findViewById(R.id.ringtone_name)
        internal var selected_ringtone: ImageView = itemView.findViewById(R.id.selected_ringtone)
    }

    interface RingtoneSelectionListener {
        fun onRingtoneSelectionListener(position: Int)
    }
}