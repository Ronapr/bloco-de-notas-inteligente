package com.securenotes.app.domain.model

data class Note(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val color: String = "",
    val deleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val favorite: Boolean = false,
    val folderId: Long? = null,
    val pinned: Boolean = false
)
