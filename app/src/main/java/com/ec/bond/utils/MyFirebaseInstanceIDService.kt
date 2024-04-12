package com.ec.bond.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ec.bond.R
import com.ec.bond.activity.HomeActivity
import com.ec.bond.activity.SplashScreenActivity
import com.ec.bond.activity.VoiceCallActivity
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.services.ForegroundService
import com.google.firebase.messaging.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyFirebaseInstanceIDService : FirebaseMessagingService() {
    private var type:String=""
    companion object{
        val MESSAGE_NOTIFICATION_ID=101
        val TEXT="newmsg";
        val AUDIO_CALL="voicecall"
        val VIDEO_CALL="videocall"
        val NEW_MESSAGE="You got a new message"
        val NEW_VOICE_CALL="Incoming voice call"
        val MESSAGE="Message"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        if(remoteMessage.data.isNotEmpty()){
            if(remoteMessage.data.containsKey("msg")){
                type=remoteMessage.data.get("msg")!!
                var background=SharePreferenceUtility.getPreferences1(applicationContext,Constant.IS_APP_IN_BACKGROUND,SharePreferenceUtility.PREFTYPE_BOOLEAN) as Boolean
                Log.e("message----",""+background+"=="+remoteMessage.data)
                when(type){
                    AUDIO_CALL -> {
                        if(background){
                            ForegroundService.startService(applicationContext,"",false,true)
                           // openCallingScreen(NEW_VOICE_CALL)
                        }
                    }
                    TEXT -> {
                        sendNotification(NEW_MESSAGE,MESSAGE,"")

                    }
                    VIDEO_CALL -> {
                        if(background){
                            sendNotification(NEW_MESSAGE,MESSAGE,"")
                        }
                    }


                }

            }
        }
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }


    private fun openCallingScreen(title: String){
        val intent = Intent(this, HomeActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(defaultSoundUri)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder.setFullScreenIntent(pendingIntent,true)
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(MESSAGE_NOTIFICATION_ID /* ID of notification */, notificationBuilder.build())
    }

    private fun sendNotification(
        name: String,
        title: String,
        image: String?
    ) {
        val intent = Intent(this, HomeActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(name)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(MESSAGE_NOTIFICATION_ID /* ID of notification */, notificationBuilder.build())
    }




}