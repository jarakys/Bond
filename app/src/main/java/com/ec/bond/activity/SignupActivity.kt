package com.ec.bond

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.ec.bond.activity.BaseActivity
import com.ec.bond.activity.HomeActivity
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.model.SignUpViewModel
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.Constant
import com.ec.bond.utils.Injection
import com.ec.bond.utils.SharePreferenceUtility
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import java.io.FileOutputStream


class SignupActivity : BaseActivity() {




    private lateinit var viewModel: SignUpViewModel
    lateinit var viewModelFactory: ViewModelProvider.Factory


    private val cacert by lazy {
        val path = dataDir.resolve("tls-ca-chain.pem")
        assets.open("tls-ca-chain.pem").copyTo(FileOutputStream(path))
        path
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        edtOneTimePassword.setText("");
        requestPermissions();
        imageOk.visibility = View.INVISIBLE
        goBackLeft.isVisible =false
        goBack.isVisible =false

        viewModelFactory = Injection.provideViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SignUpViewModel::class.java)

        val myIntent = intent // gets the previously created intent
        val isLogin = myIntent.getStringExtra("isLogin")
        viewModel._lActivationCode.postValue(false)
        viewModel._lMasterPassword.postValue(false)
        viewModel._lMasterVerification.postValue(false)
        viewModel._lLogin.postValue(false)
        if (isLogin.isNullOrEmpty()){
            viewModel._lActivationCode.postValue(true)
        }
        else {
            viewModel._lLogin.postValue(true)
        }

        viewModel._lMasterVerification.postValue(false)


