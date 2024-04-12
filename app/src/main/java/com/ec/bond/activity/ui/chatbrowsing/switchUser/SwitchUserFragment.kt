package com.ec.bond.activity.ui.chatbrowsing.switchUser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ec.bond.R

class SwitchUserFragment: Fragment() {
    lateinit var root: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_switch_user, container, false) as View
        findNavController().navigateUp()
        return root
    }
}