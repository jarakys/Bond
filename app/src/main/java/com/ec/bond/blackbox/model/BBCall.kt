package com.ec.bond.blackbox.model

import android.content.Context
import android.content.Context.POWER_SERVICE
import android.graphics.*
import android.media.AudioManager
import android.media.Image
import android.os.PowerManager
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.lifecycle.*
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.getStringOrNull
import com.ec.bond.blackbox.model.api_responses.CallInfoResponse
import com.ec.bond.blackbox.model.api_responses.CallStatusResponse
import com.ec.bond.blackbox.model.api_responses.CallStatusResponseJsonDeserializer
import com.ec.bond.blackbox.model.api_responses.GeneralResponse
import com.ec.bond.services.VoipAudioManager
import com.ec.bond.services.video.ControllerVideo
import com.ec.bond.services.video.H264Decoder
import com.ec.bond.services.video.H264Encoder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.theeasiestway.yuv.YuvUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.System.currentTimeMillis


@Suppress("EnumEntryName")
enum class BBCallStatus(var value: Int) {
    none(0),
    setup(1),
    ringing(2),
    answeredAudioOnly(3),
    answered(4),
    active(5),
    hangup(6),
    ended(7);

    val asString: String
        get() {
            return when (this) {
                none, setup -> "calling"
                ringing -> "ringing"
                answered, answeredAudioOnly, active -> "answered"
                hangup, ended -> "hangup"
            }
        }

    val isHangupOrEnded: Boolean
        get() {
            return when (this) {
                hangup, ended -> true
                else -> false
            }
        }
}

class BBCurrentCallInfo {
    var callID: String? = null
    var callSession: Int? = null
    var isAudioReceiveStarted: Boolean = false
    var answeredTime: Long = 0

    private val _callStatus = MutableLiveData<BBCallStatus>()
    val callStatus: LiveData<BBCallStatus> get() = _callStatus

    init {
        _callStatus.postValue(BBCallStatus.none)
    }

    /**
     * Set the call status and update the observable property
     */
    fun setCallStatus(status: BBCallStatus) {
        if (status.value < BBCallStatus.hangup.value) {
            // Update the status only if needed
            val currentStatus = callStatus.value ?: return
            if (currentStatus.value < status.value) {
                _callStatus.postValue(status)
                if (status == BBCallStatus.answered) {
                    // set the answer timestamp
                    answeredTime = currentTimeMillis()
                }
            }
        } else {
            _callStatus.postValue(status)
        }
    }

    /**
     * Must be called when the call is closed with the specific contact to reset every value to the initial state.
     */
    fun reset() {
        callID = null
        callSession = null
        isAudioReceiveStarted = false
        _callStatus.postValue(BBCallStatus.none)
    }
}

/**
 * This class represents the call in progress.
 * The Call can be one of the following type:
 * 1. OneToOne
 * 2. Conference Call
 * 3. OneToOne Video Call
 *
 * A basic workflow for Outgoing calls is:
 *
 * 1. Create and Initialize the BBCall Object
 * 2. Add the Contact calling -> addContact(contact: BBContact). Call this function multiple times
 *    to add more contacts
 * 3. Start the call -> startCall(). It will automatically create a OneToOne caìll or
 *    Conference Call based on the numbers of contacts
 * 4. Start updating the BBCall object status as well as the BBContact.callInfo.callStatus
 *    (mainly used for Conference calls)
 *      1. For OneToOne calls you should call updateVoiceCallStatus().
 *          This function will automatically update the object status every second
 *      2. For Conference call you should call updateConferenceCallStatus().
 *          This function will automatically update every BBContact.callInfo.callStatus and the
 *          BBCall status based on each contact status.
 * 5. Update the UI according to BBCall.status observable property.
 * 6. Update the UI based on the BBCall.members observable property.
 *      Each BBContact has a BBCurrentCallInfo property that you can check and use to update the UI.
 *      For Conference calls, a list of user will be displayed and each user will have his own status.
 *
 * 7. As soon as the BBCall status is equal to BBCallStatus.answered we should start to
 *    transfer/receive the audio data now. And there are two possible scenarios.
 *
 * 7a. Answered status For OneToOne Calls:
 * Start to send and receive audio packets using bb_audio_send & bb_audio_receive.
 *
 * 7b. Answered status For Conference Calls:
 * The process is almost the same as above, but we must use different functions:
 * bb_audio_send_session & bb_audio_receive_session.
 * This functions needs a SESSION parameter. This parameter is automatically assigned to each
 * contact BBContact.callInfo.callSession when starting the conference call
 * (check startConferenceCall() for reference).
 *
 * To know which session of which contact to use is very simple:
 * The first contact that answered the call.
 * If this contact hang-up the call, you can pick any other contact session (if any) as long as
 * this contact already answered the same call. Otherwise wait that a contact answer and then use
 * his session.
 *
 *
 * 8. End Call:
 * You can hang-up the call by calling endCall()
 *
 * For OneToOne calls: If the other Party close the call, the *status* property of BBCall will be
 * automatically updated to "hangup"
 *
 * For Conference calls: If the all the members of the call already left (hangup status),
 * the *status* property of BBCall will be automatically updated to "hangup"
 *
 *
 *
 * ########
 * A basic worklflow for Incoming calls is:
 *
 * 1. Internal push notification is received with status 1 or 2 (call or videcall)
 * 2. Create the BBCall object and call the function getCallInfo()
 *      This call will set the callID and add the Caller contact to the members list.
 * 3. From here you can goto Outgoing call list(above) starting at point N. 4. Everything is the same.
 *
 */
class BBCall(var isOutgoing: Boolean, var hasVideo: Boolean = false) : ViewModel() {

    lateinit var context: Context

    private val TAG = BBCall::class.java.simpleName
    private val yuvUtils = YuvUtils()

