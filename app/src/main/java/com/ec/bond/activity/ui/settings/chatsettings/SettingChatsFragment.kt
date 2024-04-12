package com.ec.bond.activity.ui.settings.chatsettings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ec.bond.R
import com.ec.bond.activity.ui.settings.SettingsViewModel
import kotlinx.android.synthetic.main.settings_activity.*

class SettingChatsFragment: PreferenceFragmentCompat() {
    lateinit var settingsViewModel: SettingsViewModel
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.chat_settings_preference, rootKey)
        settingsViewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
        settingsViewModel.changetoolbarTitle(getString(R.string.wallpaper_header))
        val listener: Preference.OnPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference, selectedValue: Any?): Boolean {
                // newValue is the value you choose
                return true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity()?.item_settings_account_include.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        requireActivity()?.item_settings_account_include.visibility = View.VISIBLE

    }
}