package com.conkeep.ui.util

import android.content.Context
import android.util.TypedValue

fun Int.dpToPx(context: Context): Int =
    TypedValue
        .applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics,
        ).toInt()

fun Float.spToDp(context: Context): Float =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        context.resources.displayMetrics,
    ) / context.resources.displayMetrics.density
