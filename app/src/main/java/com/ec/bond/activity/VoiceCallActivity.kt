package com.ec.bond.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.BondApp
import com.ec.bond.R
import com.ec.bond.adapter.ConferenceListAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBCall
import com.ec.bond.blackbox.model.BBCallStatus
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.common.Const
import com.ec.bond.services.ForegroundService
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.NotificationUtility
import com.robertlevonyan.components.picker.set
import kotlinx.android.synthetic.main.call_header.*
import kotlinx.android.synthetic.main.layout_call_receiver.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


//public var player:MediaPlayer?=null
class VoiceCallActivity : BaseActivity(), ConferenceListAdapter.ConferenceAdapterListener {
    // CAMERA Fields

    private var vibrator: Vibrator? = null
    public var context: Context? = null

    private var millisecondTime: Long = 0L
    private var startTime: Long = 0L
    private var timeBuff: Long = 0L
    private var updateTime: Long = 0L
    private var handler: Handler? = null
    private var conferenceListAdapter: ConferenceListAdapter? = null
    private var conferenceMemberList: ArrayList<BBContact> = arrayListOf()
    private var fromFirebase: Boolean = false
    private var autoAcceptCall = false
    var mediaPlayerOutgoing: MediaPlayer? = null

    companion object {
        const val CALL_WAITING_TIMEOUT = 45*1000L
    }

