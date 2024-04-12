package com.ec.bond.activity.ui.calls.CallInfo

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.blackbox.model.callsHistory.BBCallDirection
import com.ec.bond.blackbox.model.callsHistory.BBCallHistory
import com.ec.bond.blackbox.model.callsHistory.BBCallType
import com.ec.bond.utils.DateTimeUtils
import kotlinx.android.synthetic.main.fragment_call_info.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*


class CallInfoFragment: Fragment() {
    private val lastVoiceCallHistory: BBCallHistory? = requireActivity().intent.getParcelableExtra<BBCallHistory>("callDetail")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_call_info, container, false)
        return root
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val currentActivity = activity as AppCompatActivity
        currentActivity.setSupportActionBar(call_info_toolbar)
        currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        currentActivity.supportActionBar?.title = getString(R.string.call_info)
        call_info_toolbar.setNavigationOnClickListener(View.OnClickListener { currentActivity.onBackPressed() })

        var simpleDate: String = ""
        var simpleTime: String = ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val current = LocalDateTime.now()
            DateTimeUtils.parseUTCDateFormat(lastVoiceCallHistory?.dtsetup.orEmpty())?.let {
                simpleDate = SimpleDateFormat("MMMM dd").apply { timeZone = TimeZone.getDefault() }.format(it)
            }
            DateTimeUtils.parseUTCDateFormat(lastVoiceCallHistory?.dtsetup.orEmpty())?.let {
                simpleTime = SimpleDateFormat("h:mm a").apply { timeZone = TimeZone.getDefault() }.format(it)
            }
//            var answer: String =  current.format(formatter)
        } else {
//            var date = Date()
//            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//            val date = formatter.parse(lastVoiceCall.dtsetup)
            simpleDate = lastVoiceCallHistory?.dtsetup?.split(" ")!![0]
            simpleTime = lastVoiceCallHistory?.dtsetup?.split(" ")!![1]

        }
        val durationSeconds = lastVoiceCallHistory?.duration!!.toInt() % 60
        val durationMinutes = lastVoiceCallHistory?.duration!!.toInt() / 60 % 60
        val durationHours = lastVoiceCallHistory?.duration!!.toInt() / 60 / 60
        contact_name_txt.text = lastVoiceCallHistory?.name
        voice_or_video_txt.text = if(lastVoiceCallHistory?.type == BBCallType.Video) getString(R.string.video_call) else getString(R.string.voice_call)
        call_date_txt.text = simpleDate
        call_time_txt.text = simpleTime
        status_imageView.set(if(lastVoiceCallHistory?.directionType == BBCallDirection.Outgoing) R.drawable.done_call
                            else if(lastVoiceCallHistory?.directionType == BBCallDirection.Incoming && lastVoiceCallHistory?.duration == "0") R.drawable.missed_call
                            else R.drawable.done_inbounding_call)
        status_txt.text =   if(lastVoiceCallHistory?.directionType == BBCallDirection.Incoming && lastVoiceCallHistory?.duration == "0") getString(R.string.call_missed)
                            else if(lastVoiceCallHistory?.directionType == BBCallDirection.Outgoing) getString(R.string.call_outgoing)
                            else getString(R.string.call_incoming)
        duration_txt.text = if(lastVoiceCallHistory?.directionType == BBCallDirection.Outgoing && lastVoiceCallHistory?.duration == "0") getString(R.string.call_not_answered)
                            else if(lastVoiceCallHistory?.duration == "0") ""
                            else if (durationHours == 0) "$durationMinutes:$durationSeconds"
                            else "$durationHours:$durationMinutes:$durationSeconds"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_call_info, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.remove_from_call_log) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Do you want to clear your entire call log?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { dialog, id ->


                    }
                    .setNegativeButton("No") { dialog, id ->
                        // Dismiss the dialog
                        dialog.dismiss()
                    }
            val alert = builder.create()
            alert.show()
        }
        return super.onOptionsItemSelected(item)
    }
}