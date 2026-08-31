package com.securenotes.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val noteId: Long = 0L,
    val reminderAt: Long = 0L,
    val shown: Boolean = false,
    val createdAt: Long = 0L
)
