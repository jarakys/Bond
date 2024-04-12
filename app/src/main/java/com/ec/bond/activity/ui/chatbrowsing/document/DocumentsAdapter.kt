package com.ec.bond.activity.ui.chatbrowsing.document

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.robertlevonyan.components.picker.set
import com.ec.bond.R
import com.ec.bond.blackbox.model.Message
import com.ec.bond.blackbox.model.MessageType
import com.ec.bond.utils.CommonUtils.indicesOf
import kotlinx.android.synthetic.main.item_document.view.*
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class DocumentsAdapter(val context: Context,val chooseDocumentsViewModel: ChooseDocumentsViewModel): RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {
    private var layoutInflater: LayoutInflater? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        var view = layoutInflater?.inflate(R.layout.item_document,parent,false)

        return DocumentViewHolder(view!!)
    }

    override fun getItemCount(): Int = chooseDocumentsViewModel.filteredDocuments.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is DocumentViewHolder) {
            holder.bind(position)
        }
    }
    inner class DocumentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int){
            if (chooseDocumentsViewModel.getSelectedDocuments().any { it.absolutePath == chooseDocumentsViewModel.filteredDocuments[position].absolutePath }) {
                itemView.document_constraintlayout.setBackgroundResource(R.color.blue_selected_documents)
            } else {
                val outValue = TypedValue()
                context?.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                itemView.document_constraintlayout.setBackgroundResource(outValue.resourceId)
            }

            var text = SpannableString(chooseDocumentsViewModel.filteredDocuments[position].name)
            if (chooseDocumentsViewModel.charFiltered.size > 0) {
                var selectedIndex = ArrayList<Int>()
                for (char in chooseDocumentsViewModel.charFiltered) {

                    if (chooseDocumentsViewModel.filteredDocuments[position].name.toLowerCase().contains(char.toLowerCase())) {
                        var foundedIndices = chooseDocumentsViewModel.filteredDocuments[position].name.toLowerCase().indicesOf(char.toLowerCase())
                        var index = chooseDocumentsViewModel.filteredDocuments[position].name.toLowerCase().indexOf(char.toLowerCase())
                        if (index in selectedIndex) {
                            foundedIndices.firstOrNull { it !in selectedIndex }.let {
                                if (it != null) {
                                    index = it
                                }
                                else return
                            }
                        }
                        selectedIndex.add(index)

                        // make "text" (characters pos-1 to pos) red
                        text.setSpan(ForegroundColorSpan(context.getColor(R.color.text_selected)), index, index + 1, 0);
                        itemView.fileName_TV.setText(text, TextView.BufferType.SPANNABLE)
                    }
                }
            }else {
                itemView.fileName_TV.text = chooseDocumentsViewModel.filteredDocuments[position].name
            }
            itemView.fileSize_TV.text = chooseDocumentsViewModel.filteredDocuments[position].let {
                readableFileSize(it.length())
            }
            var formattedDate = SimpleDateFormat("MM/dd/yy").format(Date(chooseDocumentsViewModel.filteredDocuments[position].lastModified()))
            itemView.fileDate_TV.text = formattedDate.toString()

            getFileIcon(Message.getFileType(chooseDocumentsViewModel.filteredDocuments[position].path))?.let { itemView.fileType_IV.set(it) }

            itemView.setOnClickListener { it: View? ->
                if (chooseDocumentsViewModel.isLongPressed.value!!) {
                    chooseDocumentsViewModel.getSelectedDocuments().forEach {
                        if (it.absolutePath == chooseDocumentsViewModel.filteredDocuments[position].absolutePath) {
                            chooseDocumentsViewModel.removeItemSelectedDocuments(chooseDocumentsViewModel.filteredDocuments[position])
                            val outValue = TypedValue()
                            context.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                            itemView.document_constraintlayout.setBackgroundResource(outValue.resourceId)
                            if (chooseDocumentsViewModel.getSizeSelectedDocuments() == 0) {
                                chooseDocumentsViewModel.setIsLongPressed(false)
                            } else {
                                chooseDocumentsViewModel.setLongPressedTitle(chooseDocumentsViewModel.getSizeSelectedDocuments().toString())
                            }
                            return@setOnClickListener
                        }
                    }
                    itemView.document_constraintlayout.setBackgroundResource(R.color.blue_selected_documents)
                    addInSelectedList(position)
                } else {
                    //handle onClick only
                    chooseDocumentsViewModel.showSendAlert(context, position)
                }
            }
            itemView.setOnLongClickListener {
                if (!chooseDocumentsViewModel.isLongPressed.value!!) {

                    chooseDocumentsViewModel.setIsLongPressed(true)
                    addInSelectedList(position)
                    itemView.isSelected = true
                    itemView.document_constraintlayout.setBackgroundResource(R.color.blue_selected_documents)
                    return@setOnLongClickListener true
                } else {
                    return@setOnLongClickListener false
                }
            }
        }
        @SuppressLint("UseCompatLoadingForDrawables")
        private fun getFileIcon(type: MessageType): Drawable? {
            when (type){
                MessageType.DocumentPDF -> return context.getDrawable(R.drawable.pdf_icon)
                MessageType.DocumentMicrosoftWord -> return context.getDrawable(R.drawable.word_icon)
                MessageType.DocumentMicrosoftPowerPoint -> return context.getDrawable(R.drawable.powerpoint_icon)
                MessageType.DocumentMicrosoftExcel -> return context.getDrawable(R.drawable.excel_icon)
                MessageType.DocumentAppleKeynote -> return context.getDrawable(R.drawable.keynote_icon)
                else -> return context.getDrawable(R.drawable.generic_file_icon)
            }
        }


    }

    fun readableFileSize(size: Long): String? {
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
    fun handleCancelSelection() {
        chooseDocumentsViewModel.clearSelectedDocuments()
        chooseDocumentsViewModel.setIsLongPressed(false)
    }

    fun addInSelectedList(position: Int) {
        chooseDocumentsViewModel.addToSelectedDocuments(chooseDocumentsViewModel.filteredDocuments[position])
        chooseDocumentsViewModel.setLongPressedTitle(chooseDocumentsViewModel.getSizeSelectedDocuments().toString())
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    chooseDocumentsViewModel.filteredDocuments.clear()
                    if (chooseDocumentsViewModel.sortType == chooseDocumentsViewModel.SORT_BY_NAME)
                        chooseDocumentsViewModel.documents.sortedBy { it.name }.toCollection(chooseDocumentsViewModel.filteredDocuments)
                    else
                        chooseDocumentsViewModel.documents.sortedByDescending { it.lastModified() }.toCollection(chooseDocumentsViewModel.filteredDocuments)

                    chooseDocumentsViewModel.charFiltered.clear()
                    chooseDocumentsViewModel.isSearch = false
                } else {
                    val filteredList: ArrayList<File> = ArrayList()
                    chooseDocumentsViewModel.isSearch = true
                    chooseDocumentsViewModel.charFiltered.clear()
                    charString.toCollection(chooseDocumentsViewModel.charFiltered)
                    for (file in chooseDocumentsViewModel.documents) {
                        if (file.name.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(file)
                        }
                    }
                    chooseDocumentsViewModel.filteredDocuments = filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = chooseDocumentsViewModel.filteredDocuments
                return filterResults
            }

            override fun publishResults(
                    charSequence: CharSequence,
                    filterResults: FilterResults
            ) {
                chooseDocumentsViewModel.filteredDocuments = filterResults.values as ArrayList<File>
                // refresh the list with filtered data
                notifyDataSetChanged()
            }
        }
    }
}