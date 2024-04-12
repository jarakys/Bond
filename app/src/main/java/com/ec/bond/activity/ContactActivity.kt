package com.ec.bond.activity

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.activity.ui.contactlist.ContactListViewModel
import com.ec.bond.adapter.ContactListAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBCall
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBPhoneNumber
import kotlinx.android.synthetic.main.fragment_contact_list.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class ContactActivity : BaseActivity(),ContactListAdapter.ContactsAdapterListener {

    lateinit var  pwdconfg_latest: String
    var adapter: ContactListAdapter? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    val contactListViewModel: ContactListViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val ab : Toolbar = findViewById(R.id.tb_toolbarsearch)
        setSupportActionBar(ab)
        val actionBar: ActionBar? = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.setTitle("Contact List")
        //setHasOptionsMenu(true)

        val mLayoutManager: RecyclerView.LayoutManager =
                LinearLayoutManager(this)

        mLayoutManager.setAutoMeasureEnabled(true)
        rv_contact_list.setLayoutManager(mLayoutManager)
        rv_contact_list.setItemAnimator(DefaultItemAnimator())


        Blackbox.pwdConf?.let {
            pwdconfg_latest = it
        }

        Blackbox.contacts.observe(this, Observer { contacts ->
            progressBar.visibility = View.GONE
            if (contacts.isEmpty()) {
                Toast.makeText(this, getString(R.string.you_have_ho_contacts_yet), Toast.LENGTH_LONG).show()
                val contactListFiltered = ArrayList<BBContact>()
                val bbContact = BBContact()
                bbContact.name = "New group"
                bbContact.image = R.mipmap.ic_group

                val bbContact1 = BBContact()
                bbContact1.name = "New Contact"
                bbContact1.image = R.mipmap.ic_contact

                contactListFiltered.add(bbContact)
                contactListFiltered.add(bbContact1)
                contactListFiltered.addAll(contacts.values.sortedByDescending { it.name })
                rv_contact_list.adapter = ContactListAdapter(this, contactListFiltered,this)
                (rv_contact_list.adapter as ContactListAdapter).notifyDataSetChanged()
            } else {
                val contactListFiltered = ArrayList<BBContact>()
                val bbContact = BBContact()
                bbContact.name = "New group"
                bbContact.image = R.mipmap.ic_group

                val bbContact1 = BBContact()
                bbContact1.name = "New Contact"
                bbContact1.image = R.mipmap.ic_contact

                contactListFiltered.add(bbContact)
                contactListFiltered.add(bbContact1)
                contactListFiltered.addAll(contacts.values.sortedByDescending { it.name })
                rv_contact_list.adapter = ContactListAdapter(this, contactListFiltered,this)
                (rv_contact_list.adapter as ContactListAdapter).notifyDataSetChanged()
            }
        })

        /*rv_contact_list.addOnItemTouchListener(
                RecyclerItemClickListener(this, rv_contact_list, object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {
                        if(position == 0){
                            return
                        }
                        val intent = Intent(this@ContactActivity, ChatListActivity::class.java)

                        val data = Blackbox.contacts.value!!.get(position)
                        val recipient = data.phonesjson.get(0).phone
                        intent.putExtra("recipient", recipient)
                        startActivity(intent)
                    }

                    override fun onLongItemClick(view: View?, position: Int) {

                    }
                })
        )*/

        /*bottomAppBar.visibility = View.GONE
        floatingButtonSend.visibility = View.GONE*/
    }

    override fun onGroupSelected() {
        val intent = Intent(this@ContactActivity, NewgroupActivity::class.java)
        startActivity(intent)
    }

    override fun onAddNewContact() {
        val intent = Intent(this@ContactActivity, AddContactActivity::class.java)
        startActivity(intent)
    }

    override fun onContactSelected(contact: BBContact?) {
        val intent = Intent(this@ContactActivity, ChatBrowsingActivity::class.java)
        val recipient = contact!!.phonesjson.get(0).phone
        intent.putExtra("recipient", recipient)
        startActivity(intent)
    }

    override fun onCallSelected(contact: BBContact) {
        val context = this ?: return
        val call = BBCall(true)
        call.addContact(contact)
        Blackbox.openCallActivity(call, context)
    }

    override fun onVideoCallSelected(contact: BBContact) {
        Log.d("onVideoCallSelected", contact.getContactName())

        // TEST
        lifecycleScope.launch {
            val number = contact.registeredNumber
            val _contact = BBContact()
            _contact.registeredNumber = number
            _contact.phonesjson = listOf(BBPhoneNumber("mobile", number, ""))
            _contact.phonejsonreg = listOf(BBPhoneNumber("mobile", number, ""))
            _contact.name = "Saahra Android Nuovo"

            if (Blackbox.addContactAsync(_contact))
                Log.d("Add Contact", "Success")
            else
                Log.d("Add Contact", "Failed")
        }


        val context = this ?: return
        val call = BBCall(isOutgoing = true, hasVideo = true)
        call.addContact(contact)
        Blackbox.openCallActivity(call, context)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //return super.onCreateOptionsMenu(menu)
        menu!!.clear()

        getMenuInflater().inflate(R.menu.menu_search, menu)
        val item = menu.findItem(R.id.action_search)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search)
                .actionView as SearchView
        searchView.setSearchableInfo(
                searchManager
                        .getSearchableInfo(componentName)
        )
        searchView.queryHint = "Search here..."
        searchView.setMaxWidth(Int.MAX_VALUE)

        //searchView.background = getDrawable(context!!, R.drawable.search_bg)

        searchView.setOnCloseListener {
            searchView.onActionViewCollapsed()
            (rv_contact_list.adapter as ContactListAdapter).notifyDataSetChanged()
            return@setOnCloseListener true
        }
        searchView.setOnQueryTextListener(object :
                SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean { // filter recycler view when query submitted
                (rv_contact_list.adapter as ContactListAdapter).getFilter().filter(query)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean { // filter recycler view when text is changed
                (rv_contact_list.adapter as ContactListAdapter).getFilter().filter(query)
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

    private  fun initCall(number1: String) {
        val _contact = BBContact()
        lifecycleScope.launch {
            val number = number1
            _contact.registeredNumber = number
            _contact.phonesjson = listOf(BBPhoneNumber("mobile", number, ""))
            _contact.phonejsonreg = listOf(BBPhoneNumber("mobile", number, ""))
            _contact.name = number

        }


        val context = this ?: return
        val call = BBCall(isOutgoing = true, hasVideo = false)
        call.addContact(_contact)
        Blackbox.openCallActivity(call, context)

    }


}