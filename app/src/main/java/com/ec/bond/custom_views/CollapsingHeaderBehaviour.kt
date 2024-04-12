package com.ec.bond.custom_views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.ec.bond.R
import kotlin.math.abs


class CollapsingHeaderBehaviour : CoordinatorLayout.Behavior<HeaderView> {
    private var mContext: Context
    private var mStartMarginLeft = 0
    private var mEndMarginLeft = 0
    private var mMarginRight = 0
    private var mStartMarginBottom = 0
    private var mTitleStartSize = 0f
    private var mTitleEndSize = 0f
    private var isHide = false

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mContext = context
    }

    constructor(context: Context?, attrs: AttributeSet, mContext: Context) : super(context, attrs) {
        this.mContext = mContext
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: HeaderView, dependency: View): Boolean {
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: HeaderView, dependency: View): Boolean {
        shouldInitProperties()
        val maxScroll: Int = (dependency as AppBarLayout).totalScrollRange
        val percentage = abs(dependency.y) / maxScroll.toFloat()
        var childPosition: Float = ((dependency.height + dependency.y) - child.height - (getToolbarHeight(mContext) - child.height) * percentage / 2)
        childPosition -= mStartMarginBottom * (1f - percentage)
        val lp: CoordinatorLayout.LayoutParams = child.layoutParams as CoordinatorLayout.LayoutParams
        if (abs(dependency.y) >= maxScroll.toFloat() / 2.8) {
            val layoutPercentage = (abs(dependency.y) - maxScroll / 2) / abs(maxScroll / 2)
            lp.leftMargin = (layoutPercentage * mEndMarginLeft).toInt() + mStartMarginLeft
            child.setTextSize(getTranslationOffset(mTitleStartSize, mTitleEndSize, layoutPercentage))
        } else {
            lp.leftMargin = (-0.2823 * mEndMarginLeft).toInt() + mStartMarginLeft
        }
        if (abs(dependency.y) >= maxScroll.toFloat() / 1.92) {
            val layoutPercentage = (abs(dependency.y) - maxScroll / 2) / abs(maxScroll / 2)
            lp.rightMargin = (layoutPercentage * mMarginRight * 10).toInt() + 12
        } else {
            lp.rightMargin = mMarginRight
        }
        child.layoutParams = lp
        child.y = childPosition
        if (isHide && percentage < 1) {
            child.visibility = View.VISIBLE
            isHide = false
        } else if (!isHide && percentage == 1f) {
            child.visibility = View.GONE
            isHide = true
        }
        return true
    }

    private fun getTranslationOffset(expandedOffset: Float, collapsedOffset: Float, ratio: Float): Float {
        return expandedOffset + ratio * (collapsedOffset - expandedOffset)
    }

    private fun shouldInitProperties() {
        if (mStartMarginLeft == 0) {
            mStartMarginLeft = mContext.resources.getDimensionPixelOffset(R.dimen._16sdp)
        }
        if (mEndMarginLeft == 0) {
            mEndMarginLeft = mContext.resources.getDimensionPixelOffset(R.dimen._40sdp)
        }
        if (mStartMarginBottom == 0) {
            mStartMarginBottom = mContext.resources.getDimensionPixelOffset(R.dimen._14sdp)
        }
        if (mMarginRight == 0) {
            mMarginRight = mContext.resources.getDimensionPixelOffset(R.dimen._4sdp)
        }
        if (mTitleStartSize == 0f) {
            mTitleEndSize = mContext.resources.getDimensionPixelSize(R.dimen._16sdp).toFloat()
        }
        if (mTitleStartSize == 0f) {
            mTitleStartSize = mContext.resources.getDimensionPixelSize(R.dimen._24sdp).toFloat()
        }
    }

    companion object {
        fun getToolbarHeight(context: Context): Int {
            var result = 0
            val tv = TypedValue()
            if (context.theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
                result = TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
            }
            return result
        }
    }
}
