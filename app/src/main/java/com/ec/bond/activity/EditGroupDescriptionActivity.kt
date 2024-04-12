package com.ec.bond.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.aghajari.emojiview.AXEmojiManager
import com.aghajari.emojiview.emoji.iosprovider.AXIOSEmojiProvider
import com.aghajari.emojiview.view.AXEmojiPopup
import com.aghajari.emojiview.view.AXEmojiView
import com.ec.bond.R
import kotlinx.android.synthetic.main.activity_edit_group_description.*


class EditGroupDescriptionActivity : BaseActivity() {
    private val emojiView by lazy {
        AXEmojiView(this)
    }
    private val emojiPopup by lazy {
        AXEmojiPopup(emojiView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_group_description)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.enter_new_subject)
        initTextChangeListener()
        val groupDescription = intent?.extras?.getString("groupDescription") ?: ""
        description.setText(groupDescription)
        description.setSelection(groupDescription.length)
        AXEmojiManager.install(this, AXIOSEmojiProvider(this))
        AXEmojiManager.enableTouchEmojiVariantPopup()
        AXEmojiManager.getEmojiViewTheme().isFooterEnabled = true
        emojiView.editText = description
        initClickListeners()
    }

    private fun initTextChangeListener() {
        description.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val length: Int = description.length()
                char_count.text = (30 - length).toString()
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun initClickListeners() {
        cancel_description.setOnClickListener {
            finish()
        }
        confirm_description.setOnClickListener {
            val intent = Intent(this, ChatBrowsingDetailActivity::class.java)
            intent.putExtra("groupDescription", description.text.toString())
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        emoji_icon.setOnClickListener {
            emojiPopup.toggle();
        }
        description.setOnClickListener {
            if (emojiPopup.isShowing) {
                emojiPopup.dismiss()
            }
        }
    }
}