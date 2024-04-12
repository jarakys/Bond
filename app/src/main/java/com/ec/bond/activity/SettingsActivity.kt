package com.ec.bond.activity

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Pair
import android.view.View

import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.ui.settings.SettingsViewModel
import com.ec.bond.activity.ui.settings.main.MainSettingsFragment
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.di.Injectable
import kotlinx.android.synthetic.main.item_settings_account.*
import kotlinx.android.synthetic.main.item_settings_account.view.*
import kotlinx.android.synthetic.main.settings_activity.*
import java.io.File
import javax.inject.Inject


private const val TITLE_TAG = "settings"

class SettingsActivity : BaseActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback , Injectable {
    private lateinit var settingsViewModel: SettingsViewModel
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setupActionbar()
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, MainSettingsFragment())
                    .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.settings)
            }
        }

    }

    override fun onResume() {
        super.onResume()
//        var x = item_settings_account_include
        item_settings_account_include.account_shared_image.set(R.drawable.logo_old)
        Blackbox.account.photoProfilePath.observe(this, Observer {
            if (it.isNullOrEmpty() || !File(Blackbox.getDocumentsDir(this) + "/" + it).exists())
                item_settings_account_include.account_shared_image.set(R.drawable.contact)
            else
                item_settings_account_include.account_shared_image.set(BitmapFactory.decodeFile(Blackbox.getDocumentsDir(this) + "/" + it))
        })
        item_settings_account_include.setOnClickListener {
            val intent = Intent(this,AccountSettingsActivity::class.java)
//            val tansition = TransitionInflater.from(requireContext()).inflateTransition(R.transition)
            val pairs = listOf<Pair<View,String>>(
//                    Pair(account_shared_cardView,"account_imageCardView_transition"),
                    Pair(account_shared_image,"account_image_transition"),
                    Pair(account_shared_name,"account_name_transition")
            )

            val options = ActivityOptions.makeSceneTransitionAnimation(this, pairs[0],pairs[1])
//            val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), Pair(imageShared,ViewCompat.getTransitionName(imageShared)!!))
//            ActivityOptionsCompat.
            startActivity(intent, options.toBundle())
        }
    }
    private fun setupActionbar() {

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar = findViewById<Toolbar>(R.id.settings_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener(View.OnClickListener { onBackPressed() })
        toolbar.title = getString(R.string.settings)
        settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        settingsViewModel.toolbarTitle.observe(this, Observer {
            toolbar?.title = it
        })
    }

//    override fun onStart() {
//        super.onStart()
//        account_constrainsLayout.visibility = View.VISIBLE
//    }
//
//    override fun onPause() {
//        super.onPause()
//        account_constrainsLayout.visibility = View.GONE
//    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = pref.fragment?.let {
            supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                it
            ).apply {
                arguments = args
                setTargetFragment(caller, 0)
            }
        }
        // Replace the existing Fragment with the new Fragment
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()
        }
        title = pref.title
        return true
    }

}