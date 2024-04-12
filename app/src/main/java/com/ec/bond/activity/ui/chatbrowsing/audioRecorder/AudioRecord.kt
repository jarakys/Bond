package com.ec.bond.activity.ui.chatbrowsing.audioRecorder

import android.animation.Animator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ec.bond.R
import java.text.SimpleDateFormat
import java.util.*

class AudioRecord {
    enum class UserBehaviour {
        CANCELING, LOCKING, NONE
    }

    enum class RecordingBehaviour {
        CANCELED, LOCKED, LOCK_DONE, RELEASED
    }

    interface RecordingListener {
        fun onRecordingStarted()
        fun onRecordingLocked()
        fun onRecordingCompleted()
        fun onRecordingCanceled()
    }

    private val TAG = "AudioRecordView"
    private val viewContainer: LinearLayout? = null
    private val layoutAttachmentOptions: LinearLayout? = null
    private var recordAudio_IV: View? = null
    private var lockArrow_IV: View? = null
    private var lock_IV: View? = null
    private var mic_IV: View? = null
    private var trash: View? = null
    private var trash_cover: View? = null
    private var sendAudio_IV: View? = null
    var sendMessage_IV: View? = null
        private set
    private val layoutAttachment: View? = null
    private var trash_layout: View? = null
    private val layoutMessage: View? = null
    val attachmentView: View? = null
    var attachment_IV: View? = null
    var camera_IV: View? = null
        private set
    var emoji_IV: View? = null
        private set
    private var slideCancel_Layout: View? = null
    private var cancel_Layout: View? = null
    private var layoutLock: View? = null
    private var effect1_Layout: View? = null
    private var effect2_Layout: View? = null
    var messageView: EditText? = null
        private set
    private var recordTimer_TV: TextView? = null
    private var slide_TV: TextView? = null
    private val stop: ImageView? = null
    private val audio: ImageView? = null
    private val send: ImageView? = null
    private var animBlink: Animation? = null
    private var animJump: Animation? = null
    private var animJumpFast: Animation? = null
    private var isDeleting = false
    private var stopTrackingAction = false
    private var handler: Handler? = null
    private var audioTotalTime = 0
    private var timerTask: TimerTask? = null
    private var audioTimer: Timer? = null
    private var timeFormatter: SimpleDateFormat? = null
    private var lastX = 0f
    private var lastY = 0f
    private var firstX = 0f
    private var firstY = 0f
    private val directionOffset = 0f
    private var cancelOffset = 0f
    private var lockOffset = 0f
    private var dp = 0f
    private var isLocked = false
    private var userBehaviour = UserBehaviour.NONE
    var recordingListener: RecordingListener? = null
    var isLayoutDirectionRightToLeft = false
    var screenWidth = 0
    var screenHeight = 0

