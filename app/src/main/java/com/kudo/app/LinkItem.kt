package com.kudo.app

data class LinkItem(
    val id: String,
    val url: String,
    val title: String?,
    val timestamp: Double
)

data class DocItem(
    val id: String,
    val filename: String,
    val original_name: String,
    val size: Long,
    val mime_type: String,
    val timestamp: Double
)

data class ChatMessage(
    val role: String,
    val content: String
)