    private val videoEncoder = H264Encoder(object : H264Encoder.H264EncoderListener {
        override fun onFrameEncoded(encodedFrame: ByteArray) {
            viewModelScope.launch(Dispatchers.IO) {
                Log.d("DRE", "sending network feed size is ${encodedFrame.size} and " +
                        encodedFrame.joinToString("") { "%02x".format(it) })
//                decodeVideoFrame(encodedFrame)
                val result = bb_video_send(encodedFrame, encodedFrame.size)
                if (result == 0) {
                    Log.d("sendEncodedVideoFrame", "bb_video_send failed")
                }
            }
        }
    })
    private var videoDecoder = H264Decoder()
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var previewView: PreviewView? = null

    private var audioManager: AudioManager? = null

    private val _status = MutableLiveData(BBCallStatus.setup)
    val status: LiveData<BBCallStatus> get() = _status

    private val statusObserver: Observer<BBCallStatus> by lazy {
        Observer<BBCallStatus> { callStatus ->
            when (callStatus) {
                BBCallStatus.ringing -> {
                    if (isOutgoing) {
                        // TODO: START Playing ringtone

                    }
                }
                BBCallStatus.answeredAudioOnly -> {
                    if (hasVideo) {
                        // TODO: Start audio without video
                    }
                    if (isOutgoing) {
                        // TODO: STOP ringing ringtone
                    }
                }
                BBCallStatus.answered -> {
                    if (isOutgoing) {
                        // TODO: The ringing can occur for maximum 45 seconds, Stop & Discard any timer here if necessary
                        // TODO:  STOP ringing ringtone
                    } else {
                        wakeLock?.acquire()
                    }


                    if (isConference) {
                        val members = members.value ?: arrayListOf()
                        members.filter { it.callInfo.callSession != null }.forEach { contact ->
                            viewModelScope.launch {
                                listenToContactCallStatusChange(contact)
                            }
                        }
                    }
                    // TODO: Start Audio if needed and route the audio to the normal receiver
                    VoipAudioManager.initializeAudioForCall(this)
                    VoipAudioManager.startAudio()

                    if (hasVideo) {
                        viewModelScope.launch(Dispatchers.IO) {
                            receiveVideoFrames()
                        }
                    }

                }
                BBCallStatus.hangup -> {
                    Log.e("call_status-----", "Hangup" + Blackbox.currentCall)

                    if (isOutgoing == false) {
                        // TODO: The ringing can occur for maximum 45 seconds,
                        //       Stop & Discard any timer here if necessary
                        // TODO:  STOP ringing ringtone
                    }

                    VoipAudioManager.stopAudio()

                    viewModelScope.launch(Dispatchers.IO) {
                        if (hasVideo) {
                            endVideoCall()
                        } else {
                            if (isConference) {
                                endConferenceCall()
                            } else {
                                endOneToOneCall()
                            }
                        }
                        setCallStatus(BBCallStatus.ended)
                        Log.e("binh", "setCallStatus end 270")
                    }

                    Blackbox.currentCall = null
                }
                BBCallStatus.ended -> {
                    // TODO: Clean / destroy everything here
                    wakeLock?.let {
                        if (it.isHeld) it.release()
                    }
                    if (hasVideo) {
                        videoEncoder.close()
                        videoDecoder.close()
                        ControllerVideo.destroyCamera()
                    }
                    members.value?.forEach { contact ->
                        contact.callInfo.reset()
                    }
                    removeStatusObserver()
                    Blackbox.currentCall = null
                }
                else -> {

                }
            }
        }
    }

    private val _members = MutableLiveData<ArrayList<BBContact>>().apply {
        postValue(arrayListOf())
    }
    val members: LiveData<ArrayList<BBContact>> get() = _members

    private val _isMute = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val isMute: LiveData<Boolean> get() = _isMute

    private val _isVideoPaused = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val isVideoPaused: LiveData<Boolean> get() = _isVideoPaused

    private val _isSpeaker = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val isSpeaker: LiveData<Boolean> get() = _isSpeaker

    val isAudioStarted: Boolean
        get() {
            val status = status.value ?: return false
            return status == BBCallStatus.answeredAudioOnly ||
                    status == BBCallStatus.answered ||
                    status == BBCallStatus.active
        }

    // This value must be set
    var callID: String? = null
    var isConference: Boolean = false
    var cameraPermission = true
    var audioPermission = true

    // Flag needed for outgoing call. It will change the status of the call from answered to Active.
    private var isVideoConfirmed = false

    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var field = 0x00000020

    // region Public Utility Functions

    fun toggleSpeaker() {
        var isOn = _isSpeaker.value ?: false
        audioManager?.isSpeakerphoneOn = !isOn
        _isSpeaker.postValue(!isOn)
    }

    fun togglePause() {
        val isPause = _isMute.value ?: false
        audioManager?.isMicrophoneMute = !isPause
        _isMute.postValue(!isPause)
    }

    fun toggleVideoPause() {
        val isPause = _isVideoPaused.value ?: false
        if (!isPause) {
            closeCamera()
        } else {
            startCamera()
        }
        _isVideoPaused.postValue(!isPause)
    }

    fun getCallersNames(): String {
        var names = ""
        members.value?.let {
            it.forEachIndexed { index, bbContact ->
                val name = bbContact.getContactName()
                names = if (index == 0) {
                    name
                } else {
                    "$names, $name"
                }
            }
        }
        return names
    }

    /**
     * Add a contact to the call if not present
     */
    fun addContact(contact: BBContact) {
        val members = members.value ?: arrayListOf()
        val exist = members.any { it.registeredNumber == contact.registeredNumber }
        if (exist == false) {
            members.add(contact)
            _members.postValue(members)
        }
    }

