package com.kazi.clinicapp

object JsonUtils {
    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
    fun buildJson(visitDate: String, drCode: String, drName: String, patientName: String, mobileNo: String, dob: String, ageYears: Int, ageMonths: Int, ageDays: Int, sex: String, visitType: String): String {
        return """{
  "VisitDate":"${esc(visitDate)}",
  "DrCode":"${esc(drCode)}",
  "DrName":"${esc(drName)}",
  "PatientName":"${esc(patientName)}",
  "MobileNo":"${esc(mobileNo)}",
  "Dob":"${esc(dob)}",
  "AgeDay":"$ageDays",
  "AgeMonth":"$ageMonths",
  "AgeYear":"$ageYears",
  "Sex":"${esc(sex)}",
  "VisitType":"${esc(visitType)}"
}""".trimIndent()
    }
}