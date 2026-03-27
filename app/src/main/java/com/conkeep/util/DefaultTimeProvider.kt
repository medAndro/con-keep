package com.conkeep.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock

class DefaultTimeProvider
    @Inject
    constructor() : TimeProvider {
        override fun getToday(): LocalDate =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
    }
