package com.securenotes.app.domain.model

data class Folder(
    val id: Long,
    val name: String,
    val noteCount: Int = 0
)
