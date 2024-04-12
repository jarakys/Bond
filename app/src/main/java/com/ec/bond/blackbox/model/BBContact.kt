package com.ec.bond.blackbox.model

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ec.bond.blackbox.Blackbox
import com.ec.bond.blackbox.model.api_responses.GeneralResponse
import com.ec.bond.blackbox.model.api_responses.ProfileInfoJsonDeserializer
import com.ec.bond.blackbox.model.api_responses.ProfileInfoResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

@Parcelize
data class BBPhoneNumber(var tag: String? = null, var phone: String, var prefix: String? = null) :
    Parcelable

@Parcelize
data class BBContact(
    @SerializedName("id") var ID: String = "",
    var registeredNumber: String = "",
    var prefix: String = "",
    var name: String = "",
    @SerializedName("middlename") var middleName: String = "",
    var surname: String = "",
    var suffix: String = "",
    var nickname: String = "",
    @SerializedName("maidenname") var maidenName: String = "",
    @SerializedName("phoneticname") var phoneticName: String = "",
    @SerializedName("phoneticmiddlename") var phoneticMiddleName: String = "",
    @SerializedName("phoneticsurname") var phoneticSurname: String = "",
    var phonesjson: List<BBPhoneNumber> = emptyList(),
    var phonejsonreg: List<BBPhoneNumber> = emptyList(),
    var birthday: String = "",
    var image: Int? = null,
    var imagePath: String? = null,
    var contactPosition: Int? = null,
    var callStatus: String? = null,
    var note: String = "",
    var statusMessage: String = ""
) : BBChat(), Parcelable {

    fun getContactName(): String {
        Blackbox.account.registeredNumber?.let {
            if (it == name) {
                return "You"
            }
        }
        if (name.isNotBlank()) {
            return name
        }
        if (registeredNumber.isNotBlank()) {
            return registeredNumber
        }
        return ""
    }

    var isSelected: Boolean = false
    var isSavedContact: Boolean = false

    // List of groups/role the contact belongs to
    var groups = HashMap<String, BBGroupRole>()

    // CallInfo
    var callInfo = BBCurrentCallInfo()

    val initials: String
        get() {
            val builder = StringBuilder()

            if (name.isNotBlank()) {
                builder.append(name.substring(0, 1))
            }

            if (surname.isNotBlank()) {
                builder.append(surname.substring(0, 1))
            } else if (phoneticSurname.isNotBlank()) {
                builder.append(surname.substring(0, 1))
            } else if (middleName.isNotBlank()) {
                builder.append(surname.substring(0, 1))
            } else if (phoneticMiddleName.isNotBlank()) {
                builder.append(surname.substring(0, 1))
            } else if (maidenName.isNotBlank()) {
                builder.append(surname.substring(0, 1))
            } else if (suffix.isNotBlank()) {
                builder.append(surname.substring(0, 1))
            } else if (nickname.isNotBlank()) {
                builder.append(surname.substring(0, 1))
            } else {
                if (name.count() > 1) {
                    builder.append(surname.substring(0, 2))
                }
            }

            return builder.toString()
        }

    val contactFullName: String get() = "$name $surname"

    private val _onlineStatus = MutableLiveData(BBStatus.offline)
    val onlineStatus: LiveData<BBStatus> get() = _onlineStatus

    var onlineVisibility: Boolean = false
    var lastSeen: Date? = null


    // region Utility functions


    // endregion


    // region API Calls

    /**
     * Send a text message
     * @param message The message object
     * @return true if success
     */
    suspend fun sendTextMessage(message: Message): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_send_txt_msg(
            registeredNumber,
            message.body,
            message.repliedToMsgId,
            message.repliedToText,
            pwdConf
        )
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            message.deliveredToServer = true
            message.ID = response.msgid ?: ""
            message.setCheckmarkType(CheckmarkType.sent)
            response.autodelete?.let {
                message.setAutoDelete(it == "1")
            }

            Blackbox.updateChatItems(this@BBContact, message)

            return@withContext true
        }
        false
    }

    /**
     * Send and file with the body (if not empty)
     * @param message The message object
     * @return true if success
     */
    suspend fun sendFileMessage(message: Message): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val filePath =
            if (message.originalFilePath != null) message.originalFilePath!! else message.localFileName.value!!
        val jsonString = bb_send_file(
            filePath,
            registeredNumber,
            message.body,
            message.repliedToMsgId,
            message.repliedToText,
            pwdConf
        )
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

            Blackbox.updateChatItems(this@BBContact, message)

            return@withContext true
        }
        false
    }

    /**
     * Send location message to group chat. Location must be set to the message body in this format: body="latitude,longitude".
     * @param message The message object
     * @return true if success
     */
    suspend fun sendLocation(message: Message): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        message.groupID = ID
        val latitude = message.body.split(",")[0]
        val longitude = message.body.split(",")[1]
        val jsonString = bb_send_location(
            registeredNumber,
            latitude,
            longitude,
            message.repliedToMsgId,
            message.repliedToText,
            pwdConf
        )
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            message.deliveredToServer = true
            message.ID = response.msgid ?: ""
            response.autodelete?.let {
                message.setAutoDelete(it == "1")
            }
            message.setCheckmarkType(CheckmarkType.sent)

            Blackbox.updateChatItems(this@BBContact, message)

            return@withContext true
        }
        false
    }

    /**
     * Send the Typing notification
     * @param message The message object
     * @return true if success
     */
    suspend fun sendTyping() = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext
        val jsonString = bb_send_typing(registeredNumber, pwdConf)
        val response = Gson().fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess) {
            Log.d("Typing", "Sent")
        }
    }

    // endregion

    /**
     * Fetch the Contact profile Photo and post the new Path to Observable property *profilePhotoPath*
     *
     * @return true if success
     */
    suspend fun fetchProfileImageAsync(): Boolean = withContext(Dispatchers.IO) {
        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        var jsonString = bb_get_photoprofile_filename(registeredNumber, pwdConf)
        val gson = Gson()
        var response = gson.fromJson(jsonString, GeneralResponse::class.java)
        if (response.isSuccess && response.filename.isNullOrEmpty() == false) {
            val fileName = response.filename ?: return@withContext false
            if (fileName.isNotBlank()) {
                jsonString = bb_get_photo(fileName, pwdConf)
                response = gson.fromJson(jsonString, GeneralResponse::class.java)

                if (response.isSuccess) {
                    response.localFilename?.let { path ->
                        if (getChatImagePath() == null) {
                            setChatImagePath(path)
                        } else if (getChatImagePath() != response.localFilename) {
                            setChatImagePath(path)
                        }
                    }
                    return@withContext true
                }
            }
        }
        false
    }

    /**
     * Update the Contact profile information and update the observable property *onlineStatus*
     */
    suspend fun refreshInfo(): Boolean = withContext(Dispatchers.IO) {
        if (registeredNumber.isBlank())
            return@withContext false

        val pwdConf = Blackbox.pwdConf ?: return@withContext false
        val jsonString = bb_get_profileinfo(registeredNumber, pwdConf)
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(
            ProfileInfoResponse::class.java,
            ProfileInfoJsonDeserializer()
        )
        val gson = gsonBuilder.create()
        val response = gson.fromJson(jsonString, ProfileInfoResponse::class.java)
        if (response.isSuccess) {
            _onlineStatus.postValue(response.onlineStatus)
            onlineVisibility = response.onlineVisibility
            lastSeen = response.lastSeen
            statusMessage = response.statusMessage
            return@withContext true
        }
        false
    }

    // endregion


    // region C EXTERNAL FUNCTIONS

    private external fun bb_get_photoprofile_filename(number: String, pwdconf: String): String
    private external fun bb_send_txt_msg(
        recipient: String,
        body: String,
        replytomsgid: String,
        replybody: String,
        pwdconf: String
    ): String

    private external fun bb_send_file(
        filepath: String,
        recipient: String,
        body: String,
        replytomsgid: String,
        replybody: String,
        pwdconf: String
    ): String

    private external fun bb_send_location(
        recipient: String,
        latitude: String,
        longitude: String,
        replytomsgid: String,
        replybody: String,
        pwdconf: String
    ): String

    private external fun bb_send_typing(recipient: String, pwdconf: String): String
    private external fun bb_get_profileinfo(number: String, pwdconf: String): String

    // endregion

    fun updateStatus(newStatus: BBStatus) {
        _onlineStatus.postValue(newStatus)
    }

}


fun ArrayList<BBContact>.equalTo(contacts: ArrayList<BBContact>): Boolean {
    if (size != contacts.size)
        return false

    var matchCount = 0
    for (i in 0 until size) {
        val lContact = get(i)
        val rContact = contacts[i]
        if (lContact.ID == rContact.ID && lContact.name == rContact.name && lContact.surname == rContact.surname && lContact.registeredNumber == rContact.registeredNumber) {
            matchCount += 1
        }
    }
    return matchCount == size
}