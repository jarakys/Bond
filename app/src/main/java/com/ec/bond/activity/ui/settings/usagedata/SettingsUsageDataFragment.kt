package com.ec.bond.activity.ui.settings.usagedata

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ec.bond.R
import com.ec.bond.activity.ui.settings.SettingsViewModel
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBAccountSettings
import kotlinx.android.synthetic.main.settings_activity.*


class SettingsUsageDataFragment : PreferenceFragmentCompat() {
    lateinit var multiSelectListPreferenceMobileData: MultiSelectListPreference
    lateinit var multiSelectListPreferenceWifi: MultiSelectListPreference
    lateinit var settingsUsageDataViewModel: SettingsUsageDataViewModel
    lateinit var settingsViewModel: SettingsViewModel

    lateinit var selectedMobileDataSummary: String
    lateinit var selectedWifiSummary: String
    lateinit var generalConfiguration: BBAccountSettings
    var isChanged = false
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.data_usage_settings_preferences, rootKey)
//        requireActivity()?.account_constrainsLayout.visibility = View.GONE

        settingsViewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
        settingsViewModel.changetoolbarTitle(getString(R.string.data_usage_header))
        settingsUsageDataViewModel = ViewModelProvider(this).get(SettingsUsageDataViewModel::class.java)
        settingsUsageDataViewModel.isConfigUpdated.observe(this, androidx.lifecycle.Observer {
                isConfigUpdated -> Boolean.let {
            requireActivity()?.settings_progressBar.visibility = View.GONE
            Toast.makeText(activity,if (isConfigUpdated) getString(R.string.configuration_updated_successfully) else getString(R.string.general_error_msg) ,Toast.LENGTH_LONG).show()
                getConfgTypeFromNumber(isConfigUpdated)
            }
        })
        getGeneralConfiguration()
        val listener: Preference.OnPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference, selectedValues: Any?): Boolean {
                // newValue is the value you choose
                print(selectedValues)
                isChanged = true
                setPreferenceSummary(preference as MultiSelectListPreference, selectedValues as Set<String>)
                return true
            }
        }
        multiSelectListPreferenceMobileData = findPreference("when_using_mobile_data_preference")!!
        multiSelectListPreferenceWifi = findPreference("when_connected_on_wifi_preference")!!
        multiSelectListPreferenceMobileData.onPreferenceChangeListener = listener
        multiSelectListPreferenceWifi.onPreferenceChangeListener = listener
    }
    // listener for general configuration and get data if changed from server
    private fun getGeneralConfiguration() {
        Blackbox.account.settings.observe(this, Observer {
            generalConfiguration = it
            if(generalConfiguration.language == "" && generalConfiguration.autoDownloadPhotos == "")
                getConfgTypeFromNumber(false)
            else
                getConfgTypeFromNumber(true)
        })

        settingsUsageDataViewModel.fetchAccountSettings()
    }

    private fun setPreferenceSummary(multiPreference: MultiSelectListPreference, values: Set<String>) {
        var summary: String = getString(R.string.media_nothing)

        values.forEach {
            when (it) {
                getString(R.string.media_photos).toLowerCase() -> if (summary == getString(R.string.media_nothing)) summary = getString(R.string.media_photos) else summary += ", " + getString(R.string.media_photos)
                getString(R.string.media_audio).toLowerCase() -> if (summary == getString(R.string.media_nothing)) summary = getString(R.string.media_audio) else summary += ", " + getString(R.string.media_audio)
                getString(R.string.media_videos).toLowerCase() -> if (summary == getString(R.string.media_nothing)) summary = getString(R.string.media_videos) else summary += ", " + getString(R.string.media_videos)
                getString(R.string.media_documents).toLowerCase() -> if (summary == getString(R.string.media_nothing)) summary = getString(R.string.media_documents) else summary += ", " + getString(R.string.media_documents)
            }
        }

        if (multiPreference.key == "when_using_mobile_data_preference") {
            selectedMobileDataSummary = summary
        }else{
            selectedWifiSummary = summary
        }


        if (isChanged && this::selectedMobileDataSummary.isInitialized && this::selectedWifiSummary.isInitialized) {
            saveDataInServer()
        }

    }

    // this method preview details in Ui for both wifi and mobile data
    private fun showInDesign(summary: String, preference: MultiSelectListPreference) {
        if (summary.contains(getString(R.string.media_photos)) && summary.contains(getString(R.string.media_audio)) && summary.contains(getString(R.string.media_videos)) && summary.contains(getString(R.string.media_documents)))
            preference.summary = getString(R.string.media_all)
        else
            preference.summary = summary
    }

    // this method will save new settings configuration if user changes his configuration in server
    private fun saveDataInServer() {
        requireActivity()?.settings_progressBar.visibility = View.VISIBLE
        generalConfiguration.autoDownloadPhotos = getConfNumber(getString(R.string.media_photos))
        generalConfiguration.autoDownloadAudio = getConfNumber(getString(R.string.media_audio))
        generalConfiguration.autoDownloadVideos = getConfNumber(getString(R.string.media_videos))
        generalConfiguration.autoDownloadDocuments = getConfNumber(getString(R.string.media_documents))
        settingsUsageDataViewModel.setMediaConfigration(generalConfiguration)
    }
    /* this method get configuration number from selection media type from user to prepare save it to server
        and works as if media type chosen in mobile data or wifi and mobile this will return number 0
        if media type chosen in wifi only this will return number 1
        if media type not selected in both this will return number 2
     */
    private fun getConfNumber(mediaType: String): String {
        if (selectedMobileDataSummary.contains(mediaType) || (selectedMobileDataSummary.contains(mediaType) && selectedWifiSummary.contains(mediaType)) )
            return "0"
        else if (selectedWifiSummary.contains(mediaType))
            return "1"
        else
            return "2"
    }

    /* get type of configuration for each media type (Photos, Videos, Documentation, Audio) from number that saved in server
        and if media type config = 0 -> this means that media type automatically download from both mobile data and wifi
        if media type config = 1 -> this means that media type automatically downloaded from wifi only
        if media type config = 2 -> this means that media type don't automatically downloaded
     */
    private fun getConfgTypeFromNumber(configUpdated: Boolean) {

        var wifiList = mutableSetOf<String>()
        var dataList = mutableSetOf<String>()
        if(configUpdated) {
            selectedMobileDataSummary = getString(R.string.media_nothing)
            selectedWifiSummary = getString(R.string.media_nothing)
            if (generalConfiguration.autoDownloadAudio == "0") {
                if (selectedMobileDataSummary == getString(R.string.media_nothing)) selectedMobileDataSummary = getString(R.string.media_audio) else selectedMobileDataSummary += ", " + getString(R.string.media_audio)

                if (selectedWifiSummary == getString(R.string.media_nothing)) selectedWifiSummary = getString(R.string.media_audio) else selectedWifiSummary += ", " + getString(R.string.media_audio)

                dataList.add(getString(R.string.media_audio).toLowerCase())
                wifiList.add(getString(R.string.media_audio).toLowerCase())
            } else if (generalConfiguration.autoDownloadAudio == "1") {
                if (selectedWifiSummary == getString(R.string.media_nothing)) selectedWifiSummary = getString(R.string.media_audio) else selectedWifiSummary += ", " + getString(R.string.media_audio)
                wifiList.add(getString(R.string.media_audio).toLowerCase())
            }

            if (generalConfiguration.autoDownloadPhotos == "0") {
                if (selectedMobileDataSummary == getString(R.string.media_nothing)) selectedMobileDataSummary = getString(R.string.media_photos) else selectedMobileDataSummary += ", " + getString(R.string.media_photos)
                if (selectedWifiSummary == getString(R.string.media_nothing)) selectedWifiSummary = getString(R.string.media_photos) else selectedWifiSummary += ", " + getString(R.string.media_photos)

                dataList.add(getString(R.string.media_photos).toLowerCase())
                wifiList.add(getString(R.string.media_photos).toLowerCase())
            } else if (generalConfiguration.autoDownloadPhotos == "1") {
                if (selectedWifiSummary == getString(R.string.media_nothing)) selectedWifiSummary = getString(R.string.media_photos) else selectedWifiSummary += ", " + getString(R.string.media_photos)
                wifiList.add(getString(R.string.media_photos).toLowerCase())
            }

            if (generalConfiguration.autoDownloadVideos == "0") {
                if (selectedMobileDataSummary == getString(R.string.media_nothing)) selectedMobileDataSummary = getString(R.string.media_videos) else selectedMobileDataSummary += ", " + getString(R.string.media_videos)
                if (selectedWifiSummary == getString(R.string.media_nothing)) selectedWifiSummary = getString(R.string.media_videos) else selectedWifiSummary += ", " + getString(R.string.media_videos)

                dataList.add(getString(R.string.media_videos).toLowerCase())
                wifiList.add(getString(R.string.media_videos).toLowerCase())
            } else if (generalConfiguration.autoDownloadVideos == "1") {
                if (selectedWifiSummary == getString(R.string.media_nothing)) selectedWifiSummary = getString(R.string.media_videos) else selectedWifiSummary += ", " + getString(R.string.media_videos)

                wifiList.add(getString(R.string.media_videos).toLowerCase())
            }

            if (generalConfiguration.autoDownloadDocuments == "0") {
                if (selectedMobileDataSummary == getString(R.string.media_nothing)) selectedMobileDataSummary = getString(R.string.media_documents) else selectedMobileDataSummary += ", " + getString(R.string.media_documents)
                if (selectedWifiSummary == getString(R.string.media_nothing)) selectedWifiSummary = getString(R.string.media_documents) else selectedWifiSummary += ", " + getString(R.string.media_documents)

                dataList.add(getString(R.string.media_documents).toLowerCase())
                wifiList.add(getString(R.string.media_documents).toLowerCase())
            } else if (generalConfiguration.autoDownloadDocuments == "1") {
                if (selectedWifiSummary == getString(R.string.media_nothing)) selectedWifiSummary = getString(R.string.media_documents) else selectedWifiSummary += ", " + getString(R.string.media_documents)

                wifiList.add(getString(R.string.media_documents).toLowerCase())
            }
        }
        else {
            selectedWifiSummary = getString(R.string.no_internet_Connection)
            selectedMobileDataSummary = getString(R.string.no_internet_Connection)
        }
        showInDesign(selectedWifiSummary, multiSelectListPreferenceWifi)
        showInDesign(selectedMobileDataSummary, multiSelectListPreferenceMobileData)
        multiSelectListPreferenceWifi.values = wifiList
        multiSelectListPreferenceMobileData.values = dataList
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