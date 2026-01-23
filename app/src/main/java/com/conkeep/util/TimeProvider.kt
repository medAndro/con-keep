package com.conkeep.util

import kotlinx.datetime.LocalDate

interface TimeProvider {
    fun getToday(): LocalDate
}
