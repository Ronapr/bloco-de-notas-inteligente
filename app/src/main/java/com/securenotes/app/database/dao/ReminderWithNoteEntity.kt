package com.securenotes.app.database.dao

data class ReminderWithNoteEntity(
    val id: Long,
    val noteId: Long,
    val noteTitle: String,
    val reminderAt: Long,
    val shown: Boolean,
    val createdAt: Long
)

data class NoteReminderAt(
    val noteId: Long,
    val reminderAt: Long
)
