package com.ec.bond.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.robertlevonyan.components.picker.set
import com.ec.bond.BuildConfig
import com.ec.bond.R
import com.ec.bond.adapter.MemberAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBGroup
import com.ec.bond.utils.CommonUtils
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.fragment_picker.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class CreateGroupActivity : BaseActivity() {

    private val REQ_CAMERA_IMAGE: Int = 123
    private val REQ_GALLEY_IMAGE: Int = 456
    private lateinit var alertDialog: ProgressDialog
    var member = ArrayList<Contact>()
    var member_bbcontact = ArrayList<BBContact>()
    var imagepath = ""
    private lateinit var imagepickerbottomDialog: ImagePickerBottomDialog
    private lateinit var destination: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        val ab: Toolbar = findViewById(R.id.tb_toolbarsearch)
        setSupportActionBar(ab)
        val actionBar: ActionBar? = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.setTitle("New group")

        alertDialog = ProgressDialog(this)
        alertDialog.setMessage(getString(R.string.creating_group))
        alertDialog.setCancelable(false)
        initTextChangeListener()
        val mLayoutManagerHori: RecyclerView.LayoutManager = GridLayoutManager(this, 4)
        rv_member_list.layoutManager = mLayoutManagerHori

        if (intent.extras != null) {
            member = intent.extras!!.getParcelableArrayList<Contact>("list") as ArrayList<Contact>
            rv_member_list.adapter = MemberAdapter(this, member)
            (rv_member_list.adapter as MemberAdapter).notifyDataSetChanged()
            progressBar.visibility = View.GONE
            count.text = "Participants: ${member.size}"
        }
        ButtonSend.setOnClickListener {
            CommonUtils.hideKeybord(this@CreateGroupActivity)
            val subject = edt_subject.text.toString()
            if (subject.isNotEmpty() && !member.isNullOrEmpty()) {
                alertDialog.show()
                member.forEach {
                    val contact = it.contact
                    contact.registeredNumber = it.registerno!!
                    contact.setChatImagePath(it.image ?: "")
                    member_bbcontact.add(contact)
                }
                creategroup(subject, member_bbcontact, imagepath)
            }
        }

        img_pic.setOnClickListener {
            imagepickerbottomDialog = ImagePickerBottomDialog()
            imagepickerbottomDialog.show(supportFragmentManager, "image_picker_bottomDialog")
        }
    }

    private fun initTextChangeListener() {
        edt_subject.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val length: Int = edt_subject.length()
                char_count.text = (30 - length).toString()
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    fun onGallerySelect() {
        imagepickerbottomDialog.dismiss()
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQ_GALLEY_IMAGE)
    }

    fun onCameraSelect() {
        imagepickerbottomDialog.dismiss()
        destination = File(getExternalFilesDir(null), "image.jpg")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider",
                destination))
        startActivityForResult(intent, REQ_CAMERA_IMAGE)
    }

    fun creategroup(subject: String, list: ArrayList<BBContact>, imagepath: String) = GlobalScope.launch(Dispatchers.Main) {
        val group = Blackbox.createGroup(subject, list)
        if (group != null && !imagepath.isNullOrEmpty()) {
            group.setImage(imagepath)
            /*val intent = Intent(this@CreateGroupActivity, ChatBrowsingActivity::class.java)
            intent.putExtra("groupid", group!!.ID)*/
        } else {
            alertDialog.dismiss()
            this@CreateGroupActivity.finish()
        }
    }

    fun BBGroup.setImage(Imagepath: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            val isset = setGroupImage(Imagepath)
            runOnUiThread {
                alertDialog.dismiss()
                this@CreateGroupActivity.finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQ_CAMERA_IMAGE) {
                val bitmap = BitmapFactory.decodeFile(destination.path)
                img_pic.set(bitmap)
                imagepath = CommonUtils.bitmapToJpegFile(this, bitmap).path
            } else if (requestCode == REQ_GALLEY_IMAGE) {
                val path = CommonUtils.getPathFromUri(this, data!!.data!!)!!
                val bitmap = BitmapFactory.decodeFile(path)
                img_pic.set(bitmap)
                imagepath = CommonUtils.bitmapToJpegFile(this, bitmap).path

            }
        }
    }
}

class ImagePickerBottomDialog : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageCamera.setOnClickListener {
            (activity as CreateGroupActivity).onCameraSelect()
        }
        imageGallery.setOnClickListener {
            (activity as CreateGroupActivity).onGallerySelect()
        }
    }
}