    /**
     * Set the call status
     */
    fun setCallStatus(status: BBCallStatus) {
        // Update the status only if needed
        if (status.value < BBCallStatus.hangup.value) {
            // Update the status only if needed
            this.status.value?.let {
                if (it.value < status.value) {
                    _status.postValue(status)
                }
            }
        } else {
            // Always set the status for hangup or ended
            _status.postValue(status)
        }
    }

    /**
     * Prepare and Render the Camera on the specified preview.
     */
    fun prepareCamera(preview: PreviewView) {
        previewView = preview
        startCamera()
    }

    private fun startCamera() {
        val preview = previewView ?: return

        ControllerVideo.destroyCamera()
        ControllerVideo.subscribe(javaClass.name) { image -> sendVideoFrame(image) }
        ControllerVideo.initCamera(context, lensFacing, preview)
    }

    fun toggleCameraLens() {
        if (isVideoPaused.value == true)
            return

        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        startCamera()
    }

    fun closeCamera() {
        ControllerVideo.destroyCamera()
    }

    // endregion

    // region Private Utility Functions

    private fun startStatusObserver() {
        viewModelScope.launch {
            status.observeForever(statusObserver)
        }
    }

    private fun removeStatusObserver() {
        viewModelScope.launch {
            status.removeObserver(statusObserver)
        }
    }

    private fun initAudioManager() {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager!!.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager!!.isSpeakerphoneOn = false
        audioManager!!.isMicrophoneMute = false
    }

    private fun initPowerManager() {
        try {
            // Yeah, this is hidden field.
            field = PowerManager::class.java.javaClass.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK")
                .getInt(null)
        } catch (ignored: Throwable) {
        }

        powerManager = context.getSystemService(POWER_SERVICE) as? PowerManager;
        wakeLock = powerManager!!.newWakeLock(field, this.javaClass.name);
    }

    private fun startProximitySensor() {
        initPowerManager()

        val wakeLock = wakeLock ?: return
        if (wakeLock.isHeld == false)
            wakeLock.acquire()
    }

    private suspend fun confirmVideoCall(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val members = members.value ?: return@withContext false
        val contact = members.firstOrNull() ?: return@withContext false
        val contactCallID = contact.callInfo.callID ?: return@withContext false
        val jsonString = bb_confirm_videocall(contactCallID, pwdConf)
        val gson = Gson()
        val response = gson.fromJson(jsonString, GeneralResponse::class.java)
        return@withContext response.isSuccess
    }

    // endregion

    // region Start Call Functions

    /**
     * Start one of the possible calls based on this object props value:
     * 1. One to one call
     * 2. Conference Call
     * 3. Video call
     */
    fun startOutgoingCall() = viewModelScope.launch {
        // restore the contacts call status to .none before starting the call
        val members = members.value ?: return@launch
        for (member in members) member.callInfo.reset()

        val success = if (isConference) startConferenceCall() else startCall()
        if (success) {
            initAudioManager()
            startProximitySensor()
            startStatusObserver()
            updateCallStatus()
        } else {
            setCallStatus(BBCallStatus.ended)
            Log.e("binh", "setCallStatus end 516")
        }
    }

    /**
     * Start the call
     */
    private suspend fun startCall(): Boolean = withContext(Dispatchers.IO) {
        if (hasVideo) {
            startVideoCall()
        } else {
            val members = members.value ?: return@withContext false
            val isConference = members.size > 1
            if (isConference) {
                startConferenceCall()
            } else {
                startOneToOneCall()
            }
        }
    }

    /**
     * Start a new Voice Call with the all the *members*
     */
    private suspend fun startOneToOneCall(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val members = members.value ?: return@withContext false
        val contact = members.firstOrNull() ?: return@withContext false
        val jsonString = bb_originate_voicecall(contact.registeredNumber, pwdConf)
        val jsonObject = JSONObject(jsonString)

        val answer = jsonObject.getStringOrNull("answer") ?: return@withContext false
        if (answer == "OK") {
            val callID = jsonObject.getStringOrNull("callid") ?: return@withContext false
            contact.callInfo.callID = callID
            return@withContext true
        } else {
            val message = jsonObject.getStringOrNull("message") ?: return@withContext false
            Log.e("Start Voice Call Error", message)
            false
        }
    }

    /**
     * Start a new Voice Call with the all the *members*
     */
    private suspend fun startConferenceCall(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val members = members.value ?: return@withContext false

        val successMembers = arrayListOf<BBContact>()
        members.forEachIndexed { index, contact ->
            if (contact.callInfo.callSession != null)
                return@forEachIndexed

            contact.callInfo.callSession = index

            val jsonString = bb_originate_voicecall_id(index, contact.registeredNumber, pwdConf)
            val jsonObject = JSONObject(jsonString)
            val answer = jsonObject.getStringOrNull("answer") ?: "KO"
            if (answer == "OK") {
                jsonObject.getStringOrNull("callid")?.let { callid ->
                    contact.callInfo.callID = callid

                    successMembers.add(contact)
                }
            } else {
                Log.e(
                    "Start Voice Call Error",
                    "Failed to start call with ${contact.getContactName()}"
                )
            }
        }

        if (successMembers.isEmpty())
            return@withContext false

        if (successMembers.size < members.size) {
            // Update the members list with only contacts that successfully originated the call
            _members.postValue(successMembers)
            return@withContext true
        }

        if (successMembers.size == members.size)
            return@withContext true

        false
    }

    private suspend fun startVideoCall(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val members = members.value ?: return@withContext false
        val contact = members.firstOrNull() ?: return@withContext false
        val jsonString = bb_originate_videocall(contact.registeredNumber, pwdConf)
        val jsonObject = JSONObject(jsonString)
        val answer = jsonObject.getStringOrNull("answer") ?: return@withContext false
        if (answer == "OK") {
            val callID = jsonObject.getStringOrNull("callid") ?: return@withContext false
            contact.callInfo.callID = callID
            return@withContext true
        } else {
            val message = jsonObject.getStringOrNull("message") ?: return@withContext false
            Log.e("Start Video Call Error", message)
            false
        }
    }

