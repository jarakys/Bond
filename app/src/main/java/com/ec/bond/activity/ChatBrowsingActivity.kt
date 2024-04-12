package com.ec.bond.activity

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class ChatBrowsingActivity : BaseActivity(), HasSupportFragmentInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_browsing)
        chatBrowsingViewModel = ViewModelProvider(this).get(ChatBrowsingViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        chatBrowsingViewModel.registerScreenshotObserver(this)
    }

    override fun onStop() {
        chatBrowsingViewModel.unregisterScreenshotObserver(this)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    external fun chat_list(pwd_conf: String): String
    external fun send_message(recipient: String, bodymsg: String, pwdconf: String, repliedto: String, repliedtotxt: String): String

    override fun supportFragmentInjector() = dispatchingAndroidInjector


    override fun onBackPressed() {
        val fragment = this.supportFragmentManager.findFragmentById(R.id.nav_host_fragment_cl) as? NavHostFragment
        val currentFragment = fragment?.childFragmentManager?.fragments?.get(0) as? IOnBackPressed
        currentFragment?.onBackPressed()?.takeIf { !it }?.let { super.onBackPressed() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        finish()
        startActivity(intent)
    }
}

interface IOnBackPressed {
    fun onBackPressed(): Boolean
}
