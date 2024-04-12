package com.ec.bond.worker

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ec.bond.R
import com.ec.bond.activity.VoiceCallActivity
import com.ec.bond.common.Const
import com.ec.bond.utils.HeadsUpNotificationActionReceiver
import com.ec.bond.utils.NotificationUtility
import okhttp3.internal.notify


class IncommingCallWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    lateinit var incomingCallDeclineButton:Intent;
    lateinit var incomingCallAcceptButton:Intent
    lateinit var fullScreenIntent:Intent

    override fun doWork(): Result {
        //call methods to perform background task
        //get Input Data back using "inputData" variable
        val callerName =  inputData.getString(Const.CALLER_NAME).orEmpty()
        val hasVideoCall =  inputData.getBoolean(Const.HAS_VIDEO, false)
        handleIncommingCall(callerName, hasVideoCall)
        return Result.success()
    }

    companion object {
        private const val TAG = "BackupWorker"
    }

    private fun handleIncommingCall(callerName: String, hasVideoCall: Boolean) {
        showNotification(applicationContext,callerName,hasVideoCall);
    }

    private fun showNotification(context: Context, callerName: String, hasVideoCall: Boolean = false,fromFirebase:Boolean=false) {
        Log.e("binh", "showNotification in incomming call worker")
        val incomingCallNotificationUI = if (hasVideoCall) {
            RemoteViews(context.packageName, R.layout.incoming_call_video_notification)
        } else {
            RemoteViews(context.packageName, R.layout.incoming_call_notification)
        }
        incomingCallNotificationUI.setTextViewText(R.id.person_name, callerName)
        incomingCallDeclineButton = Intent("INCOMING_CALL_RECEIVER").apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("DECLINE_CALL", true)
            putExtra("ACCEPT_CALL", false)
            putExtra("VIDEO_CALL", hasVideoCall)
        }


        incomingCallAcceptButton =   Intent(context, VoiceCallActivity::class.java).putExtra("fromFirebase",fromFirebase).addFlags(
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("DECLINE_CALL", false)
            putExtra("ACCEPT_CALL", true)
            putExtra("VIDEO_CALL", hasVideoCall)
        }


        fullScreenIntent =   Intent(context, VoiceCallActivity::class.java).putExtra("fromFirebase",fromFirebase)
            .apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("ACCEPT_CALL", false)
                putExtra("DECLINE_CALL", false)
                putExtra("VIDEO_CALL", hasVideoCall)
            }

        val pendingSwitchIntentDeclineCall: PendingIntent = PendingIntent.getBroadcast(context, 100, incomingCallDeclineButton, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        incomingCallNotificationUI.setOnClickPendingIntent(R.id.decline_call, pendingSwitchIntentDeclineCall)

        val pendingSwitchIntentAnswerCall = PendingIntent.getActivity(
            context,
            101,
            incomingCallAcceptButton,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        incomingCallNotificationUI.setOnClickPendingIntent(R.id.answer_call, pendingSwitchIntentAnswerCall)


        val fullScreenIntent: PendingIntent = PendingIntent.getActivity(
            context,
            102,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var notification= NotificationUtility.displayCustomNotificationView1(context,
            Const.INCOMMING_NOTIFICATION_ID, remoteViews = incomingCallNotificationUI, remoteViewsHeadUp = incomingCallNotificationUI)
        notification.setFullScreenIntent(fullScreenIntent,true)
        notification.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
        notification.setDefaults(Notification.DEFAULT_VIBRATE)
        //var note=notification.build()
        val mNotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(Const.INCOMMING_NOTIFICATION_ID, notification.build())
        //NotificationManagerCompat.from(applicationContext).notify(100001, notification.build())
    }

}