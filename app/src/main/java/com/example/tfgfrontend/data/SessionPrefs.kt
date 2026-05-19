package com.example.tfgfrontend.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionPrefs {
    private const val PREFS_NAME = "session_prefs"
    private const val KEY_USER_REMEMBER = "user_remember"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PASSWORD = "user_password"

    private const val KEY_BUSINESS_REMEMBER = "business_remember"
    private const val KEY_BUSINESS_DNI = "business_dni"
    private const val KEY_BUSINESS_NAME = "business_name"
    private const val KEY_BUSINESS_EMAIL = "business_email"
    private const val KEY_BUSINESS_PASSWORD = "business_password"
    private const val KEY_BUSINESS_UID = "business_uid"

    private fun prefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveUserCredentials(context: Context, email: String, password: String) {
        prefs(context).edit()
            .putBoolean(KEY_USER_REMEMBER, true)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PASSWORD, password)
            .apply()
    }

    fun clearUserCredentials(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_USER_REMEMBER, false)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PASSWORD)
            .apply()
    }

    fun shouldRememberUser(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USER_REMEMBER, false)

    fun getUserEmail(context: Context): String =
        prefs(context).getString(KEY_USER_EMAIL, "").orEmpty()

    fun getUserPassword(context: Context): String =
        prefs(context).getString(KEY_USER_PASSWORD, "").orEmpty()

    fun saveBusinessCredentials(
        context: Context,
        dni: String,
        businessName: String,
        email: String,
        password: String,
        businessUid: String
    ) {
        prefs(context).edit()
            .putBoolean(KEY_BUSINESS_REMEMBER, true)
            .putString(KEY_BUSINESS_DNI, dni)
            .putString(KEY_BUSINESS_NAME, businessName)
            .putString(KEY_BUSINESS_EMAIL, email)
            .putString(KEY_BUSINESS_PASSWORD, password)
            .putString(KEY_BUSINESS_UID, businessUid)
            .apply()
    }

    fun clearBusinessCredentials(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_BUSINESS_REMEMBER, false)
            .remove(KEY_BUSINESS_DNI)
            .remove(KEY_BUSINESS_NAME)
            .remove(KEY_BUSINESS_EMAIL)
            .remove(KEY_BUSINESS_PASSWORD)
            .remove(KEY_BUSINESS_UID)
            .apply()
    }

    fun shouldRememberBusiness(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BUSINESS_REMEMBER, false)

    fun getBusinessDni(context: Context): String =
        prefs(context).getString(KEY_BUSINESS_DNI, "").orEmpty()

    fun getBusinessName(context: Context): String =
        prefs(context).getString(KEY_BUSINESS_NAME, "").orEmpty()

    fun getBusinessEmail(context: Context): String =
        prefs(context).getString(KEY_BUSINESS_EMAIL, "").orEmpty()

    fun getBusinessPassword(context: Context): String =
        prefs(context).getString(KEY_BUSINESS_PASSWORD, "").orEmpty()

    fun getBusinessUid(context: Context): String =
        prefs(context).getString(KEY_BUSINESS_UID, "").orEmpty()

}
