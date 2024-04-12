package com.ec.bond.activity.ui.settings.main

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ec.bond.BuildConfig
import com.ec.bond.R
import com.ec.bond.activity.AccountSettingsActivity
import kotlinx.android.synthetic.main.item_settings_account.*
import kotlinx.android.synthetic.main.item_settings_account.view.*

class MainSettingsFragment : PreferenceFragmentCompat() {
    lateinit var mainSettingsViewModel: MainSettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_settings_preferences, rootKey)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.item_settings_account, container, false)
        val imageShared = view.account_shared_image
        val accountPreference: Preference? = findPreference<Preference>("account_preference")
        accountPreference?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), AccountSettingsActivity::class.java)
//            val tansition = TransitionInflater.from(requireContext()).inflateTransition(R.transition)
            val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), Pair(imageShared, "account_image_transition"))
//            val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), Pair(imageShared,ViewCompat.getTransitionName(imageShared)!!))
//            ActivityOptionsCompat.
            requireContext().startActivity(intent, options.toBundle())
//            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);


            return@setOnPreferenceClickListener true
        }
        val app_version: Preference? = findPreference<Preference>("app_version")
        app_version?.summary = BuildConfig.VERSION_NAME


        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }
}