package com.ec.bond.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.ec.bond.BondApp
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.*
import com.ec.bond.common.Const
import com.ec.bond.services.ForegroundService
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.Event
import com.ec.bond.utils.NotificationUtility
import com.ec.bond.worker.IncommingCallWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * Every Activity with this project <p>Must</p> inherit from this BaseActivity
 */

var vibrator : Vibrator?=null

open class BaseActivity : AppCompatActivity() {

    class IncomingCallObserver(var context: Activity) : Observer<Event<BBCall>> {
        private var vibrator: Vibrator? = null
        val handler = Handler(Looper.getMainLooper())


        override fun onChanged(t: Event<BBCall>?) {
            val call = t?.getContentIfNotHandled() ?: return
            handleCallStatus(call)
            startIncommingWorker(call)
            //vibrate()
            //showNotification(context, call.getCallersNames(), call.hasVideo)
        }

        @SuppressLint("RestrictedApi")
        private fun startIncommingWorker(call: BBCall) {
            val inputData = Data.Builder()
            inputData.put(Const.CALLER_NAME, call.getCallersNames())
            inputData.put(Const.HAS_VIDEO, call.hasVideo)
            val request = OneTimeWorkRequest.Builder(IncommingCallWorker::class.java)
                .addTag("BACKUP_WORKER_TAG").setInputData(inputData.build()).build()
            WorkManager.getInstance(context).enqueue(request)
        }

        private fun handleCallStatus(call: BBCall) {
            Blackbox.currentCall = call
            GlobalScope.launch(Dispatchers.Main) {
                call.status.observe(context as LifecycleOwner, { status ->
                    if (status == BBCallStatus.hangup || status == BBCallStatus.ended) {
                        Log.e("call_statsu====",""+status)
                        // Close Notification
                        NotificationUtility.clearNotification(context, Const.INCOMMING_NOTIFICATION_ID)
                        ForegroundService.stopService(context)
                        stopHandler()
                    }
                })
            }
        }

/*        private fun initVibrateService(context: Context) {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            try {
                player = MediaPlayer.create(
                    BondApp.applicationContext(),
                    Settings.System.DEFAULT_RINGTONE_URI
                )
                player?.isLooping=true
                player?.start()
            }catch (e:Exception){

            }

        }*/

/*        private fun showNotification(context: Context, callerName: String, hasVideoCall: Boolean = false) {
            initVibrateService(context)
            val incomingCallNotificationUI = if (hasVideoCall) {
                RemoteViews(context.packageName, R.layout.incoming_call_video_notification)
            } else {
                RemoteViews(context.packageName, R.layout.incoming_call_notification)
            }
            incomingCallNotificationUI.setTextViewText(R.id.person_name, callerName)

            val incomingCallDeclineButton = Intent("INCOMING_CALL_RECEIVER")
            incomingCallDeclineButton.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            incomingCallDeclineButton.putExtra("DECLINE_CALL", true)
            incomingCallDeclineButton.putExtra("ACCEPT_CALL", false)
            incomingCallDeclineButton.putExtra("VIDEO_CALL", hasVideoCall)

            val incomingCallAcceptButton = Intent("INCOMING_CALL_RECEIVER")
            incomingCallAcceptButton.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            incomingCallAcceptButton.putExtra("DECLINE_CALL", false)
            incomingCallAcceptButton.putExtra("ACCEPT_CALL", true)
            incomingCallAcceptButton.putExtra("VIDEO_CALL", hasVideoCall)

            val pendingSwitchIntentDeclineCall: PendingIntent = PendingIntent.getBroadcast(context, 100, incomingCallDeclineButton, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            incomingCallNotificationUI.setOnClickPendingIntent(R.id.decline_call, pendingSwitchIntentDeclineCall)

            val pendingSwitchIntentAnswerCall: PendingIntent = PendingIntent.getBroadcast(context, 101, incomingCallAcceptButton, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            incomingCallNotificationUI.setOnClickPendingIntent(R.id.answer_call, pendingSwitchIntentAnswerCall)
            NotificationUtility.displayCustomNotificationView(context,
                100001, remoteViews = incomingCallNotificationUI, remoteViewsHeadUp = incomingCallNotificationUI)
            vibrate()

           *//* handler.postDelayed(object : Runnable {
                override fun run() {

                   // handler.postDelayed(this, 3000)
                }
            }, 100)*//*
        }*/

