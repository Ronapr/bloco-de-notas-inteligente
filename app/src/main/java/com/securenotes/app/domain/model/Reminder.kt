package com.securenotes.app.domain.model

data class Reminder(
    val id: Long = 0L,
    val noteId: Long = 0L,
    val noteTitle: String = "",
    val reminderAt: Long = 0L,
    val shown: Boolean = false,
    val createdAt: Long = 0L
)
