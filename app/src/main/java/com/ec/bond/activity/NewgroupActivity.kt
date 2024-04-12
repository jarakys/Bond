package com.ec.bond.activity

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.ec.bond.R
import com.ec.bond.adapter.ContactSelectedAdapter
import com.ec.bond.adapter.GrouplistAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBContactGroup
import com.ec.bond.blackbox.model.BBGroup
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_newgroup.*
import java.util.stream.Collectors


@Parcelize
data class Contact(
        var contact: BBContact,
        var image: String?,
        var registerno: String?
) : Parcelable

class NewgroupActivity : BaseActivity(), GrouplistAdapter.GroupAdapterListener, ContactSelectedAdapter.ContactSelectedListener {
    lateinit var pwdconfg_latest: String
    var adapter: GrouplistAdapter? = null
    var memberadapter: ContactSelectedAdapter? = null
    val _contacts_selected = MutableLiveData<Int>()
    val contacts: ArrayList<BBContactGroup> = arrayListOf()
    val memberlist: ArrayList<BBContactGroup> = arrayListOf()
    var group: BBGroup? = null

    /*@Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    val contactListViewModel: ContactListViewModel by viewModels {
        viewModelFactory
    }*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newgroup)

        val ab: Toolbar = findViewById(R.id.tb_toolbarsearch)
        setSupportActionBar(ab)
        val actionBar: ActionBar? = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        //this is when we add participants into chat list from chat browsing detail screen
        val groupId = intent?.extras?.getString("groupId", "")
        if (!groupId.isNullOrEmpty()) {
            group = Blackbox.chatItems.value?.map { it.group }?.firstOrNull { it?.ID == groupId }
            actionBar.title = "Add Participants"
            ButtonSend.visibility = View.GONE
        } else {
            actionBar.title = "New group"
        }

        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        val mLayoutManagerHori: RecyclerView.LayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv_member_list.layoutManager = mLayoutManagerHori
        rv_member_list.itemAnimator = DefaultItemAnimator()


        mLayoutManager.isAutoMeasureEnabled = true
        rv_contact_list.layoutManager = mLayoutManager
        rv_contact_list.itemAnimator = DefaultItemAnimator()


        ButtonSend.visibility = View.GONE

        Blackbox.pwdConf?.let {
            pwdconfg_latest = it
        }

        memberadapter = ContactSelectedAdapter(this, memberlist, this)
        rv_member_list.adapter = memberadapter

        _contacts_selected.observe(this, Observer {
            if (it <= 0) {
                if (group != null) {
                    add_participants.visibility = View.GONE
                } else {
                    ButtonSend.visibility = View.GONE
                }
            } else {
                if (group != null) {
                    add_participants.visibility = View.VISIBLE
                } else {
                    ButtonSend.visibility = View.VISIBLE
                }
            }
        })

        ButtonSend.setOnClickListener {
            val contactlist = arrayListOf<Contact>()
            memberadapter!!.memberlist().forEach {
                val contact = Contact(it.contact, it.contact.getChatImagePath(), it.contact.registeredNumber)
                contactlist.add(contact)
            }
            finish()
            val intent = Intent(this@NewgroupActivity, CreateGroupActivity::class.java)
            intent.putParcelableArrayListExtra("list", contactlist)
            startActivity(intent)
        }

        add_participants.setOnClickListener {
            val contactlist = arrayListOf<BBContact>()
            memberadapter!!.memberlist().forEach {
                contactlist.add(it.contact)
            }
            val intent = Intent(this@NewgroupActivity, ChatBrowsingDetailActivity::class.java)
            intent.putParcelableArrayListExtra("bbContactList", contactlist)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        Blackbox.contacts.observe(this, Observer {
            contacts.addAll(it.values.map { BBContactGroup(it, false) })
            group?.let { bbGroup ->
                val contactIDValues = bbGroup.members.value?.stream()?.map(BBContact::ID)?.collect(Collectors.toSet())
                val data = contacts.stream().filter { bbContact -> !contactIDValues?.contains(bbContact.contact.ID)!! }.collect(Collectors.toList())
                contacts.clear()
                contacts.addAll(data)
            }
            progressBar.visibility = View.GONE
            if (contacts.isEmpty()) {
                Toast.makeText(this, getString(R.string.you_have_ho_contacts_yet), Toast.LENGTH_LONG).show()
            } else {
                // maximum 250 MEMBERS added into chat list
                val maxNoOfContactSelected = if (group == null) {
                    250
                } else {
                    250 - (group?.members?.value?.size ?: 0)
                }
                adapter = GrouplistAdapter(this, contacts, this, true, maxNoOfContactSelected)
                rv_contact_list.adapter = adapter
                adapter?.notifyDataSetChanged()
            }
        })

    }

    override fun onContactSelected(contact: BBContactGroup?, index: Int) {
        contacts.forEach {
            if (contact!! == it) {
                adapter!!.noOfContactSelected--
                it.isselected = false
                adapter?.notifyDataSetChanged()
            }
        }

        memberadapter!!.removeat(index)

        val changeBounds = ChangeBounds()
        changeBounds.startDelay = 3L
        changeBounds.excludeChildren(rv_contact_list, true)

        if (memberadapter!!.memberlist().size > 0) {
            root.post {
                TransitionManager.beginDelayedTransition(root, changeBounds)
                rv_member_list.visibility = View.VISIBLE
            }
        } else {
            TransitionManager.beginDelayedTransition(root, changeBounds)
            rv_member_list.visibility = View.GONE
        }

        _contacts_selected.postValue(memberadapter!!.memberlist().size)

    }

    override fun onGroupSelected(contact: BBContactGroup?, position: Int) {
        if (contact!!.isselected) {
            contact.contact.imagePath = contact.contact.getChatImagePath()
            memberadapter!!.add(contact)
            adapter!!.noOfContactSelected++
        } else {
            memberadapter!!.remove(contact)
            adapter!!.noOfContactSelected--
        }

        val changeBounds = ChangeBounds()
        changeBounds.startDelay = 3L
        changeBounds.excludeChildren(rv_contact_list, true)

        if (memberadapter!!.memberlist().size > 0) {
            root.post {
                TransitionManager.beginDelayedTransition(root, changeBounds)
                rv_member_list.visibility = View.VISIBLE
            }
        } else {
            TransitionManager.beginDelayedTransition(root, changeBounds)
            rv_member_list.visibility = View.GONE
        }
        _contacts_selected.postValue(memberadapter!!.memberlist().size)
        adapter!!.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!.clear()

        menuInflater.inflate(R.menu.menu_group, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search)
                .actionView as SearchView
        searchView.setSearchableInfo(
                searchManager
                        .getSearchableInfo(componentName)
        )
        searchView.queryHint = "Search here..."
        searchView.maxWidth = Int.MAX_VALUE

        //searchView.background = getDrawable(context!!, R.drawable.search_bg)

        searchView.setOnCloseListener {
            searchView.onActionViewCollapsed()
            adapter?.notifyDataSetChanged()
            return@setOnCloseListener true
        }
        searchView.setOnQueryTextListener(object :
                SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean { // filter recycler view when query submitted
                adapter?.filter?.filter(query)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean { // filter recycler view when text is changed
                adapter?.filter?.filter(query)
                return false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}