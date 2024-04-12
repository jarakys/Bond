package com.ec.bond.utils

import android.content.Context
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import com.ec.bond.R
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.OnBalloonClickListener
import java.util.*

object BallonUtils {
    fun getNavigationBalloon(
        context: Context,
        onBalloonClickListener: OnBalloonClickListener,
        lifecycleOwner: LifecycleOwner
    ): Balloon {
        return Balloon.Builder(context)
            .setArrowSize(10)
            .setArrowPosition(0.5f)
            .setTextSize(15f)
            .setCornerRadius(4f)
            .setLayout(R.layout.options_dialog)
            .setBackgroundColorResource(R.color.green)
            .setOnBalloonClickListener(onBalloonClickListener)
            .setBalloonAnimation(BalloonAnimation.ELASTIC)
            .setLifecycleOwner(lifecycleOwner)
            .build()
    }


    fun getNavigationBalloonFloating(
        context: Context,
        onBalloonClickListener: OnBalloonClickListener,
        lifecycleOwner: LifecycleOwner
    ): Balloon {
        return Balloon.Builder(context)
            .setArrowSize(10)
            .setArrowPosition(0.5f)
            .setTextSize(15f)
            .setCornerRadius(4f)
            .setLayout(R.layout.options_dialog_chat)
            .setBackgroundColorResource(R.color.green)
            .setOnBalloonClickListener(onBalloonClickListener)
            .setBalloonAnimation(BalloonAnimation.ELASTIC)
            .setLifecycleOwner(lifecycleOwner)
            .build()
    }


    fun isRtlLayout(): Boolean {
        return TextUtilsCompat.getLayoutDirectionFromLocale(
            Locale.getDefault()
        ) == ViewCompat.LAYOUT_DIRECTION_RTL
    }
}