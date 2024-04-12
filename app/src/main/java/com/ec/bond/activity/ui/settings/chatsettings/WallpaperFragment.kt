package com.ec.bond.activity.ui.settings.chatsettings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.ec.bond.R
import com.ec.bond.activity.IOnBackPressed
import com.ec.bond.adapter.WallpaperGridAdapter
import kotlinx.android.synthetic.main.fragment_wallpaper.*

class WallpaperFragment : Fragment(), IOnBackPressed , WallpaperGridAdapter.GridImageItemClickListener {
    private lateinit var wallpaperAdapter: WallpaperGridAdapter
    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_wallpaper,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        addDataSet()
    }
    private fun addDataSet() {
        val data = WallpaperDataSource.createDataSet()
        wallpaperAdapter.submitList(data)
    }

    private fun initRecyclerView() {
        wallpaper_recycler_view.apply {
            layoutManager = GridLayoutManager(activity,3)
            wallpaperAdapter = WallpaperGridAdapter(activity!!,this@WallpaperFragment)
            adapter = wallpaperAdapter
        }
    }

    override fun onItemClick(item: Int, imageView: ImageView) {
        val detailIntent = Intent(activity, WallpaperPreviewActivity::class.java)
        detailIntent.putExtra(WallpaperPreviewActivity.DATA, item)
        detailIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(detailIntent)
    }
}