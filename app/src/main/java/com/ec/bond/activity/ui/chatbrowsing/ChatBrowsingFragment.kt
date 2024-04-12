package com.ec.bond.activity.ui.chatbrowsing

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import com.aghajari.emojiview.AXEmojiManager
import com.aghajari.emojiview.emoji.iosprovider.AXIOSEmojiProvider
import com.aghajari.emojiview.view.AXEmojiPopup
import com.aghajari.emojiview.view.AXEmojiView
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.alexvasilkov.gestures.views.GestureFrameLayout
import com.alexvasilkov.gestures.views.GestureImageView
import com.bumptech.glide.Glide
import com.ec.bond.R
import com.ec.bond.activity.ChatBrowsingDetailActivity
import com.ec.bond.activity.IOnBackPressed
import com.ec.bond.activity.ui.chatbrowsing.audioRecorder.AudioRecord
import com.ec.bond.activity.ui.chatbrowsing.pickimage.Options
import com.ec.bond.activity.ui.chatbrowsing.pickimage.PickImage
import com.ec.bond.activity.ui.chatbrowsing.protocols.IChatBrowsingListener
import com.ec.bond.activity.ui.chatbrowsing.reply.MessageSwipeController
import com.ec.bond.activity.ui.chatbrowsing.reply.SwipeControllerActions
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.*
import com.ec.bond.blackbox.model.Message
import com.ec.bond.di.Injectable
import com.ec.bond.extensions.debounce
import com.ec.bond.model.ChatListMesssages
import com.ec.bond.utils.*
import com.ec.bond.utils.CommonUtils.getTimeString
import com.ec.bond.utils.CommonUtils.hideKeybord
import com.ec.bond.utils.CommonUtils.showKeybord
import com.robertlevonyan.components.picker.ItemModel
import com.robertlevonyan.components.picker.PickerDialog
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.OnBalloonClickListener
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.fragment_chat_browsing.*
import kotlinx.android.synthetic.main.item_date_chat_browsing.view.*
import kotlinx.android.synthetic.main.scroll_bottom_btn.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class ChatBrowsingFragment : Fragment(), Injectable, IOnBackPressed, AudioRecord.RecordingListener,
    IChatBrowsingListener, View.OnCreateContextMenuListener, OnBalloonClickListener {

    val CHAT_DETAIL_ACTIVITY = 77
    lateinit var audioRecord: AudioRecord
    private var time: Long = 0
    val audioFile by lazy {
        CommonUtils.createAudioFile(requireContext(), "mp3")
    }

    var mediaRecorder: MediaRecorder? = null
    val mediaPlayer by lazy {
        MediaPlayer()
    }

    var messageText: String = ""
    lateinit var pickerDialog: PickerDialog
    lateinit var pwdconfg_latest: String
    lateinit var root: View
    var replyMessage = Pair<String, String>("", "")
    var isNewDayTimeSet = false
    var isCameraRequested: Boolean = false
    var contact_data: ArrayList<ChatListMesssages> = ArrayList()
    lateinit var adapter: ChatBrowsingAdapter
    val REQUEST_IMAGE_CAPTURE = 1
    private var isLongPressed = false
    private var isLongViewPressed: View? = null
    lateinit var currentActivity: AppCompatActivity
    lateinit var toolbar: Toolbar
    var ballon: Balloon? = null
    val emojiView by lazy {
        AXEmojiView(requireContext())
    }
    val emojiPopup by lazy {
        AXEmojiPopup(emojiView)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel
    lateinit var mLayoutManager: ChatBrowsingLayoutManager
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
    }

    var chatAutoDeleteTimer: ChatAutoDeleteTimer = ChatAutoDeleteTimer.Never
    var progressAutoDeleteTimer: Int = 100

    var isEnableSendNewTypingMessage = true
    var typingTime: Long = 2500

//    val fullBackground by lazy {
//        root.findViewById<View>(R.id.single_image_back)
//    }

//    val expandedImage by lazy {
//        root.findViewById<GestureImageView>(R.id.expanded_image)
//    }

    lateinit var fullBackground: View

    lateinit var expandedImage: GestureImageView

    lateinit var expandedVideo: GestureFrameLayout


//    val expandedVideo by lazy {
//        root.findViewById<GestureFrameLayout>(R.id.expanded_video)
//    }

    lateinit var animator: ViewsTransitionAnimator<ImageView>
    lateinit var mMediaPlayer: MediaPlayer


    var timerForReceiveTyping = object : CountDownTimer(typingTime, typingTime) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            chatBrowsingViewModel.chatTypeRef.typingMessage.value = ""
            if (lastSeenTV != null) {
                if (chatBrowsingViewModel.chatTypeRef is BBContact) {
                    lastSeenTV.text =
                        (chatBrowsingViewModel.chatTypeRef as BBContact).onlineStatus.value?.name
                } else if (chatBrowsingViewModel.chatTypeRef is BBGroup) {
                    lastSeenTV.text =
                        (chatBrowsingViewModel.chatTypeRef as BBGroup).getMembersName()
                }
            }
        }
    }

    var timerForSendNewGetDataRequest = object : CountDownTimer(10000, 10000) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            chatBrowsingViewModel.retry_msg_data(null, null, 80, chatBrowsingViewModel.recipient)
        }
    }


    var timerForSendTyping = object : CountDownTimer(typingTime, typingTime) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            isEnableSendNewTypingMessage = true
        }
    }

    override fun onBackPressed(): Boolean {

        // We should leave full image mode instead of closing the screen
        if (this::animator.isInitialized && !animator.isLeaving) {
            animator.exit(true)
        } else {
            return false
        }
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_chat_browsing, container, false) as View
        chatBrowsingViewModel =
            ViewModelProvider(requireActivity()).get(ChatBrowsingViewModel::class.java)
        chatBrowsingViewModel.context = requireContext()
        chatBrowsingViewModel.recipient = requireActivity().intent.getStringExtra("recipient")!!
        if (chatBrowsingViewModel.isFromForward) {
            chatBrowsingViewModel.chatTypeRef.updateUI.removeObservers(requireActivity())
            chatBrowsingViewModel.chatTypeRef.notifyItemChangedPosition.removeObservers(
                requireActivity()
            )
            chatBrowsingViewModel.isFromForward = false
        } else {
            chatBrowsingViewModel.registerChatRefType(chatBrowsingViewModel.recipient)
        }

        if (chatBrowsingViewModel.recipient.isEmpty()) {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
                .navigate(
                    ChatBrowsingFragmentDirections.actionNavigationHomeToCameraFragment(
                        "0", ImageFormat.JPEG, chatBrowsingViewModel.recipient
                    )
                )
        }
        val add: ImageView = root.findViewById(R.id.attachment_IV)

        val arr: ArrayList<ItemModel> = ArrayList<ItemModel>()
        // Some optional parameters

        arr.add(ItemModel(ItemModel.ITEM_CAMERA))
        arr.add(ItemModel(ItemModel.ITEM_GALLERY))

        pickerDialog = PickerDialog.Builder(this)// Activity or Fragment
            .setTitle("")          // String value or resource ID
            .setTitleTextSize(14f)  // Text size of title
            .setTitleTextColor(R.color.primaryTextColor) // Color of title text
            .setListType(PickerDialog.TYPE_GRID)       // Type of the picker, must be PickerDialog.TYPE_LIST or PickerDialog.TYPE_Grid
            .setItems(arr)          // List of ItemModel-s which should be in picker
            .setDialogStyle(PickerDialog.DIALOG_MATERIAL)    // PickerDialog.DIALOG_STANDARD (square corners) or PickerDialog.DIALOG_MATERIAL (rounded corners)
            .create()               // Create picker

        pickerDialog.setPickerCloseListener { type, uri ->
            when (type) {
                ItemModel.ITEM_CAMERA -> /* do something with the photo you've taken */ {
                    send_file(uri)
                }

                ItemModel.ITEM_GALLERY -> {
                    send_file(uri)
                }
            }
        }
        audioRecord = AudioRecord()
        // this is to make your layout the root of audio record view, root layout supposed to be empty..
        audioRecord.initView(root)
        // this is to provide the container layout to the audio record view..

        audioRecord.recordingListener = this
        chatBrowsingViewModel.refreshInfo()
        chatBrowsingViewModel.fetchAutoDeleteTimer()
        chatBrowsingViewModel.refreshMembersList()

        expandedImage = root.findViewById(R.id.expanded_image)
        expandedVideo = root.findViewById(R.id.expanded_video)
        fullBackground = root.findViewById<View>(R.id.single_image_back)

        val text: CharSequence? = requireActivity().intent
            .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
