package com.ec.bond.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.adapter.ArchivedAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.ChatItem
import kotlinx.android.synthetic.main.activity_archive.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArchiveActivity : BaseActivity(),ArchivedAdapter.ArchiveChatlistitemclick {

    private val actionModeCallback: androidx.appcompat.view.ActionMode.Callback = object : androidx.appcompat.view.ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater: MenuInflater = mode!!.menuInflater
            inflater.inflate(R.menu.menu_archive_contexual, menu)
            var muteItem = menu!!.findItem(R.id.action_mute)

            return true
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
            return false // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        override fun onActionItemClicked(mode: androidx.appcompat.view.ActionMode?, item: MenuItem?): Boolean {
            return when (item!!.itemId) {
                R.id.action_unarchive -> {
                    selectedChat.forEach {
                        indexList.add(chatlist.indexOf(it))
                    }
                    lifecycleScope.launch(Dispatchers.Main) {
                        val chats = selectedChat.toList()
                        actionmode?.finish()
                        indexList.clear()
                        mode?.finish()
                        Blackbox.unArchiveChats(chats)
                    }
                    true
                }
                R.id.action_mute -> {
                    true
                }
                else -> false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode?) {
            selectedChat.forEach {
                adapter!!.unSelectItem(it)
            }
            selectedChat.clear()
            adapter!!.setLongpress(false)
//            if(adapter!!.chatlistisEmpty()){
//                finish()
//            }

        }
    }
    var adapter: ArchivedAdapter? = null
    var selectedChat: ArrayList<ChatItem> = arrayListOf()
    var chatlist: ArrayList<ChatItem> = arrayListOf()
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel



    companion object {
        var actionmode: androidx.appcompat.view.ActionMode? = null
        var islongpress: Boolean = false
        var isvisible: Boolean = true
        var isArchived: Boolean = false
        var indexList: ArrayList<Int> = arrayListOf()
        /**
         * remove the contextual action mode
         */
        fun removeBar() {
            if (actionmode != null) {
                isvisible = false
                actionmode!!.finish()
                actionmode = null
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        chatBrowsingViewModel = ViewModelProvider(this).get(ChatBrowsingViewModel::class.java)
        rv_archive_list.layoutManager = ArchiveLayoutManager(this,chatBrowsingViewModel)
        rv_archive_list.setHasFixedSize(true)

        val ab: Toolbar = findViewById(R.id.tb_toolbarsearch)
        setSupportActionBar(ab)
        val actionBar: ActionBar? = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.title = "Archived chats"

        Blackbox.archivedChatItems.observe(this, Observer {
            chatlist = it
            progressBar.visibility = View.GONE
            if (it.isEmpty()) {
                adapter = ArchivedAdapter(this, arrayListOf(), this)
                Toast.makeText(this, getString(R.string.you_have_ho_chat_lists_yet), Toast.LENGTH_LONG).show()
            } else {
                adapter = ArchivedAdapter(this, it, this)
            }
            rv_archive_list.adapter = adapter
            adapter?.notifyDataSetChanged()
        })

    }
    override fun onChatitemSelect(chatItem: ChatItem, checkview: ImageView, itemview: View, islongpress: Boolean) {
        if (!islongpress) {
            adapter!!.setLongpress(true)
            itemview.setBackgroundResource(R.color.light_gray_selection)
            checkview.visibility = View.VISIBLE
            chatItem.isSelected = true
            selectedChat.add(chatItem)
            actionmode = startSupportActionMode(actionModeCallback)
            actionmode!!.title = selectedChat.size.toString()
            actionmode!!.invalidate()
        } else {
            if (chatItem.isSelected) {
                checkview.visibility = View.INVISIBLE
                val outValue = TypedValue()
                theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                itemview.setBackgroundResource(outValue.resourceId)
                chatItem.isSelected = false
                selectedChat.remove(chatItem)
                if (selectedChat.isEmpty()) {
                    adapter!!.setLongpress(false)
                    actionmode!!.finish()
                } else {
                    actionmode!!.title = selectedChat.size.toString()
                    actionmode!!.invalidate()
                }
            } else {
                checkview.visibility = View.VISIBLE
                itemview.setBackgroundResource(R.color.light_gray_selection)
                chatItem.isSelected = true
                selectedChat.add(chatItem)
                actionmode!!.title = selectedChat.size.toString()
                actionmode!!.invalidate()

            }
        }
    }

    override fun onChatitemclick(chatItem: ChatItem) {
        val intent = Intent(this, ChatBrowsingActivity::class.java)

        var recipient = ""
        var image: String
        if (chatItem.contact != null) {
            recipient = chatItem.contact.registeredNumber
            image = chatItem.contact.chatImagePath.value ?: ""
        } else {
            recipient = chatItem.group!!.ID
            image = chatItem.group!!.chatImagePath.value ?: ""
        }
        //intent.putExtra("chatItem", data)
        //intent.putExtra("recipientImage", image)
        intent.putExtra("recipient", recipient)
        //intent.putExtra("isFromChatList", true)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class ArchiveLayoutManager(context: Context,val chatBrowsingViewModel: ChatBrowsingViewModel): LinearLayoutManager(context){
        var isFinish = false
        override fun onLayoutCompleted(state: RecyclerView.State?) {
            super.onLayoutCompleted(state)
            for (index in 0..findLastVisibleItemPosition()) {
//            chatBrowsingViewModel.chatItemType = chatViewModel.chatItems[index]
//            chatBrowsingViewModel.registerChatRefType()
//            if (chatViewModel.chatItems[index].)
                if (chatlist[index].group == null){
                    if (chatlist[index].contact?.isGetData == false){
                        chatBrowsingViewModel.retry_msg_data(null, null ,80,chatlist[index].contact?.registeredNumber!!)

                    }
                } else {
                    if (chatlist[index].group?.isGetData == false){
                        chatBrowsingViewModel.retry_msg_data(null, null ,80,chatlist[index].group?.ID!!)
                    }
                }
            }
        }

        override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
            val result = super.scrollVerticallyBy(dy, recycler, state)
            if(!isFinish){
                for(index in findFirstVisibleItemPosition()..findLastVisibleItemPosition()){
                    if (findLastVisibleItemPosition() == chatlist.size - 1) {
                        isFinish = true
                    }
//                chatBrowsingViewModel.chatItemType = chatViewModel.chatItems[index]
//                chatBrowsingViewModel.registerChatRefType()
                    if (chatlist[index].group == null){
                        if (chatlist[index].contact?.isGetData == false){
                            chatBrowsingViewModel.retry_msg_data(null, null ,80,chatlist[index].contact?.registeredNumber!!)


                        }
                    } else {
                        if (chatlist[index].group?.isGetData == false){
                            chatBrowsingViewModel.retry_msg_data(null, null ,80,chatlist[index].group?.ID!!)
                        }
                    }
//                chatBrowsingViewModel.retry_msg_data(null, null, 80, chatViewModel.chatItems[index])
                }
            }
            return result
        }
    }
}