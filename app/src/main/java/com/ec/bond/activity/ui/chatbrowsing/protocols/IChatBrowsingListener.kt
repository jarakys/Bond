package com.ec.bond.activity.ui.chatbrowsing.protocols

import android.view.View
import android.widget.ImageView

interface IChatBrowsingListener {
    fun showAlertDialog(layout: Int)
    fun showBallonDialog(layout: Int,view: View)
    fun scrollToSpecificMessage(position: Int)
    fun retrieveOldMessagesToGetMessage(msgId: String)
    fun setBigImageVisible(fromImageView: ImageView, isImage: Boolean, body: String, senderName: String, path: String? = null)
}