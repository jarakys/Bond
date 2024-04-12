package com.ec.bond.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.ec.bond.R
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject


class ContactListActivityNew : BaseActivity(), HasSupportFragmentInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_list)


    }

    private fun whiteNotificationBar(view: View) {
        var flags: Int = view.getSystemUiVisibility()
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        view.setSystemUiVisibility(flags)
        window.statusBarColor = Color.WHITE
    }

    override fun supportFragmentInjector() = dispatchingAndroidInjector
}


