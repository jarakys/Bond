package com.ec.bond.blackbox.model

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.getStringOrNull
import com.ec.bond.blackbox.model.api_responses.GeneralResponse
import com.ec.bond.utils.DateStyle
import com.ec.bond.utils.dateString
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

enum class BBGroupRole {
    Normal, Admin, Creator;

    fun toApiString() : String {
        return when (this) {
            Normal -> "normal"
            Admin -> "administrator"
            Creator -> "creator"
        }
    }

    fun getName() : String {
        return when (this) {
            Normal -> "default"
            Admin -> "admin"
            Creator -> "creator"
        }
    }

    fun hasSuperPowers() : Boolean {
        return this == Creator || this == Admin
    }

    companion object {
        fun fromString(string: String) : BBGroupRole {
            return when (string) {
                "administrator" -> Admin
                "creator" -> Creator
                else -> Normal
            }
        }
    }
}
@Parcelize
class BBGroup(var ID: String,
              var desc: String = "",
              var role: BBGroupRole? = BBGroupRole.Normal) : BBChat(), Parcelable {

    var expiryDate: Date? = null

    /**
     * Observable property members
     */
    val members: LiveData<ArrayList<BBContact>> get() = _members
    private val _members = MutableLiveData<ArrayList<BBContact>>().apply {
        // A group can exist only if we are one of his members.
        val defaultContact = BBContact()
        defaultContact.name = "You"
        defaultContact.registeredNumber = Blackbox.account.registeredNumber ?: ""
        postValue(arrayListOf(defaultContact))
    }

    /**
     * Observable property members
     */
    val _description: MutableLiveData<String> by lazy {
        MutableLiveData(desc)
    }
    val description: LiveData<String> get() = _description

    fun getMembers() : ArrayList<BBContact> {
        return members.value ?: arrayListOf()
    }

    /**
     * Return members names separated by a comma.
     */
    fun getMembersName() : String {
        var names = ""
        getMembers().forEachIndexed { index, bbContact ->
            val name = bbContact.getContactName()
            names = if (index == 0) {
                name
            } else {
                "$names, $name"
            }
        }
        return names
    }

    /**
     * Return true if the App Account has admin power
     */
    fun isAccountGroupAdmin() : Boolean {
        return role?.hasSuperPowers() ?: false
//        val accountNumber = Blackbox.account.registeredNumber ?: return false
//        val members = members.value ?: arrayListOf()
//        for (contact in members) {
//            val role = contact.groups[ID] ?: continue
//            if (contact.registeredNumber == accountNumber && role.hasSuperPowers())
//                return true
//        }
//        return false
    }

    // region Send Messages Functions

    /**
     * Send text message to group chat
     *
     * @return true if success
     */
    suspend fun sendTextMessage(message: Message) : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        message.groupID = ID
        val jsonString = bb_send_txt_msg_groupchat(ID, message.body, message.repliedToMsgId, message.repliedToText, pwdConf)

        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            message.deliveredToServer = true
            message.ID = response.msgid ?: ""
            response.autodelete?.let {
                message.setAutoDelete(it == "1")
            }
            message.setCheckmarkType(CheckmarkType.sent)

            Blackbox.updateChatItems(this@BBGroup, message)

            return@withContext true
        }
        false
    }

    /**
     * Send file message to group chat
     *
     * @return true if success
     */
    suspend fun sendFileMessage(message: Message) : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        message.groupID = ID

        val filePath = if (message.originalFilePath != null) message.originalFilePath!! else message.localFileName.value!!
        val jsonString = bb_send_file_groupchat(filePath, ID, message.body, message.repliedToMsgId, message.repliedToText, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)

        if (response.isSuccess) {
            message.originalFilePath = null
            message.deliveredToServer = true
            message.ID = response.msgid ?: ""
            response.filename?.let {
                message.fileName = it
            }
            response.localFilename?.let {
                message._localFileName.postValue(it)
                message.fileSize = File(it).length()
            }
            response.autodelete?.let {
                message.setAutoDelete(it == "1")
            }
            message.setCheckmarkType(CheckmarkType.sent)

            Blackbox.updateChatItems(this@BBGroup, message)

            return@withContext true
        }
        false
    }

    /**
     * Send location message to group chat. Location must be set to the message body in this format: body="latitude,longitude".
     *
     * @return true if success
     */
    suspend fun sendLocation(message: Message) : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        message.groupID = ID

        val latitude = message.body.split(",")[0]
        val longitude = message.body.split(",")[1]
        val jsonString = bb_send_location_groupchat(ID, latitude, longitude, message.repliedToMsgId, message.repliedToText, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            message.deliveredToServer = true
            message.ID = response.msgid ?: ""
            response.autodelete?.let {
                message.setAutoDelete(it == "1")
            }

            message.setCheckmarkType(CheckmarkType.sent)
            Blackbox.updateChatItems(this@BBGroup, message)
            return@withContext true
        }
        false
    }

    /**
     * Send the Typing notification
     */
    suspend fun sendTyping() = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_send_typing_groupchat(ID, pwdConf)
        val gson = Gson()
        val response = gson.fromJson(jsonString, GeneralResponse::class.java)
        Log.d("Typing", "Sent $response.isSuccess")
    }

    // endregion

    // region Utility functions

    fun updateGroupDescription(description: String) {
        _description.postValue(description)
    }

    /**
     * Add the contacts to the group
     * @param contacts Group members to add
     * @return return the list of the successfully added contacts
     */
    suspend fun addMembers(contacts: List<BBContact>) : List<BBContact> = withContext(Dispatchers.IO) {
        if (isAccountGroupAdmin() == false)
            return@withContext listOf()

        val pwdConf = Blackbox.pwdConf ?: return@withContext listOf()
        val  gson = Gson()
        val addedMembers = mutableListOf<BBContact>()
        val members = members.value ?: arrayListOf()

        // make sure it has a valid registered number
        // make sure the contact is not already present
        contacts.filter { contact ->
            contact.registeredNumber.isNotBlank()
                    && members.any { member -> contact.registeredNumber == member.registeredNumber } == false
                    && addedMembers.any { member -> contact.registeredNumber == member.registeredNumber } == false}.forEach { contact ->
            val jsonString = bb_add_member_groupchat(ID, contact.registeredNumber, pwdConf)
            val response = gson.fromJson(jsonString, GeneralResponse::class.java)
            if (response.isSuccess)
                addedMembers.add(contact)
        }

        members.addAll(addedMembers)
        _members.postValue(members)

        addedMembers.toList()
    }

    /**
     * Fetch the members list and update the *members* observable property
     */
    suspend fun refreshMembersListAsync() : Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_get_list_members_groupchat(ID, pwdConf)
        val jsonObject = JSONObject(jsonString)
        jsonObject.getStringOrNull("answer")?.let {
            if (it == "OK") {
                val membersJsonString = jsonObject.getStringOrNull("members") ?: return@withContext false
                val blackbox = Blackbox
                val jsonArray: JSONArray = jsonObject.getJSONArray("members")

                val members = ArrayList<BBContact>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObjectDetail: JSONObject = jsonArray.getJSONObject(i)
                    val memberNumber = jsonObjectDetail.getStringOrNull("mobilenumber") ?: continue
                    // If this is us, just update the group Role.
                    blackbox.account.registeredNumber?.let { num ->
                        if (num == memberNumber) {
                            jsonObjectDetail.getStringOrNull("role")?.let { _role ->
                                role = BBGroupRole.fromString(_role)
                            }
                        }
                    }

                    val role = BBGroupRole.fromString(jsonObjectDetail.getString("role"))

                    var contact = blackbox.getContact(memberNumber)
                            ?: blackbox.getTemporaryContact(memberNumber)
                    if (contact == null) {
                        // We don't know this contact yet. Lets add it to the temporary list
                        contact = BBContact()
                        contact.registeredNumber = memberNumber
                        contact.groups[ID] = role
                        jsonObjectDetail.getStringOrNull("name")?.let { name ->
                            contact.name = name
                        }
                        jsonObjectDetail.getStringOrNull("surname")?.let { surname ->
                            contact.surname = surname
                        }

                        blackbox.addTemporaryContact(contact)
                        members.add(contact)
                    } else {
                        contact.groups[ID] = role
                        members.add(contact)
                    }
                }

                _members.postValue(members)
            }
        }
        false
    }

    /**
     * Set the group Image and delete the original image passed to this function
     * @return true if success
     */
    suspend fun setGroupImage(filePath: String) : Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists() == false) {
            return@withContext false
        }

        Blackbox.pwdConf?.let { pwdConf ->
            val jsonString = bb_update_photo_groupchat(filePath, ID, pwdConf)
            val gson = Gson()
            val response = gson.fromJson(jsonString, GeneralResponse::class.java)

            if (response.isSuccess) {
                response.localFilename?.let { path ->
                    setChatImagePath(path)
                }
                file.delete()
                return@withContext true
            }
            Log.e("setGroupImage", response.message)
        }
        false
    }

    /**
     * Fetch the Contact profile Photo and post the new Path to Observable property *profilePhotoPath*
     *
     * @return true if success
     */
    suspend fun fetchProfileImageAsync(filename: String) : Boolean = withContext(Dispatchers.IO) {
        Blackbox.pwdConf?.let {
            val gson = Gson()
            val jsonString = bb_get_photo(filename, it)
            val response = gson.fromJson(jsonString, GeneralResponse::class.java)

            if (response.isSuccess) {
                response.localFilename?.let { path ->
                    if (getChatImagePath() == null) {
                        setChatImagePath(path)
                    } else if (getChatImagePath().equals(response.localFilename) == false) {
                        setChatImagePath(path)
                    }
                }
                return@withContext true
            }
        }
        false
    }

    /**
     * Set the group description and update the observable property "description"
     * @return true if success
     */
    suspend fun setGroupDescription(description: String) : Boolean = withContext(Dispatchers.IO) {
        if (isAccountGroupAdmin() == false)
            return@withContext false


        val pwdconf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_change_groupchat(ID, description, pwdconf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            updateGroupDescription(description)
            return@withContext true
        }
        false
    }

    /**
     * Delete the group
     * @return True if success
     */
    suspend fun deleteGroup() : Boolean = withContext(Dispatchers.IO) {
        if (isAccountGroupAdmin() == false)
            return@withContext false

        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_delete_groupchat(ID, pwdConf)
        val gson = Gson()
        val response = gson.fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            async { clearChat() }
            Blackbox.removeGroupFromChatItems(this@BBGroup)
        }
        return@withContext response.isSuccess
    }

    /**
     * Remove the contact from the group and update the observable varialble: members
     * @param contact the contact to remove
     * @return true if success
     */
    suspend fun removeMember(contact: BBContact): Boolean = withContext(Dispatchers.IO) {
        if (contact.registeredNumber.isEmpty())
            return@withContext false

        val members = members.value ?: return@withContext false
        if (members.size == 0)
            return@withContext false
        if (members.any { it.registeredNumber == contact.registeredNumber } == false)
            return@withContext false

        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_revoke_member_groupchat(ID, contact.registeredNumber, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            members.removeAll { it.registeredNumber == contact.registeredNumber }
            _members.postValue(members)
            return@withContext true
        }
        Log.e("removeMember", response.message)
        false
    }

    /**
     * Change the role of a contact with the group and update the BBContact.groups hashtable.
     * @return true if success
     */
    suspend fun changeMemberRole(contact: BBContact, role: BBGroupRole) : Boolean = withContext(Dispatchers.IO) {
        if (isAccountGroupAdmin() == false)
            return@withContext false

        if (contact.registeredNumber.isEmpty())
            return@withContext false

        val members = members.value ?: arrayListOf()
        if (members.size == 0)
            return@withContext false
        if (members.any { it.registeredNumber == contact.registeredNumber } == false)
            return@withContext false

        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_change_role_member_groupchat(ID, contact.registeredNumber, role.toApiString(), pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            contact.groups[ID] = role
            return@withContext true
        }
        Log.e("changeMemberRole", response.message)

        false
    }

    /**
     * Exit from the group
     */
    suspend fun exitGroup() : Boolean = withContext(Dispatchers.IO) {
        val members = members.value ?: return@withContext false
        val contact = members.firstOrNull { it.registeredNumber == Blackbox.account.registeredNumber } ?: return@withContext false
        // To exit the group we simply remove ourself from the group
        val result = removeMember(contact)
        if (result) {
            async { clearChat() }
            Blackbox.removeGroupFromChatItems(this@BBGroup)
        }
        result
    }

    suspend fun setExpiryDate(date: Date?) : Boolean = withContext(Dispatchers.IO) {
        if (isAccountGroupAdmin() == false)
            return@withContext false

        val dateString = date?.dateString(DateStyle.default) ?: "0000-00-00 00:00:00"

        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_setexpiringdate_groupchat(ID, dateString, pwdConf)
        val gson = Gson()
        val response = gson.fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            expiryDate = date
        }
        response.isSuccess
    }

    //endregion

    // region C EXTERNAL FUNCTIONS

    private external fun bb_add_member_groupchat(groupid: String, number: String, pwdconf: String) : String
    private external fun bb_delete_groupchat(groupid: String, pwdconf: String) : String
    private external fun bb_change_groupchat(groupid: String, description: String, pwdconf: String) : String

    private external fun bb_send_txt_msg_groupchat(groupid: String, body: String, replytomsgid: String, replybody: String, pwdconf: String) : String
    private external fun bb_send_file_groupchat(filename: String, groupid: String, body: String, replytomsgid: String, replybody: String, pwdconf: String) : String
    private external fun bb_send_location_groupchat(groupid: String, latitude: String, longitude: String, replytomsgid: String, replybody: String, pwdconf: String) : String
    private external fun bb_send_typing_groupchat(groupid: String, pwdconf: String) : String

    private external fun bb_update_photo_groupchat(filepath: String, groupid: String, pwdconf: String) : String

    private external fun bb_get_list_members_groupchat(groupid: String, pwdconf: String) : String
    private external fun bb_revoke_member_groupchat(groupid: String, number: String, pwdconf: String) : String
    private external fun bb_change_role_member_groupchat(groupid: String, number: String, role: String, pwdconf: String) : String
    private external fun bb_setexpiringdate_groupchat(groupid: String, date: String, pwdconf: String) : String


    // endregion
}
