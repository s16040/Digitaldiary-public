package com.example.app.model

data class Entry(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val lat: Double? = null,
    val lon: Double? = null,
    val place: String? = null,
    val photoUrl: String? = null,
    val audioUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) 