    /**
     * Try to add the the contacts to the conference call
     *
     * @param contacts the contacts to add
     * @return return the list of contacts that successfully originated the call
     */
    private suspend fun addContactsToConferenceCall(contacts: List<BBContact>): List<BBContact> =
        withContext(Dispatchers.IO) {
            val pwdConf = Blackbox.pwdConf ?: return@withContext listOf()
            val members = members.value ?: return@withContext listOf()


            val addedMembers = arrayListOf<BBContact>()
            for (contact in contacts) {
                // get all available Session index
                var session: Int
                val sessions = members.filter { it.callInfo.callSession != null }
                    .map { it.callInfo.callSession!! }
                if (sessions.contains(0) == false) {
                    session = 0
                } else if (sessions.contains(1) == false) {
                    session = 1
                } else if (sessions.contains(2) == false) {
                    session = 2
                } else if (sessions.contains(3) == false) {
                    session = 3
                } else {
                    Log.e(
                        TAG,
                        "Unable to add contact to the conference call: Invalid contact session"
                    )
                    continue
                }

                contact.callInfo.callSession = session
                val jsonString =
                    bb_originate_voicecall_id(session, contact.registeredNumber, pwdConf)
                val jsonObject = JSONObject(jsonString)
                val answer = jsonObject.getStringOrNull("answer") ?: "KO"
                if (answer == "OK") {
                    contact.callInfo.callID = jsonObject.getString("callid")
                    addedMembers.add(contact)
                } else {
                    Log.e(
                        "Start Voice Call Error",
                        "Failed to start call with ${contact.getContactName()}"
                    )
                }
            }

            return@withContext addedMembers
        }

    // endregion

    // region Get call info Functions

    /**
     * Called when receiving a call to get Call info
     * 1. set call status to setup on success
     * 2. Add the calling contact to the call members
     */
    suspend fun getCallInfo(): Boolean = withContext(Dispatchers.IO) {
        if (hasVideo) {
            if (getVideoCallInfo()) {
                // Update video call info
                startStatusObserver()
                updateCallStatus()
                return@withContext true
            }
        } else {
            if (getVoiceCallInfo()) {
                startStatusObserver()
                updateCallStatus()
                return@withContext true
            }
        }
        false
    }

    private suspend fun getVoiceCallInfo(): Boolean = withContext(Dispatchers.IO) {
        Log.e("call_info-----", "" + Blackbox.pwdConf)
        Blackbox.pwdConf?.let { pwdConf ->
            val jsonString = bb_info_voicecall(pwdConf)
            Log.e("call_info-----", jsonString)
            val gson = Gson()
            val response = gson.fromJson(jsonString, CallInfoResponse::class.java)
            if (response.isSuccess) {
                setCallStatus(BBCallStatus.setup)

                if (response.callerID.isNotEmpty()) {
                    // Add the incoming call contact to the members array
                    Blackbox.getContact(response.callerID)?.let { contact ->
                        contact.callInfo.callID = response.callID
                        addContact(contact)
                    }

                    Blackbox.getTemporaryContact(response.callerID)?.let { contact ->
                        contact.callInfo.callID = response.callID
                        addContact(contact)
                    }
                }

                // Create a new contact if the array list is still empty
                members.value?.let {
                    if (it.size == 0) {
                        val contact = BBContact()
                        contact.ID = response.contactID
                        contact.name = response.contactName
                        contact.registeredNumber = response.callerID
                        contact.phonesjson = listOf(BBPhoneNumber("mobile", response.callerID))
                        contact.phonejsonreg = listOf(BBPhoneNumber("mobile", response.callerID))
                        contact.callInfo.callID = response.callID
                        addContact(contact)

                        Blackbox.addTemporaryContact(contact)
                    }
                }

                return@withContext true
            } else {
                setCallStatus(BBCallStatus.ended)
            }
        }

        false
    }

    private suspend fun getVideoCallInfo(): Boolean = withContext(Dispatchers.IO) {
        Blackbox.pwdConf?.let { pwdConf ->
            val jsonString = bb_info_videocall(pwdConf)
            val gson = Gson()
            val response = gson.fromJson(jsonString, CallInfoResponse::class.java)
            if (response.isSuccess) {
                hasVideo = true
                setCallStatus(BBCallStatus.setup)

                if (response.callerID.isNotEmpty()) {
                    // Add the incoming call contact to the members array
                    Blackbox.getContact(response.callerID)?.let { contact ->
                        contact.callInfo.callID = response.callID
                        addContact(contact)
                    }

                    Blackbox.getTemporaryContact(response.callerID)?.let { contact ->
                        contact.callInfo.callID = response.callID
                        addContact(contact)
                    }
                }

                // Create a new contact if the array list is still empty
                members.value?.let {
                    if (it.size == 0) {
                        val contact = BBContact()
                        contact.ID = response.contactID
                        contact.name = response.contactName
                        contact.registeredNumber = response.callerID
                        contact.phonesjson = listOf(BBPhoneNumber("mobile", response.callerID))
                        contact.phonejsonreg = listOf(BBPhoneNumber("mobile", response.callerID))
                        contact.callInfo.callID = response.callID
                        addContact(contact)

                        Blackbox.addTemporaryContact(contact)
                    }
                }

                return@withContext true
            }
        }

        false
    }

    // endregion

    // region Answer Call functions

