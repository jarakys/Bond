package com.ec.bond.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

object SharePreferenceUtility {
    const val PREFTYPE_BOOLEAN = 0
    const val PREFTYPE_INT = 1
    const val PREFTYPE_STRING = 2
    const val PREFTYPE_LONG = 3
    private val TAG = SharePreferenceUtility::class.java.name

    fun saveStringPreferences(
        context: Context?,
        strKey: String?,
        strValue: String?
    ) {
        try {
            if (context != null) {
                strKey?.let { Log.d(TAG, it) }
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.putString(strKey, strValue)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveIntPreferences(
        context: Context?,
        strKey: String?,
        intValue: Int
    ) {
        try {
            if (context != null) {
                strKey?.let { Log.d(TAG, it) }
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.putInt(strKey, intValue)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun saveBooleanPreferences(
        context: Context?,
        strKey: String?,
        boolValue: Boolean
    ) {
        try {
            if (context != null) {
                strKey?.let { Log.d(TAG, it) }
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.putBoolean(strKey, boolValue)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveLongPreferences(
        context: Context?,
        strKey: String?,
        ll_steps: Long
    ) {
        try {
            if (context != null) {
                strKey?.let { Log.d(TAG, it) }
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.putLong(strKey, ll_steps)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPreferences(
        context: Context?,
        key: String?,
        preferenceDataType: Int
    ): Any? {
        var value: Any? = null
        val sharedPreferences: SharedPreferences
        try {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            when (preferenceDataType) {
                PREFTYPE_BOOLEAN -> value =
                    sharedPreferences.getBoolean(key, false)
                PREFTYPE_INT -> value =
                    sharedPreferences.getInt(key, 0)
                PREFTYPE_STRING -> value =
                    sharedPreferences.getString(key, "")
                PREFTYPE_LONG -> value =
                    sharedPreferences.getLong(key, 0L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            when (preferenceDataType) {
                PREFTYPE_BOOLEAN -> value = false
                PREFTYPE_INT -> value = 0
                PREFTYPE_STRING -> value = ""
                PREFTYPE_LONG -> value = 0L
            }
        }
        return value
    }

    fun getPreferences1(
        context: Context?,
        key: String?,
        preferenceDataType: Int
    ): Any? {
        var value: Any? = null
        val sharedPreferences: SharedPreferences
        try {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            when (preferenceDataType) {
                PREFTYPE_BOOLEAN -> value =
                    sharedPreferences.getBoolean(key, true)
                PREFTYPE_INT -> value =
                    sharedPreferences.getInt(key, 0)
                PREFTYPE_STRING -> value =
                    sharedPreferences.getString(key, "")
                PREFTYPE_LONG -> value =
                    sharedPreferences.getLong(key, 0L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            when (preferenceDataType) {
                PREFTYPE_BOOLEAN -> value = false
                PREFTYPE_INT -> value = 0
                PREFTYPE_STRING -> value = ""
                PREFTYPE_LONG -> value = 0L
            }
        }
        return value
    }

    fun removeStringPreferences(
        context: Context?,
        strKey: String?
    ) {
        try {
            if (context != null) {
                strKey?.let { Log.d(TAG, it) }
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.remove(strKey)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeIntPreferences(
        context: Context?,
        strKey: String?
    ) {
        try {
            if (context != null) {
                strKey?.let { Log.d(TAG, it) }
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.remove(strKey)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeLongPreferences(
        context: Context?,
        strKey: String?
    ) {
        try {
            if (context != null) {
                strKey?.let { Log.d(TAG, it) }
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.remove(strKey)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeBooleanPreferences(
        context: Context?,
        strKey: String?
    ) {
        try {
            if (context != null) {
                strKey?.let { Log.d(TAG, it) }
                val sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.remove(strKey)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}