package com.ec.bond.activity.ui.settings.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ec.bond.R
import kotlinx.android.synthetic.main.help_settings_fragment.*
import kotlinx.android.synthetic.main.settings_activity.*

class HelpSettingsFragment: Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.help_settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatLive_linearLayout.setOnClickListener {
            Toast.makeText(requireContext(),"Chat Live Clicked",Toast.LENGTH_SHORT).show()
        }
        inAppCall_linearLayout.setOnClickListener {
            Toast.makeText(requireContext(),"In App Call Clicked",Toast.LENGTH_SHORT).show()

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