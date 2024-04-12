package com.ec.bond.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBPhoneNumber
import com.ec.bond.model.NewContactViewModel
import com.ec.bond.utils.validations.ValidationResult
import kotlinx.android.synthetic.main.activity_add_contact.*

class AddContactActivity : BaseActivity(),View.OnClickListener {
    lateinit var newContactViewModel: NewContactViewModel
    lateinit var pwdconfg_latest: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)
        newContactViewModel = ViewModelProvider(this).get(NewContactViewModel::class.java)
        setToolbar()
        setObserver()
        initListner()
    }

    private fun initListner() {
        addBt.setOnClickListener(this)
    }

    private fun setObserver() {
        newContactViewModel.validationResult?.observe(this, Observer {
            it.let {
                when(it){
                    ValidationResult.SUCCESS ->{
                        val _contact = BBContact()
                        _contact.registeredNumber = userNumberEt1.text.toString()
                        _contact.phonesjson = listOf(BBPhoneNumber("mobile", userNumberEt1.text.toString(), ""))
                        _contact.phonejsonreg = listOf(BBPhoneNumber("mobile", userNumberEt1.text.toString(), ""))
                        _contact.name = userNameEt.text.toString()
                        _contact.surname=userNameEt1.text.toString()
                        _contact.note=userTitleEt1.text.toString()
                        newContactViewModel.addContact(_contact)

                    }
                    ValidationResult.EMPTY_NAME ->{openToast(getString(R.string.enter_name))}
                    ValidationResult.EMPTY_PHONE ->{openToast(getString(R.string.enter_phone))}
                    ValidationResult.EMPTY_DESCRIPTION ->{openToast(getString(R.string.enter_surname))}
                    ValidationResult.EMPTY_TITLE ->{openToast(getString(R.string.enter_title))}
                    else -> {openToast(getString(R.string.internal_error))}

                }
            }
        })


        newContactViewModel.contactResult?.observe(this, Observer {
            it?.let {
                if(it){
                    finish()
                }
            }
        })
    }


    private fun openToast(message:String){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show()
    }

    private fun setToolbar() {
        val ab: Toolbar = findViewById(R.id.tb_toolbarsearch)
        setSupportActionBar(ab)
        val actionBar: ActionBar? = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.title = "New Contact"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
           R.id.addBt ->{
               newContactViewModel.checkValidData(userNameEt.text.toString(),userNameEt1.text.toString(),userTitleEt1.text.toString(),userNumberEt1.text.toString())
           }
        }
    }
}