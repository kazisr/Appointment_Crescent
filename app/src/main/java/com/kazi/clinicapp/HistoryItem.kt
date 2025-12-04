package com.kazi.clinicapp

data class HistoryItem(
    val id: Int,
    val timestamp: String,
    val statusCodeOrError: String,
    val responseBody: String,
    val rawPayload: String,
    val patientName: String? = null,
    val visitDate: String? = null,
    val mobileNo: String? = null,
    val ageYears: Int? = null,
    val ageMonths: Int? = null,
    val ageDays: Int? = null,
    val drName: String? = null,
    val visitType: String? = null
)