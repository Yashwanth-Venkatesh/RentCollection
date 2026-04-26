package com.rentcollection.utils

import java.text.NumberFormat
import java.util.Locale

fun Double.toRupees(): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return fmt.format(this)
}

fun Int.toMonthName(): String {
    val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec")
    return if (this in 1..12) months[this - 1] else "-"
}

fun Int.toFullMonthName(): String {
    val months = arrayOf("January","February","March","April","May","June",
        "July","August","September","October","November","December")
    return if (this in 1..12) months[this - 1] else "-"
}
