package com.examples.wavesoffood

import android.content.Context
import android.content.SharedPreferences

object PrefManager {
    private const val PREF_NAME = "app_prefs"
    private const val IS_FIRST_TIME = "isFirstTime"

    fun isFirstTime(context: Context): Boolean {
        val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(IS_FIRST_TIME, true)
    }

    fun setFirstTime(context: Context, isFirstTime: Boolean) {
        val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean(IS_FIRST_TIME, isFirstTime)
        editor.apply()
    }
}
