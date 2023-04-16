package com.br.triatodetect.utils

import android.content.Context
import android.content.SharedPreferences
import com.br.triatodetect.models.User

class SessionManager private constructor(private val context: Context) {

    fun saveUserData(user: User) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(USER_NAME, user.name)
        editor.putString(USER_EMAIL, user.email)
        editor.apply()
    }

    fun getUserData(): User? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val name = sharedPreferences.getString(USER_NAME, null)
        val email = sharedPreferences.getString(USER_EMAIL, null)
        if (name != null && email != null) {
            return User(name, email)
        }
        return null
    }

    fun clearAllData() {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.clear()
        editor.apply()
    }

    companion object {

        private const val SHARED_PREFS = "TriatokeyPreference"
        private const val USER_EMAIL = "user_email"
        private const val USER_NAME = "user_name"

        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            if (instance == null) {
                instance = SessionManager(context)
            }
            return instance as SessionManager
        }
    }


}
