package com.ec.bond.activity.ui.chatbrowsing.document

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.utils.CommonUtils
import java.io.File

class ChooseDocumentsViewModel: ViewModel() {
    var documents = ArrayList<File>()
    var filteredDocuments = ArrayList<File>()
    var charFiltered = ArrayList<Char>()
    lateinit var searchView : SearchView
    lateinit var searchItem : MenuItem
    var isSearch = false
    var sortType = 0
    val SORT_BY_NAME = 0
    val SORT_BY_DATE = 1
    private val _endFragment = MutableLiveData<Boolean>(false)

    val endFragment: LiveData<Boolean> get() = _endFragment

    private val _isLongPressed = MutableLiveData<Boolean>(false)

    val isLongPressed: LiveData<Boolean> get() = _isLongPressed

    private val _longPressedTitle = MutableLiveData<String>()

    val longPressedTitle: LiveData<String> get() = _longPressedTitle

    var menuActionBar = MutableLiveData<Menu>()

    var menuInflater = MutableLiveData<MenuInflater>()
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel

    private var _selectedDocuments = MutableLiveData<ArrayList<File>>().apply {
        value = ArrayList()
    }

    fun addToSelectedDocuments(item: File){
        _selectedDocuments.value?.add(item)
    }

    fun clearSelectedDocuments(){
        _selectedDocuments.value?.clear()
    }

    fun getSizeSelectedDocuments(): Int? {
        return _selectedDocuments.value?.size
    }

    fun removeItemSelectedDocuments(item: File) {
        val removedItem = _selectedDocuments.value?.filter { it.absolutePath == item.absolutePath }?.first()
        _selectedDocuments.value?.remove(removedItem)
    }

    fun getSelectedDocuments() : ArrayList<File> {
        return _selectedDocuments.value!!
    }

    fun setIsLongPressed(isLongPress: Boolean) = _isLongPressed.postValue(isLongPress)

    fun setLongPressedTitle(title: String) = _longPressedTitle.postValue(title)

    /**
       position if null that means more than one document selected and shoud use
       _selectedDocuments
     */
    fun showSendAlert(context: Context,position: Int?) {
        var message: String
        var activity = context as AppCompatActivity
        if (_selectedDocuments.value?.size!! > 1) {
            message = "${_selectedDocuments.value?.size} " + activity.getString(R.string.media_documents)
        } else {
            if (position == null) {
                message = "${_selectedDocuments.value?.get(0)?.name} "
            } else {
                message = "${filteredDocuments[position].name} "
            }
        }
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Send $message ?")
                .setCancelable(false)
                .setPositiveButton(activity.getString(R.string.send)) { dialog, id ->
                    sendFiles(context, position)
                }
                .setNegativeButton(activity.getString(R.string.cancel)) { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
        val alert = builder.create()
        alert.show()
    }

    private fun sendFiles(context: Context, position: Int?) {
        chatBrowsingViewModel.send_file(getFilesUri(context,position),"",getFilesName(position))
        _endFragment.postValue(true)
    }

    private fun getFilesName(position: Int?): ArrayList<String> {
        var response = ArrayList<String>()
        if (getSizeSelectedDocuments()!! > 0) {
            for (file in getSelectedDocuments()) {
                response.add(file.name)
            }
        } else {
            if (position != null) {
                response.add(filteredDocuments[position].name)
            }
        }
        return response
    }

    private fun getFilesUri(context: Context, position: Int?): Array<String> {
        var urisWithExtensions = ArrayList<Pair<String,String>>()
        if (getSizeSelectedDocuments()!! > 0) {
            for (file in getSelectedDocuments()) {
                CommonUtils.getPathFromUri(context, file.toUri())?.let { urisWithExtensions.add(Pair(it,file.extension)) }

            }
        } else {
            if (position != null) {
                CommonUtils.getPathFromUri(context, filteredDocuments[position].toUri())?.let { urisWithExtensions.add(Pair(it,filteredDocuments[position].extension)) }
            }
        }
        var response = CommonUtils.moveDocumentToLocalPath(context,urisWithExtensions.toTypedArray())
        return response.toTypedArray()
    }
}