package com.ec.bond.activity.ui.home

//import com.spe2eeapp.masmak.activity.ChatListActivity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import com.ec.bond.R
import com.ec.bond.activity.ConferenceGroupSelectionActivity
import com.ec.bond.activity.ContactActivity
import com.ec.bond.activity.SettingsActivity
import com.ec.bond.activity.StarredActivity
import com.ec.bond.activity.ui.calls.CallsFragment
import com.ec.bond.activity.ui.chat.ChatFragment
import com.ec.bond.activity.ui.contactlist.ContactListFragment
import com.ec.bond.di.Injectable
import com.ec.bond.utils.CommonUtils
import javax.inject.Inject

class HomeFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true);

        //val TAB_TITLES = mapOf(0 to "contacts", 1 to "chats", 2 to "calls")
        val TAB_TITLES = listOf<String>("contacts", "chats", "calls")

        val activity: AppCompatActivity = activity as AppCompatActivity
        val fragmentView = requireNotNull(view) { "View should not be null when calling onActivityCreated" }

        val toolbar: Toolbar = fragmentView.findViewById(R.id.toolbar)
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar!!.title = "Bond"
        val floatingActionButton: FloatingActionButton = fragmentView.findViewById(R.id.floating_action_button)

        val tabLayout: TabLayout = fragmentView.findViewById(R.id.tabs)
        val chatTab: TabLayout.Tab = tabLayout.getTabAt(1)!!

        val viewPager: ViewPager2 = fragmentView.findViewById(R.id.view_pager)
        viewPager.adapter = TabsAdapter(childFragmentManager, lifecycle)

        // connect the tabs and view pager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_TITLES[position]
            viewPager.setCurrentItem(tab.position, true)
        }.attach()
        tabLayout.selectTab(chatTab)
        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 2) {
                    floatingActionButton.setImageResource(R.drawable.ic_baseline_add_ic_conference_call_24)
                    floatingActionButton.visibility = View.VISIBLE
                } else if(tab.position==1) {
                    floatingActionButton.setImageResource(R.drawable.ic_message_black_24dp)
                    floatingActionButton.visibility = View.VISIBLE
                }else{
                    floatingActionButton.setImageResource(R.drawable.ic_message_black_24dp)
                    floatingActionButton.visibility = View.GONE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                CallsFragment.removeBar()
                ChatFragment.removeBar()
            }
        })

        // handle tint for the camera icon
        val colors = resources.getColorStateList(R.color.tab_icon, activity.theme)

        for (i in 0 until tabLayout.tabCount) {
            val tab: TabLayout.Tab = tabLayout.getTabAt(i)!!
            var icon = tab.icon
            if (icon != null) {
                icon = DrawableCompat.wrap(icon)
                DrawableCompat.setTintList(icon, colors)
            }
        }

        floatingActionButton.visibility = View.VISIBLE
        floatingActionButton.setOnClickListener {
            if (tabLayout.selectedTabPosition == 2) {
                startActivity(Intent(context, ConferenceGroupSelectionActivity::class.java))
            } else {
                //startActivity(Intent(context, ContactListActivityNew::class.java))
                startActivity(Intent(context, ContactActivity::class.java))
            }
            //startActivity(Intent(context, ChatListActivity::class.java))
        }

        // TEST
//        lifecycleScope.launch {
//            val number = "9660006011"
//            val _contact = BBContact()
//            _contact.registeredNumber = number
//            _contact.phonesjson = listOf(BBPhoneNumber("mobile", number, ""))
//            _contact.phonejsonreg = listOf(BBPhoneNumber("mobile", number, ""))
//            _contact.name = "Saahra Android Nuovo"
//
//            if (Blackbox.addContactAsync(_contact))
//                Log.d("Add Contact", "Success")
//            else
//                Log.d("Add Contact", "Failed")
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        val searchMenuitem = menu.findItem(R.id.action_search)
        val searchManager =
                activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search)
                .actionView as SearchView
        searchView.setSearchableInfo(
                searchManager
                        .getSearchableInfo(activity!!.componentName)
        )
        searchView.queryHint = "Search here..."
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                CommonUtils.filterText.postValue(newText!!)
                return true
            }

        })
        searchView.setOnCloseListener {
            CommonUtils.filterText.postValue("")
            return@setOnCloseListener true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            val settingIntent = Intent(this.context, SettingsActivity::class.java)
            startActivity(settingIntent)
        }
        if (id == R.id.action_starredmsg) {

            val settingIntent = Intent(this.context, StarredActivity::class.java)
            startActivity(settingIntent)

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.getAccountConfig()
    }
}


class TabsAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                ContactListFragment()
            }
            1 -> {
                ChatFragment()
            }
            else -> {
                CallsFragment()
            }
        }
    }

}