        tv_done.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || isStoragePermissionGranted()) {
                when {
                    viewModel._lActivationCode.value!! -> { viewModel.checkValidation(edtOneTimePassword.text.toString().trim(), this)}
                    viewModel._lMasterPassword.value!! -> {viewModel.chackMasterPassword(edtOneTimePassword.text.toString().trim(), this) }
                    viewModel._lMasterVerification.value!! -> {viewModel.chackMasterPasswordVerification(edtOneTimePassword.text.toString().trim(), this) }
                    viewModel._lLogin.value!! -> {viewModel.checkLogin(edtOneTimePassword.text.toString().trim(), this) }
                }
            }
        }

        edtOneTimePassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                viewModel.changeText(edtOneTimePassword.text.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        goBackLeft.setOnClickListener{

            viewModel.stepBack()
        }

        goBack.setOnClickListener{
            viewModel.stepBack()
        }




        viewModel.signUpErrorText.observe(this, Observer<String?> { value ->
            tv_label_hint.setText(value)
            if (value?.isEmpty()!!){
                //tv_label_hint.setTextColor(getColor(R.color.white))
            } else{
                //tv_label_hint.setTextColor(getColor(R.color.red))
            }
        })

        viewModel.lMasterPassword.observe(this, Observer<Boolean> { value ->
            if (value){
                tv_label_registration.setText(getString(R.string.Activation))
                tv_done.setText(getString(R.string.next))
                edtOneTimePassword.setHint(getString(R.string.enter_your_master_password))
                tv_label_hint.setText(getString(R.string.password_shall_have_a_minimum_of_4_characters_or_numbers))
                tv_label_hint.setTextColor(getColor(R.color.white))
                edtOneTimePassword.inputType = InputType.TYPE_NUMBER_VARIATION_PASSWORD or InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_PHONE
                edtOneTimePassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                edtOneTimePassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        })

        viewModel.lActivationCode.observe(this, Observer<Boolean> { value ->
            if (value) {
                tv_label_registration.setText(getString(R.string.Activation))
                tv_done.setText(getString(R.string.next))
                edtOneTimePassword.setHint(getString(R.string.enter_your_activation_code))
                tv_label_hint.setText("")
            }
       })

        viewModel.lMasterConformed.observe(this, Observer<Boolean> { value ->
            if (value) {


                    FirebaseInstanceId.getInstance().instanceId
                        .addOnCompleteListener(OnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                return@OnCompleteListener
                            }
                            val token = task.result?.token
                            GlobalScope.launch {
                                if (Blackbox.account.registerAsync(token!!)) {
                                    SharePreferenceUtility.saveStringPreferences(this@SignupActivity, Constant.IS_ACTIVATION_CODE, edtOneTimePassword.text.toString())
                                startActivity(Intent(this@SignupActivity, HomeActivity::class.java))
                                finish()
                            }
                            }

                        })




            }
        })

        viewModel.lMasterVerification.observe(this, Observer<Boolean> { value ->
            if (value){
                goBackLeft.isVisible =true
                goBack.isVisible =true

                tv_label_registration.setText(getString(R.string.verify_master_password))
                tv_done.setText(getString(R.string.set_master_password))
                edtOneTimePassword.setHint("")
                tv_label_hint.setText(getString(R.string.re_enter_the_master_password))
                tv_label_hint.setTextColor(getColor(R.color.white))
                imageOk.visibility = View.INVISIBLE

            } else {
                imageOk.visibility = View.INVISIBLE
                goBackLeft.isVisible =false
                goBack.isVisible =false
            }
        })

        viewModel.lMasterVerificationMatch.observe(this, Observer<Boolean> { value ->
            if (value){
                viewModel._signUpErrorText.postValue(getString(R.string.your_password_matches))
                imageOk.setImageDrawable(getDrawable(R.drawable.ok))
                imageOk.visibility = View.VISIBLE
            }
        })

        viewModel.lMasterVerificationDoesntMatch.observe(this, Observer<Boolean> { value ->
            if (value){
                tv_label_hint.setText(getString(R.string.your_password_doesnt_matches))
                tv_label_hint.setTextColor(getColor(R.color.red))
                imageOk.setImageDrawable(getDrawable(R.drawable.cross))
                imageOk.visibility = View.VISIBLE
            } else {
                tv_label_hint.setTextColor(getColor(R.color.white))
            }
        })


        viewModel._lResetTExt.postValue(false)
        viewModel.lResetTExt.observe(this, Observer<Boolean> { value ->
            if (value){
                edtOneTimePassword.setText("")
            }
        })

        viewModel.lLogin.observe(this, Observer<Boolean> { value ->
            if (value){
                tv_label_registration.setText(getString(R.string.Activation))
                tv_done.setText(getString(R.string.next))
                edtOneTimePassword.setHint(getString(R.string.enter_your_master_password))
                tv_label_hint.setText(getString(R.string.password_shall_have_a_minimum_of_4_characters_or_numbers))
                tv_label_hint.setTextColor(getColor(R.color.white))
                edtOneTimePassword.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                edtOneTimePassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        })

    }

    private fun checkPermission(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("permission", "Permission is granted");
            //File write logic here
            return true;
        }
        return false;
    }


    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("permission", "Permission is granted")
                true
            } else {
                Log.v("permission", "Permission is revoked")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("permission", "Permission is granted")
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("permission", "Permission: " + permissions[0] + "was " + grantResults[0])
            //resume tasks needing this permission
        }
    }



    private fun refresh() {

/*
        if (lMasterPassword) {

        } else if (lActivationCode) {
            tv_label_registration.setText(getString(R.string.Activation))
            tv_done.setText(getString(R.string.next))
            edtOneTimePassword.setHint(getString(R.string.enter_your_activation_code))
            tv_label_hint.setText("")
        } else if (lMasterVerification) {
            goBackLeft.isVisible =true
            goBack.isVisible =true

            tv_label_registration.setText(getString(R.string.verify_master_password))
            tv_done.setText(getString(R.string.set_master_password))
            edtOneTimePassword.setHint("")
            tv_label_hint.setText(getString(R.string.re_enter_the_master_password))
            tv_label_hint.setTextColor(getColor(R.color.white))
            imageOk.visibility = View.INVISIBLE
            if (lMasterVerificationMatch)  {
                _signUpErrorText.postValue(getString(R.string.your_password_matches))
                imageOk.setImageDrawable(getDrawable(R.drawable.ok))
                imageOk.visibility = View.VISIBLE
            }
            if (lMasterVerificationDoesntMatch)  {
                tv_label_hint.setText(getString(R.string.your_password_doesnt_matches))
                tv_label_hint.setTextColor(getColor(R.color.red))
                imageOk.setImageDrawable(getDrawable(R.drawable.cross))
                imageOk.visibility = View.VISIBLE
            }
        } else if (lMasterConformed) {
            startActivity(Intent(this@SIgnupActivity, HomeActivity::class.java))
            finish()
        }

        tv_label_hint.setText(lSignUpErrorText)
        if (lSignUpErrorText.isNotEmpty())
        {
            tv_label_hint.setTextColor(getColor(R.color.red))
        } else {
            tv_label_hint.setTextColor(getColor(R.color.white))
        }*/

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






}

