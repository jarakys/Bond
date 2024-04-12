package com.ec.bond.activity.ui.settings.chatsettings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.activity.BaseActivity
import com.ec.bond.adapter.ImagePreviewAdapter

import kotlinx.android.synthetic.main.activity_wallpaper_preview.*


class WallpaperPreviewActivity : BaseActivity(), View.OnClickListener {
    companion object {
        val DATA = "DATA"
    }

    private var position: Int = 0
    private lateinit var previewAdapter: ImagePreviewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_preview)
        setSupportActionBar(chat_setting_toolbar)
        setTitle(R.string.wallpaper_preview_label)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        btn_set.setOnClickListener(this)
        btn_cancel.setOnClickListener(this)

        initRecyclerView();
        addDataSet()
    }

    private fun addDataSet() {
        val data = WallpaperDataSource.createDataSet()
        previewAdapter.submitList(data)

        for (i in data.indices) {
            if (intent.extras!!.get(DATA) as Int == data[i]) {
                wallpaper_pager_list.scrollToPosition(i)
            }
        }
    }

    private fun initRecyclerView() {

        wallpaper_pager_list.apply {
            layoutManager = LinearLayoutManager(this@WallpaperPreviewActivity, LinearLayoutManager.HORIZONTAL, false)
            previewAdapter = ImagePreviewAdapter(this@WallpaperPreviewActivity)
            adapter = previewAdapter
        }
        wallpaper_pager_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    position = getCurrentItem()

                }
            }
        })
        var snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(wallpaper_pager_list)
    }

    private fun getCurrentItem(): Int {
        return (wallpaper_pager_list.layoutManager as LinearLayoutManager)
                .findFirstVisibleItemPosition()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(0,0)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(p0: View?) {
        if (p0 == btn_set) {

            Toast.makeText(this, "Wallpaper Set", Toast.LENGTH_LONG).show()
            overridePendingTransition(0,0)
        } else {
            overridePendingTransition(0,0)
        }
        finish()

    }
}