package com.ec.bond.activity.ui.contactlistselect

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.activity.IOnBackPressed
import com.ec.bond.adapter.ChatListAdapter
import com.ec.bond.di.Injectable
import com.ec.bond.model.ChatList
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.RecyclerItemClickListener
import kotlinx.android.synthetic.main.fragment_select_list_contact.*
import javax.inject.Inject

class ContactListSelectFragment : Fragment(), Injectable, IOnBackPressed {

    lateinit var  pwdconfg_latest: String
    lateinit var root: View
    var contact_data: ArrayList<ChatList> = ArrayList()
    var adapter: ChatListAdapter? = null
    lateinit  var rv_contact: RecyclerView
    private val args: ContactListSelectFragmentArgs by navArgs()
    var recipients: String = ""

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    val contactListSelectViewModel: ContactListSelectViewModel by viewModels {
        viewModelFactory
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_cl)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_select_list_contact, container, false) as View



        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()

        contactListSelectViewModel.retry()

        contactListSelectViewModel.retringDone.observe(viewLifecycleOwner, Observer<Boolean> { value ->
            if (value) {
                progressBar.visibility = View.GONE
            }
        })

        contactListSelectViewModel.contact_data.observe(viewLifecycleOwner, Observer<ArrayList<ChatList>> { value ->
            if (value.isEmpty()){
                Toast.makeText(context!!, getString(R.string.you_have_ho_chat_lists_yet), Toast.LENGTH_LONG).show()
            } else {
                contact_data = value
                //TODO need to change it to contact list adapterOld
//                rv_contact.adapterOld = ChatListAdapter(context!!, value)
                (rv_contact.adapter as ChatListAdapter).notifyDataSetChanged()

            }
        })

    }

    private fun initUI() {

        rv_contact = root!!.findViewById(R.id.rv_contact_list) as RecyclerView
        val mLayoutManager: RecyclerView.LayoutManager =
                LinearLayoutManager(context)

        mLayoutManager.setAutoMeasureEnabled(true)
        rv_contact!!.setLayoutManager(mLayoutManager)
        rv_contact!!.setItemAnimator(DefaultItemAnimator())


        //TODO need to change it to contact list adapterOld
//        adapterOld = ChatListAdapter(context!!, contact_data)
        rv_contact!!.adapter = adapter




        rv_contact.addOnItemTouchListener(
                RecyclerItemClickListener(context, rv_contact, object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {

                        contact_data.get(position).selected = !contact_data.get(position).selected
                        //TODO need to change it to contact list adapterOld
//                        adapterOld = ChatListAdapter(context!!, contact_data)
                        rv_contact!!.adapter = adapter

                        updateBottomBar()

                        /*
                        if (data.recipient.equals(data.mobilenumber))
                            recipients = data.sender
                        else
                            recipients = data.recipient

                        contactListSelectViewModel.send_file(args.path, args.caption, recipients)
                        */

                    }

                    override fun onLongItemClick(view: View?, position: Int) {

                    }
                })
        )

        val ab: Toolbar = root.findViewById(R.id.tb_toolbarsearch)
        (activity as AppCompatActivity).setSupportActionBar(ab)
        val actionBar: ActionBar? = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.setTitle("Contact List")
        setHasOptionsMenu(true)

        contactListSelectViewModel.msg_sent.observe(viewLifecycleOwner, Observer<String> { value ->

            if (value.isNotEmpty()) {
                CommonUtils.recipient = value;
                navController.previousBackStackEntry?.savedStateHandle?.set("keyImage", "sd")
                navController.popBackStack()
            }
        })

        floatingButtonSend.setOnClickListener {
            val arrayRec = ArrayList<String>()
            var recipient = ""
            contact_data.forEach { data ->
                if (data.selected) {
                    if (data.recipient.equals(data.mobilenumber)) {
                        recipient = data.sender
                    } else {
                        recipient = data.recipient
                    }
                    arrayRec.add(recipient)
                }
            }
            if (arrayRec.isNotEmpty())
                contactListSelectViewModel.send_file(args.path, args.caption, arrayRec)
        }

        updateBottomBar()

    }

    private fun updateBottomBar() {
        var selected = ""
        var recipient = ""
        contact_data.forEach {
            if (it.selected) {
                if (it.recipient.equals(it.mobilenumber))
                    recipient = it.sender
                else
                    recipient = it.recipient
                if (selected.isEmpty()) {
                    selected = recipient
                } else {
                    selected = selected + ", " + recipient
                }
            }
        }

        if (selected.isNotEmpty()){
            bottomAppBar.visibility = View.VISIBLE
            floatingButtonSend.visibility = View.VISIBLE
            textViewNames.setText(selected)
        } else {
            bottomAppBar.visibility = View.GONE
            floatingButtonSend.visibility = View.GONE
        }
    }


}
