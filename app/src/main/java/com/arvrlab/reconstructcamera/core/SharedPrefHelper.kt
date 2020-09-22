package com.arvrlab.reconstructcamera.core

import android.content.SharedPreferences
import android.content.res.Resources

class SharedPrefHelper(private val resources: Resources, private val sharedPref: SharedPreferences) {
    private val TAG = "SharedPrefHelper"

    fun getInstance(): SharedPreferences {
        return this.sharedPref
    }

    fun getString(
        attributeId: Int, defaultId: Int
    ): String {
        val attributeName = resources.getString(attributeId)
        val defaultValue = resources.getString(defaultId)

        return sharedPref.getString(attributeName, defaultValue).toString()
    }

    fun getBoolean(
        attributeId: Int, defaultId: Int
    ): Boolean {
        val attributeName = resources.getString(attributeId)
        val defaultValue = java.lang.Boolean.parseBoolean(resources.getString(defaultId))

        return sharedPref.getBoolean(attributeName, defaultValue)
    }

    fun getBoolean(
        attributeId: Int, defaultValue: Boolean
    ): Boolean {
        val attributeName = resources.getString(attributeId)
        return sharedPref.getBoolean(attributeName, defaultValue)
    }

    fun getInt(
        attributeId: Int, defaultId: Int
    ): Int {
        val attributeName = resources.getString(attributeId)
        val defaultValue = resources.getString(defaultId)

        return sharedPref.getString(attributeName, defaultValue).toString().toInt()
    }

    fun putString(attributeId:Int,value:String){
        val attributeName = resources.getString(attributeId)
        sharedPref.edit()
            .putString(attributeName,value)
            .apply()
    }

    fun putBoolean(key:Int,value:Boolean){
        val attributeName = resources.getString(key)
        sharedPref.edit()
            .putBoolean(attributeName,value)
            .apply()
    }
}