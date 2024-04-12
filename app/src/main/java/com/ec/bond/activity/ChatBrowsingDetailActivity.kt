package com.ec.bond.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.android.material.appbar.AppBarLayout
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingDetailFactory
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingDetailViewModel
import com.ec.bond.adapter.ChatBrowsingDetailGroupListAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBCall
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBGroup
import com.ec.bond.blackbox.model.BBGroupRole
import com.ec.bond.custom_views.HeaderView
import com.ec.bond.extensions.setCustomChecked
import com.ec.bond.utils.*
import kotlinx.android.synthetic.main.activity_chat_browsing_detail.*
import kotlinx.android.synthetic.main.widget_header_view.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


class ChatBrowsingDetailActivity : BaseActivity(),
        AppBarLayout.OnOffsetChangedListener,
        HeaderView.ClickListener,
        ChatBrowsingDetailGroupListAdapter.GroupMembersItemClickListener,
        DatePickerUtility.DatePickerInterface,
        TimePickerUtility.TimePickerInterface {

    private lateinit var chatBrowsingDetailViewModel: ChatBrowsingDetailViewModel
    private lateinit var groupListAdapter: ChatBrowsingDetailGroupListAdapter
    private var openMessageScreen = false
    private var isHideToolbarView = false
    private var groupMembersList = ArrayList<BBContact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_browsing_detail)

        //for single contact
        intent?.extras?.getString("registeredNumber")?.let { regNum ->
            val contact = Blackbox.getContact(regNum)
                    ?: Blackbox.getTemporaryContact(regNum)
            if (contact == null) {
                finish()
                return@let
            }
            val factory = ChatBrowsingDetailFactory(contact)
            chatBrowsingDetailViewModel = ViewModelProvider(this, factory).get(ChatBrowsingDetailViewModel::class.java)
        }

        //for group chat
        intent?.extras?.getString("groupId")?.let { groupId ->
            val group: BBGroup? = Blackbox.chatItems.value?.map { it.group }?.firstOrNull { it?.ID == groupId }
            if (group == null) {
                finish()
                return@let
            }
            val factory = ChatBrowsingDetailFactory(group)
            chatBrowsingDetailViewModel = ViewModelProvider(this, factory).get(ChatBrowsingDetailViewModel::class.java)
        }

        initCollapsingToolbarUi()

        if (!this::chatBrowsingDetailViewModel.isInitialized) {
            return
        }

        phone_number.text = chatBrowsingDetailViewModel.getRegisteredNumber()
        if (chatBrowsingDetailViewModel.getStatusMessage().isNotEmpty()) {
            status.text = chatBrowsingDetailViewModel.getStatusMessage()
            phoneNumTv.text = getString(R.string.about_and_phone_number)
        } else {
            status.visibility = View.GONE
            status_divider_view.visibility = View.GONE
        }

        clear_chat.setOnClickListener {
            UserAlertUtility.showAlertDialog("Delete all messages?", "", this,
                    { _, _ ->
                        run {
                            UserProgressUtility.showProgressDialog(this)
                            chatBrowsingDetailViewModel.clearChat()
                        }
                    }, { _, _ -> UserAlertUtility.hideAlertDialog() }, getString(R.string.action_deleteMsg), getString(R.string.cancel))
        }
        chatBrowsingDetailViewModel.getClearChatResponse().observe(this, {
            UserProgressUtility.hideProgressDialog()
        })

        chatBrowsingDetailViewModel.getMemberListResponse().observe(this, {
            groupMembersList.clear()
            groupMembersList.addAll(chatBrowsingDetailViewModel.getGroupMembers())
            groupListAdapter.notifyDataSetChanged()
            UserProgressUtility.hideProgressDialog()
        })

        chatBrowsingDetailViewModel.getDescriptionUpdateResponse().observe(this, {
            if (toolbar_header_view is HeaderView) {
                (toolbar_header_view as HeaderView).updateName(chatBrowsingDetailViewModel.getChatName())
            }
            if (float_header_view is HeaderView) {
                (float_header_view as HeaderView).updateName(chatBrowsingDetailViewModel.getChatName())
            }
            UserProgressUtility.hideProgressDialog()
        })

        chatBrowsingDetailViewModel.getExitAndDeleteGroupResponse().observe(this, {
            UserProgressUtility.hideProgressDialog()
            if (it) {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.pull_right, R.anim.pull_left)
                finish()
            }
        })

        chatBrowsingDetailViewModel.getAddGroupMembersResponse().observe(this, {
            groupMembersList.addAll(it)
            groupListAdapter.notifyDataSetChanged()
            UserProgressUtility.hideProgressDialog()
        })

        chatBrowsingDetailViewModel.getGroupExpiryDateResponse().observe(this, {
            UserProgressUtility.hideProgressDialog()
            //TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            if (it.first == false) {
                temporary_group.setCustomChecked(it.third != true, temporaryGroupSwitchListener)
                return@observe
            }
            if (it.second != null) {
                temporary_group_delete_date_container.visibility = View.VISIBLE
                temporary_group_delete_date.text = it.second?.dateString(DateStyle.long)
                temporary_group.setCustomChecked(true, temporaryGroupSwitchListener)
            } else {
                temporary_group_delete_date_container.visibility = View.GONE
                temporary_group_delete_date.text = ""
                temporary_group.setCustomChecked(false, temporaryGroupSwitchListener)
            }
        })

        chatBrowsingDetailViewModel.getRingtoneNameResponse().observe(this, {
            UserProgressUtility.hideProgressDialog()
            custom_tone_name.text = it.second
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.first == true) {
                val recipient = if (chatBrowsingDetailViewModel.getRegisteredNumber().isNotEmpty()) {
                    chatBrowsingDetailViewModel.getRegisteredNumber()
                } else if (!chatBrowsingDetailViewModel.group?.ID.isNullOrEmpty()) {
                    chatBrowsingDetailViewModel.group?.ID
                } else {
                    ""
                }
                if (recipient?.isNotEmpty()!!) {
                    NotificationUtility.deleteChannel(this, receipient = recipient)
                }
            }
        })

        if (chatBrowsingDetailViewModel.isGroupChat()) {
            exit_group.visibility = View.VISIBLE
            group_members_recyclerview.visibility = View.VISIBLE
            status_and_phone_container.visibility = View.GONE
            initGroupListRecyclerView()
        }

        if (chatBrowsingDetailViewModel.isGroupAdmin()) {
            temporary_group_container.visibility = View.VISIBLE
            delete_group.visibility = View.VISIBLE
        }

        video_call.setOnClickListener {
            val contact = chatBrowsingDetailViewModel.getContactData() ?: return@setOnClickListener
            makeVideoCall(contact)
        }
        voice_call.setOnClickListener {
            val contact = chatBrowsingDetailViewModel.getContactData() ?: return@setOnClickListener
            makeVoiceCall(contact)
        }
        message.setOnClickListener {
            chatBrowsingDetailViewModel.getContactData()?.let { openMessageScreen(it) }
        }
        exit_group.setOnClickListener {
            UserAlertUtility.showAlertDialog("Exit ${chatBrowsingDetailViewModel.getChatName()} group?",
                    "",
                    this, { _, _ ->
                UserProgressUtility.showProgressDialog(this)
                chatBrowsingDetailViewModel.exitGroup()
            }, { _, _ -> UserAlertUtility.hideAlertDialog() }, getString(R.string.exit), getString(R.string.cancel))
        }
        delete_group.setOnClickListener {
            UserAlertUtility.showAlertDialog("Delete ${chatBrowsingDetailViewModel.getChatName()} group?",
                    "",
                    this, { _, _ ->
                UserProgressUtility.showProgressDialog(this)
                chatBrowsingDetailViewModel.deleteGroup()
            }, { _, _ -> UserAlertUtility.hideAlertDialog() }, getString(R.string.delete), getString(R.string.cancel))
        }
        starred_message.setOnClickListener {
            if(chatBrowsingDetailViewModel.getRegisteredNumber().isNotEmpty()){
                val settingIntent = Intent(this, StarredActivity::class.java).putExtra("recipent",chatBrowsingDetailViewModel.getRegisteredNumber())
                startActivity(settingIntent)
            }


        }
        custom_tone.setOnClickListener {
            val intent = Intent(this@ChatBrowsingDetailActivity, CustomRingtoneActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("selectedRingtone", chatBrowsingDetailViewModel.getRingtoneName())
            startActivityForResult(intent, 1005)
        }
    }

    private fun initCollapsingToolbarUi() {
        setSupportActionBar(anim_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appbar.post {
            setAppBarOffset()
        }
        chatBrowsingDetailViewModel.getImage()?.let {
            Glide.with(this)
                    .asBitmap()
                    .load(BitmapFactory.decodeFile(it))
                    .placeholder(R.drawable.placeholder)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.placeholder)
                            makePaletteForCollapsingBar(bitmap)
                            return false
                        }

                        override fun onResourceReady(resource: Bitmap?, model: Any?, target: com.bumptech.glide.request.target.Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            if (resource != null) {
                                makePaletteForCollapsingBar(resource)
                            } else {
                                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.placeholder)
                                makePaletteForCollapsingBar(bitmap)
                            }
                            return false
                        }

                    })
                    .into(headerImage)
        }

        appbar.addOnOffsetChangedListener(this)

        if (toolbar_header_view is HeaderView) {
            (toolbar_header_view as HeaderView).setClickListenerInstance(this)
            (toolbar_header_view as HeaderView).bindTo(chatBrowsingDetailViewModel.getChatName(), chatBrowsingDetailViewModel.isGroupChat() && chatBrowsingDetailViewModel.isGroupAdmin())
        }
        if (float_header_view is HeaderView) {
            (float_header_view as HeaderView).setClickListenerInstance(this)
            (float_header_view as HeaderView).bindTo(chatBrowsingDetailViewModel.getChatName(), chatBrowsingDetailViewModel.isGroupChat() && chatBrowsingDetailViewModel.isGroupAdmin())
        }
    }

    private fun makePaletteForCollapsingBar(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val vibrantColor: Int? = palette?.getVibrantColor(ContextCompat.getColor(this@ChatBrowsingDetailActivity, R.color.colorPrimary))
            collapsing_toolbar.setContentScrimColor(vibrantColor
                    ?: ContextCompat.getColor(this@ChatBrowsingDetailActivity, R.color.colorPrimary))
            collapsing_toolbar.setStatusBarScrimColor(ContextCompat.getColor(this@ChatBrowsingDetailActivity, R.color.colorPrimary))
            vibrantColor?.let { window.statusBarColor = CommonUtils.manipulateColor(it, 0.90f) }
        }
    }

    private fun initGroupListRecyclerView() {
        groupMembersList.addAll(chatBrowsingDetailViewModel.getGroupMembers())
        groupListAdapter = ChatBrowsingDetailGroupListAdapter(this,
                groupMembersList,
                chatBrowsingDetailViewModel.isGroupAdmin(),
                chatBrowsingDetailViewModel.group?.ID, this)
        group_members_recyclerview.layoutManager = LinearLayoutManager(this)
        group_members_recyclerview.adapter = groupListAdapter
    }

    private fun setAppBarOffset() {
        val params = appbar.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as AppBarLayout.Behavior?
        behavior!!.onNestedPreScroll(coordinator, appbar, appbar, 0, 250, intArrayOf(0, 0), ViewCompat.TYPE_TOUCH)

        if (intent?.extras?.getBoolean("notPerformHeaderAnimation", false) == false) {
            CommonUtils.animateView(headerImage, 100, appbar.height, boundHeight = true, duration = 300L)
            CommonUtils.animateView(headerImage, 200, appbar.width, boundWidth = true, duration = 300L)
        }
    }

    private fun makeVoiceCall(contact: BBContact) {
        val call = BBCall(isOutgoing = true)
        call.addContact(contact)
        Blackbox.openCallActivity(call, this)
    }

    private fun makeVideoCall(contact: BBContact) {
        val call = BBCall(isOutgoing = true, hasVideo = true)
        call.addContact(contact)
        Blackbox.openCallActivity(call, this)
    }

    private fun openMessageScreen(contact: BBContact) {
        openMessageScreen = true
        val intent = Intent(this, ChatBrowsingActivity::class.java)
        val recipient = contact.registeredNumber
        intent.putExtra("recipient", recipient)
        startActivity(intent)
        overridePendingTransition(R.anim.pull_right, R.anim.pull_left)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1003 && resultCode == RESULT_OK) {
            val description = data?.extras?.getString("groupDescription")
            if (description.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.subject_not_empty), Toast.LENGTH_SHORT).show()
            } else {
                //update group description
                UserProgressUtility.showProgressDialog(this)
                chatBrowsingDetailViewModel.updateGroupDescription(description)
            }
        } else if (requestCode == 1004 && resultCode == RESULT_OK) {
            val list = data?.extras?.getParcelableArrayList<BBContact>("bbContactList")
            list?.let {
                UserProgressUtility.showProgressDialog(this)
                chatBrowsingDetailViewModel.addGroupMembers(it)
            }
        } else if (requestCode == 1005 && resultCode == RESULT_OK) {
            val selectedRingtoneName = data?.extras?.getString("selectedRingtone", "")
            if (selectedRingtoneName?.isNotEmpty()!!) {
                UserProgressUtility.showProgressDialog(this)
                chatBrowsingDetailViewModel.setRingtone(selectedRingtoneName)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.non_slide, R.anim.slide_down)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (openMessageScreen) {
            super.onBackPressed()
        } else {
            finish()
            overridePendingTransition(R.anim.non_slide, R.anim.slide_down)
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, offset: Int) {
        val maxScroll = appBarLayout!!.totalScrollRange
        val percentage = abs(offset).toFloat() / maxScroll.toFloat()
        anim_toolbar.title = ""
        toolbar_header_view.visibility = View.VISIBLE
        if (percentage == 1f && isHideToolbarView) {
            toolbar_header_view.name.visibility = View.VISIBLE
            if (chatBrowsingDetailViewModel.isGroupChat() && chatBrowsingDetailViewModel.isGroupAdmin()) {
                toolbar_header_view.edit_icon.visibility = View.VISIBLE
            } else {
                toolbar_header_view.edit_icon.visibility = View.GONE
            }
            isHideToolbarView = !isHideToolbarView
        } else if (percentage < 1f && !isHideToolbarView) {
            toolbar_header_view.name.visibility = View.GONE
            toolbar_header_view.edit_icon.visibility = View.GONE
            isHideToolbarView = !isHideToolbarView
        }
    }

    override fun onEditIconClick() {
        val intent = Intent(this@ChatBrowsingDetailActivity, EditGroupDescriptionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("groupDescription", chatBrowsingDetailViewModel.getChatName())
        startActivityForResult(intent, 1003)
    }

    override fun onPersonAddIconClick() {
        chatBrowsingDetailViewModel.group?.let {
            val intent = Intent(this, NewgroupActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("groupId", it.ID)
            startActivityForResult(intent, 1004)
        }
    }

    override fun viewGroupContactInfo(bbContact: BBContact?) {
        bbContact?.let {
            val intent = Intent(this, ChatBrowsingDetailActivity::class.java)
            intent.putExtra("registeredNumber", it.registeredNumber)
            intent.putExtra("notPerformHeaderAnimation", true)
            startActivity(intent)
        }
    }

    override fun openChatScreen(bbContact: BBContact?) {
        bbContact?.let {
            openMessageScreen(it)
        }
    }

    override fun doVideoCall(bbContact: BBContact?) {
        bbContact?.let {
            makeVideoCall(it)
        }
    }

    override fun doVoiceCall(bbContact: BBContact?) {
        bbContact?.let {
            makeVoiceCall(it)
        }
    }

    override fun updateMemberRole(bbContact: BBContact?, role: BBGroupRole) {
        bbContact?.let {
            UserProgressUtility.showProgressDialog(this)
            chatBrowsingDetailViewModel.updateMemberRole(it, role)
        }
    }

    override fun removeMember(bbContact: BBContact?) {
        bbContact?.let {
            UserAlertUtility.showAlertDialog("Remove ${it.getContactName()} from ${chatBrowsingDetailViewModel.getChatName()}?",
                    "",
                    this, { _, _ ->
                UserProgressUtility.showProgressDialog(this)
                chatBrowsingDetailViewModel.removeMember(it)
            }, { _, _ -> UserAlertUtility.hideAlertDialog() }, getString(R.string.ok_message), getString(R.string.cancel))
        }
    }

    override fun onDateGet(pDayOfMonth: Int, pYear: Int, pMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, pMonth - 1)
        calendar.set(Calendar.DATE, pDayOfMonth)
        calendar.set(Calendar.YEAR, pYear)
        chatBrowsingDetailViewModel.setGroupDeletedDate(calendar.time)
        TimePickerUtility.showTimerPicker(this, this, calendar.time)
    }

    override fun onDatePickerCancelListener() {
        //TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        temporary_group.setCustomChecked(chatBrowsingDetailViewModel.getGroupExpiryDateResponse().value?.second != null, temporaryGroupSwitchListener)
    }

    private val temporaryGroupSwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            //TimeZone.setDefault(null)
            DatePickerUtility.showDatePicker(this@ChatBrowsingDetailActivity, this@ChatBrowsingDetailActivity, pSetMinimumDate = true)
        } else {
            UserProgressUtility.showProgressDialog(this)
            chatBrowsingDetailViewModel.setGroupExpiryDate(null, temporary_group.isChecked)
        }
    }

    override fun onTimeGet(pHourOfDay: Int, pMinute: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = chatBrowsingDetailViewModel.getGroupDeletedDate()
        calendar.set(Calendar.HOUR, pHourOfDay)
        calendar.set(Calendar.MINUTE, pMinute)
        if (calendar.time.before(Calendar.getInstance().time)) {
            //TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            temporary_group.setCustomChecked(chatBrowsingDetailViewModel.getGroupExpiryDateResponse().value?.second != null, temporaryGroupSwitchListener)
            Toast.makeText(this, "Selected time should be greater than current time", Toast.LENGTH_SHORT).show()
            return
        }
        //calendar.timeZone = TimeZone.getTimeZone("UTC")
        UserProgressUtility.showProgressDialog(this)
        chatBrowsingDetailViewModel.setGroupExpiryDate(calendar.time, temporary_group.isChecked)
    }

    override fun onTimePickerCancelListener() {
        //TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        temporary_group.setCustomChecked(chatBrowsingDetailViewModel.getGroupExpiryDateResponse().value?.second != null, temporaryGroupSwitchListener)
    }
}