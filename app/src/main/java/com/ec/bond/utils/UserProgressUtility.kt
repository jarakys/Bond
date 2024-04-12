package com.ec.bond.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.ec.bond.R


class UserProgressUtility {
    companion object {
        private var mDialog: Dialog? = null

        @SuppressLint("InflateParams")
        fun showProgressDialog(pContext: Context?, pIsCancleable: Boolean = false) {
            // If our activity is going to finish then we don't need to show any progress.
            if (pContext != null && !(pContext as Activity).isFinishing) {
                if (mDialog == null || !this.mDialog!!.isShowing) {
                    mDialog = Dialog(pContext)
                    val inflate = LayoutInflater.from(pContext).inflate(R.layout.progress_dialog, null)
                    inflate.findViewById<TextView>(R.id.tv_loader).visibility = View.GONE
                    mDialog!!.setContentView(inflate)
                    mDialog!!.setCancelable(pIsCancleable)
                    mDialog!!.window!!.setBackgroundDrawable(
                            ColorDrawable(Color.TRANSPARENT)
                    )
                    mDialog!!.show()
                }
            }
        }

        fun hideProgressDialog() {
            if (mDialog != null && mDialog!!.isShowing) {
                mDialog!!.setCancelable(true)
                mDialog!!.dismiss()
                mDialog!!.hide()
                mDialog = null
            }
        }
    }
}