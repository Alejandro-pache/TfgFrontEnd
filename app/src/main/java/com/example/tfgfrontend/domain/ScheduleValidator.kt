package com.example.tfgfrontend.domain

object ScheduleValidator {
    private val hourRegex = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")

    fun isValidHour(value: String): Boolean = hourRegex.matches(value)

    fun isOpenBeforeClose(open: String, close: String): Boolean = open < close
}
