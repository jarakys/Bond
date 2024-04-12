package com.ec.bond.activity.ui.chat

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ec.bond.activity.ui.chatbrowsing.ChatBrowsingViewModel
//import com.spe2eeapp.masmak.activity.ui.chatbrowsing.ChatStatusType

class ChatListLayoutManager(context: Context, val chatBrowsingViewModel: ChatBrowsingViewModel, val chatViewModel: ChatViewModel): LinearLayoutManager(context) {
    var isFinish = false
    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        for (index in 0 until findLastVisibleItemPosition())
        {
//            chatBrowsingViewModel.chatItemType = chatViewModel.chatItems[index]
//            chatBrowsingViewModel.registerChatRefType()
//            if (chatViewModel.chatItems[index].)
            if (chatViewModel.chatItems[index].group == null){
                if (chatViewModel.chatItems[index].contact?.isGetData == false){
                    chatBrowsingViewModel.retry_msg_data(null, null ,80,chatViewModel.chatItems[index].contact?.registeredNumber!!)

                }
            } else {
                if (chatViewModel.chatItems[index].group?.isGetData == false){
                    chatBrowsingViewModel.retry_msg_data(null, null ,80,chatViewModel.chatItems[index].group?.ID!!)
                }
            }
        }
    }
    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val result = super.scrollVerticallyBy(dy, recycler, state)
        if (!isFinish) {
            for (index in findFirstVisibleItemPosition() until findLastVisibleItemPosition()) {
                if (findLastVisibleItemPosition() == chatViewModel.chatItems.size - 1) {
                    isFinish = true
                }
//                chatBrowsingViewModel.chatItemType = chatViewModel.chatItems[index]
//                chatBrowsingViewModel.registerChatRefType()
                if (chatViewModel.chatItems[index].group == null){
                    if (chatViewModel.chatItems[index].contact?.isGetData == false){
                        chatBrowsingViewModel.retry_msg_data(null, null ,80,chatViewModel.chatItems[index].contact?.registeredNumber!!)


                    }
                } else {
                    if (chatViewModel.chatItems[index].group?.isGetData == false){
                        chatBrowsingViewModel.retry_msg_data(null, null ,80,chatViewModel.chatItems[index].group?.ID!!)
                    }
                }
//                chatBrowsingViewModel.retry_msg_data(null, null, 80, chatViewModel.chatItems[index])
            }
        }
        return result
    }


}