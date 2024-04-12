package com.ec.bond.activity

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Fade
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ec.bond.R
import com.ec.bond.activity.ui.settings.accountinfo.AccountSettingsViewModel
import com.ec.bond.activity.ui.settings.accountinfo.MainAccountInfoAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBAccount
import com.ec.bond.blackbox.model.BBAccountSettings
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.UserAlertUtility
import com.ec.bond.utils.UserProgressUtility
import kotlinx.android.synthetic.main.activity_account_settings.*

data class AccountInfoBind(var title: String, var summary: String, var icon: Int, var setting: BBAccount)

class AccountSettingsActivity : BaseActivity(), MainAccountInfoAdapter.onSettingItemClick {

    val PICK_IMAGE = 1
    var adapterAcc: MainAccountInfoAdapter?=null
    private var firstEnter = true
    private lateinit var progressDialog: ProgressDialog
    private lateinit var accountSettingsViewModel: AccountSettingsViewModel
    var accountInfoList: List<AccountInfoBind> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstEnter = true
        setContentView(R.layout.activity_account_settings)
        account_info_recyclerView.layoutManager = LinearLayoutManager(this);
        accountSettingsViewModel = ViewModelProvider(this).get(AccountSettingsViewModel::class.java)

        account_info_toolbar.title = getString(R.string.profile)
        setSupportActionBar(account_info_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        account_info_toolbar.setNavigationOnClickListener { onBackPressed() }

        val fade = Fade()
        fade.excludeTarget(account_info_toolbar, true)
        fade.excludeTarget(android.R.id.statusBarBackground, true)
        fade.excludeTarget(android.R.id.navigationBarBackground, true)

        window.enterTransition = fade
        window.exitTransition = fade

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading")
        progressDialog.setCancelable(false)
        accountSettingsViewModel.getMainUserInfo()

        Blackbox.account.settings.observe(this, Observer {
            Log.e("account--",""+firstEnter)
            /*if (firstEnter) {
                firstEnter = false
                adapterAcc = MainAccountInfoAdapter(this, accountSettinglist(it), this)
                account_info_recyclerView.apply {
                    adapter = adapterAcc
                    postponeEnterTransition()
                    viewTreeObserver
                            .addOnPreDrawListener {
                                startPostponedEnterTransition()
                                true
                            }
                }
            } else {
                adapterAcc?.notifyItemRangeChanged(4, 2)
            }*/

        })
        Blackbox.account.photoProfilePath.observe(this, Observer {
            adapterAcc?.updateProfileImage()
        })

        accountSettingsViewModel.isImageSavedSuccessfully.observe(this, Observer {
            it?.let {
                //adapterAcc?.updateProfileImage()
            }
        })

        accountSettingsViewModel.isConfUpdated.observe(this, Observer {
            if(progressDialog.isShowing)
                progressDialog.dismiss()

        })

        accountSettingsViewModel._mainUserInfo.observe(this, Observer {
            Log.e("acoount---",""+it)
            adapterAcc = MainAccountInfoAdapter(this, accountSettinglist(it), this)
            account_info_recyclerView.apply {
                adapter = adapterAcc
                postponeEnterTransition()
                viewTreeObserver
                    .addOnPreDrawListener {
                        startPostponedEnterTransition()
                        true
                    }
            }
        })
    }