    //    private List<AttachmentOption> attachmentOptionList;
    //    private AttachmentOptionsListener attachmentOptionsListener;
    private val layoutAttachments: List<LinearLayout>? = null
    private var context: Context? = null
    var isShowCameraIcon = true
        private set
    var isShowAttachmentIcon = true
        private set
    var isShowEmojiIcon = true
        private set
    private val removeAttachmentOptionAnimation = false
    fun initView(view: View?) {
        if (view == null) {
            showErrorLog("initView ViewGroup can't be NULL")
            return
        }
        context = view.context

//        view.removeAllViews();
//        view.addView(LayoutInflater.from(view.getContext()).inflate(R.layout.record_view, null));
        timeFormatter = SimpleDateFormat("m:ss", Locale.getDefault())
        val displayMetrics = view.context.resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels

//        isLayoutDirectionRightToLeft = view.getContext().getResources().getBoolean(R.bool.is_right_to_left);

//        viewContainer = view.findViewById(R.id.layoutContainer);
//        layoutAttachmentOptions = view.findViewById(R.id.layoutAttachmentOptions);

        attachment_IV = view.findViewById(R.id.attachment_IV);
        camera_IV = view.findViewById(R.id.camera_IV)
        emoji_IV = view.findViewById(R.id.emoji_IV)
        messageView = view.findViewById(R.id.editText)


        recordAudio_IV = view.findViewById(R.id.recordAudio_IV)
        sendAudio_IV = view.findViewById(R.id.sendAudio_IV)
        sendMessage_IV = view.findViewById(R.id.sendMessage_IV)
        lock_IV = view.findViewById(R.id.lock_IV)
        lockArrow_IV = view.findViewById(R.id.lockArrow_IV)
        trash_layout = view.findViewById(R.id.trash_layout)
        //        layoutMessage = view.findViewById(R.id.layoutMessage);
//        layoutAttachment = view.findViewById(R.id.layoutAttachment);
        slide_TV = view.findViewById(R.id.slide_TV)
        recordTimer_TV = view.findViewById(R.id.recordTimer_TV)
        slideCancel_Layout = view.findViewById(R.id.slideCancel_Layout)
        cancel_Layout = view.findViewById(R.id.cancel_Layout)
        effect2_Layout = view.findViewById(R.id.effect2_Layout)
        effect1_Layout = view.findViewById(R.id.effect1_Layout)
        layoutLock = view.findViewById(R.id.layoutLock)
        mic_IV = view.findViewById(R.id.mic_IV)
        trash = view.findViewById(R.id.trash)
        trash_cover = view.findViewById(R.id.trash_cover)
        handler = Handler(Looper.getMainLooper())
        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, view.context.resources.displayMetrics)
        animBlink = AnimationUtils.loadAnimation(view.context,
                R.anim.blink)
        animJump = AnimationUtils.loadAnimation(view.context,
                R.anim.jump)
        animJumpFast = AnimationUtils.loadAnimation(view.context,
                R.anim.jump_fast)
        setupRecording()
        //        setupAttachmentOptions();
    }

    fun changeSlideToCancelText(textResourceId: Int) {
        slide_TV!!.setText(textResourceId)
    }

    fun showCameraIcon(showCameraIcon: Boolean) {
        isShowCameraIcon = showCameraIcon
        if (showCameraIcon) {
            camera_IV!!.visibility = View.VISIBLE
        } else {
            camera_IV!!.visibility = View.GONE
        }
    }

    fun showAttachmentIcon(showAttachmentIcon: Boolean) {
        isShowAttachmentIcon = showAttachmentIcon

        if (showAttachmentIcon) {
            attachment_IV!!.setVisibility(View.VISIBLE);
        } else {
            attachment_IV!!.setVisibility(View.INVISIBLE);
        }
    }

    fun showEmojiIcon(showEmojiIcon: Boolean) {
        isShowEmojiIcon = showEmojiIcon
        if (showEmojiIcon) {
            emoji_IV!!.visibility = View.VISIBLE
        } else {
            emoji_IV!!.visibility = View.INVISIBLE
        }
    }

    //    public void setAttachmentOptions(List<AttachmentOption> attachmentOptionList, final AttachmentOptionsListener attachmentOptionsListener) {
    //
    //        this.attachmentOptionList = attachmentOptionList;
    //        this.attachmentOptionsListener = attachmentOptionsListener;
    //
    //        if (this.attachmentOptionList != null && !this.attachmentOptionList.isEmpty()) {
    //            layoutAttachmentOptions.removeAllViews();
    //            int count = 0;
    //            LinearLayout linearLayoutMain = null;
    //            layoutAttachments = new ArrayList<>();
    //
    //            for (final AttachmentOption attachmentOption : this.attachmentOptionList) {
    //
    //                if (count == 6) {
    //                    break;
    //                }
    //
    //                if (count == 0 || count == 3) {
    //                    linearLayoutMain = new LinearLayout(context);
    //                    linearLayoutMain.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    //                    linearLayoutMain.setOrientation(LinearLayout.HORIZONTAL);
    //                    linearLayoutMain.setGravity(Gravity.CENTER);
    //
    //                    layoutAttachmentOptions.addView(linearLayoutMain);
    //                }
    //
    //                LinearLayout linearLayout = new LinearLayout(context);
    //                linearLayout.setLayoutParams(new LinearLayout.LayoutParams((int) (dp * 84), LinearLayout.LayoutParams.WRAP_CONTENT));
    //                linearLayout.setPadding((int) (dp * 4), (int) (dp * 12), (int) (dp * 4), (int) (dp * 0));
    //                linearLayout.setOrientation(LinearLayout.VERTICAL);
    //                linearLayout.setGravity(Gravity.CENTER);
    //
    //                layoutAttachments.add(linearLayout);
    //
    //                ImageView imageView = new ImageView(context);
    //                imageView.setLayoutParams(new LinearLayout.LayoutParams((int) (dp * 48), (int) (dp * 48)));
    //                imageView.setImageResource(attachmentOption.getResourceImage());
    //
    //                TextView textView = new TextView(context);
    //                TextViewCompat.setTextAppearance(textView, R.style.TextAttachmentOptions);
    //                textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    //                textView.setPadding((int) (dp * 4), (int) (dp * 4), (int) (dp * 4), (int) (dp * 0));
    //                textView.setMaxLines(1);
    //                textView.setText(attachmentOption.getTitle());
    //
    //                linearLayout.addView(imageView);
    //                linearLayout.addView(textView);
    //
    //                linearLayoutMain.addView(linearLayout);
    //
    //                linearLayout.setOnClickListener(new View.OnClickListener() {
    //                    @Override
    //                    public void onClick(View view) {
    //                        hideAttachmentOptionView();
    //                        AudioRecordView.this.attachmentOptionsListener.onClick(attachmentOption);
    //                    }
    //                });
    //
    //                count++;
    //            }
    //        }
    //    }
    //
    //    public void hideAttachmentOptionView() {
    //        if (layoutAttachment.getVisibility() == View.VISIBLE) {
    //            imageViewAttachment.performClick();
    //        }
    //    }
    //
    //    public void showAttachmentOptionView() {
    //        if (layoutAttachment.getVisibility() != View.VISIBLE) {
    //            imageViewAttachment.performClick();
    //        }
    //    }
    //
    //    private void setupAttachmentOptions() {
    //        imageViewAttachment.setOnClickListener(new View.OnClickListener() {
    //            @Override
    //            public void onClick(View view) {
    //                if (layoutAttachment.getVisibility() == View.VISIBLE) {
    //                    int x = isLayoutDirectionRightToLeft ? (int) (dp * (18 + 40 + 4 + 56)) : (int) (screenWidth - (dp * (18 + 40 + 4 + 56)));
    //                    int y = (int) (dp * 220);
    //
    //                    int startRadius = 0;
    //                    int endRadius = (int) Math.hypot(screenWidth - (dp * (8 + 8)), (dp * 220));
    //
    //                    Animator anim = ViewAnimationUtils.createCircularReveal(layoutAttachment, x, y, endRadius, startRadius);
    //                    anim.addListener(new Animator.AnimatorListener() {
    //                        @Override
    //                        public void onAnimationStart(Animator animator) {
    //
    //                        }
    //
    //                        @Override
    //                        public void onAnimationEnd(Animator animator) {
    //                            layoutAttachment.setVisibility(View.GONE);
    //                        }
    //
    //                        @Override
    //                        public void onAnimationCancel(Animator animator) {
    //
    //                        }
    //
    //                        @Override
    //                        public void onAnimationRepeat(Animator animator) {
    //
    //                        }
    //                    });
    //                    anim.start();
    //
    //                } else {
    //
    //                    if (!removeAttachmentOptionAnimation) {
    //                        int count = 0;
    //                        if (layoutAttachments != null && !layoutAttachments.isEmpty()) {
    //
    //                            int[] arr = new int[]{5, 4, 2, 3, 1, 0};
    //
    //                            if (isLayoutDirectionRightToLeft) {
    //                                arr = new int[]{3, 4, 0, 5, 1, 2};
    //                            }
    //
    //                            for (int i = 0; i < layoutAttachments.size(); i++) {
    //                                if (arr[i] < layoutAttachments.size()) {
    //                                    final LinearLayout layout = layoutAttachments.get(arr[i]);
    //                                    layout.setScaleX(0.4f);
    //                                    layout.setAlpha(0f);
    //                                    layout.setScaleY(0.4f);
    //                                    layout.setTranslationY(dp * 48 * 2);
    //                                    layout.setVisibility(View.INVISIBLE);
    //
    //                                    layout.animate().scaleX(1f).scaleY(1f).alpha(1f).translationY(0).setStartDelay((count * 25) + 50).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
    //                                    layout.setVisibility(View.VISIBLE);
    //
    //                                    count++;
    //                                }
    //                            }
    //                        }
    //                    }
    //
    //                    int x = isLayoutDirectionRightToLeft ? (int) (dp * (18 + 40 + 4 + 56)) : (int) (screenWidth - (dp * (18 + 40 + 4 + 56)));
    //                    int y = (int) (dp * 220);
    //
    //                    int startRadius = 0;
    //                    int endRadius = (int) Math.hypot(screenWidth - (dp * (8 + 8)), (dp * 220));
    //
    //                    Animator anim = ViewAnimationUtils.createCircularReveal(layoutAttachment, x, y, startRadius, endRadius);
    //                    anim.setDuration(500);
    //                    layoutAttachment.setVisibility(View.VISIBLE);
    //                    anim.start();
    //                }
    //            }
    //        });
    //    }
    //
    //    public void removeAttachmentOptionAnimation(boolean removeAttachmentOptionAnimation) {
    //        this.removeAttachmentOptionAnimation = removeAttachmentOptionAnimation;
    //    }
    //    public View setContainerView(int layoutResourceID) {
    //        View view = LayoutInflater.from(viewContainer.getContext()).inflate(layoutResourceID, null);
    //
    //        if (view == null) {
    //            showErrorLog("Unable to create the Container View from the layoutResourceID");
    //            return null;
    //        }
    //
    //        viewContainer.removeAllViews();
    //        viewContainer.addView(view);
    //        return view;
    //    }
    fun setAudioRecordButtonImage(imageResource: Int) {
        audio!!.setImageResource(imageResource)
    }

    fun setStopButtonImage(imageResource: Int) {
        stop!!.setImageResource(imageResource)
    }

    fun setSendButtonImage(imageResource: Int) {
        send!!.setImageResource(imageResource)
    }

    private fun setupRecording() {
        sendMessage_IV!!.animate().scaleX(0f).scaleY(0f).setDuration(100).setInterpolator(LinearInterpolator()).start()
        messageView!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString().trim { it <= ' ' }.isEmpty()) {
                    if (sendMessage_IV!!.visibility != View.GONE) {
                        sendMessage_IV!!.visibility = View.GONE
                        sendMessage_IV!!.animate().scaleX(0f).scaleY(0f).setDuration(100).setInterpolator(LinearInterpolator()).start()
                    }
                    if (isShowCameraIcon) {
                        if (camera_IV!!.visibility != View.VISIBLE && !isLocked) {
                            camera_IV!!.visibility = View.VISIBLE
                            camera_IV!!.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(LinearInterpolator()).start()
                        }
                    }
                } else {
                    if (sendMessage_IV!!.visibility != View.VISIBLE && !isLocked) {
                        sendMessage_IV!!.visibility = View.VISIBLE
                        sendMessage_IV!!.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(LinearInterpolator()).start()
                    }
                    if (isShowCameraIcon) {
                        if (camera_IV!!.visibility != View.GONE) {
                            camera_IV!!.visibility = View.GONE
                            camera_IV!!.animate().scaleX(0f).scaleY(0f).setDuration(100).setInterpolator(LinearInterpolator()).start()
                        }
                    }
                }
            }
        })
        recordAudio_IV!!.setOnTouchListener(OnTouchListener { view, motionEvent ->
            if (isDeleting) {
                return@OnTouchListener true
            }
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                cancelOffset = (screenWidth / 2.8).toFloat()
                lockOffset = (screenWidth / 2.5).toFloat()
                if (firstX == 0f) {
                    firstX = motionEvent.rawX
                }
                if (firstY == 0f) {
                    firstY = motionEvent.rawY
                }
                startRecord()
            } else if (motionEvent.action == MotionEvent.ACTION_UP
                    || motionEvent.action == MotionEvent.ACTION_CANCEL) {
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    stopRecording(RecordingBehaviour.RELEASED)
                }
            } else if (motionEvent.action == MotionEvent.ACTION_MOVE) {
                if (stopTrackingAction) {
                    return@OnTouchListener true
                }
                var direction = UserBehaviour.NONE
                val motionX = Math.abs(firstX - motionEvent.rawX)
                val motionY = Math.abs(firstY - motionEvent.rawY)
                if (if (isLayoutDirectionRightToLeft) motionX > directionOffset && lastX > firstX && lastY > firstY else motionX > directionOffset && lastX < firstX && lastY < firstY) {
                    if (if (isLayoutDirectionRightToLeft) motionX > motionY && lastX > firstX else motionX > motionY && lastX < firstX) {
                        direction = UserBehaviour.CANCELING
                    } else if (motionY > motionX && lastY < firstY) {
                        direction = UserBehaviour.LOCKING
                    }
                } else if (if (isLayoutDirectionRightToLeft) motionX > motionY && motionX > directionOffset && lastX > firstX else motionX > motionY && motionX > directionOffset && lastX < firstX) {
                    direction = UserBehaviour.CANCELING
                } else if (motionY > motionX && motionY > directionOffset && lastY < firstY) {
                    direction = UserBehaviour.LOCKING
                }
                if (direction == UserBehaviour.CANCELING) {
                    if (userBehaviour == UserBehaviour.NONE || motionEvent.rawY + recordAudio_IV!!.width / 2 > firstY) {
                        userBehaviour = UserBehaviour.CANCELING
                    }
                    if (userBehaviour == UserBehaviour.CANCELING) {
                        translateX(-(firstX - motionEvent.rawX))
                    }
                } else if (direction == UserBehaviour.LOCKING) {
                    if (userBehaviour == UserBehaviour.NONE || motionEvent.rawX + recordAudio_IV!!.width / 2 > firstX) {
                        userBehaviour = UserBehaviour.LOCKING
                    }
                    if (userBehaviour == UserBehaviour.LOCKING) {
                        translateY(-(firstY - motionEvent.rawY))
                    }
                }
                lastX = motionEvent.rawX
                lastY = motionEvent.rawY
            }
            view.onTouchEvent(motionEvent)
            true
        })
        sendAudio_IV!!.setOnClickListener {
            isLocked = false
            stopRecording(RecordingBehaviour.LOCK_DONE)
        }
        cancel_Layout!!.setOnClickListener {
            isLocked = false
            stopRecording(RecordingBehaviour.CANCELED)
        }

    }

    private fun translateY(y: Float) {
        if (y < -lockOffset) {
            locked()
            recordAudio_IV!!.translationY = 0f
            return
        }
        if (layoutLock!!.visibility != View.VISIBLE) {
            layoutLock!!.visibility = View.VISIBLE
        }
        recordAudio_IV!!.translationY = y
        layoutLock!!.translationY = y / 2
        recordAudio_IV!!.translationX = 0f
    }

    private fun translateX(x: Float) {
        if (if (isLayoutDirectionRightToLeft) x > cancelOffset else x < -cancelOffset) {
            canceled()
            recordAudio_IV!!.translationX = 0f
            slideCancel_Layout!!.translationX = 0f
            return
        }
        recordAudio_IV!!.translationX = x
        slideCancel_Layout!!.translationX = x
        layoutLock!!.translationY = 0f
        recordAudio_IV!!.translationY = 0f
        if (Math.abs(x) < mic_IV!!.width / 2) {
            if (layoutLock!!.visibility != View.VISIBLE) {
                layoutLock!!.visibility = View.VISIBLE
            }
        } else {
            if (layoutLock!!.visibility != View.GONE) {
                layoutLock!!.visibility = View.GONE
            }
        }
    }

    private fun locked() {
        stopTrackingAction = true
        stopRecording(RecordingBehaviour.LOCKED)
        isLocked = true
    }

    private fun canceled() {
        stopTrackingAction = true
        stopRecording(RecordingBehaviour.CANCELED)
    }

    private fun stopRecording(recordingBehaviour: RecordingBehaviour) {
        stopTrackingAction = true
        firstX = 0f
        firstY = 0f
        lastX = 0f
        lastY = 0f
        userBehaviour = UserBehaviour.NONE
        recordAudio_IV!!.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f).setDuration(100).setInterpolator(LinearInterpolator()).start()
        slideCancel_Layout!!.translationX = 0f
        slideCancel_Layout!!.visibility = View.GONE
        layoutLock!!.visibility = View.GONE
        layoutLock!!.translationY = 0f
        lockArrow_IV!!.clearAnimation()
        lock_IV!!.clearAnimation()
        if (isLocked) {
            return
        }
        if (recordingBehaviour == RecordingBehaviour.LOCKED) {
            sendAudio_IV!!.visibility = View.VISIBLE
            cancel_Layout!!.visibility = View.VISIBLE

            if (recordingListener != null) recordingListener!!.onRecordingLocked()
        }
        else if (recordingBehaviour == RecordingBehaviour.CANCELED) {
            mic_IV!!.clearAnimation()
            recordTimer_TV!!.visibility = View.INVISIBLE
            cancel_Layout!!.visibility = View.INVISIBLE
            mic_IV!!.visibility = View.INVISIBLE
            sendAudio_IV!!.visibility = View.GONE
            effect2_Layout!!.visibility = View.GONE
            effect1_Layout!!.visibility = View.GONE
            timerTask!!.cancel()
            delete()
            if (recordingListener != null) recordingListener!!.onRecordingCanceled()
        }
        else if (recordingBehaviour == RecordingBehaviour.RELEASED || recordingBehaviour == RecordingBehaviour.LOCK_DONE) {
            mic_IV!!.clearAnimation()
            recordTimer_TV!!.visibility = View.INVISIBLE
            mic_IV!!.visibility = View.INVISIBLE
            cancel_Layout!!.visibility = View.INVISIBLE
            messageView!!.visibility = View.VISIBLE
            if (isShowAttachmentIcon) {
                attachment_IV!!.setVisibility(View.VISIBLE);
            }
            if (isShowCameraIcon) {
                camera_IV!!.visibility = View.VISIBLE
            }
            if (isShowEmojiIcon) {
                emoji_IV!!.visibility = View.VISIBLE
            }
            sendAudio_IV!!.visibility = View.GONE
            messageView!!.requestFocus()
            effect2_Layout!!.visibility = View.GONE
            effect1_Layout!!.visibility = View.GONE
            timerTask!!.cancel()
            if (recordingListener != null) recordingListener!!.onRecordingCompleted()
        }
    }

    private fun startRecord() {
        if (recordingListener != null) recordingListener!!.onRecordingStarted()

//        hideAttachmentOptionView();
        stopTrackingAction = false
        messageView!!.visibility = View.INVISIBLE
                attachment_IV!!.setVisibility(View.INVISIBLE);
        camera_IV!!.visibility = View.INVISIBLE
        emoji_IV!!.visibility = View.INVISIBLE
        recordAudio_IV!!.animate().scaleXBy(1f).scaleYBy(1f).setDuration(200).setInterpolator(OvershootInterpolator()).start()
        recordTimer_TV!!.visibility = View.VISIBLE
        layoutLock!!.visibility = View.VISIBLE
        slideCancel_Layout!!.visibility = View.VISIBLE
        cancel_Layout!!.visibility = View.GONE
        mic_IV!!.visibility = View.VISIBLE
        effect2_Layout!!.visibility = View.VISIBLE
        effect1_Layout!!.visibility = View.VISIBLE
        mic_IV!!.startAnimation(animBlink)
        lockArrow_IV!!.clearAnimation()
        lock_IV!!.clearAnimation()
        lockArrow_IV!!.startAnimation(animJumpFast)
        lock_IV!!.startAnimation(animJump)
        if (audioTimer == null) {
            audioTimer = Timer()
            //timeFormatter!!.timeZone = TimeZone.getTimeZone("UTC")
        }
        timerTask = object : TimerTask() {
            override fun run() {
                handler!!.post {
                    recordTimer_TV!!.text = timeFormatter!!.format(Date((audioTotalTime * 1000).toLong()))
                    audioTotalTime++
                }
            }
        }
        audioTotalTime = 0
        audioTimer!!.schedule(timerTask, 0, 1000)
    }

    private fun delete() {
        mic_IV!!.visibility = View.VISIBLE
        mic_IV!!.rotation = 0f
        isDeleting = true
        recordAudio_IV!!.isEnabled = false
        handler!!.postDelayed(object : Runnable {
            override fun run() {
                isDeleting = false
                recordAudio_IV!!.isEnabled = true

                if (isShowAttachmentIcon) {
                    attachment_IV!!.setVisibility(View.VISIBLE);
                }
                if (isShowCameraIcon) {
                    camera_IV!!.visibility = View.VISIBLE
                }
                if (isShowEmojiIcon) {
                    emoji_IV!!.visibility = View.VISIBLE
                }
            }
        }, 1250)
        mic_IV!!.animate()
                .translationY(-dp * 150)
                .rotation(180f)
                .scaleXBy(0.6f)
                .scaleYBy(0.6f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                var displacement = 0f
                displacement = if (isLayoutDirectionRightToLeft) {
                    dp * 40
                } else {
                    -dp * 40
                }
                trash!!.translationX = displacement
                trash_cover!!.translationX = displacement
                trash_cover!!.animate().translationX(0f).rotation(-120f).setDuration(350).setInterpolator(DecelerateInterpolator()).start()
                trash!!.animate().translationX(0f).setDuration(350).setInterpolator(DecelerateInterpolator()).setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        trash!!.visibility = View.VISIBLE
                        trash_cover!!.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animator) {}
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                }).start()
            }

            override fun onAnimationEnd(animation: Animator) {
                mic_IV!!.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(LinearInterpolator()).setListener(
                    object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            mic_IV!!.visibility = View.INVISIBLE
                            mic_IV!!.rotation = 0f
                            var displacement = 0f
                            displacement = if (isLayoutDirectionRightToLeft) {
                                dp * 40
                            } else {
                                -dp * 40
                            }
                            trash_cover!!.animate().rotation(0f).setDuration(150).setStartDelay(50).start()
                            trash!!.animate().translationX(displacement).setDuration(200).setStartDelay(250).setInterpolator(DecelerateInterpolator()).start()
                            trash_cover!!.animate().translationX(displacement).setDuration(200).setStartDelay(250).setInterpolator(DecelerateInterpolator()).setListener(object : Animator.AnimatorListener {
                                override fun onAnimationStart(animation: Animator) {}
                                override fun onAnimationEnd(animation: Animator) {
                                    messageView!!.visibility = View.VISIBLE
                                    messageView!!.requestFocus()
                                }

                                override fun onAnimationCancel(animation: Animator) {}
                                override fun onAnimationRepeat(animation: Animator) {}
                            }).start()
                        }

                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}
                    }
                ).start()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        }).start()
    }

    private fun showErrorLog(s: String) {
        Log.e(TAG, s)
    }
}