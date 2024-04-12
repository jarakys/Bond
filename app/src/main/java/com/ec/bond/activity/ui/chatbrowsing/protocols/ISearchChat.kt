package com.ec.bond.activity.ui.chatbrowsing.protocols

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

interface ISearchChat {
    fun searchWithText(word: String, firstPosition: Int, lastPosition: Int)
    fun searchUpArrow(textView: TextView, firstPosition: Int, lastPosition: Int, recyclerView: RecyclerView)
    fun searchDownArrow(textView: TextView, lastPosition: Int, recyclerView: RecyclerView)
    fun cancelSearch()
}