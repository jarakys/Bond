package com.ec.bond.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.ec.bond.R
import com.ec.bond.activity.VoiceCallActivity
import com.ec.bond.common.Const
import com.ec.bond.utils.HeadsUpNotificationActionReceiver
import com.ec.bond.utils.NotificationUtility

class ForegroundService : Service() {
    lateinit var incomingCallDeclineButton:Intent;
    lateinit var incomingCallAcceptButton:Intent
    lateinit var fullScreenIntent:Intent
    companion object {
        fun startService(context: Context, message: String,hasVideoCall: Boolean = false, fromFirebase:Boolean=false) {
            val startIntent = Intent(context, ForegroundService::class.java)
            startIntent.putExtra("callerName", message)
            startIntent.putExtra("hasVideoCall", hasVideoCall)
            startIntent.putExtra("fromFirebase", fromFirebase)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callerName = intent?.getStringExtra("callerName")
        val hasVideoCall = intent?.getBooleanExtra("hasVideoCall",false)
        val fromFirebase = intent?.getBooleanExtra("fromFirebase",false)
        showNotification(applicationContext,callerName!!,hasVideoCall!!,fromFirebase!!);
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun showNotification(context: Context, callerName: String, hasVideoCall: Boolean = false,fromFirebase:Boolean=false) {
        val incomingCallNotificationUI = if (hasVideoCall) {
            RemoteViews(context.packageName, R.layout.incoming_call_video_notification)
        } else {
            RemoteViews(context.packageName, R.layout.incoming_call_notification)
        }
        incomingCallNotificationUI.setTextViewText(R.id.person_name, callerName)

        if(fromFirebase){
             incomingCallDeclineButton = Intent(applicationContext,HeadsUpNotificationActionReceiver::class.java)
        }else{
            incomingCallDeclineButton = Intent("INCOMING_CALL_RECEIVER")
        }
        incomingCallDeclineButton.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        incomingCallDeclineButton.putExtra("DECLINE_CALL", true)
        incomingCallDeclineButton.putExtra("ACCEPT_CALL", false)
        incomingCallDeclineButton.putExtra("VIDEO_CALL", hasVideoCall)

        if(fromFirebase){
            //incomingCallAcceptButton = Intent(applicationContext,HeadsUpNotificationActionReceiver::class.java)
            incomingCallAcceptButton = getAcceptFirebaseIntent(context, hasVideoCall)
            fullScreenIntent = getAcceptFirebaseIntent(context, hasVideoCall)
           /* incomingCallAcceptButton = context.packageManager?.getLaunchIntentForPackage("com.ec.bond")!!
            incomingCallAcceptButton.putExtra("hasVideo",hasVideoCall)
            incomingCallAcceptButton.action="android.intent.action.MAIN"
            incomingCallAcceptButton.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)*/
        }else{
           // incomingCallAcceptButton = Intent("INCOMING_CALL_RECEIVER")
            incomingCallAcceptButton =   Intent(context, VoiceCallActivity::class.java).putExtra("fromFirebase",fromFirebase).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            fullScreenIntent =   Intent(context, VoiceCallActivity::class.java).putExtra("fromFirebase",fromFirebase)
        }
        incomingCallAcceptButton.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        incomingCallAcceptButton.putExtra("DECLINE_CALL", false)
        incomingCallAcceptButton.putExtra("ACCEPT_CALL", true)
        incomingCallAcceptButton.putExtra("VIDEO_CALL", hasVideoCall)

        fullScreenIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        fullScreenIntent.putExtra("DECLINE_CALL", false)
        fullScreenIntent.putExtra("ACCEPT_CALL", false)
        fullScreenIntent.putExtra("VIDEO_CALL", hasVideoCall)

        val pendingSwitchIntentDeclineCall: PendingIntent = PendingIntent.getBroadcast(context, 200, incomingCallDeclineButton, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        incomingCallNotificationUI.setOnClickPendingIntent(R.id.decline_call, pendingSwitchIntentDeclineCall)

        val pendingSwitchIntentAnswerCall: PendingIntent = PendingIntent.getActivity(
            context,
            201,
            incomingCallAcceptButton,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        incomingCallNotificationUI.setOnClickPendingIntent(R.id.answer_call, pendingSwitchIntentAnswerCall)

        val fullScreenIntent: PendingIntent = PendingIntent.getActivity(
            context,
            202,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
       var notification=NotificationUtility.displayCustomNotificationView1(context,
            Const.INCOMMING_NOTIFICATION_ID, remoteViews = incomingCallNotificationUI, remoteViewsHeadUp = incomingCallNotificationUI)
        notification.setFullScreenIntent(fullScreenIntent,true)
        notification.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
        notification.setDefaults(Notification.DEFAULT_VIBRATE)
        var note=notification.build()
        startForeground(Const.INCOMMING_NOTIFICATION_ID,note)

        /* handler.postDelayed(object : Runnable {
             override fun run() {

                // handler.postDelayed(this, 3000)
             }
         }, 100)*/
    }

    private fun getAcceptFirebaseIntent(context: Context, hasVideoCall: Boolean) : Intent {
        val resultIntent = context.packageManager?.getLaunchIntentForPackage("com.ec.bond")!!
        resultIntent.putExtra("hasVideo",hasVideoCall)
        resultIntent.action="android.intent.action.MAIN"
        resultIntent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return resultIntent;
    }
}