package com.ec.bond.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import com.ec.bond.R
import com.ec.bond.model.SignUpViewModel
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.Constant
import com.ec.bond.utils.Injection
import com.ec.bond.utils.SharePreferenceUtility
import kotlinx.android.synthetic.main.activity_sign_up.edtOneTimePassword
import kotlinx.android.synthetic.main.activity_sign_up.imageOk
import kotlinx.android.synthetic.main.activity_sign_up.tv_done
import kotlinx.android.synthetic.main.activity_sign_up.tv_label_hint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MasterPasswordLoginActivity : BaseActivity() {
    private lateinit var viewModel: SignUpViewModel
    lateinit var viewModelFactory: ViewModelProvider.Factory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_master_password_login)
        requestPermissions();
        imageOk.visibility = View.INVISIBLE
        viewModelFactory = Injection.provideViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(SignUpViewModel::class.java)
        edtOneTimePassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        imageOk.setOnClickListener {

        }

        tv_done?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main){
                try {
                    var key=edtOneTimePassword?.text.toString()
                   /* val pwdConfSuccess = Blackbox.decrypt_pwdconf(this@MasterPasswordLoginActivity,key)
                    Log.e("Password---",key+"=="+SharePreferenceUtility.getPreferences(this@MasterPasswordLoginActivity, Constant.IS_ACTIVATION_CODE, SharePreferenceUtility.PREFTYPE_STRING) as String)
                    if(pwdConfSuccess){
                        tv_label_hint.setTextColor(getColor(R.color.white))
                        imageOk.setImageDrawable(getDrawable(R.drawable.ok))
                        startActivity(Intent(this@MasterPasswordLoginActivity, HomeActivity::class.java))
                        finish()
                    }*/

                    if (key.isEmpty() || key != SharePreferenceUtility.getPreferences(
                        this@MasterPasswordLoginActivity,
                        Constant.IS_ACTIVATION_CODE,
                        SharePreferenceUtility.PREFTYPE_STRING
                    )
                    ) {
                        tv_label_hint.setText(getString(R.string.your_password_doesnt_matches))
                        tv_label_hint.setTextColor(getColor(R.color.white))
                        imageOk.setImageDrawable(getDrawable(R.drawable.cross))
                        imageOk.visibility = View.VISIBLE
                    } else {
                        tv_label_hint.setTextColor(getColor(R.color.white))
                        imageOk.setImageDrawable(getDrawable(R.drawable.ok))
                        startActivity(
                            Intent(
                                this@MasterPasswordLoginActivity,
                                HomeActivity::class.java
                            )
                        )
                        finish()
                    }


                }catch (e:Exception){

                }

            }

        }


    }

    private fun requestPermissions() {
        val permissions = ArrayList<String>()
        if (!CommonUtils.hasPermissions(this, Manifest.permission.RECORD_AUDIO)) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!CommonUtils.hasPermissions(this, Manifest.permission.CAMERA)) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (!CommonUtils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !CommonUtils.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 10001);
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("permission", "Permission: " + permissions[0] + "was " + grantResults[0])
            //resume tasks needing this permission
        }

    }

}