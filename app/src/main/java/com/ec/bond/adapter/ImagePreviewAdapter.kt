package com.ec.bond.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import kotlinx.android.synthetic.main.item_full_image.view.*

class ImagePreviewAdapter(private var c: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var wallpapers: List<Int> = ArrayList()

    override fun getItemCount(): Int {
        return wallpapers.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        return WallpaperViewHolder(LayoutInflater.from(c).inflate(R.layout.item_full_image, parent, false))
    }

    fun submitList(wallpapersList: List<Int>) {
        wallpapers = wallpapersList
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {

            is WallpaperViewHolder -> {
                holder.bind(wallpapers[position])
            }

        }
        holder.itemView.setOnClickListener(View.OnClickListener {
        })
    }

    class WallpaperViewHolder
    constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val wallpaper: ImageView = itemView.img_preview

        fun bind(res: Int) {
            wallpaper.setImageResource(res)

        }

    }

}