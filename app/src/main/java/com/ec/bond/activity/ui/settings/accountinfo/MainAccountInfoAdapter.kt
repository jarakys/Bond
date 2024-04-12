package com.ec.bond.activity.ui.settings.accountinfo

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.AccountInfoBind
import com.ec.bond.activity.AccountSettingsActivity
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBAccountSettings
import com.ec.bond.utils.Constant
import com.ec.bond.utils.SharePreferenceUtility
import kotlinx.android.synthetic.main.item_account_header_settings.view.*
import kotlinx.android.synthetic.main.item_general_account_info_settings.view.*
import java.io.File


class MainAccountInfoAdapter(private var context: Context, var accountInfoList: List<AccountInfoBind>, var callback: onSettingItemClick?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var layoutInflater: LayoutInflater? = null
    private val TYPE_HEADER = 1
    private val TYPE_GENERAL = 2
    private val TYPE_EDITCELL = 3
    private val TYPE_TOGGLECELL = 4
    private var isEnter = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view: View
        return when (viewType) {
            TYPE_HEADER -> {
                view = layoutInflater!!.inflate(R.layout.item_account_header_settings, parent, false)
                HeaderInfoViewHolder(view)
            }
            TYPE_EDITCELL -> {
                view = layoutInflater!!.inflate(R.layout.item_language, parent, false)
                EditInfoViewHolder(view)
            }
            TYPE_TOGGLECELL -> {
                view = layoutInflater!!.inflate(R.layout.item_toggle, parent, false)
                ToggleInfoViewHolder(view)
            }
            else -> {
                view = layoutInflater!!.inflate(R.layout.item_general_account_info_settings, parent, false)
                GeneralInfoViewHolder(view)
            }
        }

    }

    override fun getItemCount(): Int {
        return accountInfoList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_HEADER) {
            (holder as HeaderInfoViewHolder).bind()
        } else if (getItemViewType(position) == TYPE_EDITCELL) {
            (holder as EditInfoViewHolder).bind()
        } else if (getItemViewType(position) == TYPE_TOGGLECELL) {
            (holder as ToggleInfoViewHolder).bind()
        } else {
            (holder as GeneralInfoViewHolder).bind()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            accountInfoList[position].title == context.getString(R.string.media_photo) -> {
                TYPE_HEADER
            }
            accountInfoList[position].summary == context.getString(R.string.tap_to_update_your_Calendar_type) -> {
                TYPE_EDITCELL
            }
            accountInfoList[position].summary == context.getString(R.string.last_seen) -> {
                TYPE_TOGGLECELL
            }
            accountInfoList[position].summary == context.getString(R.string.auto_login) -> {
                TYPE_TOGGLECELL
            }
            else -> TYPE_GENERAL
        }
    }

    inner class GeneralInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.account_info_details_title.text = accountInfoList[adapterPosition].title
            itemView.account_info_details_summary.text = accountInfoList[adapterPosition].summary
            itemView.account_info_details_imageView.set(accountInfoList[adapterPosition].icon)
        }
    }

    inner class HeaderInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind() {
            if (isEnter)
                itemView.select_new_image.startAnimation(AnimationUtils.loadAnimation(context, R.anim.camera_rotation))
            else {
                itemView.select_new_image.startAnimation(AnimationUtils.loadAnimation(context, R.anim.camera_rotation_exit))
            }

            if (accountInfoList[0].summary.isNullOrEmpty() || (!File(Blackbox.getDocumentsDir(context) + "/" + accountInfoList[0].summary).exists()&&!accountInfoList[0].summary.toString().contains("/"))) {
                itemView.accountInfo_shared_image.set(R.drawable.contact)
            } else {
                if(!accountInfoList[0].summary.toString().contains("/")){
                    Log.e("im---","image")
                    itemView.accountInfo_shared_image.set(BitmapFactory.decodeFile(Blackbox.getDocumentsDir(context) + "/" + accountInfoList[0].summary))
                }else{
                    var indexs=accountInfoList[0].summary.lastIndexOf("/")
                    var image=accountInfoList[0].summary.substring(indexs)
                    Log.e("im---",image)
                    itemView.accountInfo_shared_image.set(BitmapFactory.decodeFile(Blackbox.getDocumentsDir(context) + "/" + image))
                }

            }
            itemView.select_new_image.setOnClickListener {
                val chooserIntent = Intent(
                        Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                (context as AccountSettingsActivity).startActivityForResult(chooserIntent, 1)
            }
        }

    }

    inner class EditInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val outValue = TypedValue()
            context.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            //itemView.account_info_details_summary.text = accountInfoList[adapterPosition].setting.calendar
            itemView.account_info_details_imageView.set(accountInfoList[adapterPosition].icon)
            itemView.setBackgroundResource(outValue.resourceId)
            itemView.edit_imageView.visibility = View.VISIBLE
            itemView.separatorLine_view.visibility = View.GONE
            /*itemView.setOnClickListener {
                callback!!.onCalenderClick(accountInfoList[adapterPosition].setting)
            }*/
        }
    }

    inner class ToggleInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.account_info_details_summary.text = accountInfoList[adapterPosition].summary
            itemView.account_info_details_imageView.set(accountInfoList[adapterPosition].icon)

           var autoLogin = SharePreferenceUtility.getPreferences(context, Constant.IS_AUTO_LOGIN, SharePreferenceUtility.PREFTYPE_BOOLEAN) as Boolean
            itemView.visibility_switch.isChecked=autoLogin
            //itemView.visibility_switch.isChecked = accountInfoList[adapterPosition].setting.onlineVisibility == "Y"
            itemView.visibility_switch.setOnClickListener {
              //  accountInfoList[adapterPosition].setting.onlineVisibility = if (itemView.visibility_switch.isChecked) "Y" else "N"
               // callback!!.onLastseenClick(accountInfoList[adapterPosition].setting)
                if(itemView.visibility_switch.isChecked){
                    SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_AUTO_LOGIN, true)
                    callback!!.onAutoLoginClick(true)
                }else{
                    SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_AUTO_LOGIN, false)
                    callback!!.onAutoLoginClick(false)
                }
            }
        }
    }

    fun leaveAdapter() {
        isEnter = false
        notifyItemChanged(0)
    }

    fun updateProfileImage() {
        accountInfoList[0].summary = Blackbox.account.photoProfilePath.value ?: ""
        Log.e("image--",""+Blackbox.account.photoProfilePath.value)
        notifyItemChanged(0)
    }

    interface onSettingItemClick {
        fun onLastseenClick(settings: BBAccountSettings)
        fun onAutoLoginClick(settings:Boolean)
        fun onCalenderClick(settings: BBAccountSettings)
    }
}
