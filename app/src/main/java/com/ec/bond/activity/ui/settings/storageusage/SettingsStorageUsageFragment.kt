package com.ec.bond.activity.ui.settings.storageusage

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ec.bond.R
import kotlinx.android.synthetic.main.settings_activity.*

class SettingsStorageUsageFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.storage_usage_settings_preferences, rootKey)

        var pref = findPreference<Preference>("storage_value_data_storage_usage")
        pref?.summary = "2.0 MB" // prepare for getting data from server
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