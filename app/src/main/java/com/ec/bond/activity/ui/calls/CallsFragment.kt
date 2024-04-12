package com.ec.bond.activity.ui.calls

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.R
import com.ec.bond.adapter.LastCallsAdapter
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.BBCall
import com.ec.bond.blackbox.model.BBContact
import com.ec.bond.blackbox.model.callsHistory.BBCallDirection
import com.ec.bond.blackbox.model.callsHistory.BBCallHistoryGroup
import com.ec.bond.blackbox.model.callsHistory.BBCallType
import com.ec.bond.utils.CommonUtils
import kotlinx.android.synthetic.main.fragment_calls.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallsFragment : Fragment(), LastCallsAdapter.CallAdapterListener {

    private lateinit var callsViewModel: CallsViewModel
    private var lastVoiceCalls: ArrayList<BBCallHistoryGroup> = arrayListOf()
    private var selectcall: ArrayList<BBCallHistoryGroup> = arrayListOf()
    lateinit var callsRecyclerView: RecyclerView
    lateinit var callProgressBar: ProgressBar

    //    lateinit var menuActionBar: Menu
//    lateinit var menuInflater: MenuInflater
    private var isLongPressed = false
    lateinit var currentActivity: AppCompatActivity
    lateinit var callsAdapter: LastCallsAdapter
    lateinit var toolbar: Toolbar

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
            inflater.inflate(R.menu.menu_call, menu)
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
                R.id.action_deleteCall -> {
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setMessage("Do you want to clear your entire call log?")
                            .setCancelable(false)
                            .setPositiveButton("Yes") { dialog, id ->


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
                else -> false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode?) {
            if(!isvisible){
                dismissBar()
            }else{
                selectcall.forEach {
                    callsAdapter.unSelectitem(it)
                }
                selectcall.clear()
                callsViewModel.setIsLongPressed(false)
            }

        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_calls, container, false)
        callsRecyclerView = root.findViewById(R.id.calls_recyclerView)
        var manager = LinearLayoutManager(context)
        callsRecyclerView.layoutManager = manager
        callsRecyclerView.setHasFixedSize(true)
        callProgressBar = root.call_progressBar
        callProgressBar.visibility = View.VISIBLE
        callsViewModel = ViewModelProvider(this).get(CallsViewModel::class.java)
        Log.e("Call_history----","CreaView")
        /*if (!this::lastVoiceCalls.isInitialized) {
            callsViewModel = ViewModelProvider(this).get(CallsViewModel::class.java)

        }*/
        return root
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        if (!isLongPressed)
            inflater.inflate(R.menu.menu_home, menu)
        else
            inflater.inflate(R.menu.menu_call, menu)
        super.onCreateOptionsMenu(menu, inflater)
        callsViewModel.menuActionBar.postValue(menu)
        callsViewModel.menuInflater.postValue(inflater)
    }*/

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_deleteCall) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Do you want to clear your entire call log?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { dialog, id ->


                    }
                    .setNegativeButton("No") { dialog, id ->
                        // Dismiss the dialog
                        dialog.dismiss()
                    }
            val alert = builder.create()
            alert.show()
        }
        return super.onOptionsItemSelected(item)
    }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*val fragmentView = parentFragment?.view
        toolbar = fragmentView?.findViewById(R.id.toolbar)!!
        currentActivity = activity as AppCompatActivity
        currentActivity.setSupportActionBar(toolbar)
        currentActivity.supportActionBar?.title = "Masmak"*/

        /*callsViewModel.longPressedTitle.observe(viewLifecycleOwner, Observer {
            if(it.toInt() < 1){
                callsAdapter.handleCancelSelection()
            }else{
                currentActivity.supportActionBar?.title = it
            }
//            currentActivity.onBackPressed()
        })*/
        currentActivity = activity as AppCompatActivity
        Log.e("Call_history----","Created")

        Blackbox.callsHistory.observe(viewLifecycleOwner, Observer {
            lastVoiceCalls = it
            val bbCallHistoryGroup = BBCallHistoryGroup(arrayListOf(), BBCallDirection.Incoming, BBCallType.Call, BBContact())
            bbCallHistoryGroup.isLastItem = true
            lastVoiceCalls.add(bbCallHistoryGroup)
            callProgressBar.visibility = View.GONE
            callsAdapter = LastCallsAdapter(this, lastVoiceCalls, this)
            callsRecyclerView.adapter = callsAdapter
            (callsRecyclerView.adapter as LastCallsAdapter).notifyDataSetChanged()

        })

    }

    override fun onResume() {
        super.onResume()
        isvisible = true
        Log.e("Call_history----","Resume")
        lifecycleScope.launch(Dispatchers.Main){
            val fetchCallsHistorySuccess = Blackbox.fetchCallsHistoryAsync()
        }



        /*Blackbox.callsHistory.observe(viewLifecycleOwner, Observer {
            lastVoiceCalls = it
            callProgressBar.visibility = View.GONE
            callsRecyclerView.layoutManager = LinearLayoutManager(context)
            callsAdapter = LastCallsAdapter(this, lastVoiceCalls, this)
            callsRecyclerView.adapter = callsAdapter
            (callsRecyclerView.adapter as LastCallsAdapter).notifyDataSetChanged()

        })*/
        /*callsViewModel.isLongPressed.observe(viewLifecycleOwner, Observer {
            isLongPressed = it
            if (it) {
                callsViewModel.menuActionBar.value?.clear()
                callsViewModel.menuInflater.value?.inflate(R.menu.menu_call, callsViewModel.menuActionBar.value)
                currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                toolbar.setNavigationOnClickListener {
                    callsAdapter.handleCancelSelection()
                }
            } else {
                callsViewModel.menuActionBar.value?.clear()
                callsViewModel.menuInflater.value?.inflate(R.menu.menu_home, callsViewModel.menuActionBar.value)
                currentActivity.supportActionBar?.title = "Masmak"
                currentActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                callsRecyclerView.adapter?.notifyDataSetChanged()
            }

        })*/
        CommonUtils.filterText.observe(viewLifecycleOwner, {
            if (!it.isNullOrEmpty()) {
                (callsRecyclerView.adapter as LastCallsAdapter).filter.filter(it)
            } else {
                (callsRecyclerView.adapter as LastCallsAdapter).filter.filter("")
                (callsRecyclerView.adapter as LastCallsAdapter).notifyDataSetChanged()
            }
        })

    }

    override fun onPause() {
        super.onPause()
        CommonUtils.filterText.removeObservers(viewLifecycleOwner)
    }


    override fun onCallStarted(historyGroup: BBCallHistoryGroup?) {
        val context = context ?: return
        val contact = historyGroup?.contact ?: return
        val call = BBCall(isOutgoing = true)
        call.addContact(contact)
        Blackbox.openCallActivity(call, context)
    }

    override fun onItemclik(position: Int, contact: BBCallHistoryGroup, itemeview: View, checkview: ImageView) {
        if (!callsViewModel.isLongPressed.value!!) {
            callsViewModel.setIsLongPressed(true)
            checkview.visibility = View.VISIBLE
            itemeview.setBackgroundResource(R.color.light_gray_selection)
            contact.isSelected = true
            selectcall.add(contact)
            actionmode = currentActivity.startSupportActionMode(actionModeCallback)
            actionmode!!.title = selectcall.size.toString()
            actionmode!!.invalidate()
        } else {
            if (contact.isSelected) {
                checkview.visibility = View.INVISIBLE
                val outValue = TypedValue()
                context?.theme?.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                itemeview.setBackgroundResource(outValue.resourceId)
                contact.isSelected = false
                selectcall.remove(contact)
                if (selectcall.isEmpty()) {
                    callsViewModel.setIsLongPressed(false)
                    actionmode!!.finish()
                }else{
                    actionmode!!.title = selectcall.size.toString()
                    actionmode!!.invalidate()
                }
            } else {
                checkview.visibility = View.VISIBLE
                itemeview.setBackgroundResource(R.color.light_gray_selection)
                contact.isSelected = true
                selectcall.add(contact)
                actionmode!!.title = selectcall.size.toString()
                actionmode!!.invalidate()

            }

        }
    }

    /**
     * refresh adapter and remove the contextual action mode
     */
    fun dismissBar() {
        selectcall.clear()
        if (this::callsViewModel.isInitialized) {
            callsViewModel.setIsLongPressed(false)
        }
        if (!lastVoiceCalls.isNullOrEmpty()) {
            lastVoiceCalls.forEach {
                it.isSelected = false
            }
            callsAdapter = LastCallsAdapter(this, lastVoiceCalls, this)
            callsRecyclerView.adapter = callsAdapter
        }

        if (actionmode != null) {
            actionmode!!.finish()
            actionmode = null
        }

    }

}