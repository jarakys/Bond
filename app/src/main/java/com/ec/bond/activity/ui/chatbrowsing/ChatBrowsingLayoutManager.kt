package com.ec.bond.activity.ui.chatbrowsing

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.ec.bond.blackbox.model.ChatStatusType

class ChatBrowsingLayoutManager(context: Context,val chatBrowsingViewModel: ChatBrowsingViewModel): LinearLayoutManager(context) {
    var currentPos: Int = -1
    var cdTime: Long = 1000
    var timer = object: CountDownTimer(cdTime, 1000) {
        override fun onTick(millisUntilFinished: Long) {

        }
        override fun onFinish() {
            chatBrowsingViewModel.changeDateAppear(false)
        }
    }
    override fun scrollVerticallyBy(dy: Int, recycler: Recycler?, state: RecyclerView.State?): Int {
        timerAppear()
        val result = super.scrollVerticallyBy(dy, recycler, state)
            chatBrowsingViewModel.showBottomScrollBtn(findFirstVisibleItemPosition() > 2)

        if (chatBrowsingViewModel.chatTypeRef.messages.size > 0 && (chatBrowsingViewModel.chatTypeRef.chatStatusType != ChatStatusType.FinishPrev && chatBrowsingViewModel.chatTypeRef.chatStatusType != ChatStatusType.GettingNewData) && findLastVisibleItemPosition() >= chatBrowsingViewModel.chatTypeRef.messages.size - 45) {
            chatBrowsingViewModel.chatTypeRef.chatStatusType = ChatStatusType.GettingNewData

            chatBrowsingViewModel.retry_msg_data(null, (chatBrowsingViewModel.chatTypeRef.messages.filterIsInstance<MessageItem>().lastOrNull()?.message?.ID?.toInt()?.minus(1)).toString() ,80,"")
        }
        if (findLastVisibleItemPosition() + 1>= chatBrowsingViewModel.chatTypeRef.messages.size){
            chatBrowsingViewModel.changeDateAppear(false)
            return result
        }
            var y = chatBrowsingViewModel.chatTypeRef.datePositionList.reversed().sortedByDescending { it.position }.zipWithNext()
        if (y.isNotEmpty()) {
            if (y[0].first.position <= findLastVisibleItemPosition()) {
                chatBrowsingViewModel.changeDateText(y[0].first.date)
                chatBrowsingViewModel.changeDateAppear(false)
                currentPos = -1
                checkPosition()
                return result
            }

            y.forEach {
                if (it.first.position >= findLastVisibleItemPosition() && it.second.position <= findLastVisibleItemPosition()) {
                    chatBrowsingViewModel.changeDateText(it.second.date)
                    currentPos = it.second.position
                    checkPosition()
                    return result
                }
            }
        }

        return result
    }
    fun timerAppear() {
        Handler(Looper.getMainLooper()).post {
            cdTime += 1000


            timer.cancel()
            timer.start()
        }
    }
    fun checkPosition() {
        if (currentPos > 0){
            if (currentPos in findFirstVisibleItemPosition()..findLastVisibleItemPosition()){
                chatBrowsingViewModel.changeDateAppear(false)
            } else {
                chatBrowsingViewModel.changeDateAppear(true)
            }
        }
    }
    override fun scrollToPosition(position: Int) {
        super.scrollToPosition(position)
        chatBrowsingViewModel.showBottomScrollBtn(false)
    }
}