    private fun scheduleStartPostponedTransition(sharedElement: View) {
        sharedElement.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        sharedElement.viewTreeObserver.removeOnPreDrawListener(this)
                        startPostponedEnterTransition()
                        return true
                    }
                })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == PICK_IMAGE) {
//            val intent = data ?: return
//            val dataUri = intent.data ?: return
//
//            val currentStream: InputStream? = contentResolver.openInputStream(dataUri)
//            var bitmap = BitmapFactory.decodeStream(currentStream)
//            val divider = if  (bitmap.width > bitmap.height) {
//                bitmap.width / 1024
//            } else {
//                bitmap.height / 1024
//            }
//            bitmap = bitmap.scale(bitmap.width / divider, bitmap.height / divider)
//
//            val path = File(Blackbox.getDocumentsDir(this))
//            val outFile = File(path, getRealNameFromURI(dataUri))
//            val outputStream = FileOutputStream(outFile)
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//
//            currentStream?.close()
//            outputStream.close()
            val intent = data ?: return
            val dataUri = intent.data ?: return
            val path = CommonUtils.getPathFromUri(this, dataUri) ?: return
            val bitmap = BitmapFactory.decodeFile(path)
            if (path.endsWith("jpg") == false) {

            }
            val imagePath = CommonUtils.bitmapToJpegFile(this, bitmap).path
            accountSettingsViewModel.setPhotoProfile(imagePath)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        adapterAcc?.leaveAdapter()
    }

    private fun getRealNameFromURI(contentUri: Uri?): String? {
        val proj = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val cursor = contentResolver.query(contentUri!!, proj, null, null, null)
        val columnIndex: Int = cursor?.getColumnIndexOrThrow(proj[0])!!
        cursor.moveToFirst()
        return cursor.getString(columnIndex)
    }

    /**
     * generate list of setting info for adapter
     *
     * @param setting BBAccountSettings
     *
     * @return The list of setting info
     */
    fun accountSettinglist(setting: BBAccount): List<AccountInfoBind> {
        var accountInfoList = listOf(
                AccountInfoBind(getString(R.string.media_photo), Blackbox.account.photoProfilePath?.value
                        ?: "", 0, setting),
                AccountInfoBind(getString(R.string.name), Blackbox.account.name.value
                        ?: "No name", R.drawable.user_info_icon, setting),
                AccountInfoBind(getString(R.string.about), Blackbox.account.statusMessage.value
                        ?: "Hey there! I am using Masmak", R.drawable.info_icon, setting),
                AccountInfoBind(getString(R.string.phone), Blackbox.account.registeredNumber
                        ?: "", R.drawable.phone_account_info_icon, setting),
                AccountInfoBind("", getString(R.string.auto_login)
                ?: "", R.drawable.auto_login, setting)
                /*AccountInfoBind("",getString(R.string.last_seen),R.drawable.eye_icon, setting),*/

        )
        return accountInfoList
    }

    override fun onLastseenClick(settings: BBAccountSettings) {
        progressDialog.show()
        accountSettingsViewModel.updateAccountSettings(settings)

    }

    override fun onAutoLoginClick(settings: Boolean) {
       Log.e("autoLoging---",""+settings)
        if(settings){
            UserAlertUtility.showAlertDialog("Change Master Password",
                "We strongly recommend that you leave this option OFF, Setting this ON will let anyone, " +
                        "who has access to your phone, open the app and see its content.",
                this, { _, _ ->

                }, { _, _ -> UserAlertUtility.hideAlertDialog() }, getString(R.string.proceed), getString(R.string.cancel))
        }


    }

    override fun onCalenderClick(settings: BBAccountSettings) {

        val checkedItemType = when (settings.calendar) {
            getString(R.string.islamic_calendar).toLowerCase() -> 0
            getString(R.string.gregorian_calendar).toLowerCase() -> 1
            else -> -1
        }
        val values = arrayOf<CharSequence>(getString(R.string.islamic_calendar), getString(R.string.gregorian_calendar))
        val builder = AlertDialog.Builder(this)
        builder.setTitle("choose one")
        builder.setSingleChoiceItems(values, checkedItemType) { dialog, i ->
            progressDialog.show()
            dialog.dismiss()
            when (i) {
                0 -> {
                    settings.calendar = getString(R.string.islamic_calendar).toLowerCase()
                }
                1 -> {
                    settings.calendar = getString(R.string.gregorian_calendar).toLowerCase()
                }
            }
            accountSettingsViewModel.updateAccountSettings(settings)
            adapterAcc?.notifyItemChanged(5)

        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()

    }
}