//        editText.text = text
//        backButton()
        return root
    }

    private fun setupObservers() {
        chatBrowsingViewModel.chatTypeRef.isLoadedLiveData.observe(viewLifecycleOwner, Observer {
            if (chatBrowsingViewModel.chatTypeRef.isLoadedLiveData.value == LoadedMessagesStatus.Loaded) {
                Log.i("getInitialMessages", "new Data getInitialMessages()")
                Log.i(
                    "getInitialMessages",
                    "size = ${chatBrowsingViewModel.chatTypeRef.messages.size}"
                )
                chatBrowsingViewModel.markAllMessagesAsRead()
                adapter.notifyDataSetChanged()
                progressBar_chatBrowsing?.let {
                    it.visibility = View.GONE
                }
            } else if (chatBrowsingViewModel.chatTypeRef.isLoadedLiveData.value == LoadedMessagesStatus.Waiting) {
                if (chatBrowsingViewModel.chatTypeRef.messages.size == 0) {
                    progressBar_chatBrowsing.visibility = View.VISIBLE
                }
            } else {
                chatBrowsingViewModel.retry_msg_data(
                    null,
                    null,
                    80,
                    chatBrowsingViewModel.recipient
                )
            }
        })

//        registerForContextMenu(editText);
//        editText.setOnCreateContextMenuListener(this)

//        onInitializeMenu(menu)

//
//        editText.setTypeface(null, Typeface.BOLD);
//        editText.setTypeface(null, Typeface.ITALIC);
//        editText.setTypeface(null, Typeface.NORMAL);
//        editText.setPaintFlags(editText.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)

        chatBrowsingViewModel.chatTypeRef.updateUI.observe(requireActivity(), Observer { message ->
            adapter.notifyDataSetChanged()
            scrollToBottom()
            updateDatePosition(message)
        })

        chatBrowsingViewModel.chatTypeRef._isReceiveNewMessages.observe(
            viewLifecycleOwner,
            Observer { message ->
                if (message.ID.isNotEmpty()) {
//                if (chatBrowsingViewModel.chatTypeRef.messagesSection2.size > 0 && message.ID != (chatBrowsingViewModel.chatTypeRef.messagesSection2[0] as? MessageItem)?.message?.ID) {
                    if (chatBrowsingViewModel.chatTypeRef.messages.size > 0) {
                        if (scrollToBottom_btn.visibility == View.GONE) {
                            scrollToBottom()
                            chatBrowsingViewModel.chatTypeRef.numberNewMessages = 0
                            chatBrowsingViewModel.markMessageRead(message)
                        } else {
                            noUnReadMsgs_txt.visibility = View.VISIBLE
                            noUnReadMsgs_txt.text =
                                chatBrowsingViewModel.chatTypeRef.numberNewMessages.toString()
                            chatBrowsingViewModel.waitingReadMessages.add(message)
                        }
                    }
                    updateDatePosition(message)
                    chatBrowsingViewModel.chatTypeRef._isReceiveNewMessages.value = Message()
                }
            })

        chatBrowsingViewModel.chatTypeRef.notifyItemChangedPosition.observe(
            requireActivity(),
            Observer { msgID ->
                notifyItemChangedPosition(msgID)
            })
        if (chatBrowsingViewModel.chatTypeRef is BBContact) {
            (chatBrowsingViewModel.chatTypeRef as BBContact).onlineStatus.debounce().observe(this) {
                lastSeenTV.text = it.name
            }

        } else if (chatBrowsingViewModel.chatTypeRef is BBGroup) {
            lastSeenTV.text = (chatBrowsingViewModel.chatTypeRef as BBGroup).getMembersName()
        }
        chatBrowsingViewModel.chatTypeRef.typingMessage.observe(this, Observer {
            if (it.isNotEmpty()) {
                lastSeenTV.text = it
                timerAppear(timerForReceiveTyping)
            }
        })

        chatBrowsingViewModel.chatTypeRef.autoDeleteTimer.observe(this, Observer {
            Log.i("autoDeleteTimerObs", it.name)
            adapter.notifyDataSetChanged()
            chatAutoDeleteTimer = it
            progressAutoDeleteTimer = getProgressFromTimer()
        })

        setupSearchObserver()
    }

    private fun getProgressFromTimer(): Int {
        return when (chatAutoDeleteTimer) {
            ChatAutoDeleteTimer.Never -> 100
            ChatAutoDeleteTimer.OneHour -> 0
            ChatAutoDeleteTimer.TwoHours -> 20
            ChatAutoDeleteTimer.OneDay -> 40
            ChatAutoDeleteTimer.TwoDays -> 60
            ChatAutoDeleteTimer.OneWeek -> 80
        }
    }

    fun send_file(uri: Uri) {
        val path: String = CommonUtils.getPathFromUri(requireContext()!!, uri)!!
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            val paths = data?.getStringArrayListExtra(PickImage.IMAGE_RESULTS)?.toTypedArray()!!
            viewLifecycleOwner.lifecycleScope.launch {
                if (paths.size == 1) {
                    if (context?.filesDir?.absolutePath?.let { paths[0].contains(it) }!!) {
                        moveToImageViewer(CommonUtils.pickPhoto(requireContext(), paths))
                        val file = File(paths[0])
                        file.delete()
                    }
                } else {
                    moveToImageViewer(CommonUtils.pickPhoto(requireContext(), paths))
                }
            }
        } else if (requestCode == 500) {
            print("hello")
        }
    }

    private fun moveToImageViewer(paths: ArrayList<String>) {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
            .navigate(
                ChatBrowsingFragmentDirections.actionNavigationHomeToImageViewerFragment(
                    paths.toTypedArray(), 0, false, chatBrowsingViewModel.recipient
                )
            )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()

        if (isLongPressed) {
            /*inflater.inflate(R.menu.menu_chat_browsing_selected_item, menu)
            val item = menu.findItem(R.id.action_msg_info)
            Log.e("menus---","done+"+isLongViewPressed)*/

        } else {
            inflater.inflate(R.menu.menu_chat_browsing, menu)
            changeItemColor(menu, R.id.action_voiceCall)
            changeItemColor(menu, R.id.action_videoCall)

        }
        chatBrowsingViewModel.menuActionBar.postValue(menu)
        chatBrowsingViewModel.menuInflater.postValue(inflater)
    }


    fun openDialog(view: View) {
        ballon?.let {
            if (it?.isShowing) {
                it?.dismiss()
            }
        }
        ballon = BallonUtils.getNavigationBalloonFloating(requireContext(), this, this)
        val action_replyMsgImg: ImageView =
            ballon?.getContentView()?.findViewById(R.id.action_replyMsgImg)!!
        val action_starMsgImg: ImageView =
            ballon?.getContentView()?.findViewById(R.id.action_starMsgImg)!!
        val action_deleteMsgImg: ImageView =
            ballon?.getContentView()?.findViewById(R.id.action_deleteMsgImg)!!
        val action_forwardMsgImg: ImageView =
            ballon?.getContentView()?.findViewById(R.id.action_forwardMsgImg)!!
        val action_msg_infoImg: ImageView =
            ballon?.getContentView()?.findViewById(R.id.action_msg_infoImg)!!
        val action_msg_copyImg: ImageView =
            ballon?.getContentView()?.findViewById(R.id.action_msg_copyImg)!!
        action_replyMsgImg.setOnClickListener {
            var message =
                chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>().find {
                    it.message.ID == chatBrowsingViewModel.getSelectedMessages()[0].message.ID
                }?.message
            replyUIOnSpecificMessage(message!!)
            ballon?.dismiss()
        }
        action_starMsgImg.setOnClickListener {
            chatBrowsingViewModel.getSelectedMessages().forEachIndexed { index, messageItem ->
                if (chatBrowsingViewModel.isStarredMessagesAction) {
                    if (messageItem.message.isStarred.value == false)
                        chatBrowsingViewModel.setStarredMsg(messageItem.message)
                } else {
                    chatBrowsingViewModel.setUnStarredMsg(messageItem.message)
                }
                messageItem.message._isStarred.postValue(chatBrowsingViewModel.isStarredMessagesAction)
                chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>()
                    .first { it.message.ID == messageItem.message.ID }.message._isStarred.value =
                    chatBrowsingViewModel.isStarredMessagesAction

            }
            handleCancelSelection()
            ballon?.dismiss()
        }
        action_deleteMsgImg.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Delete message for every one?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    chatBrowsingViewModel.getSelectedMessages()
                        .forEachIndexed { index, messageItem ->
                            chatBrowsingViewModel.deleteMsg(messageItem.message)
                            if (messageItem.message.localFileName.value?.isNotEmpty() == true) {
                                var file = File(messageItem.message.localFileName.value)
//                                Log.i("fileDeleted","isExist = ${File(messageItem.message.localFileName.value).exists()}")
                                file.delete()
//                                Log.i("fileDeleted","isExist = ${File(messageItem.message.localFileName.value).exists()}")
                            }
                            var messageSelected =
                                chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>()
                                    .first { it.message.ID == messageItem.message.ID }.message
                            messageSelected.type = MessageType.Deleted
                        }
                    handleCancelSelection()
                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
            ballon?.dismiss()
        }
        action_forwardMsgImg.setOnClickListener {
            ballon?.dismiss()
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
                .navigate(
                    ChatBrowsingFragmentDirections.actionNavigationHomeToForwardMessageFragment(
                        chatBrowsingViewModel.getSelectedMessages().toTypedArray()
                    )
                )
        }
        action_msg_infoImg.setOnClickListener {
            ballon?.dismiss()
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
                .navigate(
                    ChatBrowsingFragmentDirections.actionNavigationHomeToMessageInfoFragment(
                        chatBrowsingViewModel.getSelectedMessages()[0].message
                    )
                )
        }
        action_msg_copyImg.setOnClickListener {
            ballon?.dismiss()
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "copy",
                chatBrowsingViewModel.getSelectedMessages()[0].message.body
            )
            clipboard.setPrimaryClip(clip)
            val message = chatBrowsingViewModel.getSelectedMessages()[0].message
            if (!message.isOutgoing) {
                chatBrowsingViewModel.sendChatAlertAsync(AlertType.MessageCopied, message)
            }
            val toast =
                Toast.makeText(context, getText(R.string.message_copied), Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            handleCancelSelection()
        }



        ballon?.show(view)
    }

    private fun changeItemColor(menu: Menu, action: Int) {
        var drawable: Drawable? = menu.findItem(action).icon

        drawable = DrawableCompat.wrap(drawable!!)
        DrawableCompat.setTint(drawable, ContextCompat.getColor(requireContext(), R.color.white))
        menu.findItem(action).setIcon(drawable)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_forwardMsg) {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
                .navigate(
                    ChatBrowsingFragmentDirections.actionNavigationHomeToForwardMessageFragment(
                        chatBrowsingViewModel.getSelectedMessages().toTypedArray()
                    )
                )
        } else if (id == R.id.action_deleteMsg) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Delete message for every one?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    chatBrowsingViewModel.getSelectedMessages()
                        .forEachIndexed { index, messageItem ->
                            chatBrowsingViewModel.deleteMsg(messageItem.message)
                            if (messageItem.message.localFileName.value?.isNotEmpty() == true) {
                                var file = File(messageItem.message.localFileName.value)
//                                Log.i("fileDeleted","isExist = ${File(messageItem.message.localFileName.value).exists()}")
                                file.delete()
//                                Log.i("fileDeleted","isExist = ${File(messageItem.message.localFileName.value).exists()}")
                            }
                            var messageSelected =
                                chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>()
                                    .first { it.message.ID == messageItem.message.ID }.message
                            messageSelected.type = MessageType.Deleted
                        }
                    handleCancelSelection()
                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        } else if (id == R.id.action_starMsg) {

            chatBrowsingViewModel.getSelectedMessages().forEachIndexed { index, messageItem ->
                if (chatBrowsingViewModel.isStarredMessagesAction) {
                    if (messageItem.message.isStarred.value == false)
                        chatBrowsingViewModel.setStarredMsg(messageItem.message)
                } else {
                    chatBrowsingViewModel.setUnStarredMsg(messageItem.message)
                }
                messageItem.message._isStarred.postValue(chatBrowsingViewModel.isStarredMessagesAction)
                chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>()
                    .first { it.message.ID == messageItem.message.ID }.message._isStarred.value =
                    chatBrowsingViewModel.isStarredMessagesAction

            }
            handleCancelSelection()

        } else if (id == R.id.action_msg_info) {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
                .navigate(
                    ChatBrowsingFragmentDirections.actionNavigationHomeToMessageInfoFragment(
                        chatBrowsingViewModel.getSelectedMessages()[0].message
                    )
                )
        } else if (id == R.id.action_replyMsg) {
            var message =
                chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>().find {
                    it.message.ID == chatBrowsingViewModel.getSelectedMessages()[0].message.ID
                }?.message
            replyUIOnSpecificMessage(message!!)
        } else if (id == R.id.action_voiceCall) {
            if (chatBrowsingViewModel.chatTypeRef is BBContact) {
                val call = BBCall(isOutgoing = true)
                call.addContact(chatBrowsingViewModel.chatTypeRef as BBContact)
                Blackbox.openCallActivity(call, requireContext())
            }
        } else if (id == R.id.action_videoCall) {
            if (chatBrowsingViewModel.chatTypeRef is BBContact) {
                val call = BBCall(isOutgoing = true, hasVideo = true)
                call.addContact(chatBrowsingViewModel.chatTypeRef as BBContact)
                Blackbox.openCallActivity(call, requireContext())
            }
        } else if (id == R.id.action_autoDelete) {
            showAlertDialog(R.layout.dialog_auto_delete)
        } else if (id == R.id.action_clearChat) {
            clearChatDialog()

        } else if (id == R.id.action_search) {
            chatBrowsingViewModel.isSearchStart.value = true
        } else if (id == R.id.action_msg_copy) {
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "copy",
                chatBrowsingViewModel.getSelectedMessages()[0].message.body
            )
            clipboard.setPrimaryClip(clip)
            val message = chatBrowsingViewModel.getSelectedMessages()[0].message
            if (!message.isOutgoing) {
                chatBrowsingViewModel.sendChatAlertAsync(AlertType.MessageCopied, message)
            }
            val toast =
                Toast.makeText(context, getText(R.string.message_copied), Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            handleCancelSelection()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun clearChatDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(R.string.clear_caht_message)
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                progressBar_chatBrowsing.visibility = View.VISIBLE
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (chatBrowsingViewModel.clearChat()) {
                        chatBrowsingViewModel.chatTypeRef.messages.clear()
                        dateSection_include.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to clear chat please try again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    progressBar_chatBrowsing.let {
                        it.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton("No") { dialog, id ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }


    fun scaleView(v: View, startScale: Float, endScale: Float) {
        val anim: Animation = ScaleAnimation(
            1f, 1f,  // Start and end values for the X axis scaling
            startScale, endScale,  // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, 0f,  // Pivot point of X scaling
            Animation.RELATIVE_TO_SELF, 1f
        ) // Pivot point of Y scaling
        anim.fillAfter = true // Needed to keep the result of the animation
        anim.duration = 1000
        v.startAnimation(anim)
    }

    fun handleCancelSelection() {
        chatBrowsingViewModel.getSelectedMessages().forEach {
            notifyItemChangedPosition(it.message.ID)
        }
        chatBrowsingViewModel.clearSelectedMessages()
        chatBrowsingViewModel.setIsLongPressed(false)
    }

    private fun replyUIOnSpecificMessage(msg: Message) {

        if (!sendMessage_replyPart_ConstraintLayout.isVisible) {

            sendMessage_replyPart_ConstraintLayout.visibility = View.VISIBLE
        }
        sendMessage_ConstraintLayout.setBackgroundResource(R.drawable.round_view_reply_text)
        if (msg.sender == Blackbox.account.registeredNumber) {
            replyOwnerMsgName_Txt.text = getString(R.string.you)
            replyOwnerMsgName_Txt.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.whatsapp_color
                )
            )
            lineReplyDecoration.setBackgroundResource(R.drawable.rounded_reply_view_decoration_outgoing)
        } else {
            replyOwnerMsgName_Txt.text = Blackbox.getContact(msg.sender)?.name
            replyOwnerMsgName_Txt.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.purple
                )
            )
            lineReplyDecoration.setBackgroundResource(R.drawable.rounded_reply_view_decoration_incoming)
        }
        replyMessage = replyMessage.copy(first = msg.ID, second = msg.body)
