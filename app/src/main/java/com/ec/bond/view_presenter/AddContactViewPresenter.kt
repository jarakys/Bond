package com.ec.bond.view_presenter

interface AddContactViewPresenter {

    interface AddContactResultView {

        fun validateError()
        fun showProgressbar()
        fun hideProgressbar()
        fun onSuccessAddResult(reposnseModel: String)
        fun onSuccessListResult(reposnseModel: String)
        fun onError(throwable: Throwable)
        fun onError(messgae: String)
        fun checkInternet(): Boolean
        fun destroyActivityTestResult()
    }

    interface AddContactResultPresenter {
        fun requestAddContactResult(
                pwdconf: String,
                contactjson: String
        )

        fun requestListContactResult(search_text: String, contactid: Int, flagsearch: Int, limitsearch: Int, pwdconf: String)

    }
}