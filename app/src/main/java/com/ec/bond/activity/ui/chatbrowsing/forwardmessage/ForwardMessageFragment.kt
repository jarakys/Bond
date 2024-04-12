package com.ec.bond.activity.ui.chatbrowsing.forwardmessage

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.activity.IOnBackPressed
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
import com.ec.bond.adapter.ForwardMessageAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBChat
import kotlinx.android.synthetic.main.fragment_contact_list.*

class ForwardMessageFragment() : Fragment(),IOnBackPressed {
    lateinit var root: View
    lateinit var forwardMessageViewModel: ForwardMessageViewModel
    lateinit var currentActivity: AppCompatActivity
    lateinit var chatBrowsingViewModel: ChatBrowsingViewModel
    private val args: ForwardMessageFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



//        parentFragmentManager.setFragmentResult("request_key") { requestKey: String, bundle: Bundle ->
//            val result = bundle.getString("your_data_key")
//            // do something with the result
//        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_contact_list, container, false) as View
        forwardMessageViewModel = ViewModelProvider(this).get(ForwardMessageViewModel::class.java)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActionBar()
//        getViewModel
//        chatBrowsingViewModel = args.chatBrowsingViewModel
        chatBrowsingViewModel = ViewModelProvider(requireActivity()).get(ChatBrowsingViewModel::class.java)
        tb_toolbarsearch.visibility = View.VISIBLE
        tb_toolbarsearch.title = requireActivity().getString(R.string.forward_to)

        val mLayoutManager: RecyclerView.LayoutManager =
                LinearLayoutManager(context)

        mLayoutManager.setAutoMeasureEnabled(true)
        rv_contact_list!!.setLayoutManager(mLayoutManager)
        Blackbox.contacts.observe(viewLifecycleOwner, Observer {
            progressBar.visibility = View.GONE
            if (it.isEmpty()){
                Toast.makeText(context!!, getString(R.string.you_have_ho_contacts_yet), Toast.LENGTH_LONG).show()
            } else {
                var allContactsList = ArrayList<BBChat>()
                it.values.toCollection(allContactsList)

                allContactsList.addAll(Blackbox.temporaryContacts.values)
                Blackbox.chatItems.value?.forEach { chatItem ->
                    if (chatItem.isGroup) {
                        chatItem.group?.let { group -> allContactsList.add(group) }
                    }
                }

                rv_contact_list.adapter = ForwardMessageAdapter(context!!, ArrayList(allContactsList), forwardMessageViewModel)
                (rv_contact_list.adapter as ForwardMessageAdapter).notifyDataSetChanged()
            }
        })

    }

    private fun setupActionBar() {
//        setHasOptionsMenu(true)
        currentActivity = activity as AppCompatActivity
        currentActivity.setSupportActionBar(tb_toolbarsearch)

        currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        currentActivity.supportActionBar?.setDisplayShowHomeEnabled(true);
        tb_toolbarsearch.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        floatingButtonSend.setOnClickListener {
            chatBrowsingViewModel.sendForwardMessage(args.messages.sortedBy { it.message.ID.toInt() }.toTypedArray(),forwardMessageViewModel.getSelectedContacts())

            NavHostFragment.findNavController(this).navigateUp()

//            findNavController().previousBackStackEntry?.savedStateHandle?.set("isSend", true)
        }
        forwardMessageViewModel.selectionTitle.observe(this, Observer {
            if (forwardMessageViewModel.getSizeSelectedContacts() > 0){
                selectedContacts_coordinatorLayout.visibility = View.VISIBLE
                floatingButtonSend.visibility = View.VISIBLE
                textViewNames.text = it
            } else {
                selectedContacts_coordinatorLayout.visibility = View.GONE
                floatingButtonSend.visibility = View.GONE
            }
        })
    }

    override fun onBackPressed(): Boolean {
        NavHostFragment.findNavController(this).navigateUp()
//        findNavController().previousBackStackEntry?.savedStateHandle?.set("isSend", false)
        chatBrowsingViewModel._isSendForward.value = false
        return true

    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        inflater.inflate(R.menu.menu_search,menu)
//        menu.findItem(R.id.action_settings)?.isVisible = false
//    }
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val id = item.itemId
//        if(id == R.id.action_search) {
//
//        }
//        return super.onOptionsItemSelected(item)
//    }

}