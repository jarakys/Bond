package com.ec.bond.model

data class ChatList(
        var recipient:String,
        var sender:String,
        var mobilenumber:String,
        var msgbody:String,
        var selected:Boolean){
}