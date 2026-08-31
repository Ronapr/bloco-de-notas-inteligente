package com.securenotes.app.database.dao

data class FolderWithCount(
    val id: Long,
    val name: String,
    val noteCount: Int
)
