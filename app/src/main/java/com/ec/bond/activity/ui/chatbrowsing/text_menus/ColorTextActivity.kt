package com.ec.bond.activity.ui.chatbrowsing.text_menus

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.text.color
import com.apandroid.colorwheel.ColorWheel
import com.ec.bond.R
import kotlinx.android.synthetic.main.activity_color_text.*

class ColorTextActivity : Activity() {
    var selectedColor: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_text)
        val colorWheel = findViewById<ColorWheel>(R.id.colorWheel)

        colorWheel.rgb = Color.rgb(0, 0, 0)

        val currentColor = colorWheel.rgb
        selectedColor = currentColor
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        val readonly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        root_layout.setOnClickListener {
            if(!readonly && text != null){
                val hex = String.format("#%02x%02x%02x", selectedColor?.red, selectedColor?.green, selectedColor?.blue)
                val outgoingIntent = Intent()
                val spanString = SpannableStringBuilder()
                        .append("<color hex=\"$hex\">")
                        .color(selectedColor!!) { append("$text") }
                        .append("</color>")
                outgoingIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, spanString)
                setResult(Activity.RESULT_OK, outgoingIntent)

            } else {
                Toast.makeText(this, "Text cannot be modified", Toast.LENGTH_SHORT).show()
            }

            finish()
        }
        colorWheel.colorChangeListener = { rgb: Int ->
            selectedColor = rgb
        }

    }
}