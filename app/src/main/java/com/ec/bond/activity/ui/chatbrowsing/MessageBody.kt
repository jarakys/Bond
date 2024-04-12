package com.ec.bond.activity.ui.chatbrowsing

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.core.text.*

data class MessageBody(
        var text: String,
        var startPosition: Int = 0,
        var endPosition: Int = 0,
        var decorationTypes: ArrayList<DecorType> = ArrayList(),
        var color: Int? = null
) {
    fun getString(): String {
        var result: SpannableStringBuilder = SpannableStringBuilder()
        for (decor in decorationTypes) {
            when (decor) {
                DecorType.Bold -> {
                    result = result.bold { append(text) }
                }
                DecorType.Italic -> {
                    result = result.italic { append(text) }
                }
                DecorType.Underlined -> {
                    result = result.underline { append(text) }
                }
                DecorType.Strikethrough -> {
                    result = result.strikeThrough { append(text) }
                }
                DecorType.Color -> {
                    result = result.color(color!!) { append(text) }
                }
            }
        }
        return result.toString()
    }
    companion object {
        val listOfTypes = listOf("*","_","~","•", "<color hex=\"")

        fun convertStringToMessageBody(str: String): ArrayList<MessageBody> {

            var messageBodies = ArrayList<MessageBody>()
//            var boldNo =
            checkFirstType(str, messageBodies, 0)
//            var arr = str.split(delimiters = listOfTypes,ignoreCase = true)
            return messageBodies
        }

        private fun checkFirstType(str: String, messageBodies: ArrayList<MessageBody>, cycleNo: Int = 0) {
            val firstPosition = str.indexOfAny(listOfTypes)
            var newMessage = str
            if (firstPosition != -1) {

                newMessage = handleTypeAtPosition(str, firstPosition, messageBodies, cycleNo)
                checkFirstType(newMessage, messageBodies, cycleNo + 1)
            }
        }

        private fun handleTypeAtPosition(str: String, firstPosition: Int, messageBodies: ArrayList<MessageBody>, cycleNo: Int = 0): String {
//            val foundMessage = messageBodies.sortedBy { it.startPosition }.firstOrNull {
////                if (it.endPosition + cycleNo > str.length) {
//                    str.substring(it.startPosition, it.endPosition + cycleNo).contains(it.text)
////                } else {
////                    false
////                }
//            }
//            var messageBody = if (foundMessage != null) {
//                foundMessage
//            } else {
//                MessageBody(str)
//            }
//            messageBodies[0].text = str
            var foundMessage: MessageBody? = null
            var messageBody = MessageBody(str)
            var tempString = str
            for (char in listOfTypes) {
                if (str[firstPosition] == char.toCharArray()[0]){
                    if (char == "<color hex=\"") {
                        if (str.contains(char)) {
                            messageBody.startPosition = firstPosition

                            tempString = tempString.removeRange(firstPosition, firstPosition + char.length)
                            val endPositionOfFirstPart = tempString.indexOf("\">")
                            val colorInString = tempString.substring(firstPosition, endPositionOfFirstPart)
                            messageBody.color = Color.parseColor(colorInString)
                            messageBody.decorationTypes.add(DecorType.Color)
                            tempString = tempString.removeRange(firstPosition, firstPosition + colorInString.length + 2)

                            val lastPosition = tempString.indexOf("</color>")
                            if (lastPosition == -1) {
                                break
                            }
                            messageBody.endPosition = lastPosition - 1
                            tempString = tempString.removeRange(lastPosition, lastPosition + "</color>".length)
                            messageBody.text = tempString.substring(firstPosition, lastPosition)
                            foundMessage = messageBodies.sortedByDescending { it.startPosition }.firstOrNull {
//                        str.substring(it.startPosition, it.endPosition + cycleNo).contains(it.text)
                                it.text.contains(messageBody.text)
                            }
                        }
                    } else {
                        messageBody.startPosition = firstPosition
                        tempString = tempString.removeRange(firstPosition, firstPosition + 1)
                        val lastPosition = tempString.indexOf(char.toCharArray()[0])
                        if (lastPosition == -1) {
                            break
                        }
                        messageBody.endPosition = lastPosition - 1
                        tempString = tempString.removeRange(lastPosition, lastPosition + 1)
                        messageBody.text = tempString.substring(firstPosition, lastPosition)
                        foundMessage = messageBodies.sortedByDescending { it.startPosition }.firstOrNull {
//                        str.substring(it.startPosition, it.endPosition + cycleNo).contains(it.text)
                            it.text.contains(messageBody.text)
                        }
                    }
                    if (foundMessage != null) {

                        foundMessage.apply {
                            this.text = messageBody.text
                            this.startPosition = messageBody.startPosition
                            this.endPosition = messageBody.endPosition
                            this.color = messageBody.color
                            this.decorationTypes = messageBody.decorationTypes
                        }
                        addDecorType(foundMessage, char)
                    } else {
                        addDecorType(messageBody, char)
                    }
                }
            }
            if (foundMessage == null) {
                messageBodies.add(messageBody)
            }
            val fullMsgBody = messageBodies.firstOrNull {
                it.startPosition == -1
            }
            if (fullMsgBody == null) {
                messageBodies.add(0, MessageBody(tempString, -1, -1))
            } else {
                fullMsgBody.text = tempString
            }
            return tempString
        }

        private fun addDecorType(obj: MessageBody, char: String) {
            when (char) {
                "*" -> {
                    obj.decorationTypes.add(DecorType.Bold)
                }
                "_" -> {
                    obj.decorationTypes.add(DecorType.Italic)
                }
                "~" -> {
                    obj.decorationTypes.add(DecorType.Strikethrough)
                }
                "•" -> {
                    obj.decorationTypes.add(DecorType.Underlined)
                }
                else -> {

                }
            }
        }

        fun getFullString(str: String, messageBodies: List<MessageBody>): SpannableString {
//            val result = SpannableStringBuilder()
            var spannableString = SpannableString(str)

            for (msg in messageBodies) {
//                msg.startPosition
                for (type in msg.decorationTypes) {
                    when (type) {
                        DecorType.Bold -> {
                            spannableString.setSpan(StyleSpan(Typeface.BOLD), msg.startPosition, msg.endPosition + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //bold
                        }
                        DecorType.Italic -> {
                            spannableString.setSpan(StyleSpan(Typeface.ITALIC), msg.startPosition, msg.endPosition + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //bold
                        }
                        DecorType.Underlined -> {
                            spannableString.setSpan(UnderlineSpan(), msg.startPosition, msg.endPosition + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //bold
                        }
                        DecorType.Strikethrough -> {
                            spannableString.setSpan(StrikethroughSpan(), msg.startPosition, msg.endPosition + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //bold
                        }
                        DecorType.Color -> {
                            msg.color?.let { color ->

                                spannableString.setSpan(ForegroundColorSpan(color), msg.startPosition, msg.endPosition + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) //bold
                            }
                        }
                    }
                }
            }
            return spannableString
        }
    }
}

data class StringHasColor(var text: SpannableString)

enum class DecorType {
    Bold, Italic, Strikethrough, Color, Underlined
}