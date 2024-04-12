package com.ec.bond.activity.ui.chatbrowsing.document

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ec.bond.R
import com.ec.bond.activity.IOnBackPressed
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import kotlinx.android.synthetic.main.fragment_documents.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ChooseDocumentsFragment: Fragment(),IOnBackPressed {
    lateinit var root: View
    val currentActivity: AppCompatActivity by lazy { activity as AppCompatActivity }
    val documentsAdapter : DocumentsAdapter by lazy {
    DocumentsAdapter(requireContext(), chooseDocumentsViewModel)
    }
    private var isLongPressed = false
    private lateinit var chooseDocumentsViewModel: ChooseDocumentsViewModel
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_documents, container, false) as View
        chooseDocumentsViewModel = ViewModelProvider(this).get(ChooseDocumentsViewModel::class.java)
        chooseDocumentsViewModel.chatBrowsingViewModel = ViewModelProvider(requireActivity()).get(ChatBrowsingViewModel::class.java)

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            if (chooseDocumentsViewModel.filteredDocuments.size == 0) {
                documents_progressBar.visibility = View.VISIBLE
                fillDocuments()
                renderUI()
            } else {
                renderUI()
            }
        }


    }

    private fun renderUI() {
        documents_progressBar.visibility = View.GONE
        var linearLayoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
        linearLayoutManager.isAutoMeasureEnabled = true
        documents_RV.layoutManager = linearLayoutManager
        documents_RV.adapter = documentsAdapter

    }

    suspend fun fillDocuments() {
        withContext(Dispatchers.IO){
            val ROOT_DIR = Environment.getExternalStorageDirectory().absolutePath
            val ANDROID_DIR = File("$ROOT_DIR/Android")
            val DATA_DIR = File("$ROOT_DIR/data")
            var x = File(ROOT_DIR).walk()
                    // befor entering this dir check if
                    .onEnter{ !it.isHidden // it is not hidden
                            && it != ANDROID_DIR // it is not Android directory
                            && it != DATA_DIR // it is not data directory
                            && !File(it, ".nomedia").exists() //there is no .nomedia file inside
                    }.filter { it.extension == "pdf" || it.extension == "docx" || it.extension == "doc" || it.extension == "odt"
                            || it.extension == "rtf" || it.extension == "xls" || it.extension == "xlsb"
                            || it.extension == "xlsm" || it.extension == "xlsx" || it.extension == "pptx"
                            || it.extension == "pptm" || it.extension == "ppt" || it.extension == "pages"
                            || it.extension == "numbers" || it.extension == "key"}.sortedBy { it.name }
                    .toCollection(chooseDocumentsViewModel.documents)
            chooseDocumentsViewModel.documents.toCollection(chooseDocumentsViewModel.filteredDocuments)
        }
    }

    override fun onResume() {
        super.onResume()

        if(chooseDocumentsViewModel.isSearch == true) {
            chooseDocumentsViewModel.searchItem.expandActionView()
            chooseDocumentsViewModel.searchView.setQuery(chooseDocumentsViewModel.charFiltered.joinToString(""),false)
        }
        if (chooseDocumentsViewModel.isLongPressed.value == true){
            documentsAdapter.notifyDataSetChanged()
        }
        chooseDocumentsViewModel.isLongPressed.observe(viewLifecycleOwner, Observer {
            isLongPressed = it
            if (it) {
                chooseDocumentsViewModel.menuActionBar.value?.clear()
                chooseDocumentsViewModel.menuInflater.value?.inflate(R.menu.menu_document_selected, chooseDocumentsViewModel.menuActionBar.value)
                currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                documents_toolbar.setNavigationOnClickListener {
                    documentsAdapter.handleCancelSelection()
                }
            } else {
                chooseDocumentsViewModel.menuActionBar.value?.clear()
                chooseDocumentsViewModel.menuInflater.value?.inflate(R.menu.menu_document, chooseDocumentsViewModel.menuActionBar.value)
                currentActivity.supportActionBar?.title = getString(R.string.media_documents)
                documents_toolbar.setNavigationOnClickListener {
                    onBackPressed()
                }
                documents_RV.adapter?.notifyDataSetChanged()
            }

        })
        chooseDocumentsViewModel.longPressedTitle.observe(viewLifecycleOwner, Observer {
            if (chooseDocumentsViewModel.isLongPressed.value == true)
                currentActivity.supportActionBar?.title = it
        })
        chooseDocumentsViewModel.endFragment.observe(viewLifecycleOwner, Observer {
            if (it){
                onBackPressed()
            }
        })
    }
    private fun setupActionBar() {
        currentActivity.setSupportActionBar(documents_toolbar)
        setHasOptionsMenu(true)
        currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        currentActivity.supportActionBar?.setDisplayShowHomeEnabled(true);
        documents_toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()

        if (!isLongPressed) {
            inflater.inflate(R.menu.menu_document, menu)
            val item = menu.findItem(R.id.action_search)

            val searchManager = currentActivity.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            chooseDocumentsViewModel.searchItem = menu.findItem(R.id.action_search)
            chooseDocumentsViewModel.searchView = chooseDocumentsViewModel.searchItem.actionView as SearchView

            chooseDocumentsViewModel.searchView = menu.findItem(R.id.action_search)
                    .actionView as SearchView
            chooseDocumentsViewModel.searchView.setSearchableInfo(
                    searchManager
                            .getSearchableInfo(currentActivity.componentName)
            )
            chooseDocumentsViewModel.searchView.queryHint = "Search"
            chooseDocumentsViewModel.searchView.setMaxWidth(Int.MAX_VALUE)

            chooseDocumentsViewModel.searchView.setOnCloseListener {
                chooseDocumentsViewModel.searchView.onActionViewCollapsed()
                chooseDocumentsViewModel.filteredDocuments.clear()
                chooseDocumentsViewModel.charFiltered.clear()
                if (chooseDocumentsViewModel.sortType == chooseDocumentsViewModel.SORT_BY_NAME)
                    chooseDocumentsViewModel.documents.sortedBy { it.name }.toCollection(chooseDocumentsViewModel.filteredDocuments)
                else
                    chooseDocumentsViewModel.documents.sortedByDescending { it.lastModified() }.toCollection(chooseDocumentsViewModel.filteredDocuments)
                chooseDocumentsViewModel.isSearch = false
                documentsAdapter.notifyDataSetChanged()
                return@setOnCloseListener true
            }
            chooseDocumentsViewModel.searchView.setOnQueryTextListener(object :
                    SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean { // filter recycler view when query submitted
                    documentsAdapter.getFilter().filter(query)
                    return false
                }

                override fun onQueryTextChange(query: String): Boolean { // filter recycler view when text is changed
                    documentsAdapter.getFilter().filter(query)
                    return false
                }
            })

        }
        else {
            inflater.inflate(R.menu.menu_document_selected, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
        chooseDocumentsViewModel.menuActionBar.postValue(menu)
        chooseDocumentsViewModel.menuInflater.postValue(inflater)
        chooseDocumentsViewModel.menuActionBar.value?.get(chooseDocumentsViewModel.sortType)?.isChecked = true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var id = item.itemId
        if (id == R.id.action_sort_by_name) {
            if (item.isChecked == false) {
                item.isChecked = true
                chooseDocumentsViewModel.sortType = chooseDocumentsViewModel.SORT_BY_NAME
                chooseDocumentsViewModel.filteredDocuments.clear()
                chooseDocumentsViewModel.documents.sortedBy { it.name }.toCollection(chooseDocumentsViewModel.filteredDocuments)

                documentsAdapter.notifyDataSetChanged()
            }
        } else if (id == R.id.action_sort_by_date) {
            if (item.isChecked == false) {
                item.isChecked = true
                chooseDocumentsViewModel.sortType = chooseDocumentsViewModel.SORT_BY_DATE
                chooseDocumentsViewModel.filteredDocuments.clear()
                chooseDocumentsViewModel.documents.sortedByDescending { it.lastModified() }.toCollection(chooseDocumentsViewModel.filteredDocuments)
                documentsAdapter.notifyDataSetChanged()
            }
        } else if(id == R.id.action_send) {
            if (chooseDocumentsViewModel.getSizeSelectedDocuments()!! > 0) {
                chooseDocumentsViewModel.showSendAlert(requireContext(), null)
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed(): Boolean {
        NavHostFragment.findNavController(this).navigateUp()
        return true
    }
}