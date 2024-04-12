package com.ec.bond.activity.ui.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ec.bond.R
import com.ec.bond.blackbox.model.BBCall

class AudioCallFragment :Fragment(){
    val bbCall: BBCall = BBCall(true)
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.layout_call_receiver, container, false)
//        bbCall.bb_audio_send()
        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}