package com.ec.bond.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.utils.validations.ValidationHelper
import com.ec.bond.utils.validations.ValidationResult
import com.ec.bond.utils.validations.Validator
import kotlinx.coroutines.launch

class NewContactViewModel:BaseViewModel<String>() {
    public val validationResult = MutableLiveData<ValidationResult>()
    public val contactResult = MutableLiveData<Boolean>()
    val validator= Validator(ValidationHelper)


    fun checkValidData( name:String, surname:String, title:String, phone:String){
        if(nameCheck(name)!=ValidationResult.SUCCESS){
            validationResult.postValue(nameCheck(name))
        }else if(phoneCheck(phone)!=ValidationResult.SUCCESS){
            validationResult.postValue(phoneCheck(phone))
        }
        else{
            validationResult.postValue(ValidationResult.SUCCESS)
        }

    }

    fun surnameCheck(surname:String):ValidationResult{
        return validator.validDescription(surname)
    }

    fun titleCheck(password:String):ValidationResult{
        return validator.validTitle(password)
    }

    fun nameCheck(name:String):ValidationResult{
        return validator.validName(name)

    }
    fun phoneCheck(phone:String):ValidationResult{
        return validator.validPhone(phone)
    }

    fun phoneNumberCheck(phone:String):ValidationResult{
        return validator.validPhoneNumber(phone)
    }

    fun addContact(contact: BBContact) = viewModelScope.launch {
        contactResult.value=  Blackbox.addContactAsync(contact)
    }

    fun updateContact(contact: BBContact) = viewModelScope.launch {
        contactResult.value=  Blackbox.updateContactAsync(contact)
    }
}