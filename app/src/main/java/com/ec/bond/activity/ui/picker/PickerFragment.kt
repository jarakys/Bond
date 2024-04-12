package com.ec.bond.activity.ui.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.activity.ui.chatbrowsing.pickimage.Options
import com.ec.bond.activity.ui.chatbrowsing.pickimage.PickImage
import com.ec.bond.di.Injectable
import com.ec.bond.utils.CommonUtils.pickPhoto
import com.esafirm.imagepicker.features.registerImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kbeanie.multipicker.api.CameraImagePicker
import com.kbeanie.multipicker.api.FilePicker
import com.kbeanie.multipicker.api.Picker
import com.kbeanie.multipicker.api.callbacks.FilePickerCallback
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback
import com.kbeanie.multipicker.api.entity.ChosenFile
import com.kbeanie.multipicker.api.entity.ChosenImage
import com.robertlevonyan.components.picker.PickerDialog
import kotlinx.android.synthetic.main.fragment_picker.*
import kotlinx.coroutines.launch
import java.io.File


class PickerFragment : BottomSheetDialogFragment(), Injectable , FilePickerCallback,
    ImagePickerCallback {
    private val REQUEST_CODE_PICK_PDF = 1058
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel
    private val args: PickerFragmentArgs by navArgs()
    var returnValue = ArrayList<String>()
    lateinit var intentOpenGallery: Intent
    private lateinit var filePicker: FilePicker
    private lateinit var cameraPicker: CameraImagePicker

    val launcher = registerImagePicker {
            images ->
        var paths= arrayListOf<String>()
        (images.indices).map {
                index ->
            var originalFileUri= Uri.parse("file://"+images.get(index).path)
            paths.add(originalFileUri?.path!!)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            moveToImageViewer(pickPhoto(requireContext(), paths.toTypedArray()));
        }
    }
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_picker, container, false)
        chatBrowsingViewModel = ViewModelProvider(requireActivity()).get(ChatBrowsingViewModel::class.java)
        return root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageCamera.setOnClickListener {
            /*val options = Options.init()
                    .setRequestCode(100) //Request code for activity results
                    .setCount(15) //Number of images to restict selection count
                    .setFrontfacing(false) //Front Facing camera on start
                    .setPreSelectedUrls(returnValue) //Pre selected Image Urls
                    .setExcludeVideos(false) //Option to exclude videos
                    .setVideoDurationLimitinSeconds(30) //Duration for video recording
                    .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT) //Orientaion
                    .setPath(context?.filesDir?.absolutePath) //Custom Path For media Storage


            PickImage.start(this, options)*/
            showCamera()

            //openImageClick()
        }

        imageGallery.setOnClickListener {
            openImagePickMultiple()
        }

        pickDocument.setOnClickListener {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                }
                startActivityForResult(intent, REQUEST_CODE_PICK_PDF)
            } else {
                openDocumentPicker()
            }
        }

    }

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


    fun openImageClick(){
        cameraPicker= CameraImagePicker(this)
        cameraPicker.shouldGenerateMetadata(true)
        cameraPicker.shouldGenerateThumbnails(true)
        cameraPicker.setImagePickerCallback(this)
        cameraPicker.pickImage()
        dismiss()


    }
    private fun openGallery() {
        intentOpenGallery = Intent()
        intentOpenGallery.type = "image/*, video/*"
        intentOpenGallery.putExtra("limit", 15)
        intentOpenGallery.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intentOpenGallery.addCategory(Intent.CATEGORY_OPENABLE)
        intentOpenGallery.action = Intent.ACTION_GET_CONTENT
        intentOpenGallery.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        startActivityForResult(Intent.createChooser(intentOpenGallery, "Select Picture"), PickerDialog.REQUEST_PICK_PHOTO)

    }


    fun openImagePickMultiple(){
        launcher.launch()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val paths: Array<String>
            if (requestCode == 101) {
                var x = data?.data
            }

            if(requestCode==2296){
                if (SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        // perform action when allow permission success
                    } else {
                        Toast.makeText(requireContext(), "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            if(requestCode==Picker.PICK_FILE){
                filePicker.submit(data)
            }

            if(requestCode==Picker.PICK_IMAGE_CAMERA){
                cameraPicker.submit(data)
            }

            if (requestCode == REQUEST_CODE_PICK_PDF) {
                Toast.makeText(requireContext(), "REQUEST_CODE_PICK_PDF", Toast.LENGTH_SHORT).show()
                data?.data?.let {
                    val filePath = it.path?.replace("/document/primary:", "/storage/emulated/0/").orEmpty()
                    showSendAlert(requireContext(),
                        arrayListOf(filePath)
                        )
                }
            }

            if (requestCode == 100) {
                paths = data?.getStringArrayListExtra(PickImage.IMAGE_RESULTS)?.toTypedArray()!!
                viewLifecycleOwner.lifecycleScope.launch {
                    if (paths.size == 1) {
                        if (context?.filesDir?.absolutePath?.let { paths[0].contains(it) }!!) {
                            moveToImageViewer(pickPhoto(requireContext(), paths))
                            val file = File(paths[0])
                            file.delete()
                        }
                    } else {
                        moveToImageViewer(pickPhoto(requireContext(), paths))
                    }
                }
            } else {
                /*paths = getRealPaths(requireContext(),data).toTypedArray()
                viewLifecycleOwner.lifecycleScope.launch {
                    moveToImageViewer(pickPhoto(requireContext(), paths))
                }*/
            }
        }
    }

    private fun moveToImageViewer(paths: ArrayList<String>) {
        Log.e("paths---",""+paths)
        if(paths.size>0){
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
                .navigate(PickerFragmentDirections.actionPickerFragmentToImageViewerFragment(
                    paths.toTypedArray(),  0, false, args.recipient
                ))
        }

    }

    private fun moveToChooseDocuments(){
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
                .navigate(PickerFragmentDirections.actionPickerFragmentToChooseDocumentsFragment())
    }

    fun openDocumentPicker(){
        filePicker= FilePicker(this)
        filePicker.setFilePickerCallback(this)
        filePicker.setMimeType("application/pdf")
        filePicker.pickFile()
    }

    override fun onError(p0: String?) {

    }

    override fun onImagesChosen(p0: MutableList<ChosenImage>?) {
        p0?.let {

            if(p0.size>0){
                var files1= arrayListOf<String>()
                for(item in p0){
                    var originalFileUri= Uri.parse("file://"+item.getOriginalPath())
                    files1.add(originalFileUri.path!!)
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    moveToImageViewer(pickPhoto(requireContext(), files1.toTypedArray()));

                }

            }
        }
    }

    override fun onFilesChosen(p0: MutableList<ChosenFile>?) {
        p0?.let {


            if(p0.size>0){
                var files1= arrayListOf<String>()
                for(item in p0){
                    var originalFileUri= Uri.parse("file://"+item.getOriginalPath())
                    files1.add(originalFileUri.path!!)
                }

                showSendAlert(requireContext(),files1)
            }
        }
    }

    fun showSendAlert(context: Context, files: ArrayList<String>) {
        var message: String
        var activity = context as AppCompatActivity
        message = "${files.size} " + activity.getString(R.string.media_documents)
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Send $message ?")
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.send)) { dialog, id ->
                sendFiles(context, files)
            }
            .setNegativeButton(activity.getString(R.string.cancel)) { dialog, id ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun sendFiles(context: Context, files: ArrayList<String>) {
        chatBrowsingViewModel.send_file(files.toTypedArray(),"",getFilesName(files))
        dismiss()
    }

    private fun getFilesName(files: ArrayList<String>): ArrayList<String> {
        var response = ArrayList<String>()
        if (files.size!! > 0) {
            for (file in files) {
                var fileCreated=File(file)
                response.add(fileCreated.name)
            }
        } else {

        }
        return response
    }




}