//        replyOwnerMsg_Txt.text = (chatBrowsingViewModel.chatTypeRef.messagesSection2[position] as MessageItem).message.body
        replyOwnerMsg_Txt.text = msg.body
        if (msg.type == MessageType.Photo) {
            chatBrowsingViewModel.replyForPhotos(
                requireContext(),
                msg,
                mediaReply_imageView,
                mediaTypeReply_imageView,
                replyOwnerMsg_Txt
            )
        } else if (msg.type == MessageType.Video) {
            chatBrowsingViewModel.replyForVideos(
                requireContext(),
                msg,
                mediaReply_imageView,
                mediaTypeReply_imageView,
                replyOwnerMsg_Txt
            )
        } else if (msg.type.isDocumentMessage == true) {
            mediaReply_imageView.visibility = View.GONE
            chatBrowsingViewModel.replyForDocuments(
                requireContext(),
                msg.body,
                mediaTypeReply_imageView,
                replyOwnerMsg_Txt
            )
        } else if (msg.type == MessageType.Audio) {
            mediaReply_imageView.visibility = View.GONE
            chatBrowsingViewModel.replyForAudio(
                requireContext(),
                msg.localFileName.value!!,
                mediaTypeReply_imageView,
                replyOwnerMsg_Txt
            )
        } else {
            mediaReply_imageView.visibility = View.GONE
            mediaTypeReply_imageView.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLayoutManager = ChatBrowsingLayoutManager(requireContext()!!, chatBrowsingViewModel)

        if (chatBrowsingViewModel.chatTypeRef is BBContact) {
            val contact = chatBrowsingViewModel.chatTypeRef as BBContact
            NotificationUtility.clearNotification(
                context,
                contact.registeredNumber.toBigInteger().toInt()
            )
        } else if (chatBrowsingViewModel.chatTypeRef is BBGroup) {
            val group = chatBrowsingViewModel.chatTypeRef as BBGroup
            NotificationUtility.clearNotification(context, group.ID.toBigInteger().toInt())
        }
        clearNotification()
        AXEmojiManager.install(requireContext(), AXIOSEmojiProvider(requireContext()))
        AXEmojiManager.enableTouchEmojiVariantPopup()
        AXEmojiManager.getEmojiViewTheme().isFooterEnabled = true
        AXEmojiManager.setBackspaceCategoryEnabled(true)
        emojiView.setEditText(editText)

        toolbar = root.findViewById(R.id.tb_toolbarsearch)
        currentActivity = activity as AppCompatActivity
        currentActivity.setSupportActionBar(toolbar)

        val actionBar: ActionBar? = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        val nameTV: TextView? = root.findViewById(R.id.nameTV)
        if (chatBrowsingViewModel.chatTypeRef.chatImagePath.value.isNullOrEmpty() == true) {
            Glide.with(requireContext()).load(R.drawable.contact)
                .apply(chatBrowsingViewModel.requestOptions).dontTransform().into(userIV)

        } else {

            Glide.with(requireContext()).load(chatBrowsingViewModel.chatTypeRef.chatImagePath.value)
                .apply(chatBrowsingViewModel.requestOptions).dontTransform().into(userIV)

        }

        if (chatBrowsingViewModel.chatTypeRef is BBContact) {
            if ((chatBrowsingViewModel.chatTypeRef as BBContact).name.isNotEmpty()) {
                nameTV?.text = (chatBrowsingViewModel.chatTypeRef as BBContact).name
            } else {
                nameTV?.text = (chatBrowsingViewModel.chatTypeRef as BBContact).registeredNumber
            }
        } else {
            nameTV?.text = (chatBrowsingViewModel.chatTypeRef as BBGroup).desc
        }

        setHasOptionsMenu(true)

        mLayoutManager.setAutoMeasureEnabled(true)
        mLayoutManager.reverseLayout = true

        chatsBrowsingRecyclerView.setLayoutManager(mLayoutManager)
        chatsBrowsingRecyclerView.setItemAnimator(DefaultItemAnimator())

        mLayoutManager.onAttachedToWindow(chatsBrowsingRecyclerView)

        val messageSwipeController = MessageSwipeController(
            requireContext(),
            chatBrowsingViewModel,
            object : SwipeControllerActions {
                override fun showReplyUI(position: Int) {
                    var msg =
                        (chatBrowsingViewModel.chatTypeRef.messages[position] as MessageItem).message
                    replyUIOnSpecificMessage(msg)
                }
            })
        cancelReply_CV.setOnClickListener {
            cancelReplyOnMessage()
        }
        cancelReply_Btn.setOnClickListener {
            cancelReplyOnMessage()
        }
        val itemTouchHelper = ItemTouchHelper(messageSwipeController)
        itemTouchHelper.attachToRecyclerView(chatsBrowsingRecyclerView)
        editText.setOnClickListener {
            if (emojiPopup.isShowing) {
                emojiPopup.dismiss()
            }

        }

        editText.addTextChangedListener {
//            if ()
            messageText = editText.text?.trim().toString()
//            if (it?.contains("<color hex=\"")!!) {
//                val stringHasColor = StringHasColor(text = SpannableString(messageText))
//                removeColorTags(stringHasColor)
//                editText.setText(stringHasColor.text)
//            }
//            var arr = MessageBody.convertStringToMessageBody(editText.text.toString())
            if (isEnableSendNewTypingMessage) {
                timerAppear(timerForSendTyping)
                isEnableSendNewTypingMessage = false
                Log.i("sendTyping", "YES")
                chatBrowsingViewModel.sendTyping()
            }
        }
//        imageViewSend.setImageDrawable(resources.getDrawable(R.drawable.input_mic_white, requireContext().theme));
        sendMessage_IV.setOnClickListener {
            if (messageText.isNotEmpty()) {
                chatBrowsingViewModel.sendMessage(
                    chatBrowsingViewModel.chatTypeRef,
                    messageText,
                    replyMessage
                )
                editText.text?.clear()
                messageText = ""
                cancelReplyOnMessage()
            }
        }

        Blackbox.pwdConf?.let {
            pwdconfg_latest = it
        }

        attachment_IV.setOnClickListener {
            isCameraRequested = false
            if (isPermissionGranted()) {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
                    .navigate(
                        ChatBrowsingFragmentDirections.actionNavigationHomeToPickerFragment(
                            chatBrowsingViewModel.recipient
                        )
                    )
            }

        }

        camera_IV.setOnClickListener {
            isCameraRequested = true
            if (isPermissionGranted()) {
                showCamera()
            }
        }
        setupAdapter()
//        getInitialMessages()
    }

    private fun removeColorTags(messageText: StringHasColor) {
        val char = "<color hex=\""
        var tempString: String = messageText.text.toString()
        val firstPosition = tempString.indexOf(char)

        tempString = tempString.removeRange(firstPosition, firstPosition + char.length)
        val endPositionOfFirstPart = tempString.indexOf("\">")
        val colorInString = tempString.substring(firstPosition, endPositionOfFirstPart)
        val color = Color.parseColor(colorInString)
        tempString = tempString.removeRange(firstPosition, firstPosition + colorInString.length + 2)

        val lastPosition = tempString.indexOf("</color>")
        if (lastPosition == -1) {
            return
        }
        tempString = tempString.removeRange(lastPosition, lastPosition + "</color>".length)
        messageText.text = SpannableString(tempString)
        messageText.text.setSpan(ForegroundColorSpan(color), firstPosition, lastPosition, 0)

    }

    private fun clearNotification() {
        val recipient = if (chatBrowsingViewModel.chatTypeRef is BBContact) {
            (chatBrowsingViewModel.chatTypeRef as BBContact).registeredNumber
        } else if (chatBrowsingViewModel.chatTypeRef is BBGroup) {
            (chatBrowsingViewModel.chatTypeRef as BBGroup).ID
        } else {
            ""
        }
        if (!recipient.isNullOrEmpty()) {
            val notificationId = recipient.toBigInteger().toInt()
            context?.let { NotificationUtility.clearDataFromPreferences(it, notificationId) }
        }
    }


    private fun updateDatePosition(message: Message) {
        Log.i(
            "newMessage",
            "addNewMessage()  messageId = ${message.ID} and contactId = ${(chatBrowsingViewModel.chatTypeRef as? BBContact)?.registeredNumber}"
        )

        val currentDate = message.dateSent
        val lastMessage =
            (chatBrowsingViewModel.chatTypeRef.messages.firstOrNull() as? MessageItem)?.message

        if (lastMessage?.dateSent?.getTimeString() != currentDate?.getTimeString()) {
            currentDate?.let {
                DateItem(it).let {
                    chatBrowsingViewModel.chatTypeRef.datePositionList.forEach {
                        it.position += 1
                    }
                    chatBrowsingViewModel.chatTypeRef.datePositionList.add(
                        DatePosition(
                            it.date.getTimeString(),
                            chatBrowsingViewModel.chatTypeRef.messages.lastIndex - 1,
                            message.dateSent!!,
                            1
                        )
                    )
                }
            }
        } else {
            chatBrowsingViewModel.chatTypeRef.datePositionList.forEach {
                it.position += 1
            }
            chatBrowsingViewModel.chatTypeRef.datePositionList.firstOrNull().let {
                it?.msgsNumber = it?.msgsNumber?.plus(1)!!
            }
        }
    }

    private fun setupAdapter() {

        adapter = ChatBrowsingAdapter(requireContext(), chatBrowsingViewModel, this, this)
        chatsBrowsingRecyclerView.apply {
            adapter = this@ChatBrowsingFragment.adapter
            postponeEnterTransition()
            viewTreeObserver
                .addOnPreDrawListener {
                    startPostponedEnterTransition()
                    true
                }
        }
        chatsBrowsingRecyclerView.adapter = adapter
    }

