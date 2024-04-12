package com.ec.bond.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.ec.bond.R
import com.ec.bond.activity.ui.imageviewer.ImageViewModel
import kotlinx.android.synthetic.main.item_small_image_view.view.*
import kotlinx.android.synthetic.main.item_video_view.view.*

class ImageViewerAdapter(
        val filePaths: ArrayList<String>,
        val context: Context,
        val isMainAdapter: Boolean,
        val imageViewModel: ImageViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var layoutInflater: LayoutInflater? = null
    lateinit var image : ImageView
    private val TYPE_IMAGE = 1
    private val TYPE_VIDEO = 2
    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        var view: View
        if (viewType == TYPE_IMAGE) {
            view = layoutInflater!!.inflate(if (isMainAdapter) R.layout.item_image else R.layout.item_small_image_view, parent, false)
            return ImageViewHolder(view)
        } else {
            view = layoutInflater!!.inflate(if (isMainAdapter) R.layout.item_video_view else R.layout.item_small_image_view, parent, false)
            return VideoViewHolder(view)
        }

    }

    override fun getItemCount(): Int {
        if (filePaths.size > 15){
            cleanArrayElements()
            Toast.makeText(context,R.string.exceed_max_selecting_size,Toast.LENGTH_SHORT).show()
        }
       return filePaths.size
    }

    private fun cleanArrayElements() {
        while (filePaths.size > 15) {
            filePaths.removeAt(filePaths.size - 1)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (filePaths[position].contains(".jpg")) {
            TYPE_IMAGE
        } else {
            TYPE_VIDEO
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageViewHolder) {
            holder.onbind(position)
        } else if (holder is VideoViewHolder) {
            holder.onbind(position)
        }
    }
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var image: ImageView = itemView.findViewById(R.id.imageShowImageView)
        fun onbind(position: Int) {
//            val bitmap = BitmapFactory.decodeFile(filePaths[position])
            Glide.with(context).load(filePaths[position]).apply(requestOptions).dontTransform().into(image)

//            image.set(bitmap)
            if (!isMainAdapter) {
                itemView.setOnClickListener {
                    imageViewModel.changeItemSelected(position)
                }
                if (imageViewModel.imageSelected.value == position) {
                    itemView.imageViewSelected_constraintLayout.setBackgroundColor((context as AppCompatActivity).getColor(R.color.image_view_selected))
                } else {
                    itemView.imageViewSelected_constraintLayout.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun onbind(position: Int) {
//            var videoThumbnail = ThumbnailUtils.createVideoThumbnail(filePaths[position], MediaStore.Video.Thumbnails.MICRO_KIND)
            if (isMainAdapter) {
                val mediaController = MediaController(context)
                itemView.videoView.setVideoPath(filePaths[position])
                mediaController.setAnchorView(itemView.videoView)
                itemView.videoView.setMediaController(mediaController)
                itemView.videoView.setOnCompletionListener{
                    itemView.videoView.stopPlayback()
                }
            } else {
                Glide.with(context).load(filePaths[position]).thumbnail(0.1f).apply(requestOptions).dontTransform().into(itemView.findViewById<ImageView>(R.id.imageShowImageView))

//                itemView.findViewById<ImageView>(R.id.imageShowImageView).set(videoThumbnail!!)
                itemView.setOnClickListener {
                    imageViewModel.changeItemSelected(position)
                }
                if (imageViewModel.imageSelected.value == position) {
                    itemView.imageViewSelected_constraintLayout.setBackgroundColor((context as AppCompatActivity).getColor(R.color.image_view_selected))
                } else {
                    itemView.imageViewSelected_constraintLayout.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }
}