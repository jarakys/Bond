/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ec.bond.activity.ui.imageviewer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.android.camera.utils.decodeExifOrientation
import com.robertlevonyan.components.picker.PickerDialog
import com.ec.bond.R
import com.ec.bond.activity.IOnBackPressed
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.adapter.ImageViewerAdapter
import com.ec.bond.di.Injectable
import com.ec.bond.utils.CommonUtils.getRealPaths
import com.ec.bond.utils.CommonUtils.pickPhoto
import kotlinx.android.synthetic.main.image_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import javax.inject.Inject
import kotlin.math.max


class ImageViewerFragment : Fragment(), Injectable, IOnBackPressed {

//    lateinit  var imgShow: ImageView

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    val imageViewModel: ImageViewModel by viewModels {
        viewModelFactory
    }

    /** AndroidX navigation arguments */
    private val args: ImageViewerFragmentArgs by navArgs()
    lateinit var root: View
    lateinit var editText: EditText
    lateinit var mainAdapter: ImageViewerAdapter
    lateinit var secondaryAdapter: ImageViewerAdapter
    lateinit var mLayoutManager: RecyclerView.LayoutManager
    lateinit var mLayoutManagerSec: RecyclerView.LayoutManager
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
    }

    /** Default Bitmap decoding options */
    private val bitmapOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        // Keep Bitmaps at less than 1 MP
        if (max(outHeight, outWidth) > DOWNSAMPLE_SIZE) {
            val scaleFactorX = outWidth / DOWNSAMPLE_SIZE + 1
            val scaleFactorY = outHeight / DOWNSAMPLE_SIZE + 1
            inSampleSize = max(scaleFactorX, scaleFactorY)
        }
    }

    /** Bitmap transformation derived from passed arguments */
    private val bitmapTransformation: Matrix by lazy { decodeExifOrientation(args.orientation) }

    /** Flag indicating that there is depth data available for this image */
    private val isDepth: Boolean by lazy { args.depth }

    /** Data backing our Bitmap viewpager */
    private val bitmapList: MutableList<Bitmap> = mutableListOf()
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel
    val bitmaps = ArrayList<Bitmap>()
    lateinit var currentActivity: AppCompatActivity

    private fun imageViewFactory() = ImageView(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    override fun onBackPressed(): Boolean {
        findNavController().navigateUp()
        return true
    }


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View{

        root = inflater.inflate(R.layout.image_fragment, container, false) as View
//        imgShow = root.findViewById(R.id.imageShowImageView) as ImageView

        editText = root.findViewById(R.id.editText) as EditText
        chatBrowsingViewModel = ViewModelProvider(requireActivity()).get(ChatBrowsingViewModel::class.java)

        args.filePath.toCollection(imageViewModel.paths)
        return  root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUI()
        setupActionBar()
        imageButton.setOnClickListener{
//            startActivityForResult(Intent.createChooser(intentOpenGallery, "Select Picture"), PickerDialog.REQUEST_PICK_PHOTO)
//            GligarPicker().requestCode(PickerDialog.REQUEST_PICK_PHOTO).withFragment(this).show()
            val intentOpenGallery = Intent()


            intentOpenGallery.type = "image/*, video/*"
            intentOpenGallery.putExtra("limit", 15)
            intentOpenGallery.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intentOpenGallery.addCategory(Intent.CATEGORY_OPENABLE)
            intentOpenGallery.action = Intent.ACTION_GET_CONTENT
            intentOpenGallery.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            startActivityForResult(Intent.createChooser(intentOpenGallery, "Select Picture"), PickerDialog.REQUEST_PICK_PHOTO)
        }
        floatingButtonSendImage.setOnClickListener {

            if (args.recepient.isNotEmpty()){
                chatBrowsingViewModel.send_file(imageViewModel.paths.toTypedArray(), editText.text.toString())
            }
            NavHostFragment.findNavController(this).navigateUp()
        }

        imageViewModel.msg_sent.observe(viewLifecycleOwner, Observer<Boolean> { value ->

            if (value){
                navController.previousBackStackEntry?.savedStateHandle?.set("keyCamera", args.filePath)
                navController.popBackStack()
            }
        })

        textViewNames.setText(args.recepient)

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("keyImage")?.observe(
                viewLifecycleOwner, Observer<String> { result ->

            navController.previousBackStackEntry?.savedStateHandle?.set("keyCamera", args.filePath)
            navController.popBackStack()

        })


    }

    override fun onResume() {
        super.onResume()
        imageViewModel.imageSelected.observe(requireActivity(), Observer {
            if (this::secondaryAdapter.isInitialized) {
                mLayoutManager.scrollToPosition(it)
                mLayoutManagerSec.scrollToPosition(it)
                secondaryAdapter.notifyDataSetChanged()
            }
        })
        imageViewer_recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState === RecyclerView.SCROLL_STATE_IDLE) {
                    val position: Int = (imageViewer_recyclerView.layoutManager as LinearLayoutManager)
                            .findFirstVisibleItemPosition()
                    imageViewModel.changeItemSelected(position)
                }
            }
        } )
    }
    private fun updateUI() {
            if (!this@ImageViewerFragment::mainAdapter.isInitialized) {
                mLayoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                mLayoutManagerSec =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                mLayoutManager.setAutoMeasureEnabled(true)
                mLayoutManager.scrollToPosition(imageViewModel.imageSelected.value!!)
                mLayoutManagerSec.isAutoMeasureEnabled = true

                    imageViewer_recyclerView.setLayoutManager(mLayoutManager)
                    imageViewer_recyclerView.isNestedScrollingEnabled = false;
                    imageViewerSample_RecyclerView.setLayoutManager(mLayoutManagerSec)
                    imageViewerSample_RecyclerView.isNestedScrollingEnabled = false;
                    val helper = PagerSnapHelper()
                    helper.attachToRecyclerView(imageViewer_recyclerView)
                    mainAdapter = ImageViewerAdapter(imageViewModel.paths, requireContext(),true,imageViewModel)
                    secondaryAdapter = ImageViewerAdapter(imageViewModel.paths,requireContext(),false,imageViewModel)
                    imageViewer_recyclerView.adapter = mainAdapter
                    imageViewerSample_RecyclerView.adapter = secondaryAdapter
            } else {
                    mainAdapter.notifyDataSetChanged()
                    secondaryAdapter.notifyDataSetChanged()
            }
    }

    /** Utility function used to read input file into a byte array */
    private fun loadInputBuffer(filePath: String): ByteArray {
        val inputFile = File(filePath)
        return BufferedInputStream(inputFile.inputStream()).let { stream ->
            ByteArray(stream.available()).also {
                stream.read(it)
                stream.close()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val paths = getRealPaths(requireContext(),data).toTypedArray()
            viewLifecycleOwner.lifecycleScope.launch {
                imageViewModel.paths.addAll(pickPhoto(requireContext(), paths))
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    updateUI()
                }
            }
        }
    }
    /** Utility function used to add an item to the viewpager and notify it, in the main thread */
    private fun addItemToViewPager(view: ViewPager2, item: Bitmap) = view.post {
        bitmapList.add(item)
        view.adapter!!.notifyDataSetChanged()
    }

    /** Utility function used to decode a [Bitmap] from a byte array */
    private fun decodeBitmap(buffer: ByteArray, start: Int, length: Int): Bitmap {

        // Load bitmap from given buffer
        var bitmap = BitmapFactory.decodeByteArray(buffer, start, length, bitmapOptions)
        return bitmap
    }

    companion object {
        private val TAG = ImageViewerFragment::class.java.simpleName

        /** Maximum size of [Bitmap] decoded */
        private const val DOWNSAMPLE_SIZE: Int = 100  // 1MP

        /** These are the magic numbers used to separate the different JPG data chunks */
        private val JPEG_DELIMITER_BYTES = arrayOf(-1, -39)

        /**
         * Utility function used to find the markers indicating separation between JPEG data chunks
         */
        private fun findNextJpegEndMarker(jpegBuffer: ByteArray, start: Int): Int {

            // Sanitize input arguments
            assert(start >= 0) { "Invalid start marker: $start" }
            assert(jpegBuffer.size > start) {
                "Buffer size (${jpegBuffer.size}) smaller than start marker ($start)" }

            // Perform a linear search until the delimiter is found
            for (i in start until jpegBuffer.size - 1) {
                if (jpegBuffer[i].toInt() == JPEG_DELIMITER_BYTES[0] &&
                        jpegBuffer[i + 1].toInt() == JPEG_DELIMITER_BYTES[1]) {
                    return i + 2
                }
            }

            // If we reach this, it means that no marker was found
            throw RuntimeException("Separator marker not found in buffer (${jpegBuffer.size})")
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_call, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_deleteCall) {
            val file = File(imageViewModel.paths[imageViewModel.imageSelected.value!!])
            if (file.exists()){
                file.delete()
            }
            imageViewModel.paths.removeAt(imageViewModel.imageSelected.value!!)
            if (imageViewModel.paths.size == 0) {
                NavHostFragment.findNavController(this).navigateUp()
            } else {
                if ((imageViewModel.paths.size - 1) < imageViewModel.imageSelected.value!!){
                    imageViewModel.changeItemSelected(imageViewModel.paths.size - 1)
                }
                mainAdapter.notifyDataSetChanged()
                secondaryAdapter.notifyDataSetChanged()
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun setupActionBar() {
        setHasOptionsMenu(true)
        currentActivity = activity as AppCompatActivity
        currentActivity.setSupportActionBar(toolbar_imageViewer)

        currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        currentActivity.supportActionBar?.setDisplayShowHomeEnabled(true);
        toolbar_imageViewer.setNavigationOnClickListener { onBackPressed() }
    }
}
