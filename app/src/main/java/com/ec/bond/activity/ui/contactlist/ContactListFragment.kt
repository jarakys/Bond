package com.ec.bond.activity.ui.contactlist

//import android.widget.SearchView
import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.onTextChangeListener
import com.simplemobiletools.commons.extensions.performHapticFeedback
import com.ec.bond.R
import com.ec.bond.activity.*
import com.ec.bond.activity.ui.calls.CallsFragment
import com.ec.bond.adapter.ContactAdapter
import com.ec.bond.adapter.LastCallsAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBCall
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.BBPhoneNumber
import com.ec.bond.blackbox.model.callsHistory.BBCallHistoryGroup
import com.ec.bond.di.Injectable
import com.ec.bond.utils.BallonUtils
import com.ec.bond.utils.CommonUtils
import com.ec.bond.utils.addCharacter
import com.ec.bond.utils.getKeyEvent
import com.skydoves.balloon.OnBalloonClickListener
import kotlinx.android.synthetic.main.dialpad.view.*
import kotlinx.android.synthetic.main.fragment_contact_list.*
import kotlinx.android.synthetic.main.fragment_contact_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.ArrayList

class ContactListFragment : Fragment(), Injectable, ContactAdapter.ContactsAdapterListener, View.OnClickListener,
    OnBalloonClickListener {

    lateinit var toolbar: Toolbar
    var dialpad=true;
    private lateinit var dialInput:EditText
    private var selectContact: ArrayList<BBContact> = arrayListOf()
    lateinit var currentActivity: AppCompatActivity
   var deleteContact: BBContact?=null
     var deletedPosition:Int=-1
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    val contactListViewModel: ContactListViewModel by viewModels {
        viewModelFactory
    }

    companion object {
        var actionmode: androidx.appcompat.view.ActionMode? = null
        var isvisible: Boolean = true

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

    private val actionModeCallback: androidx.appcompat.view.ActionMode.Callback = object : androidx.appcompat.view.ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater: MenuInflater = mode!!.menuInflater
            inflater.inflate(R.menu.menu_contact, menu)
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
                R.id.action_delete -> {
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setMessage("Do you want to delete contact?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, id ->
                            deleteContact?.let {
                                contactListViewModel.deleteContact(it)
                                contactListViewModel.setIsLongPressed(false)
                            }



                        }
                        .setNegativeButton("No") { dialog, id ->
                            // Dismiss the dialog
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()
                    mode!!.finish() // Action picked, so close the CAB
                    true
                }

                R.id.action_edit -> {
                    Log.e("action----","edit")
                    deleteContact?.let{
                        val intent = Intent(context, UpdateContactActivity::class.java)
                        intent.putExtra("registeredNumber", it.registeredNumber)
                        startActivity(intent)
                    }
                    true

                }
                else -> false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode?) {
            if(!ContactListFragment.isvisible){
                dismissBar()
            }else{
                deleteContact?.let {
                    it.isSelected=false
                    (rv_contact_list.adapter as ContactAdapter).deselectedContact(it!!)
                }
                contactListViewModel.setIsLongPressed(false)
            }


        }
    }

    fun dismissBar() {


        contactListViewModel.setIsLongPressed(false)
        if (CallsFragment.actionmode != null) {
            CallsFragment.actionmode!!.finish()
            CallsFragment.actionmode = null
            contactListViewModel.setIsLongPressed(false)
        }

    }
    override fun onResume() {
        updateData()
        super.onResume()
    }

    fun updateData() = GlobalScope.launch(Dispatchers.IO) {
        if (Blackbox.fetchContactsAsync())
            Blackbox.fetchChatListAsync()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contact_list, container, false) as View
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mLayoutManager: RecyclerView.LayoutManager =
                LinearLayoutManager(context)
        currentActivity = activity as AppCompatActivity
        rv_contact_list.layoutManager = mLayoutManager
        rv_contact_list.itemAnimator = DefaultItemAnimator()
        rv_contact_list.adapter = ContactAdapter(context!!, arrayListOf(), this,this)
        view.dialpad_contact.setOnClickListener(this)
        dialInput=view.dialpad_input
        initView(view)

        Blackbox.contacts.observe(viewLifecycleOwner, Observer { value ->
            progressBar.visibility = View.GONE
            if (value.isEmpty()) {
                Toast.makeText(context!!, getString(R.string.you_have_ho_contacts_yet), Toast.LENGTH_LONG).show()
                val contactListFiltered = ArrayList<BBContact>()
                val bbContact = BBContact()
                bbContact.name = "New group"
                bbContact.ID = "000000"

                val bbContact1 = BBContact()
                bbContact1.name = "New Contact"
                bbContact1.ID = "0000000"


                contactListFiltered.add(bbContact)
                contactListFiltered.add(bbContact1)
                contactListFiltered.addAll(value.values)
                rv_contact_list.adapter = ContactAdapter(context!!, contactListFiltered, this,this)
                (rv_contact_list.adapter as ContactAdapter).notifyDataSetChanged()
            } else {
                val contactListFiltered = ArrayList<BBContact>()
                val bbContact = BBContact()
                bbContact.name = "New group"
                bbContact.ID = "000000"

                val bbContact1 = BBContact()
                bbContact1.name = "New Contact"
                bbContact1.ID = "0000000"


                contactListFiltered.add(bbContact)
                contactListFiltered.add(bbContact1)
                contactListFiltered.addAll(value.values)
                rv_contact_list.adapter = ContactAdapter(context!!, contactListFiltered, this,this)
                (rv_contact_list.adapter as ContactAdapter).notifyDataSetChanged()
            }
        })
        CommonUtils.filterText.observe(viewLifecycleOwner, Observer {
            if(!it.isNullOrEmpty()){
                (rv_contact_list.adapter as ContactAdapter).filter.filter(it)
            }else{
                (rv_contact_list.adapter as ContactAdapter).filter.filter("")
                (rv_contact_list.adapter as ContactAdapter).notifyDataSetChanged()
            }
        })

        contactListViewModel.contactResult?.observe(viewLifecycleOwner, Observer {
            it?.let {
                if(it){
                    contactListViewModel.clearSelectedCalls()
                    deleteContact?.let {
                        it.isSelected=false
                        (rv_contact_list.adapter as ContactAdapter).deselectedContact(it!!)
                    }
                    deleteContact?.let {
                        (rv_contact_list.adapter as ContactAdapter).deleteContactAt(it)
                    }


                }
            }
        })

    }

    private fun initView(view: View) {
        view.dialpad_wrapper.dialpad_0_holder.setOnClickListener { dialpadPressed('0', view) }
        view.dialpad_wrapper.dialpad_1_holder.setOnClickListener { dialpadPressed('1', view) }
        view.dialpad_wrapper.dialpad_2_holder.setOnClickListener { dialpadPressed('2', view) }
        view.dialpad_wrapper.dialpad_3_holder.setOnClickListener { dialpadPressed('3', view) }
        view.dialpad_wrapper.dialpad_4_holder.setOnClickListener { dialpadPressed('4', view) }
        view.dialpad_wrapper.dialpad_5_holder.setOnClickListener { dialpadPressed('5', view) }
        view.dialpad_wrapper.dialpad_6_holder.setOnClickListener { dialpadPressed('6', view) }
        view.dialpad_wrapper.dialpad_7_holder.setOnClickListener { dialpadPressed('7', view) }
        view.dialpad_wrapper.dialpad_8_holder.setOnClickListener { dialpadPressed('8', view) }
        view.dialpad_wrapper.dialpad_9_holder.setOnClickListener { dialpadPressed('9', view) }

        view.dialpad_wrapper.dialpad_0_holder.setOnLongClickListener { dialpadPressed('+', null); true }
        view.dialpad_wrapper.dialpad_asterisk_holder.setOnClickListener { dialpadPressed('*', it) }
        view.dialpad_wrapper.dialpad_hashtag_holder.setOnClickListener { dialpadPressed('#', it) }
        view.dialpad_clear_char.setOnClickListener { clearChar(it) }
        view.dialpad_clear_char.setOnLongClickListener { clearInput(); true }
        view.dialpad_call_button.setOnClickListener { initCall(dialInput.text.toString()) }
        view.dialpad_input.onTextChangeListener { dialpadValueChanged(it) }
        disableKeyboardPopping()
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


        val context = context ?: return
        val call = BBCall(isOutgoing = true, hasVideo = false)
        call.addContact(_contact)
        Blackbox.openCallActivity(call, context)

    }

    private fun dialpadPressed(char: Char, view: View?) {
        dialInput?.addCharacter(char)
        //dialInput?.setText(char.toInt().toString())
        Log.e("input----",char+"")
        view?.performHapticFeedback()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun dialpadValueChanged(text: String) {
        val len = text.length
    }


    private fun clearChar(view: View) {
        dialInput.dispatchKeyEvent(dialpad_input.getKeyEvent(KeyEvent.KEYCODE_DEL))
        view.performHapticFeedback()
    }

    private fun clearInput() {
        dialInput.setText("")
    }

    private fun disableKeyboardPopping() {
        dialInput.showSoftInputOnFocus = false
    }

    override fun onGroupSelected() {
        val intent = Intent(activity, NewgroupActivity::class.java)
        startActivity(intent)
    }

    override fun onNewContactSelected() {
        val intent = Intent(requireActivity(), AddContactActivity::class.java)
        startActivity(intent)
    }

    override fun onContactSelected(contact: BBContact?,view: View) {
        deleteContact=contact!!
        if (!contactListViewModel.isLongPressed.value!!) {
            contactListViewModel.setIsLongPressed(true)
            contactListViewModel.clearSelectedCalls()
            val intent = Intent(activity, ChatBrowsingActivity::class.java)
            val recipient = contact!!.phonesjson[0].phone
            intent.putExtra("recipient", recipient)
            intent.putExtra("chatItem", contact)
            intent.putExtra("recipientImage", contact.getChatImagePath() ?: "")
            intent.putExtra("isFromChatList", false)
            startActivity(intent)
        }else{
            contact.isSelected=true
            (rv_contact_list.adapter as ContactAdapter).selectedContact(contact)
            contactListViewModel.addToSelectedCalls(contact)
            (rv_contact_list.adapter as ContactAdapter).notifyDataSetChanged()
            contactListViewModel.setIsLongPressed(false)
            openDialog(view);
            /*CallsFragment.actionmode = currentActivity.startSupportActionMode(actionModeCallback)
            CallsFragment.actionmode!!.invalidate()*/
        }

    }

    override fun onCallSelected(contact: BBContact) {
        val context = context ?: return
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


        val context = context ?: return
        val call = BBCall(isOutgoing = true, hasVideo = true)
        call.addContact(contact)
        Blackbox.openCallActivity(call, context)

    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.dialpad_contact -> {
                if(dialpad){
                    dialpad=false
                    //dialpad_input.visibility=View.GONE
                    dialpad_wrapper.visibility=View.GONE
                }else{
                    dialpad=true
                   // dialpad_input.visibility=View.VISIBLE
                    dialpad_wrapper.visibility=View.VISIBLE
                }

            }
        }
    }

    fun openDialog(view: View){
        val ballon= BallonUtils.getNavigationBalloon(requireContext(),this,this)
        ballon.show(view)
        val yes: TextView = ballon.getContentView().findViewById(R.id.deleteTv)
        val cancel: TextView = ballon.getContentView().findViewById(R.id.editTv)
        yes.setOnClickListener {
            val builder = AlertDialog.Builder(requireActivity())
            builder.setMessage("Do you want to delete contact?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    deleteContact?.let {
                        contactListViewModel.deleteContact(it)
                        contactListViewModel.setIsLongPressed(false)
                    }



                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show() // Action picked, so close the CAB
            ballon.dismiss()
        }

        cancel.setOnClickListener{
            deleteContact?.let{
                val intent = Intent(context, UpdateContactActivity::class.java)
                intent.putExtra("registeredNumber", it.registeredNumber)
                startActivity(intent)
            }
            ballon.dismiss()
        }
    }

    override fun onBalloonClick(view: View) {

    }

}
