package com.ec.bond.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
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
import com.ec.bond.blackbox.model.BBCall
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBContactGroup
import kotlinx.android.synthetic.main.activity_conference_group.*


class ConferenceGroupSelectionActivity : BaseActivity(), GrouplistAdapter.GroupAdapterListener, ContactSelectedAdapter.ContactSelectedListener {
    var adapter: GrouplistAdapter? = null
    private var memberadapter: ContactSelectedAdapter? = null
    private val _contacts_selected = MutableLiveData<Int>()
    val contacts: ArrayList<BBContactGroup> = arrayListOf()
    private val memberlist: ArrayList<BBContactGroup> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conference_group)
        val ab: Toolbar = findViewById(R.id.tb_toolbarsearch)
        setSupportActionBar(ab)
        val actionBar: ActionBar? = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.title = getString(R.string.new_conference_call)


        val bundle = intent.extras
        val list = bundle?.getSerializable("bbCallMembers") as? ArrayList<BBContact>?

        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        val mLayoutManagerHori: RecyclerView.LayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv_member_list.layoutManager = mLayoutManagerHori
        rv_member_list.itemAnimator = DefaultItemAnimator()


        mLayoutManager.isAutoMeasureEnabled = true
        rv_contact_list.layoutManager = mLayoutManager
        rv_contact_list.itemAnimator = DefaultItemAnimator()

        memberadapter = ContactSelectedAdapter(this, memberlist, this)
        rv_member_list.adapter = memberadapter

        Blackbox.contacts.observe(this, Observer {
            contacts.addAll(it.values.map { BBContactGroup(it, false) })
            progressBar.visibility = View.GONE
            for (bbContactIndex in 0 until (list?.size ?: 0)) {
                for (bbGroupIndex in 0 until contacts.size) {
                    if (contacts[bbGroupIndex].contact.ID == list?.get(bbContactIndex)?.ID) {
                        contacts.removeAt(bbGroupIndex)
                        break
                    }
                }
            }
            if (contacts.isEmpty()) {
                Toast.makeText(this, getString(R.string.you_have_ho_contacts_yet), Toast.LENGTH_LONG).show()
            } else {
                val maxNoOfContactSelected = if (list == null) {
                    4
                } else {
                    4 - list.size
                }
                adapter = GrouplistAdapter(this, contacts, this, selectLimitedContact = true, maxNoOfContactSelected = maxNoOfContactSelected)
                rv_contact_list.adapter = adapter
                adapter?.notifyDataSetChanged()
            }
        })
        makeConferenceCall.setOnClickListener {
            if (memberadapter!!.memberlist().size > 0) {
                val contactList = ArrayList<BBContact>()
                for (index in 0 until memberadapter!!.memberlist().size) {
                    val contact = memberadapter!!.memberlist()[index].contact
                    contact.imagePath = memberadapter!!.memberlist()[index].contact.getChatImagePath()
                    contactList.add(contact)
                }
                //list is null when there is no content in bundle
                if (list == null) {
                    val call = BBCall(isOutgoing = true)
                    call.isConference = true

                    call.addMultipleContacts(contactList)
                    Blackbox.openCallActivity(call, this)
                } else {
                    val intent = Intent(this, VoiceCallActivity::class.java)
                    intent.putExtra("bbCallMembers", contactList)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    setResult(Activity.RESULT_OK, intent)
                }
                finish()
            }
        }
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
                makeConferenceCall.visibility = View.VISIBLE
            }
        } else {
            TransitionManager.beginDelayedTransition(root, changeBounds)
            rv_member_list.visibility = View.GONE
            makeConferenceCall.visibility = View.GONE
        }

        _contacts_selected.postValue(memberadapter!!.memberlist().size)
    }

    override fun onGroupSelected(contact: BBContactGroup?, position: Int) {
        if (contact!!.isselected) {
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
                makeConferenceCall.visibility = View.VISIBLE
            }
        } else {
            TransitionManager.beginDelayedTransition(root, changeBounds)
            rv_member_list.visibility = View.GONE
            makeConferenceCall.visibility = View.GONE
        }
        _contacts_selected.postValue(memberadapter!!.memberlist().size)
        adapter!!.notifyDataSetChanged()
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