    /**
     * Answer and Incoming call. Set the status to ended if fail.
     *
     * @param context : Used to initialize AudioManager
     */
    fun answerCall() = viewModelScope.launch {
        // init audio manager before the call start
        initAudioManager()
        val result = if (hasVideo) answerVideoCall(false) else answerVoiceCall()
        if (result == false) {
            setCallStatus(BBCallStatus.ended)
            Log.e("binh", "setCallStatus end 887")
        } else {
            // Star the proximity sensor only if the call has been answered successfully
            startProximitySensor()
        }
    }

    /**
     * Answer the call without setting the status to *answered*. bb_status_voicecall_id is responsable to assign the status to this object
     */
    private suspend fun answerVoiceCall(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_answer_voicecall(pwdConf)
        Log.e("binh", "answerVoiceCall $jsonString")
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        Log.e("binh", "answerVoiceCall response = ${response.isSuccess}")
        return@withContext response.isSuccess
    }

    /**
     * Answer the call without setting the status to *answered*. bb_status_videocall will assign the status to this object
     */
    private suspend fun answerVideoCall(audioOnly: Boolean): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_answer_videocall(if (audioOnly) "Y" else "N", pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        return@withContext response.isSuccess
    }

    // endregion

    // region Update call Status Functions
    private fun updateCallStatus() {
        viewModelScope.launch {
            if (hasVideo) {
                // Video status update
                updateVideoCallStatus()
            } else {
                if (isConference) {
                    updateConferenceCallStatus()
                } else {
                    updateVoiceCallStatus()
                }
            }
        }
    }

