package com.ec.bond.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ec.bond.R
import kotlinx.android.synthetic.main.wallpaper_item_grid.view.*

class WallpaperGridAdapter( public var c: Context, private val clickListener: GridImageItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var wallpapers: List<Int> = ArrayList()

    override fun getItemCount(): Int {
        return wallpapers.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        return WallpaperViewHolder(LayoutInflater.from(c).inflate(R.layout.wallpaper_item_grid, parent, false))
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
            clickListener.onItemClick(wallpapers[position], holder.itemView.img_wallpaper)
        })
    }

  inner  class WallpaperViewHolder
    constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val blog_image: ImageView = itemView.img_wallpaper

        fun bind(res: Int) {
           // blog_image.setImageResource(c)
            Glide.with(c).load(res).into(blog_image)

        }

    }
    interface GridImageItemClickListener {
        fun onItemClick(item: Int, imageView: ImageView)
    }
}