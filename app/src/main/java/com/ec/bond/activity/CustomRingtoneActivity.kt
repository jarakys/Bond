package com.ec.bond.activity

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.ec.bond.R
import com.ec.bond.adapter.CustomRingtoneAdapter
import kotlinx.android.synthetic.main.activity_custom_ringtone.*


class CustomRingtoneActivity : BaseActivity(), CustomRingtoneAdapter.RingtoneSelectionListener {
    lateinit var customRingtoneAdapter: CustomRingtoneAdapter
    private var mp: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var selectedRingtone: String = "Default"
    private var selectedPosition: Int = 0
    private val arrayList = arrayListOf("Default", "tone - 1", "tone - 2", "tone - 3", "tone - 4", "tone - 5", "tone - 6", "tone - 7", "tone - 8", "tone - 9", "tone - 10")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_ringtone)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sound"
        val selectedRingtoneName = intent?.extras?.getString("selectedRingtone", "")
        if (!selectedRingtoneName.isNullOrEmpty()) {
            arrayList.mapIndexed { index, string ->
                if (string.replace("\\s".toRegex(), "").equals(selectedRingtoneName.replace("\\s".toRegex(), ""), ignoreCase = true)) {
                    selectedPosition = index
                }
            }
        }
        initMediaPlayer()
        initRingtoneRecyclerView()
        save_tone.setOnClickListener {
            val intent = Intent(this, ChatBrowsingDetailActivity::class.java)
            intent.putExtra("selectedRingtone", selectedRingtone)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun initMediaPlayer() {
        mp = MediaPlayer()
    }

    private fun initRingtoneRecyclerView() {
        customRingtoneAdapter = CustomRingtoneAdapter(this, arrayList, this, selectedPosition)
        ringtone_recycler_view.layoutManager = LinearLayoutManager(this)
        ringtone_recycler_view.adapter = customRingtoneAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRingtoneSelectionListener(position: Int) {
        startMedia(position)
    }

    private fun startMedia(position: Int) {
        mp?.stop()
        ringtone?.stop()
        var rawfile: String? = null
        when (position) {
            0 -> playDefaultSystemRingtone()
            1 -> {
                mp = MediaPlayer.create(this, R.raw.tone_1)
                rawfile = "tone_1.wav"
            }
            2 -> {
                mp = MediaPlayer.create(this, R.raw.tone_2)
                rawfile = "tone_2.wav"
            }
            3 -> {
                mp = MediaPlayer.create(this, R.raw.tone_3)
                rawfile = "tone_3.wav"
            }
            4 -> {
                mp = MediaPlayer.create(this, R.raw.tone_4)
                rawfile = "tone_4.wav"
            }
            5 -> {
                mp = MediaPlayer.create(this, R.raw.tone_5)
                rawfile = "tone_5.wav"
            }
            6 -> {
                mp = MediaPlayer.create(this, R.raw.tone_6)
                rawfile = "tone_6.wav"
            }
            7 -> {
                mp = MediaPlayer.create(this, R.raw.tone_7)
                rawfile = "tone_7.wav"
            }
            8 -> {
                mp = MediaPlayer.create(this, R.raw.tone_8)
                rawfile = "tone_8.wav"
            }
            9 -> {
                mp = MediaPlayer.create(this, R.raw.tone_9)
                rawfile = "tone_9.wav"
            }
            10 -> {
                mp = MediaPlayer.create(this, R.raw.tone_10)
                rawfile = "tone_10.wav"
            }
        }
        selectedRingtone = rawfile ?: arrayList[position]
        mp?.start()
    }

    private fun playDefaultSystemRingtone() {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}