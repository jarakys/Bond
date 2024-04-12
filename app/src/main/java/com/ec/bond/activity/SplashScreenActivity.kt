package com.ec.bond.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.ec.bond.R
import com.ec.bond.SignupActivity
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBCall
import com.ec.bond.common.Const
import com.ec.bond.services.ForegroundService
import com.ec.bond.utils.Constant
import com.ec.bond.utils.NotificationUtility
import com.ec.bond.utils.SharePreferenceUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.internal.and
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class SplashScreenActivity : BaseActivity() {
    var autoLogin:Boolean=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        setContentView(R.layout.activity_splash_screen)

        val acceptCall = intent?.getBooleanExtra("ACCEPT_CALL", false)
        Log.e("binh","auto accept call 0 = "+acceptCall)
        if (acceptCall == true) {
            NotificationUtility.clearNotification(applicationContext, Const.INCOMMING_NOTIFICATION_ID)
            ForegroundService.stopService(applicationContext)
        }

        if (!isTaskRoot
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && intent.action != null
            && intent.action.equals(Intent.ACTION_MAIN)) {
            finish()
            return
        }
        //TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.loadLibrary("hello-jni")
        bb_set_home_env_dir(filesDir.path)
        autoLogin = SharePreferenceUtility.getPreferences(this, Constant.IS_AUTO_LOGIN, SharePreferenceUtility.PREFTYPE_BOOLEAN) as Boolean
        val isLogin: Boolean = SharePreferenceUtility.getPreferences(this, Constant.IS_REGISTER_DONE, SharePreferenceUtility.PREFTYPE_BOOLEAN) as Boolean
        if (isLogin) {
            FirebaseInstanceId.getInstance().instanceId
                    .addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            return@OnCompleteListener
                        }
                        val token = task.result?.token
                        register_presence(token ?: "")
                    })
        } else {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    fun register_presence(token: String) = lifecycleScope.launch(Dispatchers.Main) {
        val pwdConfSuccess = Blackbox.decrypt_pwdconf(this@SplashScreenActivity)
        if (pwdConfSuccess) {
            val registerSuccess = Blackbox.account.registerAsync(token)
            if (registerSuccess) {
                val accountInfoSuccess = Blackbox.account.fetchAccountInfoAsync()
                if (accountInfoSuccess) {
                    val fetchContactsSuccess = Blackbox.fetchContactsAsync()
                    if (fetchContactsSuccess) {
                        val fetchChatListSuccess = Blackbox.fetchChatListAsync()
                        if (fetchChatListSuccess) {
                            val fetchCallsHistorySuccess = Blackbox.fetchCallsHistoryAsync()
                            if (fetchCallsHistorySuccess) {
                                async { Blackbox.fetchNotificationsSounds() }
                                intent?.let {
                                    if (it?.hasExtra("hasVideo")) {
                                        val hasvideo = it?.getBooleanExtra("hasVideo", false)
                                        val acceptCall = it?.getBooleanExtra("ACCEPT_CALL", false)

                                        startActivity(
                                            Intent(
                                                this@SplashScreenActivity,
                                                HomeActivity::class.java
                                            ).putExtra("hasVideo", hasvideo)
                                                .putExtra("ACCEPT_CALL", acceptCall)
                                        )
                                    } else {
                                        if (!autoLogin) {
                                            startActivity(
                                                Intent(
                                                    this@SplashScreenActivity,
                                                    MasterPasswordLoginActivity::class.java
                                                )
                                            )
                                        } else {
                                            startActivity(
                                                Intent(
                                                    this@SplashScreenActivity,
                                                    HomeActivity::class.java
                                                )
                                            )
                                        }
                                    }
                                }?: kotlin.run {

                                    if(!autoLogin){
                                        startActivity(Intent(this@SplashScreenActivity, MasterPasswordLoginActivity::class.java))
                                    }else{
                                        startActivity(Intent(this@SplashScreenActivity, HomeActivity::class.java))
                                    }

                                }

                                //startActivity(Intent(this@SplashScreenActivity, HomeActivity::class.java))
                                Log.e("SplashScreenActivity", "finish")
                                finish()
                            } else {
                                showErrorToast(getString(R.string.general_error_msg))
                            }
                        } else {
                            showErrorToast(getString(R.string.general_error_msg))
                        }
                    } else {
                        showErrorToast(getString(R.string.general_error_msg))
                    }
                } else {
                    showErrorToast(getString(R.string.general_error_msg))
                }
            } else {
                showErrorToast(getString(R.string.general_error_msg))
            }
        } else {
            showErrorToast(getString(R.string.general_error_msg))
        }
    }

    private fun showErrorToast(errorMsg: String) {
//        Toast.makeText(this@SplashScreenActivity,getString(R.string.general_error_msg),Toast.LENGTH_SHORT)
    }

    private fun showLogin() {
        val myIntent = Intent(this, SignupActivity::class.java)
        myIntent.putExtra("isLogin", "True")
        startActivity(myIntent)
    }

    fun getSha256Hash(password: String): String? {
        return try {
            var digest: MessageDigest? = null
            try {
                digest = MessageDigest.getInstance("SHA-256")
            } catch (e1: NoSuchAlgorithmException) {
                e1.printStackTrace()
            }
            digest!!.reset()
            bin2hex(digest.digest(password.toByteArray()))
        } catch (ignored: Exception) {
            null
        }
    }

    private fun bin2hex(data: ByteArray): String? {
        val hex = StringBuilder(data.size * 2)
        for (b in data) hex.append(String.format("%02x", b and 0xFF))
        return hex.toString()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    external fun bb_set_home_env_dir(path: String)

}