    /**
     * Update the status of the call every second
     */
    private suspend fun updateVoiceCallStatus() = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext
        var retryCount = 0
        while (true) {
            if (status.value != null && status.value!!.value < BBCallStatus.hangup.value) {
                val members = members.value ?: return@withContext
                val contact = members.firstOrNull() ?: return@withContext
                val callID = contact.callInfo.callID ?: return@withContext
                val jsonString = bb_status_voicecall(callID, pwdConf)

                val gson = GsonBuilder().registerTypeAdapter(
                    CallStatusResponse::class.java,
                    CallStatusResponseJsonDeserializer()
                ).create()
                val response = gson.fromJson(jsonString, CallStatusResponse::class.java)
                if (response.isSuccess) {

                    // check every member of the conference call Field to see if there is more then 1 Contact in this call.
                    // Show the table if contacts > 1
                    // Show avatar if contacts == 1
                    response.conferenceMembersStatus?.let { membersStatus ->
                        if (membersStatus.isEmpty()) return@let
                        val newMembers =
                            members.filter { it.registeredNumber == membersStatus.first().callerID } as ArrayList
                        for (memberStatus in membersStatus) {
                            if (memberStatus.calledID == Blackbox.account.registeredNumber) continue
                            val cnt = Blackbox.getContact(memberStatus.calledID)
                                ?: Blackbox.getTemporaryContact(memberStatus.calledID)
                                ?: BBContact(
                                    name = memberStatus.calledID,
                                    registeredNumber = memberStatus.calledID
                                )
                            cnt.callInfo.callID = callID
                            cnt.callInfo.setCallStatus(memberStatus.status)
                            newMembers.add(cnt)
                        }
                        _members.postValue(newMembers)
                    }

                    // reset the retry count if success
                    retryCount = 0
                    setCallStatus(response.status)
                    contact.callInfo.setCallStatus(response.status)
                    if (response.status == BBCallStatus.hangup) {
                        return@withContext
                    }
                } else {
                    retryCount += 1
                    if (retryCount > 3) {
                        return@withContext
                    }
                }

                // Wait 1 second and update the status again until the call is not in hangup state
                delay(1000)
            } else {
                break
            }
        }
    }

    /**
     * Update the call status for each contact (BBContact.callInfo.callStatus) and se the BBCall status
     * based on each contact status.
     */
    private suspend fun updateConferenceCallStatus() = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext
        val currentStatus = status.value ?: return@withContext

        while (currentStatus.value < BBCallStatus.hangup.value) {
            val members = members.value ?: break
            val sanitizedMembers =
                members.filter { it.callInfo.callID != null && it.callInfo.callSession != null }

            // For conference calls we don't set the BBCall status, just the BBContact.callInfo.callStatus
            // The Call status will be derived from all the members statuses
            for (contact in sanitizedMembers) {
                val callID = contact.callInfo.callID ?: continue
                val contactCallSession = contact.callInfo.callSession ?: continue

                val jsonString = bb_status_voicecall_id(callID, contactCallSession, pwdConf)
                val gson = GsonBuilder().registerTypeAdapter(
                    CallStatusResponse::class.java,
                    CallStatusResponseJsonDeserializer()
                ).create()
                val response = gson.fromJson(jsonString, CallStatusResponse::class.java)

                if (response.isSuccess) {
                    Log.d(
                        TAG,
                        "${contact.registeredNumber} current status = ${contact.callInfo.callStatus.value!!.asString}"
                    )
                    Log.d(
                        TAG,
                        "${contact.registeredNumber} new status = ${response.status.asString}"
                    )

                    contact.callInfo.setCallStatus(response.status)
                    when (response.status) {
                        BBCallStatus.setup, BBCallStatus.ringing, BBCallStatus.answered -> {
                            setConferenceCallStatus()
                        }
                        BBCallStatus.hangup -> {
                            removeContactAndHangUpIfNeeded(contact)
                        }
                        else -> {
                        }
                    }
                }

                // Wait 750ms and check the next contact
                delay(750)
            }
        }
    }

    private suspend fun updateVideoCallStatus() = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext
        var retryCount = 0
        while (true) {
            if (status.value != null && status.value!!.value < BBCallStatus.hangup.value) {
                val members = members.value ?: return@withContext
                val contact = members.firstOrNull() ?: return@withContext
                val callID = contact.callInfo.callID ?: return@withContext
                val jsonString = bb_status_videocall(callID, pwdConf)
                val gson = GsonBuilder().registerTypeAdapter(
                    CallStatusResponse::class.java,
                    CallStatusResponseJsonDeserializer()
                ).create()
                val response = gson.fromJson(jsonString, CallStatusResponse::class.java)
                if (response.isSuccess) {
                    // reset the retry count if success
                    retryCount = 0
                    setCallStatus(response.status)
                    contact.callInfo.setCallStatus(response.status)
                    if (response.status == BBCallStatus.hangup) {
                        return@withContext
                    }
                } else {
                    retryCount += 1
                    if (retryCount > 3) {
                        return@withContext
                    }
                }

                // Wait 1 second and update the status again until the call is not in hangup state
                delay(1000)
            } else {
                break
            }
        }
    }

    private fun setConferenceCallStatus() {
        var setupCount = 0
        var ringingCount = 0
        var answeredCount = 0

        val members = members.value ?: return
        for (member in members) {
            val memberCalLStatus = member.callInfo.callStatus.value ?: continue
            if (memberCalLStatus == BBCallStatus.setup) {
                setupCount += 1
            } else if (memberCalLStatus == BBCallStatus.ringing) {
                ringingCount += 1
            } else if (memberCalLStatus == BBCallStatus.answered ||
                memberCalLStatus == BBCallStatus.active ||
                memberCalLStatus == BBCallStatus.answeredAudioOnly
            ) {
                answeredCount += 1
            }
        }

        val callStatus = status.value ?: return
        if (answeredCount > 0) {
            if (callStatus.value < BBCallStatus.answered.value) {
                setCallStatus(BBCallStatus.answered)
            }
        } else if (ringingCount > 0) {
            if (callStatus == BBCallStatus.ringing == false) {
                setCallStatus(BBCallStatus.ringing)
            }
        } else {
            setCallStatus(BBCallStatus.setup)
        }
    }

    /**
     * Remove it from the members list and HangUp the call if there are no more Contacts presents
     */
    private fun removeContactAndHangUpIfNeeded(contact: BBContact) {
        val members = members.value ?: return
        val newList = ArrayList(members.filter { it.registeredNumber != contact.registeredNumber })
        _members.postValue(newList)
        if (newList.size == 0) {
            setCallStatus(BBCallStatus.hangup)
        }
    }

    private suspend fun listenToContactCallStatusChange(contact: BBContact) =
        withContext(Dispatchers.Main) {
            contact.callInfo.callStatus.observe(context as LifecycleOwner, Observer { status ->
                if (status == BBCallStatus.answeredAudioOnly ||
                    status == BBCallStatus.answered ||
                    status == BBCallStatus.active
                ) {
                    viewModelScope.launch {
                        fetchConferenceCallFirstAudioPacketForContact(contact)
                    }
                }
            })
        }

    /**
     * Add multiple contacts to an Active Conference Call (Max 4 members)
     */
    fun addMembersToConferenceCall(contacts: List<BBContact>) =
        viewModelScope.launch(Dispatchers.IO) {
            val members = members.value ?: arrayListOf()
            val contactsToAdd = sanitizeContactsToAdd(contacts)
            if (contactsToAdd.isNotEmpty()) {

                if (canAddContacts(contactsToAdd) == false) {
                    return@launch
                }

                val addedContacts = addContactsToConferenceCall(contacts)
                for (contact in addedContacts) {
                    listenToContactCallStatusChange(contact)
                }

                members.addAll(addedContacts)
                _members.postValue(members)
            }
        }

    /**
     * Add multiple contacts to the call object (Max 4 members)
     *
     * @return the added contacts
     */
    fun addMultipleContacts(contacts: List<BBContact>): Boolean {
        val members = members.value ?: arrayListOf()
        val newMembers = sanitizeContactsToAdd(contacts)
        if (canAddContacts(newMembers) == false) {
            Log.e(TAG, "You are trying to add too many contacts to the call (max 4)")
            return false
        }

        // add the new members to the observable list
        members.addAll(newMembers)
        _members.postValue(members)
        return true
    }


    private fun canAddContacts(contacts: List<BBContact>): Boolean {
        val members = members.value ?: arrayListOf()
        if (members.size == 4) {
            Log.e(TAG, "The call already has the maximum number of concurrent contacts (4)")
            return false
        }

        if (members.size + contacts.size > 4) {
            Log.e(TAG, "You are trying to add too many contacts to the call (max 4)")
            return false
        }

        return true
    }

    /**
     * Return a list of contacts that are not present in members list
     */
    private fun sanitizeContactsToAdd(contacts: List<BBContact>): List<BBContact> {
        val members = members.value ?: arrayListOf()
        if (contacts.size + members.size > 4)
            return listOf()

        // Reset the callInfo object before starting the call
        for (contact in contacts)
            contact.callInfo.reset()

        // Sanity check for already present contacts within the call
        return ArrayList(contacts.filter { cnt ->
            !members.any { cnt.registeredNumber == it.registeredNumber }
        })
    }

    // endregion

    // region End Call Functions

    /**
     * End the call. bb_status_voicecall_id is responsable to assign the status to this object
     */
    fun endCall() {
        val status = status.value ?: return
        if (status.value < BBCallStatus.hangup.value) {
            setCallStatus(BBCallStatus.hangup)
        }
    }

    private suspend fun endOneToOneCall(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val members = members.value ?: return@withContext false
        val contact = members.firstOrNull() ?: return@withContext false
        val contactCallID = contact.callInfo.callID ?: return@withContext false
        val jsonString = bb_hangup_voicecall(contactCallID, pwdConf)
        val gson = Gson()
        val response = gson.fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            return@withContext true
        }
        false
    }

    private suspend fun endConferenceCall(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        var successCount = 0
        val members = members.value ?: return@withContext false
        for (contact in members) {
            if (contact.callInfo.callID == null || contact.callInfo.callSession == null) {
                continue
            }
            val jsonString = bb_hangup_voicecall_id(
                contact.callInfo.callID!!,
                contact.callInfo.callSession!!,
                pwdConf
            )
            val gson = Gson()
            val response = gson.fromJson(jsonString, GeneralResponse::class.java)
            if (response.isSuccess) {
                successCount++
            }
        }
        if (successCount == members.size) {
            return@withContext true
        }
        false
    }

    private suspend fun endVideoCall(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val members = members.value ?: return@withContext false
        val contact = members.firstOrNull() ?: return@withContext false
        val contactCallID = contact.callInfo.callID ?: return@withContext false
        val jsonString = bb_hangup_videocall(contactCallID, pwdConf)
        val gson = Gson()
        val response = gson.fromJson(jsonString, GeneralResponse::class.java)
        return@withContext response.isSuccess
    }

    fun removeContactFromCall(contact: BBContact) = viewModelScope.launch {
        endCallWithContact(contact)
    }

    /**
     * Remove a specific contact from the call
     * @param contact The contact to remove remove from the call and call hang-up with
     * @return true if success
     */
    private suspend fun endCallWithContact(contact: BBContact): Boolean =
        withContext(Dispatchers.IO) {
            val pwdConf = Blackbox.pwdConf ?: return@withContext false
            val members = members.value ?: return@withContext false
            if (members.any {
                    it.registeredNumber == contact.registeredNumber &&
                            contact.callInfo.callSession != null &&
                            contact.callInfo.callID != null
                }) {
                val jsonString = bb_hangup_voicecall_id(
                    contact.callInfo.callID!!,
                    contact.callInfo.callSession!!,
                    pwdConf
                )
                val gson = Gson()
                val response = gson.fromJson(jsonString, GeneralResponse::class.java)
                if (response.isSuccess) {
                    contact.callInfo.setCallStatus(BBCallStatus.hangup)
                    removeContactAndHangUpIfNeeded(contact)
                    return@withContext true
                }
            }
            false
        }

    // endregion

    // region Audio Transfer Functions

    /**
     * Return a Pair of two values. First is the function result, second is the audio buffer.
     * The second value is not null only if the first value > 0
     *
     * if first value is equal to -1 -> "Audio - Timed-out"
     * if first value is equal to -2 -> "Audio - Hang Up"
     * if first value is > 0 -> Valid buffer of size 1920
     */
    fun receiveAudioPacket(): Pair<Int, ByteArray?> {
        val buffer = ByteArray(1920)
        val result = bb_audio_receive(buffer)
        return Pair(result, if (result > 0) buffer else null)
    }

    /**
     * Send the audio packet.
     * @return true if success
     */
    fun sendAudio(buffer: ByteArray, session: Int? = null) {
        if (isMute.value == true) {
            // Byte array with all values equal to Zero = Mute
            sendAudioToNetwork(ByteArray(1920), session)
        } else {
            val bufferSize = buffer.count()
            if (bufferSize == 1920) {
                sendAudioToNetwork(buffer, session)
            } else if (bufferSize > 1920) {
                val bufferChunksCount = bufferSize / 1920
                for (i in 0..bufferChunksCount) {
                    val startIndex = i * 1920
                    if (i < bufferChunksCount) {
                        val endIndex = startIndex + 1920
                        sendAudioToNetwork(buffer.copyOfRange(startIndex, endIndex), session)
                        continue
                    }
                    // check if there is any data left to send.
                    val diff = bufferSize - startIndex
                    if (diff > 0) {
                        // Initialize a Zeroed ByteArray for the remaining bytes to send.
                        val arr = ByteArray(1920 - diff)
                        val newBuffer = buffer.copyOfRange(startIndex, bufferSize) + arr
                        sendAudioToNetwork(newBuffer, session)
                    }
                }
            } else {
                val arr = ByteArray(1920 - bufferSize)
                val newBuffer = buffer + arr
                sendAudioToNetwork(newBuffer, session)
            }
        }
    }

    private fun sendAudioToNetwork(buffer: ByteArray, session: Int? = null): Int {
        return if (session != null) bb_audio_send_session(
            session,
            buffer
        ) else bb_audio_send(buffer)
    }

    // endregion

    // region Video Transfer Functions

    fun initIncomingVideoDecoder(surface: Surface, width: Int, height: Int) {
        videoDecoder.prepare(surface, width, height)
    }

    /**
     * Receive a buffer frame
     * @return byteArray containing the NALU(s)
     */
    private suspend fun receiveVideoFrames() = withContext(Dispatchers.IO) {
        while (true) {
            val call = Blackbox.currentCall ?: break
            val callStatus = call.status.value ?: break
            if (callStatus.value < BBCallStatus.hangup.value) {

                if (call.isOutgoing && isVideoConfirmed == false) {
                    isVideoConfirmed = confirmVideoCall()
                }

                val frameBuffer = bb_video_receive() ?: continue
                decodeVideoFrame(frameBuffer)
            }
        }
    }

    private fun sendVideoFrame(image: Image) {
        // Android return an image rotated by 90% (It assume that we recorded in landscape mode)
        // rotating the image directly causes the image color to change.
        // As a workaround we do the following:
        // 1. Take the YUV image
        // 2. Rotate by 270 degree
        // 3. Flip horizontally
        // If we don't perform the following 3 steps, the color is distorted.
        // 4. Convert back to ARGB
        // 5. Create bitmap
        // 6. Convert bitmap to YUV ByteArray

        if (image.width <= 0 || image.height <= 0) return

        // this step isn't mandatory but it may help you in case if the colors of the output frame (after scale, rotate etc.) are distorted
        var yuvFrame = yuvUtils.convertToI420(image)
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
//            yuvFrame = yuvUtils.rotate(yuvFrame, 270)
//            yuvFrame = yuvUtils.scale(yuvFrame, 480, 640, Constants.FILTER_BOX)
            yuvFrame = yuvUtils.mirrorH(yuvFrame)
        }
