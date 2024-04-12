package com.ec.bond.activity.ui.chatbrowsing.text_menus

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.core.text.underline

class UnderlinedTextActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        val readonly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        if(!readonly && text != null) {
            if (!text.contains("•")) {

                val outgoingIntent = Intent()
                val spanString = SpannableStringBuilder()
                        .append("•")
                        .underline { append("$text") }
                        .append("•")
                outgoingIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, spanString)
                setResult(Activity.RESULT_OK, outgoingIntent)

            } else {
                Toast.makeText(this, "Text cannot be modified", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        finish()
        startActivity(intent)
    }
}