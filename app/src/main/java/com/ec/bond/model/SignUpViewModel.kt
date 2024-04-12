package com.ec.bond.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.ec.bond.R
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.CommonUtils.getByte
import com.ec.bond.utils.Constant
import com.ec.bond.utils.SharePreferenceUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.*
import java.util.regex.Pattern

class SignUpViewModel:  BaseViewModel<String>()
{
    var keySecond: String = ""
    var keyFirst: String = ""


    val _lMasterVerification = MutableLiveData<Boolean>()
    val _lMasterVerificationMatch = MutableLiveData<Boolean>()
    val _lMasterVerificationDoesntMatch = MutableLiveData<Boolean>()
    val _lMasterConformed = MutableLiveData<Boolean>()
    val _lMasterPassword = MutableLiveData<Boolean>()
    val _lActivationCode = MutableLiveData<Boolean>()
    val _signUpErrorText = MutableLiveData<String?>()
    val _lResetTExt = MutableLiveData<Boolean>()
    val _lLogin = MutableLiveData<Boolean>()

    val signUpErrorText: LiveData<String?> get() = _signUpErrorText
    val lResetTExt: LiveData<Boolean> get() = _lResetTExt

    val lMasterVerification: LiveData<Boolean> get() = _lMasterVerification
    val lMasterVerificationMatch: LiveData<Boolean> get() = _lMasterVerificationMatch
    val lMasterVerificationDoesntMatch: LiveData<Boolean> get() = _lMasterVerificationDoesntMatch
    val lMasterConformed: LiveData<Boolean> get() = _lMasterConformed
    val lMasterPassword: LiveData<Boolean> get() = _lMasterPassword
    val lActivationCode: LiveData<Boolean> get() = _lActivationCode
    val lLogin: LiveData<Boolean> get() = _lLogin