//    fun callGetInitialMessages() {
//        Timer().schedule(500) {
//            getInitialMessages()
//        }
//    }


//    private fun getInitialMessages() = MainScope().launch(Dispatchers.Main) {
//        Log.i("getInitialMessages","called getInitialMessages()")
//        if ((chatBrowsingViewModel.retreiveInitialMessages()) == false) {
//            Log.i("getInitialMessages","chatBrowsingViewModel.retreiveInitialMessages()) == false")
//
//            if (progressBar_chatBrowsing == null) {
//                return@launch
//            }
//
//            progressBar_chatBrowsing.visibility = View.VISIBLE
//            chatBrowsingViewModel.chatTypeRef.isLoadedLiveData.observe(viewLifecycleOwner, Observer {
//                chatBrowsingViewModel.chatTypeRef.isLoadedLiveData.removeObservers(viewLifecycleOwner)
//                callGetInitialMessages()
//            })
////            if (!chatBrowsingViewModel.chatTypeRef.isGetData || chatBrowsingViewModel.chatTypeRef.isLoadedLiveData.value == LoadedMessagesStatus.Failed || chatBrowsingViewModel.chatTypeRef.isLoadedLiveData.value == LoadedMessagesStatus.NotStarted) {
////                chatBrowsingViewModel.retry_msg_data(null, null, 80, chatBrowsingViewModel.recipient)
////            } else if (chatBrowsingViewModel.chatTypeRef.isLoadedLiveData.value == LoadedMessagesStatus.Waiting) {
////                timerAppear(timerForSendNewGetDataRequest)
////            }
////            if (!chatBrowsingViewModel.chatTypeRef.getDataJob?.isCancelled!!) {
////                chatBrowsingViewModel.chatTypeRef.getDataJob?.cancel()
////            }
////            chatBrowsingViewModel.chatTypeRef.getDataJob?.cancelChildren()
//
//
//        } else {
//            Log.i("getInitialMessages","new Data getInitialMessages()")
//            Log.i("getInitialMessages","size = ${chatBrowsingViewModel.chatTypeRef.messages.size}")
//            chatBrowsingViewModel.markAllMessagesAsRead()
//            adapter.notifyDataSetChanged()
//            progressBar_chatBrowsing?.let {
//                it.visibility = View.GONE
//            }
//
//        }
//    }

    private fun showCamera() {
        val options = Options.init()
            .setRequestCode(100) //Request code for activity results
            .setCount(15) //Number of images to restict selection count
            .setFrontfacing(false) //Front Facing camera on start
            .setExcludeVideos(false) //Option to exclude videos
            .setVideoDurationLimitinSeconds(30) //Duration for video recording
            .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT) //Orientaion
            .setPath(context?.filesDir?.absolutePath) //Custom Path For media Storage
        PickImage.start(this, options)
    }


    fun isPermissionGranted(): Boolean {
        val perrmissions = arrayListOf<String>(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perrmissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            perrmissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            perrmissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            perrmissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            perrmissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val requestPermissions = perrmissions.toTypedArray()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasPermissions(requireActivity(), *requestPermissions)) {
                true
            } else {
                ActivityCompat.requestPermissions(requireActivity(), requestPermissions, 1001)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("permission", "Permission is granted")
            true
        }
    }

    fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        pickerDialog.onPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("permission", "Permission: " + permissions[0] + "was " + grantResults[0])
            if (isCameraRequested)
                showCamera()
            else
                pickerDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_APP_IN_BACKGROUND, false)
        setupObservers()

        chatBrowsingViewModel._isSendForward.observe(requireActivity(), Observer {
            if (it) {
                chatBrowsingViewModel._isSendForward.value = false
                handleCancelSelection()
            }
        })
        chatBrowsingViewModel.ismessagesLoaded.observe(requireActivity(), Observer {
            if (chatsBrowsingRecyclerView != null) {
                adapter.notifyDataSetChanged()
            }

        })

        chatBrowsingViewModel.isLongPressed.observe(viewLifecycleOwner, Observer {
            isLongPressed = it



            if (chatBrowsingViewModel.isSearchStart.value == true) {
                chatBrowsingViewModel.isSearchStart.value = false
                handleCancelSearch()
            }
            if (it) {
                chatBrowsingViewModel.menuActionBar.value?.clear()
                chatBrowsingInfo.visibility = View.GONE
                toolbar.setBackgroundResource(R.color.toolbar_selected)
                //              chatBrowsingViewModel.menuInflater.value?.inflate(R.menu.menu_chat_browsing_selected_item, chatBrowsingViewModel.menuActionBar.value)
//                currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                if (chatBrowsingViewModel.getSelectedMessages()
                        .firstOrNull()?.message?.isStarred?.value == false
                ) {
                    chatBrowsingViewModel.menuActionBar.value?.findItem(R.id.action_starMsg)
                        ?.setIcon(R.drawable.star_icon);
                    chatBrowsingViewModel.isStarredMessagesAction = true
                } else {
                    chatBrowsingViewModel.menuActionBar.value?.findItem(R.id.action_starMsg)
                        ?.setIcon(R.drawable.unstar_white);
                    chatBrowsingViewModel.isStarredMessagesAction = false
                }
                if (chatBrowsingViewModel.getSelectedMessages()
                        .filter { it.message.sender != Blackbox.account.registeredNumber }.any()
                ) {
                    chatBrowsingViewModel.menuActionBar.value?.findItem(R.id.action_deleteMsg)?.isVisible =
                        false
                }

                toolbar.setNavigationOnClickListener {
                    handleCancelSelection()
                }
            } else {
                toolbarDefaultMode()
            }

        })

        chatBrowsingViewModel.longPressedTitle.observe(viewLifecycleOwner, Observer {
            currentActivity.supportActionBar?.title = it
        })

        chatBrowsingViewModel.scrollToBottomBtnAppear.observe(viewLifecycleOwner, Observer {
            if (it) {
                scrollToBottom_btn.visibility = View.VISIBLE
            } else {
                scrollToBottom_btn.visibility = View.GONE
                noUnReadMsgs_txt.visibility = View.GONE
                chatBrowsingViewModel.chatTypeRef.numberNewMessages = 0
            }
        })
        chatBrowsingViewModel.dateText.observe(viewLifecycleOwner, Observer {
            dateSection_include.date_section_txt.text = it
            dateSection_include.visibility = View.VISIBLE
        })
        chatBrowsingViewModel._isDateAppear.observe(viewLifecycleOwner, Observer {
            if (it == true && dateSection_include.visibility == View.VISIBLE || it == false && dateSection_include.visibility == View.GONE) {
                return@Observer
            }
            if (it) {
                dateSection_include.visibility = View.VISIBLE
            } else {
                dateSection_include.visibility = View.GONE
            }

        })
        scrollToBottom_btn.setOnClickListener {
            if (chatBrowsingViewModel.waitingReadMessages.size > 0) {
                chatBrowsingViewModel.waitingReadMessages.forEach {
                    chatBrowsingViewModel.markMessageRead(it)
                }
            }
            mLayoutManager.scrollToPosition(0)
        }

        call_Btn.setOnClickListener {
            if (chatBrowsingViewModel.chatTypeRef is BBContact) {
                val call = BBCall(isOutgoing = true)
                call.addContact(chatBrowsingViewModel.chatTypeRef as BBContact)
                Blackbox.openCallActivity(call, requireContext())
            }
        }

        video_call_Btn.setOnClickListener {
            if (chatBrowsingViewModel.chatTypeRef is BBContact) {
                val call = BBCall(isOutgoing = true, hasVideo = true)
                call.addContact(chatBrowsingViewModel.chatTypeRef as BBContact)
                Blackbox.openCallActivity(call, requireContext())
            }
        }
        chatHeaderView.setOnClickListener {
            val intent = Intent(context, ChatBrowsingDetailActivity::class.java)
            if (chatBrowsingViewModel.chatTypeRef is BBContact) {
                val contact = chatBrowsingViewModel.chatTypeRef as BBContact
                intent.putExtra("registeredNumber", contact.registeredNumber)
            } else if (chatBrowsingViewModel.chatTypeRef is BBGroup) {
                val group = chatBrowsingViewModel.chatTypeRef as BBGroup
                intent.putExtra("groupId", group.ID)
            }
            startActivityForResult(intent, CHAT_DETAIL_ACTIVITY)
        }
        emoji_IV.setOnClickListener {
            emojiPopup.toggle();
        }

        search_TV.addTextChangedListener {
            adapter.searchWithText(
                it.toString(),
                mLayoutManager.findFirstVisibleItemPosition(),
                mLayoutManager.findLastVisibleItemPosition()
            )
        }
        searchDown_IV.setOnClickListener {
            adapter.searchDownArrow(
                search_TV,
                mLayoutManager.findLastVisibleItemPosition(),
                chatsBrowsingRecyclerView
            )
        }
        searchUp_IV.setOnClickListener {
            adapter.searchUpArrow(
                search_TV,
                mLayoutManager.findFirstVisibleItemPosition(),
                mLayoutManager.findLastVisibleItemPosition(),
                chatsBrowsingRecyclerView
            )
        }
    }

    private fun notifyItemChangedPosition(msgID: String?) {
        val position =
            chatBrowsingViewModel.chatTypeRef.messages.indexOfFirst { (it as? MessageItem)?.message?.ID == msgID }
//        adapter.inNotifyChanged = true
        adapter.notifyItemChanged(position, Unit)
    }

    fun scrollToBottom() {
        if (chatsBrowsingRecyclerView != null) chatsBrowsingRecyclerView.smoothScrollToPosition(0)
    }

    fun scrollToBottomForced() {
        chatsBrowsingRecyclerView.scrollToPosition(0)
    }

    fun Date.getDifferenceBetweenDates(): Int {

        val cal = Calendar.getInstance()
        val currentLocalTime = cal.time
        currentLocalTime.hours = 0
        currentLocalTime.minutes = 0
        currentLocalTime.seconds = 0
        this.hours = 0
        this.minutes = 0
        this.seconds = 0
        return TimeUnit.DAYS.convert(this.time - currentLocalTime.time, TimeUnit.MILLISECONDS)
            .toInt()
    }

    override fun onRecordingCanceled() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRecordingStarted() {
        time = System.currentTimeMillis() / 1000

        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )
