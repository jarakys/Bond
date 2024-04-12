package com.ec.bond.utils.validations

import java.io.File

class Validator constructor(val validationHelper: ValidationHelper) {

    fun validateEmail(email:String):ValidationResult{
        return  if (!validationHelper.isEmptyField(email))
            if(!validationHelper.isValid6Digit(email))
                if(validationHelper.isValidEmail(email))
                 ValidationResult.SUCCESS
                else ValidationResult.ERROR_EMAIL
            else ValidationResult.ERROR_EMAIL_6
        else ValidationResult.EMPTY_EMAIL
    }

    fun validPassword(password: String):ValidationResult{
        return if(!validationHelper.isEmptyField(password))
            if(validationHelper.isValidPassword(password))
            ValidationResult.SUCCESS
        else ValidationResult.ERROR_PASSWORD
        else ValidationResult.EMPTY_PASSWORD
    }

    fun validName(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
                ValidationResult.SUCCESS
        else ValidationResult.EMPTY_NAME
    }

    fun validPhone(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_PHONE
    }

    fun validPhoneNumber(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            if(validationHelper.isValid10Digit(name))
            ValidationResult.SUCCESS
        else ValidationResult.VALID_PHONE
        else ValidationResult.EMPTY_PHONE
    }

    fun validGender(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))

            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_GENDER
    }

    fun validTitle(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_TITLE
    }

    fun validLocation(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_LOCATION
    }

    fun validStartDate(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_START_DATE
    }

    fun validEndDate(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_END_DATE
    }

    fun validStartTime(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_START_TIME
    }

    fun validEndTime(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_END_TIME
    }

    fun validDescription(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.EMPTY_DESCRIPTION
    }

    fun validShortNote(name: String):ValidationResult{
        return if(!validationHelper.isEmptyField(name))
            ValidationResult.SUCCESS
        else ValidationResult.SHORT_NOTE
    }


    fun validPasswordSignup(password: String): ValidationResult{
        return  if(!validationHelper.isEmptyField(password))
            if(validationHelper.isValidPasswordMinLength(password))
                if(validationHelper.isValidPasswordMaxLength(password))
            ValidationResult.SUCCESS
        else ValidationResult.ERROR_PASSWORD_TOO_LONG
        else ValidationResult.ERROR_PASSWORD_TOO_SMALL
        else ValidationResult.EMPTY_PASSWORD

    }
}