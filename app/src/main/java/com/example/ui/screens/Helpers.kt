package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.*

fun formatTimestamp(timestamp: Long, format: String = "dd MMM yyyy, HH:mm"): String {
    return try {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "N/A"
    }
}

fun getTeamFlag(teamName: String): String {
    return when (teamName.trim().uppercase()) {
        "MÉXICO", "MEXICO", "MEX" -> "🇲🇽"
        "CANADÁ", "CANADA", "CAN" -> "🇨🇦"
        "ESTADOS UNIDOS", "EEUU", "USA" -> "🇺🇸"
        "ARGENTINA", "ARG" -> "🇦🇷"
        "BRAZIL", "BRASIL", "BRA" -> "🇧🇷"
        "ESPAÑA", "ESP" -> "🇪🇸"
        "FRANCIA", "FRA" -> "🇫🇷"
        "ALEMANIA", "GER" -> "🇩🇪"
        "ITALIA", "ITA" -> "🇮🇹"
        "INGLATERRA", "ENG" -> "🇬🇧"
        "JAPÓN", "JPN" -> "🇯🇵"
        else -> "⚽"
    }
}
