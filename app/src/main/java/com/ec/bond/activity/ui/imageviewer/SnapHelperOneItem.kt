package com.ec.bond.activity.ui.imageviewer

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider


class SnapHelperOneItem(val imageViewModel: ImageViewModel): LinearSnapHelper() {



    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager, velocityX: Int, velocityY: Int): Int {
        if (layoutManager !is ScrollVectorProvider) {
            return RecyclerView.NO_POSITION
        }
        val currentView: View = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
        val myLayoutManager = layoutManager as LinearLayoutManager
        val position1 = myLayoutManager.findFirstVisibleItemPosition()
        val position2 = myLayoutManager.findLastVisibleItemPosition()
        var currentPosition = layoutManager.getPosition(currentView)
        if (velocityX > 400) {
            currentPosition = position2
        } else if (velocityX < 400) {
            currentPosition = position1
        }
        imageViewModel.changeItemSelected(currentPosition)
        return if (currentPosition == RecyclerView.NO_POSITION) {
            RecyclerView.NO_POSITION
        } else {
            currentPosition
        }
    }


}