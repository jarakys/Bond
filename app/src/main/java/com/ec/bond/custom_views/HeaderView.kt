package com.ec.bond.custom_views

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ec.bond.R

class HeaderView : LinearLayout {

    interface ClickListener {
        fun onEditIconClick()
        fun onPersonAddIconClick()
    }

    var name: TextView? = null
    var edit_icon: ImageView? = null
    var person_add: ImageView? = null
    var clickListener: ClickListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onFinishInflate() {
        super.onFinishInflate()
        this.name = findViewById(R.id.name)
        this.edit_icon = findViewById(R.id.edit_icon)
        this.person_add = findViewById(R.id.person_add)
    }

    fun setClickListenerInstance(ClickListenerInstance: ClickListener) {
        this.clickListener = ClickListenerInstance
    }

    fun bindTo(name: String?, isGroupAdminOrCreator: Boolean) {
        this.name?.text = name
        if (isGroupAdminOrCreator) {
            edit_icon?.visibility = View.VISIBLE
            person_add?.visibility = View.VISIBLE
            edit_icon?.setOnClickListener {
                clickListener?.onEditIconClick()
            }
            person_add?.setOnClickListener {
                clickListener?.onPersonAddIconClick()
            }
        } else {
            edit_icon?.visibility = View.INVISIBLE
            person_add?.visibility = View.GONE
        }
    }

    fun updateName(name: String?) {
        this.name?.text = name
    }

    fun setTextSize(size: Float) {
        name?.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }
}
