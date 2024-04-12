package com.ec.bond.activity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ec.bond.R
import com.ec.bond.activity.ui.home.HomeViewModel
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBCall
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class HomeActivity : BaseActivity() , HasSupportFragmentInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>
    lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        getData();

    }

    private fun getData() {
        Log.e("has_video--------",""+"no")
        intent?.let {
            Log.e("has_video--------",""+it?.hasExtra("hasVideo"))
           if(it?.hasExtra("hasVideo")) {
               val autoAcceptCall = it.getBooleanExtra("ACCEPT_CALL", false)
               val call = BBCall(false, it?.getBooleanExtra("hasVideo",false))
               Log.e("binh","auto accept call = "+autoAcceptCall)
               Blackbox.currentCall=call
               Blackbox.openCallActivity(call,this,true, autoAcceptCall)
            }
        }

    }

//    override fun onStart() {
//        super.onStart()
//        homeViewModel.setOnlineStatus(BBStatus.online)
//        Log.i("Status","contact ID = ${Blackbox.account.registeredNumber} and onStart()" )
//
//    }

    override fun supportFragmentInjector() = dispatchingAndroidInjector
//    override fun onDestroy() {
//        super.onDestroy()
//        homeViewModel.setOnlineStatus(BBStatus.offline)
//        Log.i("Status","contact ID = ${Blackbox.account.registeredNumber} and onDestroy()" )
//
//    }

//    override fun onStop() {
//        super.onStop()
//        Log.i("Status","contact ID = ${Blackbox.account.registeredNumber} and onStop()" )
//
//    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

    }

}
