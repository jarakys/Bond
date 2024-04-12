package com.ec.bond.activity.ui.chatbrowsing.openimage

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.activity.IOnBackPressed
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.utils.CommonUtils
import kotlinx.android.synthetic.main.fragment_open_image.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OpenImageFragment: Fragment(), IOnBackPressed {
    lateinit var root: View
    private val args: OpenImageFragmentArgs by navArgs()
    lateinit var currentActivity: AppCompatActivity
    lateinit var mMediaPlayer: MediaPlayer
    lateinit var mMediaController: MediaController
    lateinit var openImageViewModel: OpenImageViewModel
    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
    val waterMark by lazy {
        CommonUtils.waterMarkText(Blackbox.account.registeredNumber
                ?: "", "51e3eb37471db46a4c4f9472deb594d4a56ceae0a163728aa45b6a06ed1d43cb")
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_open_image, container, false) as View
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        openImageViewModel = ViewModelProvider(this).get(OpenImageViewModel::class.java)
        openImageViewModel.path = args.path
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActionBar()
        if (args.messageBody.isNotEmpty() && args.messageBody != "alert:#screenshot") {
            messageBody_TV.text = args.messageBody
            messageBody_TV.visibility = View.VISIBLE
        }
        openImage_imageView.apply {
            if (args.isImage) {
                transitionName = args.path
                set(BitmapFactory.decodeFile(args.path))
            } else {
                transitionName = "VID"
                Glide.with(context).load(args.path).thumbnail(0.1f).apply(requestOptions).dontTransform().into(this)
            }
        }
        waterMark_TV.text = waterMark
        waterMark_TV.visibility=View.GONE
        if (!args.isImage) {
            GlobalScope.launch(Dispatchers.Main) {
                delay(250)
                openVideo_videoView.setVideoPath(args.path)
                mMediaPlayer = MediaPlayer.create(context, Uri.parse(args.path))
                mMediaController = MediaController(requireContext())
                mMediaController.setAnchorView(openVideo_videoView)
                openVideo_videoView.setMediaController(mMediaController)
                mMediaPlayer.start()
                openVideo_videoView.start()
                openVideo_videoView.setOnPreparedListener {
                    mMediaPlayer = it
                    openImageViewModel.setVideoPlay(true)
                    mMediaPlayer.setOnSeekCompleteListener {
                        mMediaPlayer.start()
                    }
                }
            }

        }
        setBigImageVisible()
    }

    fun setBigImageVisible() {
//        isBigImageShowing = visible
//        if (!visible) {
//            mediaSection_layout.visibility = View.GONE
//        } else {
//            Glide.with(requireActivity())
//                    .load(imageUri)
//                    .into(expanded_imageView)
//            mediaSection_layout.visibility = View.VISIBLE
            expanded_image.positionAnimator.enter(true)
            val handler = Handler()
            val runnable = Runnable {
                expanded_image.positionAnimator.addPositionUpdateListener { float: Float, isLeaving: Boolean ->
                    if (isLeaving) {
                        onBackPressed()
//                        mediaSection_layout.visibility = View.INVISIBLE
//                        setBigImageVisible(false, true)
                    } else {
                        print(float)
                        root_layout.background = resources.getDrawable(R.color.transparent_color)
                        mediaSection_layout.background = resources.getDrawable(R.color.transparent_color)
                    }
                }
            }
            handler.postDelayed(runnable, 1000)
//        }
    }

    override fun onResume() {
        super.onResume()
        if (!args.isImage) {
            openImageViewModel.isPlaying.observe(this, androidx.lifecycle.Observer {
                if (it) {
                    openVideo_videoView.visibility = View.VISIBLE
                } else {
                    openVideo_videoView.visibility = View.GONE
                    openVideo_videoView.start()
                }
            })
        }
    }
    override fun onBackPressed(): Boolean {
        findNavController().navigateUp()
        return true
    }
    private fun setupActionBar() {
        setHasOptionsMenu(true)
        currentActivity = activity as AppCompatActivity
        currentActivity.setSupportActionBar(toolbar_openImage)

        currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        currentActivity.supportActionBar?.setDisplayShowHomeEnabled(true);
        toolbar_openImage.setNavigationOnClickListener { onBackPressed() }
    }
}