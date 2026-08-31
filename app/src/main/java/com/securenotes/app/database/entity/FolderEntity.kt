package com.securenotes.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [Index(value = ["name"], unique = true)]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String = ""
)
