package com.ec.bond.activity.ui.home

import androidx.lifecycle.ViewModel
import com.ec.bond.blackbox.Blackbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject


class HomeViewModel @Inject constructor() : ViewModel() {
    fun getAccountConfig() = GlobalScope.launch(Dispatchers.IO) {
        Blackbox.account.fetchAccountConfigAsync()
    }
}