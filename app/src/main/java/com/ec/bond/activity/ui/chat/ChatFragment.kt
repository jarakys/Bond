package com.ec.bond.activity.ui.chat

//import com.spe2eeapp.masmak.blackbox.model.BBAccountOnlineStatus
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.activity.ArchiveActivity
import com.ec.bond.activity.ChatBrowsingActivity
import com.ec.bond.activity.ui.calls.CallsViewModel
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.adapter.ArchiveCount
import com.ec.bond.adapter.ChatListAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBAccountRegistrationState
import com.ec.bond.blackbox.model.ChatItem
import com.ec.bond.di.Injectable
import com.ec.bond.utils.CommonUtils
import kotlinx.android.synthetic.main.fragment_chats.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChatFragment : Fragment(), Injectable, ChatListAdapter.Chatlistitemclick , ArchiveCount.Archivecountitemclick {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    val chatViewModel: ChatViewModel by viewModels {
        viewModelFactory
    }
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel
    lateinit var mLayoutManager: LinearLayoutManager
    lateinit var recyclerViewReadyCallback: RecyclerViewReadyCallback
    private lateinit var callsViewModel: CallsViewModel

    //    var chatItems: ArrayList<ChatItem> = ArrayList()
    var chatListAdapter: ChatListAdapter? = null
    var archiveCountAdapter: ArchiveCount? = null
    var contactAdapter: ConcatAdapter? = null
    var root: View? = null
    var chatlist: ArrayList<ChatItem> = arrayListOf()
    var chatlistdata: List<ChatItem> = listOf()
    lateinit var rv_contact: RecyclerView
    lateinit var currentActivity: AppCompatActivity
    var selectedChat: ArrayList<ChatItem> = arrayListOf()
    var title: String = ""
    var isMute: Boolean = false

    private val actionModeCallback: androidx.appcompat.view.ActionMode.Callback = object : androidx.appcompat.view.ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater: MenuInflater = mode!!.menuInflater
            inflater.inflate(R.menu.menu_chat, menu)
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
                R.id.action_archive -> {
                    selectedChat.forEach {
                        indexList.add(chatlist.indexOf(it))
                    }
                    lifecycleScope.launch(Dispatchers.Main) {
                        val chats = selectedChat.toList()
                        selectedChat.forEach {
                            chatListAdapter?.unSelectItem(it)
                            archiveCountAdapter?.updateCount()
                        }
                        selectedChat.clear()
                        indexList.clear()
                        mode!!.finish()

                        Blackbox.archiveChats(chats)
                    }
                    true
                }
                R.id.action_mute -> {
                    true
                }
                R.id.action_selectall -> {
                    chatlist.forEach {
                        it.isSelected = true
                    }
                    chatListAdapter!!.setLongpress(true)
                    selectedChat.addAll(chatlist)
                    chatListAdapter = ChatListAdapter(context!!, chatlist, this@ChatFragment)
                    rv_contact.adapter = chatListAdapter
                    mode!!.title = selectedChat.size.toString()
                    mode!!.invalidate()
                    true
                }
                else -> false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode?) {
            if (!isvisible) {
                dismissBar()
            } else {
                selectedChat.forEach {
                    chatListAdapter!!.unSelectItem(it)
                }
                selectedChat.clear()
                chatListAdapter!!.setLongpress(false)
            }
        }
    }

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

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_chats, container, false) as View
        chatBrowsingViewModel = ViewModelProvider(this).get(ChatBrowsingViewModel::class.java)
        callsViewModel = ViewModelProvider(this).get(CallsViewModel::class.java)

        //chatViewModel.setOnOfflineStatus(BBAccountOnlineStatus.Online)
        initUI()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentActivity = activity as AppCompatActivity

        chatListAdapter = ChatListAdapter(context!!, arrayListOf(), this)

        Blackbox.chatItems.observe(viewLifecycleOwner, Observer<ArrayList<ChatItem>> {
            chatlist = it
            if (chatlist.isEmpty()) {
                Toast.makeText(context!!, getString(R.string.you_have_ho_chat_lists_yet), Toast.LENGTH_LONG).show()
            } else {
                val context = context ?: return@Observer
                chatViewModel.chatItems = it

                val size = Blackbox.archivedChatItems.value!!.size
                title = if (size == 0) "Tap and hold on a chat for more options" else "Archived (${size})"

                chatListAdapter = ChatListAdapter(context, chatlist, this)
                archiveCountAdapter = ArchiveCount(requireContext(),title,this)
                contactAdapter = ConcatAdapter(chatListAdapter,archiveCountAdapter)
                rv_contact.adapter = contactAdapter
                chatListAdapter?.notifyDataSetChanged()
            }
        })

        Blackbox.account.state.observe(viewLifecycleOwner, Observer<BBAccountRegistrationState> { value ->
            GlobalScope.launch(Dispatchers.Main) {
                if (value == BBAccountRegistrationState.Registered) {
                    progressBar.visibility = View.GONE
                }
            }
        })
        CommonUtils.filterText.observe(viewLifecycleOwner, Observer {
            if (!chatlist.isNullOrEmpty()) {
                if (!it.isNullOrEmpty()) {
                    chatListAdapter?.filter?.filter(it)
//                (rv_contact.adapter as ChatListAdapter).filter.filter(it)
                } else {
                    chatListAdapter?.filter?.filter("")
                    chatListAdapter?.notifyDataSetChanged()
//                (rv_contact.adapter as ChatListAdapter).filter.filter("")
//                (rv_contact.adapter as ChatListAdapter).notifyDataSetChanged()
                }
            }

        })

    }

    private fun initUI() {

        rv_contact = root!!.findViewById(R.id.rv_contact_list) as RecyclerView
//        mLayoutManager =
//                LinearLayoutManager(context)
        rv_contact.setHasFixedSize(true)
        mLayoutManager = ChatListLayoutManager(requireContext(), chatBrowsingViewModel, chatViewModel)
        //mLayoutManager = LinearLayoutManager(requireContext())
        rv_contact.layoutManager = mLayoutManager
        //rv_contact.setItemAnimator(DefaultItemAnimator())


        /*rv_contact.addOnItemTouchListener(
                RecyclerItemClickListener(context, rv_contact, object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {
                        val intent = Intent(context, ChatBrowsingActivity::class.java)
//
                        val data = chatViewModel.chatItems[position]
                        var recipient = ""
                        var image: String
                        if (data.contact != null) {
                            recipient = data.contact.registeredNumber
                            image = data.contact.chatImagePath.value ?: ""
                        } else {
                            recipient = data.group!!.ID
                            image = data.group!!.chatImagePath.value ?: ""
                        }
//                        intent.putExtra("chatItem", data)
//                        intent.putExtra("recipientImage", image)
                        intent.putExtra("recipient", recipient)
//                        intent.putExtra("isFromChatList", true)
                        startActivity(intent)
                    }

                    override fun onLongItemClick(view: View?, position: Int) {
                        val options: Array<CharSequence> = arrayOf("option 1", "option 2", "option 3")
                        val builder = AlertDialog.Builder(context!!)
                        builder.setTitle("Take Action on this chat")

                        builder.setItems(options) { dialog, item ->
                            if (options[item] == "option 1") {

                            } else if (options[item] == "option 2") {

                            } else if (options[item] == "option 3") {

                            }

                        }
                        builder.show()
                    }
                })
        )*/

    }

    override fun onChatitemSelect(chatItem: ChatItem, checkview: ImageView, itemview: View, islongpress: Boolean) {
        if (!islongpress) {
            chatListAdapter!!.setLongpress(true)
            itemview.setBackgroundResource(R.color.light_gray_selection)
            checkview.visibility = View.VISIBLE
            chatItem.isSelected = true
            selectedChat.add(chatItem)
            actionmode = currentActivity.startSupportActionMode(actionModeCallback)
            actionmode!!.title = selectedChat.size.toString()
            actionmode!!.invalidate()
        } else {
            if (chatItem.isSelected) {
                checkview.visibility = View.INVISIBLE
                val outValue = TypedValue()
                context?.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                itemview.setBackgroundResource(outValue.resourceId)
                chatItem.isSelected = false
                selectedChat.remove(chatItem)
                if (selectedChat.isEmpty()) {
                    chatListAdapter!!.setLongpress(false)
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
        val intent = Intent(context, ChatBrowsingActivity::class.java)

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

    override fun onArchiveitemclick() {
        startActivity(Intent(activity,ArchiveActivity::class.java))
    }
    /*override fun onChatitemLongclick() {
        val options: Array<CharSequence> = arrayOf("option 1", "option 2", "option 3")
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("Take Action on this chat")

        builder.setItems(options) { dialog, item ->
            if (options[item] == "option 1") {

            } else if (options[item] == "option 2") {

            } else if (options[item] == "option 3") {

            }

        }
        builder.show()
    }*/

    override fun onResume() {
        super.onResume()
        isvisible = true
        if(chatListAdapter != null){
            chatListAdapter!!.updateChatlist()
        }
        if(archiveCountAdapter != null){
            archiveCountAdapter!!.updateCount()
        }
       /*chatViewModel.registerNewContact()
        if (Blackbox.chatItems.value?.size!! == 0) {
          chatViewModel.updateData()
        }*/

    }

    fun dismissBar() {
        selectedChat.clear()
        if (chatListAdapter != null) {
            chatListAdapter!!.setLongpress(false)
        }
        if (!chatlist.isNullOrEmpty()) {
            chatlist.forEach {
                it.isSelected = false
            }
            chatListAdapter!!.notifyDataSetChanged()
            /*chatListAdapter = ChatListAdapter(context!!, chatlist, this)
            contactAdapter = ConcatAdapter(chatListAdapter,archiveCountAdapter)
            rv_contact.adapter = contactAdapter*/

        }

        if (actionmode != null) {
            actionmode!!.finish()
            actionmode = null
        }

    }

}

interface RecyclerViewReadyCallback {
    fun onLayoutReady()
}