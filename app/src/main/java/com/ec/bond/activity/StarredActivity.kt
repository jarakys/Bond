package com.ec.bond.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingListItem
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.activity.ui.chatbrowsing.protocols.IChatBrowsingListener
import com.ec.bond.adapter.ArchivedAdapter
import com.ec.bond.adapter.StarredAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.ChatItem
import com.ec.bond.model.Message
import kotlinx.android.synthetic.main.activity_archive.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StarredActivity : BaseActivity() , IChatBrowsingListener {
    var adapter: StarredAdapter? = null
    var chatlist: MutableList<ChatBrowsingListItem> = mutableListOf<ChatBrowsingListItem>()
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starred2)
        chatBrowsingViewModel = ViewModelProvider(this).get(ChatBrowsingViewModel::class.java)
        var manager =
            LinearLayoutManager(this)
        rv_archive_list.layoutManager=manager
        manager.reverseLayout=true
        rv_archive_list.setHasFixedSize(true)

        val ab: Toolbar = findViewById(R.id.tb_toolbarsearch)
        setSupportActionBar(ab)
        val actionBar: ActionBar? = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.title = "Starred Chat"

        intent?.let {
            if(it?.hasExtra("recipent")){
                chatBrowsingViewModel.getStarredMessages(it?.getStringExtra("recipent")!!)
            }else{
                chatBrowsingViewModel.getStarredMessages("")
            }


        }?: kotlin.run {
            chatBrowsingViewModel.getStarredMessages("")
        }


        Blackbox.starredChatItems.observe(this, Observer {
            chatlist= mutableListOf<ChatBrowsingListItem>()
            chatlist = it
            Log.e("starredddd---",""+chatlist)
            progressBar.visibility = View.GONE
            if (it.isEmpty()) {
                adapter = StarredAdapter(this,chatBrowsingViewModel,chatlist,this)
            } else {
                adapter = StarredAdapter(this,chatBrowsingViewModel,chatlist,this)
            }
            rv_archive_list.adapter = adapter
            adapter?.notifyDataSetChanged()
        })


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showAlertDialog(layout: Int) {

    }

    override fun showBallonDialog(layout: Int, view: View) {

    }

    override fun scrollToSpecificMessage(position: Int) {

    }

    override fun retrieveOldMessagesToGetMessage(msgId: String) {

    }

    override fun setBigImageVisible(
        fromImageView: ImageView,
        isImage: Boolean,
        body: String,
        senderName: String,
        path: String?
    ) {

    }


}