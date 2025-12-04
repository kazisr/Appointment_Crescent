package com.kazi.clinicapp

import java.time.LocalDate
import java.time.Period

fun computeFullAge(dobString: String): Triple<Int, Int, Int> {
    return try {
        val birth = LocalDate.parse(dobString)
        val today = LocalDate.now()

        if (birth.isAfter(today)) return Triple(0, 0, 0)

        val total = Period.between(birth, today)

        Triple(
            total.years.coerceAtLeast(0),
            total.months.coerceAtLeast(0),
            total.days.coerceAtLeast(0)
        )

    } catch (e: Exception) {
        Triple(0, 0, 0)
    }
}