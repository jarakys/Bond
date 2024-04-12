package com.ec.bond.model

import java.util.*

data class ChatListMesssages(var msgid: String,
                             var sender: String,
                             var recipient: String,
                             var msgtype: String,
                             var msgbody: String,
                             var repliedto: String,
                             var repliedtotxt: String,
                             var groupid: String,
                             var dtsent: Date,
                             var dtreceived: String,
                             var dtread: String,
                             val forwarded: String,
                             var dtdeleted: String,
                             var autodelete: String,
                             var filename: String,
                             var originfilename: String,
                             var localfilename: String)
{
}