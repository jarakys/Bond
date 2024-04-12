package com.ec.bond.activity.ui.settings.notifications

import android.media.MediaPlayer
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ec.bond.R
import com.ec.bond.activity.ui.settings.SettingsViewModel

class SettingsNotificationsFragment : PreferenceFragmentCompat() {

   lateinit var listPreferenceTonesMessage: ListPreference
   lateinit var listPreferenceTonesGroups: ListPreference
    lateinit var settingsViewModel: SettingsViewModel
//    var listPreference: ListPreference = ListPreference(context)
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notification_settings_preferences, rootKey)
        settingsViewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
        settingsViewModel.changetoolbarTitle(getString(R.string.notifications_header))
        listPreferenceTonesMessage = findPreference("tones_messages")!!
        listPreferenceTonesGroups = findPreference("tones_groups")!!
        val listener: Preference.OnPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference, selectedValue: Any?): Boolean {
                // newValue is the value you choose
                print(selectedValue)
                startMedia(selectedValue)
                return true
            }
        }
    listPreferenceTonesMessage.setOnPreferenceChangeListener(listener)
    listPreferenceTonesGroups.setOnPreferenceChangeListener(listener)
    }

    private fun startMedia(selectedValue: Any?) {
        lateinit var mp: MediaPlayer
        when (selectedValue) {
            "tone_1" -> mp = MediaPlayer.create(context, R.raw.tone_1)
            "tone_2" -> mp = MediaPlayer.create(context, R.raw.tone_2)
            "tone_3" -> mp = MediaPlayer.create(context, R.raw.tone_3)
            "tone_4" -> mp = MediaPlayer.create(context, R.raw.tone_4)
            "tone_5" -> mp = MediaPlayer.create(context, R.raw.tone_5)
            "tone_6" -> mp = MediaPlayer.create(context, R.raw.tone_6)
            "tone_7" -> mp = MediaPlayer.create(context, R.raw.tone_7)
            "tone_8" -> mp = MediaPlayer.create(context, R.raw.tone_8)
            "tone_9" -> mp = MediaPlayer.create(context, R.raw.tone_9)
            "tone_10" -> mp = MediaPlayer.create(context, R.raw.tone_10)
        }

        mp.start()
    }
}