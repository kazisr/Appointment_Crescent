package com.kazi.clinicapp

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// small app context locator (set in Application or MainActivity if you prefer)
// For simplicity we set it here; in production use a proper Application subclass.
object AppContextLocator {
    lateinit var appContext: Context
}

private val Context.dataStore by preferencesDataStore(name = "clinic_prefs")

object DataStoreManager {
    private val KEY_PATIENT = stringPreferencesKey("patient_name")
    private val KEY_MOBILE = stringPreferencesKey("mobile")
    private val KEY_DOB = stringPreferencesKey("dob")
    private val KEY_SEX = stringPreferencesKey("sex")
    private val KEY_VISIT_TYPE = stringPreferencesKey("visit_type")
    private val KEY_VISIT_DATE = stringPreferencesKey("visit_date")

    fun patientNameFlow(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_PATIENT] ?: "" }
    fun mobileFlow(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_MOBILE] ?: "" }
    fun dobFlow(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_DOB] ?: "" }
    fun sexFlow(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_SEX] ?: "Female" }
    fun visitTypeFlow(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_VISIT_TYPE] ?: "" }
    fun visitDateFlow(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_VISIT_DATE] ?: "" }

    suspend fun savePatientName(context: Context, value: String) = context.dataStore.edit { it[KEY_PATIENT] = value }
    suspend fun saveMobile(context: Context, value: String) = context.dataStore.edit { it[KEY_MOBILE] = value }
    suspend fun saveDob(context: Context, value: String) = context.dataStore.edit { it[KEY_DOB] = value }
    suspend fun saveSex(context: Context, value: String) = context.dataStore.edit { it[KEY_SEX] = value }
    suspend fun saveVisitType(context: Context, value: String) = context.dataStore.edit { it[KEY_VISIT_TYPE] = value }
    suspend fun saveVisitDate(context: Context, value: String) = context.dataStore.edit { it[KEY_VISIT_DATE] = value }
}