    private var runnable: Runnable = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            millisecondTime = SystemClock.uptimeMillis() - startTime
            updateTime = timeBuff + millisecondTime
            val seconds = (updateTime / 1000).toInt()
            callStatusTV.text = fromSecondsToMmSS(seconds.toLong())
            handler?.postDelayed(this, 250)
        }
    }

    fun updateData() = GlobalScope.launch(Dispatchers.IO) {
        if (Blackbox.fetchContactsAsync())
            Blackbox.fetchChatListAsync()
        val accountInfoSuccess = Blackbox.account.fetchAccountInfoAsync()
        val fetchCallsHistorySuccess = Blackbox.fetchCallsHistoryAsync()
        Blackbox.fetchNotificationsSounds()
    }

    fun fromSecondsToMmSS(seconds: Long): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val remainMinutes = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, remainMinutes)
    }

    fun stopPlayRingings() {
        BMediaPlayer.stopRingtune()
    }

    fun starRingings() {
        BMediaPlayer.playRingtune()
    }

    private fun vibrate(v: LongArray) {
        com.ec.bond.activity.vibrator = (BondApp.applicationContext()
            .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?)!!;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            com.ec.bond.activity.vibrator?.vibrate(VibrationEffect.createWaveform(v, 0));
            //vibrator?.vibrate(v,0)
        } else {
            vibrator?.vibrate(450)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        setContentView(R.layout.layout_call_receiver)
        updateData()
        clearIncommingCallNotification()
        handler = Handler(Looper.getMainLooper())
        GlobalScope.launch {
            Blackbox.currentCall?.getCallInfo()
        }

        Log.e("logd----", "" + Blackbox.currentCall)
        val call = Blackbox.currentCall ?: run {
            finish()
            return
        }
        intent?.let {
            if (it.hasExtra("fromFirebase")) {
                fromFirebase = it.getBooleanExtra("fromFirebase", false)
            }
        }

        intent?.let {
            if (it.hasExtra("ACCEPT_CALL")) {
                autoAcceptCall = it.getBooleanExtra("ACCEPT_CALL", false)
            }
        }
        // The first thing to do is set the call context
        call.context = this

        if (call.isConference == false) {
            call.members.value?.let { members ->
                members.firstOrNull()?.let { contact ->
                    contact.chatImagePath.observe(this, Observer { path ->
                        if (call.hasVideo) {
                            backgroundFriendViewForVideo.set(BitmapFactory.decodeFile(path))
                        } else {
                            backgroundFriendView.set(BitmapFactory.decodeFile(path))
                        }
                    })
                }
            }
        } else {
            //conference call
            sender_name.visibility = View.GONE

            backgroundFriendView.setImageResource(R.color.colorPrimary)
            header_message.text = getString(R.string.voice_call)
            initConferenceRecyclerView()
            call.members.observe(this, Observer {
                if (it.size == 4) {
                    person_add.visibility = View.GONE
                } else {
                    person_add.visibility = View.VISIBLE
                    if (startTime == 0L) {
                        person_add.alpha = 0.5f
                    }
                }
                if (it.size == 1) {
                    backgroundConferenceViewParent.visibility = View.VISIBLE
                    sender_name.visibility = View.VISIBLE
                    conferenceCallRecyclerView.visibility = View.GONE
                    val contact = it[0]
                    if (!contact.getChatImagePath().isNullOrEmpty()) {
                        backgroundConferenceView.set(BitmapFactory.decodeFile(it[0].imagePath))
                    } else {
                        backgroundConferenceView.setImageResource(R.drawable.contact)
                    }
                    sender_name.text = contact.name
                    observeConferenceContacts(contact, 0)
                    end_conference_call.setOnClickListener {
                        //end conference call for single member
                        removeMembersFromConferenceCall(contact, 0)
                    }
                } else {
                    backgroundConferenceViewParent.visibility = View.GONE
                    sender_name.visibility = View.GONE
                    conferenceCallRecyclerView.visibility = View.VISIBLE
                    it.mapIndexed { index, bbContact ->
                        observeConferenceContacts(
                            bbContact,
                            index
                        )
                    }
                }
                conferenceMemberList.clear()
                conferenceMemberList.addAll(it)
                conferenceListAdapter!!.notifyDataSetChanged()
            })

        }

        if (call.isOutgoing) {
            // Start the new call
            sender_name.text = call.getCallersNames()
            call.startOutgoingCall()
            startWaitingCallResponseTimer(call)

        } else {
            // Incoming call: Answer the call
            sender_name.text = call.getCallersNames()
            val keyguardManager: KeyguardManager =
                applicationContext?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            Log.e("binh","auto accept call 2 = "+autoAcceptCall)
            if (keyguardManager.isDeviceLocked || !autoAcceptCall) {
                Log.e("locked1----", "lock")
                // call.answerCall()
                answerButton.visibility = View.VISIBLE
                starRingings();
            } else {
                var powerManager = getSystemService(POWER_SERVICE) as PowerManager
                var isScreenOn: Boolean
                isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                    powerManager.isInteractive
                } else {
                    powerManager.isScreenOn
                }

                if (!isScreenOn) {
                    Log.e("locked2----", "lock")
                    starRingings();
                    answerButton.visibility = View.VISIBLE

                } else {
                    answerButton.visibility = View.GONE
                    Log.e("binh", "call answer at object call= $call and activity $this")
                    lifecycleScope.launch(Dispatchers.Main) {
                        val fetchContactsSuccess = Blackbox.fetchContactsAsync()
                        if (fetchContactsSuccess)
                            call.answerCall()
                    }
                }
            }
        }

        initVibrateService()

        call.status.observe(this, Observer {
            Log.e("bb_call Status", " ${it.asString}")
            Log.e("number_call---", "inf===" + call?.getCallersNames() + "")
            sender_name.text = call.getCallersNames()
            when (it) {
                BBCallStatus.setup -> {
                    callStatusTV.set(R.string.calling)
                }
                BBCallStatus.ringing -> {
                    callStatusTV.set(R.string.ringing)
                    if (call.isOutgoing) {
                        startOutgoingRinging()
                    }
                }
                BBCallStatus.answered -> {
                    startTime = SystemClock.uptimeMillis()
                    handler!!.postDelayed(runnable, 0)
                    answerButton.visibility = View.GONE
                    movePreview()

                    if (call.hasVideo && call.isSpeaker.value == false) {
                        call.toggleSpeaker()
                    }
                    if (call.isOutgoing) {
                        stopOutgoingRinging()
                    }
                }
                BBCallStatus.active -> {
                    handler!!.removeCallbacks(runnable)
                    if (startTime == 0L) {
                        startTime = SystemClock.uptimeMillis()
                    }
                    handler!!.postDelayed(runnable, 0)

                    movePreview()

                    if (call.isSpeaker.value == false) {
                        call.toggleSpeaker()
                    }
                    if (call.isOutgoing) {
                        stopOutgoingRinging()
                    }
                }
                BBCallStatus.hangup -> {
                    disableUIElements()
                    timeBuff += millisecondTime
                    if (call.isOutgoing) {
                        stopOutgoingRinging()
                    }
                }
                BBCallStatus.ended -> {
                    disableUIElements()
                    Log.e("binh", "End call test ")
                    finish()
                    if (call.isOutgoing) {
                        stopOutgoingRinging()
                    }
                }
                else -> {

                }
            }
        })



        call.isSpeaker.observe(this, Observer { speakerState ->
            Log.e("speaker----", "" + speakerState)
            speakerButton.background = if (speakerState) {
                ContextCompat.getDrawable(this, R.drawable.speaker_active)
            } else {
                ContextCompat.getDrawable(this, R.color.transparent_color)
            }
        })

        call.isMute.observe(this, Observer { microPhoneState ->
            muteButton.background = if (microPhoneState) {
                ContextCompat.getDrawable(this, R.drawable.speaker_active)
            } else {
                ContextCompat.getDrawable(this, R.color.transparent_color)
            }
        })

        call.isVideoPaused.observe(this, Observer { isVideoPaused ->
            stopCameraButton.background = if (isVideoPaused) {
                ContextCompat.getDrawable(this, R.drawable.circle_primary_color_light)
            } else {
                ContextCompat.getDrawable(this, R.color.transparent_color)
            }
        })

        hangUpButton.setOnClickListener {
            Blackbox.currentCall?.endCall()
            stopPlayRingings()
        }

        speakerButton.setOnClickListener {
            call.toggleSpeaker()
            vibrate()
        }

        muteButton.setOnClickListener {
            call.togglePause()
            vibrate()
        }

        flipCameraButton.setOnClickListener {
            call.toggleCameraLens()
            vibrate()
        }

        answerButton.setOnClickListener {
            Log.e("binh", "Call answer at 390")
            call.answerCall()
            vibrate()
            stopPlayRingings()
        }

        stopCameraButton.setOnClickListener {
            call.toggleVideoPause()
            vibrate()
        }

        if (call.hasVideo) {
            speakerButton.visibility = View.GONE
            backgroundFriendView.visibility = View.GONE
            flipCameraButton.visibility = View.VISIBLE
            stopCameraButton.visibility = View.VISIBLE
            cameraPreviewParentView.visibility = View.VISIBLE
            backgroundFriendViewParentView.visibility = View.VISIBLE
            incomingVideoTextureView.visibility = View.VISIBLE
            incomingVideoTextureParentView.visibility = View.VISIBLE
            header.setBackgroundColor(Color.TRANSPARENT)
            // make sure the aspect ratio is kept
            val matrix = calculateSurfaceHolderTransform()
            incomingVideoTextureView.setTransform(matrix)

            incomingVideoTextureView.surfaceTexture?.let {
                call.initIncomingVideoDecoder(
                    Surface(it),
                    incomingVideoTextureView.width,
                    incomingVideoTextureView.height
                )
            }

            incomingVideoTextureView.surfaceTextureListener =
                object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                        // pass the surface that we will use to show the incoming video
                        call.initIncomingVideoDecoder(
                            Surface(p0),
                            incomingVideoTextureView.width,
                            incomingVideoTextureView.height
                        )
                    }

                    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

                    }

                    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                        return true
                    }

                    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

                    }
                }

            callControlsLinearLayoutView.setBackgroundColor(Color.TRANSPARENT)
            header.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun startWaitingCallResponseTimer(call: BBCall) {
        val timer = object : CountDownTimer(CALL_WAITING_TIMEOUT, CALL_WAITING_TIMEOUT) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                val nonAnswerList = arrayListOf<BBCallStatus>(
                    BBCallStatus.none,
                    BBCallStatus.ringing,
                    BBCallStatus.setup
                )
                if (nonAnswerList.contains(call.status.value)) {
                    Blackbox.currentCall?.endCall()
                    stopPlayRingings()
                }

            }

        }
        timer.start()
    }

    private fun clearIncommingCallNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.cancel(Const.INCOMMING_NOTIFICATION_ID)
        stopService(Intent(this, ForegroundService::class.java))
        NotificationUtility.clearNotification(this,Const.INCOMMING_NOTIFICATION_ID)
    }


    private fun startOutgoingRinging() {
        var redId = resources.getIdentifier("ring_ring", "raw", packageName)
        mediaPlayerOutgoing = MediaPlayer.create(this, redId)
        mediaPlayerOutgoing?.start()
        mediaPlayerOutgoing?.isLooping = true
    }

    private fun stopOutgoingRinging() {
        mediaPlayerOutgoing?.let {
            if (it.isPlaying) {
                it.stop()
            }
        }
    }

    private val conferencePersonAddListenr = View.OnClickListener {
        val call = Blackbox.currentCall ?: return@OnClickListener
        val intent = Intent(this@VoiceCallActivity, ConferenceGroupSelectionActivity::class.java)
        intent.putExtra("bbCallMembers", call.members.value)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivityForResult(intent, 1002)
        overridePendingTransition(R.anim.slide_up, R.anim.non_slide)
    }

    private fun movePreview() {
        val call = Blackbox.currentCall ?: return
        if (call.hasVideo && friendInfoContainer.visibility == View.VISIBLE) {
            val videoCallViewParams =
                videoCallParentView.layoutParams as (RelativeLayout.LayoutParams)
            videoCallViewParams.addRule(RelativeLayout.ABOVE, R.id.callControlsLinearLayoutView)
            videoCallViewParams.addRule(RelativeLayout.BELOW, R.id.header)
            videoCallParentView.layoutParams = videoCallViewParams

            val params = cameraPreviewParentView.layoutParams as (RelativeLayout.LayoutParams)
            CommonUtils.animateView(
                cameraPreviewParentView,
                cameraPreviewParentView.measuredHeight,
                400,
                boundHeight = true
            )
            CommonUtils.animateView(
                cameraPreviewParentView,
                cameraPreviewParentView.measuredWidth,
                250,
                boundWidth = true
            )
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            cameraPreviewParentView.layoutParams = params
            cameraPreviewParentView.radius = resources.getDimension(R.dimen._8sdp)

            friendInfoContainer.visibility = View.GONE
        }
    }

    private fun initConferenceRecyclerView() {
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        conferenceCallRecyclerView.layoutManager = layoutManager
        conferenceCallRecyclerView.itemAnimator = DefaultItemAnimator()
        conferenceListAdapter = ConferenceListAdapter(this, conferenceMemberList, this)
        conferenceCallRecyclerView.adapter = conferenceListAdapter
    }

    private fun disableUIElements() {
        hangUpButton.isEnabled = false
        speakerButton.isEnabled = false
        muteButton.isEnabled = false
    }

    /**
     * But its screen size is: 2340 * 1080, so the most suitableCamera.Size It is 1920 * 1080,
     * you will find that the preview interface is stretched. Imagine that the height of 1920
     * will be stretched to the height of 2340. At this time, you need to do a view of the
     * preview interface.Zoom with Offset Operation, the approximate code is as follows:
     */
    private fun calculateSurfaceHolderTransform(): Matrix {
        // Preview the size of the view, such as SurfaceView
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val viewHeight: Float = displayMetrics.heightPixels.toFloat()
        val viewWidth: Float = displayMetrics.widthPixels.toFloat()

        val cameraHeight = 640.0F
        val cameraWidth = 480.0F

        val ratioPreview = cameraWidth / cameraHeight
        val ratioView = viewWidth / viewHeight
        val scaleX: Float
        val scaleY: Float

        if (ratioView < ratioPreview) {
            scaleX = ratioPreview / ratioView
            scaleY = 1.0F
        } else {
            scaleX = 1.0F
            scaleY = ratioView / ratioPreview
        }
        // Calculate the offset of the View
        val scaledWidth = viewWidth * scaleX
        val scaledHeight = viewHeight * scaleY
        val dx = (viewWidth - scaledWidth) / 2
        val dy = (viewHeight - scaledHeight) / 2

        val matrix = Matrix()
        matrix.postScale(scaleX, scaleY)
        matrix.postTranslate(dx, dy)

        return matrix
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val call = Blackbox.currentCall ?: return
            if (call.hasVideo) {
                if (CommonUtils.hasPermissions(this, Manifest.permission.CAMERA)) {
                    startCamera()
                } else {
                    requestPermissions()
                }
            } else {
                if (CommonUtils.hasPermissions(this, Manifest.permission.RECORD_AUDIO) == false) {
                    requestPermissions()
                }
            }
        }
    }

    override fun onPause() {
        Blackbox.currentCall?.let {
            val call = Blackbox.currentCall ?: return
            if (call.hasVideo) {
                call.closeCamera()
            }
        }


        super.onPause()
    }

    private fun requestPermissions() {
        val call = Blackbox.currentCall ?: return
        call.audioPermission = CommonUtils.hasPermissions(this, Manifest.permission.RECORD_AUDIO)
        call.cameraPermission = CommonUtils.hasPermissions(this, Manifest.permission.CAMERA)

        val permissions = if (call.hasVideo) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
        if (CommonUtils.hasPermissions(this, *permissions) == false) {
            CommonUtils.requestPermissions(this, *permissions)
        }
    }

    private fun startCamera() {
        val call = Blackbox.currentCall ?: return
        call.prepareCamera(cameraPreview)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        val call = Blackbox.currentCall ?: return
        when (requestCode) {
            CommonUtils.PERMISSIONS_CODE -> {

                var permissionsGranted = true
                for (i in grantResults.indices) {
                    if (permissionsGranted == false) {
                        break
                    }
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        permissionsGranted = false
                    }
                }

                if (permissionsGranted) {
                    call.audioPermission = true
                    if (call.hasVideo) {
                        startCamera()
                        call.cameraPermission = true
                    }

                    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG)
                        .show()
                    finish()
                }


            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == RESULT_OK) {
            val conferenceCallNewMembers =
                data?.getSerializableExtra("bbCallMembers") as? ArrayList<BBContact>?
            if (conferenceCallNewMembers != null) {
                val call = Blackbox.currentCall ?: return
                call.addMembersToConferenceCall(conferenceCallNewMembers)
            }
        }
    }

    private fun initVibrateService() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(100)
        }
    }

    override fun onBackPressed() {}

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacks(runnable)
        val call = Blackbox.currentCall ?: return
        if (call.isConference) {
            conferenceMemberList.map { bbContact -> bbContact.callStatus = "" }
        }
        Blackbox.currentCall = null
        NotificationUtility.clearNotification(applicationContext, Const.INCOMMING_NOTIFICATION_ID)
        stopPlayRingings()
    }

    //observe conference calls members status
    private fun observeConferenceContacts(bbContact: BBContact, position: Int) {
        bbContact.contactPosition = position
        bbContact.callInfo.callStatus.observe(this, Observer { status ->
            Log.d("bb_conference_call Status", " ${status.asString}")
            when (status) {
                BBCallStatus.setup -> {
                    if (startTime == 0L) {
                        callStatusTV.set(R.string.calling)
                    }
                    conferenceListAdapter?.setCallStatus(
                        bbContact.contactPosition!!,
                        getString(R.string.calling)
                    )
                }
                BBCallStatus.ringing -> {
                    if (startTime == 0L) {
                        callStatusTV.set(R.string.ringing)
                    }
                    conferenceListAdapter?.setCallStatus(
                        bbContact.contactPosition!!,
                        getString(R.string.ringing)
                    )
                }
                BBCallStatus.answered -> {
                    if (startTime == 0L) {
                        startTime = SystemClock.uptimeMillis()
                        handler!!.postDelayed(runnable, 0)
                        person_add.alpha = 1.0f
                        person_add.setOnClickListener(conferencePersonAddListenr)
                    }
                    conferenceListAdapter?.setCallStatus(
                        bbContact.contactPosition!!,
                        getString(R.string.outgoing)
                    )
                }
                BBCallStatus.active -> {
                    if (startTime == 0L) {
                        startTime = SystemClock.uptimeMillis()
                        handler!!.postDelayed(runnable, 0)
                    }
                    conferenceListAdapter?.setCallStatus(
                        bbContact.contactPosition!!,
                        getString(R.string.outgoing)
                    )
                }
                BBCallStatus.hangup -> {

                }
                BBCallStatus.ended -> {

                }
                else -> {
                }
            }
            conferenceCallRecyclerView.post {
                conferenceListAdapter?.notifyItemChanged(bbContact.contactPosition!!)

            }
        })
    }

    override fun deletedSelectedConference(contact: BBContact?, position: Int) {
        if (contact != null) {
            removeMembersFromConferenceCall(contact, position)
        }
    }

    private fun removeMembersFromConferenceCall(contact: BBContact, position: Int) {
        val call = Blackbox.currentCall ?: return
        call.removeContactFromCall(contact)
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}