        @Suppress("DEPRECATION")
        private fun vibrate() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(450)
            }
        }

        fun stopHandler() {
            handler.removeCallbacksAndMessages(null)
        }
    }

   inner class IncomingMessageObserver(var context: Activity) : Observer<Event<com.ec.bond.blackbox.model.Message>> {

        override fun onChanged(t: Event<com.ec.bond.blackbox.model.Message>?) {
            val message = t?.getContentIfNotHandled() ?: return

            if (message.isGroupChat) {
                val group = Blackbox.getGroupFromMessage(message)
                // Show Notification based on the Group and the Message
                println(group.ID)
                if(t.peekContent().msgtype.equals("system")){
                    NotificationUtility.clearNotification(context, Const.INCOMMING_NOTIFICATION_ID)
                    ForegroundService.stopService(context)
                    BMediaPlayer.stopRingtune()
                    //Blackbox.currentCall=null
                }
                handleMessageNotification(bbGroup = group, messageItem = message)
            } else {
                val contact = Blackbox.getContactFromMessage(message)
                // Show Notification based on the Contact and the Message
                if(t.peekContent().msgtype.equals("system")){
                    NotificationUtility.clearNotification(context, Const.INCOMMING_NOTIFICATION_ID)
                    ForegroundService.stopService(context)
                    BMediaPlayer.stopRingtune()
                   // Blackbox.currentCall=null
                }
                println(contact.registeredNumber)
                handleMessageNotification(bbContact = contact, messageItem = message)
            }
        }

        private fun handleMessageNotification(bbGroup: BBGroup? = null, bbContact: BBContact? = null, messageItem: com.ec.bond.blackbox.model.Message) {
            var isSameChat = false
            if (context.applicationContext is BondApp) {
                val app = context.applicationContext as BondApp
                val activity = app.getCurrentActivity()
                if (activity is ChatBrowsingActivity) {
                    //if current contact chat is showing
                    if (activity.chatBrowsingViewModel.chatTypeRef is BBContact) {
                        val contact = activity.chatBrowsingViewModel.chatTypeRef as BBContact
                        bbContact?.let { bbContactGet ->
                            if (bbContactGet.registeredNumber == contact.registeredNumber) {
                                isSameChat = true
                            }
                        }
                    }
                    //if current group chat is showing
                    if (activity.chatBrowsingViewModel.chatTypeRef is BBGroup) {
                        val group = activity.chatBrowsingViewModel.chatTypeRef as BBGroup
                        bbGroup?.let { bbGroupGet ->
                            if (bbGroupGet.ID == group.ID) {
                                isSameChat = true
                            }
                        }
                    }
                }else{
                    if (!isSameChat) {
                        val intent = Intent(context, ChatBrowsingActivity::class.java)
                        val recipient = bbContact?.registeredNumber ?: bbGroup?.ID ?: return
                        val title = bbContact?.getContactName() ?: messageItem.sender
                        val notificationTitle = bbGroup?.desc ?: ""
                        val notificationId = recipient.toBigInteger().toInt()
                        intent.putExtra("recipient", recipient)
                        intent.putExtra("notification_id", notificationId)

                        val toneType = bbGroup?.messageNotificationSoundName
                            ?: bbContact?.messageNotificationSoundName ?: ""
                        val uri: Uri? = CommonUtils.getToneTyoe(toneType, context)

                        NotificationUtility.displayBundledNotification(pContext = context,
                            pNotificationId = notificationId,
                            pMessage = messageItem,
                            pTitle = title,
                            intent = intent,
                            channelId = (recipient.toLong() + 123456).toString() + "_".plus(SystemClock.uptimeMillis().toInt()),
                            channelName = bbContact?.registeredNumber ?: bbGroup?.description?.value
                            ?: "",
                            groupId = "Chat Notification",
                            notificationTitle = notificationTitle,
                            pUri = uri)
                    }
                }
            }

            //display notification
            if (!isSameChat) {
                val intent = Intent(context, ChatBrowsingActivity::class.java)
                val recipient = bbContact?.registeredNumber ?: bbGroup?.ID ?: return
                val title = bbContact?.getContactName() ?: messageItem.sender
                val notificationTitle = bbGroup?.desc ?: ""
                val notificationId = recipient.toBigInteger().toInt()
                intent.putExtra("recipient", recipient)
                intent.putExtra("notification_id", notificationId)

                val toneType = bbGroup?.messageNotificationSoundName
                        ?: bbContact?.messageNotificationSoundName ?: ""
                val uri: Uri? = CommonUtils.getToneTyoe(toneType, context)

                NotificationUtility.displayBundledNotification(pContext = context,
                        pNotificationId = notificationId,
                        pMessage = messageItem,
                        pTitle = title,
                        intent = intent,
                        channelId = (recipient.toLong() + 123456).toString() + "_".plus(SystemClock.uptimeMillis().toInt()),
                        channelName = bbContact?.registeredNumber ?: bbGroup?.description?.value
                        ?: "",
                        groupId = "Chat Notification",
                        notificationTitle = notificationTitle,
                        pUri = uri)
            }
        }
    }

    private var mMyApp: BondApp? = null
    private lateinit var incomingCallObserver: IncomingCallObserver
    private lateinit var incomingMessageObserver: IncomingMessageObserver

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            NotificationUtility.clearNotification(context, Const.INCOMMING_NOTIFICATION_ID)
            ForegroundService.stopService(context!!)
            BMediaPlayer.stopRingtune()
            incomingCallObserver.stopHandler()
            if (intent?.extras?.getBoolean("ACCEPT_CALL", false) == true) {
                GlobalScope.launch {
                    val call = Blackbox.currentCall ?: return@launch
                    val context = context ?: return@launch
                    Blackbox.openCallActivity(call, context)
                }
            } else if (intent?.extras?.getBoolean("DECLINE_CALL", false) == true) {
                GlobalScope.launch {
                    val call = Blackbox.currentCall ?: return@launch
                    call.endCall()
                    Blackbox.currentCall = null
                }
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMyApp = this.applicationContext as BondApp

        incomingCallObserver = IncomingCallObserver(this)
        Blackbox.account.incomingCallPublisher.observe(this, incomingCallObserver)

        incomingMessageObserver = IncomingMessageObserver(this)
        Blackbox.account.incomingMessagesPublisher.observe(this, incomingMessageObserver)

        registerReceiver(broadcastReceiver, IntentFilter("INCOMING_CALL_RECEIVER"))
        Blackbox.account.incomingMessagesPublisher.observeForever {
            incomingMessageObserver.onChanged(it)
        }

        Blackbox.account.incomingCallPublisher.observeForever {
            incomingCallObserver.onChanged(it)
        }
    }

    override fun onResume() {
        super.onResume()
        setOnlineStatus(BBStatus.online)
        Log.i("Status", "contact ID = ${Blackbox.account.registeredNumber} and onResume()")
        mMyApp?.setCurrentActivity(this)
    }

    override fun onPause() {
        setOnlineStatus(BBStatus.offline)
       // clearReferences()

       // Log.i("Status", "contact ID = ${Blackbox.account.registeredNumber} and onPause()")
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        incomingCallObserver.stopHandler()
        clearReferences()
    }

    private fun clearReferences() {
        mMyApp?.getCurrentActivity()?.let {
            if (this == it) mMyApp?.setCurrentActivity(null)
        }
    }

    private fun setOnlineStatus(status: BBStatus) = GlobalScope.launch(Dispatchers.IO) {
       var value= Blackbox.account.setOnlineStatus(status)
        Log.e("status-----",""+value)
    }

}