    fun checkValidation(otp_in: String, context: Context) {

        var otp: String = otp_in

        //otp = "xxx-012345678"

        if (otp.isEmpty()) {
            //Toast.makeText(this@SIgnupActivity, "Please enter activation code", Toast.LENGTH_LONG).show()
            _signUpErrorText.postValue("Please enter activation code")
            return
        }

        if (otp.indexOf("-")<=0){
            //Toast.makeText(this@SIgnupActivity, "Activation code must include -", Toast.LENGTH_LONG).show()
            _signUpErrorText.postValue("Activation code must include -")
            return
        }

        val mobile = otp.substring(0, otp.indexOf("-"))
        otp = otp.substring(otp.indexOf("-") + 1)

        if (otp.length < 8) {
            //Toast.makeText(this@SIgnupActivity, "Please enter valid otp number", Toast.LENGTH_LONG).show()
            _signUpErrorText.postValue("Please enter valid otp number")
            return
        }

        if (mobile.length < 3) {
            //Toast.makeText(this@SIgnupActivity, "Please enter valid mobile number", Toast.LENGTH_LONG).show()
            _signUpErrorText.postValue("Please enter valid mobile number")
            return
        }

        if (mobile.isEmpty()) {
            //Toast.makeText(this@SIgnupActivity, "Please enter mobile number", Toast.LENGTH_LONG).show()
            _signUpErrorText.postValue("Please enter mobile number")
            return
        }

        val output: String = bb_signup_newdevice(mobile, otp, otp)
        Log.e("bb_signup_newdevice", output)

        try {
            val jsonObject = JSONObject(output)

            if (jsonObject.has("answer")) {
                if (jsonObject.getString("answer") == "OK") {

                    val pwdconfg = jsonObject.getString("pwdconf")
                    Blackbox.pwdConf = CommonUtils.decode(pwdconfg)
                    SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_REGISTER_DONE, true)
                    //+ SergeiKudrya
                    _lActivationCode.postValue(false)
                    _lMasterPassword.postValue(true)
                    _lResetTExt.postValue(true)
                    //- SergeiKudrya

                } else if (jsonObject.getString("answer").equals("KO")) {
                    SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_REGISTER_DONE, false)
                    _signUpErrorText.postValue(jsonObject.getString("message"))
                }
            } else {
                SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_REGISTER_DONE, false)
                _signUpErrorText.postValue("Something went wrong")
            }
        } catch (e: Exception) {
            _signUpErrorText.postValue("Something went wrong")
        }

    }

     fun stepBack() {
        _lMasterPassword.postValue(true)
        _lMasterVerification.postValue(false)
        _lMasterVerificationMatch.postValue(false)
        _lMasterVerificationDoesntMatch.postValue(false)
        _lResetTExt.postValue(true)
    }

    fun chackMasterPassword(otp: String, context: Context) {
        keyFirst  =  otp
        var pattern=Pattern.compile("[0-9]")
        var matcher=pattern.matcher(keyFirst)

        if (keyFirst.length < 4) {
            _signUpErrorText.postValue(context.getString(R.string.password_shall_have_a_minimum_of_4_characters_or_numbers))
            return
        }else if(!keyFirst.matches("[0-9*+/%.-]*".toRegex())){
            _signUpErrorText.postValue(context.getString(R.string.password_shall_have_a_minimum_of_contain_characters_or_numbers))
            return
        }else if(matcher.find()){
            _signUpErrorText.postValue(context.getString(R.string.password_shall_have_a_minimum_of_contain_characters_or_numbers))
        }

        _lActivationCode.postValue(false)
         _lMasterPassword.postValue(false)
         _lMasterConformed.postValue(false)
         _lMasterVerification.postValue(true)
         _lResetTExt.postValue(true)
    }

    fun chackMasterPasswordVerification(otp: String, context: Context) {

        keySecond  =  otp

        if (!keyFirst.equals(keySecond)){
            //Toast.makeText(this@SIgnupActivity, getString(R.string.password_shall_have_a_minimum_of_4_characters_or_numbers), Toast.LENGTH_LONG).show()
            _signUpErrorText.postValue(context.getString(R.string.password_shall_have_a_minimum_of_4_characters_or_numbers))
            return
        }

        val path = Blackbox.getDocumentsDir(context)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch {
            Blackbox.pwdConf?.let {
                val pwdConfDataLen = it.toByteArray().count() + 64
                val pwdconfenc: ByteArray = bb_encrypt_pwdconf(it, keySecond, pwdConfDataLen, path)

                val file = File(path + "/pwdconf.enc")
                file.writeBytes(pwdconfenc);

                _lActivationCode.postValue(false)
                _lMasterPassword.postValue(false)
                _lMasterVerification.postValue(false)
                _lMasterConformed.postValue(true)
            }
        }

    }

    fun checkLogin(otp: String, context: Context) {

        keySecond  =  otp

        if (keySecond.length < 4){
            //Toast.makeText(this@SIgnupActivity, getString(R.string.password_shall_have_a_minimum_of_4_characters_or_numbers), Toast.LENGTH_LONG).show()
            _signUpErrorText.postValue(context.getString(R.string.password_shall_have_a_minimum_of_4_characters_or_numbers))
            return
        }



        val path = CommonUtils.getFilesDirectory(context)+ "/pwdconf.enc"
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch {
            val data: ByteArray = getByte(path)

            if (Blackbox.decrypt_pwdconf(context)) {
                _lActivationCode.postValue(false)
                _lMasterPassword.postValue(false)
                _lMasterVerification.postValue(false)
                _lMasterConformed.postValue(true)
            } else {
                _signUpErrorText.postValue("error with bb_decrypt_pwdconf")
            }
        }

    }

    external fun bb_signup_newdevice(mobile: String, otp: String, smsotp: String): String
    external fun bb_encrypt_pwdconf(pwdconf: String, key: String, pwdconfenclen: Int, tmpfolder: String): ByteArray


    fun changeText(otp: String) {
        if (_lMasterVerification.value!!) {
            if (!keyFirst.isEmpty()) {
                if (otp.isEmpty()){
                    _lMasterVerificationMatch.postValue(false)
                    _lMasterVerificationDoesntMatch.postValue(false)
                    _lMasterVerification.postValue(true)

                } else {
                    if (otp.equals(keyFirst)) {

                        _lMasterVerificationMatch.postValue(true)
                        _lMasterVerificationDoesntMatch.postValue(false)
                    } else {
                        _lMasterVerificationMatch.postValue(false)
                        _lMasterVerificationDoesntMatch.postValue(true)
                    }
                }

            }
        }
    }

}