//            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setAudioChannels(1);
            mediaRecorder?.setAudioSamplingRate(44100);
//            mediaRecorder?.setAudioEncodingBitRate(96000);
            mediaRecorder?.setAudioEncodingBitRate(16 * 44100);
            mediaRecorder?.setOutputFile(audioFile)

            mediaRecorder?.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder?.start()
    }

    override fun onRecordingLocked() {
//        TODO("Not yet implemented")
    }

    override fun onRecordingCompleted() {
        val recordTime = (System.currentTimeMillis() / 1000 - time).toInt()

        if (recordTime > 1) {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            var uriArr = Array<String>(1) { audioFile.path }
            chatBrowsingViewModel.send_file(uriArr, "")

        }
    }

    fun timerAppear(timer: CountDownTimer) {
        Handler(Looper.getMainLooper()).post {
            if (timer === timerForReceiveTyping) {
                typingTime += typingTime
            }

            timer.cancel()
            timer.start()
        }
    }

    override fun showAlertDialog(layout: Int) {
        var timer = chatAutoDeleteTimer
        var progress = progressAutoDeleteTimer
        var dialogBuilder = AlertDialog.Builder(requireContext(), R.style.blurTheme)
        var layoutView = layoutInflater.inflate(layout, null);
        var dialogButton = layoutView.findViewById<Button>(R.id.cancelAutoDelete_btn)
        dialogBuilder.setView(layoutView)
        var alertDialog = dialogBuilder.create()
        alertDialog.getWindow()!!.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
//        alertDialog.getWindow()!!.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
        val autoDelete_seekbar: IndicatorSeekBar = layoutView.findViewById(R.id.autoDelete_seekbar)
        val autoDelete_relativeLayout: RelativeLayout =
            layoutView.findViewById(R.id.autoDelete_relativeLayout)
        val autoDelete_CV: CardView = layoutView.findViewById(R.id.autoDelete_CV)

        autoDelete_seekbar.setProgress(progressAutoDeleteTimer.toFloat())

        autoDelete_relativeLayout.setOnClickListener {
            if (chatAutoDeleteTimer != timer) {
                chatAutoDeleteTimer = timer
                progressAutoDeleteTimer = progress
                chatBrowsingViewModel.setAutoDeleteMessages(chatAutoDeleteTimer)
            }
            alertDialog.dismiss()
        }
        autoDelete_CV.setOnClickListener {

        }
        autoDelete_seekbar.onSeekChangeListener = object : OnSeekChangeListener {

            override fun onSeeking(seekParams: SeekParams?) {}

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
                Log.i("autoDelete_seekbar", "progress = ${seekBar?.progress}")

                timer = when (seekBar?.progress) {
                    0 -> ChatAutoDeleteTimer.OneHour
                    20 -> ChatAutoDeleteTimer.TwoHours
                    40 -> ChatAutoDeleteTimer.OneDay
                    60 -> ChatAutoDeleteTimer.TwoDays
                    80 -> ChatAutoDeleteTimer.OneWeek
                    else -> ChatAutoDeleteTimer.Never
                }
                progress = seekBar?.progress!!

            }


        }
        alertDialog.getWindow()?.getAttributes()?.windowAnimations = R.style.DialogAnimation

        alertDialog.show()
        dialogButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                alertDialog.dismiss()
            }
        })
    }

    override fun showBallonDialog(layout: Int, view: View) {
        openDialog(view)
    }

    fun onContactGroupSelect(name: String, number: String) {
        val contact = Blackbox.getContact(number) ?: Blackbox.getTemporaryContact(number)

        val options: Array<CharSequence> =
            arrayOf("Message $name", "Voice call $name", "Voice call $name")
        val builder = AlertDialog.Builder(requireContext())

        builder.setItems(options) { dialog, item ->
            if (options[item] == "Message $name") {
                if (contact != null) {
                    chatBrowsingViewModel.isFromForward = true
                    chatBrowsingViewModel.chatTypeRef = contact
                    val action =
                        ChatBrowsingFragmentDirections.actionNavigationHomeToSwitchUserFragment()
                    NavHostFragment.findNavController(this).navigate(action)
                }
            } else if (options[item] == "Voice call $name") {
                val call = BBCall(isOutgoing = true)
                call.addContact(contact!!)
                Blackbox.openCallActivity(call, requireContext())
            } else if (options[item] == "Video call $name") {
                val call = BBCall(isOutgoing = true, hasVideo = true)
                call.addContact(contact!!)
                Blackbox.openCallActivity(call, requireContext())
            }
        }
        builder.show()
    }

    private fun handleCancelSearch() {
        adapter.cancelSearch()
        search_TV.text.clear()
        activity?.let { hideKeybord(it) }
        chatBrowsingSearch.visibility = View.GONE
        recordAudio_IV.visibility = View.VISIBLE
        sendMessage_ConstraintLayout.visibility = View.VISIBLE
        chatBrowsingViewModel.isSearchStart.postValue(false)
    }

    private fun setupSearchObserver() {
        chatBrowsingViewModel.isSearchStart.observe(this, Observer {
            if (!isLongPressed) {
                if (it) {
                    chatBrowsingViewModel.menuActionBar.value?.clear()
                    chatBrowsingInfo.visibility = View.GONE
                    toolbar.setBackgroundResource(R.color.toolbar_selected)
                    chatBrowsingSearch.visibility = View.VISIBLE
                    search_TV.isFocusable = true
                    search_TV.requestFocus()
                    context?.let { it1 -> showKeybord(it1) }
                    recordAudio_IV.visibility = View.GONE
                    sendMessage_ConstraintLayout.visibility = View.GONE
                    ballon?.let {
                        it?.dismiss()
                    }
                    toolbar.setNavigationOnClickListener {
                        handleCancelSearch()
                    }
                } else {
                    toolbarDefaultMode()
                }
            }
        })
    }

    private fun toolbarDefaultMode() {
        chatBrowsingViewModel.menuActionBar.value?.clear()
        chatBrowsingViewModel.menuInflater.value?.inflate(
            R.menu.menu_chat_browsing,
            chatBrowsingViewModel.menuActionBar.value
        )
        chatBrowsingInfo.visibility = View.VISIBLE
        toolbar.setBackgroundResource(R.color.toolbar_color)
        toolbar.setNavigationOnClickListener {
            currentActivity.onBackPressed()
        }
    }

    override fun onPause() {
        SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_APP_IN_BACKGROUND, true)
        super.onPause()
    }

    override fun scrollToSpecificMessage(position: Int) {
        mLayoutManager.scrollToPosition(position)
    }

    override fun retrieveOldMessagesToGetMessage(msgId: String) {
        progressBar_chatBrowsing.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (chatBrowsingViewModel.retrieveOldMessagesToGetMessage(msgId)) {
                progressBar_chatBrowsing.visibility = View.GONE
                adapter.searchForSpecificMessage(msgId)
            } else {
                progressBar_chatBrowsing.visibility = View.GONE
                Toast.makeText(
                    context,
                    context?.getString(R.string.general_error_msg),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun cancelReplyOnMessage() {
        replyMessage = replyMessage.copy(first = "", second = "")
        sendMessage_replyPart_ConstraintLayout.visibility = View.GONE
        sendMessage_ConstraintLayout.setBackgroundResource(R.drawable.rect_round)
    }

    override fun onDestroy() {
        chatBrowsingViewModel.checkMessagesNotExceedLimit()
        SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_APP_IN_BACKGROUND, true)
        super.onDestroy()
    }

    override fun setBigImageVisible(
        fromImageView: ImageView,
        isImage: Boolean,
        body: String,
        senderName: String,
        path: String?
    ) {
        if (isImage) {

            animator = GestureTransitions.from<ImageView>(fromImageView).into(expandedImage);

            animator.addPositionUpdateListener(this::applyImageAnimationState)
            openFullImage(path)
        } else {
            animator = GestureTransitions.from<ImageView>(fromImageView).into(expandedVideo);

            animator.addPositionUpdateListener(this::applyVideoAnimationState)
            openFullVideo(path)
        }
        image_toolbar.title = senderName
        if (body.trim().isEmpty()) {
            messageBody_TV.visibility = View.INVISIBLE
        } else {
            messageBody_TV.visibility = View.VISIBLE
            messageBody_TV.text = body
        }
    }

    private fun applyImageAnimationState(position: Float, isLeaving: Boolean) {
        fullBackground = root.findViewById<View>(R.id.single_image_back)
        fullBackground.alpha = position
        waterMark_TV_fullscreen.alpha = position
        image_toolbar.alpha = position
        expandedVideo.visibility = View.GONE
        openImage_layout.visibility =
            if (position == 0f && isLeaving) View.INVISIBLE else View.VISIBLE
        fullBackground.visibility =
            if (position == 0f && isLeaving) View.INVISIBLE else View.VISIBLE
        expandedImage.visibility = if (position == 0f && isLeaving) View.INVISIBLE else View.VISIBLE
        image_toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        expandedImage.setOnClickListener {
            if (image_toolbar.visibility == View.VISIBLE && image_toolbar.alpha > 0) {
                image_toolbar.visibility = View.INVISIBLE
            } else {
                image_toolbar.visibility = View.VISIBLE
            }
        }
        if (isLeaving) {
            messageBody_TV.visibility = View.GONE
//            adapter.notifyDataSetChanged()
            animator.removePositionUpdateListener(this::applyImageAnimationState)
        }
    }

    private fun applyVideoAnimationState(position: Float, isLeaving: Boolean) {
        fullBackground.alpha = position
        waterMark_TV_fullscreen.alpha = position
        expandedImage.visibility = View.GONE
        openImage_layout.visibility =
            if (position == 0f && isLeaving) View.INVISIBLE else View.VISIBLE
        fullBackground.visibility =
            if (position == 0f && isLeaving) View.INVISIBLE else View.VISIBLE
        expandedVideo.visibility = if (position == 0f && isLeaving) View.INVISIBLE else View.VISIBLE
        openVideo_videoView.visibility =
            if (position == 0f && isLeaving) View.INVISIBLE else View.VISIBLE
        if (isLeaving && openVideo_videoView.isPlaying) {
            messageBody_TV.visibility = View.GONE
//            adapter.notifyDataSetChanged()
            animator.removePositionUpdateListener(this::applyVideoAnimationState)
            mMediaPlayer.stop()
        }

    }

    private fun openFullImage(path: String?) {
        // Setting image drawable from 'from' view to 'to' to prevent flickering
        if (expandedImage.getDrawable() == null) {
            expandedImage.setImageDrawable(userIV.getDrawable())
        }
        // Updating gesture image settings
        // Resetting to initial image state
        expandedImage.getController().resetState()
        animator.enterSingle(true)
        Glide.with(requireActivity())
            .load(path)
            .into(expandedImage)
    }

    private fun openFullVideo(path: String?) {
        // Setting image drawable from 'from' view to 'to' to prevent flickering
//        if (expandedImage.getDrawable() == null) {
//            expandedImage.setImageDrawable(userIV.getDrawable())
//        }
        // Updating gesture image settings
//        getSettingsController().apply(expandedImage)
        // Resetting to initial image state
        expandedVideo.getController().resetState()
        animator.enterSingle(true)
        GlobalScope.launch(Dispatchers.Main) {
            delay(250)
            openVideo_videoView.setVideoPath(path)
            mMediaPlayer = MediaPlayer.create(context, Uri.parse(path))
            val mMediaController = MediaController(requireContext())
            mMediaController.setAnchorView(openVideo_videoView)
            openVideo_videoView.setMediaController(mMediaController)
            mMediaPlayer.start()
            openVideo_videoView.start()
            openVideo_videoView.setOnPreparedListener {
                mMediaPlayer = it
                openVideo_videoView.visibility = View.VISIBLE
                mMediaPlayer.setOnSeekCompleteListener {
                    mMediaPlayer.start()
                }
            }
        }
    }

    fun onInitializeMenu(menu: Menu) {
        // Start with a menu Item order value that is high enough
        // so that your "PROCESS_TEXT" menu items appear after the
        // standard selection menu items like Cut, Copy, Paste.
        var menuItemOrder = 100
        for (resolveInfo in getSupportedActivities()) {
            menu.add(
                Menu.NONE, Menu.NONE,
                menuItemOrder++,
                resolveInfo.labelRes
            )
                .setIntent(createProcessTextIntentForResolveInfo(resolveInfo))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
    }

    private fun createProcessTextIntentForResolveInfo(info: ResolveInfo): Intent? {
        return createProcessTextIntent()
            ?.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, editText.editableText)
            ?.setClassName(
                info.activityInfo.packageName,
                info.activityInfo.name
            )
    }

    private fun createProcessTextIntent(): Intent? {
        return Intent()
            .setAction(Intent.ACTION_PROCESS_TEXT)
            .setType("text/plain")
    }

    private fun getSupportedActivities(): List<ResolveInfo> {
        val packageManager: PackageManager = requireContext().getPackageManager()
        return packageManager.queryIntentActivities(
            createProcessTextIntent()!!,
            0
        )
    }

//    private fun getSupportedActivities(context: Context,
//                                       packageManager: PackageManager): List<ResolveInfo>? {
//        val supportedActivities: MutableList<ResolveInfo> = java.util.ArrayList()
//        val canStartActivityForResult = context is Activity
//        if (!canStartActivityForResult) {
//            return supportedActivities
//        }
//        val unfiltered = packageManager.queryIntentActivities(createProcessTextIntent(), 0)
//        for (info in unfiltered) {
//            if (isSupportedActivity(info, context)) {
//                supportedActivities.add(info)
//            }
//        }
//        return supportedActivities
//    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.editText) {
            menu.add(0, 1, 0, "Bold");
            menu.add(0, 2, 0, "Italic");
        }
    }


    override fun onDetach() {
        SharePreferenceUtility.saveBooleanPreferences(context, Constant.IS_APP_IN_BACKGROUND, true)
        super.onDetach()
    }

    override fun onBalloonClick(view: View) {

    }


}