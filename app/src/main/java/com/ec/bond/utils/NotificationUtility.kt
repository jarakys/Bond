package com.ec.bond.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ec.bond.R
import com.ec.bond.activity.BMediaPlayer
import com.ec.bond.activity.vibrator
import com.ec.bond.blackbox.model.Message
import com.ec.bond.blackbox.model.MessageType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.internal.LinkedTreeMap


/**
 * Make an notification Utility class
 * This will create notification and it will also handle notification channels for oreo and above supported devices
 * */
class NotificationUtility {
    companion object {
        private const val bundleNotificationId = 100009

        private fun initNotification(
                pContext: Context,
                pString: String? = pContext.getString(R.string.app_name),
                pTitle: String,
                channelId: String = pContext.packageName,
                channelName: String = pContext.packageName,
                pUri: Uri? = null
        ): NotificationCompat.Builder {
            val appName = pContext.resources.getString(R.string.app_name)
            val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(pContext, channelId, channelName, pUri)
            } else {
                Constant.sEmptyString
            }
            val sound = pUri ?: try {
                //Define sound URI
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            } catch (exception: Exception) {
                null
            }

            return NotificationCompat.Builder(pContext, channel)

                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setTicker(appName)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(pTitle))
                    .setContentTitle(pTitle)
                    .setContentText(pString).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            priority = NotificationManager.IMPORTANCE_HIGH
                        }
                    }
                    .setVibrate(LongArray(0))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                    .apply {
                        if (sound != null) {
                            setSound(sound)
                        }
                    }
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setWhen(System.currentTimeMillis())
                    .setColor(ContextCompat.getColor(pContext, R.color.colorAccent))
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createNotificationChannel(
                pContext: Context,
                channelId: String,
                channelName: String,
                pUri: Uri? = null
        ): String {
            val att = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            val sound = pUri ?: try {
                //Define sound URI
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } catch (exception: Exception) {
                null
            }

            val chan = NotificationChannel(
                    channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH
            )
            chan.lightColor = Color.GREEN
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            chan.setSound(sound, att)
            chan.enableVibration(true)
            val service = pContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
            return channelId
        }

        fun displaySingleNotification(
                pContext: Context,
                pNotificationId: Int,
                pMessage: String?,
                pTitle: String,
                intent: Intent,
                pUri: Uri? = null
        ) {
            val notification = initNotification(pContext, pMessage, pTitle)
            val pendingIntent =
                    PendingIntent.getActivity(pContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            notification.setContentIntent(pendingIntent)

            val notificationManager =
                    pContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(pNotificationId, notification.build())
        }

        @Synchronized
        fun displayBundledNotification(pContext: Context,
                                       pNotificationId: Int,
                                       pMessage: Message,
                                       pTitle: String,
                                       intent: Intent,
                                       groupId: String,
                                       channelId: String,
                                       channelName: String,
                                       notificationTitle: String,
                                       pUri: Uri? = null) {
            val map = saveAndGetDataFromPreferencees(pContext, pNotificationId, pMessage)
            val conversationTitle = if (map.keys.size > 1) {
                "" + map.values.stream().mapToInt(ArrayList<Message>::size).sum() + " messages from " + map.keys.size + " chats"
            } else {
                val message = map.values.stream().mapToInt(ArrayList<Message>::size).sum()
                if (message > 1) {
                    "" + message + " messages from " + map.keys.size + " chat"
                } else {
                    "" + message + " message from " + map.keys.size + " chat"
                }
            }
            val pendingIntent = PendingIntent.getActivity(pContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val summaryNotificationBuilder = initNotification(pContext, pMessage.body, pTitle, channelId = channelId, channelName = channelName, pUri = pUri)
            summaryNotificationBuilder.setGroup("bundle_notification_$groupId")
            summaryNotificationBuilder.setGroupSummary(true)
            if (notificationTitle.isEmpty()) {
                summaryNotificationBuilder.setStyle(getStyleForNotification(context = pContext,
                        pNotificationId = pNotificationId, title = pTitle, summmaryTitle = conversationTitle))
            } else {
                if (map.keys.size == 1) {
                    summaryNotificationBuilder.setStyle(getStyleForNotification(context = pContext,
                            pNotificationId = pNotificationId, title = pTitle, summmaryTitle = null))
                } else {
                    summaryNotificationBuilder.setStyle(getStyleForNotification(context = pContext,
                            pNotificationId = pNotificationId, title = pTitle, summmaryTitle = conversationTitle))
                }
            }
            summaryNotificationBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            summaryNotificationBuilder.setContentIntent(pendingIntent)

            //actual notification display in the notification menu
            val notification = initNotification(pContext, pMessage.body, pTitle, channelId = channelId, channelName = channelName, pUri = pUri)
            notification.setGroup("bundle_notification_$groupId")
            notification.setGroupSummary(false)
            if (notificationTitle.isEmpty() && map.size == 1) {
                notification.setStyle(getStyleForNotification(context = pContext,
                        pNotificationId = pNotificationId, title = pTitle, summmaryTitle = null))
            } else {
                notification.setStyle(getStyleForNotification(context = pContext,
                        pNotificationId = pNotificationId, title = pTitle, summmaryTitle = notificationTitle))
            }
            notification.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            notification.setContentIntent(pendingIntent)
            notification.setCategory(NotificationCompat.CATEGORY_MESSAGE)

            val notificationManager = pContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(bundleNotificationId, summaryNotificationBuilder.build())
            notificationManager.notify(pNotificationId, notification.build())
        }

        private fun saveAndGetDataFromPreferencees(context: Context, pNotificationId: Int, messageBody: Message): HashMap<String, ArrayList<Message>> {
            val sharedPref = context.getSharedPreferences("SaveNotificationData", 0)
            val notificationData = sharedPref.getString("notificationData", "")
            val map = if (notificationData.isNullOrEmpty()) {
                HashMap()
            } else {
                val storedHashMapString = sharedPref.getString("notificationData", "")
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.fromJson(storedHashMapString, HashMap::class.java) as HashMap<String, ArrayList<Message>>
            }
            val list = ArrayList<Message>()
            if (map.containsKey("" + pNotificationId)) {
                list.addAll(map.get("" + pNotificationId) ?: ArrayList())
            }
            list.add(messageBody)
            map["" + pNotificationId] = list
            sharedPref.edit().putString("notificationData", Gson().toJson(map)).apply()
            return map
        }

        fun clearDataFromPreferences(context: Context, pNotificationId: Int) {
            val sharedPref = context.getSharedPreferences("SaveNotificationData", 0)
            val notificationData = sharedPref.getString("notificationData", "")
            val map = if (notificationData.isNullOrEmpty()) {
                HashMap()
            } else {
                val storedHashMapString = sharedPref.getString("notificationData", "")
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.fromJson(storedHashMapString, HashMap::class.java) as HashMap<String, ArrayList<Message>>
            }
            map.remove("" + pNotificationId)
            sharedPref.edit().putString("notificationData", Gson().toJson(map)).apply()
        }

        private fun getStyleForNotification(context: Context, summmaryTitle: String?,
                                            title: String, pNotificationId: Int): NotificationCompat.InboxStyle {
            val inbox: NotificationCompat.InboxStyle = NotificationCompat.InboxStyle()
            val sharedPref = context.getSharedPreferences("SaveNotificationData", 0)
            val notificationData = sharedPref.getString("notificationData", "")
            val gson = GsonBuilder().setPrettyPrinting().create()
            val notificationMessages = gson.fromJson(notificationData, HashMap::class.java) as HashMap<String, ArrayList<Message>>
            val myNewHashMap: MutableMap<String, ArrayList<Message>> = HashMap()
            for ((key, value) in notificationMessages) {
                myNewHashMap[key] = value
            }
            val messages = myNewHashMap["" + pNotificationId]
            for (index in 0 until messages?.size!!) {
                val messageInstance = (gson.fromJson(gson.toJson(messages), ArrayList::class.java)[index] as LinkedTreeMap<*, *>)
                val type = messageInstance["type"]
                val messageBody = messageInstance["body"]
                if (type == MessageType.Text.name) {
                    inbox.addLine("" + messageBody)
                } else {
                    inbox.addLine("" + type)
                }
            }
            inbox.setBigContentTitle(title)
            inbox.setSummaryText(summmaryTitle)
            return inbox
        }

        private fun getStyleNotificationInMessagingStyle(context: Context, summmaryTitle: String?,
                                                         title: String, pNotificationId: Int): NotificationCompat.MessagingStyle {
            val p: androidx.core.app.Person = androidx.core.app.Person.Builder().setName(title).build()
            val inbox: NotificationCompat.MessagingStyle = NotificationCompat.MessagingStyle(p)
            val sharedPref = context.getSharedPreferences("SaveNotificationData", 0)
            val notificationData = sharedPref.getString("notificationData", "")
            val gson = GsonBuilder().setPrettyPrinting().create()
            val notificationMessages = gson.fromJson(notificationData, HashMap::class.java) as HashMap<String, ArrayList<Message>>
            val myNewHashMap: MutableMap<String, ArrayList<Message>> = HashMap()
            for ((key, value) in notificationMessages) {
                myNewHashMap[key] = value
            }
            val messages = myNewHashMap["" + pNotificationId]
            for (index in 0 until messages?.size!!) {
                val messageInstance = (gson.fromJson(gson.toJson(messages), ArrayList::class.java)[index] as LinkedTreeMap<*, *>)
                val type = messageInstance["type"]
                val messageBody = messageInstance["body"]
                if (type == MessageType.Text.name) {
                    inbox.addMessage("" + messageBody, System.currentTimeMillis(), p)
                } else {
                    inbox.addMessage("" + type, System.currentTimeMillis(), p)
                }
            }
            inbox.conversationTitle = summmaryTitle
            return inbox
        }

        fun displayCustomNotificationView(pContext: Context, pNotificationId: Int, remoteViews: RemoteViews, remoteViewsHeadUp: RemoteViews, pUri: Uri? = null) {
            val notification = initNotification(pContext = pContext, pTitle = "", pUri = pUri)
            notification.setStyle(NotificationCompat.BigTextStyle())
            notification.setCustomContentView(remoteViews)
            notification.setCustomHeadsUpContentView(remoteViewsHeadUp)
            notification.setCustomBigContentView(remoteViews)
            notification.setOngoing(false)
            notification.setAutoCancel(false);
            notification.setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS)
            notification.setCategory(NotificationCompat.CATEGORY_CALL)
            notification.setPriority(NotificationCompat.PRIORITY_MAX)
            notification.setWhen(0)
            notification.setShowWhen(true)
            val mNotificationManager = pContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.notify(pNotificationId, notification.build())

        }

        @SuppressLint("WrongConstant")
        fun displayCustomNotificationView1(pContext: Context, pNotificationId: Int, remoteViews: RemoteViews, remoteViewsHeadUp: RemoteViews, pUri: Uri? = null):NotificationCompat.Builder {
            BMediaPlayer.playRingtune()
            val notification = initNotification(pContext = pContext, pTitle = "", pUri = pUri)
            notification.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            notification.setStyle(NotificationCompat.BigTextStyle())
            notification.setCustomContentView(remoteViews)
            notification.setCustomHeadsUpContentView(remoteViewsHeadUp)
            notification.setCustomBigContentView(remoteViews)
            notification.setOngoing(true)
            notification.setAutoCancel(false);
            notification.setCategory(NotificationCompat.CATEGORY_ALARM)
            notification.setPriority(NotificationCompat.PRIORITY_HIGH)
            notification.setWhen(0)
            notification.setVibrate(longArrayOf(0, 250, 250, 250))
            val mNotificationManager = pContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //mNotificationManager.notify(pNotificationId, notification.build())
            return notification

        }
        private fun vibrate(v:LongArray) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(v, 0));
                //vibrator?.vibrate(v,0)
            } else {
                vibrator?.vibrate(450)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun deleteChannel(pContext: Context, receipient: String) {
            val notificationManager = pContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val list = notificationManager.notificationChannels
            var channelId = ""
            for (index in 0 until list.size) {
                val underScoreCharAt = list[index].id.indexOf("_")
                if (underScoreCharAt == -1) {
                    continue
                }
                try {
                    val receipientGet = list[index].id.substring(0, underScoreCharAt).toLong() - 123456
                    val id = receipientGet.toString()
                    if (id.contains(receipient)) {
                        channelId = list[index].id
                        break
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            if (channelId.isNotEmpty()) {
                notificationManager.deleteNotificationChannel(channelId)
            }
        }

        fun clearNotification(pContext: Context?, pNotificationId: Int) {
            BMediaPlayer.stopRingtune()
            vibrator?.let {
                if(it?.hasVibrator()){
                    Log.e("vibratorr_cancel","cancel")
                    val keyguardManager: KeyguardManager = pContext?.getSystemService (Context.KEYGUARD_SERVICE) as KeyguardManager
                    var powerManager = pContext.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
                    var isScreenOn: Boolean
                    isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                        powerManager.isInteractive
                    } else {
                        powerManager.isScreenOn
                    }

                    if (!isScreenOn) {

                    }else{
                        it.cancel()
                    }

                }

            }
            val notificationManager = pContext?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager?.cancel(pNotificationId)
        }

        fun clearAllNotifications(pContext: Context?) {
            val notificationManager = pContext?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager?.cancelAll()
        }
    }
}