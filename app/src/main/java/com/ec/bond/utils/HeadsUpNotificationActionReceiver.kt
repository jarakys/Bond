package com.ec.bond.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.provider.Settings
import com.ec.bond.BondApp
import com.ec.bond.activity.BMediaPlayer
import com.ec.bond.common.Const
import com.ec.bond.services.ForegroundService


class HeadsUpNotificationActionReceiver : BroadcastReceiver() {
    public var decline:Boolean=false
    public var accept:Boolean=false
    public var  hasVideo:Boolean=false
    public var player: MediaPlayer?=null
    override fun onReceive(p0: Context?, p1: Intent?) {
        p1?.let {
            decline= it?.getBooleanExtra("DECLINE_CALL",false)
            accept= it?.getBooleanExtra("ACCEPT_CALL",false)
            hasVideo= it?.getBooleanExtra("VIDEO_CALL",false)
            val notificationManager = p0?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager?.cancel(Const.INCOMMING_NOTIFICATION_ID)
            p0?.stopService(Intent(p0, ForegroundService::class.java))
            NotificationUtility.clearNotification(p0,Const.INCOMMING_NOTIFICATION_ID)
            BMediaPlayer.stopRingtune()
            if (!decline)
                perfromClick(accept,hasVideo,p0)
        }
    }

    fun stopPlayRingings(){
        player = MediaPlayer.create(
            BondApp.applicationContext(),
            Settings.System.DEFAULT_RINGTONE_URI
        )
       player?.isLooping=true

        player?.let {
            if(it.isPlaying){
                it?.stop()
                it?.release()
            }
        }
    }


    fun perfromClick(accept:Boolean,hasVideo:Boolean,p0: Context?){
        val i: Intent = p0?.getPackageManager()?.getLaunchIntentForPackage("com.ec.bond")!!
        i.putExtra("hasVideo",hasVideo)
        i.action="android.intent.action.MAIN"
        i.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        p0?.startActivity(i)
       // p0?.startActivity(Intent(p0!!, SplashScreenActivity::class.java).putExtra("hasVideo",hasVideo).addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))


    }
}