//        else {
//            yuvFrame = yuvUtils.rotate(yuvFrame, 90)
//            yuvFrame = yuvUtils.scale(yuvFrame, 480, 640, Constants.FILTER_BOX)
//        }
        sendVideoFrame(yuvFrame.asArray(), yuvFrame.width, yuvFrame.height)

    }

    /**
     * Send video buffer frame
     * @return true if success
     */
    private fun sendVideoFrame(data: ByteArray, width: Int, height: Int) {
        if (videoEncoder.isReady == false) {
            videoEncoder.prepare(width, height)
        }

        if (data.isEmpty()) return

//        if (isVideoPaused.value == true || hasVideo == false || status.value != BBCallStatus.active) {
//            return
//        }

//        viewModelScope.launch(Dispatchers.IO) {
//            videoEncoder.encode(buffer)
//        }

        videoEncoder.encode(data)
    }

    private fun decodeVideoFrame(frameBuffer: ByteArray) {
        videoDecoder.decodeFrame(frameBuffer)
    }

    // endregion

    // region Conference Audio Transfer Functions

    /**
     * Fetch the first packet for each contact of the call
     *
     * @param contact: The contact
     */
    private suspend fun fetchConferenceCallFirstAudioPacketForContact(contact: BBContact) =
        withContext(Dispatchers.IO) {
            val contactCallSession = contact.callInfo.callSession ?: return@withContext

            // Say to the blackbox that a we want to marge this contact audio
            bb_audio_set_audioconference(contactCallSession)

            while (true) {
                val contactCallStatus = contact.callInfo.callStatus.value ?: break
                val callStatus = status.value ?: break
                if (contactCallStatus.value < BBCallStatus.hangup.value && callStatus.value < BBCallStatus.hangup.value) {
                    val firstPacket = receiveAudioPacketSession(contactCallSession)

                    if (firstPacket.first > 0) {
                        val bytes = firstPacket.second ?: continue
                        if (bytes.isNotEmpty()) {
                            // success
                            contact.callInfo.isAudioReceiveStarted = true
                            break
                        }
                    } else if (firstPacket.first == -1) {
                        // time-out
                        break
                    } else {
                        // hang-up
                        break
                    }
                } else {
                    break
                }
            }
        }

    /**
     * Function used by the Host of the conference (the one that starts the conference call)
     *
     * Return a Pair of two values. First is the function result, second is the audio buffer.
     * The second value is not null only if the first value > 0
     *
     * if first value is equal to -1 -> "Audio - Timed-out"
     * if first value is equal to -2 -> "Audio - Hang Up"
     * if first value is > 0 -> Valid buffer of size 1920
     */
    fun receiveAudioPacketSession(session: Int): Pair<Int, ByteArray?> {
        val buffer = ByteArray(1920)
        val result = bb_audio_receive_session(session, buffer)
        return Pair(result, if (result > 0) buffer else null)
    }


    /**
     * Function used by the Host of the conference (the one that starts the conference call)
     * Send the audio packet to the specific session.
     * @return true if success
     */
    fun sendAudioSession(session: Int, buffer: ByteArray): Boolean {
        return bb_audio_send_session(session, buffer) == 1
    }

    // endregion

    // region C FUNCTIONS

    private external fun bb_originate_voicecall_id(
        session: Int,
        recipient: String,
        pwdconf: String
    ): String

    private external fun bb_originate_voicecall(recipient: String, pwdconf: String): String
    private external fun bb_info_voicecall(pwdconf: String): String
    private external fun bb_info_videocall(pwdconf: String): String
    private external fun bb_answer_voicecall(pwdconf: String): String
    private external fun bb_answer_videocall(audioOnly: String, pwdconf: String): String
    private external fun bb_status_voicecall(callid: String, pwdconf: String): String
    private external fun bb_status_voicecall_id(
        callid: String,
        session: Int,
        pwdconf: String
    ): String

    private external fun bb_hangup_voicecall(callid: String, pwdconf: String): String
    private external fun bb_hangup_voicecall_id(
        callid: String,
        session: Int,
        pwdconf: String
    ): String

    // Video functions
    private external fun bb_originate_videocall(recipient: String, pwdconf: String): String
    private external fun bb_video_send(frameBuffer: ByteArray, bufferSize: Int): Int
    private external fun bb_video_receive(): ByteArray?
    private external fun bb_status_videocall(callID: String, pwdconf: String): String
    private external fun bb_hangup_videocall(callID: String, pwdconf: String): String
    private external fun bb_confirm_videocall(callid: String, pwdconf: String): String

    // One to One call
    /**
     * This function will fill the audio buffer with data. It must be 1920 of size.
     *
     * @param audioBuffer bytes of data
     * @return > 0, success. If return == -1, Audio - Timed-out.
     * If return == -2, "Call - Hang Up"
     */
    private external fun bb_audio_receive(audioBuffer: ByteArray): Int

    /**
     * This function send the audio buffer.
     *
     * @param audioBuffer bytes of data
     * @return 0 = error, 1 = success
     */
    private external fun bb_audio_send(audioBuffer: ByteArray): Int

    // Conference call
    /**
     * This function send the audio buffer to the specified session.
     *
     * @param audioBuffer bytes of data
     * @return 0 = error, 1 = success
     */
    private external fun bb_audio_send_session(session: Int, audioBuffer: ByteArray): Int
    private external fun bb_audio_receive_session(session: Int, audioBuffer: ByteArray): Int
    private external fun bb_audio_set_audioconference(session: Int)

    // endregion

}